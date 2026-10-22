#!/usr/bin/env python3
"""Headless previews of the EX-Singularity grade (the one above Genesis, Shishou's grade). docs/relics/ex_singularity.md

    python3 -B tools/relics/preview_ex_singularity.py [tooltip|frame|sword|all]

The game cannot be run here, so these are MOCKS built from the same numbers the game uses:
  * the palette functions below are line-for-line ports of src/main/java/.../relics/ExSingularity.java (the self check at the top compares them with values printed by the Java class);
  * the tooltip is drawn with Unifont (the font Minecraft itself falls back to for the star glyphs) at 2 px per Minecraft pixel, laid out like client/ui/tooltip/UnifiedTooltip
    (name, banner row with a hairline, type row, the item's lines, a hairline, the footer, a gradient border over dark glass). Letter widths differ slightly from the game's own font;
  * the slot frame replays the 15 fills of client/ex/ExSingularityClient.paint() with ordinary alpha blending on a dark slot, the item icon in the middle;
  * the sword is rasterised with tools/item_preview/preview_weapons.py's renderer (the real mesh and skin), the glow tinted by ExSingularity.glowRgb on the neutral prism glow skin, plus the blade
    veil (the blade's own quads once more, additive, ExSingularity.veilRgb per quad) and the eight orbiting shards (the real star texture, additive).
Output: docs/previews/ex_singularity/{tooltip_compare,tooltip_phases,slot_frame_phases,sword_phases}.png
"""
import importlib.util
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ROOT, "docs", "previews", "ex_singularity")
UNIFONT = "/usr/share/fonts/opentype/unifont/unifont.otf"
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

# ======================================================================== ports of relics/ExSingularity.java (no allocation concerns here)
TEXT_PERIOD_MS, TEXT_HUE_STEP, GLOW_PERIOD_TICKS, FRAME_PERIOD_MS = 5200, 0.055, 200.0, 6000
STARS = "✦" + "★" * 9 + "✦"


def clamp01(v):
    return 0.0 if v < 0 else min(v, 1.0)


def hsv(h, s, v):
    h = h - math.floor(h)
    s, v = clamp01(s), clamp01(v)
    x = h * 6.0
    i = int(x)
    f = x - i
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    r, g, b = [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)][min(i, 5)]
    return (round(r * 255) << 16) | (round(g * 255) << 8) | round(b * 255)


def toward_white(rgb, amount):
    k = clamp01(amount)
    r, g, b = rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255
    r += round((255 - r) * k); g += round((255 - g) * k); b += round((255 - b) * k)
    return (r << 16) | (g << 8) | b


def text_rgb(i, count, ms):
    hue = (ms % TEXT_PERIOD_MS) / TEXT_PERIOD_MS + i * TEXT_HUE_STEP
    rgb = hsv(hue, 0.78, 1.0)
    n = max(1, count)
    pos = (ms % 1600) / 1600.0 * 1.5 - 0.25
    d = abs(pos - i / n)
    return toward_white(rgb, 0.85 * (1 - d / 0.12)) if d < 0.12 else rgb


def row_rgb(ms, offset):
    return hsv((ms % TEXT_PERIOD_MS) / TEXT_PERIOD_MS + offset, 0.72, 1.0)


def border_rgb(ms, end):
    return hsv((ms % 7000) / 7000.0 + (0.33 if end else 0.0), 0.80, 1.0)


def glow_rgb(t): return hsv(t / GLOW_PERIOD_TICKS, 0.90, 0.78)
def veil_rgb(y, t):
    """ExSingularity.veilRgb: the spectrum along the blade (y 0 = pommel end, 1 = tip), dim, with a bright band running up it every 60 ticks."""
    hue = t / GLOW_PERIOD_TICKS + y * 0.8
    pos = ((t / 60.0) % 1.0) * 1.5 - 0.25
    d = abs(y - pos)
    band = 1.0 - d / 0.25 if d < 0.25 else 0.0
    return hsv(hue, 0.85, 0.16 + 0.50 * band * band)
