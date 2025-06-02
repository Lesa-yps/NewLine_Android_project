package com.example.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import com.example.models.TaskPriority.*
import com.example.models.Task
import com.example.models.TaskStatus
import java.time.DayOfWeek
import java.time.Duration
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.TextStyle
import java.util.Locale
import java.util.logging.Logger


interface IStatisticService {
    // Расчёт нагрузки по дням следующей недели для заданного пользователя
    suspend fun calcStatisticWorkload(userId: Int): WorkloadStatistic
    // Расчёт времён выполнения задач (cycle time) и вычисление переданных процентильных значений
    suspend fun calcStatisticCycleTime(userId: Int, categoryId: Int, percentiles: List<Percentile>): CycleTimeStatistic
}


class StatisticService(private val databaseService: IDatabaseService) : IStatisticService {

    private val logger = Logger.getLogger(StatisticService::class.java.name)

    // Расчёт нагрузки по дням следующей недели для заданного пользователя
    override suspend fun calcStatisticWorkload(userId: Int): WorkloadStatistic {
        logger.info("Начало расчёта нагрузки для пользователя с id: $userId")

        val now = LocalDate.now()
        val nextWeekStart = now.plusDays(1) // Завтра
        val nextWeekEnd = now.plusDays(7)

        // Получение задач, которые имеют дедлайн в пределах следующей недели и не выполнены
        val tasks: List<Task> = databaseService.getTasks(userId) { task ->
            task.deadlineDate in nextWeekStart..nextWeekEnd && task.status != TaskStatus.DONE
        }

        logger.info("Найдено ${tasks.size} задач для пользователя $userId с дедлайном на следующую неделю")

        val workloadByDay = mutableMapOf<DayOfWeek, Double>()
        DayOfWeek.entries.forEach { day -> workloadByDay[day] = 0.0 }

        // Заполнение массива нагрузок
        tasks.forEach { task ->
            val daysUntilDeadline = ChronoUnit.DAYS.between(nextWeekStart, task.deadlineDate).toInt()
            val dayOfWeek = nextWeekStart.plusDays(daysUntilDeadline.toLong()).dayOfWeek
            workloadByDay[dayOfWeek] = workloadByDay.getOrDefault(dayOfWeek, 0.0) + getWeight(task)
        }

        logger.info("Завершён расчёт нагрузки, возврат результата")

        return WorkloadStatistic(workloadByDay)
    }

    // Расчёт времён выполнения задач (cycle time) и вычисление переданных процентильных значений
    override suspend fun calcStatisticCycleTime(userId: Int, categoryId: Int, percentiles: List<Percentile>): CycleTimeStatistic {
        logger.info("Начало расчёта времён выполнения задач для пользователя $userId и категории $categoryId")

        val lastMonthStart = LocalDate.now().minusMonths(1)

        // Получение задач, которые были завершены за последний месяц и принадлежат заданной категории
        val tasks: List<Task> = databaseService.getTasks(userId).filter { task ->
            task.status == TaskStatus.DONE && task.categoryId == categoryId && task.deadlineDate.isAfter(lastMonthStart)
        }

        logger.info("Найдено ${tasks.size} завершённых задач за последний месяц в категории $categoryId")

        // Для каждой задачи расчёт времени выполнения и сортировка по увеличению
        val cycleTimes: List<Duration> = tasks.mapNotNull { it.getWorkTime() }.sorted()

        logger.info("Вычислены времена выполнения задач, начало расчёта процентилей")

        // Вычисление значения перцентилей (берётся последний элемент, если индекс больше размера массива, или 0, если индекс < 0)
        val percentileValues: Map<Percentile, Duration> = percentiles.associateWith { percentile ->
            val index = ceil(percentile.getValue() * cycleTimes.size).toInt() - 1
            if (index < 0) Duration.ZERO
            else cycleTimes.getOrElse(index) { cycleTimes.last() }
        }

        logger.info("Завершён расчёт времён выполнения задач и процентилей, возврат результата")

        return CycleTimeStatistic(cycleTimes, percentileValues)
    }

    // Расчёт веса задачи в зависимости от её приоритета и количества дней до дедлайна
    private fun getWeight(task: Task): Double {
        val weightPriority = when (task.priority) {
            LOW -> 1.0
            MEDIUM -> 1.5
            HIGH -> 2.0
        }
        val daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), task.deadlineDate).toDouble()
        val weight = BigDecimal(weightPriority / daysUntilDeadline).setScale(2, RoundingMode.HALF_UP).toDouble()

        logger.fine("Для задачи с id ${task.id} рассчитан вес: $weight")

        return weight
    }
}


// Класс-перцентиль
data class Percentile(private val value: Double) {
    init {
        require(value in 0.0..1.0) { "Значение перцентиля должно быть в диапазоне от 0 до 1." }
    }

    fun getValue(): Double = value
}

