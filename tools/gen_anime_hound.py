"""The anime hell hound: ONE pilot model of the monster remake (docs/mobs/remake_plan.md, docs/mobs/phaseE_hound.md). PREVIEW ONLY: nothing is wired into the game.

    python3 -B tools/gen_anime_hound.py                      # writes geo (3 LODs + a glow-only fx model), the sheets, the animations, the preview scenes
    python3 -B tools/gen_anime_hound.py --count              # measured cubes / quads of every model
    python3 -B tools/gen_anime_hound.py --preview [scene..]  # renders tools/render_preview scenes (own RENDER_PREVIEW_BUILD dir) into <HOUND_SCRATCH>/previews_hound
    python3 -B tools/anime_hound_sheets.py                   # composes the labelled contact sheets into docs/mobs/phaseE/hound_*.png

Files written (all new, nothing existing is touched):
    assets/nusmp/geo/entity/anime_hound{,_lod1,_lod2,_fx}.geo.json           the model: a four-legged beast, y up, front -z, +x the model's left, 1/16 block units
    assets/nusmp/textures/entity/anime_hound.png, anime_hound_glow.png       the cel sheet and its glow sheet (PNG8, painted at 4x4 samples a texel and reduced by mode)
    assets/nusmp/animations/entity/anime_hound.animation.json                idle walk run bite pounce howl hurt death sleep (+ pose_face_* for the five eye plates)
    tools/render_preview/scenes.d/anime_hound.json                           the preview scenes

Rotation signs (as gen_veldanava): positive x swings a hanging limb FORWARD (toward -z) and lifts a forward-pointing head, negative x folds a knee / trails the tail BACK,
positive z swings a hanging limb to +x, positive y turns the head to the model's right. Face plates are bones, shown by scale 1 and hidden by scale 0.001.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import anime_hound_paint as P  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp")
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "anime_hound.json")
SCRATCH = os.environ.get("HOUND_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
PREVIEW_OUT = os.path.join(SCRATCH, "previews_hound")
ID = "anime_hound"
HID = 0.001

ALL = ("up", "down", "west", "north", "east", "south")
KEY = {"west": "east", "east": "west", "north": "north", "south": "south", "up": "up", "down": "down"}     # the loader reads Bedrock's sides from the other end of x
NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
D2R = math.pi / 180.0


# ------------------------------------------------------------------------------------------------------------------ rotations (file frame, right-handed, R = Rz Ry Rx)
def rx(a):
    c, s = math.cos(a * D2R), math.sin(a * D2R)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]], float)


def ry(a):
    c, s = math.cos(a * D2R), math.sin(a * D2R)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]], float)


def rz(a):
    c, s = math.cos(a * D2R), math.sin(a * D2R)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]], float)


def euler(R):
    """(rx, ry, rz) degrees with R = Rz(rz) Ry(ry) Rx(rx), which is what GeoDraw applies to a cube."""
    b = -math.asin(max(-1.0, min(1.0, R[2, 0])))
    if abs(math.cos(b)) < 1e-6:
        return (0.0, math.degrees(b), math.degrees(math.atan2(-R[0, 1], R[1, 1])))
    return (math.degrees(math.atan2(R[2, 1], R[2, 2])), math.degrees(b), math.degrees(math.atan2(R[1, 0], R[0, 0])))


def unit(v):
    v = np.array(v, float)
    return v / np.linalg.norm(v)


def R_axes(yaxis, xhint=(1, 0, 0)):
    """Rotation whose local y is yaxis and whose local x is as near xhint as possible."""
    y = unit(yaxis)
    x = np.array(xhint, float)
    x = x - np.dot(x, y) * y
    x = unit(x)
    z = np.cross(x, y)
    return np.stack([x, y, z], 1)


# ------------------------------------------------------------------------------------------------------------------ the model under construction
class Part:
    def __init__(self, bone, name, kind, center, size, R, mat, faces, dens, lv, fx, ol, pa):
        self.bone, self.name, self.kind = bone, name, kind
        self.center, self.size, self.R, self.mat, self.faces, self.dens, self.lv, self.fx, self.ol, self.pa = np.array(center, float), tuple(size), R, mat, faces, dens, lv, fx, ol, pa
        self.rects = {}                                          # face -> (x, y, w, h) in the sheet


class Hound:
    def __init__(self):
        self.bones, self.order, self.parts = {}, [], []

    def bone(self, name, parent, pivot):
        self.bones[name] = (parent, tuple(float(v) for v in pivot))
        self.order.append(name)

    def box(self, bone, name, center, size, R=None, mat="fur", faces=ALL, dens=2.0, lv=2, fx=False, ol="tblr", **pa):
        p = Part(bone, name, "cube", center, size, np.eye(3) if R is None else R, mat, tuple(faces), dens, lv, fx, ol, pa)
        self.parts.append(p)
        return p

    def rbox(self, bone, name, center, size, c, mat="fur", caps=("north", "south"), dens=2.0, lv=2, **pa):
        """A box with its four long edges chamfered (an octagonal section along z): a cross of two boxes and four 45-degree prisms. The prisms stop 0.06 short at both ends, so no face z-fights."""
        cx, cy, cz = center
        sx, sy, sz = size
        self.box(bone, name + "_w", (cx, cy, cz), (sx, sy - 2 * c, sz), mat=mat, faces=("west", "east") + tuple(caps), dens=dens, lv=lv, **pa)
        self.box(bone, name + "_t", (cx, cy, cz), (sx - 2 * c, sy, sz), mat=mat, faces=("up", "down") + tuple(caps), dens=dens, lv=lv, **pa)
        k = c * math.sqrt(2.0)
        for sxs, sys_, a in ((1, 1, -45), (-1, 1, 45), (1, -1, -135), (-1, -1, 135)):
            self.box(bone, "%s_c%d%d" % (name, sxs, sys_), (cx + sxs * (sx / 2 - c), cy + sys_ * (sy / 2 - c), cz), (k, k, sz - 0.12), R=rz(a), mat=mat, faces=("up",) + tuple(caps), dens=dens, lv=lv, **pa)

    def limb(self, bone, name, A, B, w, d, over=0.35, mat="fur", faces=ALL, dens=2.0, lv=2, ol="tblr", side=0.0, **pa):
        """A box laid from joint A to joint B (it is turned about x only: legs and tails lie in the y-z plane)."""
        A, B = np.array(A, float), np.array(B, float)
        v = B - A
        L = float(np.linalg.norm(v)) + 2 * over
        c = (A + B) / 2
        dy, dz = v[1], v[2]
        if dy > 0:
            dy, dz = dy, dz
        else:
            dy, dz = -dy, -dz
        th = math.degrees(math.atan2(dz, dy))                    # the box's local +y is (cos, sin) in (y, z): positive turns the top backward
        return self.box(bone, name, c, (w, L, d), rx(th), mat, faces, dens, lv, ol=ol, **pa)

    def card(self, bone, name, base, tip_dir, w, h, mat, hint=(1, 0, 0), apex="top", dens=3.0, lv=0, fx=False, **pa):
        """A single alpha-cut quad standing on `base`, pointing along tip_dir. apex 'top': the shape's point is the tip; 'bottom': the point is the tip and the picture is flipped."""
        td = unit(tip_dir)
        c = np.array(base, float) + td * (h / 2)
        R = R_axes(td if apex == "top" else -td, hint)
        p = Part(bone, name, "card", c, (w, h, 0.0), R, mat, ("north",), dens, lv, fx, "", pa)
        self.parts.append(p)
        return p

    def shift(self, bones, d):
        d = np.array(d, float)
        for b in bones:
            par, pv = self.bones[b]
            self.bones[b] = (par, tuple(float(v) for v in np.array(pv) + d))
        for p in self.parts:
            if p.bone in bones:
                p.center = p.center + d

    # ------------------------------------------------------------------ sheet
    def face_dims(self, p, f):
        sx, sy, sz = p.size
        return {"north": (sx, sy), "south": (sx, sy), "west": (sz, sy), "east": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[f]

    def paint_face(self, p, f, W, Hh):
        S = P.SS
        u = (np.arange(W * S) + 0.5) / (W * S)
        v = (np.arange(Hh * S) + 0.5) / (Hh * S)
        U, V = np.meshgrid(u, v)
        sx, sy, sz = p.size
        if f == "north":
            lx, ly, lz = -sx / 2 + U * sx, sy / 2 - V * sy, np.full_like(U, -sz / 2)
        elif f == "south":
            lx, ly, lz = sx / 2 - U * sx, sy / 2 - V * sy, np.full_like(U, sz / 2)
        elif f == "west":
            lx, ly, lz = np.full_like(U, -sx / 2), sy / 2 - V * sy, sz / 2 - U * sz
        elif f == "east":
            lx, ly, lz = np.full_like(U, sx / 2), sy / 2 - V * sy, -sz / 2 + U * sz
        elif f == "up":
            lx, ly, lz = -sx / 2 + U * sx, np.full_like(U, sy / 2), sz / 2 - V * sz
        else:
            lx, ly, lz = -sx / 2 + U * sx, np.full_like(U, -sy / 2), sz / 2 - V * sz
        pts = np.stack([lx, ly, lz], -1) @ p.R.T + p.center
        n = p.R @ np.array(NORMAL[f], float)
        fw, fh = self.face_dims(p, f)
        c = P.Ctx(pts[..., 0], pts[..., 1], pts[..., 2], U, V, fw, fh, n, f, dict(p.pa, face=f), (W, Hh))
        col4, glow4, a4 = P.MATS[p.mat](c)
        edges = p.ol if f in ("north", "south", "west", "east") else ""
        return P.reduce_face(col4, glow4, a4, W, Hh, outline=(p.kind == "card") or bool(edges), edges=edges, is_card=(p.kind == "card"), glow_outside=(p.mat == "eye"))

    def pack_and_paint(self, width=256):
        jobs = []
        for p in self.parts:
            for f in p.faces:
                fw, fh = self.face_dims(p, f)
                W, Hh = max(2, int(round(fw * p.dens))), max(2, int(round(fh * p.dens)))
                jobs.append((p, f, W, Hh))
        pad = 1
        order = sorted(range(len(jobs)), key=lambda i: (-jobs[i][3], -jobs[i][2]))
        x = y = row = 0
        for i in order:
            p, f, W, Hh = jobs[i]
            if x + W > width:
                x, y, row = 0, y + row + pad, 0
            p.rects[f] = (x, y, W, Hh)
            x += W + pad
            row = max(row, Hh)
        height = int(math.ceil((y + row) / 8.0) * 8)
        col = np.zeros((height, width), np.uint8)
        glow = np.zeros((height, width), np.uint8)
        for p, f, W, Hh in jobs:
            c, g, a = self.paint_face(p, f, W, Hh)
            x0, y0, _, _ = p.rects[f]
            col[y0:y0 + Hh, x0:x0 + W] = c
            glow[y0:y0 + Hh, x0:x0 + W] = g
        self.sheet = (col, glow)
        self.tex = (width, height)
        return col, glow

    # ------------------------------------------------------------------ geo
    def geo(self, level=0, only_fx=False, ident=ID):
        W, H = self.tex
        bones = []
        for name in self.order:
            parent, pivot = self.bones[name]
            b = {"name": name, "pivot": [round(v, 3) for v in pivot]}
            if parent:
                b["parent"] = parent
            cubes = []
            for p in self.parts:
                if p.bone != name or p.lv < level or (only_fx and not p.fx):
                    continue
                size = p.size
                org = p.center - np.array(size) / 2
                c = {"origin": [round(float(v), 3) for v in org], "size": [round(float(v), 3) for v in size]}
                e = euler(p.R)
                if max(abs(e[0]), abs(e[1]), abs(e[2])) > 1e-3:
                    c["rotation"] = [round(v, 3) for v in e]
                    c["pivot"] = [round(float(v), 3) for v in p.center]
                uv = {}
                for f in p.faces:
                    x0, y0, w, h = p.rects[f]
                    uv[KEY[f]] = {"uv": [x0, y0], "uv_size": [w, h]}
                c["uv"] = uv
                cubes.append(c)
            b["cubes"] = cubes
            bones.append(b)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + ident, "texture_width": W, "texture_height": H, "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]},
            "bones": bones}]}

    def counts(self, level=0, only_fx=False):
        ps = [p for p in self.parts if p.lv >= level and (p.fx or not only_fx)]
        return len(ps), sum(len(p.faces) for p in ps)

    def save(self, anim):
        os.makedirs(os.path.join(ASSETS, "geo", "entity"), exist_ok=True)
        os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)
        os.makedirs(os.path.join(ASSETS, "animations", "entity"), exist_ok=True)
        for level, suffix in ((0, ""), (1, "_lod1"), (2, "_lod2")):
            with open(os.path.join(ASSETS, "geo", "entity", ID + suffix + ".geo.json"), "w", newline="\n") as f:
                json.dump(self.geo(level, ident=ID + suffix), f, separators=(",", ":"))
                f.write("\n")
        with open(os.path.join(ASSETS, "geo", "entity", ID + "_fx.geo.json"), "w", newline="\n") as f:
            json.dump(self.geo(0, only_fx=True, ident=ID + "_fx"), f, separators=(",", ":"))
            f.write("\n")
        col, glow = self.sheet
        for name, arr in ((ID, col), (ID + "_glow", glow)):
            im = Image.fromarray(arr, "P")
            im.putpalette(P.RGB.tobytes() + bytes(3 * (256 - len(P.RGB))))
            im.save(os.path.join(ASSETS, "textures", "entity", name + ".png"), transparency=0, optimize=True)
        with open(os.path.join(ASSETS, "animations", "entity", ID + ".animation.json"), "w", newline="\n") as f:
            json.dump({"format_version": "1.8.0", "animations": anim}, f, separators=(",", ":"))
            f.write("\n")


