package com.avgtechie.glsurfaceview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.SurfaceHolder;

import com.avgtechie.surfaceviewdemo.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ashish on 10/26/14.
 */
public class DemoSurfaceViewRenderer implements GLSurfaceView.Renderer {

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
    int mProgram;

    public DemoSurfaceViewRenderer(SurfaceHolder sh, Context context) {
        surfaceHolder = sh;
        mContext = context;
        mLastTime = System.currentTimeMillis() + 100;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        //setup clear color to gray
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1);
        setupShaderProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged");

        mScreenWidth = width;
        mScreenHeight = height;

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

        //update the example

        //draw example
        draw(mtrxProjectionAndView);

        //mark now as last
        mLastTime = now;
    }


    public void draw(float[] mtrxProjectionAndView) {
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

    private void setupImage() {
        uvs = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };
        //texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        //generate texture if more needed, alter these numbers
        int[] texturenames = new int[1];
        GLES20.glGenTextures(1, texturenames, 0);

        //temporary create bitmap from image
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

        //bind texture to texturename
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);


        //load bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        //we're done using it so recycle it
        bitmap.recycle();


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
}
