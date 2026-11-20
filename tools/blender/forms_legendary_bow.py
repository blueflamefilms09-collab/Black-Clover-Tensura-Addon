"""Weapon-form models of the LEGENDARY BOW (hero forms), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_legendary_bow.py [--only hunters_bow,scouts_bow,...] [--trial DIR] [--quick] [--lowres] [--samples N]

mesh ids (docs/heroweapon/form_models.md): legendary_bow__<form> and the draw stages legendary_bow__<form>_pull1 / _pull2 / _pull3 (they share the skin).
--trial DIR  write meshes + skins into DIR instead of the mod's assets
--quick      geometry only: quad estimate + parts table, no bake
--lowres     a fast 256 px bake (look at the shape first), written into --trial only
Every form is built from real geometry (stepped plates, thick cambered feathers, wraps, claws, icicles); the skin is 1024 px, baked at 2048 and boxed down.
Seeded: the same seed gives the same weapon.

Bow rules: tip to tip 2 x 84.239 (the base bow's stave); the place line is the base's grown about the grip (G below) so the mesh is 1.3 blocks long
(BowDisplayGuardTest: 0.9 to 1.4); limb parts are named limb*, the string string*, the arrow arrow* (tools/relics/ranged_meshes.py moves them in the draw stages);
nothing else flexes. Part names avoid the grip words (wrap, grip, handle, butt, foot ...) so no `grip` directive is derived (a bow is held at its riser).
"""
import json
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU, bl  # noqa: E402
import numpy as np  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
MANIFEST = os.path.join(HERE, "forms_manifest_legendary_bow.json")
sys.path.insert(0, os.path.join(K.ROOT, "tools", "relics"))
import ranged_meshes as rm  # noqa: E402

# 4.55.0 (docs/heroweapon/flat_parts_blender.md): the arrow of the draw stages is modelled SOLID; --classic-flat builds the old hairline arrow (the shipped originals: weapon_meshes/flat_parts_old)
SOLID = "--classic-flat" not in sys.argv
LEAN_ABOVE = 4350                # scene quad estimate (before the arrow) above which the solid arrow is built lean (bind_arrow: the rest bow alone is 4916 of the 5000 quad lines)

HALF = 84.239                       # tip to tip is 2 x HALF, the length of the base bow
DRAW, FLEX, FAR = 32.0, 8.0, 400.0  # the draw rig of legendary.py
G = 1.12                            # the place line grown about the grip: 168.5 cm x 0.007978 = 1.344 blocks
SCALE = 0.007123 * G
ORIGIN = 0.5 + 0.70711 * HALF * SCALE
PLACE = (round(ORIGIN, 5), round(ORIGIN, 5), 225.0, round(SCALE, 6), -HALF)
SKIN_PX, BAKE_PX = 1024, 2048
MAX_KB = 350


# ================================================================ scene
def begin(samples=16):
    sc = K.begin(samples)
    sc.render.threads_mode = "FIXED"
    sc.render.threads = 2
    return sc


# ================================================================ the stave: shapes, paths, sections
def bow_x(y, sh):
    """x of the stave at height y: the riser is straight at x = belly, then the limb bends to the tip (curl = recurve toward the target)."""
    u = max(0.0, (abs(y) - sh["riser"]) / (HALF - sh["riser"]))
    cs = sh.get("cstart", 0.78)
    c = max(0.0, (u - cs) / (1.0 - cs))
    pw = sh["power"] if y >= 0 else sh.get("power_lo", sh["power"])
    cu = sh["curl"] if y >= 0 else sh.get("curl_lo", sh["curl"])
    return sh["belly"] * (1.0 - u ** pw) + cu * c * c


def limb_pts(side, sh, n=28, y0=None):
    y0 = sh["riser"] - 1.0 if y0 is None else y0
    ys = [side * (y0 + (HALF - y0) * i / n) for i in range(n + 1)]
    return [V((bow_x(y, sh), y, 0.0)) for y in ys]


def at(P, t):
    """(point, tangent, normal toward +x) on a limb path at t 0..1 (linear between path points)."""
    n = len(P) - 1
    f = min(max(t, 0.0), 1.0) * n
    i = min(int(f), n - 1)
    k = f - i
    p = P[i] * (1 - k) + P[i + 1] * k
    tn = P[min(i + 2, n)] - P[max(i - 1, 0)]
    tn.z = 0.0
    tn.normalize()
    nr = V((tn.y, -tn.x, 0.0))
    if nr.x < 0:
        nr = -nr
    return p, tn, nr


def stave(name, P, secfn, mat, part, caps=(True, True), smooth=True, angle=48.0):
    """Skins a limb path with a section function(t) -> [(xf, z)] (xf toward the target, cm). A closed solid; faces are turned outward."""
    rings = []
    for i in range(len(P)):
        t = i / (len(P) - 1)
        _, tn, nr = at(P, t)
        rings.append([P[i] + nr * a + V((0, 0, z)) for a, z in secfn(t)])
    return K._loft(name, rings, mat, caps, part, False, smooth, angle)


def rsec(w, h, r=0.4, n=2, dx=0.0):
    return [(x + dx, z) for x, z in bl.rrect(w, h, min(r, w * 0.95, h * 0.95), n)]


def lsec(w, h, k=0.35, dx=0.0, back=1.0):
    """A hexagonal 'ridged' section: width w, thickness h, a ridge on the front (string side, -x) and a flatter back."""
    return [(dx + w * back, 0.0), (dx + w * 0.55 * back, h), (dx - w * 0.5, h * (1 - k)), (dx - w, 0.0), (dx - w * 0.5, -h * (1 - k)), (dx + w * 0.55 * back, -h)]


def band(name, P, t, secfn, grow, mat, part, width=0.018, prof=(1.0, 1.28, 1.34, 1.28, 1.0)):
    """A collar round the limb at t: rings of the limb's section blown up by `grow` cm (a binding, a copper sleeve, a clasp)."""
    n = len(P) - 1
    i0 = max(0, int(round((t - width) * n)))
    i1 = min(n, max(i0 + 2, int(round((t + width) * n))))
    sub = P[i0:i1 + 1]
    tm = t

    def fn(tt):
        k = prof[min(len(prof) - 1, int(tt * (len(prof) - 1) + 0.5))]
        return [(x * 1.0 + (grow if x > 0 else -grow) * k * 0.55, z * k + (grow * k * 0.55 if z > 0 else -grow * k * 0.55)) for x, z in secfn(tm)]
    return stave(name, sub, fn, mat, part)


def path_helix(name, P, t0, t1, turns, rx, rz, cord, mat, part, per=7, phase=0.0, sides=4):
    """A cord / wire wound round a limb (real geometry): a thin 4-sided tube along a helix about the limb's centre line."""
    steps = int(turns * per)
    pts = []
    for i in range(steps + 1):
        u = i / steps
        p, tn, nr = at(P, t0 + (t1 - t0) * u)
        a = phase + TAU * turns * u
        pts.append(p + nr * (rx * math.cos(a)) + V((0, 0, rz * math.sin(a))))
    ob = bl.sweep(name, pts, bl.circle(1.0, sides), mat, scale=lambda t: cord, part=part)
    return K._finish_obj(ob, part, True, 55.0)


def on_limb(P, t, off=0.0, z=0.0):
    p, tn, nr = at(P, t)
    return p + nr * off + V((0, 0, z)), tn, nr


def noisy(ob, amp, scale, seed):
    return K.displace(ob, amp, scale, seed, 3)


def shift(obs, dx=0.0, dy=0.0, dz=0.0):
    for o in (obs if isinstance(obs, (list, tuple)) else [obs]):
        bl.move(o, (dx, dy, dz))
    return obs


# ================================================================ string and arrow
def string(sh, mat, r=0.4, part="string", sag=0.0):
    """The string, tip to tip, 13 points (the draw stages bend it into the V). Slightly thicker than the base's for the 1.3 block bow."""
    xa, xb = bow_x(HALF, sh), bow_x(-HALF, sh)
    pts = [V((xa + (xb - xa) * i / 12, HALF - 2 * HALF * i / 12, 0.0)) for i in range(13)]
    ob = bl.sweep(part, pts, bl.circle(1.0, 5), mat, scale=lambda t: r * (1.0 + 0.0 * t), part=part)
    return K._finish_obj(ob, part, True, 55.0)


def arrow_solid(sh, shaft_mat, head_mat, fletch_mat, z, kind="broad", cap_mat=None, glow_mat=None):
    """The arrow of the draw stages, SOLID (4.55.0): an octagonal shaft (2.1 units = 0.017 blocks of radius) with a nock collar and a neck, a hexagonal broadhead flattened to a leaf (squash 0.55)
    (ridge, half width 4.6 units, half thickness about 2) by `kind`, three crossed vanes 1.1 units thick and 5.4 high. Same length as the old arrow (tip at x0 + 76)."""
    x0 = (bow_x(HALF, sh) + bow_x(-HALF, sh)) / 2
    zz = z + FAR
    lean = K.quad_estimate() > LEAN_ABOVE       # a bow that already uses nearly all of the 5000 quad lines gets a hexagonal shaft and no socket collar (the form: ~70 quads instead of ~125)
    sides = 6 if lean else 8
    if lean:
        cap_mat = None
    print("   solid arrow: %s (scene estimate %d)" % ("lean" if lean else "full", K.quad_estimate()))
    sh_ob = bl.lathe("arrow_stem", [(2.8, x0 - 2.6), (2.8, x0 + 1.6), (2.0, x0 + 3.0), (2.1, x0 + 40.0), (2.1, x0 + 61.5), (2.7, x0 + 62.4)] if not lean else [(2.8, x0 - 2.6), (2.8, x0 + 1.6), (2.0, x0 + 3.0), (2.1, x0 + 61.5), (2.7, x0 + 62.4)], shaft_mat, sides=sides, axis="x", origin=(0, 0, zz), part="arrow_stem")
    K._finish_obj(sh_ob, "arrow_stem", True, 55.0)
    hx = x0 + 62.0
    prof = {"broad": [(1.8, hx), (4.8, hx + 3.2), (4.2, hx + 6.6), (0.05, hx + 14.0)],
            "bodkin": [(1.8, hx), (3.4, hx + 3.0), (3.1, hx + 8.0), (0.05, hx + 14.0)],
            "barb": [(1.8, hx), (4.0, hx + 2.6), (2.2, hx + 5.2), (4.8, hx + 8.2), (0.05, hx + 14.0)]}[kind]
    hd = bl.lathe("arrow_head", prof, head_mat, sides=6, squash=0.55, axis="x", origin=(0, 0, zz), caps=(True, False), part="arrow_head")
    K._finish_obj(hd, "arrow_head", True, 40.0)
    if cap_mat is not None:
        bl.lathe("arrow_socket", [(2.0, hx - 3.0), (3.0, hx - 2.4), (3.0, hx - 0.6), (2.0, hx - 0.1)], cap_mat, sides=8, axis="x", origin=(0, 0, zz), part="arrow_head")
    for k in range(3):                                       # fletching: three thick vanes crossed at 120 degrees
        v = bl.plate("arrow_fletch%d" % k, [(x0 - 0.4, 0.9), (x0 + 15.0, 0.9), (x0 + 15.0, 5.4), (x0 + 5.0, 4.6)], -0.55, 0.55, fletch_mat, part="arrow_fletch")
        bl.move(v, rot=(120 * k, 0, 0))
        bl.move(v, (0, 0, zz))
    if glow_mat is not None:
        K.blob("arrow_core", (x0 + 33.0, 0, zz), (31.0, 0.9, 0.9), glow_mat, seed=1, lump=0.0, rings=5, segs=6, part="arrow_stem")


def arrow(sh, shaft_mat, head_mat, fletch_mat, z, kind="broad", cap_mat=None, glow_mat=None):
    """The arrow of the draw stages (baked FAR off in z, brought back by the stage writer). A shaft with a nock, a head by `kind`, three thick cambered vanes."""
    if SOLID:
        return arrow_solid(sh, shaft_mat, head_mat, fletch_mat, z, kind, cap_mat, glow_mat)
    x0 = (bow_x(HALF, sh) + bow_x(-HALF, sh)) / 2
    zz = z + FAR
    sh_ob = bl.lathe("arrow_stem", [(0.7, x0 - 1.7), (0.5, x0 - 1.0), (0.42, x0 + 1.0), (0.46, x0 + 40.0), (0.42, x0 + 66.0)], shaft_mat, sides=7, axis="x", origin=(0, 0, zz), part="arrow_stem")
    K._finish_obj(sh_ob, "arrow_stem", True, 55.0)
    for k in range(2):                                       # nock horns
        a = math.pi * k
        K.tapered("arrow_nock%d" % k, [V((x0 - 1.7, 0.45 * math.cos(a), zz + 0.45 * math.sin(a))), V((x0 - 3.2, 0.75 * math.cos(a), zz + 0.75 * math.sin(a)))], 0.3, 0.14, shaft_mat, sides=4, sub=1, part="arrow_stem")
    hx = x0 + 66.0
    prof = {"broad": [(0.5, hx - 0.4), (1.5, hx + 2.6), (1.0, hx + 5.6), (0.01, hx + 9.0)],
            "bodkin": [(0.55, hx - 0.4), (0.85, hx + 1.6), (0.7, hx + 5.5), (0.01, hx + 9.5)],
            "barb": [(0.5, hx - 0.4), (1.1, hx + 1.4), (0.5, hx + 2.8), (1.4, hx + 4.8), (0.01, hx + 9.0)]}[kind]
    hd = bl.lathe("arrow_head", prof, head_mat, sides=6, axis="x", origin=(0, 0, zz), caps=(True, False), part="arrow_head")
    K._finish_obj(hd, "arrow_head", True, 40.0)
    if cap_mat is not None:
        bl.lathe("arrow_socket", [(0.5, hx - 3.0), (0.85, hx - 2.4), (0.85, hx - 0.8), (0.55, hx - 0.2)], cap_mat, sides=7, axis="x", origin=(0, 0, zz), part="arrow_head")
    for k in range(3):                                       # fletching: thick cambered vanes, a rib and a curl, not flat cards
        a = TAU * k / 3 + 0.5
        d = V((0.0, math.cos(a), math.sin(a)))
        base = V((x0 + 2.2, 0.0, zz))
        K.feather("arrow_fletch%d" % k, base + d * 0.4, V((1, 0, 0)), 13.0, 3.6, fletch_mat, facing=(0, d.z * -1.0 or 1.0, d.y), curl=0.0, rows=6, serrate=0.12,
                  thick=0.26, part="arrow_fletch", seed=40 + k, compact=False, profile=lambda t: min(1.0, t / 0.2 + 0.25) * (1 - t ** 3.2) ** 0.8, bend=d * 1.3)
    if glow_mat is not None:
        K.blob("arrow_core", (x0 + 33.0, 0, zz), (31.0, 0.34, 0.34), glow_mat, seed=1, lump=0.0, rings=5, segs=6, part="arrow_stem")


# ================================================================ the riser pieces every form uses
def riser(name, sh, rs, mat, part="riser", sides_r=1.0):
    """The riser: a loft of (y, half width, half thickness) stations at x = belly; rounded rectangles; returns the object."""
    ob = bl.loft(name, [bl.ring(y, bl.rrect(w, t, min(w, t) * 0.55, 2), ox=bow_x(y, sh)) for y, w, t in rs], mat, part=part)
    return K._finish_obj(ob, part, True, 50.0)


def hand_binding(sh, M_core, M_cord, y0=-10.0, y1=2.4, r=2.6, bands=(), band_mat=None, turns=10.0, cord=0.36, name="hand"):
    """The hand hold (not named grip/wrap: no grip directive): a swelling core with a wound cord and metal collars, moved onto the riser line."""
    obs = K.grip_wrapped(name, y0, y1, r, M_core, M_cord, turns=turns, cord=cord, bands=bands, band_mat=band_mat or M_core, swell=0.12, part="hand_bind", seed=3, sides=12, squash=1.0)
    shift(obs, bow_x(0.0, sh))
    return obs


def jewel(sh, M_gem, M_metal, rr, y=7.6, r=4.3, claws=6, z=3.0, both=True, part="jewel"):
    """The yellow jewel of the Legendary Bow in a bezel with real claws, on both faces of the riser."""
    for s in ((1, -1) if both else (1,)):
        K.gem_setting("gem%d" % s, (bow_x(y, sh), y, z * s), r, M_gem, M_metal, normal=(0, 0, s), claws=claws, sides=8, part=part, rng_=rr, claw_r=0.5)


