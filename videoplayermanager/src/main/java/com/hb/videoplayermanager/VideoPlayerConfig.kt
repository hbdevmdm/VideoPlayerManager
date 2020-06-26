package com.hb.videoplayermanager

import java.io.Serializable

data class VideoPlayerConfig(
    var videoPath: String? = "",
    var allowPictureInPicture: Boolean = false,
    var autoPlay: Boolean = false,
    var loopVideo: Boolean = false,
    var orientation: Int = ORIENTATION_PORTRAIT_ONLY
) : Serializable {

    companion object {
        const val ORIENTATION_PORTRAIT_ONLY = 0
        const val ORIENTATION_LANDSCAPE_ONLY = 1
        const val ORIENTATION_USER_ORIENTATION = 2
    }

    class Builder {
        val videoPlayerConfig = VideoPlayerConfig()

        fun videoPath(videoPath: String) = apply { videoPlayerConfig.videoPath = videoPath }
        fun autoPlay(autoPlay: Boolean) = apply { videoPlayerConfig.autoPlay = autoPlay }
        fun loopVideo(loopVideo: Boolean) = apply { videoPlayerConfig.loopVideo = loopVideo }
        fun allowPictureInPicture(allowPictureInPicture: Boolean) =
            apply { videoPlayerConfig.allowPictureInPicture = allowPictureInPicture }

        fun orientation(orientation: Int) =
            apply { videoPlayerConfig.orientation = orientation }

        fun build() = videoPlayerConfig
    }
}