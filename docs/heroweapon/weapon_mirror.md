# Vassal Mirror (`vassal_mirror`): the hand mirror of Glass' world, Envy by design

Builder `hvd`. Logic: `heroweapon/weapon/mirror/**`, forms: `data/nusmp/hero/forms/vassal_mirror.json`. Style CAST, class Vassal Weapon.
Written in two passes: the LOOK section (1) first so the art builder could start, then the rest.

## 1. The look (for the art builder `hart4`)

Source: the weapon itself is only described in words in the series' wiki summaries ("magical defence and movement", glass shield formations, a barrier of stardust);
no picture could be read from the build machine, so the shape below is a DESIGN, not a copy of an illustration (state of knowledge: from memory and design).

* **What it is.** An ornate silver hand mirror, held by its handle with the glass facing forward like a small shield. Oval, slightly teardrop-shaped frame, about 0.55 blocks wide
  and 0.80 tall; a short fluted handle of 0.35 blocks below it that ends in a pommel. Whole weapon about 1.3 blocks long with the handle (size class `shield` 0.9 to 1.2 or the
  nearest `item` class: the art builder decides, the rule is real-life size). The glass is the largest flat face and must read from the front as a bright reflective panel.
* **Named parts** (8 to 12): `frame` (the loft of the oval rim, bevelled), `glass` (a thin panel slightly inset), `crest` (a small three-lobed filigree crown on top of the frame),
  `scrolls` (two mirrored scrolls running down both sides of the frame), `beads` (a row of small pearls along the rim, repeated geometry, keep it cheap), `neck` (the throat
  between frame and handle), `handle` (fluted grip), `pommel` (a round cap), `gem` (see below), `chain_loop` (a small ring and short chain hanging from the pommel: it is the
  "mirror seen once, travel to it" motif).
* **The gem.** The series' central jewel sits at the CROWN of the frame (the crest holds it): an aquamarine cabochon, about 0.12 blocks, in a claw setting. It is the brightest
  point of the glow skin.
* **Palette** (painted skin): frame polished silver `#C9D2DC`, engraved scrollwork and shadow lines `#7C8794`, pearls `#EEF3F7`, handle and pommel the same silver with a darker
  tarnish at the grooves `#8D98A5`, glass `#E8F6FF` with a faint blue edge `#BFD9EA` (a flat bright face; add two diagonal sheen bands a shade lighter `#FFFFFF` at low alpha
  on the base skin so it is not blank), gem `#4FD8E8`.
* **Glow skin** (additive): the glass (a soft cold white-cyan `#BFEFFF`, strongest in a diagonal band and at the rim), the gem (`#7FF2FF`, full), the beads (a faint `#DDF6FF`).
  Nothing else glows.
* **Mood.** Cold, clean, noble, still: like a Holy Weapon's plain base look but finer. The curse forms of this weapon (Envy) do NOT have a second mesh (the vassal weapons
  have no curse look); the forms keep the base look and only the HUD changes, so do not model a cursed twin.
* **Icon (16 x 16 sprite).** A silver oval mirror with a bright cyan-white face and a cyan dot at the top, handle running to the lower left (the tip of a held item points
  up-right on the icon diagonal; here the "tip" is the frame).

## 2. Canon: what the series says, and what is design

