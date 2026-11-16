"""The part generators of the Black Clover boss models: hair tiers, crowns, halos, bracers with tassels, fur mantles, capes, translucent skirts, horns, wings, tails, ears.  See bclib.py.

Every generator draws into a `Build` (one atlas, three geo files):
  main  `pmx_bc_<id>`        always drawn: the silhouette (hair shell, crown, mantle, cape, wings, horns, bracers)
  fine  `pmx_bc_<id>_fine`   drawn only within the detail distance (24 blocks, the same rule that drops the kit's hair): single strands, fringes, tassels, fur tufts, skirt fringe
  fx    `pmx_bc_<id>_fx`     the glowing parts (halo, gems, runes) with the additive glow pass; the glow pass is dropped beyond the far distance by the kit
Coordinates are the geo file's (see bclib).  A card is a single face (the cutout type draws both sides): one quad.
"""
import math

import numpy as np

import bclib as L
from bclib import Atlas, Geo, Anim, rgb, tone, hsh


class Build:
    def __init__(self, name, atlas_size=256):
        self.name = name
        self.atlas = Atlas(atlas_size, atlas_size)
        self.layers = {"main": Geo("pmx_bc_" + name, self.atlas), "fine": Geo("pmx_bc_" + name + "_fine", self.atlas), "fx": Geo("pmx_bc_" + name + "_fx", self.atlas)}
        self.anims = {"main": Anim(), "fine": Anim(), "fx": Anim()}
        self.slim = False
        self.n_sway = 0

    def g(self, layer):
        return self.layers[layer]

    def sway(self, layer, anchor, name, pivot, ax=2.0, az=1.5, phase=None, walk=1.0, run=2.0, cast=1.0, spin=0.0, bob=0.0):
        """A child bone of `anchor` that swings about `pivot` (an animation target for the cubes that go into it)."""
        geo = self.g(layer)
        geo.bone(name, anchor, pivot)
        if phase is None:
            phase = (self.n_sway * 1.7) % math.tau
            self.n_sway += 1
        self.anims[layer].sway(name, ax=ax, az=az, phase=phase, walk=walk, run=run, cast=cast, spin=spin, bob=bob)
        return name

    # ---- tiles
    def t_strand(self, color, tw=12, th=48, seed=0, **kw):
        return self.atlas.tile(("strand", L.hexs(rgb(color)), tw, th, seed, tuple(sorted(kw.items()))), tw, th, lambda: L.m_strand(color, tw, th, seed, **kw))

    def t_cloth(self, color, tw=32, th=32, seed=0, **kw):
        return self.atlas.tile(("cloth", L.hexs(rgb(color)), tw, th, seed, tuple(sorted((k, str(v)) for k, v in kw.items()))), tw, th, lambda: L.m_cloth(color, tw, th, seed, **kw))

    def t_metal(self, color="#e8b830", tw=16, th=16, **kw):
        return self.atlas.tile(("metal", L.hexs(rgb(color)), tw, th, tuple(sorted(kw.items()))), tw, th, lambda: L.m_metal(color, tw, th, **kw))

    def t_fur(self, color, tw=32, th=16, seed=0, **kw):
        return self.atlas.tile(("fur", L.hexs(rgb(color)), tw, th, seed, tuple(sorted(kw.items()))), tw, th, lambda: L.m_fur(color, tw, th, seed, **kw))

    def t_mesh(self, color, tw=16, th=16, seed=0, **kw):
        return self.atlas.tile(("mesh", L.hexs(rgb(color)), tw, th, seed, tuple(sorted(kw.items()))), tw, th, lambda: L.m_mesh(color, tw, th, seed, **kw))

    def t_gem(self, color="#d81c40", tw=8, th=8):
        return self.atlas.tile(("gem", L.hexs(rgb(color)), tw, th), tw, th, lambda: L.m_gem(color, tw, th))

    def t_ring(self, color="#fff2a8", tw=64, th=64, rings=2, seed=0):
        return self.atlas.tile(("ring", L.hexs(rgb(color)), tw, th, rings, seed), tw, th, lambda: L.m_ring(color, tw, th, rings, seed))

    def t_horn(self, color="#2a2230", tw=8, th=24, **kw):
        return self.atlas.tile(("horn", L.hexs(rgb(color)), tw, th, tuple(sorted(kw.items()))), tw, th, lambda: L.m_horn(color, tw, th, **kw))

    def t_membrane(self, color="#241a34", vein="#6a4a8a", tw=32, th=32, **kw):
        return self.atlas.tile(("mem", L.hexs(rgb(color)), L.hexs(rgb(vein)), tw, th, tuple(sorted(kw.items()))), tw, th, lambda: L.m_membrane(color, vein, tw, th, **kw))

    def t_flat(self, color, tw=8, th=8, shade=0.18):
        return self.atlas.tile(("flat", L.hexs(rgb(color)), tw, th, shade), tw, th, lambda: L.m_flat(color, tw, th, shade))

    def t_rune(self, color="#ffe9a0", tw=16, th=16, seed=0):
        return self.atlas.tile(("rune", L.hexs(rgb(color)), tw, th, seed), tw, th, lambda: L.m_rune(color, tw, th, seed))

    def t_mass(self, color, tw=16, th=16, seed=0):
        """A hair MASS block (a solid clump texture with lit and shaded streaks and a lit top)."""
        def paint():
            a = np.zeros((th, tw, 4), np.float32)
            a[..., 3] = 1.0
            ramp = L._grad(th, [(0.0, tone(color, 1.32)), (0.35, tone(color, 1.05)), (0.7, rgb(color)), (1.0, tone(color, 0.62))])
            a[..., :3] = ramp[:, None, :]
            for x in range(tw):
                k = hsh(x // 2, seed, 5) - 0.5
                a[:, x, :3] = np.clip(a[:, x, :3] * (1.0 + k * 0.34), 0, 1)
                if x % 4 == 1:
                    a[:, x, :3] = np.clip(a[:, x, :3] * 0.86, 0, 1)
            for y in range(th):
                for x in range(tw):
                    if hsh(x, y, seed, 8) > 0.93:
                        a[y, x, :3] = np.clip(a[y, x, :3] + (tone(color, 1.7) - a[y, x, :3]) * 0.5, 0, 1)
            return a
        return self.atlas.tile(("mass", L.hexs(rgb(color)), tw, th, seed), tw, th, paint)

    def t_spike(self, color, tw=16, th=24, seed=0, tip=None):
        """A spike of hair as a flat card: a triangle (alpha) with a dark root, a lit ridge down the middle and a pale tip."""
        def paint():
            a = np.zeros((th, tw, 4), np.float32)
            ramp = L._grad(th, [(0.0, tone(tip or color, 1.5)), (0.35, tone(color, 1.2)), (0.75, rgb(color)), (1.0, tone(color, 0.55))])
            for y in range(th):
                half = (tw / 2.0) * (0.12 + 0.88 * (y / (th - 1.0)) ** 0.85)
                for x in range(tw):
                    d = abs(x + 0.5 - tw / 2.0)
                    if d <= half:
                        c = ramp[y].copy()
                        k = 1.0 + 0.22 * (1.0 - d / max(half, 0.5)) - 0.18 * (d / max(half, 0.5)) ** 2 + (hsh(x, y, seed) - 0.5) * 0.08
                        a[y, x, :3] = np.clip(c * k, 0, 1)
                        a[y, x, 3] = 1.0
            return L._edge_outline(a, tone(color, 0.42), 0.5)
        return self.atlas.tile(("spike", L.hexs(rgb(color)), tw, th, seed, tip), tw, th, paint)

    def t_tip(self, kind, color, tw=16, th=16, edge=None):
        """The end of a tail as a flat card: spade, heart, arrow or trident (alpha shape, lit toward the top)."""
        def paint():
            ys, xs = np.mgrid[0:th, 0:tw].astype(np.float32)
            x = (xs + 0.5 - tw / 2.0) / (tw / 2.0)
            y = 1.0 - (ys + 0.5) / (th / 2.0)               # up = +
            if kind == "heart":
                xx, yy = x * 1.15, (y - 0.1) * 1.2
                m = (xx * xx + yy * yy - 1.0) ** 3 - xx * xx * yy ** 3 < 0.0
            elif kind == "spade":
                xx, yy = x * 1.2, -(y + 0.25) * 1.25
                m = ((xx * xx + yy * yy - 1.0) ** 3 - xx * xx * yy ** 3 < 0.0) & (y > -0.45) | ((np.abs(x) < 0.12) & (y < -0.2) & (y > -0.95)) | ((np.abs(x) < 0.55 * (1 + (y + 0.95))) & (y <= -0.78) & (y > -0.98))
            elif kind == "arrow":
                m = ((y > -1.0) & (y < 0.9) & (np.abs(x) < (y + 1.0) * 0.52) & (y < 0.9 - 0.0)) & (y > -0.2) | ((np.abs(x) < 0.12) & (y <= -0.2) & (y > -1.0))
                m = ((np.abs(x) < (0.95 - y) * 0.58) & (y > -0.2) & (y < 0.95)) | ((np.abs(x) < 0.13) & (y <= -0.2) & (y > -1.0))
            else:                                            # trident
                m = ((np.abs(x) < 0.12) & (y > -1.0) & (y < 0.95)) | ((np.abs(np.abs(x) - 0.58) < 0.12) & (y > -0.2) & (y < 0.75)) | ((np.abs(x) < 0.7) & (np.abs(x) > 0.0) & (y > -0.34) & (y < -0.2) & (np.abs(x) < 0.7))
                m = m | (np.abs(np.abs(x) - 0.58) < 0.14) & (y > 0.5) & (y < 0.9) & False
            a = np.zeros((th, tw, 4), np.float32)
            ramp = L._grad(th, [(0.0, tone(color, 1.35)), (0.5, rgb(color)), (1.0, tone(color, 0.6))])
            a[..., :3] = ramp[:, None, :]
            a[..., 3] = m.astype(np.float32)
            return L._edge_outline(a, tone(edge or color, 0.35), 0.6)
        return self.atlas.tile(("tip", kind, L.hexs(rgb(color)), tw, th), tw, th, paint)

    def t_hairup(self, color, tw=12, th=24, seed=0):
        """The UPPER section of a long strand (root dark -> colour), no ragged end: it ends under the lower section."""
        def paint():
            a = L.m_strand(color, tw, th * 2, seed, prongs=1, tip=0.0)
            return a[:th].copy()
        return self.atlas.tile(("hup", L.hexs(rgb(color)), tw, th, seed), tw, th, paint)

    def t_hairlow(self, color, tw=12, th=36, seed=0, prongs=3, tip=0.3):
        """The LOWER section of a long strand (colour -> lit, a ragged end)."""
        def paint():
            a = L.m_strand(color, tw, th * 2, seed, prongs=prongs, tip=tip * 0.5, dark=0.9)
            return a[th:].copy()
        return self.atlas.tile(("hlow", L.hexs(rgb(color)), tw, th, seed, prongs, tip), tw, th, paint)

    def finish(self, out):
        return self.atlas


# ================================================================================================ hair
HEAD = "head"


def hair_shell(b, color, g=0.3, top=True, sides=True, back=True, front_band=True, back_low=25.6, side_low=28.4, seed=1, layer="main", front_low=30.5, thick=1.0, side_front=-3.2):
    """The hair as a 3D mass round the skull (the hat layer of the skin is cleared, this is the hair): a top slab, a back plate, side plates and a brow band that leaves the face free.
    Everything stands `g` pixels off the head. `side_front` is how far forward the side plates reach (z; -4 = to the face), `front_low` how far the brow band comes down (y)."""
    geo = b.g(layer)
    mass = b.t_mass(color, seed=seed)
    top_y = 32.0 + g
    w = 8.0 + 2 * g
    if top:
        geo.cube(HEAD, (-w / 2, top_y, -w / 2), (w, thick, w), mass)
    if back:
        geo.cube(HEAD, (-w / 2, back_low, 4.0 + g), (w, top_y + thick - back_low, thick), mass)
    if sides:
        for s in (-1, 1):
            x0 = 4.0 + g if s > 0 else -(4.0 + g) - thick
            geo.cube(HEAD, (x0, side_low, side_front), (thick, top_y + thick - side_low, 4.0 + g - side_front + thick), mass)
    if front_band:
        geo.cube(HEAD, (-w / 2, front_low, -4.0 - g - thick), (w, top_y + thick - front_low, thick), mass)


def hair_tiers(b, color, n=4, y0=33.6, w0=9.2, shrink=1.3, h=1.3, d0=None, seed=2, layer="main", step_back=0.0):
    """A stack of tiers (the reference's purple pyramid): each tier smaller than the one below and a little higher; `step_back` shifts the stack toward the back."""
    geo = b.g(layer)
    d0 = d0 or w0
    for i in range(n):
        w = w0 - shrink * i
        d = d0 - shrink * i
        mass = b.t_mass(color, seed=seed + i)
        geo.cube(HEAD, (-w / 2, y0 + h * i, -d / 2 + step_back), (w, h, d), mass)


def spike(b, geo, anchor, base, ang_z, ang_x, length, w, mass, segs=3, depth=None, lean=0.0):
    """A spike of `segs` stacked boxes (each narrower), starting at `base` and leaning by ang_z (sideways, degrees) and ang_x (forward / back); the boxes are rotated about their own centres."""
    depth = depth or w
    dirv = np.array([math.sin(math.radians(ang_z)) * math.cos(math.radians(ang_x)), math.cos(math.radians(ang_z)) * math.cos(math.radians(ang_x)), -math.sin(math.radians(ang_x))])
    seg = length / segs
    p = np.array(base, float)
    for i in range(segs):
        k = 1.0 - i / float(segs) * 0.78
        ww, dd = w * k, depth * k
        c = p + dirv * (seg / 2.0)
        geo.cube(anchor, (c[0] - ww / 2, c[1] - seg / 2, c[2] - dd / 2), (ww, seg + 0.25, dd), mass, rot=(ang_x, 0.0, -ang_z), pivot=tuple(c))
        p = p + dirv * seg


def hair_spikes(b, color, n=9, y=33.2, spread=3.6, length=5.5, w=2.4, seed=3, layer="main", rows=2, back_lean=14.0, tall=1.0, fan=26.0, tip=None, depth_step=3.2, z0=-1.8):
    """A crown of spikes standing on the head's top slab: `rows` rows (front, back), fanned out and leaning back. Each spike is two crossed single-face cards (a triangle with a lit ridge): 2 quads."""
    geo = b.g(layer)
    for r in range(rows):
        cnt = n if r == 0 else max(3, n - 2)
        for i in range(cnt):
            t = (i + 0.5) / cnt * 2 - 1
            z = z0 + r * depth_step + 0.4 * hsh(i, r, seed)
            x = t * spread
            ang_z = t * fan + (hsh(i, r, seed, 1) - 0.5) * 8
            ang_x = back_lean + r * 7 + (hsh(i, r, seed, 2) - 0.5) * 8
            ln = length * tall * (0.78 + 0.5 * hsh(i, r, seed, 3)) * (1.0 + 0.25 * (1 - abs(t)))
            tile = b.t_spike(color, seed=seed + i + r * 7, tip=tip)
            ww = w * (1.0 - 0.12 * abs(t))
            rot = (ang_x, 0.0, -ang_z)
            piv = (x, y, z)
            geo.cube(HEAD, (x - ww / 2, y, z), (ww, ln, 0.0), tile, faces=("north",), rot=rot, pivot=piv)
            geo.cube(HEAD, (x, y, z - ww / 2), (0.0, ln, ww), tile, faces=("east",), rot=rot, pivot=piv)


def fringe(b, color, n=9, span=8.6, y_top=31.4, length=3.6, z=-4.75, seed=4, layer="fine", rot=10.0, w=1.9, side_bangs=True, part=0.0):
    """Front strands hanging over the forehead: `n` cards across `span`, their lengths and angles varying, the middle parted by `part`."""
    geo = b.g(layer)
    for i in range(n):
        t = (i + 0.5) / n * 2 - 1
        x = t * span / 2 + part * (0.5 if t > 0 else -0.5)
        ln = length * (0.78 + 0.45 * hsh(i, seed)) * (1.0 + 0.45 * abs(t) ** 2 * (1 if not side_bangs else 0.4))
        tile = b.t_strand(color, seed=seed + i, prongs=2, tip=0.3)
        zz = z - 0.05 * (i % 3) - (0.0 if i % 2 else 0.08)
        geo.card(HEAD, x, y_top, zz, w, ln, tile, rot=(0.0, 0.0, (hsh(i, seed, 2) - 0.5) * rot * 2 - t * 5.0), pivot=(x, y_top, zz))


def side_locks(b, color, x=4.85, y_top=29.5, z=-2.2, length=11.0, w=2.2, seed=5, layer="fine", count=2, z_step=1.5, flare=6.0):
    """Long locks framing the face: `count` stacked cards on each side of the head, hanging past the shoulders (the lower ones on the body so they do not swing with the head)."""
    geo = b.g(layer)
    for s in (-1, 1):
        for k in range(count):
            xx = s * (x + 0.45 * k)
            zz = z + k * z_step * (-0.4 if k else 1.0)
            ln = length * (1.0 - 0.14 * k)
            tile = b.t_strand(color, tw=12, th=48, seed=seed + k + (3 if s > 0 else 0), prongs=3, tip=0.25)
            bone = b.sway(layer, HEAD, "lock_%s%d" % ("l" if s > 0 else "r", k), (xx, y_top, zz), ax=2.2, az=1.6)
            geo.card(bone, xx, y_top, zz, w, ln, tile, rot=(0.0, 90.0, s * flare) if False else (0.0, 0.0, s * flare * 0.5), pivot=(xx, y_top, zz))
            # a second card turned a quarter so the lock has body from the front too
            geo.cube(bone, (xx, y_top - ln, zz - w / 2), (0.0, ln, w), tile, faces=("east",), rot=(0.0, 0.0, s * flare * 0.5), pivot=(xx, y_top, zz))


def long_hair(b, color, y_top=31.4, y_split=21.5, tail=18.0, tiers=3, count=7, width=9.6, seed=6, layer="fine", sway=2.4, taper=0.0, z0=4.85, light=1.0, prongs=3, tip=0.3):
    """Hair down the back in `tiers` layers (each a little further out and a little longer, like the reference's stacked strand rows). The strands are in two sections so the upper part follows the head
    and the lower part follows the body; both are cards. `tail` is the length below `y_split`."""
    geo = b.g(layer)
    for t in range(tiers):
        z = z0 + 0.5 * t
        n = count + (1 if t % 2 else 0)
        for i in range(n):
            u = (i + 0.5) / n * 2 - 1
            x = u * (width / 2 - 0.5 * (1 - abs(u)) * taper)
            ln_up = y_top - y_split + 1.0 - t * 0.0
            sec = hsh(i, t, seed)
            w = width / n * 1.25
            tail_ln = tail * (1.0 - 0.18 * t) * (0.82 + 0.3 * sec)
            up = b.t_hairup(color, seed=seed + (i + t * 3) % 4)
            low = b.t_hairlow(color, seed=seed + (i + t * 3) % 4, prongs=prongs, tip=tip)
            bu = b.sway(layer, HEAD, "hu_%d_%d" % (t, i % 3), (x, y_top, z), ax=sway * 0.4, az=sway * 0.3, phase=(t * 2.1 + (i % 3) * 1.9))
            geo.card(bu, x, y_top, z, w, ln_up, up, pivot=(x, y_top, z))
            bl = b.sway(layer, "body", "hl_%d_%d" % (t, i % 3), (x, y_split + 0.5, z + 0.1), ax=sway, az=sway * 0.7, phase=(t * 2.1 + (i % 3) * 1.9 + 0.6), walk=1.4, run=2.4)
            geo.card(bl, x, y_split + 1.0, z + 0.1, w, tail_ln + 1.0, low, pivot=(x, y_split + 0.5, z + 0.1))


def bob_hair(b, color, y_top=31.5, y_low=24.6, seed=7, layer="fine", count=6, z0=4.85, ring=True):
    """A bob: strands round the back and sides down to the jaw (head-anchored), the ends curling in."""
    geo = b.g(layer)
    ln = y_top - y_low
    for i in range(count):
        u = (i + 0.5) / count * 2 - 1
        x = u * 4.5
        tile = b.t_strand(color, seed=seed + i, prongs=2, tip=0.22)
        bone = b.sway(layer, HEAD, "bob_%d" % (i % 3), (x, y_top, z0), ax=1.5, az=1.2)
        geo.card(bone, x, y_top, z0 + 0.1 * (i % 2), 9.0 / count * 1.3, ln, tile, pivot=(x, y_top, z0))
    if ring:
        for s in (-1, 1):
            tile = b.t_strand(color, seed=seed + 20 + s, prongs=2, tip=0.22)
            for k in range(2):
                bone = b.sway(layer, HEAD, "bobs_%s%d" % ("l" if s > 0 else "r", k), (s * 4.9, y_top, -1.0 + k * 2.4), ax=1.2, az=1.0)
                geo.cube(bone, (s * 4.9, y_top - ln + 0.6, -1.0 + k * 2.4 - 1.2), (0.0, ln - 0.6, 2.4), tile, faces=("east",), pivot=(s * 4.9, y_top, -1.0 + k * 2.4))


# ================================================================================================ headwear and halos
def crown(b, gold="#e8b830", gem="#d81c40", spikes=5, y=32.35, tall=3.4, width=9.7, layer="main", seed=8, gems=True, band_h=1.3, point_w=1.6):
    """A gold crown: a band round the head with `spikes` points on the front half and gems at the base of each point (the gems are in the fx layer so they shine)."""
    geo = b.g(layer)
    metal = b.t_metal(gold, 16, 16, seed=seed)
    w = width
    d = width
    geo.cube(HEAD, (-w / 2, y, -d / 2), (w, band_h, 1.0), metal, faces=("north", "south", "east", "west", "up", "down"))
    geo.cube(HEAD, (-w / 2, y, d / 2 - 1.0), (w, band_h, 1.0), metal)
    for s in (-1, 1):
        geo.cube(HEAD, (s * (w / 2 - 0.5) - 0.5, y, -d / 2 + 1.0), (1.0, band_h, d - 2.0), metal)
    xs = [(-0.5 + i / (spikes - 1.0)) * (w - 2.2) for i in range(spikes)]
    for i, x in enumerate(xs):
        k = 1.0 - abs(x) / (w / 2) * 0.35
        hh = tall * k
        geo.cube(HEAD, (x - point_w / 2, y + band_h, -d / 2), (point_w, hh * 0.55, 1.0), metal)
        geo.cube(HEAD, (x - point_w * 0.32, y + band_h + hh * 0.55, -d / 2 + 0.1), (point_w * 0.64, hh * 0.45, 0.8), metal)
        if gems:
            b.g("fx").cube(HEAD, (x - 0.55, y + 0.15, -d / 2 - 0.35), (1.1, 1.0, 0.5), b.t_gem(gem), faces=("north", "up"))
    for s in (-1, 1):                                                  # back points
        for j in range(2):
            x = s * (1.4 + j * 2.0)
            geo.cube(HEAD, (x - 0.7, y + band_h, d / 2 - 1.0), (1.4, tall * 0.7 - 0.4 * j, 1.0), metal)


def circlet(b, gold="#e8b830", gem="#4cb8ff", y=31.0, layer="main", width=9.5, gem_size=1.5, gem_y=None):
    """A thin gold band across the brow with one gem in the middle."""
    geo = b.g(layer)
    metal = b.t_metal(gold, 16, 8)
    geo.cube(HEAD, (-width / 2, y, -4.8), (width, 0.9, 0.8), metal)
    for s in (-1, 1):
        geo.cube(HEAD, (s * width / 2 - (0.0 if s < 0 else 0.9), y, -4.8), (0.9, 0.9, 9.6), metal)
    geo.cube(HEAD, (-width / 2, y, 4.0), (width, 0.9, 0.9), metal)
    b.g("fx").cube(HEAD, (-gem_size / 2, (gem_y if gem_y is not None else y - 0.2), -5.2), (gem_size, gem_size * 1.1, 0.55), b.t_gem(gem), faces=("north", "up"))


def halo(b, y=36.6, r=7.2, color="#fff2a8", tilt=-6.0, spin=22.0, layer="fx", rings=2):
    """A halo: a flat quad with ring art, a little tilted, turning slowly (a bone with a spin and a gentle bob); drawn additive in the fx layer."""
    geo = b.g(layer)
    tile = b.t_ring(color, rings=rings)
    bone = b.sway(layer, HEAD, "halo", (0.0, y, 0.0), ax=0.0, az=0.0, spin=spin, bob=0.25, phase=0.0)
    geo.cube(bone, (-r, y, -r), (2 * r, 0.05, 2 * r), tile, faces=("up", "down"), rot=(tilt, 0.0, 0.0), pivot=(0, y, 0))


def horns(b, color="#2a2230", tip="#d8d0e0", base=(3.2, 31.6, -1.0), segs=5, length=3.6, w0=2.4, w1=0.7, curl=28.0, out=40.0, layer="main", mirrored=True, back=0.0, fwd=0.0, seed=9, up_bias=0.0):
    """A curved horn made of `segs` boxes that follow an arc: it starts going out at `out` degrees from vertical and bends by `curl` degrees per segment (positive = toward the middle)."""
    geo = b.g(layer)
    tile = b.t_horn(color, tip=tip, seed=seed)
    for s in ((-1, 1) if mirrored else (1,)):
        ang = out
        p = np.array([base[0] * s, base[1], base[2]], float)
        for i in range(segs):
            k = i / max(1.0, segs - 1.0)
            w = w0 + (w1 - w0) * k
            seg = length * (1.0 - 0.1 * k)
            a = math.radians(ang)
            d = np.array([math.sin(a) * s, math.cos(a), back / segs * 0.2 - fwd / segs * 0.2])
            c = p + d * seg / 2
            geo.cube(HEAD, (c[0] - w / 2, c[1] - seg / 2, c[2] - w / 2), (w, seg + 0.3, w), tile, rot=(0.0, 0.0, -ang * s), pivot=tuple(c))
            p = p + d * seg
            ang -= curl


def elf_ears(b, skin, layer="main", y=28.2, length=3.4, tilt=22.0):
    """Long pointed ears (elves): a two-box ear on each side of the head swept back and up."""
    geo = b.g(layer)
    tile = b.t_flat(skin, shade=0.12)
    for s in (-1, 1):
        for i, (w, ln) in enumerate(((1.0, length * 0.55), (0.6, length * 0.45))):
            x = s * (4.55 + 0.35 * i)
            y0 = y + (0.6 if i == 0 else length * 0.45)
            geo.cube(HEAD, (x - w / 2 - 0.0, y0, -0.4 + 0.5 * i), (w, ln, 1.2), tile, rot=(0.0, 0.0, -s * tilt * (1 + i * 0.5)), pivot=(x, y0, 0.0))


# ================================================================================================ body parts
def bracers(b, slim, skin_x=None, y_top=21.4, rings=3, gold="#e8b830", gem="#d86ad8", tassel="#8a5ad0", layer="main", tassels=True, tassel_len=6.5, gap=0.15, ring_h=1.25, grow=0.5, sides=("r", "l")):
    """Gold bracers on both forearms: `rings` stacked bands (the reference's layered armbands) and, hanging from the lowest, a tassel (cord, bead, tuft) that sways; the bead is a gem in the fx layer."""
    geo = b.g(layer)
    metal = b.t_metal(gold, 16, 16)
    aw = 3.0 if slim else 4.0
    for sd in sides:
        s = -1 if sd == "r" else 1
        cx = s * (4.0 + aw / 2.0) if not slim else s * (4.0 + aw / 2.0)
        anchor = "arm_" + sd
        for i in range(rings):
            y = y_top - i * (ring_h + gap)
            ww = aw + grow * 2
            geo.cube(anchor, (cx - ww / 2, y - ring_h, -2.0 - grow), (ww, ring_h, 4.0 + grow * 2), metal)
            if i % 2 == 0:
                geo.cube(anchor, (cx - ww / 2 - 0.12, y - ring_h * 0.3 - 0.1, -2.0 - grow - 0.12), (ww + 0.24, 0.3, 4.0 + grow * 2 + 0.24), b.t_metal(gold, 16, 4, band=False, rivets=False))
        if tassels:
            yb = y_top - rings * (ring_h + gap) + gap
            for k, off in enumerate((-1.2, 1.2)):
                x = cx + off * 0.9
                bone = b.sway("fine", anchor, "tas_%s%d" % (sd, k), (x, yb, 2.0 + grow), ax=5.0, az=4.0, walk=2.0, run=3.0, phase=1.3 * k + (0.7 if s > 0 else 0))
                f = b.g("fine")
                f.cube(bone, (x - 0.3, yb - tassel_len, 2.0 + grow - 0.3), (0.6, tassel_len, 0.6), b.t_flat(tassel, 4, 8), pivot=(x, yb, 2.0 + grow))
                b.g("fx").cube(anchor, (x - 0.65, yb - tassel_len - 0.9, 2.0 + grow - 0.65), (1.3, 1.3, 1.3), b.t_gem(gem), faces=("north", "south", "east", "west", "up"))


def leg_bands(b, slim=False, gold="#e8b830", y=3.2, rings=2, layer="main", sides=("r", "l")):
    geo = b.g(layer)
    metal = b.t_metal(gold, 16, 16)
    for sd in sides:
        s = -1 if sd == "r" else 1
        for i in range(rings):
            geo.cube("leg_" + sd, (s * 1.9 - 2.5, y + i * 1.5, -2.5), (5.0, 1.2, 5.0), metal)


def mantle(b, color="#f2f0f6", dark="#bcb8cc", y=21.6, thick=2.6, layer="main", seed=10, fur_hem=True, width=11.6, depth=6.6, drop=2.4, fringe_layer="fine", over_arms=True, collar_h=0.0):
    """A fur mantle round the shoulders: a thick collar ring (the solid mass) and, below its edge, fur cards with tufted hems (fine layer). Sits past the body's jacket layer."""
    geo = b.g(layer)
    fur = b.t_fur(color, seed=seed)
    top = y + thick
    geo.cube("body", (-width / 2, y, -depth / 2), (width, thick, depth), fur)
    geo.cube("body", (-width / 2 + 0.6, top - 0.2, -depth / 2 + 0.6), (width - 1.2, 0.9, depth - 1.2), b.t_fur(dark, seed=seed + 1))
    if collar_h:
        geo.cube("body", (-3.1, top, -2.8), (6.2, collar_h, 5.6), b.t_fur(color, seed=seed + 4))
    if fur_hem:
        f = b.g(fringe_layer)
        hem = b.t_fur(color, tw=32, th=16, seed=seed + 2, tuft=4)
        n = 6
        for i in range(n):
            u = (i + 0.5) / n * 2 - 1
            f.card("body", u * (width / 2 - 1.0), y + 0.3, -depth / 2 - 0.05, width / n * 1.05, drop + 0.6 * hsh(i, seed), hem)
            f.card("body", u * (width / 2 - 1.0), y + 0.3, depth / 2 + 0.05, width / n * 1.05, drop + 0.6 * hsh(i, seed, 1), hem)
        for s in (-1, 1):
            for k in range(3):
                zz = -2.4 + k * 2.4
                f.cube("body", (s * (width / 2 + 0.02), y + 0.3 - drop, zz - 1.2), (0.0, drop + 0.3, 2.4), hem, faces=("east",))


def cape(b, color="#1c1a30", lining="#6a2a40", length=24.0, y_top=23.4, width=9.0, tiers=3, layer="main", trim="#e8b830", emblem=None, emblem_color="#e8b830", seed=11, fur="", z=2.55, sway=2.6, hem="zigzag", fine_hem=True, flare=7.0):
    """A cape down the back in `tiers` overlapping panels, each a little longer and wider than the one above, swinging in the wind (a bone per tier); a gold trim and a fringed or zigzag hem."""
    geo = b.g(layer)
    seg = length / tiers
    for t in range(tiers):
        y1 = y_top - t * seg * 0.82
        w = width + t * 1.2
        zz = z + 0.45 * t
        last = t == tiers - 1
        tile = b.t_cloth(color, 32, 32, seed=seed + t, trim=trim if last else None, hem=hem if last else "straight", emblem=emblem if t == 0 else None, emblem_color=emblem_color, folds=4 + t)
        bone = b.sway(layer, "body", "cape_%d" % t, (0.0, y1, zz), ax=2.2 + t * 1.4, az=0.6, walk=2.0 + t, run=3.0 + t, phase=0.8 * t)
        geo.cube(bone, (-w / 2, y1 - seg * 1.1, zz), (w, seg * 1.1, 0.0), tile, faces=("north",), rot=(flare * t * 0.5, 0.0, 0.0), pivot=(0.0, y1, zz))
        geo.cube(bone, (-w / 2, y1 - seg * 1.1, zz + 0.05), (w, seg * 1.1, 0.0), b.t_cloth(lining, 32, 32, seed=seed + t + 9, hem=hem if last else "straight", folds=3 + t), faces=("south",), rot=(flare * t * 0.5, 0.0, 0.0), pivot=(0.0, y1, zz))
    if fur:
        geo.cube("body", (-width / 2 - 0.8, y_top - 1.2, z - 0.4), (width + 1.6, 1.6, 1.4), b.t_fur(fur, seed=seed + 12))


def skirt(b, color="#d8c8f0", edge="#e8b830", y_top=13.2, length=11.0, layers_=2, layer="fine", seed=12, radius=4.6, panels=8, sway=3.0, flare=14.0, hem_fringe=3):
    """A translucent mesh skirt in `layers_` rings of `panels` cards round the hips, each ring longer and wider (the reference's veil with a fringe), flaring out and swaying."""
    geo = b.g(layer)
    for ring in range(layers_):
        r = radius + 0.55 * ring
        ln = length * (1.0 - 0.2 * ring)
        tile = b.t_mesh(color, 16, 16, seed=seed + ring, edge=edge)
        for i in range(panels):
            a = i / panels * math.tau + ring * 0.2
            x, z = math.sin(a) * r, -math.cos(a) * r
            w = 2 * math.pi * r / panels * 1.15
            bone = b.sway(layer, "body", "sk_%d_%d" % (ring, i % 4), (x, y_top, z), ax=sway, az=sway * 0.7, walk=1.6, run=2.6, phase=0.9 * i + ring)
            geo.cube(bone, (x - w / 2, y_top - ln, z), (w, ln, 0.0), tile, faces=("north",), rot=(-flare, -math.degrees(a) + 180.0, 0.0) if False else (0.0, -math.degrees(a), 0.0), pivot=(x, y_top, z))


def wing(b, color="#241a34", vein="#6a4a8a", edge="#8a6ab0", span=26.0, drop=18.0, layer="main", y=22.0, side_offset=2.4, bone_color="#1c1426", seed=13, fold=22.0, pair=1, tilt=0.0, z=3.2, sway=4.0, fan=3):
    """A bat wing per side on the body: an arm of two bones angled out, `fan` membrane panels between the arm and the body (scalloped lower edges) and a finger bone along each panel's edge."""
    geo = b.g(layer)
    horn = b.t_horn(bone_color, tip=edge, seed=seed)
    for s in (-1, 1):
        nm = "wing%d_%s" % (pair, "l" if s > 0 else "r")
        bone = b.sway(layer, "body", nm, (s * side_offset, y, z), ax=1.0, az=sway, walk=3.0, run=5.0, cast=3.0, phase=0.0 if s > 0 else math.pi)
        a1 = 62.0 - fold
        # the arm: two boxes up and out
        geo.cube(bone, (s * side_offset - 0.6 + s * 0.0, y, z - 0.6), (1.2, span * 0.45, 1.2), horn, rot=(0.0, 0.0, -s * (a1 - 10)), pivot=(s * side_offset, y, z))
        for k in range(fan):
            ang = (a1 - 8) + k * (62.0 / max(1, fan)) + tilt
            ln = span * (0.9 - 0.1 * (k % 2)) + 0.0
            mem = b.t_membrane(color, vein, 32, 32, seed=seed + k, edge=edge)
            geo.cube(bone, (s * side_offset - (ln if s < 0 else 0.0), y - drop * 0.0, z + 0.05 * k), (ln, drop, 0.0), mem, faces=("north",), rot=(0.0, 0.0, -s * ang * 0.5), pivot=(s * side_offset, y + drop, z))
            geo.cube(bone, (s * side_offset - 0.4, y + drop * 0.25, z - 0.4 + 0.05 * k), (0.8, ln * 0.9, 0.8), horn, rot=(0.0, 0.0, -s * (ang + 20)), pivot=(s * side_offset, y + drop * 0.25, z))


def tail(b, color="#241a34", tip="#c8283a", segs=7, length=22.0, layer="main", y=13.0, z=2.4, sway=6.0, w0=2.0, w1=0.6, spade="spade", seed=14, curl=-18.0, curl_step=-6.0, tip_size=5.0):
    """A tail on the back of the body: `segs` boxes, thinner toward the end, each in a bone that swings a little more than the one before it, ending in a flat tip (`spade`: spade, heart, arrow,
    trident or None)."""
    geo = b.g(layer)
    tile = b.t_horn(color, tip=tip, seed=seed, ridges=7)
    seg = length / segs
    yy, zz = y, z
    ang = 0.0
    bone = None
    for i in range(segs):
        k = i / max(1.0, segs - 1.0)
        w = w0 + (w1 - w0) * k
        bone = b.sway(layer, "body", "tail%d" % i, (0.0, yy, zz), ax=sway * (0.4 + 0.2 * i), az=sway * (0.5 + 0.3 * i), walk=1.0, run=1.5, phase=0.5 * i)
        ang += curl if i == 0 else curl_step
        geo.cube(bone, (-w / 2, yy - seg, zz), (w, seg + 0.2, w), tile, rot=(ang, 0.0, 0.0), pivot=(0.0, yy, zz))
        yy -= seg * 0.9 * abs(math.cos(math.radians(ang)))
        zz += seg * 0.9 * abs(math.sin(math.radians(ang)))
    if spade and bone is not None:
        t = b.t_tip(spade, tip)
        ts = tip_size
        rot = (ang, 0.0, 0.0)
        geo.cube(bone, (-ts / 2, yy - ts + 0.6, zz), (ts, ts, 0.0), t, faces=("north",), rot=rot, pivot=(0.0, yy, zz))
        geo.cube(bone, (0.0, yy - ts + 0.6, zz - ts / 2), (0.0, ts, ts), t, faces=("east",), rot=rot, pivot=(0.0, yy, zz))


def collar(b, color="#1c1a30", gold="#e8b830", layer="main", high=3.2, width=9.4, seed=15):
    """A high standing collar round the neck (a box ring with a gold edge)."""
    geo = b.g(layer)
    cloth = b.t_cloth(color, 16, 16, seed=seed)
    geo.cube("body", (-width / 2 + 1.4, 24.0, -3.0), (width - 2.8, high, 1.0), cloth)
    geo.cube("body", (-width / 2 + 1.4, 24.0, 2.0), (width - 2.8, high, 1.0), cloth)
    for s in (-1, 1):
        geo.cube("body", (s * (width / 2 - 1.4) - (0.0 if s > 0 else 1.0), 24.0, -2.0), (1.0, high, 4.0), cloth)
    geo.cube("body", (-width / 2 + 1.4, 24.0 + high - 0.5, -3.0), (width - 2.8, 0.5, 6.0), b.t_metal(gold, 16, 4, band=False, rivets=False), faces=("up",))
