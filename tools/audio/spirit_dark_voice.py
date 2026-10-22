#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""The spoken lines of the Spirit of Darkness (docs/magic/spirit_dark.md, section voice): the plan, the converter and the registration.

The spirit speaks in a smooth, mocking, aristocratic voice (ElevenLabs library voice "Oliver - Warm British Narrator", id lo1O1Dz1Tu4FWVEPaxuT, model eleven_v4, one take per line; see the doc for the
licence note). 47 short lines in 12 groups; the group is what the game asks for (spiritdark/DarkMoodRules: link, cast, step, hurt, feast, idle, low, kill, died, unlink, power, power_end).

    python3 -B tools/audio/spirit_dark_voice.py manifest          writes tools/audio/spirit_dark_lines.json (the plan: id, group, text, prompt, voice)
    python3 -B tools/audio/spirit_dark_voice.py missing           lists the ids that have no recording yet (assets/nusmp/sounds/spirit/dark/voice/<id>.ogg)
    python3 -B tools/audio/spirit_dark_voice.py convert <raw_dir> converts <raw_dir>/<id>.mp3 (or .wav) of every planned line that has one: mono 32 kHz, silence trimmed, loudnorm about -16 LUFS,
                                                                  true peak <= -1.5 dB, Ogg Vorbis q3 -> assets/nusmp/sounds/spirit/dark/voice/<id>.ogg; then runs `sync`
    python3 -B tools/audio/spirit_dark_voice.py sync              rewrites the two files the game reads from the recordings that exist: assets/nusmp/spirit_dark_voice.json (id -> text, only
                                                                  lines whose ogg exists) and the spirit.dark.voice.<id> entries of sounds.json; also tools/audio/spirit_dark_voice_report.json
    python3 -B tools/audio/spirit_dark_voice.py check             verifies files, entries and the 3 MB budget

