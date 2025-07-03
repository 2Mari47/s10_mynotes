package com.example.s10_mynotes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Views
    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnAddNote: Button
    private lateinit var fabAddNote: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Verificar autenticación
        checkAuthentication()

        // Inicializar views
        initViews()

        // Configurar UI
        setupUI()

        // Configurar listeners
        setupListeners()
    }

    private fun checkAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Usuario no autenticado, redirigir al login
            redirectToLogin()
            return
        }
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tv_welcome)
        btnLogout = findViewById(R.id.btn_logout)
        btnAddNote = findViewById(R.id.btn_add_note)
        fabAddNote = findViewById(R.id.fab_add_note)
    }

    private fun setupUI() {
        // Mostrar información del usuario actual
        val currentUser = auth.currentUser
        val displayName = currentUser?.displayName
        val email = currentUser?.email

        // Mostrar nombre si está disponible, sino mostrar email
        val welcomeText = if (!displayName.isNullOrEmpty()) {
            "¡Hola, $displayName!"
        } else {
            "¡Hola, $email!"
        }

        tvWelcome.text = welcomeText
    }

    private fun setupListeners() {
        // ÚNICO BOTÓN FUNCIONAL: Cerrar sesión
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Estos botones NO funcionan - solo muestran mensaje
        btnAddNote.setOnClickListener {
            showNotImplementedMessage()
        }

        fabAddNote.setOnClickListener {
            showNotImplementedMessage()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performLogout() {
        // Cerrar sesión en Firebase
        auth.signOut()

        // Mostrar mensaje de confirmación
        showToast("Sesión cerrada correctamente")

        // Redirigir al login
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showNotImplementedMessage() {
        showToast("Esta función aún no está implementada")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // Verificar autenticación cada vez que se inicia la actividad
        checkAuthentication()
    }

    override fun onBackPressed() {
        // Mostrar diálogo de confirmación para salir de la app
        AlertDialog.Builder(this)
            .setTitle("Salir de la aplicación")
            .setMessage("¿Deseas salir de Mis Notas?")
            .setPositiveButton("Salir") { _, _ ->
                finishAffinity() // Cierra completamente la app
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}