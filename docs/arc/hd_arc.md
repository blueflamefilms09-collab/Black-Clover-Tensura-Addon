# HD arc characters: the Seven Deadly Sins and Black Clover kit skins at 256x256 (3.38)

Owner's three answers this builds on: hair sheets go HD too: **NO** (they stay 64x64); the Seven Deadly Sins and Black Clover arc NPCs get the same treatment as the Tier List bosses: **YES (this
change)**; the new bosses follow after that: **YES (next, not part of this change)**.

Scope: the 56 base ids of `tools/arc_hd/roster.py` (7 Seven Deadly Sins, 49 Black Clover) which are **81 look names** with their second forms. Not touched: DanMachi, the generic and hub NPCs, the
merchants, the other franchises, the seven Black Clover NPCs the kit also draws but that are not in the owner's list (`BLACK_CLOVER_NOT_LISTED`: Charlotte, Kahono, Kiato, Dorothy, Jack the Ripper,
Grey, the Black Market witch; the same command does them when the owner says so).

## 1. What changed, in one paragraph

Every look of the roster now has a **256x256 skin** (and a 256x256 glow map where it had one) under the **same file name** as before. The UV layout is the vanilla 64x64 one, so every model pixel is a 4x4 block
of texels; the model, the animations, the packets and the look table keys are unchanged (the game samples a bigger PNG; `SkinPath.validSkinSize` accepts a square power-of-two multiple of 64 up to 1024 and
`PlayerModelAudit` requires the glow map to be as big as its skin). The hair of the 3D hair system (`anime_*` styles, `docs/playermodel.md`) was chosen per character (`tools/arc_hd/hair_map.py`, a reason for
every id); the skin only carries the scalp under it. **The hair sheets themselves stay 64x64.** One character, Meliodas, is painted by hand as the pilot of the quality bar; the other 80 names are the
**baseline**: the 64x64 generator's result put through the generic HD bridge (smooth upscale, cloth gradients, outlines, metal bevel, the second layer's shadow) with materials named from each sheet.

## 2. How it works

```
tools/arc/char_*.py (the art-kit specs)  ->  tools/playermodel/gen_skins.py paint_character()  = the 64x64 blueprint (unchanged, still the source of truth for colours and layout)
        |                                          |
        |   tools/arc_hd/chars/<name>.py           |   tools/arc_hd/arc_hdlib.py  bridge()  = the baseline (HD bridge of tools/tier_bosses/hd, materials from the sheet)
        +-- hand-painted module (optional) --------+
                                  |
                  tools/arc_hd/gen_arc_hd.py  ->  skins/<name>.png (+ _glow.png)  and  tools/arc_hd/looks/<name>.json (the look entry)
                                  |
                  tools/arc_hd/merge_looks.py ->  assets/nusmp/playermodel/looks.json (entries replaced, every other byte untouched)
```

* `tools/arc_hd/roster.py`: the ids. `hair_map.py`: the kit hair of each look and why. `arc_hdlib.py`: the one module a character painter imports (`blueprint`, `bridge`, `baseline`, `lay`, `materials_for`).
* **Ownership (so the two generators never undo each other).** A look with a fragment in `tools/arc_hd/looks/` belongs to this pass. `tools/playermodel/gen_skins.py` skips those names (their PNGs, glow maps
  and look entries) unless run with `--force-64`; `merge_looks.py` writes exactly those entries into `looks.json`. `looks.json` is read and written byte-compatibly (`json.dump(indent=1)`, no final newline); the
  merger refuses to go on when a round trip would change a byte, and `--check` proves `looks.json` equals the merge of the fragments.
* **The 64 mode is kept (rule 2).** `gen_arc_hd.py --size 64` writes the OLD 64x64 skin with its classic hair, byte for byte what `gen_skins.py` makes (checked on all 81 names: 0 differences in skins, glow
  maps and look entries). `--size 128` averages the 256 result down by an exact 2x2 average. To bring an id back to 64: `gen_arc_hd.py <id> --size 64`, then delete its fragment.
* **Portrait.** `GsTalkScreen` / `FaceInfo` read the face of the skin for the talk portrait; `FaceInfo.from()` is now size-aware (it samples the centre texel of each model-pixel block), so an HD skin gives the same
  skin tone, slimness and legacy flag as before (`GsTalkScreenPureTest.anHdSkinIsReadAtTheCentreOfEachModelPixelBlock`).
