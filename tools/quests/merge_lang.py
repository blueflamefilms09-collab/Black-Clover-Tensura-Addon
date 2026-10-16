#!/usr/bin/env python3
"""Merge tools/quests/lang/qst.lang.json into assets/nusmp/lang/en_us.json (idempotent; existing keys are never overwritten).

Why: the advancement titles live in a fragment the generator owns; the lead (or a maintainer) runs this once per regeneration so en_us.json
stays the single lang file the game reads. Keys already present with a different text are reported and kept (the owner's text wins).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json")
FRAG = os.path.join(HERE, "lang", "qst.lang.json")


def main(argv):
    target = argv[argv.index("--to") + 1] if "--to" in argv else LANG
    raw = open(target, encoding="utf-8").read()
    cur = json.loads(raw)
    frag = json.load(open(FRAG, encoding="utf-8"))
    added = {k: v for k, v in frag.items() if k not in cur}
    changed = [k for k, v in frag.items() if k in cur and cur[k] != v]
    if "--check" in argv:
        print("missing in %s: %d, different: %d" % (os.path.basename(target), len(added), len(changed)))
        return 1 if added else 0
    if not added:
        print("nothing to add")
        return 0
    # keep the file's own formatting: insert the new entries before the final closing brace
    body = raw.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    indent = "  "
    lines = ",\n".join('%s%s: %s' % (indent, json.dumps(k), json.dumps(v, ensure_ascii=True)) for k, v in added.items())
    body = body + ",\n" + lines + "\n}\n"
    json.loads(body)   # must still be valid
    open(target, "w", encoding="utf-8", newline="\n").write(body)
    print("added %d keys to %s (%d differing keys kept)" % (len(added), target, len(changed)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