# ------------------------------------------------------------------------------------------------------------------ the hound
FUR_BODY = dict(sh_y=10.2, li_y=15.0, em_y=8.4, crack=True, thr=0.12, pseed=0.0, seed=3, cell=5.6, cw=0.5)
HD = (0.0, 0.8, -4.8)                                              # the head group is carried forward and up on a longer neck
RD = (0.0, 0.0, -4.5)                                              # the rear group moves forward (a shorter barrel)
SIDES2 = ((1, "_l"), (-1, "_r"))
PLATES = ("open", "narrow", "closed", "glow", "snarl")


def build():
    h = Hound()
    # ----- skeleton (pivots are the joints; the face plates pivot at their own centre so scaling them hides them in place)
    h.bone("root", None, (0, 0, 0))
    h.bone("body", "root", (0, 12.5, 3.0))
    h.bone("chest", "body", (0, 13.0, -5.0))
    h.bone("hips", "body", (0, 13.0, 9.5))
    h.bone("neck", "chest", (0, 14.8, -8.4))
    h.bone("head", "neck", (0, 19.0, -13.2))
    h.bone("jaw", "head", (0, 15.3, -17.2))
    for s, sf in SIDES2:
        h.bone("ear" + sf, "head", (s * 3.0, 22.8, -13.2))
    for k in PLATES:
        h.bone("face_" + k, "head", (0, 20.7, -18.35))
    NECK_A, NECK_B = np.array((0, 14.8, -8.4)), np.array((0, 19.8, -15.6))
    # mane flames: four on the neck, two on the withers, two on the back, two on the croup
    mane = []
    nrm = unit((0, 0.77, 0.64))
    for i, t in enumerate((0.85, 0.62, 0.38, 0.14)):
        base = NECK_A + (NECK_B - NECK_A) * t + nrm * 3.3
        mane.append(("mane_n%d" % i, "neck", base, 7.4 + 1.2 * i))
    for i, z in enumerate((-5.4, -1.4, 2.0, 5.6, 8.8)):
        mane.append(("mane_b%d" % i, "chest" if i < 2 else ("body" if i < 4 else "hips"), np.array((0, 18.0, z)), 8.4 - 0.6 * i))
    for name, parent, base, hgt in mane:
        h.bone(name, parent, tuple(base))
    for s, sf in SIDES2:
        h.bone("fl_up" + sf, "chest", (s * 4.5, 13.2, -6.4))
        h.bone("fl_low" + sf, "fl_up" + sf, (s * 4.3, 6.8, -7.0))
        h.bone("fl_paw" + sf, "fl_low" + sf, (s * 4.2, 2.0, -6.6))
        h.bone("bl_up" + sf, "hips", (s * 5.0, 14.6, 11.4))
        h.bone("bl_low" + sf, "bl_up" + sf, (s * 5.2, 8.6, 9.4))
        h.bone("bl_paw" + sf, "bl_low" + sf, (s * 4.9, 3.6, 12.8))
    TA = [np.array((0, 15.2, 14.6))]
    for ang, L in ((18, 6.5), (42, 6.0), (70, 5.0)):
        TA.append(TA[-1] + np.array((0, math.sin(ang * D2R) * L, math.cos(ang * D2R) * L)))
    h.bone("tail1", "hips", tuple(TA[0]))
    h.bone("tail2", "tail1", tuple(TA[1]))
    h.bone("tail3", "tail2", tuple(TA[2]))
    h.bone("tail_f", "tail3", tuple(TA[3]))

    fur = dict(FUR_BODY)
    # ----- torso: a deep chest, a tucked waist, a strong croup
    h.rbox("body", "barrel", (0, 12.8, 0.5), (8.6, 9.0, 13.4), 1.5, caps=(), **dict(fur, seed=5, pseed=1.3))
    h.rbox("chest", "chest", (0, 12.4, -6.8), (9.8, 10.8, 7.8), 1.8, **dict(fur, seed=3, bib=14.6))
    h.rbox("hips", "hips", (0, 12.6, 11.2), (9.2, 10.0, 7.4), 1.7, **dict(fur, seed=7, pseed=2.6))
    for s, sf in SIDES2:
        h.box("chest", "scap" + sf, (s * 3.9, 17.0, -6.8), (3.6, 2.8, 6.6), R=rz(-14 * s), mat="fur", lv=1, **dict(fur, sh_y=None, em_y=None, crack=False))
        h.box("hips", "haunch" + sf, (s * 4.4, 16.6, 11.2), (3.4, 3.0, 6.4), R=rz(-12 * s), mat="fur", lv=1, **dict(fur, sh_y=None, em_y=None, crack=False))
    # ----- neck and head
    nv = NECK_B - NECK_A
    th = math.degrees(math.atan2(nv[2], nv[1]))
    h.box("neck", "neck", (NECK_A + NECK_B) / 2 + np.array((0, 0.1, 0)), (7.0, 10.0, 6.8), R=rx(th), mat="fur", **dict(fur, sh_y=None, em_y=None, crack=False, li_y=20.5))
    SK = dict(fur, sh_y=None, em_y=None, crack=False, li_y=21.8)
    ang = 39.6
    h.box("head", "skull_back", (0, 19.0, -13.05), (8.4, 8.8, 6.5), mat="skull", dens=3, faces=("up", "down", "west", "east", "south"), **SK)
    h.box("head", "skull_front", (0, 19.0, -17.2), (3.8, 8.8, 2.0), mat="skull", dens=3, faces=("up", "down", "north"), forehead=True, **SK)
    for s, sf in SIDES2:
        Rf = ry(-s * ang)
        h.box("head", "temple" + sf, (s * 2.28, 18.9, -16.33), (3.0, 8.6, 2.4), R=Rf, mat="skull", dens=3, faces=("up", "north"), **SK)
    h.box("head", "muzzle", (0, 17.5, -20.3), (5.2, 4.2, 4.4), mat="muzzle", dens=3, sh_y=16.4)
    h.box("head", "muzzle2", (0, 17.9, -24.0), (4.2, 3.4, 3.6), mat="muzzle", dens=3, sh_y=16.6)
    h.box("head", "nose", (0, 18.4, -26.4), (3.6, 2.4, 1.2), mat="nose", faces=("up", "north", "west", "east"), dens=3, ol="tblr")
    h.box("jaw", "jaw", (0, 14.45, -21.4), (3.9, 2.0, 7.2), mat="jaw", dens=3, **dict(fur, sh_y=None, em_y=None, crack=False, li_y=99))
    # eye plates: two cards per plate, lying on the angled front facets of the skull
    for k in PLATES:
        for s, sf in SIDES2:
            n = ry(-s * ang) @ np.array((0, 0, -1.0))
            c0 = np.array((s * 3.05, 19.6, -17.25)) + n * 0.12
            h.card("face_" + k, "eye_%s%s" % (k, sf), c0, (0, 1, 0), 3.0, 2.2, "eye", hint=ry(-s * ang) @ np.array((1, 0, 0)), apex="top", dens=7, lv=1 if k in ("open", "glow") else 0, fx=True,
                   plate=k, mirror=(s < 0))
    # ears: an outer fur card behind and an inner ember card in front
    for s, sf in SIDES2:
        base = np.array((s * 3.0, 22.8, -13.2))
        Rr = rz(-s * 14) @ rx(14)
        td = Rr @ np.array((0, 1, 0))
        h.card("ear" + sf, "ear_out" + sf, base + np.array((0, 0, 0.18)), td, 5.4, 7.4, "ear_out", hint=Rr @ np.array((1, 0, 0)), lv=2)
        h.card("ear" + sf, "ear_in" + sf, base + np.array((0, 0, -0.06)) + td * 0.5, td, 3.4, 5.6, "ear_in", hint=Rr @ np.array((1, 0, 0)), lv=1)
    # fangs
    for s, sf in SIDES2:
        h.card("head", "fang" + sf, (s * 2.45, 15.7, -23.6), (0, -1, 0), 1.7, 2.9, "fang", hint=ry(-s * 35) @ np.array((1, 0, 0)), apex="bottom", lv=1)
        h.card("jaw", "fang_low" + sf, (s * 1.5, 15.2, -24.4), (0, 1, 0), 1.2, 2.0, "fang", hint=ry(s * 25) @ np.array((1, 0, 0)), apex="top", lv=0)
        # cheek fur
        for j, (yy, ln) in enumerate(((19.0, 4.8), (17.0, 4.0))):
            h.card("head", "cheek%d%s" % (j, sf), (s * 4.2, yy, -12.2 - 1.3 * j), unit((s * 0.55, -0.55, 0.65)), 3.8, ln, "card_fur", hint=(0, 0, -1), apex="bottom", lv=0, shape="tuft", lean=0.15)
    # bone-plate collar around the neck base
    axis = unit(NECK_B - NECK_A)
    cen = NECK_A + (NECK_B - NECK_A) * 0.42 + np.array((0, 0.2, 0))
    Rn = R_axes(axis, (1, 0, 0))
    for k, ang in enumerate((-125, -85, -45, -15, 15, 45, 85, 125)):
        a = ang * D2R
        radial = Rn @ np.array((math.sin(a), 0, -math.cos(a)))     # -z of the neck's frame is the front
        pos = cen + radial * 4.0
        R = Rn @ ry(-ang) @ rx(14)
        h.box("neck", "collar%d" % k, pos, (2.7, 2.4, 1.1), R=R, mat="bone", lv=1, dens=3, salt=float(k), seam=(k % 2 == 0), ol="tblr")
        sp = pos + radial * 0.5 + axis * 1.0
        h.card("neck", "collar_sp%d" % k, sp, unit(radial * 0.8 + axis * 0.7), 1.9, 3.4 + (0.8 if abs(ang) < 50 else 0), "spike", hint=np.cross(axis, radial), lv=0, salt=float(k))
    # mane flames: two crossed cards each, so the mane has volume from every side
    for i, (name, parent, base, hgt) in enumerate(mane):
        for k, phi in enumerate((38, -38) if i % 2 == 0 else (-38, 38)):
            tip = unit((math.sin(phi * D2R) * 0.30, 1.0, 0.50 + 0.05 * (i % 3)))
            hint = (math.cos(phi * D2R), 0, -math.sin(phi * D2R))
            h.card(name, "%s_c%d" % (name, k), base + np.array((0, -0.4, 0)), tip, 4.4, hgt * (1.0 if k == 0 else 0.82), "flame", hint=hint, dens=3.5, lv=1 if k == 0 else 0, fx=True,
                   phase=0.9 * i + 1.7 * k, lean=0.18 * (1 if (i + k) % 2 else -1))
    # chest ruff and belly fringe: fur tufts
    for j, (x, y, z) in enumerate(((-3.8, 10.6, -11.1), (-1.9, 9.0, -11.2), (0, 8.4, -11.2), (1.9, 9.0, -11.2), (3.8, 10.6, -11.1))):
        h.card("chest", "ruff%d" % j, (x, y + 1.2, z), unit((x * 0.12, -0.55, -0.85)), 3.0, 4.6 - 0.5 * abs(x) / 3.8, "card_fur", hint=(1, 0, 0), apex="bottom", lv=1 if j % 2 == 0 else 0, shape="tuft", lean=0.1 * (1 if x >= 0 else -1))
    for s, sf in SIDES2:
        for j, z in enumerate((-1.5, 4.5)):
            h.card("body", "fringe%d%s" % (j, sf), (s * 4.1, 9.6, z), unit((s * 0.35, -0.9, 0.1)), 3.6, 3.4, "card_fur", hint=(0, 0, -1), apex="bottom", lv=0, shape="tuft", lean=0.0)
    # legs
    for s, sf in SIDES2:
        paw = dict(sh_y=None, em_y=None, crack=False, dark_y=3.2, li_y=99)
        lf = dict(sh_y=None, em_y=4.4, crack=True, thr=0.62, pseed=4.0 + s, seed=11, dark_y=4.0)
        A, B, C = np.array((s * 4.5, 13.2, -6.4)), np.array((s * 4.3, 6.8, -7.0)), np.array((s * 4.2, 2.0, -6.6))
        h.limb("fl_up" + sf, "fl_up" + sf, A, B, 4.6, 4.8, **dict(lf, sh_y=None, dark_y=None, em_y=None, li_y=99))
        h.limb("fl_low" + sf, "fl_low" + sf, B, C, 3.6, 3.8, **lf)
        h.box("fl_paw" + sf, "fl_paw" + sf, (s * 4.2, 0.95, -8.0), (3.8, 1.9, 6.0), mat="fur", faces=("up", "west", "north", "east", "south"), dens=3, **paw)
        for j, dx in enumerate((-1.2, 0, 1.2)):
            h.card("fl_paw" + sf, "fl_claw%d%s" % (j, sf), (s * 4.2 + dx, 0.9, -10.9), unit((dx * 0.1, -0.45, -0.9)), 1.2, 1.9, "claw", hint=ry(s * 10) @ np.array((1, 0, 0)), apex="bottom", lv=0)
        h.card("fl_low" + sf, "fl_tuft" + sf, (s * 4.4, 8.4, -5.0), unit((s * 0.3, -0.4, 0.9)), 2.6, 3.4, "card_fur", hint=(1, 0, 0), apex="bottom", lv=0, shape="tuft", lean=0.0)
        # hind leg
        H_, K, Cc, Q = np.array((s * 5.0, 14.6, 11.4)), np.array((s * 5.2, 8.6, 9.4)), np.array((s * 4.9, 3.6, 12.8)), np.array((s * 4.7, 1.8, 11.2))
        h.limb("bl_up" + sf, "bl_up" + sf, H_, K, 4.4, 5.2, over=0.6, **dict(lf, dark_y=None, em_y=None, li_y=99, crack=True, thr=0.4, seed=13))
        h.limb("bl_low" + sf, "bl_low" + sf, K, Cc, 3.8, 3.8, **dict(lf, dark_y=None, em_y=None, li_y=99))
        h.limb("bl_paw" + sf, "bl_foot" + sf, Cc, Q, 3.2, 3.2, **dict(lf, em_y=None, crack=False))
        h.box("bl_paw" + sf, "bl_paw" + sf, (s * 4.7, 0.95, 9.4), (3.8, 1.9, 6.0), mat="fur", faces=("up", "west", "north", "east", "south"), dens=3, **paw)
        for j, dx in enumerate((-1.2, 0, 1.2)):
            h.card("bl_paw" + sf, "bl_claw%d%s" % (j, sf), (s * 4.7 + dx, 0.9, 6.5), unit((dx * 0.1, -0.45, -0.9)), 1.2, 1.9, "claw", hint=ry(s * 10) @ np.array((1, 0, 0)), apex="bottom", lv=0)
        h.card("bl_low" + sf, "hock_tuft" + sf, (s * 5.0, 5.0, 13.6), unit((s * 0.2, -0.3, 1.0)), 2.6, 3.6, "card_fur", hint=(1, 0, 0), apex="bottom", lv=0, shape="tuft", lean=0.0)
    # tail: three spiked segments and a flame
    for i, (bn, w) in enumerate((("tail1", 3.8), ("tail2", 3.2), ("tail3", 2.6))):
        h.limb(bn, bn, TA[i], TA[i + 1], w, w, over=0.3, **dict(fur, sh_y=None, em_y=None, li_y=99, thr=0.3, seed=17 + i))
        seg = unit(TA[i + 1] - TA[i])
        up = unit(np.cross(np.cross(seg, (0, 1, 0)), seg))
        for j in (0.3, 0.75):
            base = TA[i] + (TA[i + 1] - TA[i]) * j + up * (w / 2 - 0.2)
            h.card(bn, "tspike%d_%d" % (i, int(j * 10)), base, unit(up * 1.0 + seg * 0.25), 2.0, 3.2, "spike", hint=np.cross(seg, up), lv=1 if j < 0.5 else 0, salt=float(i))
    tipdir = unit(TA[3] - TA[2])
    for j, (ang, hgt) in enumerate(((-28, 6.5), (0, 8.0), (28, 6.5))):
        d = unit(tipdir + np.array((math.sin(ang * D2R) * 0.5, 0.25, 0)))
        h.card("tail_f", "tflame%d" % j, TA[3] - tipdir * 0.5, d, 3.4, hgt, "flame", hint=(math.cos(ang * D2R), 0, math.sin(ang * D2R)), dens=3.5, lv=1 if j == 1 else 0, fx=True, phase=1.3 * j, lean=0.18)
    # more fur: belly fringe, neck ruff, shoulder and haunch tufts, leg feathering, forehead lock, tail fur
    for s, sf in SIDES2:
        for j, z in enumerate((-8.0, -4.0, 0.0, 3.5, 6.5)):
            h.card("chest" if z < -2 else "body", "fringe%d%s" % (j, sf), (s * (4.3 if j in (0, 4) else 4.0), 9.9, z), unit((s * 0.25, -0.95, 0.12)), 3.4, 3.0 + 0.4 * (j % 2), "card_fur",
                   hint=(0, 0, -1), apex="bottom", lv=1 if j % 2 == 0 else 0, shape="tuft", lean=0.0 if j % 2 else 0.1)
        for j, (zz, ln) in enumerate(((-8.8, 4.4), (-4.6, 3.8))):
            h.card("chest", "shtuft%d%s" % (j, sf), (s * 4.9, 16.8, zz), unit((s * 0.6, 0.55, -0.1)), 3.2, ln, "card_fur", hint=(0, 0, -1), apex="top", lv=0, shape="tuft", lean=-0.1)
        h.card("hips", "hatuft" + sf, (s * 4.8, 15.8, 11.6), unit((s * 0.6, 0.4, 0.55)), 3.6, 4.2, "card_fur", hint=(0, 0, -1), apex="top", lv=0, shape="tuft", lean=0.1)
        h.card("fl_low" + sf, "fl_feather" + sf, (s * 4.4, 7.4, -4.4), unit((s * 0.1, -0.7, 0.6)), 2.4, 4.4, "card_fur", hint=(1, 0, 0), apex="bottom", lv=0, shape="tuft", lean=0.0)
        h.card("bl_up" + sf, "bl_feather" + sf, (s * 5.3, 13.2, 13.6), unit((s * 0.15, -0.55, 0.8)), 3.4, 5.4, "card_fur", hint=(1, 0, 0), apex="bottom", lv=0, shape="tuft", lean=0.0)
    for j in range(6):
        a = (-100 + 40 * j) * D2R
        radial = Rn @ np.array((math.sin(a), 0, -math.cos(a)))
        pos = NECK_A + (NECK_B - NECK_A) * 0.22 + radial * 3.2 + np.array((0, 0.6, 0))
        h.card("neck", "ruff_n%d" % j, pos, unit(radial * 0.7 - axis * 0.55), 3.4, 4.2, "card_fur", hint=np.cross(axis, radial), apex="bottom", lv=0, shape="tuft", lean=0.1)
    for i, bn in enumerate(("tail1", "tail2")):
        seg = unit(TA[i + 1] - TA[i])
        h.card(bn, "tfur%d" % i, TA[i] + seg * 2.5 + np.array((0, -1.3, 0)), unit(np.array((0, -0.8, 0.4)) + seg * 0.6), 2.6, 3.6, "card_fur", hint=(1, 0, 0), apex="bottom", lv=0, shape="tuft", lean=0.0)
    h.card("head", "forelock", (0, 23.2, -17.0), unit((0, 0.5, -0.85)), 2.6, 3.8, "card_fur", hint=(1, 0, 0), apex="top", lv=0, shape="tuft", lean=0.0, root_top=False)
    # bone plates: under every back flame, on the shoulders, and the vertebra rings of the tail
    for i, (name, parent, base, hgt) in enumerate(mane[4:]):
        h.box(parent, "spine_plate%d" % i, (0, 17.75, base[2]), (3.6, 1.1, 3.4), R=rx(-2 + 2 * i), mat="bone", lv=1, dens=3, salt=float(i), seam=(i % 2 == 1))
    for s, sf in SIDES2:
        h.box("chest", "shoulder_plate" + sf, (s * 4.6, 17.9, -6.6), (3.4, 1.0, 5.0), R=rz(-22 * s) @ rx(-4), mat="bone", lv=1, dens=3, salt=2.0 + s, seam=True)
    for i, (bn, w) in enumerate((("tail1", 4.4), ("tail2", 3.8))):
        h.box(bn, "tail_ring%d" % i, TA[i + 1], (w, 1.4, w), R=rx(math.degrees(math.atan2(*(TA[i + 2] - TA[i])[[2, 1]]))), mat="bone", lv=1, dens=3, salt=float(i), seam=(i == 0))
    h.shift(["head", "jaw", "ear_l", "ear_r"] + ["face_" + k for k in PLATES], HD)
    h.shift(["hips", "tail1", "tail2", "tail3", "tail_f"] + [b + sf for b in ("bl_up", "bl_low", "bl_paw") for _, sf in SIDES2], RD)
    return h


