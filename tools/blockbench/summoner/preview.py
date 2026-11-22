"""Software mock-ups of the summon models (docs/previews/summoner_creatures/): NOT the game. The repo's numpy renderer (tools/playermodel), three views and the walk and ability clips per species."""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "playermodel"))
import pmgeo as G  # noqa: E402
import pmrender as R  # noqa: E402

OUT = os.path.join(ROOT, "docs", "previews", "summoner_creatures")
ANCHORS = {"body": (np.eye(4), (0, 0, 0))}


def render(sheet, geo, clip, t, yaw, top, size, width, zoom=1.0):
    base, glow = sheet
    scene = R.Scene()
    anim = G.sample_clip(clip, t) if clip else None
    scene.add(G.geo_faces(geo, base / 255.0, ANCHORS, anim=anim))
    scene.add(G.geo_faces(geo, glow / 255.0, ANCHORS, emissive=True, anim=anim))
    zs = [c["origin"][2] + sz for b in geo["bones"] for c in b.get("cubes", []) for sz in (0, c["size"][2])]
    xs = [abs(c["origin"][0]) + c["size"][0] for b in geo["bones"] for c in b.get("cubes", [])]
    span = max(top, (max(zs) - min(zs)) * 1.05, max(xs) * 2.0) + 10.0
    scale = size[1] * 0.84 / span * zoom
    return R.render(scene, size=size, yaw=yaw, pitch=10.0, center=(0, 24 - top / 2.0, 3), scale=scale, bg=(0.17, 0.15, 0.2))


def sheet_for(sid, entry):
    sheet, geo, anims, st = entry
    ident = "animation.summon_%s." % sid
    views = [("idle", 0.4, 0, "front"), ("idle", 0.4, 40, "3/4"), ("idle", 0.4, 90, "side"), ("walk", 0.25, 60, "walk"), ("attack", 0.32, 60, "attack"), ("ability", 0.8, 50, "ability")]
    w, h = 240, 300
    img = Image.new("RGB", (w * len(views), h + 18), (30, 26, 36))
    d = ImageDraw.Draw(img)
    width = geo["description"]["visible_bounds_width"]
    for i, (clip, t, yaw, nm) in enumerate(views):
        img.paste(render(sheet, geo, anims.get(ident + clip), t, yaw, st["top_px"], (w, h), width), (i * w, 18))
        d.text((i * w + 6, 3), "%s  %s" % (sid, nm), fill=(235, 230, 240))
    return img


def zoom_sheet(sid, entry, zoom=1.45):
    """Big front, 3/4 and side views of one model (set ZOOM=1 in the environment): for judging the faces and details."""
    sheet, geo, anims, st = entry
    ident = "animation.summon_%s." % sid
    views = [("idle", 0.4, 0, "front"), ("idle", 0.4, 40, "3/4"), ("idle", 0.4, 100, "side")]
    w, h = 460, 560
    img = Image.new("RGB", (w * len(views), h), (30, 26, 36))
    for i, (clip, t, yaw, nm) in enumerate(views):
        img.paste(render(sheet, geo, anims.get(ident + clip), t, yaw, st["top_px"], (w, h), geo["description"]["visible_bounds_width"], zoom), (i * w, 0))
    return img


def main(out):
    if os.environ.get("ZOOM"):
        os.makedirs(OUT, exist_ok=True)
        for sid, entry in out.items():
            zoom_sheet(sid, entry).save(os.path.join("/tmp/claude-0", "zoom_" + sid + ".png"))
        print("zoom -> /tmp/claude-0/zoom_<id>.png")
        return
    os.makedirs(OUT, exist_ok=True)
    tiles = []
    for sid, entry in out.items():
        im = sheet_for(sid, entry)
        im.save(os.path.join(OUT, sid + ".png"))
        tiles.append(im.crop((240 * 1, 0, 240 * 2, 318)))
    if tiles:
        W = Image.new("RGB", (240 * min(6, len(tiles)), 318 * ((len(tiles) + 5) // 6)), (30, 26, 36))
        for i, t in enumerate(tiles):
            W.paste(t, ((i % 6) * 240, (i // 6) * 318))
        W.save(os.path.join(OUT, "overview.png"))
    print("previews ->", OUT)
