#!/usr/bin/env python3
"""solidify_flat_parts: makes the flat parts of the hero weapon meshes solid (the Blender-less kit: pure Python 3 + numpy, no bpy).

Found by tools/heroweapon/flat_parts_audit.py (docs/heroweapon/flat_parts.md):

  * the head of the Legendary Spear (base, curse and its nine forms) and of the Vassal Harpoon was a paper-thin leaf: it is THICKENED along its thinnest
    axis (the leaf keeps its outline, its UVs, its inlay and its glow; the section becomes a fat diamond with a ridge);
  * the arrow nocked on the Legendary Bow in its three draw stages (base, curse, nine forms: 33 meshes) was a hairline with a speck for a head: it is
    REBUILT as a real arrow: an octagonal shaft with a nock collar, a diamond broadhead with a ridge, three fletching vanes crossed in 3D.

Every mesh that is changed is first copied, untouched, to weapon_meshes/flat_parts_old/<id>.txt (CLAUDE.md rule 2: nothing is deleted; the client setting
`visual.heroSolidParts = false` draws these old files). The new arrow quads take the skin texel of the part they replace (the old arrow is a plain colour
on the skin; the spear head keeps its own UVs), so no skin is repainted. Running it twice does nothing (a part that is thick enough, or an arrow that is
already rebuilt, is left alone).

    python3 -B tools/heroweapon/solidify_flat_parts.py            # change the shipped meshes
    python3 -B tools/heroweapon/solidify_flat_parts.py --dry-run  # only say what would change
    python3 -B tools/heroweapon/solidify_flat_parts.py --out DIR  # write the changed meshes to DIR instead (no backup, shipped files untouched)
"""
import argparse
import glob
import math
import os
import re
import shutil
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import flat_parts_audit as audit  # noqa: E402

MESHES = audit.MESHES
OLD = os.path.join(MESHES, "flat_parts_old")

HEAD_TARGET_BLOCKS = 0.105        # thickness a spear / harpoon head gets (about 2.6 % of the weapon; the audit asks for 2 %)
HEAD_MAX_FACTOR = 4.0             # never stretch a head more than this (a leaf that was a card of 0.02 gets 0.08)
HEAD_PARTS = re.compile(r"^(spearhead|head_blade|head_barbs|head_prongs)$")

# the arrow, in blocks (converted with the mesh's `place` scale): the bow is 1.2 blocks in the mesh, the arrow about 0.5
SHAFT_R = 0.0165
HEAD_LEN, HEAD_HALF_W, HEAD_HALF_T, NECK_R = 0.095, 0.034, 0.0155, 0.0195
VANE_LEN, VANE_H, VANE_T, VANE_START = 0.14, 0.028, 0.006, 0.014
SHAFT_SIDES = 8
ARROW_PARTS = {"shaft": re.compile(r"^arrow_(shaft|stem)$"), "head": re.compile(r"^arrow_head$"), "fletch": re.compile(r"^arrow_fletch$")}


# ================================================================ mesh text
class Mesh:
    def __init__(self, path):
        self.path = path
        with open(path, encoding="utf-8") as f:
            self.lines = f.read().split("\n")
        self.q = [i for i, ln in enumerate(self.lines) if ln.startswith("qn ") or ln.startswith("q ")]
        self.parts = {}                                   # name -> (label line, first, count)
        self.scale = 1.0
        for i, ln in enumerate(self.lines):
            v = ln.split()
            if v[:2] == ["#", "part"] and len(v) >= 5:
                self.parts[v[2]] = (i, int(v[3]), int(v[4]))
            elif v[:1] == ["place"] and len(v) > 4:
                self.scale = float(v[4])

    def quad(self, k):
        """-> (layer, 4x3 xyz, 4x2 uv, 4x3 normals) of quad number k; only `qn` lines (the hero meshes have no `q` lines)."""
        v = self.lines[self.q[k]].split()
        if v[0] != "qn":
            raise ValueError("q line in %s" % self.path)
        a = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
        return int(v[1]), a[:, 0:3], a[:, 3:5], a[:, 5:8]

    def range(self, name):
        _, first, count = self.parts[name]
        return range(first, first + count)

    def write(self, path):
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(self.lines))


