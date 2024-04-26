package com.fevly.kasuaristream;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.fevly.kasuaristream.encdr.FrameRect;
import com.fevly.kasuaristream.encdr.TextureUtility;
import com.fevly.kasuaristream.encdr.VideoEncoder;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class CameraSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "Kasuaristream";
    private static final boolean VERBOSE = false;

    private MainActivity.CameraHandler mCameraHandler;
    private VideoEncoder mVideoEncoder;
    private File mOutputFile;

    private FrameRect frameWindow;

    private final float[] mSTMatrix = new float[16];
    private int mTextureId;

    private SurfaceTexture mSurfaceTexture;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private int mFrameCount;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    // width/height dari  incoming prview
    static boolean mIncomingSizeUpdated;
    static int mIncomingWidth;
    static int mIncomingHeight;



    /*
     * ==========================================================
     * Note   output file dari encoded video di passing ke  video  encoder
     * ==========================================================
     */
    public CameraSurfaceRenderer(MainActivity.CameraHandler cameraHandler,
                                 VideoEncoder vidEncoder, File outputFile) {
        mCameraHandler = cameraHandler;
        mVideoEncoder = vidEncoder;
        mOutputFile = outputFile;
        mTextureId = -1;
        mRecordingStatus = -1;
        mRecordingEnabled = false;
        mFrameCount = -1;
        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;
    }


    public void changeRecordingState(boolean isRecording) {
        mRecordingEnabled = isRecording;
    }



    // #2
    // terinvoke MELALUI glSurfaceView.onResume()

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d("flow", "onSurfaceCreated");
        // 1st time hit = false
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }


        // note bukan untuk  fase recording, recording punya shader sendiri
        TextureUtility texture2dProgram = new TextureUtility(TextureUtility.ProgramType.TEXTURE_EXT);
        frameWindow = new FrameRect(
                texture2dProgram
        );


        //array will store 1 texture object IDs
        int[] textures = new int[1];
        //generates a texture object names and stores it  'in the textures'
        GLES20.glGenTextures(1, textures, 0);



        //ext. that support for external textures with the OES extension
        int mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

        int texId = textures[0];

        // bind 'texture' dgn external texture (mTextureTarget,  not managed by the OpenGL context)
        // texture external ini dtg dari camera/video frames
        GLES20.glBindTexture(mTextureTarget, texId);


        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);


        mTextureId = texId;

        // preview surfacenya (ingat juga TextureView punya internal SurfaceTexture )
        // preview surface diset up berdasarkan texture yg sudah ter-bind dng extrnal texture
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // perhatikan ada pass 'mSurfaceTexture'
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                MainActivity.CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
    }


    // #5
    // diinvoked setiap kali ada frame baru
    // diinvoked setelah  glSurfaceView.requestRender();
    @Override
    public void onDrawFrame(GL10 unused) {
        Log.d("flow", "onDrawFrame");

        // panggil di SurfaceTexture untuk update texture image
        // dgn most recent frame
        mSurfaceTexture.updateTexImage();


        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");

                    VideoEncoder.EncoderConfig config =

                            new VideoEncoder.EncoderConfig(
                                    mOutputFile, 640, 480, 1000000, EGL14.eglGetCurrentContext());

                    // start recording
                    //MSG_START_RECORDING firing didisini
                    try {
                        mVideoEncoder.startRecording(config);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    mVideoEncoder.refreshEGLSurface(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    break;
            }
        } else {

            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
            }
        } //ends else


        //#6
        // MASUK SINI KALAU TOMBOL BELUM  DITEKAN
        mVideoEncoder.setTextureId(mTextureId);

        mVideoEncoder.frameAvailable(mSurfaceTexture);

        if (mIncomingSizeUpdated) {
            // MASUK SINI KALAU TOMBOL BELUM  DITEKAN
            //#8
            mIncomingSizeUpdated = false;
        }

        // #9
        // Draw the video frame.
       // frameWindow init di onSurfaceCreated()
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        frameWindow.drawFrame(mTextureId, mSTMatrix);

    }


}