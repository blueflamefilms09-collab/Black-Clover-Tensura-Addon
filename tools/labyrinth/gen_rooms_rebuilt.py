#!/usr/bin/env python3
"""
Writes the rebuilt rooms, halls and hallways of the Mega-Dungeon (docs/labyrinth/rooms_rebuilt.md).

    python3 tools/labyrinth/gen_rooms_rebuilt.py                 # (re)writes every variant under structure/mega_r/ and the manifest
    python3 tools/labyrinth/gen_rooms_rebuilt.py hall/library_a  # only the shipped templates whose name contains this text
    python3 tools/labyrinth/gen_rooms_rebuilt.py --check         # build everything in memory, run every check, fail when a file on disk differs
    python3 tools/labyrinth/gen_rooms_rebuilt.py --list          # the templates and their generator
    python3 tools/labyrinth/gen_rooms_rebuilt.py --report out.md # a table of every variant: blocks, light, the problems

Each shipped template `mega/<folder>/<stem>` gets variants `mega_r/<folder>/<stem>_v1` .. `_v4` (same size, same sockets, same data markers) and the manifest
`data/nusmp/mega/rooms_rebuilt.json` lists them for the generator (labyrinth/mega/gen/RoomVariants). The shipped templates are never touched. Templates that are not
regular boxes (domes, caverns, rock-carved works) and the boss-chain, safe, stair, trial, trap and puzzle pieces are not rebuilt (their functions are read by other systems).
"""
import concurrent.futures
import glob
import json
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import nbt  # noqa: E402
import rr_core as C  # noqa: E402
import rr_halls as H  # noqa: E402
import rr_corridor as K  # noqa: E402
import rr_check as CK  # noqa: E402

VARIANTS = 4
FOLDERS = ("hall", "junction", "corridor", "link", "treasure")
OUT_ROOT = "mega_r"
MANIFEST = os.path.join(L.RES, "data", "nusmp", "mega", "rooms_rebuilt.json")
#: shipped templates that are not rebuilt, and why
SKIP = {
    "mega/hall/round_hall_a": "a domed round hall: its roof is the room",
    "mega/hall/round_hall_b": "a domed round hall: its roof is the room",
    "mega/hall/dwarven_forgehold_mine_works": "rock-carved works: the roof is solid rock eight layers thick",
    "mega/hall/mountain_monastery_prayer_wheel_hall": "a seven-layer roof of hanging prayer wheels",
    "mega/hall/feywild_grove_hollow_tree": "a tree: its roof is the canopy",
}


def shipped(folders=FOLDERS):
    out = []
    for f in folders:
        for p in sorted(glob.glob(os.path.join(C.STRUCT, "mega", f, "*.nbt"))):
            out.append("mega/%s/%s" % (f, os.path.basename(p)[:-4]))
    return out


def generator_of(name):
    folder = name.split("/")[1]
    if folder in ("corridor", "link") and not name.split("/")[-1].startswith("gallery"):
        return "corridor"
    return "hall"


def out_name(name, v):
    parts = name.split("/")
    return "%s/%s/%s_v%d" % (OUT_ROOT, parts[1], parts[2], v)


def out_path(name, v):
    return os.path.join(C.STRUCT, *out_name(name, v).split("/")) + ".nbt"


def build_one(name, v):
    """(Room, problems, stats): builds a variant, retrying with a lower vault if the checks object to the cost or the light."""
    gen = generator_of(name)
    last = None
    for scale, restraint in ((1.0, 0), (0.7, 0), (0.7, 1), (0.4, 1), (0.4, 2), (0.0, 2)):
        room = (K.build(name, v, scale, restraint) if gen == "corridor" else H.build(name, v, scale, restraint))
        H.finish(room)
        room.t.name = out_name(name, v)
        problems, stats = CK.check(room)
        stats["rise_scale"] = scale
        stats["restraint"] = restraint
        last = (room, problems, stats)
        if not problems:
            return last
    return last


