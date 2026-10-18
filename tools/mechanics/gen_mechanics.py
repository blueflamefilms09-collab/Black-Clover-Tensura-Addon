#!/usr/bin/env python3
"""Why: the dungeon mechanics library (1000 mechanics) is generated, not hand-written. Writes the library: data/nusmp/mega/mechanics/<category>.json + ops.json, tools/mechanics/lang_fragment.json and the base-design table
of docs/labyrinth/mechanics.md.

    python3 tools/mechanics/gen_mechanics.py            # writes
    python3 tools/mechanics/gen_mechanics.py --check    # exit 1 when a file on disk differs, or a guard fails
    python3 tools/mechanics/gen_mechanics.py --stats    # counts per category / source / trigger / counter / primitive

Why a generator: a thousand mechanics cannot be hand-written without becoming renamed copies. About 150 BASE designs (tables/<category>.py) are written by hand; each carries a few
ELEMENT, TRIGGER and COUNTER alternatives, and this script expands every base into itself plus variants that change something the player notices (what it is made of, what
sets it off, how it is beaten). The guards below FAIL the run when two mechanics share a behaviour signature (primitives + trigger + counter + bucketed numbers), a name or a
description, when a source is missing or too common, or when an argument is outside the catalogue (tables/ops.py). The same signature rule is in Java (Signature.java) and
a test compares the two."""
import hashlib, importlib, json, math, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(HERE, "tables"))
import ops as OPS_MOD            # noqa: E402
from dsl import CATEGORIES, THEMES   # noqa: E402

OUT = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "mechanics")
LANG = os.path.join(HERE, "lang_fragment.json")
LANG_STATIC = os.path.join(HERE, "lang_static.json")
DOC = os.path.join(ROOT, "docs", "labyrinth", "mechanics.md")
PER_CATEGORY = int(os.environ.get("MECH_PER", "125"))
MAX_SHARE = 0.08
MIN_ANIME_SOURCES = 30

REWARD_DEFAULT = {"floor": "none", "puzzle": "loot", "hazard": "xp", "combat": "xp", "risk": "loot", "boss": "none", "travel": "mana", "party": "heal"}