# ------------------------------------------------------------------------------------------------------------------ animation
def S(x):
    return math.sin(x)


def C(x):
    return math.cos(x)


def hold(v):
    return lambda t: v


class Clips:
    """Builds the clips: every track is a function of time sampled at 30 keys per second (loops close on their first key), plus plate keys (scale steps)."""

    def __init__(self):
        self.d = {}

    def clip(self, name, length, loop):
        self.cur = {"loop": loop, "animation_length": length, "bones": {}}
        self.L, self.loop = length, loop
        self.d[name] = self.cur
        return self

    def _put(self, bone, kind, frames):
        b = self.cur["bones"].setdefault(bone, {})
        old = b.get(kind)
        if old:
            keys = sorted(set(old) | set(frames), key=float)
            frames = {k: tuple(_interp(old, float(k))[i] + _interp(frames, float(k))[i] for i in range(3)) for k in keys}
        b[kind] = {k: [round(float(x), 3) for x in v] for k, v in sorted(frames.items(), key=lambda kv: float(kv[0]))}

    def fn(self, bone, kind, f, n=None):
        n = n or max(8, int(round(self.L * 30)))
        ts = [self.L * i / n for i in range(n + 1)]
        self._put(bone, kind, {_tk(t): tuple(f(t)) for t in ts})

    def rot(self, bone, f, n=None):
        self.fn(bone, "rotation", f, n)

    def pos(self, bone, f, n=None):
        self.fn(bone, "position", f, n)

    def seq(self, bone, kind, times, vals, ease=True, sub=4):
        """Smooth keys through (times, vals): smoothstep between the poses, `sub` keys a segment."""
        frames = {}
        for i in range(len(times) - 1):
            for k in range(sub + (1 if i == len(times) - 2 else 0)):
                f = k / float(sub)
                e = f * f * (3 - 2 * f) if ease else f
                t = times[i] + (times[i + 1] - times[i]) * f
                frames[_tk(t)] = tuple(vals[i][j] + (vals[i + 1][j] - vals[i][j]) * e for j in range(3))
        self._put(bone, kind, frames)

    def plate(self, shown_times):
        """shown_times: [(t, plate name)] from t on that plate is shown (steps 10 ms wide)."""
        for k in PLATES:
            frames = {}
            cur = None
            for t, name in shown_times:
                v = 1.0 if name == k else HID
                if cur is None:
                    frames[_tk(t)] = (v, v, v)
                elif cur != v:
                    frames[_tk(max(0.0, t - 0.01))] = (cur, cur, cur)
                    frames[_tk(t)] = (v, v, v)
                cur = v
            frames[_tk(self.L)] = (cur, cur, cur)
            self._put("face_" + k, "scale", frames)


