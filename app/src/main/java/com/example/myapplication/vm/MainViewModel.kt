package com.example.myapplication.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.models.Audio
import com.example.myapplication.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: MainRepository) :ViewModel() {

    val audioList= MutableLiveData<MutableList<Audio>>()
    val loading=MutableLiveData<Boolean>()
    val exception=MutableLiveData<Boolean>()

    suspend fun getMusics()=viewModelScope.launch {
        loading.postValue(true)
        try {
            audioList.postValue(repository.getMusics())
        }catch (e:Exception){
            Log.e("exc", "getMusics: "+e.message )
            exception.postValue(true)
        }
        loading.postValue(false)
    }
}