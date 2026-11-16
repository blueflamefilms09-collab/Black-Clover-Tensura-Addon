# Devil forms, group 3: five Blue Exorcist Demon-King kin and two Tensura daemons

Player-scale bodies for the Devil Contract system (a summoning NPC and the form a player takes). Every body is MADE IN BLENDER (Blender 5.3 as the `bpy` module,
`pip install bpy`), through the existing entity pipeline (`tools/blender/bl_lib.py`, `bl_entity.py`: bmesh lofts, Subdivision Surface, Solidify, Displace and
Decimate modifiers, Smart UV unwrap, Cycles bake of the skin and the emission). Nothing is hand-placed from code boxes. Nothing in this group could not be made in Blender.

* Builder: `tools/blender/devil_group3.py` (one builder function per devil, shared helpers inside; `--only id,id`, `--trial DIR`, `--quick`). Reproduce with
  `python3 -B tools/blender/devil_group3.py`. The check script `tools/blender/devil_group3_check.py` re-reads the shipped files with the rules of `BossMeshWave2Test` / `EntityMeshData`.
* Files per devil `<id>`: `entity_meshes/devil_<id>.txt`, `devil_<id>_lod.txt`, `textures/entity/boss_mesh/devil_<id>.png` and `devil_<id>_glow.png`, previews
  `docs/devil_forms/previews/<id>.png` (front three-quarter) and `<id>_back.png`.
* Format: header `entity devil_<id> 0.0625` (1 unit = 1/16 block, so 32 units = 2 blocks = the player; the standing body is about 1.9 to 2.0 blocks to the top of the head), y up,
  face +z, its left +x, feet on y = 0. Parts named by the roles `torso head arm_l arm_r forearm_l forearm_r leg_l leg_r` (all eight on every devil) plus `wing_l wing_r` / `tail` only where
  the devil has them. Pivots at the joints, parents first: torso (hip) with head, arms, wings, tail as children, forearm under arm, legs free. The hands are part of the forearm,
  the feet part of the leg, horns and hair part of the head.
* Skins: 1024 px square, palette PNG (Pillow fast octree, alpha kept), all body skins 135 to 292 KB, glow skins 5 to 32 KB. Glow = emissive parts in the devil's colour plus an accent where the
  colour itself is too dark to glow (Noir).
* Not seen in a game: the meshes were checked with the game's parser rules and rendered in Blender (Cycles, the baked skin), not loaded in the client.

## Result

| Devil | Roles | Quads body + glow | LOD (no glow pass) | Body skin | Height (feet to head top incl. horns) | Blender |
|---|---|---|---|---|---|---|
| kin_of_iblis | 8 + tail | 1562 + 499 = 2061 | 884 | 135 KB | 2.28 blocks | yes |
| kin_of_egyn | 8 + tail | 1278 + 189 = 1467 | 643 | 262 KB | 2.43 | yes |
| kin_of_amaimon | 8 | 1250 + 80 = 1330 | 619 | 165 KB | 2.02 | yes |
| kin_of_astaroth | 8 | 1241 + 140 = 1381 | 597 | 215 KB | 2.06 | yes |
| kin_of_lucifer | 8 + wings | 2010 + 242 = 2252 | 1022 | 267 KB | 2.27 (wings reach 2.44) | yes |
| daemon_noir | 8 + wings | 1491 + 235 = 1726 | 858 | 146 KB | 2.40 (wing tips 2.47) | yes |
| daemon_blanc | 8 + wings | 1887 + 182 = 2069 | 892 | 292 KB | 2.20 | yes |

All under the 2800 build budget (the runtime cap is 3000), every LOD under half of its full mesh, no glow pass in the LOD copies. Draw cost per devil in view: at most 2252 quads near,
at most 1022 far (client only; no server, packet or tick cost from these files).

## Sources and what is canon (accuracy pass)

* English Wikipedia character list of Blue Exorcist, saved by the coordinator as plain text (`.../scratchpad/canon/be.txt`): **read**. It gives roles and powers only: Amaimon is the Earth King,
  seventh-strongest of the Eight Demon Kings, sucks lollipops, is a son of Satan; Astaroth is the King of Rot (it possessed Reiji Shiratori); Lucifer is the King of Light, the strongest of the
  Eight Demon Kings and head of the Illuminati; Mephisto is Samael, King of Time. It has **nothing on Iblis or Egyn** and almost nothing on looks.
* Fandom wikis (English) and Japanese fandom: not reachable in this session (HTTP 403 for scripted requests). `ja.wikipedia.org`: tried once (a character-list page); that title answered
  "404 Not Found" and the next request was rate-limited, so **nothing from ja.wikipedia was read** and none is claimed.
* Every look detail below is therefore "memory" unless the table says "read". Where memory is not sure the feature was left out instead of invented (the first build of this group had
  more ornaments; they were removed in this pass: Egyn's fin-wings and pearl armour, Amaimon's golem body and ram horns, Astaroth's antlers, fungus, wings and tail, Iblis's pauldron spikes and
  tusks, Lucifer's top hat and tail, Noir's spiked void ring and shoulder spikes, Blanc's antlers and crystal fan wings).
* Noir and Blanc are the mod's own "kin of a Primordial" characters: they carry the Primordial colour (black / white) and the Tensura demon look (a tall, slender, formally dressed humanoid
  with a small pair of horns and bat-like demon wings); no individual character was copied.

### kin_of_iblis, King of Fire (confidence: low for the look, high for the element)

