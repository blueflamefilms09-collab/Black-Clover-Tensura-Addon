#!/usr/bin/env python3
"""Generate data/nusmp/skill_trees/<line>.json for every Line in tools/lines/rows/*.py.

usage: python3 tools/lines/gen_lines.py [--only <franchise>] [--check] [--list]
 --check  fail (exit 1) when a generated file differs from the one on disk, or when a rule is broken (CI guard)
 --only   generate/validate one franchise file only (content authors use this)
WHY: see lines_lib.py. Rules enforced here (the build fails on any): 15 to 30 nodes a line, unique line ids/names/node names, only catalogue kinds and enum values,
numbers inside the catalogue's [min, max] at EVERY level, an active art never cooler than 3 s and a finisher never cooler than 45 s at its last level.
"""
import importlib.util
import json
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src/main/resources/data/nusmp/skill_trees")
sys.path.insert(0, HERE)
import catalog as C  # noqa: E402
from lines_lib import FRANCHISES  # noqa: E402

CLAMPED = []


def slug(s):
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


def clampv(spec, v):
    base, lo, hi, grow = spec
    return max(lo, min(hi, v))


def table(spec, base, levels, integer_hint):
    _, lo, hi, grow = spec
    out = []
    for lv in range(1, levels + 1):
        v = base * (1 + grow * (lv - 1)) if base != 0 else (hi * grow * (lv - 1) * 0.0 if False else 0.0)
        if base == 0 and grow != 0 and hi > 0:
            v = 0.0
        v = max(lo, min(hi, v))
        out.append(v)
    res = []
    for v in out:
        if integer_hint:
            res.append(float(int(round(v))))
        else:
            res.append(round(v, 4))
    return res


def is_int_param(name, spec):
    base = spec[0]
    return name in ("cooldown", "seconds", "seconds2", "targets", "jumps", "hits", "shots", "pierce", "interval", "delay", "rest", "max_stacks", "amp", "amp2", "resist_amp",
                    "regen_seconds", "resist_seconds", "mult_hits") or (float(base).is_integer() and abs(base) >= 3 and name not in ("range", "radius", "distance", "length", "damage", "amount", "height", "strength", "half_angle", "armor", "per_second"))


def build_values(kind_spec, params, levels, problems, where):
    values, strings = {}, {}
    for name, spec in kind_spec["params"].items():
        base = params.get(name, spec[0])
        if not isinstance(base, (int, float)):
            problems.append("%s: param %s must be a number" % (where, name))
            continue
        raw_last = float(base) * (1 + spec[3] * (levels - 1))
        if base != 0 and (raw_last > spec[2] + 1e-9 or raw_last < spec[1] - 1e-9):
            CLAMPED.append("%s: %s grows to %.3g at level %d but is capped to [%s, %s]" % (where, name, raw_last, levels, spec[1], spec[2]))
        values[name] = table(spec, float(base), levels, is_int_param(name, spec))
        for v in values[name]:
            if v < spec[1] - 1e-9 or v > spec[2] + 1e-9:
                problems.append("%s: %s=%s is outside [%s, %s]" % (where, name, v, spec[1], spec[2]))
    for name, allowed in kind_spec.get("enums", {}).items():
        v = params.get(name)
        if v is None:
            v = allowed[0] if name == "vfx" else None
        if v is None:
            problems.append("%s: needs the enum param '%s' (one of %s...)" % (where, name, ", ".join(allowed[:6])))
            continue
        if v not in allowed:
            problems.append("%s: %s='%s' is not allowed" % (where, name, v))
        strings[name] = v
    for name in params:
        if name not in kind_spec["params"] and name not in kind_spec.get("enums", {}):
            problems.append("%s: unknown param '%s' for this kind" % (where, name))
    return values, strings


def node(nid, name, desc, parents, costs, levels, ntype, kind, values, strings, x, y, icon, passive=False):
    n = {"id": nid, "name": name, "desc": desc, "parents": [{"node": p, "level": l} for p, l in parents], "cost": costs[0], "costs": costs, "max_level": levels,
         "type": ntype, "kind": kind}
    if strings:
        n["params"] = strings
    n["values"] = values
    n["x"], n["y"], n["icon"] = x, y, icon
    return n


_VANILLA = None


def check_icon(icon, where, problems):
    """Every icon must be a real 1.21.1 item (tools/lines/vanilla_items.txt, dumped from the API stubs)."""
    global _VANILLA
    if _VANILLA is None:
        path = os.path.join(HERE, "vanilla_items.txt")
        _VANILLA = set(open(path).read().split()) if os.path.exists(path) else set()
    if _VANILLA and icon.startswith("minecraft:") and icon[10:] not in _VANILLA:
        problems.append("%s: icon %s is not a vanilla 1.21.1 item" % (where, icon))


