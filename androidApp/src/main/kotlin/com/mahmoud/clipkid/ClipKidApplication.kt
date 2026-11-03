package com.mahmoud.clipkid

import android.app.Application
import com.mahmoud.clipkid.di.initKoinAndroid

class ClipKidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
