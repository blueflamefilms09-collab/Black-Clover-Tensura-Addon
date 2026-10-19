"""Why: the preview of the player-model kit (preview.py) has to draw the GeoLite extras (the geo.json models of the arc art kit, anchored on a model part) the way client/playermodel's
extras layer does: every root bone x_<anchor> under the pose of that part of the model, then the file's bone tree with the same pivots, rotations and animation as client/geo/GeoDraw. This is
that part of the software renderer, on top of pmrender.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pmrender as R  # noqa: E402
from pmrender import ASSETS, cube_faces, mat_rot_zyx, mat_translate, box_regions  # noqa: E402


def geo_load(stem):
    p = os.path.join(ASSETS, "geo", "entity", stem + ".geo.json")
    if not os.path.isfile(p):
        return None
    return json.load(open(p))["minecraft:geometry"][0]


def anim_load(stem):
    p = os.path.join(ASSETS, "animations", "entity", stem + ".animation.json")
    if not os.path.isfile(p):
        return {}
    return json.load(open(p)).get("animations", {})


def face_rects(c):
    """Per-face uv rectangles (u1, v1, u2, v2) by vanilla face name (west, north, east, south, top, bottom) from a cube dict, as client/geo/GeoModelData reads them."""
    uv = c.get("uv")
    out = {}
    if isinstance(uv, dict):
        for fname, vname in (("east", "west"), ("north", "north"), ("west", "east"), ("south", "south"), ("up", "top"), ("down", "bottom")):
            f = uv.get(fname)
            if not f:
                continue
            u, v = f["uv"]
            w, h = f["uv_size"]
            out[vname] = (u, v + h, u + w, v) if vname == "bottom" else (u, v, u + w, v + h)
    else:
        u, v = uv if uv else (0, 0)
        w, h, d = c["size"]
        for k_, (x, y, rw, rh) in box_regions(u, v, w, h, d).items():
            out[k_] = (x, y, x + rw, y + rh)
        out["bottom"] = (u + d + w, v + d, u + d + 2 * w, v)
    return out


def _vec(v):
    if isinstance(v, dict):
        v = v.get("post", v.get("pre", [0, 0, 0]))
    if isinstance(v, (int, float)):
        return [float(v)] * 3
    out = []
    for x in v:
        try:
            out.append(float(x))
        except (TypeError, ValueError):
            out.append(0.0)
    while len(out) < 3:
        out.append(0.0)
    return out


def _sample(track, t):
    if not isinstance(track, dict) or "post" in track or "pre" in track or "vector" in track:
        return _vec(track)
    keys = sorted(((float(k), v) for k, v in track.items()), key=lambda kv: kv[0])
    if not keys:
        return [0.0, 0.0, 0.0]
    if t <= keys[0][0]:
        return _vec(keys[0][1])
    for i in range(len(keys) - 1):
        if keys[i][0] <= t <= keys[i + 1][0]:
            a, b = _vec(keys[i][1]), _vec(keys[i + 1][1])
            span = keys[i + 1][0] - keys[i][0]
            k = 0.0 if span <= 1e-9 else (t - keys[i][0]) / span
            return [a[j] + (b[j] - a[j]) * k for j in range(3)]
    return _vec(keys[-1][1])


def sample_clip(clip, t):
    """{bone: {'rotation': [x, y, z], 'position': [x, y, z]}} of a clip dict at t seconds (looped)."""
    if not clip:
        return {}
    length = float(clip.get("animation_length", 0) or 0)
    tt = (t % length) if length > 0 else 0.0
    out = {}
    for bn, tr in clip.get("bones", {}).items():
        d = {}
        for kind in ("rotation", "position"):
            if kind in tr:
                d[kind] = _sample(tr[kind], tt)
        out[bn] = d
    return out


def geo_faces(g, tex, anchors, tint=(1, 1, 1), emissive=False, mode="cutout", anim=None):
    """The faces of a model whose roots are x_<anchor>. `anchors` maps an anchor name to (pose matrix, pivot): the matrix is the part's own pose (it puts the origin at the pivot), so the
    file's absolute coordinates (y up, 24 px = the neck line) are restored by undoing the pivot, as RebuildSetKit.paint does."""
    bones = {b["name"]: b for b in g["bones"]}
    children = {}
    roots = []
    for b in g["bones"]:
        p = b.get("parent")
        if p and p in bones:
            children.setdefault(p, []).append(b)
        else:
            roots.append(b)
    th, tw = tex.shape[0], tex.shape[1]
    desc = g["description"]
    su = tw / float(desc.get("texture_width", tw))
    sv = th / float(desc.get("texture_height", th))
    faces = []

    def walk(b, parent, m):
        px, py, pz = b["pivot"]
        x = px - (parent["pivot"][0] if parent else 0)
        y = -py + (parent["pivot"][1] if parent else 0)
        z = pz - (parent["pivot"][2] if parent else 0)
        rx, ry, rz = b.get("rotation") or (0, 0, 0)
        a = (anim or {}).get(b["name"])
        if a:
            if a.get("rotation") is not None:
                rx, ry, rz = rx + a["rotation"][0], ry + a["rotation"][1], rz + a["rotation"][2]
            if a.get("position") is not None:
                x += a["position"][0]
                y -= a["position"][1]
                z += a["position"][2]
        mb = m @ mat_translate(x, y, z)
        if rx or ry or rz:
            mb = mb @ mat_rot_zyx(-math.radians(rx), math.radians(ry), -math.radians(rz))
        for c in b.get("cubes", []):
            o, s = c["origin"], c["size"]
            g_ = c.get("inflate", 0.0)
            minx, maxx = o[0] - px - g_, o[0] - px + s[0] + g_
            miny, maxy = -(o[1] + s[1]) + py - g_, -o[1] + py + g_
            minz, maxz = o[2] - pz - g_, o[2] - pz + s[2] + g_
            mc = mb
            if c.get("rotation") and any(c["rotation"]):
                cp = c.get("pivot", b["pivot"])
                cx, cy, cz = cp[0] - px, -cp[1] + py, cp[2] - pz
                crx, cry, crz = c["rotation"]
                mc = mb @ mat_translate(cx, cy, cz) @ mat_rot_zyx(-math.radians(crx), math.radians(cry), -math.radians(crz)) @ mat_translate(-cx, -cy, -cz)
            mirror = bool(c.get("mirror", b.get("mirror", False)))
            fuv = {k_: (r[0] * su, r[1] * sv, r[2] * su, r[3] * sv) for k_, r in face_rects(c).items()}
            # cube_faces takes pixel uv rectangles of the sheet size it is given; the rectangles above are in the file's texture units, so convert with the sheet's scale
            faces.extend(cube_faces(mc, (minx, miny, minz), (maxx - minx, maxy - miny, maxz - minz), 0.0, None, None, tex, tint, mirror=mirror, emissive=emissive, mode=mode,
                                    face_uv=fuv))
        for ch in children.get(b["name"], []):
            walk(ch, b, mb)

    for r in roots:
        anchor = r["name"][2:] if r["name"].startswith("x_") else "body"
        if anchor not in anchors:
            continue
        am, piv = anchors[anchor]
        base = am @ mat_translate(-piv[0], -piv[1], -piv[2]) @ mat_translate(0, 24, 0)
        walk(r, None, base)
    return faces
