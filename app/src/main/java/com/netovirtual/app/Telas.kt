package com.netovirtual.app

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Peças que várias telas usam. */
object Telas {

    /** Adiciona uma linha "(1) texto  emoji" dentro de [lista]. */
    fun adicionarPasso(lista: LinearLayout, numero: Int, texto: String, emoji: String = "") {
        val linha = LayoutInflater.from(lista.context).inflate(R.layout.item_passo, lista, false)
        linha.findViewById<TextView>(R.id.numero).text = numero.toString()
        linha.findViewById<TextView>(R.id.texto).text = texto
        linha.findViewById<TextView>(R.id.emoji).text = emoji
        lista.addView(linha)
    }

    /** Cria uma opção tocável (ícone, título, explicação e ✔ quando escolhida). */
    fun criarOpcao(
        lista: LinearLayout,
        icone: String,
        titulo: String,
        subtitulo: String,
        aoTocar: () -> Unit
    ): View {
        val opcao = LayoutInflater.from(lista.context).inflate(R.layout.item_opcao, lista, false)
        opcao.findViewById<TextView>(R.id.icone).text = icone
        opcao.findViewById<TextView>(R.id.titulo).text = titulo
        opcao.findViewById<TextView>(R.id.subtitulo).apply {
            text = subtitulo
            visibility = if (subtitulo.isBlank()) View.GONE else View.VISIBLE
        }
        opcao.setOnClickListener { aoTocar() }
        lista.addView(opcao)
        return opcao
    }

    /** Marca qual opção está escolhida (fundo verde e ✔). */
    fun marcar(opcoes: List<View>, escolhida: Int) {
        opcoes.forEachIndexed { i, v ->
            v.isSelected = i == escolhida
            v.findViewById<TextView>(R.id.marcado).visibility =
                if (i == escolhida) View.VISIBLE else View.INVISIBLE
        }
    }

    /** O Neto está ligado nas configurações de Acessibilidade? */
    fun servicoAtivo(contexto: Context): Boolean {
        val esperado = ComponentName(contexto, NetoAccessibilityService::class.java)
        val ativos = Settings.Secure.getString(
            contexto.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val divisor = TextUtils.SimpleStringSplitter(':')
        divisor.setString(ativos)
        return divisor.any { ComponentName.unflattenFromString(it) == esperado }
    }

    /** Uma voz simples para as telas do app (teste de voz e exemplo). */
    class Voz(contexto: Context) {
        private val app = contexto.applicationContext
        private var pronta = false
        private var pendente: String? = null
        private val tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                pronta = true
                pendente?.let { falar(it); pendente = null }
            }
        }

        fun falar(texto: String) {
            if (!pronta) { pendente = texto; return }
            tts.language = Locale("pt", "BR")
            tts.setSpeechRate(Preferencias.velocidade(app).valor)
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "teste")
        }

        fun desligar() {
            tts.stop()
            tts.shutdown()
        }
    }
}
