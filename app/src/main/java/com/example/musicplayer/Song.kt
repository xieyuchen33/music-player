package com.example.musicplayer

import android.os.Parcel
import android.os.Parcelable

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long
) : Parcelable {
    // 1. 从 Parcel 中读取数据
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )

    // 2. 将数据写入 Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeString(path)
        parcel.writeLong(duration)
    }

    override fun describeContents(): Int {
        return 0
    }

    // 3. 必须提供 CREATOR 对象，系统通过它来创建 Song 对象
    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: Parcel): Song {
            return Song(parcel)
        }

        override fun newArray(size: Int): Array<Song?> {
            return arrayOfNulls(size)
        }
    }
}