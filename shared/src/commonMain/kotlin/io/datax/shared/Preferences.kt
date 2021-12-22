package io.datax.shared

expect class Preferences

internal expect fun Preferences.getString(key: String): String?
internal expect fun Preferences.setString(key: String, value: String)
internal expect fun Preferences.removeString(key: String)
