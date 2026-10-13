package com.example.cycla

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult

class PersonalizarModoFragment : Fragment(R.layout.fragment_personalizar_modo) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etBajo = view.findViewById<EditText>(R.id.etRangoBajo)
        val etMedio = view.findViewById<EditText>(R.id.etRangoMedio)
        val etCritico = view.findViewById<EditText>(R.id.etRangoCritico)
        val btnAplicar = view.findViewById<Button>(R.id.btnAplicarModo)

        btnAplicar.setOnClickListener {
            val strBajo = etBajo.text.toString().ifEmpty { "2.0m - 3.0m" }
            val strMedio = etMedio.text.toString().ifEmpty { "1.0m - 2.0m" }
            val strCritico = etCritico.text.toString().ifEmpty { "<= 1.0m" }

            val umbralesActualizados = "Umbrales de proximidad\nBajo: $strBajo\nMedio: $strMedio\nCrítico: $strCritico"

            setFragmentResult("requestKeyModo", bundleOf("nuevosUmbrales" to umbralesActualizados))

            parentFragmentManager.popBackStack()
        }
    }
}