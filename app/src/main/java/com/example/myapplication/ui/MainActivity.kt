package com.example.myapplication.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.models.Audio
import com.example.myapplication.ui.adapter.MusicAdapter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //request permission
        runTimePermission()
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
                    //run service
                    startService(it)
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
        val intent=Intent(this,MyMusicService::class.java)
        intent.apply {
            putExtra(Constants.MUSIC_PATH,audio.path)
            putExtra(Constants.MUSIC_TITLE,audio.title)
            putExtra(Constants.MUSIC_ARTIST,audio.artist)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
    }


}