# Transformation armour overlays: Blender blueprint (0.38)

References: the owner's mode pack (`Black_clover_modes.zip`). It covers Yuno's Wind Spirit Dive (fur-lined green coat, a
crown of star blades, wind wings, the bow), Fuegoleon's Salamander Spirit Dive (clawed flame gauntlets, the dragon frame,
flame hair), Asta's Black Asta / Demon Mode (one horn, one tattered wing, black pixel wisps; the Minecraft shot with the
cube particles), Luck's lightning armour (the white-and-gold Minecraft armour with bolts) and Noelle's Valkyrie Dress (blue
crystal plates, the avian water wings, the drill lance).

**In game (0.38):**
- **Five overlays, no armour slot.** The overlays are temporary and state-driven. They appear on the instant the spell is
  cast and drop when the mode ends.
- **How they are drawn.** The client render layer `client/mode/ModeArmorLayer` draws each overlay over whatever armour or
  skin you wear, bound to the player model's bones, so it follows every animation.
- **How the mode is tracked.** The server keeps the mode and its end time on the player (`blackclover/ModeArmor`). A sync
  packet (`ModePayload`) tells the caster and everyone tracking them. Players who come into range later get it too, and it
  also arrives on login.
- **Textures:** `tools/gen_mode_armor_textures.py` writes them to `textures/entity/mode`.
- **Preview without the game:** `python tools/mode_preview/preview.py` runs the real layer and renders front, back and side
  views.

| Mode | Turned on by | Lasts |
|---|---|---|
| Wind Spirit Dive (Yuno) | Spirit Dive on the Wind book; Spirit of Zephyr | the dive (8 s) / Zephyr (30 s) |
| Fire Spirit Dive (Salamander) | Spirit Dive on the Fire book | the dive (8 s) |
| Anti-Magic Demon Mode (Asta) | Black Asta; the Anti-Magic Spirit Lord's Black Form | 30 s / while the form is on |
| Lightning God Mode (Luck) | Thunder Fiend; Black Lightning Battle Fiend | 30 s |
| Valkyrie Dress (Noelle) | Valkyrie Dress | 30 s |

An early end also removes the overlay: recasting the dive, losing the spirit, the AMP running dry, or death.

---

## 1. Modular topology & layering

### Base rig and sockets
- **Base mesh:** model on a Minecraft-proportioned player rig.
  - Bones: `head` (pivot at the neck, 0/24/0 px), `body` (pivot at the neck), `arm_R` (-5/22/0), `arm_L` (5/22/0),
    `leg_R` (-1.9/12/0), `leg_L` (1.9/12/0).
  - Units are Minecraft pixels (1 px = 1/16 block = 0.0625 m), so the numbers below are the same numbers as
    `ModeArmorLayer`.
- **Sockets:** add one Empty per overlay group, parented to its bone, with Child Of influence 1.
  - `sock_head_crown`: 1.2 px above the head top.
  - `sock_head_horn`: left forehead, at (2.4, -7.4, -2.2) in head space.
  - `sock_back_wings`: the upper back, at (+/-1.6, 2, 2.3) in body space.
  - `sock_hand_R`: the right palm, at (-1, 10.5, -1) in arm space.
  - `sock_waist`: body y 10.
- **Every overlay piece is a separate object parented to a socket, never to the base mesh.** The game draws pieces per bone
  in the same way: `ModelPart.translateAndRotate`, then the piece.

### Non-slot accessory overlays
- **Shells:** overlays are shells inflated 0.3 to 0.9 px off the body (a box cage around each bone), never deformations of
  the skin. This way they sit over any armour or Tensura gear.
- **One object per material slot:**
  - `plate`: opaque cutout.
  - `glow`: the same shell inflated a further 0.05 px, additive emission.
  - `wing` / `crest`: alpha-cut cards, translucent or emissive.
- **Name pieces `<mode>_<part>_<bone>`**, for example `wind_cuff_arm_L`. The exporter maps bone → `ModelPart`.

### Low-poly + stylised normals
- **Shells are cuboids:** 6 quads each. Do not bevel them. Paint a 1-texel dark rim and a 1-texel highlight into the
  32 px texture instead (that is what the generated `_plate` textures do).
- **Spikes and claws:** 4-sided pyramids (4 tris).
- **Horns:** chains of 4 shrinking cuboids, each rotated 6-16 degrees further.
- **Lance:** an 8-sided cone.
- **Cards:** single quads (wings, crowns, flame strands, helmet wings, skirt plates, coat tails), double-sided (no back-face
  culling).
- **Stylised normals:**
  - On the shells, set Shade Flat. Minecraft lights per face.
  - For renders, add a Weighted Normal modifier (Face Area, weight 50, Keep Sharp) on the horn and lance only.
  - Bake a 32 px tangent normal map from a high-poly sculpt only if you want render shots. The game uses none.
- **Budget:** about 60-120 quads per mode in game (the preview prints the counts: wind 73, fire 81, demon 119 with the
  wisps, lightning 59, valkyrie 90). Keep Blender game-export versions under 150.

## 2. Per-mode asset breakdowns

