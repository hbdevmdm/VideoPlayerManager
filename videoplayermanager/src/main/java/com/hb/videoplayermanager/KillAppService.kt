package com.hb.videoplayermanager

import android.app.Service
import android.content.Intent
import android.os.IBinder

class KillAppService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }
}