package com.example.musicplayer

import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerDetailActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var btnPlayPause: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_detail)

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvArtist = findViewById<TextView>(R.id.tvDetailArtist)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        // 1. 接收从列表页传过来的歌曲数据
        val song = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("SONG_DATA", Song::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("SONG_DATA")
        }

        if (song != null) {
            // 显示歌名和歌手
            tvTitle.text = song.title
            tvArtist.text = song.artist

            // 2. 初始化 ExoPlayer
            player = ExoPlayer.Builder(this).build()

            // 把本地文件路径传进去
            val mediaItem = MediaItem.fromUri(song.path)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true // 进来直接播放

            // 3. 按钮点击控制
            btnPlayPause.setOnClickListener {
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        it.play()
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                    }
                }
            }
        } else {
            Toast.makeText(this, "获取歌曲信息失败", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. 页面销毁时释放播放器，防止内存泄漏（非常重要，答辩会问）
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}