### Wind Spirit Dive (Yuno)
- **Coat:**
  - A body shell (-4.45..4.45, -0.2..12.3, -2.45..2.45).
  - Three hanging tails (back 8 px, two front halves 7 px with a 1.2 px gap). Hinge the tails on the hem edge with one
    bone each and drive the flare from run speed: in game, flare = 6 + 2.5 x limb swing.
- **High collar:** a 4-piece fur ring that stands 3.6 px above the neck at the back and 2.2 px at the front, plus fur cuffs
  (2.2 px tall) on both forearms. Fur is a separate material (`wind_fur`), a white/mint noise. For renders, add a Hair
  particle system (length 0.6 px, clumping 0.6) on the collar only.
- **Floating crown:** 8 star-tipped blades on a ring of radius 4.6 px, alternating 7 and 5 px tall. The ring floats 1.2 px
  above the head, bobs 0.5 px and turns 1.2 degrees a tick.
- **Wind wings:** two cards, 27 x 25 px, rooted at the shoulder blades, swept back 30 degrees. The beat is 9 degrees at
  0.18 rad/tick. The texture is 5 blade feathers fanned over 90 degrees, with bright feather edges.

### Fire Spirit Dive (Salamander)
- **Asymmetric gauntlets:**
  - Right: heavy (-3.8..1.8 x 4.2..10.7), with a spiked pauldron (two spikes, the bigger one 5.5 px) and 4 claws 4.2 px
    long.
  - Left: lighter (-1.5..3.5 x 5.6..10.5), with a flat pauldron and 3 claws.
  - Claws grow from 0 over the 6-tick materialise.
- **Dragon frame:** two wing cards, 18 x 18 px. The texture is 4 bones plus a flame membrane, heat-graded yellow at the
  root to orange-red at the edge. Swept back 38 degrees, beat 7 degrees.
- **Flame hair:** 7 strands round the back half of the head, leaning out 25 degrees, each flickering 5.5 +/- 1.8 px.

### Anti-Magic Demon Mode (Asta)
- **Horn:** one horn on the left forehead. It is 4 cuboid segments (3.2 → 2.3 px long, width x 0.74 each) plus a tip
  pyramid, curving out (Z +18, then +16 / +6 per segment) and back (X -12, alternating -6 / +8), so it reads as jagged.
- **Tattered wing:** a single wing on the left shoulder only, 24 x 23 px, opaque cutout. The texture is 4 bones, a
  scalloped torn membrane, holes and a dark red rim. Swept back 32 degrees, slow 6-degree beat.
- **Black arm:** the right arm in an obsidian shell with molten red veins (additive).
- **Wisps:** 14 small dark cubes (0.9-1.4 px) that rise from the feet past the head on a spiral, each growing and shrinking
  over its rise. This is the "floating dark pixel" cloud of the Minecraft reference.

### Lightning God Mode (Luck)
- **Plating:**
  - Chest plate (-4.6..4.6, -0.5..7.6).
  - Shoulder plates (6 x 4.7 x 5.8) with two swept spikes each (4.8 / 3.6 px, raked outward).
  - White enamel with gold runic chevrons.
- **Circuit glow:** an additive layer of runic circuit lines pulsing 0.65 ↔ 1.0 at 0.9 rad/tick.
- **Energy crown:** 7 zig-zag bolt cards on a radius-4.4 ring. Every 2 ticks each bolt gets a new random height
  (4.5-8 px), yaw jitter (14 degrees), lean (8-20 degrees) and mirror, so the crown crackles.

### Valkyrie Dress (Noelle)
- **Crystalline water armour:** translucent (alpha 0.88), faceted texture.
  - Chest plate.
  - 6 skirt plates flaring 22 degrees from the waist, more when running.
  - Gauntlets with pauldrons.
  - Greaves on both legs.
  - Glow on the edges.
- **Helmet wings:** two small wing cards (6.5 px) at the temples, swept back.
- **Avian water wings:** 29 x 27 px. The texture has 3 layered feather rows (pale to deep blue) with white quills. Swept
  back 34 degrees.
- **Drill lance:**
  - An 8-sided cone (radius 2.9, length 24 px) on `sock_hand_R`, pointing forward along the hand.
  - A square guard and a grip running back past the hand.
  - It spins 24 degrees a tick about its axis.

## 3. Shaders, emission & scrolling textures

### Blender (EEVEE for look-dev, matching the game)
- **Plate material:**
  - Image Texture `_plate` with **Closest** filtering, into Principled BSDF.
  - Roughness: 0.55 for coats, 0.3 for metal and crystal.
  - Metallic: 0.8 for Luck's enamel, 0 elsewhere.
  - For the toon step, add Shader to RGB → ColorRamp (Constant: 0.35 / 0.7 / 1.0).
- **Glow material (`_glow` shells and crests):**
  - Emission, strength 4, colour from the texture.
  - Alpha from the texture alpha.
  - Blend Mode Additive (EEVEE: Material > Blend Mode Alpha Blend, plus an Add Shader with Transparent BSDF).
  - In game this is `RenderType.eyes`: additive and full-bright.
