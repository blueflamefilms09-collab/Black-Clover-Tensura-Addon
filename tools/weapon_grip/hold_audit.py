"""
hold_audit: where a held weapon really is in the hand frame, in every context, as numbers (docs/weapon_hold.md).

    python tools/weapon_grip/hold_audit.py                       # the hero weapons (legendary_*, vassal_*), every form, every context; prints the flagged rows
    python tools/weapon_grip/hold_audit.py --all                 # every weapon mesh of the mod (the rule only applies to the hero weapons, see applies())
    python tools/weapon_grip/hold_audit.py --ids vassal_katana,legendary_bow__scouts_bow --verbose
    python tools/weapon_grip/hold_audit.py --old                 # the placement of 4.44.x (no hold rule, no grip fixes): the "before" numbers
    python tools/weapon_grip/hold_audit.py --write               # rewrite tools/weapon_grip/hold_golden.json (what HoldAuditTest compares the Java rule with)
    python tools/weapon_grip/hold_audit.py --csv out.csv

Nothing here needs a game. The chain is the one of docs/weapon_grip.md (vanilla: `T(t/16) * R * S(s) * T(-0.5)` after the holder's own transform; a mesh point is put in the item's unit cube by
WeaponRenderer's placement, then moved by the grip shift), done with numpy over surface samples (at most 3 cm apart) of every mesh. The Java twin of the numbers and of the rule is
`visual/grip/HoldFit`, checked against this file's golden by `HoldAuditTest`.

Frames. HAND FRAME = the pose of `ItemInHandLayer.renderArmWithItem` before the display block (the same one `HandWrap` documents): origin at the end of the arm on its front face; +x outward
(right hand), +y forward (the way the player faces), +z up the arm toward the shoulder. In it the forearm is the box x +-2px, y -4..0px, z 0..12px (player; the armour stand's arm is x -2..0px,
y -3..-1px) and the torso lies beside it at x -10..-2px (right hand; mirrored for the left hand), same y and z range. An armour stand and an item display with a hand context run the very same chain
(ItemInHandLayer / the entity's own origin), so their numbers are the player's. FIRST PERSON: vanilla draws no arm under a held item; the checks are the camera ones.

Checks (a "flag" is a number past its limit). The arm and the torso are modelled at rest (an idle arm); a bow in a draw state (`_pullN`), a swing, a cast raise the arm, so those are not judged.
  FLOAT    third person: the nearest surface point of the weapon is further than FLOAT_MAX from the hand origin (the hand holds nothing)
  ARM      third person: the farthest weapon surface point inside the forearm box is further than ARM_REACH from the hand origin (a part behind the grip goes through the wrist / arm;
           vanilla's own sprite sword reaches 0.30)
  BODY     third person: weapon surface inside the torso box (the weapon goes through the body); the first TORSO_SLACK are not counted (an arm hangs a hair away from the body)
  CAMERA   first person: a surface point nearer than Z_MARGIN to the camera (the weapon reaches through the camera)
  LOW      first person: the lowest point of the weapon on screen is above LOW_MAX (-1 = the bottom edge; vanilla's sword: -1.29): the butt floats in view instead of leaving the screen at the hand
  NONE     item display with no transform ("none"): the nearest surface point is further than FLOAT_MAX from the entity's pivot (the unit cube's centre)
  DIRECTIVE the mesh states where it is held (`grip <fraction>`) and the grip used differs by more than 0.05 (a hand mirror held by its glass instead of its handle)
  SHIELD   a shield on vanilla's shield frame is worse than vanilla's own shield in the same hand by more than SHIELD_SLACK (ARM / BODY / FLOAT)
  NOGRIP   no grip found / no golden entry
"""
import argparse
import csv
import json
import math
import os
import re
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import grip_lib as gl      # noqa: E402
import grip_table as gt    # noqa: E402

