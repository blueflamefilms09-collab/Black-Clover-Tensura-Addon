#!/usr/bin/env python3
"""Why: appends the katana look of Hellblaze, The Demon Sin to its visual_upgrade spec (docs/hellblaze_katana.md). The classic fields stay exactly as they were (the client switch
`visual.hellblazeKatana = false` reads them); the new look is one extra top-level block, "katana", that client/upgrade/UpgradeSpec merges over the classic spec (objects key by key, arrays and
numbers replaced). Run it again and it rewrites only that block. The skins are the ones tools/blender/hellblaze_katana.py bakes.

    python tools/hellblaze/make_spec.py
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SPEC = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "visual_upgrade", "hellblaze_the_demon_sin.json")

K = "up/hellblaze_the_demon_sin__katana"
BLACK, DEEP, RED, BRIGHT, PURPLE = [28, 22, 34], [150, 15, 40], [170, 22, 42], [200, 40, 60], [150, 60, 200]
# the old orange recolours of the act recipes: every rgb of the classic recipes is mapped to black / dark red (the act ids and shapes stay)
RECOLOUR = {(255, 100, 40): RED, (255, 170, 80): BRIGHT, (255, 200, 120): [190, 70, 130], (255, 70, 40): DEEP, (255, 110, 40): RED, (255, 90, 40): [160, 18, 40]}


def recolour(o):
    if isinstance(o, dict):
        return {k: (RECOLOUR.get(tuple(v), v) if k == "rgb" and isinstance(v, list) and len(v) == 3 else recolour(v)) for k, v in o.items()}
    if isinstance(o, list):
        return [recolour(x) for x in o]
    return o


def build(classic):
    acts = recolour(classic["acts"])
    return {
        "note": "the katana look (3.x): black steel and dark red, the sheath in the off hand, a purple flash on every hit",
        "palette": {"primary": DEEP, "secondary": BLACK},
        "mesh": K,
        "maps": {"skin": K + ".png", "glow": K + "_glow.png", "veins": K + "_veins.png"},
        "lod": {"mesh": K + "_lod", "near": 12.0, "glow_off": 24.0, "skin": K + "_lod.png", "glow": K + "_lod_glow.png"},
        "idle": ["glow_breath", "flow_veins", "halo_breath", "rune_flicker"],
        "pass_cfg": {
            "veins": {"rgb": [168, 20, 42], "speed": 0.55, "wavelength": 22.0, "min": 0.2, "max": 1.0, "tint": True},
            "halo": {"rgb": [110, 12, 34], "scale": 1.05, "alpha_min": 8, "alpha_max": 34, "speed": 0.09},
            "flicker": {"amount": 0.1, "speed": 0.7},
        },
        "hit_tone": {"rgb": PURPLE, "ticks": 10},
        "ambient": [
            {"type": "motes", "anchor": "mid", "particle": "anime_multiverse:smoke_wisp", "rgb": [90, 10, 28], "rate": 8, "radius": 0.4, "rise": 0.03},
            {"type": "edge", "anchor": "blade", "particle": "anime_multiverse:smoke_wisp", "rgb": BLACK, "rate": 8, "radius": 0.08, "from": 0.2, "to": 1.0},
            {"type": "drip", "anchor": "tip", "particle": "anime_multiverse:lava_blob", "rgb": [150, 15, 34], "rate": 3, "radius": 0.2},
            {"type": "ground", "anchor": "feet", "particle": "anime_multiverse:flame_tongue", "rgb": [120, 12, 30], "rate": 5, "radius": 0.9, "speed": 0.07, "size": 0.45},
        ],
        "hand": {"length": 1.3, "tilt": 26},
        "swing": {"pattern": "arc", "particle": "anime_multiverse:flame_tongue", "rgb": RED, "rgb2": BLACK, "count": 4, "size": 0.7, "reach": 1.4},
        "acts": acts,
    }


def main():
    with open(SPEC, encoding="utf-8") as fh:
        spec = json.load(fh)
    spec.pop("katana", None)
    classic = dict(spec)
    spec["katana"] = build(classic)
    with open(SPEC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(json.dumps(spec, indent=1, ensure_ascii=False) + "\n")
    print("wrote", SPEC)


if __name__ == "__main__":
    main()
