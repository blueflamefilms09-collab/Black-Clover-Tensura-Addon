# Vassal Projectile (`vassal_projectile`): the thrown blade that comes back

Class SEVEN_STAR, style THROW. In the series the Vassal Projectile is one of the seven Seven Star Weapons of Raphtalia's world; its base form is a semi-transparent knife that
becomes chakrams, kunai, hatchets, boomerangs and short spears, its power-up method is Over Custom (customisation paid for with money) and its holder is Rishia Ivyred (wiki
summaries). The owner calls it "Projectile (throwing weapon)". Logic `heroweapon/weapon/projectile/ProjectileLogic`, forms `data/nusmp/hero/forms/vassal_projectile.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart2`) reads it first.

## 1. Look (for the art builder)

Source of the look: the wiki's "semi-transparent knife" base form, the owner's "throwing weapon" and the seed "a throwing blade with the gem in the middle". Colours and
proportions are DESIGN. Every form of the weapon uses this one look (the forms are different throwing weapons in play, but the hand shows the base knife).

* **Silhouette.** ONE throwing knife of crystal: a leaf-shaped, double-edged blade with a centre ridge, a short oval guard, a cord-wrapped grip and a RING pommel (an open ring a
  finger could go through) that holds the gem. Apparent length 0.50 blocks, class `dagger` of `weapon_classes.json` (0.45 to 0.7). The blade is about 0.60 of the length, the grip
  0.25, the ring 0.15. The blade points up-forward on the icon's diagonal, the ring at the butt.
* **Named parts (7 to 10).** `blade` (a loft with the ridge, slightly tapered to a sharp point), `edge_glint` (a thin lighter strip along both edges), `guard` (small, dark teal
  metal), `grip` (cord wrap in two tones), `pommel_ring` (a torus, outer radius 0.07 of the length, that is 0.035 blocks), `gem` (a round cabochon set into the ring) and `cord` (a short
  loop of three or four small segments hanging from the ring, optional).
* **Semi-transparent look.** The cutout render type cannot blend, so the glass is a painting: the blade skin is pale and desaturated, aqua `#A8EDE6` in the middle, nearly white
  `#E6FFFB` at the edges, with faint darker veins `#6BCFC4` that run along the ridge like a crack in ice; the additive glow skin adds a very faint sheen along the ridge so the blade
  looks lit from inside.
* **Palette.** Blade aqua / white as above; guard and ring dark teal `#24545A` with brighter edges `#4FA3A8`; grip cord `#1C3F44` and `#3C7C80`; gem emerald-teal `#38F0C0` with a core
  `#B8FFEA`.
* **The gem.** A round cabochon (radius about 0.05 of the length) in the pommel ring; strongest spot of the glow skin; the `glow` line colour is `38F0C0`.
* **Glow skin** (additive): the gem (strong), the ridge (very faint), nothing else.
* **In hand.** Held by the grip, the blade up-forward. Sprite (16 x 16, kept as the inventory icon): a diagonal pale-teal knife with a small ring and one emerald dot.
* **Mood.** Light, quick, clean: the lightest of the family.

## 2. Canon and sources

Each fact is tagged [summary] (read in a web search summary of the fandom wiki; the pages themselves were not opened), [memory] or [design].

