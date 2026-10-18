# Fire Dragon Slayer (1.62.0): the first Fairy Tail route of the Skill Tree, the Fang of Smoldering Insanity

The route `nusmp:fire_dragon_slayer` is the content that proves the Skill Book & Skill Tree engine (`docs/skilltree/design.md`): one JSON file, seven techniques that are real ManasCore skills,
two passives, and the owner's required shape *Fire Dragon's Iron Fist, then Fire Dragon's Roar, then a Secret Art (Crimson Lotus: Exploding Flame Blade)*. Reading the **Fang of Smoldering Insanity**
(`nusmp:skill_book` with the book id `nusmp:fire_dragon_slayer_book`, made by the items builder) unlocks the whole route for the player, who then spends Skill Points on its nodes; every bought node
teaches its technique, which is cast with Tensura's own skill keys. Everything here runs on the server; the client only sees effect packets and the tree screen.

**New or replacement (CLAUDE.md rule 5).** All of it is NEW: nothing in the mod was a Fire Dragon Slayer, an SP-priced route or a Fairy Tail technique. Nothing is replaced or removed. KEPT and reused
unchanged: `BalanceLaw` (every hit and heal), `VfxSpawn` and the existing effect shapes (`FLAME_TRAIL`, `FIRE_BURST`, `FIRE_PILLAR`, `FIRE_SPIRAL`, `FIRE_WILD`, `FLAME_EXPLOSION`, `WIND_SLASH`,
`MAGIC_CIRCLE`), `Tells` (the zogratis warning dust), Tensura's energy helper, the self-test registry `labyrinth/mega/SelfTests`, and the icon framework `tools/gen_skill_icons.py`. Nothing of the Black Clover
grimoire is imported (the engine's `IsolationTest` passes on these files).

## 1. Canon, and what is from where

Research was done with web search limited to the Fairy Tail wiki (`fairytail.fandom.com`; the pages cannot be fetched from the build machine, so every canon fact below comes from the search
**summaries** of those pages, marked *[summary]*; what I know from the series without a source on hand is marked *[memory]*; what I decided is marked *[design]*).

| Canon fact | Source | Used for |
|---|---|---|
| Fire Dragon Slayer Magic: Natsu's basic offensive spells are the Roar (a breath of fire from the mouth), the Claw (flames on the feet, a kick; the flames can also work as a jet), the Iron Fist (fists engulfed in flame), the Wing Attack (a rush at the foes, a stream of fire from each arm, the flames shaped like a pair of dragon wings; burns the foes and sends them flying), the Sword Horn (the whole body ablaze, a headbutt at high speed, a pillar of fire) and the Brilliant Flame (both hands lit and brought together for an explosion; in the anime a large fireball is thrown instead) *[summary]* | https://fairytail.fandom.com/wiki/Fire_Dragon_Slayer_Magic , https://fairytail.fandom.com/wiki/Natsu_Dragneel , https://fairytail.fandom.com/wiki/Fire_Dragon%27s_Wing_Attack , https://fairytail.fandom.com/wiki/Fire_Dragon's_Sword_Horn , https://fairytail.fandom.com/wiki/Fire_Dragon's_Claw , https://fairytail.fandom.com/wiki/Fire_Dragon's_Brilliant_Flame | the six active techniques that are not the Secret Art |
| Crimson Lotus: Exploding Flame Blade (Guren Bakuenjin): the arms swipe in a circle, a destructive torrent of flame barrages the target, each connecting strike makes a burning explosion, and a larger explosion ends the barrage (used against Gildarts; in the anime the explosions cover the whole body, in the manga they are around the head) *[summary]* | https://fairytail.fandom.com/wiki/Crimson_Lotus:_Exploding_Flame_Blade , https://fairytail.fandom.com/wiki/Natsu_Dragneel/Synopsis | the Secret Art: a lane barrage with a burst at every pulse and a final blast |
| A Dragon Slayer can eat the element he wields to restore himself and his power, but not magic he made himself; this makes Natsu immune to most flames, including explosions *[summary]* | https://fairytail.fandom.com/wiki/Dragon_Slayer , https://fairytail.fandom.com/wiki/Natsu_Dragneel | Eating Flame (heals and recharges; your own flames do not count) |
| Dragon Slayers have keener smell, sight and hearing, and (the advanced ones) suffer motion sickness in vehicles *[summary]*; Dragon Force is the strongest state and usually needs an outside source *[summary]* | https://fairytail.fandom.com/wiki/Dragonization , https://fairytail.fandom.com/wiki/Dragon_Slayer | NOT built (see section 8): documented flavour only |
| Natsu's body burns when he fights; the flames wreathe his arms and fists in every spell *[memory]* | the series | Blazing Scales (the burning body) *[design]* |

The names *Eating Flame* and *Blazing Scales* are this builder's names for canon behaviours; the node order, the prices, the numbers, the tell and the way each technique is shaped for a block game are **design**.

## 2. The tree

Nine nodes, three branches from the root, one Secret Art that needs two of them. `x` is the column and `y` the row of the canvas (0..12 by 0..9). The map and the table below are written by
the script that also writes the route file and the Java number tables from one source, so they cannot disagree with the file in section 9.

```
      x=0                 x=2                 x=4                 x=6
y=0                                           [Brilliant Flame]
y=1                       [Roar]
y=2                                                               {Crimson Lotus}
y=3                                           [Wing Attack]
y=4   [Iron Fist]         [Claw]
y=5                                           [Sword Horn]
y=6
y=7                       (Eating Flame)
y=8                                           (Blazing Scales)

[ ] a technique   ( ) a passive   { } the Secret Art.   Lines, parent (level it must have) --> child:
  Iron Fist (3) --> Roar
  Iron Fist (2) --> Claw
  Iron Fist (1) --> Eating Flame
  Roar (2) --> Brilliant Flame
  Claw (2) --> Wing Attack
  Claw (2) --> Sword Horn
  Eating Flame (2) --> Blazing Scales
  Roar (3) --> Crimson Lotus
  Wing Attack (3) --> Crimson Lotus
```

| node | name | parents (node level) | SP per level | max | skill | kind |
|---|---|---|---|---|---|---|
| `iron_fist` | Fire Dragon's Iron Fist | root | 1 each | 5 | `nusmp:ft_fire_dragons_iron_fist` | active |
| `roar` | Fire Dragon's Roar | iron_fist 3 | 2/3/3 | 3 | `nusmp:ft_fire_dragons_roar` | active |
| `claw` | Fire Dragon's Claw | iron_fist 2 | 2/2/3 | 3 | `nusmp:ft_fire_dragons_claw` | active |
| `flame_eater` | Eating Flame | iron_fist 1 | 1/2/2 | 3 | - | passive |
| `brilliant_flame` | Fire Dragon's Brilliant Flame | roar 2 | 3 each | 3 | `nusmp:ft_fire_dragons_brilliant_flame` | active |
| `wing_attack` | Fire Dragon's Wing Attack | claw 2 | 2/2/3/3 | 4 | `nusmp:ft_fire_dragons_wing_attack` | active |
| `sword_horn` | Fire Dragon's Sword Horn | claw 2 | 2/3/3 | 3 | `nusmp:ft_fire_dragons_sword_horn` | active |
| `blazing_scales` | Blazing Scales | flame_eater 2 | 2/2/3 | 3 | - | passive |
| `crimson_lotus` | Crimson Lotus: Exploding Flame Blade | roar 3, wing_attack 3 | 4/6/8 | 3 | `nusmp:ft_crimson_lotus_exploding_flame_blade` | Secret Art |

Total to buy every level of every node: 77 SP.

The Secret Art needs the Roar branch and the Wing Attack branch (two different children of the root), at level 3 each. Before its first level can be bought, **22 SP** must have been
spent on what it needs (Iron Fist 3, 3 SP; Roar 3, 8 SP; Claw 2, 4 SP; Wing Attack 3, 7 SP), so its first level is the 26th SP of the route at the earliest; buying every level of every node costs 77 SP.

### 2.1 Why this shape

* **One root, the Iron Fist, and every branch hangs on a different LEVEL of it** (Eating Flame at fist 1, Claw at fist 2, Roar at fist 3). The owner's required shape starts at the fist; making the levels of the fist the
  key to the branches means that levelling the root is not busywork, it opens the tree in order, cheapest branch first, and a player who stops at fist 1 or 2 has still made a real choice.
* **Three branches, three ways to play**: the Roar branch is range (Roar, Brilliant Flame), the Claw branch is melee (Claw, Wing Attack, Sword Horn), the Eating Flame branch is staying power (Eating Flame, Blazing
  Scales). The design asks for two or three branches; three gives each way of fighting its own corner of the canvas.
* **The Secret Art needs the Roar at 3 AND the Wing Attack at 3**: the end of the ranged branch and the middle of the melee branch. It cannot be reached by playing one style only (the owner's "two branches"), and it
  contains the owner's shape exactly: Iron Fist 3, then Roar 3, then the Secret Art.
* **The passives are off the road to the Secret Art**, so a player who wants only techniques can skip them and one who wants staying power can stop there. Eating Flame is cheap (1 SP) and hangs on fist 1 so that a
  beginner gets a real defensive option at once; Blazing Scales is its upgrade (Eating Flame 2).
* **Layout**: depth goes to the right (columns 0, 2, 4, 6); the branches are stacked top to bottom (Roar branch, Claw branch, the passives) and the Secret Art stands alone in the last column, between the two
  branches it needs, so the screen reads as a tree that ends in one fruit.

## 3. The techniques

All numbers are the tables of section 6. "Tell" is the owner's rule that any technique above 8 raw damage shows what it is about to do (a line, a cone or a ring of coloured dust drawn by `Tells`, server side).
"Counter" is how a foe (a monster, or a player in PvP) avoids it. A cast that has to be refused (no room, riding a mount, not enough magicules) is refused BEFORE it is paid for.

**An honest note on the tell.** A technique that reaches farther than melee (more than about 7 blocks) or carries the caster forward has a real WIND-UP, and the wind-up is its tell: the dust is drawn at the cast
and the hit comes a few ticks later along the same line, ring or lane, so a foe has time to leave it (here: the Roar, the Sword Horn, the Brilliant Flame, the Crimson Lotus). The short-reach blows (the Iron Fist,
the Claw, the Wing Attack; 7 blocks at most) land at once: their dust is drawn at the moment of the hit, it shows where the technique went but is not a warning in time, and their counter is distance. If the owner wants
a wind-up on those too, the tables already take a `windup` key and `DragonTimers` already queues it.

| Technique | What it does | Tell | Counter |
|---|---|---|---|
| **Fire Dragon's Iron Fist** (root, 5 levels) | A flaming punch at the foes in a cone in front of you (35 to 45 degrees half angle, 3.5 to 4.5 blocks): the nearest 1 to 3 are hit for raw 6, 7, 8, 9, 11 by level, set alight 3 to 5 s and pushed a little. It costs 1 to 1.5 percent of the magicules, the cooldown falls from 5 s to 3 s with the level, and a punch that finds nobody still shows its flames. | at levels 4 and 5 (raw 9 and 11): a line of dust from the fist to the first foe | step out of the short cone; water or fire resistance |
| **Fire Dragon's Roar** (3 levels) | You draw a breath (6 to 8 ticks: motes gather at your mouth and a line shows the lane), then breathe a lane of fire down the way you LOOK WHEN THE BREATH LEAVES (so you steer it during the inhale), 9 to 13 blocks long: everything within 1.4 to 2.2 blocks of the lane, nearest first, up to 4, 6 or 8 foes, takes raw 9, 11, 13 and burns 4 to 6 s. 3 to 3.5 percent of the magicules, cooldown 8, 7, 6 s. | the gathering motes and the lane line during the inhale | leave the lane in the 6 to 8 ticks (0.3 to 0.4 s) of the inhale |
| **Fire Dragon's Claw** (3 levels) | A flaming kick in a wide cone (55 to 65 degrees, 3.2 to 4 blocks): up to 3 to 5 foes take raw 7, 8, 10, burn 3 to 5 s and are LAUNCHED upward (0.5 to 0.7); a small jet-propelled hop carries you forward. 1.5 to 2 percent, cooldown 6, 5, 4 s. | at level 3 (raw 10): the two edges of the cone | stay out of the short cone |
| **Fire Dragon's Wing Attack** (4 levels) | Both arms sweep out like the wings of a dragon: a cone of 50 to 65 degrees and 5 to 7 blocks; up to 4, 6, 8 or 10 foes take raw 8, 9, 10, 12, burn 3 to 5 s and are thrown away from you (0.9 to 1.5). Two flame streams from the shoulders and two crescents are the picture. 2 to 3.5 percent, cooldown 7 s down to 5 s. | at levels 2 to 4 (raw 9 to 12): the cone edges | distance (more than 7 blocks) or the side of the cone |
| **Fire Dragon's Sword Horn** (3 levels) | You gather flame round you (6, 5, 5 ticks, 0.25 to 0.3 s: the line of the charge is drawn and motes gather), then charge 5, 6 or 7 blocks forward along that same line, wreathed in flame, and stop at the first wall (no room is a refusal before the wind-up; so is riding a mount). The foes within 1.1 to 1.5 blocks of the path, up to 3, 4 or 5, take raw 9, 11, 13, burn 4 to 6 s and are thrown ahead; a pillar of fire marks where you stop. 2.5 to 3.5 percent, cooldown 7.5, 6.5, 5.5 s. | from the cast: the line of the charge and the gathering flames (raw is always above 8) | step off the line in the wind-up; the line does not follow you |
| **Fire Dragon's Brilliant Flame** (3 levels) | You throw a great fireball at what you aim at: the first foe on your look ray, else the first wall, else the ground below the end of the range (20, 24, 28 blocks). It flies 6 to 16 ticks (the distance over 2), then bursts: foes within 3, 3.5, 4 blocks of the point, up to 6, 8, 10, take raw 8, 10, 12, burn 4 to 6 s and are thrown outward. 3 to 4 percent, cooldown 10, 9, 8 s. | a RING of dust on the ground at the landing point and radius, from the throw until the burst | walk out of the ring in the flight time (0.3 to 0.8 s) |
| **Crimson Lotus: Exploding Flame Blade** (Secret Art, 3 levels, 4/6/8 SP) | You swirl both arms (8 ticks), a spiral of flame is drawn along a lane fixed at the cast (8 to 10 blocks, 2.2 to 3 wide), and 3, 4 or 5 PULSES, 11 ticks apart, hit everyone in the lane (up to 6, 9 or 12 foes, raw 9, 11, 13, burn 6 to 8 s), each with a burst on the foe; 6 ticks after the last pulse a FINAL BLAST at the end of the lane (radius 4.5, 5, 5.5) hits everyone in it for raw 18, 24, 30 and throws them back (1.0 to 1.6). 6, 7, 8 percent of the magicules; cooldown 60 s, 54 s, 45 s. | from the cast: the lane line and the ring of the final blast | leave the lane and the ring: the lane does not follow you |
| **Eating Flame** (passive, 3 levels) | Fire cannot hurt you, it feeds you. Fire damage (the game's fire, lit campfires, Tensura's fire magic and flame breath) is cancelled when it comes from somebody else, lava from level 3; the first such hit each second heals 25, 40, 60 percent of the cancelled damage and refills 2, 3, 4 percent of your magicules; burning and being in fire are put out. Each second that you burn or stand within 1.5, 2, 2.5 blocks of a flame (fire, a lit campfire, a torch, lava at level 3) you heal 2, 3, 4 percent of your health and refill 0.8, 1.4, 2 percent of your magicules. Your own flames do not feed you (canon). | none (a passive) | fight without fire |
| **Blazing Scales** (passive, 3 levels) | Whoever hits you in melee is set alight for 2, 3, 4 s and takes a recoil of raw 2, 3, 4 through the balance law, at most once per attacker every 40, 30, 20 ticks (projectiles and spells do not trigger it, and a recoil never triggers another recoil). Each second, hostile creatures within 1.5, 2, 2.5 blocks of you catch fire for 2, 2, 3 s (animals, villagers and pets never). | none (a passive) | shoot, do not touch |

Numbers that are not in a table: a hit is never reset by the hurt-frames trick (see section 8), so two hits on the same foe are always at least 11 ticks apart; every technique stops if the caster dies
or leaves between two parts; effects are drawn for at most 6 of the foes that were hit. Walls: a blow that comes out of the body (the punch, the claw, the wing sweep) hits only foes the caster can SEE (the game's
eye-to-eye line-of-sight test); a lane (the Roar, the Sword Horn, the Crimson Lotus) ends at the first wall; a burst at a distant point (the fireball, the final blast) is not blocked by a wall between the point and a foe.

## 4. The ability hook, in plain words

1. A player presses a Tensura skill key. ManasCore finds the skill in the player's list and calls `onRelease` on it. Every Fairy Tail skill extends `TreeSkill` (the engine's class).
2. `TreeSkill.onRelease` asks ONE question first, `SkillTreeApi.canCast(player, route, node)`: is the engine switched on, has the player unlocked this route (consumed its book), has the player bought this node
   (level 1 or more) and, if the server asks for it, is this route their active class. If the answer is no the technique shows "This technique is locked" and NOTHING happens: no cost, no cooldown, no effect.
   So a skill that was learned by a command, or kept after a refund, still cannot be cast. The same question is asked by the skill menu (`canInteractSkill`), which hides what cannot be used.
3. If the answer is yes, `TreeSkill` checks the cooldown, reads the node level the player bought (1 to the maximum) and calls `cast`. In this route `cast` is `DragonSkill.cast`, which only wraps the technique
   in a safety net (an exception is caught, logged, answered with "The technique fizzles" and counted, never allowed out into the key handler) and calls the technique's `perform`.
4. `perform` is the technique: it reads its numbers for that level from the loaded route (`RouteRegistry.value`; a datapack that retunes the file changes the technique after `/reload`), refuses before paying when it
   must, pays a share of the maximum magicules (`AbilityKit.pay`), hits through the balance law (`AbilityKit.hit` and `BalanceLaw`), applies burn and knock-back, sends the effect (`AbilityKit.fx`) and the sound, and
   returns true. Delayed parts (the inhale, the wind-up of the charge, the flight of the fireball, the pulses) are queued on `DragonTimers`.
5. `TreeSkill` then starts the cooldown from the node's `cooldown` table and gives one mastery point.

The passives have no key: they are NeoForge game-bus listeners (the damage-taken event and a once-a-second player tick), each starting with the same `canCast` question.

## 5. The route file

`src/main/resources/data/nusmp/skill_trees/fire_dragon_slayer.json` (the exact file is in section 9). The WHY of each structure in it:

| Structure | Why it is built that way |
|---|---|
| One file per route, id `nusmp:fire_dragon_slayer` from the file name | A new magic is a file, not a Java class or an item: the engine's loader reads `data/<namespace>/skill_trees/*.json`, so a datapack can add, retune or replace a route and `/reload` applies it. |
| `magic` `fairy_tail`, `name`, `desc` | The family the screen groups routes under, the route's name and a sentence for its tooltip. The sentence sells the route's idea (eat the flame, breathe it back, the Secret Art that ends a fight). |
| `book`, `book_name`, `book_lore` | One registered Skill Book item says which book it is by a component; the route names the book, so the item, its name and its lore need no code. The name is the owner's *Fang of Smoldering Insanity*; the lore line is mine. |
| `color` `#E8541C`, `icon` `minecraft:blaze_powder` | The screen draws an unlocked route in its colour and the book is tinted by it (one sprite serves every route). An ember orange for fire, the colour of the owner's example. |
| `class_id` `fire_dragon_slayer` | The route is also the player's *class* while it is the active one; only the active class casts (config). Unique among routes. |
| `loot_weight` 3, `duplicate_sp` 3 | The Mega-Dungeon drop picks a route by weight: the Fire book is the common first (3), Lightning is rarer (2). A second copy of the book is worth 3 SP so a duplicate is never wasted. |
| `nodes` in order, parents first | The loader refuses a parent that comes later in the file: this also makes a cycle impossible, with no graph search. |
| `parents`: objects `{node, level}` | Every parent is written with the level it must have, so the tree has real depth: the Roar needs the Iron Fist at level 3 (the owner's required Iron Fist, then Roar). The Secret Art lists two parents, which is the "two branches". |
| `cost` and `costs` | `costs` lists the price of each level so later levels cost more; `cost` is the first price (what the screen shows as "from"). Ordinary levels cost 1 to 3 SP, the Secret Art 4, 6 and 8. |
| `skill` | The ManasCore skill the node grants at level 1: the id of a class registered in `FireDragonSkills`. Passives have an empty `skill` and name their passive in `passives`. |
| `values` | Every number a technique uses, by level, so balance is data. The keys and their units are in section 6. A table has exactly one entry per level, so every level has its own number. |
| `x`, `y` | The canvas cell. Columns grow with the depth of the tree, three branches fan out from the root, the Secret Art sits at the right between the two branches it needs. |
| `icon` (item id) | The node's picture in the screen: an item everyone knows (a blaze rod for the fist, a fire charge for the roar, flint and steel for the claw, a campfire for eating, magma cream for the fireball, a feather for the wings, a goat horn for the horn, blaze powder for the scales, a nether star for the Secret Art). |

## 6. The numbers

Units: damage keys (`damage`, `hit_damage`, `final_damage`, `recoil`) are RAW damage before the balance law scales it by the caster's EP (x1 at 1,000 EP up to x12) and caps it against players;
`range`, `radius`, `width`, `dash`, `final_radius`, `aura_radius` are blocks; `half_angle` is degrees; `burn_seconds` and `aura_burn_seconds` seconds; `cooldown`, `windup`, `gap`, `icd` ticks (20 per second);
`cost_pct`, `heal_pct`, `recharge_pct`, `eat_recharge_pct` percent (of the maximum magicules, or of the maximum health for `heal_pct`); `targets` and `hits` counts; `knockback`, `launch`, `hop` the strength of a push;
`eat_heal_ratio` a fraction of the eaten damage; `eats_lava` a 0 or 1 flag.

**Fire Dragon's Iron Fist** (`iron_fist`)

| value | L1 | L2 | L3 | L4 | L5 |
|---|---|---|---|---|---|
| `damage` | 6 | 7 | 8 | 9 | 11 |
| `burn_seconds` | 3 | 3 | 4 | 4 | 5 |
| `range` | 3.5 | 3.5 | 4.0 | 4.0 | 4.5 |
| `half_angle` | 35 | 35 | 40 | 40 | 45 |
| `targets` | 1 | 1 | 2 | 2 | 3 |
| `knockback` | 0.3 | 0.3 | 0.4 | 0.5 | 0.6 |
| `cost_pct` | 1 | 1 | 1 | 1.5 | 1.5 |
| `cooldown` | 100 | 90 | 80 | 70 | 60 |

**Fire Dragon's Roar** (`roar`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 9 | 11 | 13 |
| `range` | 9 | 11 | 13 |
| `width` | 1.4 | 1.8 | 2.2 |
| `burn_seconds` | 4 | 5 | 6 |
| `targets` | 4 | 6 | 8 |
| `windup` | 8 | 7 | 6 |
| `cost_pct` | 3 | 3 | 3.5 |
| `cooldown` | 160 | 140 | 120 |

**Fire Dragon's Claw** (`claw`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 7 | 8 | 10 |
| `range` | 3.2 | 3.6 | 4.0 |
| `half_angle` | 55 | 60 | 65 |
| `targets` | 3 | 4 | 5 |
| `burn_seconds` | 3 | 4 | 5 |
| `launch` | 0.5 | 0.6 | 0.7 |
| `hop` | 0.35 | 0.4 | 0.45 |
| `cost_pct` | 1.5 | 1.5 | 2 |
| `cooldown` | 120 | 100 | 80 |

**Eating Flame** (`flame_eater`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `heal_pct` | 2.0 | 3.0 | 4.0 |
| `recharge_pct` | 0.8 | 1.4 | 2.0 |
| `radius` | 1.5 | 2.0 | 2.5 |
| `eat_heal_ratio` | 0.25 | 0.4 | 0.6 |
| `eat_recharge_pct` | 2 | 3 | 4 |
| `eats_lava` | 0 | 0 | 1 |

**Fire Dragon's Brilliant Flame** (`brilliant_flame`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 8 | 10 | 12 |
| `radius` | 3.0 | 3.5 | 4.0 |
| `range` | 20 | 24 | 28 |
| `burn_seconds` | 4 | 5 | 6 |
| `targets` | 6 | 8 | 10 |
| `knockback` | 0.6 | 0.8 | 1.0 |
| `cost_pct` | 3 | 3.5 | 4 |
| `cooldown` | 200 | 180 | 160 |

**Fire Dragon's Wing Attack** (`wing_attack`)

| value | L1 | L2 | L3 | L4 |
|---|---|---|---|---|
| `damage` | 8 | 9 | 10 | 12 |
| `range` | 5.0 | 5.5 | 6.0 | 7.0 |
| `half_angle` | 50 | 55 | 60 | 65 |
| `targets` | 4 | 6 | 8 | 10 |
| `burn_seconds` | 3 | 4 | 4 | 5 |
| `knockback` | 0.9 | 1.1 | 1.3 | 1.5 |
| `cost_pct` | 2 | 2.5 | 3 | 3.5 |
| `cooldown` | 140 | 125 | 110 | 100 |

**Fire Dragon's Sword Horn** (`sword_horn`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 9 | 11 | 13 |
| `dash` | 5 | 6 | 7 |
| `width` | 1.1 | 1.3 | 1.5 |
| `targets` | 3 | 4 | 5 |
| `burn_seconds` | 4 | 5 | 6 |
| `knockback` | 0.8 | 1.0 | 1.2 |
| `windup` | 6 | 5 | 5 |
| `cost_pct` | 2.5 | 3 | 3.5 |
| `cooldown` | 150 | 130 | 110 |

**Blazing Scales** (`blazing_scales`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `burn_seconds` | 2 | 3 | 4 |
| `recoil` | 2 | 3 | 4 |
| `icd` | 40 | 30 | 20 |
| `aura_radius` | 1.5 | 2.0 | 2.5 |
| `aura_burn_seconds` | 2 | 2 | 3 |

**Crimson Lotus: Exploding Flame Blade** (`crimson_lotus`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `hit_damage` | 9 | 11 | 13 |
| `hits` | 3 | 4 | 5 |
| `final_damage` | 18 | 24 | 30 |
| `range` | 8 | 9 | 10 |
| `width` | 2.2 | 2.6 | 3.0 |
| `final_radius` | 4.5 | 5.0 | 5.5 |
| `knockback` | 1.0 | 1.3 | 1.6 |
| `gap` | 11 | 11 | 11 |
| `burn_seconds` | 6 | 7 | 8 |
| `targets` | 6 | 9 | 12 |
| `cost_pct` | 6 | 7 | 8 |
| `cooldown` | 1200 | 1080 | 900 |

Budget (design.md section 6), as enforced by `DragonBudget` on these tables at build time and on the live registry in the self-test: raw damage per hit 4 to 14 (this route: 6 to 13) and at most 30 for the Secret Art
(18 to 30 on the last blast, 9 to 13 on the pulses); cooldown at least 60 ticks (3 s) and at least 900 (45 s) for the Secret Art (this route: 60 ticks to 200, and 900 to 1200); cost 1 to 4 percent and at most 8 for the
Secret Art (this route: 1 to 4, and 6 to 8); at most 12 targets (the Secret Art reaches 12); heals far below the quarter of the health (the largest bite is 4 percent); no hard control at all in this route; no block is placed or broken;
no entity is spawned. A higher level is never worse: every power number rises and every delay falls with the level (a unit test says so).

### 6.1 Why these numbers

| Family | What the tables do | Why |
|---|---|---|
| `damage`, `hit_damage`, `final_damage` | Raw 6 to 13 for the ordinary techniques, rising with the level; the Secret Art 9 to 13 per pulse and 18 to 30 on the last blast. | `BalanceLaw` multiplies raw damage by the caster's EP scale (x1 at 1,000 EP, at most x12) and caps one hit on a player at 40 percent of the victim's health, so these numbers are the SHAPE of the power curve, not hit points. The budget of the design is raw 4 to 14 (30 for a Secret Art); the route uses its middle, and the cheap root punch is the weakest hit (6 to 11) so that every other node is a reason to spend SP. |
| `cooldown` | The punch 5 s down to 3 s; the other techniques 4 s to 10 s, falling by half a second to a second per level; the Secret Art 60, 54 and 45 s. | The budget's floors are 3 s and 45 s (Secret Art). A wider or longer-reaching technique waits longer, and each level shortens the wait, which rewards SP without raising the damage ceiling. |
| `cost_pct` | 1 to 1.5 percent for the punch, 1.5 to 4 for the rest, 6 to 8 for the Secret Art. | A share of the MAXIMUM magicules, so it scales with the player. The budget's ceilings are 4 and 8. The cost rises with the level, but more slowly than the damage, so a higher level is better value, not only stronger. |
| `range`, `width`, `radius`, `half_angle` | Reach grows with the level: a fist 3.5 to 4.5 blocks, a claw 3.2 to 4, a wing sweep 5 to 7, a breath 9 to 13, a fireball 20 to 28. | Each shape keeps its role (melee reaches little, the breath a room, the fireball a field of battle); a level makes it more forgiving, not a different technique. |
| `targets`, `hits` | 1 to 3 foes for the punch, 3 to 10 for the area techniques, 12 for the Secret Art; 3 to 5 pulses. | The budget's cap is 12 foes. The nearest are hit first, so a small technique cannot be a crowd-wide one. |
| `windup`, `gap` | The inhale of the Roar 6 to 8 ticks, the wind-up of the Sword Horn 5 to 6; the pulses of the Secret Art 11 ticks apart. | The inhale and the wind-up are the tell (0.25 to 0.4 s: long enough to step aside from a lane, short enough that the technique does not feel slow). 11 is the vanilla hurt-frame margin: a foe that was hit can be hit again from the 11th tick on, so every pulse lands and none is wasted or doubled. |
| `burn_seconds`, `aura_burn_seconds` | 3 to 6 s on the ordinary techniques, 6 to 8 s on the Secret Art, 2 to 4 s from the passives. | Vanilla burning (the game's own fire damage over time): the fire is an extra, never the main damage, and it stops at once in water. |
| `knockback`, `launch`, `hop` | 0.3 to 1.6 for pushes, 0.5 to 0.7 for the Claw's launch, 0.35 to 0.45 for its hop. | Strength of a push in the game's own units; the strongest push belongs to the Secret Art. Knock-back is not hard control, so it needs no control gate. |
| `heal_pct`, `recharge_pct`, `eat_heal_ratio`, `eat_recharge_pct` | Standing in flame: 2 to 4 percent of the health and 0.8 to 2 percent of the magicules per second; eating a hit: 25 to 60 percent of the cancelled damage and 2 to 4 percent of the magicules, once a second. | Small on purpose. `BalanceLaw.heal` also caps a heal at half the health and cuts it to 40 percent when the target was healed in the last 20 seconds, so a campfire is a comfort and not a way to be immortal. |
| `recoil`, `icd` (Blazing Scales) | A recoil of raw 2 to 4, at most once per attacker every 20 to 40 ticks. | A thorn must hurt a little and never be a weapon on its own; the interval stops a fast attacker from being drained by it, and the balance law scales it like any hit. |

## 7. WHY of the code, by file group

| Files | Why |
|---|---|
| `ft/FireDragonSkills` (the register, owned by this builder) | A ManasCore skill must be registered before the skill registry freezes, and every route has its OWN register so two builders never edit one file. Its static initializer also starts the timer, the passives and the self-test, so the lead's `SkillTreeSystem` needs no extra line; `init()` is public and idempotent in case the lead prefers to call it. |
| `ft/fire/FireDragonsIronFistSkill`, `...RoarSkill`, `...ClawSkill`, `...WingAttackSkill`, `...SwordHornSkill`, `...BrilliantFlameSkill`, `CrimsonLotusSkill` | One class per active node because the id of a registered skill and the node it serves must be one to one (the engine's self-test fails on a mismatch), and because each technique is a different shape (a cone, a lane, a dash, a thrown burst, a barrage). Each is a ManasCore skill so it lives in Tensura's skill menu, on Tensura's keys, with its cooldown display. Everything runs on the server in `perform`; the client sees only effect packets. |
| `ft/fire/FireDragonSkill` | The route-wide part: the route id, the shipped numbers, the colour of the warning dust. |
| `ft/fire/FireDragonPassives` | A passive is something that happens TO the player, so it is a game-bus listener on the damage-taken event and a once-a-second player tick, server side, each gated by `canCast`. The decisions are public static methods with plain arguments so the self-test can drive them (a fake player is invulnerable, so the real event never reaches it). |
| `ft/fire/FireDragonNumbers` | The shipped numbers once, in a PURE class: the fallback under the data, the node ids in one place, and the subject of the unit tests. |
| `ft/fire/FireDragonSelfTests` | The route's key of the CI smoke test, `tree.ft.fire_dragon_slayer`, and the questions only the Fire route can ask (do both passives answer?). |
| `ft/dragon/*` (shared with the Lightning route) | `DragonSkill` (the base of both routes' techniques), `DragonKit` (geometry and effect helpers written once so the balance law is applied the same way in all fifteen techniques), `DragonTimers` + `DelayQueue` (the only way a server waits: a bounded queue drained on the server tick), `DragonBudget` + `DragonTables` (the rules and the numbers as pure code, so they are tested), `DragonSelfTests` (the shared proof). |
| `textures/skill/ft/ft_fire_*.png`, `tools/skilltree/gen_ft_dragon_icons.py` | The skill menu needs a 32 px icon per skill. They are drawn with the owner's own icon framework so they sit next to the grimoire icons, in an ember frame of their own (not the clover frame of the grimoire). |
| `src/test/.../ft/dragon/*Test` | The route file, the code table, the budget and the source are checked against each other at build time (section 10). |

## 8. Decisions and alternatives rejected

* **Numbers in the JSON, a mirror of them in code (`FireDragonNumbers`).** Alternatives: only the JSON with literals at each `value(...)` call (the numbers scatter over seven classes, a typo cannot be seen), only the code (no datapack retune, which the owner asked for). With a mirror and a test that proves it equals the file, a cast can never read a hole and a retune is still data.
* **Delayed effects on a server-tick queue (`DragonTimers`).** Alternatives: a projectile or marker entity (the design allows very few spawned entities and each needs tracking and cleanup), the grimoire's `SpellRuntime` (a different system: the isolation rule forbids importing it), ManasCore's `onTick` of a held skill (the technique would have to be held and could be dropped by the key). A bounded queue of runnables is the smallest thing that works and is emptied when the server stops.
* **A charge is a teleport to a point computed first.** A velocity push depends on lag and friction and can end anywhere; a teleport to the last free spot of a 0.5 block stepped path is exact and cannot cross a wall. The small hop of the Claw IS a velocity push (it only has to feel like a jet).
* **Wind-ups only where the reach is beyond melee.** The Sword Horn (a rush) waits 5 or 6 ticks with its line already drawn, and charges along that line; the Iron Fist, the Claw and the Wing Attack (7 blocks at most) land at
  once, because a wind-up on a punch that is pressed every 3 to 7 s would make the route feel slow, and a foe 3 blocks away cannot react in 0.25 s anyway. The alternative, the same wind-up on everything, was rejected
  for that reason; the tables take a `windup` key on any node if the owner wants more.
* **Walls.** What comes out of the body (the punch, the claw, the wing sweep) needs line of sight to each foe, because `AbilityKit.cone` measures only angle and distance and a blow through a wall is a bug in PvP; a lane
  (the Roar, the Sword Horn, the Crimson Lotus) stops at the first wall by construction; a burst at a distant point (the fireball, the final blast) is NOT blocked, because it is an explosion and a wall test per foe is not worth the rare case.
* **Hurt-frames are not reset.** Several mods set the foe's invulnerability to 0 so a barrage always lands; here every hit obeys the game's ten-tick economy and the pulses are 11 ticks apart. That keeps the Secret Art inside the balance law's cap per hit and makes PvP honest.
* **Passives ask `canCast`, not `isNodeActive`.** The brief said `isNodeActive`; the two differ only in the class rule (the route's class must be the active class when the server asks for it). A player who owns two routes and plays one should get the passives of the one they play; with the class rule off they are identical.
* **Punches may miss.** A key press always answers with the flames and costs 1 to 1.5 percent. Techniques that NEED a target (Breakdown Fist, Roaring Thunder of the Lightning route) refuse before paying.
* **No Dragon Force, no motion sickness, no Dragon-Force-like mode.** Canon has them, but a transformation needs a buff-window model and balance that the owner has not asked for, and a motion sickness would punish players for riding boats and horses. The route format can take a Dragon Force node later (a JSON node and one skill class). They are listed here so nobody thinks they were forgotten.
* **Skill icons are generated, not vanilla item textures.** The owner's skill menu already has painted 32 px icons for every grimoire; vanilla item sprites next to them would look like a different game.

## 9. The exact route file

`src/main/resources/data/nusmp/skill_trees/fire_dragon_slayer.json`

```json
{
  "magic": "fairy_tail",
  "name": "Fire Dragon Slayer",
  "desc": "The lost magic of the fire dragons: eat the flame that would burn you, breathe it back, and learn the Secret Art that ends a fight.",
  "book": "nusmp:fire_dragon_slayer_book",
  "book_name": "Fang of Smoldering Insanity",
  "book_lore": "Bound in scorched hide and warm to the touch. Its readers were laughed at until the night the margins began to burn.",
  "color": "#E8541C",
  "icon": "minecraft:blaze_powder",
  "class_id": "fire_dragon_slayer",
  "duplicate_sp": 3,
  "loot_weight": 3,
  "nodes": [
    {
      "id": "iron_fist",
      "name": "Fire Dragon's Iron Fist",
      "desc": "Wrap a fist in flame and punch the foes in front of you. They burn, and at higher levels the blow reaches more than one.",
      "parents": [],
      "cost": 1,
      "max_level": 5,
      "skill": "nusmp:ft_fire_dragons_iron_fist",
      "passives": [],
      "values": {
        "damage": [6, 7, 8, 9, 11],
        "burn_seconds": [3, 3, 4, 4, 5],
        "range": [3.5, 3.5, 4.0, 4.0, 4.5],
        "half_angle": [35, 35, 40, 40, 45],
        "targets": [1, 1, 2, 2, 3],
        "knockback": [0.3, 0.3, 0.4, 0.5, 0.6],
        "cost_pct": [1, 1, 1, 1.5, 1.5],
        "cooldown": [100, 90, 80, 70, 60]
      },
      "x": 0,
      "y": 4,
      "icon": "minecraft:blaze_rod"
    },
    {
      "id": "roar",
      "name": "Fire Dragon's Roar",
      "desc": "Draw a deep breath, then roar a lane of fire down the way you look. Everything in it burns.",
      "parents": [ { "node": "iron_fist", "level": 3 } ],
      "cost": 2,
      "costs": [2, 3, 3],
      "max_level": 3,
      "skill": "nusmp:ft_fire_dragons_roar",
      "passives": [],
      "values": {
        "damage": [9, 11, 13],
        "range": [9, 11, 13],
        "width": [1.4, 1.8, 2.2],
        "burn_seconds": [4, 5, 6],
        "targets": [4, 6, 8],
        "windup": [8, 7, 6],
        "cost_pct": [3, 3, 3.5],
        "cooldown": [160, 140, 120]
      },
      "x": 2,
      "y": 1,
      "icon": "minecraft:fire_charge"
    },
    {
      "id": "claw",
      "name": "Fire Dragon's Claw",
      "desc": "A flaming kick with a jet of fire under the heel: it hops you forward and launches the foes in front of you into the air.",
      "parents": [ { "node": "iron_fist", "level": 2 } ],
      "cost": 2,
      "costs": [2, 2, 3],
      "max_level": 3,
      "skill": "nusmp:ft_fire_dragons_claw",
      "passives": [],
      "values": {
        "damage": [7, 8, 10],
        "range": [3.2, 3.6, 4.0],
        "half_angle": [55, 60, 65],
        "targets": [3, 4, 5],
        "burn_seconds": [3, 4, 5],
        "launch": [0.5, 0.6, 0.7],
        "hop": [0.35, 0.4, 0.45],
        "cost_pct": [1.5, 1.5, 2],
        "cooldown": [120, 100, 80]
      },
      "x": 2,
      "y": 4,
      "icon": "minecraft:flint_and_steel"
    },
    {
      "id": "flame_eater",
      "name": "Eating Flame",
      "desc": "Passive. Fire does not hurt a dragon slayer, it feeds him. Standing near flames, or being struck by an enemy's fire, heals you and refills your magicules. Flames you made yourself do not count; lava needs the last level.",
      "parents": [ { "node": "iron_fist", "level": 1 } ],
      "cost": 1,
      "costs": [1, 2, 2],
      "max_level": 3,
      "skill": "",
      "passives": ["flame_eater"],
      "values": {
        "heal_pct": [2.0, 3.0, 4.0],
        "recharge_pct": [0.8, 1.4, 2.0],
        "radius": [1.5, 2.0, 2.5],
        "eat_heal_ratio": [0.25, 0.4, 0.6],
        "eat_recharge_pct": [2, 3, 4],
        "eats_lava": [0, 0, 1]
      },
      "x": 2,
      "y": 7,
      "icon": "minecraft:campfire"
    },
    {
      "id": "brilliant_flame",
      "name": "Fire Dragon's Brilliant Flame",
      "desc": "Bring both hands together and throw a great fireball at what you aim at. It bursts where it lands and throws the foes around it back.",
      "parents": [ { "node": "roar", "level": 2 } ],
      "cost": 3,
      "max_level": 3,
      "skill": "nusmp:ft_fire_dragons_brilliant_flame",
      "passives": [],
      "values": {
        "damage": [8, 10, 12],
        "radius": [3.0, 3.5, 4.0],
        "range": [20, 24, 28],
        "burn_seconds": [4, 5, 6],
        "targets": [6, 8, 10],
        "knockback": [0.6, 0.8, 1.0],
        "cost_pct": [3, 3.5, 4],
        "cooldown": [200, 180, 160]
      },
      "x": 4,
      "y": 0,
      "icon": "minecraft:magma_cream"
    },
    {
      "id": "wing_attack",
      "name": "Fire Dragon's Wing Attack",
      "desc": "Swing both flaming arms out like the wings of a dragon in a wide sweep, burning and throwing back everything in front of you.",
      "parents": [ { "node": "claw", "level": 2 } ],
      "cost": 2,
      "costs": [2, 2, 3, 3],
      "max_level": 4,
      "skill": "nusmp:ft_fire_dragons_wing_attack",
      "passives": [],
      "values": {
        "damage": [8, 9, 10, 12],
        "range": [5.0, 5.5, 6.0, 7.0],
        "half_angle": [50, 55, 60, 65],
        "targets": [4, 6, 8, 10],
        "burn_seconds": [3, 4, 4, 5],
        "knockback": [0.9, 1.1, 1.3, 1.5],
        "cost_pct": [2, 2.5, 3, 3.5],
        "cooldown": [140, 125, 110, 100]
      },
      "x": 4,
      "y": 3,
      "icon": "minecraft:feather"
    },
    {
      "id": "sword_horn",
      "name": "Fire Dragon's Sword Horn",
      "desc": "Set yourself ablaze and charge head first through the foes ahead of you, throwing them aside in a pillar of fire. The charge stops at a wall.",
      "parents": [ { "node": "claw", "level": 2 } ],
      "cost": 2,
      "costs": [2, 3, 3],
      "max_level": 3,
      "skill": "nusmp:ft_fire_dragons_sword_horn",
      "passives": [],
      "values": {
        "damage": [9, 11, 13],
        "dash": [5, 6, 7],
        "width": [1.1, 1.3, 1.5],
        "targets": [3, 4, 5],
        "burn_seconds": [4, 5, 6],
        "knockback": [0.8, 1.0, 1.2],
        "windup": [6, 5, 5],
        "cost_pct": [2.5, 3, 3.5],
        "cooldown": [150, 130, 110]
      },
      "x": 4,
      "y": 5,
      "icon": "minecraft:goat_horn"
    },
    {
      "id": "blazing_scales",
      "name": "Blazing Scales",
      "desc": "Passive. Your body burns like a dragon's: hostile creatures that stand next to you catch fire, and whoever strikes you in melee is burned and singed.",
      "parents": [ { "node": "flame_eater", "level": 2 } ],
      "cost": 2,
      "costs": [2, 2, 3],
      "max_level": 3,
      "skill": "",
      "passives": ["blazing_scales"],
      "values": {
        "burn_seconds": [2, 3, 4],
        "recoil": [2, 3, 4],
        "icd": [40, 30, 20],
        "aura_radius": [1.5, 2.0, 2.5],
        "aura_burn_seconds": [2, 2, 3]
      },
      "x": 4,
      "y": 8,
      "icon": "minecraft:blaze_powder"
    },
    {
      "id": "crimson_lotus",
      "name": "Crimson Lotus: Exploding Flame Blade",
      "desc": "Secret Art. Swirl both arms into a spiral of flame that barrages the lane before you, every strike bursting where it lands, and close it with one great explosion.",
      "parents": [ { "node": "roar", "level": 3 }, { "node": "wing_attack", "level": 3 } ],
      "cost": 4,
      "costs": [4, 6, 8],
      "max_level": 3,
      "skill": "nusmp:ft_crimson_lotus_exploding_flame_blade",
      "passives": [],
      "values": {
        "hit_damage": [9, 11, 13],
        "hits": [3, 4, 5],
        "final_damage": [18, 24, 30],
        "range": [8, 9, 10],
        "width": [2.2, 2.6, 3.0],
        "final_radius": [4.5, 5.0, 5.5],
        "knockback": [1.0, 1.3, 1.6],
        "gap": [11, 11, 11],
        "burn_seconds": [6, 7, 8],
        "targets": [6, 9, 12],
        "cost_pct": [6, 7, 8],
        "cooldown": [1200, 1080, 900]
      },
      "x": 6,
      "y": 2,
      "icon": "minecraft:nether_star"
    }
  ]
}
```

## 10. What proves it, and what could not be proved

Checked at build time (all pure, run with the JUnit shim here, run by the real JUnit in CI): `FireDragonRouteTest` (the file obeys the schema and the price bands; the owner's names; Iron Fist then Roar then the Secret Art;
the Secret Art needs two different branches; the file and the code table say the same numbers and obey the budget; every number the code reads exists and every number is read; the register registers exactly the skills the
file names, each class serves the node of its id; every skill has its 32 px icon), `DragonBudgetTest`, `DelayQueueTest`. Each was mutated once (a cap loosened, the monotone rule removed, a number changed in the file, a key misspelt in a skill, a registered id and an icon name changed) and failed as it should.
The route files were also run through the REAL schema of the core builder (`RouteSchema`, `RouteIndex`, `RouteWire`) and its `SkillTreeFilesTest` and `IsolationTest`: no problem and no warning; 5.3 KB on the wire.

**Type-checked, not built.** There was no Gradle build and no game in this session. All sources under `skilltree/ft/**` were compiled with `javac` against API stubs of Minecraft, NeoForge, ManasCore and Tensura
together with the REAL sources of the core builder; the only errors left are four reports "MobEffectInstance is abstract", which are an artifact of the stubs (the real class is concrete). The JUnit tests ran on a
small shim of JUnit 5 (the plain `assertTrue`, `assertFalse`, `assertEquals`, `assertNotNull` family), not under Gradle.

In the game, on the CI server, `tree.ft.fire_dragon_slayer` (the key prefix `tree` is the one the smoke script expects): the route is loaded and its numbers obey the budget; every node agrees with a registered tree skill; WITHOUT the route
the stand-in player cannot cast any of them, spends nothing, hurts nothing and starts no cooldown; WITH the route every technique is cast at level 1 and at the top level next to a dummy mob and nothing throws or is swallowed;
at least one hurts the dummy; the delayed ones queued their follow-ups; both passives answer (fire eaten, own fire not, lava only at the top level, a blow answered with fire, nothing without the node).

**Not verified (nothing could be run in the game):** how the effects LOOK (the effect shapes were chosen from the descriptions of their layers: sizes, durations and the colour argument of `WIND_SLASH` are guesses to be judged by eye);
the feel of the timings (inhale length, flight time, pulse spacing); how Tensura's skill menu draws the icons; that a fake player can be taught a skill, can stand at (0.5, 100, 0.5) and can hurt a mob in the CI world (the self-test is written
for it, and reports the exact exception if not); `LivingEntity.knockback` and the velocity push of the Claw for a player's client (they rely on `hurtMarked`); the Tensura damage-type keys `FIRE_ELEMENTAL` and `FLAME_BREATH` and the
effect `tensura:paralysis` (the latter is used by the Lightning route); the `nusmp.skill.<id>` lang keys (taken from the 465 existing entries of the mod). The route depends on two additions of the core builder that the contract stub does
not have: `RouteRegistry.value(...)` and `SkillTreeApi.isEnabled()`.

## 11. Files

New: `skilltree/ft/dragon/**`, `skilltree/ft/fire/**`, `data/nusmp/skill_trees/fire_dragon_slayer.json`, `assets/nusmp/textures/skill/ft/ft_fire_*.png`, `tools/skilltree/gen_ft_dragon_icons.py`,
`src/test/.../skilltree/ft/dragon/*Test`, this page. Edited: `skilltree/ft/FireDragonSkills.java` (the register body; its declaration of `SKILLS` is unchanged). Lang: `nusmp.skill.ft_*` keys, delivered as a fragment.
