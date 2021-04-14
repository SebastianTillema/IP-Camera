package com.example.ipcam.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class CameraController(private val context: Context) {
    private val TAG = "THIS_IS_MY_TAG"

    var size = Size(1920, 1080)
    var format = ImageFormat.JPEG


    private val imageQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(4)
    private val socketThread = Thread(SendImageTCP(imageQueue))

    private var outPutSurfaces: MutableList<Surface>

    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mBackgroundHandlerThread: HandlerThread
    private lateinit var cameraBackgroundHandler: Handler
    private lateinit var mPreviewCaptureSession: CameraCaptureSession
    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder

    private val mCameraStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
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

    init {
        outPutSurfaces = mutableListOf(initImageReaderSurface())
        setupCamera()

        socketThread.start()
    }

    private fun setupCamera(): Boolean {
        val cameraID = getCameraId() ?: return false
        return connectCamera(cameraID)
    }

    private fun getCameraId(): String? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (cameraID in cameraManager.cameraIdList) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID)
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraID
            }
        }
        return null
    }

    private fun connectCamera(cameraID: String): Boolean {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (!getCameraPermission()) return false
        }
        mBackgroundHandlerThread = HandlerThread("cameraBackgroundHandler")
        mBackgroundHandlerThread.start()
        cameraBackgroundHandler = Handler(mBackgroundHandlerThread.looper)

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraID, mCameraStateCallback, cameraBackgroundHandler)
        return true
    }

    private fun startPreview() {
        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        for (surface in outPutSurfaces) {
            mCaptureRequestBuilder.addTarget(surface)
        }

        mCameraDevice.createCaptureSession(outPutSurfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mPreviewCaptureSession = session
                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, cameraBackgroundHandler)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.d(TAG, "error on configure")
            }
        }, null)
    }

    private fun initImageReaderSurface(): Surface {
        val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
        val imageReaderHandler = Handler(imageReaderThread.looper)
        val mImageReader = ImageReader.newInstance(size.width, size.height, format, 1)

        mImageReader.setOnImageAvailableListener({ reader ->
            val image: Image = reader.acquireNextImage()
            sendImageOnSocket(image)
            image.close()
        }, imageReaderHandler)

        return mImageReader.surface
    }

    private var lastTimestamp = 0L
    private fun sendImageOnSocket(image: Image) {
        val buffer: ByteBuffer = image.planes[0].buffer // jpeg only uses a single plane
        val bytes = ByteArray(buffer.capacity())
        buffer[bytes]

        val timestamp = image.timestamp
        //Log.d(TAG, "frame rate " + 1000 / ((timestamp - lastTimestamp) / 1000000) + " fps")
        lastTimestamp = timestamp
        imageQueue.offer(bytes)
    }


    private fun getCameraPermission(): Boolean {
        // TODO
        return false
    }

    fun stopCapture() {
        // TODO
        mPreviewCaptureSession.stopRepeating()
    }
    fun startCapture() {
        // TODO
    }
    fun destroy() {
        socketThread.interrupt()
//        mPreviewCaptureSession.close()
//        mCameraDevice.close()
    }



    /**
     * This is only for checking encoding overhead of image format
     * jpeg encoding for 'Exynos 8 Octa 8890 processor' is 0?
     */
    private fun checkEncodingOfImage(cameraCharacteristics: CameraCharacteristics) {
        val configs: StreamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val format = ImageFormat.JPEG
        val sizes = configs.getOutputSizes(format)
        for (s in sizes) {
            val res = configs.getOutputStallDuration(format, s)
            //Log.d(TAG, "stall duration; size $s time $res")
        }
    }
}