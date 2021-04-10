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

    private val imageQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(4)
    private val socketThread = Thread(SendImageTCP(imageQueue))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lock the orientation to portrait (for now)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        val surface = initImageReaderSurface()
        val cameraController = CameraController(this, listOf(surface))
        socketThread.start()

        //Accelerometer(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        socketThread.interrupt()
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

    private fun getPreviewSurface(): Surface {
        val mTextureView: TextureView = findViewById<TextureView>(R.id.textureViewIdTest)
        while (!mTextureView.isAvailable) {
        } // TODO better options than busy waiting

        val surfaceTexture: SurfaceTexture = mTextureView.surfaceTexture!!
        return Surface(surfaceTexture)
    }
}