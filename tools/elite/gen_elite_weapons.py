"""elite/items: the 3D in-hand models of the six elite weapons, built to the Genesis Demon-Slayer's standard with the tools of tools/gen_weapon_models.py.

    python3 -B tools/elite/gen_items_art.py weapons          (the 16x16 icons first: each mesh is placed on its icon's diagonal)
    python3 -B tools/elite/gen_elite_weapons.py [ids...]     (meshes + skins + glow skins + the model jsons + the weapon_classes entries)
    python3 -B tools/item_preview/preview_weapons.py astral_edge void_edge fenrir_sword runic_sword_renewed runic_sword_ancient runic_sword_mythical

The module imports gen_weapon_models (its Atlas, Mesh, Blade, box, plate, lathe, paints and its write()) and registers six more specs; the older weapons are NOT regenerated.
Model units are pixels: y runs along the weapon (0 = where the blade meets the guard, the grip below 0), x across it, z through it.

  Astral Edge     a slim star-metal longsword: a pale blade scattered with a constellation (the stars glow, faint lines join them), a crescent guard, a midnight grip, a star pommel
  Void Edge       a black glass blade with a notched back edge and violet cracks; the crimson-violet dimensional void shows through four fractures (layer 1); a broken-ring guard
  Fenrir Sword    a broad ice-silver greatsword, fang notches along the back, two wolf heads as quillons biting the blade, a fur-wrapped grip, a fang pommel
  Runic Swords    one bastard sword at three ages: Renewed (mended steel, bronze, 1 rune lit), Ancient (dark gold inlay, 2 runes lit), Mythical (violet crystal, 3 runes lit, a winged crown guard)
"""
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
sys.path.insert(0, HERE)
import gen_weapon_models as g  # noqa: E402
from art_lib import ASSETS, ROOT  # noqa: E402

col, shade, nz, fz, hsh = g.col, g.shade, g.nz, g.fz, g.hsh
box, plate, Blade, straight_stations, wrap_paint, metal_paint, steel = g.box, g.plate, g.Blade, g.straight_stations, g.wrap_paint, g.metal_paint, g.steel
lerp = g.lerp


# ================================================================ small helpers
def smile_mask(w, rise, thick, hub=3):
    """A crescent (a smile): columns -w/2..w/2, the arc rises by `rise` at the ends, `thick` rows thick, with a hub in the middle that is `hub` rows taller."""
    h = rise + thick + hub
    mk = np.zeros((h, w), bool)
    for i in range(w):
        x = (i + 0.5 - w / 2) / (w / 2)
        top = rise * (abs(x) ** 1.7)                          # distance of the arc above the bottom of the hub
        lo = int(round(top))
        hi = lo + thick
        extra = hub if abs(x) < 0.22 else 0
        for yy in range(lo, hi + extra):
            mk[h - 1 - yy, i] = True
    return mk


def star_mask(r, points=5, inner=0.45):
    n = int(2 * r + 3)
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    c = n / 2
    ang = np.arctan2(yy - c, xx - c) + math.pi / 2
    rad = np.hypot(xx - c, yy - c)
    k = (ang % (2 * math.pi / points)) / (2 * math.pi / points)
    edge = r * (inner + (1 - inner) * np.abs(1 - 2 * k) ** 0.8)
    return rad <= edge


def seg_dist(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / max(1e-9, dx * dx + dy * dy)))
    return math.hypot(px - ax - t * dx, py - ay - t * dy)


GLYPHS = [
    ["..#..", ".###.", "#.#.#", "..#..", "#.#.#", ".###.", "..#.."],
    ["#...#", ".#.#.", "..#..", "###.#", "..#..", ".#.#.", "#...#"],
    ["#####", "#...#", "#.#.#", "..#..", "#.#.#", "#...#", "#####"],
    ["..#..", "..#..", "#####", "..#..", ".###.", "#.#.#", "..#.."],
]


