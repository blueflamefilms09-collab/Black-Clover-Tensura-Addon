# Escanor, the Lion's Sin of Pride (boss, tier 3)

Why this page exists: Escanor is the boss whose fight is a clock. The owner asked that the game's time of day matter and that the fight still work in the dark
Mega-Dungeon through a documented mechanic; this page documents it (section 3), then every move with its number and counter, the config and quest hooks, the
weapon he would give (not built) and what is not verified. Nothing here has run in a game.

## 1. Who he is (canon, with the limits of what I could read)

The pages could not be opened (the fetch tool was refused), so every fact comes from SEARCH SUMMARIES read on 2026-10-17, and from memory of the series.

* Lion's Sin of Pride. By day the strongest of the Seven Deadly Sins; at night a timid, bashful, small man. His Grace **Sunshine**: weakest at midnight, rising
  from sunrise, peaking at noon (when his personality turns arrogant). Sources: the Looper explainer
  <https://www.looper.com/735035/escanors-powers-from-the-seven-deadly-sins-explained>, the fan wiki <https://all-worlds-alliance.fandom.com/wiki/Sunshine_(Seven_Deadly_Sins)>,
  <https://en.wikipedia.org/wiki/List_of_The_Seven_Deadly_Sins_characters>, a VS Battles thread
  <https://vsbattles.com/threads/sunshine-against-ice-escanor-vs-esdeath-concluded.25521/post-777203> (all through search summaries; several other hits were
  content farms and were ignored).
