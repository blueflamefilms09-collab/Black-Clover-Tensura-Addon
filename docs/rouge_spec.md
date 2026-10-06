# Rouge (Red Thread of Fate): Blender build blueprint (0.34)

Reference: the owner's Rouge screenshots and the Black Clover wiki entry for *Red Thread of Fate*. In the series, Rouge is a
high-drain supplementary Thread Magic construct of Vanessa Enoteca's. She is a cat made of condensed crimson thread that
rewrites unfavourable outcomes into favourable ones for the people Vanessa cares about. She can unravel parts of her body to
dart about, dodge attacks, or stretch strands out to distant allies to widen her field.

In game (0.34) Rouge is the **Red Thread of Fate: Rouge** page of the Thread grimoire:

- **Watch:** she watches you for a day.
- **Save:** if a blow would kill you, she ties your thread back to where you stood 4 s earlier, at half health.
- **Dodge:** each hit has a 12% chance to make her unravel and pull you out of it.
- **VFX:** the `ROUGE_CAT` VFX draws her sitting on your head inside a web of crossing red threads, with sparkles where they
  cross. When she saves you, she unravels.

The model below is the high-detail version, for a GeckoLib entity later or for renders.

---

## 1. Asset blocking & stylised topology

### Proportions & silhouette
- **Pose:** sitting upright, front legs straight and together, haunches tucked, tail rising from behind the right hip and
  curling up at the tip. Head is large (about 0.45 of body height, chibi proportions), with tall pointed ears. In the anime
  she is drawn very flat and graphic, so keep forms simple and readable.
- **Scale:** sitting height 0.35 m, the size of a house cat perched on Vanessa's head.
- **Blocking:**
  1. Head: a UV sphere, 16 segments x 12 rings, scaled 1.0 / 0.92 / 0.85 (slightly wider than tall).
  2. Ears: two cones of 4 vertices each (a pyramid with a square base), rotated 12 degrees outward. Bevel the tips with 1
     segment so they stay crisp.
  3. Body: a cylinder of 12 sides and 6 loops, tapered at the shoulder (scale the top loop to 0.7) and widened at the
     haunch.
  4. Front legs: two cylinders of 8 sides each, flattened at the paw.
  5. Tail: a Bezier curve with a 0.018 m bevel depth and 4 bevel resolution, converted to mesh after posing.
  6. Collar: a torus at the neck seam (32 x 8 segments, minor radius 0.006 m). In the reference it is a darker red band.
- **Topology:** all quads. Edge loops follow the eye sockets, the mouth line and the ear bases so the face can deform.
  Target about 4-6k triangles before subdivision. Add a Subdivision Surface modifier (viewport 1, render 2). Set crease to
  1.0 on the ear rims and paw bottoms so they stay sharp under subdivision.
- **Face states:** three shape keys on the head.
  - `happy`: closed eyes as two downward arcs, open smiling mouth with tongue (the third reference image).
  - `neutral`: open round eyes, small cat mouth (first and second images).
  - `alert`: ears forward, pupils narrowed.
  Drive them from a single custom property, `mood` (0 / 1 / 2).

### Thread-bound construction (she must look woven, not clay)
- **Grain grooves:** UV unwrap with seams down the spine and under the chin. Then apply a Wave Texture (Bands, Sine, scale
  80, distortion 2) along a second UV map rotated 45 degrees. Drive a Displacement node with it (Midlevel 0.5, Scale
  0.0015) so fine diagonal fibre grooves run across the surface.
- **Crossing fibres:** add a second Wave Texture rotated -45 degrees and take the Maximum of the two. That gives
  intersecting fibre paths, as if the body were wound from two directions. Use the same mask, lightly, on roughness (the
  fibres are glossier than the gaps).
- **Seams of thread:** along the silhouette edges (ear rims, the back of the legs, the tail), duplicate the edge loops into
  curves. Give them a 0.0015 m bevel and a Spiral modifier-like twist (Curve > Geometry > Twist) so they read as single
  threads binding the outline.

## 2. Procedural Geometry Nodes & thread vector rigging

### Ambient intersecting strands (the red lines crossing the space round her)
Geometry Nodes on an empty mesh object called `RedThreads`:
1. Distribute Points in Volume (a 4 m cube around Rouge), 12 points, seeded. These are the strands' midpoints.
2. For each point, a Curve Line whose direction is a random unit vector, biased toward horizontal (multiply Y by 0.35, then
   normalise), and whose length is 8 m. The strands are long and nearly straight, as in the screenshots.
3. Resample Curve to 64 points. Offset them with Noise Texture x 0.05 so the threads sag and wave slightly. Animate the
   noise W with `#frame / 240`.
