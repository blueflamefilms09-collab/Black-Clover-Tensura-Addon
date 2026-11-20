#!/usr/bin/env python3
"""FREE, OFFLINE character-flavoured voice lines for bosses (no ElevenLabs credits needed). Docs: docs/audio/offline_voice.md.

Two steps, the same split as the ElevenLabs tools (generate takes -> convert + register):

  python3 -B tools/audio/offline_voice.py synth   <lines.json> <raw_dir> --profile goku [--only id,id] [--force]
  python3 -B tools/audio/offline_voice.py convert <lines.json> <raw_dir> --role boss --speaker Goku [--no-json] [--java <file>] [--root <other tree>]
  python3 -B tools/audio/offline_voice.py samples <out_dir> --profile goku        (3 neutral boss sentences, for listening; touches nothing else)

synth:   for every line of the lines JSON (shape of tools/audio/shishou_lines.json: {"voice","lines":[{id,group,prompt,text,[profile]}]}; the older list shape of voice_lines.json works too) that has
         no <raw_dir>/<id>.wav yet: text -> speech (Kokoro-82M, Apache-2.0, or a Piper voice, see PROFILES in offline_voice_profiles.json) -> numpy reverb -> ffmpeg effect chain (rubberband pitch, EQ,
         compressor, chorus, echo, radio band) -> <raw_dir>/<id>.wav. A leading [tag, tag] of the prompt nudges speed (urgent/sharp/angry: faster; calm/quiet/tense/weary: slower) because these models
         cannot act; the words are the line's "text" (or the prompt without its tags). A line may carry its own "profile".
convert: the same as tools/audio/shishou_voice_convert.py (same CHAIN: trim, loudnorm -16 LUFS, TP -1.5, mono, 32 kHz, Ogg Vorbis q3 -> src/main/resources/assets/anime_multiverse/sounds/voice/<role>/<id>.ogg;
         0.5 to 12 s) plus sounds.json 'voice.<id>' entries (appended before the sound sets' own entries, existing entries kept, subtitle key), tools/lang/<role>_voice.json subtitle fragment
         ('<speaker>: "<words>"') and a report tools/audio/<role>_voice_report.json. With --java <file> it also rewrites the REGISTERED array between the '// BEGIN REGISTERED' / '// END REGISTERED' markers.
         A line with no take and no file is simply not registered; an existing file stays registered (nothing is ever deleted).

Needs: ffmpeg + ffprobe (with libvorbis and rubberband), and in Python:  pip install kokoro-onnx soundfile piper-tts   (numpy comes with them).
Models are NOT in the repository (400 MB): by default they are looked up in $VOICE_MODELS (default ~/.cache/nusmp_voice): kokoro/kokoro-v1.0.onnx, kokoro/voices-v1.0.bin, piper/<voice>.onnx(+.json). `fetch` downloads them.
Performance: Kokoro on 4 CPU cores is about 1 to 3 s per line after a 10 s warm-up; the whole 41-line set is a couple of minutes. Nothing here runs in the game: only the finished .ogg files ship.
"""
import json, os, re, subprocess, sys, tempfile, wave

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
SOUNDS_JSON = os.path.join(ASSETS, "sounds.json")
PROFILES_JSON = os.path.join(HERE, "offline_voice_profiles.json")
MODELS = os.environ.get("VOICE_MODELS", os.path.join(os.path.expanduser("~"), ".cache", "nusmp_voice"))
KOKORO_URL = "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/"
PIPER_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/"
PIPER_PATHS = {"en_US-joe-medium": "en/en_US/joe/medium/", "en_US-john-medium": "en/en_US/john/medium/", "en_US-bryce-medium": "en/en_US/bryce/medium/"}   # only licences safe for a mod jar
# identical to tools/audio/shishou_voice_convert.py (the project's voice format)
CHAIN = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.04,areverse,"
         "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
         "loudnorm=I=-16:TP=-1.5:LRA=7")
MIN_SECONDS, MAX_SECONDS = 0.5, 12.0
FAST = re.compile(r"urgent|sharp|angry|shout|fierce|excited|battle|roar|furious|triumph", re.I)
SLOW = re.compile(r"calm|quiet|tense|weary|soft|whisper|sad|thought|slow|cold|dry", re.I)
SAMPLE_LINES = ["You dare stand before me? Show me everything you have.",
                "That was a good fight. But it is not over yet.",
                "Remember this day. It is the day you met your match."]


