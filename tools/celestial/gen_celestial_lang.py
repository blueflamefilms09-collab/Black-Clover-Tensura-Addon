"""Celestial Spirit Magic: the lang fragment (flat key -> English text) for tools/lang/celestial_keys.json.

    python3 -B tools/celestial/gen_celestial_lang.py

Reads the keys and their texts from celestial/CelestialKey.java and every translatable fallback in the celestial code (Component.translatableWithFallback, SkillMsg.t / mode), so
a message added to the code appears here without another list. The fragment is merged into en_us.json by the lead (this branch does not touch en_us.json).
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from celestial_data import ROOT  # noqa: E402

J = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
FILES = [os.path.join(J, "celestial", f) for f in sorted(os.listdir(os.path.join(J, "celestial"))) if f.endswith(".java")] + [
    os.path.join(J, "skill", "CelestialArts.java")]
LIT = r'"((?:[^"\\]|\\.)*)"'


def unescape(s):
    return re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), s.replace('\\"', '"').replace("\\\\", "\\"))


def main():
    out = {}
    src = open(os.path.join(J, "celestial", "CelestialKey.java"), encoding="utf-8").read()
    for m in re.finditer(r'^\s+([A-Z_]+)\(' + LIT + r',\s*' + LIT + r',\s*' + LIT + r',\s*Rank\.(\w+),\s*0x([0-9A-Fa-f]+),\s*' + LIT + r',\s*' + LIT + r',', src, re.M):
        _name, kid, display, spirit, rank, _rgb, ability, text = m.groups()
        kid, display, spirit, ability, text = map(unescape, (kid, display, spirit, ability, text))
        item = kid + "_key"
        if rank == "GOLD":
            out["item.nusmp." + item] = "Gold Key of " + display
        elif rank == "SILVER":
            out["item.nusmp." + item] = "Silver Key of " + display
        else:
            out["item.nusmp." + item] = "Key of the Celestial Gate"
    for m in re.finditer(r'"Star Dress",|^\s+"([A-Za-z ]+ Form)",', src, re.M):
        pass
    for m in re.finditer(r'^\s+[A-Z_]+\(' + LIT + r'.*?\n\s+' + LIT + r',', src, re.M | re.S):
        pass
    # the dresses: the "<Sign> Form" string is the first literal on the last line of each gold entry
    for m in re.finditer(r'^\s+([A-Z_]+)\(' + LIT + r'.*?"([A-Za-z]+ Form)"', src, re.M | re.S):
        pass   # the Star Dress mode names went with the skill that used them
    out["item.nusmp.spirit_king_sceptre"] = "Sceptre of the Celestial Spirit King"
    out["entity.nusmp.spirit_king"] = "The Celestial Spirit King"
    out["curios.identifier.celestial_key"] = "Celestial Key"
    for f in FILES:
        text = open(f, encoding="utf-8").read()
        for m in re.finditer(r'translatableWithFallback\(\s*' + LIT + r',\s*' + LIT, text):
            out[unescape(m.group(1))] = unescape(m.group(2))
        for m in re.finditer(r'SkillMsg\.t\(\s*' + LIT + r',\s*' + LIT, text):
            out["nusmp.skill.msg." + unescape(m.group(1))] = unescape(m.group(2))
        for m in re.finditer(r'SkillMsg\.mode\(\s*' + LIT + r',\s*' + LIT, text):
            out["nusmp.skill.mode." + unescape(m.group(1))] = unescape(m.group(2))
    # keys built from a variable in the code are not found by the scan: the ones that are written by hand
    out.pop("nusmp.skill.mode.celestial.", None)
    for k in [k for k in out if k.endswith(".")]:
        out.pop(k)
    path = os.path.join(ROOT, "tools", "lang", "celestial_keys.json")
    with open(path, "w", newline="\n", encoding="utf-8") as fh:
        fh.write(json.dumps(dict(sorted(out.items())), indent=1, ensure_ascii=False) + "\n")
    print(len(out), "lang keys")


if __name__ == "__main__":
    main()
