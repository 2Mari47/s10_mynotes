package com.example.s10_mynotes

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoRegister: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Verificar si el usuario ya está logueado
        checkCurrentUser()

        // Inicializar views
        initViews()

        // Configurar listeners
        setupListeners()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya está logueado, ir directo a MainActivity
            navigateToMain()
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnGoRegister = findViewById(R.id.btn_go_register)

        // Agregar ProgressBar al XML si no lo tienes
        // progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupListeners() {
        // Listener para login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        // Listener para ir a RegisterActivity
        btnGoRegister.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        // Validar email vacío
        if (email.isEmpty()) {
            etEmail.error = "El email es requerido"
            etEmail.requestFocus()
            return false
        }

        // Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un email válido"
            etEmail.requestFocus()
            return false
        }

        // Validar password vacío
        if (password.isEmpty()) {
            etPassword.error = "La contraseña es requerida"
            etPassword.requestFocus()
            return false
        }

        // Validar longitud mínima de password
        if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        // Mostrar loading
        setLoadingState(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoadingState(false)

                if (task.isSuccessful) {
                    // Login exitoso
                    showToast("¡Bienvenido de vuelta!")
                    navigateToMain()
                } else {
                    // Login fallido - manejar diferentes tipos de errores
                    handleLoginError(task.exception)
                }
            }
    }

    private fun handleLoginError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException ->
                "No existe una cuenta con este email"
            is FirebaseAuthInvalidCredentialsException ->
                "Email o contraseña incorrectos"
            else ->
                "Error de conexión. Verifica tu internet"
        }

        showToast(errorMessage)

        // Limpiar password por seguridad
        etPassword.text.clear()
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            // Desactivar botones y mostrar progreso
            btnLogin.isEnabled = false
            btnGoRegister.isEnabled = false
            btnLogin.text = "Iniciando sesión..."

            // Si tienes ProgressBar, mostrarlo
            // progressBar.visibility = View.VISIBLE
        } else {
            // Reactivar botones y ocultar progreso
            btnLogin.isEnabled = true
            btnGoRegister.isEnabled = true
            btnLogin.text = "INICIAR SESIÓN"

            // Si tienes ProgressBar, ocultarlo
            // progressBar.visibility = View.GONE
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // Verificar si el usuario sigue autenticado
        checkCurrentUser()
    }
}