PX = 1.0 / 16.0
# ---------------------------------------------------------------- limits (HoldFit.java repeats every one of them)
FLOAT_MAX = 0.10           # blocks
NONE_MAX = 0.15            # blocks: an item display with no transform turns about the cube centre; a pivot this near the weapon is a hand's reach
ARM_REACH = 0.34           # blocks (vanilla's own sword: 0.30)
TORSO_SLACK = 0.5 * PX
LOW_MAX = -1.0
SHIELD_SLACK = 0.05
DIRECTIVE_TOL = 0.05
SAMPLE_STEP = 0.03         # blocks between surface samples
MAX_OUT = 0.20             # the rule: the most it moves a weapon away from the body
OUT_MARGIN = 0.5 * PX
MAX_SLIDE = 0.08           # the rule: the most it slides a weapon along its own axis (the hand toward the butt)
SLIDE_MARGIN = 0.5 * PX
# boxes in the hand frame, right hand {x0, x1, y0, y1, z0, z1}; the left hand is the mirror image in x
ARM_BOX = (-2 * PX, 2 * PX, -4 * PX, 0.0, 0.0, 12 * PX)
STAND_ARM_BOX = (-2 * PX, 0.0, -3 * PX, -1 * PX, 0.0, 12 * PX)
TORSO_Y = (-4 * PX, 0.0)
TORSO_Z = (0.0, 12 * PX)
TORSO_NEAR, TORSO_FAR = 2 * PX, 10 * PX        # the torso's face toward the arm and its far side, measured toward the body

HERO_PREFIX = ("legendary_", "vassal_")
WORN = ("vassal_gauntlet", "vassal_claw")      # HandWrap: worn on the hand, own frame
KEYS = ("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand")
CTX = ("tp_right", "tp_left", "fp_right", "fp_left")
PULL = re.compile(r"_pull[123]$")


def applies(mesh_id):
    """HoldFit.applies: the rule is for the hero weapons' held meshes (not the worn ones, not a draw state of a bow)."""
    return mesh_id.startswith(HERO_PREFIX) and mesh_id.split("__")[0] not in WORN and not PULL.search(mesh_id)


# ---------------------------------------------------------------- mesh reading
def read_mesh(path):
    """(place, body quads [n,4,3], all-layer quads [m,4,3], directive, gripxyz)."""
    place, directive, explicit = None, None, None
    layers = {0: [], 1: [], 2: []}
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "place":
                place = [float(t) for t in v[1:6]]
            elif v[0] == "grip" and len(v) > 1:
                directive = float(v[1])
            elif v[0] == "gripxyz" and len(v) > 3:
                explicit = (float(v[1]), float(v[2]), float(v[3]))
            elif v[0] in ("q", "qn"):
                layer = int(v[1])
                if layer > 2:
                    continue
                st = 5 if v[0] == "q" else 8
                layers[layer].append([[float(v[2 + k * st]), float(v[3 + k * st]), float(v[4 + k * st])] for k in range(4)])
    body = layers[0] or layers[2]
    allq = layers[0] + layers[1] + layers[2]
    return (place, np.array(body, dtype=np.float64).reshape(-1, 4, 3), np.array(allq, dtype=np.float64).reshape(-1, 4, 3), directive, explicit)


def surface(quads, unit, step=SAMPLE_STEP):
    """Bilinear samples on every quad, at most `step` blocks apart (`unit` = blocks per mesh unit) -> (N, 3) in mesh space."""
    if len(quads) == 0:
        return np.zeros((0, 3))
    a, b, c, d = quads[:, 0], quads[:, 1], quads[:, 2], quads[:, 3]
    nu = np.clip(np.ceil(np.maximum(np.linalg.norm(b - a, axis=1), np.linalg.norm(c - d, axis=1)) * unit / step).astype(int), 1, 60)
    nv = np.clip(np.ceil(np.maximum(np.linalg.norm(d - a, axis=1), np.linalg.norm(c - b, axis=1)) * unit / step).astype(int), 1, 60)
    out = []
    for pu, pv in set(zip(nu.tolist(), nv.tolist())):
        sel = (nu == pu) & (nv == pv)
        s = np.linspace(0.0, 1.0, pu + 1)[:, None, None, None]
        t = np.linspace(0.0, 1.0, pv + 1)[None, :, None, None]
        qa, qb, qc, qd = a[sel][None, None], b[sel][None, None], c[sel][None, None], d[sel][None, None]
        pts = (1 - s) * ((1 - t) * qa + t * qd) + s * ((1 - t) * qb + t * qc)
        out.append(pts.reshape(-1, 3))
    return np.concatenate(out, axis=0)


