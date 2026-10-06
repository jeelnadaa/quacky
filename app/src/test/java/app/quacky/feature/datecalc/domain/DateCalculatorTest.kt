package app.quacky.feature.datecalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DateCalculatorTest {

    @Test
    fun `calculateAge computes exact years, months and days`() {
        val birth = LocalDate.of(2000, 1, 15)
        val target = LocalDate.of(2025, 3, 20)
        val age = DateCalculator.calculateAge(birth, target)

        assertEquals(25, age.years)
        assertEquals(2, age.months)
        assertEquals(5, age.days)
    }

    @Test
    fun `calculateDifference computes calendar days and business days`() {
        // Monday 2026-03-02 to Friday 2026-03-06 (Mon to Fri = 4 intervals, 5 if including end date)
        val start = LocalDate.of(2026, 3, 2)
        val end = LocalDate.of(2026, 3, 6)

        val diffExclusive = DateCalculator.calculateDifference(start, end, includeEndDate = false)
        assertEquals(4, diffExclusive.totalDays)
        assertEquals(4, diffExclusive.businessDays)

        val diffInclusive = DateCalculator.calculateDifference(start, end, includeEndDate = true)
        assertEquals(5, diffInclusive.totalDays)
        assertEquals(5, diffInclusive.businessDays)
    }

    @Test
    fun `business days excludes weekends correctly`() {
        // Friday 2026-03-06 to Monday 2026-03-09 (Fri, Sat, Sun, Mon)
        val start = LocalDate.of(2026, 3, 6)
        val end = LocalDate.of(2026, 3, 9)

        val diff = DateCalculator.calculateDifference(start, end, includeEndDate = true)
        assertEquals(4, diff.totalDays)
        // Friday + Monday = 2 business days
        assertEquals(2, diff.businessDays)
    }

    @Test
    fun `addOrSubtract adds and subtracts months and days accurately`() {
        val start = LocalDate.of(2026, 1, 31)
        val plusOneMonth = DateCalculator.addOrSubtract(start, months = 1, isAdd = true)
        // Jan 31 + 1 month in 2026 (non-leap) -> Feb 28
        assertEquals(LocalDate.of(2026, 2, 28), plusOneMonth)
    }

    @Test
    fun `getDayInfo reports correct day of year and quarter`() {
        val date = LocalDate.of(2024, 2, 29) // 2024 is leap year
        val info = DateCalculator.getDayInfo(date)

        assertTrue(info.isLeapYear)
        assertEquals(DayOfWeek.THURSDAY, info.dayOfWeek)
        assertEquals(60, info.dayOfYear)
        assertEquals(1, info.quarter)
    }
}
