package com.example.cycla

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialFragment : Fragment(R.layout.fragment_historial) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvListaCompleta: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        tvListaCompleta = view.findViewById(R.id.tvHistorialListaCompleta)

        // Botón volver
        view.findViewById<Button>(R.id.btnVolverHistorial).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cargarHistorialCompleto()
    }

    private fun cargarHistorialCompleto() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            tvListaCompleta.text = "Inicia sesión para ver tu historial."
            return
        }

        db.collection("usuarios").document(uid).collection("historial_critico")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                val sb = java.lang.StringBuilder()

                for (doc in snapshots) {
                    val modo = doc.getString("modo") ?: "Desconocido"
                    val distancia = doc.getDouble("distancia") ?: 0.0
                    val hora = doc.getString("hora") ?: ""
                    val fecha = doc.getString("fecha") ?: ""

                    sb.append("- $modo, $distancia m, $hora, $fecha\n")
                }

                if (sb.isEmpty()) {
                    tvListaCompleta.text = "No hay eventos críticos registrados."
                } else {
                    tvListaCompleta.text = sb.toString().trim()
                }
            }
            .addOnFailureListener {
                tvListaCompleta.text = "Error al cargar la base de datos."
            }
    }
}