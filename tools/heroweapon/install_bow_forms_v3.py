#!/usr/bin/env python3
"""install_bow_forms_v3: puts the six bows of the third look into the mod and keeps the looks they replace (4.59.0, docs/heroweapon/bow_designs.md).

    python3 -B tools/blender/forms_legendary_bow_v3.py --trial DIR             # build (rest + three draw stages + skin + glow skin per form)
    python3 -B tools/heroweapon/install_bow_forms_v3.py DIR [DIR ...] [--dry-run]

Per form (hunters_bow, scouts_bow, bird_hunting, frozen_rain, eagle_piercer, sagittarius), in this order:
  1. ARCHIVE the look that ships now: the rest mesh and the three draw stages go to weapon_meshes/classic/, the skin and glow skin to textures/entity/weapon/classic/ (never overwrites a
     copy that is already there). WeaponRenderer reads that folder for every `legendary_*` id when the client setting classicHeroWeaponModels is on, so the setting that restores the old
     models keeps working for these forms with no Java change.
  2. INSTALL the new meshes (the `# blender-solid` header line kept, no `gripxyz` copied: the old point was measured on another riser, grip_table.py --write finds the new one) and the new skins.
  3. CHECK: at most 5000 quad lines per mesh, skin + glow skin at most 350 KB together.
Idempotent. Afterwards: register_form_meshes.py, bettercombat/inventory.py, weapon_grip/grip_table.py --write, weapon_grip/hold_audit.py --write.
"""
import argparse
import glob
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import install_solid_meshes as ism  # noqa: E402

FORMS = ["hunters_bow", "scouts_bow", "bird_hunting", "frozen_rain", "eagle_piercer", "sagittarius"]
MARK3 = "# third look 4.59.0 (docs/heroweapon/bow_designs.md)"
CLASSIC_M = os.path.join(ism.MESHES, "classic")
CLASSIC_S = os.path.join(ism.SKINS, "classic")


def archive(form, dry):
    wid = "legendary_bow__" + form
    n = 0
    for name in [wid] + [wid + "_pull%d" % i for i in (1, 2, 3)]:
        src, dst = os.path.join(ism.MESHES, name + ".txt"), os.path.join(CLASSIC_M, name + ".txt")
        if os.path.exists(src) and not os.path.exists(dst):
            n += 1
            if not dry:
                os.makedirs(CLASSIC_M, exist_ok=True)
                shutil.copyfile(src, dst)
    for suf in (".png", "_glow.png"):
        src, dst = os.path.join(ism.SKINS, wid + suf), os.path.join(CLASSIC_S, wid + suf)
        if os.path.exists(src) and not os.path.exists(dst):
            n += 1
            if not dry:
                os.makedirs(CLASSIC_S, exist_ok=True)
                shutil.copyfile(src, dst)
    return n


def install(dirs, dry=False):
    problems, done = [], 0
    for d in dirs:
        mdir, sdir = ism.layout(d)
        for form in FORMS:
            wid = "legendary_bow__" + form
            sk = os.path.join(sdir, wid + ".png")
            if not (os.path.exists(sk) and os.path.exists(sk[:-4] + "_glow.png")):
                continue
            meshes = sorted(p for p in glob.glob(os.path.join(mdir, "*.txt")) if re.match(r"^%s(_pull[123])?\.txt$" % re.escape(wid), os.path.basename(p)))
            if len(meshes) != 4:
                problems.append("%s: %d meshes in %s (want rest + 3 draw stages)" % (wid, len(meshes), mdir))
                continue
            texts = {}
            for m in meshes:
                with open(m, encoding="utf-8") as f:
                    lines = f.read().split("\n")
                lines = [ln for ln in lines if not ln.startswith(("# blender-solid", "# third look", "gripxyz"))]
                at = 1 if lines[0].startswith("#") else 0
                lines[at:at] = [ism.MARK, MARK3]
                texts[os.path.basename(m)] = "\n".join(lines)
                q = ism.quads(texts[os.path.basename(m)])
                if q > ism.QUAD_BUDGET:
                    problems.append("%s: %d quad lines, over the budget of %d" % (os.path.basename(m), q, ism.QUAD_BUDGET))
            kb = (os.path.getsize(sk) + os.path.getsize(sk[:-4] + "_glow.png")) / 1024.0
            if kb > ism.SKIN_KB:
                problems.append("%s: skin + glow %.0f KB, over %.0f KB" % (wid, kb, ism.SKIN_KB))
            n = archive(form, dry)
            print("%s: %d classic copies %s, %d meshes %s, skin + glow %.0f KB, quad lines %s" % (wid, n, "would be made" if dry else "made", len(texts), "would be written" if dry else "written", kb, sorted({ism.quads(t) for t in texts.values()})))
            if dry:
                continue
            for name, t in texts.items():
                with open(os.path.join(ism.MESHES, name), "w", encoding="utf-8", newline="\n") as f:
                    f.write(t)
            for suf in (".png", "_glow.png"):
                shutil.copyfile(os.path.join(sdir, wid + suf), os.path.join(ism.SKINS, wid + suf))
            done += 1
    for p in problems:
        print("  ! " + p)
    print("%d form(s) installed, %d problem(s)" % (done, len(problems)))
    return 1 if problems else 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("dirs", nargs="+")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    sys.exit(install(a.dirs, a.dry_run))
