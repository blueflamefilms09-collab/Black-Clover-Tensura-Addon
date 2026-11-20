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
| Size class | `scythe` (2.0 - 2.6 blocks); apparent length 2.522 (inside the class). Mesh runs y 0 .. 270.7, blade toward -x |
| Quad lines | 4056 body + 722 glow copies = 4778 (was 4798; inside 3000-5000) |
| PNG | skin 322.6 KB + glow 25.9 KB = about 349 KB (1024 px, 80-colour palette PNG; was about 345 KB) |
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
| Files | `djinn/belial_bone/<piece>.txt` (25 pieces: the old 23 plus `arm2_l`, `arm2_r` appended last), atlas `textures/entity/djinn/belial_bone_armor.png` + `belial_bone_armor_glow.png` |
| Script | `tools/blender/belial_bone.py`; preview `tools/blender/belial_bone_preview.py` |
| Pieces | helm, horn_l/r, crest, chest, belt, pauldron_l/r, vambrace_l/r, gauntlet_l/r, greave_l/r, wing_in_l/r, wing_out_l/r (bat wings), tail_seg, tail_tip (bone-dragon spine), backdrop (bone dragon: spine, ribs, clawed forelimb, horned skull), mantle (dark cape), arm2_l/r (thin bone-white second vambrace + gauntlet; drawn since 4.58.0 on the body below the main arms, appended at the end of the table), weapon (the scythe at 0.22 scale, in item space) |
| Quad lines | DjinnBoneSetTest total 6358 of 6400 (was 6282): core pieces with tail_seg x3, plus mantle and horns. Largest: backdrop 1124, weapon 751, chest 685, helm 440. `arm2_l/r` 118 / 117 are extra and not in that total |
| Atlas | 1024 px, one atlas for all pieces: skin 310.7 KB (96-colour palette, denoised) + glow 30.2 KB (40 colours) = about 341 KB |
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

- Scythe: the flat pale patch at the blade root is gone (blade UV density x1.75, baked scutes, ribs and cracks). The emblem ring is deeper (vertebrae with spinous
  shape), the tassels are tapered folded cloth, and a gold-teal eight-pointed star is inlaid on the blade and the ring studs. Orientation: the mesh matches vassal_scythe
  and black_sword_greed_scythe on every `weapon_orientation.py` measure (imbalance -0.345, head at -x, centroid 0.70, no flags); no mirror or turn was found, so an
  in-hand complaint is in the pose / grip anchor (Java / json), not the mesh. Still weak: the blade relief is texture, not geometry; PNG sits at 349 KB.
- Bone set: real horned dragon skull (brow ridges, cheekbones, sockets, hinged jaw with teeth, nasal ridge), vertebrae with spines, curved ribs, finer wing bones,
  black-violet jagged wing membranes with bone-white fingers, forelimb claws curling forward. Canon extras: two vertical teal forehead eyes, swept-back white hair
  and gold studs on the helm, dragon-bone graft on the pauldrons, `arm2_l/r` (anchored since 4.58.0). Weak: close up the skull is still faceted and the 96-colour atlas is
  blocky; the helm crest is busy. Assembly previews approximate DjinnArmorLayer, so the in-game pose can differ. Not seen in a game.
- Worst case: assets only, no server cost, no packets; client about 6360 body quads per wearer plus the repeated tail, far wearers skipped by the layer LOD.
- Not done: no Java, no engravings, no Better Combat file, no inventory icon change; the test sweep was not run.
