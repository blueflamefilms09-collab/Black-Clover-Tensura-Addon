"""Textures for the 0.42 Mirror Magic overhaul (Gauche Adlai): ornate antique mirror frames and their glass, a mana corona,
a light-distortion ripple and a prismatic streak. Deterministic, no fonts, no external images.

    python tools/gen_mirror_vfx_textures.py  ->  src/main/resources/assets/nusmp/textures/particle/mirror_*.png

Every frame shape (oval, arch, round, crest, diamond) has a pair:
    mirror_frame_<shape>  silver frame with bevel shading, scrollwork and a crest at the top; transparent where the glass is
    mirror_glass_<shape>  the glass only: a cool sheen gradient and a soft vignette, alpha ~0.8 (the VFX tints it violet)
Convention (as the rest of textures/particle): greyscale + alpha, tinted per quad by the vertex colour.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey, save  # noqa: E402

S = 256          # texture size
SS = 4           # supersampling


def shape_mask(shape, inset):
    """The mirror outline (1 inside) at S x S, shrunk by 'inset' (fraction of S)."""
    big = S * SS
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)
    m = inset * big
    if shape == "oval":
        d.ellipse((big * 0.18 + m, big * 0.04 + m, big * 0.82 - m, big * 0.96 - m), fill=255)
    elif shape == "round":
        d.ellipse((big * 0.06 + m, big * 0.06 + m, big * 0.94 - m, big * 0.94 - m), fill=255)
    elif shape == "arch":           # an arched rectangle: square bottom, round top
        x0, x1, y0, y1 = big * 0.16 + m, big * 0.84 - m, big * 0.04 + m, big * 0.96 - m
        r = (x1 - x0) / 2
        d.rectangle((x0, y0 + r, x1, y1), fill=255)
        d.ellipse((x0, y0, x1, y0 + 2 * r), fill=255)
    elif shape == "crest":          # a gothic crest: pointed arch top, straight sides, a point at the bottom
        x0, x1, y0, y1 = big * 0.16 + m, big * 0.84 - m, big * 0.04 + m, big * 0.96 - m
        cx = (x0 + x1) / 2
        pts = [(cx, y0)]
        for k in range(1, 21):          # the ogee curve down to the right shoulder
            t = k / 20
            pts.append((cx + (x1 - cx) * math.sin(t * math.pi / 2), y0 + (y1 - y0) * 0.28 * (1 - math.cos(t * math.pi / 2))))
        pts += [(x1, y0 + (y1 - y0) * 0.72), (cx, y1), (x0, y0 + (y1 - y0) * 0.72)]
        for k in range(20, 0, -1):
            t = k / 20
            pts.append((cx - (cx - x0) * math.sin(t * math.pi / 2), y0 + (y1 - y0) * 0.28 * (1 - math.cos(t * math.pi / 2))))
        d.polygon(pts, fill=255)
    elif shape == "diamond":
        cx, cy = big / 2, big / 2
        h, w = big * 0.46 - m, big * 0.34 - m
        d.polygon([(cx, cy - h), (cx + w, cy), (cx, cy + h), (cx - w, cy)], fill=255)
    return np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255


def frame(shape):
    outer, inner = shape_mask(shape, 0.0), shape_mask(shape, 0.08)
    band = np.clip(outer - inner, 0, 1)
    # bevel: distance-ish shading across the band (bright outer lip, dark groove, bright inner lip)
    mid = shape_mask(shape, 0.04)
    lip_out = np.clip(outer - mid, 0, 1)
    shade = 0.62 + 0.38 * lip_out + 0.25 * np.clip(mid - inner, 0, 1)
    # scrollwork: little round beads along the band, and a chased pattern
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    ang = np.arctan2(yy - S / 2, xx - S / 2)
    beads = (np.sin(ang * 36) * 0.5 + 0.5) ** 4 * np.clip(mid - inner, 0, 1)
    chase = fbm(S, S, 6, 91) * 0.25
    g = np.clip(shade + beads * 0.35 + chase - 0.1, 0, 1)
    a = band.copy()
    # an ornament crest at the top: a scrolling leaf flourish
    big = S * SS
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)
    top = {"oval": 0.04, "round": 0.06, "arch": 0.04, "crest": 0.04, "diamond": 0.04}[shape] * big
    cx = big / 2
    d.ellipse((cx - big * 0.06, top - big * 0.05, cx + big * 0.06, top + big * 0.05), fill=255)
    for sgn in (-1, 1):
        d.arc((cx + sgn * big * 0.02 - big * 0.12, top - big * 0.06, cx + sgn * big * 0.02 + big * 0.12, top + big * 0.1),
              200 if sgn < 0 else -20, 340 if sgn < 0 else 160, fill=255, width=int(big * 0.022))
        d.ellipse((cx + sgn * big * 0.15 - big * 0.02, top - big * 0.01, cx + sgn * big * 0.15 + big * 0.02, top + big * 0.03), fill=255)
    orn = np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255
    a = np.clip(a + orn, 0, 1)
    g = np.where(orn > band, 0.75 + 0.25 * fbm(S, S, 4, 93), g)
    save(grey(a, g), "mirror_frame_" + shape)
    # the glass: a cool sheen (a bright diagonal band, darker toward the rim)
    glass = inner
    diag = (xx + yy * 0.6) / (S * 1.6)
    sheen = np.exp(-((diag - 0.42) / 0.07) ** 2) * 0.55 + np.exp(-((diag - 0.55) / 0.025) ** 2) * 0.35
    vign = np.clip(Image.fromarray((inner * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(14)), 0, 255)
    vign = np.asarray(vign).astype(np.float32) / 255
    gg = np.clip(0.55 + 0.3 * vign + sheen, 0, 1)
    save(grey(glass * (0.78 + 0.2 * sheen), gg), "mirror_glass_" + shape)


def corona(size=256):
    """A ring of flickering mana flames round an empty middle: the violet aura behind a summoned mirror."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2 + 0.5) / (size / 2), (yy - size / 2 + 0.5) / (size / 2)
    r = np.hypot(x, y)
    ang = np.arctan2(y, x)
    tongues = 0.5 + 0.5 * np.sin(ang * 13 + np.sin(ang * 5) * 2) * (fbm(size, size, 32, 71) * 0.8 + 0.2)
    reach = 0.62 + 0.3 * tongues
    a = np.clip((reach - r) / 0.22, 0, 1) * np.clip((r - 0.3) / 0.25, 0, 1)
    a = a ** 1.4 + np.clip(1 - np.abs(r - 0.58) / 0.06, 0, 1) * 0.4
    save(grey(np.clip(a, 0, 1)), "mirror_corona")


def ripple(size=128):
    """Concentric distortion ripples, brightest in a ring that the VFX expands (the shimmer across the glass)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.hypot(xx - size / 2 + 0.5, yy - size / 2 + 0.5) / (size / 2)
    a = (np.sin(r * 30) * 0.5 + 0.5) ** 3 * np.clip(1 - r, 0, 1) * np.clip(r * 4, 0, 1)
    save(grey(a * 0.9), "mirror_ripple")


def prism(w=64, h=256):
    """A tall light streak with soft edges and fine scanlines (prismatic refraction: tinted red -> violet across copies)."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (xx + 0.5) / w, (yy + 0.5) / h
    core = np.exp(-((u - 0.5) / 0.18) ** 2)
    lines = 0.8 + 0.2 * np.sin(v * 160)
    fade = np.clip(v * 4, 0, 1) * np.clip((1 - v) * 4, 0, 1)
    save(grey(core * lines * fade), "mirror_prism")


def main():
    for s in ("oval", "arch", "round", "crest", "diamond"):
        frame(s)
    corona()
    ripple()
    prism()


if __name__ == "__main__":
    main()
