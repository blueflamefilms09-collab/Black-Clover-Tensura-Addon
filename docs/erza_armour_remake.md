# Requip armours: the Djinn-quality remake (1.99.0)

The owner's request: "Completely remake all of Erza's Requip magic armor models. The current models are very low quality ('they look the worst'). Use the existing Djinn armors as the strict baseline and standard
for quality, detail and resolution; Erza's new models must match that standard." All fourteen armours of `skill.erza_requip` (Requip: The Knight, `docs/erza_requip.md`) were rebuilt from nothing, as full
sets, with a generator that can be re-run (`tools/gen_erza_armour.py`).

The list of armours is the one in the code (`requip/Armour.java`, the spec `docs/owner_specs/fairy_tail_update_prompt.md`): Heart Kreuz, Heaven's Wheel, Flame Empress, Lightning Empress, Sea Empress, Black
Wing, Clear Heart Clothing, Nakagami, Heaven's Wheel: Grand Chariot, Adamantine, Purgatory, Flight Armor, Wind God, Fairy Armor. The request also named a "Morning Star" and a "Knight / Giant" armour; neither exists in
the code or in the spec (the skill is called *The Knight*), so none was invented: every registered armour was remade and no armour was added.

## NEW / REPLACED / KEPT

| What | Status |
|---|---|
| Every Erza piece model, texture and glow map that existed in 1.96.0 (176 `erza_<armour>_arm_<piece>.geo.json`, `textures/entity/erza_*_arm_*.png` and `_glow.png`): geometry, atlas, paint | REPLACED (written over by the generator under the same file names, so nothing references a missing file) |
| Pieces the armours did not have (112 new models): `gorget`, `backplate`, `belt`, `cuisse_r/l`, `sabaton_r/l` for every armour; `ribbon` for Heart Kreuz, Clear Heart and Flight Armor; `gauntlet_r/l` and `pauldron_r/l` and a `tail` for Clear Heart; a `cape` for Adamantine and Purgatory | NEW |
| The generator: `tools/gen_erza_armour.py` + `gen_erza_armour_core / _parts / _limbs / _extras / _sets.py`, `erza_armour_kit.py`, `erza_armour_paint.py` | NEW |
| `tools/gen_erza_requip_geo.py` (the 1.96.0 generator) | KEPT, but only runs with `--legacy` (it writes the same file names and would overwrite the remake) |
| `requip/ArmourSets.java`: the table of pieces | REPLACED in content (eighteen standard pieces and the signature ones, forging order by weight), API kept (`Piece` record, `of`, `modelId`, `allModelIds`, `maxDelay`), `weightOf` and `anchorOfName` added |
| `requip/ArmourQuality.java` (the measurement the tests and the self-test share) | NEW |
| `tools/erza_armour_audit.py`, `tools/erza_armour_sheets.py`, `tools/gen_erza_armour_doc.py`, `src/test/resources/erza_armour_bar.json` | NEW |
| Tests `ErzaArmourAuditTest`, `ErzaArmourPiecesTest`; self-test `skill.erza_requip2` | NEW |
| Armour ids, mode order, skill id `nusmp:erza_requip`, page ids, saved data (the worn armour is an ordinal), stats, resistances, attacks, costs, `Aura.ERZA`, `ErzaAura`, `EquipAnimation` and `WaterAuraRig` (not touched) | KEPT |
| The six weapons (`flame_empress_blade` ... `purgatory_mace`), the book, the skill icon, their engravings | KEPT (see "Weapons" below) |
| The Djinn files (`client/relics/djinn/**`, `assets/nusmp/djinn/**`, their textures) | KEPT, read only: they are the measured standard |

## The standard: what was measured

