# Vassal Whip (`nusmp:vassal_whip`, `HeroKind.WHIP`, class Seven Star, style SWING)

Builder `hva`. Code: `heroweapon/weapon/whip/**` (+ the shared helpers of the four striking weapons in `heroweapon/weapon/strike/**`), forms file
`data/nusmp/hero/forms/vassal_whip.json`, lang fragment `hva.lang.json`. This page is written in the order the art builder needs it: the look first.

## 1. Look (for the art builder `hart2`)

Canon, from search summaries of the fandom wiki: the Vassal Whip (Seven Star Whip) of Raphtalia's world was made to direct the monsters that pulled the Bow
Hero's vassal chariot, and its special ability is to control the power of monsters; in the web novel it is Wyndia's. The legend says it could take the shape of a
chain; canon forms include a **Morning Star** form (skill Gigantic Star), a **Rope Whip**-type form (Air Strike Backwhip) and the **Dragon Hourglass Sand Whip**
(skill Portal Whip, unlocked by absorbing sand from the Dragon Hourglass); canon skills include Bind Whip, Lightning Whip, Lightning Impact Whip and Dimension
Whip. The wiki gives no picture. The model below is DESIGN.

* **Silhouette.** A braided whip held coiled: a stout handle, two or three loops of the lash gathered at the hand, and the free lash trailing ahead in one
  smooth S-curve that tapers to a **weighted gem tip** in a small cage. The curve is what makes it read as a whip and not a rope: keep it graceful, one bend and a
  half, never a zigzag. It is the longest-reaching of the four striking weapons.
* **Size.** Class `coilwhip` (1.1 to 1.7 blocks apparent) with the lash posed, aim for 1.5; the class `whip` (1.6 to 2.4) is for a fully extended lash and is not
  wanted here because a long thin mesh reads badly in the hand. The handle is 0.28 of the length from the butt (class grip 0.15 is fine).
* **Parts (8 to 14 named parts).** `pommel`, `pommel_gem`, `grip_braid`, `guard_disc`, `ring_a`, `ring_b` (gold rings on the handle), `coil_loops` (three stacked
  loops at the hand, one part), `lash_a` to `lash_e` (five tapering segments along the S-curve; keep the quad count low by making each a simple loft), `tip_cage`,
  `tip_gem`, four tiny `tip_spikes` (the morning-star hint).
* **Palette.** Handle and lash: braided dark leather `#4A3426` with a lighter braid line `#6B4A33`. Rings, guard disc, cage and pommel cap: gold `#C9A24A`.
  Tip spikes: steel `#9AA3B2`.
* **The gem.** Two violet cabochons: the **pommel gem** (about 0.1 across, the series' central jewel) and the **tip gem** (a faceted ball about 0.12 across in the
  gold cage). Glow colour `#9B5CFF` (electric violet, the colour of the Lightning Whip and of the strings of the Command).
* **Glow skin.** Both gems, plus a hairline of violet thread inlaid along the lash (alpha under 30 percent, brighter toward the tip), and the seven-star mark (seven tiny
  dots) on the guard disc. Nothing else glows.
* **Orientation.** Mesh -x is forward: the free lash leaves the hand toward -x and curves up (+y) with the S-curve in the item plane (so it reads from the side); the
  handle is at the butt (low y). Tip up and forward on the icon diagonal.
* **Icon.** Keep the 16 x 16 sprite as the inventory icon (a curled whip with a violet dot).
* **Do not.** No curse look (the Whip carries no curse series). No straight stick: a whip posed rigid looks like a wand.

## 2. Canon: what is from a summary, what is from memory, what is design

The fandom wiki could not be opened from the build machine (a fetch of its pages fails), so every canon claim below comes from SEARCH SUMMARIES of the pages named, run on
2026-10-18. Tags: **S** = a search summary of that page, **M** = my memory of the series, **D** = my design for Minecraft. Nothing here was read on the page itself.