def spike_row(prefix, P, ts, off, length, r, mat, rr, part, lean=0.5, side_z=0.0, bend=0.6, sides=5, len_jit=0.3, dz=0.0):
    """Thorns / fangs / bristles along a limb path, leaning toward the tip: tapered curved cones, seeded."""
    out = []
    for i, t in enumerate(ts):
        p, tn, nr = at(P, t)
        base = p + nr * off + V((0, 0, side_z + dz * rr.uniform(-1, 1)))
        ln = length * (1.0 + rr.uniform(-len_jit, len_jit))
        d = (nr * (1.0 - lean) + tn * lean + V((0, 0, dz * rr.uniform(-0.5, 0.5)))).normalized()
        pts = [base, base + d * ln * 0.45 + tn * bend * 0.2, base + d * ln * 0.8 + tn * bend * 0.8, base + d * ln + tn * bend * 1.4]
        out.append(K.tapered("%s%d" % (prefix, i), pts, r * rr.uniform(0.85, 1.15), 0.06, mat, sides=sides, sub=2, part=part))
    return out


def tuft(prefix, base, direction, count, length, width, mat, rr, part, spread=28.0, facing=(0, 0, 1), curl=1.6, rows=7, thick=0.22, droop=0.6):
    """A tuft of thick cambered feathers / fur blades fanning from one point (seeded angles, lengths, curls)."""
    out = []
    d0 = V(direction).normalized()
    side = d0.cross(V(facing))
    if side.length < 1e-4:
        side = V((1, 0, 0))
    side.normalize()
    for i in range(count):
        u = (i / max(count - 1, 1)) * 2 - 1
        a = math.radians(u * spread + rr.uniform(-4, 4))
        d = (d0 * math.cos(a) + side * math.sin(a)).normalized()
        out.append(K.feather("%s%d" % (prefix, i), V(base) + V((0, 0, 0.12 * i * (1 if i % 2 else -1))), d, length * rr.uniform(0.82, 1.14), width * rr.uniform(0.85, 1.15), mat, facing=facing,
                             curl=curl * rr.uniform(0.5, 1.4) * (1 if rr.random() > 0.2 else -0.6), rows=rows, serrate=0.18, thick=thick, part=part, seed=rr.randint(0, 9999), compact=False,
                             droop=droop * rr.uniform(0.3, 1.2), ridge=1.8))
    return out


# ================================================================ finish: bake, states, files
def _dirs(trial):
    if trial:
        return os.path.join(trial, "weapon_meshes"), os.path.join(trial, "textures", "entity", "weapon")
    return bl.MESH_DIR, bl.SKIN_DIR


def guard_length(path):
    """The length BowDisplayGuardTest measures: (max y - place ymin) x place scale, over every quad vertex."""
    txt = open(path).read().split("\n")
    pl = next(l for l in txt if l.startswith("place ")).split()
    scale, ymin = float(pl[4]), float(pl[5])
    ymax = -1e9
    for l in txt:
        v = l.split()
        if len(v) >= 22 and v[0] in ("q", "qn"):
            stride = 5 if v[0] == "q" else 8
            for k in range(4):
                ymax = max(ymax, float(v[2 + k * stride + 1]))
    return (ymax - ymin) * scale


def pack_to_budget(skin_dir, mesh_id, kb=MAX_KB):
    """Packs skin + glow with K.pack_png (median blend + palette), lowering the colour count until both fit `kb` together."""
    sk, gl = os.path.join(skin_dir, mesh_id + ".png"), os.path.join(skin_dir, mesh_id + "_glow.png")
    from PIL import Image
    raw_sk = Image.open(sk).convert("RGBA")
    raw_gl = Image.open(gl).convert("RGBA")
    for colors, dn in ((256, 0.5), (192, 0.55), (160, 0.6), (128, 0.65), (96, 0.7), (64, 0.75)):
        raw_sk.save(sk)
        raw_gl.save(gl)
        K.pack_png(sk, colors, dn)
        K.pack_png(gl, min(colors, 128), dn)
        tot = (os.path.getsize(sk) + os.path.getsize(gl)) / 1024.0
        if tot <= kb:
            break
    print("[pack] %s: skin %.0f KB + glow %.0f KB = %.0f KB with %d colours" % (mesh_id, os.path.getsize(sk) / 1024.0, os.path.getsize(gl) / 1024.0, tot, colors))
    return tot


