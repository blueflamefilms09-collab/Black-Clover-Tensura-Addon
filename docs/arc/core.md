# The core of the characters of the arcs (1.60.0, builder ARC-CORE)

The contract (`docs/labyrinth/mega_v2.md`, section 6) promised a roster of characters, a boss layer over the Zogratis framework and one shared NPC entity. This
file documents the systems every character stands on: the NPC entity, the dialogue, the story chain, the Charter Grimoires of the bosses, the global boss
buff rows, the spawn doors, the commands, the config, the self-tests. Nothing here was run in the game: the last section lists what is unverified.

Sources: none outside this repository (no web research was needed: every API used is already used elsewhere in the mod, and the signatures were checked
against the API stubs of the build). The ideas follow the mod's own Zogratis boss framework, the Charter, the quest hook `NusmpEvents` and the Mega-Dungeon
self-test registry.

## 1. How it is started (one line)

`ArcSystem.init(modEventBus, modContainer)` in the mod constructor, **before** `NUItems.ITEMS.register(modEventBus)` (the grimoire items and the spawn egg go
on `NUItems.ITEMS`). It does, in this order, and nothing else needs wiring:

1. `ArcCharacters.bootstrap()`: fills the roster and loads every boss class (which registers its entity type).
2. `ArcEntities.init(modBus)`: the boss types, the NPC type, their attributes.
3. `ArcItems.init(modBus)`: the data component `nusmp:arc_character` (item `nusmp:arc_spawn_egg` registers on `NUItems.ITEMS` when the class loads).
4. `ArcStory.init(modBus)`: the story attachment, then for every boss: the boss registry entry, the buff row, the Charter row and item; and the defeat listener.
5. `registerConfig(SERVER, ArcConfig.SPEC, "nusmp-arc-server.toml")`: the config registers itself, there is no separate line for it.
6. Game-bus: the dialogue reload listener, the clear at server stop.
7. `ArcSelfTests.register()`: the keys `arc.*`.

Client: `ArcClient.init(modEventBus)` (renderers in a loop over `ArcEntities.bosses()`, the NPC renderer, the tint of the spawn egg).
Commands: `root.then(ArcCommand.node())` under `/multiverse`.

## 2. Class by class (why)

