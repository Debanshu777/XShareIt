package com.debanshu.xshareit

import android.app.Application
import com.debanshu.xshareit.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class XShareItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin{
            androidContext(this@XShareItApplication)
            androidLogger()
        }
    }
}