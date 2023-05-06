package com.example.myapplication.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace.Connector
import android.media.MediaMetadataRetriever
import android.media.session.MediaSession
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.MediaStore.Audio
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.example.myapplication.ui.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.PlaybackPreparer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.QueueNavigator
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.extractor.mp4.Track
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class AudioService :MediaBrowserServiceCompat() {

    //player
    private var player: ExoPlayer?=null
    //media session
    private lateinit var mediaSession:MediaSessionCompat
    //media session connector
    private lateinit var mediaSessionConnector: MediaSessionConnector
    //currentAudio
    private var currentAudio:com.example.myapplication.models.Audio?=null
    //notif manager
    private lateinit var playerNotificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()
        //init
        player=ExoPlayer.Builder(this).build()
        mediaSession= MediaSessionCompat(this,Constants.EXO_TAG)

        playerNotificationManager=PlayerNotificationManager.Builder(applicationContext
            ,Constants.NOTIFICATION_ID,Constants.CHANNEL_ID)
                //notif
            .setMediaDescriptionAdapter(object :PlayerNotificationManager.MediaDescriptionAdapter{
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return currentAudio?.title?.subSequence(IntRange(0,10)) ?: "Unknown"
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    var intent=MainActivity.getCallingIntent(this@AudioService,null)
                    return PendingIntent.getActivity(this@AudioService,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    TODO("Not yet implemented")
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    val retriever= MediaMetadataRetriever()
                    retriever.setDataSource(currentAudio?.path)
                    val coverByte= retriever.embeddedPicture
                    val coverBitmap= if (coverByte!=null){
                        BitmapFactory.decodeByteArray(coverByte,0,coverByte.size)
                    }else{
                        Bitmap.createBitmap(resources.getDrawable(android.R.drawable.ic_media_play,null).toBitmap())
                    }
                    retriever.release()
                    return coverBitmap
                }

            })
            .setNotificationListener(object :PlayerNotificationManager.NotificationListener{
                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    startForeground(notificationId,notification)
                }
            })
            .build()
        playerNotificationManager.setPlayer(player)
        sessionToken=mediaSession.sessionToken
        mediaSession.isActive=true
        mediaSessionConnector= MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)
        //connecting for playBack
        mediaSessionConnector.setPlaybackPreparer(object :PlaybackPreparer{
            override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
                TODO("Not yet implemented")
            }
            override fun onPrepare(playWhenReady: Boolean) {
            }
            override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {

            }
            override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {

            }
            //playBack with Uri
            override fun getSupportedPrepareActions(): Long {
                return PlaybackStateCompat.ACTION_PREPARE_FROM_URI
            }
            //playBack
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
                val audioToPlay=extras?.getParcelable(Constants.MUSIC_AUDIO,com.example.myapplication.models.Audio::class.java)
                if (audioToPlay!=null && audioToPlay.path != currentAudio?.path){
                    player?.prepare()
                    player?.playWhenReady=true
                }
            }
        })
        //sync clients
        mediaSessionConnector.setQueueNavigator(object :TimelineQueueNavigator(mediaSession){
            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                return MediaDescriptionCompat.Builder()
                    .setTitle(currentAudio?.title)
                    .setIconUri(currentAudio?.path?.toUri())
                    .build()
            }

        })

    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot("empty_root",null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onDestroy() {
        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
        player?.release()
        player=null
        super.onDestroy()
    }
}