"""Software-rasteriser stills of the Legendary Bow looks (4.59.0, docs/heroweapon/bow_designs.md). NOT Cycles renders, NOT an in-game screenshot.

    python3 -B tools/blender/forms_legendary_bow_previews.py form    ID [ID ...] [--assets DIR]   # docs/heroweapon/form_previews/legendary_bow_<form>.png (profile, three-quarter, back)
    python3 -B tools/blender/forms_legendary_bow_previews.py sheet   [--before DIR] [--assets DIR]  # docs/previews/bows_v3/all_bows.png: all 11 bows, labelled, same scale, before and after rows
    python3 -B tools/blender/forms_legendary_bow_previews.py closeup [--assets DIR]                 # docs/previews/bows_v3/sagittarius_closeup.png
    python3 -B tools/blender/forms_legendary_bow_previews.py before                                 # renders the shipped (pre-change) looks into docs/previews/bows_v3/before/ (run BEFORE installing)

--assets DIR: a trial folder (weapon_meshes/ and textures/entity/weapon/ below it) instead of the mod's assets.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as pm  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

MOD = pm.ASSETS
FORMS = ["hunters_bow", "scouts_bow", "bind_arrow", "bird_hunting", "arrow_shower", "meteor_bow", "frozen_rain", "eagle_piercer", "sagittarius"]
IDS = ["legendary_bow", "legendary_bow_curse"] + ["legendary_bow__" + f for f in FORMS]
OUT = os.path.join(ROOT, "docs", "previews", "bows_v3")


def render(mid, out, views, panel, assets=None):
    pm.ASSETS = assets or MOD
    path = os.path.join(pm.ASSETS, "weapon_meshes", mid + ".txt")
    pm.render_preview(path, None, out, views=views, normals="vertex", panel=panel)
    pm.ASSETS = MOD
    return out


def form(ids, assets, outdir=None):
    d = outdir or os.path.join(ROOT, "docs", "heroweapon", "form_previews")
    os.makedirs(d, exist_ok=True)
    for f in ids:
        mid = f if f.startswith("legendary_bow") else "legendary_bow__" + f
        out = os.path.join(d, mid.replace("legendary_bow__", "legendary_bow_") + ".png")
        print(render(mid, out, ("profile", "three_quarter", "back"), (300, 540), assets))


def tile(mid, assets, panel=(190, 440)):
    tmp = os.path.join(OUT, "_t.png")
    os.makedirs(OUT, exist_ok=True)
    render(mid, tmp, ("profile",), panel, assets)
    im = Image.open(tmp).convert("RGB")
    os.remove(tmp)
    return im


def sheet(before_dir, assets):
    os.makedirs(OUT, exist_ok=True)
    cols = len(IDS)
    pw, ph = 190, 440
    W, H = cols * pw, 2 * (ph + 22) + 6
    img = Image.new("RGB", (W, H), (52, 58, 72))
    d = ImageDraw.Draw(img)
    for row, (title, src) in enumerate((("BEFORE (4.58.0)", before_dir), ("AFTER (4.59.0)", None))):
        for i, mid in enumerate(IDS):
            if src:
                p = os.path.join(src, mid + ".png")
                if not os.path.exists(p):
                    continue
                im = Image.open(p).convert("RGB")
            else:
                im = tile(mid, assets)
            y0 = row * (ph + 22) + 20
            img.paste(im, (i * pw, y0))
            d.text((i * pw + 6, row * (ph + 22) + 5), mid.replace("legendary_bow__", "").replace("legendary_bow", "base bow") if mid != "legendary_bow_curse" else "curse bow", fill=(255, 255, 160))
        d.text((64, row * (ph + 22) + 5), title.replace("BEFORE (4.58.0)", "BEFORE 4.58.0").replace("AFTER (4.59.0)", "AFTER 4.59.0"), fill=(160, 255, 200))
    img.save(os.path.join(OUT, "all_bows.png"), optimize=True)
    print(os.path.join(OUT, "all_bows.png"))


def closeup(assets):
    mid = "legendary_bow__sagittarius"
    os.makedirs(OUT, exist_ok=True)
    pm.ASSETS = assets or MOD
    path = os.path.join(pm.ASSETS, "weapon_meshes", mid + ".txt")
    tiles = []
    for tag, views, focus, panel in (("whole", ("profile", "three_quarter", "back"), None, (300, 540)), ("riser", ("profile", "three_quarter", "back"), (-14.0, 30.0), (300, 420)), ("limb", ("profile", "back", "edge"), (28.0, 72.0), (300, 420))):
        out = os.path.join(OUT, "_c_%s.png" % tag)
        pm.render_preview(path, None, out, views=views, normals="vertex", panel=panel, focus=focus)
        tiles.append(Image.open(out).convert("RGB"))
        os.remove(out)
    pm.ASSETS = MOD
    W = max(t.width for t in tiles)
    img = Image.new("RGB", (W, sum(t.height for t in tiles)), (52, 58, 72))
    y = 0
    for t in tiles:
        img.paste(t, (0, y))
        y += t.height
    img.save(os.path.join(OUT, "sagittarius_closeup.png"), optimize=True)
    print(os.path.join(OUT, "sagittarius_closeup.png"))


def silhouette(path, w=150, h=270):
    """The profile silhouette of a rest mesh (every quad of the body layer, filled, on a common canvas: y -90..90, x -30..70 mesh units) as a boolean mask."""
    import numpy as np
    d = pm.parse_game_txt(path)
    im = Image.new("L", (w, h), 0)
    dr = ImageDraw.Draw(im)
    sc = h / 180.0
    for layer, P, UV, N in d["quads"]:
        if layer != 0:
            continue
        dr.polygon([((p[0] + 30.0) * sc, (90.0 - p[1]) * sc) for p in P], fill=255)
    return np.asarray(im) > 0


def similarity(assets, out):
    """Pairwise overlap (intersection over union) of the profile silhouettes of the 11 bows, and each bow's most similar other bow."""
    import numpy as np
    base = assets or MOD
    M = {i: silhouette(os.path.join(base, "weapon_meshes", i + ".txt")) for i in IDS}
    names = [i.replace("legendary_bow__", "").replace("legendary_bow", "base") for i in IDS]
    lines = ["silhouette overlap (IoU of the profile masks, 1.00 = identical outline), %s" % base, ""]
    lines.append("%-14s " % "" + " ".join("%6s" % n[:6] for n in names))
    worst = []
    for a, ia in enumerate(IDS):
        row = []
        for b, ib in enumerate(IDS):
            iou = (M[ia] & M[ib]).sum() / max(1, (M[ia] | M[ib]).sum())
            row.append(iou)
        lines.append("%-14s " % names[a][:14] + " ".join("%6.2f" % v for v in row))
        j = max((k for k in range(len(IDS)) if k != a), key=lambda k: row[k])
        worst.append((names[a], names[j], row[j]))
    lines.append("")
    lines.append("most similar other bow: " + "; ".join("%s ~ %s %.2f" % w for w in worst))
    allp = [(M[a] & M[b]).sum() / max(1, (M[a] | M[b]).sum()) for ia, a in enumerate(IDS) for ib, b in enumerate(IDS) if ia < ib]
    lines.append("mean of all %d pairs %.3f, highest %.3f" % (len(allp), sum(allp) / len(allp), max(allp)))
    open(out, "w").write("\n".join(lines) + "\n")
    print("\n".join(lines))


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd")
    ap.add_argument("ids", nargs="*")
    ap.add_argument("--assets")
    ap.add_argument("--before", default=os.path.join(OUT, "before"))
    ap.add_argument("--outdir")
    a = ap.parse_args(argv)
    if a.cmd == "form":
        form(a.ids or FORMS, a.assets, a.outdir)
    elif a.cmd == "sheet":
        sheet(a.before, a.assets)
    elif a.cmd == "closeup":
        closeup(a.assets)
    elif a.cmd == "similarity":
        similarity(a.assets, a.outdir or os.path.join(OUT, "similarity.txt"))
    elif a.cmd == "before":
        os.makedirs(a.before, exist_ok=True)
        for mid in IDS:
            render(mid, os.path.join(a.before, mid + ".png"), ("profile",), (190, 440))
            print(mid)


if __name__ == "__main__":
    main(sys.argv[1:])
