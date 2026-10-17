#!/usr/bin/env python3
"""Why: every line of the Soul Forge is written once, in the Java code, as a translation key together with its English words (ForgeText.t / ForgeText.s,
Component.translatableWithFallback, and the ritual's note(...)), so the screen is readable even before a language file knows the key. The language file is
a flat JSON that the lead merges into assets/nusmp/lang/en_us.json; this tool WRITES that fragment from the code, so the two can never drift apart. It also
refuses to write when the code contradicts itself (one key with two different English texts) and lists every plain Component.translatable("literal") key of
the Avatar code that is in neither en_us.json nor the fragment, which would show up as a raw key in game.

    python3 -B tools/avatar/gen_soulforge_lang.py --out <fragment.json>          write the fragment
    python3 -B tools/avatar/gen_soulforge_lang.py --out <fragment.json> --check  exit 1 when the file on disk differs from what the code says

Keys that en_us.json already holds are never written to the fragment (the existing text wins; a different text is reported). Non-ASCII characters are
written as \\uXXXX escapes, as the build rules ask.
"""
import argparse
import json
import os
import re
import sys

sys.dont_write_bytecode = True

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
EN_US = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json")
SCAN = [os.path.join(JAVA, "avatar"), os.path.join(JAVA, "client", "avatar")]

# texts that are not in code at all (names of registered things)
MANUAL = {
    "block.nusmp.soul_forge": "Soul Forge",
}

LIT = r'"((?:[^"\\]|\\.)*)"'
CALLS = [
    re.compile(r'ForgeText\s*\.\s*[ts]\s*\(\s*' + LIT + r'\s*,\s*' + LIT),
    re.compile(r'translatableWithFallback\s*\(\s*' + LIT + r'\s*,\s*' + LIT),
    re.compile(r'\bnote\s*\(\s*' + LIT + r'\s*,\s*' + LIT),
]
KEY_ONLY = [
    re.compile(r'ForgeText\s*\.\s*[ts]\s*\(\s*' + LIT),
    re.compile(r'translatableWithFallback\s*\(\s*' + LIT),
    re.compile(r'\bnote\s*\(\s*' + LIT + r'\s*,'),
]
PLAIN = re.compile(r'Component\s*\.\s*translatable\s*\(\s*' + LIT + r'\s*[,)]')


def unescape(lit):
    """A Java string literal body to its text (the escapes the code uses: \\" \\\\ \\n \\t \\uXXXX)."""
    out, i = [], 0
    while i < len(lit):
        c = lit[i]
        if c != "\\":
            out.append(c)
            i += 1
            continue
        n = lit[i + 1]
        if n == "u":
            out.append(chr(int(lit[i + 2:i + 6], 16)))
            i += 6
            continue
        out.append({"n": "\n", "t": "\t", '"': '"', "\\": "\\", "'": "'"}.get(n, n))
        i += 2
    return "".join(out)


def java_files():
    for base in SCAN:
        for dp, _dn, fn in os.walk(base):
            for f in sorted(fn):
                if f.endswith(".java"):
                    yield os.path.join(dp, f)


def collect():
    found, problems, plain = {}, [], {}
    for path in java_files():
        text = open(path, encoding="utf-8").read()
        rel = os.path.relpath(path, ROOT)
        parsed_keys = set()
        for rx in CALLS:
            for m in rx.finditer(text):
                key, fb = unescape(m.group(1)), unescape(m.group(2))
                parsed_keys.add((rx.pattern, m.start()))
                if key in found and found[key][0] != fb:
                    problems.append("conflict: %s has two texts: %r (%s) and %r (%s)" % (key, found[key][0], found[key][1], fb, rel))
                found.setdefault(key, (fb, rel))
        # a call whose key is a literal but whose fallback is not (a concatenation, a variable): would be silently missed
        for rx, full in zip(KEY_ONLY, CALLS):
            starts = {m.start() for m in full.finditer(text)}
            for m in rx.finditer(text):
                if m.start() not in starts:
                    key = unescape(m.group(1))
                    if key.endswith("."):                    # "nusmp.avatar.cond." + id: a family of keys the language file already holds
                        continue
                    line = text.count("\n", 0, m.start()) + 1
                    problems.append("%s:%d: key %r has no plain-literal fallback (use one string literal)" % (rel, line, key))
        for m in PLAIN.finditer(text):
            plain.setdefault(unescape(m.group(1)), rel)
    return found, problems, plain


def build():
    found, problems, plain = collect()
    en = json.load(open(EN_US, encoding="utf-8"))
    frag, notes = {}, []
    for key, (fb, rel) in sorted(found.items()):
        if key in en:
            if en[key] != fb:
                notes.append("note: %s is already in en_us.json as %r; the code's fallback %r (%s) is not written" % (key, en[key], fb, rel))
            continue
        frag[key] = fb
    for key, txt in MANUAL.items():
        if key in en:
            continue
        frag[key] = txt
    for key, rel in sorted(plain.items()):
        if key not in en and key not in frag:
            problems.append("%s uses Component.translatable(%r) but neither en_us.json nor the fragment has it" % (rel, key))
    return frag, problems, notes


def render(frag):
    return json.dumps(frag, indent=2, ensure_ascii=True, sort_keys=True) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", required=True, help="the fragment file to write (or check)")
    ap.add_argument("--check", action="store_true", help="do not write; exit 1 if the file differs")
    args = ap.parse_args()
    frag, problems, notes = build()
    for n in notes:
        print(n)
    for p in problems:
        print("PROBLEM:", p)
    if problems:
        return 1
    text = render(frag)
    if args.check:
        cur = open(args.out, encoding="utf-8").read() if os.path.isfile(args.out) else None
        if cur != text:
            print("the fragment differs from the code: run without --check")
            return 1
        print("the fragment matches the code: %d keys" % len(frag))
        return 0
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    open(args.out, "w", encoding="utf-8", newline="\n").write(text)
    print("wrote %d keys to %s" % (len(frag), args.out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
