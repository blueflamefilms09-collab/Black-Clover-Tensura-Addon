#!/usr/bin/env python3
"""Turns the downloaded ElevenLabs takes of the Undine's spoken lines into the mod's voice files and registers exactly those (docs/magic/undine.md, "Voice lines"; docs/audio/spoken_voices.md).

usage: python3 tools/audio/undine_voice_convert.py <raw_dir> [--no-json]

For every line of tools/audio/undine_lines.json that has <raw_dir>/<id>.mp3:
  * ffmpeg: mono, 32 kHz, trim leading / trailing silence, loudnorm (about -16 LUFS, true peak <= -1.5 dB), Ogg Vorbis q3
    -> src/main/resources/assets/nusmp/sounds/voice/undine/<id>.ogg   (the project's voice format, same chain as tools/audio/voice_convert.py)
  * ffprobe: the duration; written with the size to tools/audio/undine_voice_report.json (converted ids, missing ids, bytes)
Then it keeps four things in step, so a line is registered if and only if its file exists:
  * assets/nusmp/sounds.json gets 'voice.<id>' (appended as text, the rest of the file is not reformatted; an existing entry is kept) with a subtitle key
  * tools/lang/undine.json gets 'subtitles.nusmp.voice.<id>' = the spoken text (the owner merges the fragment into en_us.json)
  * src/main/java/com/newuniverse/nusmp/undine/UndineLines.java: the REGISTERED array between its two marker comments is rewritten
A line with no take and no file is simply not registered (its moment falls back to the sung hum cue); a file made by an earlier run stays registered even when its take is not in <raw_dir>; the raw takes stay outside the repository. Needs ffmpeg and ffprobe on the path."""
import json, os, re, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ASSETS, "sounds", "voice", "undine")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
LANG = os.path.join(ROOT, "tools", "lang", "undine.json")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "undine", "UndineLines.java")
REPORT = os.path.join(HERE, "undine_voice_report.json")
CHAIN = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.04,areverse,"
         "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
         "loudnorm=I=-16:TP=-1.5:LRA=7")
MIN_SECONDS, MAX_SECONDS = 0.5, 12.0


def spoken_text(prompt):
    """The words of a prompt without its leading audio tags: '[soft, serene] I am here.' -> 'I am here.'"""
    return re.sub(r"\[[^\]]*\]\s*", "", prompt).strip()


def convert(src, dst):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", src, "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", dst], check=True)


def seconds(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


def sounds_entry(i):
    return ('  "voice.%s": {\n    "subtitle": "subtitles.nusmp.voice.%s",\n    "sounds": [\n      { "name": "nusmp:voice/undine/%s", "stream": false }\n    ]\n  }' % (i, i, i))


def main():
    raw = sys.argv[1]
    lines = json.load(open(os.path.join(HERE, "undine_lines.json"), encoding="utf-8"))
    report, missing, rejected = {}, [], []
    for ln in lines:
        src = os.path.join(raw, ln["id"] + ".mp3")
        dst = os.path.join(OUT, ln["id"] + ".ogg")
        if not os.path.exists(src):
            if os.path.exists(dst):                                                      # no take here, but the file was made by an earlier run: it stays (nothing is ever deleted) and stays registered
                report[ln["id"]] = {"seconds": round(seconds(dst), 2), "bytes": os.path.getsize(dst), "text": spoken_text(ln["prompt"])}
            else:
                missing.append(ln["id"])
            continue
        convert(src, dst)
        sec = seconds(dst)
        if sec < MIN_SECONDS or sec > MAX_SECONDS:
            os.remove(dst)
            rejected.append("%s (%.2f s)" % (ln["id"], sec))
            continue
        report[ln["id"]] = {"seconds": round(sec, 2), "bytes": os.path.getsize(dst), "text": spoken_text(ln["prompt"])}
    done = [ln["id"] for ln in lines if ln["id"] in report]
    if "--no-json" not in sys.argv:
        text = open(SOUNDS_JSON, encoding="utf-8").read()
        new = []
        for i in done:
            if '"voice.%s"' % i not in text:
                new.append(sounds_entry(i))
        if new:
            at = text.find('\n  "undine.')                                            # before the sound set's own entries, which tools/gen_undine_sounds.py rewrites from there to the end
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
        lang = {k: v for k, v in lang.items() if not k.startswith("subtitles.nusmp.voice.undine_")}
        for i in done:
            lang["subtitles.nusmp.voice." + i] = "Undine: “%s”" % report[i]["text"]
        with open(LANG, "w", encoding="utf-8", newline="\n") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False)
            f.write("\n")
        java = open(JAVA, encoding="utf-8").read()
        a, b = java.index("    // BEGIN REGISTERED"), java.index("    // END REGISTERED")
        block = "    // BEGIN REGISTERED (rewritten by tools/audio/undine_voice_convert.py)\n    private static final String[] REGISTERED = {" + ", ".join('"%s"' % i for i in done) + "};\n"
        java = java[:a] + block + java[b:]
        with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
            f.write(java)
    total = sum(v["bytes"] for v in report.values())
    json.dump({"converted": report, "missing": missing, "rejected": rejected, "bytes": total}, open(REPORT, "w", encoding="utf-8"), indent=1)
    print("converted %d of %d lines, %d bytes (%.1f KB); missing %d: %s; rejected: %s" % (len(report), len(lines), total, total / 1024.0, len(missing), missing, rejected))


if __name__ == "__main__":
    main()
