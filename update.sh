#!/bin/bash
#
# Обновление Ruomi на VPS: скачивает последний релиз с GitHub и перезапускает сервис.
#
# Использование:
#   sudo bash update.sh              # последний релиз + перезапуск
#   sudo bash update.sh --tag 1.4.55 # конкретная версия
#   sudo bash update.sh --no-restart # только скачать, без перезапуска
#
# Переменные окружения (опционально):
#   APP_NAME, APP_DIR, GITHUB_OWNER, GITHUB_REPO, GITHUB_TOKEN

set -euo pipefail

APP_NAME="${APP_NAME:-ruomi}"
APP_DIR="${APP_DIR:-/opt/$APP_NAME}"
GITHUB_OWNER="${GITHUB_OWNER:-leonius1991}"
GITHUB_REPO="${GITHUB_REPO:-ruomi}"
JAR_FILE="${JAR_FILE:-$APP_DIR/app.jar}"
BACKUP_DIR="${BACKUP_DIR:-$APP_DIR/backups}"
DOWNLOAD_DIR="${DOWNLOAD_DIR:-$APP_DIR/updates}"
EXTERNAL_RESOURCES="${EXTERNAL_RESOURCES:-$APP_DIR/external-resources}"

TAG=""
SKIP_RESTART=false
SKIP_RESOURCES=false

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

usage() {
    cat <<EOF
Обновление Ruomi с GitHub Releases

  sudo bash update.sh [опции]

Опции:
  --tag VERSION       Установить конкретный тег (например 1.4.55)
  --no-restart        Не перезапускать сервис после обновления
  --skip-resources    Не обновлять templates/static из ZIP
  -h, --help          Показать эту справку

Примеры:
  sudo bash update.sh
  sudo bash update.sh --tag 1.4.55
  GITHUB_TOKEN=ghp_xxx sudo bash update.sh
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tag)
            TAG="${2:-}"
            [[ -n "$TAG" ]] || error "Укажите тег после --tag"
            shift 2
            ;;
        --no-restart)
            SKIP_RESTART=true
            shift
            ;;
        --skip-resources)
            SKIP_RESOURCES=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            error "Неизвестный аргумент: $1 (используйте --help)"
            ;;
    esac
done

if [[ "$EUID" -ne 0 ]]; then
    if command -v sudo >/dev/null 2>&1; then
        exec sudo "$0" "$@"
    fi
    error "Запустите скрипт от root: sudo bash update.sh"
fi

if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
    error "Установите curl или wget"
fi

if ! command -v unzip >/dev/null 2>&1; then
    error "Установите unzip: apt install unzip"
fi

if ! systemctl list-unit-files 2>/dev/null | grep -q "${APP_NAME}.service"; then
    error "Systemd сервис ${APP_NAME}.service не найден"
fi

mkdir -p "$BACKUP_DIR" "$DOWNLOAD_DIR" "$EXTERNAL_RESOURCES/templates" "$EXTERNAL_RESOURCES/static"

download_url() {
    local url="$1"
    local dest="$2"
    info "Скачивание: $(basename "$dest")"
    if command -v curl >/dev/null 2>&1; then
        local args=(-fsSL "$url" -o "$dest")
        if [[ -n "${GITHUB_TOKEN:-}" ]]; then
            args=(-fsSL -H "Authorization: Bearer $GITHUB_TOKEN" "$url" -o "$dest")
        fi
        curl "${args[@]}"
    else
        if [[ -n "${GITHUB_TOKEN:-}" ]]; then
            wget -q --header="Authorization: Bearer $GITHUB_TOKEN" "$url" -O "$dest"
        else
            wget -q "$url" -O "$dest"
        fi
    fi
}

fetch_release_json() {
    local api_url
    if [[ -n "$TAG" ]]; then
        api_url="https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/tags/${TAG}"
    else
        api_url="https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest"
    fi

    local tmp
    tmp="$(mktemp)"
    if command -v curl >/dev/null 2>&1; then
        local args=(-fsSL "$api_url" -o "$tmp")
        if [[ -n "${GITHUB_TOKEN:-}" ]]; then
            args=(-fsSL -H "Authorization: Bearer $GITHUB_TOKEN" -H "Accept: application/vnd.github+json" "$api_url" -o "$tmp")
        fi
        if ! curl "${args[@]}"; then
            rm -f "$tmp"
            error "Не удалось получить релиз: $api_url"
        fi
    else
        if [[ -n "${GITHUB_TOKEN:-}" ]]; then
            if ! wget -q --header="Authorization: Bearer $GITHUB_TOKEN" --header="Accept: application/vnd.github+json" "$api_url" -O "$tmp"; then
                rm -f "$tmp"
                error "Не удалось получить релиз: $api_url"
            fi
        else
            if ! wget -q "$api_url" -O "$tmp"; then
                rm -f "$tmp"
                error "Не удалось получить релиз: $api_url"
            fi
        fi
    fi
    echo "$tmp"
}

