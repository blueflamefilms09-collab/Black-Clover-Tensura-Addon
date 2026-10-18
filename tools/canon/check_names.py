#!/usr/bin/env python3
"""Checks the rename Charter Grimoire -> Canon Grimoire.

  1. Greps the repo for player-visible "Charter" text that is left (exit code 1 if any):
       - Java string literals of src/main/java that are text and not an id (an id is lower case with no space: `charter`, `charter_grimoire_`, `nusmp:charter_sync`, `has_dropped_charter_...`),
       - data/ and quest_pack/ json and snbt text, the quest and advancement generators in tools/quests, the dialogue source,
       - en_us.json, EXCEPT the keys of the fragment (those are the lead's merge; they are listed as "waiting for the merge").
  2. Verifies tools/canon/lang_canon.json: every value is free of "Charter", every key exists in en_us.json (no invented keys) and the fragment covers every en_us entry that still names it.

    python3 tools/canon/check_names.py
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
LANG = os.path.join(RES, "assets", "nusmp", "lang", "en_us.json")
FRAG = os.path.join(HERE, "lang_canon.json")

ID_LIKE = re.compile(r"^[a-z0-9_:./\-\[\]{}%$#@ ]*$")          # lower case, no capital letter: an id, a command word, a file name
STRING = re.compile(r'"(?:[^"\\\n]|\\.)*"')
SKIP_DIRS = {".git", "build", "dist", "logs", "node_modules", "__pycache__"}


def problems_java():
    out = []
    base = os.path.join(ROOT, "src", "main", "java")
    for r, _, fs in os.walk(base):
        for fn in fs:
            if not fn.endswith(".java"):
                continue
            p = os.path.join(r, fn)
            for n, line in enumerate(open(p, encoding="utf-8").read().splitlines(), 1):
                code = "" if line.lstrip().startswith(("*", "/*", "//")) else line.split(" // ")[0]
                for m in STRING.finditer(code):
                    s = m.group(0)
                    if re.search("charter", s, re.I) and not (ID_LIKE.match(s[1:-1]) and " " not in s[1:-1]):
                        out.append("%s:%d: %s" % (os.path.relpath(p, ROOT), n, s[:120]))
    return out


def problems_text(frag_keys):
    out = []
    waiting = []
    targets = []
    for sub in ("data", "quest_pack"):
        for r, _, fs in os.walk(os.path.join(RES, sub)):
            targets += [os.path.join(r, f) for f in fs if f.endswith((".json", ".snbt"))]
    for r, _, fs in os.walk(os.path.join(ROOT, "tools", "quests")):
        targets += [os.path.join(r, f) for f in fs if f.endswith((".py", ".json"))]
    targets += [os.path.join(ROOT, "tools", "arc", f) for f in os.listdir(os.path.join(ROOT, "tools", "arc")) if f.endswith(".py")]
    for p in targets:
        for n, line in enumerate(open(p, encoding="utf-8", errors="replace").read().splitlines(), 1):
            for m in STRING.finditer(line):
                s = m.group(0)
                if re.search("charter", s, re.I) and not (ID_LIKE.match(s[1:-1]) and " " not in s[1:-1]):
                    if "Charter of the First Master" in s:          # a skill book of the Fairy Tail route: unrelated to the grimoires
                        continue
                    out.append("%s:%d: %s" % (os.path.relpath(p, ROOT), n, s[:120]))
    lang = json.load(open(LANG, encoding="utf-8"))
    for k, v in lang.items():
        if isinstance(v, str) and re.search("charter", v, re.I):
            (waiting if k in frag_keys else out).append("%s: %s" % ("en_us.json " + k, v[:100]))
    return out, waiting


def main():
    frag = json.load(open(FRAG, encoding="utf-8")) if os.path.exists(FRAG) else {}
    lang = json.load(open(LANG, encoding="utf-8"))
    bad = []
    for k, v in frag.items():
        if "charter" in v.lower():
            bad.append("fragment value still says Charter: %s" % k)
        if k not in lang:
            bad.append("fragment key not in en_us.json (invented?): %s" % k)
    for k, v in lang.items():
        if isinstance(v, str) and re.search("Charter Grimoire|charter grimoire", v) and k not in frag:
            bad.append("en_us.json entry names the Charter Grimoire and is not in the fragment: %s" % k)
    left_java = problems_java()
    left_text, waiting = problems_text(set(frag))
    for title, items in (("Java strings", left_java), ("data / quests / dialogue / en_us", left_text), ("fragment", bad)):
        print("%s: %d left" % (title, len(items)))
        for i in items:
            print("   " + i)
    print("en_us.json entries waiting for the merge of tools/canon/lang_canon.json: %d" % len(waiting))
    print("fragment size: %d entries" % len(frag))
    return 1 if (left_java or left_text or bad) else 0


if __name__ == "__main__":
    sys.exit(main())
