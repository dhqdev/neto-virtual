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

- Etapa 1 (base): bolinha flutuante, leitura da tela via AccessibilityService,
  voz (TTS) e destaque amarelo. **Compila** (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24,
  JDK 17); o GitHub Actions (`.github/workflows/android.yml`) gera o APK de debug
  a cada push. **Aguardando o David testar no celular** antes da Etapa 2.
- Destaque amarelo: a janela ignora barra de status e notch
  (`setFitInsetsTypes(0)` + `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`) e, depois de
  aparecer, mede a própria posição e corrige qualquer diferença.
- Telas: apresentação em 3 páginas (`ApresentacaoActivity`), início
  (`MainActivity`) e ajustes (`AjustesActivity`: velocidade da voz e tamanho
  da bolinha, salvos em `Preferencias`). Visual em `res/values/themes.xml`.
- Prints automáticos: `.github/workflows/prints.yml` roda `scripts/tirar-prints.sh`
  num emulador e salva em `docs/prints/`. No APK de debug dá para abrir telas e
  "tocar" na bolinha pelo adb (ver comentários em `MainActivity` e no serviço).
- `local.properties` é de cada máquina (fica fora do git): `sdk.dir=<caminho do SDK>`.
- A skill `neto-virtual` ainda não está no repositório
  (`.claude/skills/neto-virtual/SKILL.md` não existe).

## Comandos

```bash
./gradlew assembleDebug               # compila
./gradlew installDebug                # instala no celular conectado (adb)
adb logcat -s NetoVirtual             # logs do app
```

## Regras que nunca mudam

1. O app nunca executa toques, digitação ou gestos pelo usuário.
2. Chave de API de IA nunca vai dentro do APK de produção (usar backend).
3. Nada da tela do usuário é guardado no servidor além do necessário para responder.
4. Linguagem sempre simples, calma, sem termos técnicos ("toque no botão verde", não "clique no FAB").
