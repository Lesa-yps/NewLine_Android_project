package com.example.view

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.adapter.TaskAdapter
import com.example.models.Task
import com.example.viewmodel.State
import com.example.viewmodel.TaskViewModel
import com.facebook.shimmer.ShimmerFrameLayout

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var errorTextView: TextView
    private lateinit var buttonAddUpdate: Button
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showAuth()

        recyclerView = findViewById(R.id.recyclerView)
        shimmerLayout = findViewById(R.id.shimmerLayout)
        errorTextView = findViewById(R.id.errorTextView)

        buttonAddUpdate = findViewById<Button>(R.id.buttonAddUpdate)

        adapter = TaskAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.state.observe(this) { state ->
            when (state) {
                is State.Loading -> {
                    //println("LOADING")
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                    errorTextView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    buttonAddUpdate.isEnabled = false
                    buttonAddUpdate.text = "Загрузка..."
                }

                is State.Error -> {
                    //println("ERROR")
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = state.message
                    recyclerView.visibility = View.GONE
                    buttonAddUpdate.isEnabled = true
                    buttonAddUpdate.text = "Обновить"
                }

                is State.Data<*> -> {
                    //println("DATA")
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.setItems(state.data as List<Task>)
                    buttonAddUpdate.isEnabled = true
                    buttonAddUpdate.text = "Добавить"
                }
            }

            val buttonTasks = findViewById<View>(R.id.buttonTasks)
            val buttonStats = findViewById<View>(R.id.buttonStats)
            val buttonLogout = findViewById<View>(R.id.buttonLogout)

            buttonTasks.setOnClickListener {
                viewModel.loadItems()
            }

            buttonStats.setOnClickListener {
                if (adapter.getItemCount() == 0)
                    Toast.makeText(this, "Просмотр статистики закрыт, пока список задач пуст", Toast.LENGTH_LONG).show()
                else {
                    // запускается активность статистики
                    val intent = StatisticActivity.Companion.createIntent(this)
                    startActivity(intent)
                }
            }

            buttonLogout.setOnClickListener {
                showAuth()
            }
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val task = adapter.getTaskAt(position)
                val side = when (direction) {
                    ItemTouchHelper.RIGHT -> 1
                    ItemTouchHelper.LEFT -> -1
                    else -> 0
                }

                viewModel.moveItem(
                    task = task,
                    side = side,
                    onError = { adapter.notifyItemChanged(position) },
                    onNotMove = { cantMove() }
                )
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        buttonAddUpdate.setOnClickListener {
            when (viewModel.state.value) {
                is State.Error -> {
                    viewModel.loadItems()
                }
                else -> {
                    val intent = DetailActivity.createIntent(this, null, isReadOnly = false)
                    addItemLauncher.launch(intent)
                }
            }
        }
    }

    private val addItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val newObj: Task? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra(DetailActivity.TASK_OBJ, Task::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(DetailActivity.TASK_OBJ) as? Task
            }
            newObj?.let { obj -> viewModel.addItem(obj) }
        }
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadItems()
        } else {
            finish() // закрытие, если авторизация не пройдена
        }
    }

    private fun showAuth() {
        val intent = AuthActivity.createIntent(this)
        authLauncher.launch(intent)
    }

    private fun cantMove() {
        Toast.makeText(this, "Нельзя изменить статус", Toast.LENGTH_LONG).show()
    }
}