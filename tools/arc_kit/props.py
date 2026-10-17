"""Why: weapons, books, orbs and shields held by a character are the part of a model the player looks at most, and every one of them needs a hand grip, a glow and
a pose that suits it. This module builds the props from a few numbers (colour, accent, glow, length) and tells the clip generator how each one is held: which
hand, one or two hands (and where the second hand grips, so it can be solved with inverse kinematics), which attack it swings, how it is tilted in the fist.

A prop is authored standing up (the grip at the origin, the long axis along +y, flat faces towards +-z) and the bone `prop_r` / `prop_l` (a child of the hand bone) turns
it by `grip` so that at rest the long axis points forward out of the fist. To add a prop: write `p_<name>(pb)` with the `PB` helpers, add a `PropDef` row to PROPS,
and a line to docs/arc/art_kit.md.
"""
import math

from . import color as C
from .core import SIDES, blade_paint, fabric, leather, metal, plate, bandage_paint, bone_paint, px, mix, hsh, solid, canvas_plate, Canvas
from .rig import rot_file, r5

SIN, COS = math.sin, math.cos


class PropDef:
    """How one kind of prop is built and held.
    hand: the hand that holds it ('r' or 'l'); a shield is strapped to the forearm of that side instead; two: whether the other hand also grips it;
    grip2: where the other hand grips, in prop-local pixels (x, y, z); attack: the swing style of the clips; grip: the bone rotation that points the long axis forward;
    cast: the cast style its holder uses; orient: (local vector, mode) which side of the prop (its cutting edge, the string of a bow, the pages of a book) must face where
    (anim.side_dir has the modes). Where the prop rests in the hand and how it swings is in clips.HOLD and recipes.py."""

    def __init__(self, build, hand="r", two=False, grip2=(0.0, 12.0, 0.0), attack="slash", grip=(-90.0, 0.0, 0.0), cast="gesture", orient=None):
        self.build, self.hand, self.two, self.grip2, self.attack, self.grip, self.cast, self.orient = build, hand, two, grip2, attack, grip, cast, orient


class PB:
    """Prop builder: boxes in prop-local coordinates (the grip at the origin, +y up the long axis)."""

    def __init__(self, R, bone, G, P, scale, glow_on=True):
        self.R, self.bone, self.G, self.P, self.s = R, bone, G, P, scale
        self.glow = C.rgb(P["glow"]) if (P.get("glow") and glow_on) else None
        self.col = P.get("color")
        self.col2 = P.get("color2")
        self.n = 0

    def L(self, v):
        """A length scaled by the character's size and the spec's length."""
        return r5(v * self.s * self.P.get("length", 1.0))

    def pt(self, x, y, z):
        return (self.G[0] + x, self.G[1] + y, self.G[2] + z)

    def box(self, x, y, z, sx, sy, sz, paint, rot=None, pivot=None, **kw):
        piv = None if pivot is None else self.pt(*pivot)
        return self.R.box(self.bone, self.pt(x, y, z), (sx, sy, sz), paint, rotation=rot, pivot=piv, **kw)

    def steel(self, salt=1, edge=None):
        cols = C.cloth_palette(self.col or "#aeb8c8", 5, 1.25)
        g = None if self.glow is None else (self.glow[0], self.glow[1], self.glow[2], 200)
        return blade_paint(cols, edge=edge, glow=g, salt=salt)

    def metal(self, color=None, salt=2, glow=None):
        return metal(C.cloth_palette(color or self.col2 or "#c8a040", 5, 1.2), salt, glow=glow)

    def leather(self, color="#4a3020", salt=3):
        return leather(C.cloth_palette(color, 5, 0.9), salt)

    def wrap(self, color=None, salt=4):
        return bandage_paint(C.cloth_palette(color or "#3a2a22", 5, 0.9), salt, slope=1.0, gap=3.0)

    def curve(self, start, segs, w, paint, plane="xy", thick=0.8, taper=1.0, pivot_origin=True):
        """A chain of boxes along a bend (scythe blades, bow limbs, curved swords): start = (x, y, z) in the prop frame; segs = [(length, angle_deg)] where the angle is
        the TURN added to the direction before the segment (0 = straight up the +y axis; positive turns toward +x). Each box is `w` wide (x) and `thick` deep (z)."""
        x, y, z = start
        ang = 0.0
        width = w
        out = []
        for (ln, turn) in segs:
            ang += turn
            ln = max(0.5, r5(ln))
            wd = max(0.5, r5(width))
            c = self.box(x - wd / 2, y, z - thick / 2, wd, ln, thick, paint, rot=(0, 0, -ang), pivot=(x, y, z))
            d = rot_file((0, ln, 0), 0, 0, -ang)
            x, y, z = x + d[0], y + d[1], z + d[2]
            width *= taper
            out.append(c)
        return (x, y, z), ang