def shard_rgb(i, n, t): return hsv(t / GLOW_PERIOD_TICKS + i / max(1, n), 0.70, 1.0)
def pulse(t): return 0.81 + 0.19 * math.sin(t * 0.07)


def frame_rgb(k, n, ms, counter):
    t = (ms % FRAME_PERIOD_MS) / FRAME_PERIOD_MS
    seg = k / max(1, n)
    return hsv(seg - t if counter else seg + t, 0.85, 1.0)


def frame_alpha(inner, ms):
    b = 0.5 + 0.5 * math.sin((ms % FRAME_PERIOD_MS) / FRAME_PERIOD_MS * 2 * math.pi * 2)
    return 100 + round(50 * b) if inner else 200 + round(55 * b)


def twinkle(c, ms):
    p = ((ms % 2400) / 2400.0 - c * 0.25) % 1.0
    return math.sin(p / 0.25 * math.pi) if p < 0.25 else 0.0


def shard(i, count, t, radius):
    a = i * (2 * math.pi / max(1, count)) + t * (0.045 + 0.006 * (i % 3))
    r = radius * (0.85 + 0.3 * ((i * 7) % 5) / 4.0)
    y = 0.12 + 0.76 * (((i * 0.381966 + t * 0.0035 * (1 + (i & 1))) % 1.0))
    return math.cos(a) * r, y, math.sin(a) * r, 0.5 + 0.5 * math.sin(t * 0.21 + i * 1.7)


def self_check():
    """The ports against the Java class (values printed by a one-off main over the compiled ExSingularity)."""
    assert f"{text_rgb(0, 24, 0):06X}" == "FF3838" and f"{text_rgb(5, 24, 900):06X}" == "38FFC1" and f"{text_rgb(23, 24, 2500):06X}" == "9738FF"
    assert f"{glow_rgb(33):06X}" == "C7C514" and f"{glow_rgb(120):06X}" == "145BC7"
    assert f"{frame_rgb(3, 8, 1500, False):06X}" == "265CFF" and f"{frame_rgb(7, 8, 1500, True):06X}" == "265CFF" and frame_alpha(False, 4000) == 251 and frame_alpha(True, 4000) == 147
    assert f"{border_rgb(3000, False):06X}" == "33FFA8" and f"{border_rgb(3000, True):06X}" == "A333FF"
    s = shard(7, 8, 333.0, 0.13)
    assert abs(s[0] + 0.1319) < 1e-3 and abs(s[1] - 0.1236) < 1e-3 and abs(s[2] + 0.0703) < 1e-3 and abs(s[3] - 0.5740) < 1e-3
    assert abs(twinkle(1, 900) - 1.0) < 1e-6 and abs(twinkle(0, 150) - 0.7071) < 1e-3


# ======================================================================== the tooltip mock
S = 2                                   # px per Minecraft pixel
LINE = 10


