#!/usr/bin/env python3
"""Generates the takes of the Undine's spoken lines that are still missing (docs/magic/undine.md, "Voice lines"), with the ElevenLabs text-to-speech REST API.

usage:  ELEVENLABS_API_KEY=<your key> python3 tools/audio/undine_voice_generate.py <raw_dir>
then:   python3 tools/audio/undine_voice_convert.py <raw_dir>        (converts, registers, rewrites UndineLines.java and sounds.json)

Reads tools/audio/undine_lines.json (id, group, library voice id, model, prompt with its audio tags). For every line without <raw_dir>/<id>.mp3 it asks for one take (about 35 to 50 credits each) and saves it;
a line that already has a take, or already has its converted file in the repository, is never asked for again (--force asks for all), so the script can be run again and again until nothing is missing (a 429 'rate limited' answer is waited out and retried: nothing is charged for a
refused request). The voice is the library voice named in the manifest ("Julie I"), never a clone of anyone; its licence terms are the creator's, shown on the voice's page in the ElevenLabs library.
Only the standard library is used."""
import json, os, sys, time, urllib.error, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))


def take(key, line, path):
    body = json.dumps({"text": line["prompt"], "model_id": line["model"]}).encode("utf-8")
    req = urllib.request.Request("https://api.elevenlabs.io/v1/text-to-speech/%s?output_format=mp3_44100_128" % line["voice"], data=body,
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
    key = os.environ.get("ELEVENLABS_API_KEY")
    if not key or len(sys.argv) < 2:
        sys.exit(__doc__)
    raw = sys.argv[1]
    os.makedirs(raw, exist_ok=True)
    lines = json.load(open(os.path.join(HERE, "undine_lines.json"), encoding="utf-8"))
    done_dir = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "nusmp", "sounds", "voice", "undine")
    again = "--force" in sys.argv
    todo = [ln for ln in lines if again or not (os.path.exists(os.path.join(raw, ln["id"] + ".mp3")) or os.path.exists(os.path.join(done_dir, ln["id"] + ".ogg")))]
    print("%d of %d lines still need a take" % (len(todo), len(lines)))
    failed = []
    for ln in todo:
        print(ln["id"], "-", ln["prompt"])
        if not take(key, ln, os.path.join(raw, ln["id"] + ".mp3")):
            failed.append(ln["id"])
    print("done; failed: %s" % (failed or "none"))


if __name__ == "__main__":
    main()
