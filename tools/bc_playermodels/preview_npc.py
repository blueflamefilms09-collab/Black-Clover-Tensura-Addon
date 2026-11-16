"""Software previews of the Black Clover NPC and Midnight Sun player models (docs/npcs_bc_previews/): a turnaround sheet (front, three-quarter, side, back) per look, the old look on the left with --old,
and an overview sheet.  It reuses preview_bc.py's renderer; only the table (looks_bc_npc.json), the output folder and the overview title differ.

    python3 -B tools/bc_playermodels/preview_npc.py [id-part ...] [--out DIR] [--old] [--no-overview]

SOFTWARE RENDERS (not the game): see preview_bc.py.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

from PIL import Image, ImageDraw  # noqa: E402

import preview_bc as B  # noqa: E402

LOOKS_NPC = os.path.join(B.L.ASSETS, "playermodel", "looks_bc_npc.json")


def overview(looks, cols=8, title="Black Clover NPCs and the Midnight Sun on the player model, three-quarter view   (software renders, idle pose; not the game)"):
    ids = list(looks)
    tw, th = 190, 320
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
    out_dir = os.path.join(B.ROOT, "docs", "npcs_bc_previews")
    flt, use_old, ov = [], False, True
    i = 0
    while i < len(argv):
        if argv[i] == "--out":
            out_dir = argv[i + 1]
            i += 2
            continue
        if argv[i] == "--old":
            use_old = True
        elif argv[i] == "--no-overview":
            ov = False
        else:
            flt.append(argv[i])
        i += 1
    looks = json.load(open(LOOKS_NPC))["looks"]
    olds = B.PV.load_looks()
    os.makedirs(out_dir, exist_ok=True)
    for lid, look in looks.items():
        if flt and not any(f in lid for f in flt):
            continue
        im = B.sheet(lid, look, olds.get(lid[3:]) if use_old else None)
        p = os.path.join(out_dir, lid + ".png")
        im.save(p)
        print(p)
    if not flt and ov:
        p = os.path.join(out_dir, "overview.png")
        overview(looks).save(p)
        print(p)


if __name__ == "__main__":
    main(sys.argv[1:])
