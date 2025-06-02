package com.example.facade

import java.time.LocalDate
import com.example.logic.*
import com.example.models.*


const val DEF_ID = 0


class Facade(
    private val authorizeUserService: IAuthorizeUserService,
    private val taskService: ITaskService,
    private val statisticService: IStatisticService
) {
    private var idCurUser: Int? = null

    suspend fun createTask(newTask: Task): Int? {
        newTask.userId = idCurUser ?: throw IllegalStateException("Пользователь не авторизован")
        return taskService.createTask(newTask)
    }

    suspend fun moveTask(taskId: Int, newStatus: TaskStatus): Boolean {
        return taskService.moveTask(taskId, newStatus)
    }

    suspend fun moveTaskBySide(taskId: Int, sideInt: Int): Boolean {
        val task = taskService.getTaskById(taskId) ?: return false
        val currentIndex = TaskStatus.entries.indexOf(task.status)
        val newIndex = currentIndex + sideInt

        if (newIndex !in TaskStatus.entries.toTypedArray().indices) return false

        val newStatus = TaskStatus.entries[newIndex]
        return taskService.moveTask(taskId, newStatus)
    }

    suspend fun createCategory(nameStr: String, colorStr: String): Int? {
        val userId = idCurUser ?: throw IllegalStateException("Пользователь не авторизован")
        val newCategory = Category(
            id = DEF_ID,
            userId = userId,
            nameStr = nameStr,
            colorStr = colorStr
        )
        return taskService.createCategory(newCategory)
    }

    suspend fun getTasks(filter: ((Task) -> Boolean)? = null): List<Task> {
        val userId = idCurUser ?: throw IllegalStateException("Пользователь не авторизован")
        return taskService.getTasks(userId, filter)
    }

    suspend fun getCategories(filter: ((Category) -> Boolean)? = null): List<Category> {
        val userId = idCurUser ?: throw IllegalStateException("Пользователь не авторизован")
        return taskService.getCategories(userId, filter)
    }

    suspend fun authorize(emailStr: String, passwordStr: String): Boolean {
        val email = Email(emailStr)
        idCurUser = authorizeUserService.authorize(email, passwordStr)
        return true
    }

    suspend fun register(nameStr: String, emailStr: String, passwordStr: String): Boolean {
         val newUser = User(
             id = DEF_ID,
             nameStr = nameStr,
             emailStr = emailStr,
             passwordStr = passwordStr,
             registrationDate = LocalDate.now()
         )
        idCurUser = authorizeUserService.register(newUser)
        return idCurUser != null
    }

    fun unAuthorize() {
        idCurUser = null
    }

    suspend fun calcStatisticWorkload(): WorkloadStatistic {
        val userId = idCurUser ?: throw IllegalStateException("Пользователь не авторизован")
        return statisticService.calcStatisticWorkload(userId)
    }

    suspend fun calcStatisticCycleTime(categoryId: Int, percentilesLst: List<Double>): CycleTimeStatistic {
        val userId = idCurUser ?: throw IllegalStateException("Пользователь не авторизован")
        val percentiles = percentilesLst.map { Percentile(it) }
        return statisticService.calcStatisticCycleTime(userId, categoryId, percentiles)
    }
}
