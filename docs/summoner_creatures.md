# Summoner creatures (4.106.0)

Source: the owner's Hell Mode pack (`HELL_MODE_ALLEN_MASTER.md`, 61 reference images, `ALLEN_LORE.md`), which holds the English-wiki research (fandom first, as rule 10 asks; no Japanese fallback was needed for these twelve). Reference images are for the modeller only and are not shipped.

## What it is
Every Summoner card now calls a real Hell Mode creature instead of a vanilla wolf, iron golem or vex, and every cast mode shows its creature for a few seconds. One entity type (`anime_multiverse:summon_beast`, class `SummonBeast`, GeckoLib) carries a synced species number; twelve species, twelve script-written models.

| Species | Called by | Look |
|---|---|---|
| Hayate | Beast Card, Legion Card (rank 0-1) | white maned wolf, forehead gem |
| Cerorin | Beast / Legion from Synthesis rank 2 | black three-headed hound, red eyes |
| Rocanel | Stone Card (rank 0-1) | horned knight with ring shield |
| Mira | Stone from Synthesis rank 2 | hulking stone brute, mirror stone chest |
| Ellie | Wraith Card (rank 0), Spirit cast | blonde porcelain doll in a dark-blue dress, hovers |
| Okiyosan | Wraith from Synthesis rank 1 | white-kimono doll, candle headband, curse nail, hovers |
| Hatch | Insect cast (performer) | queen hornet |
| Genbu | Fish cast (performer) | archelon with a glowing scute shell |
| Dradra | Dragon cast (performer) | winged spiny dragon, glowing mouth |
| Merus | Angel cast (performer) | the one angel: halo, layered wings, white wrap with gold bands |
| Tsubame | Bird Card pulse turned on (performer) | swallow |
| Solarin | Grass Card pulse turned on (performer) | bean pod with a sprout |

* **Creatures** (Hayate, Cerorin, Rocanel, Mira, Ellie, Okiyosan) are ordinary summons: same ledger, cap, expiry, steering, Strengthening, Deputize and Synthesis as before. Their health and damage are the card's numbers times the species factor times the tier power: 1 (Unique), 1.25 (mastered Unique), 2 (Ultimate), 2.5 (mastered Ultimate). The Ultimate's Stone Card keeps its 1.35 scale. Their blow carries an effect (Cerorin wither, Rocanel / Mira knock-back, Ellie / Okiyosan weakness).
* **Performers** (Hatch, Genbu, Ellie, Dradra, Merus, Tsubame, Solarin) are the same entity: harmless, invulnerable, no goals, marked as the owner's side (so no area skill takes one for a foe) but not tagged as a summon (so they never count toward the cap), playing their `ability` clip, gone after 3 s (4 s for the Ultimate) or when the owner dies, logs out or is more than 48 blocks away. One per owner; a new cast replaces the old one. The Ultimate's performers are 1.35 times bigger.

## Setting
Server `summonerRework` section, `summonerCreatures` (default on; needs `summonerRework`). Off = the vanilla wolf, golem and vex of 4.105.0 and no performers. It applies to creatures called after the change.

## Numbers are guesses
Health, damage, speed and box sizes are first estimates (nothing was played). The wiki gives ranks and abilities, not stats.

## Models
Script-written, **not made in Blockbench or Blender** (neither app runs in this session): `tools/blockbench/summoner/` (`summon_lib.py` primitives and painter, `species_a/b/c.py` the twelve models, `gen_summons.py` the driver, `preview.py` the mock-up renderer). It writes the geo, animation, sheet and glow mask under `assets/anime_multiverse/` and a `.bbmodel` per species next to the scripts that Blockbench can open and refine. Previews in `docs/previews/summoner_creatures/` are software mock-ups, not the game.

## Worst-case cost (rule 11)
* Server: at most the owner's cap of creatures (config 3 / 6, Ultimate 12; hard cap 64) of vanilla melee AI; the existing once-a-second upkeep; one performer per owner for at most 80 ticks; no scan of its own; no new packet (only the vanilla entity data of two small values). Entity updates every 3 ticks within 10 chunks.
* Client: each creature is 90 to 410 quads plus one glow pass, nothing beyond 64 blocks; resource locations are built once per species.
* Cleanup: performers discard themselves (life, owner gone, owner far); creatures expire and leave with their owner as before; nothing is saved for a performer.

## Not included
The Allen boss and his seven forms, the other 31 summons, Griff, Summon Cards / Grimoire drops and the Hell Mode race line (designs stay in the owner's master file for later releases).
