package com.avgtechie.glsurfaceview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.avgtechie.surfaceviewdemo.R;

import java.io.File;

public class GLSurfaceViewActivity extends Activity implements SurfaceHolder.Callback, View.OnTouchListener, Choreographer.FrameCallback {

    public static final String TAG = "GLSurfaceViewActivity";
    private SurfaceView surfaceView;
    private RenderThread mRenderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_glsurface_view);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    public void stopRecording(View view) {
        Log.d(TAG, "stopRecording");
        mRenderThread.stopEncoder();
    }

    public void startRecording(View view) {
        Log.d(TAG, "startRecording");
        mRenderThread.startEncoder();
    }


    //SurfaceHolder.Callback methods
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");
        File outputFile = new File(FileUtil.getInstance().getMemeDirPath(), "video-recording.mp4");
        //File outputFile = new File(getFilesDir(), "video-recording.mp4");
        mRenderThread = new RenderThread(surfaceView.getHolder(), outputFile, this);
        mRenderThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendSurfaceCreated();
        }
        // start the draw events
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendSurfaceChanged(format, width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed holder=" + holder);

        // We need to wait for the render thread to shut down before continuing because we
        // don't want the Surface to disappear out from under it mid-render.  The frame
        // notifications will have been stopped back in onPause(), but there might have
        // been one in progress.
        //
        // TODO: the RenderThread doesn't currently wait for the encoder / muxer to stop,
        //       so we can't use this as an indication that the .mp4 file is complete.

        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            rh.sendShutdown();
            try {
                mRenderThread.join();
            } catch (InterruptedException ie) {
                // not expected
                throw new RuntimeException("join was interrupted", ie);
            }
        }
        mRenderThread = null;
        //mRecordingEnabled = false;

        // If the callback was posted, remove it.  Without this, we could get one more
        // call on doFrame().
        Choreographer.getInstance().removeFrameCallback(this);
        Log.d(TAG, "surfaceDestroyed complete");
    }

    //OnTouchListenerMethod
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, String.format("onTouch X = %f, Y = %f", event.getX(), event.getY()));
        return true;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        //Log.d(TAG, "doFrame : " + frameTimeNanos);
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            Choreographer.getInstance().postFrameCallback(this);
            rh.sendDoFrame(frameTimeNanos);
        }
    }
}
