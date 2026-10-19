"""djinn36_preview: contact sheets of the new Djinn on a mannequin (software rasteriser of mesh_lib, no game needed).

One image per Djinn: [Half Equip 3/4] [Full light 3/4 + back] [weapon profile] [Full dark 3/4 + back] [Half dark 3/4]. The mannequin poses the pieces the way
DjinnArmorLayer does (see gen_djinn_unique.mannequin_scene). The preview is a re-implementation of the layer's pose, not the game."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from PIL import Image                                    # noqa: E402
import djinn36_build as B                                # noqa: E402
import gen_djinn_unique as gdu                           # noqa: E402


def sheet(dj, outdir, panel=(300, 480), views_full=("three_quarter", "back")):
    os.makedirs(outdir, exist_ok=True)
    tmp = os.path.join(outdir, "_t")
    os.makedirs(tmp, exist_ok=True)
    did = dj["id"]
    tiles = []
    jobs = [("half_u", "u", True, ["three_quarter"]), ("full_u", "u", False, list(views_full))]
    for name, var, half, views in jobs:
        sc = gdu.mannequin_scene(dj, var, half)
        out = gdu.render(sc, dj, os.path.join(tmp, f"{did}_{name}.png"), views, panel, variant=var)
        tiles.append(Image.open(out).convert("RGB"))
    wsc = gdu.weapon_scene(dj, "u")
    tiles.append(Image.open(gdu.render(wsc, dj, os.path.join(tmp, f"{did}_weapon.png"), ["profile"], (200, panel[1]), 1024)).convert("RGB"))
    for name, var, half, views in (("full_d", "d", False, list(views_full)), ("half_d", "d", True, ["three_quarter"])):
        sc = gdu.mannequin_scene(dj, var, half)
        out = gdu.render(sc, dj, os.path.join(tmp, f"{did}_{name}.png"), views, panel, variant=var)
        tiles.append(Image.open(out).convert("RGB"))
    W = sum(t.width for t in tiles)
    H = max(t.height for t in tiles)
    sh = Image.new("RGB", (W, H), (84, 89, 107))
    x = 0
    for t in tiles:
        sh.paste(t, (x, 0))
        x += t.width
    path = os.path.join(outdir, f"{did}_sheet.png")
    sh.save(path)
    for f in os.listdir(tmp):
        os.remove(os.path.join(tmp, f))
    os.rmdir(tmp)
    return path


def icon_sheet(outdir, cell=88):
    """The 24 Seals, then the 24 light and the 24 dark weapon icons (16 x 16, enlarged) on one sheet: row 1 seals, rows 2-5 weapons."""
    import djinn36_roster as R
    from PIL import ImageDraw
    root = B.ASSETS
    ids = R.ids()
    rows = [["djinn_seal_" + i for i in ids], ["djinn_weapon_" + i for i in ids], ["djinn_weapon_" + i + "_dark" for i in ids]]
    cols = 12
    nrows = sum((len(r) + cols - 1) // cols for r in rows)
    sh = Image.new("RGBA", (cols * cell, nrows * cell), (84, 89, 107, 255))
    y = 0
    for r in rows:
        for n, item in enumerate(r):
            im = Image.open(os.path.join(root, "textures", "item", item + ".png")).convert("RGBA").resize((cell - 8, cell - 8), Image.NEAREST)
            sh.alpha_composite(im, ((n % cols) * cell + 4, y + (n // cols) * cell + 4))
        y += ((len(r) + cols - 1) // cols) * cell
    os.makedirs(outdir, exist_ok=True)
    path = os.path.join(outdir, "icons.png")
    sh.convert("RGB").save(path)
    return path
