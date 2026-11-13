package com.example.musicplayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MusicWidgetProvider : AppWidgetProvider() {

    // 当小组件被添加到桌面时调用
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // 接收点击事件的广播（用于播放/暂停）
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_TOGGLE_PLAY") {
            // 1. 连接后台服务
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture: ListenableFuture<MediaController> = MediaController.Builder(context, sessionToken).buildAsync()

            controllerFuture.addListener({
                val controller = controllerFuture.get()
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
                // 释放连接，避免内存泄漏 (因为小组件不在前台常驻)
                MediaController.releaseFuture(controllerFuture)
            }, MoreExecutors.directExecutor())
        }
    }

    companion object {
        // 更新小组件的 UI 和点击事件
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // 1. 设置点击事件 (发送广播到当前类)
            val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = "ACTION_TOGGLE_PLAY"
            }
            // 注意：Android 12+ 必须指定 FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetPlayPause, pendingIntent)

            // 2. 告诉系统更新这个小组件的 UI
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}