# ---------------------------------------------------------------------------------------------------------- words for the swapped triggers and counters
# kind -> (label used in the name, clause used in the description, default arguments)
TRIG_WORDS = {
    "floor_enter": ("Omen", "It strikes once, the moment the area is first entered.", {}),
    "area_enter": ("Threshold", "It strikes every time someone steps back into the area.", {"cooldown": 300}),
    "timer": ("Clockwork", "It runs on a timer for as long as anyone stays.", {"every": (300, -30)}),
    "proximity": ("Tripwire", "It springs when someone comes within a few blocks of its heart.", {"r": 4, "cooldown": 160}),
    "sight": ("Gaze", "It wakes when someone looks straight at it.", {"range": 16, "cooldown": 160}),
    "kill": ("Bloodprice", "It answers the dungeon's deaths: every few kills in the area set it off.", {"n": 3}),
    "hp_below": ("Wounded", "It smells a wounded delver and strikes when someone drops under half health.", {"frac": 0.5, "cooldown": 300}),
    "item_use": ("Greed", "It reacts to the use of items in its area.", {"item": "any", "cooldown": 160}),
    "interact": ("Altar", "It needs a hand on its altar block to begin.", {"cooldown": 100}),
    "damage_taken": ("Backlash", "It answers every real wound dealt in its area.", {"min": 2, "cooldown": 100}),
    "sprint": ("Footfall", "Running in its area sets it off.", {"ticks": 30, "cooldown": 160}),
    "idle": ("Stillness", "Standing still in its area for a few seconds sets it off.", {"ticks": 100, "cooldown": 160}),
    "sneak": ("Hush", "Crouching in its area for a few seconds sets it off.", {"ticks": 60, "cooldown": 160}),
    "jump": ("Leap", "Jumping in its area sets it off.", {"n": 3, "cooldown": 160}),
    "boss_hp": ("Wound of the Lord", "It stirs when the boss falls under a fraction of its health.", {"frac": 0.5}),
    "party_size": ("Quorum", "It only wakes when several delvers stand together in the area.", {"n": 2, "cooldown": 300}),
    "death": ("Last Breath", "It answers a delver's death in its area.", {"cooldown": 200}),
    "dark": ("Nightfall", "It thrives where the light is low and strikes the delver who lingers in the dark.", {"light": 4, "ticks": 60, "cooldown": 160}),
}
CTR_WORDS = {
    "none": ("Unbound", "It cannot be beaten, only survived or left behind.", {}),
    "kill_all": ("Warded", "It is held up by wardens that spawn with it; kill them all to end it.", {"group": "undead", "count": 3}),
    "kill_marked": ("Keystone", "One marked warden holds it up; slay the keystone to end it.", {"group": "knight"}),
    "item_offer": ("Tribute", "Lay an offering on its altar to quiet it.", {"item": "minecraft:gold_ingot", "count": 1}),
    "stand_plate": ("Anchored", "Hold a marked plate for a few seconds to still it.", {"secs": 6}),
    "sneak_through": ("Creeping", "Sneak through its area long enough and it loses interest.", {"secs": 8}),
    "hold_light": ("Lantern-bearer", "Carry a light through it long enough and it relents.", {"secs": 8}),
    "sequence": ("Sequenced", "Use its pedestals in the right order to end it.", {"steps": 3}),
    "lever": ("Switch", "A lever somewhere in the area switches it off.", {}),
    "survive": ("Endurance", "Last out the time and it ends by itself.", {"secs": 30}),
    "all_present": ("Muster", "Every living member of the party must stand on the plate together.", {"secs": 4}),
    "split_plates": ("Split", "Two delvers must hold two plates at once.", {"n": 2}),
    "carry": ("Courier", "Carry the key item from one pedestal to the other.", {"item": "minecraft:amethyst_shard"}),
    "no_damage": ("Untouched", "Take no damage for a while and it lets go.", {"secs": 20}),
    "quiet": ("Muffled", "Neither sprint nor jump for a while and it lets go.", {"secs": 15}),
    "pay_xp": ("Soul-tithe", "Pay a few levels of experience at the altar to end it.", {"levels": 3}),
    "pay_hp": ("Blood-tithe", "Pay in hearts at the altar to end it.", {"hearts": 3}),
    "pay_item": ("Toll", "Pay the price in items at the altar.", {"item": "minecraft:iron_ingot", "count": 4}),
    "pillars_sync": ("Synchrony", "Light every pillar within a few seconds of each other.", {"secs": 4, "steps": 3}),
    "mirror": ("Echo", "Repeat the pattern it shows.", {"steps": 3}),
}


# ---------------------------------------------------------------------------------------------------------- helpers
def norm(v):
    """Python values -> JSON values: (a, b) -> [a, b], whole floats -> ints."""
    if isinstance(v, bool): return v
    if isinstance(v, tuple): return [norm(x) for x in v]
    if isinstance(v, float) and v == int(v) and abs(v) < 1e9: return int(v)
    return v


def norm_obj(d):
    return {k: norm(v) for k, v in d.items()}


def slug(s):
    return re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")


def s_of(floor):
    return (min(1000, max(1, abs(floor))) - 1) / 200.0


def val_at(v, floor):
    if isinstance(v, (list, tuple)): return v[0] + v[1] * s_of(floor)
    return v


LADDER = [0.0, 0.25, 0.5, 1, 1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 2048, 4096]


def bucket(x):
    m = abs(x)
    n = 0
    for t in LADDER:
        if m > t: n += 1
        else: break
    return -n if x < 0 else n


def spec_defaults(specs):
    return {s["n"]: s["d"] for s in specs}


def sig_args(args, specs):
    names = sorted(set(args.keys()) | {s["n"] for s in specs})
    dflt = spec_defaults(specs)
    parts = []
    for n in names:
        v = args[n] if n in args else dflt.get(n)
        if v is None: continue
        if isinstance(v, bool): parts.append("%s=%s" % (n, "T" if v else "F"))
        elif isinstance(v, (list, tuple)): parts.append("%s=%d:%d" % (n, bucket(val_at(v, 1)), bucket(val_at(v, 1000))))
        elif isinstance(v, (int, float)): parts.append("%s=%d:%d" % (n, bucket(v), bucket(v)))
        else: parts.append("%s=%s" % (n, v))
    return ",".join(parts)


