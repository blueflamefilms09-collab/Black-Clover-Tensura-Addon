"""Requip: The Knight (Erza Scarlet): the six weapons the armours put in the hand and the skill icon. Deterministic.

    python3 -B tools/gen_erza_weapons.py                  (then: python3 -B tools/item_preview/preview_weapons.py flame_empress_blade ...)

It builds on tools/gen_weapon_models.py (the Genesis Demon-Slayer's standard): for every weapon it writes
  assets/nusmp/weapon_meshes/<id>.txt                 the in-hand mesh WeaponRenderer draws
  assets/nusmp/textures/entity/weapon/<id>.png        its skin (+ _glow.png, drawn additive and full bright)
  assets/nusmp/textures/item/<id>.png                 the 16x16 inventory icon (the mesh is placed on its diagonal)
  assets/nusmp/models/item/<id>.json, <id>_icon.json  the 3D in-hand model and the flat icon model
and assets/nusmp/textures/skill/erza_requip.png (32x32).
The weapons: flame_empress_blade, lightning_empress_halberd, sea_empress_trident, black_wing_blade, nakagami_edge, purgatory_mace (engravings: item/WeaponEngravings.java).
Model units are pixels: y along the weapon (0 = where the head meets the guard, the grip below 0), x across it, z through it.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_weapon_models as G  # noqa: E402

np, Image = G.np, G.Image
col, fz, nz, hsh, steel = G.col, G.fz, G.nz, G.hsh, G.steel
box, plate, Blade, lathe = G.box, G.plate, G.Blade, G.lathe
metal_paint, wrap_paint = G.metal_paint, G.wrap_paint
ASSETS = G.ASSETS


def rgbv(c):
    return np.array(c, np.float32)


# ====================================================================================================================== the weapons
def flame_empress_blade(m, a):
    """Flame Empress's blade: a broad crimson bastard sword, a gold fuller that glows like an ember seam, a flame-shaped edge, a winged gold guard with two flame prongs, a red gem pommel."""
    L = 54
    st = [(0, -4.4, 0, 4.4), (14, -5.0, 0, 5.0), (30, -4.4, 0, 4.4), (44, -3.4, 0, 3.4), (L - 7, -2.4, 0, 2.4), (L, 0, 0, 0)]

    def paint(x, y, front, e, rd):
        if x is None:
            return col((200, 60, 40)), col((255, 140, 40), 0.7)
        c = steel(x, y, (170, 36, 40), e, (255, 214, 140), rd, (220, 84, 60), 20)
        g = None
        if rd < 0.9 and 3 < y < L - 12:                                   # the fuller: a seam of ember
            c = rgbv((255, 150, 50)) + fz(x, y, 3) * 20
            g = col((255, 140, 40), 0.55 + 0.4 * (hsh(int(y)) > 0.4))
        tongue = 0.5 + 0.5 * math.sin(y * 0.9 + x * 0.7)                  # flame tongues licking up the blade
        if e < 1.6 and tongue > 0.8:
            g = col((255, 170, 60), 0.5)
        if e < 0.8:
            g = col((255, 220, 140), 0.7)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.5, 1.4, paint).build(m, a)
    gold = (222, 170, 56)
    box(m, a, -9.5, -2, -1.5, 9.5, 1.0, 1.5, metal_paint(gold, rivets=True))
    for sx in (-1, 1):                                                    # the winged ends and the two flame prongs
        lo, hi = sorted((sx * 7.5, sx * 11.0))
        box(m, a, lo, -3.0, -1.2, hi, 2.4, 1.2, metal_paint(gold))
        lo, hi = sorted((sx * 3.0, sx * 5.0))
        box(m, a, lo, 1.0, -1.0, hi, 7.0, 1.0, metal_paint((226, 96, 36), gem=(255, 160, 50), glow_rgb=(255, 140, 40)))
    box(m, a, -1.7, -15, -1.6, 1.7, -2, 1.6, wrap_paint((92, 22, 24), (222, 170, 56), diamonds=True))
    box(m, a, -2.6, -19, -2.4, 2.6, -15, 2.4, metal_paint(gold, gem=(255, 90, 40), glow_rgb=(255, 120, 40)))
    return dict(glow=("FF8A2A", 0.08, 0.45, 1.0))


