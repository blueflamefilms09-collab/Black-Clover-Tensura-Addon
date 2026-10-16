"""Software preview of the relics AS HELD OBJECTS (no game, no GPU): the same maths as client/relics/hand/HandItemRenderer, in the same frames the game uses.

    python3 tools/relics/hand_preview.py <relic id> [<relic id> ...] [--out sheet.png]        a row of tiles per relic
    python3 tools/relics/hand_preview.py --sheet <name> [--ids a,b,c] [--out file.png]        a contact sheet (names: see SHEETS)

A tile row shows, left to right: third person (front three-quarter), third person (from the side), first person, on the ground, in an item frame, the inventory sprite.
What is replicated: the pose chain of ItemInHandLayer (arm at rest tilted 18 degrees forward as when it holds an item, then the turn that stands an item on the hand), the
first person offset of ItemInHandRenderer, the ground hover and the item frame turn. What is not: the player's animation (walking, swinging), the first person arm and the
exact field of view (an orthographic view stands in for the perspective). It is for judging how a thing sits in a hand, not a pixel-exact render.
"""
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview_worn as P  # noqa: E402
import relic_inventory as RI  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
HAND = os.path.join(ASSETS, "relic_hand")
WORN = os.path.join(ASSETS, "relic_worn")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
CTXS = ["ground", "fixed", "tp_right", "tp_left", "fp_right", "fp_left", "head"]


# ---------------------------------------------------------------- matrices (column vectors, 4x4)
def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def S(a, b=None, c=None):
    m = np.eye(4)
    m[0, 0], m[1, 1], m[2, 2] = a, a if b is None else b, a if c is None else c
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


def Rxyz(x, y, z):
    """JOML rotationXYZ: the matrix is Rx * Ry * Rz (a vector is turned about z first)."""
    return Rx(x) @ Ry(y) @ Rz(z)


# ---------------------------------------------------------------- the transform file (a port of HandTransforms.java)
class Styles:
    def __init__(self, path=None):
        self.raw, self.uses = {}, {}
        path = path or os.path.join(HAND, "transforms.txt")
        if os.path.exists(path):
            self._parse(open(path, encoding="utf8").read().splitlines())

    @staticmethod
    def _kv(tokens):
        out = {}
        for t in tokens:
            k, _, v = t.partition("=")
            out[k] = v
        return out

    def _parse(self, lines):
        for line in lines:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            v = line.split()
            if v[0] == "style":
                self.raw[v[1]] = {"kv": self._kv(v[2:]), "xf": {}}
            elif v[0] == "xf":
                self.raw[v[1]]["xf"][v[2]] = tuple(float(x) for x in v[3:10])
            elif v[0] == "use":
                for key in v[1].split(","):
                    self.uses[key] = (v[2], self._kv(v[3:]))

    def resolve(self, name, over=None):
        chain, kv, xf = [], {}, {}
        n = name
        order = []
        while n:
            order.append(n)
            n = self.raw[n]["kv"].get("from")
        for n in reversed(order):
            kv.update({k: v for k, v in self.raw[n]["kv"].items() if k != "from"})
            xf.update(self.raw[n]["xf"])
        if over:
            kv.update(over)
        fit = dict(size=0.9, hold=(0, 0, 0), pre=(0, 0, 0), sway=0.0, only=0)
        if "size" in kv:
            fit["size"] = float(kv["size"])
        if "hold" in kv:
            fit["hold"] = tuple(float(x) for x in kv["hold"].split(","))
        if "pre" in kv:
            fit["pre"] = tuple(float(x) for x in kv["pre"].split(","))
        if "sway" in kv:
            fit["sway"] = float(kv["sway"])
        if "only" in kv:
            fit["only"] = int(kv["only"])
        return fit, xf

    def style_for(self, rid, slot, look):
        for key in ("id:" + rid, "look:%s:%s" % (slot, look), "slot:" + slot):
            if key in self.uses:
                name, over = self.uses[key]
                return self.resolve(name, over), name
        return self.resolve("default"), "default"


# ---------------------------------------------------------------- the pieces of a relic
def read_catalog(path, key):
    out = {}
    if os.path.exists(path):
        for line in open(path, encoding="utf8"):
            v = line.split()
            if len(v) >= 3 and v[0] == key:
                out[v[1]] = v[2:]
    return out


