# Holster harness, Julius's Time grimoire and Rouge: combined Blender blueprint (0.35)

References: the owner's belt-holster photos (a tan leather belt with a brass buckle and a hanging strap round the book; the
hooded cosplays wearing grimoire holsters at the hip and on the back), the in-game shot of a coverless page cylinder, and the
Clover Verse note that **Julius Novachrono's Time Magic grimoire is the only grimoire in the Clover Kingdom with no front or
back cover**. Rouge's own deep-dive is in `docs/rouge_spec.md`. Section 3 summarises it and adds how she sits with the other
two assets.

**In game (0.35):**
- **Harness:** every player with a bound grimoire wears a leather belt with a brass buckle. A strap hangs from the right hip
  and wraps the dormant book: down the front cover to a brass tip, over the top, down the back, with a keeper loop. It is drawn
  on top of the player (`GrimoireFloatClient.belt` / `bookStrap`) and uses no armour slot.
- **The Time grimoire** (Julius's canon book only; 0.37 briefly drew every Time grimoire this way, 0.38 reverted it) is drawn as the page drum of the
  owner's screenshot: a solid upright cylinder of about 96 cream pages packed edge to edge from the centre out, a ribbed band
  of page edges round the outside, and a radial fan of page tops. It is about 0.8 blocks across and stands off the leg at
  the hip. Summoned it stays upright beside you, glows, turns faster and flutters. Opening fans it slightly wider, and a
  spell switch sends a ripple round it (`juliusCylinder`).
- Textures: `tools/gen_harness_textures.py`. The page leaf is the grimoire's own `page_leaf.png`.

---

## 1. Modular harness & holster geometry (accessory layer)

### Hard-surface modelling
Units: metres, with the character at Minecraft scale (body 0.5 x 0.25 x 0.75 m).

1. **Belt band:**
   - Make a plane strip 0.04 m tall that wraps the waist as a rounded rectangle (0.54 x 0.31 m). Use Curve > Bezier with
     Extrude 0.02 and Bevel Depth 0.002, then convert to mesh.
   - Solidify the band by 0.004 m (leather thickness). Give it a Bevel modifier (2 segments, width 0.0015 m, limit by
     angle 40 degrees) so the burnished edges catch light.
   - Punch eyelets with a Boolean against 5 small cylinders (0.004 m radius) behind the buckle. Add a Bevel to each hole
     (1 segment).
2. **Buckle:**
   - Model a rounded rectangle frame (0.045 x 0.055 m) from a Circle of 16 vertices, scaled and inset, then Solidify by
     0.003 m. Add a cylinder tongue (prong) across it.
   - Brass tip on the belt end: a 3-vertex fan rounded with Subdivision level 2, then Solidify by 0.001 m.
3. **Hanging strap and keeper:**
   - A second band 0.035 m wide drops from a belt loop at the right hip, over the top of the book, down the front cover,
     through a keeper loop, and ends in a brass shield tip (as in the first reference photo).
   - Model it as a Curve with a 0.035 x 0.004 m rectangular profile (Geometry > Bevel > Object, using a rectangle profile
     curve) so its length can be re-routed.
4. **The holster pouch** (for the closed books): a five-sided box (open at the top) that hugs the book with 0.003 m of
   clearance. Bevel it at 0.002 m and lace the side seams with a curve array of cord loops: an Array of a small torus along
   the edge with a Curve modifier, 6 to 8 loops per side (the black lacing in the reference).
5. **Leather texture:** woven and stitched details are mostly in the shader (section 4). Model one row of stitches as an
   Array of tiny capsules along each edge, 3 mm apart and 2 mm in from the edge, so close-ups hold up.

### Slot-free integration (rigging)
- Give the harness its own **accessory bones**, parented to the character's spine and hip bones but in a separate bone
  collection (`ACC_harness`): `acc_belt` (child of `spine_01`), `acc_hip_R` (child of `thigh.R`, weight 0.3, and
  `spine_01`, weight 0.7, through a Copy Transforms blend so the strap swings between the torso and the leg), `acc_book`
  (child of `acc_hip_R`).
- Skin the belt **only** to `acc_belt` and the strap and pouch to `acc_hip_R` / `acc_book`. Nothing touches the body's
  deform bones, so the harness overlays any outfit or armour mesh without being part of it. This mirrors the game, where it
  is a separate render layer, not an armour item.
- Add a Shrinkwrap constraint target (`Body_Proxy`, a low-poly torso) with an offset of 0.006 m, so the belt follows
  breathing and crouching without clipping.
- Export the accessory armature as its own glTF / GeckoLib bone group (`harness`) so the game, or a GeckoLib player layer
  later, can toggle it independently.

## 2. Julius Novachrono's Time Magic grimoire (cylindrical, unbound)

### Mesh & array setup
The book is a **solid drum** of loose pages standing on end (0.37, after the owner's screenshot). There are no covers, no
spine and no binding. The pages are packed edge to edge from the centre out, so from above they read as a radial fan and
from the side as a ribbed wall of page edges. The drum is about 0.8 blocks across and a little shorter than it is wide.

- **One page:** a plane from the centre to the rim (0.4 m radial) x 0.33 m tall, subdivided 4 x 8 so it can bend. Origin at
  its inner edge (on the axis).
- **The ring (Geometry Nodes, `TimeGrimoire`):**
  1. Use a Mesh Circle of 96 vertices (radius 0.01 m: the pages meet at the axis) and Instance on Points the page object.
  2. Use Align Euler to Vector (X axis to the point's normal) so every page points outward.
  3. Use Realize Instances, then Set Position with an offset of Noise Texture (scale 3, W = `#frame / 90`) x 0.004 m along Z.
     Each page drifts slightly in height, as in the reference image.
  4. Store an attribute `page_id` (the instance Index) for the shader and the rig. Alternate the page tint by
     `page_id % 2` (`#F4ECD6` / `#E2D8BE`) so the ribbing reads.
  5. **Edge band:** a Cylinder (32 sides, no caps) at the page radius + 1 mm, with the `drum_edge` texture (vertical page
     edges, every other one darker, uneven tops and bottoms) wrapped once round.
- **Alternative without Geometry Nodes:** an Array modifier (count 64, Object Offset, an Empty rotated 5.625 degrees about
  Z), then Apply.

### Temporal animation rig
- **Drive the Geometry Nodes inputs** (Group inputs on the modifier, keyable):
  - `spin`: rotates the whole ring about Z; animate 0 to 360 degrees per 10 s at rest and per 2 s while casting.
  - `fan`: scales the instance positions outward, radius 0.022 to 0.04 m; the pages spread when the book opens. This
    matches the game's three opening steps.
  - `flutter`: the amplitude of a per-page Sine on the Z rotation, phase = `page_id` x 0.9. 0 at rest, 6 degrees while
    summoned.
  - `ripple`: a travelling wave of rotation round the ring, phase = `page_id` x 0.45 - `#frame` x 0.6, fading over 20
    frames; trigger it on each spell switch.
  - `compress`: scales the ring's height to 0.85 and radius to 0.9 for a "snap shut" between casts.
- **Bones** (for engines without Geometry Nodes): one root bone (`tg_root`) and 8 page-cluster bones arranged round it,
  each weighted to 8 neighbouring pages. Rotate the clusters with an Action that offsets 4 frames per bone for the flutter
  and ripple, and Scale the root for fan and compress.

## 3. Rouge (Thread Magic cat) & thread vectors

Summary of `docs/rouge_spec.md`:
- **Form:** a sitting chibi cat, about 0.35 m tall. The head is a sphere scaled 1.0 / 0.92 / 0.85, with tall four-sided ears
  rotated 12 degrees outward, a tapered cylinder body, straight front legs, a Bezier tail with a 0.018 m bevel curling up
  behind the right hip, and a torus collar. All quads, 4-6k triangles before subdivision, ear rims and paws creased to 1.0.
  Shape keys `happy` / `neutral` / `alert` are driven by a `mood` property.
- **Woven look:** two Wave Textures at +45 and -45 degrees, combined with Maximum, drive displacement (0.0015) and
  roughness, so she looks wound from thread rather than solid clay. The silhouette edge loops are duplicated as 0.0015 m
  thread curves.
- **Procedural threads:** a Geometry Nodes object (`RedThreads`):
  - Strands: 12 long, nearly horizontal Curve Lines through a 4 m volume, resampled to 64 points and noise-swayed, then
    Curve to Mesh with a 0.0025 m radius.
  - Crossing sparkles: Sample Nearest picks out the points where threads cross, and Ico Spheres are instanced there.
  - Fate orbs: 3-4 slow orbiting orbs.
- **Unravelling (the wiki's dodge):** a duplicate body with Mesh to Curve on the fibre seams and Trim Curve, with End and
  Set Position driven by `unravel` 0 to 1. Delete Geometry hides the solid body behind a gradient from the tail forward. A
  hooked Bezier from the tail tip (`Ally_Target` Empty) stretches one strand to an ally. Bake 6 shape keys for engines
  without Geometry Nodes.
- **With the other assets:** Rouge sits on the wearer's head (child of `head`, offset 0.02 m above the scalp). The
  `RedThreads` volume is parented to her, so the threads also cross the harness and the time grimoire, and Sample Nearest
  sparkles appear on them too. Exclude the harness from the thread collision so threads pass in front of it.

## 4. Shading, texturing & lighting (Cycles / EEVEE)

### Leather (`M_Harness_Leather`)
- **Principled BSDF:**
  - Base Color: a ColorRamp over a Voronoi texture (scale 220, F1, Smooth) for pores, mixed with a Musgrave / fBm (scale
    40) for mottling, from `#7A3418` (creases) through `#A8542A` to `#C8743E` (worn highlights).
  - Edge wear: Geometry > Pointiness (Cycles), or a baked curvature map, brightens the edges to `#D8955A` and lowers
    roughness there (burnishing).
  - Roughness 0.55 (body) to 0.35 (burnished edges). Coat 0.15 for a waxed finish.
  - Normal: Bump strength 0.25 from the pore Voronoi, plus a stitched groove from a Wave texture masked to the edge band.
- **Stitching (`M_Harness_Thread`):** Base `#E8D6AA`, roughness 0.7, slight Sheen 0.3.
- **Brass (`M_Harness_Brass`):** Metallic 1.0, Base `#C49640`, Roughness 0.28, with a cavity-masked darkening to `#5A4020`
  for tarnish in the recesses (from Ambient Occlusion > ColorRamp).

### Aged translucent paper (`M_TimeGrimoire_Page`)
- Base Color: `#F2E6C8` mixed with a Noise (scale 12) toward `#E0CFA4` for age, plus faint ink lines from a Wave texture
  (scale 60) at 0.08 opacity.
- **Translucency:** Subsurface Weight 0.15, radius (0.6, 0.5, 0.35) x 0.004, plus a Translucent BSDF mixed at 0.25, so
  the light behind the drum glows through the pages. Roughness 0.8.
- Page edges: a Layer Weight Facing mask lightens the edges to `#FFF4D8`.

### Mana & temporal glow
- **Rouge (`M_Rouge_Body`):** Base `#99102E` (grooves) to `#CC1C3F` (fibres); Subsurface 0.35, radius (1.0, 0.25,
  0.2) x 0.05, colour `#FF3050`; emission 0 to 4 (`#FF4060`) where the ambient threads pass through her (the
  `near_thread` attribute). Threads: Emission `#FF3355` at 6, with a Fresnel fade.
- **Julius's temporal aura (`M_TimeGrimoire_Glow`):**
  - Add Emission to the page shader, masked by `page_id` noise, with Emission `#FFF0C0` at 1.5 at rest and 6 while
    casting (drive it from the rig's `flutter` input).
  - A separate inner cylinder (radius 0.02 m, open ends) with Emission `#FFE9A0` at 12 behind the pages. It is the "core"
    the light shines out from, as in the anime frame.
  - A Volume Scatter shell round the drum (density 0.3, anisotropy 0.6) catches the god rays.
- **Shared compositor:**
  1. **Glare (Fog Glow)**, threshold 1.0, size 8, for the bloom round the threads, orbs and the grimoire core.
  2. **Glare (Streaks)**, threshold 4.0, 4 streaks at 45 degrees, fade 0.85, for the star glints at the thread crossings
     and the brightest page edges.
  3. Mix (Add), then **Color Balance**:
     - Night scene: lift toward violet `#3A2A5A` for Rouge's anime look.
     - Day scene: gain toward warm gold `#FFE0A0` for the Julius shot.
  4. EEVEE: Bloom on (threshold 1.0, radius 4, intensity 0.08), Soft Shadows, and Screen Space Refraction for the page
     translucency.

### Lighting
- **Key:** an area light from above and in front, 300 W, colour `#FFF2DE` for day or `#B8A0FF` for night.
- **Rim:**
  - Rouge's night shot: red (`#FF3050`, 150 W) from behind.
  - Julius's shot: gold (`#FFD070`, 200 W) under and behind the drum, so the pages glow through.
- **World:** HDRI at 0.4 for daylight, or a flat `#14102A` at 0.3 for night scenes. The harness reads best under the warm
  key, with the brass catching the rim.

---

## Game colours (keep the model matched)
| Part | Hex |
|---|---|
| Leather (texture base) | `#A85428` |
| Stitching | `#E8D6AA` |
| Brass | `#C49640` |
| Julius's pages (summoned tint) | `#FFF6D6` |
| Rouge body | `#CC1C3F` |
