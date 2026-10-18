# Smoldering Fang of Insanity (1.81.0): the fourth Exiled Heavy Knight route

The Fang used to be only the NAME of the Fire Dragon Slayer's book (the shop text said "the target: the Fang of Smoldering Insanity" and the first route built for it was the Fire Dragon Slayer).
It is now a route of its own, next to Oath of the Heavy Armor, Basic Swordsmanship and Lady Luck, with its own book, class, skills and passives, and nothing to do with dragons or their fire.

* Route file: `data/nusmp/skill_trees/smoldering_fang_of_insanity.json` (`magic: exiled_heavy_knight`, class `smoldering_fang`, book `nusmp:smoldering_fang_book`, "Smoldering Fang of Insanity").
* Like the other three it works only while it is the EQUIPPED route (single-active-route rule); every passive asks `HkPassives.level` first.
* The Fire Dragon Slayer keeps its seven techniques and is untouched except for its book name, now *Heart of the Fire Dragon* (it was the Fang's name before). The shop's flagship line `fang_of_smoldering_insanity` now sells the Fang route's book; the Fire Dragon Slayer book is on the shelf like every other route.

| Node | Kind | What it does |
|---|---|---|
| Ember Bite | technique | Bite the foe in front: damage, Wither, heal a share of the damage. |
| Rabid Fangs | passive | Your blows grow stronger the lower your health is (up to the node's bonus). |
| Ash Skin | passive | A foe that strikes you may wither (chance, seconds, rest from the file). |
| Fit of Insanity | technique | Strength and Speed for a while; every blow you take hurts more (`vulnerability`). |
| Howl of Ruin | technique | Weakens, slows and shoves back the hostile creatures around you. |
| Fang of Insanity | Secret Art | Leap at the nearest foe, a huge withering bite, heal a share of it, then a short Weakness. Cooldown 150 to 180 s. |

Why this shape: the route is the berserker of the franchise (the tank has the Oath, the swordsman the Swordsmanship, the gambler Lady Luck); its price is paid in guard (the Fit of Insanity) and its reward is withering damage and life from the bite.
All numbers are in the file by node level; the level-1 values are the fallbacks in `HkRoute`, kept in step by `HkRouteFilesTest`. Code: `HkTechniques` (4 skills), `HkPassives` (Rabid Fangs, Ash Skin, the vulnerability of the Fit), `HeavyKnightSkills`, `HkRoute`.

Not verified in game: the numbers are a first balance guess, and the leap of the Fang is a simple velocity push. The names and numbers are the mod's own; I could not look the original skill up.
