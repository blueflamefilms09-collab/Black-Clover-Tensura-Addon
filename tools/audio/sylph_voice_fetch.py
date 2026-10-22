#!/usr/bin/env python3
"""Fetches the spoken lines of Sylph (tools/audio/sylph_lines.json) that are still missing from the ElevenLabs text-to-speech API, one at a time, with a back-off on rate limits. The one-command way to
finish the voice set when the connector used in the build session was rate limited (docs/magic/sylph.md, "Voice lines"):

    ELEVENLABS_API_KEY=<your key> python3 -B tools/audio/sylph_voice_fetch.py <raw_dir> && python3 -B tools/audio/sylph_voice_convert.py <raw_dir>

It skips every line that already has <raw_dir>/<id>.mp3 or a converted file under assets/nusmp/sounds/voice/sylph/, so it can be run again and again. Options: --model <id> (default eleven_v4, the model
the other voice lines were made with; eleven_v3 also understands the audio tags), --only <group,group> (just those groups), --dry-run (print what would be asked, no network). The voice is the manifest's
(an ElevenLabs library voice: check its licence for commercial use, see the doc). Standard library only. Each line costs about 50 credits.
"""
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sounds", "voice", "sylph")


def request(voice, text, model, key):
    body = json.dumps({"text": text, "model_id": model}).encode("utf-8")
    req = urllib.request.Request("https://api.elevenlabs.io/v1/text-to-speech/%s?output_format=mp3_44100_128" % voice, data=body, method="POST",
                                 headers={"xi-api-key": key, "Content-Type": "application/json", "Accept": "audio/mpeg"})
    with urllib.request.urlopen(req, timeout=90) as r:
        return r.read()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("raw_dir")
    ap.add_argument("--model", default="eleven_v4")
    ap.add_argument("--only", default="")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    key = os.environ.get("ELEVENLABS_API_KEY", "")
    if not key and not a.dry_run:
        sys.exit("set ELEVENLABS_API_KEY")
    only = {g for g in a.only.split(",") if g}
    lines = json.load(open(os.path.join(HERE, "sylph_lines.json"), encoding="utf-8"))
    os.makedirs(a.raw_dir, exist_ok=True)
    todo = [ln for ln in lines if (not only or ln["group"] in only) and not os.path.exists(os.path.join(a.raw_dir, ln["id"] + ".mp3")) and not os.path.exists(os.path.join(OUT, ln["id"] + ".ogg"))]
    print("%d of %d lines to fetch" % (len(todo), len(lines)))
    for ln in todo:
        print("  %-18s %s" % (ln["id"], ln["prompt"]))
        if a.dry_run:
            continue
        wait = 20
        while True:
            try:
                data = request(ln["voice"], ln["prompt"], a.model, key)
                with open(os.path.join(a.raw_dir, ln["id"] + ".mp3"), "wb") as f:
                    f.write(data)
                break
            except urllib.error.HTTPError as e:
                if e.code in (429, 503):
                    print("    rate limited, waiting %d s" % wait)
                    time.sleep(wait)
                    wait = min(wait * 2, 600)
                    continue
                sys.exit("HTTP %d for %s: %s" % (e.code, ln["id"], e.read()[:200]))
        time.sleep(2)
    print("done; now run: python3 -B tools/audio/sylph_voice_convert.py %s" % a.raw_dir)


if __name__ == "__main__":
    main()
