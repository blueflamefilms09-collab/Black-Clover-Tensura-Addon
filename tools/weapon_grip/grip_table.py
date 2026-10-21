"""
grip_table: writes assets/nusmp/weapon_grips.json (the per-weapon half of the grip table) and tools/weapon_grip/grip_golden.json (what the finder gives for every mesh, which
client/relics/item/GripFinder.java is tested against in GripTableTest).    docs/weapon_grip.md

    python tools/weapon_grip/grip_table.py            # print what would change
    python tools/weapon_grip/grip_table.py --write    # rewrite both files
    python tools/weapon_grip/grip_table.py --check    # exit 1 if a file is out of date (CI)
    python tools/weapon_grip/grip_table.py --list     # one line per mesh: class, fraction, source

The table has three parts besides the finder (see GripFinder.java for the order of sources):
  classOf    the grip class of a weapon that weapon_classes.json does not list (the Djinn weapons: their kind -> class)
  points     the grip point itself (mesh space) of the upgraded meshes (weapon_meshes_vu), whose effect groups would blur a profile; found from the static body, then frozen here
  fractions  per-weapon fractions (OVERRIDES below), for a mesh whose handle the finder cannot read; each was checked by eye on the sheets of docs/weapon_grip/
A new weapon mesh must be known to weapon_classes.json or to `classOf` (add it to DJINN_KIND / EXTRA_CLASS below, or run tools/relics/audit_weapon_scale.py --write): the test
GripTableTest fails for a mesh with no grip class, so nothing ships with only a keyword guess.
"""
import argparse
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import grip_lib as gl  # noqa: E402

GRIPS = os.path.join(gl.ASSETS, "weapon_grips.json")
GOLDEN = os.path.join(HERE, "grip_golden.json")

# the Djinn weapons (docs/djinn_weapons.md, tools/relics/djinn36_weapon_data.py): kind -> grip class. Two-handed pairs and fans are held as one object at their middle.
KIND_CLASS = {
    "lance": "spear", "harpoon": "spear", "spear": "spear", "claymore": "greatsword", "greatsword": "greatsword", "daggers": "handitem", "bow": "bow", "rapier": "rapier",
    "anchor": "hammer", "fans": "handitem", "staff": "staff", "pincers": "handitem", "sabre": "sabre", "claws": "handitem", "cleaver": "cleaver", "halberd": "glaive",
    "glaive": "glaive", "trident": "trident", "longsword": "longsword", "mace": "mace", "axe": "battleaxe", "horn": "club", "scythe": "glaive",
}
DJINN_KIND = {
    "baal": "lance", "amon": "greatsword", "vinea": "trident", "focalor": "glaive", "zagan": "staff", "zepar": "axe", "furfur": "horn", "marchosias": "claws", "sitri": "bow",
    "bifron": "glaive", "paimon": "glaive", "vassago": "scythe",
    "bael": "claymore", "agares": "harpoon", "valefor": "daggers", "barbatos": "bow", "gusion": "rapier", "vepar": "anchor", "shax": "fans", "buer": "staff", "andrealphus": "lance",
    "kimaris": "lance", "amdusias": "rapier", "belial": "greatsword", "decarabia": "pincers", "seere": "sabre", "dantalion": "staff", "andromalius": "spear",
    "glasya_labolas": "claws", "haagenti": "cleaver", "crocell": "staff", "furcas": "halberd", "balam": "trident", "alloces": "longsword", "camio": "staff", "murmur": "mace",
}
EXTRA_CLASS = {"demon_slayer_sword": "greatsword"}          # id -> class for any other mesh weapon_classes.json does not list

# per-weapon fractions (share of the length from the butt) and points the finder cannot find; filled from the sheets of docs/weapon_grip/
OVERRIDES = {
    # swords whose handle the finder cannot read (no guard of a different width): the hilt is in the lowest tenth, read off the sheets
    "jade_xiphos": 0.09, "rainbow_rapier": 0.08, "spell_forged_rapier": 0.12, "wavebreaker": 0.08, "whirling_blades": 0.08,
    "elsdocia": 0.12,                                            # the old mesh; the upgraded one has its point in `points`
    "djinn_weapon_haagenti": 0.15, "djinn_weapon_haagenti_dark": 0.15,                       # a cleaver on a long handle with a gourd pommel
    "gauches_hand_mirror": 0.18, "spirit_king_sceptre": 0.30, "vassal_fan": 0.12, "glutton_chalice": 0.30,                   # held things: by the handle, the shaft, the stem
    "fairy_glitter_gauntlets": 0.45, "fairy_heart_gauntlets": 0.45,                          # a pair of gauntlets: held in the middle
}
POINT_OVERRIDES = {"demon_slayer_sword": (0.0, -12.0, 0.0)}      # the middle of the cloth grip (hilt() of DemonSlayerMesh: y -22 to -2)


