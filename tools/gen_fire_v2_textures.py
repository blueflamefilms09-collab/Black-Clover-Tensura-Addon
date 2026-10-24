#!/usr/bin/env python3
"""
Fire VFX V2 (in the game, docs/vfx_remake/fire_ingame.md): writes the game textures `fnw_*.png` into src/main/resources/assets/nusmp/textures/particle.

    python tools/gen_fire_v2_textures.py            # Pillow + numpy only, deterministic (fixed seeds)

It reuses the painters of tools/gen_fire_new_textures.py (the owner-approved A "anime cel" and B "volumetric glow" textures of the preview round, unchanged) and adds the shapes of the in-game
build: the round cel fireball of Wild Bursting Flame (A_orb_a/b), its light beam (A_beam), the ground fire carpet (A_floor) and the ground swirl of the Ignis Columna vortex (A_swirl).

Post process, so the game files are safe to draw:
  * colour is bled into fully transparent texels (no dark fringe when the texture is sampled with linear filtering);
  * every texture except the smoke puffs gets a grey twin `fnw_<name>_t.png` (luminance only, same silhouette); an effect whose colour is not a fire colour (blue, green, black ...) draws the
    twin multiplied by its colour, so the tint of the pages that reuse the fire shapes keeps working (the same idea as the wmc_*_t twins of the Wind look).

File names: fnw_<a|b>_<name>[_<frame>][_t].png, the list lives in vfx/FireV2.java (TEXTURE_NAMES) and is pinned by FireV2AssetTest.
"""
import math
import os
import shutil
import sys
import tempfile

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_fire_new_textures as g                                                  # noqa: E402

ROOT = g.ROOT
GAME = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
PREFIX = "fnw_"
DROP = {"a_spark", "b_smoke", "b_spark", "b_tongue_a", "b_tongue_b"}                          # preview-only textures that no in-game effect draws (no dead assets)
NO_TWIN = {"a_smoke_a", "a_smoke_b"}                                    # smoke stays the same dark brown-grey whatever the colour of the spell


# ------------------------------------------------------------------------------------------------ the new A textures
def radial_cel(size, rad_fn, bands, centre_shift=(0.0, 0.0), ss=None):
    """a flat-tone disc painted analytically (round inner bands, no box-erosion corners): rad_fn(theta) = outline radius as a fraction of size, bands = [(colour, fraction of the local radius at which the band starts)],
    the inner bands slide by centre_shift (fractions of size) so the highlight sits low and to one side like the cel tutorials"""
    ss = ss or g.SS
    s = size * ss
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    out = np.zeros((s, s, 4), np.float32)
    dx, dy = xx - s * 0.5, yy - s * 0.5
    th = np.arctan2(dy, dx)
    r0 = np.sqrt(dx ** 2 + dy ** 2) / s
    rad = rad_fn(th)
    inside = r0 < rad
    out[inside, 3] = 1.0
    for col, frac in bands:
        sx, sy = centre_shift if frac > 0 else (0.0, 0.0)
        k = frac * 1.0
        ddx, ddy = dx - s * sx * frac, dy - s * sy * frac
        rr = np.sqrt(ddx ** 2 + ddy ** 2) / s
        reg = (rr < rad * (1.0 - frac)) & inside
        out[reg, :3] = np.array(col, np.float32) / 255.0
    return out


def cel_orb(frame):
    """the round cel fireball of Leopold_wild_bursting_flame: a big pale-yellow disc, a gold and an orange band, a thin red-orange rim, a slightly scalloped outline"""
    r = g.rg(5100 + frame)
    ph = r.random(3) * 6.28

    def rad(th):
        return 0.40 * (1 + 0.035 * np.sin(9 * th + ph[0]) + 0.025 * np.sin(14 * th + ph[1]) + 0.02 * np.sin(21 * th + ph[2]))
    img = radial_cel(128, rad, [(g.C_RIM, 0.0), (g.C_BODY, 0.028), (g.C_LIGHT, 0.055), (g.C_GOLD, 0.095), (g.C_CORE, 0.16), (g.C_HOT, 0.52)], centre_shift=(-0.05, 0.06))
    return g.save(g.down(img, (128, 128)), f"A_orb_{'ab'[frame]}")


