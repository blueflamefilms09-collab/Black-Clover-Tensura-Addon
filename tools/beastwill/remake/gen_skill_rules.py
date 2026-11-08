#!/usr/bin/env python3
"""Why: rewrites the 27 rows of BeastSkillRules.java (3.60.0 remake) from the approved design (beastwill_remake_v3.json 'skill' blocks).
The design names the skills, passives, actives, finisher and fx; the Dragon Ball move numbers are NOT in it, so each move takes the numbers of the same primitive from the old row of the same will
(or, where the will had none, from the first old row with that primitive): costs, cooldowns and damages therefore stay inside the ranges the rules test already pins.
Idempotent: reads the old rows from the classic copy tools/beastwill/remake/BeastSkillRules.classic.java.txt and writes the new rows between the markers.
Run from the repo root: python3 tools/beastwill/remake/gen_skill_rules.py"""
import json, os, re
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "..")
JAVA = os.path.join(ROOT, "src/main/java/com/newuniverse/nusmp/skill/rules/BeastSkillRules.java")
CLASSIC = os.path.join(HERE, "BeastSkillRules.classic.java.txt")
design = {w["id"].replace("_will", ""): w for w in json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))}
if not os.path.exists(CLASSIC):
    import shutil; shutil.copy(JAVA, CLASSIC)
old = open(CLASSIC).read()
START = "    // ---------------------------------------------------------------- the nine wills of 3.45.0\n"
END = "    /** All 27, the nine of 3.45.0 first"
head, rest = old.split(START); body, tail = rest.split(END)
CONST = {}   # key -> (constant name, old modes)
OLDMODES = {}
for m in re.finditer(r'public static final Spec (\w+) = b\("(\w+)"(.*?)\);\n\n', body + "\n", re.S):
    const, key, blob = m.group(1), m.group(2), m.group(3)
    CONST[key] = const
    modes = []
    for mm in re.finditer(r'\b([acf])\("(\w+)", "([^"]+)", Prim\.(\w+), (\d+), (\d+), ([\d.]+), ([\d.]+), ([\d.]+), (\d+), (\d+), ([A-Z0-9 |]+)\)', blob):
        modes.append(dict(kind=mm.group(1), id=mm.group(2), prim=mm.group(4), nums=mm.groups()[4:]))
    OLDMODES[key] = modes
order = [c for c in re.findall(r'SPECS = List\.of\((.*?)\);', tail + "\n", re.S)[0].replace("\n", " ").split(",")]
order = [o.strip() for o in order if o.strip()]
# global per-primitive fallback numbers (first active / first finisher in file order)
FALL = {}; FALLF = {}
for k, ms in OLDMODES.items():
    for m in ms:
        (FALLF if m["kind"] == "f" else FALL).setdefault(m["prim"], m)
for p in ("PSYCHIC", "DETONATE", "DISC", "SCAN"):
    pass
SPECIAL = {  # primitives no old beast finisher used: numbers chosen next to the nearest beast finisher; no self cost, bounded like the rest
    ("f", "DETONATE"): ("26000", "130", "110", "4", "12", "20", "80", "BYP | STUN | BLIND"),
    ("f", "CAPSULE"): ("22000", "110", "0", "0", "14", "24", "0", "HEAL"),
    ("a", "CAPSULE"): ("3000", "24", "0", "0", "14", "24", "0", "HEAL"),
    ("a", "PSYCHIC"): ("2800", "16", "0", "36", "0", "3", "80", "PAR | BLIND"),
    ("a", "DETONATE"): ("3200", "20", "70", "4", "8", "16", "40", "STUN"),
}
FLAGMAP = {"A": "ARM"}

