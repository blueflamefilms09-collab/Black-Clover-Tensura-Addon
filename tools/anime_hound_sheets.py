"""Labelled contact sheets for the anime hell hound pilot (PREVIEW ONLY).

    python3 -B tools/gen_anime_hound.py --preview          # renders the scenes first (software rasteriser)
    python3 -B tools/anime_hound_sheets.py                 # composes docs/mobs/phaseE/hound_*.png

Reads the renders from <HOUND_SCRATCH>/previews_hound (same default as gen_anime_hound.py) and the geo files for the quad counts.
"""
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
sys.path.insert(0, HERE)
import gen_anime_hound as G  # noqa: E402  (only for SCRATCH / PREVIEW_OUT)

SRC = G.PREVIEW_OUT
OUT = os.path.join(REPO, "docs", "mobs", "phaseE")
GEO = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp", "geo", "entity")
BG = (24, 24, 30)
NEW_C, OLD_C = (120, 230, 140), (240, 130, 110)


def font(sz, bold=False):
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""),):
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()


def quads(name):
    g = json.load(open(os.path.join(GEO, name + ".geo.json")))
    n = c = 0
    for b in g["minecraft:geometry"][0]["bones"]:
        for cu in b.get("cubes", []):
            c += 1
            uv = cu.get("uv")
            if isinstance(uv, dict):
                n += len(uv)
            else:
                n += 6
    return c, n


def load(name):
    return Image.open(os.path.join(SRC, name + ".png")).convert("RGB")


def sheet(title, parts, path, cols=1, gap=10, tag=None, tag_col=NEW_C, subtitle=""):
    """parts: [(label, image)]; laid out in `cols` columns under a title bar."""
    cw = max(i.width for _, i in parts)
    rows = [parts[k:k + cols] for k in range(0, len(parts), cols)]
    rh = [max(i.height for _, i in r) + 22 for r in rows]
    W = cols * cw + (cols + 1) * gap
    H = 46 + sum(rh) + gap * (len(rows) + 1)
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    x = gap
    if tag:
        d.rounded_rectangle((gap, 8, gap + 62, 36), 5, fill=tag_col)
        d.text((gap + 8, 11), tag, fill=(10, 10, 10), font=font(18, True))
        x = gap + 74
    d.text((x, 9), title, fill=(235, 235, 240), font=font(17, True))
    if subtitle:
        d.text((x, 29), subtitle, fill=(160, 165, 180), font=font(11))
    y = 46 + gap
    for r, h in zip(rows, rh):
        for k, (lab, i) in enumerate(r):
            px = gap + k * (cw + gap)
            d.text((px, y), lab, fill=(200, 205, 220), font=font(12, True))
            im.paste(i, (px, y + 20))
        y += h + gap
    im.save(path, optimize=True)
    return im.size


def main():
    os.makedirs(OUT, exist_ok=True)
    c0, q0 = quads("anime_hound")
    c1, q1 = quads("anime_hound_lod1")
    c2, q2 = quads("anime_hound_lod2")
    cf, qf = quads("anime_hound_fx")
    ow = quads("arc_dungeon_direwolf")
    ob = quads("foundry_bramble_pup")
    sizes = {}

    # 1. views: front/back/side/34 side by side
    views = [(n.replace("hound_", ""), load(n)) for n in ("hound_front", "hound_back", "hound_side", "hound_34")]
    sizes["views"] = sheet("Hell hound - front / back / side / 3/4", views, os.path.join(OUT, "hound_views.png"), cols=4,
                           tag="NEW", subtitle="LOD0: %d cubes/cards, %d quads (+%d glow-pass quads)  |  sheet 256x256 + glow sheet" % (c0, q0, qf))

    # 2. face plates
    faces = [("plate: " + p, load("hound_face_" + p)) for p in G.PLATES]
    sizes["faces"] = sheet("Face plates: open / narrow / closed / glow / snarl (bones toggled by scale)", faces, os.path.join(OUT, "hound_faces.png"), cols=5,
                           tag="NEW", subtitle="eye cards + glow; each plate is a bone, shown at scale 1 and hidden at scale 0.001")

    # 3. clips
    clips = [(n, load("hound_" + n)) for n in ("idle", "walk", "run", "bite", "pounce", "howl", "hurt", "death", "sleep")]
    sizes["clips"] = sheet("Clip pose strips: idle / walk / run / bite / pounce / howl / hurt / death / sleep", clips, os.path.join(OUT, "hound_clips.png"), cols=1,
                           tag="NEW", subtitle="9 clips in anime_hound.animation.json (+ pose_face_* preview-only clips)")

    # 4. LODs
    lods = [("LOD1 - %d cubes, %d quads" % (c1, q1), load("hound_lod1")), ("LOD2 - %d cubes, %d quads" % (c2, q2), load("hound_lod2"))]
    sizes["lods"] = sheet("Level of detail (same texture, same skeleton, same clips)", lods, os.path.join(OUT, "hound_lods.png"), cols=2, tag="NEW",
                          subtitle="LOD0 %d / LOD1 %d / LOD2 %d quads" % (q0, q1, q2))

    # 5. old vs new
    n34 = load("hound_34")
    new = ("NEW hell hound - %d cubes/cards, %d quads" % (c0, q0), n34)
    old1 = ("OLD arc_dungeon_direwolf - %d cubes, %d quads (humanoid)" % (ow[0], ow[1]), load("old_direwolf"))
    old2 = ("OLD foundry_bramble_pup - %d cubes, %d quads (humanoid)" % (ob[0], ob[1]), load("old_bramble"))
    im = Image.new("RGB", (10, 10))
    parts = [old1, old2, new]
    cw = max(i.width for _, i in parts)
    hh = max(i.height for _, i in parts)
    W = 3 * cw + 40
    H = 60 + hh + 30
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((10, 8), "OLD vs NEW - dungeon pup / direwolf", fill=(235, 235, 240), font=font(18, True))
    d.text((10, 32), "OLD = existing art-kit humanoid rig (kept, not touched). NEW = four-legged hound, preview only, nowhere wired in.", fill=(160, 165, 180), font=font(11))
    for k, (lab, i) in enumerate(parts):
        px = 10 + k * (cw + 10)
        col = NEW_C if lab.startswith("NEW") else OLD_C
        d.rounded_rectangle((px, 54, px + 54, 76), 4, fill=col)
        d.text((px + 6, 56), "NEW" if lab.startswith("NEW") else "OLD", fill=(10, 10, 10), font=font(15, True))
        d.text((px + 62, 58), lab.split(" ", 1)[1], fill=(215, 220, 232), font=font(11, True))
        im.paste(i, (px, 84))
    im.save(os.path.join(OUT, "hound_old_vs_new.png"), optimize=True)
    sizes["old_vs_new"] = im.size

    for k, v in sizes.items():
        print("hound_%s.png %dx%d" % (k, *v))
    tot = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT) if f.startswith("hound_"))
    print("sheets total %.0f KB" % (tot / 1024))


if __name__ == "__main__":
    main()
