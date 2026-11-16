"""Why: the Floor Warden's DEFAULT look in the game is the player-model kit's body (client.playermodel.dungeon is on by default), painted from tools/arc/char_floor_warden.py by
tools/dungeon_skins/special.py. gen_dungeon_skins.py's `special` step rewrites the Living Armor, the Goliath and the three soul-corpse puppets as well; after the Warden's rework only the
Warden's own skin, glow map, extras model (pmx_floor_warden) and table row must change, so this script runs that one row and merges it into looks_dungeon.json:

    python3 -B tools/dungeon_bosses/regen_warden_look.py
"""
import os
import sys

sys.dont_write_bytecode = True
TOOLS = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
sys.path.insert(0, TOOLS)
sys.path.insert(0, os.path.join(TOOLS, "dungeon_skins"))

import gen_dungeon_skins as G  # noqa: E402
import special  # noqa: E402

special.ARC = tuple(r for r in special.ARC if r[0] == "floor_warden")
new = special.arc_looks()
table = G.load_table()
table.update(new)
G.save_table(table)
print("floor_warden look rewritten:", new["floor_warden"])
