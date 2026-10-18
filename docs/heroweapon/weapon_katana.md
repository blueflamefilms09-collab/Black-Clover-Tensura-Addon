# Vassal Katana (`vassal_katana`): the duelist's blade

Class VASSAL, style SWING. In the series the Vassal Katana chose Raphtalia (the Katana Hero) and, per the wiki's list for Glass' World, it is one of the eight Vassal
Weapons there. The owner names no holder for it. Logic `heroweapon/weapon/katana/KatanaLogic`, forms `data/nusmp/hero/forms/vassal_katana.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart3`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text, the wiki's skill and form names (Sakura, Moon, Mist) and the design below. Colours are DESIGN.

* **Silhouette.** A katana: a slender blade with a gentle curve, tip toward the cutting edge (mesh -x, as the repo's orientation rule says for a katana), a clear
  shinogi ridge and a kissaki (angled tip). Class `katana` of `weapon_classes.json` (apparent length 1.4 to 1.8 blocks, the blade about 0.85 of it). The silhouette is
  plain and elegant so that it reads as the same weapon as the other katanas of the mod, but finer.
* **Named parts (9 to 13).** `blade` (loft with the ridge), `hamon` (a thin wavy strip along the edge, lighter than the blade; may be texture only), `habaki` (gold collar
  at the blade root), `tsuba` (a round iron guard, 0.16 block across, with FIVE petal-shaped cut-outs around the centre like a cherry blossom), `seppa` (two thin gold
  washers), `tsuka` (the grip, 0.28 block, an oval section, diamond-wrapped in indigo cord over white ray-skin), `menuki` (two small gold ornaments under the wrap),
  `fuchi` (a gold collar at the grip's top), `kashira` (the pommel cap), `gem`.
* **Palette.** blade steel `#D6DEE8` with a lighter hamon `#F4F7FB`, tsuba dark iron `#2B2B33` with gold inlay `#C9A24A`, wrap indigo `#233A6B`, ray-skin white `#E9E6DD`,
  gem sakura pink `#FF86B3`.
* **The gem.** A pink cabochon (radius about 0.04 block) set in the CENTRE of the tsuba, on the wielder's side (visible in first person), ringed by the five petal
  cut-outs; a second tiny gold dot in the kashira is optional.
* **Glow skin** (additive): the gem (strong), the five petal cut-outs (soft pink), a hair-thin pink line on the hamon. Nothing else.
* **In hand.** The grip at the mesh origin, the tsuba just above it, the blade pointing up-forward on the icon's diagonal (like the existing katanas of the mod).
  Sprite (16 x 16, kept as the inventory icon): a thin steel diagonal with an indigo grip and a pink dot at the guard.
* **Mood.** Precise, quiet, a little cold; the pink is the only warm note.

## 2. Canon facts and where they come from

Only search summaries of the fandom wiki could be read (the pages themselves cannot be opened from the build machine). "Basis" says which part of a claim is a
SUMMARY (read), MEMORY (what I remember of the series, not confirmed) or DESIGN (mine).

| fact | page | basis |
|---|---|---|
| The Vassal Katana is one of the eight Vassal Weapons listed for Glass' World; it chose Raphtalia (the Katana Hero), and so is most active in her world. | https://shield-hero.fandom.com/wiki/Vassal_Katana , https://shield-hero.fandom.com/wiki/Katana_Hero , https://shield-hero.fandom.com/wiki/Vassal_Weapons | summary |
| Power-up method: Refinement: a weapon form is powered up with ores, each success adds a plus, a failure resets it. | https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods | summary |
| Offensive skills: Crescent Moon Sword, Powder Snow, Instant Blade: Mist, Brave Blade: Mist, Misty Moon, Spirit Blade: Soul Slice (depletes enemy SP, hurts non-corporeal enemies), Thrusting Technique: Lethal Formations One and Two, Wind Blade: Vacuum, Triple Thrust, Stardust Blade, Ki Blade: Guard Breaker, the Kagura Dance of Sakura formations, Moon Katana: Crescent, Sakura: Powder Snow, Swallow Fall, Katana 0, Wind Wing Cutter. Utility: Double Sword, Scroll of Return, Poison Sensing, Return Dragon Vein. | https://shield-hero.fandom.com/wiki/Vassal_Katana , https://shield-hero.fandom.com/wiki/Raphtalia/Powers_and_Abilities | summary |
| Forms: Beginner's Katana (Crescent Moon Sword, Powder Snow, Instant Blade: Mist), the Sakura Stone of Destiny Katana (made against Q'ten Lo's Sakura Stone weapons; Kagura Dance of Sakura), White Tiger Katana (Double Sword), Siderite Katana (Stardust Blade), Soul Eater Katana (Spirit Blade: Soul Slice), Shichishito (Release Evade), Katana 0 (the poison of Fitoria's sanctuary). | the Vassal Katana page | summary |
| The shape (a slender katana, a round guard with sakura petals) and the colours. | - | DESIGN |
| NOT built: Powder Snow, Brave Blade, Misty Moon, the Guard Breaker, Wind Blade, Swallow Fall, the utility skills, Katana 0, the combination skills. The Shichishito's Release Evade is given to three forms instead of its own form. | - | DESIGN |
| The tempo chain, the Keen Opening and the Riposte. | - | DESIGN |

## 3. How it plays

The Katana is the duelist's weapon and its rhythm is **tempo**. Using DIFFERENT katana techniques one after another, each within four seconds of the last, builds tempo (0 to 3); each
level adds damage to the cuts and thrusts and to the ultimate, a parry jumps it to full, and a pause longer than four seconds lets it lapse. **Crescent Moon Sword** (right click) is the
long thin cut; **Instant Blade: Mist** is the iaido draw, a breath and then a dash through the line; the **Counter Stance** is a 0.7 second parry that answers the blow it stops (and it stops at
most 60 percent of the wielder's health); **Release Evade** is a step away that stops the next blow without an answer. The White Tiger line adds the **Double Sword** (a phantom second
blade for the next four hits) and the **Triple Thrust**; the Soul Eater Katana the **Soul Slice** (a cut that drains magicules) and the Riposte; the Siderite Katana the **Stardust Blade**
(a five-cut flurry, which the Soul Eater and Sakura Stone Katanas carry too); and the Sakura Stone Katana, which needs both the Siderite and the Soul Eater, the ultimate **Kagura Dance of Sakura**. The **Keen Opening** rewards a quiet
four seconds with a free strike on the first full swing.

## 4. The forms

| form | tier | parents | requires (absorbed) | mastery | stats while active | skills (slot: technique) | passives |
|---|---|---|---|---|---|---|---|
| **Beginner's Katana** (`base`) | 0 | - | nothing | - | attack_damage 7, attack_speed -1.8 | use: Crescent Moon Sword; sneak: Instant Blade: Mist; key1: Counter Stance; key2: Release Evade | `iai_tempo`, `keen_opening` |
| **White Tiger Katana** (`white_tiger_katana`) | 1 | `base` | 32 x iron_ingot; 24 x quartz; 16 x leather | 100 | attack_damage 7, attack_speed -1.2, movement_speed 0.04 | use: Crescent Moon Sword; sneak: Instant Blade: Mist; key1: Double Sword; key2: Release Evade; key3: Triple Thrust | `iai_tempo`, `keen_opening` |
| **Soul Eater Katana** (`soul_eater_katana`) | 1 | `base` | 32 x soul_sand; 32 x bone; 24 x iron_ingot | 100 | attack_damage 8, attack_speed -1.8, armor_toughness 1, max_health 2 | use: Crescent Moon Sword; sneak: Counter Stance; key1: Soul Slice; key2: Release Evade; key3: Stardust Blade | `iai_tempo`, `riposte` |
| **Siderite Katana** (`siderite_katana`) | 2 | `white_tiger_katana` | 12 x diamond; 24 x amethyst_shard; 16 x iron_block; 8 x tensura:low_magisteel_ingot | 200 | attack_damage 10, attack_speed -1.4, movement_speed 0.04, reach 0.5 | use: Crescent Moon Sword; sneak: Instant Blade: Mist; key1: Stardust Blade; key2: Triple Thrust; key3: Double Sword | `iai_tempo`, `keen_opening` |
| **Sakura Stone of Destiny Katana** (`sakura_stone_katana`) | 3 | `siderite_katana`, `soul_eater_katana` | 64 x cherry_leaves; 32 x pink_petals; 48 x cherry_log; 8 x tensura:mithril_ingot | 320 | attack_damage 12, attack_speed -1.2, movement_speed 0.06, luck 1, max_health 4 | use: Crescent Moon Sword; sneak: Instant Blade: Mist; key1: Counter Stance; key2: Stardust Blade; key3: Kagura Dance of Sakura | `iai_tempo`, `keen_opening`, `riposte` |

The tree is a diamond: the Beginner's Katana splits into the White Tiger Katana (toward the Siderite Katana) and the Soul Eater Katana, and the Sakura Stone Katana needs BOTH ends. Tier 1
asks for common things (iron, quartz, leather; soul sand, bone, iron), tier 2 for rarer (diamonds, amethyst, iron blocks and Tensura low-magisteel), the top form for a real task (a
cherry grove's worth of leaves, petals and logs and eight Tensura mithril ingots). The gem takes only items some form of the Katana asks for; absorbed items accumulate and are never
consumed by learning a form. Every stat is inside the budget of design.md section 6 and every ability bonus inside a quarter of it (`KatanaFormsTest` checks both).

## 5. The techniques

| technique (`id`) | kind | rest | cost | raw damage | foes | works for |
|---|---|---|---|---|---|---|
| **Crescent Moon Sword** (`katana.crescent_moon`) | ordinary | 3 s (60 ticks) | 1 % | 8 | 5 | instant |
| **Instant Blade: Mist** (`katana.instant_blade`) | ordinary | 5 s (100 ticks) | 2 % | 10.5 | 6 | 0.25 s |
| **Counter Stance** (`katana.counter_stance`) | utility | 6 s (120 ticks) | 1.5 % | 12 | 1 | 0.7 s |
| **Release Evade** (`katana.release_evade`) | utility | 5 s (100 ticks) | 1 % | none | - | 0.4 s |
| **Triple Thrust** (`katana.triple_thrust`) | heavy | 7 s (140 ticks) | 2.5 % | 5.75 x 3 = 17.25 | 5 | 0.9 s |
| **Stardust Blade** (`katana.stardust_blade`) | ordinary | 6 s (120 ticks) | 2 % | 2.4 x 5 = 12 | 1 | 0.75 s |
| **Double Sword** (`katana.double_sword`) | utility | 10 s (200 ticks) | 2 % | 2.5 x 4 = 10 | 1 | 6 s |
| **Soul Slice** (`katana.soul_slice`) | ordinary | 7 s (140 ticks) | 2 % | 8 | 1 | instant |
| **Kagura Dance of Sakura** (`katana.sakura_dance`) | ultimate | 35 s (700 ticks) | 6 % | 10 x 3 = 30 | 12 | 1.5 s |

Rests are before the level scaling (the dispatcher shortens them by up to a quarter); damage is raw, before the level factor (1.0 to 2.2) and the balance law, and the table shows the
amount at FULL tempo; a technique that finds nothing to act on refuses and costs nothing.

### Crescent Moon Sword (`katana.crescent_moon`)

A long thin slash: a crescent of light cuts up to 5 foes in a line 9 blocks long for 6.5 damage, a little more with tempo. Counter: step out of the line.

* **Numbers:** ordinary technique, rests 3 s, costs 1 % of the maximum magicules, raw damage 8 per foe at most.
* **Tell:** None needed (6.5 raw, 8 at full tempo, never over the 8 that asks for one): the cut is the effect.
* **Answer:** Step out of the line (0.9 blocks to each side) or put a wall in the way.
* **Effects:** `SLASH_WAVE` (a pale pink crescent along the line), the sweep sound, an arm swing.
* **Why:** Canon Crescent Moon Sword (the Beginner's Katana's first skill) and the brief's 'long thin slashes'. It needs a foe in the line, and it is the technique the tempo chain starts from.

### Instant Blade: Mist (`katana.instant_blade`)

Iaido. After a breath (a glint at your hip) you dash up to 7 blocks and cut everything on the line for 9 damage, a little more with tempo. Counter: leave the line during the breath.

* **Numbers:** ordinary technique, rests 5 s, costs 2 % of the maximum magicules, raw damage 10.5 per foe at most.
* **Tell:** The sword is sheathed: a glint at the hip and the click of the guard for 5 ticks before the cut.
* **Answer:** Leave the line during the breath; the dash goes where the wielder looks when the breath ends, so a foe that steps aside is missed.
* **Effects:** `STAR_MAGIC` (the glint), then `DARK_IAI` (one bright cut line with an afterimage), the sweep sound at a high pitch.
* **Why:** Canon Instant Blade: Mist and the brief's iaido 'draw-cut'. The contract gives SWING weapons no hold-and-release, so the draw is a short fixed breath with a tell, not a held charge (listed under what is unverified / possible follow-up).

### Counter Stance (`katana.counter_stance`)

Raises a parry for 0.7 seconds: the first blow from a living foe is stopped (at most 60 percent of your health) and answered with a cut for 4 to 12 damage, and your tempo jumps to full. Counter: it stops one blow, so wait it out.

* **Numbers:** utility technique, rests 6 s, costs 1.5 % of the maximum magicules, raw damage 12 per foe at most.
* **Tell:** A glint at the chest while the stance is up (0.7 s) and a clink when it opens.
* **Answer:** A parry stops ONE blow from a living foe (a melee blow, an arrow, a spell with a caster); wait it out or hit with two things at once; a blow over 60 percent of the wielder's health only partly stops.
* **Effects:** `STAR_MAGIC` glint, a chain sound; on a parry `STAR_MAGIC` to the foe, `WIND_RING`, a shield-block sound and 'Parried. Tempo full.' on the action bar.
* **Why:** The brief's 'counter stances (a parry window that returns damage)': the answer cut is 4 plus half the blow (4 to 12 raw, capped by the ordinary limit), and the tempo jumps to full. It is handled in `onDamaged`, and the stance closes before the answer so it cannot loop.

### Release Evade (`katana.release_evade`)

A flick of the sword and a step away from the nearest foe: for 0.4 seconds the next blow from a living foe misses you, and your tempo rises. Counter: attacks that arrive after the step.

* **Numbers:** utility technique, rests 5 s, costs 1 % of the maximum magicules, no damage.
* **Tell:** A ring of wind and a glint at the start of the step.
* **Answer:** Attacks that arrive after the 0.4 second window, area effects without a living source (fire, falls, explosions without an owner).
* **Effects:** `WIND_RING`, `STAR_MAGIC` along the step, a phantom-flap sound.
* **Why:** Canon Release Evade (the Shichishito's skill): a step away from the nearest foe (else backwards) whose window stops the next blow, like a parry that does not answer; both share the 60 percent cap, so the wielder is never immune.

### Triple Thrust (`katana.triple_thrust`)

After a short draw you step forward and thrust 3 times along a line 4.8 blocks long, 5 damage each, a little more with tempo. Counter: sidestep during the draw.

* **Numbers:** heavy technique, rests 7 s, costs 2.5 % of the maximum magicules, raw damage 17.25 per foe at most.
* **Tell:** The blade is drawn back and a glint gathers on its tip for 6 ticks (0.3 s).
* **Answer:** Sidestep during the draw; the three thrusts follow the wielder's look every 4 ticks, each a line of 4.8 blocks.
* **Effects:** `STAR_MAGIC` (the draw), then three `LIGHT_BLADE` lines, rising sounds, an arm swing each; the wielder steps forward a quarter block a thrust.
* **Why:** Canon Thrusting Technique (Triple Thrust). It needs a foe within 6.8 blocks in front; 5 raw each and 0.25 more a thrust for each level of tempo (17.25 at most).

### Stardust Blade (`katana.stardust_blade`)

5 quick cuts in a flurry on the foe on your crosshair within 8 blocks, 2.4 damage each (12 in all), with stardust where the blade lands. Counter: get out of reach.

* **Numbers:** ordinary technique, rests 6 s, costs 2 % of the maximum magicules, raw damage 12 per foe at most.
* **Tell:** None needed (2.4 raw a cut): the flurry of glints on the foe.
* **Answer:** Get out of reach (11 blocks at most while the flurry runs); it stops if the foe dies.
* **Effects:** `STAR_MAGIC` at a random point of the foe's body for each of the five cuts, a rising chime, an arm swing each.
* **Why:** Canon Stardust Blade (the Siderite Katana's skill): a flurry on the foe under the crosshair, 12 raw in all.

### Double Sword (`katana.double_sword`)

A phantom second blade follows yours for 6 seconds: your next 4 melee hits are each doubled by a phantom cut for 2.5 damage.

* **Numbers:** utility technique, rests 10 s, costs 2 % of the maximum magicules, raw damage 10 per foe at most.
* **Tell:** The pale spirit spirals round the wielder (`SPIRIT_AURA`) for the six seconds.
* **Answer:** -
* **Effects:** `SPIRIT_AURA` following the wielder, a spell sound, 'A phantom blade follows yours.' on the action bar.
* **Why:** Canon Double Sword (the White Tiger Katana's skill): for 6 seconds the next 4 melee hits each draw a phantom cut of 2.5 raw (never two within 6 ticks), so it is bounded by its cuts, not by time.

### Soul Slice (`katana.soul_slice`)

A cut that goes through the body to the spirit: 8 damage and 4 percent of the foe's magicules drained; you take back 50 percent of it, at most 1.5 percent of your own. Foes without magicules lose nothing.

* **Numbers:** ordinary technique, rests 7 s, costs 2 % of the maximum magicules, raw damage 8 per foe at most.
* **Tell:** None needed (8 raw); the pink motes drawn into the wielder show what was taken.
* **Answer:** None; a foe without magicules loses nothing and the wielder gains nothing.
* **Effects:** `KOTO_SHATTER` (motes dissolving from the foe toward the wielder), a vex sound, an arm swing.
* **Why:** Canon Spirit Blade: Soul Slice (depletes the enemy's SP). It drains 4 percent of the foe's magicules through `EnergyBridge.drain` and gives the wielder back half of that, at most 1.5 percent of THEIR OWN maximum, so it is a discount on its 2 percent cost, never a mana engine.

### Kagura Dance of Sakura (`katana.sakura_dance`)

Ultimate. Three formations in a row (blossom, beginning blossom and a final line), together 18 damage plus 4 for every level of tempo you hold (up to 30 in all); the tempo is spent. Counter: leave the circle, then the cone, then the line.

* **Numbers:** ultimate technique, rests 35 s, costs 6 % of the maximum magicules, raw damage 30 per foe at most.
* **Tell:** A pink dust ring of 4.8 blocks and a pink ground seal of petals for the 1.5 seconds the three formations take; the action bar names the tempo.
* **Answer:** Leave the circle (10 ticks in), then the cone (20), then the line (30).
* **Effects:** `CHERRY_BLOSSOM_FX2` (the seal), a `CHERRY_BLOSSOM_FX3` burst at each formation, `DARK_IAI` for the final line, three sounds.
* **Why:** Canon Kagura Dance of Sakura (First Formation: Blossom, Second Formation: Beginning Blossom) as the Katana's ultimate. It spends the tempo: 18 raw and 4 for each level, a third in each formation (30 at most).

## 6. Passives

| passive | what it does | forms |
|---|---|---|
| **Iaido Tempo** (`iai_tempo`) | Using different katana techniques one after another, each within 4 seconds of the last, builds tempo (up to 3): every level adds damage to the cuts and thrusts and the dance of sakura grows with it. | all five |
| **Keen Opening** (`keen_opening`) | The first full swing after 4 seconds without a katana action strikes again for 3 damage, as a draw cut should. | Beginner's, White Tiger, Siderite and Sakura Stone Katana |
| **Riposte** (`riposte`) | After a successful parry the next full swing within 3 seconds is followed by a riposte for 3 damage. | Soul Eater and Sakura Stone Katana |

## 7. Curse series

None. The brief gives the Katana no series, and the seven sins are taken: the Holy Weapons have the canon ones, the Mirror (Envy) and the Chain (Sloth) the other two
(design.md section 2). The forms file therefore has no `curse` form and only the base look.

## 8. Engravings (for `item/WeaponEngravings`, lead integration)

```java
Map.entry("vassal_katana", List.of(e("severance", 2), e("swift", 2), e("barrier_piercing", 1))),   // the duelist's katana
```

* `severance` 2: a katana cuts clean, and its techniques are lines.
* `swift` 2: iaido is speed; the draw, the step and the thrusts.
* `barrier_piercing` 1: the series' Guard Breaker and a duelist's thrust through a guard.

## 9. Numbers and the budget

`KatanaNumbers` holds every number once, `TempoChain` the tempo, `ParryRules` the parry and `KatanaSession` the short windows. The table is checked by `KatanaNumbersTest` against `Budget`
(design.md section 5). The edges the tests pin: the plain cut at full tempo is 8 (so it needs no tell), the draw 10.5, a thrust 5.75 (17.25 for three), the dance of sakura at full tempo
exactly 30 (three formations of 10), the parry stops at most 60 percent of the wielder's health and its answer is between 4 and 12, the phantom blade gives exactly four cuts and never two
within 6 ticks, tempo grows only on a different technique and lapses after 80 ticks. The texts (`KatanaText`) are built from the same constants.

## 10. Code map (the WHY of each file)

| file | why |
|---|---|
| `weapon/katana/KatanaLogic` | the part the shared machinery calls: lists the skills, the parry and the evade in `onDamaged`, the riders to a real melee hit (phantom cut, Keen Opening, Riposte), the self-test `hero.weapon.katana` (with a parry round), forgetting a wielder |
| `weapon/katana/KatanaSkills` | the nine techniques, one small method each |
| `weapon/katana/KatanaNumbers` | every number, the budget rows, the pure tempo-damage rules |
| `weapon/katana/TempoChain` | the pure tempo counter |
| `weapon/katana/ParryRules` | the pure parry arithmetic |
| `weapon/katana/KatanaSession` | the pure short windows and counters of one wielder |
| `weapon/katana/KatanaText` | every name and description, built from the numbers; writes the lang entries |
| `weapon/katana/KatanaState` | the per-wielder memory (tempo, the parry and evade windows, the phantom blade), bounded, cleared on logout, when the katana is put away and on server stop |
| `data/nusmp/hero/forms/vassal_katana.json` | the five forms |
| `weapon/glassworld/*` | the kit shared with the Fan, the Scythe and the Book (see `docs/heroweapon/weapon_glassworld.md`) |

Tests: `KatanaNumbersTest`, `TempoChainTest`, `ParryRulesTest`, `KatanaSessionTest`, `KatanaTextTest`, `KatanaFormsTest` (and the kit's tests). Self-test in the game: `hero.weapon.katana` (with a parry round), run with `/multiverse mega selftest hero.weapon.katana` (permission 2).
Config keys: none. Commands: none of its own. Language: `hero.skill.<id>.name|desc` for the nine skills and `hero.passive.katana.<id>` (+ `.desc`) for the three passives, in `frag/hvc.lang.json`.

## 11. What is unverified

* **Nothing here was run in a game.** The type checker (javac against the API stubs) and the pure unit tests are all that ran; every statement about how a technique
  feels (distances of a dash, the look of an effect, the timing of a tell) is a calculation or a reading of the effect layers' comments, not something seen.
* **The dispatcher and the hooks are the core builder's.** The skills assume what the contract says: a refused cast (returns false) costs nothing and starts no rest,
  `onHit` follows a melee blow of the wielder, `onDamaged` runs before the wielder takes the damage and its return value replaces the amount, `onTick` runs about every
  five ticks while the weapon is summoned. The kit's guard `GlassKit.striking()` keeps a weapon from answering its own techniques' damage if the core's `onHit` also fires
  for them.
* **Effects.** Shapes, colours, durations and sizes were chosen from the javadoc of the effect layers (`vfx/client/layer/*`); the layers were not run. A shape whose
  `power` means something else in play than its comment says would look off, not break.
* **Forms.** Every item id was checked against the vanilla item list and the Tensura id reference, and the three Tensura ids used are the ones other code of the repo
  already uses; the forms system's own self-test `hero.forms` resolves them against the real registries in the game. The counts are a first balance, not tuned.
* **Stats.** `attack_damage` and `attack_speed` are read as the forms builder reads them (modifiers on the item); if it reads them as absolute values the weapons
  would hit far too softly or fast, and the numbers here would need one pass.
* **Damage immunity.** Every technique strike first cuts the foe's vanilla damage immunity (`invulnerableTime`) to ten ticks (`GlassKit.strike`, rule `Immunity`, tested), so a blow lands in full instead of being swallowed by the hit before it, as the older weapons of this mod do. The hits of a quick series (the Triple Thrust, four ticks apart, and the Stardust Blade, three ticks apart) are marked `rapid`: on a PLAYER they keep the vanilla immunity, so a flurry cannot stack past the balance law's 40 percent cap on players, and such a series lands only some of its hits there (as a vanilla sword swung that fast would). Whether another mod changes the immunity of its entities was not checked.
* **No hold-and-release.** The brief asks for draw cuts 'hold then release'. The contract delivers `onRelease` only to the DRAW, THROW and CAST styles, so a SWING weapon cannot
  see a held key; the draw is a fixed 5 tick breath with a tell. A real held charge would need the core to deliver a hold to a SWING weapon's skill.
* **The parry window** is 14 ticks of game time; with network lag a human may find it tight or loose, and the numbers are the first guess. Whether `onDamaged` sees damage from every
  source with a living owner (arrows, spells) is the core's wiring, not tested here.
* **Soul Slice** gives magicules back through the Tensura storage (`TensuraStorages.getExistenceFrom`), the same calls the core's `HeroCtx.pay` uses; it is capped at 1.5 percent of the wielder's pool.
* **Canon:** the colours and shape of the weapon, and which of the series' skills each form owns, are from summaries and memory only.
