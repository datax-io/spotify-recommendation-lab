package io.datax.shared

actual class Platform actual constructor() {

    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"

}
