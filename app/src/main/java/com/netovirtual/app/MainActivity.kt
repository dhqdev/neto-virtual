package com.netovirtual.app

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

/**
 * Tela de início. Na primeira vez, manda para a apresentação.
 * Depois, mostra se o Neto está ligado e os atalhos principais.
 */
class MainActivity : Activity() {

    private var voz: Telas.Voz? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (abrirTelaDeTeste()) return

        if (!Preferencias.viuApresentacao(this)) {
            startActivity(Intent(this, ApresentacaoActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val passos = findViewById<LinearLayout>(R.id.passos_ajuda)
        Telas.adicionarPasso(passos, 1, "Toque na bolinha verde", "🙂")
        Telas.adicionarPasso(passos, 2, "Ouça o que eu falo", "🔊")
        Telas.adicionarPasso(passos, 3, "Toque no quadrado amarelo", "👆")

        findViewById<Button>(R.id.botao_ativar).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.botao_testar_voz).setOnClickListener {
            voz().falar("Oi! Eu sou o Neto Virtual. Quando precisar, é só tocar na bolinha verde.")
        }
        findViewById<Button>(R.id.botao_ajustes).setOnClickListener {
            startActivity(Intent(this, AjustesActivity::class.java))
        }
        findViewById<Button>(R.id.botao_rever).setOnClickListener {
            startActivity(Intent(this, ApresentacaoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) return
        findViewById<TextView>(R.id.saudacao).text = saudacao()

        val ativo = Telas.servicoAtivo(this)
        findViewById<View>(R.id.cartao_status).setBackgroundResource(
            if (ativo) R.drawable.bg_cartao_ok else R.drawable.bg_cartao_aviso
        )
        findViewById<TextView>(R.id.status_icone).text = if (ativo) "✅" else "⚠️"
        findViewById<TextView>(R.id.status_titulo).text =
            if (ativo) "Estou ligado e pronto para ajudar" else "Estou desligado"
        findViewById<TextView>(R.id.status_texto).text =
            if (ativo) "Toque na bolinha verde em qualquer tela."
            else "Para eu te ajudar, toque no botão abaixo e me ligue nas configurações."
        findViewById<View>(R.id.botao_ativar).visibility = if (ativo) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        voz?.desligar()
        super.onDestroy()
    }

    /**
     * Só no APK de teste (debug): abre uma tela direto, para tirar prints.
     *   adb shell am start -n com.netovirtual.app/.MainActivity --es abrir ajustes
     * Valores: apresentacao (com --ei pagina 0, 1 ou 2), ajustes, inicio.
     */
    private fun abrirTelaDeTeste(): Boolean {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return false
        when (intent.getStringExtra("abrir")) {
            "apresentacao" -> startActivity(
                Intent(this, ApresentacaoActivity::class.java)
                    .putExtra(ApresentacaoActivity.EXTRA_PAGINA, intent.getIntExtra("pagina", 0))
            )
            "ajustes" -> startActivity(Intent(this, AjustesActivity::class.java))
            "inicio" -> { Preferencias.marcarApresentacaoVista(this); return false }
            else -> return false
        }
        finish()
        return true
    }

    private fun voz(): Telas.Voz = voz ?: Telas.Voz(this).also { voz = it }

    private fun saudacao(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Bom dia!"
        in 12..17 -> "Boa tarde!"
        else -> "Boa noite!"
    }
}