def _gcol(pb, a=210):
    return None if pb.glow is None else (pb.glow[0], pb.glow[1], pb.glow[2], a)


# ------------------------------------------------------------------------------------------------------------------------ the props
def p_sword(pb):
    L = pb.L(22.0)
    pb.box(-0.6, -2.5, -0.6, 1.2, 5.0, 1.2, pb.leather("#4a3020"))                       # grip
    pb.box(-0.9, -3.3, -0.9, 1.8, 1.0, 1.8, pb.metal())                                   # pommel
    pb.box(-3.5, 2.4, -0.9, 7.0, 1.0, 1.8, pb.metal())                                    # crossguard
    pb.box(-1.3, 3.4, -0.5, 2.6, L, 1.0, pb.steel(1))
    pb.box(-0.8, 3.4 + L, -0.4, 1.6, 2.0, 0.8, pb.steel(2))
    pb.box(-0.35, 3.4 + L + 2.0, -0.3, 0.7, 1.0, 0.6, pb.steel(3))


def p_katana(pb):
    L = pb.L(24.0)
    pb.box(-0.7, -4.0, -0.7, 1.4, 7.5, 1.4, pb.wrap("#1c1c24"))                          # tsuka wrapped in cord
    pb.box(-0.9, -4.8, -0.9, 1.8, 1.0, 1.8, pb.metal("#8c98a8"))
    pb.box(-1.9, 3.5, -1.9, 3.8, 0.8, 3.8, pb.metal("#c8a040", 5))                        # tsuba
    start = pb.pt(0, 4.3, 0)
    segs = ((L * 0.36, 0.0), (L * 0.34, 2.5), (L * 0.22, 3.0), (L * 0.08, 5.0))
    x, y, z = 0.0, 4.3, 0.0
    ang = 0.0
    for k, (ln, turn) in enumerate(segs):
        ang += turn
        ln = max(0.5, r5(ln))
        wd = 1.6 - 0.15 * k
        pb.box(x - 0.8 + 0.0, y, z - 0.4, max(0.5, wd), ln, 0.8, pb.steel(10 + k, edge=None), rot=(0, 0, -ang), pivot=(x, y, z))
        d = rot_file((0, ln, 0), 0, 0, -ang)
        x, y, z = x + d[0], y + d[1], z + d[2]


def p_dagger(pb):
    L = pb.L(10.0)
    pb.box(-0.5, -2.0, -0.5, 1.0, 4.0, 1.0, pb.leather("#2a2a30"))
    pb.box(-2.0, 1.8, -0.7, 4.0, 0.8, 1.4, pb.metal())
    pb.box(-1.0, 2.6, -0.4, 2.0, L, 0.8, pb.steel(1))
    pb.box(-0.5, 2.6 + L, -0.3, 1.0, 1.5, 0.6, pb.steel(2))


def p_greatsword(pb):
    L = pb.L(36.0)
    pb.box(-0.8, -5.0, -0.8, 1.6, 10.0, 1.6, pb.leather("#2a2024"))
    pb.box(-1.4, -6.4, -1.4, 2.8, 1.6, 2.8, pb.metal())
    pb.box(-6.0, 5.0, -1.2, 12.0, 1.4, 2.4, pb.metal())
    pb.box(-2.6, 6.4, -0.7, 5.2, L, 1.4, pb.steel(1))
    pb.box(-1.6, 6.4 + L, -0.6, 3.2, 3.0, 1.2, pb.steel(2))
    pb.box(-0.6, 6.4 + L + 3.0, -0.4, 1.2, 1.5, 0.8, pb.steel(3))


