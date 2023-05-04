package com.example.myapplication.repository

import com.example.myapplication.data.ContentProvider
import javax.inject.Inject

class MainRepository @Inject constructor(private val contentProvider: ContentProvider) {
    suspend fun getMusics()=contentProvider.getMusics()
}