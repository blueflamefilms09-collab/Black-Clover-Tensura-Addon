# Mana Method (Magic Formula): rune constructs, Homing and Growth, True Magic blocks

Builder `manamethod`. Spec: the owner's master prompt, section 1 (Mana Method). The audit that decided what is new is `docs/magic/manamethod_audit.md`.
Self-test prefix for the smoke list: **`magic.mana_method`** (3.4.0: `magic.mana_method` and `.true_blocks`; 3.5.0 adds `.charge`, `.physical`, `.cage`, `.lightning`, `.air_step`, `.dash`, all under the same prefix, which `.github/workflows/smoke.yml` already lists). Server setting: `serverconfig/nusmp-mana-method-server.toml`.
3.5.0 (the sections called 3.5.0 below): hold to charge, the command filter, True Magic's physical hit, world and Anti-Magic answer, two mobility pages, and the canon check.

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

## 3.5.0: what changed (hold to charge, the filter, True Magic's body and world, two mobility pages)

Plain words first (rule 12): the 3.4.0 Mana Method wrote a rune of a fixed size, always for the same time, and the bolt always flew the same. In the anime the Mana Method is a rune you WRITE: the mage shapes natural mana into runes that give the spell
instructions ("pursue the target", "grow bigger"), and a bigger, more complex array makes a stronger spell. So the cast now follows how long you hold, the bolt takes its instructions from the runes, True Magic is the real element (a real hit, real changes to the world),
and there are two runes that carry the caster. Every part is behind its own SERVER setting; **with the settings off the cast is exactly the 3.4.0 cast** (`RuneCommandsTest` pins the legacy numbers value by value, the source scans pin the "off" branches).

### 1. Hold to charge (`holdToCharge`, `RuneHold`, `RuneCharge`, `RuneHoldView`)

* **How it is read.** The grimoire already calls `onHeld` every tick while the page is held and `onRelease(heldTicks)` when it is let go. `RuneHold` (server, per player UUID) counts the ticks, and at the release `RuneCharge.of(held, minHold, maxHold)` turns them into a charge:
  **BASIC** (held less than `minHoldTicks`, default 8): power x0.5, size x0.75, Growth x0.6, one ring (the hand ring), no second rune; **CHARGED** (8 to 39 ticks): power from x0.75 rising to x1.6, size 0.9 to 1.4, Growth 0.9 to 1.4, two rings;
  **FULL** (`maxHoldTicks`, default 40, and more): power x1.6, size x1.5, Growth x1.5, every ring and the loose glyphs, and sneaking writes both runes. The wind-up after the release is 6 / 8 / 10 ticks. A tap is a quick weak spell, a full hold is the max tier. The server measures the hold; the client only draws it.
* **What the client draws.** `RuneHoldView` (pure timeline) writes the hand ring during the first `minHold` ticks, the middle ring up to the middle of the charged range, the outer ring up to `maxHold`, the loose glyphs in the last 40 %. At the release the hand ring is always finished, the middle ring too when the minimum was reached, and the outer ring and
  the glyphs only when the hold was full (a half-written ring stays half-written: the array the caster did not finish). `RuneGlyphLayout.fillStaged` draws it, `RuneWritingLayer` reads the view through the effect's seed (bits 8 to 15 = the view's key; bits 0 to 5 = the spec, as before). A held rune follows the caster's eye and look until the release.
* **The packet.** The rune packet `nusmp:mana_method_rune` is **unchanged (28 bytes)**. A new small packet `nusmp:mana_method_hold` (`HoldPayload`: a varint owner id and 5 bytes; phases HOLD, RELEASE, CANCEL) goes through `PerfBudgets.effectPacket` to players within 64 blocks, server to client only, registered on the same optional registrar (a client without the mod sees nothing).
  The client checks it with `RuneNet.validHold` (phase 1 to 3, ticks 0 to 200, spec bits in range). If the hold or release packet is dropped, the cast packet creates or adopts the rune; if the cast never comes (cancelled, died, out of mana) a released rune ends after 14 ticks; a hold that stops sending ticks is dropped by the server's sweep (`STALE` 6 ticks) and the clients are told CANCEL.
