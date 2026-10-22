#!/usr/bin/env python3
"""Generates the takes of Shishou's spoken lines that are still missing (docs/items/shishou.md, "Voice"), with the ElevenLabs text-to-speech REST API, then converts and registers them.

usage:  ELEVENLABS_API_KEY=<your key> python3 -B tools/audio/shishou_voice_generate.py <raw_dir>
then (done by this script too, unless --no-convert):  python3 -B tools/audio/shishou_voice_convert.py <raw_dir>

It reads tools/audio/shishou_lines.json: the header names the voice ("voice": the ElevenLabs voice id, "voice_name"), each line has its prompt with the audio tag. While the header's voice is null (the voice is not
chosen yet) this script REFUSES to run and spends nothing. For every line without <raw_dir>/<id>.mp3 and without its converted file in the repository it asks for one take and saves it; a line that already has a take
or a file is never asked for again (--force asks for all), a 429 answer is waited out and retried (nothing is charged for a refused request). Only the standard library is used."""
import json, os, subprocess, sys, time, urllib.error, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))


def take(key, voice, model, line, path):
    body = json.dumps({"text": line["prompt"], "model_id": model}).encode("utf-8")
    req = urllib.request.Request("https://api.elevenlabs.io/v1/text-to-speech/%s?output_format=mp3_44100_128" % voice, data=body,
                                 headers={"xi-api-key": key, "Content-Type": "application/json", "Accept": "audio/mpeg"})
    wait = 20
    for attempt in range(12):
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            with open(path, "wb") as f:
                f.write(data)
            return True
        except urllib.error.HTTPError as e:
            if e.code == 429:
                print("  rate limited, waiting %d s" % wait)
                time.sleep(wait)
                wait = min(wait * 2, 300)
                continue
            print("  HTTP %d for %s: %s" % (e.code, line["id"], e.read()[:200]))
            return False
    return False


def main():
    doc = json.load(open(os.path.join(HERE, "shishou_lines.json"), encoding="utf-8"))
    if not doc.get("voice"):
        sys.exit("The voice of Shishou is not chosen yet (shishou_lines.json: \"voice\" is null). Design or choose it with the owner, write its id into the header, then run this again.")
    key = os.environ.get("ELEVENLABS_API_KEY")
    if not key or len(sys.argv) < 2:
        sys.exit(__doc__)
    raw = sys.argv[1]
    os.makedirs(raw, exist_ok=True)
    done_dir = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp", "sounds", "voice", "shishou")
    again = "--force" in sys.argv
    todo = [ln for ln in doc["lines"] if again or not (os.path.exists(os.path.join(raw, ln["id"] + ".mp3")) or os.path.exists(os.path.join(done_dir, ln["id"] + ".ogg")))]
    print("%d of %d lines still need a take" % (len(todo), len(doc["lines"])))
    failed = []
    for ln in todo:
        print(ln["id"], "-", ln["prompt"])
        if not take(key, doc["voice"], doc.get("model", "eleven_v4"), ln, os.path.join(raw, ln["id"] + ".mp3")):
            failed.append(ln["id"])
    print("done; failed: %s" % (failed or "none"))
    if "--no-convert" not in sys.argv:
        subprocess.run([sys.executable, "-B", os.path.join(HERE, "shishou_voice_convert.py"), raw], check=True)


if __name__ == "__main__":
    main()
