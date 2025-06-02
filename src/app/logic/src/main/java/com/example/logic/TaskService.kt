package com.example.logic

import com.example.models.Category
import com.example.models.Task
import com.example.models.TaskStatus
import com.example.models.User
import java.time.Duration
import java.time.LocalDateTime
import java.util.logging.Logger


interface ITaskService {
    suspend fun createTask(newTask: Task): Int?
    suspend fun moveTask(taskId: Int, newStatus: TaskStatus): Boolean
    suspend fun createCategory(newCategory: Category): Int?
    suspend fun getTasks(userId: Int, filter: ((Task) -> Boolean)? = null): List<Task>
    suspend fun getCategories(userId: Int, filter: ((Category) -> Boolean)? = null): List<Category>

    suspend fun getTaskById(taskId: Int): Task?
}


class TaskService(private val databaseService: IDatabaseService) : ITaskService {

    private val logger = Logger.getLogger(TaskService::class.java.name)

    override suspend fun createTask(newTask: Task): Int? {
        logger.info("Попытка создать задачу для пользователя с id: ${newTask.userId}")

        // пользователь существует?
        val user = getUserById(newTask.userId)
        if (user == null) {
            logger.warning("Ошибка: Пользователь с id ${newTask.userId} не существует в базе.")
            throw IllegalArgumentException("Пользователь с id ${newTask.userId} не существует в базе.")
        }

        // категория существует?
        val category = getCategoryById(newTask.categoryId)
        if (category == null) {
            logger.warning("Ошибка: Категория с id ${newTask.categoryId} не существует в базе.")
            throw IllegalArgumentException("Категория с id ${newTask.categoryId} не существует в базе.")
        }

        logger.info("Задача успешно создана для пользователя с id: ${newTask.userId} и категории с id: ${newTask.categoryId}")

        return databaseService.createTask(newTask)
    }

    override suspend fun moveTask(taskId: Int, newStatus: TaskStatus): Boolean {
        logger.info("Попытка изменить статус задачи с id: $taskId на $newStatus")

        // задача существует?
        val task = getTaskById(taskId) ?: run {
            logger.warning("Ошибка: Задача с id $taskId не существует в базе.")
            throw IllegalArgumentException("Перемещаемой задачи с id $taskId не существует в базе.")
        }

        // проверки возможности изменения статуса
        if (!isPossibleMoveTask(task.status, newStatus)) {
            logger.warning("Ошибка: Невозможно переместить задачу из статуса ${task.status} в $newStatus.")
            throw IllegalArgumentException("Невозможно переместить задачу из статуса ${task.status} в $newStatus.")
        }

        // обновление времён
        setTimesMoveTask(task, newStatus)

        // обновление статуса
        task.status = newStatus
        logger.info("Статус задачи с id: $taskId успешно изменён на $newStatus")

        return databaseService.saveChangeTask(task)
    }

    override suspend fun createCategory(newCategory: Category): Int? {
        logger.info("Попытка создать категорию с названием: ${newCategory.name}")
        return databaseService.createCategory(newCategory)
    }

    override suspend fun getTasks(userId: Int, filter: ((Task) -> Boolean)?): List<Task> {
        logger.info("Получение задач для пользователя с id: $userId")
        return databaseService.getTasks(userId, filter)
    }

    override suspend fun getCategories(userId: Int, filter: ((Category) -> Boolean)?): List<Category> {
        logger.info("Получение категорий для пользователя с id: $userId")
        return databaseService.getCategories(userId, filter)
    }

    private suspend fun getUserById(userId: Int): User? {
        logger.info("Попытка найти в БД пользователя с id = $userId")
        return databaseService.getUserById(userId)
    }

    private suspend fun getCategoryById(categoryId: Int): Category? {
        logger.info("Попытка найти в БД категорию с id = $categoryId")
        return databaseService.getCategoryById(categoryId)
    }

    override suspend fun getTaskById(taskId: Int): Task? {
        logger.info("Попытка найти в БД задачу с id = $taskId")
        return databaseService.getTaskById(taskId)
    }

    // проверки возможности изменения статуса
    private fun isPossibleMoveTask(curStatus: TaskStatus, newStatus: TaskStatus): Boolean {
        val possible = !((curStatus == TaskStatus.TODO && newStatus == TaskStatus.DONE) ||
                (curStatus == TaskStatus.DONE && newStatus == TaskStatus.TODO))
        logger.fine("Проверка возможности перемещения задачи из $curStatus в $newStatus: $possible")
        return possible
    }

    // обновление времён
    private fun setTimesMoveTask(task: Task, newStatus: TaskStatus) {
        val now = LocalDateTime.now()
        val curStatus = task.status

        val newStart = when {
            curStatus == TaskStatus.TODO && newStatus == TaskStatus.IN_PROGRESS -> now
            curStatus == TaskStatus.DONE && newStatus == TaskStatus.IN_PROGRESS ->
                task.startDateTime?.plus(Duration.between(task.finalDateTime, now))
            curStatus == TaskStatus.IN_PROGRESS && newStatus == TaskStatus.TODO -> null
            else -> task.startDateTime
        }

        val newFinal = when {
            curStatus == TaskStatus.IN_PROGRESS && newStatus == TaskStatus.DONE -> now
            curStatus == TaskStatus.DONE && newStatus == TaskStatus.IN_PROGRESS -> null
            else -> task.finalDateTime
        }

        logger.fine("Изменение времён задачи с id: ${task.id} с (${task.startDateTime}, ${task.finalExecuteTime}) на ($newStart, $newFinal)")
        task.setExecuteTime(newStart, newFinal)
    }
}