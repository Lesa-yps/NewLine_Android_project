package com.example.logic

import com.example.models.Email
import com.example.models.User
import java.util.logging.Logger


interface IAuthorizeUserService {
    suspend fun authorize(email: Email, passwordStr: String): Int
    suspend fun register(newUser: User): Int?
}


class AuthorizeUserService(private val databaseService: IDatabaseService) : IAuthorizeUserService {

    private val logger = Logger.getLogger(AuthorizeUserService::class.java.name)

    override suspend fun authorize(email: Email, passwordStr: String): Int {
        logger.info("Попытка авторизовать пользователя с email: $email")
        val user = databaseService.getUserByEmail(email)

        return if (user != null && user.password.checkPassword(passwordStr)) {
            logger.info("Успешная авторизация пользователя с id: ${user.id}")
            user.id
        } else {
            logger.warning("Неуспешная попытка авторизации с email: $email")
            throw IllegalArgumentException("Пользователя с такими данными не существует.")
        }
    }

    override suspend fun register(newUser: User): Int? {
        logger.info("Регистрация нового пользователя: ${newUser.email}")
        if (databaseService.getUserByEmail(newUser.email) != null) {
            logger.warning("Регистрация отклонена: пользователь с таким email уже существует.")
            throw IllegalArgumentException("Пользователь с таким email уже существует.")
        }
        val userId = databaseService.createUser(newUser)
        logger.info("Пользователь зарегистрирован с id: $userId")
        return userId
    }
}