`tools/erza_armour_audit.py` reads the files in the jar, nothing is assumed. For the Djinn it reads `assets/nusmp/djinn/<set>/<piece>.txt` (baked quad meshes) and `textures/entity/djinn/<id>_[u_|d_]armor[_glow].png`
(three looks per Djinn: the shared rig, the unique `u_` look and the dark `d_` look, 108 sets); for Erza `geo/entity/erza_<armour>_arm_<piece>.geo.json` and the two textures of each piece. "Faces" is the number of
polygons the model draws (a Djinn quad; a GeoLite box face that has a uv), so the two are the same thing. "Density" is texels per model pixel: the square root of the uv area over the surface area, per polygon,
area-weighted median. The bar is the minimum over the 36 unique Djinn sets (the sets that carry a Djinn's own look).

<!-- numbers -->

### The Djinn standard, measured

### Djinn sets (min / median / max)

| Look | Sets | Pieces | Faces (quads) | Glow faces | Skin texture | Painted texels | Palette | Glow palette | Density (texels per model px, median) |
|---|---|---|---|---|---|---|---|---|---|
| shared rig | 36 | 20 / 22 / 23 | 3194 / 4325 / 4856 | 398 / 796 / 1132 | 512x512 (mixed) | 168191 / 181521 / 836124 | 112 / 173 / 214 | 17 / 63 / 89 | 3.66 / 3.91 / 9.12 |
| unique look (u_) | 36 | 19 / 24 / 25 | 2856 / 3928 / 4351 | 440 / 888 / 1878 | 1024x1024 (all) | 664603 / 748215 / 849509 | 69 / 167 / 240 | 17 / 62 / 88 | 5.45 / 6.1 / 7.32 |
| dark look (d_) | 36 | 20 / 25 / 26 | 3487 / 4296 / 4756 | 374 / 842 / 1604 | 1024x1024 (all) | 674666 / 750536 / 838854 | 78 / 117 / 163 | 35 / 88 / 114 | 5.18 / 5.74 / 6.68 |


The bar the Erza armours must reach (the minimum over the 36 unique Djinn sets) and that `ErzaArmourAuditTest` and the in-game self-test `skill.erza_requip2` hold them to:

| Measure | Djinn minimum |
|---|---|
| pieces in a set | 19 |
| drawn faces (quads) | 2856 |
| glow faces | 440 |
| painted skin texels | 664603 |
| painted glow texels | 20976 |
| palette (5-bit colours holding 0.1 percent of the paint) | 69 |
| glow palette | 17 |
| texels per model pixel (median) | 5.45 |
| skin atlas side (texels; the whole set shares one atlas) | 1024 |

### The 1.96.0 armours against the bar (before)

| Armour | Pieces | Cubes | Drawn faces | Largest piece texture | Painted texels | Density |
|---|---|---|---|---|---|---|
| adamantine | 11 | 73 | 438 | 256 | 54446 | 3.07 |
| black_wing | 13 | 91 | 546 | 128 | 52630 | 3.07 |
| clear_heart | 7 | 27 | 162 | 128 | 22792 | 3.05 |
| fairy_armor | 14 | 88 | 528 | 128 | 54672 | 3.07 |
| flame_empress | 13 | 97 | 582 | 256 | 46874 | 3.08 |
| flight_armor | 13 | 68 | 408 | 256 | 46670 | 3.07 |
| grand_chariot | 13 | 137 | 822 | 256 | 74074 | 3.11 |
| heart_kreuz | 11 | 66 | 396 | 128 | 42600 | 3.07 |
| heavens_wheel | 13 | 120 | 720 | 256 | 66030 | 3.1 |
| lightning_empress | 14 | 97 | 582 | 256 | 53170 | 3.07 |
| nakagami | 13 | 87 | 522 | 256 | 61054 | 3.07 |
| purgatory | 12 | 112 | 672 | 128 | 44734 | 3.07 |
| sea_empress | 14 | 110 | 660 | 128 | 56476 | 3.07 |
| wind_god | 15 | 100 | 600 | 256 | 58936 | 3.07 |

The old sets had 7 to 15 pieces (bar 19), 162 to 822 faces (bar 2856), pieces textured at 128 or 256 px at 3.0 texels per model pixel (bar 5.45) and 22792 to 74074 painted texels (bar 664603).

### The 1.99.0 armours (after)

| Armour | Pieces | Cubes | Faces | Glow faces | Largest piece texture | Sum of texture texels | Painted texels | Palette (whole set) | Glow palette | Density (median) | Animated pieces |
|---|---|---|---|---|---|---|---|---|---|---|---|
| adamantine | 19 | 2258 | 4791 | 4791 | 512 | 1703936 | 860413 | 94 | 65 | 13.55 | 2 |
| black_wing | 20 | 2344 | 4865 | 4865 | 512 | 2621440 | 1201922 | 159 | 51 | 13.56 | 3 |
| clear_heart | 20 | 1800 | 3562 | 3562 | 512 | 1343488 | 700444 | 152 | 43 | 13.46 | 3 |
| fairy_armor | 21 | 2265 | 5010 | 5010 | 512 | 1966080 | 983010 | 128 | 71 | 14.04 | 4 |
| flame_empress | 20 | 2135 | 5040 | 5040 | 512 | 2228224 | 990458 | 146 | 108 | 14.09 | 3 |
| flight_armor | 21 | 2102 | 4399 | 4399 | 512 | 1720320 | 815422 | 118 | 82 | 13.55 | 4 |
| grand_chariot | 22 | 2555 | 5900 | 5900 | 512 | 3014656 | 1476094 | 140 | 73 | 14.08 | 5 |
| heart_kreuz | 19 | 1943 | 4046 | 4046 | 512 | 1589248 | 747688 | 141 | 77 | 13.54 | 2 |
| heavens_wheel | 22 | 2506 | 5650 | 5650 | 512 | 2752512 | 1365270 | 141 | 68 | 14.03 | 5 |
| lightning_empress | 21 | 2268 | 5159 | 5159 | 512 | 2490368 | 1130824 | 132 | 76 | 13.85 | 4 |
| nakagami | 20 | 2277 | 4987 | 4987 | 512 | 1835008 | 916732 | 118 | 85 | 13.66 | 3 |
| purgatory | 20 | 2213 | 5057 | 5057 | 512 | 1966080 | 982355 | 89 | 72 | 14.04 | 3 |
| sea_empress | 21 | 2267 | 5249 | 5249 | 512 | 2228224 | 1172962 | 152 | 79 | 13.54 | 4 |
| wind_god | 22 | 2422 | 5163 | 5163 | 512 | 3031040 | 1280751 | 146 | 61 | 13.58 | 5 |

The new sets have 19 to 22 pieces, 3562 to 5900 drawn faces, 13.5 to 14.1 texels per model pixel, 700444 to 1476094 painted texels (every one at or above the Djinn minimum).

<!-- /numbers -->

What the Djinn has that the first Erza armours lacked, in one line each: a mesh of 3000 to 4400 smooth polygons a set where the old Erza set had 160 to 820 flat ones; a skin of 1024 x 1024 painted at 5.5 to 7.3
texels per model pixel where the old pieces had 128 or 256 px at 3.1; 19 to 25 pieces (helm with horns and crest, gorget-like belt and mantle, greaves, wings in two articulated segments, tail, halo, backdrop)
where the old sets had 7 to 15; a glow layer of 440 to 1900 polygons with a palette of its own where the old glow maps held 3 to 80 colours on blocks.

## How the new models are built

### Geometry: shells of small panels instead of boxes

An armour plate is not a box: a breastplate curves round the chest, a pauldron is a dome, a greave follows the shin. A GeoLite model can only hold boxes, so a curved plate is built as a **shell**: a grid of thin
boxes (a panel is about 1.2 model pixels across and 0.5 thick) that follow a parametric surface, tilted to its normal, each with ONLY its outer face written (the faces nobody can see are never written, so the
face count is honest). `erza_armour_kit.Piece.shell` takes the surface `S(u, v)`; `gen_erza_armour_core.Loft` builds the surfaces: a tube lofted through stations (height, half width, front depth, back depth,
exponent). The exponent turns an ellipse (2) into a rounded box (4 to 6) so a plate can wrap the player's 8 x 4 body without a corner of the skin poking through: the skin box corner must lie inside the
superellipse `|x/a|^n + |z/b|^n = 1`, which the stations are chosen to satisfy, and panels are spaced evenly along the ARC LENGTH of the section (equal angles are not equal lengths on a flat-fronted section; this
was the first bug: a one panel wide gap down the middle of the chest). Hard parts (spikes, horns, blades, feathers, straps, gems, rivets) are chains of oriented boxes: `Piece.oriented / between`, `core.spike`,
`core.blade`, `core.gem`.

Rotation of a cube in GeoLite (`client/geo/GeoDraw.rotate`, proved here because the whole kit rests on it): the file's frame is y up; the game turns a cube by `rotationZYX(-rz, ry, -rx)` in the model frame (y down).
The y flip `F = diag(1, -1, 1)` conjugates a rotation about x or z to its inverse and leaves y alone, so in the file frame the cube turns by `R = Rz(rz) . Ry(ry) . Rx(rx)`: x first, then y, then z, about the cube's
pivot. `erza_armour_kit.rot_xyz` / `euler_of` implement exactly this and are inverse to each other (checked), and the previews (which run the real `GeoDraw`) agree with the kit's own bounding boxes.

### Texture: one painted sheet per plate, at 14 texels per model pixel

All panels of a plate share ONE painted sheet: each panel's outer face is a window into it, so an emblem, a gilded vine or a row of rivets runs across the panels without a seam and the density is the same
everywhere. On the north face of a box u runs toward +x and v runs down (the box net of the loader: west toward the front, east toward the back, south toward -x), and a shell builds each panel so that its local x
is the viewer's right and its local y the up tangent, so a sheet reads the way it is painted. The thickness faces on the rim of a plate are written and painted on their own (they give the plates their edge).
`erza_armour_paint.py` paints every face procedurally from a height field that is then lit: a normal from the height, a fixed light from the top left (bevel highlights on the lit edges, shadow on the far ones),
ambient occlusion along the edges, wear (worn-bright edges, scratches, patina), a damask lattice and a sweeping sheen on enamel, a running gold vine along the border of every large plate, hammered or brushed
metal, scales, feathers with barbs, woven cloth with folds and pleats, leather with stitching, faceted gems, translucent membranes with ribs for fins and wings; decals draw the emblems (heart, cross, sun, bolt,
wave, feather, rune, wheel, flame, star, shield ...) as raised inlays or engraved glowing marks. The ramp from dark to light goes through the palette of each armour (`gen_erza_armour_sets.py`). The glow map is
painted at the same time, from the same marks (inlay lines, engravings, gems, veins): white-hot in the core, the armour's glow colour at the fringe, so the painter's tint (`Armour.glow`) gives the hue.

### Which pieces, and how they forge

Every armour has the same eighteen pieces and its own signature ones: helm (circlet, crown, winged, horned, crested, visored, ribbon), gorget, chest (bust, breast, heavy, cloth) with its emblem, backplate, belt,
skirt of eight swaying plates, two pauldrons (round, spiked, wing, tiered, fin, cloth), two vambraces, two gauntlets (with fingers and claws), two cuisses, two greaves, two sabatons, and then wings (layered
rows of curved feathers: feather, blade, bolt, fin, fairy petals, jet), halo (blades, runes, flame, coral, petals, ring), tail (flame or ribbon), cape, backdrop (wheel of swords, chariot, spines), ribbon. The
rows of the table below are generated from `ArmourSets.java`. The forging order is by weight (`ArmourSets.weightOf`): belt and gorget first, then hands and feet, legs, helm and skirt, pauldrons, cuirass and
back, wings and halo, the backdrop last (delay 12): a heavier piece never forges before a lighter one, so the armour is built from the underlayers out and the big dramatic pieces arrive last.

| Armour | Look |
|---|---|
| Heart Kreuz | crimson enamel, silver lames, gold vines; circlet with a heart gem and a gold ridge, heart on the breastplate and the limbs, a cross on the back, two ribbons streaming from the head |
| Heaven's Wheel | silver-white and navy with gold; winged helm, a fan of feather-blades on the pauldrons and greaves, wings of blade-feathers in three layers, a halo of swords, a wheel of swords turning behind the back |
| Flame Empress | crimson and gold with glowing ember inlays; crested helm, spiked pauldrons and cuisses, claws, a curled armoured tail ending in flame, a halo of flames |
| Lightning Empress | gold enamel with white bolt inlays, cream plates; winged helm, spiked pauldrons, wings of translucent lightning, a halo of rune plates |
| Sea Empress | overlapping teal scales with pearl plates and silver trim; crown of spikes, fin pauldrons, translucent fin wings with ribs, a coral halo |
| Black Wing | black-violet plate with violet inlays; horned helm, feathered pauldrons, claws and pointed sabatons, wings of black feathers with glowing barbs |
| Clear Heart Clothing | white and pale blue cloth with gold hems and pleats, a headband with a bow, soft sleeve caps, wrapped wrists, a pleated cloth skirt, ribbons streaming behind |
| Nakagami | white plate with gold and cyan rune inlays; visored helm with a brow peak, tiered pauldrons, ring bracers, a cloak and a halo of floating rune plates |
| Grand Chariot | gold and cream; the Heaven's Wheel set at its largest: bigger wheel of swords with a second wheel turning the other way and the yoke between them |
| Adamantine | dark steel-green heavy plate, a second raised plate over the breast with a shield mark, tiered pauldrons, a mail mantle |
| Purgatory | black iron with magma inlays; horned helm, spiked everything, claws, a tattered cape and eight spines of the back |
| Flight Armor | sky blue and white with cyan inlays; visored helm, rigid jet wings with thrusters, a streaming scarf |
| Wind God | pale green and emerald; winged helm, feathered wings of pale green, a ring halo of gems, a ribbon tail |
| Fairy Armor | pink and cream with gold; crown with gems, a bust plate with a heart, translucent fairy wings (four leaves each), a halo of petals |

### Fit on the skin (wide and slim arms)

Solid plates are authored 2 percent or more off the skin everywhere (the skin boxes are in `erza_armour_kit.SKIN`; the generator warns about any face lying within 0.03 pixel of a skin plane, which would
z-fight, and reported none). The arm pieces are drawn by `WaterAuraRig` narrowed to 80 percent about the arm's pivot on a slim (Alex) arm: they are built around the wide arm (centre x = -6, half width
2.4 to 2.7), so the narrowed piece still stands at least 2 percent off the 3 pixel slim arm (`ErzaArmourPiecesTest.theSolidPlatesStandAtLeastTwoPercentOffTheSkin` computes both). The slim front view of every
armour is in its sheet. The inner side of every limb piece (toward the body) is left open: nothing can be seen there and the two legs would otherwise cut into each other.

