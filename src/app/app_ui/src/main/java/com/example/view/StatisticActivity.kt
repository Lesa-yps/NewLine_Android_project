package com.example.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.view.graph.CycleTimeGraphView
import com.example.R
import com.example.view.graph.WorkloadGraphView
import com.example.logic.CycleTimeStatistic
import com.example.logic.WorkloadStatistic
import com.example.models.Category
import com.example.viewmodel.CategoryViewModel
import com.example.viewmodel.State
import com.example.viewmodel.StatisticViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import org.koin.java.KoinJavaComponent
import java.time.Duration
import java.time.format.TextStyle
import java.util.Locale

class StatisticActivity : AppCompatActivity() {
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var errorTextView: TextView
    private lateinit var buttonReturn: Button

    private lateinit var buttonWorkload: Button
    private lateinit var buttonCycleTime: Button

    private lateinit var categorySpinner: Spinner
    private lateinit var inputNumbers: TextView
    private lateinit var buttonCalculateCycle: Button
    private lateinit var cycleTimeInputContainer: View

    private lateinit var resTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var workloadGraphView: WorkloadGraphView
    private lateinit var cycleTimeGraphView: CycleTimeGraphView

    private val categoryViewModel: CategoryViewModel by KoinJavaComponent.inject(CategoryViewModel::class.java)
    private var categories: List<Category> = listOf()
    private var restoredCategoryId: Int? = null

    private enum class GraphType { WORKLOAD, CYCLE_TIME }
    private var currentGraph: GraphType = GraphType.WORKLOAD

    private var lastCategoryIdGraphRun: Int? = null
    private var lastPercentilesGraphRun: List<Double> = listOf(0.1, 0.5, 0.9)

