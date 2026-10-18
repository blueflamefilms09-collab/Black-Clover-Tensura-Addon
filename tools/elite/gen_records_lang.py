#!/usr/bin/env python3
"""Writes tools/elite/records_lang.json from the def("key", "English text") calls of RecordsText.java.

The owner does not want en_us.json edited by this job; this flat file is what the lead merges into it later. RecordsText.java is the single source (the game
falls back to its English text when a language file lacks a key), and RecordsTextTest compares the two in both directions, so run this after changing a string:
    python tools/elite/gen_records_lang.py
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "src", "main", "java", "com", "newuniverse", "nusmp", "elite", "records", "RecordsText.java")
OUT = os.path.join(HERE, "records_lang.json")


def main():
    text = open(SRC, encoding="utf-8").read()
    out = {}
    for m in re.finditer(r'def\("([^"]+)",\s*"((?:[^"\\]|\\.)*)"\)', text):
        out[m.group(1)] = json.loads('"' + m.group(2) + '"')
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(len(out), "strings ->", OUT)


if __name__ == "__main__":
    main()