Pages (all on https://shield-hero.fandom.com/wiki/): `Vassal_Whip`, `Vassal_Weapons`, `Vassal_Weapons/Web_Novel`, `Heroes`, `Heroes_(Web_Novel)`, `Wyndia_(Web_Novel)`,
`Seven_Star_Whip_Hero_(chapter)`, `Takt_Alsaholn_Faubrey/Powers_and_Abilities`, `Curse_Series`, `Curse_Series/Weapon_List`, `Legendary_Weapons/Power-up_Methods`.

| fact | tag | what the mod does with it |
|---|---|---|
| The Vassal Whip (Seven Star Whip) of Raphtalia's world was made to direct the monsters that pulled the Bow Hero's vassal chariot, to give him mobility; its special ability is to control the power of monsters | S | the **Command** (section 4) is its special ability; the reach and the tethers are the mobility |
| Holders: Takt (light novel; Naofumi stripped him of his vassal weapons), Wyndia (web novel: "Whip Hero"); Holn Anthreya made Fitoria with it | S, conflicting | not used |
| Skills named: **Bind Whip** (coils round the target like a living snake and holds it), **Lightning Whip** (lightning over a larger area), Lightning Impact Whip (lightning on the whip, then a bind), **Dimension Whip** (light novel: the whip splits into several lashes that fly at the target; web novel: a high-level bind that can bind anything; the pages disagree), Air Strike Backwhip, **Gigantic Star**, **Portal Whip**, Whip of 0 and Whip of X0 (effective against illegitimate powers and gods; not described) | S | Bind Whip, Lightning Whip, Dimension Whip (both readings: many lashes AND a bind), Gigantic Star and Portal Whip are techniques; the rest are not built |
| Forms: a **Morning Star** form (equip bonus Gigantic Star), the **Dragon Hourglass Sand Whip** (unlocked by absorbing sand from the Dragon Hourglass; gives Portal Whip), a Rope Whip with Air Strike Backwhip (marked unconfirmed); a chain and a mace form appear in a legend | S | Morning Star Whip and Hourglass Sand Whip are forms; the chain, the mace and the rope form are not built (the chain belongs to another weapon) |
| Power-up method: **Growth Revision** (sacrifice levels for a permanent stat growth modifier; usable on non-heroes with consent); the Heroes page calls it Growth Elevation | S | not built: it is a levelling system, and the bond level belongs to the core |
| Cursed whips: the summaries found no cursed Vassal Whip; the only mention is "Idol's whip" of the curse text, which is not this weapon | S | the Whip has no curse form (section 7) |
| Lash, Reel In, Tether Swing, the forms Vine Whip and Storm Whip, the numbers and the safeguards of the Command | D | the owner's brief: reach, lashes, pulling and tethering; forms from string, leather and vines; a bounded "command" |

## 3. The forms tree

Five forms, all ordinary (the brief asks for 3 to 5, the base form counts): `base` -> `vine_whip`, then two branches `storm_whip` and `morning_star_whip`, and `hourglass_whip` which
needs both. Names: Small Whip, Vine Whip (the brief's string, leather and vines), Storm Whip (design: the Lightning Whip and its charge),
Morning Star Whip and Hourglass Sand Whip (canon). The top form has two parents, and so a tier one deeper than the deepest of them.

The table is generated from `data/nusmp/hero/forms/vassal_whip.json`:

| form (id) | tier | parents | requirements | mastery | stats | ability bonus | skills (slot: id) | passives |
|---|---|---|---|---|---|---|---|---|
| Small Whip (`base`) | 0 | - | none | 0 | movement_speed +0.01 | - | use: `whip.lash`, sneak: `whip.bind_whip`, key1: `whip.reel_in` | `coil` |
| Vine Whip (`vine_whip`) | 1 | `base` | 48 x `minecraft:string`, 16 x `minecraft:leather`, 24 x `minecraft:vine` | 100 | attack_speed +0.2, reach +0.5, movement_speed +0.02 | reach +0.1, movement_speed +0.005 | use: `whip.lash`, sneak: `whip.bind_whip`, key1: `whip.reel_in`, key2: `whip.tether_swing` | `coil` |
| Storm Whip (`storm_whip`) | 2 | `vine_whip` | 48 x `minecraft:copper_ingot`, 4 x `minecraft:lightning_rod`, 16 x `minecraft:amethyst_shard` | 180 | attack_damage +1, reach +0.5, movement_speed +0.03 | attack_damage +0.25 | use: `whip.lash`, sneak: `whip.lightning_whip`, key1: `whip.bind_whip`, key2: `whip.tether_swing`, key3: `whip.command` | `coil`, `static_charge` |
| Morning Star Whip (`morning_star_whip`) | 2 | `vine_whip` | 40 x `minecraft:iron_ingot`, 8 x `minecraft:chain`, 16 x `minecraft:flint` | 180 | attack_damage +3, armor +2, knockback_resistance +0.15, reach +0.5 | armor +0.5 | use: `whip.lash`, sneak: `whip.gigantic_star`, key1: `whip.reel_in`, key2: `whip.bind_whip`, key3: `whip.command` | `coil`, `star_weight`, `taskmaster` |
| Hourglass Sand Whip (`hourglass_whip`) | 3 | `storm_whip`, `morning_star_whip` | 64 x `minecraft:sand`, 16 x `minecraft:ender_pearl`, 4 x `minecraft:clock` | 320 | attack_damage +3, armor +2, reach +1, movement_speed +0.05 | attack_damage +0.75, reach +0.25, movement_speed +0.01 | use: `whip.lash`, sneak: `whip.portal_whip`, key1: `whip.command`, key2: `whip.tether_swing`, key3: `whip.dimension_whip` | `coil`, `taskmaster`, `hourglass_flow` |

Fairness of the climb: tier 1 is 88 common items (string, leather, vines), tier 2 is 68 items of copper, lightning rods and amethyst (storm) or 64 of iron, chain and flint (morning
star), tier 3 is 64 sand, 16 ender pearls and 4 clocks (a real task: the pearls and the gold of the clocks). The Hourglass Whip's two parents mean the player learns both
branches first. Every item id was checked against the vanilla item list; the CI self-test `hero.forms` resolves them again. Reach bonuses of +0.5 to +1 are in the forms' stats (the
forms system's closed key set has `reach`).

## 4. The techniques

Nine techniques, one ultimate. The budget rows (the pure table `WhipNumbers`, judged by `Budget` in `WhipNumbersTest`; "raw" is before the bond-level factor 1.0 to 2.2 and the balance
law; the cooldown is the BASE cooldown, see section 11):

| skill id | kind | raw per hit | hits | cooldown | cost (of max magicules) | targets |
|---|---|---|---|---|---|---|
| `whip.lash` | damage | 6 | 1 | 60 ticks (3 s) | 1 % | 3 |
| `whip.bind_whip` | utility | 3 | 1 | 160 ticks (8 s) | 2 % | 1 |
| `whip.reel_in` | utility | 3 | 1 | 140 ticks (7 s) | 2 % | 1 |
| `whip.tether_swing` | utility | 0 | 0 | 140 ticks (7 s) | 1.5 % | 1 |
| `whip.lightning_whip` | damage | 7 | 1 | 100 ticks (5 s) | 2 % | 5 |
| `whip.command` | utility | 0 | 0 | 400 ticks (20 s) | 3 % | 3 |
| `whip.gigantic_star` | heavy | 12 | 1 | 120 ticks (6 s) | 2.5 % | 12 |
| `whip.portal_whip` | utility | 6 | 1 | 140 ticks (7 s) | 2 % | 8 |
| `whip.dimension_whip` | ultimate | 14 | 1 | 900 ticks (45 s) | 6 % | 12 |

**The control rules.** Every technique that holds, pulls or turns a foe first passes the shared control lock (`HeroCtx.control()`, one use per 12 seconds per wielder, the law's
rule) and then the balance law's own caps (`BalanceLaw.controlTicks`: at most 3 s on a player, 5 s on a mob, 1 s on a boss, halved or quartered when the same foe was controlled in
the last 30 s). A boss is never pulled or rooted, only slowed. When the lock is busy the technique says how long it will be ("control is ready in n s") and does its softer
alternative (Bind Whip slows) or refuses at no cost (Reel In, Command). The Hammer's break and the Whip share that one lock, so a wielder of the Whip really has one hard control every 12 s.

* **Lash** (`use` of every form).
  *What:* cracks the whip along the look, 7 blocks (stopped by a wall): it cuts up to 3 foes in the line (0.8 blocks either side) and, with Coil, slows each for a second. With the Storm Whip's Static Charge it also charges the foe (section 5). It is instant (a hitscan along the look): the damage is small enough not to need a tell, and a mob under a Command is not struck.
  *Numbers:* raw 6, up to 3 foes.
  *Counter:* it cannot be dodged once cast; it is 6 raw, and the cooldown is 3 s.
  *Cost / cooldown:* 1 %, 60 ticks.
* **Bind Whip** (`sneak` of the Small and Vine Whips, `key1` of the Storm Whip, `key2` of the Morning Star Whip; canon).
  *What:* the whip coils round the foe you face (within 9 blocks, 20 degrees, line of sight) like a living snake and holds it. Instant.
  *Numbers:* raw 3; if the control lock is free: Slowness VII and Tensura's Movement Interference for 40 ticks (2 s) at most, as the balance law allows, and the foe is marked BOUND for 100 ticks (the Morning Star's Star Weight reads that mark). A boss is only slowed (Slowness III, 1 s). If the lock is busy: Slowness III for 2 s and the message "The bind only slows: control is ready in n s."
  *Tell:* the violet lash to the foe, seal chains in gold round it while it is held.
  *Counter:* break the line of sight, keep more than 9 blocks away; the hold is 2 s at most and a second bind on the same foe in 30 s is shorter.
  *Cost / cooldown:* 2 %, 160 ticks.
