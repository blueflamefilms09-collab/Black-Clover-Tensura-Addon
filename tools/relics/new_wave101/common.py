"""common: what the twelve worn / held models of wave 101 (release 1.111.0, doc docs/relics/genesis_new.md) share: the ids and palettes, the colours of gold and lapis, the Eye of Wdjat
relief, the hieroglyph and lapis painters, and the glue to the visual upgrade kit of tools/visual_upgrade/charms_genesis_worn (the same VU model builder, skin painter, effect layers
and caps the 27 upgraded charms use; nothing of that kit is changed, this module only imports it).

Frame of every model (as in the kit): model pixels (1/16 block), Y UP, the front of the wearer is -Z; a body piece is relative to the neck line, a head piece to the neck under the head
(the head box is y 0..8), an arm piece to the shoulder pivot (the hand is near y -10).
"""
import math
import os
import sys
import zlib

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
CG = os.path.join(ROOT, "tools", "visual_upgrade", "charms_genesis_worn")
RELICS = os.path.join(ROOT, "tools", "relics")
for _p in (CG, RELICS):
    if _p not in sys.path:
        sys.path.insert(0, _p)

import geo  # noqa: E402
import kitlib as K  # noqa: E402
import paint as PT  # noqa: E402
import roster as R  # noqa: E402
import spec as SP  # noqa: E402
import vu as V  # noqa: E402
from spec import Item, geo_layer as GL, part_layer as PL, step as ST  # noqa: E402,F401
from vu import MAIN, TRIM, GLOW, DARK, ACCENT  # noqa: E402,F401

# ---------------------------------------------------------------- the ids, grades, slots and palettes of the twelve models (copied from Wave101.java)
# id, display name, grade, slot, (main, trim, glow), lore
INFO = [
    ("millennium_puzzle", "Millennium Puzzle", "GENESIS", "NECK", (0x2A4FC0, 0xF0C050, 0xFFE890),
     "A golden pyramid with the Eye of Wdjat on its face, in which a second soul sleeps."),
    ("millennium_eye", "Millennium Eye", "GENESIS", "HEAD", (0x1A3A9A, 0xE8B040, 0x58F0E8),
     "A golden eye set in the brow of a lapis circlet. It sees what is true and what is meant."),
    ("millennium_ring", "Millennium Ring", "GENESIS", "HANDS", (0x233E8C, 0xF2C860, 0xFFD870),
     "A golden ring hung with five needles that swing to whatever means you harm and to whatever holds power."),
    ("millennium_key", "Millennium Key", "GENESIS", "BELT", (0x2C46A8, 0xE4AE3C, 0xFFD060),
     "A golden ankh on a lapis belt: the key to the room of the soul."),
    ("millennium_scale", "Millennium Scale", "GENESIS", "CHARM", (0x1F4AB4, 0xF4C85A, 0x78F0D8),
     "A small golden balance with a feather on one pan. It weighs the heart of a monster and judges it."),
    ("millennium_necklace", "Millennium Necklace", "GENESIS", "CHEST", (0x1C3C90, 0xEBBB52, 0x62E6F0),
     "A broad collar of gold and lapis that shows what is to come."),
    ("truth_seeking_orbs_of_the_sage", "Truth-Seeking Orbs of the Sage", "GENESIS", "BACK", (0x14141C, 0xC8C8D8, 0xE0E0FF),
     "Nine black orbs that float behind the shoulders of whoever carries the sage's power."),
    ("hogyoku_of_the_crystal_dissolution", "Hogyoku, the Crystal of Dissolution", "GENESIS", "CHARM", (0x8A5AC8, 0xE8D8FF, 0xD8A0FF),
     "A crystal that dissolves the line between a soul and what it is meant to be."),
    ("seven_dragon_balls_of_shenron", "Seven Dragon Balls of Shenron", "GENESIS", "BELT", (0xF08A20, 0xC83A2A, 0xFFC040),
     "A belt-pouch with the seven orange spheres, one star to seven."),
    ("straw_hat_of_joy_boy", "Straw Hat of Joy Boy", "GENESIS", "HEAD", (0xE8C870, 0xC8302A, 0xFFF0B0),
     "A worn straw hat with a red band, the promise of a man who laughed at the end of the world."),
    ("law_of_cycles_gown", "Law of Cycles Gown", "GENESIS", "CHEST", (0xF8E8F0, 0xF080B8, 0xFFC0E0),
     "The long white and pink gown of a girl who became a law, so that no one has to despair."),
    ("greed_island_original_binder", "Greed Island Original Binder", "GENESIS", "CHARM", (0x2E7A4A, 0xE0C060, 0x90FFB0),
     "The binder of the game that was built to be won: a green book with forty slots for cards."),
]
IDS = [i[0] for i in INFO]
for _i in INFO:
    R.BY_ID[_i[0]] = _i                    # the kit looks a relic's palette up in the roster of its slice

