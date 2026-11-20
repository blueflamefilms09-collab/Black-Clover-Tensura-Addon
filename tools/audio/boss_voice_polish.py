#!/usr/bin/env python3
"""Second pass of the Goku and Sinbad boss voices (docs/boss_redesign_goku_sinbad.md, "Kokoro voice pass"). OFFLINE only: Kokoro-82M (Apache-2.0) + numpy + ffmpeg. Nothing here calls a paid service.

  python3 -B tools/audio/boss_voice_polish.py synth   --boss goku|sinbad [--only id,id] [--raw DIR] [--profile NAME]   text -> polished take (<raw>/<id>.wav, 32 kHz mono float)
  python3 -B tools/audio/boss_voice_polish.py convert --boss goku|sinbad [--only id,id] [--raw DIR] [--out DIR]       take -> two-pass loudness-normalised Ogg Vorbis (default: the game's sounds/voice/<boss>/)
  python3 -B tools/audio/boss_voice_polish.py probe   [--boss ...]                                                    prints the phonemes each line is spoken with (needs Kokoro, no audio)
  python3 -B tools/audio/boss_voice_polish.py report                                                                  measures classic/ and the current files, writes boss_voice_polish_report.json and the two *_voice_report.json
  python3 -B tools/audio/boss_voice_polish.py selftest                                                               the pure parts (needs nothing)

It is the older tools/audio/offline_voice.py (kept, unchanged: its profiles 'goku' and 'sinbad' still work and made the files now kept under sounds/voice/<boss>/classic/) with five fixes:
  1. PRONUNCIATION. Kokoro's own text-to-phoneme step reads 'Kamehameha' as 'kay-m-hay-m-ha', 'Saiyan' as 'say-yan', 'Kaio-ken' as 'kye-oh-ken' and cannot say 'Nngh' or 'HAAAAAA'. PRON below holds an IPA respelling
     per such word (checked with `probe`); only lines that contain one go through the phoneme path, every other line is spoken exactly as before.
  2. VOICE AND SPEED. New profiles 'goku2' and 'sinbad2' in offline_voice_profiles.json (measured: the median pitch and the spectral balance of the blend, see the doc), plus a small per-group speed table
     (a charge-up is slower, a shout is faster) on top of the tag-based speed of offline_voice.
  3. ORDER OF THE CHAIN. Pitch, high-pass, EQ, de-esser, compressor, chorus FIRST, then the room: a numpy reverb with a pre-delay is added AFTER the pitch shift (the old chain pitch-shifted its own reverb tail), and the
     tail is faded instead of being cut.
  4. PER-LINE CLEANUP. DC removed, the start and the end of the speech found from a 10 ms envelope (-45 dBFS) so Kokoro's breath / babble tails are cut, 20 ms lead, a 5 ms fade-in (no click), a 60 ms fade-out.
  5. LOUDNESS. A measured gain to -16 LUFS (EBU R128), applied, limited at -2.3 dBFS and measured again, instead of the single loudnorm pass that over- or undershoots on 1 to 3 s clips; Ogg Vorbis q4 at 32 kHz
     mono (the old files were q3).

The ids, the texts and the file names of the 117 lines do not change, so sounds.json, the subtitles and BossLines.REGISTERED stay valid; `python3 tools/audio/boss_lines_gen.py --register` is run after it as before.
"""
import json
import os
import re
import subprocess
import sys
import tempfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True
import offline_voice as OV  # noqa: E402  (Engines, profiles, FAST/SLOW tags, write_wav, ffmpeg_chain pieces)

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
VOICE_OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "sounds", "voice")
SR = 32000
TARGET_LUFS = -16.0

