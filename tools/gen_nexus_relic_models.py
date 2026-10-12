"""1.21.0: the 3D in-hand models of the Dimensional Nexus relics and navigation gear, built to the Butcher Cleaver's standard.

This file is not run on its own: tools/gen_weapon_models.py executes it in its own namespace (one line before `def mirror_x`), so np, math, Image,
Mesh, Atlas, box, plate, disc_mask, ascii_mask, col, shade, mix, nz, fz, hsh, lerp, SPECS, RELICS, REAL, GRIP, _lathe_dir, _bc_tube, _xb_packed and
sprite_axis / sprite_box all exist here.

    python3 -B tools/gen_nexus_relic_icons.py                       (the 16x16 sprites first: the meshes are placed on them)
    python3 -B tools/gen_weapon_models.py ki_resonance_lantern ki_resonance_lantern_off ki_resonance_lantern_blue ki_resonance_lantern_red \\
        anti_bird_compass glutton_chalice devil_bound_sheath gamblers_grimoire_page
    python3 -B tools/item_preview/preview_weapons.py ki_resonance_lantern  (the headless preview)

Model units are texels of the 128 skin; y runs up the item (along the sheath for the sheath), x across it, z through it, the front faces +z.
  ki_resonance_lantern  a hexagonal cage of iron lattice and glass round an open flame in a brass fuel cup, a pointed brass-trimmed roof and a ring to hold it
                        by. Four meshes, one per flame state: _off (no flame, no glow), the default (amber), _blue (an IllusionStone wall ahead) and _red
                        (a descent gate ahead): the flame, the glints in the glass and the runes on the base take its colour.
  anti_bird_compass     a bronze case with a dark green dial, a feather-shaped needle (a pale tail, a biomass-green tip) and a crow's feather hanging off its
                        ring. The 3D needle is neutral (up): the inventory icon is the 32-frame one.
  glutton_chalice       a living goblet of bone: teeth round the rim, a glowing green stew, red veins that glow and crawl over the bowl and the stem, tendrils
                        round the foot.
  devil_bound_sheath    a black lacquered scabbard wrapped in iron chain, a violet-red sigil that glows, a glowing void in its mouth, horns at the chape and a
                        chain lanyard from the mouth.
  gamblers_grimoire_page one torn parchment page with a curled corner, a d20 inked in glowing violet and gold, runes down its margins in shifting colours and a
                        few motes of light floating round it.
"""
import colorsys


