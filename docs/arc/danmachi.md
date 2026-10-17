# The DanMachi saga: four monsters of the Dungeon and six friends (1.61.0, builder DM)

The core (`docs/arc/core.md`) gave the characters a roster, a boss layer over the Zogratis framework, one NPC entity and a story chain; the art kit
(`docs/arc/art_kit.md`) builds their models; the world part (`docs/arc/world.md`) gives them floors and arenas. This part is the DanMachi saga: **four monsters** (the
Minotaur, the Black Goliath, the Amphisbaena, Udaeus), **six friends** (Eina Tulle, Bell Cranel, Hestia, Welf Crozzo, Liliruca Arde, Ais Wallenstein), and one mechanic
that ties them to the framework: the **Dungeon Level**. Nothing here was run in the game: section 12 lists exactly what is unverified. Per character:
[`minotaur.md`](minotaur.md), [`black_goliath.md`](black_goliath.md), [`amphisbaena.md`](amphisbaena.md), [`udaeus.md`](udaeus.md), [`danmachi_npcs.md`](danmachi_npcs.md).

## 1. What is new and what it replaces

| Part | New or replacement | Why |
|---|---|---|
| The four monsters (classes, tables, models, Charter Grimoires) | **new** | nothing like them existed |
| The stone Goliath and the Charybdis Spawn (floor bosses of the old dungeon) | **kept, untouched** | the brief: "do not rebuild them". ARC-WORLD gives them roster entries (`goliath`, `charybdis_spawn`); the Black Goliath is a separate, irregular kin and follows the stone Goliath in the story |
| The six friends (records, models, dialogue) | **new** | |
| The Dungeon Level | **new** | the brief asked for a "Leveled up" second form tied to the Gates; it uses the Tensura EP the balance law already reads and adds no data of its own |
| The System, the Time Magic VFX, any shared file | **not touched** | owner rules; the lead's lines are in section 10 |

## 2. The Dungeon Level (how it ties to the Gates)

DanMachi measures everybody in **Levels** (an adventurer's Falna; a monster's rating in the Guild's books) and says the Dungeon answers strong adventurers with stronger
monsters. The mod has no Falna, but it has one number that already says how strong a fighter is, the Tensura **existence points (EP)** the balance law reads
(`Nullification.maxEP`, 0 for a fighter without Tensura data, which is Level 1). So a Level is a band of EP on a log scale (`DungeonLevel`, pure, tested):

| Level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| from EP (defaults: base 1,000, half a decade per Level) | 0 | 3,162 | 10,000 | 31,623 | 100,000 | 316,228 | 1,000,000 | 3,162,278 | 10,000,000 |

The monsters have fixed Levels: **Minotaur 2, Black Goliath 5, Amphisbaena 5, Udaeus 6.** The party's Level is the mean of the Levels of the players near the boss (mode
`mean`; `median`, `strongest`, `weakest` are config choices; summons count only when no player is near).

**At the Gates** (the framework opens them at a quarter of the monster's health: 100% power, a 60-tick invulnerable transformation, the second health bar of the
100% mode) `DanMachiBoss.onGatesOpened` measures the party:

* party Level below the monster's Level plus the margin (default 0): **0 steps**. The Gates only enrage it (100% power, as for every boss); its body does not change; the chat says
  "Dungeon Level: your party is Lv.1; Minotaur stays Lv.2 until the party reaches Lv.2." (this is how a player learns the rule exists).
* otherwise **1 step, plus one for every two Levels above that line, at most 3** (`maxLevelSteps`). A step is **+10% damage** and **+3% speed**; the monster's Level goes up by
  the steps (so its Howl frightens more fighters); **the first step also gives the leveled form** (`gatesForm()` answers `FORM_HYBRID` only when it Levelled up, so the model
  swap to `arc_<id>_form2` shows only then; the hitbox grows 8%), and it **unlocks its one leveled move** (the Wild Cross, the Titan Slam, the Blue Flame Cyclone, the Black Sword Wave).
* the Gates' own refill (the second health bar) is the framework's and is unchanged: a Level-Up does not heal anything extra.
* an abandoned fight (everybody gone for ten seconds) resets the framework to 20% power and this layer to the start: steps, speed bonus, stun, the mechanics, the called monsters.
* with `levelUp = false` nothing of this happens (no steps, no notice).

The brief's wish that the Falna/Status idea "can echo the System" is met on the reading side only: `DungeonLevel` is pure and has no game types, so the System (whose code is untouched) can read a Level
from the same EP whenever it wants to. Why this shape: the Gates already are the one moment of a Zogratis fight in which the monster is invulnerable for three seconds, changes form and draws its full
strength, so a rule "a strong party meets a stronger monster" has its natural place there, costs the framework nothing, and is decided once (never while the
players are mid-combo). A weak party is never punished by the rule: it meets the same monster it would have met anyway.

