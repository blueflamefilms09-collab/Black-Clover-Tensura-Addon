# Mirror Magic overhaul (0.42) - Gauche Adlai

Mirror Magic is no longer a utility book. It is a high-tier attribute built on four ideas from the Black Clover wiki:
mirrors **reflect and weaponise light**, they **make duplicates**, they **turn attacks back**, and they let you **travel from
glass to glass**. It is weak to Gel and Dark and counters Light.

## Pages

| Page | Status | What it does |
|---|---|---|
| Reflect Refrain | replacement | A real mirror hangs before you for 4 s. It catches 2-4 shots (by power), sends them back and puts `tensura:silence` on the shooter (magic jamming). |
| Real Double | replacement | 1-3 living doubles (by power) step out of mirrors in front of you for 60 s. Each costs 10% of your max magicules; if your magicules run short, the rest comes from your aura. Sneak-cast to trade places with the nearest double. |
| Reflect Ray | replacement | A volley of 3-5 rays. Each hit also takes 1 from the target's spiritual health (Tensura). Your doubles fire with you. |
| Large Reflect Ray | kept | Unchanged (wiki spell). |
| Full Reflection | replacement | For 8 s a great mirror turns back every shot and returns 60% of every blow to whoever struck. |
| Mirror Array | new | 3-5 mirrors of mixed shapes orbit you for 40 s. They catch and redirect shots, and one shatters in place of any blow that would take 35% of your health or kill you. |
| Mirror Step | new | You step into a mirror and out of another where you look (up to 32 blocks). |
| Mirrors Slash | new | Blade mirrors spin out in an arc: 9 x power damage plus `tensura:fragility`. |
| Mirrors Meteorite | new | A storm of mirrors falls where you aim: 8 x power each, plus blindness and `tensura:fragility`. |

## Real Double (entity `nusmp:mirror_double`)

- **Look:** it wears the owner's own skin, mirrored left to right (the wiki says the clone is the inverted reflection), holds the owner's weapon, and has a pulsing violet glass sheen (`client/MirrorDoubleRenderer`).
- **Stats:** health is half the owner's max health x power, and attack is 3 + 3 x power. It copies the owner's active effects (`CloneEntity.copyEffects` when Tensura is present).
- **As in Tensura's Body Double:**
  - it costs a tenth of your magic power;
  - it takes 50% extra damage;
  - it fights until the end;
  - you can swap places with it.
- **Combat:**
  - It follows you, defends you and attacks what you attack.
  - It fires a Reflect Ray every 3.5 s.
  - When you cast any page, every double echoes it with a ray at your target.
  - Half of the mobs targeting you switch to a double when it appears.
- **Sharing hits:** while doubles are out, 35% of the blows aimed at you land on a double instead. Each double adds +20% to your damage, up to 3 doubles ("twice the power" in canon).
- **When it ends:** it shatters into glass when killed, when its time runs out, or when its owner dies.

## Scaling

`MirrorWorks.power(e) = clamp(0.8 + max(0, log10(EP) - 1.5) * 0.24 + armour / 60, 0.8, 2.4)`

- EP comes from Tensura (`IExistence.getEP`), so a demon lord's mirrors hit harder than a new mage's.
- Armour adds a little power, so equipment counts.
- The tier (0/1/2) sets the counts: doubles 1-3, array mirrors 3-5, rays 3-5, shots caught 2-4.

## Status effects

| Effect | Source |
|---|---|
| `tensura:silence` | Any shot a mirror sends back silences its shooter (magic jamming). |
| Spiritual damage | Reflect Ray takes 1 from the target's spiritual health per hit. |
| `tensura:fragility` | Mirrors Slash and Mirrors Meteorite. |
| Blindness | The meteorite's glare. |

Effects are looked up by id, so the book still works without Tensura. Trnightmare's effects are not on this project's build path, so none are used. If that mod is added later, its ids can go through `MirrorWorks.tensuraEffect` in the same way.

## VFX (`vfx/client/layer/MirrorLayer.java`, textures from `tools/gen_mirror_vfx_textures.py`)

Every summoned mirror is drawn back to front:

1. two counter-rotating **mana coronas** (violet and deep violet, flickering);
2. the **glass** (cool sheen band and vignette, tinted lilac);
3. a **distortion ripple** expanding across the glass;
4. the **silver frame** (bevel, bead row, crest flourish);
5. an additive **violet edge glow**.

Frame shapes: oval, arched rectangle, round, gothic crest and diamond. The shape is picked from the instance seed, so an array is always a mix (as in the reference art).

Shapes: `MIRROR_FRAME` (one mirror), `MIRROR_ARRAY` (an orbiting ring), `MIRROR_SHATTER` (glass shards and a fading corona), `MIRROR_STEP` (a double stepping out: silhouette, prismatic red-to-violet light streaks, ground ring).
Preview: `python tools/vfx_preview/preview.py mirror_frame mirror_array mirror_shatter mirror_step`.

## Blender / shader guidance (for a 3D mirror model or a shader pack)

These steps are optional. The in-game VFX already use the textures above.

1. **Frame:**
   - Draw the outline as a curve: an oval, an arch (a rectangle plus a half-circle), or an ogee crest.
   - Add depth with Extrude 0.08 and Bevel 0.02, using 3 segments for a rounded lip.
   - For the bead row, put an Array modifier on a small UV sphere along the curve (Curve modifier), with a count of about 36.
   - Model the top crest from two mirrored Bezier scrolls with a Solidify modifier.
   - Material: Principled BSDF, Metallic 1, Roughness 0.25, base colour #B8B4CC. Add a Noise texture into Roughness (scale 40, mix 0.1) to suggest chased silver.
2. **Glass:**
   - Use a plane inset 0.01 behind the frame lip.
   - Material: Metallic 1, Roughness 0.02, base colour #DCD2FF.
   - Add a Layer Weight (Facing) node into an emission of #B89CFF at 0.4, so the edges glow violet.
   - Export it as a separate mesh so a renderer can treat it as a reflective surface.
3. **Corona:**
   - Use a camera-facing plane behind the mirror with an emission shader.
   - Feed a radial gradient times a Voronoi texture (randomness 1, animated W) into a ColorRamp, from transparent to #B89CFF to #7A4CFF.
   - Set the blend mode to Additive.
4. **Ripple:**
   - Animate a Wave texture (bands, rings) with a phase driven by frame, plugged into the glass normal at strength 0.05.
5. **Photon / Iris shader packs:**
   - Mirrors are emissive translucent quads, so they bloom under Photon.
   - For true reflections, put the glass on a block or entity with a material id the pack marks as reflective. Keep the frame on its own render type so it is not blurred.
6. **Export:**
   - glTF 2.0, with scale applied and +Y up.
   - For Blockbench/GeckoLib, triangulate and keep it under ~800 faces per mirror. Bake the silver into a 128px texture.
