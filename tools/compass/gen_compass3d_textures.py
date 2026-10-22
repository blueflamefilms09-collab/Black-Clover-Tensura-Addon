"""Compass Magic 3D (docs/magic/compass_3d.md): the textures of the one floating 3D compass and of the things that come out of it.

    python3 -B tools/compass/gen_compass3d_textures.py

Writes assets/nusmp/textures/entity/compass3d/{rim,face,face_glow,star,needle,glass,aura,glint,seal_ring}.png, deterministically (fixed seed, no clock).
The layout (rim profile rows and their texture rows, the star's texture columns, the sizes) is tools/compass/compass3d_layout.json, shared with
compass3d/CompassMesh.java. The look follows the anime compass: a thick olive-bronze rim, concentric green rings, a pale cream and gold 8 point
compass rose, a thin gold needle with a small ring at its tail, a glassy glint, a spiky yellow aura behind.
Every additive texture (aura, glint, seal_ring, face_glow) has no colour under a fully transparent texel (SafeAdditiveTextureScanTest).
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "entity", "compass3d")
LAYOUT = json.load(open(os.path.join(HERE, "compass3d_layout.json")))
SZ = LAYOUT["textures"]

BRONZE = np.array([122, 104, 46], np.float32)
BRONZE_HI = np.array([222, 198, 116], np.float32)
BRONZE_LO = np.array([58, 48, 22], np.float32)
GOLD = np.array([238, 200, 96], np.float32)
GOLD_HI = np.array([255, 236, 160], np.float32)
GREEN_OUT = np.array([22, 74, 50], np.float32)
GREEN_MID = np.array([44, 128, 78], np.float32)
GREEN_LIGHT = np.array([84, 176, 108], np.float32)
GREEN_CORE = np.array([152, 212, 148], np.float32)
CREAM = np.array([250, 240, 196], np.float32)
CREAM_DARK = np.array([212, 176, 88], np.float32)


def save(name, rgba):
    arr = np.clip(rgba, 0, 255).astype(np.uint8)
    arr[arr[..., 3] == 0, :3] = 0                      # no colour under a fully transparent texel
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(arr, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, arr.shape[1], "x", arr.shape[0])


def mix(a, b, t):
    t = np.asarray(t, np.float32)
    if t.ndim == 0:
        return a * (1 - t) + b * t
    t = t[..., None]
    return a * (1 - t) + b * t


def put(img, mask, colour):
    """img[..., :3][mask] = colour (a constant colour, or a full-size colour array)."""
    colour = np.asarray(colour, np.float32)
    img[..., :3][mask] = colour[mask] if colour.ndim == 3 else colour


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def smooth(x):
    x = np.clip(x, 0, 1)
    return x * x * (3 - 2 * x)


# ================================================================ rim
def rim():
    w, h = SZ["rim"]
    u, v = grid(w, h)
    img = np.zeros((h, w, 4), np.float32)
    img[..., 3] = 255
    rng = np.random.default_rng(5)
    brushed = rng.normal(0, 1, (h, w)).astype(np.float32)
    for k in range(3):                                      # smear along u: brushed metal
        brushed = (brushed + np.roll(brushed, 1, 1) + np.roll(brushed, -1, 1)) / 3
    base = mix(BRONZE_LO, BRONZE, np.clip(0.55 + 0.6 * brushed, 0, 1))
    img[..., :3] = base
    rows = LAYOUT["rimRows"]

    def band(i):
        return (v >= rows[i][4]) & (v < rows[i][5] + (1e-6 if i == len(rows) - 1 else 0))

    def local(i):
        return (v - rows[i][4]) / (rows[i][5] - rows[i][4])

    # back dome (0..2): dark bronze with two engraved rings
    for i in (0, 1, 2):
        m = band(i)
        put(img, m, mix(BRONZE_LO * 1.1, BRONZE * 0.8, np.clip(0.4 + 0.6 * brushed, 0, 1)))
    put(img, (np.abs(v - 0.06) < 0.012) | (np.abs(v - 0.12) < 0.012), BRONZE_LO * 0.7)
    # back outer bevel (3): lighter
    put(img, band(3), mix(BRONZE, BRONZE_HI, 0.5 * smooth(local(3))))
    # outer wall (4): olive bronze with 16 engraved notches and a worn highlight stripe
    m = band(4)
    lv = local(4)
    notch = (np.abs(((u * 16) % 1.0) - 0.5) < 0.07) & (lv > 0.18) & (lv < 0.82)
    wall = mix(BRONZE, BRONZE_HI, np.clip(0.18 + 0.5 * np.exp(-((lv - 0.30) / 0.10) ** 2), 0, 1))
    put(img, m, wall)
    put(img, m & notch, BRONZE_LO * 0.8)
    edge = m & (((lv < 0.08)) | (lv > 0.92))
    put(img, edge, BRONZE_LO)
    # front outer bevel (5) and top flat (6): the bright lip, with a travelling pattern of fine dashes
    put(img, band(5), mix(BRONZE, BRONZE_HI, 0.55))
    m = band(6)
    lv = local(6)
    lip = mix(BRONZE_HI, np.array([250, 232, 160], np.float32), 0.5 + 0.25 * np.sin(u * 2 * math.pi * 3))
    put(img, m, lip)
    dash = m & (np.abs(((u * 64) % 1.0) - 0.5) < 0.1) & (lv > 0.3) & (lv < 0.7)
    put(img, dash, BRONZE * 0.85)
    # inner bevel (7): darker (in shadow)
    put(img, band(7), mix(BRONZE, BRONZE_LO, 0.55))
    # inner lip (8): a gold ring, the glass seat
    m = band(8)
    put(img, m, mix(GOLD, GOLD_HI, 0.5 + 0.5 * np.sin(u * 2 * math.pi * 8)))
    save("rim", img)


# ================================================================ face
def polar(w, h, R):
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, (0.5 - v) * 2
    return np.sqrt(x * x + y * y), np.degrees(np.arctan2(x, y)) % 360.0     # r (1 = the disc edge), bearing from +y (north) clockwise


def face():
    w, h = SZ["face"]
    r, ang = polar(w, h, 1.0)
    img = np.zeros((h, w, 4), np.float32)
    glow = np.zeros((h, w, 4), np.float32)
    col = np.zeros((h, w, 3), np.float32)
    # concentric green bands, light to dark outward
    col[...] = GREEN_CORE
    col = np.where((r > 0.30)[..., None], mix(GREEN_LIGHT, GREEN_MID, np.clip((r - 0.30) / 0.30, 0, 1) * 0.6), col)
    col = np.where((r > 0.60)[..., None], mix(GREEN_MID, GREEN_OUT, np.clip((r - 0.60) / 0.26, 0, 1)), col)
    col = np.where((r > 0.86)[..., None], GREEN_OUT * 0.9, col)
    # fine concentric engraving: dark thin lines
    for rr in (0.45, 0.72):
        col = np.where((np.abs(r - rr) < 0.008)[..., None], col * 0.72, col)
    gold_lines = np.zeros((h, w), np.float32)
    for rr, wd in ((0.30, 0.012), (0.60, 0.014), (0.86, 0.014), (0.89, 0.007), (0.985, 0.012), (0.15, 0.008)):
        gold_lines = np.maximum(gold_lines, (np.abs(r - rr) < wd).astype(np.float32))
    # tick marks: every 5 degrees short, every 15 long, every 90 a gold triangle
    ticks = np.zeros((h, w), np.float32)
    a5 = np.abs(((ang + 2.5) % 5) - 2.5)
    ticks = np.maximum(ticks, ((a5 < 0.55 * (0.97 / np.maximum(r, 0.2))) & (r > 0.915) & (r < 0.96)).astype(np.float32))
    a15 = np.abs(((ang + 7.5) % 15) - 7.5)
    ticks = np.maximum(ticks, ((a15 < 0.9) & (r > 0.905) & (r < 0.975)).astype(np.float32))
    for k in range(4):
        d = np.abs(((ang - k * 90 + 180) % 360) - 180)
        tri = (r > 0.865) & (r < 0.985) & (d < 6.0 * (r - 0.865) / 0.12 * 0 + (1.0 - (r - 0.865) / 0.12) * 5.0)
        ticks = np.maximum(ticks, tri.astype(np.float32))
    # a ring of small diamonds between the green bands (decor), 16 of them
    d16 = np.abs(((ang + 11.25) % 22.5) - 11.25)
    diam = (np.abs(r - 0.45) * 5.0 + d16 / 180.0 * 2 * math.pi * 0.45 * 5.0 < 0.045) & (r > 0.40) & (r < 0.5)
    gold = np.maximum(gold_lines, ticks)
    gold = np.maximum(gold, diam.astype(np.float32))
    col = mix(col, GOLD, gold)
    # soft lighting: the upper left is brighter, an inner vignette near the rim
    u, v = grid(w, h)
    light = 1.0 + 0.16 * ((0.5 - u) + (0.5 - v)) - 0.12 * np.clip((r - 0.8) / 0.2, 0, 1)
    col = col * light[..., None]
    img[..., :3] = col
    img[..., 3] = np.where(r <= 1.0, 255, 0)
    save("face", img)
    # the glow map: only the gold lines and ticks (what shines at night), black elsewhere (additive: black adds nothing)
    glow[..., :3] = GOLD_HI[None, None, :] * gold[..., None] * 0.9
    glow[..., 3] = 255
    save("face_glow", glow)


# ================================================================ star
def facet_tex(name, lo, hi, u_ranges, size, lines=True):
    w, h = size
    u, v = grid(w, h)
    img = np.zeros((h, w, 4), np.float32)
    img[..., 3] = 255
    for key, c0, c1 in (("light", lo[0], lo[1]), ("dark", hi[0], hi[1])):
        a, b = u_ranges[key]
        m = (u >= a - 0.02) & (u <= b + 0.02)             # painted a little wider than sampled, so no black texel at a seam
        col = mix(c0, c1, v)
        img[..., :3][m] = col[m]
    return img


def star_and_needle():
    t = LAYOUT["starTexU"]
    # star: light facet (u .02-.38), dark facet (u .42-.78), outline (u .82-.98); v runs hub (0) to tip (1)
    img = facet_tex("star", (CREAM * 1.0, CREAM_DARK * 1.02), (CREAM_DARK * 0.95, CREAM_DARK * 0.62), t, SZ["star"])
    w, h = SZ["star"]
    u, v = grid(w, h)
    m = (u >= t["outline"][0] - 0.02) & (u <= t["outline"][1] + 0.02)
    img[..., :3][m] = (GOLD * 0.62)[None, :]
    # a gold edge on both facets along the ridge side of v=0..1 (thin line near u edges)
    save("star", img)
    # needle: same columns, gold; the tail ring uses the light column
    nd = facet_tex("needle", (np.array([255, 216, 92], np.float32), np.array([236, 164, 36], np.float32)), (np.array([196, 128, 30], np.float32), np.array([112, 70, 18], np.float32)), t, SZ["needle"])
    w2, h2 = SZ["needle"]
    u2, _ = grid(w2, h2)
    put(nd, u2 >= t["dark"][1] + 0.02, np.array([96, 62, 16], np.float32))              # the unused columns hold the darkest gold, never black
    save("needle", nd)


# ================================================================ glass
def glass():
    w, h = SZ["glass"]
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, (0.5 - v) * 2
    r = np.sqrt(x * x + y * y)
    img = np.zeros((h, w, 4), np.float32)
    base = np.array([210, 240, 235], np.float32)
    a = 22 + 18 * smooth((r - 0.6) / 0.4)                                   # a faint tint, a little more toward the edge
    # a bright crescent highlight upper left and a small specular dot
    cres = np.exp(-((np.sqrt((x + 0.30) ** 2 + (y - 0.30) ** 2) - 0.62) / 0.07) ** 2) * ((x < 0) & (y > 0) & (r < 0.92))
    dot = np.exp(-(((x + 0.46) ** 2 + (y - 0.50) ** 2) / 0.004))
    stripe = np.exp(-(((x + y * 0.9 - 0.18) / 0.05) ** 2)) * (r < 0.9) * 0.35         # a soft diagonal sheen
    k = np.clip(cres * 0.85 + dot + stripe, 0, 1)
    img[..., :3] = mix(base, np.array([255, 255, 255], np.float32), k)
    img[..., 3] = np.where(r <= 1.0, np.clip(a + 190 * k, 0, 235), 0)
    save("glass", img)


# ================================================================ aura: the spiky yellow burst behind the compass
def aura():
    w, h = SZ["aura"]
    k = 4                                                                              # supersample, then box-filter down (smooth spike edges)
    r, ang = polar(w * k, h * k, 1.0)
    rng = np.random.default_rng(11)
    n = 26
    spike = np.zeros_like(r)
    for i in range(n):
        a0 = i * 360.0 / n + rng.uniform(-3, 3)
        length = (0.78 + 0.22 * rng.random()) if i % 2 == 0 else (0.52 + 0.14 * rng.random())
        d = np.abs(((ang - a0 + 180) % 360) - 180)
        t = np.clip((r - 0.40) / (length - 0.40 + 1e-6), 0, 1)
        half = 5.2 * (1 - t) ** 1.15                                                 # tapers to a point
        spike = np.maximum(spike, ((d < half) & (r < length) & (r > 0.3)) * (1 - 0.55 * t))
    body = smooth((0.92 - r) / 0.55) * 0.5                                           # the soft disc behind
    core = np.clip(np.maximum(spike * 0.95, body), 0, 1)
    core = core * (1 - smooth((r - 0.92) / 0.08))
    core = core * (r > 0.30)                                                         # the compass covers the middle anyway
    colour = mix(np.array([255, 168, 30], np.float32), np.array([255, 236, 130], np.float32), np.clip(1.15 - r * 1.35, 0, 1))
    pre = np.zeros((h * k, w * k, 4), np.float32)                                    # premultiplied for the filter
    pre[..., :3] = colour * core[..., None]
    pre[..., 3] = core
    pre = pre.reshape(h, k, w, k, 4).mean(axis=(1, 3))
    img = np.zeros((h, w, 4), np.float32)
    a = pre[..., 3]
    img[..., :3] = np.where(a[..., None] > 1e-4, pre[..., :3] / np.maximum(a[..., None], 1e-4), 0)
    img[..., 3] = a * 255 * 0.9
    save("aura", img)


# ================================================================ glint: a four point flare
def glint():
    w, h = SZ["glint"]
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, (0.5 - v) * 2
    r = np.sqrt(x * x + y * y)
    flare = np.exp(-(np.minimum(np.abs(x), np.abs(y)) / 0.07) ** 2) * np.exp(-r * 1.6) + np.exp(-(r / 0.22) ** 2)
    flare = np.clip(flare, 0, 1) * (1 - smooth((r - 0.85) / 0.15))
    img = np.zeros((h, w, 4), np.float32)
    img[..., :3] = np.array([255, 246, 200], np.float32)
    img[..., 3] = flare * 255
    save("glint", img)


# ================================================================ seal ring: the card under a locked target
def seal_ring():
    w, h = SZ["sealRing"]
    k = 4
    r, ang = polar(w * k, h * k, 1.0)
    a = np.zeros(r.shape, np.float32)
    for rr, wd in ((0.93, 0.022), (0.62, 0.012)):
        a = np.maximum(a, (np.abs(r - rr) < wd).astype(np.float32))
    a5 = np.abs(((ang + 5) % 10) - 5)
    a = np.maximum(a, ((a5 < 0.8) & (r > 0.80) & (r < 0.90)).astype(np.float32))
    a45 = np.abs(((ang + 22.5) % 45) - 22.5)
    a = np.maximum(a, ((a45 < 1.4) & (r > 0.62) & (r < 0.80)).astype(np.float32))
    for q in range(4):                                       # four corner brackets
        d = np.abs(((ang - q * 90 - 45 + 180) % 360) - 180)
        a = np.maximum(a, ((d < 11) & (np.abs(r - 0.985) < 0.012)).astype(np.float32))
    a = np.maximum(a, np.exp(-((r - 0.93) / 0.06) ** 2) * 0.35)                       # a soft halo on the outer ring
    a = a * (r <= 1.0)
    a = a.reshape(h, k, w, k).mean(axis=(1, 3))
    img = np.zeros((h, w, 4), np.float32)
    img[..., :3] = np.array([255, 214, 110], np.float32)
    img[..., 3] = np.clip(a, 0, 1) * 255
    save("seal_ring", img)


def main():
    rim()
    face()
    star_and_needle()
    glass()
    aura()
    glint()
    seal_ring()


if __name__ == "__main__":
    main()
