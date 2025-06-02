package com.example.logic

import com.example.models.Category
import com.example.models.Email
import com.example.models.Task
import com.example.models.User

interface IDatabaseService {
    suspend fun getTasks(userId: Int, filter: ((Task) -> Boolean)? = null): List<Task>
    suspend fun getCategories(userId: Int, filter: ((Category) -> Boolean)? = null): List<Category>
    suspend fun saveChangeTask(task: Task): Boolean
    suspend fun createTask(task: Task): Int?
    suspend fun createCategory(category: Category): Int?
    suspend fun createUser(user: User): Int?
    suspend fun getUserByEmail(email: Email): User?
    suspend fun getCategoryById(categoryId: Int): Category?
    suspend fun getUserById(userId: Int): User?
    suspend fun getTaskById(taskId: Int): Task?
}