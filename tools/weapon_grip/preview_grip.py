"""
Before / after sheets of where the hand holds a weapon (docs/weapon_grip.md).

    python tools/weapon_grip/preview_grip.py [--out docs/weapon_grip/sheet.png] [--tile 260] [ids...]

For each weapon one row of tiles, all in the HAND FRAME of the third-person display transform (the hand is the red ring in the middle of the frame; the weapon is drawn
exactly where the display block of its item model puts it):
    1. 1.110.0  - the mesh on the old sprite diagonal: the hand lands on whatever is 25 to 33 percent up the weapon
    2. 1.111.0  - the grip point of the mesh (green cross) lands in the hand      (side view, right hand)
    3. 1.111.0  - the same from the front
    4. 1.111.0  - left hand, side view
    5. 1.111.0  - first person, right hand (camera space; the cross is where a sword's hilt sits)
The ring is the palm (a 4 x 4 pixel arm is 0.25 block wide); the cross is the grip point the finder chose (source printed in the caption). The skins are the shipped ones, the
lighting is the preview's (tools/item_preview/preview_weapons.py's rasteriser is reused). An upgraded weapon (weapon_meshes_vu) is shown with its upgraded mesh.
"""
import argparse
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "tools", "item_preview"))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import grip_lib as gl          # noqa: E402
import preview_weapons as pw   # noqa: E402
import weapon_classes as wcl   # noqa: E402

ASSETS = gl.ASSETS


def mesh_paths(item_id):
    """(path, kind) of the mesh the game draws for an id: the upgraded one when there is one, the thickened copy when there is one, else the shipped file."""
    for d, kind in (("weapon_meshes_vu_sz", "vu"), ("weapon_meshes_vu", "vu"), ("weapon_meshes_sz", "old"), ("weapon_meshes", "old")):
        p = os.path.join(ASSETS, d, item_id + ".txt")
        if os.path.exists(p):
            return p, kind
    return None, None


def read_quads(path):
    """[(layer, pts[4x5], normal)] in mesh space + place + glow + skin name (vu) from a mesh file."""
    place, glow, skin, quads = None, None, None, []
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "place":
            place = [float(x) for x in v[1:6]]
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "skin":
            skin = v[1]
        elif v[0] == "q":
            quads.append((int(v[1]), np.array([float(x) for x in v[2:22]]).reshape(4, 5), np.array([float(x) for x in v[22:25]])))
        elif v[0] == "qn":
            arr = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
            nn = arr[:, 5:8].sum(axis=0)
            quads.append((int(v[1]), arr[:, :5].copy(), nn / (np.linalg.norm(nn) or 1.0)))
    return place, glow, skin, [q for q in quads if q[0] <= 2]


def hand_frame_quads(quads, place, shift, disp, left, fp):
    """Mesh-space quads -> the hand frame (third person) / camera space (first person); also returns the hand/anchor point and the grip point in that frame."""
    rot, tr, sc = disp
    if left:
        rot, tr = (rot[0], -rot[1], -rot[2]), (-tr[0], tr[1], tr[2])
    R = np.array(gl.rot_matrix(*rot))
    t = np.array(tr) / 16.0 + (np.array(gl.HAND_FP_LEFT if left else gl.HAND_FP) if fp else 0)
    out = []
    for (ly, pts, n) in quads:
        p = pts.copy()
        for k in range(4):
            c = np.array(gl.cube_point(place, *pts[k, :3])) + shift
            p[k, :3] = t + R @ (sc * (c - 0.5))
        out.append((ly, p, R @ (rot_to_cube(place) @ n)))
    return out, R, t, sc


def rot_to_cube(place):
    a = math.radians(place[2] - 90)
    return np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])


