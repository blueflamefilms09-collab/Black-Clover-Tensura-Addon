# Worn armour relics are never invisible (armor_visible)

Owner report (third time, still open in 1.110.0): a crown relic sits in the head armour slot and the player's head shows nothing, in the inventory preview and in third person; chest and legs are empty; several chest relics lie unworn in the inventory.

## Honest status

The game cannot be run in the build session, so the single frame on the owner's screen could not be reproduced. The whole path was read (item, armour material, texture painter, reload and join hooks, the item's armour-model hook, the plan, the worn layer, the model library draw, the layer registration and its safety net). No line was found that, on its own, explains "everything is invisible"; instead the code had **several places where the mod believed something was drawn without ever checking**, and any one of them leaves a worn relic bare with no log line. So the fix is a structural guarantee that does not need the cause to be known: after every frame, what was *really* drawn is compared with what was *planned*, and if nothing of a worn armour relic reached the screen, plain painted boxes of the relic are drawn and one warning per relic is logged with the reason.

## Findings (file:line are the 1.110.0 sources)

| # | Finding | Evidence | Effect |
|---|---|---|---|
| 1 | The painted layers were registered in one loop with a single `try` around all of it. The first relic whose painter threw ended the loop: every relic after it in `Relics.all()` had no texture at all, `painted` stayed false, and the only trace was one error line. | `RelicTextures.java:73-92` (loop at 73, `painted = true` at 89, the single `catch` at 90) | Vanilla draws the missing-texture or nothing for every relic after the bad one. Fixed: one `try` per relic, a plain layer in the relic's colours as the fallback. |
| 2 | Nothing ever checked that the registered textures are still there. They are registered in a reload listener and on `LoggingIn` only. A reload that runs another mod's listener later, a pack toggle, or a texture manager reset drops them silently. | `RelicClient.java` (reload listener, `LoggingIn`); no later check existed | Added `RelicTextures.verify()` (first world tick after each registration, then every 10 s): presence, then non-empty pixels, then re-register (max 6 repairs per session). |
| 3 | The item hides the vanilla armour layer (returns the empty model) for head and feet relics whenever the library *has* a model, but "has a model" is not "drew a model". If the layer is not reached for that renderer, throws, draws zero pieces, or is skipped by the budget (`MAX_SETS`, `POOL`), the head is bare. | `ArmorItemRenderer.java:62` (hook) -> `ArmorSlotClient.hidesPainted` -> `WornPlan.java` (LIBRARY_MODEL is chosen from `hasModel` alone) | This is the exact symptom (crown in the slot, nothing on the head). Fixed by the failsafe below. |
| 4 | `WornTrace.drawn` was `plan.drawnCount()`, the number the plan *meant* to draw. `/multiverse relic debug` therefore showed "drawn 1" for a crown that reached the screen as nothing. | `WornTrace.java:49` | Made truthful: `drawn` is the real count (models with at least one piece, plain boxes, own renderers that ran, failsafe boxes); `planned` keeps the old number. |
| 5 | The model library draw had no guard: an exception there propagated out of the layer (a crash or a lost frame), and the library recorded nothing about what it drew. | `RelicWornLayer.java:118,128` (old); `WornLibDraw.draw` | Wrapped (pose stack restored, one error log); `WornLibDraw` now records pieces drawn per ensemble id. |
| 6 | An own renderer that failed was replaced by a no-op that "ran fine" from then on. | `RelicWornLayer.drawCustom` | Failure is remembered; a failed renderer never counts as drawn. |
| 7 | "The player's switch is obeyed" was false for a far wearer: `TOO_FAR` outranks `ARMOUR_OPTION` in the plan, so with `relicfx.hideArmourRelics` on, a wearer beyond 32 blocks got its painted armour back. | `WornPlan.java:142,154`; `ArmorSlotPlan.decide` | Fixed in `ArmorSlotPlan.decide` (the switch wins at any distance). |
| 8 | Hypothesis checks that came out negative: the layer is added to both skins in `RelicClient.addLayers` and to any renderer the event missed by `RelicLayerGuard`; the layer index is right (`layer_1` for head, chest and boots, `layer_2` only for leggings, which relics never use); the painted image is 64x32 and both painters return non-empty images for every look (`ArmorPainterV2Test`); the material layer points at `nusmp:relic_<id>` (`RelicArmorMaterials`, checked by `visual.armor_slot_relics`); no mixin touches armour slots (`nusmp.mixins.json`); `getHumanoidArmorModel` has the 1.21.1 NeoForge signature; `HandClient` skips items already registered. |  |  |
| 9 | Equipping: `RelicArmorItem.use` equips on a plain right-click (`super.use`). With **sneak** held and an unattuned, attunement-needing relic it starts the five-second attunement ritual instead, so a sneaking player cannot equip such a piece by right-click. Vanilla slot clicks and shift-click do not go through `use`; `Slot.mayPlace` accepts the piece because it is an `ArmorItem` of the right type. No event cancels equipping. | `RelicArmorItem.java:51-57`, `RelicEngine.java:319-330` | Not changed (the ritual is a designed feature); named here because it can explain unworn chest relics. Equip with a plain right-click or by clicking into the slot. |