* **Reel In** (`key1` of the Small, Vine and Morning Star Whips).
  *What:* snares the foe you face from up to 11 blocks and yanks it to you. Refuses (no cost) on a boss ("A boss cannot be pulled"), on a foe already close, and when the lock is busy. The foe is marked BOUND.
  *Numbers:* raw 3 first (its shove would undo the pull), then the foe's velocity is set toward the wielder: a speed that grows with the distance, clamped to 0.45 to 1.5 blocks a tick, with a small lift of 0.28.
  *Tell:* the violet lash to the foe.
  *Counter:* none needed (3 raw); the foe arrives where the wielder's friends can strike it.
  *Cost / cooldown:* 2 %, 140 ticks.
* **Tether Swing** (`key2` of the Vine, Storm and Hourglass Whips).
  *What:* throws the whip at the foe or the ledge you look at, up to 16 blocks away and at least 3 blocks away, and swings the wielder to it: the wielder's own velocity is set toward the mark (0.6 to 1.6 blocks a tick, a lift of 0.25 to 0.9 that grows with the height of the mark), with Slow Falling for 2 seconds, and no fall damage. Nothing is pulled but the wielder. If the wielder looks at neither a foe nor a block: "The whip has nothing to catch on", no cost.
  *Numbers:* no damage. Hourglass Flow shortens the cooldown to 80 percent.
  *Tell:* the violet lash to the mark. It is not an attack.
  *Cost / cooldown:* 1.5 %, 140 ticks (112 with Hourglass Flow).
