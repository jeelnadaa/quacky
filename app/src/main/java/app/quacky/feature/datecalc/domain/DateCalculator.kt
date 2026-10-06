package app.quacky.feature.datecalc.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalMonths: Long,
    val totalWeeks: Long,
    val totalDays: Long,
    val totalHours: Long,
    val nextBirthdayDays: Long,
    val nextBirthdayWeekday: DayOfWeek
)

data class DateDifferenceResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long,
    val totalWeeks: Long,
    val businessDays: Long
)

data class DayInfoResult(
    val dayOfWeek: DayOfWeek,
    val dayOfYear: Int,
    val isoWeekNumber: Int,
    val quarter: Int,
    val isLeapYear: Boolean,
    val daysRemainingInYear: Int
)

object DateCalculator {

    /**
     * Compute exact age and birthday countdown.
     */
    fun calculateAge(birthDate: LocalDate, targetDate: LocalDate = LocalDate.now()): AgeResult {
        val period = Period.between(birthDate, targetDate)
        val totalMonths = ChronoUnit.MONTHS.between(birthDate, targetDate)
        val totalDays = ChronoUnit.DAYS.between(birthDate, targetDate)
        val totalWeeks = totalDays / 7
        val totalHours = totalDays * 24

        // Next birthday calculation
        var nextBday = birthDate.withYear(targetDate.year)
        if (nextBday.isBefore(targetDate) || nextBday.isEqual(targetDate)) {
            nextBday = nextBday.plusYears(1)
        }
        val daysUntilNextBday = ChronoUnit.DAYS.between(targetDate, nextBday)

        return AgeResult(
            years = period.years,
            months = period.months,
            days = period.days,
            totalMonths = totalMonths,
            totalWeeks = totalWeeks,
            totalDays = totalDays,
            totalHours = totalHours,
            nextBirthdayDays = daysUntilNextBday,
            nextBirthdayWeekday = nextBday.dayOfWeek
        )
    }

    /**
     * Compute difference between two dates with optional end date inclusion and business days.
     */
    fun calculateDifference(
        start: LocalDate,
        end: LocalDate,
        includeEndDate: Boolean = false,
        holidays: Set<LocalDate> = emptySet()
    ): DateDifferenceResult {
        val effectiveEnd = if (includeEndDate) end.plusDays(1) else end
        val period = Period.between(start, effectiveEnd)
        val totalDays = ChronoUnit.DAYS.between(start, effectiveEnd)
        val totalWeeks = totalDays / 7

        // Business days calculation (Mon-Fri excluding holidays)
        var businessDaysCount = 0L
        var cur = start
        while (cur.isBefore(effectiveEnd)) {
            val dow = cur.dayOfWeek
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(cur)) {
                businessDaysCount++
            }
            cur = cur.plusDays(1)
        }

        return DateDifferenceResult(
            years = period.years,
            months = period.months,
            days = period.days,
            totalDays = totalDays,
            totalWeeks = totalWeeks,
            businessDays = businessDaysCount
        )
    }

    /**
     * Add or subtract years, months, weeks, days, or business days.
     */
    fun addOrSubtract(
        start: LocalDate,
        years: Int = 0,
        months: Int = 0,
        weeks: Int = 0,
        days: Int = 0,
        businessDays: Int = 0,
        isAdd: Boolean = true
    ): LocalDate {
        var result = start
        val sign = if (isAdd) 1 else -1

        if (years != 0) result = result.plusYears((years * sign).toLong())
        if (months != 0) result = result.plusMonths((months * sign).toLong())
        if (weeks != 0) result = result.plusWeeks((weeks * sign).toLong())
        if (days != 0) result = result.plusDays((days * sign).toLong())

        if (businessDays != 0) {
            var bDaysRemaining = kotlin.math.abs(businessDays)
            val step = if (sign > 0) 1L else -1L
            while (bDaysRemaining > 0) {
                result = result.plusDays(step)
                if (result.dayOfWeek != DayOfWeek.SATURDAY && result.dayOfWeek != DayOfWeek.SUNDAY) {
                    bDaysRemaining--
                }
            }
        }

        return result
    }

    /**
     * Detailed information about any date.
     */
    fun getDayInfo(date: LocalDate): DayInfoResult {
        val totalDaysInYear = if (date.isLeapYear) 366 else 365
        val daysRemaining = totalDaysInYear - date.dayOfYear
        val quarter = (date.monthValue - 1) / 3 + 1
        val isoWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

        return DayInfoResult(
            dayOfWeek = date.dayOfWeek,
            dayOfYear = date.dayOfYear,
            isoWeekNumber = isoWeek,
            quarter = quarter,
            isLeapYear = date.isLeapYear,
            daysRemainingInYear = daysRemaining
        )
    }
}