* **Both layers.** The game drops the second layer and the hair beyond 24 blocks (`PlayerLod`): crisp details are drawn on BOTH layers; the second layer is a cutout (alpha 0 or 255) and only where the 64x64 sheet
  has cloth; the base layer is opaque; the glow map is premultiplied and as big as its skin.

## 3. Commands (for the next agents)

```
python3 -B tools/arc_hd/gen_arc_hd.py --list                                   # every look name and whether it is (hand) or (baseline)
python3 -B tools/arc_hd/gen_arc_hd.py <id> [<id> ...] [--exact] [--baseline]   # paint these ids (+ their _form2; --exact = only the names given; --baseline = ignore the hand module)
python3 -B tools/arc_hd/gen_arc_hd.py --all [--quiet]                          # every id of the roster (about 80 s: hand modules where they exist, else the baseline)
python3 -B tools/arc_hd/gen_arc_hd.py <id> --exact --out-dir /tmp/x/p --frag-dir /tmp/x/pf   # a try-out that touches nothing in the repo
python3 -B tools/arc_hd/merge_looks.py                                         # fragments -> looks.json
python3 -B tools/arc_hd/merge_looks.py --check                                 # exit 1 unless looks.json == merge(fragments) and every skin/glow obeys the kit's size rule
python3 -B tools/arc_hd/preview_arc.py out.png --trio --cols 1 <name>          # front, back, 3/4 bust in the software renderer (also --back, --close, --side; --skin-dir / --frag-dir for a try-out)
python3 -B tools/arc_hd/hair_map.py                                            # the hair table, and a check that every style exists
python3 -B tools/arc_hd/test_arc_hd.py                                         # the Python tests (8)
/tmp/claude-0/quick.sh com.newuniverse.nusmp.client.playermodel.ArcHdSkinsTest # the Java twin (square, multiple of 64, glow equals skin, fragments vs looks.json); also PlayerModelDataTest, GsTalkScreenPureTest
```

After any repaint: `merge_looks.py`, `merge_looks.py --check`, the tests above. **Do not run a full `tools/playermodel/gen_skins.py` to "see that it keeps the HD ids"**: PIL re-encodes about 25 other PNGs (the
pixels are identical, the bytes are not) and it changes one rotation in `pmx_zagred_legacy_form2.geo.json`; revert those files with `git checkout -- <files>` if it was run.

## 4. How to paint a character by hand

Copy `tools/arc_hd/chars/meliodas.py`; the file name is the look name (`meliodas_form2.py` for the second form). The contract is one function:

```python
def paint(spec, cid, masks):                     # -> (skin 256 PIL, glow 256 PIL or None, hair style id, hair rgb 0..255, slim)
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair="anime_spike_swept")   # the 64x64 result, scalp painted for that hair
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, hrgb, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)   # the baseline everywhere except the islands you redraw
    ...                                           # your redraws: base, overlay = A.lay(cv, part, face_name); H.cloth / H.trim_band / H.eye / H.stud ...
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim
```

Rules learned on the pilot:

