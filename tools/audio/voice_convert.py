#!/usr/bin/env python3
"""Turns the downloaded ElevenLabs takes into the mod's voice files and registers them (docs/audio/spoken_voices.md).

usage: python3 tools/audio/voice_convert.py <raw_dir> [--no-json]

For every line of tools/audio/voice_lines.json that has <raw_dir>/<id>.mp3:
  * ffmpeg: mono, 32 kHz, trim leading / trailing silence, loudnorm (about -16 LUFS, true peak <= -1 dB), Ogg Vorbis q3
    -> src/main/resources/assets/nusmp/sounds/voice/<role>/<id>.ogg
  * ffprobe: the duration, written to tools/audio/voice_report.json (id, seconds, bytes) so a test can check the 0.5 to 12 s range
Then appends the events 'voice.<id>' (subtitle off) to assets/nusmp/sounds.json (append-only: an existing entry is kept).
The raw takes stay outside the repository. Needs ffmpeg and ffprobe on the path."""
import json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "voice")
SOUNDS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds.json")
REPORT = os.path.join(HERE, "voice_report.json")
# the silence filter is applied twice, the second time on the reversed signal, so both ends are trimmed
CHAIN = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.04,areverse,"
         "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
         "loudnorm=I=-16:TP=-1.5:LRA=7")


def convert(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    cmd = ["ffmpeg", "-y", "-v", "error", "-i", src, "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", dst]
    subprocess.run(cmd, check=True)


def seconds(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


def main():
    raw = sys.argv[1]
    lines = json.load(open(os.path.join(HERE, "voice_lines.json"), encoding="utf-8"))
    report, missing = {}, []
    for ln in lines:
        src = os.path.join(raw, ln["id"] + ".mp3")
        if not os.path.exists(src):
            missing.append(ln["id"])
            continue
        dst = os.path.join(OUT, ln["role"], ln["id"] + ".ogg")
        convert(src, dst)
        report[ln["id"]] = {"seconds": round(seconds(dst), 2), "bytes": os.path.getsize(dst)}
    json.dump(report, open(REPORT, "w", encoding="utf-8"), indent=1, sort_keys=True)
    if "--no-json" not in sys.argv:
        doc = json.load(open(SOUNDS_JSON, encoding="utf-8"))
        added = 0
        for ln in lines:
            if ln["id"] not in report:
                continue
            key = "voice." + ln["id"]
            if key in doc:
                continue
            doc[key] = {"sounds": [{"name": "nusmp:voice/%s/%s" % (ln["role"], ln["id"]), "stream": False}]}
            added += 1
        with open(SOUNDS_JSON, "w", encoding="utf-8") as f:
            json.dump(doc, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print("sounds.json: %d events added" % added)
    total = sum(v["bytes"] for v in report.values())
    print("converted %d of %d lines, %d bytes in total (%.1f KB); missing: %s" % (len(report), len(lines), total, total / 1024.0, missing))


if __name__ == "__main__":
    main()
