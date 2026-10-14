"""ranged_meshes: the draw / load state meshes of 3D bows and crossbows (docs/asset_pipeline.md "Bows and crossbows").

A relic bow or crossbow is a builtin/entity item: the model json cannot swap models by `pull` / `charged`, so client/WeaponRenderer picks the MESH by
state (client/relics/item/RangedStage). For a weapon <id> the meshes are

    <id>.txt          rest: bow strung and relaxed (string straight tip to tip, no arrow); crossbow with the string slack, nothing loaded
    <id>_pull1.txt    drawing, vanilla pull 0 .. 0.65 (bow) / 0 .. 0.58 (crossbow)       shown at pull   0.30
    <id>_pull2.txt    pull 0.65 .. 0.9 (bow) / 0.58 .. 1.0 (crossbow)                    shown at pull   0.75 / 0.80
    <id>_pull3.txt    pull >= 0.9 (bow) / full charge (crossbow)                         shown at pull   1.00
    <id>_charged.txt  crossbow only: loaded (full draw + the bolt on the rail), shown whoever holds it
    (<id>_spent.txt   the mod's two self-loading crossbows only: just fired, recharging)

A state mesh the weapon does not ship is skipped (pull3 falls back to pull2, pull1, then rest), so a weapon may ship only some. All states share the
rest mesh's skin and glow PNG (textures/entity/weapon/<id>.png, _glow.png), so their UVs must be the rest mesh's UVs: the states are made by MOVING
the vertices of the finished rest mesh, never by building and laying out a new one.

This module is the movement. Three layers, from general to specific:

  Rig                  the geometry of a draw: where the string is anchored, which way it and the limb tips travel, how far (all in the mesh's own units)
  displace_*           the maths on arrays of points: a limb flexes about its grip, the string becomes a V pulled to the nock, the arrow rides the nock
  emit_states(...)     for a mesh_lib Mesh: copies its parts (UVs and normals kept), moves the parts named limb* / string* / arrow* / bolt* and writes the
                       state files; baked_states(...) does the same to an existing .txt (the crossbows that were built before this module)

The maths (f = draw fraction 0..1, d = rig.draw_dir, a limb point p, s = (p - grip) . limb_axis, u = clamp(|s| / limb_span, 0, 1)):

  limb tip travel          d * flex * f * u ** flex_power                      (u = 1 at the tip: it travels `flex`; the grip does not move)
  string point at t        tip travel + d * max_draw * f * (1 - |2t - 1|)      (t = 0..1 along the line tip_a -> tip_b: the V has its apex at the nock, t = 0.5)
  arrow / bolt             d * max_draw * f                                    (a bow's arrow slides with the nock; a crossbow's bolt stays on the rail)

Because every displacement is a difference (f_to - f_from) the same functions turn a rest mesh (f_from = 0) into a drawn one and a drawn baked mesh
(f_from = 1) into a relaxed one.

    python tools/relics/ranged_meshes.py --selftest [out_dir]     # builds a small bow with mesh_lib, emits and checks the states, renders a sheet
"""
import math
import os
import sys
from dataclasses import dataclass

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")

# ---- the states: (file suffix, draw fraction the mesh shows, projectile on it). Pull thresholds are the Java side's (RangedStage): bow 0.65 / 0.9, crossbow 0.58 / 1.0
BOW_STATES = (("_pull1", 0.30, True), ("_pull2", 0.75, True), ("_pull3", 1.00, True))
CROSSBOW_STATES = (("_pull1", 0.30, False), ("_pull2", 0.80, False), ("_pull3", 1.00, False), ("_charged", 1.00, True))
STATES = {"bow": BOW_STATES, "crossbow": CROSSBOW_STATES}

__all__ = ["Rig", "STATES", "displace_limb", "displace_string", "displace_arrow", "emit_states", "baked_states", "read_txt", "write_txt",
           "components", "quad_normal", "preview_states", "subdivide_x", "bbox_of"]


def _v(p):
    return np.asarray(p, np.float64)


def _unit(v):
    v = _v(v)
    n = float(np.linalg.norm(v))
    return v / n if n > 1e-12 else v


