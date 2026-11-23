package com.shiftair.app.wifidirect

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class TransferService : Service() {

    companion object {
        const val PORT = 8988
        const val ACTION_SEND_FILE = "com.shiftair.app.ACTION_SEND_FILE"
        const val ACTION_START_SERVER = "com.shiftair.app.ACTION_START_SERVER"
        const val EXTRA_FILE_URI = "extra_file_uri"
        const val EXTRA_HOST_ADDRESS = "extra_host_address"

        const val BROADCAST_TRANSFER_PROGRESS = "com.shiftair.app.TRANSFER_PROGRESS"
        const val EXTRA_STATUS_MSG = "extra_status_msg"
        const val EXTRA_PROGRESS = "extra_progress"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVER -> {
                startReceiveServer()
            }
            ACTION_SEND_FILE -> {
                val uriString = intent.getStringExtra(EXTRA_FILE_URI)
                val host = intent.getStringExtra(EXTRA_HOST_ADDRESS)
                if (!uriString.isNullOrBlank() && !host.isNullOrBlank()) {
                    sendFile(Uri.parse(uriString), host)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startReceiveServer() {
        serviceScope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(PORT)
                emitProgress("Server ya kupokea iko tayari (Port $PORT)...", 0)

                val client = serverSocket!!.accept()
                emitProgress("Kifaa kimeunganishwa, inapokea faili...", 5)

                val input = DataInputStream(BufferedInputStream(client.getInputStream()))
                val fileName = input.readUTF()
                val fileSize = input.readLong()

                val outDir = File(getExternalFilesDir(null), "Downloads/ShiftAir")
                if (!outDir.exists()) outDir.mkdirs()

                val outFile = File(outDir, fileName)
                val fileOut = BufferedOutputStream(FileOutputStream(outFile))

                val buffer = ByteArray(8192)
                var totalRead = 0L
                var bytesRead: Int

                while (totalRead < fileSize && input.read(buffer).also { bytesRead = it } != -1) {
                    fileOut.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    val pct = if (fileSize > 0) ((totalRead * 100) / fileSize).toInt() else 100
                    emitProgress("Inapokea: $fileName ($pct%)", pct)
                }

                fileOut.flush()
                fileOut.close()
                input.close()
                client.close()
                serverSocket?.close()

                emitProgress("Imepokewa kikamilifu: ${outFile.absolutePath}", 100)
            } catch (e: Exception) {
                emitProgress("Hitilafu ya kupokea: ${e.message}", 0)
            }
        }
    }

    private fun sendFile(uri: Uri, hostAddress: String) {
        serviceScope.launch {
            val socket = Socket()
            try {
                emitProgress("Inaunganisha kwa mpokeaji ($hostAddress)...", 5)
                socket.connect(InetSocketAddress(hostAddress, PORT), 10000)

                val fileName = getFileName(uri)
                val fileSize = getFileSize(uri)

                val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                output.writeUTF(fileName)
                output.writeLong(fileSize)

                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Imeshindwa kufungua faili")

                val buffer = ByteArray(8192)
                var totalSent = 0L
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalSent += bytesRead
                    val pct = if (fileSize > 0) ((totalSent * 100) / fileSize).toInt() else 100
                    emitProgress("Inatuma: $fileName ($pct%)", pct)
                }

                output.flush()
                inputStream.close()
                output.close()
                socket.close()

                emitProgress("Faili imetumwa kikamilifu: $fileName", 100)
            } catch (e: Exception) {
                emitProgress("Hitilafu ya kutuma: ${e.message}", 0)
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "shiftair_file_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index != -1 && cursor.moveToFirst()) {
                size = cursor.getLong(index)
            }
        }
        return size
    }

    private fun emitProgress(message: String, progress: Int) {
        val intent = Intent(BROADCAST_TRANSFER_PROGRESS).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS_MSG, message)
            putExtra(EXTRA_PROGRESS, progress)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serviceScope.cancel()
    }
}
