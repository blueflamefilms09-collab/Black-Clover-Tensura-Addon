# Hero katanas: true katanas (white tiger, soul eater, siderite, sakura stone, wind king, Hellblaze)

Status: meshes and skins rebuilt, not yet released (no version bump, no changelog here: the lead does that). Nothing below was seen in a game.

## What was wrong

4.61.0 only thinned the blades. The outline was still a fantasy sword: wings, ribs, petals, a crown and a star guard as wide as the blade is long (see the BEFORE row of
`docs/previews/hero_katanas_v2/all_hero_katanas.png`). The measured numbers of the 4.61.0 meshes against the new ones are in the "Measured" table below.

## What it is now

Each form is rebuilt as a katana first and the lore is put ON the katana silhouette, never round it. Common to all six:

- blade: single edge, gentle sori (about 2 percent of the length), shinogi ridge, mune, yokote and kissaki; a baked hamon band (the temper line) and a line of light in the groove
- width of the blade about 1/30 of the whole length (measured below); thickness 0.28 to 0.30 of the width
- tsuba: a real thin plate, at most about 1/14 of the length across, cut with sukashi windows in the shape of the lore (windows are real holes: the tsuba is built from sector plates), seppa washers either side
- habaki, fuchi, tsuka with a diamond ito lattice over ray skin, kashira with a small crest, tassel (sageo) at the kashira, two menuki on the flats of the grip
- the big lore shapes are gone from the outline and live in the blade (inlays / engravings baked into the skin: zero quads in game), the menuki, the kashira crest and the tassel

| katana | lore on the katana |
|---|---|
| white tiger | black tiger stripes engraved across the hira (11 per side, thick at the spine, thinning to the edge), a second glowing line beside the groove (the Double Sword), claw-slit sukashi tsuba, gold tiger-paw menuki on each flat, a gem in a setting on the kashira, a striped tail with a fur tuft as the tassel |
| soul eater | gunome hamon that glows soul-green, rib arcs engraved on the hira, rib-cage sukashi tsuba, a skull menuki with glowing eye sockets on each flat, a gem on the kashira, soul wisps as the tassel (short and narrow) |
| siderite | meteor-iron cracked skin with four-point star flecks inlaid, suguha hamon, an eight-point star sukashi tsuba, a thin orbit ring above the tsuba (radius 0.86 of the tsuba, so it does not widen the guard), a four-point star menuki (silver plate under a glowing one) on each flat, a star plate on the kashira, a cord of glowing beads as the tassel |
| sakura stone | rose-stone blade with a petal-inlay line and petal flecks, notare hamon, a five-petal sukashi tsuba (the petals are the cut-outs), a five-petal blossom menuki on each flat, a blossom crest on the kashira, a cord with three loose petals as the tassel |
| wind king | long wind-green blade (kept at its 1.7 block class), notare hamon with swirl engravings, a four-feather sukashi tsuba, two small crossed feathers as menuki, a gem in a setting on the kashira, a cord with feathers as the tassel |
| Hellblaze | black steel, dark-red midare hamon with a glowing edge, three jagged sin veins climbing from the habaki, a bo-hi with a line of light, a ring of eight thorns as the tsuba (diameter 7.2, windows between the thorns), black ito with a dark-red thread, sin-eye menuki and an eye on the kashira, a sageo; plus a true saya: lacquered black, follows the blade's sori at 1.2 times its width, koiguchi, kojiri, kurikata, flame veins, sageo; and the `_lod` copy |

## New, replaced, kept (rule 5)

REPLACED (same ids, the old versions are kept under classic_thin, rule 2):

- `weapon_meshes/{vassal_katana__white_tiger_katana, vassal_katana__soul_eater_katana, vassal_katana__siderite_katana, vassal_katana__sakura_stone_katana, legendary_sword__wind_king_katana}.txt` and their `textures/entity/weapon/<id>.png` + `_glow.png`
- `weapon_meshes/up/hellblaze_the_demon_sin__{katana, katana_lod, saya}.txt` and `textures/entity/weapon/up/hellblaze_the_demon_sin__{katana, katana_lod, saya}.png`, `_glow.png`, and `hellblaze_the_demon_sin__katana_veins.png`

