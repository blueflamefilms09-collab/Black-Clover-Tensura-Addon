# Vassal Scythe (`vassal_scythe`): L'Arc Berg's scythe

Class VASSAL, style SWING, canon holder L'Arc Berg Sickle (the Scythe Hero). Logic `heroweapon/weapon/scythe/ScytheLogic`, forms `data/nusmp/hero/forms/vassal_scythe.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart3`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text ("Vassal Scythe (L'Arc Berg)"), the series' scythe hero as remembered (a large scythe, a long-hafted reaper) and the design
below. Colours are MEMORY or DESIGN; no picture could be read.

* **Silhouette.** A tall scythe: a straight dark shaft with a long, sweeping crescent blade at the top. The blade starts at the shaft's top, runs FORWARD (mesh -x)
  and curves down to a sharp tip, the cutting edge on the concave (inner) side. The class is `scythe` of `weapon_classes.json` (apparent length 2.0 to 2.6 blocks,
  head at least 0.6 block). The blade is long and slim (about 0.9 block along its curve, 0.14 block wide at the root, tapering), the spine thick and ridged.
* **Named parts (9 to 13).** `shaft` (a lathe, round, slightly thicker at the grip), `grip_wrap` (the lower third wrapped in crimson leather), `bands` (three silver
  rings), `butt` (a short steel spike cap at the bottom), `collar` (a bronze collar at the top of the shaft where the blade roots, the gem is set in it), `gem`,
  `blade` (the curved loft), `spine` (the thick back ridge of the blade), `edge` (a thin bright strip along the cutting edge), `notch` (a small hook cut near the
  root on the spine side), `finial` (a small curled crescent behind the collar, like a lantern hook).
* **Palette.** shaft black-brown `#241C20`, silver bands `#B9C0CC`, blade steel `#C9D3E0` with the spine darker `#6D7482`, edge line bright `#F2F6FF`, bronze collar
  `#8A6A3B`, crimson wrap `#6E1B2B`, gem violet `#9B4DFF`.
* **The gem.** A cabochon (radius about 0.06 block) set in the bronze collar at the blade's root, facing the wielder's side, violet; the soul of a monster is "attached"
  in the series (Spirit Enchantment), so the gem is the soul.
* **Glow skin** (additive): the gem (strong), a thin violet line along the whole cutting edge, faint soul-wisp spirals on the blade's flat. Nothing on the shaft.
* **In hand.** The hand holds the shaft at about a third of its length from the butt (class grip 0.3); the blade is at the top, pointing up-forward on the icon's
  diagonal with its curve bending toward -x (forward). Sprite (16 x 16, kept as the inventory icon): a dark diagonal shaft with a silver crescent and a violet dot.
* **Mood.** Elegant and a little eerie, a reaper of spirits, but clean: no bones, no gore.

## 2. Canon facts and where they come from

Only search summaries of the fandom wiki could be read (the pages themselves cannot be opened from the build machine). "Basis" says which part of a claim is a
SUMMARY (read), MEMORY (what I remember of the series, not confirmed) or DESIGN (mine).

