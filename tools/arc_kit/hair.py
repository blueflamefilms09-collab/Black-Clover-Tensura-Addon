"""Why: hair is most of a character's silhouette, and thirty hand-built hair models would be thirty days of work. A hair style here is a small recipe of a few
reusable pieces (a shell over the scalp, rings of standing flame-shaped tufts, hanging locks, a long back slab, ponytails, buns, braids, a hood, a mane) plus a
fringe that is painted onto the face canvas (before and after the eyes), and a mask that says which texels of the head box itself show hair. All of it is
coloured from one hair palette (and an optional streak colour).

STYLES is the vocabulary (`HAIR_STYLES` lists the names); a spec says `hair={"style": "spiky", "color": "#101014"}` and nothing else is required.
"""
import math

from .core import clump, hair_paint, fabric, hsh, px, mix
from .rig import r5

SIN, COS = math.sin, math.cos


# ------------------------------------------------------------------------------------------------------------------------ fringes (painted on the face canvas)
def _lock(cv, H, x0, x1, tipx, tipy, base):
    """One hanging lock: a triangle from the hair line to a tip, with a dark centre strand and a pale side strand."""
    cv.poly([(x0, base), (x1, base), (tipx, tipy)], H[1])
    mx = (x0 + x1) / 2.0
    cv.line(mx, base, tipx, tipy - 0.4, H[0])
    cv.line((x0 + mx) / 2.0, base, (tipx + x0) / 2.0 + 0.5, (tipy + base) / 2.0, H[2])


def _cap(cv, H, rows, ragged=0):
    cv.rect(0, 0, 31, rows, H[1])
    for x in range(0, 32, 3):
        cv.put(x, rows, H[0])
        cv.put(x + 1, max(0, rows - 1), H[2])
    if ragged:
        for x in range(1, 31):
            if hsh(x, rows, 3) > 0.55:
                cv.put(x, rows + 1, H[1])
    for x in range(3, 30):
        if x % 3 != 1:
            cv.put(x, 1, H[3])


def _temples(cv, H, ly, ry):
    """The side locks beside the eyes: down to row ly on the viewer's left, ry on the right."""
    cv.poly([(0, 4), (5, 4), (4, ly - 3), (2, ly), (0, ly - 2)], H[1])
    cv.poly([(26, 4), (31.5, 4), (31.5, ry - 2), (29, ry), (27, ry - 4)], H[1])
    cv.line(2, 5, 2, ly - 2, H[0])
    cv.line(29, 5, 29, ry - 2, H[0])
    cv.line(3, 5, 3, ly - 5, H[2])
    cv.line(28, 5, 28, ry - 5, H[2])


def fringe_none(cv, stage, H):
    pass


def fringe_cap(cv, stage, H, rows=3):
    if stage == 0:
        _cap(cv, H, rows)
        cv.poly([(0, rows), (3, rows), (2, rows + 5), (0, rows + 6)], H[1])
        cv.poly([(28, rows), (31.5, rows), (31.5, rows + 6), (29, rows + 5)], H[1])


def fringe_short(cv, stage, H, drop=1.0):
    if stage == 0:
        _cap(cv, H, 4)
        _temples(cv, H, 15, 15)
        for a, b, tx, ty in ((5, 10.5, 7, 11), (10, 16, 13.5, 9.5), (15.5, 21, 18, 9), (20.5, 26.5, 24, 11)):
            _lock(cv, H, a, b, tx, 4 + (ty - 4) * drop, 4)


def fringe_spiky(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 3)
        _temples(cv, H, 14, 13)
        tips = ((3, 8, 5, 10), (7, 13, 9, 12), (11, 17, 14, 8), (15, 21, 17, 11), (19, 25, 22, 9), (23, 28, 26, 11), (26, 31, 29, 8))
        for a, b, tx, ty in tips:
            _lock(cv, H, a, b, tx, ty, 3)


def fringe_sweep(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 4)
        cv.poly([(0, 4), (31, 4), (31, 6), (22, 9), (14, 11), (6, 13), (2, 15), (0, 15)], H[1])
        for k in range(5):
            cv.line(26 - k * 4, 5, 18 - k * 4, 10 + k, H[0])
        cv.line(27, 5, 19, 9, H[3])
        cv.line(21, 6, 12, 11, H[2])
        cv.poly([(26, 4), (31.5, 4), (31.5, 12), (29, 10)], H[1])


def fringe_side_part(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 4)
        cv.poly([(0, 4), (12, 4), (14, 6), (8, 8), (3, 11), (0, 13)], H[1])
        cv.poly([(26, 4), (31.5, 4), (31.5, 12), (29, 9)], H[1])
        cv.line(11, 0, 14, 5, H[0])
        for k in range(4):
            cv.line(2 + k * 3, 5, 5 + k * 2, 9 + (k % 2), H[0])
        cv.line(9, 5, 4, 10, H[3])


