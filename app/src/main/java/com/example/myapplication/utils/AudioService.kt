package com.example.myapplication.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.example.myapplication.ui.MainActivity
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.PlaybackPreparer
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.MimeTypes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AudioService :MediaBrowserServiceCompat() {

    //player
    @Inject
    lateinit var player:ExoPlayer
    //media session
    @Inject
    lateinit var mediaSession:MediaSessionCompat
    //media session connector
    private lateinit var mediaSessionConnector:MediaSessionConnector
    //notif manager
    private lateinit var playerNotificationManager: PlayerNotificationManager
    //music data//
    //currentAudio
    private var currentPath:Uri?=null
    //currentAudio
    private var currentTitle:String?=null
    //currentAudio
    private var currentArtist:String?=null

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        //init
        mediaSession.isActive=true
        sessionToken=mediaSession.sessionToken
        mediaSessionConnector= MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)

        //notification
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
                    //Main Activity Intent
                    val intentMainActivity=Intent(this@AudioService,MainActivity::class.java)
                    intentMainActivity.putExtra(Constants.MUSIC_PATH,currentPath)
                    intentMainActivity.putExtra(Constants.MUSIC_TITLE,currentTitle)
                    intentMainActivity.putExtra(Constants.MUSIC_ARTIST,currentArtist)
                    intentMainActivity.action = Constants.ACTION_NOTIF_CLICKED

                    return PendingIntent.getActivity(this@AudioService,0,intentMainActivity,flag)
                }
                override fun getCurrentContentText(player: Player): CharSequence? {
                    return currentArtist
                }
                @SuppressLint("UseCompatLoadingForDrawables")
                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    val cover = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        resources.getDrawable(android.R.drawable.ic_media_play,null)
                    }else{
                        resources.getDrawable(android.R.drawable.ic_media_play)
                    }
                    val retriever= MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(this@AudioService,currentPath)
                        val coverByte= retriever.embeddedPicture
                        val coverBitmap= if (coverByte!=null){
                            BitmapFactory.decodeByteArray(coverByte,0,coverByte.size)
                        }else{
                            Bitmap.createBitmap(cover.toBitmap())
                        }
                        retriever.release()
                        return coverBitmap
                    }catch (e:Exception){
                        Log.e("exc", "getCurrentLargeIcon: "+e.message )
                    }
                    return Bitmap.createBitmap(cover.toBitmap())
                }
            })
            .setNotificationListener(object :PlayerNotificationManager.NotificationListener{

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(true)
                    stopSelf()
                }
                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    ContextCompat.startForegroundService(
                        this@AudioService,
                        Intent(applicationContext,this@AudioService::class.java)
                    )
                    startForeground(Constants.NOTIFICATION_ID,notification)
                }
            })
            .build().apply {
                setUsePreviousAction(true)
                setUsePlayPauseActions(true)
                setUseNextAction(true)
                setPlayer(player)
                setMediaSessionToken(mediaSession.sessionToken)
            }




        //connecting to MediaController for playBack
        mediaSessionConnector.setPlaybackPreparer(object :PlaybackPreparer{
            override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?)=false
            override fun onPrepare(playWhenReady: Boolean)=Unit
            override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) =Unit
            override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?)=Unit
            //playBack with Uri
            override fun getSupportedPrepareActions(): Long {
                return PlaybackStateCompat.ACTION_PLAY_FROM_URI
            }
            //playBack
            override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
                val pathToPlay=uri.toString()
                val title=extras?.getString(Constants.MUSIC_TITLE)
                val artist=extras?.getString(Constants.MUSIC_ARTIST)

                if (pathToPlay!="" && pathToPlay != currentPath.toString()){
//                    val dataSourceFactory=DefaultDataSourceFactory(this@AudioService,"media player")
//                    val mediaSource=ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
//                        MediaItem.fromUri(uri))
                    currentTitle=title
                    currentArtist=artist
                    currentPath=pathToPlay.toUri()
                    player.setMediaItem(MediaItem.Builder().setMimeType(MimeTypes.BASE_TYPE_AUDIO).setUri(uri).build())
                    player.prepare()
                    player.playWhenReady =true

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
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        //Returns a root ID that clients can use with onLoadChildren() to retrieve
        //the content hierarchy.
        return BrowserRoot(Constants.MY_MEDIA_ROOT_ID,null)
    }
    //determinate clients access level
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    override fun onDestroy() {
        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
        player.release()
        super.onDestroy()
    }
}