def signature(m):
    t = m["trigger"]; c = m["counter"]
    tspec = OPS_MOD.TRIGGERS[t["kind"]]["args"]; cspec = OPS_MOD.COUNTERS[c["kind"]]["args"]
    out = "%s;t:%s(%s);c:%s(%s);r:%s" % (m["scope"], t["kind"], sig_args({k: v for k, v in t.items() if k != "kind"}, tspec), c["kind"],
                                          sig_args({k: v for k, v in c.items() if k != "kind"}, cspec), m["reward"])
    for e in m["effects"]:
        spec = OPS_MOD.OPS[e["op"]]["args"]
        args = {k: v for k, v in e.items() if k not in ("op", "delay", "chance", "times", "every", "if", "on")}
        out += ";e:%s[%d,%d,%d,%d,%s,%s](%s)" % (e["op"], bucket(e.get("delay", 0)), bucket(e.get("chance", 1.0) * 10.0), e.get("times", 1), bucket(e.get("every", 20)), e.get("if", ""), e.get("on", ""), sig_args(args, spec))
    return out


def sig_hash(sig):
    return hashlib.sha1(sig.encode("utf-8")).hexdigest()[:12]


def check_args(where, args, specs, problems):
    byname = {s["n"]: s for s in specs}
    for k, v in args.items():
        s = byname.get(k)
        if s is None:
            problems.append("%s: unknown argument '%s'" % (where, k)); continue
        if s["t"] == "num":
            if isinstance(v, bool) or not (isinstance(v, (int, float, list, tuple))):
                problems.append("%s: '%s' must be numeric" % (where, k)); continue
            for fl in (1, 1000):
                x = val_at(v, fl)
                if x < s.get("lo", -1e18) - 1e-9 or x > s.get("hi", 1e18) + 1e-9:
                    problems.append("%s: '%s'=%s at floor %d outside %s..%s" % (where, k, x, fl, s.get("lo"), s.get("hi"))); break
        elif s["t"] == "bool":
            if not isinstance(v, bool): problems.append("%s: '%s' must be bool" % (where, k))
        else:
            if not isinstance(v, str): problems.append("%s: '%s' must be a string" % (where, k))
            elif "e" in s and v not in s["e"]: problems.append("%s: '%s'='%s' not in %s" % (where, k, v, s["e"]))


# ---------------------------------------------------------------------------------------------------------- expansion
def apply_element(fx, el):
    fx = [dict(e) for e in fx]
    for i, patch in el.set.items():
        fx[i].update(patch)
    for i, (opname, patch) in el.swap.items():
        keep = {k: v for k, v in fx[i].items() if k in ("delay", "chance", "times", "every", "if", "on")}
        fx[i] = {"op": opname}
        fx[i].update(keep)
        fx[i].update(patch)
    for e in el.add:
        fx.append(dict(e))
    for i in sorted(el.drop, reverse=True):
        del fx[i]
    for e in fx:
        if "frm" in e: e["from"] = e.pop("frm")          # `from` is a Python keyword: tables write frm
    return fx


def trig_alt(entry):
    """entry: kind | (kind, args) | (kind, args, label, clause)"""
    if isinstance(entry, str): entry = (entry, {})
    kind, args = entry[0], entry[1]
    label, clause, dflt = TRIG_WORDS[kind]
    if len(entry) > 2: label, clause = entry[2], entry[3]
    d = {"kind": kind}; d.update(dflt); d.update(args)
    return d, label, clause


def ctr_alt(entry):
    if isinstance(entry, str): entry = (entry, {})
    kind, args = entry[0], entry[1]
    label, clause, dflt = CTR_WORDS[kind]
    if len(entry) > 2: label, clause = entry[2], entry[3]
    d = {"kind": kind}; d.update(dflt); d.update(args)
    return d, label, clause


def combos(ne, nt, nc):
    """The variant choices (e, t, c), each None or an index: single changes first, round-robin over the three dimensions, then pairs, then triples."""
    singles = []
    for i in range(max(ne, nt, nc)):
        if i < ne: singles.append((i, None, None))
        if i < nt: singles.append((None, i, None))
        if i < nc: singles.append((None, None, i))
    pairs = []
    for e in range(ne):
        for t in range(nt): pairs.append((e, t, None))
    for e in range(ne):
        for c in range(nc): pairs.append((e, None, c))
    for t in range(nt):
        for c in range(nc): pairs.append((None, t, c))
    triples = [(e, t, c) for e in range(ne) for t in range(nt) for c in range(nc)]
    # interleave the pairs so that early variants do not all share one element
    pairs.sort(key=lambda p: ((p[0] or 0) + (p[1] or 0) + (p[2] or 0), tuple(-1 if x is None else x for x in p)))
    return singles + pairs + triples