GOLD, GOLD_HI, GOLD_DK = (232, 180, 64), (255, 228, 132), (190, 140, 44)
LAPIS, LAPIS_DK, LAPIS_HI = (30, 66, 172), (16, 36, 112), (62, 112, 226)
TURQ = (70, 226, 214)


# ================================================================ painters
def deco_lapis(vm, part, flecks=1.0, gold=(236, 196, 90), seed=0, veins=True):
    """Lapis lazuli: a deep blue with paler cloudy veins and a scatter of golden pyrite flecks."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 41 + grp.rect[0] * 5 + grp.rect[1]) & 0xFFFFFFFF)
        if veins:
            n = rng.random((max(2, h // 4 + 2), max(2, w // 4 + 2))).astype(np.float32)
            cloud = np.asarray(Image.fromarray((n * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), np.float32) / 255.0
            tint = np.array([70, 120, 230], np.float32)
            img[..., :3] = img[..., :3] * (1 - 0.35 * cloud[..., None]) + tint * 0.35 * cloud[..., None] * (img[..., :3].mean(axis=2, keepdims=True) / 90.0)
        cnt = int(max(2, flecks * (w * h) / 60))
        for _ in range(cnt):
            x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
            k = float(rng.uniform(0.5, 1.0))
            img[y, x, :3] = np.array(gold, np.float32) * k
            if rng.random() < 0.3 and x + 1 < w:
                img[y, x + 1, :3] = np.array(gold, np.float32) * k * 0.8

    for g in PT.groups_of(part, 2, 2):
        vm.atlas.paint_in(g, run)


def _line(d, S, pts, w, fill=255):
    d.line([(x * S, y * S) for (x, y) in pts], fill=fill, width=max(1, int(w * S)), joint="curve")


def _hiero(d, S, kind, x0, y0, gw, gh, w):
    """One Egyptian sign drawn with lines: eye, ankh, sun disc, water, feather, reeds, scarab, was sceptre, djed pillar."""
    cx, cy = x0 + gw / 2, y0 + gh / 2
    if kind == 0:                                           # the eye
        pts = [(x0 + gw * (0.05 + 0.9 * t), cy - gh * 0.22 * math.sin(math.pi * t)) for t in np.linspace(0, 1, 9)]
        pts2 = [(x0 + gw * (0.05 + 0.9 * t), cy + gh * 0.18 * math.sin(math.pi * t)) for t in np.linspace(0, 1, 9)]
        _line(d, S, pts, w)
        _line(d, S, pts2, w)
        r = gh * 0.14
        d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=255)
        _line(d, S, [(cx, cy + gh * 0.2), (cx, y0 + gh * 0.95)], w)
    elif kind == 1:                                         # the ankh
        r = gh * 0.2
        d.ellipse([(cx - r) * S, (y0 + gh * 0.05) * S, (cx + r) * S, (y0 + gh * 0.05 + 2 * r * 1.15) * S], outline=255, width=max(1, int(w * S)))
        _line(d, S, [(cx, y0 + gh * 0.05 + 2.3 * r), (cx, y0 + gh * 0.95)], w)
        _line(d, S, [(cx - gw * 0.34, y0 + gh * 0.5), (cx + gw * 0.34, y0 + gh * 0.5)], w)
    elif kind == 2:                                         # the sun disc
        r = min(gw, gh) * 0.3
        d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=255, width=max(1, int(w * S)))
        d.ellipse([(cx - w) * S, (cy - w) * S, (cx + w) * S, (cy + w) * S], fill=255)
    elif kind == 3:                                         # water
        for k in range(3):
            yy = y0 + gh * (0.25 + 0.25 * k)
            _line(d, S, [(x0 + gw * (0.05 + 0.15 * i), yy + (gh * 0.08 if i % 2 else -gh * 0.08)) for i in range(7)], w)
    elif kind == 4:                                         # the feather of Ma'at
        _line(d, S, [(cx, y0 + gh * 0.05), (cx, y0 + gh * 0.95)], w)
        for t in np.linspace(0.15, 0.8, 5):
            yy = y0 + gh * t
            _line(d, S, [(cx, yy), (cx + gw * 0.38, yy - gh * 0.12)], w * 0.8)
            _line(d, S, [(cx, yy), (cx - gw * 0.38, yy - gh * 0.12)], w * 0.8)
    elif kind == 5:                                         # reeds
        for k in range(3):
            xx = x0 + gw * (0.2 + 0.3 * k)
            _line(d, S, [(xx, y0 + gh * 0.95), (xx + gw * 0.04 * (k - 1), y0 + gh * 0.08)], w)
    elif kind == 6:                                         # a scarab
        r = min(gw, gh) * 0.28
        d.ellipse([(cx - r) * S, (cy - r * 1.2) * S, (cx + r) * S, (cy + r * 1.2) * S], outline=255, width=max(1, int(w * S)))
        _line(d, S, [(cx, cy - r * 1.2), (cx, cy + r * 1.2)], w * 0.8)
        for sgn in (-1, 1):
            _line(d, S, [(cx + sgn * r, cy - r * 0.3), (cx + sgn * gw * 0.46, cy - r * 0.9)], w)
            _line(d, S, [(cx + sgn * r, cy + r * 0.5), (cx + sgn * gw * 0.46, cy + r * 1.1)], w)
    elif kind == 7:                                         # the was sceptre
        _line(d, S, [(cx, y0 + gh * 0.95), (cx, y0 + gh * 0.15)], w)
        _line(d, S, [(cx, y0 + gh * 0.15), (cx + gw * 0.3, y0 + gh * 0.05)], w)
        _line(d, S, [(cx, y0 + gh * 0.95), (cx - gw * 0.28, y0 + gh * 0.95)], w)
        _line(d, S, [(cx, y0 + gh * 0.95), (cx + gw * 0.28, y0 + gh * 0.95)], w)
    else:                                                   # the djed pillar
        _line(d, S, [(cx, y0 + gh * 0.95), (cx, y0 + gh * 0.08)], w)
        for t in (0.2, 0.34, 0.48, 0.62):
            _line(d, S, [(cx - gw * 0.3, y0 + gh * t), (cx + gw * 0.3, y0 + gh * t)], w)


def deco_hiero(vm, part, rgb, rows=1, seed=0, glow=True, strength=0.9, engrave=0.5, min_w=12, min_h=6, vertical=False, kinds=None):
    """Bands of hieroglyphs (the eye, the ankh, the sun, water, the feather, reeds, a scarab, the was sceptre, the djed pillar) engraved in the skin and, with `glow`, lit in the glow map."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 977 + grp.rect[0] * 31 + grp.rect[1]) & 0xFFFFFFFF)
        cw, ch = (h, w) if vertical else (w, h)
        rowh = max(5, int(ch / max(1, rows)))
        gh = max(4.0, rowh * 0.74)
        gw = gh * 0.8
        lw = max(0.55, gh / 10.0)

        def drawer(d, S):
            for r in range(rows):
                y0 = r * rowh + (rowh - gh) / 2
                x = 1.0
                while x + gw < cw - 1:
                    pool = kinds if kinds else range(9)
                    _hiero(d, S, int(rng.choice(list(pool))), x, y0, gw, gh, lw)
                    x += gw * 1.3
                if rowh >= 8:
                    d.line([0, (y0 - 1) * S, cw * S, (y0 - 1) * S], fill=255, width=max(1, int(0.7 * S)))
                    d.line([0, (y0 + gh + 1) * S, cw * S, (y0 + gh + 1) * S], fill=255, width=max(1, int(0.7 * S)))

        m = PT._mask(cw, ch, drawer)
        if vertical:
            m = m.T
        PT._apply_engrave(img, m, dark=engrave)
        if glow:
            PT._put_glow(gl, m, rgb, strength, halo=0.9)

    for g in PT.groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


