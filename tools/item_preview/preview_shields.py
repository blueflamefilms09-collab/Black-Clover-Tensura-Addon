"""
Headless before / after preview of the shield fix (1.110.0, docs/visual_upgrade/shield_fix.md).

    python3 -B tools/item_preview/preview_shields.py [ids...]

For each shield-class item (visual/shields/ShieldSet.java) it draws, side by side, in the same hand with the same camera:
  vanilla   the vanilla shield (a 12 x 22 plate with a red cross on its face, a handle behind it) under minecraft:item/shield[_blocking]
  before    the mod's mesh on the sprite diagonal under the OLD display of the item's model (docs/visual_upgrade/shield_fix_before.json)
  after     the mod's mesh on vanilla's shield frame (ShieldFrame) under the NEW display (the shipped model jsons)
for the lowered shield and the raised one, in third person (right hand and left hand, from the front-right) and in first person (right and left hand).
The hand chain is the game's: LivingEntityRenderer, the arm of the humanoid model with the ITEM / BLOCK arm pose, ItemInHandLayer's turn to the grip, ItemTransform#apply;
first person is ItemInHandRenderer's applyItemArmTransform. The runtime normaliser's scale-up of the old placement and the oversize guard are not simulated (both only
matter for the old look). Rasteriser: preview_weapons.py. Output: docs/previews/shields/<id>.png and shields_sheet.png.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview_weapons as PW  # noqa: E402

ROOT = PW.ROOT
ASSETS = PW.ASSETS
OUT = os.path.join(ROOT, "docs", "previews", "shields")
IDS = "magebane_shield legendary_shield aegis_of_the_abyss celestial_support heros_shield thunder_shield calamity_wave_aegis".split()
TILE = 200
PW.W = PW.H = TILE
PW.STARS = np.zeros((TILE, TILE, 3), np.float32)

D = math.pi / 180


# ---------------------------------------------------------------- matrices (column vectors, like JOML / PoseStack)
def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = [x, y, z]
    return m


def S(x, y, z):
    return np.diag([x, y, z, 1.0])


def RX(a):
    c, s = math.cos(a * D), math.sin(a * D)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1.0]])


def RY(a):
    c, s = math.cos(a * D), math.sin(a * D)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1.0]])


def RZ(a):
    c, s = math.cos(a * D), math.sin(a * D)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1.0]])


def display(d, left):
    """ItemTransform#apply: translate, then Rx * Ry * Rz (rotationXYZ), then scale; a left hand mirrors x and negates the y and z turn."""
    rx, ry, rz = d["rotation"]
    if left:
        ry, rz = -ry, -rz
    i = -1 if left else 1
    tx, ty, tz = d["translation"]
    sx, sy, sz = d["scale"]
    return T(i * tx / 16, ty / 16, tz / 16) @ RX(rx) @ RY(ry) @ RZ(rz) @ S(sx, sy, sz)


# ---------------------------------------------------------------- player model
WORLD = RY(180) @ S(-1, -1, 1) @ T(0, -1.501, 0)           # LivingEntityRenderer: model space -> the world, the player facing +z (south)


def arm_chain(left, blocking):
    """Model space -> the item pose of a held item (ItemInHandLayer): the arm, then the turn to the grip."""
    px = 5 if left else -5
    xr = -54.0 if blocking else -18.0                       # ArmPose.BLOCK: xRot*0.5 - 0.9424779 rad; ArmPose.ITEM: xRot*0.5 - 0.31415927 rad
    yr = (30.0 if left else -30.0) if blocking else 0.0
    arm = T(px / 16, 2 / 16, 0) @ RZ(0) @ RY(yr) @ RX(xr)  # ModelPart#translateAndRotate: rotationZYX
    return arm @ RX(-90) @ RY(180) @ T((-1 if left else 1) / 16, 0.125, -0.625)


def third_person(d, left, blocking):
    return WORLD @ arm_chain(left, blocking) @ display(d, left) @ T(-0.5, -0.5, -0.5)


def first_person(d, left):
    i = -1 if left else 1
    return T(i * 0.56, -0.52, -0.72) @ display(d, left) @ T(-0.5, -0.5, -0.5)


# ---------------------------------------------------------------- geometry
PAL = {"skin": (230, 180, 140), "shirt": (60, 130, 200), "pants": (60, 60, 140), "dark": (40, 30, 30), "wood": (150, 105, 60), "red": (200, 30, 30),
       "grey": (120, 120, 125), "white": (240, 240, 240), "floor": (96, 120, 80)}
PAL_NAMES = list(PAL)


