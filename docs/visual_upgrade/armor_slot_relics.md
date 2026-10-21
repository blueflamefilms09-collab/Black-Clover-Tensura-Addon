# Armour-slot relics (1.110.0)

Owner report: a crown relic sits in the helmet slot of the inventory, the player in third person shows no crown; a relic chestplate shows only a flat, murky, blocky painted layer.

## 1. Everything that is worn in a vanilla armour slot, and how it is drawn

| Item | Slots | Worn through | Drawn by |
|---|---|---|---|
| 146 relic defs with slot HEAD (83), CHEST (40), FEET (23) (`relics/wave/Wave*`, `WaveBoss`), registered as `RelicArmorItem` | helmet, chestplate, boots (there is no LEGS relic slot) | `RelicArmorItem`, material `nusmp:relic_<id>`, layer `textures/models/armor/relic_<id>_layer_1/2.png` painted at run time by `client/relics/ArmorPainter` (older) or `ArmorPainterV2` (1.110.0) | the vanilla `HumanoidArmorLayer` (painted layer), plus the worn model library through `RelicWornLayer` (players), `KitRelicLayer` (player-model characters), `HumanoidRelicLayer` (armour stands, mobs): HEAD and FEET relics with a model are drawn as that model, CHEST relics keep the painted body and get the library ornaments on top; the glow channel by `ArmorGlow` |
| Adopted relics (`RelicDef.adopt`, an existing item of the mod given a relic def) | whatever the item is | their own item class and layer | their own renderer; the relic layer reads them through `Relics.defOf` |
| Magic Knight robe sets (`NUItems.robe`, `RobeItem`, 11 sets x hood/chest/legs/boots) | all four | `RobeItem` (GeckoLib) | the library through `ArmorItems.collect` (`catalog.txt` `item` lines, else `ArmorLooks`), the GeckoLib renderer is replaced by an empty model (`RobeItem`) |
| Dungeon armour (`LootArmorItem`, Drake-Scale and Shadowsteel) | all four | materials `loot_<set>`, layer_1 and layer_2 png on disk | the vanilla layer; `client/loot/LootClient` swaps in an empty model when the library draws it |
| Boss relic crowns / rings with own renderers (`WornRenderers`: `dante_gravity_crown`, `zenon_spatial_ring`, `artifact_mimic`) | head / Curios | own `WornRenderer` | the renderer, drawn after the library |
| Erza requip pieces and the mode armour (`ModeArmor`, `client/mode`) | not equipment items | a mode overlay | `ModeArmorClient` (other builders own the Erza pieces) |

## 2. Root cause

The data is complete: a JUnit test over the real wave sources, `catalog.txt`, the visual upgrade `catalog.txt` and every model file finds a worn model on the right body part for every one of the 146 armour-slot relics (`ArmorSlotRenderCoverageTest`). The Curios slots, the anchor table and the 1.106.0 merge of the God and Charms worn libraries did not drop the armour path.

The failure is in how the helmet and boots are hidden. `RelicTextures.registerAll` decided ONCE, when the textures were painted (resource reload or joining a world), that a helmet or boots relic "has a worn model" and registered its armour layer as a fully transparent texture ("the model stands where the box would"). Whether the model is really drawn is decided on EVERY frame by `WornPlan` through a different chain of conditions (the worn layer reached for the wearer, the plan not skipping the relic, the wearer within range, the library able to dress it from the resources loaded at that moment, `relicfx.hideArmourRelics`, the first-person rule, an entity renderer that has the layer). Whenever the two decisions disagreed, the relic was drawn by neither: the vanilla layer drew a blank texture and the worn layer drew nothing. A crown in the helmet slot with no crown on the head is exactly that state, and nothing in the code could notice, because the blank texture was the only trace left of the decision.

What could not be checked here: which of the frame-time conditions disagreed in the owner's world (nothing can be run). The fix removes the whole class of failure instead of one branch.

## 3. The fix

* The painted layer is now ALWAYS registered (`RelicTextures`, with its glow channel). Nothing is blank any more.
* `ArmorSlotPlan` (pure) answers, from `WornPlan` itself, what the vanilla layer draws: its painted layer, or an empty model. It is empty only when `WornPlan` really draws a model or an own renderer for the relic, or the player chose it (`relicfx.hideArmourRelics`, the camera in the head). In every other case (no model, wearer out of range, plan skipped, entity without the worn layer) the painted armour shows.
* `ArmorSlotClient` evaluates it per frame and the relic items hand `ArmorItems.emptyModel()` to the armour layer through `IClientItemExtensions.getHumanoidArmorModel` (`ArmorItemRenderer.registerExtensions`), the same mechanism the robes and the dungeon armour already use.
* The glow pass of the worn layer (`RelicWornLayer`) uses the same verdict: only over a painted layer.

