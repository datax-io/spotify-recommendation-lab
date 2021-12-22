package io.datax.shared

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

actual typealias Preferences = Activity

fun Preferences.editPref(block: (SharedPreferences.Editor) -> Unit) =
    this.getSharedPreferences("prefs", MODE_PRIVATE).edit()
        .let {
            block(it)
            it.apply()
        }

actual fun Preferences.getString(key: String): String? = this.getSharedPreferences("prefs", MODE_PRIVATE)
    .getString(key, "")
    .takeIf { it?.length != 0 }

actual fun Preferences.setString(key: String, value: String): Unit = editPref { it.putString(key, value) }

actual fun Preferences.removeString(key: String): Unit = editPref { it.remove(key) }