| Class | Why it exists |
|---|---|
| `arc/ArcSystem` | one entry point and one fixed order; the roster must exist before anything reads it, and the items must be registered before the registry freezes |
| `arc/ArcConfig` | the server switches (section 6); every value a plain `define` / `defineInRange` read through `get(...)`, which answers the default until the spec is loaded |
| `arc/ArcNpcEntity` | the one friendly entity: persists, never despawns, cannot be hurt (`/kill`, the void and creative players excepted, unless `arc.npcsMortal`), stays near its post, faces the player and shows `talking()` while it talks, has the character's name |
| `arc/ArcItems`, `arc/ArcSpawnEggItem` | ONE egg item whose character is a data component, so a new character needs no item, no model, no registration; `creativeEggs()` gives the stacks for the creative tab |
| `arc/ArcSpawner` | the one place with the rules of bringing a character into the world (layer on, is a boss, has an entity, story gate, not already standing); used by the command, the egg, and (for the arcworld builder) the arenas |
| `arc/ArcCommand` | `/multiverse arc ...` (section 5) |
| `arc/ArcBossProfile` | pure: the tier (1..5) of a character as the row of the global buff engine, so thirty bosses need no thirty hand-written rows |
| `arc/ArcCharterRules` | pure: turns the grimoire a character declares into a Charter row, applying the Charter's laws (cap 12, never a thief's skill, LEGEND or GOD) |
| `arc/ArcSelfTests` | the in-game checks the CI smoke test runs (section 7) |
| `arc/dialogue/DialogueContext` | the six situations a character speaks in |
| `arc/dialogue/DialogueLine`, `DialogueSituation`, `DialogueBook` | pure: parse a dialogue file, say which line a player hears |
| `arc/dialogue/TalkPlan` | pure: what the character talks about (greet a stranger, offer a quest once, thank once, otherwise a hint) |
| `arc/dialogue/DialogueLoader` | the data reload listener: files into an immutable snapshot; the generic `_fallback` book |
| `arc/dialogue/DialogueSender`, `ArcDialogue` | the chat line in the character's colour; the glue from "right-click" to a line |
| `arc/story/StoryData` | pure: one player's story (defeated, met, quest stage per NPC, saga) |
| `arc/story/StoryRules` | pure: stage, saga, unlock, gate, what a victory opens, closure, loops |
| `arc/story/StoryAttachments` | the player attachment `nusmp:arc_story` with its codec |
| `arc/story/ArcStory` | the game-side door: credit on defeat, the gate, the quest hook keys, the boot wiring of every boss |
| `client/arc/ArcClient` | renderer registration loop, egg tint |
| `client/arc/stubs_for_core/*` | signature-only stand-ins for ARC-ART's two renderers so that this code type-checks alone. **The lead deletes the folder when ARC-ART is merged.** |
| `charter/CharterTable.addArc`, `CharterItems.registerArc` | additive entries for the arc grimoires (copy-on-write; the seven are untouched; `CharterGrimoireItem.getName` falls back to "Charter Grimoire of <name>") |
| `boss/BossRegistry.addArc`, `BossBuffTable.addArc` | additive entries (copy-on-write; the old rows cannot be overwritten) |
| `boss/bar/BossBarSync` | the bar tint now asks the boss for `styleColor()/styleAccent()`: a devil's colours for the Zogratis family (unchanged), the character's own for the arcs |
| `charter/CharterNet` | the claim packet reader capped the list at 64 entries while the writer had no cap (a longer list would have corrupted the stream); both are capped at 256 now |
| `data/nusmp/arc/dialogue/_fallback.json` | the generic lines (greeting, hint, farewell) of every character that has no file of its own, so a character that is merged before its writer finishes still talks; it holds no quest lines on purpose (a generic offer would start a quest that does not exist) |
| `assets/nusmp/models/item/arc_spawn_egg.json` | the vanilla spawn-egg template model; the two layers are tinted per character by `ArcClient`, so the egg needs no texture of its own |
| `tools/gen_arc_dialogue_lang.py` | keeps dialogue JSON, language keys and the language file in step (section 3) |
| `tools/gen_arc_grimoire_assets.py` | draws the model and texture of every arc boss's grimoire from its record (section 4) |

## 3. Dialogue

`data/nusmp/arc/dialogue/<character_id>.json` (a datapack replaces a file by shipping the same path). The file `_fallback.json` is what any character without a
file says (so none is mute); if even that is missing a one-line book built into the code is used.

```json
{
  "character": "yami_sukehiro",
  "voice": "#C8C8D8",
  "lines": {
    "greeting": [
      "A plain string is a line with no conditions.",
      { "text": "Hello, {player}. I am {npc}.", "key": "arc.dialogue.yami_sukehiro.2", "first": true }
    ],
    "hint": [
      { "text": "Heath Grice is down. Good.", "key": "arc.dialogue.yami_sukehiro.3",
        "defeated": ["heath_grice"], "notDefeated": ["zagred"], "saga": ["elf_reincarnation"],
        "minStage": 1, "maxStage": 5, "minQuest": 0, "maxQuest": 1, "priority": 3, "weight": 2 }
    ],
    "quest_offer": [], "quest_done": [], "shop": [], "farewell": []
  }
}
```

* **Situations** (`DialogueContext`): `greeting`, `hint`, `shop`, `quest_offer`, `quest_done`, `farewell`.
* **Conditions**: `minStage`/`maxStage` (the number of bosses of the roster the player has defeated), `saga` (any of these sagas), `defeated` (all of these have
  fallen), `notDefeated` (none of these has), `minQuest`/`maxQuest` (the quest stage with this character), `first` (true: only the first meeting; false: never the first).
* **Choosing**: of the lines whose conditions hold, the highest `priority` wins (default: the number of conditions, so a line that reacts to the story beats a
  generic one; `"priority": 0` lets a conditional line mix with the generic ones); among those, a random one weighted by `weight`; the line said last time is
  skipped when there is another of the same priority.
