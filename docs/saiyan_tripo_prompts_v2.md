# Tripo3D prompts v2: made for the Minecraft mod

Why a v2: the first two wigs came back with a mannequin head, neck and base fused into the hair (one mesh, so it can't be split cleanly), smooth shading, and a size that doesn't match Minecraft's head. These prompts fix that. They are for the mod: blocky, hair only, sized to the 8x8x8 pixel Minecraft head.

## The Minecraft rules every prompt follows
- **Head size:** the Minecraft head is an 8x8x8 pixel cube (half a block). The hair is a shell about 1 pixel thick over it (so about 10 pixels wide), with spikes sticking out.
- **Look:** Minecraft style, voxel, built from cubes and flat tapered prisms, flat colours, no smooth blobs. The texture is a small pixel-art one (16x16 or 32x32 px per face, hard pixels, no gradients).
- **Hair only:** no head, no face, no skull inside, no neck, no stand, no base, no mannequin. The shell is hollow and open at the bottom, so it can sit on the player's own head.
- **Polycount:** cube-built hair is cheap: **300-900 triangles** is plenty. Do not go over 1200.
- **Position:** hair centred on the origin, bottom of the shell at the bottom of the model, facing front (+Z), upright.

## Prefix (put before every hair prompt)
`Minecraft style voxel hair piece only, made of cubes and flat spikes, hollow shell open at the bottom, no head, no face, no skull, no neck, no stand, no mannequin, no base,`

## Suffix (put after every hair prompt)
`pixel-art texture with hard pixels and flat colours, low poly under 900 triangles, a single mesh, centred, upright, front view, plain white background, no shadow on the ground, no text`

## Negative prompt (if Tripo has the box)
`head, face, eyes, skin, neck, shoulders, mannequin, stand, base, pedestal, smooth, realistic, hair strands, fur, wig cap with a face, background`

## 1. Hair, one prompt per form (16)
Fill in: prefix + this line + suffix. The colour is the mod's table value.

**kaioken** (#1A1A24)
> wild upswept black hair with a red tint, 8 short sharp spikes, main colour #1A1A24, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**false_super_saiyan** (#D0B040)
> dull straw-gold unruly spiky hair, 9 uneven spikes, main colour #D0B040, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan** (#FFD93A)
> upright gold hair in 10 sharp flame-shaped spikes, a few fall over the forehead, main colour #FFD93A, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_full_power** (#FFE266)
> upright pale-gold hair in 10 sharp spikes, calm and bright, main colour #FFE266, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_2** (#FFE850)
> taller, sharper gold hair in 12 spikes with thin lightning-bright tips, main colour #FFE850, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_3** (#FFC82A)
> extremely long gold hair falling to the knees, one tall central spike and long side locks, no eyebrow ridge, main colour #FFC82A, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_4** (#14101A)
> long wild black hair swept back, 12 spikes, a dark crimson sheen, main colour #14101A, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_full_power_super_saiyan_4** (#1C1420)
> longer wild black-violet hair swept back, 13 spikes, main colour #1C1420, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_4_limit_breaker** (#241018)
> very long wild dark-plum hair swept back with magenta glints, 14 spikes, main colour #241018, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_god** (#E02424)
> upright crimson-red hair, smooth and clean, 9 sleek spikes, main colour #E02424, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_blue** (#38B4FF)
> upright electric-blue hair, smooth and clean, 9 sleek spikes, main colour #38B4FF, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**perfected_super_saiyan_blue** (#7CD6FF)
> upright ice-blue hair with a faint silver sheen, 9 sleek spikes, main colour #7CD6FF, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**super_saiyan_blue_kaioken** (#38B4FF)
> upright electric-blue hair with red-pink streaks, 9 spikes, main colour #38B4FF, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**ultra_instinct_sign** (#9AA4C0)
> silver-grey spiky hair, 9 spikes, a few strands lifting, main colour #9AA4C0, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**perfected_ultra_instinct** (#DCE6FF)
> long flowing silver-white hair, 11 spikes, loosely swept back, main colour #DCE6FF, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

**true_ultra_instinct** (#FFFFFF)
> long flowing pure-white hair, 11 spikes, a faint glow at the edges, main colour #FFFFFF, a 1-pixel thick cap hugging an 8-pixel head, spikes built as stacked cubes that taper to one pixel at the tip, darker roots and lighter tips in the pixel texture.

## 2. Armour and outfits (Minecraft biped)
Same rules, but for worn pieces: `Minecraft style voxel armour piece only, made of cubes, flat colours, pixel-art texture with hard pixels, no body, no mannequin, no stand, ...`. Ask for **one part per generation** so each fits a Minecraft body part: chest (12x8x4 px plus a 1 px shell), arm (4x12x4), leg (4x12x4), boot, glove, shoulder guard. Under 700 triangles each.
1. **Saiyan Battle Armour (classic), chest piece:** `white chest plate with rounded shoulder guards and a short waist skirt guard, yellow trim lines, black underlayer at the neck and arm holes, 1 px thick shell around a 12x8x4 px torso, hollow, open at the neck, arms and bottom`.
2. **Saiyan Battle Armour, gloves and boots:** `white gloves with a yellow cuff, white boots with a yellow cuff, each hollow and sized for a 4x12x4 px limb`.
3. **Elite dark variant:** same pieces in black with deep red trim and thin red glowing seam pixels.
4. **Orange gi (Son Goku), top and trousers:** `orange martial-arts gi top with a blue undershirt collar, blue sash belt, and trousers with blue wristband and boot pixels, a 1 px shell around the torso, arms and legs`.
5. **Blue-and-white gi (God / Blue era):** the same cut in deep blue with a white undershirt and orange sash.
6. **Ultra Instinct gi:** the same cut in black with silver-white trim and a faint silver edge.
7. **Super Saiyan 4 fur outfit:** `dark red fur panels over the shoulders, forearms and shins built as stacked jagged cubes, a wrapped waist band, a dark-blue sash, black wristbands`. Make the **tail** as its own piece: `a long fur tail as 6 stacked tapered cube segments curving up, red-brown fur, white tip pixels`.

## 3. Aura and charge-up (for the mod's effects)
These are mesh pieces the mod tints and animates, so make them white/grey. Keep them **flat-shaded and chunky** to fit Minecraft: `Minecraft style voxel effect piece, flat white and light-grey colours, hard pixel texture, no smooth shading, ...`.
1. **Flame tongue:** `a single tall flame tongue built as 6 stacked cubes that taper and lean in a gentle S-curve, white to light grey, under 150 triangles`.
2. **Flame shell:** `a tall hollow ring of 12 overlapping flame tongues built from cubes, open at the bottom and top, white at the tips, under 1200 triangles`.
3. **Lightning bolt:** `a single jagged lightning bolt made of 5 to 7 thin cube segments, white, under 100 triangles`.
4. **Charge ring:** `a flat blocky ring shockwave made of 16 cubes in a circle, thick in the middle and thinner at the edge, white, under 300 triangles`.
5. **Energy column:** `a tall thin hollow square column of light with vertical stripe pixels, open at both ends, white, under 300 triangles`.
6. **Rock shards:** `five floating angular rock chunks made of a few cubes each, different sizes, grey, under 120 triangles each`.
7. **Spark:** `a tiny star of 4 short cube rays and a centre cube, white, under 60 triangles`.

## 4. Order
Hair first (so each form gets a quick check on the player), then the flame tongue and lightning (reused everywhere), then the shell, ring and column, then armours. Regenerate with a different seed rather than rewriting a prompt that was close.