NEW:

- `weapon_meshes/classic_thin/`, `textures/entity/weapon/classic_thin/` (the five forms of 4.61.0), `weapon_meshes/up/classic_thin/`, `textures/entity/weapon/up/classic_thin/` (the Hellblaze katana, lod, saya and veins of 4.61.0): archive copies, byte for byte
- `tools/blender/hero_katanas.py` (the five forms; `--keep` makes the classic_thin copies), `tools/blender/hero_hellblaze.py` (katana, lod, saya), `tools/blender/hero_katana_sheet.py` (the before/after stills)
- `src/test/java/com/newuniverse/nusmp/visual/sizes/HeroKatanasTest.java`
- `docs/previews/hero_katanas_v2/`

KEPT, not touched: all ids, the place line (origin, angle, and the scale of the five forms), the glow header (tint, strength) of every mesh, `orient manual`, the grip line (re-measured by `grip_table.py`), the Better Combat files, `WeaponEngravings.java`, the Java classes (no Java main-source change at all), `classic_vassal` (the files before 4.61.0), `katanas.py`, `katanas_specs.py`, `katanas_classic.py`, `hellblaze_katana.py` and the other builders, the classic broadsword look of Hellblaze.

The `vassalForms.classic` ("classicVassalForms") client setting still restores an old look: it still draws `classic_vassal` (the pre-4.61.0 fat form) for the five forms, exactly as before; `WeaponRenderer` was not touched. The new `classic_thin` copies (the 4.61.0 look) are archive only: nothing selects them. The renderer therefore never mixes the new rest mesh with variants of the old design. Making `classic_thin` selectable would be a small renderer change plus a setting; it was not asked for.

## Measured (katana_measure.py, pure python; `mid` = 45-70 percent band, `outline` = widest slice of the whole mesh, layers 0 and 2)

| katana | blade w/L before (4.61.0) | blade w/L now | t/w now | habaki w / blade length now | outline / L before | outline / L now |
|---|---|---|---|---|---|---|
| white tiger | 1/11.9 | 1/32.2 | 0.28 | 1/15.8 | 1/3.4 | 1/15.5 |
| soul eater | 1/8.6 | 1/31.3 | 0.30 | 1/16.7 | 1/5.3 | 1/14.4 |
| siderite | 1/22.9 | 1/29.9 | 0.30 | 1/17.2 | 1/3.9 | 1/14.7 |
| sakura stone | 1/8.6 | 1/32.2 | 0.28 | 1/15.6 | 1/4.6 | 1/15.1 |
| wind king | 1/8.7 | 1/30.5 | 0.28 | 1/18.3 | 1/5.0 | 1/16.4 |
| Hellblaze | 1/28.0 | 1/28.4 | 0.28 | 1/16.6 | 1/10.0 | 1/13.8 |

Target was blade <= 1/25 (all six pass, 1/28 to 1/32) and the outline at the guard about 1/14 (five pass; Hellblaze is 1/13.8 after its tsuba was cut from diameter 9.0 to 7.2). The mid-blade of the 4.61.0 meshes reads 1/8.6 to 1/11.9 for four forms because the wings, ribs and petals lie in the 45-70 percent band; that is the number the size rule would see, which is why those forms looked fat. Sori now 0.018-0.022 of the blade length (a real katana is 0.01-0.04); taper (width at 85 percent / width at the habaki) 0.71-0.81.

Whole mesh lengths (mesh units / cm): white tiger 134.5, soul eater 124.2, siderite 125.4, sakura stone 128.2, wind king 168.4, Hellblaze 100.7. The place line scale of the five forms is the 4.61.0 one, so the length in hand moves a little with the mesh (soul eater 135.0 to 124.2 is the largest change, -8 percent); Hellblaze stays at 1.72 blocks (pinned by `HellblazeKatanaAssetsTest`, checked below).

