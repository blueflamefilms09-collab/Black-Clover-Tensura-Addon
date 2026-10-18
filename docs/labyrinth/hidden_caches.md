# Hidden relic caches in ordinary floors (1.75.0)

Relics were findable only through the Crucible of Choice (one capped relic per region). The owner asked that relics can also be found in the **hidden rooms of regular floors**.

- **Which rooms:** the Mega-Dungeon's treasure rooms (`structure/mega/treasure/*.nbt`: three closets and the twelve themed nooks and vaults). Their chests now roll `nusmp:chests/hidden_cache` (was `nusmp:chests/labyrinth_trial`, which is kept and still included by the new table, so the plain loot is unchanged).
- **What a cache holds:** the trial loot, plus a 30% chance per chest of one relic of **Normal to Rare** grade (`nusmp:relic` loot entries), falling back to a few emeralds when every relic of the range already lives. The table is a datapack file: server owners can retune the chance and grades.
- **Why only up to Rare:** Unique and above are hard-capped by the God of Order's ledger and, in the Mega-Dungeon, belong to the Crucible alone. `RelicExclusivity.allowsWildRoll(inMega, maxGrade)` allows a roll inside the Mega-Dungeon only when its highest grade is at most `HIDDEN_CACHE_MAX` (Rare); everywhere else nothing changed, and Mimics still drop no relic in the Mega-Dungeon.
- **Regenerating rooms:** the room generators still name the old table; run `python3 tools/labyrinth/retarget_treasure_loot.py` afterwards (`--check` for CI). `RelicCacheTemplatesTest` fails if a treasure chest or the table's grades drift.