## 3. How a fight is built (the shared layer, `arc/danmachi`)

| Class | What it is |
|---|---|
| `DanMachiBoss` | base class on `ArcBoss`: turns the move table into framework `Move`s, the Level-Up, the stun of a crash, the busy time of a long effect, the notices, save data (`DmSteps`) |
| `DmSpec`, `*Table` | pure: a move is one row of data (power gate, wind-up, cooldown, range, weight, shape, numbers); four tables, one per monster, tested for the pacing rules |
| `DmShapes`, `DmFx` | the executor of the shapes (cone, circle, ring wave, lanes, marks, charge, leap, volley, spokes, breath) and the one place that knows which effect shape draws which material |
| `DmGeometry` | pure: the one answer to "is this foe inside the shape" that the telegraph and the strike both ask |
| `PreyBook`, `ChargeMath`, `HowlMath`, `HideState`, `HeadCut`, `RibGuard`, `AddPlan` | pure state machines and numbers of the mechanics (hunt, charge and crash, howl, black hide, red head, rib guard, called monsters) |
| `DmAdds` | the called monsters: vanilla mobs tagged with the boss and an expiry tick, no drops, no experience, never above the cap |
| `PartyLevels`, `DungeonLevel` | the Levels |
| `QuestBook`, `DanMachiQuests` | which friend's request which monster's fall finishes; the listener |
| `DanMachiRoster`, `DanMachiNpcs`, `DanMachiCast` | the ten records (pure), the six friends, and the one line that registers all ten |
| `DmBlows`, `DmText`, `DmSafe` | what kind of blow a `DamageSource` is; the words (pure); the guard around every zone step and delayed task |

Rules every move follows (the owner's, and the framework's): a telegraph of at least 8 ticks (ultimates at least 30; the tables are tested for 16 ticks at the opening and still 12 at the
Gates, and 56 and 42 for an ultimate); the **aim is locked when the wind-up begins** (the first tell), so a foe can step out of the shape; every hit goes through `ZogratisBoss.strike` (power tuning, the
victim's EP, the balance law's cap of a hit at the pvp share of a player's health); a move that can hit one foe more than once shares a **damage budget** (45% of the victim's
maximum health at most, `GoliathMath.DamageBudget`); **no block is placed, moved or broken** (so `DungeonGuard.internal` is never needed); nothing heals; effects go out as
`VfxSpawn`/`BossMoveVfx`/`Tells` (no vanilla particle is used for an ability); **hard control** is never applied: the Howl is a Slowness and a Weakness whose length goes through
`BalanceLaw.controlTicks` after `BalanceLaw.beginControl(boss)` (the shared 12-second control cooldown), and a stunned monster is the monster's own stun, not a player's.

## 4. The called monsters (`DmAdds`, `AddPlan`)

Three roars call help: Udaeus's (Spartoi: skeletons in iron helmets with iron swords), the Amphisbaena's Water City (drowned), the Black Goliath's (husks and spiders). They are
**vanilla mobs** (no new entity, model or renderer), tagged in their persistent data with the boss's UUID and the game tick they expire at, found again from that tag, **no drops
and no experience** (two NeoForge listeners, registered the first time a monster is called), scaled by `AddPlan` (health 60% of the kit's base at the opening, +15% per power
step; attack 70%, +10%), at most `2 + stage` alive (2 at 20% ... 6 at the Gates) and never above `maxAdds` (default 6) or the hard ceiling of 24, living a minute at the opening
and ten seconds more per step, and removed when the fight ends or is abandoned.

## 5. Config (`nusmp-danmachi-server.toml`, section `[danmachi]`)

Every value is a plain `define`/`defineInRange` (never `defineInList`) read through `DanMachiConfig.get(...)`, which answers the default until the spec is loaded.

| key | default | what |
|---|---|---|
| `levelUp` | true | the Level-Up at the Gates; false: the Gates only enrage |
| `levelBaseEp` | 1000 (1 to 1e12) | EP at which a fighter counts as Level 1 |
| `levelDecades` | 0.5 (0.05 to 3) | powers of ten of EP per Level |
| `partyMode` | "mean" | `mean`, `median`, `strongest` or `weakest` (anything else means `mean`) |
| `levelUpMargin` | 0 (-4 to 9) | the party's Level must reach the monster's plus this |
| `maxLevelSteps` | 3 (0 to 5) | most steps (0 switches the Level-Up off) |
| `damagePerStep` | 0.10 (0 to 1) | damage bonus per step |
| `speedPerStep` | 0.03 (0 to 0.5) | speed bonus per step |
| `howlFear` | true | the Howls frighten (slow and weaken) fighters below the monster's Level |
| `maxAdds` | 6 (0 to 24) | most called monsters per boss at a time (0: the calls only roar) |
| `quests` | true | the friends' requests finish by themselves when their monster falls |

