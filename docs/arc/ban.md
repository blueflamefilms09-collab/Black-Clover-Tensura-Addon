# Ban, the Fox's Sin of Greed (boss, tier 3)

Why this page exists: Ban cannot die in the series and takes what is not his. A boss that cannot die cannot be beaten, and this mod's boss rules allow no free
healing, so his immortality had to become a RULE of the fight with a price and a counter. This page documents that rule (section 3), the Snatch that steals a capped
share of a player's attack, every move with its number and counter, the config and quest hooks, the weapon he would give (not built) and what is not verified.
Nothing here has run in a game.

## 1. Who he is (canon, with the limits of what I could read)

The pages could not be opened (the fetch tool was refused), so every fact comes from SEARCH SUMMARIES read on 2026-10-17, and from memory of the series.

* Fox's Sin of Greed. Very tall, muscular, pale spiky hair, scarlet eyes, an open jacket over a bare chest, a fox mark on the left of the stomach (a scar on the
  jaw was only half confirmed). Sources: the Looper explainer <https://www.looper.com/796948/bans-powers-from-the-seven-deadly-sins-explained>,
  <https://en.wikipedia.org/wiki/List_of_The_Seven_Deadly_Sins_characters>, <https://comicvine.gamespot.com/app.php/ban/4005-93744/>, the figure listing
  <https://www.prime1studio.com/ntz-meliodas-ban-and-king/CMNTZ-01DXS.html> for the in-series look (search summaries only; the chatbot-persona pages that also came up were ignored).
* **Fountain of Youth**: he drank from it in the Fairy King's Forest; wounds, even the worst, close almost at once; no ageing, hunger or thirst; immune to poison; but "not
  invincible: heavy magical attacks can hurt him". In the series he later gives it up to bring Elaine back.
* **Snatch**: pulls an object to him and "steals" it; other sources say it steals physical traits (strength, speed) without touching, and a whip-like reach can grab
  hearts from a distance; one source says it fails against purification or psychic shields.
* **Courechouse**: his Sacred Treasure, a staff; the sources DISAGREE on its form (three sections, four sections joined by chains, a knife that extends into a
  nunchaku). The model here uses four sections. Its property is **Super Concentration**, which raises his strength and mental acuity.
* Not found: **Hunter Fest** and **Fox's Hunt** (names remembered from the series; two searches could not confirm them).

## 2. The record (`SdsCharacters.BAN`)

Saga `SEVEN_DEADLY_SINS`, tier 3. 2400 health, attack 16, speed 0.38 (the fastest of the three), armour 8, toughness 4. Floors 20 to 80 on the ABYSSAL road. Favoured
themes `verdant`, `cursed_archive`, `fungal_underdark`, `shadow`. After the four Eye of the Midnight Sun bosses (`fana`, `patolli`, `rades_spirito`, `rhya`). Colours
0x3C9E6B (green), accent 0xD9E8A0. Magic family `world_tree`.

## 3. The fight: two rules, each with a counter

### A. The Fountain of Youth (`FountainOfYouth`, pure; the glue is in `BanBoss`)

