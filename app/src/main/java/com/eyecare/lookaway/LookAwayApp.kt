package com.eyecare.lookaway

import android.app.Application
import com.eyecare.lookaway.service.Notifications

class LookAwayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
    }
}