| Feature | Basis | Where |
|---|---|---|
| Fire as the whole theme: charred body with glowing lava cracks, ember heart on the chest, flame mane, flame-tipped tail | the title "King of Fire" (read in the mod's roster); the rendering is design | skin cracks, `heart`, mane, tail |
| Two swept-back horns, broad brute build, claws | memory of the generic Gehenna demon look, not of Iblis; low | head, body |
| Colour FF6A2A glow, charcoal body | the roster colour; design | glow skin |
| Not claimed: any armour (a belt and a bracer remain as plain dark plates). The kin has no source in `be.txt`. | | |

### kin_of_egyn, King of Water (confidence: low for the look, high for the element)

| Feature | Basis | Where |
|---|---|---|
| Blue scaled body with flowing glow lines, gill slits | "King of Water" (roster); the scales and gills are an aquatic reading of the element, design | skin, torso |
| Swept dorsal crest, fin ears, coral-like spikes, fin blades on the forearms, a finned tail | design for an aquatic kin; memory gives no canon form for Egyn; low | head, forearms, tail |
| Removed in this pass: wings, pearl breastplate | invented | |

### kin_of_amaimon, King of Earth (confidence: medium)

| Feature | Basis | Where |
|---|---|---|
| Earth King, son of Satan, always sucking a lollipop | read in `be.txt` | lollipop in the mouth (stick and candy disc on the head) |
| A slim, pale young man with pointed ears, wide green eyes, a spiky bowl of dark green-black hair with a fringe, a dark coat | memory of the manga and anime; the colour of the hair and the coat is the least sure part; medium | whole body |
| Earth element: brown stone gauntlets and greaves with amber cracks (palette 8A6A3A) | design from the roster colour; the stone armour is not canon | forearm, legs |
| Removed in this pass: the golem body, ram horns and amber crystal back of the first build | invented | |

### kin_of_astaroth, King of Rot (confidence: low)

| Feature | Basis | Where |
|---|---|---|
| King of Rot, takes over a human body (Reiji Shiratori) | read in `be.txt`; says nothing of its own form | |
| A gaunt, hunched, rotting figure: green decayed skin, exposed ribs and spine, a glowing rot cavity, a skull-like face with hollow glowing eyes and a jaw of teeth, a ragged shroud down the back, long bone claws | design for the element; memory has no canon form; low | torso, head, hands |
| Removed in this pass: antlers, fungus, bat wings, spine tail | invented | |

### kin_of_lucifer, King of Light (confidence: low to medium)

| Feature | Basis | Where |
|---|---|---|
| King of Light, strongest of the Eight Demon Kings, head of the Illuminati | read in `be.txt` | |
| A tall, slender, noble figure in a white and gold coat with epaulettes and gold cuffs, long pale-gold hair, pointed ears, glowing pale eyes, a halo ring | design from "King of Light" and a noble ruler; the exact canon outfit is not remembered with confidence; low to medium | whole body, head |
| Three tiers of white feathered wings with a gold bone | design for "light"; not a confirmed canon feature; low | wing_l, wing_r |
| Removed in this pass: the top hat (it belongs to Mephisto / Samael, a different king) and the spade tail | not Lucifer's | |

### daemon_noir, Primordial Noir's line (Supreme) (confidence: medium for the type, none for an individual)

| Feature | Basis | Where |
|---|---|---|
| Black body with silver trim and a violet glow (the 1A1A24 colour is too dark to glow, so the glow is the cold violet 9A88FF) | roster colour plus an accent; design | skin, glow |
| Tall, slender, in a long black formal coat with a high collar, a smooth mask-like face, burning eyes, a pair of swept horns, clawed hands, large angular bat-blade wings | memory of the Tensura demon type (formally dressed, horned, winged); medium | whole body |
| Removed in this pass: the spiked void ring, the shoulder spikes, the forward horns | invented | |

### daemon_blanc, Primordial Blanc's line (confidence: medium for the type, none for an individual)

| Feature | Basis | Where |
|---|---|---|
| White body in the Primordial colour F0F0FF, pale blue glow | roster colour; design | skin, glow |
| Slender, elegant, in a layered white gown, long white hair, a silver circlet, a small pair of curved horns, pale glowing eyes, white bat-blade wings (the same build as Noir's, smaller, so the two daemons read as one species in two colours) | memory of the Tensura demon type; medium | whole body |
| Removed in this pass: the glass antlers and the crystal fan wings | invented | |

## Cost (rule 11)

* Client only: at most 2252 body+glow quads per devil within the near range, at most 1022 beyond it (LOD, no glow pass); two draw batches as the other Blender bodies. Hard cap 3000
  (`EntityMeshData.MAX_QUADS`). No allocation, no entity, no packet, no tick work from the assets.
* Memory: 0.3 to 0.5 MB of text per devil (plus 0.14 to 0.24 MB for the LOD copy) before the packed mesh folders compress it; skins 135 to 292 KB each as palette PNGs.

## What was not done

* The renderer code that draws these files and the pose animation for the new roles (wings, tail) is the other agent's work; no Java, lang, version or shared file was touched.
* No engravings, Better Combat file or weapon: these are bodies, not weapons.
* Not loaded in a game, so the pose maths (arm swing with the pauldrons and gauntlets, a rigid skirt and coat tails against swinging legs) was not seen moving. Noir's and Blanc's skirts and coat tails
  are part of the torso and can clip the legs at a large stride; Blanc's skirt is wide on purpose.
* Individual canon looks of Iblis, Egyn and Astaroth could not be confirmed from any source readable here; they are low-confidence design, flagged above.
