package com.example.ipcam

import java.io.*
import java.net.Socket
import java.util.concurrent.BlockingQueue

class SendImageSocket(private val imageQueue: BlockingQueue<ByteArray>) : Runnable {

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
                dataOutputStream.writeInt(bytes.size)
                dataOutputStream.write(bytes)
            }
        } catch (e: IOException) {
            // TODO: Handle this
            e.printStackTrace()
        }
        finally {
            outputStream?.close()
            dataOutputStream?.close()
            socket?.close()
        }
    }
}


