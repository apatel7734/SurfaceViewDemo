package com.avgtechie.glsurfaceview;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.avgtechie.surfaceviewdemo.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ashish on 10/26/14.
 */
public class DemoSurfaceViewRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "DemoSurfaceViewRenderer";
    private float mRed;
    private float mGreen;
    private float mBlue;
    private SurfaceHolder surfaceHolder;

    // Our matrices
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    // Geometric variables
    public static float vertices[];
    public static short indices[];

    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;

    public static float uvs[];
    public static FloatBuffer uvBuffer;

    // Our screenresolution
    float mScreenWidth = 1280;
    float mScreenHeight = 768;

    // Misc
    Context mContext;
    long mLastTime;

    //**************************************************
    //Video Player code
    //**************************************************
    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private MediaPlayer mMediaPlayer;
    private boolean updateSurface;
    private SurfaceTexture mSurfaceTexture;
    private int mProgram;
    private int mTextureHandle;
    private int mPositionHandler;
    private int mTextureID;
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f,
    };

    private FloatBuffer mTriangleVertices;

    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private final String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";


    public DemoSurfaceViewRenderer(SurfaceHolder sh, Context context) throws IOException {
        surfaceHolder = sh;
        mContext = context;
        mLastTime = System.currentTimeMillis() + 100;
        mMediaPlayer = new MediaPlayer();
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.testvideo);
        mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
        Matrix.setIdentityM(mSTMatrix, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        //step 2: surface created now setup Shader and Program
        setupShaderProgram();
        setupVideoShaderProgram();
    }

    /**
     * do all initialize about window size here.
     *
     * @param gl
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged");

        mScreenWidth = width;
        mScreenHeight = height;
        //setup all the shapes to draw during draw() method call
        setupTriangle();

        Log.d(TAG, String.format("mScreenWidth = %f,mScreenHeight = %f", mScreenWidth, mScreenHeight));

        //redo the viewport making it full screen
        GLES20.glViewport(0, 0, (int) mScreenWidth, (int) mScreenHeight);

        //clear out matrices
        for (int i = 0; i < 16; i++) {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        //setup screenwidth and screenheight for normal sprite translation
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);

        //setup the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        //calculate project and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //Log.d(VideoSurfaceViewActivity.TAG, "onDrawFrame");
        //first get the current time
        long now = System.currentTimeMillis();

        //make sure call is valid
        if (mLastTime > now) {
            return;
        }

        //time last frame took to draw stuff
        long elapsed = now = mLastTime;

        draw(mtrxProjectionAndView);
        //update the example
        drawVideo();

        //mark now as last
        //mark now as last
        mLastTime = now;
    }

    private void drawVideo() {
        synchronized (this) {
            if (updateSurface) {
                mSurfaceTexture.updateTexImage();
                mSurfaceTexture.getTransformMatrix(mSTMatrix);
                updateSurface = false;
            }
            //set the background color
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            //clear and redraw background color
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(mPositionHandler, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            //checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(mPositionHandler);
            //checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(mTextureHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            //checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(mTextureHandle);
            //checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            //checkGlError("glDrawArrays");

            GLES20.glFinish();

        }
    }


    public void draw(float[] mtrxProjectionAndView) {

        GLES20.glUseProgram(ShaderGraphicTool.sp_SolidColor);

        //First clear the screen and buffer to redraw stuff, we've set the clear color as black
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // get handle to Vertex Shader's vPosition
        int mPositionHandle = GLES20.glGetAttribLocation(ShaderGraphicTool.sp_SolidColor, "vPosition");

        //enable generic vertex attribute
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        //prepare triangle coordinate data to draw
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);


        //Get handle to shape 's transformation matrix
        int mtrxhandle = GLES20.glGetUniformLocation(ShaderGraphicTool.sp_SolidColor, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, mtrxProjectionAndView, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public void setColor(float r, float g, float b) {
        Log.d(TAG, "setColor");
        mRed = r;
        mGreen = g;
        mBlue = b;
    }


    public void onPause() {
        //do nothing for now.
    }

    public void onResume() {
        mLastTime = System.currentTimeMillis();
    }

    private void setupTriangle() {

        //vertices for our triangle bottom left corner is (0,0) and top right corner is m(ScreenWidth , mScreenHeight)
        vertices = new float[]{
                mScreenWidth / 2.0f, (mScreenHeight / 2.0f) + 100f, 0.0f,
                mScreenWidth / 4.0f, (mScreenHeight / 2.0f) - 100f, 0.0f,
                mScreenWidth / 4.0f + mScreenWidth / 2.0f, (mScreenHeight / 2.0f) - 100f, 0.0f,
                mScreenWidth / 2.0f, mScreenHeight / 4.0f, 0.0f
        };
        //order of vertex rendering to draw triangle
        indices = new short[]{0, 1, 2, 0, 2, 3};

        //vertices buffer
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

    }

    private void setupVideoShaderProgram() {

        mProgram = createProgram(mVertexShader, mFragmentShader);

        if (mProgram == 0) {
            return;
        }
        //store globally if needed later
        mPositionHandler = GLES20.glGetAttribLocation(mProgram, "aPosition");
        if (mPositionHandler == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        //store globally if needed later
        mTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        if (mTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetAttribLocation(mProgram, "uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            //throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        //checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            //throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        //store globally if needed
        mTextureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        Surface mSurface = new Surface(mSurfaceTexture);
        mMediaPlayer.setSurface(mSurface);
        mMediaPlayer.setScreenOnWhilePlaying(true);
        mSurface.release();

        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (this) {
            updateSurface = false;
        }
        mMediaPlayer.start();
    }

    //create Vertex and Fragment shaders
    private void setupShaderProgram() {
        //1. Vertex shader
        int vertexShader = ShaderGraphicTool.loadShader(GLES20.GL_VERTEX_SHADER, ShaderGraphicTool.vs_SolidColor);

        //2. Fragment shader
        int fragmentShader = ShaderGraphicTool.loadShader(GLES20.GL_FRAGMENT_SHADER, ShaderGraphicTool.fs_SolidColor);

        //create empty openGL ES program
        ShaderGraphicTool.sp_SolidColor = GLES20.glCreateProgram();

        //add vertex shader to program
        GLES20.glAttachShader(ShaderGraphicTool.sp_SolidColor, vertexShader);

        //add fragment shader to program
        GLES20.glAttachShader(ShaderGraphicTool.sp_SolidColor, fragmentShader);

        //create OpenGL ES program executable
        GLES20.glLinkProgram(ShaderGraphicTool.sp_SolidColor);

        //set our shader program
        GLES20.glUseProgram(ShaderGraphicTool.sp_SolidColor);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            updateSurface = true;
        }
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    //Creates the program for shaders
    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            //checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            //checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }
}