Not verifiable here: whether another mod on the owner's setup (Tensura race forms, a player HUD or minimap renderer, Curios' own layers) replaces or hides the player's armour layers. The failsafe covers that case too, because it does not depend on the vanilla layer.

## What changed

| Part | Change |
|---|---|
| (a) failsafe | `RelicWornLayer.armourFailsafe`: after the plan is executed, each worn armour relic (HEAD, CHEST, FEET, found in the armour slots) is resolved from real facts: pieces the library drew (`WornLibDraw.piecesDrawn`), own renderer ran, the vanilla layer was asked for the painted layer (`ArmorTrace.markPainted`, set by `ArmorSlotClient`) with a registered, non-empty texture (`RelicTextures.paintedOk`). If none holds and the player did not switch armour visuals off, `failsafeBoxes` draws the plain painted boxes (helm + band + stone / chest + hem + sleeves / boots + cuffs) in the relic palette, slightly inside the vanilla armour layer so they never fight a layer that is there. One warning per relic per session. A chest piece needs the painted body (or an own renderer); ornaments alone do not count. |
| (b) decision table | `ArmorVisible` (pure): `resolve(plan, facts)` and `checkTable()`: every slot x hasModel x custom x models option x hide option x far x view gives a visible plan unless the player switched it off or the camera is in the head; every combination of real facts gives MODEL / CUSTOM / PAINTED, the player's choice, out of range, or FAILSAFE, never nothing. |
| (c) texture check | `RelicTextures`: per-relic isolation with a plain fallback layer and a non-empty check at registration; `verify()` / `onClientTick()` (hooked in `RelicClient`) check presence and pixels at the first world tick after every (re)registration and every 200 ticks, and re-register. |
| (d) debug | `/multiverse relic debug` (`WornDebugClient`) prints per worn armour relic: slot, painted texture present, count and last check result, vanilla layer chosen (plan kind and reason), whether the vanilla layer asked for the painted layer, model pieces drawn, own renderer ran, final result, whether the failsafe boxes were drawn. Lang keys `message.nusmp.relic_debug.armour_*` in `tools/lang/armor_visible.json` (mirrored in `relic_render2.json`, which `WornWiringTest` reads). |
| (e) self-test | `relic.armor_visible` (`relics/ArmorVisibleSelfTests`, registered in `RelicSystem.registerGame`): the decision table, non-empty painted layers (both painters) for every armour relic, a covering fallback layer, a visible verdict in every circumstance. Pure; server safe; the file is in `docs/server_safe_allow.txt` because it imports the pure client-package classes (same as `ArmorSlotSelfTests`). |
| trace | `WornTrace.drawn` is the real count (`planned` is the plan's). |

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `client/relics/worn/ArmorVisible`, `ArmorTrace`; `RelicWornLayer.armourFailsafe/failsafeBoxes/drawEnsembles`; `RelicTextures.verify/onClientTick/paintedOk/paintedCount/healCount/lastVerify`; `WornLibDraw.resetDrawRecord/piecesDrawn/setsDrawn`; `WornTrace.reached(..., drawnReal)`, `Seen.planned`; `relics/ArmorVisibleSelfTests`; `ArmorVisibleTest`; `tools/lang/armor_visible.json`; this document. |
| REPLACED | `RelicTextures.registerAll` loop (one `try` for all relics -> one per relic with a plain fallback); `WornTrace.Seen.drawn` meaning (planned -> real; the planned count moved to `planned`); `ArmorSlotPlan.decide` far + hide (the switch now wins at any distance). |
| KEPT | All painters, models, the plan, the empty-model hook, the vanilla layer path, the emissive pass, every other fallback, the attunement ritual on sneak-use, all ids and save data. Time Magic VFX untouched. |

## Tests

`ArmorVisibleTest` (table, failsafe facts, missing texture, model that drew nothing, chest ornaments, the player's two choices, fallback layer, trace, wiring scan), with `ArmorSlotPlanTest`, `WornPlanTest`, `WornCoverageTest`, `WornWiringTest`, `PerfSourceScanTest`, `ArmorPainterV2Test`, `ServerSafetyTest` all passing in the shim.

## Unverified (nothing visual could be run)

* That the failsafe boxes look right on a real head, chest and boots (sizes were set from the vanilla armour model; they sit about 0.6 px outside the body and inside the vanilla armour inflation).
* That `getTexture(loc, null)` and `DynamicTexture.getPixels()` behave in the game as the stubs say (they type-check against the API dump).
* That the vanilla armour layer runs before `RelicWornLayer` in every renderer; if a renderer runs it after, the first frame after equipping may draw the failsafe boxes for one frame and log the warning.
* That no other mod on the owner's setup draws the player through a renderer that is neither `PlayerRenderer` nor accepts a layer (`RelicLayerGuard` logs that case).
* The real root cause on the owner's machine: after this build, `latest.log` lines `armour relic <id> (<slot>) was worn but nothing of it was drawn (...)`, `armour layer(s) were missing or empty`, `the worn model library threw`, or `the armour layer of relic <id> could not be painted` name it, and `/multiverse relic debug` shows the per-slot facts.
