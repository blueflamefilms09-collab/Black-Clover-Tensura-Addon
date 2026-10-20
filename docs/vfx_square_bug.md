# The square bug (release 1.105.0)

Owner report: "fix the weird square bug on magic circles and spells". Three screenshots of game 1.100.0.

## What the screenshots show

* **Two shots of an Erza armour just put on** (an angel-white set, and the red-and-gold Flame Empress): under the wearer lies a flat, evenly lit **white** (first) or **pale yellow** (second) square, with the
  ring of the magic circle only faintly visible inside it. That is the ground ring of the equip flash (`ErzaAura.extras`, "a ring of light opening at the feet over the first 16 ticks, then a slow turning
  ring": `ErzaAura.java:106-109`), drawn as a flat quad by `SealDraw.flat` (`SealDraw.java:56`). The ring texture (`particle/flamefx_ring`) is a ring on a transparent background; the square is the whole
  quad lit, background included. The bright diamonds on the chest and wrist of the same two characters are the same thing: `SealDraw.glow` sprites (camera-facing glints and motes) drawn as solid quads.
* **One shot of the Dark Cloaked Dimension Slash**: soft round blue puffs (correct) and big pale polygons with stair-stepped edges over the sky. The puffs are `VfxBloom.glow` quads of the
  effect engine's vertex buffer, textured with `particle/glow`; they are round and soft, so **the engine's alpha and blend work in that very frame**. The pale polygons are not VFX quads: a VFX quad
  textured with `glow` cannot come out as a polygon, and their edges step in whole texels of the cloud layer. They match vanilla's cloud layer seen from below (the camera looks straight up along
  the body during the cast). This is stated as a reading of the picture, not proven (no game here); nothing in the spell's layers produces a quad of that size or shape.

So there are two different things: the **ground and glint squares on the auras are a real defect** (fixed below), the **sky polygons are very probably vanilla clouds** (nothing to fix in the effect engine).

## The real defect

Vanilla's `RenderType.eyes` (what `AuraRender.additive` returned, `AuraRender.java:26`) adds the texture's RGB to the screen and does **not weigh it by the texture's alpha**: black costs nothing, so vanilla's
eye textures are black outside the eyes. The mod's sprite textures (rings, sigils, glints, glows) are painted the other way round: **colour everywhere, alpha shapes the sprite** (the generator comment
in `tools/gen_body_textures.py`: "lum is defined everywhere, also under alpha 0, so edges never get dark fringes"). `air_ring.png` is white under its alpha-0 corners, `flamefx_ring.png` grey (158),
`particle/glow.png` white. Drawn with `eyes` every transparent texel adds its colour, so the whole quad lights up: a solid square of the ring's colour times the vertex colour. White for the air and
white-flash ring, pale yellow when a gold tint is added over sand. The previews of the mod (`tools/render_preview`, which add `rgb * alpha`) show the intended look, so nobody saw the squares
before running the game.

Audit (`tools/vfx_square_bug/png_audit.py`): of the 346 sprites the aura and prop painters ask for plus the ring/circle/glow sprites of `textures/particle`, **251 carry a colour under their transparent texels**
(the ones from the 1.93 magic rebuilds on: air, ash, flame, water, lightning, gravity, memory, mirror, permeation, slash rip, smoke, snow, soul, sound, spatial, vortex, wing, dark, scale, beast, and the old seal
sprites), 86 are black under alpha (nature rebuild, hair, nail, spike, shakudo...: fine), 9 are opaque (haze and shell skins of the body on black, fine). All of the ring sprites have transparent corners
(alpha 0): the PNGs are right, the blend was wrong.

## What the engine (VFX vertex buffer) does: proven not to be the cause

The brief's first suspect was the 1.100.0 performance pass of `VfxVertexBuffer`. `tools/vfx_square_bug/run.py` compiles the buffer of the 1.93 base commit (`40e74fa`) and the current one against fake
`RenderSystem` / `BufferBuilder` classes (`tools/vfx_square_bug/fakes`), runs the same 5-frame scenario through both (all shapes, six textures including a Time Magic one, every blend, equal-but-not-identical
location objects, a frame whose state was dirtied first, a sweep of all 256 alpha values) and compares, per draw, the texture, the blend factors and every vertex bit for bit with its RGBA bytes.
Result: **135 draws and 16432 vertices, identical in the old and the new buffer; every one of the 256 alpha bytes comes out.** The performance pass kept the batch key (texture and blend), the draw order and
the colour packing. The only difference a draw can see is the one added here (the colour modulator is reset per batch).

## The fix

