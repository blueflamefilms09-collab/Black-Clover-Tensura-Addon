# Tripo3D prompts: Saiyan hair, armour and aura (for the Multiverse mod)

Owner request: prompts to remake every Saiyan hair, the armours, and a cooler aura / charge-up. Nothing here is in the mod yet; these are what to feed Tripo3D, plus how each result gets used.

## How to use these
- **Mode:** text-to-model first. If a result is off, switch to image-to-model with a Tripo output you liked as the reference (keeps the style identical across the set).
- **Always add (suffix):** `stylized game asset, clean topology, low poly under 3000 triangles, single mesh, centered, plain white background, no ground, no text, no watermark`
- **Style anchor (prefix for everything):** `Stylized anime-inspired game asset, flat-shaded with a soft rim light, bold readable silhouette,`
- **Hair is made as a separate wig mesh** (head-shaped cap + hair, no face, no neck), 8 cube-units wide at the head, so it sits on the Minecraft head. Ask Tripo for `a wig on a plain featureless mannequin head, front view, no face` and delete the mannequin in Blender.
- **Export:** GLB, then Blender to OBJ/JSON for the mod. Keep each hair under 1500 triangles (the mod draws it for every nearby player, so smaller is cheaper).
- **Colours** below are the mod's table values (hex), so the new hair keeps the same palette.

## 1. Hair (16 forms)
Each prompt: prefix + this line + suffix.

