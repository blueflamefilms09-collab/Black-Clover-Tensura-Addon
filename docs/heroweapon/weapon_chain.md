# Vassal Chain (`vassal_chain`): the war chain of S'yne's world, Sloth by design

Builder `hvd`. Logic: `heroweapon/weapon/chain/**`, forms: `data/nusmp/hero/forms/vassal_chain.json`. Style SWING (reach), class Vassal Weapon.
Written in two passes: the LOOK section (1) first so the art builder could start, then the rest.

## 1. The look (for the art builder `hart4`)

The wiki summaries give the chain's skills (chains wrapped round targets, four groups of chains that bind, a slam that sends many chains racing along the ground) and say its
wielder fights several heroes alone; no picture could be read, so the shape is DESIGN: a heavy kusarigama-style war chain without the sickle. (State of knowledge: skills from
summaries, shape from design.)

* **What it is.** A leather-wrapped hand grip with a forged clasp, from which a heavy chain hangs and curves, ending in a weighted spiked bar-and-ball. About 2.0 blocks long when
  hanging in the hand (size class `whip` 1.6 to 2.4 or `coilwhip` 1.1 to 1.7: the art builder decides by the real-life size rule). Held at the grip, the chain falls in a slow
  S-curve forward and down, the weight at the end hanging lower than the grip; tip forward on the icon diagonal.
* **Quad budget.** The chain is the cost: build ONE link as a small oval torus (about 24 quads) and repeat it 14 to 16 times with the links alternately turned 90 degrees along a
  gentle curve (a named part `links`, repeated geometry), staying well under 2800 quads in all. The weight and the clasp carry the detail.
* **Named parts** (8 to 12): `grip` (cylinder with a leather wrap as a skin detail), `grip_cap` (pommel), `clasp` (a forged ring-and-hook housing the gem), `links` (the repeated
  links), `swivel` (the joint before the weight), `weight` (a heavy bar with a ball), `spikes` (four short studs on the weight), `strap` (a short leather loop on the grip), `gem`.
* **The gem.** Set in the CLASP, between the grip and the first link, in an iron bezel: a slate-blue cabochon, about 0.12 blocks.
* **Palette** (Sloth: heavy, dull-bright steel): links dull steel `#7C8791` with bright wear on the outer curves `#C5CDD3` and rust-brown patches `#6B4E3B` (a worn, soot-darkened chain);
  weight and spikes dark iron `#3A3F45` with lighter studs `#69727C`; clasp blackened steel `#2E3238`; grip leather `#4A3A2C` with wrap lines `#6A5340`; gem `#7FA3C7`.
* **Glow skin** (additive): the gem (`#BFD8EE` full) and a very faint pulse along the first three links nearest the clasp (`#8FB0CC`), as if the weapon were heavy with sleep.
  Nothing else glows.
* **Mood.** Heavy, patient, tired: nothing shiny, nothing fast-looking. The Sloth curse forms keep this look (vassal weapons have no curse mesh).
* **Icon (16 x 16 sprite).** A short grip at the lower left, a dotted grey chain curving to a dark ball at the upper right, one slate-blue pixel at the clasp.

## 2. Canon: what the series says, and what is design

Source types: **S** = a search summary of the series' fan wiki (the pages themselves could not be opened from the build machine), **M** = memory, **D** = design. Nothing
here was checked against the light novels or the anime.

| Fact | Source |
|---|---|
| The Vassal Chain is a weapon of S'yne's world; the wielder is S'yne's Sister, who is very skilled with it (she defeats several heroes alone) and keeps it of her own free will even while on the side of the Waves. | S: https://shield-hero.fandom.com/wiki/Vassal_Chain, https://shield-hero.fandom.com/wiki/S'yne's_Sister |
| Chain Defence wraps chains round the targets to shield them from attacks. | S |
| Binding: Multi-Headed Orochi (the Japanese name reads "Capture: the Eight-Headed Serpent") summons four groups of chains that restrict the target's movement. | S |
| Hydra: the user slams the chain into the ground and several chains race out at great speed. | S |
| Also listed: an unnamed chain-binding skill, an unnamed teleport, teleport jamming, an unnamed movement skill and Weapon Wearing. The "unlocked chain" column is marked unknown ("?") for every skill. | S |
| No gem is described for the Chain; the Vassal Weapons page says every Vassal Weapon has one, which stays through its transformations. | S: https://shield-hero.fandom.com/wiki/Vassal_Weapons |
| Sloth in canon: the Hunting Tool of Sloth (a bear motif; using it costs levels and weakens power-up methods). The wiki SUSPECTS the Vassal Weapons tied to the Legendary Hunting Tool are the Vassal Book and the Vassal Harpoon. Nothing in the summaries ties Sloth to the Chain. | S: https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List |
| The war-chain look, the heavy links, the lash, the pull, the grapple, every number, the form names "Tempered Chain", "Hydra Chain" and "Orochi Chain" as tiers, and the whole Sloth series. | D |

