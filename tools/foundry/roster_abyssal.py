"""Why: the 200 mobs of the ABYSSAL road of the Mega-Dungeon (floors -1..-1000), 40 for each of the five tiers (tier t = floors 200(t-1)+1 .. 200t of depth, DndRules.tierOf).
The Abyssal road is the descent: demons, the restless dead, cults, oozes, insectoids, hags, aberrations and hollow knights, and everything that lives where no light was sent.
One row per line, eight fields separated by ' | ' (format and every token: tools/foundry/roster_common.py and docs/labyrinth/foundry.md):

    Name | faction | build | palette (3 colour names) | source | look tokens | archetype | abilities

The `source` is the series or D&D monster the DESIGN is inspired by; names and looks are the mod's own, never a copied character. Rows are appended, never rewritten.
"""

TIERS = {}

TIERS[1] = """
Gutter Imp | demon | small | crimson soot gold | D&D: imp | skin=red horns=small tail=devil wings=bat/ember hair=bald face=sinister/amber top=bare legs=shorts boots=bare prop=dagger | skirmisher | shadow_dash conjure_imps
Cellar Ghoul | undead | lanky | bone mud rust | D&D: ghoul | skin=undead hair=shaggy/ash face=mad/lumen/cracks+veins top=tunic/knee/-/runes ragged sleeves=torn legs=baggy boots=bare gloves=wraps | assassin | vampiric_bite lunge
Hollow Squire | undead | average | iron soot rust | Berserk | skin=undead mask=skull top=tabard/knee/chain armor=pauldrons+helm/chain cape=tattered prop=sword off=shield | tank | shield_bash bulwark
Candle Acolyte | cultist | slim | wine ink amber | Overlord | skin=pale hair=hood/ink face=cold/amber top=robe/floor/-/runes neck=necklace prop=grimoire acc=ear_cuffs | caster | shadow_bolt hex
"""
