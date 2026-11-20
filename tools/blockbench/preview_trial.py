"""Software previews of the Trial Devil (docs/devil_looks_previews/trial_*.png): NOT the game. They use the repo's numpy renderer (tools/playermodel/pmrender + pmgeo, the same
one the player-model looks are previewed with): orthographic camera, three simple lights, the glow mask drawn as a flat emissive pass, no mip-mapping, fog or GeckoLib blending.

    python3 -B tools/blockbench/gen_devil_trial.py --preview
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "playermodel"))
import pmgeo as G  # noqa: E402
import pmrender as R  # noqa: E402

OUT = os.path.join(ROOT, "docs", "devil_looks_previews")
ANCHORS = {"body": (np.eye(4), (0, 0, 0))}


def render(sheet, geo, clip, t, yaw, size=(300, 380), scale=6.4):
    base, glow = sheet
    scene = R.Scene()
    anim = G.sample_clip(clip, t) if clip else None
    scene.add(G.geo_faces(geo, base / 255.0, ANCHORS, anim=anim))
    scene.add(G.geo_faces(geo, glow / 255.0, ANCHORS, emissive=True, anim=anim))
    return R.render(scene, size=size, yaw=yaw, pitch=10.0, center=(0, 3, 4), scale=scale, bg=(0.17, 0.15, 0.2))


def sheet_for(sheets, dev_id):
    return sheets["_" + dev_id]


def turnaround(sheet, geo, anims, title):
    idle = anims["animation.devil_trial.idle"]
    views = [(idle, 0.4, 0), (idle, 0.4, 35), (idle, 0.4, 90), (idle, 0.4, 180), (anims["animation.devil_trial.walk"], 0.25, 30), (anims["animation.devil_trial.attack"], 0.2, 40)]
    names = ["front", "three-quarter", "side", "back", "walk", "attack"]
    w, h = 250, 330
    out = Image.new("RGB", (w * len(views), h + 20), (30, 26, 36))
    d = ImageDraw.Draw(out)
    for i, (clip, t, yaw) in enumerate(views):
        out.paste(render(sheet, geo, clip, t, yaw, (w, h), 5.6), (i * w, 20))
        d.text((i * w + 6, 4), "%s  %s" % (title, names[i]), fill=(235, 230, 240))
    return out


def overview(sheets, geo, anims, ids):
    cols = 7
    w, h = 180, 250
    rows = (len(ids) + cols - 1) // cols
    out = Image.new("RGB", (w * cols, (h + 14) * rows), (30, 26, 36))
    d = ImageDraw.Draw(out)
    idle = anims["animation.devil_trial.idle"]
    for i, dev in enumerate(ids):
        x, y = (i % cols) * w, (i // cols) * (h + 14)
        out.paste(render(sheet_for(sheets, dev), geo, idle, 0.4, 30, (w, h), 4.4), (x, y + 14))
        d.text((x + 6, y + 2), dev, fill=(235, 230, 240))
    return out


def main(sheets, geo, anims, dvtable):
    os.makedirs(OUT, exist_ok=True)
    ids = [d["id"] for d in dvtable.DEVILS]
    overview(sheets, geo, anims, ids).save(os.path.join(OUT, "trial_overview.png"))
    for dev in ("liebe", "lucifero", "fox_devil", "daemon_rouge"):
        turnaround(sheet_for(sheets, dev), geo, anims, dev).save(os.path.join(OUT, "trial_%s.png" % dev))
    print("previews ->", OUT)
