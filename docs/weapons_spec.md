# Black Clover weapons: concept spec (0.28)

These are built from the owner's weapons art pack (Asta's black swords and their red anti-magic variants; Licht's white
Demon-Dweller and Demon-Destroyer; Yami's katana) and from the Black Clover wiki (Anti Magic, Sword Magic, Yami Sukehiro).
The in-game sprites come from `tools/gen_weapon_textures.py`. The Blender notes are for anyone who wants a full 3D version
later, for example a GeckoLib or OBJ model or a render.

Shared shader setup (Blender, Principled BSDF):

- **Demon steel (black):** Base Color #1F1F24, Metallic 1.0, Roughness 0.55. A Noise Texture (scale 40) through a
  ColorRamp drives Roughness 0.45-0.75. A second Noise (scale 8) masks a rust colour (#52301A, Metallic 0) mixed in at about
  30%, which gives the dirty, battle-worn look.
- **Edge wear:** Geometry > Pointiness (Cycles), or a baked curvature map, through a ColorRamp mixes bright steel
  (#6B6B75, Roughness 0.3) onto the edges and chips.
- **Anti-magic glow (red / black):** the Emission colour is #E01020 at strength 3-8. A Wave Texture along the blade (bands)
  times a Noise drives Emission strength so the aura "crawls". Add a separate shell mesh (Solidify 2 mm, flipped normals off)
  using Transparent BSDF + Emission with a Fresnel mask for the outer haze. In EEVEE, turn on Bloom.
- **Mana glow (Licht, white-gold):** the Emission colour is #FFE9A0 at strength 2-4, masked by the marking texture only.
- **Darkness coat (Yami):** a shell mesh with Volume Absorption (density 3, black) plus a Noise-driven Emission (#2A1040,
  strength 0.5). Animate the noise W input for a slow smoky drift.

General hard-surface workflow for every blade:

1. Block out the silhouette from a plane: trace the reference in front view, then use Extrude and Solidify for thickness
   (about 6-10 mm, game scale x1.5).
2. Make the edge bevels by scaling the edge loop on the cutting edge to 0 along the blade's thickness axis. That gives a
   true V grind.
3. Add support loops (Ctrl+R) 1-2 mm from every hard corner, or use a Bevel modifier (Limit: Angle 30 degrees, 2 segments,
   Harden Normals). Then apply Subdivision level 1-2. Do not subdivide the cutting edge: keep it crisp with weighted normals.
4. Cut the fullers and grooves with Boolean (Difference) using a simple cutter, then clean up with Remesh off and manual
   loops. The alternative is Inset + Extrude inward on a pre-planned face strip.
5. Model the guard and pommel separately (mirror modifier on X). Join them only for export.
6. Unwrap. Mark seams along the spine and the edge, and give the blade's flat faces one large UV island each so the
   rust/scratch noise stays unstretched.

---

## 1. Demon-Slayer Sword (Asta)

**Name & classification:** Demon-Slayer Sword. Anti-magic greatsword (a grimoire-bound demon sword), two-handed.
Rarity: Epic. In-game item `demon_slayer_sword`.

**Visual description:** A massive, broad, dirty black greatsword, as tall as its wielder. The edges are chipped and nicked,
rust is blooming on the flat, and the tip is pointed. The base of the blade angles inward where it meets the hilt, and a
narrow fuller runs from the hilt into the blade. It has a short, thick square crossguard, a long wrapped grip and a round
pommel. When anti-magic flows, a black-and-red haze wraps the blade, crackling like torn paper at the edges.

**Magical attributes & abilities:**
- Passive: the flat of the blade reflects spells. Each hit strips a beneficial effect and builds anti-magic amp.
- **Black Divider** (right-click): bats every nearby spell back at its caster, then a huge 6-block anti-magic sweep.
- Grimoire page *Black Divider*: a bigger, 9-block version. Also used for **Black Meteorite** and **Bull Thrust**.

**Blender breakdown:**
- Topology: one quad strip for the blade, 12-16 loops along its length, more near the chips. Blade thickness tapers from
  14 mm at the spine to 0 at the edge.
- Chips: use the Knife tool for irregular notches in the edge loop, then pull them in 3-8 mm. Add a Bevel modifier
  (1 segment) so they catch light.
- Inward base: move the first two edge loops above the guard inward by about 25% so the blade "steps" down into the
  ricasso.
- Fuller: a long, narrow Boolean cutter, 40% of the blade length, 3 mm deep.
- Guard: a cube with bevelled edges (0.15 m x 0.04 m x 0.05 m) and a Bevel modifier (3 segments). Pommel: UV sphere,
  12 segments, slightly flattened.
- Shaders: Demon steel with heavy rust (mix 45%) and Edge wear. The anti-magic glow shell only shows while it is active.

## 2. Demon-Dweller Sword (Asta)

**Name & classification:** Demon-Dweller Sword. Anti-magic longsword that absorbs magic, one-handed. Epic.
`demon_dweller_sword`.

**Visual description:** A long, slender black blade tapering to a needle point. A stepped central fuller runs down it,
flanked by thin black markings that light up in the colour of whatever magic the sword absorbed. It has a four-sided ornate
guard (a diamond with flared points across the blade and short points along it), a spiral-wound grip and a sphere pommel.

**Magical attributes & abilities:**
- Passive: absorbs magic it touches. Hits strip a buff.
- **Black Slash** (right-click): a flying anti-magic slash that erases spells in its 16-block path and knocks the target
  back.
- Grimoire page *Black Slash*: Asta's flying-slash version.

**Blender breakdown:**
- Topology: a hexagonal cross-section (diamond with a flat fuller). 24 loops along the length so the markings can be
  modelled or projected.
- Fuller: Inset the central face strip twice and push each inset 1 mm deeper to make the stepped groove.
- Markings: a separate emission texture (black chevrons on a transparent background) projected onto the flats. The glow
  colour is driven by a single Color attribute so it can be animated to the absorbed magic.
- Guard: start from a cube, then use Extrude and Scale on four faces to make the four points. Mirror on X and Y. Use
  Bevel with Harden Normals.
- Spiral grip: a cylinder with a Screw modifier on a small rectangle profile (screw 0.12 m, 6 iterations) wrapped around
  it. Pommel: an ico sphere at subdivision 3.

## 3. Demon-Destroyer Sword (Asta)

**Name & classification:** Demon-Destroyer Sword. Anti-magic broadsword with the power to break causality. Epic.
`demon_destroyer_sword`.

**Visual description:** A broad black blade with a ragged chunk torn out of one edge near the guard. The blade end flares
much wider than the rest and carries a three-leaf clover symbol before narrowing to a point. A dark channel runs from the
guard to the clover. The guard is short and arrow-shaped, and the pommel is round.

**Magical attributes & abilities:**
- Passive: hits strip three buffs at once.
- **Causality Break** (right-click): undoes spell effects. It cleanses curses and harmful effects from you and allies, erases
  spells around you, and strips every buff from foes close by.

**Blender breakdown:**
- Topology: a blade plane with extra loops in the flared end (it is the widest part). Shape it with Proportional Editing
  (smooth falloff) from a straight blade.
- Bite: a Boolean Difference with a jagged cutter mesh (a sculpted, low-poly torn shape). Then re-bevel the new edge.
- Clover: three cylinders and a stem curve, combined and used as a shallow Boolean (1 mm) or as a Displacement plus
  emission mask.
- Shader: Demon steel with medium rust. The clover takes a faint red Emission (strength 0.5) that rises to 4 while active.

## 4. Demon-Slasher Katana (Asta)

**Name & classification:** Demon-Slasher Katana. Anti-magic katana. Epic, grimoire-bound. `demon_slasher_katana`.

**Visual description:** A slim, curved black katana. The cutting edge is lined in crimson anti-magic and the guard (tsuba)
is round and black. The grip is wrapped in red over black. In use, the red edge flares into a long crescent of red-black
light.

**Magical attributes & abilities:**
- **Infinite Slash** (right-click): a large overhead flying anti-magic slash that cuts every spell in a 20-block line,
  strips a buff and drains 5% magicule from everything hit.
- Grimoire pages *Infinite Slash* (24 blocks) and *Infinite Slash Equinox* (48 blocks, never harms allies).

**Blender breakdown:**
- Topology: make a straight blade with a shinogi ridge (a 5-sided cross-section), then curve it with the Simple Deform
  (Bend) or Curve modifier for the sori.
- The red edge is a separate face strip (Select edge loop, then Inset) with its own material: Emission #E01020 at strength 3.
- Tsuba: a cylinder (24 sides), Solidify, with an Inset hole for the blade. Ito (grip wrap): use an array of flattened
  diamonds along a curve, or a normal-mapped texture on a simple cylinder.

## 5. Yami's Katana

**Name & classification:** Yami's Katana (item id `miasma_infused_katana`, kept for old worlds). A custom-made katana from
the Land of the Sun (Hino Country) and the conduit of Dark Magic. Rare.

**Visual description:** A plain, well-used curved steel katana. It has a wavy hamon (temper line) along the edge, a dark
round tsuba, and a black-and-white diamond-wrapped grip. With Dark Cloaked spells it is coated in a smoky, light-eating
darkness that stretches past the real blade.

**Magical attributes & abilities:**
- Passive: hits leave the target in darkness.
- **Dark Cloaked Dimension Slash** (right-click): a 16-block cut of darkness that cuts spells and space first.
- Dark Magic grimoire: Avidya Slash, Avidya Wild Slash, Black Hole, Black Blade (+reach), Black Moon (Mana Zone), Iai
  Slash, Death Thrust (needs Black Moon), Dimension Slash: Equinox.

**Blender breakdown:**
- Same base as the katana above. The hamon is a texture mask (a Wave Texture with distortion 3) mixing polished steel
  (Roughness 0.15) with satin steel (Roughness 0.4).
- Darkness coat: duplicate the blade, scale it 1.4 along its length and 2 across it, then apply the Darkness coat shader
  (volume + noise emission). For Black Blade, scale it 1.8 along the length.

## 6. Licht's Demon-Dweller Sword (white)

**Name & classification:** Licht's Demon-Dweller Sword. A Sword Magic longsword, drawn from the grimoire for 60 s. Epic.
`licht_dweller_sword`.

**Visual description:** It has the Demon-Dweller's silhouette in white steel. The markings are gold-white, and the
four-sided guard and sphere pommel are pale gold. The spiral grip is cream and white. It is clean, bright and unworn.

**Magical attributes & abilities:**
- **Conquering Eon** (right-click): a 20-block slash that grows with every ally near you, and heals you.
- Sword Magic page *Demon-Dweller Sword: Conquering Eon*: the full 28-block version.

**Blender breakdown:** reuse the Demon-Dweller mesh. Use white steel (Base #D6DAE0, Metallic 1, Roughness 0.25), gold trim
(#D9CC9E, Metallic 1, Roughness 0.3), and the Mana glow (white-gold) on the markings.

## 7. Licht's Demon-Destroyer Sword (white)

**Name & classification:** Licht's Demon-Destroyer Sword. A Sword Magic broadsword, drawn from the grimoire for 60 s.
Epic. `licht_destroyer_sword`.

**Visual description:** It has the Demon-Destroyer's silhouette (torn bite, wide blade end) in white steel, with a pale-gold
clover and channel and a gold guard and pommel.

**Magical attributes & abilities:**
- **Causality Break** (right-click): strips every buff from foes in front of you and erases their spells nearby.

**Blender breakdown:** reuse the Demon-Destroyer mesh with the white steel and gold trim materials. The clover uses the Mana
glow at strength 2.

---

## Spell list after the wiki (0.28)

| Book | Page (slot) | Notes |
|---|---|---|
| Anti Magic | 1 Black Slash, 2 Black Divider, 3 Black Meteorite, 4 Black Hurricane, 5 Black Asta, 6 Bull Thrust, 7 Infinite Slash, 8 Infinite Slash Equinox | No magicule cost. Black Asta: twice a day; a third use brings weakness. |
| Sword Magic (Licht) | 1 Origin Flash, 2 Origin Flash Barrage, 3 Conquering Eon, 4 Demon-Dweller Sword, 5 Demon-Destroyer Sword | Pages 4-5 draw Licht's white swords for 60 s. |
| Dark Magic (Yami) | 1 Avidya Slash, 2 Black Hole, 3 Dimension Slash, 4 Death Thrust, 5 Black Blade, 6 Avidya Wild Slash, 7 Black Moon, 8 Iai Slash, 9 Dimension Slash: Equinox | Death Thrust and Iai Slash are stronger under Black Moon; Death Thrust spends it. |
