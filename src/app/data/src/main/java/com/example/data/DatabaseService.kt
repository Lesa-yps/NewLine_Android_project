package com.example.data

import com.example.logic.IDatabaseService
import com.example.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


class DatabaseService : IDatabaseService {
    private val TAG = "DatabaseService"
    private val db: FirebaseFirestore = Firebase.firestore

    override suspend fun getTasks(userId: Int, filter: ((Task) -> Boolean)?): List<Task> = withContext(Dispatchers.IO) {
        getCollection("tasks", "userId", userId) { transformTaskFromDbFormat(it) }
            .let { filter?.let(it::filter) ?: it }
            .also { Log.i(TAG, "Найдено задач: ${it.size}") }
    }

    override suspend fun getCategories(userId: Int, filter: ((Category) -> Boolean)?): List<Category> = withContext(Dispatchers.IO) {
        getCollection("categories", "userId", userId) { transformCategoryFromDbFormat(it) }
            .let { filter?.let(it::filter) ?: it }
            .also { Log.i(TAG, "Найдено категорий: ${it.size}") }
    }

    override suspend fun createTask(task: Task): Int? = withContext(Dispatchers.IO) {
        createWithId("tasks", task) { transformTaskToDbFormat(it) }
            .also { if (it != null) Log.i(TAG, "Задача создана с id = $it") }
    }

    override suspend fun createCategory(category: Category): Int? = withContext(Dispatchers.IO) {
        createWithId("categories", category) { transformCategoryToDbFormat(it) }
            .also { if (it != null) Log.i(TAG, "Категория создана с id = $it") }
    }

    override suspend fun createUser(user: User): Int? = withContext(Dispatchers.IO) {
        createWithId("users", user) { transformUserToDbFormat(it) }
            .also { if (it != null) Log.i(TAG, "Пользователь создан с id = $it") }
    }

    override suspend fun saveChangeTask(task: Task): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val doc = db.collection("tasks")
                .whereEqualTo("id", task.id).limit(1)
                .get().await().documents.firstOrNull()

            doc?.reference?.set(transformTaskToDbFormat(task))?.await()
            Log.i(TAG, "Задача изменена: id = ${task.id}")
            true
        }.onFailure {
            Log.e(TAG, "Ошибка обновления задачи ${task.id}: ${it.message}")
        }.getOrDefault(false)
    }

    override suspend fun getUserByEmail(email: Email): User? = withContext(Dispatchers.IO) {
        getSingleDocument("users", "email", email.toString(), ::transformUserFromDbFormat)
    }

    override suspend fun getUserById(userId: Int): User? = withContext(Dispatchers.IO) {
        getSingleDocument("users", "id", userId, ::transformUserFromDbFormat)
    }

    override suspend fun getCategoryById(categoryId: Int): Category? = withContext(Dispatchers.IO) {
        getSingleDocument("categories", "id", categoryId, ::transformCategoryFromDbFormat)
    }

    override suspend fun getTaskById(taskId: Int): Task? = withContext(Dispatchers.IO) {
        getSingleDocument("tasks", "id", taskId, ::transformTaskFromDbFormat)
    }

    // Функции, сокращающие код выше

    private suspend fun <T> getCollection(
        collection: String,
        field: String,
        value: Any,
        transform: (Map<String, Any>) -> T?
    ): List<T> = runCatching {
        db.collection(collection).whereEqualTo(field, value).get().await()
            .documents.mapNotNull { doc -> doc.data?.let(transform) }
    }.onFailure {
        Log.e(TAG, "Ошибка получения коллекции $collection: ${it.message}")
    }.getOrDefault(emptyList())

    private suspend fun <T> getSingleDocument(
        collection: String,
        field: String,
        value: Any,
        transform: (Map<String, Any>) -> T?
    ): T? = runCatching {
        db.collection(collection).whereEqualTo(field, value).limit(1).get().await()
            .documents.firstOrNull()?.data?.let(transform)
    }.onFailure {
        Log.e(TAG, "Ошибка получения объекта из $collection по $field=$value: ${it.message}")
    }.getOrNull()

    private suspend fun <T> createWithId(
        collection: String,
        obj: T,
        toMap: (T) -> Map<String, Any?>
    ): Int? = runCatching {
        val docRef = db.collection(collection).document()
        val id = docRef.id.hashCode()
        when (obj) {
            is Task -> obj.id = id
            is Category -> obj.id = id
            is User -> obj.id = id
        }
        docRef.set(toMap(obj)).await()
        id
    }.onFailure {
        Log.e(TAG, "Ошибка при создании объекта в $collection: ${it.message}")
    }.getOrNull()


    // Преобразование Task из формата базы данных
    private fun transformTaskFromDbFormat(doc: Map<String, Any>): Task {
        return Task(
            doc["id"].toString().toInt(),
            doc["userId"].toString().toInt(),
            doc["categoryId"].toString().toInt(),
            NonEmptyString(doc["name"].toString()),
            doc["description"].toString(),
            doc["deadlineDay"].toString(),
            TaskPriority.valueOf(doc["priority"].toString()),
            TaskStatus.valueOf(doc["status"].toString()),
            doc["creationDay"].toString(),
            doc["startExecuteTime"]?.toString(),
            doc["finalExecuteTime"]?.toString()
        )
    }
    // Преобразование Task в формат базы данных
    private fun transformTaskToDbFormat(task: Task): Map<String, Any> = buildMap {
        put("id", task.id)
        put("userId", task.userId)
        put("categoryId", task.categoryId)
        put("name", task.name.toString())
        put("description", task.description)
        put("deadlineDay", task.deadlineDay.toString())
        put("priority", task.priority.name)
        put("status", task.status.name)
        put("creationDay", task.creationDay.toString())
        // Добавление startExecuteTime и finalExecuteTime, только если они не null
        task.startExecuteTime?.let { put("startExecuteTime", it.toString()) }
        task.finalExecuteTime?.let { put("finalExecuteTime", it.toString()) }
    }

    // Преобразование Category из формата базы данных
    private fun transformCategoryFromDbFormat(doc: Map<String, Any>): Category {
        return Category(
            doc["id"].toString().toInt(),
            doc["userId"].toString().toInt(),
            doc["name"].toString(),
            doc["color"].toString()
        )
    }
    // Преобразование Category в формат базы данных
    private fun transformCategoryToDbFormat(category: Category): Map<String, Any> {
        return mapOf(
            "id" to category.id,
            "userId" to category.userId,
            "name" to category.name.toString(),
            "color" to category.color.toString()
        )
    }

    // Преобразование User из формата базы данных
    private fun transformUserFromDbFormat(doc: Map<String, Any>): User {
        return User(
            doc["id"].toString().toInt(),
            doc["name"].toString(),
            doc["email"].toString(),
            doc["password"].toString(),
            LocalDate.parse(doc["registrationDate"].toString())
        )
    }
    // Преобразование User в формат базы данных
    private fun transformUserToDbFormat(user: User): Map<String, Any> {
        return mapOf(
            "id" to user.id,
            "name" to user.name.toString(),
            "email" to user.email.toString(),
            "password" to user.password.toString(),
            "registrationDate" to user.registrationDate.toString()
        )
    }
}
