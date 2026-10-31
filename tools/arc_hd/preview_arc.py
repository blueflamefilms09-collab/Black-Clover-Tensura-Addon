#!/usr/bin/env python3
"""Renders the HD arc characters as characters (the player model with the skin, the kit hair at its colour and the extras: wings, weapons, horns), like a skin showcase.

    python3 -B tools/arc_hd/preview_arc.py out.png [--back | --close | --trio | --side] [--cols N] [--aa N] [--skin-dir DIR] [--frag-dir DIR] [--true-scale] [--no-extras] [--looks-json] <look name ...>

The look of each name is read from its fragment tools/arc_hd/looks/<name>.json (what merge_looks.py will write into looks.json), else from looks.json (`--looks-json` forces looks.json). A base id
also shows its `_form2` only if you name it. `--skin-dir DIR` reads the skins (and glow maps) from another folder, e.g. an export made with `gen_arc_hd.py --size 64 --out-dir DIR`, for a side by side.
`--trio` draws front, back and a three-quarter bust of every name in one row; `--close` the bust only; `--back` front and back; `--side` front, three-quarter, side and back. `--aa N` renders at N times
the size and averages down (default 2 for an HD skin: the renderer samples nearest, so an HD skin needs it to show its detail). Every character is drawn at scale 1 so the details compare
(`--true-scale` keeps the sheet's height). It is the software renderer of tools/playermodel (a review tool: flat shading, no game light).
"""
import json
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import arc_hdlib as A  # noqa: E402
import preview as P  # noqa: E402  (tools/playermodel/preview.py)
import pmrender as R  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402


def skin_size(look, skin_dir=None):
    d = skin_dir or A.SKINS
    try:
        return Image.open(os.path.join(d, look["skin"] + ".png")).width
    except OSError:
        return 64


def tile(look, yaw, w=210, h=300, scale=6.3, cy=6, pitch=4, aa=None, skin_dir=None, true_scale=False, extras=True):
    look = dict(look)
    if not true_scale:
        look["scale"] = 1.0
    if not extras:
        look.pop("extras", None)
    if aa is None:
        aa = 2 if skin_size(look, skin_dir) > 64 else 1
    if skin_dir:                                                             # the skin and its _glow map are read relative to the kit's skin folder: a relative path reaches another folder
        look["skin"] = os.path.relpath(os.path.join(skin_dir, look["skin"]), A.SKINS)
    sc = P.scene_of(look)
    s = look.get("scale", 1.0) if true_scale else 1.0
    img = R.render(sc, size=(w * aa, h * aa), yaw=yaw, pitch=pitch, center=(0, cy, 0), scale=scale * aa / max(1.0, s * 0.9) if true_scale else scale * aa, floor=False, bg=(1, 1, 1))
    return img.resize((w, h), Image.LANCZOS) if aa > 1 else img


def look_of(name, frag_dir, use_looks_json, looks):
    if not use_looks_json:
        fr = A.read_fragments(frag_dir).get(name)
        if fr:
            return fr["look"]
    return looks[name]


def main(argv):
    out = argv[0]
    opts = {"--cols": "8", "--aa": None, "--skin-dir": None, "--frag-dir": None}
    skip = set()
    for k in opts:
        if k in argv:
            skip.add(argv.index(k) + 1)
            opts[k] = argv[argv.index(k) + 1]
    names = [a for i, a in enumerate(argv) if i > 0 and not a.startswith("--") and i not in skip]
    looks = A.read_looks()
    frags = A.read_fragments(opts["--frag-dir"])
    if not names:
        names = list(frags) or list(looks)
    cols = int(opts["--cols"])
    aa = int(opts["--aa"]) if opts["--aa"] else None
    sd = opts["--skin-dir"]
    use_json = "--looks-json" in argv
    ts = "--true-scale" in argv
    ex = "--no-extras" not in argv
    w, h, views, kw = 160, 300, [0], [{}]
    if "--back" in argv:
        w, views, kw = 320, [0, 180], [{}, {}]
    if "--close" in argv:
        w, h, views, kw = 190, 270, [24], [{"scale": 8.5, "cy": -1, "pitch": 6}]
    if "--trio" in argv:
        w, h, views, kw = 210 * 3, 300, [0, 180, 24], [{}, {}, {"scale": 9.5, "cy": 0, "pitch": 6}]
    if "--side" in argv:
        w, h, views, kw = 200 * 4, 300, [0, 35, 90, 180], [{}, {}, {}, {}]
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * rows), (255, 255, 255))
    for i, name in enumerate(names):
        look = look_of(name, opts["--frag-dir"], use_json, looks)
        for vi, yaw in enumerate(views):
            tw = w // len(views)
            t = tile(look, yaw, tw, h, aa=aa, skin_dir=sd, true_scale=ts, extras=ex, **kw[vi % len(kw)])
            sheet.paste(t, ((i % cols) * w + vi * tw, (i // cols) * h))
        ImageDraw.Draw(sheet).text(((i % cols) * w + 4, (i // cols) * h + 4), name, fill=(0, 0, 0))
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main(sys.argv[1:])