def p_spear(pb):
    L = pb.L(46.0)
    wood = pb.leather(pb.col2 or "#6a4a2c", 5)
    pb.box(-0.6, -14.0, -0.6, 1.2, L, 1.2, wood)
    pb.box(-1.0, -14.2, -1.0, 2.0, 1.2, 2.0, pb.metal("#8c98a8"))
    top = -14.0 + L
    pb.box(-1.1, top - 3.0, -1.1, 2.2, 3.0, 2.2, pb.metal("#8c98a8"))
    pb.box(-1.8, top, -0.5, 3.6, 5.0, 1.0, pb.steel(1))
    pb.box(-1.0, top + 5.0, -0.4, 2.0, 3.0, 0.8, pb.steel(2))
    pb.box(-0.4, top + 8.0, -0.3, 0.8, 1.5, 0.6, pb.steel(3))
    pb.box(-0.4, top - 4.0, -2.2, 0.8, 4.0, 1.4, fabric(C.shades("#b02424", 5, 0.9), 6, period=4.0, seam=False))


def p_trident(pb):
    L = pb.L(46.0)
    wood = pb.leather(pb.col2 or "#5a4a3c", 5)
    pb.box(-0.6, -14.0, -0.6, 1.2, L, 1.2, wood)
    top = -14.0 + L
    pb.box(-3.0, top, -0.6, 6.0, 1.2, 1.2, pb.steel(1))
    for x in (-3.0, -0.5, 2.0):
        pb.box(x, top + 1.0, -0.5, 1.0, 5.0, 1.0, pb.steel(2))
        pb.box(x + 0.25, top + 6.0, -0.3, 0.5, 1.5, 0.6, pb.steel(3))


def p_staff(pb):
    L = pb.L(46.0)
    wood = pb.leather(pb.col2 or "#5a3c28", 5)
    pb.box(-0.7, -16.0, -0.7, 1.4, L, 1.4, wood)
    top = -16.0 + L
    g = _gcol(pb, 230)
    pb.box(-1.4, top - 2.0, -1.4, 2.8, 1.2, 2.8, pb.metal())
    # a crescent cradle of three boxes round a glowing crystal
    pb.box(-2.4, top - 0.8, -0.7, 1.0, 5.0, 1.4, pb.metal())
    pb.box(1.4, top - 0.8, -0.7, 1.0, 5.0, 1.4, pb.metal())
    pb.box(-1.4, top + 3.6, -0.7, 2.8, 1.0, 1.4, pb.metal())
    gem = C.shades(pb.glow or (120, 200, 255), 5, 1.1)
    pb.box(-1.0, top + 0.4, -1.0, 2.0, 3.0, 2.0, metal(gem, 7, glow=g if g else (120, 200, 255, 220), emboss=False, rim=False))


def p_wand(pb):
    pb.box(-0.5, -3.0, -0.5, 1.0, 9.0, 1.0, pb.leather(pb.col2 or "#3a2820", 5))
    g = _gcol(pb, 240)
    gem = C.shades(pb.glow or (255, 200, 120), 5, 1.1)
    pb.box(-0.8, 6.0, -0.8, 1.6, 1.6, 1.6, metal(gem, 7, glow=g if g else (255, 200, 120, 220), emboss=False, rim=False))


def p_axe(pb):
    L = pb.L(26.0)
    pb.box(-0.7, -6.0, -0.7, 1.4, L, 1.4, pb.leather(pb.col2 or "#5a3c28", 5))
    top = -6.0 + L
    pb.box(-1.0, top - 6.0, -1.1, 2.0, 7.0, 2.2, pb.metal("#8c98a8"))
    pb.box(-6.0, top - 5.0, -0.6, 5.0, 5.0, 1.2, pb.steel(1))                              # the blade (to the model's right)
    pb.box(-7.5, top - 6.0, -0.5, 1.5, 7.0, 1.0, pb.steel(2))
    pb.box(1.0, top - 2.5, -0.6, 1.8, 2.5, 1.2, pb.steel(3))                              # the spike behind


