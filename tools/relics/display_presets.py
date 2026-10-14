"""display_presets: the oversized-weapon rule (MAS phase 1, docs/asset_pipeline.md "Oversized weapons").

A hand-held 3D weapon is drawn by client/WeaponRenderer from assets/nusmp/weapon_meshes/<id>.txt under the display transforms of
assets/nusmp/models/item/<id>.json (parent minecraft:builtin/entity). The old transforms are vanilla's `handheld` ones (scale 0.68 about the
model centre), which suit a one-block sprite sword. A longer mesh (a polearm, a greatsword, a staff) then rises toward the camera and through the near
plane in first person. This tool

  1. reads each mesh: its `place ox oy angle scale ymin` line and the bounding box of its vertices -> its length in blocks;
  2. for every weapon longer than LONG_BLOCKS (1.4) rewrites ONLY display.firstperson_righthand / firstperson_lefthand of its model json:
     a reduced scale and a translation that keeps the grip where the grip of an ordinary sword is, so the tip stays in view and in front of the
     camera. thirdperson_* (and every other display entry) are left as they are: the weapon stays massive in third person;
  3. 1.29.0: for a weapon longer than LONG_FRAME_BLOCKS (1.6) also shrinks the `ground` and `fixed` (item frame) scales so a lying or framed weapon
     stays about GROUND_APPARENT_MAX / FIXED_APPARENT_MAX blocks long (the inventory shows the old 16 x 16 icon and the head / gui entries are untouched);
     a weapon under the limit gets the base values back;
  4. writes a manifest of what it did (tools/relics/display_presets_manifest.json).

It is idempotent: the result depends only on the mesh and on the rotation already in the json (which it never changes), not on the scale or
translation it wrote last time; running it again changes nothing. A weapon that has shrunk to LONG_BLOCKS or below gets the vanilla handheld
values back (it carries the marker key "nusmp_display_preset" while it is under a preset). A json with "nusmp_display_preset": "manual" is hand-tuned
and never touched. The lead re-runs it after new weapons arrive:

    python tools/relics/display_presets.py            # dry run: prints the table
    python tools/relics/display_presets.py --apply    # rewrites the jsons and the manifest
    python tools/relics/display_presets.py --check    # exit 1 if --apply would change anything (for CI)

THE MATHS (every number is a named constant below; the Java guard client/relics/item/OversizeGuard uses the same ones)

 Mesh space. A mesh is placed by WeaponRenderer as: translate(ox, oy, 0.5); rotateZ(angle - 90); scale(sc); translate(0, -ymin, 0). The mesh y axis
 therefore lies along the direction d = (cos angle, sin angle) in the item's unit cube, the butt (mesh y = ymin) sits at B = (ox, oy), a point at
 mesh y = ym at B + (ym - ymin) * sc * d, mesh x runs along (sin angle, -cos angle) and mesh z along the cube's z about 0.5.
     length L = (ymax - ymin) * sc            [blocks; the cube is one block]

 First-person chain. A point p of the cube lands in camera space (x right, y up, z toward the viewer, the camera looks down -z) at
     c = H + T + R * (s * (p - P)),   P = (0.5, 0.5, 0.5)
 H = (0.56, -0.52, -0.72) is vanilla's resting hand position (ItemInHandRenderer.applyItemArmTransform, right hand), T the json translation / 16, s the
 json scale and R = Rx(rx) * Ry(ry) * Rz(rz) the json rotation (JOML rotationXYZ). With the vanilla [0, -90, 25] the model's +x axis points TOWARD
 the viewer, so a blade running up and to the right rises and comes toward the camera: that is the clipping.

 The grip. The player's hand is at the weapon's grip G = B + GRIP_FRACTION * L * d (the point a quarter of the way up from the butt). Where the
 hand should be is where an ordinary sprite sword's hilt is: ANCHOR = H + T0 + R * (s0 * (Gs - P)), with Gs = (0.25, 0.25, 0.5) the hilt of a sprite
 and (s0, T0) the vanilla handheld scale 0.68 and translation [1.13, 3.2, 1.13] / 16. For a scale s the translation that puts G on ANCHOR is
     T(s) = ANCHOR - H - R * (s * (G - P))                         (in px: * 16)
 and a point q of the weapon then lies at   c(q, s) = ANCHOR + s * R * (q - G).

 The largest scale. Every constraint is linear in s, so each one gives a bound in closed form with  c(q, s) = A + s * v  (A = ANCHOR, v = R (q - G)):
     all eight corners of the mesh box:   c.z <= -Z_MARGIN                          ->  s <= (-Z_MARGIN - A.z) / v.z           (v.z > 0)
     the tip (top-centre of the box):     |c.y| <= Y_VIEW * (-c.z)   and   |c.x| <= X_VIEW * (-c.z)     (each a bound on s)
 with Y_VIEW = 0.92 * tan(35 deg) (the hand is drawn with a fixed 70 degree vertical field of view), X_VIEW = 1.32 (the hand sits at the window's edge:
 x = 0.63 at depth 0.49 is a ratio of 1.30 for the tip of an ordinary sprite sword, so the tip may come no nearer than that), Z_MARGIN = 0.16 (the near
 plane is 0.05). The scale is also capped so that the apparent length s * L <= APPARENT_MAX_BLOCKS, and never goes
 below SCALE_FLOOR (0.28: a weapon that cannot fit even then is listed in the manifest as "tip_out_of_view": true).
     s_new = clamp(min(s0, bounds, APPARENT_MAX_BLOCKS / L), SCALE_FLOOR, s0)

 Left hand. Vanilla mirrors a left-hand transform (y and z rotation and the x translation change sign), so the left entry takes the same scale and
 translation as the right one and keeps its own rotation.
"""
import argparse
import glob
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MESHES = os.path.join(ASSETS, "weapon_meshes")
MODELS = os.path.join(ASSETS, "models", "item")
MANIFEST = os.path.join(HERE, "display_presets_manifest.json")

