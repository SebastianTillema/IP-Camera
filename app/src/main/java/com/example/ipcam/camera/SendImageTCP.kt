package com.example.ipcam.camera

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.BlockingQueue

class SendImageTCP(private val imageQueue: BlockingQueue<ByteArray>) : Runnable {

    override fun run() {
        var socket: Socket? = null
        var dataOutputStream: DataOutputStream? = null
        var outputStream: OutputStream? = null

        try {
            socket = Socket("192.168.1.74", 9782)
            outputStream = socket.getOutputStream()
            dataOutputStream = DataOutputStream(outputStream)

            var bytes: ByteArray;
            while (!Thread.currentThread().isInterrupted) {
                bytes = imageQueue.take()
                Log.d("THIS_", "size " + bytes.size)
                Log.d("THIS_", "data size" + bytes.size)
                Log.d("THIS_", "data" + bytesToHex(bytes))
                outputStream.write(intToByteArray(bytes.size))
                outputStream.write(bytes)

                //dataOutputStream.writeInt(bytes.size)
                //dataOutputStream.write(bytes)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            outputStream?.close()
            //dataOutputStream?.close()
            socket?.close()
        }
    }

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String? {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v: Int = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

    fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte())
    }
}


