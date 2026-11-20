#!/usr/bin/env python3
"""Adds the swing rig (docs/vassal_forms_v2.md) to the whip / flail / morning-star meshes, from their geometry, without a re-bake.

    python3 -B tools/weaponmotion/build_swing.py            # every mesh of motion_specs.json
    python3 -B tools/weaponmotion/build_swing.py --only vassal_chain,vassal_whip --dry
    python3 -B tools/weaponmotion/build_swing.py --check    # exit 1 when a mesh's rig is missing or stale

What it writes into a mesh file (the renderer reads these two directives, every older reader ignores them):
  bone <i> <parent> <px py pz> <ax ay az> <bx by bz> <gain> <w> <zeta> <maxDeg>
      a joint: pivot, main axis A (the plane normal of the chain: the lag bends the chain in its plane), secondary axis B (out of plane, for the flutter),
      the forcing gain, the spring's natural frequency w (rad/s) and damping ratio zeta, and the bone's angle limit.
  skin <layer> <first vertex> <count> <code> ...
      per vertex of the animated tail of that layer: code = bone * 64 + round(63 * w); the vertex follows bone `bone`, blended towards bone + 1 by w (w = 0: rigid on its bone).
The animated quads are moved to the end of every layer (a rigid vertex never meets a skinned one), the `# part` comments are rewritten for the new order.

Two kinds (motion_specs.json):
  chain  the links are the connected pieces of the `chain_parts` (each link is rigid on its own bone, the joint is the middle between two links); the pieces above `root_y` that are
         not links (ball, spikes, clock, heads) hang on the last bone. `main` limits how many links form the swinging chain (hydra / orochi: the main chain, the heads ride on its end).
  lash   a smooth whip: `bones` slices along y from `root_y` to the tip, every vertex above the root blended between the two slices around it (a bend, no tears).
"""
import argparse
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import meshio  # noqa: E402

SPECS = os.path.join(HERE, "motion_specs.json")
ROUND = 3          # decimals of the vertex keys that join the quads of one link


def key(p):
    return (round(p[0], ROUND), round(p[1], ROUND), round(p[2], ROUND))


class DSU:
    def __init__(self):
        self.p = {}

    def find(self, a):
        self.p.setdefault(a, a)
        while self.p[a] != a:
            self.p[a] = self.p[self.p[a]]
            a = self.p[a]
        return a

    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra != rb:
            self.p[ra] = rb


def components(quads):
    """{vertex key: component id} for the body quads (layer 0 and 2 are copies of the same faces; both are used)."""
    d = DSU()
    for q in quads:
        ks = [key(p) for p in q.pts]
        for k in ks[1:]:
            d.union(ks[0], k)
    return d


def unit(v):
    v = np.asarray(v, float)
    n = float(np.linalg.norm(v))
    return v / n if n > 1e-9 else np.array([0.0, 0.0, 1.0])


def bone_line(i, parent, pivot, axis_a, axis_b, spec):
    return "bone %d %d %.3f %.3f %.3f %.4f %.4f %.4f %.4f %.4f %.4f %.3f %.2f %.3f %.1f" % (
        i, parent, pivot[0], pivot[1], pivot[2], axis_a[0], axis_a[1], axis_a[2], axis_b[0], axis_b[1], axis_b[2],
        spec.get("gain", 1.0), spec.get("w", 12.0), spec.get("zeta", 0.2), spec.get("max", 8.0))


def secondary(axis_a, d):
    b = np.cross(axis_a, d)
    return unit(b) if np.linalg.norm(b) > 1e-6 else unit(np.cross(axis_a, (1, 0, 0)))


