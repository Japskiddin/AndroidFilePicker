package io.github.japskiddin.androidfilepicker.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;
import androidx.core.os.EnvironmentCompat;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class StorageUtils {
  private static final String TAG = StorageUtils.class.getSimpleName();
  private static final long A_GB = 1073741824;
  private static final long A_MB = 1048576;
  private static final int A_KB = 1024;

  public static ArrayList<StorageBean> getStorageData(Context pContext) {
    final StorageManager storageManager =
        (StorageManager) pContext.getSystemService(Context.STORAGE_SERVICE);
    try {
      final Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
      final Class<?> storageValumeClazz = Class.forName("android.os.storage.StorageVolume");
      final Method getPath = storageValumeClazz.getMethod("getPath");
      Method isRemovable = storageValumeClazz.getMethod("isRemovable");
      Method mGetState = null;

      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
        try {
          mGetState = storageValumeClazz.getMethod("getState");
        } catch (NoSuchMethodException e) {
          e.printStackTrace();
        }
      }

      final Object invokeVolumeList = getVolumeList.invoke(storageManager);
      final int length = Array.getLength(invokeVolumeList);
      ArrayList<StorageBean> list = new ArrayList<>();

      for (int i = 0; i < length; i++) {
        final Object storageValume = Array.get(invokeVolumeList, i);
        final String path = (String) getPath.invoke(storageValume);
        final boolean removable = (Boolean) isRemovable.invoke(storageValume);
        String state;
        if (mGetState != null) {
          state = (String) mGetState.invoke(storageValume);
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            state = Environment.getStorageState(new File(path));
          } else {
            if (removable) {
              state = EnvironmentCompat.getStorageState(new File(path));
            } else {
              state = Environment.MEDIA_MOUNTED;
            }
            final File externalStorageDirectory = Environment.getExternalStorageDirectory();
            Log.e(TAG, "externalStorageDirectory==" + externalStorageDirectory);
          }
        }
        long totalSize = 0;
        long availableSize = 0;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
          totalSize = StorageUtils.getTotalSize(path);
          availableSize = StorageUtils.getAvailableSize(path);
        }
        final String msg = "path=="
            + path
            + " ,removable=="
            + removable
            + ",state=="
            + state
            + ",total size=="
            + totalSize
            + "("
            + StorageUtils.fmtSpace(totalSize)
            + ")"
            + ",availale size=="
            + availableSize
            + "("
            + StorageUtils.fmtSpace(availableSize)
            + ")";
        Log.e(TAG, msg);
        StorageBean storageBean = new StorageBean();
        storageBean.setAvailableSize(availableSize);
        storageBean.setTotalSize(totalSize);
        storageBean.setMounted(state);
        storageBean.setPath(path);
        storageBean.setRemovable(removable);
        list.add(storageBean);
      }
      return list;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static long getTotalSize(String path) {
    try {
      final StatFs statFs = new StatFs(path);
      long blockSize;
      long blockCountLong;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        blockSize = statFs.getBlockSizeLong();
        blockCountLong = statFs.getBlockCountLong();
      } else {
        blockSize = statFs.getBlockSize();
        blockCountLong = statFs.getBlockCount();
      }
      return blockSize * blockCountLong;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  private static long getAvailableSize(String path) {
    try {
      final StatFs statFs = new StatFs(path);
      long blockSize;
      long availableBlocks;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        blockSize = statFs.getBlockSizeLong();
        availableBlocks = statFs.getAvailableBlocksLong();
      } else {
        blockSize = statFs.getBlockSize();
        availableBlocks = statFs.getAvailableBlocks();
      }
      return availableBlocks * blockSize;
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  @SuppressWarnings("deprecation") public static long getAvailableInternalMemorySize() {
    StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
    long availSize;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      availSize = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
    } else {
      availSize = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
    }

    return availSize;
  }

  @SuppressWarnings("deprecation") public static long getTotalInternalMemorySize() {
    StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
    long availSize;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      availSize = stat.getBlockCountLong() * stat.getBlockSizeLong();
    } else {
      availSize = (long) stat.getBlockSize() * (long) stat.getBlockCount();
    }

    return availSize;
  }

  public static String fmtSpace(long space) {
    if (space <= 0) {
      return "0";
    }
    double gbValue = (double) space / A_GB;
    if (gbValue >= 1) {
      return String.format("%.2fGB", gbValue);
    } else {
      double mbValue = (double) space / A_MB;
      Log.e("GB", "gbvalue=" + mbValue);
      if (mbValue >= 1) {
        return String.format("%.2fMB", mbValue);
      } else {
        final double kbValue = space / A_KB;
        return String.format("%.2fKB", kbValue);
      }
    }
  }
}