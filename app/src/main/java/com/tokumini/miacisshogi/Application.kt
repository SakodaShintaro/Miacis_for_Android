package com.tokumini.miacisshogi

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初期化
        AndroidThreeTen.init(this)
    }
}