Source types: **S** = a search summary of the series' fan wiki (the wiki pages themselves could not be opened from the build machine, so every canon claim is
second-hand), **M** = memory, **D** = design (the author's own choice). Nothing here was checked against the light novels or the anime.

| Fact | Source |
|---|---|
| The Vassal Mirror is one of the eight Vassal Weapons of Glass' world. Its abilities are magical defence and movement; its physical defence is lower than the Legendary Shield's. | S: https://shield-hero.fandom.com/wiki/Vassal_Mirror |
| Naofumi holds it now (it flew into him in the fight with Mald). Albert held it before and Kyo killed him. One Web Novel page lists "Kuu" as the Mirror hero (the summary says the sources disagree). | S: .../wiki/Vassal_Mirror, .../wiki/Albert, .../wiki/Heroes_(Web_Novel) |
| Mirror Bash is the mirror version of Shield Bash: it pushes targets back and knocks them out for a moment. | S |
| Formation One/Two/Three: Glass Shield summons a static mirror of energy in the air; when it breaks its fragments hurt the enemy, and it still reflects magic after it breaks. The 2nd and 3rd are the equivalents of Second Shield and Dritte Shield. | S |
| Stardust Mirror puts a barrier round a target (like Shooting Star Shield). | S |
| Chain Binding Mirror links the glass shields already summoned with a chain to restrain the target; it is unlocked by the White Tiger Clone Mirror. | S |
| Release Rebound reflects all magic, buffs and debuffs included; it is unlocked by the Okitsu Kagami: Hetsu Kagami mirror. | S |
| Movement Mirror (also Teleport Mirror) returns the user to any mirror seen before. Transport Mirror joins two mirrors like a doorway and Relay Mirror shows the destination. Mirror Cage and Changing Mirror are named without a description. | S |
| Every Vassal Weapon has a gem that is the functional matrix of its abilities and stays through every transformation; absorbed items are kept (not destroyed); the strengthening method is unique to each weapon. | S: https://shield-hero.fandom.com/wiki/Vassal_Weapons |
| Envy: the wiki's Envy entry is the Lust and Envy Spear IV, whose skill Ressentiment magnifies the jealousy of the targets in an area. Nothing in the summaries ties Envy to the Mirror. | S: https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List |
| The silver hand-mirror look, the aquamarine gem on the crown, panes, gleam and the image, Mirror Bash's cone, the Prism Ray, Kaleidoscope, Covet, every number, and the form names Envious Glass, Green-Eyed Mirror and Mirror of Envy. | D |
| The form names Hetsu Kagami and Yata Mirror. "Okitsu Kagami: Hetsu Kagami" is the wiki's; the Yata Mirror is the mirror of Japanese myth, picked from memory as the name of the top form. | S for Hetsu Kagami, M/D for Yata Mirror |

Canon techniques deliberately NOT built: **Transport Mirror** and **Relay Mirror** (a doorway between two mirrors needs a placed, linked object and a safe
arrival across dimensions; the Movement Mirror's step-and-return covers the travelling part), **Mirror Cage** and **Changing Mirror** (no description to build from).

**Envy is a design assignment, not canon.** The brief and design.md (sections 7 and 18) give the Mirror the Envy Curse Series because canon ties no Holy Weapon to
Envy; the closest canon is the Envy Spear above. The skill name "Ressentiment" is that spear's; the effect (jealousy turned against each other) fits it, but the
technique here is this weapon's own.

## 3. How it plays

The Mirror is a defender that turns what is thrown at it into light and spends the light on offence.

1. **Panes.** A Glass Shield stands a pane of glass in the air ahead for six seconds. A hostile shot that flies through it is sent back at its shooter. Each catch
   leaves **gleam** (light stored in the glass, 0 to 100, fading when it is not fed). When the pane is spent or out of time it bursts into shards that cut the foes
   near it.
2. **Gleam.** Every shot a pane returns, every shard that cuts, every foe the bash strikes and every spell the rebound returns adds gleam. The **Prism Ray** spends
   all of it: seven damage with an empty glass, up to thirteen with a full one. A ray that passes a standing pane bounces off it and strikes a second foe.
3. **The image.** The Movement Mirror steps the wielder up to nine blocks and leaves a silent image where they stood. The image has no body and no entity: it is
   a marker that (a) takes the next blow a creature lands on the wielder, up to 35 percent of their health, (b) is a place to step back to, (c) anchors the
   Chain Binding Mirror together with the panes, and (d) with the Afterimage passive repeats the next Mirror Bash or Prism Ray once at half strength.
4. **Spells.** The Spell Ward passive cuts the magic damage the wielder takes by 15 percent; Release Rebound turns half of every spell for four seconds back on its
   caster (capped).
5. **Envy** (curse forms only): copy a foe's helpful effects, or turn the creatures round you against each other, at the price of corruption.

The weapon never reflects physical blows, never makes the wielder immune to anything, and every figure is capped (see the numbers and the safety list below).

## 4. The forms tree

Items to absorb are listed per form; absorbed items are kept, never consumed (design.md section 6). All forms use the base look (the vassals have no curse mesh).

| form (id) | tier | parents | needs | mastery | stats | use / sneak / key1 / key2 / key3 | passives |
|---|---|---|---|---|---|---|---|
| Hand Mirror (`base`) | 0 | - | nothing (always learned) | 0 | armor 1, luck 0.5 | `mirror.mirror_bash` / - / `mirror.glass_shield` / `mirror.prism_ray` / - | spell_ward |
| White Tiger Clone Mirror (`white_tiger_clone_mirror`) | 1 | base | minecraft:glass x48, minecraft:iron_ingot x24, minecraft:quartz x16 | 80 | armor 2, armor_toughness 1, luck 1 | `mirror.mirror_bash` / `mirror.stardust_mirror` / `mirror.glass_shield` / `mirror.prism_ray` / `mirror.chain_binding` | spell_ward |
| Okitsu Kagami: Hetsu Kagami (`hetsu_kagami`) | 2 | white_tiger_clone_mirror | minecraft:amethyst_shard x48, minecraft:gold_ingot x24, minecraft:ender_pearl x12 | 160 | armor 3, armor_toughness 1.5, knockback_resistance 0.1, movement_speed 0.02, luck 1 | `mirror.mirror_bash` / `mirror.release_rebound` / `mirror.glass_shield` / `mirror.prism_ray` / `mirror.movement_mirror` | spell_ward, afterimage |
| Yata Mirror (`yata_mirror`) | 3 | hetsu_kagami | minecraft:diamond x24, minecraft:amethyst_shard x96, minecraft:echo_shard x8, tensura:high_quality_magic_crystal x6 | 280 | armor 4, armor_toughness 2, max_health 4, knockback_resistance 0.15, movement_speed 0.03, luck 1.5 | `mirror.mirror_bash` / `mirror.movement_mirror` / `mirror.glass_shield` / `mirror.prism_ray` / `mirror.kaleidoscope` | spell_ward, afterimage |
| Envious Glass (curse: envy, +4 corruption/s) (`envious_glass`) | 1 | base | minecraft:emerald x32 | 120 | armor 3, movement_speed 0.04, luck 1 | `mirror.mirror_bash` / `mirror.covet` / `mirror.glass_shield` / `mirror.prism_ray` / `mirror.movement_mirror` | spell_ward, afterimage |
| Green-Eyed Mirror (curse: envy, +7 corruption/s) (`green_eyed_mirror`) | 2 | envious_glass | minecraft:emerald x64, minecraft:spider_eye x32 | 220 | armor 4, armor_toughness 1.5, movement_speed 0.06, luck 1.5 | `mirror.mirror_bash` / `mirror.covet` / `mirror.glass_shield` / `mirror.prism_ray` / `mirror.ressentiment` | spell_ward |
| Mirror of Envy (curse: envy, +10 corruption/s) (`mirror_of_envy`) | 3 | green_eyed_mirror | minecraft:emerald x128, minecraft:fermented_spider_eye x32, tensura:magic_stone x48 | 320 | armor 5, armor_toughness 2.5, movement_speed 0.08, luck 2 | `mirror.mirror_bash` / `mirror.covet` / `mirror.ressentiment` / `mirror.movement_mirror` / `mirror.kaleidoscope` | spell_ward, afterimage |

Notes on the tree: Chain Binding Mirror and Stardust Mirror arrive with the White Tiger Clone Mirror (canon: Chain Binding Mirror is unlocked by it), Release Rebound
and the Movement Mirror with Hetsu Kagami (canon: Release Rebound is unlocked by it), and the Kaleidoscope with the top form. Every ordinary form keeps the Spell Ward.
The curse forms are a second branch from the base form, three forms deep, with somewhat better armor and speed than the ordinary branch of the same tier and a corruption price.

## 5. The techniques

### 5.1 The budget (design.md section 5), straight from `MirrorNumbers`

Raw damage is before the bond-level factor (1.0 to 2.2) and the balance law. "Most on one target" counts every hit of the technique, the Afterimage echo included.

| technique | kind | raw per hit | most on one target | targets | cooldown | cost | heal |
|---|---|---|---|---|---|---|---|
| `mirror.mirror_bash` | ordinary | 6 | 9 | 6 | 60 ticks (3 s) | 1% | - |
| `mirror.glass_shield` | utility | 4 | 4 | 6 | 120 ticks (6 s) | 2% | - |
| `mirror.prism_ray` | heavy | 13 | 19.5 | 6 | 100 ticks (5 s) | 2% | - |
| `mirror.stardust_mirror` | utility | 0 | 0 | 0 | 160 ticks (8 s) | 2% | - |
| `mirror.release_rebound` | heavy | 12 | 20 | 6 | 200 ticks (10 s) | 2% | - |
| `mirror.movement_mirror` | utility | 0 | 0 | 0 | 120 ticks (6 s) | 2% | - |
| `mirror.chain_binding` | utility | 4 | 4 | 4 | 240 ticks (12 s) | 2% | - |
| `mirror.kaleidoscope` | ultimate | 5 | 30 | 8 | 700 ticks (35 s) | 6% | - |
| `mirror.covet` | utility | 0 | 0 | 0 | 160 ticks (8 s) | 2% | - |
| `mirror.ressentiment` | utility | 0 | 0 | 0 | 280 ticks (14 s) | 3% | - |

The test `MirrorNumbersTest` hands exactly these rows to `Budget.violations`; a later edit that breaks the budget fails the build. The skills are BUILT from the
rows (cooldown and cost), so the table and the game cannot differ.

### 5.2 Technique by technique

**`mirror.mirror_bash` Mirror Bash** (ordinary, 3 s, 1 percent)
* What: a cone from the eye, 55 degrees to each side, 3.6 blocks long. Each foe in it (six at most) takes 6, is shoved 0.9 and slowed (Slowness IV for 12 ticks, a
  soft stagger, not hard control), and adds 4 gleam. With the Afterimage and a standing image, the image repeats it 12 ticks later for 3 (half), once.
* Tell: a flash of glass in front of the wielder and a chime. The blow is instant and weak (6 is under the 8 that needs a tell).
* Counter: stay out of 3.6 blocks, or trade blows: it is the weakest hit of the weapon.

**`mirror.glass_shield` Glass Shield** (utility, 6 s, 2 percent)
* What: a pane (a disc of radius 1.7 blocks, 0.5 thick) stands 2.6 blocks ahead of the wielder for 120 ticks, facing the wielder's flat look. Every tick, each hostile
  projectile that crossed the disc since the last tick is turned back at its shooter (a little faster) and adds 12 gleam; the pane holds three charges. When spent or out
  of time it bursts: foes within 3.5 blocks take 4 (six at most) and add 3 gleam each. Two panes stand at most; a third pushes the oldest out (it bursts).
* Tell: the pane is a visible mirror frame for its whole life; the burst is visible and has a glass-breaking sound.
* Counter: do not shoot into a standing pane; walk round it (it is not a wall, melee goes through); do not stand within 3.5 blocks of it when it runs out.
  Spells that are not projectile entities (instant ones) are not stopped by it.

**`mirror.prism_ray` Prism Ray** (heavy, 5 s, 2 percent)
* What: after 7 ticks of charge, a beam 22 blocks long and 0.7 wide along the wielder's look at that moment. It hits up to six foes for 7 plus 6 times the gleam
  over 100 (7 to 13) and spends all the gleam. If the beam passes within a pane's radius it ends there and a second beam goes to the nearest other foe within 14 blocks
  for 0.8 of the damage (a foe already struck is skipped). The Afterimage repeats it from the image at half strength.
* Tell: a bright line along the planned path and a glass flash 0.35 seconds before it fires (the line is drawn from the look at the start of the charge, the beam
  follows the look at the end of it).
* Counter: leave the line or break line of sight. The bounce needs a standing pane, which lasts only six seconds, and a shot fired into a pane comes back at the shooter.

**`mirror.stardust_mirror` Stardust Mirror** (utility, 8 s, 2 percent)
* What: the ally in the crosshair (20 blocks) or, with none, the wielder, plus the allies within 4 blocks of them, three at most: Absorption for 8 seconds (4 health;
  8 for a body with 40 maximum health or more) and the next two hostile shots that come within 2.4 blocks of each are turned back.
* Tell: a mirror array follows each protected ally for the 8 seconds.
* Counter: melee and instant spells are not stopped; the absorption is small and wears off.

**`mirror.release_rebound` Release Rebound** (heavy, 10 s, 2 percent)
* What: for 80 ticks, half of every magic blow a creature lands on the wielder (after the image and the Spell Ward have had their say) is dealt back to its caster as
  indirect magic damage, at most 12 per blow and 20 in all for the cast, and never more than the damage the wielder would have taken. Each reflected blow adds gleam
  (1.5 times the damage, at most 20). On cast, up to two harmful effects are cleansed from the wielder; of those, the plain damaging or weakening ones (poison, wither,
  weakness, hunger, mining fatigue, bad luck; slowness up to level II) go on to the nearest foe within 14 blocks for 6 seconds at level II at most. Hard-control effects
  of other mods are cleansed but never sent on.
* Tell: a mirror array round the wielder for the 4 seconds and a resonant chime.
* Counter: use physical blows or arrows for four seconds, or wait. The reflected damage is half of the spell and capped.

**`mirror.movement_mirror` Movement Mirror** (utility, 6 s, 2 percent)
* What: step up to nine blocks the way you look (horizontal, stopped by walls, landing on the farthest safe spot) and leave the image where you stood for 240 ticks.
  Cast again while the image stands to step back to its place (the nearest safe spot) and dissolve it. The image soaks one creature's blow (35 percent of the wielder's
  maximum health at most, the rest passes) and shatters.
* Tell: step flashes at both ends and an aqua frame at the image.
* Counter: it is movement, not an attack; chase the wielder. The image cannot be struck, it is only a marker.

**`mirror.chain_binding` Chain Binding Mirror** (utility, 12 s, 2 percent)
* What: needs a standing pane or the image. Up to four foes within 5 blocks of any of them are bound: the shared control lock, Slowness X for the law's control time (3 s
  base, shortened for players and bosses), 4 damage each, 5 gleam each.
