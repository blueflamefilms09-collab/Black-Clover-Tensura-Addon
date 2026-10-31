"""HD skins, part 3: the bridge from a hand-painted 64x64 skin to an HD one (3.38.0).

`from64(cv, skin64, glow64, materials, ...)` takes the 64x64 pixel-art result of tools/tier_bosses/remake/<id>.py as the colour and layout blueprint and fills an HD `Canvas` with it, island
by island (so nothing crosses a seam):

 (a) region-aware smooth upscaling: every distinct colour of an island is a class; the one-hot class maps are interpolated with a cubic filter and the strongest class wins per texel. Straight
     edges stay straight, stair-steps become smooth curves, a one-pixel line stays about one pixel wide, a checker becomes a diamond weave. No colour that was not in the blueprint appears.
 (b) a cel plus gradient pass: tone steps of one material (a ramp's neighbouring shades) are blended with an edge-preserving filter, so bands become soft cloth gradients, while the edges
     between different materials and different colours stay crisp; then thin outlines in a darker, cooler hue are drawn on the darker side of every material edge.
 (c) detail passes per material: cloth (folds, a faint weave, shadow under hems), metal (a domed bevel with a specular streak), skin (a gentle roundness), hair (strand streaks), and a
     cross-layer shadow (the second layer's pieces darken the body under their edges so trims stand off the cloth).

Materials are named by the caller: `materials={"metal": [family("#e0b030")], "skin": [family("#f4cdae")], "gem": [near("#c81c40", 0.2)]}`; everything unmatched is cloth.
"""
import colorsys
import math

import numpy as np

import hd_core as C
from hd_core import Canvas, blur, col, shift, smoothstep, tone, lum, to_array, hsh
import hd_detail as D

# ================================================================================================ materials
def family(hex_or_rgb, hue_tol=16.0, sat_tol=0.35, min_sat=0.12):
    """A matcher for every tone of one colour (a ramp: the same hue, any brightness). A near-grey base matches any near-grey."""
    c = col(hex_or_rgb)
    h0, s0, v0 = colorsys.rgb_to_hsv(*[float(x) for x in c])

    def match(rgb):
        h, s, v = colorsys.rgb_to_hsv(*[float(x) for x in rgb])
        if s0 < min_sat:
            return s < max(min_sat * 1.5, 0.18)
        if s < min_sat * 0.7:
            return False
        dh = abs(h - h0)
        dh = min(dh, 1.0 - dh) * 360.0
        return dh <= hue_tol and abs(s - s0) <= sat_tol
    return match


def near(hex_or_rgb, dist=0.12):
    """A matcher for colours within `dist` (RGB distance, 0..1) of one colour."""
    c = col(hex_or_rgb)
    return lambda rgb: float(np.sqrt(((np.asarray(rgb[:3], np.float32) - c) ** 2).sum())) <= dist


def exact(*hexes):
    """A matcher for these exact colours."""
    cs = [tuple(rgb8_(h)) for h in hexes]
    return lambda rgb: tuple(int(round(float(x) * 255)) for x in rgb[:3]) in cs


def rgb8_(c):
    return C.rgb8(c)


class Materials:
    """name -> list of matchers (callables on an (r, g, b) float triple, or '#rrggbb' = family). The order of the dict is the order of the tests; no match = `default`."""

    def __init__(self, spec=None, default="cloth"):
        self.default = default
        self.rules = []
        for name, ms in (spec or {}).items():
            ms = ms if isinstance(ms, list) else [ms]
            self.rules.append((name, [family(m) if isinstance(m, (str, tuple)) else m for m in ms]))
        self.names = [r[0] for r in self.rules] + [default]

    def of(self, rgb):
        for name, fs in self.rules:
            for m in fs:
                if m(rgb):
                    return name
        return self.default


# ================================================================================================ (a) smooth upscaling
_CR_CACHE = {}


def _cr_matrix(n_src, f, pad=2):
    """(n_src * f, n_src + 2 * pad): Catmull-Rom samples at the texel centres of an f-times finer grid, from the edge-padded source."""
    key = (n_src, f, pad)
    m = _CR_CACHE.get(key)
    if m is None:
        n_out = n_src * f
        m = np.zeros((n_out, n_src + 2 * pad), np.float32)
        for i in range(n_out):
            s = (i + 0.5) / f - 0.5 + pad
            i0 = int(math.floor(s))
            t = s - i0
            w = (-0.5 * t ** 3 + t ** 2 - 0.5 * t, 1.5 * t ** 3 - 2.5 * t ** 2 + 1.0, -1.5 * t ** 3 + 2.0 * t ** 2 + 0.5 * t, 0.5 * t ** 3 - 0.5 * t ** 2)
            for k in range(4):
                m[i, min(max(i0 - 1 + k, 0), n_src + 2 * pad - 1)] += w[k]
        _CR_CACHE[key] = m
    return m


