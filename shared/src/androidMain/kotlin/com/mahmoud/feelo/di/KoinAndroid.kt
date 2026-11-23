package com.mahmoud.feelo.di

import android.content.Context
import org.koin.android.ext.koin.androidContext

/** Android entry point: gives Koin the application context (Room, DataStore, Keystore later). */
fun initKoinAndroid(context: Context) {
    initKoin { androidContext(context.applicationContext) }
}
