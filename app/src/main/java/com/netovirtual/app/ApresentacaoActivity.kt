package com.netovirtual.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Apresentação em 3 páginas, mostrada na primeira vez:
 * 1. Boas-vindas  2. Como funciona  3. Ativar nas configurações.
 *
 * Por que uma tela só com 3 "páginas" em vez de 3 telas? Assim o botão
 * Voltar e os pontinhos ficam sempre no mesmo lugar, o que ajuda quem
 * tem pouca prática com celular.
 */
class ApresentacaoActivity : Activity() {

    private lateinit var paginas: List<View>
    private lateinit var pontos: LinearLayout
    private lateinit var botaoPrincipal: Button
    private lateinit var botaoSecundario: Button
    private var pagina = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apresentacao)

        paginas = listOf(
            findViewById(R.id.pagina_boas_vindas),
            findViewById(R.id.pagina_como_funciona),
            findViewById(R.id.pagina_ativar),
        )
        pontos = findViewById(R.id.pontos)
        botaoPrincipal = findViewById(R.id.botao_principal)
        botaoSecundario = findViewById(R.id.botao_secundario)

        val passosComo = findViewById<LinearLayout>(R.id.passos_como_funciona)
        Telas.adicionarPasso(passosComo, 1, "Toque na bolinha verde", "🙂")
        Telas.adicionarPasso(passosComo, 2, "Ouça com atenção: eu explico por voz", "🔊")
        Telas.adicionarPasso(passosComo, 3, "Toque onde aparecer o quadrado amarelo", "👆")

        val passosAtivar = findViewById<LinearLayout>(R.id.passos_ativar)
        Telas.adicionarPasso(passosAtivar, 1, "Toque no botão verde aqui embaixo")
        Telas.adicionarPasso(passosAtivar, 2, "Procure \"Neto Virtual\" na lista e toque nele")
        Telas.adicionarPasso(passosAtivar, 3, "Ligue a chave e toque em \"Permitir\"")
        Telas.adicionarPasso(passosAtivar, 4, "Volte para cá com o botão de voltar", "↩️")

        // Pontinhos que mostram em qual página estamos. O tamanho precisa ser
        // fixo: uma View "vazia" com WRAP_CONTENT ocupa todo o espaço livre.
        repeat(paginas.size) {
            pontos.addView(View(this).apply {
                setBackgroundResource(R.drawable.bg_ponto)
                layoutParams = LinearLayout.LayoutParams(12.dp, 12.dp)
                    .apply { marginStart = 6.dp; marginEnd = 6.dp }
            })
        }

        botaoPrincipal.setOnClickListener { aoTocarPrincipal() }
        botaoSecundario.setOnClickListener { mostrar(pagina - 1) }

        mostrar(intent.getIntExtra(EXTRA_PAGINA, savedInstanceState?.getInt(EXTRA_PAGINA) ?: 0))
    }

    override fun onResume() {
        super.onResume()
        // Ao voltar das configurações, a página 3 já mostra "Tudo pronto!".
        if (pagina == paginas.lastIndex) mostrar(pagina)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_PAGINA, pagina)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pagina > 0) mostrar(pagina - 1) else super.onBackPressed()
    }

    private fun mostrar(nova: Int) {
        pagina = nova.coerceIn(0, paginas.lastIndex)
        paginas.forEachIndexed { i, v -> v.visibility = if (i == pagina) View.VISIBLE else View.GONE }
        for (i in 0 until pontos.childCount) {
            val ponto = pontos.getChildAt(i)
            ponto.isSelected = i == pagina
            ponto.layoutParams = ponto.layoutParams.apply { width = if (i == pagina) 28.dp else 12.dp }
        }
        botaoSecundario.visibility = if (pagina > 0) View.VISIBLE else View.GONE

        botaoPrincipal.text = when (pagina) {
            0 -> "Começar"
            1 -> "Entendi"
            else -> if (Telas.servicoAtivo(this)) "Vamos lá!" else "Abrir configurações"
        }

        if (pagina == paginas.lastIndex) {
            val ativo = Telas.servicoAtivo(this)
            findViewById<View>(R.id.passos_ativar).visibility = if (ativo) View.GONE else View.VISIBLE
            findViewById<View>(R.id.ativar_texto).visibility = if (ativo) View.GONE else View.VISIBLE
            findViewById<View>(R.id.ativar_pronto).visibility = if (ativo) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.ativar_titulo).text =
                if (ativo) "Pronto para usar" else "Falta só um passo"
        }
    }

    private fun aoTocarPrincipal() {
        when {
            pagina < paginas.lastIndex -> mostrar(pagina + 1)
            Telas.servicoAtivo(this) -> {
                Preferencias.marcarApresentacaoVista(this)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            else -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_PAGINA = "pagina"
    }
}
