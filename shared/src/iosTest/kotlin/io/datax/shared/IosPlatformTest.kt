package io.datax.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class IosPlatformTest {

    @Test
    fun testExample() {
        assertTrue(Platform().name.contains("iOS"), "Check iOS is mentioned")
    }
}
