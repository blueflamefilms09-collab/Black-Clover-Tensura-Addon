# The D&D layer of the Decennial bosses (1.65.0)

Owner's brief: multi-phase Decennial bosses that fuse overpowered D&D statblocks (Legendary Actions, Lair Actions, Legendary Resistances, mythic phase-shifting, Power Word Kill, anti-magic fields) with anime spectacle (Acnologia's magic absorption, True Dragon pressure, the Anti-Magic Devil). **NEW**, nothing replaced.

## Why a layer on the Labyrinth Guardian and not a second boss entity
The Guardian already is the sector boss: scaling by threat, the theme trait, the leash, the damage budget, the altar that forges the Mana Core, the Gatekeeper that the core opens. A second entity would duplicate all of it and the Mana Core / Gatekeeper chain would have two owners. So `DndBoss` (one object per Guardian, no static state) holds the new rules and the entity calls it from five places: `aiStep` (tick), `hurt` (incoming / reacted), `die` (a cancelled death reports nothing), save and load.

## The rules (all numbers in `DndRules`, pure, pinned by `DndRulesTest`)
| rule | what it does | why it is fair |
|---|---|---|
| Legendary Resistance | a harmful effect is refused (`MobEffectEvent.Applicable` -> DO_NOT_APPLY) and a charge spent, 2 to 5 charges by tier, announced | it runs out; the same effect re-applied within a second costs one charge |
| Legendary Actions | 3 to 5 points per 6 s round, spent between the party's turns (after a player's blow, at most one per 0.75 s) on the profile's moves: tail sweep, cantrip, blink, wing buffet (telegraphed), frightful presence, magic drain | a cycle, never the unaffordable; hits are shares of the boss's attack |
| Lair Actions | every 10 s while a player is in the hall: tremor, searing light, anti-magic field, each ringed with a visible telegraph 1.2 to 1.8 s before it lands | never the same twice in a row; only the profile's own list |
| Phases | form 1, form 2 under half health, form 3 after the Mythic Trait; the boss bar name carries the form | |
| Mythic Trait | when it would die it returns to half health once, with +2 resistances, +1 point, 3 s of invulnerability; `/kill` and the void always end it | once, and only in a fight |
| Anti-magic aura / field | players within 6 blocks (tier 4 and up) or inside a field lose 2 % / 5 % of their magicules a second | only magicules; no block is changed |
| Magic absorption | a quarter of a magical blow is eaten into a shield (10 % of max health at most) | never a heal (the boss heal rule is kept) |
| Frightful presence | d20 + armour/4 + Luck against DC 15, a natural 20 saves; failure: Weakness 6 s, Slowness 3 s | a real save, with a bonus for gear |
| Word of Ending (Power Word Kill) | only in the mythic form of tier 3+ profiles: names a player at or under 35 % health, marks them (Glowing, a title) for 3 s, then kills them | fails if they heal above 35 %, break line of sight, or the boss loses 3 % of its health meanwhile; once per 60 s; server config `execution` switches the kill off |

## 1.66.0: more of the kit
Four new legendary actions: **counterspell** (snaps a spell back at whoever just hit it: 10 % of their magicules and 0.3 x the boss's attack), **life drain** (a necrotic bolt, half of it becomes a shield, never health), **haki pulse** (the True Dragon's pressure as a wave: a save against DC 15 or Slowness II and Mining Fatigue) and **gravity crush** (everyone within 12 blocks is dragged to its feet, 0.2 x attack). Two new lair actions: **falling stars** (five telegraphed stars round the party, 0.25 x attack each) and **necrotic mist** (a 5.5 block zone for 7 s that hurts 0.08 x attack a second). The Holy road gets counterspell and falling stars, the Evil road life drain and the mist; the dragons add the haki pulse, the tier 4 and 5 bosses the gravity crush and both lair zones.

## Data: twenty profiles
`data/nusmp/mega/dnd_bosses/<side>_t<tier><a|b>.json` (written by `tools/labyrinth/gen_dnd_bosses.py`): ten Holy, ten Evil; five tiers of 200 floors, two bosses to a tier (floors 1-100 and 101-200 of a tier). Each names the anime and the D&D creature it fuses. A datapack can replace a file or add one; a bad file is logged and skipped (the boss gets the built-in default). Examples: `holy_t5b` Sovereign of the Throne of Primeval Light (Alma Torran + Solar/Tiamat), `evil_t5a` Acnologia-Class Dragon of Frozen Tartarus (Fairy Tail + Tiamat), `evil_t4a` Voidweaver Matron of Anti-Mana (the Anti-Magic Devil + Vecna).

## Config (`nusmp-labyrinth-mega-guard-server.toml`, `[guard]`)
`dnd` (default true: false gives the 1.64 Guardian) and `execution` (default true).

## Tests
`DndRulesTest` (rules and the twenty files), in-game `guard.dnd` (resistance by the real effect path, a legendary action in reaction to a blow, the mythic trait cancelling the first death only, `/kill`, the Word of Ending's mark, absorption).