def build_chain(m, spec):
    A = unit(spec.get("axis", (0, 0, 1)))
    root_y = spec["root_y"]
    chain_parts = set(spec["chain_parts"])
    link_quads = [q for q in m.quads if q.part in chain_parts and q.layer != 1]
    if not link_quads:
        raise SystemExit("%s: no quads in %s" % (m.id, sorted(chain_parts)))
    dsu = components(link_quads)
    comp = {}
    for q in link_quads:
        comp.setdefault(dsu.find(key(q.pts[0])), []).append(q)
    # order the links from the anchor (the chain's root) by walking to the nearest unvisited link
    cen = {c: np.mean([p for q in qs for p in q.pts], axis=0) for c, qs in comp.items()}
    anchor = np.array(spec.get("anchor") or [0.0, root_y, 0.0], float)
    order, left, cur = [], set(comp), anchor
    steps = {}
    while left:
        nxt = min(left, key=lambda c: float(np.linalg.norm(cen[c] - cur)))
        steps[nxt] = float(np.linalg.norm(cen[nxt] - cur))
        order.append(nxt)
        left.remove(nxt)
        cur = cen[nxt]
    main = spec.get("main") or len(order)
    main_links, extra_links = order[:main], order[main:]
    # braided chains (several strands per level): links closer than group_step to the one before share a bone
    groups = []
    for c in main_links:
        if groups and steps[c] < spec.get("group_step", 0.0):
            groups[-1].append(c)
        else:
            groups.append([c])
    n = len(groups)
    if n + 1 > 31:
        raise SystemExit("%s: %d bones, over the 31 the renderer takes" % (m.id, n + 1))
    link_of = {}
    for i, g in enumerate(groups):
        for c in g:
            for q in comp[c]:
                link_of[id(q)] = i
    # every other piece above the root hangs on the last bone: connected pieces judged by their middle height (glow copies by their own centre)
    head_idx = n
    link_keys = {}
    for i, g in enumerate(groups):
        for c in g:
            for q in comp[c]:
                for p in q.pts:
                    link_keys[key(p)] = i
    rest = [q for q in m.quads if id(q) not in link_of]
    dsu2 = components([q for q in rest if q.layer != 1])
    pieces = {}
    for q in rest:
        pieces.setdefault(dsu2.find(key(q.pts[0])), []).append(q)
    bone_of = dict(link_of)
    for _c, qs in pieces.items():
        cy = float(np.mean([p[1] for q in qs for p in q.pts]))
        if cy >= root_y:
            for q in qs:
                bone_of[id(q)] = head_idx
    # glow copies of a link: the quad sits on the same points as a body quad, which names the link
    for q in m.quads:
        if q.layer == 2 and id(q) not in bone_of:
            ks = [link_keys.get(key(p)) for p in q.pts]
            ks = [k for k in ks if k is not None]
            if ks:
                bone_of[id(q)] = max(set(ks), key=ks.count)
    # pivots
    cen_main = [np.mean([cen[c] for c in g], axis=0) for g in groups]
    head_pts = np.array([p for q in m.quads if bone_of.get(id(q)) == head_idx and q.layer == 0 for p in q.pts]) if any(v == head_idx for v in bone_of.values()) else None
    pivots = [anchor.copy()]
    for i in range(1, n):
        pivots.append((cen_main[i - 1] + cen_main[i]) / 2.0)
    if head_pts is not None and len(head_pts):
        near = head_pts[int(np.argmin(np.linalg.norm(head_pts - cen_main[-1], axis=1)))]
        pivots.append((cen_main[-1] + near) / 2.0)
    else:
        pivots.append(cen_main[-1] + (cen_main[-1] - cen_main[-2]) * 0.5)
    bones = []
    for i, pv in enumerate(pivots):
        nxt = pivots[i + 1] if i + 1 < len(pivots) else pv + (pv - pivots[i - 1])
        d = unit(nxt - pv)
        bones.append(bone_line(i, i - 1, pv, A, secondary(A, d), dict(spec, max=spec.get("max", 8.0) * (1.0 if i < n else 0.6))))
    animated = [q for q in m.quads if id(q) in bone_of]
    return bones, animated, {id(q): bone_of[id(q)] * 64 for q in animated}, n