def work(args):
    name, v = args
    t0 = time.time()
    try:
        room, problems, stats = build_one(name, v)
    except Exception as e:                 # a template the engine cannot handle is reported, not hidden
        return name, v, None, ["ENGINE: %s: %s" % (type(e).__name__, e)], {}, time.time() - t0
    data = nbt.dumps(room.t.to_nbt(), compress=True)
    stats["stand_slot"] = bool(getattr(room, "stand_slot", None))
    stats["family"] = H.family_of(name)
    return name, v, data, problems, stats, time.time() - t0


def verify():
    """Runs the contract checker of the shipped templates (mega_check.check_template) on every rebuilt file on disk: sizes, sockets on the outer layer, markers, blocks, states."""
    import mega_check as MC
    errors, warns = [], []
    known = {"vanilla": MC.vanilla_blocks(), "nusmp": MC.registered_nusmp_blocks()}
    pools = MC.check_pools(errors, warns)
    files = sorted(glob.glob(os.path.join(C.STRUCT, OUT_ROOT, "**", "*.nbt"), recursive=True))
    for p in files:
        MC.check_template(p, known, errors, warns, pools)
    for e in errors:
        print("ERROR  " + e)
    print("%d rebuilt templates, %d errors, %d warnings (the palette warnings are the shipped rooms' own non-palette architecture)" % (len(files), len(errors), len(warns)))
    return 1 if errors else 0


def write_manifest_from_disk():
    """Rebuilds data/nusmp/mega/rooms_rebuilt.json from the files under structure/mega_r (how many contiguous variants each shipped template has, which carry a stand slot), so a
    partial re-run of a few templates still leaves a consistent manifest."""
    from template import load
    variants, stands = {}, set()
    skipped = dict(SKIP)
    for name in shipped():
        if name in SKIP or generator_of(name) != "hall":
            continue
        if not C.Room(name, 0).regular:
            skipped[name] = "no flat roof layer was found (an irregular shell: the roof is part of the room)"
    for name in shipped():
        if name in skipped:
            continue
        n = 0
        while os.path.exists(out_path(name, n + 1)):
            n += 1
        if n == 0:
            continue
        variants[name] = n
        for k in range(1, n + 1):
            t = load(out_path(name, k), out_name(name, k))
            if any(str(e["nbt"].get("id", "")) == "minecraft:armor_stand" for e in t.entities):
                stands.add(name)
    text = json.dumps({"_doc": ["Written by tools/labyrinth/gen_rooms_rebuilt.py (docs/labyrinth/rooms_rebuilt.md). variants: shipped template -> number of rebuilt variants "
                                "(structure/mega_r/<folder>/<stem>_v1.._vN); stand_rooms: the rebuilt rooms that carry the single armour stand slot of the rarity roll; "
                                "skipped: shipped templates that are not rebuilt, and why."],
                       "variants": dict(sorted(variants.items())), "stand_rooms": sorted(stands), "skipped": dict(sorted(skipped.items()))}, indent=1) + "\n"
    os.makedirs(os.path.dirname(MANIFEST), exist_ok=True)
    with open(MANIFEST, "w") as f:
        f.write(text)
    print("manifest: %d templates, %d stand rooms, %d skipped" % (len(variants), len(stands), len(skipped)))
    return 0


