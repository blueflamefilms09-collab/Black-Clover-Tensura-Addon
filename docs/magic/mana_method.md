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
5. **`RuneGeneratorUtil`**: pure maths (no Minecraft class, JOML) for the rune array, plus a client-only holder that draws it (see below).

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

## RuneGeneratorUtil

`mana/method/RuneGeneratorUtil.java` (pure) and `client/manamethod/RuneClient.java` (the only client class).

* `Params(radius, complexity, ringParticle, glyphParticle, tint)`; `of(radius, complexity, particle, tint)` is the general entry; `paramsFor(spec)` is the game's look (complexity 2 + modifiers + 1 for True Magic, radius 1.15 to 1.8).
  Complexity 1..5 = that many rings and `4 + 2 x complexity` symbols.
* `geometry`: the figure on the unit circle with `Math.sin` / `Math.cos`: rings (radius 1 - 0.17 k, 2 pi r x 6 points), a ring of symbols between the first two rings, a star polygon {m/2}
  inside; ordered ring by ring from the outside so the rune can draw itself; cached in a `BoundedCache`; at most 400 points (166 at the largest).
* `faceQuaternion(look)`: a JOML quaternion `rotateY(yaw).rotateX(pitch)` taking the plane's normal to **minus the look vector** (it faces the caster), with no roll so the symbols stay upright;
  `frame` turns it into the plane's two axes once, `toWorld` then needs no quaternion and no allocation per point.
