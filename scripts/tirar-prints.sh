#!/usr/bin/env bash
# Tira prints de todas as telas do Neto Virtual num celular virtual (emulador).
# Usado pelo GitHub Actions (.github/workflows/prints.yml), mas roda em
# qualquer computador com um emulador ou celular ligado no adb.
set -uo pipefail

APP=com.netovirtual.app
OUT=${1:-prints}
mkdir -p "$OUT"

esperar_boot() {
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
  sleep 5
}

# Celular em português (só funciona em emulador com root).
adb root >/dev/null 2>&1 && sleep 3 && esperar_boot
if [ "$(adb shell getprop persist.sys.locale | tr -d '\r')" != "pt-BR" ]; then
  adb shell setprop persist.sys.locale pt-BR
  adb shell setprop ctl.restart zygote
  sleep 10
  esperar_boot
fi

# Simula um notch (furo da câmera) para conferir o destaque amarelo.
adb shell cmd overlay enable com.android.internal.display.cutout.emulation.hole || true

# Tela sempre ligada e barra de status "limpa" (9:30, bateria cheia).
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c

print() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "print: $1"; }
abrir() { adb shell am start -W --activity-clear-task -n "$APP/.MainActivity" "$@" >/dev/null; }

abrir --es abrir apresentacao --ei pagina 0; print 01-boas-vindas
abrir --es abrir apresentacao --ei pagina 1; print 02-como-funciona
abrir --es abrir apresentacao --ei pagina 2; print 03-ativar
abrir --es abrir inicio;                     print 04-inicio-desligado

# Liga o Neto Virtual nas configurações de Acessibilidade.
adb shell settings put secure enabled_accessibility_services "$APP/$APP.NetoAccessibilityService"
adb shell settings put secure accessibility_enabled 1
sleep 5

abrir --es abrir apresentacao --ei pagina 2; print 05-ativado
abrir --es abrir inicio;                     print 06-inicio-ligado
abrir --es abrir ajustes;                    print 07-ajustes

# A bolinha por cima de outro app (Configurações do celular).
adb shell am start -W -a android.settings.SETTINGS >/dev/null
print 08-bolinha-em-outro-app 4

# "Toca" na bolinha e fotografa o destaque amarelo andando pelos botões.
adb shell am broadcast -a "$APP.PEDIR_AJUDA" -p "$APP" >/dev/null
for i in 1 2 3 4 5 6; do print "09-destaque-$i" 2; done

adb logcat -d -s NetoVirtual TextToSpeech AndroidRuntime > "$OUT/logcat.txt"
ls -la "$OUT"
