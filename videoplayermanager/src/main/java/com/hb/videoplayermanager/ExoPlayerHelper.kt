package com.hb.videoplayermanager

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class ExoPlayerHelper {
    companion object {
        fun buildMediaSource(context: Context, uri: Uri): MediaSource {
            val mDataSourceFactory =
                DefaultDataSourceFactory(context, Util.getUserAgent(context, "mediaPlayerSample"))
            val type = Util.inferContentType(uri)
            when (type) {
                C.TYPE_SS -> return SsMediaSource.Factory(mDataSourceFactory).createMediaSource(uri)
                C.TYPE_DASH -> return DashMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(uri)
                C.TYPE_HLS -> return HlsMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(uri)
                C.TYPE_OTHER -> return ExtractorMediaSource.Factory(mDataSourceFactory)
                    .createMediaSource(uri)
                else -> {
                    throw IllegalStateException("Unsupported type: $type") as Throwable
                }
            }
        }
    }
}