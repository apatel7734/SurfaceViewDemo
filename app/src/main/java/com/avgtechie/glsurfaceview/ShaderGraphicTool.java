package com.avgtechie.glsurfaceview;

import android.opengl.GLES20;

/**
 * Created by ashish on 10/27/14.
 */
public class ShaderGraphicTool {

    public static int sp_SolidColor;

    /* SHADER Solid
     *
     * This shader is for rendering a colored primitive.
     *
     */
    //vertex Solid color
    public static final String vs_SolidColor =
            "uniform mat4 uMVPMatrix;" +
                    "attribute  vec4        vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    //fragment Solid color
    public static final String fs_SolidColor =
            "precision mediump float;" +
                    "void main() {" +
                    "  gl_FragColor = vec4(0.5,0,0,1);" +
                    "}";

    public static int loadShader(int type, String shaderCode) {

        //create shader by type e.g
        //1. GLES20.GL_VERTEX_SHADER;
        //2. GLES20.GL_FRAGMENT_SHADER;
        int shader = GLES20.glCreateShader(type);

        //add sourcecode to shader and compile it.
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
}
