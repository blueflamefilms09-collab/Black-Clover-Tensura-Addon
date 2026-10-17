#!/usr/bin/env python3
"""
Language keys for the dialogue of the characters of the arcs (arc/dialogue/DialogueBook.java).

Why: every character's words live in data/nusmp/arc/dialogue/<character_id>.json and hold the English text, so a datapack can replace them. A line that
should be TRANSLATABLE also carries a language key (arc.dialogue.<id>.<n>) and the same English sits in the language file; the game asks the language
file first and falls back to the JSON. This tool keeps the three in step so nobody numbers keys by hand:

    python3 tools/gen_arc_dialogue_lang.py                       # prints the lang fragment {key: text} for every keyed line
    python3 tools/gen_arc_dialogue_lang.py --out frag.json       # writes it (ASCII-escaped, flat JSON object)
    python3 tools/gen_arc_dialogue_lang.py --write-keys          # gives every line without a key its key, in the files
    python3 tools/gen_arc_dialogue_lang.py --check path/to/en_us.json   # every keyed line must be in the language file with the same text (exit 1 if not)

Numbering (the Java test DialogueFilesTest checks it): n counts the lines of a file from 1, the situations in the order greeting, hint, shop,
quest_offer, quest_done, farewell, and the lines of a situation in the order they stand in the file. Text becomes a language template the way
DialogueLine.template() does it: '%' is doubled, {player} -> %1$s, {npc} -> %2$s, {saga} -> %3$s.
"""
import argparse
import json
import os
import sys
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DIR = os.path.join(HERE, "..", "src", "main", "resources", "data", "nusmp", "arc", "dialogue")
CONTEXTS = ["greeting", "hint", "shop", "quest_offer", "quest_done", "farewell"]


def template(text):
    return text.replace("%", "%%").replace("{player}", "%1$s").replace("{npc}", "%2$s").replace("{saga}", "%3$s")


def line_text(line):
    if isinstance(line, str):
        return line
    return line.get("text", "")


def walk(doc):
    """Yields (context, index_in_context, n, line) in the numbering order."""
    n = 0
    lines = doc.get("lines", {})
    for ctx in CONTEXTS:
        for i, line in enumerate(lines.get(ctx, [])):
            n += 1
            yield ctx, i, n, line


def load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh, object_pairs_hook=OrderedDict)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dir", default=DEFAULT_DIR)
    ap.add_argument("--out")
    ap.add_argument("--write-keys", action="store_true")
    ap.add_argument("--check")
    args = ap.parse_args()

    files = sorted(f for f in os.listdir(args.dir) if f.endswith(".json"))
    fragment = OrderedDict()
    errors = []
    for name in files:
        stem = name[:-5]
        path = os.path.join(args.dir, name)
        doc = load(path)
        unknown = [c for c in doc.get("lines", {}) if c not in CONTEXTS]
        for c in unknown:
            errors.append("%s: unknown situation '%s'" % (name, c))
        changed = False
        for ctx, i, n, line in walk(doc):
            want = "arc.dialogue.%s.%d" % (stem, n)
            have = "" if isinstance(line, str) else line.get("key", "")
            if have and have != want:
                errors.append("%s: line %d (%s[%d]) has the key %s, the numbering says %s" % (name, n, ctx, i, have, want))
                continue
            if not have and args.write_keys:
                # the key goes right after the text, before the conditions, so a line reads text, key, conditions
                obj = OrderedDict()
                if isinstance(line, str):
                    obj["text"] = line
                else:
                    obj["text"] = line.get("text", "")
                obj["key"] = want
                if not isinstance(line, str):
                    for k, v in line.items():
                        if k not in ("text", "key"):
                            obj[k] = v
                doc["lines"][ctx][i] = obj
                changed = True
                have = want
            if have:
                fragment[have] = template(line_text(doc["lines"][ctx][i]))
        if changed:
            with open(path, "w", encoding="utf-8") as fh:
                json.dump(doc, fh, indent=2, ensure_ascii=False)
                fh.write("\n")
            print("keys written: " + name, file=sys.stderr)

    if args.check:
        lang = json.load(open(args.check, encoding="utf-8"))
        for key, text in fragment.items():
            if key not in lang:
                errors.append("%s is not in %s" % (key, args.check))
            elif lang[key] != text:
                errors.append("%s differs: file says %r, language file says %r" % (key, text, lang[key]))
    for e in errors:
        print("ERROR " + e, file=sys.stderr)
    if args.out:
        with open(args.out, "w", encoding="ascii") as fh:
            json.dump(fragment, fh, indent=2, ensure_ascii=True)
            fh.write("\n")
        print("wrote %d keys to %s" % (len(fragment), args.out), file=sys.stderr)
    elif not args.check:
        print(json.dumps(fragment, indent=2, ensure_ascii=True))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
