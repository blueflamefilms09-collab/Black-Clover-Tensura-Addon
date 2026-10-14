"""audit_weapon_scale: the size and orientation audit of every baked weapon mesh (docs/weapon_scale_orientation.md).

    python tools/relics/audit_weapon_scale.py                       the table: every mesh, its class, its apparent size against the class range, flags
    python tools/relics/audit_weapon_scale.py --problems            only the meshes that are too small / too large / flagged
    python tools/relics/audit_weapon_scale.py --json out.json       the table as json
    python tools/relics/audit_weapon_scale.py --sheet out.png [--only problems|all] [ids ...]   the contact sheet (front + held next to Steve)
    python tools/relics/audit_weapon_scale.py --write               copy the class table to assets/nusmp/weapon_classes.json (the game's copy)
    python tools/relics/audit_weapon_scale.py --check               exit 1 when the two class files differ or a mesh has no reviewed class

For every assets/nusmp/weapon_meshes/<id>.txt it places the mesh the way WeaponRenderer does (place line), takes the third-person display scale of the
model json (a state mesh uses its base id's), and compares the APPARENT length (mesh length x display scale, in blocks) with the class table of
tools/relics/weapon_classes.json (min / max, and `head_min` for an axe or hammer head). It also runs the orientation detector
(tools/relics/weapon_orientation.py): tip down, head toward the player, mirrored. The fixes are tools/relics/weapon_fixes.json, applied by
tools/relics/apply_weapon_fixes.py.
"""
import argparse
import glob
import json
import math
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import weapon_classes as wc          # noqa: E402
import weapon_orientation as wo      # noqa: E402

ROOT = wc.ROOT
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MESHES = os.path.join(ASSETS, "weapon_meshes")
MODELS = os.path.join(ASSETS, "models", "item")
SKIP_PREFIXES = ("djinn_weapon_",)
SMALL_TOL = 0.97                     # below this share of the class minimum a weapon is "small"
RUNTIME_SMALL = 0.60                 # the runtime normaliser's trigger (WeaponClasses.RUNTIME_SMALL)


def third_person_scale(item_id, models_dir=MODELS):
    for i in (item_id, wc.base_id(item_id)):
        p = os.path.join(models_dir, i + ".json")
        if os.path.exists(p):
            try:
                s = json.load(open(p, encoding="utf-8")).get("display", {}).get("thirdperson_righthand", {}).get("scale")
                if s:
                    return float(s[0])
            except (ValueError, OSError):
                pass
    return 0.85


def audit_one(item_id, data, meshes_dir=MESHES, models_dir=MODELS):
    mesh = wo.read_mesh_file(os.path.join(meshes_dir, item_id + ".txt"))
    if mesh["place"] is None or not mesh["quads"]:
        return None
    place = mesh["place"]
    sc = place[3]
    m = wo.measure(mesh["quads"], sc)
    if m.empty:
        return None
    cls = wc.class_of(item_id, data)
    tab = wc.table(data)[cls]
    s3 = third_person_scale(item_id, models_dir)
    quads = wo.body_quads(mesh["quads"])
    xs = [p[0] for _l, P, _n in quads for p in P]
    zs = [p[2] for _l, P, _n in quads for p in P]
    ymax_all = max(pt[1] for _l, P, _n in mesh["quads"] for pt in P)
    game_len = (ymax_all - place[4]) * sc                       # WeaponRenderer.mesh: (ymax over every layer - the place ymin) * scale
    extent = max(game_len, (max(xs) - min(xs)) * sc, (max(zs) - min(zs)) * sc) * s3
    apparent = game_len * s3
    head = m.max_width * s3
    gd = mesh["directives"].get("grip")
    gd = float(gd[0]) if gd else None
    flags = wo.detect(m, cls, place[2], gd)
    if item_id.endswith("_left"):                                # the left half of a pair is the right half mirrored: its curve bends the other way on purpose
        flags = [f for f in flags if f != wo.FLAG_MIRRORED]
    status = []
    if wc.is_item_class(cls):
        if extent < data.get("item_min_extent", wc.ITEM_MIN_EXTENT) * SMALL_TOL:
            status.append("small")
    else:
        if apparent < tab["min"] * SMALL_TOL:
            status.append("small")
        if apparent > tab["max"] * wc.OVERSIZE_FACTOR:
            status.append("large")
        if tab.get("head_min", 0) and head < tab["head_min"] * SMALL_TOL:
            status.append("head_small")
    return {"id": item_id, "class": cls, "s3p": s3, "length": round(game_len, 3), "apparent": round(apparent, 3), "extent": round(extent, 3),
            "head": round(head, 3), "min": tab["min"], "max": tab["max"], "head_min": tab.get("head_min", 0), "centroid": round(m.centroid, 2),
            "imbalance": round(m.imbalance, 3), "curve": round(m.curve, 3), "angle": place[2], "flags": flags, "status": status,
            "runtime_small": not wc.is_item_class(cls) and apparent < tab["min"] * RUNTIME_SMALL,
            "grip_at": wo.grip_at(m, wo.TRAITS.get(cls, set()), gd), "grip_directive": gd, "top_section": round(m.top_section, 3), "widest_at": round(m.widest_at, 2)}


