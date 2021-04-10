package com.example.ipcam.camera

import android.util.Log
import java.io.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.BlockingQueue

class SendImageUDP(private val imageQueue: BlockingQueue<ByteArray>) : Runnable {
    override fun run() {
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket()

            val address =  InetAddress.getByName("192.168.1.74")
            while (!Thread.currentThread().isInterrupted) {
                val bytes = imageQueue.take()
                Log.d("THIS_", "send something! " + bytes.size)
                val packet = DatagramPacket(bytes, bytes.size, address, 9782)
                socket.send(packet)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finally {
            socket?.close()
        }
    }
}


