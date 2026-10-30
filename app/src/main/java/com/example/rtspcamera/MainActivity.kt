package com.example.rtspcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartStop: Button
    private val PERMISSIONS_REQUEST_CODE = 101

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartStop = findViewById(R.id.btnStartStop)
        updateButtonState()

        btnStartStop.setOnClickListener {
            if (!hasPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }

            val serviceIntent = Intent(this, RtspService::class.java)

            if (!RtspService.isRunning) {
                ContextCompat.startForegroundService(this, serviceIntent)
                // Small delay to allow service to start before updating UI text
                btnStartStop.postDelayed({ updateButtonState() }, 500)
            } else {
                serviceIntent.action = "STOP_STREAM"
                startService(serviceIntent)
                btnStartStop.postDelayed({ updateButtonState() }, 500)
            }
        }

        if (!hasPermissions()) {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun getTailscaleIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (intf.name.contains("tun")) {
                    for (addr in intf.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (ip != null && ip.startsWith("100.")) {
                                return ip
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Tailscale IP Not Found"
    }

    private fun updateButtonState() {
        if (RtspService.isRunning) {
            val tailscaleIp = getTailscaleIp()
            btnStartStop.text = "Stop Background Server\nrtsp://tailscale:securecam123@$tailscaleIp:1935"
        } else {
            btnStartStop.text = "Start Background Server"
        }
    }

    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (!hasPermissions()) {
                Toast.makeText(this, "Permissions required for streaming", Toast.LENGTH_SHORT).show()
            }
        }
    }
}