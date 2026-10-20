"""Contact sheets of the Erza armours (1.99.0), side by side with a Djinn set.

    python3 -B tools/erza_armour_sheets.py --scenes                    # writes tools/render_preview/scenes.d/erza_armour.json (the scenes below)
    python3 -B tools/erza_armour_sheets.py                             # renders and composes every armour -> docs/previews/erza/<armour>_sheet.png
    python3 -B tools/erza_armour_sheets.py heart_kreuz nakagami        # some armours
    RENDER_PREVIEW_BUILD=/some/dir python3 -B tools/erza_armour_sheets.py   # where the raw preview images go (default build/render_preview)

The images are made by tools/render_preview (the REAL ErzaAura painter, the real WaterAuraRig / GeoDraw / EquipAnimation and the models of the jar, rasterised headless with the vanilla entity render types),
so what the sheet shows is what the game's code draws; what it cannot show is the real lighting, the skin of a real player and motion. Each sheet: the Djinn set (Agares, unique look, full equip, from the Djinn's
own preview tool, tools/relics/djinn36_preview.py: front three-quarter and back) at the left for scale, then the armour from the front, the back, the side and on a slim (Alex) body, and the close-ups of
the head, the torso and the legs, with the measured numbers of the set (tools/erza_armour_audit.py).
"""
import json
import os
import subprocess
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, HERE)
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

IDS = ["heart_kreuz", "heavens_wheel", "flame_empress", "lightning_empress", "sea_empress", "black_wing", "clear_heart", "nakagami", "grand_chariot", "adamantine", "purgatory",
       "flight_armor", "wind_god", "fairy_armor"]
EX = ["../../../src/main/java/com/newuniverse/nusmp/requip/Armour.java", "../../../src/main/java/com/newuniverse/nusmp/requip/ArmourSets.java"]
BUILD = os.path.abspath(os.environ.get("RENDER_PREVIEW_BUILD") or os.path.join(ROOT, "build", "render_preview"))
OUT = os.path.join(ROOT, "docs", "previews", "erza")
DJINN_SHEET = os.path.join(ROOT, "docs", "previews", "djinn36", "agares_sheet.png")
MARGIN, HEAD = 6, 30


def scenes():
    out = {"erza_geo_source": {"kind": "geo", "model": "erza_heart_kreuz_arm_belt", "space": "PLAYER", "layer": "CUTOUT", "glow": "C0FFFFFF", "ages": [0], "tile": [240, 240]}}
    for i, a in enumerate(IDS):
        base = {"kind": "aura", "attr": "Erza", "style": i, "total": 100, "ages": [50], "exampleFiles": EX}
        out["erza_armour_" + a] = dict(base, tile=[560, 760], views=[
            {"name": "front", "yaw": 12, "pitch": 6, "target": [0, 1.05, 0.1], "dist": 3.7},
            {"name": "back 3/4", "yaw": 155, "pitch": 12, "target": [0, 1.05, 0.1], "dist": 3.7},
            {"name": "side", "yaw": 90, "pitch": 6, "target": [0, 1.05, 0.1], "dist": 3.7}])
        out["erza_slim_" + a] = dict(base, tile=[560, 760], skin="alex", views=[{"name": "slim (Alex) front", "yaw": 12, "pitch": 6, "target": [0, 1.05, 0.1], "dist": 3.7}])
        out["erza_closeup_" + a] = dict(base, tile=[560, 560], views=[
            {"name": "head", "yaw": 20, "pitch": 8, "target": [0, 1.85, 0], "dist": 1.7},
            {"name": "torso", "yaw": 25, "pitch": 8, "target": [0, 1.3, 0], "dist": 2.3},
            {"name": "legs", "yaw": 25, "pitch": 6, "target": [0, 0.55, 0], "dist": 2.2}])
    return out


def write_scenes():
    p = os.path.join(HERE, "render_preview", "scenes.d", "erza_armour.json")
    json.dump(scenes(), open(p, "w"), indent=1)
    print("wrote", os.path.relpath(p, ROOT))


