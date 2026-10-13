package com.example.cycla

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RecuperarActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        auth = FirebaseAuth.getInstance()

        val etEmailRecuperar = findViewById<EditText>(R.id.etEmailRecuperar)
        val btnEnviarRecuperacion = findViewById<Button>(R.id.btnEnviarRecuperacion)
        val tvMensajeBandeja = findViewById<TextView>(R.id.tvMensajeBandeja)
        val tvVolverLogin = findViewById<TextView>(R.id.tvVolverLogin) // Nuevo botón

        btnEnviarRecuperacion.setOnClickListener {
            val email = etEmailRecuperar.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa tu correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        tvMensajeBandeja.visibility = View.VISIBLE
                        Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvVolverLogin.setOnClickListener {
            finish()
        }
    }
}