# ================================================================ helpers
def _rc_lathe(m, a, prof, sides, paint, sw=2, base=(0.0, 0.0, 0.0), direction=(0.0, 1.0, 0.0), jitter=0.0, salt=0):
    """A surface of revolution round +y from a profile [(y, r), ...] that may run up, across and down (an outer wall, a rim, an inner wall, a floor): each
    segment faces the way the profile turns (up a wall: out; back along a rim: up; down an inner wall: in). Turned so +y points along 'direction', moved to
    'base'. paint(k, s, d, seg, theta) -> (rgba, glow): side k at fraction s across it, d the distance along the profile, seg its segment, theta 0..1 round."""
    st = [(float(y), float(r)) for (y, r) in prof]
    cum = [0.0]
    for (y0, r0), (y1, r1) in zip(st, st[1:]):
        cum.append(cum[-1] + math.hypot(y1 - y0, r1 - r0))
    H = int(math.ceil(cum[-1])) + 1
    W = sides * sw
    u0, v0 = a.alloc(W, H)

    def seg_at(d):
        for i in range(len(cum) - 1):
            if d <= cum[i + 1] + 1e-9:
                return i
        return len(cum) - 2

    def f(lx, ly):
        k = min(sides - 1, int(lx // sw))
        s = (lx - k * sw) / sw
        d = min(ly, cum[-1])
        return paint(k, s, d, seg_at(d), (k + s) / sides)
    a.paint(u0, v0, W, H, f)
    R = _lathe_dir(direction)
    org = np.array(base, np.float64)

    def ring(i, k):
        y, r = st[i]
        th = 2 * math.pi * (k % sides) / sides
        rr = r * (1 + jitter * (hsh(k % sides, int(y * 2), salt) - 0.5) * 2) if r > 0 else 0.0
        return tuple(org + R @ np.array([rr * math.cos(th), y, rr * math.sin(th)]))

    def vt(d):
        return v0 + min(max(d, 0.02), H - 0.02)
    for i in range(len(st) - 1):
        (y0, r0), (y1, r1) = st[i], st[i + 1]
        if abs(y1 - y0) < 1e-9 and abs(r1 - r0) < 1e-9:
            continue
        for k in range(sides):
            thm = 2 * math.pi * (k + 0.5) / sides
            out = R @ np.array([(y1 - y0) * math.cos(thm), -(r1 - r0), (y1 - y0) * math.sin(thm)])
            ua, ub = u0 + k * sw + 0.02, u0 + (k + 1) * sw - 0.02
            m.quad(0, [ring(i, k) + (ua, vt(cum[i])), ring(i, k + 1) + (ub, vt(cum[i])), ring(i + 1, k + 1) + (ub, vt(cum[i + 1])),
                       ring(i + 1, k) + (ua, vt(cum[i + 1]))], tuple(out))


def _rc_tube(m, a, path, radius, sides, paint, sw=2, closed=False, caps=(False, False)):
    """A tube swept along a 3D polyline (a ring, a chain link, a tendril, a helix); a closed path must not repeat its first point. radius: one number or
    one per point. paint(k, s, d, theta) -> (rgba, glow): side k at fraction s across it, d the distance along the path, theta 0..1 round it."""
    P = [np.array(p, np.float64) for p in path]
    n = len(P)
    rad = list(radius) if isinstance(radius, (list, tuple)) else [float(radius)] * n
    T = []
    for i in range(n):
        d = (P[(i + 1) % n] - P[(i - 1) % n]) if closed else (P[min(n - 1, i + 1)] - P[max(0, i - 1)])
        T.append(d / np.linalg.norm(d))
    ref = np.array([0.0, 1.0, 0.0]) if abs(T[0][1]) < 0.9 else np.array([1.0, 0.0, 0.0])
    N = [np.cross(T[0], ref)]
    N[0] /= np.linalg.norm(N[0])
    for i in range(1, n):
        v = N[-1] - np.dot(N[-1], T[i]) * T[i]
        N.append(v / np.linalg.norm(v))
    B = [np.cross(T[i], N[i]) for i in range(n)]
    segs = n if closed else n - 1
    cum = [0.0]
    for i in range(segs):
        cum.append(cum[-1] + float(np.linalg.norm(P[(i + 1) % n] - P[i])))
    H = int(math.ceil(cum[-1])) + 1
    W = sides * sw
    u0, v0 = a.alloc(W, H)

    def f(lx, ly):
        k = min(sides - 1, int(lx // sw))
        s = (lx - k * sw) / sw
        return paint(k, s, min(ly, cum[-1]), (k + s) / sides)
    a.paint(u0, v0, W, H, f)

    def radial(i, k):
        th = 2 * math.pi * (k % sides) / sides
        return math.cos(th) * N[i] + math.sin(th) * B[i]

    def ring(i, k):
        j = i % n
        return tuple(P[j] + rad[j] * radial(j, k))

    def vt(d):
        return v0 + min(max(d, 0.02), H - 0.02)
    for i in range(segs):
        for k in range(sides):
            ua, ub = u0 + k * sw + 0.02, u0 + (k + 1) * sw - 0.02
            out = radial(i, k) + radial(i, k + 1) + radial((i + 1) % n, k) + radial((i + 1) % n, k + 1)
            m.quad(0, [ring(i, k) + (ua, vt(cum[i])), ring(i, k + 1) + (ub, vt(cum[i])), ring(i + 1, k + 1) + (ub, vt(cum[i + 1])),
                       ring(i + 1, k) + (ua, vt(cum[i + 1]))], tuple(out))
    if not closed:
        for (idx, sgn, on) in ((0, -1, caps[0]), (n - 1, 1, caps[1])):
            if not on:
                continue
            cu, cv = u0 + W / 2, vt(cum[idx])
            for k in range(sides):
                m.quad(0, [tuple(P[idx]) + (cu, cv), ring(idx, k) + (cu, cv), ring(idx, k + 1) + (cu, cv), ring(idx, k + 1) + (cu, cv)], tuple(sgn * T[idx]))


def _rc_card(m, a, p0, p1, p2, p3, w, h, paint, both=True):
    """A flat card with corners bottom-left, bottom-right, top-right, top-left, a w x h texel region painted by paint(i, j, w, h) -> (rgba, glow) (j down);
    both sides share the region (mirrored, so draw only symmetric things on a two-sided card)."""
    u0, v0 = a.alloc(w, h)
    a.paint(u0, v0, w, h, lambda lx, ly: paint(int(lx), int(ly), w, h))
    P = [np.array(p, np.float64) for p in (p0, p1, p2, p3)]
    n = np.cross(P[1] - P[0], P[3] - P[0])
    uv = [(u0 + 0.01, v0 + h - 0.01), (u0 + w - 0.01, v0 + h - 0.01), (u0 + w - 0.01, v0 + 0.01), (u0 + 0.01, v0 + 0.01)]
    m.quad(0, [tuple(P[k]) + uv[k] for k in range(4)], tuple(n))
    if both:
        m.quad(0, [tuple(P[k]) + uv[k] for k in (1, 0, 3, 2)], tuple(-n))


def _rc_chain(m, a, start, direction, count, pitch, link_len, link_w, r, paint, sw=1, plane_up=(0.0, 0.0, 1.0)):
    """A hanging chain: 'count' elongated links along 'direction' from 'start', each turned a quarter to the one before."""
    d = np.array(direction, np.float64)
    d /= np.linalg.norm(d)
    up = np.array(plane_up, np.float64)
    side = np.cross(d, up)
    side /= np.linalg.norm(side)
    up = np.cross(side, d)
    for i in range(count):
        c = np.array(start, np.float64) + d * (pitch * i + link_len / 2)
        wv = side if i % 2 == 0 else up
        path = []
        for q in range(12):
            t = 2 * math.pi * q / 12
            ca, sa = math.cos(t), math.sin(t)
            # a stadium: long straight sides, round ends
            ex = math.copysign(abs(ca) ** 0.8, ca)
            ey = math.copysign(abs(sa) ** 0.8, sa)
            path.append(tuple(c + d * ex * link_len / 2 + wv * ey * link_w / 2))
        _rc_tube(m, a, path, r, 5, paint, sw=sw, closed=True)


def _rc_shade(th, lo=0.74, amp=0.34, phase=2.2):
    return lo + amp * math.cos(2 * math.pi * th - phase)


def _rc_cum(prof):
    cum = [0.0]
    for (y0, r0), (y1, r1) in zip(prof, prof[1:]):
        cum.append(cum[-1] + math.hypot(y1 - y0, r1 - r0))
    return cum


def _rc_clip(c):
    return np.clip(np.array(c, np.float32), 0, 255)


# palettes
_RC_IRON = (60, 64, 88)
_RC_BRASS = (200, 142, 56)
_RC_BRASS_L = (252, 210, 112)
_RC_BRASS_D = (112, 72, 30)


def _rc_iron_lathe(base=_RC_IRON, brass_segs=(), glow_segs=(), glow_rgb=None, rune=None, salt=0):
    """Painter for a lathe: dark iron with grain, a lit edge band at both ends of every segment, brass on the listed segments, optional glowing runes."""
    def p(k, s, d, seg, th):
        x = (k + s) * 2.0
        if seg in brass_segs:
            c = np.array(_RC_BRASS, np.float32) * _rc_shade(th, 0.7, 0.4) + fz(x * 2, d * 2, 70 + salt) * 26
            if fz(x * 1.3, d * 0.9, 71 + salt) > 0.32:
                c = c * 0.78 + np.array((90, 120, 90), np.float32) * 0.22                 # verdigris
            return col(_rc_clip(c)), None
        c = np.array(base, np.float32) * _rc_shade(th, 0.72, 0.42) + nz(x, d * 1.3, 60 + salt) * 30 + fz(x * 2, d * 2, 61 + salt) * 14
        g = None
        if glow_rgb is not None and seg in glow_segs and rune is not None and rune(k, s, d):
            return col(glow_rgb), col(glow_rgb, 1.0)
        return col(_rc_clip(c)), g
    return p


# ================================================================ the Ki-Resonance Lantern
_RC_FLAMES = {
    # state: (outer, mid, core, glint on the glass, runes, glow tint, pulse speed, pulse min)
    "amber": ((224, 108, 28), (255, 176, 52), (255, 244, 196), (255, 196, 90), (255, 176, 60), "FFFFFF", 0.30, 0.62),
    "blue": ((38, 96, 224), (86, 176, 255), (226, 248, 255), (130, 200, 255), (110, 190, 255), "FFFFFF", 0.30, 0.62),
    "red": ((168, 22, 30), (255, 80, 58), (255, 222, 200), (255, 110, 90), (255, 84, 66), "FFFFFF", 0.30, 0.62),
    "off": None,
}
_RC_LANTERN_IDS = {"ki_resonance_lantern": "amber", "ki_resonance_lantern_off": "off", "ki_resonance_lantern_blue": "blue", "ki_resonance_lantern_red": "red"}

_RC_HEX = 6
_RC_LR = 10.5                     # radius of the cage's corner posts
_RC_Y_DECK, _RC_Y_CAGE = 6.2, 30.4


def _rc_lantern(m, a, state):
    spec = _RC_FLAMES[state]
    lit = spec is not None
    outer, mid, core, glint, runec = (spec[0], spec[1], spec[2], spec[3], spec[4]) if lit else (None,) * 5
    R = _RC_LR

    # ---- the base: a hexagonal plinth, runes round its flare, a brass deck rim
    base_prof = [(0.0, 0.0), (0.0, 12.6), (1.3, 13.8), (4.2, 13.8), (5.4, 12.4), (6.2, 11.6), (6.2, 0.0)]
    bc = _rc_cum(base_prof)

    def runes_on_base(k, s, d):
        dl = d - bc[2]
        return 0.4 < dl < 2.5 and (0.18 < s < 0.34 or 0.62 < s < 0.78)
    _rc_lathe(m, a, base_prof, _RC_HEX, _rc_iron_lathe(brass_segs=(5,), glow_segs=(2,), glow_rgb=runec, rune=runes_on_base if lit else None, salt=1), sw=3)
    # ---- the fuel cup: a brass dish holding a shard of magic stone, the flame's wick
    cup = [(6.2, 3.4), (7.6, 4.4), (10.6, 4.2), (10.6, 3.2), (9.4, 3.0), (9.4, 0.0)]
    _rc_lathe(m, a, cup, 8, lambda k, s, d, seg, th: (col(_rc_clip(np.array(_RC_BRASS if seg < 3 else (70, 44, 24), np.float32) * _rc_shade(th, 0.7, 0.4) + fz(k * 2 + s, d * 2, 72) * 22)), None), sw=2)
    if lit:
        shard = [(9.4, 0.0), (9.4, 2.4), (11.2, 3.0), (12.8, 1.6), (13.6, 0.0)]
        _rc_lathe(m, a, shard, 5, lambda k, s, d, seg, th: (col(mix(mid, core, 0.35 + 0.4 * (k % 2))), col(mid, 1.0)), sw=2, jitter=0.1, salt=4)
    else:
        wick = [(9.4, 0.0), (9.4, 0.7), (12.6, 0.6), (13.0, 0.0)]
        _rc_lathe(m, a, wick, 4, lambda k, s, d, seg, th: (col((38, 34, 38)), None), sw=2)
    # ---- the flame: four crossed cards, each a teardrop with a pale core
    if lit:
        fy0, fy1, fh, fw = 10.6, 30.0, 26, 17

        def flame_paint(i, j, w, h):
            x = (i + 0.5 - w / 2) / (w / 2)
            y = 1.0 - (j + 0.5) / h
            sway = 0.16 * math.sin(math.pi * 1.4 * y + 0.6)
            wd = 0.96 * math.sin(math.pi * min(1.0, max(0.0, y) ** 0.66))
            if wd <= 0.04 or abs(x - sway) > wd:
                return None, None
            u = abs(x - sway) / wd
            c = core if (u < 0.34 and 0.06 < y < 0.62) else mid if (u < 0.7 and y < 0.84) else outer
            c = tuple(np.array(c, np.float32) * (0.86 + 0.14 * (1 - y)))
            return col(c), col(c, 1.0)
        for (ang, hw_) in ((0.0, fw / 2), (math.pi / 4, fw / 2), (math.pi / 2, fw / 2), (3 * math.pi / 4, fw / 2)):
            dx, dz = math.cos(ang) * hw_, math.sin(ang) * hw_
            _rc_card(m, a, (-dx, fy0, -dz), (dx, fy0, dz), (dx, fy1, dz), (-dx, fy0 + (fy1 - fy0), -dz), fw, fh, flame_paint)
    # ---- the six posts, brass collars at their ends
    for q in range(_RC_HEX):
        ang = 2 * math.pi * q / _RC_HEX
        post = [(0.0, 0.0), (0.0, 1.5), (1.1, 1.6), (1.1, 1.0), (1.9, 1.5), (2.7, 1.0), (_RC_Y_CAGE - _RC_Y_DECK - 2.7, 1.0), (_RC_Y_CAGE - _RC_Y_DECK - 1.9, 1.5),
                (_RC_Y_CAGE - _RC_Y_DECK - 1.1, 1.0), (_RC_Y_CAGE - _RC_Y_DECK - 1.1, 1.6), (_RC_Y_CAGE - _RC_Y_DECK, 1.5), (_RC_Y_CAGE - _RC_Y_DECK, 0.0)]
        _rc_lathe(m, a, post, 4, _rc_iron_lathe(brass_segs=(1, 2, 3, 4, 7, 8, 9, 10), salt=q), sw=2, base=(R * math.cos(ang), _RC_Y_DECK, R * math.sin(ang)))
    # ---- the panes: an iron diamond lattice with glints of glass, open between (you see the flame through them)
    y0p, y1p = _RC_Y_DECK + 1.6, _RC_Y_CAGE - 1.6
    pw_, ph_ = int(round(R)), int(round(y1p - y0p))

    def pane_paint(i, j, w, h):
        edge = (j == 0 or j == h - 1)
        d1, d2 = (i + j) % 11, (i - j) % 11
        if edge or d1 == 0 or d2 == 0:
            n = fz(i * 2, j * 2, 80) * 14
            return col(_rc_clip(np.array((78, 82, 108), np.float32) + n + (34 if (d1 == 0 and d2 == 0) else 0))), None
        if (i * 5 + j * 3) % 37 == 0:                                                   # a few glints of glass
            return col((200, 230, 244), 0.95), (col(glint, 0.5) if lit else None)
        return None, None
    for q in range(_RC_HEX):
        a0, a1 = 2 * math.pi * q / _RC_HEX, 2 * math.pi * (q + 1) / _RC_HEX
        A = (R * math.cos(a0), R * math.sin(a0))
        Bp = (R * math.cos(a1), R * math.sin(a1))
        _rc_card(m, a, (A[0], y0p, A[1]), (Bp[0], y0p, Bp[1]), (Bp[0], y1p, Bp[1]), (A[0], y1p, A[1]), pw_, ph_, pane_paint)
    # ---- the roof: a flange with a brass rim, a pointed hex roof with runes, a finial
    y = _RC_Y_CAGE
    roof = [(y - 0.8, 0.0), (y - 0.8, 12.6), (y + 0.4, 14.0), (y + 1.6, 14.0), (y + 2.4, 12.2), (y + 3.4, 11.6), (y + 7.0, 8.2), (y + 10.4, 4.6), (y + 12.0, 2.8),
            (y + 12.8, 2.8), (y + 13.8, 1.7), (y + 14.8, 0.0)]
    rc = _rc_cum(roof)

    def runes_on_roof(k, s, d):
        dl = d - rc[5]
        return 0.8 < dl < 3.6 and (0.2 < s < 0.34 or 0.66 < s < 0.8)
    _rc_lathe(m, a, roof, _RC_HEX, _rc_iron_lathe(brass_segs=(2, 3, 4, 9), glow_segs=(5,), glow_rgb=runec, rune=runes_on_roof if lit else None, salt=7), sw=3)
    for q in range(_RC_HEX):                                                # little brass finials on the flange corners
        ang = 2 * math.pi * q / _RC_HEX
        _rc_lathe(m, a, [(0.0, 0.0), (0.0, 1.3), (1.4, 0.9), (3.6, 0.0)], 4, _rc_iron_lathe(brass_segs=(0, 1, 2), salt=20 + q), sw=2,
                  base=(13.2 * math.cos(ang), y + 0.4, 13.2 * math.sin(ang)))
    # ---- the ring to hold it by
    cy = y + 14.8 + 3.4
    ring = [(4.2 * math.cos(2 * math.pi * q / 20), cy + 4.2 * math.sin(2 * math.pi * q / 20), 0.0) for q in range(20)]
    _rc_tube(m, a, ring, 1.05, 6, lambda k, s, d, th: (col(_rc_clip(np.array(_RC_BRASS, np.float32) * _rc_shade(th, 0.7, 0.4) + fz(k + s, d * 2, 73) * 22)), None), sw=2, closed=True)
    return dict(glow=(spec[5], spec[6], spec[7], 1.0)) if lit else dict()


def _rc_lantern_fn(state):
    @_xb_packed
    def build(m, a):
        return _rc_lantern(m, a, state)
    build.__doc__ = "The Ki-Resonance Lantern with the flame '%s'." % state
    return build



# ================================================================ the Anti-Bird Compass
def _rc_bronze(k, s, d, seg, th, salt=0):
    """Worn bronze: grain, a darker side, verdigris in patches, a few bright dents."""
    x = (k + s) * 2.0
    c = np.array((190, 118, 52), np.float32) * _rc_shade(th, 0.7, 0.4) + nz(x, d * 1.3, 110 + salt) * 34 + fz(x * 2, d * 2, 111 + salt) * 16
    if nz(x * 0.7, d * 0.8, 112 + salt) > 0.15:
        c = c * 0.78 + np.array((70, 130, 106), np.float32) * 0.22
    if hsh(int(x), int(d), 113 + salt) > 0.96:
        c = c * 1.25 + 14
    return col(_rc_clip(c)), None


def _rc_dial(k, s, d, seg, th, rho):
    """The dial, in polar texels: a deep green with two rings, sixteen ticks, the four cardinal ones long and glowing; a green triangle marks up."""
    c = np.array((22, 50, 46), np.float32) + (1 - rho) * np.array((14, 34, 26), np.float32) + fz(th * 40, rho * 20, 120) * 10
    g = None
    for r0 in (0.34, 0.7):
        if abs(rho - r0) < 0.035:
            c = np.array((52, 112, 92), np.float32)
    t16 = (th * 16) % 1.0
    near = min(t16, 1 - t16) < 0.07
    cardinal = abs(((th * 4) % 1.0) - 0.0) < 0.05 or abs(((th * 4) % 1.0) - 1.0) < 0.05
    if near and rho > 0.84:
        c = np.array((236, 224, 170), np.float32)
    if cardinal and rho > 0.74:
        c = np.array((255, 236, 160), np.float32)
        g = col((255, 226, 130), 0.8)
    if abs(th - 0.75) < 0.028 and 0.5 < rho < 0.7:                                        # the green triangle that marks up
        c = np.array((150, 255, 120), np.float32)
        g = col((150, 255, 120), 0.9)
    if rho < 0.1:
        c = np.array((120, 90, 44), np.float32)
    return col(_rc_clip(c)), g


@_xb_packed
def anti_bird_compass(m, a):
    """Anti-Bird Compass: a bronze case round a dark green dial, a feather-shaped needle (a pale tail, a biomass-green tip, neutral: pointing up), a few
    glints of glass over it, and a crow's feather hanging off the ring."""
    prof = [(-3.2, 0.0), (-3.2, 11.6), (-2.4, 14.4), (2.8, 15.0), (3.9, 14.0), (3.9, 12.3), (3.0, 12.0), (1.4, 11.8), (1.4, 0.0)]
    cum = [0.0]
    for (y0, r0), (y1, r1) in zip(prof, prof[1:]):
        cum.append(cum[-1] + math.hypot(y1 - y0, r1 - r0))

    def case_paint(k, s, d, seg, th):
        if seg == 7:                                                  # the dial
            rho = max(0.0, min(1.0, 1.0 - (d - cum[7]) / 11.8))
            return _rc_dial(k, s, d, seg, th, rho)
        if seg == 4:                                                  # the polished rim, ticks engraved round it
            c, _ = _rc_bronze(k, s, d, seg, th, 5)
            c = np.array(c, np.float32) * 1.18 + 14
            if (th * 32) % 1.0 < 0.14:
                c = c * 0.5
            return col(_rc_clip(c)), None
        if seg == 6:
            c, _ = _rc_bronze(k, s, d, seg, th, 9)
            return col(_rc_clip(np.array(c, np.float32) * 0.72)), None
        return _rc_bronze(k, s, d, seg, th)
    _rc_lathe(m, a, prof, 32, case_paint, sw=2, direction=(0.0, 0.0, 1.0))
    # ---- the needle: one card above the dial
    def needle(i, j, w, h):
        x = (i + 0.5 - w / 2) / (w / 2)
        y = 1.0 - (j + 0.5) / h
        wd = math.sin(math.pi * min(1.0, max(0.0, y)) ** 0.9) ** 0.75
        if abs(x) > wd * 0.98:
            return None, None
        if abs(x) < 0.16:
            return col((255, 244, 190)), (col((200, 255, 150), 0.5) if y > 0.45 else None)
        if y > 0.44:
            return col((170, 250, 112)), col((150, 255, 110), 0.9)
        return col((232, 232, 242) if abs(x) < 0.6 else (190, 190, 206)), None
    _rc_card(m, a, (-3.2, -9.0, 2.15), (3.2, -9.0, 2.15), (3.2, 13.0, 2.15), (-3.2, 13.0, 2.15), 8, 22, needle)
    _rc_lathe(m, a, [(0.0, 0.0), (0.0, 2.2), (1.1, 1.8), (1.9, 1.0), (2.2, 0.0)], 8, lambda k, s, d, seg, th: _rc_bronze(k, s, d, seg, th, 3), sw=2,
              base=(0.0, 0.0, 2.1), direction=(0.0, 0.0, 1.0))
    # ---- a few glints of glass over the dial
    def glints(i, j, w, h):
        if abs((i - 1) + (j - 9)) < 0.9 and 1 <= i <= 5:                                 # one streak and a dot, high on the left of the dial
            return col((226, 246, 255), 0.95), None
        if (i, j) == (7, 3):
            return col((226, 246, 255), 0.95), None
        return None, None
    _rc_card(m, a, (-8.5, 4.5, 3.7), (-0.5, 4.5, 3.7), (-0.5, 12.5, 3.7), (-8.5, 12.5, 3.7), 8, 8, glints, both=False)
    # ---- the neck, the ring, and the crow's feather
    _rc_lathe(m, a, [(0.0, 0.0), (0.0, 2.4), (1.0, 1.7), (2.0, 1.4), (2.2, 0.0)], 8, lambda k, s, d, seg, th: _rc_bronze(k, s, d, seg, th, 4), sw=2, base=(0.0, 14.7, 0.0))
    cy = 19.6
    ring = [(2.9 * math.cos(2 * math.pi * q / 16), cy + 2.9 * math.sin(2 * math.pi * q / 16), 0.0) for q in range(16)]
    _rc_tube(m, a, ring, 0.85, 6, lambda k, s, d, th: _rc_bronze(k, s, d * 2, 0, th, 6), sw=2, closed=True)
    ang = math.radians(48)
    ux, uy = math.cos(ang), math.sin(ang)
    vx, vy = -uy, ux
    bx, by, L, hw = 2.2, 21.8, 14.0, 3.4

    def feather(i, j, w, h):
        x = (i + 0.5 - w / 2) / (w / 2)
        y = 1.0 - (j + 0.5) / h
        wf = 0.98 * math.sin(math.pi * min(1.0, 0.12 + 0.88 * y) ** 0.85) ** 0.7
        notch = ((j * 3 + (1 if x > 0 else 0) * 2) % 6 == 0) and abs(x) > wf * 0.7
        if abs(x) > wf or notch:
            return None, None
        if abs(x) < 0.13:
            return col((226, 214, 180)), None
        if 0.13 <= x < 0.5 and (j % 5) != 0:
            return col((72, 150, 128)), col((90, 200, 160), 0.3)
        return col(_rc_clip(np.array((34, 32, 48), np.float32) + fz(i * 2, j * 2, 130) * 10)), None
    _rc_card(m, a, (bx - vx * hw, by - vy * hw, 0.0), (bx + vx * hw, by + vy * hw, 0.0), (bx + vx * hw + ux * L, by + vy * hw + uy * L, 0.0),
             (bx - vx * hw + ux * L, by - vy * hw + uy * L, 0.0), 9, 28, feather)
    return dict(glow=("B8FFB0", 0.07, 0.5, 1.0))


# ================================================================ the Glutton's Chalice
def _rc_pn(x, y, W, salt=0):
    """Noise that wraps round a strip of width W (so a lathe's seam does not show)."""
    t = (x % W) / W
    return nz(x, y, salt) * (1 - t) + nz(x - W, y, salt) * t


_RC_VEIN = (190, 30, 70)
_RC_VEINS = [(u, 0.9 + 0.9 * hsh(u, 5, 12), 1.4 + 2.6 * hsh(u, 6, 13)) for u in range(0, 9)]


def _rc_vein(x, d, W):
    """How much vein is at texel (x round the strip of width W, d along the profile): 0..1. Eight main veins climb the bowl, swaying, each throwing
    two branches, and they fade out below the stem."""
    best = 0.0
    for n, (u0, ph, sway) in enumerate(_RC_VEINS):
        u = (u0 * W / 9.0 + 3.0 * math.sin(d * 0.19 + ph * 4)) % W
        dx = abs(((x - u + W / 2) % W) - W / 2)
        thick = 0.6 + 0.45 * min(1.0, max(0.0, (d - 8) / 22.0))                          # thicker towards the bowl
        if 5.0 < d < 62.0:
            best = max(best, 1.0 - dx / thick)
        for (db, sg) in ((14.0 + 6.0 * ph, 1), (27.0 + 6.0 * ph, -1)):                  # branches
            if 0.0 < d - db < 15.0 and 5.0 < d < 62.0:
                ub = (u0 * W / 9.0 + 3.0 * math.sin(db * 0.19 + ph * 4) + sg * 0.7 * (d - db)) % W
                dxb = abs(((x - ub + W / 2) % W) - W / 2)
                best = max(best, 1.0 - dxb / (0.8 * (1 - (d - db) / 16.0) + 0.15))
    return best if best > 0.0 else 0.0


@_xb_packed
def glutton_chalice(m, a):
    """The Glutton's Chalice: a living goblet of bone. Teeth round the rim, a glowing green stew, red veins that glow and crawl over the bowl, down the
    stem and out over the foot as tendrils."""
    prof = [(0.0, 0.0), (0.0, 8.4), (1.4, 10.0), (3.0, 10.4), (4.6, 8.6), (6.6, 5.4), (9.0, 3.4), (12.0, 2.6), (15.0, 2.3), (17.0, 3.6), (19.0, 2.4), (22.0, 2.7),
            (24.0, 4.8), (27.0, 8.6), (31.0, 11.8), (36.0, 13.6), (41.0, 14.0), (44.0, 13.2), (45.6, 13.8), (46.2, 13.0), (46.2, 11.9),
            (43.0, 11.9), (38.0, 11.2), (37.4, 11.0), (37.4, 0.0)]
    cum = [0.0]
    for (y0, r0), (y1, r1) in zip(prof, prof[1:]):
        cum.append(cum[-1] + math.hypot(y1 - y0, r1 - r0))
    sides, sw = 12, 5
    Wn = sides * sw * 1.0

    def bone(x, d, salt):
        return np.array((236, 224, 194), np.float32) + _rc_pn(x, d * 1.1, Wn, 90 + salt) * 62 + fz(x * 2, d * 2, 91 + salt) * 18

    def paint(k, s, d, seg, th):
        x = (k + s) * sw
        if seg == len(prof) - 2:                                                    # the stew: swirling green, bright, glowing
            rho = 1.0 - (d - cum[seg]) / 11.0
            sw_ = _rc_pn(x * 0.9 + rho * 14, rho * 20, Wn, 96)
            c = np.array((70, 188, 96), np.float32) + sw_ * 150 + (1 - rho) * np.array((60, 40, 20), np.float32)
            if hsh(int(x), int(rho * 12), 97) > 0.93:
                c = np.array((220, 255, 180), np.float32)                              # bubbles
            return col(_rc_clip(c)), col(_rc_clip(c * 0.95 + 20), 1.0)
        if seg >= 20:                                                                # the rim's top and the inside of the bowl
            c = bone(x, d, 3) * (1.02 if seg < 21 else 0.78)
            if seg >= 21:
                t = min(1.0, max(0.0, (d - cum[21]) / (cum[23] - cum[21])))
                c = c * (1 - 0.55 * t) + np.array((60, 160, 90), np.float32) * 0.55 * t           # stained green towards the stew
            return col(_rc_clip(c)), (col((90, 230, 130), 0.25 * min(1.0, max(0.0, (d - cum[21]) / (cum[23] - cum[21])))) if seg >= 21 else None)
        c = bone(x, d, 0) * _rc_shade(th, 0.74, 0.34)
        if nz(x * 1.2, d * 0.6, 98) < -0.24:
            c = c * 0.86                                                              # pits
        g = None
        w = _rc_vein(x, d, Wn)
        if w > 0.25:                                                                  # the veins, running up the bone
            c = np.array(_RC_VEIN, np.float32) * (0.8 if w < 0.5 else 1.0) + fz(x * 2, d * 2, 101) * 14
            g = col((255, 110, 140), 0.4 if w > 0.5 else 0.22)
        return col(_rc_clip(c)), g
    _rc_lathe(m, a, prof, sides, paint, sw=sw, jitter=0.035, salt=3)
    # ---- the teeth round the rim, a little uneven, leaning in
    n_teeth = 26
    for q in range(n_teeth):
        ph = 2 * math.pi * (q + 0.25 * math.sin(q * 2.3)) / n_teeth
        hgt = 2.3 + 1.3 * hsh(q, 7, 8)
        lean = 0.30
        base = (12.5 * math.cos(ph), 46.0, 12.5 * math.sin(ph))
        direction = (-lean * math.cos(ph), 1.0, -lean * math.sin(ph))

        def tooth(k, s, d, seg, th):
            c = np.array((252, 246, 228), np.float32) * (0.82 + 0.18 * math.cos(2 * math.pi * th - 1.0)) + fz(k * 2 + s, d * 3, 102) * 8
            if d < 1.0:
                c = np.array((214, 126, 138), np.float32)                                   # the gum
            return col(_rc_clip(c)), None
        _rc_lathe(m, a, [(0.0, 0.0), (0.0, 1.3), (hgt * 0.6, 0.95), (hgt, 0.0)], 4, tooth, sw=2, base=base, direction=direction)
    # ---- tendrils: four crawling out over the table from the foot, three twining up the stem
    def vein_paint(k, s, d, th):
        c = np.array((176, 32, 62), np.float32) * (0.8 + 0.3 * math.cos(2 * math.pi * th - 1.2)) + fz(k + s, d * 2, 103) * 14
        return col(_rc_clip(c)), col((255, 110, 140), 0.3)
    for q in range(4):
        ph = 0.5 + q * math.pi / 2 + 0.3 * math.sin(q)
        pts, rads = [], []
        n = 12
        for i in range(n):
            t = i / (n - 1)
            r = 9.2 + 11.0 * t
            ang = ph + 0.9 * math.sin(t * 2.4 + q)
            pts.append((r * math.cos(ang), 1.2 + 4.2 * (t ** 3) + 0.4 * math.sin(t * 9), r * math.sin(ang)))
            rads.append(1.7 * (1 - t) + 0.45)
        _rc_tube(m, a, pts, rads, 5, vein_paint, sw=1, caps=(False, True))
    for q in range(3):
        ph = q * 2 * math.pi / 3 + 0.4
        pts, rads = [], []
        n = 16
        for i in range(n):
            t = i / (n - 1)
            y = 4.5 + 20.0 * t
            r_stem = 2.6 + 0.8 * math.sin(t * math.pi) + (1.8 * max(0.0, t - 0.8) * 5)
            ang = ph + 2.6 * t * math.pi
            pts.append(((r_stem + 0.45) * math.cos(ang), y, (r_stem + 0.45) * math.sin(ang)))
            rads.append(0.95 * (1 - 0.55 * t) + 0.3)
        _rc_tube(m, a, pts, rads, 5, vein_paint, sw=1, caps=(False, True))
    return dict(glow=("B4FFC4", 0.075, 0.38, 1.0))


# ================================================================ the Devil-Bound Weapon Sheath
_RC_SH_ST = [(0.0, 3.7, 2.7), (1.2, 4.1, 3.1), (2.2, 3.7, 2.7), (9.0, 3.7, 2.7), (10.0, 4.0, 3.0), (11.0, 3.6, 2.6), (56.0, 3.3, 2.4), (76.0, 2.7, 2.0),
             (80.0, 2.5, 1.9), (82.0, 3.0, 2.3), (84.0, 3.0, 2.3), (85.0, 2.3, 1.8), (87.0, 1.2, 1.0), (88.6, 0.3, 0.3)]


def _rc_sh_r(y):
    st = _RC_SH_ST
    for (y0, x0, z0), (y1, x1, z1) in zip(st, st[1:]):
        if y0 <= y <= y1:
            t = (y - y0) / max(1e-6, y1 - y0)
            return x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
    return st[-1][1], st[-1][2]


def _rc_chain_paint(light=(182, 188, 210), dark=(104, 108, 134), period=3.0):
    def p(k, s, d, th):
        link = int(d / period)
        ph = (d % period) / period
        c = np.array(light if link % 2 == 0 else dark, np.float32) * (0.5 + 0.55 * math.sin(math.pi * ph)) * (0.78 + 0.34 * math.cos(2 * math.pi * th - 1.6))
        c = c + fz(k + s, d * 2, 150) * 10
        return col(_rc_clip(c)), None
    return p


@_xb_packed
def devil_bound_sheath(m, a):
    """Devil-Bound Weapon Sheath: a black lacquered scabbard (a violet sheen, hairline cracks of glowing power) in an iron mouth collar and chape, wound with two
    counter-turned iron chains, an iron seal band with a violet-red sigil on a medallion, a void glowing in its mouth, two horns by the chape, and a chain
    lanyard with a crystal drop. The mouth is at the bottom of the axis, the chape at the top."""
    def body(k, s, y, t):
        x = (k + s) * 2.0
        if y < 10.2:                                                                 # the iron mouth collar with brass bands
            c = np.array((64, 66, 90), np.float32) * (0.74 + 0.4 * math.cos(2 * math.pi * t - 2.2)) + nz(x, y * 1.4, 160) * 22 + fz(x * 2, y * 2, 161) * 10
            if 1.0 < y < 2.4 or 8.6 < y < 9.8:
                c = np.array(_RC_BRASS, np.float32) * (0.72 + 0.4 * math.cos(2 * math.pi * t - 2.0)) + fz(x * 2, y * 2, 162) * 20
            return col(_rc_clip(c)), None
        if y > 79.5:
            c = np.array((78, 70, 112), np.float32) * (0.7 + 0.45 * math.cos(2 * math.pi * t - 2.2)) + nz(x, y * 1.4, 163) * 18
            if 81.5 < y < 84.5:
                c = c * 1.18
            return col(_rc_clip(c)), (col((230, 70, 150), 0.25) if y > 86 else None)
        c = np.array((24, 19, 36), np.float32) + nz(x * 0.6, y * 0.1, 164) * 8 + fz(x * 2, y * 0.5, 165) * 6
        sheen = math.exp(-((((t - 0.34) + 0.5) % 1.0 - 0.5) / 0.07) ** 2)
        c = c + sheen * np.array((44, 30, 66), np.float32)
        g = None
        if fz(x * 3.1, y * 0.35, 166) > 0.31 and y > 16:                           # a scratch
            c = c + 26
        crack = abs(_rc_pn(x * 0.7, y * 0.16, 32.0, 167))
        if crack < 0.017 and y > 14:                                                  # the power seeping through the lacquer
            c = np.array((190, 56, 150), np.float32)
            g = col((255, 80, 190), 0.75)
        return col(_rc_clip(c)), g
    _bc_tube(m, a, _RC_SH_ST, 16, body, sw=2, p=2.2, caps=(False, True))
    # ---- the glowing void in the mouth
    yy, xx = np.mgrid[0:6, 0:8] + 0.5
    ell = ((xx - 4) / 3.7) ** 2 + ((yy - 3) / 2.6) ** 2 <= 1.0

    def void(i, j, front):
        d = math.hypot((i + 0.5 - 4) / 3.7, (j + 0.5 - 3) / 2.6)
        c = np.array((120, 30, 120), np.float32) * (1 - d) + np.array((30, 8, 40), np.float32) * d
        return col(_rc_clip(c)), col((255, 70, 180), 0.9 * (1 - d * 0.6))
    plate(m, a, ell, -4.0, -3.0, 0.12, void, "xz", -0.2)
    # ---- the chains: two wraps, turned against each other
    def helix(y0, y1, turns, phi0, base_r):
        """Points along a helix wound round the scabbard, one per unit of length, and a bead radius that swells every third point (a chain's links)."""
        approx = math.hypot(abs(turns) * 2 * math.pi * 4.2, y1 - y0)
        n = max(8, int(approx / 1.35))
        pts, rads = [], []
        for i in range(n + 1):
            t = i / n
            y = y0 + (y1 - y0) * t
            ph = phi0 + turns * 2 * math.pi * t
            rx, rz = _rc_sh_r(y)
            pts.append(((rx + 0.8) * math.cos(ph), y, (rz + 0.8) * math.sin(ph)))
            rads.append(base_r * (0.64 + 0.5 * max(0.0, math.cos(2 * math.pi * i / 3.0))))
        return pts, rads
    for (y0, y1, turns, phi0, rb, cl) in ((13.0, 46.0, 3.0, 0.4, 0.8, ((182, 188, 210), (104, 108, 134))), (64.0, 78.0, 2.0, 2.3, 0.8, ((196, 202, 224), (112, 116, 142))),
                                          (22.0, 44.0, -2.5, 1.0, 0.7, ((150, 156, 180), (90, 94, 118)))):
        pts, rads = helix(y0, y1, turns, phi0, rb)
        _rc_tube(m, a, pts, rads, 6, _rc_chain_paint(cl[0], cl[1], 4.05), sw=1, caps=(True, True))
    # ---- the seal band and its medallion
    band_st = [(49.0, _rc_sh_r(49.0)[0] + 0.9, _rc_sh_r(49.0)[1] + 0.9), (50.0, _rc_sh_r(50.0)[0] + 1.3, _rc_sh_r(50.0)[1] + 1.3),
               (58.0, _rc_sh_r(58.0)[0] + 1.3, _rc_sh_r(58.0)[1] + 1.3), (59.0, _rc_sh_r(59.0)[0] + 0.9, _rc_sh_r(59.0)[1] + 0.9)]

    def band_paint(k, s, y, t):
        c = np.array((70, 72, 98), np.float32) * (0.74 + 0.4 * math.cos(2 * math.pi * t - 2.2)) + fz((k + s) * 2, y * 2, 168) * 14
        if y < 50.4 or y > 57.6:
            c = np.array(_RC_BRASS, np.float32) * (0.72 + 0.4 * math.cos(2 * math.pi * t - 2.0))
        return col(_rc_clip(c)), None
    _bc_tube(m, a, band_st, 16, band_paint, sw=2, p=2.2, caps=(False, False))
    disc = disc_mask(4.5)
    n9 = disc.shape[0]

    def sigil(i, j, front):
        dx, dy = i + 0.5 - n9 / 2, j + 0.5 - n9 / 2
        r = math.hypot(dx, dy)
        g = None
        c = np.array((30, 24, 44), np.float32)
        if r > 3.6:
            c = np.array((92, 94, 122), np.float32)
        elif abs(r - 2.6) < 0.55:
            c, g = np.array((226, 60, 150), np.float32), col((255, 80, 190), 1.0)
        elif dy > -1.8 and abs(dx) < (dy + 2.2) * 0.62 and abs(abs(dx) - (dy + 2.2) * 0.62) < 0.65 and dy < 1.8:       # a downward triangle
            c, g = np.array((170, 90, 255), np.float32), col((190, 110, 255), 1.0)
        if r < 0.9:
            c, g = np.array((255, 226, 244), np.float32), col((255, 226, 244), 1.0)
        return col(_rc_clip(c)), g
    zf = _rc_sh_r(54.0)[1] + 1.3 + 0.05
    plate(m, a, disc, -n9 / 2, 54.0 - n9 / 2, 0.30, sigil, "xy", zf + 0.30)
    plate(m, a, disc, -n9 / 2, 54.0 - n9 / 2, 0.30, sigil, "xy", -(zf + 0.30))
    # ---- two horns by the chape
    for sx in (-1, 1):
        _rc_lathe(m, a, [(0.0, 0.0), (0.0, 1.5), (2.8, 1.0), (6.0, 0.55), (8.2, 0.0)], 5,
                  lambda k, s, d, seg, th: (col(_rc_clip(np.array((34, 28, 46), np.float32) * _rc_shade(th, 0.7, 0.5) + nz(k * 2 + s, d * 2, 169) * 12)),
                                            (col((255, 70, 150), 0.55) if d > 6.2 else None)),
                  sw=2, base=(sx * 2.4, 73.0, 0.0), direction=(sx * 0.85, 0.7, 0.0))
    # ---- the chain lanyard and its drop
    _rc_chain(m, a, (0.0, -0.9, 0.0), (0.0, -1.0, 0.0), 6, 3.5, 4.8, 3.2, 0.62, _rc_chain_paint((182, 188, 210), (110, 114, 140), 2.4))
    drop = [(0.0, 0.0), (0.0, 1.3), (2.0, 1.9), (4.2, 1.2), (5.8, 0.0)]
    _rc_lathe(m, a, drop, 6, lambda k, s, d, seg, th: (col(mix((190, 60, 150), (255, 150, 230), 0.4 + 0.5 * (k % 2))), col((255, 90, 200), 1.0)), sw=2,
              base=(0.0, -22.6, 0.0), direction=(0.0, -1.0, 0.0))
    return dict(glow=("FFC0EE", 0.1, 0.42, 1.0))


# ================================================================ the Gambler's Grimoire Page
def _rc_hue(h, v=1.0, sat=0.62):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, sat, v)
    return (int(r * 255), int(g * 255), int(b * 255))


_RC_RUNES = ["#.#,###,#.#,.#.,#.#", ".#.,#.#,###,#.#,.#.", "###,.#.,.#.,#.#,###", "#..,###,.#.,###,..#", ".##,#..,.#.,..#,##.", "#.#,.#.,###,.#.,#.#"]


def _rc_rune(idx):
    return [[ch == "#" for ch in row] for row in _RC_RUNES[idx % len(_RC_RUNES)].split(",")]


@_xb_packed
def gamblers_grimoire_page(m, a):
    """The Gambler's Grimoire Page: one torn parchment page, a curled corner, a d20 inked on it in glowing violet and gold, runes down both margins in shifting
    colours, scorched edges that leak a violet light, and motes floating round it."""
    W, H = 32, 42
    mask = np.zeros((H, W), bool)
    for j in range(H):
        for i in range(W):
            top = 1 + int(2.6 * hsh(i // 2, 3, 1))
            bot = H - 1 - int(1.8 * hsh(i // 2, 5, 2))
            left = int(1.6 * hsh(j // 3, 7, 3))
            right = W - 1 - int(1.6 * hsh(j // 3, 9, 4))
            mask[j, i] = top <= j <= bot and left <= i <= right
    for j in range(H):
        for i in range(W):
            if (i - (W - 11)) + (j - (H - 11)) >= 11:
                mask[j, i] = False                                                    # the curled corner is folded away
    pad = np.pad(mask, 4, constant_values=False)
    ed = np.full((H, W), 9.0)
    for dy in range(-4, 5):
        for dx in range(-4, 5):
            if dx == 0 and dy == 0:
                continue
            sh = pad[4 + dy:4 + dy + H, 4 + dx:4 + dx + W]
            d = math.hypot(dx, dy)
            ed = np.where(~sh, np.minimum(ed, d), ed)
    cx, cy, R = 16.0, 19.5, 11.0
    verts = [(cx + R * math.cos(math.radians(90 + 60 * k)), cy - R * math.sin(math.radians(90 + 60 * k))) for k in range(6)]
    tri = [(cx + 0.5 * R * math.cos(math.radians(90 + 120 * k)), cy - 0.5 * R * math.sin(math.radians(90 + 120 * k))) for k in range(3)]

    def seg(p, q, x, y):
        vx, vy = q[0] - p[0], q[1] - p[1]
        t = max(0.0, min(1.0, ((x - p[0]) * vx + (y - p[1]) * vy) / (vx * vx + vy * vy)))
        return math.hypot(x - p[0] - t * vx, y - p[1] - t * vy)

    def inside_tri(X, Y):
        s1 = (tri[1][0] - tri[0][0]) * (Y - tri[0][1]) - (tri[1][1] - tri[0][1]) * (X - tri[0][0])
        s2 = (tri[2][0] - tri[1][0]) * (Y - tri[1][1]) - (tri[2][1] - tri[1][1]) * (X - tri[1][0])
        s3 = (tri[0][0] - tri[2][0]) * (Y - tri[2][1]) - (tri[0][1] - tri[2][1]) * (X - tri[2][0])
        return (s1 >= 0 and s2 >= 0 and s3 >= 0) or (s1 <= 0 and s2 <= 0 and s3 <= 0)
    # the rune glyphs in the margins
    glyph = {}
    for n, j0 in enumerate(range(5, 36, 7)):
        for side, i0 in ((0, 2), (1, 27)):
            rune = _rc_rune(n * 2 + side)
            hue = (n * 0.16 + side * 0.5 + 0.05) % 1.0
            for gy, row in enumerate(rune):
                for gx, on in enumerate(row):
                    if on:
                        glyph[(i0 + gx, j0 + gy)] = hue

    def front_paint(i, j, front):
        X, Y = i + 0.5, j + 0.5
        c = np.array((248, 236, 204), np.float32) + fz(i * 2, j * 2, 140) * 12 + nz(i * 0.7, j * 0.7, 141) * 34
        g = col((206, 176, 255), 0.13)                                                  # the whole page glows a little
        e = ed[j, i]
        if e < 1.2:
            c = np.array((78, 50, 38), np.float32) + fz(i, j, 142) * 10
            g = col((190, 110, 255), 0.6) if hsh(i, j, 143) > 0.55 else None
        elif e < 2.3:
            c = np.array((176, 134, 90), np.float32) * 0.8 + c * 0.2
        elif e < 3.4:
            c = c * 0.9 + np.array((176, 134, 90), np.float32) * 0.1
        hexe = min(seg(verts[k], verts[(k + 1) % 6], X, Y) for k in range(6))
        inner = min(seg(tri[k], tri[(k + 1) % 3], X, Y) for k in range(3))
        conn = min(min(seg(tri[k], verts[(2 * k + 1) % 6], X, Y), seg(tri[k], verts[(2 * k - 1) % 6], X, Y), seg(tri[k], verts[(2 * k) % 6], X, Y)) for k in range(3))
        hue = (math.atan2(Y - cy, X - cx) / (2 * math.pi) + 0.75) % 1.0
        if hexe < 0.82:
            rgb = mix((150, 70, 236), _rc_hue(0.7 + 0.2 * math.sin(hue * 6.28 * 2)), 0.5)
            c, g = np.array(rgb, np.float32), col(rgb, 1.0)
        elif inner < 0.7 or conn < 0.55:
            c, g = np.array((176, 100, 246), np.float32), col((190, 120, 255), 1.0)
        elif inside_tri(X, Y):
            d = math.hypot(X - cx, Y - (cy + 0.9))
            rgb = (255, 252, 222) if d < 1.7 else (255, 226, 126) if d < 3.0 else (248, 188, 76)
            c, g = np.array(rgb, np.float32), col(rgb, 1.0)
        elif math.hypot(X - cx, Y - cy) < R + 1.4 and hsh(i, j, 144) > 0.9:
            c, g = np.array((255, 226, 126), np.float32), col((255, 226, 126), 0.9)              # sparkles round the die
        if (i, j) in glyph and e > 2.0:
            h = glyph[(i, j)]
            rgb = _rc_hue(h, 1.0, 0.7)
            c, g = np.array(rgb, np.float32), col(rgb, 1.0)
        return col(_rc_clip(c)), g

    def back_paint(i, j, front):
        c = np.array((220, 204, 168), np.float32) + fz(i * 2, j * 2, 145) * 12 + nz(i * 0.6, j * 0.6, 146) * 34
        g = None
        e = ed[j, i]
        if e < 1.2:
            c = np.array((70, 46, 36), np.float32)
        elif e < 2.3:
            c = c * 0.78
        hexe = min(seg(verts[k], verts[(k + 1) % 6], i + 0.5, j + 0.5) for k in range(6))
        if hexe < 0.7:
            c = c * 0.86 + np.array((120, 70, 200), np.float32) * 0.14                       # the ink showing through, faint
            g = col((150, 90, 255), 0.25)
        return col(_rc_clip(c)), g
    plate(m, a, mask, -W / 2, -H / 2, 0.45, lambda i, j, f: front_paint(i, j, f) if f else back_paint(i, j, f), "xy", 0.0)

    # ---- the curled corner: the folded-over flap lifts off the page, showing its back
    def flap(i, j, w, h):
        t = j / max(1, h - 1)
        c = np.array((226, 210, 174), np.float32) * (0.7 + 0.35 * t) + fz(i * 2, j * 2, 147) * 8
        g = col((150, 90, 255), 0.18) if (i + j) % 5 == 0 else None
        return col(_rc_clip(c)), g
    A, B, Cp = (W - 11 - W / 2, -H / 2, 0.5), (W - W / 2, H / 2 - (H - 11), 0.5), (W - 11 - W / 2 + 2.4, H / 2 - (H - 11) - 2.4, 3.4)
    _rc_card(m, a, A, B, Cp, Cp, 10, 10, flap)
    # ---- motes of light floating round it
    motes = [(-19.5, 12.0, 3.0, 1.5, (255, 214, 120)), (18.5, 4.0, 4.5, 1.3, (190, 120, 255)), (-18.0, -14.0, 5.0, 1.6, (110, 230, 240)), (19.0, -9.0, 2.5, 1.2, (255, 110, 170)),
             (-12.0, 22.0, 4.0, 1.3, (255, 214, 120)), (13.0, 23.0, 3.2, 1.5, (110, 230, 240)), (1.0, -23.5, 4.5, 1.4, (190, 120, 255)), (-20.0, 0.0, -4.0, 1.4, (255, 110, 170)),
             (20.0, 17.0, -3.5, 1.3, (255, 214, 120))]
    for (x, y, z, sz, rgb) in motes:
        box(m, a, x - sz / 2, y - sz / 2, z - sz / 2, x + sz / 2, y + sz / 2, z + sz / 2, lambda face, i, j, fw, fh, rgb=rgb: (col(rgb), col(rgb, 1.0)))
    return dict(glow=("FFFFFF", 0.09, 0.45, 1.0))


# ================================================================ registration
SPECS += [("devil_bound_sheath", devil_bound_sheath)]
RELICS += [("anti_bird_compass", anti_bird_compass), ("glutton_chalice", glutton_chalice), ("gamblers_grimoire_page", gamblers_grimoire_page)]
RELICS += [(i, _rc_lantern_fn(s)) for i, s in _RC_LANTERN_IDS.items()]
REAL.update({"devil_bound_sheath": 1.1})              # metres: the scabbard 0.88 m and the lanyard
GRIP.update({"devil_bound_sheath": 0.25})             # held round the mouth
