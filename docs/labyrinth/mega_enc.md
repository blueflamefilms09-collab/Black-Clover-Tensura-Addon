# The Dungeon Master (`mega.enc`): encounters, noise and light, wandering bosses

Builder ENC of the 1.54.0 "Singular, Expanding Mega-Dungeon". Package `com.newuniverse.nusmp.labyrinth.mega.enc`, config
`nusmp-labyrinth-mega-enc-server.toml` (section `[enc]`), data `data/nusmp/mega/encounters/*.json` and `data/nusmp/mega/wanderers.json`,
admin tree `/multiverse mega dm ...`. Nothing here has been run in a game; section 12 is the honest list of what is therefore unverified.

## 1. What it does, in one page

The Mega-Dungeon has no natural spawns. Everything that lives in it is put there by the generator (its rooms), the Sealed Archway trials, the
Guardian Altar and the Crucible, and by this system. "Vanilla spawning is too predictable" was the brief, so the Dungeon Master (DM) behaves like a
game master behind the screen:

```
every rollEveryTicks (100)            one party per tick
  players of the Mega-Dungeon  ->  PartyMath.group  ->  parties (within 48 blocks, chained)
  for a party that is exposed (not resting, not in the boss chain), off cooldown, not already facing a crowd:
      sector  = MegaDungeon.sectorAt          threat = Sector.threat, table = the theme's JSON (else default)
      noise   = NoiseEvents reading (decays)  light  = LightRules (block light + light in hand)
      d20 + threat + noise + light + party (+ table bonus)   against the DC (17)
          margin < 0 nothing | 0-1 minor | 2-3 standard | 4-5 hard | 6+ ambush   (natural 1 quiet, natural 20 one band worse)
      EncounterRoll.choose  -> an entry of that band that fits threat, party size and the party's needs (dark / lit / noisy)
      EncounterRoll.plan    -> a headcount within the budget  base * band * (1 + 0.25 (threat - 1)) * party factor
      SpawnPicker + EncounterSpawner -> cells 24-48 blocks away, out of sight, never in a safe room, in the sector
      EncounterCue (a sound from the monsters' side, a little dust)  +  the entry's message, if it has one
      the monsters hunt the party for a while (the band's hunt time), then fend for themselves; a sweep removes the strays

every 200 ticks per sector with players   WandererDirector: maybe a wandering boss (the roster), far from the party, out of its sight
every 10 ticks per wandering boss         WandererMind: perceive (sight, hearing) -> PATROL / INVESTIGATE / CHASE / SEARCH on the room graph
```

Everything is data-driven where the owner will want to tune it (tables, roster, config) and pure where it can be tested without a server
(the dice, the noise, the light, the party, the picker, the perception, the patrol brain).

## 2. Why each design decision

* **Why a d20 and bands, not a spawn timer.** A timer is predictable, and predictability was the complaint. A d20 with small capped modifiers is the
  oldest tool of the tabletop: the modifiers are the whole design (a deeper sector, a louder party, a brighter party and a bigger party are met more
  often) and the die still matters. Every modifier is a small integer with its own cap (threat 5, noise 4, party 2, light 3, table 3), so no single
  factor can drown the others. The band then decides how much trouble, through a budget, so one roll answers both "whether" and "how bad".
  A natural 1 is always quiet (a lucky party gets a breather whatever it did) and a natural 20 is one band worse (and turns a miss into a minor encounter).
* **Why a budget in "weak-monster equivalents".** A 100-health Ravager and five zombies are the same fight on paper; the budget (a 20-health monster
  costs 1, a 100-health brute 5) lets one table entry mean the same thing for a solo player and a party of four, and makes "the first monster of an
  entry never costs more than its band can pay" a checkable property (it is: see `EncounterDataTest`).
* **Why noise decays.** A level that only rose would end every long expedition in a siege; one that reset after every roll would ignore the fight that
  ended three seconds ago. So it is an exponentially decaying accumulator per player (`noiseDecay`, a fifth a second by default), fed by vanilla game
  events (footsteps, doors, blocks, shots, blasts: the same stream a sculk sensor listens to) and one direct hook for damage, faded by distance, and
  the party's reading is the maximum of its members. Footsteps and doors count only when a PLAYER made them.
