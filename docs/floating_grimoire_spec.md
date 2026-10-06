# Low-profile floating grimoires: Blender blueprint (0.36)

References: the owner's floating-grimoire screenshots (a slim open book lying at a steep tilt low beside the mage, swirl
particles round it; an open book drifting at the mage's side; the classic two-handed hold in front of the chest). The
covers, emblems and motifs come from the art-pack grimoire system (`GrimoireBookPlan`, `BookLook`, `CanonBook`).
Companion documents: `docs/grimoire_harness_julius_rouge_spec.md` (the holster, Julius's coverless drum, Rouge) and
`docs/rouge_spec.md`.

**In game (0.36):**
- **Thinner book:** the grimoire geometry is ultra-thin. Board to board it is 2.8 model units (was 5); the boards are 0.5
  thick and the page block 1.8.
- **Flatter opening:** when summoned it opens almost flat (half-angles 55 / 30 / 14 degrees across the three opening steps;
  the old V was 62 / 46 / 34).
- **New hover position:** it floats low beside the owner's right side (0.78 right, 0.82 up, 0.5 forward), lying open at a
  steep tilt: pitched 55 degrees so the pages face up at the owner, yawed 28 degrees inward, rolled -10 degrees.
- **Glowing runes:** while it is held open, full-bright runes in the book's trim metal glow on both pages.
- **No armour slot:** it is drawn by its own renderer from the Grimoire Slot. The dormant book hangs in the holster
  harness (0.35).

---

## 1. Ultra-thin geometry & tilted profile

### Low-poly hard-surface modelling (Blockbench or Blender, in Minecraft model units: 1 unit = 1/16 block)
1. **Boards:** two boxes 11 x 13.4 x 0.5 units (x 2.5-13.5, y 1.3-14.7; back board z 6.6-7.1, front board z 8.9-9.4).
   Keep them as plain cuboids, with no bevel geometry. The "sharp angular bevel" is a 1-texel darker rim painted into the
   cover texture. Real bevels would add 4x the quads and shimmer at item scale.
2. **Page block:** one box 10.5 x 12.8 x 1.8 (z 7.1-8.9), inset 0.2 from the boards on the three open sides. Only the
   top, bottom and fore-edge faces are needed. The two faces against the boards are never seen, so delete them.
3. **Spine:** a box 0.8 x 13.8 x 3.0 (z 6.5-9.5) whose front protrudes 0.1 past the boards, with three metal bands
   0.5 units tall at y 3.95 / 8.0 / 12.05.
4. **Cover relief:** a frame of 4 bars, 0.45 wide and 0.3 proud, inset 0.4. The emblem medallion is a 4 x 4 x 0.35 plate
   with the emblem quad 0.02 above it, so it never z-fights.
5. **Open book (the summoned state):** split the book at the spine into two halves. Each half is a board (7.6 wide, 0.45
   thick) plus a page block (0.9 thick). Hinge both on a vertical axis at (8, *, 11), and rotate each half back by the
   step's angle (55 / 30 / 14 degrees). Duplicate every face reversed: double-sided, so no viewing angle culls a half.
6. **Budget:** under 140 quads closed and under 420 open, including the back faces. The test suite checks both.

### Rotation & tilt pivot setup
- **Origin:** put the object origin at the spine's centre (8, 8, 11) for the open book, and at the board centre (8, 8, 8)
  for the closed book. The tilt then pivots round the spine, not a corner, so the book "settles" open and doesn't swing.
- **Hover rig (Blender):**
  - Parent the book to an Empty `grimoire_anchor` that follows the player root with a Child Of constraint, influence 1, on
    location and the Z rotation only, so the book turns with the body but doesn't pitch with it.
  - On the anchor, set Location (right 0.78, up 0.82, forward 0.5) and Rotation (yaw 28 degrees, pitch 55 degrees,
    roll -10 degrees). These are the same numbers as `GrimoireCarry.HAND` in game.
  - Add a Noise modifier on the anchor's Z location (strength 0.03 m, scale 40 frames) and Y rotation (strength 4 degrees),
    for the bob and sway.
  - For the trip from the hip, keyframe the anchor between the hip pose (0.42, 0.55, 0, yaw -90, scale 0.84) and the hover
    pose over 12 frames with an ease-in-out cubic F-curve. Add a 0.18 m upward arc at mid-trip (a Z offset keyed at frame 6),
    exactly as the game's trip.
- **First-person variant:** a second anchor parented to the camera (forward 1.35, right 0.75, down 0.42), pitched +22
  degrees so the pages face up at the eye.

## 2. Modular variant architecture

### Standard Clover grimoires
- **One master mesh with material slots for every variant:** cover leather, frame metal, medallion, emblem, pages, spine.
  The variants are texture and tint data, not separate meshes.
- **Emblems:** the clover emblems (3-leaf / 4-leaf / 5-leaf, the suits, cracked variants, Black Magic, God-Tier) are 64 px
  alpha-cut textures on the emblem quad.
  - In Blender, use an Image Texture with Alpha Clip on a 4 x 4 plane, parented to the medallion.
  - The 5-leaf emblem sits on a tattered cover (`cover_tattered`), with no frame bars, for Asta's grimoire.
- **Cover designs:** the art-pack motif layer (`cover_art_<tier>.png`) is a second UV layer over the leather, tinted by
  the cover colour. In Blender, Mix it over the leather with Multiply at 1.0, then Overlay at 0.3 for the sheen.
- **Tints:** cover colour, trim colour and emblem colour are vertex-colour channels (layers 0, 1 and 2 in game). In
  Blender, use Color Attributes `tint_cover` / `tint_trim` / `tint_emblem`, each multiplied in its material.

### Special variants
- **Julius's Time grimoire:** swap the master mesh for the coverless page drum (Geometry Nodes ring of 44-64 radial pages;
  see the harness / Julius / Rouge blueprint). It keeps the same anchor, so it hovers and travels exactly like a book.