* **Placeholders**: `{player}`, `{npc}`, `{saga}`.
* **Translation and datapacks**: English lives in the JSON. A line with a `key` is sent as a translatable component whose fallback is the JSON text: the
  language file wins (translations work). A line **without** a key is sent literally, so a datapack that replaces the file with new words is heard as written
  (a key would have let the old language entry win). Shipped lines get keys with the tool:
  `python3 tools/gen_arc_dialogue_lang.py --write-keys` (numbers the keys `arc.dialogue.<file>.<n>`), `--out frag.json` (the fragment of language entries, with the
  placeholders as `%1$s`, `%2$s`, `%3$s` and `%` doubled), `--check en_us.json` (every keyed line is in the language file with the same text). The numbering is
  checked by the Java test `DialogueFilesTest`.
* **Talking** (`ArcDialogue.talk`): the first meeting greets; a finished quest (stage 2) is thanked once (stage 3); a quest is offered once when the book has an
  applicable `quest_offer` line (stage 0 to 1); otherwise a hint; with no hint, a greeting. The quest stages are 0 none, 1 offered, 2 done (set by whoever hands
  out the quest, through `ArcStory.setQuestStage` or `/multiverse arc story <player> quest <npc> <stage>`), 3 done and thanked. A shop or quest system speaks
  with `ArcDialogue.say(player, character, topic, npcOrNull)`. When the conversation ends (80 ticks after the last click, or the player walks off) the
  character's own `farewell` line is said if it has one.
* A problem in a file (a typo in a situation, a line without text, a number that is not one, an unknown placeholder, an impossible range) is reported with a
  readable message in the log and in `/multiverse arc info <id>`; the rest of the file still works.

## 4. The Charter Grimoires of the arc bosses

A boss declares `.charter(chance, skills...)` in its `ArcCharacter`. `ArcStory.init` turns it into a Charter row (`ArcCharterRules.toBoss`) and an item
`nusmp:charter_grimoire_<id>`. The drop, the world lock (`has_dropped_charter_grimoire_<id>`), the mirroring and the tooltips are the existing ones. Skills that
the Charter forbids (`tensura:usurper`), duplicates and anything beyond 12 are dropped (and the in-game test `arc.charter` names them); the test also checks every skill id
against the Tensura skill registry. The item name is "Charter Grimoire of <name>" (no language entry per character is needed).

Each grimoire item needs a model and textures or it renders as the purple-and-black cube: after characters are merged run
`python3 tools/gen_arc_grimoire_assets.py` (it reads every `ArcCharacter.boss("<id>"...)` that has `.charter(` in the sources, takes `.colors(main, accent)`, writes
`models/item/charter_grimoire_<id>.json`, `_icon.json` and the two textures; the seven older grimoires are skipped). `--list` shows what it found, `--preview x.png` a contact sheet.

## 5. Commands (`/multiverse arc ...`)

| Command | Permission | What |
|---|---|---|
| `list [saga]` | 0 | the roster, by saga |
| `info <id>` | 0 | the record, entity, grimoire, dialogue file and its problems |
| `summon <id> [pos]` | 2 | a boss at the position (default: where you stand), ignoring the story gate |
| `spawn_npc <id> [pos]` | 2 | a friendly character with its post there |
| `story` | 0 | your own story |
| `story <player> [list]` | 2 | their story: stage, saga, defeated, met, quests, what is open |
| `story <player> unlock <id>` | 2 | credits everything that must fall before `<id>` (not `<id>` itself) |
| `story <player> reset` | 2 | forgets the story |
| `story <player> quest <npc> <stage>` | 2 | sets a quest stage (0..9) |

## 6. Config (`nusmp-arc-server.toml`, `[arc]`)

`enabled` true, `storyGate` true, `npcsMortal` false, `floorBosses` true, `floorBossChance` 0.5 (0..1), `shareStoryCredit` true, `npcWanderRadius` 6 (0..32;
0 = stands still), `talkCooldownTicks` 20 (0..600). `floorBosses`/`floorBossChance` are read by the floor-boss picker of the arcworld builder through
`ArcConfig.floorBosses()` and `ArcConfig.floorBossChance()`.

## 7. The story

* **Credit**: when an `ArcBoss` is beaten (`ArcBoss.ON_DEFEATED`), every online fighter (`fighters()`) and, with `shareStoryCredit` and the quests' share switch on,
  the nearby FTB team members of each (through `QuestTeams`, the existing reflection bridge) get the character into their `StoryData`, with a short message (who
  fell, what is now open, a new saga).