def audit_all(data=None, meshes_dir=MESHES, models_dir=MODELS, include_excluded=False):
    data = data or wc.load()
    out = []
    for p in sorted(glob.glob(os.path.join(meshes_dir, "*.txt"))):
        i = os.path.basename(p)[:-4]
        if i.startswith(SKIP_PREFIXES):                          # 1.34.0: the Djinn temporary weapons belong to another agent / release
            continue
        if not include_excluded and is_excluded(i, data):
            continue
        r = audit_one(i, data, meshes_dir, models_dir)
        if r:
            out.append(r)
    return out


def is_excluded(item_id, data):
    """Meshes other agents own (Elsdocia and its legacy mesh, the dual-wield pairs and their left / right meshes): never edited here."""
    return any(item_id == e or item_id.startswith(e + "_") for e in data.get("excluded", {}))


def excluded_ids(data):
    return sorted(data.get("excluded", {}))


def print_table(rows, problems_only=False):
    print("%-34s %-10s %5s %5s %5s %-11s %5s  %-24s %s" % ("id", "class", "L", "s3p", "app", "range", "head", "status", "flags"))
    for r in sorted(rows, key=lambda r: (r["class"], r["id"])):
        if problems_only and not (r["status"] or r["flags"]):
            continue
        rng = "%.2f-%.2f" % (r["min"], r["max"])
        print("%-34s %-10s %5.2f %5.2f %5.2f %-11s %5.2f  %-24s %s" % (r["id"], r["class"], r["length"], r["s3p"], r["apparent"], rng, r["head"],
                                                                        ",".join(r["status"]), ",".join(r["flags"])))


