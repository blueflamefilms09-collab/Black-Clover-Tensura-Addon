# The Mob Foundry (400 dungeon mobs)

Why: the owner asked "where are the custom 400 different entities, 200 for each dungeon type?". Until this work the Mega-Dungeon filled its floors from DATA tables that name
vanilla, Tensura and Ice-and-Fire mobs (about 85 ids). The Foundry adds 400 mobs that exist only in this mod: **200 for the Holy road, 200 for the Abyssal road, 40 per tier per road**
(tier t = floors 200(t-1)+1 .. 200t, `DndRules.tierOf`; the dungeon has five tiers of 200 floors), each one a character of the Arc art kit (`docs/arc/art_kit.md`) on the player rig,
spawned by the Dungeon Master next to the old encounter tables, which stay exactly as they were.

Verification report with the real numbers (generated, do not edit): `docs/labyrinth/foundry_report.txt`.

## 1. The roster

| tier | floors | Holy road (the climb) | Abyssal road (the descent) |
|---|---|---|---|
| 1 | 1-200 | the Dawn Marches: squires, sprites, kobolds, scarecrows, forest beasts, apprentice mages, slimelings, guild cadets | gutter imps, ghouls, hollow squires, candle cultists, goblin raiders, rat folk, slimes, cave spiders |
| 2 | 201-400 | warbands and courts: centaur lancers, satyrs, griffon scouts, pixie court, unicorn maidens, shinigami and breath-corps slayers, class heroes | orc warbands, hags, zombies, vampire spawn, drow riders, gnoll packs, hell hound handlers, mimics, mind flayer thralls |
| 3 | 401-600 | the citadels: paladins, devas, sphinxes, giants, gold saints, forgehold constructs, geode and feywild courts, sky knights | trolls, wraiths, mind flayers, devils (pit fiend, barbed, bone), banshees, ghoul kings, lamias, hag mothers |
| 4 | 601-800 | the archons: planetars, gold and silver dragonkin, class servants, spirit guardians, holy knights, titans, chimeras | balors, liches, death knights, aboleths, beholders, red and black dragons, vampire lords, cursed spirits |
| 5 | 801-1000 | the unsetting: solars, empyreans, ancient gold dragon, archangels, demigods, sage kings, phoenixes, saint marshals | archdevils, demon lords, the tarrasque, death tyrants, monarchs, evil gods, void horrors, elder dragons |

* **Factions** (the eleven of the Mega-Dungeon contract, decide who a mob fights in a turf war and what it leaves): undead 39, beast 72, construct 56, cultist 88, demon 34, elemental 42,
  aquatic 21, insect 14, ogre 15, slime 13, goblin 6.
* **Archetypes** (stats and movement): brute 62, caster 68, assassin 53, tank 49, support 44, summoner 37, charger 29, flier 24, skirmisher 23, archer 11.
* **Sources** (the series or D&D monster the DESIGN is inspired by; names and looks are the mod's own, no copied character or text): 194 different. 45 anime and game series (Berserk 16,
  Frieren 15, Fate 12, Made in Abyss, Dark Souls and Overlord 11 each, then Princess Mononoke, Re:Zero, Attack on Titan, Dragon Quest, Fire Force, Fairy Tail, Black Clover, Dungeon Meshi, Tensura,
  Fullmetal Alchemist, Konosuba, Record of Ragnarok, One Piece, Jujutsu Kaisen, Bleach, Magi, Elden Ring, Saint Seiya, Solo Leveling, Monster Hunter, Goblin Slayer, Naruto, Hunter x Hunter,
  Mushoku Tensei, Seven Deadly Sins, Zelda, Bloodborne, Final Fantasy, Spirited Away, Demon Slayer, Shield Hero, Sword Art Online, Log Horizon, Soul Eater, Akame ga Kill, Tower of God,
  My Hero Academia, DanMachi, Chainsaw Man) and 149 D&D monsters or classes (kobold, gnoll, mind flayer, drow, revenant, gargoyle, mimic, hag, beholder, lich, ...; each D&D
  monster counts as its own source, e.g. `D&D: gargoyle` has 5 mobs). The largest single source is 16 mobs = 4 percent of the roster (the guard allows 8 percent). Counted as ONE family,
  D&D is 178 mobs (44 percent): if the owner wants D&D read as a single source, about 70 rows would have to change series; the roster files make that a text edit.