def finish_bow(mesh_id, sh, trial=None, lowres=False, glow=("FFFFFF", 0.12, 0.45, 1.0)):
    """Bakes the joined bow (Cycles, 2048 px boxed to a 1024 skin, glow skin), writes the rest mesh and the three draw stages on the shared skin."""
    t0 = K._T0[0]
    md, sd = _dirs(trial)
    os.makedirs(md, exist_ok=True)
    os.makedirs(sd, exist_ok=True)
    hdr = os.path.join(trial or "/tmp", "hdr_%s.txt" % mesh_id)
    os.makedirs(os.path.dirname(os.path.abspath(hdr)), exist_ok=True)
    with open(hdr, "w") as fh:
        fh.write("place %.5f %.5f %.3f %.6f %.3f\n" % PLACE)
    bake, skin = (256, 256) if lowres else (BAKE_PX, SKIN_PX)
    mesh, atlas, stats = bl.finish(mesh_id, glow=glow, header_from=hdr, bake=bake, skin=skin, out_mesh=md, out_skin=sd, preview=None, write=False, budget=10 ** 6, cage=0.5)
    ml = bl.ml
    rig = rm.Rig(tip_a=(bow_x(HALF, sh), HALF, 0.0), tip_b=(bow_x(-HALF, sh), -HALF, 0.0), draw_dir=(-1, 0, 0), max_draw=DRAW, flex=FLEX, grip=(sh["belly"], 0.0, 0.0), limb_axis=(0, 1, 0), limb_span=HALF)
    nock = rig.flex + rig.max_draw

    def state(f, with_arrow):
        m2 = ml.Mesh(mesh_id)
        m2.finish = lambda: None
        m2.place(*PLACE)
        m2.set_glow(*glow)
        m2.orient_cfg = "manual"
        for p in mesh.parts:
            role = rm._role(p.name)
            if role == "arrow" and not with_arrow:
                continue
            c = rm._copy_part(p)
            if role == "limb":
                c.V = list(rm.displace_limb(rig, np.array(p.V), 0.0, f))
            elif role == "string":
                c.V = list(rm.displace_string(rig, np.array(p.V), 0.0, f))
            elif role == "arrow":
                c.V = list(np.array(p.V) + rig.d() * nock * f - np.array((0.0, 0.0, FAR)))
            m2.add(c)
        return m2

    states = [("", state(0.0, False))] + [(suf, state(f, True)) for suf, f, _ in rm.BOW_STATES]
    from PIL import Image
    out = {}
    for suf, m2 in states:
        path = os.path.join(md, mesh_id + suf + ".txt")
        m2.write_txt(path, atlas, normals="vertex", comment="made in Blender by tools/blender (forms_legendary_bow.py)")
        lines = sum(m2.stats(atlas)["quads"].values())
        out[suf] = (lines, guard_length(path))
        print("%s%s: %d quad lines, guard length %.3f blocks" % (mesh_id, suf, lines, out[suf][1]))
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(sd, mesh_id + ".png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(sd, mesh_id + "_glow.png"), optimize=True)
    kb = pack_to_budget(sd, mesh_id)
    res = {"id": mesh_id, "stages": out, "png_kb": round(kb, 1), "seconds": round(time.time() - t0)}
    base_rep = K.report(mesh_id, "legendary_bow", md, sd, mesh, atlas, time.time() - t0)
    res["apparent"] = base_rep.get("apparent")
    print("[bow] ", res)
    return res


# ==== FORMS (each form is a function(opts) -> shape dict; registered in FORMS below) ====
# ================================================================ 1. HUNTER'S BOW: carved bone, sinew backing, leather lashings, fangs, barred feathers, a fang strand
def hunters_palette():
    S = K.surface
    P = {}
    P["bone"] = S("bone", rgb(226, 212, 176), 0.0, 0.55, wear=0.8, wear_color=rgb(250, 242, 214), dirt=0.9, dirt_color=(0.05, 0.03, 0.012, 1), scratches=0.7, bump="grain", bump_scale=0.9, bump_strength=0.28,
                  mottle=1.0, mottle_color=rgb(150, 110, 60), fine=0.6, seed=31, bevel=0.2, ao_dist=2.2, groove=rgb(120, 86, 46), groove_amount=0.6)
    P["bone2"] = S("bone_dark", rgb(172, 150, 108), 0.0, 0.6, wear=0.9, wear_color=rgb(236, 224, 190), dirt=1.0, dirt_color=(0.04, 0.025, 0.01, 1), scratches=0.6, bump="grain", bump_strength=0.3,
                   mottle=1.0, mottle_color=rgb(96, 64, 30), seed=32, bevel=0.18, ao_dist=2.0)
    P["sinew"] = S("sinew", rgb(176, 124, 70), 0.0, 0.55, bump="wrap", bump_scale=1.0, bump_strength=0.8, groove=rgb(60, 36, 16), groove_amount=0.9, dirt=0.8, wear=0.5, wear_color=rgb(222, 178, 112),
                   mottle=0.8, mottle_color=rgb(96, 58, 28), seed=33, bevel=0.0, fine=0.5)
    P["leather"] = S("hleather", rgb(96, 58, 32), 0.0, 0.62, bump="leather", bump_scale=1.3, bump_strength=0.8, groove=rgb(30, 16, 8), groove_amount=0.9, wear=0.8, wear_color=rgb(168, 112, 68), dirt=0.9,
                     mottle=0.8, mottle_color=rgb(54, 30, 14), seed=34, bevel=0.0, fine=0.5)
    P["cord"] = S("hcord", rgb(206, 170, 108), 0.0, 0.6, bump="wrap", bump_strength=0.9, groove=rgb(70, 46, 20), groove_amount=0.9, dirt=0.7, wear=0.6, seed=35, bevel=0.0, fine=0.4)
    P["feather"] = S("hfeather", rgb(150, 98, 52), 0.0, 0.55, wear=0.4, wear_color=rgb(236, 214, 170), feather=(11, 4, 0.7, 0.8), dirt=0.4,
                     tramp=[(0.0, rgb(76, 44, 22)), (0.35, rgb(168, 112, 58)), (0.7, rgb(236, 220, 186)), (1.0, rgb(92, 52, 28))], mottle=0.7, mottle_color=rgb(60, 34, 16), seed=36, bevel=0.08, fine=0.4)
    P["feather2"] = S("hfeather2", rgb(220, 206, 176), 0.0, 0.55, feather=(11, 4, 0.7, 0.8), dirt=0.5, tramp=[(0.0, rgb(130, 96, 60)), (0.5, rgb(232, 220, 190)), (1.0, rgb(70, 44, 24))],
                      mottle=0.6, mottle_color=rgb(120, 84, 48), seed=37, bevel=0.08)
    P["fang"] = S("fang", rgb(240, 232, 208), 0.0, 0.4, wear=0.6, wear_color=rgb(255, 250, 232), dirt=0.8, dirt_color=(0.05, 0.035, 0.015, 1), bump="grain", bump_strength=0.2, mottle=0.8,
                  mottle_color=rgb(170, 130, 80), seed=38, bevel=0.1)
    P["gem"] = S("hgem", rgb(244, 168, 14), 0.1, 0.06, emit=rgb(255, 190, 40), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 230, 90), seed=40, bevel=0.0)
    P["gold"] = S("hgold", rgb(196, 150, 60), 1.0, 0.34, wear=0.9, wear_color=rgb(250, 226, 150), dirt=0.9, dirt_color=(0.04, 0.025, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.2,
                  mottle=0.8, mottle_color=rgb(110, 70, 20), seed=41, bevel=0.18, ao_dist=2.0)
    P["string"] = S("hstring", rgb(224, 196, 140), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(100, 70, 36), groove_amount=0.8, seed=42, bevel=0.0)
    return P


def hunters_bow(o):
    sh = dict(belly=24.0, power=1.62, curl=9.0, riser=11.0, cstart=0.7, curl_lo=7.5, power_lo=1.7)
    begin(o["samples"])
    P = hunters_palette()
    rr = K.rng(1101)
    L = {1: limb_pts(1, sh, 26), -1: limb_pts(-1, sh, 26)}
    wd = lambda t: 3.5 - 2.4 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.5 - 1.5 * t                   # noqa: E731
    for s in (1, -1):
        sec = lambda t: lsec(wd(t), hh(t), 0.3, 0.0, 1.0)         # noqa: E731
        ob = stave("limb_bone%d" % s, L[s], sec, P["bone"], "limb_bone")
        noisy(ob, 0.32, 0.07, 5 + s)
        # the sinew backing, glued along the back with cross lashings
        stave("limb_sinew%d" % s, L[s], lambda t: lsec(wd(t) * 0.6, hh(t) * 0.7, 0.2, wd(t) + 0.5), P["sinew"], "limb_sinew")
        # bone knuckles carved along the front ridge
        for k, t in enumerate((0.3, 0.62)):
            p, tn, nr = at(L[s], t)
            K.blob("limb_knuckle%d_%d" % (s, k), tuple(p - nr * (wd(t) * 0.6) + V((0, 0, hh(t) * 0.9 * rr.choice((-1, 1))))), (1.5 * (1 - t * 0.5), 2.4 * (1 - t * 0.5), 1.1), P["bone2"], seed=k + 7 * s, lump=0.2, rings=6, segs=8, part="limb_knuckle")
        # leather lashings with hanging thongs and beads
        for k, t in enumerate((0.14, 0.3, 0.46, 0.62, 0.78, 0.9)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.2, hh(t) + 0.1, 0.3, 0.3)       # noqa: E731
            band("limb_lash%d_%d" % (s, k), L[s], t, sc, 0.85 - 0.25 * t, P["leather"] if k % 2 == 0 else P["cord"], "limb_lash", width=0.016 + 0.004 * (k % 2))
            if k in (2, 5):
                p, tn, nr = at(L[s], t)
                base = p + nr * (wd(t) + 1.2) + V((0, 0, hh(t) * 0.5))
                path = [base + tn * (-j * 1.7) + nr * (0.12 * j) + V((0.4 * math.sin(j * 0.9), 0, 0.2 * j * (1 if k == 3 else -1))) for j in range(7)]
                K.ribbon("limb_thong%d_%d" % (s, k), path, lambda tt: 1.5 * (1 - 0.5 * tt) + 0.35, P["leather"], facing=(0, 0, 1), thick=0.16, part="limb_thong", fray=0.5, rng_=rr, edge_wave=0.1, notch=0.6)
                K.blob("limb_bead%d_%d" % (s, k), tuple(path[-1] + tn * -0.9), (0.55, 0.9, 0.55), P["bone"], seed=k + s * 3, lump=0.1, rings=5, segs=6, part="limb_thong")
        # fangs lashed on the back, leaning toward the tip
        spike_row("limb_fang%d_" % s, L[s], (0.26, 0.44, 0.6, 0.74), wd(0.5) + 1.7, 10.0, 1.0, P["fang"], rr, "limb_fang", lean=0.55, bend=1.2, dz=0.6)
        # barred feathers tied on the limb, fletching-fashion
        for k, t in enumerate((0.34, 0.68)):
            p, tn, nr = at(L[s], t)
            base = p + nr * (wd(t) + 1.0) + V((0, 0, 1.0 * (1 if k else -1)))
            for j in range(3):
                d = (tn * 0.92 + nr * (0.18 + 0.3 * j) + V((0, 0, 0.25 * (j - 1)))).normalized()
                K.feather("limb_feather%d_%d_%d" % (s, k, j), base + V((0, 0, 0.18 * j)), d, (25.0 - 4 * k) * rr.uniform(0.85, 1.1), 5.6 - k * 0.8, P["feather"] if (j + k) % 2 == 0 else P["feather2"],
                          facing=(0, 0, 1), curl=2.8, rows=7, serrate=0.2, thick=0.28, part="limb_feather", seed=rr.randint(0, 999), compact=False, droop=1.2 * (1 if j % 2 else -1), ridge=1.9)
        # the bone nock tip: a curved horn with gold rings
        pe, tn, nr = at(L[s], 1.0)
        tip = [pe - tn * 5.0, pe, pe + tn * 0.8 + nr * 1.8, pe + tn * 0.3 + nr * 4.6]
        K.tapered("limb_nock%d" % s, tip, 1.3, 0.28, P["bone2"], sides=6, sub=3, part="limb_nock")
        for k in (0, 1):
            bl.lathe("limb_ring%d_%d" % (s, k), [(1.15, 0), (1.55, 0.3), (1.55, 0.9), (1.15, 1.2)], P["gold"], sides=8, axis="y", origin=(pe.x, pe.y - s * (3.8 - 1.5 * k) - 0.6, 0), part="limb_nock")
    # the riser: bone plates over a core, a cradle of claws round the jewel
    riser("riser_core", sh, [(-14.0, 2.5, 2.7), (-11.0, 3.3, 3.2), (-7.0, 2.9, 3.0), (3.0, 3.0, 3.0), (5.0, 4.4, 3.4), (10.0, 5.2, 3.4), (13.5, 3.6, 3.2), (15.5, 2.3, 2.6)], P["bone2"])
    bx = bow_x(0, sh)
    for s in (1, -1):
        for i, (y0, y1, w) in enumerate(((3.4, 12.0, 4.6), (12.0, 16.0, 3.0))):       # stepped bone plates
            z0, z1 = (2.8, 3.8) if s > 0 else (-3.8, -2.8)
            K.plate("riser_plate%d_%d" % (s, i), [(bx - w, y0), (bx - w * 0.6, y1), (bx + w * 0.6, y1), (bx + w, y0 + 0.8)], z0, z1, P["bone"], part="riser_plate", bevel_w=0.25)
    hand_binding(sh, P["leather"], P["cord"], -9.8, 2.6, 2.7, bands=(-9.2, -4.0, 1.8), band_mat=P["bone"], turns=8)
    jewel(sh, P["gem"], P["gold"], rr, y=7.6, r=3.9, claws=7)
    for k in range(6):                                                     # talons of the cradle curling over the stone
        a = TAU * k / 6 + 0.3
        c = V((bx + 5.6 * math.cos(a), 7.6 + 5.6 * math.sin(a), 0))
        zs = 1 if k % 2 else -1
        K.tapered("riser_talon%d" % k, [c, c + V((2.2 * math.cos(a), 2.2 * math.sin(a), 1.4 * zs)), c + V((3.6 * math.cos(a + 0.5), 3.6 * math.sin(a + 0.5), 2.6 * zs))], 1.0, 0.1, P["fang"], sides=5, sub=2, part="riser_talon")
    strand = [V((bx + (i - 4) * 1.35, -12.5 - 2.2 * math.sin(math.pi * i / 8), 3.7)) for i in range(9)]
    K.tube("riser_strand", [tuple(p) for p in strand], 0.22, P["cord"], sides=5, sub=2, part="riser_strand")
    for i, p in enumerate(strand[1:-1]):
        K.tapered("riser_strand_fang%d" % i, [p, p + V((0.1 * (i - 3), -3.0 - 0.6 * (i % 3), 0.2)), p + V((0.2 * (i - 3), -6.2 - 0.9 * (i % 3), 0.4))], 0.7, 0.07, P["fang"], sides=5, sub=1, part="riser_strand")
    tuft("riser_feather", V((bx - 0.4, -9.4, -3.0)), V((0.1, -1.0, -0.2)), 5, 29.0, 6.0, P["feather"], rr, "riser_feathers", spread=38.0, facing=(0, 0, 1), curl=2.4)
    tuft("riser_feather_b", V((bx + 0.8, -9.4, 3.0)), V((0.5, -1.0, 0.2)), 3, 22.0, 5.4, P["feather2"], rr, "riser_feathers", spread=22.0, facing=(0, 0, 1), curl=-2.2)
    string(sh, P["string"], 0.42)
    arrow(sh, P["bone2"], P["bone"], P["feather"], 3.3, "barb", cap_mat=P["leather"])
    return sh


# ================================================================ 2. SCOUT'S BOW: a slim ash recurve, wound copper wire, copper sleeves and leaf-wings, rabbit-fur ruff and ears, a peep ring
def scouts_palette():
    S = K.surface
    P = {}
    P["wood"] = S("ash", rgb(214, 168, 106), 0.0, 0.5, wear=0.7, wear_color=rgb(246, 214, 160), dirt=0.8, dirt_color=(0.04, 0.025, 0.01, 1), scratches=0.7, bump="grain", bump_scale=1.2, bump_strength=0.35,
                  mottle=1.0, mottle_color=rgb(126, 80, 40), fine=0.5, seed=51, bevel=0.15, ao_dist=2.0, groove=rgb(110, 70, 34), groove_amount=0.7)
    P["wood2"] = S("ash_dark", rgb(132, 86, 46), 0.0, 0.55, wear=0.8, wear_color=rgb(200, 150, 90), dirt=0.9, bump="grain", bump_strength=0.35, mottle=1.0, mottle_color=rgb(70, 40, 18), seed=52, bevel=0.15)
    P["copper"] = S("copper", rgb(214, 122, 74), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 214, 168), dirt=0.9, dirt_color=(0.02, 0.05, 0.04, 1), scratches=0.8, bump="hammer", bump_scale=0.8, bump_strength=0.22,
                    mottle=1.0, mottle_color=rgb(54, 150, 126), fine=0.6, seed=53, bevel=0.18, ao_dist=1.8)
    P["verd"] = S("verdigris", rgb(88, 170, 142), 0.6, 0.45, wear=1.0, wear_color=rgb(232, 140, 90), wear_metal=1.0, dirt=0.8, dirt_color=(0.01, 0.03, 0.03, 1), scratches=0.5, bump="hammer", bump_strength=0.25,
                  mottle=1.0, mottle_color=rgb(200, 120, 80), seed=54, bevel=0.15)
    P["wire"] = S("wire", rgb(230, 138, 84), 1.0, 0.28, wear=0.8, wear_color=rgb(255, 220, 180), dirt=0.6, scratches=0.6, mottle=0.8, mottle_color=rgb(70, 160, 130), seed=55, bevel=0.0, bump="wrap", bump_strength=0.3)
    P["leather"] = S("sleather", rgb(92, 78, 46), 0.0, 0.62, bump="leather", bump_scale=1.3, bump_strength=0.8, groove=rgb(26, 20, 10), groove_amount=0.9, wear=0.8, wear_color=rgb(160, 134, 84), dirt=0.9,
                     mottle=0.8, mottle_color=rgb(46, 38, 20), seed=56, bevel=0.0, fine=0.5)
    P["fur"] = S("fur", rgb(168, 138, 104), 0.0, 0.9, wear=0.3, wear_color=rgb(240, 228, 206), feather=(15, 3, 0.8, 0.3), dirt=0.5, tramp=[(0.0, rgb(92, 70, 48)), (0.5, rgb(190, 164, 128)), (1.0, rgb(246, 238, 222))],
                 mottle=0.8, mottle_color=rgb(110, 84, 56), seed=57, bevel=0.06, fine=0.5)
    P["ear"] = S("ear", rgb(220, 200, 176), 0.0, 0.9, feather=(15, 3, 0.7, 0.4), tramp=[(0.0, rgb(140, 110, 80)), (0.5, rgb(236, 220, 200)), (1.0, rgb(250, 214, 208))], mottle=0.5, mottle_color=rgb(170, 130, 100), seed=58, bevel=0.06)
    P["gem"] = S("sgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=59, bevel=0.0)
    P["string"] = S("sstring", rgb(226, 210, 164), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(110, 90, 50), groove_amount=0.8, seed=60, bevel=0.0)
    P["feather"] = S("sfeather", rgb(98, 124, 70), 0.0, 0.55, feather=(11, 4, 0.7, 0.8), tramp=[(0.0, rgb(50, 70, 36)), (0.5, rgb(112, 140, 78)), (1.0, rgb(226, 226, 190))], mottle=0.6, mottle_color=rgb(60, 80, 40), seed=61, bevel=0.08)
    return P


def scouts_bow(o):
    sh = dict(belly=19.0, power=2.25, curl=15.0, riser=10.0, cstart=0.58, curl_lo=12.0, power_lo=2.1)
    begin(o["samples"])
    P = scouts_palette()
    rr = K.rng(2202)
    L = {1: limb_pts(1, sh, 26), -1: limb_pts(-1, sh, 26)}
    wd = lambda t: 2.7 - 1.7 * t ** 0.8            # noqa: E731
    hh = lambda t: 1.8 - 1.0 * t                   # noqa: E731
    for s in (1, -1):
        ob = stave("limb_wood%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.25), P["wood"], "limb_wood")
        noisy(ob, 0.12, 0.05, 11 + s)
        stave("limb_lam%d" % s, L[s], lambda t: [(x + wd(t) * 0.2, z * 1.14 + (0.18 if z > 0 else -0.18)) for x, z in lsec(wd(t) * 0.28, hh(t) * 0.85, 0.2)], P["wood2"], "limb_lam")   # a dark core lamination on both faces
        # copper sleeves: a long tip sleeve, a mid sleeve, thin hoops
        sl = lambda t: lsec(wd(t) + 0.1, hh(t) + 0.05, 0.25, 0.0)       # noqa: E731
        band("limb_sleeve%d_a" % s, L[s], 0.5, sl, 0.55, P["copper"], "limb_sleeve", width=0.045, prof=(1.0, 1.25, 1.3, 1.3, 1.3, 1.25, 1.0))
        band("limb_sleeve%d_b" % s, L[s], 0.93, sl, 0.5, P["copper"], "limb_sleeve", width=0.05, prof=(1.0, 1.3, 1.3, 1.3, 1.3, 1.0))
        for k, t in enumerate((0.14, 0.27, 0.72)):
            band("limb_hoop%d_%d" % (s, k), L[s], t, sl, 0.45, P["verd"], "limb_hoop", width=0.014)
        path_helix("limb_wire%d" % s, L[s], 0.12, 0.44, 8.0, wd(0.28) + 0.35, hh(0.28) + 0.3, 0.2, P["wire"], "limb_wire", per=5)
        path_helix("limb_wire2%d" % s, L[s], 0.58, 0.82, 6.0, wd(0.7) + 0.35, hh(0.7) + 0.3, 0.2, P["wire"], "limb_wire", per=5, phase=1.0)
        # copper leaf-wings, small, overlapping, laid along the limb on the back
        for k, t in enumerate((0.3, 0.62)):
            p, tn, nr = at(L[s], t)
            for j in range(3):
                d = (tn * (0.85 - 0.1 * j) + nr * (0.35 + 0.22 * j)).normalized()
                K.feather("limb_leaf%d_%d_%d" % (s, k, j), p + nr * (wd(t) + 0.6) + V((0, 0, 0.35 * (j - 1))), d, (14.0 - 3 * k) * (1 - 0.12 * j), 4.0 - 0.6 * k, P["copper"] if j % 2 == 0 else P["verd"], facing=(0, 0, 1),
                          curl=1.8, rows=7, serrate=0.0, thick=0.3, part="limb_leaf", seed=rr.randint(0, 99), compact=False, droop=0.8 * (1 if j else -1), ridge=2.0,
                          profile=lambda u: math.sin(math.pi * min(1.0, u ** 0.7)) ** 0.8)
        # scroll tips of copper: a curl toward the target
        pe, tn, nr = at(L[s], 1.0)
        pts = [pe - tn * 3.0, pe, pe + tn * 1.4 + nr * 1.2, pe + tn * 1.3 + nr * 3.8, pe - tn * 0.8 + nr * 5.2, pe - tn * 2.6 + nr * 3.6, pe - tn * 1.6 + nr * 2.0]
        K.tube("limb_scroll%d" % s, [tuple(p) for p in pts], 0.5, P["copper"], sides=6, sub=3, part="limb_scroll", taper=(0.75, 0.22))
        K.blob("limb_scroll_bead%d" % s, tuple(pts[-1]), (0.55, 0.55, 0.55), P["verd"], seed=s, lump=0.08, rings=5, segs=6, part="limb_scroll")
    # the riser: slim wood, a leather belt with a copper buckle, a peep ring, the stone in a copper setting
    bx = bow_x(0, sh)
    riser("riser_core", sh, [(-13.0, 2.0, 2.3), (-10.0, 2.7, 2.7), (-6.0, 2.4, 2.5), (2.6, 2.4, 2.5), (4.6, 3.6, 2.9), (8.0, 4.4, 3.0), (12.0, 3.2, 2.7), (14.0, 2.0, 2.1)], P["wood2"])
    hand_binding(sh, P["leather"], P["leather"], -9.6, 2.5, 2.55, bands=(-9.0, 1.4), band_mat=P["copper"], turns=7, cord=0.5)
    jewel(sh, P["gem"], P["copper"], rr, y=6.8, r=3.4, claws=5)
    for s in (1, -1):
        z0, z1 = (2.5, 3.2) if s > 0 else (-3.2, -2.5)
        K.plate("riser_buckle%d" % s, [(bx - 2.6, -3.2), (bx + 2.6, -3.2), (bx + 2.6, -0.2), (bx - 2.6, -0.2)], z0, z1, P["copper"], part="riser_buckle", bevel_w=0.3)
        K.plate("riser_buckle_in%d" % s, [(bx - 1.5, -2.6), (bx + 1.5, -2.6), (bx + 1.5, -0.8), (bx - 1.5, -0.8)], z0 + 0.5 * (1 if s > 0 else -1) * 0.0, z1 + 0.35 * (1 if s > 0 else -1), P["leather"], part="riser_buckle", bevel_w=0.15)
        for k, (yy, xx) in enumerate(((-2.9, -2.1), (-2.9, 2.1), (-0.5, 2.1), (-0.5, -2.1))):
            K.rivets("riser_rivet%d_%d" % (s, k), [(V((bx + xx, yy, z1 * 1.0 + 0.15 * s * 0)), (0, 0, s))], P["verd"], r=0.5, detail=False, sides=5, height=0.7)
    ring_c = V((bx + 6.4, 17.6, 0))
    K.tube("riser_peep", [tuple(ring_c + V((3.4 * math.cos(a), 3.4 * math.sin(a), 0.0))) for a in [TAU * i / 12 for i in range(13)]], 0.5, P["copper"], sides=6, sub=1, part="riser_peep")
    K.tapered("riser_peep_strut", [V((bx + 0.5, 15.0, 0)), V((bx + 3.0, 16.5, 0)), ring_c + V((-3.2, -1.0, 0))], 0.7, 0.45, P["verd"], sides=5, sub=1, part="riser_peep")
    K.bl.lathe("riser_peep_bead", [(0.2, -0.5), (0.9, 0), (0.2, 0.5)], P["verd"], sides=6, axis="z", origin=(ring_c.x + 3.4, ring_c.y, 0), part="riser_peep")
    # rabbit-fur ruff under the hand hold, and the long ears hanging from it
    tuft("riser_fur_a", V((bx + 0.2, -9.6, -2.2)), V((0.0, -1.0, -0.15)), 9, 10.5, 3.6, P["fur"], rr, "riser_fur", spread=62.0, facing=(0, 0, 1), curl=1.0, rows=6, thick=0.2)
    tuft("riser_fur_b", V((bx + 0.2, -9.6, 2.2)), V((0.0, -1.0, 0.15)), 9, 10.0, 3.6, P["fur"], rr, "riser_fur", spread=62.0, facing=(0, 0, 1), curl=-1.0, rows=6, thick=0.2)
    for i, (xo, zo, ln) in enumerate(((-1.5, -1.8, 29.0), (1.7, 1.8, 25.0))):
        K.feather("riser_ear%d" % i, V((bx + xo, -11.0, zo)), V((0.12 * (1 if i else -1), -1.0, 0.0)), ln, 6.2, P["ear"], facing=(0, 0, 1), curl=3.0 * (1 if i else -1), rows=10, serrate=0.0, thick=0.3, part="riser_ears",
                  seed=70 + i, compact=False, droop=1.4, ridge=2.2, profile=lambda u: math.sin(math.pi * min(1.0, u ** 0.6)) ** 0.9)
    string(sh, P["string"], 0.38)
    arrow(sh, P["wood"], P["copper"], P["feather"], 3.3, "bodkin", cap_mat=P["verd"])
    return sh


# ================================================================ 3. BIND ARROW: a spider-chitin bow, limbs that end in hooked legs, a cobweb at the riser, eight eyes, slime
def bind_palette():
    S = K.surface
    P = {}
    P["chit"] = S("chitin", rgb(46, 30, 66), 0.75, 0.32, wear=1.0, wear_color=rgb(170, 130, 220), wear_metal=0.9, dirt=0.9, dirt_color=(0.004, 0.002, 0.01, 1), scratches=0.7, bump="scales", bump_scale=0.8, bump_strength=0.3,
                  mottle=1.0, mottle_color=rgb(88, 40, 96), fine=0.5, seed=71, bevel=0.2, ao_dist=2.2)
    P["chit2"] = S("chitin2", rgb(96, 58, 110), 0.6, 0.38, wear=1.0, wear_color=rgb(214, 170, 240), wear_metal=0.8, dirt=0.9, dirt_color=(0.01, 0.004, 0.015, 1), scratches=0.6, bump="hammer", bump_strength=0.2,
                   mottle=1.0, mottle_color=rgb(50, 24, 66), seed=72, bevel=0.16)
    P["slime"] = S("slime", rgb(110, 222, 80), 0.0, 0.08, transmission=0.8, ior=1.15, rim=(rgb(230, 255, 190), 0.6), mottle=0.7, mottle_color=rgb(60, 180, 90),
                   bump="leather", bump_scale=0.4, bump_strength=0.12, seed=73, bevel=0.0)
    P["web"] = S("web", rgb(226, 230, 236), 0.0, 0.5, wear=0.5, wear_color=rgb(255, 255, 255), dirt=0.6, dirt_color=(0.05, 0.05, 0.06, 1), bump="wrap", bump_strength=0.4, mottle=0.7, mottle_color=rgb(150, 160, 176), seed=74, bevel=0.0)
    P["eye"] = S("eye", rgb(255, 30, 40), 0.1, 0.05, emit=rgb(255, 40, 40), emit_strength=1.2, emit_flicker=0.3, mottle=0.8, mottle_color=rgb(255, 120, 90), seed=75, bevel=0.0)
    P["gem"] = S("bgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=76, bevel=0.0)
    P["metal"] = S("bmetal", rgb(120, 112, 134), 1.0, 0.4, wear=0.9, wear_color=rgb(220, 214, 232), dirt=0.9, scratches=0.7, bump="hammer", bump_strength=0.2, mottle=0.6, seed=77, bevel=0.15)
    P["fang"] = S("bfang", rgb(210, 200, 224), 0.2, 0.3, wear=0.8, wear_color=rgb(255, 250, 255), dirt=0.8, bump="grain", bump_strength=0.2, mottle=0.8, mottle_color=rgb(110, 90, 130), seed=78, bevel=0.1)
    return P


def bind_arrow(o):
    sh = dict(belly=29.0, power=1.55, curl=9.0, riser=12.0, cstart=0.55, curl_lo=8.0, power_lo=1.6)
    begin(o["samples"])
    P = bind_palette()
    rr = K.rng(3303)
    L = {1: limb_pts(1, sh, 26), -1: limb_pts(-1, sh, 26)}
    wd = lambda t: 3.5 - 2.1 * t ** 0.9            # noqa: E731
    hh = lambda t: 2.3 - 1.2 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_chit%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.4, 0.0, 0.9), P["chit"], "limb_chit")
        noisy(ob, 0.2, 0.08, 21 + s)
        # overlapping carapace segments (armour bands with a raised ridge) and big joint knobs
        for k, t in enumerate((0.1, 0.2, 0.3, 0.42, 0.55, 0.68, 0.8)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.3, hh(t) + 0.25, 0.4, 0.0, 0.9)       # noqa: E731
            band("limb_seg%d_%d" % (s, k), L[s], t, sc, 0.45, P["chit2"] if k % 2 else P["chit"], "limb_seg", width=0.026 - 0.002 * k, prof=(1.0, 1.22, 1.3, 1.22, 1.0))
        for k, t in enumerate((0.34, 0.66)):
            p, tn, nr = at(L[s], t)
            K.blob("limb_joint%d_%d" % (s, k), tuple(p + nr * 0.6), (2.6 - 0.5 * k, 3.1 - 0.6 * k, 2.2), P["chit2"], seed=k + 5 * s, lump=0.16, rings=7, segs=9, part="limb_joint")
        # bristles on the back
        spike_row("limb_bristle%d_" % s, L[s], [0.14 + 0.1 * i for i in range(8)], wd(0.5) + 0.4, 5.5, 0.42, P["chit2"], rr, "limb_bristle", lean=0.6, bend=0.8, dz=0.8, sides=3)
        # two folded spider legs: femur, knee knob, tibia, claw
        for k, t in enumerate((0.3, 0.58)):
            p, tn, nr = at(L[s], t)
            base = p + nr * (wd(t) + 0.4) + V((0, 0, hh(t) * 0.4 * (1 if k else -1)))
            knee = base + nr * (10.5 - 2 * k) + tn * (6.5 + 3 * k) + V((0, 0, 2.5 * (1 if k else -1)))
            foot = knee + nr * 3.5 + tn * (11.0 - 2 * k) + V((0, 0, -1.2 * (1 if k else -1)))
            K.tube("limb_leg%d_%d" % (s, k), [tuple(base), tuple(base + nr * 5.0 + tn * 2.8), tuple(knee), tuple(knee + nr * 2.5 + tn * 6.0), tuple(foot)], 0.9, P["chit2"], sides=5, sub=1, part="limb_leg", taper=(1.15, 0.38))
            K.blob("limb_knee%d_%d" % (s, k), tuple(knee), (1.45, 1.45, 1.45), P["chit"], seed=k + 3 * s, lump=0.14, rings=6, segs=8, part="limb_leg")
            K.tapered("limb_claw%d_%d" % (s, k), [foot, foot + tn * 1.8 + nr * 1.4, foot + tn * 3.6 + nr * 1.0 - V((0, 0, 0.5))], 0.4, 0.05, P["fang"], sides=4, sub=1, part="limb_leg")
        # slime clumps
        for k, t in enumerate((0.28, 0.64)):
            p, tn, nr = at(L[s], t)
            K.blob("limb_slime%d_%d" % (s, k), tuple(p - nr * (wd(t) * 0.5) + V((0, 0, hh(t) * 0.9 * (1 if k % 2 else -1)))), (1.5, 2.0, 1.2), P["slime"], seed=k + 2 * s, lump=0.25, rings=6, segs=8, part="limb_slime")
            K.drip("limb_drip%d_%d" % (s, k), tuple(p - nr * (wd(t) * 0.5) + V((0, 0, hh(t) * 0.9 * (1 if k % 2 else -1)))), 6.0 + 3 * k, 0.75, P["slime"], seed=k + s, sides=6, part="limb_slime", bend=(0.3, 0, 0))
        # the hooked leg-tip: a long curved fang
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_hook%d" % s, [pe - tn * 4.0, pe, pe + tn * 1.0 + nr * 2.8, pe + nr * 5.5 - tn * 0.8], 1.45, 0.12, P["fang"], sides=6, sub=3, part="limb_hook")
        K.bl.lathe("limb_hook_ring%d" % s, [(1.5, 0), (2.0, 0.35), (2.0, 1.0), (1.5, 1.35)], P["metal"], sides=8, axis="y", origin=(pe.x, pe.y - s * 4.0, 0), part="limb_hook")
    # riser: carapace plates, eight eyes, mandibles
    riser("riser_core", sh, [(-14.0, 2.5, 2.6), (-11.0, 3.4, 3.1), (-7.0, 3.0, 2.9), (3.0, 3.0, 2.9), (5.0, 4.4, 3.3), (10.0, 5.2, 3.3), (14.0, 3.6, 3.1), (16.5, 2.3, 2.5)], P["chit"])
    hand_binding(sh, P["chit2"], P["web"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["metal"], turns=6, cord=0.34)
    plate = [(bx - 1.9, 12.0), (bx - 3.9, 15.0), (bx - 2.3, 19.0), (bx, 20.2), (bx + 2.3, 19.0), (bx + 3.9, 15.0), (bx + 1.9, 12.0)]
    for s in (1, -1):
        K.stepped_plate("riser_carapace%d" % s, plate, 2.4 * s if s > 0 else -2.4, P["chit2"], steps=3, step_h=0.55, inset=0.55, part="riser_carapace", up=s)
        lo = [(bx - 2.9, 2.4), (bx - 4.2, 5.0), (bx - 3.4, 9.0), (bx, 11.0), (bx + 3.4, 9.0), (bx + 4.2, 5.0), (bx + 2.9, 2.4)]
    for s in (1, -1):
        zz = 3.6 * s
        for k, (xx, yy, rd) in enumerate(((-1.3, 17.4, 1.25), (1.3, 17.4, 1.25), (-2.7, 15.6, 0.9), (2.7, 15.6, 0.9), (-1.2, 14.2, 0.6), (1.2, 14.2, 0.6))):
            K.blob("eye%d_%d" % (s, k), (bx + xx, yy, zz + 0.2 * s), (rd, rd, rd * 0.8), P["eye"], seed=k, lump=0.05, rings=5, segs=6, part="riser_eyes")
    jewel(sh, P["gem"], P["metal"], rr, y=7.6, r=3.5, claws=6)
    for s in (1, -1):                                                         # mandibles hanging under the hand hold, dripping
        K.tapered("riser_mandible%d" % s, [V((bx + 1.0 * s, -10.5, 1.6 * s)), V((bx + 2.8 * s, -15.0, 2.4 * s)), V((bx + 3.4 * s, -20.5, 1.6 * s)), V((bx + 1.6 * s, -25.0, 0.6 * s))], 1.7, 0.1, P["fang"], sides=6, sub=3, part="riser_mandible")
        K.drip("riser_mandible_drip%d" % s, (bx + 1.6 * s, -24.0, 0.8 * s), 8.0, 0.8, P["slime"], seed=s + 10, sides=6, part="riser_mandible")
    # the cobweb: radials and spiral threads in the plane of the bow, behind the riser, hung with slime beads
    c = V((bx + 12.0, 3.0, -0.4))
    R = 22.0
    nrad = 8
    ang = [TAU * (i + rr.uniform(-0.25, 0.25)) / nrad for i in range(nrad)]
    rad_len = [R * rr.uniform(0.82, 1.1) for _ in range(nrad)]
    for i, a in enumerate(ang):
        d = V((math.cos(a), math.sin(a), 0))
        K.tube("web_radial%d" % i, [tuple(c + d * rad_len[i] * f + V((0, 0, 0.25 * math.sin(f * 5 + i)))) for f in (0.0, 0.35, 0.7, 1.0)], 0.4, P["web"], sides=4, sub=1, part="web_radials")
    for ri, f in enumerate((0.36, 0.58, 0.78, 0.95)):
        for i in range(nrad):
            a0, a1 = ang[i], ang[(i + 1) % nrad] + (TAU if i == nrad - 1 else 0)
            p0 = c + V((math.cos(a0), math.sin(a0), 0)) * rad_len[i] * f
            p1 = c + V((math.cos(a1), math.sin(a1), 0)) * rad_len[(i + 1) % nrad] * f
            mid = (p0 + p1) * 0.5
            mid = c + (mid - c) * (0.9 + 0.02 * ri) + V((0, 0, 0.25 * (1 if (i + ri) % 2 else -1)))
            K.tube("web_ring%d_%d" % (ri, i), [tuple(p0), tuple(mid), tuple(p1)], 0.27, P["web"], sides=4, sub=1, part="web_rings")
    for i in range(0, nrad, 3):
        e = c + V((math.cos(ang[i]), math.sin(ang[i]), 0)) * rad_len[i] * 0.97
        K.blob("web_bead%d" % i, tuple(e), (0.9, 1.2, 0.9), P["slime"], seed=i, lump=0.18, rings=5, segs=6, part="web_beads")
        K.drip("web_drip%d" % i, tuple(e), 4.5 + 2.5 * (i % 3), 0.5, P["slime"], seed=i + 30, sides=5, part="web_beads")
    string(sh, P["web"], 0.38)
    arrow(sh, P["chit"], P["fang"], P["web"], 3.3, "barb", cap_mat=P["slime"])
    return sh


# ================================================================ 4. BIRD HUNTING: spirit-bone limbs carrying phantom-membrane wings on bone fingers, plumage, bird talons round the riser, a rabbit's foot charm
def bird_palette():
    S = K.surface
    P = {}
    P["bone"] = S("pbone", rgb(214, 224, 232), 0.1, 0.42, wear=0.8, wear_color=rgb(250, 252, 255), dirt=0.8, dirt_color=(0.02, 0.03, 0.05, 1), scratches=0.7, bump="grain", bump_scale=0.9, bump_strength=0.25,
                  mottle=1.0, mottle_color=rgb(120, 156, 184), fine=0.5, seed=81, bevel=0.2, ao_dist=2.0)
    P["bone2"] = S("pbone2", rgb(150, 176, 196), 0.2, 0.45, wear=0.9, wear_color=rgb(226, 238, 250), dirt=0.9, dirt_color=(0.01, 0.02, 0.04, 1), bump="grain", bump_strength=0.3, mottle=1.0, mottle_color=rgb(70, 100, 130), seed=82, bevel=0.16)
    P["memb"] = S("membrane", rgb(120, 190, 210), 0.0, 0.3, transmission=0.5, ior=1.1, rim=(rgb(220, 250, 255), 0.7), mottle=0.9, mottle_color=rgb(70, 130, 170), veins=0.5, vein_color=rgb(210, 250, 255),
                  bump="leather", bump_scale=0.5, bump_strength=0.15, seed=83, bevel=0.0)
    P["feather"] = S("pfeather", rgb(150, 200, 220), 0.0, 0.5, feather=(13, 5, 0.75, 0.9), dirt=0.3, wear=0.3, wear_color=rgb(255, 255, 255), tramp=[(0.0, rgb(56, 98, 140)), (0.4, rgb(126, 190, 214)), (0.8, rgb(236, 248, 252)), (1.0, rgb(255, 255, 255))],
                     mottle=0.6, mottle_color=rgb(70, 120, 160), seed=84, bevel=0.08, fine=0.4)
    P["feather2"] = S("pfeather2", rgb(236, 244, 248), 0.0, 0.5, feather=(13, 5, 0.75, 0.9), dirt=0.3, tramp=[(0.0, rgb(150, 176, 200)), (0.5, rgb(240, 248, 252)), (1.0, rgb(120, 200, 214))],
                      mottle=0.5, mottle_color=rgb(140, 170, 200), seed=85, bevel=0.08)
    P["fur"] = S("rabbit", rgb(196, 176, 150), 0.0, 0.9, feather=(15, 3, 0.8, 0.3), tramp=[(0.0, rgb(116, 92, 68)), (0.5, rgb(210, 190, 160)), (1.0, rgb(250, 244, 232))], mottle=0.8, mottle_color=rgb(130, 104, 78), seed=86, bevel=0.06, fine=0.4)
    P["claw"] = S("pclaw", rgb(60, 66, 80), 0.6, 0.35, wear=0.9, wear_color=rgb(190, 200, 220), dirt=0.8, bump="grain", bump_strength=0.2, mottle=0.6, seed=87, bevel=0.1)
    P["silver"] = S("psilver", rgb(200, 212, 224), 1.0, 0.28, wear=0.9, wear_color=rgb(255, 255, 255), dirt=0.8, scratches=0.8, bump="hammer", bump_strength=0.2, mottle=0.7, mottle_color=rgb(100, 130, 160), seed=88, bevel=0.18, ao_dist=1.8)
    P["gem"] = S("pgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=89, bevel=0.0)
    P["spark"] = S("spark", rgb(180, 240, 255), 0.0, 0.2, emit=rgb(150, 240, 255), emit_strength=1.0, emit_flicker=0.3, seed=90, bevel=0.0)
    P["string"] = S("pstring", rgb(220, 240, 250), 0.0, 0.5, bump="wrap", bump_strength=0.5, seed=91, bevel=0.0, mottle=0.6, mottle_color=rgb(130, 190, 220))
    return P


def bird_hunting(o):
    sh = dict(belly=24.0, power=1.85, curl=8.0, riser=11.0, cstart=0.74, curl_lo=7.0)
    begin(o["samples"])
    P = bird_palette()
    rr = K.rng(4404)
    L = {1: limb_pts(1, sh, 26), -1: limb_pts(-1, sh, 26)}
    wd = lambda t: 2.9 - 1.8 * t ** 0.85           # noqa: E731
    hh = lambda t: 1.9 - 1.0 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_bone%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.3), P["bone"], "limb_bone")
        noisy(ob, 0.15, 0.06, 41 + s)
        for k, t in enumerate((0.1, 0.34, 0.58, 0.82)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.15, hh(t) + 0.1, 0.3)       # noqa: E731
            band("limb_ring%d_%d" % (s, k), L[s], t, sc, 0.55, P["silver"], "limb_ring", width=0.02)
        # the wing: wrist on the limb's back, four bone fingers, scalloped phantom membrane between them
        tw = 0.2
        pw_, tnw, nrw = at(L[s], tw)
        W = pw_ + nrw * (wd(tw) + 0.8)
        tf = (0.46, 0.66, 0.84, 0.98)
        reach = (24.0, 25.0, 20.0, 12.0)
        tips = []
        for i, (t, rc) in enumerate(zip(tf, reach)):
            p, tn, nr = at(L[s], t)
            F = p + nr * (wd(t) + rc * 0.62) + V((0, 0, 0.7 * (1 if i % 2 else -1)))
            tips.append(F)
            mid = (W + F) * 0.5 + nr * 2.0 + V((0, 0, 0.8 * (1 if i % 2 else -1)))
            K.tube("limb_finger%d_%d" % (s, i), [tuple(W), tuple(mid), tuple(F)], 0.6, P["bone2"], sides=5, sub=3, part="limb_finger", taper=(0.85, 0.22))
            K.blob("limb_fknob%d_%d" % (s, i), tuple(F), (0.7, 0.7, 0.7), P["spark"], seed=i + 4 * s, lump=0.1, rings=5, segs=6, part="limb_finger")
        K.blob("limb_wrist%d" % s, tuple(W), (1.5, 1.7, 1.5), P["silver"], seed=s, lump=0.12, rings=6, segs=8, part="limb_finger")
        for i in range(len(tips) - 1):
            a, b = tips[i], tips[i + 1]
            rows = []
            for j in range(10):
                u = 0.1 + 0.9 * j / 9
                pl, pr = W + (a - W) * u, W + (b - W) * u
                if u > 0.82:                                         # the scalloped trailing edge
                    cut = (u - 0.82) / 0.18
                    mid = (pl + pr) * 0.5
                    k = 0.28 * math.sin(math.pi * cut) * (1 if j < 9 else 0.0)
                    pl, pr = pl + (mid - pl) * k * 0.0, pr + (mid - pr) * k * 0.0
                    sag = (mid - W).normalized() * (-3.6 * math.sin(math.pi * 0.5 * cut) ** 1.2)
                    pl, pr = pl + sag * 0.55, pr + sag * 0.55
                rows.append((pl, pr))
            K.strip("limb_memb%d_%d" % (s, i), rows, P["memb"], (-1.0, -0.5, 0.5, 1.0), (0.0, 1.0, 1.0, 0.0), 0.34, part="limb_memb", up=(0, 0, 1), angle=60,
                    wobble=lambda q, t, i=i: (0, 0, 1.7 * math.sin(math.pi * t) * (1 if i % 2 == 0 else -0.8) + 0.4 * K.noise3(q, 0.1, 5 + i, 2)))
        # plumage along the leading edge: pale feathers that lie over the membrane's root
        tuft("limb_plume%d" % s, W + V((0, 0, 0.5)), nrw * 0.75 + tnw * 0.6, 6, 22.0, 5.0, P["feather"], rr, "limb_plume", spread=44.0, facing=(0, 0, 1), curl=2.4, rows=8, thick=0.26)
        K.tapered("limb_plume_big%d" % s, [W, W + nrw * 3.0 + tnw * 6.0, W + nrw * 5.0 + tnw * 12.0], 0.7, 0.2, P["bone2"], sides=5, sub=1, part="limb_plume")
        # ghostly streamers from the tip
        pe, tn, nr = at(L[s], 1.0)
        for k in range(2):
            path = [pe + tn * (0.8 * j) + nr * (0.5 * j * (k + 1) * 0.4) + V((0.6 * math.sin(j * 0.8 + k), 0, 0.5 * math.cos(j * 0.7))) for j in range(9)]
            K.ribbon("limb_wisp%d_%d" % (s, k), path, lambda tt: 1.6 * (1 - 0.7 * tt) + 0.2, P["memb"], facing=(0, 0, 1), thick=0.12, part="limb_wisp", fray=0.4, rng_=rr, edge_wave=0.1)
        K.tapered("limb_nock%d" % s, [pe - tn * 4.0, pe, pe + tn * 0.6 + nr * 1.4], 1.2, 0.25, P["silver"], sides=6, sub=2, part="limb_nock")
    # riser: bone core, silver collars, the stone, a perched bird foot (three front toes, one back)
    riser("riser_core", sh, [(-14.0, 2.4, 2.6), (-11.0, 3.2, 3.0), (-7.0, 2.8, 2.8), (3.0, 2.9, 2.8), (5.0, 4.2, 3.2), (10.0, 5.0, 3.2), (13.5, 3.5, 3.0), (15.5, 2.2, 2.4)], P["bone2"])
    hand_binding(sh, P["bone2"], P["feather2"], -9.8, 2.4, 2.55, bands=(-9.2, -4.0, 1.7), band_mat=P["silver"], turns=7, cord=0.4)
    jewel(sh, P["gem"], P["silver"], rr, y=7.4, r=3.8, claws=6)
    for i, (zo, yaw, ln) in enumerate(((-2.4, -0.38, 12.5), (0.0, 0.0, 14.0), (2.4, 0.38, 12.0), (0.0, 3.14, 8.0))):
        d = V((math.cos(yaw) if i < 3 else -1.0, 0, math.sin(yaw) if i < 3 else 0))
        b0 = V((bx + (3.2 if i < 3 else -3.0) * (1 if i < 3 else 1), -11.5, zo))
        pts = [b0, b0 + d * ln * 0.35 + V((0, 0.4, 0)), b0 + d * ln * 0.7 + V((0, -0.8, 0)), b0 + d * ln + V((0, -4.0, 0))]
        K.tube("riser_toe%d" % i, [tuple(p) for p in pts], 0.9, P["bone2"], sides=6, sub=2, part="riser_toe", taper=(1.1, 0.7))
        for j in (1, 2):
            K.blob("riser_knuckle%d_%d" % (i, j), tuple(pts[j]), (1.0, 1.0, 1.0), P["bone"], seed=i * 3 + j, lump=0.15, rings=5, segs=6, part="riser_toe")
        K.tapered("riser_talon%d" % i, [pts[3], pts[3] + d * 2.2 + V((0, -2.8, 0)), pts[3] + d * 2.4 + V((0, -6.4, 0))], 0.85, 0.06, P["claw"], sides=5, sub=2, part="riser_talon")
    # the rabbit's foot charm on a silver chain
    cx = 12.5
    K.tube("riser_cord", [(bx - 2.4, -12.0, 0.0), (bx - 6.0, -15.5, 0.4), (cx + 0.8, -19.0, 0.2), (cx, -22.0, 0.0)], 0.28, P["silver"], sides=5, sub=2, part="riser_charm")
    for k, yy in enumerate((-14.0, -18.5)):
        K.blob("riser_cbead%d" % k, (cx + 0.1 + 0.4 * k, yy, 0.2), (0.8, 0.8, 0.8), P["spark"] if k else P["silver"], seed=k + 5, lump=0.05, rings=5, segs=6, part="riser_charm")
    K.bl.lathe("riser_cuff", [(1.5, -23.8), (1.9, -23.2), (1.9, -21.8), (1.5, -21.2)], P["silver"], sides=8, origin=(cx, 0, 0), part="riser_charm")
    K.blob("riser_foot_palm", (cx, -28.0, 0), (2.4, 4.8, 2.0), P["fur"], seed=3, lump=0.14, rings=7, segs=9, part="riser_charm")
    for i, (xo, ln) in enumerate(((-1.4, 4.2), (-0.45, 5.0), (0.5, 4.9), (1.4, 4.0))):
        K.blob("riser_foot_toe%d" % i, (cx + xo, -32.8 - ln * 0.5, 0.2), (0.85, ln * 0.55, 0.85), P["fur"], seed=i + 9, lump=0.12, rings=5, segs=6, part="riser_charm")
        K.tapered("riser_foot_claw%d" % i, [V((cx + xo, -34.0 - ln, 0.3)), V((cx + xo * 1.1, -35.2 - ln, 0.4)), V((cx + xo * 1.15, -36.4 - ln, 0.2))], 0.3, 0.04, P["claw"], sides=4, sub=1, part="riser_charm")
    tuft("riser_foot_fur", V((cx, -23.0, 0)), V((0, -1.0, 0)), 8, 6.5, 2.4, P["fur"], rr, "riser_charm", spread=70.0, facing=(1, 0, 0), curl=0.8, rows=5, thick=0.18)
    string(sh, P["string"], 0.36)
    arrow(sh, P["bone"], P["memb"], P["feather2"], 3.3, "bodkin", cap_mat=P["silver"])
    return sh


# ================================================================ 5. ARROW SHOWER: a blackened-iron bow with a quiver whose arrows fan up like a peacock tail, powder charges strapped to the limbs, fuses and sparks
def shower_palette():
    S = K.surface
    P = {}
    P["iron"] = S("siron", rgb(44, 42, 48), 1.0, 0.45, wear=1.0, wear_color=rgb(190, 176, 150), dirt=0.9, scratches=0.8, bump="hammer", bump_scale=0.7, bump_strength=0.24, mottle=0.9, mottle_color=rgb(80, 40, 30),
                  fine=0.5, seed=101, bevel=0.2, ao_dist=2.2)
    P["brass"] = S("sbrass", rgb(206, 156, 60), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 232, 150), dirt=0.9, dirt_color=(0.04, 0.025, 0.006, 1), scratches=0.8, bump="hammer", bump_strength=0.2, mottle=0.9, mottle_color=rgb(110, 70, 20),
                   seed=102, bevel=0.18, ao_dist=1.8)
    P["leather"] = S("sleath", rgb(96, 52, 30), 0.0, 0.6, bump="leather", bump_scale=1.3, bump_strength=0.8, groove=rgb(30, 14, 8), groove_amount=0.9, wear=0.8, wear_color=rgb(170, 108, 64), dirt=0.9,
                     mottle=0.8, mottle_color=rgb(50, 26, 14), seed=103, bevel=0.0, fine=0.5)
    P["paper"] = S("charge", rgb(176, 40, 26), 0.0, 0.7, bump="wrap", bump_scale=1.0, bump_strength=0.5, groove=rgb(70, 12, 8), groove_amount=0.8, dirt=0.9, dirt_color=(0.02, 0.008, 0.005, 1), wear=0.6, wear_color=rgb(230, 180, 120),
                   mottle=0.9, mottle_color=rgb(110, 22, 14), seed=104, bevel=0.0, fine=0.4)
    P["fuse"] = S("fuse", rgb(200, 180, 130), 0.0, 0.7, bump="wrap", bump_strength=0.7, groove=rgb(80, 60, 30), groove_amount=0.8, seed=105, bevel=0.0)
    P["spark"] = S("sspark", rgb(255, 160, 40), 0.0, 0.3, emit=rgb(255, 150, 30), emit_strength=1.6, emit_flicker=0.5, seed=106, bevel=0.0)
    P["shaft"] = S("sshaft", rgb(140, 100, 60), 0.0, 0.5, bump="grain", bump_scale=1.2, bump_strength=0.3, wear=0.6, wear_color=rgb(210, 170, 110), mottle=0.9, mottle_color=rgb(80, 50, 24), dirt=0.7, seed=107, bevel=0.1)
    P["steel"] = S("ssteel", rgb(176, 180, 190), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 255, 255), dirt=0.8, scratches=0.8, bump="brushed", bump_strength=0.12, mottle=0.5, seed=108, bevel=0.15)
    P["red"] = S("sfeath", rgb(190, 40, 30), 0.0, 0.55, feather=(11, 4, 0.7, 0.8), tramp=[(0.0, rgb(60, 14, 12)), (0.5, rgb(190, 40, 30)), (1.0, rgb(240, 120, 70))], mottle=0.6, mottle_color=rgb(80, 16, 12), seed=109, bevel=0.08)
    P["black"] = S("sfeath2", rgb(34, 30, 34), 0.0, 0.55, feather=(11, 4, 0.7, 0.8), tramp=[(0.0, rgb(20, 18, 22)), (0.6, rgb(60, 56, 64)), (1.0, rgb(170, 160, 160))], seed=110, bevel=0.08)
    P["gem"] = S("sgem", rgb(244, 160, 14), 0.1, 0.06, emit=rgb(255, 170, 30), emit_strength=0.9, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(255, 220, 80), seed=111, bevel=0.0)
    P["string"] = S("sstr", rgb(200, 170, 120), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(70, 50, 26), groove_amount=0.8, seed=112, bevel=0.0)
    return P