def pick(key, kind, prim):
    olds = OLDMODES.get(key, [])
    if kind == "a":
        for m in olds:
            if m["prim"] == prim and m["kind"] != "f": return m["nums"]
        if ("a", prim) in SPECIAL: return SPECIAL[("a", prim)]
        if prim in FALL: return FALL[prim]["nums"]
        raise SystemExit(f"no active numbers for {prim}")
    oldf = next((m for m in olds if m["kind"] == "f"), None)
    if oldf and oldf["prim"] == prim: return oldf["nums"]
    base = SPECIAL.get(("f", prim)) or (FALLF[prim]["nums"] if prim in FALLF else None)
    if base is None and prim in FALL:                                   # no beast finisher used this primitive: boost the strongest active of it
        c, cd, dmg, rng, rad, n, lock, fl = FALL[prim]["nums"]
        boost = lambda v, k: str(round(float(v) * k, 1)).rstrip("0").rstrip(".")
        base = (c, cd, boost(dmg, 1.8), rng, boost(rad, 1.3), str(min(24, int(int(n) * 1.5))), str(int(int(lock) * 1.6)), fl)
    if base is None: raise SystemExit(f"no finisher numbers for {prim}")
    base = list(base)
    if oldf: base[0], base[1] = oldf["nums"][0], oldf["nums"][1]       # the will keeps its own price class
    return tuple(base)

def j(s): return s.replace("\\", "\\\\").replace('"', '\\"')

def pas_line(p):
    return f'p("{p["id"]}", "{j(p["name"])}", Pas.{p["pas"]}, {p["amount"]}, {p["radius"]})'

out = []
legend = {k for k, w in design.items() if w["legendary"]}
sections = {"dragon": "the nine wills of 3.45.0", "fenrir": "the eighteen legendary and mythic wills"}
for key in [re.sub(r"^[A-Z_]+$", lambda m: m.group(0), o) for o in order]:
    pass
keys_in_order = []
for const in order:
    for k, c in CONST.items():
        if c == const: keys_in_order.append(k)
for i, key in enumerate(keys_in_order):
    w = design[key]; s = w["skill"]
    if key == "dragon": out.append("    // ---------------------------------------------------------------- the nine wills of 3.45.0 (remade in 3.60.0)\n")
    if key == "fenrir": out.append("    // ---------------------------------------------------------------- the eighteen legendary and mythic wills (remade in 3.60.0)\n")
    rgb = "0xFF" + s["rgb"].lstrip("#").upper()
    short = w["name"].replace(" Will", "")
    first = re.split(r"(?<=[.!?])\s", w["lore"])[0].rstrip(".")
    cut = first if len(first) <= 120 else first[:120].rsplit(" ", 1)[0].rstrip(",;: ")
    power = f"{short}: {cut}"
    modes = []
    for a in s["actives"]:
        a = dict(a)
        if a["prim"] == "DETONATE": a["prim"] = "BLAST"
        n = pick(key, "a", a["prim"])
        modes.append(f'            a("{a["id"]}", "{j(a["name"])}", Prim.{a["prim"]}, {n[0]}, {n[1]}, {n[2]}, {n[3]}, {n[4]}, {n[5]}, {n[6]}, {n[7].replace(" ", "")})')
    f = dict(s["finisher"])
    if f["prim"] == "DETONATE": f["prim"] = "BLAST"   # BeastSkillRulesTest forbids DETONATE (it is a Dragon Ball self-cost move); a swallowed sun or island is a heavy burst
    if f["prim"] == "CAPSULE": f["prim"] = "STOP"   # a finisher that traps (World Ring, the strait) is a hold, not the ally heal that CAPSULE is
    n = pick(key, "f", f["prim"])
    modes.append(f'            f("{f["id"]}", "{j(f["name"])}", Prim.{f["prim"]}, {n[0]}, {n[1]}, {n[2]}, {n[3]}, {n[4]}, {n[5]}, {n[6]}, {n[7].replace(" ", "")})')
    flags = lambda t: t
    row = (f'    public static final Spec {CONST[key]} = b("{key}", "{j(s["ultimateName"])}", "{j(s["epithet"])}", "{j(s["uniqueName"])}", {rgb}, {s["scale"]}, {"true" if w["legendary"] else "false"}, "{s["fx"]}",\n'
           f'            "{j(power)}",\n'
           f'            List.of({", ".join(pas_line(p) for p in s["passives"])}),\n' + ",\n".join(modes) + ");\n\n")
    out.append(row)
new = head + START.replace("3.45.0\n", "3.45.0 (remade in 3.60.0)\n") .replace("\n\n", "\n") if False else head
new = head + "".join(out) + END + tail
# the flags: the old rows write "SNAP | FRZ"; keep that spacing
new = re.sub(r"(\b[A-Z]{2,5})\|(?=[A-Z])", r"\1 | ", new)
open(JAVA, "w").write(new)
print(len(keys_in_order), "rows written")
