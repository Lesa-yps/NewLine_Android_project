package com.example.models

import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDate

data class Task(
    var id: Int,
    var userId: Int,
    val categoryId: Int,
    val name: NonEmptyString,
    val description: String,
    val deadlineDay: String,
    val priority: TaskPriority,
    var status: TaskStatus,
    val creationDay: String,
    var startExecuteTime: String?,
    var finalExecuteTime: String?
) : Serializable {

    // Вычисляемое свойство для получения deadlineDay как LocalDate
    val deadlineDate: LocalDate
        get() = LocalDate.parse(deadlineDay)

    // Вычисляемое свойство для startExecuteTime
    val startDateTime: LocalDateTime?
        get() = startExecuteTime?.let { LocalDateTime.parse(it) }

    // Вычисляемое свойство для finalExecuteTime
    val finalDateTime: LocalDateTime?
        get() = finalExecuteTime?.let { LocalDateTime.parse(it) }

    // Возвращает время, потраченное на выполнение задачи
    fun getWorkTime(): Duration? {
        val start = startDateTime
        val end = finalDateTime
        if (start != null && end != null) {
            return Duration.between(start, end)
        }
        return null
    }

    // Обновление времён
    fun setExecuteTime(newStartExecuteTime: LocalDateTime?, newFinalExecuteTime: LocalDateTime?) {
        startExecuteTime = newStartExecuteTime?.toString()
        finalExecuteTime = newFinalExecuteTime?.toString()
    }

    // Вывод самой важной информации
    fun printShortInfo() {
        println("Задача #$id | Название: $name | Приоритет: ${priority.name} | Статус: ${status.name} | Срок: $deadlineDay")
    }
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class TaskStatus {
    TODO, IN_PROGRESS, DONE
}