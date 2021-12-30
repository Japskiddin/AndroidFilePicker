package io.github.japskiddin.androidfilepicker.utils;

import io.github.japskiddin.androidfilepicker.R;
import java.io.File;

public class FileTypeUtils {
  public enum FileType {
    DIRECTORY(R.drawable.ic_afp_folder, R.string.afp_type_directory), DOCUMENT(R.drawable.ic_afp_file,
        R.string.afp_type_document);

    private final int icon, description;

    FileType(int icon, int description) {
      this.icon = icon;
      this.description = description;
    }

    public int getIcon() {
      return icon;
    }

    public int getDescription() {
      return description;
    }
  }

  public static FileType getFileType(File file) {
    if (file.isDirectory()) {
      return FileType.DIRECTORY;
    }

    return FileType.DOCUMENT;
  }
}
