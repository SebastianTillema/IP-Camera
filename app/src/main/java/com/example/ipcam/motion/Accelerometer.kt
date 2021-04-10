package com.example.ipcam.motion
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


class Accelerometer(private val context: Context) : SensorEventListener {
    private val TAG = "THIS_IS_MY_TAG"

    private val dataQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(4)
    private val socketThread = Thread(SendDataSocket(dataQueue))

    private val mRotationMatrix = FloatArray(16)
    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensorAccelerometer: Sensor
    private lateinit var mSensorRotation: Sensor


    init {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mSensorRotation, SensorManager.SENSOR_DELAY_FASTEST)

        socketThread.start()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0];
            val ay = event.values[1];
            val az = event.values[2];
            //Log.d(TAG, "onSensorChanged x: $ax, y: $ay, z $az")
        }
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values)
            val floatMatrix = mRotationMatrix.toTypedArray().toFloatArray()
            val bytes = floatArray2ByteArray(floatMatrix)

            dataQueue.offer(bytes)
            //Log.d(TAG, mRotationMatrix.contentToString())
        }
    }


    private fun floatArray2ByteArray(values: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(4 * values.size)
        for (value in values) {
            buffer.putFloat(value)
        }
        return buffer.array()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged: $accuracy")
    }
}