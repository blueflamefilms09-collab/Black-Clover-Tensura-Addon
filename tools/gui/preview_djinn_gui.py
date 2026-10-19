#!/usr/bin/env python3
"""
Previews the active-Djinn panel without the game.

    python3 -B tools/gui/preview_djinn_gui.py [OUT_DIR] [--all-gifs]      # default docs/previews/djinn_gui

It compiles tools/gui/DjinnGuiPreview.java against src/main/java (only the pure classes of client/relics/djinn/gui and relics/djinn/Djinn are
pulled in, no Minecraft jar is needed), runs it, and then builds one animated GIF per Djinn out of its frames. The picture is produced by the SAME
DjinnGuiPainter the game uses; only the drawing surface is a stand-in (alpha blended rectangles, a monospace font of 6 pixels a character), so
colours, shapes and motion are the real ones while the text metrics are approximate.
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
ALL_GIFS = "--all-gifs" in sys.argv
OUT = os.path.abspath(ARGS[0]) if ARGS else os.path.join(ROOT, "docs", "previews", "djinn_gui")
# animated GIFs are 1.3 MB each: the repo keeps four (one of each look family), --all-gifs writes all of them
GIFS = {"baal", "amon", "vinea", "zepar"}


def main():
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    build = tempfile.mkdtemp(prefix="djinn_gui_prev_")
    try:
        subprocess.check_call(["javac", "-sourcepath", os.path.join(ROOT, "src", "main", "java"), "-d", build,
                               os.path.join(ROOT, "tools", "gui", "DjinnGuiPreview.java")], env=env)
        os.makedirs(OUT, exist_ok=True)
        subprocess.check_call(["java", "-Djava.awt.headless=true", "-cp", build, "DjinnGuiPreview", OUT], env=env)
    finally:
        shutil.rmtree(build, ignore_errors=True)
    try:
        from PIL import Image
    except ImportError:
        print("Pillow missing: PNGs written, GIFs skipped")
        return
    for name in sorted(os.listdir(OUT)):
        d = os.path.join(OUT, name)
        if name.startswith("frames_") and os.path.isdir(d):
            if not ALL_GIFS and name[len("frames_"):] not in GIFS:
                shutil.rmtree(d)
                continue
            frames = [Image.open(os.path.join(d, f)).convert("P", palette=Image.ADAPTIVE) for f in sorted(os.listdir(d)) if f.endswith(".png")]
            frames[0].save(os.path.join(OUT, "anim_" + name[len("frames_"):] + ".gif"), save_all=True, append_images=frames[1:], duration=100, loop=0, disposal=2)
            shutil.rmtree(d)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