_CACHE = {}


def catalogs():
    if not _CACHE:
        _CACHE["sig"] = read_catalog(os.path.join(HAND, "catalog.txt"), "sig")
        _CACHE["relic"] = read_catalog(os.path.join(WORN, "catalog.txt"), "relic")
        looks = {}
        for line in open(os.path.join(WORN, "catalog.txt"), encoding="utf8"):
            v = line.split()
            if len(v) >= 4 and v[0] == "look":
                looks[(v[1], v[2])] = v[3:]
        _CACHE["look"] = looks
        _CACHE["styles"] = Styles()
    return _CACHE


def model_path(name):
    p = os.path.join(HAND, name + ".txt")
    return p if os.path.exists(p) else os.path.join(WORN, name + ".txt")


def pieces_of(rid, slot, look):
    """[(file, anchor or None, mirror)] and where they came from ('sig' | 'worn' | None)."""
    c = catalogs()
    if rid in c["sig"]:
        return _pp(c["sig"][rid]), "sig"
    if slot in ("CONSUMABLE", "MAIN_HAND"):
        return [], None
    names = c["relic"].get(rid) or c["look"].get((slot, look))
    if not names:
        return [], None
    return _pp(names), "worn"


def _pp(specs):
    out = []
    for s in specs:
        mirror = s.endswith("~")
        s = s.rstrip("~")
        name, _, anchor = s.partition("@")
        out.append((model_path(name), anchor or None, mirror))
    return out


def load_pieces(pieces):
    """[(quads, anchor, mirror)]; quads from preview_worn.parse."""
    out = []
    for (f, anchor, mirror) in pieces:
        a, quads, _g = P.parse(f)
        out.append((quads, anchor or a, mirror))
    return out


def piece_points(quads, anchor, mirror):
    off = np.array(P.ANCHORS[anchor], float)
    res = []
    for (g, slot, flags, Pq, UV, N) in quads:
        Pm, Nm = Pq.copy(), N.copy()
        if mirror:
            Pm[:, 0] *= -1
            Nm[:, 0] *= -1
        res.append((slot, flags, Pm + off, UV, Nm))
    return res


def bounds(loaded):
    lo, hi = np.full(3, 1e9), np.full(3, -1e9)
    for (quads, anchor, mirror) in loaded:
        for (_s, _f, Pm, _u, _n) in piece_points(quads, anchor, mirror):
            lo, hi = np.minimum(lo, Pm.min(axis=0)), np.maximum(hi, Pm.max(axis=0))
    if lo[0] > hi[0]:
        lo, hi = np.full(3, -8.0), np.full(3, 8.0)
    return lo, hi


# ---------------------------------------------------------------- the renderer maths
def object_matrix(loaded, fit, xf, t=0.0, in_hand=False):
    """Layer space px -> item cube space (blocks, origin at the cube corner), exactly as HandItemRenderer.drawForm composes it."""
    lo, hi = bounds(loaded)
    ex, ey, ez = hi - lo
    sc = fit["size"] / max(0.001, max(ex, ey, ez))
    cx, cy, cz = (lo + hi) / 2
    rx, ry, rz, tx, ty, tz, s = xf
    m = T(0.5, 0.5, 0.5) @ T(tx / 16, ty / 16, tz / 16) @ Rxyz(rx, ry, rz) @ S(s)
    pr = fit["pre"]
    if any(pr):
        m = m @ Rxyz(*pr)
    if in_hand:
        if fit["sway"]:
            m = m @ Rz(fit["sway"] * math.sin(t * 0.11))
        hx, hy, hz = fit["hold"]
        m = m @ T(-hx * ex * sc / 2, -hy * ey * sc / 2, -hz * ez * sc / 2)
    m = m @ Rx(180) @ S(sc) @ T(-cx, -cy, -cz)
    return m


def tp_hand(right=True, arm_x=-18.0):
    """Item cube centre frame -> layer px for a third person hand (ItemInHandLayer.renderArmWithItem): the arm pivot, the arm's pose, then Rx(-90) Ry(180) and the offset."""
    side = 1 if right else -1
    pivot = (-5, 2, 0) if right else (5, 2, 0)
    m = S(16) @ T(pivot[0] / 16, pivot[1] / 16, pivot[2] / 16) @ Rx(arm_x) @ Rx(-90) @ Ry(180) @ T((1 if right else -1) * (1 / 16), 0.125, -0.625)
    return m


