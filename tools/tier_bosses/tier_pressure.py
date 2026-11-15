#!/usr/bin/env python3
"""Pressure of the Tier List bosses: the Python twin of TierBalance.pressure (docs/bosses/tier_powers.md, section 3).

It reads the roster tables (TierRosterF.java, TierRosterD.java, ...) as text, runs the same deterministic scheduler the Java class runs (one move at a
time, the ultimate when it is ready, otherwise the highest weight x time-since-use, a 50% hit rate, 60% for the ultimate, 180 seconds, a long exposed
recovery after the ultimate), adds what the traits add, and prints one line per boss and the tier bands (+-25% of the tier mean at 60% power).

Run it from anywhere:  python tools/tier_bosses/tier_pressure.py [--power 60] [--csv]

The numbers must match TierBalance.table(...) of the Java class; TierPowersDocTest holds the Java side, this script is for tuning a row without compiling.
Keep the constants below in step with TierBalance / TierTrait / TierLetter.
"""
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
TIER_DIR = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "java", "com", "newuniverse", "nusmp", "arc", "tier"))
ROSTERS = ["TierRosterF.java", "TierRosterD.java"]

# TierLetter: (health, attack)
LETTER = {"F": (700, 10.0), "D": (1000, 13.0)}
TRAIT_BUDGET = {"F": 5, "D": 7}

SECONDS, REF_POWER, BAND = 180, 60, 0.25
HIT_RATE, ULT_HIT_RATE = 0.5, 0.6
RECOVERY_TICKS, ULT_EXPOSED_TICKS = 20, 60
STEAM_DPS, POISON_DPS, KARMA_UPTIME, FRENZY_UPTIME = 0.4, 0.3, 0.4, 0.3
CLIMB_DAMAGE, CLIMB_MAX = 0.08, 3
FUSION_DAMAGE, FUSION_AT = 1.15, (0.66, 0.33)
FRENZY_DAMAGE, KARMA_DAMAGE = 1.10, 1.20
PRICE = {"SLOW_FIELD": 2, "STEAM": 2, "ARMOUR": 3, "BALANCE": 3, "SOUL_MARK": 3, "MIRROR": 2, "SYSTEM_LOCK": 3, "PULL": 2, "SPORE_POISON": 2,
         "FUSION": 2, "SIGHT": 2, "KARMA": 2, "ECHO": 2, "UNTOUCHABLE": 2, "CLIMB": 2, "SHIELD_BREAK": 1}


def health_at(power):
    if power >= 100: return 0.15
    if power >= 80: return 0.35
    if power >= 60: return 0.55
    if power >= 40: return 0.75
    return 0.92


def balanced(text, start):
    """The text inside the parenthesis that opens at text[start] == '('."""
    depth = 0
    for i in range(start, len(text)):
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
    return text[start + 1:]


def calls(chain):
    """The .name(args) calls of a builder chain, in order, with the arguments split at the top level commas."""
    out = []
    i = 0
    while i < len(chain):
        m = re.compile(r"\.(\w+)\(").match(chain, i)
        if not m:
            i += 1
            continue
        args = balanced(chain, m.end() - 1)
        out.append((m.group(1), args))
        i = m.end() + len(args) + 1
    return out


def parse_move(expr):
    m = re.match(r'\s*of\("(\w+)",\s*"([^"]*)",\s*(\w+)\)', expr)
    mv = dict(id=m.group(1), kind=m.group(3), power=20, wind=20, cd=100, w=3, dmg=1.0, count=1, ult=False)
    for name, args in calls(expr[m.end():]):
        a = [x.strip() for x in args.split(",")] if args.strip() else []
        if name == "power": mv["power"] = int(a[0])
        elif name == "wind": mv["wind"] = int(a[0])
        elif name == "cd": mv["cd"] = int(a[0])
        elif name == "weight": mv["w"] = int(a[0])
        elif name == "dmg": mv["dmg"] = float(a[0])
        elif name == "count": mv["count"] = int(a[0])
        elif name == "ult":
            mv["ult"] = True
            mv["power"] = max(mv["power"], 60)
            mv["wind"] = max(mv["wind"], 44)
            mv["cd"] = max(mv["cd"], 400)
    return mv


def load_rows():
    rows = []
    for fn in ROSTERS:
        path = os.path.join(TIER_DIR, fn)
        if not os.path.exists(path):
            continue
        txt = open(path, encoding="utf-8").read()
        for chunk in re.split(r"TierRow\.of\(", txt)[1:]:
            h = re.match(r'\s*TierLetter\.(\w+),\s*"(\w+)",\s*"([^"]+)"', chunk)
            if not h:
                continue
            letter, cid, name = h.groups()
            sc = re.search(r"\.scale\(([\d.]+),\s*([\d.]+)\)", chunk)
            hp, atk = (float(sc.group(1)), float(sc.group(2))) if sc else (1.0, 1.0)
            base_hp, base_atk = LETTER[letter]
            moves, traits = [], []
            for mm in re.finditer(r"\.move\(", chunk):
                moves.append(parse_move(balanced(chunk, mm.end() - 1)))
            for tm in re.finditer(r"\.trait\(", chunk):
                args = [x.strip() for x in balanced(chunk, tm.end() - 1).split(",")]
                traits.append((args[0].split(".")[-1], args[1].strip('"') if len(args) > 1 else None))
            rows.append(dict(letter=letter, id=cid, name=name, hp=round(base_hp * hp), atk=math.floor(base_atk * atk * 10 + 0.5) / 10.0, moves=moves, traits=traits))
    return rows


