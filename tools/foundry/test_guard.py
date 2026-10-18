"""Why: the guard (tools/foundry/guard.py) is the owner's "no two of the 400 may look the same" promise and the "at least 30 sources, none above 8 percent, a source on every row" rule,
so the guard itself must be tested: this file feeds it small synthetic rosters that break each rule one at a time and checks that it complains, then runs it on the real roster and
checks that it is clean. Plain asserts, no framework: `python3 -B tools/foundry/test_guard.py` (exit 0 = all pass); the generator runs the same guard before it writes anything.
"""
import copy
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import guard as G  # noqa: E402
import palette as P  # noqa: E402
import roster_common as RC  # noqa: E402

ABIL = {"dash", "lunge", "volley", "heal_pulse", "ground_slam", "cleave"}
LINE = "Alpha One | beast | small | rust sand iron | S1 | skin=red hair=crop/ember face=grin top=vest legs=shorts prop=dagger | skirmisher | dash lunge"


def row(name, **over):
    r = RC.parse_row(LINE.replace("Alpha One", name), "holy", 1, "test")
    r.update(over)
    return r


def distinct(n, source="S"):
    """n (at most 8) rows that differ from each other in every group (hair, top, legs, prop ...) and in palette; the source is `source` or S0, S1 ..."""
    hairs = ["crop", "spiky", "bob", "long", "bun", "afro", "mohawk", "wizard"]
    tops = ["vest", "coat", "robe", "tunic", "dress", "kimono", "tabard", "uniform"]
    legs = ["shorts", "skirt", "baggy", "chaps", "hakama", "leggings", "trousers", "bare"]
    props = ["dagger", "sword", "staff", "axe", "spear", "mace", "wand", "hammer"]
    wings = ["bat", "angel", "fairy", "dragon", "feathered", "mechanical", "bat", "angel"]
    horns = ["small", "ram", "oni", "bull", "devil", "single", "antlers", "small"]
    cols = sorted(P.COLORS)
    rows = []
    for i in range(n):
        look = "hair=%s top=%s legs=%s prop=%s wings=%s horns=%s" % (hairs[i], tops[i], legs[i], props[i], wings[i], horns[i])
        pal = " ".join([cols[i], cols[i + 20], cols[i + 40]])
        line = "Mob %s | beast | average | %s | %s | %s | brute | dash lunge" % (chr(65 + i), pal, source if source != "S" else "S%d" % i, look)
        rows.append(RC.parse_row(line, "holy" if i % 2 else "abyssal", 1 + i % 5, "test"))
    return rows


def problems(rows, full=False):
    return G.check(rows, ABIL, P.COLORS, P.SKIN_NAMES, expect_total=full)


def has(bad, text):
    return any(text in b for b in bad)


def main():
    a, b = row("Alpha One"), row("Beta Two")
    bad = problems([a, b])
    assert has(bad, "same look signature") and has(bad, "share the palette") and has(bad, "share an ability set and a palette"), bad[:3]
    assert has(bad, "too alike"), bad[:3]

    c = copy.deepcopy(b)
    c["name"] = "alpha one"
    assert has(problems([a, c]), "duplicate name"), "a name that differs only by case is a duplicate"

    d = row("Gamma Three", source=" ")
    assert has(problems([d]), "empty source"), "a row with no source must fail"

    d = row("Delta Four", tier=9)
    assert has(problems([d]), "tier"), "a tier outside 1..5 must fail"
    d = row("Delta Five", abilities=("dash",))
    assert has(problems([d]), "needs 2 or 3 abilities"), "one ability is too few"
    d = row("Delta Six", abilities=("dash", "nonsense"))
    assert has(problems([d]), "unknown ability"), "an unknown ability must fail"

    ok = distinct(8)
    assert not [x for x in problems(ok) if "too alike" in x or "same look" in x or "share the palette" in x], problems(ok)[:3]

    # the distribution rules (full roster only): 40 per tier per road, 30 sources, none above 8 percent
    bad = problems(distinct(8, source="One Series"), full=True)
    assert has(bad, "only 1 different sources"), bad[:3]
    assert has(bad, "of the roster, the limit is 8%"), bad[:3]
    assert has(bad, "tier 1 has"), bad[:3]

    sys.path.insert(0, HERE)
    import gen_foundry as GF
    rows = GF.load_rows()
    real = G.check(rows, GF.load_abilities(), P.COLORS, P.SKIN_NAMES, expect_total=True)
    assert real == [], real
    assert len(rows) == 400
    print("guard tests passed: synthetic breaks are caught, the real roster of %d mobs is clean" % len(rows))


if __name__ == "__main__":
    main()