def p_hammer(pb):
    L = pb.L(26.0)
    pb.box(-0.7, -6.0, -0.7, 1.4, L, 1.4, pb.leather(pb.col2 or "#5a3c28", 5))
    top = -6.0 + L
    g = _gcol(pb, 200)
    pb.box(-4.5, top - 3.0, -3.0, 9.0, 6.0, 6.0, plate(C.cloth_palette(pb.col or "#8c98a8", 5, 1.25), 3, glow=g))
    pb.box(-5.0, top - 2.5, -2.5, 0.8, 5.0, 5.0, pb.metal())
    pb.box(4.2, top - 2.5, -2.5, 0.8, 5.0, 5.0, pb.metal())


def p_mace(pb):
    L = pb.L(22.0)
    pb.box(-0.7, -5.0, -0.7, 1.4, L, 1.4, pb.leather(pb.col2 or "#3a2a22", 5))
    top = -5.0 + L
    head = plate(C.cloth_palette(pb.col or "#6c7482", 5, 1.2), 3, glow=_gcol(pb, 200))
    pb.box(-2.5, top - 1.0, -2.5, 5.0, 5.0, 5.0, head)
    for (x, y, z, sx, sy, sz) in ((-3.5, 0.5, -0.75, 1.0, 2.0, 1.5), (2.5, 0.5, -0.75, 1.0, 2.0, 1.5), (-0.75, 0.5, -3.5, 1.5, 2.0, 1.0), (-0.75, 0.5, 2.5, 1.5, 2.0, 1.0),
                                  (-0.75, 4.0, -0.75, 1.5, 1.5, 1.5)):
        pb.box(x, top - 1.0 + y, z, sx, sy, sz, pb.metal("#8c98a8"))


def p_scythe(pb):
    L = pb.L(50.0)
    pb.box(-0.7, -16.0, -0.7, 1.4, L, 1.4, pb.leather(pb.col2 or "#2a2024", 5))
    pb.box(-1.2, -16.6, -1.2, 2.4, 1.2, 2.4, pb.metal("#5c6270"))
    top = -16.0 + L
    pb.box(-1.4, top - 4.0, -1.4, 2.8, 5.0, 2.8, pb.metal("#5c6270"))
    blade = pb.steel(1)
    # the blade: starts at the head of the shaft, sweeps out to the model's right and then curls down, ending in a point
    x, y, z = 0.0, top + 0.8, 0.0
    segs = ((5.0, -78.0), (6.0, -14.0), (6.0, -16.0), (5.0, -18.0), (4.0, -20.0), (3.0, -22.0))
    ang = 0.0
    w = 4.2
    for k, (ln, turn) in enumerate(segs):
        ang += turn
        ln = r5(ln * pb.s)
        pb.box(x - w / 2, y, z - 0.45, w, ln, 0.9, blade, rot=(0, 0, -ang), pivot=(x, y, z))
        d = rot_file((0, ln, 0), 0, 0, -ang)
        x, y, z = x + d[0], y + d[1], z + d[2]
        w = max(0.5, w * 0.78)


def p_bow(pb):
    """A bow standing up in the hand: the grip at the origin, two limbs of three boxes that both curve towards +x, the string joining the two tips on the +x side.
    The string side is the side that faces the archer (PropDef.orient puts local +x toward the shooter)."""
    s = pb.s
    wood = pb.leather(pb.col or "#6a4a2c", 5)
    grip = pb.wrap("#2a2024")
    pb.box(-0.8, -3.0, -0.8, 1.6, 6.0, 1.6, grip)
    tips = []
    for sgn in (1, -1):
        x, y, ang = 0.0, 3.0 * sgn, 0.0
        for k, (ln, turn) in enumerate(((5.0, 12.0), (4.5, 16.0), (3.5, 22.0))):
            ang += turn
            ln = r5(ln * s)
            w = 1.4 - 0.2 * k
            if sgn > 0:
                pb.box(x - w / 2, y, -0.6, w, ln, 1.2, wood, rot=(0, 0, -ang), pivot=(x, y, 0))
                d = rot_file((0, ln, 0), 0, 0, -ang)
            else:
                pb.box(x - w / 2, y - ln, -0.6, w, ln, 1.2, wood, rot=(0, 0, ang), pivot=(x, y, 0))
                d = rot_file((0, -ln, 0), 0, 0, ang)
            x, y = x + d[0], y + d[1]
        tips.append((x, y))
    (tx, ty), (bx, by) = tips
    pb.box(min(tx, bx) - 0.2, by, -0.15, 0.4, ty - by, 0.3, solid((226, 220, 200)))      # the string