def rgb3(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


class Font:
    def __init__(self):
        self.f = ImageFont.truetype(UNIFONT, 8 * S)

    def width(self, s):
        return round(self.f.getlength(s))


FONT = None


def hairline(d, x0, y, x1, ms, alpha):
    """PrismText.hairline: eight segments, each its own hue (ExSingularity.rowRgb with offsets k/8 * 0.6)."""
    w = x1 - x0
    for k in range(8):
        a, b = x0 + w * k // 8, x0 + w * (k + 1) // 8
        d.rectangle([a, y, b - 1, y + S - 1], fill=rgb3(row_rgb(ms, k / 8.0 * 0.6)) + (alpha,))


def tooltip(rows, ms, style):
    """rows: list of (kind, text, colour|None). Kinds: name, banner, type, text, hair, foot, prism, prism_name, prism_banner.
    Returns a PIL image. style: 'genesis'|'unique'|'ex' picks the frame."""
    global FONT
    FONT = FONT or Font()
    f = FONT.f
    pad = 4 * S
    width = max([FONT.width(t) for k, t, c in rows if k not in ("hair",)] + [60 * S])
    height = sum((LINE * S if k != "hair" else 4 * S) for k, t, c in rows)
    W, H = width + 2 * pad + 2 * S, height + 2 * pad + 2 * S - 2 * S
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    if style == "ex":
        bg0, bg1 = 0xF2160C2E, 0xF2080414
        b0, b1 = border_rgb(ms, False), border_rgb(ms, True)
    elif style == "genesis":
        bg0, bg1 = 0xF2101A30, 0xF2070B16
        b0 = hsv((ms % 4000) / 4000.0, 0.35, 1.0); b1 = None
    else:
        bg0, bg1 = 0xF2101A30, 0xF2070B16
        b0 = 0xFF77FF; b1 = None
    if b1 is None:                                                      # the border fades toward violet (UiMath.mix(c, 0xFF3A2A8A, 0.6))
        r, g, b = rgb3(b0)
        b1 = ((round(r + (0x3A - r) * 0.6)) << 16) | (round(g + (0x2A - g) * 0.6) << 8) | round(b + (0x8A - b) * 0.6)
    d = ImageDraw.Draw(img)
    for y in range(H):                                                  # vertical gradient glass + 1 MC px border
        t = y / max(1, H - 1)
        bgc = tuple(round(((bg0 >> sh) & 255) * (1 - t) + ((bg1 >> sh) & 255) * t) for sh in (16, 8, 0))
        a = round(((bg0 >> 24) & 255) * (1 - t) + ((bg1 >> 24) & 255) * t)
        d.line([(0, y), (W, y)], fill=bgc + (a,))
        bc = tuple(round(((b0 >> sh) & 255) * (1 - t) + ((b1 >> sh) & 255) * t) for sh in (16, 8, 0))
        d.rectangle([0, y, S - 1, y], fill=bc + (255,))
        d.rectangle([W - S, y, W - 1, y], fill=bc + (255,))
    bt = tuple((b0 >> sh) & 255 for sh in (16, 8, 0)); bb = tuple((b1 >> sh) & 255 for sh in (16, 8, 0))
    d.rectangle([0, 0, W - 1, S - 1], fill=bt + (255,))
    d.rectangle([0, H - S, W - 1, H - 1], fill=bb + (255,))
    y = pad
    for kind, text, col in rows:
        if kind == "hair_prism":
            hairline(d, pad, y + S, W - pad, ms, 110)
            y += 4 * S
            continue
        if kind == "hair":
            hc = col if col is not None else 0x8899AA
            d.rectangle([pad, y + S, W - pad, y + 2 * S - 1], fill=rgb3(hc) + (110,))
            y += 4 * S
            continue
        if kind in ("prism", "prism_name", "prism_banner"):
            x = pad
            n = len(text)
            for i, ch in enumerate(text):
                d.text((x, y), ch, font=f, fill=rgb3(text_rgb(i, n, ms)) + (255,))
                x += FONT.width(ch)
            if kind == "prism_banner":
                hairline(d, pad, y + (LINE - 1) * S, W - pad, ms, 200)
        else:
            d.text((pad, y), text, font=f, fill=rgb3(col) + (255,))
            if kind == "banner":
                d.rectangle([pad, y + (LINE - 1) * S, W - pad, y + LINE * S - 1], fill=rgb3(col) + (115,))
        y += LINE * S
    return img


SLATE, FOOT, AQUA, GRAY, DGRAY, DAQUA = 0x8899AA, 0x777788, 0x55FFFF, 0xAAAAAA, 0x555555, 0x00AAAA


def shishou_lines():
    return [("text", "Level 12", AQUA), ("text", "Soulbound. Only its wielder may hold it.", GRAY),
            ("text", "Shift + right click: Stomach (internal storage).", DGRAY),
            ("text", "Keys: throw or recall, cast, next skill.", DGRAY),
            ("text", "Stomach (Ibukuro)", DAQUA), ("text", "Sword Arts Mastery (Kenjutsu)", DAQUA)]


def tooltip_ex(ms):
    return tooltip([("prism_name", "Shishou", 0), ("prism_banner", STARS + "  EX-Singularity Grade", 0), ("type", "Type: Akashic Relic", SLATE),
                    ("prism", "Akashic Relic - Grade: EX-Singularity", 0)] + shishou_lines() + [("hair", "", 0xFFFFFF), ("foot", "Multiverse of Anime  ·  Akashic Relic", FOOT)], ms, "ex")


def tooltip_before(ms):
    return tooltip([("name", "Shishou", 0xFF77FF), ("banner", "★★★★☆☆☆  Unique Grade", 0xFF77FF), ("type", "Type: Item", SLATE),
                    ("text", "Akashic Relic - Grade: EX-Singularity", 0xFFD76A)] + shishou_lines() + [("hair", "", 0xFF77FF), ("foot", "Multiverse of Anime  ·  Item", FOOT)], ms, "unique")


def tooltip_genesis(ms):
    c = 0xFF000000 | hsv((ms % 4000) / 4000.0, 0.35, 1.0)
    cc = hsv((ms % 4000) / 4000.0, 0.35, 1.0)
    return tooltip([("name", "Zahard's Crown Fragment", 0xFFFFFF), ("banner", "★★★★★★★  Genesis Grade", cc), ("type", "Type: Material", SLATE),
                    ("text", "A shard of a fallen crown.", GRAY), ("hair", "", 0xFFFFFF), ("foot", "Multiverse of Anime  ·  Material", FOOT)], ms, "genesis")


def sheet(images, labels, gap=18, pad=14, label_h=34, bg=(24, 26, 34)):
    lab = ImageFont.truetype(SANS, 15)
    w = sum(i.width for i in images) + gap * (len(images) - 1) + 2 * pad
    h = max(i.height for i in images) + label_h + 2 * pad
    out = Image.new("RGBA", (w, h), bg + (255,))
    x = pad
    d = ImageDraw.Draw(out)
    for im, text in zip(images, labels):
        d.text((x, pad), text, font=lab, fill=(220, 222, 230, 255))
        out.alpha_composite(im, (x, pad + label_h))
        x += im.width + gap
    return out


def do_tooltip():
    os.makedirs(OUT, exist_ok=True)
    ms = 900
    s = sheet([tooltip_before(ms), tooltip_genesis(ms), tooltip_ex(ms)],
              ["Shishou in 2.0.0 (Unique, yellow line)", "Genesis (Zahard's Crown Fragment)", "Shishou now: EX-Singularity"])
    s.save(os.path.join(OUT, "tooltip_compare.png"))
    ph = [0, 1300, 2600, 3900]
    s2 = sheet([tooltip_ex(m) for m in ph], [f"EX-Singularity at t = {m} ms" for m in ph])
    s2.save(os.path.join(OUT, "tooltip_phases.png"))
    print("wrote tooltip_compare.png, tooltip_phases.png")


# ======================================================================== the slot frame
def blend(img, x0, y0, x1, y1, rgb, a):
    """alpha-blend a filled rectangle on a numpy float image (MC px coordinates scaled by the caller)."""
    c = np.array(rgb3(rgb), np.float32)
    region = img[y0:y1, x0:x1]
    region[:] = region * (1 - a / 255.0) + c * (a / 255.0)


def slot_image(ms, K=10):
    icon = Image.open(os.path.join(ASSETS, "textures", "item", "shishou.png")).convert("RGBA")
    W = 20 * K
    base = Image.new("RGBA", (W, W), (55, 55, 62, 255))
    ImageDraw.Draw(base).rectangle([K, K, W - K - 1, W - K - 1], fill=(139, 139, 139, 255))
    ImageDraw.Draw(base).rectangle([K, K, W - K - 1, W - K - 1], outline=(55, 55, 55, 255))
    ic = icon.resize((16 * K, 16 * K), Image.NEAREST)
    base.alpha_composite(ic, (2 * K, 2 * K))
    img = np.asarray(base.convert("RGB"), np.float32).copy()
    x, y = 2, 2                                            # the item's top-left in slot px

    def fill(x0, y0, x1, y1, argb):
        blend(img, x0 * K, y0 * K, x1 * K, y1 * K, argb & 0xFFFFFF, argb >> 24 & 255)

    def grad(x0, y0, x1, y1, a_from, a_to):                # GuiGraphics.fillGradient: top colour to bottom colour, rows interpolated
        rows = (y1 - y0) * K
        for r in range(rows):
            t = (r + 0.5) / rows
            c = [round(((a_from >> sh) & 255) * (1 - t) + ((a_to >> sh) & 255) * t) for sh in (24, 16, 8, 0)]
            blend(img, x0 * K, y0 * K + r, x1 * K, y0 * K + r + 1, (c[1] << 16) | (c[2] << 8) | c[3], c[0])

    oa, ia = frame_alpha(False, ms) << 24, frame_alpha(True, ms) << 24
    n = 68
    for i in range(6):
        fill(x - 1 + 3 * i, y - 1, x + 2 + 3 * i, y, oa | frame_rgb(3 * i + 1, n, ms, False))
        fill(x + 14 - 3 * i, y + 16, x + 17 - 3 * i, y + 17, oa | frame_rgb(35 + 3 * i, n, ms, False))
    grad(x + 16, y, x + 17, y + 8, oa | frame_rgb(18, n, ms, False), oa | frame_rgb(26, n, ms, False))
    grad(x + 16, y + 8, x + 17, y + 16, oa | frame_rgb(26, n, ms, False), oa | frame_rgb(34, n, ms, False))
    grad(x - 1, y + 8, x, y + 16, oa | frame_rgb(60, n, ms, False), oa | frame_rgb(52, n, ms, False))
    grad(x - 1, y, x, y + 8, oa | frame_rgb(68, n, ms, False), oa | frame_rgb(60, n, ms, False))
    inner = [(x, y, x + 16, y + 1), (x + 15, y + 1, x + 16, y + 16), (x, y + 15, x + 15, y + 16), (x, y + 1, x + 1, y + 15)]
    for k, (a, b, c, d) in enumerate(inner):
        fill(a, b, c, d, ia | frame_rgb(k, 4, ms, True))
    fill(x, y, x + 16, y + 16, 0x14000000 | frame_rgb(0, 1, ms, False))
    for c in range(4):
        tw = twinkle(c, ms)
        if tw <= 0.05:
            continue
        cx = x if c in (0, 3) else x + 15
        cy = y if c < 2 else y + 15
        a = round(255 * tw) << 24 | toward_white(frame_rgb(c * 17, n, ms, False), 0.7)
        fill(cx - 2, cy, cx + 3, cy + 1, a)
        fill(cx, cy - 2, cx + 1, cy + 3, a)
        break
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).convert("RGBA")


