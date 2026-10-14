"""weapon_sheet: contact-sheet previews of the baked weapon meshes for the size / orientation audit (no game, no GPU).

    python tools/relics/audit_weapon_scale.py --sheet out.png [ids...]          (the audit drives this module)

One tile per weapon:
  left   the weapon in the item sprite's plane (mesh -x is the LEFT of this picture = FORWARD; the tip is up-right), flat-lit, coloured from its
         skin, with a 1-block ruler and the class's target length as a green bar;
  right  the held view: the real third-person chain of a right hand (arm pose, ItemInHandLayer, the model json's thirdperson_righthand
         display transform) projected on the side plane, next to Steve (1.8 m, the model of the player: head 8, body 12, arms and legs 12 pixels).
         The player faces right; the weapon's forward side is the side of the picture that faces the front.
A red frame marks a flagged / too-small weapon, a yellow one a weapon over the class maximum.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import weapon_orientation as wo  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
ENTITY_SCALE = 0.9375                       # PlayerRenderer.scale


# ================================================================ small matrices (4x4, column vectors)
def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def Rx(d):
    c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1.0]])


def Ry(d):
    c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1.0]])


def Rz(d):
    c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1.0]])


def S(x, y, z):
    return np.diag([x, y, z, 1.0])


def hand_matrix(rotation, translation, scale):
    """Item cube (0..1) -> player model space (blocks, y DOWN, forward = -z) for the right hand, as ItemInHandLayer + ItemTransform.apply +
    ItemRenderer do it: arm.translateAndRotate (xRot -18 degrees when an item is held), Rx(-90), Ry(180), T(1/16, 1/8, -5/8), the display
    transform T(t/16) Rx Ry Rz S, and finally T(-1/2)."""
    arm = T(-5 / 16, 2 / 16, 0) @ Rx(-18.0)
    disp = T(translation[0] / 16, translation[1] / 16, translation[2] / 16) @ Rx(rotation[0]) @ Ry(rotation[1]) @ Rz(rotation[2]) @ S(*scale)
    return arm @ Rx(-90) @ Ry(180) @ T(1 / 16, 0.125, -0.625) @ disp @ T(-0.5, -0.5, -0.5)


def place_matrix(place):
    """Mesh space -> item cube: translate(ox, oy, .5) Rz(angle - 90) scale(sc) translate(0, -ymin, 0)  (WeaponRenderer.renderByItem)."""
    ox, oy, ang, sc, ymin = place
    return T(ox, oy, 0.5) @ Rz(ang - 90.0) @ S(sc, sc, sc) @ T(0, -ymin, 0)


def display_of(item_id, models_dir=None):
    """(rotation, translation, scale) of thirdperson_righthand from the model json of the id (the base id for a state mesh), vanilla handheld else."""
    import json
    import weapon_classes as wc
    d = models_dir or os.path.join(ASSETS, "models", "item")
    for i in (item_id, wc.base_id(item_id)):
        p = os.path.join(d, i + ".json")
        if os.path.exists(p):
            disp = json.load(open(p, encoding="utf-8")).get("display", {}).get("thirdperson_righthand")
            if disp:
                return (tuple(disp.get("rotation", (0, -90, 55))), tuple(disp.get("translation", (0, 4, 0.5))), tuple(disp.get("scale", (0.85,) * 3)))
    return (0, -90, 55), (0, 4, 0.5), (0.85, 0.85, 0.85)


# ================================================================ drawing
_TEX = {}


def skin_of(item_id):
    import weapon_classes as wc
    for i in (item_id, wc.base_id(item_id)):
        p = os.path.join(ASSETS, "textures", "entity", "weapon", i + ".png")
        if os.path.exists(p):
            if p not in _TEX:
                _TEX[p] = np.asarray(Image.open(p).convert("RGBA"), np.float32) / 255.0
            return _TEX[p]
    return None


def quad_colours(raw_lines, skin):
    """One RGB colour per body quad: the skin at the quad's UV centre (mean of nearby texels), grey when there is no skin."""
    cols = []
    for line in raw_lines:
        v = line.split()
        if not v or v[0] not in ("q", "qn") or int(v[1]) != 0:
            continue
        st = 5 if v[0] == "q" else 8
        us = [float(v[2 + k * st + 3]) for k in range(4)]
        vs = [float(v[2 + k * st + 4]) for k in range(4)]
        if skin is None:
            cols.append((0.6, 0.6, 0.65))
            continue
        th, tw = skin.shape[:2]
        u, w = sum(us) / 4 * tw / 128.0, sum(vs) / 4 * th / 128.0
        x0, y0 = int(max(0, min(tw - 1, u))), int(max(0, min(th - 1, w)))
        patch = skin[max(0, y0 - 1):y0 + 2, max(0, x0 - 1):x0 + 2].reshape(-1, 4)
        a = patch[:, 3:4]
        c = (patch[:, :3] * a).sum(0) / max(a.sum(), 1e-6) if a.sum() > 0 else np.array([0.5, 0.5, 0.55])
        cols.append(tuple(c))
    return cols


