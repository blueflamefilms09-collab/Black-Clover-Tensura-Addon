#!/usr/bin/env python3
"""Writes the REAL generator's output for four synthetic lines into src/test/resources/lines_sample/ (the engine tests read it).

WHY: the engine (skilltree/line/ in Java) must accept exactly what tools/lines/gen_lines.py writes, byte for byte in shape. A hand-written JSON fixture could drift from the generator; this one is made by
the generator's own build_line() from four synthetic Line objects that between them use EVERY kind of the catalogue (26 active, 25 passive, the stance, attr), so a Java test can load the files, check
that each validates cleanly and that each kind has an executor. Run it again after the catalogue changes: python3 tools/lines/engine_fixture.py
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import catalog as C  # noqa: E402
import gen_lines as G  # noqa: E402
from lines_lib import Line, Stance, Stat, Art, Pas  # noqa: E402

OUT = os.path.join(ROOT, "src/test/resources/lines_sample")


def enums_for(kind_spec, i):
    out = {}
    for name, allowed in kind_spec.get("enums", {}).items():
        if name == "effect2":
            out[name] = "none"
        elif name == "vfx":
            out[name] = allowed[i % len(allowed)]
        else:
            out[name] = allowed[(i * 5 + 1) % len(allowed)] if name != "effect" else allowed[(i * 3) % len(allowed)]
    return out


def art(kind, i, finisher=False):
    p = enums_for(C.ACTIVE[kind], i)
    if finisher:
        p["cooldown"] = 1500
    return Art("Fixture %s %d" % (kind.replace("_", " ").title(), i), "A test art of kind %s." % kind, kind, p)


def pas(kind, i):
    return Pas("Fixture Passive %s %d" % (kind.replace("_", " ").title(), i), "A test passive of kind %s." % kind, kind, enums_for(C.PASSIVE[kind], i))


def build():
    act, pass_ = sorted(C.ACTIVE), sorted(C.PASSIVE)
    lines = []
    for n in range(4):
        arts = [art(act[(n * 8 + j) % len(act)], n * 8 + j) for j in range(8)]
        fins = [art(act[(n * 8 + 8 + j) % len(act)], n * 8 + 8 + j, True) for j in range(2)]
        pas_ = [pas(pass_[(n * 7 + j) % len(pass_)], n * 7 + j) for j in range(8)]
        stats = [Stat("Fixture Stat %d %s" % (n, a), "A test stat.", a, 1.0 if a not in ("movement_speed", "attack_speed") else 0.05, "add" if a not in ("movement_speed",) else "mult")
                 for a in ("max_health", "armor", "attack_damage", "movement_speed")]
        stances = [Stance("Fixture Stance %d A" % n, "A test stance.", params={"damage_pct": 0.1, "armor": 2, "vfx": "SPIRIT_AURA"}),
                   Stance("Fixture Stance %d B" % n, "Another test stance.", params={"speed_pct": 0.1, "regen": 0.5, "crit_chance": 0.05, "knockback_res": 0.2, "vfx": "WIND_RING"})]
        lines.append(Line("fixture_line_%d" % n, "Fixture Line %d" % n, "tensura", "A synthetic line for the engine tests.", "#44AACC", C.BACKGROUNDS[n], "minecraft:stick",
                          "Fixture Book %d" % n, "Made by engine_fixture.py.", stances, stats, arts, pas_, fins))
    return lines


def main():
    os.makedirs(OUT, exist_ok=True)
    problems = []
    for L in build():
        route = G.build_line(L, problems)
        if route is None:
            continue
        with open(os.path.join(OUT, L.id + ".json"), "w", encoding="utf-8") as f:
            f.write(json.dumps(route, indent=2, ensure_ascii=False) + "\n")
    if problems:
        print("PROBLEMS:")
        for p in problems:
            print(" -", p)
        sys.exit(1)
    print("ok: wrote the fixture lines to", OUT)


if __name__ == "__main__":
    main()
