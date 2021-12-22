package io.datax.shared

expect object TestEnvironment

internal expect fun TestEnvironment.getEnv(key: String): String?
