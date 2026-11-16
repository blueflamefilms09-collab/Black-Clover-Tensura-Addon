"""Software previews of the devil player models (docs/devil_looks_previews/): a turnaround sheet (front, three-quarter, side, back) per devil, and an overview sheet.

    python3 -B tools/bc_playermodels/preview_dv.py [id-part ...] [--out DIR] [--form]

SOFTWARE RENDERS (pmrender / pmgeo's orthographic numpy renderer, through preview_bc's helpers): not the game. Colours and lighting differ, cutout cards have no mipmaps or fog, the extras are shown at
t = 0.4 s of their idle clip, the additive glow is drawn at a fixed level. --form draws the look as it is WORN on a player (the hair layers left out, the player's own skin = a plain stand-in skin).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

from PIL import Image, ImageDraw  # noqa: E402

import preview_bc as B  # noqa: E402

LOOKS_DV = os.path.join(B.L.ASSETS, "playermodel", "looks_dv.json")


def worn(look):
    """The look as a player wears it: no hair layers (the player keeps their own hair)."""
    out = dict(look)
    out["extras"] = [x for x in look["extras"] if not (x["model"].endswith("_hair") or x["model"].endswith("_hair_fine"))]
    return out


def overview(looks, title):
    import dvtable
    ids = [("dv_" + i) for i in dvtable.IDS if ("dv_" + i) in looks]
    cols = 7
    tw, th = 200, 330
    rows = (len(ids) + cols - 1) // cols
    out = Image.new("RGB", (tw * cols, (th + 16) * rows + 22), (232, 236, 242))
    d = ImageDraw.Draw(out)
    d.text((6, 5), title, fill=(20, 20, 20))
    for i, lid in enumerate(ids):
        look = looks[lid]
        zoom, cy = B.fit(look)
        t = B.render(look, 32, 7, zoom=zoom, cy=cy).resize((tw, int(tw * 600 / 340)))
        x, y = (i % cols) * tw, 22 + (i // cols) * (th + 16)
        out.paste(t.crop((0, 0, tw, th)), (x, y + 16))
        d.text((x + 4, y + 2), lid[3:], fill=(20, 20, 20))
    return out


def main(argv):
    out_dir = os.path.join(B.ROOT, "docs", "devil_looks_previews")
    flt, form = [], False
    i = 0
    while i < len(argv):
        if argv[i] == "--out":
            out_dir = argv[i + 1]
            i += 2
            continue
        if argv[i] == "--form":
            form = True
        else:
            flt.append(argv[i])
        i += 1
    looks = json.load(open(LOOKS_DV))["looks"]
    if form:
        looks = {k: worn(v) for k, v in looks.items()}
    os.makedirs(out_dir, exist_ok=True)
    for lid, look in looks.items():
        if flt and not any(f in lid for f in flt):
            continue
        p = os.path.join(out_dir, lid + ("_worn" if form else "") + ".png")
        B.sheet(lid, look).save(p)
        print(p)
    if not flt:
        p = os.path.join(out_dir, "overview_worn.png" if form else "overview.png")
        overview(looks, "Devils on the player model, three-quarter view" + (", as worn on a player (hair layers off)" if form else "") + "   (software renders, idle pose; not the game)").save(p)
        print(p)


if __name__ == "__main__":
    main(sys.argv[1:])