def p_grimoire(pb):
    cov = C.cloth_palette(pb.col or "#5a2430", 5, 1.0)
    cover = leather(cov, 7)
    pages = fabric(C.shades("#e8dcc0", 5, 0.6), 8, period=3.0, amp=0.1, seam=False)
    g = _gcol(pb, 230)
    # an open book standing in the hand, the spine at the grip: two covers with pages between, a glowing rune on the left page
    rune = Canvas(16, 20)
    rg = tuple(pb.glow) if pb.glow else (255, 170, 90)
    for (x, y) in ((8, 4), (7, 5), (9, 5), (6, 6), (10, 6), (7, 8), (9, 8), (8, 9), (8, 10), (6, 11), (10, 11), (7, 13), (9, 13), (8, 15)):
        rune.put(x, y, (mix(rg, (255, 255, 255), 0.4)), 255, rg, 255)

    def page_paint(face, i, j, fw, fh):
        if face in ("front", "back"):
            c, gg = rune.at(i * 16 // max(1, fw), j * 20 // max(1, fh))
            if c is not None and pb.glow is not None:
                return c, gg
        return pages(face, i, j, fw, fh)
    pb.box(-4.5, -4.5, -0.9, 0.8, 9.5, 1.8, cover, faces=SIDES + ("top", "bottom"))
    pb.box(-4.0, -4.0, -0.5, 4.0, 8.5, 0.8, page_paint)
    pb.box(0.2, -4.0, -0.5, 4.0, 8.5, 0.8, page_paint)
    pb.box(3.7, -4.5, -0.9, 0.8, 9.5, 1.8, cover, faces=SIDES + ("top", "bottom"))
    pb.box(-4.5, -4.8, -0.9, 9.3, 0.8, 1.8, cover)


def p_orb(pb):
    g = _gcol(pb, 235)
    gem = C.shades(pb.glow or (120, 200, 255), 5, 1.1)
    p = metal(gem, 5, glow=g if g else (120, 200, 255, 230), emboss=False, rim=False)
    pb.box(-2.0, 1.5, -1.2, 4.0, 3.0, 2.4, p)
    pb.box(-1.2, 1.5, -2.0, 2.4, 3.0, 4.0, p)
    pb.box(-1.2, 0.8, -1.2, 2.4, 4.4, 2.4, p)
    pb.box(-1.6, -0.4, -1.6, 3.2, 1.2, 3.2, pb.metal())


def p_shield(pb):
    """Strapped to the forearm (see build_props): a heater shield on the outer side of the arm, turned 35 degrees toward the front so it faces the viewer; the built shape is
    for the model's left arm, a shield on the right arm is mirrored."""
    d = pb.R.d
    s = pb.s
    body = plate(C.cloth_palette(pb.col or "#6c7482", 5, 1.2), 9, glow=_gcol(pb, 200), trim=C.cloth_palette(pb.col2 or "#c8a040", 5, 1.2))
    boss = metal(C.cloth_palette(pb.col2 or "#c8a040", 5, 1.2), 6, glow=_gcol(pb, 220))
    h = r5(15.0 * s)
    w = r5(10.0 * s)
    ax = d.arm_x
    fw = max(2.0, d.aw - 0.5)
    ox = ax + fw / 2 + 1.0
    oy = (d.elbow_y + d.wrist_y) / 2.0
    cz = -d.ad / 2 - 0.5
    piv = (ox, oy, cz)
    rot = (0, -35, 0)
    top = oy + h * 0.5
    pb.R.box(pb.bone, (ox - 0.8, oy - h * 0.2, cz - w / 2), (1.6, h * 0.7, w), body, rotation=rot, pivot=piv)
    pb.R.box(pb.bone, (ox - 0.8, oy - h * 0.5, cz - w / 2 + w * 0.15), (1.6, h * 0.3, w * 0.7), body, rotation=rot, pivot=piv)
    pb.R.box(pb.bone, (ox - 0.8, oy - h * 0.62, cz - w / 2 + w * 0.32), (1.6, h * 0.14, w * 0.36), body, rotation=rot, pivot=piv)
    pb.R.box(pb.bone, (ox - 1.4, oy - 1.5, cz - 1.5), (0.7, 3.0, 3.0), boss, rotation=rot, pivot=piv)


PROPS = {
    "sword": PropDef(p_sword, attack="slash", orient=((1.0, 0.0, 0.0), "edge")),
    "katana": PropDef(p_katana, attack="slash", orient=((-1.0, 0.0, 0.0), "edge")),
    "dagger": PropDef(p_dagger, attack="stab", orient=((1.0, 0.0, 0.0), "edge")),
    "greatsword": PropDef(p_greatsword, two=True, grip2=(0.0, 7.0, 0.0), attack="overhead", orient=((1.0, 0.0, 0.0), "edge")),
    "spear": PropDef(p_spear, two=True, grip2=(0.0, 6.5, 0.0), attack="thrust", orient=((0.0, 0.0, 1.0), "forward")),
    "trident": PropDef(p_trident, two=True, grip2=(0.0, 6.5, 0.0), attack="thrust", orient=((0.0, 0.0, 1.0), "forward")),
    "staff": PropDef(p_staff, attack="cast_jab", cast="magic", orient=((0.0, 0.0, 1.0), "forward")),
    "wand": PropDef(p_wand, attack="cast_jab", cast="magic"),
    "axe": PropDef(p_axe, attack="smash", orient=((-1.0, 0.0, 0.0), "forward")),
    "hammer": PropDef(p_hammer, attack="smash"),
    "mace": PropDef(p_mace, attack="smash"),
    "scythe": PropDef(p_scythe, two=True, grip2=(0.0, 7.0, 0.0), attack="sweep", orient=((-1.0, 0.0, 0.0), "forward")),
    "bow": PropDef(p_bow, hand="l", two=True, attack="shoot", grip=(-90.0, 0.0, 0.0), orient=((1.0, 0.0, 0.0), "back")),
    "grimoire": PropDef(p_grimoire, hand="l", attack="cast_jab", cast="book", grip=(-90.0, 0.0, 0.0), orient=((0.0, 0.0, -1.0), "forward")),
    "orb": PropDef(p_orb, attack="cast_jab", cast="magic", grip=(-90.0, 0.0, 0.0)),
    "shield": PropDef(p_shield, hand="l", attack="slash"),
}
PROP_KINDS = tuple(PROPS)
ATTACH = {"r": "hand_r", "l": "hand_l"}
# the blades that can hang in a scabbard at the hip (a spec says sheathed: true): kind -> (where the blade starts above the grip, its length, the scabbard's width and depth)
SHEATHABLE = ("sword", "katana", "dagger")
SHEATH = {"sword": (3.0, 25.5, 3.6, 1.8), "katana": (4.0, 25.0, 2.6, 1.8), "dagger": (2.4, 12.0, 2.6, 1.6)}


def _scale(R):
    return max(0.6, min(2.4, R.d.top / 32.0))


def build_props(R, S, Lk):
    """Adds the props of the `props` section. Facts for the clips: R.facts['props'] = {slot: PropDef} (only what is held in a hand), R.facts['sheathed'] = [slots]."""
    R.facts["props"] = {}
    R.facts["glows"] = []
    d = R.d
    sc = _scale(R)
    R.facts["sheathed"] = []
    for slot, P in S["props"].items():
        pd = PROPS[P["kind"]]
        if P["sheathed"]:
            _build_sheathed(R, S, P, pd, sc, slot)
            R.facts["sheathed"].append(slot)
            continue
        if slot in ("right", "left"):
            hand = "r" if slot == "right" else "l"
        elif slot == "back":
            hand = None
        else:
            hand = None
        if slot in ("right", "left"):
            if P["kind"] == "shield":
                bone = "prop_l" if slot == "left" else "prop_r"
                parent = "forearm_" + hand
                pivot = (( -1 if hand == "r" else 1) * d.arm_x, d.elbow_y, 0)
                R.bone(bone, parent, pivot)
                pb = PB(R, bone, (0, 0, 0), P, sc)
                if hand == "r":
                    _build_mirrored(R, pb, pd, S)
                else:
                    pd.build(pb)
                R.facts["props"][slot] = pd
                continue
            bone = "prop_" + hand
            G = ((-1 if hand == "r" else 1) * d.arm_x, d.wrist_y - d.hand * 0.5, 0.0)
            gr = P["grip"] or pd.grip
            R.bone(bone, "hand_" + hand, G, rotation=tuple(gr))
            pb = PB(R, bone, G, P, sc)
            pd.build(pb)
            R.facts["props"][slot] = pd
            if pb.glow is not None:
                R.facts["glows"].append(slot)
        elif slot == "back":
            _build_back(R, S, P, pd, sc)
            R.facts["props"][slot] = pd
        elif slot == "float":
            _build_float(R, S, P, pd, sc)
            R.facts["props"][slot] = pd


def _build_mirrored(R, pb, pd, S):
    """A shield on the model's right arm: built for the left side, then mirrored (x -> -x) cube by cube."""
    pb2 = PB(R, "prop_tmp", (0, 0, 0), pb.P, pb.s)
    R.bone("prop_tmp", "forearm_r", (-R.d.arm_x, R.d.elbow_y, 0))
    pd.build(pb2)
    cubes = R.m.bones.pop("prop_tmp")["cubes"]
    R.m.order.remove("prop_tmp")
    for c in cubes:
        sx = c["size"][0]
        o = c["origin"]
        t = R.m.cube(pb.bone, (-(o[0] + sx), o[1], o[2]), tuple(c["size"]), uv=(0, 0), inflate=c.get("inflate", 0.0), mirror=True)
        R.pk.links.append((t, c))                      # the original cubes were queued for painting on a bone that is gone: they still paint (their uv is set on the cube dict)


def _build_sheathed(R, S, P, pd, sc, slot):
    """A blade in its scabbard at the hip: the whole weapon is built on the body bone, tilted back along the hip (a right-hand weapon hangs on the model's left, so the
    right hand draws it across the body), with a lacquered scabbard box over the blade; only the hilt shows."""
    d = R.d
    side = 1.0 if slot == "right" else -1.0
    bone = "prop_hip_" + ("l" if side > 0 else "r")
    G = (side * (d.tw / 2 + 1.6), d.hip_y + 1.5, -1.0)
    R.bone(bone, "body", G, rotation=(104.0, 0.0, -side * 8.0))
    pb = PB(R, bone, G, P, sc)
    pd.build(pb)
    y0, ln, w, th = SHEATH[P["kind"]]
    ln = r5(ln * sc * P.get("length", 1.0))
    lac = leather(C.cloth_palette(P["color2"] or "#1c1c24", 5, 0.9), 14)
    pb.box(-w / 2, y0 + 0.2, -th / 2, w, ln, th, lac)
    pb.box(-w / 2 - 0.2, y0 + 0.1, -th / 2 - 0.2, w + 0.4, 1.4, th + 0.4, metal(C.cloth_palette("#c8a040", 5, 1.2), 15, emboss=False))
    pb.box(-w / 2 - 0.2, y0 + ln - 1.2, -th / 2 - 0.2, w + 0.4, 1.6, th + 0.4, metal(C.cloth_palette("#c8a040", 5, 1.2), 16, emboss=False))


def _build_back(R, S, P, pd, sc):
    """A weapon stowed across the back: the same model, carried on the body, tilted along the back."""
    d = R.d
    bone = "prop_back"
    G = (0.0, d.hip_y + d.torso_h * 0.45, d.td / 2 + 1.8)
    R.bone(bone, "body", G, rotation=(0.0, 0.0, 35.0 if pd.hand == "r" else -35.0))
    pb = PB(R, bone, G, P, sc)
    pd.build(pb)


def _build_float(R, S, P, pd, sc):
    """An object floating beside the character (a grimoire, an orb, a sword): it orbits slowly in the clips."""
    d = R.d
    bone = "prop_float"
    G = (-d.tw / 2 - 6.0, d.hip_y + d.torso_h * 0.9, -2.0)
    R.bone(bone, "root", G)
    pb = PB(R, bone, G, P, sc)
    pd.build(pb)
