"""Shared by tools/audio/spirit_light_voice.py and tools/gen_spirit_light_sounds.py: keeps the Spirit of Light's entries in assets/nusmp/sounds.json as ONE block of single-line entries right after
the first entry ("zagred_word"), so the rest of the hand-formatted file is never rewritten and the block merges cleanly. Entries are written as

  "spirit_light.link": { "subtitle": "...", "sounds": [ { "name": "nusmp:spirit_light/link_1" }, ... ] },
  "spirit_light.voice.spiritlight_greet_1": { "sounds": [ { "name": "nusmp:spirit_light/voice/spiritlight_greet_1", "stream": false } ] },

(the spoken lines have no subtitle, like every other spoken voice line of the mod: the line's text is shown on the owner's action bar instead).
"""
import json
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds.json")
PREFIX = "spirit_light."
ENTRY = re.compile(r'^\s*"(spirit_light\.[^"]+)":\s*(\{.*\}),?\s*$')


def read_block():
    """The Spirit of Light entries now in the file: {key: dict}."""
    out = {}
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        for line in f:
            m = ENTRY.match(line)
            if m:
                out[m.group(1)] = json.loads(m.group(2))
    return out


def write_block(entries):
    """Replaces the block with `entries` ({key: dict}, written in the order given: sfx first, then voice)."""
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        lines = f.read().split("\n")
    kept = [ln for ln in lines if not ENTRY.match(ln)]
    at = None
    for i, ln in enumerate(kept):
        if ln.startswith('  "zagred_word"'):
            j = i
            while not kept[j].startswith("  },"):
                j += 1
            at = j + 1
            break
    if at is None:
        raise SystemExit("sounds.json: the anchor entry zagred_word was not found")
    block = ["  %s: %s," % (json.dumps(k), json.dumps(v, ensure_ascii=False).replace('{"', '{ "').replace('"}', '" }').replace("[{", "[ {").replace("}]", "} ]")) for k, v in entries.items()]
    out = kept[:at] + block + kept[at:]
    with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out))
    json.loads(open(SOUNDS_JSON, encoding="utf-8").read())          # still valid JSON
    return len(block)


def split(entries):
    """(sfx, voice) key dicts of a block."""
    sfx = {k: v for k, v in entries.items() if not k.startswith("spirit_light.voice.")}
    voice = {k: v for k, v in entries.items() if k.startswith("spirit_light.voice.")}
    return sfx, voice
