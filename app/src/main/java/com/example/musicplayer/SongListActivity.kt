package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private val songList = mutableListOf<Song>()

    // 1. 动态权限申请器 (老师要求的：权限申请与降级处理)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            loadMusicData()
            loadLastProgress() // 权限通过后，读取进度记忆
        } else {
            // 降级处理：拒绝后提示且不崩溃
            progressBar.visibility = View.GONE
            tvEmpty.text = "您拒绝了权限，无法读取本地音乐。\n请去设置中开启权限。"
            tvEmpty.visibility = View.VISIBLE
            Toast.makeText(this, "权限被拒绝，功能受限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_list)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = LinearLayoutManager(this)
        checkPermissionAndLoad()

        // 找到定时按钮并设置跳转
        findViewById<ImageButton>(R.id.btnOpenTimer).setOnClickListener {
            val intent = Intent(this, SleepTimerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusicData()
            loadLastProgress() // 有权限时，直接读取进度记忆
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    // 🚀 新增：读取上次保存的播放进度
    private fun loadLastProgress() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefsManager = PrefsManager(this@SongListActivity)
            val lastPath = prefsManager.getLastSongPath()
            if (lastPath != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SongListActivity, "上次播放记录已加载", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 3. 扫描本地音乐 (异步处理，不阻塞主线程)
    private fun loadMusicData() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            val songs = scanLocalMusic()

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                if (songs.isEmpty()) {
                    tvEmpty.text = "暂未找到本地音乐"
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    songList.clear()
                    songList.addAll(songs)
                    // 显示数据，并加入点击跳转逻辑
                    recyclerView.adapter = SongAdapter(songList) { clickedSong ->
                        val intent = Intent(this@SongListActivity, PlayerDetailActivity::class.java)
                        // 1. 传入整个歌单列表
                        intent.putParcelableArrayListExtra("SONG_LIST", ArrayList(songList))
                        // 2. 传入当前点击的是第几首歌
                        val currentIndex = songList.indexOf(clickedSong)
                        intent.putExtra("SONG_INDEX", currentIndex)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun scanLocalMusic(): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "未知歌曲"
                val artist = it.getString(artistColumn) ?: "未知歌手"
                val path = it.getString(pathColumn) ?: ""
                val duration = it.getLong(durationColumn)
                songs.add(Song(id, title, artist, path, duration))
            }
        }
        return songs
    }
}

// 4. 基础 Adapter
class SongAdapter(
    private val songs: List<Song>,
    private val onItemClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = "${song.title} - ${song.artist}"
        holder.title.setTextColor(android.graphics.Color.BLACK)
        holder.title.textSize = 16f
        holder.title.setPadding(30, 30, 30, 30)

        holder.itemView.setOnClickListener {
            onItemClick(song)
        }
    }

    override fun getItemCount() = songs.size
}