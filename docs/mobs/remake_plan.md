# Remake of all NPCs and mobs: audit and phased plan (phase 0, round 2.00)

Owner request: "also remake all npc and mobs in my mod they look bad" (models and silhouettes, skins and textures, animations, faces and expressions; in phases).
This file is phase 0: the inventory, the quality verdict, a shared style guide, the budgets, the tooling plan and the phases. It builds nothing and changes no game file.
It only adds `docs/mobs/**` and two read-only helpers in `tools/mobs_audit/` (`asset_census.py`, `geo_stats.py`); `docs/mobs/inventory.csv` is the census output.

## 0. The plan in plain words

1. **What there is.** 68 entity types of this mod (about 140 more things are effect props or armour pieces and are not mobs). They hide about 700 distinct bodies: 40 hub / story NPCs,
   23 arc bosses (20 people, 3 monsters, most with a second form), the Zogratis family, 6 dungeon bosses, one `foundry_mob` type that is 400 different mobs, one `labyrinth_guardian`
   type that is 200 Decennial bosses drawn with only 22 body designs, and a handful of summons, puppets and spirits. Mob art is small: about 23 MB of the 450 MB jar (5 percent).
   The jar is made fat by weapon textures (about 203 MB), the Djinn set (59 MB) and the Erza armour (17 MB), not by mobs.
2. **What looks bad, and why.** Three different problems, not one:
   * Most people are drawn on the **vanilla player model with a 64x64 skin** (the 1.94.0 and 1.99.0 "player-model kit"). That gives the "Steve with a painted shirt" silhouette for 41 NPCs, 20 arc
     bosses, 314 dungeon people and 9 of the Decennial boss designs. The richer art-kit models they replaced (400 to 800 quads, painted faces, real wings) are still in the jar as the fallback.
   * The **"creatures" are not creatures**: the 86 Foundry "creatures" (pup, owlbear, black pudding, tarrasque shell walker, griffon scout, treant ...) are the same humanoid art-kit rig, so a pup is a shirtless man.
     Real animal / serpent / insect / slime bodies exist for only about 10 entities.
   * The **Decennial bosses repeat**: 200 bosses, 78 archetypes, 21 bodies; the holy road's 100 bosses use 6 bodies and 58 of them are the same Solar. Several designs are flat slabs for wings and boxes for bodies.
   * Textures are pixel-art noise (ordered dither plus noise in `tools/arc_kit/core.py`), faces are 8x8 or a single painted expression, animation is a handful of shared clips, so nobody shows an emotion.
3. **What to build.** One shared "anime kit" (generators from tables, built from the Salamander / Sylph / Compass / Recombination tooling) plus one small client engine (a generic anime-mob renderer
   with 3 levels of detail, face plates for expressions and a frame budget), then every entity as a table row, in this order:
   * **Phase E (engine and pilots):** the kit, the renderer, 3 pilot characters and a style sign-off by the owner. 3 builders.
   * **Phase 1, named talking characters:** 41 NPC bodies, 20 arc bosses (40 looks with the second forms), the avatar kit. 62 entities, 4 lanes.
   * **Phase 2, bosses and signature monsters:** Zogratis and Order bosses, Rades puppets, dungeon bosses, summons, and the 22 Decennial designs x 3 phases (covers all 200 Decennial bosses). 40 entities + 66 boss models, 7 lanes.
   * **Phase 3, the dungeon's 400 Foundry mobs:** 12 new creature rigs first, then 10 lanes of 40 mobs (holy and abyssal road x tier 1..5). 400 mobs.
   * **Phase 4, the rest:** 16 celestial spirits, 36 tiny djinn polish, the 78 Decennial archetype variants, and (only if the owner agrees) own replacements for the vanilla / Tensura mobs of the dungeon. About 130 entities, 4 lanes.
4. **Budget.** Target +60 MB for the whole remake (hard cap +120 MB), 3 levels of detail everywhere, a shared quad budget per frame, no gameplay change (hit boxes, attributes and AI stay).
5. **Rule 2 and the old art.** Everything is added behind a client switch and the old renderers stay; the owner has now explicitly asked for a replacement, so a last cleanup phase may delete old assets only after sign-off.
6. **Decisions only the owner can take** are in section 11 (the biggest: leave the vanilla player rig for named characters; replace vanilla / Tensura mobs in the dungeon; the jar cap).

## 1. What was done, and what could not be done (honest)

Done: grepped every `EntityType.Builder` registration and `ArcEntities.boss(...)` call; read the renderer registrations (`RemodelRenderers`, `PlayerModelRegistry`, `DungeonRenderers`, `ArcRenderers`, ...);
read `docs/playermodel.md`, `docs/playermodel_phase2.md`, `docs/arc/art_kit.md`, `docs/labyrinth/foundry.md`, `decennial_bosses.md`, `docs/magic/sylph.md`, `fire_casting.md`, `recomb_anime.md`, `compass_3d.md`;
parsed the roster data (400 Foundry rows, 200 Decennial profiles, 22 abyssal looks, the 28 encounter tables, the entity tags); counted every geo / texture / animation file and its deflated size
(`tools/mobs_audit/asset_census.py`, `geo_stats.py`); rendered 28 samples with the repo's own software rasteriser (`tools/render_preview/preview.py`, 12 s start + about 1.5 s a scene) and looked at them;
looked at the committed contact sheets of the player-model kit (`docs/previews/playermodel/contact_sheet.png`, `humanoids/*.png`) and the Sylph / Salamander / Tiny Djinn sheets as the quality reference.
The four contact sheets I made are in `docs/mobs/samples/`.

Could NOT be done / not verified:
* Nothing was seen in a game. All pictures are the project's software rasteriser (GeoLite through stubs): colour, light and the vanilla layers (armour, held items, shadow) differ from the game. The player-model kit
  was judged from its own committed previews (made by `tools/playermodel/preview.py`, a re-implementation), not re-rendered by me.
* I rendered 1 to 3 per type, not every entity; the verdicts per type are from those and from the committed sheets. The 400 Foundry mobs were judged from 8 renders and 8 committed contact sheets (`docs/previews/humanoids/foundry0..7.png`).
* No canon reference was fetched (rule 10, English wiki first, Japanese wiki as fallback): no network research was part of this task. Every lane must do it for its characters and list sources.
* The 450 MB jar figure is the lead's. The only jar in the tree (`dist/multiverse-of-anime-0.20.0.jar`, 1.2 MB) is a stale stub, so my MB numbers are deflate estimates of the resource files (a jar stores PNG about as is and JSON about 7 times smaller).
* Spawn visibility is read from data (encounter tables, tags, config weights), not measured in play. How many NPCs stand in the hub at once was not checked.
* Quad counts are cubes x 6 (an upper bound: a card counts 6 too). Per-frame cost was not measured (nothing can run here); the budgets of section 7 are copied from what the owner already approved for the Salamander and Sylph.
* AI, attributes, spawn logic, sound sets and hit boxes were not audited (they are not part of the request and stay untouched).
* The estimates for effort, size and CI time (sections 8 and 10) are estimates from the existing generators (arc kit 1 to 3 s per model, Foundry skins 45 s for 385, previews 1.5 s a scene).

## 2. Inventory

### 2.1 Entity types by group (68 entity types registered by the mod)

"Looks" = distinct bodies a player can see (forms, phases and decay stages counted). Renderer = the one that wins at run time today (the player-model kit registers last; the flag `client.playermodel.dungeon` /
`playermodel` gives the old art-kit renderer back).