def palette_skin():
    img = np.zeros((128, 128, 4), np.float32)
    for i, n in enumerate(PAL_NAMES):
        img[(i // 16) * 8:(i // 16) * 8 + 8, (i % 16) * 8:(i % 16) * 8 + 8] = [*(np.array(PAL[n]) / 255), 1.0]
    return img


def uv_of(name):
    i = PAL_NAMES.index(name)
    return (i % 16) * 8 + 4, (i // 16) * 8 + 4


def box(lo, hi, colors, face_colors=None):
    """A box as quads (layer 0) with outward Newell normals; colors is a palette name, face_colors maps a face name (+x -x +y -y +z -z) to another."""
    x0, y0, z0 = lo
    x1, y1, z1 = hi
    faces = {
        "+x": [(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)], "-x": [(x0, y0, z1), (x0, y1, z1), (x0, y1, z0), (x0, y0, z0)],
        "+y": [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)], "-y": [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)],
        "+z": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], "-z": [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)],
    }
    out = []
    for name, pts in faces.items():
        p = np.array(pts, float)
        n = np.zeros(3)
        for i in range(4):
            a, b = p[i], p[(i + 1) % 4]
            n += [(a[1] - b[1]) * (a[2] + b[2]), (a[2] - b[2]) * (a[0] + b[0]), (a[0] - b[0]) * (a[1] + b[1])]
        centre = (np.array(lo) + np.array(hi)) / 2
        if n @ (p.mean(axis=0) - centre) < 0:
            p = p[::-1]
            n = -n
        u, v = uv_of((face_colors or {}).get(name, colors))
        out.append((0, np.concatenate([p, np.tile([u, v], (4, 1))], axis=1), n / (np.linalg.norm(n) or 1)))
    return out


def vanilla_shield():
    """ShieldModel after BlockEntityWithoutLevelRenderer's scale(1, -1, -1): the plate (12 x 22 x 1, emblem on +z) and the handle behind it (toward -z), in blocks."""
    q = box((-6 / 16, -11 / 16, 1 / 16), (6 / 16, 11 / 16, 2 / 16), "wood", {"+z": "red"})
    q += box((-1.5 / 16, -1.5 / 16, 2.01 / 16), (1.5 / 16, 6 / 16, 2.3 / 16), "white")                 # the emblem: a plain mark on the face
    q += box((-4 / 16, -1.5 / 16, 2.01 / 16), (4 / 16, 1.5 / 16, 2.3 / 16), "white")
    q += box((-1 / 16, -3 / 16, -5 / 16), (1 / 16, 3 / 16, 1 / 16), "grey")
    return q


def transform(quads, m):
    r = m[:3, :3]
    out = []
    for (layer, pts, n) in quads:
        p = pts.copy()
        p[:, :3] = pts[:, :3] @ r.T + m[:3, 3]
        out.append((layer, p, r @ n))
    return out


def load_raw(item_id):
    """The mesh as the file states it (no placement): [(layer, 4 x 5 array, normal)], the glow line, the place line."""
    place, glow, quads = None, None, []
    for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt")):
        v = line.split()
        if not v or v[0] == "#":
            continue
        if v[0] == "place":
            place = [float(x) for x in v[1:]]
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "q" and int(v[1]) < 3:
            quads.append((int(v[1]), np.array([float(x) for x in v[2:22]]).reshape(4, 5), np.array([float(x) for x in v[22:25]])))
        elif v[0] == "qn" and int(v[1]) < 3:
            arr = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
            nn = arr[:, 5:8].sum(axis=0)
            quads.append((int(v[1]), arr[:, :5].copy(), nn / (np.linalg.norm(nn) or 1.0)))
    return place, glow, quads


def old_frame(place, quads):
    """WeaponRenderer before 1.110.0: translate(ox, oy, 0.5), turn about z by angle - 90, scale, translate(0, -ymin, 0)."""
    ox, oy, ang, sc, ymin = place
    return T(ox, oy, 0.5) @ RZ(ang - 90) @ S(sc, sc, sc) @ T(0, -ymin, 0)


def new_frame(quads):
    """ShieldFrame.fit: scale to the box, centre, front plane on the plate's front."""
    xs = [v for (_, p, _) in quads for v in p[:, 0]]
    ys = [v for (_, p, _) in quads for v in p[:, 1]]
    zs = [v for (_, p, _) in quads for v in p[:, 2]]
    w, h = max(xs) - min(xs), max(ys) - min(ys)
    k = min(1.3 / w, 1.2 / h)
    return T(-k * (min(xs) + max(xs)) / 2, -k * (min(ys) + max(ys)) / 2, 2 / 16 - k * max(zs)) @ S(k, k, k)