def entries_for(index, nbases, target):
    q, r = divmod(target, nbases)
    return q + 1 if index < r else q


def expand(cat, bases):
    out = []
    n = len(bases)
    for bi, b in enumerate(bases):
        b.cat = cat
        want = entries_for(bi, n, PER_CATEGORY)
        base_m = build(cat, b, None, None, None)
        out.append(base_m)
        have = 1
        for (ei, ti, ci) in combos(len(b.elems), len(b.trigs), len(b.ctrs)):
            if have >= want: break
            out.append(build(cat, b, ei, ti, ci))
            have += 1
        if have < want:
            raise SystemExit("%s.%s: needs %d entries but its alternatives give only %d (add elements, triggers or counters)" % (cat, b.key, want, have))
    return out


def build(cat, b, ei, ti, ci):
    parts_k, parts_l, clauses = [], [], []
    fx = [dict(e) for e in b.fx]
    trig = dict(b.trig); ctr = dict(b.ctr)
    tiers, road, themes, cost, flavor = b.tiers, b.road, list(b.themes), b.cost, b.flavor
    if ei is not None:
        el = b.elems[ei]
        fx = apply_element(fx, el)
        parts_k.append("e_" + el.key); parts_l.append(el.label); clauses.append(el.clause)
        if el.tiers: tiers = el.tiers
        if el.road: road = el.road
        if el.themes is not None: themes = list(el.themes)
        cost += el.cost
        if el.flavor: flavor = el.flavor
        if el.trig: trig = dict(el.trig)
        if el.ctr: ctr = dict(el.ctr)
    if ti is not None:
        trig, label, clause = trig_alt(b.trigs[ti])
        parts_k.append("t_" + trig["kind"]); parts_l.append(label); clauses.append(clause)
    if ci is not None:
        ctr, label, clause = ctr_alt(b.ctrs[ci])
        parts_k.append("c_" + ctr["kind"]); parts_l.append(label); clauses.append(clause)
    if b.ctr["kind"] == "none" and ctr["kind"] != "none":
        for e in fx:                                     # a price was added to a design that had none: its rewards wait behind the price
            if e["op"] in ("spawn_chest", "give_loot", "xp_orbs") and "on" not in e: e["on"] = "solve"
    if not (ei is not None and b.elems[ei].tiers):
        lo, hi = tiers                                   # dearer mechanics start deeper, trivial ones end sooner (an explicit tiers= on a card or an element wins)
        if cost >= 7: lo = max(lo, 3)
        elif cost >= 5: lo = max(lo, 2)
        if cost <= 1: hi = min(hi, 3)
        elif cost <= 2: hi = min(hi, 4)
        tiers = (min(lo, hi), hi)
    mid = "%s.%s" % (cat, b.key) + ("." + ".".join(parts_k) if parts_k else "")
    name = b.name + (": " + " / ".join(parts_l) if parts_l else "")
    desc = b.desc + ((" " + " ".join(clauses)) if clauses else "")
    reward = "none" if ctr["kind"] == "none" else (b.reward or REWARD_DEFAULT[cat])
    m = {"id": mid, "name": name, "desc": desc, "flavor": flavor, "cat": cat, "src": b.src, "base": b.key, "scope": b.scope, "tiers": list(tiers), "road": road,
         "themes": themes, "cost": max(1, min(12, cost)), "weight": b.weight if ei is None and ti is None and ci is None else max(1, int(b.weight * 0.7)),
         "trigger": norm_obj(trig), "effects": [norm_obj(e) for e in fx], "counter": norm_obj(ctr), "reward": reward}
    m["sig"] = sig_hash(signature(m))
    return m


