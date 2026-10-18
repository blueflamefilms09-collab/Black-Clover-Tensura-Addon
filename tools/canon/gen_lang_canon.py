#!/usr/bin/env python3
"""Writes tools/canon/lang_canon.json: every lang entry of en_us.json whose text names the Charter Grimoire (flat key -> the full NEW text), for the lead to merge into en_us.json.

en_us.json is not edited here (owner rule: one hand merges it). Idempotent: when en_us.json already has the new text the entry is still written (the fragment is the source of truth), so the
file does not shrink after the merge. Keys are never renamed; only the player-visible words change: "Charter Grimoire" -> "Canon Grimoire", and the words that describe those books
("A Written Charter", "The Complete Charter", "charter grimoires", ...).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json")
OUT = os.path.join(HERE, "lang_canon.json")

RULES = [
    ("A Written Charter", "A Written Canon"), ("The Complete Charter", "The Complete Canon"),
    ("Carry a charter grimoire for a boss.", "Carry a canon grimoire for a boss."),
    ("Carry all seven boss charter grimoires.", "Carry all seven boss canon grimoires."),
    ("Charter Grimoire", "Canon Grimoire"), ("charter grimoire", "canon grimoire"),
]


def renamed(text):
    for a, b in RULES:
        text = text.replace(a, b)
    return text


def main():
    lang = json.load(open(LANG, encoding="utf-8"))
    out = {}
    for k, v in lang.items():
        if not isinstance(v, str):
            continue
        n = renamed(v)
        if n != v:
            out[k] = n
        elif "Canon Grimoire" in v or "A Written Canon" in v or "The Complete Canon" in v:
            out[k] = v                                  # already merged: keep it in the fragment
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("{\n" + ",\n".join("  %s: %s" % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in out.items()) + "\n}\n")
    print("wrote %d entries to %s" % (len(out), OUT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
