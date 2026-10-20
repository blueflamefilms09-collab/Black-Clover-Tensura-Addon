"""Builds the God worn upgrade (slice `god_worn`): skins, models (full, held, LOD), the catalog `assets/nusmp/visual_upgrade/catalog.txt` and the per-relic effect tables.

    python3 -B tools/visual_upgrade/god_worn/build.py [--ids a,b] [--check] [--no-skin]

Adds files only: nothing of the old models, atlas or catalog is touched (rule 2). The shared generator tools/relics/gen_worn_all.py calls `main()` once at its end (one append-only
hook line), so regenerating the worn library regenerates this too.
"""
import hashlib
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import gw_lib as G  # noqa: E402
import skin as SK  # noqa: E402
import items as IT  # noqa: E402
import caps as CP  # noqa: E402
import metrics as M  # noqa: E402

UPG = os.path.join(G.ASSETS, "visual_upgrade")


def all_builders():
    import importlib
    out = {}
    for name in ("b_back", "b_back2", "b_body", "b_hands", "b_head"):
        try:
            out.update(importlib.import_module(name).BUILDERS)
        except ModuleNotFoundError as e:
            if e.name != name:
                raise
    return out


def all_skins():
    import skins_spec
    return skins_spec.SPECS


def quads_at(rid, builder, level, scale):
    D = G.Detail(level, scale)
    b = G.Item(rid, D)
    builder(b, D)
    return b.quads()


def fit_scale(rid, builder, level, target, lo_frac=1.0, hi_frac=1.12, start=1.0):
    """The largest detail scale whose quad count stays <= target * hi_frac (a bisection; quads grow about with the square of the scale, with plateaus where whole features appear).
    `lo_frac` is only used to stop early once the count is inside [target * lo_frac, target * hi_frac]."""
    q1 = quads_at(rid, builder, level, start)
    if target * lo_frac <= q1 <= target * hi_frac:
        return start, q1
    lo_s, hi_s = 0.08, 4.0
    best = (lo_s, quads_at(rid, builder, level, lo_s))
    for _ in range(14):
        mid = (lo_s * hi_s) ** 0.5
        q = quads_at(rid, builder, level, mid)
        if q <= target * hi_frac:
            if mid >= best[0]:
                best = (mid, q)
            lo_s = mid
            if q >= target * lo_frac:
                break
        else:
            hi_s = mid
    return best


