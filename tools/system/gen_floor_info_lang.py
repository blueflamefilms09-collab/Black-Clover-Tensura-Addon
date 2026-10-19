#!/usr/bin/env python3
"""Why: the words of the System's floor report (theme, mechanics, depth) and of the escape rules (release rune, rescue, /dungeon unstuck) are written once, in FloorInfoText
and LockText (the code's own English fallback), and the lang fragment must say the same. This writes the flat fragment tools/lang/system_floor_info.json from those two maps (run
through FloorInfoLangDump.java). The fragment is merged into assets/nusmp/lang/en_us.json by the lead; keys that exist are never overwritten. A unit test
(FloorInfoTextTest) fails when the file and the maps differ, so run this again after changing a text.

    python3 -B tools/system/gen_floor_info_lang.py [--out FILE] [--work DIR]
"""
import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "tools", "lang", "system_floor_info.json"))
    ap.add_argument("--work", default=os.path.join(ROOT, "build", "floor_info_lang"))
    args = ap.parse_args()
    classes = os.path.join(args.work, "classes")
    os.makedirs(classes, exist_ok=True)
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    src = os.path.join(ROOT, "src", "main", "java")
    r = subprocess.run(["javac", "-proc:none", "-d", classes, "-sourcepath", src, os.path.join(HERE, "FloorInfoLangDump.java")], capture_output=True, text=True, env=env)
    if r.returncode != 0:
        sys.exit(r.stderr)
    r = subprocess.run(["java", "-cp", classes, "FloorInfoLangDump"], capture_output=True, text=True, env=env)
    if r.returncode != 0:
        sys.exit(r.stderr)
    lang = {}
    for line in r.stdout.splitlines():
        if "\t" in line:
            k, v = line.split("\t", 1)
            lang[k] = v
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote %d keys to %s" % (len(lang), args.out))


if __name__ == "__main__":
    main()
