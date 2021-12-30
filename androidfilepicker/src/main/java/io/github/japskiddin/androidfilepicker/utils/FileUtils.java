package io.github.japskiddin.androidfilepicker.utils;

import io.github.japskiddin.androidfilepicker.FileItem;
import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileUtils {
  public static List<FileItem> getFileListByDirPath(String path, FileFilter filter) {
    File directory = new File(path);
    File[] files = directory.listFiles(filter);

    if (files == null) {
      return new ArrayList<>();
    }

    List<File> list = Arrays.asList(files);
    Collections.sort(list, new FileComparator());
    List<FileItem> result = new ArrayList<>();
    for (File f : list) {
      result.add(new FileItem(f.getName(), f.getAbsolutePath()));
    }
    return result;
  }

  public static String cutLastSegmentOfPath(String path) {
    if (path.length() - path.replace("/", "").length() <= 1) return "/";
    String newPath = path.substring(0, path.lastIndexOf("/"));
    // We don't need to list the content of /storage/emulated
    if (newPath.equals("/storage/emulated")) newPath = "/storage";
    return newPath;
  }

  public static String getReadableFileSize(long size) {
    if (size <= 0) return "0";
    final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
    int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
    return new DecimalFormat("#").format(size / Math.pow(1024, digitGroups))
        + " "
        + units[digitGroups];
  }

  public static String getCurrentDir(String path) {
    return path.substring(path.lastIndexOf("/") + 1);
  }
}
