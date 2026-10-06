#!/usr/bin/env bash
# Deep, one-shot provisioning: turn a stock Android / GrapheneOS phone into the
# MetroShell (Windows Phone 8.1) experience.
#
# This applies every adb-level grant the shell needs (default home + keyboard,
# draw-over-other-apps, accessibility, notification listeners, secure-settings,
# runtime permissions, battery exemption, full-screen intent) and, by default,
# hides Android's real status bar via `policy_control immersive.status=*` so the
# Metro tray becomes the status chrome. Gesture navigation is left untouched.
#
# Usage:
#   ./scripts/provision.sh                 # provision (immersive status bar)
#   ./scripts/provision.sh --no-immersive  # provision, keep Android status bar
#   ./scripts/provision.sh --status-nav    # also hide the navigation bar
#   ./scripts/provision.sh --restore       # undo (restore Android defaults)
#   ANDROID_SERIAL=XXXX ./scripts/provision.sh
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY="$ROOT/deploy"
STATE_FILE="$DEPLOY/provision-backup.env"
mkdir -p "$DEPLOY"

ADB_BIN="${ADB:-adb}"
if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  echo "ERROR: adb not found (set ADB=/path/to/adb)" >&2
  exit 1
fi
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB=("$ADB_BIN" -s "$ANDROID_SERIAL")
else
  ADB=("$ADB_BIN")
fi
if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
  echo "ERROR: no device connected" >&2
  exit 1
fi

sh() { "${ADB[@]}" shell "$@"; }
ok()   { echo "  OK   $*"; }
warn() { echo "  warn $*"; }

# --- args -------------------------------------------------------------------
IMMERSIVE="status"   # status | none | status-nav
RESTORE=0
for arg in "$@"; do
  case "$arg" in
    --no-immersive) IMMERSIVE="none" ;;
    --status-nav)   IMMERSIVE="status-nav" ;;
    --restore)      RESTORE=1 ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

# --- suite components -------------------------------------------------------
SHELL_APPS=(launcher statusbar navbar volume notifications lockscreen)
ACCESSIBILITY_SERVICES=(
  "com.metro.statusbar/com.metro.statusbar.StatusBarAccessibilityService"
  "com.metro.navbar/com.metro.navbar.NavbarAccessibilityService"
  "com.metro.volume/com.metro.volume.VolumeAccessibilityService"
  "com.metro.notifications/com.metro.notifications.NotificationsAccessibilityService"
  "com.metro.lockscreen/com.metro.lockscreen.LockscreenAccessibilityService"
)
NOTIFICATION_LISTENERS=(
  "com.metro.launcher/com.metro.launcher.TileNotificationListenerService"
  "com.metro.statusbar/com.metro.statusbar.StatusBarNotificationListenerService"
  "com.metro.volume/com.metro.volume.VolumeMediaNotificationListenerService"
  "com.metro.notifications/com.metro.notifications.ActionNotificationListenerService"
  "com.metro.lockscreen/com.metro.lockscreen.LockscreenNotificationListenerService"
)
IME_COMPONENT="com.metro.keyboard/dev.patrickgold.florisboard.FlorisImeService"
HOME_COMPONENT="com.metro.launcher/com.metro.launcher.MainActivity"

is_installed() { sh pm list packages | grep -q "package:$1"; }
installed_shell_java() { sh pm list packages -3 | grep -q "package:$1"; }

policy_control_value() {
  case "$IMMERSIVE" in
    status)      echo "immersive.status=*" ;;
    status-nav)  echo "immersive.status=*:immersive.navigation=*" ;;
    *)           echo "" ;;
  esac
}

