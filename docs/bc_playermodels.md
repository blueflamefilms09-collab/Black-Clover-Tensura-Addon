# Black Clover bosses on the redesigned player model

Owner request: the Black Clover bosses should use the player's model, but better designed, with the quality of the No Index mod's player-model characters (painted anime face, layered cube hair, crown with gem, halo, bracers with tassels, translucent skirt, sparkle).

Setting: **`bossLooks.bcPlayerModel`** in `config/nusmp-playermodel-client.toml`. It is a **CLIENT** config (like every other look option of the kit, because it only changes how a boss is drawn, nothing about the fight). Default **true**. **false restores the previous rendering exactly**, Lucifero's Blender body included. Ids, hitboxes, saves, drops and fights are untouched.

## What is a software render here, and what was not seen

- The sheets in `docs/bc_playermodels_previews/` (`overview.png` and one turnaround sheet per look: front, three-quarter, side, back, with the old look on the left) are **software renders** made by `tools/bc_playermodels/preview_bc.py` on top of the repo's `tools/playermodel` numpy renderer. They are not the game: lighting, mip-mapping, fog, the additive glow level and the sway of the cloth are different, the extras are frozen at an idle frame.
- **Nothing was run in a game and Gradle was not run** (no Gradle in this session; CI compiles). The pure classes (`BcAudit`, `BcLooks`, `PlayerLookTables`, `PlayerLook`) and `BcPlayerModelsTest` were compiled with `javac` and the audit was run on the real resource files (0 problems); the renderer-side edits (`PlayerModelRenderer`, `ExtrasDraw`, `ExtrasLayer`, `PlayerBodies`, `PlayerAssets`) are small and were reviewed by reading only.
- **Models were not made in Blender** (rule 14): Blender is not installed here. The parts are cubes and single cards built by a generator (`tools/bc_playermodels/`), like the rest of the player-model kit's extras, with painted atlases. Colour and shape look-dev would still gain from a real modelling pass.

## Sources (rule 10)

