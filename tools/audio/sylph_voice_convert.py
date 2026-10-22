#!/usr/bin/env python3
"""Turns the ElevenLabs takes of Sylph's spoken lines into the mod's voice files and registers exactly the lines whose file exists (docs/magic/sylph.md, "Voice lines").

usage: python3 -B tools/audio/sylph_voice_convert.py <raw_dir>

The raw dir holds <id>.mp3 for the lines of tools/audio/sylph_lines.json that were generated (the raw takes stay outside the repository). For every such line:
  * ffmpeg: mono, 32 kHz, leading and trailing silence trimmed, loudnorm (about -16 LUFS, true peak <= -1.5 dB), Ogg Vorbis q3 -> src/main/resources/assets/nusmp/sounds/voice/sylph/<id>.ogg
  * ffprobe: the duration, written to tools/audio/sylph_voice_report.json (id, seconds, bytes)
Then, append only (an existing entry is kept): the event 'voice.<id>' with its subtitle key to assets/nusmp/sounds.json, the subtitle text ("Sylph: <text>") to tools/lang/sylph_voice.json, and the
count of existing lines per group into the generated block of src/main/java/com/newuniverse/nusmp/sylph/SylphVoiceLines.java, so a line that was not generated is NOT registered (the voice catalogue
reads that list, the sound events and sounds.json follow it). Re-running with more takes adds more lines. A line counts for the Java list only while the ids of its group are contiguous from _1.
Needs ffmpeg and ffprobe on the path. Deterministic given the same takes."""
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "voice", "sylph")
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds.json")
LANG = os.path.join(ROOT, "tools", "lang", "sylph_voice.json")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "sylph", "SylphVoiceLines.java")
REPORT = os.path.join(HERE, "sylph_voice_report.json")
MANIFEST = os.path.join(HERE, "sylph_lines.json")
CHAIN = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.04,areverse,"
         "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
         "loudnorm=I=-16:TP=-1.5:LRA=7")
BEGIN, END = "        // BEGIN GENERATED", "        // END GENERATED"


def convert(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", src, "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", dst], check=True)


def seconds(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


def main():
    raw = sys.argv[1]
    lines = json.load(open(MANIFEST, encoding="utf-8"))
    report = json.load(open(REPORT, encoding="utf-8")) if os.path.exists(REPORT) else {}
    for ln in lines:
        src = os.path.join(raw, ln["id"] + ".mp3")
        dst = os.path.join(OUT, ln["id"] + ".ogg")
        if os.path.exists(src):
            convert(src, dst)
        if os.path.exists(dst):
            report[ln["id"]] = {"seconds": round(seconds(dst), 2), "bytes": os.path.getsize(dst)}
    have = {ln["id"] for ln in lines if os.path.exists(os.path.join(OUT, ln["id"] + ".ogg"))}
    report = {k: v for k, v in report.items() if k in have}
    json.dump(report, open(REPORT, "w", encoding="utf-8", newline="\n"), indent=1, sort_keys=True)
    open(REPORT, "a").write("\n")

    text = open(SOUNDS_JSON, encoding="utf-8").read()
    lang = json.load(open(LANG, encoding="utf-8")) if os.path.exists(LANG) else {}
    added = 0
    tail = ""
    for ln in lines:
        if ln["id"] not in have:
            continue
        key = "voice." + ln["id"]
        sub = "subtitles.nusmp.voice." + ln["id"]
        if '"%s"' % key not in text:
            tail += ',\n  "%s": {\n    "sounds": [{ "name": "nusmp:voice/sylph/%s", "stream": false }],\n    "subtitle": "%s"\n  }' % (key, ln["id"], sub)
            added += 1
        lang[sub] = 'Sylph: "%s"' % ln["text"]
    if tail:                                                                       # append only: the text of every older entry stays byte for byte
        body = text.rstrip()
        assert body.endswith("}"), "sounds.json does not end with }"
        text = body[:-1].rstrip() + tail + "\n}\n"
        json.loads(text)
        with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    with open(LANG, "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")

    # the count per group of the lines that exist (contiguous from 1), into the generated block of the Java list
    groups = []
    for ln in lines:
        if ln["group"] not in [g for g, _ in groups]:
            groups.append((ln["group"], []))
    counts = []
    for g, _ in groups:
        n = 0
        while ("sylph_%s_%d" % (g, n + 1)) in have:
            n += 1
        counts.append((g, n))
    block = BEGIN + " (tools/audio/sylph_voice_convert.py): group -> number of lines that have a file; do not edit by hand\n" + "".join('        COUNTS.put("%s", %d);\n' % (g, n) for g, n in counts if n > 0) + END
    src = open(JAVA, encoding="utf-8").read()
    new = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), lambda m: block, src, flags=re.S)
    assert new != src or block in src, "the generated block is missing in " + JAVA
    open(JAVA, "w", encoding="utf-8", newline="\n").write(new)

    total = sum(v["bytes"] for v in report.values())
    missing = [ln["id"] for ln in lines if ln["id"] not in have]
    print("lines with a file: %d of %d, %d bytes (%.1f KB); sounds.json entries added: %d; missing: %s" % (len(have), len(lines), total, total / 1024.0, added, missing))


if __name__ == "__main__":
    main()
