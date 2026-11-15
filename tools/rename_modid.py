#!/usr/bin/env python3
"""Rename the mod id `nusmp` to `anime_multiverse` (3.84.0).

What it changes: the id as a namespace / folder / lang-key segment / literal ("anime_multiverse", anime_multiverse:x, assets/anime_multiverse, item.anime_multiverse.x, /anime_multiverse ...).
What it keeps on purpose:
  * the Java package com.newuniverse.nusmp (players never see it)
  * identifiers that continue the word: nusmp_x (stored NBT keys, files), nusmp-x (config file names), nusmpFoo
  * CHANGELOG.md and docs/ (history; docs/server_safe_allow.txt lists the package)
Usage: python3 tools/rename_modid.py [--dry]   (run from the repo root; it also moves the folders)
"""
import os, re, subprocess, sys

OLD, NEW = "nus" + "mp", "anime_multiverse"
DRY = "--dry" in sys.argv
PAT = re.compile(r"(?<![A-Za-z0-9_])(?<!newuniverse\.)(?<!newuniverse/)" + OLD + r"(?![A-Za-z0-9_-])")
ROOTS = ["src", "tools", ".github", "build.gradle", "gradle.properties", "settings.gradle", "README.md", "LIBRARIES.md"]
SKIP_DIRS = {".git", "build", ".gradle", "run", "__pycache__", "node_modules", "bvenv"}

changed = files = 0
def rewrite(path):
    global changed, files
    try:
        with open(path, "r", encoding="utf-8") as f: s = f.read()
    except (UnicodeDecodeError, OSError):
        return
    n, k = PAT.subn(NEW, s)
    if k:
        files += 1; changed += k
        if not DRY:
            with open(path, "w", encoding="utf-8", newline="") as f: f.write(n)

for r in ROOTS:
    if os.path.isfile(r): rewrite(r); continue
    for d, ds, fs in os.walk(r):
        ds[:] = [x for x in ds if x not in SKIP_DIRS]
        for fn in fs: rewrite(os.path.join(d, fn))
print(f"{'would change' if DRY else 'changed'} {changed} places in {files} files")

# folders named anime_multiverse that are NOT the java package, and the mixin config
moves = []
for r in ["src"]:
    for d, ds, fs in os.walk(r):
        for x in list(ds):
            if x == OLD and not d.endswith("newuniverse"): moves.append((os.path.join(d, x), os.path.join(d, NEW)))
        ds[:] = [x for x in ds if not (x == OLD)]
for fn in ("src/main/resources/" + OLD + ".mixins.json",):
    if os.path.exists(fn): moves.append((fn, fn.replace(OLD, NEW)))
for a, b in moves:
    print("move", a, "->", b)
    if not DRY: subprocess.check_call(["git", "mv", a, b])
