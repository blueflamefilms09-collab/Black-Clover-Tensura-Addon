#!/usr/bin/env python3
"""3.33.0: one skin per Tier List boss, cut from the shared body arc_npc_default (a plain humanoid).

    python tools/tier_bosses/gen_tier_skins.py [--sheet out.png]

Writes assets/nusmp/textures/entity/arc_<id>.png and arc_<id>_glow.png (the accent pixels only, so the buckle / the eyes burn). The body keeps its shading:
every region (hair, skin, outfit, trousers) is recoloured so that a pixel keeps its brightness relative to the region's mean. Regions come from the model's UV rectangles
(assets/nusmp/geo/entity/arc_npc_default.geo.json), so a new tier only needs a new row in PALETTES. Existing files are only ever overwritten for the ids listed here.
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/nusmp"
GEO = ROOT / "geo/entity/arc_npc_default.geo.json"
BASE = ROOT / "textures/entity/arc_npc_default.png"
OUT = ROOT / "textures/entity"

# hair, skin, outfit, trousers, accent (hex).  The letter says where the row stands; the look is the character's own, from memory.
PALETTES = {
    # tier F
    "charlos":         dict(hair=0xD8C060, skin=0xF2C9A4, outfit=0xE8E4D0, pants=0xB8A050, accent=0xFFD54F),
    "zetsu":           dict(hair=0x2E7D32, skin=0x7CB342, outfit=0x1B5E20, pants=0x33691E, accent=0xE8F5E9),
    "yammy_llargo":    dict(hair=0x1C1C1C, skin=0xC49A7A, outfit=0xEFEFEF, pants=0x8D8D8D, accent=0xFF7043),
    "sogou_nobuyuki":  dict(hair=0x3E2723, skin=0xF2C9A4, outfit=0x6A1B9A, pants=0x311B55, accent=0xFFD54F),
    "mr_2_bon_clay":   dict(hair=0x151515, skin=0xF8D8C8, outfit=0xF8BBD0, pants=0xFFFFFF, accent=0xFF4081),
    "kurozumi_orochi": dict(hair=0x1A1A1A, skin=0xE0B890, outfit=0x7B1FA2, pants=0x2E1A47, accent=0xE1BEE7),
    # tier D
    "malty_melromarc": dict(hair=0xB71C1C, skin=0xF8D8C8, outfit=0xC62828, pants=0x6D1B1B, accent=0xFFCDD2),
    "rachel":          dict(hair=0x6B3E26, skin=0xF2C9A4, outfit=0x80DEEA, pants=0x37474F, accent=0xFFFFFF),
    "jugram_haschwalth": dict(hair=0xE8E0C8, skin=0xF2C9A4, outfit=0xECEFF1, pants=0xB0BEC5, accent=0x90CAF9),
    "big_mom":         dict(hair=0xF06292, skin=0xF2C9A4, outfit=0xF06292, pants=0x8E244B, accent=0xFFE082),
    "code":            dict(hair=0xDDDDDD, skin=0xE8C8A8, outfit=0x455A64, pants=0x263238, accent=0x80DEEA),
    "bertholdt_hoover": dict(hair=0x2C1B10, skin=0xE0B890, outfit=0x6D4C41, pants=0x3E2723, accent=0xFF7043),
    "axe_hand_morgan": dict(hair=0x4E342E, skin=0xE0B890, outfit=0x78909C, pants=0x37474F, accent=0xFFE0B2),
    "foxy":            dict(hair=0x9E9E9E, skin=0xE0B890, outfit=0xB0BEC5, pants=0x455A64, accent=0xFF8A65),
    "shou_tucker":     dict(hair=0x5D4037, skin=0xF2C9A4, outfit=0x8D6E63, pants=0x4E342E, accent=0xC5E1A5),
    "ao":              dict(hair=0x1A1A2E, skin=0xE8C8A8, outfit=0x4FC3F7, pants=0x0D47A1, accent=0xFFFFFF),
    # 3.36.0: the gag boss (tier F of the new ladder); the kit skin is painted by gen_tier_looks.py, this is only the fallback when the kit is off
    "light_yagami":    dict(hair=0x6B4426, skin=0xF2DCD0, outfit=0xB79C73, pants=0x45463F, accent=0xC41E24),
}

HEAD_HAIR_ROWS = 10          # of 32: the fringe of the face texture


def rect(face):
    (x, y), (w, h) = face["uv"], face["uv_size"]
    return x, y, w, h


def regions():
    """label per pixel: 'skin' 'hair' 'outfit' 'pants' (or absent)."""
    geo = json.loads(GEO.read_text())["minecraft:geometry"][0]
    lab = {}
    for bone in geo["bones"]:
        n = bone["name"]
        for ci, cube in enumerate(bone.get("cubes", [])):
            kind = None
            if n == "body": kind = "outfit"
            elif n == "head": kind = "face" if ci == 0 else "skin"
            elif n == "hair": kind = "hair"
            elif n.startswith(("arm_", "forearm_")): kind = "outfit"
            elif n.startswith("hand_"): kind = "skin"
            elif n.startswith(("leg_", "shin_")): kind = "pants"
            elif n.startswith("skirt_"): kind = "outfit"
            if not kind: continue
            for side, face in cube["uv"].items():
                x, y, w, h = rect(face)
                for yy in range(y, y + h):
                    for xx in range(x, x + w):
                        k = kind
                        if kind == "face": k = "skinface:%d:%d:%d:%d" % (xx - x, yy - y, w, h)
                        lab[(xx, yy)] = k
    return lab


def lum(p): return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def hexrgb(v): return (v >> 16 & 255, v >> 8 & 255, v & 255)


def is_skin_tone(p): return p[0] > 190 and p[1] > 140 and p[2] > 110 and p[0] >= p[1] >= p[2]


def classify(px, lab, p):
    """the final region of a pixel ('eye' and 'accent' stay special)."""
    l = lab
    if l is None: return None
    if l.startswith("skinface:"):
        _, fx, fy, w, h = l.split(":")
        fx, fy, w, h = int(fx), int(fy), int(w), int(h)
        if is_skin_tone(p): return "skin"
        if fy < h * HEAD_HAIR_ROWS / 32 or (fy < h * 0.65 and (fx < w * 0.12 or fx > w * 0.88)):
            return "hair" if lum(p) < 140 else None
        return None                                   # eyes, brows, mouth, lashes stay
    if l == "outfit" and p[0] > 170 and p[1] > 130 and p[2] < 100: return "accent"     # the buckle
    return l


def main(argv):
    base = Image.open(BASE).convert("RGBA")
    lab = regions()
    W, H = base.size
    px = base.load()
    # per-region mean brightness of the base, for relative shading
    sums, cnt = {}, {}
    cls = {}
    for (x, y), l in lab.items():
        if x >= W or y >= H: continue
        p = px[x, y]
        if p[3] < 128: continue
        c = classify(px, l, p)
        cls[(x, y)] = c
        if c in ("hair", "skin", "outfit", "pants"):
            sums[c] = sums.get(c, 0.0) + lum(p)
            cnt[c] = cnt.get(c, 0) + 1
    mean = {c: sums[c] / cnt[c] for c in sums}
    sheet = []
    only = argv[argv.index("--only") + 1:] if "--only" in argv else []        # 3.36.0: --only id ... repaints just those (every file is otherwise rewritten)
    for cid, pal in PALETTES.items():
        if only and cid not in only:
            continue
        out = base.copy()
        glow = Image.new("RGBA", base.size, (0, 0, 0, 0))
        op, gp = out.load(), glow.load()
        for (x, y), c in cls.items():
            p = px[x, y]
            if c in ("hair", "skin", "outfit", "pants"):
                t = hexrgb(pal[c])
                k = max(0.35, min(1.6, lum(p) / mean[c]))
                op[x, y] = (min(255, int(t[0] * k)), min(255, int(t[1] * k)), min(255, int(t[2] * k)), 255)
            elif c == "accent":
                t = hexrgb(pal["accent"])
                k = max(0.6, min(1.2, lum(p) / 190))
                col = (min(255, int(t[0] * k)), min(255, int(t[1] * k)), min(255, int(t[2] * k)), 255)
                op[x, y] = col
                gp[x, y] = col
        out.save(OUT / ("arc_%s.png" % cid), optimize=True)
        glow.save(OUT / ("arc_%s_glow.png" % cid), optimize=True)
        sheet.append((cid, out.crop((0, 0, 33, 32)), out.crop((33, 0, 50, 21)), out.crop((205, 0, 220, 13))))
    print("wrote", len(PALETTES), "skins")
    if "--sheet" in argv:
        path = argv[argv.index("--sheet") + 1]
        S = 4
        cell_w, cell_h = 200, 150
        img = Image.new("RGB", (cell_w * 4, cell_h * ((len(sheet) + 3) // 4)), (30, 32, 38))
        d = ImageDraw.Draw(img)
        for i, (cid, face, chest, leg) in enumerate(sheet):
            ox, oy = (i % 4) * cell_w, (i // 4) * cell_h
            bg = Image.new("RGBA", (cell_w, cell_h), (30, 32, 38, 255))
            img.paste(face.resize((33 * S // 2 * 2 // 2, 32 * S // 2 * 2 // 2), Image.NEAREST), (ox + 6, oy + 18), face.resize((33, 32), Image.NEAREST).resize((33 * S // 2 * 2 // 2, 32 * S // 2 * 2 // 2), Image.NEAREST))
            img.paste(chest.resize((17 * S, 21 * S), Image.NEAREST), (ox + 80, oy + 18))
            img.paste(leg.resize((15 * S, 13 * S), Image.NEAREST), (ox + 150, oy + 18))
            d.text((ox + 6, oy + 4), cid, fill=(240, 240, 240))
        img.save(path)
        print("sheet", path)


if __name__ == "__main__":
    main(sys.argv[1:])
