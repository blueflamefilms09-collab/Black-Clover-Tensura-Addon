"""preview_hart3: two software views that the repo's own previews do not give, for the weapons of art_hart3.py (no game, no GPU).

    python3 -B tools/heroweapon/preview_hart3.py entity [out.png] [yaw,pitch ...] [--id vassal_carriage] [--zoom CM --center HEIGHT,ALONG,ACROSS]
    python3 -B tools/heroweapon/preview_hart3.py firstperson [out.png] id [id ...] [--display rx,ry,rz,tx,ty,tz,scale]

Why: nothing can be seen in the game while the art is made, so each question that the game would answer needs a copy of the game's own rule.

  entity       The carriage's mesh is also the entity's model, and the entity renderer draws it in another frame than the weapon preview does: nose (+y) to the
               right, roof (-x) up, +z toward the viewer, wheels' lowest point on the ground. mesh_lib's previews look along the weapon's y axis, which shows a
               carriage on its nose. Here the same z-buffered rasteriser draws the written mesh and skins in the entity's frame, from any yaw and pitch, with an
               optional zoom on a point given as (height above the ground, along the nose, across) in cm of the model.
  firstperson  What the player sees: ItemInHandRenderer puts the item at the hand (0.56 right, 0.52 down, 0.72 ahead of the camera), the model json's
               firstperson_righthand display transform (translate, rotate x y z, scale) is applied about the item cube's centre, then the mesh's own `place`
               line puts the mesh into the cube. The picture is 16:9 with the 70 degree field the hand is drawn with. The vertices are projected exactly; the
               texture is interpolated flat on the screen (the game interpolates it in perspective), which is invisible at this size. This is the chain that
               tools/relics/display_presets.py documents and bounds; it is not the game, and the player's bobbing, the equip animation and the arm model are absent.
Output goes to build/hart3/ unless a path is given (build/ is not committed).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as pm                                                       # noqa: E402
import weapon_sheet as ws                                                       # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT_DIR = os.path.join(ROOT, "build", "hart3")
FOV = 70.0                                                                      # the vertical field of the hand (GameRenderer draws it with the player's field of view setting, 70 by default)
SCREEN = (640, 360)

# mesh (x, y, z) -> entity view (X, Y, Z) = (y, -x, z): nose to the right, roof up, +z toward the viewer (a proper rotation, so no face turns inside out)
ENTITY_FRAME = np.array([[0, 1, 0], [-1, 0, 0], [0, 0, 1]], float)


def _paths(item_id):
    td = os.path.join(ASSETS, "textures", "entity", "weapon")
    return (os.path.join(ASSETS, "weapon_meshes", item_id + ".txt"), os.path.join(td, item_id + ".png"), os.path.join(td, item_id + "_glow.png"))


def entity_view(item_id="vassal_carriage", views=((0, 0), (-35, 20), (145, 20), (0, 90), (90, 0)), zoom=None, center=None, panel=(560, 460)):
    """A row of renders of the mesh in the entity frame; returns a PIL image."""
    mesh, skin, glow = _paths(item_id)
    d = pm.parse_game_txt(mesh)
    quads = [(ly, P @ ENTITY_FRAME.T, UV, N @ ENTITY_FRAME.T, (0.7, 0.7, 0.7)) for (ly, P, UV, N) in d["quads"]]
    body = np.concatenate([q[1] for q in quads if q[0] == 0])
    pw, ph = panel
    if center is not None:
        u, f, z = center
        c3 = np.array([f, u, z], float)                                          # entity frame: X along the nose, Y up (height), Z across
    else:
        c3 = (body.min(axis=0) + body.max(axis=0)) / 2
    if zoom:
        k = min(pw, ph) / (2.0 * zoom)
    else:
        diag = float(np.linalg.norm(body.max(axis=0) - body.min(axis=0)))
        k = min(pw - 30, ph - 30) / (diag * 0.9)
    tiles = []
    for (yaw, pitch) in views:
        R = pm._rot(yaw, pitch)
        c = R @ c3
        tiles.append((pm.render_view(quads, yaw, pitch, panel, k, (c[0], c[1]), skin, glow, d["glow"], 2, True, True, True), "yaw %g pitch %g" % (yaw, pitch)))
    row = Image.fromarray((np.concatenate([t[0] for t in tiles], axis=1) * 255).astype(np.uint8))
    dr = ImageDraw.Draw(row)
    for i, (_, name) in enumerate(tiles):
        dr.text((i * pw + 6, 4), name, fill=(255, 255, 255))
    return row


def display_of(item_id, override=None):
    """(rotation, translation px, scale) of firstperson_righthand from the model json, or the override (rx, ry, rz, tx, ty, tz, scale)."""
    if override:
        return override[0:3], override[3:6], [override[6]] * 3
    with open(os.path.join(ASSETS, "models", "item", item_id + ".json"), encoding="utf-8") as f:
        dsp = json.load(f)["display"]["firstperson_righthand"]
    return dsp["rotation"], dsp["translation"], dsp["scale"]


def first_person_matrix(item_id, override=None):
    """Item cube -> camera space (x right, y up, z toward the viewer, the camera looks down -z): T(hand) T(translation / 16) Rx Ry Rz S T(-1/2)."""
    rot, tr, sc = display_of(item_id, override)
    hand = ws.T(0.56, -0.52, -0.72)
    disp = ws.T(tr[0] / 16, tr[1] / 16, tr[2] / 16) @ ws.Rx(rot[0]) @ ws.Ry(rot[1]) @ ws.Rz(rot[2]) @ ws.S(*sc)
    return hand @ disp @ ws.T(-0.5, -0.5, -0.5)


def first_person_view(item_id, override=None):
    """The first person picture of one weapon; returns a PIL image."""
    mesh, skin, glow = _paths(item_id)
    d = pm.parse_game_txt(mesh)
    M = first_person_matrix(item_id, override) @ ws.place_matrix(d["place"])
    W, H = SCREEN
    f = 1.0 / math.tan(math.radians(FOV / 2))
    quads = []
    for (ly, P, UV, N) in d["quads"]:
        Q = np.array([(M @ np.array([p[0], p[1], p[2], 1.0]))[:3] for p in P])
        Nv = np.array([M[:3, :3] @ n for n in N])
        Nv /= np.maximum(np.linalg.norm(Nv, axis=1, keepdims=True), 1e-9)
        z = Q[:, 2]
        if (z > -0.02).any():
            continue                                                            # a quad that reaches the near plane is dropped, as the game clips it
        quads.append((ly, np.stack([f / (W / H) * Q[:, 0] / (-z) * (W / 2), f * Q[:, 1] / (-z) * (H / 2), 1000.0 / (-z)], axis=1), UV, Nv, (0.7, 0.7, 0.7)))
    img = pm.render_view(quads, 0, 0, SCREEN, 1.0, (0.0, 0.0), skin, glow, d["glow"], 1, True, True, True)
    im = Image.fromarray((img * 255).astype(np.uint8))
    dr = ImageDraw.Draw(im)
    dr.line([(W // 2 - 8, H // 2), (W // 2 + 8, H // 2)], fill=(255, 255, 255), width=1)
    dr.line([(W // 2, H // 2 - 8), (W // 2, H // 2 + 8)], fill=(255, 255, 255), width=1)
    dr.text((6, 4), item_id, fill=(255, 255, 255))
    return im


def main(argv):
    if not argv or argv[0] not in ("entity", "firstperson"):
        print(__doc__)
        return 2
    mode, rest = argv[0], argv[1:]
    opts, args = {}, []
    it = iter(rest)
    for a in it:
        if a in ("--id", "--zoom", "--center", "--display"):
            opts[a] = next(it)
        else:
            args.append(a)
    out = args.pop(0) if args and args[0].lower().endswith(".png") else os.path.join(OUT_DIR, mode + ".png")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    if mode == "entity":
        views = tuple(tuple(float(x) for x in a.split(",")) for a in args) or ((0, 0), (-35, 20), (145, 20), (0, 90), (90, 0))
        center = tuple(float(x) for x in opts["--center"].split(",")) if "--center" in opts else None
        entity_view(opts.get("--id", "vassal_carriage"), views, float(opts["--zoom"]) if "--zoom" in opts else None, center).save(out)
    else:
        override = [float(x) for x in opts["--display"].split(",")] if "--display" in opts else None
        ims = [first_person_view(i, override) for i in args]
        cols = min(len(ims), 2)
        sheet = Image.new("RGB", (SCREEN[0] * cols, SCREEN[1] * ((len(ims) + 1) // 2)), (20, 20, 20))
        for k, im in enumerate(ims):
            sheet.paste(im, ((k % 2) * SCREEN[0], (k // 2) * SCREEN[1]))
        sheet.save(out)
    print("wrote", out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
