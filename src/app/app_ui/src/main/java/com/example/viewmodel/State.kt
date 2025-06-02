package com.example.viewmodel

// статусы загрузки данных (как на семинаре)
sealed interface State<T> {
    // загрузка данных
    class Loading<T> : State<T>

    // данные загружены
    class Data<T>(val data: T) : State<T>

    // ошибка загрузки данных
    class Error<T>(val message: String) : State<T>
}