package com.hb.videoplayermanager


import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AppOpsManagerCompat
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.hb.videoplayermanager.databinding.ActivityMediaPlayerBinding


class VideoPlayerActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMediaPlayerBinding
    private lateinit var simpleExoPlayer: SimpleExoPlayer

    private val PAUSE_BROADCAST = "com.pip.pause"
    private val PLAY_BROADCAST = "com.pip.play"
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    companion object {

        fun createIntent(context: Context, videoPlayerConfig: VideoPlayerConfig): Intent {
            val intentX = Intent(context, VideoPlayerActivity::class.java)
            intentX.putExtra("videoPlayerConfig", videoPlayerConfig)
            return intentX
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_player)
        init()
        registerPIPBroadcasts()
        requestPictureInPicturePermission()
    }

    lateinit var videoPlayerConfig: VideoPlayerConfig
    private fun init() {
        if (intent != null) {
            videoPlayerConfig =
                intent.getSerializableExtra("videoPlayerConfig") as VideoPlayerConfig
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun initializePlayer() {

        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this)

        val mediaSource =
            ExoPlayerHelper.buildMediaSource(this, Uri.parse(videoPlayerConfig.videoPath))

        simpleExoPlayer.prepare(mediaSource, false, false)
        simpleExoPlayer.playWhenReady = videoPlayerConfig.autoPlay

        binding.playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        binding.playerView.player = simpleExoPlayer
        binding.playerView.requestFocus()

        simpleExoPlayer.addListener(object : EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    STATE_READY -> {
                        binding.progressBar.visibility = View.INVISIBLE
                    }
                    STATE_IDLE -> {
                    }
                    STATE_ENDED -> {
                    }
                }
                if (playWhenReady && playbackState == STATE_READY) {
                    makeActionsPlayBased()
                    hideControllerPip()
                } else {
                    makeActionsPauseBased()
                    hideControllerPip()
                }
            }
        })

        binding.youtubeOverlay.performListener = object : ForwardViewOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.youtubeOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.youtubeOverlay.visibility = View.VISIBLE
            }
        }

        binding.playerView.activateDoubleTap(true)
            .setDoubleTapDelay(300)
            .setDoubleTapListener(binding.youtubeOverlay)
        binding.youtubeOverlay.setPlayer(simpleExoPlayer)
        binding.youtubeOverlay.animationDuration = 1000


        binding.playerView.setOnPinchListener(object : CustomExoPlayerView.OnPinchListener {
            override fun onPinchZoomOut() {
                showToast("Original")

                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                simpleExoPlayer.videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }

            override fun onPinchZoom() {
                showToast("Zoomed to Fill")
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                simpleExoPlayer.videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
        })

        simpleExoPlayer.addVideoListener(object : VideoListener {

            override fun onVideoSizeChanged(
                width: Int,
                height: Int,
                unappliedRotationDegrees: Int,
                pixelWidthHeightRatio: Float
            ) {
                super.onVideoSizeChanged(
                    width,
                    height,
                    unappliedRotationDegrees,
                    pixelWidthHeightRatio
                )
                videoWidth = width
                videoHeight = height
            }
        })

        simpleExoPlayer.repeatMode =
            if (videoPlayerConfig.loopVideo) REPEAT_MODE_ALL else REPEAT_MODE_OFF

        when (videoPlayerConfig.orientation) {
            VideoPlayerConfig.ORIENTATION_PORTRAIT_ONLY -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            VideoPlayerConfig.ORIENTATION_LANDSCAPE_ONLY -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            VideoPlayerConfig.ORIENTATION_USER_ORIENTATION -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            }
        }

    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(this@VideoPlayerActivity, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
    }

    private fun hideControllerPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInPictureInPictureMode) {
                binding.playerView.hideController()
            }
        }
    }

    private fun releasePlayer() {
        simpleExoPlayer.release()
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        binding.playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun requestPictureInPicturePermission() {
        if (supportsPiPMode()) {
            if (hasPIPFeature() && !canEnterPiPMode()) {
                AlertDialog.Builder(this)
                    .setMessage("To watch video while using another application. Please enable Picture in picture mode")
                    .setNegativeButton(
                        "Not for now"
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Allow Picture in Picture") { dialog, _ ->
                        dialog.dismiss()
                        requestPIPPermission()
                    }.show()
            }
        }
    }

    private fun requestPIPPermission() {
        val intent = Intent(
            "android.settings.PICTURE_IN_PICTURE_SETTINGS",
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, 123)
    }

    private fun goPIP() {
        if (videoPlayerConfig.allowPictureInPicture && canEnterPiPMode()) {
            if (supportsPiPMode()) {
                val pictureInPictureParams = if (simpleExoPlayer.playWhenReady) {
                    getActionPlayBased()
                } else {
                    getActionPausedBased()
                }
                enterPictureInPictureMode(pictureInPictureParams)
            }
        }
    }

    private fun getPipRatio(): Rational? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Rational(videoWidth, videoHeight)
        } else null
    }

    private fun supportsPiPMode(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    private fun hasPIPFeature(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else false
    }

    private fun canEnterPiPMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppOpsManagerCompat.MODE_ALLOWED == AppOpsManagerCompat.noteOpNoThrow(
                this,
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                packageName
            )
        } else false
    }

    private fun getPipActions(
        intentFilerAction: String,
        drawable: Int
    ): List<RemoteAction?>? {
        val intent = Intent(intentFilerAction)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remoteAction = RemoteAction(
                Icon.createWithResource(this, drawable),
                "Play/Pause",
                "As",
                pendingIntent
            )
            return listOf(remoteAction)
        }
        return null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        goPIP()
    }

    private fun registerPIPBroadcasts() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                simpleExoPlayer.playWhenReady = false
            }
        }, IntentFilter(PAUSE_BROADCAST))
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                simpleExoPlayer.playWhenReady = true
            }
        }, IntentFilter(PLAY_BROADCAST))
    }

    private fun makeActionsPlayBased() {
        if (supportsPiPMode()) {
            val mParams = getActionPlayBased()
            setPictureInPictureParams(mParams)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getActionPlayBased(): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setAspectRatio(getPipRatio())
            .setActions(
                getPipActions(
                    PAUSE_BROADCAST,
                    R.drawable.ic_pause
                )
            )
            .build()
    }

    private fun makeActionsPauseBased() {
        if (supportsPiPMode()) {
            val mParams = getActionPausedBased()
            setPictureInPictureParams(mParams)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getActionPausedBased(): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setAspectRatio(getPipRatio())
            .setActions(
                getPipActions(
                    PLAY_BROADCAST,
                    R.drawable.ic_play
                )
            )
            .build()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.playerView.hideController()
        } else {
            binding.playerView.showController()
        }
    }
}