* **Stage** = the number of bosses of the roster defeated. **Saga** = the earliest saga that still has an undefeated boss (the last saga when all are down).
* **Unlocked** = every prerequisite of the character is defeated by this player. A prerequisite that is not in the roster is skipped (otherwise a chain with one
  unmerged link could never be opened). **The gate**: with `storyGate` on, `ArcSpawner.summon` for a party needs at least one of them to have the boss unlocked.
  Operators (`force`, the command, the egg) ignore it.
* **Quest hook keys** (`NusmpEvents.fire(player, key, arg)`; the lead wires advancements, see `tools/quests/spec_adv.py`):

| Key | Argument | When |
|---|---|---|
| `arc.defeated` | boss id | the player is credited with a defeat (once per boss) |
| `arc.unlocked` | boss id | a boss became fightable for the player (once per boss) |
| `arc.saga` | saga id | the player's saga moved on |
| `arc.met` | npc id | the first conversation with an NPC |
| `arc.talk` | npc id | every conversation |
| `arc.quest` | `npc:stage` | a quest stage changed |

## 8. Self-tests (`/multiverse mega selftest`, keys `arc.*`)

`arc.roster` (prerequisites resolve, no loops, floors sane, every boss in the boss registry), `arc.entities` (every boss type registered with attributes and the
right id, the NPC type), `arc.dialogue` (the fallback loads; every NPC has a file that parses and a line for a newcomer), `arc.story` (once-only records,
quest stage, codec round trip through NBT, the gate and the unlock on the first boss that follows another), `arc.charter` (row, item, buff row, real skill
ids, nothing the laws drop), `arc.profiles` (the five tier profiles use only what the boss rules allow and never get weaker), `arc.spawn` (a blank NPC, the egg
component, the first NPC placed/named/saved, the first boss summoned and recognised by the boss registry, the Charter table and the buff engine; everything
spawned is removed again). With an empty roster they pass and say so, so the prefix `arc` is always reported (`SMOKE_EXPECT` gets `arc`).

## 9. For the builders of characters

**A boss**: a class extending `ArcBoss` with `public static final ArcCharacter CHARACTER = ArcRoster.register(ArcCharacter.boss(...)...build())`, a registration
`ArcEntities.boss("<character id>", Factory::new, w, h, look, attributes)` in a static field (the entity path must be the character id), its moves; a line in `ArcCharacters.bootstrap()`; a model
spec of the art kit; `.charter(...)` for a grimoire (run the grimoire tool afterwards). Everything else (buff row, boss registry, bar colours, story credit,
spawn egg, command, renderer) follows from the record. **An NPC**: an `ArcCharacter.npc(...)` registered in `bootstrap()`, a model spec, and a dialogue file
(then `gen_arc_dialogue_lang.py --write-keys` and the fragment). Tests must not call `ArcSystem.init` (it fills the global boss tables; the arc tests of this
package clean up after themselves with `clearArcForTests()`).

## 10. What is unverified (honest list)

* Nothing was run in the game. Every class type-checks against the API stubs (the only errors are the known stub artifacts: the abstract `new BlockPos` /
  `ItemEntity` / `MobEffectInstance` and `INBTSerializable.deserializeNBT` on an entity subclass) and the pure logic has JUnit tests that pass in the local shim.
* The self-tests (`arc.*`) have never run; they are the first thing the smoke test will tell us.
* `Entity.shouldShowName()` is overridden to true for a known character, so a renderer that honours the standard name-tag contract draws the name; if the
  ARC-ART renderer draws its own tag the two will double up (one of them must give way).
* The goal order and the `restrictTo` leash of the NPC are vanilla mechanisms used in a way no other entity of the mod uses; whether an NPC strolls and returns as
  intended is untested. `arc.npcWanderRadius` 0 pins it (radius-1 leash).
* `Component.translatableWithFallback` with positional arguments in the fallback is vanilla behaviour, but the dialogue lines are not seen in a client yet.
* The Charter claim packet cap was changed from 64 to 256 (reader and writer). The packet's shape is unchanged; an old client reading a new server's list would
  only go wrong with more than 64 claims, which needs more than 57 arc grimoires claimed.
