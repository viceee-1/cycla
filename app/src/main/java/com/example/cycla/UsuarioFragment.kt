package com.example.cycla

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsuarioFragment : Fragment(R.layout.fragment_usuario) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvNombre = view.findViewById<TextView>(R.id.tvNombreUsuario)
        val tvCorreo = view.findViewById<TextView>(R.id.tvCorreoUsuario)
        val tvTelefono = view.findViewById<TextView>(R.id.tvTelefonoUsuario)

        val btnEditCorreo = view.findViewById<ImageButton>(R.id.btnEditCorreo)
        val btnEditTelefono = view.findViewById<ImageButton>(R.id.btnEditTelefono)
        val btnEditContrasena = view.findViewById<ImageButton>(R.id.btnEditContrasena)

        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesion)
        val btnEmergencia = view.findViewById<Button>(R.id.btnEmergencia)
        val bottomNavigation = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)

        cargarDatosUsuario(tvNombre, tvCorreo, tvTelefono)

        bottomNavigation?.selectedItemId = R.id.nav_usuario
        bottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    parentFragmentManager.popBackStack()
                    true
                }
                R.id.nav_usuario -> true
                R.id.nav_ajustes -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AjustesFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_sos -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SosFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }

        btnEditCorreo?.setOnClickListener {
            mostrarDialogoEditarCorreo(tvCorreo)
        }
        btnEditTelefono?.setOnClickListener {
            mostrarDialogoEditarTelefono(tvTelefono)
        }
        btnEditContrasena?.setOnClickListener {
            mostrarDialogoEditarContrasena()
        }

        btnEmergencia?.setOnClickListener {
            Toast.makeText(context ?: return@setOnClickListener, "Solicitando asistencia de emergencia...", Toast.LENGTH_SHORT).show()
        }

        btnCerrarSesion?.setOnClickListener {
            auth.signOut()
            val safeContext = context ?: return@setOnClickListener
            val intent = Intent(safeContext, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun cargarDatosUsuario(tvNombre: TextView?, tvCorreo: TextView?, tvTelefono: TextView?) {
        val user = auth.currentUser

        if (user != null) {
            tvCorreo?.text = "Correo Electrónico: ${user.email}"

            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (context == null) return@addOnSuccessListener
                    val nombre = doc.getString("nombre") ?: "Sin registrar"
                    val telefono = doc.getString("telefono") ?: "Sin registrar"

                    tvNombre?.text = "Nombre: $nombre"
                    tvTelefono?.text = "Teléfono: $telefono"
                }
                .addOnFailureListener {
                    if (context == null) return@addOnFailureListener
                    tvNombre?.text = "Nombre: Error de carga"
                    tvTelefono?.text = "Teléfono: Error de carga"
                }
        } else {
            tvNombre?.text = "Nombre: Invitado"
            tvCorreo?.text = "Correo Electrónico: No disponible"
            tvTelefono?.text = "Teléfono: No disponible"
        }
    }

    private fun mostrarDialogoEditarTelefono(tvTelefono: TextView?) {
        val safeContext = context ?: return
        val user = auth.currentUser ?: return

        val input = EditText(safeContext)
        input.hint = "Ingresa nuevo teléfono"
        input.inputType = InputType.TYPE_CLASS_PHONE

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Editar Teléfono")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevoTel = input.text.toString().trim()
                if (nuevoTel.isNotEmpty()) {
                    db.collection("usuarios").document(user.uid).update("telefono", nuevoTel)
                        .addOnSuccessListener {
                            if (context == null) return@addOnSuccessListener
                            tvTelefono?.text = "Teléfono: $nuevoTel"
                            Toast.makeText(context, "Teléfono actualizado", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoEditarCorreo(tvCorreo: TextView?) {
        val safeContext = context ?: return
        val user = auth.currentUser ?: return

        val input = EditText(safeContext)
        input.hint = "Ingresa nuevo correo"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Editar Correo Electrónico")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevoCorreo = input.text.toString().trim()
                if (nuevoCorreo.isNotEmpty()) {
                    user.updateEmail(nuevoCorreo).addOnCompleteListener { task ->
                        if (context == null) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            tvCorreo?.text = "Correo Electrónico: $nuevoCorreo"
                            Toast.makeText(context, "Correo actualizado exitosamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoEditarContrasena() {
        val safeContext = context ?: return
        val user = auth.currentUser ?: return

        val layout = LinearLayout(safeContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val etActual = EditText(safeContext)
        etActual.hint = "Contraseña actual"
        etActual.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etActual)

        val etNueva = EditText(safeContext)
        etNueva.hint = "Nueva contraseña (min. 8 caracteres)"
        etNueva.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etNueva)

        val etRepetir = EditText(safeContext)
        etRepetir.hint = "Confirmar nueva contraseña"
        etRepetir.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etRepetir)

        MaterialAlertDialogBuilder(safeContext)
            .setTitle("Cambiar Contraseña")
            .setView(layout)
            .setPositiveButton("Actualizar") { dialog, _ ->
                val passActual = etActual.text.toString().trim()
                val passNueva = etNueva.text.toString().trim()
                val passRepetir = etRepetir.text.toString().trim()

                if (passNueva.length < 8) {
                    Toast.makeText(safeContext, "La contraseña debe tener mínimo 8 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (passNueva != passRepetir) {
                    Toast.makeText(safeContext, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val email = user.email
                if (email != null && passActual.isNotEmpty()) {
                    val credential = EmailAuthProvider.getCredential(email, passActual)
                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (context == null) return@addOnCompleteListener
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(passNueva).addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}