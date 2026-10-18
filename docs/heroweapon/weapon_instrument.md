# Vassal Musical Instrument (`vassal_instrument`): the violin of Glass' world

Builder `hvd`. Logic: `heroweapon/weapon/instrument/**`, forms: `data/nusmp/hero/forms/vassal_instrument.json`. Style CAST, class Vassal Weapon.
Written in two passes: the LOOK section (1) first so the art builder could start, then the rest.

## 1. The look (for the art builder `hart4`)

**The instrument is a VIOLIN with its bow.** Why: the series' wiki summaries list a violin among the instrument's forms (and a guitar for the stun skill, a grilled-fish
shaped one for a joke skill, bells and a piano elsewhere); the weapon changes shape, but one item has one mesh, and the violin is the form every summary names first. It is
also the best silhouette for a first-person weapon: the long neck and scroll give a clear "tip", the bow gives a second line that reads as a blade from the front, and the
curved body is easy to loft with the owner's mesh method. (State of knowledge: the violin form is from a summary; the choice of it as THE look is design.)

* **What it is.** A full-size violin, about 0.60 blocks of body plus neck and scroll (0.90 in all), held by the neck with the scroll pointing up and forward, and the bow held
  alongside, crossing the strings diagonally with its tip forward (the bow is 0.75 blocks long). The whole read is about 1.0 blocks (size class `handitem` or `wand`: the art
  builder decides by the real-life size rule). Keep the violin's real proportions: waist, two bouts, f-holes, bridge, tailpiece, chin-rest, scroll, pegbox with four pegs.
* **Named parts** (10 to 14): `body` (the lofted top and back plates with the ribs), `waist` can be part of the body loft, `fingerboard`, `neck`, `scroll` (the curl), `pegbox`
  and `pegs` (four small cylinders), `bridge`, `strings` (four very thin sweeps), `tailpiece`, `f_holes` (cut into the top plate in the skin, not in the geometry), `bow`
  (stick, a thin loft, with `hair` as a flat ribbon and a `frog` at the grip end), `gem`.
* **The gem.** Set into the EYE of the scroll (the centre of the curl): a rose-coral cabochon, about 0.10 blocks, in a gold claw. It glows.
* **Palette**: body varnish maple-amber `#B8631F` shading to `#7A3B14` at the edges and a pale gold highlight `#E0A24A` on the upper bout; ebony fingerboard, pegs, chin-rest and
  tailpiece `#1C1410`; strings pale gold `#EBD9A0`; bridge maple `#D9B07A`; gold fittings (rings on the bow frog, the gem claw) `#E3B341`; bow stick brown `#5A3A22`; bow hair
  white `#F2EDE2`; gem `#FF7AC8`.
* **Glow skin** (additive): the gem (`#FF9AD8` full), the f-holes (a thin warm line `#FFB347`), a few small note-shaped inlays on the top plate (`#FFDDAA`, three at most, tiny),
  and the four strings (a very faint `#FFE9B0`). The varnish does not glow.
* **Mood.** Warm, elegant, a little playful; the opposite temperature of the Mirror. Vassal weapons are more ornate than Holy ones: carve the scroll well and add a thin gold
  inlay line (purfling) around the top plate.
* **No curse look** (this weapon has no Curse Series).
* **Icon (16 x 16 sprite).** An amber violin on the diagonal, scroll at the upper right, a pink dot in the scroll, the bow as a light line across it.

## 2. Canon: what the series says, and what is design

Source types: **S** = a search summary of the series' fan wiki (the pages themselves could not be opened from the build machine), **M** = memory, **D** = design. Nothing
here was checked against the light novels or the anime.

