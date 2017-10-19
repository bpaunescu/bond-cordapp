package net.corda.examples.bond.model

import net.corda.core.contracts.Amount
import net.corda.testing.ALICE
import net.corda.testing.BOB
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


class BondTest {

    @Test
    fun testConstructor() {
        val issuer = ALICE
        val owner = BOB
        val issueDate = Instant.now()
        val maturityDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(1, ChronoUnit.YEARS).toInstant()
        val principal = Amount.parseCurrency("£1000")
        val couponRate = 0.05
        val couponType = CouponType.ANNUALLY
        val bond = Bond(issuer, owner, issueDate, maturityDate, principal, couponRate, couponType)

        assert(ALICE in bond.participants)
        assert(BOB in bond.participants)

        var testDate = Instant.now()
        assertNotNull(bond.getNextPayableCoupon(testDate))
        assertNull(bond.getNextPayableCoupon(ZonedDateTime.ofInstant(testDate, ZoneOffset.UTC).plus(2, ChronoUnit.YEARS).toInstant()))
    }

    @Test
    fun testAnnualCouponGeneration() {
        val issuer = ALICE
        val owner = BOB
        val issueDate = Instant.now()
        val expectedNumberOfGeneratedCoupons = 10L
        val maturityDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(10, ChronoUnit.YEARS).toInstant()
        val principal = Amount.parseCurrency("£1000")
        val couponRate = 0.05
        val couponType = CouponType.ANNUALLY
        val expectedCouponValue = principal.toDecimal().multiply(BigDecimal(couponRate))
        val bond = Bond(issuer, owner, issueDate, maturityDate, principal, couponRate, couponType)

        var currentDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(1, ChronoUnit.MONTHS).toInstant()
        var actualNumberOfGeneratedCoupons = 0

        while (currentDate.isBefore(maturityDate)) {
            val nextCoupon = bond.getNextPayableCoupon(currentDate) ?: break
            actualNumberOfGeneratedCoupons++
            assertEquals(expectedCouponValue, nextCoupon.value)
            currentDate = ZonedDateTime.ofInstant(currentDate, ZoneOffset.UTC).plus(couponType.incrementStep, couponType.timeUnit).toInstant()
        }

        assertEquals(expectedNumberOfGeneratedCoupons, actualNumberOfGeneratedCoupons.toLong())
    }

    @Test
    fun testSemiAnnualCouponGeneration() {
        val locale = Locale.UK
        val issuer = ALICE
        val owner = BOB
        val issueDate = Instant.now()
        val expectedNumberOfGeneratedCoupons = 20L
        val maturityDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(10, ChronoUnit.YEARS).toInstant()
        val principal = Amount<Currency>(1000, BigDecimal.ONE, Currency.getInstance(locale))
        val couponRate = 0.05
        val couponType = CouponType.SEMIANNUALLY
        val expectedCouponValue = principal.toDecimal().multiply(BigDecimal(couponRate))
        val bond = Bond(issuer, owner, issueDate, maturityDate, principal, couponRate, couponType)

        var currentDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(1, ChronoUnit.MONTHS).toInstant()
        var actualNumberOfGeneratedCoupons = 0

        while (currentDate.isBefore(maturityDate)) {
            val nextCoupon = bond.getNextPayableCoupon(currentDate) ?: break
            actualNumberOfGeneratedCoupons++
            assertEquals(expectedCouponValue, nextCoupon.value)
            currentDate = ZonedDateTime.ofInstant(currentDate, ZoneOffset.UTC).plus(couponType.incrementStep, couponType.timeUnit).toInstant()
        }

        assertEquals(expectedNumberOfGeneratedCoupons, actualNumberOfGeneratedCoupons.toLong())
    }