def make_sheet(rows, out, data, meshes_dir=None):
    from weapon_sheet import tile, sheet
    tiles = []
    meshes_dir = meshes_dir or MESHES
    for r in rows:
        mesh = wo.read_mesh_file(os.path.join(meshes_dir, r["id"] + ".txt"))
        lines = ["%s  [%s]" % (r["id"], r["class"]),
                 "apparent %.2f blocks (target %.2f-%.2f)  head %.2f (min %.2f)" % (r["apparent"], r["min"], r["max"], r["head"], r["head_min"]),
                 "centroid %.2f  imbalance %+.3f  curve %+.3f" % (r["centroid"], r["imbalance"], r["curve"]),
                 ("FLAGS: " + ",".join(r["flags"]) if r["flags"] else "") + ("  " + ",".join(r["status"]) if r["status"] else "")]
        frame = (230, 60, 60) if (r["flags"] or "small" in r["status"] or "head_small" in r["status"]) else ((230, 200, 60) if "large" in r["status"] else None)
        tiles.append(tile(r["id"], mesh, lines, cls_range=(r["min"], r["max"]) if not wc.is_item_class(r["class"]) else None, frame=frame, s3p=r["s3p"]))
    per = 8
    base, ext = os.path.splitext(out)
    outs = []
    for n in range(0, len(tiles), per):
        outs.append(sheet(tiles[n:n + per], cols=2, out="%s_%02d%s" % (base, n // per + 1, ext)))
    return outs


def make_overview(rows, out, per=36, cols=6, size=210, meshes_dir=None):
    """Compact overview: only the front view of every weapon (tip up-right, -x left), with its id: the quick scan for upside-down meshes."""
    from weapon_sheet import front_tile, sheet
    from PIL import ImageDraw
    tiles = []
    meshes_dir = meshes_dir or MESHES
    for r in rows:
        mesh = wo.read_mesh_file(os.path.join(meshes_dir, r["id"] + ".txt"))
        mesh["s3p"] = r["s3p"]
        t = front_tile(mesh, r["id"], w=size, h=size)
        d = ImageDraw.Draw(t)
        d.text((4, 3), r["id"][:30], fill=(255, 255, 255))
        d.text((4, 15), "%s %.2f %s" % (r["class"], r["apparent"], ",".join(r["flags"])), fill=(255, 220, 120))
        tiles.append(t)
    base, ext = os.path.splitext(out)
    return [sheet(tiles[n:n + per], cols=cols, out="%s_%02d%s" % (base, n // per + 1, ext)) for n in range(0, len(tiles), per)]


def pristine_copy(dest):
    """A copy of the meshes directory with every recorded fix undone (what the generators wrote): the 'before' of a before / after sheet."""
    import apply_weapon_fixes as af
    if os.path.exists(dest):
        shutil.rmtree(dest)
    shutil.copytree(MESHES, dest)
    for p in glob.glob(os.path.join(dest, "*.txt")):
        text = open(p, encoding="utf-8").read()
        if af.MARK in text:
            new, _rep = af.apply_to_text(text, os.path.basename(p)[:-4], {})
            with open(p, "w", encoding="utf-8", newline="\n") as f:
                f.write(new)
    return dest


def before_after(outdir, data):
    """Contact sheets of every weapon with a fix, before (the generator output) and after, plus compact overviews of all meshes."""
    import apply_weapon_fixes as af
    os.makedirs(outdir, exist_ok=True)
    tmp = pristine_copy(os.path.join(outdir, "_before_meshes"))
    ids = sorted(af.load_fixes())
    out = {}
    for tag, mdir in (("before", tmp), ("after", MESHES)):
        rows = [r for r in audit_all(data, mdir) if wc.base_id(r["id"]) in ids and r["id"] == wc.base_id(r["id"])]
        out[tag + "_fixed"] = make_sheet(rows, os.path.join(outdir, tag + "_fixed.png"), data, mdir)
        allrows = audit_all(data, mdir)
        out[tag + "_overview"] = make_overview(allrows, os.path.join(outdir, tag + "_overview.png"), meshes_dir=mdir)
    shutil.rmtree(tmp)
    return out


def write_asset_copy(data):
    """The game's copy of the class table: the same json, without the review notes."""
    out = {"classes": data["classes"], "item_min_extent": data.get("item_min_extent", wc.ITEM_MIN_EXTENT),
           "weapons": {k: (v["class"] if isinstance(v, dict) else v) for k, v in data["weapons"].items()}}
    os.makedirs(os.path.dirname(wc.ASSET_JSON), exist_ok=True)
    with open(wc.ASSET_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1, sort_keys=False)
        f.write("\n")
    return wc.ASSET_JSON


def asset_copy_matches(data):
    if not os.path.exists(wc.ASSET_JSON):
        return False
    return json.load(open(wc.ASSET_JSON, encoding="utf-8")) == json.loads(json.dumps({
        "classes": data["classes"], "item_min_extent": data.get("item_min_extent", wc.ITEM_MIN_EXTENT),
        "weapons": {k: (v["class"] if isinstance(v, dict) else v) for k, v in data["weapons"].items()}}))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("ids", nargs="*")
    ap.add_argument("--problems", action="store_true")
    ap.add_argument("--json")
    ap.add_argument("--sheet")
    ap.add_argument("--overview")
    ap.add_argument("--before-after", metavar="DIR", help="write before / after contact sheets of every fixed weapon (and overviews of all) into DIR")
    ap.add_argument("--only", default="all", choices=("all", "problems"))
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args(argv)
    data = wc.load()
    if a.write:
        print("wrote", os.path.relpath(write_asset_copy(data), ROOT))
    rows = audit_all(data)
    if a.ids:
        rows = [r for r in rows if r["id"] in a.ids]
    if a.json:
        json.dump(rows, open(a.json, "w"), indent=1)
    if a.before_after:
        res = before_after(a.before_after, data)
        for k, v in res.items():
            print(k, len(v), "pages")
    if a.overview:
        print("overview pages:", make_overview(rows, a.overview))
    if a.sheet:
        sel = [r for r in rows if r["status"] or r["flags"]] if a.only == "problems" else rows
        print("sheet pages:", make_sheet(sel, a.sheet, data), len(sel), "weapons")
    elif not a.write and not a.check and not a.overview and not a.before_after:
        print_table(rows, a.problems)
        bad = [r for r in rows if r["status"] or r["flags"]]
        print("\n%d meshes, %d with a size or orientation problem; excluded (other agents): %s" % (len(rows), len(bad), ", ".join(excluded_ids(data))))
    if a.check:
        missing = [r["id"] for r in rows if wc.base_id(r["id"]) not in data["weapons"]]
        ok = asset_copy_matches(data)
        print("meshes without a reviewed class:", missing or "none", "| asset copy in sync:", ok)
        return 0 if ok and not missing else 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
