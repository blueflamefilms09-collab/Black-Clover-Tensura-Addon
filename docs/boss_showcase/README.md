# Boss showcase: Goku and Sinbad (looks, forms, bars, arts)

Everything here was made BEFORE anything is seen in the game, as the owner asked. Read this first, then `docs/boss_redesign_goku_sinbad.md` for the rules and the cost.

## Honest labels (read these)

* **Looks, djinn armours and bars** are real renders of the real assets: the model and textures that ship in the jar (`pmx_gs_*` geo and textures, `looks_gs.json`, the boss bar frames) drawn by the repo's own software renderer
  (`tools/bc_playermodels/preview_gs.py`, `tools/boss_bar_preview.py`). This machine has no Blender and no game, so the renders are software renders, not in-game screenshots.
* **Art stills** (`art_*.png`) are RECIPE PREVIEWS: top-down plans drawn by `tools/bc_playermodels/preview_arts.py` from the numbers read out of the Java source and a port of `arc/BossShapes.java`. They show where each
  telegraph is drawn, where it hurts and where it is safe. They do NOT show the particle look; that has to be judged in the game.

## 1. The looks (new player-model style, 16 looks)

| File | What it shows |
|---|---|
| `overview.png` | All 16 looks side by side (Goku's eight forms in the top rows, Sinbad's base and seven djinn below). |
| `gs_goku_base.png` | Goku, Base: orange gi with the blue undershirt and wristbands, black spiked hair, dark eyes. |
| `gs_goku_ssj.png` | Super Saiyan: golden upright hair with pale tips, teal eyes, a soft glow layer. |
| `gs_goku_ssj2.png` | Super Saiyan 2: taller hair with one more spike and shorter bangs, teal eyes, yellow spark lines in the glow. |
| `gs_goku_ssj3.png` | Super Saiyan 3: the long mane down the back to the knees, no eyebrows (brow ridge marks instead). |
| `gs_goku_god.png` | Super Saiyan God: crimson hair and crimson glowing eyes. |
| `gs_goku_blue.png` | Super Saiyan Blue: cyan hair, blue eyes, pale blue spark lines. |
| `gs_goku_ui_sign.png` | Ultra Instinct sign: silver hair over black roots, silver eyes, the grey-white Ultra Instinct gi. |
| `gs_goku_mui.png` | Perfected Ultra Instinct: white-silver hair, white glowing eyes, white spark lines and the extra aura mesh (`pmx_gs_goku_mui_fx`). |
| `gs_sinbad_base.png` | Sinbad without a djinn (reference image 12): tan skin, golden eyes, very long loose purple hair, white turban with a red brow gem and a gold feather ornament, silver-and-white collar, white robe with a navy hem and a navy front sweep, gold arm band, silver wrist cuffs, gold wing sandals. |
| `gs_sinbad_baal.png` (ref 13), `gs_sinbad_furfur.png` (ref 14), `gs_sinbad_zepar.png` (ref 15), `gs_sinbad_valefor.png`, `gs_sinbad_focalor.png`, `gs_sinbad_vepar.png`, `gs_sinbad_crocell.png` | The body of each Djinn Equip in the design of the owner's Sinbad references: long loose hair in the djinn's colour, a gold crown or diadem (antlers for Baal), a wide gold collar and chains, feather / scale / crystal shoulders, feather cuffs, a belt with a round buckle, a long skirt with a jagged sweep, wing sandals and a ring of light behind him. The four without a reference use the same design language in their own table colour. |

## 1b. Comparison sheets: the owner's reference next to every render

Made with `python3 tools/bc_playermodels/compare_gs.py`; the five reference images are copied to `reference/`. Left is the reference, right is the software render of the look (front, three-quarter, side, back).