    @Test
    fun testQuarterlyCouponGeneration() {
        val locale = Locale.UK
        val issuer = ALICE
        val owner = BOB
        val issueDate = Instant.now()
        val expectedNumberOfGeneratedCoupons = 20L
        val maturityDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(5, ChronoUnit.YEARS).toInstant()
        val principal = Amount<Currency>(1000, BigDecimal.ONE, Currency.getInstance(locale))
        val couponRate = 0.05
        val couponType = CouponType.QUARTERLY
        val expectedCouponValue = principal.toDecimal().multiply(BigDecimal(couponRate))
        val bond = Bond(issuer, owner, issueDate, maturityDate, principal, couponRate, couponType)

        var currentDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(1, ChronoUnit.MONTHS).toInstant()
        var actualNumberOfGeneratedCoupons = 0

        while (currentDate.isBefore(maturityDate)) {
            val nextCoupon = bond.getNextPayableCoupon(currentDate) ?: break
            actualNumberOfGeneratedCoupons++
            assertEquals(expectedCouponValue, nextCoupon.value)
            currentDate = ZonedDateTime.ofInstant(currentDate, ZoneOffset.UTC).plus(couponType.incrementStep, couponType.timeUnit).toInstant()
        }

        assertEquals(expectedNumberOfGeneratedCoupons, actualNumberOfGeneratedCoupons.toLong())
    }

    @Test
    fun testMonthlyCouponGeneration() {
        val locale = Locale.UK
        val issuer = ALICE
        val owner = BOB
        val issueDate = Instant.now()
        val expectedNumberOfGeneratedCoupons = 300L
        val maturityDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(25, ChronoUnit.YEARS).toInstant()
        val principal = Amount<Currency>(1000, BigDecimal.ONE, Currency.getInstance(locale))
        val couponRate = 0.05
        val couponType = CouponType.MONTHLY
        val expectedCouponValue = principal.toDecimal().multiply(BigDecimal(couponRate))
        val bond = Bond(issuer, owner, issueDate, maturityDate, principal, couponRate, couponType)

        var currentDate = issueDate
        var actualNumberOfGeneratedCoupons = 0

        while (currentDate.isBefore(maturityDate)) {
            val nextCoupon = bond.getNextPayableCoupon(currentDate) ?: break
            actualNumberOfGeneratedCoupons++
            assertEquals(expectedCouponValue, nextCoupon.value)
            currentDate = ZonedDateTime.ofInstant(currentDate, ZoneOffset.UTC).plus(couponType.incrementStep, couponType.timeUnit).toInstant()
        }

        assertEquals(expectedNumberOfGeneratedCoupons, actualNumberOfGeneratedCoupons.toLong())
    }

    @Test
    fun testWeeklyCouponGeneration() {
        val locale = Locale.UK
        val issuer = ALICE
        val owner = BOB
        val issueDate = Instant.now()
        val maturityDate = ZonedDateTime.ofInstant(issueDate, ZoneOffset.UTC).plus(6, ChronoUnit.WEEKS).toInstant()
        val expectedNumberOfGeneratedCoupons = 6L
        val principal = Amount<Currency>(1, BigDecimal(1000), Currency.getInstance(locale))
        val couponRate = 0.05
        val couponType = CouponType.WEEKLY
        val expectedCouponValue = principal.toDecimal().multiply(BigDecimal(couponRate))
        val bond = Bond(issuer, owner, issueDate, maturityDate, principal, couponRate, couponType)

        var currentDate = issueDate
        var actualNumberOfGeneratedCoupons = 0

        while (currentDate.isBefore(maturityDate)) {
            val nextCoupon = bond.getNextPayableCoupon(currentDate) ?: break
            actualNumberOfGeneratedCoupons++
            assertEquals(expectedCouponValue, nextCoupon.value)
            currentDate = ZonedDateTime.ofInstant(currentDate, ZoneOffset.UTC).plus(couponType.incrementStep, couponType.timeUnit).toInstant()
        }

        assertEquals(expectedNumberOfGeneratedCoupons, actualNumberOfGeneratedCoupons.toLong())
    }
}