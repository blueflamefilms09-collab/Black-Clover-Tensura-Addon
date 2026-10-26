"""Row builders for the Unique Skill Lines (see docs/skillbooks_unique.md and catalog.py).

A franchise file in rows/ defines LINES = [Line(...), ...]; gen_lines.py turns every Line into data/nusmp/skill_trees/<id>.json.
WHY a builder and not raw JSON: the layout (stance -> stats and arts -> passive synergy -> finishers), the costs, the level tables and the prerequisite wiring are the same
for all 275 lines, so a content author writes only what is THEIRS (names, flavour, which kind with which numbers) and the generator makes the tree.
"""
from dataclasses import dataclass, field
from typing import Dict, List, Optional


@dataclass
class Stance:
    name: str
    desc: str
    icon: str = "minecraft:shield"
    # any of the stance params of catalog.STANCE (damage_pct, armor, speed_pct, knockback_res, regen, crit_chance) plus vfx
    params: Dict = field(default_factory=dict)


@dataclass
class Stat:
    name: str
    desc: str
    attribute: str          # catalog.ATTRIBUTES
    amount: float           # level-1 amount (grows .25 per level by catalog)
    op: str = "add"         # add | mult
    icon: str = "minecraft:iron_ingot"


@dataclass
class Art:
    name: str
    desc: str
    kind: str               # catalog.ACTIVE
    params: Dict = field(default_factory=dict)   # numbers override the catalogue base values; strings pick enums (effect, vfx, ...)
    icon: str = "minecraft:blaze_rod"
    ultimate: bool = False  # 3.26.0: the Ultimate Evolution art (only the LAST finisher of a line that has `evolution`)


@dataclass
class Pas:
    name: str
    desc: str
    kind: str               # catalog.PASSIVE
    params: Dict = field(default_factory=dict)
    icon: str = "minecraft:amethyst_shard"
    evolved: Dict = field(default_factory=dict)  # 3.26.0: params that replace the passive's numbers once the line has evolved (same names as `params`, same ranges)


@dataclass
class Line:
    id: str                 # file stem and route id suffix: nusmp:<id>; lower snake, unique across ALL franchises
    name: str               # the line's display name
    franchise: str          # one of FRANCHISES
    desc: str               # what the line is about (2 sentences)
    color: str              # '#rrggbb'
    background: str         # one of catalog.BACKGROUNDS (the generated gui background theme)
    icon: str               # item id
    book_name: str
    book_lore: str
    stances: List[Stance] = field(default_factory=list)      # 2
    stats: List[Stat] = field(default_factory=list)          # 3 to 4
    arts: List[Art] = field(default_factory=list)            # 6 to 8
    passives: List[Pas] = field(default_factory=list)        # 6 to 8
    finishers: List[Art] = field(default_factory=list)       # 2 (the ultimate techniques)
    loot_weight: int = 1
    evolution: Optional[Dict] = None  # 3.26.0: {"requirement": kind, "count": N, "text": "...", "ultimate_name": "..."} (catalog.EVOLUTION)


FRANCHISES = {
    "exiled_heavy_knight": "The Exiled Heavy Knight",
    "tensura": "That Time I Got Reincarnated as a Slime",
    "black_clover": "Black Clover",
    "magi": "Magi: The Labyrinth of Magic",
    "solo_leveling": "Solo Leveling",
    "danmachi": "DanMachi",
    "tbate": "The Beginning After the End",
    "tower_of_god": "Tower of God",
    "god_of_high_school": "The God of High School",
    "omniscient_readers_viewpoint": "Omniscient Reader's Viewpoint",
}
