package com.example.musicplayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false)
        }
    }

    // 点击时，直接发送 Intent 给 PlaybackService
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_TOGGLE_PLAY") {
            // 直接把指令丢给服务，让服务去处理！
            val serviceIntent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_TOGGLE_FROM_WIDGET"
            }
            // 使用 startService 唤醒服务并发送指令
            context.startService(serviceIntent)
        }
    }

    companion object {
        // 动态更新小组件UI的方法（保持原样）
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, isPlaying: Boolean) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            if (isPlaying) {
                views.setTextViewText(R.id.tvWidgetSong, "正在播放音乐")
                views.setTextViewText(R.id.btnWidgetPlayPause, "⏸")
            } else {
                views.setTextViewText(R.id.tvWidgetSong, "已暂停播放")
                views.setTextViewText(R.id.btnWidgetPlayPause, "▶")
            }

            val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = "ACTION_TOGGLE_PLAY"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetPlayPause, pendingIntent)
            views.setOnClickPendingIntent(R.id.tvWidgetSong, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}