def glyph_hit(x, y, cx, cy, bmp):
    """Is the blade point (x, y) inside the glyph bitmap whose centre is (cx, cy)? One bitmap pixel = 1 model unit."""
    h, w = len(bmp), len(bmp[0])
    i = int(math.floor(x - cx + w / 2))
    j = int(math.floor(cy + h / 2 - y))
    return 0 <= i < w and 0 <= j < h and bmp[j][i] == "#"


def scale_mask(mk, f):
    """A mask resampled by the factor f (nearest neighbour), so a hand-drawn shape can be drawn at a size that fits its weapon."""
    h, w = mk.shape
    nh, nw = max(1, int(round(h * f))), max(1, int(round(w * f)))
    ys = np.minimum(h - 1, (np.arange(nh) / f).astype(int))
    xs = np.minimum(w - 1, (np.arange(nw) / f).astype(int))
    return mk[np.ix_(ys, xs)]


def mirror_rows(rows):
    return [r + r[::-1] for r in rows]


# ================================================================ Astral Edge
def astral_edge(m, a):
    """Astral Edge: star-metal, a constellation on each face, a crescent guard, a midnight grip and a star pommel."""
    L = 56
    st = straight_stations(L, 3.4, 3.0, 10, steps=5, ridge=0)
    stars = [(-1.6, 7), (1.3, 15), (-0.9, 22), (1.7, 30), (-1.3, 38), (0.6, 45), (-0.4, 50)]

    def paint(x, y, front, e, rd):
        if x is None:
            return col((222, 206, 150)), col((190, 215, 255), 0.25)
        k = y / L
        base = np.array((248, 238, 206), np.float32) * (1 - k) + np.array((255, 232, 170), np.float32) * k
        c = base + nz(x, y, 3) * 14 + fz(x, y, 4) * 6
        gl = None
        if abs(x) < 0.55:
            c = c * 0.93 + np.array((10, 6, -6), np.float32)
        pts = [((-sx if front else sx), sy) for (sx, sy) in stars]
        for (sx, sy) in pts:
            if math.hypot(x - sx, y - sy) < 0.95:
                c = np.array((255, 255, 255), np.float32)
                gl = col((200, 225, 255), 1.0)
        if gl is None:
            for (p, q) in zip(pts, pts[1:]):
                if seg_dist(x, y, p[0], p[1], q[0], q[1]) < 0.28:
                    c = c * 0.7 + np.array((60, 80, 110), np.float32)
                    gl = col((150, 190, 255), 0.4)
        if e < 0.9:
            c = np.array((255, 255, 252), np.float32)
            gl = col((255, 250, 220), 0.3)
        return col(np.clip(c, 0, 255)), gl
    Blade(st, 0.7, 1.45, paint).build(m, a)
    gold = (212, 168, 66)
    mk = smile_mask(34, 7, 3, hub=2)
    h = mk.shape[0]

    def gp(i, j, front):
        c = np.array(gold, np.float32) * (0.85 + 0.3 * (1 - j / h)) + fz(i, j, 8) * 18
        gl = None
        if abs(i - 17) <= 1 and j >= h - 4:
            c = np.array((110, 190, 255), np.float32)
            gl = col((150, 215, 255), 1.0)
        return col(np.clip(c, 0, 255)), gl
    plate(m, a, mk, -17, -2.6, 0.9, gp, "xy", 0.0)
    box(m, a, -3.2, -2.8, -1.6, 3.2, 0.9, 1.6, metal_paint(gold, gem=(110, 190, 255), glow_rgb=(150, 215, 255)))
    box(m, a, -1.5, -15, -1.4, 1.5, -2.8, 1.4, wrap_paint((26, 32, 90), (210, 218, 240), diamonds=True))
    box(m, a, -2.3, -17.4, -2.0, 2.3, -15, 2.0, metal_paint(gold))
    smk = star_mask(4.6)
    n = smk.shape[0]

    def sp(i, j, front):
        r = math.hypot(i + 0.5 - n / 2, j + 0.5 - n / 2)
        c = np.array((150, 200, 255), np.float32) * (1.15 - r / 7) if r < 5 else np.array(gold, np.float32)
        return col(np.clip(c, 0, 255)), col((190, 225, 255), 0.9)
    plate(m, a, smk, -n / 2, -17.4 - n, 0.9, sp, "xy", 0.0)
    return dict(glow=("BFD8FF", 0.10, 0.42, 1.0))


