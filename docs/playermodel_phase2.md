# The player-model kit, phase 2: the dungeon's humanoids (1.99.0)

Owner request (open since phase 1, `docs/playermodel.md`): "remake all models to use the player model" for ALL humanoid NPCs and bosses of the dungeon. The spec the request grew from (sections 1 and 2 of the
owner's design prompt): dungeon mobs must look like their anime / internet-lore counterparts, but they MUST stand on the vanilla player skeleton, so they can hold weapons, wear armour and use the standard
biped animations, with custom HD skins and small 3D appendages (horns, ears, tails, wings, auras) on the player rig. Phase 1 built the kit and converted the arcs' people; this phase converts everything that is
a person in the dungeon and adds what the spec asks of a crowd: skins by script, appendages, postures, level of detail, and a switch back.

Status: **written, type-checked against the API stubs, the pure parts and every data file tested through the local JUnit shim, previewed with the repo's software rasteriser
(`docs/previews/humanoids/`). Nothing here has run in the game.** Section 12 lists what could not be verified.

## 1. NEW / REPLACED / KEPT (rule 5)

| part | |
|---|---|
| the player-model renderer of the Mob Foundry's people (`foundry_mob`, 314 of its 400 mobs), the Living Armor, the Floor Warden, the Labyrinth Guardian, the Goliath, the soul-corpse puppets Jimmy, Alfredo, Carl | **NEW** renderer path. It is registered next to the old one (a `Split`), and **REPLACES it as the default** for the entities that have a look; the old renderer classes, models, textures and animations are **KEPT** and are what the flag `client.playermodel.dungeon = false` gives back |
| 385 looks and skins (`looks_dungeon.json`): 55 archetypes `dg_*`, 314 Foundry people `foundry_*`, `living_armor`, `floor_warden`, `goliath`, `goliath_form2`, 12 puppet stages `corpse_*_s0..3` | **NEW** (nothing like them existed) |
| the 27 abyssal looks of `looks_abyssal.json` (nine humanoid Decennial bosses x three phases) | **NEW in use**: the file existed (built by `tools/abyssal`) but no loader read it. `PlayerLookTables` is the loader; `DungeonLooks.guardian` turns a boss profile's `look` into its player-model look. The 13 creature looks (beholder, dragons ...) **KEPT** on `AbyssalRenderer` |
| the 86 Foundry creatures (13 slimes, 14 insects, 59 beasts, dragons, spirits, wisps ... `dungeon_legacy.json`) | **KEPT** on `FoundryRenderer` (the owner's rule: creatures stay on their own models) |
| `PlayerLookTables`: `looks.json` + `looks_abyssal.json` + `looks_dungeon.json` merged add-only | **NEW** (the owner said "extend `looks.json` entries: add new, keep old": a second file is the same rule with no merge conflict between builders; an earlier table wins a duplicate id) |
| appendages: 12 head parts (`pmx_dg_ears_goblin` ...) and the extras cut out of 362 art-kit models | **NEW**; the art kit's models are **KEPT** |
| `tools/gen_dungeon_skins.py` and `tools/dungeon_skins/*` | **NEW**; `tools/playermodel/extract_extras.py` and `gen_skins.py` got additive options (section 8) |
| performance work in the kit (level of detail, no per-frame allocation, caches) | **REPLACES** the phase-1 internals (same API), see section 7 |
| hub NPCs (quest giver, merchant), the arcs' allies and Saga bosses, Zogratis family, Avatars | **KEPT** as phase 1 converted them (nothing to redo; `render.playermodel2` checks that every ally has a look) |
| not converted, on purpose | `rouge`, the spirit lords, the grimoire daemon, cotton creatures, the painted beast and giant, the direwolf, the basilisk, the dungeon slime, Charybdis, `mirror_double` (already the player model), the Spirit King (a celestial boss outside the dungeon, drawn from three geo files per phase: left for its own builder) |

## 2. Who is a person in the dungeon

| entity id | before (effective renderer) | now | look |
|---|---|---|---|
| `foundry_mob` (400 mobs, one entity type, the mob is a synced id) | `FoundryRenderer`: GeoLite `foundry_<id>` with the shared class clips | `Split`: kit for the 314 people, `FoundryRenderer` for the 86 creatures | `foundry_<id>` |
| `living_armor` | `KitMobRenderer` + `KitProbes.ARMOUR` (`arc_living_armor`) | `Split`: kit, old as fallback | `living_armor` |
| `floor_warden` | the same, model `floor_warden` | the same | `floor_warden` |
| `labyrinth_guardian` (every Decennial boss) | `AbyssalRenderer` (a geo body per boss look; the Warden's body without a look) | `Split`: kit for a humanoid look and for no look, `AbyssalRenderer` for a creature look | `abyssal_<look>_p<phase>` / `floor_warden` / (own model) |
| `goliath` | `KitBossRenderer` + `KitBossHooks.GOLIATH` (`arc_goliath`) | `Split`: kit | `goliath`, `goliath_form2` (Gates open) |
| `corpse_jimmy`, `corpse_alfredo`, `corpse_carl` | `NamedCorpseRenderer` (GeoLite `corpse_*`, four decay skins) | `Split`: kit | `corpse_<name>_s0..s3` |
| `quest_giver_npc`, `merchant_npc`, `arc_npc` (40), the arcs' bosses, Zogratis, Avatars, Tower Wizard | phase 1 | phase 1 | `looks.json` |

The Foundry mobs are all art-kit characters (a person with horns, wings or a tail where the sheet says so), which is why the conversion is mechanical: each mob's skin is painted from the very row its model was
built from. `tools/dungeon_skins/foundry_class.py` draws the line between a person and a creature, once, as data (`dungeon_legacy.json` carries it to the game): the factions `slime` and `insect` stay, and so
do 59 listed mobs whose concept is not a person (a pup, a stirge swarm, a roper, a mimic, an owlbear, a griffon, every dragon, a will-o'-wisp, a treant, a cursed spirit, a poltergeist ...). A dragon-kin
duelist, a wyrm-priest, a hell-hound handler or a golem built like a man are people.

## 3. How the registration works (the WHY of the wiring)

* `PlayerModelRegistry.register` (the kit's listener, LOWEST priority, registered after the art kit's, so its renderer is the last one registered for a type and wins) calls `DungeonRenderers.register(e)`
  after the phase-1 conversions. Each type is registered inside its own guard, so one that is renamed or missing logs an error and the rest still load.
* `DungeonRenderers.kitOrOld` registers a `PlayerModelRegistry.Split` (the dispatcher phase 1 already built for the painted construct) per type: the kit's `PlayerModelRenderer` when
  `DungeonLooks.usesKit(PlayerModelOptions.dungeon(), kitDraws(e))`, the type's previous renderer otherwise. **Why a Split and not a replacement**: the owner's rule is add the new, keep the old, and the flag
  `client.playermodel.dungeon` must give the old look back without a new jar; a Guardian whose boss is a creature and a Foundry mob that is a creature also need the old renderer in the same type. The check is
  a flag read and a map get, no allocation.
* The old renderer is the exact one that was effective: `RemodelRenderers.put` now records every provider it registers (`RemodelRenderers.providerOf(type)`, 3 additive lines), so the fallback for the Living Armor,
  the Warden and the Goliath is the art kit's own provider, not a copy of it; `AbyssalRenderer`, `FoundryRenderer` and `NamedCorpseRenderer` are public and are built directly. With no old renderer to fall back
  to, the kit's renderer alone is registered.
* The entity types, ids, attributes, AI, goals, drops, loot, saved data and the sound overrides of another builder are untouched: everything here is client code. The only entity-side data the renderer needs
  already exists (the Guardian's synced look id, phase and cast time of 1.94.0; the Foundry mob's synced id; the corpse's decay mark; the Goliath's rise).
* `config/nusmp-playermodel-client.toml`, section `playermodel`: `dungeon` (true), `detailDistance` (24), `farDistance` (64). Registered from `PlayerModelRegistry.init` (the mod's client constructor), so
  `NUSMP.java` is not edited; `PlayerModelSelfTests.register()` (already called from `NUSMP`) registers `render.playermodel2`.

## 4. The looks and the loader

`assets/nusmp/playermodel/looks_dungeon.json` has the format of `looks.json` (skin, slim, glow, hair, hair_color, scale, extras, hold_r / hold_l) plus **`stance`** (new, optional, `PlayerLook.stance`).

| id | what | scale |
|---|---|---|
| `dg_<archetype>` (55) | the named families (section 5) | the kit's model height over 1.875 |
| `foundry_<id>` (314) | one Foundry person | model height times the mob's size over 1.875 (`assets/nusmp/foundry/index.json`); the body divides the entity's SCALE attribute out (the hitbox follows the attribute, the look carries the height) |
| `living_armor`, `floor_warden` | the armoured bodies; the Warden / Guardian size comes from the hitbox (`PlayerBodies.fit` over the attribute) | 1.95 / 3.2 high |
| `goliath`, `goliath_form2` | the stone giant and the Gates open | 6.0 high, followed to the hitbox |
| `corpse_<name>_s<stage>` (12) | Jimmy 1.95, Alfredo 2.55, Carl 1.9 blocks, four stages of decay (stage 0..3 as the Soul Corpse mark carries it) | from the entity's height |
| `abyssal_<look>_p<phase>` (27, `looks_abyssal.json`) | the nine humanoid Decennial bosses, three phases | the look's reference height; the body sizes it to the hitbox times the look's boost, exactly as `AbyssalRenderer` did |

**The loader.** `PlayerLookTables.FILES` lists the tables in merge order (`looks.json`, `looks_abyssal.json`, `looks_dungeon.json`, and `looks_decennial.json`, a name reserved for the 200 Decennial bosses of
the next builder: the file is optional and is picked up when it exists); `PlayerAssets.loadLooks` reads each through the resource manager (the first is required, the others optional), and
`PlayerLookTables.merge` adds a look only if its id is not there yet and reports a duplicate. `DungeonAudit`, the JUnit tests and `render.playermodel2` read the same files through the class path. A boss
profile's `look` (`data/nusmp/mega/dnd_bosses/**`, the field of 1.94.0 that the Decennial builder reuses) reaches the screen like this: the server writes the id into the Guardian's synced data (`syncLook`), the
body asks `DungeonLooks.guardian(lookId, phase)`, which answers `AbyssalLooks.kitId(look, phase)` for a humanoid look, the Warden's look for no look or an unknown one (the old behaviour), and "" for a creature
look (the kit steps aside, `AbyssalRenderer` draws it). A new boss profile of the next builder that names one of the 22 looks needs nothing here; one that names a NEW humanoid look needs its three entries in
`looks_abyssal.json` (or in any later table) and `render.playermodel2` fails until they are there.

## 5. The skins (`tools/gen_dungeon_skins.py`, `tools/dungeon_skins/`)

Every skin is procedural and deterministic: **64x64, the vanilla player layout, wide or slim arms as the build asks (a test checks the skin against the flag), the second layer (hat, jacket, sleeves, pants) used for
plates, cloth, hoods, torn hems**, and a `_glow` map where eyes, runes, cracks or veins shine.

* **Painter**: the player-model kit's own (`tools/playermodel/skin_lib.py` via `gen_skins.paint_character`: skin tone ramp, face, garments on both layers, armour plates, accessories, masks) run on the art
  kit's sheet of the character. A new hook `post(cv, spec, seed, glow)` lets `tools/dungeon_skins/wear.py` weather the finished skin.
* **Wear** (`wear.py`, `recipes.py`): grime that gathers towards the feet and hems, rot and stain blotches, warts and scale speckles, scars, tears in the second layer (the skin under shows), stitches, rivets
  and edge light on plates, a rib cage, a skull, wandering glowing veins, hair-thin glowing cracks, glyphs, rime, moss, blood, fur flecks, soot with embers, wet sheen, gold leaf, wrapped bandages. A recipe is a list
  of (pass, arguments); 46 recipes: 35 for the archetypes, the dungeon's bodies and the puppets' stages, 11 light ones (`f_*`) that weather a Foundry mob by its family without touching the art kit's design.
* **55 archetypes** (`archetypes.py`, one line each in the Foundry roster's own language, so the art kit builds their horns, wings, tails, halos and capes too): goblin scrapper, shaman, archer; hobgoblin
  legionnaire; orc raider and warchief; kobold trapper; bugbear brute; gnoll hunter; cave troll; hill ogre; skeleton knight and archer; zombie labourer; ghoul; wight sentinel; wraith; banshee; vampire noble;
  lich; necromancer; death knight; mummy; imp; demon knight; succubus broker; warlock; cultist; dark priest; plague doctor; executioner; priest; cleric; paladin; angelic knight; monk; witch; druid; elf ranger;
  dwarf smith; bandit; mercenary; town guard; merchant; quest giver; guildmaster; assassin; gargoyle; stone golem; frost revenant; pyromancer; sahuagin warrior; wolfkin warrior; lizardfolk spearman; harbinger
  of ash. Previews: `docs/previews/humanoids/archetypes_0..6.png`.
* **The Foundry's 314 people**: painted from their own rows, so the roster guard's promise (400 mobs that do not look alike, `tools/foundry/guard.py`) carries over; no two of them share a skin (a test). Previews:
  `docs/previews/humanoids/foundry0..7.png` (eight contact sheets).
* **The dungeon's bodies**: Living Armor (wet dark-violet flesh in scavenged plate, a pale pit of an eye), Floor Warden (the same grown huge, violet runes and cracks), Goliath (pale cut stone, moss in the creases,
  amber cracks; the Gates open: every crack burning), and the three puppets in four stages of decay (stitches, then rot, then exposed flesh and bone, then glowing veins and cracks).
* **Appendages**: what the art kit builds as separate bones (wings, tails, horns, halos, capes, hats, the held weapon) is cut out by `extract_extras.py` and hung on the player model's head, body or arms
  (362 models: 308 Foundry people, 48 archetypes, the Living Armor and the Warden, 4 puppet stages: `pmx_foundry_<id>`, `pmx_dg_<id>`, `pmx_living_armor` ...; the others have nothing but a body), with the extras' tracks of the mob's shared animation class (idle, walk, run,
  attack, cast, cast_big, hurt, death). What the art kit drew on its head box (ears of goblins, elves and fish-folk) cannot be cut out, so `parts.py` rebuilds it, and adds what the player model's head needs:
  `pmx_dg_ears_goblin / elf / pointed / fin`, `tusks_orc / boar / small`, `beard_dwarf / short`, `snout_reptile`, `crown_spikes`, `horns_helm`: grey boxes with a texture each, tinted with the skin, bone, hair
  or gold colour by the look (`Extra.tint`). 44 Foundry people wear a head part; the others get their horns, wings and tails from the art kit.
* **Hair**: from the sheet's style through the kit's hair system (23 styles, springs, helmets cut it); the colour is the sheet's. Examples: goblins a mohawk or bald, orcs a mane or a mohawk, hooded cultists
  the `hood` style, long wavy hair for the banshee and the succubus, a bun for monks. 245 of the 314 Foundry people have hair, 69 are bald or wear a mask.
* **Size**: `tools/dungeon_skins` writes `scale = height / 1.875` into the look and the body keeps the world height of the old model (Foundry: height times size from the asset index; Guardian: the hitbox times
  the look's boost). A body drawn large (scale above 1) steps with a longer stride (`KitPlayerModel.stride`, one over the total scale).

Disk: 586 skin PNGs (385 skins and 201 glow maps, 2.7 MB), 374 extras models (362 cut from art-kit models and 12 head parts: 8.3 MB of minified json and PNGs, about a third of that in the jar). Regenerate with `python3 -B tools/gen_dungeon_skins.py` (45 s;
`--only a,b` for some Foundry ids, a step name for one step).

## 6. Gear, weapons and animation

Everything a player's renderer does, the kit's `Body` already does for any `LivingEntity`, so every converted mob has it with no code of its own: the vanilla walk, swing, sneak, ride, swim and death
animations; the main and off hand items (`ItemInHandLayer`, `ArmPoses`: item, shield block, drawn bow, crossbow charge and hold, spear throw, spyglass, horn, brush; a two-handed pose takes the other arm too);
the armour layers drawing the equipped armour (wide and slim models; a Living Armor or Warden spawned with armour wears it over the skin); head items; elytra; relics (`KitRelicLayer`). Phase 2 adds:

* **a real item replaces the art kit's weapon**: when the entity holds an item in a hand, the extras hung on that arm (the art kit's picture of its weapon) are not drawn (`ExtrasDraw` `skipArmR/L`);
* **stances** (`Stances`, `look.stance`): `hunch` (goblins, kobolds, fish-folk: 39 looks) bends the back and juts the head; `zombie` (the dead: 19 looks, Jimmy and Alfredo) reaches the arms forward, high while
  hunting, low and swaying while shambling; the arms are left alone during a swing so the blow keeps the vanilla sword-swing, and any pull of the body's own pose wins over the stance;
* **casts**: a Foundry mob's wind-up is the cast pose (`PosePatterns.cast`, big from 20 ticks), a Guardian's synced cast cue plays it for 24 ticks (a change of phase the big one);
* **the Goliath's moves** are poses (`DungeonPoses`: slam, stomp, roar, fist, line, rubble, quake, rise): the blow lands at 80 percent of the wind-up as the old clips were paced;
* **Carl's throw** is an arm pose paced by the entity's 12-tick countdown (`DungeonPoses.throwArm`);
* **the Goliath rises out of the floor** (lifted by minus the unrisen part of its height, no shadow until it stands, the arms overhead while the fists break through);
* **glow**: the Guardian's glow colour and level come from the abyssal table (`AbyssalLooks.Look.glow`, `glowLevel(phase)`), a Foundry mob's brightens with its tier and its wind-up, a puppet pulses;
* **death**: the vanilla fall, with the fade of the old renderers; an undead abyssal look crumbles to ash and soul fire as before.

## 7. Performance (the owner asked)

| what | before | now |
|---|---|---|
| second skin layer and hair of a far character | always drawn | `PlayerLod`: beyond `detailDistance` (24 blocks) the second layer (hat, jacket, sleeves, pants: `KitPlayerModel.setOuterVisible`) and the hair are skipped; beyond `farDistance` (64) every additive glow pass too. The body, the armour, the held item and the extras' solid pass stay (the silhouette). One comparison of squared distances per frame |
| culling | the kit's frustum test | kept; the layers are only drawn for a body that was drawn (frustum culling happens before `render`) |
| renderer instances, model parts | one `PlayerModelRenderer` (a wide and a slim `Body`) per entity type | the same; the Foundry's 314 people share ONE renderer and one `Body`, and the looks are records in one table |
| look of an id | one map get | the Foundry body caches the look per id until the next reload (`PlayerAssets.generation`): no string is built per frame (`"foundry_" + id` is interned once in `DungeonLooks`) |
| skin / glow / extras texture locations | a key string (`"skin:" + name`) concatenated per call, per layer, per entity, per frame | one `HashMap` per kind keyed by the plain name: no allocation (`PlayerAssets`) |
| extras model lookup | a resource location built per extra per frame | one map get and one identity check (`ExtraModels.of`) |
| `ExtrasLayer` | a `Motion` record per entity per frame | the primitives are passed through (`ExtrasDraw.draw` overload) |
| tint wrapper, decoration object | a `TintedBuffers` and an anonymous `Decoration` per frame | one of each per renderer, re-aimed per frame |
| hair palette | a record per frame per entity | a 64-slot direct-mapped cache (`HairPalette.cached`) |
| the skin of a mob | a model of 100 to 400 cubes drawn with GeoLite | the vanilla player model (6 boxes, 6 overlay boxes) plus only the mob's extras |

## 8. Edits to existing files (all additions or one-word changes)

* `client/remodel/RemodelRenderers.java`: `PROVIDERS` + `providerOf(type)` + one line in `put`.
* `client/playermodel/PlayerModelRegistry.java`: one guarded call to `DungeonRenderers.register`, the client config registration in `init`.
* `client/playermodel/PlayerLook.java`: `stance` component (a 13-argument constructor stays); `PlayerAssets`, `PlayerModelRenderer`, `KitPlayerModel`, `TintedBuffers`, `ExtraModels`, `ExtrasDraw`, `ExtrasLayer`,
  `HairLayer`, `GlowLayer`, `HairPalette`, `PlayerBody.Dynamic` (`noShadow`), `PlayerModelAudit` (reads every table), `PlayerModelSelfTests` (`render.playermodel2`): the internals of section 7 and the new rules.
* `tools/playermodel/extract_extras.py`: `extract(...)` takes `src`, `geo`, `anim_src`, `tex_src`, `tex_images`, `kinds`, `compact` (the old call is unchanged), more prop tilts (mace, trident, scythe, shield);
  `tools/playermodel/gen_skins.py`: `paint_character(..., post=None)`.
* No file of another builder: nothing in `NUSMP`, the sound code, loot, bosses data, rifts or the admin commands.
* New: `client/playermodel/` `DungeonLooks`, `DungeonBodies`, `DungeonRenderers`, `DungeonPoses`, `DungeonAudit`, `PlayerLookTables`, `PlayerModelOptions`, `PlayerLod`, `Stances`; assets `playermodel/looks_dungeon.json`,
  `playermodel/dungeon_legacy.json`, `textures/entity/playermodel/{foundry_,dg_,corpse_}*`, `living_armor*`, `floor_warden*`, `goliath*`, `geo|animations|textures/entity/pmx_*`; `tools/gen_dungeon_skins.py`,
  `tools/dungeon_skins/`; `tools/lang/humanoid_p2.json`; tests `DungeonLooksTest`, `DungeonSkinsTest`, `PlayerLodStanceTest`; previews `docs/previews/humanoids/`.

## 9. Tests

* `DungeonLooksTest` (entity id to look id to an existing skin for all eight entity types, all four decay stages, every Foundry id of the asset index, every abyssal look in every phase; the flag's rule; the
  loader and the add-only merge; the stance column), `DungeonSkinsTest` (64x64 PNGs, lit glow maps, a filled base layer, a used second layer for the archetypes, 50+ distinct archetype skins, no two Foundry
  mobs with one skin, arm width of every look against its skin, every extras model well-formed with a texture of its size, the person / creature books add up to 400), `PlayerLodStanceTest` (the level of detail, the
  stances, every Goliath pose finite and in range, the throw, the rise), and the existing `PlayerModelDataTest` (now auditing all three tables), `GeoAssetsTest`, `AbyssalAssetsTest`, `AbyssalLooksTest`,
  `FoundryRosterTest`, `FoundryIndexTest` all pass.
* Self-test `render.playermodel2` (runs on the dedicated server in the CI smoke; add nothing to the workflow if `render` is already in `SMOKE_EXPECT`): `DungeonAudit`, every converted entity type is registered,
  every ally of the arcs has a look, every loaded boss profile that names a look resolves in each of its three phases, the flag rule.

## 10. The WHY, class by class (the owner's standing request)

* `DungeonLooks` (pure): the mapping from "what the server says" to "which skin" is the part that breaks without a compiler noticing, so it is plain values, interned, and tested for every id; `usesKit` is the
  flag's rule in one place so the registration and the tests cannot disagree.
* `PlayerLookTables` (pure): three generators write three files; one merge rule (earlier wins, duplicates reported) keeps one boss from changing its face because another builder reused an id.
* `DungeonBodies`: one small class per kind of body that only says what the entity already says (look, size, glow, cast, pose); the kit does the rest. Bodies read synced data, never the server.
* `DungeonRenderers`: a Split per type, because the owner wants the old renderer kept and switchable and some of the types hold both kinds (a Guardian of a creature look, a Foundry creature).
* `PlayerLod` and `PlayerModelOptions`: a crowd costs frames; the far second layer, hair and glow are the cheapest things to drop and the least visible; the numbers are the owner's to change.
* `Stances`: the same spine for a goblin and a paladin would make them one race; a posture costs nothing at draw time and reads at once.
* `wear.py` and `recipes.py`: a clean skin from a sheet is a character; a worn one is somebody who lives in a dungeon. Data, not code, so a family's wear is one line.
* `parts.py`: the head box is the one thing the player model does not borrow from the art kit; a part is a few boxes and a grey texture, so one part serves every colour.
* `DungeonAudit`: nothing of this can be seen by its author; a file that is missing, a mob that is both a person and a creature, a boss phase nobody drew are all found without a person.

## 11. Adding a person later

* **A Foundry mob**: add its row to `tools/foundry/rows/*.txt` and run the foundry generator as before, then `python3 -B tools/gen_dungeon_skins.py foundry --only <id>` (a person gets its skin, extras and look; a
  creature goes into `foundry_class.LEGACY` with a reason). `render.playermodel2` fails until the mob is one or the other.
* **A new family**: one line in `tools/dungeon_skins/archetypes.py` (the roster's language) and, if it needs its own wear, a recipe in `recipes.py`; run the `archetypes` step.
* **A boss of the Decennial layer**: name an existing look in the profile's `look` (it is drawn as that boss). A NEW humanoid look is the three entries `abyssal_<look>_p1..p3` of the kit's format in
  `playermodel/looks_abyssal.json` (or `looks_decennial.json`) plus the look's row in `assets/nusmp/abyssal/looks.json` (`kind: humanoid`); a new creature look is a geo model and needs nothing in the kit.
* **A switch back**: set `playermodel.dungeon = false` in `config/nusmp-playermodel-client.toml`.

## 12. Not verified (nothing visual could be run)

* the look in the game: light and colours (the previews are a re-implementation), the second layer's thickness against the armour layer's, the extras' placement on slim arms, the tint of the head parts at
  game lighting, the stances' angles, the Goliath's poses at six blocks, whether a stride of one over the scale looks natural at 6 blocks;
* `LivingEntityRenderer` applying the SCALE attribute before `scale()` (the Foundry body divides it out, the Guardian body divides it out of the hitbox fit: both assume it is applied once);
* the shadow radius of a scaled body (the dispatcher may or may not multiply it by the scale);
* `Config registration from the mod container` in `PlayerModelRegistry.init` (it follows the pattern of `AvatarInventoryMenus`; a failure is caught and logged);
* the CI smoke: `render.playermodel2` has never run in the server (it uses `ArcCharacters.bootstrap`, `DndProfiles.all`, `BuiltInRegistries`);
* the type check reports only the stub artefacts (abstract vanilla classes the stubs cannot instantiate, `setupAnim` of the stubbed `HumanoidModel`).
