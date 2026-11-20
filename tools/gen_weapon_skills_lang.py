"""Writes tools/lang/weapon_skills.json (docs/weapon_skills.md) from the Java tables and merges it into en_us.json (json load, update, dump: the same two-space, UTF-8, trailing-newline format the file already has).
Reads skill/rules/WeaponSkillRules.java (the three wheels) and heroweapon/HeroKind.java (the ten Vassal Weapons: the entries whose class is HeroClass.VASSAL).
Run: python tools/gen_weapon_skills_lang.py   (add --no-merge to only write the fragment)"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "lang", "en_us.json")
FRAGMENT = os.path.join(ROOT, "tools", "lang", "weapon_skills.json")


def unescape(s):
    return s.replace('\\\\', '\\').replace('\\"', '"')


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


rules = read(os.path.join(JAVA, "skill", "rules", "WeaponSkillRules.java"))
modes = {}
for kind in ("HERO", "DJINN", "BEAST"):
    block = re.search(r"List<Mode> " + kind + r" = List\.of\((.*?)\);\n", rules, re.S).group(1)
    modes[kind] = [(m.group(1), unescape(m.group(2)), unescape(m.group(3)))
                   for m in re.finditer(r'new Mode\("([a-z0-9_]+)", "((?:[^"\\]|\\.)*)", Act\.[A-Z0-9_]+, "((?:[^"\\]|\\.)*)"\)', block)]
assert [len(modes[k]) for k in ("HERO", "DJINN", "BEAST")] == [5, 7, 6], modes

weapons = []
for m in re.finditer(r'^\s*[A-Z_]+\("([a-z_]+)", "([^"]+)", HeroClass\.VASSAL,', read(os.path.join(JAVA, "heroweapon", "HeroKind.java")), re.M):
    weapons.append((m.group(1), m.group(2)))
assert len(weapons) == 10, weapons


def describe(kind, of):
    return "Extra. Uses " + of + " from the skill wheel, so its keys are not needed (they still work). It works only while you own it. Modes: " + \
        ", ".join("%s (%s)" % (n, b) for _, n, b in modes[kind]) + "."


out = {}
for item_id, display in weapons:
    out["anime_multiverse.skill.ws_" + item_id] = display + " Arts"
    out["anime_multiverse.skill.ws_" + item_id + ".description"] = describe("HERO", "the " + display + " you are bonded to")
out["anime_multiverse.skill.ws_djinn"] = "Djinn Equip"
out["anime_multiverse.skill.ws_djinn.description"] = describe("DJINN", "the Djinn you hold (bound, or lent to you)")
out["anime_multiverse.skill.ws_beast_will"] = "Beast Will"
out["anime_multiverse.skill.ws_beast_will.description"] = describe("BEAST", "the Beast Will you carry")
for kind in ("HERO", "DJINN", "BEAST"):
    for mid, name, _ in modes[kind]:
        out["anime_multiverse.skill.mode.weapon." + mid] = name
out.update({
    "anime_multiverse.skill.msg.weapon.off": "The weapon skills are switched off on this server.",
    "anime_multiverse.skill.msg.weapon.hero_unbound": "The %s is not bonded to you.",
    "anime_multiverse.skill.msg.weapon.djinn_none": "You hold no Djinn. Bind a Djinn Seal to a Legend-grade vessel.",
    "anime_multiverse.skill.msg.weapon.beast_none": "You carry no Beast Will.",
    "anime_multiverse.skill.msg.weapon.granted": "%s is now on your skill list.",
    "anime_multiverse.skill.msg.weapon.revoked": "%s leaves your skill list.",
})

with open(FRAGMENT, "w", encoding="utf-8", newline="\n") as f:
    f.write(json.dumps(out, indent=2, ensure_ascii=False) + "\n")
print("wrote", FRAGMENT, len(out), "keys")

if "--no-merge" not in sys.argv:
    with open(LANG, encoding="utf-8") as f:
        lang = json.loads(f.read())
    before = len(lang)
    lang.update(out)
    with open(LANG, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(lang, indent=2, ensure_ascii=False) + "\n")
    print("merged into en_us.json:", len(lang) - before, "new keys,", len(lang), "total")
