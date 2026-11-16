# Devil forms, group 2 (Devil Contract system)

Custom player-scale bodies for six devils of `devil/WorldDevil.java`, **made in Blender** (rule 14): bmesh lofts and sweeps, Subdivision Surface (torsos only), Solidify (hoods, skirts), Decimate (the far copy),
Smart UV Project into one skin, Cycles bake of the lit surface and of the emission, all through `tools/blender/bl_lib.py` and `bl_entity.py` (not edited). One script builds all six:
`tools/blender/devil_group2.py` (`python -B tools/blender/devil_group2.py [--only id,id] [--trial DIR] [--quick]`, Blender 5.3 as the `bpy` module, `pip install bpy pillow numpy`).
`tools/blender/devil_check.py` re-checks the written files against the rules of `BossMeshWave2Test` (parts, pivots, budget, LOD, skins) without Blender.

This doc is assets only: nothing in Java, lang or the version changed. Another change draws the meshes.

## What was delivered (NEW; nothing replaced, nothing removed)

For each id, in `src/main/resources/assets/anime_multiverse/`:
`entity_meshes/devil_<id>.txt`, `entity_meshes/devil_<id>_lod.txt`, `textures/entity/boss_mesh/devil_<id>.png` and `devil_<id>_glow.png` (1024 px, palette PNG, the Goliath's folder and naming).
Previews (rendered in Blender from the baked skins, front 3/4 and back): `docs/devil_forms/previews/<id>.png` and `<id>_back.png`.

Format (the entity mesh format of `mesh/EntityMeshData`): model unit 1/16 block (header scale 0.0625, so 1 unit = 1/16 block and the player, 29 units, is 1.8 blocks), y up, feet on y = 0, face +z, the devil's left = +x.
Parts by role name, pivots at the joints, a parent always before its children: `torso head arm_l arm_r forearm_l forearm_r leg_l leg_r` on every devil, plus `wing_l wing_r` (Ghost Devil, Sebastian) and `tail` (Ghost Devil) only where the devil has them.
Layer 0 is the body, layer 2 the glow copy of the quads that carry emission (drawn additive with the `_glow` skin); the LOD copy is Blender's Decimate of the body, same skin and parts, no glow pass.

| Mesh | Parts | Quads (body + glow) | LOD copy | Total height (blocks, ears/horns/hands included) | Skin |
|---|---|---|---|---|---|
| `devil_fox_devil` | 8 roles | 1427 + 78 = 1505 | 586 | 2.35 | 292 KB |
| `devil_curse_devil` | 8 roles | 1308 + 60 = 1368 | 534 | 1.93 | 278 KB |
| `devil_future_devil` | 8 roles | 1167 + 39 = 1206 | 498 | 1.88 | 266 KB |
| `devil_ghost_devil` | 8 + wing_l wing_r tail | 2062 + 222 = 2284 | 880 | 2.44 | 276 KB |
| `devil_sebastian` | 8 + wing_l wing_r | 2325 + 132 = 2457 | 990 | 1.95 | 269 KB |
| `devil_hall_demon` | 8 roles | 1931 + 90 = 2021 | 771 | 2.15 | 246 KB |

All six: roles present with quads, at most 3000 quads (the build budget is 2800), LOD about 40 % of the body (always under half of body + glow, the rule of the boss mesh test), feet on the floor, skins square powers of two (1024), palette PNGs re-quantised to fit 300 KB (`fit_skin`).
The total height includes ears, horns and raised hands; the figures were fitted (`FIT` in the script) so the head tops stand at about 1.8 to 2.0 blocks (Fox 1.86, Curse 1.92, Future 1.88, Ghost 1.97, Sebastian 1.95, Hall demon 1.78 with horns to 2.15).

## Cost (rule 11)

Assets only: no server work, no packet, no state. Client draw cost when the other change draws them: the quads in the table per devil in view within the near distance, one cutout batch and one additive batch
(the glow copies are the few glow quads in the table), the far copy beyond; the same order as the Goliath (2746) and the Minotaur (2635), below the 3000 hard cap of `EntityMeshData.MAX_QUADS`.

## Sources and how accurate each devil is

The wikis were reached like this (rule 10): the **fandom wikis could not be read** in this session (403 / refused through the proxy; no attempt to disguise the client). What was read:

* English Wikipedia, "List of Chainsaw Man characters" (saved as plain text): the entries of the Fox, Curse, Ghost and Future Devils (their roles and powers; Wikipedia says almost nothing about looks).
* ja.wikipedia.org, `どろろ` (the Hall of Hell and its 48 statues) and `黒執事の登場人物` (Sebastian's entry), read with a plain request and a descriptive client name.

Everything about **looks** that is not marked "read" comes from the builder's memory of the series and is marked with its confidence. Where the builder was not confident, the model is deliberately neutral
instead of invented (the owner's rule for this pass). Ornaments of the first draft that contradicted or could not be backed by canon were removed (a tail and a second head on the Fox Devil, a nail halo, thorns and a pentagram on the Curse Devil,
a clock halo, crystal fins and armour on the Future Devil, bronze, gold, flame wings and a flame tail on the Hall demon, a seal on Sebastian's back and pointed ears).

### fox_devil: the Fox Devil (狐の悪魔), colour E08A2A

| Feature | Basis | Source | Confidence |
|---|---|---|---|
| a fox devil, female ("parts of her body"), friendly to humans; its head is summoned through the fox hand sign | the English Wikipedia entry | read | high (role) |
| a white fox with a huge head, a long muzzle and wide-open jaws, tall pointed ears | the summoned fox head of the series | memory | medium |
| orange (the devil colour) ear tips, inner ears and face markings, orange forearm mitts and paws | the colour of the contract entry; the exact markings of the series are not remembered | colour from `WorldDevil`, markings invented as accents | low (accent only) |
| humanoid body, digitigrade legs, white fur, cream bib | the owner asked for a humanoid form; the series shows the head and parts | design | low (neutral) |
| no tail, no armour, no extra heads | removed from the first draft (not canon) | | |

Design: lean white fox figure, the head 30 % too big on purpose (the head is the Devil), the lower jaw hanging open on a glowing orange mouth with fangs, amber glowing eyes.
Quads: 1427 body + 78 glow = 1505, far copy 586. Not done: the series' exact face pattern, a fox body of its own (only the humanoid form was asked for).

### curse_devil: the Curse Devil (呪いの悪魔), colour 4A1A6A

| Feature | Basis | Source | Confidence |
|---|---|---|---|
| the devil of curses; a contractor stabs the victim three times with a sword and it manifests to crush them, at the cost of years of the contractor's life; contracted with Aki and Santa Claus | English Wikipedia entry | read | high (role) |
| what it looks like | the builder does not remember it with confidence | none | **low: left neutral** |
| a gaunt hooded figure in tattered violet-black cloth, long arms, violet-lit eyes, no face | a neutral reading of "curse": a shrouded shape in the devil colour | design | low |
| nail halo, thorn crown, pentagram, tags, stitched mouth, wrist rings | removed from the first draft (invented) | | |

Quads: 1308 body + 60 glow = 1368, far copy 534. Not done: the real look (to be replaced when a reference is available; the rig and parts are ready for it).

### future_devil: the Future Devil (未来の悪魔), colour 3AC8C8

| Feature | Basis | Source | Confidence |
|---|---|---|---|
| the devil of the fear of the future, a he, sees the future of whoever he touches; made a contract with Aki after being captured | English Wikipedia entry | read | high (role) |
| lives in the contractor's right eye | the contract entry of `WorldDevil` ("it lives in your right eye") and the series | owner's text + memory | high |
| a slim figure with one big glowing cyan right eye, the left eye shut, a plain dark-teal coat, a short dark-teal hair cap | the eye is the only feature that is certain; the rest is a neutral human build | design | low (the real look is not remembered) |
| halo of clock ticks, crystal fins, armour, dials, chest eye | removed from the first draft (invented) | | |

Quads: 1167 body + 39 glow = 1206, far copy 498. Not done: the series' real body.

### ghost_devil: the Ghost Devil (幽霊の悪魔), colour 9AA8C8

| Feature | Basis | Source | Confidence |
|---|---|---|---|
| the devil of the fear of ghosts; contract with Himeno that gave her invisible arms to grab things from anywhere (the invisible right hand), it manifests to fight when she pays with her body; killed by Aki | English Wikipedia entry | read | high (role) |
| many reaching hands | the owner's brief ("its hands reach from nowhere") and the "invisible arms" of the entry | read + owner's text | medium |
| a pale blue-white shrouded ghost: blank mask face with hollow eyes, a cowl, a ragged shroud, thin legs fading to points, a wisp tail | the usual ghost reading; the series' exact face is not remembered | design | low |
| eight extra arms with open hands as the wings, two long arms with big hands | design for the "many hands" | design | medium (concept), low (shape) |
| the one hand that the entry stresses (the right) is not enlarged | not done | | |

Quads: 2062 body + 222 glow = 2284, far copy 880. Kept from the first draft: the whole figure (the only change in this pass was the removal of glow from the wing arms for the quad budget, before the accuracy pass).

### sebastian: Sebastian Michaelis (セバスチャン・ミカエリス), colour 7A3CC8

| Feature | Basis | Source | Confidence |
|---|---|---|---|
| a demon who serves as the Phantomhive butler; his contract mark is an **inverted pentacle on the back of his left hand**, paired with the magic circle in Ciel's right eye | ja.wikipedia 黒執事の登場人物 | read | high |
| a tall slim man, pale skin, black hair with a long fringe over the face, a black tailcoat, white shirt and white gloves with long black nails, black tie, a watch chain, polished black shoes | the series' butler uniform | memory | high |
| crimson glowing slit eyes (his demon eyes) | memory | memory | medium |
| a pair of black feather wings folded behind (his true form shows black feathers) | memory | memory | medium |
| a silver chain and watch, silver cuff links and buttons | memory (silver, not gold) | memory | medium |
| the violet glow of the seal is the devil colour | colour from `WorldDevil` (the series draws the mark pink-purple) | | |
| removed from the first draft: a seal on the back of the coat, pointed ears, epaulettes, gold | not canon | | |

Quads: 2325 body + 132 glow = 2457, far copy 990. Not done: his cutlery, the monocle-less fine detail of the face, the black fire of his true form.

### hall_demon: a demon of the Hall of Hell (Dororo), colour 8A6A3A

| Feature | Basis | Source | Confidence |
|---|---|---|---|
| the 48 demons are **48 wooden statues** of demons (木彫像) in the Hall of Hell (地獄堂), carved by a Buddhist sculptor who went mad afterwards; Daigo Kagemitsu prayed to them for the realm and paid with his unborn son's body | ja.wikipedia どろろ | read | high |
| wood: dark brown weathered grain with chisel marks | the statues are wood; the colour of `WorldDevil` is brown | read + colour | high |
| a muscular, fierce guardian style (heavy brow, snarling fanged face, horns, big ear lobes, loincloth) | the usual look of Buddhist guardian statues carved in that tradition; the 48 statues differ from each other, so this is one of them | memory | medium |
| amber glowing eyes and mouth (the demon inside the statue) | design | design | low |
| removed from the first draft: bronze, gold, flame wings, flame tail, skulls, sutra marks | not canon | | |

Quads: 1931 body + 90 glow = 2021, far copy 771. Not done: the other 47 statues (one design only; each devil id is one body).

## What could not be done or was not seen

* The fandom wikis could not be read (403), so nothing was checked against them; the confidence column says what rests on memory.
* The meshes were seen only in the Blender previews (Cycles, from the baked skins), not in a game: there is no animation test and no in-game lighting check. The previews show the glow with a plain emission, the game draws it additive.
* The models are low-poly bake-detailed bodies, below the hero art of the Full Djinn Equip: fingers, claws and horns are plain tubes and cones.
* Sebastian's hair is a cap with a fringe and side locks: from behind the nape shows skin under the cap.
* The pose is a standing pose only; arms hang at the sides so that the arm and forearm turns of `MeshPose` do not cut through the body (not tested in game).
