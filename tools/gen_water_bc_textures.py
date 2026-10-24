#!/usr/bin/env python3
"""
WATER remake, hybrid "BC" (C 3D toon core + B volumetric glass wrap) = PREVIEW ONLY (nothing is registered in the game, nothing goes to assets/).

Extra glass sprites that the hybrid needs on top of the 26 sprites of tools/gen_water_new_textures.py (those stay, nothing is replaced):

  bc_shell    a fresnel shell: clear in the middle, bright at the rim, a crescent specular upper left, a faint refracted crescent lower right   (sphere / dome / head sheen)
  bc_mist     a soft turbulent puff (mist, spray, foam haze)
  bc_sheen    a tileable translucent water sheet with bright edge lines and slow flow streaks (the shell that wraps the body of the dragon)
  bc_ribbon   a thin glass ribbon with a bright core line and tapered ends (flow ribbons spiralling round the body / the lance)

    python tools/gen_water_bc_textures.py     ->  docs/previews/vfx_remake/water_new/tex/bc_*.png   (Pillow + numpy, deterministic)
                                              ->  src/main/resources/assets/nusmp/textures/particle/wbc_*.png   (the game set: the sprites the in-game build draws, copied from the preview
                                                  folder under the prefix `wbc_`, plus the grey twins `wbc_<name>_t` of the sprites the tintable shapes use, and `wbc_solid` for the mesh quads)

The names must equal vfx/WaterBc.SPRITES / TINT_TWINS (WaterBcAssetTest checks both ways).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_water_new_textures import blur, smoothstep, finish, OUT   # noqa: E402  (the same helpers and the same output folder)


def noise(n, seed, octaves=((4, 1.0), (8, 0.5), (16, 0.25))):
    rng = np.random.default_rng(seed)
    acc = np.zeros((n, n), np.float32)
    for g, a in octaves:
        base = rng.random((g, g)).astype(np.float32)
        im = Image.fromarray((base * 255).astype(np.uint8), "L").resize((n, n), Image.BICUBIC)
        acc += a * np.asarray(im, np.float32) / 255.0
    acc -= acc.min()
    return acc / acc.max()


def shell(n=256):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x, y = (xx - n / 2) / (n / 2), (yy - n / 2) / (n / 2)
    r = np.sqrt(x * x + y * y)
    disc = smoothstep(1.0, 0.965, r)
    rimv = smoothstep(0.62, 0.98, r)
    a = disc * (0.07 + 0.62 * rimv ** 1.6)
    col = np.zeros((n, n, 3), np.float32)
    col[:] = np.array([150, 214, 255], np.float32) / 255.0
    col = col * (1 - rimv[..., None]) + np.array([225, 248, 255], np.float32) / 255.0 * rimv[..., None]
    ang = np.arctan2(y, x)
    d_ul = np.abs(((ang - math.radians(-135) + math.pi) % (2 * math.pi)) - math.pi)          # angular distance to the upper left
    spec = smoothstep(math.radians(34), 0.0, d_ul) * smoothstep(0.62, 0.76, r) * smoothstep(0.96, 0.86, r)
    a = np.maximum(a, spec * 0.92)
    col = col * (1 - spec[..., None]) + spec[..., None]
    d_lr = np.abs(((ang - math.radians(45) + math.pi) % (2 * math.pi)) - math.pi)            # faint refracted light, lower right
    refr = smoothstep(math.radians(40), 0.0, d_lr) * smoothstep(0.58, 0.7, r) * smoothstep(0.92, 0.8, r)
    a = np.maximum(a, refr * 0.35)
    col = col * (1 - refr[..., None] * 0.6) + np.array([120, 232, 255], np.float32) / 255.0 * refr[..., None] * 0.6
    rgba = np.zeros((n, n, 4), np.float32)
    rgba[..., :3] = col
    rgba[..., 3] = a
    return Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def mist(n=128):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.sqrt(((xx - n / 2) / (n / 2)) ** 2 + ((yy - n / 2) / (n / 2)) ** 2)
    nz = noise(n, 11)
    a = np.clip((1 - r) * 1.5, 0, 1) ** 1.4 * (0.35 + 0.9 * nz)
    a = np.clip(a * 0.62, 0, 1)
    rgba = np.zeros((n, n, 4), np.float32)
    rgba[..., :3] = np.array([226, 246, 255], np.float32) / 255.0
    rgba[..., 3] = blur(a, 1.6)
    return Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def sheen(w=256, h=64):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = np.abs(yy / (h - 1) - 0.5) * 2                                       # 0 centre .. 1 edge
    u = xx / w
    body = smoothstep(1.0, 0.86, v)
    edge = smoothstep(0.62, 0.9, v) * smoothstep(1.0, 0.93, v)
    flow = np.zeros((h, w), np.float32)
    for k, (fu, fv, ph) in enumerate(((3, 7, 0.4), (5, 11, 1.9), (2, 17, 3.1), (7, 5, 2.4))):
        flow += np.sin(2 * math.pi * (fu * u + fv * (yy / h) * 0.35) + ph + 1.4 * np.sin(2 * math.pi * 2 * u + k))
    flow = smoothstep(0.55, 1.7, flow / 2.2)                                 # tileable (whole numbers of periods in u)
    a = body * (0.30 + 0.42 * edge + 0.28 * flow)
    col = np.zeros((h, w, 3), np.float32)
    deep = np.array([84, 170, 236], np.float32) / 255.0
    pale = np.array([214, 244, 255], np.float32) / 255.0
    t = np.clip(edge * 0.9 + flow * 0.7, 0, 1)[..., None]
    col[:] = deep * (1 - t) + pale * t
    rgba = np.zeros((h, w, 4), np.float32)
    rgba[..., :3] = col
    rgba[..., 3] = np.clip(a, 0, 1)
    return Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def ribbon(w=256, h=32):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = np.abs(yy / (h - 1) - 0.5) * 2
    u = xx / (w - 1)
    taper = smoothstep(0.0, 0.22, u) * smoothstep(1.0, 0.68, u)
    prof = np.clip(1 - v ** 2, 0, 1) * (taper ** 0.8)
    core = np.exp(-(v * 4.2) ** 2) * taper
    a = np.clip(prof * 0.62 + core * 0.45, 0, 1)
    col = np.zeros((h, w, 3), np.float32)
    deep = np.array([120, 205, 255], np.float32) / 255.0
    col[:] = deep * (1 - core[..., None]) + np.array([255, 255, 255], np.float32) / 255.0 * core[..., None]
    rgba = np.zeros((h, w, 4), np.float32)
    rgba[..., :3] = col
    rgba[..., 3] = a
    return Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


GAME = os.path.join(os.path.abspath(os.path.join(HERE, "..")), "src", "main", "resources", "assets", "nusmp", "textures", "particle")
# preview file -> game name (prefix wbc_); the bc_ prefix of the preview is dropped
GAME_SPRITES = {"b_body": "b_body", "b_ripple": "b_ripple", "b_crown": "b_crown", "b_drop": "b_drop", "b_bubble": "b_bubble", "b_glint": "b_glint", "b_lance": "b_lance", "b_sphere": "b_sphere",
                "b_dome": "b_dome", "b_swirl": "b_swirl", "bc_mist": "mist", "bc_ribbon": "ribbon", "bc_sheen": "sheen", "bc_shell": "shell", "glow": "glow",
                "a_body": "a_body", "a_head": "a_head", "a_drop": "a_drop", "a_ring": "a_ring", "a_crown": "a_crown", "a_lance": "a_lance", "a_sphere": "a_sphere", "a_blob": "a_blob",
                "a_dome": "a_dome", "a_flash": "a_flash"}
# sprites the tintable shapes (SPLASH and RING, reused by blood and paint pages) draw: they get a grey twin that the effect's own colour multiplies
TINT_TWINS = ("b_ripple", "b_crown", "b_drop", "b_bubble", "mist", "a_crown", "a_ring", "a_blob")


def grey_twin(im):
    """same silhouette and alpha, colour replaced by its luma, lifted so that colour x grey still reads (the vertex colour multiplies it)"""
    a = np.asarray(im.convert("RGBA"), np.float32) / 255.0
    lum = a[..., 0] * 0.30 + a[..., 1] * 0.59 + a[..., 2] * 0.11
    live = a[..., 3] > 0.02
    top = float(np.percentile(lum[live], 97)) if live.any() else 1.0
    g = np.clip(lum / max(top, 1e-3) * 0.96, 0, 1)
    out = np.zeros_like(a)
    out[..., 0] = out[..., 1] = out[..., 2] = g
    out[..., 3] = a[..., 3]
    return Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA")


def export_game():
    """copy the sprites of the game set out of the preview folder under the wbc_ prefix, write the grey twins and the white solid cell the mesh quads use"""
    os.makedirs(GAME, exist_ok=True)
    for src, name in GAME_SPRITES.items():
        im = Image.open(os.path.join(OUT, src + ".png")).convert("RGBA")
        im.save(os.path.join(GAME, "wbc_" + name + ".png"), optimize=True)
        if name in TINT_TWINS:
            grey_twin(im).save(os.path.join(GAME, "wbc_" + name + "_t.png"), optimize=True)
    Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(os.path.join(GAME, "wbc_solid.png"))
    print(len(GAME_SPRITES) + len(TINT_TWINS) + 1, "game textures ->", GAME)


def main():
    os.makedirs(OUT, exist_ok=True)
    tex = {"bc_shell": shell(), "bc_mist": mist(), "bc_sheen": sheen(), "bc_ribbon": ribbon()}
    for k, im in tex.items():
        im.save(os.path.join(OUT, k + ".png"))
    print(len(tex), "textures ->", OUT)
    export_game()


if __name__ == "__main__":
    main()
