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

## Rune writing look (STAGED: preview only, the game still draws the dot rings above)

The owner's reference frame (`tools/mana_method/reference_frame.png`) shows rings of readable rune symbols with a bright yellow core and an orange-red edge, a tight ring round the hands, a bigger tilted outer
ring, loose glyphs above, yellow lightning and an open glowing grimoire; the owner's clarification: **actual rune symbols written in the air**. This look is built and previewed; **nothing in the game calls it**
(a source-scan test fails if any other file names `RuneGlyphLayout` or `RuneAlphabet`), so the five pages and the dot-ring particles are exactly as described above until you say go.

Previews (`docs/previews/mana_method/`, drawn from the output of the real classes; the body is a flat placeholder):
**`rune_glyph_rings.png`** (the reference frame, the new rune at the same framing, and the rune from the caster's eye, face on), **`rune_glyph_writing.png`** (8 frames of ONE glyph being written, then the ring being
written next to the reference), **`rune_glyph_alphabet.png`** (all 32 glyphs at 4x with names and stroke counts), `rune_glyph_charge.png` (the 18-tick charge and the release), `rune_glyph_lod.png` (the four levels
and the six elements, each writing its own words). Re-make them with `python tools/mana_method/preview_glyph_rings.py`.

* **Alphabet** (`mana/method/RuneAlphabet.java`, pure): 32 hand-authored runes, each 2 to 6 straight strokes on a 5x5 lattice (x right, y up), in the angular Elder-Futhark style of the reference: vertical bars with hooks
  and branches (fehu, uruz, ansuz, raido ...), triangles, crosses, arrows, diamonds (ingwaz, lantern), gates and tridents. Pinned by `RuneAlphabetTest`: 32 distinct names, no two glyphs equal or mirror images, every one at
  least 3 lattice units tall or wide, at most 6 segments (this bounds the quads of a rune). The older **stamped sprite** alphabet (`gen_rune_glyphs.py`, 32 random 32x32 sprites, `glyph_atlas/`, `rune_glyph_sprites_alt.png`)
  was not what the owner meant (random strokes, not real runes); it is kept as a cheaper alternative (one quad per glyph) and is not used by the writing look.
