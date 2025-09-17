package org.adman.kmp.tiny.expr

import android.os.Build

actual fun getPlatform(): Platform {
    return AndroidPlatform()
}

private class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getTinyExprResult(): Double {
    return TinyExprJNI.eval("6 + 10* 2")
}