package io.datax.shared

import kotlin.test.Test

class AndroidParcelHelperTest {

    @Test
    fun `get uri`() {
        val parcelHelper = ParcelHelper()
        val url = parcelHelper.getAuthUrl()
        println(url)
    }

}
