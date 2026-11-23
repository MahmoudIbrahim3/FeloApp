package com.mahmoud.feelo

import android.app.Application
import com.mahmoud.feelo.di.initKoinAndroid

class FeeloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
