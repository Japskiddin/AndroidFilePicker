package io.github.japskiddin.androidfilepicker.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import io.github.japskiddin.androidfilepicker.FileItem;
import io.github.japskiddin.androidfilepicker.R;
import io.github.japskiddin.androidfilepicker.filter.CompositeFilter;
import io.github.japskiddin.androidfilepicker.filter.PatternFilter;
import io.github.japskiddin.androidfilepicker.storage.StorageBean;
import io.github.japskiddin.androidfilepicker.storage.StorageUtils;
import io.github.japskiddin.androidfilepicker.utils.FileUtils;
import io.github.japskiddin.androidfilepicker.utils.PermissionUtils;
import io.github.japskiddin.androidfilepicker.widget.EmptyRecyclerView;
import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class FilePickerActivity extends AppCompatActivity {
  public static final String ARG_START_PATH = "arg_start_path";
  public static final String ARG_CURRENT_PATH = "arg_current_path";

  public static final String ARG_FILTER = "arg_filter";
  public static final String ARG_CLOSEABLE = "arg_closeable";
  public static final String ARG_TITLE = "arg_title";
  public static final String ARG_FILE_PICK = "arg_file_pick";
  public static final String ARG_ADD_DIRS = "arg_add_dirs";

  public static final String STATE_START_PATH = "state_start_path";
  private static final String STATE_CURRENT_PATH = "state_current_path";

  public static final String RESULT_FILE_PATH = "result_file_path";
  private static final int HANDLE_CLICK_DELAY = 150;

  private EmptyRecyclerView mDirectoryRecyclerView;
  private DirectoryAdapter mDirectoryAdapter;
  private View mEmptyView;
  private String mStartPath = Environment.getExternalStorageDirectory().getAbsolutePath();
  private String mCurrentPath = mStartPath;
  private CharSequence mTitle;
  private TextView tvToolbarTitle;
  private boolean mCloseable;
  private boolean isFilePick;
  private boolean addDirs;
  private boolean isHome = true;
  private CompositeFilter mFilter;
  private List<FileItem> storages = new ArrayList<>();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_file_picker);

    if (!PermissionUtils.hasPermissions(this)) {
      finish();
      return;
    }

    initArguments(savedInstanceState);
    initViews();
    initToolbar();
    initFilesList();
  }

  @Override public void onBackPressed() {
    backClick(true);
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_CURRENT_PATH, mCurrentPath);
    outState.putString(STATE_START_PATH, mStartPath);
  }

  private void initArguments(Bundle savedInstanceState) {
    Intent intent = getIntent();
    if (intent.hasExtra(ARG_FILTER)) {
      Serializable filter;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        filter = intent.getSerializableExtra(ARG_FILTER, Serializable.class);
      } else {
        //noinspection deprecation
        filter = intent.getSerializableExtra(ARG_FILTER);
      }

      if (filter instanceof Pattern) {
        ArrayList<FileFilter> filters = new ArrayList<>();
        filters.add(new PatternFilter((Pattern) filter, false));
        mFilter = new CompositeFilter(filters);
      } else {
        mFilter = (CompositeFilter) filter;
      }
    }

    if (savedInstanceState != null) {
      mStartPath = savedInstanceState.getString(STATE_START_PATH);
      mCurrentPath = savedInstanceState.getString(STATE_CURRENT_PATH);
    } else {
      if (intent.hasExtra(ARG_START_PATH)) {
        mStartPath = intent.getStringExtra(ARG_START_PATH);
        mCurrentPath = mStartPath;
      }

      if (intent.hasExtra(ARG_CURRENT_PATH)) {
        String currentPath = intent.getStringExtra(ARG_CURRENT_PATH);

        if (currentPath != null && currentPath.startsWith(mStartPath)) {
          mCurrentPath = currentPath;
        }
      }
    }

    if (intent.hasExtra(ARG_TITLE)) {
      mTitle = intent.getCharSequenceExtra(ARG_TITLE);
    }

    if (intent.hasExtra(ARG_CLOSEABLE)) {
      mCloseable = intent.getBooleanExtra(ARG_CLOSEABLE, true);
    }

    if (intent.hasExtra(ARG_FILE_PICK)) {
      isFilePick = intent.getBooleanExtra(ARG_FILE_PICK, false);
    }

    if (intent.hasExtra(ARG_ADD_DIRS)) {
      addDirs = intent.getBooleanExtra(ARG_ADD_DIRS, true);
    }

    prepareStoragesList();
  }

  private void initToolbar() {
    // Truncate start of path
    tvToolbarTitle.setSingleLine();
    tvToolbarTitle.setHorizontallyScrolling(true);
    tvToolbarTitle.setEllipsize(TextUtils.TruncateAt.START);
    ImageView ivToolbarAdd = findViewById(R.id.iv_toolbar_add);
    ImageView ivToolbarBack = findViewById(R.id.iv_filepicker_toolbar_back);
    ImageView ivToolbarCheck = findViewById(R.id.iv_toolbar_check);

    if (!TextUtils.isEmpty(mTitle)) {
      tvToolbarTitle.setText(mTitle);
    }
    if (!addDirs) {
      ivToolbarAdd.setVisibility(View.GONE);
    }
    if (isFilePick) {
      ivToolbarCheck.setVisibility(View.GONE);
    }
    ivToolbarAdd.setOnClickListener(v -> showNewFolderDialog());
    ivToolbarCheck.setOnClickListener(v -> setResultAndFinish(mCurrentPath));
    ivToolbarBack.setOnClickListener(view -> {
      setResult(RESULT_CANCELED);
      finish();
    });
    updateTitle();
  }

  private void initViews() {
    tvToolbarTitle = findViewById(R.id.tv_filepicker_toolbar_title);
    mDirectoryRecyclerView = findViewById(R.id.directory_recycler_view);
    mEmptyView = findViewById(R.id.directory_empty_view);
    ImageView ivHome = findViewById(R.id.iv_home);
    LinearLayout btnUp = findViewById(R.id.btn_back);

    btnUp.setOnClickListener(view -> backClick(false));
    ivHome.setOnClickListener(view -> {
      isHome = true;
      updateTitle();
      initFilesList();
    });
  }

  private void updateTitle() {
    if (isHome) {
      tvToolbarTitle.setText(R.string.afp_select_directory);
    } else {
      String titlePath = mCurrentPath.isEmpty() ? "/" : mCurrentPath;
      tvToolbarTitle.setText(titlePath);
    }
  }

  private void initFilesList() {
    mDirectoryAdapter =
        new DirectoryAdapter(this,
            isHome ? storages : FileUtils.getFileListByDirPath(mCurrentPath, mFilter), isHome);

    mDirectoryAdapter.setOnItemClickListener(
        (view, position) -> onFileClicked(mDirectoryAdapter.getModel(position)));

    mDirectoryRecyclerView.setAdapter(mDirectoryAdapter);
    mDirectoryRecyclerView.setEmptyView(mEmptyView);
  }

  private void onFileClicked(final File clickedFile) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> handleFileClicked(clickedFile),
        HANDLE_CLICK_DELAY);
  }

  private void backClick(boolean isBackPressed) {
    if (!isHome) {
      if (!mCurrentPath.equals(mStartPath)) {
        mCurrentPath = FileUtils.cutLastSegmentOfPath(mCurrentPath);
        updateTitle();
        initFilesList();
      } else {
        isHome = true;
        updateTitle();
        initFilesList();
      }
    } else {
      if (isBackPressed) {
        setResult(RESULT_CANCELED);
        finish();
      }
    }
  }

  private void showNewFolderDialog() {
    final LayoutInflater layoutInflater = this.getLayoutInflater();
    LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.afp_dialog_new_dir,
        findViewById(R.id.main_layout), false);
    final EditText dirName = linearLayout.findViewById(R.id.et_dirName);
    AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
    alertDialog.setView(linearLayout);
    alertDialog.setTitle(R.string.afp_dialog_title);
    alertDialog.setNegativeButton(R.string.afp_dialog_cancel,
        (dialogInterface, i) -> dialogInterface.dismiss());
    alertDialog.setPositiveButton(R.string.afp_dialog_create,
        (dialogInterface, i) -> createNewFolder(dirName.getText().toString()));
    alertDialog.create().show();
  }

  private void createNewFolder(String dirName) {
    if (!TextUtils.isEmpty(dirName)) {
      File dir = new File(mCurrentPath, dirName);
      if (!dir.exists()) {
        boolean created = dir.mkdir();
        if (created) {
          initFilesList();
        } else {
          Toast.makeText(getApplicationContext(), getString(R.string.afp_error_folder_created),
              Toast.LENGTH_LONG).show();
        }
      } else {
        Toast.makeText(getApplicationContext(), getString(R.string.afp_error_folder_exists),
            Toast.LENGTH_LONG).show();
      }
    } else {
      Toast.makeText(getApplicationContext(), getString(R.string.afp_error_folder_name),
          Toast.LENGTH_LONG).show();
    }
  }

  private void handleFileClicked(final File clickedFile) {
    if (clickedFile.isDirectory()) {
      if (isHome) {
        mStartPath = clickedFile.getPath();
        isHome = false;
      }

      mCurrentPath = clickedFile.getPath();
      // If the user wanna go to the emulated directory, he will be taken to the
      // corresponding user emulated folder.
      if (mCurrentPath.equals("/storage/emulated")) {
        mCurrentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
      }

      updateTitle();
      initFilesList();
    }

    if (isFilePick && clickedFile.isFile()) {
      mCurrentPath = clickedFile.getPath();
      setResultAndFinish(mCurrentPath);
    }
  }

  private void setResultAndFinish(String filePath) {
    Intent data = new Intent();
    data.putExtra(RESULT_FILE_PATH, filePath);
    setResult(RESULT_OK, data);
    finish();
  }

  private void prepareStoragesList() {
    List<StorageBean> storageBeans = StorageUtils.getStorageData(this);
    storages = new ArrayList<>();
    int removableCount = 0;
    if (storageBeans != null) {
      for (StorageBean storageBean : storageBeans) {
        if (storageBean.getRemovable()) {
          removableCount++;
        }
      }

      int removablePosName = 1;

      for (StorageBean storageBean : storageBeans) {
        if (!storageBean.getRemovable()) {
          storages.add(
              new FileItem(getString(R.string.afp_internal_storage), storageBean.getPath()));
        } else {
          String name =
              removableCount > 1 ? (getString(R.string.afp_micro_sd) + " " + removablePosName)
                  : getString(R.string.afp_micro_sd);
          storages.add(new FileItem(name, storageBean.getPath()));
          if (removableCount > 1) removablePosName++;
        }
      }
    } else {
      String path = Environment.getExternalStorageDirectory().getAbsolutePath();
      storages.add(new FileItem(getString(R.string.afp_internal_storage), path));
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      File[] folders = getExternalCacheDirs();
      if (folders != null) {
        for (File folder : folders) {
          if (folder == null) continue;
          String path = folder.getAbsolutePath();
          int folderIndex = path.indexOf("/Android");
          if (folderIndex == -1) continue;
          File storageFile = new File(path.substring(0, folderIndex));
          boolean found = false;
          for (FileItem fileItem : storages) {
            if (fileItem.getFilePath().equals(storageFile.getAbsolutePath())) {
              found = true;
              break;
            }
          }

          if (!found) {
            storages.add(new FileItem(storageFile.getName(), storageFile.getAbsolutePath()));
          }
        }
      }
    }

    if (storages.size() > 1) {
      Collections.sort(storages, (fileItem1, fileItem2) -> {
        if (fileItem1.getFileName().equals(getString(R.string.afp_internal_storage))
            || fileItem2.getFileName().equals(getString(R.string.afp_internal_storage))) {
          return 1;
        } else {
          return fileItem1.getFileName().compareTo(fileItem2.getFileName());
        }
      });
    }
  }
}