## 6. The friends (see [`danmachi_npcs.md`](danmachi_npcs.md))

Six records (`DanMachiNpcs`), six models (`tools/arc/char_<id>.py`), six dialogue files (`data/nusmp/arc/dialogue/<id>.json`, 125 language keys with the four entity names).
Their **roles are their dialogue**: the shared NPC entity has no shop or quest system, so the smith's and the supporter's `shop` lines wait for one (they are never said otherwise)
and the four requests are driven by `DanMachiQuests` through the arc system's quest stage: the friend offers it once (stage 0 to 1, only to a player who has not yet beaten the
monster), the monster's fall sets stage 2 for every credited fighter (`ArcStory.credited`, FTB teammates included), the friend thanks them once (stage 3). Eina asks for the
Minotaur, Welf for the Black Goliath, Lili for the Amphisbaena, Ais for Udaeus.

## 7. Floors, story, themes

| monster | tier / health | floors (both roads) | story after | themes |
|---|---|---|---|---|
| Minotaur | 1 / 1,000 | 5 to 14 | nothing in this saga | dwarven_forgehold, gravity, verdant |
| Black Goliath | 2 / 1,800 | 12 to 26 | Minotaur, Goliath (the stone one) | gravity, dwarven_forgehold, flame |
| Amphisbaena | 2 / 1,900 | 22 to 36 | Black Goliath | sunken_temple, storm, verdant |
| Udaeus | 3 / 2,600 | 32 to 50 | Amphisbaena | necropolis, shadow, cursed_archive |

The floors overlap on purpose and cover 5 to 50 without a hole (a test checks every floor on both roads). No record names an arena: the arena of the saga, the Pit of the Dungeon,
is the one they are fought in (`ArenaShapes.forCharacter`). The saga as a whole is not chained behind another saga: the lead may add `after(...)` to the Minotaur's record in
`DanMachiRoster` to put it behind another arc.

## 8. Models (the art kit; what could not be done)

All ten are built by `tools/arc/char_<id>.py` through `python3 -B tools/gen_arc_characters.py <id>` (the generated files are the kit's own output, checked with `--check`: 91 files,
0 differ, 0 problems). The kit builds **humanoid bodies only** (no quadruped, no snake, no centaur), so the monsters are humanoid-with-parts forms:

* **Minotaur**: a `huge` humanoid with a bull's head made in `extra` (muzzle with nostrils, a gold nose ring, a brow ridge) and `post` (the head bone is drawn 1.38 times as big in every clip),
  the kit's bull horns; Asterius (`form2`): a black hide, red glowing eyes and veins, ONE long bull horn and the broken stump of the other (built in `extra` with the kit's own horn chain).
* **Black Goliath**: a `huge` widened humanoid (height 3.9) with ember cracks over the hide, a jaw with a lit mouth and tusks, a crest of black stone spikes, plate armour with glowing edges; the head is drawn 1.65 times as big.
* **Amphisbaena**: a `huge` humanoid in white-blue with plate armour, devil horns, fin ears and a dragon's tail, and **the second (red) head on its own neck bone `head2`** beside the first, as big as the main head, swaying like cloth. A real serpent body is not possible; the two heads are.
* **Udaeus**: a `lanky` humanoid (thin limbs at 3.9 blocks tall) in bone with the kit's skull mask, a rib cage, breastbone, collar bones, spine and a glowing magic stone built in `extra`, a tattered cloak and a black greatsword; the skull is drawn 1.5 times as big.
* the six friends are ordinary humanoids of the kit's builds (`slim`, `small`, `broad`); Lili has the kit's backpack, Welf a hammer in the hand and a great sword on his back, Eina the open ledger (the kit's grimoire).

The builder looked at previews of every model (front, face, idle and attack of every monster, the leveled forms, front and face of every friend), drawn by the kit's software
rasteriser, which is not the game; the pictures are not part of the repository.

## 9. Weapons

None is built in this commit (the brief: "no weapons now"). If the owner wants them later, the characters would give: Bell's Hestia Knife (a dagger), Ais's sword, Welf's great
magic sword and hammer, and from the monsters Udaeus's black greatsword. Per the owner's rule 6 each would carry Tensura engravings that fit its lore, chosen from
`severance`, `barrier_piercing`, `holy_weapon`, `magic_interference` (the only four of the `#tensura:engraving` tag known to this builder; the rest of the tag was not checked).

## 10. Integration (the lead's lines; nothing shared was edited)

1. `ArcCharacters.bootstrap()`: `com.newuniverse.nusmp.arc.danmachi.DanMachiCast.registerAll();` (loads the four boss classes, so their entity types register before the registry events, and registers the six friends).
2. `ArcSystem.init`: next to the arc config, `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.arc.danmachi.DanMachiConfig.SPEC, "nusmp-danmachi-server.toml");` (without it every value is its default).
3. Lang: merge `frag/dm.lang.json` (125 keys) into `en_us.json`; `python3 tools/gen_arc_dialogue_lang.py --check` then passes for the six files.
4. `python3 tools/gen_arc_grimoire_assets.py` (it finds the four records in `DanMachiRoster.java`).
5. Assets: `python3 -B tools/gen_arc_characters.py` regenerates `tools/arc/manifest.json`; the ten new `arc_<id>` models (and the four `_form2`) are in this commit.
6. The quest listener registers itself when the first monster class loads (`DanMachiBoss` static initialiser): no line.
7. `tools/quests/arc_roster.txt` (the advancement roster, `id|name|saga|role`; `/multiverse arc dump` writes it in a game, or append by hand): `minotaur|Minotaur|danmachi|boss`, `black_goliath|Black Goliath|danmachi|boss`,
   `amphisbaena|Amphisbaena|danmachi|boss`, `udaeus|Udaeus|danmachi|boss`, `eina_tulle|Eina Tulle|danmachi|npc`, `bell_cranel|Bell Cranel|danmachi|npc`, `hestia|Hestia|danmachi|npc`,
   `welf_crozzo|Welf Crozzo|danmachi|npc`, `liliruca_arde|Liliruca Arde|danmachi|npc`, `ais_wallenstein|Ais Wallenstein|danmachi|npc`.
8. The Hub's guide (`arc.hubGuide`, default `eina_tulle` in the arc world's config) is the id of one of the six friends: she stands at the Sanctuary's lectern once her record is in the roster.

