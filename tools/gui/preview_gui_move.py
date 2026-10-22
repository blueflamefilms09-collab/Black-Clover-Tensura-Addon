#!/usr/bin/env python3
"""
Previews GUI Move's edit mode without the game.

    python3 -B tools/gui/preview_gui_move.py [OUT_DIR]        # default docs/previews/gui_move

It compiles tools/gui/GuiMovePreview.java against src/main/java (only pure classes are pulled in: GuiMoveMath, GuiMoveRegistry, the System window's OverlayLayout and the
Djinn panel painter, no Minecraft jar is needed) and runs it. The Djinn panel and the System window are drawn by the SAME code the game uses and every rectangle comes from the
game's own GuiMoveMath; the drawing surface, the world behind and the edit screen's outlines are stand-ins with the same geometry and colours as client/ui/move/GuiMoveScreen.
Writes edit_mode.png, djinn_dragged.png, djinn_moved_play.png, djinn_bigger.png, reset.png and window_sizes.png.
"""
import os
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(ROOT, "docs", "previews", "gui_move")


def main():
    env = dict(os.environ, JAVA_TOOL_OPTIONS="")
    build = tempfile.mkdtemp(prefix="gui_move_prev_")
    try:
        subprocess.check_call(["javac", "-nowarn", "-sourcepath", os.path.join(ROOT, "src", "main", "java"), "-d", build,
                               os.path.join(ROOT, "tools", "gui", "GuiMovePreview.java")], env=env)
        os.makedirs(OUT, exist_ok=True)
        subprocess.check_call(["java", "-Djava.awt.headless=true", "-cp", build, "GuiMovePreview", OUT, os.path.join(ROOT, "tools", "lang", "gui_move.json")], env=env)
    finally:
        shutil.rmtree(build, ignore_errors=True)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
