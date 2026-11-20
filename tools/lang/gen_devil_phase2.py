#!/usr/bin/env python3
"""Lang lines of the devil contract rules of phase 2 (docs/devil_contract_phase2.md), generated from the code so the lang file and the code can not drift apart.

    python3 -B tools/lang/gen_devil_phase2.py           read every Phase2Text.t("key", "English", ...) call of src/main/java/com/newuniverse/nusmp/devil/phase2/, write tools/lang/devil_phase2.json,
                                                          and append the new keys at the end of src/main/resources/assets/anime_multiverse/lang/en_us.json
    python3 -B tools/lang/gen_devil_phase2.py --check   change nothing; exit 1 if a key is missing from en_us.json or differs from it

Only appends (rule 2): an existing key is never changed; a key that is already there with another text is reported and left alone. The file's own layout (two-space indent, one key per line) is kept.
Every key is anime_multiverse.devil.phase2.<key>; the English text is also the fallback in the code (Component.translatableWithFallback), so a missing line still reads right.
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LANG = os.path.join(ROOT, 'src/main/resources/assets/anime_multiverse/lang/en_us.json')
FRAG = os.path.join(ROOT, 'tools/lang/devil_phase2.json')
SRC = os.path.join(ROOT, 'src/main/java/com/newuniverse/nusmp/devil/phase2')
PREFIX = 'anime_multiverse.devil.phase2.'

CALL = re.compile(r'Phase2Text\.t\(\s*"([a-z0-9_.]+)"\s*,\s*"((?:[^"\\]|\\.)*)"')
EXTRA = {
    'entity.anime_multiverse.devil_trial': 'Devil of the Trial',
}


def unescape(s):
    return s.replace('\\"', '"').replace('\\\\', '\\').replace('\\n', '\n')


def collect():
    out, clash = {}, []
    for path in sorted(glob.glob(os.path.join(SRC, '*.java'))):
        with open(path, encoding='utf-8') as f:
            text = f.read()
        for m in CALL.finditer(text):
            key, english = PREFIX + m.group(1), unescape(m.group(2))
            if key in out and out[key] != english:
                clash.append(key)
            out.setdefault(key, english)
    out.update(EXTRA)
    return out, clash


def main():
    check = '--check' in sys.argv
    frag, dup = collect()
    for k in dup:
        print('the code says two different things for %s' % k)
    if not frag:
        print('no Phase2Text.t call found')
        return 1
    with open(LANG, encoding='utf-8') as f:
        text = f.read()
    lang = json.loads(text)
    add, clash = {}, []
    for k, v in frag.items():
        if k in lang and lang[k] != v:
            clash.append(k)
        elif k not in lang:
            add[k] = v
    for k in clash:
        print('differs from what en_us.json already has: %s' % k)
    print('%d texts in the code, %d new keys, %d clashes' % (len(frag), len(add), len(clash)))
    if check:
        return 1 if add or clash or dup else 0
    with open(FRAG, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(frag, f, ensure_ascii=False, indent=2)
        f.write('\n')
    if add:
        body = text.rstrip()
        if not body.endswith('}'):
            print('en_us.json does not end with }')
            return 1
        body = body[:-1].rstrip()
        lines = ',\n'.join('  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in add.items())
        with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
            f.write(body + ',\n' + lines + '\n}\n')
    return 1 if clash or dup else 0


if __name__ == '__main__':
    sys.exit(main())
