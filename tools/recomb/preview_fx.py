"""Preview of the Recombination anime look's particles (vfx/client/RecombAnimeFx.java) without the game: a side view of the fist's landing (the orange flash with spark lines, the dust ring, thrown
bricks) and of a footfall (the dust ring), at several ticks, drawn from the REAL particle sprites (textures/particle/<id>_<n>.png) and the REAL physics profiles of data/nusmp/particle_catalog.json
(gravity, drag, size curve, fade, frames, blend). It re-implements the spawn code of RecombAnimeFx (same counts, speeds and sizes) and the per-tick update of FxParticle, so it shows the layout and the
timing of the effect, not the game's exact pixels (no camera perspective, no lighting, no random jitter beyond a fixed seed).

    python3 -B tools/recomb/preview_fx.py            # writes docs/previews/recomb_anime/fx_impact.png and fx_footfall.png
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
CAT = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "particle_catalog.json")
OUT = os.path.join(ROOT, "docs", "previews", "recomb_anime")

with open(CAT) as f:
    CATALOG = {p["id"]: p for p in json.load(f)["particles"]}

DUST, DUST_DARK, FLASH, FLASH_CORE, SMOKE = 0xB9A27C, 0x8C7A5E, 0xFF8A2A, 0xFFE9A0, 0xC9C2B8
PPB = 92                        # pixels per block in the preview


def size_at(curve, start, end, t):
    t = max(0.0, min(1.0, t))
    lerp = lambda a, b, u: a + (b - a) * u
    if curve == "pop":
        if t < 0.2:
            return start * (0.35 + 0.8 * (t / 0.2))
        return lerp(start * 1.15, end, (t - 0.2) / 0.8)
    if curve == "ease_out":
        return lerp(start, end, 1 - (1 - t) * (1 - t))
    if curve == "pulse":
        return lerp(start, end, t) * (1 + 0.18 * math.sin(t * 6.2831855 * 2))
    if curve == "hold":
        return start if t < 0.75 else lerp(start, end, (t - 0.75) / 0.25)
    return lerp(start, end, t)


def alpha_at(fade, t):
    a = 1.0
    if fade[0] > 0 and t < fade[0]:
        a = min(a, t / fade[0])
    if fade[1] > 0 and t > 1 - fade[1]:
        a = min(a, (1 - t) / fade[1])
    return max(0.0, min(1.0, a))


class P:
    def __init__(self, pid, rgb, scale, pos, vel, rng):
        d = CATALOG[pid]
        self.id, self.d, self.prof = pid, d, d["profile"]
        self.tint = (rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255) if d["tint"] and rgb >= 0 else None
        self.scale = scale
        self.pos, self.vel = list(pos), list(vel)
        lo, hi = self.prof["life"]
        self.life = lo + int(rng.integers(0, hi - lo + 1))
        self.jit = 0.88 + rng.random() * 0.24
        self.phase = int(rng.integers(0, 1000))
        self.age = 0
        self.spin = self.prof["spin"]
        self.rot = float(rng.random() * 6.28)
        self.frames = [Image.open(os.path.join(TEX, "%s_%d.png" % (pid, i))).convert("RGBA") for i in range(d["frames"])]

    def step(self):
        p = self.prof
        self.age += 1
        self.vel[1] -= 0.04 * p["gravity"]
        for i in range(3):
            self.pos[i] += self.vel[i]
        if p.get("collide") and self.pos[1] < 0:
            self.pos[1] = 0
            self.vel[1] = abs(self.vel[1]) * 0.3
            self.vel[0] *= 0.6
            self.vel[2] *= 0.6
        for i in range(3):
            self.vel[i] *= p["drag"]
        self.rot += self.spin

    def alive(self):
        return self.age < self.life

    def frame(self):
        n = len(self.frames)
        if n <= 1:
            return self.frames[0]
        if self.prof["anim"] == "age":
            return self.frames[min(n - 1, self.age * n // max(1, self.life))]
        return self.frames[(self.age // max(1, self.prof["loop"]) + self.phase) % n]


def spawn_ring(ps, rng, x, y, z, radius, count, size):
    cloud, dark = ("smoke_wisp", DUST, 3.4 * size), ("soot_puff", DUST_DARK, 2.0 * size)
    sand, chip = ("sand_grain", DUST, 1.6 * size), ("earth_chip", DUST, 1.1 * size)
    for i in range(count):
        a = (i + rng.random() * 0.7) * (math.pi * 2 / count)
        c, s, sp = math.cos(a), math.sin(a), 0.06 + rng.random() * 0.1
        px, pz = x + c * radius, z + s * radius
        if i % 4 == 3:
            ps.append(P(dark[0], dark[1], dark[2], (px, y + 0.25, pz), (c * sp * 0.8, 0.05, s * sp * 0.8), rng))
        else:
            ps.append(P(cloud[0], cloud[1], cloud[2], (px, y + 0.2 + rng.random() * 0.3, pz), (c * sp, 0.04 + rng.random() * 0.05, s * sp), rng))
        if i % 2 == 0:
            ps.append(P(sand[0], sand[1], sand[2], (px, y + 0.3, pz), (c * sp * 1.6, 0.18 + rng.random() * 0.15, s * sp * 1.6), rng))
        if i % 5 == 0:
            ps.append(P(chip[0], chip[1], chip[2], (px, y + 0.3, pz), (c * sp * 1.2, 0.3 + rng.random() * 0.2, s * sp * 1.2), rng))


def spawn_impact(ps, rng, x, y, z, sc, sparks, dust, chips):
    my = y + 0.35 * sc
    ps.append(P("impact_burst", FLASH, 2.6 * sc, (x, my, z), (0, 0, 0), rng))
    ps.append(P("impact_burst", FLASH_CORE, 1.4 * sc, (x, my + 0.05, z), (0, 0, 0), rng))
    ps.append(P("shock_ring", FLASH, 2.4 * sc, (x, y + 0.2, z), (0, 0, 0), rng))
    for i in range(sparks):
        a = (i + rng.random() * 0.6) * (math.pi * 2 / max(1, sparks))
        sp = 0.42 + rng.random() * 0.3
        ps.append(P("spark", -1, 3.0 * sc, (x, my, z), (math.cos(a) * sp, 0.05 + rng.random() * 0.12, math.sin(a) * sp), rng))
        if i % 3 == 0:
            ps.append(P("ember", -1, 2.4 * sc, (x, my, z), (math.cos(a) * sp * 0.45, 0.12 + rng.random() * 0.12, math.sin(a) * sp * 0.45), rng))
        if i % 4 == 1:
            ps.append(P("flame_tongue", -1, 2.4 * sc, (x, y + 0.2, z), (math.cos(a) * sp * 0.25, 0.1 + rng.random() * 0.08, math.sin(a) * sp * 0.25), rng))
    spawn_ring(ps, rng, x, y, z, 0.8 * sc, dust, 1.7 * sc)
    for i in range(chips):
        a = rng.random() * math.pi * 2
        sp = 0.1 + rng.random() * 0.2
        ps.append(P("earth_chip", DUST, 2.4 * sc, (x, my, z), (math.cos(a) * sp, 0.35 + rng.random() * 0.3, math.sin(a) * sp), rng))


def draw(ps, ticks, ox, w, h, ground_px, title, extra=None):
    """One row of frames: each is a side view (x to the right, y up) with a one-block grid."""
    sheet = Image.new("RGB", (w * len(ticks), h), (0, 0, 0))
    for k, tk in enumerate(ticks):
        base = np.zeros((h, w, 3), np.float32)
        yy = np.linspace(0, 1, h)[:, None, None]
        base[:] = np.array([54, 62, 82]) * (1 - yy) + np.array([128, 138, 150]) * yy
        base[ground_px:, :] = np.array([88, 96, 84])
        canvas = Image.fromarray(base.astype(np.uint8))
        d = ImageDraw.Draw(canvas)
        for gx in range(0, w, PPB):
            d.line([(gx, ground_px), (gx, ground_px + 6)], fill=(150, 160, 150))
        if extra:
            extra(d, ground_px, ox)
        arr = np.asarray(canvas).astype(np.float32)
        order = sorted((p for p in ps[tk] if p.alive()), key=lambda p: p.pos[2])
        for p in order:
            t = p.age / p.life
            wblocks = size_at(p.prof["curve"], p.prof["size"][0], p.prof["size"][1], t) * p.scale * p.jit
            px = max(2, int(wblocks * PPB))
            spr = p.frame().resize((px, px), Image.BILINEAR)
            if p.spin:
                spr = spr.rotate(-math.degrees(p.rot), expand=True)
            a = np.asarray(spr).astype(np.float32)
            al = a[..., 3:4] / 255.0 * alpha_at(p.prof["fade"], t)
            rgb = a[..., :3]
            if p.tint:
                rgb = rgb * (np.array(p.tint, np.float32) / 255.0)
            cx = int(ox + p.pos[0] * PPB)
            cy = int(ground_px - p.pos[1] * PPB)
            x0, y0 = cx - a.shape[1] // 2, cy - a.shape[0] // 2
            xa, ya, xb, yb = max(0, x0), max(0, y0), min(w, x0 + a.shape[1]), min(h, y0 + a.shape[0])
            if xb <= xa or yb <= ya:
                continue
            sub = arr[ya:yb, xa:xb]
            s_rgb, s_al = rgb[ya - y0:yb - y0, xa - x0:xb - x0], al[ya - y0:yb - y0, xa - x0:xb - x0]
            if p.prof["blend"] == "add":
                sub += s_rgb * s_al
            else:
                sub[:] = sub * (1 - s_al) + s_rgb * s_al
            arr[ya:yb, xa:xb] = np.clip(sub, 0, 255)
        im = Image.fromarray(arr.astype(np.uint8))
        dd = ImageDraw.Draw(im)
        dd.text((8, 6), "%s  tick %d" % (title, tk), fill=(255, 255, 255))
        sheet.paste(im, (k * w, 0))
    return sheet


def simulate(ticks, spawn):
    rng = np.random.default_rng(7)
    live, frames = [], {}
    spawn(live, rng)
    last = max(ticks)
    for t in range(0, last + 1):
        if t in ticks:
            frames[t] = [p for p in live]
            # a snapshot of positions and ages (the objects change later), copied
            import copy
            frames[t] = [copy.copy(p) for p in live]
            for q, p in zip(frames[t], live):
                q.pos, q.vel = list(p.pos), list(p.vel)
        for p in live:
            if p.alive():
                p.step()
    return frames


def main():
    os.makedirs(OUT, exist_ok=True)
    sys.path.insert(0, HERE)
    sc = 0.84
    ticks = [1, 3, 6, 10, 16, 26]
    W, H, G = 520, 460, 380

    def fist(d, g, ox):                                           # a block for the brick fist and the ground line
        d.rectangle([ox - 4.2 * PPB * 0.35 - 20, g - 30, ox - 4, g - 2], fill=(160, 78, 52), outline=(90, 40, 28))
        d.text((ox - 130, g - 54), "fist", fill=(230, 230, 230))
        d.line([(ox + 2 * PPB, g + 14), (ox + 3 * PPB, g + 14)], fill=(255, 255, 255))
        d.text((ox + 2 * PPB, g + 18), "1 block", fill=(255, 255, 255))

    f = simulate(ticks, lambda ps, rng: spawn_impact(ps, rng, 0.0, 0.1 * sc, 0.0, sc, 22, 14, 6))
    draw(f, ticks, 220, W, H, G, "fist lands", fist).save(os.path.join(OUT, "fx_impact.png"))
    f2 = simulate(ticks, lambda ps, rng: spawn_ring(ps, rng, 0.0, 0.0, 0.0, 0.6 * sc, 8, 1.5 * sc))
    draw(f2, ticks, 220, W, H, G, "footfall", None).save(os.path.join(OUT, "fx_footfall.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
