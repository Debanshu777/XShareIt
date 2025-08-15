package com.debanshu.xshareit

import androidx.compose.ui.window.ComposeUIViewController
import com.debanshu.xshareit.di.initKoin
import com.debanshu.xshareit.ui.App

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}