package com.example.myapplication.utils

import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.myapplication.R

class MyMusicService :Service() {
    //media player
    lateinit var mediaPlayer:MediaPlayer

    //
    private lateinit var mediaSession:MediaSessionCompat

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //disable prev music
        try {
            mediaPlayer.pause()
            mediaPlayer.release()
        }catch (e:Exception){
            Log.e("exc", "onCreate: "+e.message )
        }
        //data
        val path= intent?.getStringExtra(Constants.MUSIC_PATH)?.toUri()
        val title=intent?.getStringExtra(Constants.MUSIC_TITLE)
        //media player
        mediaPlayer=MediaPlayer.create(this,path)
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
        //building media session
        mediaSession= MediaSessionCompat(baseContext,"my music")
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE,title)
                .putLong(MediaMetadata.METADATA_KEY_DURATION,mediaPlayer.duration.toLong())
                .build())
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(
            PlaybackStateCompat.STATE_PLAYING,mediaPlayer.currentPosition.toLong(),1F)
            .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
            .build())
        //notification
        val notification=NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("music")
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            .build()

        startForeground(1,notification)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.pause()
        mediaPlayer.release()
    }


}