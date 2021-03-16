package com.example.ipcam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


class MainActivity : AppCompatActivity() {
    private val TAG = "THIS_IS_MY_TAG";

    // configs
    private val size = Size(1920, 1080)
    private val format = ImageFormat.JPEG

    private val imageQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(1)

    private lateinit var mCameraId: String
    private lateinit var mCameraDevice: CameraDevice
    private val mCameraStateCallback: StateCallback = object : StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "on opened")
            mCameraDevice = camera
            startPreview()
        }
        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice.close()
        }
        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDevice.close()
        }
    }

    private val socketThread = Thread(SendImageSocket(imageQueue))

    private lateinit var mBackgroundHandlerThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler

    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    private lateinit var mImageReader: ImageReader
    private lateinit var mPreviewCaptureSession: CameraCaptureSession
    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        socketThread.start()

        var last_ts = 0L

        mImageReader = ImageReader.newInstance(size.width, size.height, format, 1)
        mImageReader.setOnImageAvailableListener({ reader ->
            val image: Image = reader.acquireNextImage()
            val buffer: ByteBuffer = image.planes[0].buffer // jpeg only uses a single plane
            val bytes = ByteArray(buffer.capacity())
            buffer[bytes]

            val curr = image.timestamp
            Log.d(TAG, "frame rate " + 1000/((curr - last_ts) / 1000000) + " fps")
            last_ts = curr

            imageQueue.offer(bytes)
            image.close()
        }, imageReaderHandler)

        setupCamera()
        connectCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        socketThread.interrupt()
    }
    private fun setupCamera() {
        Log.d(TAG, "setup camera")
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                mCameraId = cameraId
                return
            }
        }
    }

    private fun connectCamera() {
        Log.d(TAG, "connect camera")
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mBackgroundHandlerThread = HandlerThread("mBackgroundHandlerThread")
        mBackgroundHandlerThread.start()
        mBackgroundHandler = Handler(mBackgroundHandlerThread.looper)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No Permission!")
            return
        }
        cameraManager.openCamera(mCameraId, mCameraStateCallback, mBackgroundHandler)
    }

    private fun startPreview() {
        Log.d(TAG, "start preview")

        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        val mTextureView: TextureView = findViewById<TextureView>(R.id.textureViewIdTest)
        while (!mTextureView.isAvailable) {
        }
        val surfaceTexture: SurfaceTexture = mTextureView.surfaceTexture!!
        val previewSurface = Surface(surfaceTexture)

        mCaptureRequestBuilder.addTarget(previewSurface)
        mCaptureRequestBuilder.addTarget(mImageReader.surface)

        mCameraDevice.createCaptureSession(
            listOf(previewSurface, mImageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG, "onConfigured: startPreview")
                    mPreviewCaptureSession = session
                    mPreviewCaptureSession.setRepeatingRequest(
                        mCaptureRequestBuilder.build(),
                        null,
                        mBackgroundHandler
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(TAG, "onConfigureFailed: startPreview")
                }
            }, null
        )
    }

    /**
     * This is only for checking encoding overhead of image format
     * jpeg encoding for 'Exynos 8 Octa 8890 processor' is 0?
     */
    private fun checkEncodingOfImage(cameraCharacteristics: CameraCharacteristics) {
        val configs: StreamConfigurationMap = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )!!
        val format = ImageFormat.JPEG
        val sizes = configs.getOutputSizes(format)
        for (s in sizes) {
            val res = configs.getOutputStallDuration(format, s)
            Log.d(TAG, "stall duration; size $s time $res")
        }
    }
}