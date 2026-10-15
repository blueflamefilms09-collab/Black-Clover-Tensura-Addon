#!/usr/bin/env python3
"""Why: Better Combat 1.21 reads one JSON per item id from data/<namespace>/weapon_attributes/<id>.json. The mod has ~116 melee weapons, so
the files are generated per weapon CLASS (tools/bettercombat/profiles.py) from the inventory (inventory.py, which reads the repo sources), with
a few hand-written profiles for the named weapons. The generator is deterministic and idempotent, `--check` is the build guard (a weapon without a
file, or a file that no longer matches its profile, exits 1), and the JUnit test WeaponAttributesTest runs the same guard from Gradle so a new
weapon fails the build until it has a file.

Schema subset written (from memory of Better Combat 2.x; the mod could not be loaded here, see docs/better_combat.md):
  {"parent": "bettercombat:<builtin>",
   "attributes": {"range_bonus": <number>, "two_handed": true,                       # two_handed only where a class is two-handed
                  "attacks": [{"hitbox": "HORIZONTAL_PLANE|VERTICAL_PLANE|FORWARD_BOX", "damage_multiplier": n,
                               "angle": n, "upswing": 0..1, "animation": "nusmp:<name>"}]}}   # attacks only where we ship our own combo
No `pose`, `off_hand_pose`, `swing_sound` or `category` keys are written: their value spaces (animation / sound ids) are not certain offline, and
Better Combat falls back to the parent's for anything we do not set.

Usage:  python tools/bettercombat/gen_attributes.py [--check] [--list]
  --check  write nothing; exit 1 when a weapon has no file or a file differs from its profile
  --list   print the weapons per class
Run  python tools/bettercombat/inventory.py  first when a weapon was added (it refreshes manifest.json; --check does it in memory).
"""
import copy
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import inventory as INV  # noqa: E402
import profiles as PR  # noqa: E402

ROOT = INV.ROOT
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "weapon_attributes")
ANIM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "player_animations")


def r2(x):
    return round(x + 1e-9, 2)


def profile_for(wid, w):
    """The effective profile of one weapon: {parent, range_bonus, two_handed, attacks} (None = not written)."""
    cls, prof = w["class"], w["profile"]
    base = PR.SPECIAL_PROFILES.get(prof) or PR.CLASS_PROFILES[cls]
    p = copy.deepcopy(base)
    if wid in PR.SIZE_RANGE and prof == cls:
        p["range_bonus"] = PR.SIZE_RANGE[wid]
    if prof.startswith("djinn_"):
        # The Djinn weapon already carries its reach as a vanilla entity_interaction_range modifier (DjinnWeaponItem.props); Better Combat adds
        # range_bonus on top of the player's interaction range, so only the part of the class size the modifier does not cover is added.
        reach = float(w["source"].rsplit("reach", 1)[1])
        p["range_bonus"] = max(0.0, r2(p["range_bonus"] - reach))
        if p["attacks"]:
            p["attacks"][-1]["damage_multiplier"] = r2(p["attacks"][-1]["damage_multiplier"] * 1.5)   # the finisher of a Djinn weapon lands harder
    legacy = PR.LEGACY.get(wid)
    if legacy:
        p["parent"] = legacy["parent"]
        if "range_bonus" in legacy:
            p["range_bonus"] = legacy["range_bonus"]
        if not legacy["combo"]:
            p["two_handed"], p["attacks"] = None, None
    return p


def document(p):
    attrs = {"range_bonus": p["range_bonus"]}
    if p["two_handed"]:
        attrs["two_handed"] = True
    if p["attacks"]:
        attrs["attacks"] = p["attacks"]
    return {"parent": "bettercombat:" + p["parent"], "attributes": attrs}


def render(wid, w):
    return json.dumps(document(profile_for(wid, w)), indent=2) + "\n"


def problems_of(weapons):
    out = []
    for wid, w in weapons.items():
        doc = document(profile_for(wid, w))
        if doc["parent"].split(":", 1)[1] not in PR.BUILTIN_PARENTS:
            out.append("%s: parent %s is not a Better Combat built-in" % (wid, doc["parent"]))
        for a in doc["attributes"].get("attacks", []):
            name = a["animation"].split(":", 1)[1]
            if not os.path.exists(os.path.join(ANIM_DIR, name + ".json")):
                out.append("%s: animation %s has no file under assets/nusmp/player_animations" % (wid, a["animation"]))
    return out


def main(argv):
    weapons, _excluded, probs = INV.build()
    probs = probs + problems_of(weapons)
    for p in probs:
        print("PROBLEM: " + p)
    if "--list" in argv:
        by = {}
        for wid, w in weapons.items():
            by.setdefault(w["class"], []).append(wid)
        for cls in sorted(by):
            print("%s (%d): %s" % (cls, len(by[cls]), ", ".join(by[cls])))
        return 1 if probs else 0
    check = "--check" in argv
    bad = len(probs)
    os.makedirs(OUT, exist_ok=True)
    wrote = 0
    for wid, w in weapons.items():
        path = os.path.join(OUT, wid + ".json")
        text = render(wid, w)
        cur = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if cur == text:
            continue
        if check:
            print("%s: %s" % ("missing" if cur is None else "differs", os.path.relpath(path, ROOT)))
            bad += 1
        else:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
            wrote += 1
    if check:
        if bad:
            print("weapon_attributes out of date: run  python tools/bettercombat/gen_attributes.py")
        return 1 if bad else 0
    print("wrote %d file(s); %d weapons covered" % (wrote, len(weapons)))
    return 1 if probs else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
