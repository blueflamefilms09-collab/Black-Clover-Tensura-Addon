# The 30 wiki magics: effects, textures, armour, and the held-cast fix

Everything about how the thirty "wiki" attributes (`MagicType` AIR .. WING, the set `WikiBooks.magicForColor` returns) look in
game, plus the fix for the held cast key making the grimoire circles flash. Written for 1.27.0.

> Status: written without a compiler or a game client. The pure logic (`WikiStyles`, `CastHold`, `CastHoldView`, `CastAnimGate`)
> has JUnit tests that were run and mutation-checked; the Java that draws (`WikiMagicLayer`, `WikiAura`, `CastHoldLayer`,
> `CastAnimClient`) was type-checked against API stubs only. Nothing has been seen in a running game.

---

## 1. The visual groups

Each magic belongs to one of seven groups. The **group** decides the texture, the blend and the shape language; the magic's
**place in its group** (its variant) and its own accent sprite make every magic different from its neighbours. The table lives in
one pure class, `vfx/WikiStyles.java`; `WikiStylesTest` pins it (the owner's groups, the old numeric styles, the variant order the
layers' small tables rely on, and that it matches `WikiBooks`).

| Group | Magics | Texture (bulk) | Blend | Shape language |
|---|---|---|---|---|
| **MOTES** | Air, Smoke, Snow | `wiki_grp_motes` | alpha | wind-like motes that drift: Air hangs, Smoke billows up (dusky), Snow falls |
| **TRAILS** | Hair, Poison Plant, Vine, Wing | `wiki_grp_trails` | additive | crescent slashes and a streak; Hair = three thin strands, Vine wiggles, Wing = wide beating crescents |
| **CIRCLES** | Memory, Modification, Permeation, Soul, Sound, Switching, Tree | `wiki_grp_circles` | additive | rotating rune rings with tick marks; the magic's own glyph ring turns the other way inside |
| **SHARDS** | Mineral, Nail, Red Ochre, Rock, Sandstone, Scale, Shakudo, Spike, Stone | `wiki_grp_shards` | additive | radial crystal spikes: a starburst head, shards flung out pointing away from the path |
| **FLUID** | Mucus, Mud, Tongue | `wiki_grp_fluid` | alpha | droplets on sagging arcs, splash crowns, ripples; Mud is heavy and brown, Tongue a lashing ribbon |
| **SPIRAL** | Skin, Song, Vortex | `wiki_grp_spiral` | additive | helices of motes round the path; Song drops notes, Vortex is a tight cone |
| **INFERNAL** | Soul Corpse | `wiki_grp_infernal` | alpha (dark) + additive embers | a dark circle with red embers rising, the skull in the middle |

Sandstone is the one magic the owner's list did not name. The first layer put it with the motes by an index slip (its slot, 12,
belonged to Snow's comment), while its field effect already sparkled like a stone, so it joins the **shards**. To move it, change
one line of the table in `WikiStyles` (and the test).

### The 30 magics

`#` is the numeric style the first WikiMagicLayer used (kept, it is the variation index); the colour is `WikiBooks.color`, which is
also how a payload names its magic. Variant = place inside the group, in table order.

| # | Magic | Group | Variant | Accent texture | Palette |
|---|---|---|---|---|---|
| 0 | Air | MOTES | 0 | `wiki_air` | `9FE8FF` |
| 1 | Hair | TRAILS | 0 | `wiki_hair` | `FF6E9A` |
| 2 | Memory | CIRCLES | 0 | `wiki_memory` | `BCA8FF` |
| 3 | Mineral | SHARDS | 0 | `wiki_mineral` | `78A8C8` |
| 4 | Modification | CIRCLES | 1 | `wiki_modification` | `FF9B58` |
| 5 | Mucus | FLUID | 0 | `wiki_mucus` | `72D6B4` |
| 6 | Mud | FLUID | 1 | `wiki_mud` | `8A593D` |
| 7 | Nail | SHARDS | 1 | `wiki_nail` | `C7B9A6` |
| 8 | Permeation | CIRCLES | 2 | `wiki_permeation` | `65D7E8` |
| 9 | Poison Plant | TRAILS | 1 | `wiki_poison_plant` | `9FCB3B` |
| 10 | Red Ochre | SHARDS | 2 | `wiki_red_ochre` | `D64C37` |
| 11 | Rock | SHARDS | 3 | `wiki_rock` | `777C86` |
| 12 | Sandstone | SHARDS | 4 | `wiki_sandstone` | `D5A76C` |
| 13 | Scale | SHARDS | 5 | `wiki_scale` | `63BDB5` |
| 14 | Shakudo | SHARDS | 6 | `wiki_shakudo` | `6E4946` (lifted, see 4) |
| 15 | Skin | SPIRAL | 0 | `wiki_skin` | `E1A995` |
| 16 | Smoke | MOTES | 1 | `wiki_smoke` | `82788F` |
| 17 | Snow | MOTES | 2 | `wiki_snow` | `E7F7FF` |
| 18 | Song | SPIRAL | 1 | `wiki_song` | `FFB4E8` |
| 19 | Soul Corpse | INFERNAL | 0 | `wiki_soul_corpse` | `6C7899` (drawn dark) |
| 20 | Soul | CIRCLES | 3 | `wiki_soul` | `72D9CC` |
| 21 | Sound | CIRCLES | 4 | `wiki_sound` | `70C8F0` |
| 22 | Spike | SHARDS | 7 | `wiki_spike` | `9BA8B9` |
| 23 | Switching | CIRCLES | 5 | `wiki_switching` | `FFD260` |
| 24 | Tongue | FLUID | 2 | `wiki_tongue` | `E66C70` |
| 25 | Tree | CIRCLES | 6 | `wiki_tree` | `477D39` |
| 26 | Stone | SHARDS | 8 | `wiki_stone` | `8F8C86` |
| 27 | Vine | TRAILS | 2 | `wiki_vine` | `6DB743` |
| 28 | Vortex | SPIRAL | 2 | `wiki_vortex` | `59B8C9` |
| 29 | Wing | TRAILS | 3 | `wiki_wing` | `E6E2F4` |

## 2. What each shape does per group (`WikiMagicLayer`)

The three payload shapes keep their meaning (callers did not change): `WIKI_MAGIC_CAST` a bolt over `from`..`to`,
`WIKI_MAGIC_FIELD` an area on the ground (power = radius / 3), `WIKI_MAGIC_BURST` a nova facing the camera. Every field also draws
a faint ring on the real area's edge so the sigil stays readable next to it.

| Group | CAST (a bolt with a short tail) | FIELD (ground area) | BURST (nova) |
|---|---|---|---|
| MOTES | a cloud strung along the path, wandering sideways, rising / falling by magic; accent sprite at the head | motes orbiting and rising through the area | motes thrown out and drifting, the accent behind |
| TRAILS | crescents bulging along the flight, a streak behind, accent sprites (thorn, leaf, feather) shed along it | three crescent arcs turning on the ground, an accent at each head | three crescents spreading, accent at each head |
| CIRCLES | the rune ring stands across the flight like a launch gate, two fading gates behind | the rune ring, the glyph ring turning against it, six glints on the rim | the rune ring and glyph ring, expanding |
| SHARDS | a spear of light, a starburst head, shards flung round the path pointing away | upright crystal spikes rise round the rim (grow in over the first quarter) | a starburst and shards flying out |
| FLUID | droplets sagging behind the head, a splash accent; Tongue adds a lashing ribbon | a puddle, two ripples, droplets hopping | a splash crown thrown out and falling back |
| SPIRAL | strands of motes winding round the path (a cone for Vortex), accent spinning at the head | helices of motes rising from the rim | a spinning spiral with arms of motes |
| INFERNAL | a dark seal at the head, the skull, embers trailing and rising | a dark circle, a skull sigil, embers rising | a dark ring, an ember ring, embers thrown out |

Rules the layer keeps:

* **The colour names the magic.** `styleForColor` reads the 30 palettes of `WikiBooks.color` once (no per-frame `MagicType` walk, no
  allocation); a colour that is not a wiki palette draws as Air, exactly as before.
* **Never throws.** The whole of `render` is inside a `try`; a failure logs once and skips the frame.
* **Vertex budget.** Every variant stays under the 400 vertices of one activation (the worst, the fluid field, is about 376).
* **Dark palettes show.** Additive groups lift a dark colour with `WikiStyles.lift` (Shakudo's `6E4946` would otherwise glow almost
  invisibly); alpha groups keep the palette as it is.

## 3. Textures and the GLOW fallback

All textures are generated by **`tools/gen_wiki_magic_textures.py`** (PIL + numpy, deterministic; `--preview OUT.png` writes a
contact sheet, kept at `docs/vfx_previews/wiki_magic_textures.png`). They follow the rest of `textures/particle`: greyscale + alpha,
tinted per quad by the vertex colour. The script never touches `time_*.png`.

* `wiki_grp_<group>.png` (7): the bulk sprite of a group (mote with a wisp, slash streak, rune ring with 60 ticks, crystal starburst,
  droplet with splash crown, two-arm spiral, dark seal with a {7/2} star). 128 px, the two rings 256 px.
* `wiki_<magic>.png` (30): each magic's own motif drawn in its group's language (snowflake, thorn vine, eye ring, gear ring, nails,
  strata, scales, music staff with notes, skull ...). 128 px.
* `wiki_shell_<group>.png` (7): 64x64 **tileable** patterns the armour wraps round the player model (wind flow lines, slash scratches,
  rune tiles, facets, bubbles, flowing stripes, cracks).

**Null-safe lookups** (`vfx/client/WikiTextures`): whether each of the 44 files exists is checked once, at resource load
(`VfxDefinitions.onResourceManagerReload` calls `WikiTextures.reload`, so F3+T and resource packs are honoured) or on first use, and
cached in arrays. The render path only reads the arrays; it never asks the resource manager. A missing file, or a failed lookup,
answers `VfxTextures.GLOW` with **additive** blend, so a wiki effect degrades to a plain glow and never shows the purple
missing-texture square or throws.

## 4. The temporary armour / aura (`Aura.WIKI`, `client/aura/WikiAura`)

The existing system is `PlayerAuras.set(player, Aura, ticks, style)` on the server and one registered `AuraPainter` per `Aura` on the
client (`PlayerAuraLayer` calls it for every player who has the aura, fading it in and out). Rather than 30 new enum entries,
**one** slot `Aura.WIKI` was appended and its **style is the magic's `WikiStyles` index**, so one painter serves all 30 and the
packet (`PlayerAuraPayload`, a byte of style) needs no change.

* **When:** `ElementBook.empower` (the Guard page of every wiki book, 180 ticks) calls
  `PlayerAuras.set(p, Aura.WIKI, ticks, WikiStyles.styleOf(magic))`, next to the Bone Armor precedent. The armour lasts as long as the
  guard and is dropped with the player's other auras on logout. Domain / Bolt do not add an armour (extension point: call the same
  line from any other page).
