package es.infolot.tv

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

/**
 * Pantalla de espera de activación.
 * Se muestra cuando el dispositivo no tiene credenciales almacenadas.
 * Hace polling al servidor cada 30 segundos hasta recibir las credenciales.
 */
class ActivationActivity : AppCompatActivity() {

    private lateinit var tvCode:   TextView
    private lateinit var tvStatus: TextView
    private lateinit var statusDot: View

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null
    private var dotAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.activity_activation)

        tvCode    = findViewById(R.id.tvActivationCode)
        tvStatus  = findViewById(R.id.tvStatus)
        statusDot = findViewById(R.id.statusDot)

        // Generar y mostrar el código de este dispositivo
        val code = ActivationManager.getActivationCode(this)
        // Mostrar con guión en el medio para facilitar la lectura: ABC-DEF
        tvCode.text = "${code.take(3)}-${code.drop(3)}"

        startPolling()
        startDotAnimation()
    }

    private fun startPolling(intervalMs: Long = AppConfig.POLLING_INTERVAL_MS) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                checkActivation()
            }
        }
    }

    private suspend fun checkActivation() {
        val code = ActivationManager.getActivationCode(this@ActivationActivity)

        val result = withContext(Dispatchers.IO) {
            ActivationManager.checkActivation(code)
        }

        when (result) {
            is ActivationManager.ActivationResult.Activated -> {
                // Guardar credenciales y lanzar la app
                EncryptedPrefs.saveCredentials(
                    this@ActivationActivity,
                    result.token,
                    result.pass,
                    result.appId
                )
                launchMainApp()
            }
            is ActivationManager.ActivationResult.Throttled -> {
                // Demasiadas peticiones: ampliar el intervalo a 5 minutos
                updateStatus("Reintentando en 5 minutos...")
                startPolling(AppConfig.POLLING_INTERVAL_THROTTLED_MS)
            }
            is ActivationManager.ActivationResult.Pending,
            is ActivationManager.ActivationResult.NotFound -> {
                updateStatus("Esperando activación...")
            }
            is ActivationManager.ActivationResult.Error -> {
                updateStatus("Sin conexión. Reintentando...")
            }
        }
    }

    private fun launchMainApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun updateStatus(text: String) {
        tvStatus.text = text
    }

    private fun startDotAnimation() {
        dotAnimator = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.1f).apply {
            duration    = 800
            repeatCount = ObjectAnimator.INFINITE
            repeatMode  = ObjectAnimator.REVERSE
            start()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        dotAnimator?.cancel()
        super.onDestroy()
    }

    // Bloquear botón atrás — modo kiosco
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* no-op */ }
}
