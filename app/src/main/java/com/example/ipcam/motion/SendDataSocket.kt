package com.example.ipcam.motion

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.BlockingQueue

class SendDataSocket(private val imageQueue: BlockingQueue<ByteArray>) : Runnable {

    override fun run() {
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket()

            val address =  InetAddress.getByName("192.168.1.74")
            while (!Thread.currentThread().isInterrupted) {
                val bytes = imageQueue.take()
                val packet = DatagramPacket(bytes, bytes.size, address, 19792)
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


