"""The tab layout of the advancements (4.101.0): which advancement heads which tab, and which branch hangs under which tab.

Why: the game draws an advancement tab as a tidy tree and decides every position from the SHAPE of the tree alone (an advancement file has no x/y), so one root with 454 shown
advancements was a single column about 330 rows tall (the Magic branch alone 130 rows): players had to drag through screens of nothing to find anything. The branches are now split
over themed tabs, each a short tree of its own. Nothing about an advancement changes but its parent: ids, titles, criteria, rewards and the shared tab background are untouched, so saved
progress, the quest book and every test that reads them keep working. A tab head is a new advancement that completes on the first tick (like the old root), so every tab is always there.

Set the environment variable ADV_LAYOUT=classic before running gen_advancements.py to write the old single-tree layout again (the setting that restores the old behaviour).
"""
import os

import spec_lib as L

# (tab path, title, description, icon (None = the icon of the first branch), [branch heads that hang directly under the tab head])
TABS = [
    ("tab/magic", "Magic", "Awaken your magic, count the kinds you know and master whole branches of it. The souls have their own tabs.",
     None, ["magic/awakening"]),
    ("tab/souls_elements", "Elemental Souls", "Flame, Water, Wind and Light: the magic of each soul and the signature of its master.",
     "anime_multiverse:grimoire", ["magic/soul_flame", "magic/soul_water", "magic/soul_wind", "magic/soul_light"]),
    ("tab/soul_earth", "Earth Soul", "Every magic born of the Earth soul, from stone and sand to vines and the World Tree.",
     None, ["magic/soul_earth"]),
    ("tab/souls_space_time", "Space, Time and Empty Souls", "The magic of space, of time and of the empty soul.",
     None, ["magic/soul_space", "magic/soul_time", "magic/soul_empty"]),
    ("tab/souls_battle_dark", "Battle and Darkness Souls", "The magic of the body and of the dark: blood, bone, steel, shadow, curses.",
     None, ["magic/soul_battle", "magic/soul_darkness"]),
    ("tab/soul_fantasy", "Fantasy Soul", "Every magic born of the Fantasy soul: dreams, threads, songs, dice and more.",
     None, ["magic/soul_fantasy"]),
    ("tab/spells", "Spells", "Signature casts, merged spells, engravings and the strange arts.",
     None, ["spells/signature"]),
    ("tab/character_pages", "Character Pages", "Walk in the shoes of the characters whose pages you can cast.",
     None, ["spells/characters"]),
    ("tab/bosses", "Bosses and Trials", "Every boss of the Multiverse and the long deeds that prove a champion.",
     None, ["bosses/first", "challenges/root"]),
    ("tab/relics", "Relics and Weapons", "Relics of many worlds, their grades and forges, and the mod's named weapons.",
     None, ["relics/first", "weapons/first"]),
    ("tab/dungeon", "Dungeons", "The Mega-Dungeon, the Labyrinth and everything that lives in them.",
     None, ["dungeon/entered", "dungeon/mega"]),
    ("tab/story", "Story Arcs", "The arcs of the story in order, and the sagas of the Clover, Spade and Seven Deadly Sins worlds.",
     None, ["arcs/hage_village", "arc/root"]),
    ("tab/sagas_more", "Elf and DanMachi Sagas", "The Elf Reincarnation and DanMachi sagas and the characters in them.",
     None, ["arc/saga_elf_reincarnation", "arc/saga_danmachi"]),
]


FACES_PER_TAB = 16      # the "Met: ..." advancements of the arcs are one leaf each; sixteen to a tab keeps every tab short


def _faces(S):
    return [a.path for a in S.advs if a.parent == "arc/root" and a.path.startswith("arc/met_")]


def apply(S):
    if os.environ.get("ADV_LAYOUT", "").lower() == "classic":
        return
    bg = S.by_path["root"].background
    tabs = list(TABS)
    faces = _faces(S)
    per = -(-len(faces) // max(1, -(-len(faces) // FACES_PER_TAB)))           # even pages, never a page of one
    chunks = [faces[i:i + per] for i in range(0, len(faces), per)]
    for n, chunk in enumerate(chunks, 1):
        tabs.append(("tab/faces_%d" % n, "Faces of the Arcs %d" % n, "The characters of the arcs you have met (page %d of %d)." % (n, len(chunks)), None, chunk))
    for path, title, desc, icon, heads in tabs:
        ic = icon or S.by_path[heads[0]].icon
        S.add(path, None, title, desc, ic, L.tick(), frame="task", toast=False, chat=False, background=bg)
    for path, title, desc, icon, heads in tabs:
        for h in heads:
            S.by_path[h].parent = path
