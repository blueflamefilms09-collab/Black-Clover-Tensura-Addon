#!/usr/bin/env python3
"""Writes tools/quests/arc_roster.txt from the Java records of the characters instead of from the running game.

Why: the roster file (id|name|saga|role per line) is normally written by `/multiverse arc dump`, which needs a game. The lead has no game, and the records are plain
Java builder chains (`ArcCharacter.boss("id", "Name").saga(ArcSaga.X)...;`), so the same four facts can be read from the source. The result is merged with the file
that is already there (an id already in it is kept as it is), then `python tools/quests/gen_arc_adv.py` writes the advancements.

    python3 tools/quests/arc_roster_from_source.py            # add every character found in src/main/java to the roster file
    python3 tools/quests/arc_roster_from_source.py --list     # only print what it found
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "src", "main", "java")
ROSTER = os.path.join(HERE, "arc_roster.txt")
REC = re.compile(r'ArcCharacter\s*\.\s*(boss|npc)\(\s*"([a-z][a-z0-9_]*)"\s*,\s*"([^"]+)"\s*\)(.*?);', re.S)
SAGA = re.compile(r'\.saga\(\s*ArcSaga\.([A-Z_]+)\s*\)')
DEFAULT_SAGA = {"boss": "ELF_REINCARNATION", "npc": "ALLIES"}


def found():
    out = {}
    for root, _dirs, files in os.walk(os.path.join(SRC, "com", "newuniverse", "nusmp", "arc")):
        for f in sorted(files):
            if not f.endswith(".java"):
                continue
            text = open(os.path.join(root, f), encoding="utf-8").read()
            for m in REC.finditer(text):
                role, cid, name, chain = m.groups()
                s = SAGA.search(chain)
                saga = (s.group(1) if s else DEFAULT_SAGA[role]).lower()
                out.setdefault(cid, (cid, name, saga, role))
    return out


def main(argv):
    chars = found()
    if "--list" in argv:
        for c in chars.values():
            print("|".join(c))
        print(len(chars), "characters")
        return 0
    known = set()
    text = "# id|name|saga|role : written by /multiverse arc dump, read by tools/quests/spec_arc.py\n"
    if os.path.exists(ROSTER):
        text = open(ROSTER, encoding="utf-8").read()
        for line in text.splitlines():
            if line.strip() and not line.startswith("#"):
                known.add(line.split("|")[0].strip())
    if text and not text.endswith("\n"):
        text += "\n"
    added = [c for cid, c in chars.items() if cid not in known]
    with open(ROSTER, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text + "".join("|".join(c) + "\n" for c in added))
    print("added %d, already there %d" % (len(added), len(known)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
