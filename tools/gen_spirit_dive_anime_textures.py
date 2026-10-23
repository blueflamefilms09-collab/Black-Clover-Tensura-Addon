"""Spirit Dive, ANIME look: writes the textures of the look to src/main/resources/assets/nusmp/textures/aura/ (all deterministic, redo with `python3 -B tools/gen_spirit_dive_anime_textures.py`).

Everything is cel-shaded: flat bands (a dark outline where the art has one, a base, a mid band, a bright core), no noise gradients:

    sda_swirl_<el>.png        256x256 RGB, seamless in both axes: the energy that flows along the limbs (additive; black = no light). Laid over the 64x64 skin UV layout, so each limb shows a
                              32 x 80 texel crop of it at four times the skin's resolution.
    sda_plume_<el>.png        128x256 RGBA: the translucent body of a plume sheet (u across: leading edge .. trailing edge, v along: root .. tip), outline and bands baked in.
    sda_plume_glow_<el>.png   the additive glow of the same sheet (bright leading edge and streak lines; no colour under alpha 0).
    sda_lock_<el>.png / sda_lock_glow_<el>.png   the same for a lock of hair (64x128).
    sda_halo_<el>.png         256x256 additive: the thin spiky crown of light (wind: sharp spikes, fire: flame tongues, water: scalloped double ring, earth: crystal shards).
    sda_hair_<el>.png         256x256 additive: the glowing hair cap on the hat layer of the head (the 64x64 skin layout at x4: hair on the top, back, sides and a fringe).
    sda_rim.png, sda_soft.png 64x64: the solid rim of the shell hull and its soft outer hull.
    sda_eye.png               64x32: the glowing eye.
    sda_burst.png             128x128: the star-burst flash of the entry.

SafeAdditive: every additive texture has RGB = 0 wherever alpha = 0 (checked by SpiritDiveAnimeAssetTest).
"""
import math
import os
import sys

sys.dont_write_bytecode = True

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "aura")
ELS = ["wind", "water", "fire", "earth"]


def rgb(h):
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], np.float32)


# colour bands of each element: outline, shade, base, mid, light (cel steps from dark to bright)
BANDS = {
    "wind": [0x4FC796, 0x8FE6BE, 0xC2F7DC, 0xE4FFF1, 0xFFFFFF],
    "fire": [0x6A1008, 0xC02410, 0xFF6A18, 0xFFA828, 0xFFE46A],
    "water": [0x1B4FA8, 0x2F7BE0, 0x6DB8FF, 0xB4E2FF, 0xFFFFFF],
    "earth": [0x3A2C18, 0x6A6252, 0x9A927E, 0xD4BC7A, 0xFFD070],
}
GLOW = {"wind": 0x98FFD0, "fire": 0xFF8A2A, "water": 0xA8E4FF, "earth": 0xFFB040}
ALPHA = {"wind": 0.74, "fire": 0.92, "water": 0.76, "earth": 0.95}


def save(arr, name, mode):
    os.makedirs(OUT, exist_ok=True)
    a = np.clip(arr, 0, 255).astype(np.uint8)
    Image.fromarray(a, mode).save(os.path.join(OUT, name + ".png"), optimize=True)


def clean_additive(rgba):
    """RGB := 0 where alpha is 0 (SafeAdditive), and premultiply nothing else."""
    a = rgba[..., 3:4] > 0.5
    rgba[..., :3] = np.where(a, rgba[..., :3], 0.0)
    return rgba


def tile_dist(a, b, n):
    d = np.abs(a - b)
    return np.minimum(d, n - d)


