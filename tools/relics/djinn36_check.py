#!/usr/bin/env python3
"""djinn36_check: the CI check of the new Djinn's art, with no game. Exit 1 on a problem.

    python3 -B tools/relics/djinn36_check.py

Checks: every one of the 24 has both armour sets with the core pieces and under the frame budget (the same numbers as DjinnUniqueAssetsTest), the fallback rig,
every texture, the weapon mesh / skins / models / icons and the seal (72 item ids), the lang fragment, DjinnRoster36.java in step with the roster table, the
motion rows and ambient recipes distinct, and tools/relics/djinn_weapons.py --check (the weapon files are what the tool writes).
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import djinn36_roster as R                      # noqa: E402
import djinn36_doc as D                         # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
CORE = ["helm", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l", "gauntlet_r", "greave_l", "greave_r", "crest", "wing_in_l", "wing_in_r",
        "wing_out_l", "wing_out_r", "weapon", "backdrop"]
BUDGET = 5200
bad = []


def need(path, what=""):
    if not os.path.exists(path):
        bad.append("missing " + os.path.relpath(path, ROOT) + (" " + what if what else ""))
        return False
    return True


def quads(path):
    return sum(1 for ln in open(path) if ln.startswith("qn"))


def main():
    for n in R.NAMES:
        i = n.lower()
        for pre in ("u", "d"):
            d = os.path.join(A, "djinn", f"{pre}_{i}")
            tot = 0
            for piece in CORE:
                p = os.path.join(d, piece + ".txt")
                if need(p):
                    q = quads(p)
                    if q == 0 or q >= 800:
                        bad.append(f"{pre}_{i}/{piece}: {q} quads")
            for f in os.listdir(d) if os.path.isdir(d) else []:
                q = quads(os.path.join(d, f))
                tot += q * (5 if f == "tail_seg.txt" else 1)
            if not 2000 <= tot <= BUDGET:
                bad.append(f"{pre}_{i}: {tot} quads in a Full Equip (2000 to {BUDGET})")
            if (pre == "u") == os.path.exists(os.path.join(d, "eye.txt")):
                bad.append(f"{pre}_{i}: the eye crest belongs to the dark set only")
            tail = os.path.exists(os.path.join(d, "tail_seg.txt"))
            if tail == (n in R.TAIL_NONE):
                bad.append(f"{pre}_{i}: tail piece {'present' if tail else 'missing'} against the Look")
        need(os.path.join(A, "djinn", i, "chest.txt"), "(fallback rig)")
        need(os.path.join(A, "djinn", i, "sheath.txt"), "(fallback rig)")
        ent = os.path.join(A, "textures", "entity", "djinn")
        for f in (f"{i}_armor.png", f"{i}_armor_glow.png", f"{i}_u_armor.png", f"{i}_u_armor_glow.png", f"{i}_d_armor.png", f"{i}_d_armor_glow.png", f"{i}_rune.png", f"{i}_u_rune.png",
                  f"{i}_d_rune.png", f"{i}_d_art.png", f"{i}_bust.png", f"{i}_d_bust.png"):
            need(os.path.join(ent, f))
    for item in D.item_ids():
        if item.startswith("djinn_seal_"):
            need(os.path.join(A, "textures", "item", item + ".png"))
            need(os.path.join(A, "models", "item", item + ".json"))
        else:
            for p in (f"weapon_meshes/{item}.txt", f"textures/entity/weapon/{item}.png", f"textures/entity/weapon/{item}_glow.png", f"models/item/{item}.json", f"models/item/{item}_icon.json",
                      f"textures/item/{item}.png"):
                need(os.path.join(A, *p.split("/")))
    lang = json.load(open(os.path.join(ROOT, "tools", "lang", "djinn36_art.json"), encoding="utf-8"))
    if set(lang) != set(D.lang()):
        bad.append("tools/lang/djinn36_art.json is out of date")
    if subprocess.call([sys.executable, "-B", os.path.join(HERE, "djinn36_java.py"), "--check"]) != 0:
        bad.append("DjinnRoster36.java is out of date (run tools/relics/djinn36_java.py)")
    rows = [R.style_row(n) for n in R.NAMES]
    if len(set(rows)) != 24:
        bad.append("two of the 24 share a motion row")
    amb = [R.AMBIENT[n] for n in R.NAMES]
    if len({(a[0], a[2], a[4]) for a in amb}) != 24:
        bad.append("two of the 24 shed exactly the same particles")
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "djinn_weapons.py"), "--check"], capture_output=True, text=True)
    if r.returncode != 0:
        bad.append("djinn_weapons.py --check: " + r.stdout.strip().splitlines()[-1])
    for b in bad:
        print("PROBLEM", b)
    print("djinn36_check:", "OK" if not bad else f"{len(bad)} problem(s)")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
