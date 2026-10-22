#!/usr/bin/env python3
"""1.110.0 safe-additive texture scan (docs/visual_upgrade/safe_additive.md).

Scans every texture an additive / emissive pass draws (the SCOPE below) and reports the defects of the white-square bug:
  * COLOUR_UNDER_ALPHA : a fully transparent pixel (alpha 0) that carries a non-black colour;
  * SOLID_RECT         : an opaque near-white axis-aligned rectangle (a solid card or background) in a card sheet;
  * CARD_BORDER        : a card-sheet cell whose border is not transparent (a hard card edge).
Run with --fix to clean them:  python3 -B tools/safe_additive/scan_textures.py --fix
  * colour under alpha 0 is zeroed (alpha-0 pixels add nothing in any blend, so the visible result is unchanged);
  * a card cell border (halo sheets) gets a smooth edge window on the ALPHA only (the interior is untouched), the same window vu_halo.build applies.
The Time Magic textures (time_*) are never touched. Idempotent. Needs: pillow numpy.
"""
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.abspath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp", "textures"))

# (directory below textures/, substrings of the file name that make it an additive texture; "" = every png)
SCOPE = [
    ("entity/weapon_vu", ("",)),
    ("visual_upgrade", ("vu_fx", "halo", "glow", "veins", "nebula", "star")),
    ("entity/weapon", ("_glow", "veins", "halo", "nebula", "star")),
    ("entity/weapon/up", ("_glow", "veins", "halo", "nebula", "star")),
    ("entity/djinn", ("glow", "rune", "nebula", "star", "halo")),
    ("relic_worn", ("glow", "nebula", "star", "halo", "atlas", "shimmer", "fx")),
    ("aura", ("glow",)),
    ("entity/scalecast", ("glow",)),                    # Scale casting (docs/magic/scale_casting.md): the runes, the halo and the ring
    ("entity", ("firecast_salamander_fx", "firecast_dragon_fx")),          # fire casting: the glow-only models of the two Salamander forms (eyes, mouth, licks of flame)
    ("entity", ("sylph_small_fx", "sylph_powered_fx", "sylph_halo", "sylph_small_wings_glow", "sylph_powered_wings_glow")),          # the Sylph: the glow-only models, the halo and the glow maps of the wings
    ("entity/spirit_space", ("",)),                      # Spirit of Space: the starfield layers, the fins, the halo, the portal (docs/magic/spirit_space.md)
    ("entity", ("spirit_space_orb", "spirit_space_wisp_fx")),          # Spirit of Space: the additive rift ring of the orb (and its levels) and the glow-only model of the wisp's head
    ("entity", ("firecast_salamander_fx", "firecast_dragon_fx")),
    ("entity", ("undine_fx", "undine_ring")),          # the Undine: the soft additive foam pass and the ripple ring (docs/magic/undine.md)          # fire casting: the glow-only models of the two Salamander forms (eyes, mouth, licks of flame)
    ("entity", ("spiritlight_cub_fx", "spiritlight_seraph_fx")),          # Spirit of Light: the glow-only models of the cub and the seraph (halo, eyes, rings, core, prism)
    ("particle", ("spiritlight_",)),                                       # Spirit of Light: the soft beam, ring, glow and spark textures

    ("entity", ("spirit_dark_companion_fx", "spirit_dark_powered_fx", "spirit_dark_companion_faceglow", "spirit_dark_powered_faceglow")),          # Spirit of Darkness: the glow-only models and the face glow sheets (eyes, grin, runes, seams, smoke wisps)

    ("entity", ("gnome_companion_fx", "gnome_full_fx", "gnome_faces_fx")),                  # the Gnome (docs/magic/gnome.md): the glow-only models (eyes, magic circle, rune cuffs, crystal veins)

]
HALO_SHEETS = {"entity/weapon_vu/halo.png": 128}      # path -> cell size (a card sheet: every cell must fade to alpha 0 at its border)
TIME_PREFIX = "time_"
BORDER_BAND = 6                                       # px of a cell over which the edge window fades
BORDER_MAX_ALPHA = 6                                  # a card border may not be brighter than this
WHITE_MIN = 235
OPAQUE_MIN = 200


