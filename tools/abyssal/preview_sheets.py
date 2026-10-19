"""Why: renders the preview scenes of the abyssal looks with the repo's own preview harness (tools/render_preview, the real GeoDraw on a software rasteriser: not the game) and lays the three
phases of each look side by side, so the reviewer can see in one picture what each phase adds. Output: docs/previews/abyssal/<id>_phases.png (the 3/4 view of phase 1, 2 and 3) and, with
--clips, <id>_<clip>.png strips.

    python3 -B tools/abyssal/preview_sheets.py                 # every look
    python3 -B tools/abyssal/preview_sheets.py lich vecna      # only these
    python3 -B tools/abyssal/preview_sheets.py --clips attack cast death lich
Needs JDK 21 and numpy + Pillow (the harness). The harness build folder is $RENDER_PREVIEW_BUILD (default build/render_preview).
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True
from PIL import Image  # noqa: E402
import gen_abyssal as G  # noqa: E402

REPO = G.REPO
OUT = os.path.join(REPO, "docs", "previews", "abyssal")
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(REPO, "build", "render_preview"))
CLIPNAMES = set(G.CLIPS)


def run(scenes):
    env = dict(os.environ, RENDER_PREVIEW_BUILD=BUILD)
    for i in range(0, len(scenes), 24):
        subprocess.run([sys.executable, "-B", os.path.join(REPO, "tools", "render_preview", "preview.py")] + scenes[i:i + 24], cwd=REPO, env=env, check=False, stdout=subprocess.DEVNULL)


def main(argv):
    clips = []
    ids = []
    take_clips = False
    for a in argv:
        if a == "--clips":
            take_clips = True
        elif take_clips and a in CLIPNAMES:
            clips.append(a)
        else:
            ids.append(a)
    ids = ids or G.ORDER
    os.makedirs(OUT, exist_ok=True)
    scenes = []
    for k in ids:
        scenes += ["abyssal_%s_p%d_front" % (k, p) for p in (1, 2, 3)]
        scenes += ["abyssal_%s_%s" % (k, c) for c in clips]
    run(scenes)
    for k in ids:
        tiles = []
        for p in (1, 2, 3):
            path = os.path.join(BUILD, "abyssal_%s_p%d_front.png" % (k, p))
            if not os.path.exists(path):
                continue
            im = Image.open(path).convert("RGB")
            im = im.crop((0, 0, im.width, im.height - 32))                           # the harness may append a one-line budget note: drop the bottom strip
            half = (im.height - 36) // 2
            tiles.append(im.crop((0, 36 + half, im.width, im.height)))             # the 3/4 view (the lower of the two)
        if tiles:
            w = sum(t.width for t in tiles)
            sheet = Image.new("RGB", (w, max(t.height for t in tiles)), (20, 20, 26))
            x = 0
            for t in tiles:
                sheet.paste(t, (x, 0))
                x += t.width
            sheet.thumbnail((1500, 900))
            sheet.save(os.path.join(OUT, "%s_phases.png" % k), optimize=True)
        for c in clips:
            path = os.path.join(BUILD, "abyssal_%s_%s.png" % (k, c))
            if os.path.exists(path):
                im = Image.open(path).convert("RGB")
                im.thumbnail((1500, 900))
                im.save(os.path.join(OUT, "%s_%s.png" % (k, c)), optimize=True)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