* The client draws each point ONCE, when the rune reaches it (the catalog's rune particles live 24 to 36 ticks), with the custom particles `mana_mote` (rings, star) and the element's
  rune glyph (`rune_flame`, `rune_wave`, `rune_luna`, `rune_key`, `rune_bolt`, `rune_leaf`), dyed in the element's colour; plain dust when `particles.custom` is off. The outer ring flares once when the rune releases.
* Budgets: effect quality x distance give a stride (every point / every 2nd / 3rd / 4th, nothing beyond 64 blocks; your own rune always at full detail), each tick spawns only the NEW
  points (6 to 10), at most 40 per rune, each batch asks `PerfBudgets.clientParticles`, at most 6 runes at once (the oldest goes first), nothing allocated per point.

Previews (software render of the REAL class output, `python tools/mana_method/preview_runes.py`): `docs/previews/mana_method/rune_faces_the_caster.png` (six looks incl. up and down: always face-on from the caster's eye, edge-on
from the side with the normal pointing back), `rune_drawing_sequence.png` (the charge and the release), `rune_elements.png` (six elements, single modifier and True Magic).

## Glyph-ring look (STAGED: preview only, the game still draws the dot rings above)

The owner's reference frame (`tools/mana_method/reference_frame.png`): several concentric rings of readable angular rune glyphs, each with a bright yellow core and a red-orange outline, a tight ring round the
hands, a bigger outer ring tilted like a bowl, a few loose glyphs floating above, yellow lightning arcs weaving through the rings, an open glowing grimoire at the lower right. The new look is built and previewed;
**nothing in the game calls it** (a source-scan test fails if any other file names `RuneGlyphLayout`), so the five pages and the dot-ring particles are exactly as described above until you say go.

Previews (`docs/previews/mana_method/`): **`rune_glyph_rings.png`** (the reference frame and the new rune side by side at the same framing, the body is a flat placeholder), **`rune_glyph_alphabet.png`** (the 32 glyphs
and the element tints), **`rune_glyph_charge.png`** (the 18-tick charge and the release), `rune_glyph_lod.png` (the four levels of detail and the six element tints), `glyph_atlas/rune_glyphs_<tint>.png` (the atlases).
Re-make them with `python tools/mana_method/gen_rune_glyphs.py` then `python tools/mana_method/preview_glyph_rings.py`; the quads in the previews are the output of the real class.

* **Alphabet** (`tools/mana_method/gen_rune_glyphs.py`, seeded, strokes in `rune_glyphs.json`): 32 angular glyphs of 32x32 pixels in an 8x4 atlas (cell = index, row major), strokes on a 5x5 lattice (horizontal, vertical and diagonal steps
  only, a tall stem with hooks and ticks like the reference), a bright core and a red-orange outline with a faint glow; a glyph is kept only if it differs from every earlier one and from their mirror images. One atlas per tint:
  gold (the reference look, True Magic), flame, water, wind, earth, lightning, plant (7 atlases of 256x128). The element tints are core + outline colour pairs (water: pale cyan / deep blue, wind: white-mint / sea green, ...).
* **Layout** (`mana/method/RuneGlyphLayout.java`, pure, keeps the quaternion plane of `RuneGeneratorUtil.frameFacing`, so the whole figure still faces the caster): three rings in the caster-facing plane,
  the **hand ring** (8 glyphs, radius 0.28 R, turns +0.09 rad/tick), the **middle ring** (14, 0.55 R, turns -0.05: counter-rotation) and the **outer ring** (20, 0.80 R, tilted 0.5 rad and lifted like a bowl, +0.035), plus
  2 to 6 **floating glyphs** above (bobbing, wobbling), up to 3 **lightning arcs** and the **open grimoire**. Each ring glyph is a quad lying on its ring: its up axis is the outward radius, its right axis the clockwise tangent
  (right x up = the plane's normal, so it reads correctly to the caster, tested). The glyph of each slot is a hash of the spell and the slot (deterministic, the same rune always reads the same).
* **Charge**: over the charge the glyphs light up one after the other (hand ring, middle ring, outer ring, then the floating glyphs): each one grows 60 to 100 % and flashes to 1.6 x brightness for 3 ticks, then settles at 1.
  The arcs start once 35 % is charged; the book fades in from the start. On the release the outer ring widens 25 % and flares for 6 ticks.
* **Lightning**: up to 3 arcs per rune, each 7 jagged strip quads (0.06 wide, turned to the camera) from an outer-ring glyph to a hand-ring glyph or a floating glyph, a new shape every 3 ticks, present 2 slices in 3.
* **Grimoire**: three quads (two tilted gold pages meeting at the spine, a glow behind), hovering with a slow bob at the lower right of the plane as the caster sees it. The preview uses a procedural page texture; in the game the
  floating grimoire model of the mod (`GrimoireFloatClient`) could replace it, which was not checked, so the cheap three-quad book is what is costed.
* **Levels of detail** (`levelFor(detail)`, detail = effect quality x distance): full 72 quads; medium (no book, 3 floating glyphs, 2 arcs) 59; low (half the glyphs of the rings, 1 arc) 28; minimal (outer ring only, 10 glyphs) 10; none beyond 64 blocks.

**Worst case with this look (per rune, replacing the 166 to 204 dot particles when it is switched on):** 72 quads = 288 vertices at full detail (48 glyph quads + 21 arc strips + 3 book quads), 59 / 28 / 10 at the lower levels;
at most 6 runes at once = **432 quads = 1,728 vertices a frame** (and the farther ones are drawn at a lower level, so a crowd costs far less); at most 3 spark particles per rune per tick on top, each through
`PerfBudgets.clientParticles` (0 while that budget is spent); a layout is filled into the rune's own preallocated arrays each frame (about 150 sin / cos, **no allocation**), 7 atlases of 32 KB bound the textures;
server cost and packets: unchanged (the same one 28-byte packet). To switch it on the game needs (1) a client setting `visual.runeStyle` (classic dots / glyph rings; the dot rings stay), (2) a render layer that draws the quads through the
existing `VfxVertexBuffer` batches (so the frame vertex cap of `VfxQuality` applies), (3) the atlases copied to `assets/nusmp/textures/`. None of that is done, so nothing about the game's cost has changed.

## Settings (SERVER, `nusmp-mana-method-server.toml`)

`manaMethod.enabled` (true; **false = the five pages are unusable and everything is as before**), `maxPerCaster` (2, 1..4), `maxGlobal` (24, 4..64), `trueMagic.placeBlocks` (true),
`trueMagic.placeFire` (true), `trueMagic.blockSeconds` (30, 5..300). Gameplay settings are server configs; there is no client setting (the drawing follows the existing effect quality and `particles.custom`).

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
| Client | at most 6 runes, 166 points each (204 with the flare), 6 to 10 spawns per rune per tick, at most 40, under the shared client particle budget; none beyond 64 blocks; the geometry is built once per (rings, symbols) |
| Time Magic VFX | untouched |

## Tests

* Pure (`mana/method`): `RuneGeneratorUtilTest` (plane orientation for 60 look vectors incl. straight up and down: normal = minus look, orthonormal right-handed axes, no roll, every point in the plane, ring radii, symbols keep
  their radius when they turn, caching, bounded points, the draw-in prefix, strides and the once-per-point guarantee, catalog particles exist), `RuneRulesTest` (steering turn bound and unit length, 180 degree turn, Growth caps, both runes, resistance bypass
  and its caps, spec packing, caps clamped), `TrueBlockRulesTest`, `ManaMethodSourceScanTest` (server-friendly: client classes, no keybind, clientbound packet only, clean-up on every exit; no lag: idle path, caps before work, budgeted sweep,
  no chunk load, permission per cell, allocation-free draw; wiring: pages last, gate order, one config file, one boot line, the older True Magic page and the Rune Stone untouched, Time Magic not referenced; docs and previews exist).
* In game (CI server smoke, **not run here**): `magic.mana_method` (the five pages are the last of every grimoire, closed for a fresh player, off the Avatar's list; NBT round trip for every spec; caps per caster; despawn by itself; clean-up of a dropped
  caster; a Homing rune strikes a zombie 3 blocks off its line; Growth numbers) and `magic.mana_method.true_blocks` (water source, stone mound, fire; nothing where the caster may not build; wind and lightning place none; caps; everything taken back).
* Staged look: `RuneGlyphLayoutTest` (glyphs face the caster and stand on their rings pointing outward, the rings are circles in the plane and counter-rotate, the sequence lights hand ring first, the flash, every count bounded at every level, arcs late, bounded and camera-facing, the book at full detail only, deterministic, `fill` allocates nothing, the atlases are 32 distinct glyphs per tint) and a scan test that nothing in the game names the staged class.
* Two pinned lists that assumed the spirit pages are the last of a book now look past the five new pages (`SpiritControlSelfTests`, `ManaZoneSelfTests`, through `ManaMethodPages.tailCount`).

## NEW / REPLACED / KEPT

* **NEW**: the five pages; `mana/method/*` (rules, spec, generator, manager, true blocks, packet, config, self-tests, boot); `client/manamethod/RuneClient`; the packet `nusmp:mana_method_rune`; `nusmp-mana-method-server.toml`; `tools/mana_method/` (preview); `tools/lang/mana_method.json`; the three previews; the STAGED glyph-ring look (`RuneGlyphLayout`, `gen_rune_glyphs.py`, `preview_glyph_rings.py`, `DumpGlyphRings.java`, 4 more previews, 7 preview atlases, the reference frame), not wired into the game.
* **REPLACED**: nothing.
* **KEPT**: the older True Magic page (`true_magic_rune_array`) and its tests, the Mana Method Rune Stone item, the Heart kingdom perk, `SpellRuntime`, every older page and mode number, the Time Magic VFX.

## Sources and what could not be fetched

* English wiki first (rule 10): the Black Clover wiki page Wind Magic ("with the Heart Kingdom's magic technique the user can cast True Wind Magic: runes channel natural mana into a spell and create real wind") and the pages of the Heart Kingdom spirit guardians; Sportskeeda "7 strongest Mana Method users in Black Clover"
  (runes program a spell: follow a target, explode on contact; True Magic is the real natural element, for natural attributes). Found through web search summaries only.
* The numbers (ranges, caps, 85 % for two runes, the cone, the turn rate) are the author's interpretation, as with the older True Magic page; the canon gives no values. The Japanese wiki was not needed (the English pages covered the names and effects) and was not fetched.
  Fandom pages could not be fetched directly (search summaries only).

## Not seen in a game / honest caveats

* **Nothing was run in Minecraft.** The type-check used the API stubs (their only complaints in my files are the known stub artifacts: `MobEffectInstance` / `BlockPos` shown as abstract); the pure and source-scan tests ran offline; the self-tests have never run.
* How Tensura applies `MAGIC_RESISTANCE` is not verified: the bypass multiplies the raw damage by 1 / (1 - resistance), reading the attribute as the existing `Paladins.resistance` does. If Tensura applies it differently the bypass is off by that difference.
* The hit of the Homing self-test goes through `GrimoireBook.hurt` with a stand-in player; if the stand-in cannot be a damage source the test fails with "did not strike", look at the server log for the caught fault (`mana-method`).
* The look of the particles (size, brightness against real terrain, the rune's rise from `mana_mote`'s gentle drift) is only seen in the software preview.
* Anti-Magic users still cancel it as they cancel any magic (their rules read the damage type); True Magic does not change that.
* The Rune Stone's armed runes (`MagicGear.placeRune`, a 5 minute `SpellRuntime.zone` with an uncapped entity query per half second) were not touched; they are not capped, which a later pass may want.
