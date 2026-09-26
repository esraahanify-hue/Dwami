package com.schoolschedule.app.domain

import java.time.LocalDate
import java.time.LocalTime

enum class UserRole { STUDENT, TEACHER }

data class UserSelection(val role: UserRole, val value: String)

data class Period(val id: String, val start: LocalTime, val end: LocalTime)

data class ScheduleData(
    val schedule: Map<String, Map<String, List<String>>>,
    val periods: List<Period>,
    val holidays: Set<LocalDate>
)

data class Lesson(
    val period: Period,
    val subject: String,
    val grades: List<String>
)

sealed interface DayStatus {
    data object Holiday : DayStatus
    data object Finished : DayStatus
    data class BeforeStart(val next: Lesson) : DayStatus
    data class InLesson(val current: Lesson, val remainingSeconds: Long, val next: Lesson?) : DayStatus
    data class Break(val next: Lesson?, val remainingSeconds: Long?) : DayStatus
}

data class DataLoadResult(val data: ScheduleData?, val errors: List<String>)

val arabicDays = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

fun normalizeSubject(value: String): String = value.trim().lowercase()
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')

fun isLesson(value: String) = value.trim().isNotEmpty() && value.trim() != "-"