| Fact | Source |
|---|---|
| The Vassal Musical Instrument is one of the eight Vassal Weapons of Glass' world; Itsuki Kawasumi (also the Bow Hero) is its current wielder, chosen by it. | S: https://shield-hero.fandom.com/wiki/Vassal_Musical_Instrument |
| Hidemasa Miyaji was its first known user; he took power in the instrument country with it and kidnapped Kizuna. He was not a worthy wielder and had to suppress it with an accessory; once freed it chose Itsuki. | S: https://shield-hero.fandom.com/wiki/Hidemasa_Miyaji |
| An untrained wielder's notes fly in random directions (Miyaji's Music Stream did); Itsuki's ESPer power lets him aim. | S: https://shield-hero.fandom.com/wiki/Itsuki_Kawasumi/Powers_and_Abilities, .../wiki/ESPer |
| Music Strike fires something like an arrow from the instrument. Demon's Melody fires countless notes. Stun Beat turns the instrument into a guitar; the notes burst in a bright flash and stun the target. Shock Music and Music Stream are listed. | S |
| Deadly Poison Music shoots a note that inflicts a deadly poison, which may kill if not cured. | S |
| Glutton God Tango belongs to an instrument that looks like a grilled fish; it "enhances digestion" for those who hear it. | S |
| Musical Instrument 0 (the 0 series form, made by absorbing the mysterious poison in Fitoria's sanctuary): no damage, no cooldown, no SP cost, effective against illegitimate powers. | S: https://shield-hero.fandom.com/wiki/0_Series |
| The wiki's forms include a violin, a guitar, a fish-shaped instrument, Humming Fairy instruments, a True Demon Dragon instrument, and the Four Heavenly King's Bell accessory. A second search of the full weapon list found no violin and no Humming Fairy, so those two names are less certain. | S (conflicting) |
| The songs as party auras (March, War Song, Lullaby, Dirge), the harmony and Tempo mechanic, the Crescendo, every number, the violin as the look, the form names "Stun Beat Guitar", "Grilled Fish Horn" and "Humming Fairy Lyre" as tier names. | D (the Dirge is named for the canon Deadly Poison Music; the March and War Song follow the brief) |

Canon techniques deliberately NOT built: Music Stream and Shock Music (no description to build from; Music Stream's random notes became the harmony idea), the True Demon
Dragon instrument and the Four Heavenly King's Bell (an accessory with fixed compositions).

## 3. How it plays: songs, notes and a beat

The Musical Instrument has two jobs. **Notes** (Music Strike, Demon's Melody, Stun Beat) are hitscan shots. **Songs** (March, War Song, Lullaby, Dirge, Glutton God Tango,
Zero Note) are auras and tunes for allies and foes round the wielder. The rhythm input the brief welcomed is the **harmony**:

* The beat is the gap between two techniques. Cast the next one **20 to 100 ticks** after the last (1 to 5 seconds; up to 6 with the Steady Beat passive) and the harmony of
  the phrase grows by one, up to four. Cast it sooner and the harmony is neither gained nor lost; let the pocket pass and the phrase starts again from zero. The dispatcher keeps
  two seconds between any two techniques by default (`skillGlobalLockTicks` 40), so in play the window is 2 to 5 seconds. A technique that is refused (no target) does not count.
* Harmony tightens the notes (the series says an untrained wielder's notes fly wildly) and lengthens and strengthens the songs. At **harmony 3** the Crescendo opens.

| harmony | Music Strike (damage / stray) | Demon's Melody fan (half-angle) | song length (March / War Song) | at 3 and above |
|---|---|---|---|---|
| 0 | 6.0 / 6 degrees | 20 degrees | 10 s / 8 s | |
| 1 | 6.5 / 4 degrees | 17 degrees | 11 s / 8.8 s | |
| 2 | 7.0 / 2 degrees | 14 degrees | 12 s / 9.6 s | |
| 3 | 7.5 / none | 11 degrees | 13 s / 10.4 s | March Speed II, War Song adds Resistance I, Dirge 2 s longer, Crescendo opens |
| 4 | 8.0 / none | 8 degrees | 14 s / 11.2 s | the same |

Why this and not a held key: the dispatcher only wires a DRAW-style release for the bow, so a held-and-released rhythm would need a new hook. Timing the keys needs nothing new,
works on a server, and rewards a wielder who plays a phrase rather than pressing one key.

The songs are **auras that follow the wielder** (they pulse once a second for their length and stop when the instrument leaves the hand), not zones left on the ground.

## 4. The forms tree

Items to absorb are listed per form; absorbed items are kept, never consumed (design.md section 6). All forms use the base look (a violin); there is no curse series.

| form (id) | tier | parents | needs | mastery | stats | use / sneak / key1 / key2 / key3 | passives |
|---|---|---|---|---|---|---|---|
| Violin (`base`) | 0 | - | nothing (always learned) | 0 | movement_speed 0.01, luck 0.5 | `instrument.music_strike` / - / `instrument.march` / `instrument.lullaby` / - | - |
| Stun Beat Guitar (`guitar`) | 1 | base | #minecraft:planks x32, minecraft:string x24, minecraft:note_block x8 | 80 | movement_speed 0.02, luck 1 | `instrument.music_strike` / `instrument.stun_beat` / `instrument.march` / `instrument.lullaby` / `instrument.demons_melody` | perfect_pitch |
| Humming Fairy Lyre (`humming_fairy_lyre`) | 2 | guitar | minecraft:amethyst_shard x32, minecraft:gold_ingot x16, minecraft:phantom_membrane x8 | 160 | movement_speed 0.03, max_health 2, luck 1.5 | `instrument.music_strike` / `instrument.dirge` / `instrument.march` / `instrument.war_song` / `instrument.demons_melody` | perfect_pitch, fairy_choir |
| Grilled Fish Horn (`grilled_fish_horn`) | 2 | guitar | minecraft:cooked_cod x32, minecraft:cooked_salmon x16, minecraft:kelp x16 | 160 | movement_speed 0.02, max_health 4, luck 1 | `instrument.music_strike` / `instrument.glutton_god_tango` / `instrument.march` / `instrument.lullaby` / `instrument.war_song` | perfect_pitch |
| Musical Instrument 0 (`instrument_zero`) | 3 | humming_fairy_lyre, grilled_fish_horn | minecraft:fermented_spider_eye x24, minecraft:poisonous_potato x32, minecraft:pufferfish x12, tensura:magic_stone x32 | 280 | movement_speed 0.05, max_health 4, armor_toughness 1, luck 2 | `instrument.music_strike` / `instrument.zero_note` / `instrument.march` / `instrument.war_song` / `instrument.crescendo` | perfect_pitch, fairy_choir, steady_beat |

Notes on the tree: the Guitar turns Music Strike accurate (Perfect Pitch) and adds the Stun Beat and the Demon's Melody; from it the tree splits into the Lyre
(a quarter wider songs, War Song, the Dirge) and the Fish Horn (the Glutton God Tango, a healthier wielder), and the 0 series form needs both. The Instrument 0 form carries
the Zero Note and the Crescendo and all three passives.

## 5. The techniques

### 5.1 The budget (design.md section 5), straight from `InstrumentNumbers`

Raw damage is before the bond-level factor (1.0 to 2.2) and the balance law. A song's figures are per effect application, not per second.

| technique | kind | raw per hit | most on one target | targets | cooldown | cost | heal |
|---|---|---|---|---|---|---|---|
| `instrument.music_strike` | ordinary | 8 | 8 | 1 | 60 ticks (3 s) | 1% | - |
| `instrument.demons_melody` | ordinary | 9 | 9 | 8 | 100 ticks (5 s) | 2% | - |
| `instrument.stun_beat` | ordinary | 6 | 6 | 6 | 200 ticks (10 s) | 2% | - |
| `instrument.march` | utility | 0 | 0 | 0 | 240 ticks (12 s) | 2% | - |
| `instrument.war_song` | utility | 0 | 0 | 0 | 300 ticks (15 s) | 2% | - |
| `instrument.lullaby` | utility | 0 | 0 | 0 | 260 ticks (13 s) | 2% | - |
| `instrument.dirge` | utility | 0 | 0 | 0 | 260 ticks (13 s) | 2% | - |
| `instrument.glutton_god_tango` | utility | 0 | 0 | 0 | 400 ticks (20 s) | 2% | 10% |
| `instrument.zero_note` | utility | 0 | 0 | 0 | 360 ticks (18 s) | 1% | - |
| `instrument.crescendo` | ultimate | 4 | 24 | 10 | 700 ticks (35 s) | 6% | - |

The test `InstrumentNumbersTest` hands exactly these rows to `Budget.violations`. The Music Strike row says 8 because that is the most it can reach (6 plus half a point per
harmony); the Crescendo's 24 is six pulses of 4.

### 5.2 Technique by technique

Every technique except the Crescendo goes through the beat (`InstrumentSkills.perform`): the harmony is previewed before the technique runs and counted only if it really happened.

**`instrument.music_strike` Music Strike** (ordinary, 3 s, 1 percent)
* What: a note shot like an arrow, 24 blocks, 0.35 wide, along the look direction turned by a random stray (yaw and half as much pitch) of up to 6 degrees minus 2 per harmony. The first
  foe on the ray takes 6 plus half a point per harmony. Refused (and free) when it finds no one.
* Tell: a star-bright line from the instrument; no wind-up (8 raw or less needs none).
* Counter: strafe or break the line of sight; at low harmony it is wild.

**`instrument.demons_melody` Demon's Melody** (ordinary, 5 s, 2 percent)
* What: seven notes in a fan inside a half-angle of 20 degrees minus 3 per harmony (8 at the least), 14 blocks. Each note that strikes does 3; one foe takes three notes at most (9);
  eight foes at most.
* Tell: seven short lines.
* Counter: distance (the notes stray at range) or cover.

**`instrument.stun_beat` Stun Beat** (ordinary, 10 s, 2 percent)
* What: the guitar's note flies to the foe in the crosshair (or the ground where you look, 18 blocks) and bursts 10 ticks later in a flash: every foe within 3.5 blocks (six) takes 6. Behind
  the shared control lock they are also stunned for the law's control time (2 s base, Slowness X) and blinded for 1 second; without the lock the damage still lands.
  Refused when no foe stands where it would fall.
* Tell: a ring on the ground at the landing point for half a second, and the note in flight.
* Counter: leave the ring.

**`instrument.march` March** (utility, 12 s, 2 percent)
* What: an aura round the wielder, radius 10 (12.5 with Fairy Choir), pulsing once a second for 10 seconds (longer with harmony): allies in it (eight at most) get Speed I (Speed II
  from harmony 3) and Haste I for three seconds, refreshed each pulse. Ends if the instrument leaves the hand.
* Tell: a ring of notes at the wielder's feet and notes floating round them; the effects show on the allies.
* Counter: out-range the wielder, or kill them: the aura moves with them.

**`instrument.war_song` War Song** (utility, 15 s, 2 percent)
* What: the same aura for 8 seconds (longer with harmony): allies get Strength I, and from harmony 3 Resistance I.
* Tell and counter: as the March.

**`instrument.lullaby` Lullaby** (utility, 13 s, 2 percent)
* What: foes within 9 blocks (eight at most; the nearest first) are slowed (Slowness III) and weakened (Weakness I) for the law's control time (6 s base) and their pathfinding is stopped.
  Shared control lock; refused when no one is near or the lock is taken.
* Tell: a lavender ring and a sleepy flute note; the victims visibly slow.
* Counter: stay out of 9 blocks; cure the effects (milk).

**`instrument.dirge` Dirge** (utility, 13 s, 2 percent)
* What: foes within 9 blocks (eight at most) get Poison I and Weakness I for 6 seconds (8 from harmony 3). Both level I on purpose: poison is damage that does not go through the
  balance law, and level I over eight seconds is about six health, which keeps it inside the ordinary budget and under the cap on a player. Poison never kills a player.
  No control lock (not control).
* Tell: a purple ring and a low note.
* Counter: milk, or leave.

**`instrument.glutton_god_tango` Glutton God Tango** (utility, 20 s, 2 percent, heals 10 percent)
* What: allies within 8 blocks (six at most): players who are hungry eat 4 food and 0.8 saturation, anyone hurt mends a tenth of their maximum health (`HeroCtx.heal`, so the law's caps
  apply). Refused (and free) when nobody needs it.
* Tell: a peach-coloured ring and a banjo note.
* Counter: it is support.

**`instrument.zero_note` Zero Note** (utility, 18 s, 1 percent)
* What: no damage. Every ally within 8 blocks (eight at most) is freed of up to two harmful effects and of a sealed grimoire (the mod's `nusmp_sealed_until` mark). Refused (and free)
  when there is nothing to free. The canon "effective against illegitimate powers" is expressed as breaking bonds.
* Tell: a white ring and a bell.
* Counter: none needed.

**`instrument.crescendo` Crescendo** (ultimate, 35 s, 6 percent)
* What: needs harmony 3 (refused and free below it) and spends the phrase. After 20 ticks of gathering, six pulses 12 ticks apart: each hurts foes within 10 blocks (ten at most) for 4
  (24 in all on one target) and slows them for 1.5 seconds (Slowness I), and mends allies in range by 2.5 percent of their health (15 percent in all). It stops if the instrument is put away.
* Tell: a ring on the ground at the wielder's feet and notes rising for a second before the first pulse, and a ring at each pulse.
* Counter: leave the 10-block radius, or force the wielder to put the instrument away. The slow is light so the answer is open.

## 6. The passives

* **`perfect_pitch`** (Guitar and every form after it): the notes never stray (Music Strike) and the Demon's Melody half-angle is at most 12 degrees even at harmony 0. Canon: Itsuki's accuracy.
* **`fairy_choir`** (Humming Fairy Lyre, Instrument 0): songs, the Lullaby, the Dirge and the Crescendo reach a quarter farther.
* **`steady_beat`** (Instrument 0): the pocket for the next technique is 20 to 120 ticks instead of 20 to 100, so the harmony is easier to keep.

## 7. Curse series

None. The brief gives curse series only to the Mirror (Envy) and the Chain (Sloth).

## 8. How it differs from what the mod already has

The mod already has **Song Magic** (`song/**`): a Black Clover attribute book with four modes (Healing Hymn, Lullaby, Barrier Chorus, Requiem) driven by how loudly the player sings
(a microphone through a voice mod, or a held key), with a magicule bill for every pulse. The Vassal Instrument shares no code with it. What is different:

| Song Magic (grimoire) | Vassal Musical Instrument |
|---|---|
| input is the player's voice or a held key | input is the timing of technique keys (the harmony) |
| a mode is chosen, then sung, and every pulse costs magicules | each technique is one cast with one cost; the aura pulses are free |
| four fixed modes: heal, sleep, barrier, damage | notes (damage), speed and strength songs, slow-and-weaken, poison, a meal, a cleanse, a finisher |
| the Lullaby puts creatures to sleep while you sing | the Lullaby slows and weakens them once for six seconds |

The two can both be used; they share no state.

## 9. Engravings (the owner's rule 6) and why

```java
Map.entry("vassal_instrument", List.of(e("swift", 1), e("magic_weapon", 1), e("enervation", 1))), // a violin and bow: quick, magical, and its dirges sap strength
```

* `swift` 1: a bow drawn fast across the strings; the notes are quick shots.
* `magic_weapon` 1: every song is magic played through an instrument (the same engraving Tensura's mana-forged tools carry).
* `enervation` 1: the Lullaby and the Dirge sap the strength of what they reach (canon: Deadly Poison Music).

## 10. Code map: what each file is for

Config keys: none (a weapon keeps its numbers in code, in a pure table with a test). Commands: none. Quest keys: none. Lang: the keys `hero.skill.instrument.<name>.name` and `.desc` for each skill and `hero.passive.instrument.<id>` (and `.desc`) for each passive are in the fragment `hvd.lang.json`, which the lead merges into `en_us.json`.

All under `heroweapon/weapon/instrument/` (every class has a "Why:" Javadoc).

| File | Why it exists | Tests |
|---|---|---|
| `InstrumentLogic` | the `HeroLogic`: lists the ten skills from the rows (each wrapped in the beat), clears the beat on `onDismiss`, registers the self-test `hero.weapon.instrument` | self-test, `InstrumentFormsTest` (3) |
| `InstrumentSkills` | the bodies of the techniques and the `perform` wrapper | self-test |
| `InstrumentNumbers` | every figure and the rules that turn harmony into numbers (stray, damage, fan angle, song length, March speed, Melody damage) as pure code | `InstrumentNumbersTest` (9) |
| `Tempo` | the beat: pocket, harmony, preview and commit | `TempoTest` (8) |
| `vassal_instrument.json` | the forms file | `InstrumentFormsTest` runs the shared checker on the shipped file |

## 11. Server rules, checked

* Damage: all through `HeroCtx.hit`, except the Dirge's poison (a vanilla effect at level I, about six health, kept inside the ordinary budget on purpose; it never kills a player).
* Heals: only through `HeroCtx.heal` (Glutton God Tango 10 percent, the Crescendo 2.5 percent a pulse, 15 percent in all; both under the 25 percent cap).
* Control: the Stun Beat's stun, the Lullaby behind `HeroCtx.control()`; the Crescendo's slow is Slowness I for 1.5 seconds, which is not hard control.
* No entity is spawned. No block is changed. Longest task: the March's 14 seconds at harmony 4 (limit 20), clamped to 20 by `Safe`.
* Targets: foes and allies from `Foes`; at most 10. Songs stop when the weapon leaves the hand, the person leaves or the bond ends.
* State: `PlayerState` for the beat (cleared on logout and server stop). Effects: `VfxSpawn` (`STAR_MAGIC`, `SONG_RING`, `SONG_NOTES`, `LIGHT_FLARE`), `Tells` and note-block sounds. No vanilla particles.

## 12. Unverified, assumed, and to be checked by the lead

* **Nothing has run in the game**: type-checked and unit-tested only; the self-test `hero.weapon.instrument` has not run.
* The harmony pocket assumes the dispatcher lets a different technique be cast while another one is on cooldown (each technique has its own cooldown). With the core's default global lock
  of 40 ticks the lower edge of the pocket (20 ticks) can never be reached, which is harmless: the window is simply 2 to 5 seconds and the "too soon" branch is dormant.
* The `VfxShape` conventions for `SONG_RING` (position and size) and `SONG_NOTES` were read from their Javadoc, not seen.
* `Shock Music`, `Music Stream` and the accessories are not built (see section 2).
* Item ids: every vanilla id the forms use was checked as a field of `net.minecraft.world.item.Items` in the API dump (the registry name is the field in lower case), the tag `minecraft:planks` is a vanilla item tag, and `tensura:magic_stone` is used elsewhere in this repository and is a field of `TensuraMaterialItems` in the Tensura dump. None of this was seen in the running game; CI's `hero.forms` and this weapon's self-test resolve them against the real registry.
* The look is a violin; the wiki's own summaries are ambiguous about which instrument is "the" form.
* The figures (harmony table, song radii, cooldowns) are first guesses and will need playtesting.

## 13. Sources

* https://shield-hero.fandom.com/wiki/Vassal_Musical_Instrument (summary)
* https://shield-hero.fandom.com/wiki/Hidemasa_Miyaji, https://shield-hero.fandom.com/wiki/Itsuki_Kawasumi/Powers_and_Abilities, https://shield-hero.fandom.com/wiki/ESPer (named in search results)
* https://shield-hero.fandom.com/wiki/0_Series (summary of the 0 series)
* https://shield-hero.fandom.com/wiki/Vassal_Weapons/Full_list_of_Weapons (summary; the list by world)
* In the repository: `song/**` (compared, not reused), `docs/heroweapon/design.md`, `item/WeaponEngravings.java`.
