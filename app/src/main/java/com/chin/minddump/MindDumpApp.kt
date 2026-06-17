package com.chin.minddump

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.chin.minddump.notification.EventChannels
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MindDumpApp :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Create notification channels up front so reminders/digest posts never fail.
        EventChannels.ensureCreated(this)
        Timber.d("MindDump initialized")
    }

    /**
     * Supplies the app-wide [ImageLoader] that [coil3.compose.AsyncImage] resolves via
     * [SingletonImageLoader.get]. Registers [VideoFrameDecoder.Factory] so that `.mp4`
     * entries decode a real first-frame thumbnail instead of rendering blank. This is
     * Coil 3's canonical singleton mechanism; a Hilt-provided loader would not be
     * discovered by AsyncImage.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