# ------------------------------------------------------------------ the swirl textures (seamless)
def swirl(el, rnd):
    N = 256
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    img = np.zeros((N, N, 3), np.float32)
    bands = [rgb(h) for h in BANDS[el]]
    img[:] = bands[0] * 0.10                                                          # a faint wash of the colour everywhere: the body is bathed in it
    if el == "wind":
        for i in range(30):                                                           # long streaks that sway: flowing air
            x0, ln, w = rnd.uniform(0, N), rnd.uniform(70, 210), rnd.uniform(2.5, 7.5)
            y0, k, ph, amp = rnd.uniform(0, N), rnd.choice([1, 2]), rnd.uniform(0, 6.28), rnd.uniform(3, 9)
            t = ((yy - y0) % N) / ln
            on = t <= 1.0
            xc = x0 + amp * np.sin(2 * math.pi * k * yy / N + ph)
            hw = w * np.maximum(np.sin(np.pi * np.clip(t, 0, 1)), 0.0) ** 0.7
            d = tile_dist(xx, xc, N) / np.maximum(hw, 1e-3)
            for lvl, thr in ((1, 1.0), (2, 0.66), (3, 0.38), (4, 0.18)):
                m = on & (d < thr) & (hw > 0.8)
                img[m] = np.maximum(img[m], bands[lvl] * (0.55 if lvl == 1 else 1.0))
    elif el == "fire":
        for i in range(16):                                                           # rising flame tongues, cel-banded like the flame arm: dark red outline, red, orange, yellow core
            x0, hgt, w = rnd.uniform(0, N), rnd.uniform(90, 170), rnd.uniform(14, 28)
            y0 = rnd.uniform(0, N)
            t = ((y0 - yy) % N) / hgt                                                 # 0 at the base, 1 at the tip (flames rise: up = smaller y)
            on = t <= 1.0
            wob = 1.0 + 0.25 * np.sin(2 * math.pi * 3 * t + i)
            hw = w * np.maximum(np.sin(np.pi * np.clip(t, 0, 1) ** 0.55), 0.0) ** 1.1 * wob
            xc = x0 + 6.0 * np.sin(2 * math.pi * 2 * t + i * 1.7) * t
            d = tile_dist(xx, xc, N) / np.maximum(hw, 1e-3)
            for lvl, thr in ((0, 1.0), (1, 0.88), (2, 0.66), (3, 0.42), (4, 0.2)):
                m = on & (d < thr) & (hw > 1.0)
                img[m] = bands[lvl] * (1.0 if lvl else 0.8)
    elif el == "water":
        for i in range(24):                                                           # sinuous currents running along the limb
            x0, w = rnd.uniform(0, N), rnd.uniform(3, 9)
            k, ph, amp = rnd.choice([1, 2, 3]), rnd.uniform(0, 6.28), rnd.uniform(4, 12)
            xc = x0 + amp * np.sin(2 * math.pi * k * yy / N + ph)
            d = tile_dist(xx, xc, N)
            fade = 0.5 + 0.5 * np.sin(2 * math.pi * (yy / N + i * 0.13))
            hw = w * (0.35 + 0.65 * fade)
            for lvl, thr in ((1, 1.0), (2, 0.66), (3, 0.38), (4, 0.16)):
                m = d < hw * thr
                img[m] = np.maximum(img[m], bands[lvl])
        for i in range(34):                                                           # bubbles
            cx, cy, r = rnd.uniform(0, N), rnd.uniform(0, N), rnd.uniform(2.5, 6.0)
            d = np.hypot(tile_dist(xx, cx, N), tile_dist(yy, cy, N))
            ring = (d < r) & (d > r - 1.4)
            img[ring] = np.maximum(img[ring], bands[4])
    else:
        pts = np.array([(rnd.uniform(0, N), rnd.uniform(0, N)) for _ in range(22)], np.float32)      # cracked rock: the glowing seams between plates
        d1 = np.full((N, N), 1e9, np.float32)
        d2 = np.full((N, N), 1e9, np.float32)
        for (px, py) in pts:
            d = np.hypot(tile_dist(xx, px, N), tile_dist(yy, py, N))
            n1 = np.minimum(d1, d)
            d2 = np.minimum(d2, np.maximum(d1, d))
            d1 = n1
        gap = d2 - d1
        for lvl, thr in ((1, 7.0), (2, 4.5), (3, 2.6), (4, 1.2)):
            m = gap < thr
            img[m] = bands[lvl] * (0.9 if lvl < 4 else 1.0)
        plate = np.hypot(tile_dist(xx, pts[3][0], N), tile_dist(yy, pts[3][1], N)) < 40
        img[plate & (gap >= 7.0)] = bands[1] * 0.35
    return img