- **Thread constructs (Rouge):** an accessory socket, the Empty `acc_head_socket` on the wearer's head bone. Rouge parents
  to it, and her `RedThreads` Geometry Nodes volume parents to her, so the threads cross the floating book as well.
- **Variant switch:** a single Geometry Nodes switch on the book object, `variant` = 0 (book) / 1 (drum), driven from a
  custom property. In game this is the `Canon` tag (`julius` → drum).

## 3. Non-armour-slot entity binding
- **In game:**
  - The grimoire lives in the Grimoire Slot, a player attachment, not armour.
  - The client renderer (`GrimoireFloatClient`) draws it in world space every frame, from synced data: the hip stack,
    the summon state and the flip events.
  - It never uses an armour or curio slot, and it works with any armour, Tensura gear or skin layer.
- **For a GeckoLib or entity version later:** a client-only "companion" render that holds no entity on the server. Bind it
  by owner UUID with tags `grimoire.owner`, `grimoire.state` (`hip` / `travel` / `summoned`), `grimoire.variant`
  (`book` / `drum`) and `grimoire.open` (0-3), and compute the transform from the owner's body yaw. Using no entity avoids
  collision, AI and chunk-saving costs.
- **Export:**
  - glTF of the master mesh with its three tint attributes and one armature bone per half (`half_L`, `half_R`) for the
    opening.
  - Name the hover anchor `grimoire_anchor`, so an exporter or GeckoLib animation can drive the same pose numbers as the
    game.

## 4. Stylised texturing & shading

### Pixel-art / clean low-poly look
- **Filtering:** set every Image Texture to **Closest** interpolation. Bake at 16 px per model unit and never mip the
  emblem.
- **Shading:**
  - Principled BSDF with Roughness 0.8 for leather and paper, and Metallic 1.0 / Roughness 0.35 for trim.
  - Flat shading on the boards (Shade Flat). Minecraft's quad lighting is flat per face, so smooth normals would mismatch.
  - Fake the bevels with a 1-texel rim, 30% darker, painted into each texture.
- **Light ramp:** in EEVEE, add a Shader to RGB → ColorRamp (Constant interpolation, 3 stops: 0.35 / 0.7 / 1.0) after the
  BSDF, for a stepped toon look that matches pixel art.

### Emissive runes on open pages (the cast state)
- Add a third texture on the page surfaces, `page_runes.png`: a magic circle and rune lines, white on transparent.
- Material:
  - Mix Shader: page diffuse ↔ Emission, using the rune texture's alpha as the factor.
  - Emission colour = the trim tint (gold for most books, silver for cold and metal magic, the anti-magic dark red for
    Black Magic covers). Emission strength 4.
  - Drive strength with `cast_state` (0 dormant, 1 summoned): 0 → 4 over 6 frames.
- The page block's own emission (the "held glow") is a separate 0.6-strength warm `#FFF4D8` emission. It glows only while
  summoned.
- **Bloom:** EEVEE Bloom on (threshold 0.9, radius 3, intensity 0.06). Cycles: Compositor Glare (Fog Glow, threshold
  1.0, size 6).
- **Swirl particles** (the reference's floating spirals): a particle system on the anchor, 6 billboards of a spiral sprite
  orbiting at 0.8 m, emission tinted by the magic's glow colour. In game the summoning effects already supply the motes.

### Colours
| Part | Hex |
|---|---|
| Pages | `#F2E6C8` |
| Held page glow | `#FFF4D8` |
| Runes (gold trim) | `#E0B04A` |
| Runes (silver trim) | `#C8D4E0` |
| Leather / trim / emblem | per book (`BookPalette`, `CanonBook`) |
