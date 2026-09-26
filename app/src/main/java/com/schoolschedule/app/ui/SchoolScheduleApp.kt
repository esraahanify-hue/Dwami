@file:OptIn(ExperimentalMaterial3Api::class)

package com.schoolschedule.app.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.schoolschedule.app.data.AppPreferences
import com.schoolschedule.app.data.StorageRepository
import com.schoolschedule.app.domain.DataLoadResult
import com.schoolschedule.app.domain.DayStatus
import com.schoolschedule.app.domain.Lesson
import com.schoolschedule.app.domain.ScheduleData
import com.schoolschedule.app.domain.ScheduleEngine
import com.schoolschedule.app.domain.UserRole
import com.schoolschedule.app.domain.UserSelection
import com.schoolschedule.app.notifications.NotificationScheduler
import com.schoolschedule.app.ui.theme.SchoolScheduleTheme
import com.schoolschedule.app.ui.theme.ThemeMode
import com.schoolschedule.app.ui.theme.subjectColor
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class Screen { HOME, TODAY, FULL, SETTINGS }

class AppController(private val context: Context) {
    private val repository = StorageRepository(context)
    private val preferences = AppPreferences(context)
    var data by mutableStateOf<ScheduleData?>(null)
        private set
    var errors by mutableStateOf<List<String>>(emptyList())
        private set
    var selection by mutableStateOf(preferences.selection)
        private set
    var screen by mutableStateOf(Screen.HOME)
    var notifyStart by mutableStateOf(preferences.notifyStart)
    var notifyEnd by mutableStateOf(preferences.notifyEnd)
    var notifyBefore by mutableStateOf(preferences.notifyBefore)
    var themeMode by mutableStateOf(runCatching { ThemeMode.valueOf(preferences.themeMode) }.getOrDefault(ThemeMode.SYSTEM))

    init { reload() }
    fun path() = repository.dataPath()
    fun reload() {
        val result: DataLoadResult = repository.load()
        data = result.data
        errors = result.errors
        if (data != null && selection != null) NotificationScheduler.reschedule(context)
    }
    fun restore() { errors = repository.restoreDefaults(); reload() }
    fun saveSelection(value: UserSelection) { preferences.selection = value; selection = value; screen = Screen.HOME; NotificationScheduler.reschedule(context) }
    fun clearSelection() { preferences.selection = null; selection = null; NotificationScheduler.cancelAll(context) }
    fun updateNotifications(start: Boolean = notifyStart, end: Boolean = notifyEnd, before: Boolean = notifyBefore) {
        notifyStart = start; notifyEnd = end; notifyBefore = before
        preferences.notifyStart = start; preferences.notifyEnd = end; preferences.notifyBefore = before
        NotificationScheduler.reschedule(context)
    }
    fun setThemeMode(mode: ThemeMode) { themeMode = mode; preferences.themeMode = mode.name }
}

@Composable
fun SchoolScheduleApp(controller: AppController) {
    val darkTheme = when (controller.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    SchoolScheduleTheme(darkTheme = darkTheme, dynamicColor = false) {
        Crossfade(targetState = whichRoot(controller), label = "root") { root ->
            when (root) {
                Root.PROBLEM -> DataProblemScreen(controller)
                Root.SETUP -> SetupScreen(controller)
                Root.MAIN -> MainScaffold(controller)
            }
        }
    }
}

private enum class Root { PROBLEM, SETUP, MAIN }
private fun whichRoot(controller: AppController): Root = when {
    controller.data == null -> Root.PROBLEM
    controller.selection == null -> Root.SETUP
    else -> Root.MAIN
}

@Composable
private fun DataProblemScreen(controller: AppController) = Scaffold(topBar = { TitleBar("دوامي المدرسي") }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("تعذر تحميل بيانات الدوام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        controller.errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("مسار الملفات: ${controller.path()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Button(onClick = controller::reload, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("إعادة تحميل ملفات البيانات") }
        OutlinedButton(onClick = controller::restore, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = MaterialTheme.shapes.medium) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text("استعادة البيانات الافتراضية") }
    }
}

@Composable
private fun SetupScreen(controller: AppController) {
    val engine = remember(controller.data) { ScheduleEngine(requireNotNull(controller.data)) }
    var role by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { TitleBar("إعداد الدوام") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(
                    if (role == null) "مرحبًا 👋 اختر نوع المستخدم" else if (role == UserRole.STUDENT) "اختر الصف" else "اختر المادة",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                )
            }
            item { Text("سيتم حفظ اختيارك محليًا ويمكن تغييره لاحقًا من الإعدادات.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (role == null) {
                item { OptionCard("طالب", "لعرض جدول صفك فقط", Icons.Default.School) { role = UserRole.STUDENT } }
                item { OptionCard("مدرس", "لعرض برنامج المادة التي تدرّسها", Icons.Default.Person) { role = UserRole.TEACHER } }
            } else {
                val values = if (role == UserRole.STUDENT) engine.grades() else engine.subjects()
                if (values.size > 8) item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = MaterialTheme.shapes.medium, singleLine = true,
                    )
                }
                val filtered = values.filter { it.contains(query.trim(), ignoreCase = true) }
                items(filtered) { value ->
                    OptionCard(value, if (role == UserRole.STUDENT) "عرض جدول $value" else "برنامج مادة $value", if (role == UserRole.STUDENT) Icons.Default.School else Icons.Default.Person) {
                        controller.saveSelection(UserSelection(requireNotNull(role), value))
                    }
                }
                if (filtered.isEmpty()) item { EmptyCard("لا توجد نتائج مطابقة") }
                item { TextButton(onClick = { role = null; query = "" }) { Text("رجوع لاختيار النوع") } }
            }
        }
    }
}

