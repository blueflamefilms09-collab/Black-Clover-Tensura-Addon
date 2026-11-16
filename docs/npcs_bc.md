# Black Clover NPCs and the Midnight Sun on the redesigned player model

Request: the Midnight Sun elves and every other Black Clover NPC / character humanoid get the same quality as the Black Clover bosses of `docs/bc_playermodels.md` (4.14.0): a painted anime face on a 256x256 skin, layered cube hair and fringe, clothes and ornaments, glowing eyes and marks, the wiki palette, a near-vanilla head proportion and an idle sway.

Setting: the **same client option** as the bosses, **`bossLooks.bcPlayerModel`** in `config/nusmp-playermodel-client.toml` (CLIENT config, default on). **false restores the previous look of every character in this document**, no new setting and no new lang key. Ids, hitboxes, saves, fights, drops and AI are untouched.

## What was NOT seen, and what was not done

- The sheets in `docs/npcs_bc_previews/` (`overview.png` and one turnaround sheet per look: front, three-quarter, side, back) are **software renders** from `tools/bc_playermodels/preview_npc.py` (it reuses the boss preview renderer). They are not the game: lighting, mip-mapping, fog, the additive glow level and the sway are different, the extras are frozen at an idle frame. **Nothing was run in a game.**
- **Gradle was not run** (none in this session; CI compiles). The Java is small and was reviewed by reading only; the pure test `BcNpcPlayerModelsTest` and the CI self-test `bcnpc.looks` exist but have not been executed by me.
- **Not made in Blender** (rule 14): Blender is not available. Every part is a cube or a card built by the generator `tools/bc_playermodels/specs_npc.py` + `parts_npc.py` (same method as the bosses), with generator-painted atlases. The **skins are not hand-painted**: the face is drawn by the face painter over each character's earlier skin (the kit's hat layer is cleared so the cube hair sits on a clean head). Six old skins (Charlotte, Dorothy, Grey, Jack the Ripper, Kahono, Kiato) were 64x64 and are upscaled 4x nearest-neighbour by `bcskin._load`, the face tone of Grey, Dorothy and Kahono is given by hand where the cheek of the old skin was unreadable.
- Colours and shapes come from the English wiki text only (no reference images were looked at), so proportions of accessories are an interpretation.

## Sources (rule 10)

English Black Clover Fandom wiki first, `Appearance` section of each page, fetched with curl through the MediaWiki `api.php` and treated as untrusted data: Rhya, Fana, Patolli, Rades Spirito, Asta, Yuno, Noelle Silva, Yami Sukehiro, Finral Roulacase, Magna Swing, Luck Voltia, Charmy Pappitson, Vanessa Enoteca, Gordon Agrippa, Zora Ideale, Gauche Adlai, Grey, Secre Swallowtail, Nacht Faust, Julius Novachrono, Marx Francois, Mimosa Vermillion, Klaus Lunettes, Leopold, Fuegoleon and Mereoleona Vermillion, Charlotte Roselei, Nozel and Solid Silva, Jack the Ripper, Dorothy Unsworth, William Vangeance, Kahono, Kiato. The Japanese wiki was not needed. Each character's skin tone is sampled from the cheek and chin of the character's existing skin so the head matches the arms. The `_form2` looks of the Midnight Sun (released-magic forms) come from the same pages' elf-form descriptions; nothing was fetched that failed.

## What it is

| Layer | What |
|---|---|
| Skin (256x256) | New face painted on the head of the character's existing skin (two-tone iris, lashes, brows, mouth, blush, per-character marks: scars, Magna's forehead marks, the Vermillions' eye markings and diamond, Julius' star, the Midnight Sun's slashes, crescents, tattoos and stone frames); glowing eye and mark pixels go to the `_glow` map |
| Hair | Cube hair of the extras model (shell, tiers, spikes, fringe, side locks, braids, buns, tails, mohawk); **no kit hair** (`BcNpcAudit` fails a look that has one) |
| Parts | Elf ears, kippah, braids, hoop earrings and shawl (Patolli), eye bandage (Rades), forehead stones (Fana), witch hats (Vanessa, Dorothy), cap, hoods, scarves, sashes, glasses, half mask (William), fish masks (Kahono, Kiato), feather panels (Nozel, Solid), gauntlets and collar plate (Charlotte), shells, bun and ribbon, headbands, banner |
| Glow | Only the `_fx` model has the glow flag and only where the character glows (gem on the scarf, forehead stones, marks); at most 18 glowing quads |
| Shimmer | none for these looks (`BcLooks.theme` is null for them, so `BcShimmer` returns at once); no new particles |

