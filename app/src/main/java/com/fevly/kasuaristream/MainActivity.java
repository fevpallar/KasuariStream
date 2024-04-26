package com.fevly.kasuaristream;
/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.opengl.GLSurfaceView;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.Button;

import com.fevly.kasuaristream.encdr.VideoEncoder;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class MainActivity extends Activity
        implements SurfaceTexture.OnFrameAvailableListener {

    private boolean mRecordingEnabled;
    private int mCameraPreviewWidth, mCameraPreviewHeight;
    static Camera mCamera;
    private CameraHandler mCameraHandler; // Handler
    private static VideoEncoder sVideoEncoder = new VideoEncoder();

    private GLSurfaceView glSurfaceView;
    private CameraSurfaceRenderer mRenderer;
    @Override
    protected void onResume() {
        Log.d("flow", "onResume");


        super.onResume(); // enable kan
        updateControls();

        if (mCamera == null) {
            try {
                openCamera();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // INI YG TRIGGER ON SURFACE CREATED
        glSurfaceView.onResume();
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {

                  /*========================================================
                     pass lebar tinggi dari current preview.
                      mCameraPreviewWidth , mCameraPreviewHeight sudah diinit di openCamera
                      ========================================================*/
                CameraSurfaceRenderer.mIncomingWidth = mCameraPreviewWidth;
                CameraSurfaceRenderer.mIncomingHeight = mCameraPreviewHeight;
                CameraSurfaceRenderer.mIncomingSizeUpdated = true;

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // handler ini untuk terima pesan2 instruksi yg dikirim dari thread lain
        mCameraHandler = new CameraHandler(this);

        mRecordingEnabled = sVideoEncoder.isRecording();

        glSurfaceView = (GLSurfaceView) findViewById(R.id.surfacepreview);

        glSurfaceView.setEGLContextClientVersion(2); // versi GLES yg dipakai

        File outputFile = new File(getFilesDir(), "hasil.mp4");
        //start callbacknya (GLSurfaceView.Renderer)
        // dan trigger onSurfaceCreated
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, outputFile);

        // set rendernya ke viewnya GL
        glSurfaceView.setRenderer(mRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.FOCUSABLES_TOUCH_MODE);

    }


    private void openCamera() throws CameraAccessException {
        Log.d("flow", "openCamera");
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            mCamera = Camera.open();
        }

        Camera.Parameters parms = mCamera.getParameters();
        parms.setRecordingHint(true);
        mCamera.setParameters(parms);

        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);

        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
        } else if (display.getRotation() == Surface.ROTATION_270) {

            mCamera.setDisplayOrientation(180);
        }
    }

    public void clickedStartRecord(@SuppressWarnings("unused") View unused) {
        mRecordingEnabled = !mRecordingEnabled;
        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeRecordingState(mRecordingEnabled);
            }
        });
        updateControls();
    }


    private void updateControls() {
        Button recordButton = (Button) findViewById(R.id.record);
        int id = mRecordingEnabled ?
                R.string.recordingOff : R.string.recordingOn;
        recordButton.setText(id);
    }


    //#4
    // ditrigger setelah camera ' mCamera.startPreview();'
    //invoked  asynchronously by the SurfaceTexture itself when a new frame becomes available for rendering.
    @Override
    public void onFrameAvailable(SurfaceTexture st) {

        //request that the GLSurfaceView render a new frame.
        // INILAH YG INVOKE onDrameFrame setiap kali ada frame baru
        glSurfaceView.requestRender();
    }

    // Handler milik main thread
    static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<MainActivity> mWeakActivity;

        public CameraHandler(MainActivity activity) {
            mWeakActivity = new WeakReference<MainActivity>(activity);
        }


        //#3
        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;

            MainActivity activity = mWeakActivity.get();
            if (activity == null) {
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    // set OnFrameAvailableListener untuk SurfaceTexture
                    // sehingga JIKA ada frame nanti tersedia, maka invoke (onFrameAvailable)
                    ((SurfaceTexture) inputMessage.obj).setOnFrameAvailableListener(activity);
                    try {

                        // sets the SurfaceTexture object as the preview texture for the camera
                        // minta camera untuk output preview framesnya ke texture ini
                        MainActivity.mCamera.setPreviewTexture((SurfaceTexture) inputMessage.obj);
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                    mCamera.startPreview();
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    } // ends CameraHandler
}