def fringe_center_part(cv, stage, H, reach=14):
    if stage == 0:
        _cap(cv, H, 3)
        cv.poly([(0, 3), (15, 3), (13, 6), (9, 10), (6, reach), (2, reach + 5), (0, reach + 7)], H[1])
        cv.poly([(31.5, 3), (16, 3), (18, 6), (22, 10), (25, reach), (29, reach + 5), (31.5, reach + 7)], H[1])
        for k in range(5):
            cv.line(2 + k * 2.4, 4, 1 + k * 1.2, reach + 3 - k, H[0])
            cv.line(29 - k * 2.4, 4, 30 - k * 1.2, reach + 3 - k, H[0])
        cv.line(14, 3, 12, 6, H[3])
        cv.line(17, 3, 19, 6, H[3])
        cv.line(15.5, 0, 15.5, 3, H[0])


def fringe_bangs(cv, stage, H, rows=8):
    if stage == 0:
        _cap(cv, H, 4)
        cv.rect(0, 4, 31, rows, H[1])
        for x in range(0, 32):
            if hsh(x, 7, 3) > 0.5:
                cv.put(x, rows + 1, H[1])
        for x in (4, 9, 14, 19, 24, 29):
            cv.line(x, 4, x + 1, rows, H[0])
        for x in (6, 12, 22):
            cv.line(x, 5, x, rows - 2, H[2])
        _temples(cv, H, 17, 17)


def fringe_long_bangs(cv, stage, H, cover=None):
    """Bangs to the brows (stage 0) and, with cover = 'l' / 'r', a heavy lock across one eye (stage 1)."""
    if stage == 0:
        _cap(cv, H, 4)
        cv.poly([(0, 4), (31.5, 4), (31.5, 11), (27, 13), (22, 11), (17, 13), (15, 11), (11, 13), (7, 11), (3, 13), (0, 12)], H[1])
        for x in (3, 7, 11, 17, 22, 27):
            cv.line(x, 5, x, 11, H[0])
        for x in (5, 14, 20, 25):
            cv.line(x, 5, x, 9, H[2])
        _temples(cv, H, 19, 19)
    elif cover:
        x0 = 6 if cover == "r" else 18
        cv.poly([(x0, 10), (x0 + 8, 10), (x0 + 6, 20), (x0 + 3, 23), (x0 + 1, 19)], H[1])
        cv.line(x0 + 3, 10, x0 + 3, 21, H[0])
        cv.line(x0 + 5, 11, x0 + 5, 18, H[2])


def fringe_slick(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 3)
        cv.poly([(0, 3), (6, 3), (5, 8), (3, 10), (0, 9)], H[1])
        cv.poly([(26, 3), (31.5, 3), (31.5, 9), (28, 10), (26, 8)], H[1])
        for k in range(6):
            cv.line(5 + k * 4.5, 0, 6 + k * 4, 3, H[2] if k % 2 else H[0])


def fringe_swept_back(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 4)
        cv.poly([(0, 4), (9, 4), (6, 8), (2, 11), (0, 11)], H[1])
        cv.poly([(22, 4), (31.5, 4), (31.5, 11), (29, 11), (25, 8)], H[1])
        cv.poly([(9, 4), (22, 4), (15.5, 7)], H[1])
        cv.line(15.5, 0, 15.5, 6, H[0])
        for k in range(5):
            cv.line(3 + k * 6, 1, 4 + k * 5, 5, H[2])


def fringe_messy(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 4)
        _temples(cv, H, 16, 13)
        for a, b, tx, ty in ((2, 7, 3, 12), (6, 11, 9, 9), (10, 15, 11, 12), (14, 19, 17, 8), (18, 23, 20, 12), (22, 27, 26, 9), (26, 31, 30, 13)):
            _lock(cv, H, a, b, tx, ty, 4)


def fringe_hood(cv, stage, H):
    if stage == 0:
        _cap(cv, H, 2)
        for a, b, tx, ty in ((7, 13, 9, 7), (13, 19, 16, 6), (19, 25, 22, 7)):
            _lock(cv, H, a, b, tx, ty, 2)
        for y in range(0, 32):                                 # the hood's shadow on the sides of the face
            for x in (0, 1, 2, 29, 30, 31):
                cv.put(x, y, H[0])


FRINGES = {"none": fringe_none, "cap": fringe_cap, "short": fringe_short, "spiky": fringe_spiky, "sweep": fringe_sweep, "side_part": fringe_side_part,
           "center_part": fringe_center_part, "bangs": fringe_bangs, "long_bangs": fringe_long_bangs, "slick": fringe_slick, "swept_back": fringe_swept_back,
           "messy": fringe_messy, "hood": fringe_hood}