# IPA respelling of the words Kokoro's grapheme-to-phoneme step gets wrong (the stress marks are Kokoro's: ˈ main, ˌ secondary, ː long). Lower case keys; a word that is not here is left to Kokoro.
PRON = {
    "kamehameha": "kˌɑːmeɪhˈɑːmeɪhˌɑː",
    "kaio-ken": "kˈeɪoʊkˌɛn", "taiyoken": "tˈaɪjoʊkˌɛn", "zanzoken": "zˈænzoʊkˌɛn",
    "saiyan": "sˈaɪjən",
    "bararaq": "bˈɑːɹɑːɹˌæk", "saiqa": "sˈaɪkɑː", "inqerad": "ɪnkˈɛɹɑːd", "garufor": "ɡˈɑːɹuːfˌɔːɹ", "zairu": "zˈaɪɹuː", "kirestal": "kˈɪɹɛstəl",
    "foraz": "fˈɔːɹæz", "zora": "zˈɔːɹɑː", "istella": "ɪstˈɛlɑː",
    "valefor": "vˈælɪfˌɔːɹ", "focalor": "fˈoʊkɑːlˌɔːɹ", "magoi": "mˈɑːɡɔɪ",
    "nngh": "ˈɜːŋ", "hya": "hˈiːjɑː", "haa": "hˈɑːɑː", "hah": "hˈɑː", "heh": "hˈɛ", "ugh": "ˈʌɡ",
}
# whole lines whose words are only syllables ('me' is also an English word, so it cannot be a word entry)
LINE_IPA = {"Ka... me... ha... me... HAAAAA!": "kˈɑː ... mˈeɪ ... hˈɑː ... mˈeɪ ... hˈɑːɑːɑːɑː !"}
SHOUT = re.compile(r"^(h?a{3,}|a{3,}rgh|h?a{3,}h?)$", re.I)           # HAAAAAA, AAAAAAARGH: a held vowel
SHOUT_IPA = {"h": "hˈɑːɑːɑːɑː", "a": "ˈɑːɑːɑːɑː", "argh": "ˈɑːɑːɑːɹɡ"}
WORD = re.compile(r"[A-Za-z][A-Za-z'-]*")

# group -> speed factor on top of the profile and the delivery tags: charge-ups are slower, quick calls faster
GROUP_SPEED = {"kamehameha": 0.9, "spirit_bomb": 0.92, "stagnation": 0.85, "inqerad": 0.95, "form_ui_sign": 0.95, "form_mui": 0.93, "form_god": 0.9,
               "combo": 1.06, "kaioken_burst": 1.05, "transmit": 1.05, "bararaq": 1.04, "garufor": 1.02, "sword": 0.97, "scream": 1.0}


def shout_ipa(word):
    w = word.lower()
    if w.endswith("rgh"):
        return SHOUT_IPA["argh"]
    return SHOUT_IPA["h" if w.startswith("h") else "a"]


def british(ipa):
    """The respelling for a British voice: no r after a vowel unless a vowel follows, the British diphthong of 'go'."""
    return re.sub(r"ɹ(?![ˈˌaeiouɑɐɒæɛəɜɪɔʊʌ])", "", ipa).replace("oʊ", "əʊ")


def respell(w, lang="en-us"):
    """The IPA of one word that needs a respelling, else None."""
    lw = w.lower()
    parts = lw.split("-")
    if lw in PRON:
        ipa = PRON[lw]
    elif SHOUT.match(w):
        ipa = shout_ipa(w)
    elif len(parts) > 1 and all(q in PRON for q in parts):                         # 'Inqerad-Saiqa'
        ipa = " ".join(PRON[q] for q in parts)
    else:
        return None
    return british(ipa) if lang == "en-gb" else ipa


def phonemes(tokenizer, text, lang):
    """The phoneme string of a line with the respellings applied, or None when no word needs one (then the line is spoken from its text, as before)."""
    if text in LINE_IPA:
        return LINE_IPA[text]
    pieces, start, hit = [], 0, False

    def plain(s):
        s = s.strip()
        if s:
            pieces.append(tokenizer.phonemize(s, lang) if re.search(r"[A-Za-z0-9]", s) else s)   # punctuation alone is kept: it paces the line

    for m in WORD.finditer(text):
        ipa = respell(m.group(0), lang)
        if ipa is not None:
            plain(text[start:m.start()])
            pieces.append(ipa)
            start, hit = m.end(), True
    plain(text[start:])
    return " ".join(pieces) if hit else None


