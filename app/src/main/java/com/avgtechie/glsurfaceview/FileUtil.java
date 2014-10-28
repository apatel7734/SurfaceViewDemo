package com.avgtechie.glsurfaceview;

import android.os.Environment;

import java.io.File;

/**
 * Created by ashish on 10/26/14.
 */
public class FileUtil {
    private static FileUtil fileUtil;

    private String fileName = "video-recording.mp4";

    public static FileUtil getInstance() {
        if (fileUtil == null) {
            fileUtil = new FileUtil();
        }
        return fileUtil;
    }

    public File getMemeDirPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
    }


    public File getMemeFilePath() {
        File memeDir = getMemeDirPath();
        File videoFile = new File(memeDir, fileName);
        return videoFile;
    }
}
