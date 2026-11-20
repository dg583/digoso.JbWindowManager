package com.intellij.openapi.diagnostic

/** Stand-in for the platform logger so ProfileStore compiles without the IDE (CI only, never packaged). */
class Logger private constructor() {
    fun warn(message: String, t: Throwable?) {}

    companion object {
        @JvmStatic
        fun getInstance(c: Class<*>): Logger = Logger()
    }
}
