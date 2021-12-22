package io.datax.shared

import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject

actual typealias Preferences = NSObject

actual fun Preferences.getString(key: String): String? = NSUserDefaults.standardUserDefaults()
    .stringForKey(key)

actual fun Preferences.setString(key: String, value: String) = NSUserDefaults.standardUserDefaults()
    .setObject(value, forKey = key)

actual fun Preferences.removeString(key: String) = NSUserDefaults.standardUserDefaults()
    .removeObjectForKey(key)
