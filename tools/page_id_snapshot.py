#!/usr/bin/env python3
"""Writes the page-id snapshot that ArmourPolicyTest compares the sources against: for every java file under book/ the grimoire page ids it defines, in source order
(`starter("id"`, `mid("id"`, `zone("id"`, `signature("id"`, `daily("id"` and `new BookPage("id"`, the same regular expression the test uses).

    python3 -B tools/page_id_snapshot.py                 snapshot of the working tree
    python3 -B tools/page_id_snapshot.py --rev <commit>  snapshot of a commit (the previous release), read with `git show`

The committed snapshot (src/test/resources/armour_policy/page_ids_1_98.txt) was made from the 1.98.0 release commit, before the armour policy changed any book.
"""
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
BOOKS = 'src/main/java/com/newuniverse/nusmp/book'
PAT = re.compile(r'(?:\b(?:starter|mid|zone|signature|daily)|new BookPage)\(\s*"([a-z0-9_]+)"')


def files_at(rev):
    if rev is None:
        out = []
        for d, _, fs in os.walk(os.path.join(ROOT, BOOKS)):
            for f in fs:
                if f.endswith('.java'):
                    out.append(os.path.relpath(os.path.join(d, f), os.path.join(ROOT, BOOKS)).replace(os.sep, '/'))
        return sorted(out)
    lst = subprocess.run(['git', 'ls-tree', '-r', '--name-only', rev, BOOKS], cwd=ROOT, capture_output=True, text=True, check=True).stdout.split()
    return sorted(p[len(BOOKS) + 1:] for p in lst if p.endswith('.java'))


def read(rev, rel):
    if rev is None:
        return open(os.path.join(ROOT, BOOKS, rel), encoding='utf-8').read()
    return subprocess.run(['git', 'show', '%s:%s/%s' % (rev, BOOKS, rel)], cwd=ROOT, capture_output=True, text=True, check=True).stdout


def main():
    rev = sys.argv[sys.argv.index('--rev') + 1] if '--rev' in sys.argv else None
    lines = ['# grimoire page ids per source file under book/ (source order); made by tools/page_id_snapshot.py from ' + (rev or 'the working tree')]
    n = 0
    for rel in files_at(rev):
        ids = PAT.findall(read(rev, rel))
        if ids:
            lines.append(rel + ': ' + ' '.join(ids))
            n += len(ids)
    out = os.path.join(ROOT, 'src/test/resources/armour_policy/page_ids_1_98.txt') if rev else None
    text = '\n'.join(lines) + '\n'
    if out:
        os.makedirs(os.path.dirname(out), exist_ok=True)
        open(out, 'w', encoding='utf-8').write(text)
        print('wrote', out, len(lines) - 1, 'files', n, 'ids')
    else:
        sys.stdout.write(text)


if __name__ == '__main__':
    main()
