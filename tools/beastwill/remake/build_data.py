#!/usr/bin/env python3
"""Why: builds the 27 remade Beast Will data files (3.60.0) from the approved design (beastwill_remake_v3.json) and the classic files.
Operational numbers the design does not touch (host races, caps, unlock levels, passive stat, keywords) are KEPT from the classic file in data/nusmp/beast_will_classic;
everything the design changes (name, beast, rank, drop chance, EP, tint, costs, stats, abilities) comes from the design. Extra text fields (lore, pitch, spirit, equip, ...) ride along for the docs and the codex.
Run from the repo root: python3 tools/beastwill/remake/build_data.py"""
import json, os
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp")
design = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "beastwill_remake_v3.json")))

def hexint(s):
    v = int(s.lstrip("#"), 16)
    r, g, b = v >> 16, (v >> 8) & 255, v & 255
    if 0.299 * r + 0.587 * g + 0.114 * b < 70:          # a near-black core would be invisible as an additive glow (Shadow Panther): lift it, as the preview did
        r, g, b = (round(255 - (255 - c) * 0.70) for c in (r, g, b))
        v = r << 16 | g << 8 | b
    return v

CAPS = {"movement_speed": (0.02, 0.40), "attack_damage": (8.0, 0.50), "max_health": (12.0, 0.30), "armor": (10.0, 0), "armor_toughness": (5.0, 0), "knockback_resistance": (0.9, 0), "attack_speed": (0, 0.30), "step_height": (0.6, 0), "jump_strength": (0, 0.4), "safe_fall_distance": (6.0, 0)}

def ability(a):
    out = {k: a[k] for k in ("id", "kind", "slot", "costPct", "cooldownTicks", "strain", "range", "radius", "durationTicks", "power", "maxTargets", "bossFactor")}
    out["name"] = a.get("name", a["id"]); out["effect"] = a.get("effect", "")
    # boss safety of the engine (kept from 3.45.0): a hold never takes a boss, the hard controls keep their boss resistance
    if out["kind"] == "time_exclusion": out["bossFactor"] = 0.0
    elif out["kind"] in ("world_howl", "restraining_vines"): out["bossFactor"] = min(out["bossFactor"], 0.25)
    return out

for w in design:
    wid = w["id"]
    old = json.load(open(os.path.join(DATA, "beast_will_classic", wid + ".json")))
    o = dict(old)
    o["name"] = w["name"]; o["beast"] = w["beast"].split("(")[0].strip()[:48].strip(" ,"); o["beastNote"] = w["beast"]; o["affinity"] = w["affinity"]; o["legendary"] = w["legendary"]; o["minRank"] = w["minRank"]
    o["dropChance"] = w["dropChance"]; o["requireEp"] = w["requireEp"]; o["tint"] = hexint(w["core"]["tint"])
    for ph in ("acquire", "integrate"):
        n = dict(old[ph])
        n["drainPct"] = w[ph]["drainPct"]; n["strainPerSecond"] = w[ph]["strainPerSecond"]
        n["stats"] = [dict(x) for x in w[ph]["stats"]]
        for st in n["stats"]:                                   # passive + phase stat must fit the engine's attribute cap (BeastWillRules.ATTR_CAPS)
            cap = CAPS.get(st["attribute"].split(".")[-1])
            if cap:
                room = cap[0 if st["op"] == "add" else 1] - sum(x["amount"] for x in old.get("passive", []) if x["attribute"] == st["attribute"] and x["op"] == st["op"])
                st["amount"] = round(min(st["amount"], room), 4)
        n["abilities"] = [ability(a) for a in w[ph]["abilities"]]
        o[ph] = n
    o["acquire"]["equipPiece"] = w["acquire"]["equip_piece"]
    o["integrate"]["transformation"] = w["integrate"]["transformation"]
    o["integrate"]["equip"] = w["integrate"]["equip"]
    o["lore"] = w["lore"]; o["pitch"] = w["pitch"]; o["untrained"] = w["untrained"]; o["designTag"] = w["tag"]
    o["spirit"] = {"colors": w["spirit"]["colors"], "echo": w["spirit"]["echo"], "accent": w["core"]["accent"], "look": w["core"]["look"]}
    with open(os.path.join(DATA, "beast_will", wid + ".json"), "w") as f:
        json.dump(o, f, indent=2); f.write("\n")
print(len(design), "will files written")