class Voice(OV.Engines):
    """offline_voice.Engines plus the phoneme path."""

    def say2(self, profile, text, speed):
        import numpy as np_
        if self.kokoro is None:
            from kokoro_onnx import Kokoro
            self.kokoro = Kokoro(os.path.join(OV.MODELS, "kokoro", "kokoro-v1.0.onnx"), os.path.join(OV.MODELS, "kokoro", "voices-v1.0.bin"))
        v = profile["voice"]
        if isinstance(v, dict):
            total = float(sum(v.values()))
            v = sum(self.kokoro.get_voice_style(n) * (w / total) for n, w in v.items())
        lang = profile.get("lang", "en-us")
        ph = phonemes(self.kokoro.tokenizer, text, lang)
        if ph is not None:
            audio, sr = self.kokoro.create(ph, voice=v, speed=speed, lang=lang, is_phonemes=True)
        else:
            audio, sr = self.kokoro.create(text, voice=v, speed=speed, lang=lang)
        return np_.asarray(audio, dtype=np_.float32), sr


# ------------------------------------------------------------------------------------------------ the chain
def ffmpeg_pre(p):
    """Pitch, high-pass, EQ, de-esser, compressor, chorus (everything except the room), from the profile."""
    f = []
    if p.get("pitch"):
        f.append("rubberband=pitch=%.5f:formant=%s:transients=smooth" % (2 ** (p["pitch"] / 12.0), p.get("formant", "preserved")))
    f.append("highpass=f=%d" % p.get("highpass", 70))
    if p.get("lowpass"):
        f.append("lowpass=f=%d" % p["lowpass"])
    for hz, q, gain in p.get("eq", []):
        f.append("equalizer=f=%d:t=q:w=%s:g=%s" % (hz, q, gain))
    if p.get("deess", 0) > 0:
        f.append("deesser=i=%.2f:m=0.5:f=0.5" % p["deess"])
    f.append("acompressor=threshold=%s:ratio=%s:attack=8:release=110:makeup=%s" % tuple(p.get("compressor", ["-20dB", 2.5, 2])))
    if p.get("chorus"):
        f.append("chorus=0.7:0.9:%s:0.2:0.2:2" % p["chorus"])
    return ",".join(f)


def room(x, sr, r):
    """Pre-delay + decaying noise tail, mixed dry/wet; the tail is faded at its end. Deterministic."""
    if not r:
        return np.concatenate([x, np.zeros(int(sr * 0.1), np.float32)])
    secs = r.get("seconds", 0.5)
    n = int(sr * secs)
    rng = np.random.default_rng(7)
    ir = rng.standard_normal(n).astype(np.float32) * np.exp(-6.9 * np.arange(n) / n).astype(np.float32)
    d = r.get("damp", 0.5)
    for i in range(1, n):                                                          # one-pole low-pass: a room swallows highs
        ir[i] = ir[i - 1] * d + ir[i] * (1 - d)
    pre = int(sr * r.get("predelay_ms", 0) / 1000.0)
    ir = np.concatenate([np.zeros(pre, np.float32), ir])
    ir /= np.sqrt((ir ** 2).sum()) + 1e-9
    out = np.zeros(len(x) + len(ir), dtype=np.float32)
    size = 1 << int(np.ceil(np.log2(len(out) + len(ir))))
    wet = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:len(out)].astype(np.float32)
    out[:len(x)] += x
    w = r.get("wet", 0.1)
    return out * (1 - w) + wet * w * 2.0


def speech_span(x, sr, floor_db=-45.0, win=0.01):
    """First and last sample of the speech by a 10 ms RMS envelope (breath and babble tails below the floor are not speech)."""
    k = max(1, int(sr * win))
    pad = (-len(x)) % k
    e = np.sqrt((np.concatenate([x, np.zeros(pad, np.float32)]).reshape(-1, k) ** 2).mean(axis=1))
    live = np.where(e > 10 ** (floor_db / 20))[0]
    if not len(live):
        return 0, len(x)
    return int(live[0] * k), min(len(x), int((live[-1] + 1) * k))


def ff(args, data=None):
    return subprocess.run(["ffmpeg", "-y", "-v", "error"] + args, input=data, capture_output=True, check=True)


def read_f32(path):
    return np.frombuffer(ff(["-i", path, "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"]).stdout, dtype=np.float32).copy()


def write_f32(path, x):
    ff(["-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", "-", "-c:a", "pcm_f32le", path], x.astype(np.float32).tobytes())


