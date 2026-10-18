#!/usr/bin/env python3
"""Writes tools/lines/lang_engine.json (flat key -> English text) from the single list of strings in skilltree/line/LineText.java.

WHY: the owner does not want assets/nusmp/lang/en_us.json edited by the engine job, so the engine's new strings are collected here for the lead to merge. The Java code reads every string with
Component.translatableWithFallback(key, english), so the game never shows a raw key before the merge. LineTextTest keeps this file and LineText in step (both directions).
usage: python3 tools/lines/gen_lang_engine.py
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "src/main/java/com/newuniverse/nusmp/skilltree/line/LineText.java")


def main():
    text = open(SRC, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'def\("([^"]+)",\s*"((?:[^"\\]|\\.)*)"\)', text):
        out[m.group(1)] = json.loads('"%s"' % m.group(2))
    with open(os.path.join(HERE, "lang_engine.json"), "w", encoding="utf-8") as f:
        f.write(json.dumps(out, indent=2, ensure_ascii=False) + "\n")
    print("ok: %d strings" % len(out))


if __name__ == "__main__":
    main()