parse_asset_url() {
    local json_file="$1"
    local pattern="$2"
    grep -o "\"browser_download_url\": \"[^\"]*${pattern}[^\"]*\"" "$json_file" \
        | head -1 \
        | cut -d'"' -f4
}

rollback_jar() {
    local backup="$1"
    if [[ -f "$backup" ]]; then
        warn "Откат JAR из бэкапа..."
        cp "$backup" "$JAR_FILE"
        chown "$APP_NAME:$APP_NAME" "$JAR_FILE" 2>/dev/null || true
    fi
}

echo "========================================"
echo "   Обновление Ruomi"
echo "========================================"
info "Репозиторий: ${GITHUB_OWNER}/${GITHUB_REPO}"
info "Директория:  $APP_DIR"
info "JAR:         $JAR_FILE"
echo

RELEASE_JSON="$(fetch_release_json)"
RELEASE_TAG="$(grep -m1 '"tag_name"' "$RELEASE_JSON" | cut -d'"' -f4)"
RELEASE_NAME="${RELEASE_TAG:-unknown}"

JAR_URL="$(parse_asset_url "$RELEASE_JSON" '\.jar')"
RESOURCES_URL="$(parse_asset_url "$RELEASE_JSON" 'resources.*\.zip')"

rm -f "$RELEASE_JSON"

[[ -n "$JAR_URL" ]] || error "В релизе ${RELEASE_NAME} нет JAR файла. Сначала создайте релиз на GitHub."

info "Релиз: $RELEASE_NAME"
info "JAR URL: $JAR_URL"
if [[ -n "$RESOURCES_URL" && "$SKIP_RESOURCES" = false ]]; then
    info "Resources URL: $RESOURCES_URL"
fi

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
NEW_JAR="$DOWNLOAD_DIR/doska-${RELEASE_NAME}.jar"
BACKUP_JAR="$BACKUP_DIR/app-${TIMESTAMP}.jar"

if [[ -f "$JAR_FILE" ]]; then
    info "Бэкап текущего JAR -> $BACKUP_JAR"
    cp "$JAR_FILE" "$BACKUP_JAR"
    chown "$APP_NAME:$APP_NAME" "$BACKUP_JAR" 2>/dev/null || true
fi

download_url "$JAR_URL" "$NEW_JAR"
chown "$APP_NAME:$APP_NAME" "$NEW_JAR" 2>/dev/null || true

info "Установка нового JAR..."
cp "$NEW_JAR" "$JAR_FILE"
chown "$APP_NAME:$APP_NAME" "$JAR_FILE" 2>/dev/null || true
chmod 644 "$JAR_FILE"

if [[ -n "$RESOURCES_URL" && "$SKIP_RESOURCES" = false ]]; then
    RESOURCES_ZIP="$DOWNLOAD_DIR/resources-${RELEASE_NAME}.zip"
    download_url "$RESOURCES_URL" "$RESOURCES_ZIP"

    EXTRACT_DIR="$(mktemp -d)"
    info "Распаковка ресурсов в $EXTERNAL_RESOURCES..."
    unzip -oq "$RESOURCES_ZIP" -d "$EXTRACT_DIR"

    if [[ -d "$EXTRACT_DIR/templates" ]]; then
        rsync -a "$EXTRACT_DIR/templates/" "$EXTERNAL_RESOURCES/templates/" 2>/dev/null \
            || cp -r "$EXTRACT_DIR/templates/." "$EXTERNAL_RESOURCES/templates/"
    fi
    if [[ -d "$EXTRACT_DIR/static" ]]; then
        rsync -a "$EXTRACT_DIR/static/" "$EXTERNAL_RESOURCES/static/" 2>/dev/null \
            || cp -r "$EXTRACT_DIR/static/." "$EXTERNAL_RESOURCES/static/"
    fi

    chown -R "$APP_NAME:$APP_NAME" "$EXTERNAL_RESOURCES" 2>/dev/null || true
    rm -rf "$EXTRACT_DIR" "$RESOURCES_ZIP"
    info "Ресурсы обновлены"
fi

if [[ "$SKIP_RESTART" = true ]]; then
    info "Перезапуск пропущен (--no-restart)"
    info "Запустите вручную: sudo systemctl restart $APP_NAME"
    exit 0
fi

info "Перезапуск сервиса $APP_NAME..."
if systemctl restart "$APP_NAME"; then
    sleep 4
    if systemctl is-active --quiet "$APP_NAME"; then
        echo
        info "Обновление завершено успешно! Версия: $RELEASE_NAME"
        systemctl status "$APP_NAME" --no-pager -l | head -15
        exit 0
    fi
fi

error_msg="Сервис не запустился после обновления"
warn "$error_msg"
journalctl -u "$APP_NAME" -n 30 --no-pager || true
rollback_jar "$BACKUP_JAR"
systemctl restart "$APP_NAME" || true
error "$error_msg. JAR откачен к предыдущей версии. Проверьте: sudo journalctl -u $APP_NAME -n 50"
