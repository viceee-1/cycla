package com.example.cycla

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class AjustesFragment : Fragment(R.layout.fragment_ajustes) {

    private lateinit var cardClasico: MaterialCardView
    private lateinit var cardDaltonismo: MaterialCardView
    private lateinit var sbBuzzer: SeekBar
    private val MODO_CLASICO = 0
    private val MODO_DALTONISMO = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardClasico = view.findViewById(R.id.cardLucesClasico)
        cardDaltonismo = view.findViewById(R.id.cardLucesDaltonismo)
        sbBuzzer = view.findViewById(R.id.sbBuzzerVolumen)

        cargarAjustes()

        cardClasico.setOnClickListener {
            seleccionarModoLuces(MODO_CLASICO)
        }

        cardDaltonismo.setOnClickListener {
            seleccionarModoLuces(MODO_DALTONISMO)
        }

        sbBuzzer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    guardarVolumen(seekBar.progress)
                }
            }
        })

        view.findViewById<Button>(R.id.btnEmergencia)?.setOnClickListener {
        }

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_ajustes // Marca la pestaña actual

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    true
                }
                R.id.nav_usuario -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, UsuarioFragment())
                        .addToBackStack(null).commit()
                    true
                }
                R.id.nav_ajustes -> true // Ya estamos aquí
                R.id.nav_sos -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, SosFragment())
                        .addToBackStack(null).commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun cargarAjustes() {
        val safeContext = context ?: return
        val sharedPref = safeContext.getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val modoSeleccionado = sharedPref.getInt("modo_luces", MODO_CLASICO) // Por defecto Clásico
        val volumenGuardado = sharedPref.getInt("volumen_buzzer", 50) // Por defecto 50%

        seleccionarModoLuces(modoSeleccionado)
        sbBuzzer.progress = volumenGuardado
    }

    private fun seleccionarModoLuces(modo: Int) {
        val safeContext = context ?: return
        val sharedPref = safeContext.getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)

        if (modo == MODO_CLASICO) {
            cardClasico.setCardBackgroundColor(Color.parseColor("#E0E0E0")) // Gris claro (seleccionado)
            cardDaltonismo.setCardBackgroundColor(Color.parseColor("#FFFFFF")) // Blanco
        } else {
            cardClasico.setCardBackgroundColor(Color.parseColor("#FFFFFF")) // Blanco
            cardDaltonismo.setCardBackgroundColor(Color.parseColor("#E0E0E0")) // Gris claro (seleccionado)
        }

        with(sharedPref.edit()) {
            putInt("modo_luces", modo)
            apply()
        }
    }

    private fun guardarVolumen(volumen: Int) {
        val safeContext = context ?: return
        val sharedPref = safeContext.getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("volumen_buzzer", volumen)
            apply()
        }
    }
}