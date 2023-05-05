package com.example.myapplication.data

import android.content.Context
import android.provider.MediaStore
import com.example.myapplication.models.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContentProvider @Inject constructor(@ApplicationContext private val context: Context) {
    //Audio List
    var audioList= mutableListOf<Audio>()

    private val projection= arrayOf(
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ARTIST
    )
    private val cursor=context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,null,null,null
    )

    public suspend fun getMusics():MutableList<Audio>{
        while (cursor!!.moveToNext()){
            audioList.add(Audio(cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3)))
        }
        return audioList
    }
}