#!/usr/bin/env python3
"""install_solid_meshes: puts the solid hero weapon meshes the Blender builders made into the mod (4.55.0, docs/heroweapon/flat_parts_blender.md).

The builders (tools/blender/legendary.py, forms_legendary_bow.py, forms_legendary_spear.py, vassal_a.py) write a mesh + skin + glow skin per weapon into a --trial DIR. This script takes
those DIRs and, per weapon id:

  1. keeps the skin the shipped meshes use: copies textures/entity/weapon/<id>.png and <id>_glow.png to textures/entity/weapon/flat_parts_old/ (never overwrites a copy that is already
     there), because the old meshes in weapon_meshes/flat_parts_old/ have the UVs of that skin and a Blender re-bake lays the skin out again; WeaponRenderer draws them with it;
  2. writes the new meshes (the rest mesh and the three draw stages of a bow) with a `# blender-solid` header line, and keeps the `gripxyz` line of the shipped mesh of the same name
     (the hand holds the weapon exactly where it did: the thicker arrow would otherwise move the automatic grip point);
  3. packs and writes the new skin + glow skin (palette PNG, together at most 350 KB) and checks every mesh against the 5000 quad-line budget.

    python3 -B tools/heroweapon/install_solid_meshes.py DIR [DIR ...] [--dry-run]
    python3 -B tools/heroweapon/install_solid_meshes.py --pin-grips      # after grip_table.py --write; then grip_table.py --write again

DIR is either a flat folder (legendary.py, vassal_a.py: <id>.txt / <id>.png side by side) or a forms folder (weapon_meshes/ and textures/entity/weapon/ below it). Idempotent.
After it: python3 -B tools/heroweapon/register_form_meshes.py, tools/bettercombat/inventory.py, tools/weapon_grip/grip_table.py --write and hold_audit.py.
"""
import argparse
import glob
import os
import re
import shutil
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
MESHES = os.path.join(ASSETS, "weapon_meshes")
SKINS = os.path.join(ASSETS, "textures", "entity", "weapon")
OLD_SKINS = os.path.join(SKINS, "flat_parts_old")
MARK = "# blender-solid 4.55.0 (docs/heroweapon/flat_parts_blender.md)"
QUAD_BUDGET = 5000
SKIN_KB = 350.0


def made_solid(path):
    with open(path, encoding="utf-8") as f:
        return any(ln.startswith("# blender-solid") for ln in (next(f, ""), next(f, ""), next(f, "")))


def layout(d):
    """-> (mesh dir, skin dir) of a trial folder."""
    m = os.path.join(d, "weapon_meshes")
    s = os.path.join(d, "textures", "entity", "weapon")
    return (m, s) if os.path.isdir(m) else (d, d)


def quads(text):
    return sum(1 for ln in text.split("\n") if ln.startswith(("q ", "qn ")))


def with_marks(new_text, old_path):
    """The new mesh text + the blender-solid line + the shipped mesh's `gripxyz` (when the new one has none)."""
    lines = new_text.split("\n")
    if any(ln.startswith("# blender-solid") for ln in lines):
        out = lines
    else:
        out = [lines[0], MARK] + lines[1:] if lines[0].startswith("#") else [MARK] + lines
    if os.path.exists(old_path) and not any(ln.startswith("gripxyz") for ln in out):
        with open(old_path, encoding="utf-8") as f:
            old = [ln for ln in f.read().split("\n") if ln.startswith("gripxyz")]
        if old:
            at = next((i for i, ln in enumerate(out) if ln.startswith("orient")), 0) + 1
            out.insert(at, old[0])
    return "\n".join(out)


def pack(path):
    """Palette-packs a skin that is still true colour (forms_kit.pack_png: median pass + 256 colours); a palette PNG is left alone."""
    from PIL import Image
    if Image.open(path).mode == "P":
        return
    sys.path.insert(0, os.path.join(ROOT, "tools", "blender"))
    import forms_kit as K
    for colors, dn in ((256, 0.5), (192, 0.55), (128, 0.65)):
        K.pack_png(path, colors, dn)
        break