private val bottomDestinations = listOf(
    Triple(Screen.HOME, "الرئيسية", Icons.Default.Home),
    Triple(Screen.TODAY, "اليوم", Icons.Default.CalendarToday),
    Triple(Screen.FULL, "الجدول", Icons.Default.CalendarMonth),
    Triple(Screen.SETTINGS, "الإعدادات", Icons.Default.Settings),
)

@Composable
private fun MainScaffold(controller: AppController) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { (screen, label, icon) ->
                    NavigationBarItem(
                        selected = controller.screen == screen,
                        onClick = { controller.screen = screen },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnimatedContent(
                targetState = controller.screen,
                label = "screen",
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(controller)
                    Screen.TODAY -> TableScreen(controller, todayOnly = true)
                    Screen.FULL -> TableScreen(controller, todayOnly = false)
                    Screen.SETTINGS -> SettingsScreen(controller)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(controller: AppController) {
    val data = requireNotNull(controller.data); val selection = requireNotNull(controller.selection); val engine = remember(data) { ScheduleEngine(data) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }
    val status = engine.status(now, selection)
    val todayLessons = remember(now.toLocalDate(), data) { engine.lessonsFor(now.toLocalDate(), selection) }
    Scaffold(topBar = { TitleBar("دوامي المدرسي") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { HeaderDate(engine, now.toLocalDate(), selection) }
            item {
                AnimatedContent(targetState = status::class, label = "status") { _ ->
                    CurrentStatusCard(status, selection.role == UserRole.TEACHER)
                }
            }
            if (todayLessons.isNotEmpty()) {
                item { Text("خط سير اليوم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                item { DayTimeline(todayLessons, now.toLocalTime()) }
            }
        }
    }
}

@Composable
private fun DayTimeline(lessons: List<Lesson>, now: LocalTime) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(lessons) { lesson ->
            val isPast = now.isAfter(lesson.period.end)
            val isCurrent = !now.isBefore(lesson.period.start) && now.isBefore(lesson.period.end)
            val colors = subjectColor(lesson.subject, dark)
            Card(
                modifier = Modifier.width(132.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else colors.container.copy(alpha = if (isPast) 0.45f else 1f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 0.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPast) Icon(Icons.Default.CheckCircle, null, tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else colors.onContainer, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "الحصة ${lesson.period.id}", style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else colors.onContainer,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        lesson.subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else colors.onContainer, maxLines = 1,
                    )
                    Text(
                        "${lesson.period.start}", style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else colors.onContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderDate(engine: ScheduleEngine, date: LocalDate, selection: UserSelection) = Column {
    Text("${engine.dayName(date)} ${arabicDate(date)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (selection.role == UserRole.STUDENT) Icons.Default.School else Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(if (selection.role == UserRole.STUDENT) "الصف: ${selection.value}" else "المادة: ${selection.value}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CurrentStatusCard(status: DayStatus, teacher: Boolean) = Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.fillMaxWidth(),
) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            DayStatus.Holiday -> { Text("اليوم عطلة 🌤️", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("الجمعة والسبت والعطل الاستثنائية لا تحتوي على حصص.") }
            DayStatus.Finished -> { Text("انتهى الدوام اليوم ✅", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("يمكنك مراجعة جدول اليوم أو الجدول الكامل من الأسفل.") }
            is DayStatus.BeforeStart -> { Text("لم يبدأ الدوام بعد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); NextLesson(status.next, status.next.period.start, status = null, teacher = teacher) }
            is DayStatus.Break -> {
                Text("استراحة ☕", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                status.next?.let { NextLesson(it, it.period.start, status.remainingSeconds, teacher) } ?: Text("لا توجد حصة قادمة اليوم")
            }
            is DayStatus.InLesson -> {
                Text("الحصة الحالية", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                LessonDetails(status.current, teacher, prominent = true)
                val totalSeconds = java.time.Duration.between(status.current.period.start, status.current.period.end).seconds.coerceAtLeast(1)
                val progress = (1f - status.remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Text("باقي ${formatDuration(status.remainingSeconds)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Divider(Modifier.padding(vertical = 4.dp))
                Text("الحصة القادمة", style = MaterialTheme.typography.labelLarge)
                status.next?.let { NextLesson(it, it.period.start, null, teacher) } ?: Text("لا توجد حصة قادمة اليوم")
            }
        }
    }
}

@Composable
private fun NextLesson(lesson: Lesson, starts: LocalTime, status: Long?, teacher: Boolean) {
    LessonDetails(lesson, teacher)
    Text("تبدأ ${starts}", fontWeight = FontWeight.Medium)
    if (status != null) Text("بعد ${formatDuration(status)}")
}

@Composable
private fun LessonDetails(lesson: Lesson, teacher: Boolean, prominent: Boolean = false) = Column {
    Text("الحصة ${lesson.period.id} — ${lesson.subject}", style = if (prominent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (teacher) Text(lesson.grades.joinToString(" + "), color = MaterialTheme.colorScheme.onPrimaryContainer)
}

@Composable
private fun TableScreen(controller: AppController, todayOnly: Boolean) {
    val data = requireNotNull(controller.data); val selection = requireNotNull(controller.selection); val engine = remember(data) { ScheduleEngine(data) }
    val days = if (todayOnly) listOf(engine.dayName(LocalDate.now())) else listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    var teacherTab by rememberSaveable { mutableStateOf(0) }
    val title = if (todayOnly) "جدول اليوم" else if (selection.role == UserRole.TEACHER) "الجدول" else "الجدول الكامل"
    Scaffold(topBar = { TitleBar(title) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!todayOnly && selection.role == UserRole.TEACHER) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SegmentedButton(selected = teacherTab == 0, onClick = { teacherTab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("كل الصفوف") }
                    SegmentedButton(selected = teacherTab == 1, onClick = { teacherTab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("برنامجي الأسبوعي") }
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (todayOnly && engine.isHoliday(LocalDate.now())) item { EmptyCard("اليوم عطلة") }
                else if (!todayOnly && selection.role == UserRole.TEACHER && teacherTab == 1) {
                    items(days) { day -> TeacherDayCard(day, engine.lessonsForDay(day, selection)) }
                } else items(days) { day ->
                    val now = if (todayOnly) LocalTime.now() else null
                    if (!todayOnly && selection.role == UserRole.TEACHER) PeriodGrid(day, engine.allLessonsByPeriod(day))
                    else DayTable(day, engine.lessonsForDay(day, selection), selection.role == UserRole.TEACHER, now)
                }
            }
        }
    }
}

@Composable
private fun TeacherDayCard(day: String, lessons: List<Lesson>) = Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
    Column(Modifier.padding(16.dp)) {
        Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        if (lessons.isEmpty()) Text("لا توجد حصة للمادة", color = MaterialTheme.colorScheme.onSurfaceVariant)
        lessons.forEach { lesson ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PeriodChip(lesson.period.id)
                Spacer(Modifier.width(10.dp))
                Text(lesson.grades.joinToString(" + "), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text("${lesson.period.start}–${lesson.period.end}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PeriodGrid(day: String, rows: List<Pair<com.schoolschedule.app.domain.Period, List<Lesson>>>) = Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(16.dp)) {
        Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        rows.forEach { (period, lessons) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                PeriodChip(period.id)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    if (lessons.isEmpty()) Text("لا توجد حصة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else lessons.groupBy { it.subject }.forEach { (subject, subjectLessons) ->
                        Text(subject, fontWeight = FontWeight.Bold)
                        Text(subjectLessons.joinToString(" + ") { it.grades.first() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${period.start}–${period.end}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (period != rows.last().first) Divider()
        }
    }
}

@Composable
private fun DayTable(day: String, lessons: List<Lesson>, showGrades: Boolean, now: LocalTime?) = Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(Modifier.padding(16.dp)) {
        Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        if (lessons.isEmpty()) Text("لا توجد حصص مسجلة", color = MaterialTheme.colorScheme.onSurfaceVariant)
        lessons.forEachIndexed { i, lesson ->
            val isCurrent = now != null && !now.isBefore(lesson.period.start) && now.isBefore(lesson.period.end)
            val colors = subjectColor(lesson.subject, dark)
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isCurrent) colors.container else Color.Transparent, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = if (isCurrent) 8.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PeriodChip(lesson.period.id)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(lesson.subject, fontWeight = FontWeight.Bold, color = if (isCurrent) colors.onContainer else MaterialTheme.colorScheme.onSurface)
                    if (showGrades) Text(lesson.grades.joinToString(" + "), style = MaterialTheme.typography.bodySmall, color = if (isCurrent) colors.onContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${lesson.period.start}–${lesson.period.end}", style = MaterialTheme.typography.bodySmall, color = if (isCurrent) colors.onContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (i < lessons.lastIndex) Divider()
        }
    }
}

@Composable
private fun PeriodChip(id: String) = AssistChip(
    onClick = {}, label = { Text(id) },
    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
)

@Composable
private fun SettingsScreen(controller: AppController) = Scaffold(topBar = { TitleBar("الإعدادات") }) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionHeader("المظهر") }
        item {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(16.dp)) {
                    Text("وضع الألوان", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = controller.themeMode == ThemeMode.SYSTEM, onClick = { controller.setThemeMode(ThemeMode.SYSTEM) }, shape = SegmentedButtonDefaults.itemShape(0, 3), icon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.size(18.dp)) }) { Text("تلقائي") }
                        SegmentedButton(selected = controller.themeMode == ThemeMode.LIGHT, onClick = { controller.setThemeMode(ThemeMode.LIGHT) }, shape = SegmentedButtonDefaults.itemShape(1, 3), icon = { Icon(Icons.Default.LightMode, null, modifier = Modifier.size(18.dp)) }) { Text("فاتح") }
                        SegmentedButton(selected = controller.themeMode == ThemeMode.DARK, onClick = { controller.setThemeMode(ThemeMode.DARK) }, shape = SegmentedButtonDefaults.itemShape(2, 3), icon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(18.dp)) }) { Text("داكن") }
                    }
                }
            }
        }
        item { SectionHeader("المستخدم") }
        item { SettingItem(Icons.Default.SwapHoriz, "تغيير نوع المستخدم أو الصف/المادة", controller.selection?.value.orEmpty()) { controller.clearSelection() } }
        item { SectionHeader("الإشعارات") }
        item { ToggleItem(Icons.Default.NotificationsActive, "إشعار عند بداية الحصة", controller.notifyStart) { controller.updateNotifications(start = it) } }
        item { ToggleItem(Icons.Default.NotificationsActive, "إشعار عند نهاية الحصة", controller.notifyEnd) { controller.updateNotifications(end = it) } }
        item { ToggleItem(Icons.Default.NotificationsActive, "إشعار قبل الحصة بـ5 دقائق", controller.notifyBefore) { controller.updateNotifications(before = it) } }
        item { SectionHeader("البيانات") }
        item { SettingItem(Icons.Default.Refresh, "إعادة تحميل ملفات البيانات", "لتطبيق تغييرات JSON الخارجية") { controller.reload() } }
        item { SettingItem(Icons.Default.Restore, "استعادة البيانات الافتراضية", "يستبدل ملفات JSON الحالية") { controller.restore() } }
        item {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("مسار ملفات البيانات", fontWeight = FontWeight.Bold)
                    Text(controller.path(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) = Text(
    title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
)

@Composable
private fun SettingItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.medium) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ToggleItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) = Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) = Card(
    Modifier.fillMaxWidth().clickable(onClick = click), shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
) {
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondary)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun EmptyCard(text: String) = Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleBar(title: String, action: @Composable (() -> Unit)? = null) = CenterAlignedTopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }, actions = { action?.invoke() }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface))

private fun formatDuration(seconds: Long): String { val m = seconds / 60; val s = seconds % 60; return if (m > 0) "$m دقيقة${if (s > 0) " و$s ثانية" else ""}" else "$s ثانية" }
private fun arabicDate(date: LocalDate): String { val months = listOf("", "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"); return "${date.dayOfMonth} ${months[date.monthValue]}" }
