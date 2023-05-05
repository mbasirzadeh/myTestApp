package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.example.myapplication.R
import kotlinx.coroutines.coroutineScope

class MyMusicService :Service() {
    //media player
    lateinit var mediaPlayer:MediaPlayer
    //media session
    private lateinit var mediaSession:MediaSessionCompat

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //disable prev music
        try {
            mediaPlayer.pause()
            mediaPlayer.release()
        }catch (e:Exception){
            Log.e("exc", "onCreate: "+e.message )
        }
        //getting music data
        val path= intent?.getStringExtra(Constants.MUSIC_PATH)?.toUri()
        val title=intent?.getStringExtra(Constants.MUSIC_TITLE)
        val artist=intent?.getStringExtra(Constants.MUSIC_ARTIST)
        //media player
        mediaPlayer=MediaPlayer.create(this,path)
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
        mediaPlayer.setOnCompletionListener {
            stopSelf()
        }
        //building media session
        mediaSession= MediaSessionCompat(baseContext,"my music")
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE,title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST,artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION,mediaPlayer.duration.toLong())
                .build())
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(
            PlaybackStateCompat.STATE_PLAYING,mediaPlayer.currentPosition.toLong(),1F)
            .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
            .build())
        //media session callback
        mediaSession.setCallback(object: MediaSessionCompat.Callback(){
            override fun onSeekTo(pos: Long) {
                super.onSeekTo(pos)
                mediaPlayer.seekTo(pos.toInt())
                val playBackStateNew = PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.currentPosition.toLong(),1f)
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .build()
                mediaSession.setPlaybackState(playBackStateNew)
            }
        })
        //get cover
        val retriever=MediaMetadataRetriever()
        retriever.setDataSource(path.toString())
        val coverByte= retriever.embeddedPicture
        val coverBitmap:Bitmap = if (coverByte!=null){
            BitmapFactory.decodeByteArray(coverByte,0,coverByte.size)
        }else{
            Bitmap.createBitmap(resources.getDrawable(R.drawable.ic_launcher_foreground,null).toBitmap())
        }
        //notification
        val notification=NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(coverBitmap)
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