package com.netovirtual.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Tela inicial: explica o app e leva a pessoa até a configuração
 * de Acessibilidade, onde o Neto Virtual precisa ser ativado.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(Color.WHITE)
        }

        val titulo = TextView(this).apply {
            text = "Neto Virtual"
            textSize = 34f
            setTextColor(Color.parseColor("#1B5E20"))
            gravity = Gravity.CENTER
        }

        val explicacao = TextView(this).apply {
            text = "Eu te ajudo a usar o celular, explicando por voz onde tocar.\n\n" +
                "Para funcionar, ative o Neto Virtual nas configurações de Acessibilidade."
            textSize = 20f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 48)
        }

        val botao = Button(this).apply {
            text = "Ativar o Neto Virtual"
            textSize = 22f
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
        }

        layout.addView(titulo)
        layout.addView(explicacao)
        layout.addView(botao)
        layout.addView(status)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        if (servicoAtivo()) {
            status.text = "✅ Ativado! Toque na bolinha verde em qualquer tela."
            status.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            status.text = "Ainda não ativado."
            status.setTextColor(Color.parseColor("#C62828"))
        }
    }

    private fun servicoAtivo(): Boolean {
        val esperado = ComponentName(this, NetoAccessibilityService::class.java)
        val ativos = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(ativos)
        return splitter.any { ComponentName.unflattenFromString(it) == esperado }
    }
}
