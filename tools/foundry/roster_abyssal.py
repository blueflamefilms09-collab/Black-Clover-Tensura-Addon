"""Why: the 200 mobs of the ABYSSAL road of the Mega-Dungeon (floors -1..-1000), 40 for each of the five tiers (tier t = floors 200(t-1)+1 .. 200t of depth, DndRules.tierOf).
The Abyssal road is the descent: demons, the restless dead, cults, oozes, insectoids, hags, aberrations and hollow knights, and everything that lives where no light was sent.
One row per line (in rows/abyssal_t<tier>*.txt, one or more files per tier; `#` starts a comment line), eight fields separated by ' | ' (format and every token: tools/foundry/roster_common.py and docs/labyrinth/foundry.md):

    Name | faction | build | palette (3 colour names) | source | look tokens | archetype | abilities

The `source` is the series or D&D monster the DESIGN is inspired by; names and looks are the mod's own, never a copied character. Rows are appended, never rewritten.
"""

import glob
import os

_HERE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rows")
TIERS = {}
for _t in range(1, 6):                       # a tier may be split over several files (abyssal_t2a.txt, abyssal_t2b.txt ...): they are read in name order
    TIERS[_t] = "".join(open(_p).read().rstrip("\n") + "\n" for _p in sorted(glob.glob(os.path.join(_HERE, "abyssal_t%d*.txt" % _t))))
