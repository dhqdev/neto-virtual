package com.netovirtual.app

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

/**
 * ETAPA 1 do protótipo.
 *
 * - Mostra uma bolinha verde flutuante por cima de qualquer app.
 * - Ao tocar nela, lê os botões/opções da tela atual.
 * - Fala em voz alta cada opção e desenha um círculo em volta dela.
 *
 * Ainda sem IA: isso só prova que conseguimos VER a tela, FALAR e DESTACAR.
 * Na Etapa 2 a IA decide qual é o próximo passo certo.
 */
class NetoAccessibilityService : AccessibilityService() {

    /** Um item clicável encontrado na tela. */
    data class ItemTela(val rotulo: String, val area: Rect)

    private lateinit var windowManager: WindowManager
    private val main = Handler(Looper.getMainLooper())

    private var bolinha: View? = null
    private var destaque: View? = null

    private var tts: TextToSpeech? = null
    private var ttsPronto = false
    private var itensFalados: List<ItemTela> = emptyList()

    // ---------------------------------------------------------------- ciclo de vida

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
                tts?.setSpeechRate(0.85f) // um pouco mais devagar, pra ficar claro
                tts?.setOnUtteranceProgressListener(ouvinteDaFala)
                ttsPronto = true
            } else {
                Log.w(TAG, "A voz (TTS) não iniciou. Status: $status")
            }
        }

        mostrarBolinha()
        Log.i(TAG, "Serviço ligado. Bolinha na tela.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Etapa 1: não reagimos a eventos automaticamente.
        // Na Etapa 2 vamos usar isso para perceber quando a tela mudou.
    }

    override fun onInterrupt() {
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.shutdown()
        bolinha?.let { windowManager.removeView(it) }
        removerDestaque()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- bolinha flutuante

    @SuppressLint("ClickableViewAccessibility")
    private fun mostrarBolinha() {
        val tamanho = dp(72)

        val view = TextView(this).apply {
            text = "🙂"
            textSize = 32f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2E7D32"))
                setStroke(dp(4), Color.WHITE)
            }
            contentDescription = "Neto Virtual. Toque para pedir ajuda."
        }

        val params = WindowManager.LayoutParams(
            tamanho, tamanho,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(300)
        }

        // Arrastar para mudar de lugar, ou tocar para pedir ajuda.
        var inicioX = 0
        var inicioY = 0
        var toqueX = 0f
        var toqueY = 0f
        var arrastou = false

        view.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    inicioX = params.x; inicioY = params.y
                    toqueX = e.rawX; toqueY = e.rawY
                    arrastou = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - toqueX).toInt()
                    val dy = (e.rawY - toqueY).toInt()
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) arrastou = true
                    if (arrastou) {
                        params.x = inicioX + dx
                        params.y = inicioY + dy
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!arrastou) aoTocarNaBolinha()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        bolinha = view
    }

    private fun aoTocarNaBolinha() {
        if (!ttsPronto) return

        // Se estiver falando, um toque faz parar.
        if (tts?.isSpeaking == true) {
            tts?.stop()
            removerDestaque()
            return
        }

        val itens = lerItensDaTela()
        itensFalados = itens
        Log.i(TAG, "Encontrei ${itens.size} opções: " + itens.take(5).joinToString { "${it.rotulo} ${it.area.toShortString()}" })

        if (itens.isEmpty()) {
            falar("Não consegui ver nenhum botão nesta tela.", "fim")
            return
        }

        val mostrar = itens.take(5)
        falar("Nesta tela eu vejo ${itens.size} opções. Vou te mostrar algumas.", "intro")
        mostrar.forEachIndexed { i, item -> falar(item.rotulo, "item_$i") }
        falar("Toque em mim de novo quando precisar.", "fim")
    }

    // ---------------------------------------------------------------- leitura da tela

    private fun lerItensDaTela(): List<ItemTela> {
        val raiz = rootInActiveWindow ?: return emptyList()
        val encontrados = mutableListOf<ItemTela>()
        percorrer(raiz, encontrados)
        // Remove repetidos e ordena de cima para baixo, da esquerda para a direita.
        return encontrados
            .distinctBy { it.rotulo }
            .sortedWith(compareBy({ it.area.top }, { it.area.left }))
    }

    private fun percorrer(no: AccessibilityNodeInfo, saida: MutableList<ItemTela>) {
        if (no.packageName == packageName) return // ignora a nossa própria bolinha
        if (!no.isVisibleToUser) return

        if (no.isClickable) {
            val rotulo = rotuloDe(no)
            val area = Rect().also { no.getBoundsInScreen(it) }
            if (!rotulo.isNullOrBlank() && area.width() > 0 && area.height() > 0) {
                saida.add(ItemTela(rotulo.trim(), area))
            }
        }

        for (i in 0 until no.childCount) {
            no.getChild(i)?.let { percorrer(it, saida) }
        }
    }

    /** Nome do botão: texto, descrição, ou o primeiro texto dentro dele. */
    private fun rotuloDe(no: AccessibilityNodeInfo, profundidade: Int = 0): String? {
        no.text?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        no.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        if (profundidade > 3) return null
        for (i in 0 until no.childCount) {
            val filho = no.getChild(i) ?: continue
            rotuloDe(filho, profundidade + 1)?.let { return it }
        }
        return null
    }

    // ---------------------------------------------------------------- voz

    private fun falar(texto: String, id: String) {
        tts?.speak(texto, TextToSpeech.QUEUE_ADD, null, id)
    }

    /** Quando a voz começa a falar um item, o círculo vai para ele. */
    private val ouvinteDaFala = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val id = utteranceId ?: return
            main.post {
                if (id.startsWith("item_")) {
                    val indice = id.removePrefix("item_").toIntOrNull() ?: return@post
                    itensFalados.getOrNull(indice)?.let { destacar(it.area) }
                } else {
                    removerDestaque()
                }
            }
        }

        override fun onDone(utteranceId: String?) {
            if (utteranceId == "fim") main.post { removerDestaque() }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post { removerDestaque() }
        }
    }

    // ---------------------------------------------------------------- círculo de destaque

    private fun destacar(area: Rect) {
        removerDestaque()
        val margem = dp(6)

        val view = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(dp(5), Color.parseColor("#FFC107"))
            }
            // Pisca suavemente para chamar atenção.
            animate().alpha(0.3f).setDuration(500).withEndAction {
                animate().alpha(1f).setDuration(500).start()
            }.start()
        }

        val params = WindowManager.LayoutParams(
            area.width() + margem * 2,
            area.height() + margem * 2,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // LEFT (e não START): as coordenadas da tela são sempre a partir da esquerda.
            gravity = Gravity.TOP or Gravity.LEFT
            x = area.left - margem
            y = area.top - margem
            // Deixa o destaque usar a tela inteira, inclusive a área do notch
            // e por baixo da barra de status. Sem isso, o Android empurra a
            // janela para baixo e o destaque fica fora do lugar.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setFitInsetsTypes(0)
                setFitInsetsSides(0)
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        windowManager.addView(view, params)
        destaque = view

        // Conferência final: depois que o destaque aparece, medimos onde ele
        // ficou de verdade. Se algum aparelho deslocou a janela (barra de
        // status, notch, modo de tela dividida), corrigimos a diferença.
        view.post { corrigirPosicao(view, params, area.left - margem, area.top - margem) }
    }

    private fun corrigirPosicao(
        view: View,
        params: WindowManager.LayoutParams,
        xEsperado: Int,
        yEsperado: Int
    ) {
        if (destaque !== view || !view.isAttachedToWindow) return
        val ondeFicou = IntArray(2)
        view.getLocationOnScreen(ondeFicou)
        val dx = xEsperado - ondeFicou[0]
        val dy = yEsperado - ondeFicou[1]
        if (dx == 0 && dy == 0) return
        Log.d(TAG, "Destaque deslocado em ($dx, $dy) px. Corrigindo.")
        params.x += dx
        params.y += dy
        windowManager.updateViewLayout(view, params)
    }

    private fun removerDestaque() {
        destaque?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        destaque = null
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "NetoVirtual"
    }
}
