#!/usr/bin/env python3
"""
Previews the companion talk screen without the game.

    python3 -B tools/gui/preview_companion_gui.py [OUT_DIR]      # default docs/previews/companions

It compiles tools/gui/CompanionTalkPreview.java against src/main/java (only the pure classes of client/companion are pulled in, no Minecraft jar is needed), runs it, and builds one contact sheet
(talk_screen_sheet.png) of the four tabs out of the pictures. The picture is produced by the SAME TalkPainter and TalkLayout the game uses; only the drawing surface is a stand-in (alpha blended rectangles,
a monospace font of 6 pixels a character, items as coloured squares), so colours, shapes and layout are the real ones while the text metrics are approximate.
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
OUT = os.path.abspath(ARGS[0]) if ARGS else os.path.join(ROOT, "docs", "previews", "companions")


def main():
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    build = tempfile.mkdtemp(prefix="companion_gui_prev_")
    try:
        subprocess.check_call(["javac", "-sourcepath", os.path.join(ROOT, "src", "main", "java"), "-d", build,
                               os.path.join(ROOT, "tools", "gui", "CompanionTalkPreview.java")], env=env)
        os.makedirs(OUT, exist_ok=True)
        subprocess.check_call(["java", "-Djava.awt.headless=true", "-cp", build, "CompanionTalkPreview", OUT], env=env)
    finally:
        shutil.rmtree(build, ignore_errors=True)
    try:
        from PIL import Image
    except ImportError:
        print("Pillow missing: PNGs written, contact sheet skipped")
        return
    names = ["talk_talk.png", "talk_interact.png", "talk_bond.png", "talk_pouch.png"]
    imgs = [Image.open(os.path.join(OUT, n)).convert("RGB") for n in names]
    w, h = imgs[0].size
    sheet = Image.new("RGB", (w * 2 + 12, h * 2 + 12), (12, 12, 16))
    for i, im in enumerate(imgs):
        sheet.paste(im, ((i % 2) * (w + 12), (i // 2) * (h + 12)))
    sheet.save(os.path.join(OUT, "talk_screen_sheet.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
