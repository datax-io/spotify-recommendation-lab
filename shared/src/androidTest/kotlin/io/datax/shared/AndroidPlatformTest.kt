package io.datax.shared

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidPlatformTest {

    @Test
    fun testExample() {
        assertTrue("Check Android is mentioned", runBlocking { Platform().name }.contains("Android"))
    }
}