* **Settings.** `manaMethod.holdToCharge` (true), `minHoldTicks` (8, 2 to 30), `maxHoldTicks` (40, 12 to 100, always at least 8 above the minimum). Off = the 3.4.0 cast (fixed charge, no hold packet).
* **Previews.** `docs/previews/mana_method/rune_hold_tap.png`, `rune_hold_half.png`, `rune_hold_full.png` (frame by frame) and `rune_hold_compare.png` (the three side by side five ticks after the release), made through the game's own layer with `python tools/mana_method/preview_hold.py`
  (it uses the real `RuneHoldView`, `RuneGlyphLayout.fillStaged` and `RuneWritingLayer`, through a new append-only `--hold` option of `tools/vfx_preview/Preview.java`). It cannot draw the grimoire item or real terrain.

### 2. The command filter (`commandFilter`, `RuneCommands`)

At the launch `RuneCommands.inject(spec, charge)` turns what the rune says into the bolt's own state (`BoltState`): **Pursue** switches Homing on (turn rate, hit radius) and **Grow** switches Growth on (growth scale); the charge scales them (a basic bolt grows 0.6x, a full one 1.5x), all within hard caps
(hit radius at most 3.6, splash at most 4.8, damage multiplier at most 3.0). `RuneManager.fly` and `impact` read the bolt state instead of the spec, so the bolt really does what its runes command, and a rune can be given an instruction its element did not have by default. With the setting off, `RuneCommands.legacy(spec)` gives the 3.4.0 numbers value by value (tested).

**Why the rune is not an entity (rule 11).** A vanilla entity for each rune would be tracked, ticked, saved and synced to every nearby client, with a spawn packet and a removal packet, and a hold that is cancelled would have to be cleaned up like a mob. On a public server with many casters that is the most expensive way to keep
a few numbers. The rune stays a **small server record** (`RuneManager.Cast`: a few fields, at most 2 per caster and 24 on the server by default, 4 / 64 hard ceilings): no tracking, no saving, nothing in a chunk, cleaned up on logout, death, dimension change and server stop. The look is the client's own effect, which costs the server nothing.

### 3. True Magic is a real hit (`trueMagic.physicalDamage`, `trueMagic.antiMagicStillCancels`, `TrueDamageRules`)

* With `physicalDamage` (default true) a True Magic hit is no longer Tensura magic damage. It is a vanilla physical or environmental damage source credited to the caster (`new DamageSource(type, caster, caster)`), by element: fire `in_fire`, wind `fly_into_wall`, earth `falling_block`, lightning `lightning_bolt`, water and plant the blow `player_attack`.
  It goes through `GrimoireBook.hurtDirect` (the same multiplier chain as `hurtAs`) and then the **balance law's caps, which stay**. Tensura's magic resistance and the protections that read "magic" do not see it as magic.
* **Does True Magic slip past Anti-Magic? The answer.** Anti-Magic decides by damage type (`AntiMagic.isMagic(DamageSource)`). **With the defaults (`physicalDamage` true AND `antiMagicStillCancels` true) it does NOT slip past:** against a creature that has Anti-Magic active, `RuneManager.antiMagicUser` is true and the OLD magic route is taken, which Anti-Magic cancels as before
  (if the lookup fails the target counts as an Anti-Magic user: the safe answer). Against everybody else True Magic is physical. **With `antiMagicStillCancels` set to false, True Magic DOES slip past Anti-Magic** (and past anything else that reads the damage type, such as the Black Form, Physique and shields): that is the literal "the real element" reading of the spec, so it is a switch and the owner decides the default.
  With `physicalDamage` false everything is the 3.4.0 magic route.

### 4. True Magic changes the world (`plantCage`, `lightningBolt`, `lightningRealBolt`, `TrueWorldRules`, `TrueBlocks.placeCage`, `TrueLightning`)

