# Neto Virtual

App Android que ensina idosos a usar o celular. A pessoa toca numa bolinha
flutuante, fala o que quer fazer ("quero mandar foto pro meu filho") e o app
guia por voz, passo a passo, desenhando um destaque em volta de onde tocar.
Quem toca é sempre o idoso: o app **nunca** toca sozinho.

Quem paga é o filho/familiar (assinatura). Público: idosos brasileiros com Android.

## Antes de qualquer coisa

Carregue a skill `neto-virtual` (`.claude/skills/neto-virtual/SKILL.md`).
Ela tem o plano de etapas, a arquitetura e as regras do produto.

## Como trabalhar com o David

- Ele quer ser **guiado passo a passo** e que você construa direto no código.
- Faça **uma etapa por vez**. Ao terminar uma etapa: compile, explique em
  português simples o que mudou, diga como testar no celular e **pare**,
  esperando ele confirmar que funcionou antes de começar a próxima.
- Responda sempre em **português do Brasil**.
- Ele domina React, n8n, Evolution API (WhatsApp) e Python. Kotlin/Android é
  novo pra ele: explique o "porquê" das partes nativas.
- Infra dele: VM Oracle Cloud com Portainer + Docker Swarm + Traefik,
  n8n em https://auto.tekvosoft.com.

## Estado atual

- Etapa 1 (base) escrita: bolinha flutuante, leitura da tela via
  AccessibilityService, voz (TTS) e destaque amarelo. **Ainda não foi compilada.**
  Primeira tarefa: gerar o Gradle wrapper, compilar e corrigir o que precisar.

## Comandos

```bash
gradle wrapper --gradle-version 8.7   # só na primeira vez, se não houver ./gradlew
./gradlew assembleDebug               # compila
./gradlew installDebug                # instala no celular conectado (adb)
adb logcat -s NetoVirtual             # logs do app
```

## Regras que nunca mudam

1. O app nunca executa toques, digitação ou gestos pelo usuário.
2. Chave de API de IA nunca vai dentro do APK de produção (usar backend).
3. Nada da tela do usuário é guardado no servidor além do necessário para responder.
4. Linguagem sempre simples, calma, sem termos técnicos ("toque no botão verde", não "clique no FAB").
