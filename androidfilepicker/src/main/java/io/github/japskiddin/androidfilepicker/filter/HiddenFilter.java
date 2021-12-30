package io.github.japskiddin.androidfilepicker.filter;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

public class HiddenFilter implements FileFilter, Serializable {

  @Override public boolean accept(File f) {
    return !f.isHidden();
  }
}