def _shade(n):
    l1 = np.array([0.35, 0.8, 0.6])
    l1 /= np.linalg.norm(l1)
    return 0.45 + 0.55 * max(0.0, float(np.dot(n, l1)))


def draw_polys(draw, polys, to_px):
    for depth, pts, col in sorted(polys, key=lambda p: p[0]):
        draw.polygon([to_px(p) for p in pts], fill=col)


def mesh_polys(mesh_data, item_id, matrix, view):
    """Body quads transformed by `matrix` (mesh space -> some space) into (depth, [(u, v) * 4] , colour) for a view function
    view(P) -> (u, v, depth)."""
    skin = skin_of(item_id)
    cols = quad_colours(mesh_data["lines"], skin)
    polys = []
    qi = 0
    for layer, P, N in mesh_data["quads"]:
        if layer != 0:
            continue
        pts = [matrix @ np.array([p[0], p[1], p[2], 1.0]) for p in P]
        n3 = matrix[:3, :3] @ np.array(N[0])
        nn = n3 / max(np.linalg.norm(n3), 1e-9)
        c = np.array(cols[qi] if qi < len(cols) else (0.6, 0.6, 0.6)) * _shade(nn)
        qi += 1
        proj = [view(p[:3]) for p in pts]
        polys.append((sum(p[2] for p in proj) / 4, [(p[0], p[1]) for p in proj],
                      tuple(int(max(0, min(255, v * 255))) for v in c)))
    return polys


def steve_polys():
    """Steve's side silhouette in model space (blocks, y down, forward = -z): list of (name, [(z, y) points]) rectangles, the right arm excluded."""
    def rect(z0, z1, y0, y1):
        return [(z0, y0), (z1, y0), (z1, y1), (z0, y1)]
    k = 1 / 16.0
    return {
        "head": rect(-4 * k, 4 * k, -8 * k, 0), "body": rect(-2 * k, 2 * k, 0, 12 * k), "legs": rect(-2 * k, 2 * k, 12 * k, 24 * k),
    }


def arm_poly():
    """The right arm (pivot (-5, 2) px, 4 x 12 px, rotated Rx(-18)) as an (z, y) polygon in model space."""
    k = 1 / 16.0
    pts = [(-2 * k, -2 * k), (2 * k, -2 * k), (2 * k, 10 * k), (-2 * k, 10 * k)]
    a = math.radians(-18.0)
    out = []
    for z, y in pts:                              # rotation about the x axis through the pivot: y' = y cos - z sin, z' = y sin + z cos
        out.append((y * math.sin(a) + z * math.cos(a), 2 * k + y * math.cos(a) - z * math.sin(a)))
    return out


def held_tile(mesh_data, item_id, w=330, h=360, target=None):
    """The held view: Steve and the weapon, side on, the player facing right. Returns a PIL image and the weapon's apparent length."""
    rot, tr, sc = display_of(item_id)
    M = hand_matrix(rot, tr, sc) @ place_matrix(mesh_data["place"])
    img = Image.new("RGB", (w, h), (74, 80, 96))
    d = ImageDraw.Draw(img)
    pxb = 118.0                                    # pixels per block
    ground = h - 22
    cx = w * 0.42
    # model space -> screen: forward (-z) is right, up (-y) is up; feet are at model y = 1.5 (24 px)
    def to_px(p):
        return (cx - p[0] * ENTITY_SCALE * pxb, ground - (1.5 - p[1]) * ENTITY_SCALE * pxb)
    d.line([(8, ground), (w - 8, ground)], fill=(40, 44, 54), width=2)
    for name, pts in steve_polys().items():
        d.polygon([to_px((z, y)) for z, y in pts], fill=(56, 64, 84) if name != "head" else (74, 90, 116))
    # z, y -> use to_px with (z, y): to_px takes p[0]=z? keep explicit
    def zy(pt):
        return (cx - pt[0] * ENTITY_SCALE * pxb, ground - (1.5 - pt[1]) * ENTITY_SCALE * pxb)
    d.polygon([zy(p) for p in arm_poly()], fill=(88, 100, 128))
    # the weapon: view = (screen x from -z, screen y from -y, depth = x so that the +x side is drawn first)
    def view(P):
        return (cx - P[2] * ENTITY_SCALE * pxb, ground - (1.5 - P[1]) * ENTITY_SCALE * pxb, -P[0])
    polys = mesh_polys(mesh_data, item_id, M, lambda P: view(P))
    for depth, pts, col in sorted(polys, key=lambda p: p[0]):
        d.polygon(pts, fill=col)
    # a 1.8 m ruler beside Steve is implied by Steve; mark the target apparent length from the hand when given
    quads = [q for q in mesh_data["quads"] if q[0] == 0] or mesh_data["quads"]
    ys = [p[1] for _l, P, _n in quads for p in P]
    ox, oy, ang, scl, ymin = mesh_data["place"]
    length = (max(ys) - ymin) * scl * sc[0]
    return img, length