// Класс, хранящий словарь из семи дней недели и весов нагрузок по дням и возвращающий их в отсортированном виде (в зависимости от текущего дня недели)
data class WorkloadStatistic(
    val workloadByDay: Map<DayOfWeek, Double>
) {
    fun getSortedWeekDays(): Map<DayOfWeek, Double> {
        val tomorrow = LocalDate.now().plusDays(1).dayOfWeek
        return workloadByDay.toSortedMap(compareBy { (it.ordinal - tomorrow.ordinal + 7) % 7 })
    }

    fun getWorkloadReport(): String {
        val sb = StringBuilder()
        sb.appendLine("📊 Нагрузка на следующую неделю:")

        getSortedWeekDays().forEach { (day, load) ->
            val loadBar = "█".repeat((load * 2 * 5).toInt().coerceAtMost(20)) // максимум 20 символов
            val formattedLoad = String.format(Locale.US, "%.2f", load)
            sb.appendLine("${day.name.padEnd(10)} | $loadBar ($formattedLoad)")
        }

        return sb.toString()
    }

    fun getRecommendations(): String {
        val overloadedDays = workloadByDay.filterValues { it > 1.0 }

        val recommendation = StringBuilder("📌 Рекомендации:\n")

        if (overloadedDays.isNotEmpty()) {
            val minLoadDays = workloadByDay
                .filterValues { it <= 1.0 }
                .toList()
                .sortedBy { it.second }
                .joinToString(", ") { it.first.toRussianShort() }

            overloadedDays.forEach { (day, load) ->
                recommendation.appendLine("⚠️ ${day.toRussianFull()}: нагрузка $load — слишком высокая.")
                recommendation.appendLine("   ➤ Рекомендуется перераспределить задачи на дни с меньшей нагрузкой (${minLoadDays.ifEmpty { "нет подходящих дней" }}).")
            }
        }

        if (overloadedDays.size >= 4) {
            recommendation.appendLine("\n🔥 Внимание: нагрузка слишком велика в течение недели.")
            recommendation.appendLine("   ➤ Рекомендуется пересмотреть планирование.")
        } else if (overloadedDays.isEmpty()) {
            recommendation.appendLine("✅ Всё в порядке: ни один день не перегружен.")
        } else {
            recommendation.appendLine("🟡 В целом всё хорошо, но есть перегруженные дни.")
        }

        return recommendation.toString()
    }
}

// Класс, хранящий список промежутков времён выполнений всех задач и словарь перцентиль - промежуток времени
data class CycleTimeStatistic(
    val cycleTimes: List<Duration>,
    val percentileValues: Map<Percentile, Duration>
) {
    override fun toString(): String {
        val formattedCycleTimes = cycleTimes.joinToString(", ") { it.toHumanReadable() }

        val formattedPercentiles = percentileValues.entries.joinToString("\n") { (percentile, duration) ->
            "${(percentile.getValue() * 100).toInt()}% задач выполнено за ${duration.toHumanReadable()} или меньше"
        }

        return "Времена выполнения задач: [$formattedCycleTimes]\nПерцентили:\n$formattedPercentiles"
    }

    fun getCycleTimeReport(): String {
        val sb = StringBuilder()
        sb.appendLine("⏱️ Времена выполнения задач:")

        if (cycleTimes.isEmpty()) {
            sb.appendLine("Нет завершённых задач за последний месяц.")
        } else {
            sb.appendLine(cycleTimes.joinToString(", ") { it.toHumanReadable() })

            sb.appendLine("📌 Перцентили:")
            percentileValues.entries
                .sortedBy { it.key.getValue() }
                .forEach { (percentile, duration) ->
                    sb.appendLine("• ${"%.0f".format(percentile.getValue() * 100)}% задач — ${duration.toHumanReadable()} или меньше")
                }
        }

        return sb.toString()
    }

    fun getRecommendations(): String {
        val sb = StringBuilder("📌 Рекомендации:\n")

        if (cycleTimes.isEmpty()) {
            sb.appendLine("Нет данных для анализа. Нужно больше завершённых задач.")
            return sb.toString()
        }

        val durationsMillis = cycleTimes.map { it.toMillis() }
        val average = durationsMillis.average()
        val stdDev = kotlin.math.sqrt(durationsMillis.map { (it - average).let { d -> d * d } }.average())

        val highThresholdMillis = Duration.ofDays(2).toMillis()
        if (average > highThresholdMillis) {
            sb.appendLine("⏳ Среднее время выполнения задач высокое (${Duration.ofMillis(average.toLong()).toHumanReadable()}).")
            sb.appendLine("   ➤ Рекомендуется пересмотреть объём задач или улучшить планирование.")
        }

        if (stdDev > average / 2) {
            sb.appendLine("📉 Большой разброс времени выполнения (σ = ${Duration.ofMillis(stdDev.toLong()).toHumanReadable()}).")
            sb.appendLine("   ➤ Рекомендуется улучшить процесс: дробить крупные задачи, стандартизировать подходы.")
        }

        return sb.toString()
    }
}

// Функция для форматирования Duration в человекочитаемый вид
fun Duration.toHumanReadable(): String {
    val hours = this.toHours()
    val minutes = this.minusHours(hours).toMinutes()

    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

// Перевод дней недели на русский
// Полное название (понедельник, вторник...)
fun DayOfWeek.toRussianFull(): String =
    getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"))

// Короткое название (пн, вт...)
fun DayOfWeek.toRussianShort(): String =
    getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru"))