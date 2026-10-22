#!/usr/bin/env python3
"""
Writes the preview sheets of the rebuilt rooms (docs/previews/dungeon_rooms/, needs Pillow and numpy):

    python3 tools/labyrinth/rr_previews.py                 # every sheet
    python3 tools/labyrinth/rr_previews.py hall/library_a  # the sheets of the shipped templates whose name contains this text

One sheet per room: the shipped room on the left and a rebuilt variant on the right, three rows: an isometric cutaway (near walls and roof off), the floor plan (the highest block
of every column up to the height of a person plus a hand, so walls, pillars, props and the floor pattern show), and a first-person view from the first door. They come from the same
shape-aware renderer as the other dungeon previews (flat colours, not game textures), so they show shape, rhythm, light and clutter, not the look of a texture pack.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image, ImageDraw  # noqa: E402
import mega_lib as L  # noqa: E402
import mega_shape_preview as SP  # noqa: E402
import rr_core as C  # noqa: E402
import rr_view as V  # noqa: E402

OUT = os.path.join(L.ROOT, "docs", "previews", "dungeon_rooms")
#: (shipped template, variant, caption): a spread of room types, hall types and hallways
SHEETS = [
    ("mega/hall/library_a", 2, "Library: barrel vault, galleries with shelves, runner"),
    ("mega/hall/armory_forge_a", 1, "Armoury: groin vault on pillars, forge niches"),
    ("mega/hall/barracks_a", 3, "Barracks: low coffered roof, heavier wear"),
    ("mega/hall/shrine_a", 3, "Shrine: dome, inlaid floor, shrine niches"),
    ("mega/hall/crypt_a", 1, "Crypt: groin vault, tomb niches, diamond floor"),
    ("mega/hall/great_hall", 4, "Great hall: gable roof, pillars, mid course"),
    ("mega/hall/holy_church_nave", 2, "Church nave: barrel vault, galleries"),
    ("mega/treasure/closet_a", 1, "Treasure closet with the armour stand slot"),
    ("mega/junction/plaza_2x2_a", 1, "Plaza: groin vault, door crowns"),
    ("mega/corridor/gallery_2x4_a", 2, "Gallery"),
    ("mega/corridor/straight_1x3_a", 1, "Hallway: barrel vault, pilaster strips"),
    ("mega/corridor/straight_1x3_b", 3, "Hallway: timber gable, bookcase panels"),
    ("mega/corridor/straight_1x4_c", 4, "Hallway ruin: coffered roof, wear, breach"),
    ("mega/corridor/cross_1x1_a", 2, "Crossing: groin vault"),
    ("mega/corridor/bend_1x1_b", 1, "Bend: dome crown"),
]


def ceil_of(name):
    r = C.Room(name, 0)
    return r.ceil if r.ceil else 20


def plan_img(t, scale):
    W, H, D = t.size
    im = Image.new("RGB", (1, 1))
    path = os.path.join(OUT, "_tmp_plan.png")
    SP.render(t, path, (0, W - 1), (8, 11), (0, D - 1), scale, top=True, light=False, markers=True, lit=False, rot=0)
    im = Image.open(path).convert("RGB")
    return im


def iso_img(t, ytop, scale):
    path = os.path.join(OUT, "_tmp_iso.png")
    V.cutaway(t, path, scale=scale, ytop=ytop)
    return Image.open(path).convert("RGB")


def fp_img(t, door_cam=None):
    W, H, D = t.size
    jig = t.jigsaws()
    cam, look = (W / 2.0, 11.0, D - 4.0), (W / 2.0, 12.5, 0.0)
    if jig:
        (x, y, z), orient, _ = jig[0]
        face = orient.split("_")[0]
        step = {"north": (0, 1), "south": (0, -1), "west": (1, 0), "east": (-1, 0)}[face]
        cam = (x + 0.5 + 3 * step[0], 11.0, z + 0.5 + 3 * step[1])
        look = (x + 0.5 + 14 * step[0], 12.5, z + 0.5 + 14 * step[1])
    path = os.path.join(OUT, "_tmp_fp.png")
    V.firstperson(t, path, cam, look, 85.0, (560, 315), key=("fp", id(t)))
    return Image.open(path).convert("RGB")


def fit(im, w, h):
    im = im.copy()
    im.thumbnail((w, h))
    out = Image.new("RGB", (w, h), (20, 20, 26))
    out.paste(im, ((w - im.width) // 2, (h - im.height) // 2))
    return out


def sheet(name, variant, caption):
    stem = name.split("/")[-1]
    parts = name.split("/")
    after_name = "mega_r/%s/%s_v%d" % (parts[1], parts[2], variant)
    before, after = V.open_template(name), V.open_template(after_name)
    ytop = ceil_of(name) - 1
    scale = 7 if max(before.size[0], before.size[2]) > 20 else 10
    colw = 640
    rows = [("isometric cutaway", fit(iso_img(before, ytop, scale), colw, 340), fit(iso_img(after, ytop, scale), colw, 340)),
            ("floor plan", fit(plan_img(before, 12), colw, 300), fit(plan_img(after, 12), colw, 300)),
            ("first person from the first door", fit(fp_img(before), colw, 315), fit(fp_img(after), colw, 315))]
    head = 52
    H = head + sum(r[1].height + 22 for r in rows)
    img = Image.new("RGB", (colw * 2 + 30, H), (20, 20, 26))
    d = ImageDraw.Draw(img)
    d.text((10, 8), "%s -> variant %d: %s" % (name, variant, caption), fill=(235, 230, 200))
    d.text((10, 28), "left: shipped room   right: rebuilt room (same size, doors and markers)", fill=(150, 150, 160))
    y = head
    for label, a, b in rows:
        d.text((10, y), label, fill=(170, 170, 185))
        y += 18
        img.paste(a, (10, y))
        img.paste(b, (colw + 20, y))
        y += a.height + 4
    out = os.path.join(OUT, "%s_%s_v%d.png" % (parts[1], stem, variant))
    img.save(out, optimize=True)
    return out


def main(argv):
    os.makedirs(OUT, exist_ok=True)
    only = [a for a in argv if not a.startswith("--")]
    for name, variant, caption in SHEETS:
        if only and not any(o in name for o in only):
            continue
        print("wrote", sheet(name, variant, caption))
    for tmp in ("_tmp_plan.png", "_tmp_iso.png", "_tmp_fp.png"):
        p = os.path.join(OUT, tmp)
        if os.path.exists(p):
            os.remove(p)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
