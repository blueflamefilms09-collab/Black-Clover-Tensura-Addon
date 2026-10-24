"""
The Mana Method rune through the game's OWN render path: tools/vfx_preview/preview.py compiles the real vfx layer (vfx/client/layer/RuneWritingLayer.java) with the real vertex buffer
(patched only to drain its quads instead of drawing them, and to take the 1,200 vertex budget of this layer) and the real textures (assets/nusmp/textures/particle/rune_*.png), runs the
layer's render() for the ages asked, and rasterises the quads the layer emitted with their blend, vertex colours and textures. This script drives it for the scenes of the owner's review and
puts the result next to the reference frame. What the headless tool cannot draw is the open grimoire (the mod's item model, drawn by client.manamethod.RuneClient): only its gold glow shows.

    python tools/mana_method/preview_ingame.py     # writes docs/previews/mana_method/rune_ingame.png, rune_glyph_rings.png, rune_glyph_charge.png, rune_glyph_lod.png

Needs JDK 21, Python 3 with numpy + Pillow, JOML (tools/vfx_preview/lib/joml-1.10.5.jar or $JOML).
"""
import importlib.util
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "docs", "previews", "mana_method")
REFERENCE = os.environ.get("MM_REFERENCE") or os.path.join(HERE, "reference_frame.png")

spec = importlib.util.spec_from_file_location("vfxpreview", os.path.join(ROOT, "tools", "vfx_preview", "preview.py"))
pv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pv)
pv.W, pv.H = 576, 347
pv.FOV = 94.0

CASTER = np.array([0.0, 70.0, 0.0])
LOOK = np.array([0.0, -0.05, 1.0]) / np.linalg.norm([0.0, -0.05, 1.0])
CENTRE = CASTER + LOOK * 2.6
SHOULDER = [0.30, 70.55, -0.35]
TARGET = list(CENTRE + np.array([0.0, -0.1, 0.0]))


def scene(seed=7, detail=1.0, cam=None, target=None, charge=18, ages=(18,), frm=None):
    return {"layer": "RuneWritingLayer", "shape": "RUNE_WRITING", "from": list(CENTRE if frm is None else frm), "to": list((CENTRE if frm is None else np.array(frm)) + LOOK), "power": 1.0,
            "duration": charge + 10, "seed": seed, "budget": 1200, "detail": detail, "cam": cam or SHOULDER, "target": target or TARGET, "ages": list(ages),
            "figures": [[0, 68.38, 0, 1.8]], "ground": 68.38, "sky": [0.14, 0.18, 0.2], "horizon": [0.22, 0.26, 0.28]}


def frames(classes, name, sc, size=None, figures=True):
    data = pv.run_java(sc, classes, os.path.join(pv.BUILD, name + ".json"))
    out = []
    for fr in data["frames"]:
        img = pv.background(sc, sc["cam"], sc["target"])
        if figures and sc.get("figures"):
            img = pv.draw_figures(img, sc, sc["cam"], sc["target"])
        img = pv.raster(img, fr["quads"], sc["cam"], sc["target"])
        im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        out.append((im, fr))
    return out


def label(im, text, right=None):
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, im.width, 15], fill=(0, 0, 0))
    d.text((4, 2), text, fill=(235, 235, 255))
    if right:
        d.text((im.width - 190, 2), right, fill=(255, 230, 150))
    return im


def sheet(images, path, title, cols, pad=4):
    w, h = max(i.size[0] for i in images), max(i.size[1] for i in images)
    rows = (len(images) + cols - 1) // cols
    out = Image.new("RGB", (cols * (w + pad) + pad, rows * (h + pad) + pad + 20), (6, 6, 12))
    ImageDraw.Draw(out).text((8, 4), title, fill=(255, 235, 170))
    for i, im in enumerate(images):
        out.paste(im, (pad + (i % cols) * (w + pad), 20 + pad + (i // cols) * (h + pad)))
    out.save(os.path.join(OUT, path))


def main():
    os.makedirs(pv.BUILD, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    classes = pv.build(["RuneWritingLayer"])
    ref = Image.open(REFERENCE).convert("RGB").resize((pv.W, pv.H)) if os.path.exists(REFERENCE) else Image.new("RGB", (pv.W, pv.H), (30, 30, 30))
    label(ref, "owner's reference frame")

    # the rune at the end of its charge, over the caster's shoulder (the reference's framing), and from the caster's eye
    main_fr = frames(classes, "main", scene(ages=(18,)))[0]
    pv.W, pv.H = 420, 420
    eye_fr = frames(classes, "eye", scene(cam=list(CASTER), target=list(CENTRE), ages=(18,)), figures=False)[0]
    pv.W, pv.H = 576, 347
    stat = lambda fr: "%d quads %d verts" % (len(fr["quads"]), fr["vertices"])
    ingame = label(main_fr[0], "in-game path, True Flame, tick 18 (book: glow only)", stat(main_fr[1]))
    eye = label(eye_fr[0], "caster's eye, face on", stat(eye_fr[1]))
    sheet([ref, ingame], "rune_ingame.png", "The written rune through the game's own render path (real layer, real vertex buffer, real textures) next to the owner's reference frame", 2)
    sheet([ref, ingame, eye], "rune_glyph_rings.png", "Written rune symbols: the reference, the rune over the caster's shoulder (the reference's framing) and from the caster's eye", 3)

    # the charge
    ages = (1, 3, 5, 7, 9, 11, 13, 15, 18, 21)
    strip = []
    for im, fr in frames(classes, "charge", scene(ages=ages)):
        a = ages[len(strip)]
        names = {3: "tick 3: hand ring", 7: "tick 7: middle ring", 11: "tick 11: outer ring", 13: "tick 13: floating, arcs", 18: "tick 18: written", 21: "release: outer ring flares"}
        strip.append(label(im, names.get(a, "tick %d" % a), "%d verts" % fr["vertices"]).resize((pv.W // 2, pv.H // 2), Image.LANCZOS))
    sheet(strip, "rune_glyph_charge.png", "Charge (18 ticks), through the game's render path: glyph after glyph is written, three rings at once; the lines and the arcs come in last", 5)

    # the levels of detail and the six elements
    lod = []
    for det in (1.0, 0.75, 0.5, 0.2):
        im, fr = frames(classes, "lod", scene(detail=det, ages=(19,)))[0]
        lod.append(label(im, "detail %.2f" % det, "%d verts" % fr["vertices"]).resize((pv.W // 2, pv.H // 2), Image.LANCZOS))
    for name, seed in (("flame", 0 << 3 | 1), ("water", 1 << 3 | 1), ("wind", 2 << 3 | 1), ("earth", 3 << 3 | 1), ("lightning", 4 << 3 | 1 | 4), ("plant", 5 << 3 | 1)):     # packed RuneSpec: modifiers, True Magic bit, element
        im, fr = frames(classes, "el", scene(seed=seed, ages=(19,)))[0]
        lod.append(label(im, name + " (its own words)", "%d verts" % fr["vertices"]).resize((pv.W // 2, pv.H // 2), Image.LANCZOS))
    sheet(lod, "rune_glyph_lod.png", "Levels of detail (full, medium, low, minimal) and the six elements, each writing its own words", 5)
    print("wrote", OUT, "| full rune at tick 18:", stat(main_fr[1]))


if __name__ == "__main__":
    sys.exit(main())
