package com.thruxion.app.rt.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.thruxion.app.MainActivity
import com.thruxion.app.R
import com.thruxion.app.data.AppDatabase
import com.thruxion.app.rt.db.LocationPointConverter
import com.thruxion.app.rt.ws.RtWebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RtLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationManager: LocationManager
    private var isTracking = false

    private val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            val point = LocationPointConverter.convertLocationToPoint(location) ?: return
            serviceScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.pendingPointDao()
                dao.insert(point)
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
                dao.deleteOlderThan(sevenDaysAgo)
                dao.trimToMaxRows()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopTrackingService()
            return START_NOT_STICKY
        }

        if (!isTracking) {
            isTracking = true
            startForeground(NOTIFICATION_ID, createNotification())
            startLocationUpdates()
            RtWebSocketClient.getInstance(this).startTracking()
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val provider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
            ) {
                LocationManager.FUSED_PROVIDER
            } else {
                LocationManager.GPS_PROVIDER
            }

            val locationRequest = LocationRequestCompat.Builder(10_000L)
                .setMinUpdateIntervalMillis(5_000L)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()

            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                provider,
                locationRequest,
                ContextCompat.getMainExecutor(this),
                locationListener
            )
        } catch (e: Exception) {
            // Permission missing or location provider unavailable
        }
    }

    private fun stopTrackingService() {
        if (!isTracking) return
        isTracking = false
        try {
            LocationManagerCompat.removeUpdates(locationManager, locationListener)
        } catch (e: SecurityException) {
            // Permission missing
        } catch (e: Exception) {
            // Ignore
        }
        RtWebSocketClient.getInstance(this).stopTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingService()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RtLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live Location Tracking")
            .setContentText("Recording and sending position...")
            .setSmallIcon(R.drawable.ic_my_location)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_logout,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    companion object {
        const val ACTION_START = "com.thruxion.app.rt.action.START"
        const val ACTION_STOP = "com.thruxion.app.rt.action.STOP"
        private const val CHANNEL_ID = "rt_location_channel"
        private const val NOTIFICATION_ID = 4001

        fun startService(context: Context) {
            val intent = Intent(context, RtLocationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RtLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