def do_frame():
    os.makedirs(OUT, exist_ok=True)
    ph = [0, 1500, 3000, 4500]
    plain = slot_image_plain()
    s = sheet([plain] + [slot_image(m) for m in ph], ["2.0.0: no frame"] + [f"EX frame, t = {m} ms" for m in ph])
    s.save(os.path.join(OUT, "slot_frame_phases.png"))
    print("wrote slot_frame_phases.png")


def slot_image_plain(K=10):
    icon = Image.open(os.path.join(ASSETS, "textures", "item", "shishou.png")).convert("RGBA")
    W = 20 * K
    base = Image.new("RGBA", (W, W), (55, 55, 62, 255))
    ImageDraw.Draw(base).rectangle([K, K, W - K - 1, W - K - 1], fill=(139, 139, 139, 255))
    base.alpha_composite(icon.resize((16 * K, 16 * K), Image.NEAREST), (2 * K, 2 * K))
    return base


# ======================================================================== the sword
SIZE = 400


def load_psw():
    spec = importlib.util.spec_from_file_location("psw", os.path.join(ROOT, "tools", "item_preview", "preview_weapons.py"))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    m.W = m.H = SIZE
    m.STARS = np.zeros((SIZE, SIZE, 3), np.float32)
    return m


def place_info(item_id):
    for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt")):
        v = line.split()
        if v and v[0] == "place":
            return [float(x) for x in v[1:]]