Three models per look at most (`pmx_bc_<id>`, `_fine`, `_fx`): `main` always, `_fine` only inside the detail distance (24 blocks by default, `ExtrasDraw`), `_fx` the glowing ones. The old extras of every character (Midnight Sun glows, weapons, capes) are **kept** in front of the new parts (`keep_extras`).

## Per character

Counts are the NEW parts (`tools/bc_playermodels/stats_npc.json`); the old extras stay as before and are not in the numbers. The quad budget checked by `BcNpcAudit` is `BcAudit.MAX_QUADS` = 700 (all new models) and 120 glowing quads; the worst look is **278** quads (`patolli`, `patolli_form2`), the worst glow is 18 (`fuegoleon_vermillion`).


### Midnight Sun

| Look | Canon palette and marks (English wiki, Appearance) | Cubes main / fine / fx | Quads main / fine / fx | Full-detail quads | Atlas tiles |
|---|---|---|---|---|---|
| `rhya` | black messy hair, white-black elf robes, blue-violet eyes, red slash scars on the cheek | 36 / 11 / 0 | 76 / 11 / 0 | 87 | 29 |
| `rhya_form2` | white hair, golden glowing eyes, red tattoo lines up the face (glow) | 46 / 11 / 0 | 106 / 11 / 0 | 117 | 32 |
| `fana` | pink hair, green eyes, stone frame mark on the forehead | 11 / 45 / 1 | 66 / 45 / 2 | 113 | 30 |
| `fana_form2` | bright pink hair, slit amber glowing eyes, fang mouth, red stone frame (glow) | 40 / 45 / 1 | 120 / 45 / 2 | 167 | 44 |
| `patolli` | near-white hair, golden eyes, red crescent marks | 51 / 0 / 2 | 270 / 0 / 8 | 278 | 19 |
| `patolli_form2` | white-violet hair, magenta glowing eyes on dark sclera, fang mouth, magenta crescents | 51 / 0 / 2 | 270 / 0 / 8 | 278 | 19 |
| `rades_spirito` | purple hair, blue eyes, diagonal scar through one brow | 29 / 43 / 1 | 72 / 43 / 2 | 117 | 35 |
| `rades_spirito_form2` | light violet hair, green glowing slit eyes, fang mouth, green glowing scar | 42 / 61 / 1 | 110 / 61 / 2 | 173 | 40 |

### Black Bulls

| Look | Canon palette and marks (English wiki, Appearance) | Cubes main / fine / fx | Quads main / fine / fx | Full-detail quads | Atlas tiles |
|---|---|---|---|---|---|
| `asta` | short grey-white spiky hair, green eyes, wide grin | 44 / 11 / 0 | 107 / 11 / 0 | 118 | 30 |
| `yami_sukehiro` | black spiky hair, grey eyes, stubble and goatee | 37 / 8 / 0 | 72 / 8 / 0 | 80 | 26 |
| `noelle_silva` | silver-white twin tails, pink eyes, blush, open mouth | 10 / 21 / 2 | 60 / 21 / 8 | 89 | 28 |
| `finral_roulacase` | brown-gold hair, green eyes, open mouth | 28 / 6 / 2 | 68 / 6 / 8 | 82 | 21 |
| `magna_swing` | black mohawk with grey tip, blue eyes, tinted glasses, grin, hood down | 27 / 0 / 0 | 78 / 0 / 0 | 78 | 7 |
| `luck_voltia` | yellow spiky hair, light-blue eyes, wild grin | 32 / 13 / 0 | 72 / 13 / 0 | 85 | 27 |
| `charmy_pappitson` | dark short hair, green eyes, big blush and open mouth | 9 / 4 / 0 | 54 / 4 / 0 | 58 | 7 |
| `vanessa_enoteca` | pink hair, violet eyes, long lashes, witch hat | 14 / 26 / 1 | 84 / 26 / 2 | 112 | 31 |
| `gordon_agrippa` | black hair, red eyes with dark eyeshadow, cap | 10 / 0 / 0 | 55 / 0 / 0 | 55 | 5 |
| `zora_ideale` | red spiky hair, amber eyes, straps across the face | 40 / 0 / 0 | 80 / 0 / 0 | 80 | 18 |
| `gauche_adlai` | brown hair, brown eyes, mirror mark on the cheek | 32 / 11 / 0 | 72 / 11 / 0 | 83 | 23 |
| `grey` | light-blue hair, blue eyes, soft blush | 8 / 24 / 0 | 48 / 24 / 0 | 72 | 24 |
| `secre_swallowtail` | dark hair, grey eyes, eye bags, antenna locks, hood down | 11 / 20 / 0 | 46 / 20 / 0 | 66 | 22 |
| `nacht_faust` | black hair, light-blue eyes, smirk | 12 / 21 / 1 | 72 / 21 / 2 | 95 | 22 |

