"""Preview the Engravement Codex book without the game: compiles the real pure look and geometry classes (EngravingCodexLook, GrimoireBookPlan, ...) with the two tiny stand-ins of
tools/compat/preview_stubs for classes that need the game, renders the book like the inventory (gui) view, held (glowing) front, 3/4, from behind and from the spine side, with the
real textures and tints, and the 16x16 icon beside it.

    python3 -B tools/engraving_codex/preview_codex_book.py   ->  build/engraving_codex/book.png  (and docs/previews/engraving_codex/book.png)
"""
import json, os, shutil, subprocess, sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "grimoire_preview"))
import preview  # noqa: E402  (its render() draws the plan's quads with the real textures and tints)

SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp")
OUT = os.path.join(ROOT, "build", "engraving_codex")
DOCS = os.path.join(ROOT, "docs", "previews", "engraving_codex")


def build():
    cls = os.path.join(OUT, "classes")
    shutil.rmtree(cls, ignore_errors=True)
    os.makedirs(cls)
    files = [os.path.join(SRC, "grimoire", f + ".java") for f in ("BookMotif", "BookPalette", "CanonBook", "BookLook", "GrimoireBookPlan", "GrimoireDesignGenerator", "FusionDesign")]
    files.append(os.path.join(SRC, "engraving", "codex", "EngravingCodexLook.java"))
    for root, _, names in os.walk(os.path.join(ROOT, "tools", "compat", "preview_stubs")):
        files += [os.path.join(root, n) for n in names if n.endswith(".java")]
    files.append(os.path.join(HERE, "EngravingPreview.java"))
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    subprocess.run(["javac", "-nowarn", "-d", cls] + files, check=True, env=env)
    return cls


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(DOCS, exist_ok=True)
    cls = build()
    out_json = os.path.join(OUT, "book.json")
    subprocess.run(["java", "-cp", cls, "EngravingPreview", out_json], check=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
    books = json.load(open(out_json))
    W, H = preview.W, preview.H
    views = [("inventory", books[0], preview.rot(20, -35), [0.55, 0.55, 0.6]), ("held, front", books[1], preview.rot(0, 0), [0.12, 0.12, 0.16]),
             ("held, 3/4", books[1], preview.rot(12, -28), [0.12, 0.12, 0.16]), ("back", books[0], preview.rot(15, 150), [0.30, 0.30, 0.36]),
             ("spine side", books[0], preview.rot(10, 70), [0.30, 0.30, 0.36])]
    sheet = Image.new("RGB", (W * len(views) + 150, H + 18), (24, 24, 30))
    for i, (label, book, R, bg) in enumerate(views):
        img = preview.render(book, R, bg)
        tile = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        sheet.paste(tile, (i * W, 18))
        ImageDraw.Draw(sheet).text((i * W + 6, 3), label, fill=(240, 220, 160))
    icon = Image.open(os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item", "engraving_codex.png")).convert("RGBA")
    bg = Image.new("RGBA", (128, 128), (110, 110, 120, 255))
    bg.alpha_composite(icon.resize((128, 128), Image.NEAREST))
    sheet.paste(bg.convert("RGB"), (W * len(views) + 10, 40))
    ImageDraw.Draw(sheet).text((W * len(views) + 10, 3), "16x16 icon", fill=(240, 220, 160))
    for path in (os.path.join(OUT, "book.png"), os.path.join(DOCS, "book.png")):
        sheet.save(path)
        print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