* [summary] The Vassal Projectile is one of the eight Vassal Weapons of Raphtalia's world. Its base form is a semi-transparent knife that can become chakrams, kunai, hatchets, boomerangs and short spears; its power-up method is Over Custom (enhanced customisation paid for with money; the wiki's currency is "G"). https://shield-hero.fandom.com/wiki/Vassal_Projectile
* [summary] The holder is Rishia Ivyred, the Projectile Hero (the weapon first appeared as a shadow of the Vassal Projectile that came from the Legendary Bow). https://shield-hero.fandom.com/wiki/Rishia_Ivyred and https://shield-hero.fandom.com/wiki/Rishia_Ivyred/Powers_and_Abilities
* [summary] The wiki's skills: Air Strike Throw (the web novel's page calls the first throw Instant Throw), Second Throw, Dritte Throw (throwing a third weapon), Tornado Throw (three weapons circle the target; the web novel names a knife, a hatchet and a short spear), Bolas Blast (the projectile becomes bolas that restrain the target and then burst into flame), Knife Rain (one thrown knife splits into many), Shadow Bind (a dagger thrown into the target's shadow pins it), Projectile 0 (no damage, effective against illegitimate powers; not used here).
* [design] Everything else is this mod's design: the MARK (every blade that hits marks its foe) and Recall Blades that cashes it in, the Returning Edge, the Ricochet Throw and the Thousand Blade Rain are invented to give a thrown-weapon kit a shape (set up, then cash in) that plays well in first person. The forms (Kunai, Chakram, Hatchet, Over Custom Arsenal) are the wiki's weapon types.

## 3. How it plays

A thrower. Every technique is a **hitscan line** (a blade stops at a wall, strikes the first foes on its path, and a thread of light is drawn along it) or a short zone, so there is no entity to track, no projectile that outlives its cast and nothing to clean up. A blade that "returns" is a second line drawn back to the wielder with its own strike, scheduled one step later; the delayed half re-checks that the wielder is still alive, in the same world and still bonded to the weapon.

The system that gives it shape is the **mark**. Every foe a blade (or the knife in a melee blow) hits is marked for 8 s (this is the Marked Prey passive, which every form has; the mark is drawn as a lock sigil), up to 32 marks per wielder. Recall Blades strikes every marked foe in 28 blocks with a returning blade and uses the marks up. Shadow Bind marks too.

The limits of the budget are kept by a `Tally` per cast (`kit/Tally`, tested): at most N different foes and at most M hits on one foe, whatever the geometry does, and a repeat hit on a foe resets its hurt time so that it lands. A blade never passes through stone. The only hard control (Shadow Bind) asks the shared control gate first and lets the balance law cap its length.

State per wielder (`ProjectileState`): the marks only, in memory, dropped on logout, when the weapon is put away and when the server stops.

## 4. Forms

Five forms, no curse series. The weapon has one look (the knife); the forms are different thrown weapons in play, named after the wiki's weapon types.

| form | name | tier | parents | requirements | mastery | stats | skills (slot: skill) | passives |
|---|---|---|---|---|---|---|---|---|
| `base` | Throwing Knife | 0 | - | - | 0 | attack_speed 0.2 | use: instant_throw, sneak: knife_rain, key1: shadow_bind | mark_prey |
| `kunai` | Kunai | 1 | base | iron_ingot x32, string x24 | 120 | attack_speed 0.3, movement_speed 0.02, luck 1 | use: instant_throw, sneak: dritte_throw, key1: shadow_bind, key2: knife_rain, key3: recall | mark_prey, second_throw |
| `chakram` | Chakram | 2 | kunai | gold_ingot x32, copper_ingot x32, tensura:low_magisteel_ingot x8 | 200 | movement_speed 0.03, reach 0.5, luck 1 | use: boomerang, sneak: ricochet, key1: shadow_bind, key2: recall, key3: instant_throw | mark_prey, wide_ricochet |
| `hatchet` | Hatchet | 2 | kunai | #logs x32, flint x32, iron_ingot x16 | 200 | attack_damage 3, max_health 2 | use: instant_throw, sneak: tornado_throw, key1: bolas_blast, key2: shadow_bind, key3: knife_rain | mark_prey, heavy_hatchet |
| `over_custom_arsenal` | Over Custom Arsenal | 3 | chakram, hatchet | emerald x64, gold_block x16, diamond x16, tensura:medium_quality_magic_crystal x8 | 320 | movement_speed 0.06, attack_damage 3, attack_speed 0.4, luck 3 | use: instant_throw, sneak: ricochet, key1: tornado_throw, key2: recall, key3: thousand_blades | mark_prey, second_throw, wide_ricochet, over_custom |

The forms file passes the real forms validator (the job's forms check script); the `tensura:` ids are ones other systems of the mod already use (`low_magisteel_ingot` in the relic tiers, the crystals in the economy drops).

## 5. Techniques

Ten techniques (6 required), one ultimate (only in the top form). Every number lives in `ProjectileNumbers`; the row of each technique already counts the passive that raises it (a second blade, one more ricochet), so the budget holds in the best form of the weapon: ordinary 3 to 12 raw to one foe in all, ultimate at most 32, at most 12 foes, cooldown at least 60 / 100 / 600, cost 1 to 3 percent (ultimate 5 to 8). Over Custom shortens every cooldown by a tenth, never below the floor of its kind.

| technique | id | kind | cooldown | cost | raw damage (base..strongest, hits per foe) | foes | heal |
|---|---|---|---|---|---|---|---|
| Air Strike Throw | `projectile.instant_throw` | ordinary | 60 ticks (3 s) | 1% | 6..6 x2 | 4 | 0% |
| Returning Edge | `projectile.boomerang` | ordinary | 80 ticks (4 s) | 1.5% | 4..4 x2 | 4 | 0% |
| Ricochet Throw | `projectile.ricochet` | ordinary | 100 ticks (5 s) | 2% | 5..5 x1 | 5 | 0% |
| Knife Rain | `projectile.knife_rain` | ordinary | 120 ticks (6 s) | 2% | 3..3 x4 | 7 | 0% |
| Tornado Throw | `projectile.tornado_throw` | ordinary | 150 ticks (7.5 s) | 2% | 4..4 x3 | 1 | 0% |
| Bolas Blast | `projectile.bolas_blast` | ordinary | 180 ticks (9 s) | 2.5% | 8..8 x1 | 6 | 0% |
| Dritte Throw | `projectile.dritte_throw` | ordinary | 140 ticks (7 s) | 2% | 4..4 x3 | 3 | 0% |
| Recall Blades | `projectile.recall` | ordinary | 200 ticks (10 s) | 2% | 5..5 x1 | 6 | 0% |
| Shadow Bind | `projectile.shadow_bind` | utility | 300 ticks (15 s) | 2% | none | 1 | 0% |
| Thousand Blade Rain | `projectile.thousand_blades` | ultimate | 900 ticks (45 s) | 6% | 4..4 x8 | 12 | 0% |

### Air Strike Throw (`projectile.instant_throw`)

* **What.** One blade along the look line (26 blocks, 0.5 wide, stopped by a wall) through up to 2 foes for 6 raw each. Every foe it hits is marked for 8 s. With Second Throw a second blade follows 0.3 s later along the look direction at that moment; one foe can take both (12 raw), and a repeat hit resets the foe's hurt time so that it lands.
* **Numbers.** ordinary; cooldown 60 ticks (3 s); cost 1% of the maximum magicules; raw damage 6..6 x2; foes 4; heal 0%. Given by: base:use, kunai:use, chakram:key3, hatchet:use, over_custom_arsenal:use.
* **Tell.** The throwing swing, the whoosh and a thread of teal light along the path (no wind-up: 6 raw is under the 8 raw above which a wind-up is required).
* **Counter.** Cover or a side-step before the throw; the blade stops at a wall.

### Returning Edge (`projectile.boomerang`)

* **What.** The blade flies out 22 blocks (up to 4 foes on the way, 4 raw each) and 0.5 s later comes back by a curved path through a point 3 blocks to the side of the middle, striking foes on both legs for 4 raw each. A foe is hit at most twice by one throw.
* **Numbers.** ordinary; cooldown 80 ticks (4 s); cost 1.5% of the maximum magicules; raw damage 4..4 x2; foes 4; heal 0%. Given by: chakram:use.
* **Tell.** Two threads of light: the way out, and the way back that bends round.
* **Counter.** The way back is a different line from the way out: step off both.

### Ricochet Throw (`projectile.ricochet`)

* **What.** The blade strikes the foe you aim at (24 blocks) or, with no foe in the line, glances off the wall it meets, and then jumps from foe to foe: up to 3 more (4 with Wide Ricochet), each within 9 blocks (11 with Wide Ricochet) of the last and in line of sight of it; 5 raw each, once per foe.
* **Numbers.** ordinary; cooldown 100 ticks (5 s); cost 2% of the maximum magicules; raw damage 5..5 x1; foes 5; heal 0%. Given by: chakram:sneak, over_custom_arsenal:sneak.
* **Tell.** Threads of light from foe to foe; a chain sound when it glances off a wall.
* **Counter.** Stand more than 9 blocks from the other foes, or behind cover from the last one it struck.

### Knife Rain (`projectile.knife_rain`)

* **What.** One knife becomes 7, fanned over 36 degrees. Refused when no foe is in the fan. The fan is locked when the technique starts; 0.25 s later each knife flies from the wielder's hand along its line (14 blocks, stopped by a wall) and hits the first foe it meets. A foe is struck once for 3 raw per knife that met it, at most 4 knives (12 raw in all); up to 7 foes. A repeat hit never needs the foe's hurt time because it is one combined hit.
* **Numbers.** ordinary; cooldown 120 ticks (6 s); cost 2% of the maximum magicules; raw damage 3..3 x4; foes 7; heal 0%. Given by: base:sneak, kunai:key2, hatchet:key3.
* **Tell.** Three dust lines (the two edges and the middle of the fan) for 0.25 s, then the knives' threads. 12 raw is possible only at point blank, which is why the throw has a tell.
* **Counter.** Leave the fan in the quarter second it shows, or stand outside 14 blocks.

### Tornado Throw (`projectile.tornado_throw`)

* **What.** Aim at a foe (22 blocks). Three blades whirl round it: at once, 0.5 s and 1 s later each strikes for 4 raw (3 hits of the cast at most), and the foe is slowed (Slowness III for 2 s, 4 s with Heavy Hatchet). The zone follows the foe; it ends early if the foe dies.
* **Numbers.** ordinary; cooldown 150 ticks (7.5 s); cost 2% of the maximum magicules; raw damage 4..4 x3; foes 1; heal 0%. Given by: hatchet:sneak, over_custom_arsenal:key1.
* **Tell.** A thread to the foe and a small tornado that follows it for 1.4 s.
* **Counter.** Break the line of sight before it is cast; once cast it cannot be dodged, which is why it is only 12 raw in all over a second.

### Bolas Blast (`projectile.bolas_blast`)

* **What.** Bolas wrap the foe you aim at (20 blocks): Slowness III for 2.5 s. After 1.2 s they burst in flame: every foe within 3 blocks (4 with Heavy Hatchet) of the wrapped foe, up to 6, takes 8 raw. The burst is centred on the foe where it stands then.
* **Numbers.** ordinary; cooldown 180 ticks (9 s); cost 2.5% of the maximum magicules; raw damage 8..8 x1; foes 6; heal 0%. Given by: hatchet:key1.
* **Tell.** A dust ring (3 blocks) on the ground where the foe stood and a cocoon of thread that follows the foe, for the whole 1.2 s.
* **Counter.** The foe is slowed but can still walk out of the ring if it reacts at once; allies standing near the wrapped foe leave it.

### Dritte Throw (`projectile.dritte_throw`)

* **What.** Three blades in quick turn (4 ticks apart), each at a different foe in front (within 50 degrees of the look direction, 20 blocks); with fewer foes the picks repeat (the same foe can take 3 blades, 12 raw). A blade is skipped when a wall stands in the way at the moment it flies.
* **Numbers.** ordinary; cooldown 140 ticks (7 s); cost 2% of the maximum magicules; raw damage 4..4 x3; foes 3; heal 0%. Given by: kunai:sneak.
* **Tell.** The swing, then each blade's thread, one after the other.
* **Counter.** Cover; the picks are made when the technique starts.

### Recall Blades (`projectile.recall`)

* **What.** Every marked foe within 28 blocks, nearest first, up to 6, is struck for 5 raw by a blade that flies back to the wielder; each mark that struck is used up. Refused (nothing spent) when no foe is marked or none is within reach.
* **Numbers.** ordinary; cooldown 200 ticks (10 s); cost 2% of the maximum magicules; raw damage 5..5 x1; foes 6; heal 0%. Given by: kunai:key3, chakram:key2, over_custom_arsenal:key2.
* **Tell.** A thread from each marked foe back to the wielder.
* **Counter.** Marks fade after 8 s; staying out of 28 blocks or killing the wielder.

### Shadow Bind (`projectile.shadow_bind`)

* **What.** Pins the foe you aim at (20 blocks) by throwing a blade into its shadow: it asks the shared control gate first (the balance law allows one hard control per wielder every 12 s, otherwise the technique is refused and says how long to wait), then applies Slowness VII (it cannot walk) for 50 ticks as capped by the law for the target (3 s on a player, 5 s on a mob, 1 s on a boss). The foe is marked.
* **Numbers.** utility; cooldown 300 ticks (15 s); cost 2% of the maximum magicules; raw damage none; foes 1; heal 0%. Given by: base:key1, kunai:key1, chakram:key1, hatchet:key2.
* **Tell.** A thread to the shadow and chains that close round the foe for as long as it is pinned.
* **Counter.** Break the line of sight; it can be answered at range; allies can hit the pinned foe.

### Thousand Blade Rain (`projectile.thousand_blades`)

* **What.** The ultimate. Marks a field of radius 5 where you aim (22 blocks, dropped to the ground). After 1.2 s eight volleys fall 4 ticks apart; each strikes every foe inside (up to 12) for 4 raw, 32 in all to one foe at the most; every foe struck is marked.
* **Numbers.** ultimate; cooldown 900 ticks (45 s); cost 6% of the maximum magicules; raw damage 4..4 x8; foes 12; heal 0%. Given by: over_custom_arsenal:key3.
* **Tell.** A dust ring, a 5 block column of dust and a seal circle for 1.2 s before the first volley, then streaks of light falling into the field.
* **Counter.** Leave the field in the 1.2 s warning and stay out for the 1.4 s the volleys last.

## 6. Passives

| passive | id | effect | forms that have it |
|---|---|---|---|
| Marked Prey | `mark_prey` | Every foe your blades or your knife hit is marked for 8 s; Recall Blades strikes the marked. | base, kunai, chakram, hatchet, over_custom_arsenal |
| Second Throw | `second_throw` | Air Strike Throw throws a second blade 0.3 s after the first. | kunai, over_custom_arsenal |
| Wide Ricochet | `wide_ricochet` | Ricochet Throw jumps to one more foe and 2 blocks farther each time. | chakram, over_custom_arsenal |
| Heavy Hatchet | `heavy_hatchet` | Bolas Blast bursts 1 block wider and Tornado Throw slows its foe 2 s longer. | hatchet |
| Over Custom | `over_custom` | Every technique can be used again 10 percent sooner, the customisation paid for with coin. | over_custom_arsenal |

## 7. Engravings (the owner's rule 6: every weapon gets Tensura engravings)

The line for `item/WeaponEngravings.java`:

```java
Map.entry("vassal_projectile", List.of(e("swift", 2), e("severance", 1), e("magic_weapon", 1))),
```

* `swift` 2: the weapon is about speed of throw (Air Strike Throw, Second Throw, Dritte Throw).
* `severance` 1: knives, kunai and chakrams are edged weapons that cut clean.
* `magic_weapon` 1: the base form is a semi-transparent blade conjured through the gem, not forged steel.

## 8. Files

* Logic: `heroweapon/weapon/projectile/`: `ProjectileLogic`, `ProjectileSpells`, `ProjectileNumbers` (no game classes), `ProjectileText` (no game classes), `ProjectileState`.
* Shared kit: `heroweapon/weapon/kit/`, described class by class in `weapon_staff.md` section 8 (`Tally` is this weapon's own idea).
* Forms: `src/main/resources/data/nusmp/hero/forms/vassal_projectile.json`. Lang: the builder's fragment (`hero.skill.projectile.*`, `hero.passive.projectile.*`).
* Tests: `ProjectileNumbersTest` (budget, strongest values, the cooldown rule of Over Custom), `ProjectileFormsTest`, and the kit's `TallyTest` (the per-foe and per-cast limits).

## 9. Tests and the self-test

* JUnit: the table against the budget, each strongest value (a second blade, a wider ricochet), the Over Custom cooldown floor, the tally's two limits, the forms file and the texts.
* Self-test `hero.weapon.projectile`: resolves the forms file and every skill id of every form, binds the weapon to a stand-in player at the world spawn with the weapon in hand, casts all ten techniques once beside a dummy husk (any exception is a failure), checks the live skills against the table and removes everything it made.

## 10. What is unverified (honest list)

Nothing has been run in the game. Type-checked against API stubs: yes. Unit-tested: the numbers, the tally, the data. Not verified:

* The feel of hitscan throws with a thread of light (`THREAD_LINE` honours the payload colour; its length and thickness are guesses); the look of `WIND_TORNADO`, `THREAD_COCOON`, `SEAL_CHAINS`, `COMPASS_LOCK`, `FIRE_BURST` in teal.
* That a hit on a foe that was hurt a moment ago lands: the repeat-hit reset of the hurt time is the standard way, but the interaction with other mods' damage handling (armour of the foes, thorns) is untested.
* That the balance law's control cap (`BalanceLaw.controlTicks`) and gate (`beginControl`) behave as their comments say for Shadow Bind.
* That the melee stab of the knife (the `onHit` hook) fires for the item's hits as the contract says.
* The behaviour of the stand-in player used by the self-test.