def black_wing_blade(m, a):
    """Black Wing's blade: a curved night-black katana-sabre with a violet edge, a guard of two spread black wings, a violet gem."""
    L = 52
    st = G.katana_stations(L, 5.0, 3.8, 3.6, kissaki=6)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((40, 28, 70)), col((150, 90, 255), 0.5)
        c = steel(x, y, (34, 30, 52), e, (190, 150, 255), rd, (70, 56, 110), 16)
        g = None
        if e < 1.1:
            g = col((160, 100, 255), 0.65)
        if 6 < y < L - 12 and hsh(int(y / 3), int(x)) > 0.9:                 # feather scratches
            c = c * 1.5
            g = col((150, 90, 255), 0.4)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.5, 1.3, paint).build(m, a)
    rows = ["..#.............#..", ".###...........###.", "#####.........#####", ".######.......######", "..#######...#######",
            "...##############..", "....############...", ".....##########...."]
    n = len(rows)
    mask = [[ch == "#" for ch in r.ljust(20, ".")] for r in rows]

    def gpaint(i, j, f):
        c = rgbv((30, 24, 44)) + fz(i, j, 9) * 24
        if j % 2 == 0 and 3 < i < 16:
            c = c * 1.4
        g = col((150, 90, 255), 0.6) if (i + j) % 7 == 0 else None
        return col(np.clip(c, 0, 255)), g
    plate(m, a, mask, -10.0, -4.5, 1.2, gpaint, "xy")
    box(m, a, -1.5, -17, -1.4, 1.5, -4, 1.4, wrap_paint((24, 20, 36), (120, 80, 200), diamonds=True))
    box(m, a, -2.4, -20, -2.2, 2.4, -17, 2.2, metal_paint((50, 40, 76), gem=(170, 110, 255), glow_rgb=(170, 110, 255)))
    return dict(glow=("A66EFF", 0.09, 0.45, 1.0))


def nakagami_edge(m, a):
    """Nakagami's edge: a long straight blade of white-silver with blue anti-magic glyph lines, a gold-and-white cross guard around a blue gem, a white grip."""
    L = 58
    st = G.straight_stations(L, 3.4, 3.0, 9, steps=4, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((200, 220, 250)), col((90, 190, 255), 0.5)
        c = steel(x, y, (222, 230, 244), e, (250, 252, 255), rd, (236, 242, 252), 10)
        g = None
        glyph = (int(y) % 10 in (4, 5)) and abs(x) < 2.4 and 5 < y < L - 12
        diag = abs(((x * 1.2 + y) % 8) - 4) < 0.35 and abs(x) < 2.6 and 5 < y < L - 12
        if glyph or diag:
            c = rgbv((90, 190, 250))
            g = col((90, 200, 255), 0.95)
        if e < 0.8:
            g = col((190, 230, 255), 0.5)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.5, 1.2, paint).build(m, a)
    box(m, a, -10, -2, -1.4, 10, 0.8, 1.4, metal_paint((226, 190, 90), rivets=True))
    for sx in (-1, 1):
        lo, hi = sorted((sx * 7.5, sx * 10.5))
        box(m, a, lo, -3.6, -1.1, hi, 3.0, 1.1, metal_paint((236, 240, 250)))
    box(m, a, -2.0, -3.5, -1.6, 2.0, 0.8, 1.6, metal_paint((226, 190, 90), gem=(90, 190, 255), glow_rgb=(90, 200, 255)))
    box(m, a, -1.5, -17, -1.4, 1.5, -3.5, 1.4, wrap_paint((232, 236, 246), (90, 160, 230), diamonds=True))
    box(m, a, -2.4, -20, -2.2, 2.4, -17, 2.2, metal_paint((226, 190, 90), gem=(90, 190, 255), glow_rgb=(90, 200, 255)))
    return dict(glow=("5AC8FF", 0.08, 0.45, 1.0))


