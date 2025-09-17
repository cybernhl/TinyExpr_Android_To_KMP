package org.adman.kmp.tiny.expr

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getTinyExprResult(): Double {
    return TinyExprJNI.eval("7 + 30* 2")
}