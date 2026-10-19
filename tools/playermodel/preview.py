"""Why: nothing of the player-model kit can be run in a game here, so this renders what the kit draws (the player model with the character's skin, the hair style at its colour, and the extras
anchored on the model's parts) with the software renderer in pmrender.py / pmgeo.py, straight from assets/nusmp/playermodel/looks.json, and saves the images the review needs.
    python3 -B tools/playermodel/preview.py asta escanor ...            characters (front, three-quarter, back), written to docs/previews/playermodel/
    python3 -B tools/playermodel/preview.py --hair                       every hair style on a plain body, three views
    python3 -B tools/playermodel/preview.py --hair-motion                three hair styles at rest, running and nodding (the spring chain's targets)
    python3 -B tools/playermodel/preview.py --sheet                      a contact sheet of every look
Options: --out <dir> (default docs/previews/playermodel), --scale N.
It is a re-implementation for review: colours and lighting differ from the game's, and the extras' animation is shown at rest (idle clip, t = 0.4 s).
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import numpy as np  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

import pmgeo as G  # noqa: E402
import pmrender as R  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = R.ASSETS
LOOKS = os.path.join(ASSETS, "playermodel", "looks.json")
BASE = 1.875


def hex_rgb(s, default=(60, 40, 30)):
    if not s:
        return default
    s = str(s).lstrip("#")
    try:
        return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))
    except ValueError:
        return default


def load_looks():
    return json.load(open(LOOKS))["looks"]


def scene_of(look, pose=None, chain_angles=None, clip="idle", t=0.4, helmet=False, nohair=False):
    slim = bool(look.get("slim"))
    pose = dict(pose or {})
    if look.get("hold_r") and "arm_r" not in pose:
        pose["arm_r"] = (look["hold_r"], 0.0, 0.0)
    if look.get("hold_l") and "arm_l" not in pose:
        pose["arm_l"] = (look["hold_l"], 0.0, 0.0)
    skin = R.load_tex(os.path.join(ASSETS, "textures", "entity", "playermodel", look["skin"] + ".png"))
    sc = R.Scene()
    sc.add(R.player_faces(skin, slim=slim, pose=pose))
    if look.get("glow"):
        gp = os.path.join(ASSETS, "textures", "entity", "playermodel", look["skin"] + "_glow.png")
        if os.path.isfile(gp):
            gt = R.load_tex(gp)
            gf = R.player_faces(gt, slim=slim, pose=pose, overlays=False, alpha_mode="additive")
            for f in gf:
                f.emissive = True
            sc.add(gf)
    if look.get("hair") and look["hair"] != "bald" and not nohair:
        sc.add(R.hair_faces(look["hair"], hex_rgb(look.get("hair_color")), slim=slim, pose=pose, chain_angles=chain_angles, helmet=helmet))
    anchors = {}
    for name in ("head", "body", "arm_r", "arm_l"):
        m, piv = R.part_pose_matrix(name, slim, pose)
        anchors[name] = (m, piv)
    for ex in look.get("extras", []):
        g = G.geo_load(ex["model"])
        if g is None:
            continue
        tp = os.path.join(ASSETS, "textures", "entity", ex.get("texture", ex["model"]) + ".png")
        if not os.path.isfile(tp):
            continue
        tex = R.load_tex(tp)
        clips = G.anim_load(ex["model"])
        anim = G.sample_clip(clips.get(clip), t) if clips.get(clip) else None
        sc.add(G.geo_faces(g, tex, anchors, anim=anim))
        gp = os.path.join(ASSETS, "textures", "entity", ex.get("texture", ex["model"]) + "_glow.png")
        if ex.get("glow", True) and os.path.isfile(gp):
            gf = G.geo_faces(g, R.load_tex(gp), anchors, emissive=True, mode="additive", anim=anim)
            sc.add(gf)
    return sc


def render_look(look, yaw, pitch=6, size=(300, 430), scale=None, **kw):
    s = look.get("scale", 1.0)
    sc = scene_of(look, **kw)
    px = scale or 7.4 / max(1.0, s * 0.9)
    return R.render(sc, size=size, yaw=yaw, pitch=pitch, center=(0, 12, 0), scale=px, floor=True)


def character_sheet(name, look, out_dir, scale=None):
    views = [(0, 6), (35, 6), (180, 6)]
    tiles = [render_look(look, y, p, scale=scale) for y, p in views]
    W, H = tiles[0].size
    sheet = Image.new("RGB", (W * len(tiles), H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    d = ImageDraw.Draw(sheet)
    d.text((6, 6), name, fill=(0, 0, 0))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, name + ".png")
    sheet.save(path)
    return path, sheet


def hair_sheet(out_dir):
    doc = R.hair_doc()
    skin = R.flat_skin()
    cols = [(110, 70, 40), (230, 200, 110), (40, 40, 60), (200, 60, 70), (150, 190, 230), (235, 235, 240), (60, 150, 90), (150, 90, 190)]
    W, H = 200, 250
    views = [(0, 6), (40, 6), (180, 6)]
    cells = []
    for i, sid in enumerate(doc["styles"]):
        cell = Image.new("RGB", (W * len(views), H))
        for vi, (yaw, pitch) in enumerate(views):
            sc = R.Scene()
            sc.add(R.player_faces(skin))
            sc.add(R.hair_faces(sid, cols[i % len(cols)]))
            cell.paste(R.render(sc, size=(W, H), yaw=yaw, pitch=pitch, center=(0, 2, 0), scale=9.0, floor=False), (vi * W, 0))
        ImageDraw.Draw(cell).text((4, 4), sid, fill=(0, 0, 0))
        cells.append(cell)
    per_row = 2
    rows = (len(cells) + per_row - 1) // per_row
    out = Image.new("RGB", (W * len(views) * per_row, H * rows), (255, 255, 255))
    for i, c in enumerate(cells):
        out.paste(c, ((i % per_row) * W * len(views), (i // per_row) * H))
    os.makedirs(out_dir, exist_ok=True)
    # split into pages of 6 styles so each image stays readable
    pages = []
    page_h = H * 3
    n = 0
    y = 0
    while y < out.size[1]:
        part = out.crop((0, y, out.size[0], min(out.size[1], y + page_h)))
        path = os.path.join(out_dir, "hair_styles_%d.png" % n)
        part.save(path)
        pages.append(path)
        n += 1
        y += page_h
    return pages


def hair_motion(out_dir):
    """Three styles at rest, running (wind pitch), turning (roll) and nodding: the targets the spring chain is pulled to, applied as static angles."""
    doc = R.hair_doc()
    skin = R.flat_skin()
    styles = [s for s in ("long", "ponytail", "twin_tails", "braid") if s in doc["styles"]]
    W, H = 210, 260
    poses = [("rest", {}, 0.0, 0.0, {}), ("running", {"body": (0.15, 0, 0)}, 0.55, 0.0, {}), ("turning", {}, 0.0, 0.5, {}), ("nodding", {"head": (0.6, 0, 0)}, -0.0, 0.0, {})]
    out = Image.new("RGB", (W * len(poses), H * len(styles)), (255, 255, 255))
    for si, sid in enumerate(styles):
        for pi, (label, pose, pitch, roll, _) in enumerate(poses):
            angles = {}
            for c in doc["styles"][sid]["chains"]:
                n = len(c["segments"])
                shares = [(1.0 + 0.55 * j) for j in range(n)]
                tot = sum(shares)
                tp = pitch - (pose.get("head", (0, 0, 0))[0] * 0.85 if label == "nodding" else 0.0)
                angles[c["name"]] = [(tp * shares[j] / tot, roll * shares[j] / tot) for j in range(n)]
            sc = R.Scene()
            sc.add(R.player_faces(skin, pose=pose))
            sc.add(R.hair_faces(sid, (120, 80, 50), pose=pose, chain_angles=angles))
            im = R.render(sc, size=(W, H), yaw=60 if label != "turning" else 150, pitch=4, center=(0, 5, 0), scale=8.2, floor=False)
            ImageDraw.Draw(im).text((4, 4), "%s: %s" % (sid, label), fill=(0, 0, 0))
            out.paste(im, (pi * W, si * H))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "hair_motion.png")
    out.save(path)
    return path


def contact_sheet(out_dir, looks, ids):
    W, H = 150, 215
    cols = 10
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (W * cols, H * rows), (255, 255, 255))
    for i, name in enumerate(ids):
        look = looks[name]
        im = R.render(scene_of(look), size=(W, H), yaw=25, pitch=6, center=(0, 12, 0), scale=3.3 * min(1.6, max(0.7, look.get("scale", 1.0))) / max(1.0, look.get("scale", 1.0) * 0.8), floor=False)
        ImageDraw.Draw(im).text((3, 3), name[:22], fill=(0, 0, 0))
        sheet.paste(im, ((i % cols) * W, (i // cols) * H))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "contact_sheet.png")
    sheet.save(path)
    return path


def main():
    args = sys.argv[1:]
    out = os.path.join(ROOT, "docs", "previews", "playermodel")
    if "--out" in args:
        i = args.index("--out")
        out = args[i + 1]
        del args[i:i + 2]
    scale = None
    if "--scale" in args:
        i = args.index("--scale")
        scale = float(args[i + 1])
        del args[i:i + 2]
    if "--hair" in args:
        for p in hair_sheet(out):
            print(p)
        return
    if "--hair-motion" in args:
        print(hair_motion(out))
        return
    looks = load_looks()
    if "--sheet" in args:
        print(contact_sheet(out, looks, list(looks)))
        return
    for name in args:
        if name not in looks:
            print("no look", name)
            continue
        path, _ = character_sheet(name, looks[name], out, scale)
        print(path)


if __name__ == "__main__":
    main()