def mesh_ymax(item_id):
    top = 0.0
    for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt")):
        v = line.split()
        if v and v[0] in ("q", "qn"):
            step = 5 if v[0] == "q" else 8
            vals = [float(x) for x in v[2:2 + 4 * step]]
            top = max(top, max(vals[1::step]))
    return top


def veil_fracs(item_id, ymin, ymax):
    """For every quad of the mesh file (in file order, like load()): its height share 0..1 and whether the veil covers it (body layer, all four vertices above VEIL_FROM = 0.38 of the length)."""
    out = []
    for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt")):
        v = line.split()
        if not v or v[0] not in ("q", "qn"):
            continue
        step = 5 if v[0] == "q" else 8
        vals = [float(x) for x in v[2:2 + 4 * step]]
        ys = vals[1::step]
        out.append((sum(ys) / 4.0 - ymin) / (ymax - ymin) if int(v[1]) == 0 and (min(ys) - ymin) / (ymax - ymin) >= 0.38 else None)
    return out


def render_dark(m, quads, skin, glowtex, tint, cam, veil=None, t=0.0, k_pulse=1.0):
    """Body (cutout, lit), then the glow pass ADDITIVE on the neutral prism map, then the veil: the blade's own quads additive with the blade skin, one colour per quad."""
    img = np.zeros((SIZE, SIZE, 3), np.float32) + [0.045, 0.05, 0.075]
    zbuf = np.full((SIZE, SIZE), np.inf, np.float32)
    l1 = np.array([0.2, 1.0, -0.7]); l1 /= np.linalg.norm(l1)
    l2 = np.array([-0.2, 1.0, 0.7]); l2 /= np.linalg.norm(l2)
    t_main = np.array(rgb3(tint), np.float32) / 255 * k_pulse

    def faces(layer):
        for idx, (ly, pts, n) in enumerate(quads):
            if ly != layer:
                continue
            P = m.project(pts[:, :3], cam)
            if np.any(P[:, 2] <= 0.01):
                continue
            area = (P[1, 0] - P[0, 0]) * (P[2, 1] - P[0, 1]) - (P[2, 0] - P[0, 0]) * (P[1, 1] - P[0, 1])
            if area > 0:
                continue
            yield idx, P, pts, n

    for idx, P, pts, n in faces(0):
        sh = 0.45 + 0.35 * max(0, n @ l1) + 0.2 * max(0, n @ l2)
        for tri in ((0, 1, 2), (0, 2, 3)):
            m.raster(img, zbuf, P[list(tri)], pts[list(tri), 3:5], 0, sh, skin, None)
    for idx, P, pts, n in faces(2):
        for tri in ((0, 1, 2), (0, 2, 3)):
            m.raster(img, zbuf, P[list(tri)], pts[list(tri), 3:5], 2, 1.0, glowtex, t_main)
    if veil is not None:
        for idx, P, pts, n in faces(0):
            f = veil[idx]
            if f is None:
                continue
            tint_q = np.array(rgb3(veil_rgb(f, t)), np.float32) / 255
            for tri in ((0, 1, 2), (0, 2, 3)):
                m.raster(img, zbuf, P[list(tri)], pts[list(tri), 3:5], 2, 1.0, skin, tint_q)
    return img


