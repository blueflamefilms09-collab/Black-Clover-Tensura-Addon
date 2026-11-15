"""Reads every relic def out of the wave sources (no game, no compiler) and says what each one is: id, card name, grade, slot, look, palette, flags.

    python3 tools/relics/relic_inventory.py            # prints a summary
    from relic_inventory import relics                # list of dicts, for the hand-model generators

Same source-reading idea as src/test/java/com/newuniverse/nusmp/relics/RelicRenderCoverageTest (the test is the check, this is the tool the generators use).
A def whose id is not a string literal or an ID_ constant of its own wave is reported in `unresolved` and skipped.
"""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
WAVES = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "wave")

LOOK_DEFAULT = {"MAIN_HAND": "SWORD", "HEAD": "CIRCLET", "CHEST": "PLATE", "NECK": "AMULET", "BACK": "CLOAK", "BELT": "BELT", "HANDS": "GAUNTLETS",
                "CHARM": "CHARM", "FEET": "BOOTS", "CONSUMABLE": "POTION"}


def _balanced(src, i):
    """src[i] is '(': returns the index after the matching ')', skipping string literals."""
    depth = 0
    n = len(src)
    while i < n:
        c = src[i]
        if c == '"':
            i += 1
            while i < n and src[i] != '"':
                i += 2 if src[i] == '\\' else 1
        elif c == "'":
            i += 1
            while i < n and src[i] != "'":
                i += 2 if src[i] == '\\' else 1
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return n


def _split_args(s):
    out, depth, cur, i = [], 0, [], 0
    while i < len(s):
        c = s[i]
        if c == '"':
            j = i + 1
            while j < len(s) and s[j] != '"':
                j += 2 if s[j] == '\\' else 1
            cur.append(s[i:j + 1])
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == ',' and depth == 0:
            out.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    out.append("".join(cur).strip())
    return out


def _hex(a):
    m = re.fullmatch(r"\s*0x([0-9A-Fa-f]+)\s*", a)
    return int(m.group(1), 16) if m else None


def _all_consts():
    g = {}
    for f in glob.glob(os.path.join(WAVES, "*.java")):
        cls = os.path.basename(f)[:-5]
        for k, v in re.findall(r'\b(\w+)\s*=\s*"([a-z0-9_]+)"', open(f, encoding="utf8").read()):
            g[cls + "." + k] = v
    return g


def relics():
    out, unresolved = [], []
    glob_consts = _all_consts()
    for f in sorted(glob.glob(os.path.join(WAVES, "*.java"))):
        t = open(f, encoding="utf8").read()
        cls = os.path.basename(f)[:-5]
        wave = re.fullmatch(r"Wave(\d+)", cls)
        wave = int(wave.group(1)) if wave else (-1 if cls == "WaveBoss" else None)
        consts = dict(re.findall(r'\b(\w+)\s*=\s*"([a-z0-9_]+)"', t))
        for m in re.finditer(r"RelicDef\s*\.\s*of\s*\(", t):
            s = m.end() - 1
            e = _balanced(t, s)
            args = _split_args(t[s + 1:e - 1])
            if len(args) < 4:
                continue
            ident = args[0]
            if ident.startswith('"'):
                rid = ident[1:-1]
            else:
                rid = glob_consts.get(ident) if "." in ident else consts.get(ident)
            if not rid:
                unresolved.append((cls, ident))
                continue
            sl = re.search(r"RelicSlot\.(\w+)", args[3])
            if not sl:
                unresolved.append((cls, ident))
                continue
            slot = sl.group(1)
            name = args[1][1:-1] if args[1].startswith('"') else args[1]
            grade = args[2].replace("Grade.", "")
            methods = {}
            i = e
            while True:
                mm = re.compile(r"\s*\.\s*(\w+)\s*\(").match(t, i)
                if not mm:
                    break
                st = mm.end() - 1
                en = _balanced(t, st)
                methods.setdefault(mm.group(1), []).append(t[st + 1:en - 1])
                i = en
            look = LOOK_DEFAULT[slot]
            if "look" in methods:
                lk = re.search(r"Look\.(\w+)", methods["look"][0])
                if lk:
                    look = lk.group(1)
            pal = None
            if "palette" in methods:
                p = [_hex(a) for a in _split_args(methods["palette"][0])]
                if len(p) == 3 and None not in p:
                    pal = p
            ranged = "BOW" if "bow" in methods else "CROSSBOW" if "crossbow" in methods else None
            if "ranged" in methods:
                rk = re.search(r"(BOW|CROSSBOW)", methods["ranged"][0])
                ranged = rk.group(1) if rk else ranged
            d = dict(id=rid, name=name, grade=grade, slot=slot, look=look, palette=pal, model3d="model3d" in methods, adopted="adopt" in methods,
                     pair="pair" in methods, ranged=ranged, wave=wave, file=cls)
            out.append(d)
            if d["pair"]:
                left = dict(d)
                left["id"] = rid + "_left"
                left["name"] = (methods["pair"][0].strip()[1:-1] if methods["pair"][0].strip().startswith('"') else name + " (Left)")
                left["pair"] = False
                left["pair_left"] = True
                out.append(left)
    # the Ancient Notebooks come from a helper of Wave17: notebook(out, id, name, check, main, trim, glow, desc)
    t17 = open(os.path.join(WAVES, "Wave17.java"), encoding="utf8").read()
    for m in re.finditer(r'notebook\(out,\s*"([a-z0-9_]+)",\s*"([^"]+)",\s*"[^"]*",\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+),\s*(0x[0-9A-Fa-f]+)', t17):
        out.append(dict(id=m.group(1), name=m.group(2), grade="NORMAL", slot="CHARM", look="CHARM", palette=[int(m.group(i), 16) for i in (3, 4, 5)], model3d=False,
                        adopted=False, pair=False, ranged=None, wave=17, file="Wave17"))
    unresolved = [u for u in unresolved if u != ("Wave17", "id")]
    return out, unresolved


def main():
    rs, bad = relics()
    from collections import Counter
    print(len(rs), "defs;", len(bad), "unresolved", bad)
    print(Counter(r["slot"] for r in rs))
    print("adopted:", sum(r["adopted"] for r in rs), " main-hand without model3d:", [r["id"] for r in rs if r["slot"] == "MAIN_HAND" and not r["model3d"]])


if __name__ == "__main__":
    sys.exit(main())
