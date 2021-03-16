package com.example.ipcam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat

class CameraController(private val context: Context, private val outPutSurfaces: List<Surface>) {

    var size = Size(1920, 1080)
    var format = ImageFormat.JPEG

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
        setupCamera()
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
        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        for (surface in outPutSurfaces) {
            mCaptureRequestBuilder.addTarget(surface)
        }

        mCameraDevice.createCaptureSession(outPutSurfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mPreviewCaptureSession = session
                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, cameraBackgroundHandler)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                // TODO
            }
        }, null)
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