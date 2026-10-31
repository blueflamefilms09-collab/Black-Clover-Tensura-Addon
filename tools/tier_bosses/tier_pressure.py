"""Expected damage pressure of the light tier bosses (docs/bosses/tier_powers.md): reads TierRosterF / TierRosterD, simulates the scheduler (one move at a time, weighted pick, cooldowns) and prints hp, attack, expected damage per second at 20 / 60 / 100 percent power, the biggest ordinary hit and the ultimate. Read only; run: python tools/tier_bosses/tier_pressure.py"""
import re, sys, random, statistics
import os
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
base = os.path.join(ROOT, "src/main/java/com/newuniverse/nusmp/arc/tier") + os.sep
LET = {"F": (700, 10), "D": (1000, 13)}
rows = []
for fn in ("TierRosterF.java", "TierRosterD.java"):
    txt = open(base + fn).read()
    for chunk in re.split(r'TierRow\.of\(', txt)[1:]:
        m = re.match(r'TierLetter\.(\w+), "(\w+)", "([^"]+)"', chunk)
        if not m: continue
        letter, cid, name = m.groups()
        sc = re.search(r'\.scale\(([\d.]+), ([\d.]+)\)', chunk)
        hp, atk = (float(sc.group(1)), float(sc.group(2))) if sc else (1.0, 1.0)
        moves = []
        for mv in re.finditer(r'\.move\(of\("(\w+)", "([^"]+)", (\w+)\)(.*)', chunk):
            mid, label, kind, rest = mv.groups()
            def g(p, d):
                x = re.search(p, rest); return float(x.group(1)) if x else d
            ult = '.ult()' in rest
            moves.append(dict(id=mid, label=label, kind=kind, power=max(60, g(r'\.power\((\d+)\)', 20)) if ult else g(r'\.power\((\d+)\)', 20),
                              wind=max(44, g(r'\.wind\((\d+)\)', 20)) if ult else g(r'\.wind\((\d+)\)', 20),
                              cd=max(400, g(r'\.cd\((\d+)\)', 100)) if ult else g(r'\.cd\((\d+)\)', 100),
                              w=g(r'\.weight\((\d+)\)', 3), dmg=g(r'\.dmg\(([\d.]+)\)', 1.0), count=g(r'\.count\((\d+)\)', 1), ult=ult,
                              fx=bool(re.search(r'\.fx\(', rest))))
        rows.append(dict(letter=letter, id=cid, name=name, hp=LET[letter][0]*hp, atk=LET[letter][1]*atk, moves=moves))

def hits(mv):
    return {"VOLLEY": mv["count"], "MARKED": mv["count"], "SWARM": mv["count"]}.get(mv["kind"], 1)

def sim(row, power, seconds=180, hit_rate=0.5, seed=1):
    rnd = random.Random(seed)
    t = 0; cd = {m["id"]: 0 for m in row["moves"]}; dmg = 0; ult_dmg = 0
    exposed = 0
    while t < seconds*20:
        avail = [m for m in row["moves"] if m["power"] <= power and cd[m["id"]] <= t]
        if not avail: t += 20; continue
        ults = [m for m in avail if m["ult"]]
        m = ults[0] if ults else rnd.choices(avail, [x["w"] for x in avail])[0]
        d = m["dmg"] * row["atk"] * hits(m) * (0.6 if m["ult"] else hit_rate)
        dmg += d
        if m["ult"]: ult_dmg += d
        cd[m["id"]] = t + m["wind"] + m["cd"]
        t += m["wind"] + 20 + (60 if m["ult"] else 0)   # recovery, exposed after the ultimate
    return dmg / seconds, ult_dmg / seconds

print(f'{"boss":22} {"L":1} {"hp":>5} {"atk":>5} {"pps@20":>7} {"pps@60":>7} {"pps@100":>8} {"maxhit":>7} {"ult":>6} {"fx":>3}')
res = []
for r in rows:
    p20, _ = sim(r, 20); p60, _ = sim(r, 60); p100, _ = sim(r, 100)
    mh = max(m["dmg"] * r["atk"] * (1 if m["kind"] not in ("MARKED",) else 1) for m in r["moves"] if not m["ult"])
    ult = [m for m in r["moves"] if m["ult"]][0]
    ulthit = ult["dmg"] * r["atk"]
    fx = sum(m["fx"] for m in r["moves"])
    res.append((r["letter"], r["id"], r["hp"], r["atk"], p20, p60, p100, mh, ulthit, fx))
    print(f'{r["id"]:22} {r["letter"]:1} {r["hp"]:5.0f} {r["atk"]:5.1f} {p20:7.2f} {p60:7.2f} {p100:8.2f} {mh:7.1f} {ulthit:6.1f} {fx:3d}')
for L in ("F", "D"):
    xs = [x for x in res if x[0] == L]
    print(L, "mean pps@60 %.2f  min %.2f max %.2f" % (statistics.mean(x[5] for x in xs), min(x[5] for x in xs), max(x[5] for x in xs)))