def splat_star(img, star, cx, cy, size, rgb, alpha):
    """Additive splat of the star texture (premultiplied white) tinted with rgb at screen position."""
    s = max(2, int(size))
    sp = np.asarray(Image.fromarray(star).resize((s, s), Image.BILINEAR), np.float32) / 255.0
    x0, y0 = int(cx - s / 2), int(cy - s / 2)
    for yy in range(s):
        for xx in range(s):
            X, Y = x0 + xx, y0 + yy
            if 0 <= X < SIZE and 0 <= Y < SIZE:
                img[Y, X] += sp[yy, xx, 3] * np.array(rgb3(rgb), np.float32) / 255.0 * alpha


def sword_frame(m, quads, skin, glowtex, prism, veil, place, ymax, star, t, view, before=False):
    cam = m.view(*view)
    if before:
        img = render_dark(m, quads, skin, glowtex, 0x7FEBFF, cam, k_pulse=0.78)
        return np.clip(img, 0, 1)
    k = pulse(t) * 0.78
    img = render_dark(m, quads, skin, prism, glow_rgb(t), cam, veil=veil, t=t, k_pulse=k)
    ox, oy, ang, sc, ymin = place
    a = math.radians(ang - 90)
    R = np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])
    length = ymax - ymin
    for i in range(8):
        sx, sy, sz, tw = shard(i, 8, t, 0.13)
        mesh_p = np.array([sx * length, sy * length, sz * length])
        item_p = (mesh_p * sc) @ R.T + [ox, oy, 0.5]
        P = m.project(item_p[None, :], cam)[0]
        hh = length * (0.030 + 0.030 * tw) * sc
        kproj = (SIZE / 2) / math.tan(math.radians(25)) / P[2]
        splat_star(img, star, P[0], P[1], hh * 2 * 1.6 * kproj, shard_rgb(i, 8, t), (120 + 135 * tw) / 255.0)
    return np.clip(img, 0, 1)