# ------------------------------------------------------------------ plumes and locks
def plume(el, w, h, rnd, glow, lock):
    """One strip texture: u across (0 leading edge .. 1 trailing edge), v along (0 root .. 1 tip), rows top to bottom = root to tip."""
    bands = [rgb(b) for b in BANDS[el]]
    u = (np.arange(w, dtype=np.float32) + 0.5) / w
    v = (np.arange(h, dtype=np.float32) + 0.5) / h
    U, V = np.meshgrid(u, v)
    # across the width: outline | bright leading line | mid | base | shade | outline
    o = 0.045 if not lock else 0.07
    col = np.zeros((h, w, 3), np.float32)
    lvl = np.full((h, w), 2.0, np.float32)
    lvl[U < 0.55] = 3.0
    lvl[U < 0.30] = 3.0
    lvl[U > 0.78] = 1.0
    lead = (U >= o) & (U < o + (0.11 if not lock else 0.18))
    lvl[lead] = 4.0
    if el == "earth":                                                              # faceted: the two halves of the shard are two flat tones
        lvl = np.where(U < 0.5, 3.0, 1.5)
        lvl[lead] = 4.0
    # streak lines along the strip (curved, fading toward the tip)
    nstreak = 3 if lock else 6
    for i in range(nstreak):
        u0 = rnd.uniform(0.2, 0.9)
        bend = rnd.uniform(-0.08, 0.08)
        uc = u0 + bend * np.sin(V * math.pi * rnd.choice([1.0, 1.5]) + i)
        line = (np.abs(U - uc) < (0.012 if not lock else 0.02)) & (V > rnd.uniform(0.05, 0.35)) & (V < rnd.uniform(0.6, 0.97))
        lvl = np.where(line, np.minimum(lvl + 1.0, 4.0), lvl)
    # the band colours (interpolate between the cel steps, but each pixel takes a single flat step)
    steps = np.rint(lvl).astype(int)
    for s in range(5):
        col[steps == s] = bands[s]
    outline = (U < o) | (U > 1.0 - o)
    col[outline] = bands[0]
    # alpha: the body is translucent, the outline solid, a soft fade at the root and a cut at the tip
    base_a = ALPHA[el]
    a = np.full((h, w), base_a, np.float32)
    a[outline] = 1.0
    a[lead] = np.minimum(1.0, base_a + 0.18)
    a *= np.clip(V / 0.06, 0.0, 1.0) ** 0.8                                         # transparent at the root: it grows out of the body
    a *= np.clip((1.0 - V) / 0.04, 0.0, 1.0)
    edge = np.minimum(U, 1.0 - U)
    a *= np.clip(edge / 0.012, 0.0, 1.0)                                            # one texel of anti-aliasing at the border
    if el in ("fire", "water"):                                                     # a wavy, torn border
        wob = 0.5 + 0.5 * np.sin(V * (38.0 if el == "fire" else 16.0) + U * 9.0 + (1.7 if el == "fire" else 0.0))
        cut = 0.012 + 0.05 * wob * V
        a *= np.clip((edge - cut) / 0.015, 0.0, 1.0)
    if not glow:
        out = np.dstack([col, a * 255.0])
        out[..., :3] = np.where(a[..., None] > 0.02, out[..., :3], 0.0)
        return out
    # the additive glow: the bright leading edge and the streak lines only, plus a faint inner haze
    g = rgb(GLOW[el])
    hot = np.where(lead | (steps >= 4), 1.0, 0.0)
    haze = np.clip(1.0 - np.abs(U - 0.35) / 0.55, 0.0, 1.0) * 0.22
    inten = np.clip(hot * 0.9 + haze, 0.0, 1.0) * (a > 0.02) * np.clip(V / 0.08, 0, 1) * np.clip((1.0 - V) / 0.1, 0, 1) ** 0.7
    colg = g[None, None, :] * (1.0 - 0.5 * inten[..., None]) + 255.0 * 0.5 * inten[..., None]
    out = np.dstack([colg * inten[..., None], inten * 255.0])
    return clean_additive(out)


