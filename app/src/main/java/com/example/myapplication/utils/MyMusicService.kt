package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.example.myapplication.R

class MyMusicService :Service() {
    //media player
    lateinit var mediaPlayer:MediaPlayer
    //media session
    private lateinit var mediaSession:MediaSessionCompat
    //media meta data
    private lateinit var retriever: MediaMetadataRetriever

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer=MediaPlayer()

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //pending Intents to broadcast
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val playIntent=Intent(baseContext,MyMusicService::class.java).setAction(Constants.PENDING_INTENT_ACTION_PLAY)
        val playPendingIntent =PendingIntent.getService(this,0,playIntent,flag)

        //handel actions
        when(intent?.action){
            //clicked music from activity
            Constants.INTENT_ACTION_START_MUSIC_SERVICE->{
                //disable prev music
                try {
                    mediaPlayer.pause()
                    mediaPlayer.release()
                }catch (e:Exception){
                    Log.e("exc", "onCreate: "+e.message )
                }
                //getting Audio data
                val path=intent.getStringExtra(Constants.MUSIC_PATH)!!
                val title=intent.getStringExtra(Constants.MUSIC_TITLE)!!
                val artist=intent.getStringExtra(Constants.MUSIC_ARTIST)!!
                //get cover
                val coverBitmap=getCover(path)
                //play pending intent data
                playIntent.putExtra(Constants.MUSIC_PATH,path)

                //media player
                mediaPlayer=MediaPlayer.create(this,path.toUri())
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
                //starting play back state
                mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING,mediaPlayer.currentPosition.toLong(),1F)
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

                //notification
                showNotification(coverBitmap, R.drawable.pause, playPendingIntent)
            }
            //clicked play-pause btn in notification
            Constants.PENDING_INTENT_ACTION_PLAY->{
//                //getting path
//                val path=intent.getStringExtra(Constants.MUSIC_PATH)!!
//                //get cover
//                val coverBitmap=getCover(path)
//                //pending Intents data
//                playIntent.putExtra(Constants.MUSIC_PATH,path)

                if (mediaPlayer.isPlaying){
                    mediaPlayer.pause()
                    mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED,mediaPlayer.currentPosition.toLong(),0F)
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                        .build())

//                    showNotification(coverBitmap,R.drawable.baseline_play_circle_black_24dp, playPendingIntent)

                }else{
                    mediaPlayer.start()
                    mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING,mediaPlayer.currentPosition.toLong(),1F)
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                        .build())
//                    showNotification(coverBitmap,R.drawable.baseline_pause_circle_black_24dp, playPendingIntent)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.pause()
        mediaPlayer.release()
        mediaSession.release()
        retriever.release()
    }

    private fun showNotification(
        coverBitmap: Bitmap,
        playActionIcon:Int,
        pendingIntent: PendingIntent) {
        val notification=NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(coverBitmap)
            .setStyle(//media style
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
                    .setShowCancelButton(true))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            //actions
            .addAction(playActionIcon,"play", pendingIntent)
            .build()
        startForeground(1,notification)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getCover(path:String):Bitmap{
        retriever= MediaMetadataRetriever()
        retriever.setDataSource(path)
        val coverByte= retriever.embeddedPicture
        val coverBitmap= if (coverByte!=null){
            BitmapFactory.decodeByteArray(coverByte,0,coverByte.size)
        }else{
            Bitmap.createBitmap(resources.getDrawable(android.R.drawable.ic_media_play,null).toBitmap())
        }
        retriever.release()
        return coverBitmap
    }


}