def build_line(L, problems):
    w = "line %s" % L.id
    if L.franchise not in FRANCHISES:
        problems.append("%s: unknown franchise %s" % (w, L.franchise))
    if not re.fullmatch(r"[a-z0-9_]+", L.id):
        problems.append("%s: id must be lower snake" % w)
    if L.background not in C.BACKGROUNDS:
        problems.append("%s: background must be one of %s" % (w, C.BACKGROUNDS))
    if not re.fullmatch(r"#[0-9a-fA-F]{6}", L.color):
        problems.append("%s: color must be #rrggbb" % w)
    if len(L.stances) != 2 or not 3 <= len(L.stats) <= 4 or not 6 <= len(L.arts) <= 8 or not 6 <= len(L.passives) <= 8 or len(L.finishers) != 2:
        problems.append("%s: needs 2 stances, 3-4 stats, 6-8 arts, 6-8 passives, 2 finishers (has %d/%d/%d/%d/%d)" % (w, len(L.stances), len(L.stats), len(L.arts), len(L.passives), len(L.finishers)))
        return None
    nodes, used = [], set()

    def nid(name):
        base = slug(name) or "node"
        i, cand = 2, base
        while cand in used:
            cand = "%s_%d" % (base, i)
            i += 1
        used.add(cand)
        return cand

    # --- column 0: the two stances (roots; the second wants the first at level 2)
    stance_ids = []
    for i, s in enumerate(L.stances):
        spec = C.STANCE["stance"]
        params = dict(s.params)
        vals, strs = build_values(spec, params, 3, problems, "%s stance %s" % (w, s.name))
        i_d = nid(s.name)
        stance_ids.append(i_d)
        parents = [] if i == 0 else [(stance_ids[0], 2)]
        nodes.append(node(i_d, s.name, s.desc, parents, [1, 2, 2], 3, "active_ability", "stance", vals, strs, 0, 2 + 4 * i, s.icon))
    # --- column 1: stats
    for i, st in enumerate(L.stats):
        spec = C.ATTR["attr"]
        vals, strs = build_values(spec, {"amount": st.amount, "attribute": st.attribute, "op": st.op}, 5, problems, "%s stat %s" % (w, st.name))
        parent = stance_ids[i % 2]
        nodes.append(node(nid(st.name), st.name, st.desc, [(parent, 1)], [1, 1, 2, 2, 3], 5, "attribute_modifier", "attr", vals, strs, 2, i * 2 + (0 if i % 2 == 0 else 1), st.icon))
    # --- column 2: arts in two lanes (lane A under stance 1, lane B under stance 2), each chained to the previous art of its lane at level 2
    art_ids, last_in_lane = [], {0: stance_ids[0], 1: stance_ids[1]}
    lane_y = {0: 0, 1: 0}
    for i, a in enumerate(L.arts):
        lane = i % 2
        if a.kind not in C.ACTIVE:
            problems.append("%s art %s: unknown active kind %s" % (w, a.name, a.kind))
            continue
        vals, strs = build_values(C.ACTIVE[a.kind], a.params, 4, problems, "%s art %s" % (w, a.name))
        cd = vals.get("cooldown", [100])
        if cd and cd[-1] < 60:
            problems.append("%s art %s: cooldown %s is under 3 s at its last level" % (w, a.name, cd[-1]))
        i_d = nid(a.name)
        art_ids.append(i_d)
        parent_level = 1 if last_in_lane[lane] in stance_ids else 2
        nodes.append(node(i_d, a.name, a.desc, [(last_in_lane[lane], parent_level)], [1 + i // 3, 2 + i // 3, 2 + i // 3, 3 + i // 3], 4, "active_ability", a.kind, vals, strs,
                          4 + 2 * (i // 2), lane * 6 + 0 if True else 0, a.icon))
        last_in_lane[lane] = i_d
    # --- column 3: passive synergy: passive j follows art j at level 2 (the player builds an art, then its synergy)
    pas_ids = []
    for j, p in enumerate(L.passives):
        if p.kind not in C.PASSIVE:
            problems.append("%s passive %s: unknown passive kind %s" % (w, p.name, p.kind))
            continue
        vals, strs = build_values(C.PASSIVE[p.kind], p.params, 4, problems, "%s passive %s" % (w, p.name))
        i_d = nid(p.name)
        pas_ids.append(i_d)
        parent = art_ids[j] if j < len(art_ids) else art_ids[-1]
        pn = node(i_d, p.name, p.desc, [(parent, 2)], [1 + j // 3, 2 + j // 3, 2 + j // 3, 3 + j // 3], 4, "passive_toggle", p.kind, vals, strs, 10 + 2 * (j // 2), j % 2, p.icon)
        if p.evolved:
            ev = {}
            spec_p = C.PASSIVE[p.kind]
            for name, v in p.evolved.items():
                if name in spec_p["params"]:
                    lo, hi = spec_p["params"][name][1], spec_p["params"][name][2]
                    if not isinstance(v, (int, float)) or v < lo - 1e-9 or v > hi + 1e-9:
                        problems.append("%s passive %s: evolved %s=%s is outside [%s, %s]" % (w, p.name, name, v, lo, hi))
                elif name in spec_p.get("enums", {}):
                    if v not in spec_p["enums"][name]:
                        problems.append("%s passive %s: evolved %s='%s' is not allowed" % (w, p.name, name, v))
                else:
                    problems.append("%s passive %s: evolved param '%s' is unknown for this kind" % (w, p.name, name))
                ev[name] = v
            pn["evolved"] = ev
        nodes.append(pn)
    # --- column 4: finishers need three nodes of the two lanes
    for k, f in enumerate(L.finishers):
        if f.kind not in C.ACTIVE:
            problems.append("%s finisher %s: unknown active kind %s" % (w, f.name, f.kind))
            continue
        vals, strs = build_values(C.ACTIVE[f.kind], f.params, 3, problems, "%s finisher %s" % (w, f.name))
        cd = vals.get("cooldown", [100])
        if cd and cd[-1] < 900:
            problems.append("%s finisher %s: cooldown %s is under 45 s at its last level" % (w, f.name, cd[-1]))
        lane_last = [x for x in art_ids if x][-1 - k] if len(art_ids) > k else art_ids[-1]
        parents = [(lane_last, 3), (pas_ids[-1 - k], 2)]
        fn = node(nid(f.name), f.name, f.desc, parents, [4, 5, 6], 3, "active_ability", f.kind, vals, strs, 16, 3 + 4 * k, f.icon)
        if f.ultimate:
            fn["ultimate"] = True
        nodes.append(fn)
    ults = [f for f in L.finishers if f.ultimate]
    if L.evolution is None and ults:
        problems.append("%s: has an ultimate finisher but no evolution block" % w)
    if L.evolution is not None:
        ev = L.evolution
        if set(ev.keys()) - {"requirement", "count", "text", "ultimate_name", "params"} or not all(k in ev for k in ("requirement", "count", "text", "ultimate_name")):
            problems.append("%s: evolution needs exactly requirement, count, text, ultimate_name (and optional params)" % w)
        elif ev["requirement"] not in C.EVOLUTION:
            problems.append("%s: unknown evolution requirement %s" % (w, ev["requirement"]))
        elif not isinstance(ev["count"], int) or not 1 <= ev["count"] <= C.EVOLUTION_MAX_COUNT:
            problems.append("%s: evolution count must be 1..%d" % (w, C.EVOLUTION_MAX_COUNT))
        else:
            for name, v in ev.get("params", {}).items():
                if v not in C.EVOLUTION[ev["requirement"]].get("enums", {}).get(name, []):
                    problems.append("%s: evolution param %s='%s' is not allowed" % (w, name, v))
        if len(ults) != 1 or not L.finishers[-1].ultimate:
            problems.append("%s: exactly one finisher, the LAST one, must be the ultimate" % w)
    check_icon(L.icon, w, problems)
    for n in nodes:
        check_icon(n["icon"], "%s node %s" % (w, n["id"]), problems)
    if not 15 <= len(nodes) <= 30:
        problems.append("%s: has %d nodes (15 to 30)" % (w, len(nodes)))
    # layout fix: arrange y so no two nodes of a column share a cell
    col = {}
    for n in nodes:
        key = n["x"]
        col.setdefault(key, []).append(n)
    for key, lst in col.items():
        for idx, n in enumerate(sorted(lst, key=lambda z: (z["y"], z["id"]))):
            n["y"] = idx * 2
    route = {
        "magic": L.franchise, "franchise": L.franchise, "name": L.name, "desc": L.desc,
        "book": "nusmp:%s_book" % L.id, "book_name": L.book_name, "book_lore": L.book_lore,
        "color": L.color.upper(), "icon": L.icon, "class_id": L.id, "duplicate_sp": 3, "loot_weight": L.loot_weight,
        "book_texture": "nusmp:item/skill_book_3d/%s" % L.franchise,
        "nodes": nodes,
    }
    if L.evolution is not None:
        route["evolution"] = dict(L.evolution)
    return route


def load_rows(only=None):
    lines = []
    d = os.path.join(HERE, "rows")
    for f in sorted(os.listdir(d)):
        if not f.endswith(".py") or f.startswith("_"):
            continue
        if only and f[:-3] != only:
            continue
        spec = importlib.util.spec_from_file_location("rows_" + f[:-3], os.path.join(d, f))
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        for L in mod.LINES:
            lines.append((f[:-3], L))
    return lines


def dump_catalog():
    """tools/lines/catalog.json: the vocabulary for the Java side (a test pins executors to it) and for the GUI background generator."""
    def spec(group):
        return {k: {"params": {n: {"base": v[0], "min": v[1], "max": v[2], "grow": v[3]} for n, v in d["params"].items()},
                    "enums": {n: a for n, a in d.get("enums", {}).items()}, "desc": d["desc"]} for k, d in group.items()}
    data = {"active": spec(C.ACTIVE), "passive": spec(C.PASSIVE), "stance": spec(C.STANCE), "attr": spec(C.ATTR), "backgrounds": C.BACKGROUNDS,
            "franchises": FRANCHISES, "vfx": C.VFX, "evolution": {k: {"desc": v["desc"], "enums": v.get("enums", {})} for k, v in C.EVOLUTION.items()}}
    text = json.dumps(data, indent=1, ensure_ascii=False) + "\n"
    path = os.path.join(HERE, "catalog.json")
    if "--check" in sys.argv and os.path.exists(path) and open(path, encoding="utf-8").read() != text:
        print("catalog.json is stale")
        sys.exit(1)
    if "--check" not in sys.argv:
        open(path, "w", encoding="utf-8").write(text)


def main():
    dump_catalog()
    args = sys.argv[1:]
    only = args[args.index("--only") + 1] if "--only" in args else None
    check = "--check" in args
    problems, files = [], {}
    names, ids, dup_names = {}, set(), []
    rows = load_rows(only)
    for fname, L in rows:
        if L.id in ids:
            problems.append("duplicate line id %s" % L.id)
        ids.add(L.id)
        if fname != L.franchise:
            problems.append("line %s is in rows/%s.py but its franchise is %s" % (L.id, fname, L.franchise))
        route = build_line(L, problems)
        if route is None:
            continue
        for n in route["nodes"]:
            key = n["name"].lower()
            if key in names and names[key] != L.id:
                dup_names.append("node name '%s' used in %s and %s" % (n["name"], names[key], L.id))   # allowed across lines (a "Shield Wall" in two lines is fine); counted and listed with --names
            names[key] = L.id
        if L.name.lower() in names and names[L.name.lower()] != L.id:
            problems.append("line name '%s' clashes" % L.name)
        files[L.id] = json.dumps(route, indent=2, ensure_ascii=False) + "\n"
    # variety guard: no two lines may have the same ordered kinds of arts, nor the same set of passive kinds
    sig_a, sig_p = {}, {}
    for fname, L in rows:
        a = tuple(x.kind for x in L.arts)
        p = tuple(sorted(x.kind for x in L.passives))
        if a in sig_a:
            problems.append("lines %s and %s have the same art kinds in the same order" % (sig_a[a], L.id))
        if p in sig_p:
            print("note: lines %s and %s have the same set of passive kinds (allowed: their numbers and enums differ)" % (sig_p[p], L.id))
        sig_a[a], sig_p[p] = L.id, L.id
        if len(set(x.kind for x in L.arts)) < 5 or len(set(x.kind for x in L.passives)) < 5:
            problems.append("line %s uses fewer than 5 different art kinds or fewer than 5 different passive kinds" % L.id)
    if "--list" in args:
        for fname, L in rows:
            print(fname, L.id, "|", L.name)
    per = {}
    for fname, L in rows:
        per[fname] = per.get(fname, 0) + 1
    print("lines:", len(files), "by franchise:", per)
    if CLAMPED:
        print("note: %d values are capped by the catalogue range at their last level (harmless plateaus; list with --clamped)" % len(CLAMPED))
        if "--clamped" in args:
            for c in CLAMPED:
                print("   ", c)
    if dup_names:
        print("note: %d node names are shared between two lines (allowed; list with --names)" % len(dup_names))
        if "--names" in args:
            for d in dup_names:
                print("   ", d)
    if problems:
        print("PROBLEMS (%d):" % len(problems))
        for p in problems[:80]:
            print("  -", p)
        sys.exit(1)
    changed = 0
    for lid, text in files.items():
        p = os.path.join(OUT, lid + ".json")
        old = open(p, encoding="utf-8").read() if os.path.exists(p) else None
        if old != text:
            changed += 1
            if check:
                print("DIFFERS:", lid)
            else:
                open(p, "w", encoding="utf-8").write(text)
    if check and changed:
        sys.exit(1)
    print("ok: %d files %s" % (len(files), "checked" if check else "written (%d changed)" % changed))


if __name__ == "__main__":
    main()
