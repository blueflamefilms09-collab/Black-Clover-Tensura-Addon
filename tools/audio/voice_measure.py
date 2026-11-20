#!/usr/bin/env python3
"""Measures voice clips (offline; ffmpeg + numpy): loudness, peak, spectrum, silences. Used to compare the boss voice lines before and after the polish
(docs/boss_redesign_goku_sinbad.md, "Kokoro voice pass"). It measures; it cannot judge how a voice sounds.

  python3 -B tools/audio/voice_measure.py <dir-of-ogg> [<dir-of-ogg> ...] [--json out.json]

Per clip: seconds, integrated loudness (LUFS, EBU R128, ffmpeg ebur128), true peak (dBTP), sample peak (dBFS), spectral centroid (Hz), the share of the energy above 5 kHz (harshness /
hiss proxy) and below 150 Hz (rumble / boom proxy), the median pitch f0 (Hz, autocorrelation), lead and tail silence (s, threshold -45 dB). Per directory: mean and spread of the loudness (a steady set has a small spread).
"""
import json
import os
import re
import subprocess
import sys

import numpy as np

SR = 32000


def decode(path):
    r = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"], capture_output=True, check=True)
    return np.frombuffer(r.stdout, dtype=np.float32)


def ebur(path):
    r = subprocess.run(["ffmpeg", "-nostats", "-i", path, "-af", "ebur128=peak=true", "-f", "null", "-"], capture_output=True, text=True)
    t = r.stderr[r.stderr.rfind("Summary:"):]
    g = lambda pat: float(re.search(pat, t).group(1)) if re.search(pat, t) else float("nan")
    return g(r"I:\s+(-?[\d.]+) LUFS"), g(r"Peak:\s+(-?[\d.]+) dBFS")


def db(x):
    return 20 * np.log10(max(float(x), 1e-9))


def f0_median(x):
    """Median pitch (Hz) of the voiced 40 ms frames by autocorrelation (70 to 400 Hz); NaN when nothing is voiced. A rough number, good for comparing two takes of one line."""
    fl, hop = int(SR * 0.04), int(SR * 0.01)
    lo, hi = int(SR / 400), int(SR / 70)
    out = []
    for i in range(0, len(x) - fl * 2, hop):
        fr = x[i:i + fl * 2]
        if np.sqrt((fr ** 2).mean()) < 0.02:
            continue
        fr = fr - fr.mean()
        ac = np.correlate(fr, fr, "full")[len(fr) - 1:]
        p = int(ac[lo:hi].argmax()) + lo
        if ac[p] / (ac[0] + 1e-9) > 0.45:
            out.append(SR / p)
    return float(np.median(out)) if out else float("nan")


def clip_stats(path):
    x = decode(path)
    n = len(x)
    lufs, tp = ebur(path)
    spec = np.abs(np.fft.rfft(x * np.hanning(n))) ** 2
    f = np.fft.rfftfreq(n, 1.0 / SR)
    tot = spec.sum() + 1e-12
    env = np.abs(x)
    live = np.where(env > 10 ** (-45 / 20))[0]
    lead = live[0] / SR if len(live) else n / SR
    tail = (n - 1 - live[-1]) / SR if len(live) else n / SR
    return {"seconds": round(n / SR, 3), "lufs": round(lufs, 2), "true_peak_db": round(tp, 2), "sample_peak_db": round(db(env.max()), 2),
            "centroid_hz": round(float((f * spec).sum() / tot), 0), "above_5k_pct": round(100 * float(spec[f > 5000].sum() / tot), 2),
            "below_150_pct": round(100 * float(spec[f < 150].sum() / tot), 2), "f0_hz": round(f0_median(x), 0), "lead_s": round(lead, 3), "tail_s": round(tail, 3)}


def measure_dir(d):
    out = {}
    for n in sorted(os.listdir(d)):
        if n.endswith(".ogg"):
            out[n[:-4]] = clip_stats(os.path.join(d, n))
    return out


def summary(stats):
    v = list(stats.values())
    col = lambda k: np.array([c[k] for c in v], dtype=float)
    return {"clips": len(v), "seconds_total": round(float(col("seconds").sum()), 1), "seconds_mean": round(float(col("seconds").mean()), 2),
            "lufs_mean": round(float(np.nanmean(col("lufs"))), 2), "lufs_std": round(float(np.nanstd(col("lufs"))), 2), "lufs_min": round(float(np.nanmin(col("lufs"))), 2), "lufs_max": round(float(np.nanmax(col("lufs"))), 2),
            "true_peak_max_db": round(float(np.nanmax(col("true_peak_db"))), 2), "centroid_mean_hz": round(float(col("centroid_hz").mean()), 0),
            "above_5k_mean_pct": round(float(col("above_5k_pct").mean()), 2), "below_150_mean_pct": round(float(col("below_150_pct").mean()), 2), "f0_mean_hz": round(float(np.nanmean(col("f0_hz"))), 0),
            "lead_mean_s": round(float(col("lead_s").mean()), 3), "tail_mean_s": round(float(col("tail_s").mean()), 3)}


def main(argv):
    out_json = argv[argv.index("--json") + 1] if "--json" in argv else None
    dirs = [a for i, a in enumerate(argv) if not a.startswith("--") and (i == 0 or argv[i - 1] != "--json")]
    res = {}
    for d in dirs:
        st = measure_dir(d)
        res[d] = {"summary": summary(st), "clips": st}
        print(d, json.dumps(res[d]["summary"]))
    if out_json:
        with open(out_json, "w") as f:
            json.dump(res, f, indent=1)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
