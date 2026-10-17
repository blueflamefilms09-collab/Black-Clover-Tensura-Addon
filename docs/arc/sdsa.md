# The Seven Deadly Sins, part 1: Meliodas, Escanor, Ban (boss characters, tier 3)

Why this page exists: three bosses of one group were built together on one small toolbox, one config and one set of prerequisites, and what they share is written once
here. Each boss has its own page: [meliodas](meliodas.md), [escanor](escanor.md), [ban](ban.md). Nothing here has been run in a game: every number is a first guess
and every effect an unseen one (see "What is NOT verified" on each page and at the end of this one).

## The three, and the one lesson each fight teaches

The owner's note: players will have fought and acquired powers from the Seven Deadly Sins, so each fight must "transcend standard stat checks": it must answer the
tricks a strong player brings, within the framework's rules (no free healing, no Unique/Ultimate abuse, crowd control only through `BalanceLaw`, telegraphs on
everything, a damage budget). So each of them has one rule that only he has, and the party that learns it wins faster than the party that only raises numbers.

| id | who | road and floors | stats (health / attack / speed / armour) | the rule that is only his | the counter |
|----|-----|-----------------|-------------------------------------------|---------------------------|-------------|
| `meliodas` | Wrath, the Dragon's Sin | any road, 60 to 90 | 3200 / 20 / 0.36 / 14 | **Full Counter** sends the magic that hits his front back at the caster; **Assault Mode** gives up his guard; the **Wrath** nova eats magic | steel, arrows and anti-magic; from behind; enough magic to shatter it; the Assault window; steel to break the nova |
| `escanor` | Pride, the Lion's Sin | Holy road, 20 to 80 | 3000 / 24 / 0.30 / 16 | **Sunshine**: his power, form and moves follow the sun of a fight clock | fight him at night; heat resistance; break the **Cruel Sun** with a burst, cover, a shield or your feet |
| `ban` | Greed, the Fox's Sin | Abyssal road, 20 to 80 | 2400 / 16 / 0.38 / 8 | the **Fountain of Youth** (three shrinking lives, he cannot be hurt while it works) and the **Snatch** (a capped share of your attack) | cut the Fountain in the window (anti-magic, holy, soul, severing); hit him three times in four seconds to take your strength back |

