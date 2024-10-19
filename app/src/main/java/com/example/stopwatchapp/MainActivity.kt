package com.example.stopwatchapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.stopwatchapp.databinding.ActivityMainBinding
import com.example.stopwatchapp.services.StopwatchService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var isServiceRunning=false
    private lateinit var chronometerReceiver: BroadcastReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener {
            if (!isServiceRunning){
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission()
                } else {
                    startForegroundServiceAndNotification()
                    isServiceRunning=true
                }
            }

        }
        binding.stop.setOnClickListener {
            Intent(this, StopwatchService::class.java).also {
                stopService(it)
                isServiceRunning=false

            }
        }
        chronometerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val elapsedTime = intent?.getIntExtra("elapsedTime", 0) ?: 0
                val minutes = elapsedTime / 60
                val seconds = elapsedTime % 60
                val hours = minutes / 60
                binding.stopwatch.text = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)

            }
        }

        // BroadcastReceiver'ı register et
        registerReceiver(chronometerReceiver, IntentFilter("ChronometerUpdate"))


    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startForegroundServiceAndNotification()
                isServiceRunning=true
                Log.d("osman", "${permissions[0]}izin verildi")
            }
        }else{
            Log.d("osman","${permissions[0]}izin verilmedi")

        }
    }

    private fun startForegroundServiceAndNotification() {
        Intent(this, StopwatchService::class.java).also {
           startService(it)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiver'ı unregister et
        unregisterReceiver(chronometerReceiver)
    }
}