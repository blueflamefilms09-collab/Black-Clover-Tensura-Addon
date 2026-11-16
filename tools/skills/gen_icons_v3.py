"""Skill icons v3: paints the skill icons in the Tensura house style (docs/skill_icons_study.md, docs/skill_icons_rework.md).

    python tools/skills/gen_icons_v3.py                    # write every icon (stills, animated strips, classic copies, manifest, index)
    python tools/skills/gen_icons_v3.py --only goku        # only ids containing 'goku' (does not touch the manifest)
    python tools/skills/gen_icons_v3.py --sheet out.png    # a contact sheet of the new icons (not shipped)
    python tools/skills/gen_icons_v3.py --doc              # before/after sheets and frame strips for docs/skill_icons/

Outputs (all under src/main/resources/assets/anime_multiverse/):
  textures/skill/<id>.png                 the new 32 x 32 icon (the still / hero frame; the path the skill names as its icon)
  textures/skill/anim/<id>_anim.png       Ultimates only: the vertical strip, 32 x 32N, N <= 8 frames (a loop of N frames)
  textures/skill/anim/<id>_anim.png.mcmeta   {"animation": {"frametime": 6}}  (read by the client helper; vanilla ignores it, the strip is not in an atlas)
  textures/skill/classic/v440/<id>.png    the icon that shipped up to 4.4.0, copied once and never overwritten (rule 2); shown when skillIcons.newStyle is off
  skill_icons.json                        the index the game and the tests read: styled ids, animated ids, frame counts
Nothing is deleted or renamed. Deterministic: the same code gives the same bytes.
"""
import argparse
import json
import os
import shutil
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import icon3_lib as L                                  # noqa: E402
from icon3_parts import ICONS                          # noqa: E402
import icon3_root                                      # noqa: E402,F401
import icon3_beasts                                    # noqa: E402,F401
import icon3_db                                        # noqa: E402,F401

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
TEX = os.path.join(ASSETS, "textures", "skill")
ANIM = os.path.join(TEX, "anim")
OLD = os.path.join(TEX, "classic", "v440")
INDEX = os.path.join(ASSETS, "skill_icons.json")
MANIFEST = os.path.join(HERE, "icons_v3.txt")
DOCS = os.path.join(ROOT, "docs", "skill_icons")
FRAMES = 8
FRAMETIME = 6


def seed_of(id_):
    h = 2166136261
    for ch in id_:
        h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return h


def paint_icon(ic, t=None):
    return L.render(ic["fn"], ic["pal"], ic["cls"], seed_of(ic["id"]), t, ic["anim"] and t is not None)


def hero(ic):
    """The still: for an animated icon the frame where the glint rests on the symbol (a quarter loop), for the others the plain picture."""
    return paint_icon(ic, 0.0) if False else L.render(ic["fn"], ic["pal"], ic["cls"], seed_of(ic["id"]), None, False)