| Group | Entity types (count) | Characters / looks | Spawned by | How visible | Drawn today by |
|---|---|---|---|---|---|
| A. Hub and story NPCs | `arc_npc` (1 type, 37 characters + `npc_default` fallback), `quest_giver_npc`, `merchant_npc`, `tower_wizard` | 41 bodies. Black Bulls 14, knights and court 16, DanMachi friends 6, Old Witch 1 | `HubNpcSpawner` / `HubPosts` / `ArcSpawner` when sagas open, spawn eggs, `/summon` | **Highest**: the hub is where every player lives; they talk | player-model kit (`looks.json`, 64x64 skins); art kit `arc_<id>` as fallback; hub pair was a vanilla villager model |
| A2. Avatars | `player_avatar` (1 type, parametric), `mirror_double` | one body per player-made avatar (16 hair, 8 horns, 6 wings, outfits, races from data) | creation GUI, Mirror Magic | High in the hub and parties | vanilla `PlayerModel` + composed skin + 23 kit hair styles; mirror double wears the owner's skin (kept on purpose) |
| B. Arc bosses | 23 types: 20 people (heath_grice, vetto, lotus_whomalt, mars, revchi_salik, meliodas, merlin, king, escanor, ban, diane, gowther, fana, rhya, rades_spirito, patolli, lucifero, morris_libardirt, zagred, udaeus) + 3 monsters (minotaur, black_goliath, amphisbaena) | 20 x 2 forms + 3 x 2 = 46 looks | arena via Chronicle sigil (`ArcArenaDirector`), floor bosses (`ArcFloorBosses`, `ExistingBossSpawner`) | Medium: one fight per boss, but it is the "story" content | player-model kit for the 20 people; art kit `ArcBossRenderer` for the 3 monsters |
| C. Zogratis / Order / devil | `dante_zogratis`, `vanica_zogratis`, `zenon_zogratis`, `lucius_zogratis`, `dark_disciple` (4 devil colours), `zogratis_clone`, `blood_beast`, `god_of_order`, `devil_avatar`, `zagred_legacy` (10 types) | about 15 looks (base / hybrid / divine, forms) | boss summons, eggs, floors | Medium / rare | kit; `blood_beast` art-kit geo |
| D. Dungeon bosses and guardians | `goliath`, `charybdis_spawn`, `magicule_scale`, `floor_warden`, `labyrinth_guardian`, `spirit_king` (6 types) | goliath 2, charybdis 2, scale 1, warden 1, guardian default 1, king 3 phases | floor bosses, seals, every tenth floor | Medium; guardians seen every 10 floors | Goliath, warden: kit (`dg` skin of a stretched player model); Charybdis, king: GeoLite |
| D2. Decennial bosses | the `labyrinth_guardian` type; 200 profiles (100 holy, 100 evil) | **22 bodies x 3 phases = 66 models**; 9 humanoid looks (113 bosses), 13 creature looks (87 bosses) | boss floor of each level 1..100 / -1..-100, rifts | **Rare per player, memorable**: every boss floor | humanoid looks: kit (27 skins `abyssal_<look>_p<n>`); creature looks: `AbyssalRenderer` GeoLite (39 models) |
| E. Dungeon mobs | `foundry_mob` (1 type = **400 mobs**: 314 people, 86 "creatures"), `living_armor`, `dungeon_slime`, `dungeon_direwolf`, `basilisk` (5 types) | 400 + 4 | Dungeon Master: 60 percent of each encounter's mobs become a Foundry mob of the floor's tier and road (config `weight`), living armor in 26 of 28 themes (42 slots) | **The most seen mobs after the hub NPCs**, floors 1..1000 in 5 tiers of 200 | kit for the 314 people + living armor; `FoundryRenderer` GeoLite (art-kit people with 100 to 400 cubes) for the 86 "creatures" |
| F. Rades puppets | `jimmy`, `alfredo`, `carl`, `david`, `michael_caesal` (named corpses, 4 decay stages), `soul_zombie` (a vanilla `Zombie` subclass) | 5 x 4 + 1 = 21 | Soul Corpse magic | Rare, player-owned | kit; soul zombie uses the vanilla zombie model |
| G. Summons, spirits, pets | `spirit_lord`, `elemental_spirit_lord` (4 elements), `wind_spirit_lord`, `painted_construct` (knight / hound / giant), `grimoire_daemon` (lesser, greater, arch, stone), `cotton_sheep` (+cook), `cotton_cloud`, `rouge` | about 17 | magic casts, books, Zagred | Medium; seen all game by their owners | GeoLite art kit (`KitMobRenderer`); knight on the kit; Sylph / Salamander already rebuilt (not in scope) |
| not mobs | `magic_prop` (149 prop kinds = effect models), `thread_trap`, `carriage`, `notice_of_order` | | | | prop painters; done separately |
| Reused, not ours | 47 vanilla / Tensura entity ids in the 28 encounter tables (30 vanilla, 17 Tensura; the top: vex 59 slots, living armor 42, evoker 21, iron golem 16, cave spider 15), tags `dungeon/tier1..3` (72 entries, 24 of them Ice and Fire), 13 vanilla kinds as Decennial adds (skeleton, zombie, vex, spider, blaze, witch, drowned ...) | | the Dungeon Master | **Very high**: 40 percent of encounter mobs plus every boss add | vanilla / Tensura / Ice and Fire models. This plan cannot change them without new entity types (decision 4, section 11) |

Not mobs, kept out of this plan (they share the geo folder): Erza requip armour (288 geo, 576 textures, 17 MB), Fairy Tail armour (70 geo), Spirit Dive armour (44), Recombination armour, Djinn weapons and Djinn armour, weapon models.
The 36 Tiny Djinn (`tinydjinn_*`, client actors beside the player), the 16 zodiac celestial spirits (`celestial_*`, summons) and the Sylph / Salamander are creatures but already at or near the target style (see 3.6).

What `pmx` means: **p**layer-**m**odel e**x**tras (my reading; no doc spells it out). `pmx_<id>` is the set of non-body bones (horns, wings, tails, halos, capes, floating weapons) cut out of an art-kit model and hung on the vanilla player
rig by `tools/playermodel/extract_extras.py` (94 for the story cast, 308 `pmx_foundry_*`, 60 `pmx_dg_*`, 27 `pmx_abyssal_*` = 489 geo files; the first four `pmx` rows of the census below). They are not separate characters.

### 2.2 Asset census (`python3 -B tools/mobs_audit/asset_census.py`; full CSV: `docs/mobs/inventory.csv`)

Mob art only. "defl." = deflated size (what the jar stores). Textures are PNG, so raw is about the jar size.

| Group (file prefix) | geo files | geo MB raw / defl. | texture files | texture MB | animation files | anim MB raw / defl. | notes |
|---|---|---|---|---|---|---|---|
| `arc_*` (94 characters + 30 second forms) | 124 | 9.16 / 0.59 | 260 (skin + glow) | 0.59 | 124 | 26.52 / 3.43 | avg 68 cubes (409 quads), 512 px wide sheets of 64 to 256 px height, 9 clips each, the fallback renderer of group A, B, C, G |
| `foundry_<id>` (400 bodies) + 107 shared class clips | 400 | 8.27 / 1.33 | 800 | 1.76 | 107 | 7.14 / 1.62 | avg 61 cubes (366 quads), 512 x 64..256 sheets; one shared animation file per class (107 classes) |
| `pmx_foundry_*` extras | 308 | 1.59 / 0.34 | 616 | 1.35 | 308 | 1.94 / 0.46 | avg 14 cubes (84 quads) |
| `pmx_*` (arc extras) | 94 | 1.21 / 0.11 | 200 | 0.45 | 94 | 3.49 / 0.47 | |
| `pmx_dg_*` archetype extras and head parts | 60 | 0.26 / 0.06 | 108 | 0.19 | 48 | 0.26 / 0.06 | 55 archetypes + 12 head parts |
| `pmx_abyssal_*` | 27 | 0.72 / 0.11 | 54 | 0.49 | 27 | 1.83 / 0.30 | 9 humanoid looks x 3 phases; avg 78 cubes |
| player-model skins (`textures/entity/playermodel`) | | | 815 (792 skins + glow, 23 hair sheets) | 1.37 | | | 513 looks: 101 `looks.json`, 27 abyssal, 385 dungeon; 64x64 |
| `abyssal_<look>_p<n>` (22 looks x 3 phases) | 66 | 2.70 / 0.38 | 132 | 2.98 | 22 | 2.39 / 0.42 | avg 120 cubes (720 quads as cubes x 6), Tarrasque 340 cubes; the previewer draws 834 quads per pass for it |
| `zogratis_*` | 12 | 0.90 / 0.09 | 32 | 0.42 | 12 | 1.50 / 0.19 | avg 135 cubes; 812 quads |
| `rades_*` / `corpse_*` | 8 | 0.08 / 0.01 | 69 | 0.41 | 8 | 0.27 / 0.03 | 4 decay skins each |
| Goliath, Charybdis, scale, Tower Wizard, direwolf, basilisk | 12 | 0.39 / 0.03 | 29 | 0.29 | 12 | 1.44 / 0.16 | |
| `celestial_*` (16 spirits, gate, king x 3) | 20 | 0.16 / 0.02 | 40 | 0.24 | 20 | 0.10 / 0.01 | |
| `tinydjinn_*` | 36 | 0.95 / 0.09 | 144 | 0.31 | 1 | 0.03 / 0.00 | 64x64 sheets |
| `sylph_*`, `spirit_*` (spirit lords) | 21 | 0.87 / 0.05 | 36 | 1.40 | 13 | 0.67 / 0.06 | the new quality reference (Sylph 0.34 MB small, 0.65 MB powered, with LODs) |
| `avatar_*` (hair, horns, wings, extras) | 38 | 0.17 / 0.02 | 95 | 0.05 | 30 | 0.02 / 0.01 | |
| **Mob art total (about)** | **~1,230** | **~27 / ~3.4** | **~3,450** | **~12.3** | **~830** | **~48 / ~7** | **about 23 MB in the jar** |
| Not mobs: weapons, Djinn, Erza, ft, spiritdive, VFX models | | | | 203 (weapon + weapon_vu) + 59 (djinn) + 17 (erza) + 6 (VFX) | | | the real jar weight; `tools/optimize_pngs.py` (oxipng) exists |

