package com.example.cycla

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
data class ModoProximidad(
    val titulo: String,
    val umbrales: String,
    val mostrarPersonalizar: Boolean
)

class ModoAdapter(
    private val listaModos: List<ModoProximidad>,
    private val onPersonalizarClick: () -> Unit
) : RecyclerView.Adapter<ModoAdapter.ModoViewHolder>() {

    class ModoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloModo)
        val tvUmbrales: TextView = view.findViewById(R.id.tvUmbrales)
        val tvPersonalizar: TextView = view.findViewById(R.id.tvPersonalizar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_modo_proximidad, parent, false)
        return ModoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModoViewHolder, position: Int) {
        val modo = listaModos[position]
        holder.tvTitulo.text = modo.titulo

        if (modo.umbrales.isEmpty()) {
            holder.tvUmbrales.visibility = View.GONE
        } else {
            holder.tvUmbrales.visibility = View.VISIBLE
            holder.tvUmbrales.text = modo.umbrales
        }

        if (modo.mostrarPersonalizar) {
            holder.tvPersonalizar.visibility = View.VISIBLE
            holder.tvPersonalizar.setOnClickListener { onPersonalizarClick() }
        } else {
            holder.tvPersonalizar.visibility = View.GONE
        }
    }

    override fun getItemCount() = listaModos.size
}

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var historialListener: ListenerRegistration? = null

    private var dotNodoA: View? = null
    private var dotNodoB: View? = null
    private var imgWifiStatus: ImageView? = null
    private var tvUltimosEventos: TextView? = null

    private var isWifiConnected = false
    private var modoActivoActual: ModoProximidad? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (smsGranted && locGranted) {
            enviarSmsEmergencia()
        } else {
            Toast.makeText(requireContext(), "Faltan permisos para completar la acción", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        dotNodoA = view.findViewById(R.id.dotEstadoNodoA)
        dotNodoB = view.findViewById(R.id.dotEstadoNodoB)
        imgWifiStatus = view.findViewById(R.id.imgWifiStatus)
        tvUltimosEventos = view.findViewById(R.id.tvUltimosEventos)

        view.findViewById<ImageButton>(R.id.btnCerrarNotif)?.setOnClickListener {
            view.findViewById<MaterialCardView>(R.id.cardNotificationBanner)?.visibility = View.GONE
        }

        view.findViewById<Button>(R.id.btnEmergencia)?.setOnClickListener {
            val safeContext = context ?: return@setOnClickListener
            val hasSms = ContextCompat.checkSelfPermission(safeContext, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
            val hasLoc = ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (hasSms && hasLoc) {
                enviarSmsEmergencia()
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        imgWifiStatus?.setOnClickListener {
            val safeContext = context ?: return@setOnClickListener
            val hasLoc = ContextCompat.checkSelfPermission(safeContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasLoc) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                Toast.makeText(safeContext, "Se requieren permisos de ubicación para escanear redes WiFi", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isWifiConnected) {
                mostrarDialogoWifi()
            } else {
                desconectarWifiESP32()
            }
        }

        view.findViewById<View>(R.id.layoutNodoA)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, NodoAFragment()).addToBackStack(null).commit()
        }
        view.findViewById<View>(R.id.layoutNodoB)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, NodoBFragment()).addToBackStack(null).commit()
        }

        val vpModos = view.findViewById<ViewPager2>(R.id.vpModosProximidad)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMode)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMode)

        val listaDeModos = mutableListOf(
            ModoProximidad("MODO\nURBANO", "Umbrales de proximidad\nBajo: 2.0 m a 3.0 m\nMedio: 1.5 m a 2.0 m\nCrítico: <= 1.0 m", false),
            ModoProximidad("MODO\nCARRETERA", "Umbrales de proximidad\nBajo: 7.0 m a 5.0 m\nMedio: 5.0 m a 3.0 m\nCrítico: <= 2.0 m", false),
            ModoProximidad("MI MODO", "Umbrales de proximidad\n(No configurado)", true)
        )

        val adaptadorModos = ModoAdapter(listaDeModos) {
            parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, PersonalizarModoFragment()).addToBackStack(null).commit()
        }
        vpModos?.adapter = adaptadorModos

        parentFragmentManager.setFragmentResultListener("requestKeyModo", viewLifecycleOwner) { _, bundle ->
            val nuevosUmbrales = bundle.getString("nuevosUmbrales")
            if (nuevosUmbrales != null) {
                listaDeModos[2] = listaDeModos[2].copy(umbrales = nuevosUmbrales)
                adaptadorModos.notifyItemChanged(2)
            }
        }

        vpModos?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                modoActivoActual = listaDeModos[position]
            }
        })

        btnNext?.setOnClickListener { if (vpModos != null && vpModos.currentItem < listaDeModos.size - 1) vpModos.currentItem += 1 }
        btnPrev?.setOnClickListener { if (vpModos != null && vpModos.currentItem > 0) vpModos.currentItem -= 1 }

        cargarHistorialCriticoDesdeBD()

        val btnVerHistorial = view.findViewById<ImageButton>(R.id.btnVerHistorial)
        btnVerHistorial?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, HistorialFragment()).addToBackStack(null).commit()
        }

        val btnSimular = view.findViewById<Button>(R.id.btnSimularRegistro)
        btnSimular?.setOnClickListener {
            val distanciaSimulada = kotlin.math.round(kotlin.random.Random.nextDouble(0.5, 2.5) * 100) / 100.0
            guardarEventoCritico(distanciaSimulada)
        }

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_usuario -> { parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, UsuarioFragment()).addToBackStack(null).commit(); true }
                R.id.nav_ajustes -> { parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, AjustesFragment()).addToBackStack(null).commit(); true }
                R.id.nav_sos -> { parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, SosFragment()).addToBackStack(null).commit(); true }
                else -> false
            }
        }

        actualizarEstadoNodoA(false)
        actualizarEstadoNodoB(false)
        cambiarEstadoConexionGlobal(false)
    }

    private fun conectarWifiESP32(ssidEsp32: String, passwordEsp32: String? = null) {
        val safeContext = context ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(safeContext, "La conexión WiFi automática requiere Android 10 o superior", Toast.LENGTH_LONG).show()
            return
        }
        conectarWifiESP32Api29(ssidEsp32, passwordEsp32)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun conectarWifiESP32Api29(ssidEsp32: String, passwordEsp32: String?) {
        val safeContext = context ?: return
        val connectivityManager = safeContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(ssidEsp32)
        if (!passwordEsp32.isNullOrEmpty()) {
            specifierBuilder.setWpa2Passphrase(passwordEsp32)
        }

        val specifier = specifierBuilder.build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                try {
                    connectivityManager.bindProcessToNetwork(network)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    cambiarEstadoConexionGlobal(true)
                    actualizarEstadoNodoA(true)
                    actualizarEstadoNodoB(true)
                    Toast.makeText(context, "Conectado al ESP32", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    Toast.makeText(context, "No se pudo encontrar la red del ESP32", Toast.LENGTH_LONG).show()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    desconectarWifiESP32()
                    mostrarBannerNotificacion("Se ha perdido la conexión.", "Revise los módulos ESP32")
                }
            }
        }

        connectivityManager.requestNetwork(request, networkCallback!!)
    }

    private fun desconectarWifiESP32() {
        val safeContext = context
        if (safeContext != null) {
            val connectivityManager = safeContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try {
                connectivityManager?.bindProcessToNetwork(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            networkCallback?.let {
                try {
                    connectivityManager?.unregisterNetworkCallback(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        networkCallback = null

        cambiarEstadoConexionGlobal(false)
        actualizarEstadoNodoA(false)
        actualizarEstadoNodoB(false)
    }

    private fun mostrarDialogoWifi() {
        val safeContext = context ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_wifi, null)
        val alertDialog = MaterialAlertDialogBuilder(safeContext)
            .setView(dialogView)
            .setBackground(ColorDrawable(Color.TRANSPARENT))
            .create()

        val btnConectar = dialogView.findViewById<Button>(R.id.btnConectarWifi)
        btnConectar.setOnClickListener {
            Toast.makeText(safeContext, "Buscando red ESP32...", Toast.LENGTH_SHORT).show()

            conectarWifiESP32("Cycla_ESP32")

            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun mostrarBannerNotificacion(titulo: String, subtitulo: String) {
        val banner = view?.findViewById<MaterialCardView>(R.id.cardNotificationBanner)
        val tvTitulo = view?.findViewById<TextView>(R.id.tvNotifTitulo)
        val tvSubtitulo = view?.findViewById<TextView>(R.id.tvNotifSubtitulo)

        tvTitulo?.text = titulo
        tvSubtitulo?.text = subtitulo

        if (subtitulo.isEmpty()) {
            tvSubtitulo?.visibility = View.GONE
        } else {
            tvSubtitulo?.visibility = View.VISIBLE
        }

        banner?.visibility = View.VISIBLE

        banner?.postDelayed({
            banner.visibility = View.GONE
        }, 6000)
    }

    private fun enviarSmsEmergencia() {
        val uid = auth.currentUser?.uid
        val safeContext = context ?: return

        if (uid == null) {
            Toast.makeText(safeContext, "Inicia sesión para enviar SMS de emergencia", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (context == null) return@addOnSuccessListener
                    var linkMapa = "Ubicación GPS no disponible en este momento."
                    if (location != null) {
                        linkMapa = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    }

                    db.collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
                        if (context == null) return@addOnSuccessListener
                        val nombre = doc.getString("nombre") ?: "Un usuario de Cycla"
                        val mensajeSms = "¡EMERGENCIA! $nombre requiere asistencia inmediata.\nUbicación: $linkMapa"

                        db.collection("usuarios").document(uid).collection("contactos").get()
                            .addOnSuccessListener { snapshots ->
                                val finalContext = context ?: return@addOnSuccessListener
                                if (snapshots.isEmpty) {
                                    Toast.makeText(finalContext, "No hay contactos registrados.", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                val smsManager = finalContext.getSystemService(SmsManager::class.java)
                                for (contacto in snapshots) {
                                    val telefono = contacto.getString("telefono") ?: continue
                                    try {
                                        smsManager?.sendTextMessage(telefono, null, mensajeSms, null, null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val sdfFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                val date = Date()
                                val fechaHora = "Ubicación, ${sdfHora.format(date)}, ${sdfFecha.format(date)}"
                                mostrarBannerNotificacion("Contactos de emergencia solicitados.", fechaHora)
                            }
                    }
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(safeContext, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarHistorialCriticoDesdeBD() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            tvUltimosEventos?.text = "- Sin eventos (no autenticado)."
            return
        }

        historialListener?.remove()
        historialListener = db.collection("usuarios").document(uid).collection("historial_critico")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) {
                    tvUltimosEventos?.text = "Error al cargar historial."
                    return@addSnapshotListener
                }

                val sbCompleto = java.lang.StringBuilder()

                for (doc in snapshots) {
                    val modo = doc.getString("modo") ?: "Desconocido"
                    val distancia = doc.getDouble("distancia") ?: 0.0
                    val hora = doc.getString("hora") ?: ""
                    val fecha = doc.getString("fecha") ?: ""

                    sbCompleto.append("- $modo, $distancia m, $hora, $fecha\n")
                }

                if (sbCompleto.isEmpty()) {
                    tvUltimosEventos?.text = "- Sin eventos registrados."
                } else {
                    tvUltimosEventos?.text = sbCompleto.toString().trim()
                }
            }
    }

    private fun guardarEventoCritico(distancia: Double) {
        val uid = auth.currentUser?.uid
        val safeContext = context

        if (uid == null) {
            if (safeContext != null) {
                Toast.makeText(safeContext, "Inicia sesión para registrar eventos", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val nombreModo = modoActivoActual?.titulo?.replace("MODO\n", "")?.replace("MODO ", "") ?: "Urbano"

        val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date = Date()

        val horaStr = sdfHora.format(date)
        val fechaStr = sdfFecha.format(date)

        val eventoMap = hashMapOf(
            "modo" to nombreModo,
            "distancia" to distancia,
            "hora" to horaStr,
            "fecha" to fechaStr,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(uid).collection("historial_critico").add(eventoMap).addOnSuccessListener {
            val subTexto = "$nombreModo, $distancia m, $horaStr, $fechaStr"
            mostrarBannerNotificacion("Alerta de Proximidad Crítica", subTexto)
        }
    }

    private fun actualizarEstadoNodoA(conectado: Boolean) {
        val color = if (conectado) Color.GREEN else Color.RED
        dotNodoA?.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun actualizarEstadoNodoB(conectado: Boolean) {
        val color = if (conectado) Color.GREEN else Color.RED
        dotNodoB?.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun cambiarEstadoConexionGlobal(conectado: Boolean) {
        isWifiConnected = conectado
        if (conectado) imgWifiStatus?.setImageResource(R.drawable.ic_wifi_connected)
        else imgWifiStatus?.setImageResource(R.drawable.ic_wifi_disconnected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historialListener?.remove()
        historialListener = null
        desconectarWifiESP32()
        dotNodoA = null
        dotNodoB = null
        imgWifiStatus = null
        tvUltimosEventos = null
    }
}