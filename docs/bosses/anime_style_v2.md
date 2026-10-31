# Anime skin style v2 (3.37, work in progress build, not released)

Owner request: the bosses and NPCs must look like the Minecraft anime skins of the three reference images (a spiky layered yellow head of hair with a white coat and raised shoulder and arm plates, a silver
voluminous head of hair with a fringe over the eyes and a dark coat with gold trim and steel gauntlets, a brown layered head of hair with a teal and gold tabard, a gold collar and armoured boots).
What they share: volume from the hair geometry and the outer skin layers, shaded eyes, strong light-to-shade clusters with a few bright pixels, trims and plates standing off the body on the overlay layers.

## What changed

| | Kind | What |
|---|---|---|
| Tier boss skins (17 PNGs `textures/entity/playermodel/<id>.png`) | REPLACED (same file names, repainted) | hair no longer painted flat on the hat layer; scalp on the base head only; plates, collar, tabard, cuffed boots, buckle block on the overlay layers; stepped (cel) light; headgear (crown, tiara, headband, ribbon) on row 2 of the hat layer |
| `looks_tier.json` | REPLACED entries (same ids, heights, glow) | every look now has `hair` (an `anime_*` kit style) and `hair_color` |
| 10 hair styles `anime_short`, `anime_short_open`, `anime_spiky`, `anime_swept`, `anime_long`, `anime_long_open`, `anime_afro`, `anime_topknot`, `anime_mane`, `anime_hood` | NEW (appended to `hair_styles.json`, new sheets `hair/anime_*.png`) | chunkier cuts, clump sheets with 4 grey steps and highlight pixels |
| `tools/tier_bosses/anime_skin.py` volume mode, `--flat` flag, `_v2` sheet block, `preview_tier.py --close/--cols` | NEW | the old flat painter is untouched and reachable with `gen_tier_looks.py --flat` |
| The 23 classic hair styles and their sheets, `looks.json`, every other character | KEPT (pixel-identical; `gen_hair.py` no longer rewrites an unchanged sheet) | |
| `TierBossesTest` rule, `LightYagamiDataTest` hair assert, the style-rule paragraph of `tier_f_d.md` | REPLACED (the owner asked for the new rule) | |
| Old arc skins `arc_<id>.png` (fallback of the kit being off) | KEPT | |

Light Yagami keeps his own post (the notebook and the teal cuff line, `POSTS` in `gen_tier_looks.py`); he gets the `anime_swept` hair and the shared collar / cel light like the rest.

## How it is built

* Hair: `tools/playermodel/hair_lib.py` has `clump_grey` / `anime_cap_grey` (clumps 2 to 4 strands wide, a lit band at the root, a shaded lower half, a dark gap between clumps, four grey steps, a few highlight
  pixels) and the extra taper `zigzag`; `Style(..., anime=True)` paints with them. `gen_hair.py` builds the classic 23 first (unchanged), then `build_anime()` appends the ten anime styles.
  The tint pipeline is untouched: sheets stay grey, `HairPalette` tints base / highlight / shade.
* Skin: `anime_skin.paint_character(spec, name, masks, post, volume=True)` returns the kit hair style and the hair rgb (so the look gets `hair` + `hair_color`); `--flat` gives the old result (`bald`).
  `paint_scalp` paints only hairline, crown, sides and back on the base head; `paint_headgear` puts circlets and headbands under the hair; `volume_pass` paints, from the sheet, on the overlay layers:
  shoulder plates (`armor.pieces` has `pauldrons`, or `_v2.pauldrons`), forearm guards (`bracers` / `vambraces`, or `_v2.gauntlets`), a standing collar (the sheet's `collar` with a trim colour, or
  `_v2.collar_color`), a tabard over body and legs (`top.kind == "tabard"` or `_v2.tabard`), cuffed boots, a two-row buckle block on the belt, and a few highlight pixels on the lit side of bright garments.
  `light_pass(stepped=True)` makes the light four bands per part with fold clusters.
* Sheets (`tools/tier_bosses/tier_specs.py`): everything comes from the art kit's vocabulary (top, sleeves, armor, accessories, boots, belt). The only addition is the optional `v2={...}` argument of
  `sheet(...)`, stored as `_v2` and kept OUTSIDE `arc_kit.spec.normalize` (so the shared art kit is unchanged and a sheet without it still works). Keys, all optional (default off):
  `pauldrons`, `gauntlets`, `plate` (plate colour), `plate_trim`, `collar_color`, `collar_high`, `tabard`, `tabard_trim`, `boot_cuff`, `no_boot_cuff`.

## Add a character

1. Add a `sheet(...)` to `tier_specs.py` (a hair style the map knows: short, crop, spiky, side_part, slick, long, afro, topknot, wild_mane, hood; any other style falls back to the classic kit cut).
   Add `v2={...}` for plates, collar or tabard if the character wears them. A crown, tiara, headband or ribbon automatically picks the `_open` hair (no centre fringe) and sits on row 2.
2. `python3 -B tools/tier_bosses/gen_tier_looks.py [id]`, then `python3 -B tools/tier_bosses/preview_tier.py out.png --back --cols 3 [ids]` (or `--close` for the three-quarter bust).
3. Run `TierBossesTest`; it checks the look has an `anime_*` hair and a colour, that the hat layer's top is empty (no flat hair) and that the overlay layers carry details.

## Cost (rule 11)

Zero runtime change in code: textures, the `hair` field in `looks_tier.json`, ten appended styles in `hair_styles.json`. The hair is the existing kit hair (bounded boxes and spring chains,
worst case per style 23 boxes + 14 chain segments = 180 quads; the biggest classic style, mane, is 168), drawn only for nearby entities through the existing hair layer and perf budgets. No new
packets, no server work, no per-tick work. The ten new sheets are 0.7 to 1.1 KB each.

## Not seen in a game

All previews (`docs/previews/tier_style_v2/*.png`) come from the software renderer of `tools/playermodel` (lighting differs from the game; hair springs at rest). Not verified in a client: the hair
physics of the new chains, the look of the clump sheets under the game's lighting, helmets on bosses, the crown and tiara rows under the 3D hair, and the slim-arm fit of the plates (Light, Malty, Rachel,
Tucker use slim arms). The eyes are the 3.33.1 anime eyes, unchanged. Gradle was not run for this change (the pure-Java tests were run with the quick runner, see the report).
