"""Icon forge v3: pure-Python mirror of the invariants the Java tests pin (SkillIconsV2Test, SkillIconsV3Test, SkillAssetsTest, SkillIconsForgeTest),
so a regeneration can be checked without Gradle.   python tools/icon_forge_v3/check.py"""
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "skill")
OLD = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "skill_old")
fail = []


def rd(p):
    return np.asarray(Image.open(p).convert("RGBA")).astype(int)


def centre(a, b):
    return np.abs(a[7:25, 7:25, :3] - b[7:25, 7:25, :3]).sum() / (18 * 18 * 3.0)


def main():
    # V2: per set, no two icons the same, centre diff >= MIN, fills the frame
    sets = {}
    for line in open(os.path.join(ROOT, "tools", "skills", "icons_v2.txt"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        k, p = line.strip().split("|")
        sets.setdefault(k, []).append(p)
    mind = 1e9
    for k, ps in sets.items():
        ims = [rd(os.path.join(TEX, p)) for p in ps]
        for i in range(len(ims)):
            assert ims[i].shape == (32, 32, 4), ps[i]
            op = (ims[i][..., 3] > 200).sum()
            if k == "A-sym":
                continue
            if op <= 32 * 32 * 0.9:
                fail.append("fill %s %d" % (ps[i], op))
            for j in range(i + 1, len(ims)):
                d = centre(ims[i], ims[j])
                mind = min(mind, d)
                if d < 25:
                    fail.append("V2 distinct %s %s %.1f" % (ps[i], ps[j], d))
    print("V2 min centre diff (non-sym sets): %.1f" % mind)
    # V3: root manifest
    n = 0
    for line in open(os.path.join(ROOT, "tools", "skills", "icons_v3.txt"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        i, cls, fr = line.strip().split("|")
        a = rd(os.path.join(TEX, i + ".png"))
        n += 1
        if a[0, 0, 3] != 0 or a[16, 16, 3] != 255:
            fail.append("corner/centre " + i)
        r, b = a[16, 1, 0], a[16, 1, 2]
        if cls == "unique" and not b > r:
            fail.append("frame unique " + i)
        if cls != "unique" and not r > b + 40:
            fail.append("frame gold " + i)
        old = rd(os.path.join(TEX, "classic", "v440", i + ".png"))
        if np.abs(a[..., :3] - old[..., :3]).mean() <= 4.0:
            fail.append("same as v440 " + i)
        fr = int(fr)
        sp = os.path.join(TEX, "anim", i + "_anim.png")
        if (fr > 1) != os.path.exists(sp):
            fail.append("strip existence " + i)
        if fr > 1:
            s = rd(sp)
            if s.shape[1] != 32 or s.shape[0] != 32 * fr:
                fail.append("strip size " + i)
            fs = [s[k * 32:(k + 1) * 32, :, :3] for k in range(fr)]
            moved = max(np.abs(fs[0] - fs[k]).mean() for k in range(1, fr))
            loop = np.abs(fs[-1] - fs[0]).mean()
            step = np.abs(fs[fr // 2 - 1] - fs[fr // 2]).mean()
            if moved <= 0.3:
                fail.append("static strip " + i)
            if loop > max(step * 2.5, 6.0):
                fail.append("loop not seamless %s %.2f vs %.2f" % (i, loop, step))
    print("root manifest icons:", n)
    # SkillAssetsTest: every skill icon 32x32, no two root icons identical, > 80 % opaque
    seen = {}
    for f in sorted(os.listdir(TEX)):
        if f.endswith(".png"):
            a = rd(os.path.join(TEX, f))
            if a.shape != (32, 32, 4):
                fail.append("size " + f)
            if (a[..., 3] > 0).sum() <= 32 * 32 * 0.8:
                fail.append("opaque " + f)
            h = a.tobytes()
            if h in seen:
                fail.append("duplicate %s %s" % (f, seen[h]))
            seen[h] = f
    # forge: every replaced path exists at its size, has an old copy and a small palette
    cols = []
    for line in open(os.path.join(ROOT, "tools", "icon_forge_v3", "icons_v4.txt"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        p = line.strip()
        a = rd(os.path.join(TEX, p))
        if a.shape[1] != 32 or a.shape[0] % 32:
            fail.append("forge size " + p)
        if not p.startswith("anim/") and not os.path.exists(os.path.join(OLD, p)):
            fail.append("no old copy " + p)
        if a.shape[0] == 32:
            c = len({tuple(x) for x in a.reshape(-1, 4)})
            cols.append(c)
            if c > 160:
                fail.append("palette too big %s %d" % (p, c))
    print("forge icons %d, colours: median %d max %d" % (len(cols), np.median(cols), max(cols)))
    print("FAIL" if fail else "OK")
    for f in fail:
        print("  ", f)
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