# ---------------------------------------------------------------- the chain
def entry(disp, left):
    """(R, t blocks, s) of an entry as vanilla applies it (a left entry is mirrored first)."""
    rot, tr, sc = disp
    if left:
        rot, tr = (rot[0], -rot[1], -rot[2]), (-tr[0], tr[1], tr[2])
    return np.array(gl.rot_matrix(*rot), dtype=np.float64), np.array(tr, dtype=np.float64) / 16.0, sc


def to_cube(place, pts):
    """WeaponRenderer's placement: mesh points -> the item's unit cube."""
    ox, oy, ang, sc, ymin = place
    a = math.radians(ang)
    along, across = (pts[:, 1] - ymin) * sc, pts[:, 0] * sc
    return np.stack([ox + along * math.cos(a) + across * math.sin(a), oy + along * math.sin(a) - across * math.cos(a), 0.5 + pts[:, 2] * sc], axis=1)


def to_hand(disp, left, cube):
    """Unit-cube points -> the frame the display block hangs on (the hand frame in third person, the camera frame minus the hand in first person)."""
    r, t, s = entry(disp, left)
    return t + s * (cube - 0.5) @ r.T


def in_box(p, box, left):
    x0, x1, y0, y1, z0, z1 = box
    if left:
        x0, x1 = -x1, -x0
    return (p[:, 0] > x0) & (p[:, 0] < x1) & (p[:, 1] > y0) & (p[:, 1] < y1) & (p[:, 2] > z0) & (p[:, 2] < z1)


def toward_body(q, left):
    return q[:, 0] if left else -q[:, 0]


def torso_depth(q, left, slack=TORSO_SLACK):
    """How far past the torso's face the deepest weapon point reaches (0 = none), blocks."""
    sx = toward_body(q, left)
    t = (sx > TORSO_NEAR + slack) & (sx < TORSO_FAR) & (q[:, 1] > TORSO_Y[0]) & (q[:, 1] < TORSO_Y[1]) & (q[:, 2] > TORSO_Z[0]) & (q[:, 2] < TORSO_Z[1])
    return float(sx[t].max() - TORSO_NEAR) if t.any() else 0.0


def arm_reach(q, left, box=ARM_BOX):
    a = in_box(q, box, left)
    return float(np.linalg.norm(q[a], axis=1).max()) if a.any() else 0.0


# ---------------------------------------------------------------- the rule (HoldFit.extra)
def hold_extra(q, d, left, axis_cube):
    """The extra translation of the unit cube (after the grip landed) that clears the torso and the forearm: a sideways move away from the body, then a slide along the weapon's own axis."""
    r, t, s = entry(d, left)
    dq = np.zeros(3)

    def holds(m):                                                    # the hand still holds something: a surface point within FLOAT_MAX of it
        return float(np.linalg.norm(q + m, axis=1).min()) <= FLOAT_MAX

    depth = torso_depth(q, left)
    if depth > 0.0:
        out = min(MAX_OUT, depth + OUT_MARGIN)
        for f in (1.0, 0.75, 0.5, 0.25):                             # as far out as the hand can still reach the weapon
            m = np.array([(-1.0 if left else 1.0) * out * f, 0.0, 0.0])
            if holds(m):
                dq = m
                break
    reach = arm_reach(q + dq, left)
    if reach > ARM_REACH:
        slide = min(MAX_SLIDE, reach - ARM_REACH + SLIDE_MARGIN)
        v = r @ np.array(axis_cube)                                  # the weapon's axis (toward the tip) in the hand frame; the weapon moves along it, the butt comes toward the hand
        u = v / max(np.linalg.norm(v), 1e-9)
        best = reach
        for step in [f * slide * u for f in (1.0, 0.5, 0.25)] + [np.array([0.0, f, 0.0]) for f in (0.02, 0.04, 0.06)]:      # slide along the axis, or move forward out of the arm: the one that clears most and still holds
            m = dq + step
            r_ = arm_reach(q + m, left)
            if r_ < best and holds(m):
                best, pick = r_, m
        if best < reach:
            dq = pick
    return r.T @ dq / s            # hand-frame move -> unit-cube move (q = t + s R (c - 0.5))


