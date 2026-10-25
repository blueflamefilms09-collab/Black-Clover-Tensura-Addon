# Mana Method (Magic Formula): rune constructs, Homing and Growth, True Magic blocks

Builder `manamethod`. Spec: the owner's master prompt, section 1 (Mana Method). The audit that decided what is new is `docs/magic/manamethod_audit.md`.
Self-test prefix for the smoke list: **`magic.mana_method`** (3.4.0: `magic.mana_method` and `.true_blocks`; 3.5.0 adds `.charge`, `.physical`, `.cage`, `.lightning`, `.air_step`, `.dash`, all under the same prefix, which `.github/workflows/smoke.yml` already lists). Server setting: `serverconfig/nusmp-mana-method-server.toml`.
3.5.0 (the sections called 3.5.0 below): hold to charge, the command filter, True Magic's physical hit, world and Anti-Magic answer, two mobility pages, and the canon check.
3.6.0 (the sections called 3.6.0 below; self-tests `magic.mana_method.cost`, `magic.mana_method.lightning_charge`, `magic.mana_method.body` and `magic.mana_method.brush`, all under the existing smoke prefix `magic.mana_method`): the price follows the charge (a refused cast now costs nothing), True Lightning grows with the charge, and the two canon extras the owner asked for, the **Body Rune** (Gadjah) and the **Brush Rune** (Rill), as new pages appended last.

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

`page cast` (server) -> `RuneManager.begin` (switch, per-caster cap, global cap; a refusal is a refusal of the cast, and since 3.6.0 the price the grimoire had already taken is given back, see 3.6.0 section 1: the older text here said "refused before anything is spent", which was not true, the grimoire takes the price before the page runs) -> `RuneNet` packet to players within 64 blocks (28 bytes) ->
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

## 3.6.0: what changed (the price follows the charge, True Lightning grows, the Body Rune and the Brush Rune)

