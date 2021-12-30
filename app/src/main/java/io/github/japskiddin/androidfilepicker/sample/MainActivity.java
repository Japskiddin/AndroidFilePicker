package io.github.japskiddin.androidfilepicker.sample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.github.japskiddin.androidfilepicker.AndroidFilePicker;
import io.github.japskiddin.androidfilepicker.ui.FilePickerActivity;

@SuppressWarnings("Convert2Lambda") public class MainActivity extends AppCompatActivity {
  public static final int PERMISSIONS_REQUEST_CODE = 0;
  public static final int FILE_PICKER_REQUEST_CODE = 1;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button pickButton = findViewById(R.id.pick_from_activity);
    pickButton.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        checkPermissionsAndOpenFilePicker();
      }
    });
  }

  private void checkPermissionsAndOpenFilePicker() {
    if (PermissionUtils.hasPermissions(this)) {
      openFilePicker();
    } else {
      PermissionUtils.requestPermissions(this, PERMISSIONS_REQUEST_CODE);
    }
  }

  private void showError() {
    Toast.makeText(this, "Allow external storage reading", Toast.LENGTH_SHORT).show();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST_CODE) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        openFilePicker();
      } else {
        showError();
      }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  private void openFilePicker() {
    new AndroidFilePicker().withActivity(this)
        .withRequestCode(FILE_PICKER_REQUEST_CODE)
        .withHiddenFiles(true)
        .withTitle("Sample title")
        .withFilePick(false)
        .withAddDirs(true)
        .start();
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case FILE_PICKER_REQUEST_CODE: {
        if (resultCode == RESULT_OK) {
          String path = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

          if (path != null) {
            Log.d("Path: ", path);
            Toast.makeText(this, "Picked file: " + path, Toast.LENGTH_LONG).show();
          }
        }
        break;
      }
      case PERMISSIONS_REQUEST_CODE: {
        if (PermissionUtils.hasPermissions(this)) {
          openFilePicker();
        } else {
          showError();
        }
        break;
      }
    }

    super.onActivityResult(requestCode, resultCode, data);
  }
}