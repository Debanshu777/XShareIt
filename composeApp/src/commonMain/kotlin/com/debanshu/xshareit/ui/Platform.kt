package com.debanshu.xshareit.ui

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform