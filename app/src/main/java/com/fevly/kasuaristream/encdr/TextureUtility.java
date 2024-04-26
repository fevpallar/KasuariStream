package com.fevly.kasuaristream.encdr;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.fevly.kasuaristream.constants.ShaderAndCoordConstants;

import java.nio.FloatBuffer;


public class TextureUtility {
    private static final String TAG = FloatByteBufferAllocator.TAG;

    public enum ProgramType {
        TEXTURE_EXT}

    public static final int KERNEL_SIZE = 9;

    private ProgramType mProgramType;

    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private int mTextureTarget;

    private float[] mKernel = new float[KERNEL_SIZE];
    private float[] mTexOffset;
    private float mColorAdjust;


    public TextureUtility(ProgramType programType) {
        mProgramType = programType;

        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        mProgramHandle = FloatByteBufferAllocator.createProgram(ShaderAndCoordConstants.VERTEX_SHADER, ShaderAndCoordConstants.FRAGMENT_SHADER_EXT);

        // init. fields di VERTEX_SHADER dan FRAGMENT_SHADER_EXT
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");

            // kalau punya kernal maka punya tex offset and color adj
            muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
            muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
    }


    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }


}
