package com.example.rtspcamera

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.rtspserver.RtspServerCamera2

class RtspService : Service(), ConnectChecker {

    companion object {
        var isRunning = false
    }

    private lateinit var rtspServerCamera2: RtspServerCamera2
    private val channelId = "rtsp_stream_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Passing Context (this) creates a headless SurfaceTexture for background streaming
        rtspServerCamera2 = RtspServerCamera2(this, this, 1935)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_STREAM") {
            stopStreamAndService()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("RTSP Server Active")
            .setContentText("Streaming camera headlessly on port 1935")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }

        if (!rtspServerCamera2.isStreaming) {
            if (rtspServerCamera2.prepareAudio(64000, 32000, false) &&
                rtspServerCamera2.prepareVideo(1920, 1080, 30, 4000 * 1024, 0)) {
                rtspServerCamera2.startStream()
                isRunning = true
            } else {
                stopStreamAndService()
            }
        }

        return START_STICKY
    }

    private fun stopStreamAndService() {
        if (rtspServerCamera2.isStreaming) {
            rtspServerCamera2.stopStream()
        }
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStreamAndService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "RTSP Stream", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ConnectChecker callbacks
    override fun onConnectionStarted(url: String) {}
    override fun onConnectionSuccess() {}
    override fun onConnectionFailed(reason: String) { stopStreamAndService() }
    override fun onNewBitrate(bitrate: Long) {}
    override fun onDisconnect() {}
    override fun onAuthError() {}
    override fun onAuthSuccess() {}
}