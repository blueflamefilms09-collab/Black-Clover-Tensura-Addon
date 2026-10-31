#!/usr/bin/env python3
"""Applies the look fragments of tools/arc_hd/looks/<name>.json to assets/nusmp/playermodel/looks.json (3.38, docs/arc/hd_arc.md).

    python3 -B tools/arc_hd/merge_looks.py            # replace the entries of the names that have a fragment; every other entry stays byte for byte as it is
    python3 -B tools/arc_hd/merge_looks.py --check    # change nothing; exit 1 when looks.json does not equal the merge of the fragments, or a fragment's skin breaks the kit's rules
    options: --frag-dir DIR, --skins-dir DIR, --looks FILE (default: the repo's), --add (a fragment whose name is not in the table is appended instead of refused)

looks.json is written by tools/playermodel/gen_skins.py with json.dump(indent=1); this script reads and writes the same way, so the key order, the indentation and every untouched entry are
identical (it first checks that reading and writing the file as it is gives the same bytes, and refuses to go on when not). The top-level `note` names this tool as a writer of the file. The file is
replaced atomically. tools/playermodel/gen_skins.py keeps the HD names out of its own regeneration (it reads the same fragments), so the two tools never undo each other.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import arc_hdlib as A  # noqa: E402


def dump(doc):
    return json.dumps(doc, indent=1)


def merge(doc, frags, add=False):
    """-> (new doc, [replaced names], [problems]). `doc` is the parsed looks.json."""
    new = json.loads(json.dumps(doc))                               # a deep copy that keeps the key order
    looks = new["looks"]
    replaced, problems = [], []
    for name, fr in frags.items():
        if name not in looks and not add:
            problems.append("%s: not in looks.json (use --add to append it)" % name)
            continue
        looks[name] = fr["look"]
        replaced.append(name)
    if replaced:
        new["note"] = A.GS.NOTE_HD
    return new, replaced, problems


def fragment_problems(frags, skins_dir=None):
    """The kit's rules for every fragment: its skin PNG exists and has the size the fragment says, the glow map exists exactly when the look says `glow` and has the skin's size."""
    sd = skins_dir or A.SKINS
    out = []
    for name, fr in frags.items():
        look = fr["look"]
        skin = os.path.join(sd, look["skin"] + ".png")
        glow = os.path.join(sd, look["skin"] + "_glow.png")
        out += A.png_problems(skin, glow, fr.get("size"))
        if look.get("glow") and not os.path.isfile(glow):
            out.append("%s says glow but %s is missing" % (name, os.path.basename(glow)))
        if not look.get("glow") and os.path.isfile(glow):
            out.append("%s has a glow map %s but the look does not say glow" % (name, os.path.basename(glow)))
        h = look.get("hair")
        if h and h not in A.HM.known_styles():
            out.append("%s: hair %s is not in hair_styles.json" % (name, h))
    return out


def main(argv):
    check = "--check" in argv
    add = "--add" in argv
    frag_dir = argv[argv.index("--frag-dir") + 1] if "--frag-dir" in argv else None
    skins_dir = argv[argv.index("--skins-dir") + 1] if "--skins-dir" in argv else None
    path = argv[argv.index("--looks") + 1] if "--looks" in argv else A.LOOKS_JSON
    text = open(path).read()
    doc = json.loads(text)
    if dump(doc) != text:
        raise SystemExit("refusing to go on: reading and writing %s does not give the same bytes (the file was edited by hand or by another tool)" % path)
    frags = A.read_fragments(frag_dir)
    new, replaced, problems = merge(doc, frags, add)
    new_text = dump(new)
    problems += fragment_problems(frags, skins_dir)
    changed = [n for n in replaced if doc["looks"].get(n) != new["looks"][n]]
    if check:
        if new_text != text:
            problems.append("looks.json differs from the merge of the fragments (%d names: %s); run merge_looks.py" % (len(changed), ", ".join(changed[:6]) + (" ..." if len(changed) > 6 else "")))
        for p in problems:
            print("PROBLEM", p)
        print("checked %d fragments: %s" % (len(frags), "OK" if not problems else "%d problems" % len(problems)))
        return 1 if problems else 0
    if problems:
        for p in problems:
            print("PROBLEM", p)
        print("nothing written")
        return 1
    if new_text == text:
        print("looks.json is already up to date (%d fragments)" % len(frags))
        return 0
    tmp = path + ".tmp"
    with open(tmp, "w") as f:
        f.write(new_text)
    os.replace(tmp, path)
    print("looks.json: replaced %d of %d fragments' entries (%s)" % (len(changed), len(frags), ", ".join(changed[:8]) + (" ..." if len(changed) > 8 else "")))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
