package net.corda.examples.bond.model

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Currency

data class Bond(val issuer: AbstractParty,
                val owner: AbstractParty,
                val issueDate: Instant,
                val maturityDate: Instant,
                val principal: Amount<Currency>,
                val couponRate: Double,
                val couponType: CouponType,
                override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(issuer, owner)

    fun withNewOwner(newOwner: AbstractParty) = copy(owner = newOwner)

    fun getNextPayableCoupon(date: Instant): Coupon? {
        payableCoupons.forEach { coupon ->
            if (coupon.redemptionDate.isAfter(date))
                return coupon
        }

        return null
    }

    private val payableCoupons: List<Coupon> = generatePayableCoupons()

    private fun generatePayableCoupons(): List<Coupon> {
        val couponList: MutableList<Coupon> = mutableListOf()
        val couponValue = principal.toDecimal().multiply(BigDecimal(couponRate))
        var couponRedemptionDate = issueDate

        while (couponRedemptionDate.isBefore(maturityDate)) {
            couponRedemptionDate = ZonedDateTime.ofInstant(couponRedemptionDate, ZoneOffset.UTC)
                    .plus(couponType.incrementStep, couponType.timeUnit).toInstant()
            couponList.add(Coupon(couponType, couponRate, couponValue, couponRedemptionDate))
        }

        return couponList
    }

    override fun toString(): String {
        val issuerString = (issuer as Party).name.organisation
        val ownerString = (owner as Party).name.organisation

        return "Bond $linearId issued by $issuerString, owned by $ownerString"
    }
}