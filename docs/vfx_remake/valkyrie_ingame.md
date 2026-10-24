# Valkyrie armour v2 and the Valkyrie Drill Lance, built into the game

Owner decision "go" on the finished previews of `valkyrie_wings.md`: (1) the Valkyrie armour as a player render layer with three looks, (2) the merged drill lance AB drawn by the layer, in its own quad pool,
(3) the lance as a real weapon item, (4) the arm pieces in first person. Nothing here could be run in a game: see section 9. Version, changelog, README, LIBRARIES, `en_us.json` and `.github` are untouched;
the lang text is the fragment `tools/lang/valkyrie_armor.json`; the CI smoke prefix is **`magic.valkyrie_armor`**.

## 1. NEW / REPLACED / KEPT (rule 5)

* **NEW:** `client/mode/ModeArmorValkyrieV2` (the layer), `ModeArmorValkyrieWorld` (what the game says about the wearers), `ModeArmorValkyrieEvents` (first person + Mermaid leg hiding), `ModeArmorValkyrieConfig` (three options);
  pure `book/ModeArmorValkyrieRules` (options, levels, piece table, quad counts), `ModeArmorValkyrieMesh` (loader + baker + wing flutter), `ModeArmorValkyrieBudget` (admission), `ModeArmorValkyrieSpin`,
  `ModeArmorValkyrieSelfTests`; item `valkyrielance/ValkyrieDrillLanceItem`, `ValkyrieLanceRules`, `ValkyrieLanceBoot`; `WeaponRenderer` drill feature; assets `valkyrie_meshes/` (66 mesh files + `pieces.json`),
  `textures/entity/mode/valk2_atlas.png` (256 x 256) and `valk2_scales.png`, the lance's mesh, skins, icon, models and rows; generators `tools/gen_valkyrie_ingame_assets.py`, `tools/gen_valkyrie_lance_model.py`;
  tests; the harness scenes; the previews `docs/previews/vfx_remake/valkyrie/ingame_*.png`.
* **REPLACED:** nothing is removed. The v2 look replaces the old overlay only while `modeArmor.valkyrieLook = V2` (the default); in that state the old overlay (and the worn-library 3D pieces of the mode) are not drawn.
  The Water Spirit Dive skips six of its pieces while V2 is on (section 6).
* **KEPT:** the old overlay `ModeArmorLayer.valkyrie()` exactly (CLASSIC), the old Valkyrie `valkyrie_lance` texture, the old dive pieces, all preview files and generators of the earlier rounds, the lances A, B and C (preview only).

## 2. Settings (CLIENT config `nusmp-modearmor-client.toml`, group `modeArmor`, next to `windLook`; only change what this client draws)

| Option | Values | Default |
|---|---|---|
| `valkyrieLook` | `V2`, `CLASSIC` (the old overlay, exactly) | V2 |
| `valkyrieForm` | `VALKYRIE`, `SAINT` (feathered wings, halo, winged helm, gold sash), `MERMAID` (scaled tail, shell top, horn; the vanilla legs hidden while drawn) | VALKYRIE |
| `valkyrieLance` | `OFF`, `ATTACHED` (hip; beside the tail in the Mermaid look), `FLOATING` (beside the right shoulder, bobbing) | ATTACHED |

The lance is also left off while the wearer holds the Valkyrie Drill Lance item (it would double). No server config, no packet.

## 3. How the armour is drawn (pattern of `ModeArmorWindV2`)

Meshes are baked once per piece / level / copy into the space of the player-model part (px, y down, front -z, pivot at the part: body, head, arm `x - 5, 22 - y`, leg `x - 2, 12 - y`), the right copy of a paired piece is
a baked mirror (quad order reversed), uvs are inset into the 256 x 256 atlas (nearest sampling never reads the next cell), flat plates sample a white cell with a toon tone baked from the normal, quads are sorted into
four passes. Per wearer the layer captures one `PoseStack.Pose` per part (body, head, both arms, both legs, lance), then draws **opaque** (`entityCutoutNoCull`, atlas), the **tail's scales** (own sheet, Mermaid only),
**translucent** (`entityTranslucent`) and **additive** (`SafeAdditive.additive`): 4 buffer switches, 5 with the tail. Only wing vertices do per-frame maths (flap about the shoulder hinge, bend towards the tip, sweep, a wave
travelling along the veil; a 4096-entry sine table, no allocation). Light: opaque passes use the wearer's light, translucent a copy lifted to block level 9, additive full bright. The grow-in / fade-out is the alpha of the
translucent and additive passes and the wing amplitude.

## 4. Cost (rule 11)