def files():
    seen = set()
    for d, keys in SCOPE:
        root = os.path.join(TEX, d)
        if not os.path.isdir(root):
            continue
        for f in sorted(os.listdir(root)):
            if not f.endswith(".png") or f.startswith(TIME_PREFIX):
                continue
            if not any(k in f for k in keys):
                continue
            rel = d + "/" + f
            if rel not in seen:
                seen.add(rel)
                yield rel


def edge_window_1d(n, band=BORDER_BAND):
    """0 at the edge pixel, smoothstep up to 1 at `band` px in."""
    i = np.arange(n, dtype=np.float32)
    d = np.minimum(i, n - 1 - i)
    t = np.clip(d / float(band), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def window2d(size, band=BORDER_BAND):
    w = edge_window_1d(size, band)
    return np.minimum.outer(w, w).astype(np.float32)


def has_solid_rect(a, side):
    """True when an opaque near-white side x side square exists (integral image over the mask)."""
    m = ((a[..., 3] >= OPAQUE_MIN) & (a[..., :3].min(-1) >= WHITE_MIN)).astype(np.int64)
    if m.sum() < side * side:
        return False
    ii = np.zeros((m.shape[0] + 1, m.shape[1] + 1), np.int64)
    ii[1:, 1:] = m.cumsum(0).cumsum(1)
    s = ii[side:, side:] - ii[:-side, side:] - ii[side:, :-side] + ii[:-side, :-side]
    return bool((s == side * side).any())


def solid_cell(sub):
    """A card cell that is mostly an opaque near-white block (35 percent or more of its area): a flat square, not a glow shape."""
    m = (sub[..., 3] >= OPAQUE_MIN) & (sub[..., :3].min(-1) >= WHITE_MIN)
    return bool(m.mean() >= 0.35)


def border_bad(a, cell):
    h, w = a.shape[:2]
    for cy in range(0, h, cell):
        for cx in range(0, w, cell):
            c = a[cy:cy + cell, cx:cx + cell, 3]
            if max(c[0].max(), c[-1].max(), c[:, 0].max(), c[:, -1].max()) > BORDER_MAX_ALPHA:
                return True
    return False


def scan(rel):
    im = Image.open(os.path.join(TEX, rel))
    a = np.asarray(im.convert("RGBA")).copy()
    out = []
    if ((a[..., 3] == 0) & (a[..., :3].max(-1) > 0)).any():
        out.append("COLOUR_UNDER_ALPHA")
    cell = HALO_SHEETS.get(rel)
    if cell:
        if border_bad(a, cell):
            out.append("CARD_BORDER")
        if any(solid_cell(a[y:y + cell, x:x + cell]) for y in range(0, a.shape[0], cell) for x in range(0, a.shape[1], cell)):
            out.append("SOLID_RECT")
    return out, a


def fix(rel, a, problems):
    cell = HALO_SHEETS.get(rel)
    if cell and ("CARD_BORDER" in problems or "SOLID_RECT" in problems):
        w = window2d(cell)
        for cy in range(0, a.shape[0], cell):
            for cx in range(0, a.shape[1], cell):
                blk = a[cy:cy + cell, cx:cx + cell, 3].astype(np.float32)
                sub = a[cy:cy + cell, cx:cx + cell]
                if solid_cell(sub):                       # a solid card (halo cell "solid"): a soft radial falloff instead of a flat square
                    yy, xx = np.mgrid[0:cell, 0:cell].astype(np.float32)
                    r = np.hypot(xx - (cell - 1) / 2, yy - (cell - 1) / 2) / (cell / 2)
                    blk = blk * np.clip(1.0 - r, 0, 1) ** 0.7
                a[cy:cy + cell, cx:cx + cell, 3] = np.round(blk * w).astype(np.uint8)
    a[a[..., 3] == 0, :3] = 0
    Image.fromarray(a, "RGBA").save(os.path.join(TEX, rel), optimize=True)


def main():
    do_fix = "--fix" in sys.argv
    bad = 0
    for rel in files():
        problems, a = scan(rel)
        if not problems:
            continue
        bad += 1
        print(("FIXED " if do_fix else "BAD   ") + rel + " " + ",".join(problems))
        if do_fix:
            fix(rel, a, problems)
    print(("fixed " if do_fix else "offending: ") + str(bad))
    return 0 if (do_fix or bad == 0) else 1


if __name__ == "__main__":
    sys.exit(main())