# ---------------------------------------------------------------- rendering
def render(quads, skins, cam, fov, glow_tex=None, glow=None):
    eye, f, r, u = cam
    img = np.zeros((TILE, TILE, 3), np.float32) + [0.33, 0.35, 0.42]
    zbuf = np.full((TILE, TILE), np.inf, np.float32)
    l1 = np.array([0.2, 1.0, -0.7]); l1 /= np.linalg.norm(l1)
    l2 = np.array([-0.2, 1.0, 0.7]); l2 /= np.linalg.norm(l2)
    k = (TILE / 2) / math.tan(math.radians(fov / 2))
    tint = np.array([(glow[0] >> 16) & 255, (glow[0] >> 8) & 255, glow[0] & 255], np.float32) / 255 * glow[3] if glow else None
    for layer in (0, 2):
        for (ly, pts, n, skin) in quads:
            if ly != layer or (layer == 2 and (glow_tex is None or skin is not skins["mod"])):
                continue
            d = pts[:, :3] - eye
            z = d @ f
            if np.any(z <= 0.01):
                continue
            P = np.stack([TILE / 2 + (d @ r) / z * k, TILE / 2 - (d @ u) / z * k, z], axis=-1)
            area = (P[1, 0] - P[0, 0]) * (P[2, 1] - P[0, 1]) - (P[2, 0] - P[0, 0]) * (P[1, 1] - P[0, 1])
            if area > 0:
                continue
            sh = 0.45 + 0.35 * max(0, n @ l1) + 0.2 * max(0, n @ l2)
            for tri in ((0, 1, 2), (0, 2, 3)):
                PW.raster(img, zbuf, P[list(tri)], pts[list(tri), 3:5], layer, sh, skin if layer == 0 else glow_tex, tint)
    return np.clip(img, 0, 1)


def look(eye, target):
    eye, target = np.array(eye, float), np.array(target, float)
    f = target - eye
    f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0])
    r /= np.linalg.norm(r)
    return eye, f, r, np.cross(r, f)


def player_quads():
    """A plain player (Steve proportions) facing +z in the world, feet on y = 0, the arms hanging; the held arm is added by the caller."""
    def part(lo, hi, name, face=None):
        return transform(box(tuple(v / 16 for v in lo), tuple(v / 16 for v in hi), name, face), WORLD)
    q = []
    q += part((-4, 0, -2), (4, 12, 2), "shirt")                 # body, model space (y down from the neck)
    q += part((-4, -8, -4), (4, 0, 4), "skin", {"-z": "dark"})  # head (the dark face is -z in the model: the front)
    q += part((-4, 12, -2), (0, 24, 2), "pants")
    q += part((0, 12, -2), (4, 24, 2), "pants")
    return q


def arm_quad(left, blocking):
    px = 5 if left else -5
    arm = box((-2 / 16, -2 / 16, -2 / 16), (2 / 16, 10 / 16, 2 / 16), "skin")
    return transform(arm, WORLD @ T(0, 0, 0) @ (arm_chain_only(left, blocking)))


def arm_chain_only(left, blocking):
    px = 5 if left else -5
    xr = -54.0 if blocking else -18.0
    yr = (30.0 if left else -30.0) if blocking else 0.0
    return T(px / 16, 2 / 16, 0) @ RY(yr) @ RX(xr)


def other_arm(left, blocking_other=False):
    """The arm that holds nothing hangs at the side."""
    px = -5 if left else 5
    arm = box((-2 / 16, -2 / 16, -2 / 16), (2 / 16, 10 / 16, 2 / 16), "skin")
    return transform(arm, WORLD @ T(px / 16, 2 / 16, 0))


def with_skin(quads, skin):
    return [(ly, p, n, skin) for (ly, p, n) in quads]


def scene_third(shield_quads_model_frame, disp, left, blocking):
    """The shield (in its own frame) held by the arm, the rest of the player around it."""
    m = third_person(disp, left, blocking)
    return transform(shield_quads_model_frame, m)


def tile_label(img, text):
    im = Image.fromarray((img * 255).astype(np.uint8))
    ImageDraw.Draw(im).text((4, 3), text, fill=(255, 255, 255))
    return np.asarray(im, np.float32) / 255