* **Handwriting** (`RuneGlyphLayout.write`): a glyph is written by a PEN that walks its strokes one after the other (a pen lift is instant) at a constant speed along the path; every stroke segment up to the pen is ONE strip quad
  in the caster-facing plane (0.32 of the glyph's half size wide: yellow core, orange-red edge, square caps so the joints close), the last one shortened to the pen's tip, and a glow quad sits on the pen. The line is brightest
  right behind the pen (1.7 x), cools to its resting glow over 3 ticks, and then **breathes** (+-12 %, each glyph a little out of step). The preview shows 8 pen positions of the glyph raido.
* **Reads right**: a glyph is written in its own frame (right = the clockwise tangent of its ring, up = outward), right x up = the plane's normal, so it reads correctly to the caster and is never mirrored. Tested by the
  winding of every strip (a mirrored glyph would be wound away from the caster) and by fehu's branches ending on the right of its stem, for four look directions including straight down.
* **Rings** (the quaternion plane of `RuneGeneratorUtil.frameFacing` is kept): the **hand ring** (6 glyphs, radius 0.34 R, turns +0.09 rad/tick), the **middle ring** (9, 0.60 R, -0.05: counter-rotation) and the **outer
  ring** (13, 0.84 R, tilted 0.5 rad and lifted like a bowl, +0.035), plus 1 to 4 **floating glyphs** above. The rings are written AT THE SAME TIME, each glyph after the one before it (hand ring over 3..48 % of the charge,
  middle 12..66 %, outer 22..84 %, floating 60..92 %): about 1.7 to 2.7 ticks per glyph in an 18-tick charge, a pen sweeping each in a few frames. Written glyphs keep glowing and breathing and the rings keep turning; on
  the release the outer ring widens 25 % and flares for 6 ticks.
* **Words per element** (`RuneAlphabet.phrase`): each element has a fixed run of six glyphs (Flame: kenaz, sowilo, hagal, fehu, triangle, algiz; Water: laguz, jera, ingwaz, ansuz, eihwaz, zigzag; Wind, Earth, Lightning,
  Plant likewise). The hand ring writes the phrase, the other rings write it half the time and the rest of the alphabet otherwise (deterministic from the spell). True Magic replaces the first and the fourth word by gebo
  and othala, the two glyphs only it writes. Two elements share at most two words (tested).
* **Lightning**: up to 3 arcs of 7 camera-facing strip quads (0.06 wide) from an outer-ring point to a hand-ring point, a new shape every 3 ticks, present 2 slices in 3, from 55 % of the charge. **Grimoire**: 3 quads (two
  tilted gold pages and a glow), bobbing at the caster's lower right; the mod's floating grimoire model could replace it, which was not checked.
* **Colours**: one stroke texture per element tint (core + edge colour pairs: gold / True Magic, flame, water, wind, earth, lightning, plant; the preview builds them from the table in `preview_glyph_rings.py`).
* **Levels of detail** (`levelFor(detail)`, detail = effect quality x distance): full (all glyphs, book, 3 arcs), medium (no book, half the floating glyphs, 2 arcs), low (half the glyphs of each ring, 1 arc), minimal (half the
  outer ring only), none beyond 64 blocks.

**Worst case with this look (per rune, replacing the 166 to 204 dot particles when it is switched on):**

| What | Bound |
|---|---|
| Stroke strips | at most 32 glyphs x 6 segments = **192** (a typical full rune: about 100, the alphabet averages 3.4 segments), 4 pen glows |
| Other quads | at most 21 lightning strips and 3 book quads |
| **Per rune** | at most **220 quads = 880 vertices** at full detail (typically about 125 quads); 59-to-10 times fewer at the lower levels |
| **Six runes** | at most **1,320 quads = 5,280 vertices a frame** (and the farther ones are drawn at a lower level, so a crowd costs far less; the near ones are capped by the `VfxQuality` frame vertex cap once the quads go through `VfxVertexBuffer`) |
| Particles | at most 3 spark particles per rune per tick on top, each through `PerfBudgets.clientParticles` (the spark spawning is not written yet) |
| Per frame work | one `fill` per drawn rune: about 130 sin / cos for the rings and one multiply-add per strip corner, **no allocation** (preallocated `Layout` arrays; a test reads the bodies of `fill`, `write`, `emit`, `arc`, `book`, `writeSingle` and fails on `new`) |
| Textures | 1 stroke strip texture per tint (tiny), 1 pen glow, 1 book page |
| Server | unchanged: the same one 28-byte packet per cast |

To switch it on the game needs (1) a client setting `visual.runeStyle` (classic dots / written runes; the dot rings stay), (2) a render layer drawing the quads through the existing `VfxVertexBuffer` batches (so the frame
vertex cap of `VfxQuality` applies) with the stroke textures, (3) the textures copied to `assets/nusmp/textures/`. None of that is done, so nothing about the game's cost has changed.

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
* Staged look: `RuneAlphabetTest` (32 distinct legible glyphs of at most 6 segments, none a mirror image of another, the words of each element) and `RuneGlyphLayoutTest` (the pen walks the strokes and the line only grows, glyphs read right way round in every plane, strokes in the plane and inside the glyph's box, the glow cools behind the pen, rings written at once glyph after glyph hand ring first, each element its own words, every count bounded at every level, arcs late and camera-facing, the book at full detail only, deterministic, nothing allocated), and a scan test that nothing in the game names the staged classes.
* Two pinned lists that assumed the spirit pages are the last of a book now look past the five new pages (`SpiritControlSelfTests`, `ManaZoneSelfTests`, through `ManaMethodPages.tailCount`).

## NEW / REPLACED / KEPT

* **NEW**: the five pages; `mana/method/*` (rules, spec, generator, manager, true blocks, packet, config, self-tests, boot); `client/manamethod/RuneClient`; the packet `nusmp:mana_method_rune`; `nusmp-mana-method-server.toml`; `tools/mana_method/` (preview); `tools/lang/mana_method.json`; the three previews; the STAGED rune-writing look (`RuneAlphabet`, `RuneGlyphLayout`, `preview_glyph_rings.py`, `DumpGlyphRings.java`, 5 more previews, the reference frame, and the older stamped-sprite alternative `gen_rune_glyphs.py` with its 7 atlases), not wired into the game.
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
