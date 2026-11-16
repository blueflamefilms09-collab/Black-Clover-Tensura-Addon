# Devil bodies, group 4: the Primordial lines of Jaune, Violet, Rouge, Vert and Bleu

Five player-scale devil bodies **made in Blender** (`tools/blender/devil_group4.py`, Blender 5.3 as a Python module, Cycles bake, Decimate LOD, rule 14) for the Devil Contract: `daemon_jaune`,
`daemon_violet`, `daemon_rouge`, `daemon_vert`, `daemon_bleu`. Files per devil: `entity_meshes/devil_<id>.txt` and `devil_<id>_lod.txt`, `textures/entity/boss_mesh/devil_<id>.png` and `_glow.png`.
Checker (pure Python, the BossMeshWave2Test rules): `python tools/blender/devil_group4_check.py`.

## NEW / REPLACED / KEPT (rule 5)

* **NEW**: all of the above (nothing like it existed). The mod's other meshes, the shared Blender libs and every Java / lang / config file are untouched.
* **REPLACED**: only inside this work. The first version of these five (horned, winged, tailed, armoured "generic demons": flame lyre horns, ram horns and skeletal wings, a winged blade-feather noble,
  a bark-and-antler golem, a finned sea demon) was dropped after the owner's accuracy requirement: none of it is in the source.
* **KEPT**: the old bodies were never committed, so nothing of them is kept.

## Sources (rule 10) and what could not be read

* **Read this session**: `ja.wikipedia.org`, article 転生したらスライムだった件 (character section and the list of changes between the web and book versions), through the MediaWiki API with a plain
  request that names the client, one request per minute because of the rate limit. Everything marked *source* below is from that one article.
* **Not readable**: the English / Japanese Fandom wikis (blocked from here). The separate character-list article was requested and not returned. No image of any character was seen, so
  **no clothing, build or pose below comes from a picture**; where the article says nothing the feature is either left out or marked *design choice*.
* What the article says about the seven Primordials (原初の悪魔, a colour each): Noir = Diablo (black hair with red and gold streaks, male); **Blanc = Testarossa** (silver-white hair, red eyes, female);
  **Jaune = Carrera** (blonde woman); **Violet = Ultima** (purple hair in a side ponytail, a cheerful-looking girl); **Rouge = Guy Crimson** (the oldest Demon Lord, "an alluring handsome man with red hair");
  **Vert = Misery** (green-haired beauty) and **Bleu = Rain** (blue-haired beauty), both "in a dark-crimson maid uniform". It names no horns, wings or tail for any of them, so the bodies have none.
* The mod's entries are "a daemon of X's line" (the Primordial's kin). Each body shows **the line's Primordial as the article describes them** (their hair colour, build and the clothing the article names);
  the entry's colour is carried in the eyes, hair and glowing piping. Whether the kin should instead look like a subordinate (for example Carrera's sheep-horned Esprit, 羊角の悪魔) is a decision for the owner.

## Per devil: canon basis, source or memory, confidence