# ---------------------------------------------------------------------------------------------------------- guards
def guard(all_m, bases_by_cat, partial=False):
    problems = []
    seen_id, seen_name, seen_desc, seen_sig = {}, {}, {}, {}
    for m in all_m:
        for key, seen in (("id", seen_id), ("name", seen_name), ("desc", seen_desc), ("sig", seen_sig)):
            k = m[key].lower() if key in ("name", "desc") else m[key]
            if k in seen: problems.append("duplicate %s: %s and %s" % (key, seen[k], m["id"]))
            else: seen[k] = m["id"]
        if not m["src"].strip(): problems.append(m["id"] + ": empty source")
        for t in m["themes"]:
            if t not in THEMES: problems.append("%s: unknown theme %s" % (m["id"], t))
        if m["scope"] not in ("floor", "room", "zone", "boss"): problems.append(m["id"] + ": bad scope")
        if m["road"] not in ("both", "holy", "abyssal"): problems.append(m["id"] + ": bad road")
        if not (1 <= m["tiers"][0] <= m["tiers"][1] <= 5): problems.append(m["id"] + ": bad tiers")
        t = m["trigger"]
        if t["kind"] not in OPS_MOD.TRIGGERS: problems.append("%s: unknown trigger %s" % (m["id"], t["kind"]))
        else: check_args(m["id"] + " trigger", {k: v for k, v in t.items() if k != "kind"}, OPS_MOD.TRIGGERS[t["kind"]]["args"], problems)
        c = m["counter"]
        if c["kind"] not in OPS_MOD.COUNTERS: problems.append("%s: unknown counter %s" % (m["id"], c["kind"]))
        else: check_args(m["id"] + " counter", {k: v for k, v in c.items() if k != "kind"}, OPS_MOD.COUNTERS[c["kind"]]["args"], problems)
        if t["kind"] == "boss_hp" and m["scope"] != "boss": problems.append(m["id"] + ": boss_hp trigger outside a boss mechanic")
        for i, e in enumerate(m["effects"]):
            if e.get("on", "solve") != "solve" or (e.get("on") == "solve" and m["counter"]["kind"] == "none"): problems.append("%s effect %d: bad on-solve" % (m["id"], i))
            if "if" in e and not re.match(r"^\s*[a-z0-9_]+\s*(>=|<=|==|!=|>|<)\s*-?\d+\s*$", e["if"]): problems.append("%s effect %d: bad condition %r" % (m["id"], i, e["if"]))
            if e["op"] not in OPS_MOD.OPS: problems.append("%s: unknown primitive %s" % (m["id"], e["op"])); continue
            check_args("%s effect %d (%s)" % (m["id"], i, e["op"]), {k: v for k, v in e.items() if k not in ("op", "delay", "chance", "times", "every", "if", "on")}, OPS_MOD.OPS[e["op"]]["args"], problems)
    if partial: return problems
    total = len(all_m)
    per_cat = {}
    for m in all_m: per_cat[m["cat"]] = per_cat.get(m["cat"], 0) + 1
    for c in CATEGORIES:
        if per_cat.get(c, 0) != PER_CATEGORY: problems.append("category %s has %d mechanics, not %d" % (c, per_cat.get(c, 0), PER_CATEGORY))
    per_src = {}
    for m in all_m: per_src[m["src"]] = per_src.get(m["src"], 0) + 1
    for s, n in per_src.items():
        if n > MAX_SHARE * total + 1e-9: problems.append("source %s has %d mechanics (%.1f%%), more than %d%%" % (s, n, 100.0 * n / total, MAX_SHARE * 100))
    anime = [s for s in per_src if not s.startswith("D&D")]
    if len(anime) < MIN_ANIME_SOURCES: problems.append("only %d anime sources, need %d" % (len(anime), MIN_ANIME_SOURCES))
    used_ops = {e["op"] for m in all_m for e in m["effects"]}
    for o in OPS_MOD.OPS:
        if o not in used_ops: problems.append("primitive '%s' is used by no mechanic" % o)
    used_trig = {m["trigger"]["kind"] for m in all_m}
    for k in OPS_MOD.TRIGGERS:
        if k not in used_trig: problems.append("trigger '%s' is used by no mechanic" % k)
    used_ctr = {m["counter"]["kind"] for m in all_m}
    for k in OPS_MOD.COUNTERS:
        if k not in used_ctr: problems.append("counter '%s' is used by no mechanic" % k)
    return problems


# ---------------------------------------------------------------------------------------------------------- output
def dumps_category(cat, ms):
    why = "Why: the %s mechanics of the dungeon mechanics library (labyrinth/mega/mechanics), generated by tools/mechanics/gen_mechanics.py from tools/mechanics/tables/%s.py; do not edit by hand, edit the tables and regenerate." % (cat, cat)
    lines = ['{"_why":%s,"category":%s,"count":%d,"mechanics":[' % (json.dumps(why), json.dumps(cat), len(ms))]
    for i, m in enumerate(ms):
        lines.append("  " + json.dumps(m, ensure_ascii=False, separators=(",", ":")) + ("," if i + 1 < len(ms) else ""))
    lines.append("]}")
    return "\n".join(lines) + "\n"


