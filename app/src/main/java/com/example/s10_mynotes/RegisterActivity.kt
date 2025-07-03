package com.example.s10_mynotes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var registrationJob: Job? = null

    // Views
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoLogin: Button
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Inicializar views
        initViews()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        etFirstName = findViewById(R.id.et_first_name)
        etLastName = findViewById(R.id.et_last_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        btnGoLogin = findViewById(R.id.btn_go_login)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupListeners() {
        // Listener para registrarse
        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInput(firstName, lastName, email, password, confirmPassword)) {
                registerUser(firstName, lastName, email, password)
            }
        }

        // Listener para ir a LoginActivity
        btnGoLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun validateInput(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        // Validar nombre
        if (firstName.isEmpty()) {
            etFirstName.error = "El nombre es requerido"
            etFirstName.requestFocus()
            return false
        }

        if (firstName.length < 2) {
            etFirstName.error = "El nombre debe tener al menos 2 caracteres"
            etFirstName.requestFocus()
            return false
        }

        // Validar apellido
        if (lastName.isEmpty()) {
            etLastName.error = "El apellido es requerido"
            etLastName.requestFocus()
            return false
        }

        if (lastName.length < 2) {
            etLastName.error = "El apellido debe tener al menos 2 caracteres"
            etLastName.requestFocus()
            return false
        }

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

        // Validar que las contraseñas coincidan
        if (password != confirmPassword) {
            etConfirmPassword.error = "Las contraseñas no coinciden"
            etConfirmPassword.requestFocus()
            return false
        }

        // Validar fortaleza de contraseña (opcional)
        if (!isPasswordStrong(password)) {
            etPassword.error = "La contraseña debe contener al menos una letra y un número"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun isPasswordStrong(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasNumber = password.any { it.isDigit() }
        return hasLetter && hasNumber
    }

    private fun registerUser(firstName: String, lastName: String, email: String, password: String) {
        Log.d(TAG, "Iniciando registro para: $email")
        setLoadingState(true)

        // Cancelar job anterior si existe
        registrationJob?.cancel()

        // Crear job con timeout
        registrationJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Timeout de 30 segundos
                withTimeout(30000) {
                    performRegistration(firstName, lastName, email, password)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Timeout en registro")
                setLoadingState(false)
                showToast("Error: El registro está tomando demasiado tiempo. Verifica tu conexión.")
            } catch (e: Exception) {
                Log.e(TAG, "Error en registro", e)
                setLoadingState(false)
                showToast("Error inesperado: ${e.message}")
            }
        }
    }

    private fun performRegistration(firstName: String, lastName: String, email: String, password: String) {
        Log.d(TAG, "Creando usuario con email y contraseña")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                Log.d(TAG, "Resultado createUserWithEmailAndPassword: ${task.isSuccessful}")

                if (task.isSuccessful) {
                    Log.d(TAG, "Usuario creado exitosamente")
                    // Registro exitoso - actualizar perfil del usuario
                    val user = auth.currentUser
                    val fullName = "$firstName $lastName"

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            Log.d(TAG, "Resultado updateProfile: ${profileTask.isSuccessful}")

                            if (profileTask.isSuccessful) {
                                Log.d(TAG, "Perfil actualizado, guardando en Firestore")
                                // Guardar datos adicionales en Firestore
                                saveUserToFirestore(firstName, lastName, email)
                            } else {
                                Log.e(TAG, "Error al actualizar perfil: ${profileTask.exception?.message}")
                                setLoadingState(false)
                                showToast("Error al actualizar perfil")
                            }
                        }
                } else {
                    Log.e(TAG, "Error al crear usuario: ${task.exception?.message}")
                    setLoadingState(false)
                    handleRegistrationError(task.exception)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Fallo createUserWithEmailAndPassword", exception)
                setLoadingState(false)
                showToast("Error de conexión: ${exception.message}")
            }
    }

    private fun saveUserToFirestore(firstName: String, lastName: String, email: String) {
        Log.d(TAG, "Guardando usuario en Firestore")

        val user = auth.currentUser
        val userId = user?.uid ?: return

        val userData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "fullName" to "$firstName $lastName",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "isActive" to true
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Usuario guardado exitosamente en Firestore")
                setLoadingState(false)
                showToast("¡Cuenta creada exitosamente!")

                // Cerrar sesión para forzar login
                auth.signOut()

                // Redirigir a LoginActivity
                navigateToLogin()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al guardar en Firestore", exception)
                setLoadingState(false)
                showToast("Error al guardar datos: ${exception.message}")
                // Aunque falle Firestore, el usuario ya está creado en Auth
                // También cerramos sesión en caso de error
                auth.signOut()
                navigateToLogin()
            }
    }

    private fun handleRegistrationError(exception: Exception?) {
        Log.e(TAG, "Error de registro", exception)

        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException ->
                "La contraseña es muy débil"
            is FirebaseAuthUserCollisionException ->
                "Ya existe una cuenta con este email"
            else ->
                "Error de registro: ${exception?.message}"
        }

        showToast(errorMessage)
    }

    private fun setLoadingState(isLoading: Boolean) {
        Log.d(TAG, "setLoadingState: $isLoading")

        if (isLoading) {
            // Desactivar campos y botones
            setFieldsEnabled(false)
            btnRegister.text = "Creando cuenta..."
            progressBar.visibility = View.VISIBLE
        } else {
            // Reactivar campos y botones
            setFieldsEnabled(true)
            btnRegister.text = "CREAR CUENTA"
            progressBar.visibility = View.GONE
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        etFirstName.isEnabled = enabled
        etLastName.isEnabled = enabled
        etEmail.isEnabled = enabled
        etPassword.isEnabled = enabled
        etConfirmPassword.isEnabled = enabled
        btnRegister.isEnabled = enabled
        btnGoLogin.isEnabled = enabled
    }

    private fun navigateToLogin() {
        Log.d(TAG, "Navegando a LoginActivity")

        // para ir a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Toast: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        registrationJob?.cancel()
        navigateToLogin()
    }

    override fun onDestroy() {
        super.onDestroy()
        registrationJob?.cancel()
    }
}