def build_lash(m, spec):
    A = unit(spec.get("axis", (0, 0, 1)))
    root_y = spec["root_y"]
    nb = spec["bones"]
    ys = [p[1] for q in m.quads if q.layer != 1 for p in q.pts]
    tip_y = spec.get("tip_y", max(ys))
    h = (tip_y - root_y) / nb
    rigid_parts = set(spec.get("rigid_parts", []))
    # a rigid part's connected pieces ride on the bone of their own middle (a glass, a sun disc, a gate): they move with the lash and are never sheared by the blend
    rigid_bone = {}
    rq = [q for q in m.quads if q.part in rigid_parts and q.layer != 1]
    if rq:
        d = components(rq)
        pieces = {}
        for q in rq:
            pieces.setdefault(d.find(key(q.pts[0])), []).append(q)
        for qs in pieces.values():
            cy = float(np.mean([p[1] for q in qs for p in q.pts]))
            b = max(0, min(nb - 1, int((cy - root_y) / h)))
            for q in qs:
                rigid_bone[id(q)] = b
    animated, code_of = [], {}
    for q in m.quads:
        if q.layer == 1:
            continue
        if max(p[1] for p in q.pts) <= root_y + 1e-3:
            continue
        animated.append(q)
        if id(q) in rigid_bone:
            code_of[id(q)] = [rigid_bone[id(q)] * 64] * 4
            continue
        if q.part in rigid_parts:                                   # a glow copy that sits on a rigid piece's points: the bone of its own middle
            b = max(0, min(nb - 1, int((float(np.mean([p[1] for p in q.pts])) - root_y) / h)))
            code_of[id(q)] = [b * 64] * 4
            continue
        cs = []
        for p in q.pts:
            t = max(0.0, (p[1] - root_y) / h)
            b = min(int(t), nb - 1)
            w = 0.0 if b >= nb - 1 else t - b
            cs.append(b * 64 + int(round(w * 63)))
        code_of[id(q)] = cs
    # pivots on the lash's centre line: the mean x / z of the animated vertices within each slice (smoothed), the root on the first slice
    pts = np.array([p for q in animated if id(q) not in rigid_bone for p in q.pts])
    piv = []
    for i in range(nb):
        y0 = root_y + i * h
        sel = pts[np.abs(pts[:, 1] - y0) <= h * 0.75]
        c = sel.mean(axis=0) if len(sel) else np.array([0.0, y0, 0.0])
        piv.append(np.array([c[0], y0, c[2]]))
    sm = [piv[0]] + [(piv[i - 1] + 2 * piv[i] + piv[min(i + 1, nb - 1)]) / 4.0 for i in range(1, nb)]
    bones = []
    for i, pv in enumerate(sm):
        nxt = sm[i + 1] if i + 1 < nb else pv + (pv - sm[i - 1])
        bones.append(bone_line(i, i - 1, pv, A, secondary(A, unit(nxt - pv)), spec))
    return bones, animated, code_of, nb


def process(mesh_id, spec, write=True):
    m = meshio.MeshFile(mesh_id)
    kind = spec["kind"]
    bones, animated, codes, nbones = (build_chain if kind == "chain" else build_lash)(m, spec)
    anim_ids = {id(q) for q in animated}
    rigid = [q for q in m.quads if id(q) not in anim_ids]
    order = rigid + sorted(animated, key=lambda q: q.idx)
    skin = {0: [], 1: [], 2: []}
    for q in sorted(animated, key=lambda q: q.idx):
        c = codes[id(q)]
        cs = c if isinstance(c, list) else [c] * 4
        skin[q.layer].extend(cs)
    if write:
        m.write(order, bones, skin)
    layers = {l: len(v) // 4 for l, v in skin.items() if v}
    return {"bones": nbones, "animated_quads": len(animated), "animated_per_layer": layers, "total_quads": len(m.quads)}


def check(mesh_id, spec):
    path = os.path.join(meshio.MESH_DIR, mesh_id + ".txt")
    txt = open(path, encoding="utf-8").read()
    return "\nbone 0 " in txt and "\nskin 0 " in txt


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--only")
    ap.add_argument("--dry", action="store_true")
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args(argv)
    specs = json.load(open(SPECS, encoding="utf-8"))["meshes"]
    ids = a.only.split(",") if a.only else list(specs)
    bad = 0
    for mid in ids:
        if a.check:
            ok = check(mid, specs[mid])
            print("%-40s %s" % (mid, "ok" if ok else "MISSING"))
            bad += 0 if ok else 1
            continue
        r = process(mid, specs[mid], write=not a.dry)
        print("%-40s %s" % (mid, r))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
