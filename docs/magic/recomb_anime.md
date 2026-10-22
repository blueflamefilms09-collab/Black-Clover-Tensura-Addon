# Recombination Magic: the anime look (`visual.recombAnime`)

The owner's request: "Recombination Magic make it better looking like the anime." The reference was Henry Legolant's Raging Black Bull, the giant built from the buildings of a town:
a torso of stacked houses (timber-framed and stone walls, arched windows with warm or dark panes, doors, gables, chimneys, small trees and moss on the roofs), blocky brick fists,
arms of stacked house segments, legs of thick pillars on stone slabs, a horned head with two tower spires and two glowing yellow-green eyes, dust bursting up when it steps, a fiery
orange flash with spark lines when a fist lands.

Nothing in this document was run in the game. What was type-checked, tested and looked at, and what was not, is in section 8.

## 1. NEW / REPLACED / KEPT

Rule 5 check: the golem, the house, the ring and the drill are replacements of the looks the mod already had for the same four props (the four-legged bull, the oak room, the ring of boards,
the mana drill), so they are REPLACED as the default look, but by rule 2 the old models, textures and painters stay and are one config switch away. The dust, the flash and the shake are NEW.

| Part | Status | Notes |
| --- | --- | --- |
| The Raging Black Bull drawn as a humanoid house-golem (`recombanime_golem`, 93 cubes) | REPLACED (look only) | the four-legged bull `recombination_bull` stays in the mod and is drawn when `visual.recombAnime` is off |
| The recombined room drawn as a small timber house (`recombanime_house`, 14 cubes) | REPLACED (look only) | `recombination_room` stays (off = old look) |
| The ring of boards drawn as flying building pieces (`recombanime_ring`, 9 cubes) | REPLACED (look only) | `recombination_halo` stays |
| The Mana Corkscrew drawn as a brick-and-tile spire (`recombanime_drill`, 18 cubes) | REPLACED (look only) | `recombination_drill` stays |
| Client config `visual.recombAnime` (default on) and `visual.recombAnimeParticles` (default on), file `nusmp-recomb-anime-client.toml` | NEW | `client/prop/RecombAnimeOptions` |
| Walk / punch / stomp / fire / assemble / collapse animations, the stride that plants the feet by the distance walked | NEW | the old bull had idle, walk, charge, fire, assemble, collapse |
| Dust bursts under every step and the rise, the orange impact flash with spark lines when the fist lands, thrown bricks, chimney smoke, a small camera shake, all as custom particles | NEW | `vfx/client/RecombAnimeFx`; custom particles (particle/fx) only |
| Dust and bricks at a zone laid out (FX2) and at an impact (FX3) | NEW | `RecombinationLayer.onSpawn` (the layer's drawing is unchanged) |
| Pure rules `prop/RecombAnimeRules`, self-test `magic.recomb_anime`, tests | NEW | section 7 |
| Game rules: hit box, ram, cannons, state word, cooldowns, page ids and order, prop kinds, packets | KEPT | nothing on the server changed except one self-test registration line |
| The four-legged bull, the oak room, the boards, the drill, the 11 armour pieces, the layers and textures of 1.93.0 | KEPT | |
| Lang text of the Bull and the book (`tools/lang/recomb_anime.json`) | REPLACED (text) | three existing keys get new text (the old text still described the armour that policy 1.99.0 stopped using) |

## 2. Canon, honestly

* The wiki page (`blackclover.fandom.com/wiki/Recombination_Magic`) could not be fetched from the session (`getaddrinfo ENOTFOUND`) and no dump of it is in the repo. The look follows the owner's description of the anime frames
  (above) and the repo's own wiki text for Ultra Giant Bull (`.github/wiki/spell_ultra_giant_bull.txt`: the magic house made into an even bigger bull, a cockpit, cannons all over the body). The golem is NOT a copy of a frame:
  nobody here could look at the frames, so every proportion is from the owner's words.
* "Rebuilt armour / stitching / weapons of recombined objects" could not be matched to the wiki. The house worn as armour has been unused since 1.99.0 (no cast puts it on; `docs/rebuild/armour_policy.md`), so its eleven
  pieces were not remade; House Armour's cast still sends its `RECOMBINATION_FX3` glow, and now also dust (section 5). No stitching or weapon effects exist in the mod's Recombination pages, so none were invented.
* Not canon, chosen for the game: the golem walking to foes and ramming (the server's existing behaviour), the cannons (the "fire" state), the shake.

## 3. The golem (`recombanime_golem`, `client/prop/RecombAnimePainter`)

Built by `tools/recomb/gen_recomb_anime.py` (geometry, animations, textures) with the face painters of `tools/recomb/recomb_kit.py`; everything is deterministic (a seed per model and per face), so the files can be redone.
156 px (9.75 blocks) tall at scale 1; the painter draws it at `max(0.3, prop scale) * 0.70`, about 8 blocks at the Bull's usual scale of 1.1 to 1.3, and 1.6 times that in the Ultra form.

| Part | What it is |
| --- | --- |
| Feet and legs | two stone slabs (grass and moss on top), brick pillars with an iron ring, a stone knee platform, a timber-and-stone house segment as the thigh with an arched window; feet counter-rotate so they stay flat |
| Pelvis | a stone platform on dark arches |
| Belly | a plastered timber-frame house with a door and two lit windows, under a clay-tile cornice |
| Chest | a stone base (moss), a timber-framed storey with five arched windows (lit and dark), shutters, an iron collar |
| Shoulders | two gabled roofs (slate on the left, clay tile on the right) with stepped timber gables, moss, a brick chimney on the left that smokes, a small tree on the right that sways |
| Terrace and head | a grassy terrace between the shoulders; a weathered stone head block with a visor face: two slanted slit eyes, yellow-green and glowing (glow map), a mouth seam, a slate brow roof |
| Horns | two TOWERS, leaning outward 20 degrees: stone shafts with glowing arrow slits, a crenellated crown, a four-step slate cone roof and a spike |
| Arms | a tile roof cap (shoulder), a timber house segment with a window, an iron band, a stone elbow with a brick chimney, a second house segment, an iron band, and a fist: one block of running-bond brick with soft shading and four knuckle blocks |
| Textures | 2048 x 512 colour and glow maps, 3.4 texels per model pixel, per-face uvs; faces that cannot be seen (undersides, tops covered by roofs) are not drawn |

Animations (clip, length, when the painter plays it):

| Clip | Length | Played when |
| --- | --- | --- |
| `assemble` | 1.3 s (26 ticks) | the first 26 ticks: legs, body, arms, head rise out of the ground in turn with a small overshoot |
| `stomp` | 0.9 s (18 ticks) | after the rise: the left foot lifts and slams (lands at 0.52 s), a settling dust ring |
| `idle` | 4 s loop | standing: breath, a slow head turn, swaying roof tree, heavy arms |
| `walk` | 1.0 s loop (two steps) | moving: the phase is advanced by the DISTANCE the body really moved (`RecombAnimeRules.advancePhase`), so the feet do not slide; a footfall (dust) at 0.25 and 0.75 of the cycle; the body is lowest when the legs are spread |
| `punch` | 1.4 s (28 ticks = the server's ram state) | the ram: the right arm and the torso wind back, then the fist comes down and forward; it lands at 0.60 s (tick 12), the tick the server's blow lands; recoil |
| `fire` | 0.6 s (12 ticks = the server's volley state) | the cannons: the torso leans back, both arms spread, the head roars up, the chimney kicks |
| `collapse` | 1.2 s (24 ticks) | the last 24 ticks: the head tips and drops, the arms fall away, the body sinks back into the earth |

The fist's landing point, the feet's, the chimney's and the model's height are copied into `RecombAnimeRules` from `tools/recomb/golem_numbers.json` (written by the generator); `RecombAnimeRulesTest`
compares them with the file and the clip lengths with the server's tick constants.

## 4. The other spells

| Page (prop) | Look in the anime mode |
| --- | --- |
| Raging Black Bull, Ultra Giant Bull (`RECOMBINATION_3`) | the golem; the Ultra form grows to 1.6 times over 16 ticks and the eyes stay at full glow |
| Bulwark, Room Swap, Room Rebuild (`RECOMBINATION_1`) | `recombanime_house`: stone foundation, plastered timber walls, arched lit windows, shutters, a door, a clay-tile gabled roof with moss, a chimney that smokes, a small tree; rises with dust, swaps with a squash |
| Plank Snare, the effects drawn through the book (`RECOMBINATION_2`) | `recombanime_ring`: eight loose pieces turning round the point (brick, beam, window frame, shingles, chimney pot, door, stone block, a small tower) |
| Mana Corkscrew (`RECOMBINATION_4`) | `recombanime_drill`: a stone base, an iron collar and eight tapering turning rings of brick and clay tile with glowing seams, a glowing tip; sheds a little rubble |
| House Armour | no armour is drawn (policy 1.99.0); the cast's glow now also bursts dust |
| Zone and impact effects (`RECOMBINATION_FX2`, `_FX3`) | the layer draws as before; on top, dust and bricks burst from the ground (`onSpawn`) |

## 5. Particles (custom, `particle/fx`)

All through `Fx`'s catalog ids, the client particle budget (`PerfBudgets.clientParticles`), the quality level and the camera distance (`FxClientLod`); `particles.custom` off or `visual.recombAnimeParticles` off silences them.
Ids used: `smoke_wisp`, `soot_puff`, `sand_grain`, `earth_chip`, `ash_flake`, `impact_burst`, `shock_ring`, `spark`, `ember`, `flame_tongue` (a test checks they exist in the catalog). No particle type was added.

| Moment | What |
| --- | --- |
| A footfall | a ring of tan smoke wisps and dark soot puffs bursting out, sand grains and a few bricks thrown up (5 to 14, scaled by size and quality) |
| The rise, the collapse | a column of rubble and dust every second tick |
| The fist lands (tick 12 of the ram) | `impact_burst` (orange, with a pale core), a `shock_ring`, 10 to 30 `spark` flying out along the ground as spark lines, embers and tongues of fire, the dust ring and thrown bricks; a small camera shake with distance falloff |
| Chimney | a smoke wisp every 8 ticks, a burst of three on the cannon volley |

`docs/previews/recomb_anime/fx_impact.png` and `fx_footfall.png` show the effect from the real sprites and profiles of the catalog (a re-implementation of the spawn code and the particle update, side view).

## 6. Performance and server rules (CLAUDE.md rule 9)

* Everything new is client side. The server decides every rule; the only change near it is one line in `RecombinationProps.init` registering the self-test. The state the painter keeps (walk phase, last tick) is per prop ENTITY ID in a client table
  capped at 48 golems that forgets one not drawn for 400 ticks; nothing is a static "current player"; `Minecraft.getInstance()` is used only in `vfx/client` and `client/`.
* Common classes (`prop/RecombAnimeRules`, `prop/RecombAnimeSelfTest`) touch no client class (`ServerSafetyTest` passes). The setting only changes looks, so it is a CLIENT config.
* Perf: the golem is 93 cubes, 1792 vertices a pass (3584 with the glow pass; the preview tool's note for a prop is 6000); the house 536 in both passes, the ring 368, the drill 800. Particle counts are capped per event (at most 28 + 30 + 14) and go through the client budget.
  Effects are drawn by the clients near the golem only (a client far away spawns nothing: distance detail 0).
* Hit box: the server's hit box of the Bull (2.2 x 3.0 blocks times scale) is unchanged, so the drawn giant is far larger than its hit box; props are never culled by their box (`MagicPropRenderer.shouldRender` is true).

## 7. Files

New: `tools/recomb/recomb_kit.py`, `gen_recomb_anime.py`, `golem_numbers.json`, `preview_fx.py`; 4 geo models, 4 animation files, 8 textures (`recombanime_*`); `prop/RecombAnimeRules`, `prop/RecombAnimeSelfTest`,
`client/prop/RecombAnimeOptions`, `RecombAnimePainter`, `vfx/client/RecombAnimeFx`; tests `RecombAnimeRulesTest` (12), `RecombAnimeAssetsTest` (7); `tools/render_preview/scenes.d/recomb_anime.json`; `tools/lang/recomb_anime.json`; this file; previews.
Edited (additions): `RecombinationPropPainter` (four one-line dispatches to the anime painters when `visual.recombAnime` is on), `RecombinationLayer` (`onSpawn`), `RecombinationProps` (one self-test line), `NUSMP` (one config registration line),
`tools/render_preview` stubs (`perf`, `AdditiveTypes`, `SafeAdditive`, `EntityRenderDispatcher.distanceToSqr`: the preview tool no longer compiled against the 1.99.0 performance classes and now does).
Self-test: `magic.recomb_anime`.

Redo: `python3 -B tools/recomb/gen_recomb_anime.py` (models), `python3 -B tools/recomb/preview_fx.py` (particle preview), `python3 -B tools/render_preview/preview.py recomb_anime_golem_idle recomb_anime_golem_big recomb_anime_golem_walk recomb_anime_golem_punch recomb_anime_golem_punch34 recomb_anime_golem_stomp recomb_anime_golem_fire recomb_anime_golem_assemble recomb_anime_golem_collapse recomb_anime_house recomb_anime_ring recomb_anime_drill`.

## 8. Checked and NOT verified

Checked: `geo_check` (4 models OK); the type check of the new and edited Java (0 errors in them, a deliberate wrong call was caught); JUnit `RecombAnimeRulesTest`, `RecombAnimeAssetsTest`, `GeoAssetsTest`, `ConfigFileNamesTest`,
`ServerSafetyTest`, `SafeAdditiveSourceScanTest`, `SafeAdditiveTextureScanTest`, `RecombinationRulesTest` (39 pass); previews of every model and clip rendered through the repo's `render_preview` tool (the real `GeoDraw` and `GeoModelData` over stubs) and looked at, iterated until the golem read as a house-golem
(the clip signs of the torso and head were wrong in the first render and were fixed from the previews).

Not verified:
* Nothing ran in the game. The previews are a software rasteriser over stubs: no real lighting, mipmaps or render types; the glow pass in the previews is always full white, in game it pulses with the painter.
* `RecombAnimePainter`, `RecombAnimeFx` and the layer hook were type-checked only; the walk-phase bookkeeping, the tick events, the camera shake and the particle spawns have not been seen. The particle preview is a re-implementation.
* The proportions come from a verbal description of the anime frames, not from the frames; the owner should compare the golem with them. The brick fists are cubes (the anime's are blocky but a little softer).
* The server's hit box and ramming circle (3.6 x scale) are those of the old four-legged bull: the drawn fist lands about 4.5 blocks ahead, the ram hits within 3.6 x scale of the golem's centre, so against a foe at the edge of that circle the fist lands slightly past or short of the foe.
  The cannon beams still start at the old height (about 2 blocks) while the golem's shoulders are about 5 blocks up; moving them is a server change and was left alone.
* The dust clouds are smoke wisps and soot puffs: the catalog has no round dust-cloud sprite, so they read as wisps rather than the anime's billowing clouds. A new `dust_cloud` particle type (shared registry) would fix that.
* The ring and the drill sizes (scaled 0.65 and 0.9) were chosen by eye against the old models.
* `visual.recombAnime` is read every frame; switching it in game needs no reload, but the config screen text and lang were not looked at.
