# Relics as held objects (1.48.0)

Owner request: "make 3D models for all my relics that can be seen in hand". Every relic of the 31 waves (about 370 defs) is now a real 3D object in the hand, on the ground and in an item frame, and
keeps its 16x16 sprite as the inventory icon (owner rule 7). Nothing was run in the game (see "What is unverified").

Code: `client/relics/hand/*` (`HandItemRenderer`, `HandForms`, `HandModels`, `HandTransforms`, `HandCatalog`, `HandClient`), one factory in `client/relics/worn/lib/WornLibrary.ofPieces`, a branch in
`ArmorItemRenderer`. Data: `assets/nusmp/relic_hand/` (`transforms.txt` hand written, `catalog.txt` and 109 `<model>.txt` written by `tools/relics/gen_hand_all.py`). Art: `tools/relics/gen_hand_*.py`.
Pictures: `docs/relic_hand/*.png`. Tests: `HandTransformsTest`, `HandCatalogTest`, `RelicHandCoverageTest`.

## What was flat, computed from the code

The renderer of a relic item is chosen by its class (`RelicCatalog.make`): a hand relic is a `RelicWeaponItem` / `RelicBowItem` / `RelicCrossbowItem` (3D mesh, `WeaponRenderer`), head, chest and
feet relics are `RelicArmorItem` (armour), neck, back, belt, hands and charm relics are `RelicGearItem`, consumables are `RelicConsumableItem`. The item json decides how it is drawn in the hand: a
`builtin/entity` json hands the drawing to a Java renderer, `item/generated` is the flat sprite. Reading every json (`tools/relics/relic_inventory.py` finds the 374 defs in the wave sources, 0 unresolved):

| what | count | state before 1.48.0 | state now |
|---|---|---|---|
| hand relics (`RelicSlot.MAIN_HAND`) | 132 (115 new items, 3 left halves of pairs, 14 adopted older items) | 3D mesh in hand (`weapon_meshes/<id>.txt`) | unchanged |
| head relics | 35 | 15 had the 1.34.0 object form, **20 were flat sprites** (their item json was still `item/generated`) | all 35 held as objects |
| feet relics | 9 | 3D object form | held as objects (new hold styles) |
| chest relics | 20 | flat sprite | garment body + their ornaments, or the whole worn model |
| neck relics | 25 | flat sprite | hang from the fingers |
| back relics | 15 | flat sprite | draped cloaks and capes |
| belt relics | 3 | flat sprite | belts held by the buckle |
| hands relics | 26 (+1 adopted) | flat sprite | gauntlets, gloves and bracelets, one of the pair held by the cuff |
| charm relics | 86 | flat sprite | 77 signature objects + 9 worn charm models |
| consumables | 21 (+1 adopted) | flat sprite | 21 signature flasks, vials, jars, scrolls |

So 216 relic items were flat sprites in the hand and 24 had the 1.34.0 object form: **240 non-weapon relics**, all of them now have a held form (124 a signature hand model: 110 an object of their own and 14 a garment body under their ornaments; 92 the model they
wear with a hold style; and the 24 head and feet armour relics of 1.34.0 the same with the new hold styles). The two adopted non-weapon items (the Hand of Providence, the Devil Contract) are older items that already have a 3D mesh.

### The "14 hand relics without `.model3d()`" are not missing a mesh

The brief expected about 14 hand relics with only a flat sprite. They are the 14 hand relics of wave 0 that **adopt an existing item** (`.adopt(NUItems.QUICKLINGS_IVY)` and so on: Quickling's Ivy,
Magebane Longsword, Magebane Shield, Bolt Thrower, Brooch Pin, Club of Rot, Crossbow of Swift Death, Doom Hammer, Weapon of Resurgence, Prism Blade, Permafrost Sabre, Cyclone Rapier,
Bloodbound Katana, Thundercleave): the relic list does not register a new item, so `model3d()` is not on their def, but the older items they adopt already have a mesh in `weapon_meshes`, a row in both
copies of `weapon_classes.json`, a builtin/entity item json and their engravings in `item/WeaponEngravings.java`. `RelicHandCoverageTest.theOlderWeaponsThatAdoptAnExistingItemAreTheFullyModelledToo`
asserts all five for the 14, so nothing was added to `tools/gen_weapon_models.py`, to `weapon_classes.json` or to the Better Combat inventory (rerunning it would change nothing).

## How it works

* **Item json** (`tools/relics/gen_hand_item_models.py`, idempotent): `<id>.json` is `builtin/entity` with no display block and the old sprite model moves to `<id>_icon.json`, exactly the trick of the
  weapons and of the armour. 216 items were converted; the 24 armour relics of 1.34.0 had their preset display block taken out (it is the `sprite` style of `transforms.txt` now).
* **One renderer** `HandItemRenderer` (a `BlockEntityWithoutLevelRenderer`) is registered for every `RelicGearItem` and `RelicConsumableItem` by `HandClient`, and relic armour reaches it through
  `ArmorItemRenderer` (which keeps its older path for every other armour item). In the inventory, with the option off, with no form, or if anything throws, it draws the sprite (the `_icon` model; away
  from the inventory placed by the `sprite` style): it can never draw nothing and never crashes a frame (each failure is logged once).
* **What it draws** (`HandForms`, built once per relic id and cached): the hand catalog's signature models for the relic if it has any, else the models the worn library gives it (its relic entry or the
  models of its slot and look), through `WornLibrary.ofPieces` so the same code (`WornLibDraw.drawItem`) draws them tinted by the relic's palette, with the glow pass and the animation groups (a spinning
  cube, a swinging pendant, a flickering flame). Models are parsed once (`HandModels`), cleared on a resource reload, no per-frame allocation of models or textures.
* **Where it sits** (`HandTransforms` and `transforms.txt`): named hold styles with a `size` (longest side in blocks), a `hold` point (the part of the object in the hand), a `pre` turn, a `sway` and a
  display transform per context (ground, frame, third person right and left, first person right and left, head). A relic gets the style of its id, else of its slot and look, else of its slot.
  A new relic needs no Java.

### Hold styles