# --- restore ----------------------------------------------------------------
if [[ "$RESTORE" -eq 1 ]]; then
  echo "==> restore: Android defaults"
  if [[ -f "$STATE_FILE" ]]; then
    # shellcheck disable=SC1090
    source "$STATE_FILE"
    if [[ -n "${PRIOR_POLICY_CONTROL:-}" && "$PRIOR_POLICY_CONTROL" != "null" ]]; then
      sh settings put global policy_control "$PRIOR_POLICY_CONTROL" >/dev/null 2>&1 && ok "policy_control restored"
    else
      sh settings delete global policy_control >/dev/null 2>&1 && ok "policy_control cleared"
    fi
    if [[ -n "${PRIOR_ACCESSIBILITY_SERVICES:-}" && "$PRIOR_ACCESSIBILITY_SERVICES" != "null" ]]; then
      sh settings put secure enabled_accessibility_services "$PRIOR_ACCESSIBILITY_SERVICES" >/dev/null 2>&1 && ok "accessibility services restored"
    else
      sh settings delete secure enabled_accessibility_services >/dev/null 2>&1 && ok "accessibility services cleared"
    fi
    sh settings put secure accessibility_enabled "${PRIOR_ACCESSIBILITY_ENABLED:-0}" >/dev/null 2>&1 && ok "accessibility_enabled restored"
  else
    sh settings delete global policy_control >/dev/null 2>&1 || true
    echo "  (no backup found — cleared policy_control only)"
  fi
  for comp in "${NOTIFICATION_LISTENERS[@]}"; do
    sh cmd notification disallow_listener "$comp" >/dev/null 2>&1 || true
  done
  ok "notification listeners removed"
  # Optional: leave appops/perms in place (harmless); the wizard can re-provision.
  echo "==> restore done. Reboot or re-run provision to re-enable the shell."
  exit 0
fi

# --- capture prior state ----------------------------------------------------
echo "==> capturing prior state -> deploy/provision-backup.env"
PRIOR_POLICY_CONTROL="$(sh settings get global policy_control | tr -d '\r')"
PRIOR_ACCESSIBILITY_SERVICES="$(sh settings get secure enabled_accessibility_services | tr -d '\r')"
PRIOR_ACCESSIBILITY_ENABLED="$(sh settings get secure accessibility_enabled | tr -d '\r')"
cat > "$STATE_FILE" <<EOF
PRIOR_POLICY_CONTROL="$PRIOR_POLICY_CONTROL"
PRIOR_ACCESSIBILITY_SERVICES="$PRIOR_ACCESSIBILITY_SERVICES"
PRIOR_ACCESSIBILITY_ENABLED="$PRIOR_ACCESSIBILITY_ENABLED"
EOF

# --- home + keyboard --------------------------------------------------------
echo "==> home + keyboard"
sh cmd package set-home-activity "$HOME_COMPONENT" >/dev/null 2>&1 && ok "home: launcher" || warn "set-home failed (is launcher installed?)"
if is_installed com.metro.keyboard; then
  sh ime enable "$IME_COMPONENT" >/dev/null 2>&1 && ok "ime enabled" || warn "ime enable failed"
  sh ime set "$IME_COMPONENT" >/dev/null 2>&1 && ok "ime set (default)" || warn "ime set failed"
else
  warn "keyboard not installed — skipping IME"
fi

# --- overlays + secure settings --------------------------------------------
echo "==> overlays"
for pkg in statusbar volume notifications lockscreen; do
  if is_installed "com.metro.$pkg"; then
    sh appops set "com.metro.$pkg" SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
      && ok "overlay: $pkg" || warn "overlay failed: $pkg"
  fi
done
sh pm grant com.metro.notifications android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 \
  && ok "WRITE_SECURE_SETTINGS: notifications" || warn "WRITE_SECURE_SETTINGS: notifications failed"
sh pm grant com.metro.settings android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 \
  && ok "WRITE_SECURE_SETTINGS: settings (immersive toggle)" || warn "WRITE_SECURE_SETTINGS: settings failed"

# --- accessibility ----------------------------------------------------------
echo "==> accessibility services"
CURRENT_ACC="$(sh settings get secure enabled_accessibility_services | tr -d '\r')"
[[ "$CURRENT_ACC" == "null" ]] && CURRENT_ACC=""
MERGED="$CURRENT_ACC"
for svc in "${ACCESSIBILITY_SERVICES[@]}"; do
  case ":$MERGED:" in *":$svc:"*) ;; *) MERGED="${MERGED:+$MERGED:}$svc" ;; esac
done
sh settings put secure enabled_accessibility_services "$MERGED" >/dev/null 2>&1 && ok "accessibility services set" || warn "accessibility set failed"
sh settings put secure accessibility_enabled 1 >/dev/null 2>&1 && ok "accessibility enabled" || warn "accessibility enable failed"

# --- notification listeners -------------------------------------------------
echo "==> notification listeners"
for comp in "${NOTIFICATION_LISTENERS[@]}"; do
  pkg="${comp%%/*}"
  if is_installed "$pkg"; then
    sh cmd notification allow_listener "$comp" >/dev/null 2>&1 && ok "listener: $pkg" || warn "listener failed: $pkg"
  fi
done