def world_quads(loaded, M, tint_cols):
    out = []
    for (quads, anchor, mirror) in loaded:
        for (slot, flags, Pm, UV, Nm) in piece_points(quads, anchor, mirror):
            Ph = np.c_[Pm, np.ones(4)] @ M.T
            Nw = Nm @ M[:3, :3].T
            out.append((Ph[:, :3], UV, Nw, tint_cols[slot], flags))
    return out


# ---------------------------------------------------------------- a small scene renderer (orthographic, z-buffered)
class Scene:
    def __init__(self, size, bg=(0.33, 0.36, 0.43)):
        self.W, self.H = size
        self.img = np.zeros((self.H, self.W, 3), np.float32) + np.array(bg, np.float32)
        self.img *= (0.85 + 0.3 * np.linspace(1, 0, self.H, dtype=np.float32))[:, None, None]
        self.zb = np.full((self.H, self.W), 1e9, np.float32)

    def draw(self, quads, R, centre, scale, atlas, emit=True):
        W, H = self.W, self.H

        def proj(Pm):
            Q = (Pm - centre) @ R.T
            return np.stack([W / 2 + Q[:, 0] * scale, H / 2 + Q[:, 1] * scale, Q[:, 2] * scale], axis=1)

        adds = []
        for (Pm, UV, Nm, tint, flags) in quads:
            S_ = proj(Pm)
            Nv = Nm @ R.T
            for tri in ((0, 1, 2), (0, 2, 3)):
                ii = list(tri)
                P._raster(self.img, self.zb, S_[ii], UV[ii], Nv[ii], atlas, tint, "solid", 0.0)
            if flags & 1 and emit:
                adds.append((S_, UV, Nv, tint))
        for (S_, UV, Nv, tint) in adds:
            for tri in ((0, 1, 2), (0, 2, 3)):
                ii = list(tri)
                P._raster(self.img, self.zb, S_[ii], UV[ii], Nv[ii], atlas, tint * 0.9, "add", 0.5)

    def draw_cam(self, quads, f, cx, cy, atlas):
        """Perspective draw of quads given in camera space (x right, y up, the camera looks down -z): f the focal length in pixels, (cx, cy) the principal point."""
        adds = []
        for (Pm, UV, Nm, tint, flags) in quads:
            depth = np.maximum(-Pm[:, 2], 0.05)
            S_ = np.stack([cx + f * Pm[:, 0] / depth, cy - f * Pm[:, 1] / depth, depth * 100.0], axis=1)
            Nv = Nm * np.array([1, -1, -1])
            for tri in ((0, 1, 2), (0, 2, 3)):
                ii = list(tri)
                P._raster(self.img, self.zb, S_[ii], UV[ii], Nv[ii], atlas, tint, "solid", 0.0)
            if flags & 1:
                adds.append((S_, UV, Nv, tint))
        for (S_, UV, Nv, tint) in adds:
            for tri in ((0, 1, 2), (0, 2, 3)):
                ii = list(tri)
                P._raster(self.img, self.zb, S_[ii], UV[ii], Nv[ii], atlas, tint * 0.9, "add", 0.5)

    def boxes(self, boxes, R, centre, scale, M=None):
        """boxes = [(x0, y0, z0, x1, y1, z1, rgb)] drawn flat-shaded; M an optional 4x4 on their points."""
        W, H = self.W, self.H
        for b in boxes:
            for (Pq, n) in P.box_quads(b[:6]):
                if M is not None:
                    Pq = (np.c_[Pq, np.ones(4)] @ M.T)[:, :3]
                    n = M[:3, :3] @ n
                Q = (Pq - centre) @ R.T
                S_ = np.stack([W / 2 + Q[:, 0] * scale, H / 2 + Q[:, 1] * scale, Q[:, 2] * scale], axis=1)
                Nv = np.tile(n @ R.T, (4, 1))
                for tri in ((0, 1, 2), (0, 2, 3)):
                    ii = list(tri)
                    P._raster(self.img, self.zb, S_[ii], np.zeros((3, 2)), Nv[ii], None, np.array(b[6]), "solid", 0.0)

    def image(self):
        return Image.fromarray((np.clip(self.img, 0, 1) * 255).astype(np.uint8))


