"""3.58.0: the 54 icons of the Beast Will skills (textures/skill/bw_<key>.png and bw_<key>_unique.png), 32x32, opaque, one per skill, no two alike.
Each beast has a tint (its colour, the same as skill/rules/BeastSkillRules.java) and an emblem drawn as a silhouette on a tinted gradient background.
Unique = plain silver frame, Ultimate = gold frame with a glow behind the emblem.
    python tools/gen_bw_skill_icons.py            # writes the 54 icons
    python tools/gen_bw_skill_icons.py --sheet out.png   # also writes a contact sheet (27 rows: Unique | Ultimate)
Deterministic; refuses to finish if two icons are identical or one is not opaque."""
import hashlib, math, os, sys
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "skill")
S = 256

# key: (tint, emblem)
BEASTS = {
    "dragon": ((0xFF, 0x8A, 0x2A), "dragon"), "shadow_panther": ((0x7A, 0x3C, 0xC8), "panther"), "elderwood_guardian": ((0x4F, 0xA0, 0x4A), "tree"),
    "lion": ((0xE6, 0xB0, 0x30), "lion"), "ridge_wolf": ((0x8F, 0xA0, 0xB8), "wolf"), "crested_boar": ((0xB0, 0x60, 0x38), "boar"),
    "wind_skimmer": ((0x5C, 0xD6, 0xB8), "wings"), "flame_serpent": ((0xE8, 0x50, 0x1C), "flame_snake"), "thunder_fang_chimera": ((0xF2, 0xD8, 0x3A), "bolt_fang"),
    "fenrir": ((0xB8, 0xE4, 0xFF), "soul_wolf"), "jormungandr": ((0x3C, 0x8F, 0x7A), "ouroboros"), "cerberus": ((0xC0, 0x28, 0x28), "three_heads"),
    "minotaur": ((0xA6, 0x73, 0x2E), "bull"), "hydra": ((0x58, 0xC0, 0x40), "hydra"), "kraken": ((0x2A, 0x5F, 0xA8), "kraken"),
    "hafgufa": ((0x4A, 0x8A, 0x96), "island"), "typhon": ((0xA0, 0x50, 0xE0), "storm_giant"), "scylla_charybdis": ((0x5E, 0x7F, 0xB0), "whirlpool"),
    "sphinx": ((0xD8, 0xB0, 0x50), "sphinx"), "pegasus": ((0xF4, 0xF0, 0xD8), "pegasus"), "cu_sith": ((0x2F, 0x7A, 0x52), "hound"),
    "nidhoggr": ((0x5A, 0x2E, 0x6E), "root_dragon"), "yamata_no_orochi": ((0xC0, 0x20, 0x60), "eight_heads"), "kitsune": ((0xF0, 0x8A, 0x3A), "fox"),
    "nue": ((0x4A, 0x4A, 0x6E), "cloud_nue"), "ammit": ((0xD0, 0xA8, 0x30), "ammit"), "apophis": ((0x4B, 0x2A, 0x7A), "night_serpent"),
}


def lt(c, f): return tuple(min(255, int(v + (255 - v) * f)) for v in c)
def dk(c, f): return tuple(int(v * (1 - f)) for v in c)


def ink(c): return dk(c, 0.78) + (255,)


# ------------------------------------------------------------------ emblem pieces (all on a 256 canvas, centre 128,128, drawn in the dark ink colour with a light rim)
def tri(d, pts, fill): d.polygon(pts, fill=fill)


def ear_pair(d, cx, cy, w, h, gap, fill):
    tri(d, [(cx - gap - w, cy), (cx - gap, cy), (cx - gap - w * 0.4, cy - h)], fill)
    tri(d, [(cx + gap + w, cy), (cx + gap, cy), (cx + gap + w * 0.4, cy - h)], fill)