Canon techniques deliberately NOT built: the unnamed teleport and teleport jamming (the Sewing Set and the Mirror carry travel), Weapon Wearing (no description to build from).

**Sloth is a design assignment, not canon.** The brief and design.md (sections 7 and 18) give the Chain the Sloth Curse Series because canon ties no Holy Weapon to Sloth. The closest canon link is
the Vassal Harpoon or the Vassal Book (suspected), not the Chain; the owner may want to move the series, which would be a matter of swapping the three curse forms and the three Sloth techniques
between two weapons' files.

## 3. How it plays: reach, weight and the bill

The Chain is a SWING weapon with reach: the use key lashes along the look direction, and the keys fire pulls, binds and a fan of chains.

1. **The lash.** `chain.chain_lash` hits everything the chain crosses within 7 blocks (four foes) and tugs the far ones a little toward the wielder.
2. **Heavy Links (the passive of every form).** Each melee blow, and each lash, hangs a link on the creature it strikes. At the third, each within 3 seconds of the last, the weight takes hold: the
   creature is slowed (Slowness I) for 2 seconds and the count starts again. Sixteen creatures at most are tracked.
3. **Pulls.** Chain Pull yanks the first foe in sight to the wielder (a boss barely moves); the Grapple swings the wielder to the first foe in sight. Neither anchors to a block: the design changes no
   block, and the chain hooks creatures only.
4. **Binds.** Chain Binding holds one foe for 3 seconds; the ultimate Multi-Headed Orochi binds four foes in a circle and squeezes them twice.
5. **Sloth** (curse forms only): slowness and delay for the foes and for the wielder, and a bill that comes due. See section 7.

## 4. The forms tree

Items to absorb are listed per form; absorbed items are kept, never consumed (design.md section 6). All forms use the base look; the vassals have no curse mesh, so the Sloth forms keep it.

| form (id) | tier | parents | needs | mastery | stats | use / sneak / key1 / key2 / key3 | passives |
|---|---|---|---|---|---|---|---|
| Chain (`base`) | 0 | - | nothing (always learned) | 0 | attack_damage 1, reach 0.5, armor 1 | `chain.chain_lash` / - / `chain.chain_pull` / `chain.hydra` / - | heavy_links |
| Tempered Chain (`tempered_chain`) | 1 | base | minecraft:iron_ingot x48, minecraft:chain x32, minecraft:coal x24 | 80 | attack_damage 2, reach 0.5, armor 2, knockback_resistance 0.1 | `chain.chain_lash` / `chain.chain_defence` / `chain.chain_pull` / `chain.hydra` / `chain.chain_binding` | heavy_links |
| Hydra Chain (`hydra_chain`) | 2 | tempered_chain | minecraft:chain x64, minecraft:fermented_spider_eye x12, minecraft:prismarine_crystals x16 | 160 | attack_damage 3, reach 1, armor 3, knockback_resistance 0.15 | `chain.chain_lash` / `chain.chain_defence` / `chain.grapple` / `chain.hydra` / `chain.chain_binding` | heavy_links |
| Orochi Chain (`orochi_chain`) | 3 | hydra_chain | minecraft:netherite_scrap x8, minecraft:chain x96, minecraft:blaze_rod x24 | 280 | attack_damage 4, reach 1, armor 4, knockback_resistance 0.2, max_health 4 | `chain.chain_lash` / `chain.grapple` / `chain.chain_pull` / `chain.chain_binding` / `chain.orochi` | heavy_links |
| Idle Chain (curse: sloth, +3 corruption/s) (`idle_chain`) | 1 | base | minecraft:iron_block x8, minecraft:chain x32 | 120 | attack_damage 4, attack_speed -0.3, armor 3, knockback_resistance 0.2 | `chain.chain_lash` / `chain.sloth_weight` / `chain.chain_pull` / `chain.hydra` / `chain.chain_binding` | heavy_links |
| Chain of the Long Nap (curse: sloth, +5 corruption/s) (`drowsy_chain`) | 2 | idle_chain | minecraft:iron_block x16, minecraft:soul_sand x32, minecraft:cobweb x16 | 220 | attack_damage 5, attack_speed -0.5, reach 1, armor 4, knockback_resistance 0.3 | `chain.chain_lash` / `chain.sloth_weight` / `chain.sloth_snap` / `chain.hydra` / `chain.chain_binding` | heavy_links |
| Chain of the Long Afternoon (curse: sloth, +8 corruption/s) (`long_afternoon`) | 3 | drowsy_chain | minecraft:iron_block x32, minecraft:soul_sand x64, minecraft:netherite_scrap x4, tensura:magic_stone x48 | 320 | attack_damage 6, attack_speed -0.7, reach 1.5, armor 5, knockback_resistance 0.4, max_health 4 | `chain.chain_lash` / `chain.sloth_snap` / `chain.sloth_weight` / `chain.chain_binding` / `chain.debt_comes_due` | heavy_links |