* **Why light matters.** The gloom clock already punishes standing in the dark with nothing lit; the DM answers the other half of the bargain. A bright
  party (block light at the eyes, or a light in either hand) is found more often (+3 at most), a dark one less (-3), and the tables have entries that
  need the dark (a stalker hunts what cannot see it) and entries that need light (moths and vexes come to it).
* **Why parties.** One roll per group of players who stand together (a chain of players each within 48 blocks of the next), not one per player: four
  players side by side must not be hit by four ambushes, and a scout far from the others is a party of his own with his own noise and cooldown.
  The party size scales the budget through a table (1.0, 1.6, 2.1, 2.5, then +0.3 each) that grows more slowly than the head count.
* **Why toughness windows and tags for factions.** "A goblin patrol" must mean "some goblin-faction monsters" and the faction is an entity-type TAG
  (`nusmp:mega/<faction>`, written by the ecology builder) so every mod that adds a goblin-ish creature can join it. Tags mix 20-health and 400-health
  members, so a member is drawn only when its default maximum health lies between 12 percent of a cap and the cap, where the cap is `12 + 14 * threat`
  (26 in sector one, 82 in sector five, 152 in sector ten): "deeper is harder" and "sector one is survivable" stay true without per-sector monster lists.
  When a room has a native faction (`Room.faction`) the faction monsters of an entry that names none come from it; the table's `defaultFaction` fills the gaps.
* **Why the monsters hunt.** A monster that merely appears forty blocks away and wanders off is not an encounter. For the band's hunt time the Director
  walks the monsters to the nearest exposed player once a second and aims them when they are close; after that they are on their own and the sweep
  removes the ones nobody is near.
* **Why out of sight, in the sector, never in a safe room.** An encounter that pops into existence in front of the party looks like a bug; one that
  appears a hundred blocks away never happens; one that appears in the room where the party rests breaks the dungeon's own rule. The picker is the rule
  a game master uses behind the screen: 24 to 48 blocks (an entry may narrow it), no line of sight from any member (a ray cast that loads no chunk), not
  in a Sanctuary room, not two monsters on one cell.
* **Why perception-driven chase.** A boss with its own target goals would pick the nearest player the moment he came into its follow range, through a
  wall, in the dark, crouching. So the mind decides who may be hunted (`WandererMind.approvedTarget`), a guard cancels every other attempt to target a
  player, and the mind sees a player only with a line of sight inside a range that shrinks when he crouches (half), stands in the dark (0.6) or stands
  still (0.8): hiding works. Loud events turn its head (INVESTIGATE), a player who gets out of sight for `loseTicks` is lost (SEARCH, then PATROL), and a
  player who reaches a safe room ends the chase at the door.
* **Why room-graph patrol.** A boss that walks to random points cuts through walls in the planner's mind and stalls against them in the world. The sector
  already saves its rooms and the doorways between them (`Sector.edges`, loops included), so the boss walks ROOM to ROOM along the doorways, to the room's
  PATROL marker (else its ROOM_CENTER marker, else its middle), preferring rooms it has not visited lately, never entering a safe room and never ending
  a chase inside one.

## 3. Classes

Pure classes (no `net.minecraft` import, JUnit tested) first, then the game glue. Every class has a `Why:` Javadoc; this is the short form.