def upscale_labels(region, f):
    """region: uint8 (h, w, 4). -> (labels (h*f, w*f) int, palette (n, 4) float 0..1). Transparent texels are one class with alpha 0."""
    h, w = region.shape[:2]
    reg = region.copy()
    reg[reg[..., 3] < 8] = 0
    keys = (reg[..., 0].astype(np.uint32) << 24) | (reg[..., 1].astype(np.uint32) << 16) | (reg[..., 2].astype(np.uint32) << 8) | reg[..., 3].astype(np.uint32)
    uk, inv = np.unique(keys.reshape(-1), return_inverse=True)
    inv = inv.reshape(h, w)
    n = len(uk)
    pal = np.zeros((n, 4), np.float32)
    pal[:, 0] = ((uk >> 24) & 255) / 255.0
    pal[:, 1] = ((uk >> 16) & 255) / 255.0
    pal[:, 2] = ((uk >> 8) & 255) / 255.0
    pal[:, 3] = (uk & 255) / 255.0
    if n == 1:
        return np.zeros((h * f, w * f), np.int32), pal
    onehot = np.zeros((h, w, n), np.float32)
    onehot[np.arange(h)[:, None], np.arange(w)[None, :], inv] = 1.0
    pad = np.pad(onehot, ((2, 2), (2, 2), (0, 0)), mode="edge")
    rv, rh = _cr_matrix(h, f), _cr_matrix(w, f)
    up = np.einsum("ij,jkc->ikc", rv, pad)
    up = np.einsum("ikc,lk->ilc", up, rh)
    return up.argmax(-1).astype(np.int32), pal


def upscale_mask(m64, f):
    """A binary 64-grid mask (h, w) -> a smooth (h*f, w*f) float mask in 0..1 (cubic, then a threshold ramp)."""
    h, w = m64.shape
    pad = np.pad(m64.astype(np.float32), 2, mode="edge")
    up = _cr_matrix(h, f) @ pad @ _cr_matrix(w, f).T
    return np.clip((up - 0.5) * 3.0 + 0.5, 0, 1)


# ================================================================================================ (b) gradient and outline
def bilateral(rgb, matid, alpha, radius, sigma_s, sigma_c):
    """Blends every texel with its neighbours of the SAME material, weighted by distance and colour likeness, so tone steps become gradients and different colours stay crisp."""
    acc = np.zeros_like(rgb)
    wsum = np.zeros(rgb.shape[:2], np.float32)
    r = int(radius)
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            d2 = dx * dx + dy * dy
            if d2 > radius * radius:
                continue
            ws = math.exp(-d2 / (2.0 * sigma_s * sigma_s))
            nrgb = shift(rgb, dy, dx)
            diff = ((nrgb - rgb) ** 2).sum(-1)
            wgt = ws * np.exp(-diff / (2.0 * sigma_c * sigma_c)) * (shift(matid, dy, dx) == matid) * shift(alpha, dy, dx)
            acc += nrgb * wgt[..., None]
            wsum += wgt
    out = np.where((wsum > 1e-6)[..., None], acc / np.maximum(wsum, 1e-6)[..., None], rgb)
    return out.astype(np.float32)


def class_groups(pal, mat_ids):
    """Groups the colour classes of an island into tone families: the same material and the same hue (a ramp's shades), near-greys together, darks together. -> int array per class."""
    n = len(pal)
    hsv = [colorsys.rgb_to_hsv(float(p[0]), float(p[1]), float(p[2])) for p in pal]
    parent = list(range(n))

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i

    def kind(h):
        return "dark" if h[2] < 0.22 else ("grey" if h[1] < 0.14 else "col")

    for i in range(n):
        if pal[i][3] < 0.5:
            continue
        for j in range(i):
            if pal[j][3] < 0.5 or mat_ids[i] != mat_ids[j]:
                continue
            ki, kj = kind(hsv[i]), kind(hsv[j])
            same = False
            if ki == kj == "col":
                dh = abs(hsv[i][0] - hsv[j][0])
                dh = min(dh, 1.0 - dh) * 360.0
                same = dh < 26.0 and abs(hsv[i][1] - hsv[j][1]) < 0.34
            elif ki == kj:
                same = True
            elif "grey" in (ki, kj) and "col" in (ki, kj):
                c = hsv[i] if ki == "col" else hsv[j]
                same = c[1] < 0.24                                                   # a pale cream with a grey shade is one cloth
            if same:
                parent[find(i)] = find(j)
    return np.array([find(i) for i in range(n)], np.int32)


