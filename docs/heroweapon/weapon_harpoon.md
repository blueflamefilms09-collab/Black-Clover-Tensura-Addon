# Vassal Harpoon (`vassal_harpoon`): the whaling harpoon of Glass' world

Builder `hvd`. Logic: `heroweapon/weapon/harpoon/**`, forms: `data/nusmp/hero/forms/vassal_harpoon.json`. Style THROW, class Vassal Weapon.
Written in two passes: the LOOK section (1) first so the art builder could start, then the rest.

## 1. The look (for the art builder `hart4`)

The wiki summaries only say what the harpoon DOES (lightning-wreathed charges, a bind-and-burn cross, a whale hunter's throw, a "Harpoon of 0"); its shape is a DESIGN here,
the plain reading of the word: a real whaling harpoon with a tether. (State of knowledge: skills from summaries, shape from design.)

* **What it is.** A harpoon about 2.3 blocks long (size class `spear` or `trident`: the art builder decides by the real-life size rule, the harpoon is shorter than a lance).
  A straight shaft, a broad leaf-shaped head with ONE large swept-back barb on each side (a toggle-head silhouette: wide at the barbs, narrow at the point), an iron socket where
  the head meets the shaft, a ring-shaped eyelet just below the socket, and a weighted butt cap. A rope (the tether) is wound in two loose coils on the shaft, its free end
  running from the eyelet and hanging in a loop. Tip up and forward on the icon diagonal.
* **Named parts** (9 to 13): `shaft`, `bands` (three bronze rings on the shaft, repeated geometry), `socket`, `head` (the leaf blade), `barbs` (two swept barbs, mirrored), `eyelet`
  (a torus), `rope_coil_a`, `rope_coil_b`, `rope_tail` (the loop hanging from the eyelet), `butt` (the weighted cap), `gem`, `grip_wrap` (a leather wrap, grip is at the mesh origin).
* **The gem.** In the socket, at the neck of the head, where the barbs meet the shaft: an electric blue-white cabochon, about 0.12 blocks, held by two small iron claws.
* **Palette**: shaft weathered dark wood `#4A3626` with lighter grain `#6A4D37`; bronze bands `#B07A2E` (edge `#E0B060`); socket and eyelet dark iron `#3C4047`; head blue-steel
  `#8FA3B5` with a bright honed edge `#DDE9F2` and a darker centre line `#5E7285`; rope hemp `#C9B27A` with shadow strands `#8E7A4A`; leather wrap `#5A3E2A`; butt cap iron
  `#4A4F57`; gem `#7FE6FF`.
* **Glow skin** (additive): the gem (`#BFF6FF`, full), a thin lightning-vein engraving running down the middle of the head and along both barbs (`#9FEFFF`, one or two
  crooked lines, not a filled area), and a faint ring of the same colour round the eyelet. The wood and the rope do not glow.
* **Mood.** Heavy, seafaring, a thunderstorm kept in a tool. Ornate enough for a vassal weapon (engraved bands, a carved barb root) but still a working harpoon.
* **No curse look** (this weapon has no Curse Series).
* **Icon (16 x 16 sprite).** A diagonal dark shaft with a steel arrowhead at the upper right and a bright blue pixel at its neck; a tan rope loop near the middle.

## 2. Canon: what the series says, and what is design

Source types: **S** = a search summary of the series' fan wiki (the pages themselves could not be opened from the build machine), **M** = memory, **D** = design. Nothing
here was checked against the light novels or the anime.

| Fact | Source |
|---|---|
| The Vassal Harpoon is one of the Vassal Weapons of Glass' world; Sadeena is its current wielder. | S: https://shield-hero.fandom.com/wiki/Vassal_Harpoon, https://shield-hero.fandom.com/wiki/Sadeena |
| The Harpoon Reincarnator held it by illegitimate means, through a control accessory made by the Third Army; Sadeena took it after Rino swapped his accessory for a breakable one. The wiki pages disagree on how he died. | S: https://shield-hero.fandom.com/wiki/Harpoon_Reincarnator |
| Skills in the wiki's table: Lightning Strike Dragon, Whale Hunting Harpoon, Brave Fish Blaster, Lightning Shock Harpoon, Crucifixion, and the 0-series skill Harpoon 0. | S |
| The Reincarnator's known skills: lightning-based charges, a second thrown harpoon, a downward strike, a bind-and-burn cross. Lightning Strike Dragon is a charge wreathed in crackling lightning that can turn 180 degrees. The descriptions of Whale Hunting and Brave Fish Blaster are short. | S |
| Harpoon of 0: no damage, cooldown or SP cost, found effective against illegitimate powers; the weapon form is obtained by absorbing the mysterious poison in Fitoria's sanctuary. | S: https://shield-hero.fandom.com/wiki/Sadeena/Powers_and_Abilities |
| The 0 Series is a mysterious series of weapons unlocked in the Legendary and Vassal weapons, effective against "those who possess eternity", thought to be left by the God Hunters; the heroes learned it could repel the connections that illegitimate power forms, and tried it on the crack in the sky made by a wave. | S: https://shield-hero.fandom.com/wiki/0_Series |
| The tether (a line back to the wielder), the skewer-reel-pin loop, the reel rule (a heavy creature pulls the wielder), the numbers, the form names "Lightning Shock Harpoon", "Brave Fish Blaster" and "Harpoon 0" as tiers. | D (the three form names are the wiki's skill names, reused as form names) |
| The shape: a whaling harpoon with a rope. | D (no picture could be read) |

**Not built from canon:** the Reincarnator's "second thrown harpoon" (it would be a second projectile; the Dragon's return leg and the Shock's arcs cover the idea) and the
downward strike (the Whale Hunting Harpoon slams the target down). The Brave Fish Blaster has no description beyond its name; its water-fish shape is design.

## 3. How it plays: a line, and what you do with it

The Harpoon is a THROW-style weapon, so the use key throws along the look direction (an instant line, 18 blocks).

1. **The throw.** `harpoon.harpoon_throw` skewers the first foe under the crosshair for 7 and **ties the line to it** for 8 seconds. One creature at a time; a new throw ties a new one.
   The design forbids anchoring the line to a block (no block is ever changed), so the line is always tied to a creature.
2. **What the line is for.** The other techniques act on the tied creature without a second aim: Reel In pulls it, Crucifixion pins it, Lightning Shock sends a current through it
   and on to its neighbours, the Whale Hunting Harpoon falls on it, Harpoon 0 strips it. Without a tie the Whale Hunting Harpoon and Harpoon 0 use the foe under the crosshair.
3. **Reeling in a whale.** Reeling does not drag a boss to the boat. A light creature is pulled to the wielder; a heavy one (body volume of 4 cubic blocks or more, or 150 maximum
   health or more) or any boss pulls the wielder to it instead; a creature already within 3 blocks is left alone. Both go through the shared control lock.
4. **The line's life.** Eight seconds from the throw; a melee blow on the tied creature with the Barbed Head passive adds 2 seconds, up to 12 seconds after the throw; it is cut at
   once if the creature dies, vanishes or is more than 40 blocks away, and when the harpoon is put away.
5. **Harpoon 0** is the bounded dispel the brief asked for: no damage, up to three helpful effects off the tied or aimed creature and one off each of up to three foes beside it.

## 4. The forms tree

Items to absorb are listed per form; absorbed items are kept, never consumed (design.md section 6). All forms use the base look; there is no curse series.

| form (id) | tier | parents | needs | mastery | stats | use / sneak / key1 / key2 / key3 | passives |
|---|---|---|---|---|---|---|---|
| Harpoon (`base`) | 0 | - | nothing (always learned) | 0 | attack_damage 1, reach 0.5 | `harpoon.harpoon_throw` / - / `harpoon.reel_in` / `harpoon.lightning_strike_dragon` / - | barbed_head |
| Whale Hunting Harpoon (`whale_hunter`) | 1 | base | minecraft:prismarine_shard x32, minecraft:cod x32, minecraft:iron_ingot x24 | 80 | attack_damage 2, reach 0.5, armor 1 | `harpoon.harpoon_throw` / `harpoon.whale_hunting_harpoon` / `harpoon.reel_in` / `harpoon.lightning_strike_dragon` / - | barbed_head |
| Lightning Shock Harpoon (`lightning_shock_harpoon`) | 2 | whale_hunter | minecraft:copper_ingot x48, minecraft:lightning_rod x8, minecraft:redstone x32 | 160 | attack_damage 3, reach 1, movement_speed 0.02 | `harpoon.harpoon_throw` / `harpoon.crucifixion` / `harpoon.reel_in` / `harpoon.lightning_strike_dragon` / `harpoon.lightning_shock_harpoon` | barbed_head, storm_charged |
| Brave Fish Blaster (`brave_fish_blaster`) | 3 | lightning_shock_harpoon | minecraft:heart_of_the_sea x4, minecraft:nautilus_shell x12, minecraft:prismarine_crystals x32, minecraft:tropical_fish x16 | 280 | attack_damage 4, reach 1, armor 2, movement_speed 0.03, max_health 4 | `harpoon.harpoon_throw` / `harpoon.whale_hunting_harpoon` / `harpoon.reel_in` / `harpoon.lightning_shock_harpoon` / `harpoon.brave_fish_blaster` | barbed_head, storm_charged |
| Harpoon 0 (`harpoon_zero`) | 3 | lightning_shock_harpoon | minecraft:fermented_spider_eye x24, minecraft:poisonous_potato x32, minecraft:pufferfish x12, tensura:magic_stone x24 | 240 | attack_damage 3, reach 1, armor 2, luck 1, knockback_resistance 0.1 | `harpoon.harpoon_throw` / `harpoon.harpoon_zero` / `harpoon.reel_in` / `harpoon.crucifixion` / `harpoon.lightning_shock_harpoon` | barbed_head, storm_charged |

Notes on the tree: the Whale Hunter learns the great throw; the Lightning Shock Harpoon (copper and lightning rods) adds the Shock and the Crucifixion and the Storm Charged
passive; from it the tree splits into the Brave Fish Blaster (heart of the sea, the ultimate) and Harpoon 0 (the poison form, the dispel). The two top forms are alternatives, not
steps: neither requires the other.

## 5. The techniques

### 5.1 The budget (design.md section 5), straight from `HarpoonNumbers`

Raw damage is before the bond-level factor (1.0 to 2.2) and the balance law. "Most on one target" counts every hit of the technique.

| technique | kind | raw per hit | most on one target | targets | cooldown | cost | heal |
|---|---|---|---|---|---|---|---|
| `harpoon.harpoon_throw` | ordinary | 7 | 7 | 2 | 60 ticks (3 s) | 1% | - |
| `harpoon.reel_in` | utility | 4 | 4 | 1 | 120 ticks (6 s) | 1.5% | - |
| `harpoon.lightning_strike_dragon` | ordinary | 7 | 12 | 6 | 160 ticks (8 s) | 2% | - |
| `harpoon.whale_hunting_harpoon` | heavy | 17.5 | 17.5 | 1 | 220 ticks (11 s) | 2.5% | - |
| `harpoon.crucifixion` | ordinary | 4 | 12 | 1 | 260 ticks (13 s) | 3% | - |
| `harpoon.lightning_shock_harpoon` | ordinary | 7 | 7 | 4 | 100 ticks (5 s) | 2% | - |
| `harpoon.harpoon_zero` | utility | 0 | 0 | 0 | 200 ticks (10 s) | 1% | - |
| `harpoon.brave_fish_blaster` | ultimate | 16 | 16 | 8 | 700 ticks (35 s) | 7% | - |

The Whale Hunting Harpoon's raw is 17.5 because it is 14 with a quarter more against a body of 40 health or more; that is inside the heavy limit of 20. `HarpoonNumbersTest`
hands exactly these rows to `Budget.violations`.

### 5.2 Technique by technique

**`harpoon.harpoon_throw` Harpoon Throw** (ordinary, 3 s, 1 percent)
* What: the first foe under the crosshair within 18 blocks takes 7 and is tied. With Storm Charged the lightning also arcs to the nearest other foe within 4.5 blocks of it for 3.
  Refused (and free) when nothing is under the crosshair.
* Tell: a lightning line from the hand to the target and a thread line that stays.
* Counter: sidestep or break line of sight; it is a hitscan throw. An escape beyond 40 blocks cuts the line.

**`harpoon.reel_in` Reel In** (utility, 6 s, 1.5 percent)
* What: needs a tie within 30 blocks. See the rule in section 3. Over 14 ticks the pulled body is dragged at 1.3 blocks a tick until it stands 2.5 blocks from the other, and strikes
  for 4 on arrival. Shared control lock.
* Tell: the thread line is drawn and a reeling sound plays; the pull is visible.
* Counter: the pull is a push of velocity, not an effect, so no cure helps; leave the 30 blocks, or use a heavy body (which pulls the wielder to it instead).

**`harpoon.lightning_strike_dragon` Lightning Strike Dragon** (ordinary, 8 s, 2 percent)
* What: a horizontal dash of up to 8 blocks the way you look, wreathed in lightning: every foe within 1.4 blocks of the path (six at most) takes 7 and is shoved. 12 ticks later the
  wielder turns and dashes back toward the start (at most 8 blocks), hitting again for 5. The 12-tick delay is longer than the 10-tick window in which a creature that was just hurt
  ignores a second blow.
* Tell: the lightning trail (no wind-up: 7 raw needs none).
* Counter: do not stand on a straight 8-block line ahead of the wielder, and step off it between the legs.

**`harpoon.whale_hunting_harpoon` Whale Hunting Harpoon** (heavy, 11 s, 2.5 percent)
* What: after 12 ticks of gathering, one great throw on the tied creature (or the foe under the crosshair, 24 blocks): 14, a quarter more (17.5) against a body of 40 maximum health or
  more. It slams the target down and, behind the shared control lock, staggers it for 1 second (Slowness X).
* Tell: a ring and a column of light at the target's feet, a rising thunder sound, for 0.6 seconds. The blow follows the tied creature wherever it is within about 30 blocks, so it is **not
  dodgeable once begun**: it is answered by blocking it (it is an ordinary attack, a raised shield works), by armour, and before the cast by not being tied (dodge the Harpoon Throw).
* Counter: as above. This is the one technique of the weapon that cannot be sidestepped; a lead who wants a miss condition can compare the target's distance from the tell ring.

**`harpoon.crucifixion` Crucifixion** (ordinary, 13 s, 3 percent)
* What: needs a tie within 28 blocks. Shared control lock; the creature is pinned (Slowness X, the law's control time, 3 s base) under a cross of lightning that burns it three times for 4
  (12 in all), at 0, 15 and 30 ticks. 15 ticks apart is more than the ten-tick window.
* Tell: two lightning bolts cross over the creature as it is pinned, and a cocoon of thread.
* Counter: cure slowness; the pulses are small and the control lock is shared, so it cannot chain with the weapon's other holds for 12 seconds.

**`harpoon.lightning_shock_harpoon` Lightning Shock Harpoon** (ordinary, 5 s, 2 percent)
* What: needs a tie within 28 blocks. The tied creature takes 7; the current jumps from it to up to three foes within 6 blocks, nearest first, for 4 each.
* Tell: an arc of lightning along the line.
* Counter: break the tie (leave 28 blocks) or do not stand close to the tied creature.

**`harpoon.harpoon_zero` Harpoon 0** (utility, 10 s, 1 percent)
* What: no damage. Strips up to three helpful effects from the tied creature (or the foe under the crosshair, 18 blocks), and up to one from each of up to three other foes within 5 blocks of
  it. Refused (and free) when there is nothing to strip.
* Tell: shards of glass fly from the stripped creature to the wielder; a totem chime.
* Counter: none needed; it only takes borrowed power. A creature with no helpful effects is untouched.

**`harpoon.brave_fish_blaster` Brave Fish Blaster** (ultimate, 35 s, 7 percent)
* What: after 20 ticks of gathering, a surge of lightning-charged water 20 blocks long and 2.2 wide along the look at that moment: every foe in it (eight at most) takes 16 and, behind the
  shared control lock, is staggered for 1 second.
* Tell: a line on the ground along the planned path and gathering light at the wielder's hands for a second.
* Counter: leave the line (it is narrow).

## 6. The passives

* **`barbed_head`** (every form): a melee blow with the harpoon on the tied creature adds 2 seconds to the line, never past 12 seconds after the throw.
* **`storm_charged`** (Lightning Shock Harpoon, Brave Fish Blaster, Harpoon 0): the Harpoon Throw's lightning arcs to one more foe beside the target for 3.

## 7. Curse series

None. The brief gives curse series only to the Mirror (Envy) and the Chain (Sloth). (A note for the owner: the summaries say the wiki SUSPECTS the Vassal Harpoon and the Vassal Book
are the vassal weapons tied to the Legendary Hunting Tool, whose curse is Sloth. That is canon's closest link to Sloth; the design gives Sloth to the Chain as the brief says.)

## 8. How it differs from what the mod already has

Nothing in the mod ties a line to a creature that other techniques then use. The nearest existing thing is the **Scorpio Chain** of Chain Magic (`book/ChainArts.scorpio`): a barbed chain
pierces the target, hauls it in and poisons it. Reel In is a pull like it; what is different is that here the pull is one of several follow-ups of a persistent tie, a heavy creature
pulls the wielder instead, and the Harpoon 0 dispel has no equivalent. The Lightning Strike Dragon, the Shock and the Fish Blaster are lightning shapes like the lightning grimoires'
but are fixed techniques with fixed numbers, not attribute pages.

## 9. Engravings (the owner's rule 6) and why

```java
Map.entry("vassal_harpoon", List.of(e("barrier_piercing", 2), e("elemental_boost", 2), e("severance", 1))), // a lightning harpoon that skewers through wards and fells whales
```

* `barrier_piercing` 2: a harpoon goes through what stands in front of it, and the 0 series is canon's answer to illegitimate protections.
* `elemental_boost` 2: the lightning (Lightning Strike Dragon, Lightning Shock, Crucifixion, the Fish Blaster's charge).
* `severance` 1: the whale hunter's throw is a clean, deep cut.

## 10. Code map: what each file is for

Config keys: none (a weapon keeps its numbers in code, in a pure table with a test). Commands: none. Quest keys: none. Lang: the keys `hero.skill.harpoon.<name>.name` and `.desc` for each skill and `hero.passive.harpoon.<id>` (and `.desc`) for each passive are in the fragment `hvd.lang.json`, which the lead merges into `en_us.json`.

All under `heroweapon/weapon/harpoon/` (every class has a "Why:" Javadoc).

| File | Why it exists | Tests |
|---|---|---|
| `HarpoonLogic` | the `HeroLogic`: lists the eight skills from the rows; `onTick` redraws the line and lets go of a gone creature, `onHit` extends it, `onDismiss` cuts it; registers the self-test `hero.weapon.harpoon` | self-test, `HarpoonFormsTest` (3) |
| `HarpoonSkills` | the bodies of the techniques and the line's upkeep | self-test |
| `HarpoonNumbers` | every figure and the Whale Hunting damage rule as pure code | `HarpoonNumbersTest` (8) |
| `TetherBook` | the line: who is tied, when it runs out, the extension and its ceiling, the draw beat | `TetherBookTest` (7) |
| `ReelRule` | the decision between pulling the creature, pulling the wielder and refusing | `ReelRuleTest` (6) |
| `vassal_harpoon.json` | the forms file | `HarpoonFormsTest` runs the shared checker on the shipped file |

## 11. Server rules, checked

* Damage: all through `HeroCtx.hit`. Heals: none. Mana: the dispatcher pays; nothing extra is paid inside.
* Control: Reel In, Crucifixion, the Whale's stagger and the Fish Blaster's stagger each take `HeroCtx.control()` first; the Whale and the Fish still hurt without it.
* The wielder is moved only by `Moves.pullStep` / `Moves.dash` (to a creature or along their own look, never to a block anchor, never through a wall).
* No entity is spawned. No block is changed. Longest task: the Crucifixion's 30 ticks.
* Targets: from `Foes`; at most 8. The line is cut when the weapon is put away, the person leaves or the bond ends.
* State: `PlayerState` for the line (cleared on logout and server stop). Effects: `VfxSpawn` (`LIGHTNING_SPEAR`, `THREAD_STRINGS`, `THREAD_COCOON`, `FX_LIGHTNING_ARC`, `LIGHTNING_FIEND`,
  `LIGHTNING_GOD`, `WATER_DRAGON`, `KOTO_SHATTER`), `Tells` and sounds. No vanilla particles.

## 12. Unverified, assumed, and to be checked by the lead

* **Nothing has run in the game**: type-checked and unit-tested only; the self-test `hero.weapon.harpoon` has not run.
* THROW-style `use` is assumed to call the dispatcher, which calls `harpoon.harpoon_throw` (the first slot); the throw is hitscan (no projectile entity), as the brief allows.
* `Moves.pullStep` sets the velocity of a player and marks it as hurt so the client learns; whether the client smooths the pull is not known. The pull moves the wielder by velocity, so a
  wall stops it where vanilla physics does.
* The Whale Hunting Harpoon cannot be sidestepped (section 5.2); the lead may add a distance check against the tell ring if undodgeable heavy blows are unwanted.
* `Effects.strip` removes helpful effects by `MobEffect` category; effects from other mods that are mis-categorised are treated as they declare themselves.
* The `VfxShape` conventions for the lightning and water shapes were read from their Javadoc, not seen.
* Item ids: every vanilla id the forms use was checked as a field of `net.minecraft.world.item.Items` in the API dump (the registry name is the field in lower case), and `tensura:magic_stone` is used elsewhere in this repository and is a field of `TensuraMaterialItems` in the Tensura dump. None of this was seen in the running game; CI's `hero.forms` and this weapon's self-test resolve them against the real registry.
* The figures are first guesses and will need playtesting.

## 13. Sources

* https://shield-hero.fandom.com/wiki/Vassal_Harpoon (summary)
* https://shield-hero.fandom.com/wiki/Sadeena/Powers_and_Abilities, https://shield-hero.fandom.com/wiki/Harpoon_Reincarnator, https://shield-hero.fandom.com/wiki/0_Series (summaries)
* https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List (summary: Sloth and the Hunting Tool)
* In the repository: `book/ChainArts.java` (compared, not reused), `docs/heroweapon/design.md`, `item/WeaponEngravings.java`.
