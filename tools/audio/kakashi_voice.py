#!/usr/bin/env python3
"""Drop-in for the recorded Kakashi voice lines (docs/bosses/kakashi.md, section "Voice").

1. Record each line of tools/audio/kakashi_lines.json to  src/main/resources/assets/anime_multiverse/sounds/kakashi/<id>.ogg  (mono, 44.1 kHz, Ogg Vorbis).
2. Run  python3 -B tools/audio/kakashi_voice.py
   - writes data/anime_multiverse/kakashi/recorded.json (the ids whose file exists; the game registers a sound event for exactly those),
   - adds a  kakashi.voice.<id>  entry (with its subtitle key) to sounds.json for every recorded id that has none (text insert: the file's own layout is kept),
   - reports ids with a file but no manifest line, and manifest lines still without a file.
A line without a file shows its text in chat only; nothing else changes. Own line set, no ElevenLabs.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
SOUND_DIR = os.path.join(ASSETS, "sounds", "kakashi")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
RECORDED = os.path.join(ROOT, "src", "main", "resources", "data", "anime_multiverse", "kakashi", "recorded.json")
MANIFEST = os.path.join(HERE, "kakashi_lines.json")


def main():
    manifest = json.load(open(MANIFEST, encoding="utf-8"))
    ids = [l["id"] for l in manifest["lines"]]
    have = [i for i in ids if os.path.isfile(os.path.join(SOUND_DIR, i + ".ogg"))]
    extra = []
    if os.path.isdir(SOUND_DIR):
        extra = sorted(f[:-4] for f in os.listdir(SOUND_DIR) if f.endswith(".ogg") and f[:-4] not in ids)
    with open(RECORDED, "w", encoding="utf-8", newline="\n") as fh:
        json.dump({"note": "Ids of the Kakashi voice lines that have a recording (written by tools/audio/kakashi_voice.py from the .ogg files in assets/anime_multiverse/sounds/kakashi/). Empty: no audio has been recorded yet, every line shows its text only.",
                   "recorded": have}, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
    text = open(SOUNDS_JSON, encoding="utf-8").read()
    sj = json.loads(text)
    add = []
    for i in have:
        key = "kakashi.voice." + i
        if key not in sj:
            add.append('  "%s": { "subtitle": "subtitles.anime_multiverse.kakashi.%s", "sounds": [ { "name": "anime_multiverse:kakashi/%s", "stream": true } ] }' % (key, i, i))
    if add:
        end = text.rstrip().rfind("}")
        body = text[:end].rstrip()
        text = body + ",\n" + ",\n".join(add) + "\n}\n"
        json.loads(text)
        with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
    print("recorded:", len(have), "of", len(ids), "| sounds.json entries added:", len(add))
    if extra:
        print("files with no manifest line:", ", ".join(extra))
    missing = [i for i in ids if i not in have]
    print("still without a file:", len(missing))


if __name__ == "__main__":
    main()