def lightning_empress_halberd(m, a):
    """Lightning Empress's halberd: a long gold-wrapped shaft, a spear head of white-gold with a bolt inlay, a curved axe blade on one side and a hook on the other, bolt glow."""
    def head(x, y, front, e, rd):
        if x is None:
            return col((240, 220, 120)), col((255, 240, 130), 0.7)
        c = steel(x, y, (238, 226, 170), e, (255, 250, 220), rd, (255, 244, 200), 12)
        g = None
        zig = abs(x - 0.9 * math.sin(y * 0.8)) < 0.5 and 2 < y < 20
        if zig:
            c = rgbv((255, 240, 120))
            g = col((255, 240, 110), 1.0)
        if e < 0.8:
            g = col((255, 250, 170), 0.5)
        return col(np.clip(c, 0, 255)), g
    Blade([(0, -2.4, 0, 2.4), (6, -3.2, 0, 3.2), (16, -2.0, 0, 2.0), (24, 0, 0, 0)], 0.5, 1.2, head).build(m, a)
    rows = ["....####", "...#####", "..######", ".#######", "########", "#######.", "######..", "####....", "##......"]
    mask = [[ch == "#" for ch in r[::-1]] for r in rows]          # the axe blade leads at -x (the mesh convention: -x is forward)

    def axe(i, j, f):
        c = rgbv((236, 224, 168)) + fz(i, j, 4) * 20
        g = None
        if i > 5 or j > 6:
            c = rgbv((255, 250, 220))
        if (i + 2 * j) % 6 == 0 and i < 5:
            c = rgbv((255, 240, 110))
            g = col((255, 240, 110), 0.9)
        return col(np.clip(c, 0, 255)), g
    plate(m, a, mask, -10.0, 1.0, 0.7, axe, "xy")
    hook = [[ch == "#" for ch in r[::-1]] for r in ["###", ".##", "..#", "..#"]]
    plate(m, a, hook, 2.0, 2.0, 0.7, lambda i, j, f: (col((236, 224, 168)), None), "xy")
    box(m, a, -2.2, -3.0, -1.6, 2.2, 0.0, 1.6, metal_paint((222, 170, 56), gem=(255, 240, 110), glow_rgb=(255, 240, 110)))

    def shaft(face, i, j, fw, fh):
        c = rgbv((226, 190, 90)) + fz(i, j * 0.4, face) * 20
        if j % 6 in (0, 1):
            c = rgbv((120, 40, 40))
        g = col((255, 240, 110), 0.5) if j % 12 == 3 and face >= 2 else None
        return col(np.clip(c, 0, 255)), g
    box(m, a, -1.0, -60, -1.0, 1.0, -3.0, 1.0, shaft)
    box(m, a, -1.5, -64, -1.5, 1.5, -60, 1.5, metal_paint((222, 170, 56), gem=(255, 240, 110), glow_rgb=(255, 240, 110)))
    return dict(glow=("FFF07A", 0.12, 0.45, 1.0))