def render(voice, profile, text, tags, group, dst):
    mul = 1.08 if OV.FAST.search(tags) else (0.93 if OV.SLOW.search(tags) else 1.0)
    mul *= GROUP_SPEED.get(group, 1.0)
    audio, sr = voice.say2(profile, text, profile.get("speed", 1.0) * mul)
    a0, a1 = speech_span(audio, sr)                                                # cut Kokoro's lead / tail babble before the effects
    audio = audio[a0:a1]
    audio = audio - audio.mean()
    with tempfile.TemporaryDirectory() as td:
        raw = os.path.join(td, "dry.wav")
        OV.write_wav(raw, np.concatenate([np.zeros(int(sr * 0.05), np.float32), audio, np.zeros(int(sr * 0.05), np.float32)]), sr)
        wet = os.path.join(td, "pre.wav")
        ff(["-i", raw, "-af", ffmpeg_pre(profile), "-ac", "1", "-ar", str(SR), "-c:a", "pcm_f32le", wet])
        x = read_f32(wet)
    b0, b1 = speech_span(x, SR, -50.0)
    x = np.concatenate([np.zeros(int(SR * 0.02), np.float32), x[b0:b1]])         # 20 ms lead
    x = room(x, SR, profile.get("reverb"))
    c0, c1 = speech_span(x, SR, -52.0)                                              # the tail ends where the room has decayed
    x = x[:min(len(x), c1 + int(SR * 0.06))]
    fi, fo = int(SR * 0.005), int(SR * 0.06)
    x[:fi] *= np.linspace(0, 1, fi, dtype=np.float32)
    x[-fo:] *= np.linspace(1, 0, fo, dtype=np.float32)
    write_f32(dst, np.clip(x, -1.0, 1.0))


def lufs(path):
    """Integrated loudness (EBU R128, ffmpeg ebur128) of a file."""
    r = subprocess.run(["ffmpeg", "-nostats", "-i", path, "-af", "ebur128", "-f", "null", "-"], capture_output=True, text=True)
    m = re.search(r"I:\s+(-?[\d.]+) LUFS", r.stderr[r.stderr.rfind("Summary:"):])
    return float(m.group(1)) if m else float("nan")