### Magic Knights, Golden Dawn, Crimson Lion, Silver Eagle, Blue Rose, Seabed Temple, Green Mantis, Coral Peacock, court

| Look | Canon palette and marks (English wiki, Appearance) | Cubes main / fine / fx | Quads main / fine / fx | Full-detail quads | Atlas tiles |
|---|---|---|---|---|---|
| `julius_novachrono` | pale-blond hair, violet eyes, six-point star mark, mantle and necklace | 39 / 27 / 1 | 94 / 27 / 2 | 123 | 29 |
| `marx_francois` | sand-blond hair, blue-grey eyes, mantle and necklace | 11 / 27 / 1 | 66 / 27 / 2 | 95 | 16 |
| `yuno` | black hair, glowing golden eyes, side locks, necklace, hood down | 36 / 13 / 1 | 96 / 13 / 2 | 111 | 30 |
| `mimosa_vermillion` | vermilion-orange hair (see decisions), gold eyes, blush | 9 / 43 / 2 | 54 / 43 / 8 | 105 | 27 |
| `klaus_lunettes` | blue hair, magenta eyes, glasses, hoop earrings, scarf and sash | 16 / 6 / 2 | 76 / 6 / 8 | 90 | 15 |
| `leopold_vermillion` | vermilion hair, teal eyes, eye markings and diamond mark, grin | 47 / 11 / 2 | 117 / 21 / 10 | 148 | 33 |
| `fuegoleon_vermillion` | deep red hair, violet eyes, eye markings and diamond mark | 8 / 37 / 4 | 48 / 47 / 18 | 113 | 24 |
| `mereoleona_vermillion` | red-orange hair, blue eyes, eye markings, fang mouth | 37 / 65 / 2 | 82 / 75 / 10 | 167 | 43 |
| `charlotte_roselei` | blond hair, light-blue eyes | 31 / 7 / 0 | 176 / 7 / 0 | 183 | 20 |
| `nozel_silva` | white-silver hair, violet eyes, narrow brows | 42 / 56 / 0 | 107 / 56 / 0 | 163 | 37 |
| `solid_silva` | blue hair, blue eyes | 43 / 39 / 2 | 133 / 39 / 4 | 176 | 38 |
| `jack_the_ripper` | black hair, glowing golden eyes, long scar down the face | 35 / 13 / 0 | 80 / 13 / 0 | 93 | 29 |
| `dorothy_unsworth` | lilac hair, violet eyes, drowsy lids, witch hat with flowers | 23 / 20 / 1 | 138 / 20 / 2 | 160 | 32 |
| `william_vangeance` | white hair, violet eyes, half mask | 37 / 0 / 1 | 92 / 0 / 2 | 94 | 22 |
| `kahono` | crimson hair, red eyes, blush | 15 / 21 / 1 | 90 / 21 / 2 | 113 | 24 |
| `kiato` | navy hair, blue-grey eyes | 34 / 9 / 0 | 84 / 9 / 0 | 93 | 27 |


Decisions worth a look by the owner: **Mimosa's hair is vermilion-orange** because the wiki text says "long wavy vermilion hair"; the anime shows it closer to a pink-orange, so change the colour of `mimosa` in `specs_npc.py` if the owner prefers that. Rades' scar crosses one brow, which is skipped on purpose. Fana and Patolli keep the elf ear colour of their old skin (`ears_color`).

## How the looks are used

