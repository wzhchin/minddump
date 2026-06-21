package com.chin.minddump.di

import com.chin.minddump.audio.AudioRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object MediaModule {

    @Provides
    @ActivityScoped
    fun provideAudioRecorder(): AudioRecorder = AudioRecorder()
}