def cel_beam():
    """a long thin light streak (the yellow beams of the still): sharp tail at the left, full width for most of the length, a blunt head at the right"""
    w, h = 256 * g.SS, 32 * g.SS
    cy = h * 0.5
    n = 60
    top, bot = [], []
    for i in range(n + 1):
        t = i / n
        env = min(1.0, t / 0.55) ** 0.9 * (1.0 if t < 0.93 else max(0.0, 1.0 - (t - 0.93) / 0.07 * 0.9))
        wd = h * 0.30 * env
        x = w * (0.01 + 0.98 * t)
        top.append((x, cy - wd))
        bot.append((x, cy + wd))
    m = g.mask_of([top + bot[::-1]], (w, h))
    img = g.cel_paint(m, [g.C_BODY, (g.C_GOLD, 9, 0, 0), (g.C_CORE, 22, 0, 0), (g.C_HOT, 34, 0, 0)], 5300, wobble=1.5)
    return g.save(g.down(img, (256, 32)), "A_beam")


def cel_floor():
    """the ground fire carpet seen from above: a filled disc whose rim is a ring of outward flame teeth, round nested yellow tones towards a hot centre (the flat yellow floor of the still)"""
    S = g.SS
    s = 256 * S
    r = g.rg(5400)
    polys = [[(s * 0.5 + s * 0.31 * math.cos(a), s * 0.5 + s * 0.31 * math.sin(a)) for a in np.linspace(0, 6.28318, 120)]]
    n = 26
    for k in range(n):
        a = 6.28318 * k / n + (r.random() - 0.5) * 0.05
        bx, by = s * 0.5 + s * 0.29 * math.cos(a), s * 0.5 + s * 0.29 * math.sin(a)
        ln = s * (0.10 + 0.07 * r.random()) * (1.0 if k % 3 else 0.7)
        polys.append(g.place(g.tongue_pts(ln, s * 0.046, 0.5 * (1 if k % 2 else -1), 5500 + k), bx, by, ang=a + math.pi / 2))
    m = g.mask_of(polys, (s, s)) > 0.5
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    rr = np.sqrt((xx - s * 0.5) ** 2 + (yy - s * 0.5) ** 2) / s
    out = np.zeros((s, s, 4), np.float32)
    out[m, 3] = 1.0
    for col, rmax in [(g.C_BODY, 9.9), (g.C_LIGHT, 0.335), (g.C_GOLD, 0.28), (g.C_CORE, 0.20), (g.C_HOT, 0.10)]:
        reg = m & (rr < rmax)
        out[reg, :3] = np.array(col, np.float32) / 255.0
    return g.save(g.down(out, (256, 256)), "A_floor")


def cel_swirl():
    """a spinning vortex of flame seen from above: three curved arms of hooked flame going round the centre, hole in the middle (the caster stands there)"""
    s = 256 * g.SS
    r = g.rg(5700)
    polys = []
    arms = 3
    for k in range(arms):
        a0 = 6.28318 * k / arms
        pts = []
        steps = 40
        left, right = [], []
        for i in range(steps + 1):
            t = i / steps
            ang = a0 + t * 3.5
            rad = s * (0.14 + 0.29 * t)
            cx, cy = s * 0.5 + rad * math.cos(ang), s * 0.5 + rad * math.sin(ang)
            tx, ty = -math.sin(ang), math.cos(ang)
            nx, ny = math.cos(ang), math.sin(ang)
            wd = s * 0.078 * (0.35 + 0.65 * math.sin(min(1.0, t * 1.4) * math.pi / 2)) * (1 - t ** 3 * 0.85)
            left.append((cx + nx * wd, cy + ny * wd))
            right.append((cx - nx * wd, cy - ny * wd))
        polys.append(left + right[::-1])
        for j in range(3):
            tt = 0.25 + 0.25 * j
            ang = a0 + tt * 3.1
            rad = s * (0.16 + 0.27 * tt)
            bx, by = s * 0.5 + rad * math.cos(ang), s * 0.5 + rad * math.sin(ang)
            polys.append(g.place(g.tongue_pts(s * (0.10 + 0.04 * r.random()), s * 0.030, 0.9, 5800 + k * 5 + j), bx, by, ang=ang + math.pi))
    m = g.mask_of(polys, (s, s))
    img = g.cel_paint(m, [g.C_RIM, (g.C_BODY, 6, 0, 0), (g.C_LIGHT, 12, 0, 0), (g.C_GOLD, 20, 0, 0), (g.C_CORE, 30, 0, 0)], 5900, wobble=3)
    return g.save(g.down(img, (256, 256)), "A_swirl")


