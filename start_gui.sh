#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "=== 1. Checking Database ==="
if ! sudo service mariadb status >/dev/null 2>&1; then
    echo "Starting MariaDB..."
    sudo service mariadb start
fi

echo "=== 2. Starting Virtual Display (Xvfb) ==="
if ! pgrep -f "Xvfb :99" >/dev/null; then
    Xvfb :99 -screen 0 1280x800x24 &
    sleep 1
fi
export DISPLAY=:99

echo "=== 3. Starting Window Manager (fluxbox) ==="
if ! pgrep -f "fluxbox" >/dev/null; then
    fluxbox >/dev/null 2>&1 &
fi

echo "=== 4. Starting VNC Server ==="
if ! pgrep -f "x11vnc.*5900" >/dev/null; then
    x11vnc -display :99 -forever -nopw -shared -rfbport 5900 -bg >/dev/null 2>&1
fi

echo "=== 5. Starting Web Viewer (noVNC on port 6080) ==="
if ! pgrep -f "websockify.*6080" >/dev/null; then
    websockify --web /usr/share/novnc 6080 localhost:5900 >/dev/null 2>&1 &
    sleep 1
fi

echo ""
echo "==============================================================="
echo " Web GUI viewer is ready!"
echo " Port 6080 is open. Open port 6080 in your browser or VS Code."
echo " Default App Credentials: admin / admin123"
echo "==============================================================="
echo ""

echo "=== 6. Launching Solar Equipment Maintenance Tracker ==="
mvn exec:java -Dexec.mainClass=com.piet.solar.App