# --- runtime permissions ----------------------------------------------------
echo "==> runtime permissions"
grant() { sh pm grant "$1" "$2" >/dev/null 2>&1 && ok "perm: ${1##*.} $2" || true; }
grant com.metro.statusbar android.permission.READ_PHONE_STATE
grant com.metro.statusbar android.permission.POST_NOTIFICATIONS
grant com.metro.notifications android.permission.POST_NOTIFICATIONS
grant com.metro.lockscreen android.permission.POST_NOTIFICATIONS
grant com.metro.lockscreen android.permission.READ_CALENDAR
grant com.metro.lockscreen android.permission.READ_PHONE_STATE
grant com.metro.volume android.permission.MODIFY_AUDIO_SETTINGS
grant com.metro.launcher android.permission.POST_NOTIFICATIONS

# --- content apps -----------------------------------------------------------
echo "==> content app permissions"
CONTENT_PERMS=(
  "com.metro.photos android.permission.READ_MEDIA_IMAGES"
  "com.metro.photos android.permission.READ_MEDIA_VIDEO"
  "com.metro.photos android.permission.POST_NOTIFICATIONS"
  "com.metro.music android.permission.READ_MEDIA_AUDIO"
  "com.metro.music android.permission.POST_NOTIFICATIONS"
  "com.metro.people android.permission.READ_CONTACTS"
  "com.metro.people android.permission.POST_NOTIFICATIONS"
  "com.metro.dialer android.permission.READ_CONTACTS"
  "com.metro.dialer android.permission.READ_PHONE_STATE"
  "com.metro.dialer android.permission.CALL_PHONE"
  "com.metro.dialer android.permission.READ_CALL_LOG"
  "com.metro.dialer android.permission.POST_NOTIFICATIONS"
  "com.metro.calendar android.permission.READ_CALENDAR"
  "com.metro.calendar android.permission.WRITE_CALENDAR"
  "com.metro.calendar android.permission.POST_NOTIFICATIONS"
  "com.metro.messaging android.permission.READ_SMS"
  "com.metro.messaging android.permission.SEND_SMS"
  "com.metro.messaging android.permission.RECEIVE_SMS"
  "com.metro.messaging android.permission.READ_CONTACTS"
  "com.metro.messaging android.permission.POST_NOTIFICATIONS"
  "com.metro.weather android.permission.ACCESS_COARSE_LOCATION"
  "com.metro.weather android.permission.ACCESS_FINE_LOCATION"
  "com.metro.weather android.permission.POST_NOTIFICATIONS"
  "com.metro.widgets android.permission.POST_NOTIFICATIONS"
  "com.metro.widgets android.permission.CAMERA"
  "com.metro.conversations android.permission.POST_NOTIFICATIONS"
)
for spec in "${CONTENT_PERMS[@]}"; do
  set -- $spec
  grant "$1" "$2"
done
if is_installed com.metro.conversations; then
  sh cmd notification allow_listener "com.metro.conversations/com.metro.conversations.ConversationsListenerService" >/dev/null 2>&1 \
    && ok "listener: conversations" || true
fi

# --- battery exemption ------------------------------------------------------
echo "==> battery exemption (Doze whitelist)"
for pkg in "${SHELL_APPS[@]}"; do
  p="com.metro.$pkg"
  if is_installed "$p"; then
    if sh cmd deviceidle whitelist "+$p" >/dev/null 2>&1 \
      || sh dumpsys deviceidle whitelist "+$p" >/dev/null 2>&1; then
      ok "battery: $pkg"
    else
      warn "battery failed: $pkg"
    fi
  fi
done

# --- full-screen intent + DND ----------------------------------------------
echo "==> full-screen intent / DND"
sh appops set com.metro.lockscreen android:use_full_screen_intent allow >/dev/null 2>&1 \
  && ok "full-screen intent: lockscreen" || warn "full-screen intent failed"
sh cmd notification allow_dnd com.metro.notifications >/dev/null 2>&1 \
  && ok "DND access: notifications" || true

# --- immersive takeover -----------------------------------------------------
echo "==> immersive takeover"
POLICY="$(policy_control_value)"
if [[ -z "$POLICY" ]]; then
  echo "  (skipped — keeping Android status/nav bars)"
else
  sh settings put global policy_control "$POLICY" >/dev/null 2>&1 \
    && ok "policy_control: $POLICY" || warn "policy_control failed"
fi

echo ""
echo "==> provision complete"
echo "    Immersive: $IMMERSIVE  (change in Settings → 'Set up Windows Phone', or re-run)"
echo "    Restore Android defaults:  ./scripts/provision.sh --restore"
