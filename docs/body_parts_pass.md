# Body-parts pass: Saiyan monkey tail, Hydra fan, head scale

Owner asks: (1) "Rework the Saiyan tail. It should be a monkey tail, not some lizard looking tail." (2) "Rework the Hydra heads, because they're too close together and they don't feel like Hydra heads."
(3) "Fix the Fenrir wolf head to be smaller and not so giant ... anything that's too big compared to the actual player's head."

Nothing here was seen in a game. Everything below is checked by generator output, the numpy previews in `docs/body_parts_previews/`, and pure tests.

## NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| NEW | Saiyan monkey tail meshes `saiyan_tail_monkey` (hang) and `saiyan_tail_wrap` (curled round the waist), generator `tools/gen_saiyan_monkey_tail.py` |
| NEW | Fanned neck rig for Hydra, Typhon and Yamata no Orochi (own neck chain, own jaw bone, own sway / bite phase per head) |
| NEW | `BeastWillHeadFit` (table of the reworked wills, limits, suffix rule), audit tool `tools/audit/head_scale_audit.py`, preview tool `tools/beastwill/remake/body_parts_preview.py` |
| REPLACED (default look) | Saiyan SS4 tail look (lizard tail -> monkey tail), Hydra / Typhon / Orochi heads (even ring -> fan), head piece size of 12 other wills (Fenrir, Ridge Wolf, Lion, Thunder Fang Chimera, Cu Sith, Cerberus, Minotaur, Pegasus, Sphinx, Ammit, Elderwood Guardian, Scylla and Charybdis), the Beast Will aura sprite heads and the Demon Water crown |
| KEPT | The lizard tail meshes (`saiyan_tail*` old ids) stay registered; the old head models stay as `bwf_<stem>_v2_arm_*` geo / animation files; the old aura / Demon Water layout stays behind the same setting; `_v1` classic forms untouched |

## Settings (client, looks only; nothing changes gameplay)

| Key | Default | Effect |
|---|---|---|
| `saiyan.monkeyTail` | on | off = the old tail look |
| `saiyan.tailWrap` | off | on = the tail is wrapped round the waist (looser sway) |
| `saiyan.tailAllForms` | off | on = the tail also shows on the other Saiyan forms, not only SS4 |
| `beastwill.classicHeads` | off | on = the `_v2` models (before this pass), the old fanned-less aura heads and the even Demon Water ring |

`beastwill.classicForms` (3.61.0) still wins over `classicHeads`: `BeastWillHeadFit.suffix(classicForms, classicHeads, stem)` gives `_v1`, `_v2` or none.

## 1. Saiyan monkey tail

Furry brown tube along a Catmull-Rom path: 8-sided, radius 1.45 tapering to ~0.9 with a small tuft swell at the tip, fur strands laid along it in a staggered spiral (two quads each, so backface culling never hides them),
a tip tuft fan, a fur skin (tube columns plus strand variants) and a glow map. Three parts (root / middle / tip) driven by the existing `SaiyanHairDraw.Rig` sway, so it has an idle sway and a looser
sway when wrapped. Tint follows the form's fur colour (`SaiyanHair.monkeyTint`). Quads: hang 388 (LOD 157), wrap 472 (LOD 193); the existing cap `SaiyanHair.MAX_TAIL_QUADS` is 700.
Test: `SaiyanMonkeyTailTest` (quad caps, parts, ids, skins). The existing self-test prefix `saiyan.hair.rules` also checks the monkey id rule; no new prefix.

## 2. Hydra fan

Before: the necks of the 4 / 7 heads started a few px apart on one shoulder line and leaned in the same direction, so the heads sat in one clump. Now every head has a base angle (tilt and azimuth), a
different neck length (17 to 26 px), a root staggered across the shoulders and along the back, an S-bend, a lower neck that sways, an upper neck that whips on its own phase, and a jaw bone that bites at
its own time (staggered over the 4.8 s clip). Angles between any two necks: Hydra and Typhon 52 degrees at the least, Orochi 36 degrees (limit 35). Head counts kept: 4, 4, 7.
Typhon's heads use the light head version (368 cubes) to stay under the 400-cube cap. Cerberus keeps its old two side heads (a hydra look does not fit it).
Previews: `hydra_`, `typhon_`, `yamata_no_orochi_before_after.png`.

Aura sprite heads (`BeastWillAura`) and the Demon Water crown (`DemonWaterLayer`) get the same idea: fanned directions, own sway / bob / bite phase per head, own neck length and stand-off.

## 3. Head scale

Yardstick: the player's head, 8 x 8 x 8 px. `python3 tools/audit/head_scale_audit.py` measures 896 geo head bones (skull = biggest chunky cube without mane / horn / ear / hair children) and, for the
Beast Will forms, the whole head piece (mane, ears and horns included), plus the Blender entity meshes.

Result: no unexplained outlier (skull above 1.6x). The 25 geo models above 1.6x are all exempt, each with its reason printed by the tool:

| Exempt | Why |
|---|---|
| firecast dragon (all LODs), charybdis spawn, anime goliath / goliath body, recombanime golem, gnome_full (all LODs) | huge creature / boss by design (gnome_full is drawn at 0.82 and is about 8 blocks tall) |
| abyssal hezrou p1 to p3 | Abyssal demon boss, large by design |
| spiritlight cub (all LODs) | drawn at scale 0.25 (`SpiritLightDraw`) |
| spirit space wisp (all LODs) | flat fx quad, not a head |
| nail_hammer, copper_mace | the "head" of a hammer, not a body part |
| beast_form_ghost, recombination_bull | large construct / quadruped by design (1.75x) |

Beast Will head pieces, whole piece, width x height as a multiple of the player's head, before (the kept `_v2`) -> after:

| Will | Before | After |
|---|---|---|
| Fenrir | 2.37 x 1.98 | 1.69 x 1.66 |
| Ridge Wolf | 2.24 x 1.98 | 1.68 x 1.71 |
| Lion | 2.49 x 1.61 | 1.74 x 1.54 |
| Thunder Fang Chimera | 2.28 x 1.61 | 1.88 x 1.54 |
| Cu Sith | 2.19 x 2.31 | 1.67 x 1.95 |
| Cerberus | 2.15 x 1.98 | 1.65 x 1.71 |
| Minotaur | 2.31 x 1.98 | 1.94 x 1.55 |
| Pegasus | 2.19 x 1.99 | 1.67 x 1.78 |
| Sphinx | 2.06 x 2.15 | 1.61 x 1.98 |
| Ammit | 2.06 x 1.45 | 1.61 x 1.45 |
| Elderwood Guardian | 2.01 x 2.54 | 1.33 x 2.00 |
| Scylla and Charybdis | 2.06 x 1.80 | 1.61 x 1.80 |
| Typhon, Hydra, Orochi | head pieces unchanged (the heads are on the body piece, see section 2) | |

How: smaller head scale (`hs`), smaller mane (`mc`), smaller ears (`es`) and horns (`horn`) per will (`STYLE` knobs in `tools/beastwill/remake/forms_gen.py`). The wolf-family manes now hug the skull. Fenrir
is held to 1.75x, every other reworked will to 2.05x, by `BeastWillHeadFitTest`.

Still tall, listed and not changed: Kraken 2.92 high (tentacle crown), Nidhoggr 2.42, Dragon 2.23, Kitsune 2.16 (fox ears). Their skulls are 1.10 to 1.15x; the height is horns, tentacles and ears.
The audit prints them as EXEMPT with that reason. If the owner wants those shorter too, it is a knob change (`horn`, `es`) plus regeneration.

Blender entity meshes (devils, bosses): the head part includes horns, hair and wings and the skull cannot be told apart, so the audit only lists them (Lucifero 5.0x high with horns, Daemon Blanc
3.1x, Astaroth 2.5x, ...). Not changed: these are demon lords / bosses where a large crown is the design, and no mesh skull could be measured to prove they are over. Caveat: unverified in game.

## Worst-case cost (rule 11)

- Saiyan tail: at most 472 quads per SS4 player (LOD 193 far away), under the existing 700 cap, same distance culling and perf budgets as the old tail; no packets, no server work.
- Hydra fan: generated geo only. Cubes per will stay under the 400 cap (typhon 368), quads under 2000; the rig is bone animation (no per-tick Java work, no allocations).
- Aura sprite heads: at most 7 heads and 4 neck ribbons (quad cap 22 per aura), the existing aura budget; Demon Water crown: the same ribbon count as before, only per-head arithmetic.
- No new packets, no server state, no new entities. All settings are CLIENT configs; nothing touches a server path.

## Tests

- `BeastWillHeadFitTest` (new, pure file reads): head piece size limits (Fenrir 1.75x, others 2.05x, Fenrir clearly smaller than before), fan angles >= 35 degrees and head counts 4 / 4 / 7 with a jaw bone each, `_v2` copies of every reworked piece, suffix rule.
- `SaiyanMonkeyTailTest` (new), `SaiyanLookSelfTests` (extended inside `saiyan.hair.rules`, no new prefix).
- Existing `BeastWillFormsTest`, `GeoAssetsTest` and the source-scan tests cover the regenerated files (cube / quad caps, uv inside the sheet, loop length).
- CI compiles; nothing was compiled or run locally (no Gradle in this session).

## Caveats

- Never seen in a game: all looks are judged from the python previews only (flat shading, no real lighting, no animation playback).
- The monkey tail fur strands depend on the 128 px skin layout in `SaiyanHairDraw`; check in game that the tuft and the wrap do not clip the legs in the sprint pose.
- Cube rotations are ignored by the audit (sizes as written) and the renderer's own scale is only applied where the exempt table says so.
- Case-only identifier noise from the regeneration (`armL` -> `arml`) was reverted for the pieces that did not really change.

## Re-running

```
python3 tools/gen_saiyan_monkey_tail.py
python3 tools/audit/head_scale_audit.py
python3 tools/beastwill/remake/body_parts_preview.py fenrir_will hydra_will
python3 tools/beastwill/remake/snapshot_heads_v2.py   # only before changing the generator: copies the current models to _v2
```
