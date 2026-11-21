"""Style wave 3 stills (docs/previews/style_wave3/): one PNG per weapon, a BEFORE row (the classic_style3 copy) over an AFTER row (the shipped mesh), plus before_after.png (all ten).

    python3 tools/heroweapon/style/wave3_previews.py

Software-rasteriser stills (tools/relics/preview_mesh.py), not game screenshots. Read-only on the assets; writes only the PNGs.
"""
import os
import shutil
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as pm  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

A = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
IDS = ["legendary_spear__dragon_minute_hand", "legendary_spear", "legendary_spear_curse", "legendary_spear__portal_spear", "vassal_katana__siderite_katana", "vassal_katana__white_tiger_katana",
       "vassal_carriage__war_carriage", "vassal_carriage__queens_chariot", "vassal_mirror__yata_mirror", "legendary_sword__phoenix_sword"]
OUT = os.path.join(ROOT, "docs", "previews", "style_wave3")


def classic_root():
    """A temp assets root laid out like the mod's (weapon_meshes/<id>.txt, textures/entity/weapon/<id>.png) holding the classic_style3 copies."""
    t = tempfile.mkdtemp(prefix="w3_classic_")
    os.makedirs(os.path.join(t, "weapon_meshes"))
    os.makedirs(os.path.join(t, "textures", "entity", "weapon"))
    for i in IDS:
        shutil.copy(os.path.join(A, "weapon_meshes", "classic_style3", i + ".txt"), os.path.join(t, "weapon_meshes", i + ".txt"))
        for suf in (".png", "_glow.png"):
            p = os.path.join(A, "textures", "entity", "weapon", "classic_style3", i + suf)
            if os.path.exists(p):
                shutil.copy(p, os.path.join(t, "textures", "entity", "weapon", i + suf))
    return t


def still(root, mesh_id, out):
    old = pm.ASSETS
    pm.ASSETS = root
    try:
        pm.render_preview(os.path.join(root, "weapon_meshes", mesh_id + ".txt"), None, out, views=("profile", "three_quarter", "edge"), normals="vertex", panel=(300, 440), label=False)
    finally:
        pm.ASSETS = old
    return Image.open(out).convert("RGB")


def main():
    os.makedirs(OUT, exist_ok=True)
    cl = classic_root()
    tmp = tempfile.mkdtemp(prefix="w3_prev_")
    pairs = []
    for i in IDS:
        b = still(cl, i, os.path.join(tmp, "b.png"))
        a = still(A, i, os.path.join(tmp, "a.png"))
        w = max(a.width, b.width)
        im = Image.new("RGB", (w, a.height + b.height + 20), (52, 58, 72))
        im.paste(b, (0, 20))
        im.paste(a, (0, 20 + b.height))
        d = ImageDraw.Draw(im)
        d.text((6, 4), "%s   before (classic_style3)  /  after" % i, fill=(255, 255, 255))
        im.save(os.path.join(OUT, i + ".png"), optimize=True)
        pairs.append((i, b, a))
    cw = 300 * 3
    ch = 440
    sheet = Image.new("RGB", (cw, (ch * 2 + 14) * len(pairs)), (52, 58, 72))
    for r, (i, b, a) in enumerate(pairs):
        y = r * (ch * 2 + 14)
        sheet.paste(b.crop((0, 0, min(b.width, cw), ch)), (0, y + 14))
        sheet.paste(a.crop((0, 0, min(a.width, cw), ch)), (0, y + 14 + ch))
        ImageDraw.Draw(sheet).text((6, y), i, fill=(255, 255, 255))
    sheet = sheet.crop((0, 0, cw, sheet.height))
    sheet.save(os.path.join(OUT, "before_after.png"), optimize=True)
    shutil.rmtree(cl, ignore_errors=True)
    shutil.rmtree(tmp, ignore_errors=True)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
