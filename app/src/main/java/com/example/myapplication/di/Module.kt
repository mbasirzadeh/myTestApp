package com.example.myapplication.di

import android.content.Context
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.example.myapplication.models.Audio
import com.example.myapplication.utils.Constants
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object Module {
    //Audio service
    //exoplayer
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context
    ):ExoPlayer=ExoPlayer.Builder(context).build()
    //media session
    @Provides
    fun provideMediaSession(
        @ApplicationContext context: Context
    ):MediaSessionCompat= MediaSessionCompat(context,Constants.EXO_TAG)
    //media session connector
    @Provides
    fun provideMediaSessionConnector(mediaSession: MediaSessionCompat):MediaSessionConnector=MediaSessionConnector(mediaSession)
}