def _tk(t):
    return ("%.3f" % t).rstrip("0").rstrip(".") if t else "0.0"


def _interp(frames, t):
    ks = sorted(frames, key=float)
    if t <= float(ks[0]):
        return frames[ks[0]]
    if t >= float(ks[-1]):
        return frames[ks[-1]]
    for a, b in zip(ks, ks[1:]):
        if float(a) <= t <= float(b):
            f = (t - float(a)) / (float(b) - float(a)) if float(b) > float(a) else 0
            return tuple(frames[a][i] + (frames[b][i] - frames[a][i]) * f for i in range(3))
    return frames[ks[-1]]


FLAMES_N = ["mane_n%d" % i for i in range(4)]
FLAMES_B = ["mane_b%d" % i for i in range(5)]
LEGS = ("fl", "bl")


def clips():
    c = Clips()
    TAU = 2 * math.pi
    pos = lambda t: t

    def mane(L, amp=6.0, base=14.0, lag=0.5, spd=1.0, side=2.0):
        for i, n in enumerate(FLAMES_N + FLAMES_B):
            c.rot(n, lambda t, i=i: (base + amp * S(TAU * t / L * spd - lag * i), 0, side * S(TAU * t / L * spd * 0.5 + i)))

    def tail(L, base=(0, 0, 0), amp=8.0, lag=0.7, spd=1.0, side=10.0):
        for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
            c.rot(n, lambda t, i=i: (base[0] + amp * S(TAU * t / L * spd - lag * i), side * S(TAU * t / L * spd - lag * i - 0.7) * (0.6 + 0.2 * i), 0))

    # ----- idle: standing, breathing, a slow tail, an ear flick, a blink
    L = 3.2
    c.clip("idle", L, True)
    c.rot("chest", lambda t: (1.0 * S(TAU * t / L), 0, 0))
    c.pos("chest", lambda t: (0, 0.25 * S(TAU * t / L), 0))
    c.rot("head", lambda t: (3.0 * S(TAU * t / L - 0.6) + 2, 2.5 * S(TAU * t / L * 0.5), 0))
    c.rot("neck", lambda t: (-2, 0, 0))
    c.rot("jaw", lambda t: (-0.4 - 1.0 * (0.5 + 0.5 * S(TAU * t / L - 0.4)), 0, 0))
    c.rot("ear_l", lambda t: (0, 0, 2.0 * S(TAU * t / L * 2) + (-10 * max(0.0, S(TAU * t / L * 1.0 - 2.0)) ** 8)))
    c.rot("ear_r", lambda t: (0, 0, -2.0 * S(TAU * t / L * 2 + 1)))
    mane(L, amp=5.0, base=6.0, lag=0.5)
    tail(L, base=(0, 0, 0), amp=7.0, side=9.0)
    c.plate([(0.0, "open"), (2.78, "closed"), (2.90, "open")])

    # ----- walk: a diagonal four-beat gait
    L = 1.2
    ph = {"fl_l": 0.0, "bl_r": 0.0, "fl_r": math.pi, "bl_l": math.pi}
    c.clip("walk", L, True)
    legs(c, L, ph, front=(24, 30), rear=(26, 34), lift=1.0, paw=0.8)
    c.pos("root", lambda t: (0, 0.5 * abs(S(TAU * t / L)) + 0.1, 0), None)
    c.rot("body", lambda t: (0.8 * S(TAU * t / L * 2), 3.0 * S(TAU * t / L), 0.8 * S(TAU * t / L)))
    c.rot("chest", lambda t: (-1.5 * S(TAU * t / L * 2), -3.0 * S(TAU * t / L), 0))
    c.rot("neck", lambda t: (-2 + 2.0 * S(TAU * t / L * 2 + 1), 0, 0))
    c.rot("head", lambda t: (3 + 3.5 * S(TAU * t / L * 2 + 0.6), 2.5 * S(TAU * t / L + 2.0), 0))
    c.rot("jaw", hold((-2.0, 0, 0)))
    mane(L, amp=6.0, base=14.0, lag=0.55, spd=2.0)
    tail(L, base=(8, 0, 0), amp=7.0, spd=2.0, side=12.0)
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.rot("ear" + sf, lambda t, sg=sg: (4 * S(TAU * t / L * 2), 0, sg * -3 * S(TAU * t / L * 2 + 1)))
    c.plate([(0.0, "open")])

    # ----- run: a bounding gallop, the back flexing, the flames streaming
    L = 0.62
    c.clip("run", L, True)
    legs(c, L, {"fl_l": 0.0, "fl_r": 0.55, "bl_l": math.pi + 0.35, "bl_r": math.pi + 0.9}, front=(46, 58), rear=(48, 66), lift=1.3, paw=0.7)
    c.pos("root", lambda t: (0, 3.0 * max(0.0, S(TAU * t / L + 1.0)) + 0.4, 0))
    c.rot("body", lambda t: (-6.0 * S(TAU * t / L + 0.6), 2.0 * S(TAU * t / L), 0))
    c.rot("chest", lambda t: (-10.0 * S(TAU * t / L + 0.5), 0, 0))
    c.rot("hips", lambda t: (12.0 * S(TAU * t / L + 0.2), 0, 0))
    c.rot("neck", lambda t: (-8 + 5 * S(TAU * t / L + 1.0), 0, 0))
    c.rot("head", lambda t: (7 + 4 * S(TAU * t / L + 1.4), 0, 0))
    c.rot("jaw", lambda t: (-14 - 6 * S(TAU * t / L + 0.5), 0, 0))
    mane(L, amp=6.0, base=44.0, lag=0.5, spd=1.0)
    tail(L, base=(-22, 0, 0), amp=10.0, spd=1.0, side=6.0)
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.rot("ear" + sf, lambda t, sg=sg: (-40 + 6 * S(TAU * t / L), 0, sg * 10))
    c.plate([(0.0, "narrow")])

    # ----- bite: wind up (head and shoulders back, jaws wide), lunge, snap shut at 0.64 s (80 percent), recover
    L = 0.8
    c.clip("bite", L, False)
    T = [0.0, 0.24, 0.46, 0.6, 0.64, 0.7, 0.8]
    c.seq("root", "position", T, [(0, 0, 0), (0, 0, 1.6), (0, 0, 2.0), (0, 0.6, -6.5), (0, 0.4, -7.0), (0, 0.2, -5.0), (0, 0, 0)])
    c.seq("body", "rotation", T, [(0, 0, 0), (-5, 0, 0), (-8, 0, 0), (7, 0, 0), (6, 0, 0), (3, 0, 0), (0, 0, 0)])
    c.seq("chest", "rotation", T, [(0, 0, 0), (-7, 0, 0), (-11, 0, 0), (9, 0, 0), (9, 0, 0), (4, 0, 0), (0, 0, 0)])
    c.seq("neck", "rotation", T, [(-2, 0, 0), (-14, 0, 0), (-22, 0, 0), (18, 0, 0), (20, 0, 0), (8, 0, 0), (-2, 0, 0)])
    c.seq("head", "rotation", T, [(2, 0, 0), (16, 0, 0), (22, 0, 0), (-8, 0, 0), (-12, 0, 0), (-2, 0, 0), (2, 0, 0)])
    c.seq("jaw", "rotation", T, [(-2, 0, 0), (-30, 0, 0), (-52, 0, 0), (-60, 0, 0), (-2, 0, 0), (-4, 0, 0), (-2, 0, 0)], sub=6)
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.seq("fl_up" + sf, "rotation", T, [(0, 0, 0), (-18, 0, 0), (-28, 0, 0), (34, 0, 0), (36, 0, 0), (18, 0, 0), (0, 0, 0)])
        c.seq("fl_low" + sf, "rotation", T, [(0, 0, 0), (12, 0, 0), (20, 0, 0), (-20, 0, 0), (-22, 0, 0), (-10, 0, 0), (0, 0, 0)])
        c.seq("bl_up" + sf, "rotation", T, [(0, 0, 0), (-12, 0, 0), (-18, 0, 0), (14, 0, 0), (14, 0, 0), (6, 0, 0), (0, 0, 0)])
        c.seq("bl_low" + sf, "rotation", T, [(0, 0, 0), (14, 0, 0), (24, 0, 0), (-8, 0, 0), (-8, 0, 0), (-4, 0, 0), (0, 0, 0)])
        c.seq("ear" + sf, "rotation", T, [(0, 0, 0), (-30, 0, sg * 6), (-44, 0, sg * 10), (-52, 0, sg * 8), (-52, 0, sg * 8), (-30, 0, sg * 4), (0, 0, 0)])
    for i, n in enumerate(FLAMES_N + FLAMES_B):
        c.seq(n, "rotation", T, [(6, 0, 0), (26, 0, 0), (36, 0, 0), (66, 0, 0), (60, 0, 0), (34, 0, 0), (6, 0, 0)])
    for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
        c.seq(n, "rotation", T, [(0, 0, 0), (-10 - 4 * i, 0, 0), (-16 - 6 * i, 0, 0), (14 + 8 * i, 0, 0), (12 + 8 * i, 0, 0), (4, 0, 0), (0, 0, 0)])
    c.plate([(0.0, "open"), (0.2, "snarl"), (0.7, "open")])

    # ----- pounce: crouch, spring, fly stretched out, land (the claws come down at 0.96 s)
    L = 1.2
    c.clip("pounce", L, False)
    T = [0.0, 0.36, 0.52, 0.7, 0.96, 1.08, 1.2]
    c.seq("root", "position", T, [(0, 0, 0), (0, -3.5, 2.5), (0, 8.0, -6), (0, 12.0, -16), (0, 0, -26), (0, -2.0, -26), (0, 0, -26)])
    c.seq("body", "rotation", T, [(0, 0, 0), (6, 0, 0), (-16, 0, 0), (-10, 0, 0), (8, 0, 0), (4, 0, 0), (0, 0, 0)])
    c.seq("chest", "rotation", T, [(0, 0, 0), (14, 0, 0), (-12, 0, 0), (-6, 0, 0), (12, 0, 0), (6, 0, 0), (0, 0, 0)])
    c.seq("hips", "rotation", T, [(0, 0, 0), (-8, 0, 0), (10, 0, 0), (10, 0, 0), (-8, 0, 0), (-4, 0, 0), (0, 0, 0)])
    c.seq("neck", "rotation", T, [(-2, 0, 0), (-16, 0, 0), (8, 0, 0), (8, 0, 0), (-6, 0, 0), (-6, 0, 0), (-2, 0, 0)])
    c.seq("head", "rotation", T, [(2, 0, 0), (18, 0, 0), (-4, 0, 0), (-4, 0, 0), (8, 0, 0), (4, 0, 0), (2, 0, 0)])
    c.seq("jaw", "rotation", T, [(-2, 0, 0), (-8, 0, 0), (-46, 0, 0), (-52, 0, 0), (-50, 0, 0), (-10, 0, 0), (-2, 0, 0)])
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.seq("fl_up" + sf, "rotation", T, [(0, 0, 0), (40, 0, 0), (-86, 0, sg * 6), (-96, 0, sg * 8), (-70, 0, 0), (18, 0, 0), (0, 0, 0)])
        c.seq("fl_low" + sf, "rotation", T, [(0, 0, 0), (-30, 0, 0), (24, 0, 0), (10, 0, 0), (28, 0, 0), (-10, 0, 0), (0, 0, 0)])
        c.seq("fl_paw" + sf, "rotation", T, [(0, 0, 0), (-10, 0, 0), (30, 0, 0), (36, 0, 0), (50, 0, 0), (0, 0, 0), (0, 0, 0)])
        c.seq("bl_up" + sf, "rotation", T, [(0, 0, 0), (-34, 0, 0), (50, 0, sg * 4), (60, 0, 0), (20, 0, 0), (-16, 0, 0), (0, 0, 0)])
        c.seq("bl_low" + sf, "rotation", T, [(0, 0, 0), (46, 0, 0), (-30, 0, 0), (-40, 0, 0), (-10, 0, 0), (20, 0, 0), (0, 0, 0)])
        c.seq("bl_paw" + sf, "rotation", T, [(0, 0, 0), (-14, 0, 0), (40, 0, 0), (46, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)])
        c.seq("ear" + sf, "rotation", T, [(0, 0, 0), (10, 0, 0), (-50, 0, sg * 10), (-56, 0, sg * 12), (-30, 0, 0), (0, 0, 0), (0, 0, 0)])
    for i, n in enumerate(FLAMES_N + FLAMES_B):
        c.seq(n, "rotation", T, [(6, 0, 0), (20, 0, 0), (62, 0, 0), (70, 0, 0), (36, 0, 0), (10, 0, 0), (6, 0, 0)])
    for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
        c.seq(n, "rotation", T, [(0, 0, 0), (6 + 4 * i, 0, 0), (-26 - 4 * i, 0, 0), (-30 - 6 * i, 0, 0), (10 + 6 * i, 0, 0), (6, 0, 0), (0, 0, 0)])
    c.plate([(0.0, "open"), (0.3, "glow"), (1.1, "open")])

    # ----- howl: head thrown up, chest lifted, jaws open, the flames flaring; eyes closed (loop 3 s, one shot)
    L = 3.0
    c.clip("howl", L, False)
    T = [0.0, 0.55, 0.9, 2.1, 2.6, 3.0]
    c.seq("chest", "rotation", T, [(0, 0, 0), (-12, 0, 0), (-18, 0, 0), (-18, 0, 0), (-6, 0, 0), (0, 0, 0)])
    c.seq("body", "rotation", T, [(0, 0, 0), (-3, 0, 0), (-5, 0, 0), (-5, 0, 0), (-1, 0, 0), (0, 0, 0)])
    c.seq("neck", "rotation", T, [(-2, 0, 0), (14, 0, 0), (26, 0, 0), (26, 0, 0), (8, 0, 0), (-2, 0, 0)])
    c.seq("head", "rotation", T, [(2, 0, 0), (28, 0, 0), (52, 0, 0), (54, 0, 0), (14, 0, 0), (2, 0, 0)])
    c.seq("jaw", "rotation", T, [(-2, 0, 0), (-30, 0, 0), (-44, 0, 0), (-46, 0, 0), (-12, 0, 0), (-2, 0, 0)])
    c.fn("head", "position", lambda t: (0, 0, 0))
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.seq("fl_up" + sf, "rotation", T, [(0, 0, 0), (-8, 0, 0), (-14, 0, 0), (-14, 0, 0), (-4, 0, 0), (0, 0, 0)])
        c.seq("bl_up" + sf, "rotation", T, [(0, 0, 0), (8, 0, 0), (14, 0, 0), (14, 0, 0), (4, 0, 0), (0, 0, 0)])
        c.seq("bl_low" + sf, "rotation", T, [(0, 0, 0), (-6, 0, 0), (-10, 0, 0), (-10, 0, 0), (-3, 0, 0), (0, 0, 0)])
        c.seq("ear" + sf, "rotation", T, [(0, 0, 0), (-24, 0, sg * 4), (-44, 0, sg * 6), (-46, 0, sg * 6), (-14, 0, 0), (0, 0, 0)])
    for i, n in enumerate(FLAMES_N + FLAMES_B):
        c.fn(n, "rotation", lambda t, i=i: ((6 + 54 * min(1.0, t / 0.9, max(0.0, (3.0 - t) / 0.8))) + 6 * S(TAU * t * 1.6 - 0.5 * i), 0, 5 * S(TAU * t * 0.8 + i) * min(1.0, t / 0.9)), n=90)
    for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
        c.fn(n, "rotation", lambda t, i=i: (-6 * min(1.0, t / 0.9, max(0.0, (3.0 - t) / 0.8)) - 3 * i + 5 * S(TAU * t * 0.7 - 0.7 * i), 0, 0), n=90)
    c.fn("root", "position", lambda t: (0, 0.35 * S(TAU * t * 7) * min(1.0, max(0.0, (t - 0.9) / 0.2), max(0.0, (2.5 - t) / 0.3)), 0), n=90)
    c.plate([(0.0, "open"), (0.45, "closed"), (2.5, "open")])

    # ----- hurt: a flinch
    L = 0.45
    c.clip("hurt", L, False)
    T = [0.0, 0.08, 0.2, 0.45]
    c.seq("root", "position", T, [(0, 0, 0), (0, 0.6, 3.5), (0, 0, 1.5), (0, 0, 0)])
    c.seq("body", "rotation", T, [(0, 0, 0), (-6, 0, 3), (-2, 0, 1), (0, 0, 0)])
    c.seq("chest", "rotation", T, [(0, 0, 0), (-8, 0, 0), (-3, 0, 0), (0, 0, 0)])
    c.seq("neck", "rotation", T, [(-2, 0, 0), (12, 0, 0), (4, 0, 0), (-2, 0, 0)])
    c.seq("head", "rotation", T, [(2, 0, 0), (22, 0, 8), (8, 0, 3), (2, 0, 0)])
    c.seq("jaw", "rotation", T, [(-2, 0, 0), (-34, 0, 0), (-14, 0, 0), (-2, 0, 0)])
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.seq("ear" + sf, "rotation", T, [(0, 0, 0), (-34, 0, sg * 12), (-14, 0, sg * 4), (0, 0, 0)])
        c.seq("fl_up" + sf, "rotation", T, [(0, 0, 0), (-10, 0, 0), (-4, 0, 0), (0, 0, 0)])
    for i, n in enumerate(FLAMES_N + FLAMES_B):
        c.seq(n, "rotation", T, [(6, 0, 0), (-8, 0, 0), (12, 0, 0), (6, 0, 0)])
    for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
        c.seq(n, "rotation", T, [(0, 0, 0), (-16 - 4 * i, 0, 0), (6, 0, 0), (0, 0, 0)])
    c.plate([(0.0, "closed"), (0.38, "open")])

    # ----- death: stagger, the legs give way, it falls on its side and the fire dies down
    L = 2.0
    c.clip("death", L, False)
    T = [0.0, 0.3, 0.8, 1.3, 2.0]
    c.seq("root", "rotation", T, [(0, 0, 0), (0, 0, 0), (0, 0, 20), (0, 0, 76), (0, 0, 86)])
    c.seq("root", "position", T, [(0, 0, 0), (0, 0, 2), (4, -2.0, 3), (13, 2.0, 2), (14.5, 5.8, 2)])
    c.seq("body", "rotation", T, [(0, 0, 0), (-6, 0, 3), (-3, 0, 6), (2, 0, 2), (2, 0, 2)])
    c.seq("neck", "rotation", T, [(-2, 0, 0), (12, 0, 0), (6, 0, 0), (-14, 0, 0), (-14, 0, 0)])
    c.seq("head", "rotation", T, [(2, 0, 0), (20, 0, 6), (30, 0, 8), (24, 0, 0), (24, 0, 0)])
    c.seq("jaw", "rotation", T, [(-2, 0, 0), (-30, 0, 0), (-18, 0, 0), (-26, 0, 0), (-18, 0, 0)])
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.seq("fl_up" + sf, "rotation", T, [(0, 0, 0), (-12, 0, 0), (28, 0, sg * 10), (46, 0, sg * 10), (42, 0, sg * 10)])
        c.seq("fl_low" + sf, "rotation", T, [(0, 0, 0), (10, 0, 0), (-34, 0, 0), (-46, 0, 0), (-40, 0, 0)])
        c.seq("bl_up" + sf, "rotation", T, [(0, 0, 0), (-10, 0, 0), (36, 0, 0), (60, 0, 0), (52, 0, 0)])
        c.seq("bl_low" + sf, "rotation", T, [(0, 0, 0), (8, 0, 0), (-24, 0, 0), (-50, 0, 0), (-44, 0, 0)])
        c.seq("ear" + sf, "rotation", T, [(0, 0, 0), (-30, 0, sg * 8), (-10, 0, sg * 20), (10, 0, sg * 30), (14, 0, sg * 32)])
    for i, n in enumerate(FLAMES_N + FLAMES_B):
        c.seq(n, "rotation", T, [(6, 0, 0), (30, 0, 0), (-6, 0, 0), (-20, 0, 0), (-24, 0, 0)])
        c.seq(n, "scale", T, [(1, 1, 1), (1, 1, 1), (1, 0.8, 1), (1, 0.45, 1), (1, 0.25, 1)])
    for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
        c.seq(n, "rotation", T, [(0, 0, 0), (-10, 0, 0), (12 + 4 * i, 0, 0), (22 + 6 * i, 0, 0), (22 + 6 * i, 0, 0)])
    c.plate([(0.0, "open"), (0.25, "closed")])

    # ----- sleep: curled on the ground, head on the paws, a slow breath
    L = 4.0
    c.clip("sleep", L, True)
    c.pos("root", lambda t: (0, -6.6 + 0.35 * S(TAU * t / L), 0))
    c.rot("chest", lambda t: (6, 0, 0))
    c.rot("hips", lambda t: (-4, 0, 0))
    c.rot("neck", lambda t: (-6 + 1.0 * S(TAU * t / L), 0, 0))
    c.rot("head", lambda t: (-16, 14, 5))
    c.rot("jaw", hold((-2, 0, 0)))
    for sf, sg in (("_l", 1), ("_r", -1)):
        c.rot("fl_up" + sf, hold((86, 0, sg * -4)))
        c.rot("fl_low" + sf, hold((-72, 0, 0)))
        c.rot("fl_paw" + sf, hold((-6, 0, 0)))
        c.rot("bl_up" + sf, hold((74, 0, sg * -12)))
        c.rot("bl_low" + sf, hold((-100, 0, 0)))
        c.rot("bl_paw" + sf, hold((-6, 0, 0)))
        c.rot("ear" + sf, hold((-22, 0, sg * 12)))
    for i, n in enumerate(FLAMES_N + FLAMES_B):
        c.rot(n, lambda t, i=i: (-14 + 3 * S(TAU * t / L - 0.4 * i), 0, 0))
        c.fn(n, "scale", lambda t: (1, 0.55 + 0.05 * S(TAU * t / L), 1))
    for i, n in enumerate(("tail1", "tail2", "tail3", "tail_f")):
        c.rot(n, lambda t, i=i: (-4 + 6 * i, 40 + 12 * i, 0))
    c.plate([(0.0, "closed")])

    # ----- one static pose per eye plate, for the previews (the engine picks the plate from the entity's state)
    for k in PLATES:
        c.clip("pose_face_" + k, 1.0, True)
        c.plate([(0.0, k)])
        c.fn("head", "rotation", lambda t: (2, 0, 0))
    return c.d


