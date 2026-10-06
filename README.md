# Neto Virtual

App Android que guia idosos por voz no celular.

## Como começar com o Claude Code

1. Descompacte esta pasta e abra um terminal dentro dela.
2. Rode `claude`.
3. Peça: **"Leia o CLAUDE.md, carregue a skill neto-virtual e comece a Etapa 1."**

O Claude Code vai compilar a base, corrigir o que precisar e te passar o
roteiro de teste no celular. Depois de cada etapa ele para e espera você
confirmar que funcionou.

## Pré-requisitos no computador

- Android Studio (traz o SDK do Android e o JDK 17)
- `adb` no PATH (vem com o Android Studio, em `platform-tools`)
- Celular Android com Depuração USB ligada

## Estrutura

```
CLAUDE.md                         contexto do projeto para o Claude Code
.claude/skills/neto-virtual/      plano de etapas, arquitetura e regras
app/src/main/java/.../            código Kotlin
```
