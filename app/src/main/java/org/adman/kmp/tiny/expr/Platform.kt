package org.adman.kmp.tiny.expr

interface Platform {
    val name: String
}

fun getPlatform(): Platform= AndroidPlatform()