- **Translucent material (wind and valkyrie wings, crystal plates, lance):**
  - Mix the Principled BSDF with a Transparent BSDF by texture alpha x 0.75-0.9.
  - Add Emission 1.5 in the wing colour, so the wings read as energy, not glass.
  - In game: `entityTranslucent` at full-bright light.
- **Scrolling and animated textures:**
  - **Wind wings:** add a Mapping node with Location X = `#frame * 0.01` on a second, noise-alpha texture multiplied into
    the wing alpha. This makes gusts run along the feathers.
  - **Fire membrane:** use a Noise Texture (scale 6, detail 4) with W = `#frame / 20` to drive the Emission strength
    (2-6) and a Hue offset (0 to 0.04).
  - **Lightning crown:** in Blender, swap between 4 bolt textures every 2 frames with an Image Sequence (cyclic). In game
    the bolt geometry is re-rolled every 2 ticks instead.
  - **Lance:** add a Mapping Rotation Z of `#frame * 0.42` (24 degrees a frame). The stripes are painted diagonally, so the
    spin reads as a drill.
  - **Minecraft animated textures:** for a resource-pack variant, give a texture a `.png.mcmeta` with
    `{"animation":{"frametime":2}}` and vertically stacked frames. The generated textures are single frames, and the motion
    comes from geometry.
- **Bloom:**
  - EEVEE Bloom: threshold 0.9, radius 4, intensity 0.08.
  - Cycles: Compositor Glare, Fog Glow, threshold 1.0, size 7.
  - Add a Streaks glare (4 streaks) only for the lightning crown.

### Colours
| Mode | Plate | Glow |
|---|---|---|
| Wind | `#469650` coat, `#E6C85A` buttons | `#AAFFBE` |
| Fire | `#AA281E` scales, `#F0B446` rim | `#FFAA32` |
| Demon | `#1A161E` obsidian | `#C81428` |
| Lightning | `#DCDCE6` enamel, `#FFE678` runes | `#FFEB78` |
| Valkyrie | `#5AAFEB` crystal, `#D2F5FF` facets | `#C8F5FF` |

## 4. Minecraft integration

### Models (JSON / Blockbench)
- **Hand-off format:** the game draws the overlays from code (`ModeArmorLayer`) for exact bone binding with no extra model
  loading. For artists, the hand-off is a Blockbench **Modded Entity** project with one group per bone (`head`, `body`,
  `right_arm`, `left_arm`, `right_leg`, `left_leg`) and the pivots above. Each overlay piece is a cube or plane inside its
  group.
- **Export:**
  - Export JSON (Java Block/Item model per bone group, or a GeckoLib `.geo.json` for an animated version).
  - Copy the cube bounds into `ModeArmorLayer`'s `box(...)` / `card(...)` calls. The numbers are the same pixels.
- **GeckoLib route** (for animated wings in a later version):
  - A `GeoArmorRenderer`-style render layer that is *not* registered as armour.
  - It is drawn by the same layer, with bones named as above, so the vanilla bone transforms are copied onto the Geo bones
    each frame.

### Animation state triggers
- **Server:** spells call `ModeArmor.start(player, Mode, ticks)` and `ModeArmor.stop(player, Mode)`.
  - Spirit Dive: `GrimoireBook.spiritDive`, and its recast / upkeep end.
  - Spirit of Zephyr: `WindBook.zephyr`.
  - Valkyrie Dress: `WaterBook.valkyrieDress`.
  - Thunder Fiend / Black Lightning: `WikiSpells`.
  - Black Asta: `CanonSpells.blackAsta`.
  - Black Form: `AntiMagicLordSkill`, refreshed every second while toggled on and stopped on toggle-off or when the AMP
    runs dry.
- **State:**
  - Persistent data `nusmp_mode_armor` (ordinal) and `nusmp_mode_armor_until` (game time).
  - `ModeArmor.Mode` is append-only, because the ordinal goes over the network.
- **Sync:** `ModePayload(entityId, mode, ticks)`, sent to the player and to everyone tracking them. It is re-sent on
  StartTracking and on login, and mode 0 clears it.
- **Client timing (`ModeArmorClient`):** it keeps start and end per entity.
  - The layer **grows the pieces in over 6 ticks**: claws, horn, wings, crown and lance scale from 0.
  - It **fades them over the last 10 ticks**.
  - A refresh of the same mode keeps its start, so Black Form's per-second refresh never re-grows it.

### Client renderer layers (instant swap on cast)
- **Registration:** `ModeArmorLayer` is added to both player skins (wide and slim) in `EntityRenderersEvent.AddLayers`.
- **Each frame it:**
  1. Reads the mode for the player's entity id.
  2. Enters each bone with `translateAndRotate`.
  3. Draws three passes:
     - `entityCutoutNoCull(<mode>_plate)` at world light;
     - `entityTranslucent(...)` at full-bright for wings and crystal;
     - `eyes(<mode>_glow / _crest)` for the additive emission.
- **The swap is instant:** the layer reads the synced mode on the next frame after the cast packet, and any previous mode
  is replaced, because a player wears one mode at a time.
- **Not drawn:** while the player is invisible, or in first person (vanilla draws only the arm there).