Plain words first (rule 12). **What is built:** (1) a rune page now costs by the hold: a tap about 40 % of the normal price, a full hold the whole price; a cast the page refuses costs nothing; (2) True Lightning grows with the charge: 1, 2 or 3 jagged bolts, taller and brighter, a wider hit, and more arcs in the written rune;
(3) two new universal pages the owner asked for, appended LAST after the seven that exist: the **Body Rune** (Gadjah writes runes on his own body: small temporary gifts) and the **Brush Rune** (Rill paints a rune array on a surface, which fires one bolt at the first foe that steps in).
**How it is read:** all of it is decided on the server (the client only draws); each part has its own SERVER setting, default ON, and with the setting off the 3.5.0 behaviour is back (the two new pages cannot be used). **What is not included:** the real vanilla lightning bolt stays OFF (`lightningRealBolt`, owner's answer),
Anti-Magic still cancels True Magic (owner's answer), the Air Step's and the Dash's fall grace is unchanged, the grimoire's cooldown is not scaled, no GUI code was touched, and no new key is bound.

### 1. The price follows the charge (`costScalesWithCharge`, `minCostFraction`, `RuneCost`, `ManaMethodPages.costFactor`)

* **The curve (pure, `RuneCost`).** `fraction = min + (1 - min) * level`, `level = (power - 0.5) / (1.6 - 0.5)` clamped to 0..1, `min` = the setting `minCostFraction` (default 0.40, 0.10 to 1.0). The fraction never passes 1: a cast never costs more than the page's normal price. A cast without a measured hold (the 3.4.0 spell, hold-to-charge off) costs the normal price.

  | Charge | Power | Fraction of the normal price (default) |
  |---|---|---|
  | BASIC (a tap) | x0.5 | **40 %** |
  | CHARGED, just past the minimum hold (8 ticks) | x0.75 | 53.6 % |
  | CHARGED, half way (24 ticks) | x1.18 | 76.8 % |
  | FULL (40 ticks and more) | x1.6 | **100 %** |
  | no measured hold (legacy cast) | x1 | 100 % |

* **How it is read.** One line in `GrimoireBook.getMagiculeCost` multiplies the page's price by `ManaMethodPages.costFactor`, which PEEKS the charge of the release that is waiting for its cast (`RuneHold.peek`, it is not used up) and gives 1 when the setting is off, hold-to-charge is off, nothing is waiting, or for the Air Step Rune (nothing to charge).
  Applies to the Flame, Water, Wind, Earth, True Magic, Dash, Body and Brush pages. The price already carries the percent of the pool, the floor, the Heart kingdom perk and the other multipliers, so the fraction multiplies all of them.
* **A cancelled hold costs nothing** (the grimoire only prices a cast that happens). **The cooldown is NOT scaled** (documented choice: a tap is cheap but not spammable; the grimoire sets the cooldown after a successful cast, as before).
* **A refused cast costs nothing (fix).** The grimoire takes the price BEFORE the page's cast runs, so a cast the page then refuses (runes still forming, server cap full, no free surface to paint on, ...) used to cost mana; the old doc line "refused before anything is spent" was wrong. Now `ManaMethodPages.refund` gives back exactly what was charged (`getMagiculeCost * paidFraction`; a creative player paid nothing), through `Tensura EnergyHelper.gainMagicule`.
  The True Magic page's own fuel refund (the environment pays part of the cost) now works on the scaled price.

### 2. True Lightning grows with the charge (`lightningChargeScale`, `LightningCharge`)

The owner's answer: the real bolt (`lightningRealBolt`) stays OFF; "just add the charge effect". Three things scale, all inside hard caps and all pure (`LightningCharge`, tested):

| | Tap (level 0) | Half hold (24) | Full hold | Hard cap |
|---|---|---|---|---|
| Visual bolts at the strike | 1 | 2 | 3 | 3 |
| Height of a bolt | 8 blocks | 16.6 | 22 | 24 |
| Width (the effect's power) | 0.9 | 1.45 | 1.8 | 2.0 |
| Ticks | 10 | 14 | 16 | |
| Side bolts stand off the centre by | 0.6 | 1.15 | 1.5 blocks | |
| Hit and splash radius of the bolt | x1.0 | x1.21 | x1.35 | x1.4 (inside `RuneCommands.MAX_HIT_RADIUS` / `MAX_SPLASH` too) |
| Arcs in the written rune (lightning element, held rune) | 1 (from the hand ring) | 2 (from the middle ring) | 3 (from the outer ring), longer, thicker, brighter | 3 (`RuneGlyphLayout.MAX_ARCS`) |

* **Server.** After `TrueLightning.strike` has let a strike through (the existing allowance of 3 a second for the whole server), `RuneManager.lightningFx` sends up to 3 effect packets of the EXISTING `LIGHTNING_SPEAR` effect (`VfxSpawn.send`, through the effect packet budget, nearby players only). Nothing is placed, nothing is hurt by the picture. Only for a measured hold (a legacy cast gets no extra bolts).
* **Correction to the 3.5.0 text.** `TrueLightning.strike` DOES spawn a vanilla `LightningBolt` entity, but with `setVisualOnly(true)` (no fire, no damage, no mob changes) unless `lightningRealBolt` is on; the line "no vanilla lightning entity" in 3.5.0 section 4 and in the 3.5.0 cost table meant "no harmful bolt". The cost is one short-lived visual-only entity per strike, at most 3 a second on the server, as before; 3.6.0 adds no entity.
* **The hit radius** is `size x radiusMult` for the lightning element only, and `RuneCommands` still clamps it.
* **Previews:** `docs/previews/mana_method/rune_lightning_strike.png` (the strike of a tap, half hold and full hold) and `rune_lightning_rune.png` (the arcs of the written rune), from `python tools/mana_method/preview_lightning.py`.

### 3. The Body Rune (`mana_method_body`, `bodyRunes`, `bodySeconds`, `BodyRuneRules`, `BodyRunes`)

Canon: Gadjah writes runes on his own body and takes very powerful attacks unharmed (chapter 226; see the canon check). The page does NOT copy "takes attacks unharmed" (a damage shield would be balance-breaking and is the thing the 3.5.0 doc marked as an owner decision); it gives small temporary gifts that grow with the hold, and a short glide.

| Page | Cost | Cooldown | Gate |
|---|---|---|---|
| Body Rune `mana_method_body` | 10 % of the pool (floor 200) x the charge fraction | 600 ticks | the Mana Zone's gate, any grimoire, `bodyRunes` on |

| Gift (transient attribute modifier) | Tap | Full hold | Hard cap |
|---|---|---|---|
| Movement speed (+ percent) | +6 % | +20 % | +25 % |
| Attack damage (+ percent) | +5 % | +20 % | +25 % |
| Jump strength | +0.04 | +0.12 | +0.15 |
| Step height (second ring tier and up) | 0 | +0.4 | +0.4 |
| Safe fall distance | +1 block | +5 | +5 |
| Knockback resistance | +0.1 | +0.4 | +0.4 |
| Slow falling (a full hold only, once, at the cast) | | 60 ticks | 60 |
| Duration | 7.5 s | 15 s (`bodySeconds`, 5 to 60) | 60 s |

* **Bounded and clean.** One session per player (a second cast replaces the first: every modifier is removed, then added; it never stacks), at most 64 sessions on the server, six attribute modifiers per session with fixed ids (`nusmp:mana_method_body_{speed,attack,jump,step,fall,knockback}`), **transient** (never saved, a crash cannot leave one behind). A sweep every 5 ticks, only while a session exists, compares two numbers per session.
  Ends by time, logout, death, dimension change, the server stopping, and when the setting is switched off. Time is the WORLD clock.
* **No flight, no shield:** the glide is slow falling (it works only while falling, 60 ticks, once per full-hold cast). Nothing reduces damage except the knockback resistance.
* **Packets:** two small clientbound packets per cast (`BodyPayload` START and END), nearby players only, through the effect packet budget, validated on the client (`RuneNet.validBody`).
* **The look.** The client writes a chest rune (a small array 0.32 blocks in front of the chest) and, from the second ring tier, a rune at the feet, as painted runes (`RuneHoldView.paintTicks`: written over 24 ticks, then kept until the end of the gift, then faded). They follow the body every frame (`RuneClient.onRenderBodies`, only when the body moved or turned). No grimoire glow is drawn for a painted rune.
  Preview: `docs/previews/mana_method/rune_body.png` (`python tools/mana_method/preview_body.py`; the capsule is a stand-in for the player and the body stands still, so it shows the look, not the following).

### 4. The Brush Rune (`mana_method_brush`, `brushRunes`, `brushSeconds`, `brushPerCaster`, `brushGlobal`, `BrushRules`, `BrushRunes`)

Canon: Rill, a Painting Magic user, learns the Mana Method in the Heart Kingdom by writing the runes with his paint and brush (Potrof's training, anime episode 155; see the canon check). The page paints a rune array on the surface you look at.

| Page | Cost | Cooldown | Gate |
|---|---|---|---|
| Brush Rune `mana_method_brush` | 9 % of the pool (floor 180) x the charge fraction | 240 ticks | the Mana Zone's gate, any grimoire, `brushRunes` on |

* **Where.** A block face within 5 blocks of the look (floor, wall or ceiling), only where the caster `mayInteract` (spawn protection, world border), only in a loaded chunk (nothing is ever loaded), only where the cell in front of the face is free, never in the Labyrinth or a world the Dungeon Guard holds, at least 1.5 blocks from the caster's other arrays. If there is no such surface the cast is refused and costs nothing.
* **What it does.** The array draws itself and is armed after 14 ticks. A foe that steps into it (inside the trigger radius in the surface's plane and from 0.3 behind to 2 blocks in front of the surface; on a floor that is "standing in it", on a wall "standing in front of it") makes it fire ONE bolt toward the foe through `RuneManager.beginAt` (the same bolt as the other runes: Pursue / Grow, the balance law, credited to the caster), and the array is spent. It never triggers on, or hurts, the caster or an ally. If the rune caps are full (2 per caster, 24 on the server) the array just keeps waiting.
  Element: the grimoire's natural element, else Wind (the neutral one).
* **The charge** sizes the array and its life: trigger radius 1.3 blocks (tap) to 2.4 (full hold), life `brushSeconds` (default 30, 5 to 120) x 0.5 (tap) to 1 (full hold), and the power of the bolt (the charge).
* **Bounded.** An array is NOT an entity and NOT a block: it is a small record in a list. At most `brushPerCaster` (default 3, hard ceiling 5) per caster and `brushGlobal` (default 24, hard 4..64) on the server. Each array looks around every 4 ticks (staggered by id) with ONE entity query of a small box; all arrays together may do 8 queries a tick (`PerfBudget`); no block access, no chunk load. Gone at once when the caster dies, logs out, changes dimension, the server stops or the setting is switched off.
* **Packets:** `BrushPayload` APPEAR, then FIRED or GONE (an array that just runs out is faded by the client from the life it was told): at most 2 small clientbound packets per array, nearby players only, through the effect packet budget, validated (`RuneNet.validBrush`).
* **The look.** A painted rune on the surface (the plane of the face; on a floor or ceiling the caster's yaw turns the glyphs upright toward the caster), written in 24 ticks, kept, flaring when it fires and fading. A client draws at most 8 painted runes (brush and body together) and none farther than 56 blocks.
  Previews: `docs/previews/mana_method/rune_brush_floor.png` (written, waiting, fired) and `rune_brush_surfaces.png` (tap against full hold; wall and ceiling), from `python tools/mana_method/preview_brush.py`.

### 5. Three faults found on the way (all fixed in 3.6.0)

* **A refused cast cost mana** (see section 1).
* **Two ledgers read the wrong clock.** `AirStep.start` and `RuneHold` stamp `getGameTime`, but the tick handlers passed `server.getTickCount()` (which restarts at a server start): the sweeps now all use `server.overworld().getGameTime()`, the same clock the stamps use (`ManaMethodSourceScanTest.theTimeBasesOfTheLedgersAreOneClock` pins it).
* **The lightning arcs were never drawn, and painted runes drew a stray gold glow** (both found by the previews, both fixed). `RuneGlyphLayout` asked `LightningCharge.arcsReached` with the layout's timeline progress (a hand ring ends at 0.48) instead of the written share of each ring (0 to 1, new `RuneGlyphLayout.writtenOf`), so the 0.5 threshold was never met and the lightning rune showed no arc; and `RuneWritingLayer` drew the book glow
  (a glow where the grimoire hovers) for painted runes, which have no grimoire. Both are pinned by tests (`LightningChargeTest.theLayoutAsksTheArcsWithTheWrittenShareOfEachRingNotWithTheTimelineProgress` and two scan pins).

### 6. Page order and the Avatar

The ids in the book are now: `fire, water, wind, earth, true, air_step, dash, body, brush` (nine), appended last, after the spirit pages; the new two come after the seven that existed, the order of the old ones never moves. Neither new id holds an attack word, so an Avatar never casts them (`ManaMethodPages.tailCount` and the tests that pin "the last pages" look past nine, seven and five).

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
* Runes applied to the BODY (Gadjah takes powerful attacks while his body carries runes): was an owner decision in 3.5.0; **the owner said yes, built in 3.6.0 as the Body Rune page** (section 3 of "3.6.0", with small gifts and no damage shield).
* Painting the runes with a brush (Rill, Potrof's training): was an owner decision in 3.5.0; **the owner said yes, built in 3.6.0 as the Brush Rune page** (section 4 of "3.6.0").
* More named runes (Fly, Speed, Size): not named by the sources, so not invented.

**3.6.0 canon check of the two extras** (English wiki first; the sandbox still cannot open `blackclover.fandom.com`, DNS `ENOTFOUND`, so every line is from the **search summaries** of the page named, not a full read; the Japanese wiki was not needed for these two and was not fetched):

| Fact | Used for | Source (English wiki unless noted) |
|---|---|---|
| Gadjah is the Spirit Guardian of the Heart Kingdom and a lightning mage; he uses the Mana Method to enlarge his Lightning Magic and was the one who taught Luck True Lightning | the lightning growing with the charge; the Body Rune's owner | "Gadjah", "Spirit Guardian", "Lightning Magic" |
| He places runes on his own body, so he takes very powerful attacks unharmed (chapter 226) | the Body Rune (read as small gifts, NOT as a damage shield: the canon names an effect, not a number, and a shield would be unbalanced) | "Gadjah", "Chapter 226" |
| He hovers in the air without magic (a separate ability of his, not a rune) | only the short slow-fall glide of a full hold; no flight was built | "Gadjah" |
| Rill Boismortier is a Painting Magic user who fights with a brush and a palette; he learned the Mana Method in the Heart Kingdom | the Brush Rune's owner | "Rill Boismortier", "Painting Magic", "Mana Method" |
| Potrof told him to write the runes directly with his paint (anime episode 155) | the Brush Rune (the rune is painted on a surface and left standing: it is the nearest honest reading of "painted runes") | "Potrof", "Episode 155" |

What is the author's, not canon: every number (the gifts, the durations, the radii, the caps, the cooldowns), "fires at the first foe that steps in" (the canon says runes written with paint, not that they are traps), and the chest-and-feet pictures of the Body Rune. **Not fetched at all:** the full text of the Gadjah, Rill, Potrof and Chapter 226 pages (summaries only), the episode 155 page itself, and every Japanese fandom page. Nothing here was contradicted by what could be read.

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

3.6.0, all SERVER settings in the same file, all default ON; each OFF restores the 3.5.0 behaviour of its part (the two new pages: they cannot be used). The keys are plain names inside the file's own categories, as the 3.5.0 keys:

| Key | Default | Range | Off / low means |
|---|---|---|---|
| `costScalesWithCharge` | true | | the 3.5.0 flat price (the refund of a refused cast stays: a refused cast never costs) |
| `minCostFraction` | 0.40 | 0.10 to 1.0 | the share of the normal price a tap pays; 1.0 = no discount for a tap |
| `lightningChargeScale` | true | | the 3.5.0 strike (one base bolt, radius x1.0 whatever the charge) and the 3.5.0 arcs of the lightning rune |
| `bodyRunes` | true | | the Body Rune page cannot be used; a running session ends within 5 ticks |
| `bodySeconds` | 15 | 5 to 60 | seconds at a full hold (a tap lasts half) |
| `brushRunes` | true | | the Brush Rune page cannot be used; standing arrays go |
| `brushSeconds` | 30 | 5 to 120 | seconds an array waits at a full hold (a tap half) |
| `brushPerCaster` | 3 | 1 to 5 | arrays one caster may have standing |
| `brushGlobal` | 24 | 4 to 64 | arrays the whole server may have standing |

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
| 3.6.0 price | a pure multiplication (`RuneCost`) and one `RuneHold.peek` (a map read) per price question; no per-tick cost; the refund is one `gainMagicule` call on a refused cast |
| 3.6.0 lightning | per strike (the existing 3 a second for the whole server): at most 3 effect packets of the existing `LIGHTNING_SPEAR` effect, so **at most 9 packets a second server-wide**, each small, each through `PerfBudgets.effectPacket`, nearby players only; no new entity (the visual-only bolt of 3.5.0 is unchanged); the radius is a number inside the 3.5.0 caps. Client: one `LIGHTNING_SPEAR` costs 72 vertices (8 segments), so a 3-bolt strike is 216 vertices for 10 to 16 ticks, inside the layer's 400 per effect; the rune's arcs are at most 3 x 7 = 21 quads, already inside the 300-quad bound of a rune |
| 3.6.0 Body Rune | at most 1 session per player (hard 64 on the server), 6 transient attribute modifiers each; a sweep every 5 ticks only while a session exists (two number comparisons per session); **2 clientbound packets per cast** (START, END), a caster casts at most once per 30 s; no entity, no block |
| 3.6.0 Brush Rune | at most 3 arrays per caster (hard 5) and 24 on the server (hard 64), none an entity or a block, no chunk loaded; per array **one entity query of a small box every 4 ticks**, all arrays together **at most 8 queries per tick** (`PerfBudget`), so at 24 arrays about 6 a tick and at the hard ceiling of 64 still 8; a fired array makes ONE bolt through `RuneManager.beginAt` (inside the rune caps of 2 per caster and 24 on the server, so it adds nothing beyond the 3.5.0 bound); **2 packets per array** (APPEAR, FIRED or GONE), nearby players only; lives 2.5 to 120 s |
| 3.6.0 client | at most **8 painted runes** (brush and body together) drawn at once and none beyond 56 blocks; a painted rune is the same layer and the same bound as a held one (at most 300 quads / 1,200 vertices; **680 vertices measured for a fully written one in the previews**, 1,360 for a body with a chest and a feet rune), so at worst 8 x 1,200 = 9,600 vertices on top of the 7,200 of six cast runes, in practice about 5,500; the frame vertex cap and the distance levels of detail cut the rest; `RuneWritingLayer` has 16 slots (was 8) and no per-frame allocation (a pin test) |
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
* 3.6.0 pure tests (`mana/method`, in the offline JUnit shim: the four new classes pass 40 of 40 with the arc test, the 11 older rule classes 94 of 94, the whole `mana/method` folder 163 of 163): `RuneCostTest` (the curve at 40 / 53.6 / 76.8 / 100 %, never above 1, the setting's range, off = flat, NaN safe), `LightningChargeTest` (bolts 1 to 3, height / width / radius up and inside the caps, the old spell one base bolt, only the lightning element scales, the side bolts stand apart,
  the arcs follow the rings, and the layout asks with the written share), `BodyRuneRulesTest` (every gift grows with the hold and stays under its cap, the step height only from the second tier, the glide only at a full hold, the duration, the fixed ids), `BrushRulesTest` (caps, lifetimes, the arming grace, the staggered turn, spacing, **the trigger geometry on a floor, a wall and a ceiling**, the look of each surface, the packet bytes).
* 3.6.0 source scans (`ManaMethodSourceScanTest`, now 29 tests; `PerfSourceScanTest` also lists `RuneManager` s.mana.method, `AirStep` s.mana.airstep, `BodyRunes` s.mana.body and `BrushRunes` s.mana.brush as probed hot files): the price goes through one line of the grimoire and a refusal is refunded; the lightning scales behind its setting through the existing effect; the Body Rune is transient, bounded and cleaned on every exit;
  the Brush Rune is capped, budgeted, permission-checked and loads no chunk; the new packets are small, validated and clientbound and the pages are appended last and gated; the ledgers share one clock; the painted rune draws no grimoire glow; the config keys are server settings with hard ranges.
* 3.6.0 self-tests (CI server smoke, **not run here**), under the existing smoke prefix `magic.mana_method` (no workflow file was touched): `.cost` (no waiting release = the normal price, asking the price never uses the release up, a tap pays the curve's fraction and the grimoire really charges it, a release for another page changes nothing, the Air Step Rune is unscaled, a full hold pays everything, the price never falls with a longer hold or passes the normal price),
  `.lightning_charge` (the strike plan and the hit and splash radius grow inside their caps, never smaller than the 3.5.0 numbers, another element and the old spell do not scale),
  `.body` (a tap raises the speed, a second cast refreshes and does not stack, a full hold is stronger and under every cap, exactly the modifiers the charge gives are there, a full hold glides and nothing gives flight, the session ends at the charge's tick and on time, a caster who left and `clearAll` leave no modifier),
  `.brush` (arrays only up to the per-caster cap and none on top of another, the caster does not trigger their own array, a foe standing in it draws exactly ONE bolt and the array is spent and does not fire again, arrays expire, a caster who left and `clearAll` leave none). The refund of a refused cast is covered by the source scan, not by a self-test, and the ally rule by the shared `Foes.isFoe` check (both unrun in a game).
* Two pinned lists that assumed the spirit pages are the last of a book now look past the five new pages (`SpiritControlSelfTests`, `ManaZoneSelfTests`, through `ManaMethodPages.tailCount`).

## NEW / REPLACED / KEPT

**3.6.0** (rule 5: each part compared with what the mod already had)

* **NEW**: the cost curve (`RuneCost`, the settings `costScalesWithCharge`, `minCostFraction`, `ManaMethodPages.costFactor`, the one-line hook in `GrimoireBook.getMagiculeCost`); the lightning growth (`LightningCharge`, `lightningChargeScale`, `RuneManager.lightningFx`, `RuneCommands.inject(spec, charge, lightningScale)`, `RuneGlyphLayout.writtenOf`);
  the **Body Rune** page (`mana_method_body`; `BodyRuneRules`, `BodyRunes`, the packet `BodyPayload`, the client pictures in `RuneClient`) and the **Brush Rune** page (`mana_method_brush`; `BrushRules`, `BrushRunes`, `RuneManager.beginAt`, the packet `BrushPayload`, the painted-rune view `RuneHoldView.paintTicks`);
  seven settings (`lightningChargeScale`, `bodyRunes`, `bodySeconds`, `brushRunes`, `brushSeconds`, `brushPerCaster`, `brushGlobal`) plus the two cost ones; four self-tests; four pure test classes; the previews and their scripts (`tools/mana_method/preview_lightning.py`, `preview_body.py`, `preview_brush.py`, `mm36_common.py`, `DumpMm36.java`, an append-only `--paint` option in `tools/vfx_preview/Preview.java` and `preview.py`); the lang keys in `tools/lang/mana_method.json` (two page descriptions and three messages).
* **REPLACED** (each behind its setting, off = the 3.5.0 behaviour): the flat price of a rune page (by the curve, `costScalesWithCharge`); the 3.5.0 True Lightning strike of one base bolt and its fixed radius (by the growing one, `lightningChargeScale`); the arcs of the held lightning rune (by the arcs that follow the rings, same setting).
  Without a setting, small and deliberate: a refused cast no longer costs mana (it did, by accident); the ledgers' sweeps use the world clock the stamps use (they used the server's tick count); the `RuneWritingLayer` has 16 slots instead of 8; the first line of the doc's "refused before anything is spent" (it was not true).
* **KEPT**: the seven older pages with their ids and order (the two new ones are appended last, after them, never in between), the 28-byte rune packet and the hold packet, every older config key and the file, the data ids, the balance law's caps, Anti-Magic cancelling True Magic (owner's answer), `lightningRealBolt` off, the fall grace of the Air Step and Dash, the cooldowns (unscaled), the older True Magic page, the Rune Stone, the Heart kingdom perk, the GUI (untouched), the Time Magic VFX (untouched, not referenced).

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

3.6.0: the Gadjah and Rill facts are from the search summaries of the Black Clover Wiki pages Gadjah, Chapter 226, Spirit Guardian, Lightning Magic, Potrof, Rill Boismortier, Mana Method, Painting Magic and Episode 155 (English wiki first, rule 10); `blackclover.fandom.com` could not be opened from the sandbox (`ENOTFOUND`), so no page was read in full, and the Japanese wiki was not fetched. See "3.6.0 canon check of the two extras".

## Not seen in a game / honest caveats

3.6.0 (read these first):

* **Nothing was run in Minecraft.** The type-check (API stubs, whose only complaints in these files are the known stub artifacts `MobEffectInstance` / `BlockPos` shown as abstract), the pure tests, the source scans and the headless preview renders are all that was run; the four new self-tests (`magic.mana_method.cost`, `.lightning_charge`, `.body`, `.brush`) have never run.
  The `.body` test assumes the `FakePlayer` of the smoke server has a connection (it reads attributes and effects only, but the glide check adds an effect).
* **The Body Rune in a game.** The gifts are small attribute modifiers: whether they feel like "Gadjah's body runes" is a play-test question. The attribute ids are unique, but another mod that reads `SAFE_FALL_DISTANCE`, `STEP_HEIGHT` or `JUMP_STRENGTH` will see the bonus; they are transient and end with the session. The chest and feet pictures follow the body per frame only in a game (they are moved in `RenderLevelStageEvent` AFTER_ENTITIES, with the partial tick, and the effects are drawn later, at AFTER_PARTICLES); the preview has a still capsule, so how smoothly they follow a running player is unseen.
* **The Brush Rune in a game.** The trigger (a box query on the floor, wall and ceiling) is pinned by pure tests and the self-test, not by a play test; a foe that is a flying mob 3 blocks above a floor array does not trigger it (the trigger reaches 2 blocks above the surface). The first array after a cast waits 14 ticks to be armed so the caster who steps back is never hit. A rune that is painted and then its block broken stays until its time ends (it is a record, not a block).
  The bolt leaves the surface toward the foe through the normal rune bolt, so a bolt that starts inside a block (a wall array seen from behind) may hit the wall at once.
* **Previews are a software raster**, without the world, light or bloom; the foe and the player are dark capsules; the bolt that leaves a brush array is the existing bolt and is not drawn there. The lightning strike was drawn with the real `ElementShapesLayer` and the numbers of the real `LightningCharge`, but its look next to terrain is unseen.
* **Costs.** The price follows the hold, so a tap costs about 40 % of the normal price: a player can now cast four cheap taps for what one full hold costs (the cooldown, which is not scaled, is what limits that). If that is too cheap, raise `minCostFraction` (1.0 = no discount).
* **The refund** adds the mana back through Tensura's `EnergyHelper.gainMagicule`; whether Tensura clamps the pool on that call was not checked (the refund is never more than what was taken, so it should not matter).
* **The time-base fix** changes when the Air Step and the held-rune sweeps expire after a server RESTART (before: the stamps and the sweep disagreed, so a stale entry could live too long or too short); it is invisible otherwise.

3.5.0:

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

## 3.7.0: the owner's answers to the 3.6.0 decisions

1. **Tap price 40 %: kept** (no change). 3. **Brush Rune stays a one-shot trap** (no change).
2. **Body Rune damage reduction: built.** A FULL hold (a measured full hold only) also turns away **15 %** of each hit the wearer takes while the runes last (`BodyRuneRules.reduction`, hard cap 20 %; a tap, a half hold and a cast without a measured hold give 0). Server setting `manaMethod.bodyDamageReduction` (default true; false = the 3.6.0 Body Rune, gifts only). It is applied in `BodyRunes.onIncomingDamage` (`LivingIncomingDamageEvent`, players with a session only; void and kill damage, the `bypasses_invulnerability` tag, are never touched). It is still not a shield: 15 % of a hit, for the 15 s of a full hold, with the page's 600 tick cooldown. Cost: one `isEmpty()` check per damage event while no body carries runes, otherwise one map read per hit on a player; no packet, no allocation.
   Tests: `BodyRuneRulesTest` (only a full hold reduces, cap, the reduced amount is smaller, non-negative and clamped); self-test `magic.mana_method.body` (a full hold records the reduction, a tap none). Caveat: Tensura's own damage handling may change the amount again after this event; the order of the two was not checked in a game.

## 3.8.0: the runes as a passive on mastery, and a rune writes your own element

Plain words first (rule 12). **What is built:** (A) who may use the Mana Method pages is now a server setting, `manaMethod.access`, with three modes: **PASSIVE** (the new default: nothing to switch on; the pages open when your grimoire's mastery reaches `passiveMastery`, 0.7), **TIMED** (a new page, **Rune State**, switches the runes on for 8 minutes, then a 10 minute cooldown; both are settings) and **MANA_ZONE_GATE** (exactly the 3.7.0 behaviour, the late-game gate);
(B) a rune writes **the user's own element**, as in the anime: with `manaMethod.ownElementOnly` (default true) the Flame, Water, Wind and Earth Rune pages are listed and castable only in a grimoire of that element (a fused grimoire counts for both parents); `false` gives every grimoire all four, as in 3.7.0.
**How it is read:** the server decides everything; the client only lists. The mastery and the element are read from the grimoire's skill instance, which the client already has (the same way the Signature Spell's `Fusion` tag is read on both sides), so PASSIVE and the element rule need **no packet**; TIMED needs one tiny packet to the player themselves so the menu knows the state is on.
**What is not included:** the Mana Zone page, Omni Volley and Pressure Space keep the Mana Zone gate whatever `access` says (`GrimoireBook.isZoneGated` is untouched); `ManaZoneManager.gateOpenFor` is unchanged; no GUI code, no key, no new entity, no new block; the rune bolt, the sneak "both runes" and the True Magic page keep their element rules.

### Settings (SERVER, `nusmp-mana-method-server.toml`, category `manaMethod`)

| Key | Default | Range | Meaning / what restores 3.7.0 |
|---|---|---|---|
| `access` | **PASSIVE** | PASSIVE, TIMED, MANA_ZONE_GATE | who may use the nine pages; **MANA_ZONE_GATE = the 3.7.0 late-game gate** |
| `passiveMastery` | **0.7** | 0.0 to 1.0 | PASSIVE: the share of the grimoire's mastery at which the pages open (see decision 1) |
| `timedSeconds` | **480** (8 min) | 30 to 3600 | TIMED: how long the rune state stays on |
| `timedCooldownSeconds` | **600** (10 min) | 0 to 7200 | TIMED: the rest after the state ends before the page can be cast again |
| `ownElementOnly` | **true** | | **false = every grimoire shows and casts all four elemental runes, as in 3.7.0** |

To get exactly the 3.7.0 behaviour set `access = "MANA_ZONE_GATE"` and `ownElementOnly = false`.

### The three access modes (`RuneAccess`, `ManaMethodPages.usable(book, instance, entity, pageId)`)

One question, asked in one place (`GrimoireBook.usable` for a Mana Method page): is `manaMethod.enabled` on and the page's own switch (mobility, body, brush) on? Then the mode, then the element rule. "The nine pages" are Flame, Water, Wind, Earth, True Magic, Air Step, Dash, Body and Brush; the tenth, **Rune State**, is the new page.

| Mode | Nine pages usable when | Rune State page |
|---|---|---|
| PASSIVE | the mastery fraction of the grimoire the page belongs to is at least `passiveMastery` (`GrimoireBook.masteryFrac(instance)`, the same number `SoulCorpseSpells` and the Mana Zone gate use); a menu with no instance cannot know it and shows nothing | not usable (hidden) |
| TIMED | the caster's rune state is ON; **mastery and the late-game gate do not matter** | usable (always, to see the time left or to start it) |
| MANA_ZONE_GATE | `ManaZoneManager.gateOpenFor` is open (six requirements), exactly as 3.7.0 | not usable (hidden) |

**Listing on both sides.** PASSIVE: the client reads `instance.getMastery()` (already synced for the stage display) and the book's `getMaxMastery()`; nothing is sent. TIMED: the client reads `RuneStateGate` (one volatile number, no Minecraft types, the pattern of `ManaZoneClientGate`) that the new packet `nusmp:mana_method_state` sets; it is read when a menu asks whether a page is usable, never per frame. MANA_ZONE_GATE: the existing `ManaZoneClientGate`.
The server asks the same function again when a page is cast (`castPage` checks `usable`), so a client that lists a page too early (a stale number) still cannot cast it.

**The TIMED timeline** (defaults 480 s on, 600 s cooldown; the clock is the WORLD game time, stored as two stamps on the player's persisted data, so a relog, a death or a restart cannot reset the cooldown):

| Time | What happens |
|---|---|
| 0:00 | the player casts **Rune State**: action bar "Rune State: ON for 8:00. The runes answer your call."; stamps `OnUntil = 8:00`, `CooldownUntil = 18:00`; the runes are usable |
| 3:00 | casting it again: "Rune State: already ON (5:00 left)." (free, changes nothing) |
| 7:30 | "Rune State: the runes fade in 0:30." (once) |
| 8:00 | the state ends: "Rune State: OFF. It can be called again in 10:00."; the pages close |
| 12:00 | casting it: "Rune State: the runes have not settled yet (6:00 left)." (refused, free) |
| 18:00 | the page works again |
| early cut | the player logs out at 2:00 (or dies, or changes dimension, or the server stops): the state ends THERE, the cooldown starts there (cooldown ends at 12:00); logging in again at 2:05 shows "...not settled yet (9:55 left)" and the clock cannot be reset by relogging. A state still marked ON in the file after a crash is ended at the next login the same way |

The cooldown counts from the END of the state (a long state does not eat into the rest); an early cut starts the rest at the cut and loses the remaining on-time, so leaving early gains nothing.

### The element rule (`RuneOwnElement`, `ManaMethodPages.elementOk`)

The page belongs to a grimoire, and so does the rule: it reads **that grimoire's** magic (`GrimoirePages.magicOf(instance)`) and the two parents written in its `Fusion` tag. A player with two grimoires (for example a second Star grimoire, or one in the pack) gets the elemental pages each grimoire earns; a grimoire never decides for another. The cast comes from the page of the grimoire the player is casting with (the bound / held / slotted one, `GrimoirePages.grimoireOf`), so switching grimoires switches the pages. The table is the natural list of True Magic (`TrueMagicRules.NATURAL`) without Lightning and Plant (they have no basic rune page; the True Magic Rune page covers them); it is pinned for **every** `MagicType` by `RuneOwnElementTest`.

| Grimoire (its magic, or a parent of its fusion) | Elemental rune pages listed and castable (with `ownElementOnly`) |
|---|---|
| Flame | Flame Rune |
| Water | Water Rune |
| Wind | Wind Rune |
| Earth | Earth Rune |
| a fused grimoire with two natural parents (`FLAME:EARTH` on a Glass book) | both: Flame and Earth |
| a fused grimoire with one natural parent (`STAR:WIND`) | that one: Wind |
| Lightning, Plant | none of the four (they have the True Magic Rune page) |
| Time, Star, Sealing, Spatial, Gravity, Light, Dark, Anti-Magic, Reinforcement, ... and also Magma, Explosion, Ice, Mist, Storm, Sand ... (not a basic element by this table) | none |

* **Never filtered by element:** True Magic Rune (it already writes "your natural element": the grimoire's own magic, Stage 3 control; this was checked and is consistent), Air Step, Dash, Body, Brush (they use the natural element, else Wind, for their picture) and Rune State.
* **The bolt keeps its element.** The element of the rune is still the page's (`RuneElement`); the sneak "both runes (pursue and grow)" is about the two instructions, not two elements, and is unchanged.
* **Second look at the cast.** `castElement` asks `elementOk` again and refuses (and refunds the price) a rune that is not the user's, whatever called it.

### Worst-case cost (rule 11)

| What | Bound |
|---|---|
| `usable` (key press, a held tick, a menu) | a few field reads and compares; for the four elemental pages one `getString("Fusion")` and an allocation-free scan (`regionMatches`); the gate is evaluated only in MANA_ZONE_GATE (it was already cached for 2 s); no packet, no world access |
| PASSIVE | no state at all, no tick, no packet |
| TIMED, idle | one `ACTIVE.isEmpty()` check per server tick |
| TIMED, busy | one pass over the players who are ON once a second (a map entry and one persisted-data read each), at most one entry per online player; no world access, no entity query |
| TIMED, network | **at most 2 packets per ON period per player** (start, end), a third on a death or dimension cut: each 2 to 4 bytes of body, to that player only (never to others), 18 minutes apart with the defaults. It is a state flag, not an effect, so it is not dropped by the effect packet budget |
| TIMED, memory and storage | two `long`s per player in the persisted data; one `UUID -> Long` entry per player who is ON; cleared on logout, death, dimension change and server stop |
| Client | one volatile read when a menu asks; nothing per frame |

### Tests

* Pure: `RuneAccessTest` (16 tests: all three modes and the Rune State page in each, the mastery edge, the clamps, the **timeline with the cooldown across a relog**, a crash-left state, death and dimension cut, cooldown 0, the clock text, two players independent) and `RuneOwnElementTest` (10 tests: a table over **every `MagicType`** read from the enum's source, hybrids and malformed fusion strings, the setting off, the other pages unfiltered, the ids agree with the book, True Magic's natural list agrees).
* Source scans: `ManaMethodSourceScanTest` (6 new pins: one decision place, the old gate and `usable(LivingEntity)` kept, the zone-gated pages untouched; the element rule reads only the instance; per-player UUID state, idle check, world clock, persisted stamps, every exit wired; the packet tiny, validated, clientbound and to the player only; the page appended last and free; the five new settings and their defaults; this doc and the lang keys) and `PerfSourceScanTest` (`RuneState` probe section `s.mana.state`, and a new pin that the rune-state tick looks at the empty map and the once-a-second modulo before it sweeps, scans no entities and never sends to others).
* In game (CI server smoke, **not run here**), under the existing prefix `magic.mana_method`: **`magic.mana_method.access`** (the real grimoire's answer follows the configured mode: PASSIVE at the mastery edge, TIMED with the state, MANA_ZONE_GATE with the gate; the Rune State page only in TIMED and last in the book; on a stand-in player the state starts, is refused while on and while cooling, keeps its cooldown across a logout, ends by time, and `clearAll` leaves none) and **`magic.mana_method.element`** (each element's grimoire shows its own page only, a `FLAME:EARTH` fused book both, Time / Star / Lightning none, two grimoires decide for themselves, the other pages unfiltered, the cast of a foreign element is refused; with the setting off all four are open). The two tests adapt to the configured mode; they do not change the config.

### NEW / REPLACED / KEPT (rule 5)

* **NEW:** `RuneAccess` (the pure decision and timeline), `RuneOwnElement` (the pure element rule), `RuneState` (the server side of TIMED), `RuneStateGate` (the client flag), the packet `nusmp:mana_method_state` (`RuneNet.StatePayload`, `validState`), the page `mana_method_state` (**Rune State**, appended LAST after `mana_method_brush`; free; no attack word, so an Avatar never casts it), five settings (`access`, `passiveMastery`, `timedSeconds`, `timedCooldownSeconds`, `ownElementOnly`), the `ManaMethodPages.usable(book, instance, entity, pageId)` and `elementOk`, the self-tests `.access` and `.element`, two pure test classes, six scan pins, one perf pin, one probe section, six lang keys and one page description.
* **REPLACED** (behind settings that restore the old behaviour): the late-game gate as the *default* way to open the Mana Method pages (now PASSIVE on mastery; `access = MANA_ZONE_GATE` restores it); "every grimoire shows all four elemental runes" (now only the user's own; `ownElementOnly = false` restores it); the sentence "Late-game: the Mana Zone gate applies." in the ten page descriptions (lang only, now says who may use it); the single grimoire call `ManaMethodPages.usable(e, id)` (now `usable(this, i, e, id)`; the old method stays).
* **KEPT:** every older page, id and mode number (the new page is appended after Brush; `ManaMethodPages.tailCount` still recognises the 9-, 7- and 5-page tails), `ManaZoneManager.gateOpenFor` and its six requirements, the Mana Zone / Omni Volley / Pressure Space gate, all older settings and the config file, all packets, the bolt, Homing / Growth, the cost curve, the Body and Brush Rune, True Magic's own rules (natural attribute, Stage 3 control), the Time Magic VFX (untouched, not referenced). `en_us.json` was not edited (the coordinator merges `tools/lang/mana_method.json`).

### Honest caveats (3.8.0)

* **Nothing was run in Minecraft.** Type-checked against the API stubs (only the known stub artifacts remain), pure tests and source scans run offline; the two new self-tests have never run. Whether ManasCore syncs the skill instance's mastery and tags to the client at the moment a menu opens (the code relies on it, as the Signature Spell page names already do) is read from the existing code, not seen.
* **A change for existing worlds.** The default is now PASSIVE. A player who had passed the Mana Zone gate but whose grimoire is below 0.7 mastery **loses** the Mana Method pages until the grimoire catches up; a player at 0.7 mastery who never passed the gate **gains** them. Set `access = "MANA_ZONE_GATE"` to keep the 3.7.0 behaviour.
* **Mastery is per grimoire**, not per player: a second grimoire at low mastery shows no runes while the first one does.
* **TIMED needs only the page**: any mage, whatever the mastery and whatever the gate, can call the runes for 8 minutes out of every 18 (True Magic still needs Stage 3 control inside its own cast). That is what was asked; it is the loosest of the three modes.
* **A dimension change ends the TIMED state** (the same rule as the other Mana Method sessions). Going through a portal costs the rest of the state and starts the 10 minute rest. If that is too harsh, it is one handler (`RuneState.onDimension`) to drop.
* **The client's menu flag runs on the machine's own clock.** In single player, pausing the game stops the server's game time but not the client's wall clock, so the menu may stop listing the pages a little before the server ends the state; the server's answer is the one that counts at the cast.
* **Strict element table.** Magma, Explosion, Ice, Storm and the other sub-attributes of the four soul types write no elemental rune page (they are not a basic element in `TrueMagicRules.NATURAL`), and neither do Lightning and Plant (they have the True Magic Rune page). A fused grimoire counts only its two parents. A fused grimoire whose own magic is not natural (a Glass book) still cannot cast the True Magic Rune page (unchanged: it follows the book's own magic).
* The page is free and gives the usual mastery point per cast; with a cooldown of minutes that is negligible.

### Owner decisions (3.8.0)

1. **`passiveMastery` 0.7.** The book's stage is `9 - round(mastery * 9)`: Stage 3 is the band from 0.611 to 0.722, Stage 2 starts at 0.722. The canon says only the Third Stage and above can use the Mana Method in combat, so 0.7 sits inside Stage 3 (late Stage 3). Use 0.62 for "as soon as Stage 3 is reached", 0.73 for Stage 2. Keep 0.7?
2. **Which magics get which elemental rune.** The strict table above gives nothing to Magma, Ice, Storm, Explosion and the like. Should the sub-attributes of the four soul types (Magma = Flame + Earth, Ice = Water, Storm = Wind + Water, ...) count for their parent elements? It is one table in `RuneOwnElement.basicOf`.
3. **TIMED for everyone.** In TIMED the Rune State needs no mastery. Should it also need a minimum mastery (for example the same `passiveMastery`) before the page can be cast?
4. **A dimension change cuts the state.** Keep, or let the state survive a portal (and only end on logout, death and server stop)?
5. **Lightning and Plant mages** have only the True Magic Rune among the elemental pages. Add a Lightning Rune and a Plant Rune page later (new pages, appended last)?

## Owner decisions (3.6.0)

Answered in the brief and built as answered: (1) Anti-Magic still cancels True Magic (no change); (2) the cost scales with the charge (tap about 40 %, full 100 %, `costScalesWithCharge`, `minCostFraction`; the cooldown stays unscaled); (3) `lightningRealBolt` stays OFF and the strike and the rune's lightning grow with the charge instead; (4) the Air Step and Dash fall grace is unchanged;
(5) the Body Rune and the Brush Rune were built, as pages appended last, behind server settings (default ON); (6) the GUI stays anime and was not touched. Still open:

1. **Tap price.** A tap pays 40 %. Is that the right floor (`minCostFraction`)? Four taps cost 1.6 full holds and make four casts (weaker ones, power x0.5); only the (unscaled) cooldown limits how often.
2. **Body Rune strength.** The gifts are small by design (at most +25 % speed and attack, +5 blocks safe fall, +0.4 knockback resistance). Gadjah's canon is "takes very powerful attacks unharmed"; a real damage reduction was NOT built (balance). Should a full hold also give a small damage reduction?
3. **Brush Rune trigger.** It fires at the first foe that steps in and is then spent (a trap). Alternatives that are also canon-neutral: it fires every second until its life ends (more server work, more packets), or it is a visible circle that the caster fires by hand. Keep the trap?
4. **Defaults of the two new pages' numbers** (10 % / 200 / 600 ticks for the Body Rune, 9 % / 180 / 240 for the Brush Rune, 15 s and 30 s lives, 3 arrays per caster, 24 on the server) are the author's; the canon gives none. Tune after a play test.

## Owner decisions (3.5.0)

(Answered for 3.6.0 above: the Anti-Magic default stays, the cost scales with the charge, the real bolt stays off, the fall grace stays, and the body and brush extras are built.)

1. **Anti-Magic against True Magic.** Default: Anti-Magic still cancels it (`antiMagicStillCancels` true). Set it to false and True Magic slips past Anti-Magic, the Black Form and shields that read the damage type. Which should be the default?
2. **Does charging cost more?** Today the cost of a page is the same for a tap and a full hold (a tap is simply weak). A cost that scales with the charge (for example 50 % to 100 %) is one small change in `ManaMethodPages`; it was not built because the owner did not ask for it.
3. **Lightning.** Visual-only by default. Switch on `lightningRealBolt` for the full vanilla bolt (fire, mob changes) where it is safe?
4. **Air Step** uses invisible barrier blocks (a ledger, 4 per caster). An alternative with no blocks (a client-trusting flight) would be less server-safe, so it was not chosen. Fall grace after a Dash is 3 blocks: keep?
5. **Canon extras not built:** runes applied to the body (Gadjah), painting runes with a brush (Rill). Wanted?
6. The tap/half/full damage and size numbers (x0.5 / x0.75..1.6 / x1.6) and the hold defaults (8 and 40 ticks) are the author's; the canon gives none. Tune after a play test.
