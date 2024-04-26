package com.fevly.kasuaristream.constants;
/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */
public class ShaderAndCoordConstants {

    public static String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    public static String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

   public static float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // bottom-left
            1.0f, -1.0f,   // bottom-right
            -1.0f, 1.0f,   //  top-left
            1.0f, 1.0f,   //  top-right
    };

 public   static float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     //  bottom-left
            1.0f, 0.0f,     // bottom-right
            0.0f, 1.0f,     // top-left
            1.0f, 1.0f      // top-right
    };
}