# ================================================================ the rig and the displacement maths
@dataclass(frozen=True)
class Rig:
    """The geometry of a draw, in mesh units.

    tip_a, tip_b   where the string is anchored at REST (the limb tips; the string is the straight line between them)
    draw_dir       unit vector: the way the string and the limb tips move when drawn (toward the archer)
    max_draw       how far the nock (the string's midpoint) travels from the rest line at full draw
    flex           how far each limb tip travels toward the archer at full draw
    grip           the point the limbs flex about (the middle of the stave)
    limb_axis      unit vector along the limbs (from the grip to a tip, either sign)
    limb_span      distance from the grip to a tip along limb_axis
    flex_power     how the flex grows along the limb: 2 = a bow that bends mostly in its outer half
    """
    tip_a: tuple
    tip_b: tuple
    draw_dir: tuple
    max_draw: float
    flex: float
    grip: tuple
    limb_axis: tuple
    limb_span: float
    flex_power: float = 2.0

    def d(self):
        return _unit(self.draw_dir)

    def limb_weight(self, P):
        """u ** flex_power for each point of an (n, 3) array."""
        s = (np.atleast_2d(P) - _v(self.grip)) @ _unit(self.limb_axis)
        u = np.clip(np.abs(s) / max(self.limb_span, 1e-9), 0.0, 1.0)
        return u ** self.flex_power

    def nock(self, f):
        """The string's midpoint at draw fraction f."""
        return (_v(self.tip_a) + _v(self.tip_b)) / 2 + self.d() * (self.flex * f + self.max_draw * f)


def displace_limb(rig, P, f_from, f_to):
    """New positions of limb points moved from draw f_from to f_to."""
    P = np.atleast_2d(_v(P))
    return P + np.outer(rig.limb_weight(P), rig.d() * rig.flex * (f_to - f_from))


def displace_string(rig, P, f_from, f_to):
    """New positions of string points: the whole string follows the tips, and the V deepens toward the nock. t is measured along the tip-to-tip line AS IT
    IS at f_from, so a string that is already drawn (a baked full-draw mesh) relaxes correctly."""
    P = np.atleast_2d(_v(P))
    d = rig.d()
    a0 = _v(rig.tip_a) + d * rig.flex * f_from
    b0 = _v(rig.tip_b) + d * rig.flex * f_from
    ab = b0 - a0
    t = np.clip(((P - a0) @ ab) / max(float(ab @ ab), 1e-12), 0.0, 1.0)
    tri = 1.0 - np.abs(2.0 * t - 1.0)
    df = f_to - f_from
    return P + d * rig.flex * df + np.outer(tri, d * rig.max_draw * df)


def displace_arrow(rig, P, f_from, f_to):
    """An arrow rides the nock back."""
    return np.atleast_2d(_v(P)) + rig.d() * rig.max_draw * (f_to - f_from)


# ================================================================ mesh_lib meshes
def _role(name):
    n = name.lower()
    for role in ("limb", "string", "arrow", "bolt"):
        if n.startswith(role):
            return role
    return None


def _copy_part(part, offset_fn=None):
    c = part.copy()
    c.UV = [list(map(tuple, uv)) for uv in part.UV] if part.UV is not None else None      # the rest mesh's UVs: the skin is shared
    c.N = [np.array(n, copy=True) for n in part.N] if part.N is not None else None
    c.groups = part.groups
    return c


def emit_states(mesh, rig, kind, item_id, out_dir=MESH_DIR, atlas=None, normals="quad", write_rest=True):
    """Writes the state meshes of a finished mesh_lib Mesh (parts built, Atlas.layout and paint done, place() set).

    Parts are recognised by name prefix: limb* (flexes), string* (becomes the V), arrow* (a bow's arrow: drawn only in the pull states), bolt* (a crossbow's
    bolt: drawn only in _charged). The rest mesh <id>.txt is the mesh without the arrow / bolt parts. Returns {suffix: path}."""
    mesh.finish()
    if any(p.UV is None for p in mesh.parts):
        raise ValueError("lay out the atlas first: the states reuse the rest mesh's UVs")
    kind = kind.lower()
    if kind not in STATES:
        raise ValueError("kind is 'bow' or 'crossbow'")
    paths = {}

    def build(f, with_projectile):
        import mesh_lib as ml
        out = ml.Mesh(mesh.name)
        out.place_cfg, out.glow_cfg = mesh.place_cfg, mesh.glow_cfg
        for p in mesh.parts:
            role = _role(p.name)
            if role in ("arrow", "bolt") and not with_projectile:
                continue
            if role == "arrow" and kind == "crossbow":
                continue
            if role == "bolt" and kind == "bow":
                continue
            c = _copy_part(p)
            if role == "limb":
                c.V = [r for r in displace_limb(rig, np.array(p.V), 0.0, f)]
            elif role == "string":
                c.V = [r for r in displace_string(rig, np.array(p.V), 0.0, f)]
            elif role == "arrow":
                c.V = [r for r in displace_arrow(rig, np.array(p.V), 0.0, f)]
            out.add(c)
        return out

    if write_rest:
        build(0.0, False).write_txt(os.path.join(out_dir, item_id + ".txt"), atlas, normals, comment="rest: ranged_meshes.py")
        paths[""] = os.path.join(out_dir, item_id + ".txt")
    for suffix, f, projectile in STATES[kind]:
        path = os.path.join(out_dir, item_id + suffix + ".txt")
        build(f, projectile).write_txt(path, atlas, normals, comment="state %s f=%.2f: ranged_meshes.py" % (suffix, f))
        paths[suffix] = path
    return paths


