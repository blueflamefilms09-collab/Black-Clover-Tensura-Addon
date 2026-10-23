#!/usr/bin/env python3
"""Merge the lang fragments of the "15 spells for every grimoire" batches into en_us.json  (run by the COORDINATOR, once, after the batches are merged).

    python3 -B tools/lang/merge_pages15.py           merge tools/lang/pages15_batch*.json into src/main/resources/assets/nusmp/lang/en_us.json (new keys are appended at the end)
    python3 -B tools/lang/merge_pages15.py --check   change nothing; exit 1 if a fragment key is missing from en_us.json or differs from it

A fragment is a flat json object {"nusmp.page.<book registry path>.<page id>": "one or two sentences"}.  An existing key of en_us.json is never changed by the merge
(rule 2: nothing is replaced); a key that is already there with another text is reported.  Builders do not run this: they only write their own fragment.
"""
import glob
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
LANG = os.path.join(ROOT, 'src/main/resources/assets/nusmp/lang/en_us.json')


def main():
    check = '--check' in sys.argv
    with open(LANG, encoding='utf-8') as f:
        text = f.read()
    lang = json.loads(text)
    add, clash, files = {}, [], sorted(glob.glob(os.path.join(ROOT, 'tools/lang/pages15_batch*.json')))
    for p in files:
        with open(p, encoding='utf-8') as f:
            frag = json.load(f)
        for k, v in frag.items():
            if not k.startswith('nusmp.page.') or not isinstance(v, str) or not v.strip():
                print('bad key or text in %s: %r' % (os.path.basename(p), k))
                return 1
            if k in lang and lang[k] != v:
                clash.append(k)
            elif k not in lang:
                if k in add and add[k] != v:
                    clash.append(k)
                add[k] = v
    for k in clash:
        print('differs from what en_us.json (or another fragment) already has: %s' % k)
    print('%d fragments, %d new keys, %d clashes' % (len(files), len(add), len(clash)))
    if check:
        return 1 if (add or clash) else 0
    if clash:
        return 1
    if not add:
        return 0
    body = text.rstrip()
    assert body.endswith('}')
    body = body[:-1].rstrip()
    lines = ',\n'.join('  %s: %s' % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in add.items())
    with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
        f.write(body + ',\n' + lines + '\n}\n')
    json.load(open(LANG, encoding='utf-8'))      # still valid
    return 0


if __name__ == '__main__':
    sys.exit(main())
