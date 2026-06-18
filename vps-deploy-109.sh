#!/bin/bash
set -euo pipefail
TAG="1.0.9"
APP_DIR="/opt/ruomi"
TMP=$(mktemp -d)
curl -fsSL "https://api.github.com/repos/leonius1991/ruomi/releases/tags/${TAG}" -o "$TMP/release.json"
JAR_URL=$(grep -o '"browser_download_url": "[^"]*\.jar"' "$TMP/release.json" | head -1 | cut -d'"' -f4)
ZIP_URL=$(grep -o '"browser_download_url": "[^"]*\.zip"' "$TMP/release.json" | head -1 | cut -d'"' -f4)
mkdir -p "$APP_DIR/backups"
cp "$APP_DIR/app.jar" "$APP_DIR/backups/app-$(date +%Y%m%d_%H%M%S).jar" 2>/dev/null || true
curl -fsSL "$JAR_URL" -o "$APP_DIR/app.jar"
chown ruomi:ruomi "$APP_DIR/app.jar"
if [ -n "$ZIP_URL" ]; then
  curl -fsSL "$ZIP_URL" -o "$TMP/resources.zip"
  unzip -qo "$TMP/resources.zip" -d "$APP_DIR/external-resources/" 2>/dev/null || true
  chown -R ruomi:ruomi "$APP_DIR/external-resources" 2>/dev/null || true
fi
sed -i "s/^app.version=.*/app.version=${TAG}/" "$APP_DIR/application.properties" 2>/dev/null || echo "app.version=${TAG}" >> "$APP_DIR/application.properties"
systemctl restart ruomi
sleep 25
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:8080/
mysql -u ruomi -p'Hjvfynbr1221' ruomi -e "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 2;" 2>/dev/null || true
rm -rf "$TMP"
echo "Deploy ${TAG} done"