def build_tile(quads_h, skin, glowtex, glow, cam, hand_xy, grip_xy, size, label):
    img = pw.render(quads_h, skin, glowtex, glow, cam)
    im = Image.fromarray((img * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    if hand_xy is not None:
        x, y, r = hand_xy
        d.ellipse([x - r, y - r, x + r, y + r], outline=(255, 60, 60), width=2)
    if grip_xy is not None:
        x, y = grip_xy
        d.line([x - 6, y, x + 6, y], fill=(60, 255, 90), width=2)
        d.line([x, y - 6, x, y + 6], fill=(60, 255, 90), width=2)
    d.text((4, 3), label, fill=(255, 255, 255))
    return np.asarray(im, np.float32) / 255


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("ids", nargs="*")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "weapon_grip", "sheet.png"))
    ap.add_argument("--tile", type=int, default=260)
    ap.add_argument("--no-fp", action="store_true")
    ap.add_argument("--grid", type=int, default=1, help="lay the rows out this many per line (for a review sheet of one view per weapon)")
    ap.add_argument("--views",default="0,1,2,3,4", help="which tiles: 0 before, 1 after side, 2 after front, 3 left hand, 4 first person")
    args = ap.parse_args(argv)
    size = args.tile
    pw.W = pw.H = size
    pw.STARS = np.zeros((size, size, 3), np.float32) + [0.05, 0, 0.02]
    classes = gl.classes()
    table = {}
    tp = os.path.join(ASSETS, "weapon_grips.json")
    if os.path.exists(tp):
        table = json.load(open(tp))
    rows = []
    for item_id in args.ids:
        path, kind = mesh_paths(item_id)
        if path is None:
            print("no mesh for", item_id)
            continue
        place, glow, skin_name, quads = read_quads(path)
        pl, directive, xyz, body = gl.load_mesh(path)
        box = gl.body_box(body)
        base = gl.STATE_SUFFIX.sub("", item_id)
        cls = gl.grip_class(item_id, classes, table)
        cg = classes["classes"].get(cls, {"grip": 0.5})["grip"]
        ov = (table.get("fractions") or {}).get(item_id, (table.get("fractions") or {}).get(base))
        explicit = (table.get('points') or {}).get(base) if kind == 'vu' else xyz
        gx, gy, gz, frac, src = gl.grip_point(body, cls, cg, override=ov, directive=directive, explicit=explicit)
        model = json.load(open(os.path.join(ASSETS, "models", "item", (item_id if os.path.exists(os.path.join(ASSETS, "models", "item", item_id + ".json")) else base) + ".json")))
        skin_dir = os.path.join(ASSETS, "textures", "entity", "weapon_vu" if kind == "vu" else "weapon")
        sname = skin_name if kind == "vu" and skin_name else item_id
        skin = pw.tex(os.path.join(skin_dir, sname + ".png"))
        if skin is None:
            skin = pw.tex(os.path.join(ASSETS, "textures", "entity", "weapon", base + ".png"))
        glowtex = pw.tex(os.path.join(skin_dir, sname + "_glow.png"))
        if skin is None:
            print("no skin for", item_id)
            continue
        k = skin.shape[1] / 128.0
        quads = [(ly, np.concatenate([pts[:, :3], pts[:, 3:5] * k], axis=1), n) for (ly, pts, n) in quads]
        glow_t = glow
        gpt = (gx, gy, gz)
        tiles = []
        views = [int(x) for x in args.views.split(",")]
        for vi, (title, (ctx, left, fp), use_shift) in enumerate((("1.110.0", ("thirdperson_righthand", False, False), False), ("1.111.0 side", ("thirdperson_righthand", False, False), True),
                                                  ("front", ("thirdperson_righthand", False, False), True), ("left hand", ("thirdperson_lefthand", True, False), True),
                                                  ("first person", ("firstperson_righthand", False, True), True))):
            if vi not in views or (fp and args.no_fp):
                continue
            disp = gl.read_display(model, ctx)
            tgt = gl.fp_target(disp, left) if fp else gl.display_for(model, ctx)
            shift = np.array(gl.shift_for(place, gpt, tgt)) if use_shift else np.zeros(3)
            if use_shift and fp:
                shift = shift * gl.fp_share(place, box, tuple(shift), disp, left)
            qh, R, t, sc = hand_frame_quads(quads, place, shift, disp, left, fp)
            gc = np.array(gl.cube_point(place, *gpt)) + shift
            grip_h = t + R @ (sc * (gc - 0.5))
            origin = t + R @ (sc * (np.array(tgt) - 0.5))        # = where the hand / the sword-hilt spot is
            if fp:
                cam = (np.zeros(3), np.array([0, 0, -1.0]), np.array([1.0, 0, 0]), np.array([0, 1.0, 0]))
                pw.project = fp_project(size)
            else:
                allp = np.concatenate([q[1][:, :3] for q in qh] + [np.zeros((1, 3))])
                ctr = (allp.min(0) + allp.max(0)) / 2
                ext = float((allp.max(0) - allp.min(0)).max())
                dist = max(2.6, ext * 1.45 / (2 * math.tan(math.radians(25))) + 0.6)
                yaw = {"1.111.0 side": 90, "1.110.0": 90, "left hand": 90, "front": 0}.get(title, 90) + (180 if left and title != "front" else 0)
                cam = view_at(ctr, yaw, 0, dist)
                pw.project = project_default(size)
            hp = pw.project(np.array([origin]), cam)[0]
            gp = pw.project(np.array([grip_h]), cam)[0]
            rad = 0.13 * ((size / 2) / (1.3 if fp else math.tan(math.radians(25)))) / max(hp[2], 0.05)
            label = "%s %s" % (item_id[:22], title) if title == "1.110.0" else title
            if title == "1.111.0 side":
                label = "after (%s %.2f)" % (src, frac) if len(views) > 1 else "%s %s %.2f" % (item_id[:26], src[:4], frac)
            tiles.append(build_tile(qh, skin, glowtex, glow_t, cam, (hp[0], hp[1], rad), (gp[0], gp[1]), size, label))
        rows.append(np.concatenate(tiles, 1))
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    if args.grid > 1:                                                        # rows side by side, `grid` of them per line
        h, w = rows[0].shape[:2]
        while len(rows) % args.grid:
            rows.append(np.zeros((h, w, 3), np.float32))
        rows = [np.concatenate(rows[i:i + args.grid], 1) for i in range(0, len(rows), args.grid)]
    Image.fromarray((np.concatenate(rows, 0) * 255).astype(np.uint8)).save(args.out, optimize=True)
    print("wrote", args.out)


def view_at(target, yaw, pitch, dist):
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    eye = target + dist * np.array([sy * cp, sp, cy * cp])
    f = target - eye
    f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0])
    r /= np.linalg.norm(r)
    return eye, f, r, np.cross(r, f)


def project_default(size):
    def project(p, cam):
        eye, f, r, u = cam
        d = p - eye
        z = d @ f
        k = (size / 2) / math.tan(math.radians(25))
        return np.stack([size / 2 + (d @ r) / z * k, size / 2 - (d @ u) / z * k, z], axis=-1)
    return project


def fp_project(size):
    def project(p, cam):
        z = -p[..., 2]
        k = (size / 2) / 1.3                                                   # a 16:9 window: +-1.25 across, the tile shows +-1.3 both ways
        return np.stack([size / 2 + p[..., 0] / z * k - 0.35 * k * 0.5, size / 2 - (p[..., 1] + 0.1) / z * k, z], axis=-1)
    return project


if __name__ == "__main__":
    main()