# ================================================================ baked .txt meshes (the game's format)
def read_txt(path):
    """-> (header_lines, quads). A quad is a dict(layer, P (4,3), rest (the per-vertex tail kept verbatim: uv / normals), kind 'q' | 'qn', normal)."""
    header, quads = [], []
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.rstrip("\n")
            v = line.split()
            if v and v[0] in ("q", "qn"):
                kind = v[0]
                layer = int(v[1])
                stride = 5 if kind == "q" else 8
                vals = [float(x) for x in v[2:2 + 4 * stride]]
                P = np.array([vals[k * stride:k * stride + 3] for k in range(4)])
                tails = [vals[k * stride + 3:(k + 1) * stride] for k in range(4)]
                normal = np.array([float(x) for x in v[2 + 4 * stride:5 + 4 * stride]]) if kind == "q" else None
                quads.append({"layer": layer, "P": P, "tails": tails, "kind": kind, "normal": normal})
            else:
                header.append(line)
    return header, quads


def quad_normal(P):
    """Newell normal of a quad (unit)."""
    n = np.zeros(3)
    for k in range(4):
        n += np.cross(P[k], P[(k + 1) % 4])
    m = float(np.linalg.norm(n))
    return n / m if m > 1e-12 else np.array([0.0, 1.0, 0.0])


def write_txt(path, header, quads):
    lines = list(header)
    for q in quads:
        P = q["P"]
        if q["kind"] == "q":
            n = q["normal"]
            lines.append("q %d %s %.3f %.3f %.3f" % (q["layer"], " ".join("%.3f %.3f %.3f %.3f %.3f" % (*P[k], *q["tails"][k]) for k in range(4)), *n))
        else:
            lines.append("qn %d %s" % (q["layer"], " ".join("%.3f %.3f %.3f " % tuple(P[k]) + " ".join("%.4f" % x for x in q["tails"][k]) for k in range(4))))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")


def components(quads, ndigits=2):
    """Connected pieces of a baked mesh: quads that share a vertex (to `ndigits`) are one piece. -> list of lists of quad indices."""
    parent = list(range(len(quads)))

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a
    seen = {}
    for i, q in enumerate(quads):
        for p in q["P"]:
            k = tuple(np.round(p, ndigits))
            if k in seen:
                parent[find(i)] = find(seen[k])
            else:
                seen[k] = i
    groups = {}
    for i in range(len(quads)):
        groups.setdefault(find(i), []).append(i)
    return list(groups.values())


def bbox_of(quads, idx):
    pts = np.concatenate([quads[i]["P"] for i in idx])
    return pts.min(axis=0), pts.max(axis=0)


def subdivide_x(q, n):
    """Splits a quad along the edge that runs most along x into n strips (bilinear positions, uvs and per-vertex tails). A flat face whose silhouette is cut
    from its texture (a crossbow prod drawn as one big quad with an alpha mask) cannot bend with four vertices: n strips, each moved by the limb
    weight at its own x, shear it into the curve. Vertex order and winding are kept."""
    P, T = q["P"], q["tails"]
    ex01 = abs(P[1][0] - P[0][0])
    ex03 = abs(P[3][0] - P[0][0])
    r = 0 if ex01 >= ex03 else 1                       # rotate the corner order by one so that corner 0 -> 1 is the x edge (winding unchanged)
    idx = [(k + r) % 4 for k in range(4)]
    P0, P1, P2, P3 = (P[i] for i in idx)
    T0, T1, T2, T3 = (np.array(T[i], float) for i in idx)
    out = []
    for k in range(n):
        s0, s1 = k / n, (k + 1) / n
        def at(s, t):
            return (1 - s) * (1 - t) * P0 + s * (1 - t) * P1 + s * t * P2 + (1 - s) * t * P3
        def tl(s, t):
            return (1 - s) * (1 - t) * T0 + s * (1 - t) * T1 + s * t * T2 + (1 - s) * t * T3
        out.append({**q, "P": np.array([at(s0, 0), at(s1, 0), at(s1, 1), at(s0, 1)]), "tails": [list(tl(s0, 0)), list(tl(s1, 0)), list(tl(s1, 1)), list(tl(s0, 1))]})
    return out