All three: saga `SEVEN_DEADLY_SINS`, tier 3 (health inside the 2200 to 3200 band and under Lucius's 3600), open in any order after the four Eye of the Midnight Sun bosses
(`fana`, `patolli`, `rades_spirito`, `rhya`; ids as the first-saga builder named them; the story rules skip an id the roster does not know). Charter Grimoire chance 0.30
with eight or nine real Tensura skills each (checked against the skill list). Every boss is NEW: nothing about these characters existed in the mod before
(`ArcSaga` only mentions their names in a comment), so nothing is replaced and nothing is kept or removed.

## The fairness rules every move obeys

The tables are in pure classes and are tested for these (`*TableTest`): a telegraph that is still at least 12 ticks after the framework shortens every wind-up to 75% at the
Gates (so at least 16 on paper) and at least 30 for an ultimate (so 40 on paper; the ultimates here have 58 to 62); a recovery of at least 8 ticks after every move and at
least 40 after an ultimate (exposed: +25% damage); a cooldown of at least 60 ticks, and for an ultimate at least the framework's gap at the Gates (900); power gates only at the
five steps (20, 40, 60, 80, 100); at the opening (power 20) every distance has a move; every move is used somewhere in a fight played through the real `MoveScheduler`
(`SdsaFightSim`); and every mark can be left at a walk in the time the telegraph shows it (`DemonKingMath.walkable`, `CruelSunMath.walkable`). None of the three applies hard control (no stun, root or slow); the only forced movement is a knock-back and the geysers' lift (no fall damage), so `BalanceLaw.beginControl` is not
needed. In the game the same holds in the
glue: the aim, the mark or the ring is LOCKED at the start of the wind-up and drawn for the whole of it, and the strike uses the same geometry as the drawing.

## Files (all new; `src/main/java/com/newuniverse/nusmp/arc/sds/`)

| file | what it is | why it exists |
|------|------------|---------------|
| `SdsCharacters` (pure) | the three `ArcCharacter` records | a test can check the Charter ids, the laws, the stats, the floors and the prerequisites without a game |
| `SdsaConfig` | the config (`nusmp-arc-sdsa-server.toml`, `[sdsa]`) | one config class for the group, plain `define` / `defineInRange`, read through a `get()` that answers the default while the spec is not loaded |
| `SdsaStrikes` | the toolbox the three bosses share: geometry (cone, strip, disc), the telegraph outlines, typed damage through the boss's law, a push, a shot, a ring wave, the spread of marks, effects and sounds | the Zogratis helpers are package-private to `entity.zogratis`; written once so that a fix lands in all three, bounded everywhere, no block changes |
| `FullCounter` (pure) | the state machine of Meliodas's stance | the rule must be exact and fair, and a test must be able to prove it |
| `DemonKingMath` (pure) | the meaning of each power step for Meliodas, the images, the Hellblaze numbers, the Wrath's arithmetic | one test proves the ladder never goes backwards and the nova can be walked out of |
| `MeliodasTable` (pure), `MeliodasMoves`, `MeliodasBoss` | his move numbers, strikes and rules | |
| `SunClock`, `Sunshine`, `CruelSunMath` (pure) | the fight clock, what the sun does, the Cruel Sun and Zenith numbers | |
| `EscanorTable` (pure), `EscanorMoves`, `EscanorBoss` | his move numbers, strikes and rules | |
| `FountainOfYouth`, `SnatchLedger` (pure) | the lives and the ladder, the stolen shares | |
| `BanTable` (pure), `BanMoves`, `BanBoss` | his move numbers, strikes and rules | |

Models: `tools/arc/char_meliodas.py`, `char_escanor.py`, `char_ban.py` (art kit specs; Escanor's Rhitta and Ban's Courechouse are props of their own, registered in the spec file
as the kit's manual allows); generated assets `assets/nusmp/{geo,animations}/entity/arc_<id>[_form2].*`, `textures/entity/arc_<id>[_form2][_glow].png`, the preview scenes
in `tools/render_preview/scenes.d/`, the three entries of `tools/arc/manifest.json`. Grimoire items: `models/item/charter_grimoire_<id>[_icon].json` and the two textures per boss
(written by `tools/gen_arc_grimoire_assets.py`, which finds the three records in `SdsCharacters`).

Tests (`src/test/java/com/newuniverse/nusmp/arc/sds/`): `FullCounterTest`, `DemonKingMathTest`, `MeliodasTableTest`, `SunClockTest`, `SunshineTest`, `CruelSunMathTest`,
`EscanorTableTest`, `SnatchLedgerTest`, `FountainOfYouthTest`, `BanTableTest`, `SdsCharactersTest` and the helper `SdsaFightSim`: 94 tests, all passing in the local shim;
19 deliberate mutations of the pure classes (a changed cap, a removed rule, a moved power gate) were each caught by a test.

## Config (`nusmp-arc-sdsa-server.toml`, `[sdsa]`)

| key | default | range | what it does |
|-----|---------|-------|--------------|
| `meliodasFullCounter` | true | | false: he never raises Full Counter; the rest of the fight is unchanged |
| `escanorSunSpeed` | 3.0 | 0 to 12 | how many times faster than the game's day the fight's sun runs (1 = the world clock, 0 = it stays at the start hour) |
| `escanorSunStart` | "world" | "world", "dawn", "noon", "midnight" (anything else is "world") | where the fight's sun starts |
| `escanorGatesSunFloor` | 0.60 | 0 to 1 | the lowest sun once the Gates are open |
| `escanorHeatAura` | true | | the heat round his body |
| `banFountain` | true | | false: Ban dies like any boss (one life, the Gates refill him as for every boss) |
| `banFountainLives` | 3 | 1 to 3 | how many lives the Fountain gives |
| `banSnatchCapPercent` | 30 | 5 to 60 | the most of one player's attack a Snatch may hold |

## Quest hook keys (`NusmpEvents.fire(player, key)`; the lead wires advancements, see `tools/quests/spec_adv.py`)

`sds.full_counter_broken` (a player's spell broke Meliodas's Full Counter), `sds.nova_broken` (a player's blow broke the Wrath), `sds.cruel_sun_broken` (a player hit Escanor
in the window in which the party broke the Cruel Sun), `sds.fountain_cut` (a player's blow cut Ban's Fountain), `sds.snatch_retaken` (a player took their strength back).

## For the lead (the integration lines)

* `ArcCharacters.bootstrap()`, after the Eye of the Midnight Sun lines (it also loads the boss classes, which register their entity types):
  `ArcRoster.register(MeliodasBoss.CHARACTER);` `ArcRoster.register(EscanorBoss.CHARACTER);` `ArcRoster.register(BanBoss.CHARACTER);`
  (import `com.newuniverse.nusmp.arc.sds.MeliodasBoss`, `EscanorBoss`, `BanBoss`; each class also registers its own record when it is loaded, so a repeat is harmless).
* The config, next to the other `registerConfig` lines of `NUSMP.java`:
  `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.arc.sds.SdsaConfig.SPEC, "nusmp-arc-sdsa-server.toml");`
* The language fragment `arc/frag/sdsa.lang.json` (three entity names; everything the bosses say is plain English text, like the rest of the Zogratis fights).
* Nothing for the client: `ArcClient` registers one renderer per boss of `ArcEntities` by itself.
* `python3 tools/gen_arc_grimoire_assets.py` has been run here for these three; run it again after the merge to cover every character (it is idempotent).

## What the lead and the other builders must know

* The framework's own text for the Gates ("The Gates of the Tree of Qliphoth open!") is rewritten in each boss's voice by an override of `announce` (the same way the first-saga
  bosses do it); the other devil-voiced parts of the framework are not: every power step and the Gates still send a `DEVIL_CIRCLE` effect and the start of a fight a summoning
  circle in the character's colour (`spawnAt`), which looks wrong for a sun-god and a fox. A hook in `ArcBoss` for the effect shape would fix it for every arc boss.
* `ZogratisBoss.refreshBar()` is private, so a boss bar title that changes between power steps (Escanor's phase of the day, Ban's life) is pushed by calling
  `startSeenByPlayer` for one fighter, which rebuilds the title for everyone. Making `refreshBar` protected would be cleaner.
* The generic renderer has no lying-down pose: Ban's fall is shown with effects, not an animation. A boolean on `ArcBoss` that the renderer maps to the `death` clip held
  at its end would give it; I did not touch the shared files.
* Escanor's model changes with the sun (frail man, Pride, The One) by an instant swap with a flare, not by the transform clip, and the hitbox changes size with it (x0.88,
  x1.2, x1.42).
* `chooseMove` is overridden in all three (the framework calls it every few ticks with the scheduler's draw); the three of them call `super` after changing what the scheduler sees.

## Sources (read through search summaries only; the pages themselves could not be opened)

<https://www.looper.com/720355/meliodass-powers-from-the-seven-deadly-sins-explained/>, <https://www.looper.com/735035/escanors-powers-from-the-seven-deadly-sins-explained>,
<https://www.looper.com/796948/bans-powers-from-the-seven-deadly-sins-explained>, <https://en.wikipedia.org/wiki/Meliodas>,
<https://en.wikipedia.org/wiki/List_of_The_Seven_Deadly_Sins_characters>, <https://nanatsu-no-taizai.fandom.com/wiki/Meliodas>,
<https://all-worlds-alliance.fandom.com/wiki/Sunshine_(Seven_Deadly_Sins)>, <https://comicvine.gamespot.com/app.php/ban/4005-93744/>,
<https://myanimelist.net/featured/141/Nanatsu_No_Taizais_7_Main_Characters__Seven_Deadly_Sins>, <https://vsbattles.com/threads/sunshine-against-ice-escanor-vs-esdeath-concluded.25521/post-777203>,
<https://www.prime1studio.com/ntz-meliodas-ban-and-king/CMNTZ-01DXS.html>. Many other hits were content farms and chatbot-persona pages; they were not used. Where the
sources disagreed (Courechouse's sections, the mark's place on Meliodas's arm, the multiplier of Full Counter) the page says so.

## What is NOT verified (the group)

* Nothing has run in a game. The code type-checks against the API stubs (only the known stub artifacts remain: the abstract `MobEffectInstance`, used once by Ban's Concentration, and the missing
  `deserializeNBT` on an entity subclass) and the pure logic has passing tests; the glue (the filters, the move bodies, the zones) has been read, not run.
* The shapes of the visual effects are borrowed from other characters and tinted (the effect list is in each page); what they look like in the game is unseen. The models were
  looked at in the offline preview, not in the game.
* All damage numbers go through the balance law, so the real figures depend on the player's EP; the first smoke test will show whether any move is far too weak or too strong.
* The canon is thin (search summaries, fan wikis); the names that are mine are listed on each page.
