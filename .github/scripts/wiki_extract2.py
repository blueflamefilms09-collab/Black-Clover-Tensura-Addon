"""Prints a cleaned reading of a Black Clover wiki page: usage  python3 wiki_extract2.py magic|spell < api-json

magic: the intro, the whole Description / Abilities-style sections, and the names of the spell pages it links to.
spell: the intro (what it is, who uses it) and the Description / Usage / Effects-style sections, in full.
Wikitext templates, links, files and refs are flattened into plain sentences."""
import json
import re
import sys

mode = sys.argv[1] if len(sys.argv) > 1 else "spell"
d = json.load(sys.stdin)
raw = d.get("parse", {}).get("wikitext", "")
if not raw:
    print("(no wikitext)", str(d)[:300])
    sys.exit()


def clean(t):
    t = re.sub(r"<!--.*?-->", "", t, flags=re.S)
    t = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", t, flags=re.S)
    prev = None
    while prev != t:
        prev = t
        t = re.sub(r"\{\{[Nn]ihongo\|([^|{}]*)(?:\|[^{}]*)?\}\}", r"\1", t)
        t = re.sub(r"\{\{[Cc]olor\|[^|{}]*\|([^{}]*)\}\}", r"\1", t)
        t = re.sub(r"\{\{[^{}]*\}\}", "", t)
    t = re.sub(r"\[\[(?:File|Image|Category):[^\]]*\]\]", "", t)
    t = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", t)
    t = re.sub(r"'{2,}|<[^>]+>", "", t)
    t = re.sub(r"\{\|.*?\|\}", "", t, flags=re.S)
    return re.sub(r"[ \t]+", " ", t).strip()


spell_links = []
for m in re.finditer(r"link=([^|\n]+)\|", raw):
    n = m.group(1).strip()
    if n and n not in spell_links:
        spell_links.append(n)

parts = re.split(r"\n(==+)\s*([^=]+?)\s*\1[ \t]*\n", "\n" + raw)
intro = clean(parts[0])
print("-- intro:", intro[:900].replace("\n", " "))
want = re.compile(r"description|abilit|usage|effect|overview|spell|technique|appearance|powers|characteristics|operation|summary|capabilit|trait|feature", re.I)
skip = re.compile(r"trivia|gallery|reference|navigation|see also|external|quote|anime|manga|episode|chapter|translat|image", re.I)
cap = 3800 if mode == "magic" else 2600
for i in range(1, len(parts) - 2, 3):
    title, body = parts[i + 1], parts[i + 2]
    if skip.search(title) or not want.search(title):
        continue
    if re.fullmatch(r"\s*spells?\s*", title, re.I) and mode == "magic":
        continue                                  # only the link names matter there (printed below)
    text = clean(body)
    lines = [l.strip() for l in text.splitlines() if l.strip() and not l.strip().startswith(("|", "!", "{", "}", "[[", "*"))]
    print(f"-- {title}:")
    out = " ".join(lines)
    print("   ", out[:cap])
if mode == "magic":
    print("-- spell pages:", "; ".join(spell_links) if spell_links else "(none listed)")