def canine_head(d, cx, cy, sc, fill, eye):
    ear_pair(d, cx, cy - 20 * sc, 20 * sc, 38 * sc, 12 * sc, fill)
    d.ellipse((cx - 36 * sc, cy - 28 * sc, cx + 36 * sc, cy + 26 * sc), fill=fill)
    tri(d, [(cx - 16 * sc, cy + 6 * sc), (cx + 16 * sc, cy + 6 * sc), (cx, cy + 52 * sc)], fill)
    d.ellipse((cx - 22 * sc, cy - 8 * sc, cx - 10 * sc, cy + 2 * sc), fill=eye)
    d.ellipse((cx + 10 * sc, cy - 8 * sc, cx + 22 * sc, cy + 2 * sc), fill=eye)


def cat_head(d, cx, cy, sc, fill, eye, mane=False):
    if mane:
        for k in range(14):
            a = 2 * math.pi * k / 14
            tri(d, [(cx + math.cos(a - 0.2) * 40 * sc, cy + math.sin(a - 0.2) * 40 * sc), (cx + math.cos(a + 0.2) * 40 * sc, cy + math.sin(a + 0.2) * 40 * sc),
                    (cx + math.cos(a) * 66 * sc, cy + math.sin(a) * 66 * sc)], fill)
    ear_pair(d, cx, cy - 24 * sc, 16 * sc, 24 * sc, 22 * sc, fill)
    d.ellipse((cx - 38 * sc, cy - 32 * sc, cx + 38 * sc, cy + 34 * sc), fill=fill)
    d.ellipse((cx - 24 * sc, cy - 6 * sc, cx - 10 * sc, cy + 4 * sc), fill=eye)
    d.ellipse((cx + 10 * sc, cy - 6 * sc, cx + 24 * sc, cy + 4 * sc), fill=eye)


def snake_path(cx, cy, r0, r1, turns, n=60, a0=0.0):
    pts = []
    for i in range(n + 1):
        t = i / n
        a = a0 + t * turns * 2 * math.pi
        r = r0 + (r1 - r0) * t
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    return pts


def thick_line(d, pts, w0, w1, fill):
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        w = w0 + (w1 - w0) * i / max(1, n - 1)
        d.ellipse((x - w, y - w, x + w, y + w), fill=fill)


def snake_head(d, x, y, ang, sc, fill, eye):
    pts = [(x + math.cos(ang) * 26 * sc, y + math.sin(ang) * 26 * sc), (x + math.cos(ang + 2.4) * 22 * sc, y + math.sin(ang + 2.4) * 22 * sc), (x + math.cos(ang - 2.4) * 22 * sc, y + math.sin(ang - 2.4) * 22 * sc)]
    d.polygon(pts, fill=fill)
    d.ellipse((x - 4 * sc + math.cos(ang + 0.9) * 8 * sc, y - 4 * sc + math.sin(ang + 0.9) * 8 * sc, x + 4 * sc + math.cos(ang + 0.9) * 8 * sc, y + 4 * sc + math.sin(ang + 0.9) * 8 * sc), fill=eye)


def neck(d, base, top, bend, w0, w1, fill):
    pts = []
    for i in range(25):
        t = i / 24
        x = base[0] + (top[0] - base[0]) * t + math.sin(t * math.pi) * bend
        y = base[1] + (top[1] - base[1]) * t
        pts.append((x, y))
    thick_line(d, pts, w0, w1, fill)
    return pts[-1], pts[-2]


def bolt(d, cx, cy, sc, fill):
    pts = [(cx + 8 * sc, cy - 70 * sc), (cx - 30 * sc, cy + 6 * sc), (cx - 2 * sc, cy + 6 * sc), (cx - 16 * sc, cy + 70 * sc), (cx + 34 * sc, cy - 14 * sc), (cx + 4 * sc, cy - 14 * sc)]
    d.polygon(pts, fill=fill)