def figure_scene(loaded, form, yaw, pitch, panel, atlas, cols, right=True, close=False):
    """The third person tile: a Steve-like figure with the arm raised as it is when it holds an item, the object in its hand."""
    fit, xfs = form
    xf = xfs["tp_right" if right else "tp_left"]
    Mo = tp_hand(right) @ T(-0.5, -0.5, -0.5) @ object_matrix(loaded, fit, xf, in_hand=True)
    sc = Scene(panel)
    R = P.rot(yaw, pitch)
    centre = np.array([0.0, 8.0, -4.0])
    scale = min(panel[0], panel[1]) / 34.0
    if close:                                                                     # a close view of the hand
        centre = tp_hand(right)[:3, 3] + np.array([0.0, 3.0, 0.0])
        scale = min(panel[0], panel[1]) / 19.0
    fig = P.figure_boxes()
    keep = [fig[0], fig[1], fig[4], fig[5], fig[6], fig[7], fig[8]]                 # head, body, legs, shoes, hair
    sc.boxes([b[:7] for b in keep], R, centre, scale)
    other = fig[3] if right else fig[2]                                           # the arm that holds nothing hangs at rest
    sc.boxes([other[:7]], R, centre, scale)
    mine = fig[2] if right else fig[3]
    pivot = (-5, 2, 0) if right else (5, 2, 0)
    Am = T(*pivot) @ Rx(-18.0) @ T(-pivot[0], -pivot[1], -pivot[2])                # the arm that holds the item, raised a little
    sc.boxes([mine[:7]], R, centre, scale, M=Am)
    sc.draw(world_quads(loaded, Mo, cols), R, centre, scale, atlas)
    return sc.image()


def first_person_scene(loaded, form, panel, atlas, cols, right=True):
    """The first person tile: the camera looks down -z with a 70 degree field; the item sits where ItemInHandRenderer puts it (0.56 right, 0.52 down, 0.72 ahead). The tile shows
    the lower right part of the screen, perspective drawn, so a sprite turned [0 -90 25] is seen as foreshortened as it is in the game."""
    fit, xfs = form
    xf = xfs["fp_right" if right else "fp_left"]
    side = 1 if right else -1
    Mo = T(0.56 * side, -0.52, -0.72) @ T(-0.5, -0.5, -0.5) @ object_matrix(loaded, fit, xf, in_hand=True)
    sc = Scene(panel)
    f = panel[1] / math.tan(math.radians(35.0))
    quads = world_quads(loaded, Mo, cols)
    sc.draw_cam(quads, f, panel[0] / 2 - side * f * 0.62 / 0.8 + side * 0.0, panel[1] / 2 - f * 0.30 / 0.8, atlas)
    return sc.image()


def ground_scene(loaded, form, panel, atlas, cols):
    fit, xfs = form
    xf = xfs["ground"]
    Mo = T(0, 0.25 * xf[6], 0) @ T(-0.5, -0.5, -0.5) @ object_matrix(loaded, fit, xf)
    Mf = S(1, -1, 1) @ Mo                                                         # y up in the item frame, down in the raster
    Mf[1, 3] -= 0.0
    sc = Scene(panel)
    R = P.rot(25, 24)
    quads = world_quads(loaded, Mf, cols)
    scale = panel[1] / 1.5
    centre = np.array([0.0, -0.3, 0.0])
    # the ground: a flat slab; the item hovers 0.25 of its scale above it (ItemEntityRenderer), the slab top is at y = 0
    sc.boxes([(-0.6, 0.0, -0.6, 0.6, 0.04, 0.6, (0.25, 0.34, 0.22))], R, centre, scale)
    sc.draw(quads, R, centre, scale, atlas)
    return sc.image()


