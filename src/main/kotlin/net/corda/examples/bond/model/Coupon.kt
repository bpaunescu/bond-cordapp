package net.corda.examples.bond.model

import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.time.Instant

@CordaSerializable
data class Coupon(val type: CouponType,
                  val rate: Double,
                  val value: BigDecimal,
                  val redemptionDate: Instant)