def horns(d, cx, cy, spread, h, w, fill):
    for sgn in (-1, 1):
        pts = [(cx + sgn * spread, cy)]
        for i in range(1, 12):
            t = i / 11
            pts.append((cx + sgn * (spread + 34 * math.sin(t * 1.6)), cy - h * t))
        thick_line(d, pts, w, 2, fill)


def wings(d, cx, cy, sc, fill, n=6):
    for sgn in (-1, 1):
        for k in range(n):
            a = -0.5 + k * 0.28
            x1 = cx + sgn * math.cos(a) * 100 * sc
            y1 = cy - math.sin(a) * 90 * sc + k * 4 * sc
            tri(d, [(cx + sgn * 10 * sc, cy + 10 * sc), (x1, y1), (cx + sgn * (math.cos(a) * 100 * sc - 14 * sc), y1 + 24 * sc)], fill)


def draw_emblem(d, name, c, fill, eye, rim):
    cx = cy = 128
    if name == "wolf":
        canine_head(d, cx, cy + 6, 1.35, fill, eye)
    elif name == "hound":
        canine_head(d, cx, cy + 8, 1.15, fill, eye)
        for k in range(3): d.ellipse((60 + k * 50, 200, 78 + k * 50, 218), fill=rim)
    elif name == "soul_wolf":
        canine_head(d, cx, cy + 10, 1.4, fill, rim)
        for k in range(7):
            a = -math.pi / 2 + (k - 3) * 0.28
            tri(d, [(cx + math.cos(a) * 80, cy + 10 + math.sin(a) * 80), (cx + math.cos(a - 0.08) * 112, cy + 10 + math.sin(a - 0.08) * 112), (cx + math.cos(a + 0.08) * 112, cy + 10 + math.sin(a + 0.08) * 112)], rim)
    elif name == "three_heads":
        for k, off in enumerate((-62, 0, 62)):
            canine_head(d, cx + off, cy + (14 if off else -4), 0.78 if off else 0.95, fill, rim)
    elif name == "lion":
        cat_head(d, cx, cy + 6, 1.25, fill, eye, mane=True)
    elif name == "panther":
        cat_head(d, cx, cy + 8, 1.3, fill, rim)
        d.pieslice((cx - 80, cy - 90, cx + 80, cy + 70), 200, 340, fill=None, outline=rim, width=5)
    elif name == "dragon":
        horns(d, cx, cy - 20, 34, 70, 9, fill)
        d.ellipse((cx - 52, cy - 36, cx + 52, cy + 36), fill=fill)
        tri(d, [(cx - 40, cy + 14), (cx + 40, cy + 14), (cx, cy + 84)], fill)
        d.ellipse((cx - 36, cy - 14, cx - 14, cy + 2), fill=eye); d.ellipse((cx + 14, cy - 14, cx + 36, cy + 2), fill=eye)
        for k in range(4): tri(d, [(cx - 18 + k * 12, cy + 56), (cx - 12 + k * 12, cy + 56), (cx - 15 + k * 12, cy + 70)], rim)
    elif name == "storm_giant":
        bolt(d, cx, cy, 1.5, fill)
        for k, ang in enumerate((-1.2, -0.6, 0.0, 0.6, 1.2)):
            nk = neck(d, (cx, cy + 70), (cx + math.sin(ang) * 100, cy - 70 + abs(ang) * 20), 12, 9, 6, fill)
            snake_head(d, nk[0][0], nk[0][1], -math.pi / 2 + ang * 0.7, 0.55, fill, rim)
    elif name == "boar":
        d.ellipse((cx - 62, cy - 40, cx + 62, cy + 50), fill=fill)
        for sgn in (-1, 1):
            tri(d, [(cx + sgn * 50, cy + 20), (cx + sgn * 72, cy - 40), (cx + sgn * 36, cy + 4)], rim)
            tri(d, [(cx + sgn * 30, cy - 38), (cx + sgn * 56, cy - 70), (cx + sgn * 56, cy - 30)], fill)
        d.ellipse((cx - 22, cy + 6, cx + 22, cy + 40), fill=dk(fill[:3], -0.0) + (255,)); d.ellipse((cx - 12, cy + 16, cx - 4, cy + 28), fill=eye); d.ellipse((cx + 4, cy + 16, cx + 12, cy + 28), fill=eye)
        for k in range(5): tri(d, [(cx - 40 + k * 20, cy - 40), (cx - 30 + k * 20, cy - 40), (cx - 35 + k * 20, cy - 66 - (k % 2) * 12)], fill)
    elif name == "bull":
        horns(d, cx, cy - 6, 40, 76, 11, rim)
        d.ellipse((cx - 50, cy - 40, cx + 50, cy + 40), fill=fill)
        tri(d, [(cx - 30, cy + 10), (cx + 30, cy + 10), (cx, cy + 70)], fill)
        d.ellipse((cx - 8, cy + 36, cx + 8, cy + 48), fill=eye)
        d.ellipse((cx - 36, cy - 14, cx - 18, cy), fill=eye); d.ellipse((cx + 18, cy - 14, cx + 36, cy), fill=eye)
        d.ellipse((cx - 36, cy + 30, cx - 28, cy + 38), outline=rim, width=3); d.arc((cx - 22, cy + 28, cx + 22, cy + 70), 0, 180, fill=rim, width=6)
    elif name == "tree":
        d.polygon([(cx - 14, cy + 90), (cx + 14, cy + 90), (cx + 8, cy - 10), (cx - 8, cy - 10)], fill=fill)
        for (ox, oy, r) in ((0, -50, 52), (-44, -16, 38), (44, -16, 38), (0, -10, 34)): d.ellipse((cx + ox - r, cy + oy - r, cx + ox + r, cy + oy + r), fill=fill)
        d.ellipse((cx - 14, cy + 4, cx - 4, cy + 16), fill=eye); d.ellipse((cx + 4, cy + 4, cx + 14, cy + 16), fill=eye)
        for sgn in (-1, 1): thick_line(d, [(cx + sgn * 10, cy + 90), (cx + sgn * 40, cy + 98), (cx + sgn * 70, cy + 90)], 6, 2, fill)
    elif name == "wings":
        wings(d, cx, cy, 1.0, fill); d.ellipse((cx - 14, cy - 24, cx + 14, cy + 40), fill=rim)
    elif name == "pegasus":
        wings(d, cx + 6, cy - 6, 0.9, rim, 5)
        d.polygon([(cx - 60, cy + 70), (cx - 34, cy - 10), (cx - 24, cy - 56), (cx + 6, cy - 70), (cx + 34, cy - 40), (cx + 66, cy + 10), (cx + 58, cy + 26), (cx + 24, cy + 4), (cx + 12, cy + 70)], fill=fill)
        d.ellipse((cx - 2, cy - 48, cx + 12, cy - 36), fill=eye)
        for k in range(5): tri(d, [(cx - 30, cy - 40 + k * 18), (cx - 56 - (k % 2) * 8, cy - 30 + k * 18), (cx - 28, cy - 24 + k * 18)], fill)
    elif name == "flame_snake":
        thick_line(d, snake_path(cx, cy + 10, 6, 76, 2.2, 90, 0.4), 6, 14, fill)
        snake_head(d, *snake_path(cx, cy + 10, 6, 76, 2.2, 90, 0.4)[-1], 0.4 + 2.2 * 2 * math.pi + math.pi / 2, 1.1, fill, rim)
        for k in range(5): tri(d, [(cx - 50 + k * 25, cy - 70), (cx - 38 + k * 25, cy - 70), (cx - 44 + k * 25, cy - 104 - (k % 2) * 14)], rim)
    elif name == "ouroboros":
        d.ellipse((cx - 84, cy - 84, cx + 84, cy + 84), outline=fill, width=26)
        snake_head(d, cx + 70, cy - 40, 0.5, 1.2, fill, rim)
        d.ellipse((cx - 36, cy - 36, cx + 36, cy + 36), fill=rim)
        d.ellipse((cx - 20, cy - 20, cx + 20, cy + 20), fill=fill)
    elif name == "hydra":
        for k, ang in enumerate((-1.1, -0.55, 0.0, 0.55, 1.1)):
            nk = neck(d, (cx, cy + 84), (cx + math.sin(ang) * 96, cy - 56 + abs(ang) * 34), 18 * (1 if k % 2 else -1), 10, 7, fill)
            snake_head(d, nk[0][0], nk[0][1], -math.pi / 2 + ang * 0.8, 0.62, fill, rim)
        d.ellipse((cx - 30, cy + 60, cx + 30, cy + 110), fill=fill)
    elif name == "eight_heads":
        for k in range(8):
            ang = -1.35 + k * 0.386
            nk = neck(d, (cx, cy + 90), (cx + math.sin(ang) * 112, cy - 62 + abs(ang) * 30), 14 * (1 if k % 2 else -1), 8, 5, fill)
            snake_head(d, nk[0][0], nk[0][1], -math.pi / 2 + ang * 0.8, 0.5, fill, rim)
        d.ellipse((cx - 36, cy + 70, cx + 36, cy + 118), fill=fill)
    elif name == "kraken":
        d.ellipse((cx - 50, cy - 80, cx + 50, cy + 10), fill=fill)
        d.ellipse((cx - 30, cy - 36, cx - 12, cy - 18), fill=eye); d.ellipse((cx + 12, cy - 36, cx + 30, cy - 18), fill=eye)
        for k in range(7):
            x0 = cx - 54 + k * 18
            pts = [(x0 + math.sin(t / 3.0 + k) * 14 * (1 + t / 12.0), cy + 4 + t * 9) for t in range(14)]
            thick_line(d, pts, 8, 2, fill)
    elif name == "island":
        d.pieslice((cx - 110, cy - 20, cx + 110, cy + 190), 180, 360, fill=fill)
        for (ox, h) in ((-50, 50), (-10, 74), (36, 56)): tri(d, [(cx + ox - 16, cy + 10), (cx + ox + 16, cy + 10), (cx + ox, cy + 10 - h)], rim)
        d.ellipse((cx + 56, cy + 24, cx + 76, cy + 44), fill=eye)
        d.arc((cx - 100, cy + 40, cx + 100, cy + 120), 20, 160, fill=rim, width=6)
    elif name == "whirlpool":
        thick_line(d, snake_path(cx, cy + 20, 4, 86, 3.0, 120), 5, 10, fill)
        d.polygon([(cx + 40, cy - 100), (cx + 96, cy - 20), (cx + 70, cy + 10), (cx + 30, cy - 40)], fill=fill)
        for k in range(3): d.ellipse((cx + 28 + k * 18, cy - 112 - (k % 2) * 8, cx + 44 + k * 18, cy - 96 - (k % 2) * 8), fill=rim)
    elif name == "sphinx":
        tri(d, [(cx - 100, cy + 70), (cx + 100, cy + 70), (cx, cy - 80)], fill)
        d.ellipse((cx - 38, cy - 28, cx + 38, cy + 28), fill=rim); d.ellipse((cx - 14, cy - 28, cx + 14, cy + 28), fill=fill); d.ellipse((cx - 6, cy - 10, cx + 6, cy + 10), fill=eye)
        d.line([(cx - 60, cy + 70), (cx + 60, cy + 70)], fill=rim, width=5)
    elif name == "ammit":
        d.polygon([(cx - 90, cy + 10), (cx + 80, cy - 40), (cx + 100, cy - 16), (cx + 30, cy + 8), (cx + 100, cy + 30), (cx + 70, cy + 54), (cx - 90, cy + 40)], fill=fill)
        for k in range(6): tri(d, [(cx - 40 + k * 18, cy + 4), (cx - 28 + k * 18, cy + 4), (cx - 34 + k * 18, cy + 22)], rim)
        d.ellipse((cx + 24, cy - 30, cx + 40, cy - 14), fill=eye)
        d.line([(cx, cy - 100), (cx, cy - 56)], fill=rim, width=6); d.arc((cx - 40, cy - 82, cx + 40, cy - 56), 0, 180, fill=rim, width=6)
    elif name == "fox":
        for k in range(9):
            a = -math.pi / 2 + (k - 4) * 0.34
            pts = [(cx + math.cos(a) * 30, cy + 60 + math.sin(a) * 30)] + [(cx + math.cos(a) * (30 + t * 11) + math.sin(t / 2.0) * 6, cy + 60 + math.sin(a) * (30 + t * 11)) for t in range(1, 8)]
            thick_line(d, pts, 7, 2, rim if k == 4 else fill)
        d.ellipse((cx - 36, cy - 8, cx + 36, cy + 52), fill=fill)
        ear_pair(d, cx, cy - 4, 18, 44, 10, fill)
        tri(d, [(cx - 14, cy + 28), (cx + 14, cy + 28), (cx, cy + 68)], fill)
        d.ellipse((cx - 18, cy + 8, cx - 8, cy + 16), fill=eye); d.ellipse((cx + 8, cy + 8, cx + 18, cy + 16), fill=eye)
    elif name == "cloud_nue":
        for (ox, oy, r) in ((-46, 6, 40), (0, -22, 52), (48, 6, 40), (0, 14, 44)): d.ellipse((cx + ox - r, cy + oy - r, cx + ox + r, cy + oy + r), fill=fill)
        thick_line(d, [(cx + 50 + t * 6, cy + 40 + math.sin(t / 2.0) * 14) for t in range(14)], 8, 2, rim)
        d.ellipse((cx - 22, cy - 26, cx - 8, cy - 12), fill=rim); d.ellipse((cx + 8, cy - 26, cx + 22, cy - 12), fill=rim)
        bolt(d, cx - 4, cy + 18, 0.45, rim)
    elif name == "bolt_fang":
        bolt(d, cx, cy, 1.3, fill)
        tri(d, [(cx - 90, cy - 70), (cx - 62, cy - 70), (cx - 76, cy - 20)], rim); tri(d, [(cx + 90, cy - 70), (cx + 62, cy - 70), (cx + 76, cy - 20)], rim)
    elif name == "root_dragon":
        for k, ox in enumerate((-70, -34, 0, 34, 70)): thick_line(d, [(cx + ox * (1 + t / 20.0) + math.sin(t / 2.0 + k) * 8, cy + 20 + t * 5) for t in range(18)], 9, 2, fill)
        d.polygon([(cx - 12, cy + 20), (cx + 12, cy + 20), (cx + 6, cy - 40), (cx - 6, cy - 40)], fill=fill)
        horns(d, cx, cy - 56, 18, 40, 7, rim)
        d.ellipse((cx - 30, cy - 70, cx + 30, cy - 20), fill=fill); d.ellipse((cx - 16, cy - 54, cx - 6, cy - 44), fill=eye); d.ellipse((cx + 6, cy - 54, cx + 16, cy - 44), fill=eye)
    elif name == "night_serpent":
        d.ellipse((cx - 56, cy - 100, cx + 56, cy + 12), fill=rim)
        d.ellipse((cx - 48, cy - 92, cx + 48, cy + 4), fill=dk(fill[:3], 0.0) + (255,))
        d.ellipse((cx - 36, cy - 78, cx + 60, cy + 18), fill=dk(rim[:3], 0.55) + (255,))
        thick_line(d, snake_path(cx, cy + 40, 6, 62, 1.8, 80, 0.3), 6, 14, fill)
        snake_head(d, *snake_path(cx, cy + 40, 6, 62, 1.8, 80, 0.3)[-1], 0.3 + 1.8 * 2 * math.pi + math.pi / 2, 1.0, fill, rim)