def legs(c, L, ph, front, rear, lift, paw):
    """One leg set: upper swing, the elbow / knee folding while the leg is brought forward, the paw kept flat. ph = phase of each leg."""
    TAU = 2 * math.pi
    for sf in ("_l", "_r"):
        for lg, amp, fold in (("fl", front[0], front[1]), ("bl", rear[0], rear[1])):
            p0 = ph[lg + sf]
            sw = lambda t, p0=p0, amp=amp: amp * S(TAU * t / L + p0)
            fw = lambda t, p0=p0, fold=fold: fold * max(0.0, C(TAU * t / L + p0)) ** 1.0 * lift
            if lg == "fl":
                c.rot("fl_up" + sf, lambda t, sw=sw: (sw(t), 0, 0))
                c.rot("fl_low" + sf, lambda t, fw=fw: (-fw(t), 0, 0))
                c.rot("fl_paw" + sf, lambda t, sw=sw, fw=fw: (-(sw(t) - fw(t)) * paw + 0.35 * fw(t), 0, 0))
            else:
                c.rot("bl_up" + sf, lambda t, sw=sw: (sw(t), 0, 0))
                c.rot("bl_low" + sf, lambda t, fw=fw: (-fw(t) * 0.9, 0, 0))
                c.rot("bl_paw" + sf, lambda t, sw=sw, fw=fw: (-(sw(t) - 0.9 * fw(t)) * paw + 0.25 * fw(t), 0, 0))


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile=(380, 400), texture=None, animations=None):
    s = {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile),
         "floor": True, "figure": False, "expect": [], "anim": anim, "views": views}
    if texture:
        s["texture"] = texture
    if animations:
        s["animations"] = animations
    return s


