package com.example.app_ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.models.Category
import com.example.data.DatabaseService
import com.example.models.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID
import org.junit.Before
import org.junit.Test


@RunWith(AndroidJUnit4::class)
class DatabaseServiceTest {

 private lateinit var databaseService: DatabaseService
 private lateinit var testUser: User
 private lateinit var testCategory: Category
 private lateinit var testTask: Task

 @Before
 fun setUp() {
  databaseService = DatabaseService()

  // создание тестового пользователя в БД
  val randomEmail = "test_${UUID.randomUUID()}@example.com"
  testUser = User(0, "Test_user", randomEmail, "TestPassword1!", LocalDate.now())
  runBlocking { testUser.id = databaseService.createUser(testUser)!! }

  // создание тестовой категории
  testCategory = Category(0, testUser.id, "Test Category", "#0000FF")
  runBlocking { testCategory.id = databaseService.createCategory(testCategory)!! }

  // создание тестовой задачи
  testTask = Task(0, testUser.id, testCategory.id, NonEmptyString("Test Task"), "This is a test task",
      LocalDate.now().plusDays(1).toString(), TaskPriority.MEDIUM, TaskStatus.TODO,
      LocalDate.now().toString(), null, null)
  runBlocking { testTask.id = databaseService.createTask(testTask)!! }
 }

 @Test
 fun find_user_by_ID() = runBlocking {
  // поиск пользователя по ID
  val foundUser = databaseService.getUserById(testUser.id)
  // проверки, что пользователь найден и данные совпадают
  assertNotNull(foundUser)
  assertEquals(testUser.id, foundUser?.id)
  assertEquals(testUser.name, foundUser?.name)
  assertEquals(testUser.email.toString(), foundUser?.email.toString())
 }

 @Test
 fun find_user_by_email() = runBlocking {
  // поиск пользователя по email
  val foundUser = databaseService.getUserByEmail(testUser.email)
  // проверки, что пользователь найден и данные совпадают
  assertNotNull(foundUser)
  assertEquals(testUser.id, foundUser?.id)
  assertEquals(testUser.name, foundUser?.name)
  assertEquals(testUser.email.toString(), foundUser?.email.toString())
 }

 @Test
 fun find_category_by_ID() = runBlocking {
  // поиск категории по ID
  val foundCategory = databaseService.getCategoryById(testCategory.id)
  // проверки, что категория найдена и данные совпадают
  assertNotNull(foundCategory)
  assertEquals(testCategory.id, foundCategory?.id)
  assertEquals(testCategory.name, foundCategory?.name)
  assertEquals(testCategory.color.toString(), foundCategory?.color.toString())
 }

 @Test
 fun find_task_by_ID() = runBlocking {
  // поиск задачи по ID
  val foundTask = databaseService.getTaskById(testTask.id)
  // проверки, что задача найдена и данные совпадают
  assertNotNull(foundTask)
  assertEquals(testTask.id, foundTask?.id)
  assertEquals(testTask.name, foundTask?.name)
  assertEquals(testTask.description, foundTask?.description)
  assertEquals(testTask.deadlineDay, foundTask?.deadlineDay)
  assertEquals(testTask.priority, foundTask?.priority)
  assertEquals(testTask.status, foundTask?.status)
 }

 @Test
 fun getTasks_return_one_task() = runBlocking {
  val tasks = databaseService.getTasks(testUser.id)
  assertTrue(tasks.any { it.id == testTask.id })
 }

 @Test
 fun getTasks_filter() = runBlocking {
  val filtered = databaseService.getTasks(testUser.id) { it.priority == TaskPriority.MEDIUM }
  assertTrue(filtered.all { it.priority == TaskPriority.MEDIUM })
 }

 @Test
 fun getCategories_return_one_category() = runBlocking {
  val categories = databaseService.getCategories(testUser.id)
  assertTrue(categories.any { it.id == testCategory.id })
 }

 @Test
 fun getCategories_filter() = runBlocking {
  val filtered = databaseService.getCategories(testUser.id) { it.name.toString().contains("Test") }
  assertTrue(filtered.all { it.name.toString().contains("Test") })
 }

 @Test
 fun saveChangeTask_should_update_task_status_to_IN_PROGRESS() = runBlocking {
  // изменение статуса задачи
  testTask.status = TaskStatus.IN_PROGRESS
  // сохранение изменений
  val success = databaseService.saveChangeTask(testTask)
  assertTrue(success) // проверка успешности
  // поиск задачи по id в базе и проверка изменения статуса
  val foundTask = databaseService.getTaskById(testTask.id)
  assertEquals(TaskStatus.IN_PROGRESS, foundTask?.status)
 }
}
