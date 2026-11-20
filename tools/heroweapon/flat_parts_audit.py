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
    python3 tools/heroweapon/flat_parts_audit.py --md docs/heroweapon/flat_parts_audit.md   # the full list of every mesh with a flat part (4.55.0), with counts

4.55.0: `--md` also measures the ORIGINAL meshes kept in weapon_meshes/flat_parts_old (the before), tells a mesh the Blender builders made solid from one the Blender-less
solidify script patched (a Blender build carries a `# blender-solid` header line), and lists every other part of every mesh that is a sheet (thinner than SHEET_BLOCKS
and wider than SHEET_WIDE), split into thin by design (strings, fullers, rivets, ...) and candidates.
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


SHEET_BLOCKS, SHEET_WIDE = 0.004, 0.02      # a part thinner than this and wider than that is a sheet (a card); thinner and narrower is a hairline
# parts that are thin on purpose (a string, a fuller groove, a rivet head, an edge line): reported, not asked for a redo
BY_DESIGN = re.compile(r"^(string|strings|fuller_[fb]|head_fuller_[fb]|riser_rivets-?\d*|blade_edge|blade_ribs|edge_line.*|limb_blades|paper|portal|pg_studs|wisp_[ab]_wings|pinwheel|vane_\d+)$")


def sheets(path):
    """-> [(part, quads, thickness blocks, width blocks)] of the sheet-like parts of one mesh (absolute thickness, no ratio: curved rims and rings are not sheets)."""
    scale, parts, quads = read_mesh(path)
    out = []
    for name, (first, count) in parts.items():
        body = [q[1] for q in quads[first:first + count] if q[0] == 0] or [q[1] for q in quads[first:first + count]]
        if not body:
            continue
        t, m, _ = thickness(np.vstack(body))
        if t * scale < SHEET_BLOCKS and m * scale >= SHEET_WIDE:
            out.append((name, len(body), round(float(t * scale), 4), round(float(m * scale), 3)))
    return out


def made_by(path):
    """'Blender' when the mesh carries the `# blender-solid` line the installer (tools/heroweapon/install_solid_meshes.py) writes after a Blender build, else 'script' (the Blender-less
    solidify script of 4.5x, or an original)."""
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("# blender-solid"):
                return "Blender"
            if line.startswith(("q ", "qn ")):
                break
    return "script"


