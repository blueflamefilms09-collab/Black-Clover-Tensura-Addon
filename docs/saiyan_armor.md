# Saiyan alloy armour (craftable set)

You no longer get a Saiyan armour set from a form. You craft one. The look is the same battle armour the forms used to draw by themselves (white plate with the shoulder pauldrons, navy lame, gold trim, waist skirt guards, greaves, scouter), now as four real armour items made from a new material.

## What is in it

| Item | Slot | Recipe | Worn model (Blender) |
|---|---|---|---|
| `saiyan_alloy_ingot` | material | see below | none (flat item) |
| `saiyan_alloy_scouter` | helmet | 5 ingots + 1 glass pane | `saiyan_alloy_scouter` (54 quads, lit lens) |
| `saiyan_alloy_battle_armor` | chestplate | 8 ingots | `saiyan_alloy_chest` + pauldron x2 + vambrace x2 (650 quads) |
| `saiyan_alloy_waist_guard` | leggings | 7 ingots | `saiyan_alloy_skirt` (188 quads) |
| `saiyan_alloy_greaves` | boots | 4 ingots | `saiyan_alloy_greave` x2 (210 quads) |

Ingot recipes (shapeless, both give 2): 3 iron ingots + 1 gold ingot + 1 copper ingot, or 1 netherite scrap + 1 iron ingot + 1 gold ingot. Pieces use the vanilla shapes. There is **no smithing path** on purpose: netherite armour is better than this set, so upgrading from it would be a downgrade. Mend a piece with an alloy ingot in an anvil.

Material `saiyan_alloy` (between iron and netherite on every figure, pinned by `SaiyanArmorFilesTest`):

| | helmet | chest | legs | boots | total | toughness | knockback res. | durability factor | enchantability |
|---|---|---|---|---|---|---|---|---|---|
| iron | 2 | 6 | 5 | 2 | 15 | 0 | 0 | 15 | 9 |
| **saiyan alloy** | 2 | 7 | 5 | 3 | 17 | 1.5 | 0.05 | 27 | 17 |
| netherite | 3 | 8 | 6 | 3 | 20 | 3 | 0.1 | 37 | 15 |

Creative menu: the four pieces are in the Armor tab, the ingot in the Misc tab, and all five also in the Anime Worlds franchise tab. Engravings do not apply (armour).

## Look

- Worn: the worn-model library (`relic_worn/`, catalog `item saiyan_alloy_*` lines) draws real shaped 3D pieces with baked skins and a glow map (scouter lens, belt jewel, gold seams). The vanilla armour box is hidden while the library draws the piece (`client/saiyan/SaiyanArmorClient`), so nothing is drawn twice; a mob or armour stand wearing the item falls back to the plain 64x32 layers `textures/models/armor/saiyan_alloy_layer_1/2.png`.
- Hand / ground / frame: the same 3D model (`builtin/entity` item model, `ArmorItemRenderer`). Inventory: a 16x16 sprite (`<id>_icon.json`).
- Previews (software rasteriser of the real mesh files, not the game): `docs/saiyan_armor/previews/saiyan_alloy_set.png` (front, side, back) and `saiyan_alloy_pieces.png`.

## Settings

- **Client `saiyan.autoArmour`** (default OFF): ON restores the old look where a transformed Saiyan draws the cosmetic battle armour by itself (needs `saiyan.armour` too). A worn alloy set always wins: while one or more alloy pieces are worn the cosmetic armour is not drawn (no double drawing). Looks only.
- **Server `nusmp-saiyan-armor-server.toml`**: `setBonus` (default true) and `setKiRegenPercent` (default 1.0, range 0..10). Wearing all four pieces adds that percent of maximum Ki per second to an awakened Saiyan's Ki regeneration (one added term on the existing regen call in `SaiyanManager`). Three pieces give nothing.

## New / replaced / kept

- NEW: the material, ingot, four armour items, recipes, worn models, sprites, plain layers, the set bonus, both settings, the catalog entries, lang keys (`tools/lang/saiyan_armor.json`), self-tests `saiyan.armor.*`, tests, tools (`tools/blender/saiyan_armor.py`, `tools/saiyan_armor/`).
- REPLACED: nothing removed. The default behaviour of forms changes (no automatic armour) but is switched back by `saiyan.autoArmour`.
- KEPT: the cosmetic armour code, geo models, textures and `tools/gen_saiyan_armor_geo.py`.

## Cost (rules 9 and 11)

- Server: one `getItemBySlot` x4 per awakened Saiyan per second. No scan, no allocation, no packets, no entities, no chunk loads. The pieces add no ticking code. Nothing to clean up on logout or death (armour is item state).
- Network: none new.
- Client: per visible wearer with the full set at most 1102 quads in the solid pass (about 4400 vertices) plus about 90 glow-map quads in the additive pass. The battle-armour item is exactly at the library budget of 2600 vertices (`ArmorLooksTest`). The pieces go through the library's existing distance culling and level of detail and the existing perf budgets; no per-frame allocation was added. The cosmetic painter got one more boolean check and an equipment read (four slot reads) and still allocates nothing.

## Made in Blender, and what was not

Made in Blender (`tools/blender/saiyan_armor.py`, `tools/blender/bl_lib.py`, modelled, unwrapped, baked with Cycles): chest, pauldron, vambrace, skirt, greave, scouter. Not made in Blender: the 16x16 inventory sprites and the plain 64x32 vanilla layers (pixel art from `tools/saiyan_armor/gen_icons.py`, as for the other armour). The ingot is a flat sprite.

## Not checked

Nothing was compiled or run: there is no Gradle in this session, CI is the compiler. Nothing was seen in the game; the previews come from the software rasteriser, so the real lighting, the glow pass, the scouter on the head and the pose fit on the player model are unverified. `WornAssetsTest`, `ArmorLooksTest` and the new tests were checked by reading (positions inside 40 px, no EMIT quads, 2600 vertices) but not run.