Notes on the tree: the ordinary branch is a line of four (Chain, Tempered Chain, Hydra Chain, Orochi Chain); the Sloth branch is a second line of three from the base form. A Sloth form has more
attack damage, armor and knockback resistance than the ordinary form of its tier and a negative attack speed (slower swings), which is part of its price.

## 5. The techniques

### 5.1 The budget (design.md section 5), straight from `ChainNumbers`

Raw damage is before the bond-level factor (1.0 to 2.2) and the balance law. "Most on one target" counts every hit of the technique.

| technique | kind | raw per hit | most on one target | targets | cooldown | cost | heal |
|---|---|---|---|---|---|---|---|
| `chain.chain_lash` | ordinary | 6 | 6 | 4 | 60 ticks (3 s) | 1% | - |
| `chain.chain_pull` | utility | 4 | 4 | 1 | 160 ticks (8 s) | 2% | - |
| `chain.chain_binding` | utility | 3 | 3 | 1 | 240 ticks (12 s) | 2% | - |
| `chain.hydra` | ordinary | 7 | 7 | 8 | 140 ticks (7 s) | 2% | - |
| `chain.chain_defence` | utility | 0 | 0 | 0 | 240 ticks (12 s) | 2% | - |
| `chain.grapple` | ordinary | 6 | 6 | 1 | 180 ticks (9 s) | 2% | - |
| `chain.orochi` | ultimate | 8 | 16 | 4 | 700 ticks (35 s) | 6% | - |
| `chain.sloth_weight` | utility | 0 | 0 | 0 | 260 ticks (13 s) | 3% | - |
| `chain.sloth_snap` | heavy | 12 | 12 | 1 | 200 ticks (10 s) | 2.5% | - |
| `chain.debt_comes_due` | ultimate | 16 | 16 | 8 | 700 ticks (35 s) | 7% | - |

`ChainNumbersTest` hands exactly these rows to `Budget.violations`. Two ultimates are in the table; a form lists at most one (the Orochi Chain the Orochi, the Chain of the Long Afternoon the bill).

### 5.2 Technique by technique

**`chain.chain_lash` Chain Lash** (ordinary, 3 s, 1 percent)
* What: foes within 1 block of the line from the eye to 7 blocks ahead (four at most, wall-clipped) take 6; those beyond 3 blocks are tugged toward the wielder. Refused (and free) when nothing is hit.
* Tell: a chain flies out of the hand (no wind-up; 6 raw needs none).
* Counter: stay out of 7 blocks or break the line.

**`chain.chain_pull` Chain Pull** (utility, 8 s, 2 percent)
* What: the first foe under the crosshair (16 blocks) is dragged to within 2.2 blocks of the wielder over 14 ticks and takes 4 on arrival; a boss is pulled at a quarter of the strength.
  Shared control lock.
