package net.corda.examples.bond.model

import net.corda.core.serialization.CordaSerializable
import java.time.temporal.ChronoUnit

@CordaSerializable
enum class CouponType(val timeUnit: ChronoUnit, val incrementStep: Long){
    WEEKLY(ChronoUnit.WEEKS, 1),
    MONTHLY(ChronoUnit.MONTHS, 1),
    QUARTERLY(ChronoUnit.MONTHS, 3),
    SEMIANNUALLY(ChronoUnit.MONTHS, 6),
    ANNUALLY(ChronoUnit.YEARS, 1)
}