def front_tile(mesh_data, item_id, w=330, h=360, cls_range=None, grip=0.25):
    """The weapon in the item sprite plane (mesh -x = left), flat-lit and coloured, with a 1-block ruler and the class range bar."""
    img = Image.new("RGB", (w, h), (60, 66, 82))
    d = ImageDraw.Draw(img)
    M = place_matrix(mesh_data["place"])
    quads = [q for q in mesh_data["quads"] if q[0] == 0] or mesh_data["quads"]
    pts = np.array([(M @ np.array([p[0], p[1], p[2], 1.0]))[:3] for _l, P, _n in quads for p in P])
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    span = max(hi[0] - lo[0], hi[1] - lo[1], 1.0)
    k = (min(w, h) - 70) / span                     # px per item-cube unit
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2

    def view(P):
        return (w / 2 + (P[0] - cx) * k, h / 2 - 8 - (P[1] - cy) * k, P[2])
    for depth, pp, col in sorted(mesh_polys(mesh_data, item_id, M, view), key=lambda p: p[0]):
        d.polygon(pp, fill=col)
    # 1-block ruler (item-cube units = blocks before the display scale)
    x0, y0 = 14, h - 18
    d.line([(x0, y0), (x0 + k, y0)], fill=(255, 255, 255), width=3)
    d.line([(x0, y0 - 4), (x0, y0 + 4)], fill=(255, 255, 255), width=2)
    d.line([(x0 + k, y0 - 4), (x0 + k, y0 + 4)], fill=(255, 255, 255), width=2)
    d.text((x0 + 4, y0 - 16), "1 block", fill=(255, 255, 255))
    if cls_range:                                    # the target range along the axis from the butt, in blocks / display scale
        sc3 = mesh_data.get("s3p", 0.85)
        ox, oy, ang, scl, ymin = mesh_data["place"]
        a = math.radians(ang)
        bx, by = view((ox, oy, 0))[:2]
        for frac, colr in ((cls_range[0] / sc3, (70, 220, 110)), (cls_range[1] / sc3, (70, 220, 110))):
            ex, ey = bx + math.cos(a) * frac * k, by - math.sin(a) * frac * k
            nx, ny = -math.sin(a) * 9, -math.cos(a) * 9
            d.line([(ex - nx, ey - ny), (ex + nx, ey + ny)], fill=colr, width=2)
        d.line([(bx + math.cos(a) * cls_range[0] / sc3 * k, by - math.sin(a) * cls_range[0] / sc3 * k),
                (bx + math.cos(a) * cls_range[1] / sc3 * k, by - math.sin(a) * cls_range[1] / sc3 * k)], fill=(70, 220, 110), width=1)
    return img


def tile(item_id, mesh_data, label_lines, cls_range=None, frame=None, s3p=0.85):
    mesh_data = dict(mesh_data)
    mesh_data["s3p"] = s3p
    a = front_tile(mesh_data, item_id, cls_range=cls_range)
    b, _ = held_tile(mesh_data, item_id)
    t = Image.new("RGB", (a.width + b.width, a.height + 66), (30, 32, 40))
    t.paste(a, (0, 0))
    t.paste(b, (a.width, 0))
    d = ImageDraw.Draw(t)
    for i, line in enumerate(label_lines[:4]):
        d.text((6, a.height + 4 + 15 * i), line, fill=(235, 235, 240))
    if frame:
        d.rectangle([0, 0, t.width - 1, t.height - 1], outline=frame, width=4)
    return t


def sheet(tiles, cols=2, out="sheet.png"):
    if not tiles:
        return None
    tw, th = tiles[0].size
    rows = (len(tiles) + cols - 1) // cols
    img = Image.new("RGB", (tw * cols, th * rows), (20, 22, 28))
    for i, t in enumerate(tiles):
        img.paste(t, ((i % cols) * tw, (i // cols) * th))
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    img.save(out)
    return out