def fmt_qn(layer, xyz, uv, nrm):
    out = ["qn", str(layer)]
    for k in range(4):
        out += ["%.3f" % xyz[k][0], "%.3f" % xyz[k][1], "%.3f" % xyz[k][2], "%.3f" % uv[k][0], "%.3f" % uv[k][1],
                "%.4f" % nrm[k][0], "%.4f" % nrm[k][1], "%.4f" % nrm[k][2]]
    return " ".join(out)


# ================================================================ thickening a head
def thicken(mesh, name, target_blocks, max_factor):
    """Stretches the part along its thinnest axis about its centre; True when it changed."""
    marker = "# solid %s" % name                           # written when a part was thickened: running again does nothing
    if marker in mesh.lines:
        return False
    ids = list(mesh.range(name))
    body = [mesh.quad(k) for k in ids]
    pts = np.vstack([b[1] for b in body if b[0] == 0] or [b[1] for b in body])
    c = pts.mean(axis=0)
    a = np.linalg.eigh(np.cov((pts - c).T))[1][:, 0]       # the thinnest principal axis (eigh sorts ascending)
    t = ((pts - c) @ a).max() - ((pts - c) @ a).min()
    thickness = t * mesh.scale
    if thickness >= target_blocks * 0.95:
        return False
    f = min(max_factor, target_blocks / thickness)
    for k in ids:
        layer, xyz, uv, nrm = mesh.quad(k)
        xyz = xyz + np.outer((xyz - c) @ a, a) * (f - 1.0)
        n2 = nrm + np.outer(nrm @ a, a) * (1.0 / f - 1.0)           # the normal of a stretched surface (inverse transpose)
        n2 /= np.maximum(np.linalg.norm(n2, axis=1, keepdims=True), 1e-9)
        mesh.lines[mesh.q[k]] = fmt_qn(layer, xyz, uv, n2)
    mesh.lines.insert(len(mesh.lines) - 1 if mesh.lines and mesh.lines[-1] == "" else len(mesh.lines), marker)
    return True


# ================================================================ building an arrow
class Builder:
    """The quads of convex solids; each is wound counter-clockwise seen from outside (normal = (p1-p0) x (p3-p0)), the way the renderer wants them."""

    def __init__(self):
        self.faces = []

    def add_solid(self, faces):
        faces = [np.asarray(f, float) for f in faces]
        centre = np.vstack(faces).mean(axis=0)
        for f in faces:
            n = np.cross(f[1] - f[0], f[3] - f[0])
            if np.linalg.norm(n) < 1e-9:                      # a triangle written (a, b, c, c): use the other pair
                n = np.cross(f[1] - f[0], f[2] - f[0])
            if n @ (f.mean(axis=0) - centre) < 0:
                f = f[[0, 3, 2, 1]]
                n = -n
            self.faces.append((f, n / (np.linalg.norm(n) or 1.0)))


def loft_faces(sections):
    """sections: N x 3 rings (same N, in order about the axis): the side quads between neighbours and a cap fan at both ends (a ring that is a point has no cap)."""
    faces = []
    n = len(sections[0])
    for s0, s1 in zip(sections, sections[1:]):
        for i in range(n):
            j = (i + 1) % n
            faces.append([s0[i], s0[j], s1[j], s1[i]])
    for sec in (sections[0], sections[-1]):
        if np.allclose(sec, sec[0]):
            continue
        for i in range(1, n - 1, 2):
            faces.append([sec[0], sec[i], sec[i + 1], sec[i + 2] if i + 2 < n else sec[i + 1]])
    return faces


