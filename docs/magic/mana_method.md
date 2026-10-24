# Mana Method (Magic Formula): rune constructs, Homing and Growth, True Magic blocks

Builder `manamethod`. Spec: the owner's master prompt, section 1 (Mana Method). The audit that decided what is new is `docs/magic/manamethod_audit.md`.
Self-test prefix for the smoke list: **`magic.mana_method`** (two tests: `magic.mana_method` and `magic.mana_method.true_blocks`). Server setting: `serverconfig/nusmp-mana-method-server.toml`.

## What is built, in plain words

1. Five new grimoire pages, appended last to every grimoire (after the spirit pages), late-game like the Mana Zone: **Flame Rune, Water Rune, Wind Rune, Earth Rune, True Magic Rune**.
   No key is bound: they are ordinary pages, cast through the grimoire's own hold, release, cost and cooldown.
2. Casting a page forms a **rune array** 2.6 blocks in front of your eyes, facing you. It draws itself for 18 ticks (26 for True Magic, +4 when two runes are written),
   then ONE bolt leaves the rune's centre in the direction you look at that moment. The rune is not an entity: it is a small server record (audit row 4).
3. **Rune modifiers**, carried in the spell's data (`RuneSpec`, written to a tag by `RuneSpecNbt`, one packed int in the packet):
   * **Homing** (pursue): when the bolt leaves the rune it picks ONE foe in a 60 degree cone (one query per cast) and turns toward it by at most 0.22 rad per tick for at most 36 ticks.
   * **Growth** (grow): the hit radius grows 0.09 per block flown (x3 at most) and the damage 3.5 % per block (x1.8 at most).
   * Fire and Earth carry Growth, Water and Wind carry Homing. **Sneaking while you cast writes both runes**: 85 % of the power, 4 more ticks of charge.