**Kaioken** (`kaioken`, hair #1A1A24, 8 strands)
> A wig on a plain featureless mannequin head, front view, no face: wild upswept black hair, flushed red energy, base colour #1A1A24, 8 separate sharp strands, hair length about 34 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**False Super Saiyan** (`false_super_saiyan`, hair #D0B040, 9 strands)
> A wig on a plain featureless mannequin head, front view, no face: dull straw-gold spiky hair, slightly unruly, base colour #D0B040, 9 separate sharp strands, hair length about 36 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan** (`super_saiyan`, hair #FFD93A, 10 strands)
> A wig on a plain featureless mannequin head, front view, no face: upright gold spiky hair in a few sharp flame-shaped clumps, base colour #FFD93A, 10 separate sharp strands, hair length about 42 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan Full Power** (`super_saiyan_full_power`, hair #FFE266, 10 strands)
> A wig on a plain featureless mannequin head, front view, no face: upright gold spiky hair, a little calmer and brighter, base colour #FFE266, 10 separate sharp strands, hair length about 42 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan 2** (`super_saiyan_2`, hair #FFE850, 12 strands)
> A wig on a plain featureless mannequin head, front view, no face: taller, sharper gold spikes with lightning-bright tips, base colour #FFE850, 12 separate sharp strands, hair length about 50 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan 3** (`super_saiyan_3`, hair #FFC82A, 15 strands)
> A wig on a plain featureless mannequin head, front view, no face: extremely long gold hair falling to the knees, with one tall central spike and long side locks, no eyebrows, base colour #FFC82A, 15 separate sharp strands, hair length about 145 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan 4** (`super_saiyan_4`, hair #14101A, 12 strands)
> A wig on a plain featureless mannequin head, front view, no face: long wild black spiky hair, swept back, with a dark crimson sheen, base colour #14101A, 12 separate sharp strands, hair length about 75 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Full Power Super Saiyan 4** (`super_full_power_super_saiyan_4`, hair #1C1420, 13 strands)
> A wig on a plain featureless mannequin head, front view, no face: longer wild black-violet spiky hair, swept back, base colour #1C1420, 13 separate sharp strands, hair length about 85 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan 4 Limit Breaker** (`super_saiyan_4_limit_breaker`, hair #241018, 14 strands)
> A wig on a plain featureless mannequin head, front view, no face: very long wild dark-plum hair with magenta glints, swept back, base colour #241018, 14 separate sharp strands, hair length about 100 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan God** (`super_saiyan_god`, hair #E02424, 9 strands)
> A wig on a plain featureless mannequin head, front view, no face: upright crimson-red spiky hair, smooth and clean, sleeker than Super Saiyan, base colour #E02424, 9 separate sharp strands, hair length about 40 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan Blue** (`super_saiyan_blue`, hair #38B4FF, 9 strands)
> A wig on a plain featureless mannequin head, front view, no face: upright electric-blue spiky hair, smooth and clean, base colour #38B4FF, 9 separate sharp strands, hair length about 40 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Perfected Super Saiyan Blue** (`perfected_super_saiyan_blue`, hair #7CD6FF, 9 strands)
> A wig on a plain featureless mannequin head, front view, no face: upright ice-blue spiky hair with a faint silver sheen, base colour #7CD6FF, 9 separate sharp strands, hair length about 40 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Super Saiyan Blue Kaioken** (`super_saiyan_blue_kaioken`, hair #38B4FF, 9 strands)
> A wig on a plain featureless mannequin head, front view, no face: upright electric-blue spiky hair with red-pink energy streaks, base colour #38B4FF, 9 separate sharp strands, hair length about 40 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Ultra Instinct Sign** (`ultra_instinct_sign`, hair #9AA4C0, 9 strands)
> A wig on a plain featureless mannequin head, front view, no face: silver-grey spiky hair, a few strands lifting, base colour #9AA4C0, 9 separate sharp strands, hair length about 40 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**Perfected Ultra Instinct** (`perfected_ultra_instinct`, hair #DCE6FF, 11 strands)
> A wig on a plain featureless mannequin head, front view, no face: long flowing silver-white spiky hair, loosely swept back, base colour #DCE6FF, 11 separate sharp strands, hair length about 55 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

**True Ultra Instinct** (`true_ultra_instinct`, hair #FFFFFF, 11 strands)
> A wig on a plain featureless mannequin head, front view, no face: long flowing pure-white spiky hair with a faint glow at the edges, base colour #FFFFFF, 11 separate sharp strands, hair length about 60 cm of strand on a head of 50 cm, clean hard-edged stylised anime hair clumps, subtle darker roots and brighter tips.

## 2. Armour and outfits
The mod has no Saiyan armour yet, so these are new. All are worn on a standard Minecraft biped (head 8, body 12x8x4, arms 4x12x4, legs 4x12x4 pixels). Ask for `T-pose, front view, on a blocky humanoid mannequin, torso/arms/legs as separate parts`.

1. **Saiyan Battle Armour (classic)**
> Stylized anime-inspired game asset, a Saiyan battle armour set on a blocky humanoid mannequin, T-pose: white chest plate with rounded shoulder guards and a skirt-like waist guard, yellow trim, black stretch bodysuit under it, white gloves and boots with yellow cuffs, slightly worn, matte armour with a faint glossy edge, clean topology, low poly, single mesh per part.
2. **Saiyan Battle Armour (Elite, dark)**
> Same as above but black chest plate and shoulder guards with deep red trim and a faint red glow in the seams, dark-grey bodysuit, black boots and gloves, battle-scratched.
3. **Orange training gi (Son Goku)**
> Stylized anime-inspired game asset, an orange martial-arts gi with blue undershirt, blue wristbands, blue sash belt, blue boots with yellow laces, loose sleeves, soft cloth folds, T-pose, on a blocky humanoid mannequin, low poly.
4. **Blue and white gi (God / Blue era)**
> Same gi cut, deep blue top with a white undershirt, orange sash, wristbands and boots, cleaner and sharper cloth folds.
5. **Ultra Instinct gi**
> Same gi cut, all-black top and trousers with silver-white trim, silver sash, a faint silver shimmer on the edges.
6. **Super Saiyan 4 outfit (fur and wraps)**
> Stylized anime-inspired game asset, a Super Saiyan 4 outfit on a blocky humanoid mannequin: dark red fur covering the shoulders, forearms and shins, bare chest with red fur edging, a white-yellow wrapped waist band, a dark-blue sash, black wristbands, a long fur tail as a separate part, T-pose, low poly.
7. **Kaioken wraps**
> Orange gi with a red energy-stained sleeve edge and a red glowing waist sash, low poly, T-pose.

## 3. Aura and charge-up
Tripo makes meshes, so these are **mesh pieces the mod animates** (scaled, rotated, scrolled, tinted per form), not particles. All are white or grey so the mod can tint them in the form's colour. For each, ask for `no base, open at the bottom, thin shell` where it says so.
1. **Flame shell (the aura body)**
> Stylized anime-inspired game asset, a tall hollow cone-shaped aura made of overlapping flame tongues, thin shell, open at the bottom and top, hard stylised flame shapes, bright white at the tips fading to light grey at the base, no ground, centered, low poly under 1500 triangles.
2. **Flame tongue (single, repeated around the body)**
> A single tall curved flame tongue, flat stylised shape with a sharp tip and a slight S-curve, a thin double-sided plane with a small amount of thickness, white to light-grey gradient, under 120 triangles.
3. **Lightning bolt (for Super Saiyan 2, 3 and the later forms)**
> A single jagged anime lightning bolt, 5 to 7 sharp segments, thin flat shape, white core, under 80 triangles.
4. **Charge-up ring (the shockwave when a form starts)**
> A flat ring shockwave disc, thick at the centre, thinning to the edge, faint concentric rings and tiny debris notches, white, laid flat, under 400 triangles.
5. **Energy column (the vertical beam at the start of a power-up)**
> A tall thin hollow cylinder of light with a slightly twisted shell and faint vertical streaks, open at both ends, white to pale grey, under 300 triangles.
6. **Rock shards (the ground breaks as you charge)**
> Five floating angular rock shards of different sizes, flat-shaded stone, a chipped edge and a flat bottom, grey, under 150 triangles each, on a white background.
7. **Spark orb (the sparks that orbit the Saiyan)**
> A small glowing sphere with a thin outer shell and a few short rays, white, under 100 triangles.

### What I will do with the results (the "make the aura look cooler" part)
- Flame shell + tongues replace the flat textured quads: the aura gets depth and moves (scroll up, wobble, tint per form, pulse faster when stronger).
- The charge-up becomes three beats: **energy column + shard lift** while charging, **ring + flash** at the transformation, then the **steady aura** with bolts and sparks for the strong forms.
- Everything stays inside the existing budgets: one look per nearby player, a hard quad cap (220 at close range, about a third of that far away), nothing drawn for far players.

## 4. Order I suggest
Hair first (16 small meshes, easy), then flame tongue + lightning (small, reused everywhere), then the shell and ring, then armours. If a result is bad, regenerate with the same prompt and a different seed rather than rewriting it.