def build_arrow_parts(origin, axis, up, side, length, scale):
    """The arrow as three solids' worth of quads: shaft (with nock collar and neck), head, fletching. origin = the tail end, axis = unit vector to the point,
    up = the broadhead's wide direction, side = the third axis; length in mesh units; sizes in blocks / scale."""
    k = 1.0 / scale
    r = SHAFT_R * k
    head0 = length - HEAD_LEN * k

    def ring(t, rad, n=SHAFT_SIDES):
        ph = math.pi / n
        return np.array([origin + axis * t + up * rad * math.cos(2 * math.pi * i / n + ph) + side * rad * math.sin(2 * math.pi * i / n + ph) for i in range(n)])

    def diamond(t, w, h):
        return np.array([origin + axis * t + up * w, origin + axis * t + side * h, origin + axis * t - up * w, origin + axis * t - side * h])

    shaft, head, fletch = Builder(), Builder(), Builder()
    nock = 0.022 * k
    shaft.add_solid(loft_faces([ring(0.0, r * 1.3), ring(nock, r * 1.3), ring(nock + 0.008 * k, r), ring(head0, r), ring(head0 + 0.006 * k, NECK_R * k)]))
    head.add_solid(loft_faces([diamond(head0 + 0.006 * k, NECK_R * k, NECK_R * k * 0.9), diamond(head0 + HEAD_LEN * k * 0.36, HEAD_HALF_W * k, HEAD_HALF_T * k),
                               diamond(head0 + HEAD_LEN * k * 0.64, HEAD_HALF_W * k * 0.78, HEAD_HALF_T * k * 0.8), diamond(length, 0.0, 0.0)]))
    t0, t1 = VANE_START * k, (VANE_START + VANE_LEN) * k
    half = VANE_T * k * 0.5
    for v in range(3):                                         # three vanes, 120 degrees apart: crossed in 3D
        ang = math.radians(90 + 120 * v)
        radial = up * math.cos(ang) + side * math.sin(ang)
        tang = np.cross(axis, radial)
        prof = [(t0, r * 0.9), (t0 + 0.2 * (t1 - t0), r + VANE_H * k), (t1 - 0.12 * (t1 - t0), r + VANE_H * k), (t1, r * 0.9)]
        pts = [origin + axis * t + radial * rr for (t, rr) in prof]
        faces = [[p - tang * half for p in pts], [p + tang * half for p in pts]]
        for i in range(4):
            j = (i + 1) % 4
            faces.append([pts[i] - tang * half, pts[j] - tang * half, pts[j] + tang * half, pts[i] + tang * half])
        fletch.add_solid(faces)
    return {"shaft": shaft, "head": head, "fletch": fletch}


def skin_image(mesh, glow):
    """The skin (or glow skin) PNG of a mesh as an array, with the fallback to the rest mesh's skin the game uses for the draw stages; None if there is none."""
    from PIL import Image
    mid = os.path.splitext(os.path.basename(mesh.path))[0]
    d = os.path.join(audit.MESHES, "..", "textures", "entity", "weapon")
    for n in (mid, re.sub(r"_(pull[123]|charged|spent)$", "", mid)):
        p = os.path.join(d, n + ("_glow" if glow else "") + ".png")
        if os.path.exists(p):
            return np.asarray(Image.open(p).convert("RGBA"))
    return None


def old_uv(mesh, name):
    """A painted skin texel (uv, 128 space) of a part, per layer (0 = the skin, 2 = the glow copy): the centre of the first of its quads that lands on an opaque texel
    (the mean of the part's uv can fall between islands, on an empty texel); None for a layer the part has no quads in."""
    out = {}
    for layer in (0, 2):
        cents = [q[2].mean(axis=0) for q in (mesh.quad(k) for k in mesh.range(name)) if q[0] == layer]
        if not cents:
            out[layer] = None
            continue
        img = skin_image(mesh, layer == 2)
        pick = np.vstack(cents).mean(axis=0)
        if img is not None:
            k = img.shape[1] / 128.0
            for c in cents:
                x, y = int(c[0] * k), int(c[1] * k)
                if 0 <= y < img.shape[0] and 0 <= x < img.shape[1] and img[y, x, 3] > 230:
                    pick = c
                    break
        out[layer] = pick
    return out