* Tell: chains are drawn between the glass and the victims; the victims must already be near the glass, which is the warning.
* Counter: stay more than 5 blocks from standing glass and from the image; cure slowness.

**`mirror.kaleidoscope` Kaleidoscope** (ultimate, 35 s, 6 percent)
* What: the ground under the aimed point (18 blocks) becomes the centre of a ring of radius 6 with six panes round it. After 14 ticks, six pulses of light 12 ticks apart
  strike every foe inside the ring (eight at most) for 5 each, 30 in all, adding 2 gleam per hit. It stops if the weapon is put away.
* Tell: a ring on the ground and the six panes for 0.7 seconds before the first pulse, and rays between the panes at every pulse.
* Counter: leave the ring. Each pulse is only 5, so a late dodge still saves most of it.

**`mirror.covet` Covet** (Envy, utility, 8 s, 2 percent plus 2 percent paid inside, plus 35 corruption)
* What: the foe in the crosshair (16 blocks) loses nothing; the wielder copies up to three of its strongest helpful effects for up to 200 ticks, at level III at most.
  Refused (and free) with no target, with nothing to copy, or when the extra cost cannot be paid.
* Tell: a green ray from the foe to the wielder and a step flash.
* Counter: do not lean on a buff against an Envy wielder; the copies last ten seconds at most and the wielder pays corruption for every cast.

