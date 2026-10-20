# Spatial Magic: the 1.93.0 rebuild

Magic: `MagicType.SPATIAL`, book `book_spatial`, grimoire colour `0xB088FF`. Part of the 1.93.0 "magic rebuild" (brief: `docs/rebuild/magic_rebuild_brief.md`).

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | NEW | `client/aura/SpatialAura` (`Aura.SPATIAL`, appended to the enum): the **Gate Garb**, 8 GeoLite pieces drawn through `client/aura/SpatialArmourKit` and `EquipAnimation` (the Djinn full-equip forge-in / shatter-out) |
| Summon models | NEW | `spatial_gate` (Fallen Angel Gate / Door of Fate frame, `PropKind.SPATIAL_1`) and `spatial_rift` (a standing tear, `PropKind.SPATIAL_2`), `client/prop/SpatialPropPainter`, `prop/SpatialProps` |
| Effects | NEW | `vfx/client/layer/SpatialFxLayer` for `SPATIAL_FX1..3` (appended to `VfxShape`), 9 sprites from `tools/gen_spatial_textures.py` |
| Pages | NEW (appended) | `dimensional_armour`, `gate_strike`, `gate_swap`, `dimension_collapse` |
| Pages | REPLACED (same ids, same place, same mastery bit) | the casts of `space_cut`, `unopening_red_room`, `myriad_black`, `door_of_fate` are now `SpatialWorks` wrappers that call the old cast unchanged and add the rift / gate models and the new effects; every page also puts the Gate Garb on the caster for 5 s |
| Pages | KEPT | `domination`, `fallen_angel_gate` (gate pair, momentum, hop) exactly as before; the gate now also stands a `spatial_gate` frame at each gate |
| Old effects | KEPT | `SpatialPortalLayer` (the 3D shader portal), `SPATIAL_RIFT`, `SPACE_PORTAL`, `SPACE_CUBE` stay registered and are still what draws the gate itself |

## Canon sources and honesty

No network to blackclover.fandom.com was possible from the build environment; the repo's `.github/wiki` has no Spatial page. The look and spells follow what the repo
already attributed (Finral Roulacase's gate magic and Langris Vaude's folded space: Fallen Angel Gate, Spatial Cut, Unopening Red Room, Myriad Black, Door of Fate) and
general knowledge of the anime. The **Gate Garb** is an original design: pearl-lilac plates with gold and violet void and portal rings, inspired by Finral's gate imagery; it is
NOT a verified canon armour. The four new pages are adaptations ("nearest honest version"), not 1:1 canon spells.

## Armour pieces (tools/gen_spatial_geo.py; Space.PLAYER; anchored on the model part each was built for)

| Piece | Anchor | Content | Delay / fly-in (model px) |
|---|---|---|---|
| `spatial_chest` | body | pearl cuirass, gold-framed portal disc on the breast, void belt with two hip gates, void collar | 0 / from the front |
| `spatial_helm` | head | pearl circlet, gate jewel on the brow, hood panel, a portal ring at each ear | 2 / from above |
| `spatial_arm_r` / `_l` | arms | standing portal ring beside the shoulder on a pearl cap, void vambrace with gold bands, pearl gauntlet with a gate disc (5 px wide: covers slim and wide arms) | 4 / 5 / from the side |
| `spatial_leg_r` / `_l` | legs | void boot, pearl greave, gold knee ring with a gate disc, thigh band | 7 / 8 / from below |
| `spatial_back` | body | the **gate wheel**: three concentric portal rings (12 / 8 / 8 segments) turning at different speeds, a gate disc, four orbiting rift shards, a void mantle (clip `idle`) | 10 / from behind |
| `spatial_halo` | head | a floating gold portal ring, an inner void ring, four rune blocks (clip `idle`) | 12 / from above |

Render passes (`SpatialAura`): 1 pieces (cutout, translucent only while forging / shattering, glow map additive and full bright straight after each piece);
2 ground gate ring + a counter-turning ring (+ sigil in style 1), additive; 3 glints and falling motes, additive, camera-facing. Style 0 = plain set, style 1 = signature pages (sigil, more motes).
First person: head pieces and halo skipped. About 210 cubes (about 10 000 vertices with the glow pass; the preview prints a soft budget note above 6 000).

## Summon models

