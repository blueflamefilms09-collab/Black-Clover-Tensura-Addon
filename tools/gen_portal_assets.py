"""
Generates the textures of the 3D spatial portal (src/main/resources/assets/nusmp/textures/particle/portal_*.png).

Why: the gate is drawn from polar strips (u = angle round the gate, v = distance from the middle / across a band) so the same texture
works for the shader path (it adds a per-pixel swirl on top) and for the plain translucent fallback. All textures are near-white
with the shape in the alpha channel: the vertex colour carries the per-gate Spatial palette (0xB088FF). u wraps seamlessly.

    python tools/gen_portal_assets.py
Needs numpy and Pillow.
"""
import os
import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
TAU = np.pi * 2


def save(name, rgb, a):
    img = np.dstack([np.clip(rgb, 0, 1), np.clip(a, 0, 1)])
    Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, name))


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def swirl():
    """256 x 128: spiral arms in polar space (a straight diagonal there), with fine seamless filaments. Row 0 is the middle."""
    W, H = 256, 128
    u = (np.arange(W) + 0.5) / W
    v = (np.arange(H) + 0.5) / H
    U, V = np.meshgrid(u, v)
    rng = np.random.default_rng(7)
    arms = 3
    twist = 1.25
    phase = TAU * (arms * U + twist * V)
    main = 0.5 + 0.5 * np.cos(phase)
    main = main ** 3.0                                           # thin bright arms with dark gaps
    # a second, finer set of arms twisting the other way gives the filament look
    fine = 0.5 + 0.5 * np.cos(TAU * (9 * U - 2.2 * V) + 0.8)
    fine = fine ** 6.0
    # seamless noise: sums of integer-frequency waves in u
    noise = np.zeros_like(U)
    for k in range(1, 7):
        noise += rng.uniform(0.4, 1.0) * np.cos(TAU * (k * 2 * U + rng.uniform(0, 1)) + 3 * V * k) / k
    noise = 0.5 + 0.5 * noise / 1.6
    body = np.clip(0.75 * main * (0.55 + 0.45 * noise) + 0.45 * fine * main + 0.12 * noise, 0, 1)
    radial = smooth(0.06, 0.38, V) * (1 - smooth(0.86, 1.0, V))      # empty core, soft edge
    a = body * radial
    rim = np.exp(-((V - 0.93) / 0.045) ** 2) * (0.35 + 0.65 * noise)  # a ring of light hugging the rim
    a = np.maximum(a, rim * 0.8)
    rgb = np.dstack([0.85 + 0.15 * a, 0.80 + 0.2 * a, np.ones_like(a)])
    save("portal_swirl.png", rgb, a)


def shell():
    """256 x 64: a tunnel ring seen across its width (v 0 = inner edge, 1 = outer edge): bright broken outer line, tick marks, soft inner glow."""
    W, H = 256, 64
    u = (np.arange(W) + 0.5) / W
    v = (np.arange(H) + 0.5) / H
    U, V = np.meshgrid(u, v)
    line = np.exp(-((V - 0.90) / 0.05) ** 2)
    dashes = smooth(0.15, 0.55, 0.5 + 0.5 * np.cos(TAU * (12 * U)))     # 12 gaps per tile (3 tiles round the circle)
    line = line * (0.25 + 0.75 * dashes)
    glow = np.exp(-((1 - V) / 0.45) ** 2) * 0.35
    ticks = (np.exp(-((V - 0.62) / 0.03) ** 2) * smooth(0.82, 0.98, 0.5 + 0.5 * np.cos(TAU * 24 * U))) * 0.7
    inner = np.exp(-((V - 0.12) / 0.04) ** 2) * 0.25
    a = np.clip(line + glow + ticks + inner, 0, 1) * smooth(0.0, 0.08, V)
    a = a * (1 - smooth(0.97, 1.0, V))
    rgb = np.dstack([0.88 + 0.12 * a, 0.84 + 0.16 * a, np.ones_like(a)])
    save("portal_shell.png", rgb, a)


