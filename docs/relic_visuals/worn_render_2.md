# Worn relics, second pass (1.99.0)

Owner request: "Relics are no longer rendering visually on the player model. Restore the previous functionality so that relics properly show on the player's body when equipped." This is the second
time (the first pass is `docs/relic_worn_fix.md`, 1.95.0, self-test `relic.worn_render`), so this pass starts from the question the first one did not ask: not "does every relic have a model file"
(it does: 512 worn relics, 400 with a model of their own, 112 by slot and look, 0 without) but "what has to be true, at run time, on the client, for a model to reach the screen, and where can that fail
without a word".

Nothing here could be run in a game. What is below as PROVED is proved by reading the code, by the unit tests, by the data (geometry, catalog, assets) or by a type check against API stubs; what is SUSPECT
is a cause that fits the symptom and that only a run can confirm. The tool that settles it is `/multiverse relic debug` (section 6).

## 1. Result in one table

| # | Finding | Status | Fix |
|---|---|---|---|
| F1 | **A named mob's pack never reaches a client.** The rings, charms, cloaks, belts, necklaces and gloves a named mob, an arc character or a player's Avatar wears live in the entity's persistent data (`nusmp_mob_relics`, `MobRelics.pack`, `AvatarTrinkets`). Persistent data is never sent to a client, so the client always read an empty pack: those relics were never drawn on anyone, on any client, whatever the layer did. (`KitRelicLayer` of 1.95.0 could not have shown them either.) | **PROVED** (code: `MobRelics.pack` reads `getPersistentData()`; `RelicHolderSync` documents the same fact; `WornRender2SelfTests` builds a pack and reads it on the server) | `WornSync`: the server tells every client what a wearer carries outside the vanilla equipment (Curios slots, pack); the client merges it with its own look (`WornMerge`, `Relics#addSynced`) |
| F2 | **The glowing parts of every mirrored piece were culled.** A left arm / left leg piece is the right one drawn through a matrix that flips x (determinant -1), which reverses the winding of every quad. The solid pass uses a no-cull render type, the emissive pass uses `RenderType.eyes`, which culls back faces: every gem, rune and flame of a mirrored piece vanished. 142 mirrored pieces of the worn relics carry 3464 glowing quads. | **PROVED** for the geometry; the culling premise is vanilla's source (`RenderType.eyes` sets no cull state, so the default, culling on, applies; not run) (geometry: all 20140 glowing quads of the 491 model files are wound outward, so the unmirrored pass is fine and the mirrored one is exactly inverted; `WornAssetsTest.theGlowingPartsOfEveryModelAreWoundFrontFacing`, `WornMesh.corner`) | the corners of a mirrored quad are written in the other order in the emissive pass (`WornLibDraw.glow`) |
| F3 | **The layer could be absent, silently.** It reached a player renderer through one event only (`EntityRenderersEvent.AddLayers`, once per renderer build) and only for `instanceof PlayerRenderer`. A renderer swapped, wrapped or rebuilt by another mod after that event had no layer, and nothing said so. | design hole PROVED, that it happened SUSPECT | `RelicLayerGuard`: the first time a player is drawn by a renderer the event never reached, the layer is attached then (once, logged); `RelicClient.addLayers` logs what it added and refused |
| F4 | **A catalog read that failed was remembered for the session.** `WornModels.catalog()` stored an empty catalog when the file was not readable yet, and `WornLibrary` then cached "no model" for every relic it was asked about (also for armour items, modes and Curios items) until the next resource reload. | hazard PROVED, that it happened SUSPECT | an unreadable catalog is not kept (empty answer, retried after 3 s), the library caches nothing derived from it, a model read that fails because the resource manager is not ready is not cached either |
| F5 | **One failed Curios read blanked every worn Curios relic for ten seconds, and a missing method was fatal for the read.** `CuriosLink.equipped` used a single road (`getEquippedCurios`) and latched `brokenUntil` on any exception; the failure was logged at most per ten seconds, so a permanent failure filled the log and told nobody which step broke. | hazard PROVED, that it happened SUSPECT | a second road (each slot type's own stack handler), `getRenders` optional (without it every worn stack counts as shown), one log line per distinct reason, `CuriosProbe` walks the calls step by step |
| F6 | The relic renderers of the boss relics were told "first person" whenever the camera was first person, also in the inventory preview of the own body. | PROVED (code) | `WornPlan.viewOf`: first person only for the own body with no screen open |
| F7 | Pieces anchored on an arm hung one pixel off a slim arm. | PROVED (vanilla model: slim right arm x -2..1, wide -3..1) | `WornPlan.SLIM_INSET` half a pixel toward the body for arm pieces of slim wearers |
| S1 | The Curios reflection (`CuriosApi.getCuriosInventory`, `ICuriosItemHandler.getEquippedCurios`) fails against the Curios build in the owner's game. It was written in 1.15.0 "and has never met the real mod" (CHANGELOG 1.15.0); the 1.46.0 fix repaired one such failure. | SUSPECT | `/multiverse relic debug` prints step 1 to 5 of the read on the server AND on the client; F5 and F1 make the failure survivable |
| S2 | Another mod replaces the renderer the player is drawn with. | SUSPECT | F3's guard and its log line; the debug report says how many renderers the guard had to fix |
| S3 | A Curios eye icon is off for the slot. | SUSPECT | the report says `EYE_OFF` for the relic |
| S4 | Curios is not installed or does not give the player the slots `necklace`, `back`, `belt`, `hands`, `charm`. The mod lists Curios as optional. | SUSPECT | step 0 of the probe says "Curios mod: NOT LOADED" / "no Curios inventory for this entity" |

None of F1..F7 by itself explains "my own character shows no relic although everything is installed and healthy" except F1 for characters that wear a pack. If the cause on the owner's machine is S1..S4, the
first run of `/multiverse relic debug` names it.

## 2. How a relic becomes "worn" (investigation point 1 and 5)

`Relics.wornList(entity)` is the one list every consumer reads (the worn layer, the particles, the HUD key ring, the engine). It is cached per entity and per side for 10 ticks (`Relics.TTL`; client and
server are separate maps) and is built by `Relics#scan`:

| Source (`Relics.Where`) | Read from | Reaches a client? |
|---|---|---|
| HAND | main and off hand | yes (vanilla syncs it) |
| ARMOUR | head, chest and feet slots | yes (vanilla syncs it) |
| CURIOS | `CuriosLink.equipped`: reflection on `CuriosApi.getCuriosInventory(LivingEntity)` (an `Optional`, correct for 1.21.1) then `getEquippedCurios()` | Curios syncs its own inventory; the mod cannot see whether that works, and the read itself is reflection |
| PACK | `MobRelics.pack`: the persistent data of a named mob, an arc character or an Avatar | **never** (F1) |
| SYNCED (new) | the list the server sends (`WornSync`) for what vanilla does not sync: CURIOS and PACK | yes, by `WornSync` |

Changes 1.93..1.98 against this path (`git log` over `client/relics`, `Relics.java`, `CuriosLink.java`, `mixin`, `nusmp.mixins.json`, `data/nusmp/curios`, `data/curios`):

| Release | What touched it | Effect on drawing a worn relic |
|---|---|---|
| 1.94.0 | player-model kit (`client/playermodel/**`) | none for players: it registers renderers for entity types of NPCs and bosses only, never `EntityType.PLAYER` (`PlayerModelRegistry.register`), and the mixin it did not touch |
| 1.95.0 | `KitRelicLayer`, default-look and floating-charm fallbacks, `relic.worn_render` | added; nothing removed. It could not show a pack (F1) |
| 1.95.0 (also) | GUI Size touched `client/relics/djinn/gui/DjinnGui.java` | none: a scale hook of the Djinn screen |
| 1.97.0 | data: new Curios slot `celestial_key` (slot, entity assignment, tag); code: one line in `client/relics/item/ItemStateVariants` (Elsdocia's state) | none; every file under `data/curios` and `data/nusmp/curios` parses as JSON and every required id of a tag is a registered item (checked by name against the sources) |
| 1.93.0 .. 1.98.0 | `RelicWornLayer`, `RelicClient`, `Relics`, `CuriosLink` | **unchanged** (last changes: 1.30.0, 1.48.0, 1.50.0, 1.46.0). The 1.93 to 1.98 releases did not change the detection, the layer or its registration. If the symptom started in that window, the cause is outside these files: another layer, another mod, or the data (S1..S4) |

## 3. Layer registration (point 2)

* `RelicClient.addLayers` visits `event.getSkins()` (both "default" and "slim") and adds the layer to each `PlayerRenderer`. It now logs `added to N player renderers` or the class of a renderer it had to refuse.
* `RelicLayerGuard` (`RenderLivingEvent.Pre`) attaches the layer to a player renderer that never got it, once per renderer.
* The kit's characters (`PlayerModelRenderer.Body`, wide and slim) carry `KitRelicLayer` for every body, including the Avatar's and every conversion the humanoid builder adds later.
* `HumanoidRelicLayer` (new) is added to every other living-entity renderer whose model is a humanoid model (armour stands, zombies, skeletons, piglins, the Mirror Double, humanoid mobs of other
  mods): a head or boots relic has a blank painted armour box (section 5), so on such a wearer it was simply gone. It does nothing unless the entity wears a relic in an armour slot or the server sent a
  list for it. Not given it: villagers and the Hub NPCs (not a humanoid model), the GeoLite creatures, Tensura's own player-like mobs when their model is not a humanoid model.
* Nothing in this mod hides or scales a part of the vanilla `PlayerModel` that the layer reads: `visible = false` appears only in the first-person head hiding of two auras and on the private model of `ArmorGlow`; no `xScale`/`yScale`/`zScale` anywhere.

## 4. Mixins and events (point 3)

`nusmp.mixins.json` has one player mixin, `PlayerModelMixin`, which only appends poses at the tail of `PlayerModel#setupAnim` (`require = 0`). No code in the mod cancels `RenderPlayerEvent` (not used), `RenderLivingEvent`
(`HolderTracker` only records the entity) or a `RenderLayer`. Better Combat / PlayerAnimator change poses, not the layer list; the layer draws through `ModelPart#translateAndRotate`, which they leave alone. The
first-person hand path (`client/relics/hand`) draws relics held in a hand and is a different path from the worn layer; vanilla never draws the own body in first person, so the worn layer does not run there
(by design, `docs/relic_visuals.md`).

## 5. Render types, culling, alpha, assets (point 4)

* Solid pass `entityCutoutNoCull(atlas)`, emissive `eyes(atlas)` (culls: F2), rim `entityTranslucentEmissive(shimmer)` (no cull). The three textures exist in the jar (`WornAssetsTest`); the white pixel of the plain boxes is registered
  by `RelicTextures.registerAll` (reload listener and login), and the debug report prints whether it is.
* All 423 models used by worn relics parse, have quads, an anchor (head, body, arm, leg: the "bone", an enum, so no lookup can return null) and stay inside 40 pixels of their pivot (`WornAssetsTest`). Placement scales are 0.6 to 1.0 (`WornPlacer`).
* No gating by ModList, dimension, game mode or the Rukh purge rule exists in the worn path; the two options are `relicfx.models` (off: plain boxes) and `relicfx.hideArmourRelics` (off by default). The particles are
  untouched by this change (the Rukh purge test still passes).
* A head or boots relic with a model has a deliberately blank painted armour texture (`RelicTextures.registerAll`, `WornLibrary.hidesArmor`): for those the layer is the ONLY visual, so a layer that does not run
  makes a helmet or boots invisible. This is why F3 matters.

## 6. The decision class and the diagnostic command

`client/relics/worn/WornPlan` (pure, no Minecraft type) decides what is drawn for one wearer. Input: one `Entry` per relic (id, slot, look, where it was found, Curios eye state, own renderer?, model?, tier) and a
`Ctx` (slim or wide, view = other / own body / own body with the camera in the head, spectator, invisible, too far, the two options, fallback boxes or charm). Output: one `Item` per relic with a `Kind`
(`LIBRARY_MODEL`, `PLAIN_BOX`, `FLOATING_CHARM`, `CUSTOM`, `SKIP`) and a `Why`. `RelicWornLayer` and `KitRelicLayer` only carry the plan out (`WornExec`), `WornTrace` records per wearer what the layer last did.

| Rule (in this order) | Answer |
|---|---|
| spectator / invisible / beyond 32 blocks | every relic `SKIP` with that reason |
| slot is a hand | `SKIP` held in a hand |
| slot is not armour or Curios (consumable) | `SKIP` not worn |
| Curios relic, found in a Curios slot, eye off | `SKIP` eye off (an unknown eye is on) |
| armour relic and `hideArmourRelics` | `SKIP` option |
| own renderer exists | `CUSTOM` |
| camera in the head and a head relic | `SKIP` first person |
| (models on or armour slot) and a model loads | `LIBRARY_MODEL`, best grade first, ties by id |
| armour relic worn in the armour slots, no model | `SKIP` the armour layer draws its painted armour |
| Curios relic, player, no model (or models off) | `PLAIN_BOX` |
| anything else (a pack relic, a kit character) | `FLOATING_CHARM` |

Boxes and charms stand after the library models of their slot. Slim arms add `SLIM_INSET` for arm pieces.

`/multiverse relic debug [player]` (operators) prints, in the operator's chat and the server log:

```
Worn relics of Steve (server view)
Curios read: every step passed        (or: A STEP FAILED, see below)
  Curios mod: loaded, version 9.x.x
  step 1 class CuriosApi: found
  step 2 method getCuriosInventory(LivingEntity): found, returns Optional
  step 3 invoke: an inventory (...)
  step 4 getEquippedCurios: ok, 20 slots, 2 worn [nusmp:amulet_of_the_coven, ...]
  step 5 per-slot road: 7 slot types: necklace[1/12 eye=[true, ...]] ...
Worn texture atlas in the jar: yes, shimmer: yes; catalog: every step passed
1. amulet_of_the_coven (Legend): slot NECK, look AMULET, found in: curios, Curios slot "necklace"
     catalog route: SIGNATURE
     model amulet_coven: 312 quads, bone BODY, 2 animation groups, 14 glowing quads
     decision: amulet_of_the_coven: LIBRARY_MODEL (drawn)
Worn list last sent to clients: nusmp:amulet_of_the_coven
Worn relics of Steve (client view, this machine)
Worn layer added to 2 player renderers; refused: -
Renderers that got the layer from the first-frame guard instead of the event: 0 (-)
Atlas texture resolved: yes, shimmer: yes, white pixel registered: yes
Server lists received: 3 (last 4100 ms ago)
Layer reached 1 ticks ago (5120 frames, layer player, view SELF_THIRD, slim arms yes)
This client's own read of the Curios inventory: every step passed
1. amulet_of_the_coven: slot NECK, found by this client in: curios
     model library: 4 pieces, 1312 vertices, tier 5
     last decision: amulet_of_the_coven: LIBRARY_MODEL (drawn)
```

How to read it: a step that says FAILED names the call to fix; `EMPTY` at step 3 means no Curios slot is assigned to the entity; "LAYER NEVER REACHED" means the wearer is not drawn by the worn layer (first
person, out of view, another renderer; see the guard line); "The server sees X but this client did not find it" means the client's own Curios read failed (it is then drawn from the server's list, and the line says
when it is not); "model library: NO MODEL LOADED" means the client's resources do not hold the catalog entry.

The report texts are `message.nusmp.relic_debug.*` (`tools/lang/relic_render2.json`, read with `translatableWithFallback`; a test pins every text in the code to the fragment).

## 7. What was built

| | NEW | REPLACED | KEPT |
|---|---|---|---|
| Decision | `WornPlan`, `WornExec`, `WornTrace`, `WornAssets` (client/relics/worn) | the decision loops inside `RelicWornLayer.render` and `KitRelicLayer.render` | every draw routine (boxes, library, custom renderers, armour glow), every model, texture, id, catalog line |
| Data path | `WornSync` (packet `nusmp:relic_worn_sync`, server events, client store), `WornMerge`, `Relics.Located/Where/locatedList/scanExtra`, `MobRelics.hasPack` | `Relics#scan` is built from `scanExtra` plus hands, armour and the client merge | `Relics.wornList`, `Worn`, `Loadout`, the 10-tick cache, attunement rules |
| Robustness | `RelicLayerGuard`, `CuriosProbe`, second Curios road, failure notes, the unreadable-catalog rule | the all-or-nothing Curios read, the permanent empty catalog | `CuriosLink` API (`equipped`, `flags`, `isRendered`, `carries`, `available`) |
| Diagnostics | `/multiverse relic debug [player]`, `WornDebug`, `WornDebugPayload`, `WornDebugClient` | - | the other `relic` subcommands |
| Drawing | `WornMesh.corner`, `WornLibDraw.draw(..., armInset)`, `HumanoidRelicLayer` | the emissive pass of mirrored pieces | the old `draw` signature (delegates) |
| Tests | `WornPlanTest`, `WornPlanCoverageTest`, `WornMergeTest`, `WornAssetsTest`, `WornWiringTest`, self-test `relic.worn_render2` | - | `relic.worn_render`, `WornCoverageTest`, `RelicRenderCoverageTest` |

Edits to existing files (additions or one-line changes): `RelicPayloads` (two `playToClient` lines), `RelicSystem` (two lines in `registerGame`), `RelicCommands` (the `debug` node and its method), `RelicClient`
(guard listener, `known(r)`, logout clears, layer log), `RelicTextures` (`whiteRegistered`), `Relics` (see above), `MobRelics` (`hasPack`), `CuriosLink` (second road, notes), `WornModels` and `WornLibrary`
(no negative caching of an unreadable read), `WornLibDraw` (F2, F7, takes a `HumanoidModel`; every caller passes a `PlayerModel`, which is one) and `WornMesh`, `KitRelicLayer` and `RelicWornLayer` (render body now plan-driven; `KitRelicLayer`'s class, constructor and package are unchanged).

Numbers: 683 relic defs in the wave sources, 512 worn on the body (BACK 52, BELT 29, CHARM 165, CHEST 39, FEET 17, HANDS 70, HEAD 76, NECK 64), 423 distinct worn models, 31 looks, 401 signature entries.
`WornPlanCoverageTest` runs all of them through the plan in 16 contexts (wide/slim x own/other body x models on/off x boxes/charm): none is skipped for a reason a wearer cannot see; the only `SKIP` left is
`VANILLA_ARMOUR` (an armour relic with no model: none exists, every armour relic has one).

## 8. What could NOT be verified

* Nothing was run in a game, so no frame of this was seen. The Curios reflection has still not met the real mod in any session; `/multiverse relic debug` is how it will.
* `WornSync` is written against the NeoForge 1.21.1 API from the type check (stubs), its codecs by a self-test on a real server buffer, the rest by reading. A packet is added to the mod's network: both sides of a
  connection must have this version (the registrar is optional, a client without it simply never receives the list).
* A Curios relic that is removed is still drawn from the server's list for up to about 10 ticks plus latency (the list is checked every 10 ticks).
* The slim-arm inset and the mirrored glow are geometry arguments, not seen. The F2 fix is a corner order; if a mirrored piece looked right before, it is a case the winding argument did not cover.
* Not changed on purpose: weapons worn in a Curios belt/back/curio slot (the sheathed weapons of 1.15.0, hand relics) are still not drawn on the body ("Curios draws nothing on the body for them", CHANGELOG
  1.15.0); that is a feature that never existed, not a regression, and is left for its own request.
* The CI smoke test must list `relic.worn_render2` (`.github/workflows/smoke.yml`, not edited in this round).
