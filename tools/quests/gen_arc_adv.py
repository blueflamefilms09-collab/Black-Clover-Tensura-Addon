#!/usr/bin/env python3
"""Regenerate the advancements of the characters of the arcs from the roster file.

Why: the characters are written by many builders and merged one by one; the owner wants an advancement for every one of them, and the lead must be able to
refresh them in one step after a merge without knowing the generator. The roster file (tools/quests/arc_roster.txt, `id|name|saga|role` per line) is the
bridge: the in-game command `/multiverse arc dump` writes it from the live roster, this script reads it (through spec_arc.py, which the advancement spec imports)
and runs the ordinary advancement generator, which writes the JSON files under data/nusmp/advancement/arc and refreshes tools/quests/lang/qst.lang.json.

Usage:
    python tools/quests/gen_arc_adv.py                      regenerate from tools/quests/arc_roster.txt
    python tools/quests/gen_arc_adv.py --check              only report what is out of date (exit 1 if something is)
    python tools/quests/gen_arc_adv.py ID|NAME|SAGA|ROLE ...  add these characters to the roster file first (no duplicates; an id already there is left as it is)
    python tools/quests/gen_arc_adv.py --roster FILE        use another roster file (the ARC_ROSTER environment variable does the same)
Afterwards merge the lang fragment into the game's lang file (python tools/quests/merge_lang.py) and run AdvancementDataTest.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)


def main(argv):
    args = list(argv)
    check = "--check" in args
    if check:
        args.remove("--check")
    if "--roster" in args:
        i = args.index("--roster")
        os.environ["ARC_ROSTER"] = os.path.abspath(args[i + 1])
        del args[i:i + 2]
    import spec_arc  # noqa: E402  (read after ARC_ROSTER is set)
    path = spec_arc.ROSTER_FILE
    if args:
        known = {c["id"] for c in spec_arc.read_roster(path)}
        added = []
        for line in args:
            parts = [p.strip() for p in line.split("|")]
            if len(parts) != 4:
                print("not a roster line (id|name|saga|role): %r" % line)
                return 2
            if parts[0] in known:
                print("%s is already in the roster file, left as it is" % parts[0])
                continue
            known.add(parts[0])
            added.append("|".join(parts))
        if added:
            text = open(path, encoding="utf-8").read() if os.path.exists(path) else "# id|name|saga|role : written by /multiverse arc dump, read by tools/quests/spec_arc.py\n"
            if text and not text.endswith("\n"):
                text += "\n"
            with open(path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(text + "\n".join(added) + "\n")
            spec_arc.read_roster(path)                                       # validates what was written
            print("added to %s: %s" % (os.path.basename(path), ", ".join(a.split("|")[0] for a in added)))
    cmd = [sys.executable, "-B", os.path.join(HERE, "gen_advancements.py")] + (["--check"] if check else [])
    return subprocess.call(cmd, env=dict(os.environ))


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