def tile(name, row, w, h):
    """One view of a rendered scene image (the preview stacks the views as rows under a 30 px title)."""
    im = Image.open(os.path.join(BUILD, name + ".png")).convert("RGB")
    y = HEAD + MARGIN + row * (h + MARGIN)
    return im.crop((MARGIN, y, MARGIN + w, y + h))


def font(size):
    for f in ("DejaVuSans.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(f, size)
        except OSError:
            pass
    return ImageFont.load_default()


def render(ids):
    names = ["erza_geo_source"]
    for a in ids:
        names += ["erza_armour_" + a, "erza_slim_" + a, "erza_closeup_" + a]
    env = dict(os.environ, RENDER_PREVIEW_BUILD=BUILD, JAVA_TOOL_OPTIONS="")
    os.makedirs(BUILD, exist_ok=True)
    subprocess.run([sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + names, check=True, env=env, cwd=ROOT)


def numbers(aid):
    import erza_armour_audit as AU
    return AU.erza_set("erza_" + aid)


def sheet(aid):
    os.makedirs(OUT, exist_ok=True)
    H1 = 600
    front, back, side = (tile("erza_armour_" + aid, r, 560, 760).resize((442, H1), Image.LANCZOS) for r in range(3))
    slim = tile("erza_slim_" + aid, 0, 560, 760).resize((442, H1), Image.LANCZOS)
    head, torso, legs = (tile("erza_closeup_" + aid, r, 560, 560).resize((442, 442), Image.LANCZOS) for r in range(3))
    dj = Image.open(DJINN_SHEET).convert("RGB").crop((300, 0, 900, 480)).resize((750, H1), Image.LANCZOS)
    W = 750 + 4 * 442
    sh = Image.new("RGB", (W, 40 + H1 + 442), (24, 24, 28))
    d = ImageDraw.Draw(sh)
    n = numbers(aid)
    d.text((10, 8), "Requip armour: %s   |   %d pieces, %d cubes, %d drawn faces, %d x %d px largest piece texture, %.1f texels per model pixel, %d painted texels (Djinn unique sets: 19 to 25 pieces, 2856 to 4351 quads, 5.5 to 7.3 texels)" % (
        aid, n["pieces"], n["cubes"], n["faces"], n["texture_max"], n["texture_max"], n["density_median"], n["painted_texels"]), font=font(15), fill=(235, 235, 240))
    sh.paste(dj, (0, 40))
    for k, im in enumerate((front, back, side, slim)):
        sh.paste(im, (750 + k * 442, 40))
    d.text((8, 40 + 6), "Djinn Agares (unique look, full equip)", font=font(14), fill=(255, 235, 160))
    for k, im in enumerate((head, torso, legs)):
        sh.paste(im, (k * 442, 40 + H1))
    ty = 40 + H1 + 12
    x0 = 3 * 442 + 20
    lines = ["Reading the sheet", "", "top left: a Djinn set from its own preview tool (a different rasteriser: judge the level of detail, not the shading)",
             "top row: this armour from the front, the back, the side, and on a slim (Alex) body",
             "bottom row: head, torso and legs close up", "", "all of the armour is the game's own geo models drawn through", "the real ErzaAura painter (forge animation finished);",
             "the player model is the preview's stand-in skin."]
    for i, l in enumerate(lines):
        d.text((x0, ty + i * 21), l, font=font(15 if i else 17), fill=(235, 235, 240) if i else (255, 235, 160))
    path = os.path.join(OUT, aid + "_sheet.png")
    sh.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT), sh.size)


def main(argv):
    if "--scenes" in argv:
        write_scenes()
        return
    ids = [a for a in argv if not a.startswith("--")] or IDS
    if "--no-render" not in argv:
        render(ids)
    for a in ids:
        sheet(a)


if __name__ == "__main__":
    main(sys.argv[1:])
