#!/usr/bin/env python3
"""Great Spirit GUI redesign, phase 1: renders the BEFORE sheet, three directions (A Holo Grimoire, B Parchment & Ink, C Slime Crest) as pixel mock-ups at
two GUI scales, the per-direction sheets, the compare sheet and the texture budget. SOFTWARE MOCK-UPS, NOT THE GAME.

    python3 tools/gui_redesign/make_gui_redesign.py [--out docs/previews/gui_redesign] [--only before|A|B|C|compare|budget]

The vanilla font atlas (ascii.png) is fetched into build/gui_redesign/ once and never added to the repository. Needs Pillow and numpy; the BEFORE HUD
cell also runs tools/system/preview_overlay.py (a JDK). Window: 1280 x 720; GUI scale 2 = 640 x 360 GUI pixels, scale 3 = 427 x 240.
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from PIL import Image, ImageDraw, ImageFont  # noqa: E402

import gs_before as BEF  # noqa: E402
import gs_sprites as SP  # noqa: E402
from gs_book import alert_scene, book_scene, clover_scene, hover_scene  # noqa: E402
from gs_kit import ROOT  # noqa: E402
from gs_scenes import Kit, gauge_variants, hud_scene  # noqa: E402

DIRS = {"A": "a_holo_grimoire", "B": "b_parchment_ink", "C": "c_slime_crest"}
LABEL_FONT = "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf"
BODY_FONT = "/usr/share/fonts/opentype/inter/Inter-Regular.otf"
BG = (18, 20, 28)


def fnt(size, bold=False):
    return ImageFont.truetype(LABEL_FONT if bold else BODY_FONT, size)


def fit(im, w):
    if im.width == w:
        return im
    return im.resize((w, max(1, round(im.height * w / im.width))), Image.LANCZOS)


def captioned(im, caption, w=None, sub=None):
    if w:
        im = fit(im.convert("RGB"), w)
    cap_h = 26 if not sub else 40
    out = Image.new("RGB", (im.width, im.height + cap_h), BG)
    out.paste(im.convert("RGB"), (0, cap_h))
    d = ImageDraw.Draw(out)
    d.text((6, 4), caption, font=fnt(15, True), fill=(235, 238, 245))
    if sub:
        d.text((6, 23), sub, font=fnt(11), fill=(150, 160, 180))
    return out


def hstack(ims, gap=14, pad=14, bg=BG):
    w = sum(i.width for i in ims) + gap * (len(ims) - 1) + 2 * pad
    h = max(i.height for i in ims) + 2 * pad
    out = Image.new("RGB", (w, h), bg)
    x = pad
    for i in ims:
        out.paste(i, (x, pad))
        x += i.width + gap
    return out


def vstack(ims, gap=14, pad=0, bg=BG):
    w = max(i.width for i in ims) + 2 * pad
    h = sum(i.height for i in ims) + gap * (len(ims) - 1) + 2 * pad
    out = Image.new("RGB", (w, h), bg)
    y = pad
    for i in ims:
        out.paste(i, (pad, y))
        y += i.height + gap
    return out


def swatches(th, w):
    im = Image.new("RGB", (w, 26), BG)
    d = ImageDraw.Draw(im)
    cols = [th.neon, th.neon2, th.glowc, th.metal_hi, th.jewel, th.core_a, th.core_b, th.ink if th.key != "B" else th.cover, th.data, th.accent]
    for i, c in enumerate(cols):
        d.rectangle([i * 30, 2, i * 30 + 26, 24], fill=tuple(c[:3]), outline=(70, 74, 90))
    return im


# ------------------------------------------------------------------------------------------------------ renders
def render_direction(key, out):
    th = SP.THEMES[key]
    K = Kit(th)
    d = os.path.join(out, DIRS[key])
    os.makedirs(d, exist_ok=True)
    R = {}
    for s in (2, 3):
        R["hud%d" % s] = hud_scene(K, s).img.convert("RGB")
        R["gauges%d" % s] = gauge_variants(K, s).img.convert("RGB")
        R["book0_%d" % s] = book_scene(K, s, 0.0, t=20).img.convert("RGB")
        R["book50_%d" % s] = book_scene(K, s, 0.5, t=60).img.convert("RGB")
        R["book100_%d" % s] = book_scene(K, s, 1.0, t=100).img.convert("RGB")
        R["flip%d" % s] = book_scene(K, s, 1.0, t=100, flip=1.15, status="Page 1 > 2").img.convert("RGB")
        R["clover%d" % s] = clover_scene(K, s).img.convert("RGB")
        R["alert%d" % s] = alert_scene(K, s).img.convert("RGB")
        R["hover%d" % s] = hover_scene(K, s).img.convert("RGB")
    # textures as the mod would ship them (previews of the PNGs): with real byte sizes
    tdir = os.path.join(d, "textures")
    os.makedirs(tdir, exist_ok=True)
    sizes = {}
    for name, im in SP.TEXTURES.items():
        if not name.startswith("gui/gs_%s/" % key.lower()):
            continue
        p = os.path.join(tdir, os.path.basename(name) + ".png")
        im.save(p, optimize=True)
        sizes[name] = (im.width, im.height, os.path.getsize(p))
    # the sheet
    head = Image.new("RGB", (2300, 70), BG)
    dd = ImageDraw.Draw(head)
    dd.text((10, 6), "Direction %s: %s" % (key, th.name), font=fnt(28, True), fill=(240, 244, 252))
    dd.text((10, 42), "%s.  Software mock-up from generated textures, NOT the game. Window 1280x720; GUI scale 2 = 640x360 GUI px, scale 3 = 427x240." % th.sub, font=fnt(13), fill=(150, 160, 180))
    head.paste(swatches(th, 320), (1960, 20))
    crop = lambda im: im.crop((0, 380, 720, 720))
    row_hud = hstack([captioned(crop(R["hud3"]), "1  HUD, GUI scale 3", sub="frame (neon line + filigree corners) around the concentric gauge: outer lane Mana, inner core Magicules"),
                      captioned(R["hud2"].crop((0, 380, 720, 720)), "HUD, GUI scale 2 (same window region)", sub="the plate is the same size in GUI pixels, so it is smaller on a big GUI scale"),
                      captioned(fit(R["gauges3"], 840), "Gauge, four elements, four fill levels (scale 3)")])
    row_book = hstack([captioned(R["book0_2"], "2a  Grimoire menu, frame 1 (ink script), scale 2", w=720),
                       captioned(R["book50_2"], "frame 2: compile line passes, ink turns into matrix data", w=720),
                       captioned(R["book100_2"], "frame 3: compiled, 'Analysis complete.'", w=720)])
    row_book3 = hstack([captioned(R["book50_3"], "Grimoire menu at GUI scale 3 (mid compile)", w=1080), captioned(R["flip2"], "page flip mid-turn (existing 450 ms leaf, restyled), scale 2", w=720)])
    row_cl = hstack([captioned(R["clover3"], "3  Clover icon set, GUI scale 3", w=1260), captioned(R["clover2"], "scale 2", w=840)])
    row_al = hstack([captioned(R["alert3"], "4  Alert widget (crest + two notices), scale 3", w=900), captioned(R["alert2"], "scale 2", w=600), captioned(R["hover3"], "Hover ripple + click snap, scale 3", w=756)])
    sheet = vstack([head, row_hud, row_book, row_book3, row_cl, row_al], gap=6)
    sheet.save(os.path.join(out, "gui_dir_%s_%s.png" % (key, DIRS[key].split("_", 1)[1])), optimize=True)
    return R, sizes


def render_before(out):
    R = {}
    for s in (2, 3):
        R["hud%d" % s] = BEF.before_hud(s).img.convert("RGB")
        R["book%d" % s] = BEF.harness_screen("four_kingdoms", s)
        R["codex%d" % s] = BEF.harness_screen("codex", s)
        R["status%d" % s] = BEF.harness_screen("status", s)
        R["toast%d" % s] = BEF.harness_screen("toast", s)
        R["clover%d" % s] = BEF.before_clover(s).img.convert("RGB")
        R["hover%d" % s] = BEF.before_button(s, 0).img.convert("RGB")
    head = Image.new("RGB", (2300, 56), BG)
    dd = ImageDraw.Draw(head)
    dd.text((10, 6), "BEFORE: the GUI the mod ships today (theme 'anime', the default since 1.27.0)", font=fnt(24, True), fill=(240, 244, 252))
    dd.text((10, 36), "Harness mock-ups (tools/ui_preview, tools/system) with the vanilla font; the classic theme is the older flat parchment look and is not shown. NOT the game.", font=fnt(13), fill=(150, 160, 180))
    row1 = hstack([captioned(R["hud3"].crop((0, 380, 1280, 720)), "HUD today, scale 3 (bottom: vanilla only; top left: dungeon System window)", w=900), captioned(R["hud2"], "scale 2", w=620)])
    row2 = hstack([captioned(R["book3"], "Four Kingdoms grimoire menu, scale 3", w=860), captioned(R["book2"], "scale 2", w=860)])
    row3 = hstack([captioned(R["codex3"], "Codex (book) and page-flip, scale 3", w=860), captioned(R["status3"], "Multiverse Status, scale 3", w=860)])
    row4 = hstack([captioned(R["clover3"], "Skill / clover icons today, scale 3", w=1000), captioned(R["toast3"], "Page-unlock toast (notice), scale 3", w=720), captioned(R["hover3"], "Button idle / hover, scale 3", w=520)])
    sheet = vstack([head, row1, row2, row3, row4], gap=6)
    sheet.save(os.path.join(out, "gui_before.png"), optimize=True)
    return R


def render_compare(out, before, dirs):
    W = 640
    cols = []
    names = ["BEFORE (today)"] + ["A  Holo Grimoire", "B  Parchment & Ink", "C  Slime Crest"]
    cells = [
        [before["hud3"].crop((0, 0, 1280, 720)), before["book3"].resize((1280, 720)), before["clover3"], before["toast3"].crop((0, 0, 1281, 320)), before["hover3"]],
    ]
    for key in "ABC":
        R = dirs[key]
        cells.append([R["hud3"].crop((0, 380, 760, 720)), R["book100_3"], R["clover3"], R["alert3"], R["hover3"]])
    rowlabels = ["HUD  (scale 3)", "Grimoire menu, compiled page  (scale 3)", "Clover icons  (scale 3)", "Alert widget  (scale 3)", "Hover ripple / click  (scale 3)"]
    title_h = 38
    colimgs = []
    for ci, col in enumerate(cells):
        parts = []
        h = Image.new("RGB", (W, title_h), BG)
        ImageDraw.Draw(h).text((6, 6), names[ci], font=fnt(22, True), fill=(240, 244, 252))
        parts.append(h)
        for ri, im in enumerate(col):
            parts.append(captioned(fit(im.convert("RGB"), W), rowlabels[ri]) if ci == 0 else fit(im.convert("RGB"), W))
            if ci != 0:
                pad = Image.new("RGB", (W, 26), BG)
                parts[-1] = vstack([pad, parts[-1]], gap=0)
        colimgs.append(vstack(parts, gap=8))
    sheet = hstack(colimgs, gap=12, pad=12)
    sheet.save(os.path.join(out, "gui_compare.png"), optimize=True)
    return sheet


def budget(out, sizes_by_dir):
    lines = ["| direction | texture | px | PNG bytes | VRAM (RGBA8, no mipmaps) |", "|---|---|---|---|---|"]
    totals = {}
    for key, sizes in sizes_by_dir.items():
        tp = tv = 0
        for name, (w, h, b) in sorted(sizes.items()):
            lines.append("| %s | %s | %dx%d | %d | %d |" % (key, name.split("/", 1)[1], w, h, b, w * h * 4))
            tp += b
            tv += w * h * 4
        totals[key] = (len(sizes), tp, tv)
    lines.append("")
    lines.append("| direction | textures | PNG total | VRAM total |")
    lines.append("|---|---|---|---|")
    for key, (n, tp, tv) in totals.items():
        lines.append("| %s | %d | %.1f KB | %.0f KB |" % (key, n, tp / 1024.0, tv / 1024.0))
    with open(os.path.join(out, "texture_budget.md"), "w") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(out, "texture_budget.json"), "w") as f:
        json.dump({k: {"textures": v[0], "png_bytes": v[1], "vram_bytes": v[2]} for k, v in totals.items()}, f, indent=1)
    return totals


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "docs", "previews", "gui_redesign"))
    ap.add_argument("--only", default="")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    before = render_before(a.out)
    dirs, sizes = {}, {}
    for key in "ABC":
        dirs[key], sizes[key] = render_direction(key, a.out)
        print("rendered", key)
    render_compare(a.out, before, dirs)
    print(json.dumps(budget(a.out, sizes)))


if __name__ == "__main__":
    main()
