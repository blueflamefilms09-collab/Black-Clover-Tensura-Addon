#!/usr/bin/env python3
"""Writes tools/tensura_menu/lang_tensura_menu.json, the flat key -> text file the lead merges into en_us.json.

Two sources, both read from the repository (nothing is typed twice):
  * src/main/java/com/newuniverse/nusmp/skilltree/menu/MenuText.java   the def("key", "English") strings of the nested panel and the admin switch
  * src/main/resources/assets/nusmp/skill_books.json                   one name per line: nusmp.skill.line_<id> = the line's name (the stand-in skill's name)

The game reads every one of these with Component.translatableWithFallback(key, english), so the file is optional: until it is merged the English
text declared in code shows. Run from the repository root:  python3 tools/tensura_menu/gen_lang.py
A unit test (MenuTextTest) compares this file with the code in both directions, so a stale file fails the build.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TEXT = ROOT / "src/main/java/com/newuniverse/nusmp/skilltree/menu/MenuText.java"
BOOKS = ROOT / "src/main/resources/assets/nusmp/skill_books.json"
OUT = ROOT / "tools/tensura_menu/lang_tensura_menu.json"


def main() -> int:
    src = TEXT.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r'def\("([^"]+)",\s*"((?:[^"\\]|\\.)*)"\)', src):
        out[m.group(1)] = json.loads('"' + m.group(2) + '"')
    if not out:
        print("no strings found in MenuText.java", file=sys.stderr)
        return 1
    lines = json.loads(BOOKS.read_text(encoding="utf-8"))["lines"]
    for bare in sorted(lines):
        out["nusmp.skill.line_" + bare] = lines[bare].get("name") or bare
    OUT.write_text(json.dumps(out, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)}: {len(out)} keys")
    return 0


if __name__ == "__main__":
    sys.exit(main())