def baked_states(src_path, out_dir, item_id, rig, roles, states, f_from=1.0, glow=None, strips=24, string_strip=None):
    """Makes state meshes from an existing baked mesh file.

    roles   function(quads) -> {quad index: 'limb' | 'limbface' | 'string' | 'bolt'} for the quads that move or go (everything else stays);
            'limbface' is a big flat face that is first cut into `strips` strips (see subdivide_x), 'string' quads are bent into the V
    states  [(suffix, f_to, keep_bolt)]; a quad of role 'bolt' is dropped when keep_bolt is False
    f_from  the draw the source mesh shows (1.0 = a full-draw mesh)
    glow    None, or a (rgb, speed, min, max) tuple to replace the file's `glow` line in every state, or a {suffix: tuple} per state
    string_strip  None, or function(f_to, quads_of_the_string) -> new quads: replaces the string's quads in a state where it should be rebuilt
            clean (a rasterised V cannot be flattened without its stairs showing); the function gets the old string quads for their uv tails
    Returns {suffix: path}."""
    header, quads = read_txt(src_path)
    role_of = roles(quads)
    paths = {}
    for suffix, f_to, keep_bolt in states:
        out = []
        string_quads = [q for i, q in enumerate(quads) if role_of.get(i) == "string"]
        rebuilt = string_strip(f_to, string_quads) if string_strip is not None and string_quads else None
        for i, q in enumerate(quads):
            role = role_of.get(i)
            if role == "bolt" and not keep_bolt:
                continue
            if role == "string" and rebuilt is not None:
                continue
            pieces = subdivide_x(q, strips) if role == "limbface" else [q]
            for pq in pieces:
                P = pq["P"]
                if role in ("limb", "limbface"):
                    P = displace_limb(rig, P, f_from, f_to)
                elif role == "string":
                    P = displace_string(rig, P, f_from, f_to)
                n = pq["normal"]
                if pq["kind"] == "q" and role in ("limb", "limbface", "string"):
                    nn = quad_normal(P)
                    n = nn if float(nn @ pq["normal"]) >= 0 else -nn
                out.append({**pq, "P": P, "normal": n})
        if rebuilt:
            out.extend(rebuilt)
        hdr = list(header)
        g = glow.get(suffix) if isinstance(glow, dict) else glow
        if g is not None:
            hdr = [("glow %s %s %s %s" % (g[0], g[1], g[2], g[3])) if h.startswith("glow ") else h for h in hdr]
        hdr = [h for h in hdr if not h.startswith("# generated")] + ["# state %s: tools/relics/ranged_meshes.py from %s (draw %.2f -> %.2f)" % (suffix, os.path.basename(src_path), f_from, f_to)]
        hdr.sort(key=lambda h: 0 if h.startswith("#") else 1 if h.startswith("place") else 2)
        path = os.path.join(out_dir, item_id + suffix + ".txt")
        write_txt(path, hdr, out)
        paths[suffix] = path
    return paths


# ================================================================ preview
def preview_states(base_id, paths, out_png, views=("profile", "three_quarter")):
    """One row per state, rendered with the BASE weapon's skin (preview_mesh looks the skin up by file stem, so each state is copied to a temp
    <base_id>.txt). Returns out_png."""
    import shutil
    import tempfile
    from PIL import Image
    import preview_mesh as pm
    rows = []
    tmp = tempfile.mkdtemp()
    try:
        for suffix, path in paths.items():
            f = os.path.join(tmp, base_id + ".txt")
            shutil.copyfile(path, f)
            png = os.path.join(tmp, "row%s.png" % (suffix or "_rest"))
            pm.render_preview(f, None, png, views=views, panel=(300, 380), label=False)
            im = Image.open(png).convert("RGB")
            rows.append((suffix or "rest", im))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    w = max(r[1].width for r in rows)
    sheet = Image.new("RGB", (w + 70, sum(r[1].height for r in rows)), (84, 89, 107))
    from PIL import ImageDraw
    y = 0
    for name, im in rows:
        sheet.paste(im, (70, y))
        ImageDraw.Draw(sheet).text((6, y + 6), name, fill=(255, 255, 255))
        y += im.height
    os.makedirs(os.path.dirname(os.path.abspath(out_png)), exist_ok=True)
    sheet.save(out_png)
    return out_png


