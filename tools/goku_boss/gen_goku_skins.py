#!/usr/bin/env python3
"""The six skins of the Goku boss (the sixth, arc_goku_mui, is Mastered Ultra Instinct of Goku Omega), cut from the shared body arc_npc_default by the Tier List skin tool (tools/tier_bosses/gen_tier_skins.py).

    python tools/goku_boss/gen_goku_skins.py

Writes assets/anime_multiverse/textures/entity/arc_goku.png, arc_goku_ssj.png, arc_goku_god.png, arc_goku_blue.png, arc_goku_ui.png, arc_goku_mui.png and their _glow.png (the belt buckle only).
The boss swaps between them with his form (GokuLadder.SKINS). Only these twelve files are written. The body is the shared one: no new 3D model (see docs/goku_boss.md).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tier_bosses"))
import gen_tier_skins as g  # noqa: E402

ORANGE = 0xE8731A
g.PALETTES = {
    "goku":      dict(hair=0x151515, skin=0xF2C9A4, outfit=ORANGE,   pants=ORANGE,   accent=0x1F4E9E),   # Base and Kaio-ken: black hair, the orange gi
    "goku_ssj":  dict(hair=0xF5D442, skin=0xF2C9A4, outfit=ORANGE,   pants=ORANGE,   accent=0xFFE680),   # Super Saiyan 1-3: golden hair
    "goku_god":  dict(hair=0xD0262B, skin=0xF2C9A4, outfit=ORANGE,   pants=ORANGE,   accent=0xFF8A80),   # Super Saiyan God: red hair
    "goku_blue": dict(hair=0x39A9F0, skin=0xF2C9A4, outfit=ORANGE,   pants=ORANGE,   accent=0x9FE0FF),   # Blue and Blue Kaio-ken: blue hair
    "goku_ui":   dict(hair=0xD8DEE6, skin=0xF2C9A4, outfit=0x2A2F3A, pants=0x2A2F3A, accent=0xC9D6E8),   # Ultra Instinct: silver hair, a dark gi
    "goku_mui":  dict(hair=0xF4F8FF, skin=0xF2C9A4, outfit=0x9AA6BC, pants=0x7C879C, accent=0xFFFFFF),   # Mastered Ultra Instinct (Goku Omega, appended): white-silver hair, a silver gi, white glow
}

if __name__ == "__main__":
    g.main(sys.argv[1:])
