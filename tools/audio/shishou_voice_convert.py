#!/usr/bin/env python3
"""Turns the downloaded ElevenLabs takes of Shishou's spoken lines into the mod's voice files and registers exactly those (docs/items/shishou.md, "Voice"; docs/audio/spoken_voices.md).

usage: python3 -B tools/audio/shishou_voice_convert.py <raw_dir> [--no-json]

For every line of tools/audio/shishou_lines.json that has <raw_dir>/<id>.mp3:
  * ffmpeg: mono, 32 kHz, trim leading / trailing silence, loudnorm (about -16 LUFS, true peak <= -1.5 dB), Ogg Vorbis q3
    -> src/main/resources/assets/nusmp/sounds/voice/shishou/<id>.ogg   (the project's voice format, the same chain as tools/audio/undine_voice_convert.py)
  * ffprobe: the duration; written with the size to tools/audio/shishou_voice_report.json (converted ids, missing ids, bytes)
Then it keeps four things in step, so a line is registered if and only if its file exists:
  * assets/nusmp/sounds.json gets 'voice.<id>' (appended as text, the rest of the file is not reformatted; an existing entry is kept) with a subtitle key
  * tools/lang/shishou_voice.json gets 'subtitles.nusmp.voice.<id>' = 'Shishou: "<words>"' (the owner merges the fragment into en_us.json)
  * src/main/java/com/newuniverse/nusmp/shishou/ShishouVoiceLines.java: the REGISTERED array between its two marker comments is rewritten
A line with no take and no file is simply not registered (its moment stays Thought Communication text only); a file made by an earlier run stays registered even when its take is not in <raw_dir>; the raw takes
stay outside the repository. Run with no takes at all it writes an empty report (nothing is registered). Needs ffmpeg and ffprobe on the path (only when there is something to convert)."""
import json, os, re, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ASSETS, "sounds", "voice", "shishou")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
LANG = os.path.join(ROOT, "tools", "lang", "shishou_voice.json")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "shishou", "ShishouVoiceLines.java")
REPORT = os.path.join(HERE, "shishou_voice_report.json")
CHAIN = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.04,areverse,"
         "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
         "loudnorm=I=-16:TP=-1.5:LRA=7")
MIN_SECONDS, MAX_SECONDS = 0.5, 12.0


def convert(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", src, "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", dst], check=True)


def seconds(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


def sounds_entry(i):
    return ('  "voice.%s": {\n    "subtitle": "subtitles.nusmp.voice.%s",\n    "sounds": [\n      { "name": "nusmp:voice/shishou/%s", "stream": false }\n    ]\n  }' % (i, i, i))


def main():
    raw = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else ""
    doc = json.load(open(os.path.join(HERE, "shishou_lines.json"), encoding="utf-8"))
    lines = doc["lines"]
    report, missing, rejected = {}, [], []
    for ln in lines:
        src = os.path.join(raw, ln["id"] + ".mp3") if raw else ""
        dst = os.path.join(OUT, ln["id"] + ".ogg")
        if not src or not os.path.exists(src):
            if os.path.exists(dst):                                                      # no take here, but the file was made by an earlier run: it stays (nothing is ever deleted) and stays registered
                report[ln["id"]] = {"seconds": round(seconds(dst), 2), "bytes": os.path.getsize(dst), "text": ln["text"]}
            else:
                missing.append(ln["id"])
            continue
        convert(src, dst)
        sec = seconds(dst)
        if sec < MIN_SECONDS or sec > MAX_SECONDS:
            os.remove(dst)
            rejected.append("%s (%.2f s)" % (ln["id"], sec))
            continue
        report[ln["id"]] = {"seconds": round(sec, 2), "bytes": os.path.getsize(dst), "text": ln["text"]}
    done = [ln["id"] for ln in lines if ln["id"] in report]
    if "--no-json" not in sys.argv:
        text = open(SOUNDS_JSON, encoding="utf-8").read()
        new = [sounds_entry(i) for i in done if '"voice.%s"' % i not in text]
        if new:
            at = text.find('\n  "undine.')                                            # before the sound sets' own entries, which their generators rewrite from there to the end
            if at >= 0:
                text = text[:at] + "\n" + ",\n".join(new) + "," + text[at:]
            else:
                body = text.rstrip()
                assert body.endswith("}")
                text = body[:-1].rstrip().rstrip(",") + ",\n" + ",\n".join(new) + "\n}\n"
            json.loads(text)
            with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
        lang = json.load(open(LANG, encoding="utf-8")) if os.path.exists(LANG) else {}
        lang = {k: v for k, v in lang.items() if not k.startswith("subtitles.nusmp.voice.shishou_")}
        for i in done:
            lang["subtitles.nusmp.voice." + i] = "Shishou: “%s”" % report[i]["text"]
        with open(LANG, "w", encoding="utf-8", newline="\n") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False)
            f.write("\n")
        java = open(JAVA, encoding="utf-8").read()
        a, b = java.index("    // BEGIN REGISTERED"), java.index("    // END REGISTERED")
        block = "    // BEGIN REGISTERED (rewritten by tools/audio/shishou_voice_convert.py)\n    private static final String[] REGISTERED = {" + ", ".join('"%s"' % i for i in done) + "};\n"
        java = java[:a] + block + java[b:]
        with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
            f.write(java)
    total = sum(v["bytes"] for v in report.values())
    json.dump({"converted": report, "missing": missing, "rejected": rejected, "bytes": total}, open(REPORT, "w", encoding="utf-8"), indent=1)
    print("converted %d of %d lines, %d bytes (%.1f KB); missing %d; rejected: %s" % (len(report), len(lines), total, total / 1024.0, len(missing), rejected))


if __name__ == "__main__":
    main()
