package com.avgtechie.glsurfaceview;

import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;

import gles.EglCore;
import gles.WindowSurface;

/**
 * Created by ashish on 10/28/14.
 */
public class MoviePlayerUtil implements MoviePlayer.PlayerFeedback {

    private static final String TAG = "MoviePlayerUtil";
    private MoviePlayer.PlayTask mPlayTask;
    private static MoviePlayerUtil moviePlayerUtil;

    public static MoviePlayerUtil getInstance() {
        if (moviePlayerUtil == null) {
            moviePlayerUtil = new MoviePlayerUtil();
        }
        return moviePlayerUtil;
    }

    public void playAndStopMovie(SurfaceHolder surfaceHolder) {

        if (mPlayTask != null) {
            Log.w(TAG, "movie already playing");
            return;
        }

        Log.d(TAG, "starting movie");
        SpeedControlCallback callback = new SpeedControlCallback();

        MoviePlayer player = null;
        Surface surface = surfaceHolder.getSurface();
        try {
            File inputMediaFile = FileUtil.getInstance().getMemeFilePath();
            if (!inputMediaFile.exists()) {
                return;
            }
            player = new MoviePlayer(inputMediaFile, surface, callback);
        } catch (Exception ioe) {
            Log.e(TAG, "Unable to play movie", ioe);
            surface.release();
            return;
        }

        //AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.playMovie_afl);
        int width = player.getVideoWidth();
        int height = player.getVideoHeight();
        //layout.setAspectRatio((double) width / height);
        surfaceHolder.setFixedSize(width, height);

        mPlayTask = new MoviePlayer.PlayTask(player, this);
        mPlayTask.setLoopMode(true);
//        mShowStopLabel = true;
//        updateControls();
        mPlayTask.execute();
    }


    public void clearSurface(Surface surface) {
        // We need to do this with OpenGL ES (*not* Canvas -- the "software render" bits
        // are sticky).  We can't stay connected to the Surface after we're done because
        // that'd prevent the video encoder from attaching.
        //
        // If the Surface is resized to be larger, the new portions will be black, so
        // clearing to something other than black may look weird unless we do the clear
        // post-resize.
        EglCore eglCore = new EglCore();
        WindowSurface win = new WindowSurface(eglCore, surface, false);
        win.makeCurrent();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        win.swapBuffers();
        win.release();
        eglCore.release();
    }

    @Override
    public void playbackStopped() {
        Log.d(TAG, "playbackStopped()");
    }
}
