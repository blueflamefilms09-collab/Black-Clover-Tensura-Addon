"""Why: the owner's rule for the Mob Foundry is "no two of the 400 may look the same", and a human cannot check 80,000 pairs. This guard is the check: it fails (non-zero
exit, and the JUnit roster test fails with it) when two mobs share a name or an id, a look signature, a palette triple, an ability set together with a palette, or when two
mobs are closer than MIN_STRUCT groups in structure (so a recolour with a different hat does not count as a new creature); when a tier of a road does not hold exactly
40 mobs; when a row has no source; when fewer than MIN_SOURCES different sources are used or one source is more than MAX_SHARE of the roster. Pure python (no art kit), so
it also runs in CI and can be imported by the generator. `report()` returns the closest pairs, which is how a designer finds the two mobs that need a different hat.
"""
import itertools
import os
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import roster_common as RC  # noqa: E402

MIN_STRUCT = 3            # two mobs must differ in at least this many of the 16 structure groups
MIN_SOURCES = 30
MAX_SHARE = 0.08          # no source above 8 % of the roster (the owner said "about"; 0.08 of 400 is 32)
PER_TIER = 40
ROADS = ("holy", "abyssal")


def source_key(src):
    """The grouping key of a source: the text as written ('D&D: kobold', 'Black Clover'), case-insensitive."""
    return src.strip().lower()


def source_family(src):
    """'dnd' for D&D sources, 'anime' for everything else (the report shows both)."""
    return "dnd" if src.strip().lower().startswith("d&d") else "anime"


def check(rows, abilities, colors, skins, expect_total=True):
    """Returns the list of problems (strings). Empty = the roster is valid."""
    bad = []
    names, ids, looks, pals, abil_pal = {}, {}, {}, {}, {}
    for r in rows:
        for why in RC.check_row(r, abilities, colors, skins):
            bad.append("%s: %s" % (r["name"], why))
        if r["name"].lower() in names:
            bad.append("duplicate name %r (also %s)" % (r["name"], names[r["name"].lower()]))
        names[r["name"].lower()] = r["id"]
        if r["id"] in ids:
            bad.append("duplicate id %r" % r["id"])
        ids[r["id"]] = r["name"]
        sig = RC.look_signature(r)
        if sig in looks:
            bad.append("%s and %s have the same look signature" % (r["name"], looks[sig]))
        looks[sig] = r["name"]
        if r["pal"] in pals:
            bad.append("%s and %s share the palette %s" % (r["name"], pals[r["pal"]], " ".join(r["pal"])))
        pals[r["pal"]] = r["name"]
        k = (tuple(sorted(r["abilities"])), r["pal"])
        if k in abil_pal:
            bad.append("%s and %s share an ability set and a palette" % (r["name"], abil_pal[k]))
        abil_pal[k] = r["name"]
    for a, b in itertools.combinations(rows, 2):
        d = RC.struct_distance(a, b)
        if d < MIN_STRUCT:
            bad.append("%s and %s are too alike: only %d of %d structure groups differ" % (a["name"], b["name"], d, len(RC.STRUCT_GROUPS)))
    counts = Counter((r["road"], r["tier"]) for r in rows)
    if expect_total:
        for road in ROADS:
            for t in RC.TIERS:
                if counts[(road, t)] != PER_TIER:
                    bad.append("road %s tier %d has %d mobs, the roster needs %d" % (road, t, counts[(road, t)], PER_TIER))
    src = Counter(source_key(r["source"]) for r in rows)
    if expect_total:
        if len(src) < MIN_SOURCES:
            bad.append("only %d different sources, the roster needs at least %d" % (len(src), MIN_SOURCES))
        if rows:
            top, n = src.most_common(1)[0]
            if n > MAX_SHARE * len(rows) + 1e-9:
                bad.append("source %r has %d mobs = %.1f%% of the roster, the limit is %.0f%%" % (top, n, 100.0 * n / len(rows), 100 * MAX_SHARE))
    return bad


def report(rows):
    """Plain facts for the verification report: counts per road and tier, per faction, per archetype, the sources, and the 12 closest pairs."""
    out = []
    out.append("mobs: %d" % len(rows))
    for road in ROADS:
        out.append("  %-8s %s" % (road, "  ".join("t%d=%d" % (t, sum(1 for r in rows if r["road"] == road and r["tier"] == t)) for t in RC.TIERS)))
    out.append("factions: " + ", ".join("%s=%d" % kv for kv in sorted(Counter(r["faction"] for r in rows).items())))
    out.append("archetypes: " + ", ".join("%s=%d" % kv for kv in sorted(Counter(r["arch"] for r in rows).items())))
    out.append("builds: " + ", ".join("%s=%d" % kv for kv in sorted(Counter(r["build"] for r in rows).items())))
    src = Counter(source_key(r["source"]) for r in rows)
    fam = Counter(source_family(r["source"]) for r in rows)
    out.append("sources: %d different (dnd %d mobs, anime/game %d mobs); largest: %s" % (len(src), fam["dnd"], fam["anime"], ", ".join("%s=%d" % kv for kv in src.most_common(6))))
    pairs = sorted(((RC.struct_distance(a, b), a["name"], b["name"]) for a, b in itertools.combinations(rows, 2)))[:12]
    out.append("closest pairs (structure groups that differ, of %d):" % len(RC.STRUCT_GROUPS))
    for d, a, b in pairs:
        out.append("  %2d  %s / %s" % (d, a, b))
    ab = Counter(a for r in rows for a in r["abilities"])
    out.append("abilities used: %d of the library; least used: %s" % (len(ab), ", ".join("%s=%d" % kv for kv in sorted(ab.items(), key=lambda kv: kv[1])[:6])))
    return out