Own pool: **1200 mesh quads (4,800 vertices) per frame for all armour wearers together** (separate from the Water BC 1200); 720 on MEDIUM and 360 on LOW effect quality. Plan once a frame (first wearer drawn, or the leg query):
one pass over `level.players()` (indexed loop), the 16 nearest wearers, admission nearest first, each tries its best level for its distance (NEAR to 16 blocks, MID to 32, FAR to 64, nothing beyond) with the lance
(NEAR and MID only), then without, then the next level down; the first that fits the running total is taken, else not drawn. A wearer outside the plan is admitted on the spot under the same pool. A wearer drawn twice under one clock
(a paused game) triggers a new plan.

| Per wearer, quads (vertices) | NEAR | MID | FAR |
|---|---|---|---|
| VALKYRIE | 692 (2,768) | 344 (1,376) | 174 (696) |
| SAINT | 512 (2,048) | 296 (1,184) | 170 (680) |
| MERMAID | 343 (1,372) | 177 (708) | 117 (468) |
| lance AB (any look) | +312 (+1,248) | +179 (+716) | 0 |
| VALKYRIE with lance | 1,004 (4,016, measured in the harness) | 523 | 174 |

Worst case with N wearers (VALKYRIE, lance on, pool 1200): all within 16 blocks: 1 x 1,004 + 1 x FAR 174 = 1,178 quads, the others are not drawn; all at 16 to 32 blocks: 2 x 523 = 1,046, the rest not drawn;
all at 32 to 64 blocks: 6 x 174 = 1,044, the rest not drawn. Whatever N, at most **1,200 quads / 4,800 vertices / 5 buffer switches per admitted wearer** (at most ~6 wearers) per frame. Per-frame CPU: wing vertex maths of the
admitted wearers (a NEAR VALKYRIE wearer: 912 wing vertices; SAINT 400), the rest is copying baked arrays. Allocation per frame: none (static arrays, poses, scratch quaternion; the pose stack's own pushes as in every layer).
Spin state: 16 slots, pruned each plan. First person: at most 69 quads of one arm outside the pool (only the local player). Network: 0 packets. Server: 0 work for the armour; the lance item's hit burst is capped at
8 drops per burst, one burst per attacker every 4 ticks and 4 bursts a tick over the server (32 particles per tick worst), sent as ordinary particle packets to the players near the spot. `PerfSourceScanTest`'s hot-file list now includes the three client classes.

## 5. The lance

Armour lance (`ATTACHED` / `FLOATING`): the rigid mesh AB at the hip, aimed by a precomputed quaternion, turned about its own axis by a per-wearer integrated angle: idle 60 degrees a second, up to 720 while the wearer swings,
uses an item or casts (`DiveCast`), ramping up in 6 ticks and back to idle in 10. `ATTACHED` is the preview's pose **flattened** (down 0.22 instead of 0.62) so a standing wearer's lance does not go into the floor; in the Mermaid look
it lies beside the tail pointing back. `FLOATING`: right shoulder, 17 px out, forward, 0.8 px bob.

Item `valkyrie_drill_lance` (creative tab only; no loot, recipe or trade): a `SwordItem` with a trident's numbers (iron tier, +6 modifier = 9 shown, speed -2.9, 250 uses), a capped water-drop hit burst, no throw.
Engravings in `item/WeaponEngravings`: `barrier_piercing` 3, `elemental_boost` 2, `crushing` 1, `magic_weapon` 1 (all four are in `tensura_enchantments.txt`, the list of the real `#tensura:engraving` ids). Class `spear`
(both `weapon_classes.json`), grip fraction 0.25 (`weapon_grips.json` via `grip_table.py` OVERRIDES; the mesh also states `gripxyz` at the guard so the length of the trailing ribbons does not move the hand), grip golden refreshed,
Better Combat `bettercombat:spear` (+1 reach), franchise `black_clover`, display preset, 16 x 16 diagonal-drill icon drawn by the script, model json + icon json, lang fragment.
Model (`tools/gen_valkyrie_lance_model.py`, hooked as `gen_weapon_models.py valkyrie_lance`): the AB mesh (312 quads) with `part grip` (grip, guard, trailing ribbons: rigid, 36 quads) and `part drill` (cone, foam mane,
crystal screw, ribbons round the cone, needle, tip glow); `orient manual`; 3.0 m in hand. Skins 128 x 128: body (foam cards + flat tone swatches), glow (veil, tip glow, glass tones, glowing axis; additive, so the crystal and the ribbons
shine instead of being translucent). `client/WeaponRenderer`: a mesh with a `part drill*` line keeps the index of its first drill vertex per layer; `emitLayer` draws the quads after it after a rotation about +y by
`ModeArmorValkyrieSpin.itemAngle(game time + partial, charge)`, charge = the holder's use time or swing time (the holder from `HolderTracker`), a pure function of the clock: no state, no packet, one quaternion rotation per held lance.
A mesh without a drill part takes the old path.

## 6. First person, the Water dive, the Mermaid legs

* **First person:** `RenderArmEvent` (empty hand, pose already at the arm) and `RenderHandEvent` (a hand with an item: the arm placement of `ItemInHandRenderer.renderPlayerArm` redone), the same two hooks and the same placement as
  `FireArmFirstPerson` and the Djinn gauntlets (both names exist in this repo's NeoForge 1.21.1 and are in the API stubs). They draw the sleeve, shoulder cap, cuff and (VALKYRIE look) sleeve veil of the arm being drawn. The ear discs are skipped.
* **Water Spirit Dive:** `SpiritDiveAura.drawPieces` skips `water:wing_r`, `water:wing_l`, `water:helm`, `spiritdive:saint_wing_r/l`, `spiritdive:saint_halo` while the V2 layer is on for that diver (`replacesAuraPieces`).
  The old plates, skirt, pauldrons, vambraces, gauntlets, greaves, tail and backdrop of the dive stay: they overlap the new armour (a decision for the owner, section 10).
* **Mermaid:** `RenderLivingEvent.Pre / Post` (after the player model's own properties are set) hide `leftLeg`, `rightLeg` and both trouser layers of a wearer whose tail is drawn and put them back; worn leggings / boots are drawn by
  the armour layer as usual and still show.

## 7. Tests (run here with the repo's JUnit shim; Gradle was not available)

`ModeArmorValkyrieTest` (options, levels, quad counts of every look = the preview numbers, admission examples and a randomised crowd under every pool, the spin, every mesh file bakes to the table's quads, finite, unit normals, uv inside the
cell, passes, mirror, wing weights, flutter identity at rest), `ModeArmorValkyrieAssetTest` (the code's piece table = `pieces.json`, atlas, files, the lance's files and rows), `ModeArmorValkyrieScanTest` (server safety, no allocation per
frame, one plan a frame, wiring), `ValkyrieLanceRulesTest`, the in-game self-test `magic.valkyrie_armor` (`ModeArmorValkyrieSelfTests`: every mesh in the jar and baking, a crowd of 16 under all three pools, assets, options, lance
registration / trident damage / engravings once / capped burst, spin cap). The sweep of section 8 also ran.

## 8. Previews (`docs/previews/vfx_remake/valkyrie/`)

`ingame_full.png`, `ingame_wings.png`, `ingame_upper.png`, `ingame_saint.png`, `ingame_mermaid.png`, `ingame_lance_modes.png`, `ingame_run.png`, `ingame_ages.png`: BEFORE (the old overlay, `valkyrieLook = CLASSIC`) | NEW, rendered by the repo's headless harness
(`python3 -B tools/mode_armor_valkyrie_scenes.py`, `preview.py` with the scene names, `python3 -B tools/mode_armor_valkyrie_sheets.py`), which runs the **real** `ModeArmorLayer` and `ModeArmorValkyrieV2` (meshes, plan, wings, lance, spin) against stubs of the
world (stubs added: `ModeArmorValkyrieWorld`, and `WornLibrary` / `WornLibDraw`, which the harness lacked since the layer started to call them). `ingame_weapon.png`: the weapon item in the item preview (`tools/vfx_preview/valkyrie_ingame_weapon.py`; `preview_weapons.py` got an optional spin and camera target).

## 9. What was not verified in a game (honest)

Nothing was run in Minecraft: not the layer, not the events, not the item, not the config file. Software renders have no bloom, no real light, approximated additive, nearest-sampled 64 x 64 procedural sheets, a block stand-in with hanging arms.
Unverified: that the first-person arm placement lines up with the vanilla arm of a skin (copied from two existing hooks); that `RenderLivingEvent.Pre` hides the legs before the body is drawn on every skin and with other mods; the
look of the toon tones under the entity shader's own lighting; the lance clearing the floor in every pose (sneak, swim, elytra, riding); the weapon spin in hand (first person scale of a 3 m weapon comes from the display preset, which is applied but
was not seen); that `Fx.spray` water drops look right; the NeoForge `ModConfigSpec` group (compiled against stubs only). The type check is against the API stubs (negative control run); Gradle / `build` was not run.

## 10. Decisions for the owner

1. The old Water dive plates (chest, skirt, pauldrons, vambraces, gauntlets, greaves) still draw under the new armour; only wings, helm and halo are skipped. Skip more?
2. `valkyrieForm` is a client choice for every wearer; following the server's Noelle form (VALKYRIE / MERMAID / Saint dive) per wearer would need a packet or the existing form state: not done.
3. Item "charging": the lance has no right-click use, so only swinging (and use of other items by the armour wearer) spins it up.
4. The rest of the hold-state: other players' lances idle-spin; their swing spins up through `HolderTracker`.
