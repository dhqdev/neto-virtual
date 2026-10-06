package com.netovirtual.app

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
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
    private var piscar: ObjectAnimator? = null

    // Onde a bolinha estava (para não pular de lugar quando muda de tamanho).
    private var bolinhaX = -1
    private var bolinhaY = -1

    private var tts: TextToSpeech? = null
    private var ttsPronto = false
    private var itensFalados: List<ItemTela> = emptyList()

    // ---------------------------------------------------------------- ciclo de vida

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val idioma = tts?.setLanguage(Locale("pt", "BR"))
                Log.i(TAG, "Voz pronta. Português do Brasil: " +
                    if (idioma != null && idioma >= TextToSpeech.LANG_AVAILABLE) "sim" else "não instalado ($idioma)")
                tts?.setSpeechRate(Preferencias.velocidade(this).valor)
                tts?.setOnUtteranceProgressListener(ouvinteDaFala)
                ttsPronto = true
            } else {
                Log.w(TAG, "A voz (TTS) não iniciou. Status: $status")
            }
        }

        mostrarBolinha()
        Preferencias.abrir(this).registerOnSharedPreferenceChangeListener(ouvinteAjustes)
        registrarAtalhoDeTeste()
        Log.i(TAG, "Serviço ligado. Bolinha na tela.")
    }

    /** Quando a pessoa muda algo em Ajustes, a bolinha se atualiza na hora. */
    private val ouvinteAjustes = SharedPreferences.OnSharedPreferenceChangeListener { _, chave ->
        if (chave == Preferencias.CHAVE_BOLINHA_GRANDE) {
            bolinha?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
            bolinha = null
            mostrarBolinha()
        }
    }

    /**
     * Só no APK de teste (debug): permite "tocar na bolinha" por um comando,
     * sem precisar do dedo. Útil para testes automáticos e prints:
     *   adb shell am broadcast -a com.netovirtual.app.PEDIR_AJUDA -p com.netovirtual.app
     */
    private var atalhoDeTeste: BroadcastReceiver? = null

    private fun registrarAtalhoDeTeste() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val receptor = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { aoTocarNaBolinha() }
        }
        val filtro = IntentFilter(ACAO_PEDIR_AJUDA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptor, filtro, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receptor, filtro)
        }
        atalhoDeTeste = receptor
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
        Preferencias.abrir(this).unregisterOnSharedPreferenceChangeListener(ouvinteAjustes)
        atalhoDeTeste?.let { unregisterReceiver(it) }
        bolinha?.let { windowManager.removeView(it) }
        removerDestaque()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- bolinha flutuante

    @SuppressLint("ClickableViewAccessibility")
    private fun mostrarBolinha() {
        val grande = Preferencias.bolinhaGrande(this)
        val tamanho = dp(if (grande) 92 else 72)

        val view = TextView(this).apply {
            text = "🙂"
            textSize = if (grande) 42f else 32f
            gravity = Gravity.CENTER
            background = getDrawable(R.drawable.bg_bolinha)
            contentDescription = "Neto Virtual. Toque para pedir ajuda."
        }

        val params = WindowManager.LayoutParams(
            tamanho, tamanho,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Começa no canto direito, mais para baixo: perto do polegar
            // e longe dos títulos das telas.
            val tela = resources.displayMetrics
            x = if (bolinhaX >= 0) bolinhaX else tela.widthPixels - tamanho - dp(12)
            y = if (bolinhaY >= 0) bolinhaY else (tela.heightPixels * 0.62).toInt()
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
                        bolinhaX = params.x
                        bolinhaY = params.y
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
        // Se já estiver mostrando algo, um toque faz parar.
        if (tts?.isSpeaking == true || mostrandoSemVoz) {
            pararTudo()
            return
        }

        val itens = lerItensDaTela()
        itensFalados = itens
        Log.i(TAG, "Encontrei ${itens.size} opções: " + itens.take(5).joinToString { "${it.rotulo} ${it.area.toShortString()}" })

        if (!ttsPronto) {
            // Sem voz no aparelho: pelo menos mostramos os botões, um de cada vez.
            Log.w(TAG, "Voz indisponível. Mostrando só o destaque.")
            mostrarSemVoz(itens.take(5))
            return
        }

        if (itens.isEmpty()) {
            falar("Não consegui ver nenhum botão nesta tela.", "fim")
            return
        }

        tts?.setSpeechRate(Preferencias.velocidade(this).valor)
        val mostrar = itens.take(5)
        falar("Nesta tela eu vejo ${itens.size} opções. Vou te mostrar algumas.", "intro")
        mostrar.forEachIndexed { i, item -> falar(item.rotulo, "item_$i") }
        falar("Toque em mim de novo quando precisar.", "fim")

        // Se a voz não começar logo (ex.: voz em português ainda sendo baixada),
        // mostramos os botões mesmo assim, para a pessoa não ficar sem resposta.
        falaComecou = false
        main.postDelayed({
            if (!falaComecou) {
                Log.w(TAG, "A voz não começou. Mostrando só o destaque.")
                tts?.stop()
                mostrarSemVoz(mostrar)
            }
        }, 4000)
    }

    @Volatile private var falaComecou = false

    private var mostrandoSemVoz = false

    private fun mostrarSemVoz(itens: List<ItemTela>) {
        if (itens.isEmpty()) return
        mostrandoSemVoz = true
        itens.forEachIndexed { i, item ->
            main.postDelayed({ destacar(item.area) }, i * 2500L)
        }
        main.postDelayed({ pararTudo() }, itens.size * 2500L)
    }

    private fun pararTudo() {
        tts?.stop()
        main.removeCallbacksAndMessages(null)
        mostrandoSemVoz = false
        removerDestaque()
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
            falaComecou = true
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
            Log.w(TAG, "Erro na voz ao falar: $utteranceId")
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
                setColor(Color.parseColor("#33FFC107")) // amarelo bem clarinho por dentro
                setStroke(dp(6), Color.parseColor("#FFC107"))
            }
        }
        // Pisca suavemente enquanto estiver na tela, para chamar atenção.
        // Se a pessoa desligou as animações do celular (comum em quem tem
        // tontura ou vista cansada), o destaque fica parado e bem forte.
        if (ValueAnimator.areAnimatorsEnabled()) piscar = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.55f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
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
        piscar?.cancel()
        piscar = null
        destaque?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        destaque = null
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "NetoVirtual"
        const val ACAO_PEDIR_AJUDA = "com.netovirtual.app.PEDIR_AJUDA"
    }
}
