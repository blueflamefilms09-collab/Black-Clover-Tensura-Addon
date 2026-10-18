"""Why: the one command that builds the Mob Foundry (docs/labyrinth/foundry.md). It reads the roster tables (roster_holy.py, roster_abyssal.py), runs the guard (guard.py), and writes

  src/main/resources/assets/nusmp/geo/entity/foundry_<id>.geo.json            the model of each mob (kit geometry, minified)
  src/main/resources/assets/nusmp/textures/entity/foundry_<id>[_glow].png     its texture and glow layer (8-bit palette PNG)
  src/main/resources/assets/nusmp/animations/entity/foundry_anim_<class>.animation.json   the SHARED clips of an animation class (see foundry_kit.py)
  src/main/resources/assets/nusmp/foundry/index.json                           what the client needs to draw a mob: animation class, glow colour, size
  src/main/resources/data/nusmp/mega/foundry/<id>.json                         the definition the server reads (road, tier, faction, archetype, abilities)
  docs/labyrinth/foundry_report.txt                                            the verification report: counts, closest look pairs, sizes, animation sharing

    python3 -B tools/foundry/gen_foundry.py                 # everything (guard first; a failing guard writes nothing)
    python3 -B tools/foundry/gen_foundry.py --only a b      # only these ids (models and data; the index and the report are still complete)
    python3 -B tools/foundry/gen_foundry.py --verify        # only the roster guard, no files
    python3 -B tools/foundry/gen_foundry.py --partial       # the roster may be unfinished (no 40-per-tier or source-count rule); used while a batch is being written
    python3 -B tools/foundry/gen_foundry.py --measure 12    # also build the own clips of 12 mobs and report how far the shared class clips differ from them
    python3 -B tools/foundry/gen_foundry.py --check         # rebuild into a temp folder and fail if a committed file differs (JSON bytes, PNG pixels)
    python3 -B tools/foundry/gen_foundry.py --preview id    # build the mob with its class clips and render the kit preview scenes (JDK 21, numpy, Pillow)

Models are built in parallel (one process per core) and only when their inputs changed would be an optimisation; the full build takes a few minutes, a mob about a second.
"""
import json
import multiprocessing
import os
import shutil
import sys
import tempfile
import time

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.abspath(os.path.join(HERE, "..")))
import foundry_kit as FK  # noqa: E402
import guard as G  # noqa: E402
import palette as PAL  # noqa: E402
import roster_common as RC  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(REPO, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "nusmp")
DATA = os.path.join(RES, "data", "nusmp", "mega", "foundry")
REPORT = os.path.join(REPO, "docs", "labyrinth", "foundry_report.txt")


def load_rows():
    """All rows of both roads, in file order (road, tier, line)."""
    import importlib
    rows = []
    for road in G.ROADS:
        mod = importlib.import_module("roster_" + road if road == "abyssal" else "roster_holy")
        for tier in RC.TIERS:
            block = mod.TIERS.get(tier, "")
            rows += RC.parse_block(block, road, tier, "roster_%s t%d" % (road, tier))
    return rows


def load_abilities():
    with open(os.path.join(DATA, "_abilities.json")) as f:
        return {a["id"]: a for a in json.load(f)["abilities"]}


def mob_json(r, facts):
    """The server-side definition of a mob (data/nusmp/mega/foundry/<id>.json)."""
    return {"id": r["id"], "name": r["name"], "road": r["road"], "tier": r["tier"], "faction": r["faction"], "archetype": r["arch"], "abilities": list(r["abilities"]),
            "size": r["size"], "height": facts["height"], "source": r["source"], "glow": facts["glow"], "animation": facts["cls"]}


