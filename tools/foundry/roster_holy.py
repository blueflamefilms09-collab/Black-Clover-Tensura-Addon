"""Why: the 200 mobs of the HOLY road of the Mega-Dungeon (floors +1..+1000), 40 for each of the five tiers (tier t = floors 200(t-1)+1 .. 200t, DndRules.tierOf). Radiant
hosts, knightly orders, guild adventurers, fairy courts, beastfolk of the wilds, celestial constructs: the Holy road is the climb, so even its monsters wear the light.
One row per line, eight fields separated by ' | ' (format and every token: tools/foundry/roster_common.py and docs/labyrinth/foundry.md):

    Name | faction | build | palette (3 colour names) | source | look tokens | archetype | abilities

The `source` is the series or D&D monster the DESIGN is inspired by; names and looks are the mod's own, never a copied character. Rows are appended, never rewritten.
"""

TIERS = {}

TIERS[1] = """
Tunnel Kobold | beast | small | rust sand iron | D&D: kobold | skin=red ears=pointed horns=small tail=lizard hair=crop/ember face=grin/amber top=vest/-/leather legs=shorts boots=bare prop=dagger | skirmisher | dash swarm_call
Hearth Sprite | elemental | child | flame honey ember | Fairy Tail | skin=fair ears=elf wings=fairy hair=twin_tails/flame face=cheerful/amber top=dress/thigh legs=shorts boots=shoes prop=wand glow=sunfire | caster | fire_bolt mending_light
Dawn Squire | construct | average | white gold sky | Black Clover | skin=light hair=short/wheat face=stern/azure top=tabard/knee/plate armor=pauldrons+gorget cape=cape prop=sword off=shield | tank | shield_bash shield_wall
Greenwood Archer | beast | slim | moss leather sage | Dungeon Meshi | skin=tan hair=ponytail/chestnut face=calm/jade top=tunic/thigh cape=hooded_cloak/hood prop=bow acc=bandana+quiver | archer | volley hunters_mark
"""