def sea_empress_trident(m, a):
    """Sea Empress's trident: a pearl-and-teal trident, three barbed prongs with a water-blue edge glow, a shell-fan guard, a shaft banded with coral-pink."""
    def prong(x, y, front, e, rd):
        if x is None:
            return col((90, 200, 220)), col((120, 240, 255), 0.6)
        c = steel(x, y, (60, 168, 188), e, (200, 250, 255), rd, (130, 214, 226), 14)
        g = col((120, 240, 255), 0.85) if e < 0.9 else None
        if int(y) % 7 == 3 and rd < 0.9:
            g = col((150, 250, 255), 0.7)
        return col(np.clip(c, 0, 255)), g
    for (dx, L, w) in ((0, 22, 2.0), (-5.0, 16, 1.5), (5.0, 16, 1.5)):
        st = [(0, -w, 0, w), (L - 6, -w * 0.85, 0, w * 0.85), (L - 5, -w * 1.7, 0, w * 1.7), (L, 0, 0, 0)]
        Blade(st, 0.35, 0.9, prong, dx=dx).build(m, a)
    box(m, a, -6.5, -2, -1.3, 6.5, 0.2, 1.3, metal_paint((232, 240, 244), rivets=True))
    box(m, a, -6.5, -2, -1.3, -3.6, 2.2, 1.3, metal_paint((232, 240, 244)))
    box(m, a, 3.6, -2, -1.3, 6.5, 2.2, 1.3, metal_paint((232, 240, 244)))
    box(m, a, -2.2, -5, -2.2, 2.2, -2, 2.2, metal_paint((90, 200, 220), gem=(255, 170, 190), glow_rgb=(255, 170, 190)))

    def shaft(face, i, j, fw, fh):
        c = rgbv((40, 120, 140)) + fz(i, j * 0.4, face) * 18
        if j % 9 in (0, 1):
            c = rgbv((246, 150, 170))
        return col(np.clip(c, 0, 255)), None
    box(m, a, -1.0, -58, -1.0, 1.0, -5, 1.0, shaft)
    for yb in (-16, -32, -48):
        box(m, a, -1.4, yb - 1.2, -1.4, 1.4, yb, 1.4, metal_paint((232, 240, 244)))
    box(m, a, -1.8, -62, -1.8, 1.8, -58, 1.8, metal_paint((90, 200, 220), gem=(120, 240, 255), glow_rgb=(120, 240, 255)))
    return dict(glow=("78F0FF", 0.09, 0.5, 1.0))


def purgatory_mace(m, a):
    """Purgatory's mace: a black-iron flanged ball studded with spikes, red hell-cracks glowing through it, a thick haft bound in chain-links and a spiked butt."""
    def iron(k, s, y, th):
        n = nz(k * 3 + s * 3, y * 0.7, 5) * 40 + fz(k + s, y, 6) * 20
        c = rgbv((38, 32, 40)) + n
        g = None
        crack = abs(math.sin((k + s) * 5.0 + y * 0.8)) < 0.10 or abs(math.sin(y * 1.3 + k * 2.1)) < 0.05
        if crack:
            c = rgbv((255, 70, 30))
            g = col((255, 70, 30), 0.95)
        return col(np.clip(c, 0, 255)), g
    r = 9.0
    prof = [(0, 0.0), (0.6, 4.0), (2.0, 7.0), (4.5, 8.8), (9.0, r), (13.5, 8.8), (16.0, 7.0), (17.4, 4.0), (18.0, 0.0)]
    lathe(m, a, [(y, rad) for (y, rad) in prof], 10, iron, base=(0.0, 0.0, 0.0), direction=(0, 1, 0), sw=4, jitter=0.03, salt=3)

    def spike(face, i, j, fw, fh):
        c = rgbv((60, 52, 60)) + fz(i, j, face) * 18
        if j < 2:
            c = rgbv((255, 90, 40))
        return col(np.clip(c, 0, 255)), (col((255, 70, 30), 0.8) if j < 2 else None)
    cy = 9.0
    for (sx, sy, sz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1)):          # the four side spikes
        x0, x1 = sorted((sx * r * 0.9, sx * (r + 5.5)))
        z0, z1 = sorted((sz * r * 0.9, sz * (r + 5.5)))
        box(m, a, x0 - (1.4 if sx == 0 else 0), cy - 1.4, z0 - (1.4 if sz == 0 else 0), x1 + (1.4 if sx == 0 else 0), cy + 1.4, z1 + (1.4 if sz == 0 else 0), spike)
    box(m, a, -1.6, 18.0, -1.6, 1.6, 24.0, 1.6, spike)                              # the top spike
    for (dx, dz) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):                              # four diagonal studs
        box(m, a, dx * 5.0 - 1.2, cy + 4.5, dz * 5.0 - 1.2, dx * 5.0 + 1.2, cy + 7.5, dz * 5.0 + 1.2, spike)

    def haft(face, i, j, fw, fh):
        c = rgbv((54, 46, 54)) + fz(i, j * 0.3, face) * 22
        if j % 4 in (0, 1) and i % 2 == 0:
            c = rgbv((120, 112, 124))                                              # chain links
        return col(np.clip(c, 0, 255)), None
    box(m, a, -1.8, -46, -1.8, 1.8, 0, 1.8, haft)
    box(m, a, -2.6, -3, -2.6, 2.6, 0, 2.6, metal_paint((70, 62, 74), gem=(255, 80, 40), glow_rgb=(255, 80, 40)))
    box(m, a, -2.6, -50, -2.6, 2.6, -46, 2.6, metal_paint((70, 62, 74)))
    box(m, a, -1.0, -57, -1.0, 1.0, -50, 1.0, spike)
    return dict(glow=("FF4A1E", 0.1, 0.5, 1.0))


