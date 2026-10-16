# Robe sets as anime cloaks (1.48.0)

Owner request: "remake the clover armor to look how it does in the black clover anime so that means it should only be the cloak part then maybe some custom part like asta's headband ect".

## What is new and what is kept
- **New**: `anime:<id>` entries in `assets/nusmp/relic_worn/catalog.txt` for the 44 robe pieces (11 sets x 4), 14 new worn models (`anime_cloak_<set>` x11, `anime_headband`, `anime_hood_down`, `anime_horns_devil`), the client option `relicfx.robeAnime`, the contact sheets `docs/armor_build/anime_sets_a|b|c.png`.
- **Kept (rule 2)**: every older `item <id>` line and model (hood, robe, sleeves, hip band, legs, boots). They are worn when `relicfx.robeAnime = false`. Items, stats, set bonus, recipes and the GeckoLib fallback (mobs, armour stands, `relicfx.models = false`) are unchanged. The transformation modes are untouched.
- The Time Magic VFX files are not touched.

## The slot rule
| Piece | Anime look |
|---|---|
| Chest | The cloak: shoulder drape over the arm tops, long back panel (swings with the stride), emblem on the back, clasp and chain at the throat, collar. No sleeves, no hip band. |
| Helmet | One small accessory or nothing: Asta's headband (Black Bull), a hood laid back on the cloak (Junior, Senior, Heart, Diamond, Spade), small horns (Devil). Golden Dawn, Silver Eagle, Crimson Lion and Coral Peacock draw nothing on the head: their collar belongs to the cloak. |
| Legs, boots | Nothing (catalog piece `none`). The library still "owns" the piece, so the older GeckoLib model stays silent and the player's own legs show. Armour value and durability are item stats and do not change. |
Any subset of the pieces looks right: the cloak works alone, the hood-down and headband work alone.

## Design table
Colours are main / trim / glow (hex). "Emblem" is a relief on the back (plate, raised inner outline, lit stone), the same `_badge` code as the old chest badge, size 1.3 times bigger.
| Set | Cloak | Collar | Hem | Emblem | Head piece | Colours |
|---|---|---|---|---|---|---|
| Junior | short (15), plain | none | smooth | clover, small, no clasp stone | hood laid back | 6B5E50 / B8B0A0 / 90B4E0 |
| Senior | long (21) | stand | smooth | clover | hood laid back | E6E6EA / B42424 / FFB0A0 |
| Golden Dawn | long (21) | stand, gold edge | smooth | sun | none | F2EBD2 / D9B44A / FFE890 |
| Black Bull | long (19.5), heavy folds | none | ragged saw-tooth | bull head | Asta's headband (dark cloth, two tails that trail) | 1C1C20 / E8E4DC / E4B454 |
| Silver Eagle | long (22) | white fur ring | smooth | eagle | none | D5DEE8 / 6A5FB0 / B8E8FF |
| Crimson Lion | long (20) | lion-mane fur ring with six spikes | smooth | lion mane | none | 9A1E1E / E0A040 / FF6A30 |
| Coral Peacock | long (20) | fan of five feathers with lit eyes | feathered points | peacock fan | none | 2FA8A0 / E8806C / 6CF0E0 |
| Heart Kingdom | medium (17) | stand | smooth | heart | hood laid back | E49CB4 / 54B49C / FFB8D8 |
| Diamond | medium (18) | stand | smooth | diamond | hood laid back | 6C543C / CCB484 / 9AF0FF |
| Spade | long (21), heavy folds | stand | light saw-tooth | spade | hood laid back | 34343A / 9C2424 / FF5050 |
| Devil-bound | long (20), heavy folds | stand | deep saw-tooth | horned sigil | small swept-back horns | 0C0C0C / CC0C24 / FF2038 |
Shared: the two front edges are trim, the hem is a trim band, thickness 0.8 model pixels (0.05 block), shoulder pads off.