def icon(tint, emblem, ultimate, idx):
    img = Image.new("RGBA", (S, S), (10, 10, 18, 255))
    d = ImageDraw.Draw(img)
    top, bot = lt(tint, 0.35), dk(tint, 0.62)
    for y in range(S):
        t = y / S
        d.line([(0, y), (S, y)], fill=tuple(int(top[i] * (1 - t) + bot[i] * t) for i in range(3)) + (255,))
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((34, 34, S - 34, S - 34), fill=lt(tint, 0.6) + ((190 if ultimate else 70),))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(22)))
    # emblem: a dark silhouette with a light rim (the rim is the same silhouette drawn a little bigger underneath)
    rim = lt(tint, 0.75) + (255,)
    eye = lt(tint, 0.85) + (255,)
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw_emblem(ImageDraw.Draw(layer), emblem, tint, ink(tint), eye, rim)
    edge = layer.filter(ImageFilter.MaxFilter(9))
    rimlayer = Image.new("RGBA", (S, S), lt(tint, 0.7) + (0,))
    rimlayer.putalpha(edge.getchannel("A"))
    img.alpha_composite(rimlayer)
    img.alpha_composite(layer)
    d = ImageDraw.Draw(img)
    if ultimate:
        frame, light = (232, 190, 70), (255, 236, 150)
        d.rectangle((0, 0, S - 1, S - 1), outline=frame + (255,), width=16)
        d.rectangle((16, 16, S - 17, S - 17), outline=light + (255,), width=4)
        for (x, y) in ((6, 6), (S - 7, 6), (6, S - 7), (S - 7, S - 7)): d.ellipse((x - 9, y - 9, x + 9, y + 9), fill=light + (255,))
    else:
        d.rectangle((0, 0, S - 1, S - 1), outline=(150, 156, 170, 255), width=12)
        d.rectangle((12, 12, S - 13, S - 13), outline=(20, 20, 28, 255), width=3)
    for b in range(6):                                    # per-icon tag on the frame so two icons can never be identical
        if (idx >> b) & 1: d.rectangle((24 + b * 34, 2, 38 + b * 34, 8), fill=(255, 255, 255, 255))
    return img.convert("RGB").resize((32, 32), Image.LANCZOS).convert("RGBA")