| style | used for | how it is held |
|---|---|---|
| `hang` | neck relics, chains, pendants, lantern, watch | by the top of the chain, hanging below the fingers, swinging a little |
| `palm` | charms, orbs, stones, bags, books, figurines | loose in the palm, the object centred |
| `small` | rings, coins, badges, brooches | small in the hand, larger on the ground and in a frame |
| `tool` | spoon, key, lyres, horn, mirror, chalk, spindle, the gauntlets | gripped low by the handle |
| `flask` | every consumable, perfume, sunscreen, jar of bats | gripped round the body |
| `brim` | hoods, hats, crowns, circlets, masks, helms | by the brim, tilted |
| `cuff` | gloves, gauntlets, bracers (one of the pair) | by the cuff, fingers hanging |
| `cloak` | capes and cloaks | draped from the hand |
| `garment` | chest relics | by the collar, hanging |
| `coil` | belts, the three collars | by the buckle or clasp so the ring hangs as a loop |
| `boots` | feet relics | the pair held by the straps |
| `sprite` | fallback | the flat sprite as a vanilla generated item sits |

In the third person hand the item's +y points forward and +z up (the arm is rotated by `ItemInHandLayer`), so `rx 90, ry 180` stands an object upright with its front to the onlooker; in first person
the item is 0.56 right, 0.52 down and 0.72 ahead of the camera, so `rx -20, ry -38` turns its front to the eye. The left hand is written out in full (a test checks it is the mirror of the right).
`size`/`pre` act everywhere, `hold`/`sway` only in a hand (on the ground and in a frame the object is centred).

## The models

**109 hand models** (83 signature + 21 consumable + 5 garment) in `assets/nusmp/relic_hand/<name>.txt`, in the quad format of the worn models (`gen_worn_lib.write_model`: quads with a colour slot, a tile of the shared atlas, flags and animation
groups), at most 2484 vertices each (the 2600 budget of an ensemble, tested for every relic including its ornaments). Built with the owner's method (`mesh_lib`: named parts, lofts, lathes, smoothed
normals), authored Y up with the front at -Z, 1 unit = 1 model pixel.

* `tools/relics/gen_hand_sig.py` (30): the Infinity Stones (`stone_space` cube in its frame with a spinning inner cube, `stone_mind` crystal in claws, `stone_power` faceted gem on a plate,
  `stone_time` the Eye of Agamotto with turning eyelids, `stone_reality` the aether with a turning ribbon, `stone_soul` crystal with a wisp), `infinity_gauntlet` (six differently cut stones),
  `hand_of_vecna`, `eye_of_vecna_hand`, `deck_of_many_things`, `warrens_key`, `soup_spoon` (spiral handle, bowl with a swirl), `cooking_pot` (lid, handles, rune, steam), `spirit_lantern`,
  `lyre_bewitching`, `lyre_silver_tongued`, `horn_of_leroy`, `pocket_watch`, `monster_compass` (swinging needle), `lucky_egg`, `rose_petal`, the collars (`collar_chromatic`, `collar_penitent`,
  `collar_fenrir`), the medallions (`medallion_sunheart`, `medallion_witcher`), `eye_of_aboleth`, `orb_plagues`, `orb_dryad`, `holy_handgrenade`.
* `tools/relics/gen_hand_trinkets.py` (53): ten rings (`ring_rage`, `ring_memories`, `ring_power`, `ring_regeneration`, `ring_rock_eating`, `ring_game`, `ring_mimic`, `ring_forgotten`, `ring_witch`,
  `ring_dryad`), `eye_deep_one`, eight bags (`bag_holding`, `bag_devouring`, `bag_kittens`, `bag_colding`, `bag_golding`, `bag_molding`, `bag_scolding`, `bag_unfolding`), `satchel_unmet`,
  `satchel_dragon`, `wallet_slayer`, `pouch_sand`, `doll_snugglable`, `perfume_bottle`, `sunscreen_bottle`, `jar_bats`, `idol_god`, `idol_strange`, `runechalk`, `spindle_weaversong`, three coins,
  `hearts_divide`, `elephant_room`, `mirror_spell`, `tent_campsite`, `crystal_dragon`, `cupcake_madama`, `notebook` (five relics), `tome_ornate`, `tome_illegal`, `dial_defender`, `orbit_tiny`,
  `ghost_steps`, `badge_official`, `brooch_poeur`, `sigil_flame`, `clasp_true_heart`, `badge_fury`, `puppet_lumpy`, `pact_cards`.
* `tools/relics/gen_hand_consumables.py` (21 models for the 21 consumables): `jar_honey`, `box_chocolates`, `tankard_ale`, `mug_boss`, `vial_holy`, `vial_black`, `flask_poison`, `orb_portal`,
  `flask_vampire`, `flask_sleep`, `jar_spider`, `bottle_genie`, `sack_marbles`, `scroll_scrawl`, `scroll_sealed` and the bargain-bin scroll in six emblems (`scroll_bargain_charm|fire|rock|crab|mace|copy`).
* `tools/relics/gen_hand_garments.py` (5 written): `garment_cuirass`, `garment_cuirass_lite`, `garment_coat`, `garment_coat_lite`, `garment_vest` (a body under the ornaments of a chest relic,
  see below; `garment_robe`, `garment_robe_lite` and `garment_vest_lite` are defined and written only when a chest relic has room for them).

That is **83 signature models for individual relics** (30 + 53, more than the 40 asked for) plus 21 consumable models plus 5 garment bodies = 109. `ASSIGN` in each module says which relic is held
as which model; `gen_hand_all.py` writes the models and `catalog.txt`.

**Chest relics** are only ornaments over a painted body in the worn library (a collar, two pauldrons, a pair of coat tails, crossed straps), so held alone they were scattered pieces. `gen_hand_all.py`
puts the garment body of the relic's look (plate cuirass, coat, leather vest) under its own ornaments (14 relics); a relic that already wears a whole garment (`suit_astral`, `chest_shadow`, `chest_mist`)
or whose ornaments leave no budget for a body (`nekets_practical_mage_robe`) is held as its worn model.

## How a friend adds a hand model for a new relic