Animations: art-kit characters have the clips `idle walk run attack cast cast_big hurt transform death` (+ `spawn`, `chest_gem_reveal`); the Foundry shares 107 class files; player-model bodies use the vanilla biped
animation plus the extras' clips; abyssal looks 8 clips; arc files are big because every bone is keyed at 1 ms (26.5 MB raw for 124 files; 214 KB each): re-baking at 30 fps would cut load memory with no look change.

Hit boxes today (server data, stay as they are): NPCs 0.6 x 1.95; avatar 0.6 x 1.8; Foundry mobs 1.41 to 3.09 high (size attribute); Goliath 6 blocks; boss and puppet sizes in `ArcLook` / `NamedCorpses`.

### 2.3 The rendering stacks that exist (so the plan reuses them)

| Stack | Where | What it is | Reuse |
|---|---|---|---|
| Player-model kit | `client/playermodel/**` | vanilla `PlayerModel` (6 boxes + 6 overlay boxes) + 64x64 skin + extras + 23 hair styles, vanilla armour, held items, `PlayerLod` | keep for the player-like avatar; its `Split` dispatcher (kit or old renderer per entity, by config) is the pattern for the new renderer |
| Art kit | `tools/arc_kit/**`, `client/remodel/KitMobRenderer`, `client/geo/GeoDraw` | GeoLite cubes, a spec per character, hair, outfit, IK clips, painted 32x32 face canvas, 512-wide pixel-art sheets | the only complete table-to-model generator with clips (94 characters, 400 Foundry mobs) |
| Prop painters (Sylph, Salamander, Compass, Recombination) | `client/prop/**`, `tools/sylph_geo_lib.py`, `firecast_geo_lib.py`, `sylph_art.py`, `gen_*_geo.py` | cubes + alpha-cut cards with explicit uv windows, 4x paint reduced, cel look, LOD x3, glow-only model, shared frame quad budget, `FlatLit` | the quality standard; the engine has to be lifted from props into an entity renderer |
| Abyssal creature geo | `tools/abyssal/*`, `client/remodel/AbyssalRenderer` | 22 looks x 3 phases hand-written per look (`look_*.py`) | models are weak (3.4); the generator structure (look files + phase tables) is reusable |
| Foundry generator | `tools/foundry/*`, `tools/dungeon_skins/*` | rows (8 fields) to 400 mobs; guard against look duplicates; wear recipes; archetype parts | the model for "an entity is one table row" |
| `PlayerLookTables` | `client/playermodel/PlayerLookTables` | several look files merged add-only, earlier wins, duplicate reported; `looks_decennial.json` reserved | copy the pattern for `anime/looks_<lane>.json`, so parallel lanes never touch one file |

Limits of the engine to remove first (phase E): `GeoDraw` has no per-bone hide (needed for face plates driven by entity state), no per-bone tint (palette variants), no LOD swap, no shared budget for entities,
and `FlatLit` is package-private in `client/prop`. `KitMobRenderer` draws no held item on its locators.

## 3. Quality verdict by type

Pictures: `docs/mobs/samples/*.png` (mine) and the committed sheets named in each row. "Reference" = the Sylph, Salamander, Tiny Djinn standard.

### 3.1 Humanoid named characters (groups A, B, C): player-model kit, verdict **poor / generic**
* Silhouette: all 101 looks are the same 4-head vanilla body with a 64x64 skin; height, shoulder width and head size never change. Asta, Noelle, Yami and the Hub villagers read as recolours (`docs/previews/playermodel/contact_sheet.png`).
  Costumes live on a one-pixel-thick second layer: capes, coats, skirts, hair are box layers, nothing flows.
* Face: 8x8 pixels of the head front, one fixed expression, no brows that read, no eyes that look at anything. This is the biggest loss of identity (a named character is mostly a face).
* Texture: the art kit's painter (`core.py`: ordered dither 0.045..0.06 + noise 0.16) puts grain on every cloth and fur: see Merlin's coat and hair in `docs/arc/preview/arc_merlin_front.png`. Not cel: no outline, no flat bands.
* Animation: vanilla walk / swing; casts and talks are `PosePatterns` (arm lifts). No faces, no idle personality, no emote.
* Note the regression: the art-kit models that were replaced (`arc_<id>`, 1800 to 3800 vertices: coats, spiked hair, floating weapons) have MORE shape than the player-model version (`sample_art_kit_characters.png`
  next to the contact sheet). The remake must not go back to either: it takes the kit's shape ambition and the cel look.

### 3.2 Humanoid generic people (the 314 Foundry people and 55 archetypes): **poor**
* Same player body, skins painted by a generator from one row. A "stone pillar wrestler" and a "pale fawn messenger" differ by skin and a hat. 69 are bald or masked, 245 have a hair style from 23.
* Extras (horns, wings, tails) help the silhouette, but wings are flat fans and tails are sticks (`docs/previews/humanoids/foundry0.png`).
* Goblins and kobolds get a hunch; skeletons and zombies a reach pose; no real rig difference (no digitigrade legs, no four arms, no big heads).
* The roster guard promises 400 non-alike mobs; at game scale most read as one of ~12 shapes.

### 3.3 Creatures and beasts (86 Foundry "creatures", Direwolf, Basilisk, Slime, Charybdis): **very poor for the 86, mixed for the rest**
* The 86 Foundry creatures are people (`sample_foundry_creatures_are_people.png`): `bramble_pup` is a shirtless boy with spiky hair, `thornhide_owlbear_brawler` a shirtless man with shoulder pads, `black_pudding_glutton` a grey man,
  `tarrasque_shell_walker` a robot, `griffon_scout` a goggled man with a feather fan, `heartwood_treant_warden` a man covered in leaves. Slimes (13) and insects (14) have a "slime" or "insect" faction but a humanoid body.
  The data says "creatures stay on their own models": the models were never made.
* Dungeon Direwolf, Basilisk: real quadruped / reptile boxes with decent painted skins but low detail and flat rest poses; the Slime is a cute cube with a face and the best-looking small mob.
* Charybdis (`sample_dungeon_creatures_old_geo.png`): readable octopus-like silhouette with glow; fine as a base.

### 3.4 Undead and puppets: **poor to fair**
* Jimmy, Alfredo, Carl, David, Michael: kit bodies with 4 decay skins (stitches, rot, bone, veins); the shamble stance helps, the faces are a few pixels. The soul zombie is the vanilla zombie.

### 3.5 Constructs, golems, armour bodies: **mixed, with one regression**
* Old Goliath geo (`sample_dungeon_creatures_old_geo.png`, 1512 vertices): readable mossy stone giant with cracks, the best-looking mob in the mod after the Salamander. The current default for it is the kit's stretched
  vanilla body with a 64x64 skin at 3.2x scale (`docs/previews/humanoids/dungeon_bodies.png`), which throws that detail away. This is a regression to undo first (quick win 1).
* Living Armor / Floor Warden: acceptable plate look on the kit; flat violet runes; no weight in the walk.
* Spirit Gnome (earth spirit): a brown block with two eyes.