    private val viewModel: StatisticViewModel by viewModels()

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistic)

        buttonReturn = findViewById(R.id.buttonReturn)
        shimmerLayout = findViewById(R.id.shimmerLayout)
        errorTextView = findViewById(R.id.errorTextView)

        buttonWorkload = findViewById(R.id.buttonWorkloadRun)
        buttonCycleTime = findViewById(R.id.buttonCycleTimeRun)

        cycleTimeInputContainer = findViewById(R.id.cycleTimeInputContainer)
        categorySpinner = findViewById(R.id.categorySpinner)
        inputNumbers = findViewById(R.id.inputNumbers)
        buttonCalculateCycle = findViewById(R.id.buttonCalculateCycle)

        resTextView = findViewById(R.id.resTextView)
        titleTextView = findViewById(R.id.titleTextView)
        workloadGraphView = findViewById(R.id.workloadGraphView)
        cycleTimeGraphView = findViewById(R.id.cycleTimeGraphView)

        viewModel.workloadStatistic.observe(this) { state ->
            when (state) {
                is State.Loading -> {
                    //println("LOADING")
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                    errorTextView.visibility = View.GONE
                    workloadGraphView.visibility = View.GONE
                    resTextView.visibility = View.GONE
                    cycleTimeInputContainer.visibility = View.GONE
                }

                is State.Error -> {
                    //println("ERROR")
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    workloadGraphView.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = state.message
                    resTextView.visibility = View.GONE
                    cycleTimeInputContainer.visibility = View.GONE
                }

                is State.Data<*> -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                    resTextView.visibility = View.VISIBLE
                    cycleTimeInputContainer.visibility = View.GONE

                    val stat = state.data as? WorkloadStatistic
                    if (stat != null) {
                        val dataForGraph = stat.getSortedWeekDays().map { (day, load) ->
                            val dayName = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            dayName to load.toFloat()
                        }
                        resTextView.text = stat.getRecommendations()
                        workloadGraphView.visibility = View.VISIBLE
                        workloadGraphView.setData(dataForGraph)
                    } else {
                        resTextView.visibility = View.GONE
                        workloadGraphView.visibility = View.GONE
                        errorTextView.visibility = View.VISIBLE
                        errorTextView.text = "Ошибка: данные не загружены."
                    }
                }
            }
        }

        viewModel.cycleTimeStatistic.observe(this) { state ->
            when (state) {
                is State.Loading -> {
                    //println("LOADING")
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                    errorTextView.visibility = View.GONE
                    resTextView.visibility = View.GONE
                    cycleTimeGraphView.visibility = View.GONE

                }

                is State.Error -> {
                    //println("ERROR")
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    resTextView.visibility = View.GONE
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = state.message
                    cycleTimeGraphView.visibility = View.GONE
                }

                is State.Data<*> -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                    resTextView.visibility = View.VISIBLE

                    val stat = state.data as? CycleTimeStatistic
                    if (stat != null) {
                        resTextView.text = stat.getRecommendations()
                        cycleTimeGraphView.visibility = View.VISIBLE
                        val percentileData: Map<Double, Duration> = stat.percentileValues.mapKeys { (percentile, _) ->
                            percentile.getValue()
                        }
                        cycleTimeGraphView.setData(stat.cycleTimes, percentileData)
                    } else {
                        cycleTimeGraphView.visibility = View.GONE
                        resTextView.visibility = View.GONE
                        errorTextView.visibility = View.VISIBLE
                        errorTextView.text = "Ошибка: данные не загружены."
                    }
                }
            }
        }

        if (savedInstanceState == null)
            loadWorkloadGraph()

        buttonReturn.setOnClickListener {
            finish()
        }

        // Кнопка Workload
        buttonWorkload.setOnClickListener {
            loadWorkloadGraph()
        }

        // Кнопка Cycle Time
        buttonCycleTime.setOnClickListener {
            loadCycleTimeGraph()
        }

        // При нажатии на кнопку "Подсчитать"
        buttonCalculateCycle.setOnClickListener {
            val input = inputNumbers.text.toString()
            val percentiles = input.split(" ")
                .mapNotNull { it.toDoubleOrNull() }
                .filter { it in 0.0..1.0 }

            if (percentiles.isEmpty()) {
                Toast.makeText(this, "Введите хотя бы одно число от 0.0 до 1.0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategoryIndex = categorySpinner.selectedItemPosition
            val selectedCategory = categories[selectedCategoryIndex].id
            viewModel.getCycleTimeStatistic(selectedCategory, percentiles)

            lastPercentilesGraphRun = percentiles
            lastCategoryIdGraphRun = selectedCategory
        }

        loadCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun loadWorkloadGraph() {
        currentGraph = GraphType.WORKLOAD
        cycleTimeInputContainer.visibility = View.GONE
        cycleTimeGraphView.visibility = View.GONE
        workloadGraphView.visibility = View.GONE
        titleTextView.text = "Загруженность на следующую неделю:"
        viewModel.getWorkloadStatistic()
    }

    private fun loadCycleTimeGraph() {
        currentGraph = GraphType.CYCLE_TIME
        workloadGraphView.visibility = View.GONE
        shimmerLayout.visibility = View.GONE
        resTextView.visibility = View.GONE
        errorTextView.visibility = View.GONE
        titleTextView.text = "Анализ времени выполнения задач:"
        cycleTimeInputContainer.visibility = View.VISIBLE
        if (lastCategoryIdGraphRun == null) {
            lastCategoryIdGraphRun = categories[0].id
        }
        val selectedCategory: Int = lastCategoryIdGraphRun!!
        val percentiles: List<Double> = lastPercentilesGraphRun
        viewModel.getCycleTimeStatistic(selectedCategory, percentiles)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("inputNumbers", inputNumbers.text.toString())
        outState.putString("currentGraph", currentGraph.name)

        val selectedCategory = categories.getOrNull(categorySpinner.selectedItemPosition)
        if (selectedCategory != null) {
            outState.putInt("selectedCategoryId", selectedCategory.id)
        }

        lastCategoryIdGraphRun?.let {
            outState.putInt("lastCategoryIdGraphRun", it)
        }
        outState.putDoubleArray("lastPercentilesGraphRun", lastPercentilesGraphRun.toDoubleArray())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        inputNumbers.text = savedInstanceState.getString("inputNumbers", "")

        // ID категории устанавливается только после загрузки категорий
        if (savedInstanceState.containsKey("selectedCategoryId")) {
            restoredCategoryId = savedInstanceState.getInt("selectedCategoryId")
        }
        // Восстанавливается текущий график
        val graphName = savedInstanceState.getString("currentGraph", GraphType.WORKLOAD.name)
        currentGraph = GraphType.valueOf(graphName)
        if (currentGraph == GraphType.WORKLOAD){
            loadWorkloadGraph()
        }

        if (savedInstanceState.containsKey("lastCategoryIdGraphRun")) {
            lastCategoryIdGraphRun = savedInstanceState.getInt("lastCategoryIdGraphRun")
        }
        savedInstanceState.getDoubleArray("lastPercentilesGraphRun")?.let {
            lastPercentilesGraphRun = it.toList()
        }
    }

    private fun loadCategories() {
        categoryViewModel.state.observe(this) { state ->
            when (state) {
                is State.Loading -> {
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                }
                is State.Error -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is State.Data -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    categories = state.data
                    val items = categories.map { "${it.name}" }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    categorySpinner.adapter = adapter
                    // применение сохранённой категории
                    restoredCategoryId?.let { id ->
                        val index = categories.indexOfFirst { it.id == id }
                        if (index != -1) {
                            categorySpinner.setSelection(index)
                        }
                        restoredCategoryId = null
                    }
                    if (currentGraph == GraphType.CYCLE_TIME) { loadCycleTimeGraph() }
                }
            }
        }
        categoryViewModel.getCategories()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, StatisticActivity::class.java)
    }
}