# ---------------------------------------------------------------- the shield frame (ShieldFrame.java)
FACE_Z, MAX_W, MAX_H, FP_MAX_W, FP_MAX_H, BACK_MIN, MAX_DEPTH = 2 / 16, 1.3, 1.2, 12 / 16, 1.2, -0.25, 0.85


def shield_fit(box, first_person=False):
    x0, x1, y0, y1, z0, z1 = box
    mw, mh = (FP_MAX_W, FP_MAX_H) if first_person else (MAX_W, MAX_H)

    def fit(w_, h_):
        k = min(w_ / (x1 - x0), h_ / (y1 - y0))
        return [k, -k * (x0 + x1) * 0.5, -k * (y0 + y1) * 0.5, FACE_Z - k * z1]
    f = fit(mw, mh)
    dep = z1 - z0
    if dep > 0:
        if f[0] * dep > MAX_DEPTH:
            f = fit(MAX_DEPTH / dep * (x1 - x0), MAX_DEPTH / dep * (y1 - y0))
        back = f[3] + f[0] * z0
        if back < BACK_MIN:
            f[3] += BACK_MIN - back
    return f


def vanilla_shield_points():
    def box(x0, x1, y0, y1, z0, z1, n=6):
        g = np.linspace(0, 1, n)
        return np.array([(x0 + (x1 - x0) * a, y0 + (y1 - y0) * b, z0 + (z1 - z0) * c) for a in g for b in g for c in g])
    return np.concatenate([box(-6 * PX, 6 * PX, -11 * PX, 11 * PX, 1 * PX, 2 * PX), box(-1 * PX, 1 * PX, -3 * PX, 3 * PX, -5 * PX, 1 * PX)])


def shield_mesh(mesh_id):
    base = mesh_id.split("__")[0]
    for s in ("_curse", "_lod", "_orbit"):
        base = base[:-len(s)] if base.endswith(s) else base
    return base in ("magebane_shield", "legendary_shield", "aegis_of_the_abyss", "celestial_support", "heros_shield", "thunder_shield", "calamity_wave_aegis", "hylian_hero_shield")


# ---------------------------------------------------------------- weapons
def model_ids(mesh_id, forms=True):
    """The model json ids a mesh reads its display block from, in order: its own, the one without a state suffix, and (4.45.0, `forms`) the item before the "__" of a form mesh (GripTable.modelIds)."""
    out = [mesh_id, gl.STATE_SUFFIX.sub("", mesh_id)]
    if forms and "__" in mesh_id:
        item = mesh_id.split("__")[0]
        out += [item, gl.STATE_SUFFIX.sub("", item)]
    return out


def base_json(mesh_id, forms=True):
    for cand in model_ids(mesh_id, forms):
        p = os.path.join(gl.ASSETS, "models", "item", cand + ".json")
        if os.path.exists(p):
            return json.load(open(p, encoding="utf-8"))
    return {}


class Weapon:
    def __init__(self, key, mesh_id, path, golden, forms=True):
        self.key, self.id, self.golden = key, mesh_id, golden
        self.place, self.body, self.allq, self.directive, self.explicit = read_mesh(path)
        unit = self.place[3] if self.place else 1.0
        self.pts = surface(self.body, unit) if self.place else np.zeros((0, 3))
        self.cls = golden["class"] if golden else None
        self.grip = np.array([golden["x"], golden["y"], golden["z"]]) if golden else None
        self.source = golden["source"] if golden else None
        self.fraction = golden["fraction"] if golden else None
        self.display = [gl.read_display(base_json(mesh_id, True), k) for k in KEYS]          # what vanilla really applies: the item's own model json
        self.assumed = [gl.read_display(base_json(mesh_id, forms), k) for k in KEYS]         # what the grip shift was computed with (4.44.1: no json for a form mesh -> no transform, which put the grip at the cube's centre)
        self.shield = shield_mesh(mesh_id)
        self.worn = mesh_id.split("__")[0] in WORN
        if self.place and self.shield:
            self.sbox = tuple(float(v) for v in (self.allq[:, :, 0].min(), self.allq[:, :, 0].max(), self.allq[:, :, 1].min(), self.allq[:, :, 1].max(), self.allq[:, :, 2].min(), self.allq[:, :, 2].max()))