# ------------------------------------------------------------------ halos
def halo(el):
    S, SS = 256, 2
    n = S * SS
    core = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(core)
    c = n / 2

    def pt(r, a):
        return (c + math.cos(a) * r * n / 2, c + math.sin(a) * r * n / 2)

    def ring(r, wdt):
        d.ellipse([c - r * n / 2 - wdt * n / 2, c - r * n / 2 - wdt * n / 2, c + r * n / 2 + wdt * n / 2, c + r * n / 2 + wdt * n / 2], outline=255, width=max(1, int(wdt * n)))

    if el == "wind":                                                              # a crown: a thin ring with long thin spikes, short ones between
        ring(0.5, 0.012); ring(0.4, 0.006)
        for i in range(16):
            a = i * 2 * math.pi / 16 + 0.1
            long_ = i % 2 == 0
            tip = pt(0.98 if long_ else 0.74, a)
            d.polygon([pt(0.5, a - 0.07), tip, pt(0.5, a + 0.07)], fill=255)
    elif el == "fire":                                                            # flame tongues that lean the same way (a turning sun)
        ring(0.46, 0.012); ring(0.38, 0.007)
        for i in range(10):
            a = i * 2 * math.pi / 10
            p0, p1 = pt(0.46, a - 0.16), pt(0.46, a + 0.16)
            mid = pt(0.78, a + 0.30)
            tip = pt(0.97, a + 0.55)
            d.polygon([p0, pt(0.7, a - 0.02), tip, mid, p1], fill=255)
    elif el == "water":                                                           # a double ring with scalloped waves and drops
        ring(0.5, 0.012); ring(0.4, 0.007)
        for i in range(20):
            a = i * 2 * math.pi / 20
            x, y = pt(0.56, a)
            r = 0.05 * n / 2
            d.ellipse([x - r, y - r, x + r, y + r], outline=255, width=max(1, int(0.012 * n)))
        for i in range(10):
            a = i * 2 * math.pi / 10 + 0.15
            x, y = pt(0.82, a)
            r = 0.028 * n / 2
            d.ellipse([x - r, y - r, x + r, y + r], fill=255)
    else:                                                                         # crystals: a ring with angular shards
        ring(0.48, 0.012); ring(0.4, 0.006)
        for i in range(9):
            a = i * 2 * math.pi / 9 + 0.2
            lng = 0.97 if i % 3 == 0 else 0.78
            d.polygon([pt(0.48, a - 0.09), pt(0.62, a - 0.1), pt(lng, a), pt(0.62, a + 0.1), pt(0.48, a + 0.09)], fill=255)
    core = core.resize((S, S), Image.LANCZOS)
    c_arr = np.asarray(core, np.float32) / 255.0
    glow = np.asarray(core.filter(ImageFilter.GaussianBlur(5)), np.float32) / 255.0
    inten = np.clip(c_arr * 1.0 + glow * 0.9, 0.0, 1.0)
    g = rgb(GLOW[el])
    colr = g[None, None, :] * (1.0 - c_arr[..., None]) + 255.0 * c_arr[..., None]       # white core, the element's colour in the glow
    out = np.dstack([colr * inten[..., None], inten * 255.0])
    return clean_additive(out)


