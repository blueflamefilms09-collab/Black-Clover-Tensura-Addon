#!/usr/bin/env python3
"""Merge the lang fragment of the Lords of the Mortal Sins (tools/lang/lords.json) into en_us.json.

    python3 -B tools/lang/merge_lords.py           append the new keys of tools/lang/lords.json to src/main/resources/assets/anime_multiverse/lang/en_us.json
    python3 -B tools/lang/merge_lords.py --check   change nothing; exit 1 if a fragment key is missing from en_us.json or differs from it

New keys are appended at the end. Only the four skill descriptions in REPLACE are rewritten in place (their text describes the modes, which this change extended: the decree and the form
switch); every other existing key is never changed (rule 2), a clash is reported.
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LANG = os.path.join(ROOT, 'src/main/resources/assets/anime_multiverse/lang/en_us.json')
FRAG = os.path.join(ROOT, 'tools/lang/lords.json')
REPLACE = {"anime_multiverse.skill.%s.description" % s for s in ("lord_of_slothful_weight", "lord_of_green_eyed_envy", "lord_of_empty_vanity", "seifort_holy_judgement")}


def main():
    check = '--check' in sys.argv
    text = open(LANG, encoding='utf-8').read()
    lang = json.loads(text)
    frag = json.load(open(FRAG, encoding='utf-8'))
    add, replace, clash = {}, {}, []
    for k, v in frag.items():
        if not isinstance(v, str) or not v.strip():
            print('bad text for %r' % k)
            return 1
        if k in lang and lang[k] != v:
            if k in REPLACE:
                replace[k] = v
            else:
                clash.append(k)
        elif k not in lang:
            add[k] = v
    for k in clash:
        print('differs from what en_us.json already has: %s' % k)
    print('%d fragment keys, %d new, %d replaced, %d clashes' % (len(frag), len(add), len(replace), len(clash)))
    if check:
        return 1 if (add or replace or clash) else 0
    if clash:
        return 1
    if replace:
        for k, v in replace.items():
            old = '  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(lang[k], ensure_ascii=False))
            assert old in text, k
            text = text.replace(old, '  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)), 1)
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
