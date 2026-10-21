#!/usr/bin/env python3
"""Lossless PNG shrinker for src/main/resources (re-runnable, idempotent).

Why: the textures are most of the jar (a jar is a zip and a PNG is already deflated, so zipping does nothing for them). This recompresses every PNG with oxipng and lets it reduce the
colour type where that loses nothing (RGBA -> palette when 256 colours or fewer, RGBA -> grey when the image is grey, and so on). Nothing is lossy:
  * the new file is decoded and compared with the old one pixel for pixel, INCLUDING the colour hidden under fully transparent pixels (several glow textures are painted colour-under-alpha,
    so oxipng's alpha clean-up stays off); a mismatch keeps the old file;
  * a result that is not smaller keeps the old file;
  * only safe metadata is stripped.

    python3 -B tools/optimize_pngs.py                 # rewrite in place and print the totals
    python3 -B tools/optimize_pngs.py --check         # change nothing, print what a run would save
    python3 -B tools/optimize_pngs.py --level 4       # slower, a little smaller (default 3)
    python3 -B tools/optimize_pngs.py path/to/dir     # only below that directory

Needs: pip install pyoxipng pillow numpy
"""
import argparse
import os
import sys
import time
from multiprocessing import Pool

import numpy as np
import oxipng
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "src", "main", "resources"))


def pixels(data_or_path):
    im = Image.open(data_or_path)
    im.load()
    return np.asarray(im.convert("RGBA"))


def work(args):
    path, level, check = args
    try:
        old = open(path, "rb").read()
        new = oxipng.optimize_from_memory(old, level=level, strip=oxipng.StripChunks.safe(), fix_errors=False, optimize_alpha=False)
        if len(new) >= len(old):
            return path, len(old), len(old), "same"
        import io
        a = pixels(io.BytesIO(old))
        b = pixels(io.BytesIO(new))
        if a.shape != b.shape or not np.array_equal(a, b):
            return path, len(old), len(old), "MISMATCH"
        if not check:
            tmp = path + ".opt"
            with open(tmp, "wb") as f:
                f.write(new)
            os.replace(tmp, path)
        return path, len(old), len(new), "ok"
    except Exception as e:  # keep the file as it is
        return path, 0, 0, "ERROR %s" % e


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir", nargs="?", default=ROOT)
    ap.add_argument("--level", type=int, default=3)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--jobs", type=int, default=os.cpu_count() or 2)
    a = ap.parse_args()
    files = []
    for d, _, fs in os.walk(a.dir):
        for f in fs:
            if f.lower().endswith(".png"):
                files.append(os.path.join(d, f))
    files.sort()
    t0 = time.time()
    before = after = 0
    changed = bad = 0
    with Pool(a.jobs) as p:
        for i, (path, o, n, st) in enumerate(p.imap_unordered(work, [(f, a.level, a.check) for f in files], chunksize=8), 1):
            before += o
            after += n
            if st == "ok":
                changed += 1
            elif st != "same":
                bad += 1
                print("  ", st, os.path.relpath(path, a.dir))
            if i % 1000 == 0:
                print("  %d/%d  %.0f s  saved %.1f MB so far" % (i, len(files), time.time() - t0, (before - after) / 1048576.0), flush=True)
    print("%d png files, %d smaller, %d kept as they were because of a mismatch or an error" % (len(files), changed, bad))
    print("%.1f MB -> %.1f MB (saved %.1f MB, %.1f %%) in %.0f s%s" % (before / 1048576.0, after / 1048576.0, (before - after) / 1048576.0, 100.0 * (before - after) / max(1, before), time.time() - t0, "  [check only]" if a.check else ""))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