# ------------------------------------------------------------------ hair cap (hat layer of the head, skin UV layout x4)
def hair(el, rnd):
    S, K = 256, 4
    bands = [rgb(b) for b in BANDS[el]]
    g = rgb(GLOW[el])
    img = np.zeros((S, S, 4), np.float32)

    def face(x0, y0, rows_cover, fringe=False, back=False, top=False):
        W, H = 8 * K, 8 * K
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        cov = np.zeros((H, W), np.float32)
        if top:
            cov[:] = 1.0
        else:
            cov[: int(rows_cover * K)] = 1.0
            if fringe:                                                              # spiky fringe: triangles along the lower edge
                for tx in range(0, W, 5 * K // 2):
                    for r in range(int(rows_cover * K), int((rows_cover + 1.6) * K)):
                        wdt = ((rows_cover + 1.6) * K - r) / (1.6 * K) * (K * 1.4)
                        cov[r, max(0, tx - int(wdt / 2)): min(W, tx + int(wdt / 2))] = 1.0
            else:                                                                   # ragged lower edge
                for tx in range(W):
                    ext = int(K * (0.5 + 1.2 * ((math.sin(tx * 0.9 + rows_cover) + 1) / 2)))
                    cov[int(rows_cover * K): min(H, int(rows_cover * K) + ext), tx] = 1.0
        # strands: vertical streaks in three flat tones
        stripe = np.zeros((H, W), np.float32)
        for sx in range(W):
            stripe[:, sx] = 0.4 + 0.6 * ((sx * 7 + int(rnd.uniform(0, 3))) % 5) / 4.0
        tone = np.where(stripe > 0.82, 1.0, np.where(stripe > 0.55, 0.72, 0.5))
        shade = 1.0 - 0.25 * yy / H                                                 # a little darker toward the roots' end
        inten = tone * shade
        colr = np.zeros((H, W, 3), np.float32)
        for lv, (lo, col) in enumerate(((0.0, bands[1]), (0.6, bands[2]), (0.85, bands[3]))):
            m = inten >= lo
            colr[m] = col
        colr[inten >= 0.95] = bands[4]
        colr = colr * 0.5 + g[None, None, :] * 0.5 * inten[..., None]
        sl = (slice(y0 * K, y0 * K + H), slice(x0 * K, x0 * K + W))
        img[sl][..., :3] = np.where(cov[..., None] > 0, colr, 0.0)
        img[sl][..., 3] = cov * 255.0

    face(40, 0, 8, top=True)                                                        # top
    face(56, 8, 7.0, back=True)                                                    # back: nearly the whole face
    face(32, 8, 4.0)                                                               # right side
    face(48, 8, 4.0)                                                               # left side
    face(40, 8, 2.0, fringe=True)                                                  # front: a fringe over the forehead
    return clean_additive(img)


# ------------------------------------------------------------------ small ones
def rim():
    n = 64
    v = np.zeros((n, n, 3), np.float32)
    for x in range(n):
        v[:, x] = 215 + 40 * (((x * 5) % 7) / 6.0)
    return v


def soft():
    n = 64
    v = np.zeros((n, n, 3), np.float32)
    for x in range(n):
        v[:, x] = 150 + 50 * (((x * 3) % 5) / 4.0)
    return v


def eye():
    W, H, SS = 64, 32, 4
    im = Image.new("L", (W * SS, H * SS), 0)
    d = ImageDraw.Draw(im)
    pts = [(2, 24), (14, 10), (40, 6), (62, 8), (44, 20), (20, 27)]                 # a slanted almond eye (the outer corner up)
    d.polygon([(x * SS, y * SS) for x, y in pts], fill=255)
    im = im.resize((W, H), Image.LANCZOS)
    a = np.asarray(im, np.float32) / 255.0
    glowa = np.asarray(im.filter(ImageFilter.GaussianBlur(3)), np.float32) / 255.0
    inten = np.clip(a + glowa * 0.7, 0, 1)
    colr = np.stack([np.full_like(a, 255.0)] * 3, axis=-1)
    colr = colr * (0.55 + 0.45 * a[..., None])                                       # white core, the rest takes the vertex tint
    out = np.dstack([colr * inten[..., None], inten * 255.0])
    return clean_additive(out)


def burst():
    S, SS = 128, 4
    n = S * SS
    im = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(im)
    c = n / 2
    for i in range(12):                                                            # the anime flash: long thin rays and a bright core
        a = i * math.pi / 6 + 0.13
        ln = (0.98 if i % 2 == 0 else 0.7) * n / 2
        w = 0.05 if i % 2 == 0 else 0.08
        d.polygon([(c + math.cos(a - w) * 0.05 * n, c + math.sin(a - w) * 0.05 * n), (c + math.cos(a) * ln, c + math.sin(a) * ln), (c + math.cos(a + w) * 0.05 * n, c + math.sin(a + w) * 0.05 * n)], fill=255)
    d.ellipse([c - 0.16 * n, c - 0.16 * n, c + 0.16 * n, c + 0.16 * n], fill=255)
    im = im.resize((S, S), Image.LANCZOS)
    a = np.asarray(im, np.float32) / 255.0
    glowa = np.asarray(im.filter(ImageFilter.GaussianBlur(4)), np.float32) / 255.0
    inten = np.clip(a + glowa * 0.8, 0, 1)
    out = np.dstack([np.full_like(a, 255.0) * inten, np.full_like(a, 255.0) * inten, np.full_like(a, 255.0) * inten, inten * 255.0])
    return clean_additive(out)


def main():
    import random
    r = random.Random(20260317)

    class R:                                                                      # a tiny shim: numpy's RandomState with the python-random call names used above
        def uniform(self, a, b): return r.uniform(a, b)
        def choice(self, s): return r.choice(s)

    rr = R()
    for el in ELS:
        save(swirl(el, rr), "sda_swirl_" + el, "RGB")
        save(plume(el, 128, 256, rr, False, False), "sda_plume_" + el, "RGBA")
        save(plume(el, 128, 256, rr, True, False), "sda_plume_glow_" + el, "RGBA")
        save(plume(el, 64, 128, rr, False, True), "sda_lock_" + el, "RGBA")
        save(plume(el, 64, 128, rr, True, True), "sda_lock_glow_" + el, "RGBA")
        save(halo(el), "sda_halo_" + el, "RGBA")
        save(hair(el, rr), "sda_hair_" + el, "RGBA")
    save(rim(), "sda_rim", "RGB")
    save(soft(), "sda_soft", "RGB")
    save(eye(), "sda_eye", "RGBA")
    save(burst(), "sda_burst", "RGBA")
    print("wrote", len(ELS) * 7 + 4, "textures to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
