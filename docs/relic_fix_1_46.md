# Relic fix 1.46.0: part 4 relics do not work / are not drawn

Owner report: "the new relics are not the same as the old ones: you cannot use the abilities in or out of slot, and they do not show on the player."

## Root causes
1. **Worn relics were invisible to the whole relic system (the main cause).** `item/CuriosLink.equipped()` and `flags()` shared one `if (getInventory == null)` lazy init. `flags()` runs on the client render thread and set `getInventory` first, so `equipped()` never set `getEquipped`, threw a NullPointerException, and latched `broken = true` for the session. Every reader of worn relics (`Relics.scan`, `RelicEngine.carriedStacks`, `Attune`, the act key ring, the worn layer) then saw nothing worn: no abilities from a slot, no passives, no draw. The lead fixed this in `CuriosLink` itself (each method initialised on its own). It is not part of this change. Hardening still advised there: do not latch `broken` for good on one exception, retry after about 200 ticks.
2. **Ring of the Mimic (wave 25) could not be put in a charm slot.** `tools/relics/gen_curios_tags.py` resolved a bare constant name (`ID_MIMIC`) globally, so the Wave Boss belt def and the wave 25 charm def both became `ring_of_the_mimic` and the later (belt) slot won; the item sat only in `belt.json`. Fixed: a bare name is looked up in its own class first. `charm.json` gains the entry (additive; the belt entry stays).
3. **Eleven part 4 charms and three older charms had no worn-model look.** CHARM + ORB (orb_of_plagues, orb_of_the_dryad, the six Infinity Stones) and CHARM + PENDANT (lucky_egg, official_looking_badge, pocket_time), plus holy_handgrenade (ORB), tiny_orbit (CIRCLET) and defenders_dial (BELT) had no `look` line in `relic_worn/catalog.txt`, so they only got the old plain boxes instead of a 3D model. `tools/relics/gen_worn_all.py` now has `look CHARM ORB charm_orbit`, `CHARM PENDANT brooch_badge`, `CHARM CIRCLET charm_orbit`, `CHARM BELT charm_pouch`; `catalog.txt` regenerated (only those four lines differ).

## Checked and found fine (same path as the old waves)
- Item creation: `WaveRegistry` scans `Wave8..Wave40` plus `WaveBoss`; `RelicCatalog.make` picks weapon / armour / consumable / gear item by slot; all 124 relics of waves 20 to 30 are parsed by the new test.
- Act dispatch: `RelicEngine.activate`, `useHeld`, `onActPacket` and the key ring `RelicActKeys.entries` look relics up through `Relics.loadout` (items implement `RelicBearer`), with no id tables. Attunement is the same ritual as in older waves (`attune()` relics need the 5 second sneak-and-hold first).
- `RelicHolderSync` is not a worn-relic list (it only syncs the Genesis flag and grimoire leaves), so there was nothing to add there.
- Held weapons: every non-adopted MAIN_HAND relic has `weapon_meshes/<id>.txt` and `models/item/<id>.json`; `WeaponRenderer.WEAPONS` includes `RelicCatalog.model3dItems()`.
- Curios tags: every worn part 4 relic is in its slot tag (apart from item 2).

## Guard
`src/test/java/com/newuniverse/nusmp/relics/RelicRenderCoverageTest.java` reads the wave sources and asset files (no game) and fails if a worn relic has no catalog model, a Curios relic is missing from its slot tag, or a held weapon lacks its mesh or item model.

## Not verified
Nothing was run in game. The new look entries reuse existing models (`charm_orbit`, `brooch_badge`, `charm_pouch`); their placement on the body is by the existing `WornPlacer` and was not seen. `gen_worn_all.py` still exits 1 because of older ensembles over the vertex cap (elemental_hand_wraps, bracers_of_phasing, shadow_robes ...); that predates this change.
The test was compiled and run with a plain javac and the JUnit shim, not Gradle.
