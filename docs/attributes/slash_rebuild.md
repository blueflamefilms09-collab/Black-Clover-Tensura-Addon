# Slash Magic: the 1.93.0 rebuild

Magic: `MagicType.SLASH`, book `book_slash` ("Severing Magic" in the VIZ release), grimoire colour `0x48FF7A`, canon user Jack the Ripper. Part of the 1.93.0 "magic rebuild" (brief: `docs/rebuild/magic_rebuild_brief.md`).

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Temporary armour | NEW (replaces the empty stub `SlashAura`) | `client/aura/SlashAura` (`Aura.SLASH`): the **Ripper's set**, 8 GeoLite pieces via `SpatialArmourKit` and `EquipAnimation`, with a forearm blade on each arm and the Death Scythe slung across the back |
| Summon models | NEW | `slash_crescent` (projected slash), `slash_scythe` (Death Scythe), `slash_ghost` (Ripper Dash afterimage) |
| Prop painter | REPLACED | `client/prop/SlashPropPainter` draws the geo models for `PropKind.SLASH_1` (param 0 crescent, param 1 afterimage) and `SLASH_2`; the old procedural drawing `ModeledMagicProps.slash` stays in its file, unused |
| Prop behaviour | KEPT + one branch | `prop/SlashProps`: SLASH_1 still flies 1.15 blocks/tick; param 1 (afterimage) stays put |
| Effects | NEW layer | `vfx/client/layer/SlashRipFxLayer` takes `SLASH_FX1..3` over from the old `SlashLayer` (kept untouched and still registered before it; the preview pulls helper files by name stem, and `SlashLayer`'s stem also matches the shared `SlashCompassMercuryLayer`, which cannot compile alone). 7 sprites from `tools/gen_slash_textures.py` |
| Old effects | KEPT | `SLASH_WAVE`, `SLASH_BLADES`, `SLASH_SCYTHE` (shared `SlashCompassMercuryLayer`) are no longer called by the book but stay registered |
| Pages | KEPT (ids, order, mastery bits) | `slash_wave`, `forearm_blades`, `ripper_dash`, `death_scythe`; adaptive cutting (`SlashBook.adapt`, 10 marks) untouched |
| Page casts | wrapped | each cast calls the same code as before, with `SLASH_FX1..3` instead of the old shapes, and forges the Ripper's set for 5 s (the forearm blades page: for its whole 20 s, replacing the old `SLASH_BLADES` follower) |
| Pages | NEW (appended through `SlashExt`) | `lunatic_slash`, `adaptive_edge` |

## Canon sources and honesty

Offline: no wiki access. The repo's `.github/wiki/slash_magic.txt` and `spell_death_scythe_lunatic_slash.txt` are the canon used: mana blades that cut anything, long blades from the forearms for close combat, projected slashes for range, blades that adapt to what they cut including space, healing and gravity magic; spells Death Scythe and Death Scythe: Lunatic Slash ("a chaotic flurry of flying slashes"). The armour is an original design (gunmetal-black slat plates, green edge, bladed cowl, scythe on the back), not a verified canon outfit; the afterimage and the sweep rings are original presentation.

## Armour pieces (tools/gen_slash_geo.py)

| Piece | Anchor | Content | Delay / fly-in |
|---|---|---|---|
| `slash_chest` | body | slat plates three rows deep with green slash scars, bandolier with a gem, a ragged skirt of downward blades | 0 / front |
| `slash_helm` | head | serrated mouth guard (eyes free), three swept blade fins, blade ears, brow band | 2 / above |
| `slash_arm_r` / `_l` | arms | spiked angular pauldron, vambrace, gauntlet, the **forearm blade**: an 18 px green-edged serrated blade on the outer side of the arm from the elbow past the hand, ending 2 px above the soles | 4 / 5 / sides |
| `slash_leg_r` / `_l` | legs | greave with a back fin, knee spike, toe blade | 7 / 8 / below |
| `slash_back` | body | the Death Scythe slung across the back, cape tatters, six blade shards orbiting (clip `idle`) | 10 / behind |
| `slash_halo` | head | a saw-ring of 10 blades and 10 teeth standing behind the head, turning (clip `idle`) | 12 / behind |

Passes: pieces (cutout / translucent while forging) + glow maps; serrated ground ring(s) (`slash_rip_ring`, additive); flashing rip marks round the body and glints (additive, camera-facing). Style 1 (Death Scythe, Lunatic Slash) adds a second ring and more marks. About 240 cubes (about 11 700 vertices with glow): above the preview's soft budget note.

## Summon models

* `slash_crescent` (46 cubes): 18 segments on an arc with a green edge strip and nine back spikes, tilted 18 degrees, clips `idle`, `fly` (turning). Painter: a speed streak behind and a glint at its heart.
* `slash_scythe` (48 cubes): shaft, boss and a 20-segment curved blade; clip `sweep` (a full turn in 0.6 s) then `idle`. The scale is the reach (radius / 4); a serrated ground ring shows the reach.
* `slash_ghost` (25 cubes): a hollow humanoid of translucent green glass leaning into the dash with blade arms and a trail; drawn translucent, fading over its life.

## Spells (EP scale = `EnergyBridge.scale`)

| Page | Numbers |
|---|---|
| `slash_wave` (kept) | piercing bolt, radius 1.2, speed 2.4, 8 x scale, adaptive cut |
| `forearm_blades` (kept) | 20 s, aura 2 percent then 1 percent a second; melee +5 x scale and adaptive cut |
| `ripper_dash` (kept) | 10 blocks through foes, 7 each, aura 2.5 percent; afterimages along the path (up to 7, every 1.6 blocks) |
| `death_scythe` (kept) | radius 7 x size, 15 each after 5 ticks; sweep field + crossed burst |
| `lunatic_slash` (new, signature, 700 t) | ten piercing crescents in 20 ticks over an 80 degree cone, 6 x scale each, adaptive; aura 3 percent (floor 10) |
| `adaptive_edge` (new, mid, 240 t) | three adaptation marks at once (cap 10), `Nullification.shatterBarriers` within 5, magic jammed 4 s (bosses 2 s, half the chance), a 6 x scale cut |

Damage via `GrimoireBook.hurt` (balance law). Lang: `tools/lang/slash_rebuild.json`.

## Files

`tools/gen_slash_geo.py`, `tools/gen_slash_textures.py`, `tools/render_preview/scenes.d/slash_*.json`, `tools/vfx_preview/scenes.d/slash.json`; `book/SlashBook`, `SlashWorks`, `book/ext/SlashExt`; self-test `magic.slash_rebuild`.

## Previews that were run

`preview.py slash_crescent_idle slash_scythe_idle slash_ghost_idle slash_prop_wave slash_prop_afterimage slash_prop_scythe slash_aura_armour slash_aura_detail`; `vfx_preview slash_cast slash_cast_big slash_zone slash_burst` (64, 48 and 64 of 400 vertices).

## NOT verified

* Nothing was run in a game; previews are re-implementations. The spin of the crescent in flight, the sweep timing of the scythe against the delayed damage tick, and the afterimages in motion are unseen.
* The forearm blades run 6 px past the hand and end 2 px above the soles when the arms hang; with the arm raised they follow the swing, but crouch poses (the whole body drops) were not checked against the floor.
* Heavy vertex count (see above); canon: no wiki access.
