"""Collects every message of the exclusive tombs' Java code into the lang fragment: python3 -B tools/rift/gen_pocket_lang.py

It reads Component.translatableWithFallback("key", "English text" ...) (and WaypointService.say(p, "key", "English text" ...), which prefixes message.nusmp.mega.rift.) in the sources of
labyrinth/mega/rift/pocket, RiftAdmin-adjacent files and the rift command, and merges the pairs into tools/lang/rift_pockets.json (existing keys win, keys sorted)."""
import glob
import json
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
J = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
FILES = glob.glob(os.path.join(J, "labyrinth", "mega", "rift", "pocket", "*.java")) + [os.path.join(J, "labyrinth", "mega", "rift", "RiftCommand.java"), os.path.join(J, "labyrinth", "mega", "rift", "RiftService.java")]
STR = r'"((?:[^"\\]|\\.)*)"'
TWF = re.compile(r'translatableWithFallback\(\s*' + STR + r'\s*,\s*' + STR)
SAY = re.compile(r'WaypointService\.say\(\s*\w+\s*,\s*' + STR + r'\s*,\s*' + STR)
CMD = re.compile(r'\btext\(\s*' + STR + r'\s*,\s*' + STR)


def un(s):
    return json.loads('"' + s + '"')


def main():
    path = os.path.join(ROOT, "tools", "lang", "rift_pockets.json")
    d = json.load(open(path, encoding="utf-8"))
    en = json.load(open(os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json"), encoding="utf-8"))
    n = 0
    for f in FILES:
        t = open(f, encoding="utf-8").read()
        for rx, pre in ((TWF, ""), (SAY, "message.nusmp.mega.rift."), (CMD, "message.nusmp.mega.rift.cmd." if f.endswith("RiftCommand.java") else None)):
            if pre is None:
                continue
            for m in rx.finditer(t):
                k = pre + un(m.group(1))
                if not k.startswith("message.nusmp.mega.") and not k.startswith("pocket.nusmp."):
                    continue
                if "pocket.nusmp." in k and "\" +" in t[m.start():m.end() + 6]:
                    continue
                if k not in d and k not in en:
                    d[k] = un(m.group(2))
                    n += 1
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(dict(sorted(d.items())), fh, indent=2, ensure_ascii=True)
        fh.write("\n")
    print("added %d keys, %d in the fragment" % (n, len(d)))


if __name__ == "__main__":
    main()