| Devil | Canon basis (what the mesh carries) | Source or memory | Confidence |
|---|---|---|---|
| `daemon_jaune` (Carrera) | A woman with **blonde hair**; humanoid demon; no horns, wings or tail. | Hair colour and sex: *source* (金髪の女性型悪魔). Everything else (long loose hair, a fitted black jacket with gold piping, tall boots, gold eyes) is *design choice*, kept plain. That she took up sword fighting as a hobby and reached full mastery (*source*) is **not** modelled (no weapon). | hair, sex: high; clothing, build, eyes: low (design) |
| `daemon_violet` (Ultima) | **Purple hair in a side ponytail**, a girl who looks cheerful and cute; modelled shorter (0.9) and lighter than the others. | Hair, ponytail, girl: *source* (紫色の髪をサイドポニーテール, 一見可愛げのある明るい少女). Which side the tail is on, the short black-violet dress with glowing violet piping and the violet eyes: *design choice*. | hair, ponytail, build: high; dress, side, eyes: low (design) |
| `daemon_rouge` (Guy Crimson), Supreme | A **tall, alluring handsome man with red hair**, no horns, wings or tail; the Supreme tier is shown by height (1.04) and a long coat. | Red hair and the alluring handsome man: *source* (妖艶な赤髪を持つ艶のある美丈夫). The long black coat with crimson lining and glowing red piping, the high collar and the red eyes: *design choice* (the article gives no outfit). | hair, sex: high; coat, eyes: low (design) |
| `daemon_vert` (Misery) | A **green-haired woman in a dark-crimson maid uniform**; modelled as the serious, hard-working one: a neat bun and a straight fringe, a little taller (1.02). | Green hair, woman, dark-crimson maid uniform: *source* (緑髪の美女, 暗紅色のメイド服). Apron, collar, cuffs, black stockings and the frilled headband are the usual parts of a maid uniform, **not stated by the article**: *memory of the genre*. The bun and eye colour: *design choice*. | hair, uniform colour: high; apron and headdress: medium; hair style, eyes: low (design) |
| `daemon_bleu` (Rain) | A **blue-haired woman in the same dark-crimson maid uniform**; modelled as the easy-going, spoiled youngest: long loose wavy hair and a crooked fringe, a little shorter (0.97). | Blue hair, woman, uniform colour: *source* (青髪の美女, 暗紅色のメイド服). Her nature (lazy, selfish, the youngest of the pair) is *source* and is only used to pick the hairstyle; the hairstyle itself is *design choice*. Same maid-parts note as Vert. | hair, uniform colour: high; apron and headdress: medium; hair style, eyes: low (design) |

Vert and Bleu wear the same uniform in the source, so their bodies share it; they differ in hair colour, hair style, height and glow colour only. That is canon, not a colour swap by accident.
**What was removed from the first version** (invented, not in the source): every horn, crest, wing, tail, claw, fin, antler, crystal, bark and stone plate, the halo, the soul wisps, the sun core, the skull mask.
**What every body still shares that is not canon**: pale skin, a head with slit glowing eyes in the line's colour (the article only names Testarossa's red eyes), thin glowing piping in the line's colour
(the mod's "glow = the devil's colour" rule), hands with short fingers.

## Numbers (checked by `devil_group4_check.py`)

| Devil | Quads (body + glow = total) | LOD | Height to the top of the hair (blocks) | Skin | Preview |
|---|---|---|---|---|---|
| `daemon_jaune` | 2153 + 160 = 2313 | 929 | 2.06 | 143 KB | `docs/devil_forms/previews/daemon_jaune.png` |
| `daemon_violet` | 1999 + 237 = 2236 | 858 | 1.85 | 131 KB | `docs/devil_forms/previews/daemon_violet.png` |
| `daemon_rouge` | 2187 + 175 = 2362 | 925 | 2.14 | 117 KB | `docs/devil_forms/previews/daemon_rouge.png` |
| `daemon_vert` | 2046 + 60 = 2106 | 873 | 2.16 | 149 KB | `docs/devil_forms/previews/daemon_vert.png` |
| `daemon_bleu` | 2233 + 62 = 2295 | 955 | 2.05 | 160 KB | `docs/devil_forms/previews/daemon_bleu.png` |

* Header scale 1/16 (16 model units = 1 block), feet on y = 0. Parts: torso head arm_l arm_r forearm_l forearm_r leg_l leg_r (no wing or tail part: none of them has one). Pivots at the shoulders, elbows,
  hips and neck, parents first. Every body is under the 3000-quad cap and the 2800 build budget; the LOD copy (Blender Decimate 0.28) is about 40 percent of the body quads and has no glow pass.
* Skins: 1024 px, then reduced to 256 colours (the baked noise made them about 1 MB); the glow skin holds the eyes and the piping in the line's colour.
* Rule 11, worst case of one devil in view: its quad count per frame in two batches (body, glow); the LOD copy beyond the distance the renderer picks. No server work, no packets.

## What was not done or not seen

* Nothing was seen in a game; the previews are Blender renders of the baked skins, front three-quarter, lit by one sun, not the game's lighting.
* The faces are simple (slit eyes, nose, lips): no per-character face or outfit art, because no picture of any of them could be read. The bodies are readable by hair colour, hair style, build and clothing class only.
* Rule 7 (weapons): no weapons are part of these bodies. Rule 6 and 13 (engravings, Better Combat) do not apply.
* The first version's previews and meshes were replaced in place; they were never committed.