* **Lightning Whip** (`sneak` of the Storm Whip; canon).
  *What:* cracks the whip on the foe you face (within 9 blocks, 35 degrees, line of sight); after 5 ticks lightning jumps from it to the nearest foes, up to 5 in all, each within 5.5 blocks of the one before.
  *Numbers:* raw 7 on the first foe and four-fifths of the last at every jump (7, 5.6, 4.5, 3.6, 2.9), times 1.3 on a foe charged by the Storm Whip's lash (the charge is used up); each foe struck is slowed (Slowness I, 1.5 s).
  *Tell:* a violet line from the hand to the first foe and a gathering of dust on it for 5 ticks, then the lightning spear shape from foe to foe and thunder.
  *Counter:* get out of the line or out of the 9 blocks in the 0.25 s, or stand apart from your friends (the jump looks for the nearest foe of the wielder, the chain is only as long as the crowd).
  *Cost / cooldown:* 2 %, 100 ticks.
* **Command** (`key3` of the Storm and Morning Star Whips, `key1` of the Hourglass Whip; the Vassal Whip's special ability).
  See "How the Command works" below.
  *Numbers:* up to 3 monsters within 14 blocks, 6 seconds, one use of the control lock; cost 3 %, cooldown 400 ticks (20 s).
* **Gigantic Star** (`sneak` of the Morning Star Whip; canon).
  *What:* whirls the spiked star on the end of the whip over the wielder's head and slams it down 4.5 blocks ahead (pulled back from a wall); everything within 3 blocks of the spot is hit.
  *Numbers:* raw 12 (times 1.25 on a foe that is BOUND, with Star Weight; clamped to the heavy cap 20), up to 12 foes, shove 0.9, lift 0.2. The blow lands 10 ticks after the cast; the wielder is slowed (Slowness III) for those ticks.
  *Tell:* a closing ring of the full radius at the spot for 14 ticks and a base ring, a chain sound; a strike ring and an impact when it lands.
  *Counter:* leave the 3-block ring.
  *Cost / cooldown:* 2.5 %, 120 ticks.
* **Portal Whip** (`sneak` of the Hourglass Whip; canon, the Dragon Hourglass Sand Whip's skill).
  *What:* cracks the whip into a gate of sand and steps through it up to 14 blocks along the look, to a place with room to stand (`Fx.teleportLook`; else "There is no place to step through to", no cost). Two gates are drawn, at the start and at the arrival. Foes within 3.2 blocks of the arrival are lashed.
  *Numbers:* raw 6 on up to 8 foes, Slowness II for 2 s, shove 0.4. Hourglass Flow shortens the cooldown to 80 percent.
  *Tell:* the gates; the lash on arrival is small.
  *Counter:* none (it is a movement technique); the arrival blow is 6 raw.
  *Cost / cooldown:* 2 %, 140 ticks (112 with Hourglass Flow).
* **Dimension Whip** (`key3` of the Hourglass Whip; canon, the ultimate).
  *What:* the whip splits into countless lashes that reach every foe within 11 blocks at once, after a warning; each takes a heavy lash and is bound. A foe that is a commanded monster is not a target. The wielder is held still (Slowness IV) while it gathers.
  *Numbers:* raw 14, up to 12 foes, each still within 13 blocks of the wielder when the lashes fall: bound as far as the control lock and the balance law allow (Slowness VII and Movement Interference for 50 ticks at most), once for all foes with ONE use of the lock; a boss is only slowed (1 s); if the lock was busy, the foes are only slowed (Slowness III, 2 s) and the message says so. The warning is 14 ticks (0.7 s).
  *Tell:* a ring of the full radius on the ground, a gate over the wielder's head and violet strings from the wielder's hand to every foe, all for 20 ticks; an ender sound.
  *Counter:* leave the 11-block ring in the 0.7 seconds it gives.
  *Cost / cooldown:* 6 %, 900 ticks (45 s).

### How the Command works (and why it is built the way it is)

Up to three hostile monsters within 14 blocks stop hunting the wielder and their friends and fight the nearest other monster for six seconds, on thin violet strings from the
wielder's hand to each of them, and then go back to what they were. It is the most dangerous kind of technique to write: a mob's own AI keeps trying to take its target back, and a
blow from a mob the wielder "owns" must never land on the wielder or a friend. The limits are the owner's: at most 3 monsters, 6 seconds, one use of the shared control lock, never
a boss, never a player.

* **Who may be commanded:** a plain hostile mob (it is a `Mob` and an `Enemy`), alive and with its AI on, not a boss by the balance law's test (the boss tag or 300 health), not already commanded, not a creeper, ghast or blaze (their blows break blocks or light fires), not an owned pet or summon. The nearest eligible ones are taken.
* **What happens:** each gets two marks in its own saved data (the wielder's id and the tick the order ends, `nusmp_wh_cmd_by`, `nusmp_wh_cmd_until`), its target is cleared and its path stopped, and with Taskmaster it gets Strength I for the six seconds. Every 4 ticks (a `Strike.zone` of 120 ticks, the only repeating task) each monster's target is set to the nearest other monster the wielder could hurt within 12 blocks of it (never a player, an ally, another commanded monster or itself); if there is none it has no target.
* **The two listeners** (on the game's event bus, registered once from the logic's constructor, each wrapped in try/catch): when a commanded monster is about to get a new target, the change is cancelled if the new target is a player, the wielder, an ally of the wielder or another commanded monster (clearing a target always goes through); when a commanded monster's blow is about to land, it is cancelled if the victim is a player, the wielder or an ally of the wielder; and, if the wielder is offline, every blow of a commanded monster is cancelled. The damage listener does not depend on how the monster's AI picks targets, so it protects players even from a monster whose brain ignores the target change.
* **The end:** after 120 ticks the marks are removed and the monster's target cleared. The marks expire by the CLOCK as well, so a wielder who dies, leaves, changes weapon or changes world lets the monsters go within six seconds with nothing left in a table (the steering stops at once, the protection of players and allies lasts until the clock runs out).
* **What it does NOT do:** it takes no loot or experience credit for the wielder, it changes no block, it never touches a boss or a player, and it cannot be stacked on one monster (a commanded monster is not eligible again).

## 5. The passives

| id | form that gives it | what it does |
|---|---|---|
| `coil` | every form | every lash and every full swing (attack bar at 90 percent or more) leaves the foe slowed for a second (Slowness I) |
| `static_charge` | Storm Whip | a lash or a full swing charges the foe for 6 seconds; the next Lightning Whip that reaches it deals 30 percent more and uses the charge up |
| `star_weight` | Morning Star Whip | a bound foe (bound by a Bind Whip, a Reel In or a Dimension Whip in the last 5 seconds) takes 25 percent more from the Gigantic Star |
| `taskmaster` | Morning Star and Hourglass Whips | the monsters taken by the Command are strengthened (Strength I) for as long as they obey |
| `hourglass_flow` | Hourglass Sand Whip | the Tether Swing and the Portal Whip come back a fifth sooner (cooldown x0.8) |

## 6. Tethers

A tether sets a velocity, and a velocity that is too big or points nowhere can fling a body out of the world or into the ground. The arithmetic is separate, pure and tested
(`TetherMath`): a pull never exceeds 1.5 blocks a tick, a swing 1.6; both grow with the distance but are clamped; the lift is bounded; a foe that is already close is not pulled at
all; and a direction with no length gives no motion. The wielder's Tether Swing cancels fall damage and adds Slow Falling for 2 seconds so that the landing is soft.

## 7. The curse series: none

The brief says a Seven Star weapon has no curse series unless it is argued. The Whip has none: the summaries found no cursed Vassal Whip (section 2), and its special ability is the
most delicate technique of the four: a Command whose limits depend on the wielder staying in control of their own mind. Adding corruption, an Overrun that holds the wielder, and
a monster-control technique together would be a combination I could not make safe on paper. The forms file has no curse form and `FormsCheck` for the Whip is built with
`curseAllowed = false`.

## 8. Engravings (the owner's rule 6, design.md section 16)

The exact line for `item/WeaponEngravings.java` (the lead integrates it):

```java
Map.entry("vassal_whip", List.of(e("swift", 1), e("enervation", 1), e("magic_interference", 1))),   // a quick lash that binds and saps; it takes hold of the power of monsters
```

* `swift` 1: the whip is the longest-reaching and quickest to crack of the four striking weapons (the lash is its basic attack).
* `enervation` 1: it slows, binds and saps what it holds (Coil, the Bind Whip, the Lightning Whip's slow); the table gives it to the weapons that weaken (the Club of Rot, the Butcher Cleaver, Quickling's Ivy).
* `magic_interference` 1: the canon special ability is to control the power of monsters, which is interfering with a creature's own power; the table gives it to the Magebane weapons for the same kind of reason.

## 9. The code: the WHY of each class

All in `src/main/java/com/newuniverse/nusmp/heroweapon/weapon/`. The package `strike/` is shared by the four striking weapons (its files and their WHY are in the table of
`weapon_hammer.md`, section 9): `SkillSpec`, `Budget`, `FormsCheck`, `Strike`, `StrikeFx`, `StrikeSkill`, `StrikeSelfTest`.

| file (`whip/`) | why it exists |
|---|---|
| `WhipNumbers` | every number of the weapon in one pure table, judged by the budget in a unit test, with the Command's limits |
| `TetherMath` | the speeds, lifts and directions of the pull and the swing, clamped, pure |
| `WhipCommand` | the Command and the marks of the other techniques (bound, charged): who may be commanded, the order, the steering, the end, and the two listeners that make it safe |
| `WhipSkills` | the nine techniques |
| `WhipLogic` | lists the skills, attaches the Command's listeners, adds Coil and Static Charge to a melee blow; registers the self-test (with a dummy that has its mind, because the Command refuses a monster without AI) |

## 10. Config keys, commands, quest hooks, lang, saved data

* **Config keys:** none (the numbers, including the Command's limits, are in `WhipNumbers`).
* **Commands:** none of its own.
* **Quest hooks:** none of its own (the dispatcher fires `hero.skill`).
* **Lang:** `hero.skill.whip.<id>.name` / `.desc` for the nine techniques and `hero.passive.whip.<id>` / `.desc` for the five passives, in the lang fragment `hva.lang.json` (the lead merges it into `en_us.json`). Messages to the wielder are plain strings.
* **Saved data:** on a monster `nusmp_wh_cmd_by` (the wielder's id), `nusmp_wh_cmd_until` (the tick the order ends); on any foe `nusmp_wh_bound` (bound until) and `nusmp_wh_charge` (charged until). All are tick stamps that expire by the clock.
* **Event bus:** two listeners on `NeoForge.EVENT_BUS` (`LivingChangeTargetEvent`, `LivingIncomingDamageEvent`), added once per game from the logic's constructor.

## 11. Tests, the self-test, judgement calls

Pure JUnit 5 tests, all passing in the local shim: `whip/WhipNumbersTest` (8: every blow with a tell has a delay, every row inside the budget, six techniques and one ultimate, the five
passives, Hourglass Flow never takes a movement technique under its floor, the budget can fail on the Whip, the Command stays inside the owner's limits, the lightning loses
strength at every jump and never gains from the charge above the cap), `whip/TetherMathTest` (4: the pull grows with distance but is clamped, a close foe is not pulled, the swing is clamped and needs a mark far enough, the direction is normalised and a degenerate one gives nothing); shared: `strike/BudgetTest`, `strike/FormsCheckTest`,
`strike/StrikeFormsTest`. The mutation check is described in `weapon_hammer.md`, section 11. The forms file also passes the forms builder's own validator (5 forms, 0 errors, 0 warnings).

**Self-test `hero.weapon.whip`** (registered from `WhipLogic`'s constructor; NOT run): the shipped file through `FormsCheck`, the loaded forms, every skill registered with the right flags
and texts, then a stand-in player bonded to the weapon casts every skill of every form beside a dummy zombie WITH its mind (so that the Command can take it), calls every hook, releases the bond.

Judgement calls:

* **Cooldowns are base cooldowns.** The dispatcher shortens a cooldown by the bond level (to 0.75 at level 100) and floors it at the config's 20 ticks. The 140-tick base of the Tether Swing and the Portal Whip was chosen so that Hourglass Flow (x0.8) still leaves 112, above the utility floor of 100 BEFORE the level scaling; with the scaling at level 100 it is 84. A decision for the lead if the owner wants the floors to hold after the scaling.
* **One control every 12 seconds.** The Bind Whip (8 s), the Reel In (7 s), the Command (20 s) and the Dimension Whip all share the law's lock; the wielder has to choose. This is the owner's rule, not mine, and it is why the Bind Whip has a softer fallback.
* **The Lash cannot be dodged** (hitscan), and the Lightning Whip and Gigantic Star have tells. The Lash is 6 raw with a 3-second cooldown.
* **Dimension Whip's two readings.** The pages disagree (many lashes at the target, or a bind of anything); the technique does both: a heavy lash on every foe in reach and a bind, bounded by the lock and the law.
* **Commanded monsters are exempt from the whip's own techniques** (Lash, Bind, Reel, Lightning, Star, Portal and Dimension skip them) so a technique cannot cancel its own order.
* **Taskmaster is stronger than the Command alone** on purpose: the Storm Whip commands, the Morning Star Whip commands with Strength.

## 12. What is NOT verified (honest list)

Nothing of the weapon has been run in the game. The code was type-checked against API stubs of Minecraft, NeoForge and Tensura (0 errors in my files; about 1,175 stub artefacts in
other files are ignored); the pure logic is covered by the unit tests above; the forms file by the forms builder's validator. Not verified:

* **The Command in the running game**, above all: that `Mob.setTarget` steers a monster whose AI uses target goals (most vanilla hostile mobs do); that a monster whose AI is a Brain (hoglins, piglins and their kin) may ignore `setTarget` and so is held but not steered (the damage listener still protects players and allies, because it does not depend on the AI); that the target-change event and the incoming-damage event fire for every mob and in the order I assume; that clearing the path (`getNavigation().stop()`) is enough to stop an attack that has begun; the `Mob` types of other mods (Tensura's monsters) that may not use these hooks at all. The self-test only checks that the cast does not throw.
* **Everything visual:** `THREAD_STRINGS`, `SEAL_CHAINS`, `LIGHTNING_SPEAR`, the blood arcs used for the lash, `SPACE_RINGGATE`, the divine ray and the stone telegraphs as the Whip uses them, and whether each honours the violet `#9B5CFF` and the gold `#C9A24A`.
* **The self-test** `hero.weapon.whip` (never run), including that a zombie WITH its mind placed for one cast and discarded in the same call never runs its AI.
* **The Tensura effect id** `tensura:movement_interference` (if it does not exist the hold is only the Slowness VII).
* **`Fx.teleportLook`** (the relics' helper) is used as it stands; where exactly it lands the Portal Whip, and whether it ever refuses a legal spot, is not tried.
* **`Player.pick`** (the block the Tether Swing hooks onto): the signature was read from the API dump; the behaviour at 16 blocks through fluids and glass is not tried.
* **Feel and balance:** the numbers are judged on paper by the budget; the speed of the pull and the swing and the size of the Command's effect on a crowd are not tried.
