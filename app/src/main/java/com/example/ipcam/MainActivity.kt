package com.example.ipcam

import android.content.pm.ActivityInfo
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.example.ipcam.camera.CameraController
import com.example.ipcam.camera.SendImageTCP
import com.example.ipcam.camera.SendImageUDP
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class MainActivity : AppCompatActivity() {
    private val TAG = "THIS_IS_MY_TAG"

    // configs
    private val size = Size(1920, 1080)
    private val format = ImageFormat.JPEG

    private lateinit var mCameraController: CameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lock the orientation to portrait (for now)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

//        val previewSurface = getPreviewSurface()
//        val surface = initImageReaderSurface()
        mCameraController = CameraController(this)
        //Accelerometer(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraController.destroy()

    }
}