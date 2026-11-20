"""Comparison sheets for the Goku and Sinbad boss looks (docs/boss_showcase/compare_*.png): the owner's reference image on the left, the software render of the look (front / three-quarter / side / back)
on the right, one sheet per look, plus one overview per boss.

    python3 -B tools/bc_playermodels/compare_gs.py [--out DIR]

The reference images are in docs/boss_showcase/reference/ (ref_goku.png, ref_sinbad_base.png, ref_sinbad_baal.png, ref_sinbad_furfur.png, ref_sinbad_zepar.png). Which image stands for which look is the
MAPPING below; Valefor, Focalor, Vepar and Crocell have no reference and are drawn with a "no reference" panel (they follow the same design language in their own table colours).
Software renders, not the game (see preview_gs.py).
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

from PIL import Image, ImageDraw  # noqa: E402

import preview_bc as B  # noqa: E402

LOOKS_GS = os.path.join(B.L.ASSETS, "playermodel", "looks_gs.json")
DOCS = os.path.join(B.ROOT, "docs", "boss_showcase")
REF = os.path.join(DOCS, "reference")

# look id -> (reference file or None, caption of the reference)
MAPPING = {
    "gs_sinbad_base": ("ref_sinbad_base.png", "reference image 12: the king"),
    "gs_sinbad_baal": ("ref_sinbad_baal.png", "reference image 13: Baal (blue feather and scale armour, antler diadem)"),
    "gs_sinbad_furfur": ("ref_sinbad_furfur.png", "reference image 14: Furfur (purple and orange, feather fan, ring of light)"),
    "gs_sinbad_zepar": ("ref_sinbad_zepar.png", "reference image 15: Zepar (red, gold sun buckle, ring of light)"),
    "gs_sinbad_valefor": (None, "no reference sent: built from the same design language in Valefor's table colour (violet)"),
    "gs_sinbad_focalor": (None, "no reference sent: built from the same design language in Focalor's table colour (green)"),
    "gs_sinbad_vepar": (None, "no reference sent: built from the same design language in Vepar's table colour (teal)"),
    "gs_sinbad_crocell": (None, "no reference sent: built from the same design language in Crocell's table colour (pale blue)"),
}
GOKU_REF = ("ref_goku.png", "reference image 16: the Blockbench tutorial Goku (anime on the left, the pixel skin on the right)")
BG = (232, 236, 242)
H_ = 440


def _ref_panel(fname, caption, crop=None):
    d = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    if fname is None:
        im = Image.new("RGB", (300, H_), (210, 214, 224))
        dr = ImageDraw.Draw(im)
        dr.text((16, H_ // 2 - 6), "no reference image", fill=(90, 94, 110))
        return im
    im = Image.open(os.path.join(REF, fname)).convert("RGB")
    if crop:
        im = im.crop(crop)
    w = int(im.width * (H_ / im.height))
    return im.resize((w, H_), Image.LANCZOS)


def _wrap(text, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        if len(cur) + len(w) + 1 > width:
            lines.append(cur)
            cur = w
        else:
            cur = (cur + " " + w).strip()
    lines.append(cur)
    return lines


def pair(lid, look, fname, caption, crop=None):
    sheet = B.sheet(lid, look).convert("RGB")
    sw = int(sheet.width * 0.74)
    sheet = sheet.resize((sw, int(sheet.height * 0.74)), Image.LANCZOS)
    ref = _ref_panel(fname, caption, crop)
    top = 54
    out = Image.new("RGB", (ref.width + sheet.width + 18, max(H_, sheet.height) + top + 8), BG)
    d = ImageDraw.Draw(out)
    d.text((8, 6), "%s    left: %s" % (lid, caption), fill=(20, 20, 20))
    d.text((8, 22), "right: software render of the look on the player model (front, three-quarter, side, back); not the game", fill=(70, 70, 80))
    out.paste(ref, (6, top))
    out.paste(sheet, (ref.width + 12, top))
    return out


def stack(images, title):
    w = max(i.width for i in images)
    h = sum(i.height for i in images) + 28
    out = Image.new("RGB", (w, h), BG)
    ImageDraw.Draw(out).text((8, 8), title, fill=(20, 20, 20))
    y = 28
    for i in images:
        out.paste(i, (0, y))
        y += i.height
    return out


def main(argv):
    out_dir = DOCS
    if "--out" in argv:
        out_dir = argv[argv.index("--out") + 1]
    os.makedirs(out_dir, exist_ok=True)
    looks = json.load(open(LOOKS_GS))["looks"]
    goku_crop = (40, 96, 1250, 690)                                   # the frame without the title bar
    goku, sinbad = [], []
    for lid, look in looks.items():
        if lid.startswith("gs_goku"):
            im = pair(lid, look, GOKU_REF[0], GOKU_REF[1], goku_crop)
            goku.append(im)
        elif lid in MAPPING:
            im = pair(lid, look, *MAPPING[lid])
            sinbad.append(im)
        else:
            continue
        p = os.path.join(out_dir, "compare_" + lid[3:] + ".png")
        im.save(p)
        print(p)
    for name, ims, title in (("goku", goku, "Goku: the reference (images/16) next to each of the 8 looks"), ("sinbad", sinbad, "Sinbad: the references (images 12 to 15) next to the base look and the 7 djinn bodies")):
        if ims:
            p = os.path.join(out_dir, "compare_%s_all.png" % name)
            stack(ims, title).save(p)
            print(p)


if __name__ == "__main__":
    main(sys.argv[1:])