English Black Clover Fandom wiki first (fetched with curl, treated as untrusted data): the pages of Lucius Zogratis, Dante Zogratis, Vanica Zogratis, Zenon Zogratis, Zagred, Lucifero and Megicula. They gave the canon looks used below. The Japanese wiki was not needed. Not fetched at all: any image of the characters (the references are the owner's screenshots of the No Index characters for quality, and the wiki text for the canon). Zagred's "legacy" look and the clone looks have no canon page of their own (they reuse Zagred and Lucius).

## What was built

| Layer | What |
|---|---|
| Skin (256x256, the kit's 64x64 layout at 4x4 texels per pixel) | New face painted on the head (large two-tone iris with highlight, lashes, brows, mouth, nose, blush) over the boss's earlier HD skin; the kit's hat layer is cleared so the cube hair sits on a clean head; per-character marks (Lucius' sun mark, Dante's eye band and moustache, Vanica's eyepatch and tear, Zenon's cross, Zagred's face lines) |
| Hair | Cube hair of the extras model: a shell over the scalp, stacked tiers, spike cards, fringe, side locks, long or bob hair with sway. The look has **no kit hair** (`BcAudit` fails a look that has one) |
| Head and body parts | Crowns (band plus gold spike cards plus gems), halo, horns, bone helmet, collar, mantle with fringe, cape, bracers with tassels, translucent skirt, belts, pauldrons, ribcage, bat wings (shaped cards), tails (heart, arrow or trident tip) |
| Glow | Only the `_fx` model has the glow flag (gems, halo ring, tassel tips); the other models draw no second additive pass |
| Shimmer | `BcShimmer`: one particle every 4 ticks per boss, full detail distance only, through the mod's `Fx` facade and the theme `boss.<name>` |

Three models per look (generator output `pmx_bc_<id>`, `_fine`, `_fx`): `main` (always), `_fine` (small detail, **skipped beyond the detail distance**, 24 blocks by default, by `ExtrasDraw`) and `_fx` (glow parts).

## Per boss

All counts are the NEW parts (`tools/bc_playermodels/stats.json`); the old extras kept for the non-devil forms are drawn as before and are not in the numbers.

| Look (form) | Canon palette | Cubes (main / fine / fx) | Quads (main / fine / fx) | Quads at full detail |
|---|---|---|---|---|
| Lucius Zogratis | pale skin, black spiked hair, red eyes with sun mark, dark robe, white mantle with fringe, black coat | 25 / 27 / 0 | 70 / 27 / 0 | 97 |
| Lucius Zogratis, divine form | white-gold: white long hair, gold five-spike crown with blue gem, glowing halo, white-blue translucent skirt, gold bracers with tassels, golden eyes | 57 / 77 / 10 | 182 / 97 / 32 | 311 |
| Dante Zogratis | tan skin, black pompadour and long hair, green eyes with the eye band and moustache, gold-and-red crown, navy cape, white fur mantle | 41 / 30 / 5 | 116 / 30 / 10 | 156 |
| Dante, devil form | ash skin, slit amber eyes with dark sclera, violet horns and wings, black cape, spiked pauldrons | 71 / 18 / 5 | 236 / 18 / 10 | 264 |
| Vanica Zogratis | black hair bob, brown eyes with the eyepatch, gold-and-red crown, short cape, red sash | 41 / 36 / 5 | 126 / 36 / 10 | 172 |
| Vanica, devil form | pink-magenta: slit pink eye, eyepatch, light fringe streak, curled horns, bat wings, heart-tipped tail | 65 / 39 / 5 | 240 / 39 / 10 | 289 |
| Zenon Zogratis | black spiked hair, brown eyes, grey fur mantle, navy cape with the spade emblem | 36 / 27 / 0 | 86 / 27 / 0 | 113 |
| Zenon, devil form | ash skin, red slit eyes, bone helmet half, ribcage, bone pauldrons, grey horns, wings, trident-tipped tail | 100 / 18 / 0 | 390 / 18 / 0 | 408 |
| Zagred | pale grey skin, magenta slit eyes, dark teeth, black spiked hair with long spikes, horns, spiked pauldrons, collar, arrow-tipped tail | 61 / 7 / 0 | 211 / 7 / 0 | 218 |
| Zagred, second form | the same with larger wings, more horns, longer tail and violet glowing face lines | 79 / 7 / 0 | 279 / 7 / 0 | 286 |
| Zagred legacy and legacy form 2 | the same two looks as the Zagred pair above (the legacy entity keeps its own ids) | 61 / 7 / 0 and 79 / 7 / 0 | 211 / 7 / 0 and 279 / 7 / 0 | 218 and 286 |
| Lucifero | white long hair in three tiers, magenta eyes, black-and-violet dress and mesh, big curved horns, bat wings (devil look) | 47 / 77 / 0 | 237 / 77 / 0 | 314 |
| Lucifero, second form | the same with a glowing core gem | 67 / 77 / 1 | 297 / 77 / 2 | 376 |

The clones of Lucius use `Clone.look`, which now asks the same option, so they follow Lucius' look.

The old rendering of each (kept, behind the setting): the kit skin of the same name with a kit hair style (`anime_spike_tall`, `pompadour`, `bob`, `anime_short`, `mohawk`, `spiky_long`, `anime_long_open`) and the earlier crude extras (cloak, grimoire, wings; Vanica and Zenon had none in their normal form). Lucifero's was the Blender body of wave 3.

## Worst-case cost (rule 11)

- **Client draw:** the largest look is Zenon's devil form at 408 quads (about 118 cubes) and Lucifero's second form at 376, at full detail. Beyond the detail distance (24 blocks by default, `PlayerLod`) the `_fine` layer is dropped (at most 97 quads fewer), the second skin layer and the kit hair are dropped by the existing LOD, and the glow pass is dropped beyond the far distance (64 blocks). Glowing quads are at most **32 per boss**. Atlases are at most 256x256 per model (the audit caps them at 512). The audit fails a look over 700 quads or 120 glowing quads.
- **Per-frame allocation:** none new. `BcLooks.of` is a map lookup; `BcShimmer` keeps a 64-int array and a map of the theme keys built once.
- **Particles:** at most **5 particles per second per boss**, only at full detail, none for a dead or invisible boss, through `Fx.send`, so the facade's own budgets and quality level cut it further. For a typical fight of one boss and three clones that is at most 20 per second before the budgets.
- **Network and server:** no packet, no entity, no server work, no tick work. Everything is client rendering; the shimmer is client side only (`isClientSide` guard).
- **Memory:** 14 skins of 256x256 (with glow maps) and 34 small geo files; each is loaded once per resource reload.

## Server and multiplayer safety (rule 9)

New Java is in `client/playermodel/`; `BcLooks`, `BcAudit` use no game class and the unit test reads them; `BcShimmer` is called only from the client renderer; nothing in common code was touched. The setting is a client config. CI self-test `bcmodel.looks` (prefix `bcmodel` in the smoke list, before `dboss`) runs `BcAudit` on the jar.

## NEW / REPLACED / KEPT (rule 5)

- **NEW:** 14 looks `bc_<old id>` in `playermodel/looks_bc.json`, their skins, atlases, geo and animation files, the parts library in `tools/bc_playermodels/`, `BcLooks`, `BcShimmer`, `BcAudit`, the setting `bossLooks.bcPlayerModel`, `BcPlayerModelsTest`, self-test `bcmodel.looks`, the preview tool and sheets, the lang fragment `tools/lang/bc_playermodels.json`.
- **REPLACED (only while the setting is on, default):** how Lucius, Dante, Vanica, Zenon, Zagred, Zagred legacy and Lucifero (both forms) and the clones of Lucius are drawn. For Lucifero this replaces the Blender body while on.
- **KEPT:** every old look and skin in `looks.json`, the old extras for the non-devil forms (they are still part of the new look), Lucifero's Blender body (setting off), the Time Magic VFX (untouched), ids, hitboxes, saves and drops.

## Not done / honest limits

- Not reached: the Dark Disciples and variants, the Devil Avatar, God of Order, the Midnight Sun elves (Fana, Rades, Rhya, Patolli), the early-arc bosses (Heath Grice, Lotus, Mars, Revchi, Vetto, Morris) and the Black Clover NPCs. Udaeus is DanMachi and out of scope. They keep their current look.
- The skins are the earlier HD skins with a new face painted and the hat layer cleared, not full repaints; the face is 32x32 texels (the layout's limit), so it is less detailed than a hand-painted face on a larger sheet.
- The wings are flat shaped cards. The devil forms drop the earlier crude extras, including the floating grimoire props.
- Zagred has no glowing parts (only its face lines glow in the skin's glow map).
- The shimmer uses the mod's own theme for each boss; its look in a game has not been seen.
- The smoke-list prefix sits before `dboss` (not after) because an existing test asserts `,dboss,scaling,` directly; the tail stays exactly as the other tests expect.
- Colours and proportions were judged from software renders and the wiki text only; a game pass on the owner's side may want tweaks (run `python3 -B tools/bc_playermodels/gen_bc.py` then `python3 -B tools/bc_playermodels/preview_bc.py --old` after changing `bcchars.py`).
