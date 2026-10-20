#!/usr/bin/env python3
import os
import time
import subprocess
from PIL import Image

ENV = dict(os.environ, DISPLAY=":99")
SNAPSHOT_DIR = "/workspaces/first_java_program/snapshots"
os.makedirs(SNAPSHOT_DIR, exist_ok=True)

def run(cmd):
    return subprocess.run(cmd, shell=True, env=ENV, capture_output=True, text=True)

def get_window_id(name):
    res = run(f'xdotool search --name "{name}"')
    lines = [l.strip() for l in res.stdout.splitlines() if l.strip()]
    return lines[-1] if lines else None

def get_window_box(win_id):
    res = run(f"xdotool getwindowgeometry {win_id}")
    pos, geom = (0, 0), (800, 600)
    for line in res.stdout.splitlines():
        line = line.strip()
        if line.startswith("Position:"):
            parts = line.split()[1].split(",")
            pos = (int(parts[0]), int(parts[1]))
        elif line.startswith("Geometry:"):
            parts = line.split()[1].split("x")
            geom = (int(parts[0]), int(parts[1]))
    return (pos[0], pos[1], pos[0] + geom[0], pos[1] + geom[1])

def capture_window(filename, win_id):
    raw_path = f"/tmp/raw_shot.png"
    run(f"scrot {raw_path}")
    img = Image.open(raw_path)
    box = get_window_box(win_id)
    pad = 2
    crop_box = (
        max(0, box[0] - pad),
        max(0, box[1] - 25),
        min(img.width, box[2] + pad),
        min(img.height, box[3] + pad)
    )
    img = img.crop(crop_box)
    dest = os.path.join(SNAPSHOT_DIR, filename)
    img.save(dest)
    print(f"Captured: {dest} ({img.size})")

main_win = get_window_id("Solar Equipment Maintenance Tracker")
print(f"MainFrame window: {main_win}")
run(f"xdotool windowactivate {main_win}")
time.sleep(0.5)

# Click on the tab strip to focus the JTabbedPane
run("xdotool mousemove 140 167 click 1")
time.sleep(0.5)

# Reset to tab 0 (Dashboard)
for _ in range(6):
    run("xdotool key ctrl+Page_Up")
    time.sleep(0.2)
time.sleep(0.5)

tabs = [
    ("02_dashboard_kpi.png", "Dashboard"),
    ("03_equipment_tab.png", "Equipment"),
    ("04_maintenance_schedule_tab.png", "Maintenance Schedule"),
    ("05_maintenance_records_tab.png", "Maintenance Records"),
    ("06_equipment_parts_tab.png", "Equipment Parts"),
    ("07_users_tab.png", "Users")
]

for idx, (filename, label) in enumerate(tabs):
    print(f"Recording Tab {idx}: {label}")
    capture_window(filename, main_win)
    if idx < len(tabs) - 1:
        run("xdotool key ctrl+Page_Down")
        time.sleep(0.8)

print("All snapshots recorded!")
