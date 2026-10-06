package com.netovirtual.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda as escolhas da pessoa no próprio celular (SharedPreferences é
 * como o localStorage do navegador). A tela de Ajustes escreve aqui e a
 * bolinha lê daqui.
 */
object Preferencias {

    const val ARQUIVO = "neto_preferencias"
    const val CHAVE_VELOCIDADE = "velocidade_voz"
    const val CHAVE_BOLINHA_GRANDE = "bolinha_grande"
    private const val CHAVE_VIU_APRESENTACAO = "viu_apresentacao"

    enum class Velocidade(val valor: Float, val titulo: String, val descricao: String, val icone: String) {
        DEVAGAR(0.7f, "Devagar", "Bem calma, palavra por palavra", "🐢"),
        NORMAL(0.85f, "Normal", "Calma, como numa conversa", "🙂"),
        RAPIDA(1.0f, "Mais rápida", "Para quem já está acostumado", "🐇"),
    }

    fun abrir(contexto: Context): SharedPreferences =
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun velocidade(contexto: Context): Velocidade {
        val nome = abrir(contexto).getString(CHAVE_VELOCIDADE, Velocidade.NORMAL.name)
        return Velocidade.values().firstOrNull { it.name == nome } ?: Velocidade.NORMAL
    }

    fun salvarVelocidade(contexto: Context, v: Velocidade) =
        abrir(contexto).edit().putString(CHAVE_VELOCIDADE, v.name).apply()

    fun bolinhaGrande(contexto: Context): Boolean =
        abrir(contexto).getBoolean(CHAVE_BOLINHA_GRANDE, false)

    fun salvarBolinhaGrande(contexto: Context, grande: Boolean) =
        abrir(contexto).edit().putBoolean(CHAVE_BOLINHA_GRANDE, grande).apply()

    fun viuApresentacao(contexto: Context): Boolean =
        abrir(contexto).getBoolean(CHAVE_VIU_APRESENTACAO, false)

    fun marcarApresentacaoVista(contexto: Context) =
        abrir(contexto).edit().putBoolean(CHAVE_VIU_APRESENTACAO, true).apply()
}
