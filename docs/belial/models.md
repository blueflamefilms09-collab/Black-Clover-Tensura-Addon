# Belial (Magi: Hakuryuu Ren) - Blender models

Source of the design: the owner's description (white scythe with a round emblem at the blade root and a long pole, a white skeletal bone dragon coiling behind the
wearer, dark bat-wing mantle, bead-chain necklace, bone / white-linen outfit, pale hair, white + teal glow). Wiki images were not fetchable (neither the English nor the
Japanese wiki could be used for pictures), so shapes come from the description and general knowledge of the Djinn. Nothing was seen in a game, only in the software
previews of `tools/relics/preview_mesh.py` and `tools/blender/belial_bone_preview.py` (images in `docs/belial/previews/`).

NEW vs REPLACEMENT (rule 5): the scythe mesh id and the `belial_bone` part set are NEW assets. The old `djinn_weapon_belial` mesh and the `belial/`, `u_belial/`,
`d_belial/` folders are untouched (KEPT). Switching the Djinn over to the new assets is the game-side work (Java / json).

## 1. Scythe: `djinn_weapon_belial_scythe`

| | |
|---|---|
| Files | `weapon_meshes/djinn_weapon_belial_scythe.txt`, `textures/entity/weapon/djinn_weapon_belial_scythe.png`, `..._glow.png` |
| Script | `tools/blender/belial_scythe.py` (`python tools/blender/belial_scythe.py --samples 24 --threads 2`) |
| Size class | `scythe` (2.0 - 2.6 blocks); apparent length 2.515 (inside the class). Mesh runs y 0 .. 270.7, blade toward -x |
| Quad lines | 4097 body + 701 glow copies = 4798 (inside 3000-5000) |
| PNG | skin 321 KB + glow 24 KB = about 345 KB (1024 px, 80-colour palette PNG) |
| Grip | header `grip 0.240` (grip section y 38-92 of 270.7); suggested grip point `[0.0, 65.0, 0.0]` |
| Made in Blender | yes: all of it, modelled and baked (Cycles COMBINED + EMIT) through bl_lib / forms_kit; bake-only detail objects (runes, studs, glow seams, pupil seams) are high-poly detail baked onto the low mesh |

Form: vertebra-joint pole (bone, teal inlay bands), linen-wrapped grip with silver bands, butt of three bone claws round a teal orb and a bone spike, a bone jaw-collar
socket, the **emblem** (a ring of twelve vertebrae with runes and studs, an inner ring, eight uneven spokes, a sun-burst of thorns with two broken, a teal slit-pupil
dragon eye held by six claws), a silver-white crescent blade with a saw-notched edge and chips, a vertebra spine along the back with thorns and silver clasps, teal fuller
and edge glow, a hooked tip talon, linen tassels and teal bead strings. Seeded asymmetry throughout.

Lines for the coordinator (shared files, NOT edited here):

- `tools/relics/weapon_classes.json`: `"djinn_weapon_belial_scythe": "scythe",`
- `assets/anime_multiverse/weapon_grips.json`: in `classOf` change `djinn_weapon_belial` and `djinn_weapon_belial_dark` from `"greatsword"` to `"scythe"`; in `points` add
  `"djinn_weapon_belial_scythe": [0.0, 65.0, 0.0]`.
- Engravings (rule 6, `item/WeaponEngravings.java`) and a Better Combat file (rule 13, `tools/bettercombat/gen_attributes.py`, two-handed scythe) are still to add.

## 2. Bone set: `djinn/belial_bone/`

| | |
|---|---|
| Files | `djinn/belial_bone/<piece>.txt` (23 pieces), atlas `textures/entity/djinn/belial_bone_armor.png` + `belial_bone_armor_glow.png` |
| Script | `tools/blender/belial_bone.py`; preview `tools/blender/belial_bone_preview.py` |
| Pieces | helm, horn_l/r, crest, chest, belt, pauldron_l/r, vambrace_l/r, gauntlet_l/r, greave_l/r, wing_in_l/r, wing_out_l/r (bat wings), tail_seg, tail_tip (bone-dragon spine), backdrop (bone dragon: spine, ribs, clawed forelimb, horned skull), mantle (dark cape), weapon (the scythe at 0.22 scale, in item space) |
| Quad lines | 5123 body + 889 glow = 6012 for all 23 pieces once each (the tail segment is drawn n times by the layer on top). Largest: backdrop 810, weapon 850, chest 761 |
| Atlas | 1024 px, one atlas for all pieces: skin 303 KB + glow 36 KB = about 339 KB (64-colour palette) |
| Made in Blender | yes (same pipeline), one shared bake |

Frames: authored y up, +z front, in each piece's own frame (djinn_parts ANCHOR), exported `(x, y, z) -> (x, -y, -z)` (model space); the weapon piece stays in item space.

**Java change needed (not done here):** the loader has no `_bone` kind. Add a kind BONE (=3) with directory `<id>_bone` and skin `<id>_bone_armor[_glow]` (here
`belial_bone`) to `DjinnLook` / `DjinnMeshes` / `DjinnArmorLayer` (kind chain, cache arrays, marker piece `chest`), and set Belial's Style `backSpin` to 0 for this set so the
backdrop does not spin.

## 3. Cost (rule 11), worst case

- Server: none (assets only).
- Packets: none.
- Client draw per wearer: about 5100 body quads + 900 glow quads, plus the tail repeated (tail_seg 96 x n). Far wearers should be skipped by the layer's existing LOD. One
  scythe in hand is 4798 lines, above the engine default MeshCap of 3000 (the owner's cap is 5000): the cap must allow it or the layer must use the 0.22-scale weapon piece (748 quads).

## 4. Honest quality verdict

- Scythe: reads correctly (white bone, vertebra-ring emblem with a teal dragon eye, notched crescent blade, thorns). Weak: a flat pale patch remains on the blade near the
  root (the blade gets little texel density because the unwrap packs thousands of small islands at equal density), the tassels are plain ribbons, and the emblem is only
  moderately deep. It is below Genesis Demon-Slayer detail.
- Bone set: the dragon backdrop, skull, bat wings and cape read at a distance. Close up the skull and spine are low-poly and the 64-colour atlas shows blocky texels.
  The assembly preview is an approximation of DjinnArmorLayer (wing hinge, tail chain and hand position are guessed), so the in-game pose can differ. Not seen in a game.
- Not done: no Java, no engravings, no Better Combat file, no 2D inventory icon (the old icon is kept).
