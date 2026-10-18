"""Why: the FLOOR MODIFIER bases (125 mechanics once expanded): rules that colour a whole floor, written as design cards (see dsl.py). Each card cites the series or rulebook it
is inspired by; the text is the mod's own words. Floor-wide scope: the Director draws at most two of these per floor and never two that share a channel."""
from dsl import *

BASES = [
    base("rim_curse", "The Curse of the Rim", "Made in Abyss",
         "A sickness rides every delver who has gone deep: now and then the world swims and the stomach turns, and the further down the floor, the worse it bites.",
         "Going down is a gift. Coming back is the price.", "floor",
         T("timer", every=(420, -40)),
         [E("effect", id="minecraft:nausea", secs=(5, 0.6), amp=0), E("starve", amount=(2, 0.4))],
         cost=3, weight=12, themes=["abyss", "fungal_underdark", "drowned_crypt"],
         elems=[El("wither", "Rotting Rim", "The sickness is a creeping rot that eats health instead of the stomach.", swap={1: ("effect", dict(id="minecraft:wither", secs=(3, 0.3), amp=0))}),
                El("bleed", "Bleeding Rim", "Each wave opens the old wounds and drains the legs of their strength.", swap={0: ("effect", dict(id="minecraft:weakness", secs=(6, 0.6), amp=0))}, cost=1)],
         trigs=["sprint", "damage_taken"],
         ctrs=[("hold_light", dict(secs=10)), ("stand_plate", dict(secs=8))]),

    base("mana_tide", "The Ebb and Flow of Magic", "Black Clover",
         "The floor's mana rises and falls like a tide: while it ebbs the delvers' magicules drain away, and the pulse repeats until the tide is turned.",
         "Mana is a living thing, and it breathes.", "floor",
         T("timer", every=(300, -30)),
         [E("drain_mana", pct=(6, 1.2)), E("rukh", align="dark", shape="aura", count=8)],
         cost=4, weight=12, themes=["astral", "spatial", "storm"],
         elems=[El("surge", "Surging Tide", "At the crest the tide throws the mana back with a shock that hurts.", add=[E("damage", amount=(1, 0.4), kind="magic", delay=20)]),
                El("blessing", "Tide of Plenty", "The tide gives as often as it takes: every wave also restores a share of the drained magicules, but bites hungrily afterwards.", add=[E("restore_mana", pct=(4, 0.8), delay=60)], cost=-1)],
         trigs=["area_enter", "party_size"],
         ctrs=[("lever", {}), ("kill_marked", dict(group="arcane"))]),
]
