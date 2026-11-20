"""Keeps the OLD katanas (rule 2: add the new, keep the old). Idempotent; run it BEFORE katanas.py or a form builder replaces a mesh.

    python3 -B tools/blender/katanas_classic.py            copy every old katana mesh and skin into the classic_vassal folders
    python3 -B tools/blender/katanas_classic.py --retire   then move the thickened copies (the 1.110.0 size rule) out of the way so the game draws the new thin mesh

The repo's convention for "the model of before a remodel" is the classic_vassal folders (WeaponRenderer.isRemodelledVassalForm, the vassalForms.classic setting, ClassicVassalFilesTest):
  weapon_meshes/classic_vassal/<id>.txt                  the mesh as it was
  textures/entity/weapon/classic_vassal/<id>(_glow).png  its skins
  weapon_meshes_sz/classic_vassal/<id>.txt               the thickened copy of it: what the game drew since 1.110.0 (WeaponSizeRule.szPath maps the classic path to it, so the classic setting
                                                         shows exactly the old look, slab and all)
--retire moves weapon_meshes_sz/<id>.txt (and sz/up, vu_sz) of the rebuilt ids into those classic folders. Nothing is deleted.
Hellblaze (weapon_meshes/up/..., textures/entity/weapon/up/...) goes to the same names under up/classic_vassal: kept on disk, not wired to the setting (its renderer is the visual-upgrade one).
An earlier run of this tool used the folder name classic_katana: those files are copied into classic_vassal first (--drop-staging removes the emptied staging folders).
"""
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
CL = "classic_vassal"
OLDNAME = "classic_katana"
IDS = ["bloodbound_katana", "demon_slasher_katana", "miasma_infused_katana", "hakurou_school_practice_katana", "selection_hopefuls_bokken", "katana_of_carnage",
       "katana_of_the_crimson_shogun", "shadowsteel_katana", "void_origin_katana", "corpse_eater", "ilmare_gin_the_starlight_silver", "kusanagi_the_grass_cutter",
       "dragon_tooth_blade_red", "wavebreaker",
       "vassal_katana", "vassal_katana__white_tiger_katana", "vassal_katana__soul_eater_katana", "vassal_katana__siderite_katana", "vassal_katana__sakura_stone_katana",
       "legendary_sword__wind_king_katana"]
HELLBLAZE = ["hellblaze_the_demon_sin__katana", "hellblaze_the_demon_sin__katana_lod", "hellblaze_the_demon_sin__saya"]
# upgrade / Genesis variants whose thickened copy is retired too (their bodies stay: they are generated with their effects, see docs/heroweapon/katanas.md)
UP = ["kusanagi_the_grass_cutter", "kusanagi_the_grass_cutter_lod"]
VU = ["void_origin_katana", "void_origin_katana_lod"]


def cp(src, dst):
    if os.path.exists(src) and not os.path.exists(dst):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(src, dst)
        print("copied", os.path.relpath(dst, ROOT))


def mv(src, dst):
    if os.path.exists(src):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        if os.path.exists(dst):
            os.remove(src)
        else:
            shutil.move(src, dst)
        print("moved ", os.path.relpath(src, ROOT), "->", os.path.relpath(dst, ROOT))


def migrate():
    """classic_katana (the first name of this change) -> classic_vassal: copied; the staging folders are removed only with --drop-staging (once every file is in classic_vassal)."""
    for base in (("weapon_meshes",), ("textures", "entity", "weapon"), ("weapon_meshes_sz",)):
        d = os.path.join(A, *base, OLDNAME)
        if os.path.isdir(d):
            names = sorted(os.listdir(d))
            for n in names:
                cp(os.path.join(d, n), os.path.join(A, *base, CL, n))
            if "--drop-staging" in sys.argv and all(os.path.exists(os.path.join(A, *base, CL, n)) for n in names):
                shutil.rmtree(d)
                print("removed the staging folder", os.path.relpath(d, ROOT))


def main():
    migrate()
    M, S, Z = os.path.join(A, "weapon_meshes"), os.path.join(A, "textures", "entity", "weapon"), os.path.join(A, "weapon_meshes_sz")
    for i in IDS:
        cp(os.path.join(M, i + ".txt"), os.path.join(M, CL, i + ".txt"))
        for suf in ("", "_glow"):
            cp(os.path.join(S, i + suf + ".png"), os.path.join(S, CL, i + suf + ".png"))
        cp(os.path.join(Z, i + ".txt"), os.path.join(Z, CL, i + ".txt"))
    for i in HELLBLAZE:
        cp(os.path.join(M, "up", i + ".txt"), os.path.join(M, "up", CL, i + ".txt"))
        for suf in ("", "_glow", "_veins"):
            cp(os.path.join(S, "up", i + suf + ".png"), os.path.join(S, "up", CL, i + suf + ".png"))
    if "--retire" in sys.argv:
        for i in IDS:
            mv(os.path.join(Z, i + ".txt"), os.path.join(Z, CL, i + ".txt"))
        for i in UP:
            cp(os.path.join(M, "up", i + ".txt"), os.path.join(M, "up", CL, i + ".txt"))
            for suf in ("", "_glow"):
                cp(os.path.join(S, "up", i + suf + ".png"), os.path.join(S, "up", CL, i + suf + ".png"))
            mv(os.path.join(Z, "up", i + ".txt"), os.path.join(Z, "up", CL, i + ".txt"))
        for i in VU:
            cp(os.path.join(A, "weapon_meshes_vu", i + ".txt"), os.path.join(A, "weapon_meshes_vu", CL, i + ".txt"))
            mv(os.path.join(A, "weapon_meshes_vu_sz", i + ".txt"), os.path.join(A, "weapon_meshes_vu_sz", CL, i + ".txt"))


if __name__ == "__main__":
    main()