def hits(mv):
    return max(1, mv["count"]) if mv["kind"] in ("VOLLEY", "MARKED", "SWARM") else 1


def climb_steps(power):
    return max(0, min(CLIMB_MAX, (power - 20) // 20))


def fusion_steps(hp):
    return sum(1 for at in FUSION_AT if hp < at)


def trait_multiplier(row, power):
    names = [t for t, _ in row["traits"]]
    m = 1.0
    if "CLIMB" in names: m *= 1.0 + CLIMB_DAMAGE * climb_steps(power)
    if "FUSION" in names: m *= (1.0 + (FUSION_DAMAGE - 1.0)) ** fusion_steps(health_at(power))
    if "SOUL_MARK" in names: m *= 1.0 + (FRENZY_DAMAGE - 1.0) * FRENZY_UPTIME
    if "KARMA" in names: m *= 1.0 + (KARMA_DAMAGE - 1.0) * KARMA_UPTIME
    return m


def trait_dps(row):
    names = [t for t, _ in row["traits"]]
    return (STEAM_DPS if "STEAM" in names else 0.0) + (POISON_DPS if "SPORE_POISON" in names else 0.0)


def pressure(row, power):
    attack = row["atk"]
    moves = row["moves"]
    n = len(moves)
    horizon = SECONDS * 20
    cooldown_until = [0] * n
    last_use = [-400] * n
    echoed = next((mv for t, mv in row["traits"] if t == "ECHO"), None)
    t, total = 0, 0.0
    while t < horizon:
        pick, best = -1, -1.0
        for i, m in enumerate(moves):
            if m["power"] > power or cooldown_until[i] > t:
                continue
            if m["ult"]:
                pick = i
                break
            score = m["w"] * (1.0 + (t - last_use[i]) / 20.0)
            if score > best:
                best, pick = score, i
        if pick < 0:
            t += 20
            continue
        m = moves[pick]
        use = m["dmg"] * attack * hits(m) * (ULT_HIT_RATE if m["ult"] else HIT_RATE)
        if m["id"] == echoed:
            use *= 2.0
        total += use
        last_use[pick] = t
        cooldown_until[pick] = t + m["wind"] + m["cd"]
        t += m["wind"] + RECOVERY_TICKS + (ULT_EXPOSED_TICKS if m["ult"] else 0)
    return total / SECONDS * trait_multiplier(row, power) + trait_dps(row)


def mean(rows, letter, power):
    xs = [pressure(r, power) for r in rows if r["letter"] == letter]
    return sum(xs) / len(xs) if xs else 0.0


def main(argv):
    power = REF_POWER
    csv = "--csv" in argv
    if "--power" in argv:
        power = int(argv[argv.index("--power") + 1])
    rows = load_rows()
    if not rows:
        print("no rows found in", TIER_DIR)
        return 2
    sep = "," if csv else " "
    print(sep.join(["boss", "T", "hp", "atk", "p@20", "p@60", "p@100", "pts", "traits"]) if csv else "%-20s %-2s %6s %5s %7s %7s %7s %3s  %s" % ("boss", "T", "hp", "atk", "p@20", "p@60", "p@100", "pts", "traits"))
    for r in rows:
        pts = sum(PRICE[t] for t, _ in r["traits"])
        tr = ", ".join(t + ("(" + mv + ")" if mv else "") for t, mv in r["traits"])
        vals = (pressure(r, 20), pressure(r, 60), pressure(r, 100))
        if csv:
            print(",".join([r["id"], r["letter"], str(r["hp"]), "%.1f" % r["atk"], "%.2f" % vals[0], "%.2f" % vals[1], "%.2f" % vals[2], str(pts), '"' + tr + '"']))
        else:
            print("%-20s %-2s %6.0f %5.1f %7.2f %7.2f %7.2f %3d  %s" % (r["id"], r["letter"], r["hp"], r["atk"], vals[0], vals[1], vals[2], pts, tr))
    bad = 0
    for letter in LETTER:
        if sum(1 for r in rows if r["letter"] == letter) < 3:
            continue
        mu = mean(rows, letter, power)
        lo, hi = mu * (1 - BAND), mu * (1 + BAND)
        print("\n%s tier at %d%% power: mean %.2f, band %.2f..%.2f" % (letter, power, mu, lo, hi))
        for r in rows:
            if r["letter"] != letter:
                continue
            p = pressure(r, power)
            flag = "" if lo <= p <= hi else "   <-- OUTSIDE THE BAND"
            bad += 1 if flag else 0
            print("  %-20s %6.2f  (%+.0f%%)%s" % (r["id"], p, (p / mu - 1) * 100 if mu else 0, flag))
    for r in rows:
        pts = sum(PRICE[t] for t, _ in r["traits"])
        if pts > TRAIT_BUDGET[r["letter"]] or len(r["traits"]) > 2:
            print("BUDGET:", r["id"], pts, "points,", len(r["traits"]), "traits")
            bad += 1
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
