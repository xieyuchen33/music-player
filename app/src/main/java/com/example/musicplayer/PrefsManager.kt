package com.example.musicplayer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 声明 DataStore 实例
val Context.dataStore by preferencesDataStore(name = "music_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        // 定义存储的键
        val LAST_SONG_PATH = stringPreferencesKey("last_song_path")
        val LAST_POSITION = stringPreferencesKey("last_position")
    }

    // 保存进度
    suspend fun saveProgress(songPath: String, position: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SONG_PATH] = songPath
            prefs[LAST_POSITION] = position.toString()
        }
    }

    // 读取上次的歌曲路径
    suspend fun getLastSongPath(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[LAST_SONG_PATH]
        }.first()
    }
}