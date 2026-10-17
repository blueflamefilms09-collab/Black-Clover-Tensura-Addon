#!/usr/bin/env python3
"""Why: the System's words are written once, in SystemText (the code's own English fallback), and the lang file must say the same. This writes the fragment of
assets/nusmp/lang/en_us.json for the System from that map (run through LangDump.java) plus one title per quest of data/nusmp/mega/system/quests.json ({n} and {t}
become the numbered format %1$s the client fills with the target). The lead merges the fragment with the merge script; keys that exist are never overwritten.

    python tools/system/gen_system_lang.py [--out FILE] [--work DIR]
"""
import argparse
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "system_preview", "msy.lang.json"))
    ap.add_argument("--work", default=os.path.join(ROOT, "build", "system_preview"))
    args = ap.parse_args()
    classes = os.path.join(args.work, "classes")
    os.makedirs(classes, exist_ok=True)
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    r = subprocess.run(["javac", "-d", classes, "-sourcepath", os.path.join(ROOT, "src", "main", "java"), os.path.join(HERE, "LangDump.java")], capture_output=True, text=True, env=env)
    if r.returncode != 0:
        sys.exit(r.stderr)
    r = subprocess.run(["java", "-cp", classes, "LangDump"], capture_output=True, text=True, env=env)
    if r.returncode != 0:
        sys.exit(r.stderr)
    lang = {}
    for line in r.stdout.splitlines():
        if "\t" in line:
            k, v = line.split("\t", 1)
            lang[k] = v
    quests = json.load(open(os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "mega", "system", "quests.json"), encoding="utf-8"))
    for q in quests["quests"]:
        lang["message.nusmp.mega.system.quest." + q["id"]] = re.sub(r"\{[nt]\}", "%1$s", q["title"])
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("wrote %d keys to %s" % (len(lang), args.out))


if __name__ == "__main__":
    main()