def rim():
    """8 x 64: the soft band along the gate's edge, brightest in the middle of its width with a thin white core."""
    W, H = 8, 64
    v = (np.arange(H) + 0.5) / H
    V = np.tile(v[:, None], (1, W))
    a = np.exp(-((V - 0.5) / 0.20) ** 2) * 0.75 + np.exp(-((V - 0.5) / 0.045) ** 2) * 0.9
    save("portal_rim.png", np.dstack([np.ones_like(a), 0.96 * np.ones_like(a), np.ones_like(a)]), np.clip(a, 0, 1))


def core():
    """8 x 64: the dark backdrop, opaque in the middle and soft towards the rim (v 0 = middle)."""
    W, H = 8, 64
    v = (np.arange(H) + 0.5) / H
    V = np.tile(v[:, None], (1, W))
    a = 1 - smooth(0.55, 1.0, V)
    save("portal_core.png", np.ones((H, W, 3)), a)


def thread():
    """64 x 8: the thin thread between a pair: a hairline with beads of light moving along it (u along, v across)."""
    W, H = 64, 8
    u = (np.arange(W) + 0.5) / W
    v = (np.arange(H) + 0.5) / H
    U, V = np.meshgrid(u, v)
    across = np.exp(-((V - 0.5) / 0.22) ** 2)
    beads = 0.45 + 0.55 * (0.5 + 0.5 * np.cos(TAU * 2 * U)) ** 4
    a = across * beads
    save("portal_thread.png", np.dstack([np.ones_like(a), 0.95 * np.ones_like(a), np.ones_like(a)]), a)


def runes():
    """256 x 64: four runic plates side by side (one per cell), each a bracketed plate with a glyph of straight strokes in it."""
    S = 4                                           # supersampling
    W, H = 256 * S, 64 * S
    img = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(2024)
    cell = 64 * S
    for g in range(4):
        x0 = g * cell
        # plate: faint fill, long top and bottom rails (they join the neighbours), short end brackets
        d.rectangle([x0, 12 * S, x0 + cell, 52 * S], fill=40)
        d.rectangle([x0, 10 * S, x0 + cell, 13 * S], fill=255)
        d.rectangle([x0, 51 * S, x0 + cell, 54 * S], fill=255)
        d.rectangle([x0 + 2 * S, 10 * S, x0 + 5 * S, 54 * S], fill=200)
        d.rectangle([x0 + cell - 5 * S, 10 * S, x0 + cell - 2 * S, 54 * S], fill=200)
        # the glyph: a few strokes between points of a 4 x 5 grid
        pts = [(x0 + (14 + 12 * i) * S, (18 + 7 * j) * S) for i in range(4) for j in range(5)]
        n = int(rng.integers(4, 7))
        cur = int(rng.integers(0, len(pts)))
        for _ in range(n):
            nxt = int(rng.integers(0, len(pts)))
            if nxt == cur:
                continue
            d.line([pts[cur], pts[nxt]], fill=255, width=3 * S)
            r = 2 * S
            d.ellipse([pts[nxt][0] - r, pts[nxt][1] - r, pts[nxt][0] + r, pts[nxt][1] + r], fill=255)
            cur = nxt if rng.random() < 0.7 else int(rng.integers(0, len(pts)))
    img = img.resize((256, 64), Image.LANCZOS)
    a = np.asarray(img, dtype=np.float32) / 255.0
    # keep the plates from bleeding into the neighbour cell when filtered: a hair of margin on the u edges
    a[:, 0] *= 0.6
    a[:, 63::64] *= 0.6
    rgb = np.dstack([0.9 + 0.1 * a, 0.86 + 0.14 * a, np.ones_like(a)])
    save("portal_runes.png", rgb, a)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    swirl(); shell(); rim(); core(); thread(); runes()
    print("wrote portal_*.png to", os.path.abspath(OUT))