* **"The One"**: the form at the very peak of noon; one source says it fades the minute noon passes.
* **Rhitta**, the Divine Axe (his Sacred Treasure): can be imbued with his power so its slashes burn; one blog says it lets him use some power at night (a "battery
  for the sun"; weakly sourced, and it is what the Gates floor below borrows).
* **Cruel Sun**: a small sun made in his hand and hurled; it can also be directed. **Pride Flare**: a burst of heat and light (one low-quality blog).
* The look: a small timid man at night (glasses, brown hair were not confirmed by any page I could read), a large, muscular, long-haired man by day.

## 2. The record (`SdsCharacters.ESCANOR`)

Saga `SEVEN_DEADLY_SINS`, tier 3. 3000 health, attack 24, speed 0.30, armour 16, toughness 10. Floors 20 to 80 on the HOLY road only. Favoured themes `radiant`,
`celestial_court`, `sky_citadel`, `holy_church`. After the four Eye of the Midnight Sun bosses (`fana`, `patolli`, `rades_spirito`, `rhya`). Colours 0xE8A317
(gold), accent 0xFFE680. Magic family `flame`.

## 3. The fight: the sun is the mechanic

### A. How the sun works in a dungeon with no sky (`SunClock`, pure)

* Every dimension shares the world's day time (a custom dimension's level data forwards to the overworld's), so the dungeon, which has no skylight, still has the
  world's hour. Escanor reads `ServerLevel.getDayTime()`.
* A fight of a few minutes would see one frozen hour, so the fight has its own clock: it starts at the hour the world shows and then runs `escanorSunSpeed` times
  faster than the game (default 3: a whole day in 6 minutes 40 seconds). 1 is the plain world clock, 0 holds the start hour, `escanorSunStart` can force `dawn`,
  `noon` or `midnight`. The party's choice of hour (when they enter) still matters most: it is where the fight begins. The clock does not depend on the
  `doDaylightCycle` rule, because it counts game ticks from the start of the fight.
* The sun is `sunshine(dayTime) = 0.18 + 0.82 * x^1.5` with `x = (cos(turn) + 0.25) / 1.25` (the sun's height with a quarter of twilight): 0.18 in the dark half,
  1.0 at noon. About 25% of the day is above 0.72 (afternoon sun) and about 15% above 0.90 (The One).
* **The Gates floor**: once the Gates are open, Rhitta pours out the light it stored, and the sun cannot fall below `escanorGatesSunFloor` (0.60), so the last
  quarter of the fight is never a walkover because the party arrived at midnight. The bar title says "(stored light)".

### B. What the sun does (`Sunshine`, pure)

| the sun s | his damage factor 0.60 + 0.80 s | what he takes 1.10 - 0.38 s | speed + 10% s | heat radius | power the sun allows |
|---|---|---|---|---|---|
| 0.18 (midnight) | 0.74 | 1.03 | +1.8% | none | 20 |
| 0.30 | 0.84 | 0.99 | +3% | none | 40 |
| 0.50 | 1.00 | 0.91 | +5% | 5.0 | 60 |
| 0.72 | 1.18 | 0.83 | +7% | 5.7 | 80 |
| 0.90 | 1.32 | 0.76 | +9% | 6.2 | 100 |
| 1.00 (noon) | 1.40 | 0.72 | +10% | 6.5 | 100 |

* The factor on what he takes is applied in `mitigate`, which the framework skips while an ultimate has left him exposed (so the sun shields him, and the
  exposure after an ultimate still opens him).
* **The power the sun allows.** The scheduler is shown `min(the power his health has reached, the power the sun allows)` (`EscanorBoss.chooseMove`), so the
  framework's own move gating (each move has a minimum power) becomes "needs daylight" without a second system: the opening moves (20) at any hour, the charge and the
  hazes (40) from dawn, the pillars and the spin (60) by mid-morning, **Cruel Sun (80) only while the sun is above 0.72**, and **Zenith Cleave (100) only at noon
  with the Gates open**. The damage, cooldowns and wind-ups of the framework still follow his real power.
* **The heat** (`escanorHeatAura`): while the sun is above 0.45 everything within the radius takes a pulse every second (0.8 + 2.0 s base, Tensura `heat_wave`
  damage, so heat and flame resistance count; 1.7 to 2.8). It is a nag that makes a melee party's resistances matter and a ranged party always safe (the radius
  stays under 8).
* **The form**: below about 0.45 the frail man (model 1, hitbox x0.88), above it Pride in the daylight (model 2, x1.20), above about 0.92 The One (model 2, x1.42);
  the thresholds have hysteresis (up at 0.47 and 0.92, down at 0.43 and 0.88) so the model never flickers. With the Gates open it is at least the second.
  A flare, a sound and a line in the chat mark every change. The hitbox follows the form, and the renderer scales the model to the hitbox.

### C. The counters

* **Fight him at night** (the series' own weakness): he is slower, hits at 0.74, takes 1.03, has no heat and only his four opening moves. The party that waits
  for the dark part of the day (about 47% of it: 3 minutes of real time at the default speed) is rewarded; the Gates floor stops it from ending the fight there.
* **Stay out of the heat, or carry its resistances** (flame, heat): the Charter Grimoire he drops lends exactly those.
* **Break the Cruel Sun** (section 4): 8% of his maximum health in the wind-up, or cover, or a shield, or walk out.

## 4. The moves (`EscanorTable`, numbers before the boss's damage law)

All telegraphs are at least 16 ticks (12 at the Gates) and the ultimates at least 30 (the framework); the aim, the mark or the ring is locked at the start of the
wind-up and drawn for the whole of it. The axe is steel: its blows are the boss's plain physical blow (a raised shield helps); flames, heat and light are typed
Tensura damage (`fire_elemental`, `heat_wave`, `light_elemental`).

| move (id) | needs the sun (power) | wind-up / cooldown / recovery | range | weight | base damage | what it is | counter |
|---|---|---|---|---|---|---|---|
| Rhitta Swing (`rhitta_swing`) | any | 16 / 70 / 8 | 0 to 6.2 | 6 | 17 | a cone of 140 degrees, reach 5.8 | leave the cone, or get behind him |
| Rhitta Smash (`rhitta_smash`) | any | 22 / 150 / 12 | 0 to 9 | 4 | 21 + 14 | the axe on the ground 3.8 ahead (radius 3), then a low ring wave to 7 | leave the ring, then jump the wave |
| Flare (`solar_flare`) | any | 18 / 180 / 10 | 0 to 8.5 | 3 (crowd) | 11 | a burst of fire radius 6.5 that shoves out; cover cuts it to 40% | get out of the radius |
| Rhitta Flame Wave (`rhitta_wave`) | any | 20 / 130 / 8 | 5 to 32 | 5 | 11 each | 1 to 3 lions of flame along the lines drawn | step out of the lines |
| Pride's Charge (`pride_charge`) | 40 (dawn) | 18 / 210 / 12 | 8 to 30 | 3 | 18 + 11 | a charge along a strip 3.2 wide, a ring wave where he stops | step out of the strip; jump the ring |
| Heat Haze (`heat_haze`) | 40 | 22 / 300 / 10 | 0 to 40 | 3 (crowd) | 2.5 per half second | 2 to 4 hazes of radius 3.2 for 4.5 s on the foes | leave the rings |
| Sunbeam Pillars (`sunbeam_pillars`) | 60 (mid-morning) | 24 / 230 / 12 | 4 to 36 | 4 (crowd) | 15 | 4 to 6 pillars of light radius 2.2 on the marks; shield or anti-magic cuts them | leave the rings |
| Sun Spin (`sun_spin`) | 60 | 22 / 190 / 12 | 0 to 6.8 | 4 (crowd) | 9 + 9 | two full circles of cuts, 8 ticks apart, radius 6.2 | back away; shield |
| Cruel Sun (`cruel_sun`, ultimate) | 80 (sun above 0.72) | 60 / 1300 / 60 | 0 to 40 | 5 (crowd) | 28 x (0.8 + 0.4 s) | a small sun thrown at the marked ring (radius 6.5 + 4 s, plus 1.5 at the Gates); it takes 24 ticks to arrive, then burns the ground | below |
| Zenith Cleave (`zenith_cleave`, ultimate) | 100 (noon, Gates open) | 62 / 1400 / 60 | 0 to 40 | 5 (crowd) | 32 + 22 | he comes down on the marked ring (radius 3.5, 32), and a low shock runs to 15 (22) | stay out of the middle, jump the shock |

**Cruel Sun in detail** (`CruelSunMath`, tested): the ring is on the ground for the whole wind-up and the flight, so everyone has `windUp + 24` ticks to leave
(3.4 s at the Gates), and the burst can always be walked out of (radius at most 12 at 4.3 blocks a second). A block between the sun (5 blocks above the mark)
and a foe cuts the blow to 30%, a raised shield to 35% (the better applies, never both). **If the party takes 8% of his maximum health off him while he winds
it up the sun breaks in his hand** (nothing happens but a flash and he is open for the usual 60 ticks); everyone who hit him in that window is credited with
`sds.cruel_sun_broken`; an action bar line counts the party's progress. After a burst the ground burns for 3 to 5 s (radius 55% of the burst, 2.5 a second).
After an ultimate he is exposed for 60 ticks.

## 5. Config and quest keys

`[sdsa]` in `nusmp-arc-sdsa-server.toml`: `escanorSunSpeed` 3.0 (0 to 12), `escanorSunStart` "world" ("world", "dawn", "noon", "midnight"; anything else is "world"),
`escanorGatesSunFloor` 0.60 (0 to 1), `escanorHeatAura` true. Quest hook key: `sds.cruel_sun_broken`.

## 6. The model (art kit, `tools/arc/char_escanor.py`)

Model 1: the man of the night and dawn (a short coat over a shirt, brown hair, big shy eyes, eye bags, a slouching idle). Model 2: Pride in the daylight (a
dark tank, gold pauldrons and bracers, a wild orange mane, fierce eyes, stubble, a glow that burns gold). Rhitta is a prop of this character's own (the kit's axe
has a thin haft; the Divine Axe is a long haft with a great crescent blade of three plates, a gold crest and a glowing edge), registered in the spec file as the
art manual allows. ArcLook from the manifest, with a wider shadow for the big forms: `("escanor", 2.0, 1.16, 2.06, 1.10, 1.83, 0.81, true)`, base hitbox 0.78 x 2.0,
scaled x0.88 / x1.2 / x1.42 by the form. The colours and the lion on the belt were not confirmed by a source and are for the game. Preview PNGs (looked at,
kept outside the repository in the job's scratchpad folder `previews/sdsa/`): `arc_escanor_front.png`, `_idle`, `_walk`, `_attack`, `_cast_big`, `_transform`,
`_death`, `arc_escanor_form2_front.png`, `_form2_idle`, `_form2_attack`, `_form2_cast_big`.

## 7. The Charter Grimoire (chance 0.30)

Nine real Tensura skills: `pride`, `flame_domination`, `heat_wave`, `solar_flare`, `solar_beam`, `solar_rain` (the Cruel Sun and the Flare), `flame_attack_resistance` and
`heat_resistance` (what lets a player stand near him), `strengthen_body`.

## 8. The weapon he would give (NOT built)

**Rhitta, the Divine Axe**: a real 3D model in hand with a glow that follows the sun, and the Tensura engravings `crushing` 3, `elemental_boost` 3, `sturdy` 2 and
`holy_weapon` 1 (the sun's light), to be added to `item/WeaponEngravings.java`.

## 9. Tests and what is NOT verified

Pure and tested: `SunClockTest`, `SunshineTest`, `CruelSunMathTest`, `EscanorTableTest` (the real `MoveScheduler` played through a whole day of sun: the opening
moves at midnight, the Cruel Sun only in the afternoon sun, the Zenith only at noon with the Gates open); all mutants caught. `EscanorBoss`, `EscanorMoves` type-check
against the stubs (only the known artifact remains: the missing `deserializeNBT` on an entity subclass).

Not verified: nothing has run in a game. (1) That the Mega-Dungeon's `ServerLevel.getDayTime()` is the world's (it should be: a custom dimension's level data is a
`DerivedLevelData`, which forwards to the overworld's; if it were not, the fight clock still runs from the start hour, and `escanorSunStart` can fix the hour).
(2) That the model swap and the hitbox change on a form flip look acceptable (no transform clip plays; a form change by the sun is an instant change with a flare);
and that a x1.42 hitbox fits the arenas. (3) The numbers (damage through the balance law, the 8% break threshold, the radii). (4) The look of the effects (shapes
borrowed from other characters, tinted: `FIRE_LION`, `FIRE_SPEAR`, `FIRE_WILD`, `DIVINE_PILLAR`, `LIGHT_FLARE`, `SLASH_SCYTHE`; unseen). (5) The move names that are mine:
Rhitta Swing, Smash and Flame Wave, Pride's Charge, Heat Haze, Sunbeam Pillars, Sun Spin and Zenith Cleave; only Sunshine, The One, Cruel Sun, Rhitta and (weakly)
Pride Flare come from the sources I could read. (6) The boss bar title is changed by the phase of the day through a small trick (the framework's refresh is private, so
`startSeenByPlayer` is called for one fighter, which rebuilds the title for everyone watching): if it does not update live, a new fighter's bar is right at least.
