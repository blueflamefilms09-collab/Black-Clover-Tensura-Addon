#!/usr/bin/env python3
"""flat_parts_audit: finds the parts of the weapon meshes that are drawn flat (a card, a single-sided plane, a hairline) where a solid 3D part belongs.

Reads src/main/resources/assets/anime_multiverse/weapon_meshes/*.txt (the format client/WeaponRenderer.parse reads: `place`, `orient`, `grip`, `glow`,
`# part <name> <first quad> <quad count>` labels and `q` / `qn` quad lines) and, for every named part, measures the thickness of its quads along the
thinnest principal axis. A part is FLAT when that thickness is below FLAT_BLOCKS (in blocks, via the `place` scale) or below FLAT_RATIO of its
second-thinnest extent. A part of the kind that has to be solid (FAMILIES: the head of a spear / harpoon, the arrow nocked on a bow) is also THIN when
it is under MIN_FRACTION of the weapon's longest extent (a leaf blade as thin as paper, an arrow as thin as a hair). FLAT or THIN on such a part is a finding.
Pure Python 3 + numpy; no Blender.

    python3 tools/heroweapon/flat_parts_audit.py            # report of the hero weapons (legendary_*, vassal_*)
    python3 tools/heroweapon/flat_parts_audit.py --all      # every mesh
    python3 tools/heroweapon/flat_parts_audit.py --json     # machine readable
    python3 tools/heroweapon/flat_parts_audit.py --check    # exit 1 when a must-be-solid part is flat or thin (the unit test runs this)
"""
import argparse
import glob
import json
import os
import re
import sys

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
MESHES = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "weapon_meshes")

FLAT_BLOCKS = 0.004          # thinner than this (blocks, about a quarter of a pixel of the 16 px item grid) is a card
FLAT_RATIO = 0.06            # or thinner than this share of the part's second extent
MIN_FRACTION = 0.02          # a part that has to be solid is THIN below this share of the weapon's longest extent
# the parts that have to be a solid, per family of mesh ids: the head of a spear / harpoon, and the arrow nocked on a bow in its draw stages
FAMILIES = (
    (re.compile(r"^(legendary_spear|vassal_harpoon)"), re.compile(r"^(spearhead|head_blade|head_barbs|head_prongs)$")),
    (re.compile(r"^legendary_bow.*_pull[123]$"), re.compile(r"^arrow_(shaft|stem|head|fletch)$")),
)


def read_mesh(path):
    """-> (scale, parts {name: (first, count)}, quads [ (layer, 4x3 array) ]) in file order."""
    scale, parts, quads = 1.0, {}, []
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v:
                continue
            if v[0] == "place" and len(v) > 4:
                scale = float(v[4])
            elif v[0] == "#" and len(v) > 4 and v[1] == "part":
                parts[v[2]] = (int(v[3]), int(v[4]))
            elif v[0] in ("q", "qn"):
                stride = 5 if v[0] == "q" else 8
                pts = np.array([[float(v[2 + k * stride + j]) for j in range(3)] for k in range(4)])
                quads.append((int(v[1]), pts))
    return scale, parts, quads


def thickness(pts):
    """-> (thinnest extent, second extent, third extent) of a point set along its principal axes (mesh units)."""
    p = pts - pts.mean(axis=0)
    if len(p) < 3:
        return 0.0, 0.0, 0.0
    _, vec = np.linalg.eigh(np.cov(p.T))
    ext = sorted((p @ vec[:, i]).max() - (p @ vec[:, i]).min() for i in range(3))
    return tuple(ext)


def audit_mesh(path):
    scale, parts, quads = read_mesh(path)
    mid = os.path.splitext(os.path.basename(path))[0]
    solid_re = next((r for (m, r) in FAMILIES if m.search(mid)), None)
    body_all = np.vstack([q[1] for q in quads if q[0] == 0] or [np.zeros((1, 3))])
    longest = float((body_all.max(axis=0) - body_all.min(axis=0)).max() * scale) or 1.0
    rows = []
    for name, (first, count) in parts.items():
        qs = quads[first:first + count]
        body = [q[1] for q in qs if q[0] == 0] or [q[1] for q in qs]          # the skin layer; the glow copy has the same shape
        if not body:
            continue
        pts = np.vstack(body)
        t, m, _ = thickness(pts)
        tb = t * scale
        flat = bool(tb < FLAT_BLOCKS or (m > 0 and t < FLAT_RATIO * m))
        must = bool(solid_re and solid_re.search(name))
        thin = bool(must and tb < MIN_FRACTION * longest)
        rows.append({"part": name, "quads": len(body), "thickness_blocks": round(float(tb), 4), "ratio": round(float(t / m), 3) if m else 0.0,
                     "share_of_length": round(float(tb / longest), 4), "flat": flat, "thin": thin, "must_be_solid": must})
    return rows


def hero_meshes(all_meshes=False):
    out = sorted(glob.glob(os.path.join(MESHES, "*.txt")))
    if all_meshes:
        return out
    return [p for p in out if os.path.basename(p).startswith(("legendary_", "vassal_"))]


def findings(all_meshes=False):
    """-> [(mesh id, part)] of the must-be-solid parts that are flat or thin."""
    out = []
    for p in hero_meshes(all_meshes):
        mid = os.path.splitext(os.path.basename(p))[0]
        out += [(mid, r["part"]) for r in audit_mesh(p) if r["must_be_solid"] and (r["flat"] or r["thin"])]
    return out


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--flat-only", action="store_true")
    a = ap.parse_args(argv)
    report, bad = {}, []
    for p in hero_meshes(a.all):
        mid = os.path.splitext(os.path.basename(p))[0]
        rows = audit_mesh(p)
        report[mid] = rows
        bad += [(mid, r["part"]) for r in rows if r["must_be_solid"] and (r["flat"] or r["thin"])]
    if a.json:
        print(json.dumps(report, indent=1))
    else:
        for mid, rows in report.items():
            shown = [r for r in rows if r["flat"] or r["thin"]]
            if a.flat_only and not shown:
                continue
            print(f"{mid}: {len(rows)} parts, {len([r for r in rows if r['flat']])} flat, {len([r for r in rows if r['thin']])} thin")
            for r in shown:
                tag = ("  <-- MUST BE SOLID: " + ("FLAT" if r["flat"] else "THIN")) if r["must_be_solid"] else ""
                print(f"    {r['part']:<22} quads={r['quads']:<4} thickness={r['thickness_blocks']:.4f} blocks ({100 * r['share_of_length']:.1f}% of the weapon) ratio={r['ratio']:.3f}{tag}")
        print(f"\n{len(report)} meshes, {len(bad)} must-be-solid parts are flat or paper-thin")
    if a.check and bad:
        for mid, part in bad:
            print(f"FLAT/THIN: {mid} / {part}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
