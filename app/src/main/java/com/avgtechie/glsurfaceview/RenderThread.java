package com.avgtechie.glsurfaceview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.avgtechie.surfaceviewdemo.R;

import java.io.File;
import java.io.IOException;

import gles.Drawable2d;
import gles.EglCore;
import gles.FlatShadedProgram;
import gles.FullFrameRect;
import gles.GlUtil;
import gles.Sprite2d;
import gles.Texture2dProgram;
import gles.TextureMovieEncoder2;
import gles.VideoEncoderCore;
import gles.WindowSurface;

/**
 * Created by ashish on 10/26/14.
 */
public class RenderThread extends Thread {

    private static final String TAG = "RenderThread";
    SurfaceHolder mSurfaceHolder;
    File mOutputFile;
    RenderHandler mHandler;
    EglCore mEglCore;

    private WindowSurface mWindowSurface;
    private Context mContext;
    Bitmap bitmap;

    // Used for off-screen rendering.
    private int mOffscreenTexture;
    private int mFramebuffer;
    private int mDepthBuffer;
    private FullFrameRect mFullScreen;

    private long mPrevTimeNanos;

    private WindowSurface mInputWindowSurface;
    private TextureMovieEncoder2 mVideoEncoder;

    private float mRectVelX, mRectVelY;     // velocity, in viewport units per second

