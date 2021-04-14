package com.example.ipcam.camera

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.BlockingQueue

class SendImageTCP(private val imageQueue: BlockingQueue<ByteArray>) : Runnable {
    private val TAG = "THIS_IS_MY_TAG"

    override fun run() {
        var socket: Socket? = null
        var outputStream: OutputStream? = null

        try {
            socket = Socket("192.168.1.74", 9783)
            outputStream = socket.getOutputStream()
            Log.d(TAG, "Connected and ready to send images")

            var bytes: ByteArray;
            while (!Thread.currentThread().isInterrupted) {
                bytes = imageQueue.take()
                outputStream.run {
                    write(intToByteArray(bytes.size))
                    write(bytes)
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
            e.printStackTrace()
        } finally {
            Log.d(TAG, "clean up socket")
            outputStream?.close()
            socket?.close()
        }
    }

//    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
//    fun bytesToHex(bytes: ByteArray): String? {
//        val hexChars = CharArray(bytes.size * 2)
//        for (j in bytes.indices) {
//            val v: Int = bytes[j].toInt() and 0xFF
//            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
//            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
//        }
//        return String(hexChars)
//    }

    fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())
    }
}