## 4. Regression guards

* JUnit `ArmorSlotRenderCoverageTest`: every armour-slot relic (derived from the wave sources) has a model that loads and hangs on its slot's body part (head / body and arms / legs), through the upgrade catalog first, as the library does; every robe piece and dungeon set has library models and layer files.
* JUnit `ArmorSlotPlanTest`: over every combination of slot, model, own renderer, option, view and distance an armour relic keeps a model or a painted layer (except the two cases the player chose); the old failure (model not drawn, layer blank) is impossible.
* Self-test `visual.armor_slot_relics` (`relics/ArmorSlotSelfTests`, runs on the dedicated server): the same over the loaded registries: each armour-slot relic is a registered `RelicArmorItem` with the layer `nusmp:relic_<id>`, resolves to a model on the right body part, has a painted layer that is not transparent, covers the same pixels in both painters, and is never invisible in any frame circumstance; every other `nusmp` armour item resolves to existing library models. It is a server-run test of the registries; it cannot see the client textures, so the pixel checks are done on the pure painters.
* Debug command `/nusmp debug worn` (client): lists the four armour slots and the Curios relics of the local player with the way each is drawn right now and the reason when nothing of it reaches the body.

## 5. Better relic armour (owner addition, same release)

`ArmorPainterV2` (used while `visual.upgrade` is on; the older painter answers when it is off; the layers repaint once when the switch is flipped) keeps the vanilla `layer_1` layout and the exact covered pixels (so nothing clips with the body or the sleeves; a test compares them pixel for pixel) and repaints: one five-step palette ramp per relic instead of grain, bevel on every face, one aligned trim line across torso and sleeves, a sigil emblem (4 shapes) in the glow colour on the chest (on the back for cloaks, capes and mantles), a gem on a helmet's brow, a cuff line on boots, a lift for very dark palettes. The glow channel (`paintGlow`) is the emblem and the gems, drawn by `ArmorGlow` (the existing additive pass of the worn layer) at full brightness, pixel-aligned with the layer. Previews: `docs/previews/relic_armour/` (`before_after_sheet.png`, left old, right new; regenerate with `NUSMP_PREVIEW=1` and the JUnit test `ArmorPainterV2Test.writesThePreviewsWhenAsked`).

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `ArmorSlotPlan`, `ArmorSlotClient`, `ArmorSlotCoverage`, `ArmorPainterV2`, `WornDebugCommand` (`/nusmp debug worn`), self-test `visual.armor_slot_relics`, the tests above, the previews, lang fragment `tools/lang/armor_slot_relics.json` |
| REPLACED | the blank armour texture of a helmet or boots relic with a worn model (a per-frame empty-model decision replaces it); the glow condition of the worn layer (now the `ArmorSlotPlan` verdict) |
| KEPT | `ArmorPainter` (answers with `visual.upgrade` off, and is the base of the new painter), `ArmorPainter.blank()`, `WornLibrary.hidesArmor` (no longer used by the textures), every catalog, model and layer id, `WornPlan` and its reasons, the robes' and dungeon armour's paths |

## Unverified (nothing visual could be run)

* In-game: that the crown now shows; which frame-time condition failed in the owner's world is unknown. After the fix the painted crown shows whenever the 3D model is not drawn, and `/nusmp debug worn` names the reason.
* That `getHumanoidArmorModel` on `RelicArmorItem` is honoured by every renderer that draws armour (it is for the vanilla `HumanoidArmorLayer`, the player-model kit uses the same layer class); an armour layer of another mod that ignores it would draw the painted layer under a model.
* A wearer farther than 32 blocks now shows the painted helmet and boots instead of nothing; the model replaces it when the wearer comes within range.
* The quality of the repainted layers on a real body, in the game's lighting: only the unwrapped previews were seen.
* The 3D add-on parts for armour (shoulder pieces, crests, cloak): not added; the worn library already hangs pauldrons and sleeves on CHEST looks and crowns and helmets on HEAD, and a new model pipeline for armour was out of reach in this pass.
* Non-relic `nusmp` armour items that have both a library model and a real layer texture could draw twice for a player; not changed here (the robes and the dungeon armour already hand back an empty model).
* The existing `ArmorPainterTest` and `ArmorLooksTest` use `assertArrayEquals(int[])`, which the offline JUnit shim lacks; they were not run here, the painter's only change to them is one visibility modifier.