- `PlayerBodies.ArcNpc.look` (the entity of every friendly arc character, the thralls excluded) asks `PlayerAssets.bcId(characterId)` first, so the new look is used while `bossLooks.bcPlayerModel` is on and the old id is used when it is off or when no new look exists. `PlayerBodies.Boss.look` already did this for the Midnight Sun bosses (`rhya`, `fana`, `patolli`, `rades_spirito` and their `_form2`), so the elf bosses and the elf NPC entities now wear the same new look.
- `PlayerAssets.bcId` asks `BcLooks` (the Zogratis bosses) first and then the new pure table `BcNpcLooks` (38 ids, appended, the boss table is not touched). The new ids are `bc_<old id>` in the table `looks_bc_npc.json`, merged **last** in `PlayerLookTables.FILES` (earlier tables win a duplicate, add-only).
- **Midnight Sun thralls are vanilla zombies** (`RadesThralls`): no player model, nothing to do.

## Worst-case cost (rule 11)

| What | Bound |
|---|---|
| Server | zero: client-only look data; no entity, no packet, no tick work, no config that changes gameplay |
| Network | zero new packets |
| Client draw | at most 278 quads per character at full detail (the fine layer is dropped beyond 24 blocks, so far away at most 170), 18 glowing quads; atlases are 256x256 per look; no per-frame allocation (the look and `bcId` are table lookups, `BcNpcLooks.of` allocates nothing) |
| Many NPCs at once | the arc NPCs stand in towns and arenas, a few at a time; ten visible characters at the worst look cost under 3000 extra quads, inside the player-model kit's existing extras budgets (`PerfProbe` unchanged) |

## New / replaced / kept (rule 5)

- **NEW**: `looks_bc_npc.json` (38 looks `bc_<id>`), skins `bc_<id>.png` (+ `_glow`), atlases `pmx_bc_<id>*.png`, geos and animations for 38 looks, `parts_npc.py`, `specs_npc.py`, `preview_npc.py`, `BcNpcLooks`, `BcNpcAudit`, the self-test `bcnpc.looks`, `BcNpcPlayerModelsTest`, this document and the previews.
- **REPLACED** (while the option is on): the visual look of the 38 characters above, and only that. The NPC entities and Midnight Sun bosses draw the new look instead of the old one.
- **KEPT**: every old look, skin, glow, atlas, extras model and animation stays registered and is drawn when the option is off; the old extras of each character still draw with the new look.

## Skipped and why

- **Midnight Sun thralls**: vanilla zombies, no humanoid model.
- **Non-Black-Clover people** in the same tables (Seven Deadly Sins, Danmachi, Fairy Tail ids, generic NPCs such as `merchant_npc`, `quest_giver_npc`, `npc_default`, `tower_wizard`, `painted_knight`, `black_market_witch`): not part of this task.
- **Zogratis bosses** (`lucius`, `dante`, `vanica`, `zenon`, `zagred`, `lucifero`): already done in 4.14.0 (`docs/bc_playermodels.md`), not touched.
- **Arc bosses whose canon I did not look up** (`vetto`, `heath_grice`, `lotus_whomalt`, `mars`, `revchi_salik`, `udaeus`, `morris_libardirt`, `dark_disciple*`, `devil_avatar*`): they are bosses in the other tables, their pages were not fetched here and a look drawn without the canon would be a guess. They keep their old looks.
- **Characters with no look in the tables** (any Black Clover person the mod has no humanoid look for): not invented.

## Tools and tests

```
python3 -B tools/bc_playermodels/gen_bc.py            # everything (boss looks and these), deterministic
python3 -B tools/bc_playermodels/gen_bc.py asta       # only the looks whose id contains 'asta'
python3 -B tools/bc_playermodels/preview_npc.py       # docs/npcs_bc_previews/ (add --old for the old look on the left)
```

`BcNpcPlayerModelsTest` (pure): the audit of the real resource files finds nothing (every look and its old look exist, skin 256x256, no kit hair, valid geo, every atlas rectangle inside the atlas, UV in range, quads inside the budget); every listed character has a look and the same ids are in `BcNpcLooks`, `looks_bc_npc.json` and the generator stats; the table is merged last; the NPC body asks `bcId`; the new classes have nothing server-unsafe; the self-test, doc and previews exist. `BcPlayerModelsTest` was widened by one `||` so it accepts the new `bc_` ids. The CI self-test is **`bcnpc.looks`** (the smoke list has to get the prefix `bcnpc`).
