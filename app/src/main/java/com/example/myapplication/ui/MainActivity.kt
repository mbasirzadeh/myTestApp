package com.example.myapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.media.session.MediaController
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
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
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    //exoplayer//
    //mediaBrowser (connecting to mediaBrowserService & getting sessionToken from AudioService for creating MediaController)
    private lateinit var mediaBrowser: MediaBrowserCompat
    //mediaController (control media player)
    private lateinit var mediaController:MediaControllerCompat

    //currentAudio
    private var currentPath: String?=null
    //currentAudio
    private var currentTitle:String?=null
    //currentAudio
    private var currentArtist:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //request permission
        runTimePermission(getRequiredPermissions())
        //init
        mediaBrowser= MediaBrowserCompat(this, ComponentName(this,AudioService::class.java),
        mediaBrowserConnectionCallback,null)
    }


    override fun onStart() {
        super.onStart()
        //connect to Audio service
        if (!mediaBrowser.isConnected){
            mediaBrowser.connect()
        }
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
                    currentPath=it.path
                    currentTitle=it.title
                    currentArtist=it.artist
                    buildTransportControl()
                }

                //play pause clicked
                btnPlayPause.setOnClickListener {
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
                    Log.e("excTAG", "onResume: ", )
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
    private fun runTimePermission(permissions:MutableList<String>){
        Dexter.withContext(this)
            .withPermissions(permissions)
            .withListener(object :MultiplePermissionsListener{
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    //calling
                    lifecycleScope.launchWhenCreated {
                        viewModel.getMusics()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            })
            .check()
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

    //Connection to AudioService call back
    private val mediaBrowserConnectionCallback=object : MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            try {
                mediaController= MediaControllerCompat(this@MainActivity,mediaBrowser.sessionToken)
                //update ui
                updateUi(mediaController.metadata,mediaController.playbackState)
                //register mediaController callback
                mediaController.registerCallback(mediaControllerCallback)
            }catch (e:Exception){
                Log.e("exc", "onConnected: "+e.message )
            }
        }
    }

    //transport Control
    private fun buildTransportControl(){
        if (currentPath!=null){
            //transport Audio to service
            mediaController.transportControls.playFromUri(
                currentPath?.toUri()
                ,Bundle().apply {
                    putString(Constants.MUSIC_TITLE,currentTitle)
                    putString(Constants.MUSIC_ARTIST,currentArtist)})
            binding.apply {
                //play-pause
                btnPlayPause.setOnClickListener {
                    val pbState=mediaController.playbackState.state
                    if (pbState==PlaybackStateCompat.STATE_PLAYING){
                        mediaController.transportControls.pause()
                        updateUi(mediaController.metadata,mediaController.playbackState)
                    }else{
                        mediaController.transportControls.play()
                        updateUi(mediaController.metadata,mediaController.playbackState)
                    }
                }
            }
        }
    }

    //media controller callback
    private var mediaControllerCallback=object :MediaControllerCompat.Callback(){
        @SuppressLint("UseCompatLoadingForDrawables")
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            binding.apply {
                if (state?.state==PlaybackStateCompat.STATE_PLAYING){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        btnPlayPause.background=resources.getDrawable(R.drawable.pause,null)
                    }else{
                        btnPlayPause.background=resources.getDrawable(R.drawable.pause)
                    }
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        btnPlayPause.background=resources.getDrawable(R.drawable.play,null)
                    }else{
                        btnPlayPause.background=resources.getDrawable(R.drawable.play)
                    }
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            binding.txtTitle.text=metadata?.description?.title
        }
    }

    companion object{
        //Main Activity Intent
        fun getCallingIntent(context: Context,path: String,title:String?,artist:String?):Intent{
            val intent=Intent(context,MainActivity::class.java)
            intent.putExtra(Constants.MUSIC_PATH,path)
            intent.putExtra(Constants.MUSIC_TITLE,title)
            intent.putExtra(Constants.MUSIC_ARTIST,artist)
            intent.action = Constants.ACTION_NOTIF_CLICKED
            return intent
        }
        //required permissions
        fun getRequiredPermissions():MutableList<String>{
            var permissions= mutableListOf<String>()
            if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.TIRAMISU){
                permissions.apply {
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }else{
                permissions.apply {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            return permissions
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action==Constants.ACTION_NOTIF_CLICKED){
            val path= intent.getStringExtra(Constants.MUSIC_PATH)!!
            val title=intent.getStringExtra(Constants.MUSIC_TITLE)!!
            val artist=intent.getStringExtra(Constants.MUSIC_ARTIST)!!
            currentPath=path
            currentTitle=title
            currentArtist=artist
        }
    }

    //update ui
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateUi(metadata: MediaMetadataCompat?, state: PlaybackStateCompat?){
        binding.apply {
            //btn background
            if (state?.state!=PlaybackStateCompat.STATE_PLAYING){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btnPlayPause.background=resources.getDrawable(R.drawable.play,null)
                }else{
                    btnPlayPause.background=resources.getDrawable(R.drawable.play)
                }
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btnPlayPause.background=resources.getDrawable(R.drawable.pause,null)
                }else{
                    btnPlayPause.background=resources.getDrawable(R.drawable.pause)
                }
            }
            //title
            txtTitle.text=metadata?.description?.title ?:"Title"

            //currentAudio
            currentPath=metadata?.description?.iconUri?.toString()
            currentTitle=metadata?.description?.title?.toString()
//            currentArtist=metadata?.description?.mediaUri?.authority
        }

    }
}