    // Orthographic projection matrix.
    private float[] mDisplayProjectionMatrix = new float[16];

    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.RECTANGLE);
    private Sprite2d mRect;
    private Sprite2d mEdges[];
    private Sprite2d mRecordRect;
    private float mInnerLeft, mInnerTop, mInnerRight, mInnerBottom;
    private ImageView mImgView;
    private Rect mVideoRect;


    private FlatShadedProgram mProgram;
    private final float[] mIdentityMatrix;


    public RenderThread(SurfaceHolder holder, File outputFile, Context context) {
        mSurfaceHolder = holder;
        mOutputFile = outputFile;
        mContext = context;

        mRect = new Sprite2d(mRectDrawable);
        mImgView = getNewImageView();
        mIdentityMatrix = new float[16];
        Matrix.setIdentityM(mIdentityMatrix, 0);

        mEdges = new Sprite2d[4];
        for (int i = 0; i < mEdges.length; i++) {
            mEdges[i] = new Sprite2d(mRectDrawable);
        }
        mRecordRect = new Sprite2d(mRectDrawable);
        mVideoRect = new Rect();
    }

    public ImageView getNewImageView() {
        ImageView imgView = new ImageView(mContext);
        imgView.setImageResource(R.drawable.ic_launcher);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        imgView.setLayoutParams(params);
        return imgView;
    }


    @Override
    public void run() {
        Log.d(TAG, "run()");
        Looper.prepare();
        mHandler = new RenderHandler(this);
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        Looper.loop();
    }

    public void surfaceCreated() {
        Log.d(TAG, "surfaceCreated()");
        Surface surface = mSurfaceHolder.getSurface();
        prepareGl(surface);
    }

    public void doFrame(long timeStampNanos) {
        Log.d(TAG, "doFrame() : " + timeStampNanos);

        // If we're not keeping up 60fps -- maybe something in the system is busy, maybe
        // recording is too expensive, maybe the CPU frequency governor thinks we're
        // not doing and wants to drop the clock frequencies -- we need to drop frames
        // to catch up.  The "timeStampNanos" value is based on the system monotonic
        // clock, as is System.nanoTime(), so we can compare the values directly.
        //
        // Our clumsy collision detection isn't sophisticated enough to deal with large
        // time gaps, but it's nearly cost-free, so we go ahead and do the computation
        // either way.
        //
        // We can reduce the overhead of recording, as well as the size of the movie,
        // by recording at ~30fps instead of the display refresh rate.  As a quick hack
        // we just record every-other frame, using a "recorded previous" flag.


        update(timeStampNanos);
/*
            long diff = System.nanoTime() - timeStampNanos;
            long max = mRefreshPeriodNanos - 2000000;   // if we're within 2ms, don't bother
            if (diff > max) {
                // too much, drop a frame
                Log.d(TAG, "diff is " + (diff / 1000000.0) + " ms, max " + (max / 1000000.0) +
                        ", skipping render");
                mRecordedPrevious = false;
                mPreviousWasDropped = true;
                mDroppedFrames++;
                return;
            }
*/
        boolean swapResult;

        if (mVideoEncoder == null) {
            // Render the scene, swap back to front.
            draw();
            swapResult = mWindowSurface.swapBuffers();
        } else {

            // recording
            //Log.d(TAG, "MODE: offscreen + blit 2x");
            // Render offscreen.
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
            GlUtil.checkGlError("glBindFramebuffer");
            draw();

            // Blit to display.
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GlUtil.checkGlError("glBindFramebuffer");
            mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
            swapResult = mWindowSurface.swapBuffers();

            // Blit to encoder.
            mVideoEncoder.frameAvailableSoon();
            mInputWindowSurface.makeCurrent();
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);    // again, only really need to
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);     //  clear pixels outside rect
            GLES20.glViewport(mVideoRect.left, mVideoRect.top, mVideoRect.width(), mVideoRect.height());
            mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
            mInputWindowSurface.setPresentationTime(timeStampNanos);
            mInputWindowSurface.swapBuffers();

            // Restore previous values.
            GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
            mWindowSurface.makeCurrent();
        }

        if (!swapResult) {
            // This can happen if the Activity stops without waiting for us to halt.
            Log.w(TAG, "swapBuffers failed, killing renderer thread");
            shutdown();
            return;
        }

        // Update the FPS counter.
        //
        // Ideally we'd generate something approximate quickly to make the UI look
        // reasonable, then ease into longer sampling periods.
        /*
        final int NUM_FRAMES = 120;
        final long ONE_TRILLION = 1000000000000L;
        if (mFpsCountStartNanos == 0) {
            mFpsCountStartNanos = timeStampNanos;
            mFpsCountFrame = 0;
        } else {
            mFpsCountFrame++;
            if (mFpsCountFrame == NUM_FRAMES) {
                // compute thousands of frames per second
                long elapsed = timeStampNanos - mFpsCountStartNanos;
                mActivityHandler.sendFpsUpdate((int) (NUM_FRAMES * ONE_TRILLION / elapsed),
                        mDroppedFrames);

                // reset
                mFpsCountStartNanos = timeStampNanos;
                mFpsCountFrame = 0;
            }
        }
        */
    }


    public void draw() {


        GlUtil.checkGlError("draw start");
        // Clear to a non-black color to make the content easily differentiable from
        // the pillar-/letter-boxing.
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mRect.draw(mProgram, mDisplayProjectionMatrix);


        for (int i = 0; i < 4; i++) {
            mEdges[i].setColor(0.5f, 0.5f, 0.5f);
            mEdges[i].draw(mProgram, mDisplayProjectionMatrix);
        }


        // Give a visual indication of the recording method.

        mRecordRect.setColor(0.0f, 0.0f, 1.0f);

        mRecordRect.draw(mProgram, mDisplayProjectionMatrix);

        GlUtil.checkGlError("draw done");


    }

    private void shutdown() {
        Log.d(TAG, "shutdown");
        stopEncoder();
        Looper.myLooper().quit();
    }

    public void surfaceChanged(int width, int height) {
        Log.d(TAG, "surfaceChanged " + width + "x" + height);

        prepareFramebuffer(width, height);

        // Use full window.
        GLES20.glViewport(0, 0, width, height);

        // Simple orthographic projection, with (0,0) in lower-left corner.
        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

        int smallDim = Math.min(width, height);

        // Set initial shape size / position / velocity based on window size.  Movement
        // has the same "feel" on all devices, but the actual path will vary depending
        // on the screen proportions.  We do it here, rather than defining fixed values
        // and tweaking the projection matrix, so that our squares are square.


        //color of the rectangle
        mRect.setColor(0.5f, 0.5f, 0.5f);
        //size of the rectangle
        mRect.setScale(250, 250);
        //initial position of the rectangle (center )
        mRect.setPosition(width / 2.0f, height / 2.0f);

        mRectVelX = 1 + smallDim / 4.0f;
        mRectVelY = 1 + smallDim / 5.0f;

        // left edge
        float edgeWidth = 1 + width / 64.0f;
        mEdges[0].setScale(edgeWidth, height);
        mEdges[0].setPosition(edgeWidth / 2.0f, height / 2.0f);
        // right edge
        mEdges[1].setScale(edgeWidth, height);
        mEdges[1].setPosition(width - edgeWidth / 2.0f, height / 2.0f);
        // top edge
        mEdges[2].setScale(width, edgeWidth);
        mEdges[2].setPosition(width / 2.0f, height - edgeWidth / 2.0f);
        // bottom edge
        mEdges[3].setScale(width, edgeWidth);
        mEdges[3].setPosition(width / 2.0f, edgeWidth / 2.0f);

        mRecordRect.setColor(1.0f, 1.0f, 1.0f);
        mRecordRect.setScale(edgeWidth * 2f, edgeWidth * 2f);
        mRecordRect.setPosition(edgeWidth / 2.0f, edgeWidth / 2.0f);

        // Inner bounding rect, used to bounce objects off the walls.
        mInnerLeft = mInnerBottom = edgeWidth;
        mInnerRight = width - 1 - edgeWidth;
        mInnerTop = height - 1 - edgeWidth;

        Log.d(TAG, "mRect: " + mRect);

    }


    private void prepareGl(Surface surface) {
        Log.d(TAG, "prepareGl()");
        mWindowSurface = new WindowSurface(mEglCore, surface, false);
        mWindowSurface.makeCurrent();
        mFullScreen = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));
        mProgram = new FlatShadedProgram();

        // Set the background color.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Disable depth testing -- we're 2D only.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Don't need backface culling.  (If you're feeling pedantic, you can turn it on to
        // make sure we're defining our shapes correctly.)
        GLES20.glDisable(GLES20.GL_CULL_FACE);

    }

    /**
     * Returns the render thread's Handler.  This may be called from any thread.
     */
    public RenderHandler getHandler() {
        return mHandler;
    }


    public void startEncoder() {
        Log.d(TAG, "starting to record");
        // Record at 1280x720, regardless of the window dimensions.  The encoder may
        // explode if given "strange" dimensions, e.g. a width that is not a multiple
        // of 16.  We can box it as needed to preserve dimensions.
        final int BIT_RATE = 4000000;   // 4Mbps
        final int VIDEO_WIDTH = 1280;
        final int VIDEO_HEIGHT = 720;
        int windowWidth = mWindowSurface.getWidth();
        int windowHeight = mWindowSurface.getHeight();
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
        int offX = (VIDEO_WIDTH - outWidth) / 2;
        int offY = (VIDEO_HEIGHT - outHeight) / 2;
        mVideoRect.set(offX, offY, offX + outWidth, offY + outHeight);
        //Log.d(TAG, "Adjusting window " + windowWidth + "x" + windowHeight + " to +" + offX + ",+" + offY + " " + mVideoRect.width() + "x" + mVideoRect.height());

        VideoEncoderCore encoderCore;
        try {
            encoderCore = new VideoEncoderCore(VIDEO_WIDTH, VIDEO_HEIGHT,
                    BIT_RATE, mOutputFile);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mInputWindowSurface = new WindowSurface(mEglCore, encoderCore.getInputSurface(), true);
        mVideoEncoder = new TextureMovieEncoder2(encoderCore);
    }


    public void stopEncoder() {
        if (mVideoEncoder != null) {
            Log.d(TAG, "stopping recorder, mVideoEncoder=" + mVideoEncoder);
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


    private void update(long timeStampNanos) {

        // Compute time from previous frame.
        long intervalNanos;
        if (mPrevTimeNanos == 0) {
            intervalNanos = 0;
        } else {
            intervalNanos = timeStampNanos - mPrevTimeNanos;

            final long ONE_SECOND_NANOS = 1000000000L;
            if (intervalNanos > ONE_SECOND_NANOS) {
                // A gap this big should only happen if something paused us.  We can
                // either cap the delta at one second, or just pretend like this is
                // the first frame and not advance at all.
                Log.d(TAG, "Time delta too large: " +
                        (double) intervalNanos / ONE_SECOND_NANOS + " sec");
                intervalNanos = 0;
            }
        }
        mPrevTimeNanos = timeStampNanos;

        final float ONE_BILLION_F = 1000000000.0f;
        final float elapsedSeconds = intervalNanos / ONE_BILLION_F;

        // Spin the triangle.  We want one full 360-degree rotation every 3 seconds,
        // or 120 degrees per second.
        final int SECS_PER_SPIN = 3;
        float angleDelta = (360.0f / SECS_PER_SPIN) * elapsedSeconds;

        // Bounce the rect around the screen.  The rect is a 1x1 square scaled up to NxN.
        // We don't do fancy collision detection, so it's possible for the box to slightly
        // overlap the edges.  We draw the edges last, so it's not noticeable.
        float xpos = mRect.getPositionX();
        float ypos = mRect.getPositionY();
        float xscale = mRect.getScaleX();
        float yscale = mRect.getScaleY();
        xpos += mRectVelX * elapsedSeconds;
        ypos += mRectVelY * elapsedSeconds;

        if ((mRectVelX < 0 && xpos - xscale / 2 < mInnerLeft) ||
                (mRectVelX > 0 && xpos + xscale / 2 > mInnerRight + 1)) {
            mRectVelX = -mRectVelX;
        }
        if ((mRectVelY < 0 && ypos - yscale / 2 < mInnerBottom) ||
                (mRectVelY > 0 && ypos + yscale / 2 > mInnerTop + 1)) {
            mRectVelY = -mRectVelY;
        }
        mRect.setPosition(xpos, ypos);
    }


    private void prepareFramebuffer(int width, int height) {
        GlUtil.checkGlError("prepareFramebuffer start");

        int[] values = new int[1];

        // Create a texture object and bind it.  This will be the color buffer.
        GLES20.glGenTextures(1, values, 0);
        GlUtil.checkGlError("glGenTextures");
        mOffscreenTexture = values[0];   // expected > 0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture);
        GlUtil.checkGlError("glBindTexture " + mOffscreenTexture);

        // Create texture storage.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Set parameters.  We're probably using non-power-of-two dimensions, so
        // some values may not be available for use.
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("glTexParameter");

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        GlUtil.checkGlError("glGenFramebuffers");
        mFramebuffer = values[0];    // expected > 0
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
        GlUtil.checkGlError("glBindFramebuffer " + mFramebuffer);

        // Create a depth buffer and bind it.
        GLES20.glGenRenderbuffers(1, values, 0);
        GlUtil.checkGlError("glGenRenderbuffers");
        mDepthBuffer = values[0];    // expected > 0
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffer);
        GlUtil.checkGlError("glBindRenderbuffer " + mDepthBuffer);

        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                width, height);
        GlUtil.checkGlError("glRenderbufferStorage");

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, mDepthBuffer);
        GlUtil.checkGlError("glFramebufferRenderbuffer");
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mOffscreenTexture, 0);
        GlUtil.checkGlError("glFramebufferTexture2D");

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        // Switch back to the default framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GlUtil.checkGlError("prepareFramebuffer done");
    }
}