def group_smooth(rgb, gid, alpha, sigma):
    """Smooths the colour inside every tone family with a normalised Gaussian (colours of other families and of nothing never mix in), so a garment's pixel mottling becomes a soft shade."""
    out = rgb.copy()
    for g in np.unique(gid):
        m = ((gid == g) & (alpha > 0.5)).astype(np.float32)
        if m.sum() < 1:
            continue
        den = blur(m, sigma)
        num = blur(rgb * m[..., None], sigma)
        sm = num / np.maximum(den, 1e-4)[..., None]
        out = np.where((m > 0)[..., None], sm, out)
    return out


def outline_mask(labels, rgb, alpha, thick, tol=0.30, vs_clear=True):
    """The texels along the DARKER side of every edge between two clearly different colours (and along the rim of a second-layer piece against nothing), `thick` texels deep."""
    L = lum(rgb)
    o = np.zeros(labels.shape, bool)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        nl = shift(labels, dy, dx)
        na = shift(alpha, dy, dx)
        nrgb = shift(rgb, dy, dx)
        dist = np.sqrt(((nrgb - rgb) ** 2).sum(-1))
        darker = (shift(L, dy, dx) > L + 0.02) & (dist > tol) & (nl != labels) & (na > 0.5) & (alpha > 0.5)
        rim = (na < 0.5) & (alpha > 0.5) & vs_clear
        o |= darker | rim
    for _ in range(max(0, thick - 1)):
        grow = np.zeros_like(o)
        for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            grow |= shift(o, dy, dx) & (shift(labels, dy, dx) == labels)
        o |= grow & (alpha > 0.5)
    return o


# ================================================================================================ (c) material passes
def _xs(face, n, lo=0.18, hi=0.82):
    return [face.w * (lo + (hi - lo) * (i + 0.15 + 0.7 * hsh(i, face.x0, face.y0)) / n) for i in range(n)]


def pass_cloth(face, mask, ctx):
    if face.name in ("top", "bottom"):
        return
    n = 2 if face.w <= 4 else 3
    D.folds(face, mask, _xs(face, n), depth=ctx.get("fold", 0.055), width=0.26, y0=0.5, y1=face.h)
    D.weave(face, mask, strength=ctx.get("weave", 0.025))


def pass_metal(face, mask, ctx):
    D.bevel(face, mask, depth=0.34, gain=1.25, spec=0.55, spec_pow=12)


def pass_skin(face, mask, ctx):
    if face.name in ("top", "bottom"):
        return
    D.bevel(face, mask, depth=0.55, gain=0.25)


def pass_hair(face, mask, ctx):
    if face.name in ("bottom",):
        return
    D.folds(face, mask, [face.w * (i + 0.5) / max(2, int(face.w * 1.3)) for i in range(max(2, int(face.w * 1.3)))], depth=0.07, width=0.10, sway=0.12, y0=0.0, fade_top=0.2)


def pass_gem(face, mask, ctx):
    D.bevel(face, mask, depth=0.30, gain=1.0, spec=0.7, spec_pow=10)


PASSES = {"cloth": pass_cloth, "metal": pass_metal, "skin": pass_skin, "hair": pass_hair, "gem": pass_gem}
# the outline strength per material (0 = none): the darker side of an edge gets a line of this strength
OUTLINE = {"cloth": 0.60, "metal": 0.75, "skin": 0.45, "hair": 0.50, "gem": 0.70}


def edge_roundness(face, strength=0.06, width=0.9):
    """The sides of a limb or a torso curve away from the light: the border column on the shaded side darkens, the lit side stays."""
    if face.name in ("top", "bottom"):
        return
    X = face.X
    left = smoothstep(0.0, width, X)
    right = smoothstep(0.0, width, face.w - X)
    lit_left = face.light[0] < 0
    k = 1.0 - strength * ((1.0 - right) * (1.0 if lit_left else 0.6) + (1.0 - left) * (0.6 if lit_left else 1.0))
    face.shade(face.rgba[..., 3], k)


# ================================================================================================ the bridge
def _region64(img64, face):
    f = face.f
    x0, y0 = face.x0 // f, face.y0 // f
    return img64[y0:y0 + face.h, x0:x0 + face.w]


