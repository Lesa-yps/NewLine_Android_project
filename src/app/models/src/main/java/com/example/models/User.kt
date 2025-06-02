package com.example.models

import java.time.LocalDate
import org.mindrot.jbcrypt.BCrypt
import java.io.Serializable


data class User (
    var id: Int,
    val name: NonEmptyString,
    val email: Email,
    val password: Password,
    val registrationDate: LocalDate
) {
    constructor(id: Int, nameStr: String, emailStr: String, passwordStr: String, registrationDate: LocalDate) :
            this(id, NonEmptyString(nameStr), Email(emailStr), Password(passwordStr), registrationDate)
}


data class Email(private val value: String) {
    init {
        require(isValidEmail(value)) { "Некорректный email: $value" }
    }

    override fun toString(): String = value

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

        fun isValidEmail(email: String): Boolean {
            return EMAIL_REGEX.matches(email)
        }
    }
}


data class Password(private var value: String) {
    init {
        require(isValidPassword(value)) { "Пароль слишком слабый: $value" }
        if (! isHashed(value)) { value = hashPassword() }   // Сохраняется только ХЕШ
    }

    override fun toString(): String = value

    companion object {
        private val PASSWORD_REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+$).{8,}$".toRegex()

        fun isValidPassword(password: String): Boolean {
            return PASSWORD_REGEX.matches(password)
        }

        fun isHashed(password: String): Boolean {
            val bcryptPattern = """^\$2[ayb]\$\d{2}\$[./A-Za-z0-9]{53}$""".toRegex()
            // println("[DEBUG] isHashed? ${bcryptPattern.matches(password)}")
            return bcryptPattern.matches(password)
        }
    }

    private fun hashPassword(): String {
        return BCrypt.hashpw(value, BCrypt.gensalt()) // Генерируется "солёный" хеш
    }

    fun checkPassword(checkPassword: String): Boolean {
        // println("[DEBUG] checkPassword? ${BCrypt.checkpw(checkPassword, value)} value = $value pass = $checkPassword")
        return BCrypt.checkpw(checkPassword, value)
    }
}

data class NonEmptyString(private val value: String) : Serializable {
    init {
        require(value.isNotBlank()) {
            "Строка не должна быть пустой или состоять только из пробелов"
        }
    }

    override fun toString(): String = value

    // метод для удобного получения значения
    fun getValue(): String = value
}