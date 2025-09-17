package org.adman.kmp.tiny.expr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

interface TinyExprResult {
    val result: Double
}

expect fun getTinyExprResult(): Double