def ops_json(all_m):
    counts = {c: sum(1 for m in all_m if m["cat"] == c) for c in CATEGORIES}
    o = {"_why": "Why: the vocabulary of the dungeon mechanics library: every effect primitive, trigger and counter with its arguments, ranges, defaults and the exclusive channels it occupies; generated from tools/mechanics/tables/ops.py by gen_mechanics.py. The Java OpCatalog validates every mechanic against it.", "version": 1, "total": len(all_m), "counts": counts, "ops": {}, "triggers": {}, "counters": {}, "exclusive": sorted(set(OPS_MOD.CHANNEL_EXCLUSIVE)), "effectChannels": OPS_MOD.EFFECT_CHANNELS}
    for k, v in OPS_MOD.OPS.items():
        o["ops"][k] = {"group": v["group"], "channels": v["channels"], "sustained": v["sustained"], "args": v["args"], "doc": v["doc"]}
    for k, v in OPS_MOD.TRIGGERS.items(): o["triggers"][k] = {"args": v["args"], "doc": v["doc"]}
    for k, v in OPS_MOD.COUNTERS.items(): o["counters"][k] = {"args": v["args"], "props": v["props"], "doc": v["doc"]}
    return json.dumps(o, ensure_ascii=False, indent=1) + "\n"


def lang_json(all_m):
    d = {}
    if os.path.exists(LANG_STATIC):
        with open(LANG_STATIC, encoding="utf-8") as f: d.update(json.load(f))
    for m in all_m:
        d["mechanic.nusmp.%s.name" % m["id"]] = m["name"]
        d["mechanic.nusmp.%s.desc" % m["id"]] = m["desc"]
        d["mechanic.nusmp.%s.flavor" % m["id"]] = m["flavor"]
    for c in CATEGORIES:
        pass
    return json.dumps(d, ensure_ascii=False, indent=1) + "\n"


BEGIN, END = "<!-- BASES:BEGIN -->", "<!-- BASES:END -->"


def doc_table(bases_by_cat, all_m):
    per_src = {}
    for m in all_m: per_src[m["src"]] = per_src.get(m["src"], 0) + 1
    lines = [BEGIN, "", "Generated by `tools/mechanics/gen_mechanics.py`; do not edit by hand.", ""]
    lines.append("### Counts")
    lines.append("")
    lines.append("| category | bases | mechanics |")
    lines.append("|---|---|---|")
    for c in CATEGORIES:
        lines.append("| %s | %d | %d |" % (c, len(bases_by_cat[c]), sum(1 for m in all_m if m["cat"] == c)))
    lines.append("| total | %d | %d |" % (sum(len(b) for b in bases_by_cat.values()), len(all_m)))
    lines.append("")
    lines.append("### Sources (%d)" % len(per_src))
    lines.append("")
    lines.append(", ".join("%s (%d)" % (s, n) for s, n in sorted(per_src.items(), key=lambda x: (-x[1], x[0]))))
    lines.append("")
    lines.append("### The base designs")
    lines.append("")
    for c in CATEGORIES:
        lines.append("#### %s" % c)
        lines.append("")
        lines.append("| base | name | source | scope | trigger | counter | primitives |")
        lines.append("|---|---|---|---|---|---|---|")
        for b in bases_by_cat[c]:
            lines.append("| `%s` | %s | %s | %s | %s | %s | %s |" % (b.key, b.name, b.src, b.scope, b.trig["kind"], b.ctr["kind"], ", ".join(e["op"] for e in b.fx)))
        lines.append("")
    lines.append(END)
    return "\n".join(lines)


OPS_BEGIN, OPS_END = "<!-- OPS:BEGIN -->", "<!-- OPS:END -->"


def fmt_args(args):
    out = []
    for a in args:
        d = a["d"]
        out.append("%s=%s" % (a["n"], ("%g" % d) if isinstance(d, float) else str(d).lower() if isinstance(d, bool) else d))
    return ", ".join(out)