* Tell: a chain from the hand to the foe and the sound of chain being paid out; the pull is visible.
* Counter: the pull is a push of velocity, not an effect, so no cure helps; break the line at the start or be a boss.

**`chain.chain_binding` Chain Binding** (utility, 12 s, 2 percent)
* What: the first foe under the crosshair (14 blocks) takes 3 and is held fast (Slowness X, the law's control time, 3 s base). Shared control lock.
* Tell: a chain from the hand and chains wrapped round the foe for the length of the hold.
* Counter: cure slowness; stay beyond 14 blocks.

**`chain.hydra` Hydra** (ordinary, 7 s, 2 percent)
* What: after 10 ticks of gathering, five chains race out along the floor in a fan 70 degrees wide and 10 blocks long, from where the wielder then stands, in the directions they faced when the technique began.
  Every foe in any of them (eight at most) takes 7 once and is popped up a little.
* Tell: five lines on the ground and a ring where the chain slams down, for half a second.
* Counter: step out of the lines (they are narrow: 1 block each) or jump over the gap between two of them.

**`chain.chain_defence` Chain Defence** (utility, 12 s, 2 percent)
* What: the wielder and up to three allies within 4 blocks get Absorption (4 health; 8 for a body of 40 maximum health or more) and Resistance I for 6 seconds.
* Tell: a coil of chain round the wielder for the six seconds.
* Counter: it is support; it wears off.

**`chain.grapple` Grapple** (ordinary, 9 s, 2 percent)
* What: the first foe under the crosshair (20 blocks): the wielder is pulled to within 2.5 blocks of it over 14 ticks and strikes it for 6 on arrival. Not under the control lock (it moves the wielder only).
* Tell: a chain from the hand to the foe.
* Counter: move away during the swing; the wielder arrives next to you.

**`chain.orochi` Multi-Headed Orochi** (ultimate, 35 s, 6 percent)
* What: the ground under the aimed point (18 blocks) is the centre. Refused when no foe stands in the 6-block circle at the cast, or when the control lock is taken (it is taken at once, at the cast). After 24 ticks, up to four foes
  inside the circle at that moment take 8 and are held (Slowness X for the law's control time, 4 s base), and 30 ticks later the ones still alive take 8 again. 30 ticks is more than the 10-tick window in
  which a creature that was just hurt ignores a second blow.
* Tell: a ring on the ground and four chains falling from above for 1.2 seconds before the first blow.
* Counter: leave the circle during the tell. Once bound, the second blow cannot be avoided (the bind is the point of the ultimate).

**`chain.sloth_weight` Sloth: Weight** (Sloth, utility, 13 s, 3 percent, plus 30 corruption)
* What: the foes within 8 blocks (eight at most) are slowed (Slowness III) and weakened (Weakness I) for the law's control time (6 s base); the wielder is slowed (Slowness I) for 6 seconds. Shared control lock.
* Tell: a field of chains round the wielder for the six seconds.
* Counter: leave the 8 blocks; cure the effects.

**`chain.sloth_snap` Sloth: Snap** (Sloth, heavy, 10 s, 2.5 percent, plus 35 corruption)
* What: the first foe under the crosshair (14 blocks) is wrapped now (Slowness IV for the law's control time, 3 s base, behind the control lock; without the lock it is only marked); 3 seconds later, if the
  foe is still within 18 blocks of the wielder and alive, the chain snaps tight for 12.
* Tell: a chain from the hand and a ring round the foe's feet; the snap has its own flash.
* Counter: get beyond 18 blocks in the three seconds (impossible when slowed, so the lock matters), shield the blow, or kill the wielder.

**`chain.debt_comes_due` The Bill Comes Due** (Sloth ultimate, 35 s, 7 percent, plus 120 corruption)
* What: for 6 seconds heavy chains lie round the wielder: the foes within 9 blocks (eight at most) are slowed (Slowness IV) and tired (Mining Fatigue II), and the wielder is slowed (Slowness II). Shared control lock,
  taken at the cast. Then the bill comes due on every foe within 11 blocks: 16 each, and a stagger of up to one second (Slowness X, shortened by the law's diminishing returns) that belongs to the same control use.
* Tell: a ring of radius 11 on the ground and a field of chains round the wielder for the six seconds; the blow has a flash.
* Counter: leave the 11 blocks before the six seconds run out (the foes are slowed, so it takes a dash or a teleport), or kill the wielder who is slowed too.

## 6. The passive

* **`heavy_links`** (every form): see section 3. Counted per creature, bounded to sixteen, reset by `onDismiss`.

## 7. The Sloth Curse Series (the design's assignment, not canon)

Three `curse: true` forms with `sin: sloth` and the engine's `corruptionPerSecond`: **Idle Chain** (tier 1, 3 per second), **Chain of the Long Nap** (tier 2, 5 per second) and **Chain of the Long Afternoon**
(tier 3, 8 per second). They use the base look. The curse engine (the curse builder's files) owns unlocking (despair and acceptance), the permanent price, the corruption meter, the Overrun at 1000 and the sin's own
downside (design.md section 7: "the wielder slows and techniques are delayed"); this weapon adds only what is in its own files:

| | Idle Chain | Chain of the Long Nap | Chain of the Long Afternoon |
|---|---|---|---|
| What it gives | attack damage 4, armor 3, knockback resistance 0.2; Sloth: Weight on sneak | attack damage 5, reach +1, armor 4, knockback resistance 0.3; adds Sloth: Snap | attack damage 6, reach +1.5, armor 5, knockback resistance 0.4, +2 hearts; The Bill Comes Due on key 3 |
| What it costs | attack speed -0.3; 3 corruption a second; Weight 30 corruption and a slow on the wielder | attack speed -0.5; 5 a second; Snap 35 | attack speed -0.7; 8 a second; the bill 120 corruption and a slow on the wielder |

Why Sloth: the weapon is already heavy and patient. Sloth puts things off and pays the bill later, which is exactly the Snap (a delay with a heavy blow at the end) and the bill (six seconds of weight, then
everything at once). The foes are slowed through the shared control lock; the wielder is slowed for as long, and swings slower for as long as a Sloth form is active, so the series is a loan: great stats now,
and the wielder is the slowest creature in the fight. Every effect is bounded: at most level IV on a foe, at most level II on the wielder, six seconds; no boss gets more than the law's stagger.

**Double counting to avoid:** the three Sloth techniques call `HeroCurse.addCorruption` themselves (30, 35 and 120) in addition to the form's per-second rate. If the curse engine also charges corruption per
technique, the lead should keep only one of the two. The Bill (120) is intentionally large: three uses fill an eighth of the meter.

## 8. How it differs from what the mod already has

The mod already has **Chain Magic** (`book/ChainBook`, `book/ChainArts`): a grimoire with twelve pages (Chain Strike, Chain Whip, Scorpio Chain, Dance of the Pitless Viper, Kunai Chains, Heavy Chain, Grapple Chain,
Chain Aegis, Chain Armament, Chain Cage, Magic Binding Iron Chain Formation, Chain Reckoning). The Vassal Chain shares no code and no state with it, only the VFX shapes `CHAIN_FX1` to `CHAIN_FX3`. The concepts
overlap because canon names overlap; what is different:

| Chain Magic (grimoire) | Vassal Chain (this weapon) |
|---|---|
| casts from a book; some pages bleed magicules or build a cage | a SWING weapon with reach 7 and no cage |
| Grapple Chain anchors to a creature or to a block | Grapple hooks creatures only (no block anchoring) |
| Scorpio Chain hauls a foe in and poisons it | Chain Pull yanks a foe in; no poison |
| Chain Aegis, Chain Armament: shields and a damage-resistance buff | Chain Defence: Absorption and Resistance I for you and three others |
| Iron Chain Formation, Chain Cage: bind a crowd in a cage | Orochi: four foes in a circle, bound and squeezed twice |
| no passive | Heavy Links: the third blow slows |
| none | the Sloth series |

## 9. Engravings (the owner's rule 6) and why

```java
Map.entry("vassal_chain", List.of(e("sturdy", 2), e("enervation", 2), e("crushing", 1))), // a heavy war chain: hard to break, tiring to be caught in, hits like a flail
```

* `sturdy` 2: the chain is a heavy forged thing, and the Chain Defence is its defensive technique.
* `enervation` 2: what the chain holds is tired out (Weakness, Mining Fatigue, the Sloth series).
* `crushing` 1: the weighted, spiked end hits like a flail.

## 10. Code map: what each file is for

Config keys: none (a weapon keeps its numbers in code, in a pure table with a test). Commands: none. Quest keys: none. Lang: the keys `hero.skill.chain.<name>.name` and `.desc` for each skill and `hero.passive.chain.<id>` (and `.desc`) for each passive are in the fragment `hvd.lang.json`, which the lead merges into `en_us.json`.

All under `heroweapon/weapon/chain/` (every class has a "Why:" Javadoc).

| File | Why it exists | Tests |
|---|---|---|
| `ChainLogic` | the `HeroLogic`: lists the ten skills from the rows, applies the heavy links on `onHit`, clears them on `onDismiss`, registers the self-test `hero.weapon.chain` | self-test, `ChainFormsTest` (4) |
| `ChainSkills` | the bodies of the techniques and the links | self-test |
| `ChainNumbers` | every figure and the pull weight and absorption rules as pure code | `ChainNumbersTest` (8) |
| `WeightLedger` | the heavy-link counter per creature with its time and size limits, as pure code | `WeightLedgerTest` (5) |
| `vassal_chain.json` | the forms file | `ChainFormsTest` runs the shared checker on the shipped file |

## 11. Server rules, checked

* Damage: all through `HeroCtx.hit`. Heals: none. Mana: the dispatcher pays; nothing extra is paid inside.
* Control: Chain Pull, Chain Binding, the Orochi, Sloth: Weight and The Bill take `HeroCtx.control()` first, once, at the cast (The Bill's closing stagger belongs to that same use: the lock lasts 12 seconds, so asking again at the end could never succeed); Sloth: Snap's slow is applied only if the lock is available (the damage is not).
  The Grapple and the lash move the wielder or tug a little, which is not control.
* The wielder is moved only by `Moves.pullStep` (to a creature, never to a block, never through a wall).
* No entity is spawned. No block is changed. Longest task: The Bill's 6 seconds.
* Targets: from `Foes`; at most 8. The links are bounded (16 creatures) and expire in 3 seconds.
* State: `PlayerState` for the links (cleared on logout and server stop). Effects: `VfxSpawn` (`CHAIN_FX1`, `CHAIN_FX2`, `CHAIN_FX3`), `Tells` and chain sounds. No vanilla particles.

## 12. Unverified, assumed, and to be checked by the lead

* **Nothing has run in the game**: type-checked and unit-tested only; the self-test `hero.weapon.chain` has not run.
* SWING style: `onHit` is assumed to be called for every melee blow of the summoned weapon (design: "on a hit by the wielder with the summoned weapon"). The lash counts as a blow too.
* A Chain Lash does not use vanilla reach; its 7 blocks come from the technique itself. The weapon's own melee reach comes from the core's base stats (+2) and the forms' `reach`.
* `HeroCurse.addCorruption(ServerPlayer, int)` is the stub's signature; the real curse engine must keep it. The values 30, 35 and 120 are guesses against a meter of 1000 and a Sloth rate of 3 to 8 a second.
* The Sloth forms' negative `attack_speed` is applied by the forms system as an attribute modifier; the budget (design.md section 6) allows -3.0 to +1.0 and these are -0.3 to -0.7.
* Item ids: every vanilla id the forms use was checked as a field of `net.minecraft.world.item.Items` in the API dump (the registry name is the field in lower case), and `tensura:magic_stone` is used elsewhere in this repository and is a field of `TensuraMaterialItems` in the Tensura dump. None of this was seen in the running game; CI's `hero.forms` and this weapon's self-test resolve them against the real registry.
* The figures are first guesses and will need playtesting.

## 13. Sources

* https://shield-hero.fandom.com/wiki/Vassal_Chain (summary)
* https://shield-hero.fandom.com/wiki/S'yne's_Sister, https://shield-hero.fandom.com/wiki/Vassal_Weapons (summaries)
* https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List (summary: Sloth and the Hunting Tool)
* In the repository: `book/ChainBook.java`, `book/ChainArts.java` (compared, not reused), `docs/heroweapon/design.md`, `item/WeaponEngravings.java`.
