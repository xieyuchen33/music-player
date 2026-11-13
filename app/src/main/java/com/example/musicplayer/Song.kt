package com.example.musicplayer

// 数据类，专门用来存放一首歌的信息
data class Song(
    val id: Long,       // 歌曲的唯一ID
    val title: String,  // 歌名
    val artist: String, // 歌手
    val path: String,   // 歌曲文件路径
    val duration: Long  // 歌曲时长（毫秒）
)