#!/usr/bin/env python3
"""Why: the Soul Forge screen is written against Minecraft's GUI classes and cannot be opened without the game. But the screen (SoulForgeScreen), its five
pages and every helper are plain Java over a small set of Minecraft calls. This runner compiles those REAL sources, unchanged, against a shim of the calls
(tools/avatar/ui_harness/shim: Java2D drawing, Minecraft's glyph widths, the repo's real language file and the language fragment, the real data files and the
real pure classes), plays a creator's whole visit with Harness.java (roll the stats, type a name, choose a race, personalities, skills and aptitudes, hover
tooltips, watch the slot-machine reel stop, press Finalize) and writes a picture after each step that matters:

    python3 -B tools/avatar/ui_harness/run_ui_harness.py <out-dir> [--gson gson.jar] [--joml joml.jar]

It proves layout, drawing order, input routing and the screen's own logic. It does NOT prove the real renderer: the fonts, the 3D Avatar, depth sorting,
blending and the game's own widgets are approximations or stand-ins here, so the first run in the game can still look different. Needs a JDK and the gson and
joml jars (found in the Gradle cache when not named).
"""
import argparse
import glob
import os
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))


def find_jar(pattern_parts, explicit):
    if explicit:
        return explicit
    home = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1")
    hits = sorted(glob.glob(os.path.join(home, *pattern_parts)))
    if not hits:
        sys.exit("cannot find a jar like %s: pass it explicitly" % "/".join(pattern_parts))
    return hits[-1]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("out", help="directory for the pictures")
    ap.add_argument("--gson", default="")
    ap.add_argument("--joml", default="")
    args = ap.parse_args()
    gson = find_jar(["com.google.code.gson", "gson", "*", "*", "gson-*.jar"], args.gson)
    joml = find_jar(["org.joml", "joml", "*", "*", "joml-*.jar"], args.joml)
    out = os.path.abspath(args.out)
    os.makedirs(out, exist_ok=True)
    work = tempfile.mkdtemp(prefix="ui_harness_")
    try:
        frag = os.path.join(work, "fragment.lang.json")
        subprocess.run([sys.executable, "-B", os.path.join(ROOT, "tools", "avatar", "gen_soulforge_lang.py"), "--out", frag], check=True, stdout=subprocess.DEVNULL)
        classes = os.path.join(work, "classes")
        os.makedirs(classes)
        env = dict(os.environ, JAVA_TOOL_OPTIONS="")
        sp = os.pathsep.join([os.path.join(HERE, "shim"), os.path.join(ROOT, "src", "main", "java")])
        cp = os.pathsep.join([gson, joml])
        javac = subprocess.run(["javac", "-proc:none", "-nowarn", "-Xlint:none", "-d", classes, "-cp", cp, "-sourcepath", sp, os.path.join(HERE, "Harness.java")],
                               env=env, capture_output=True, text=True)
        if javac.returncode != 0:
            sys.stderr.write(javac.stdout + javac.stderr)
            return 2
        lang = ",".join([os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "lang", "en_us.json"), frag])
        run = subprocess.run(["java", "-Djava.awt.headless=true", "-Dharness.assets=" + os.path.join(ROOT, "src", "main", "resources", "assets"), "-Dharness.lang=" + lang,
                              "-cp", os.pathsep.join([classes, gson, joml]), "com.newuniverse.nusmp.client.avatar.Harness", out, ROOT],
                             env=env, capture_output=True, text=True)
        sys.stdout.write(run.stdout)
        sys.stderr.write(run.stderr)
        return run.returncode
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