4. **True Magic Rune** (your grimoire's natural attribute: fire, water, wind, earth, lightning, plant; Stage 3 control, like the older True Magic page):
   * the bolt **ignores magic resistance**: the target's Tensura `MAGIC_RESISTANCE` is read (a fraction, or a percent above 1) and its cut is undone, at most x3 (x1.5 for a boss); the balance law's per-hit caps still hold;
   * devils take x1.6 (x1.3 boss) and dense mana adds up to +35 %, the rules of `TrueMagicRules`; the environment pays back part of the cost, as on the older page;
   * on impact it places **real blocks** (see below).
5. **`RuneGeneratorUtil`** (pure maths, JOML: the plane that faces the caster) and **the written rune** (`RuneAlphabet`, `RuneGlyphLayout`, the vfx layer `RuneWritingLayer`, the client holder `RuneClient`): the rune is drawn as real rune symbols written in the air, with the mod's own open grimoire hovering beside the caster (see below).

## How a cast reads (flow)

`page cast` (server) -> `RuneManager.begin` (switch, per-caster cap, global cap; refused before anything is spent) -> `RuneNet` packet to players within 64 blocks (28 bytes) ->
clients draw the rune over `charge` ticks -> `RuneManager.tickAll`: charge over -> `launch` (look, Homing target) -> `fly` each tick (move, block ray, budgeted entity sweep) ->
`impact` (primary + splash up to 6 foes through `GrimoireBook.hurt` = balance law, rider, VFX, particles, True blocks) -> the cast record is removed.
A wall hit explodes (splash share 50 % for everyone, the radius grows with Growth); running out of range or time fizzles (a puff, no damage).

## The pages (name, description, tier, cost, cooldown)

| Id | Name | Tier | Cost | Cooldown | Default rune | What it does |
|---|---|---|---|---|---|---|
| `mana_method_fire` | Flame Rune | mid, late game | 12 % (floor 300) | 160 ticks | Growth | a growing bolt of flame; sets the foe alight (short) |
| `mana_method_water` | Water Rune | mid, late game | 12 % (floor 300) | 160 | Homing | a pursuing bolt of water; slows (short, through the control caps) |
| `mana_method_wind` | Wind Rune | mid, late game | 12 % (floor 300) | 160 | Homing | a pursuing gale; lifts what it strikes |
| `mana_method_earth` | Earth Rune | mid, late game | 12 % (floor 300) | 160 | Growth | a growing stone; slows what it strikes |
| `mana_method_true` | True Magic Rune | zone, late game | 20 % (floor 600) | 400 | Growth (sneak: both) | the real element: resistance ignored, real blocks, devils weak |

Tiers, costs and the cooldown bounds are those of the 15-spells recipe (`pages15_recipe.md` section 3: mid 12 % / 300 / 160, zone 20 % / 600 / 300..1400). `Pages15Kit` itself does not
fit: it builds FAMILY pages of one grimoire, gated by mastery, while these are universal pages (like the Mana Zone's), so the plain `BookPage` API is used and the gate is the
Mana Zone's (`ManaZoneManager.gateOpenFor`, six requirements) plus the setting. The Heart kingdom's -5 % cost perk applies to them automatically. Descriptions: `tools/lang/mana_method.json`
(`nusmp.mana_method.page.<id>.desc`), merged by the coordinator. The ids contain no attack word, so an Avatar never casts them (tested). The pages are built outside `book/`, so
`ArmourPolicyTest` does not see them and **no new-pages file is needed**; they are not armour pages.

## True Magic blocks (`TrueBlocks`, `TrueBlockRules`)

| Element | Real blocks | Count |
|---|---|---|
| Flame | fire (only with the game rule `doFireTick` and the setting `placeFire`) on solid ground | 5 |
| Water | **water source** blocks | 5 |
| Earth | stone mound on the ground | 7 |
| Plant | moss carpet on the ground | 7 |
| Wind, Lightning | none (not blocks) | 0 |

Protected-area checks, as the existing code does them: only loaded chunks (nothing is ever loaded), only into free cells (air, or a replaceable block with no fluid), only where the
caster `mayInteract` (spawn protection, world border), never in the Labyrinth or a world the Dungeon Guard holds. Caps: 7 per cast, 21 standing per caster, 160 in the world.
Every block is taken back after `blockSeconds` (30 by default, 5 to 300), and only if it is still the block that was placed; all of them when the server stops. The ledger is memory
only: a crash right after a cast leaves those few blocks in the world.

## The written rune (the only look)

The owner's reference frame (`tools/mana_method/reference_frame.png`) shows dense rings of readable rune symbols (bright yellow core, orange-red edge), a tight ring round the hands, a bigger tilted outer ring, thin glowing circle lines,
loose glyphs above, yellow lightning and an open glowing grimoire. The owner's clarification: **actual rune symbols written in the air**. This is now what the game draws; it REPLACES the dot rings (no setting, no second look).

Previews (`docs/previews/mana_method/`, drawn through the game's own path: the real `RuneWritingLayer`, the real `VfxVertexBuffer` and the real textures, rasterised by `tools/vfx_preview/preview.py`):
**`rune_ingame.png`** (the owner's reference frame next to the rune at the same framing), **`rune_glyph_rings.png`** (reference, over the shoulder, and from the caster's eye, face on), `rune_glyph_charge.png` (the 18-tick charge and the release),
`rune_glyph_lod.png` (the four levels of detail and the six elements), `rune_glyph_writing.png` (8 frames of ONE glyph being written, then the ring), `rune_glyph_alphabet.png` (all 32 glyphs at 4x). Make them with
`python tools/mana_method/preview_ingame.py` and `python tools/mana_method/preview_glyph_rings.py`. The headless preview cannot draw the item model, so the grimoire shows only as its gold glow there.

### Pieces

* `mana/method/RuneGeneratorUtil.java` (pure): `Params` (`paramsFor(spec)`: radius 1.15 to 1.8, complexity 2 + modifiers + 1 for True Magic), `faceQuaternion(look)` (a JOML quaternion `rotateY(yaw).rotateX(pitch)` that takes the plane's normal to
  **minus the look vector**, no roll, so symbols stay upright), `frame` / `frameFacing` (the plane's two axes, computed once per rune), `distanceDetailSq`, and the client constants `MAX_RUNES_DRAWN = 6`, `CULL_DISTANCE = 64`, `LOD_NEAR = 24`, `BOOK_DISTANCE = 32`.
* `mana/method/RuneAlphabet.java` (pure): 32 hand-authored runes of 2 to 5 straight strokes on a 5x5 lattice, in the angular Elder-Futhark style of the reference; `phrase(element, trueMagic, i)` gives each element its own six words
  (True Magic replaces the first and fourth by gebo and othala). Pinned by `RuneAlphabetTest` (32 distinct names, no glyph equal or mirrored to another, at least 3 lattice units tall or wide, at most 5 segments, two elements share at most two words).
* `mana/method/RuneGlyphLayout.java` (pure): the whole picture as numbers, filled into one preallocated `Layout`. **Handwriting**: a glyph is written by a PEN that walks its strokes at a constant speed; every segment up to the pen is ONE strip quad in the
  caster-facing plane, the last one cut at the pen, a glow quad on the pen; brightest (1.7x) right behind the pen, cooling over 3 ticks, then breathing (+-12 %). **Reads right**: a glyph is written in its own frame (right = clockwise tangent of its ring,
  up = outward), so it reads correctly to the caster (tested by strip winding and by fehu's branches for four look directions including straight down).
* **Rings** (tuned to frame 134): the **hand ring** 10 glyphs (radius 0.30 R, +0.09 rad/tick), the **middle ring** 16 (0.58 R, -0.05, counter-rotating), the **outer ring** 24 (0.86 R, tilted 0.5 rad, lifted like a bowl, +0.035); evenly spaced, tangent-aligned,
  glyphs sized to FILL 92 % of the slot so neighbours nearly touch. Each ring has a **thin continuous glowing circle line** under it (a textured quad) and the outer ring a **second thin boundary ring**; 4 floating glyphs above, spread evenly. The rings are written
  at the same time, glyph after glyph (hand ring over 3..48 % of the charge, middle 12..66 %, outer 22..84 %, floating 60..92 %), the lines and arcs come in last; on release the outer ring widens 25 % and flares for 6 ticks.
* **Lightning**: up to 3 arcs of 7 camera-facing strip quads from an outer-ring point to a hand-ring point, a new shape every 3 ticks, from 55 % of the charge.
* **Levels of detail** (`levelFor(detail)`, detail = effect quality x distance, from the manager): full, medium (half the floating glyphs, 2 arcs, no book), low (half the glyphs, 1 arc), minimal (half of the outer ring), nothing beyond 64 blocks.
* `vfx/client/layer/RuneWritingLayer.java` (**new**, shape `RUNE_WRITING` appended to `VfxShape`): ONE vfx instance per rune, registered in `VfxManager`; fills the layout and emits the quads through `VfxVertexBuffer` (ADD blend), so `VfxQuality`, the frame vertex cap, the distance level of
  detail and the manager's `maxActive` apply. The shared cap of 400 vertices per activation is unchanged for every other layer; this layer asks for 1,200 through the new `AbstractVfxLayer.hardVertexCap` (default 400) and `VfxVertexBuffer.beginActivation(limit, hardCap)`. Nothing is allocated while drawing (8 slots cache the frame of each rune alive).
* `client/manamethod/RuneClient.java` (the one client holder, rewritten): receives the existing packet (validated by `RuneNet.valid`), keeps at most 6 runes, spawns the vfx instance, throws sparks, draws the book, forgets everything on logout.
  **Sparks**: `mana_mote` at the pen positions, at most `RuneGlyphLayout.MAX_SPARKS_PER_TICK` = 3 per rune per tick, each batch through `PerfBudgets.clientParticles`, scaled by the distance and `PerfSettings.vfxQuality()`.
  **The REAL grimoire**: the mod's own open grimoire (`GrimoireItem.createUnbound` + `withOpenView(GrimoireCarry.OPEN_STEPS)`, built once per element), drawn through `ItemRenderer.renderStatic(THIRD_PERSON_RIGHT_HAND, FULL_BRIGHT)` with the `GrimoireCarry.HAND` pose, the way
  the floating grimoire is drawn elsewhere, hovering at the caster's side (position from `RuneGlyphLayout.bookPos`), one per rune, only within 32 blocks, plus a gold glow billboard from the layer.
* Textures (`assets/nusmp/textures/particle/`, made by `tools/mana_method/gen_rune_textures.py`): `rune_stroke_{gold,flame,water,wind,earth,lightning,plant}.png` (32x48, bright core and orange-red edge in the tint of the element), `rune_line_ring.png` (128x128, the circle lines),
  `rune_arc_strip.png` (8x16); transparent corners, checked by the texture tests.
* **Client option for writing speed / quality**: not added (it was only to be added if cheap; the speed follows the charge time and the quality follows the existing effect-quality setting, which already drives the levels of detail).

### What was removed (the dot rings)

The old **dot rings** are removed, as the owner asked (the writing runes are the only look): `RuneGeneratorUtil.geometry` / `toWorld` / stride / point-index code and its `BoundedCache` and particle budget constants; the old draw path in `RuneClient` that spawned `mana_mote` and glyph particles
point by point; the tests of that code (the dot tests of `RuneGeneratorUtilTest`); the previews `rune_faces_the_caster.png`, `rune_drawing_sequence.png`, `rune_elements.png`; `tools/mana_method/preview_runes.py` and `DumpRunes.java`; and the earlier stamped random-sprite alternative
(`gen_rune_glyphs.py`, `rune_glyphs.json`, `glyph_atlas/`, `rune_glyph_sprites_alt.png`, which the owner had said was not what was meant).
Kept untouched: the five pages and their ids, the rune record and all server logic (`RuneManager`, `RuneRules`, `RuneSpec(Nbt)`, `TrueBlocks`), the packet `nusmp:mana_method_rune` (same 28 bytes), the config file and keys, the data component / NBT ids, so old worlds and configs load as before.

## Settings (SERVER, `nusmp-mana-method-server.toml`)

`manaMethod.enabled` (true; **false = the five pages are unusable and everything is as before**), `maxPerCaster` (2, 1..4), `maxGlobal` (24, 4..64), `trueMagic.placeBlocks` (true),
`trueMagic.placeFire` (true), `trueMagic.blockSeconds` (30, 5..300). Gameplay settings are server configs; there is no client setting (the drawing follows the existing effect quality).

## Worst case cost (rule 11)

| What | Bound |
|---|---|
| Idle server | one `ACTIVE.isEmpty()` check and one `LEDGER.isEmpty()` check per tick |
| Entities | **none spawned** (no entity type, no lightning entity) |
| Casts alive | 2 per caster, 24 on the server by default; hard ceiling 4 / 64 whatever the config says; each lives at most 30 + 40 ticks |
| Per tick, per bolt | one move, one block ray of at most 1.25 blocks (loaded chunks only), at most one entity query of the last stretch |
| Entity queries | the whole server shares 16 per tick; a bolt denied three ticks in a row queries anyway, so at the hard ceiling of 64 bolts at most 16 + 21 = 37 small queries per tick (24 bolts by default: at most 24) |
| Homing | ONE query per cast, at the launch (a 28 block box); then a lookup by id per tick |
| Packets per cast | 1 rune packet (28 bytes, players within 64 blocks), at most 13 trail packets of 3 particles (one every 3 ticks), 1 impact VFX packet, 1 burst particle packet; all through the particle and effect packet budgets; a caster casts at most once per 8 s |
| Blocks | 7 per cast, 21 per caster, 160 in the world, back after 30 s (max 300 s) |
| Client | at most 6 runes. **Per rune: at most 300 quads = 1,200 vertices** at full detail (typical at tick 18: about 190 quads / 760 vertices; 54 glyph slots, 270 stroke strips, 4 pen glows, 4 circle lines, 21 arc quads, 1 book glow). **Six runes: at most 1,800 quads = 7,200 vertices a frame**, in practice far less (farther runes are drawn at lower levels; `VfxQuality`'s frame vertex cap and the manager's `maxActive` cut the rest). Sparks: at most 3 per rune per tick (18 per tick for six), each batch through `PerfBudgets.clientParticles`. The real grimoire: at most 6 item renders per frame, only within 32 blocks. No allocation while drawing in the layout and the layer. Nothing beyond 64 blocks. Replaces the 166 to 204 dot particles of the old look. |
| Time Magic VFX | untouched |

## Tests

* Pure (`mana/method`): `RuneGeneratorUtilTest` (plane orientation for 60 look vectors incl. straight up and down: normal = minus look, orthonormal right-handed axes, no roll), `RuneAlphabetTest`, `RuneGlyphLayoutTest` (the pen walks the strokes, glyphs read right way round
  in every plane, strokes in the plane and inside the glyph's box, rings of 10 / 16 / 24 evenly spaced and tangent-aligned, circle lines, every count bounded at every level, arcs late and camera-facing, the book at full detail only, deterministic, nothing allocated, the textures exist with
  transparent corners), `RuneRulesTest` (steering, Growth caps, both runes, resistance bypass and its caps, spec packing, caps clamped), `TrueBlockRulesTest`.
* Source scans: `ManaMethodSourceScanTest` (server-friendly: client classes only in `RuneClient`, no keybind, clientbound packet only, clean-up on every exit; no lag: idle path, caps before work, budgeted sweep, no chunk load, permission per cell; **wiring**: the packet reaches `RuneClient`, the layer is
  registered, `hardVertexCap` is passed to the buffer and counted by the frame plan, the shape is appended last, the nine textures ship, the sparks and the book are budgeted, the draw is allocation-free, the dot-ring code is gone, this doc says what was removed; pages last, one config file, Time Magic not referenced).
* In game (CI server smoke, **not run here**): `magic.mana_method` (the layout of every spec at every level and age stays within its quad bound, the 9 textures and the `RUNE_WRITING` shape exist; the five pages are the last of every grimoire, closed for a fresh player, off the Avatar's list; NBT round trip for every spec; caps per caster; despawn by itself; clean-up of a dropped
  caster; a Homing rune strikes a zombie 3 blocks off its line; Growth numbers) and `magic.mana_method.true_blocks` (water source, stone mound, fire; nothing where the caster may not build; wind and lightning place none; caps; everything taken back).
* Staged look: `RuneAlphabetTest` (32 distinct legible glyphs of at most 6 segments, none a mirror image of another, the words of each element) and `RuneGlyphLayoutTest` (the pen walks the strokes and the line only grows, glyphs read right way round in every plane, strokes in the plane and inside the glyph's box, the glow cools behind the pen, rings written at once glyph after glyph hand ring first, each element its own words, every count bounded at every level, arcs late and camera-facing, the book at full detail only, deterministic, nothing allocated), and a scan test that nothing in the game names the staged classes.
* Two pinned lists that assumed the spirit pages are the last of a book now look past the five new pages (`SpiritControlSelfTests`, `ManaZoneSelfTests`, through `ManaMethodPages.tailCount`).

## NEW / REPLACED / KEPT

* **NEW**: the five pages; `mana/method/*` (rules, spec, generator, manager, true blocks, packet, config, self-tests, boot); `client/manamethod/RuneClient`; the packet `nusmp:mana_method_rune`; `nusmp-mana-method-server.toml`; `tools/lang/mana_method.json`; the written rune: `RuneAlphabet`,
  `RuneGlyphLayout`, `vfx/client/layer/RuneWritingLayer`, the shape `RUNE_WRITING`, `AbstractVfxLayer.hardVertexCap` and `VfxVertexBuffer.beginActivation(limit, hardCap)`, the 9 textures, `tools/mana_method/` (texture generator, previews, reference frame), `tools/vfx_preview/scenes.d/mana_method.json`
  and an append-only block in `tools/vfx_preview/preview.py`; the previews listed above.
* **REPLACED**: the dot-ring look (see "What was removed"); the earlier staged glyph looks (random sprites, then the staged hand-written rings) are now the one wired look.
* **KEPT**: the older True Magic page (`true_magic_rune_array`) and its tests, the Mana Method Rune Stone item, the Heart kingdom perk, `SpellRuntime`, every older page and mode number, the other vfx layers (400-vertex cap unchanged), the Time Magic VFX.

## Sources and what could not be fetched

* English wiki first (rule 10): the Black Clover wiki page Wind Magic ("with the Heart Kingdom's magic technique the user can cast True Wind Magic: runes channel natural mana into a spell and create real wind") and the pages of the Heart Kingdom spirit guardians; Sportskeeda "7 strongest Mana Method users in Black Clover"
  (runes program a spell: follow a target, explode on contact; True Magic is the real natural element, for natural attributes). Found through web search summaries only.
* The numbers (ranges, caps, 85 % for two runes, the cone, the turn rate) are the author's interpretation, as with the older True Magic page; the canon gives no values. The Japanese wiki was not needed (the English pages covered the names and effects) and was not fetched.
  Fandom pages could not be fetched directly (search summaries only).

## Not seen in a game / honest caveats

* **Nothing was run in Minecraft.** The type-check used the API stubs (their only complaints in my files are the known stub artifacts: `MobEffectInstance` / `BlockPos` shown as abstract); the pure and source-scan tests ran offline; the self-tests have never run.
* How Tensura applies `MAGIC_RESISTANCE` is not verified: the bypass multiplies the raw damage by 1 / (1 - resistance), reading the attribute as the existing `Paladins.resistance` does. If Tensura applies it differently the bypass is off by that difference.
* The hit of the Homing self-test goes through `GrimoireBook.hurt` with a stand-in player; if the stand-in cannot be a damage source the test fails with "did not strike", look at the server log for the caught fault (`mana-method`).
* The look (stroke width, brightness against real terrain, the grimoire's pose, scale and glow next to the rune) is only seen in the software preview, which cannot draw the item model (glow only). The grimoire's hover offset and size follow the existing floating-grimoire code, unverified in a game.
* Bystanders on the far side of the rune's plane see it mirrored (additive quads in the plane, no culling); the caster and anyone in front read it correctly.
* The item renderer and the pose rotations allocate a little per frame for at most 6 grimoires within 32 blocks; the layout and the layer do not.
* `VfxManager`'s `maxActive` cap and the frame vertex cap can drop a rune when many other effects run; it is then simply not drawn that frame (the server is unaffected).
* Anti-Magic users still cancel it as they cancel any magic (their rules read the damage type); True Magic does not change that.
* The Rune Stone's armed runes (`MagicGear.placeRune`, a 5 minute `SpellRuntime.zone` with an uncapped entity query per half second) were not touched; they are not capped, which a later pass may want.
