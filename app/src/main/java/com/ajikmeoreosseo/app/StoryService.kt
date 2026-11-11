package com.ajikmeoreosseo.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * 이야기를 들려주는 동안 앱이 뒤로 가도 멈추지 않게 지켜 주는 포그라운드 서비스.
 *
 * 운전 중에는 보통 내비 화면을 띄워 두므로 이 앱은 뒤에 있습니다. 포그라운드 서비스가 없으면 시스템이
 * 뒤에 있는 앱을 10초쯤 뒤에 얼려서(freezer) 이야기가 끊깁니다. 이야기 진행 자체는 화면([MainActivity])이 맡고,
 * 이 서비스는 "지금 소리를 재생 중"이라는 것을 시스템에 알리는 역할만 합니다.
 */
class StoryService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.service_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_text))
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            // 종류를 받는 startForeground는 안드로이드 10부터 있습니다.
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val TAG = "StoryService"
        private const val CHANNEL = "story"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, StoryService::class.java)) }
                .onFailure { Log.w(TAG, "could not start", it) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, StoryService::class.java)) }
        }
    }
}
