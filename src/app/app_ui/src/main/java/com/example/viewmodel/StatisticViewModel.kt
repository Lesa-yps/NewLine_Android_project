package com.example.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facade.Facade
import com.example.logic.CycleTimeStatistic
import com.example.logic.WorkloadStatistic
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class StatisticViewModel : ViewModel() {

    private val _workloadStatistic = MutableLiveData<State<WorkloadStatistic>>()
    val workloadStatistic: LiveData<State<WorkloadStatistic>> get() = _workloadStatistic

    private val _cycleTimeStatistic = MutableLiveData<State<CycleTimeStatistic>>()
    val cycleTimeStatistic: LiveData<State<CycleTimeStatistic>> get() = _cycleTimeStatistic

    private val facade: Facade by KoinJavaComponent.inject(Facade::class.java)

    fun getWorkloadStatistic() {
        viewModelScope.launch {
            _workloadStatistic.value = State.Loading()
            try {
                val stat = facade.calcStatisticWorkload()
                _workloadStatistic.value = State.Data(stat)
            } catch (e: Exception) {
                _workloadStatistic.value = State.Error("Ошибка статистики: ${e.message}")
            }
        }
    }

    fun getCycleTimeStatistic(categoryId: Int, percentiles: List<Double>) {
        viewModelScope.launch {
            _cycleTimeStatistic.value = State.Loading()
            try {
                val stat = facade.calcStatisticCycleTime(categoryId, percentiles)
                _cycleTimeStatistic.value = State.Data(stat)
            } catch (e: Exception) {
                _cycleTimeStatistic.value = State.Error("Ошибка статистики времени цикла: ${e.message}")
            }
        }
    }
}