#!/usr/bin/env python3
"""
Remakes the 3D model of EVERY Canon Grimoire (the items `charter_grimoire_<id>`: the seven bosses and the arc characters).

Why: the old model was an 11 x 15 unit slab with a display block copied from a sprite, so in the first person it filled the lower right of the screen and in the third person it floated
flat beside the hand like a tray. This tool writes, for every book, a real hardcover grimoire (stepped bevelled boards, a rounded four-banded spine, a page block with page lines, corner
caps, a raised frame, a medallion with a full-bright emblem, a buckled strap with a glowing stone, a ribbon) and the display transforms that hold it (docs/canon/canon_grimoire.md).

Per book it writes
    models/item/charter_grimoire_<id>.json      the REGISTERED model: neoforge:separate_transforms, the 3D body everywhere but the GUI, the old sprite in the GUI
    models/item/charter_grimoire_<id>_3d.json   the 3D body (elements + display)
    textures/item/charter_grimoire_<id>.png     its 192 x 240 palette atlas
and, once, keeps the old art next to it (never deleted: owner rule 2)
    models/item/charter_grimoire_<id>_v1.json   the old model, pointing at
    textures/item/charter_grimoire_<id>_v1.png  the old texture.
`_icon.json` / `_icon.png` (the old flat sprite, the inventory icon) are not touched. The colours, the metal and the 7x7 sigil are READ from the old texture (the `_v1.png`), so a book keeps
its identity; the emblem on the medallion comes from tools/canon/motifs.py (MOTIF_OF).

    python3 tools/canon/gen_canon_models.py                  # writes everything (idempotent)
    python3 tools/canon/gen_canon_models.py --list           # lists the books found and what was read from their old art
    python3 tools/canon/gen_canon_models.py --ids ban dante  # only these
    python3 tools/canon/gen_canon_models.py --out DIR        # write under DIR (the assets/nusmp layout) instead of the mod's resources
"""
import argparse
import json
import os
import re
import shutil
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import canon_book as B  # noqa: E402
import canon_tex as T  # noqa: E402
import motifs as M  # noqa: E402

RES = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
PREFIX = "charter_grimoire_"


def book_ids(res=RES):
    out = []
    for n in sorted(os.listdir(os.path.join(res, "models", "item"))):
        m = re.fullmatch(PREFIX + r"([a-z0-9_]+)\.json", n)
        if not m:
            continue
        ident = m.group(1)
        if ident.endswith(("_icon", "_3d", "_v1")):
            continue
        out.append(ident)
    return out


def keep_old(ident, src, out):
    """Keeps the old model and texture as _v1 (once; the v1 model points at the v1 texture, so the old look stays loadable) and returns the path of the old texture."""
    mdir, tdir = os.path.join(src, "models", "item"), os.path.join(src, "textures", "item")
    v1m, v1t = os.path.join(mdir, PREFIX + ident + "_v1.json"), os.path.join(tdir, PREFIX + ident + "_v1.png")
    if os.path.exists(v1m) and os.path.exists(v1t):
        old_text, old_png = open(v1m, encoding="utf-8").read(), v1t
    else:
        old = json.load(open(os.path.join(mdir, PREFIX + ident + ".json"), encoding="utf-8"))
        assert "loader" not in old, "%s is already the new model but its _v1 copy is missing" % ident
        old_text = json.dumps(old, indent=2).replace("nusmp:item/%s%s\"" % (PREFIX, ident), "nusmp:item/%s%s_v1\"" % (PREFIX, ident)) + "\n"
        old_png = os.path.join(tdir, PREFIX + ident + ".png")
    omdir, otdir = os.path.join(out, "models", "item"), os.path.join(out, "textures", "item")
    os.makedirs(omdir, exist_ok=True)
    os.makedirs(otdir, exist_ok=True)
    if not os.path.exists(os.path.join(omdir, PREFIX + ident + "_v1.json")):
        open(os.path.join(omdir, PREFIX + ident + "_v1.json"), "w", encoding="utf-8").write(old_text)
    if not os.path.exists(os.path.join(otdir, PREFIX + ident + "_v1.png")):
        shutil.copyfile(old_png, os.path.join(otdir, PREFIX + ident + "_v1.png"))
    return old_png