def targets_for(rid):
    base = M.load_json(os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_baseline.json"))["items"][rid]["quads"]
    return max(900, int(5.05 * base) + 1)


SCALES_PATH = os.path.join(HERE, "scales.json")


def _scales_cache():
    try:
        with open(SCALES_PATH, encoding="utf8") as fh:
            return json.load(fh)
    except (OSError, ValueError):
        return {}


def fitted_scales(rid, builder, refit=False):
    """The detail scales (full, held, lod) of a relic: the cached ones when they still land inside the targets (one build each), else fitted again and cached in scales.json."""
    cache = _scales_cache()
    full_t = int(targets_for(rid) * 1.04)
    got = cache.get(rid)
    if got and not refit:
        qf = quads_at(rid, builder, "full", got["full"])
        qh = quads_at(rid, builder, "held", got["held"])
        ql = quads_at(rid, builder, "lod", got["lod"])
        if full_t <= qf <= full_t * 1.10 and qh <= 600 and ql <= 0.14 * qf:
            return got
    s_full, q_full = fit_scale(rid, builder, "full", full_t, 1.0, 1.10)
    s_held, q_held = fit_scale(rid, builder, "held", 560, 0.85, 1.0, start=s_full)
    s_lod, q_lod = fit_scale(rid, builder, "lod", 0.12 * q_full, 0.7, 1.0, start=s_full)
    got = {"full": round(s_full, 4), "held": round(s_held, 4), "lod": round(s_lod, 4)}
    cache[rid] = got
    with open(SCALES_PATH, "w", encoding="utf8", newline="\n") as fh:
        json.dump(cache, fh, indent=1, sort_keys=True)
        fh.write("\n")
    return got


def build_item(rid, builder, write=True, refit=False):
    """Builds the three levels of one relic. Returns {"full": [pieces], "held": [pieces], "lod": [pieces], "stats": {...}}."""
    res = {"stats": {}}
    scales = fitted_scales(rid, builder, refit)
    if write:                                                                       # this build's own earlier files of this relic (a different split into files) are replaced, never anything else
        for fn in os.listdir(G.WORN_DIR):
            if fn.startswith("gw_%s_" % rid) and fn.endswith(".txt"):
                os.remove(os.path.join(G.WORN_DIR, fn))
    for level, suffix in (("full", ""), ("held", "_held"), ("lod", "_lod")):
        D = G.Detail(level, scales[level])
        b = G.Item(rid, D)
        builder(b, D)
        pieces = []
        quads = 0
        files = []
        for gs in b.sets:
            if not gs.items:
                continue
            if write:
                names = G.write_set(gs, suffix, rid)
            else:
                names = [("gw_%s_%s%s" % (rid, gs.key, suffix), gs.quads())]
            pieces += G.pieces_of(gs, names)
            files += [n for n, _q in names]
            quads += gs.quads() * (2 if gs.pair else 1)
        res[level] = pieces
        res["stats"][level] = {"quads": quads, "vertices": quads * 4, "files": files, "sets": len(b.sets), "scale": round(scales[level], 3)}
    return res


STAMP = os.path.join(HERE, ".build_stamp")


def source_stamp():
    """A hash of everything the outputs depend on (this folder's generators and caches, the shared mesh library and the worn library kit), so the hook in gen_worn_all.py can skip an up to date build."""
    h = hashlib.sha256()
    skip = {"doc.py", "preview.py", "after.py", "baseline.py", "counts.py"}                  # tools that read the outputs do not change them
    names = [os.path.join(HERE, f) for f in sorted(os.listdir(HERE)) if (f.endswith(".py") and f not in skip) or f == "scales.json"]
    names += [os.path.join(HERE, "..", "..", "relics", f) for f in ("mesh_lib.py", "gen_worn_lib.py")]
    names.append(os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_baseline.json"))
    for n in names:
        h.update(os.path.basename(n).encode())
        with open(n, "rb") as fh:
            h.update(fh.read())
    return h.hexdigest()


def up_to_date():
    try:
        with open(STAMP, encoding="utf8") as fh:
            return fh.read().strip() == source_stamp() and os.path.exists(os.path.join(UPG, "catalog.txt"))
    except OSError:
        return False


def main(argv=None):
    argv = argv if argv is not None else sys.argv[1:]
    if not argv and up_to_date():
        print("god_worn: the upgraded models, skins and effect tables are up to date (use --force to rebuild)")
        return 0
    only = None
    for a in argv:
        if a.startswith("--ids"):
            only = (a.split("=", 1)[1] if "=" in a else argv[argv.index(a) + 1]).split(",")
    check = "--check" in argv
    argv = [a for a in argv if a != "--force"]
    builders = all_builders()
    skins = all_skins()
    lines = ["# visual upgrade catalog (slice god_worn): written by tools/visual_upgrade/god_worn/build.py, do not edit.",
             "# up <id> <piece...>     the worn models of the upgraded relic (replace its relic / look entry of relic_worn/catalog.txt when visual.upgrade is on)",
             "# lod <id> <piece...>    the worn models drawn beyond 12 blocks (<= 15% of the vertices) and at the low quality",
             "# held <id> <piece...>   the models held in the hand, on the ground and in a frame (<= 2500 vertices)"]
    ok = True
    entries = {}
    cat_path = os.path.join(UPG, "catalog.txt")
    if os.path.exists(cat_path):
        for line in open(cat_path, encoding="utf8"):
            v = line.split()
            if len(v) > 2 and v[0] in ("up", "lod", "held"):
                entries[(v[0], v[1])] = line.strip()
    for rid in IT.IDS:
        if only and rid not in only:
            continue
        if rid not in builders:
            print("no builder yet for", rid)
            continue
        if not check and "--no-skin" not in argv:
            SK.write_skin(rid, skins[rid], G.SKIN_DIR)
        res = build_item(rid, builders[rid], write=not check)
        st = res["stats"]
        print("%-30s full %5d q  held %4d q  lod %4d q (%.1f%%)  files %d" % (rid, st["full"]["quads"], st["held"]["quads"], st["lod"]["quads"], 100.0 * st["lod"]["quads"] / max(1, st["full"]["quads"]),
                                                                       len(st["full"]["files"])))
        problems = CP.check_item(rid, IT.BY_ID[rid][2], st)
        for pr in problems:
            print("   PROBLEM", pr)
            ok = False
        entries[("up", rid)] = "up %s %s" % (rid, " ".join(res["full"]))
        entries[("lod", rid)] = "lod %s %s" % (rid, " ".join(res["lod"]))
        entries[("held", rid)] = "held %s %s" % (rid, " ".join(res["held"]))
    if not check:
        os.makedirs(UPG, exist_ok=True)
        for kind in ("up", "lod", "held"):
            for rid in IT.IDS:
                if (kind, rid) in entries:
                    lines.append(entries[(kind, rid)])
        with open(cat_path, "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")
        if not only and ok:
            import fx as _fx
            _fx.write_all()
            with open(STAMP, "w", encoding="utf8", newline="\n") as fh:
                fh.write(source_stamp() + "\n")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