def weapons(args):
    table, golden, _needed = gt.build(hold_fixes=not args.old)
    out = []
    wanted = set(args.ids.split(",")) if args.ids else None
    for key, i, path, kind in gt.mesh_files():
        if kind != "old" or "/" in key or i.endswith(("_lod", "_orbit")):
            continue
        if wanted is not None and i not in wanted:
            continue
        if wanted is None and not args.all and not i.startswith(HERO_PREFIX):
            continue
        sz = os.path.join(gl.ASSETS, "weapon_meshes_sz", i + ".txt")
        use_key, use_path = ("sz/" + i, sz) if os.path.exists(sz) else (key, path)       # the renderer reads the thickened copy when there is one
        out.append(Weapon(use_key, i, use_path, golden.get(use_key), forms=not args.old))
    return out


# ---------------------------------------------------------------- measuring
def measure(w, rule=True):
    """One row per context. rule=False: the 4.44.x placement (the grip lands on the hand, nothing else)."""
    if w.place is None or len(w.pts) == 0 or w.grip is None:
        return [{"ctx": "all", "flag": "NOGRIP"}]
    rows = []
    g_cube = to_cube(w.place, w.grip.reshape(1, 3))[0]
    cube = to_cube(w.place, w.pts)
    box = gl.body_box([tuple(p) for p in w.body.reshape(-1, 3)])
    length = (box[3] - w.place[4]) * w.place[3]
    flags_dir = []
    if w.directive is not None and w.source not in ("directive", "gripxyz") and not w.shield and abs(w.fraction - w.directive) > DIRECTIVE_TOL and not w.worn and w.cls not in ("bow", "crossbow"):
        flags_dir.append("DIRECTIVE")
    ang = math.radians(w.place[2])
    axis = (math.cos(ang), math.sin(ang), 0.0)
    idle = not PULL.search(w.id)
    van = vanilla_shield_points()
    for ci in range(4):
        d = tuple(w.display[ci])
        left = ci in (1, 3)
        fp = ci >= 2
        row = {"ctx": CTX[ci]}
        flags = list(flags_dir) if ci == 0 else []
        if w.worn:
            row["flag"] = ""
            rows.append(row)
            continue
        if w.shield:
            f = shield_fit(w.sbox, fp)
            sq = surface(w.allq, 1.0, SAMPLE_STEP / f[0])             # f[0] blocks per mesh unit: samples SAMPLE_STEP apart after scaling
            pts = sq * f[0] + np.array([f[1], f[2], f[3]])
            q = to_hand(d, left, pts)
            row.update(_shield_numbers(q, left, fp))
            vn = _shield_numbers(to_hand(d, left, van), left, fp)
            row["back"], row["depth"] = f[3] + f[0] * w.sbox[4], f[0] * (w.sbox[5] - w.sbox[4])
            if fp:                                                       # the camera: no nearer than vanilla's own shield (which fills the screen's corner), less a slack
                if row["near"] < vn["near"] - SHIELD_SLACK:
                    flags.append("SHIELD")
            elif row["float"] > vn["float"] + SHIELD_SLACK or row["back"] < BACK_MIN - 1e-3 or row["depth"] > MAX_DEPTH + 1e-3:
                flags.append("SHIELD")                                   # floating further from the hand than vanilla's, straps behind the hand, or deeper than the frame allows
            row["flag"] = ",".join(flags)
            rows.append(row)
            continue
        da = tuple(w.assumed[ci])
        target = np.array(gl.fp_target(da, left) if fp else gl.hand_point(*da, left=left))
        shift = target - g_cube
        if fp:
            share = gl.fp_share(w.place, box, tuple(shift), da, left)
            shift = shift * share
            land = g_cube + shift
            apparent = length * d[2]
            k = max(0.4, 1.5 * 1.15 / apparent) if apparent > 1.5 * 1.15 else 1.0
            c = land + k * (cube + shift - land)
            row["share"], row["k"] = round(share, 3), round(k, 3)
            q = to_hand(d, left, c)
            cam = q + np.array([-gl.HAND_FP[0] if left else gl.HAND_FP[0], gl.HAND_FP[1], gl.HAND_FP[2]])
            row["near"] = float(-cam[:, 2].max())
            row["low_ndc"] = float((cam[:, 1] / np.maximum(-cam[:, 2], 1e-3) / math.tan(math.radians(35.0))).min())
            row["spot"] = float(np.linalg.norm(to_hand(d, left, (g_cube + shift).reshape(1, 3))[0] - to_hand(d, left, target.reshape(1, 3))[0]))
            if row["near"] < gl.Z_MARGIN - 1e-6:
                flags.append("CAMERA")
            if row["low_ndc"] > LOW_MAX:
                flags.append("LOW")
        else:
            c = cube + shift
            q = to_hand(d, left, c)
            extra = np.zeros(3)
            if rule and applies(w.id):
                extra = hold_extra(q, d, left, axis)
                c = c + extra
                q = to_hand(d, left, c)
            row["extra"] = float(np.linalg.norm(extra) * d[2])
            dist = np.linalg.norm(q, axis=1)
            row["float"] = float(dist.min())
            row["arm"] = arm_reach(q, left)
            row["stand_arm"] = arm_reach(q, left, STAND_ARM_BOX)
            row["body"] = torso_depth(q, left)
            if row["float"] > FLOAT_MAX:
                flags.append("FLOAT")
            if idle and row["arm"] > ARM_REACH:
                flags.append("ARM")
            if idle and row["body"] > 0.0:
                flags.append("BODY")
            if ci == 0:                                                   # the unit cube's centre is the pivot of an item display with no transform
                row["none"] = float(np.linalg.norm(cube - 0.5, axis=1).min())
                if row["none"] > NONE_MAX:
                    flags.append("NONE")
        row["flag"] = ",".join(flags)
        rows.append(row)
    return rows