## Canon recollection versus invented
Honest status: I could not find reference art in the repo (`docs/weapons_spec.md` and `docs/mode_armor_spec.md` describe weapons and transformation modes, not robes), so everything below is from memory of the series.
- **Recalled from the series**: a Magic Knight wears the squad robe over normal clothes with no armour; the cloak is open at the front, held at the neck by a clasp, with the squad emblem on the back; Black Bull is black with a white bull head and a ragged hem; Asta wears a dark headband with two tails; Silver Eagle (Nozel) has a white fur-trimmed cape; Crimson Lion is red with gold; Golden Dawn is white and gold.
- **Less sure**: the Golden Dawn emblem (the sun outline is kept from the old badge), exact Coral Peacock colours, the Heart / Diamond / Spade uniforms (the series shows them with less detail; I used their old palettes and gave them stand collars).
- **Invented**: Junior's brown-grey cloak, Senior's red trim, Silver Eagle's purple lining, Crimson Lion's collar spikes, the Peacock feather fan, the Spade and Devil saw-tooth hems, the Devil horns, the hood laid back on five sets, all fold amounts and lengths, the chain and clasp disc.
Green Mantis and Purple Orca are not sets in the mod and were not added.

## How it works
- `tools/relics/gen_worn_armor.py` (section "1.48.0 ANIME CLOAKS"): `CloakStyle` holds the knobs of a cloak; `_cloak_pt` is the surface (a profile line from the neck down, swept round the back by an angle, folds as outward ripples that leave the emblem panel flat); `_drape` builds a sheet of whole grid columns, so the fixed shoulder drape and the swinging back panel meet without a crack. `ANIME_SETS` is the table above in code; `catalog_anime` writes the `anime:` entries.
- The back panel below the split line is an animation group of kind `swing` (the existing `WornAnim` SWING: the hem trails behind a runner, plus a slow idle breath). The emblem is part of that group. No new animation code.
- Java: `WornLibrary.ofItem` resolves `anime:<path>` first when `RelicFxOptions.robeAnime()` is on and the catalog has it; otherwise the old entry. The cache key includes the look, so flipping the option needs no restart of the cache. A piece whose entries are all `none` becomes an ensemble with no models (`WornCatalog.isNothing`). In hand, on the ground and in frames the chest and helmet show their anime model; a legs or boots piece (nothing to draw) shows the icon. The inventory sprite is unchanged. `ArmorFx` spawns no particles for an empty piece.
- Option: `relicfx.robeAnime` in `config/nusmp-relicfx-client.toml`, plain `define`, default true.

## Budget and checks
Ensemble sizes (vertices, cap 2600): cloaks 1752 to 2424, headband 772, hood 304, horns 336. `python3 tools/relics/gen_worn_all.py --check` also reports older relics over their cap that were already there before this release (elemental_hand_wraps, bracers_of_phasing, shadow_robes); they are not part of this change.
Outer cloth never sinks more than 0.03 px into the body or arm boxes of the preview figure (`arm_clear` check run during the work); the inner face of the 0.8 px thickness is hidden by the body, the top ring of the neck hides inside the head box.
Tests: `ArmorLooksTest` (anime entries for all 44 pieces, chest = one cloak model, legs and boots = `none`, older entries kept, models exist, vertex budget for every `item` line including the anime ones, parser of `none`) and the unchanged `WornMeshTest` (every generated model parses).

## What the sheets show and what I would fix
Look at `docs/armor_build/anime_sets_a.png`, `_b`, `_c` (front, three-quarter, side, back per set, on a plain-box figure):
- The cloak reads as a cloak: shoulder drape, long back panel, clasp at the throat, emblem on the back, trim hem. The headband, horns, hood bunch, fur and feather fan read correctly.
- Weak points: the back panel is boxy (a bell more than a flowing cloak); the join between the fixed drape and the swinging panel shows as a faint shading step; the fold ripples are subtle next to the cloth texture; the stand collar flares like a small vase; the fur ring is crude; the hood-down is a lump; emblems are small and the spade outline still looks like a heart (the old outline is reused). Arms swinging back can touch the panel (the panel sits 0.2 to 0.4 px behind the arm).
- Nothing was run in the game; the preview is a software rasteriser. The sway and the option switch are unverified in game.

## How to add a set
1. Add the robe item pieces as before (NUItems, the old `SETS` row for the older look).
2. Add one row to `ANIME_SETS` (id, cloak key, `CloakStyle`, head model or None, palette, tier, theme); add a new head model with `@model` in `gen_worn_armor.py` if it needs one.
3. `python3 tools/relics/gen_worn_all.py`, then `python3 tools/relics/armor_contact_sheet.py --only anime`, then run the tests.
