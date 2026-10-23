# Spirit Dive MERGE: the diver merges with the spirit, like the anime

> Owner's words: "for any Spirit Dive the User Merges with the spirit like the anime." This is that layer. It is **one** additive client-only painter, `SpiritDiveMerge`, hooked **once** in `SpiritDiveAura`
> before the look is chosen, so it runs for **every look** (REEL, ANIME, DJINN, V2, CLASSIC, and every Wind design A / B / C / PACK / REEL) and **every element** (Wind, Water, Fire, Earth), standard and character tier.
> Client option `spiritDive.merge` (default **true**; `false` = the dive is exactly as before).

Nothing here was seen in a game. Every picture in `docs/previews/spirit_dive_merge/` is the project's software harness (`tools/render_preview`, the real painter classes against Minecraft stubs): no bloom,
approximate additive blending, no first person. Section 8 lists what is not verified.

## 1. The story (what the merge shows)

A Spirit Dive is the mage taking the spirit's mana into the body (canon in `docs/magic/spirit_dives_anime.md`: Sylph the wind fairy, Undine the water mermaid, Salamander the fire lizard, Gnome the earth spirit).
One clock `m` drives everything: it runs forward over the first 22 ticks of the dive and **backwards** over the last 20 (the REEL look's clock, so the exit is the entry backwards).

| Phase | Clock | What is drawn |
|---|---|---|
| **1 appears** | 0..8 | the spirit of the element materialises **beside and above** the wearer (a ghost of its own shape, additive, in the colour of the element), rising into place, fading in, bobbing a little |
| **2 flows in** | 8..18 | it **shrinks and spirals into the chest** along a curved path (about 1.15 turns, the radius closing, a small arc), turned to face the chest, with **3 (standard) / 5 (character) trailing light strips**; at the contact (**14.5**) a **flash** (two glow sprites) and a **ring** running out from the chest; the wearer's body **takes the spirit's colour** for a moment (a cover pass over the player's own parts, bell shaped, gone by tick 21) |
| **3 within** | from 15 | a **very faint silhouette** of the spirit round the torso (alpha **12 percent at most**, 288 vertices, **full level of detail only**), breathing slowly (size and alpha); the Salamander's coils slowly round the torso |
| **Exit** | same clock backwards | the silhouette fades, the spirit **separates from the chest**, spirals out, floats off and fades |

**Character tier** (Yuno with the Sylph, Fuegoleon with the Salamander, Noelle with the Undine; never Earth): the spirit is **larger** (Sylph 1.25 vs 0.95, Undine 1.25 vs 0.95, Salamander 1.45 vs 1.05 of a 32 px mesh),
hovers farther out, has **5 strips** instead of 3, and takes its **canon form**: the Sylph is the **fairy with her wings spread**, the Undine the **mermaid**, the Salamander **coiled with its neck raised and its tail curled round**.
The standard tier uses the simple spirit models (Sylph, Salamander, Gnome); the Undine of the standard tier is the mermaid at the smaller size (the simple Undine model is a wall of hair plates and did not read as a spirit).

The side the spirit appears on (left or right of the wearer) comes from the player's UUID hash, so two divers side by side do not mirror each other. It is a function, not state: nothing is stored per player.

## 2. NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| **NEW** | the whole merge layer (`SpiritDiveMerge`, the pure rules `SpiritDiveMergeRules`, the mesh reader `SpiritDiveMergeMesh`), 11 light spirit meshes (`sdm_meshes/`) built from the mod's own spirit geo models by `tools/gen_sdm_models.py`, 2 textures (`aura/sdm_skin`, `aura/sdm_trail`), the option `spiritDive.merge`, the self-test `magic.spirit_dive_merge`, the preview scenes and sheets, 3 tests |
| **REPLACED** | nothing. The merge is a new layer; no old effect was removed or changed (the REEL/A look's own entry flash, spiral ribbons and sphere are untouched) |
| **KEPT** | every look and its painters, the ANIME look's own "spirit ghost behind the wearer" (character tier, `SpiritDiveAnime.backdrop`), the spirit geo models and renderers, all configs. Shared files only **appended** to: `SpiritDiveAura` (one hook line), `DiveClientConfig` (the option and its mirror), `SpiritDiveBoot` (the self-test registration), `tools/render_preview/examples/` (3 helpers) |

## 3. Files

| Area | File |
|---|---|
| pure rules (common, no Minecraft class) | `book/SpiritDiveMergeRules.java` (clock, path, strengths, budgets, tables, vertex counts), `book/SpiritDiveMergeMesh.java` (reads `sdm_meshes/*.txt` with the quad parser of `SpiritDiveWind3Mesh`) |
| painter (client) | `client/aura/SpiritDiveMerge.java`; hook: one line in `client/aura/SpiritDiveAura.paint`; option mirror in `client/aura/DiveClientConfig` |
| self-test | `book/SpiritDiveMergeSelfTests.java` (key `magic.spirit_dive_merge`, registered in `SpiritDiveBoot`) |
| assets | `assets/nusmp/sdm_meshes/{wind,water,fire,earth}_{std,char,core}.txt` (11 files; Earth has no char), `textures/aura/sdm_skin.png`, `sdm_trail.png` |
| generator | `tools/gen_sdm_models.py` (rebuilds the meshes and textures and `docs/previews/spirit_dive_merge/models.txt`; `--view out.png` for a debug contact sheet) |
| previews | `tools/spirit_dive_merge_scenes.py` (56 scenes), `tools/spirit_dive_merge_sheets.py`, helpers `tools/render_preview/examples/SdmOn|SdmOff|SdmSolo.java` |
| tests | `SpiritDiveMergeTest` (rules), `SpiritDiveMergeAssetTest` (files), `SpiritDiveMergeScanTest` (source) |

The meshes: the spirit geo files stay untouched. The generator poses a geo model (rest pose, plus bone rotations for the character Salamander), takes its **largest cubes by volume** (29 for a ghost = 696 vertices, 12 for the silhouette
= 288 vertices), normalises the longest side to 32 px, bakes the colour of the element and a light from the front and above, and writes quad lines (`q 1 ...`, all on the additive layer) in the layer space of the player renderer
(model pixels, y down, front -z). Wing plates are dimmed to half so the spirit itself reads.

## 4. Budget (rules 9 and 11), counted by `SpiritDiveMergeRules.vertices` and checked by the tests and the self-test

| Piece | Vertices | When | Where |
|---|---|---|---|
| ghost mesh | at most **696** (528 Sylph, 576 Gnome, 696 the rest) | only while the clock is inside 0..18.6 (the 22 tick entry and the exit) | only within **32 blocks** of the camera |
| light strips | 3 x 6 x 4 = 72 (standard), 5 x 6 x 4 = **120** (character) | while the spirit flows (clock 7.7..18) | with the ghost |
| contact sprites | 12 (flash, core, ring) | clock 12..20 | with the ghost |
| body cover | 144 (6 parts x 24) | clock 13..21 | with the ghost; the Fuegoleon flame arm is left out |
| inner silhouette | **288**, alpha at most 0.12 | clock above 15, then steady | **full level of detail only** (24 blocks) |

* **Worst case of one diver: 1,260 vertices** (character, at clock 16 to 17: ghost 696 + strips 120 + sprites 12 + cover 144 + silhouette 288); standard worst 1,212 (Salamander, Undine), 1,092 Gnome, 1,044 Sylph. **Steady state: 288 vertices, only at full detail
  (within 24 blocks), 0 beyond.** Before this layer the same dive cost 0 here.
* **Per frame: at most 3 merges** (`MERGES_PER_FRAME`, a `PerfBudget` keyed by the frame counter of `SpiritDiveFrame`; with no frame event every merge is admitted, as in `SpiritDiveFrame`): worst 3 x 1,260 = 3,780 vertices; the
  others skip entirely (so with more than three divers within range, the ones drawn first in the frame show the merge, the rest do not).
* **CPU per diver and frame:** the ghost 174 quads through the shared render type, a few `sin`/`cos` for the path, 5 strips x 7 path samples = 35 path evaluations; **no allocation** (statics for every scratch object, cached
  render types and textures, shared mesh arrays, no per-player map: the state is a function of the dive's own clock). Timed in `/multiverse perf` as `c.spiritdive.merge`.
* **Network and server: none.** No packet, no keybind, no server work, no config the server reads; everything is client side and keyed by the dive clock the server already runs. Per-player state: none (the side is derived from the UUID
  hash each frame), so there is nothing to clean up on logout, death or dimension change. First person: nothing is drawn for the wearer.
* **Entities:** none. **Self-test (CI smoke):** `magic.spirit_dive_merge` (pure; add it to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`, not edited here).

## 5. The option

`nusmp-spiritdive-client.toml`: `spiritDive.merge = true` (default). `false` draws no merge at all. It only changes what this client draws.

## 6. Previews (`docs/previews/spirit_dive_merge/`, harness renders, not game screenshots)

* `merge_<element>.png` (wind, water, fire, earth): per tier the frame strip of the entry (ticks 0, 4, 8, 12, 14, 16, 20, 24, steady 60) and of the exit (ticks 100 to 119 = the clock backwards), in the default REEL look
  (Wind in design A, `SdwV3A`); then the steady state **with the merge, without it** (`spiritDive.merge=false`) and from behind.
* `solo_<element>.png`: the merge **alone** (no look under it, `SdmSolo`) at ticks 2, 4, 6, 8, 10, 12, 13, then 14, 15, 16, 17, 18, 20, 24, then the exit: this is where the story is readable.
* `models.txt`: cubes, quads, vertices and size of every mesh.

What the stills show: the spirit appears to one side (tick 4 to 8), swings round the wearer and closes in on the chest (10 to 13), a flash and a ring at the contact (14 to 15), the body tinted for a moment, then only the faint
silhouette (the silhouette at 12 percent is hardly visible in a still: the steady with / without pair differs by 288 faint vertices, see section 8).

## 7. Honest look at the previews

* In the REEL / A looks the **look's own entry** (a white flash sphere at ticks 4 to 14, spiral ribbons, crossing circles) is bright and drowns the ghost between ticks 10 and 16; the ghost is clearest at ticks 4 to 8 and in the solo
  sheets. That entry is not part of this change (rule 2: the old effects stay). If it should be calmer, that is the look's decision.
* The spirits are boxy (the spirit models are cube models, reduced to their largest cubes): the Sylph is a small figure with a hair bun, the Salamander a lizard, the Undine a column of tail segments with a fluke; they read as
  ghosts of those models, not as the paintings of the anime.

## 8. What could not be verified (honest list)

* Nothing was seen in a running game: no bloom, real lighting, real skins, real depth with other mods, or first person (the merge is not drawn in first person for the wearer).
* The silhouette (12 percent) is meant to be faint; in the harness it is only just visible against a bright scene. If it is too faint or too strong in the game, `INNER_ALPHA` (cap 0.12) is the one number.
* Additive blending and culling are approximated by the harness; the meshes are wound outward (checked by `SpiritDiveMergeAssetTest`), but the culling of the real `AdditiveTypes` render type was not seen.
* The body cover re-draws the player's own six parts once (144 vertices) through `SpiritDiveAnimeShell.pass`, which works with the vanilla model-part path; a mod that replaces the player renderer's part rendering (Embeddium fast path)
  falls back to the vanilla path as the ANIME look does.
* The hover and spiral numbers (radius 27 / 34 px, 1.15 turns) are tuned on stills; the motion between ticks was not watched.
* `SpiritDiveFrame.frame()` is the frame counter the merge budget uses; when no frame event arrives every merge is admitted.
* The Japanese and English wiki pages were not needed (no new canon facts: the spirits and their forms come from `docs/magic/spirit_dives_anime.md` and the mod's own spirit models).

## 9. Rule 12 checklist

New work checked for new versus replacement (section 2); kept behind the option that restores the old behaviour (`spiritDive.merge=false`); shared files only appended to; pure tests plus a self-test; no version, changelog,
README, lang or `.github` change here (the coordinator's). The coordinator has to: add `magic.spirit_dive_merge` to `SMOKE_EXPECT`, and when merging with the Wind builder (who also appends an option to `DiveClientConfig`) keep both lines.