def main():
    os.makedirs(OUT, exist_ok=True)
    seen, made = {}, []
    for idx, (key, (tint, emblem)) in enumerate(BEASTS.items()):
        for ult, name in ((False, "bw_%s_unique" % key), (True, "bw_%s" % key)):
            im = icon(tint, emblem, ult, idx * 2 + (1 if ult else 0) + 1)
            h = hashlib.md5(im.tobytes()).hexdigest()
            assert h not in seen, name + " is identical to " + seen[h]
            assert all(p[3] == 255 for p in im.getdata()), name + " is not opaque"
            seen[h] = name
            im.save(os.path.join(OUT, name + ".png"))
            made.append((key, ult, im))
    print(len(seen), "icons written")
    if "--sheet" in sys.argv:
        path = sys.argv[sys.argv.index("--sheet") + 1]
        cell, pad, cols = 96, 6, 6          # 54 icons, 6 per row; each icon is shown at 3x
        rows = (len(made) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * (cell + pad) + pad, rows * (cell + pad + 14) + pad), (24, 24, 30))
        sd = ImageDraw.Draw(sheet)
        for n, (key, ult, im) in enumerate(made):
            x, y = pad + (n % cols) * (cell + pad), pad + (n // cols) * (cell + pad + 14)
            sheet.paste(im.convert("RGB").resize((cell, cell), Image.NEAREST), (x, y))
            sd.text((x, y + cell + 1), ("U " if ult else "u ") + key[:14], fill=(220, 220, 230))
        sheet.save(path)
        print("sheet", path)


if __name__ == "__main__":
    main()
