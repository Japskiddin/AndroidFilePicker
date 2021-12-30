package io.github.japskiddin.androidfilepicker;

import android.app.Activity;
import android.content.Intent;
import io.github.japskiddin.androidfilepicker.filter.CompositeFilter;
import io.github.japskiddin.androidfilepicker.filter.HiddenFilter;
import io.github.japskiddin.androidfilepicker.filter.PatternFilter;
import io.github.japskiddin.androidfilepicker.ui.FilePickerActivity;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Android File Picker builder
 */
public class AndroidFilePicker {
  private Activity mActivity;
  private Class<? extends FilePickerActivity> mFilePickerClass = FilePickerActivity.class;

  private Integer mRequestCode;
  private Pattern mFileFilter;
  private Boolean mDirectoriesFilter = false;
  private String mRootPath;
  private String mCurrentPath;
  private Boolean mShowHidden = false;
  private Boolean mCloseable = true;
  private CharSequence mTitle;
  private boolean isFilePick = false, addDirs = false;

  public AndroidFilePicker() {
  }

  /**
   * Specifies activity, which will be used to
   * start file picker
   */
  public AndroidFilePicker withActivity(Activity activity) {
    mActivity = activity;
    return this;
  }

  /**
   * Specifies request code that used in activity result
   *
   * @see <a href="https://developer.android.com/training/basics/intents/result.html">Getting a
   * Result from an Activity</a>
   */
  public AndroidFilePicker withRequestCode(int requestCode) {
    mRequestCode = requestCode;
    return this;
  }

  public AndroidFilePicker withFilePick(boolean filePick) {
    isFilePick = filePick;
    return this;
  }

  public AndroidFilePicker withAddDirs(boolean addDirs) {
    this.addDirs = addDirs;
    return this;
  }

  /**
   * Hides files that matched by specified regular expression.
   * Use {@link AndroidFilePicker#withFilterDirectories withFilterDirectories} method
   * to enable directories filtering
   */
  public AndroidFilePicker withFilter(Pattern pattern) {
    mFileFilter = pattern;
    return this;
  }

  /**
   * If directoriesFilter is true directories will also be affected by filter,
   * the default value of directories filter is false
   *
   * @see AndroidFilePicker#withFilter
   */
  public AndroidFilePicker withFilterDirectories(boolean directoriesFilter) {
    mDirectoriesFilter = directoriesFilter;
    return this;
  }

  /**
   * Specifies root directory for picker,
   * user can't go upper that specified path
   */
  public AndroidFilePicker withRootPath(String rootPath) {
    mRootPath = rootPath;
    return this;
  }

  /**
   * Specifies start directory for picker,
   * which will be shown to user at the beginning
   */
  public AndroidFilePicker withPath(String path) {
    mCurrentPath = path;
    return this;
  }

  /**
   * Show or hide hidden files in picker
   */
  public AndroidFilePicker withHiddenFiles(boolean show) {
    mShowHidden = show;
    return this;
  }

  /**
   * Show or hide close menu in picker
   */
  public AndroidFilePicker withCloseMenu(boolean closeable) {
    mCloseable = closeable;
    return this;
  }

  /**
   * Set title of picker
   */
  public AndroidFilePicker withTitle(CharSequence title) {
    mTitle = title;
    return this;
  }

  public AndroidFilePicker withCustomActivity(
      Class<? extends FilePickerActivity> customActivityClass) {
    mFilePickerClass = customActivityClass;
    return this;
  }

  public CompositeFilter getFilter() {
    ArrayList<FileFilter> filters = new ArrayList<>();

    if (!mShowHidden) {
      filters.add(new HiddenFilter());
    }

    if (mFileFilter != null) {
      filters.add(new PatternFilter(mFileFilter, mDirectoriesFilter));
    }

    return new CompositeFilter(filters);
  }

  /**
   * @return Intent that can be used to start Material File Picker
   */
  public Intent getIntent() {
    CompositeFilter filter = getFilter();

    Activity activity = null;
    if (mActivity != null) {
      activity = mActivity;
    }

    Intent intent = new Intent(activity, mFilePickerClass);
    intent.putExtra(FilePickerActivity.ARG_FILTER, filter);
    intent.putExtra(FilePickerActivity.ARG_CLOSEABLE, mCloseable);

    if (mRootPath != null) {
      intent.putExtra(FilePickerActivity.ARG_START_PATH, mRootPath);
    }

    if (mCurrentPath != null) {
      intent.putExtra(FilePickerActivity.ARG_CURRENT_PATH, mCurrentPath);
    }

    if (mTitle != null) {
      intent.putExtra(FilePickerActivity.ARG_TITLE, mTitle);
    }

    intent.putExtra(FilePickerActivity.ARG_FILE_PICK, isFilePick);
    intent.putExtra(FilePickerActivity.ARG_ADD_DIRS, addDirs);

    return intent;
  }

  /**
   * Open Material File Picker activity.
   * You should set Activity or Fragment before calling this method
   *
   * @see AndroidFilePicker#withActivity(Activity)
   */
  public void start() {
    if (mActivity == null) {
      throw new RuntimeException(
          "You must pass Activity/Fragment by calling withActivity/withFragment/withSupportFragment method");
    }

    if (mRequestCode == null) {
      throw new RuntimeException("You must pass request code by calling withRequestCode method");
    }

    Intent intent = getIntent();

    if (mActivity != null) {
      mActivity.startActivityForResult(intent, mRequestCode);
    }
  }
}