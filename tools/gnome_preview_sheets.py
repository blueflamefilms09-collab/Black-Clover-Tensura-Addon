"""Builds the pictures of the doc (docs/previews/gnome/) from the headless render preview's output, with the real render classes:

    python3 tools/render_preview/preview.py gnome_full gnome_companion_solo gnome_scale gnome_full_rise ...        (RENDER_PREVIEW_BUILD=<dir> keeps the pngs out of the repo)
    python3 -B tools/gnome_preview_sheets.py <dir of the preview pngs>

    gnome_views.png     the full form (front, 3/4, side, back) over the companion (the same four views), each with a caption
    gnome_scale.png     the player, the companion and the giant in one line (side view) and the giant from the front 3/4
    gnome_closeups.png  the face, the chest circle and a fist of the giant, the face and a 3/4 of the companion
    gnome_clips.png     rise, cast, punch, stomp and sink of the giant; walk, cast and punch of the companion
    gnome_lod.png       the three levels of detail of each form side by side
"""
import os
import sys

from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "docs", "previews", "gnome")
HEAD = 36                                                       # the title strip of a preview image
GAP = 6                                                         # between two tiles


def tiles(path, n_views, n_ages=1):
    """The tiles of a preview image: views run down, ages run across. Returns rows[view][age]."""
    im = Image.open(path).convert("RGB")
    w, h = im.size
    th = (h - HEAD - GAP * n_views + GAP) // n_views if False else (h - HEAD) // n_views
    tw = w // n_ages
    rows = []
    for v in range(n_views):
        row = []
        for a in range(n_ages):
            row.append(im.crop((a * tw + (GAP if a else 0), HEAD + v * th, (a + 1) * tw, HEAD + (v + 1) * th - (GAP if v < n_views - 1 else 0))))
        rows.append(row)
    return rows


def caption(im, text):
    d = ImageDraw.Draw(im)
    d.rectangle([0, im.height - 18, min(im.width, 8 + 7 * len(text)), im.height], fill=(20, 20, 26))
    d.text((4, im.height - 15), text, fill=(240, 230, 200))
    return im


def fit_h(im, h):
    return im.resize((max(1, int(im.width * h / im.height)), h), Image.LANCZOS)


def row(images, h, bg=(24, 24, 30)):
    ims = [fit_h(i, h) for i in images]
    out = Image.new("RGB", (sum(i.width for i in ims) + GAP * (len(ims) - 1), h), bg)
    x = 0
    for i in ims:
        out.paste(i, (x, 0))
        x += i.width + GAP
    return out


def stack(rows, bg=(24, 24, 30)):
    w = max(r.width for r in rows)
    out = Image.new("RGB", (w, sum(r.height for r in rows) + GAP * (len(rows) - 1)), bg)
    y = 0
    for r in rows:
        out.paste(r, (0, y))
        y += r.height + GAP
    return out


def save(im, path):
    """A palette png (256 colours): the pictures are flat-shaded renders, and the repo does not need megabytes of them."""
    im.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)


def main():
    src = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    names = ["front", "3/4", "side", "back"]
    full = tiles(os.path.join(src, "gnome_full.png"), 4)
    comp = tiles(os.path.join(src, "gnome_companion_solo.png"), 4)
    r1 = row([caption(full[i][0].copy(), "full form: " + names[i]) for i in range(4)], 520)
    r2 = row([caption(comp[i][0].copy(), "companion: " + names[i]) for i in range(4)], 520)
    save(stack([r1, r2]), os.path.join(OUT, "gnome_views.png"))

    sc = tiles(os.path.join(src, "gnome_scale.png"), 2)
    save(row([caption(sc[0][0].copy(), "side: the player, the companion, the giant"), caption(sc[1][0].copy(), "the giant from the front 3/4")], 560), os.path.join(OUT, "gnome_scale.png"))

    fc = tiles(os.path.join(src, "gnome_full_close.png"), 3)
    cc = tiles(os.path.join(src, "gnome_companion_close.png"), 2)
    save(row([caption(fc[0][0].copy(), "giant: face"), caption(fc[1][0].copy(), "giant: chest circle"), caption(fc[2][0].copy(), "giant: fist"),
         caption(cc[0][0].copy(), "companion: face"), caption(cc[1][0].copy(), "companion: 3/4")], 420), os.path.join(OUT, "gnome_closeups.png"))

    lines = []
    for scene, ages, label in (("gnome_full_rise", 6, "giant: rise (ticks 0 6 11 14 17 22)"), ("gnome_full_cast", 6, "giant: cast, both fists slam (0 5 9 12 15 20)"),
                               ("gnome_full_punch", 5, "giant: punch (0 5 10 13 17)"), ("gnome_full_stomp", 5, "giant: stomp (0 4 8 10 14)"), ("gnome_full_sink", 5, "giant: sink (0 5 10 15 19)"),
                               ("gnome_companion_walk", 4, "companion: walk (0 8 16 24)"), ("gnome_companion_cast", 6, "companion: cast (0 5 9 12 15 20)"), ("gnome_companion_punch", 5, "companion: punch (0 5 10 13 17)")):
        t = tiles(os.path.join(src, scene + ".png"), 1, ages)[0]
        r = row(t, 300 if "full" in scene else 220)
        lines.append(caption(r, label))
    save(stack(lines), os.path.join(OUT, "gnome_clips.png"))

    lods = []
    for form in ("full", "companion"):
        parts = [tiles(os.path.join(src, "gnome_%s.png" % form), 4)[1][0]]
        parts += [tiles(os.path.join(src, "gnome_%s_lod%d.png" % (form, l)), 1)[0][0] for l in (1, 2)]
        lods.append(row([caption(parts[i].copy(), "%s: level %d" % (form, i)) for i in range(3)], 420))
    save(stack(lods), os.path.join(OUT, "gnome_lod.png"))
    print("wrote", sorted(os.listdir(OUT)))


if __name__ == "__main__":
    main()