def do_sword():
    os.makedirs(OUT, exist_ok=True)
    m = load_psw()
    item = "shishou"
    quads, glow = m.load(item)
    d = os.path.join(ASSETS, "textures", "entity", "weapon")
    skin, glowtex = m.tex(os.path.join(d, item + ".png")), m.tex(os.path.join(d, item + "_glow.png"))
    prism = m.tex(os.path.join(d, item + "_prism_glow.png"))
    kk = skin.shape[1] / 128.0
    quads = [(ly, np.concatenate([pts[:, :3], pts[:, 3:5] * kk], axis=1), n) for (ly, pts, n) in quads]
    place = place_info(item)
    ymax = mesh_ymax(item)
    veil = veil_fracs(item, place[4], ymax)
    star = np.asarray(Image.open(os.path.join(d, "ex_singularity_star.png")).convert("RGBA"))
    phases = [0.0, 50.0, 100.0, 150.0]                       # hue phases: red, yellow-green, cyan-blue, magenta (ticks)
    views = [(35, 20), (0, 0)]
    cols = []
    for vi, view in enumerate(views):
        row = [sword_frame(m, quads, skin, glowtex, prism, veil, place, ymax, star, 0.0, view, before=True)]
        for t in phases:
            row.append(sword_frame(m, quads, skin, glowtex, prism, veil, place, ymax, star, t, view))
        cols.append(np.concatenate(row, 1))
    arr = np.concatenate(cols, 0)
    im = Image.fromarray((arr * 255).astype(np.uint8)).convert("RGBA")
    lab = ImageFont.truetype(SANS, 15)
    dr = ImageDraw.Draw(im)
    names = ["2.0.0: one blue glow"] + [f"EX-Singularity, tick {int(t)} (hue {t / GLOW_PERIOD_TICKS:.2f})" for t in phases]
    for i, n in enumerate(names):
        dr.text((i * SIZE + 8, 6), n, font=lab, fill=(230, 232, 240, 255))
    im.save(os.path.join(OUT, "sword_phases.png"))
    print("wrote sword_phases.png")


def main(argv):
    self_check()
    what = argv[0] if argv else "all"
    if what in ("tooltip", "all"):
        do_tooltip()
    if what in ("frame", "all"):
        do_frame()
    if what in ("sword", "all"):
        do_sword()


if __name__ == "__main__":
    main(sys.argv[1:])