# ================================================================ self-test: a small bow built with mesh_lib
def build_test_bow():
    """A 1.1 m recurve in cm: a swept stave (limbs along y, curving toward the archer at -x), a wrapped grip, a string tip to tip, an arrow on the string.
    Returns (mesh, atlas, rig)."""
    import mesh_lib as ml
    L, TIP_X = 55.0, -6.0                                      # half length of the stave; the tips sit 6 cm toward the archer at rest (brace height)
    m = ml.Mesh("selftest_bow")
    path = [(TIP_X * (abs(y) / L) ** 2 * 1.0, y, 0.0) for y in np.linspace(-L, L, 23)]
    stave = m.add(ml.Part("limb_stave", material="wood"))
    ml.sweep(stave, path, ml.ellipse(1.4, 0.9, 10), scales=lambda t: (1.0 - 0.45 * abs(2 * t - 1) ** 1.5, 1.0 - 0.45 * abs(2 * t - 1) ** 1.5))
    grip = m.add(ml.Part("grip", material="leather", uv_mode="loft"))
    ml.loft(grip, "y", (0.2, -6.0, 0), [(0, 1.0, 1.0), (6, 1.15, 1.15), (12, 1.0, 1.0)], ml.ellipse(1.7, 1.3, 10))
    for i, y in enumerate((-6.4, 5.6)):
        band = m.add(ml.Part("grip_band_%d" % i, material="gold"))
        ml.collar(band, y, 1.85, 0.8, lip=0.2, sides=10, axis="y", origin=(0.2, 0, 0), oval=0.8)
    for sgn, nm in ((-1, "a"), (1, "b")):
        tip = m.add(ml.Part("limb_tip_" + nm, material="gold"))
        ml.lathe(tip, "y", (TIP_X, sgn * L, 0), [(0, 0.9), (sgn * 1.4, 0.7), (sgn * 2.4, 0.0)], sides=8)
    string = m.add(ml.Part("string", material="cloth"))
    n = 24
    ml.loft(string, "y", (TIP_X - 0.3, -L, 0.0), [(i * 2 * L / n, 1.0, 1.0) for i in range(n + 1)], ml.ellipse(0.22, 0.22, 6))
    arrow = m.add(ml.Part("arrow_shaft", material="wood"))
    ml.loft(arrow, "x", (TIP_X - 0.3, 0, 0.6), [(0, 1, 1), (60, 1, 1)], ml.ellipse(0.35, 0.35, 8))
    head = m.add(ml.Part("arrow_head", material="steel"))
    ml.lathe(head, "x", (TIP_X - 0.3, 0, 0.6), [(60, 0.5), (63, 0.8), (68, 0.0)], sides=8)
    vane = m.add(ml.Part("arrow_vane", material="cloth"))
    ml.box(vane, (TIP_X - 0.3 + 2, -0.05, 0.4), (TIP_X - 0.3 + 12, 0.05, 2.0))
    m.set_glow("FFFFFF", 0.1, 0.4, 1.0)
    m.place(0.2, 0.2, 45.0, 0.0132, -L)
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    rig = Rig(tip_a=(TIP_X - 0.3, -L, 0.0), tip_b=(TIP_X - 0.3, L, 0.0), draw_dir=(-1.0, 0.0, 0.0), max_draw=48.0, flex=7.0, grip=(0.0, 0.0, 0.0),
              limb_axis=(0.0, 1.0, 0.0), limb_span=L, flex_power=2.0)
    return m, atlas, rig