* **True Plant: a cage.** On a hit on a foe a cage of wood grows round it: a 3x3 ring two blocks high (corner posts of log, the sides of leaves) and a log roof, **17 blocks**. Only free cells of loaded chunks, only where the caster `mayInteract`, never in the Labyrinth or a world the Dungeon Guard holds, never round the caster, an ally or a bystander.
  Caps: 17 per cast, 36 per caster, 240 in the world (the same ledger as the other True blocks); taken back after `blockSeconds` (at most 30 s) and only if the block is still the one placed; the leaves break by hand, so a trapped player is never stuck. If no cell is free (or the setting is off) the 3.4.0 moss carpet is placed instead.
* **True Lightning: a bolt.** `lightningBolt` (default true) strikes at the impact; **by default the strike is visual-only** (the bolt's effect and sound, no vanilla lightning entity: no fire, no mob changes, no damage to bystanders). `lightningRealBolt` (default false) allows the full vanilla bolt, and then only when nobody but foes is within 4.5 blocks, `doFireTick` is on and `placeFire` is on.
  At most 3 per second on the whole server (`TrueWorldRules.takeBolt`), never into an unloaded chunk.
* **Clean-up.** `TrueBlocks.cancelOwner` takes a player's True blocks back at once when they log out, die or change dimension (blocks in a chunk that is not loaded stay in the ledger and expire by time); `onServerStopping` takes everything back. This is a small change to the 3.4.0 behaviour (before, those blocks waited for their timer): it is listed under REPLACED.

### 5. Two universal mobility pages (`mobility`, `airStepSeconds`, `dashStrength`, `AirStep`, `Dash`, `MobilityRules`)

Appended LAST after the five older pages (their ids hold no attack word, so an Avatar never casts them; the test pins it). Same gate as the other Mana Method pages (the Mana Zone's), any grimoire, like the other universal pages.

| Id | Name | Cost | Cooldown | What it does |
|---|---|---|---|---|
| `mana_method_air_step` | Air Step Rune | 8 % (floor 150) | 240 ticks | a small platform of invisible barrier blocks under your feet for `airStepSeconds` (default 6, 2 to 30); it follows you, sneak drops it |
| `mana_method_dash` | Dash Rune | 6 % (floor 100) | 60 ticks (and a 30 tick floor between uses) | a velocity push along your look, `dashStrength` blocks per tick (default 0.9, 0.3 to 2.0; x0.55 for a tap, x1.5 for a full hold; never above 2.0), 3 blocks of fall grace |

The canon has a rune array under the feet that carries the caster (Keraunos, and Gadjah's flight by formula: see the canon check), so a rune under the feet and a rune that throws the caster are the nearest honest readings of "mobility". The Air Step platform is a **barrier-block ledger** (`AirStep`): at most 4 blocks per caster, 16 sessions and 64 blocks on the server; a block is removed only if it is still the block that was placed
(`DungeonGuard.internal` wraps the removal); sessions end on logout, death, dimension change and server stop. Settings: `manaMethod.mobility` (true; false = the two pages cannot be used), `airStepSeconds`, `dashStrength`.

## Canon check (rule 10: English wiki first, Japanese as the fallback)

Method: web search with a domain filter. The sandbox cannot open fandom pages directly (`blackclover.fandom.com` does not resolve here), so the facts below come from the **search summaries** of the pages named, not from a full read of the pages.

| Fact | Source | Language |
|---|---|---|
| Mana Method is the Heart Kingdom's unique magic technique, developed over centuries by adapting to the country's strong natural mana | Black Clover Wiki, "Mana Method" and "Heart Kingdom" | English wiki |
| The user forms runes out of natural mana and arranges them as instructions for their magic, such as "pursue the target" or "grow bigger"; larger, more complex designs make more powerful spells (so: the size and complexity of the rune is the strength of the spell) | Black Clover Wiki, "Mana Method" | English wiki |
| Its peak is True Magic: the mana of nature generates the real element (Lightning Magic becomes True Lightning Magic and makes real lightning; True Plant Magic grows and manipulates real plants; True Wind Magic: runes channel natural mana into the spell) | Black Clover Wiki, "Mana Method", "Mimosa Vermillion", "Wind Magic" | English wiki |
| Mages of the Fifth Stage and below are too weak to form the runes; only the Third Stage or higher can use it in combat (the mod's Stage 3 control gate for True Magic) | Black Clover Wiki, "Mana Method" | English wiki |
| Works best with natural attributes; can also be applied to unnatural ones such as Painting, Sealing and Spatial Magic | Black Clover Wiki, "Mana Method" | English wiki |
| Gadjah (Spirit Guardian, lightning) increases the size and power of his Lightning Magic and adds instructions; he takes very powerful attacks unharmed while runes are applied to his body; in chapter 226 he sends runes round his body into the sky and calls down a thundercloud strike | Black Clover Wiki, "Gadjah", "Chapter 226" | English wiki |
| Rill's training with Potrof: he paints the runes with his paint; Potrof uses the technique to increase the size and power of his Plant Magic; parts of the Potrof page are anime-only and not manga canon | Black Clover Wiki, "Mana Method", "Potrof" | English wiki |
| Keraunos: five layers of natural mana arrays beneath the user's feet turn the user into a lightning bolt that flies at the target at great speed (a mobility use of the Mana Method) | Black Clover Wiki, "Keraunos" | English wiki |
| Mana Method = a language that strengthens natural mana, combined into formulas that give a spell orders such as "chase" and "grow big"; needs the Third Stage or higher | Pixiv Encyclopedia (ピクシブ百科事典), Heart Kingdom and Gadjah entries, search summary | Japanese |
| Gadjah gives his spells flight without a broom and pursuit by formula; True Lightning Magic makes real lightning, is hard to learn, and took him two years | Pixiv Encyclopedia, Gadjah entry, search summary | Japanese |
| Mimosa learned her Heart Kingdom spell (a healing flower garden whose roots spread over the floor) in six months of training there | Pixiv Encyclopedia, Mimosa entry, search summary | Japanese |
| The Heart Kingdom has five Spirit Guardians of the Stage Zero | Black Clover Wiki "Heart Kingdom", Pixiv Encyclopedia | English + Japanese |

**What the canon supports in this build:** hold-to-charge as "a bigger array, a stronger spell" (rune size and complexity = power), the two named instructions (Pursue, Grow) as the filter, True Magic as the real element, the Stage 3 gate, and a rune under the feet that moves the caster (Keraunos, Gadjah's flight).
**What could not be fetched:** the full text of every fandom page (DNS to `blackclover.fandom.com` fails here); the English "Rune array" and "Category:Mana Method Spells" pages beyond their summaries, so no full list of rune names was found (only Pursue and Grow are named; Fly, Speed and Increase Size are NOT named by the sources and are not invented); any Japanese fandom page (only Pixiv Encyclopedia and Wikipedia's series page were reachable through search; the Wikipedia page had nothing on runes);
and any exact number (casting times, ranges, multipliers): the canon gives none, so every number in this doc is the author's. The Mana Zone is a different technique (it borrows the surroundings' mana); the pages share its gate only.

**Cheap extras from the canon, and where they went:**
* Sneak writes a second rune: already in 3.4.0, KEPT.
* Runes applied to the BODY (Gadjah takes powerful attacks while his body carries runes): **owner decision**, not built (it would be a damage-reduction buff, balance-sensitive).
* Painting the runes with a brush (Rill, Potrof's training): **owner decision**, not built (a different weapon and a new look).
* More named runes (Fly, Speed, Size): not named by the sources, so not invented.

## Settings (SERVER, `nusmp-mana-method-server.toml`)

`manaMethod.enabled` (true; **false = the pages are unusable and everything is as before**), `maxPerCaster` (2, 1..4), `maxGlobal` (24, 4..64), `trueMagic.placeBlocks` (true),
`trueMagic.placeFire` (true), `trueMagic.blockSeconds` (30, 5..300). Gameplay settings are server configs; there is no client setting (the drawing follows the existing effect quality).

3.5.0, all default ON except `lightningRealBolt`; each OFF restores the 3.4.0 behaviour of its part:

| Key | Default | Range | Off means |
|---|---|---|---|
| `manaMethod.holdToCharge` | true | | the fixed 3.4.0 cast, no hold packet |
| `manaMethod.minHoldTicks` | 8 | 2 to 30 | (tap threshold) |
| `manaMethod.maxHoldTicks` | 40 | 12 to 100, at least 8 above the minimum | (full hold) |
| `manaMethod.commandFilter` | true | | the bolt flies with the 3.4.0 numbers whatever the charge |
| `manaMethod.mobility` | true | | Air Step Rune and Dash Rune cannot be used |
| `manaMethod.airStepSeconds` | 6 | 2 to 30 | |
| `manaMethod.dashStrength` | 0.9 | 0.3 to 2.0 | |
| `trueMagic.physicalDamage` | true | | True Magic is magic damage again (3.4.0) |
| `trueMagic.antiMagicStillCancels` | true | | True Magic slips past Anti-Magic (see the answer above) |
| `trueMagic.plantCage` | true | | True Plant places the moss carpet (3.4.0) |
| `trueMagic.lightningBolt` | true | | True Lightning has no strike on the impact |
| `trueMagic.lightningRealBolt` | **false** | | the strike is visual-only (the full vanilla bolt only when switched on, and then only when safe) |

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
| 3.5.0 hold packets | per held rune: 1 HOLD, 1 RELEASE or 1 CANCEL (about 6 bytes of body, players within 64 blocks, through `PerfBudgets.effectPacket`); **at most 3 per cast**, none per tick (the client times the hold itself). A caster casts at most about once per page cooldown, so well under 1 packet per second per caster |
| 3.5.0 hold server work | idle: two `isEmpty()` checks per tick; busy: one map put per held tick of the holding player, one sweep every 5 ticks over the few holders (at most one entry per online player, removed after 6 stale ticks, on release, logout, death and dimension change) |
| 3.5.0 filter | one `BoltState` per launch (a few fields); no per-tick cost beyond the 3.4.0 `fly` |
| 3.5.0 physical hit | per hit: one `DamageSource` object (up to 7 hits per cast), no scan |
| 3.5.0 cage | at most 17 blocks per cast, 36 per caster, 240 in the world; one free-cell check per block, loaded chunks only; back after at most 30 s; cleaned on logout, death, dimension change, server stop |
| 3.5.0 lightning | **no entity by default**; at most 3 strikes per second on the whole server, one visual packet each through the effect budget; the full vanilla bolt (0 or 1 entity per strike, same 3 per second cap) only if `lightningRealBolt` is on and the place is safe |
| 3.5.0 Air Step | at most 16 sessions, 4 barrier blocks per caster, 64 on the server; one foot check per session per tick (an `int[12]` scratch, no allocation); gone after `airStepSeconds` (at most 30) and at logout, death, dimension change, server stop |
| 3.5.0 Dash | one velocity set per cast, one `Map<UUID,Long>` entry per recent dasher (30 tick floor, removed on logout); nothing per tick |
| 3.5.0 client | a held rune adds per tick, per held rune (at most 6): one short block ray (the wall clip), one `Frame`, a few small objects in `RuneClient.follow` (no per-frame allocation in the layer or the layout); quads and vertices per rune stay within the same 300 quads / 1,200 vertices as before (a held rune draws fewer until it is fully written: 92 to 712 vertices in the previews); six runes at most 7,200 vertices a frame |
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
* 3.5.0 pure tests (`mana/method`, 54 tests, all passing in the offline shim): `RuneChargeTest` (stages, power and size curves, the ranges of the two hold settings), `RuneCommandsTest` (inject, caps, **legacy equals the 3.4.0 numbers**), `TrueDamageRulesTest` (route, kind per element, resistance factor), `TrueWorldRulesTest` (the cage's 17 cells, caps, who may be caged, the lightning allowance, bystanders),
  `MobilityRulesTest` (platform, caps, dash maths, fall grace), `RuneHoldViewTest` (the timeline of tap, half and full hold, release, fade, the key registry). `ManaMethodSourceScanTest` has 9 new pins (server measures the hold and the hooks are append-only, packet size and budget, the filter at the launch, physical damage and the Anti-Magic route, cage and lightning bounds, Air Step, Dash, the settings, the hold view and its drawing); `PerfSourceScanTest`
  now also covers `RuneWritingLayer` and `RuneClient` as hot render files.
* 3.5.0 self-tests (CI server smoke, **not run here**), all under the smoke prefix `magic.mana_method`: `.charge` (a tap is weaker and quicker than a full hold, the charge is taken once and goes stale), `.physical` (True Magic hurts with a vanilla physical type credited to the caster, a normal rune with the Tensura type), `.cage` (free cells only, caps, gone when the caster leaves and by time),
  `.lightning` (at most the per-second allowance, never into an unloaded chunk), `.air_step` (a bounded platform that follows the feet, sneak drops it, nothing is left at the end), `.dash` (the push, the cooldown, the marked motion, the fall grace).
* Two pinned lists that assumed the spirit pages are the last of a book now look past the five new pages (`SpiritControlSelfTests`, `ManaZoneSelfTests`, through `ManaMethodPages.tailCount`).

## NEW / REPLACED / KEPT

**3.5.0**

* **NEW**: hold to charge (`RuneCharge`, `RuneHold`, `RuneHoldView`, the packet `nusmp:mana_method_hold`); the command filter (`RuneCommands`); True Magic's physical hit and its two switches (`TrueDamageRules`, `GrimoireBook.hurtDirect`); the plant cage and the lightning strike (`TrueWorldRules`, `TrueBlocks.placeCage`, `TrueLightning`); the Air Step Rune and Dash Rune pages (`AirStep`, `Dash`, `MobilityRules`, ids `mana_method_air_step`, `mana_method_dash`);
  twelve server settings; six self-tests; six pure test classes; the staged drawing (`RuneGlyphLayout.fillStaged`); `tools/mana_method/preview_hold.py` and four previews; the canon check; four lang keys.
* **REPLACED** (each only behind its setting; off = the old behaviour): the fixed-length charge of a cast (by the hold, `holdToCharge`); the bolt's numbers read from the spec (by the injected bolt state, `commandFilter`); True Magic's magic-damage hit (by the physical one, `physicalDamage`); True Plant's moss carpet (by the cage, with the moss as fallback, `plantCage`);
  True blocks waiting for their timer after the caster logged out or died (now taken back at once, no setting, small).
* **KEPT**: the five older pages and their ids and order, the 28-byte rune packet, the config keys and file, the data ids, the balance law's caps, the older True Magic page, the Rune Stone, the Heart kingdom perk, every other vfx layer, the Time Magic VFX (untouched). The two new pages are appended last, after the five older ones.

**3.4.0 and before**

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

3.5.0 (read these first):

* **Nothing was run in Minecraft.** Everything here is checked only by the type-check against the API stubs, the 54 pure tests and the source scans (offline), and the preview renders; the six new self-tests have never run.
* **Damage order tap, half, full is not guaranteed strictly.** The charge scales power 0.5x to 1.6x, but the balance law's per-hit caps run after it; a tap is never above a full hold, but two stages can end up at the same capped value against a weak target. The self-test only requires tap <= half <= full and tap < full.
* **Tensura's `MAGIC_RESISTANCE`.** It is not verified whether it applies only to magic damage types. If it applies to every type, the physical route is reduced by it too (True Magic would then not ignore it).
* **Sneak state.** The second rune is decided at the cast; the HOLD packet carries the sneak state at the start of the hold. If the player sneaks half-way, the rune first shown can differ from the second rune the server writes (the cast packet corrects it).
* **The effect instance's own anchor is fixed at its start.** The layer draws from the view's frame, which is updated every tick, but a caster who runs far while holding (at most 100 ticks) may see the rune's culling and level of detail worked out from the old anchor.
* **Anti-Magic.** The default keeps Anti-Magic working against True Magic (see the answer above); the owner may flip `antiMagicStillCancels`.
* **Air Step uses barrier blocks.** They are invisible and hold the caster, but another mod that reads barrier blocks (map or claim mods) will see them for a few seconds. The fall grace of 3 blocks is a choice.
* **Lightning is visual-only by default.** The real vanilla bolt is behind a second switch because it sets fires and changes mobs.
* **The held rune** in the dark, against real terrain, in first person, and the grimoire's pose were seen only in the headless preview, which cannot draw the item model or the world.
* A 3.4.0 server talking to a 3.5.0 client sends no hold packet: the client then draws the fixed 3.4.0 rune (the fallback).

3.4.0 and before:

* **Nothing was run in Minecraft.** The type-check used the API stubs (their only complaints in my files are the known stub artifacts: `MobEffectInstance` / `BlockPos` shown as abstract); the pure and source-scan tests ran offline; the self-tests have never run.
* How Tensura applies `MAGIC_RESISTANCE` is not verified: the bypass multiplies the raw damage by 1 / (1 - resistance), reading the attribute as the existing `Paladins.resistance` does. If Tensura applies it differently the bypass is off by that difference.
* The hit of the Homing self-test goes through `GrimoireBook.hurt` with a stand-in player; if the stand-in cannot be a damage source the test fails with "did not strike", look at the server log for the caught fault (`mana-method`).
* The look (stroke width, brightness against real terrain, the grimoire's pose, scale and glow next to the rune) is only seen in the software preview, which cannot draw the item model (glow only). The grimoire's hover offset and size follow the existing floating-grimoire code, unverified in a game.
* Bystanders on the far side of the rune's plane see it mirrored (additive quads in the plane, no culling); the caster and anyone in front read it correctly.
* The item renderer and the pose rotations allocate a little per frame for at most 6 grimoires within 32 blocks; the layout and the layer do not.
* `VfxManager`'s `maxActive` cap and the frame vertex cap can drop a rune when many other effects run; it is then simply not drawn that frame (the server is unaffected).
* Anti-Magic users still cancel it as they cancel any magic (their rules read the damage type); in 3.5.0 this is still the default (`antiMagicStillCancels` true), and the switch to let True Magic slip past is described in section 3 above.
* The Rune Stone's armed runes (`MagicGear.placeRune`, a 5 minute `SpellRuntime.zone` with an uncapped entity query per half second) were not touched; they are not capped, which a later pass may want.

## Owner decisions (3.5.0)

1. **Anti-Magic against True Magic.** Default: Anti-Magic still cancels it (`antiMagicStillCancels` true). Set it to false and True Magic slips past Anti-Magic, the Black Form and shields that read the damage type. Which should be the default?
2. **Does charging cost more?** Today the cost of a page is the same for a tap and a full hold (a tap is simply weak). A cost that scales with the charge (for example 50 % to 100 %) is one small change in `ManaMethodPages`; it was not built because the owner did not ask for it.
3. **Lightning.** Visual-only by default. Switch on `lightningRealBolt` for the full vanilla bolt (fire, mob changes) where it is safe?
4. **Air Step** uses invisible barrier blocks (a ledger, 4 per caster). An alternative with no blocks (a client-trusting flight) would be less server-safe, so it was not chosen. Fall grace after a Dash is 3 blocks: keep?
5. **Canon extras not built:** runes applied to the body (Gadjah), painting runes with a brush (Rill). Wanted?
6. The tap/half/full damage and size numbers (x0.5 / x0.75..1.6 / x1.6) and the hold defaults (8 and 40 ticks) are the author's; the canon gives none. Tune after a play test.
