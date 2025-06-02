package com.example

import android.app.Application
import com.example.data.DatabaseService
import com.example.facade.Facade
import com.example.logic.AuthorizeUserService
import com.example.logic.IAuthorizeUserService
import com.example.logic.IDatabaseService
import com.example.logic.IStatisticService
import com.example.logic.ITaskService
import com.example.logic.StatisticService
import com.example.logic.TaskService
import com.example.viewmodel.CategoryViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.startKoin

// Определяется Koin-модуль (контейнер зависимостей) с зависимостями
val appModule: Module = module {
    single<IDatabaseService> { DatabaseService() }
    single<IAuthorizeUserService> { AuthorizeUserService(get()) }
    single<ITaskService> { TaskService(get()) }
    single<IStatisticService> { StatisticService(get()) }
    single { Facade(get(), get(), get()) }
    // Регистрация ViewModel
    viewModel { CategoryViewModel() }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule)
        }
    }
}