1. **Work in model pixels** (`Face` coordinates: 8x12 front of the body is 0..8 x 0..12), but remember one texel is 0.25 model pixel: **a line thinner than 0.2 model pixel breaks into dots**. Stitches, seams and
   gap lines are kept at texel width (the pilot's `seam()` clamps the width to 0.2 and the dashes to 0.5/0.36).
2. **Keep the 64x64 silhouette.** Capture the second-layer coverage of the blueprint after the bridge (`cover_of`) and clip the redraw to it at the end (`hold_cover`); the test
   `test_the_pilot_second_layer_stays_inside_the_blueprint` checks it. A base island must be fully painted (draw over the bridge's result, or `skip` it and fill it whole).
3. **Draw crisp details on both layers** (`A.lay`), draw the head only on the front face (the other faces keep the baseline scalp; the 3D hair covers them).
4. **Hair colour: one value** for the scalp (`spec["hair"]["color"]` before `A.blueprint`), for the fringe you paint and for the returned `hrgb` (it becomes `hair_color` in the look). The sheet's
   `#e8c860` reads khaki after the hair tint; the pilot uses `#f6d044`.
5. `H.mouth_smile(..., open_h > 0)` needs a width whose half is exact in binary (4.0, 3.0, 2.0): a width like 3.9 puts one sample past the end of the curve and the helper raises a complex-number error.
6. Deterministic only (no random numbers; `H.hsh` is a hash). Numpy and Pillow only. About 1.5 s per character.
7. Glow: `face.glow_fill`, `H.gem(glow=True)`, `H.stud(glow=...)`; the glow map is premultiplied by `finish()`. The look entry gets `"glow": true` automatically when the map exists.
8. Preview after every round with `preview_arc.py` (use `--skin-dir`/`--frag-dir` on a try-out folder) and look at the texture crops too (a `PIL` crop at 8x of one island shows what the 3D view hides).

## 5. The pilot: Meliodas (`tools/arc_hd/chars/meliodas.py`)

**What he wears is the 64x64 sheet's outfit** (`tools/arc/char_meliodas.py`, `docs/arc/meliodas.md`): a dark vest with a brown yoke and a stand collar over a cream shirt, short cream sleeves with
brown cuffs, a wrapped right hand, a double belt with a gold buckle, dark trousers in calf boots. Only the drawing is finer. **Drawn at 4x detail**: the emerald anime eyes with lashes and highlights,
thick blond brows, a fringe of tapered locks with a dark edge and a light streak each, a wide grin with the upper teeth and one fang, a light blush; the vest panels with sheen, a lit edge, seams and brown
piping with a stitch beside it; the shirt with placket, buttons and the two collar flaps, shaded where the vest overlaps it; the double belt with stitching and rivets and the gold buckle (frame, dark
opening, prong, bevel); the sleeves with hem stitch and the stitched brown cuff; the skin of the forearms with a tendon line and fingers; the slanted turns of the right-hand wrap with its roll and loose end;
the trousers with knee creases and an outer seam; the boots with cuff, strap and buckle, ankle wrinkles, toe cap, welt and sole. The back has the yoke, a centre seam and the vest-tail vent.
**Added for the HD pass and not in the 64x64 sheet:** the Dragon's Sin of Wrath mark on the **left forearm** (the sources disagree on the place; below the sleeve it can be seen), the golden hair
`#f6d044`, the kit hair `anime_spike_swept`. No glow in this form (the sword's glow is on the weapon model). `meliodas_form2` stays on the baseline.

Rounds (all in `docs/previews/arc_hd/`, software renderer, flat light):

| round | file | what changed |
|---|---|---|
| 1 | `meliodas_round1_baseline_plus_first_paint.png` | first paint of all faces over the bridge: face, vest, shirt, belts, sleeves, wraps, mark, trousers, boots |
| 2 | `meliodas_round2_face_fringe_grin.png` | zig-zag fringe replaced by tapered locks; brows, eyes with a raised outer corner, the open grin with a fang |
| 3 | `meliodas_round3_contrast_buckle_wraps.png` | lighter palette, rim light on the vest panels, bigger buckle and buttons, cleaner wraps, texel-scale shading |
| 4 | `meliodas_round4_texture_crops_texel_scale_stitches.png` | texture crops: stitches and wrap gaps raised to texel width (they were dotted), no weave noise |
| 5 | `meliodas_round5_final_four_views.png`, `meliodas_final_texture_closeups.png`, `meliodas_final_texture_sheet_3x.png` | fringe gradient and brow colour; the shipped state |

`meliodas_baseline_vs_pilot.png`: the generic bridge against the hand-painted pilot from the same camera. `compare_64_vs_hd.png`: eight characters, old 64x64 against the HD baseline with kit hair.
`contact_1.png`, `contact_2.png`, `contact_3.png`: all 81 looks, front and back.

## 6. The hair (hair sheets stay 64x64; the kit hair is per character)

The reason of every choice is in the table; "KEPT classic" means the 64x64 generator's own hair style is kept because no anime cut fits (twin tails, bobs, the mohawk). A look that is not in the table gets the
classic style of its sheet. Hair sheets (`tools/playermodel/hair_*.py`, `gen_hair.py`) were not touched.

| look | kit hair | reason |
|---|---|---|
| `meliodas` | `anime_spike_swept` | a wild blond crest swept up and back: the big layered spike mass of the owner's reference 154 |
| `diane` | `twin_tails` | KEPT classic: the long twin tails of the Serpent's Sin (no anime twin-tail cut exists) |
| `ban` | `anime_spiky` | swept-up silver spikes with long side locks: thick vertical clumps |
| `king` | `anime_short` | a layered brown mop with a fringe over the brow: reference 156 |
| `gowther` | `curtains` | KEPT classic: the centre-parted pink bob |
| `gowther_form2` | `anime_long` | the long pink hair of his true form, full back and cheek locks |
| `merlin` | `bob` | KEPT classic: the black bob with the long fringe |
| `escanor` | `anime_short` | the short brown cut of Lowly Escanor |
| `escanor_form2` | `anime_mane` | the blazing mane of the Sun's noon: a full chunky mane |
| `asta` | `anime_spiky` | short upright spikes: thick clumps standing up, the hair of the main hero |
| `yuno` | `anime_bangs_long` | the parted fringe that sweeps to the temples and leaves the eyes free |
| `noelle_silva` | `twin_tails` | KEPT classic: two low twin tails |
| `yami_sukehiro` | `anime_spiky` | short black spikes swept up |
| `mimosa_vermillion` | `anime_long` | long wavy blond hair, a full back and cheek locks |
| `nozel_silva` | `anime_swept` | silver hair swept over the brow in a long fringe: the silver reference 155 |
| `luck_voltia` | `anime_short` | short messy blond |
| `magna_swing` | `mohawk` | KEPT classic: the sheet's mohawk |
| `finral_roulacase` | `anime_short` | short neat hair with a side fringe |
| `charmy_pappitson` | `anime_short` | short bob-like hair |
| `gordon_agrippa` | `anime_swept` | long black hair falling over one eye (a side-swept fringe) |
| `vanessa_enoteca` | `anime_long` | long wavy pink hair |
| `klaus_lunettes` | `anime_swept` | a neat side parting with the fringe over the glasses |
| `secre_swallowtail` | `bob` | KEPT classic: the black bob |
| `zora_ideale` | `anime_spike_swept` | red hair pushed back into a ragged crest |
| `gauche_adlai` | `anime_short` | short unkempt black hair |
| `marx_francois` | `bob` | KEPT classic: the short dark bob |
| `solid_silva` | `anime_swept` | pale hair with a side parting and a long fringe |
| `fuegoleon_vermillion` | `anime_swept` | red hair swept back with a fringe: a neat captain |
| `mereoleona_vermillion` | `anime_mane` | the wild red lion's mane |
| `leopold_vermillion` | `anime_spiky` | wild red spikes |
| `julius_novachrono` | `anime_long_open` | medium-long pale hair framing the face, no centre fringe |
| `william_vangeance` | `anime_swept` | long white hair swept over one eye |
| `nacht_faust` | `high_ponytail` | KEPT classic: the high ponytail |
| `dante_zogratis` | `slick` | KEPT classic: dark swept-back hair |
| `vanica_zogratis` | `bob` | KEPT classic: the black bob |
| `zenon_zogratis` | `anime_short` | short dark hair |
| `zenon_zogratis_form2` | `mohawk` | KEPT classic: the pale mohawk of his true form |
| `lucius_zogratis` | `anime_spike_tall` | tall black spikes, a crown of clumps; the true form is the same cut in white |
| `fana` | `anime_long` | long pink hair |
| `fana_form2` | `anime_mane` | her hair lifts into a wild mane in her second form |
| `heath_grice` | `slick` | KEPT classic: silver hair slicked back |
| `lotus_whomalt` | `anime_short` | short unkempt hair |
| `morris_libardirt` | `anime_spiky` | short blond spikes |
| `patolli` | `braid` | KEPT classic: the long braid |
| `rades_spirito` | `anime_short` | short messy hair |
| `rades_spirito_form2` | `anime_mane` | his hair flares into a mane in the second form |
| `revchi_salik` | `anime_short` | short unkempt dark hair |
| `rhya` | `anime_short` | short messy dark hair |
| `rhya_form2` | `anime_spiky` | white spikes in the second form |
| `mars` | `anime_spiky` | pale spiky hair |
| `mars_form2` | `anime_spike_tall` | taller pale spikes in the second form |
| `lucifero` | `anime_long` | long pale hair |
| `udaeus` | `-` | bald: the giant has no hair (the sheet says bald) |
| `vetto` | `anime_mane` | the wild golden mane |
| `zagred` | `anime_spike_tall` | tall dark spikes, the devil's crown of clumps |
| `zagred_legacy` | `spiky_long` | KEPT classic: the long spiky hair |
| `dark_disciple` | `anime_hood` | the cultist's hood (the 3.37 hood sheet) |
| `dark_disciple_astaroth` | `anime_hood` | the cultist's hood |
| `dark_disciple_beelzebub` | `anime_hood` | the cultist's hood |
| `dark_disciple_lucifero` | `anime_hood` | the cultist's hood |
| `dark_disciple_megicula` | `anime_hood` | the cultist's hood |
| `devil_avatar` | `-` | bald: the devil's body has no hair (horns are extras) |
| `devil_avatar_megicula` | `-` | bald |

## 7. Cost, worst case (rule 11) and server safety (rule 9)

* **Server: nothing.** Skins are client assets. No packet, no entity, no tick work, no config, no new class on the server; the look table keys are unchanged, so every network message and save is unchanged.
  The only Java change is `FaceInfo` (client, a handful of array reads per portrait build); the portrait is cached by the screen as before.
* **Disk:** the 81 skins and 46 glow maps are 3.2 MB in the jar (a 256x256 PNG of flat cloth compresses well).
* **GPU memory:** a 256x256 RGBA texture is 256 KiB (about 340 KiB with mipmaps) against 16 KiB at 64x64, 16 times more per skin; a skin is uploaded when its look is first drawn and released with the texture
  manager, so the worst case is the number of DISTINCT looks in view, not the number of entities: all 127 textures resident would be about 43 MB, a normal scene a handful (a few MB). No change when none of
  these characters is near.
* **Draw cost per frame:** unchanged geometry (the same player-model quads, the same hair meshes); only the texture lookup is larger, and mipmaps keep far characters on the small levels. `PlayerLod`
  still drops the second layer and the hair beyond 24 blocks.
* **Build cost (tools only):** about 1.2 s per hand-painted character, about 1 s per baseline character, about 80 s for the whole roster; numpy and Pillow, no GPU.

## 8. NEW, REPLACED, KEPT (rule 5)

* **REPLACED** (same file names, rule 2 exception): the 81 skin PNGs and 46 glow PNGs of the roster (64x64 to 256x256); the hair choice and `hair_color` of 54 look entries (from the sheet's classic style to
  the kit's `anime_*` cut; the rest keep their classic hair for the reason in the table); `FaceInfo.from` (now size-aware; it also reads 64x64 skins exactly as before).
* **NEW:** `tools/arc_hd/` (roster, hair map, library, generator, merger, preview, pilot, tests), `tools/arc_hd/looks/*.json` (81 fragments), the ownership guard in `gen_skins.py`,
  `ArcHdSkinsTest`, one test of `GsTalkScreenPureTest`, this page, `docs/previews/arc_hd/`, the Dragon's Sin mark on the pilot.
* **KEPT:** the 64x64 generator and its specs (still the blueprint and the `--size 64` mode), every look key, scale, hold and extras entry, every model and animation, the hair sheets, the Time Magic VFX,
  `tools/tier_bosses/hd`, DanMachi and every other franchise.

## 9. Sources (rule 10) and what was not seen in a game

* English wiki first: `nanatsu-no-taizai.fandom.com` and the Japanese `ja.wikipedia.org` were requested and **could not be fetched** (the session's network refuses those hosts); nothing was read from them
  for this change. The outfit colours come from the repo's own sheet (`tools/arc/char_meliodas.py`) and `docs/arc/meliodas.md`, which cite their sources there (search summaries, weakly sourced where marked);
  the face, hair and the shape of the Dragon's Sin mark are **from memory** of the character's look, original art, nothing traced. The other 80 names are unchanged in colour and design: they are the
  64x64 sheets enlarged by the generic bridge, so no new canon claim is made for them.
* **Not seen in a game.** Everything was checked in the repo's software renderer (flat shading, no game light, no hair tint pass) and by tests; nobody has looked at it in Minecraft. What to look at first:
  the hair colour against the skin (the tint pass may shift `#f6d044`), the baseline outlines on very dark outfits, the portrait (`GsTalkScreen`) with an HD skin, and the memory cost on a server with many
  distinct looks near each other.
* The CI build, the Gradle build and the server smoke test were not run in this session (disk and no Gradle).

## 10. Decisions and next steps for the owner

1. More hand-painted characters at the Meliodas level (the pilot took about five rounds): the natural order is the Seven Deadly Sins (`diane`, `ban`, `king`, `gowther`, `merlin`, `escanor`, then their second
   forms), then the Black Clover captains. Each is one module `tools/arc_hd/chars/<name>.py`; `gen_arc_hd.py --all` picks it up.
2. `meliodas_form2` still has the sheet's `#e8c860` hair next to the pilot's golden `#f6d044`; give it a module or the same hair colour when the owner wants the two forms to match.
3. The seven Black Clover NPCs that are not on the list (see above) are still 64x64; one command does them.
4. The new bosses follow (owner: YES): the Tier List bosses are already HD (`tools/tier_bosses/hd`); the roster of the next change is the owner's.
