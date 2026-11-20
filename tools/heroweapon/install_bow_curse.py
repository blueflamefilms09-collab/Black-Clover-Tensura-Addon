#!/usr/bin/env python3
"""install_bow_curse: gives legendary_bow_curse its own stave and keeps the look it replaces (4.65.0, docs/heroweapon/curse_bow.md).

    python3 -B tools/blender/forms_legendary_bow_curse.py --trial DIR
    python3 -B tools/heroweapon/install_bow_curse.py DIR [--dry-run]

  1. ARCHIVE the look that ships now (the base bow's stave in violet: rest + three draw stages, skin, glow skin) to weapon_meshes/shared_stave/ and textures/entity/weapon/shared_stave/
     (never overwrites a copy that is already there). The `classic/` folder is NOT used: it already holds the pre-3.83 hand-made curse bow. WeaponRenderer.readFlatOld reads the
     shared_stave folder when the setting visual.heroSolidParts is off.
  2. INSTALL the new meshes (no `gripxyz` copied: the old point belongs to the old riser; grip_table.py --write finds the new one) and the new skins.
  3. CHECK: at most 5000 quad lines per mesh, skin + glow skin at most 350 KB together.
Idempotent. Afterwards: register_form_meshes.py, bettercombat/inventory.py, weapon_grip/grip_table.py --write, weapon_grip/hold_audit.py --write.
"""
import argparse
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import install_solid_meshes as ism  # noqa: E402

WID = "legendary_bow_curse"
NAMES = [WID] + [WID + "_pull%d" % i for i in (1, 2, 3)]
MARK = "# own stave 4.65.0 (docs/heroweapon/curse_bow.md)"
OLD_M = os.path.join(ism.MESHES, "shared_stave")
OLD_S = os.path.join(ism.SKINS, "shared_stave")


def archive(dry):
    n = 0
    for name in NAMES:
        src, dst = os.path.join(ism.MESHES, name + ".txt"), os.path.join(OLD_M, name + ".txt")
        if os.path.exists(src) and not os.path.exists(dst):
            n += 1
            if not dry:
                os.makedirs(OLD_M, exist_ok=True)
                shutil.copyfile(src, dst)
    for suf in (".png", "_glow.png"):
        src, dst = os.path.join(ism.SKINS, WID + suf), os.path.join(OLD_S, WID + suf)
        if os.path.exists(src) and not os.path.exists(dst):
            n += 1
            if not dry:
                os.makedirs(OLD_S, exist_ok=True)
                shutil.copyfile(src, dst)
    return n


def install(d, dry=False):
    mdir, sdir = ism.layout(d)
    sk = os.path.join(sdir, WID + ".png")
    problems, texts = [], {}
    for name in NAMES:
        p = os.path.join(mdir, name + ".txt")
        if not os.path.exists(p) or not os.path.exists(sk) or not os.path.exists(sk[:-4] + "_glow.png"):
            problems.append("%s: missing in %s" % (name, d))
            continue
        with open(p, encoding="utf-8") as f:
            lines = [ln for ln in f.read().split("\n") if not ln.startswith(("# blender-solid", "# own stave", "gripxyz"))]
        lines[1 if lines[0].startswith("#") else 0:0] = [ism.MARK, MARK]
        texts[name] = "\n".join(lines)
        q = ism.quads(texts[name])
        if q > ism.QUAD_BUDGET:
            problems.append("%s: %d quad lines, over the budget of %d" % (name, q, ism.QUAD_BUDGET))
    if problems:
        for p in problems:
            print("  ! " + p)
        return 1
    kb = (os.path.getsize(sk) + os.path.getsize(sk[:-4] + "_glow.png")) / 1024.0
    if kb > ism.SKIN_KB:
        print("  ! skin + glow %.0f KB, over %.0f KB" % (kb, ism.SKIN_KB))
        return 1
    n = archive(dry)
    print("%s: %d archive copies %s, %d meshes %s, skin + glow %.0f KB, quad lines %s" % (WID, n, "would be made" if dry else "made", len(texts), "would be written" if dry else "written", kb, sorted({ism.quads(t) for t in texts.values()})))
    if dry:
        return 0
    for name, t in texts.items():
        with open(os.path.join(ism.MESHES, name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
            f.write(t)
    for suf in (".png", "_glow.png"):
        shutil.copyfile(os.path.join(sdir, WID + suf), os.path.join(ism.SKINS, WID + suf))
    return 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    sys.exit(install(a.dir, a.dry_run))
