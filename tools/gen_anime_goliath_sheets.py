"""Labelled contact sheets of the anime Goliath pilot (PIL only). Reads the renders in PREVIEW_OUT, writes docs/mobs/phaseE/goliath_*.png.

    python3 -B tools/gen_anime_goliath.py --preview            (render every scene)
    python3 -B tools/gen_anime_goliath.py --sheets             (compose the sheets from those renders)
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, ".."))
GEO = os.path.join(REPO, "src", "main", "resources", "assets", "nusmp", "geo", "entity")
SCRATCH = os.environ.get("AG_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
SRC = os.path.join(SCRATCH, "previews_anime_goliath")
OUT = os.path.join(REPO, "docs", "mobs", "phaseE")

BG = (24, 24, 30)
NEWC, OLDC = (120, 230, 150), (240, 170, 90)


def font(sz):
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"):
        if os.path.isfile(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()


def quads(geo_file):
    """(cubes, quads per pass) counted from a geo file; one quad per drawn face (faces with no uv entry are not drawn)."""
    d = json.load(open(os.path.join(GEO, geo_file)))
    cubes = q = 0
    for b in d["minecraft:geometry"][0]["bones"]:
        for c in b.get("cubes", []):
            cubes += 1
            uv = c.get("uv")
            q += len(uv) if isinstance(uv, dict) else 6
    return cubes, q


def load(name, crop=None):
    im = Image.open(os.path.join(SRC, name + ".png")).convert("RGB")
    return im.crop(crop) if crop else im


def tiles(name):
    """The view tiles of a vertical preview sheet (title bar 36 px on top, tiles stacked)."""
    im = load(name)
    w, h = im.size
    body = h - 36
    n = max(1, round(body / 560.0))
    th = body // n
    return [im.crop((0, 36 + k * th, w, 36 + (k + 1) * th)) for k in range(n)]


def strip(name):
    """A pose strip without its title bar."""
    im = load(name)
    return im.crop((0, 36, im.size[0], im.size[1]))


def labelled(im, text, colour, sub=None):
    w = im.size[0]
    bar = 44 if sub else 32
    out = Image.new("RGB", (w, im.size[1] + bar), BG)
    out.paste(im, (0, bar))
    d = ImageDraw.Draw(out)
    d.text((8, 5), text, font=font(18), fill=colour)
    if sub:
        d.text((8, 25), sub, font=font(12), fill=(190, 190, 200))
    return out


def row(images, gap=6):
    h = max(i.size[1] for i in images)
    out = Image.new("RGB", (sum(i.size[0] for i in images) + gap * (len(images) - 1), h), BG)
    x = 0
    for i in images:
        out.paste(i, (x, 0))
        x += i.size[0] + gap
    return out


def col(images, gap=6, width=None):
    w = width or max(i.size[0] for i in images)
    ims = []
    for i in images:
        if i.size[0] != w:
            i = i.resize((w, round(i.size[1] * w / i.size[0])), Image.LANCZOS)
        ims.append(i)
    out = Image.new("RGB", (w, sum(i.size[1] for i in ims) + gap * (len(ims) - 1)), BG)
    y = 0
    for i in ims:
        out.paste(i, (0, y))
        y += i.size[1] + gap
    return out


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    p = os.path.join(OUT, name)
    im.quantize(colors=160, method=Image.MEDIANCUT, dither=Image.NONE).save(p, optimize=True)
    print("sheet", os.path.relpath(p, REPO), im.size, os.path.getsize(p) // 1024, "KB")


def make_sheets(names=None):
    cn, qn = quads("anime_goliath.geo.json")
    co, qo = quads("goliath_body.geo.json")
    ca, qa = quads("arc_goliath.geo.json")
    newsub = "NEW anime_goliath: %d cubes, %d quads per pass (boss LOD0 budget 1500-2500)" % (cn, qn)
    # hero views
    cols = []
    for nm in ("front", "q", "back"):
        cols.append(col(tiles("anime_goliath_" + nm)))
    save(labelled(row(cols), "GOLIATH anime cel pilot: front / 3/4 / back / side", NEWC, newsub), "goliath_views.png")
    # head, chest, silhouette
    hd = col(tiles("anime_goliath_head"))
    ch = col(tiles("anime_goliath_chest"))
    far = load("anime_goliath_far", (0, 36, 312, 342))
    far = far.resize((hd.size[0], round(far.size[1] * hd.size[0] / far.size[0])), Image.LANCZOS)
    save(labelled(row([hd, ch, far]), "GOLIATH close-ups: head, chest and fist, and the far read", NEWC, newsub), "goliath_closeups.png")
    # pose strips
    rows = []
    for c in ("idle", "walk", "slam", "stomp"):
        rows.append(labelled(strip("anime_goliath_" + c), "clip: " + c, NEWC))
    save(col(rows, width=1500), "goliath_poses_a.png")
    rows = []
    for c in ("roar", "rise", "death", "transform"):
        rows.append(labelled(strip("anime_goliath_" + c), "clip: " + c, NEWC))
    save(col(rows, width=1500), "goliath_poses_b.png")
    rows = []
    for c in ("hurt", "attack"):
        rows.append(labelled(strip("anime_goliath_" + c), "clip: " + c, NEWC))
    save(col(rows, width=1500), "goliath_poses_c.png")
    # old vs new
    o1 = tiles("anime_goliath_old_front")[0]
    o2 = tiles("anime_goliath_old_arc")[0]
    n1 = tiles("anime_goliath_front")[0]
    o3 = tiles("anime_goliath_old_front")[1]
    n3 = tiles("anime_goliath_front")[1]
    top = row([labelled(o1, "OLD goliath_body (floors)", OLDC, "%d cubes, %d quads per pass" % (co, qo)),
               labelled(o2, "OLD arc_goliath (arc kit)", OLDC, "%d cubes, %d quads per pass" % (ca, qa)),
               labelled(n1, "NEW anime_goliath", NEWC, "%d cubes, %d quads per pass" % (cn, qn))])
    mid = row([labelled(o3, "OLD goliath_body 3/4", OLDC), labelled(n3, "NEW anime_goliath 3/4", NEWC), labelled(load("anime_goliath_far", (0, 36, 312, 342)), "NEW far read (2x distance)", NEWC)])
    sl = col([labelled(strip("anime_goliath_old_slam"), "OLD goliath_body: slam", OLDC), labelled(strip("anime_goliath_slam"), "NEW anime_goliath: slam", NEWC)], width=top.size[0])
    save(col([labelled(top, "GOLIATH OLD vs NEW", (255, 255, 255)), mid, sl], width=top.size[0]), "goliath_old_vs_new.png")
    return 0


if __name__ == "__main__":
    make_sheets(sys.argv[1:])
