#!/usr/bin/env python3
"""Keeps the Beast Will forms of 3.77.0 / 4.14.x as they were BEFORE the body-parts pass (docs/body_parts_pass.md), as files `bwf_<stem>_v2_arm_<piece>` (geo + animation) and `bwf_<stem>_v2(.png,_glow.png)`,
the same way the 3.61 look is kept as `_v1`. The client setting `beastwill.classicHeads` (BeastWillSpiritOptions) draws them again. Run it ONCE, before forms_export.py rewrites the current files;
it never overwrites an existing `_v2` file, so a second run does nothing.

  python3 tools/beastwill/remake/snapshot_heads_v2.py hydra fenrir ...      (stems: see BeastWillHeadFit.REWORKED)
"""
import json, os, shutil, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.abspath(os.path.join(HERE, "..", "..", "..", "src", "main", "resources", "assets", "anime_multiverse"))


def snapshot(stem):
    geo, ani, tex = (os.path.join(RES, d, "entity") for d in ("geo", "animations", "textures"))
    n = 0
    for f in sorted(os.listdir(geo)):
        if not f.startswith("bwf_%s_arm_" % stem) or not f.endswith(".geo.json"):
            continue
        piece = f[len("bwf_%s_arm_" % stem):-len(".geo.json")]
        dst = os.path.join(geo, "bwf_%s_v2_arm_%s.geo.json" % (stem, piece))
        if os.path.exists(dst):
            continue
        text = open(os.path.join(geo, f)).read().replace("geometry.bwf_%s_arm_" % stem, "geometry.bwf_%s_v2_arm_" % stem)
        open(dst, "w", newline="\n").write(text)
        a = os.path.join(ani, f.replace(".geo.json", ".animation.json"))
        if os.path.exists(a):
            shutil.copyfile(a, os.path.join(ani, "bwf_%s_v2_arm_%s.animation.json" % (stem, piece)))
        n += 1
    for suffix in (".png", "_glow.png"):
        src, dst = os.path.join(tex, "bwf_%s%s" % (stem, suffix)), os.path.join(tex, "bwf_%s_v2%s" % (stem, suffix))
        if os.path.exists(src) and not os.path.exists(dst):
            shutil.copyfile(src, dst)
    return n


if __name__ == "__main__":
    for s in sys.argv[1:]:
        print(s, snapshot(s), "pieces kept as _v2")
