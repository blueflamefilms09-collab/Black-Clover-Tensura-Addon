"""Icon forge v3: the magic icons (grimoire/<magic>.png = Active, icons/<magic>_buff.png = Buff / rune, icons/<magic>_ultimate.png = Ultimate).

The glyph silhouettes are the existing ones (gen_skill_icons.glyph_mask for the 47 original magics, tools/skills/gen_icons_v2.py for the 59 later ones: no
glyph is thrown away), but they are painted by pxr.py: hard pixel mask, 4 flat tones, 1 px selective outline, rim light. Each variant has its own banded
background and a thin plate: Active = gold plate on a midnight aura with energy rays, Buff = silver plate on ancient parchment with a rune ring, Ultimate =
black-iron plate lit in the magic colour on a void with a pentagram ring and a glowing rim. Rank pips use the colours of the relic Grades.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, os.path.join(TOOLS, "skills"))
sys.path.insert(0, TOOLS)                          # tools/gen_skill_icons.py (the magic table) must win over tools/skills/gen_skill_icons.py
import gen_skill_icons as G                        # noqa: E402
import gen_icons_v2 as V2                          # noqa: E402
import pxr                                         # noqa: E402
import icon3_lib as L                              # noqa: E402

S = L.S

EXCLUDE = {"time"}                                 # rule 3: Time Magic is not touched


def keys():
    """[(key, glow, body, dark)] for every magic with a grimoire icon, in a stable order (originals, Set A, Set B)."""
    out = []
    for name in G.MAGIC:
        if name in EXCLUDE or name in V2.SET_A or name in V2.SET_B:
            continue
        _, glow, body, _, _ = G.MAGIC[name]
        out.append((name, glow, body, name in ("anti_magic", "forbidden")))
    for key, (glow, body, glyph, dark) in list(V2.SET_A.items()) + list(V2.SET_B.items()):
        V2.register(key, glow, body, glyph)
        out.append((key, glow, body, dark))
    return out


def _hex(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def glyph_array(key):
    return G.to_arr(G.glyph_mask(key))


def fit(a, scale):
    """Resizes the 256 px glyph mask to the 128 px working canvas, scaled about the centre."""
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L")
    sz = max(2, int(round(S * scale)))
    im = im.resize((sz, sz), Image.LANCZOS)
    full = Image.new("L", (S, S), 0)
    full.paste(im, ((S - sz) // 2, (S - sz) // 2))                    # a scale above 1 is cropped to the canvas (the masks only fill about 70 %)
    return np.asarray(full, dtype=np.float32) / 255.0


def glyph_items(a, glow, body, dark, scale):
    m = fit(a, scale)
    solid = (m > 0.3).astype(np.float32)
    facet = ((m > 0.25) & (m < 0.85)).astype(np.float32)
    g, b = pxr.rgb(_hex(glow)), pxr.rgb(_hex(body))
    c = L.Ctx(dict(a=b, glow=g))
    if dark:
        top = L.mix((70, 54, 78), g, 0.22)
        bot = L.mix((22, 16, 30), g, 0.18)
        c.part(solid, top, bevel=3.0, spec=0.45, grad=(top, bot), k=1.0)
    else:
        top = L.lighten(b, 0.06)
        bot = L.mix(b, g, 0.45) * 0.78
        c.part(solid, top, bevel=3.0, spec=0.40, grad=(top, bot), k=1.0)
    c.flat(facet * solid, L.mix(b if not dark else top, g, 0.55), 0.55)
    return c.items


# ------------------------------------------------------------------------------------------------ native backgrounds
def _blend(img, x, y, col, a):
    if 0 <= x < pxr.OUT and 0 <= y < pxr.OUT:
        img[y, x] = img[y, x] * (1 - a) + np.asarray(col, np.float32) * a


def ring_px(img, cx, cy, r, col, a, ticks=0, dash=0, rot=0.0):
    n = max(24, int(r * 8))
    for i in range(n):
        if dash and (i // dash) % 2:
            continue
        ang = rot + i * 2 * math.pi / n
        _blend(img, int(round(cx + r * math.cos(ang))), int(round(cy + r * math.sin(ang))), col, a)
    for k in range(ticks):
        ang = rot + k * 2 * math.pi / ticks
        for rr in (r + 1, r + 2):
            _blend(img, int(round(cx + rr * math.cos(ang))), int(round(cy + rr * math.sin(ang))), col, a * 0.9)


def star_px(img, cx, cy, r, col, a, n=5, rot=-math.pi / 2):
    pts = [(cx + r * math.cos(rot + i * 2 * math.pi / n), cy + r * math.sin(rot + i * 2 * math.pi / n)) for i in range(n)]
    step = 2 if n >= 5 else 1
    for i in range(n):
        (x0, y0), (x1, y1) = pts[i], pts[(i + step) % n]
        k = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for j in range(k + 1):
            t = j / k
            _blend(img, int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), col, a)


def variant_bg(kind, glow, seed):
    g = pxr.rgb(_hex(glow))
    if kind == "active":
        pal = dict(bg0=L.mix((14, 18, 46), g, 0.34), bg1=(6, 8, 22), glow=g)
    elif kind == "buff":
        pal = dict(bg0=L.mix((132, 104, 70), g, 0.14), bg1=(40, 30, 26), glow=g)
    else:
        pal = dict(bg0=L.mix((12, 8, 20), g, 0.30), bg1=(4, 3, 8), glow=g)
    bg = pxr.make_bg(pal, seed, rays=(kind != "buff"))
    if kind == "buff":
        ring_px(bg, 15.5, 15.5, 12.5, g * 0.9, 0.55, ticks=12, rot=seed * 0.01)
        ring_px(bg, 15.5, 15.5, 9.0, g, 0.35, dash=2)
    elif kind == "ultimate":
        ring_px(bg, 15.5, 15.5, 12.5, g, 0.50, ticks=0)
        star_px(bg, 15.5, 15.5, 12.0, g, 0.40, 5, rot=-math.pi / 2 + (seed % 7) * 0.05)
    return pal, bg


FRAME = {"active": ("gold", None), "buff": ("silver", None), "ultimate": ("iron", None)}


def render(key, glow, body, dark, kind, seed):
    a = glyph_array(key)
    scale = {"active": 1.16, "buff": 0.94, "ultimate": 1.08}[kind]
    items = glyph_items(a, glow, body, dark, scale)
    pal, bg = variant_bg(kind, glow, seed)
    sc = pxr.build(items, pal, seed, bg=bg)
    if kind == "active":
        spec, rank = pxr.METALS["gold"], pxr.METAL_RANK["gold"]
    elif kind == "buff":
        spec, rank = pxr.METALS["silver"], pxr.METAL_RANK["silver"]
    else:
        spec, rank = pxr.iron_frame(_hex(glow)), pxr.METAL_RANK["iron"]
    arr = pxr.compose(sc, "extra", None, accent=_hex(glow), spec=spec, rank=rank, rim_all=(dark or kind == "ultimate"))
    return pxr.to_image(arr)
