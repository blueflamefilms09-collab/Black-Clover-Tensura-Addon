"""Why: the owner asked to SEE the bosses. Dungeon-boss rework 2 gives each of the nine Labyrinth Guardian sector traits its own colourway of the Warden suit, a Guardian move set and a sound set. This script writes
docs/dungeon_bosses_previews2/:

    python3 -B tools/dungeon_bosses/make_previews2.py

  guardian_colourways.png  REAL RENDER: the nine recoloured looks (guardian_<trait> rows of looks_dungeon.json) drawn by the software renderer of tools/playermodel (the same meshes, skins, glow maps and
                           extras the game uses; light and colour differ a little from the game), three views each, next to the original Floor Warden.
  guardian_moves.png       SCHEMATIC (not a render): the five moves of the Guardian's book drawn as top-down telegraph shapes with their real numbers from GuardianRules (radii, line length, ring distances).
  sound_spectrograms.png   REAL DATA: log-magnitude spectrograms of one variant of every one of the 16 cues, from the ogg files' source synthesis (needs tools/gen_dungeon_boss_sounds.py --preview first, which
                           writes wavs; if none are found the sheet is skipped and says so).
Needs numpy and Pillow (see tools/render_preview/README.md for the renderer's own needs).
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
sys.path.insert(0, TOOLS)
sys.path.insert(0, os.path.join(TOOLS, "dungeon_skins"))

from PIL import Image, ImageDraw  # noqa: E402

OUT = os.path.join(ROOT, "docs", "dungeon_bosses_previews2")
TRAITS = ["gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral"]
VERDICT = {"gravity": "keep out", "blood": "sigil", "spatial": "sigil", "flame": "keep out", "frost": "sigil", "verdant": "sigil", "storm": "sigil", "shadow": "keep out", "astral": "sigil"}


def _kit():
    """The software renderer of tools/playermodel and its scene builder (tools/dungeon_skins/preview.py has a module name that clashes with it, so the small tint wrapper is repeated here)."""
    import importlib.util
    pm = os.path.join(TOOLS, "playermodel")
    sys.path.insert(0, pm)
    import pmrender as R
    spec = importlib.util.spec_from_file_location("pm_preview", os.path.join(pm, "preview.py"))
    PV = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(PV)
    assets = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
    with open(os.path.join(assets, "playermodel", "looks_dungeon.json")) as f:
        looks = json.load(f)["looks"]

    def scene(look):
        tints = {}
        for ex in look.get("extras", []):
            t = ex.get("tint")
            if t:
                h = t.lstrip("#")[-6:]
                tints[os.path.join(assets, "textures", "entity", ex.get("texture", ex["model"]) + ".png")] = tuple(int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4))
        orig = R.load_tex

        def load(path):
            a = orig(path)
            c = tints.get(path)
            if c is not None:
                a = a.copy()
                for k in range(3):
                    a[..., k] = a[..., k] * c[k]
            return a

        R.load_tex = load
        try:
            return PV.scene_of(look)
        finally:
            R.load_tex = orig

    return R, scene, looks


def colourways():
    R, scene, looks = _kit()
    ids = ["floor_warden"] + ["guardian_" + t for t in TRAITS]
    size = (150, 240)
    cols = 5
    rows = (len(ids) + cols - 1) // cols
    sheet = Image.new("RGB", (size[0] * 2 * cols, size[1] * rows), (24, 22, 30))
    for n, i in enumerate(ids):
        look = looks[i]
        s = look.get("scale", 1.0)
        px = 5.0 / max(1.0, s * 0.9)
        front = R.render(scene(look), size=size, yaw=20, pitch=6, center=(0, 12, 0), scale=px, floor=True)
        back = R.render(scene(look), size=size, yaw=200, pitch=6, center=(0, 12, 0), scale=px, floor=True)
        x, y = (n % cols) * size[0] * 2, (n // cols) * size[1]
        sheet.paste(front, (x, y))
        sheet.paste(back, (x + size[0], y))
        d = ImageDraw.Draw(sheet)
        d.text((x + 4, y + 4), i + (" (original)" if i == "floor_warden" else "") + ("   verdict: " + VERDICT[i[9:]] if i.startswith("guardian_") else ""), fill=(255, 255, 255))
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "guardian_colourways.png")
    sheet.save(path)
    return path


def moves():
    sys.path.insert(0, os.path.join(TOOLS, "dungeon_bosses"))
    S = 11.0                               # pixels per block
    W = H = 280
    cells = []
    cfg = dict(slam=5.5, lance_len=22.0, lance_half=1.6, mark=2.4, sigil=3.0, sigil_d=9.0, keep=12.0, wave=16.0)
    titles = [("Ground Slam", "ring r %.1f, step out" % cfg["slam"]),
              ("Lance", "line %.0f long, %.1f each side; tracks, then locks" % (cfg["lance_len"], cfg["lance_half"] * 2)),
              ("Rain", "up to 3 marks r %.1f follow ~1 s then lock" % cfg["mark"]),
              ("Wave (phase 2)", "3 rings out to %.0f, jump them" % cfg["wave"]),
              ("Verdict (phase 3)", "stand in a sigil r %.0f, or get out of r %.0f" % (cfg["sigil"], cfg["keep"]))]
    for k, (name, sub) in enumerate(titles):
        im = Image.new("RGB", (W, H + 34), (24, 22, 30))
        d = ImageDraw.Draw(im)
        cx, cy = W // 2, H // 2 + 6
        d.ellipse((cx - 5, cy - 5, cx + 5, cy + 5), fill=(220, 200, 120))
        red = (200, 60, 60)
        if k == 0:
            r = cfg["slam"] * S
            d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=red, width=2)
        elif k == 1:
            L, hw = cfg["lance_len"] * S * 0.5, cfg["lance_half"] * S
            d.rectangle((cx, cy - hw, cx + L, cy + hw), outline=red, width=2)
        elif k == 2:
            for a in (0.5, 2.4, 4.3):
                mx, my = cx + math.cos(a) * 70, cy + math.sin(a) * 70
                r = cfg["mark"] * S
                d.ellipse((mx - r, my - r, mx + r, my + r), outline=red, width=2)
        elif k == 3:
            for r in (cfg["wave"] * S * 0.3, cfg["wave"] * S * 0.55, cfg["wave"] * S * 0.8):
                d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=red, width=2)
        else:
            r = cfg["keep"] * S * 0.7
            d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(200, 120, 60), width=2)
            for sgn in (1, -1):
                sx = cx + sgn * cfg["sigil_d"] * S * 0.7
                rr = cfg["sigil"] * S * 0.7
                d.ellipse((sx - rr, cy - rr, sx + rr, cy + rr), outline=(120, 220, 160), width=2)
        d.text((6, H + 4), name, fill=(255, 255, 255))
        d.text((6, H + 18), sub, fill=(180, 180, 200))
        cells.append(im)
    sheet = Image.new("RGB", (W * len(cells), H + 34), (24, 22, 30))
    for k, c in enumerate(cells):
        sheet.paste(c, (k * W, 0))
    ImageDraw.Draw(sheet).text((W * len(cells) - 330, 6), "SCHEMATIC, not a render (numbers from GuardianRules)", fill=(255, 200, 100))
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "guardian_moves.png")
    sheet.save(path)
    return path


def spectrograms():
    src = os.environ.get("DBOSS_SPEC_DIR") or os.path.join(ROOT, "build", "dboss_sound_preview")
    if not os.path.isdir(src):
        print("no spectrogram dir", src, "- skipped")
        return None
    pngs = sorted(f for f in os.listdir(src) if f.endswith("_1.png"))
    if not pngs:
        return None
    ims = [Image.open(os.path.join(src, f)).convert("RGB") for f in pngs]
    w, h = 300, 150
    cols = 4
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * rows), (0, 0, 0))
    for k, im in enumerate(ims):
        sheet.paste(im.resize((w, h)), ((k % cols) * w, (k // cols) * h))
        ImageDraw.Draw(sheet).text(((k % cols) * w + 4, (k // cols) * h + 4), pngs[k][:-6], fill=(255, 255, 255))
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "sound_spectrograms.png")
    sheet.save(path)
    return path


if __name__ == "__main__":
    for fn in (colourways, moves, spectrograms):
        try:
            print(fn.__name__, "->", fn())
        except Exception as e:  # a preview must never stop the others
            print(fn.__name__, "failed:", e)
