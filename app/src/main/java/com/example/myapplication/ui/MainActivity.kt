package com.example.myapplication.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaController2.ControllerCallback
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media.MediaBrowserCompatUtils
import androidx.media.MediaBrowserServiceCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.models.Audio
import com.example.myapplication.ui.adapter.MusicAdapter
import com.example.myapplication.utils.AudioService
import com.example.myapplication.utils.Constants
import com.example.myapplication.utils.MyMusicService
import com.example.myapplication.vm.MainViewModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    //binding
    private lateinit var binding:ActivityMainBinding
    //View Model
    private val viewModel by viewModels<MainViewModel>()
    //music adapter
    @Inject
    lateinit var musicAdapter: MusicAdapter

    //for exoplayer//
    //mediaBrowser (connecting to mediaBrowserService & getting sessionToken from AudioService for creating MediaController)
    private lateinit var mediaBrowser: MediaBrowserCompat
    //mediaController (control media player)
    private lateinit var mediaController:MediaControllerCompat
    private var currentAudio: Audio?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //request permission
        runTimePermission()
        //init
        mediaBrowser= MediaBrowserCompat(this, ComponentName(this,AudioService::class.java),
        mediaBrowserConnectionCallback,null)

    }

    //connect to Audio service
    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    override fun onResume() {
        super.onResume()
        //calling getMusics
        lifecycleScope.launchWhenCreated {
            viewModel.getMusics()
        }
        viewModel.apply {
            binding.apply {

                //audio list
                audioList.observe(this@MainActivity){
                    //recycler
                    musicAdapter.setData(it)
                    recyclerMain.apply {
                        layoutManager= LinearLayoutManager(this@MainActivity)
                        adapter=musicAdapter
                    }
                }

                //on music click listener
                musicAdapter.onMusicClickListener {
                    //run start service
                    //startService(it)
                    currentAudio=it
                    buildTransportControl()
                }



                //other
                loading.observe(this@MainActivity){
                    when(it){
                        true->progressMain.visibility= View.VISIBLE
                        false->progressMain.visibility= View.GONE
                    }
                }
                exception.observe(this@MainActivity){

                }
            }
        }
    }

    //disconnect Audio Service & unregister mediaController callback
    override fun onStop() {
        super.onStop()
        mediaController.unregisterCallback(mediaControllerCallback)
        mediaBrowser.disconnect()
    }

    //permission to access music files
    private fun runTimePermission(){
        Dexter.withContext(this)
            .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener{
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    //calling get musics
                    lifecycleScope.launchWhenCreated {
                        viewModel.getMusics()
                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    recreate()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            })
            .check();
    }

    //start music service
    private fun startService(audio: Audio){
        val intent=Intent(this,MyMusicService::class.java).setAction(Constants.INTENT_ACTION_START_MUSIC_SERVICE)
        intent.apply {
            putExtra(Constants.MUSIC_PATH,audio.path)
            putExtra(Constants.MUSIC_TITLE,audio.title)
            putExtra(Constants.MUSIC_ARTIST,audio.artist)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
    }

    //Start Audio Service
    private fun startAudioService(audio: Audio){

    }

    //Connection to AudioService call back
    private val mediaBrowserConnectionCallback=object : MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            try {
                mediaController= MediaControllerCompat(this@MainActivity,mediaBrowser.sessionToken)
                //register mediaController callback
                mediaController.registerCallback(mediaControllerCallback)
                buildTransportControl()
            }catch (e:Exception){
                Log.e("exc", "onConnected: "+e.message )
            }
        }
    }

    //transport Control
    @SuppressLint("UseCompatLoadingForDrawables")
    fun buildTransportControl(){
        if (currentAudio!=null){
            //transport Audio to service
            mediaController.transportControls.playFromUri(
                currentAudio?.path?.toUri()
                ,Bundle().apply { putParcelable(Constants.MUSIC_AUDIO,currentAudio) })
        }
        binding.apply {
            //play-pause
            btnPlayPause.setOnClickListener {
              val pbState=mediaController.playbackState.state
              if (pbState==PlaybackStateCompat.STATE_PLAYING){
                  btnPlayPause.background=resources.getDrawable(R.drawable.play,null)
                  mediaController.transportControls.pause()
              }else{
                  btnPlayPause.background=resources.getDrawable(R.drawable.pause,null)
                  mediaController.transportControls.play()
              }
            }
        }

    }

    //media controller callback
    private var mediaControllerCallback=object :MediaControllerCompat.Callback(){
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
        }
    }

    companion object{
        fun getCallingIntent(context: Context,audio: Audio?):Intent{
            val intent=Intent(context,MainActivity::class.java)
            intent.putExtra(Constants.MUSIC_AUDIO,audio)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            return intent
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        currentAudio=intent?.getParcelableExtra(Constants.MUSIC_AUDIO,Audio::class.java)
    }

}