def profiles():
    return json.load(open(PROFILES_JSON, encoding="utf-8"))


def spoken(line):
    text = line.get("text") or re.sub(r"\[[^\]]*\]\s*", "", line.get("prompt", "")).strip()
    tags = " ".join(re.findall(r"\[([^\]]*)\]", line.get("prompt") or line.get("delivery", "")))   # the boss manifests carry the note as "delivery"
    return text, tags


def fetch():
    """Downloads the models into MODELS (Kokoro 330 MB, plus the Piper voices the profiles use)."""
    import urllib.request
    todo = [("kokoro/kokoro-v1.0.onnx", KOKORO_URL + "kokoro-v1.0.onnx"), ("kokoro/voices-v1.0.bin", KOKORO_URL + "voices-v1.0.bin")]
    for v, p in PIPER_PATHS.items():
        todo += [("piper/%s.onnx" % v, PIPER_URL + p + v + ".onnx"), ("piper/%s.onnx.json" % v, PIPER_URL + p + v + ".onnx.json")]
    for rel, url in todo:
        dst = os.path.join(MODELS, rel)
        if not os.path.exists(dst):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            print("downloading", url)
            urllib.request.urlretrieve(url, dst)


class Engines:
    """Loads each model once; a model is only loaded when a profile asks for it."""
    def __init__(self):
        self.kokoro, self.piper = None, {}

    def say(self, profile, text, speed_mul=1.0):
        import numpy as np
        speed = profile.get("speed", 1.0) * speed_mul
        if profile["engine"] == "kokoro":
            if self.kokoro is None:
                from kokoro_onnx import Kokoro
                self.kokoro = Kokoro(os.path.join(MODELS, "kokoro", "kokoro-v1.0.onnx"), os.path.join(MODELS, "kokoro", "voices-v1.0.bin"))
            v = profile["voice"]
            if isinstance(v, dict):                                                       # a blend of voices: the weighted mean of their style vectors
                total = float(sum(v.values()))
                v = sum(self.kokoro.get_voice_style(n) * (w / total) for n, w in v.items())
            audio, sr = self.kokoro.create(text, voice=v, speed=speed, lang=profile.get("lang", "en-us"))
            return np.asarray(audio, dtype=np.float32), sr
        from piper import PiperVoice, SynthesisConfig
        name = profile["voice"]
        if name not in self.piper:
            self.piper[name] = PiperVoice.load(os.path.join(MODELS, "piper", name + ".onnx"))
        v = self.piper[name]
        cfg = SynthesisConfig(length_scale=1.0 / max(speed, 0.3), noise_scale=profile.get("noise", 0.667), noise_w_scale=profile.get("noise_w", 0.8))
        chunks = list(v.synthesize(text, cfg))
        return np.concatenate([c.audio_float_array for c in chunks]).astype(np.float32), chunks[0].sample_rate


def reverb(x, sr, r):
    """A real convolution reverb: an exponentially decaying noise tail (low-passed by damping), mixed dry/wet. Deterministic."""
    import numpy as np
    if not r:
        return x
    n = int(sr * r.get("seconds", 0.6))
    rng = np.random.default_rng(7)
    ir = rng.standard_normal(n).astype(np.float32) * np.exp(-6.9 * np.arange(n) / n).astype(np.float32)   # -60 dB at the end
    d = r.get("damp", 0.5)                                                             # one-pole low-pass on the tail: the room swallows highs
    for i in range(1, n):
        ir[i] = ir[i - 1] * d + ir[i] * (1 - d)
    ir[0] += 0.0
    ir /= np.sqrt((ir ** 2).sum()) + 1e-9
    tail = np.zeros(len(x) + int(sr * (r.get("seconds", 0.6) + 0.1)), dtype=np.float32)
    size = 1 << int(np.ceil(np.log2(len(tail) + n)))
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(tail)].astype(np.float32)
    tail[:len(x)] += x
    w = r.get("wet", 0.2)
    tail = tail * (1 - w) + wet * w * 2.0
    return tail


