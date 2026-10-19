# Worn relics on the player model: what was checked and fixed

Request: relics do not show on the player model. Read-through of the whole path (the mod draws worn relics with its own `RelicWornLayer`, not with Curios renderers; Curios is only read by reflection).

## What was found
| Area | Result |
|---|---|
| Layer registration | `RelicClient.addLayers` adds `RelicWornLayer` to both vanilla player renderers (wide and slim) on `EntityRenderersEvent.AddLayers`; it runs for the local player in third person and for every remote player. First person draws no worn relic (by design). |
| Model coverage | Every `RelicDef.of(...)` in the wave sources with a worn slot (HEAD, CHEST, NECK, BACK, BELT, HANDS, CHARM, FEET) resolves to a catalog entry and the model files exist (checked by `WornCoverageTest` over all wave sources). The 24 Djinn seals and tier/forge items are not worn relics (plain items). The six dungeon relics and the seven charter grimoires have `item` + `islot` entries. |
| Hidden by rules | Only the Curios eye icon (`CuriosLink.flags`) and `relicfx.hideArmourRelics`; no other hiding. |
| Player-model kit | `PlayerModelRenderer.Body` had no relic layer, so humanoids drawn by the kit (arc NPCs, named mobs with a relic pack) never showed relics. Fixed. |
| Slot/look not in the catalog | A relic whose look has no `look <SLOT> <LOOK>` entry fell back to crude boxes (Curios slots) or to nothing (armour slots). Now it wears the default look of its slot. |
| Curios items of the mod without `islot` | Drawn as nothing. Now a small floating charm. |
| Curios slot types | `necklace`, `back`, `belt`, `hands`, `charm` are assigned to the player by `data/nusmp/curios/entities/relics.json` but defined nowhere in this mod (only `dungeon_relic` is). They must come from Curios itself; not verifiable here. If a build of Curios does not ship them, relics cannot be equipped in those slots at all. |

## Changes
* `WornCatalog.piecesFor` falls back to the default look of the slot (`defaultLook`, same table as `Look.defaultFor`); `WornCatalog.sourceOf` says which route a relic takes.
* `WornLibrary.fallbackCharm(RelicDef)` / `fallbackCharm(String path)`: the generic floating charm (`charm_orbit`) tinted with the palette.
* `ArmorItems.collectCurios`: a non-weapon `nusmp` Curios item with no model gets the floating charm.
* `KitRelicLayer` (new) is added to `PlayerModelRenderer.Body`: draws the relics of the entity (Curios, armour relics, a mob's pack) through `WornLibDraw`; a relic the library cannot dress gets the floating charm.
* `WornLibDraw.draw` takes `PlayerModel<?>`.
* `WornCoverage` (pure check) used by `WornCoverageTest` (data-driven over the wave sources and the catalog) and by the server self-test `relic.worn_render` (`RelicWornSelfTests`, registered from `RelicSystem.registerGame`).

## Not verified
Nothing visual could be run. The Curios slot definitions, the real draw on a kit NPC and the floating charm placement need an in-game look.