**`mirror.ressentiment` Ressentiment** (Envy, utility, 14 s, 3 percent, plus 40 corruption)
* What: needs at least two creatures (six at most, never bosses, never players) within 9 blocks of the wielder. Shared control lock. For 4 seconds, every 10 ticks, each of
  them takes its nearest fellow in the group as its target.
* Tell: a green ring flashes round the wielder.
* Counter: players are untouched; it ends after 4 seconds; creatures stop fighting each other if they have no target left.

## 6. The passives

* **`spell_ward`** (every form): while the Mirror is out, magic damage the wielder takes is cut by 15 percent. Applied after the image and before the rebound.
* **`afterimage`** (Hetsu Kagami, Yata Mirror, Envious Glass, Mirror of Envy): the image repeats the next Mirror Bash or Prism Ray once, 12 ticks later (more than
  the ten-tick window in which a creature that was just hurt ignores a smaller blow), at half strength, then dissolves.

## 7. The Envy Curse Series (the design's assignment, not canon)

Three `curse: true` forms with `sin: envy` and the engine's `corruptionPerSecond`: **Envious Glass** (tier 1, 4 per second), **Green-Eyed Mirror** (tier 2, 7 per second)
and **Mirror of Envy** (tier 3, 10 per second). They use the base look. The curse engine (the curse builder's files) owns unlocking (despair and acceptance), the permanent
price, the corruption meter, the Overrun at 1000 and the sin's own downside (design.md section 7: "the wielder copies and loses their own technique at random"); this weapon
adds only what is in its own files:

| | Envious Glass | Green-Eyed Mirror | Mirror of Envy |
|---|---|---|---|
| What it gives | armor 3, speed +4 percent, Covet on sneak | armor 4, toughness 1.5, speed +6 percent, Ressentiment on key 3 | armor 5, toughness 2.5, speed +8 percent, Kaleidoscope on key 3, Covet, Ressentiment, Movement Mirror |
| What it costs | 4 corruption per second; each Covet 35 plus 2 percent extra magicules | 7 per second; each Ressentiment 40 | 10 per second (an Overrun from zero in about 100 seconds if only held); both Envy techniques |

Why Envy: the Mirror is the weapon of copies and reflections, and envy is wanting what the other has. Covet takes what a foe has (its buffs, for ten seconds, capped at level
III), and Ressentiment turns the crowd's jealousy on itself. Both are bounded: three effects, one foe, six creatures, four seconds, no bosses, no players, and Ressentiment sits
behind the shared control lock. The wielder pays in corruption (which ends in an Overrun at 1000) and in magicules, so the series is a loan, not a free power.

**Double counting to avoid:** the two Envy techniques call `HeroCurse.addCorruption` themselves (35 and 40) in addition to the form's per-second rate. If the curse engine also
charges corruption per technique, the lead should keep only one of the two.

## 8. How it differs from what the mod already has

The mod already has **Mirror Magic** (`book/MirrorBook`, `book/MirrorWorks`, Gauche Adlai's Black Clover attribute). It is a grimoire: nine pages, power scaled by existence
points. The Vassal Mirror shares no code and no state with it (only the VFX shapes `MIRROR_*`). The concepts overlap because canon names overlap; what is different:

| Mirror Magic (grimoire) | Vassal Mirror (this weapon) |
|---|---|
| Mirror Array: mirrors orbit you, catch shots within 3 blocks, jam the shooter's magic (silence), shatter instead of a killing blow | Glass Shield: one standing pane at a fixed place, three charges, six seconds, shards on burst; no silence, no protection from a killing blow |
| Real Double: a tangible entity (`MirrorDoubleEntity`) that costs a tenth of your magicules, fights, copies effects, echoes every spell, and swaps places | The image: no entity, free to cast with the step; soaks one blow, repeats one Mirror Bash or Prism Ray once, a marker to step back to |
| Reflect Ray, Large Reflect Ray, Full Reflection: spells sent back | Release Rebound: half of each spell, capped at 12 a blow and 20 in all, plus a cleanse |
| Mirror Step | Movement Mirror (nine blocks and an image to return to) |
| Mirrors Slash, Mirrors Meteorite (existence-scaled) | Kaleidoscope (six pulses, 30 on one target) |
| Everything scales with the caster's existence points | Only the bond level (1.0 to 2.2) through `HeroCtx.hit` |

## 9. Engravings (the owner's rule 6) and why

Exact line for `item/WeaponEngravings.ENGRAVINGS` (ids all from the table's own list):

```java
Map.entry("vassal_mirror", List.of(e("magic_interference", 2), e("magic_protection", 1), e("elemental_boost", 1))), // glass that turns spells aside and stores their light
```

* `magic_interference` 2: the mirror's whole defence is against magic (Spell Ward, Release Rebound), and a reflecting pane disturbs the spell that meets it.
* `magic_protection` 1: canon says the weapon specialises in magical defence.
* `elemental_boost` 1: the Prism Ray and the Kaleidoscope are light, an element, made into a beam.

## 10. Code map: what each file is for

Config keys: none (a weapon keeps its numbers in code, in a pure table with a test). Commands: none. Quest keys: none. Lang: the keys `hero.skill.mirror.<name>.name` and `.desc` for each skill and `hero.passive.mirror.<id>` (and `.desc`) for each passive are in the fragment `hvd.lang.json`, which the lead merges into `en_us.json`.

All under `heroweapon/weapon/mirror/` (every class has a "Why:" Javadoc). Shared helpers are in `heroweapon/weapon/vassalkit/` (see `docs/heroweapon/weapon_vassalkit.md`).

| File | Why it exists | Tests |
|---|---|---|
| `MirrorLogic` | the `HeroLogic`: lists the ten skills from the rows, wires `onDamaged` and `onDismiss`, registers the self-test `hero.weapon.mirror` | self-test, `MirrorFormsTest` |
| `MirrorSkills` | the bodies of the techniques and of the damage hook (image, ward, rebound) | self-test |
| `MirrorNumbers` | every figure and the small rules (ray damage, absorption level, ward, image soak, rebound share, which effects may be sent on) as pure code | `MirrorNumbersTest` (13) |
| `PaneBook` | where the panes are: positions, facing, charges, expiry, eviction | `PaneBookTest` (7) |
| `GleamMeter` | the gleam counter with its cap and its fading from the game clock | `GleamMeterTest` (6) |
| `MirrorLedger` | the wielder's whole memory (panes, gleam, image, rebound) in one pure object | `MirrorLedgerTest` (7) |
| `vassal_mirror.json` | the forms file | `MirrorFormsTest` (3) runs the shared checker on the shipped file |

## 11. Server rules, checked

* Damage: all through `HeroCtx.hit`, **except the rebound**: it is a reaction to a blow, so it deals `min(half the spell, 12, what is left of 20)` as indirect magic damage
  through `LivingEntity.hurt`, capped again by `BalanceLaw.cap(attacker, ...)`. It is never more than the damage the wielder would have taken (the brief's "cap reflected damage
  at the damage taken"), and a static guard stops two Mirrors from reflecting at each other for ever. The lead may prefer to route it through `hit`; that would multiply it by the
  level factor, which this design avoids on purpose.
* Heals: none. Mana: the dispatcher pays the cost; only Covet pays an extra 2 percent through `HeroCtx.pay` (after every refusal check).
* Control: Chain Binding Mirror and Ressentiment take `HeroCtx.control()` before binding anyone; the bash's slowing and the Stardust's absorption are not control.
* No entity is spawned (the budget is six). No block is changed. The longest task is the Stardust's 8 seconds (limit 20); every task runs through `Safe`.
* Targets: foes and allies come from `Foes` (never creative or spectator players, never allies, never the wielder's own pets or summons). At most 8 targets in any technique.
* State: `PlayerState` (cleared on logout and on server stop); the only static flag is the rebound's re-entrancy guard, reset in a `finally`.
* Effects: only `VfxSpawn` shapes (`MIRROR_FRAME`, `MIRROR_RAY`, `MIRROR_STEP`, `MIRROR_SHATTER`, `MIRROR_ARRAY`, `CHAIN_FX1`), `Tells` and sounds. No vanilla particles.

## 12. Unverified, assumed, and to be checked by the lead

* **Nothing has run in the game.** Type-checked against the API stubs and the pure logic unit-tested, nothing more. The self-test `hero.weapon.mirror` was written but has not run.
* The core must call `HeroLogic.onDamaged` for every bonded player (summoned or not) and use its return value as the damage the wielder takes; the image, the ward and the rebound
  all live there. If the core only calls it while summoned, the image and rebound still work (they are only created while summoned).
* The `VfxShape` argument conventions (`from` as the position and `to` as the facing for the frame shapes) were read from their Javadoc, not seen on screen.
* `AntiMagic.isMagic(DamageSource)` is used to tell a spell from a blow; how well it recognises the spells of this mod and of Tensura was not checked.
* "Hostile projectile" means a projectile entity (`Deflect.dangerous`); instant (hitscan) spells and any spell that is not a projectile entity are not stopped by the panes.
* Item ids: every vanilla id the forms use was checked as a field of `net.minecraft.world.item.Items` in the API dump (the registry name is the field in lower case), and `tensura:magic_stone` and `tensura:high_quality_magic_crystal` are used elsewhere in this repository and are fields of `TensuraMaterialItems` in the Tensura dump. None of this was seen in the running game; CI's `hero.forms` and this weapon's self-test resolve them against the real registry.
* The number of simultaneous panes (2), their size and charges are first guesses and will need playtesting.
* Mirror Bash's stagger is vanilla Slowness IV for 12 ticks, which a player can shrug off with an effect cure.

## 13. Sources

* https://shield-hero.fandom.com/wiki/Vassal_Mirror (summary)
* https://shield-hero.fandom.com/wiki/Vassal_Weapons (summary: gem and absorption)
* https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List (summary: Envy and Sloth entries)
* https://shield-hero.fandom.com/wiki/Naofumi_Iwatani/Powers_and_Abilities, https://shield-hero.fandom.com/wiki/Albert, https://shield-hero.fandom.com/wiki/Heroes_(Web_Novel) (named in search results)
* In the repository: `book/MirrorBook.java`, `book/MirrorWorks.java` (compared, not reused), `docs/heroweapon/design.md`, `item/WeaponEngravings.java`.