# ------------------------------------------------------------------------------------------------------------------------ geometry pieces
class HairCtx:
    """What a style recipe builds with: the rig, the hair palette, the head's coordinates and a few piece makers."""

    def __init__(self, rig, H, streak=None, ribbon=None):
        self.r = rig
        self.d = rig.d
        self.H = H
        self.top = rig.d.head_y + 8.0
        self.ribbon = ribbon or mix(H[1], (200, 60, 70), 0.6)
        self.streak = streak
        self.plates = [clump(H, salt=70 + k, strands=2.4, root=0.1, curl=((-1) ** k) * 0.2, dither=0.04, split=0.0) for k in range(4)]
        self.n = 0

    def _paint(self, salt, strand=2.0):
        base = hair_paint(salt=salt, cols=self.H, strand=strand)
        if not self.streak:
            return base
        st = hair_paint(salt=salt, cols=self.streak, strand=strand)

        def p(face, i, j, fw, fh):
            if hsh(int(i // 2), salt, 41) > 0.84:
                return st(face, i, j, fw, fh)
            return base(face, i, j, fw, fh)
        return p

    def paint(self, salt=5, strand=2.0):
        return self._paint(salt, strand)

    # ---- shells and slabs
    def shell(self, bone="hair", top=0.9, side=0.5, back_px=3.0, side_px=2.5, front_px=None):
        """A thin shell of hair over the scalp: a stepped cap over the crown (a wide slab and a smaller one on it, so the top is not a flat lid), one slab down the back and
        one over each temple. The front of the cap is flush with the face."""
        r, T = self.r, self.top
        r.box(bone, (-4 - side, T - 0.5, -4.0), (8 + 2 * side, 0.5 + top * 0.55, 8 + side), self.paint(5), faces=("top", "back", "left", "right") + (("front",) if front_px else ()))
        if top > 0.7:
            inset = 0.9 + side
            r.box(bone, (-4 - side + inset, T + top * 0.5, -4.0 + inset * 0.5), (8 + 2 * side - 2 * inset, top * 0.5 + 0.3, 8 + side - 1.5 * inset), self.paint(4), faces=("top", "back", "left", "right", "front"))
        if back_px > 0:
            r.box(bone, (-4 - side, T - back_px, 4.0), (8 + 2 * side, back_px, side), self.paint(6), faces=("back", "left", "right", "bottom"))
        if side_px > 0:
            for sg in (1, -1):
                x0 = 4.0 if sg > 0 else -4.0 - side
                r.box(bone, (x0, T - side_px, -4.0 + (front_px or 1.5)), (side, side_px, 8 - (front_px or 1.5)), self.paint(7 if sg > 0 else 8),
                      faces=("left", "right", "front", "back", "bottom"))

    def tuft(self, phi, alpha, w, ln, rad, dy=0.0, cross=False, bone="hair", key=None, thick=0.5, v=None, solid=False):
        """A standing tuft of hair on the crown at azimuth phi (0 = front, 90 = +x), leaning alpha degrees outward, rad px from the centre. A flat flame-shaped plate
        (fur, soft hair) or, with solid, a tapering spike of three boxes (anime spikes)."""
        a = math.radians(phi)
        pos = (rad * SIN(a), self.top + dy, -rad * COS(a))
        self.n += 1
        vv = (self.n % 4) if v is None else v
        if solid:
            self.r.spike(bone, pos, max(1.5, w * 0.78), ln, -alpha, 0.0, self.paint(30 + vv), ry=-phi, fracs=(0.5, 0.3, 0.2), taper=0.62, bend=(-5.0, 0.0))
            return
        self.r.clump(bone, pos, phi, alpha, w, ln, self.plates[vv], standing=True, key=key or ("hc%d" % vv), thick=thick)
        if cross:
            self.r.clump(bone, pos, phi + 90, alpha * 0.7, w * 0.8, ln * 0.9, self.plates[(vv + 1) % 4], standing=True, key=key and key + "x" or ("hx%d" % ((vv + 1) % 4)),
                         thick=thick)

    def ring(self, count, rad, alpha, alpha_var, ln, ln_var, w, phi0=10.0, span=360.0, dy=0.0, cross=False, bone="hair", bias=0.0, bias_dir=180.0, solid=False):
        """A ring of tufts round the crown: the lean grows toward `bias_dir` (the azimuth the hair is swept to) by `bias` degrees."""
        for q in range(count):
            phi = phi0 + (q * span / count if span >= 360 else q * span / max(1, count - 1))
            hv = hsh(q, count, 7 + self.n)
            a = alpha + alpha_var * (hv - 0.5)
            if bias:
                a += bias * (0.5 + 0.5 * COS(math.radians(phi - bias_dir)))
            self.tuft(phi, a, w, ln + ln_var * (hash_f(q, 2, count) - 0.5), rad, dy, cross, bone, solid=solid)

    def hang(self, bone, x, y, z, w, ln, alpha, theta, v=0, thick=1.0):
        """A lock hanging from (x, y, z): a clump plate turned to azimuth theta and swung alpha degrees."""
        self.r.clump(bone, (x, y, z), theta, alpha, w, ln, self.plates[v % 4], key="hg%d" % (v % 4), thick=thick)

    def slab(self, bone, origin, size, salt=9, faces=None):
        self.r.box(bone, origin, size, self.paint(salt), faces=faces)

    def tail(self, bone, root, length, w, d, sway=(0.0, 0.0), segs=3, rx0=14.0, taper=0.78, salt=11, tie=True):
        """A hanging tail of `segs` stacked boxes from `root` (x, y, z): each one tilted a little more back / out, ending in a point. The hair tie is a small box."""
        r = self.r
        pos = list(root)
        ax, az = rx0, sway[1]
        sw, sd = w, d
        sl = length / segs
        for k in range(segs):
            c = r.box(bone, (pos[0] - sw / 2, pos[1] - sl, pos[2] - sd / 2), (sw, sl, sd), self.paint(salt + k), rotation=(-ax, 0, az), pivot=tuple(pos))
            from .rig import rot_file
            dv = rot_file((0, -sl, 0), -ax, 0, az)
            pos = [pos[0] + dv[0], pos[1] + dv[1], pos[2] + dv[2]]
            ax += 6.0 + sway[0]
            sw, sd = max(1.0, sw * taper), max(1.0, sd * taper)
        if tie:
            r.box(bone, (root[0] - w / 2 - 0.25, root[1] - 0.5, root[2] - d / 2 - 0.25), (w + 0.5, 1.5, d + 0.5), px_paint(self.ribbon))

    def ball(self, bone, c, rad, salt=13):
        """A bun: a small stack of boxes that reads round (a cross of three boxes)."""
        x, y, z = c
        r = self.r
        p = self.paint(salt)
        r.box(bone, (x - rad, y - rad * 0.6, z - rad * 0.6), (2 * rad, rad * 1.2, rad * 1.2), p)
        r.box(bone, (x - rad * 0.6, y - rad, z - rad * 0.6), (rad * 1.2, 2 * rad, rad * 1.2), p)
        r.box(bone, (x - rad * 0.6, y - rad * 0.6, z - rad), (rad * 1.2, rad * 1.2, 2 * rad), p)

    def back_slab(self, bone, length, w=9.2, thick=1.5, y_top=None, z=4.0, salt=9):
        y1 = (self.top - 1.0) if y_top is None else y_top
        self.r.box(bone, (-w / 2, y1 - length, z), (w, length, thick), self.paint(salt), faces=("back", "left", "right", "front", "bottom"))

    def side_locks(self, bone, length, front=True, x=4.3, thick=1.5, w=2.5, z=-3.0):
        """Locks beside the face (and in front of the shoulders when long)."""
        T = self.top
        for sg in (1, -1):
            self.r.box(bone, (sg * x - w / 2, T - 3.0 - length, z - thick / 2), (w, length, thick), self.paint(10 + sg),
                       faces=("left", "right", "front", "back", "bottom"))


def hash_f(*k):
    return hsh(*k)


def px_paint(c):
    def p(face, i, j, fw, fh):
        return px(c), None
    return p


# ------------------------------------------------------------------------------------------------------------------------ styles
def s_bald(c):
    pass


def s_crop(c):
    c.shell(top=0.4, side=0.25, back_px=2.0, side_px=1.5)


def s_short(c):
    c.shell(top=1.0, side=0.5, back_px=3.5, side_px=2.5)
    c.ring(8, 3.1, 40, 18, 3.2, 1.0, 3.6, phi0=10, solid=True)


def s_spiky(c):
    c.shell(top=0.9, side=0.5, back_px=3.5, side_px=2.5)
    c.ring(10, 3.0, 44, 22, 6.5, 3.5, 3.8, phi0=5, solid=True)
    c.ring(5, 1.3, 20, 18, 7.5, 3.0, 3.8, phi0=30, solid=True, dy=0.2)


def s_spiky_back(c):
    c.shell(top=0.9, side=0.5, back_px=4.0, side_px=2.5)
    c.ring(9, 3.0, 38, 14, 6.0, 2.5, 3.6, phi0=0, bias=34, bias_dir=180, solid=True)
    c.ring(5, 1.3, 52, 14, 8.0, 2.5, 3.6, phi0=60, span=240, bias=30, bias_dir=180, dy=0.2, solid=True)


def s_messy(c):
    c.shell(top=1.0, side=0.6, back_px=5.0, side_px=3.5)
    c.ring(11, 3.0, 54, 40, 5.0, 3.0, 3.6, phi0=8, solid=True)
    c.ring(5, 1.3, 30, 30, 5.6, 2.5, 3.6, phi0=40, solid=True, dy=0.2)
    for q, (x, z, th) in enumerate(((-4.4, 0.5, -90), (4.4, 1.0, 90), (-4.0, 3.5, -130), (4.0, 3.8, 130))):
        c.hang("hair", x, c.top - 3.0, z, 3.0, 4.5 + (q % 2), 16, th, v=q)


def s_slick(c):
    """Combed back: a smooth cap with rows of short spikes lying flat towards the nape, like the strokes of a comb."""
    c.shell(top=1.2, side=0.5, back_px=5.0, side_px=3.0)
    for row, x in enumerate((-2.6, 0.0, 2.6)):
        for q in range(5):
            z = -3.4 + q * 1.5
            c.r.spike("hair", (x, c.top + 0.9, z), 2.4, 3.6 + 0.4 * (q % 2), -66.0, 0.0, c.paint(30 + (row + q) % 4), ry=180.0, fracs=(0.55, 0.45), taper=0.7)


def s_side_part(c):
    c.shell(top=1.3, side=0.6, back_px=3.5, side_px=3.0)
    c.tuft(-60, 70, 4.5, 3.0, 3.2, dy=0.2)


def s_bob(c):
    c.shell(top=1.0, side=0.6, back_px=3.0, side_px=1.5)
    c.back_slab("hair_back", 8.0, w=9.6, thick=1.5, y_top=c.top - 2.0)
    for sg in (1, -1):
        c.slab("hair", (sg * 4.0 + (0 if sg > 0 else -1.4), c.top - 9.0, -3.0), (1.4, 8.0, 7.5), 20 + sg, faces=("left", "right", "front", "back", "bottom"))


def s_long(c, length=17.0):
    c.shell(top=1.0, side=0.6, back_px=4.0, side_px=2.5)
    c.back_slab("hair_back", length, w=9.6, thick=1.6, y_top=c.top - 2.0)
    for sg in (1, -1):
        c.slab("hair_back", (sg * 4.2 - 0.6 if sg > 0 else -4.2 - 0.8, c.top - 2.0 - length * 0.9, 1.0), (1.4, length * 0.9, 3.5), 22 + sg, faces=("left", "right", "front", "back", "bottom"))
    c.side_locks("hair", length * 0.4)


def s_long_wavy(c):
    s_long(c, 19.0)
    for q, x in enumerate((-4.2, -1.4, 1.4, 4.2)):
        c.hang("hair_back", x, c.top - 2.0 - 15.5, 5.2, 3.6, 5.0, 14, 180 + (x * 6), v=q)


def s_ponytail(c):
    c.shell(top=1.0, side=0.5, back_px=4.0, side_px=2.5)
    c.tail("hair_back", (0.0, c.top - 3.0, 4.4), 12.0, 3.5, 2.5, segs=3, rx0=16)


def s_high_ponytail(c):
    c.shell(top=1.0, side=0.5, back_px=3.0, side_px=2.5)
    c.tail("hair_back", (0.0, c.top + 0.2, 3.2), 13.0, 3.5, 2.8, segs=3, rx0=30, sway=(2.0, 0.0))


def s_twin_tails(c):
    c.shell(top=1.0, side=0.5, back_px=3.0, side_px=2.5)
    for sg, bn in ((1, "hair_tail_l"), (-1, "hair_tail_r")):
        c.tail(bn, (sg * 5.4, c.top - 1.5, 1.0), 14.0, 3.2, 2.6, segs=3, rx0=-2, sway=(0.0, sg * 6.0), salt=30 + sg)


def s_twin_buns(c):
    c.shell(top=1.0, side=0.5, back_px=3.0, side_px=2.5)
    for sg in (1, -1):
        c.ball("hair", (sg * 3.4, c.top + 2.0, 0.5), 2.0, salt=14 + sg)
        c.r.box("hair", (sg * 3.4 - 1.2, c.top + 0.2, -0.8), (2.4, 1.2, 2.4), px_paint(c.ribbon))


def s_bun(c):
    c.shell(top=1.0, side=0.5, back_px=3.5, side_px=2.5)
    c.ball("hair", (0.0, c.top + 2.5, 1.5), 2.4, salt=14)
    c.r.box("hair", (-1.4, c.top + 0.2, 0.2), (2.8, 1.2, 2.8), px_paint(c.ribbon))


def s_topknot(c):
    c.shell(top=0.5, side=0.3, back_px=2.5, side_px=1.5)
    c.r.box("hair", (-1.25, c.top, -1.25), (2.5, 3.0, 2.5), c.paint(15))
    c.r.box("hair", (-1.75, c.top + 1.0, -1.75), (3.5, 1.0, 3.5), px_paint(c.ribbon))
    c.r.box("hair", (-1.25, c.top + 3.0, -0.25), (2.5, 2.0, 2.5), c.paint(16), rotation=(-30, 0, 0), pivot=(0, c.top + 3.0, 0))


def s_braid(c):
    c.shell(top=1.0, side=0.5, back_px=4.5, side_px=2.5)
    pos = c.top - 4.0
    for k in range(6):
        w = 3.0 if k % 2 == 0 else 2.5
        c.r.box("hair_back", (-w / 2, pos - 2.4, 4.0 + 0.2 * k), (w, 2.4, 2.2), c.paint(40 + k))
        pos -= 2.3
    c.r.box("hair_back", (-1.4, pos - 0.6, 5.0), (2.8, 1.4, 2.4), px_paint(c.ribbon))


def s_twin_braids(c):
    c.shell(top=1.0, side=0.5, back_px=3.5, side_px=2.5)
    for sg, bn in ((1, "hair_tail_l"), (-1, "hair_tail_r")):
        pos = c.top - 3.0
        for k in range(6):
            w = 2.6 if k % 2 == 0 else 2.1
            c.r.box(bn, (sg * 4.8 - w / 2, pos - 2.3, -2.6 - 0.15 * k), (w, 2.3, 2.0), c.paint(50 + k + sg))
            pos -= 2.2


def s_mohawk(c):
    c.shell(top=0.3, side=0.2, back_px=1.5, side_px=1.0)
    for q in range(7):
        z = -3.2 + q * 1.1
        ln = 7.5 - 0.5 * abs(q - 3)
        c.r.spike("hair", (0.0, c.top, z), 2.6, ln, -6.0 - 3.0 * q, 0.0, c.paint(30 + q % 4), ry=180.0, fracs=(0.5, 0.3, 0.2), taper=0.6)


def s_afro(c):
    c.shell(top=0.5, side=0.3, back_px=3.0, side_px=2.0)
    p = c.paint(60, strand=1.4)
    T = c.top
    c.r.box("hair", (-6.0, T - 3.2, -5.0), (12.0, 8.5, 10.0), p, pivot=(0, T, 0))
    c.r.box("hair", (-5.0, T - 3.2, -6.0), (10.0, 8.5, 12.0), p, pivot=(0, T, 0))
    c.r.box("hair", (-5.5, T - 3.2, -5.5), (11.0, 8.5, 11.0), p, rotation=(0, 45, 0), pivot=(0, T, 0))
    c.r.box("hair", (-4.5, T + 4.0, -4.5), (9.0, 2.0, 9.0), p, rotation=(0, 45, 0), pivot=(0, T, 0))


def s_hood(c):
    """A hood laid over the head: a box shell open at the front, a rim round the face, a peak behind. The colour is the hair colour, drawn as cloth."""
    r = c.r
    T = c.top
    cloth = fabric(c.H, salt=61, period=7.0, amp=0.22, seam=False)
    r.box("hair", (-4.9, T - 8.6, -4.9), (9.8, 9.6, 9.8), cloth, faces=("top", "back", "left", "right"))
    r.box("hair", (-4.9, T - 1.6, -4.9), (9.8, 2.0, 1.6), cloth, faces=("top", "front", "left", "right", "back", "bottom"))
    for sg in (1, -1):
        r.box("hair", (sg * 4.9 - (1.0 if sg > 0 else 0.0), T - 8.6, -4.9), (1.0, 7.2, 1.6), cloth, faces=("front", "left", "right", "back", "bottom"))
    r.box("hair", (-1.5, T - 5.0, 4.9), (3.0, 4.5, 3.0), cloth, rotation=(24, 0, 0), pivot=(0, T - 1.0, 4.5))
    r.box("hair", (-3.5, T - 12.5, 3.6), (7.0, 5.0, 1.8), cloth, faces=("back", "left", "right", "bottom", "front"))


def s_undercut(c):
    c.shell(top=1.4, side=0.2, back_px=1.5, side_px=0.5)
    c.ring(7, 2.4, 60, 16, 4.8, 1.5, 3.6, phi0=-20, span=140, bias=18, bias_dir=180, solid=True)


def s_pompadour(c):
    c.shell(top=1.2, side=0.5, back_px=3.0, side_px=2.5)
    c.r.box("hair", (-3.8, c.top + 0.2, -6.0), (7.6, 2.8, 4.0), c.paint(66), rotation=(-26, 0, 0), pivot=(0, c.top + 0.2, -3.0))
    c.ring(4, 2.0, 30, 10, 3.5, 1.0, 3.8, phi0=120, span=120, bias=12, bias_dir=180)


def s_wild_mane(c):
    c.shell(top=1.2, side=0.8, back_px=6.0, side_px=4.0)
    c.ring(12, 3.4, 52, 28, 7.0, 3.5, 4.2, phi0=5, solid=True)
    c.back_slab("hair_back", 15.0, w=10.4, thick=2.0, y_top=c.top - 1.5)
    for q in range(10):
        a = math.radians(30 + q * 32)
        c.hang("hair_back", 5.4 * SIN(a), c.top - 2.0, 5.0 * -COS(a) * 0.8 + 0.5, 3.4, 8 + (q % 3) * 2.5, 12, math.degrees(a), v=q)


def s_bangs_long(c):
    c.shell(top=1.0, side=0.6, back_px=3.5, side_px=2.5)
    c.back_slab("hair_back", 6.0, w=9.4, thick=1.4, y_top=c.top - 2.0)
    c.side_locks("hair", 4.0, w=2.2, thick=1.4)


def s_curtains(c):
    c.shell(top=1.0, side=0.6, back_px=3.5, side_px=3.0)
    c.side_locks("hair", 5.0, w=2.6, thick=1.5)
    c.back_slab("hair_back", 4.0, w=9.0, thick=1.3, y_top=c.top - 2.0)


def s_shaggy(c):
    c.shell(top=1.0, side=0.7, back_px=5.0, side_px=3.5)
    c.ring(9, 3.3, 40, 20, 3.8, 2.0, 3.6, phi0=10)
    c.back_slab("hair_back", 7.0, w=9.8, thick=1.6, y_top=c.top - 2.0)
    for q in range(6):
        c.hang("hair_back", -4.0 + q * 1.6, c.top - 9.0, 5.2, 2.6, 4.0 + (q % 2) * 1.5, 12, 180, v=q)
    c.side_locks("hair", 3.0, w=2.2, thick=1.4)


def s_drills(c):
    c.shell(top=1.0, side=0.5, back_px=3.0, side_px=2.5)
    for sg, bn in ((1, "hair_tail_l"), (-1, "hair_tail_r")):
        pos = c.top - 2.0
        for k in range(5):
            w = 3.4 - 0.25 * k
            c.r.box(bn, (sg * (5.2 + 0.4 * (k % 2)) - w / 2, pos - 2.6, -1.0 - 0.3 * (k % 2) * sg), (w, 2.6, w), c.paint(70 + k + sg))
            pos -= 2.4


def s_spiky_long(c):
    s_spiky(c)
    c.back_slab("hair_back", 12.0, w=9.4, thick=1.6, y_top=c.top - 2.0)
    for q in range(5):
        c.hang("hair_back", -3.6 + q * 1.8, c.top - 2.0 - 11.0, 5.0, 2.8, 4.5, 8, 180, v=q)


def s_samurai(c):
    s_topknot(c)
    c.side_locks("hair", 6.0, w=2.0, thick=1.2, x=4.2)


def s_wizard(c):
    """Long, swept hair with a wild crown (an old mage's mane)."""
    s_long(c, 13.0)
    c.ring(6, 2.8, 48, 18, 5.0, 2.0, 3.6, phi0=20, cross=True, bias=20, bias_dir=180)


# name -> (fringe, fringe kwargs, hair-mask rows on the sides / the back / the sideburn (texels of 16), recipe)
STYLES = {
    "bald":         (("none", {}),                           dict(side=0, back=0, burn=0, top=False, bones=()), s_bald),
    "crop":         (("cap", {"rows": 3}),                   dict(side=5, back=5, burn=5, top=True, bones=()), s_crop),
    "short":        (("short", {}),                          dict(side=7, back=11, burn=8, top=True, bones=()), s_short),
    "spiky":        (("spiky", {}),                          dict(side=7, back=12, burn=8, top=True, bones=()), s_spiky),
    "spiky_back":   (("swept_back", {}),                     dict(side=7, back=12, burn=7, top=True, bones=()), s_spiky_back),
    "messy":        (("messy", {}),                          dict(side=8, back=12, burn=9, top=True, bones=()), s_messy),
    "slick":        (("slick", {}),                          dict(side=6, back=11, burn=6, top=True, bones=()), s_slick),
    "side_part":    (("side_part", {}),                      dict(side=7, back=11, burn=8, top=True, bones=()), s_side_part),
    "bob":          (("bangs", {"rows": 8}),                 dict(side=14, back=14, burn=14, top=True, bones=("hair_back",)), s_bob),
    "long":         (("center_part", {"reach": 14}),         dict(side=12, back=16, burn=14, top=True, bones=("hair_back",)), s_long),
    "long_wavy":    (("long_bangs", {}),                     dict(side=12, back=16, burn=14, top=True, bones=("hair_back",)), s_long_wavy),
    "ponytail":     (("sweep", {}),                          dict(side=8, back=10, burn=8, top=True, bones=("hair_back",)), s_ponytail),
    "high_ponytail": (("swept_back", {}),                    dict(side=8, back=9, burn=8, top=True, bones=("hair_back",)), s_high_ponytail),
    "twin_tails":   (("bangs", {"rows": 7}),                 dict(side=8, back=9, burn=8, top=True, bones=("hair_tail_l", "hair_tail_r")), s_twin_tails),
    "twin_buns":    (("bangs", {"rows": 7}),                 dict(side=8, back=9, burn=8, top=True, bones=()), s_twin_buns),
    "bun":          (("short", {}),                          dict(side=8, back=9, burn=8, top=True, bones=()), s_bun),
    "topknot":      (("slick", {}),                          dict(side=5, back=6, burn=5, top=True, bones=()), s_topknot),
    "braid":        (("center_part", {"reach": 10}),         dict(side=8, back=11, burn=9, top=True, bones=("hair_back",)), s_braid),
    "twin_braids":  (("center_part", {"reach": 11}),         dict(side=8, back=10, burn=9, top=True, bones=("hair_tail_l", "hair_tail_r")), s_twin_braids),
    "mohawk":       (("cap", {"rows": 2}),                   dict(side=3, back=3, burn=3, top=True, bones=()), s_mohawk),
    "afro":         (("bangs", {"rows": 6}),                 dict(side=10, back=12, burn=9, top=True, bones=()), s_afro),
    "hood":         (("hood", {}),                           dict(side=16, back=16, burn=16, top=True, bones=()), s_hood),
    "undercut":     (("sweep", {}),                          dict(side=3, back=4, burn=3, top=True, bones=()), s_undercut),
    "pompadour":    (("swept_back", {}),                     dict(side=7, back=8, burn=8, top=True, bones=()), s_pompadour),
    "wild_mane":    (("messy", {}),                          dict(side=14, back=16, burn=14, top=True, bones=("hair_back",)), s_wild_mane),
    "bangs_long":   (("long_bangs", {"cover": "l"}),         dict(side=11, back=12, burn=13, top=True, bones=("hair_back",)), s_bangs_long),
    "curtains":     (("center_part", {"reach": 12}),         dict(side=10, back=12, burn=12, top=True, bones=("hair_back",)), s_curtains),
    "shaggy":       (("messy", {}),                          dict(side=10, back=14, burn=11, top=True, bones=("hair_back",)), s_shaggy),
    "drills":       (("bangs", {"rows": 7}),                 dict(side=8, back=9, burn=8, top=True, bones=("hair_tail_l", "hair_tail_r")), s_drills),
    "spiky_long":   (("spiky", {}),                          dict(side=8, back=14, burn=9, top=True, bones=("hair_back",)), s_spiky_long),
    "samurai":      (("slick", {}),                          dict(side=6, back=8, burn=12, top=True, bones=()), s_samurai),
    "wizard":       (("center_part", {"reach": 10}),         dict(side=11, back=15, burn=12, top=True, bones=("hair_back",)), s_wizard),
}
HAIR_STYLES = tuple(STYLES)

# the pivots of the hair bones (relative to the head box): the cap sways round the crown, long hair hangs from the nape
def _bones(rig, names):
    d = rig.d
    T = d.head_y + 8.0
    rig.bone("hair", "head", (0, T - 1.0, 0))
    if "hair_back" in names:
        rig.bone("hair_back", "head", (0, T - 2.0, 3.8))
    if "hair_tail_l" in names:
        rig.bone("hair_tail_l", "head", (5.0, T - 2.0, 0.5))
        rig.bone("hair_tail_r", "head", (-5.0, T - 2.0, 0.5))


def build_hair(rig, style, H, streak=None, ribbon=None, opts=None):
    """Adds the hair bones and cubes of `style` to the rig; returns (fringe(cv, stage), hair_mask) for the head box.
    The hair bones: hair (the cap and tufts), hair_back (long hair hanging behind), hair_tail_l / hair_tail_r (tails at the sides)."""
    if style not in STYLES:
        raise ValueError("unknown hair style %r; use one of: %s" % (style, ", ".join(HAIR_STYLES)))
    fr, mk, recipe = STYLES[style]
    opts = opts or {}
    _bones(rig, mk["bones"])
    ctx = HairCtx(rig, H, streak, ribbon)
    recipe(ctx)
    fname, fkw = fr
    fkw = dict(fkw)
    if "cover" in opts and fname == "long_bangs":
        fkw["cover"] = opts["cover"]
    fn = FRINGES[fname]

    def fringe(cv, stage):
        fn(cv, stage, H, **fkw)

    side_rows, back_rows, burn, top = mk["side"], mk["back"], mk["burn"], mk["top"]
    covers_ears = side_rows >= 13

    def hair_mask(face, i, j, fw, fh):
        if style == "bald":
            return False
        if face == "top":
            return top
        if face == "back":
            return j < back_rows * fh / 16.0
        if face in ("left", "right"):
            f = i if face == "left" else fw - 1 - i            # texels from the front edge (0) to the back (fw - 1)
            if covers_ears:
                return True
            if j < side_rows * fh / 16.0:
                return True
            if f >= fw - 5 and j < back_rows * fh / 16.0:
                return True
            return f < 2 and j < burn * fh / 16.0
        return False
    return fringe, hair_mask