def doc_ops():
    """The primitive, trigger and counter catalogue as three tables (written by the generator from tables/ops.py)."""
    lines = [OPS_BEGIN, "", "Generated by `tools/mechanics/gen_mechanics.py` from `tools/mechanics/tables/ops.py`; do not edit by hand.", ""]
    lines += ["### The %d effect primitives" % len(OPS_MOD.OPS), "", "| id | group | arguments (defaults) | channels | sustained | what it does |", "|---|---|---|---|---|---|"]
    for k, v in OPS_MOD.OPS.items():
        lines.append("| `%s` | %s | %s | %s | %s | %s |" % (k, v["group"], fmt_args(v["args"]), ", ".join(v["channels"]) or "-", "yes" if v["sustained"] else "", v["doc"]))
    lines += ["", "### The %d triggers" % len(OPS_MOD.TRIGGERS), "", "| id | arguments (defaults) | fires when |", "|---|---|---|"]
    for k, v in OPS_MOD.TRIGGERS.items(): lines.append("| `%s` | %s | %s |" % (k, fmt_args(v["args"]), v["doc"]))
    lines += ["", "### The %d counters" % len(OPS_MOD.COUNTERS), "", "| id | arguments (defaults) | the engine places | how it is beaten |", "|---|---|---|---|"]
    for k, v in OPS_MOD.COUNTERS.items(): lines.append("| `%s` | %s | %s | %s |" % (k, fmt_args(v["args"]), v["props"], v["doc"]))
    lines += ["", "Exclusive channels (two mechanics sharing one are never stacked): " + ", ".join("`%s`" % c for c in sorted(set(OPS_MOD.CHANNEL_EXCLUSIVE))) + ".", "", OPS_END]
    return "\n".join(lines)


def doc_text(table):
    with open(DOC, encoding="utf-8") as f: text = f.read()
    ops = doc_ops()
    if OPS_BEGIN in text and OPS_END in text:
        a = text.index(OPS_BEGIN); b = text.index(OPS_END) + len(OPS_END)
        text = text[:a] + ops + text[b:]
    if BEGIN in text and END in text:
        a = text.index(BEGIN); b = text.index(END) + len(END)
        return text[:a] + table + text[b:]
    return text.rstrip("\n") + "\n\n## The base designs\n\n" + table + "\n"


def load_tables():
    by_cat = {}
    for c in CATEGORIES:
        p = os.path.join(HERE, "tables", c + ".py")
        if not os.path.exists(p): by_cat[c] = []; continue
        mod = importlib.import_module(c)
        by_cat[c] = mod.BASES
        keys = set()
        for b in by_cat[c]:
            if b.key in keys: raise SystemExit("duplicate base key %s.%s" % (c, b.key))
            keys.add(b.key)
    return by_cat


def main():
    check = "--check" in sys.argv
    by_cat = load_tables()
    all_m = []
    files = {}
    for c in CATEGORIES:
        ms = expand(c, by_cat[c]) if by_cat[c] else []
        all_m.extend(ms)
        files[os.path.join(OUT, c + ".json")] = dumps_category(c, ms)
    files[os.path.join(OUT, "ops.json")] = ops_json(all_m)
    files[LANG] = lang_json(all_m)
    problems = guard(all_m, by_cat, "--partial" in sys.argv)
    if "--stats" in sys.argv:
        stats(all_m)
    if problems:
        print("\n".join(problems[:80]))
        print("%d problem(s)" % len(problems))
        sys.exit(1)
    files[DOC] = doc_text(doc_table(by_cat, all_m))
    if check:
        bad = 0
        for p, text in files.items():
            cur = open(p, encoding="utf-8").read() if os.path.exists(p) else None
            if cur != text:
                print("differs: " + os.path.relpath(p, ROOT)); bad += 1
        if bad: sys.exit(1)
        print("ok: %d mechanics, %d files" % (len(all_m), len(files)))
        return
    os.makedirs(OUT, exist_ok=True)
    for p, text in files.items():
        with open(p, "w", encoding="utf-8", newline="\n") as f: f.write(text)
    print("wrote %d mechanics to %d files" % (len(all_m), len(files)))


def stats(all_m):
    from collections import Counter
    for title, key in (("category", lambda m: m["cat"]), ("source", lambda m: m["src"]), ("trigger", lambda m: m["trigger"]["kind"]), ("counter", lambda m: m["counter"]["kind"]),
                       ("scope", lambda m: m["scope"]), ("road", lambda m: m["road"])):
        print("== " + title)
        for k, n in Counter(key(m) for m in all_m).most_common(): print("  %-36s %d" % (k, n))
    print("== primitive")
    for k, n in Counter(e["op"] for m in all_m for e in m["effects"]).most_common(): print("  %-36s %d" % (k, n))


if __name__ == "__main__":
    main()