## 11. Tests (pure, run with the JUnit shim; none needs a game)

`DungeonLevelTest`, `DmGeometryTest`, `PreyBookTest`, `ChargeHowlAddsTest`, `RibGuardTest`, `HeadHideTest`, `DmTablesTest` (played fights through the real `MoveScheduler`: the opening,
60% and the Gates), `DmTextTest`, `TellClockTest` (the framework's every-third-tick calling rule walked for every wind-up length of the four tables and their tuned lengths: exactly one call locks the aim), `DocumentedNumbersTest` (the numbers and the Level-Up table of the docs spelled out as literals), `DanMachiRosterTest` (health bands, floors 5 to 50 on both roads, the
story chain, every Charter skill against the real list of Tensura ids, real themes), `DanMachiDialogueTest` (the real files and the real dialogue rules, the requests against
`QuestBook`). **Mutation checks** that were run: taunt hits 3 to 4, steps `lead/2` to `lead/3`, cone slack removed, rib block 0.30 to 0.50, plain factor 0.5 to 0.6, cut fraction
0.07 to 0.08, crash bonus 1.30 to 1.20 (the last four survived the first test set because the tests compared a rule with its own constant, which is why `DocumentedNumbersTest` exists; all die now).

## 12. What is NOT verified (honest list)

* **Nothing ran in the game.** The Java type-checks against the API stubs (the negative control caught a deliberate wrong call); the pure classes are tested; every class that touches the world
  (`DanMachiBoss`, `DmShapes`, `DmFx`, `DmAdds`, the four bosses) has only been read and type-checked.
* The motion code (a charge by setting the velocity every tick, a leap along a parabola, a stun that holds the body with `navigation.stop()` and a move-control reset) assumes
  how a mob's `travel` consumes `deltaMovement` in 1.21.1; a charge that stops early or a leap that lands short would show it.
* `Move.tell` is called every third tick; the aim lock reads the first call as `left >= total - 3`. Worked out by hand for wind-ups of 8 and up.
* The effect shapes (`BOSS_STONE_*`, `BOSS_TIDE_*`, `BONE_FX*`, `WATER_JAVELIN`, `FIRE_WILD`, `POISON_BREATH`, `MIST_MAGIC`, `CORPSE_RAISE`, `LUCIUS_CORE_GEM`) were chosen from their documentation; how they look together was not seen. There is **no pure blue flame**: the Amphisbaena's flame is the mod's fire shapes with a blue glow colour.
* The framework heals a boss a little every second (`regenFractionPerSecond`, the framework's own); this layer adds no heal.
* Called monsters are vanilla mobs; whether a skeleton with a sword and a helmet picks its fights as intended was not seen.
* Canon: the wikis could not be opened (the fetch tool was blocked) and the last web searches were refused (the session's search budget was used up), so every canon fact below the monster docs is from search-result summaries
  gathered earlier. Episode and volume numbers, the exact floors and the wording of the abilities are the least certain; each monster doc says so.