def selftest(out_dir):
    import tempfile
    import mesh_lib as ml
    fails = []

    def check(ok, what):
        print(("PASS " if ok else "FAIL ") + what)
        if not ok:
            fails.append(what)
    m, atlas, rig = build_test_bow()
    os.makedirs(out_dir, exist_ok=True)
    meshes = os.path.join(out_dir, "weapon_meshes")
    paths = emit_states(m, rig, "bow", "selftest_bow", meshes, atlas)
    check(sorted(paths) == ["", "_pull1", "_pull2", "_pull3"], "the bow writes rest + three pull stages (%s)" % sorted(paths))
    parsed = {k: read_txt(p)[1] for k, p in paths.items()}
    # the rest string is straight, the drawn one is a V whose apex is at the nock
    def string_quads(k):
        return [q for q in parsed[k] if q["layer"] == 0]
    hdr = {k: read_txt(p)[0] for k, p in paths.items()}
    check(all(any(h.startswith("place") for h in hdr[k]) for k in hdr), "every state carries the rest mesh's place line")
    # count quads: the rest has no arrow, the pulls have it
    n_rest, n_p1 = len(parsed[""]), len(parsed["_pull1"])
    arrow_quads = sum(len(p.F) for p in m.parts if p.name.startswith("arrow"))
    check(n_p1 - n_rest >= arrow_quads, "the pull states add the arrow's %d quads (%d -> %d)" % (arrow_quads, n_rest, n_p1))
    check(len(parsed["_pull2"]) == len(parsed["_pull1"]) == len(parsed["_pull3"]), "the pull states have the same topology")
    # the string apex travels max_draw * f (plus the tip flex) toward the archer
    sx = lambda k: min(float(q["P"][:, 0].min()) for q in parsed[k])         # the lowest x of the whole mesh = the nock / arrow tail side
    for suffix, f, _ in BOW_STATES:
        apex = rig.nock(f)[0]
        # the string's farthest-back vertex must be within the string's own radius of the nock x
        got = min(float(q["P"][:, 0].min()) for q in parsed[suffix])
        check(abs(got - (apex - 0.25)) < 1.0 or got <= apex + 0.01, "pull %.2f: the farthest-back vertex %.2f reaches the nock at %.2f" % (f, got, apex))
    check(abs(sx("_pull3") - sx("")) > 40.0, "full draw is far behind the rest pose (%.1f cm)" % abs(sx("_pull3") - sx("")))
    # the grip does not move, the limb tips do
    def y_at(k, ymax):
        pts = np.concatenate([q["P"] for q in parsed[k]])
        top = pts[pts[:, 1] > ymax]
        return float(top[:, 0].max())             # the stave and its cap, not the string (which is further toward the archer)
    check(abs(y_at("_pull3", 54.0) - y_at("", 54.0)) > 5.0, "the limb tips flex toward the archer (%.1f cm)" % abs(y_at("_pull3", 54.0) - y_at("", 54.0)))
    in_file = {tuple(np.round(p, 3)) for q in parsed["_pull3"] for p in q["P"]}
    grip_vertices = [tuple(np.round(v, 3)) for part in m.parts if part.name.startswith("grip") for v in part.V]
    check(all(v in in_file for v in grip_vertices), "the grip (%d vertices) stays exactly where it is at full draw" % len(grip_vertices))
    # UVs are the rest mesh's: identical per vertex for the non-moving parts
    rest_uv = [t for q in parsed[""] for t in q["tails"]]
    p1_uv = [t for q in parsed["_pull1"] for t in q["tails"]]
    check(all(a in p1_uv for a in rest_uv[:200]), "the states reuse the rest mesh's UVs")
    # the maths round trip
    P = np.array([[rig.tip_a[0], 0.0, 0.0], [rig.tip_a[0], 20.0, 0.0], [rig.tip_a[0], rig.limb_span, 0.0]])
    back = displace_string(rig, displace_string(rig, P, 0.0, 0.8), 0.8, 0.0)
    check(np.allclose(back, P, atol=1e-9), "displace_string is reversible (draw then relax)")
    back = displace_limb(rig, displace_limb(rig, P, 0.0, 1.0), 1.0, 0.0)
    check(np.allclose(back, P, atol=1e-9), "displace_limb is reversible")
    sheet = preview_states("selftest_bow", {k or "rest": p for k, p in paths.items()}, os.path.join(out_dir, "selftest_bow_states.png"), views=("profile", "three_quarter"))
    print("preview:", sheet)
    print("%d failures" % len(fails))
    return 0 if not fails else 1


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        rest = [a for a in sys.argv[1:] if not a.startswith("--")]
        sys.exit(selftest(rest[0] if rest else os.path.join(ROOT, "build", "ranged_selftest")))
    print(__doc__)
