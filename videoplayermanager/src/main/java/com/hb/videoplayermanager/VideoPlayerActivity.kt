package com.hb.videoplayermanager


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.hb.videoplayermanager.databinding.ActivityMediaPlayerBinding
import com.hb.videoplayermanager.databinding.PopupSpeedOptionBinding
import pl.droidsonroids.casty.Casty
import pl.droidsonroids.casty.MediaData
import java.util.*
import java.util.concurrent.TimeUnit


class VideoPlayerActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMediaPlayerBinding
    private lateinit var simpleExoPlayer: SimpleExoPlayer

    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    companion object {

        fun createIntent(context: Context, videoPlayerConfig: VideoPlayerConfig): Intent {
            val intentX = Intent(context, VideoPlayerActivity::class.java)
            intentX.putExtra("videoPlayerConfig", videoPlayerConfig)
            return intentX
        }
    }

    var casty: Casty? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        if (videoPlayerConfig.secureScreen) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_player)

        val castButton =
            findViewById<View>(R.id.media_route_button) as androidx.mediarouter.app.MediaRouteButton

        casty = Casty.create(this).withMiniController()
        casty?.setUpMediaRouteButton(castButton)

        castButton.setOnClickListener {
            if (casty?.isConnected == true) {
                val mediaData = MediaData.Builder(videoPlayerConfig.videoPath)
                    .setStreamType(MediaData.STREAM_TYPE_BUFFERED) //required
                    .setContentType("videos/mp4") //required
                    .setMediaType(MediaData.MEDIA_TYPE_MOVIE)
                    .setTitle("Video")
                    .setPosition(simpleExoPlayer.currentPosition)
                    .setAutoPlay(true)
                    .build()

                if (casty?.player?.isPlaying == false) {
                    casty?.player?.loadMediaAndPlay(mediaData)
                    casty?.player?.play()
                }
            }

        }

        requestDrmText()
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
            if (videoPlayerConfig.encryptionConfig != null)
                ExoPlayerHelper.buildEncryptedMediaSource(
                    this,
                    Uri.parse(videoPlayerConfig.videoPath),
                    videoPlayerConfig.encryptionConfig!!
                )
            else
                ExoPlayerHelper.buildMediaSource(this, Uri.parse(videoPlayerConfig.videoPath))

        simpleExoPlayer.prepare(mediaSource, false, false)
        simpleExoPlayer.seekTo(videoPlayerConfig.startTime.toLong())
        simpleExoPlayer.playWhenReady = videoPlayerConfig.autoPlay

        binding.playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        binding.playerView.player = simpleExoPlayer
        binding.playerView.requestFocus()

        simpleExoPlayer.addListener(object : Player.EventListener {
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
                        finish()
                    }
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
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                simpleExoPlayer.videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }

            override fun onPinchZoom() {
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

        findViewById<View>(R.id.exo_fullscreen_icon).setOnClickListener {
            val intent = Intent()
            intent.putExtra("seekPosition", simpleExoPlayer.contentPosition)
            intent.putExtra("isPlaying", simpleExoPlayer.isPlaying)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        findViewById<View>(R.id.exo_speed).setOnClickListener {
            showSpeedPopup(it)
        }

        binding.ivPip.visibility = View.GONE
    }

    private fun showSpeedPopup(view: View) {
        val popup = PopupWindow(this)
        popup.setBackgroundDrawable(BitmapDrawable())
        popup.setOutsideTouchable(true)
        val bindingX = PopupSpeedOptionBinding.inflate(LayoutInflater.from(this))
        popup.contentView = bindingX.root
        popup.showAtLocation(view, Gravity.BOTTOM, view.x.toInt() - 100, view.y.toInt() - 50)
        popup.isOutsideTouchable = true

        bindingX.onex.setOnClickListener {
            popup.dismiss()
            simpleExoPlayer.playSpeed = 1.0f
            invalidateSpeedTextView(1.0f)
        }

        bindingX.onepointfivex.setOnClickListener {
            popup.dismiss()
            simpleExoPlayer.playSpeed = 1.5f
            invalidateSpeedTextView(1.5f)
        }

        bindingX.twopointzero.setOnClickListener {
            popup.dismiss()
            simpleExoPlayer.playSpeed = 2.0f
            invalidateSpeedTextView(2.0f)
        }

        bindingX.pointfive.setOnClickListener {
            popup.dismiss()
            simpleExoPlayer.playSpeed = 0.5f
            invalidateSpeedTextView(0.5f)
        }
    }

    private fun invalidateSpeedTextView(speed: Float) {
        findViewById<TextView>(R.id.exo_speed).text = speed.toString() + "x"
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("seekPosition", simpleExoPlayer.contentPosition)
        intent.putExtra("isPlaying", simpleExoPlayer.isPlaying)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(this@VideoPlayerActivity, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP, 0, 0)
        toast.show()
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


    private val HIDE_DURATION = TimeUnit.SECONDS.toMillis(2)
    private val SHOW_DURATION = TimeUnit.SECONDS.toMillis(40)

    val hideHandler = Handler()
    val showHandler = Handler()

    var hideRunnable = Runnable {
        binding.tvRandomText!!.visibility = View.GONE
        showHandler.postDelayed(showRunnable, SHOW_DURATION)
    }

    var showRunnable = Runnable { showRandomly() }
    var maxWidth = 0
    var maxHeight = 0

    private fun requestDrmText() {
        //        String userId = CITCoreActivity.getSessionValue(this, AppConstants.SES_SES_USER_ID);
        val drmText: String = videoPlayerConfig.drmText
        if (TextUtils.isEmpty(drmText)) {
            return
        }

        val displayMetrics = DisplayMetrics()

        binding.tvRandomText.setText(drmText)
        binding.tvRandomText.measure(0, 0)
        binding.tvRandomText.visibility = View.GONE
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        maxWidth = displayMetrics.widthPixels - binding.tvRandomText.measuredWidth
        maxHeight = displayMetrics.heightPixels - binding.tvRandomText.measuredHeight
        showRandomly()
    }

    private fun showRandomly() {
        val random = Random()
        val leftMargin = random.nextInt(maxWidth)
        val topMargin = random.nextInt(maxHeight)
        val lp = binding.tvRandomText.getLayoutParams() as FrameLayout.LayoutParams
        lp.leftMargin = leftMargin
        lp.topMargin = topMargin
        binding.tvRandomText.layoutParams = lp
        binding.tvRandomText.visibility = View.VISIBLE
        hideHandler.postDelayed(hideRunnable, HIDE_DURATION)
    }

}
