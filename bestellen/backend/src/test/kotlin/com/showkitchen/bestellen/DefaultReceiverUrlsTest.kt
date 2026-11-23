package com.showkitchen.bestellen

import com.showkitchen.bestellen.order.Receiver
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultReceiverUrlsTest {
    @Test
    fun `every receiver has a default address, its stub in the network of the sub-product`() {
        for (receiver in Receiver.entries) {
            assertEquals("http://stub-${receiver.value}:8080", DEFAULT_RECEIVER_URLS[receiver], "default address of ${receiver.value}")
        }
    }
}