def derive(v1_png):
    """(cover, trim, metal, sigil rows, disc) read from the old 32x32 texture (layout of tools/gen_charter_assets.py)."""
    im = Image.open(v1_png).convert("RGBA")
    px = im.load()
    # the cover: invert gen_charter_assets.cover_face's shade() on a few plain pixels of the back cover (x 11..21, y 0..14), whose formula is known
    ests = []
    for (x, y) in ((2, 2), (3, 3), (7, 3), (8, 2), (2, 12), (8, 12), (3, 12), (7, 12)):
        w, h = 11, 15
        g = 0.10 - 0.20 * (y / (h - 1)) + 0.04 * (1 - abs(x - (w - 1) / 2) / (w / 2))      # the noise term of +-0.03 averages out over the mixed parities
        v = px[11 + x, y][:3]
        ests.append(tuple((v[i] - 255 * g) / (1 - g) if g > 0 else v[i] / (1 + g) for i in range(3)))
    cover = tuple(int(round(sum(e[i] for e in ests) / len(ests))) for i in range(3))
    cover = tuple(max(0, min(255, c)) for c in cover)
    metal = px[28, 0][:3]
    # the sigil: the 7x7 cells at (2..8, 4..10) of the front cover hold two colours, the dark disc and the (lighter) sigil
    cells = [[px[2 + dx, 4 + dy][:3] for dx in range(7)] for dy in range(7)]
    flat = sorted({c for row in cells for c in row}, key=lambda c: sum(c))
    disc, trim = flat[0], flat[-1]
    rows = ["".join("#" if c != disc else "." for c in row) for row in cells]
    return cover, trim, metal, rows


def pretty_json(obj):
    """Top-level keys on their own lines, every element and every display entry on one line: short diffs, still plain JSON."""
    lines = ["{"]
    keys = list(obj)
    for i, k in enumerate(keys):
        v = obj[k]
        tail = "," if i < len(keys) - 1 else ""
        if k == "elements":
            lines.append('  "elements": [')
            for j, e in enumerate(v):
                lines.append("    " + json.dumps(e, separators=(", ", ": ")) + ("," if j < len(v) - 1 else ""))
            lines.append("  ]" + tail)
        elif k == "display":
            lines.append('  "display": {')
            ks = list(v)
            for j, d in enumerate(ks):
                lines.append("    " + json.dumps(d) + ": " + json.dumps(v[d], separators=(", ", ": ")) + ("," if j < len(ks) - 1 else ""))
            lines.append("  }" + tail)
        else:
            lines.append("  " + json.dumps(k) + ": " + json.dumps(v, indent=2).replace("\n", "\n  ") + tail)
    lines.append("}")
    return "\n".join(lines) + "\n"


def build(ident, src, res):
    v1 = keep_old(ident, src, res)
    cover, trim, metal, rows = derive(v1)
    motif, why = M.MOTIF_OF[ident]
    go = M.GLOW_OVERRIDE.get(ident)
    glow = trim if go is None else (go >> 16 & 255, go >> 8 & 255, go & 255)
    atlas = T.paint_all(cover, trim, metal, glow, rows, M.mask_of(motif))
    pal, n = T.to_palette(atlas)
    pal.save(os.path.join(res, "textures", "item", PREFIX + ident + ".png"), optimize=True, transparency=0)
    mdir = os.path.join(res, "models", "item")
    open(os.path.join(mdir, PREFIX + ident + "_3d.json"), "w", encoding="utf-8").write(pretty_json(B.body_model(ident)))
    open(os.path.join(mdir, PREFIX + ident + ".json"), "w", encoding="utf-8").write(pretty_json(B.wrapper_model(ident)))
    return dict(id=ident, cover=cover, trim=trim, metal=metal, glow=glow, motif=motif, colours=n)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", default=RES)
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--ids", nargs="*")
    args = ap.parse_args()
    res = args.out
    ids = args.ids or book_ids(RES)
    missing = [i for i in ids if i not in M.MOTIF_OF]
    if missing:
        print("no emblem chosen for: " + ", ".join(missing) + " (add them to motifs.MOTIF_OF)")
        return 1
    for ident in ids:
        if args.list:
            v1 = os.path.join(RES, "textures", "item", PREFIX + ident + "_v1.png")
            if not os.path.exists(v1):
                v1 = os.path.join(RES, "textures", "item", PREFIX + ident + ".png")
            c, t, m, rows = derive(v1)
            print("%-18s cover %02X%02X%02X trim %02X%02X%02X metal %02X%02X%02X emblem %s" % ((ident,) + c + t + m + (M.MOTIF_OF[ident][0],)))
            continue
        r = build(ident, RES, res)
        print("%-18s %-12s %3d colours" % (ident, r["motif"], r["colours"]))
    if not args.list:
        print("wrote %d Canon Grimoires under %s" % (len(ids), res))
    return 0


if __name__ == "__main__":
    sys.exit(main())
