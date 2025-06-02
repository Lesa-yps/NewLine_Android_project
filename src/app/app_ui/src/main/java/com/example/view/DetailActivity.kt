package com.example.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.models.Category
import com.example.models.NonEmptyString
import com.example.models.Task
import com.example.models.TaskPriority
import com.example.models.TaskStatus
import com.example.viewmodel.CategoryViewModel
import com.example.viewmodel.State
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.format

class DetailActivity: AppCompatActivity() {

    private val categoryViewModel: CategoryViewModel by KoinJavaComponent.inject(CategoryViewModel::class.java)

    private lateinit var formContainer: LinearLayout
    private var isReadOnly: Boolean = false
    private lateinit var prioritySpinner: Spinner
    private lateinit var statusSpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private var categories: List<Category> = emptyList()
    private var restoredCategoryId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        formContainer = findViewById(R.id.formContainer)
        prioritySpinner = findViewById(R.id.spinnerPriority)
        statusSpinner = findViewById(R.id.spinnerStatus)
        categorySpinner = findViewById(R.id.spinnerCategory)

        // наблюдение за списком категорий из ViewModel
        categoryViewModel.state.observe(this) { state ->
            when (state) {
                is State.Error -> {
                    //println("ERROR")
                    Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_LONG).show()
                    finish()
                }
                is State.Data<*> -> {
                    //println("DATA")
                    categories = state.data as List<Category>
                    setupCategorySpinner()
                    // применение сохранённой категории
                    restoredCategoryId?.let {
                        setSelectedCategory(it)
                        restoredCategoryId = null // больше не нужно
                    }
                }
                is State.Loading<*> -> {}
            }
        }
        // запрос категорий
        categoryViewModel.getCategories()
        isReadOnly = intent.getBooleanExtra(READ_ONLY, false)
        @Suppress("DEPRECATION")
        val task = intent.getSerializableExtra(TASK_OBJ) as? Task

        setupSpinners()
        renderFormForTask(task)

        findViewById<Button>(R.id.buttonSave).apply {
            if (isReadOnly)
                visibility = View.GONE
            else {
                setOnClickListener {
                    saveTask()
                }
            }
        }

        findViewById<Button>(R.id.buttonReturn).apply {
            setOnClickListener {
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val values = formContainer.children.toList()
            .filterIsInstance<TextInputLayout>()
            .mapNotNull { it.editText }

        outState.putString("name", values.find { it.tag == "editTextName" }?.text?.toString())
        outState.putString("description", values.find { it.tag == "editTextDescription" }?.text?.toString())
        outState.putString("deadline", values.find { it.tag == "editTextDeadline" }?.text?.toString())

        outState.putInt("priorityIndex", prioritySpinner.selectedItemPosition)
        outState.putInt("statusIndex", statusSpinner.selectedItemPosition)

        val selectedCategory = categories.getOrNull(categorySpinner.selectedItemPosition)
        selectedCategory?.let {
            outState.putInt("selectedCategoryId", it.id)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val values = formContainer.children.toList()
            .filterIsInstance<TextInputLayout>()
            .mapNotNull { it.editText }

        values.find { it.tag == "editTextName" }?.setText(savedInstanceState.getString("name", ""))
        values.find { it.tag == "editTextDescription" }?.setText(savedInstanceState.getString("description", ""))
        values.find { it.tag == "editTextDeadline" }?.setText(savedInstanceState.getString("deadline", ""))

        prioritySpinner.setSelection(savedInstanceState.getInt("priorityIndex", 0))
        statusSpinner.setSelection(savedInstanceState.getInt("statusIndex", 0))

        // Категория устанавливается только после загрузки списка
        if (savedInstanceState.containsKey("selectedCategoryId")) {
            restoredCategoryId = savedInstanceState.getInt("selectedCategoryId")
        }
    }

    private fun setSelectedCategory(categoryId: Int) {
        val selectedIndex = categories.indexOfFirst { cat -> cat.id == categoryId }
        if (selectedIndex != -1) {
            categorySpinner.setSelection(selectedIndex)
        }
    }

    private fun setupSpinners() {
        // Настройка спиннера приоритета
        ArrayAdapter.createFromResource(
            this,
            R.array.task_priorities,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            prioritySpinner.adapter = adapter
        }

        // Настройка спиннера статуса
        ArrayAdapter.createFromResource(
            this,
            R.array.task_statuses,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            statusSpinner.adapter = adapter
        }
    }

    private fun renderFormForTask(task: Task? = null) {
        formContainer.removeAllViews()

        addEditText("Название", "editTextName", task?.name?.getValue() ?: "")
        addEditText("Описание", "editTextDescription", task?.description ?: "")
        addEditText("Дедлайн", "editTextDeadline",
            task?.deadlineDay?.format(DateTimeFormatter.ISO_DATE) ?: "")

        // Установка выбранных значений в спиннеры
        task?.priority?.let { priority ->
            val priorityPosition = TaskPriority.entries.indexOf(priority)
            prioritySpinner.setSelection(priorityPosition)
        }

        task?.status?.let { status ->
            val statusPosition = TaskStatus.entries.indexOf(status)
            statusSpinner.setSelection(statusPosition)
        }

        if (isReadOnly) {
            prioritySpinner.isEnabled = false
            statusSpinner.isEnabled = false
            categorySpinner.isEnabled = false
        }
    }

    private fun addEditText(label: String, idName: String, text: String, inputTypeValue: Int = InputType.TYPE_CLASS_TEXT) {
        val context = formContainer.context

        val textInputLayout = TextInputLayout(context).apply {
            hint = label
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * context.resources.displayMetrics.density).toInt()
            }
        }

        val editText = TextInputEditText(context).apply {
            id = View.generateViewId()
            setText(text)
            isEnabled = !(isReadOnly || idName == "editTextDeadline") // для даты запрет ручного ввода
            tag = idName
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = inputTypeValue
        }

        if (idName == "editTextDeadline" && !isReadOnly) {
            editText.isEnabled = true
            editText.isFocusable = false
            editText.isClickable = true
            editText.setOnClickListener {
                showDatePickerDialog(editText)
            }
        }

        textInputLayout.addView(editText)
        formContainer.addView(textInputLayout)
    }

    private fun showDatePickerDialog(targetView: TextInputEditText) {
        val currentDate = try {
            LocalDate.parse(targetView.text.toString())
        } catch (_: Exception) {
            LocalDate.now()
        }
        val datePicker = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                targetView.setText(selectedDate.format(DateTimeFormatter.ISO_DATE))
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )

        datePicker.show()
    }

    private fun saveTask() {
        val values = formContainer.children.toList().filterIsInstance<TextInputLayout>().mapNotNull { it.editText }

        val name = values.find { it.tag == "editTextName" } ?.text .toString()
        val description = values.find { it.tag == "editTextDescription" } ?.text .toString()
        val deadlineStr = values.find { it.tag == "editTextDeadline" } ?.text .toString()

        val priority = TaskPriority.entries[prioritySpinner.selectedItemPosition]
        val status = TaskStatus.entries[statusSpinner.selectedItemPosition]

        try {
            val deadline = LocalDate.parse(deadlineStr)
            val selectedCategoryIndex = categorySpinner.selectedItemPosition
            val task = Task(
                id = 0,
                userId = 0,
                categoryId = categories[selectedCategoryIndex].id,
                name = NonEmptyString(name),
                description = description,
                deadlineDay = deadline.toString(),
                priority = priority,
                status = status,
                creationDay = LocalDate.now().toString(),
                startExecuteTime = null,
                finalExecuteTime = null
            )

            val resultIntent = Intent().putExtra(TASK_OBJ, task)
            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (_: Exception) {
            Toast.makeText(this, "Ошибка формата даты: используйте гггг-мм-дд", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAddCategoryDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val input = EditText(this).apply {
            hint = "Введите название категории"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        layout.addView(input)

        // Доступные цвета
        val colors = listOf(
            "#FF0000", // красный
            "#00FF00", // зеленый
            "#0000FF", // синий
            "#FFA500", // оранжевый
            "#800080", // фиолетовый
            "#008080", // бирюзовый
        )

        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }

        colors.forEachIndexed { index, colorHex ->
            val radioButton = RadioButton(this).apply {
                id = View.generateViewId()
                buttonDrawable = null

                // создание StateListDrawable для разных состояний
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked), // выбранное состояние
                    intArrayOf(-android.R.attr.state_checked) // обычное состояние
                )

                val stateListDrawable = StateListDrawable().apply {
                    // Выбранное состояние - толстая черная обводка (6dp)
                    addState(states[0], GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorHex.toColorInt())
                        setStroke(6, Color.BLACK)
                    })
                    // Обычное состояние - тонкая серая обводка (2dp)
                    addState(states[1], GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorHex.toColorInt())
                        setStroke(2, Color.LTGRAY)
                    })
                }

                background = stateListDrawable
                setPadding(20, 20, 20, 20)
                width = 100
                height = 100

                // Обновление вида при изменении состояния
                setOnCheckedChangeListener { buttonView, isChecked ->
                    buttonView.background.state = if (isChecked) states[0] else states[1]
                }
            }

            radioGroup.addView(radioButton)

            // Первый выбран по умолчанию
            if (index == 0) {
                radioButton.isChecked = true
            }
        }

        layout.addView(TextView(this).apply {
            text = "Выберите цвет категории:"
            setPadding(0, 30, 0, 10)
        })

        layout.addView(radioGroup)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Новая категория")
            .setView(layout)
            .setPositiveButton("Добавить") { dialogInterface, _ ->
                val newName = input.text.toString().trim()
                val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                val selectedIndex = radioGroup.indexOfChild(radioGroup.findViewById(selectedRadioButtonId))
                val selectedColor = colors.getOrNull(selectedIndex) ?: "#FF0000"

                if (newName.isNotEmpty()) {
                    addCategory(newName, selectedColor)
                } else {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }

                dialogInterface.dismiss()
            }
            .setNegativeButton("Отмена") { dialogInterface, _ ->
                dialogInterface.dismiss()
                if (categories.isNotEmpty()) {
                    categorySpinner.setSelection(0)
                }
            }
            .create()

        dialog.show()
    }

    private fun setupCategorySpinner() {
        val items = categories.map { "${it.name}" }.toMutableList()
        items.add("Добавить категорию...")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Устанавливается выбранная категория из задачи, если есть
        @Suppress("DEPRECATION") val task = intent.getSerializableExtra(TASK_OBJ) as? Task
        task?.let { setSelectedCategory(it.categoryId) }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == items.size - 1) {
                    showAddCategoryDialog()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun addCategory(name: String, color: String) {
        lifecycleScope.launch {
            val newCategoryId = categoryViewModel.addItem(name, color)
            if (newCategoryId != null) {
                categoryViewModel.getCategories()
                setSelectedCategory(newCategoryId)
            } else {
                Toast.makeText(this@DetailActivity, "Не удалось добавить категорию", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val TASK_OBJ = "taskObj"
        const val READ_ONLY = "readOnly"

        fun createIntent(context: Context, task: Task?, isReadOnly: Boolean = false): Intent {
            return Intent(context, DetailActivity::class.java).apply {
                putExtra(TASK_OBJ, task)
                putExtra(READ_ONLY, isReadOnly)
            }
        }
    }
}