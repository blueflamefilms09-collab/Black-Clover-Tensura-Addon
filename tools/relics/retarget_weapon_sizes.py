"""retarget_weapon_sizes: rebuilds tools/relics/weapon_fixes.json for the class table of the day (1.34.0 size rule, docs/weapon_scale_orientation.md section 10).

    python tools/relics/retarget_weapon_sizes.py            dry run: the proposed sizes (before -> after, class range)
    python tools/relics/retarget_weapon_sizes.py --write    rewrite weapon_fixes.json (size and head entries); then run apply_weapon_fixes.py --apply

The owner's rule: unless a weapon should be small (a dagger), make it bigger; a great sword is massive. For every base id (a state mesh or the left half of a
pair follows its rest mesh) it takes the mesh AS THE GENERATOR WROTE IT plus the orientation fixes already in the table (flip_x, flip_y, rotate_z, orient_manual:
those are kept), measures the apparent length (mesh length x the model json's third-person scale), and
  * a weapon below its class minimum is brought to  min + TARGET_FRACTION * (max - min)  (an `apparent` entry; `scale` for an id with several mesh files, so
    every state of a bow or a pair half grows by the same factor);
  * a weapon already inside its class range or above it is left alone (a huge sword is a design choice; nothing is shrunk);
  * a held item (class handitem / item) under ITEM_MIN is raised to ITEM_TARGET by its largest dimension (an `extent` entry);
  * a class with a head minimum (axes, hammers) whose head is narrower than it after the new size gets a `head` entry that grows it to HEAD_TARGET_FACTOR x the
    minimum (the bisection of apply_weapon_fixes.solve_head_k).
Entries of other keys (grip, note) are dropped and rewritten; the ids of other agents (djinn_weapon_*) are never touched.
"""
import argparse
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import apply_weapon_fixes as af      # noqa: E402
import weapon_classes as wc          # noqa: E402
import weapon_orientation as wo      # noqa: E402

TARGET_FRACTION = 0.40               # a weapon below its range is raised to this share of the range (toward its lower middle)
TARGET_OVERRIDE = {                  # boss / legendary weapons that should sit higher in their range
    "elsdocia": 0.55, "rimeheart_runeblade": 0.55, "severing_greatsword": 0.55, "demon_destroyer_sword": 0.55, "licht_destroyer_sword": 0.55,
    "doom_hammer": 1.0, "hunger": 0.55, "mountainguards_greataxe": 0.50, "buster_sword": 0.50,
}
ITEM_MIN = 0.50                      # a held item below this largest dimension is raised
ITEM_TARGET = 0.60
HEAD_TARGET_FACTOR = 1.12
KEEP = ("flip_x", "flip_y", "rotate_z", "orient_manual", "grip", "grip_at")
SKIP = af.SKIP_PREFIXES


def group_files(meshes_dir):
    """base id -> [mesh ids] (the rest mesh first)."""
    groups = {}
    for p in sorted(glob.glob(os.path.join(meshes_dir, "*.txt"))):
        i = os.path.basename(p)[:-4]
        if i.startswith(SKIP):
            continue
        groups.setdefault(wc.base_id(i), []).append(i)
    for b, ids in groups.items():
        ids.sort(key=lambda i: (i != b, i))
    return groups


def measure(text, item_id, fix, data):
    new, _rep = af.apply_to_text(text, item_id, fix, data)
    lines, quads = af.parse_lines(new)
    _pi, place = af.place_of(lines)
    s3p = af.s3p_of(item_id)
    m = wo.measure(af.to_quads(quads), place[3])
    return {"apparent": af.game_length(quads, place) * s3p, "extent": af.extent_of(quads, place) * s3p, "head": m.max_width * s3p, "s3p": s3p}


def solve_head(text, item_id, base, target, data):
    lo, hi = 1.0, 3.2
    if measure(text, item_id, dict(base, head={"k": hi}), data)["head"] < target:
        return hi
    for _ in range(14):
        mid = (lo + hi) / 2
        if measure(text, item_id, dict(base, head={"k": mid}), data)["head"] < target:
            lo = mid
        else:
            hi = mid
    return round(hi, 2)


def plan(data=None, old=None, meshes_dir=af.MESHES):
    data = data or wc.load()
    old = old if old is not None else af.load_fixes()
    tabs = wc.table(data)
    out, rows = {}, []
    for base, ids in group_files(meshes_dir).items():
        rest = ids[0]
        cls = wc.class_of(base, data)
        tab = tabs[cls]
        keep = {k: v for k, v in old.get(base, {}).items() if k in KEEP}
        text = open(os.path.join(meshes_dir, rest + ".txt"), encoding="utf-8").read()
        m0 = measure(text, rest, keep, data)
        fix = dict(keep)
        why = []
        k = 1.0
        if wc.is_item_class(cls):
            if m0["extent"] < ITEM_MIN * 0.999:
                if len(ids) > 1:
                    k = ITEM_TARGET / m0["extent"]
                    fix["scale"] = round(k, 5)
                else:
                    fix["extent"] = ITEM_TARGET
                    k = ITEM_TARGET / m0["extent"]
                why.append("item %.2f -> %.2f" % (m0["extent"], ITEM_TARGET))
        elif m0["apparent"] < tab["min"] * 1.03:           # (a weapon at the very edge of its range is raised too)
            f = TARGET_OVERRIDE.get(base, TARGET_FRACTION)
            target = round(tab["min"] + f * (tab["max"] - tab["min"]), 3)
            k = target / m0["apparent"]
            if len(ids) > 1:
                fix["scale"] = round(k, 5)
            else:
                fix["apparent"] = target
            why.append("%s %.2f -> %.2f (range %.2f-%.2f)" % (cls, m0["apparent"], target, tab["min"], tab["max"]))
        head_min = tab.get("head_min", 0.0)
        head_after = m0["head"] * k
        if head_min and head_after < head_min * 1.02:
            kk = solve_head(text, rest, fix, head_min * HEAD_TARGET_FACTOR, data)
            fix["head"] = {"k": kk}
            why.append("head %.2f -> x%.2f (min %.2f)" % (head_after, kk, head_min))
        if len(fix) > len(keep) or keep:
            if why:
                fix["note"] = "1.34.0: " + "; ".join(why)
            elif "note" in old.get(base, {}):
                fix["note"] = old[base]["note"]
            out[base] = fix
        rows.append((base, cls, m0, fix, why))
    return out, rows


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--write", action="store_true")
    a = ap.parse_args(argv)
    data = wc.load()
    out, rows = plan(data)
    for base, cls, m0, fix, why in rows:
        if why:
            print("%-32s %-11s %s" % (base, cls, "; ".join(why)))
    print("%d ids, %d with an entry" % (len(rows), len(out)))
    if a.write:
        cur = json.load(open(af.FIXES_JSON, encoding="utf-8")) if os.path.exists(af.FIXES_JSON) else {}
        doc = cur.get("_doc", "")
        new = {"_doc": doc}
        for k in sorted(out):
            new[k] = out[k]
        json.dump(new, open(af.FIXES_JSON, "w", encoding="utf-8", newline="\n"), indent=1)
        open(af.FIXES_JSON, "a").write("\n")
        print("wrote", os.path.relpath(af.FIXES_JSON, af.ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