# ---- named constants (the Java guard in client/relics/item/OversizeGuard repeats the first three)
LONG_BLOCKS = 1.4                       # a weapon longer than this gets a preset
APPARENT_MAX_BLOCKS = 1.0               # no weapon is drawn longer than this in first person (s * L)
SCALE_FLOOR = 0.28                      # never smaller than this
GRIP_FRACTION = 0.25                    # the hand holds the weapon this far up from its butt (share of the length)
HAND = (0.56, -0.52, -0.72)             # vanilla ItemInHandRenderer.applyItemArmTransform, right hand, nothing equipped or swung
BASE_SCALE = 0.68                       # vanilla item/handheld, first person
BASE_TRANSLATION = (1.13, 3.2, 1.13)    # px, the same
BASE_ROTATION = {"firstperson_righthand": (0, -90, 25), "firstperson_lefthand": (0, 90, -25)}
SPRITE_HILT = (0.25, 0.25, 0.5)         # where an ordinary sprite sword is held, in the item's unit cube
Z_MARGIN = 0.16
FOV_HALF_TAN = math.tan(math.radians(35.0))
VIEW_MARGIN = 0.92
Y_VIEW = VIEW_MARGIN * FOV_HALF_TAN
X_VIEW = 1.32                           # |x| <= X_VIEW * depth: an ordinary sprite sword's tip sits at 1.30 (the hand is at the window's edge), so never worse
LONG_FRAME_BLOCKS = 1.6                 # 1.29.0: beyond this the ground and item-frame views shrink too
GROUND_BASE, FIXED_BASE = 0.5, 1.0      # the values every generated model json starts with
GROUND_APPARENT_MAX = 0.8               # a lying weapon is at most this long (ground scale * mesh length)
FIXED_APPARENT_MAX = 1.5                # a framed weapon (the frame already halves the scale) is at most this long (fixed scale * mesh length)
GROUND_FLOOR, FIXED_FLOOR = 0.3, 0.55
MARKER = "nusmp_display_preset"
FIRST = ("firstperson_righthand", "firstperson_lefthand")


# ================================================================ small vector maths (no numpy: the tool must run anywhere)
def rot_matrix(rx, ry, rz):
    """R = Rx * Ry * Rz for degrees (JOML rotationXYZ: a vector is turned about z first)."""
    ax, ay, az = (math.radians(a) for a in (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    rxm = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    rym = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    rzm = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))
    return mat_mul(rxm, mat_mul(rym, rzm))


def mat_mul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))