def install(dirs, dry=False):
    done, problems = [], []
    for d in dirs:
        mdir, sdir = layout(d)
        skins = sorted(p for p in glob.glob(os.path.join(sdir, "*.png")) if not p.endswith(("_glow.png", "_preview.png")) and os.path.exists(p[:-4] + "_glow.png"))
        for sk in skins:
            wid = os.path.basename(sk)[:-4]
            pat = re.compile(r"^%s(_pull[123])?\.txt$" % re.escape(wid))
            meshes = sorted(p for p in glob.glob(os.path.join(mdir, "*.txt")) if pat.match(os.path.basename(p)))
            if not meshes:
                problems.append("%s: no mesh next to the skin" % wid)
                continue
            texts = {}
            for m in meshes:
                name = os.path.basename(m)
                with open(m, encoding="utf-8") as f:
                    texts[name] = with_marks(f.read(), os.path.join(MESHES, name))
                q = quads(texts[name])
                if q > QUAD_BUDGET:
                    problems.append("%s: %d quad lines, over the budget of %d" % (name, q, QUAD_BUDGET))
            old_sk, old_gl = os.path.join(SKINS, wid + ".png"), os.path.join(SKINS, wid + "_glow.png")
            if dry:
                print("would install %s: %d mesh(es), skin %s" % (wid, len(texts), "kept in flat_parts_old" if os.path.exists(old_sk) else "new"))
                continue
            os.makedirs(OLD_SKINS, exist_ok=True)
            for src in (old_sk, old_gl):
                dst = os.path.join(OLD_SKINS, os.path.basename(src))
                if os.path.exists(src) and not os.path.exists(dst):
                    shutil.copyfile(src, dst)
            for name, t in texts.items():
                with open(os.path.join(MESHES, name), "w", encoding="utf-8", newline="\n") as f:
                    f.write(t)
            for src, dst in ((sk, old_sk), (sk[:-4] + "_glow.png", old_gl)):
                shutil.copyfile(src, dst)
                pack(dst)
            kb = (os.path.getsize(old_sk) + os.path.getsize(old_gl)) / 1024.0
            if kb > SKIN_KB:
                problems.append("%s: skin + glow %.0f KB, over %.0f KB" % (wid, kb, SKIN_KB))
            done.append((wid, len(texts), kb))
            print("installed %s: %d mesh(es), skin + glow %.0f KB, quad lines %s" % (wid, len(texts), kb, sorted({quads(t) for t in texts.values()})))
    for p in problems:
        print("  ! " + p)
    print("%d weapon(s) installed, %d problem(s)" % (len(done), len(problems)))
    return 1 if problems else 0


def pin_grips(ids):
    """After `grip_table.py --write`: a mesh whose automatic grip point moved (the finder reads the vertices round the hand, a fatter arrow there shifts it) is given a `gripxyz` line with
    the point of the committed golden (git HEAD), so the hand holds the weapon exactly where it did. Run grip_table.py --write again afterwards. -> the ids that were pinned."""
    import json
    import subprocess
    head = json.loads(subprocess.run(["git", "show", "HEAD:tools/weapon_grip/grip_golden.json"], cwd=ROOT, capture_output=True, text=True, check=True).stdout)
    with open(os.path.join(ROOT, "tools", "weapon_grip", "grip_golden.json"), encoding="utf-8") as f:
        now = json.load(f)
    pinned = []
    for mid in sorted(ids):
        h, n = head.get(mid), now.get(mid)
        if not h or not n or n.get("source") == "gripxyz" and h.get("source") == "gripxyz":
            continue
        if max(abs(h[k] - n[k]) for k in "xyz") <= 0.05:
            continue
        path = os.path.join(MESHES, mid + ".txt")
        with open(path, encoding="utf-8") as f:
            lines = f.read().split("\n")
        lines = [ln for ln in lines if not ln.startswith("gripxyz")]
        at = next((i for i, ln in enumerate(lines) if ln.startswith("orient")), 0) + 1
        lines.insert(at, "gripxyz %.3f %.3f %.3f" % (h["x"], h["y"], h["z"]))
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lines))
        pinned.append(mid)
        print("pinned the grip of %s to %.3f %.3f %.3f" % (mid, h["x"], h["y"], h["z"]))
    return pinned


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("dirs", nargs="*")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--pin-grips", action="store_true", help="after grip_table.py --write: give every changed mesh whose grip point moved a gripxyz line with the committed point")
    a = ap.parse_args(argv)
    if a.pin_grips:
        ids = [os.path.basename(p)[:-4] for p in glob.glob(os.path.join(MESHES, "*.txt")) if made_solid(p)]
        print("%d grip(s) pinned" % len(pin_grips(ids)))
        return 0
    return install(a.dirs, a.dry_run)


if __name__ == "__main__":
    sys.exit(main())
