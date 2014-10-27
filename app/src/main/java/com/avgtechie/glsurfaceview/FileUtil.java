package com.avgtechie.glsurfaceview;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by ashish on 10/26/14.
 */
public class FileUtil {
    private static FileUtil fileUtil;

    public static FileUtil getInstance() {
        if (fileUtil == null) {
            fileUtil = new FileUtil();
        }
        return fileUtil;
    }

    public File getMemeDirPath() {
        File destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

        return destDir;
    }


    public File getMemeFilePath(String fileName) {
        File memeDir = getMemeDirPath();
        File imageFile = new File(memeDir, fileName);
        return imageFile;
    }

    public File createMemeFileIfNotExist(String fileName) {
        File destDir = getMemeDirPath();
        if (destDir != null && !destDir.exists()) {
            destDir.mkdirs();
        }

        File memeFile = getMemeFilePath(fileName);
        if (!memeFile.exists()) {
            try {
                memeFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return memeFile;
    }
}
