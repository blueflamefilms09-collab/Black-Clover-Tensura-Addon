"""Software mock-ups of the canon devil looks (docs/previews/devil_canon_looks/*.png): NOT the game. They use the repo's numpy renderer (tools/playermodel/pmrender + pmgeo): orthographic camera, three
simple lights, the glow mask as a flat emissive pass, no mip-mapping, fog or GeckoLib easing.

    python3 -B tools/blockbench/gen_devil_canon.py --preview
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

OUT = os.path.join(ROOT, "docs", "previews", "devil_canon_looks")
ANCHORS = {"body": (np.eye(4), (0, 0, 0))}


def render(sheet, geo, clip, t, yaw, top, size=(260, 340), fit=0.86):
    base, glow = sheet
    scene = R.Scene()
    anim = G.sample_clip(clip, t) if clip else None
    scene.add(G.geo_faces(geo, base / 255.0, ANCHORS, anim=anim))
    scene.add(G.geo_faces(geo, glow / 255.0, ANCHORS, emissive=True, anim=anim))
    scale = size[1] * fit / (top + 8.0)
    return R.render(scene, size=size, yaw=yaw, pitch=10.0, center=(0, 24 - top / 2.0, 3), scale=scale, bg=(0.17, 0.15, 0.2))


def top_of(spec, stats):
    return stats["top_px"]


def turnaround(entry, spec, stats, title):
    sheet, geo, anims = entry
    idle, walk, atk = anims["animation.devil_trial.idle"], anims["animation.devil_trial.walk"], anims["animation.devil_trial.attack"]
    roar = anims["animation.devil_trial.roar"]
    views = [(idle, 0.4, 0, "front"), (idle, 0.4, 35, "three-quarter"), (idle, 0.4, 90, "side"), (idle, 0.4, 180, "back"), (walk, 0.25, 30, "walk"), (atk, 0.2, 40, "attack"), (roar, 0.9, 20, "roar")]
    w, h = 240, 340
    out = Image.new("RGB", (w * len(views), h + 20), (30, 26, 36))
    d = ImageDraw.Draw(out)
    for i, (clip, t, yaw, nm) in enumerate(views):
        out.paste(render(sheet, geo, clip, t, yaw, stats["top_px"], (w, h)), (i * w, 20))
        d.text((i * w + 6, 4), "%s  %s" % (title, nm), fill=(235, 230, 240))
    return out


def overview(sheets, specs, stats_by_id):
    cols = 7
    w, h = 190, 270
    rows = (len(specs) + cols - 1) // cols
    out = Image.new("RGB", (w * cols, (h + 14) * rows), (30, 26, 36))
    d = ImageDraw.Draw(out)
    for i, spec in enumerate(specs):
        x, y = (i % cols) * w, (i // cols) * (h + 14)
        sheet, geo, anims = sheets[spec["id"]]
        # one common scale so the devils can be compared: the tallest fills the cell
        out.paste(render(sheet, geo, anims["animation.devil_trial.idle"], 0.4, 30, 52.0, (w, h)), (x, y + 14))
        d.text((x + 6, y + 2), spec["id"], fill=(235, 230, 240))
    return out


def main(sheets, specs, stats_list):
    os.makedirs(OUT, exist_ok=True)
    stats = {s["id"]: s for s in stats_list}
    if len(sheets) == len(specs):
        overview(sheets, specs, stats).save(os.path.join(OUT, "overview.png"))
    for spec in specs:
        if spec["id"] not in sheets:
            continue
        st = stats.get(spec["id"]) or dict(top_px=44.0)
        turnaround(sheets[spec["id"]], spec, st, spec["id"]).save(os.path.join(OUT, "%s.png" % spec["id"]))
    print("previews ->", OUT)