| Class | Why it exists |
|---|---|
| `PartyMath` | Group players into parties (union-find over "within 48 blocks"), the party size factor and the party's d20 modifier. |
| `NoiseKind` | The numbers of the noise table in one readable place: amount, earshot, whether a boss hears it as a burst, whether only a player makes it. |
| `NoiseMeter` | One player's decaying noise level: add with distance falloff, exponential decay, the reading as a d20 modifier. |
| `LightRules` | Block light plus light in hand to a d20 modifier (-3..+3) and the booleans `dark` and `lit`. |
| `Band` | The five sizes of trouble (nothing, minor, standard, hard, ambush), their budget factors and hunt times. |
| `CountRange` | "2-4" as a count, parsed once, rolled, scaled by the party factor, never empty when it asked for one. |
| `EncounterTable`, `EncounterTableParser` | The parsed form of a table file, and the strict, loud parser (every problem is a line of text naming the file and the entry). |
| `EncounterRoll` | The dice: the modifiers, the d20 against the DC, the bands, the budget, the cooldown, the choice of an entry, the headcount. |
| `MobPicker` | Draw a faction member by toughness window (`12 + 14 * threat` cap, 12 percent floor, relaxation so a thin tag still gives something). |
| `SpawnPicker` | Choose cells 24-48 blocks away, out of sight, not safe, apart from each other, with a cap on ray casts. |
| `PatrolGraph` | The rooms and doorways of a sector as a graph: next-room choice, nearest room, far spawn room, rooms next to a forbidden room. |
| `WandererRules` | Perception: sight range and its modifiers, close range, hearing, the chance that a wanderer appears. |
| `WandererMind` | The patrol / investigate / chase / search state machine, with stuck detection; one order per pass. |
| `WandererRoster` | The parsed `wanderers.json` and the weighted draw of a kind by threat. |
| `ThreatScale` | Health and damage multipliers per threat level above one, capped. |
| `BurstRing` | A small ring of the loud events of the last seconds (where, when, how loud) for the bosses' ears. |
| `Sanctuary` | The one rule, shared by the DM and the bosses, of which rooms are left alone (rest rooms and the boss chain). |
| `EncConfig` | The server config, plain `define` / `defineInRange`, with the default-while-unloaded reader. |
| `EncData` | The snapshots of the tables and the roster and the reload listener that replaces them (never crashes on a bad file). |
| `Bestiary` | Entity ids and faction tags to entity types, with toughness, never assuming an optional mod or a filled tag. |
| `NoiseEvents` | Vanilla game events and the damage event to noise, only in the Mega-Dungeon, no allocation per event. |
| `CellSource` | Candidate cells of a sector: its SPAWN markers first, topped up with floors of ordinary rooms. |
| `LevelSight` | The picker's line of sight: a ray cast that treats an unloaded chunk as "seen" and loads nothing. |
| `MobSteer` | The polite way to walk, aim and release both goal-driven and brain-driven mobs (the repo's own recipe, in one place). |
| `EncounterSpawner` | One monster, placed safely (floor, fluids, collisions, distance, sight), finalized as an EVENT spawn, stamped and scaled. |
| `EncounterCue` | The telegraph: a sound from the monsters' side and a little dust; the optional action-bar message. |
| `EncounterDirector` | The tick, the party queue, the roll, the hunts, the sweep, the housekeeping and the status for the admin. |
| `Wanderer`, `GoalAccess`, `PatrolGoal` | The runtime record of a boss, the reflection that adds a goal to any mob, and the goal that keeps the walk alive. |
| `WandererDirector` | Spawning wandering bosses, the half-second pass (perceive, think, act), the sanctuary guard, the quest hook, adoption after a restart. |
| `WandererGuard` | Three world rules as event hooks: only the mind picks targets, no healing in combat, no Spire key from the halls. |
| `EncCommand` | The admin tree `/multiverse mega dm` with `status`, `roll`, `spawn`, `wanderer` and `clear`. |
| `EncSelfTests` | The in-game self-tests the CI smoke test runs. |
| `EncRegistry` | `init` (self-tests) and `register` (the game-bus listeners and the reload listener). |

## 4. Data: the encounter tables

Files: `data/nusmp/mega/encounters/default.json` and one per theme: `gravity blood spatial flame frost verdant storm shadow astral`. A theme file
REPLACES the default for sectors of that theme; the default answers for a theme without a file. A datapack overrides a file by shipping the same path;
the highest-priority pack wins. Only the `nusmp` namespace is read. The tables reload with `/reload`.

```json
{
  "comment": "Why: ...",                  // free text, ignored by the game, required by the tests (it must start with "Why:")
  "description": "...",                   // one line for the admin
  "rollBonus": 0,                         // -3..3, added to the d20 of the theme's sectors (a hunting ground: +1; a quiet theme: -1)
  "budgetScale": 1.0,                     // 0.25..4, multiplies the budget (bigger or smaller fights)
  "defaultFaction": "undead",             // one of the eleven; fills faction monsters that name none
  "entries": [ { ... }, ... ]             // 8 to 14 in the shipped files (at most 64 are read)
}
```

An entry:

| Key | Default | Meaning |
|---|---|---|
| `id` | required | `a-z 0-9 _`, 1-48 characters, unique in the file (the shipped files keep it unique across all files; the admin spawns by id). |
| `band` | required | `minor`, `standard`, `hard` or `ambush`. |
| `weight` | 10 | 1..1000, the chance among the entries of the same band that fit. |
| `minThreat`, `maxThreat` | 1, none | The sector threat window. An entry with no `maxThreat` is evergreen. |
| `faction` | none | One of `undead goblin ogre beast slime construct cultist demon elemental aquatic insect`: the faction of the faction monsters of this entry. |
| `mobs` | required | 1 to 6 kinds of monster, in order of importance (the first is what the entry is about and is never trimmed below one; later ones are trimmed first when the budget is short). |
| `needs` | none | `{"dark": true/false, "lit": ..., "noisy": ...}`; absent = does not care. `dark` is block light 4 or less and no light in hand; `lit` is 13 or more (a light in hand counts as 13); `noisy` is a noise reading at or above `noisyAt`. |
| `scaleWithParty` | true | Multiply the counts by the party factor. Lone hunters set it to false. |
| `minParty` | 1 | The smallest party the entry is offered to (the late ambushes ask for two). |
| `range` | config | `[min, max]` blocks from the nearest member, inside 8..96 and at least 4 wide (a wall ambush uses 10-22, archers 16-32). |
| `cells` | 2 | 1..4, over how many different places the group is split. |
| `hunt` | the band's | Ticks the monsters go looking for the party; 0 = they stay where they appear (shulker turrets); max 12000. |
| `cue` | the band's | `{"sound": "<id>", "volume": 0.7, "pitch": 0.8, "particle": "<simple particle id>", "count": 12}`; any part may be missing. A cue with no sound plays only dust. |
| `message` | none | A translation key shown on the action bar of the party. The shipped keys are `message.nusmp.mega.enc.<theme>.<entry id>`. |
| `comment` | none | Free text; the shipped files say WHY of every entry. |

A monster is `{"type": "entity", "id": "minecraft:zombie", "count": "2-4"}` (a fixed body) or `{"type": "faction", "count": "3-5"}` with an optional
`"faction"` (one member of the faction tag, chosen per encounter, all `count` of that same member). `count` is a number or a range, 0..32. A faction
monster with no faction on the monster, on the entry or on the table is an error (it could never be drawn), and the entry is dropped.

How the faction of a faction monster is found: the monster's `faction`, else the entry's, else the NATIVE faction of the room the first cell lies in
(`Room.faction`), else the table's `defaultFaction`. An empty tag, or a tag with no member inside the toughness window, makes the entry unusable for that
sector and the roll falls back to the next band down.

The parser reports, for every file, each unknown key, each wrong type, each out-of-range number (clamped), each bad id or faction or band, each
duplicate id; an entry with a meaningless core (no id, a bad band, no monsters) is dropped, the rest of the file survives, and every problem goes to the
log as `[nusmp] <file>: <what>`. The shipped files must parse with zero problems (`EncounterDataTest`).

### What the shipped tables are made of

136 entries in ten files. Every file has minor, standard, hard and ambush entries and an evergreen entry in each band, so at any threat every band has an
answer. At threat 1 only minor and standard entries are on offer, so the first sector is never ambushed by anything harder than a fair fight. The recurring
shapes, each with its own flavour per theme:

* **Scouts and nuisances (minor).** One or two of the room's own people; a few cheap crawlers or bats out of the walls.
* **Patrols and packs (standard).** A group of the faction, a pack of beasts arriving together, a caster pair (casters first, escorts trimmed first).
* **The walls are alive (standard).** Silverfish, spiders or endermites close by (range 10-22, three cells) with a short hunt.
* **The lone hunter (standard / hard, needs dark).** One tough stalker for a party that walks without light.
* **Light-eaters (standard, needs lit).** Moths, vexes or phantoms that come to a lit party.
* **The loud brute (hard, needs noisy).** One heavy body (a Ravager, an iron golem, a giant bear, a piglin brute) for a party that fights and sprints.
* **The mimic-style surprise (hard).** A Living Armor (or an iron golem "statue") that appears a few steps behind a corner and gets up when the party passes.
* **Growing bands.** `warband`, `hive_guard`, `blizzard_pack` ... from threat 3-5, and late ambushes (threat 5-7) that need a party of two.
* **Waves (ambush).** Many bodies in three places at once, with a message as the only warning.

Theme leanings: gravity (constructs, ogres, bats from the ceiling, the Ravager), blood (undead, zoglins, demons and cultists; a hunting ground, roll bonus +1,
budget 1.1), spatial (endermites, vexes, endermen, shulker turrets, illusioners), flame (magma cubes, blazes, wither skeletons, piglin brutes, hell moths),
frost (strays, wolves, bears; quiet, roll bonus -1, budget 1.1), verdant (insects, slimes, beasts; the swarm theme, budget 1.2), storm (constructs, breezes,
vexes, creepers, an iron golem), shadow (the dark theme: most dark-gated entries, roll bonus +1), astral (cultists, vexes, phantoms, shulkers; rare and
sharp, roll bonus -1, budget 1.2).

## 5. Data: the roster of wandering bosses

`data/nusmp/mega/wanderers.json`:

```json
{ "comment": "Why: ...",
  "entries": [ {"entity": "nusmp:floor_warden", "weight": 5, "minThreat": 1, "maxThreat": 100000, "name": "The Warden",
                "healthScale": 1.0, "speed": 0.9, "comment": "..."} ] }
```

`entity` is a registry id; `weight` 1..1000 (default 5); `minThreat` / `maxThreat` the sector window; `name` is the custom name the boss carries (never
shown above its head: `setCustomNameVisible(false)`; the admin sees it); `healthScale` 0.25..6 multiplies its maximum health; `speed` 0.4..1.6 multiplies
its movement speed. An id that no installed mod registers is never drawn, so the list may name optional mods. The file ships fifteen kinds: the Floor Warden
(weight 5, from threat 1), a Ravager, Tensura's horned bear, giant bear, blade tiger, war gnome, armorsaurus, black spider, knight spider and basilisk,
a Living Armor at four times its health, a piglin brute, a wither skeleton at four times its health, a zoglin and an iron golem. All are physical bodies:
the Dungeon Master cannot control the skills a Tensura mob has, so the roster favours bodies that hit rather than cast. The Grimoire Daemons are not on it
on purpose: they belong to one boss and vanish with it.

A wandering boss obeys the boss rules: this system adds no drop of its own and never a relic (relics come only from chests, Mimics and the Crucible), the
Shadow Key the Floor Warden drops by code is removed from its drops, no heal reaches it while it was struck in the last ten seconds, and it shows no boss
bar for anyone (a bar would be a radar). Systems that key on the entity TYPE still see a Floor Warden when a wandering one dies; section 12 lists them.

## 6. Config: `nusmp-labyrinth-mega-enc-server.toml`, section `[enc]`

| Key | Default | Range | Meaning |
|---|---|---|---|
| `enabled` | true | | The Dungeon Master's encounters (wandering bosses have their own switch). |
| `rollEveryTicks` | 100 | 20..1200 | Ticks between two passes; each pass rolls once per party, one party per tick. |
| `encounterDc` | 17 | 5..30 | The difficulty class of the d20 check. |
| `baseCooldownTicks` | 1800 | 100..72000 | A party's cooldown after an encounter; shorter when loud (`base / (1 + noise / 5)`). |
| `minCooldownTicks` | 400 | 20..72000 | The cooldown never gets shorter than this. |
| `maxLiveMobsPerParty` | 8 | 1..64 | No new encounter while this many of his monsters live within 64 blocks. |
| `maxMobsPerEncounter` | 12 | 1..64 | The most monsters one encounter may bring. |
| `partyRadius` | 48.0 | 8..128 | Players this close (chained) are one party. |
| `partyFactors` | "1.0,1.6,2.1,2.5" | | The budget factor for one, two, three ... players. A bad list falls back to the default. |
| `partyFactorExtra` | 0.3 | 0..5 | What each player beyond the list adds. |
| `baseBudget` | 8.0 | 1..100 | Weak-monster equivalents, before band, threat and party. |
| `noiseDecay` | 0.2 | 0.01..1 | Fraction of the noise level lost per second. |
| `noiseScale` | 1.0 | 0..10 | Multiplies every noise; 0 = deaf. |
| `noisyAt` | 3.0 | 0.5..40 | The reading that counts as `noisy` for the tables. |
| `threatHealthPerLevel` | 0.06 | 0..1 | Extra health per threat level above one (capped at +300 percent). |
| `threatDamagePerLevel` | 0.04 | 0..1 | Extra damage per threat level above one (same cap). |
| `spawnMinDistance` | 24.0 | 8..64 | Nearest an encounter may appear to a party member. |
| `spawnMaxDistance` | 48.0 | 16..96 | Farthest from the nearest party member. |
| `spawnAttempts` | 12 | 2..64 | Positions tried per encounter before giving up (bounds the work of one roll). |
| `despawnDistance` | 96.0 | 24..256 | His monsters with no player this near are removed (sweep every five seconds). |
| `maxAgeTicks` | 12000 | 1200..240000 | An idle monster older than this, with no player within 24 blocks, is removed. |
| `cues` | true | | The distant sound and dust that announce an encounter. |
| `wanderers` | true | | Wandering bosses at all. |
| `wandererChance` | 0.10 | 0..1 | Chance per check that one appears in a sector with players (grows with time and threat, never above 0.5). |
| `wandererMinGapTicks` | 6000 | 200..720000 | After one appears, dies or leaves, the next may not come for this long in the same sector. |
| `wandererCheckTicks` | 200 | 40..6000 | Ticks between two checks. |
| `sightRange` | 24.0 | 4..96 | How far a boss sees a moving player in the light with a clear line of sight. |
| `hearRange` | 40.0 | 8..128 | How far a boss hears a very loud sound (a quiet one a quarter as far). |
| `loseTicks` | 160 | 20..2400 | Ticks without perceiving its target before a boss gives up. |
| `announce` | true | | The low rumble and "Something heavy walks the halls." when a boss appears. |

All values are plain `define` / `defineInRange` read through `EncConfig.get(...)`, which answers the default while the spec is not loaded.

## 7. Commands (permission 2), attached by the lead under `/multiverse mega`

`EncCommand.command()` returns the `dm` literal:

* `dm status`: your party, the sector, theme and threat, the noise reading, the light, the modifiers and the DC, the cooldown, the table, the live
  monsters near you, the last roll, and every live wandering boss with its state and target.
* `dm roll [minor|standard|hard|ambush]`: roll for your party now, ignoring the cooldown, the crowd cap and the safe-zone filter; with a band the dice are
  skipped and that size of trouble is spawned.
* `dm spawn <entry>`: spawn one entry of the table of the sector you stand in (tab-completes every entry id).
* `dm wanderer`: bring a wandering boss into your sector.
* `dm clear`: remove every monster of the Dungeon Master and every wandering boss of the dimension.

## 8. Contracts with the other systems

* **Quest key** `mega.wanderer_survived`, fired through `NusmpEvents.fire(player, key)` once per chase when a player who was really chased (three seconds or
  more) gets away: the boss lost him, gave up, or he reached a safe room. The lead wires the quest JSON.
* **Entity tags** (for turf wars, the economy and anyone who needs to find the Dungeon Master's creatures): `nusmp_dm` on every monster of an encounter,
  `nusmp_dm_<faction>` when a faction was involved, `nusmp_wanderer` on every wandering boss. Persistent data: `nusmp_dm_entry` (the table entry id),
  `nusmp_dm_born` (game time), `nusmp_wanderer_sector` (the sector id, which is how a boss saved with its chunk is adopted again).
* **Attribute modifier ids** (permanent, multiplicative on the final value so they compose with the Labyrinth's health buffer): `nusmp:mega_dm_health`,
  `nusmp:mega_dm_damage`, `nusmp:mega_wanderer_health`, `nusmp:mega_wanderer_speed`.
* **Faction tags**: `nusmp:mega/<faction>`, written by the ecology builder. This system only reads them.
* **Economy, health buffer, Vital Strike**: apply by themselves through `Labyrinth.in`. Not duplicated here.
* **Sanctuary rooms**: LANDING, the safe rooms and the boss chain (ARENA, ANTECHAMBER, VAULT). Nothing of the DM spawns in them, a wandering boss never
  enters them, and a sound made in one does not attract a boss.

## 9. Self-tests (`/multiverse mega selftest`)

Registered from `EncRegistry.init` through `SelfTests.register`; each runs against the real sector 0 with a stand-in player passed explicitly (the stand-in is
not in the level's player list), takes a second or two, and removes what it made.

| Key | What it checks |
|---|---|
| `enc.tables` | All ten tables and the roster loaded; every entry's first monster resolves to a live entity type (an entity of a mod that is not installed is "dormant", not a failure); at least three kinds of wandering boss are usable. |
| `enc.roll` | A forced standard roll puts monsters inside sector 0, 24 to 48 blocks (or the entry's range) from the party, out of its sight, never in a safe room, tagged, within the budget, tracked. |
| `enc.safe_zone` | Twenty rolls for a party in the Landing are refused; the control (an ordinary room) is not; no candidate cell of a safe room is unflagged and the picker never chooses one. |
| `enc.noise` | The real game-event holders map to the right kinds; a blast reads at the player and adds a burst; sprinting steps count; the level fades; a blast far away is quieter. |
| `enc.wanderer` | A boss spawns, walks the room graph for 200 passes, never stands in a safe room or leaves the sector, notices a player two blocks away and lets him go at the safe room. |

## 10. Tests

Pure (JUnit 5, `src/test/java/.../mega/enc`, 183 tests at the last run): `PartyMathTest`, `NoiseKindTest`, `NoiseMeterTest`, `LightRulesTest`,
`CountRangeTest`, `EncounterTableParserTest`, `EncounterRollTest`, `MobPickerTest`, `SpawnPickerTest`, `PatrolGraphTest`, `WandererRulesTest`,
`WandererMindTest`, `WandererRosterTest`, `BurstRingTest`, `SanctuaryTest` and `EncounterDataTest`, which reads the shipped JSON from the source tree.
`EncounterDataTest` is the guard of the data: zero parser problems; ten files, 8 to 14 entries, all four bands; every entity id exists (vanilla and
the Tensura 2.0.1.3 entity list in `src/test/resources/mega/enc/known_ids.txt`, nusmp ids read from the registrations in the source tree); every cue sound
and particle exists; every message key is in the namespace and is translated once the language fragment is merged; at threat 1 nothing is harder than
standard; no entry's first monster costs more than its band can pay; for every threat from 1 to 1000, party size 1 to 6 and state of the party, every band
finds an entry; the roster parses and the Floor Warden is on it. A new id the test does not know is added to `known_ids.txt` after checking it.

Mutation checks (break one thing in a pure class, expect a failure, restore): 92 mutations, for example `PartyMath` radius `<=` to `<`, no
transitive closure, the natural-1 rule, the threat modifier, the sight modifiers, "a safe room does not protect", "forbidden rooms are reachable", "the vault
is not a sanctuary", "a faction monster needs no faction", "a stuck room is not remembered", "an investigation never ends". 90 were killed by the tests as
first written or after the tests that closed the gaps they showed (silent damage game events, the preference for tougher members, the caution near safe
rooms, the memory of stuck rooms and arrivals, the investigation timeout). Two mutants are equivalent and are left: the `n < 0` test in `CountRange.parse`
(a negative number always contains the dash that routes it to the range branch first) and the floor of one in `EncounterRoll.plan` (the first kind either
stays at one or the whole result is empty, whichever way the loop ends).

## 11. How the dice behave (the 20 faces of the d20 at the DC of 17, from `EncounterRoll`)

| Situation | Modifier | Nothing | Minor | Standard | Hard | Ambush |
|---|---|---|---|---|---|---|
| Solo, sector 1, quiet, dim corridor | +0 | 16 | 2 | 1 | 1 | 0 |
| Solo, sector 1, sneaking in the dark | -2 | 18 | 1 | 1 | 0 | 0 |
| Solo, sector 1, walking with a torch | +2 | 14 | 2 | 2 | 1 | 1 |
| Solo, sector 1, sprinting with a torch | +3 | 13 | 2 | 2 | 2 | 1 |
| Solo, sector 3, fighting, torch | +6 | 10 | 2 | 2 | 2 | 4 |
| Duo, sector 4, sprinting, torch | +7 | 9 | 2 | 2 | 2 | 5 |
| Trio, sector 10, brawling, torch | +13 | 3 | 2 | 2 | 2 | 11 |
| Trio, sector 10, brawling, torch, blood sector (+1) | +14 | 2 | 2 | 2 | 2 | 12 |
| Trio, sector 10, sneaking in the dark | +5 | 11 | 2 | 2 | 2 | 3 |
| Solo, sector 20, quiet, dim | +5 | 11 | 2 | 2 | 2 | 3 |

(Natural 1 is always "nothing"; natural 20 is one band worse, so even the quietest roll can be a minor encounter.) A quiet soloist in sector one is
met about once in five passes, and then mostly by a nuisance; a brawling party deep in the dungeon is ambushed more often than not. The budget behind
the bands, for one player (base 8): sector 1 gives 4 / 8 / 12.8 / 17.6 points for minor / standard / hard / ambush, sector 5 gives 8 / 16 / 25.6 / 35.2;
a party of four multiplies it by 2.5.

## 12. What is unverified (nothing ran in a game)

* **Balance.** Every number the design calls a guess (the DC 17, the modifiers, the budget, the cooldown, the window `12 + 14 * threat`, the health and damage
  per level, the sight and hearing ranges, the wanderer chance) is a first estimate; they are all in the config or the tables for that reason. The table
  entries were checked against budgets by arithmetic and by `EncounterDataTest`, not by playing them. The Tensura mobs' health is read at run time; the
  tables assume only that a Tensura heavyweight costs a few points.
* **`VanillaGameEvent`.** Its accessors were checked against the API dump of the exact version (`getLevel`, `getVanillaEvent`, `getCause`, `getEventPosition`),
  but whether every footstep, door and block reaches a NeoForge listener on the game bus, and how often, was not observed. The noise meter also has the
  direct damage hook as a second source.
* **Reflection on `Mob.goalSelector`.** `GoalAccess` reads the field by its official name (as the Paladin code does). If it fails, the patrol goal is simply
  not installed and the Director's own half-second pass walks, aims and releases the boss; the behaviour is then coarser but not broken. Not observed.
* **SmartBrainLib mobs.** Tensura's mobs think with brains. `MobSteer` follows the repo's own recipe (erase the walk target, set the target slot and the
  attack-target memory, clear both to release). How a given Tensura brain reacts to a target it did not choose (some behaviours re-pick targets within a few
  ticks) was not observed: the guard cancels the target-change event, and a target set directly in the brain is released by the next pass (half a second).
* **A wandering boss with its own skills.** A Tensura mob may carry innate skills the Dungeon Master cannot see or stop. The roster favours physical bodies,
  but a "physical" Tensura mob may still cast.
* **Floor Warden as a wanderer.** It drops a Shadow Key by code (removed from its drops here). Its boss bar is hidden by calling `stopSeenByPlayer` for every
  player of the sector on each pass; if the entity re-sends the bar on a tick, a flicker of under half a second is possible.
* **A wandering Floor Warden is still a Floor Warden to every system that keys on the entity type.** `GenesisCoreDrops` (a Genesis Core for each
  participant: the first time always, then 15 percent), `QuestWorldEvents` (the quest event `labyrinth.warden_defeated`, which belongs to the Spire's boss),
  `EconomyDrops` (its crystals) and `SocketLoot` (a rare socket Core, through `BossRegistry`) all fire on its death. The owner's rule is that a Genesis Core
  drops from the boss rooms only, so this should be closed by the lead, in one of two ways: take `nusmp:floor_warden` off `wanderers.json`, or make the first two
  ignore an entity that has the tag `nusmp_wanderer` (one line each at the top of `GenesisCoreDrops.isDungeonBoss` and of the `floor_warden` line in
  `QuestWorldEvents.onDeath`). The shipped roster keeps the Warden because the brief names it the signature wanderer.
* **Peaceful difficulty** removes hostile monsters at once; the Director refuses to roll then (and says so), the wanderers are not guarded against it.
* **Tensura mobs are not `Enemy`**, and `EconomyDrops` and `HpBuffer` key on `Enemy`, so the stone and meat economy and the health buffer do not apply to
  Tensura monsters of the Dungeon Master (they do to the vanilla ones). Those two classes are outside this builder's files.
* **Deviations from the brief.** Threat scaling uses permanent attribute modifiers (they survive a chunk unload with the monster) instead of transient ones;
  the cooldown and the last roll are kept per player, a party's cooldown being the longest of its members' (so regrouping does not reset it) instead of per
  party id; no `LookAtPlayerGoal` is added (the boss's own look goals are kept); entries have the extra keys `range`, `cells`, `hunt`, `cue`, `minParty` and
  `comment`; wandering bosses are driven from a Director pass plus an optional goal, not by a goal alone.
* **The language fragment** (`message.nusmp.mega.enc.*`, 46 keys) must be merged into `en_us.json` by the lead; until then the messages show their key.

## 13. New, replaced, kept

* **NEW:** everything in the package `labyrinth/mega/enc`, the ten encounter tables, the roster, `EncConfig`, the admin tree, the five self-tests, the tests
  and `known_ids.txt`.
* **REPLACES:** only the empty `EncRegistry` stub of the contract (its `init` and `register` bodies).
* **KEPT, untouched:** the economy (stones and meat), the health buffer, the Vital Strike, the gloom clock, the Dungeon Guard, the Floor Warden, the boss
  rules; the contract classes; every shared file.