### Cost

An armour is 3500 to 5900 drawn faces (the Djinn: 2856 to 4856). The glow pass draws the model again (the loader draws the same geometry with the glow texture), so an armour is about twice that in vertices
(32000 to 50000 in the preview's count against the Djinn's 16000 to 19000 in one lit pass plus its glow quads). Pieces flagged `detail` (belt, gorget, gauntlets, skirt, cape, tail, halo, backdrop, ribbon) are
skipped beyond 24 blocks and the whole armour beyond 64 blocks, as before. The textures are 21 MB on disk in all (the Djinn's: 63 MB for 108 sets; the old Erza ones 1.5 MB); the geo files 7.4 MB (compact JSON; the old ones 1.5 MB).

## Checks

* `python3 -B tools/geo_check.py <ids>`: every one of the 288 models has linked bones, uvs inside its texture, its texture and its animation bones.
* JUnit (all run in the shim here): `ErzaArmourAuditTest` (fails if any armour has fewer pieces, drawn faces, glow faces, painted texels, glow texels, palette colours, glow palette colours or a lower density or
  a smaller skin area than the Djinn minimum; keeps the bar file equal to `ArmourQuality.BAR` and not above what the Djinn files really contain), `ErzaArmourPiecesTest` (every piece name resolves a model part, every model has
  its root bone and linked bones, every clip is `idle` and names real bones, a heavier piece never forges before a lighter one, the plates stand off the skin by 2 percent on wide and slim arms), and the
  existing `ArmourTableTest`, `ArmourAssetsTest`, `GeoAssetsTest` (`EquipAnimationTest` and `RequipAnimationTest` need Minecraft classes; the shim cannot compile them, they were not touched and CI runs them).
* In game: the self-test `skill.erza_requip2` (CI server smoke test) measures every armour from the jar with the same class and the same bar and reports the smallest armour and its numbers; `skill.erza_requip` still
  checks that every piece model, texture and glow map is in the jar.
* Type check (the stub javac of the brief) of `ArmourSets`, `ArmourQuality`, `ErzaSelfTests`: no error in these files; a deliberate wrong call in a probe file was reported (negative control).

## Previews

`docs/previews/erza/<armour>_sheet.png` (`python3 -B tools/erza_armour_sheets.py`): per armour a Djinn set (Agares, unique look, full equip, from the Djinn's own preview tool) for scale, the armour from the
front, back, side and on a slim body, and close-ups of head, torso and legs, rendered by `tools/render_preview` (the real `ErzaAura` painter and `GeoDraw`, headless). The scenes are
`tools/render_preview/scenes.d/erza_armour.json`.

## Weapons

The six weapons (`flame_empress_blade`, `lightning_empress_halberd`, `sea_empress_trident`, `black_wing_blade`, `nakagami_edge`, `purgatory_mace`) were already built to the Genesis Demon-Slayer's standard in 1.96.0
(`tools/gen_erza_weapons.py` on `tools/gen_weapon_models.py`): real 3D meshes drawn by `client/WeaponRenderer`, 102 to 270 quads, 512 x 512 skins with a glow layer, an inventory icon of the old sprite, engravings in
`item/WeaponEngravings.java`. Measured against the Demon-Slayer family (`demon_slasher_katana` 164 quads, `demon_destroyer_sword` 140, `demon_dweller_sword` 92) they are at the same level or above it, and the preview
(`python tools/item_preview/preview_weapons.py flame_empress_blade ...`) shows the same kind of surface. They were therefore left as they are (rule 2: nothing replaced that was not asked for); the armour was the request.

## What could NOT be verified

Nothing here was run in the game. What was checked is the geometry and the textures (by the tools above and by the real render classes running headless), not how it looks and feels on a real skin in motion.

* Lighting: the preview applies vanilla's entity light to the quads; the game's shader, the real sky and block light, and the lightmap may make the baked highlights read stronger or weaker.
* The forge and shatter animation was not re-shot with the new pieces (the previews show the finished armour); the delays follow the weight rule, but the look of 20 to 22 pieces forging in 12 ticks is unseen.
* The glow pass is drawn with the back faces culled (vanilla's `eyes` render type): wings and capes are built double sided or facing the back so their glow shows from the side people see them from; seen from the
  front, a single sided wing has no glow.
* Cost: 32000 to 50000 vertices an armour (both passes) is about two to three times the Djinn's; it was not timed. If a server full of armoured players is slow, the pieces flagged `detail` can be widened and
  `CELL` (panel size, 1.2 pixels) raised in `gen_erza_armour_core.py` (4000 faces become 2800 at 1.45).
* Translucent wings (Sea Empress, Lightning Empress, Fairy Armor) are drawn as one translucent batch; overlapping strips are not sorted, the vanilla way, so a pair of crossing strips can blend in the wrong order.
* Slim arms: checked by the numbers and by a preview with the Alex model, not on a real Alex skin.
* The Djinn comparison tile comes from the Djinn's own (different) rasteriser; it shows the level of detail, not an exact like-for-like render.
* The lang fragment `tools/lang/erza_armour_remake.json` (piece names, one-line looks, the self-test label) is not merged into `en_us.json` (not mine to edit) and no Java reads its keys yet; it is there for the wiki and tooltips.
* "Morning Star" and "Knight / Giant" from the request are not in the code or the spec; nothing was added for them.

## Files

| Area | Files |
|---|---|
| Generators | `tools/gen_erza_armour.py` (driver), `gen_erza_armour_core.py`, `gen_erza_armour_parts.py` (helm, gorget, chest, backplate, belt), `gen_erza_armour_limbs.py` (skirt, arms, legs), `gen_erza_armour_extras.py` (wings, halo, tail, cape, backdrop, ribbon), `gen_erza_armour_sets.py` (palettes and styles), `erza_armour_kit.py`, `erza_armour_paint.py` |
| Tools | `tools/erza_armour_audit.py`, `tools/erza_armour_sheets.py`, `tools/gen_erza_armour_doc.py` |
| Java | `requip/ArmourSets.java`, `requip/ArmourQuality.java`, `skill/ErzaSelfTests.java` (`skill.erza_requip2`) |
| Tests | `src/test/java/.../requip/ErzaArmourAuditTest.java`, `ErzaArmourPiecesTest.java`, `src/test/resources/erza_armour_bar.json` |
| Assets | `assets/nusmp/geo/entity/erza_*_arm_*.geo.json`, `animations/entity/erza_*_arm_*.animation.json`, `textures/entity/erza_*_arm_*.png` and `_glow.png` (288 models) |
| Docs and previews | this file, `docs/erza_requip.md` (the pieces tables), `docs/erza_armour_before_1_96.json`, `docs/previews/erza/*.png`, `tools/render_preview/scenes.d/erza_armour.json`, `tools/lang/erza_armour_remake.json` |