def _shield_numbers(q, left, fp):
    if fp:
        cam = q + np.array([-gl.HAND_FP[0] if left else gl.HAND_FP[0], gl.HAND_FP[1], gl.HAND_FP[2]])
        return {"near": float(-cam[:, 2].max()), "float": 0.0, "arm": 0.0, "body": 0.0}
    return {"float": float(np.linalg.norm(q, axis=1).min()), "arm": arm_reach(q, left), "body": torso_depth(q, left)}


def golden_entry(rows):
    """What the Java test compares: the rule's extra move and the third-person numbers per hand."""
    g = {}
    for r in rows:
        if r["ctx"] in ("tp_right", "tp_left") and "float" in r:
            g[r["ctx"]] = {k: round(r.get(k, 0.0), 3) for k in ("extra", "float", "arm", "body")}
    return g


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--ids")
    ap.add_argument("--old", action="store_true", help="the placement of 4.44.x: no hold rule and no grip-fraction fixes")
    ap.add_argument("--verbose", action="store_true")
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--csv")
    args = ap.parse_args(argv)
    ws = weapons(args)
    flagged, allrows, golden = {}, [], {}
    for w in ws:
        rows = measure(w, rule=not args.old)
        golden[w.key] = golden_entry(rows)
        for r in rows:
            r["id"], r["class"] = w.key, w.cls
            allrows.append(r)
            if r.get("flag"):
                flagged.setdefault(w.key, []).append(r)
        if args.verbose:
            for r in rows:
                print(w.key, {k: (round(v, 3) if isinstance(v, float) else v) for k, v in r.items()})
    print("%d meshes audited, %d flagged%s" % (len(ws), len(flagged), " (old placement)" if args.old else ""))
    by = {}
    for k, rs in sorted(flagged.items()):
        print("%-48s" % k, "; ".join("%s %s" % (r["ctx"], r["flag"]) for r in rs))
        for r in rs:
            for f in r["flag"].split(","):
                by[f] = by.get(f, 0) + 1
    print("flags:", by)
    if args.write and not args.ids and not args.all and not args.old:
        with open(os.path.join(HERE, "hold_golden.json"), "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps({k: v for k, v in golden.items() if v}, indent=1, sort_keys=True) + "\n")
        print("wrote hold_golden.json (%d meshes)" % len([1 for v in golden.values() if v]))
    if args.csv:
        keys = sorted({k for r in allrows for k in r})
        with open(args.csv, "w", newline="") as f:
            wr = csv.DictWriter(f, fieldnames=keys)
            wr.writeheader()
            wr.writerows(allrows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
