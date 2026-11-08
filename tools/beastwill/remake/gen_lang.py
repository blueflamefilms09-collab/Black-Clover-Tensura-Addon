#!/usr/bin/env python3
"""Why: the lang texts of the 3.60.0 Beast Will remake, from the approved design (beastwill_remake_v3.json).
Writes tools/lang/beastwill_remake.json (will names, lore line, ability names, the mastery incantations) and refreshes tools/lang/beastskills.json (skill names, descriptions, passive / active / finisher names),
then merges both into assets/nusmp/lang/en_us.json (existing keys are updated in place, new keys are appended; nothing is removed, so old keys stay).
Run from the repo root: python3 tools/beastwill/remake/gen_lang.py"""
import json, os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..", "..")
design = json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))
EN = os.path.join(ROOT, "src/main/resources/assets/nusmp/lang/en_us.json")

rem = {}
abil_names = {}
for w in design:
    wid = w["id"]; key = wid.replace("_will", "")
    rem[f"nusmp.beastwill.{wid}.name"] = w["name"]
    rem[f"nusmp.beastwill.{wid}.lore"] = w["lore"]
    for ph in ("acquire", "integrate"):
        for a in w[ph]["abilities"]:
            nm = a.get("name", a["id"])
            if abil_names.setdefault(a["id"], nm) != nm: sys.exit(f"ability id {a['id']} has two names: {abil_names[a['id']]} / {nm}")
            rem[f"nusmp.beastwill.ability.{a['id']}"] = nm
    rem[f"nusmp.beastwill.incant.{key}"] = w["skill"]["finisher"]["incantation"]

sk = {}
for w in design:
    s = w["skill"]; key = w["id"].replace("_will", ""); sid = "bw_" + key
    short = w["name"].replace(" Will", "")
    first = re.split(r"(?<=[.!?])\s", w["lore"])[0].rstrip(".")
    power = f"{short}: {first if len(first) <= 120 else first[:120].rsplit(' ', 1)[0].rstrip(',;: ')}"
    acts = [a["name"] for a in s["actives"]]; fin = s["finisher"]["name"]
    sk[f"nusmp.skill.{sid}"] = f"{s['ultimateName']}, {s['epithet']}"
    sk[f"nusmp.skill.{sid}.description"] = f"Ultimate. {power}. Passives (one ON/OFF switch): {', '.join(p['name'] for p in s['passives'])}, actives: {', '.join(acts)}, {fin} (mastery)."
    sk[f"nusmp.skill.{sid}_unique"] = s["uniqueName"]
    sk[f"nusmp.skill.{sid}_unique.description"] = f"Unique, evolves into {s['ultimateName']}, {s['epithet']} once mastered. {power}. Passive: {s['passives'][0]['name']}. Actives: {', '.join(acts)}."
    for p in s["passives"]: sk[f"nusmp.skill.mode.{sid}.{p['id']}"] = p["name"]
    for a in s["actives"]: sk[f"nusmp.skill.mode.{sid}.{a['id']}"] = a["name"]
    sk[f"nusmp.skill.mode.{sid}.{s['finisher']['id']}"] = fin

def merge_file(path, add):
    cur = json.load(open(path), object_pairs_hook=dict) if os.path.exists(path) else {}
    cur.update(add)
    json.dump(cur, open(path, "w"), indent=2, ensure_ascii=False); open(path, "a").write("\n")

json.dump(rem, open(os.path.join(ROOT, "tools/lang/beastwill_remake.json"), "w"), indent=2, ensure_ascii=False); open(os.path.join(ROOT, "tools/lang/beastwill_remake.json"), "a").write("\n")
merge_file(os.path.join(ROOT, "tools/lang/beastskills.json"), sk)
en = json.load(open(EN, encoding="utf-8"))
before = len(en)
en.update(rem); en.update(sk)
open(EN, "w", encoding="utf-8").write(json.dumps(en, indent=2, ensure_ascii=False) + "\n")
print(len(rem), "remake keys,", len(sk), "skill keys; en_us", before, "->", len(en))