* `spatial_gate` (67 cubes): gold 16-segment frame on two feet, a pearl ring and a void ring turning inside, the lens of folded space (four stacked plates + seam), eight anchor runes. Clips `idle`, `open`, `close`. The painter adds the `spatial_fold` swirl in the doorway plane (additive, both sides) and a runic ring. Yaw faces the caster as the gate was cast.
* `spatial_rift` (62 cubes): a 12-slab tear of void with a hot white seam, jagged lips of facet shards, six drifting fragments. Clips `idle`, `open`, `close`; param 1 (collapse) draws it 1.6x with faster fragments.

## Spells (all scale with `EnergyBridge.scale(p)` = 0.9 to 1.5 from EP)

| Page | Tier / cooldown | Numbers |
|---|---|---|
| `domination` (kept) | starter | erase the nearest hostile projectile within 8, else pull the target 4 blocks |
| `fallen_angel_gate` (kept) | mid, cooldown 0 then 45 s | two gates, 20 s, momentum-turning hop |
| `space_cut` | zone | the old Tensura `SPACE_CUT` shot (12 dmg, speed 2.2) + a rift model where it lands + `SPATIAL_FX1` |
| `unopening_red_room` | zone | old room (control gate, `BalanceLaw.controlTicks`, 5 s) + `SPATIAL_FX2` and four gate frames |
| `myriad_black` | mid | old five cuts + five rifts + `SPATIAL_FX1` |
| `door_of_fate` | signature, 2400 t | old gate to spawn + the frame model |
| `dimensional_armour` (new) | mid, 600 t | 20 s Gate Garb: 15 to 35 percent (by `EnergyBridge.modifier`) of incoming hits fall through a gate (cancelled), the rest x0.8; hook in `SpatialWorks.initHooks` via `AttributeEvents.incoming` |
| `gate_strike` (new) | mid | target within 18: 14 x scale space damage, 40 percent armour bypass (`EnergyBridge.armourBypass`), slow II 1.5 s; rift behind the target |
| `gate_swap` (new) | mid, 200 t | swap places with the target within 20; foe slowed 2 s; bosses (`BalanceLaw.isBoss`) are only slowed |
| `dimension_collapse` (new) | signature | at the aim point (26): radius 6 x grimoire size x (0.9..1.0 EP): 2 s pull (0.36 blocks/tick, bosses 0.12), then 22 x scale damage (30 percent armour bypass) |

All damage goes through `GrimoireBook.hurtAs` (the balance law caps it); text through `Component.translatableWithFallback` (`tools/lang/spatial_rebuild.json`).

## Files

`tools/spatial_geo_kit.py`, `tools/spatial_tex_kit.py` (shared by the five rebuilds), `tools/gen_spatial_geo.py`, `tools/gen_spatial_textures.py`; `tools/render_preview/scenes.d/spatial_*.json`,
`tools/vfx_preview/scenes.d/spatial.json`; `book/SpatialBook`, `SpatialWorks`, `SpatialArmourCast`, `SpatialRebuildChecks`, `SpatialPageOrder`; self-test `magic.spatial_rebuild`; unit test `SpatialPageOrderTest`.

## Previews that were run (the only eyes available)

`python3 -B tools/render_preview/preview.py spatial_gate_idle spatial_rift_idle spatial_aura_armour spatial_aura_signature spatial_aura_detail spatial_prop_gate spatial_prop_rift spatial_prop_collapse`
(a geo scene must come first in the run so the models are readable by the aura painter) and `python3 -B tools/vfx_preview/preview.py spatial_cast spatial_cast_big spatial_zone spatial_burst`
(80, 80, 100 and 72 of 400 vertices). Slim and wide arms were both rendered.

## NOT verified

* Nothing was run in a game. The preview tools are re-implementations (stub Minecraft classes); the real lighting, the translucency sort, the shader of the 3D portal under the new gate frame and the first-person camera were not seen.
* The wiki could not be reached; canon names and looks are from the repo and memory (see above).
* The armour has about 10 000 vertices a frame with its glow pass: heavier than the older sets (the preview's soft budget note). Players more than a few blocks away see the same set.
* The gate frame is drawn together with the old 3D portal at the same spot; their depth ordering in game is unchecked.
* The `SpatialWorks` hit hook and the new pages were type-checked only (stub API); the pages' balance numbers are unplayed.

## 1.99.0 armour policy

Spatial Magic has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** (policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour (damage cut, thorns, resistance, constructs, effects) is unchanged. Where the armour carried the magic's look, every cast of a family page now sends a small `SPATIAL_FX2` glow that follows the caster instead (no armour piece is drawn). See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