def ffmpeg_chain(p):
    """The effect chain of a profile as an ffmpeg -af string (rubberband pitch, EQ, radio band, compressor, chorus, echo)."""
    f = []
    if p.get("pitch"):
        f.append("rubberband=pitch=%.5f:formant=%s:transients=smooth" % (2 ** (p["pitch"] / 12.0), p.get("formant", "preserved")))
    if p.get("highpass"):
        f.append("highpass=f=%d" % p["highpass"])
    if p.get("lowpass"):
        f.append("lowpass=f=%d" % p["lowpass"])
    for hz, q, gain in p.get("eq", []):
        f.append("equalizer=f=%d:t=q:w=%s:g=%s" % (hz, q, gain))
    if p.get("radio"):                                                                 # a comms-band voice (handheld radio / mind voice)
        f.append("highpass=f=350,lowpass=f=3200,acompressor=threshold=-24dB:ratio=6:attack=3:release=40:makeup=6")
    if p.get("comp", True):
        f.append("acompressor=threshold=%s:ratio=%s:attack=6:release=90:makeup=%s" % tuple(p.get("compressor", ["-20dB", 3, 3])))
    if p.get("chorus"):                                                                # a second, slightly detuned voice: aura / regal weight
        f.append("chorus=0.7:0.9:%s:0.25:0.25:2" % p["chorus"])
    if p.get("echo"):
        e = p["echo"]
        f.append("aecho=0.8:0.6:%s:%s" % ("|".join(str(int(d)) for d in e["delays"]), "|".join(str(c) for c in e["decays"])))
    f.append("alimiter=limit=0.9")
    return ",".join(f)


def write_wav(path, x, sr):
    import numpy as np
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes(pcm.tobytes())


def render(engines, profile, text, tags, dst):
    import numpy as np
    mul = 1.08 if FAST.search(tags) else (0.93 if SLOW.search(tags) else 1.0)
    audio, sr = engines.say(profile, text, mul)
    audio = np.concatenate([np.zeros(int(sr * 0.08), np.float32), audio, np.zeros(int(sr * 0.15), np.float32)])
    audio = reverb(audio, sr, profile.get("reverb"))
    with tempfile.TemporaryDirectory() as td:
        tmp = os.path.join(td, "dry.wav")
        write_wav(tmp, audio, sr)
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", tmp, "-af", ffmpeg_chain(profile), "-ac", "1", "-ar", "32000", dst], check=True)


def load_lines(path):
    doc = json.load(open(path, encoding="utf-8"))
    return doc["lines"] if isinstance(doc, dict) else doc


def synth(lines_path, raw, profile_name, only=None, force=False):
    ps = profiles()
    eng, made = Engines(), 0
    os.makedirs(raw, exist_ok=True)
    for ln in load_lines(lines_path):
        if only and ln["id"] not in only:
            continue
        dst = os.path.join(raw, ln["id"] + ".wav")
        if os.path.exists(dst) and not force:
            continue
        p = ps[ln.get("profile", profile_name)]
        text, tags = spoken(ln)
        render(eng, p, text, tags, dst)
        made += 1
    print("synthesized %d takes into %s" % (made, raw))