def deco_wdjat(vm, part, rgb, seed=0, strength=0.9, min_w=8, min_h=8):
    """The Eye of Wdjat painted large on every big enough rectangle of the part (a face of a pyramid, a plate): the almond, the iris, the brow, the falcon's tear and the spiral; lit."""
    def run(img, gl, grp):
        h, w = img.shape[:2]

        def drawer(d, S):
            cx, cy = w * 0.5, h * 0.45
            ew, eh = w * 0.42, h * 0.17
            lw = max(0.7, min(w, h) / 22.0)
            up = [(cx - ew + 2 * ew * t, cy - eh * math.sin(math.pi * t)) for t in np.linspace(0, 1, 13)]
            lo = [(cx - ew + 2 * ew * t, cy + eh * 0.8 * math.sin(math.pi * t)) for t in np.linspace(0, 1, 13)]
            _line(d, S, up, lw)
            _line(d, S, lo, lw)
            r = eh * 0.8
            d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=255, width=max(1, int(lw * S)))
            r2 = r * 0.4
            d.ellipse([(cx - r2) * S, (cy - r2) * S, (cx + r2) * S, (cy + r2) * S], fill=255)
            brow = [(cx - ew * 1.05 + 2.1 * ew * t, cy - eh * 1.9 - eh * 0.5 * math.sin(math.pi * t)) for t in np.linspace(0, 1, 13)]
            _line(d, S, brow, lw)
            _line(d, S, [(cx - ew * 0.15, cy + eh * 0.9), (cx - ew * 0.15, cy + eh * 2.8)], lw)
            sp = [(cx + ew * 0.55 + eh * 1.5 * math.cos(a) * (1 - a / 14.0), cy + eh * 2.3 + eh * 1.5 * math.sin(a) * (1 - a / 14.0)) for a in np.linspace(0, 9.0, 30)]
            _line(d, S, sp, lw * 0.9)

        m = PT._mask(w, h, drawer)
        PT._apply_engrave(img, m, dark=0.5)
        PT._put_glow(gl, m, rgb, strength, halo=0.9)

    for g in PT.groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


