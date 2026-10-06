"""Prints the useful parts of a Black Clover wiki page's wikitext: the Description (first lines) and, for magic / team pages,
every section that lists spells, users or captains (bullet / table lines, links flattened)."""
import json
import re
import sys

d = json.load(sys.stdin)
text = d.get("parse", {}).get("wikitext", "")
if not text:
    print("(no wikitext)", str(d)[:300])
    sys.exit()
text = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", text, flags=re.S)
text = re.sub(r"\{\{[Rr]ef[^}]*\}\}", "", text)


def flat(s):
    s = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", s)
    s = re.sub(r"'''?|<[^>]+>", "", s)
    return s.strip()


sections = re.split(r"\n(==+)\s*([^=]+?)\s*\1\s*\n", "\n" + text)
print("-- intro:", flat(sections[0])[:500].replace("\n", " "))
wanted = re.compile(r"description|spell|user|captain|ability|abilities|known|member|commander|squad|magic", re.I)
for i in range(1, len(sections) - 2, 3):
    title, body = sections[i + 1], sections[i + 2]
    if not wanted.search(title):
        continue
    lines = [flat(l) for l in body.splitlines() if l.strip()]
    keep = [l for l in lines if l and not l.startswith(("{|", "|}", "[[File", "File:", "{{"))]
    print(f"-- {title}:")
    for l in keep[:60]:
        print("   ", l[:220])