def seconds(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


def sounds_entry(i, role):
    return ('  "voice.%s": {\n    "subtitle": "subtitles.anime_multiverse.voice.%s",\n    "sounds": [\n      { "name": "anime_multiverse:voice/%s/%s", "stream": false }\n    ]\n  }' % (i, i, role, i))


def convert(lines_path, raw, role, speaker, no_json=False, java=None):
    out = os.path.join(ASSETS, "sounds", "voice", role)
    lang_path = os.path.join(ROOT, "tools", "lang", role + "_voice.json")
    report_path = os.path.join(HERE, role + "_voice_report.json")
    lines = load_lines(lines_path)
    report, missing, rejected = {}, [], []
    for ln in lines:
        i = ln["id"]
        src, dst = os.path.join(raw, i + ".wav"), os.path.join(out, i + ".ogg")
        text, _ = spoken(ln)
        if not os.path.exists(src):
            if os.path.exists(dst):                                                    # made by an earlier run: stays registered
                report[i] = {"seconds": round(seconds(dst), 2), "bytes": os.path.getsize(dst), "text": text}
            else:
                missing.append(i)
            continue
        os.makedirs(out, exist_ok=True)
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", src, "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", dst], check=True)
        sec = seconds(dst)
        if sec < MIN_SECONDS or sec > MAX_SECONDS:
            os.remove(dst); rejected.append("%s (%.2f s)" % (i, sec)); continue
        report[i] = {"seconds": round(sec, 2), "bytes": os.path.getsize(dst), "text": text}
    done = [ln["id"] for ln in lines if ln["id"] in report]
    if not no_json:
        text = open(SOUNDS_JSON, encoding="utf-8").read()
        new = [sounds_entry(i, role) for i in done if '"voice.%s"' % i not in text]
        if new:
            at = text.find('\n  "undine.')
            if at >= 0:
                text = text[:at] + "\n" + ",\n".join(new) + "," + text[at:]
            else:
                body = text.rstrip(); assert body.endswith("}")
                text = body[:-1].rstrip().rstrip(",") + ",\n" + ",\n".join(new) + "\n}\n"
            json.loads(text)
            with open(SOUNDS_JSON, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
        lang = json.load(open(lang_path, encoding="utf-8")) if os.path.exists(lang_path) else {}
        for i in done:
            lang["subtitles.anime_multiverse.voice." + i] = "%s: “%s”" % (speaker, report[i]["text"])
        with open(lang_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(lang, f, indent=2, ensure_ascii=False); f.write("\n")
        if java:
            src = open(java, encoding="utf-8").read()
            a, b = src.index("    // BEGIN REGISTERED"), src.index("    // END REGISTERED")
            block = "    // BEGIN REGISTERED (rewritten by tools/audio/offline_voice.py)\n    private static final String[] REGISTERED = {" + ", ".join('"%s"' % i for i in done) + "};\n"
            with open(java, "w", encoding="utf-8", newline="\n") as f:
                f.write(src[:a] + block + src[b:])
    total = sum(v["bytes"] for v in report.values())
    json.dump({"converted": report, "missing": missing, "rejected": rejected, "bytes": total}, open(report_path, "w", encoding="utf-8"), indent=1)
    print("converted %d of %d lines, %d bytes (%.1f KB); missing %d; rejected: %s" % (len(report), len(lines), total, total / 1024.0, len(missing), rejected))


def samples(out_dir, profile_name):
    """Three neutral boss sentences per profile as .wav (ffmpeg chain, pre-convert) and .ogg (exactly what the game would play)."""
    os.makedirs(out_dir, exist_ok=True)
    eng = Engines()
    p = profiles()[profile_name]
    for n, text in enumerate(SAMPLE_LINES, 1):
        base = os.path.join(out_dir, "%s_%d" % (profile_name, n))
        render(eng, p, text, "", base + ".wav")
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", base + ".wav", "-af", CHAIN, "-ac", "1", "-ar", "32000", "-c:a", "libvorbis", "-q:a", "3", base + ".ogg"], check=True)
        print(base + ".ogg")


def main(argv):
    if not argv or argv[0] not in ("synth", "convert", "samples", "fetch"):
        print(__doc__); return 2
    opt = lambda k, d=None: argv[argv.index(k) + 1] if k in argv else d
    cmd = argv[0]
    if opt("--root"):                                                                  # test runs: write assets / sounds.json / lang / report under another tree instead of the repository
        global ROOT, ASSETS, SOUNDS_JSON, HERE
        ROOT = os.path.abspath(opt("--root")); ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
        SOUNDS_JSON = os.path.join(ASSETS, "sounds.json"); HERE = os.path.join(ROOT, "tools", "audio")
    if cmd == "fetch":
        fetch()
    elif cmd == "synth":
        synth(argv[1], argv[2], opt("--profile"), set(opt("--only", "").split(",")) - {""} or None, "--force" in argv)
    elif cmd == "convert":
        convert(argv[1], argv[2], opt("--role", "boss"), opt("--speaker", "Boss"), "--no-json" in argv, opt("--java"))
    else:
        samples(argv[1], opt("--profile"))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
