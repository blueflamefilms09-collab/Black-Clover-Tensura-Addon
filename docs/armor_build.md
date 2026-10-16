# Armour build: worn 3D models for every armour item

Owner request (1.34.0): every armour item, old and new, gets the same build as the relics and weapons. Code: `client/relics/worn/lib/*` (`ArmorItems`, `ArmorLooks`, `ArmorFx`, `ArmorItemRenderer`, `WornLibrary.ofItem/ofKey/ofCurio`), `client/relics/RelicWornLayer`, `client/mode/ModeArmorLayer`, `item/ArmorEngravings`. Art: `tools/relics/gen_worn_armor.py` (robe sets), `gen_worn_armor_relics.py` (armour relics), `gen_worn_modes.py` (transformation overlays); catalog and models are written by `python3 tools/relics/gen_worn_all.py`; item models by `python3 tools/relics/gen_armor_item_models.py`; sheets by `python3 tools/relics/armor_contact_sheet.py` (`docs/armor_build/*.png`).

## What each thing is
- **Robe sets (11 sets x 4 pieces)**: catalog `item <id>` lines (models, `ipal` palette, `itier` grade tier, `fx` particle theme). Worn as lofted 3D garments (hood/hat/crest, robe/coat/mantle with sleeves and a set badge, hip band and legs, boots) in place of the GeckoLib model for players.
- **Generic fallback** (`ArmorLooks`, pure, tested): any future `ArmorItem` of the `nusmp` namespace with no catalog entry gets a model from its slot and name words (robe/plate/cloth) and a stable palette from its path.
- **Armour relics**: the 16 chest/boot relics that only had the model of their look now have signature models (13 chests, 3 boots).
- **Modes** (`mode_<name>` entries): 3D wings, crowns, horns and a skirt replace the flat cards of the overlays; the painted plates stay.
- **Curios items that are not relics** (6 dungeon relics, 7 charter grimoires): `item` + `islot` entries reuse library models.
- **Object form**: armour in hand/ground/frame is its 3D model, the inventory keeps the 16x16 sprite (`<id>_icon` model; `builtin/entity` item json).
- **Engravings**: `ArmorEngravings` (robe sets and the 42 armour relics without engravings), same once-only flag as weapons.

- **Anime look (1.48.0)**: the robe sets are also catalogued as `anime:<id>` (cloak only, `docs/robe_anime.md`); `relicfx.robeAnime` (default true) picks it, false = the lofted garments above.

## Why
- **Catalog by item id**: new armour needs one data line, no Java.
- **Library replaces GeckoLib only for players with the option on**: mobs, armour stands and `relicfx.models=false` keep the old renderer, so nothing is ever invisible (rule 2: old kept).
- **Empty humanoid model for the replaced renderer**: the vanilla layer then draws nothing, with no texture lookups for missing `layer_2` files.
- **Exact size for armour, relic fit for Curios items**: a garment must cover the body; accessories are shrunk and spaced like relics.
- **Motes/ring only on the chest piece**: four pieces of one set must not each spawn the extras.
- **Particles share the relic per-tick budget** (`ArmorFx` is called inside `RelicFxClient`).
- **Icon model + builtin/entity**: same method as the weapons; sprite kept for the inventory.
- **Budget**: every ensemble <= 2600 vertices (tested), models parsed once and cached, 32-block cull inherited from the layer.

## Not done
Tier/socket components on non-relic armour (defined on `RelicDef`), set bonuses, Order/UniqueLedger. Nothing was run in the game.
