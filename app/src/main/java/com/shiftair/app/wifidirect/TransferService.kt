package com.shiftair.app.wifidirect

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * TransferService: inashughulikia kutuma na kupokea faili halisi
 * kupitia socket ya TCP juu ya mtandao wa Wi-Fi Direct (si intaneti).
 *
 * - Kifaa kilicho Group Owner: kinafungua ServerSocket na kusubiri faili (RECEIVE)
 * - Kifaa kingine: kinaunganisha kwa IP ya group owner na kutuma faili (SEND)
 */
class TransferService : Service() {

    companion object {
        const val PORT = 8988
        const val ACTION_SEND = "com.shiftair.app.action.SEND_FILE"
        const val ACTION_RECEIVE = "com.shiftair.app.action.RECEIVE_FILE"
        const val EXTRA_FILE_URI = "extra_file_uri"
        const val EXTRA_HOST_ADDRESS = "extra_host_address"
        private const val TAG = "ShiftAirTransfer"

        var progressListener: ((sent: Long, total: Long) -> Unit)? = null
        var completionListener: ((success: Boolean, savedPath: String?) -> Unit)? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND -> {
                val uriStr = intent.getStringExtra(EXTRA_FILE_URI)
                val host = intent.getStringExtra(EXTRA_HOST_ADDRESS)
                if (uriStr != null && host != null) {
                    Thread { sendFile(Uri.parse(uriStr), host) }.start()
                }
            }
            ACTION_RECEIVE -> {
                Thread { receiveFile() }.start()
            }
        }
        return START_NOT_STICKY
    }

    private fun sendFile(fileUri: Uri, hostAddress: String) {
        try {
            val resolver = contentResolver
            val fileName = queryFileName(fileUri) ?: "shiftair_file"
            val pfd = resolver.openFileDescriptor(fileUri, "r") ?: return
            val totalSize = pfd.statSize
            val input = FileInputStream(pfd.fileDescriptor)

            Socket().use { socket ->
                socket.connect(InetSocketAddress(hostAddress, PORT), 15000)
                val out = java.io.DataOutputStream(socket.getOutputStream())
                // Header rahisi: jina la faili + urefu wake, kisha data ghafi
                out.writeUTF(fileName)
                out.writeLong(totalSize)

                val buffer = ByteArray(8192)
                var sent = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    sent += read
                    progressListener?.invoke(sent, totalSize)
                }
                out.flush()
            }
            input.close()
            completionListener?.invoke(true, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Kutuma faili kumeshindikana", e)
            completionListener?.invoke(false, null)
        }
    }

    private fun receiveFile() {
        try {
            ServerSocket(PORT).use { serverSocket ->
                serverSocket.reuseAddress = true
                val client = serverSocket.accept()
                val input = java.io.DataInputStream(client.getInputStream())

                val fileName = input.readUTF()
                val totalSize = input.readLong()

                val downloadsDir = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "ShiftAir"
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val outFile = File(downloadsDir, fileName)

                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    var received = 0L
                    var read: Int
                    while (received < totalSize &&
                        input.read(buffer).also { read = it } != -1
                    ) {
                        output.write(buffer, 0, read)
                        received += read
                        progressListener?.invoke(received, totalSize)
                    }
                }
                client.close()
                completionListener?.invoke(true, outFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kupokea faili kumeshindikana", e)
            completionListener?.invoke(false, null)
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }
}

/** Kianzishi rahisi cha kutuma amri kwenye TransferService */
object TransferHelper {
    fun startSend(context: Context, fileUri: Uri, hostAddress: String) {
        val intent = Intent(context, TransferService::class.java).apply {
            action = TransferService.ACTION_SEND
            putExtra(TransferService.EXTRA_FILE_URI, fileUri.toString())
            putExtra(TransferService.EXTRA_HOST_ADDRESS, hostAddress)
        }
        context.startService(intent)
    }

    fun startReceive(context: Context) {
        val intent = Intent(context, TransferService::class.java).apply {
            action = TransferService.ACTION_RECEIVE
        }
        context.startService(intent)
    }
}