def one(item_id, before_json):
    place, glow, raw = load_raw(item_id)
    d = os.path.join(ASSETS, "textures", "entity", "weapon")
    skin = PW.tex(os.path.join(d, item_id + ".png"))
    glowtex = PW.tex(os.path.join(d, item_id + "_glow.png"))
    k = skin.shape[1] / 128.0
    raw = [(ly, np.concatenate([p[:, :3], p[:, 3:5] * k], axis=1), n) for (ly, p, n) in raw]
    pal = palette_skin()
    skins = {"mod": skin, "pal": pal}
    new_model = {s: json.load(open(os.path.join(ASSETS, "models", "item", item_id + s + ".json")))["display"] for s in ("", "_blocking")}
    old_model = {s: (before_json.get(item_id + s) or before_json[item_id])["display"] for s in ("", "_blocking")}
    vs = vanilla_shield()
    van = {"": VANILLA_SHIELD, "_blocking": VANILLA_BLOCKING}
    old_q = transform(raw, old_frame(place, raw))
    new_q = transform(raw, new_frame(raw))
    rows = []
    for title, left, blocking, fp in (("3rd right, lowered", False, False, False), ("3rd right, raised", False, True, False), ("3rd left, raised", True, True, False),
                                      ("1st right, lowered", False, False, True), ("1st right, raised", False, True, True), ("1st left, raised", True, True, True)):
        suf = "_blocking" if blocking else ""
        tiles = []
        for label, quads, disp, skinkey in (("vanilla", vs, van[suf], "pal"), ("before", old_q, old_model[suf], "mod"), ("after", new_q, new_model[suf], "mod")):
            hand = "left" if left else "right"
            ctx = ("firstperson_" if fp else "thirdperson_") + hand + "hand"
            dd = disp.get(ctx) or disp[ctx.replace("lefthand", "righthand")]        # a model without a left-hand entry uses its right-hand one (ItemTransforms)
            if fp:
                m = first_person(dd, left)
                world = transform(quads, m)
                cam, fov = look((0, 0, 0), (0, 0, -1)), 70
                allq = with_skin(world, skins[skinkey])
            else:
                m = third_person(dd, left, blocking)
                world = transform(quads, m)
                body = player_quads() + arm_quad(left, blocking) + other_arm(left)
                cam, fov = look((-1.9 if not left else 1.9, 1.5, 3.2), (0, 0.95, 0)), 40
                allq = with_skin(body, skins["pal"]) + with_skin(world, skins[skinkey])
            img = render(allq, skins, cam, fov, glowtex if skinkey == "mod" else None, glow)
            tiles.append(tile_label(img, label + " | " + title))
        rows.append(np.concatenate(tiles, 1))
    return np.concatenate(rows, 0)


VANILLA_SHIELD = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 2, 0], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, 0], "scale": [1.25, 1.25, 1.25]},
}
VANILLA_BLOCKING = {
    "thirdperson_righthand": {"rotation": [45, 135, 0], "translation": [3.51, 11, -2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [45, 135, 0], "translation": [13.51, 3, 5], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11], "scale": [1.25, 1.25, 1.25]},
}


def pick(display, ctx):
    return display.get(ctx) or display[ctx.replace("lefthand", "righthand")]


def face_report(item_id, before_json):
    """Where the shield's face points (a world direction) and where its centre is, for the vanilla shield, before and after: the numbers behind the pictures."""
    place, glow, raw = load_raw(item_id)
    new_model = {s: json.load(open(os.path.join(ASSETS, "models", "item", item_id + s + ".json")))["display"] for s in ("", "_blocking")}
    old_model = {s: (before_json.get(item_id + s) or before_json[item_id])["display"] for s in ("", "_blocking")}
    lines = []
    for ctx, left, blocking, fp in (("thirdperson_righthand", False, False, False), ("thirdperson_righthand", False, True, False), ("thirdperson_lefthand", True, False, False),
                                    ("thirdperson_lefthand", True, True, False), ("firstperson_righthand", False, False, True), ("firstperson_righthand", False, True, True)):
        suf = "_blocking" if blocking else ""
        row = []
        for label, frame, disp in (("vanilla", T(0, 0, 0), (VANILLA_BLOCKING if blocking else VANILLA_SHIELD)[ctx]), ("before", old_frame(place, raw), pick(old_model[suf], ctx)),
                                   ("after", new_frame(raw), pick(new_model[suf], ctx))):
            m = (first_person(disp, left) if fp else third_person(disp, left, blocking)) @ T(0, 0, 0)
            # the face direction is +z of the shield frame (vanilla: after its flip; the mesh: its front); the centre of the frame's footprint
            face = m[:3, :3] @ (frame[:3, :3] @ np.array([0, 0, 1.0]))
            face /= np.linalg.norm(face)
            row.append("%s face (%+.2f %+.2f %+.2f)" % (label, *face))
        lines.append("  %s%s: %s" % (ctx, " raised" if blocking else "", " | ".join(row)))
    return lines


def main(ids):
    ids = ids or IDS
    before_json = json.load(open(os.path.join(ROOT, "docs", "visual_upgrade", "shield_fix_before.json")))
    os.makedirs(OUT, exist_ok=True)
    sheets = []
    for i in ids:
        img = one(i, before_json)
        path = os.path.join(OUT, i + ".png")
        Image.fromarray((img * 255).astype(np.uint8)).save(path)
        print("wrote", os.path.relpath(path, ROOT))
        print("\n".join(face_report(i, before_json)))
        sheets.append(img)
    if len(ids) > 1:
        out = os.path.join(OUT, "shields_sheet.png")
        Image.fromarray((np.concatenate(sheets, 1) * 255).astype(np.uint8)).save(out)
        print("wrote", os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main(sys.argv[1:])