1. A new relic that only needs its worn model held (a ring, a hood, a cloak of an existing look): add the def as usual, then `python3 tools/relics/gen_hand_item_models.py` (turns its item json into the
   `builtin/entity` model and keeps its sprite as the icon). `RelicHandCoverageTest` fails until you do.
2. A new relic with its own object: two lines and a function. In a `gen_hand_*.py` module write `@model def my_object(): ...` (look at a neighbour; frame in `gen_hand_kit.py`) and add
   `"my_relic_id": ["my_object"],` to its `ASSIGN`; run `python3 tools/relics/gen_hand_all.py` and `python3 tools/relics/hand_preview.py my_relic_id`.
3. To change how a relic is held add one line to `transforms.txt`: `use id:my_relic_id hang size=0.7` (any style, with overrides) and look again.

Run order when the worn library changes: `gen_worn_all.py` first (the chest lines of the hand catalog read the worn catalog), then `gen_hand_all.py`, then `gen_hand_item_models.py`.

## Safety and budget

* Every hand model <= 2600 vertices, every relic's drawn pieces together <= 2600 (`HandCatalogTest`, `gen_hand_all.py`), the same cap as `ArmorLooksTest` and `WornMeshTest`.
* Parsed once and cached, cleared on a resource reload (`HandClient`'s reload listener); the pose maths allocates nothing per frame but the pose stack's own matrices.
* Falls back to the sprite when the model is missing, the transforms file is incomplete, `relicfx.models` or the new `relicfx.handModels` (client config, default true) is off, or anything throws.
  With `handModels` off the relic armour draws its 1.34.0 object form as before.
* A release cannot add a flat sprite by mistake: `RelicHandCoverageTest` reads the wave sources and fails for any relic with no held form, a json that is not builtin/entity, a missing `_icon` model, or a
  `use` line that names a relic, slot or look that does not exist.

## Pictures (`docs/relic_hand/`)

`objects_01..09.png` every hand model on its own in the colours of the relic that holds it; `held_*.png` nine sheets of six relics each, one row per relic (third person from the front, close and from the side, first person,
ground, item frame, sprite); `all_<slot>.png` every relic of a slot as a small cell. Made by `python3 tools/relics/hand_contact_sheet.py` with the software rasteriser `tools/relics/hand_preview.py`, which
replicates the pose chain of `ItemInHandLayer`, the first person offset of `ItemInHandRenderer`, the ground hover and the item frame turn. Honest reading of them: the silhouettes and the colours read well
(a lyre is a lyre, the Deck is a card case, the stones are six different cut stones, the scrolls and flasks are recognisable at hand size); the way things sit is plausible but only as good as the
replication of the game's transforms. The arm is a plain box, the player is not animated, and the first person view is a pinhole approximation that has no arm.

## What is unverified in the game

* Nothing was run in the game; there is no compiler here beyond the stub type check (no error in the new classes except the stubs' missing client packages) and the JUnit shim (17 hand tests pass).
* The exact placement per context is derived from the vanilla pose chain, not seen: third person might need nudging of `size`, `hold` or the `tp_*` rows, first person the `fp_*` rows, the item frame
  assumes the vanilla turn that makes a sprite face out (`fixed` is `ry 180`; the 1.34.0 armour renderer turned models the other way), and the ground rows assume the vanilla hover. All of it is in
  `transforms.txt`, no recompile.
* `BlockEntityWithoutLevelRenderer` items get no enchantment glint; relics that are foil lose the glint in the hand (their glow comes from the models instead).
* The glow passes (`RenderType.eyes` over the atlas) are the worn library's; an emissive quad lit by full bright on a very bright palette (holy water, honey) is brighter in the hand than in the preview.
* A resource pack that replaces `models/item/<id>.json` with a generated model simply gets the flat sprite back (the renderer is only used by builtin/entity models).
* `tools/relics/wave*_icons.py` write the flat json; after running one run `gen_hand_item_models.py` again.

## The computed list (240 non-weapon relics)

The generator `tools/relics/hand_report.py` prints it; "was" for the 24 armour relics of 1.34.0 is the record `tools/relics/hand_models_before.txt`.

| slot | relic id | name | was | now | models | hold style |
|---|---|---|---|---|---|---|
| BACK | `autumn_cloak` | Autumn Cloak | flat sprite | worn model | cloak_long, deco_leaves | cloak |
| BACK | `cape_of_bees` | Cape of Bees | flat sprite | worn model | cape_short, deco_bees | cloak |
| BACK | `cape_of_nine_lives` | Cape of Nine Lives | flat sprite | worn model | cape_nine_lives | cloak |
| BACK | `cape_of_projection` | Cape of Projection | flat sprite | worn model | cape_short | cloak |
| BACK | `cloak_mimic_tame` | Cloak Mimic, tame | flat sprite | worn model | cloak_mimic | cloak |
| BACK | `cloak_of_comfort` | Cloak of Comfort | flat sprite | worn model | cloak_long | cloak |
| BACK | `cloak_of_dichotomy` | Cloak of Dichotomy | flat sprite | worn model | cloak_split | cloak |
| BACK | `cloak_of_levitation` | Cloak of Levitation | flat sprite | worn model | cloak_levitation | cloak |
| BACK | `cloak_of_the_depths` | Cloak of the Depths | flat sprite | worn model | cloak_scaled | cloak |
| BACK | `cloak_of_the_first_snow` | Cloak of the First Snow | flat sprite | worn model | cloak_furred, deco_flakes | cloak |
| BACK | `cloak_of_the_moth_rider` | Cloak of the Moth Rider | flat sprite | worn model | wings_moth | cloak |
| BACK | `darkened_cloak_of_elvenkind` | Darkened Cloak of Elvenkind | flat sprite | worn model | cloak_elven, deco_leaves | cloak |
| BACK | `handy_towel_cape` | Handy Towel-Cape | flat sprite | worn model | cape_striped | cloak |
| BACK | `mantle_of_selflessness` | Mantle of Selflessness | flat sprite | worn model | mantle_shoulder, deco_halo_back | cloak |
| BACK | `nightstalkers_mantle` | Nightstalker's Mantle | flat sprite | worn model | mantle_shoulder, deco_wisps | cloak |
| BELT | `alchemists_belt` | Alchemist's Belt | flat sprite | worn model | belt_alchemist | coil |
| BELT | `artifact_mimic` | Artifact Mimic | flat sprite | worn model | belt_buckled | coil |
| BELT | `buffoons_pantaloons` | Buffoon's Pantaloons | flat sprite | worn model | belt_buckled | coil |
| CHARM | `alabaster_white_runechalk` | Alabaster White Runechalk | flat sprite | signature hand model | runechalk | tool |
| CHARM | `arcanists_notebook` | Arcanist's Notebook | flat sprite | signature hand model | notebook | palm |
| CHARM | `bag_of_colding` | Bag of Colding | flat sprite | signature hand model | bag_colding | palm |
| CHARM | `bag_of_devouring` | Bag of Devouring | flat sprite | signature hand model | bag_devouring | palm |
| CHARM | `bag_of_golding` | Bag of Golding | flat sprite | signature hand model | bag_golding | palm |
| CHARM | `bag_of_holding` | Bag of Holding | flat sprite | signature hand model | bag_holding | palm |
| CHARM | `bag_of_kittens` | Bag of Kittens | flat sprite | signature hand model | bag_kittens | palm |
| CHARM | `bag_of_molding` | Bag of Molding | flat sprite | signature hand model | bag_molding | palm |
| CHARM | `bag_of_realistic_capacity` | Bag of Realistic Capacity | flat sprite | worn model | charm_pouch | palm |
| CHARM | `bag_of_scolding` | Bag of Scolding | flat sprite | signature hand model | bag_scolding | palm |
| CHARM | `bag_of_unfolding` | Bag of Unfolding | flat sprite | signature hand model | bag_unfolding | palm |
| CHARM | `bags_of_transferrence` | Bags of Transferrence | flat sprite | worn model | charm_pouch_pair | palm |
| CHARM | `bewitching_lyre` | Bewitching Lyre | flat sprite | signature hand model | lyre_bewitching | tool |
| CHARM | `botanists_notebook` | Botanist's Notebook | flat sprite | signature hand model | notebook | palm |
| CHARM | `coin_of_draconic_luck` | Coin of Draconic Luck | flat sprite | signature hand model | coin_draconic | small |
| CHARM | `coin_of_fortune` | Coin of Fortune | flat sprite | signature hand model | coin_fortune | small |
| CHARM | `crystal_dragon` | Crystal Dragon | flat sprite | signature hand model | crystal_dragon | palm |
| CHARM | `deck_of_many_things` | Deck of Many Things | flat sprite | signature hand model | deck_of_many_things | palm |
| CHARM | `defenders_dial` | Defender's Dial | flat sprite | signature hand model | dial_defender | small |
| CHARM | `dragon_satchel` | Dragon Satchel | flat sprite | signature hand model | satchel_dragon | palm |
| CHARM | `drow_sunscreen` | Drow Sunscreen | flat sprite | signature hand model | sunscreen_bottle | flask |
| CHARM | `dryads_wedding_band` | Dryad's Wedding Band | flat sprite | signature hand model | ring_dryad | small |
| CHARM | `elephant_of_the_room` | Elephant of The Room | flat sprite | signature hand model | elephant_room | palm |
| CHARM | `eye_of_the_deep_one` | Eye of the Deep One | flat sprite | signature hand model | eye_deep_one | small |
| CHARM | `fast_travel_beads` | Fast Travel Beads | flat sprite | worn model | charm_beads | hang |
| CHARM | `fury_of_the_fallen` | Fury of the Fallen | flat sprite | signature hand model | badge_fury | small |
| CHARM | `historians_notebook` | Historian's Notebook | flat sprite | signature hand model | notebook | palm |
| CHARM | `holy_chains` | Holy Chains | flat sprite | worn model | charm_beads | palm |
| CHARM | `holy_handgrenade` | Holy Handgrenade | flat sprite | signature hand model | holy_handgrenade | palm |
| CHARM | `horn_of_leroy` | Horn of Leroy | flat sprite | signature hand model | horn_of_leroy | tool |
| CHARM | `instant_cooking_pot` | Instant Cooking Pot | flat sprite | signature hand model | cooking_pot | palm |
| CHARM | `jar_of_bats` | Jar of Bats | flat sprite | signature hand model | jar_bats | flask |
| CHARM | `lifes_emergency` | Life's Emergency | flat sprite | worn model | charm_heart | palm |
| CHARM | `lucky_egg` | Lucky Egg | flat sprite | signature hand model | lucky_egg | hang |
| CHARM | `lumpy` | Lumpy | flat sprite | signature hand model | puppet_lumpy | palm |
| CHARM | `madama_cupcake` | Madama Cupcake | flat sprite | signature hand model | cupcake_madama | palm |
| CHARM | `martels_magical_bag_of_candy` | Martel's Magical Bag of Candy | flat sprite | worn model | charm_flask | flask |
| CHARM | `mirror_of_spell_reflection` | Mirror of Spell Reflection | flat sprite | signature hand model | mirror_spell | tool |
| CHARM | `monster_compass` | Monster Compass | flat sprite | signature hand model | monster_compass | palm |
| CHARM | `official_looking_badge` | Official-Looking Badge | flat sprite | signature hand model | badge_official | small |
| CHARM | `old_masters_spellbook` | Old Master's Spellbook | flat sprite | signature hand model | tome_ornate | palm |
| CHARM | `orb_of_plagues` | Orb of Plagues | flat sprite | signature hand model | orb_plagues | palm |
| CHARM | `orb_of_the_dryad` | Orb of the Dryad | flat sprite | signature hand model | orb_dryad | palm |
| CHARM | `pact_of_the_deck` | Pact of the Deck | flat sprite | signature hand model | pact_cards | palm |
| CHARM | `paralyzing_perfume` | Paralyzing Perfume | flat sprite | signature hand model | perfume_bottle | flask |
| CHARM | `phantom_steps` | Phantom Steps | flat sprite | signature hand model | ghost_steps | palm |
| CHARM | `physicians_notebook` | Physician's Notebook | flat sprite | signature hand model | notebook | palm |
| CHARM | `plastic_sheriff_badge` | Plastic Sheriff Badge | flat sprite | worn model | brooch_badge | small |
| CHARM | `pocket_time` | Pocket Time | flat sprite | signature hand model | pocket_watch | hang |
| CHARM | `portable_god` | Portable God | flat sprite | signature hand model | idol_god | palm |
| CHARM | `ring_of_endless_rage` | Ring of Endless Rage | flat sprite | signature hand model | ring_rage | small |
| CHARM | `ring_of_memories` | Ring of Memories | flat sprite | signature hand model | ring_memories | small |
| CHARM | `ring_of_power` | Ring of Power | flat sprite | signature hand model | ring_power | small |
| CHARM | `ring_of_regeneration` | Ring of Regeneration | flat sprite | signature hand model | ring_regeneration | small |
| CHARM | `ring_of_rock_eating` | Ring of Rock Eating | flat sprite | signature hand model | ring_rock_eating | small |
| CHARM | `ring_of_the_game` | Ring of the Game | flat sprite | signature hand model | ring_game | small |
| CHARM | `ring_of_the_mimic` | Ring of the Mimic | flat sprite | signature hand model | ring_mimic | small |
| CHARM | `rose_petal` | Rose Petal | flat sprite | signature hand model | rose_petal | palm |
| CHARM | `sand_of_the_pocket` | Sand of the Pocket | flat sprite | signature hand model | pouch_sand | palm |
| CHARM | `satchel_of_unmet_needs` | Satchel of Unmet Needs | flat sprite | signature hand model | satchel_unmet | palm |
| CHARM | `shared_pouches_of_transfer` | Shared Pouches of Transfer | flat sprite | worn model | charm_pouch_pair | palm |
| CHARM | `silver_tongued_lyre` | Silver Tongued Lyre | flat sprite | signature hand model | lyre_silver_tongued | tool |
| CHARM | `simple_blue_runechalk` | Simple Blue Runechalk | flat sprite | signature hand model | runechalk | tool |
| CHARM | `snugglable_doll` | Snugglable Doll | flat sprite | signature hand model | doll_snugglable | palm |
| CHARM | `soup_spoon_of_improbability` | Soup Spoon of Improbability | flat sprite | signature hand model | soup_spoon | tool |
| CHARM | `spellbook_of_concentration` | Spellbook of Concentration | flat sprite | signature hand model | tome_ornate | palm |
| CHARM | `spirit_lantern` | Spirit Lantern | flat sprite | signature hand model | spirit_lantern | hang |
| CHARM | `strange_idol` | Strange Idol | flat sprite | signature hand model | idol_strange | palm |
| CHARM | `the_devils_due` | The Devil's Due | flat sprite | signature hand model | coin_devils_due | small |
| CHARM | `the_forgotten_ring` | The Forgotten Ring | flat sprite | signature hand model | ring_forgotten | small |
| CHARM | `the_lovers_divide` | The Lover's Divide | flat sprite | signature hand model | hearts_divide | small |
| CHARM | `the_mind_stone` | The Mind Stone | flat sprite | signature hand model | stone_mind | palm |
| CHARM | `the_pocket_campsite` | The Pocket Campsite | flat sprite | signature hand model | tent_campsite | palm |
| CHARM | `the_power_stone` | The Power Stone | flat sprite | signature hand model | stone_power | palm |
| CHARM | `the_reality_stone` | The Reality Stone | flat sprite | signature hand model | stone_reality | palm |
| CHARM | `the_soul_stone` | The Soul Stone | flat sprite | signature hand model | stone_soul | palm |
| CHARM | `the_space_stone_or_the_tesseract` | The Space Stone, or the Tesseract | flat sprite | signature hand model | stone_space | palm |
| CHARM | `the_time_stone_or_the_eye_of_agamotto` | The Time Stone, or the Eye of Agamotto | flat sprite | signature hand model | stone_time | hang |
| CHARM | `the_tome_of_wait_thats_illegal` | The Tome of \"Wait, That's Illegal\" | flat sprite | signature hand model | tome_illegal | palm |
| CHARM | `theologians_notebook` | Theologian's Notebook | flat sprite | signature hand model | notebook | palm |
| CHARM | `tiny_orbit` | Tiny Orbit | flat sprite | signature hand model | orbit_tiny | small |
| CHARM | `urn_of_black_pudding` | Urn of Black Pudding | flat sprite | worn model | charm_urn | flask |
| CHARM | `wallet_of_the_slayer` | Wallet of the Slayer | flat sprite | signature hand model | wallet_slayer | palm |
| CHARM | `warrens_key` | Warren's Key | flat sprite | signature hand model | warrens_key | tool |
| CHARM | `weaversong` | Weaversong | flat sprite | signature hand model | spindle_weaversong | tool |
| CHARM | `witchs_ring` | Witch's Ring | flat sprite | signature hand model | ring_witch | small |
| CHEST | `arcane_conduction_armor` | Arcane Conduction Armor | flat sprite | worn model | chest_conduction, coil_arm@rarm, coil_arm@larm~ | garment |
| CHEST | `armor_of_plot` | Armor of Plot | flat sprite | worn model | chest_plot, pauldron_plate@rarm, pauldron_plate@larm~, deco_halo_back | garment |
| CHEST | `armor_of_the_beheaded` | Armor of the Beheaded | flat sprite | signature hand model | garment_cuirass, chest_collar, pauldron_skull@rarm, pauldron_plate@larm~ | garment |
| CHEST | `armor_of_the_defiant_dawn` | Armor of the Defiant Dawn | flat sprite | signature hand model | garment_cuirass_lite, chest_dawn, pauldron_rays@rarm, pauldron_rays@larm~ | garment |
| CHEST | `armor_of_the_lionheart` | Armor of the Lionheart | flat sprite | signature hand model | garment_cuirass, chest_collar, pauldron_lion@rarm, pauldron_lion@larm~ | garment |
| CHEST | `armour_of_the_stone_sentinel` | Armour of the Stone Sentinel | flat sprite | signature hand model | garment_cuirass_lite, chest_stone, pauldron_stone@rarm, pauldron_stone@larm~ | garment |
| CHEST | `coat_of_the_duelist` | Coat of the Duelist | flat sprite | signature hand model | garment_coat, coat_tails | garment |
| CHEST | `delphynes_cursed_armor` | Delphyne's Cursed Armor | flat sprite | signature hand model | garment_cuirass, chest_collar, pauldron_horned@rarm, pauldron_horned@larm~ | garment |
| CHEST | `false_dragon_armor` | False Dragon Armor | flat sprite | signature hand model | garment_cuirass, chest_collar, pauldron_dragon@rarm, pauldron_dragon@larm~ | garment |
| CHEST | `mistwalker_robe` | Mistwalker Robe | flat sprite | worn model | chest_mist, sleeves_flared@rarm, sleeves_flared@larm~, deco_wisps | garment |
| CHEST | `nekets_practical_mage_robe` | Neket's Practical Mage Robe | flat sprite | worn model | chest_neket, sleeve_plain@rarm, sleeve_plain@larm~ | garment |
| CHEST | `shadow_robes` | Shadow Robes | flat sprite | worn model | chest_shadow, hood_shadow, sleeves_flared@rarm, sleeves_flared@larm~ | garment |
| CHEST | `skitchs_smuggling_coat` | Skitch's Smuggling Coat | flat sprite | signature hand model | garment_coat_lite, coat_smuggler | garment |
| CHEST | `skycrystal_armor` | Skycrystal Armor | flat sprite | signature hand model | garment_cuirass, chest_crystal, pauldron_crystal@rarm, pauldron_crystal@larm~ | garment |
| CHEST | `slime_armour` | Slime Armour | flat sprite | signature hand model | garment_cuirass, chest_slime, pauldron_slime@rarm, pauldron_slime@larm~ | garment |
| CHEST | `soul_armor` | Soul Armor | flat sprite | signature hand model | garment_cuirass_lite, chest_soul, pauldron_plate@rarm, pauldron_plate@larm~, deco_wisps | garment |
| CHEST | `starstudded_leathers` | Starstudded Leathers | flat sprite | signature hand model | garment_vest, chest_stars, pauldron_leather@rarm | garment |
| CHEST | `suit_of_the_astral_overseer` | Suit of the Astral Overseer | flat sprite | worn model | suit_astral | garment |
| CHEST | `suit_of_the_golden_shadow` | Suit of the Golden Shadow | flat sprite | signature hand model | garment_coat, suit_tails | garment |
| CHEST | `zar_sancuis_plate_of_the_red_prophet` | Z'ar Sancuis - Plate of the Red Prophet | flat sprite | signature hand model | garment_cuirass_lite, chest_prophet, pauldron_prophet@rarm, pauldron_prophet@larm~ | garment |
| CONSUMABLE | `ale_lemental` | Ale-lemental | flat sprite | signature hand model | tankard_ale | flask |
| CONSUMABLE | `blood_chocolates` | Blood Chocolates | flat sprite | signature hand model | box_chocolates | flask |
| CONSUMABLE | `executioners_bane` | Executioner's Bane | flat sprite | signature hand model | vial_black | hang |
| CONSUMABLE | `fey_honey` | Fey Honey | flat sprite | signature hand model | jar_honey | flask |
| CONSUMABLE | `genie_gin` | Genie Gin | flat sprite | signature hand model | bottle_genie | flask |
| CONSUMABLE | `holy_water` | Holy Water | flat sprite | signature hand model | vial_holy | flask |
| CONSUMABLE | `mad_marbles` | Mad Marbles | flat sprite | signature hand model | sack_marbles | flask |
| CONSUMABLE | `mug_of_the_boss` | Mug of The Boss | flat sprite | signature hand model | mug_boss | flask |
| CONSUMABLE | `perculiar_puking_poison` | Perculiar Puking Poison | flat sprite | signature hand model | flask_poison | flask |
| CONSUMABLE | `pocket_portal` | Pocket Portal | flat sprite | signature hand model | orb_portal | palm |
| CONSUMABLE | `potion_of_vampirism` | Potion of Vampirism | flat sprite | signature hand model | flask_vampire | flask |
| CONSUMABLE | `romlos_potion_of_sleep` | Romlo's Potion of Sleep | flat sprite | signature hand model | flask_sleep | flask |
| CONSUMABLE | `scroll_of_absorption` | Scroll of Absorption | flat sprite | signature hand model | scroll_sealed | flask |
| CONSUMABLE | `scroll_of_charm_parson_bargain_bin_scrolls_1` | Scroll of Charm Parson (Bargain Bin Scrolls #1) | flat sprite | signature hand model | scroll_bargain_charm | flask |
| CONSUMABLE | `scroll_of_facsimile_copy_bargain_bin_scrolls_6` | Scroll of Facsimile Copy (Bargain Bin Scrolls #6) | flat sprite | signature hand model | scroll_bargain_copy | flask |
| CONSUMABLE | `scroll_of_firball_bargain_bin_scrolls_2` | Scroll of Firball (Bargain Bin Scrolls #2) | flat sprite | signature hand model | scroll_bargain_fire | flask |
| CONSUMABLE | `scroll_of_mace_hand_bargain_bin_scrolls_5` | Scroll of Mace Hand (Bargain Bin Scrolls #5) | flat sprite | signature hand model | scroll_bargain_mace | flask |
| CONSUMABLE | `scroll_of_speak_with_rocks_bargain_bin_scrolls_3` | Scroll of Speak with Rocks (Bargain Bin Scrolls #3) | flat sprite | signature hand model | scroll_bargain_rock | flask |
| CONSUMABLE | `scroll_of_summon_greater_crab_bargain_bin_scrolls_4` | Scroll of Summon Greater Crab (Bargain Bin Scrolls #4) | flat sprite | signature hand model | scroll_bargain_crab | flask |
| CONSUMABLE | `scroll_of_unreadable_spells` | Scroll of Unreadable Spells | flat sprite | signature hand model | scroll_scrawl | flask |
| CONSUMABLE | `spider_brew` | Spider Brew | flat sprite | signature hand model | jar_spider | flask |
| FEET | `boots_of_burglery` | Boots of Burglery | 1.34.0 object form | worn model | boots_shadow@rleg, boots_shadow@lleg | boots |
| FEET | `boots_of_the_high_ground` | Boots of The High Ground | 1.34.0 object form | worn model | greaves_plated@rleg, greaves_plated@lleg | boots |
| FEET | `boots_of_the_indomitable` | Boots of the Indomitable | 1.34.0 object form | worn model | boots_war@rleg, boots_war@lleg | boots |
| FEET | `dragon_fire_boots` | Dragon Fire Boots | 1.34.0 object form | worn model | boots_cuffed@rleg, boots_cuffed@lleg, deco_flames | boots |
| FEET | `greaves_of_the_wind_walker` | Greaves of the Wind Walker | 1.34.0 object form | worn model | greaves_windwalker@rleg, greaves_windwalker@lleg | boots |
| FEET | `mudwalkers` | Mudwalkers | 1.34.0 object form | worn model | boots_mud@rleg, boots_mud@lleg | boots |
| FEET | `ruby_boots_of_planestriding` | Ruby Boots of Planestriding | 1.34.0 object form | worn model | boots_crystal@rleg, boots_crystal@lleg | boots |
| FEET | `sandals_of_sneaking` | Sandals of Sneaking | 1.34.0 object form | worn model | sandals_winged@rleg, sandals_winged@lleg | boots |
| FEET | `waterwalkers` | Waterwalkers | 1.34.0 object form | worn model | boots_water@rleg, boots_water@lleg | boots |
| HANDS | `arcane_tricksters_glove` | Arcane Trickster's Glove | flat sprite | worn model | glove_cuffed@rarm, glove_cuffed@larm~ | cuff |
| HANDS | `bear_bracer` | Bear Bracer | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `bears_grip` | Bear's Grip | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `bracelets_of_faithful_friendship` | Bracelets of Faithful Friendship | flat sprite | worn model | bracelet_charms@rarm, ring_gem@larm~ | palm |
| HANDS | `bracer_of_initiative` | Bracer of Initiative | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `bracers_of_arcane_blades` | Bracers of Arcane Blades | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `bracers_of_manhandling` | Bracers of Manhandling | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `bracers_of_phasing` | Bracers of Phasing | flat sprite | worn model | bracer_phasing@rarm, bracer_phasing@larm~ | cuff |
| HANDS | `claws_of_greed` | Claws of Greed | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `dragons_fire_gauntlet` | Dragon's Fire Gauntlet | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `elemental_hand_wraps` | Elemental Hand Wraps | flat sprite | worn model | wraps_hand@rarm, wraps_hand@larm~ | cuff |
| HANDS | `gauntlet_mimic` | Gauntlet Mimic | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `gauntlet_of_the_stormcaller` | Gauntlet of the Stormcaller | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `gauntlets_of_eldritch_ferocity` | Gauntlets of Eldritch Ferocity | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `glove_of_appreciation` | Glove of Appreciation | flat sprite | worn model | glove_cuffed@rarm, glove_cuffed@larm~ | cuff |
| HANDS | `gloves_of_instant_cleanliness` | Gloves of Instant Cleanliness | flat sprite | worn model | glove_cuffed@rarm, glove_cuffed@larm~ | cuff |
| HANDS | `gloves_of_opportunity` | Gloves of Opportunity | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `goristro_charger` | Goristro Charger | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `hand_of_vecna` | Hand of Vecna | flat sprite | signature hand model | hand_of_vecna | tool |
| HANDS | `hellfire_gauntlet` | Hellfire Gauntlet | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `phantoms_bracelet` | Phantom's Bracelet | flat sprite | worn model | bracelet_charms@rarm, ring_gem@larm~ | palm |
| HANDS | `proficiency_gloves` | Proficiency Gloves | flat sprite | worn model | glove_cuffed@rarm, glove_cuffed@larm~ | cuff |
| HANDS | `the_gauntlet_of_gamelon` | The Gauntlet of Gamelon | flat sprite | worn model | bracer_plated@rarm, bracer_plated@larm~ | cuff |
| HANDS | `the_infinity_gauntlet` | The Infinity Gauntlet | flat sprite | signature hand model | infinity_gauntlet | tool |
| HANDS | `thermo_gloves` | Thermo-Gloves | flat sprite | worn model | glove_cuffed@rarm, glove_cuffed@larm~ | cuff |
| HANDS | `zenon_spatial_ring` | Zenon's Spatial Ring | flat sprite | worn model | glove_cuffed@rarm, glove_cuffed@larm~ | cuff |
| HEAD | `arcanists_circlet` | Arcanist's Circlet | 1.34.0 object form | worn model | circlet_arcanist | brim |
| HEAD | `bucket_of_disguise` | Bucket of Disguise | flat sprite | worn model | bucket_of_disguise | brim |
| HEAD | `captains_tricorn` | Captain's Tricorn | 1.34.0 object form | worn model | hat_tricorn | brim |
| HEAD | `circlet_of_the_phoenix` | Circlet of the Phoenix | 1.34.0 object form | worn model | circlet_phoenix | brim |
| HEAD | `corpse_mask` | Corpse Mask | flat sprite | worn model | corpse_mask | brim |
| HEAD | `crown_of_the_conjurer` | Crown of the Conjurer | 1.34.0 object form | worn model | crown_conjurer | brim |
| HEAD | `dante_gravity_crown` | Dante's Gravity Crown | 1.34.0 object form | worn model | crown_spired | brim |
| HEAD | `dark_unicorn_crown` | Dark Unicorn Crown | 1.34.0 object form | worn model | crown_unicorn_dark | brim |
| HEAD | `death_knights_helmet` | Death Knight's Helmet | flat sprite | worn model | death_knights_helmet | brim |
| HEAD | `diadem_of_fabulous_truthiness` | Diadem of Fabulous Truthiness | 1.34.0 object form | worn model | diadem_leaf | brim |
| HEAD | `dreamwalker_cowl` | Dreamwalker Cowl | 1.34.0 object form | worn model | hood_pointed | brim |
| HEAD | `eye_of_vecna` | Eye of Vecna | flat sprite | signature hand model | eye_of_vecna_hand | palm |
| HEAD | `frank` | Frank | flat sprite | worn model | frank | brim |
| HEAD | `godking_crown` | Godking Crown | 1.34.0 object form | worn model | crown_godking | brim |
| HEAD | `goggles_of_dwarvenkin` | Goggles of Dwarvenkin | flat sprite | worn model | goggles_of_dwarvenkin | brim |
| HEAD | `gold_circlet` | Gold Circlet | 1.34.0 object form | worn model | circlet_dragon | brim |
| HEAD | `helm_of_whispers` | Helm of Whispers | flat sprite | worn model | helm_of_whispers | brim |
| HEAD | `helmet_of_headbutting` | Helmet of Headbutting | flat sprite | worn model | helmet_of_headbutting | brim |
| HEAD | `jellyfish_cap` | Jellyfish Cap | flat sprite | worn model | jellyfish_cap | brim |
| HEAD | `lens_of_straight_creepin` | Lens of Straight Creepin' | flat sprite | worn model | lens_of_straight_creepin | brim |
| HEAD | `mask_of_difference` | Mask of Difference | flat sprite | worn model | mask_difference | brim |
| HEAD | `mask_of_indifference` | Mask of Indifference | flat sprite | worn model | mask_indifference | brim |
| HEAD | `mask_of_mind_melting` | Mask of Mind Melting | flat sprite | worn model | mask_of_mind_melting | brim |
| HEAD | `mask_of_the_raven_queens_disciple` | Mask of the Raven Queen's Disciple | flat sprite | worn model | mask_of_the_raven_queens_disciple | brim |
| HEAD | `mask_of_the_witchdoctor` | Mask of the Witchdoctor | flat sprite | worn model | mask_of_the_witchdoctor | brim |
| HEAD | `mind_stone` | Mind stone | flat sprite | worn model | mind_stone | brim |
| HEAD | `prides_crown` | Pride's Crown | 1.34.0 object form | worn model | crown_pride | brim |
| HEAD | `silver_circlet` | Silver Circlet | 1.34.0 object form | worn model | circlet_dragon | brim |
| HEAD | `soul_searchers` | Soul Searchers | flat sprite | worn model | soul_searchers | brim |
| HEAD | `steel_circlet` | Steel Circlet | 1.34.0 object form | worn model | circlet_dragon | brim |
| HEAD | `the_mad_kings_crown` | The Mad King's Crown | flat sprite | worn model | the_mad_kings_crown | brim |
| HEAD | `the_orb_of_ancient_sight` | The Orb of Ancient Sight | flat sprite | worn model | the_orb_of_ancient_sight | brim |
| HEAD | `tiny_duck_crown` | Tiny Duck Crown | 1.34.0 object form | worn model | crown_duck | brim |
| HEAD | `trickster_mask` | Trickster Mask | flat sprite | worn model | trickster_mask | brim |
| HEAD | `unicorn_crown` | Unicorn Crown | 1.34.0 object form | worn model | crown_unicorn | brim |
| NECK | `amulet_of_dragon_rage` | Amulet Of Dragon Rage | flat sprite | worn model | amulet_dragon_claw | hang |
| NECK | `amulet_of_safeguarding` | Amulet of Safeguarding | flat sprite | worn model | amulet_ward | hang |
| NECK | `amulet_of_the_coven` | Amulet of the Coven | flat sprite | worn model | amulet_coven | hang |
| NECK | `amulet_of_the_forest` | Amulet of the Forest | flat sprite | worn model | amulet_forest | hang |
| NECK | `amulet_of_the_huntress` | Amulet of the Huntress | flat sprite | worn model | amulet_medallion | hang |
| NECK | `amulet_of_the_sea_of_stars` | Amulet of the Sea of Stars | flat sprite | worn model | amulet_sea_stars | hang |
| NECK | `amulet_of_the_warmage` | Amulet of the Warmage | flat sprite | worn model | pendant_drop | hang |
| NECK | `arcane_glass_of_seeing` | Arcane Glass of Seeing | flat sprite | worn model | locket_oval | hang |
| NECK | `beast_masters_amulet` | Beast Masters Amulet | flat sprite | worn model | amulet_medallion | hang |
| NECK | `brooch_of_poeur` | Brooch of Po'eur | flat sprite | signature hand model | brooch_poeur | small |
| NECK | `chromatic_collar` | Chromatic Collar | flat sprite | signature hand model | collar_chromatic | coil |
| NECK | `collar_of_the_penitent_dragon` | Collar of the Penitent Dragon | flat sprite | signature hand model | collar_penitent | coil |
| NECK | `crest_of_foresight` | Crest of Foresight | flat sprite | worn model | amulet_crest | hang |
| NECK | `eye_of_the_aboleth` | Eye of the Aboleth | flat sprite | signature hand model | eye_of_aboleth | hang |
| NECK | `fenrirs_collar` | Fenrir's Collar | flat sprite | signature hand model | collar_fenrir | coil |
| NECK | `hemlocks_amulet` | Hemlock's Amulet | flat sprite | worn model | pendant_fang | hang |
| NECK | `lovelorn_locket` | Lovelorn Locket | flat sprite | worn model | locket_oval | hang |
| NECK | `pendant_of_echoed_souls` | Pendant of Echoed Souls | flat sprite | worn model | pendant_echo | hang |
| NECK | `pendant_of_the_dreamer` | Pendant of the Dreamer | flat sprite | worn model | pendant_drop | hang |
| NECK | `sigil_of_the_faint_flame` | Sigil of the Faint Flame | flat sprite | signature hand model | sigil_flame | small |
| NECK | `sunheart_medallion` | Sunheart Medallion | flat sprite | signature hand model | medallion_sunheart | hang |
| NECK | `the_amulet_of_lost_souls` | The Amulet of Lost Souls | flat sprite | worn model | pendant_skull | hang |
| NECK | `true_heart_clasp` | True Heart Clasp | flat sprite | signature hand model | clasp_true_heart | small |
| NECK | `voice_of_the_mountain` | Voice of the Mountain | flat sprite | worn model | pendant_drop | hang |
| NECK | `witchers_medallion` | Witcher's Medallion | flat sprite | signature hand model | medallion_witcher | hang |
