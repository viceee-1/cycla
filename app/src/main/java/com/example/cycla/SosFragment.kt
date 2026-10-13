package com.example.cycla

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SosFragment : Fragment(R.layout.fragment_sos) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var nombreUsuarioGlobal = "Sin Nombre"

    // Vistas Ficha Médica
    private lateinit var layoutInfoMedica: LinearLayout
    private lateinit var layoutEditarMedica: LinearLayout
    // Textviews
    private lateinit var tvFichaNombre: TextView
    private lateinit var tvFichaSangre: TextView
    private lateinit var tvFichaPeso: TextView
    private lateinit var tvFichaAltura: TextView
    private lateinit var tvFichaNacimiento: TextView
    private lateinit var tvFichaDonante: TextView
    // Edittexts
    private lateinit var etFichaNombre: EditText
    private lateinit var etFichaSangre: EditText
    private lateinit var etFichaPeso: EditText
    private lateinit var etFichaAltura: EditText
    private lateinit var etFichaNacimiento: EditText
    private lateinit var etFichaDonante: EditText

    // Vistas Contactos
    private lateinit var layoutVistaContactos: LinearLayout
    private lateinit var layoutAgregarContacto: LinearLayout
    private lateinit var llListaContactos: LinearLayout
    private lateinit var etNuevoNombreContacto: EditText
    private lateinit var etNuevoTelContacto: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        layoutInfoMedica = view.findViewById(R.id.layoutInfoMedica)
        layoutEditarMedica = view.findViewById(R.id.layoutEditarMedica)

        tvFichaNombre = view.findViewById(R.id.tvFichaNombre)
        tvFichaSangre = view.findViewById(R.id.tvFichaSangre)
        tvFichaPeso = view.findViewById(R.id.tvFichaPeso)
        tvFichaAltura = view.findViewById(R.id.tvFichaAltura)
        tvFichaNacimiento = view.findViewById(R.id.tvFichaNacimiento)
        tvFichaDonante = view.findViewById(R.id.tvFichaDonante)

        etFichaNombre = view.findViewById(R.id.etFichaNombre)
        etFichaSangre = view.findViewById(R.id.etFichaSangre)
        etFichaPeso = view.findViewById(R.id.etFichaPeso)
        etFichaAltura = view.findViewById(R.id.etFichaAltura)
        etFichaNacimiento = view.findViewById(R.id.etFichaNacimiento)
        etFichaDonante = view.findViewById(R.id.etFichaDonante)

        layoutVistaContactos = view.findViewById(R.id.layoutVistaContactos)
        layoutAgregarContacto = view.findViewById(R.id.layoutAgregarContacto)
        llListaContactos = view.findViewById(R.id.llListaContactos)
        etNuevoNombreContacto = view.findViewById(R.id.etNuevoNombreContacto)
        etNuevoTelContacto = view.findViewById(R.id.etNuevoTelContacto)

        view.findViewById<TextView>(R.id.tvEditarInfoMedica).setOnClickListener {
            mostrarEdicionFicha()
        }
        view.findViewById<Button>(R.id.btnAplicarFicha).setOnClickListener {
            guardarFichaLocal()
        }
        view.findViewById<Button>(R.id.btnMostrarAddContacto).setOnClickListener {
            layoutVistaContactos.visibility = View.GONE
            layoutAgregarContacto.visibility = View.VISIBLE
        }
        view.findViewById<Button>(R.id.btnCancelarContacto).setOnClickListener {
            layoutAgregarContacto.visibility = View.GONE
            layoutVistaContactos.visibility = View.VISIBLE
        }
        view.findViewById<Button>(R.id.btnGuardarContacto).setOnClickListener {
            guardarContactoFirebase()
        }

        // --- NAVBAR INFERIOR ---
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_sos

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    parentFragmentManager.popBackStack()
                    true
                }
                R.id.nav_usuario -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, UsuarioFragment())
                        .addToBackStack(null).commit()
                    true
                }
                R.id.nav_ajustes -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AjustesFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_sos -> true
                else -> false
            }
        }

        cargarNombreDesdeFirebase()
        cargarContactosDesdeFirebase()
    }

    private fun cargarNombreDesdeFirebase() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
            nombreUsuarioGlobal = doc.getString("nombre") ?: "Sin nombre"
            cargarFichaLocal() // Carga el resto (peso, sangre) desde el almacenamiento interno
        }
    }

    private fun cargarFichaLocal() {
        val uid = auth.currentUser?.uid ?: return
        val safeContext = context ?: return
        val sharedPref = safeContext.getSharedPreferences("FichaMedica_$uid", Context.MODE_PRIVATE)

        val sangre = sharedPref.getString("sangre", "") ?: ""
        val peso = sharedPref.getString("peso", "") ?: ""
        val altura = sharedPref.getString("altura", "") ?: ""
        val nacimiento = sharedPref.getString("nacimiento", "") ?: ""
        val donante = sharedPref.getString("donante", "") ?: ""

        tvFichaNombre.text = "Nombre: $nombreUsuarioGlobal"
        tvFichaSangre.text = "Grupo Sanguíneo: ${sangre.ifEmpty { "Sin registrar" }}"
        tvFichaPeso.text = "Peso: ${peso.ifEmpty { "Sin registrar" }}"
        tvFichaAltura.text = "Altura: ${altura.ifEmpty { "Sin registrar" }}"
        tvFichaNacimiento.text = "Fecha de Nacimiento: ${nacimiento.ifEmpty { "Sin registrar" }}"
        tvFichaDonante.text = "Es donante de órganos: ${donante.ifEmpty { "No" }}"

        etFichaNombre.setText(nombreUsuarioGlobal)
        etFichaSangre.setText(sangre)
        etFichaPeso.setText(peso)
        etFichaAltura.setText(altura)
        etFichaNacimiento.setText(nacimiento)
        etFichaDonante.setText(donante)
    }

    private fun mostrarEdicionFicha() {
        layoutInfoMedica.visibility = View.GONE
        layoutEditarMedica.visibility = View.VISIBLE
    }

    private fun guardarFichaLocal() {
        val uid = auth.currentUser?.uid ?: return
        val safeContext = context ?: return
        val sharedPref = safeContext.getSharedPreferences("FichaMedica_$uid", Context.MODE_PRIVATE)

        with(sharedPref.edit()) {
            putString("sangre", etFichaSangre.text.toString())
            putString("peso", etFichaPeso.text.toString())
            putString("altura", etFichaAltura.text.toString())
            putString("nacimiento", etFichaNacimiento.text.toString())
            putString("donante", etFichaDonante.text.toString())
            apply()
        }

        Toast.makeText(context, "Información médica guardada en el dispositivo", Toast.LENGTH_SHORT).show()
        cargarFichaLocal()
        layoutEditarMedica.visibility = View.GONE
        layoutInfoMedica.visibility = View.VISIBLE
    }

    private fun guardarContactoFirebase() {
        val uid = auth.currentUser?.uid ?: return
        val nombreCont = etNuevoNombreContacto.text.toString().trim()
        val telCont = etNuevoTelContacto.text.toString().trim()

        if (nombreCont.isEmpty() || telCont.isEmpty()) {
            Toast.makeText(context, "Llena ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        val contactoMap = hashMapOf(
            "nombre" to nombreCont,
            "telefono" to telCont
        )

        db.collection("usuarios").document(uid).collection("contactos").add(contactoMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Contacto guardado en la nube", Toast.LENGTH_SHORT).show()
                etNuevoNombreContacto.text.clear()
                etNuevoTelContacto.text.clear()
                layoutAgregarContacto.visibility = View.GONE
                layoutVistaContactos.visibility = View.VISIBLE
                cargarContactosDesdeFirebase()
            }
    }

    private fun cargarContactosDesdeFirebase() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uid).collection("contactos").get()
            .addOnSuccessListener { documentos ->
                llListaContactos.removeAllViews()
                for (doc in documentos) {
                    val idContacto = doc.id
                    val nombre = doc.getString("nombre") ?: ""
                    val telefono = doc.getString("telefono") ?: ""

                    val vistaContacto = LayoutInflater.from(context).inflate(R.layout.fragment_contacto, llListaContactos, false)

                    vistaContacto.findViewById<TextView>(R.id.tvNombreContactoItem).text = nombre
                    vistaContacto.findViewById<TextView>(R.id.tvTelContactoItem).text = telefono

                    vistaContacto.findViewById<ImageButton>(R.id.btnEliminarContacto).setOnClickListener {
                        db.collection("usuarios").document(uid).collection("contactos").document(idContacto).delete()
                            .addOnSuccessListener {
                                cargarContactosDesdeFirebase()
                            }
                    }

                    llListaContactos.addView(vistaContacto)
                }
            }
    }
}