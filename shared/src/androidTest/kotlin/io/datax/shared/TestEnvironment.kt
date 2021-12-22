package io.datax.shared

actual object TestEnvironment

internal actual fun TestEnvironment.getEnv(key: String): String? = System.getenv(key)