def arrow_shower(o):
    sh = dict(belly=23.0, power=2.0, curl=7.0, riser=12.0, cstart=0.78)
    begin(o["samples"])
    P = shower_palette()
    rr = K.rng(5505)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 3.2 - 1.9 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.3 - 1.2 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_iron%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.3), P["iron"], "limb_iron")
        noisy(ob, 0.18, 0.07, 61 + s)
        for k, t in enumerate((0.1, 0.22, 0.38, 0.5, 0.64, 0.78, 0.9)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.2, hh(t) + 0.15, 0.3)       # noqa: E731
            band("limb_hoop%d_%d" % (s, k), L[s], t, sc, 0.6, P["brass"] if k % 2 == 0 else P["iron"], "limb_hoop", width=0.016)
        # powder charges strapped on the back: red paper cylinders, brass caps, a straps pair, a fuse with a spark
        for k, t in enumerate((0.26, 0.5, 0.72)):
            p, tn, nr = at(L[s], t)
            c0 = p + nr * (wd(t) + 2.1)
            ln = 4.2 - 0.6 * k
            K.tapered("limb_charge%d_%d" % (s, k), [c0 - tn * ln, c0 + tn * ln], 1.75 - 0.2 * k, 1.75 - 0.2 * k, P["paper"], sides=8, sub=1, part="limb_charge", bulge=lambda u: 0.88 + 0.12 * math.sin(math.pi * u))
            for e in (-1, 1):
                bl.lathe("limb_cap%d_%d_%d" % (s, k, e), [(1.3, -0.3 * e), (1.95 - 0.2 * k, 0.1 * e), (1.95 - 0.2 * k, 0.9 * e), (1.0, 1.15 * e)], P["brass"], sides=8, origin=(c0.x, c0.y + e * ln, c0.z), part="limb_cap")
            K.tube("limb_fuse%d_%d" % (s, k), [tuple(c0 + tn * (ln + 1.0)), tuple(c0 + tn * (ln + 2.4) + nr * 1.5 + V((0, 0, 0.8))), tuple(c0 + tn * (ln + 3.6) + nr * 3.2 + V((0, 0, 0.2)))], 0.22, P["fuse"], sides=4, sub=2, part="limb_fuse")
            K.blob("limb_spark%d_%d" % (s, k), tuple(c0 + tn * (ln + 3.8) + nr * 3.5 + V((0, 0, 0.2))), (0.55, 0.55, 0.55), P["spark"], seed=k, lump=0.2, rings=4, segs=6, part="limb_fuse")
            for e in (-1, 1):                                         # leather straps over the charge to the limb
                pp, tt, nn = at(L[s], t + e * 0.035)
                K.bar("limb_strap%d_%d_%d" % (s, k, e), tuple(pp + nn * (wd(t) + 3.0) + V((0, 0, 0))), tuple(pp + nn * (wd(t) - 0.2) + V((0, 0, hh(t) + 0.3))), 0.55, 0.3, P["leather"], part="limb_strap", bevel_w=0.0)
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_nock%d" % s, [pe - tn * 4.0, pe, pe + tn * 0.7 + nr * 1.6], 1.3, 0.3, P["brass"], sides=6, sub=2, part="limb_nock")
    riser("riser_core", sh, [(-14.0, 2.5, 2.6), (-11.0, 3.4, 3.1), (-7.0, 3.0, 2.9), (3.0, 3.0, 2.9), (5.0, 4.4, 3.3), (10.0, 5.2, 3.3), (14.0, 3.6, 3.1), (16.5, 2.3, 2.5)], P["iron"])
    hand_binding(sh, P["leather"], P["fuse"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["brass"], turns=8, cord=0.34)
    for s in (1, -1):
        z0, z1 = (2.8, 3.8) if s > 0 else (-3.8, -2.8)
        K.stepped_plate("riser_plate%d" % s, [(bx - 3.6, 13.0), (bx - 4.8, 16.0), (bx, 18.5), (bx + 4.8, 16.0), (bx + 3.6, 13.0)], z0, P["brass"], steps=2, step_h=0.5, inset=0.7, part="riser_plate", up=s)
        K.rivets("riser_rivets%d" % s, [(V((bx + x, y, z1 + 0.2 * s)), (0, 0, s)) for x, y in ((-3.0, 14.2), (3.0, 14.2), (0.0, 17.0))], P["steel"], r=0.55, detail=False, sides=5, height=0.8)
    jewel(sh, P["gem"], P["brass"], rr, y=7.6, r=3.9, claws=6)
    # the quiver: a leather cone on the back, riveted straps to the riser, the arrows of the shower fanning out of its mouth
    qx = bx + 8.5
    q = bl.lathe("riser_quiver", [(3.0, -6.0), (3.7, -2.0), (4.2, 8.0), (4.9, 17.0), (5.2, 19.0), (4.5, 19.6), (4.5, 17.0)], P["leather"], sides=12, origin=(qx, 0, 0), caps=(True, False), part="riser_quiver")
    K._finish_obj(q, "riser_quiver", True, 50.0)
    for k, y in enumerate((-4.0, 6.0, 18.0)):
        r0 = 3.4 + 0.07 * (y + 6)
        bl.lathe("riser_qband%d" % k, [(r0 + 0.1, y - 0.9), (r0 + 0.7, y - 0.5), (r0 + 0.7, y + 0.5), (r0 + 0.1, y + 0.9)], P["brass"], sides=12, origin=(qx, 0, 0), part="riser_quiver")
    for k, y in enumerate((0.0, 12.0)):
        K.bar("riser_qstrap%d" % k, (bx + 3.0, y, 0.0), (qx - 3.0, y + 1.0, 0.0), 0.9, 1.0, P["leather"], part="riser_quiver", bevel_w=0.0)
    for i in range(9):
        a = math.radians(-52 + 104 * i / 8 + rr.uniform(-4, 4))
        zz = rr.uniform(-1.6, 1.6)
        d = V((math.sin(a) * 0.78, math.cos(a), zz * 0.05 + 0.18 * math.sin(i * 1.7))).normalized()
        ln = 38.0 + 9.0 * math.cos(a * 1.3) + rr.uniform(-2, 2)
        b0 = V((qx + 1.1 * math.sin(a), 19.0, zz))
        K.tube("riser_ashaft%d" % i, [tuple(b0 - d * 4.0), tuple(b0 + d * ln)], 0.5, P["shaft"], sides=5, sub=1, part="riser_arrows")
        tip = b0 + d * ln
        hp = [P["steel"], P["brass"], P["iron"]][i % 3]
        K.tapered("riser_ahead%d" % i, [tip, tip + d * 3.4, tip + d * 7.0], 1.35 if i % 3 != 2 else 0.8, 0.06, hp, sides=4, sub=1, part="riser_arrows", bulge=(lambda u: 1.0 - 0.4 * u) if i % 3 == 0 else None)
        for j in range(2):                                           # fletching peeking out of the mouth
            side = V((0, 0, 1)) * (1 if j else -1)
            K.feather("riser_afl%d_%d" % (i, j), b0 + d * 1.5 + side * 0.4, d, 9.0, 2.4, P["red"] if i % 2 else P["black"], facing=(side.x, side.y, side.z) if False else (1, 0, 0), curl=0.5, rows=4, serrate=0.1, thick=0.24,
                      part="riser_arrows", seed=i * 2 + j, compact=False, profile=lambda u: min(1.0, u / 0.2 + 0.3) * (1 - u ** 3))
    # a tied powder pouch on the front of the grip, spilling a spark
    K.blob("riser_pouch", (bx - 3.8, -2.0, 0.0), (2.6, 3.4, 2.4), P["leather"], seed=5, lump=0.14, rings=7, segs=9, part="riser_pouch")
    K.tube("riser_pouch_cord", [(bx - 3.8, 1.0, 0.0), (bx - 3.0, 3.5, 1.6), (bx - 1.5, 4.6, 2.6)], 0.28, P["fuse"], sides=4, sub=2, part="riser_pouch")
    string(sh, P["string"], 0.4)
    arrow(sh, P["shaft"], P["steel"], P["red"], 3.3, "broad", cap_mat=P["brass"], glow_mat=None)
    return sh


# ================================================================ 6. METEOR BOW: star-iron limbs set with glowstone and amethyst, a burning meteor on the upper tip with a comet tail, an orbit of pearls round the stone
def meteor_palette():
    S = K.surface
    P = {}
    P["iron"] = S("miron", rgb(40, 34, 64), 0.9, 0.38, wear=1.0, wear_color=rgb(190, 170, 240), wear_metal=1.0, dirt=0.9, dirt_color=(0.004, 0.002, 0.012, 1), scratches=0.8, bump="hammer", bump_scale=0.7, bump_strength=0.24,
                  mottle=1.0, mottle_color=rgb(70, 40, 110), fine=0.6, seed=121, bevel=0.2, ao_dist=2.2)
    P["rock"] = S("rock", rgb(78, 58, 56), 0.2, 0.8, wear=0.7, wear_color=rgb(170, 130, 110), dirt=1.0, dirt_color=(0.01, 0.006, 0.006, 1), bump="grain", bump_scale=0.5, bump_strength=0.6, mottle=1.0, mottle_color=rgb(40, 28, 30),
                  veins=0.5, vein_color=rgb(255, 110, 20), vein_strength=0.55, fine=0.8, seed=122, bevel=0.1, ao_dist=3.0)
    P["glowstone"] = S("glowstone", rgb(250, 190, 70), 0.0, 0.3, emit=rgb(255, 200, 80), emit_strength=0.9, emit_flicker=0.6, mottle=0.9, mottle_color=rgb(255, 232, 140), bump="grain", bump_strength=0.2, seed=123, bevel=0.0)
    P["ame"] = S("amethyst", rgb(150, 70, 220), 0.1, 0.1, transmission=0.4, ior=1.4, rim=(rgb(230, 180, 255), 0.6), mottle=0.9, mottle_color=rgb(90, 30, 160), seed=124, bevel=0.0)
    P["pearl"] = S("pearl", rgb(240, 236, 232), 0.5, 0.18, mottle=0.5, mottle_color=rgb(200, 190, 230), rim=(rgb(255, 230, 250), 0.4), seed=125, bevel=0.0)
    P["gold"] = S("mgold", rgb(222, 170, 70), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 238, 170), dirt=0.9, dirt_color=(0.04, 0.025, 0.006, 1), scratches=0.7, bump="hammer", bump_strength=0.2, mottle=0.9, mottle_color=rgb(120, 76, 24), seed=126, bevel=0.18, ao_dist=1.8)
    P["flame"] = S("tail", rgb(255, 130, 40), 0.0, 0.5, feather=(9, 3, 0.4, 0.5), tramp=[(0.0, rgb(255, 190, 80)), (0.4, rgb(230, 90, 20)), (1.0, rgb(90, 30, 130))], emit=rgb(255, 110, 30), emit_strength=0.3, temit=[(0.0, rgb(255, 200, 110)), (1.0, rgb(150, 50, 200))],
                   emit_flicker=0.8, seed=127, bevel=0.06)
    P["star"] = S("mstar", rgb(255, 230, 150), 0.0, 0.3, emit=rgb(255, 230, 150), emit_strength=1.0, seed=128, bevel=0.0)
    P["gem"] = S("mgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=129, bevel=0.0)
    P["string"] = S("mstr", rgb(230, 214, 255), 0.0, 0.4, emit=rgb(190, 150, 255), emit_strength=0.2, bump="wrap", bump_strength=0.4, seed=130, bevel=0.0)
    return P


def meteor_bow(o):
    sh = dict(belly=22.0, power=1.9, curl=12.0, riser=12.0, cstart=0.74, curl_lo=5.0, power_lo=1.95)
    begin(o["samples"])
    P = meteor_palette()
    rr = K.rng(6606)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 4.2 - 2.4 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.9 - 1.5 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_iron%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.3), P["iron"], "limb_iron")
        noisy(ob, 0.22, 0.07, 71 + s)
        for k, t in enumerate((0.12, 0.3, 0.48, 0.66)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.2, hh(t) + 0.15, 0.3)       # noqa: E731
            band("limb_hoop%d_%d" % (s, k), L[s], t, sc, 0.55, P["gold"], "limb_hoop", width=0.014)
        # glowstone nuggets embedded in the faces and amethyst clusters on the back
        for k, t in enumerate((0.25, 0.5, 0.74)):
            p, tn, nr = at(L[s], t)
            K.blob("limb_nug%d_%d" % (s, k), tuple(p + V((0, 0, (hh(t) + 0.2) * (1 if k % 2 else -1)))), (1.3, 2.0 - 0.2 * k, 0.9), P["glowstone"], seed=k + 3 * s, lump=0.3, rings=5, segs=6, part="limb_nug")
        for k, t in enumerate((0.3, 0.56, 0.8)):
            p, tn, nr = at(L[s], t)
            for j in range(4 if k < 2 else 2):
                a = rr.uniform(-0.5, 0.9)
                d = (nr * math.cos(a) + tn * math.sin(a) * 0.9 + V((0, 0, rr.uniform(-0.5, 0.5)))).normalized()
                K.shard("limb_ame%d_%d_%d" % (s, k, j), p + nr * (wd(t) + 0.4) + V((0, 0, rr.uniform(-0.8, 0.8))), 1.5 - 0.15 * k, P["ame"], rr, part="limb_ame", sides=6, stretch=3.3 - 0.4 * j, dir_=d)
    # upper tip: the burning meteor and its comet tail trailing back along the limb
    pe, tn, nr = at(L[1], 1.0)
    mc = V((pe.x + 2.0, pe.y - 3.2, 0.0))
    rock = K.blob("limb_meteor", tuple(mc), (6.6, 6.2, 5.8), P["rock"], seed=9, lump=0.45, lump_scale=0.16, rings=11, segs=14, part="limb_meteor")
    K.displace(rock, 1.3, 0.22, 3, 3)
    for k in range(5):
        a = rr.uniform(0, TAU)
        c = mc + V((math.cos(a) * 5.5, rr.uniform(-4, 4), math.sin(a) * 4.6))
        K.shard("limb_chip%d" % k, c, 1.2, P["rock"], rr, part="limb_meteor", sides=5, stretch=1.6, dir_=c - mc)
    for k in range(5):
        d = V((0.9 + 0.1 * k, -0.15 - 0.5 * k, 0.3 * (k - 1.5))).normalized()
        K.feather("limb_tail%d" % k, mc + V((1.0, -3.0, 0.8 * (k - 1.5))), V((0.5 + 0.12 * k, -1.0, 0.0 + 0.15 * (k - 2.0))), 36.0 - 4 * k, 8.0 - 1.2 * k, P["flame"], facing=(0, 0, 1), curl=3.0 * (1 if k % 2 else -1), rows=7, serrate=0.12,
                  thick=0.3, part="limb_tail", seed=k + 50, compact=False, droop=1.5, ridge=2.0, profile=lambda u: min(1.0, u / 0.15) * (1 - u) ** 0.75)
    for k in range(8):
        sp = mc + V((rr.uniform(1.0, 9.0), rr.uniform(-34.0, -6.0), rr.uniform(-4, 4)))
        K.shard("limb_cinder%d" % k, sp, rr.uniform(0.6, 1.0), P["flame"], rr, part="limb_tail", sides=3, stretch=3.0, dir_=V((0.4, -1.0, 0.1)))
    K.tapered("limb_nockU", [pe - tn * 5.0, pe + V((0, -1.0, 0)), pe + nr * 0.5 + V((0, -1.5, 0))], 1.3, 0.8, P["gold"], sides=6, sub=1, part="limb_nock")
    # lower tip: a crystal crown of amethyst
    pe2, tn2, nr2 = at(L[-1], 1.0)
    K.tapered("limb_nockL", [pe2 - tn2 * 4.0, pe2, pe2 + tn2 * 0.6 + nr2 * 1.5], 1.3, 0.3, P["gold"], sides=6, sub=2, part="limb_nock")
    for k in range(6):
        d = (nr2 * (0.5 + 0.3 * k / 5) + tn2 * (0.3 + 0.12 * k) + V((0, 0, (k - 2.5) * 0.18))).normalized()
        K.shard("limb_crown%d" % k, pe2 + tn2 * -2.5 + nr2 * (0.8 + 0.6 * (k % 2)) + V((0, 0, (k - 2.5) * 0.5)), 1.5 - 0.12 * k, P["ame"], rr, part="limb_crown", sides=6, stretch=3.8, dir_=d)
    # riser, the stone, the orbit of pearls on two tilted rings, gold stars
    riser("riser_core", sh, [(-14.0, 2.5, 2.6), (-11.0, 3.4, 3.1), (-7.0, 3.0, 2.9), (3.0, 3.0, 2.9), (5.0, 4.4, 3.3), (10.0, 5.2, 3.3), (14.0, 3.6, 3.1), (16.5, 2.3, 2.5)], P["iron"])
    hand_binding(sh, P["iron"], P["gold"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["gold"], turns=7, cord=0.3)
    jewel(sh, P["gem"], P["gold"], rr, y=7.6, r=3.8, claws=6)
    cy = 7.6
    for ri, tilt in enumerate((62.0, -62.0)):
        ca, sa = math.cos(math.radians(tilt)), math.sin(math.radians(tilt))
        pts = []
        for i in range(17):
            a = TAU * i / 16
            x, y, z = 9.6 * math.cos(a), 9.6 * math.sin(a), 0.0
            pts.append(V((bx + x * ca + z * sa, cy + y, -x * sa + z * ca)))
        K.tube("riser_orbit%d" % ri, [tuple(p) for p in pts], 0.38, P["gold"], sides=5, sub=1, part="riser_orbit")
        for i in range(6):
            a = TAU * (i + 0.5 * ri) / 6
            x, y = 9.6 * math.cos(a), 9.6 * math.sin(a)
            K.blob("riser_pearl%d_%d" % (ri, i), (bx + x * ca, cy + y, -x * sa), (1.25, 1.25, 1.25), P["pearl"], seed=i + ri * 9, lump=0.04, rings=4, segs=6, part="riser_pearls")
    for k, (xx, yy, zz, sz) in enumerate(((bx + 14.0, 30.0, 0.5, 2.3), (bx + 12.0, -26.0, -0.5, 2.0), (bx + 9.0, 46.0, 0.0, 2.0))):
        pts = []
        for i in range(8):
            a = TAU * i / 8 + math.pi / 8
            rad = sz if i % 2 == 0 else sz * 0.36
            pts.append((xx + rad * math.cos(a), yy + rad * math.sin(a)))
        K.plate("riser_star%d" % k, pts, zz - 0.35, zz + 0.35, P["star"], part="riser_stars", bevel_w=0.08)
    for k in range(7):                                                       # a geode of amethyst bursting from the back of the riser
        a = math.radians(-40 + 14 * k + rr.uniform(-5, 5))
        d = V((math.cos(a) * 0.9, math.sin(a) * 0.6 - 0.1, rr.uniform(-0.45, 0.45))).normalized()
        K.shard("riser_geode%d" % k, V((bx + 4.5, -4.0 + 1.6 * k - 4.0, rr.uniform(-1.4, 1.4))), 2.0 + 0.5 * math.sin(k * 1.3), P["ame"], rr, part="riser_geode", sides=6, stretch=3.6, dir_=d)
    string(sh, P["string"], 0.4)
    arrow(sh, P["iron"], P["gold"], P["flame"], 3.3, "broad", cap_mat=P["ame"], glow_mat=None)
    return sh


# ================================================================ 7. FROZEN RAIN: glacier-ice limbs round a prismarine core, jagged crystal growth, hanging icicles, a snowflake of ice, falling shards
def frozen_palette():
    S = K.surface
    P = {}
    P["ice"] = S("ice", rgb(110, 190, 245), 0.0, 0.08, transmission=0.6, ior=1.3, rim=(rgb(235, 252, 255), 0.8), mottle=0.8, mottle_color=rgb(90, 170, 225), bump="grain", bump_scale=0.6, bump_strength=0.25,
                 wear=0.5, wear_color=rgb(255, 255, 255), dirt=0.3, dirt_color=(0.02, 0.05, 0.08, 1), seed=141, bevel=0.1, ao_dist=2.0)
    P["ice2"] = S("ice_deep", rgb(70, 160, 230), 0.0, 0.1, transmission=0.7, ior=1.3, rim=(rgb(210, 240, 255), 0.7), mottle=0.9, mottle_color=rgb(30, 90, 190), bump="grain", bump_scale=0.5, bump_strength=0.2, seed=142, bevel=0.06)
    P["core"] = S("prismarine", rgb(60, 180, 170), 0.2, 0.3, wear=0.8, wear_color=rgb(180, 255, 240), dirt=0.7, dirt_color=(0.0, 0.03, 0.03, 1), bump="scales", bump_scale=0.7, bump_strength=0.3, mottle=1.0, mottle_color=rgb(30, 110, 120),
                  seed=143, bevel=0.15, ao_dist=2.0)
    P["frost"] = S("frost", rgb(236, 248, 255), 0.0, 0.7, bump="grain", bump_scale=0.8, bump_strength=0.4, mottle=0.7, mottle_color=rgb(170, 210, 240), wear=0.4, wear_color=rgb(255, 255, 255), seed=144, bevel=0.1)
    P["crystal"] = S("mcrystal", rgb(120, 230, 255), 0.0, 0.05, transmission=0.5, ior=1.45, rim=(rgb(240, 255, 255), 0.8), emit=rgb(100, 220, 255), emit_strength=0.5, mottle=0.8, mottle_color=rgb(60, 170, 230), seed=145, bevel=0.0)
    P["silver"] = S("fsilver", rgb(196, 214, 232), 1.0, 0.25, wear=0.9, wear_color=rgb(255, 255, 255), dirt=0.8, dirt_color=(0.0, 0.01, 0.03, 1), scratches=0.8, bump="hammer", bump_strength=0.2, mottle=0.7, mottle_color=rgb(90, 130, 190), seed=146, bevel=0.18, ao_dist=1.8)
    P["gem"] = S("fgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=147, bevel=0.0)
    P["string"] = S("fstr", rgb(214, 240, 255), 0.0, 0.3, emit=rgb(150, 220, 255), emit_strength=0.4, bump="wrap", bump_strength=0.4, seed=148, bevel=0.0)
    return P


def frozen_rain(o):
    sh = dict(belly=25.0, power=1.75, curl=9.0, riser=11.0, cstart=0.72, curl_lo=8.0)
    begin(o["samples"])
    P = frozen_palette()
    rr = K.rng(7707)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 4.0 - 2.5 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.8 - 1.5 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_ice%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.35, 0.0, 0.95), P["ice"], "limb_ice")
        noisy(ob, 0.5, 0.09, 81 + s)
        stave("limb_core%d" % s, L[s], lambda t: rsec(wd(t) * 0.36, hh(t) * 0.5, 0.4, 1, dx=0.3), P["core"], "limb_core")
        for k, t in enumerate((0.16, 0.4, 0.64, 0.86)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.25, hh(t) + 0.2, 0.35, 0.0, 0.95)       # noqa: E731
            band("limb_frost%d_%d" % (s, k), L[s], t, sc, 0.7, P["frost"], "limb_frost", width=0.02)
        # jagged crystal growth on the back and the face edges
        for k, t in enumerate((0.2, 0.36, 0.52, 0.68, 0.84)):
            p, tn, nr = at(L[s], t)
            for j in range(2):
                a = rr.uniform(-0.4, 0.9)
                d = (nr * math.cos(a) * (1 if j == 0 else 0.7) + tn * math.sin(a) + V((0, 0, rr.uniform(-0.7, 0.7) * (1 if j else 0.3)))).normalized()
                K.shard("limb_cryst%d_%d_%d" % (s, k, j), p + nr * (wd(t) * 0.6) + V((0, 0, (hh(t) * 0.6) * (1 if j else -1))), (3.0 - 1.4 * t) * rr.uniform(0.85, 1.3), P["crystal"] if (k + j) % 2 else P["ice2"], rr,
                        part="limb_cryst", sides=6, stretch=3.2, dir_=d)
        # icicles hanging below the limb (gravity is -y)
        for k, t in enumerate((0.22, 0.38, 0.52, 0.66, 0.8)):
            p, tn, nr = at(L[s], t)
            base = p - nr * (wd(t) * 0.55) + V((0, 0, hh(t) * 0.7 * (1 if k % 2 else -1)))
            ln = (22.0 - 10.0 * t) * rr.uniform(0.8, 1.2)
            K.tapered("limb_icicle%d_%d" % (s, k), [base, base + V((0.3, -ln * 0.5, 0.1)), base + V((0.5, -ln, 0.0))], 1.55 - 0.5 * t, 0.05, P["ice2"], sides=5, sub=1, part="limb_icicle")
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_nock%d" % s, [pe - tn * 5.0, pe, pe + tn * 0.9 + nr * 2.4, pe + tn * 0.2 + nr * 5.4], 1.5, 0.2, P["ice"], sides=6, sub=3, part="limb_nock")
    # riser: silver and prismarine, the stone in an ice setting, a snowflake of six arms
    riser("riser_core", sh, [(-14.0, 2.5, 2.7), (-11.0, 3.4, 3.2), (-7.0, 3.0, 3.0), (3.0, 3.0, 3.0), (5.0, 4.4, 3.4), (10.0, 5.2, 3.4), (14.0, 3.6, 3.2), (16.5, 2.3, 2.6)], P["core"])
    hand_binding(sh, P["core"], P["silver"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["silver"], turns=7, cord=0.3)
    jewel(sh, P["gem"], P["silver"], rr, y=7.6, r=3.8, claws=6)
    for s in (1, -1):
        z0 = 2.7 if s > 0 else -3.5
        K.stepped_plate("riser_hex%d" % s, [(bx + 4.2 * math.cos(TAU * i / 6 + 0.52), 19.5 + 4.2 * math.sin(TAU * i / 6 + 0.52)) for i in range(6)], z0 if s > 0 else -2.7, P["silver"], steps=3, step_h=0.5, inset=0.8, part="riser_hex", up=s)
    fc = V((bx + 3.0, 23.0, 0.0))
    for a_i in range(6):
        a = TAU * a_i / 6 + 0.52
        d = V((math.cos(a), math.sin(a), 0))
        sd = V((-d.y, d.x, 0))
        Rr = 15.5 + rr.uniform(-0.8, 0.8)
        K.tapered("riser_arm%d" % a_i, [fc + d * 3.0, fc + d * Rr * 0.5, fc + d * Rr], 1.35, 0.3, P["ice"], sides=4, sub=1, part="riser_flake")
        for f, ln in ((0.42, 7.0), (0.72, 4.8)):
            for e in (-1, 1):
                b0 = fc + d * Rr * f
                K.tapered("riser_br%d_%d_%d" % (a_i, int(f * 100), e), [b0, b0 + (d * 0.6 + sd * e * 0.8) * ln], 0.85, 0.14, P["crystal"], sides=4, sub=1, part="riser_flake")
    for k in range(8):                                                       # a burst of glacier crystals from the back of the riser
        a = math.radians(-45 + 13 * k + rr.uniform(-4, 4))
        d = V((math.cos(a) * 0.9, math.sin(a) * 0.7 - 0.05, rr.uniform(-0.45, 0.45))).normalized()
        K.shard("riser_burst%d" % k, V((bx + 4.0, -2.0 + 1.5 * k - 6.0, rr.uniform(-1.2, 1.2))), 2.4 + 0.5 * math.sin(k * 1.3), P["crystal"] if k % 2 else P["ice2"], rr, part="riser_burst", sides=6, stretch=3.8, dir_=d)
    # falling ice: a bounded cloud of shards falling on the target side
    K.shard_cloud("rain", (bx + 10.0, 0.0, 0.0), (11.0, 52.0, 6.0), 14, (0.55, 1.0), P["crystal"], rr, avoid=lambda p: abs(p.y) < 28 and abs(p.x - bx) < 5.5, part="riser_rain", flow=lambda p: (0.1, -1.0, 0.05), stretch=3.4, max_count=14, sides=4)
    string(sh, P["string"], 0.4)
    arrow(sh, P["ice"], P["crystal"], P["frost"], 3.3, "bodkin", cap_mat=P["silver"], glow_mat=None)
    return sh


# ================================================================ 8. EAGLE PIERCER: netherite limbs edged in gold with diamonds, two swept eagle wings of layered plumage along the limbs, an eagle's head holding the stone
def eagle_palette():
    S = K.surface
    P = {}
    P["neth"] = S("netherite", rgb(52, 42, 46), 1.0, 0.36, wear=1.0, wear_color=rgb(190, 150, 130), dirt=0.9, scratches=0.8, bump="hammer", bump_scale=0.7, bump_strength=0.24, mottle=0.9, mottle_color=rgb(90, 56, 50), fine=0.5, seed=161,
                  bevel=0.2, ao_dist=2.2)
    P["gold"] = S("egold", rgb(226, 172, 66), 1.0, 0.28, wear=0.9, wear_color=rgb(255, 238, 170), dirt=0.9, dirt_color=(0.04, 0.025, 0.006, 1), scratches=0.7, bump="hammer", bump_strength=0.2, mottle=0.9, mottle_color=rgb(120, 76, 24), seed=162, bevel=0.18, ao_dist=1.8)
    P["diamond"] = S("diamond", rgb(110, 238, 240), 0.1, 0.04, transmission=0.4, ior=1.5, rim=(rgb(240, 255, 255), 0.8), emit=rgb(80, 220, 235), emit_strength=0.45, mottle=0.8, mottle_color=rgb(40, 170, 200), seed=163, bevel=0.0)
    P["wing"] = S("ewing", rgb(120, 82, 50), 0.0, 0.55, wear=0.5, wear_color=rgb(240, 214, 160), feather=(13, 5, 0.75, 0.9), dirt=0.4, tramp=[(0.0, rgb(52, 32, 20)), (0.45, rgb(112, 74, 44)), (0.8, rgb(206, 170, 112)), (1.0, rgb(250, 236, 200))],
                  mottle=0.7, mottle_color=rgb(60, 36, 22), seed=164, bevel=0.08, fine=0.4)
    P["wing2"] = S("ewing2", rgb(236, 226, 206), 0.0, 0.55, feather=(13, 5, 0.75, 0.9), dirt=0.3, tramp=[(0.0, rgb(120, 86, 56)), (0.5, rgb(240, 232, 214)), (1.0, rgb(255, 250, 240))], mottle=0.5, mottle_color=rgb(150, 110, 70), seed=165, bevel=0.08)
    P["wing3"] = S("ewing3", rgb(190, 140, 64), 1.0, 0.3, wear=0.8, wear_color=rgb(255, 232, 160), feather=(13, 5, 0.75, 0.9), dirt=0.5, tramp=[(0.0, rgb(90, 56, 24)), (0.6, rgb(200, 150, 66)), (1.0, rgb(255, 232, 150))], mottle=0.6, mottle_color=rgb(100, 60, 20), seed=166, bevel=0.1)
    P["beak"] = S("beak", rgb(236, 190, 70), 0.2, 0.35, wear=0.7, wear_color=rgb(255, 240, 180), dirt=0.8, bump="grain", bump_strength=0.2, mottle=0.9, mottle_color=rgb(150, 100, 30), seed=167, bevel=0.1)
    P["head"] = S("ehead", rgb(240, 232, 214), 0.0, 0.6, feather=(15, 3, 0.8, 0.4), dirt=0.4, tramp=[(0.0, rgb(130, 96, 60)), (0.5, rgb(240, 232, 214)), (1.0, rgb(255, 255, 250))], mottle=0.6, mottle_color=rgb(150, 110, 70), seed=168, bevel=0.06, fine=0.4)
    P["eye"] = S("eeye", rgb(255, 196, 30), 0.1, 0.06, emit=rgb(255, 200, 40), emit_strength=0.8, seed=169, bevel=0.0)
    P["gem"] = S("egem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=170, bevel=0.0)
    P["string"] = S("estr", rgb(238, 226, 190), 0.0, 0.5, bump="wrap", bump_strength=0.6, seed=171, bevel=0.0, mottle=0.6, mottle_color=rgb(160, 130, 80))
    return P


def eagle_piercer(o):
    sh = dict(belly=22.0, power=2.1, curl=5.0, riser=12.0, cstart=0.82)
    begin(o["samples"])
    P = eagle_palette()
    rr = K.rng(8808)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 3.2 - 1.9 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.4 - 1.3 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_neth%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.3), P["neth"], "limb_neth")
        noisy(ob, 0.14, 0.07, 91 + s)
        stave("limb_edge%d" % s, L[s], lambda t: [(x - wd(t) * 0.96, z * 0.5) for x, z in lsec(0.42, 0.9, 0.3)], P["gold"], "limb_edge")      # a gold ridge on the front edge
        for k, t in enumerate((0.18, 0.46, 0.76)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.2, hh(t) + 0.15, 0.3)       # noqa: E731
            band("limb_hoop%d_%d" % (s, k), L[s], t, sc, 0.55, P["gold"], "limb_hoop", width=0.014)
            p, tn, nr = at(L[s], t + 0.1)
            for z in (1, -1):
                g = K.bl.gem("limb_dia%d_%d_%d" % (s, k, z), 1.35 - 0.3 * t, P["diamond"], center=(p.x, p.y, (hh(t + 0.1) + 0.25) * z), depth=0.2, crown=0.3, sides=5, axis="z", part="limb_dia")
                K._finish_obj(g, "limb_dia", True, 25.0)
        # the wing: layered primaries swept along the limb, coverts over them (every one a thick cambered feather)
        for i in range(8):
            t = 0.05 + 0.062 * i
            p, tn, nr = at(L[s], t)
            a = math.radians(11 + 3.4 * i + rr.uniform(-2, 2))
            d = (tn * math.cos(a) + nr * math.sin(a)).normalized()
            ln = (38.0 - 2.6 * i) * rr.uniform(0.92, 1.06)
            K.feather("limb_prim%d_%d" % (s, i), p + nr * (wd(t) + 0.5) + V((0, 0, 0.5 * (1 if i % 2 else -1) + 0.15 * i * 0)), d, ln, 6.0 - 0.3 * i, P["wing"] if i % 3 != 1 else P["wing2"], facing=(0, 0, 1), curl=3.2 * (1 if i % 2 else -1),
                      rows=8, serrate=0.16, thick=0.32, part="limb_wing", seed=rr.randint(0, 999), compact=False, droop=2.0, ridge=2.1, bend=nr * (2.0 + 0.3 * i))
        for i in range(4):
            t = 0.04 + 0.09 * i
            p, tn, nr = at(L[s], t)
            a = math.radians(6 + 4.0 * i)
            d = (tn * math.cos(a) + nr * math.sin(a)).normalized()
            K.feather("limb_cov%d_%d" % (s, i), p + nr * (wd(t) + 0.9) + V((0, 0, 1.5 * (1 if i % 2 else -1))), d, 15.0 - 1.2 * i, 5.0, P["wing3"] if i % 2 == 0 else P["wing2"], facing=(0, 0, 1), curl=2.0, rows=7, serrate=0.12,
                      thick=0.3, part="limb_wing", seed=rr.randint(0, 999), compact=False, droop=1.2, ridge=2.0)
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_nock%d" % s, [pe - tn * 5.0, pe, pe + tn * 0.7 + nr * 1.6], 1.3, 0.28, P["gold"], sides=6, sub=2, part="limb_nock")
    # riser: dark plates with a gold rim, an eagle's head holding the stone
    riser("riser_core", sh, [(-14.0, 2.5, 2.7), (-11.0, 3.4, 3.2), (-7.0, 3.0, 3.0), (3.0, 3.0, 3.0), (5.0, 4.4, 3.4), (10.0, 5.2, 3.4), (14.0, 3.6, 3.2), (16.5, 2.3, 2.6)], P["neth"])
    hand_binding(sh, P["neth"], P["gold"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["gold"], turns=7, cord=0.3)
    jewel(sh, P["gem"], P["gold"], rr, y=7.6, r=3.8, claws=6)
    for s in (1, -1):
        z0 = 2.8 if s > 0 else -3.8
        K.stepped_plate("riser_plate%d" % s, [(bx - 3.4, -3.0), (bx - 4.0, -9.0), (bx, -11.5), (bx + 4.0, -9.0), (bx + 3.4, -3.0)], z0 if s > 0 else -2.8, P["gold"], steps=2, step_h=0.5, inset=0.7, part="riser_plate", up=s)
        K.rivets("riser_rivets%d" % s, [(V((bx + x, y, 3.9 * s + 0.2 * s)), (0, 0, s)) for x, y in ((-2.2, -4.6), (2.2, -4.6), (0.0, -9.0))], P["gold"], r=0.55, detail=False, sides=5, height=0.8)
    hx, hy = bx + 7.5, 15.0
    K.blob("riser_skull", (hx, hy, 0), (4.0, 3.7, 3.1), P["head"], seed=4, lump=0.1, rings=7, segs=9, part="riser_head")
    K.tube("riser_neck", [(bx + 0.5, 11.0, 0.0), (bx + 3.0, 12.4, 0.0), (hx - 1.5, hy - 1.0, 0.0)], 2.0, P["head"], sides=8, sub=3, part="riser_head", taper=(2.7, 2.2))
    K.tapered("riser_beakU", [V((hx + 2.4, hy + 0.8, 0)), V((hx + 6.2, hy + 0.3, 0)), V((hx + 9.2, hy - 1.8, 0)), V((hx + 9.9, hy - 5.0, 0))], 2.1, 0.12, P["beak"], sides=6, sub=3, part="riser_head")
    K.tapered("riser_beakL", [V((hx + 2.2, hy - 1.2, 0)), V((hx + 5.8, hy - 1.7, 0)), V((hx + 7.6, hy - 3.3, 0))], 1.35, 0.15, P["beak"], sides=6, sub=2, part="riser_head")
    for z in (1, -1):
        K.blob("riser_eye%d" % z, (hx + 1.6, hy + 1.2, 2.5 * z), (0.85, 0.85, 0.7), P["eye"], seed=z + 3, lump=0.03, rings=5, segs=6, part="riser_head")
        K.tapered("riser_brow%d" % z, [V((hx - 0.6, hy + 2.2, 2.1 * z)), V((hx + 1.6, hy + 2.8, 2.6 * z)), V((hx + 3.4, hy + 2.0, 2.2 * z))], 0.6, 0.2, P["beak"], sides=4, sub=1, part="riser_head")
    for k in range(4):                                                       # head and nape feathers sweeping back
        a = math.radians(150 + 16 * k)
        K.feather("riser_crest%d" % k, V((hx - 1.0, hy + 1.0, (k - 2) * 0.7)), V((math.cos(a), math.sin(a), (k - 2) * 0.08)), 8.5 - 0.8 * k, 2.6, P["head"], facing=(0, 0, 1), curl=1.2, rows=5, serrate=0.14,
                  thick=0.22, part="riser_head", seed=k + 120, compact=False, droop=0.6, ridge=1.8)
    tuft("riser_ruff", V((bx + 2.4, 11.4, 0)), V((-0.5, -0.8, 0)), 5, 8.0, 3.2, P["head"], rr, "riser_head", spread=60.0, facing=(0, 0, 1), curl=1.4, rows=5, thick=0.22)
    string(sh, P["string"], 0.4)
    arrow(sh, P["neth"], P["diamond"], P["wing"], 3.3, "bodkin", cap_mat=P["gold"], glow_mat=None)
    return sh


# ================================================================ 9. SAGITTARIUS: midnight-enamel and gold, a zodiac wheel with the archer's glyph over the riser, a horse's head on one tip and a flowing tail on the other, a constellation
def sag_palette():
    S = K.surface
    P = {}
    P["gold"] = S("sgold", rgb(238, 184, 72), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 244, 190), dirt=0.9, dirt_color=(0.05, 0.03, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.2, mottle=0.9, mottle_color=rgb(140, 90, 26),
                  fine=0.5, seed=181, bevel=0.18, ao_dist=1.8)
    P["gold2"] = S("sgold2", rgb(176, 124, 44), 1.0, 0.38, wear=0.9, wear_color=rgb(250, 220, 140), dirt=1.0, dirt_color=(0.04, 0.02, 0.004, 1), scratches=0.5, bump="brushed", bump_strength=0.12, mottle=0.9, mottle_color=rgb(90, 56, 14), seed=182, bevel=0.14)
    P["navy"] = S("enamel", rgb(22, 30, 88), 0.55, 0.12, wear=0.9, wear_color=rgb(150, 170, 255), wear_metal=0.9, dirt=0.8, dirt_color=(0.002, 0.003, 0.012, 1), scratches=0.6, bump="grain", bump_scale=0.6, bump_strength=0.08,
                  mottle=1.0, mottle_color=rgb(40, 60, 150), seed=183, bevel=0.16, ao_dist=2.0)
    P["mane"] = S("mane", rgb(240, 214, 140), 0.6, 0.35, wear=0.6, wear_color=rgb(255, 250, 220), feather=(15, 3, 0.6, 0.3), dirt=0.4, tramp=[(0.0, rgb(150, 100, 40)), (0.5, rgb(240, 200, 110)), (1.0, rgb(255, 250, 230))],
                  mottle=0.6, mottle_color=rgb(170, 120, 50), seed=184, bevel=0.06)
    P["star"] = S("sstar", rgb(255, 240, 180), 0.0, 0.3, emit=rgb(255, 238, 170), emit_strength=1.0, seed=185, bevel=0.0)
    P["line"] = S("sline", rgb(220, 232, 255), 0.0, 0.3, emit=rgb(190, 215, 255), emit_strength=0.8, seed=186, bevel=0.0)
    P["gem"] = S("sgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=187, bevel=0.0)
    P["string"] = S("sstr", rgb(255, 244, 200), 0.0, 0.4, emit=rgb(255, 235, 170), emit_strength=0.4, bump="wrap", bump_strength=0.4, seed=188, bevel=0.0)
    return P


def star_plate(name, cx, cy, z, r, mat, part, pts=4, thick=0.4):
    outline = []
    for i in range(pts * 2):
        a = TAU * i / (pts * 2) + math.pi / 2
        rad = r if i % 2 == 0 else r * 0.34
        outline.append((cx + rad * math.cos(a), cy + rad * math.sin(a)))
    return K.plate(name, outline, z - thick, z + thick, mat, part=part, bevel_w=0.0)


def sagittarius(o):
    sh = dict(belly=23.0, power=1.9, curl=9.0, riser=12.0, cstart=0.74, curl_lo=8.0)
    begin(o["samples"])
    P = sag_palette()
    rr = K.rng(9909)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 3.6 - 2.1 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.6 - 1.4 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_navy%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.32), P["navy"], "limb_navy")
        noisy(ob, 0.08, 0.06, 101 + s)
        stave("limb_inlay%d" % s, L[s], lambda t: [(x - wd(t) * 0.55, z * 0.55 + (0.2 if z > 0 else -0.2) * 1.0) for x, z in rsec(0.5, hh(t) * 1.08, 0.3, 1)], P["gold"], "limb_inlay")
        for k, t in enumerate((0.1, 0.24, 0.38, 0.52, 0.66, 0.8, 0.92)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.25, hh(t) + 0.2, 0.32)       # noqa: E731
            band("limb_band%d_%d" % (s, k), L[s], t, sc, 0.7 - 0.2 * t, P["gold"] if k % 2 == 0 else P["gold2"], "limb_band", width=0.022 - 0.002 * (k % 3))
        for k, t in enumerate((0.2, 0.4, 0.6)):
            p, tn, nr = at(L[s], t)
            for z in (1, -1):
                star_plate("limb_star%d_%d_%d" % (s, k, z), p.x, p.y, (hh(t) + 0.15) * z, 1.5 - 0.5 * t, P["gold"], "limb_star", pts=4, thick=0.22)
        # swept gold wing-blades along the back of each limb (layered, thick, stepped)
        for i in range(5):
            t = 0.1 + 0.11 * i
            p, tn, nr = at(L[s], t)
            a = math.radians(10 + 5 * i)
            d = (tn * math.cos(a) + nr * math.sin(a)).normalized()
            K.feather("limb_blade%d_%d" % (s, i), p + nr * (wd(t) + 0.4) + V((0, 0, 0.5 * (1 if i % 2 else -1))), d, 24.0 - 2.5 * i, 3.8, P["gold"] if i % 2 == 0 else P["mane"], facing=(0, 0, 1), curl=1.8 * (1 if i % 2 else -1),
                      rows=8, serrate=0.08, thick=0.3, part="limb_blade", seed=rr.randint(0, 99), compact=False, droop=1.2, ridge=2.0)
    # the horse's head on the upper tip, mane streaming back along the limb
    pe, tn, nr = at(L[1], 1.0)
    hc = V((pe.x + 4.2, pe.y - 0.6, 0.0))
    K.tube("limb_hneck", [tuple(pe - tn * 4.0), tuple(pe), tuple(pe + nr * 2.0 + tn * 0.8), tuple(hc - V((1.5, 0, 0)))], 1.3, P["gold"], sides=8, sub=3, part="limb_horse", taper=(1.7, 1.3))
    K.blob("limb_hskull", tuple(hc), (3.0, 2.1, 1.7), P["gold"], seed=7, lump=0.08, rings=8, segs=10, part="limb_horse")
    K.tapered("limb_hmuzzle", [hc + V((1.5, -0.2, 0)), hc + V((4.2, -1.0, 0)), hc + V((6.0, -1.8, 0))], 1.5, 0.95, P["gold"], sides=8, sub=2, part="limb_horse")
    K.blob("limb_hnose", tuple(hc + V((6.0, -1.9, 0))), (1.2, 1.0, 1.1), P["gold2"], seed=8, lump=0.08, rings=6, segs=8, part="limb_horse")
    for z in (1, -1):
        K.tapered("limb_hear%d" % z, [hc + V((-0.8, 1.6, 0.9 * z)), hc + V((-1.2, 3.1, 1.3 * z)), hc + V((-0.4, 4.4, 1.0 * z))], 0.65, 0.08, P["gold"], sides=5, sub=2, part="limb_horse")
        K.blob("limb_heye%d" % z, tuple(hc + V((1.8, 0.5, 1.5 * z))), (0.5, 0.5, 0.35), P["star"], seed=z + 4, lump=0.0, rings=4, segs=6, part="limb_horse")
    for k in range(4):
        pth = [hc + V((-1.0, 1.2, (k - 1.5) * 0.6)) + (tn * -(2.0 * j)) + nr * (0.9 * j * 0.3 * (1 + 0.2 * k)) + V((0.5 * math.sin(j * 0.7 + k), 0, 0.3 * math.cos(j * 0.6 + k))) for j in range(9)]
        K.ribbon("limb_hmane%d" % k, pth, lambda u: 2.2 * (1 - 0.6 * u) + 0.3, P["mane"], facing=(0, 0, 1), thick=0.16, part="limb_horse", fray=0.6, rng_=rr, edge_wave=0.12, notch=0.5)
    # the flowing tail on the lower tip
    pe2, tn2, nr2 = at(L[-1], 1.0)
    K.tapered("limb_nockL", [pe2 - tn2 * 4.0, pe2, pe2 + nr2 * 1.2], 1.3, 0.4, P["gold"], sides=6, sub=2, part="limb_nock")
    for k in range(5):
        pth = [pe2 + V((0, 0, (k - 2) * 0.7)) + tn2 * (-2.0 * j) + nr2 * (1.0 * j * (0.3 + 0.1 * k) * 0.4 + 0.5 * math.sin(j * 0.5 + k)) + V((0, 0, 0.6 * math.sin(j * 0.7 + k * 1.3))) for j in range(10)]
        K.ribbon("limb_tail%d" % k, pth, lambda u: 2.4 * (1 - 0.65 * u) + 0.3, P["mane"], facing=(0, 0, 1), thick=0.16, part="limb_horse", fray=0.8, rng_=rr, edge_wave=0.12, notch=0.6)
    # riser: gold-and-enamel plates, the stone, a zodiac wheel with the archer's glyph standing over it on three struts
    riser("riser_core", sh, [(-14.0, 2.5, 2.7), (-11.0, 3.4, 3.2), (-7.0, 3.0, 3.0), (3.0, 3.0, 3.0), (5.0, 4.4, 3.4), (10.0, 5.2, 3.4), (14.0, 3.6, 3.2), (16.5, 2.3, 2.6)], P["navy"])
    hand_binding(sh, P["navy"], P["gold"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["gold"], turns=7, cord=0.3)
    jewel(sh, P["gem"], P["gold"], rr, y=6.8, r=3.7, claws=6, z=3.2)
    wc = V((bx, 6.8, 0.0))                                                  # the zodiac wheel is a halo round the stone
    ring = [tuple(wc + V((9.6 * math.cos(a), 9.6 * math.sin(a), 0.0))) for a in [TAU * i / 20 for i in range(21)]]
    K.tube("riser_wheel", ring, 0.95, P["gold"], sides=6, sub=1, part="riser_wheel")
    K.tube("riser_wheel_in", [tuple(wc + V((7.0 * math.cos(a), 7.0 * math.sin(a), 0.0))) for a in [TAU * i / 20 for i in range(21)]], 0.4, P["gold2"], sides=4, sub=1, part="riser_wheel")
    for i in range(8):                                                     # the signs: studs between the rings
        a = TAU * (i + 0.5) / 8
        K.blob("riser_stud%d" % i, tuple(wc + V((8.3 * math.cos(a), 8.3 * math.sin(a), 0.0))), (0.8, 0.8, 1.0), P["star"] if i == 5 else P["gold"], seed=i, lump=0.05, rings=4, segs=5, part="riser_wheel")
    ang = math.radians(45)
    d = V((math.cos(ang), math.sin(ang), 0))
    sd = V((-d.y, d.x, 0))
    K.bar("riser_glyph_shaft", tuple(wc - d * 9.0), tuple(wc + d * 8.6), 0.6, 0.8, P["gold"], part="riser_glyph", taper=1.0, bevel_w=0.1)
    K.plate("riser_glyph_head", [tuple((wc + d * 10.6)[:2]), tuple((wc + d * 6.6 + sd * 3.3)[:2]), tuple((wc + d * 7.8)[:2]), tuple((wc + d * 6.6 - sd * 3.3)[:2])], -0.6, 0.6, P["gold"], part="riser_glyph", bevel_w=0.1)
    K.bar("riser_glyph_cross", tuple(wc - d * 4.8 - sd * 4.2), tuple(wc - d * 4.8 + sd * 4.2), 0.5, 0.65, P["gold"], part="riser_glyph", bevel_w=0.1)
    # the constellation: nine stars joined by lines of light, hung on the target side
    cons = [(bx + 14, 36), (bx + 20, 30), (bx + 25, 21), (bx + 22, 11), (bx + 28, 4), (bx + 24, -6), (bx + 17, -3), (bx + 15, 5), (bx + 20, 14)]
    for i, (x, y) in enumerate(cons):
        star_plate("riser_cstar%d" % i, x, y, 0.0, 2.1 if i in (0, 3, 5) else 1.4, P["star"], "riser_cons", pts=4 if i % 2 == 0 else 5, thick=0.3)
    for a_, b_ in ((0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7), (7, 3), (3, 8), (8, 2)):
        K.tube("riser_cline%d_%d" % (a_, b_), [(cons[a_][0], cons[a_][1], 0.0), (cons[b_][0], cons[b_][1], 0.0)], 0.18, P["line"], sides=4, sub=1, part="riser_cons")
    string(sh, P["string"], 0.4)
    arrow(sh, P["gold2"], P["gold"], P["mane"], 3.3, "broad", cap_mat=P["navy"], glow_mat=None)
    return sh


# ==== DRIVER ====
FORMS = {}   # key: (builder, form id)


def run(only, trial, quick, lowres, samples):
    entries, results = {}, []
    for key in only:
        fn, form = FORMS[key]
        mesh_id = "legendary_bow__" + form
        t0 = time.time()
        sh = fn({"samples": samples})
        print("   parts:", K.quad_table(16))
        print("[%s] built geometry: ~%d body quads in %.1f s" % (mesh_id, K.quad_estimate(), time.time() - t0))
        if quick:
            continue
        results.append(finish_bow(mesh_id, sh, trial, lowres))
        if not lowres:
            entries[mesh_id] = {"weapon": "legendary_bow", "form": form, "class": "bow"}
    if entries and not trial:
        K.write_manifest(MANIFEST, entries)
    return results


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(FORMS))
    ap.add_argument("--trial")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--lowres", action="store_true")
    ap.add_argument("--samples", type=int, default=16)
    a = ap.parse_args(argv)
    run(a.only.split(","), a.trial, a.quick, a.lowres, a.samples)


# ==== REGISTRY ====
FORMS["hunters_bow"] = (hunters_bow, "hunters_bow")
FORMS["scouts_bow"] = (scouts_bow, "scouts_bow")
FORMS["bind_arrow"] = (bind_arrow, "bind_arrow")
FORMS["bird_hunting"] = (bird_hunting, "bird_hunting")
FORMS["arrow_shower"] = (arrow_shower, "arrow_shower")
FORMS["meteor_bow"] = (meteor_bow, "meteor_bow")
FORMS["frozen_rain"] = (frozen_rain, "frozen_rain")
FORMS["eagle_piercer"] = (eagle_piercer, "eagle_piercer")
FORMS["sagittarius"] = (sagittarius, "sagittarius")

if __name__ == "__main__":
    main(sys.argv[1:])