def mat_vec(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def scale(v, k):
    return tuple(x * k for x in v)


# ================================================================ the mesh
class MeshInfo:
    """What the rule needs of a mesh file: its placement, the box of its vertices, and the length derived from them."""

    def __init__(self, ox, oy, angle, sc, ymin, box):
        self.ox, self.oy, self.angle, self.sc, self.ymin = ox, oy, angle, sc, ymin
        self.box = box                                           # (xmin, xmax, ymin, ymax, zmin, zmax) in mesh units

    @property
    def length(self):
        """Length in blocks along the weapon axis: (ymax - ymin) * scale (the mesh y axis is the weapon's)."""
        return (self.box[3] - self.ymin) * self.sc

    def to_cube(self, mx, my, mz):
        """Mesh coordinates -> the item's unit cube (the space the display transform's centre P = (.5, .5, .5) is in)."""
        a = math.radians(self.angle)
        dx, dy = math.cos(a), math.sin(a)                        # mesh y axis
        ux, uy = math.sin(a), -math.cos(a)                       # mesh x axis
        along = (my - self.ymin) * self.sc
        across = mx * self.sc
        return (self.ox + along * dx + across * ux, self.oy + along * dy + across * uy, 0.5 + mz * self.sc)

    def grip(self, fraction=GRIP_FRACTION):
        a = math.radians(self.angle)
        t = fraction * self.length
        return (self.ox + t * math.cos(a), self.oy + t * math.sin(a), 0.5)

    def corners(self):
        x0, x1, y0, y1, z0, z1 = self.box
        return [self.to_cube(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]

    def tip(self):
        x0, x1, _, y1, z0, z1 = self.box
        return self.to_cube((x0 + x1) / 2, y1, (z0 + z1) / 2)


def read_mesh(path):
    """Parse a weapon_meshes/<id>.txt (the `place` line and the q / qn quads); None if it has no placement or no quads."""
    place, xs, ys, zs = None, [], [], []
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "place":
                place = [float(t) for t in v[1:6]]
            elif v[0] in ("q", "qn"):
                stride = 5 if v[0] == "q" else 8
                for k in range(4):
                    o = 2 + k * stride
                    xs.append(float(v[o]))
                    ys.append(float(v[o + 1]))
                    zs.append(float(v[o + 2]))
    if place is None or not xs:
        return None
    return MeshInfo(place[0], place[1], place[2], place[3], place[4], (min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))


# ================================================================ the rule
def anchor(rotation):
    """Where the grip of an ordinary sprite sword sits in camera space under the vanilla handheld transform (see the module doc)."""
    r = rot_matrix(*rotation)
    p = (0.5, 0.5, 0.5)
    t = scale(BASE_TRANSLATION, 1 / 16.0)
    return add(add(HAND, t), mat_vec(r, scale(sub(SPRITE_HILT, p), BASE_SCALE)))


def _bound_le(a, v, limit):
    """The largest s >= 0 with a + s*v <= limit (None: no bound; a negative result: infeasible even at s = 0)."""
    if v <= 1e-12:
        return None
    return (limit - a) / v


def max_scale(mesh, rotation):
    """The largest first-person scale at which the tip and the whole mesh box are in front of the camera and the tip is in view (see the doc)."""
    r = rot_matrix(*rotation)
    a = anchor(rotation)
    g = mesh.grip()
    bounds = [BASE_SCALE]
    if mesh.length > 0:
        bounds.append(APPARENT_MAX_BLOCKS / mesh.length)
    for q in mesh.corners():                                     # z in front of the camera
        v = mat_vec(r, sub(q, g))
        b = _bound_le(a[2], v[2], -Z_MARGIN)
        if b is not None:
            bounds.append(b)
    v = mat_vec(r, sub(mesh.tip(), g))                           # the tip: |y| <= Y_VIEW * (-z), |x| <= X_VIEW * (-z)
    for axis, k in ((1, Y_VIEW), (0, X_VIEW)):
        for sign in (1, -1):
            # sign*(a_axis + s v_axis) <= k * (-(a_z + s v_z))  ->  s * (sign*v_axis + k*v_z) <= -k*a_z - sign*a_axis
            den = sign * v[axis] + k * v[2]
            if den > 1e-12:
                bounds.append((-k * a[2] - sign * a[axis]) / den)
    return min(bounds)


def translation_for(mesh, rotation, s):
    """The json translation (px) that puts the grip on the ANCHOR at scale s: T = ANCHOR - H - R * (s * (G - P))."""
    r = rot_matrix(*rotation)
    p = (0.5, 0.5, 0.5)
    t = sub(sub(anchor(rotation), HAND), mat_vec(r, scale(sub(mesh.grip(), p), s)))
    return scale(t, 16.0)


def tip_camera(mesh, rotation, s):
    """The tip's camera-space position under (s, the translation for s): for the manifest and the tests."""
    r = rot_matrix(*rotation)
    return add(anchor(rotation), mat_vec(r, scale(sub(mesh.tip(), mesh.grip()), s)))


def tip_in_view(mesh, rotation, s):
    c = tip_camera(mesh, rotation, s)
    d = -c[2]
    return d >= Z_MARGIN - 1e-9 and abs(c[1]) <= Y_VIEW * d + 1e-9 and abs(c[0]) <= X_VIEW * d + 1e-9


def preset(mesh, rotation):
    """(scale, translation_px, tip_in_view) for a mesh under the right-hand rotation; the preset is a pure function of these two."""
    s = max(SCALE_FLOOR, min(BASE_SCALE, max_scale(mesh, rotation)))
    s = math.floor(s * 1000.0) / 1000.0                          # round DOWN: a rounded-up scale could leave the view by a hair
    t = tuple(round(x, 2) for x in translation_for(mesh, rotation, s))
    return s, t, tip_in_view(mesh, rotation, s)


# ================================================================ the files
def _rotation_of(display, key):
    d = display.get(key) or {}
    rot = d.get("rotation")
    return tuple(rot) if rot and len(rot) == 3 else BASE_ROTATION[key]


def view_scales(length):
    """(ground, fixed) scale for a mesh of `length` blocks: the base values up to LONG_FRAME_BLOCKS, then the largest that keeps the apparent length in bounds."""
    if length <= LONG_FRAME_BLOCKS:
        return GROUND_BASE, FIXED_BASE
    g = max(GROUND_FLOOR, min(GROUND_BASE, GROUND_APPARENT_MAX / length))
    f = max(FIXED_FLOOR, min(FIXED_BASE, FIXED_APPARENT_MAX / length))
    return math.floor(g * 1000.0) / 1000.0, math.floor(f * 1000.0) / 1000.0


def plan_for(item_id, model, mesh):
    """What to do with one model json given its mesh: dict(action, ...) -> the new display entries are in 'first'. action in
    skip_not_3d | skip_manual ("nusmp_display_preset": "manual" in the json) | skip_no_display | keep (short, untouched) | restore (was preset, now short) | preset."""
    if model.get("parent") != "minecraft:builtin/entity":
        return {"action": "skip_not_3d"}
    if model.get(MARKER) == "manual":                              # an artist's hand-tuned display: the tool never touches it
        return {"action": "skip_manual"}
    display = model.get("display")
    if not isinstance(display, dict) or not any(k in display for k in FIRST):
        return {"action": "skip_no_display"}
    length = mesh.length
    rot_r = _rotation_of(display, "firstperson_righthand")
    out = {"length": round(length, 3)}
    if length <= LONG_BLOCKS:
        if MARKER in model:
            out["action"] = "restore"
            out["first"] = {k: {"rotation": list(_rotation_of(display, k)), "translation": list(BASE_TRANSLATION), "scale": [BASE_SCALE] * 3} for k in FIRST}
            out["views"] = (GROUND_BASE, FIXED_BASE)
        else:
            out["action"] = "keep"
        return out
    s, t, ok = preset(mesh, rot_r)
    out.update(action="preset", scale=s, translation=list(t), tip_in_view=ok, views=view_scales(length))
    out["first"] = {k: {"rotation": list(_rotation_of(display, k)), "translation": list(t), "scale": [s, s, s]} for k in FIRST}
    return out


def apply_plan(model, plan):
    """The model json with the plan applied (a new dict; only firstperson_* and the marker change)."""
    new = json.loads(json.dumps(model))
    if plan["action"] in ("preset", "restore"):
        for k, v in plan["first"].items():
            new["display"][k] = v
        g, f = plan.get("views", (GROUND_BASE, FIXED_BASE))
        for key, sc in (("ground", g), ("fixed", f)):                     # keep the entry's own rotation / translation, only its scale follows the length
            ent = new["display"].get(key)
            sc = int(sc) if sc == int(sc) else sc                           # 1 stays 1 (the generated jsons write the base values as integers)
            same = ent is not None and json.dumps(ent.get("scale")) == json.dumps([sc, sc, sc])
            if not same and (ent is not None or sc != (GROUND_BASE if key == "ground" else FIXED_BASE)):
                ent = ent or {"rotation": [0, 0, 0] if key == "ground" else [0, 180, 0], "translation": [0, 2, 0] if key == "ground" else [0, 0, 0]}
                ent["scale"] = [sc, sc, sc]
                new["display"][key] = ent
        if plan["action"] == "preset":
            new[MARKER] = {"length_blocks": plan["length"], "scale": plan["scale"], "rule": "tools/relics/display_presets.py"}
        else:
            new.pop(MARKER, None)
    return new


def excluded_ids():
    """Ids other agents own during a release (tools/relics/weapon_classes.json "excluded"): this tool leaves their model jsons alone unless asked."""
    try:
        with open(os.path.join(HERE, "weapon_classes.json"), encoding="utf-8") as f:
            return sorted(json.load(f).get("excluded", {}))
    except (OSError, ValueError):
        return []


def is_excluded(item_id, excluded):
    return any(item_id == e or item_id.startswith(e + "_") for e in excluded)


def run(apply=False, models_dir=MODELS, meshes_dir=MESHES, include_excluded=True):
    """Process every model json that has a mesh. Returns (manifest_entries, changed_ids). include_excluded=False skips the ids of excluded_ids()."""
    entries, changed = [], []
    excluded = [] if include_excluded else excluded_ids()
    for path in sorted(glob.glob(os.path.join(meshes_dir, "*.txt"))):
        item_id = os.path.basename(path)[:-4]
        if is_excluded(item_id, excluded):
            continue
        mjson = os.path.join(models_dir, item_id + ".json")
        if not os.path.exists(mjson):
            continue
        mesh = read_mesh(path)
        if mesh is None:
            continue
        with open(mjson, encoding="utf-8") as f:
            raw = f.read()
        model = json.loads(raw)
        plan = plan_for(item_id, model, mesh)
        entry = {"id": item_id, "action": plan["action"], "length_blocks": round(mesh.length, 3)}
        if plan["action"] == "preset":
            entry.update(first_person_scale=plan["scale"], first_person_translation_px=plan["translation"], tip_out_of_view=not plan["tip_in_view"],
                         ground_scale=plan["views"][0], fixed_scale=plan["views"][1])
        if plan["action"] in ("preset", "restore"):
            new = apply_plan(model, plan)
            text = json.dumps(new, indent=2)
            if text != raw:
                changed.append(item_id)
                entry["rewritten"] = True
                if apply:
                    with open(mjson, "w", encoding="utf-8", newline="\n") as f:
                        f.write(text)
        entries.append(entry)
    return entries, changed


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--apply", action="store_true", help="rewrite the model jsons and the manifest")
    ap.add_argument("--check", action="store_true", help="exit 1 if --apply would change anything")
    ap.add_argument("--include-excluded", action="store_true", help="also process the ids other agents own (Elsdocia, the dual-wield pairs): the lead's final run")
    args = ap.parse_args(argv)
    entries, changed = run(apply=args.apply, include_excluded=args.include_excluded)
    presets = [e for e in entries if e["action"] == "preset"]
    print("%d meshes with a model json; %d over %.1f blocks; %d to rewrite" % (len(entries), len(presets), LONG_BLOCKS, len(changed)))
    for e in sorted(presets, key=lambda e: -e["length_blocks"]):
        print("  %-36s L=%.2f  s=%.3f  T=%s%s%s" % (e["id"], e["length_blocks"], e["first_person_scale"], e["first_person_translation_px"],
                                                    "  OUT-OF-VIEW" if e["tip_out_of_view"] else "", "  (rewritten)" if e.get("rewritten") else ""))
    if args.apply:
        if not args.include_excluded:                                    # keep the manifest rows of the ids this run skipped
            skipped = excluded_ids()
            try:
                with open(MANIFEST, encoding="utf-8") as f:
                    old = json.load(f).get("weapons", [])
            except (OSError, ValueError):
                old = []
            done = {e["id"] for e in entries}
            entries = sorted(entries + [e for e in old if e["id"] not in done and is_excluded(e["id"], skipped)], key=lambda e: e["id"])
        manifest = {"rule": "tools/relics/display_presets.py", "long_blocks": LONG_BLOCKS, "apparent_max_blocks": APPARENT_MAX_BLOCKS,
                    "scale_floor": SCALE_FLOOR, "grip_fraction": GRIP_FRACTION, "long_frame_blocks": LONG_FRAME_BLOCKS, "weapons": entries}
        with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
            json.dump(manifest, f, indent=2)
            f.write("\n")
        print("manifest:", os.path.relpath(MANIFEST, ROOT))
    if args.check and changed:
        print("would change:", ", ".join(changed))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
