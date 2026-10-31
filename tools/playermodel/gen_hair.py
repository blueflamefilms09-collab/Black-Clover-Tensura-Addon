"""Why: writes the haircuts of the player-model kit: assets/nusmp/playermodel/hair_styles.json (the boxes, chains and UV regions the Java hair system reads) and one painted 64x64 sheet per
style, assets/nusmp/textures/entity/playermodel/hair/<style>.png (grey strands with an alpha mask; the game tints them with the character's hair colour). Run:
    python3 -B tools/playermodel/gen_hair.py
The styles are the eleven the owner named (short, long, ponytail, twin_tails, braid, spiky, bob, messy, bun, mane, bald) and the cuts the arc characters and the old avatar hair ids need
(slick, curtains, swept, mohawk, hime, layered, high_ponytail, drills, pompadour, ahoge, spiky_long, hood). See docs/playermodel.md for the geometry rules:
 * the scalp cap is the 8x8x8 box at the hat layer's UV (u 32, v 0) inflated 0.7 (the hat layer itself is 0.5), so it never fights with the skin or a skin's own hat layer;
 * everything that sits on the cap is inflated more (0.85 and up, a different amount per layer) so no two surfaces share a plane;
 * hanging locks are chains of 2 to 4 segments, the upper ones hung from the head and the lower ones from the body, so the back hair follows the neck;
 * boxes marked helm "hide" disappear under a helmet; chains drop the segments that would be inside the helmet's 10 x 10 x 10 box.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hair_lib import Style, write_styles, ASSETS  # noqa: E402

OUT_JSON = os.path.join(ASSETS, "playermodel", "hair_styles.json")
OUT_SHEETS = os.path.join(ASSETS, "textures", "entity", "playermodel", "hair")


# ---------------------------------------------------------------------------------------------- cap masks
def cap_mask(front=2, side_hi=2, side_lo=3, back=4, top=True, notch=(3, 4), bangs_cols=None):
    """A scalp mask: `front` rows of fringe (a notch in the middle of the second row), the side rows above and behind the temple, the back rows."""

    def m(face, lx, ly, lz):
        if face == "top":
            return top
        if face == "front":
            if ly >= front:
                return False
            if bangs_cols is not None:
                return lx in bangs_cols or ly == 0
            return not (ly == front - 1 and lx in notch and front > 1)
        if face in ("west", "east"):
            if lz < 3:
                return ly < side_hi
            return ly < side_lo
        if face == "back":
            return ly < back
        return False

    return m


def tilt(dx, dz, deg):
    """The box rotation (rx, ry, rz) that leans a standing box toward (dx back-left...) (dx = entity left, dz = back) by `deg` degrees, see hair_lib for the convention."""
    n = math.hypot(dx, dz) or 1.0
    a = deg
    return (-a * dz / n, 0.0, a * dx / n)


def tail_segments(count, width, length, depth, part_switch=1, infl=0.15, taper=0.0):
    segs = []
    for i in range(count):
        w = width * (1.0 - taper * i / max(1, count - 1))
        segs.append((w, length, depth * (1.0 - 0.15 * taper * i), "head" if i < part_switch else "body", infl + 0.02 * (i % 2)))
    return segs


def bangs(s, kind, tone=0):
    """The fringe: single-segment locks hung over the front of the scalp cap (inflated more than the cap, so they are a second layer in front of it), which sway a little with the head."""
    z = -4.75
    if kind == "blunt":
        s.chain("fringe", (0.0, -8.3, z), [(8.4, 2.8, 0.8, "head", 0.22)], tuning="stiff", params="stiff", phase=0.2, helm="hide", taper_last="ends")
    elif kind == "center":
        s.chain("fringeR", (-2.15, -8.3, z), [(4.2, 3.2, 0.8, "head", 0.22)], tuning="stiff", params="stiff", phase=0.2, helm="hide", taper_last="point_down", shared_key="fr")
        s.chain("fringeL", (2.15, -8.3, z), [(4.2, 3.2, 0.8, "head", 0.22)], tuning="stiff", params="stiff", phase=1.4, helm="hide", taper_last="point_down", shared_key="fr")
    elif kind == "side":
        s.chain("fringe", (-1.0, -8.3, z), [(6.4, 2.8, 0.8, "head", 0.22)], tuning="stiff", params="stiff", phase=0.2, helm="hide", taper_last="point_down")
        s.chain("fringe2", (3.2, -8.3, z), [(1.8, 3.8, 0.8, "head", 0.26)], tuning="stiff", params="stiff", phase=1.1, helm="hide", taper_last="point_down")
    elif kind == "strands":
        for i, (x, ln) in enumerate(((-3.2, 2.6), (-1.1, 3.4), (1.1, 2.4), (3.1, 3.2))):
            s.chain("fringe%d" % i, (x, -8.3, z - 0.02 * i), [(2.0, ln, 0.8, "head", 0.2 + 0.03 * (i % 2))], tuning="stiff", params="stiff", phase=0.7 * i, helm="hide",
                    taper_last="point_down", shared_key="frs%d" % i)


# ---------------------------------------------------------------------------------------------- the styles
STYLES = {}
ORDER = []
_CTX = {"prefix": "", "anime": False, "only": None}          # 3.37: the second pass over the classic styles paints anime sheets under another id (see build_anime)


def style(sid, anime=None):
    s = Style(_CTX["prefix"] + sid, anime=_CTX["anime"] if anime is None else anime)
    if _CTX["only"] is None or sid in _CTX["only"]:         # a style outside `only` is built (the code is shared) but not kept
        STYLES[s.id] = s
        ORDER.append(s.id)
    return s


def build():
    _build_classic()
    build_anime()
    return STYLES


def _build_classic():
    # bald: nothing but the sheet (the loader needs the style to exist)
    style("bald")

    # short: the plain cut
    s = style("short")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=4))
    bangs(s, "strands")

    # slick: swept back, no fringe, glossy
    s = style("slick")
    def slick_mask(face, lx, ly, lz):
        if face == "back":
            return ly < 3 or (ly < 5 and 1 <= lx <= 6)
        return cap_mask(front=1, side_hi=1, side_lo=2, back=4, notch=())(face, lx, ly, lz)

    s.cap(slick_mask)

    # curtains: parted in the middle
    s = style("curtains")
    s.cap(cap_mask(front=3, side_hi=4, side_lo=5, back=5, bangs_cols=(0, 1, 2, 5, 6, 7)))
    bangs(s, "center")

    # swept: a side part, the long fringe over the right eye
    s = style("swept")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=4, notch=()))
    bangs(s, "side")

    # messy: the short cut with loose tufts
    s = style("messy")
    s.cap(cap_mask(front=2, side_hi=3, side_lo=4, back=5, notch=(2, 5)), inflate=0.75)
    tufts = [(-3.0, -3.4, -18, -1, -1), (0.0, -3.9, -22, 0, -1), (3.0, -3.3, -18, 1, -1), (-3.6, 0.0, -14, -1, 0), (3.7, 0.4, -14, 1, 0), (-2.0, 3.3, -20, -1, 1),
             (1.6, 3.6, -20, 1, 1), (0.0, -0.4, -10, 0, 0)]
    for i, (x, z, a, dx, dz) in enumerate(tufts):
        rx, ry, rz = tilt(dx, dz, 24 + 8 * (i % 3))
        s.box("tuft%d" % i, (x - 0.75, -11.6 + (0.6 if i % 2 else 0.0), z - 0.75), (1.5, 3.6, 1.5), inflate=0.1, tone=1 if i % 2 else 0, rot=(rx, 0, rz),
              pivot=(x, -8.2, z), taper="point_up")
    bangs(s, "strands")

    # spiky
    s = style("spiky")
    s.cap(cap_mask(front=1, side_hi=2, side_lo=3, back=4, notch=()), inflate=0.7)
    ring = [(-2.6, -2.6, -1, -1), (0.0, -3.0, 0, -1), (2.6, -2.6, 1, -1), (-3.0, 0.2, -1, 0), (3.0, 0.2, 1, 0), (-2.4, 2.8, -1, 1), (0.0, 3.1, 0, 1), (2.4, 2.8, 1, 1),
            (0.0, 0.0, 0, 0)]
    for i, (x, z, dx, dz) in enumerate(ring):
        lean = 6 if (dx == 0 and dz == 0) else 36 + 5 * (i % 3)
        rx, ry, rz = tilt(dx, dz, lean)
        h2 = 2.4 + 0.7 * (i % 3)
        s.box("spike%da" % i, (x - 1.4, -10.4, z - 1.4), (2.8, 2.4, 2.8), inflate=0.1, tone=1 if i % 2 else 0, rot=(rx, 0, rz), pivot=(x, -8.0, z), taper="point_up")
        s.box("spike%db" % i, (x - 0.8, -10.4 - h2, z - 0.8), (1.6, h2, 1.6), inflate=0.05, tone=2 if i % 2 else 1, rot=(rx, 0, rz), pivot=(x, -8.0, z), taper="point_up", tip=0.5)

    # spiky_long: the spikes and a long tail of hair down the back
    s = style("spiky_long")
    s.cap(cap_mask(front=1, side_hi=2, side_lo=3, back=6, notch=()), inflate=0.7)
    for i, (x, z, dx, dz) in enumerate(ring[:8]):
        rx, ry, rz = tilt(dx, dz + 0.9, 34 + 5 * (i % 3))
        h2 = 2.4 + 0.7 * (i % 3)
        s.box("spike%da" % i, (x - 1.4, -10.2, z - 1.4), (2.8, 2.2, 2.8), inflate=0.1, tone=1 if i % 2 else 0, rot=(rx, 0, rz), pivot=(x, -8.0, z), taper="point_up")
        s.box("spike%db" % i, (x - 0.8, -10.2 - h2, z - 0.8), (1.6, h2, 1.6), inflate=0.05, tone=2 if i % 2 else 1, rot=(rx, 0, rz), pivot=(x, -8.0, z), taper="point_up", tip=0.5)
    s.chain("back", (0.0, -2.0, 3.7), [(5.0, 4.0, 1.6, "head", 0.2), (4.6, 4.5, 1.6, "body", 0.22), (3.6, 4.5, 1.4, "body", 0.2)], tuning="head", params="default", phase=0.2)

    # mohawk: shaved sides, a tall ridge from the brow to the nape
    s = style("mohawk")

    def stubble(face, lx, ly, lz):
        if face == "top":
            return not (2 <= lx <= 5)
        if face in ("west", "east"):
            return ly < 2 and lz >= 2
        if face == "back":
            return ly < 2 and not (3 <= lx <= 4)
        return False

    s.cap(stubble, inflate=0.62, tone=-1)
    for i in range(7):
        z = -3.9 + i * 1.25
        h = 6.4 - 0.8 * abs(i - 3.0) + (0.9 if i % 2 else 0.0)
        s.box("ridge%d" % i, (-1.15, -8.2 - h, z - 0.7), (2.3, h, 1.4), inflate=0.12, tone=1 if i % 2 else 0, taper="point_up", helm="hide", tip=0.4)
    s.chain("nape", (0.0, -3.0, 3.8), [(2.4, 3.0, 1.2, "head", 0.15), (2.2, 2.5, 1.2, "head", 0.15)], tuning="stiff", params="stiff", phase=0.5)

    # bob: ends at the jaw, swings a little
    s = style("bob")
    s.cap(cap_mask(front=2, side_hi=3, side_lo=5, back=6, notch=(3, 4)), inflate=0.72)
    bangs(s, "blunt")
    s.chain("bobB", (0.0, -2.8, 3.7), [(8.0, 3.4, 1.4, "head", 0.3)], tuning="head", params="default", phase=0.0)
    s.chain("bobL", (4.55, -3.4, 0.5), [(1.4, 3.6, 5.6, "head", 0.1)], tuning="head", params="default", phase=1.0, shared_key="bobside")
    s.chain("bobR", (-4.55, -3.4, 0.5), [(1.4, 3.6, 5.6, "head", 0.1)], tuning="head", params="default", phase=2.0, shared_key="bobside")

    # long
    s = style("long")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=5, back=8, notch=(3, 4)), inflate=0.72)
    bangs(s, "center")
    s.chain("back", (0.0, -2.0, 3.65), [(8.0, 4.5, 1.7, "head", 0.22), (8.4, 4.5, 1.7, "body", 0.24), (8.2, 4.5, 1.7, "body", 0.22), (7.4, 4.0, 1.5, "body", 0.2)], tuning="head",
            params="heavy", phase=0.0, taper_last="ends")
    s.chain("frontL", (4.55, -5.2, -2.9), [(1.3, 5.2, 1.5, "head", 0.12), (1.3, 4.4, 1.5, "body", 0.12)], tuning="head", params="default", phase=0.8, shared_key="front")
    s.chain("frontR", (-4.55, -5.2, -2.9), [(1.3, 5.2, 1.5, "head", 0.12), (1.3, 4.4, 1.5, "body", 0.12)], tuning="head", params="default", phase=2.2, shared_key="front")

    # layered: long with bangs and a second, shorter layer
    s = style("layered")
    s.cap(cap_mask(front=3, side_hi=3, side_lo=5, back=8, bangs_cols=(0, 1, 2, 5, 6, 7)), inflate=0.72)
    bangs(s, "strands")
    s.chain("back", (0.0, -2.0, 3.65), [(8.0, 4.5, 1.7, "head", 0.22), (8.4, 4.5, 1.7, "body", 0.24), (8.2, 4.5, 1.7, "body", 0.22)], tuning="head", params="heavy", phase=0.0,
            taper_last="ends")
    s.chain("back2", (0.0, -3.0, 4.35), [(6.0, 4.0, 1.3, "head", 0.3), (6.2, 4.0, 1.3, "body", 0.32)], tuning="head", params="default", phase=1.1, taper_last="ends", shared_key="b2")
    s.chain("frontL", (4.55, -5.4, -2.9), [(1.3, 5.2, 1.5, "head", 0.12), (1.3, 3.6, 1.5, "body", 0.12)], tuning="head", params="default", phase=0.8, shared_key="front")
    s.chain("frontR", (-4.55, -5.4, -2.9), [(1.3, 5.2, 1.5, "head", 0.12), (1.3, 3.6, 1.5, "body", 0.12)], tuning="head", params="default", phase=2.2, shared_key="front")

    # hime: blunt bangs, cheek locks, a long straight back
    s = style("hime")
    s.cap(cap_mask(front=3, side_hi=3, side_lo=5, back=8, notch=()), inflate=0.72)
    bangs(s, "blunt")
    s.chain("back", (0.0, -2.0, 3.65), [(8.0, 4.5, 1.7, "head", 0.22), (8.4, 5.0, 1.7, "body", 0.24), (8.4, 5.0, 1.7, "body", 0.22), (8.0, 4.0, 1.5, "body", 0.2)], tuning="head",
            params="heavy", phase=0.0, taper_last="none")
    s.chain("cheekL", (4.6, -4.4, -2.7), [(1.8, 5.0, 2.0, "head", 0.14), (1.8, 4.0, 2.0, "body", 0.14)], tuning="head", params="default", phase=0.6, shared_key="cheek")
    s.chain("cheekR", (-4.6, -4.4, -2.7), [(1.8, 5.0, 2.0, "head", 0.14), (1.8, 4.0, 2.0, "body", 0.14)], tuning="head", params="default", phase=2.4, shared_key="cheek")

    # ponytail
    s = style("ponytail")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=5))
    bangs(s, "strands")
    s.box("tie", (-1.5, -5.6, 4.1), (3.0, 1.2, 2.4), inflate=0.15, tone=2, pattern="solid", helm="hide")
    s.chain("tail", (0.0, -5.0, 5.2), tail_segments(4, 3.6, 4.2, 2.6, part_switch=1, taper=0.4), tuning="head", params="heavy", phase=0.0)

    # high_ponytail: tied at the crown
    s = style("high_ponytail")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=2, back=3))
    bangs(s, "center")
    s.box("tie", (-1.6, -9.4, 2.2), (3.2, 1.4, 2.6), inflate=0.15, tone=2, pattern="solid", helm="hide")
    s.chain("tail", (0.0, -9.2, 3.5), tail_segments(4, 3.4, 4.4, 2.6, part_switch=2, taper=0.45), tuning="head", params="heavy", phase=0.3)

    # twin_tails
    s = style("twin_tails")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=4))
    bangs(s, "center")
    for sign, nm in ((1, "L"), (-1, "R")):
        s.box("tie" + nm, (sign * 4.6 - 1.0 if sign > 0 else sign * 4.6 - 1.0, -7.2, 2.0), (2.0, 1.2, 2.4), inflate=0.15, tone=2, pattern="solid", helm="hide", key="tietwin")
    s.chain("tailL", (4.9, -6.6, 3.4), tail_segments(4, 3.0, 4.4, 2.6, part_switch=2, taper=0.45), tuning="head", params="heavy", phase=0.0, shared_key="twin")
    s.chain("tailR", (-4.9, -6.6, 3.4), tail_segments(4, 3.0, 4.4, 2.6, part_switch=2, taper=0.45), tuning="head", params="heavy", phase=2.0, shared_key="twin")

    # drills: ringlets
    s = style("drills")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=4))
    bangs(s, "strands")
    for sign, nm in ((1, "L"), (-1, "R")):
        s.chain("drill" + nm, (sign * 5.0, -6.4, 2.0), [(3.4, 3.6, 3.4, "head", 0.1), (3.2, 3.6, 3.2, "head", 0.14), (2.8, 3.6, 2.8, "body", 0.1), (2.2, 3.6, 2.2, "body", 0.14)],
                tuning="head", params="default", phase=0.0 if sign > 0 else 1.7, pattern="curl", taper_last="round", shared_key="drill", tones=[1, 0, 1, 0])

    # braid
    s = style("braid")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=6))
    bangs(s, "side")
    s.chain("braid", (0.0, -3.0, 4.3), [(3.2, 4.2, 2.6, "head", 0.12), (3.0, 4.2, 2.6, "body", 0.14), (3.0, 4.2, 2.4, "body", 0.12), (2.4, 4.0, 2.0, "body", 0.14)], tuning="head", params="heavy", phase=0.2, pattern="plait", taper_last="point_down")

    # bun
    s = style("bun")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=5))
    bangs(s, "strands")
    s.box("bun", (-2.2, -11.2, 0.6), (4.4, 3.2, 4.0), inflate=0.3, tone=1, pattern="curl", taper="round", helm="hide")
    s.box("knot", (-2.6, -8.9, 0.2), (5.2, 1.0, 4.8), inflate=0.1, tone=-1, pattern="solid", helm="hide")

    # pompadour
    s = style("pompadour")
    s.cap(cap_mask(front=1, side_hi=2, side_lo=3, back=5, notch=()))
    s.box("quiff", (-4.2, -11.0, -5.0), (8.4, 3.6, 4.4), inflate=0.25, tone=1, taper="ends", rot=(-8, 0, 0), pivot=(0, -8.0, -1.0), helm="hide")
    s.box("quiff_top", (-3.6, -11.8, -4.0), (7.2, 1.2, 3.2), inflate=0.2, tone=2, rot=(-8, 0, 0), pivot=(0, -8.0, -1.0), helm="hide")

    # ahoge: a short cut with the one loose strand
    s = style("ahoge")
    s.cap(cap_mask(front=2, side_hi=2, side_lo=3, back=4))
    bangs(s, "strands")
    s.box("stalk", (-0.6, -11.4, -1.6), (1.2, 3.4, 1.2), inflate=0.05, tone=1, rot=(0, 0, 16), pivot=(0, -8.0, -1.0), taper="point_up", helm="hide")
    s.box("hook", (1.2, -13.4, -1.6), (1.2, 2.2, 1.2), inflate=0.05, tone=2, rot=(0, 0, 52), pivot=(0.6, -11.0, -1.0), taper="point_up", helm="hide")

    # mane: a wild lion's mane
    s = style("mane")
    s.cap(cap_mask(front=2, side_hi=4, side_lo=6, back=8, notch=(2, 5)), inflate=0.76)
    fan = [(-3.4, -2.8, -1, -1, 40), (0.0, -3.6, 0, -1, 44), (3.4, -2.8, 1, -1, 40), (-4.2, 0.4, -1, 0, 48), (4.2, 0.4, 1, 0, 48), (-3.4, 3.2, -1, 1, 46), (0.0, 3.8, 0, 1, 50),
           (3.4, 3.2, 1, 1, 46), (-1.6, -0.2, -1, 0, 26), (1.6, -0.4, 1, 0, 24)]
    for i, (x, z, dx, dz, a) in enumerate(fan):
        rx, ry, rz = tilt(dx, dz, a)
        s.box("lock%da" % i, (x - 1.3, -11.0, z - 1.3), (2.6, 3.4, 2.6), inflate=0.14, tone=1 if i % 2 else 0, rot=(rx, 0, rz), pivot=(x, -7.6, z), helm="hide")
        s.box("lock%db" % i, (x - 0.9, -14.0, z - 0.9), (1.8, 3.4, 1.8), inflate=0.08, tone=2 if i % 2 else 1, rot=(rx, 0, rz), pivot=(x, -7.6, z), taper="point_up", helm="hide")
    s.chain("mantle", (0.0, -2.0, 3.8), [(9.4, 5.0, 2.4, "head", 0.3), (10.0, 5.0, 2.6, "body", 0.3), (8.8, 5.0, 2.2, "body", 0.3)], tuning="stiff", params="stiff", phase=0.3)
    s.chain("frontL", (4.7, -5.0, -2.2), [(1.8, 5.0, 2.0, "head", 0.14), (1.8, 3.8, 2.0, "body", 0.14)], tuning="head", params="default", phase=0.8, shared_key="mfront")
    s.chain("frontR", (-4.7, -5.0, -2.2), [(1.8, 5.0, 2.0, "head", 0.14), (1.8, 3.8, 2.0, "body", 0.14)], tuning="head", params="default", phase=2.2, shared_key="mfront")

    # hood: a cloth hood over the whole head with a drape (the hair colour is the cloth's colour)
    s = style("hood")

    def hood_mask(face, lx, ly, lz):
        if face == "front":
            return not (1 <= lx <= 6 and ly >= 2)          # the opening
        if face == "bottom":
            return False
        if face in ("west", "east"):
            return not (lz < 2 and ly >= 3)
        return True

    s.cap(hood_mask, inflate=1.15, helm="hide")
    s.chain("drape", (0.0, -1.0, 3.9), [(9.6, 5.0, 3.0, "head", 0.5), (10.4, 5.0, 3.2, "body", 0.5)], tuning="stiff", params="default", phase=0.2, taper_last="ends")


# ---------------------------------------------------------------------------------------------- anime styles (3.37, "anime skin style v2")
# The Tier List bosses are drawn like the anime Minecraft skins of the owner's reference sheet: a chunky hair mass with a fringe, not a flat painted scalp. These are ADDITIONS: new ids
# (anime_*), appended after the classic 23, with new sheets (clumps, four grey steps, bright highlight pixels; the tint pipeline is unchanged: grey x HairPalette base/highlight/shade).
# Worst case per style: 23 boxes + 14 chain segments = 180 quads (the biggest classic ones: mane 21 + 7 = 168, layered 1 + 13), so the draw cost is the same order as the existing hair.
ANIME_CLASSIC_COPIES = ("mane", "hood")             # the classic cuts that only get an anime sheet (same boxes and chains under the id anime_<id>)


def a_cap(s, front=2, side_hi=3, side_lo=5, back=6, notch=(3, 4), inflate=0.8):
    s.cap(cap_mask(front=front, side_hi=side_hi, side_lo=side_lo, back=back, notch=notch), inflate=inflate)


def a_crown(s, h=2.2, infl=1.25, tone=1):
    """The volume of the head of hair: a slab over the top rows of the head with a jagged lower edge, standing off the cap (inflate 1.25 against the cap's 0.8)."""
    s.box("crown", (-4.0, -8.0, -4.0), (8.0, h, 8.0), inflate=infl, tone=tone, taper="zigzag")


def a_fringe(s, locks, open_brow=False):
    """The fringe: single-segment locks over the front of the head (x, width, length, tone-ish phase); in front of both the cap and the crown. `open_brow` keeps only the two outer locks, so a
    circlet, crown or headband painted on row 2 of the hat layer stays visible under the hair."""
    z = -5.35
    if open_brow:
        locks = [lk for lk in locks if abs(lk[0]) > 3.0]
    for i, (x, w, ln) in enumerate(locks):
        s.chain("fringe%d" % i, (x, -8.2, z - 0.03 * (i % 2)), [(w, ln, 0.9, "head", 0.2 + 0.03 * (i % 2))], tuning="stiff", params="stiff", phase=0.6 * i, helm="hide",
                taper_last="point_down", shared_key="afr%d_%d" % (int(w * 10), int(ln * 10)), tones=[1 if i % 2 else 0])


def a_side_locks(s, length=4.6, w=1.8, depth=3.4, y=-6.2, z=-1.0, taper="point_down"):
    for sign, nm in ((1, "L"), (-1, "R")):
        s.chain("side" + nm, (sign * 4.7, y, z), [(w, length, depth, "head", 0.16)], tuning="head", params="default", phase=0.9 if sign > 0 else 2.1, helm="hide", taper_last=taper,
                shared_key="aside", tones=[1])


def a_nape(s, y=-3.2, w=8.0, ln=3.6, taper="ends", tail=None):
    segs = [(w, ln, 1.6, "head", 0.34)]
    if tail:
        segs.append((w - 1.0, tail, 1.5, "body", 0.34))
    s.chain("nape", (0.0, y, 4.2), segs, tuning="head", params="default", phase=0.4, taper_last=taper)


def a_tufts(s, spec, size=(2.4, 3.2, 2.4), prefix="tuft"):
    """Leaning locks on the crown: (x, z, dx, dz, degrees, tone): boxes standing on the head, tilted outward, pointed."""
    for i, (x, z, dx, dz, a, tone) in enumerate(spec):
        rx, ry, rz = tilt(dx, dz, a)
        w, h, d = size
        s.box("%s%d" % (prefix, i), (x - w / 2, -8.0 - h * 0.78, z - d / 2), (w, h, d), inflate=0.1 + 0.02 * (i % 2), tone=tone, rot=(rx, 0, rz), pivot=(x, -7.8, z), taper="point_up")


def build_anime():
    """Appends the anime styles: sheet-only copies of mane and hood, then the new chunky cuts."""
    saved = dict(_CTX)
    _CTX.update(prefix="anime_", anime=True, only=ANIME_CLASSIC_COPIES)
    try:
        _build_classic()
    finally:
        _CTX.clear()
        _CTX.update(saved)
    _CTX.update(prefix="", anime=False, only=None)

    # anime_short: a thick short cut, side locks over the temples, a jagged back, leaning tufts (Charlos, Ao, Bertholdt, Yammy, Morgan); _open: no centre fringe (a circlet shows)
    for sid, open_brow in (("anime_short", False), ("anime_short_open", True)):
        s = style(sid, anime=True)
        a_cap(s, front=2, side_hi=3, side_lo=5, back=6, notch=(3, 4))
        a_crown(s, 2.4)
        a_fringe(s, ((-3.2, 2.4, 3.0), (-1.0, 2.2, 3.8), (1.2, 2.2, 2.8), (3.3, 2.4, 3.6)), open_brow)
        a_side_locks(s, 4.4, 1.8)
        a_nape(s, -3.4, 8.0, 3.6)
        a_tufts(s, [(-2.6, -2.4, -1, -1, 24, 2), (0.4, -3.0, 0, -1, 28, 1), (2.8, -2.2, 1, -1, 24, 2), (-3.0, 1.4, -1, 0, 26, 1), (3.0, 1.4, 1, 0, 26, 2), (0.0, 2.8, 0, 1, 24, 1)])

    # anime_spiky: big layered spikes, a ragged fringe, long side locks and spikes down the nape (Code; the Meliodas family)
    s = style("anime_spiky", anime=True)
    a_cap(s, front=1, side_hi=3, side_lo=5, back=6, notch=())
    a_crown(s, 2.0, 1.3)
    ring = [(-2.8, -2.8, -1, -1), (0.0, -3.3, 0, -1), (2.8, -2.8, 1, -1), (-3.3, 0.2, -1, 0), (3.3, 0.2, 1, 0), (-2.6, 3.0, -1, 1), (0.0, 3.4, 0, 1), (2.6, 3.0, 1, 1), (0.0, 0.0, 0, 0)]
    for i, (x, z, dx, dz) in enumerate(ring):
        lean = 8 if (dx == 0 and dz == 0) else 27 + 5 * (i % 3)
        rx, ry, rz = tilt(dx, dz, lean)
        h2 = 2.8 + 0.8 * (i % 3)
        s.box("spike%da" % i, (x - 1.7, -10.6, z - 1.7), (3.4, 2.6, 3.4), inflate=0.12, tone=1 if i % 2 else 0, rot=(rx, 0, rz), pivot=(x, -8.0, z), taper="point_up")
        s.box("spike%db" % i, (x - 1.0, -10.6 - h2, z - 1.0), (2.0, h2, 2.0), inflate=0.06, tone=2 if i % 2 else 1, rot=(rx, 0, rz), pivot=(x, -8.0, z), taper="point_up", tip=0.5)
    a_fringe(s, ((-3.4, 2.2, 3.4), (-1.5, 2.0, 4.2), (0.5, 2.0, 3.0), (2.3, 2.2, 4.4), (3.9, 1.8, 3.2)))
    a_side_locks(s, 5.0, 1.9)
    for i, x in enumerate((-2.4, 0.0, 2.4)):
        s.box("nape%d" % i, (x - 1.2, -4.6, 3.6), (2.4, 4.2, 1.6), inflate=0.2, tone=1 if i == 1 else 0, rot=(-22, 0, 0), pivot=(x, -4.6, 4.4), taper="point_down")

    # anime_swept: a voluminous side-swept cut with a long fringe over the eyes, locks at the cheeks and a full back (Light, Tucker; the silver-haired reference)
    s = style("anime_swept", anime=True)
    a_cap(s, front=2, side_hi=3, side_lo=5, back=6, notch=())
    a_crown(s, 2.6, 1.3)
    s.chain("fringeA", (-1.6, -8.2, -5.35), [(5.0, 4.8, 0.9, "head", 0.2)], tuning="stiff", params="stiff", phase=0.2, helm="hide", taper_last="point_down", tones=[0])
    s.chain("fringeB", (2.6, -8.2, -5.38), [(2.6, 4.0, 0.9, "head", 0.23)], tuning="stiff", params="stiff", phase=1.1, helm="hide", taper_last="point_down", tones=[1])
    s.chain("fringeC", (-4.1, -8.2, -5.3), [(1.8, 5.6, 0.9, "head", 0.19)], tuning="stiff", params="stiff", phase=1.8, helm="hide", taper_last="point_down", tones=[1])
    s.chain("fringeD", (4.1, -8.2, -5.3), [(1.8, 4.8, 0.9, "head", 0.19)], tuning="stiff", params="stiff", phase=2.4, helm="hide", taper_last="point_down", tones=[0])
    a_side_locks(s, 5.4, 2.0, y=-6.4)
    a_nape(s, -3.6, 8.2, 3.8, tail=3.0)
    a_tufts(s, [(-1.6, -2.4, -1, -1, 22, 2), (1.8, -2.6, 1, -1, 20, 1), (-3.0, 0.6, -1, 0, 24, 1), (3.0, 0.8, 1, 0, 24, 2), (0.2, 2.8, 0, 1, 22, 2)], size=(2.8, 2.8, 2.8))

    # anime_long: a long full cut: thick crown, a centre fringe, cheek locks to the chest, a heavy back with a second layer (Sogou, Malty, Rachel, Jugram); _open: no centre fringe
    for sid, open_brow in (("anime_long", False), ("anime_long_open", True)):
        s = style(sid, anime=True)
        a_cap(s, front=2, side_hi=3, side_lo=5, back=8, notch=(3, 4))
        a_crown(s, 2.4, 1.25)
        a_fringe(s, ((-3.4, 2.2, 3.6), (-1.3, 2.2, 3.0), (1.3, 2.2, 3.0), (3.5, 2.2, 3.8)), open_brow)
        s.chain("back", (0.0, -2.0, 4.1), [(8.4, 4.5, 1.8, "head", 0.3), (8.8, 4.5, 1.8, "body", 0.32), (8.6, 4.5, 1.8, "body", 0.3), (7.8, 4.0, 1.6, "body", 0.28)], tuning="head",
                params="heavy", phase=0.0, taper_last="ends", tones=[0, 1, 0, 1])
        s.chain("back2", (0.0, -3.0, 4.9), [(6.4, 4.0, 1.3, "head", 0.4), (6.4, 4.0, 1.3, "body", 0.42)], tuning="head", params="default", phase=1.1, taper_last="ends", shared_key="ab2", tones=[1, 0])
        for sign, nm in ((1, "L"), (-1, "R")):
            s.chain("cheek" + nm, (sign * 4.85, -6.2, -2.5), [(1.9, 5.4, 2.0, "head", 0.14), (1.9, 4.6, 1.8, "body", 0.14)], tuning="head", params="default", phase=0.8 if sign > 0 else 2.2,
                    shared_key="acheek", tones=[1, 0])

    # anime_afro: a big round puff (Mr. 2 Bon Clay, Foxy)
    s = style("anime_afro", anime=True)
    a_cap(s, front=2, side_hi=4, side_lo=5, back=7, notch=(3, 4))
    s.box("puff", (-5.4, -12.8, -5.4), (10.8, 7.4, 10.8), inflate=0.3, tone=0, taper="zigzag")
    s.box("puffTop", (-4.4, -14.6, -4.4), (8.8, 2.4, 8.8), inflate=0.25, tone=1, taper="zigzag")
    s.box("puffL", (5.2, -11.0, -3.4), (2.2, 5.4, 6.8), inflate=0.2, tone=1, taper="zigzag", key="apuffside")
    s.box("puffR", (-7.4, -11.0, -3.4), (2.2, 5.4, 6.8), inflate=0.2, tone=1, taper="zigzag", key="apuffside")
    a_fringe(s, ((-2.6, 2.4, 2.4), (0.0, 2.4, 2.8), (2.6, 2.4, 2.4)))

    # anime_topknot: the thick short cut with a tied topknot (Kurozumi Orochi)
    s = style("anime_topknot", anime=True)
    a_cap(s, front=2, side_hi=3, side_lo=5, back=6, notch=(3, 4))
    a_crown(s, 2.4)
    a_fringe(s, ((-3.3, 2.4, 3.0), (-1.1, 2.2, 3.6), (1.2, 2.2, 3.0), (3.4, 2.4, 3.4)))
    a_side_locks(s, 4.2, 1.7)
    a_nape(s, -3.4, 8.0, 3.4)
    s.box("knotTie", (-2.6, -10.2, -0.6), (5.2, 1.0, 5.2), inflate=0.15, tone=-1, pattern="solid", helm="hide")
    s.box("knot", (-2.2, -13.4, 0.0), (4.4, 3.4, 4.0), inflate=0.3, tone=1, taper="round", helm="hide")
    s.box("knotTop", (-1.6, -14.8, 0.4), (3.2, 1.6, 3.2), inflate=0.2, tone=2, taper="round", helm="hide")



def main():
    build()
    for sid in ORDER:
        STYLES[sid].save_sheet(OUT_SHEETS)
    write_styles(STYLES, ORDER, OUT_JSON, meta={"note": "generated by tools/playermodel/gen_hair.py; do not edit by hand"})
    boxes = sum(len(STYLES[s].boxes) for s in ORDER)
    chains = sum(len(STYLES[s].chains) for s in ORDER)
    print("wrote %d styles, %d boxes, %d chains -> %s" % (len(ORDER), boxes, chains, os.path.relpath(OUT_JSON, ASSETS)))


if __name__ == "__main__":
    main()
