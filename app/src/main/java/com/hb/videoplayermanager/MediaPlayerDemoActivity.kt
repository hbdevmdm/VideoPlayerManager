package com.hb.videoplayermanager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.hb.hbandroidsample.DialogUtil
import com.hb.hbandroidsample.FileUtils

import com.hb.videoplayermanager.databinding.ActivityMediaPlayerDemoBinding
import com.master.permissionhelper.PermissionHelper

class MediaPlayerDemoActivity : AppCompatActivity() {

    lateinit var binding: ActivityMediaPlayerDemoBinding
    private var permissionHelper: PermissionHelper? = null
    private var captureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_player_demo)
        binding.tvPlay.setOnClickListener {
            permissionHelper?.requestAll {
                openGallery()
            }
        }
        init()
        makeItFullscreen(window)
        changeStatusbarIcon(true)

        openPlayer("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
    }

    private fun makeItFullscreen(window: Window) {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun changeStatusbarIcon(lightIcon: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags: Int = window.decorView.systemUiVisibility
            if (lightIcon) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun init() {
        permissionHelper = PermissionHelper(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
        permissionHelper?.denied { boolean ->
            if (boolean) {
                DialogUtil.showAlertDialogAction(
                    this,
                    getString(R.string.permission_need_message),
                    object : DialogUtil.IL {
                        override fun onSuccess() {
                            permissionHelper?.openAppDetailsActivity()
                        }

                        override fun onCancel(isNeutral: Boolean) {
                        }

                    },
                    getString(R.string.settings),
                    getString(
                        R.string.cancel
                    )
                )
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    val selectedImageUri = data?.data
                    captureUri = selectedImageUri
                    if (captureUri != null) {
                        openPlayer(
                            FileUtils.getPath(
                                this@MediaPlayerDemoActivity,
                                captureUri!!
                            ) ?: ""
                        )
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            captureUri = null
        }
    }

    private fun openPlayer(path: String) {
        val intent = Intent(this, MediaPlayerActivity::class.java)
        intent.putExtra("autoPlay", true)
        intent.putExtra("allowPictureInPicture", true)
        intent.putExtra(EXTRA_PATH, path)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val REQUEST_CODE_GALLERY = 500
        const val EXTRA_PATH = "EXTRA_PATH"
    }
}
