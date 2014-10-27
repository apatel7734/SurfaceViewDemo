package com.avgtechie.glsurfaceview;

import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import gles.EglCore;
import gles.TextureMovieEncoder2;
import gles.VideoEncoderCore;
import gles.WindowSurface;

/**
 * Created by ashish on 10/26/14.
 */
public class ClearRenderer implements GLSurfaceView.Renderer {

    private float mRed;
    private float mGreen;
    private float mBlue;
    private SurfaceHolder surfaceHolder;
    private File mOutputFile;
    private TextureMovieEncoder2 mVideoEncoder;
    private EglCore mEglCore;
    private WindowSurface mInputWindowSurface;
    private WindowSurface mWindowSurface;

    public ClearRenderer(SurfaceHolder sh, File outputFile) {
        surfaceHolder = sh;
        this.mOutputFile = outputFile;
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //do nothing
        prepareGl(surfaceHolder.getSurface());
        Log.d(GLSurfaceViewActivity.TAG, "Output file = " + mOutputFile.getAbsolutePath());
        startEncoder();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        Log.d(GLSurfaceViewActivity.TAG, "onSurfaceChanged");

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.d(GLSurfaceViewActivity.TAG, "onDrawFrame");
        gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        mVideoEncoder.frameAvailableSoon();
    }

    public void setColor(float r, float g, float b) {
        Log.d(GLSurfaceViewActivity.TAG, "setColor");
        mRed = r;
        mGreen = g;
        mBlue = b;
    }


    private void prepareGl(Surface surface) {
        Log.d(GLSurfaceViewActivity.TAG, "prepareGl");
        mWindowSurface = new WindowSurface(mEglCore, surface, false);
        mWindowSurface.makeCurrent();
    }


    /**
     * Creates the video encoder object and starts the encoder thread.  Creates an EGL
     * surface for encoder input.
     */
    private void startEncoder() {
        Log.d(GLSurfaceViewActivity.TAG, "starting to record");
        // Record at 1280x720, regardless of the window dimensions.  The encoder may
        // explode if given "strange" dimensions, e.g. a width that is not a multiple
        // of 16.  We can box it as needed to preserve dimensions.
        final int BIT_RATE = 4000000;   // 4Mbps
        final int VIDEO_WIDTH = 1280;
        final int VIDEO_HEIGHT = 720;
        int windowWidth = 1080;
        int windowHeight = 1080;
        float windowAspect = (float) windowHeight / (float) windowWidth;
        int outWidth, outHeight;
        if (VIDEO_HEIGHT > VIDEO_WIDTH * windowAspect) {
            // limited by narrow width; reduce height
            outWidth = VIDEO_WIDTH;
            outHeight = (int) (VIDEO_WIDTH * windowAspect);
        } else {
            // limited by short height; restrict width
            outHeight = VIDEO_HEIGHT;
            outWidth = (int) (VIDEO_HEIGHT / windowAspect);
        }

        VideoEncoderCore encoderCore;
        try {
            encoderCore = new VideoEncoderCore(VIDEO_WIDTH, VIDEO_HEIGHT, BIT_RATE, mOutputFile);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mInputWindowSurface = new WindowSurface(mEglCore, encoderCore.getInputSurface(), true);
        mVideoEncoder = new TextureMovieEncoder2(encoderCore);
    }


    public void stopEncoder() {
        if (mVideoEncoder != null) {
            Log.d(GLSurfaceViewActivity.TAG, "stopping recorder, mVideoEncoder=" + mVideoEncoder);
            mVideoEncoder.stopRecording();
            // TODO: wait (briefly) until it finishes shutting down so we know file is
            //       complete, or have a callback that updates the UI
            mVideoEncoder = null;
        }
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
    }

}
