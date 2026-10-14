# Boss VFX and buffs, part B3 (1.35.0): Zagred, the Blood-Red Commander, the Juggernaut, the Gatekeeper and the Grimoire Daemons

Addendum to `boss_overhaul.md`. Everything below only sends VFX packets and adds rules; no block is placed or broken. Nothing was run in game.

## Zagred (`entity/ZagredBossEntity`, `ZagredFx`, `ZagredPhases`, layer `ZagredBossLayer`)
Why: Zagred is a book that censors and rewrites the world, so every move looks like text being edited.
- Words (Halt, Shatter, Seal, ...): a rewriting glyph ring closes in on the floor for the whole wind-up (`ZAGRED_GLYPH_RING`, colour by word); when the word lands a bright sentence runs from his eye to the target and bursts into letters (`ZAGRED_REWRITE_STRIKE`). Halt and Fall add text rising out of the floor (`ZAGRED_TEXT_CORRUPT`).
- Redact: black redaction bars slide over the lane of the sentence while the letters are written, then flash and cut (`ZAGRED_INK_BARS`); every letter is struck down as it detonates.
- Overwrite: text corrupts the whole arena; each rule stone is a void column with a slab carrying the rule's glyph (`ZAGRED_RULE_STONE`, refreshed every 50 ticks while the stones live).
- Void lance: the lane is drawn as ink bars for 12 ticks before the cut (the hit moved from 5 to 8 ticks after the cast so the lane can be read).
- Daemons (`GrimoireDaemonEntity`): lesser ash bolts are short sentences, the greater daemon's circle is a glyph ring then a fire strike, the arch daemon's well is corrupted text over a ring.
- Gatekeeper of the Underworld Gate: a fighting Gatekeeper wears the void glyph ring.
Buffs: health 600 -> 800 (the 6%/s budget and the 45% player cap stay; the budget hooks are untouched), last-act cooldowns x0.6 (was x0.7), Redact sentence 9 letters in act IV (was 8), and in the second bar a second "echo" sentence on another curve, telegraphed with its own bars, follows the first.
Second bar: entering act IV starts `PhaseRefill` ("final"); health eases to full over 40 ticks while he is invulnerable (3 s transition), the budget is bypassed, the bar tints white, a chat line appears. `finalBar` (saved with the refill state) holds act IV for the whole second bar, so a full bar does not return him to act I. Loot and Kotodama reward fire only in `die()`.

## Blood-Red Commander (`dungeon/nexus/zone/MonarchFx`, `MonarchAttacks`, `MonarchArts`, `MonarchDriver`, layer `CommanderBossLayer`)
Why: a war host. Banners rise round every area attack (`CMD_BANNERS`), crimson chains are drawn along every line before it is cut (`CMD_CHAINS`), the vault light falls when it wakes, roars or calls soldiers (`CMD_VAULT_LIGHT`), and every cut is a crimson streak with a blood stain (`CMD_CRIMSON_STRIKE`). The old dust marks stay: they are the exact hit shapes.
Buffs: soak 4.8 -> 5.4 (5400 effective health); in the second bar moves come 20% sooner (`MonarchRules.paced`), the Domain Slam is 2 blocks wider with 8 more ticks of warning, the Shadow Rain has 3 more circles.
Second bar: the roar into phase 3 starts the refill (40 ticks inside the 50-tick roar, cannot be hurt); `nusmp_monarch_final` and the refill state are saved in the entity's persistent data; phase 3 is held by that mark. The fight reset (it falls asleep) clears both, so a new fight may refill again.

## Juggernaut (`dungeon/sentient/JuggernautSpawner`, `JuggernautMoves`, layer `JuggernautBossLayer`)
Why: the immune system's champion: ash and antibodies. Birth throws an ash pulse (`JUG_ASH_PULSE`). New move, the Antibody Pulse: white-red rings close on its feet for 30 ticks (`JUG_ANTIBODY_RING`), then an ash shockwave hits those standing on the ground within 5 blocks (jump it), 20% of health through the balance law. New second bar, the Immune Surge: once at 25% health it refills, takes Resistance V for 3 s, and then pulses every 130 ticks with a bigger, longer mark. The surge is saved on the entity.

## Not covered, and why
The Maw is a world system, not a fight; Spirit Lords are the player's companions; floor mobs (basilisk, direwolf, living armour, slime) are ordinary creatures. God of Order, the Zogratis family, Goliath and Charybdis belong to the other agents.