He has three lives of shrinking size: 100%, 60% and 45% of his health (2400, 1440, 1080 at the record's 2400). **When a life ends he does not die**: he falls, nothing
can hurt him for 10 seconds (8 after the second life), and then he rises with the next life. The rise goes through the framework's ONE sanctioned second wind, the
scripted refill that the Gates and Lucius's stages use (`refillForMode`, once per life per fight, 70 ticks, invulnerable while it runs): no regeneration, no free
healing, and the rule in `BossHealGuard` is untouched. Two writes of health go through the same sanctioned doors (`BossHealGuard.allow` inside the budget's `bypass`, as Lucius's
stage change does): the one health point he keeps at the moment of the fall (a body at 0 health is taken by the game within a second), and, only if the refill is refused (spent or
busy, which should not happen), a direct fill to the new reservoir. The last life, a kill command and the void kill him for real; so does turning the Fountain off
(`banFountain=false`, one life).

**Why this shape.** The immortality has to be a clock and a cost the party can read, not a number the party cannot reduce. The window is also the party's turn: it is the
time to drink, re-buff, reposition, and the one time a special blow matters:

* **The counter: cut the Fountain.** While he lies, anti-magic (a demon sword, the Anti-Magic Lord), or a holy, soul, severing or spirit damage type
  (`FountainOfYouth.cutsFountain`: damage-type ids containing "holy", "soul", "sever", "spirit"), does no damage (nothing can) but cuts the Fountain: **the next life is
  only 40% as big** (the second 60% becomes 24%, the third 45% becomes 18%). The chat says so and the first cutter of a window is credited with `sds.fountain_cut`.
* The total health the party must get through: 4920 with no cut, 4056 with the first window cut, 4272 with the second, 3408 with both. A framework rule still holds
  all along: no more than a per-second share of his health can be taken (the damage budget, 6% a second by default and 9% in the final phase), so each of the first two lives
  takes at least about 17 seconds and the last at least about 11.

**The three lives on the framework's power ladder** (`FountainOfYouth.top/bottom/powerFraction`, tested). The framework's power steps (20, 40, 60, 80, Gates 100)
read the health fraction. With three lives that would hand him the Gates in the first life, so `BanBoss` overrides `powerHealth()` and `powerMaxHealth()` (the pattern of
Lucius's stages) to read one virtual bar: life 1 is the top 46% of it (power 20 to 60), life 2 the next 27% (60 to 80), and life 3 sits at 24% down to 0, so **the Gates open
exactly when he rises for the last time**, and that rise is his Last Life (second model with wings, the damage and speed of the Gates, no more falls). The bar is
expressed in the units of the current life's real health so the framework's "a blow cannot skip a step" clamp cuts by the right amount. `refillOnGates` is off when
the Fountain is on (the rise already refilled him) and on when it is off.

**How he lies still.** The generic renderer has no lying-down pose and no hook for one, so the fall is shown with effects (a great tree of light, roots, a green
ring and a countdown on the action bar), the chat, the bar title ("Life 2/3 (the Fountain works)") and his emptied boss bar. To hold him still without any new
framework code, two hidden moves (`fountain_fall_1`, `fountain_fall_2`, never drawn: their power is out of reach) are played by `chooseMove` while he lies; their
recovery is the window, so the framework itself keeps him from starting a move, walking or striking (and from walking toward the party), and `strike()` returns
false while he is down as a second guard.

### B. The Snatch (`SnatchLedger`, pure)

One Snatch takes a share of one player's attack for 8 seconds: 12% at the opening, 15, 18, 21, up to 24% at the Gates (by the power stage). It is applied where it
matters: the damage that player does to Ban is multiplied by (1 minus what is taken), whatever the attack is made of, and Ban himself hits harder by 80% of what
he holds. The limits (all tested):

| limit | number |
|---|---|
| one player's taken share | at most 30% (`banSnatchCapPercent`, 5 to 60) |
| Snatches on one player | at most 2 at once; the third fizzles |
| what Ban holds from everyone together | at most 40%, so his own bonus is at most +32% |
| duration | 160 ticks; a new Snatch restarts the clock |

**The counter: take it back.** A snatched player who lands **three real blows (each at least 0.4% of his health) within 4 seconds** wrenches the strength back at once
(the thread to Ban breaks, he loses what he borrowed) and is credited with `sds.snatch_retaken`. A thread from every snatched player to Ban is drawn once a second
so the whole party sees whose strength he holds. Dodging the hands is the other counter: they are fast but drawn as lines for the whole wind-up.

**Super Concentration** is a short buff (8 seconds): speed I and +15% damage. Anti-magic breaks it ("Anti-magic breaks Ban's Concentration").

## 4. The moves (`BanTable`, numbers before the boss's damage law)

All telegraphs are at least 16 ticks (12 at the Gates) and the ultimate at least 30; the aim, the mark or the ring is locked at the start of the wind-up and drawn for the
whole of it. The staff is steel: its blows are the boss's plain physical blow (a raised shield helps); the heart he takes, the thread and the Fountain's water are typed
Tensura damage (`heart_eat`, `energy_drain`, `water_elemental`).

| move (id) | from power | wind-up / cooldown / recovery | range | weight | base damage | what it is | counter |
|---|---|---|---|---|---|---|---|
| Courechouse Combo (`courechouse_combo`) | 20 | 16 / 60 / 8 | 0 to 4.8 | 6 | 7 x 3 | three cuts 6 ticks apart in a cone of 104 degrees, reach 4.2 | leave the cone, or get behind him |
| Courechouse Sweep (`courechouse_sweep`) | 20 | 16 / 130 / 10 | 0 to 5.6 | 3 (crowd) | 10 | a full circle, radius 5 | back away |
| Courechouse: Extend (`courechouse_extend`) | 20 | 16 / 100 / 8 | 4 to 12.5 | 5 | 11 | the four sections run out: a strip 1.8 wide, 11 long | step out of the strip |
| Snatch (`snatch`) | 20 | 16 / 190 / 8 | 4 to 18 | 5 | 5 + the steal | one fast hand along the line drawn (2.2 blocks a tick, 12 ticks) | sidestep; then 3 blows back |
| Fox Step (`fox_step`) | 20 | 16 / 150 / 8 | 6 to 26 | 4 | 12 | through the strip drawn and out the far side, ending behind the target | step out of the strip |
| Hunter Fest (`hunter_fest`) | 40 | 20 / 280 / 12 | 3 to 15 | 3 | 18 | lands on the marked ring (radius 2.4) and takes the heart; a Snatch for each foe hit | leave the ring |
| Super Concentration (`super_concentration`) | 40 | 20 / 520 / 12 | any | 1 | none | the buff above | anti-magic breaks it |
| Snatch Barrage (`snatch_barrage`) | 60 | 22 / 330 / 12 | 4 to 22 | 3 (crowd) | 5 each | a hand at each of up to 3 foes (a fan of three when alone) | sidestep the lines |
| Fountain Geyser (`fountain_geyser`) | 80 | 22 / 260 / 12 | 4 to 32 | 3 (crowd) | 13 | 5 to 6 geysers of radius 2.4 on the marks, thrown 2 blocks up (no fall damage); shield or anti-magic cuts it | leave the rings |
| Fox's Hunt (`foxs_hunt`, ultimate) | 60 | 58 / 1300 / 60 | 0 to 30 | 5 (crowd) | 14 each | 4 (5 at the Gates) dashes, 11 ticks apart, round a closed circuit of 3 marks; every foe cut is offered to the Snatch | stay off the lines; he is exposed after |

## 5. Config and quest keys

`[sdsa]` in `nusmp-arc-sdsa-server.toml`: `banFountain` true, `banFountainLives` 3 (1 to 3; 1 means he never rises), `banSnatchCapPercent` 30 (5 to 60). Quest hook keys:
`sds.fountain_cut` (the player's blow cut the Fountain during a window), `sds.snatch_retaken` (the player wrenched their strength back).

## 6. The model (art kit, `tools/arc/char_ban.py`)

A very tall, lean man (2.12 blocks), pale spiky hair, scarlet eyes, a scar on the cheek and stubble, an open maroon jacket with a fur collar over a bare chest, fingerless
gloves, a double belt with pouches, a relaxed idle. Courechouse is a prop of this character's own (the kit's staff is a mage's staff with a crystal): four iron sections joined by
gold rings and glowing green links, a steel point on top. The second form is his Last Life: a darker, torn jacket, green lines over the face, shining eyes, and a pair of
fairy wings of light (an addition for the game: Elaine, whom he lost, is a fairy). The scar, the stubble, the colours of the clothes, the green glow and the wings are for the
game and not from a source. ArcLook (the bigger form, from the manifest): `("ban", 2.12, 1.31, 2.33, 0.61, 1.37, 1.45, true)`, hitbox 0.62 x 2.12; both forms are the
same size. Preview PNGs (looked at, kept outside the repository in the job's scratchpad folder `previews/sdsa/`): `arc_ban_front.png`, `_idle`, `_walk`, `_attack`,
`_cast_big`, `_transform`, `_death`, `arc_ban_form2_front.png`, `_form2_idle`, `_form2_attack`, `_form2_cast_big`.

## 7. The Charter Grimoire (chance 0.30)

Eight real Tensura skills: `greed` and `envy` (the taking), `sticky_steel_thread` (the thread of the Snatch), `ultraspeed_regeneration` and `self_regeneration` and
`poison_nullification` (what the Fountain gave him), `instant_move` (the fox's speed), `strengthen_body`.

## 8. The weapon he would give (NOT built)

**Courechouse**, his Sacred Treasure: a real 3D model in hand (four sections, glowing green links, it extends) with the Tensura engravings `swift` 2, `energy_steal` 2 (the
Snatch), `vitality` 1 and `restoration` 1 (the Fountain), to be added to `item/WeaponEngravings.java`.

## 9. Tests and what is NOT verified

Pure and tested: `FountainOfYouthTest` (the lives, the windows, the cut, the ladder across the lives, the save and load, nonsense input), `SnatchLedgerTest` (every cap, the
retake, the clock), `BanTableTest` (through the real `MoveScheduler`); all mutants caught. `BanBoss`, `BanMoves` type-check against the stubs (only the known artifacts remain).

Not verified: nothing has run in a game. The riskiest part is the glue of the Fountain, because it intercepts a fatal blow (the same door Lucius's stages use):
(1) that `die()` is reached for every fatal blow and the 1 health left is not taken by the game (Lucius's stages rely on the same); (2) that the two hidden moves really hold him
still (they are only ever started by `chooseMove`; if a fall happens with no target he simply stands there until the window ends); (3) the units of the virtual bar when the
reservoir shrinks (the clamp that stops a blow from skipping a power step is in the real health of the current life); (4) the look of the fall (effects only); (5) all the numbers
(the damage goes through the balance law, 3 blows of 0.4% within 4 seconds may be too easy or too hard for a party); (6) the look of the effects (shapes borrowed from other
characters, tinted: `TREE_CANOPY`, `TREE_ROOTS`, `TREE_DRAIN`, `THREAD_LINE`, `DARK_THRUST`, `DARK_IAI`; unseen); (7) the move names that are mine (Fox Step, Snatch Barrage, Fountain
Geyser, and the three Courechouse moves).
