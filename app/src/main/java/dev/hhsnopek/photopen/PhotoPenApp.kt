package dev.hhsnopek.photopen

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class PhotoPenApp : Application(), ImageLoaderFactory {
    /**
     * Videos need a decoder that can pull a frame out of an mp4, otherwise the
     * grid would show a grey box for every clip.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