def rebuild_arrow(mesh):
    """Replaces the arrow parts (the last quads of the file) with the new arrow; returns the number of quad lines written (0 = nothing to do)."""
    names = {kind: next((n for n in mesh.parts if rx.search(n)), None) for kind, rx in ARROW_PARTS.items()}
    if not all(names.values()):
        return 0
    first = min(mesh.parts[n][1] for n in names.values())
    last = max(mesh.parts[n][1] + mesh.parts[n][2] for n in names.values())
    if last != len(mesh.q) or sum(mesh.parts[n][2] for n in names.values()) != last - first:
        raise ValueError("the arrow parts of %s are not the last quads of the file" % mesh.path)
    allp, shaftp, headp = [], [], []
    for kind, n in names.items():
        for k in mesh.range(n):
            layer, xyz, _, _ = mesh.quad(k)
            if layer == 0:
                allp.append(xyz)
                {"shaft": shaftp, "head": headp}.get(kind, []).append(xyz)
    allp, shaftp, headp = np.vstack(allp), np.vstack(shaftp), np.vstack(headp)
    if audit.thickness(shaftp)[0] * mesh.scale >= 0.025:
        return 0                                                # already a thick arrow (this tool ran before)
    axis = np.linalg.eigh(np.cov((allp - allp.mean(axis=0)).T))[1][:, 2]
    if (headp.mean(axis=0) - shaftp.mean(axis=0)) @ axis < 0:
        axis = -axis
    proj = allp @ axis
    length = float(proj.max() - proj.min())
    if length * mesh.scale < 0.2:
        raise ValueError("the arrow of %s is too short to be one" % mesh.path)
    line_pt = shaftp.mean(axis=0)
    origin = line_pt - axis * (line_pt @ axis) + axis * proj.min()          # the centre line of the shaft, at the tail end
    up = np.array([0.0, 1.0, 0.0]) - axis * axis[1]                           # the broadhead's wide direction: across the bow, in its own plane
    if np.linalg.norm(up) < 0.2:
        up = np.array([1.0, 0.0, 0.0]) - axis * axis[0]
    up /= np.linalg.norm(up)
    side = np.cross(axis, up)
    side /= np.linalg.norm(side)
    uvs = {kind: old_uv(mesh, n) for kind, n in names.items()}               # the old arrow is a plain colour on the skin: the new quads take its centre texel
    parts = build_arrow_parts(origin, axis, up, side, length, mesh.scale)
    new_lines, counts = [], {}
    for kind in ("shaft", "head", "fletch"):
        n0 = len(new_lines)
        for f, n in parts[kind].faces:
            for layer in (0, 2):
                if uvs[kind][layer] is not None:
                    new_lines.append(fmt_qn(layer, f, np.tile(uvs[kind][layer], (4, 1)), np.tile(n, (4, 1))))
        counts[kind] = len(new_lines) - n0
    lines = mesh.lines[:mesh.q[first]] + new_lines + mesh.lines[mesh.q[-1] + 1:]      # the labels may sit after the quads: they are kept, then renumbered
    start = first
    for kind in ("shaft", "head", "fletch"):
        prefix = "# part %s " % names[kind]
        for i, ln in enumerate(lines):
            if ln.startswith(prefix):
                lines[i] = "%s%d %d" % (prefix, start, counts[kind])
        start += counts[kind]
    mesh.lines = lines
    return len(new_lines)


# ================================================================ the run
def run_one(path, out_dir, dry):
    mid = os.path.splitext(os.path.basename(path))[0]
    mesh = Mesh(path)
    changed = []
    if re.match(r"^(legendary_spear|vassal_harpoon)", mid):
        for name in list(mesh.parts):
            if HEAD_PARTS.search(name) and thicken(mesh, name, HEAD_TARGET_BLOCKS, HEAD_MAX_FACTOR):
                changed.append("thickened " + name)
    elif re.match(r"^legendary_bow.*_pull[123]$", mid):
        n = rebuild_arrow(mesh)
        if n:
            changed.append("arrow rebuilt (%d quad lines)" % n)
    if not changed:
        return None
    if not dry:
        if out_dir:
            os.makedirs(out_dir, exist_ok=True)
            mesh.write(os.path.join(out_dir, mid + ".txt"))
        else:
            os.makedirs(OLD, exist_ok=True)
            if not os.path.exists(os.path.join(OLD, mid + ".txt")):
                shutil.copyfile(path, os.path.join(OLD, mid + ".txt"))        # the old, flat mesh stays (rule 2)
            mesh.write(path)
    return changed


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--out", default=None)
    ap.add_argument("ids", nargs="*")
    a = ap.parse_args(argv)
    done = 0
    for p in sorted(glob.glob(os.path.join(MESHES, "*.txt"))):
        mid = os.path.splitext(os.path.basename(p))[0]
        if a.ids and mid not in a.ids:
            continue
        res = run_one(p, a.out, a.dry_run)
        if res:
            done += 1
            print("%-48s %s" % (mid, "; ".join(res)))
    print("%d meshes %s" % (done, "would change" if a.dry_run else "changed"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
