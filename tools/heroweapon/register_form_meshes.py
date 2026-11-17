#!/usr/bin/env python3
"""Registers the hero weapon form meshes the Blender builders made (docs/heroweapon/form_models.md).

Reads every tools/blender/forms_manifest_*.json ({meshid: {weapon, form, class}}), adds meshid -> class to tools/relics/weapon_classes.json ("weapons"), then runs the
existing syncs so the game's copies match:
    python3 -B tools/relics/audit_weapon_scale.py --write
    python3 -B tools/weapon_grip/grip_table.py --write
    python3 -B tools/safe_additive/scan_textures.py --fix
Idempotent: a mesh that is already listed with the same class changes nothing; one listed with another class is reported and left alone. Bow draw stages
(<meshid>_pull1..3) need no entry: WeaponClasses.baseId strips the suffix, exactly as it does for legendary_bow_pull1.

    python3 -B tools/heroweapon/register_form_meshes.py [--no-sync] [--root DIR]
--root DIR works on another copy of the repo (the sync scripts then run from that copy); --no-sync only edits weapon_classes.json (used by the self-check below).
Self-check: python3 -B tools/heroweapon/register_form_meshes.py --selftest   (fake manifest in a temp copy; nothing in the repo is touched)
"""
import argparse
import glob
import json
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
SYNCS = [["tools/relics/audit_weapon_scale.py", "--write"], ["tools/weapon_grip/grip_table.py", "--write"], ["tools/safe_additive/scan_textures.py", "--fix"]]


def dump(obj):
    return json.dumps(obj, indent=2) + "\n"


def register(root, sync=True):
    """Returns (added, same, problems) and writes weapon_classes.json when something was added."""
    path = os.path.join(root, "tools", "relics", "weapon_classes.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    weapons, classes = data["weapons"], data["classes"]
    added, same, problems = [], [], []
    manifests = sorted(glob.glob(os.path.join(root, "tools", "blender", "forms_manifest_*.json")))
    for m in manifests:
        with open(m, encoding="utf-8") as f:
            entries = json.load(f)
        for mesh, e in sorted(entries.items()):
            cls = e.get("class")
            if "__" not in mesh or cls not in classes:
                problems.append("%s (%s): bad mesh id or unknown class %r" % (mesh, os.path.basename(m), cls))
            elif mesh in weapons and weapons[mesh] != cls:
                problems.append("%s: already listed as %r, manifest says %r (left alone)" % (mesh, weapons[mesh], cls))
            elif mesh in weapons:
                same.append(mesh)
            else:
                weapons[mesh] = cls
                added.append(mesh)
    if added:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(dump(data))
    print("%d manifest(s): %d added, %d already listed, %d problem(s)" % (len(manifests), len(added), len(same), len(problems)))
    for a in added:
        print("  + %s: %s" % (a, weapons[a]))
    for p in problems:
        print("  ! " + p)
    if sync:
        for cmd in SYNCS:
            r = subprocess.run([sys.executable, "-B"] + cmd, cwd=root)
            print("  sync %s: %s" % (cmd[0], "ok" if r.returncode == 0 else "exit %d" % r.returncode))
            if r.returncode != 0:
                problems.append("sync failed: " + cmd[0])
    return added, same, problems


def selftest():
    tmp = tempfile.mkdtemp(prefix="formreg_")
    try:
        os.makedirs(os.path.join(tmp, "tools", "relics"))
        os.makedirs(os.path.join(tmp, "tools", "blender"))
        shutil.copy(os.path.join(REPO, "tools", "relics", "weapon_classes.json"), os.path.join(tmp, "tools", "relics"))
        with open(os.path.join(tmp, "tools", "blender", "forms_manifest_fake.json"), "w") as f:
            json.dump({"legendary_bow__fake": {"weapon": "legendary_bow", "form": "fake", "class": "bow"},
                       "vassal_axe__bad": {"weapon": "vassal_axe", "form": "bad", "class": "no_such_class"}}, f)
        a, s, p = register(tmp, sync=False)
        assert a == ["legendary_bow__fake"] and not s and len(p) == 1, (a, s, p)
        a, s, p = register(tmp, sync=False)                    # idempotent
        assert not a and s == ["legendary_bow__fake"], (a, s)
        with open(os.path.join(tmp, "tools", "relics", "weapon_classes.json"), encoding="utf-8") as f:
            assert json.load(f)["weapons"]["legendary_bow__fake"] == "bow"
        print("selftest ok")
    finally:
        shutil.rmtree(tmp)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--root", default=REPO)
    ap.add_argument("--no-sync", action="store_true")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args(argv)
    if a.selftest:
        selftest()
        return 0
    added, same, problems = register(os.path.abspath(a.root), sync=not a.no_sync)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