| File | Reference | Look |
|---|---|---|
| `compare_goku_all.png` | image 16 (Blockbench tutorial Goku) | all eight Goku looks, one row each |
| `compare_goku_base.png`, `_ssj`, `_ssj2`, `_ssj3`, `_god`, `_blue`, `_ui_sign`, `_mui` | image 16 | one look each |
| `compare_sinbad_all.png` | images 12 to 15 | all eight Sinbad looks, one row each |
| `compare_sinbad_base.png` | image 12 | the king |
| `compare_sinbad_baal.png` | image 13 | Baal |
| `compare_sinbad_furfur.png` | image 14 | Furfur |
| `compare_sinbad_zepar.png` | image 15 | Zepar |
| `compare_sinbad_valefor.png`, `_focalor`, `_vepar`, `_crocell` | none sent | built in the same design language from the lore and the Djinn table colours |

The image-to-djinn choice (13 = Baal, 14 = Furfur, 15 = Zepar) is an assumption made from the colours and the lore, see the doc.

## 2. The Djinn Equip switch (what was broken: Sinbad never changed form)

| File | What it shows |
|---|---|
| `sinbad_equips_all.png` | The seven armours in one sheet. |
| `sinbad_equip_baal.png` ... `sinbad_equip_furfur.png` (7 files) | One djinn each, on the mannequin of the player's Djinn Equip: half equip, the full armour in three quarter view, and the back. These use the SAME armour meshes the player's `DjinnArmorLayer` draws, so Sinbad now looks like a player who uses Djinn Equip. |

The cause of the bug and the fix are in the doc (the equip state lived on the server only; it is now a synced number that the client reads, and the client plays the player's equip timeline).

## 3. The boss bars

| File | What it shows |
|---|---|
| `bar_goku_dragon_ball.png` | Goku's own bar, four states (full, 58% with a lair warning, 17%, invulnerable): dark brown frame with orange-gold trim, seven Dragon Balls (one to seven stars) along the lower edge, the four-star ball as the single emblem, a smooth orange fill, six phase notches (his ten forms are grouped into seven phases), the form name on the bar. Mock-up by the existing bar previewer from the real generated frame; the animation (shimmer scroll, entrance, death) is not visible in a still. |
| `bar_sinbad_magi_djinn.png` | Sinbad's own bar, the same four states: navy frame with gold trim, teal fill, seven Metal Vessel gems in the djinn colours on a thin gold chain, the seven-point vessel star as the single emblem, and "Djinn Equip: <djinn>" as the phase name while he wears one. |

## 4. The new arts (recipe previews, see the label above)

| File | What it shows |
|---|---|
| `arts_overview.png` | The five arts in one sheet. |
| `art_goku_zanzoken_cage.png` | Zanzoken Cage: four afterimage pillars grow round the target during the 40 tick wind-up, he blinks through all four (hit r 2.8 each), then slams the middle (r 4.2). Counterplay: leave the cage before the first blink. |
| `art_goku_solar_flare.png` | Solar Flare: three warning rings, then a flash up to 28 blocks that blinds only those who face him. Counterplay: look away. |
| `art_goku_ki_disc.png` | Kienzan Boomerang: a disc out 22 blocks in 14 ticks, eased, and back on the same line (hit r 1.9). Counterplay: step off the line. |
| `art_sinbad_rukh_orrery.png` | Rukh Orrery: seven djinn lights orbit him, then land one by one, 4 ticks apart, on seven spots locked round the target at the start (spread r 3.2). Counterplay: keep moving. |
| `art_sinbad_kingdom_sigil.png` | Kingdom Sigil: a circle of radius 11 with six sectors, every second one lit; the lit ones erupt, the dark ones are safe. Which half is lit and the turn of the circle are locked at the start. |

## How to rebuild these images

```
python3 tools/bc_playermodels/gen_gs.py          # textures, geo and animation files for the 16 looks
python3 tools/bc_playermodels/preview_gs.py      # the look renders
python3 tools/bc_playermodels/preview_arts.py    # the art stills (reads the constants from the Java source)
python3 tools/boss_bar_preview.py --style dragon_ball
python3 tools/boss_bar_preview.py --style magi_djinn
```

## What this does not show

No in-game screenshot or video exists yet. The particle look of the arts, the player-model animations in motion and the bar animation can only be judged in the game; they are listed as caveats in the doc.
