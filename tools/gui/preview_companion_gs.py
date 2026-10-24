#!/usr/bin/env python3
"""
Previews the Great Spirit companion talk screen and its speech bubble without the game.

    python3 -B tools/gui/preview_companion_gs.py [OUT_DIR]      # default docs/previews/companions/gs

It compiles tools/gui/CompanionGsPreview.java against src/main/java (only the pure classes of client/companion/gs and client/ui/gs are pulled in, no Minecraft jar is needed), runs it from the repository
root, and builds one contact sheet per skin (talk_b_sheet.png, talk_c_sheet.png) of the four tabs. The pictures are produced by the SAME GsTalkPainter and GsTalkLayout the game uses, with the real texture
files of skins B and C, the real skin of Charmy for the portrait and the real English texts; only the drawing surface is a stand-in (6 pixel monospace text, coloured tiles for items).
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
OUT = os.path.abspath(ARGS[0]) if ARGS else os.path.join(ROOT, "docs", "previews", "companions", "gs")


def main():
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    build = tempfile.mkdtemp(prefix="companion_gs_prev_")
    try:
        subprocess.check_call(["javac", "-sourcepath", os.path.join(ROOT, "src", "main", "java"), "-d", build,
                               os.path.join(ROOT, "tools", "gui", "CompanionGsPreview.java")], env=env, cwd=ROOT)
        os.makedirs(OUT, exist_ok=True)
        subprocess.check_call(["java", "-Djava.awt.headless=true", "-cp", build, "CompanionGsPreview", OUT], env=env, cwd=ROOT)
    finally:
        shutil.rmtree(build, ignore_errors=True)
    try:
        from PIL import Image
    except ImportError:
        print("Pillow missing: PNGs written, contact sheets skipped")
        return
    for k in ("b", "c"):
        names = ["talk_%s_talk.png" % k, "talk_%s_interact.png" % k, "talk_%s_bond.png" % k, "talk_%s_pouch.png" % k]
        imgs = [Image.open(os.path.join(OUT, n)).convert("RGB") for n in names]
        w, h = imgs[0].size
        sheet = Image.new("RGB", (w * 2 + 12, h * 2 + 12), (12, 12, 16))
        for i, im in enumerate(imgs):
            sheet.paste(im, ((i % 2) * (w + 12), (i // 2) * (h + 12)))
        sheet.save(os.path.join(OUT, "talk_%s_sheet.png" % k))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