LOD_MODEL = {0: ID, 1: "nusmp:geo/entity/anime_hound_lod1.geo.json", 2: "nusmp:geo/entity/anime_hound_lod2.geo.json"}


def scenes():
    sc = {}
    tgt = (0, 0.95, 0.15)
    d = 5.2
    sc["hound_front"] = scene(ID, "idle", [0], [view("front", 0, 5, tgt, d * 0.8), view("front 3/4", 35, 9, tgt, d)], tile=(440, 460))
    sc["hound_back"] = scene(ID, "idle", [0], [view("back", 180, 6, tgt, d * 0.8), view("back 3/4", 215, 9, tgt, d)], tile=(440, 460))
    sc["hound_side"] = scene(ID, "idle", [0], [view("side", 90, 3, tgt, d * 0.95), view("side (other)", 270, 3, tgt, d * 0.95)], tile=(640, 440))
    sc["hound_34"] = scene(ID, "idle", [0], [view("3/4 front", 38, 10, tgt, d), view("3/4 rear", 142, 10, tgt, d)], tile=(560, 480))
    # old models for comparison
    for key, geo in (("direwolf", "arc_dungeon_direwolf"), ("bramble", "foundry_bramble_pup")):
        if key == "direwolf":
            tg, dd = (0, 0.95, 0), 4.0
        else:
            tg, dd = (0, 0.95, 0), 4.0
        sc["old_" + key] = scene(geo, "idle", [0], [view("front 3/4", 35, 9, tg, dd), view("side", 90, 3, tg, dd)], tile=(380, 400))
    # faces: close-up of the head of every plate
    fz = (0, 1.30, 1.0)
    for k in PLATES:
        sc["hound_face_" + k] = scene(ID, "pose_face_" + k, [0], [view("face " + k, 0, 3, fz, 1.9), view("3/4", 32, 6, fz, 1.9), view("side", 82, 4, fz, 1.9)], tile=(340, 340))
    # clips
    def strip(clip, secs, n, yaw=40, name=None, wide=1.0, loop=True, lod=0):
        ages = [round(secs * 20.0 * i / (n if not loop else n), 1) for i in range(n)]
        mdl = LOD_MODEL[lod]
        extra = {}
        if lod:
            extra = dict(texture="nusmp:textures/entity/anime_hound.png", animations="nusmp:animations/entity/anime_hound.animation.json")
        return scene(mdl, clip, ages, [view("3/4", yaw, 7, (0, 0.95, 0.15), d * wide * 0.9)], tile=(340, 340), **extra)
    sc["hound_idle"] = strip("idle", 3.2, 5)
    sc["hound_walk"] = strip("walk", 1.2, 6, yaw=75, wide=1.2)
    sc["hound_run"] = strip("run", 0.62, 6, yaw=75, wide=1.25)
    sc["hound_bite"] = strip("bite", 0.8, 6, yaw=62, wide=1.3, loop=False)
    sc["hound_pounce"] = strip("pounce", 1.2, 6, yaw=75, wide=1.7, loop=False)
    sc["hound_howl"] = strip("howl", 3.0, 6, yaw=62, wide=1.3, loop=False)
    sc["hound_hurt"] = strip("hurt", 0.45, 4, yaw=45, wide=1.0, loop=False)
    sc["hound_death"] = strip("death", 2.0, 6, yaw=50, wide=1.35, loop=False)
    sc["hound_sleep"] = strip("sleep", 4.0, 3, yaw=50, wide=1.0)
    # LODs
    for lod in (1, 2):
        sc["hound_lod%d" % lod] = scene(LOD_MODEL[lod], "idle", [0], [view("LOD%d 3/4" % lod, 38, 10, tgt, d), view("LOD%d side" % lod, 90, 3, tgt, d * 1.15)], tile=(380, 400),
                                        texture="nusmp:textures/entity/anime_hound.png", animations="nusmp:animations/entity/anime_hound.animation.json")
    return sc


