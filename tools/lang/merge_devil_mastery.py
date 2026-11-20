#!/usr/bin/env python3
"""Merge the lang fragment of Devil Awakening (devil mastery) (tools/lang/devil_mastery.json) into en_us.json.

    python3 -B tools/lang/merge_devil_mastery.py           append the fragment's new keys at the end of src/main/resources/assets/anime_multiverse/lang/en_us.json
    python3 -B tools/lang/merge_devil_mastery.py --check   change nothing; exit 1 if a fragment key is missing from en_us.json or differs from it

Only appends (rule 2): an existing key is never changed; a key that is already there with another text is reported and left alone. The file's own layout (two-space indent, one key per line) is kept.
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LANG = os.path.join(ROOT, 'src/main/resources/assets/anime_multiverse/lang/en_us.json')
FRAG = os.path.join(ROOT, 'tools/lang/devil_mastery.json')


def main():
    check = '--check' in sys.argv
    with open(LANG, encoding='utf-8') as f:
        text = f.read()
    lang = json.loads(text)
    with open(FRAG, encoding='utf-8') as f:
        frag = json.load(f)
    add, clash = {}, []
    for k, v in frag.items():
        if not isinstance(v, str) or not v.strip():
            print('bad text for %s' % k)
            return 1
        if k in lang and lang[k] != v:
            clash.append(k)
        elif k not in lang:
            add[k] = v
    for k in clash:
        print('differs from what en_us.json already has: %s' % k)
    print('%d new keys, %d clashes' % (len(add), len(clash)))
    if check:
        return 1 if add or clash else 0
    if not add:
        return 1 if clash else 0
    body = text.rstrip()
    if not body.endswith('}'):
        print('en_us.json does not end with }')
        return 1
    body = body[:-1].rstrip()
    lines = ',\n'.join('  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in add.items())
    with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
        f.write(body + ',\n' + lines + '\n}\n')
    return 1 if clash else 0


if __name__ == '__main__':
    sys.exit(main())