A line without a recording is NEVER registered or played: the game reads spirit_dark_voice.json, which only lists lines whose file exists. Needs ffmpeg and ffprobe for convert.
"""
import json
import os
import subprocess
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ASSETS, "sounds", "spirit", "dark", "voice")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
DATA = os.path.join(ASSETS, "spirit_dark_voice.json")
MANIFEST = os.path.join(HERE, "spirit_dark_lines.json")
REPORT = os.path.join(HERE, "spirit_dark_voice_report.json")
VOICE_ID = "lo1O1Dz1Tu4FWVEPaxuT"
VOICE_NAME = "Oliver - Warm British Narrator"
MODEL = "eleven_v4"
BUDGET = 3 * 1024 * 1024
RANGE = 32

# group: [(text shown and spoken, ElevenLabs v4 prompt)]; the prompt is the text with audio tags in front
LINES = {
    "link": [
        ("Ah. At last, a master worth the bother. Do try to be interesting.", "[smooth] [smug] Ah. At last, a master worth the bother. [short pause] Do try to be interesting."),
        ("Splendid. Another mortal who thinks they hold the leash.", "[smooth] [amused] Splendid. Another mortal who thinks they hold the leash."),
        ("Good evening. I shall be your shadow, and you shall be my entertainment.", "[smooth] [softly] Good evening. I shall be your shadow, and you shall be my entertainment."),
        ("Oh, how touching. You called, and I came. Don't make a habit of it.", "[smooth] [sarcastic] Oh, how touching. You called, and I came. Don't make a habit of it."),
    ],
    "unlink": [
        ("Do ring when you're bored again.", "[smooth] [mischievously] Do ring when you're bored again."),
        ("I shall return to the dark. It is better company.", "[smooth] [sighs] I shall return to the dark. It is better company."),
        ("Farewell, master. Try not to die before I come back.", "[smooth] [softly] Farewell, master. Try not to die before I come back."),
    ],
    "cast": [
        ("Oh, how theatrical.", "[smooth] [amused] Oh, how theatrical."),
        ("Darker, my dear. The shadows demand it.", "[smooth] [softly] Darker, my dear. The shadows demand it."),
        ("A pretty little spell. Almost frightening.", "[smooth] [sarcastic] A pretty little spell. Almost frightening."),
        ("Yes, yes, flourish. I do love a performance.", "[smooth] [chuckles] Yes, yes, flourish. I do love a performance."),
        ("Splendid. Burn it all, and do it elegantly.", "[smooth] [mischievously] Splendid. Burn it all, and do it elegantly."),
    ],
    "step": [
        ("Do keep up.", "[smooth] [smug] Do keep up."),
        ("Through the dark, and out again. Charming.", "[smooth] [softly] Through the dark, and out again. Charming."),
        ("Now you see me. Now you see the back of me.", "[smooth] [amused] Now you see me. Now you see the back of me."),
        ("Shadows are such lovely doors, aren't they?", "[smooth] [curious] Shadows are such lovely doors, aren't they?"),
    ],
    "hurt": [
        ("Oh dear. That looked rather painful.", "[smooth] [sarcastic] Oh dear. That looked rather painful."),
        ("You're bleeding, master. How unseemly.", "[smooth] [tutting] You're bleeding, master. How unseemly."),
        ("Careful. I have invested so much in you.", "[smooth] [dryly] Careful. I have invested so much in you."),
        ("Ha! They bite. How rude.", "[smooth] [laughs softly] Ha! They bite. How rude."),
    ],
    "feast": [
        ("Mm. Delicious. Do share more.", "[smooth] [softly] Mm. Delicious. Do share more."),
        ("Drink deep. They won't be needing it.", "[smooth] [sinister] Drink deep. They won't be needing it."),
        ("A little life, borrowed. Do give it back someday.", "[smooth] [mischievously] A little life, borrowed. Do give it back someday."),
        ("Ah, the taste of fear. A fine vintage.", "[smooth] [savouring] Ah, the taste of fear. A fine vintage."),
    ],
    "idle": [
        ("Is nothing happening? How dreadfully dull.", "[smooth] [bored] Is nothing happening? [sighs] How dreadfully dull."),
        ("I could rule a kingdom, you know. Instead I watch you walk.", "[smooth] [wry] I could rule a kingdom, you know. Instead, I watch you walk."),
        ("Shall we ruin someone's evening?", "[smooth] [mischievously] Shall we ruin someone's evening?"),
        ("Don't mind me. I'm merely admiring the dark.", "[smooth] [softly] Don't mind me. I'm merely admiring the dark."),
        ("Do you ever stop? Mortals are exhausting.", "[smooth] [sighs] Do you ever stop? Mortals are exhausting."),
        ("I have seen empires fall. This is nearly as thrilling.", "[smooth] [dryly] I have seen empires fall. This is nearly as thrilling."),
    ],
    "low": [
        ("Master. You are dying. How inconvenient for me.", "[smooth] [sharp] Master. You are dying. How inconvenient for me."),
        ("Mend yourself, or I shall have to find a new hobby.", "[smooth] [sarcastic] Mend yourself, or I shall have to find a new hobby."),
        ("Now, now. Don't you dare fall over.", "[smooth] [firmly] Now, now. Don't you dare fall over."),
        ("Oh, this is getting interesting. Bleed less, fight more.", "[smooth] [amused] Oh, this is getting interesting. Bleed less, fight more."),
    ],
    "kill": [
        ("Magnificent. Another one for the dark.", "[smooth] [pleased] Magnificent. Another one for the dark."),
        ("And down they go. Bravo.", "[smooth] [amused] And down they go. Bravo."),
        ("Ha ha ha! Oh, that was lovely.", "[smooth] [laughs] Ha ha ha! Oh, that was lovely."),
        ("Such a waste. Of their time, not mine.", "[smooth] [dryly] Such a waste. Of their time, not mine."),
        ("Do take a bow. I'll hold your applause.", "[smooth] [chuckles] Do take a bow. I'll hold your applause."),
    ],
    "died": [
        ("Oh. Well. That was brief.", "[smooth] [surprised] Oh. Well. That was brief."),
        ("How careless. And we were just beginning.", "[smooth] [sighs] How careless. And we were just beginning."),
        ("Pity. I did so enjoy you.", "[smooth] [softly] Pity. I did so enjoy you."),
    ],
    "power": [
        ("Enough pleasantries. Let me show them what I truly am.", "[deep voice] [smooth] Enough pleasantries. Let me show them what I truly am."),
        ("Behold. The dark, with manners.", "[deep voice] [smooth] [sinister] Behold. The dark, with manners."),
        ("Kneel, little ones. Your host has arrived.", "[deep voice] [smooth] [mischievously] Kneel, little ones. Your host has arrived."),
    ],
    "power_end": [
        ("That will do. I have a reputation to keep.", "[smooth] [sighs] That will do. I have a reputation to keep."),
        ("Back to my lounge. Do try not to need me.", "[smooth] [wry] Back to my lounge. Do try not to need me."),
    ],
}


def ids():
    out = []
    for g, rows in LINES.items():
        for i, (text, prompt) in enumerate(rows):
            out.append((g + "_%d" % (i + 1), g, text, prompt))
    return out


def ogg_path(i):
    return os.path.join(OUT, i + ".ogg")


def write_manifest():
    doc = [{"id": i, "group": g, "voice": VOICE_ID, "model": MODEL, "text": t, "prompt": p} for i, g, t, p in ids()]
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"voice": {"id": VOICE_ID, "name": VOICE_NAME, "source": "ElevenLabs voice library (library voice; licence to be checked for commercial use before a public release)"}, "model": MODEL, "lines": doc}, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print("wrote", MANIFEST, len(doc), "lines")


def existing():
    return [(i, g, t) for i, g, t, p in ids() if os.path.exists(ogg_path(i))]


def seconds(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


CHAIN = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.04,areverse,"
         "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
         "loudnorm=I=-16:TP=-1.5:LRA=7")


def convert(raw):
    os.makedirs(OUT, exist_ok=True)
    done = 0
    for i, g, t, p in ids():
        src = None
        for ext in (".mp3", ".wav"):
            if os.path.exists(os.path.join(raw, i + ext)):
                src = os.path.join(raw, i + ext)
        if not src:
            continue
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", src, "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", ogg_path(i)], check=True)
        done += 1
    print("converted", done, "lines")
    sync()


def sync():
    have = existing()
    report = {i: {"seconds": round(seconds(ogg_path(i)), 2), "bytes": os.path.getsize(ogg_path(i))} for i, g, t in have}
    with open(REPORT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(report, f, indent=1, sort_keys=True)
        f.write("\n")
    with open(DATA, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"voice": VOICE_NAME, "lines": {i: t for i, g, t in have}}, f, indent=1, ensure_ascii=False)
        f.write("\n")
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        text = f.read()
    lines = text.split("\n")
    kept = [ln for ln in lines if not ln.startswith('  "spirit.dark.voice.')]
    at = next(n for n, ln in enumerate(kept) if ln.startswith('  "spirit.dark.powered": {'))
    end = at
    while not kept[end].startswith("  },"):
        end += 1
    block = ['  "spirit.dark.voice.%s": { "sounds": [ { "name": "nusmp:spirit/dark/voice/%s", "stream": false, "attenuation_distance": %d } ] },' % (i, i, RANGE) for i, g, t in have]
    out = "\n".join(kept[:end + 1] + block + kept[end + 1:])
    json.loads(out)
    with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
        f.write(out)
    total = sum(v["bytes"] for v in report.values())
    print("sync: %d of %d planned lines have a recording; %d bytes (%.2f MB)" % (len(have), len(ids()), total, total / 1048576.0))


def missing():
    m = [i for i, g, t, p in ids() if not os.path.exists(ogg_path(i))]
    print("missing %d of %d: %s" % (len(m), len(ids()), " ".join(m)))
    return m


def check():
    bad = 0
    doc = json.load(open(SOUNDS_JSON, encoding="utf-8"))
    data = json.load(open(DATA, encoding="utf-8")) if os.path.exists(DATA) else {"lines": {}}
    total = 0
    for i, g, t in existing():
        total += os.path.getsize(ogg_path(i))
        if ("spirit.dark.voice." + i) not in doc:
            print("no sounds.json entry for", i)
            bad += 1
        if data["lines"].get(i) != t:
            print("data file differs for", i)
            bad += 1
    for i in data["lines"]:
        if not os.path.exists(ogg_path(i)):
            print("data file names a line without a file:", i)
            bad += 1
    for k in doc:
        if k.startswith("spirit.dark.voice.") and not os.path.exists(ogg_path(k[len("spirit.dark.voice."):])):
            print("sounds.json entry without a file:", k)
            bad += 1
    if total > BUDGET:
        print("over the 3 MB budget:", total)
        bad += 1
    print("problems:", bad)
    return bad


def main():
    cmd = sys.argv[1] if len(sys.argv) > 1 else "missing"
    if cmd == "manifest":
        write_manifest()
    elif cmd == "missing":
        missing()
    elif cmd == "convert":
        convert(sys.argv[2])
    elif cmd == "sync":
        sync()
    elif cmd == "check":
        sys.exit(1 if check() else 0)
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