def loudnorm_two_pass(src, dst, quality=4):
    """Brings a take to TARGET_LUFS with a measured gain and a peak limiter at -1.9 dBFS, re-measured up to three times (ffmpeg's own loudnorm falls back to a dynamic mode on a clip with a long room tail
    and misses the target by up to 2 LU; a gain that is measured, applied and measured again does not). Then Ogg Vorbis."""
    gain = 0.0
    with tempfile.TemporaryDirectory() as td:
        tmp = os.path.join(td, "n.wav")
        for _ in range(3):
            ff(["-i", src, "-af", "volume=%.3fdB,alimiter=limit=0.77:level=0" % gain, "-c:a", "pcm_f32le", tmp])
            err = TARGET_LUFS - lufs(tmp)
            if abs(err) < 0.12:
                break
            gain += err
        ff(["-i", tmp, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", str(quality), dst])


# ------------------------------------------------------------------------------------------------ driver
def lines_of(boss):
    doc = json.load(open(os.path.join(HERE, boss + "_lines.json"), encoding="utf-8"))
    return doc["lines"]


def report():
    """Measures the first-pass files (voice/<boss>/classic/) and the current ones with voice_measure, writes tools/audio/boss_voice_polish_report.json (summary and per clip, before and after) and rewrites
    tools/audio/<boss>_voice_report.json in the format offline_voice.py convert writes (seconds, bytes, text of each line)."""
    import voice_measure as VM
    out = {}
    for boss in ("goku", "sinbad"):
        d = os.path.join(VOICE_OUT, boss)
        before, after = VM.measure_dir(os.path.join(d, "classic")), VM.measure_dir(d)
        out[boss] = {"before_summary": VM.summary(before), "after_summary": VM.summary(after), "before": before, "after": after,
                     "bytes_before": sum(os.path.getsize(os.path.join(d, "classic", n + ".ogg")) for n in before), "bytes_after": sum(os.path.getsize(os.path.join(d, n + ".ogg")) for n in after)}
        conv = {ln["id"]: {"seconds": round(after[ln["id"]]["seconds"], 2), "bytes": os.path.getsize(os.path.join(d, ln["id"] + ".ogg")), "text": OV.spoken(ln)[0]} for ln in lines_of(boss)}
        with open(os.path.join(HERE, boss + "_voice_report.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"converted": conv, "missing": [], "rejected": [], "bytes": sum(c["bytes"] for c in conv.values())}, f, indent=1)
    with open(os.path.join(HERE, "boss_voice_polish_report.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=1)
    for boss, o in out.items():
        print(boss, "before", json.dumps(o["before_summary"]), "\n     after ", json.dumps(o["after_summary"]), "\n     bytes", o["bytes_before"], "->", o["bytes_after"])
    return 0


def selftest():
    """The pure parts (no Kokoro, no ffmpeg): respelling, the British adaptation, the shout syllables, the line path."""
    class Fake:
        def phonemize(self, s, lang):
            return "<%s>" % s
    assert respell("Kamehameha") == PRON["kamehameha"] and respell("kamehameha") == PRON["kamehameha"]
    assert respell("Hello") is None and respell("me") is None
    assert respell("HAAAAAA") == "hˈɑːɑːɑːɑː" and respell("AAAAAAARGH") == "ˈɑːɑːɑːɹɡ" and respell("Haaaah") == "hˈɑːɑːɑːɑː"
    assert respell("Inqerad-Saiqa") == PRON["inqerad"] + " " + PRON["saiqa"]
    assert "ɹ" not in british("fˈɔːɹ") and british("bˈɑːɹɑː") == "bˈɑːɹɑː" and british("bˈɑːɹˌæk") == "bˈɑːɹˌæk" and british("fˈoʊ") == "fˈəʊ"
    assert respell("Focalor", "en-gb") == british(PRON["focalor"]) and respell("Focalor") == PRON["focalor"]
    assert phonemes(Fake(), "Just a plain line.", "en-us") is None
    assert phonemes(Fake(), "Kamehameha!", "en-us") == PRON["kamehameha"] + " !"
    assert phonemes(Fake(), "Hey, Saiyan 3!", "en-us") == "<Hey,> " + PRON["saiyan"] + " <3!>"
    assert phonemes(Fake(), "Ka... me... ha... me... HAAAAA!", "en-us") == LINE_IPA["Ka... me... ha... me... HAAAAA!"]
    for k, v in PRON.items():
        assert v and all(c not in v for c in "0123456789"), k
    print("boss_voice_polish selftest ok (%d respellings)" % len(PRON))
    return 0


def main(argv):
    if argv and argv[0] == "selftest":
        return selftest()
    if argv and argv[0] == "report":
        return report()
    if not argv or argv[0] not in ("synth", "convert", "probe"):
        print(__doc__)
        return 2
    opt = lambda k, d=None: argv[argv.index(k) + 1] if k in argv else d
    bosses = [opt("--boss")] if opt("--boss") else ["goku", "sinbad"]
    only = set(opt("--only", "").split(",")) - {""}
    cmd = argv[0]
    if opt("--profiles"):                                                          # try a profile file other than the repository's
        OV.PROFILES_JSON = os.path.abspath(opt("--profiles"))
    profs = OV.profiles()
    voice = Voice()
    for boss in bosses:
        raw = opt("--raw", os.path.join(tempfile.gettempdir(), "nusmp_voice_raw_" + boss))
        prof = profs[opt("--profile", boss + "2")]
        out = opt("--out", os.path.join(VOICE_OUT, boss))
        os.makedirs(raw, exist_ok=True)
        n = 0
        for ln in lines_of(boss):
            if only and ln["id"] not in only:
                continue
            text, tags = OV.spoken(ln)
            wav = os.path.join(raw, ln["id"] + ".wav")
            if cmd == "probe":
                if voice.kokoro is None:
                    voice.say2(prof, "ok", 1.0)
                print("%-28s %s" % (ln["id"], phonemes(voice.kokoro.tokenizer, text, prof.get("lang", "en-us")) or "(plain text)"))
            elif cmd == "synth":
                render(voice, prof, text, tags, ln["group"], wav)
                n += 1
            else:
                os.makedirs(out, exist_ok=True)
                loudnorm_two_pass(wav, os.path.join(out, ln["id"] + ".ogg"))
                n += 1
        if cmd != "probe":
            print("%s: %s %d lines (%s)" % (boss, cmd, n, raw if cmd == "synth" else out))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