def mesh_files():
    """[(key, id, path, kind)]: key is the id, or sz/<id> for the thickened copies, vu/<id> for the upgraded meshes."""
    out = []
    for d, prefix, kind in (("weapon_meshes", "", "old"), ("weapon_meshes_sz", "sz/", "old"), ("weapon_meshes_vu", "vu/", "vu"), ("weapon_meshes_vu_sz", "vu_sz/", "vu"),
                            ("weapon_meshes/up", "up/", "old"), ("weapon_meshes_sz/up", "sz_up/", "old")):      # up/: the upgraded God melee and dark Djinn meshes (WeaponUpgrade)
        for f in sorted(glob.glob(os.path.join(gl.ASSETS, d, "*.txt"))):
            i = os.path.basename(f)[:-4]
            out.append((prefix + i, i, f, kind))
    return out


def djinn_classes():
    out = {}
    for name, kind in DJINN_KIND.items():
        cls = KIND_CLASS[kind]
        out["djinn_weapon_" + name] = cls
        out["djinn_weapon_" + name + "_dark"] = cls
    return out


def lod_base(item_id):
    """A level-of-detail mesh (<id>_lod) and an orbiting piece (<id>_orbit) are the same weapon as <id>."""
    for suffix in ("_lod", "_orbit"):
        if item_id.endswith(suffix):
            return item_id[:-len(suffix)]
    return item_id


def listed(item_id, classes_json):
    item_id = lod_base(item_id)
    w = classes_json["weapons"]
    return item_id in w or gl.STATE_SUFFIX.sub("", item_id) in w


def build():
    cj = gl.classes()
    class_of = {}
    djinn = djinn_classes()
    class_of.update(djinn)
    class_of.update(EXTRA_CLASS)
    table_now = {"classOf": class_of, "fractions": dict(OVERRIDES), "points": {}}
    golden = {}
    points = {}
    for pass_no in (0, 1):                      # pass 0 freezes the points of the full upgraded meshes; pass 1 writes every mesh (a lod or thickened copy uses its full mesh's point)
        for key, i, path, kind in mesh_files():
            full_vu = kind == "vu" and key.startswith("vu/") and not key.endswith("_lod")
            if pass_no == 0 and not full_vu:
                continue
            place, directive, xyz, body = gl.load_mesh(path)
            if not body:
                continue
            cls = gl.grip_class(lod_base(i), cj, table_now)
            row = cj["classes"].get(cls) or {"grip": 0.5}
            base = gl.STATE_SUFFIX.sub("", lod_base(i))
            ov = OVERRIDES.get(lod_base(i), OVERRIDES.get(base))
            if kind == "vu":
                explicit = POINT_OVERRIDES.get(lod_base(i)) or points.get(lod_base(i))
            else:
                explicit = xyz
            gx, gy, gz, frac, src = gl.grip_point(body, cls, row["grip"], override=ov, directive=directive, explicit=explicit)
            if pass_no == 0:
                points[i] = [round(gx, 3), round(gy, 3), round(gz, 3)]
            else:
                golden[key] = {"class": cls, "fraction": round(frac, 3), "source": src, "x": round(gx, 3), "y": round(gy, 3), "z": round(gz, 3)}
        if pass_no == 0:
            table_now["points"] = points
    table_now["points"] = points
    needed = {}
    for key, i, path, kind in mesh_files():
        if not listed(i, cj) and lod_base(i) not in class_of and gl.STATE_SUFFIX.sub("", lod_base(i)) not in class_of:
            needed[i] = key
    return table_now, golden, needed


def dump(obj):
    return json.dumps(obj, indent=1, sort_keys=True) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--write", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--list", action="store_true")
    args = ap.parse_args(argv)
    table, golden, needed = build()
    if needed:
        print("meshes with no grip class (add them to weapon_classes.json or EXTRA_CLASS):", ", ".join(sorted(needed)))
    table_out = {"version": 1, "_doc": "assets/nusmp/weapon_grips.json, written by tools/weapon_grip/grip_table.py (docs/weapon_grip.md): classOf = grip class of weapons "
                 "weapon_classes.json does not list; points = grip point (mesh space) of the upgraded meshes; fractions = per-weapon grip fraction (share of the length from the butt)",
                 "classOf": table["classOf"], "fractions": table["fractions"], "points": table["points"]}
    if args.list:
        for key, g in sorted(golden.items()):
            print("%-44s %-12s %.3f %s" % (key, g["class"], g["fraction"], g["source"]))
    changed = []
    for path, obj in ((GRIPS, table_out), (GOLDEN, golden)):
        text = dump(obj)
        old = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if old != text:
            changed.append(os.path.relpath(path, ROOT))
            if args.write:
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(text)
    print("%d meshes; %s" % (len(golden), ("rewritten: " if args.write else "out of date: ") + ", ".join(changed) if changed else "files up to date"))
    return 1 if (args.check and (changed or needed)) else 0


if __name__ == "__main__":
    sys.exit(main())