4. Curve to Mesh with a Curve Circle of radius 0.0025 m and 6 resolution. Set Material to `M_RedThread_Emissive`.
5. **Crossing sparkles:** Mesh to Points on the thread vertices. For each point, use Sample Nearest against the other
   threads and keep the points within 0.03 m (the intersections). Instance an Ico Sphere (radius 0.01) on them and give it
   the `M_Sparkle` material. These are the star glints where threads cross.
6. **Fate orbs:** 3 to 4 Ico Spheres (radius 0.06) on a slow orbit (Transform with Rotation = `#frame * 0.01`), using
   `M_FateOrb`. These are the soft red lights in the second reference image.

### Unravelling / dynamic deformation (the wiki's dodge)
- **Rig:** a simple quadruped armature (spine x 3, neck, head, 2 ears, 4 legs x 2 bones, tail x 6) with Automatic Weights,
  then cleaned up by hand around the ears and tail.
- **Unravel mesh:** duplicate the body. On the copy, add a Geometry Nodes modifier:
  - Mesh to Curve on the fibre seams.
  - Trim Curve, with End driven by `unravel` (0 to 1).
  - Set Position, offsetting each curve point along its tangent times `unravel` x 0.6 m plus Noise x 0.2 m.
  
  The copy shows only while `unravel > 0`: use Delete Geometry on the solid body with a mask that grows from the tail
  forward (a gradient along the spine compared against `unravel`).
- **Strand stretch to allies:** add a curve hook. A Bezier curve from the tip of the tail is hooked to an Empty
  (`Ally_Target`), and its Bevel Factor End is animated 0 to 1, so one strand shoots out and attaches to an ally.
- **Shape-key fallback** (for the GeckoLib export, which has no Geometry Nodes): bake 6 frames of the unravel into shape
  keys `unravel_1..6` and step through them.

## 3. Shader development & lighting (Cycles / EEVEE)

### `M_Rouge_Body`: mana-core subsurface
- **Principled BSDF:**
  - Base Color: a ColorRamp from `#99102E` (in the grooves) to `#CC1C3F` (on the fibres), driven by the fibre mask.
  - Subsurface Weight 0.35, Subsurface Radius (1.0, 0.25, 0.2) x 0.05, Subsurface Color `#FF3050`. Light bleeds red
    through the ears and tail.
  - Roughness 0.45 (gaps) to 0.3 (fibres).
  - Coat Weight 0.1, so the woven surface has a faint sheen.
- **Emission boost at the crossings:** take the fibre mask's highlights (ColorRamp, black at 0.85, white at 1.0) times
  the strand proximity. That is an Attribute from Geometry Nodes, `near_thread`, written with Sample Nearest distance.
  Feed it to Emission Strength 0 to 4 with Emission Color `#FF4060`. Where the ambient threads pass through her, her body
  glows.
- **Eyes, mouth and collar:** a separate material, darker (`#5A0818`), roughness 0.6. The mouth interior is `#E07A80` with
  a pink tongue.

### `M_RedThread_Emissive` (the strands)
- Emission `#FF3355`, strength 6. Mix with a Transparent BSDF via the Fresnel of a Layer Weight (blend 0.3), so the
  threads thin out at grazing angles.

### `M_Sparkle` and `M_FateOrb`
- Sparkle: Emission white, strength 30. In EEVEE, enable Bloom (threshold 1.0, radius 4, intensity 0.08).
- Fate orb: Emission `#FF2A3A`, strength 8, plus a Volume Scatter shell (density 0.5) for the soft halo.

### Compositor: specular bloom & star highlights
1. Render Layers > **Glare (Fog Glow)**: threshold 1.0, size 8. This gives the soft red bloom round the threads and orbs.
2. A second **Glare (Streaks)**: threshold 4.0, 4 streaks, angle offset 45 degrees, fade 0.85. Only the brightest
   crossings get star sparkles, as in the screenshots.
3. Mix (Add) both onto the image. Finish with **Color Balance** (lift slightly toward violet `#3A2A5A`) to match the
   anime's purple night lighting.

### Lighting
- Key light: an Area light from above (the spotlight column in the first two references), colour `#B8A0FF`, power 300 W,
  size 0.5 m.
- Rim light from behind in red (`#FF3050`, 150 W) to separate her silhouette.
- World: dark violet `#14102A`, strength 0.3. The threads and orbs are the main light sources.

---

## Game colours (match the model)
| Part | Hex |
|---|---|
| Body (VFX tint) | `#CC1C3F` |
| Thread highlight | `#FF5070` |
| Sparkle | `#FFFFFF` |
| Fate orbs | `#CC1C3F` (glow) |