| fact | page | basis |
|---|---|---|
| The Vassal Scythe is one of the eight Vassal Weapons of Glass' World; its hero is L'Arc Berg Sickle, the Scythe Hero, a close-range fighter. | https://shield-hero.fandom.com/wiki/Vassal_Scythe , https://shield-hero.fandom.com/wiki/Vassal_Weapons | summary |
| Skills: Flying Circle (an energy disc thrown at a target), Full Moon Army (a full circular sweep with the giant scythe), Floating Scythe (a second scythe floating alongside), Spirit Scythe: Protection Break (its damage scales with the target's defence), First Form: Wind Slice and Second Form: Sky Blade (tornadoes that push enemies away), a thrown scythe that makes a tornado where it lands, Deadly Dance (a series of repeated swings), Scythe 0 (0 damage, no cooldown, no SP cost, effective against illegitimate powers). | https://shield-hero.fandom.com/wiki/L%27Arc_Berg_Sickle/Powers_and_Abilities | summary |
| Forms: Beginner's Scythe, Small Scythe (L'Arc's way of carrying it), Demon Dragon Scythe (materials of the slain Demon Dragon Emperor), Demon Elephant Scythe (parts of the Demon Elephant), True Demon Dragon Scythe, Scythe 0 (absorbing the mysterious poison in Fitoria's sanctuary). | the Vassal Scythe page | summary |
| Power-up method: Spirit Enchantment: when a weapon form is unlocked a monster's soul is attached to it, permanently, and it cannot be increased. | https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods | summary |
| Combination skills with Therese's spells (Thunder Rain Flying Circle, Lightning Firework, Burning Disc, Ruby Disc). | the L'Arc page | summary, NOT built |
| The shape (a tall scythe with a long curved blade) and the colours. | - | MEMORY / DESIGN |
| NOT built: Sky Blade (merged into Wind Slice's push), the thrown scythe, the True Demon Dragon Scythe, the combination skills. | - | DESIGN |

## 3. How it plays

The Scythe is the reach weapon: its forms add up to 1.5 blocks of melee reach (the `reach` stat) and its techniques cut wide. **Full Moon Army** (right click) is the full circle;
**Wind Slice** pushes a crowd away; **Flying Circle** throws a disc that comes back; **Soul Reap** is the finisher, a swing-up and then a cone whose damage doubles on weak mobs and
whose kills heal the wielder a little; every kill with the scythe in hand also gathers a **soul** (up to 10, 14 with the Demon Dragon Scythe, fading two minutes after the last).
The Small Scythe adds the **Deadly Dance** and a **Floating Scythe**; the Demon Elephant Scythe the **Protection Break**, which grows with armour; the Demon Dragon Scythe the
ultimate **Soul Harvest**, which spends all the souls; and the Scythe 0 form the one technique of the Scythe that deals no damage, **Scythe 0** (a dispel), and a Null Edge that strips one
blessing from every foe it hits.

## 4. The forms

| form | tier | parents | requires (absorbed) | mastery | stats while active | skills (slot: technique) | passives |
|---|---|---|---|---|---|---|---|
| **Beginner's Scythe** (`base`) | 0 | - | nothing | - | attack_damage 6, attack_speed -2.4, reach 1 | use: Full Moon Army; sneak: Wind Slice; key1: Flying Circle; key2: Soul Reap | `soul_tithe` |
| **Small Scythe** (`small_scythe`) | 1 | `base` | 24 x iron_ingot; 16 x leather; 16 x string | 100 | attack_damage 5, attack_speed -1.4, movement_speed 0.04 | use: Deadly Dance; sneak: Full Moon Army; key1: Wind Slice; key2: Flying Circle; key3: Floating Scythe | `soul_tithe` |
| **Demon Elephant Scythe** (`demon_elephant_scythe`) | 1 | `base` | 32 x bone; 24 x leather; 32 x iron_ingot | 120 | attack_damage 9, attack_speed -3, armor 3, knockback_resistance 0.3, reach 1.5 | use: Full Moon Army; sneak: Protection Break; key1: Soul Reap; key2: Wind Slice | `soul_tithe` |
| **Demon Dragon Scythe** (`demon_dragon_scythe`) | 2 | `demon_elephant_scythe` | 4 x dragon_breath; 24 x blaze_rod; 16 x crying_obsidian; 6 x tensura:mithril_ingot | 240 | attack_damage 11, attack_speed -2.6, armor 2, max_health 4, reach 1.5 | use: Full Moon Army; sneak: Soul Reap; key1: Protection Break; key2: Floating Scythe; key3: Soul Harvest | `soul_tithe`, `dragon_soul` |
| **Scythe 0** (`scythe_zero`) | 2 | `small_scythe` | 24 x fermented_spider_eye; 12 x pufferfish; 16 x poisonous_potato; 6 x tensura:low_magisteel_ingot | 220 | attack_damage 7, attack_speed -1.8, movement_speed 0.04, armor_toughness 1, reach 1 | use: Full Moon Army; sneak: Scythe 0; key1: Soul Reap; key2: Wind Slice; key3: Flying Circle | `soul_tithe`, `null_edge` |

The tree is two branches: the Small Scythe toward Scythe 0 (agility and anti-magic) and the Demon Elephant Scythe toward the Demon Dragon Scythe (weight and the ultimate). Tier 1
asks for common things (iron, leather, string; bone, leather, iron), tier 2 for rarer (dragon's breath, blaze rods, crying obsidian and Tensura mithril for the dragon;
fermented spider eyes, pufferfish, poisonous potatoes and Tensura low-magisteel for Scythe 0). The gem takes only items some form of the Scythe asks for; absorbed items accumulate
and are never consumed by learning a form. Every stat is inside the budget of design.md section 6 and every ability bonus inside a quarter of it (`ScytheFormsTest` checks both).

## 5. The techniques

| technique (`id`) | kind | rest | cost | raw damage | foes | works for |
|---|---|---|---|---|---|---|
| **Full Moon Army** (`scythe.full_moon_army`) | ordinary | 3.5 s (70 ticks) | 1.5 % | 6 | 12 | instant |
| **Flying Circle** (`scythe.flying_circle`) | ordinary | 5 s (100 ticks) | 2 % | 5 x 2 = 10 | 6 | 0.5 s |
| **Wind Slice** (`scythe.wind_slice`) | ordinary | 4.5 s (90 ticks) | 1.5 % | 4 | 8 | instant |
| **Soul Reap** (`scythe.reap`) | heavy | 6 s (120 ticks) | 2.5 % | 20 | 6 | 0.4 s |
| **Protection Break** (`scythe.protection_break`) | heavy | 7 s (140 ticks) | 2.5 % | 17 | 1 | 0.5 s |
| **Deadly Dance** (`scythe.deadly_dance`) | ordinary | 5 s (100 ticks) | 2.5 % | 3 x 4 = 12 | 8 | 0.8 s |
| **Floating Scythe** (`scythe.floating_scythe`) | heavy | 12 s (240 ticks) | 2.5 % | 3 x 6 = 18 | 1 | 6 s |
| **Soul Harvest** (`scythe.soul_harvest`) | ultimate | 35 s (700 ticks) | 6.5 % | 32 | 12 | 1.2 s |
| **Scythe 0** (`scythe.scythe_zero`) | utility | 8 s (160 ticks) | 1 % | none | 8 | instant |

Rests are before the level scaling (the dispatcher shortens them by up to a quarter); damage is raw, before the level factor (1.0 to 2.2) and the balance law; a technique
that finds nothing to act on refuses and costs nothing. The Floating Scythe is 'heavy' in the table because it can deal 18 to one foe over its six seconds, though each hit is 3.

### Full Moon Army (`scythe.full_moon_army`)

A full circle with the long scythe: up to 12 foes within 4.8 blocks take 6 damage and are nudged away. Counter: keep beyond 4.8 blocks.

* **Numbers:** ordinary technique, rests 3.5 s, costs 1.5 % of the maximum magicules, raw damage 6 per foe at most.
* **Tell:** None needed (6 raw): the full circle of the scythe is the effect.
* **Answer:** Keep beyond 4.8 blocks; there is no safe side, so the answer is distance.
* **Effects:** `SLASH_SCYTHE` (a giant reaping arc round the wielder, tinted spirit violet), the sweep sound, an arm swing.
* **Why:** Canon Full Moon Army, the circular sweep, and the 'crowd sweep' of the brief: up to 12 foes.

### Flying Circle (`scythe.flying_circle`)

Throws a spinning disc of energy 16 blocks forward; it cuts up to 6 foes for 5 damage on the way out and returns to cut them for 5 more. Counter: step out of its line.

* **Numbers:** ordinary technique, rests 5 s, costs 2 % of the maximum magicules, raw damage 10 per foe at most.
* **Tell:** The disc leaving the blade; it flies for half a second before it comes back.
* **Answer:** Step out of the line (1.1 blocks to each side) before the disc comes home.
* **Effects:** `SLASH_WAVE` out and `SLASH_WAVE` back (violet then pale), the trident-return sound on the way back.
* **Why:** Canon Flying Circle: an energy disc. It is a hitscan out (so nothing to track) and a second pass 10 ticks later from where it ended to where the wielder now stands.

### Wind Slice (`scythe.wind_slice`)

A whirling cut in front of you: up to 8 foes within 4.8 blocks take 4 damage and are pushed back hard. It makes room when you are crowded.

* **Numbers:** ordinary technique, rests 4.5 s, costs 1.5 % of the maximum magicules, raw damage 4 per foe at most.
* **Tell:** None (4 raw).
* **Answer:** It is a push, so the answer is to be ready for it; heavy foes move less.
* **Effects:** `WIND_TORNADO` (a small one just ahead), `WIND_RING`, the wing-flap sound.
* **Why:** Canon First Form: Wind Slice (tornadoes push enemies away). It makes room when crowded.

### Soul Reap (`scythe.reap`)

After a short swing-up the scythe reaps a cone 6 blocks long for 10 damage; a mob at or under 30 percent health (never a boss or a player) takes double. Each foe it kills heals you 3 percent, at most 12 percent. Counter: leave the cone during the swing-up.

* **Numbers:** heavy technique, rests 6 s, costs 2.5 % of the maximum magicules, raw damage 20 per foe at most.
* **Tell:** A violet dust ring of 3 blocks at the wielder's feet and soul light gathering for 8 ticks (0.4 s) before the swing.
* **Answer:** Leave the cone (6 blocks, 100 degrees) during the swing-up.
* **Effects:** `SOUL_VORTEX` (the swing-up), then `SLASH_SCYTHE` (the cut), `CORPSE_RAISE` where a foe falls (at most three), a soul-escape sound.
* **Why:** The brief's 'weak foes under a threshold are reaped, bounded; heals the wielder by a capped share': 10 raw, doubled to 20 on a MOB (never a player, never a boss) at or under 30 percent health; each foe killed heals the wielder 3 percent of their health through `HeroCtx.heal`, at most 12 percent (never over the quarter limit). It is a doubled hit through the balance law, not an instant kill.

### Protection Break (`scythe.protection_break`)

After a short gathering a spirit-edged chop hits the foe on your crosshair for 9 damage plus 0.6 per point of its armour (up to 8 more). Counter: sidestep it or raise a shield.

* **Numbers:** heavy technique, rests 7 s, costs 2.5 % of the maximum magicules, raw damage 17 per foe at most.
* **Tell:** A violet dust ring under the foe and spirit light gathering on the blade for half a second (10 ticks).
* **Answer:** Sidestep, move out of the 7.5 block range, or raise a shield (the chop is an ordinary hit).
* **Effects:** `STAR_MAGIC` (the light on the blade), then `GRAVITY_IMPACT` (a shock ring and cracks) under the foe, the critical-hit sound.
* **Why:** Canon Spirit Scythe: Protection Break (its damage scales with the target's defence): 9 raw plus 0.6 per armour point, at most 8 more (17, inside the heavy limit).

### Deadly Dance (`scythe.deadly_dance`)

4 swings in a row in front of you, each cutting foes within 4.6 blocks for 3 damage (12 in all). Counter: leave the arc.

* **Numbers:** ordinary technique, rests 5 s, costs 2.5 % of the maximum magicules, raw damage 12 per foe at most.
* **Tell:** None (3 raw a swing): four swings in a row are the tell.
* **Answer:** Leave the 140 degree arc; the swings come every 4 ticks for 16 ticks.
* **Effects:** Four `SLASH_SCYTHE` arcs turning a quarter each time, rising sweep sounds, an arm swing each.
* **Why:** Canon Deadly Dance (a series of repeated swings). It needs a foe in front at the start; it re-reads who is in the arc at every swing.

### Floating Scythe (`scythe.floating_scythe`)

A second scythe floats at your side for 6 seconds and cuts the nearest foe within 5.5 blocks for 3 damage every second. You cannot conjure another while it floats.

* **Numbers:** heavy technique, rests 12 s, costs 2.5 % of the maximum magicules, raw damage 18 per foe at most.
* **Tell:** The spirit spirals round the wielder (`SPIRIT_AURA`) for the six seconds.
* **Answer:** Kill or outrun the wielder; the scythe only strikes the nearest foe within 5.5 blocks of them.
* **Effects:** `SPIRIT_AURA` following the wielder, a small `SLASH_SCYTHE` on each foe it cuts, a vex sound.
* **Why:** Canon Floating Scythe (a second scythe floating alongside). No entity is spawned: it is a counted series of six pulses, one a second, and a second one cannot be conjured while it is out.

### Soul Harvest (`scythe.soul_harvest`)

Ultimate. Souls gather round you for a moment, then a huge reaping circle of 7.5 blocks cuts every foe for 16 damage plus 1 per soul collected, 20 percent more on the weak, and heals you 2 percent per foe reaped. Counter: leave the circle.

* **Numbers:** ultimate technique, rests 35 s, costs 6.5 % of the maximum magicules, raw damage 32 per foe at most.
* **Tell:** A violet dust ring of 7.5 blocks and souls spiralling in for 24 ticks (1.2 s).
* **Answer:** Leave the circle during the gathering.
* **Effects:** `SOUL_VORTEX` (the gathering), then two `SLASH_SCYTHE` circles, `CORPSE_RAISE` where foes fall (at most four), two sounds.
* **Why:** The brief's soul-themed finisher and the Scythe's ultimate: it spends every soul collected (the series' Spirit Enchantment), 16 raw and 1 per soul, 20 percent more on the weak, never more than 32; it heals 2 percent of the wielder's health per foe killed, at most 20 percent.

### Scythe 0 (`scythe.scythe_zero`)

A cut that deals no damage: it severs the beneficial effects (speed, strength, regeneration and the like) from up to 8 foes in front of you. Counter: none, it only takes.

* **Numbers:** utility technique, rests 8 s, costs 1 % of the maximum magicules, no damage.
* **Tell:** The dark violet slash from the eye to each foe it strips.
* **Answer:** None: it only takes; it needs a foe with a beneficial effect to do anything, otherwise it refuses.
* **Effects:** `ANTI_MAGIC_SLASH` (up to four), a grindstone sound, a line on the action bar.
* **Why:** Canon Scythe 0 (0 damage, effective against illegitimate powers): a dispel. It removes every beneficial status effect from up to 8 foes in front of the wielder.

## 6. Passives

| passive | what it does | forms |
|---|---|---|
| **Soul Tithe** (`soul_tithe`) | Every kill with the scythe in hand gathers a soul (up to 10) that fades 120 seconds after the last one; Soul Harvest spends them. Putting the scythe away loses them. | all five |
| **Dragon Soul** (`dragon_soul`) | The scythe can hold up to 14 souls. | Demon Dragon Scythe |
| **Null Edge** (`null_edge`) | A melee hit strips one beneficial effect from the foe; the same foe is not stripped again for 2 seconds. | Scythe 0 |

## 7. Curse series

None. The brief gives the Scythe no series, and the seven sins are taken: the Holy Weapons have the canon ones, the Mirror (Envy) and the Chain (Sloth) the other two
(design.md section 2). The forms file therefore has no `curse` form and only the base look.

## 8. Engravings (for `item/WeaponEngravings`, lead integration)

```java
Map.entry("vassal_scythe", List.of(e("soul_eater", 2), e("severance", 1), e("energy_steal", 1))),   // L'Arc's scythe
```

* `soul_eater` 2: the series' Spirit Enchantment attaches a monster's soul to the weapon; here every kill gathers a soul.
* `severance` 1: a reaper's clean cut.
* `energy_steal` 1: the Soul Reap and the Soul Harvest give the wielder back a small, capped share of their health.

## 9. Numbers and the budget

`ScytheNumbers` holds every number once and `ReapRules` the pure rules. The table is checked by `ScytheNumbersTest` against `Budget` (design.md section 5). The edges the tests
pin: the Soul Reap at its doubled 20 is exactly the heavy limit, the Soul Harvest at full Dragon Soul souls on a weak foe would be 36 and is cut to exactly 32, the Deadly Dance totals
12, the Flying Circle 10, the Floating Scythe 18; the reap bonus belongs to a weak non-player non-boss mob only; no heal passes a quarter of the wielder's health; the souls fade
exactly 2400 ticks after the last gain. The texts (`ScytheText`) are built from the same constants.

## 10. Code map (the WHY of each file)

| file | why |
|---|---|
| `weapon/scythe/ScytheLogic` | the part the shared machinery calls: lists the skills, gathers a soul for a kill, fades the souls, the Null Edge, the self-test `hero.weapon.scythe`, forgetting a wielder |
| `weapon/scythe/ScytheSkills` | the nine techniques, one small method each |
| `weapon/scythe/ScytheNumbers` | every number and the budget rows |
| `weapon/scythe/ReapRules` | the pure rules: who is weak, the reap, the armour break, the harvest, the heals |
| `weapon/scythe/SoulLedger` | the pure counter of souls and their fading |
| `weapon/scythe/ScytheText` | every name and description, built from the numbers; writes the lang entries |
| `weapon/scythe/ScytheState` | the per-wielder memory (souls, the floating scythe, who was just stripped), bounded, cleared on logout, when the scythe is put away and on server stop |
| `data/nusmp/hero/forms/vassal_scythe.json` | the five forms |
| `weapon/glassworld/*` | the kit shared with the Fan, the Katana and the Book (see `docs/heroweapon/weapon_glassworld.md`) |

Tests: `ScytheNumbersTest`, `ReapRulesTest`, `SoulLedgerTest`, `ScytheTextTest`, `ScytheFormsTest` (and the kit's tests). Self-test in the game: `hero.weapon.scythe`, run with `/multiverse mega selftest hero.weapon.scythe` (permission 2). Config keys: none.
Commands: none of its own. Language: `hero.skill.<id>.name|desc` for the nine skills and `hero.passive.scythe.<id>` (+ `.desc`) for the three passives, in `frag/hvc.lang.json`.

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
* **Damage immunity.** Every technique strike first cuts the foe's vanilla damage immunity (`invulnerableTime`) to ten ticks (`GlassKit.strike`, rule `Immunity`, tested), so a blow lands in full instead of being swallowed by the hit before it, as the older weapons of this mod do. The hits of a quick series (the Deadly Dance: four swings four ticks apart) are marked `rapid`: on a PLAYER they keep the vanilla immunity, so a flurry cannot stack past the balance law's 40 percent cap on players, and such a series lands only some of its hits there (as a vanilla sword swung that fast would). Whether another mod changes the immunity of its entities was not checked.
* **Weakness is read from health at the moment of the hit**, so a foe killed by the first of several hits in one swing counts as reaped only for that hit; the heal counts foes
  that were not alive after their hit.
* **Corpse effects.** `CORPSE_RAISE` belongs to the Corpse magic of another part of the mod; its look on a reaped foe (a soul rising) is read from its comment only.
* **Canon:** the colours and shape of the weapon, and which of the series' skills each form owns, are from summaries and memory only.