def write_scenes():
    with open(SCENES_OUT, "w", newline="\n") as f:
        json.dump(scenes(), f, indent=1)
        f.write("\n")


def render_previews(names=None):
    import shutil
    import subprocess
    sc = scenes()
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp_hound"))
    os.makedirs(PREVIEW_OUT, exist_ok=True)
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    for l in (r.stdout + r.stderr).splitlines():
        if ("WARNING" in l or "ERROR" in l or "error" in l or "scenes:" in l or "Exception" in l) and "Picked up" not in l:
            print(l)
    for n in names:
        src = os.path.join(env["RENDER_PREVIEW_BUILD"], n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(PREVIEW_OUT, n + ".png"))
    return r.returncode


def report(h):
    out = []
    for lv, nm in ((0, "LOD0"), (1, "LOD1"), (2, "LOD2")):
        cu, q = h.counts(lv)
        out.append("%s: %d cubes/cards, %d quads" % (nm, cu, q))
    cu, q = h.counts(0, only_fx=True)
    out.append("FX (glow-only): %d cubes/cards, %d quads" % (cu, q))
    out.append("sheet %dx%d, %d bones" % (h.tex[0], h.tex[1], len(h.order)))
    cards = sum(1 for p in h.parts if p.kind == "card")
    out.append("cards %d, cubes %d" % (cards, len(h.parts) - cards))
    return out


def main(argv):
    h = build()
    h.pack_and_paint(256)
    anim = clips()
    h.save(anim)
    write_scenes()
    for l in report(h):
        print(l)


if __name__ == "__main__":
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    main(sys.argv[1:])
