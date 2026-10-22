"""Noelle Silva's armour forms (1.99.0): the sprite and skin textures of the dynamic parts of the painter client/aura/NoelleFormsAura.java.

    python3 -B tools/gen_noelle_forms_textures.py

Writes assets/nusmp/textures/aura/noelle_*.png (+ _glow for the 64x64 skin overlays). Every sprite is NEUTRAL (white-grey on transparent; the painter tints it per form with the vertex colour),
black under its transparent texels and fully transparent at its border (the additive passes of the mod ask for that: docs/vfx_square_bug.md). Deterministic.

  noelle_ring        a band for the halo and gyro rings: runes and ticks, bright rim, soft top and bottom, tiles along u
  noelle_ribbon      a flowing water / ink ribbon: a hot core, streaks running along it, soft edges
  noelle_sigil_val   the ground seal of the Valkyrie Dress: a wing-star in two rings with feathers and runes
  noelle_sigil_mer   the Mermaid's: scallop waves and a shell spiral
  noelle_sigil_drg   the Sea Dragon God's: a scale ring, four claw points and a coiled dragon
  noelle_sigil_blk   the Black Dragon's: a thorn circle, a broken inner ring and horns
  noelle_feather / noelle_ember / noelle_eye / noelle_bubble / noelle_spark    the small sprites
  noelle_*_shell     64x64 skin-layout overlays: water (val / mer), scales (drg), ink (blk) + glow maps; noelle_haze: a soft mist sheet
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import grid, fbm, smooth, TAU  # noqa: E402
from water_aura_lib import Mat, shell_texture  # noqa: E402
from geo_builder import ROOT  # noqa: E402

OUT = os.path.join(ROOT, "textures", "aura")


def save(name, lum, alpha=None, rgb=None):
    """lum 0..1 (h, w); alpha 0..1 (default: lum); rgb optional (h, w, 3) 0..1 (default white * lum). The colour is zeroed where the alpha is 0 and the border is cleared."""
    h, w = lum.shape
    a = lum if alpha is None else alpha
    a = np.clip(a, 0, 1).copy()
    b = max(1, int(min(w, h) * 0.03))
    a[:b, :] = 0
    a[-b:, :] = 0
    a[:, :b] = 0 if name.find("ring") < 0 and name.find("ribbon") < 0 else a[:, :b]       # the band textures tile / run along u
    a[:, -b:] = 0 if name.find("ring") < 0 and name.find("ribbon") < 0 else a[:, -b:]
    col = np.stack([np.clip(lum, 0, 1)] * 3, axis=-1) if rgb is None else rgb
    img = np.zeros((h, w, 4), dtype=np.float32)
    img[..., :3] = col * (a[..., None] > 0.002)
    img[..., 3] = a
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, name + ".png"))


def polar(w, h):
    u, v, x, y = grid(w, h)
    px, py = (u - 0.5) * 2, (v - 0.5) * 2
    return px, py, np.hypot(px, py), np.arctan2(py, px)


def ringline(r, r0, wd):
    return np.clip(1.0 - np.abs(r - r0) / wd, 0, 1)


# ---------------------------------------------------------------------------------------------------------------------- bands
def ring():
    w, h = 256, 64
    u, v, x, y = grid(w, h)
    rng = np.random.default_rng(7)
    core = np.clip(1.0 - np.abs(v - 0.5) * 2.0, 0, 1)
    band = smooth(core, 0.0, 0.35)
    rim = np.clip(1.0 - np.abs(np.abs(v - 0.5) * 2.0 - 0.78) / 0.07, 0, 1)
    tick = (np.abs(((u * 32) % 1.0) - 0.5) < 0.09) & (np.abs(v - 0.5) < 0.22)
    rune = np.zeros_like(u)
    for k in range(8):                                                                       # eight little runes: a bar, a hook, a dot
        c = (k + 0.5) / 8
        dx = (u - c) * w
        dy = (v - 0.5) * h
        rune = np.maximum(rune, np.clip(1 - np.maximum(np.abs(dx) - 1.0, np.abs(dy) - 9 * (0.6 + 0.4 * ((k * 5) % 3) / 2)) * 1.4, 0, 1))
        rune = np.maximum(rune, np.clip(1 - (np.hypot(dx - 4, dy + 7) - 3.0) * 1.2, 0, 1) * (np.abs(np.hypot(dx - 4, dy + 7) - 3.0) < 1.1))
    lum = np.clip(0.30 * band + 0.9 * rim + 0.6 * tick * band + 0.85 * rune + 0.05, 0, 1) * band
    save("noelle_ring", lum, alpha=np.clip(lum * 1.15 + 0.18 * band, 0, 1) * smooth(core, 0.0, 0.2))


def ribbon():
    w, h = 256, 32
    u, v, x, y = grid(w, h)
    rng = np.random.default_rng(9)
    core = np.exp(-((v - 0.5) / 0.16) ** 2)
    wide = np.exp(-((v - 0.5) / 0.42) ** 2)
    n = fbm(rng, w, h, 4, 4)
    streak = 0.5 + 0.5 * np.sin(u * TAU * 5 + (v - 0.5) * 7 + n * 5)
    caustic = np.clip(1 - np.abs(np.sin(u * TAU * 9 + n * 6) * np.sin(v * TAU * 2 + n * 3)) / 0.18, 0, 1)
    lum = np.clip(0.22 * wide + 0.85 * core * (0.65 + 0.35 * streak) + 0.45 * caustic * wide, 0, 1)
    fade = smooth(u, 0.0, 0.12) * smooth(1 - u, 0.0, 0.05)
    save("noelle_ribbon", lum * fade, alpha=np.clip(lum * 1.2, 0, 1) * fade * smooth(1 - np.abs(v - 0.5) * 2, 0, 0.2))


# ---------------------------------------------------------------------------------------------------------------------- sigils
def sigil_base(w):
    px, py, r, a = polar(w, w)
    return px, py, r, a


def sigil_val():
    w = 256
    px, py, r, a = sigil_base(w)
    L = ringline(r, 0.93, 0.018) * 0.9 + ringline(r, 0.86, 0.008) * 0.6 + ringline(r, 0.60, 0.014) * 0.85 + ringline(r, 0.36, 0.010) * 0.7
    band = (r > 0.62) & (r < 0.84)
    rune = np.clip(1 - np.abs(((a / TAU * 24) % 1.0) - 0.5) / 0.10, 0, 1) * band * 0.7 * np.clip(1 - np.abs(r - 0.73) / 0.09, 0, 1)
    star = np.zeros_like(r)
    for k in range(8):                                                                          # eight feathers: long petals from the centre
        ang = k * TAU / 8
        d = np.abs(np.sin(a - ang)) * r
        along = r * np.cos(a - ang)
        petal = np.clip(1 - d / (0.075 * (1 - np.abs(along - 0.45) / 0.45).clip(0.05, 1)), 0, 1) * (along > 0) * (along < 0.9)
        star = np.maximum(star, petal * (0.35 + 0.5 * ringline(along, 0.45, 0.45)))
    for k in range(8):                                                                          # small wings between the feathers
        ang = (k + 0.5) * TAU / 8
        d = np.abs(np.sin(a - ang)) * r
        along = r * np.cos(a - ang)
        star = np.maximum(star, np.clip(1 - d / 0.03, 0, 1) * (along > 0.35) * (along < 0.62) * 0.6)
    lum = np.clip(L + rune + star * 0.8, 0, 1) * smooth(1 - r, 0.0, 0.08)
    save("noelle_sigil_val", lum)


def sigil_mer():
    w = 256
    px, py, r, a = sigil_base(w)
    L = ringline(r, 0.93, 0.016) * 0.9 + ringline(r, 0.52, 0.012) * 0.7
    scal = np.zeros_like(r)
    for k in range(14):                                                                         # a ring of scallop arcs
        ang = k * TAU / 14
        cx, cy = math.cos(ang) * 0.74, math.sin(ang) * 0.74
        d = np.hypot(px - cx, py - cy)
        scal = np.maximum(scal, np.maximum(ringline(d, 0.20, 0.016), ringline(d, 0.13, 0.012) * 0.7) * (np.hypot(px, py) > 0.55))
    sp = np.zeros_like(r)                                                                       # a shell spiral in the middle
    th = a + TAU * 3 * (0.0)
    spiral = np.abs(((r * 5.2 + a / TAU) % 1.0) - 0.5)
    sp = np.clip(1 - spiral / 0.11, 0, 1) * (r < 0.5) * 0.8 * smooth(r, 0.0, 0.1)
    lum = np.clip(L + scal * 0.85 + sp, 0, 1) * smooth(1 - r, 0.0, 0.08)
    save("noelle_sigil_mer", lum)


def sigil_drg():
    w = 256
    px, py, r, a = sigil_base(w)
    L = ringline(r, 0.94, 0.02) * 0.95 + ringline(r, 0.80, 0.008) * 0.6 + ringline(r, 0.5, 0.012) * 0.8
    scale = np.zeros_like(r)
    band = (r > 0.54) & (r < 0.78)
    for k in range(36):                                                                         # a ring of dragon scales
        ang = k * TAU / 36
        cx, cy = math.cos(ang) * 0.67, math.sin(ang) * 0.67
        d = np.hypot(px - cx, py - cy)
        scale = np.maximum(scale, ringline(d, 0.075, 0.012))
    claws = np.zeros_like(r)
    for k in range(4):                                                                          # four claw points
        ang = k * TAU / 4 + math.pi / 4
        along = r * np.cos(a - ang)
        d = np.abs(np.sin(a - ang)) * r
        claws = np.maximum(claws, np.clip(1 - d / (0.09 * (1 - along).clip(0.02, 1)), 0, 1) * (along > 0.1) * (along < 1.0))
    coil = np.clip(1 - np.abs(r - (0.28 + 0.07 * np.sin(a * 3))) / 0.025, 0, 1) * 0.8        # a coiled serpent in the middle
    lum = np.clip(L + scale * 0.7 * band + claws * 0.65 + coil, 0, 1) * smooth(1 - r, 0.0, 0.07)
    save("noelle_sigil_drg", lum)


def sigil_blk():
    w = 256
    px, py, r, a = sigil_base(w)
    L = ringline(r, 0.95, 0.012) * 0.7
    th = np.zeros_like(r)
    for k in range(18):                                                                         # a circle of thorns, alternately long and short
        ang = k * TAU / 18
        along = r * np.cos(a - ang)
        d = np.abs(np.sin(a - ang)) * r
        ln = 0.98 if k % 2 == 0 else 0.84
        th = np.maximum(th, np.clip(1 - d / (0.04 * (1 - (along - 0.72).clip(0, 0.3) / 0.3)), 0, 1) * (along > 0.72) * (along < ln))
    broken = ringline(r, 0.62, 0.016) * (np.sin(a * 5) > -0.2) + ringline(r, 0.5, 0.01) * (np.sin(a * 7 + 1) > 0.1) * 0.8
    horn = np.zeros_like(r)
    for sgn in (-1, 1):                                                                         # two horns curving over the centre
        t = np.clip((py + 0.55) / 0.9, 0, 1)
        cx = sgn * (0.12 + 0.38 * t ** 1.4)
        horn = np.maximum(horn, np.clip(1 - np.abs(px - cx) / (0.05 * (1 - 0.7 * t) + 0.012), 0, 1) * (py > -0.55) * (py < 0.35))
    lum = np.clip(L + th * 0.9 + broken * 0.85 + horn * 0.7, 0, 1) * smooth(1 - r, 0.0, 0.06)
    save("noelle_sigil_blk", lum)


# ---------------------------------------------------------------------------------------------------------------------- small sprites
def sprites():
    w = 64
    px, py, r, a = sigil_base(w)
    # a feather glint: a slanting leaf with a quill
    u, v, x, y = grid(w, w)
    ang = math.radians(35)
    qx, qy = (px * math.cos(ang) + py * math.sin(ang)), (-px * math.sin(ang) + py * math.cos(ang))
    leaf = np.clip(1 - np.abs(qy) / (0.28 * np.clip(1 - np.abs(qx / 0.9) ** 2, 0.0, 1)).clip(0.02, 1), 0, 1) * (np.abs(qx) < 0.9)
    quill = np.clip(1 - np.abs(qy) / 0.03, 0, 1) * (np.abs(qx) < 0.9)
    save("noelle_feather", np.clip(leaf * 0.55 + quill * 0.9, 0, 1) * smooth(1 - r, 0.0, 0.1))
    # an ember / flame mote: a teardrop, hot core
    tdrop = np.clip(1 - np.hypot(px / (0.34 * (1.2 - py * 0.6).clip(0.15, 1.6)), (py + 0.1) / 0.75), 0, 1)
    save("noelle_ember", np.clip(tdrop ** 0.7 * 1.1, 0, 1) * smooth(1 - r, 0.0, 0.12))
    # a bubble: a ring with two highlights
    bub = ringline(r, 0.62, 0.07) * 0.8 + np.clip(1 - np.hypot(px + 0.28, py + 0.3) / 0.14, 0, 1) + np.clip(1 - np.hypot(px - 0.3, py - 0.32) / 0.07, 0, 1) * 0.6 + (r < 0.62) * 0.07
    save("noelle_bubble", np.clip(bub, 0, 1) * smooth(1 - r, 0.0, 0.08))
    # a four-point spark
    spark = np.clip(1 - np.minimum(np.abs(px), np.abs(py)) / 0.07, 0, 1) * np.clip(1 - r, 0, 1) ** 1.2 + np.clip(1 - r / 0.28, 0, 1) * 0.9
    save("noelle_spark", np.clip(spark, 0, 1))
    # an eye flare: a bright almond, a streak to both sides
    w2, h2 = 128, 64
    u, v, x, y = grid(w2, h2)
    ex, ey = (u - 0.5) * 2, (v - 0.5) * 2
    almond = np.clip(1 - np.abs(ey) / (0.26 * np.clip(1 - np.abs(ex / 0.45) ** 2, 0, 1) + 0.001), 0, 1) * (np.abs(ex) < 0.45)
    streak = np.exp(-(ey / 0.07) ** 2) * np.clip(1 - np.abs(ex), 0, 1) ** 1.5
    halo = np.exp(-(np.hypot(ex / 0.7, ey / 0.55) / 0.55) ** 2) * 0.35
    save("noelle_eye", np.clip(almond * 1.0 + streak * 0.8 + halo, 0, 1) * smooth(1 - np.hypot(ex, ey), 0.0, 0.1))
    # a soft glow dot
    save("noelle_dot", np.clip(1 - r, 0, 1) ** 2.2)


# ---------------------------------------------------------------------------------------------------------------------- skin overlays
def shells():
    shell_texture("noelle_val_shell", Mat("water", "5CB0F0", dark="2A70D0", light="F0FCFF", glow="B4F4FF", glow_amount=0.8, alpha=120, seed=61))
    shell_texture("noelle_mer_shell", Mat("water", "4CC4E8", dark="1E88C8", light="E8FEFF", glow="9CF8FF", glow_amount=0.9, alpha=135, seed=62))
    shell_texture("noelle_drg_shell", Mat("scales", "2A6CB8", dark="0A2450", light="7CC8F0", glow="46E4FF", glow_amount=0.8, alpha=170, cells=5, seed=63))
    shell_texture("noelle_blk_shell", Mat("wisp", "0A0E18", dark="020308", light="1C2A48", glow="2C7CFF", glow_amount=0.95, alpha=185, seed=64))
    shell_texture("noelle_haze", Mat("water", "9CD8FF", dark="5CA8F0", light="FFFFFF", glow="E8FCFF", glow_amount=1.0, alpha=255, seed=65))


def main():
    ring()
    ribbon()
    sigil_val()
    sigil_mer()
    sigil_drg()
    sigil_blk()
    sprites()
    shells()
    print("wrote", OUT)


if __name__ == "__main__":
    main()
