package com.avgtechie.glsurfaceview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;

/**
 * Created by ashish on 10/26/14.
 */
public class ClearGLSurfaceView extends GLSurfaceView {

    private ClearRenderer mRenderer;

    public ClearGLSurfaceView(Context context) {
        super(context);
        Log.d(GLSurfaceViewActivity.TAG, "ClearGLSurfaceView()");
        mRenderer = new ClearRenderer(getHolder(), new File(getMemeDirPath(), "video-output.mp4"));
        setRenderer(mRenderer);
    }


    public File getMemeDirPath() {
        String path = Environment.getExternalStorageDirectory().toString() + "/memes";
        File destDir = new File(path);
        return destDir;
    }

    public ClearGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRenderer = new ClearRenderer(getHolder(), new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "video-output.mp4"));
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        Log.d(GLSurfaceViewActivity.TAG, "onTouchEvent()");
        queueEvent(new Runnable() {
            @Override
            public void run() {
                Log.d(GLSurfaceViewActivity.TAG, "run()");
                mRenderer.setColor(event.getX() / getWidth(), event.getY() / getHeight(), 1.0f);
            }
        });
        return true;
    }

    public void stopVideo() {
        mRenderer.stopEncoder();
    }
}
