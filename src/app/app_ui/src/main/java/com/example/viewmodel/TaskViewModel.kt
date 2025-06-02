package com.example.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facade.Facade
import com.example.models.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class TaskViewModel : ViewModel() {
    private val _state = MutableLiveData<State<List<Task>>>(State.Loading())
    val state: LiveData<State<List<Task>>> get() = _state

    private val facade: Facade by KoinJavaComponent.inject(Facade::class.java)

    private var runningJob: Job? = null

    fun loadItems() {
        runningJob?.cancel()
        runningJob = viewModelScope.launch {
            _state.value = State.Loading()
            try {
                val tasks = facade.getTasks()
                _state.value = State.Data(tasks)
            } catch (e: Exception) {
                _state.value = State.Error("Ошибка загрузки задач: ${e.message}")
            }
        }
    }

    fun addItem(task: Task) {
        runningJob?.cancel()
        runningJob = viewModelScope.launch {
            _state.value = State.Loading()
            try {
                facade.createTask(task)
                loadItems()
            } catch (e: Exception) {
                _state.value = State.Error("Ошибка добавления задачи: ${e.message}")
            }
        }
    }

    fun moveItem(task: Task, side: Int, onError: (() -> Unit)? = null, onNotMove: (() -> Unit)? = null) {
        runningJob?.cancel()
        runningJob = viewModelScope.launch {
            _state.value = State.Loading()
            try {
                val success = facade.moveTaskBySide(task.id, side)
                if (!success) {
                    onNotMove?.invoke()
                    onError?.invoke()
                }
                loadItems()
            } catch (e: Exception) {
                _state.value = State.Error("Ошибка при перемещении: ${e.message}")
                onError?.invoke()
            }
        }
    }
}