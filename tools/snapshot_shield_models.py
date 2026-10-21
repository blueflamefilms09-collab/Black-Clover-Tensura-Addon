"""Writes docs/visual_upgrade/shield_fix_before.json: the display blocks and overrides of every shield-class item model as they were before
tools/gen_shield_models.py replaced them (1.110.0). Run it on a checkout from before the fix; kept so the before/after previews can be redrawn."""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
M = os.path.join(ROOT, "src/main/resources/assets/nusmp/models/item")
IDS = "magebane_shield legendary_shield aegis_of_the_abyss celestial_support heros_shield thunder_shield calamity_wave_aegis".split()

out = {}
for i in IDS:
    for suf in ("", "_blocking"):
        p = os.path.join(M, i + suf + ".json")
        out[i + suf] = json.load(open(p)) if os.path.exists(p) else None
dst = os.path.join(ROOT, "docs/visual_upgrade/shield_fix_before.json")
if os.path.exists(dst):
    print("already there, not overwritten:", dst)
else:
    json.dump(out, open(dst, "w"), indent=1, sort_keys=True)
    print("wrote", dst)
