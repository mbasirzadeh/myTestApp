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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.example.myapplication.R
import com.example.myapplication.ui.MainActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.PlaybackPreparer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.QueueNavigator
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.extractor.mp4.Track
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class AudioService :MediaBrowserServiceCompat() {

    //player
    private var player: ExoPlayer?=null
    //media session
    private lateinit var mediaSession:MediaSessionCompat
    //media session connector
    private lateinit var mediaSessionConnector: MediaSessionConnector
    //notif manager
    private lateinit var playerNotificationManager: PlayerNotificationManager
    //music data//
    //currentAudio
    private var currentPath:Uri?=null
    //currentAudio
    private var currentTitle:String?=null
    //currentAudio
    private var currentArtist:String?=null

    override fun onCreate() {
        super.onCreate()
        //init
        player=ExoPlayer.Builder(this).build()
        mediaSession= MediaSessionCompat(this,Constants.EXO_TAG)
        //notif
        playerNotificationManager=PlayerNotificationManager.Builder(applicationContext
            ,Constants.NOTIFICATION_ID,Constants.CHANNEL_ID)
            .setMediaDescriptionAdapter(object :PlayerNotificationManager.MediaDescriptionAdapter{
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return currentTitle?: "Unknown"
                }
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    //flag
                    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    //intent
                    var intent=MainActivity.getCallingIntent(this@AudioService,currentPath.toString())
                    return PendingIntent.getActivity(this@AudioService,0,intent,flag)
                }
                override fun getCurrentContentText(player: Player): CharSequence? {
                    return currentArtist
                }
                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    val retriever= MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(currentPath.toString())
                        val coverByte= retriever.embeddedPicture
                        val coverBitmap= if (coverByte!=null){
                            BitmapFactory.decodeByteArray(coverByte,0,coverByte.size)
                        }else{
                            Bitmap.createBitmap(resources.getDrawable(android.R.drawable.ic_media_play,null).toBitmap())
                        }
                        retriever.release()
                        return coverBitmap
                    }catch (e:Exception){
                        Log.e("exc", "getCurrentLargeIcon: "+e.message )
                    }
                    return Bitmap.createBitmap(resources.getDrawable(android.R.drawable.ic_media_play,null).toBitmap())
                }

            })
            .setNotificationListener(object :PlayerNotificationManager.NotificationListener{
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf()
                }
                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    startForeground(notificationId,notification)
                }
            })
            .build()
        playerNotificationManager.apply {
            setUsePreviousAction(true)
            setUsePlayPauseActions(true)
            setUseNextAction(true)
        }
        playerNotificationManager.setPlayer(player)
        sessionToken=mediaSession.sessionToken
        mediaSession.isActive=true
        mediaSessionConnector= MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)

        //connecting to MediaController for playBack
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
                return PlaybackStateCompat.ACTION_PLAY_FROM_URI
            }
            //playBack
            override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
                val pathToPlay=uri.toString()
                val title=extras?.getString(Constants.MUSIC_TITLE)
                val artist=extras?.getString(Constants.MUSIC_ARTIST)
                currentTitle=title
                currentArtist=artist

                if (pathToPlay!="" && pathToPlay != currentPath.toString()){
//                    val dataSourceFactory=DefaultDataSourceFactory(this@AudioService,"media player")
//                    val mediaSource=ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
//                        MediaItem.fromUri(uri))
                    player?.setMediaItem(MediaItem.fromUri(uri))
                    player?.prepare()
                    player?.playWhenReady=true

                }
                Log.i("infoplayback", "onPrepareFromUri: $pathToPlay")
            }
        })
        //sync clients , manage clients connections
        mediaSessionConnector.setQueueNavigator(object :TimelineQueueNavigator(mediaSession){
            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {
                return MediaDescriptionCompat.Builder()
                    .setTitle(currentTitle)
                    .setIconUri(currentPath)
                    .build()
            }

        })

    }


    //handle client connections
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        //Returns a root ID that clients can use with onLoadChildren() to retrieve
        //the content hierarchy.
        return MediaBrowserServiceCompat.BrowserRoot(Constants.MY_MEDIA_ROOT_ID,null)
    }
    //determinate clients access level
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>
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