def bridge_face(face, region, mats, soft=0.6, outline=0.55, detail=1.0, ctx=None):
    """Fills one island from its 64-grid region. Returns {material: mask} for the caller's own passes."""
    ctx = ctx or {}
    f = face.f
    labels, pal = upscale_labels(region, f)
    rgba = pal[labels]
    rgb, alpha = rgba[..., :3].copy(), rgba[..., 3].copy()
    mid_of_class = np.array([mats.names.index(mats.of(p[:3])) if p[3] > 0.5 else len(mats.names) for p in pal], np.int32)
    matid = mid_of_class[labels]
    gid_of_class = class_groups(pal, mid_of_class)
    gid = gid_of_class[labels]
    L0 = gid
    if soft > 0 and f > 1:
        sm = bilateral(rgb, matid, alpha, radius=max(2, int(round(f * 1.25))), sigma_s=f * 0.55, sigma_c=0.25)
        sm = group_smooth(sm, gid, alpha, f * ctx.get("group_sigma", 0.55))
        rgb = rgb + (sm - rgb) * soft
    masks = {}
    for i, name in enumerate(mats.names):
        masks[name] = (matid == i).astype(np.float32) * alpha
    face.rgba[..., :3] = rgb
    face.rgba[..., 3] = alpha
    if detail > 0:
        for name, m in masks.items():
            if m.any() and name in PASSES:
                PASSES[name](face, np.minimum(1.0, blur(m, 0.5) * 1.5) * (m > 0), ctx)
    if outline > 0 and f > 1:
        rgb2 = face.rgba[..., :3]
        thick = max(1, int(round(f * 0.45)))
        om = outline_mask(L0, rgb, alpha, thick)
        if om.any():
            strength = np.zeros(om.shape, np.float32)
            for i, name in enumerate(mats.names):
                strength = np.where(matid == i, OUTLINE.get(name, 0.5), strength)
            line = om.astype(np.float32) * strength * outline
            line = blur(line, 0.45) * (alpha > 0.5)
            face.shade(line, 0.42)
    if detail > 0:
        edge_roundness(face, 0.05 * detail)
    return masks


def cross_layer_shadow(cv, strength=0.22, off=0.28, soft=0.55):
    """Where a second-layer piece stands off the body, the base layer under its edge gets a soft shadow cast away from the light: trims read as raised."""
    for part in C.PARTS:
        for name in ("front", "back", "west", "east"):
            ov = cv.face(part, name, True)
            base = cv.face(part, name, False)
            a = ov.rgba[..., 3]
            if not (a > 0.5).any():
                continue
            lx, ly = base.light
            f = base.f
            dy, dx = int(round(-ly * off * f)), int(round(-lx * off * f))
            cast = shift(a, dy, dx)
            cast = blur(cast, soft * f * 0.5)
            base.shade(np.clip(cast, 0, 1) * (1.0 - a), 1.0 - strength)


def from64(cv, skin64, glow64=None, materials=None, soft=0.6, outline=0.55, detail=1.0, skip=None, ctx=None, shadow=0.22):
    """Fills the canvas from a 64x64 blueprint (PIL image or array). `skip(part, face, overlay) -> bool` leaves an island untouched (paint it yourself). Returns {(part, face, overlay): masks}."""
    img = (to_array(skin64) * 255.0 + 0.5).astype(np.uint8)
    mats = materials if isinstance(materials, Materials) else Materials(materials)
    out = {}
    for fc in cv.faces():
        if skip and skip(fc.part, fc.name, fc.overlay):
            continue
        reg = _region64(img, fc)
        if reg.shape[0] != fc.h or reg.shape[1] != fc.w:
            continue
        out[(fc.part, fc.name, fc.overlay)] = bridge_face(fc, reg, mats, soft, outline, detail, ctx)
    if shadow > 0 and detail > 0:
        cross_layer_shadow(cv, shadow)
    if glow64 is not None:
        bridge_glow(cv, glow64, img)
    return out


def bridge_glow(cv, glow64, skin_u8=None):
    """The glow map: the blueprint's lit texels, upscaled with a smooth edge (cubic mask), in the blueprint's colour, a little bloom."""
    g = (to_array(glow64) * 255.0 + 0.5).astype(np.uint8)
    f = cv.f
    for fc in cv.faces():
        reg = _region64(g, fc)
        if reg.shape[0] != fc.h or reg.shape[1] != fc.w or not (reg[..., 3] > 8).any():
            continue
        m = upscale_mask(reg[..., 3] > 8, f)
        # colour: nearest source texel, then a small blur inside the lit area so tone steps of the blueprint do not stair-step
        ys, xs = np.mgrid[0:fc.H, 0:fc.W]
        cs = reg[(ys // f), (xs // f)][..., :3].astype(np.float32) / 255.0
        cs = blur(cs, f * 0.35)
        fc.glow[..., :3] = cs
        fc.glow[..., 3] = np.clip(m, 0, 1)
        if not fc.overlay:
            fc.glow[..., 3] *= (fc.rgba[..., 3] > 0.5)


def hd_paint(cid, blueprint64, glow64=None, materials=None, size=256, slim=False, **opts):
    """cid, a 64x64 blueprint (PIL image) and optionally its glow map -> (skin RGBA at `size`, glow RGBA or None): the generic bridge with no character-specific detail. A character module
    (tools/tier_bosses/hd/<id>.py) normally builds the Canvas itself, calls `from64` and then adds its own details."""
    cv = Canvas(size, slim)
    from64(cv, blueprint64, glow64, materials, **opts)
    return cv.finish()