def strip(ic):
    frames = [paint_icon(ic, i / FRAMES) for i in range(FRAMES)]
    out = Image.new("RGBA", (L.OUT, L.OUT * FRAMES), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        out.paste(f, (0, i * L.OUT))
    return out


def save_png(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)


def clean(im):
    a = np.array(im.convert("RGBA"))
    a[a[..., 3] == 0] = 0
    return Image.fromarray(a, "RGBA")


def write_all(only=None):
    done = []
    for ic in ICONS:
        if only and only not in ic["id"]:
            continue
        live = os.path.join(TEX, ic["id"] + ".png")
        old = os.path.join(OLD, ic["id"] + ".png")
        if os.path.exists(live) and not os.path.exists(old):
            os.makedirs(OLD, exist_ok=True)
            shutil.copyfile(live, old)                   # the icon of 4.4.0, kept once (rule 2)
        save_png(clean(hero(ic)), live)
        if ic["anim"]:
            save_png(clean(strip(ic)), os.path.join(ANIM, ic["id"] + "_anim.png"))
            with open(os.path.join(ANIM, ic["id"] + "_anim.png.mcmeta"), "w", newline="\n") as f:
                json.dump({"animation": {"frametime": FRAMETIME}}, f, indent=2)
                f.write("\n")
        done.append(ic)
    return done


def write_manifest():
    lines = ["# id|class|frames (written by tools/skills/gen_icons_v3.py, read by SkillIconsV3Test)"]
    for ic in ICONS:
        lines.append("%s|%s|%d" % (ic["id"], ic["cls"], FRAMES if ic["anim"] else 1))
    with open(MANIFEST, "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    idx = {"version": 1, "frametime": FRAMETIME,
           "styled": [ic["id"] for ic in ICONS],
           "animated": [ic["id"] for ic in ICONS if ic["anim"]],
           "frames": FRAMES}
    with open(INDEX, "w", newline="\n") as f:
        json.dump(idx, f, indent=1)
        f.write("\n")


def sheet(ims, path, scale=4, cols=10, labels=None, bg=(34, 34, 42, 255)):
    cell = L.OUT * scale
    rows = (len(ims) + cols - 1) // cols
    lab = 11 if labels else 0
    sh = Image.new("RGBA", (cols * cell, rows * (cell + lab)), bg)
    d = ImageDraw.Draw(sh)
    for i, im in enumerate(ims):
        x, y = (i % cols) * cell, (i // cols) * (cell + lab)
        sh.alpha_composite(im.convert("RGBA").resize((cell, cell), Image.NEAREST), (x, y))
        if labels:
            d.text((x + 1, y + cell - 1), labels[i][:int(cell / 6)], fill=(210, 210, 220, 255))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    sh.save(path, optimize=True)


def write_docs():
    groups = [("root", lambda i: not i.startswith(("bw_", "db_"))), ("beast", lambda i: i.startswith("bw_")), ("dragonball", lambda i: i.startswith("db_"))]
    for name, pick in groups:
        sel = [ic for ic in ICONS if pick(ic["id"])]
        new = [Image.open(os.path.join(TEX, ic["id"] + ".png")) for ic in sel]
        old = [Image.open(os.path.join(OLD, ic["id"] + ".png")) for ic in sel]
        lbl = [ic["id"] for ic in sel]
        sheet(old, os.path.join(DOCS, name + "_before.png"), 3, 12, lbl)
        sheet(new, os.path.join(DOCS, name + "_after.png"), 3, 12, lbl)
    # the animated ones as frame strips: one row per icon, the 8 frames side by side
    anim = [ic for ic in ICONS if ic["anim"]]
    for k in range(0, len(anim), 14):
        part = anim[k:k + 14]
        cell = L.OUT * 3
        sh = Image.new("RGBA", (FRAMES * cell, len(part) * cell), (34, 34, 42, 255))
        for r, ic in enumerate(part):
            st = Image.open(os.path.join(ANIM, ic["id"] + "_anim.png")).convert("RGBA")
            for f in range(FRAMES):
                fr = st.crop((0, f * L.OUT, L.OUT, (f + 1) * L.OUT)).resize((cell, cell), Image.NEAREST)
                sh.alpha_composite(fr, (f * cell, r * cell))
        sh.save(os.path.join(DOCS, "animated_frames_%d.png" % (k // 14 + 1)), optimize=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only")
    ap.add_argument("--sheet")
    ap.add_argument("--doc", action="store_true")
    a = ap.parse_args()
    if a.sheet and not a.doc:
        sel = [ic for ic in ICONS if not a.only or a.only in ic["id"]]
        sheet([hero(ic) for ic in sel], a.sheet, 6, 8, [ic["id"] for ic in sel])
        return
    done = write_all(a.only)
    if not a.only:
        write_manifest()
    if a.doc:
        write_docs()
    print("wrote %d icons (%d animated)" % (len(done), sum(1 for ic in done if ic["anim"])))


if __name__ == "__main__":
    main()
