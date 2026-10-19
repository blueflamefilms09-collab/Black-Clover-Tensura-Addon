"""The size / orientation audit of docs/weapon_scale_orientation.md, filtered to the 35 weapons of 1.97.0 (and their draw states):

    python3 -B tools/relics/new197_audit.py
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from new197_roster import ROSTER                                    # noqa: E402

ids = {e["id"] for e in ROSTER}


def mine(name):
    return name in ids or any(name == i + "_charged" or name.startswith(i + "_pull") for i in ids)


def main():
    out = subprocess.run([sys.executable, "-B", os.path.join(HERE, "audit_weapon_scale.py")], capture_output=True, text=True, cwd=os.path.join(HERE, "..", "..")).stdout
    bad = 0
    for line in out.splitlines():
        parts = line.split()
        if parts and mine(parts[0]):
            print(line)
            if any(w in line for w in ("small", "large", "head_toward_player", "tip_down", "head_small")):
                bad += 1
    print("weapons of 1.97.0 with a flag:", bad)


if __name__ == "__main__":
    main()
