package com.example.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.facade.Facade
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class AuthActivity : AppCompatActivity() {

    private val facade: Facade by KoinJavaComponent.inject(Facade::class.java)

    private lateinit var titleTextView: TextView
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var switchModeText: TextView
    private lateinit var actionButton: Button
    private lateinit var nameField: LinearLayout

    private var isRegistration = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        titleTextView = findViewById(R.id.titleTextView)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        nameInput = findViewById(R.id.nameInput)
        switchModeText = findViewById(R.id.switchModeText)
        actionButton = findViewById(R.id.actionButton)
        nameField = findViewById(R.id.nameField)

        updateUI()

        switchModeText.setOnClickListener {
            isRegistration = !isRegistration
            updateUI()
        }

        actionButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val name = nameInput.text.toString().trim()

            if (email.isBlank() || password.isBlank() || (isRegistration && name.isBlank())) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // запуск корутины в жизненном цикле Activity
            lifecycleScope.launch {
                try {
                    val success = if (isRegistration) {
                        facade.register(name, email, password)
                    } else {
                        facade.authorize(email, password)
                    }

                    if (success) {
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@AuthActivity, "Ошибка: неверные данные", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI() {
        if (isRegistration) {
            titleTextView.text = "Регистрация:"
            actionButton.text = "Зарегистрироваться"
            switchModeText.text = "Уже есть аккаунт? Войти"
            nameField.visibility = View.VISIBLE
        } else {
            titleTextView.text = "Авторизация:"
            actionButton.text = "Войти"
            switchModeText.text = "Нет аккаунта? Зарегистрироваться"
            nameField.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRegistration", isRegistration)
        outState.putString("email", emailInput.text.toString())
        outState.putString("password", passwordInput.text.toString())
        outState.putString("name", nameInput.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isRegistration = savedInstanceState.getBoolean("isRegistration", false)
        emailInput.setText(savedInstanceState.getString("email", ""))
        passwordInput.setText(savedInstanceState.getString("password", ""))
        nameInput.setText(savedInstanceState.getString("name", ""))
        updateUI()
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, AuthActivity::class.java)
    }
}