def build_one(args):
    """Worker: builds the geometry and textures of one mob and returns its facts."""
    r, assets = args
    try:
        spec = FK.build_spec(r)
        R, S = FK.build_model(spec)
    except Exception as e:                                       # name the row: a bad token in 400 rows must be found at once
        raise RuntimeError("row %r: %s" % (r["name"], e)) from None
    sizes = FK.save_model(R, r["id"], assets)
    glow = S["glow"]["color"] or "#ffffff"
    return {"id": r["id"], "cls": FK.class_of(S), "height": round(R.d.top / 16.0, 2), "glow": glow, "sizes": sizes, "spec": spec}


def class_one(args):
    cls, assets = args
    R = FK.build_class_anim(cls)
    n = FK.save_class_anim(R, cls, assets)
    return cls, n, sorted(R.A.clips["idle"]["bones"])


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=1, sort_keys=True)
        f.write("\n")


def build_all(rows, only, assets, data_dir, jobs, measure):
    t0 = time.time()
    todo = [r for r in rows if not only or r["id"] in only]
    with multiprocessing.Pool(jobs) as pool:
        facts = {f["id"]: f for f in pool.map(build_one, [(r, assets) for r in todo], chunksize=4)}
        print("models: %d built in %.0f s" % (len(facts), time.time() - t0))
    # facts of the mobs that were not rebuilt are read back from their data file, so the index and the report stay complete
    for r in rows:
        if r["id"] not in facts:
            p = os.path.join(data_dir, r["id"] + ".json")
            if os.path.isfile(p):
                d = json.load(open(p))
                facts[r["id"]] = {"id": r["id"], "cls": d["animation"], "height": d["height"], "glow": d["glow"], "sizes": None, "spec": None}
    classes = sorted({f["cls"] for f in facts.values()})
    t1 = time.time()
    need = [c for c in classes if not os.path.isfile(os.path.join(assets, "animations", "entity", "foundry_anim_%s.animation.json" % c))] if only else classes
    with multiprocessing.Pool(jobs) as pool:
        csizes = {c: n for c, n, _ in pool.map(class_one, [(c, assets) for c in need])}
    print("animation classes: %d of %d built in %.0f s" % (len(need), len(classes), time.time() - t1))
    for r in rows:
        f = facts.get(r["id"])
        if f:
            write_json(os.path.join(data_dir, r["id"] + ".json"), mob_json(r, f))
    index = {"why": "what the client needs to draw a foundry mob: anim = its shared animation class, glow = glow colour 0xRRGGBB, size = model scale, height = model height in blocks, tier = 1..5 (the glow burns brighter deeper down)",
             "mobs": {}}
    for r in rows:
        f = facts.get(r["id"])
        if f:
            index["mobs"][r["id"]] = {"anim": f["cls"], "glow": int(f["glow"].lstrip("#"), 16), "size": r["size"], "height": f["height"], "tier": r["tier"]}
    write_json(os.path.join(assets, "foundry", "index.json"), index)
    return facts, classes, csizes


def folder_bytes(root, prefix, suffixes):
    n = c = 0
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.startswith(prefix) and fn.endswith(suffixes):
                n += os.path.getsize(os.path.join(dp, fn))
                c += 1
    return c, n


def zipped(root, prefix, suffixes):
    """The size these files take in a jar (deflate level 9 of every file on its own, which is what a jar does)."""
    import zlib
    total = 0
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.startswith(prefix) and fn.endswith(suffixes):
                total += len(zlib.compress(open(os.path.join(dp, fn), "rb").read(), 9)) + 90      # about 90 bytes of zip header and directory entry per file
    return total


