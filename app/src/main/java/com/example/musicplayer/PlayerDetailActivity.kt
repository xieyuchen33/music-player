package com.example.musicplayer

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class PlayerDetailActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private lateinit var btnPlayPause: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_detail)

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvArtist = findViewById<TextView>(R.id.tvDetailArtist)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        // 1. 接收整个歌单和当前歌曲的索引
        val songList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("SONG_LIST", Song::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("SONG_LIST")
        }
        val currentIndex = intent.getIntExtra("SONG_INDEX", 0)

        if (songList != null && songList.isNotEmpty()) {
            val currentSong = songList[currentIndex]
            tvTitle.text = currentSong.title
            tvArtist.text = currentSong.artist

            // 2. 初始化与后台服务的连接 (MediaController)
            val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

            controllerFuture?.addListener({
                mediaController = controllerFuture?.get()

                // 3. 将整个列表转换为 MediaItem 列表
                val mediaItems = songList.map { MediaItem.fromUri(it.path) }

                // 4. 一次性把列表传给后台，并告诉它从哪一首开始播 (currentIndex)
                mediaController?.setMediaItems(mediaItems, currentIndex, 0)
                mediaController?.prepare()
                mediaController?.play()

                // 5. 监听播放状态，更新按钮图标
                mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        } else {
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        }
                    }
                })

                // 6. 按钮点击控制
                btnPlayPause.setOnClickListener {
                    if (mediaController?.isPlaying == true) {
                        mediaController?.pause()
                    } else {
                        mediaController?.play()
                    }
                }
            }, MoreExecutors.directExecutor())
        } else {
            Toast.makeText(this, "获取歌曲信息失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 7. 页面退出时，断开连接 (但后台服务还在跑，音乐不会停！)
    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}