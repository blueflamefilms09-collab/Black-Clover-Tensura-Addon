"""Icon forge v3: regenerates the skill icons in pixel art (docs/skill_icons_v3.md, docs/skill_icons_v3/study.md).

    python tools/icon_forge_v3/forge.py                    # everything (root icons, animated strips, magic icons, manifests, index)
    python tools/icon_forge_v3/forge.py --only goku        # only ids / magic keys containing 'goku' (does not touch manifests)
    python tools/icon_forge_v3/forge.py --sheet out.png    # a contact sheet of the new icons instead of writing them
    python tools/icon_forge_v3/forge.py --doc              # before / after sheets for docs/skill_icons_v3/

Outputs (under src/main/resources/assets/anime_multiverse/), same paths and sizes as before, so nothing else changes:
  textures/skill/<id>.png, anim/<id>_anim.png(.mcmeta), grimoire/<k>.png, icons/<k>_buff.png, icons/<k>_ultimate.png
  textures/skill_old/<same relative path>   the icon as it was before this redesign, copied once and never overwritten (rule 2);
                                            the client setting skillIcons.classic = true shows these
  skill_icons.json                          index read by the game and the tests (styled ids, animated ids, frames, frame time)
  tools/skills/icons_v3.txt                 manifest read by SkillIconsV3Test
  tools/icon_forge_v3/icons_v4.txt          every path this forge replaced (read by SkillIconsForgeTest and by skill_icons_old.json)
  skill_icons_old.json                      the same list for the client switch (paths under textures/skill)
Deterministic: the same code gives the same bytes.
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
import grim                                        # noqa: E402  (must come first: fixes the import order of tools/gen_skill_icons.py)
import pxr                                         # noqa: E402
import icon3_lib as L                              # noqa: E402
from icon3_parts import ICONS                      # noqa: E402
import icon3_root                                  # noqa: E402,F401
import icon3_beasts                                # noqa: E402,F401
import icon3_db                                    # noqa: E402,F401
import veldanava_v3 as VEL                     # noqa: E402
from misc_symbols import MISC                      # noqa: E402  (the extra icons: five Berserk lords, Saiyan Prodigy)

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
TEX = os.path.join(ASSETS, "textures", "skill")
OLD = os.path.join(ASSETS, "textures", "skill_old")
ANIM = os.path.join(TEX, "anim")
INDEX = os.path.join(ASSETS, "skill_icons.json")
OLD_INDEX = os.path.join(ASSETS, "skill_icons_old.json")
MANIFEST = os.path.join(ROOT, "tools", "skills", "icons_v3.txt")
REPLACED = os.path.join(HERE, "icons_v4.txt")
DOCS = os.path.join(ROOT, "docs", "skill_icons_v3")
FRAMES = 12
FRAMETIME = 4

# root Uniques (not the Beast Will / Dragon Ball pairs) animate like the Ultimates: they are the skills a player looks at most
ANIM_UNIQUE_SKIP = ("bw_", "db_")


def seed_of(id_):
    h = 2166136261
    for ch in id_:
        h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return h


def animated(ic):
    return ic["cls"] == "ultimate" or (ic["cls"] == "unique" and not ic["id"].startswith(ANIM_UNIQUE_SKIP))


def scene(ic):
    c = L.Ctx(ic["pal"])
    ic["fn"](c)
    seed = seed_of(ic["id"])
    bg = pxr.make_bg(ic["pal"], seed)
    if ic["cls"] == "ultimate":                    # a dashed rune ring behind the symbol tells an Ultimate from the Unique that shares its symbol
        grim.ring_px(bg, 15.5, 15.5, 13.0, pxr.rgb(ic["pal"]["glow"]), 0.45, ticks=0, dash=2)
    return pxr.build(c.items, ic["pal"], seed, bg=bg)


def save_png(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    a = np.array(im.convert("RGBA"))
    a[a[..., 3] == 0] = 0
    Image.fromarray(a, "RGBA").save(path, optimize=True)


def keep_old(rel):
    """The icon as it was before this redesign, copied once and never overwritten (rule 2)."""
    src, dst = os.path.join(TEX, rel), os.path.join(OLD, rel)
    if os.path.exists(src) and not os.path.exists(dst):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)


def write_root(only=None):
    done, replaced = [], []
    for ic in ICONS:
        if only and only not in ic["id"]:
            continue
        sc = scene(ic)
        rel = ic["id"] + ".png"
        keep_old(rel)
        save_png(pxr.to_image(pxr.compose(sc, ic["cls"])), os.path.join(TEX, rel))
        replaced.append(rel)
        if animated(ic):
            srel = "anim/" + ic["id"] + "_anim.png"
            if ic["cls"] == "ultimate":                # only the Ultimates had a strip before; the Unique strips are new
                keep_old(srel)
            out = Image.new("RGBA", (L.OUT, L.OUT * FRAMES), (0, 0, 0, 0))
            for i in range(FRAMES):
                out.paste(pxr.to_image(pxr.compose(sc, ic["cls"], i / FRAMES)), (0, i * L.OUT))
            save_png(out, os.path.join(TEX, srel))
            with open(os.path.join(TEX, srel + ".mcmeta"), "w", newline="\n") as f:
                json.dump({"animation": {"frametime": FRAMETIME, "interpolate": False}}, f, indent=2)
                f.write("\n")
            replaced.append(srel)
        done.append(ic)
    return done, replaced


def write_misc(only=None):
    done, replaced = [], []
    for ic in MISC:
        if only and only not in ic["id"]:
            continue
        rel = ic["id"] + ".png"
        keep_old(rel)
        save_png(pxr.to_image(pxr.compose(scene(ic), ic["cls"])), os.path.join(TEX, rel))
        replaced.append(rel)
        done.append(ic)
    return done, replaced


def write_veld(only=None):
    """The five Veldanava Ultimates: 32 px pixel-art medallions, 32 frames (the strips AnimatedIconsTest pins); the hero still is frame 16 (no glint, neutral pulse)."""
    done, replaced = [], []
    n = 32
    for ic in VEL.VELD:
        if only and only not in ic["id"]:
            continue
        seed = seed_of(ic["id"])
        c = L.Ctx(ic["pal"])
        ic["fn"](c)
        sc = pxr.build(c.items, ic["pal"], seed, bg=pxr.make_bg(ic["pal"], seed))
        frames = [pxr.to_image(pxr.compose(sc, "ultimate", i / n, medallion=VEL.GOLD)) for i in range(n)]
        strip = Image.new("RGBA", (32, 32 * n), (0, 0, 0, 0))
        for i, f in enumerate(frames):
            strip.paste(f, (0, i * 32))
        for rel, im in (("veldanava/%s_anim.png" % ic["id"], strip), ("veldanava/%s.png" % ic["id"], frames[n // 2])):
            keep_old(rel)
            save_png(im, os.path.join(TEX, rel))
            replaced.append(rel)
        done.append(ic)
    return done, replaced


def magic_targets():
    """[(key, glow, body, dark, [(kind, relative path)])] for every magic icon the forge repaints."""
    from gen_icons_v2 import SET_B
    out = []
    for key, glow, body, dark in grim.keys():
        t = [("active", "grimoire/%s.png" % key)]
        if key not in SET_B:
            t += [("buff", "icons/%s_buff.png" % key), ("ultimate", "icons/%s_ultimate.png" % key)]
        out.append((key, glow, body, dark, t))
    return out


def write_magic(only=None):
    done, replaced = [], []
    for key, glow, body, dark, targets in magic_targets():
        if only and only not in key:
            continue
        for kind, rel in targets:
            keep_old(rel)
            im = grim.render(key, glow, body, dark, kind, seed_of(key + kind))
            save_png(im, os.path.join(TEX, rel))
            replaced.append(rel)
            done.append(rel)
    return done, replaced


def write_manifests(root_done, replaced):
    lines = ["# id|class|frames (written by tools/icon_forge_v3/forge.py, read by SkillIconsV3Test)"]
    for ic in root_done:
        lines.append("%s|%s|%d" % (ic["id"], ic["cls"], FRAMES if animated(ic) else 1))
    with open(MANIFEST, "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    idx = {"version": 2, "frametime": FRAMETIME, "styled": [ic["id"] for ic in root_done],
           "animated": [ic["id"] for ic in root_done if animated(ic)], "frames": FRAMES}
    with open(INDEX, "w", newline="\n") as f:
        json.dump(idx, f, indent=1)
        f.write("\n")
    with open(REPLACED, "w", newline="\n") as f:
        f.write("# every path under textures/skill that tools/icon_forge_v3/forge.py replaced (the old picture is under textures/skill_old/<same path>)\n")
        f.write("\n".join(sorted(replaced)) + "\n")
    # the Veldanava medallions are swapped by client/veldanava/AnimatedIcons (its own switch), so the classic list leaves them out
    still = sorted(p for p in replaced if p.endswith(".png") and not p.startswith(("anim/", "veldanava/")))
    with open(OLD_INDEX, "w", newline="\n") as f:
        json.dump({"version": 1, "paths": still}, f, indent=1)
        f.write("\n")


def sheet(ims, path, scale=3, cols=12, labels=None, bg=(34, 34, 42, 255)):
    cell = 32 * scale
    rows = (len(ims) + cols - 1) // cols
    lab = 9 if labels else 0
    sh = Image.new("RGBA", (cols * (cell + 2), rows * (cell + 2 + lab)), bg)
    d = ImageDraw.Draw(sh)
    for i, im in enumerate(ims):
        x, y = (i % cols) * (cell + 2), (i // cols) * (cell + 2 + lab)
        sh.alpha_composite(im.convert("RGBA").resize((cell, cell), Image.NEAREST), (x, y))
        if labels:
            d.text((x + 1, y + cell), labels[i][:int(cell / 5.5)], fill=(210, 210, 220, 255))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    sh.convert("RGB").quantize(colors=128, method=Image.MEDIANCUT, dither=Image.NONE).save(path, optimize=True)


def doc_sheets():
    """Before / after sheets (3x), under 600 KB each; the animated ones as 12-frame strips."""
    os.makedirs(DOCS, exist_ok=True)
    groups = [("root", lambda i: not i.startswith(("bw_", "db_"))), ("beast", lambda i: i.startswith("bw_")), ("dragonball", lambda i: i.startswith("db_"))]
    for name, pick in groups:
        ids = [ic["id"] for ic in ICONS if pick(ic["id"])]
        sheet([Image.open(os.path.join(OLD, i + ".png")) for i in ids], os.path.join(DOCS, name + "_before.png"), 3, 12, ids)
        sheet([Image.open(os.path.join(TEX, i + ".png")) for i in ids], os.path.join(DOCS, name + "_after.png"), 3, 12, ids)
    extra = [ic["id"] + ".png" for ic in MISC] + ["veldanava/%s.png" % ic["id"] for ic in VEL.VELD]
    sheet([Image.open(os.path.join(OLD, r)) for r in extra], os.path.join(DOCS, "extras_before.png"), 4, 11, [os.path.basename(r)[:-4] for r in extra])
    sheet([Image.open(os.path.join(TEX, r)) for r in extra], os.path.join(DOCS, "extras_after.png"), 4, 11, [os.path.basename(r)[:-4] for r in extra])
    for kind, suffix, folder in (("grimoire", "", "grimoire"), ("buff", "_buff", "icons"), ("ultimate", "_ultimate", "icons")):
        rels = []
        for key, glow, body, dark, targets in magic_targets():
            for k, rel in targets:
                if rel == "%s/%s%s.png" % (folder, key, suffix):
                    rels.append((key, rel))
        for half in (0, 1):
            part = rels[half * 53:(half + 1) * 53]
            if not part:
                continue
            sheet([Image.open(os.path.join(OLD, r)) for _, r in part], os.path.join(DOCS, "magic_%s_%d_before.png" % (kind, half + 1)), 3, 14, [k for k, _ in part])
            sheet([Image.open(os.path.join(TEX, r)) for _, r in part], os.path.join(DOCS, "magic_%s_%d_after.png" % (kind, half + 1)), 3, 14, [k for k, _ in part])
    anim = [ic for ic in ICONS if animated(ic)]
    for k in range(0, len(anim), 10):
        part = anim[k:k + 10]
        cell = 32 * 3
        sh = Image.new("RGBA", (FRAMES * (cell + 1), len(part) * (cell + 1)), (34, 34, 42, 255))
        for r, ic in enumerate(part):
            st = Image.open(os.path.join(ANIM, ic["id"] + "_anim.png")).convert("RGBA")
            for f in range(FRAMES):
                fr = st.crop((0, f * 32, 32, (f + 1) * 32)).resize((cell, cell), Image.NEAREST)
                sh.alpha_composite(fr, (f * (cell + 1), r * (cell + 1)))
        sh.convert("RGB").quantize(colors=160, method=Image.MEDIANCUT, dither=Image.NONE).save(os.path.join(DOCS, "animated_frames_%d.png" % (k // 10 + 1)), optimize=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only")
    ap.add_argument("--sheet")
    ap.add_argument("--doc", action="store_true")
    a = ap.parse_args()
    if a.sheet and not a.doc:
        sel = [ic for ic in ICONS + MISC if not a.only or a.only in ic["id"]]
        sheet([pxr.to_image(pxr.compose(scene(ic), ic["cls"])) for ic in sel], a.sheet, 5, 8, [ic["id"] for ic in sel])
        return
    if a.doc:
        doc_sheets()
        return
    root_done, rep1 = write_root(a.only)
    magic_done, rep2 = write_magic(a.only)
    misc_done, rep3 = write_misc(a.only)
    veld_done, rep4 = write_veld(a.only)
    if not a.only:
        write_manifests(ICONS, rep1 + rep2 + rep3 + rep4)
    print("wrote %d root icons (%d animated), %d magic icons, %d misc icons, %d Veldanava medallions" % (len(root_done), sum(1 for ic in root_done if animated(ic)), len(magic_done), len(misc_done), len(veld_done)))


if __name__ == "__main__":
    main()