def frame_scene(loaded, form, panel, atlas, cols):
    """An item frame seen from the front: the item turned as ItemFrameRenderer turns it (its local +z points at the wall, so the viewer sees its -z side)."""
    fit, xfs = form
    xf = xfs["fixed"]
    Mo = S(0.5) @ T(-0.5, -0.5, -0.5) @ object_matrix(loaded, fit, xf)          # frame scale .5, then the display transform
    Mf = S(1, -1, 1) @ Mo
    sc = Scene(panel)
    R = np.eye(3)                                                                # camera on the -z side looking at +z: x right, y down in the raster
    quads = []
    for (Pm, UV, Nm, tint, flags) in world_quads(loaded, Mf, cols):
        quads.append((Pm * np.array([-1, 1, 1]), UV, Nm * np.array([-1, 1, 1]), tint, flags))
    scale = panel[1] / 0.75
    sc.boxes([(-0.42, -0.42, 0.20, 0.42, 0.42, 0.26, (0.45, 0.30, 0.18))], R, np.zeros(3), scale)
    sc.draw(quads, R, np.zeros(3), scale, atlas)
    return sc.image()


def sprite_tile(rid, panel):
    im = Image.new("RGB", panel, (84, 92, 110))
    p = os.path.join(ITEM_TEX, rid + ".png")
    if os.path.exists(p):
        s = Image.open(p).convert("RGBA")
        k = max(1, min(panel) // max(s.size) - 1)
        s = s.resize((s.width * k, s.height * k), Image.NEAREST)
        im.paste(s, ((panel[0] - s.width) // 2, (panel[1] - s.height) // 2), s)
    return im


def prepare(d, atlas=None):
    """Everything the tiles of one relic def need, or None when it is held as its sprite: (loaded pieces, (fit, transforms), colours, style name, source, atlas)."""
    c = catalogs()
    pcs, src = pieces_of(d["id"], d["slot"], d["look"])
    if not pcs:
        return None
    if atlas is None:
        atlas = np.asarray(Image.open(P.ATLAS).convert("RGBA"), np.float32) / 255
    (fit, xfs), sname = c["styles"].style_for(d["id"], d["slot"], d["look"])
    if fit["only"] and len(pcs) > fit["only"]:
        pcs = pcs[:fit["only"]]
    loaded = load_pieces(pcs)
    pal = d["palette"] or [0x8C8C9C, 0xD8C070, 0xFFE9A0]
    cols = P.slot_colors(*pal)
    form = (fit, {k: xfs.get(k, (0, 0, 0, 0, 0, 0, 1)) for k in CTXS})
    return loaded, form, cols, sname, src, atlas


def relic_row(d, panel=(170, 210), atlas=None):
    """The tiles of one relic def (a dict from relic_inventory) as one PIL image, or None when it is held as its sprite."""
    pr = prepare(d, atlas)
    if pr is None:
        return None
    loaded, form, cols, sname, src, atlas = pr
    tiles = [figure_scene(loaded, form, 25, 8, panel, atlas, cols), figure_scene(loaded, form, 15, 5, panel, atlas, cols, close=True),
             figure_scene(loaded, form, 90, 0, panel, atlas, cols, close=True), first_person_scene(loaded, form, panel, atlas, cols), ground_scene(loaded, form, panel, atlas, cols),
             frame_scene(loaded, form, panel, atlas, cols), sprite_tile(d["id"], panel)]
    row = Image.new("RGB", (panel[0] * len(tiles), panel[1]))
    for i, t in enumerate(tiles):
        row.paste(t, (i * panel[0], 0))
    dr = ImageDraw.Draw(row)
    dr.text((6, 4), "%s  [%s %s]  style %s  (%s)" % (d["id"], d["slot"], d["look"], sname, src), fill=(255, 255, 255))
    return row


def held_cell(d, panel=(120, 140), atlas=None):
    """A small overview cell of one relic: the third person close view beside the inventory sprite (None when it is held as its sprite)."""
    pr = prepare(d, atlas)
    if pr is None:
        return None
    loaded, form, cols, sname, src, atlas = pr
    t = figure_scene(loaded, form, 15, 5, panel, atlas, cols, close=True)
    sp = sprite_tile(d["id"], (panel[0] // 2, panel[1] // 2))
    cell = Image.new("RGB", (panel[0] + panel[0] // 2, panel[1]), (30, 32, 40))
    cell.paste(t, (0, 0))
    cell.paste(sp, (panel[0], 0))
    ImageDraw.Draw(cell).text((4, panel[1] - 12), d["id"][:30], fill=(255, 255, 255))
    ImageDraw.Draw(cell).text((panel[0] + 3, panel[1] // 2 + 4), sname + "\n" + (src or ""), fill=(200, 210, 230))
    return cell


def model_sheet(names, palette=(0x8C8C9C, 0xD8C070, 0xFFE9A0), views=("front", "three", "side", "back"), panel=(220, 240), label=""):
    """A turntable of one object (its hand model, or a worn model of the same name) without figure or hand: front, three-quarter, side, back."""
    atlas = np.asarray(Image.open(P.ATLAS).convert("RGBA"), np.float32) / 255
    loaded = load_pieces(_pp(names))
    lo, hi = bounds(loaded)
    ext = float(max(hi - lo))
    centre = (lo + hi) / 2
    cols = P.slot_colors(*palette)
    tiles = []
    for v in views:
        yaw, pitch = P.VIEWS[v]
        sc = Scene(panel)
        R = P.rot(yaw, pitch)
        quads = world_quads(loaded, np.eye(4), cols)
        sc.draw(quads, R, centre, min(panel) * 0.82 / ext, atlas)
        tiles.append(sc.image())
    row = Image.new("RGB", (panel[0] * len(tiles), panel[1]))
    for i, t in enumerate(tiles):
        row.paste(t, (i * panel[0], 0))
    ImageDraw.Draw(row).text((6, 4), "%s   %.1f x %.1f x %.1f px" % (label or " ".join(names), *(hi - lo)), fill=(255, 255, 255))
    return row


def palette_of_model(name):
    """The palette of the first relic the hand catalog gives this model to (so a turntable shows the colours it will have), else a neutral one."""
    rs, _ = RI.relics()
    by = {r["id"]: r for r in rs}
    for rid, names in catalogs()["sig"].items():
        if name in [n.split("@")[0].rstrip("~") for n in names] and rid in by and by[rid]["palette"]:
            return tuple(by[rid]["palette"])
    return (0x8C8C9C, 0xD8C070, 0xFFE9A0)


def model_grid(names, cols=3, panel=(150, 170), views=("front", "three")):
    """Several objects on one sheet, two views each, in the colours of the relic that holds them."""
    tiles = []
    for n in names:
        row = model_sheet([n], palette_of_model(n), views, panel, n)
        tiles.append(row)
    rows = (len(tiles) + cols - 1) // cols
    w = tiles[0].width
    sheet = Image.new("RGB", (w * cols, tiles[0].height * rows), (30, 32, 40))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % cols) * w, (i // cols) * t.height))
    return sheet


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("ids", nargs="*")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "relics_preview", "hand.png"))
    ap.add_argument("--panel", default="170x210")
    ap.add_argument("--model", default="", help="comma list of model names: a turntable of the object itself instead of the held tiles")
    ap.add_argument("--palette", default="8C8C9C,D8C070,FFE9A0")
    a = ap.parse_args(argv)
    if a.model.startswith("grid:"):
        names = a.model[5:].split(",")
        sheet = model_grid(names, cols=3)
        os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
        sheet.save(a.out)
        print("wrote", a.out)
        return 0
    if a.model:
        pal = tuple(int(x, 16) for x in a.palette.split(","))
        rows = [model_sheet([n], pal) for n in a.model.split(",")]
        sheet = Image.new("RGB", (rows[0].width, sum(r.height for r in rows)))
        y = 0
        for r in rows:
            sheet.paste(r, (0, y))
            y += r.height
        os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
        sheet.save(a.out)
        print("wrote", a.out)
        return 0
    rs, _ = RI.relics()
    by = {r["id"]: r for r in rs}
    pw, ph = (int(x) for x in a.panel.split("x"))
    rows = []
    for i in a.ids:
        r = relic_row(by[i], (pw, ph))
        if r is None:
            print(i, "is held as its sprite")
        else:
            rows.append(r)
    if not rows:
        return 1
    sheet = Image.new("RGB", (rows[0].width, sum(r.height for r in rows)))
    y = 0
    for r in rows:
        sheet.paste(r, (0, y))
        y += r.height
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    sheet.save(a.out)
    print("wrote", a.out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
