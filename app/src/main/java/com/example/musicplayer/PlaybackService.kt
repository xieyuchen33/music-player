package com.example.musicplayer

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // 1. 创建真正的播放器
        val player = ExoPlayer.Builder(this).build()
        // 添加循环模式
        player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL

        // 2. 把播放器绑定到 MediaSession 上
        mediaSession = MediaSession.Builder(this, player).build()
    }

    // 3. 当页面（或者其他应用）请求连接时，把这个 session 交给它
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // 🚀 新增：接收来自小组件的指令
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_TOGGLE_FROM_WIDGET") {
            mediaSession?.let { session ->
                val player = session.player
                // 判断是否在播放，然后切换状态
                if (player.isPlaying) {
                    player.pause()
                } else {
                    // 如果播放器里没有歌曲，需要给个提示（可选）
                    if (player.mediaItemCount > 0) {
                        player.play()
                    }
                }

                // 状态切换后，立刻通知桌面小组件刷新UI
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val thisWidget = ComponentName(this, MusicWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                for (appWidgetId in allWidgetIds) {
                    MusicWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId, player.isPlaying)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // 🚀 当用户从后台任务列表划掉 App 时，会触发这个方法
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // 1. 停止播放
        mediaSession?.player?.stop()
        mediaSession?.player?.release()

        // 2. 停止前台服务，通知栏就会消失
        stopSelf()
    }

    // 4. 服务销毁时，释放资源，防止内存泄漏
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}