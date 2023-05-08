package com.example.myapplication.di

import android.content.Context
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.example.myapplication.models.Audio
import com.example.myapplication.utils.Constants
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioCapabilities.getCapabilities
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object Module {
    //Audio service
    //exoplayer
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
    ):ExoPlayer=ExoPlayer.Builder(context).build()
    //media session
    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context
    ):MediaSessionCompat= MediaSessionCompat(context,Constants.EXO_TAG)


}