# ================================================================ geometry
def almond(w, h, n=14, sign=1.0, skew=0.0):
    """Points of one lid of an almond (an eye outline) from the left corner to the right corner: x in -w..w, y = sign * h * lid(x)."""
    pts = []
    for i in range(n + 1):
        t = i / n
        x = -w + 2 * w * t
        pts.append((x, sign * h * math.sin(math.pi * t) ** 0.9 + skew * t * (1 - t)))
    return pts


def wdjat(vm, group, centre, w, M=None, lod=False, name="wdjat", slot_iris=MAIN, rgb_glow=None):
    """The Eye of Wdjat as a raised relief facing -Z (a gold rim round the almond, a lapis iris, a lit pupil, the brow, the falcon's tear and its curl), built in the plane through `centre`;
    M turns it onto a slanted face. Returns {'rim', 'iris', 'pupil', 'brow', 'tear'} (the parts; the caller paints them)."""
    M = np.eye(3) if M is None else M
    c = np.asarray(centre, float)
    h = w * 0.42
    r_tube = max(0.14, w * 0.075)
    rim = vm.P(name + "_rim", "gold", group=group, base=GOLD, slot=TRIM, lod=False)
    loop = [(x, y, 0.0) for (x, y) in almond(w, h, vm.S(12, 5), 1.0)] + [(x, y, 0.0) for (x, y) in almond(w, h * 0.8, vm.S(12, 5), -1.0)][::-1]
    geo.tube_path(rim, [tuple(M @ np.array(p) + c) for p in loop], r_tube, sides=vm.S(4, 3), caps=(True, True))
    iris = vm.P(name + "_iris", "stone", group=group, base=LAPIS_DK, slot=slot_iris, lod=lod)
    prof = [(-0.15, w * 0.34), (0.18, w * 0.33), (0.45, w * 0.26), (0.62, w * 0.1), (0.66, 0.0)]
    geo.merge(iris, geo.prim_lathe(prof, sides=vm.S(14, 6), caps=(True, False)), M @ geo.rot_matrix("x", -90), c)
    pupil = vm.P(name + "_pupil", "crystal", group=group, base=(255, 236, 160) if rgb_glow is None else rgb_glow, slot=GLOW, lod=lod)
    geo.merge(pupil, geo.prim_octa(w * 0.12, w * 0.16), M @ geo.rot_matrix("x", -90), c + M @ np.array([0.0, 0.0, -0.8]))
    brow = vm.P(name + "_brow", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    bp = [(-w * 1.05 + 2.1 * w * t, h * 1.7 + h * 0.5 * math.sin(math.pi * t), 0.0) for t in np.linspace(0, 1, vm.S(10, 4))]
    geo.tube_path(brow, [tuple(M @ np.array(p) + c) for p in bp], r_tube * 0.9, sides=vm.S(4, 3), caps=(True, True))
    tear = vm.P(name + "_tear", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    drop = [(-w * 0.12, -h * 0.9, 0.0), (-w * 0.12, -h * 1.9, 0.0), (-w * 0.12, -h * 2.5, 0.0)]
    geo.tube_path(tear, [tuple(M @ np.array(p) + c) for p in drop], r_tube * 0.9, sides=vm.S(4, 3), caps=(True, True))
    sp = [(w * 0.5 + h * 1.0 * math.cos(a) * (1 - a / 14.0), -h * 2.4 + h * 1.0 * math.sin(a) * (1 - a / 14.0), 0.0) for a in np.linspace(0, 8.0, vm.S(14, 6))]
    geo.tube_path(tear, [tuple(M @ np.array(p) + c) for p in sp], r_tube * 0.8, sides=vm.S(4, 3), caps=(True, True))
    return {"rim": rim, "iris": iris, "pupil": pupil, "brow": brow, "tear": tear}


def paint_wdjat(vm, parts, glow_rgb, trim_rgb):
    """The usual decorations of a wdjat() relief."""
    vm.deco(PT.deco_fill_glow, parts["pupil"], glow_rgb, strength=1.0, seed=1)
    vm.deco(deco_lapis, parts["iris"], seed=2)
    vm.deco(PT.deco_grain, parts["rim"], amount=0.12)


def ankh(vm, group, centre, s, M=None, thick=0.9, name="ankh", lod=True, base=None, slot=TRIM):
    """An ankh standing upright in the XY plane (loop up), total height about 3 s, built from tubes: returns the part."""
    M = np.eye(3) if M is None else M
    c = np.asarray(centre, float)
    p = vm.P(name, "gold", group=group, base=base or GOLD_HI, slot=slot, lod=lod)
    loop = [(s * 0.62 * math.cos(a), s * 0.95 + s * 0.82 * math.sin(a), 0.0) for a in np.linspace(0.0, 2 * math.pi, vm.S(16, 6) + 1)]
    geo.tube_path(p, [tuple(M @ np.array(q) + c) for q in loop], thick * 0.5, sides=vm.S(5, 3), caps=(True, True))
    geo.tube_path(p, [tuple(M @ np.array(q) + c) for q in [(0, s * 0.1, 0), (0, -s * 1.9, 0)]], thick * 0.55, sides=vm.S(5, 3), caps=(True, True))
    geo.tube_path(p, [tuple(M @ np.array(q) + c) for q in [(-s * 0.95, -s * 0.25, 0), (s * 0.95, -s * 0.25, 0)]], thick * 0.55, sides=vm.S(5, 3), caps=(True, True))
    return p


def orbit_motes(vm, name, group, n, R, y, r, base, slot=GLOW, phase=0.0, wobble=1.0, material="crystal", lod=False):
    return K.orbs(vm, name, group, n, R, y, r, material, base, slot=slot, phase=phase, wobble=wobble, lod=lod)


def fx_set(vm, pal, y, R, tilt=0.0, ring_group=None, shaft=None, flare=True, rune=True):
    """The shared additive set: a rune ring, a lens flare, light shafts (kitlib.halo_set), returned as the dict of its groups."""
    return K.halo_set(vm, pal, y=y, R=R, rune=rune, flare=flare, shaft=shaft, tilt=tilt)


def deco_weave(vm, part, dark=(120, 84, 30), light=(250, 226, 150), seed=0, cell=3.0):
    """Woven straw: two crossing sets of diagonal strands, a light and a dark one, with small gaps."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        a = ((xx + yy) / cell) % 2.0
        b = ((xx - yy) / cell) % 2.0
        over = ((np.floor((xx + yy) / cell) + np.floor((xx - yy) / cell)) % 2.0)
        col = np.where(over[..., None] > 0.5, np.array(light, np.float32), np.array(dark, np.float32))
        gap = (np.minimum(a, 2.0 - a) < 0.14) | (np.minimum(b, 2.0 - b) < 0.14)
        col = np.where(gap[..., None], np.array(dark, np.float32) * 0.55, col)
        img[..., :3] = img[..., :3] * 0.35 + col * 0.65

    for g in PT.groups_of(part, 4, 4):
        vm.atlas.paint_in(g, run)


def deco_slots(vm, part, rgb, cols=8, rows=5, seed=0, fill=(246, 236, 200)):
    """The forty card slots of a binder: a grid of inset rectangles, some holding a card (a coloured face), on every big enough rectangle of the part."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 131 + grp.rect[0]) & 0xFFFFFFFF)
        cw, ch = w / cols, h / rows

        def drawer(d, S):
            for r in range(rows):
                for c in range(cols):
                    x0, y0 = c * cw + cw * 0.14, r * ch + ch * 0.12
                    d.rectangle([x0 * S, y0 * S, (x0 + cw * 0.72) * S, (y0 + ch * 0.76) * S], outline=255, width=max(1, int(0.7 * S)))

        m = PT._mask(w, h, drawer)
        PT._apply_engrave(img, m, dark=0.6)
        for r in range(rows):
            for c in range(cols):
                if rng.random() < 0.45:
                    x0, y0 = int(c * cw + cw * 0.2), int(r * ch + ch * 0.18)
                    x1, y1 = int(c * cw + cw * 0.8), int(r * ch + ch * 0.82)
                    tint = np.array(fill, np.float32) * float(rng.uniform(0.75, 1.0))
                    img[y0:y1, x0:x1, :3] = img[y0:y1, x0:x1, :3] * 0.25 + tint * 0.75
        PT._put_glow(gl, m * 0.5, rgb, 0.5, halo=0.6)

    for g in PT.groups_of(part, 14, 10):
        vm.atlas.paint_in(g, run)
