"""Appends the keys of a tools/lang fragment that en_us.json does not have yet (append-only, rule 2), at the end of the file.

    python3 -B tools/lang/merge_fragment.py tools/lang/<fragment>.json
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
EN = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "lang", "en_us.json")


def main(frag_path):
    s = open(EN, encoding="utf-8").read()
    cur = json.loads(s)
    add = [(k, v) for k, v in json.load(open(frag_path, encoding="utf-8")).items() if k not in cur]
    body = s.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    for k, v in add:
        body += ",\n  " + json.dumps(k) + ": " + json.dumps(v, ensure_ascii=False)
    body += "\n}\n" if s.endswith("\n") else "\n}"
    json.loads(body)
    open(EN, "w", encoding="utf-8").write(body)
    print(len(add), "key(s) added")


if __name__ == "__main__":
    main(sys.argv[1])
