"""Builds docs/previews/sylph/sylph_poses.png (every clip of both forms, one tile each, labelled) and docs/previews/sylph/sylph_faces.png (every face, close up, labelled) from the headless renders of the
scenes pose_small_<clip>, pose_powered_<clip> and face_<kind> (tools/sylph_scenes.py, tools/render_preview).

    python3 -B tools/sylph_pose_sheets.py [<render dir>]

<render dir> defaults to $RENDER_PREVIEW_BUILD or build/render_preview. Run python as `python3 -B`.
"""
import os
import sys

from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import sylph_scenes as S  # noqa: E402

OUT = os.path.join(HERE, "..", "docs", "previews", "sylph")
BG = (22, 22, 28)


def tile(build, scene, label, size):
    p = os.path.join(build, scene + ".png")
    img = Image.open(p).convert("RGB")
    w, h = img.size
    img = img.crop((6, 36, w - 6, h - 36 if h > 120 else h)) if False else img            # the preview adds a title bar and a footer: keep the picture area
    # the preview image is: title bar (about 30 px), the tile, a warning footer (varies); the tile is the part with the frame inside, find it by its size
    tw, th = S_TILE.get(scene, (None, None))
    if tw:
        img = img.crop((6, 36, 6 + tw, 36 + th))
    img = img.resize(size, Image.LANCZOS)
    d = ImageDraw.Draw(img)
    d.rectangle((0, size[1] - 20, size[0], size[1]), fill=(0, 0, 0))
    d.text((6, size[1] - 17), label, fill=(240, 240, 240))
    return img


S_TILE = {}


def sheet(build, names, cols, size, path, title):
    rows = (len(names) + cols - 1) // cols
    out = Image.new("RGB", (cols * (size[0] + 6) + 6, rows * (size[1] + 6) + 36), BG)
    ImageDraw.Draw(out).text((8, 10), title, fill=(235, 235, 235))
    for i, (scene, label) in enumerate(names):
        t = tile(build, scene, label, size)
        out.paste(t, (6 + (i % cols) * (size[0] + 6), 36 + (i // cols) * (size[1] + 6)))
    out.save(path, optimize=True)
    print("wrote", path, out.size)


def main():
    build = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(HERE, "..", "build", "render_preview")
    import json
    scenes = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "sylph.json")))
    for k, v in scenes.items():
        if isinstance(v, dict) and "tile" in v and k.startswith(("pose_", "face_")):
            S_TILE[k] = tuple(v["tile"])
    os.makedirs(OUT, exist_ok=True)
    small = [("pose_small_" + c, "small: " + c) for c in S.SMALL_POSES]
    powered = [("pose_powered_" + c, "powered: " + c) for c in S.POWERED_POSES]
    sheet(build, small + powered, 6, (300, 333), os.path.join(OUT, "sylph_poses.png"),
          "Sylph, every pose (one moment of each looping clip; the clip is chosen by what happens: sylph/SylphMood). Headless render, not a game screenshot.")
    sheet(build, [("face_" + f, f) for f, _c, _f in S.FACES], 5, (240, 240), os.path.join(OUT, "sylph_faces.png"),
          "Sylph, every face plate (close up; one plate is shown per clip, a swap of the plate, no extra geometry)")


if __name__ == "__main__":
    main()
