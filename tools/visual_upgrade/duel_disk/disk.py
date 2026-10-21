"""disk: the duel-disk worn model of the deck relics (slice `duel_disk`, release 1.110.0).

A third-party design (the arm mounted card game device of a well known series) is used as a VISUAL BASE only: a curved plate strapped on the forearm, a fan of five
blade-like card zones on a hub, a spell/trap zone each, a deck holder with a visible stack of cards, a life-point strip. Every deck relic keeps its own colours and
emblem. Built with the toolkit of tools/visual_upgrade/charms_genesis_worn (VU, geo, paint). Authoring frame of the toolkit: Y UP, front is -Z, 1 unit = 1 model pixel; the
numbers of this file are written in the ARM frame of the game: x outward from the arm centre (left arm: +x), yd = pixels DOWN the arm from the shoulder pivot, z front = negative.
The worn library draws a relic piece at 0.85 (WornPlacer.BODY_SCALE) about the arm pivot, so every model is written at 1/0.85 and ends up at the numbers below.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
KIT = os.path.abspath(os.path.join(HERE, "..", "charms_genesis_worn"))
sys.path.insert(0, KIT)
import geo  # noqa: E402
import kitlib as K  # noqa: E402
import paint as PT  # noqa: E402
import vu as V  # noqa: E402
from vu import MAIN, TRIM, GLOW, DARK, ACCENT  # noqa: E402

BODY_SCALE = 0.85
SH0, SH1 = 3.4, 9.7                      # the plate runs down the forearm (yd)
# the shell is a C round the outer, front and back of the arm: points (x, z), all clear of the sleeve layer box x -1.25..3.25, |z| < 2.25
SHELL = [(0.9, -2.8), (2.6, -2.8), (3.7, -2.6), (3.9, -1.5), (3.9, 1.5), (3.7, 2.6), (2.6, 2.8), (0.9, 2.8)]
STRAP = [(0.9, -2.8), (-1.55, -2.7), (-1.55, 2.7), (0.9, 2.8)]
HUB = (4.25, 5.1, 0.5)                   # x, yd, z of the blade hub
ANGLES = (-26.0, -13.0, 0.0, 13.0, 26.0)
HOLDER = dict(x0=3.95, x1=5.9, yd0=7.0, yd1=9.75, hw=1.15)
CARDS_CENTRE = (9.5, 2.6)                # final arm-frame x, yd of the orbit centre of the floating cards


def Y(yd):
    return -yd


_BOX = {}


def box(w, h, d):
    k = (round(w, 3), round(h, 3), round(d, 3))
    if k not in _BOX:
        _BOX[k] = geo.prim_box(w, h, d, chamfer=0.0, caps=False)
    return _BOX[k]


def seg_box(dst, p0, p1, yd0, yd1, th, grow=0.0):
    """A box along the xz segment p0 -> p1, from yd0 to yd1 down the arm, th thick."""
    dx, dz = p1[0] - p0[0], p1[1] - p0[1]
    L = math.hypot(dx, dz) + grow
    ex = np.array([dx, 0.0, dz]) / math.hypot(dx, dz)
    ey = np.array([0.0, 1.0, 0.0])
    M = np.column_stack([ex, ey, np.cross(ex, ey)])
    geo.merge(dst, box(L, yd1 - yd0, th), M, ((p0[0] + p1[0]) / 2, Y((yd0 + yd1) / 2), (p0[1] + p1[1]) / 2))


def fan_dir(a):
    """Unit vector (authoring frame) of a blade at angle a degrees (0 straight forward, + toward the hand)."""
    r = math.radians(a)
    return np.array([0.0, -math.sin(r), -math.cos(r)])


def card_back(c1, c2):
    def deco(vm, part):
        def run(img, gl, grp):
            h, w = img.shape[:2]

            def drawer(d, S):
                d.rectangle([0, 0, w * S, h * S], outline=255, width=max(1, int(S * 0.9)))
                for k in range(-h, w + h, 3):
                    d.line([k * S, 0, (k + h) * S, h * S], fill=255, width=max(1, int(S * 0.5)))
                    d.line([k * S, h * S, (k + h) * S, 0], fill=255, width=max(1, int(S * 0.5)))
            m = PT._mask(w, h, drawer)[..., None]
            img[..., :3] = img[..., :3] * 0.25 + np.array(c1, np.float32) * 0.75
            img[..., :3] = img[..., :3] * (1 - m * 0.8) + np.array(c2, np.float32) * m * 0.8
        for g in PT.groups_of(part, 5, 5):
            vm.atlas.paint_in(g, run)
    return deco


def suit_mark(kind, rgb, glow_rgb=None):
    """Paints a card suit (spade heart diamond club) or a star (joker, fate) big on every large rectangle of the part."""
    def deco(vm, part):
        def run(img, gl, grp):
            h, w = img.shape[:2]
            cx, cy, s = w / 2, h / 2, min(w, h) * 0.34

            def drawer(d, S):
                X, Yv, R = cx * S, cy * S, s * S
                if kind == "diamond":
                    d.polygon([(X, Yv - R * 1.2), (X + R * 0.8, Yv), (X, Yv + R * 1.2), (X - R * 0.8, Yv)], fill=255)
                elif kind in ("heart", "spade"):
                    sg = 1 if kind == "heart" else -1
                    r2 = R * 0.62
                    d.ellipse([X - R * 0.95, Yv - sg * R * 0.55 - r2, X - R * 0.95 + 2 * r2, Yv - sg * R * 0.55 + r2], fill=255)
                    d.ellipse([X + R * 0.95 - 2 * r2, Yv - sg * R * 0.55 - r2, X + R * 0.95, Yv - sg * R * 0.55 + r2], fill=255)
                    d.polygon([(X - R * 0.98, Yv - sg * R * 0.35), (X + R * 0.98, Yv - sg * R * 0.35), (X, Yv + sg * R * 1.2)], fill=255)
                    if kind == "spade":
                        d.polygon([(X - R * 0.25, Yv + R * 1.3), (X + R * 0.25, Yv + R * 1.3), (X, Yv + R * 0.4)], fill=255)
                elif kind == "club":
                    r2 = R * 0.5
                    for ox, oy in ((0, -R * 0.55), (-R * 0.6, R * 0.25), (R * 0.6, R * 0.25)):
                        d.ellipse([X + ox - r2, Yv + oy - r2, X + ox + r2, Yv + oy + r2], fill=255)
                    d.polygon([(X - R * 0.22, Yv + R * 1.3), (X + R * 0.22, Yv + R * 1.3), (X, Yv)], fill=255)
                else:
                    pts = []
                    for i in range(10):
                        a = math.pi / 5 * i - math.pi / 2
                        rr = R * 1.15 if i % 2 == 0 else R * 0.5
                        pts.append((X + rr * math.cos(a), Yv + rr * math.sin(a)))
                    d.polygon(pts, fill=255)
            m = PT._mask(w, h, drawer)
            img[..., :3] = img[..., :3] * (1 - 0.9 * m[..., None]) + np.array(rgb, np.float32) * 0.9 * m[..., None]
            PT._put_glow(gl, m, glow_rgb or rgb, 0.8, halo=0.8)
        for g in PT.groups_of(part, 4, 4):
            vm.atlas.paint_in(g, run)
    return deco


def build_disk(vm, S, hand=False):
    """S: dict(pal, shell=(material, base), trim, strap, cardback=(c1, c2), marks=[5 kinds], text, stars, halo, orbit, gem_base, zone_base, spell_glow)."""
    pal = S["pal"]
    glow, trim_c = K.rgb(pal[2]), K.rgb(pal[1])
    lod = vm.lod
    hub_p = (HUB[0], Y(HUB[1]), HUB[2])
    g_fan = vm.grp("card_fan", "swing", hub_p, 3.0, 2.2, 0.0)
    g_lp = vm.grp("life_points", "pulse", (4.2, Y(7.0), 0.0), 0.05, 0.12, 0.0)
    g_top = vm.grp("deck_top_card", "bob", (5.0, Y(8.3), 0.0), 0.25, 0.07, 0.0)
    g_gem = vm.grp("hub_gem", "pulse", hub_p, 0.10, 0.10, 0.0)
    g_sky = vm.grp("fate_sky", "pulse", hub_p, 0.04, 0.1, 0.0) if S["stars"] else None
    g_seal = vm.grp("fate_seal", "pulse", (6.2, Y(8.3), 0.0), 0.10, 0.08, 0.0) if S["halo"] else None
    g_flare = vm.grp("lens_flare", "pulse", hub_p, 0.16, 0.10, 0.0)
    g_shaft = vm.grp("light_shafts", "pulse", (5.2, Y(7.0), 0.0), 0.10, 0.07, 1.3) if S["halo"] else None
    g_orb = vm.grp("pact_cards", "spin", (CARDS_CENTRE[0], Y(CARDS_CENTRE[1]), 0.0), 1.9) if S["orbit"] else None

    # ---- the shell (curved plate) with a trimmed edge
    sm, sb = S["shell"]
    shell = vm.P("shell", sm, base=sb, lod=True)
    tm, tb = S["trim"]
    trim = vm.P("shell_trim", tm, base=tb, slot=TRIM, lod=False)
    pts = SHELL if not lod else [SHELL[0], SHELL[3], SHELL[4], SHELL[7]]
    for a, b in zip(pts, pts[1:]):
        seg_box(shell, a, b, SH0, SH1, 0.5, grow=0.2)
        for yd0, yd1 in ((SH0 - 0.05, SH0 + 0.35), (SH1 - 0.35, SH1 + 0.05)):
            seg_box(trim, a, b, yd0, yd1, 0.66, grow=0.2)
    # ---- straps with a buckle
    lm, lb = S["strap"]
    strap = vm.P("straps", lm, base=lb, lod=False)
    for yd in (4.3, 8.7):
        for a, b in zip(STRAP, STRAP[1:]):
            seg_box(strap, a, b, yd - 0.45, yd + 0.45, 0.4, grow=0.3)
    buckle = vm.P("buckle", tm, base=tb, slot=TRIM, lod=False)
    for yd in (4.3, 8.7):
        geo.merge(buckle, box(1.1, 1.2, 0.3), None, (1.6, Y(yd), -2.95))
    # ---- the hub and its gem
    hub = vm.P("hub", tm, base=tb, slot=TRIM, group=g_fan, lod=False)
    geo.merge(hub, geo.prim_lathe([(-0.45, 0.0), (-0.45, 1.0), (0.45, 1.0), (0.45, 0.0)], sides=vm.S(12, 6), caps=(True, True)), geo.rot_matrix("z", 90), (HUB[0] + 0.1, Y(HUB[1]), HUB[2]))
    gem = vm.P("hub_gem", "crystal", base=S["gem_base"], slot=GLOW, group=g_gem, glow=True, lod=False)
    geo.merge(gem, geo.prim_gem(0.95, long=0.8, sides=vm.S(8, 4)), geo.rot_matrix("z", -90), (HUB[0] + 0.8, Y(HUB[1]), HUB[2]))
    # ---- the five blades (card zone fan), each with a monster zone and a spell zone
    bm, bb = S["shell"]
    blades = vm.P("blades", bm, base=bb, group=g_fan, lod=True)
    marks = S["marks"]
    zone_parts = []
    piv = np.array([HUB[0], Y(HUB[1]), HUB[2]])
    for i, ang in enumerate(ANGLES):
        if lod and i % 2:
            continue
        d = fan_dir(ang)
        M = geo.align_y(d)
        geo.merge(blades, box(0.34, 5.6, 1.4), M, tuple(piv + d * 3.5))
        if lod:
            continue
        zm = vm.P("zone_monster_%d" % i, "obsidian", base=S["zone_base"], group=g_fan, slot=DARK)
        geo.merge(zm, box(0.12, 2.5, 0.95), M, tuple(piv + d * 4.5 + np.array([0.2, 0, 0])))
        zs = vm.P("zone_spell_%d" % i, "obsidian", base=S["zone_base"], group=g_fan, slot=DARK)
        geo.merge(zs, box(0.12, 1.5, 0.75), M, tuple(piv + d * 1.95 + np.array([0.2, 0, 0])))
        zone_parts += [(zm, i, "monster"), (zs, i, "spell")]
    # ---- the life-point strip along the rim of the fan and the lower edge of the shell
    lp = vm.P("life_points", "crystal", base=S["gem_base"], slot=GLOW, group=g_lp, glow=True, lod=False)
    rim = [piv + fan_dir(a) * 6.45 + np.array([0.12, 0, 0]) for a in ANGLES]
    for a, b in zip(rim, rim[1:]):
        L = float(np.linalg.norm(b - a))
        ex = (b - a) / L
        ey = np.array([1.0, 0.0, 0.0])
        M = np.column_stack([ey, ex, np.cross(ey, ex)])
        geo.merge(lp, box(0.3, L + 0.5, 0.35), M, tuple((a + b) / 2))
    for a, b in zip(pts, pts[1:]):
        seg_box(lp, a, b, SH1 + 0.02, SH1 + 0.3, 0.62, grow=0.2)
    # ---- the deck holder with the stack of cards
    H = HOLDER
    hold = vm.P("deck_holder", tm, base=tb, slot=TRIM, lod=True)
    geo.merge(hold, box(0.3, H["yd1"] - H["yd0"], 2 * H["hw"] + 0.44), None, (H["x0"] + 0.15, Y((H["yd0"] + H["yd1"]) / 2), 0.0))
    cy = (H["yd0"] + H["yd1"]) / 2
    xm = (H["x0"] + H["x1"]) / 2
    for sg in (-1, 1) if not lod else ():
        geo.merge(hold, box(H["x1"] - H["x0"], H["yd1"] - H["yd0"], 0.22), None, (xm, Y(cy), sg * (H["hw"] + 0.11)))
    if not lod:
        geo.merge(hold, box(H["x1"] - H["x0"], 0.22, 2 * H["hw"] + 0.44), None, (xm, Y(H["yd1"] - 0.11), 0.0))
    stack = vm.P("deck_stack", "bone", base=(236, 230, 214), slot=ACCENT, lod=False)
    top = vm.P("deck_top", "bone", base=(250, 244, 226), slot=ACCENT, group=g_top, lod=True)
    n = 9 if not hand else 12
    cw, ch, ct = 2.0, 2.6, 0.13
    for i in range(n):
        x = H["x0"] + 0.45 + ct * (i + 0.5)
        jig = 0.06 * ((i * 7) % 5 - 2)
        geo.merge(top if i == n - 1 else stack, box(ct, ch, cw), None, (x + (0.55 if i == n - 1 else 0.0), Y(cy - 0.05 + jig * 2), jig))
    # ---- the floating cards of an ambient group (the pact deck; the Deck of Many Things keeps its own piece)
    if S["orbit"] and not lod:
        oc = vm.P("pact_cards", "bone", base=(248, 244, 232), group=g_orb, slot=ACCENT, lod=False)
        c2 = geo.prim_card(2.6, 4.0, 0.14)
        for i in range(7):
            th = 2 * math.pi * i / 7
            R = 4.4 + 0.5 * (i % 2)
            geo.merge(oc, c2, geo.rot_matrix("y", -math.degrees(th) + 90) @ geo.rot_matrix("x", 8), (CARDS_CENTRE[0] + R * math.cos(th), Y(CARDS_CENTRE[1] + 1.6 * math.sin(i * 1.7)), R * math.sin(th)))
    # ---- the starry inlay round the hub, the seal, the flare and the light shafts
    if S["stars"]:
        sky = vm.star("sky", g_sky, lod=False)
        geo.merge(sky, geo.prim_lathe([(0.0, 1.9), (0.06, 1.9)], sides=vm.S(14, 6), caps=(True, True)), geo.rot_matrix("z", 90), (4.18, Y(HUB[1]), HUB[2]))
    if not lod:
        vm.fx_cross(g_flare, ACCENT, (HUB[0] + 0.9, Y(HUB[1]), HUB[2]), (1, 0, 0), 3.4, "flare", n=2)
    if S["halo"] and not lod:
        vm.fx_disc(g_seal, GLOW, (H["x1"] + 0.6, Y(cy), 0.0), (1, 0, 0), 4.8, "seal")
        vm.fx_beam(g_shaft, ACCENT, (5.0, Y(H["yd0"]), 0.0), (5.0, Y(H["yd0"] - 3.8), 0.0), 2.4, "beam")
    # ---- paint: emblem, lettering, frames
    vm.deco(PT.deco_glyphs, shell, glow, text=S["text"], rows=2, glow=False, seed=3)
    vm.deco(PT.deco_filigree, trim, trim_c, seed=4, lit=glow)
    vm.deco(PT.deco_fill_glow, gem, glow, strength=0.95, seed=6)
    vm.deco(PT.deco_rim_glow, lp, glow, strength=0.9)
    vm.deco(PT.deco_grain, shell, amount=0.08)
    vm.deco(card_back(*S["cardback"]), stack)
    vm.deco(card_back(*S["cardback"]), top)
    for zp, i, kind in zone_parts:
        vm.deco(PT.deco_rim_glow, zp, glow if kind == "monster" else K.rgb(S["spell_glow"]), strength=0.9, width=1.1, min_w=4, min_h=4)
        if kind == "monster":
            vm.deco(suit_mark(marks[i], trim_c, glow), zp)
    return vm
