package com.example.cycla

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class NodoBFragment : Fragment(R.layout.fragment_nodo_b) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnVolverNodoB).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}