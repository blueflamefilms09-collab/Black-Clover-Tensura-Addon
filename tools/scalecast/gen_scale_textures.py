"""Scale Magic casting (docs/magic/scale_casting.md): the textures of the one floating balance scale and of its suppression aura.

    python3 -B tools/scalecast/gen_scale_textures.py

Writes assets/nusmp/textures/entity/scalecast/{gold,enamel,chain,gem,glow_runes,glow_halo,glow_ring}.png deterministically (fixed seed, no clock).
The solid textures (gold, enamel, chain, gem) are drawn with the lighting of the game, so they carry colour and fine structure only. The additive textures (glow_*) have no colour
under a fully transparent texel (SafeAdditiveTextureScanTest). Palette: the mod's Scale colour (teal 63BDB5) against gold.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "entity", "scalecast")

GOLD = np.array([224, 178, 70], np.float32)
GOLD_HI = np.array([255, 232, 150], np.float32)
GOLD_LO = np.array([128, 88, 28], np.float32)
TEAL = np.array([99, 189, 181], np.float32)
TEAL_DK = np.array([30, 96, 100], np.float32)
TEAL_HI = np.array([180, 240, 228], np.float32)
RNG = np.random.default_rng(7919)


def save(name, rgba):
    arr = np.clip(rgba, 0, 255).astype(np.uint8)
    arr[arr[..., 3] == 0, :3] = 0                      # no colour under a fully transparent texel
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(arr, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, arr.shape[1], "x", arr.shape[0])


def mix(a, b, t):
    t = np.asarray(t, np.float32)[..., None]
    return a * (1 - t) + b * t


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w, (y + 0.5) / h


def gold(w=64, h=64):
    u, v = grid(w, h)
    # brushed along v (the way the metal is turned): per-column streaks, plus slow bands round the bar
    streak = RNG.random((1, w)).astype(np.float32)
    streak = np.repeat(streak, h, axis=0) * 0.5 + RNG.random((h, w)).astype(np.float32) * 0.5
    band = 0.5 + 0.5 * np.cos((u - 0.28) * 2 * math.pi)                      # a highlight line round the bar
    t = np.clip(0.30 + 0.45 * band + 0.18 * (streak - 0.5), 0, 1)
    col = mix(GOLD_LO, GOLD, np.clip(t * 1.4, 0, 1))
    col = mix(col, GOLD_HI, np.clip((t - 0.72) * 3.0, 0, 1))
    # engraved rings across the bar (dark grooves with a bright lip), at a few places along v
    for c, wdt in ((0.10, 0.012), (0.50, 0.016), (0.90, 0.012)):
        g = np.exp(-((v - c) / wdt) ** 2)
        col = mix(col, GOLD_LO * 0.75, g * 0.85)
        lip = np.exp(-((v - c - 0.03) / 0.008) ** 2)
        col = mix(col, GOLD_HI, lip * 0.5)
    a = np.full((h, w, 1), 255, np.float32)
    save("gold", np.concatenate([col, a], axis=2))


def enamel(w=64, h=64):
    u, v = grid(w, h)
    base = mix(TEAL_DK, TEAL, 0.35 + 0.65 * v)
    # fish scales: rows of overlapping rounds, each rim lighter, the cleft between them dark; gold line where rows meet
    cell = 8.0
    px, py = u * w, v * h
    row = np.floor(py / (cell * 0.5))
    off = np.where(row % 2 == 0, 0.0, cell * 0.5)
    cx = (np.floor((px + off) / cell) + 0.5) * cell - off
    cy = (row + 0.5) * cell * 0.5
    d = np.sqrt((px - cx) ** 2 + ((py - cy) * 1.15) ** 2) / (cell * 0.58)
    shade = np.clip(1.0 - d, 0, 1)
    col = mix(TEAL_DK * 0.8, TEAL, 0.25 + 0.75 * shade)
    col = mix(col, TEAL_HI, np.clip((0.35 - np.abs(d - 0.78)) * 1.2, 0, 1) * 0.35)
    col = mix(col, base, 0.15)
    line = np.exp(-((v * h % (cell * 0.5) - cell * 0.25) / 0.45) ** 2) * 0.0
    a = np.full((h, w, 1), 255, np.float32)
    save("enamel", np.concatenate([col + line[..., None], a], axis=2))


def chain(w=32, h=32):
    u, v = grid(w, h)
    n = RNG.random((h, w)).astype(np.float32)
    t = 0.45 + 0.35 * np.cos((u - 0.3) * 2 * math.pi) + 0.2 * (n - 0.5)
    col = mix(GOLD_LO * 0.9, GOLD * 0.95, np.clip(t, 0, 1))
    a = np.full((h, w, 1), 255, np.float32)
    save("chain", np.concatenate([col, a], axis=2))


def gem(w=32, h=32):
    # cut stone: a coarse cell pattern, each facet its own brightness, a pale highlight in a few; light (it is tinted in the game)
    u, v = grid(w, h)
    fx, fy = 5, 4
    cx, cy = np.floor(u * fx), np.floor(v * fy)
    rnd = np.array([[RNG.random() for _ in range(fx)] for _ in range(fy)], np.float32)
    f = rnd[cy.astype(int) % fy, cx.astype(int) % fx]
    t = 0.55 + 0.45 * f
    col = mix(np.array([150, 200, 196], np.float32), np.array([250, 255, 250], np.float32), t)
    edge = (np.abs(u * fx - np.round(u * fx)) < 0.05) | (np.abs(v * fy - np.round(v * fy)) < 0.06)
    col = np.where(edge[..., None], col * 0.72, col)
    a = np.full((h, w, 1), 255, np.float32)
    save("gem", np.concatenate([col, a], axis=2))


def glow_runes(w=64, h=128):
    u, v = grid(w, h)
    a = np.zeros((h, w), np.float32)
    for c, wd in ((0.045, 0.010), (0.085, 0.005), (0.915, 0.005), (0.955, 0.010), (0.44, 0.006), (0.56, 0.006), (0.50, 0.012)):
        a = np.maximum(a, np.exp(-((v - c) / wd) ** 2))
    # a row of diamonds along each side of the beam, and short tick marks between the swells
    for cu in (0.0, 0.25, 0.5, 0.75, 1.0):
        for cv in np.linspace(0.18, 0.40, 4).tolist() + np.linspace(0.60, 0.82, 4).tolist():
            d = np.abs((u - cu) * w / 2.5) + np.abs((v - cv) * h / 3.0)
            a = np.maximum(a, np.clip(1.0 - d, 0, 1))
    col = mix(GOLD_HI, TEAL_HI, np.clip(0.5 + 0.5 * np.sin(v * 40), 0, 1) * 0.35)
    rgba = np.concatenate([col, (a * 235)[..., None]], axis=2)
    save("glow_runes", rgba)


def glow_halo(w=256, h=256):
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    core = np.exp(-(r / 0.42) ** 2) * 0.55
    ring = np.exp(-((r - 0.62) / 0.030) ** 2) * 0.85 + np.exp(-((r - 0.70) / 0.012) ** 2) * 0.5
    spikes = np.zeros_like(r)
    for k in range(12):                                                             # twelve spokes, the long ones at the quarter points: a balance's cross
        ang = k * math.pi / 6
        long = 1.0 if k % 3 == 0 else 0.55
        dth = np.abs(((th - ang + math.pi) % (2 * math.pi)) - math.pi)
        spikes = np.maximum(spikes, np.exp(-(dth / 0.045) ** 2) * np.clip(1.0 - r / (0.98 * long), 0, 1) * (r > 0.12))
    a = np.clip(core + ring * 0.8 + spikes * 0.7, 0, 1)
    a *= np.clip((1.0 - r) / 0.12, 0, 1)                                             # fades to nothing at the border of the card
    col = mix(TEAL_HI, GOLD_HI, np.clip(r * 1.2, 0, 1))
    save("glow_halo", np.concatenate([col, (a * 255)[..., None]], axis=2))


def glow_ring(w=256, h=256):
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    a = np.exp(-((r - 0.955) / 0.018) ** 2) * 0.95                                   # the bright rim
    a = np.maximum(a, np.exp(-((r - 0.90) / 0.008) ** 2) * 0.55)
    # a band of scales inside the rim: arcs of circles round the ring
    n = 36
    cell = 2 * math.pi / n
    local = ((th + math.pi) % cell) / cell - 0.5
    arc = np.abs(np.sqrt(((r - 0.80 + 0.07 * (1 - 4 * local * local)) * 8) ** 2 + 0.0))
    scale = np.exp(-(arc / 0.5) ** 2) * (r > 0.66) * (r < 0.90)
    a = np.maximum(a, scale * 0.55)
    ticks = (np.abs(local) < 0.05) * (r > 0.955) * (r < 0.995) * 0.8
    a = np.maximum(a, ticks)
    a = np.maximum(a, np.exp(-(r / 0.62) ** 2) * 0.10)                               # a faint disc inside
    a *= np.clip((1.0 - r) / 0.03, 0, 1)
    col = mix(TEAL, TEAL_HI, np.clip(r, 0, 1) ** 2)
    save("glow_ring", np.concatenate([col, (a * 255)[..., None]], axis=2))


def main():
    gold()
    enamel()
    chain()
    gem()
    glow_runes()
    glow_halo()
    glow_ring()


if __name__ == "__main__":
    sys.exit(main())