# ------------------------------------------------------------------------------------------------ the game files
def bleed(rgba):
    """copy the colour of the nearest opaque-ish texel into the fully transparent ones (linear filtering then never mixes in black)"""
    a = rgba[..., 3]
    known = a > 0
    rgb = rgba[..., :3].astype(np.float32)
    rgb[~known] = 0
    wt = known.astype(np.float32)
    for _ in range(24):
        if wt.min() > 0:
            break
        acc = np.zeros_like(rgb)
        cnt = np.zeros_like(wt)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                sh = np.roll(np.roll(wt, dy, 0), dx, 1)
                sc = np.roll(np.roll(rgb, dy, 0), dx, 1)
                acc += sc * sh[..., None]
                cnt += sh
        fill = (wt == 0) & (cnt > 0)
        rgb[fill] = acc[fill] / cnt[fill][..., None]
        wt[fill] = 1.0
    out = rgba.copy()
    out[..., :3] = np.clip(rgb + 0.5, 0, 255).astype(np.uint8)
    return out


def grey_twin(rgba):
    """luminance only: the darkest cel band (the red rim) keeps about 60 percent, the hot core is white, so a colour multiplied on keeps the banding"""
    c = rgba[..., :3].astype(np.float32) / 255.0
    y = 0.299 * c[..., 0] + 0.587 * c[..., 1] + 0.114 * c[..., 2]
    v = np.clip(0.34 + 0.66 * np.power(np.clip(y, 0, 1), 0.85), 0, 1)
    out = rgba.copy()
    out[..., :3] = np.clip(np.repeat(v[..., None], 3, axis=2) * 255 + 0.5, 0, 255).astype(np.uint8)
    return out


def game_name(src):
    return src[:-4].lower()                                                        # A_tongue_a.png -> a_tongue_a


def main():
    tmp = tempfile.mkdtemp(prefix="fnw_")
    g.OUT = tmp
    for f in range(3):
        g.cel_tongue(f), g.cel_ball(f), g.cel_body(f)
    for f in range(2):
        g.cel_swoosh(f), g.cel_lion(f), g.cel_smoke(f), g.cel_chip(f)
        cel_orb(f)
    g.cel_ring(), g.cel_rays(), g.cel_flash(), g.cel_spark()
    cel_beam(), cel_floor(), cel_swirl()
    for v in range(4):
        g.soft_puff(v)
    for v in range(2):
        g.soft_tongue(v)
    g.soft_halo(), g.soft_streak(), g.soft_ember(), g.soft_smoke(), g.soft_rays(), g.soft_ring(), g.soft_spark()
    os.makedirs(GAME, exist_ok=True)
    for old in os.listdir(GAME):
        if old.startswith(PREFIX):
            os.remove(os.path.join(GAME, old))
    n = 0
    names = []
    for fn in sorted(os.listdir(tmp)):
        if not fn.endswith(".png"):
            continue
        name = game_name(fn)
        if name in DROP:
            continue
        names.append(name)
        rgba = np.asarray(Image.open(os.path.join(tmp, fn)).convert("RGBA"))
        base = bleed(rgba)
        Image.fromarray(base, "RGBA").save(os.path.join(GAME, f"{PREFIX}{name}.png"), optimize=True)
        n += 1
        if name not in NO_TWIN:
            Image.fromarray(grey_twin(base), "RGBA").save(os.path.join(GAME, f"{PREFIX}{name}_t.png"), optimize=True)
            n += 1
    shutil.rmtree(tmp, ignore_errors=True)
    print(n, "textures ->", GAME)
    print("names:", ", ".join(names))


if __name__ == "__main__":
    main()
