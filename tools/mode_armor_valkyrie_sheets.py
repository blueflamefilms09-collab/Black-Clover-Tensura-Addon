"""Composes the preview sheets of the Valkyrie armour v2 from the harness renders (scenes.d/mode_armor_valkyrie.json): BEFORE (the old overlay, the setting CLASSIC) | NEW (the real layer classes).

    python3 -B tools/mode_armor_valkyrie_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py vg2_before_full vg2_after_full ...      (every scene of the json)
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/mode_armor_valkyrie_sheets.py                                      # writes docs/previews/vfx_remake/valkyrie/ingame_*.png

Every picture is a render of the repo's software harness (the real ModeArmorLayer and ModeArmorValkyrieV2, a stub world), NOT a game screenshot: no bloom, additive passes approximated, the arms hang, the stand-in
is the harness' block player.
"""
import json
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
SCENES = json.load(open(os.path.join(HERE, "render_preview", "scenes.d", "mode_armor_valkyrie.json")))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie")
MARGIN, HEAD = 6, 30


def font(n=14):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", n)
    except OSError:
        return ImageFont.load_default()


def tiles(scene):
    sc = SCENES[scene]
    im = Image.open(os.path.join(BUILD, scene + ".png")).convert("RGB")
    w, h = sc["tile"]
    rows, cols = len(sc["views"]), len(sc["ages"])
    return {(r, c): im.crop((MARGIN + c * (w + MARGIN), HEAD + MARGIN + r * (h + MARGIN), MARGIN + c * (w + MARGIN) + w, HEAD + MARGIN + r * (h + MARGIN) + h)) for r in range(rows) for c in range(cols)}


def label(im, text, xy=(6, 5), size=14):
    d = ImageDraw.Draw(im, "RGBA")
    f = font(size)
    box = d.textbbox(xy, text, font=f)
    d.rectangle((box[0] - 3, box[1] - 2, box[2] + 3, box[3] + 2), fill=(0, 0, 0, 170))
    d.text(xy, text, font=f, fill=(255, 245, 200))


def col_of(scene, tag, views=None, col=0):
    t = tiles(scene)
    ims = []
    for r, v in enumerate(SCENES[scene]["views"]):
        if views and v["name"] not in views:
            continue
        im = t[(r, col)].copy()
        label(im, "%s: %s" % (tag, v["name"]))
        ims.append(im)
    return ims


def row_of(scene, tag, col_range=None):
    t = tiles(scene)
    sc = SCENES[scene]
    ims = []
    for c in range(len(sc["ages"])):
        im = t[(0, c)].copy()
        label(im, "%s: tick %d" % (tag, sc["ages"][c]))
        ims.append(im)
    return ims


def hstack(ims, gap=4):
    out = Image.new("RGB", (sum(i.width for i in ims) + gap * (len(ims) - 1), max(i.height for i in ims)), (14, 14, 18))
    x = 0
    for i in ims:
        out.paste(i, (x, 0))
        x += i.width + gap
    return out


def vstack(ims, gap=6, title=None):
    out = Image.new("RGB", (max(i.width for i in ims), sum(i.height for i in ims) + gap * (len(ims) - 1) + (34 if title else 0)), (14, 14, 18))
    y = 0
    if title:
        ImageDraw.Draw(out).text((8, 8), title, font=font(18), fill=(235, 255, 235))
        y = 34
    for i in ims:
        out.paste(i, (0, y))
        y += i.height + gap
    return out


def sheet(name, title, pairs, views):
    """pairs: [(scene, tag)] side by side per view row: BEFORE | NEW ..."""
    cols = [col_of(s, tag, views) for s, tag in pairs]
    rows = [hstack([c[i] for c in cols]) for i in range(len(cols[0]))]
    vstack(rows, title=title).save(os.path.join(OUT, name))
    print("wrote", name)


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet("ingame_full.png", "Valkyrie Dress: BEFORE (old overlay, valkyrieLook = CLASSIC) | NEW (valkyrieLook = V2, lance attached): the real ModeArmorLayer + ModeArmorValkyrieV2, harness render, not a game screenshot",
          [("vg2_before_full", "BEFORE"), ("vg2_after_full", "NEW")], ("front", "front 3/4", "back 3/4", "side"))
    sheet("ingame_wings.png", "Wings from behind and the side: BEFORE | NEW", [("vg2_before_wings", "BEFORE"), ("vg2_after_wings", "NEW")], ("back", "back 3/4", "side"))
    sheet("ingame_upper.png", "Upper body close up (tiara, bodice, shoulders, cuffs, sleeves): BEFORE | NEW", [("vg2_before_upper", "BEFORE"), ("vg2_after_upper", "NEW")], ("front", "front 3/4", "side"))
    sheet("ingame_saint.png", "SAINT look (valkyrieForm = SAINT): BEFORE (old overlay) | NEW", [("vg2_before_full", "BEFORE"), ("vg2_saint_full", "SAINT")], ("front 3/4", "back 3/4", "side"))
    sheet("ingame_mermaid.png", "MERMAID look (valkyrieForm = MERMAID, the vanilla legs hidden by the event in the game; the harness draws the stand-in's legs behind the tail): BEFORE | NEW",
          [("vg2_before_full", "BEFORE"), ("vg2_mermaid_full", "MERMAID")], ("front 3/4", "back 3/4", "side"))
    sheet("ingame_lance_modes.png", "The lance: OFF | ATTACHED | FLOATING (valkyrieLance)", [("vg2_nolance_full", "OFF"), ("vg2_after_full", "ATTACHED"), ("vg2_float_full", "FLOATING")], ("front 3/4", "side"))
    sheet("ingame_run.png", "Running (limb swing 0.9: the wings beat harder): BEFORE | NEW", [("vg2_before_run", "BEFORE"), ("vg2_after_run", "NEW")], ("front 3/4", "back 3/4", "side"))
    rows = [hstack(row_of("vg2_before_ages", "BEFORE")), hstack(row_of("vg2_after_ages", "NEW")), hstack(row_of("vg2_saint_ages", "SAINT")), hstack(row_of("vg2_mermaid_ages", "MERMAID"))]
    vstack(rows, title="Grow-in over the first ticks (the mode starts at tick 0, full at tick 6) and the wing flutter over time: BEFORE | NEW | SAINT | MERMAID").save(os.path.join(OUT, "ingame_ages.png"))
    print("wrote ingame_ages.png")


if __name__ == "__main__":
    main()