### 3.6 Spirits, companions (already close to the target)
* Sylph, Salamander (rebuilt), Tiny Djinn (36 chibi companions, one 64x64 sheet each, `docs/previews/tiny_djinn/contact_dark.png`): readable, expressive, consistent. They are the reference; Tiny Djinn only needs face plates and the sheet size rule.
* Orb spirit lords, cotton sheep and cloud, grimoire daemon (a grey box face with a book): fair, low detail.

### 3.7 Bosses (Decennial and abyssal): **poor, repetitive**
* 200 bosses on 21 bodies. Holy road: Solar 58, Bahamut 20, Tarrasque 10, Lich King 7, Zariel 4, Kraken 1. Evil road: Beholder 9, Strahd 8, Balor 8, Vecna 7, six looks with 5 to 6 each. Variety is a tint and a scale.
* Humanoid looks (`docs/previews/humanoids/abyssal_humanoids.png`): Strahd, Asmodeus, Zariel, Orcus and Solar wear detached flat slabs and fans as wings; faces are skull pixels; the three phases differ by glow only.
* Creature looks (`sample_abyssal_foundry_bosses.png`): Tarrasque is a brown brick horse with a box head (6672 vertices over two passes in the previewer, which warns above 6000); Dracolich is a thin skeleton with slab wings
  (`docs/previews/abyssal/dracolich_phases.png`); Beholder is the best (a readable eye tyrant) but stiff; Kraken and Lolth were not rendered.
* Animation: 8 clips per look, phases share them.

### 3.8 Arc monsters: **fair**: Minotaur (3216 vertices) has a bull head, horns, muscles in the art-kit style and is not bad; Amphisbaena and Black Goliath not rendered.

### 3.9 Which entities reuse vanilla or Tensura models

* Vanilla villager model (the Hub pair) until 1.94.0; now the kit. `soul_zombie` = vanilla `Zombie` renderer. `tower_wizard` attributes borrow the Witch's.
* All 47 vanilla / Tensura ids in the encounter tables, the tag tiers (72 entries) and 13 add kinds are not modelled by this mod at all.
* Every kit-drawn body is the vanilla `PlayerModel` geometry (a modded skin on a vanilla model): 41 + 20 + 314 + 27 + 21 + others = about 450 bodies.

## 4. Shared style guide ("anime cel", from the Salamander / Sylph / Compass / Recombination work)

This is a proposal; the owner approves it on the three pilots (phase E) before mass production.

### 4.1 Look rules (all types)
* **Silhouette first, texture second, geometry third**: head, hair, shoulders, hands, tail, horns, wings are shape; skin, cloth folds, armour plates, scales are painted. Cubes for limbs and torso (a limb is a box laid between two joints, as `Model.limb`),
  alpha-cut **cards** (single quads) for hair strands, ruffles, capes, wings, spikes, ears, leaves.
* **Cel shading**: flat colour per material, one hard shadow band, one light band, no noise, no dither, no gradient except in glows. Painted at 4x and reduced (as `sylph_art.py`), so lines are clean.
  The renderer wraps the consumer in `FlatLit` (upward normal: no vanilla grey diffuse); block / sky light still applies, so caves stay dark.
* **Outline rule**: a painted outline, 1 texel (at final size) on every silhouette edge of every card and cube face region, in a darker hue-shifted tone of the material (never pure black, except eyes and pupils);
  line weight x2 for bosses at LOD0. **No inverted-hull outline** (doubles quads, rule 11).
* **Palette rule**: at most 5 hue families per character; each family 3 tones (base, shadow shifted toward violet, light shifted toward warm yellow, the `color.py` ramps) + the outline tone. Skin 3 tones; hair base + shade + one highlight band.
  Sheets are saved palettised (PNG8 when at most 256 colours: hero 128, standard 64) so they compress.
* **Glow**: a separate additive `_glow` sheet (eyes, runes, cracks, blade), half size, safe-additive rule respected (`tools/safe_additive/scan_textures.py`); at most 3 glow regions per character; no particles from the model.
* **Reference first**: each character is checked against canon art (English wiki first, Japanese as fallback, rule 10) and the lane doc lists the sources; no copying of frames, a stylised likeness.

### 4.2 Proportions and hit boxes (model height may differ from the hit box by about 10 percent; hit boxes never change)

| Type | Heads tall | Head size vs vanilla | Build notes | Hit box (unchanged) | Example |
|---|---|---|---|---|---|
| Hero NPC (named, talks) | 4.0 to 4.5 | 1.15x, face plate 10 x 8 units | slim limbs 3 to 4 units, hair volume +3 units, one signature silhouette item (cape, hat, coat, weapon) | 0.6 x 1.95 | Asta, Noelle, Yuno |
| Child / small NPC | 3.0 to 3.5 | 1.3x | | 0.6 x 1.95 (model scaled 0.7 to 0.85) | Charmy, Liliruca, Hestia |
| Humanoid boss | 5.0 to 6.0 | 1.0x | broad shoulders or wing span as the silhouette, 2 to 3 form variants | per `ArcLook` | Meliodas, Escanor, Lucifero |
| Generic dungeon person | 4.0 | 1.1x | one of 6 body templates (slim, average, broad, lanky, huge, small) + head / hair / outfit kit | Foundry size attr | cultist, paladin, bandit |
| Brute / ogre / giant | 3.5 (huge head and fists) | 1.0x | shoulders 1.4x, hands 1.5x | 1.4 to 3.1 | ogre, stone giant |
| Quadruped beast | body length 2.2x shoulder height | head 1.2x | 4 jointed legs (2 segments), tail chain of 3, ears as cards | 0.9 to 1.6 | direwolf, pup, owlbear |
| Serpent / worm | 10 to 14 segments | | segment chain with trailing delay | 1.6 x 1.0 | basilisk, wurm |
| Insect / arachnid | | | 6 or 8 legs of 2 cubes, wings as cards | 0.5 to 1.5 | beetle, spider queen |
| Slime / ooze | | | squash-and-stretch on one core cube + 2 to 4 wobble cubes, face plate on the front | 0.5 to 2.0 | slime, pudding |
| Spirit / wisp / floating | | | no legs, tail or skirt chain, large glow, face plate | | lantern fae, daemon |
| Flier / dragon | | | wing = arm + 4 finger bones + 4 membrane cards (the Salamander wing) | 1.6 to 5 | dragons, griffon, roc |
| Giant construct | 3.0 | | cubes only, big moss / crack textures, no face plate (an eye slot) | 6 (Goliath) | Goliath |

