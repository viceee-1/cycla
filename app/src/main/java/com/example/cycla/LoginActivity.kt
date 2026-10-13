package com.example.cycla

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cycla.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            irAMain()
            return
        }

        binding.btnAction.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Ingresa tu correo"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (pass.isEmpty()) {
                binding.tilPassword.error = "Ingresa tu contraseña"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            } else if (pass.length < 8) {
                binding.tilPassword.error = "La contraseña debe tener mínimo 8 caracteres"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            iniciarSesion(email, pass)
        }

        binding.tvSwitchMode.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        binding.tvForgotPassword.setOnClickListener {
            val intent = Intent(this, RecuperarActivity::class.java)
            startActivity(intent)
        }
    }

    private fun iniciarSesion(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Autenticación exitosa", Toast.LENGTH_SHORT).show()
                irAMain()
            } else {
                val errorMsg = task.exception?.localizedMessage ?: "Error de autenticación"
                Toast.makeText(this, "Error al iniciar sesión: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAMain() {

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}