## Budgets and worst case (rules 9 and 11)

Client drawing only. No packet, no server state, no Java change: the files are read once per resource reload by the existing `WeaponRenderer` / `WeaponUpgrade` code, exactly as the 4.61.0 files were.

| mesh | body quads | glow quads (layer 2) | quad lines | 4.61.0 quad lines | skin + glow PNG |
|---|---|---|---|---|---|
| white tiger | 3247 | 289 | 3536 | 4614 | 278 KB (<= 350) |
| soul eater | 3154 | 399 | 3553 | 4544 | 312 KB |
| siderite | 3068 | 526 | 3594 | 4064 | 332 KB |
| sakura stone | 2905 | 370 | 3275 | 4538 | 299 KB |
| wind king | 3201 | 528 | 3729 | 4521 | 308 KB |
| Hellblaze katana | 1052 | 269 | 1321 | (<= 1800 pinned) | 1024 px, 584 + 61 KB on disk (4.61.0: 572 + 104 KB) |
| Hellblaze lod | 122 | 60 | 182 | (<= 320 pinned) | 256 px, 70 + 13 KB |
| Hellblaze saya | 365 | 38 | 403 | (<= 600 pinned) | 1024 px |

Worst case per frame: one hero katana drawn per player that holds one, at most 3729 quad lines (wind king), which the existing perf budgets (`WeaponRenderer` distance culling, the jar's `MeshCap`) already bound; every mesh is smaller than the 4.61.0 one it replaces (-8 to -28 percent). The jar task's `MeshCap` default cap is 3000 quads: the five forms are 3275 to 3729 lines, so the jar cuts at most the smallest 18 percent of the quads of a form (rivets, ito lattice cells; never the extremes, void or halo), the same mechanism that already cut the 4.5k-line 4.61.0 meshes by a third. Hellblaze is far below. The painted details (stripes, inlays, hamon, ito helices on Hellblaze, star flecks) are bake-only: zero quads and zero extra draw calls. Draw calls are unchanged (one mesh, one skin, one glow pass, plus the Hellblaze saya and lod as before).

## How they were made (honest)

All six are scripted `bpy` builds: headless Blender 5.3.0, the existing pipeline (`tools/blender/forms_kit.py`, `forms_vassal_lib.py`, `forms_vassal_common.py`, `bl_lib.py`, the katana core `katanas.py`), Cycles bake of the skin, then the repo's palette pack to fit 350 KB. No Blender window was opened and no Blockbench was used; these are models made by script in Blender, not hand-modelled in the app (rule 14/15 fallback, to be reported as such). The stills below are drawn by the repo's software rasteriser (`tools/item_preview/preview_weapons.py`), not by Cycles and not by the game.

Build commands (idempotent; `--keep` first):

    python3 -B tools/blender/hero_katanas.py --keep
    python3 -B tools/blender/hero_katanas.py            # the five forms, bake 2048, 24 samples (a few minutes per form on 3 threads)
    python3 -B tools/blender/hero_hellblaze.py          # katana, lod, saya 
    python3 -B tools/heroweapon/register_form_meshes.py
    python3 -B tools/weapon_grip/grip_table.py --write
    python3 -B tools/weapon_grip/hold_audit.py --write
    python3 -B tools/bettercombat/inventory.py
    python3 -B tools/blender/hero_katana_sheet.py sheet ; python3 -B tools/blender/hero_katana_sheet.py close <id>

Preview: `docs/previews/hero_katanas_v2/all_hero_katanas.png` (BEFORE row = 4.61.0 above, AFTER row = now, same camera and scale) and `close_<id>.png` per katana (whole weapon side, hilt and guard side, hilt and guard edge-on, blade side; BEFORE left, AFTER right), including the Hellblaze saya.

## Tests (run here with javac + the JUnit console launcher; no Gradle)

New `HeroKatanasTest` (7 tests, all pass): blade w/L between 1/60 and 1/25 and t/w <= 0.45 for all six; widest slice of the whole outline <= L/13; the 4.61.0 meshes are the negative control (5 of 6 fail those limits); every classic_thin and classic_vassal copy exists and the new mesh differs; place origin and angle, the scale of the five forms, the glow header, `orient manual` and the grip line equal the 4.61.0 ones; the lore parts (menuki, tassel, kashira gem, orbit ring) are still in the mesh; budgets (5000 lines, Hellblaze 1800, 400 body quads minimum) and skin sizes (350 KB; Hellblaze not larger than the 4.61.0 skins).

Run and passed: `HeroKatanasTest` (7), `ThinKatanasTest` (6), `ClassicVassalFilesTest` (3), `FormMeshFilesTest` (3), `WeaponSizesTest` (8), `WeaponAttributesTest` (7), `GripTableTest` + `HoldAuditTest` + `GripMathTest` (20), `SwingSourceScanTest` (6).

Not runnable here (they need the Minecraft classes, which only Gradle builds): `HellblazeKatanaAssetsTest` and `MeshCapTest` (the latter only tests the cap on synthetic text). The data checks of `HellblazeKatanaAssetsTest` were replayed by a script on the real files and hold: 1321 / 182 / 403 quads against 1800 / 320 / 600, body >= 400 and glow >= 20, the place line starts `place -0.10872 -0.13203 44.750`, `orient manual` and a grip line present, in-hand length 1.72 blocks (1.71996 and 1.71999), skins 1024 / 256 px, the four glow and vein masks pure grey. The class-wide Gradle test sweep, `ConfigFileNamesTest`, `ServerSafetyTest`, the source-scan tests that need Minecraft classes and the CI server smoke test were not run.

Sync tools run: `register_form_meshes.py` (0 added, 90 already listed), `grip_table.py --write` (rewrote `tools/weapon_grip/grip_golden.json`), `hold_audit.py --write` (145 audited, 2 flagged: `vassal_book__library_tome` and `vassal_carriage__queens_chariot`, the known NONE notes; no katana is flagged, so no HOLD_FIXES entry was needed; `hold_golden.json` rewritten), `bettercombat/inventory.py` (manifest unchanged).

## Caveats

- Nothing was seen in a game: no in-hand, first-person or Better Combat swing check, no Hellblaze saya in the off hand. Check the grips and the five forms' hand spot first.
- The stills are a software rasteriser: no Cycles lighting, no glow bloom, no perf measurement. Colours in game follow the `glow` line tint, as before.
- The tsuba is deliberately small (at most 1/14 of the length), so the lore reads mainly from the blade inlays, the menuki, the kashira crest and the tassel; at arm's length the sukashi windows are small. A bigger tsuba would be a one-number change per spec (`tsuba=("claw", 4.5)` etc.) and a rebuild, but would break the 1/14 outline check.
- The five forms are above the jar's 3000-quad default cap (3275 to 3729 lines), so the shipped jar draws up to about 18 percent fewer small quads than the source file; the extremes never change.
- Hellblaze's skin PNG on disk (584 KB) is bigger than the 350 KB class of the other five: it is the 1024 px skin pinned by its own tests and the old one was 572 KB; the build step that makes the veins and grey masks re-saves it after the palette pack.
- `classic_thin` is not selectable in game (see above).

## Decisions for the owner

1. Should the 4.61.0 look (`classic_thin`) be selectable (a setting next to `vassalForms.classic`), or stay an archive?
2. Is a 1/14 tsuba enough lore on the hilt, or should one or two forms get a bigger guard (a deliberate exception, and a looser `HeroKatanasTest` limit)?
3. Raise the jar's `meshQuadCap` for these five, or accept the cap trimming the smallest quads?
