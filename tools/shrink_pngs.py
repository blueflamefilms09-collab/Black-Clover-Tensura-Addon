#!/usr/bin/env python3
"""Lossy-but-gated PNG shrinker for src/main/resources (re-runnable, idempotent). 1.113.0.

Why: the jar is a zip and a PNG is already deflated, so the ~11,000 textures (350 MB, 195 MB of it in files over 500 KB: the 2048 px weapon skins of
entity/weapon_vu, the Genesis and God skins, the big entity skins) are almost the whole jar. tools/optimize_pngs.py is lossless and saved little. This tool reduces a
true-colour texture to a 256-colour palette (what these cel-style, flat-colour skins can carry), and keeps the result ONLY when:
  * the file is not already a palette image, and is at least --min-kb (default 60 KB);
  * the new file is at least --min-gain (default 25 percent) smaller;
  * the alpha-weighted PSNR against the original is at least --min-psnr (default 38 dB) and the alpha channel's worst error stays small (--max-alpha-err, default 24 of 255),
    so a soft glow edge or a transparent cut-out is never wrecked;
  * the size in pixels is unchanged (UV maps, geo and mesh files depend on it).
Nothing is resized. Every decision is printed; --check changes nothing. After a run: python3 -B tools/safe_additive/scan_textures.py --fix (colour under alpha in glow maps) and
python3 -B tools/optimize_pngs.py (oxipng on the new palette files).

    python3 -B tools/shrink_pngs.py --check             report only
    python3 -B tools/shrink_pngs.py                     rewrite in place
    python3 -B tools/shrink_pngs.py path/to/dir         only below that directory
    python3 -B tools/shrink_pngs.py --exclude sylph,undine   skip files whose path contains one of these words
Needs: pip install pillow numpy imagequant (imagequant is optional: without it the lower-quality Pillow octree is used and fewer files pass the gate)
"""
import argparse
import io
import os
import sys
import time
from multiprocessing import Pool

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "src", "main", "resources"))


def work(args):
    path, min_kb, min_gain, min_psnr, max_alpha_err, check = args
    try:
        old_size = os.path.getsize(path)
        if old_size < min_kb * 1024:
            return path, old_size, old_size, "small"
        im = Image.open(path)
        im.load()
        if im.mode == "P":
            return path, old_size, old_size, "palette"
        rgba = im.convert("RGBA")
        a = np.asarray(rgba).astype(np.float64)
        best = None
        try:                                                  # imagequant (the library of pngquant): the best palette for RGBA, about 43 dB on the 2048 px weapon skins
            import imagequant
            q = imagequant.quantize_pil_image(rgba, dithering_level=0.0, max_colors=256)
            buf = io.BytesIO()
            q.save(buf, "PNG", optimize=True)
            best = (q, buf.getvalue())
        except Exception:
            pass
        if best is None:                                      # fallback: Pillow's fast octree (about 35 dB)
            q = rgba.quantize(colors=256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
            buf = io.BytesIO()
            q.save(buf, "PNG", optimize=True)
            best = (q, buf.getvalue())
        q, data = best
        if len(data) > old_size * (1.0 - min_gain):
            return path, old_size, old_size, "gain<%d%%" % int(min_gain * 100)
        r = np.asarray(q.convert("RGBA")).astype(np.float64)
        w = a[..., 3:4] / 255.0                                   # alpha weighted: invisible pixels do not count
        diff = ((a[..., :3] - r[..., :3]) ** 2) * w
        denom = max(1.0, float(w.sum()) * 3.0)
        mse = float(diff.sum()) / denom
        psnr = 99.0 if mse <= 1e-9 else 10.0 * np.log10(255.0 ** 2 / mse)
        aerr = float(np.abs(a[..., 3] - r[..., 3]).max())
        if psnr < min_psnr:
            return path, old_size, old_size, "psnr %.1f" % psnr
        if aerr > max_alpha_err:
            return path, old_size, old_size, "alpha %.0f" % aerr
        if q.size != im.size:
            return path, old_size, old_size, "size changed"
        if not check:
            tmp = path + ".shrink"
            with open(tmp, "wb") as f:
                f.write(data)
            os.replace(tmp, path)
        return path, old_size, len(data), "ok psnr %.1f" % psnr
    except Exception as e:  # keep the file as it is
        return path, 0, 0, "ERROR %s" % e


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir", nargs="?", default=ROOT)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--min-kb", type=int, default=60)
    ap.add_argument("--min-gain", type=float, default=0.25)
    ap.add_argument("--min-psnr", type=float, default=38.0)
    ap.add_argument("--max-alpha-err", type=float, default=24.0)
    ap.add_argument("--exclude", default="", help="comma separated words: skip files whose path contains one")
    ap.add_argument("--jobs", type=int, default=os.cpu_count() or 2)
    ap.add_argument("--verbose", action="store_true")
    a = ap.parse_args()
    ex = [w for w in a.exclude.split(",") if w]
    files = []
    for d, _, fs in os.walk(a.dir):
        for f in fs:
            p = os.path.join(d, f)
            if f.lower().endswith(".png") and not any(w in p for w in ex):
                files.append(p)
    files.sort()
    t0 = time.time()
    before = after = changed = 0
    reasons = {}
    with Pool(a.jobs) as pool:
        for i, (path, o, n, st) in enumerate(pool.imap_unordered(work, [(f, a.min_kb, a.min_gain, a.min_psnr, a.max_alpha_err, a.check) for f in files], chunksize=4), 1):
            before += o
            after += n
            key = st.split(" ")[0]
            reasons[key] = reasons.get(key, 0) + 1
            if st.startswith("ok"):
                changed += 1
                if a.verbose:
                    print("  %-60s %6d KB -> %6d KB  %s" % (os.path.relpath(path, a.dir), o // 1024, n // 1024, st))
            elif st.startswith("ERROR"):
                print("  ", st, os.path.relpath(path, a.dir))
            if i % 1000 == 0:
                print("  %d/%d  %.0f s  saved %.1f MB so far" % (i, len(files), time.time() - t0, (before - after) / 1048576.0), flush=True)
    print("%d png files, %d reduced; kept: %s" % (len(files), changed, ", ".join("%s %d" % kv for kv in sorted(reasons.items()) if kv[0] != "ok")))
    print("%.1f MB -> %.1f MB (saved %.1f MB) in %.0f s%s" % (before / 1048576.0, after / 1048576.0, (before - after) / 1048576.0, time.time() - t0, "  [check only]" if a.check else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
