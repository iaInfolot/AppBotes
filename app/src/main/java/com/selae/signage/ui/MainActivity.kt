package com.selae.signage.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.selae.signage.R
import com.selae.signage.config.AppConfig
import com.selae.signage.model.GameJackpot
import com.selae.signage.network.NetworkClient
import kotlinx.coroutines.launch
import android.widget.TextView
import android.widget.FrameLayout
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

/**
 * MainActivity.kt
 *
 * Pantalla principal de la app de señalización.
 * Muestra todos los botes de SELAE en tarjetas horizontales a pantalla completa.
 * Se auto-refresca cada N minutos sin ninguna intervención del usuario.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SelaeMainActivity"
    }

    // ============================================================
    // VISTAS Y COMPONENTES
    // ============================================================

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var adapter: JackpotAdapter
    private lateinit var networkClient: NetworkClient

    // Handler para el refresco periódico
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Iniciando refresco automático...")
            fetchJackpots()
            // Programar el siguiente refresco
            refreshHandler.postDelayed(
                this,
                TimeUnit.MINUTES.toMillis(AppConfig.REFRESH_INTERVAL_MINUTES)
            )
        }
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar pantalla completa: sin barra de estado, sin barra de navegación
        setupFullscreenWindow()

        setContentView(R.layout.activity_main)

        // Inicializar componentes
        networkClient = NetworkClient(applicationContext)
        setupViews()
        setupRecyclerView()

        // Cargar caché inmediatamente para mostrar algo mientras se carga
        showCachedDataIfAvailable()

        // Primera carga de datos
        fetchJackpots()

        // Iniciar refresco automático
        startAutoRefresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el handler para evitar fugas de memoria
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Mantener modo inmersivo si se pierde el foco
        if (hasFocus) setupFullscreenWindow()
    }

    // ============================================================
    // CONFIGURACIÓN INICIAL
    // ============================================================

    /**
     * Configura la ventana en modo pantalla completa inmersivo.
     * Oculta la barra de estado y la barra de navegación.
     */
    private fun setupFullscreenWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    /**
     * Enlaza las vistas del layout.
     */
    private fun setupViews() {
        recyclerView   = findViewById(R.id.recyclerJackpots)
        tvLastUpdate   = findViewById(R.id.tvLastUpdate)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    /**
     * Configura el RecyclerView con un GridLayoutManager horizontal.
     * El número de columnas se ajusta dinámicamente según los datos.
     */
    private fun setupRecyclerView() {
        adapter = JackpotAdapter()
        recyclerView.adapter = adapter

        // El LayoutManager se actualiza en updateGridColumns() tras conocer el número de juegos
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        // Deshabilitar scroll: todo debe caber en pantalla
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    /**
     * Muestra datos cacheados inmediatamente al arrancar, si existen.
     */
    private fun showCachedDataIfAvailable() {
        val cached = networkClient.loadFromCache()
        if (!cached.isNullOrEmpty()) {
            updateGrid(cached, fromCache = true)
            val ts = networkClient.getLastUpdateTimestamp()
            tvLastUpdate.text = FormatUtils.formatLastUpdate(ts)
        }
    }

    // ============================================================
    // CARGA DE DATOS
    // ============================================================

    /**
     * Llama al webservice y actualiza la UI.
     * Usa corrutinas de ciclo de vida para evitar fugas.
     */
    private fun fetchJackpots() {
        lifecycleScope.launch {
            // Mostrar overlay de carga solo si no hay datos en pantalla
            if (adapter.itemCount == 0) showLoading(true)

            when (val result = networkClient.fetchNextJackpots()) {

                is NetworkClient.FetchResult.Success -> {
                    showLoading(false)
                    hideError()
                    updateGrid(result.games, fromCache = false)
                    tvLastUpdate.text = FormatUtils.formatLastUpdate(System.currentTimeMillis())
                    Log.d(TAG, "UI actualizada con ${result.games.size} juegos")
                }

                is NetworkClient.FetchResult.Error -> {
                    showLoading(false)
                    Log.w(TAG, "Error: ${result.message}")

                    if (!result.cachedGames.isNullOrEmpty()) {
                        // Hay caché: mostrar aviso pero mantener los datos
                        updateGrid(result.cachedGames, fromCache = true)
                        showError("Sin conexión — mostrando datos guardados")
                        val ts = networkClient.getLastUpdateTimestamp()
                        tvLastUpdate.text = FormatUtils.formatLastUpdate(ts)
                    } else {
                        // Sin caché: mostrar error prominente
                        showError("No hay conexión y no hay datos guardados.\nComprueba la red.")
                    }
                }
            }
        }
    }

    /**
     * Actualiza el RecyclerView con la lista de juegos y ajusta el número de columnas.
     */
    private fun updateGrid(games: List<GameJackpot>, fromCache: Boolean) {
        val columns = calculateOptimalColumns(games.size)
        (recyclerView.layoutManager as GridLayoutManager).spanCount = columns
        adapter.submitList(games)

        Log.d(TAG, "Grid: ${games.size} juegos en $columns columnas (caché: $fromCache)")
    }

    /**
     * Calcula el número óptimo de columnas para que todos los juegos
     * quepan en una sola fila sin scroll.
     *
     * Para un TV de 1920px con márgenes: entre 3 y 7 columnas.
     */
    private fun calculateOptimalColumns(gameCount: Int): Int {
        return when {
            gameCount <= 0 -> 1
            gameCount <= 3 -> 3   // Mínimo 3 para que las tarjetas no queden enormes
            gameCount <= 7 -> gameCount
            else -> 7             // Máximo 7; más tarjetas resultarían ilegibles en TV
        }
    }

    // ============================================================
    // REFRESCO AUTOMÁTICO
    // ============================================================

    private fun startAutoRefresh() {
        // El primer refresco ocurre tras el intervalo completo
        // (ya se ha hecho la primera carga manual arriba)
        refreshHandler.postDelayed(
            refreshRunnable,
            TimeUnit.MINUTES.toMillis(AppConfig.REFRESH_INTERVAL_MINUTES)
        )
        Log.d(TAG, "Auto-refresco programado cada ${AppConfig.REFRESH_INTERVAL_MINUTES} minutos")
    }

    // ============================================================
    // GESTIÓN DE UI: LOADING / ERROR
    // ============================================================

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE

        // Parpadeo suave para llamar la atención sin ser molesto
        val blink = AlphaAnimation(1.0f, 0.3f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = 3
        }
        tvErrorMessage.startAnimation(blink)
    }

    private fun hideError() {
        tvErrorMessage.clearAnimation()
        tvErrorMessage.visibility = View.GONE
    }
}