* **What** (colour = the magic's palette, counts by variant):

| Group | Armour |
|---|---|
| MOTES | a soft wind veil over the body, motes orbiting and rising (Snow falls, Smoke dusky), two streaks drifting off the shoulders |
| TRAILS | crescent blades orbiting the torso; Wing: a pair of feather blades beating on the back |
| CIRCLES | a rune circle at the feet, a tilted ring at the waist, a double halo behind the back |
| SHARDS | a faceted shell, crystal spikes on the spine, shoulders and forearm bracers |
| FLUID | a glossy gel shell that wobbles, drops falling from the hands (Mud brown); the Tongue lashes a ribbon |
| SPIRAL | ribbons winding up the body (a tight cone for Vortex), a faint flowing shell, a note / mote riding each strand |
| INFERNAL | a dark cracked shell with glowing fissures, a dark seal on the ground, a skull sigil behind, embers |

* **Budget:** at most two extra passes of the player model, a few dozen quads, **no allocation per frame** (`SealDraw` / `PropDraw.quad`
  take plain floats; textures come from the cached `WikiTextures` arrays).

## 5. The held cast key: root cause and fix

### What was wrong (traced in `book/GrimoireBook`)

1. **`onHeld` sent a new `MAGIC_CIRCLE` every 4 ticks** (`charge % 4 == 0`): a 6-tick circle that pops open with an overshoot
   (`VfxAnim.circleOpen`) and fades, each with a different size. Overlapping every 4 ticks, that is five pop-and-fade strobes a second.
   They also fill the client's 64 effect slots (`VfxManager.MAX_ACTIVE` evicts the oldest effect first).
2. It did so **even when the page could not be cast** (cooldown, sealed, no summoned book, weapon missing): holding or mashing a
   page on cooldown still strobed circles, and `onRelease` spawned another circle before `castPage` failed.
3. **Release spawned one more circle** next to the last charge circle (a second pop at the end of every hold).
4. On the body: `CastAnim.play(CHANT)` on the press and again on the release, and a `FAIL` flinch for every fizzle; the client restarted
   the clip on every message (`CastAnimClient.receive` set `start = now` unconditionally), so the arms snapped back and faded in
   again. A stop followed by a play within the fade also snapped from half-faded straight to a fresh clip.

`CastAnimClient` is therefore a contributor (the body half), but the runes themselves are the VFX path above.

### The fix (cause, not symptom)

An "active casting" state, started once, refreshed while held, ended with a fade:

* **`anim/CastHold`** (pure): per hold, the server state is `(level, sentAt)`. `held()` returns `START` on the first charged tick
  (>= 2 ticks), `LEVEL` when the charge reaches the next of 4 steps, `RENEW` every 30 ticks as a keep-alive, `END` on release or if
  the page stops being castable, `TAP` for a release that never showed a hold circle (the one release circle the cast always had).
  `allowed` (the page can be cast now) gates all of it, so a blocked page shows nothing. A 200-tick hold sends about a dozen messages
  instead of fifty circles.
* **`book/CastHoldFx`** (glue): keeps that state in the grimoire's skill tag (no static map to clean up) and sends the messages as
  `VfxShape.MAGIC_CIRCLE_HOLD` / `MAGIC_CIRCLE_HOLD_END` (appended to the enum) through the existing `VfxPayload` channel, so no new
  packet needs registering.
* **`GrimoireBook`** (3 small edits + `castReady`, a side-effect-free copy of `castPage`'s checks): `onPressed` resets the hold and
  skips a chant replay inside 3 ticks, `onHeld` calls `CastHoldFx.held`, `onRelease` calls `CastHoldFx.released` before its early
  return (so a hold ends even if an earlier cast is still winding up). The cast itself, its cost, cooldown, wind-up and `castPage` are untouched.
* **`anim/CastHoldView`** (pure) + **`vfx/client/layer/CastHoldLayer`**: per followed entity the client keeps one circle on the game
  clock. A message only renews it: the pop-open, the spin and the fade are never restarted, charge steps ease the size over 6 ticks,
  a release holds through the wind-up then fades over 6 ticks, a press while it fades brings it back without a pop, and a circle that
  hears nothing for 50 ticks (lost release, death, out of range) fades by itself. It draws with `MagicCircleLayer.drawCircle`, so it
  looks like every other grimoire circle.
* **`anim/CastAnimGate`** (pure) + **`client/CastAnimClient.receive`**: naming the clip that is already playing is ignored (the
  looping chant is never restarted; a one-shot repeated within 5 ticks is the same cast), and a looping clip that was fading out is
  resumed (eased up from where it was) instead of restarted.

## 6. Files

New: `vfx/WikiStyles`, `vfx/client/WikiTextures`, `client/aura/WikiAura`, `anim/CastHold`, `anim/CastHoldView`, `anim/CastAnimGate`,
`book/CastHoldFx`, `vfx/client/layer/CastHoldLayer`, `tools/gen_wiki_magic_textures.py`, 44 textures `wiki_*.png`, the tests
`vfx/WikiStylesTest`, `anim/CastHoldTest`, `anim/CastHoldViewTest`, `anim/CastAnimGateTest`.

Replaced: `vfx/client/layer/WikiMagicLayer` (the same three shapes and payloads; the style / resource selection and the drawing were
rewritten as described above).

Small additive edits: `vfx/VfxShape` (two entries appended), `vfx/client/VfxManager` (register `CastHoldLayer`),
`vfx/client/VfxDefinitions` (one line: `WikiTextures.reload`), `aura/Aura` (`WIKI` appended), `client/aura/AuraRegistry`
(`WikiAura.register()`), `book/ElementBook` (the `Aura.WIKI` line in `empower`), `book/GrimoireBook` (see 5),
`client/CastAnimClient` (the gate and the resume).

## 7. Checking it

* `python tools/gen_wiki_magic_textures.py --preview sheet.png` and look at the sheet.
* Unit tests (`gradlew test`): the four test classes above; `WikiStylesTest.everyTextureTheTableNamesIsGenerated` fails if a texture
  is missing, `tableMatchesTheWikiBooksAttributeSet` fails if a magic is added to `WikiBooks` without a place in the table.
* In game: `/vfx` the three `wiki_magic_*` shapes with each palette colour; hold a grimoire page and look for ONE circle that grows
  and fades; hold a page on cooldown and see nothing but the fail message on release; cast a wiki Guard page and look at the armour
  of each group; delete `wiki_*.png` from a resource pack and see plain glows instead of purple squares.