def main(argv):
    args = list(argv)
    if "--verify" in args:
        return verify()
    if "--manifest" in args:
        return write_manifest_from_disk()
    check = "--check" in args
    listing = "--list" in args
    report = None
    if "--report" in args:
        i = args.index("--report")
        report = args[i + 1]
        del args[i:i + 2]
    keep_going = "--force" in args
    dry = "--dry" in args
    args = [a for a in args if not a.startswith("--")]
    names = [n for n in shipped() if not args or any(a in n for a in args)]
    if listing:
        for n in names:
            print("%-52s %s%s" % (n, generator_of(n), "  SKIP: " + SKIP[n] if n in SKIP else ""))
        return 0
    for n in names:
        if n not in SKIP and generator_of(n) == "hall":
            r = C.Room(n, 0)
            if not r.regular:
                SKIP[n] = "no flat roof layer was found (an irregular shell: the roof is part of the room)"
    jobs = [(n, v) for n in names if n not in SKIP for v in range(1, VARIANTS + 1)]
    results = {}
    failed = 0
    workers = max(1, min(4, os.cpu_count() or 1))
    t0 = time.time()
    with concurrent.futures.ProcessPoolExecutor(max_workers=workers) as ex:
        for name, v, data, problems, stats, dt in ex.map(work, jobs):
            results[(name, v)] = (data, problems, stats)
            tag = "ok  " if not problems else "FAIL"
            print("%s %-50s v%d x%.2f light %s/%s %.1fs%s" % (tag, name, v, stats.get("ratio", 0), stats.get("light_min", "-"), stats.get("light_median", "-"), dt,
                                                           "" if not problems else "\n      - " + "\n      - ".join(problems[:8])))
            sys.stdout.flush()
            if problems:
                failed += 1
    print("%d variant(s), %d failed, %.0fs" % (len(jobs), failed, time.time() - t0))
    if report:
        write_report(report, results)
    if dry:
        return 1 if failed else 0
    if failed and not keep_going:
        print("nothing written (use --force to write the variants that passed anyway)")
        return 1
    stale = 0
    manifest = {"variants": {}, "stand_rooms": [], "skipped": dict(SKIP)}
    for (name, v), (data, problems, stats) in sorted(results.items()):
        if problems or data is None:
            continue
        manifest["variants"][name] = max(manifest["variants"].get(name, 0), v)
        if stats.get("stand_slot") and name not in manifest["stand_rooms"]:
            manifest["stand_rooms"].append(name)
        p = out_path(name, v)
        if check:
            if not os.path.exists(p) or open(p, "rb").read() != data:
                print("STALE %s" % p)
                stale += 1
            continue
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "wb") as f:
            f.write(data)
    text = json.dumps({"_doc": ["Written by tools/labyrinth/gen_rooms_rebuilt.py (docs/labyrinth/rooms_rebuilt.md). variants: shipped template -> number of rebuilt variants "
                                "(structure/mega_r/<folder>/<stem>_v1.._vN); stand_rooms: the rebuilt rooms that carry the single armour stand slot of the rarity roll; "
                                "skipped: shipped templates that are not rebuilt, and why."],
                       "variants": dict(sorted(manifest["variants"].items())), "stand_rooms": sorted(manifest["stand_rooms"]),
                       "skipped": dict(sorted(manifest["skipped"].items()))}, indent=1) + "\n"
    if check:
        if not os.path.exists(MANIFEST) or open(MANIFEST).read() != text:
            print("STALE %s" % MANIFEST)
            stale += 1
        return 1 if stale else 0
    if not args:
        os.makedirs(os.path.dirname(MANIFEST), exist_ok=True)
        with open(MANIFEST, "w") as f:
            f.write(text)
    return 0


def write_report(path, results):
    rows = ["| template | v | generator | family | blocks | x | light min/median (shipped) | stand slot |", "|---|---|---|---|---|---|---|---|"]
    for (name, v), (data, problems, st) in sorted(results.items()):
        if not st:
            rows.append("| %s | %d | | | ERROR | | | |" % (name, v))
            continue
        rows.append("| %s | %d | %s | %s | %d | %.2f | %s/%s (%s/%s) | %s |" % (name, v, generator_of(name), st["family"], st["blocks"], st["ratio"], st["light_min"], st["light_median"],
                                                                              st["base_light_min"], st["base_light_median"], "yes" if st["stand_slot"] else ""))
    open(path, "w").write("\n".join(rows) + "\n")


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
