package com.fevly.kasuaristream
/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fevly.kasuaristream.cam.CamUtility
import com.fevly.kasuaristream.cam.CameraInspector
import com.fevly.kasuaristream.threading.BackgroundThread
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var cameraId: String
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size
    private lateinit var videoSize: Size
    private  val CAMERA_REQUEST_RESULT = 123 // You can choose any integer value
    private lateinit var camUtility: CamUtility
    private lateinit var backThread: BackgroundThread
    private lateinit var backThreadHandler: Handler;
    private lateinit var cameraInspector: CameraInspector;
    private var shouldProceedWithOnResume: Boolean = true

    private lateinit var mediaRecorder: MediaRecorder
    private var isRecording: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // belum  dites ke android 10 kebawah
        requestPermissionsIfNotGranted()

        //handler thread sudah dispawn otomotas sewaktu instance thread ini hidup..
        backThread = BackgroundThread("Camera task...");
        backThread.start();
        backThreadHandler = backThread.backgroundHandler

        findViewById<Button>(R.id.start).apply {
            setOnClickListener {
                if (isRecording) {
                    mediaRecorder.stop()
                    mediaRecorder.reset()
                } else {
                    mediaRecorder = MediaRecorder()
                    setupMediaRecorder()
                    startRecording()
                }
                isRecording = !isRecording
            }
        }


    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()


        backThread = BackgroundThread("Camera task...");
        backThread.start();

        if (textureView.isAvailable && shouldProceedWithOnResume) {
            setupCamera()
        } else if (!textureView.isAvailable) {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
        shouldProceedWithOnResume = !shouldProceedWithOnResume
    }

    private fun setupCamera() {

        cameraInspector = CameraInspector(this)
        cameraInspector.prepareCameraSetting(backThread.backgroundHandler)

        cameraId = cameraInspector.cameraId
        previewSize = cameraInspector.preview
        videoSize = cameraInspector.videoSize
        imageReader = cameraInspector.imageReader

    }

    private fun requestPermissionsIfNotGranted() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                CAMERA_REQUEST_RESULT // Use a constant for request code
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            surfaceTextureListener.onSurfaceTextureAvailable(
                textureView.surfaceTexture!!,
                textureView.width,
                textureView.height
            )
        } else {
            Toast.makeText(
                this,
                "You need permission to run this",
                Toast.LENGTH_LONG
            )
                .show()
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", this.packageName, null)
                startActivity(intent)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        cameraManager.openCamera(cameraId, cameraStateCallback, backThread.backgroundHandler)
    }

    private fun setupMediaRecorder() {
        camUtility = CamUtility(this, mediaRecorder)
    }

    private fun startRecording() {
        val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture


        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)

        // SurfaceTexture untuk preview
        val previewSurface: Surface = Surface(surfaceTexture)


        val recordingSurface: Surface = mediaRecorder.surface


        //CaptureRequest.Builder
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

        captureRequestBuilder.addTarget(previewSurface)

        captureRequestBuilder.addTarget(recordingSurface)

        try {
            cameraDevice.createCaptureSession(
                listOf(previewSurface, recordingSurface),
                captureStateVideoCallback,
                backThread.backgroundHandler
            )
            mediaRecorder.start()
        } catch (e: CameraAccessException) {

        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        @SuppressLint("MissingPermission")
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            requestPermissionsIfNotGranted()
            setupCamera()
            connectCamera()
        }
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        }
    }

    /**
     * Camera State Callbacks
     */

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface: Surface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(
                listOf(previewSurface, imageReader.surface),
                captureStateCallback,
                null
            )
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {

        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {

        }
    }

    /**
     * Capture State Callback
     */

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {

        }

        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session

            cameraCaptureSession.setRepeatingRequest(
                captureRequestBuilder.build(),
                null,
                backThread.backgroundHandler
            )
        }
    }

    private val captureStateVideoCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
        }

        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            try {
                cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), null,
                    backThread.backgroundHandler
                )
                mediaRecorder.start()
            } catch (e: CameraAccessException) {
                e.printStackTrace()

            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Capture Callback
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {

        }
    }

    /**
     * ImageAvailable Listener
     */
    val onImageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            Toast.makeText(this@MainActivity, "Photo Taken!", Toast.LENGTH_SHORT).show()
            val image: Image = reader.acquireLatestImage()
            image.close()
        }
    }

    /**
     * File Creation
     */

    private fun createFile(): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)

        var temp =
            File(getExternalFilesDir(null)?.toString() + "/" + "VID_${sdf.format(Date())}.mp4");
        return File(getExternalFilesDir(null)?.toString() + "/" + "VID_${sdf.format(Date())}.mp4")
    }
}