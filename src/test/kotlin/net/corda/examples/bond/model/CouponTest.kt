package net.corda.examples.bond.model

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.Instant

class CouponTest {
    @Test
    fun testConstructor() {
        val coupon = Coupon(CouponType.ANNUALLY, 0.07, BigDecimal(1000), Instant.now())
        assertEquals(CouponType.ANNUALLY, coupon.type)
        assertEquals(0.07, coupon.rate, 0.0)
        assertEquals(BigDecimal(1000), coupon.value)
    }
}