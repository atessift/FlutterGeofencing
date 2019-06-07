// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import android.util.Log
import io.flutter.view.FlutterNativeView

class IsolateHolderService : Service() {
    companion object {
        @JvmStatic
        val ACTION_SHUTDOWN = "SHUTDOWN"
        @JvmStatic
        private val WAKELOCK_TAG = "IsolateHolderService::WAKE_LOCK"
        @JvmStatic
        private val TAG = "IsolateHolderService"
        @JvmStatic
        private var sBackgroundFlutterView: FlutterNativeView? = null

        @JvmStatic
        fun setBackgroundFlutterView(view: FlutterNativeView?) {
            sBackgroundFlutterView = view
        }
    }

    override fun onBind(p0: Intent?) : IBinder? {
        return null;
    }

    override fun onCreate() {
        super.onCreate()
        val CHANNEL_ID = "geofencing_plugin_channel"
        val channel = NotificationChannel(CHANNEL_ID,
                "Persistent Notification",
                NotificationManager.IMPORTANCE_LOW)
        val imageId = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName())

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val mainActivityClass = Class.forName("com.example.frontend.MainActivity")
        val contentIntent = Intent(this, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingContentIntent : PendingIntent = PendingIntent.getActivity(this, 0, contentIntent, 0)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lumie")
                .setContentText("Tap to control your lights")
                .setSmallIcon(imageId)
                .setContentIntent(pendingContentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) : Int {
        if (intent?.getAction() == ACTION_SHUTDOWN) {
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                    if (isHeld()) {
                        release()
                    }
                }
            }
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY;
    }
}
