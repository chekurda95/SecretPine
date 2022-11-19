package com.chekurda.secret_pine.app

import android.app.Application

/**
 * [Application] Secret pine.
 */
class SecretPineApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PluginSystem.initialize(this)
    }
}