# id, builder, real length (m), grip fraction along the icon diagonal
WEAPONS = [
    ("flame_empress_blade", flame_empress_blade, 1.25, 0.2),
    ("lightning_empress_halberd", lightning_empress_halberd, 2.1, 0.33),
    ("sea_empress_trident", sea_empress_trident, 2.8, 0.42),
    ("black_wing_blade", black_wing_blade, 1.2, 0.2),
    ("nakagami_edge", nakagami_edge, 1.3, 0.2),
    ("purgatory_mace", purgatory_mace, 1.3, 0.25),
]


# ====================================================================================================================== icons (16 x 16, on the bottom-left to top-right diagonal)
def _diag(px, t0, t1, width, rgb, edge=None):
    """Paints a bar along the diagonal from t0 to t1 (0..1 of the diagonal), `width` pixels wide, with an optional lighter edge row on the upper-left side."""
    r2 = math.sqrt(2.0)
    for y in range(16):
        for x in range(16):
            u = ((x + 0.5 - 0.5) + (15.5 - (y + 0.5))) / 30.0
            s = ((15.5 - (y + 0.5)) - (x + 0.5 - 0.5)) / r2
            if t0 <= u <= t1 and abs(s) <= width / 2:
                px[(x, y)] = edge if (edge is not None and s > width / 2 - 0.9) else rgb


def _icon(kind, c):
    px = {}
    if kind == "sword":
        _diag(px, 0.00, 0.13, 1.6, c["grip"])
        _diag(px, 0.12, 0.30, 2.6, c["grip"])
        _diag(px, 0.30, 0.36, 7.0, c["guard"])
        _diag(px, 0.36, 0.98, 2.8, c["blade"], c["edge"])
        _diag(px, 0.00, 0.05, 2.0, c["gem"])
    elif kind == "halberd":
        _diag(px, 0.00, 0.80, 1.4, c["grip"])
        _diag(px, 0.74, 1.00, 2.8, c["blade"], c["edge"])
        for y in range(16):
            for x in range(16):
                if 9 <= x <= 13 and 2 <= y <= 6 and (x - 9) + (6 - y) <= 4 and (x, y) not in px:
                    px[(x, y)] = c["edge"] if y == 2 else c["blade"]
    elif kind == "trident":
        _diag(px, 0.00, 0.78, 1.4, c["grip"])
        _diag(px, 0.74, 0.80, 6.0, c["guard"])
        _diag(px, 0.78, 1.00, 1.6, c["blade"], c["edge"])
        for t in (0.82, 0.86, 0.90, 0.94, 0.98):
            for off in (-2.5, 2.5):
                u = t
                xx = 0.5 + u * 15.0 + (-off) * 0.7071 * 0.5
                yy = 15.5 - u * 15.0 - (off) * 0.7071 * 0.5
                if 0 <= int(xx) < 16 and 0 <= int(yy) < 16:
                    px[(int(xx), int(yy))] = c["blade"]
    else:                                                                       # mace
        _diag(px, 0.00, 0.78, 1.6, c["grip"])
        for y in range(16):
            for x in range(16):
                cx, cy = 0.5 + 0.84 * 15.0, 15.5 - 0.84 * 15.0
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= 3.1:
                    px[(x, y)] = c["blade"] if d > 1.2 else c["gem"]
                elif d <= 4.3 and ((x + y) % 2 == 0) and ((int(x - cx) in (-4, 4)) or (int(y - cy) in (-4, 4))):
                    px[(x, y)] = c["edge"]
    return px