def write_md(out):
    """The audit as a document: the 44 meshes with must-be-solid parts (before / now / who made them) and every other sheet-like part of every mesh."""
    old_dir = os.path.join(MESHES, "flat_parts_old")
    all_paths = sorted(glob.glob(os.path.join(MESHES, "*.txt")))
    nparts = 0
    for p in all_paths:
        nparts += len(read_mesh(p)[1])
    fam = []                                                    # (id, before rows, now rows, maker)
    for p in sorted(glob.glob(os.path.join(old_dir, "*.txt"))):
        mid = os.path.splitext(os.path.basename(p))[0]
        cur = os.path.join(MESHES, mid + ".txt")
        b = [r for r in audit_mesh(p) if r["must_be_solid"]]
        n = [r for r in audit_mesh(cur) if r["must_be_solid"]]
        fam.append((mid, b, n, made_by(cur)))
    bad_before = [f for f in fam if any(r["flat"] or r["thin"] for r in f[1])]
    bad_now = [f for f in fam if any(r["flat"] or r["thin"] for r in f[2])]
    script = [f for f in fam if f[3] == "script"]
    sh_rows = []
    for p in all_paths:
        mid = os.path.splitext(os.path.basename(p))[0]
        for r in sheets(p):
            sh_rows.append((mid,) + r)
    design = [r for r in sh_rows if BY_DESIGN.match(r[1])]
    cand = [r for r in sh_rows if not BY_DESIGN.match(r[1])]
    L = ["# Flat-part audit (every weapon mesh)", "",
         "Made by `python3 tools/heroweapon/flat_parts_audit.py --md docs/heroweapon/flat_parts_audit.md` (pure Python 3 + numpy). Each named part of each mesh is measured along its thinnest principal axis",
         "(PCA of the body-layer quads, times the `place` scale, so the numbers are blocks). Two rules:", "",
         "- **must be solid** (the heads of spears and harpoons, the arrow of every bow draw stage): FLAT below %.3f blocks or below %.0f %% of the part's second extent, THIN below %.0f %% of the weapon's length;" % (FLAT_BLOCKS, 100 * FLAT_RATIO, 100 * MIN_FRACTION),
         "- **sheet** (every other part): thinner than %.3f blocks and wider than %.2f blocks. Thinner and narrower is a hairline (strings, rivet heads)." % (SHEET_BLOCKS, SHEET_WIDE), "",
         "## Counts", "",
         "| What | Count |", "| --- | --- |",
         "| meshes measured | %d |" % len(all_paths), "| named parts measured | %d |" % nparts,
         "| meshes with a must-be-solid part (spear / harpoon head, bow arrow) | %d |" % len(fam),
         "| of those, flat or paper-thin in the ORIGINAL (`flat_parts_old`) | %d |" % len(bad_before),
         "| of those, flat or paper-thin NOW | %d |" % len(bad_now),
         "| of those, patched by the Blender-less script (no `# blender-solid` line), not modelled in Blender | %d |" % len(script),
         "| of those, rebuilt solid in Blender | %d |" % (len(fam) - len(script)),
         "| other sheet-like parts, thin by design (strings, fullers, rivets, edge lines, ...) | %d parts in %d meshes |" % (len(design), len({r[0] for r in design})),
         "| other sheet-like parts, candidates for a later redo | %d parts in %d meshes |" % (len(cand), len({r[0] for r in cand})), "",
         "## The 44 meshes with a must-be-solid part", "",
         "| Mesh | Part | Original: thickness (blocks) / share of length | Now | Made by |", "| --- | --- | --- | --- | --- |"]
    for mid, b, n, who in fam:
        for rb, rn in zip(b, n):
            L.append("| `%s` | %s | %.4f / %.1f %% | %.4f / %.1f %% | %s |" % (mid, rb["part"], rb["thickness_blocks"], 100 * rb["share_of_length"], rn["thickness_blocks"], 100 * rn["share_of_length"], who))
    L += ["", "## Other sheet-like parts: candidates", "", "These are parts of other meshes (not the arrow / spear head families) that are thinner than a quarter of a pixel and wider than a card: bow limb panels, fins and",
          "streamers, a shield ring. Not redone in this pass; the Blender builders of those bows would need a thicker section.", "",
          "| Mesh | Part | Quads | Thickness (blocks) | Width (blocks) |", "| --- | --- | --- | --- | --- |"]
    for r in cand:
        L.append("| `%s` | %s | %d | %.4f | %.3f |" % r)
    L += ["", "## Other sheet-like parts: thin by design", "", "Strings, fuller grooves, rivet heads, edge lines and the like. A string that is a pixel thick is correct; listed so the count is honest.", "",
          "| Part kind | Parts | Meshes |", "| --- | --- | --- |"]
    kinds = {}
    for r in design:
        k = re.sub(r"-?\d+$", "", r[1])
        kinds.setdefault(k, []).append(r[0])
    for k, v in sorted(kinds.items(), key=lambda kv: -len(kv[1])):
        L.append("| %s | %d | %d |" % (k, len(v), len(set(v))))
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(L) + "\n")
    print("wrote %s: %d meshes with the families (%d flat before, %d now, %d script-made), %d candidates, %d by design" % (out, len(fam), len(bad_before), len(bad_now), len(script), len(cand), len(design)))


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
    ap.add_argument("--md", metavar="OUT", help="write the full audit (before / now / who made it, every sheet-like part) as a markdown file")
    a = ap.parse_args(argv)
    if a.md:
        write_md(a.md)
        return 0
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
