package com.gmail.linhqng207.smack.Controller

import android.app.Application
import com.gmail.linhqng207.smack.Utilities.SharedPrefs

class App : Application() {

    companion object{
        lateinit var prefs: SharedPrefs
    }

    override fun onCreate() {

        prefs = SharedPrefs(applicationContext)
        super.onCreate()
    }
}