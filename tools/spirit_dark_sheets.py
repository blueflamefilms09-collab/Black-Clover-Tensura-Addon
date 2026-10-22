"""Joins the preview renders of the Spirit of Darkness into the two sheets of the doc (docs/previews/spirit_dark/spirit_dark_poses.png and spirit_dark_faces.png).

    python3 -B tools/render_preview/preview.py <the sdp_* and sdf_* scenes>      (see tools/spirit_dark_scenes.py)
    python3 -B tools/spirit_dark_sheets.py [render_dir]

render_dir defaults to $RENDER_PREVIEW_BUILD or build/render_preview. Each render is a title strip plus one picture per age; the sheets crop the title and join one picture per pose or mood.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "docs", "previews", "spirit_dark")
sys.path.insert(0, HERE)
import spirit_dark_scenes as S  # noqa: E402


def tile(path):
    im = Image.open(path).convert("RGB")
    return im.crop((0, 30, im.width, im.height))                    # the scene's own title strip is cut off


def sheet(rows, out, cols, title):
    cells = []
    for label, path in rows:
        if os.path.exists(path):
            cells.append((label, tile(path)))
    if not cells:
        print("nothing to join for", out)
        return
    cw, ch = max(c[1].width for c in cells), max(c[1].height for c in cells)
    n = len(cells)
    r = (n + cols - 1) // cols
    img = Image.new("RGB", (cols * cw, r * (ch + 22) + 26), (18, 14, 28))
    d = ImageDraw.Draw(img)
    d.text((8, 6), title, fill=(220, 200, 255))
    for i, (label, im) in enumerate(cells):
        x, y = (i % cols) * cw, 26 + (i // cols) * (ch + 22)
        img.paste(im, (x, y + 22))
        d.text((x + 8, y + 5), label, fill=(190, 170, 230))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out, optimize=True)
    print("wrote", out, img.size)


def main():
    rd = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview")
    for form, poses, cols in (("powered", S.POWERED_POSES, 5), ("companion", S.COMPANION_POSES, 5)):
        sheet([(p, os.path.join(rd, "sdp_%s_%s.png" % (form, p))) for p in poses], os.path.join(OUT, "spirit_dark_poses_%s.png" % form), cols, "Spirit of Darkness, %s form: poses (preview, not seen in a game)" % form)
    rows = []
    for form in ("powered", "companion"):
        rows += [("%s: %s" % (form, m), os.path.join(rd, "sdf_%s_%s.png" % (form, m))) for m in S.MOODS]
    sheet(rows, os.path.join(OUT, "spirit_dark_faces.png"), 7, "Spirit of Darkness: the seven faces, powered (top) and companion (bottom)")
    # one file with both forms' poses, as the doc names it
    a, b = Image.open(os.path.join(OUT, "spirit_dark_poses_powered.png")), Image.open(os.path.join(OUT, "spirit_dark_poses_companion.png"))
    w = max(a.width, b.width)
    both = Image.new("RGB", (w, a.height + b.height), (18, 14, 28))
    both.paste(a, (0, 0))
    both.paste(b, (0, a.height))
    both.save(os.path.join(OUT, "spirit_dark_poses.png"), optimize=True)
    print("wrote spirit_dark_poses.png", both.size)
    # the two portraits and the levels of detail
    for scene, name in (("sd_powered_game", "powered_demon.png"), ("sd_companion_game", "companion_wisp.png")):
        src = os.path.join(rd, scene + ".png")
        if os.path.exists(src):
            Image.open(src).convert("RGB").save(os.path.join(OUT, name), optimize=True)
            print("wrote", name)
    sheet([("powered, full", os.path.join(rd, "sd_powered_battle.png")), ("powered, level 1", os.path.join(rd, "sd_powered_lod.png")), ("powered, level 2", os.path.join(rd, "sd_powered_lod2.png")),
           ("companion, full", os.path.join(rd, "sd_companion_full.png")), ("companion, level 1", os.path.join(rd, "sd_companion_lod.png")), ("companion, level 2", os.path.join(rd, "sd_companion_lod2.png"))],
          os.path.join(OUT, "lods.png"), 3, "Spirit of Darkness: the three levels of detail of each form (quads: see docs/magic/spirit_dark.md)")


if __name__ == "__main__":
    main()
