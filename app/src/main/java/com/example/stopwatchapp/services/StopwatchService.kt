package com.example.stopwatchapp.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.stopwatchapp.MainActivity
import com.example.stopwatchapp.R

class StopwatchService:Service() {

    private lateinit var notificationManager : NotificationManager
    private lateinit var notificationBuilder : NotificationCompat.Builder
    private val TAG = "StopwatchService"
    private val CHANNEL_ID = "stopwatch_channel"
    private val CHANNEL_NAME = "Stopwatch Channel"
    private val ACTION_START = "start"
    private val ACTION_STOP = "stop"
    private var isServiceRunning=false
    private val FOREGROUND_ID = 1
    private val NOTIFICATION_ID = 2
    private var startTime = 0L
    private var elapsedTime = 0L
    private val updateInterval = 1000L // 1 saniye
    private lateinit var  pendingIntent : PendingIntent
    private  var handler =Handler(Looper.getMainLooper())
    private var runnuble = object :Runnable{
        override fun run() {
            if (isServiceRunning){
                elapsedTime = SystemClock.elapsedRealtime() - startTime
                val seconds = (elapsedTime / 1000).toInt()
                val minutes = seconds / 60
                val hours = minutes / 60
                buildNotification()
                updateNotification()
                Log.d(TAG, "Elapsed time: ${elapsedTime / 1000} seconds")

                //sendBroadcas intenti
                val intent = Intent("ChronometerUpdate")
                intent.putExtra("elapsedTime", seconds)
                sendBroadcast(intent)

                handler.postDelayed(this, updateInterval)

            }

        }

    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service is started")

        when(intent?.action){
            ACTION_START ->{
                if (!isServiceRunning) {
                    startTime = SystemClock.elapsedRealtime() - elapsedTime
                    handler.post(runnuble)
                    isServiceRunning = true
                }
            }
            ACTION_STOP -> {
                if (isServiceRunning) {
                    isServiceRunning = false
                    elapsedTime = SystemClock.elapsedRealtime() - startTime

                }
            }
            else ->{
                isServiceRunning = true
                startTime = SystemClock.elapsedRealtime()
                handler.post(runnuble)
            }

        }
        createNotificationChannel()

        return START_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacks(runnuble)
        Log.d(TAG, "Service is stopped")
        super.onDestroy()
    }

    override fun onCreate() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "onCreate called")

        createNotificationChannel()
        createIntent()
        super.onCreate()
    }
    private fun createIntent(){
        val intent=Intent(this,MainActivity::class.java)
        pendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(1001,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    }
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(channel)

        }


    }

    private fun buildNotification(){

        val startIntent = Intent(this,StopwatchService::class.java).apply {
            action = ACTION_START
        }
        val pendingStartIntent = PendingIntent.getService(this,0,startIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this,StopwatchService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(this,0,stopIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder = NotificationCompat.Builder(this,CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText(" ${time()}")
            .setSubText("foregroud")
            //.setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(resources,R.drawable.timer))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_launcher_foreground,"start",pendingStartIntent)
            .addAction(R.drawable.ic_launcher_foreground,"stop",pendingStopIntent)



        startForeground(FOREGROUND_ID,notificationBuilder.build())
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID,notificationBuilder.build())
    }
    private fun updateNotification() {


        notificationBuilder.setContentText(" ${time()}")
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

    }
    private fun time() : String{
        val elapsedTime = SystemClock.elapsedRealtime() - startTime
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60

        return String.format("Geçen süre : %02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}