| Where | What |
|---|---|
| `client/NURenderTypes.java:54-72` | new render type `additiveAlpha(tex)`: `RenderType.eyes` (same shader, format, texture state, colour-only write mask) with the blend **`SRC_ALPHA, ONE`**: transparent texels add nothing, the vertex alpha fades the sprite (what the painters assume) |
| `client/AdditiveTypes.java` | picks the type per texture (cached per location, cleared on a resource reload) from `AdditivePolicy.choose(TextureAudit.kind(tex), vfx.safeBlend)` |
| `client/aura/AuraRender.java:26` | `additive(...)` goes through `AdditiveTypes`: every aura and prop painter (253 call sites) is covered by one line |
| `client/playermodel/ZagredAuraLayer.java` | its `koto_ring` sprite (a direct `eyes` call) goes through `AdditiveTypes` |
| `vfx/TextureAlpha.java` (pure) | classifies a texture: `COLOUR_UNDER_ALPHA` (square-prone), `BLACK_UNDER_ALPHA`, `OPAQUE`, `OPAQUE_BRIGHT_EDGE`, `EMPTY` |
| `vfx/AdditivePolicy.java` (pure) | the decisions: only `COLOUR_UNDER_ALPHA` or an unreadable texture takes the alpha-weighted type, **a texture that is black under alpha keeps vanilla's `eyes` exactly as before**, `vfx.safeBlend` forces the alpha-weighted type for all; which texture is swapped for the soft circle (missing; or an additive flat sprite that came out opaque); Time Magic never |
| `vfx/client/TextureAudit.java` | reads a PNG once through the resource manager (sampled to 512), caches the kind, logs a missing texture once |
| `vfx/client/VfxVertexBuffer.java:267-279` | defensive, per batch: sets shader, **colour modulator (1,1,1,1)**, depth mask, cull, depth test, texture and blend itself (nothing a draw before it left behind reaches it); the bound texture goes through `textureGuard` |
| `vfx/client/VfxTextureGuard.java`, `SoftCircleTexture.java`, `vfx/SoftCircle.java` | a texture that does not exist, or an additive flat sprite (named ring/circle/glow...) that came out opaque, is drawn as a generated 64x64 soft white disc with transparent corners (a dynamic texture, registered once) instead of the missing-texture checker or a solid square |
| `vfx/client/VfxSafeBlendConfig.java`, `NUSMP.java` | client config `config/nusmp-vfx-client.toml`, `vfx.safeBlend` (default `false`): `true` forces the alpha-weighted blend for every additive aura sprite and also swaps any additive flat sprite that is opaque |
| `vfx/client/VfxDefinitions.java`, `VfxClientEvents.java` | the audit, guard and render-type caches are emptied on a resource reload; the guard is installed |

Why not repaint the textures (black under alpha)? The effect engine blends them as straight alpha with linear filtering, where the colour under alpha is what keeps the edges from getting a dark fringe: it
would trade the aura squares for dark rims on all of the VFX layers.

Why default `false`? The 86 textures that are black under alpha (and the skins) stay on exactly the render type they had, so nothing that looked right changes. `true` is for a machine where something else is wrong.

### Time Magic

`TimeMagicLayer`, the `time_*` textures, the generator and the `TimeBook` calls are untouched. The layer draws through `VfxVertexBuffer`: the harness scenario includes `time_sparkle`, and old and new
output are bit-identical; the guard never touches a `time_*` file (`AdditivePolicy.isTimeMagic`, tested, even for a missing or opaque one); `SquareBugAssetsTest.theTimeMagicLayerDoesNotKnowAboutTheFix`
fails if the layer ever mentions the fix.

## Tests and checks

* JUnit (run, all pass): `vfx/TextureAlphaTest`, `AdditivePolicyTest`, `SoftCircleTest`, `SquareBugAssetsTest` (the PNG audit: every square ring/circle/sigil/ripple/halo/glow sprite of
  `textures/particle` has transparent corners, the listed square-prone sprites are classified as such, every sprite an aura or prop painter asks for exists, the batch key includes texture and blend and each
  batch sets its state, the aura type goes through the audit); `perf/PerfSourceScanTest` still passes (no per-quad allocation came back).
* `python3 -B tools/vfx_square_bug/run.py [--joml <jar>]`: the old/new buffer comparison above (needs a JDK and the JOML jar).
* `python3 -B tools/vfx_square_bug/png_audit.py [--all]`: the PNG audit as a table.
* Self-test `vfx.square_bug` (server-safe: textures from the jar and the pure classes): the ring and glow sprites have transparent corners and are classified as colour-under-alpha, the choice follows
  the audit, the safe path forces the alpha-weighted blend, the soft circle is a disc, Time Magic is exempt.
* Type-check against the API stubs: clean except two known stub gaps on lines that were already there (`RenderSystem.setShader`, `setShaderTexture`); negative control caught.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `additiveAlpha` render type, `AdditiveTypes`, `TextureAlpha`, `AdditivePolicy`, `SoftCircle`, `SquareBugAudit`, `TextureAudit`, `VfxTextureGuard`, `SoftCircleTexture`, `VfxSafeBlendConfig` (`nusmp-vfx-client.toml`), self-test `vfx.square_bug`, tests, `tools/vfx_square_bug/*`, this document |
| REPLACED | `AuraRender.additive` (was `RenderType.eyes(t)`, now the audited choice); the colour/state handling at the start of `VfxVertexBuffer.flush` (per batch instead of once) |
| KEPT | every texture, every effect, `RenderType.eyes` for all textures that are black under alpha and for all direct `eyes` uses outside the aura/prop painters (glow skins of armour and weapons: relics and weapon renderers are not touched), the Time Magic VFX, batch order and vertex output of the buffer |

## What could not be verified (no game here)

* That vanilla 1.21.1's additive transparency is `ONE, ONE` (alpha ignored). This is from memory of `RenderStateShard`; it is the only explanation that fits both the uniform squares (white for a white-under-alpha ring, tinted pale yellow over sand) and the faint ring visible inside them. The fix does not depend on it: the new type states its blend explicitly (`SRC_ALPHA, ONE`) whatever vanilla's is.
* That the sky polygons of the Dimension Slash shot are clouds. If they are not, they are not produced by `VfxVertexBuffer` (proven identical to the 1.93 output) and `vfx.safeBlend`, the missing-texture guard and the per-batch state reset are the available defences; a screenshot with clouds off (Video Settings) would settle it.
* The look of the first frame after a resource reload (the audit reads a PNG on the render thread once per texture: a few hundred microseconds each).
* A shader pack (Iris/Oculus) maps `eyes` to its own program and may draw the new type differently; no pack was available.
* The `NativeImage.getPixelRGBA` / `DynamicTexture` calls type-check against the stubs; they have not run.