### 4.3 Face plates and expressions (the Sylph system, generalised)
* The face is one painted picture per expression, drawn on a thin card in front of the head cube (`face_<name>` bones, one quad each, plus one quad of glow where the eyes shine). Hidden plates are not drawn
  (the Sylph scales them to 0.001 in the clip; the engine gets a bone mask so the entity's state can choose the plate).
* Plate size: hero 32x24 texels at final size (placed in a 128-wide face strip), standard 16x12, creatures an eye pair 12x6. The face strip sits in the same sheet (no extra texture binding).
* **Expression set (hero, 10 plates)**: `neutral`, `smile`, `talk_a`, `talk_b` (mouth open small, big), `blink`, `angry`, `hurt` (eyes shut, teeth), `focus` (cast: glowing eyes), `surprised`, `down` (death / dazed).
  Optional per character: `smirk`, `cry`, `blush`, `rage`. Bosses add `phase2` and `phase3` (glow, different brows). Standard people: `neutral`, `angry`, `hurt`, `blink`, `talk` (5). Creatures: `open`, `narrow`, `closed`, `glow` (4).
* **Driver (client only, from synced state, no server cost)**: neutral by default; `talk_a` / `talk_b` alternate every 3 ticks while the dialogue line plays (the NPC's `talk` clip already exists); `angry` while it has a target; `hurt` for 10 ticks after a hit
  (`hurtTime`); `focus` while a cast or wind-up is synced; `down` on death; `blink` for 3 ticks at a random interval of 60 to 140 ticks seeded from the entity UUID (stable, no allocation); `surprised` on spawn.
* Hair is cards, never a cube cap: bangs, side locks, back locks, with swing driven by the clip (idle sway) and the entity velocity (a clamped scalar passed to the clip as a time offset, no spring simulation per frame).

### 4.4 Animation sets per type (clip names; existing names are kept, new ones appended)

| Type | Clips |
|---|---|
| Biped hero NPC | `idle`, `walk`, `run`, `talk`, `emote_wave`, `emote_think`, `emote_laugh`, `attack`, `cast`, `hurt`, `death`, `sit` |
| Biped boss | `idle`, `walk`, `run`, `attack`, `attack_b`, `cast`, `cast_big`, `hurt`, `stagger`, `transform`, `death`, `spawn` (existing: idle walk run attack cast cast_big hurt transform death) |
| Generic person | `idle`, `walk`, `run`, `attack`, `cast`, `hurt`, `death` from one shared clip file per body template and weapon pose (the Foundry already shares 107 class files) |
| Quadruped | `idle`, `walk`, `run`, `bite`, `pounce`, `howl`, `hurt`, `death`, `sleep` |
| Serpent / worm | `idle`, `slither`, `strike`, `coil`, `hurt`, `death` |
| Insect / arachnid | `idle`, `skitter`, `bite`, `hurt`, `death` |
| Slime | `idle` (breathing squash), `hop`, `lunge`, `split`, `hurt`, `death` |
| Flier / dragon | `hover`, `flap`, `glide`, `dive`, `breath`, `hurt`, `death` |
| Giant | `walk`, `slam`, `stomp`, `roar`, `rise`, `hurt`, `death`, `transform` |
| Spirit | `hover`, `drift`, `cast`, `hurt`, `vanish` |

Rules: every clip is a pure bone-rotation / position clip from the generator (IK for arms, as `tools/arc_kit/clips.py`), baked at 30 fps (not 1 ms), loops seamless, one file per rig family, not per character (the 26 MB of arc animation shrinks to about 1 MB).
Attack timing keeps the contract of `docs/arc/art_kit.md` section 9 (the hit lands at 80 percent of the wind-up) so server timing and attributes stay untouched.

## 5. Per-type quad budgets, LOD and culling (rule 11)

Copied from what the owner already approved: Sylph small 365 / 199 / 66 quads, Salamander 700 / 300 / 120, golem 448 / 158, shared frame cap 3200, own form never dropped.
Today's mobs for comparison (cubes x 6, an upper bound; the previewer draws fewer, e.g. Merlin 230 quads a pass for 68 cubes): arc characters 409 average (max 155 cubes), Foundry bodies 366, abyssal 720, Tarrasque 834 drawn a pass.

| Class | LOD0 / LOD1 / LOD2 quads | LOD distances (blocks, best quality) | Cull beyond | Glow-only model | Texture sheet (final) |
|---|---|---|---|---|---|
| Hero NPC | 900 / 400 / 140 | 20 / 40 / 64 | 80 | 20 quads (LOD0, LOD1) | 1024 x 640 max; lod1 half, lod2 quarter |
| Humanoid boss (and 2nd form) | 1300 / 600 / 200 | 32 / 64 / 96 | 128 | 40 quads | 1024 x 768 max |
| Giant / dragon boss | 1700 / 750 / 240 | 48 / 96 / 144 | 160 | 60 quads | 1024 x 1024 (shared with its LODs) |
| Generic dungeon person | 450 / 220 / 90 | 16 / 32 / 56 | 64 | 10 quads | 512 x 384, family atlas shared (see 6.3) |
| Beast / insect / slime | 260 / 130 / 60 | 12 / 24 / 40 | 48 | 8 quads | 256 x 256 family atlas |
| Tiny companion | 400 / 200 / 70 | 12 / 24 / 48 | 56 | 10 quads | 64..256 |

* **Shared frame budget for entity models**: `AnimeLod.FRAME_QUADS = 9000` for all anime mobs on screen together (3 times the prop budget because a fight is several bodies): nearest first at the level their distance asks for, stepped down
  until it fits, then culled; the camera's own party / the targeted boss is never dropped (booked past the cap as in `QuadBudget.force`). Worst legal frame: 30 standard people at LOD0 = 13,500, stepped to about 9000 + at most one forced boss (1700): bounded, tested with a pure JUnit.
* Effect-quality setting (`PerfSettings.vfxQuality().detail`) scales the distances (0.5 to 1) as for the Sylph. Frustum culling stays vanilla's. Glow pass only within LOD1 distance. Nothing allocated per frame (static scratch arrays, primitive math, one `FlatLit`).
* Server cost of the remake: **zero** (client art only; hit boxes, attributes, AI, packets unchanged). Draw calls per mob: body, glow-only body (when near), nothing else; no particles; no per-frame texture upload.
* Memory: parsed models are cached per rig family and per LOD, not per mob (one `GeoModelData` per family: 12 families x 3 LODs instead of 400 x 1).

## 6. Generator and tooling approach (everything from tables)

### 6.1 What exists and is reused
* `tools/arc_kit/` (spec dict to geo + clips + sheet + scenes, 94 characters, `gen_arc_characters.py --check` guard), `tools/foundry/` (8-field rows to 400 mobs, guard against look duplicates), `tools/dungeon_skins/` (wear recipes, parts),
  `tools/abyssal/` (a file per look), `tools/gen_tiny_djinn.py` (36 from one table), `tools/gen_spirit_lords.py`.
* `tools/firecast_geo_lib.py` (`Atlas`, `Model.limb / card / box`, rotation frames, `sine` and `merge` track helpers), `tools/sylph_geo_lib.py` (shelf-packed `Atlas` with explicit uv windows, `Model.cube`, `write_animations`), `tools/sylph_art.py`
  (`Canvas` / `Layer` painting at 4x: `face(kind)`, `hair_block`, `fringe`, `strand`, `ruffle`, `wing`, `choker`), `tools/gen_salamander_geo.py --count / --sync` (the quad table written into Java and checked by a test),
  `tools/geo_builder.py`, `tools/geo_check.py`, `tools/optimize_pngs.py` (oxipng, pixel-checked), `tools/render_preview` (`--scenes file`, `$RENDER_PREVIEW_BUILD`, works offline in 30 s for a dozen scenes).

### 6.2 New: `tools/anime_kit/` (phase E builds it; lanes only add table rows)
* `atlas.py` / `model.py`: one `Atlas` + `Model` (merge of the Sylph and Firecast ones; `cube`, `limb`, `card`, `chain`), `lod.py` (builds LOD1 and LOD2 from the LOD0 spec: merges card pairs, drops secondary cubes by priority, halves the sheet) and `budget.py` (counts quads, fails over budget).
* `rig_biped.py`, `rig_quad.py`, `rig_serpent.py`, `rig_insect.py`, `rig_slime.py`, `rig_flier.py`, `rig_spirit.py`, `rig_giant.py`: about **12 rig families** (8 modules; quad = wolf / cat / bear / boar / stag / rat, insect = beetle / arachnid, flier = bird / dragon, ...) with their clip sets (4.4). A rig is built once per body template and cached.
* `cel.py` (flat-band painter, outline pass at 4x, palette ramps from `arc_kit/color.py`, PNG8 quantiser with a colour cap), `face.py` (expression plates: generalises `sylph_art.face`), `hair.py` (card styles, 32 existing style names re-implemented as cards),
  `outfit.py` (coats, capes, robes, armour plate sets as cards + boxes), `wings.py` (membrane wing from `gen_salamander_geo`).
* `tables/<lane>.json`: one entry per entity: `{id, rig, build, height, palette{...}, face{set, eye_color, marks}, hair{style, color}, outfit{...}, props{...}, glow{...}, forms[...], lod_class, clips}`; the arc kit's `SPEC` dicts are converted by a one-time script (the sheets `tools/arc/char_*.py` already carry skin, hair, outfit, colours: no re-authoring of 94 characters from nothing).
* `build.py <lane|id>` writes `geo/entity/anime_<id>[_lod1|_lod2|_glow].geo.json`, `textures/entity/anime_<id>*.png`, rig-family `animations/entity/anime_rig_<family>.animation.json`, `assets/nusmp/anime/looks_<lane>.json`, scenes for the previewer; `--check` regenerates to a temp folder and fails on any difference (as the arc kit).
* New names only (`anime_*`): nothing existing is touched; old `arc_*`, `foundry_*`, `pmx_*`, `abyssal_*` stay (rule 2).

### 6.3 What is generated from tables, per population
* **Named characters (Phase 1, 2)**: table row per character = spec; hand-touch only the signature part (a function `extra(kit, rig)` as in the arc kit) and the face plates of canon expressions (hand-tuned eyes per character).
* **Foundry people (314)**: 6 body templates x head kit (12 heads: human, elf, dwarf, orc, goblin, kobold, lizard, wolf, bird, skull, helm, hood) x hair (20 card styles) x outfit (30 pieces) x weapon: a mob is a row, as today; the 400 sheets collapse to
  about 14 **family atlases** (body + outfits as grey-scale regions) plus one small 128 x 128 **decal sheet** per mob (face plates, emblem, hair card colours baked); per-mob colour comes from per-bone tint (engine feature), not from 400 full sheets.
* **Creatures (86)**: the 12 rig families x skin recipes (scale, fur, chitin, ooze, bark, feather) from `dungeon_skins/recipes.py`; per-mob: proportions numbers + palette + 1 to 3 signature parts (horns, shell, wings).
* **Decennial (22 looks x 3 phases)**: hand-built per look (they are the set pieces) on the biped / dragon / aberration rigs, each phase a variant table (extra parts, glow, size).
* **Tiny Djinn, celestial**: their own tables already exist; only face plates and sheet rules added.

### 6.4 Checks (so a lane cannot ship a bad entity)
`AnimeBudgetTest` (pure JUnit reading every `anime_*` geo: quads per LOD at or under the class budget, sheet size, colour cap, outline present on every plate, expression set complete for its class, clip set complete for its rig, glow sheet half size),
`AnimeLookTablesTest` (every look resolves, no duplicate id across lane files), existing `GeoAssetsTest`, `SafeAdditiveTextureScanTest`, `ServerSafetyTest` (the renderer is under `client/`), self-test `render.anime` (data only, runs on the dedicated server;
add `render.anime` to `SMOKE_EXPECT` in `smoke.yml`), `PerfSourceScanTest` (no allocation in the draw path), plus `tools/anime_kit/build.py --check` per lane.

## 7. Engine work (Phase E, Java, client only)

* `client/anime/AnimeMobRenderer<T>`: reads a look id from a `AnimeBody` (like `PlayerBody`: `look(e)`, `form(e)`, `expression(e)`, `size(e)`), picks LOD from distance and the `AnimeLod` budget, draws body + glow-only through `GeoDraw` with `FlatLit`, per-bone tint, bone mask for plates,
  held item at locators `item_r` / `item_l`, death fade, hurt flash. Hit boxes untouched.
* Registered through a `Split` per entity type (existing `PlayerModelRegistry.Split`): anime renderer when `client.anime.<group>` is true (default true once a lane ships), otherwise the type's previous renderer (kit or art kit): the old stays and switches back without a new jar.
* `AnimeLookTables` (merge add-only of `assets/nusmp/anime/looks_*.json`), `AnimeLod` (pure: bands, frame budget, own form), `FacePlates` (pure: state to plate id, blink seed), `AnimeOptions` (client config `nusmp-anime-client.toml`: `enabled`, per-group switches, `detailDistance`, `frameQuads`; looks only, so a CLIENT config, rule 9).
* Appends to `GeoDraw` / `GeoModelData`: `Predicate`-free bone mask (a `long`), per-bone colour multiplier, nothing that changes existing callers. `FlatLit` made public in a shared `client/render` package (copy, not move, so the prop painters are untouched).
* Self-test `render.anime`, `PerfProbe` counter `anime.quads`.

## 8. Phased plan

Lane rule: at most about 40 entities per lane; a lane writes only `tools/anime_kit/tables/<lane>.json`, `anime_<its ids>.*` assets, `assets/nusmp/anime/looks_<lane>.json`, `docs/mobs/<lane>.md`, `tools/lang/anime_<lane>.json`;
nothing else, so lanes never overlap. The lead adds the one-line registrations in shared files (`PlayerModelRegistry.extend`-style hook, `SelfTests`, `NUSMP`), append-only. Concurrency: the disk has about 5 GB free and a full worktree is about 1 GB
(`src` 947 MB): run at most 3 lanes at once or use sparse worktrees (`git worktree add --no-checkout`, then sparse-checkout `src/main/java`, `tools`, `docs`, `src/main/resources/assets/nusmp/anime` and only the lane's own asset names).

### Phase E: engine, kit and 3 pilots (must come first; 3 builders; about 1 round)

| Lane | Work | Entities |
|---|---|---|
| E1 engine | section 7 plus tests and self-test | none |
| E2 kit | `tools/anime_kit` core, 3 rig families (biped, quadruped, giant), cel painter, face plates, lod, budget, preview scenes | none |
| E3 pilots | one hero NPC (Asta: from `char_asta.py`), one beast (a hell hound on the quad rig, replaces the Foundry `bramble_pup` / direwolf look), one boss (Goliath, to compare with the old geo) | 3 |
Deliverables: contact sheet per pilot (front, back, 3/4, face-plate strip with all expressions, 6 clips as strips, LOD0/1/2 side by side with quad counts, next to the old render), a budget table, `docs/mobs/phaseE.md`. **Owner sign-off on the style before Phase 1.**
Risks: the engine is untested in a game (client rendering is only seen in the software rasteriser); the bone-mask and tint appends touch `GeoDraw` (hot path; keep them additive and measured by a pure test); the style may need two rounds.
Generated from tables: the pilots (3 rows) and the 5 non-pilot rigs of E2 are code.

### Phase 1: named talking characters (62 entities, about 81 looks; 4 lanes, parallel)

| Lane | Entities (exact) | Looks |
|---|---|---|
| 1A Black Bulls + hub (18) | yami_sukehiro, asta, noelle_silva, finral_roulacase, magna_swing, luck_voltia, charmy_pappitson, vanessa_enoteca, gordon_agrippa, zora_ideale, gauche_adlai, grey, secre_swallowtail, nacht_faust; `quest_giver_npc`, `merchant_npc`, `tower_wizard`, `npc_default` | 18 |
| 1B Knights, court, DanMachi (23) | charlotte_roselei, jack_the_ripper, dorothy_unsworth, julius_novachrono, marx_francois, fuegoleon_vermillion, mereoleona_vermillion, leopold_vermillion, yuno, william_vangeance, mimosa_vermillion, klaus_lunettes, kahono, kiato, nozel_silva, solid_silva; ais_wallenstein, bell_cranel, eina_tulle, hestia, liliruca_arde, welf_crozzo; black_market_witch | 23 |
| 1C Arc bosses, saga 1 and 2 (12) | heath_grice, vetto, lotus_whomalt, mars, revchi_salik, rades_spirito, fana, rhya, patolli, zagred, morris_libardirt, lucifero | 24 (2 forms) |
| 1D SDS bosses + avatar (9) | meliodas, escanor, ban, merlin, gowther, king, diane, udaeus (8 bosses, 16 looks) + the avatar kit (`player_avatar`: 16 hair, 8 horns, 6 wings, outfit parts, race tints, face plates for 6 moods; parametric, keeps the saved appearance data) | 16 + kit |
Preview deliverables: per lane a contact sheet of all characters (front + 3/4 + face strip) at 3 camera distances; for the bosses the two forms side by side; a clip sheet for 5 samples; one talk sequence (talk_a / talk_b) strip; LOD sheet; the old and the new in one picture for 5 characters.
Risks: canon likeness (rule 10 research per character; the lane doc lists sources and what could not be fetched); NPC count in the hub at once (budget); the avatar must stay compatible with saves (`AppearanceData` ids 0..15 unchanged); a talking NPC's face is the first thing every player sees, so the owner should review lane 1A before the others finish.
From tables: all of it except the signature `extra()` parts (about 25 characters have one: weapons, wings, halos) and the hand-tuned eyes. The arc-kit `SPEC` of each character is converted by script, so the identity (colours, hair, outfit) is not re-invented.

### Phase 2: bosses and signature monsters (40 entities + 66 Decennial models; 7 lanes)

| Lane | Entities (exact) | Looks / models |
|---|---|---|
| 2A Zogratis, Order, devil (12) | dante_zogratis, vanica_zogratis, zenon_zogratis, lucius_zogratis (base + hybrid / divine each), dark_disciple (4 colours), zogratis_clone, blood_beast, god_of_order (2 forms), devil_avatar (2 forms + Megicula skin), zagred_legacy (2 forms) | about 18 |
| 2B Rades puppets (6) | jimmy, alfredo, carl, david, michael_caesal (4 decay stages each), soul_zombie (own renderer instead of the vanilla zombie) | 21 |
| 2C Dungeon bosses and monsters (14) | goliath (2), charybdis_spawn (2), magicule_scale, spirit_king (3 phases), floor_warden, labyrinth_guardian (default body), living_armor, minotaur (2), black_goliath (2), amphisbaena (2), dungeon_slime, dungeon_direwolf, basilisk | about 20 |
| 2D Summons and spirits (8) | spirit_lord, elemental_spirit_lord (earth gnome, water undine; fire and wind are the new Salamander / Sylph and stay), wind_spirit_lord, grimoire_daemon (4 kinds), painted_construct (knight, hound, giant), cotton_sheep (+cook), cotton_cloud, rouge | about 17 |
| 2E Decennial humanoids | looks: lich, acererak, vecna, strahd, zariel, asmodeus, orcus, lich_king, solar, each 3 phases (covers 113 bosses) | 27 models |
| 2F-1 Decennial dragons and giants | bahamut, tiamat, dracolich, tarrasque, kraken, each 3 phases (covers 52 bosses): one dragon rig from the Salamander big form, tarrasque on the giant rig | 15 models |
| 2F-2 Decennial fiends and aberrations | balor, hezrou, vrock, quasit, demogorgon, beholder, elder_brain, lolth, each 3 phases (covers 35 bosses; quasit is a look of adds and small fiends) | 24 models |
Preview deliverables: per look the 3 phases in one row (as `docs/previews/abyssal/*_phases.png`), idle / attack / cast_big / death strips, size comparison with a player (a boss that must feel big), LOD sheet, a before / after pair.
Risks: Decennial hit boxes and arena heights depend on the model size (keep hit boxes, scale the model to them); the 3 phases must stay readable at 60 blocks; transform clips must line up with the server's phase change (existing timing contract); the dragon rig must be shared (one rig, not 5 hand models) to stay in the budget; `labyrinth_guardian` already has a `Split` (kit for humanoid looks, `AbyssalRenderer` for creatures): the anime renderer takes both.
From tables: 2A, 2B, 2D mostly (tables + signature parts); 2C, 2E, 2F are hand-built set pieces on shared rigs (the look files stay one file per look, as `tools/abyssal/look_*.py`, but on the new kit).
Quick check for 2E / 2F: with these 22 designs done, all 200 Decennial bosses change at once; Phase 4D adds the per-boss variants.

### Phase 3: the 400 Foundry mobs (12 rigs first, then 10 lanes of 40)

| Lane | Work | Mobs |
|---|---|---|
| 3.0a creature rigs | quad (hound, wolf, cat, bear, boar, stag, rat), bird / griffon, serpent / wurm | rigs for the 31 beasts + parts of the 41 beast-faction people |
| 3.0b creature rigs | slime / ooze, insect, arachnid, plant / treant, spirit / wisp, aquatic | rigs for 13 slimes, 14 insects, 15 elementals, 1 aquatic |
| 3.1 holy tier 1 / 3.2 abyssal tier 1 | floors 1..200 (the most played) | 40 + 40 |
| 3.3 holy tier 2 / 3.4 abyssal tier 2 | floors 201..400 | 40 + 40 |
| 3.5 / 3.6 tier 3 | floors 401..600 | 40 + 40 |
| 3.7 / 3.8 tier 4 | floors 601..800 | 40 + 40 |
| 3.9 / 3.10 tier 5 | floors 801..1000 | 40 + 40 |
Faction mix of the 400 (so rig choice is known): cultist 88, beast 72 (31 creatures), construct 56, elemental 42 (15 creatures), undead 39, demon 34, aquatic 21, ogre 15, insect 14 (all creatures), slime 13 (all creatures), goblin 6; archetypes caster 68, brute 62, assassin 53, tank 49, support 44, summoner 37, charger 29, flier 24, skirmisher 23, archer 11.
Note: the lanes by tier each hold people and creatures; a lane's creatures wait for 3.0a / 3.0b (build the rigs first, one round earlier).
Preview deliverables: contact sheets of 40 (4 views each, the face strip row for people), rows by faction, an encounter group picture (5 mobs of one theme side by side with the player for scale), a LOD sheet per rig family, `tools/dungeon_skins`-style duplicate guard report (no two mobs alike: the existing roster guard must still pass).
Risks: look duplicates (the roster guard promises 400 different mobs; keep `tools/foundry/guard.py` and add a perceptual-hash check on the final sheets), per-mob decals and tints need the engine's per-bone tint (phase E), 30-mob encounters (frame budget), CI time (400 models in the asset tests), review fatigue (400 pictures: review by faction sheets, not one by one).
From tables: **everything**: rows already exist (`tools/foundry/rows/*.txt` carry name, faction, archetype, source concept, animation class, size, glow); the generator maps archetype + concept to a rig, head kit, outfit and palette. About 10 to 15 percent (the "famous" names: gold saint, dragon god, sorcerer king, aboleth, archangel ...) need a hand-made signature part.

### Phase 4: everything left (about 130 entities; 4 lanes; independent, any order)

| Lane | Work | Entities |
|---|---|---|
| 4A celestial and companions | 16 celestial spirits (aquarius, aries, cancer, capricorn, crux, gemini, horologium, leo, libra, lyra, nikora, pisces, sagittarius, scorpio, taurus, virgo) + gate; Tiny Djinn polish (36: face plates, sheet size rule only) | 53 |
| 4B Decennial variants | 78 archetypes of the 200 bosses get a crest / weapon / aura / colour kit on top of the 22 bodies (data and small parts; also a holy-road re-mix: 58 Solars become 6 variants) | 78 variants |
| 4C Dungeon wildlife replacement (**only if the owner says yes**, decision 4) | own entity types (about 20) for the 30 vanilla and 17 Tensura ids of the encounter tables and the 13 boss-add kinds, on the existing rigs (skeleton, zombie, ghoul, spider, bat, wolf, bear, golem, imp, wisp ...) and tags / encounter data pointing at them. Needs server work (entity types, attributes, spawn data): it is NOT a client-only change | about 20 types |
| 4D Cleanup and report | when the owner has signed off: remove the old art-kit / kit assets that nothing uses any more (`arc_*`, `foundry_*`, `pmx_*`, 22 `abyssal_*` bodies ... about 23 MB), keep every id and data component, final jar report, final contact sheets, docs | |
Risks: 4C changes gameplay data (`dungeon/tier*.json`, 28 encounter files, `AddKinds`) and needs the dungeon builders' review; 4D breaks rule 2 unless the owner has asked for it explicitly (she / he did ask to "remake", but delete only on a clear yes).

### Totals

| | Entities | Looks / models | Lanes |
|---|---|---|---|
| Phase E | 3 pilots | 3 | 3 |
| Phase 1 | 62 | about 81 | 4 |
| Phase 2 | 40 + 22 Decennial designs | about 76 + 66 models | 7 |
| Phase 3 | 400 + 12 rig families | 400 | 12 |
| Phase 4 | about 53 + 78 variants (+ about 20 types if 4C) | about 130 | 4 |

## 9. Quick wins (cheap, visible, none changes gameplay; each is a switch or a regeneration)

1. **Goliath back on its own model.** The old `goliath_body` geo (stone, moss, cracks) is clearly better than the stretched player body. Make the `playermodel.dungeon` flag per entity type (the `Split` already knows both renderers) and default the Goliath to the old one. One config line and one registration line.
2. **`FlatLit` on the kit and art-kit renderers** (make it public, wrap the consumer): removes the vanilla grey diffuse so painted colours read as painted. Cheap, immediate "less murky" look on every NPC; check the armour layer.
3. **Face card on the player-rig characters**: one extra model (a single quad, 32 x 24 texels) in front of the head layer, using the existing extras machinery (`pmx_face_<id>`): a real face with eyes and brows for the 41 NPCs and 20 bosses before the full remake. About one quad per character.
4. **Stop the pixel noise**: set `noise` and `dither` to 0 in the art-kit painters (`tools/arc_kit/core.py` `fabric`, `leather`, `fur`, `feather`) and run `gen_arc_characters.py` / `gen_skins.py`: flat cel bands instead of grain, for 94 characters and 400 Foundry sheets, geometry untouched. Needs an owner eye on 3 characters.
5. **Real wings for the Decennial humanoids**: Strahd, Zariel, Asmodeus, Orcus and Solar use flat slabs. One membrane wing model (from `gen_salamander_geo`'s wing) shared by 5 looks x 3 phases changes 87 of the 200 bosses.
6. **13 Foundry slimes on the existing Dungeon Slime rig** (cube + face, already good) with a tint, instead of a humanoid: a table field `rig` in `dungeon_legacy.json` read by `FoundryRenderer`. Same idea later for the direwolf rig and the 31 beasts.
7. **Optimise PNGs**: run `python3 -B tools/optimize_pngs.py` on `textures/entity/{foundry,arc,pmx,abyssal}` (small, lossless) before adding new art, so the new files start from the same discipline. (Savings on mobs are about 1 MB; the 203 MB of weapon textures are where a jar diet would matter.)
8. **Bake the arc animation files at 30 fps**: 26.5 MB of JSON becomes about 2 MB of load-time memory, no visual change.

## 10. Size, disk and CI estimates

Per entity, in the jar (deflated; PNG is already compressed):

| Class | Low (palettised PNG8, family atlas, shared clips) | High (RGBA sheets as the Sylph) |
|---|---|---|
| Hero NPC (sheet + lod1 + lod2 + glow + geo) | 0.15 MB | 0.40 MB |
| Boss form | 0.25 MB | 0.65 MB |
| Generic dungeon person | 0.04 MB | 0.12 MB |
| Beast / insect / slime | 0.02 MB | 0.06 MB |

| Phase | Low | High | Hard cap I propose |
|---|---|---|---|
| E | 1 MB | 3 MB | 4 MB |
| 1 (41 hero NPCs, 40 boss forms, avatar kit) | 17 MB | 45 MB | 25 MB |
| 2 (about 76 looks, 66 Decennial models) | 20 MB | 80 MB | 35 MB |
| 3 (314 people, 86 creatures, 12 rigs) | 14 MB | 45 MB | 25 MB |
| 4 | 6 MB | 25 MB | 15 MB |
| **Total** | **about 58 MB** | about 200 MB | **+100 MB (target +60 MB)** |

If nothing old is removed the jar goes from about 450 MB to about 510 MB (target) or 550 MB (cap); the cleanup (4D) can take back about 23 MB. Rules that keep it there:
1. A sheet is at most 1024 px wide; hero 1024 x 640, standard 512 x 384, creatures 256 x 256; LOD textures are half and quarter; glow half.
2. Palettised PNG8 whenever at most 256 colours (cel art almost always is); `tools/optimize_pngs.py` on every lane before it reports done; a test fails a sheet over its class cap.
3. One animation file per rig family, baked at 30 fps; one atlas per body family, a 128 x 128 decal sheet per mob; no per-mob sheet above 256 KB.
4. No new 4096 or 2048 anything; no per-form full copies where a tint or a decal does.

Disk (the build machine has about 5.4 GB free; the working tree is about 1 GB): the new generated resources add about 60 to 100 MB of loose files per the table (geo json is 7x larger raw than in the jar; budget 3x of the MB above for the tree); keep scratch in the previewer's `$RENDER_PREVIEW_BUILD`
folder outside the tree and delete it after the lane. The 4x paint canvases are held in memory (a 1024 x 640 sheet at 4x is 40 MB as float; generate one sheet at a time).

CI: build time grows with the asset tests that walk every geo / texture (`GeoAssetsTest`, new `AnimeBudgetTest`): estimate +1 to 2 minutes for about 1500 new files; the smoke test limit is 70 minutes and `render.anime` is data only (a few seconds).
Generation: arc kit 1 to 3 s per model (about 8 minutes for 94 serial); Foundry 400 about 5 minutes; previews about 1.5 s per scene plus 12 s start (a 40-entity sheet: about 3 minutes).

## 11. Decisions that only the owner can take

1. **Leave the vanilla player rig for named characters?** I recommend yes: custom anime rigs (proportions, hair, faces) for named NPCs, bosses and creatures; the player rig stays only for the player-like Avatar and for anything that must wear vanilla armour.
   Cost: custom rigs do not wear vanilla armour; weapons come from the model (and held items at locators); a flag per group keeps the old look. If the owner prefers to stay on the player rig, the plan shrinks to face plates + better skins + extras (about a third of the work) but the silhouette stays.
2. **Style sign-off** on the 3 pilots (proportions 4 to 4.5 heads, painted outlines, flat bands, face plates) before Phase 1: yes / adjust.
   **Owner, 2026-10-26: not signed off yet, wants changes; the notes on what to change are still to come.** Until then the pilots stay previews and no Phase 1 work starts.
3. **Jar cap**: +60 MB target, +100 MB cap, or a different number; may textures go to 2048 for the big bosses (I advise no).
4. **Owner, 2026-10-26: the dungeon uses the base Tensura mobs (the Tensura: Reincarnated entity list the owner sent). Phase 4C (own replacements for the vanilla / Tensura encounter mobs) is dropped; the remake covers only this mod's own characters and monsters that Tensura does not have.** The hell hound and Goliath pilots stay as drafts and are used only where no Tensura mob fits. Original question: **Vanilla and Tensura mobs in the dungeon** (30 + 17 ids, 40 percent of encounter mobs, all boss adds): replace with own mobs (server change, Phase 4C), re-skin them client-side only inside the dungeon (not possible without touching the vanilla renderers, I advise against), or leave them. Also: raise the Foundry share (`weight`, now 60) so more of what players meet is remade.
5. **Delete the old art** after the remake (Phase 4D)? The request says remake, rule 2 says keep: a clear yes is needed.
6. **Order**: hub first (as planned), or the dungeon first (Phase 3 tiers 1 to 2, the most met mobs)? Phase E must come first either way.
7. **Creature scope**: are the 86 "creatures" to become real animals (this plan) or may some stay person-shaped (a "pup" as a small beastman)?
8. **Decennial variety**: is 22 bodies + 78 variant kits enough, or does each of the 200 bosses need its own body (about 10 times the work)?
9. **Canon likeness vs original design** for the Black Clover / DanMachi / SDS characters: stylised likeness (this plan, with wiki research per rule 10), or looser original designs.
10. Whether **chibi** (3 heads, like the Sylph) or **4 to 4.5 heads** is the house style; the pilots can show both for Asta.
11. Which **expression set** per class is wanted (hero 10 plates is my proposal) and whether NPC faces should react to dialogue (talk plates) or stay static.

## 12. Rules checklist for every lane

Rule 2: new assets are `anime_*`, old ones stay, a client switch gives the old renderer back. Rule 3: no Time VFX files touched. Rule 5: every item in a lane doc is marked NEW (new look) or REPLACED (the look it replaces: the entity id and data stay).
Rule 9: client only, no `Minecraft.getInstance()` outside `client/`, config is a CLIENT config, nothing new on the wire, `ServerSafetyTest` untouched. Rule 10: sources named per character, unfetched ones listed. Rule 11: section 5 budgets, `AnimeBudgetTest`, worst case stated in each lane doc
(entities per frame, quads per frame, draw calls). Rule 12: before each lane starts, say in plain words what is built, how it is read and what is not included; previews are sent to the owner as each lane finishes; the lead does version, changelog, `[smoke] [strict]`, CI and the pre-release.
Rule 7 (weapon models) applies where a mob holds a mod weapon: use the real weapon mesh. Rule 6 (engravings) does not apply: no new weapons.

## 13. Files made by this audit

* `docs/mobs/remake_plan.md` (this file), `docs/mobs/inventory.csv` (census), `docs/mobs/samples/*.png` (4 contact sheets, palettised, 0.9 MB together)
* `tools/mobs_audit/asset_census.py`, `tools/mobs_audit/geo_stats.py` (read-only helpers: `python3 -B tools/mobs_audit/asset_census.py [--csv file]`, `python3 -B tools/mobs_audit/geo_stats.py`)
* No game code, asset, data or config file was changed; scratch renders were kept outside the tree and deleted.
