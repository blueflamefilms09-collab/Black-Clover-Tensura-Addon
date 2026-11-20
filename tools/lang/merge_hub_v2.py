#!/usr/bin/env python3
"""Merge the Hub v2 lang fragment (tools/lang/hub_v2.json) into en_us.json.

    python3 -B tools/lang/merge_hub_v2.py           append the new keys of tools/lang/hub_v2.json to src/main/resources/assets/anime_multiverse/lang/en_us.json
    python3 -B tools/lang/merge_hub_v2.py --check   change nothing; exit 1 if a fragment key is missing from en_us.json or differs from it

New keys are appended at the end; no existing key is ever changed (rule 2): a clash is reported and nothing is written.
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LANG = os.path.join(ROOT, 'src/main/resources/assets/anime_multiverse/lang/en_us.json')
FRAG = os.path.join(ROOT, 'tools/lang/hub_v2.json')


def main():
    check = '--check' in sys.argv
    text = open(LANG, encoding='utf-8').read()
    lang = json.loads(text)
    frag = json.load(open(FRAG, encoding='utf-8'))
    add, clash = {}, []
    for k, v in frag.items():
        if not isinstance(v, str) or not v.strip():
            print('bad text for %r' % k)
            return 1
        if k in lang and lang[k] != v:
            clash.append(k)
        elif k not in lang:
            add[k] = v
    for k in clash:
        print('differs from what en_us.json already has: %s' % k)
    print('%d fragment keys, %d new, %d clashes' % (len(frag), len(add), len(clash)))
    if check:
        return 1 if (add or clash) else 0
    if clash:
        return 1
    if add:
        body = text.rstrip()
        assert body.endswith('}')
        body = body[:-1].rstrip()
        lines = ',\n'.join('  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in add.items())
        text = body + ',\n' + lines + '\n}\n'
        with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
        json.load(open(LANG, encoding='utf-8'))      # still valid
    return 0


if __name__ == '__main__':
    sys.exit(main())
