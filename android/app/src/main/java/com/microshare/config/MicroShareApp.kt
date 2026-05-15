package com.microshare.config

import android.app.Application

/**
 * Application类，全局初始化
 */
class MicroShareApp : Application() {
    companion object {
        lateinit var instance: MicroShareApp
            private set
        val context get() = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