* **No two look alike.** The guard compares the 16 structure groups of every pair (build, hair, face, top, legs, boots and gloves, trim, cape, armour, wings, tail, horns, ears, halo,
  accessories, props): the closest pair of the 400 still differs in 3 groups (38 pairs at 3, 109 at 4, 332 at 5), every palette triple is unique, and no two mobs share an ability set with a
  palette. Names and ids are unique.

## 2. How a mob is made

A mob is **one line** in `tools/foundry/rows/<road>_t<tier>*.txt` (read by `roster_holy.py`, `roster_abyssal.py`), eight fields separated by ` | `:

```
Name | faction | build | palette | source | look tokens | archetype | abilities
Tunnel Kobold | beast | small | rust sand iron | D&D: kobold | skin=red ears=pointed horns=small tail=lizard hair=crop/ember face=grin/amber top=vest/-/leather legs=shorts boots=bare prop=dagger | skirmisher | dash swarm_call
```

* `palette`: three colour names (`tools/foundry/palette.py`, about 130 names): the cloth, the second colour (leather, cape, hair) and the accent (trim, glow). Every other colour is derived.
* look tokens (`key=value`, parts separated by `/`, `-` = the kit's default): `skin hair=style/colour face=expression/eyecolour/marks mask top=kind/length/material/pattern/emblem legs boots gloves belt neck
  inner sleeves collar bandage cape=kind/ragged/hood armor=pieces/style/colour wings tail horns ears halo acc=a+b prop off back idle atk cast glow size` and flags `eglow wglow tglow hglow pglow
  aglow ragged`. Every name is the art kit's (docs/arc/art_kit.md section 4): a typo is reported with the list of valid names, for all rows at once.
* `size` scales the model (and the hitbox) 0.6 to 2.0; `glow` colours the glow layer.

**To add a mob:** write one row in the file of its road and tier (use a new file, e.g. `holy_t3d.txt`, for a batch), run `python3 -B tools/foundry/gen_foundry.py --partial` (the 40-per-tier rule is
relaxed while a batch is unfinished), read what the guard says (a duplicate name, palette, look or ability set fails the run and names the pair), `--preview <id> front idle attack` to look at it
(JDK 21, numpy, Pillow), commit the generated files. To change what a mob does, change its archetype or abilities in the row; to add an ability, add a row to
`src/main/resources/data/nusmp/mega/foundry/_abilities.json` over one of the 25 kinds (or write a new kind in `FoundryAbilityRunner`).

Commands of the generator: `gen_foundry.py` (everything), `--only a b` (some mobs), `--verify` (guard only), `--partial`, `--measure N` (how far the shared clips are from the mob's own),
`--check` (rebuild into a temp folder, fail if a committed file differs), `--preview id [scene ...]`. `python3 -B tools/foundry/test_guard.py` tests the guard itself.

## 3. What the generator writes, and what it costs

| file | what |
|---|---|
| `assets/nusmp/geo/entity/foundry_<id>.geo.json` | the mob's own geometry (kit model, minified): 20 KB average, 61 cubes average, 126 at most |
| `assets/nusmp/textures/entity/foundry_<id>.png`, `_glow.png` | 8-bit palette PNGs (the kit's textures hold well under 256 colours, so this is lossless): 4.9 KB and 1.1 KB average |
| `assets/nusmp/animations/entity/foundry_anim_<class>.animation.json` | the clips of an **animation class**, shared by all mobs of the class (107 classes for 400 mobs) |
| `assets/nusmp/foundry/index.json` | what the client needs (animation class, glow colour, size, tier, height): a client never sees data packs |
| `data/nusmp/mega/foundry/<id>.json` | the server definition: road, tier, faction, archetype, abilities, size, height, source |

**Sizes.** One Arc character is about 310 KB uncompressed (217 KB of animation). Four hundred of those would be 84.8 MB uncompressed (about 10.9 MB in the jar). The foundry's 400 mobs are
17.4 MB uncompressed (geo 8.1, textures 2.4, shared animations 7.0) and **4.58 MB in the jar** (geo 1.26, textures 1.88, animations 1.55). How the saving works: the clips of a kit character depend
only on its build, its held props and its clip styles (plus which follow-through bones exist), so a mob does not own an animation file; it names a class `build__right-left__attack-cast-idle`
(for example `broad__sword-shield__auto-auto-auto`). The class's clips are generated from a canonical character of that build and props dressed with a SUPERSET of the follow-through bones (cape,
hair, tails, wings, ears, hat, ribbon ...), so every member finds its bones; bones the member lacks are ignored by the renderer (`GeoDraw` skips animation tracks of missing bones). The kit's
`transform` clip is dropped, and the tracks are thinned (Ramer-Douglas-Peucker, 0.5 degree) on top of the kit's own thinning. `--measure N` builds a mob's own clips and compares: on the 16 sampled
mobs the arms, head, body and legs differ by at most 0.5 degrees (the same build and prop), and the worst bone is the swing of the coat's skirt panels or a ribbon in the run clip (11 to 35
degrees on a bone the mob may not even have). A model costs about 0.2 s to build; the full roster builds in about 80 s on four cores and the 107 class animations in about 70 s (cached by a
hash of the kit sources and the class, so a roster edit only rebuilds what changed).

## 4. In the game

| piece | where | what |
|---|---|---|
| entity | `labyrinth/mega/foundry/FoundryMobEntity` | ONE type `nusmp:foundry_mob`; identity is a synced string (pattern of `arc/ArcNpcEntity`); `Enemy`; tag `nusmp_dm` when the Director or the admin command makes it |
| stats | `FoundryScaling`, `FoundryArchetype` | tier base (health 24, 40, 66, 110, 180; damage 3, 5, 8, 12, 18; armour 0 to 14) x growth inside the tier (at most +40 percent health, +30 percent damage by floor) x the archetype; the last floor of tier t is weaker than the first of tier t+1 (tested for all 10 archetypes); the Dungeon Master's threat scaling (up to +300 percent) is applied on top, as to every monster it spawns |
| AI | `FoundryGoals` | six style goals that all exist on every mob and each switch themselves on for their style: melee, hit-and-run, ranged (a band of distance), support (stays by the allies), charge (telegraph and ram), air (hover and swoop); plus the ability goal and the turf-war rival goal |
| abilities | `FoundryAbility` (pure rules), `FoundryBrain` (pure chooser), `FoundryAbilityRunner` (world adapter) | 53 abilities over 25 kinds (dash, leap, guard, volley, heal, summon, drain, root, blink, enrage, aura, slam, bolt, cleave, curse, ward, spit, pull, bash, rally, regen, thorns, mark, dive, burst); a cast stands through its wind-up (the cast clip plays, a Rukh gathers), then fires; ONLY Light or Dark Rukh particles (`RukhFx`) and sounds are shown |
| summons | `FoundryAbilityRunner.summon` | a summoner calls lower-tier mobs of its own road and faction (cap `minionCap`); minions carry no tag, drop nothing and give no experience |
| drops | `FoundryDrops` and the standard listeners | the economy (Magic Stones, meat) and the gear roll already pay every `Enemy` or tagged monster, so a foundry mob drops Magic Stones and a chance of gear (`gear` package) with no new code; `FoundryDrops` adds the faction cut and the threat-scaled stone bonus by the instance's faction (`FactionLoot`) and silences minions |
| turf wars | `FoundryGoals.RivalGoal` | with no player within 12 blocks a mob goes for the nearest mob of a rival faction (`FactionRegistry.rivals`); the eco package is not edited (its membership is per entity TYPE, and there is one type) |
| Dungeon Master | `FoundryEncounters` + four one-line calls in `EncounterDirector` and `EncounterSpawner` | each monster of a resolved encounter entry becomes, with the config weight (default 60 percent), a foundry mob of the FLOOR's tier and the sector's ROAD (Holy sectors give Holy mobs, Abyssal sectors Abyssal ones), preferring the faction the room or entry asked for (x4) and the factions the region's theme likes (`themeWeights`); the cost is the mob's health in weak-monster equivalents, so the budget still limits the headcount; everything else (40 percent of spawns) comes from the old tables unchanged |
| client | `client/labyrinth/foundry/FoundryRenderer`, `FoundryIndex`, `FoundryClient` | ONE renderer: `ZogratisPose` clock, `GeoDraw`, the arc stride maths; the `GeoSpec` is built from the mob's own model and texture and the CLASS's animation file (a record, no engine change); glow brightens with the tier |
| config | `FoundryConfig`: `nusmp-labyrinth-mega-foundry-server.toml` | `enabled`, `weight` (0-100), `abilities`, `factionWars`, `warScanTicks`, `extraDrops`, `minionCap`, `healthMultiplier`, `damageMultiplier` |
| command | `/multiverse mega foundry` | `list [holy\|abyssal [tier]]`, `spawn <id>`, `count`, `verify` (200 + 200, 40 per tier, sources, abilities); permission 2 |

## 5. Tests

* JUnit (pure, run with the local shim): `FoundryRulesTest` (scaling monotone in tier for every archetype and across tier borders, caps, the ability rules and every kind's situation, the brain, the
  roster draw, minions), `FoundryRosterTest` (reads the shipped data: 400 files parse clean, 200 + 200, 40 per tier, unique names, 30+ sources, none above 8 percent, a source on every row, every
  ability and archetype exists, every mob has its model, textures, index entry and animation class file, the clips are shared), `FoundryEncountersTest`, `FoundryIndexTest`. 23 tests.
* Python: `tools/foundry/test_guard.py` (the guard catches a duplicate look, palette, name, ability set, an empty source, a bad tier, too few sources, a source above 8 percent; the real roster is clean).
* In game (`/multiverse mega selftest`, keys `foundry.*`, expected by the CI smoke test with the prefix `foundry`): `foundry.roster`, `foundry.scaling`, `foundry.ai` (a mob of every archetype builds its
  goals and has the goal of its style), `foundry.spawn` (one mob of each tier on each road is made in the dungeon level with its identity, road and tier stats; the save data round trip), `foundry.encounters`.

## 6. For the lead (the only lines outside the new files)

* `MegaSystem.java`: `FoundryRegistry.init(modBus)` after the rift init, `FoundryRegistry.register(bus)` after the rift register.
* `MegaCommands.java`: `mega.then(...foundry.FoundryRegistry.command());`
* `NUSMP.java`: `registerConfig(SERVER, FoundryConfig.SPEC, "nusmp-labyrinth-mega-foundry-server.toml")` next to the rift line.
* `client/labyrinth/LabyrinthClient.java`: `FoundryClient.init(modBus);`
* `enc/EncounterDirector.java` (3 lines + an import) and `enc/EncounterSpawner.java` (1 line + an import): the Foundry glue.
* Lang keys: `tools/foundry/lang_fragment.json` (the code uses `translatableWithFallback`, so nothing is needed to run).
* CI: add the prefix `foundry` to `SMOKE_EXPECT` so the smoke test insists on the five self-tests.

Not verified in a running game (none of this could be run here): the AI and ability feel (cast timing, charge, hover), the draw of the 60 percent mix in a real sector, the shared-clip look in the
live renderer (the kit preview harness renders them correctly), the jar size (computed from the files, not from a build), and the type check was done against API stubs, not a real compile.
