package com.netovirtual.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

/**
 * Ajustes simples: velocidade da voz e tamanho da bolinha.
 * As escolhas valem na hora: a bolinha "escuta" as mudanças.
 */
class AjustesActivity : Activity() {

    private var voz: Telas.Voz? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        findViewById<Button>(R.id.botao_voltar).setOnClickListener { finish() }

        // Velocidade da voz
        val listaVelocidade = findViewById<LinearLayout>(R.id.opcoes_velocidade)
        val velocidades = Preferencias.Velocidade.values().toList()
        val opcoesVelocidade = mutableListOf<View>()
        velocidades.forEachIndexed { i, v ->
            opcoesVelocidade += Telas.criarOpcao(listaVelocidade, v.icone, v.titulo, v.descricao) {
                Preferencias.salvarVelocidade(this, v)
                Telas.marcar(opcoesVelocidade, i)
                falarExemplo()
            }
        }
        Telas.marcar(opcoesVelocidade, velocidades.indexOf(Preferencias.velocidade(this)))

        findViewById<Button>(R.id.botao_exemplo).setOnClickListener { falarExemplo() }

        // Tamanho da bolinha (lado a lado)
        val listaTamanho = findViewById<LinearLayout>(R.id.opcoes_tamanho)
        val opcoesTamanho = mutableListOf<View>()
        listOf("Normal" to false, "Grande" to true).forEachIndexed { i, (nome, grande) ->
            val opcao = Telas.criarOpcao(listaTamanho, "🟢", nome, "") {
                Preferencias.salvarBolinhaGrande(this, grande)
                Telas.marcar(opcoesTamanho, i)
            }
            opcao.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = (10 * resources.displayMetrics.density).toInt()
                if (i == 0) marginEnd = (6 * resources.displayMetrics.density).toInt()
                else marginStart = (6 * resources.displayMetrics.density).toInt()
            }
            if (!grande) opcao.findViewById<android.widget.TextView>(R.id.icone).textSize = 22f
            opcoesTamanho += opcao
        }
        Telas.marcar(opcoesTamanho, if (Preferencias.bolinhaGrande(this)) 1 else 0)
    }

    override fun onDestroy() {
        voz?.desligar()
        super.onDestroy()
    }

    private fun falarExemplo() {
        val v = voz ?: Telas.Voz(this).also { voz = it }
        v.falar("Assim fica a minha voz. Toque no botão verde para abrir a galeria.")
    }
}
