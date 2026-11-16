"""Append-only hook of tools/relics/gen_worn_all.py: after that script rewrites relic_worn/catalog.txt, this puts the Saiyan alloy armour entries (models made in Blender by
tools/blender/saiyan_armor.py, so they have no builder in gen_worn_all) back at its end. Idempotent: running it twice adds nothing."""
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
CATALOG = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "relic_worn", "catalog.txt")

LINES = [
    "# 3.x Saiyan alloy armour (craftable set, tools/blender/saiyan_armor.py): scouter, battle armour with pauldrons and vambraces, waist guard, greaves",
    "item saiyan_alloy_scouter saiyan_alloy_scouter",
    "item saiyan_alloy_battle_armor saiyan_alloy_chest saiyan_alloy_pauldron@rarm saiyan_alloy_pauldron@larm~ saiyan_alloy_vambrace@rarm saiyan_alloy_vambrace@larm~",
    "item saiyan_alloy_waist_guard saiyan_alloy_skirt",
    "item saiyan_alloy_greaves saiyan_alloy_greave@rleg saiyan_alloy_greave@lleg~",
    "ipal saiyan_alloy_scouter 7C8496 E2B536 44E890",
    "ipal saiyan_alloy_battle_armor E9ECF2 E2B536 BFEFFF",
    "ipal saiyan_alloy_waist_guard 2A3F9A E2B536 BFEFFF",
    "ipal saiyan_alloy_greaves E9ECF2 E2B536 BFEFFF",
    "itier saiyan_alloy_scouter 2",
    "itier saiyan_alloy_battle_armor 2",
    "itier saiyan_alloy_waist_guard 2",
    "itier saiyan_alloy_greaves 2",
]


def append():
    text = open(CATALOG, encoding="utf8").read()
    if "item saiyan_alloy_scouter " in text:
        return False
    with open(CATALOG, "a", encoding="utf8", newline="\n") as f:
        f.write(("" if text.endswith("\n") else "\n") + "\n".join(LINES) + "\n")
    return True


if __name__ == "__main__":
    print("appended" if append() else "already there")