# ================================================================ Void Edge
def void_edge(m, a):
    """Void Edge: black glass, a notched back edge, violet cracks; four fractures show the void (layer 1)."""
    L = 60
    # the cutting edge (left, -x) is straight; the back (right, +x) is cut into saw teeth
    st = [(0.0, -4.0, 0.0, 4.2)]
    y = 0.0
    k = 0
    while y < L - 12:
        y += 4.0
        st.append((y, -4.0 + 0.2 * (y / L), 0.0, 3.4 if k % 2 == 0 else 4.4))
        k += 1
    st.append((L - 3, -3.2, 0.0, 3.0))
    st.append((L, 0.0, 0.0, 0.0))
    cracks = [((-1.0, 6), (0.8, 12), (-0.4, 19), (1.2, 27)), ((1.5, 30), (0.2, 36), (1.6, 44), (0.0, 50))]

    def paint(x, y, front, e, rd):
        if x is None:
            return col((22, 16, 36)), col((150, 80, 255), 0.4)
        c = np.array((24, 20, 38), np.float32) + nz(x, y, 6) * 16 + fz(x, y, 2) * 8
        gl = None
        c += np.array((10, 4, 22), np.float32) * (0.5 + 0.5 * math.sin(y * 0.2 + (1 if front else 3)))
        for ch in cracks:
            pts = [((-px if front else px), py) for (px, py) in ch]
            for (p, q) in zip(pts, pts[1:]):
                d = seg_dist(x, y, p[0], p[1], q[0], q[1])
                if d < 0.38:
                    c = np.array((190, 120, 255), np.float32)
                    gl = col((160, 90, 255), 1.0)
                elif d < 0.9 and gl is None:
                    c = c * 0.8 + np.array((40, 10, 70), np.float32)
                    gl = col((120, 50, 220), 0.3)
        if e < 0.9:
            c = np.array((170, 120, 240), np.float32)
            gl = col((150, 90, 255), 0.6)
        return col(np.clip(c, 0, 255)), gl
    voids = []
    for (vy0, vy1) in ((8, 13), (22, 27), (36, 41), (48, 52)):
        for front in (True, False):
            voids.append((-1.3, vy0, 1.0, vy1, front))
    Blade(st, 0.8, 1.5, paint).build(m, a, voids)
    ring = g.disc_mask(7.5, inner=4.4)
    ring = ring.copy()
    ring[:, ring.shape[1] // 2 + 4:] = ring[:, ring.shape[1] // 2 + 4:] & (np.arange(ring.shape[0])[:, None] > ring.shape[0] // 2)          # a broken ring: its upper right is gone
    n = ring.shape[0]

    def rp(i, j, front):
        c = np.array((54, 38, 82), np.float32) * (1.4 if (i + j) % 4 == 0 else 1.0) + fz(i, j, 11) * 16
        return col(np.clip(c, 0, 255)), col((140, 70, 230), 0.5) if (i * 3 + j * 5) % 9 == 0 else None
    plate(m, a, ring, -n / 2, -2.6 - n / 2 + 1.2, 0.8, rp, "xy", 0.0)
    box(m, a, -7.5, -2.4, -1.5, 7.5, 0.4, 1.5, metal_paint((38, 28, 58), rivets=True))
    box(m, a, -1.4, -15, -1.3, 1.4, -2.4, 1.3, wrap_paint((20, 14, 30), (120, 70, 190), diamonds=True))
    box(m, a, -2.4, -18, -2.1, 2.4, -15, 2.1, metal_paint((46, 32, 70), gem=(190, 100, 255), glow_rgb=(200, 120, 255)))
    return dict(glow=("A66BFF", 0.09, 0.35, 1.0))


# ================================================================ Fenrir Sword
WOLF = [                      # a wolf's head in profile facing +x (the quillon points outward, the open jaws bite): ear, brow, long muzzle, a gap for the mouth, a lower jaw
    "..##............",
    ".####...........",
    ".#####..........",
    "########........",
    "###########.....",
    "#############...",
    "###############.",
    ".##############.",
    ".########....###",
    "..######...####.",
    "...####.........",
]


def fenrir_sword(m, a):
    """Fenrir Sword: a broad ice-silver blade with fang notches along its back, two wolf heads for quillons, fur-wrapped grip, a fang pommel."""
    L = 66
    st = [(0.0, -6.2, 0.0, 6.2)]
    y = 0.0
    k = 0
    while y < L - 14:
        y += 4.5
        st.append((y, -6.2 + 0.4 * (y / L), 0.0, 4.8 if k % 2 == 0 else 6.0))        # fang teeth on the back (right) edge
        k += 1
    st.append((L - 13, -5.0, 0.0, 4.4))
    st.append((L, 0.0, 0.0, 0.0))

    def paint(x, y, front, e, rd):
        if x is None:
            return col((156, 178, 206)), None
        c = steel(x, y, (190, 208, 230), e, (250, 253, 255), rd, (150, 176, 208), 22, 5)
        gl = None
        if abs(x) < 1.0 and 4 < y < L - 10:
            c = np.array((120, 146, 182), np.float32) + nz(x, y, 2) * 8                      # the fuller
            if int(y / 6) % 2 == 0 and abs(x) < 0.55:
                c = np.array((150, 230, 255), np.float32)
                gl = col((130, 225, 255), 0.9)
        if e < 0.9:
            c = np.array((255, 255, 255), np.float32)
        return col(np.clip(c, 0, 255)), gl
    Blade(st, 0.8, 1.7, paint).build(m, a)
    mk = scale_mask(g.ascii_mask(WOLF), 1.5)
    h, w = mk.shape

    def wp(i, j, front):
        c = np.array((98, 106, 126), np.float32) * (1.0 + 0.35 * (1 - j / h)) + fz(i, j, 3) * 20
        gl = None
        if (i, j) in ((9, 5), (10, 5), (9, 6), (10, 6)):
            c = np.array((170, 235, 255), np.float32)
            gl = col((150, 230, 255), 1.0)
        if j > 10 and i > 12 and (i + j) % 3 == 0:
            c = np.array((236, 240, 248), np.float32)                                       # teeth
        return col(np.clip(c, 0, 255)), gl
    plate(m, a, mk, 3.5, -5.6, 1.1, wp, "xy", 0.0)                                           # right quillon: the head looks +x
    plate(m, a, mk[:, ::-1], -3.5 - w, -5.6, 1.1, lambda i, j, f: wp(w - 1 - i, j, f), "xy", 0.0)
    box(m, a, -4.0, -3.0, -1.7, 4.0, 1.4, 1.7, metal_paint((84, 90, 112), rivets=True, gem=(150, 230, 255), glow_rgb=(150, 230, 255)))
    box(m, a, -1.8, -22, -1.8, 1.8, -3.0, 1.8, wrap_paint((94, 66, 44), (150, 110, 80), diamonds=True))
    box(m, a, -2.6, -24.5, -2.6, 2.6, -22, 2.6, metal_paint((84, 90, 112)))
    fang = ["..##..", ".####.", ".####.", "..##..", "..##..", "...#.."]
    fk = g.ascii_mask(fang)
    plate(m, a, fk, -3, -24.5 - 6, 0.9, lambda i, j, f: (col((240, 244, 250) if j < 4 else (196, 206, 224)), None), "xy", 0.0)
    return dict(glow=("96E1FF", 0.09, 0.4, 1.0))


# ================================================================ Runic Swords
TIERS = {
    1: dict(L=50, hw=3.4, top=3.0, tip=9, steel=((176, 186, 200), (222, 228, 236)), trim=(176, 118, 60), rune=(127, 232, 255), runes=1, glyph_y=[24], style="mended",
            grip=(70, 48, 34), gem=(127, 232, 255)),
    2: dict(L=56, hw=3.8, top=3.4, tip=10, steel=((86, 70, 44), (140, 114, 66)), trim=(222, 178, 70), rune=(255, 200, 87), runes=2, glyph_y=[20, 36], style="ancient",
            grip=(36, 30, 24), gem=(255, 200, 87)),
    3: dict(L=64, hw=4.6, top=4.0, tip=12, steel=((190, 170, 236), (246, 240, 255)), trim=(236, 200, 90), rune=(231, 184, 255), runes=3, glyph_y=[17, 32, 47], style="crystal",
            grip=(60, 40, 110), gem=(231, 184, 255)),
}


def runic(tier):
    t = TIERS[tier]

    def build(m, a):
        L = t["L"]
        st = straight_stations(L, t["hw"], t["top"], t["tip"], steps=5, ridge=0)
        lo, hi = t["steel"]
        rc = t["rune"]
        slots = t["glyph_y"]
        total_slots = len(slots)

        def paint(x, y, front, e, rd):
            if x is None:
                return col(lo), col(rc, 0.3)
            k = y / L
            c = np.array(lo, np.float32) * (1 - k * 0.4) + np.array(hi, np.float32) * (k * 0.4) + nz(x, y, 4) * 16 + fz(x, y, 6) * 8
            gl = None
            if t["style"] == "mended" and 14 < y < 21 and abs(x) < 3:                         # a patch of newer steel over an old break
                c = np.array(hi, np.float32) + fz(x, y, 12) * 10
                if abs(y - 17.5) < 0.5:
                    c = c * 0.7
            if t["style"] == "ancient" and (abs(abs(x) - 2.4) < 0.3) and y > 8:                # a gold inlay along each side
                c = np.array(t["trim"], np.float32) + fz(x, y, 3) * 14
            if t["style"] == "crystal":
                hue = (0.74 + 0.1 * math.sin(y * 0.18 + x * 0.4)) % 1.0
                rr, gg, bb = _hsv_rgb(hue, 0.35 + 0.15 * abs(x) / 4.6, 0.9)
                c = np.array((rr, gg, bb), np.float32) * 255 * 0.55 + c * 0.45
                if abs(x) < 0.7:
                    c = c * 1.1 + 18
            for idx, gy in enumerate(slots):
                bmp = GLYPHS[(idx + tier) % len(GLYPHS)]
                if glyph_hit(x, y, 0, gy, bmp):
                    lit = idx < t["runes"]
                    c = np.array(rc, np.float32) * (1.0 if lit else 0.45)
                    gl = col(rc, 1.0 if lit else 0.25)
            if e < 0.9:
                c = np.array((255, 255, 255), np.float32) if t["style"] != "ancient" else np.array((255, 236, 170), np.float32)
                if t["style"] == "crystal":
                    gl = col(rc, 0.5)
            return col(np.clip(c, 0, 255)), gl
        Blade(st, 0.75, 1.5, paint).build(m, a)
        trim = t["trim"]
        gw = 8.5 if tier < 3 else 10.5
        box(m, a, -gw, -2.2, -1.5, gw, 0.7, 1.5, metal_paint(trim, rivets=True))
        for sx in (-1, 1):
            lo_x, hi_x = sorted((sx * (gw - 1.2), sx * (gw + 2.8)))
            box(m, a, lo_x, -3.6, -1.2, hi_x, 1.6, 1.2, metal_paint(shade(trim, 0.85)))
        # the rune sockets: one gem on the guard per slot (lit = filled)
        for idx in range(total_slots):
            gx = -3.2 + idx * (6.4 / max(1, total_slots - 1)) if total_slots > 1 else 0.0
            lit = idx < t["runes"]
            box(m, a, gx - 1.1, -2.9, -1.9, gx + 1.1, -0.4, 1.9, metal_paint(shade(t["gem"], 0.9 if lit else 0.35), gem=t["gem"] if lit else None, glow_rgb=t["gem"] if lit else None))
        if tier == 3:                                                                         # the crown wings
            wing = smile_mask(30, 8, 3, hub=0)
            nh = wing.shape[0]
            plate(m, a, wing, -15, 0.8, 0.8, lambda i, j, f: (col(np.clip(np.array(trim, np.float32) * (0.85 + 0.3 * (1 - j / nh)) + fz(i, j, 5) * 12, 0, 255)), col(rc, 0.6) if j == 0 else None), "xy", 0.0)
        box(m, a, -1.5, -15, -1.4, 1.5, -3.6, 1.4, wrap_paint(t["grip"], trim, diamonds=True))
        box(m, a, -2.4, -17.6, -2.1, 2.4, -15, 2.1, metal_paint(trim, gem=t["gem"], glow_rgb=t["gem"]))
        return dict(glow=("%02X%02X%02X" % rc, 0.09, 0.4, 1.0))
    return build


def _hsv_rgb(h, s, v):
    import colorsys
    return colorsys.hsv_to_rgb(h, s, v)


SPECS = [("astral_edge", astral_edge), ("void_edge", void_edge), ("fenrir_sword", fenrir_sword),
         ("runic_sword_renewed", runic(1)), ("runic_sword_ancient", runic(2)), ("runic_sword_mythical", runic(3))]

# the apparent length in blocks (the whole weapon, inside its class range of weapon_classes.json, like the older longswords and greatswords) and where it is held (a share of its length from the butt); see the REAL / GRIP tables of gen_weapon_models
g.REAL.update({"astral_edge": 1.55, "void_edge": 1.6, "fenrir_sword": 2.45, "runic_sword_renewed": 1.45, "runic_sword_ancient": 1.55, "runic_sword_mythical": 2.3})
g.GRIP.update({"astral_edge": 0.25, "void_edge": 0.24, "fenrir_sword": 0.22, "runic_sword_renewed": 0.27, "runic_sword_ancient": 0.26, "runic_sword_mythical": 0.24})

CLASSES = {"astral_edge": "longsword", "void_edge": "longsword", "fenrir_sword": "greatsword", "runic_sword_renewed": "longsword", "runic_sword_ancient": "longsword",
           "runic_sword_mythical": "greatsword"}


# ================================================================ the item model jsons and the class table
def write_models():
    """models/item/<id>_icon.json (the flat icon) and models/item/<id>.json (builtin/entity: the display transforms of a sword of the same class; display_presets.py then fits the long ones)."""
    models = os.path.join(ASSETS, "models", "item")
    for item_id, cls in CLASSES.items():
        with open(os.path.join(models, "magebane_longsword.json" if cls == "longsword" else "thundercleave.json")) as f:
            src = json.load(f)
        src["textures"] = {"particle": "nusmp:item/" + item_id}
        src.pop("nusmp_display_preset", None)
        icon = {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}
        for name, data in ((item_id, src), (item_id + "_icon", icon)):
            with open(os.path.join(models, name + ".json"), "w", newline="\n") as f:
                json.dump(data, f, indent=2)
                f.write("\n")
    # the 3D size table: tools/relics/weapon_classes.json is the source ({"class": ...} per weapon); audit_weapon_scale.py --write copies it to assets/nusmp/weapon_classes.json
    path = os.path.join(ROOT, "tools", "relics", "weapon_classes.json")
    with open(path, encoding="utf-8") as f:
        doc = json.load(f)
    table = doc["weapons"]
    was_sorted = list(table) == sorted(table)
    changed = False
    for item_id, cls in CLASSES.items():
        if item_id not in table:
            table[item_id] = {"class": cls}
            changed = True
    if changed:
        if was_sorted:
            doc["weapons"] = {k: table[k] for k in sorted(table)}
        with open(path, "w", newline="\n", encoding="utf-8") as f:
            f.write(json.dumps(doc, indent=1))                                 # the file has no trailing newline
        print("class table updated: tools/relics/weapon_classes.json (now run: python3 -B tools/elite/finish_weapon_assets.py)")


def main(only=None):
    for item_id, fn in SPECS:
        if only and item_id not in only:
            continue
        mesh, atlas = g.Mesh(), g.Atlas()
        extra = fn(mesh, atlas) or {}
        g.write(item_id, mesh, atlas, extra, False)
    if not only:
        write_models()


if __name__ == "__main__":
    main(sys.argv[1:])