ICONS = {
    "flame_empress_blade": ("sword", dict(grip=(120, 30, 30), guard=(222, 170, 56), blade=(190, 50, 44), edge=(255, 190, 90), gem=(255, 120, 40))),
    "black_wing_blade": ("sword", dict(grip=(30, 24, 40), guard=(60, 48, 90), blade=(44, 38, 66), edge=(180, 130, 255), gem=(170, 110, 255))),
    "nakagami_edge": ("sword", dict(grip=(230, 234, 246), guard=(226, 190, 90), blade=(214, 224, 244), edge=(255, 255, 255), gem=(90, 190, 255))),
    "lightning_empress_halberd": ("halberd", dict(grip=(226, 190, 90), blade=(238, 226, 170), edge=(255, 250, 200), gem=(255, 240, 110))),
    "sea_empress_trident": ("trident", dict(grip=(40, 120, 140), guard=(232, 240, 244), blade=(90, 200, 220), edge=(200, 250, 255), gem=(120, 240, 255))),
    "purgatory_mace": ("mace", dict(grip=(70, 62, 74), blade=(48, 42, 52), edge=(255, 90, 40), gem=(255, 70, 30))),
}


def write_icon(item_id):
    kind, c = ICONS[item_id]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), rgb in _icon(kind, c).items():
        im.putpixel((x, y), (int(rgb[0]), int(rgb[1]), int(rgb[2]), 255))
    d = os.path.join(ASSETS, "textures", "item")
    os.makedirs(d, exist_ok=True)
    im.save(os.path.join(d, item_id + ".png"))


def write_models(item_id):
    models = os.path.join(ASSETS, "models", "item")
    os.makedirs(models, exist_ok=True)

    def dump(name, data):
        with open(os.path.join(models, name + ".json"), "w", newline="\n") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    dump(item_id + "_icon", {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}})
    src = json.load(open(os.path.join(models, "magebane_longsword.json")))
    src["textures"] = {"particle": "nusmp:item/" + item_id}
    dump(item_id, src)


# ====================================================================================================================== the skill icon
def write_skill_icon():
    S = 32
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 15.0:
                k = 1.0 - d / 15.0
                im.putpixel((x, y), (int(60 + 120 * k), int(10 + 14 * k), int(20 + 26 * k), 255))
            if 14.0 < d <= 15.5:
                im.putpixel((x, y), (232, 186, 70, 255))
    for y in range(5, 27):                                               # a sword
        for x in range(15, 17):
            im.putpixel((x, y), (236, 238, 248, 255))
    for x in range(10, 22):
        for y in range(18, 20):
            im.putpixel((x, y), (232, 186, 70, 255))
    for y in range(22, 27):
        im.putpixel((15, y), (150, 60, 50, 255))
        im.putpixel((16, y), (150, 60, 50, 255))
    for (cx, cy) in ((9, 10), (22, 10), (9, 22), (22, 22)):              # the four glints of the armours changing
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            im.putpixel((cx + dx, cy + dy), (255, 214, 140, 255) if (dx, dy) != (0, 0) else (255, 255, 255, 255))
    d = os.path.join(ASSETS, "textures", "skill")
    os.makedirs(d, exist_ok=True)
    im.save(os.path.join(d, "erza_requip.png"))


def main(only=None):
    write_skill_icon()
    for item_id, fn, real, grip in WEAPONS:
        if only and item_id not in only:
            continue
        write_icon(item_id)
        write_models(item_id)
        G.REAL[item_id] = real
        G.GRIP[item_id] = grip
        mesh, atlas = G.Mesh(), G.Atlas()
        extra = fn(mesh, atlas) or {}
        G.write(item_id, mesh, atlas, extra, False)


if __name__ == "__main__":
    main(sys.argv[1:])
