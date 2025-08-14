package com.debanshu.xshareit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform