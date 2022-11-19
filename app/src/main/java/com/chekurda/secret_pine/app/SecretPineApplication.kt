package com.chekurda.secret_pine.app

import android.app.Application
import android.util.Log
import io.reactivex.plugins.RxJavaPlugins

/**
 * [Application] Secret pine.
 */
class SecretPineApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PluginSystem.initialize(this)
        RxJavaPlugins.setErrorHandler { error -> Log.e("RxError", "$error") }
    }
}