def measure_sample(rows, facts, n):
    """Builds the own clips of n mobs spread over the roster and reports the worst difference of a bone rotation between them and the shared class clips."""
    out = []
    step = max(1, len(rows) // max(1, n))
    for r in rows[::step][:n]:
        f = facts.get(r["id"])
        if not f or not f.get("spec"):
            continue
        own = FK.build_own_clips(f["spec"])
        shared = json.load(open(os.path.join(ASSETS, "animations", "entity", "foundry_anim_%s.animation.json" % f["cls"])))["animations"]
        worst, where = FK.clip_deviation(own.A.clips, shared, [b for b in own.m.order if b not in ("root", "body")])
        out.append((r["id"], f["cls"], worst, where))
    return out


def write_report(rows, facts, classes, csizes, measured, path):
    lines = ["Mob Foundry verification report (generated by tools/foundry/gen_foundry.py; do not edit)", ""]
    lines += G.report(rows)
    lines.append("")
    ma = [f["sizes"] for f in facts.values() if f.get("sizes")]
    if ma:
        lines.append("models: geo avg %.1f KB, texture avg %.1f KB, glow avg %.2f KB, cubes avg %.0f, max %.0f" % (
            sum(s["geo"] for s in ma) / len(ma) / 1024, sum(s["tex"] for s in ma) / len(ma) / 1024, sum(s["glow"] for s in ma) / len(ma) / 1024,
            sum(s["cubes"] for s in ma) / len(ma), max(s["cubes"] for s in ma)))
    gc, gb = folder_bytes(ASSETS, "foundry_", (".geo.json",))
    tc, tb = folder_bytes(ASSETS, "foundry_", (".png",))
    ac, ab = folder_bytes(ASSETS, "foundry_anim_", (".animation.json",))
    lines.append("on disk: %d geo %.1f KB, %d textures %.1f KB, %d shared animation files %.1f KB" % (gc, gb / 1024, tc, tb / 1024, ac, ab / 1024))
    zg, zt, za = zipped(ASSETS, "foundry_", (".geo.json",)), zipped(ASSETS, "foundry_", (".png",)), zipped(ASSETS, "foundry_anim_", (".animation.json",))
    lines.append("in the jar (deflate): geo %.1f KB, textures %.1f KB, animations %.1f KB, together %.2f MB" % (zg / 1024, zt / 1024, za / 1024, (zg + zt + za) / 1048576.0))
    n = max(1, len(rows))
    lines.append("without sharing the clips each mob would own ~217 KB: %.1f MB uncompressed, ~%.1f MB in the jar; with %d shared classes it is %.2f MB uncompressed" % (
        n * 217 / 1024.0, n * 28 / 1024.0, len(classes), ab / 1048576.0))
    lines.append("")
    lines.append("animation classes (%d), members each:" % len(classes))
    from collections import Counter
    cnt = Counter(f["cls"] for f in facts.values())
    for c in sorted(classes, key=lambda c: (-cnt[c], c)):
        lines.append("  %3d  %s" % (cnt[c], c))
    if measured:
        lines.append("")
        lines.append("sharing fidelity (largest rotation difference in degrees between a mob's own clips and its class clips, on the bones the mob has):")
        for mid, cls, worst, where in measured:
            lines.append("  %-28s %6.1f deg  %s" % (mid, worst, where))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")
    return lines


def check(rows, jobs):
    """Rebuilds everything into a temporary folder and compares it with the committed files."""
    tmp = tempfile.mkdtemp(prefix="foundry_check_")
    try:
        assets = os.path.join(tmp, "assets", "nusmp")
        data = os.path.join(tmp, "data")
        build_all(rows, (), assets, data, jobs, 0)
        bad = 0
        n = 0
        for root, base in ((assets, ASSETS), (data, DATA)):
            for dp, _, fns in os.walk(root):
                for fn in fns:
                    src = os.path.join(dp, fn)
                    dst = os.path.join(base, os.path.relpath(src, root))
                    n += 1
                    if not same_file(src, dst):
                        print("DIFFERS", os.path.relpath(dst, REPO))
                        bad += 1
        print("checked %d files: %d differ" % (n, bad))
        return 1 if bad else 0
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def same_file(src, dst):
    if not os.path.isfile(dst):
        return False
    if src.endswith(".png"):
        from PIL import Image
        a, b = Image.open(src).convert("RGBA"), Image.open(dst).convert("RGBA")
        return a.size == b.size and a.tobytes() == b.tobytes()
    return open(src, "rb").read() == open(dst, "rb").read()


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    flags = [a for a in argv if a.startswith("--")]
    jobs = max(1, min(8, os.cpu_count() or 1))
    rows = load_rows()
    abilities = load_abilities()
    partial = "--partial" in flags
    problems = G.check(rows, abilities, PAL.COLORS, PAL.SKIN_NAMES, expect_total=not partial)
    for p in problems[:60]:
        print("GUARD", p)
    if problems:
        print("%d guard problems (showing at most 60); nothing was written" % len(problems))
        return 1
    errs = []
    for r in rows:                                               # every row through the kit's own validation first: all the typos at once, not one per run
        try:
            FK.kit().normalize(FK.build_spec(r), r["id"])
        except Exception as e:
            errs.append("%s: %s" % (r["name"], e))
    for e in errs[:80]:
        print("KIT", e)
    if errs:
        print("%d rows the art kit refuses; nothing was written" % len(errs))
        return 1
    print("guard ok: %d mobs" % len(rows))
    if "--verify" in flags:
        for line in G.report(rows):
            print(line)
        return 0
    if "--check" in flags:
        return check(rows, jobs)
    if "--preview" in flags:
        return preview(args, rows)
    only = set(args)
    for o in only:
        if o not in {r["id"] for r in rows}:
            print("no such mob", o)
            return 1
    measure = 0
    if "--measure" in flags:
        i = argv.index("--measure")
        measure = int(argv[i + 1]) if i + 1 < len(argv) and argv[i + 1].isdigit() else 12
        only.discard(str(measure))
    facts, classes, csizes = build_all(rows, only, ASSETS, DATA, jobs, measure)
    measured = measure_sample(rows, facts, measure) if measure else []
    lines = write_report(rows, facts, classes, csizes, measured, REPORT)
    print("\n".join(lines[:14]))
    return 0


def preview(ids_and_scenes, rows):
    """Renders the kit preview scenes of foundry mobs: `--preview id [front back face idle walk run attack cast cast_big hurt death]`. The mob must have been built
    (its geo, texture and class animation exist); the scenes name the model by path and give the SHARED animation file explicitly, so what you see is what the game plays.
    The scene file is a scratch file (tools/render_preview/scenes.d/zz_foundry_preview.json) and is deleted afterwards; the PNGs go to $RENDER_PREVIEW_BUILD."""
    import subprocess
    by_id = {r["id"]: r for r in rows}
    ids = [a for a in ids_and_scenes if a in by_id]
    names = [a for a in ids_and_scenes if a not in by_id]
    from arc_kit import scenes as SC
    scene_file = os.path.join(REPO, "tools", "render_preview", "scenes.d", "zz_foundry_preview.json")
    allsc = {}
    for mid in ids:
        spec = FK.build_spec(by_id[mid])
        R, S = FK.build_model(spec)
        cls = FK.class_of(S)
        anim = json.load(open(os.path.join(ASSETS, "animations", "entity", "foundry_anim_%s.animation.json" % cls)))["animations"]
        clips = {k: (v["animation_length"], v["loop"]) for k, v in anim.items()}
        for name, sc in SC.scenes_for(R, clips).items():
            sc["model"] = "nusmp:geo/entity/foundry_%s.geo.json" % mid
            sc["texture"] = "nusmp:textures/entity/foundry_%s.png" % mid
            sc["animations"] = "nusmp:animations/entity/foundry_anim_%s.animation.json" % cls
            allsc[name.replace("arc_", "foundry_", 1)] = sc
    with open(scene_file, "w") as f:
        json.dump(allsc, f)
    try:
        want = [n for n in allsc if not names or any(n.endswith("_" + x) for x in names)]
        return subprocess.run([sys.executable, "-B", os.path.join(REPO, "tools", "render_preview", "preview.py")] + want, cwd=REPO).returncode
    finally:
        os.remove(scene_file)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
