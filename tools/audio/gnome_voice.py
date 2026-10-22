#!/usr/bin/env python3
"""The spoken lines of the Gnome, the Earth Spirit (docs/magic/gnome.md section 7): the script, the converter and the registration in ONE command.

  python3 tools/audio/gnome_voice.py <raw_dir>     convert every <raw_dir>/gnome_<line>.mp3 (the ElevenLabs takes) to sounds/voice/gnome/gnome_<line>.ogg, then register what exists
  python3 tools/audio/gnome_voice.py --sync        register what already exists (no conversion)
  python3 tools/audio/gnome_voice.py --missing     list the lines that have no recording yet (and the prompt to generate each with)

A line is REGISTERED only when its ogg exists, so the game never lists a line it cannot play. Registering means (all idempotent, nothing of other roles is touched):
  * tools/audio/voice_lines.json   the gnome entries are rewritten, in the order of LINES below, after everything else (the order of VoiceCatalog)
  * tools/audio/voice_report.json  the gnome entries (seconds, bytes) are merged in
  * assets/nusmp/sounds.json       the events 'voice.gnome_<line>' (no subtitle) are appended when missing
  * VoiceCatalog.java              the list GNOME_LINES is rewritten to the registered lines
The voice: ElevenLabs library voice "Garrison - Rugged and Stoic" (id xHxp1c5pQOzhWjBqV78M), model eleven_v4, one generation per line, no cloning of any person. The take of every line is generated
with the prompt below (the audio tags [deep, slow, gravelly] set the delivery). Needs ffmpeg and ffprobe on the path."""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
VOICE_ID = "xHxp1c5pQOzhWjBqV78M"
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "voice", "gnome")
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds.json")
LINES_JSON = os.path.join(HERE, "voice_lines.json")
REPORT = os.path.join(HERE, "voice_report.json")
CATALOG = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "audio", "voice", "VoiceCatalog.java")

# suffix -> prompt. The group (the part before the number) is the situation of GnomeMoodRules.
LINES = {
    "greet_1": "[deep, slow, gravelly] Gnome rises. The earth is with you.",
    "greet_2": "[deep, slow, gravelly] You called. I came.",
    "farewell_1": "[deep, slow, gravelly] The earth remembers you. Gnome sleeps.",
    "farewell_2": "[deep, slow, gravelly] Call again, and I will stand.",
    "cast_1": "[deep, slow, gravelly] Stone, answer.",
    "cast_2": "[deep, slow, gravelly] Earth, rise.",
    "cast_3": "[deep, slow, gravelly] Hold. Break.",
    "cast_big_1": "[deep, slow, gravelly, powerful] Mother Earth, awake!",
    "cast_big_2": "[deep, slow, gravelly, powerful] Let the ground take them!",
    "hurt_1": "[deep, slow, gravelly] Stone does not bleed.",
    "hurt_2": "[deep, slow, gravelly] Is that all?",
    "hurt_3": "[deep, slow, gravelly] Hm. A scratch.",
    "surprised_1": "[deep, gravelly, alarmed] You are hurt! Careful.",
    "lowhp_1": "[deep, slow, gravelly, firm] Gnome stands firm. Behind me.",
    "lowhp_2": "[deep, slow, gravelly, firm] You will not fall. Not while I stand.",
    "victory_1": "[deep, slow, gravelly, pleased] The earth is pleased.",
    "victory_2": "[deep, slow, gravelly, amused] Another one crumbles.",
    "death_1": "[deep, very slow, gravelly, soft] Rest now. The earth will hold you.",
    "doze_1": "[deep, very slow, gravelly, drowsy] Gnome... waits... in the stone.",
    "wake_1": "[deep, gravelly] Hm? We move.",
    "idle_1": "[deep, very slow, gravelly] Still. As the mountain is still.",
}


def line_id(suffix):
    return "gnome_" + suffix


def ogg_path(suffix):
    return os.path.join(OUT, line_id(suffix) + ".ogg")


def have():
    return [s for s in LINES if os.path.exists(ogg_path(s))]


def manifest_entries():
    """The entries of voice_lines.json for the recorded lines (also used by tools/audio/voice_lines.py)."""
    return [{"id": line_id(s), "role": "gnome", "voice": VOICE_ID, "prompt": LINES[s]} for s in have()]


def convert_all(raw):
    import voice_convert
    done = []
    for s in LINES:
        src = os.path.join(raw, line_id(s) + ".mp3")
        if os.path.exists(src):
            voice_convert.convert(src, ogg_path(s))
            done.append(s)
    return done


def register():
    from voice_convert import seconds
    got = have()
    # voice_lines.json: everything that is not gnome stays, the gnome lines follow in LINES order
    lines = [e for e in json.load(open(LINES_JSON, encoding="utf-8")) if e.get("role") != "gnome"] + manifest_entries()
    json.dump(lines, open(LINES_JSON, "w", encoding="utf-8"), indent=1)
    # voice_report.json
    report = json.load(open(REPORT, encoding="utf-8")) if os.path.exists(REPORT) else {}
    for k in [k for k in report if k.startswith("gnome_")]:
        del report[k]
    for s in got:
        report[line_id(s)] = {"seconds": round(seconds(ogg_path(s)), 2), "bytes": os.path.getsize(ogg_path(s))}
    json.dump(report, open(REPORT, "w", encoding="utf-8"), indent=1, sort_keys=True)
    # sounds.json: text append, so the rest of the file is byte for byte as it was
    raw = open(SOUNDS_JSON, encoding="utf-8", newline="").read()
    add = []
    for s in got:
        key = '"voice.%s"' % line_id(s)
        if key not in raw:
            add.append('  %s: { "sounds": [ { "name": "nusmp:voice/gnome/%s", "stream": false } ] }' % (key, line_id(s)))
    if add:
        end = raw.rstrip().rfind("}")
        raw = raw[:end].rstrip() + ",\n" + ",\n".join(add) + "\n}\n"
        json.loads(raw)
        open(SOUNDS_JSON, "w", encoding="utf-8", newline="").write(raw)
    # VoiceCatalog.java
    src = open(CATALOG, encoding="utf-8", newline="").read()
    new = "GNOME_LINES = List.of(" + ", ".join('"%s"' % s for s in got) + ");"
    out, n = re.subn(r"GNOME_LINES = List\.of\([^;]*\);", new, src)
    assert n == 1, "GNOME_LINES not found in VoiceCatalog.java"
    if out != src:
        open(CATALOG, "w", encoding="utf-8", newline="").write(out)
    total = sum(os.path.getsize(ogg_path(s)) for s in got)
    print("registered %d of %d gnome lines, %d bytes (%.1f KB)" % (len(got), len(LINES), total, total / 1024.0))
    miss = [s for s in LINES if s not in got]
    if miss:
        print("no recording yet:", ", ".join(miss))


def main():
    args = [a for a in sys.argv[1:]]
    if "--missing" in args:
        for s in LINES:
            if not os.path.exists(ogg_path(s)):
                print("%s\t%s" % (line_id(s), LINES[s]))
        return
    if args and not args[0].startswith("--"):
        done = convert_all(args[0])
        print("converted", len(done), "takes")
    register()


if __name__ == "__main__":
    main()
