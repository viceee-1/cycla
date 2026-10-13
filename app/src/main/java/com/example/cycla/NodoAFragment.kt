package com.example.cycla

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class NodoAFragment : Fragment(R.layout.fragment_nodo_a) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnVolverNodoA).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}