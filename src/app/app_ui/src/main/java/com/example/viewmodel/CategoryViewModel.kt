package com.example.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.viewmodel.State
import com.example.facade.Facade
import com.example.models.Category
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class CategoryViewModel : ViewModel() {

    private val _state = MutableLiveData<State<List<Category>>>()
    val state: LiveData<State<List<Category>>> get() = _state

    private val facade: Facade by KoinJavaComponent.inject(Facade::class.java)

    fun getCategories() {
        viewModelScope.launch {
            _state.value = State.Loading()
            try {
                val stat = facade.getCategories()
                _state.value = State.Data(stat)
            } catch (e: Exception) {
                _state.value = State.Error("Ошибка статистики: ${e.message}")
            }
        }
    }

    suspend fun addItem(name: String, color: String): Int? {
        return try {
            _state.postValue(State.Loading())
            facade.createCategory(name, color)
        } catch (e: Exception) {
            _state.postValue(State.Error("Ошибка добавления задачи: ${e.message}"))
            null
        }
    }
}