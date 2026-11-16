# Devil forms, group 1: eight player-scale devil bodies made in Blender

Owner brief: every devil of the Devil Contract system gets its own custom body, used both by a summoning NPC and as the form a player takes (the quality bar is the Full Djinn Equip: an
armoured / horned / winged silhouette with emissive details and its own colour theme). This page is **group 1**: `liebe`, `lucifero`, `beelzebub`, `astaroth`, `megicula`, `lucifugus`,
`god_hand`, `etherious` (the ids, tiers, attributes and colours are those of `devil/WorldDevil.java`). Assets only: no Java, lang, version or shared file was changed.

## Was it made in Blender?

**Yes, all eight.** Blender 5.3 ran as a Python module (`pip install bpy`, Python 3.13), through the repo's own pipeline (`tools/blender/bl_lib.py` and `bl_entity.py`, not edited): every piece is a
Blender mesh (lofted sections, tubes, lathes, plates, Subdivision Surface, Solidify, Bevel node in the materials), unwrapped with Smart UV Project into one skin, baked with Cycles (the lit,
bumped surface into `devil_<id>.png`, the emission into `devil_<id>_glow.png`), the LOD copy made by Blender's Decimate modifier, and the previews rendered by Cycles from the baked skins.
Nothing is a code-built box mesh and nothing is marked "NOT Blender". What Blender did not do: hand sculpting. The shapes are procedural (the script builds the pieces from numbers), as the
Goliath, Minotaur and Lucifero boss bodies are. They are stylised, low-poly bodies, not hand-sculpted high-detail figures.

The only adaptation needed for Blender 5.x was none in the shared libraries: they run unchanged (`bl_lib` / `bl_entity` worked as they are; the only warning is the deprecation of
`World.use_nodes`). The script replaces `bl_entity.render_previews` by its own tighter, brighter preview (inside `devil_group1.py` only).

## Files

| What | Where |
|---|---|
| Builder (all eight, one function each, shared helpers inside) | `tools/blender/devil_group1.py` |
| Check of the files against the rules `EntityMeshData` / `BossMeshWave2Test` apply (pure Python, no Java) | `tools/blender/devil_mesh_check.py` |
| Meshes | `src/main/resources/assets/anime_multiverse/entity_meshes/devil_<id>.txt` and `devil_<id>_lod.txt` |
| Skins | `src/main/resources/assets/anime_multiverse/textures/entity/boss_mesh/devil_<id>.png` and `devil_<id>_glow.png` (1024 px) |
| Previews (front three-quarter, and the back) | `docs/devil_forms/previews/<id>.png`, `<id>_back.png` |

Rebuild: `python3 -B tools/blender/devil_group1.py [--only liebe,god_hand] [--trial DIR] [--quick]` (needs `pip install bpy pillow numpy`; about 6 minutes per devil at full quality on one core,
`--quick` is about 20 seconds with a 512 px bake). Check: `python3 tools/blender/devil_mesh_check.py`.

## Format (what the draw code reads)

* `entity devil_<id> 0.0625`: model units are 1/16 block, so **1.0 = the player**. y up, face toward +z, the devil's left is +x, feet on y = 0.
* Parts named by the roles: `torso head arm_l arm_r forearm_l forearm_r leg_l leg_r` (all eight devils), `wing_l wing_r` (all eight devils have wings), `tail` (astaroth, megicula, etherious).
* Pivots at the joints, parents before children: `torso` root; `head`, `arm_*`, `wing_*`, `tail` hang from the torso; `forearm_*` from `arm_*`; `leg_*` are roots (as the boss bodies).
  Shoulder pivots at y 23.4 to 24.4, elbows y 16.8 to 17.6, hips y 12.4 to 13.0, neck y 25.2 to 25.6 (per devil in the `make_rig(...)` call of each builder).
* Body quads on layer 0, glow copies on layer 2 (the same quad drawn additive with the glow skin), at most 3000 quad lines (the builder stops at 2800). The LOD copy has no glow pass.
* Glow header (`glow RRGGBB speed min max`): the devil's colour, lifted where the WorldDevil colour is too dark to glow (liebe `F0F0FF`, astaroth `FFE07A`, ...). The glow skin itself carries the true emissive colours.

Height: every body stands 2.0 blocks to the top of the head (head top at y 32.2 to 32.6), as the player model plus a little; horns, crowns, halos and wings go above that (the tallest tip
is listed per devil). Width across the wings is listed too.

## The eight

| id | tier, attribute, colour | body + glow quads (LOD) | head part (horns incl.) / tallest tip | roles |
|---|---|---|---|---|
| `liebe` | High, Anti-Magic, `2A2A2A` | 1638 + 29 = 1667 (754) | 2.31 / 2.31 blocks | 8 + wing_l, wing_r |
| `lucifero` | Supreme, Gravity, `5A1E8A` | 2457 + 90 = 2547 (1128) | 2.52 / 2.78 blocks | 8 + wing_l, wing_r |
| `beelzebub` | Supreme, Spatial, `3A4AC8` | 2073 + 226 = 2299 (950) | 2.45 / 2.45 blocks | 8 + wing_l, wing_r |
| `astaroth` | Supreme, Time, `F5D76E` | 2485 + 286 = 2771 (1150) | 2.49 / 2.67 blocks | 8 + wing_l, wing_r + tail |
| `megicula` | High, Curse, `8A2A5A` | 2433 + 175 = 2608 (1119) | 2.09 / 2.18 blocks | 8 + wing_l, wing_r + tail |
| `lucifugus` | High, Word, `B06AFF` | 2186 + 416 = 2602 (930) | 2.38 / 2.38 blocks | 8 + wing_l, wing_r |
| `god_hand` | Supreme, Causality, `8A0A14` | 2272 + 168 = 2440 (1045) | 2.47 / 2.50 blocks | 8 + wing_l, wing_r |
| `etherious` | High, Ether, `6A3CFF` | 2150 + 109 = 2259 (1003) | 2.50 / 2.50 blocks | 8 + wing_l, wing_r + tail |

Previews: front three-quarter `docs/devil_forms/previews/<id>.png`, back `<id>_back.png` (Cycles, grey world, baked skins).


### What was read for the accuracy pass (rule 10)

* **Read**: the English Wikipedia character lists saved by the coordinator (`bc.txt` for Black Clover, `berserk.txt`, `ft.txt` for Fairy Tail). They give **roles, powers and hosts, almost nothing about looks**.
* **Tried and failed**: the fandom wikis (`blackclover`, `berserk`, `fairytail`: no connection / 403 for scripted clients, not worked around) and `ja.wikipedia.org` (one plain request with a descriptive client name
  got HTTP 429 "too many requests" from the shared egress, not retried in a loop). **Nothing from the Japanese wiki was read.**
* **Memory** (mine, not a source) is what every "look" entry below that says "memory" rests on; `docs/arc/lucifero.md` (an earlier session's notes, themselves written from search-result summaries,
  marked weak there) backs Lucifero's look. Where the confidence is low, the design is a restrained original and says so.

Facts the sources do give, which matter for the mod's own data: **Lucifugus's magic is Dark Magic** in the English list (Word / Kotodama belongs to Zagred); the mod's `WorldDevil` calls it "the Kotodama devil".
**Megicula's** Curse-Warding Magic is written 呪符魔法 (talisman magic) and she is hosted by Vanica Zogratis; **Beelzebub** (Spatial Magic) by Zenon, **Lucifero** (Gravity) by Dante, **Astaroth** (Time) by Lucius.

### liebe (Black Clover, the Anti-Magic devil)

**Design**: a lean black-bodied devil with white blank eyes and a wide sharp-toothed grin, two short horns, pointed ears, long clawed black hands, a tattered loincloth, and ragged dark wings made of long
blades. No crown, no seals, no tail. White glow only in the eyes and the grin. Header glow `F0F0FF` (the WorldDevil colour `2A2A2A` would not glow).

| Feature | Basis | Confidence |
|---|---|---|
| Anti-Magic devil, Asta's devil, Asta's adopted brother | English Wikipedia list (read) | high (role only) |
| Black body, white eyes, wide grin, horns | memory of the anime (his giant form and Asta's Devil Union) | medium |
| Dark ragged wings | memory (Devil Union shows black wings) | low to medium |
| Everything else (hair, clothes, tail, markings) | **not drawn**: the first draft's thorn crown, white seals, ribs and tail were removed as unsupported | n/a |

Not done: his small "true form" (a boy-sized figure) is not drawn; only the devil form. Quads and previews: table above.

### lucifero (Black Clover, King of Devils)

**Design**: a tall thin figure, grey skin with black patterns over the whole body (violet tint = the mod's colour), long white hair down the back, two pairs of horns (one pair up, one pair sweeping down at the
sides), two pairs of hooked bat wings (large upper, small lower), thin bony hands with long fingers, spikes on the shoulders and elbows and a third eye on the forehead (his full-power look), a dark tattered skirt.
Violet glow only in the eyes and the third eye (the first draft's glowing ribs and chest gem were removed).

| Feature | Basis | Confidence |
|---|---|---|
| King of Devils, Gravity, hosted by Dante, manifests through Qliphoth | English Wikipedia list (read) | high (role) |
| Tall, thin, grey skin with black patterns, long white hair | `docs/arc/lucifero.md` (earlier-session notes from search summaries) + memory | medium to high |
| Two pairs of horns, two pairs of hooked bat wings, thin bony hands | same notes + memory | medium to high |
| Shoulder and elbow spikes, thicker horns, a third eye (at full power) | same notes | medium |
| Violet as an accent only | the mod's colour, not canon (canon skin is grey) | n/a |

Not done: the boss-scale 2.7 block body is a different file (`lucifero*.txt`, untouched); this is a player-scale copy, so the proportions are compressed.

### beelzebub (Black Clover, ruler of the underworld, Spatial)

**Design (original, low confidence)**: a broad blue chitin-armoured devil with a compound-eyed insect helm, two long antenna horns, mandibles, a second, folded pair of small arms, layered pauldrons, a beetle
carapace of overlapping chest plates, claw hands, spiked knees, two pairs of glassy dragonfly wings with glowing veins, and a broken ring of space shards behind the head.

| Feature | Basis | Confidence |
|---|---|---|
| One of the three highest-ranking devils, Spatial Magic (portals, dimensional spaces), hosted by Zenon (Spatial and Bone Magic) | English Wikipedia list (read) | high (role) |
| The insect look (compound eyes, antennae, extra arms, four wings) | **from the name** ("Beelzebub, lord of the flies"), not from a remembered design | low |
| Space shards and glowing rift lines | from the Spatial attribute (the mod's design) | low |

I do not remember his canon appearance with any confidence, so this is a themed original; it should be replaced if the owner has a reference. Not done: Zenon's bone armour (the host's Bone Magic) is not drawn.

### astaroth (Black Clover, ruler of the underworld, Time)

**Design (original, low confidence)**: a tall robed figure in gold plate and ivory, a porcelain face mask with amber eyes, long swept-back gold horns, a clock-face breastplate with a dial, a halo of gear teeth with two
clock hands behind the head, wings made of a fan of long gold blades with glowing centres, and a pendulum on a chain as a tail.

| Feature | Basis | Confidence |
|---|---|---|
| One of the three highest-ranking devils, left the underworld, Time Magic, resides in Lucius | English Wikipedia list (read) | high (role) |
| Gold as the colour | the mod's colour (`F5D76E`), not canon | n/a |
| Every shape (mask, robe, clock, halo, blade wings, pendulum) | invented from the Time attribute | low |

His canon look is not remembered with confidence; this is a themed original, to be replaced if the owner has a reference.

### megicula (Black Clover, the Curse-Warding devil)

**Design (original, low to medium confidence)**: a feminine devil with plum-wine skin and black curse patterns, wide ram horns curling round the ears, long braided hair, a thorn collar, a bandeau and a torn veil skirt,
a belly seal, chains with hanging talisman plates (the glowing plates), thorned bracers, hooved boots, small ragged wings and a thorned tail with a spade tip.

| Feature | Basis | Confidence |
|---|---|---|
| Supreme devil of curses, hosted by Vanica Zogratis, speaking her name is dangerous | English Wikipedia list (read) | high (role) |
| Curse-Warding Magic is 呪符魔法 (talisman magic): the hanging talismans and the belly seal | the kanji in the English list (read), my reading of 符 | medium |
| Feminine form (she is shown through a woman host and is a "she") | the English list's pronouns | medium |
| Ram horns, braids, tail, wings, outfit | invented | low |

Not done: her giant manifested form is not drawn.

### lucifugus (Black Clover, "the Kotodama devil" in the mod)

**Design (original, restrained, low confidence)**: a very tall narrow figure in a high-collared indigo robe written over with glowing lilac script, a blank white mask with one slit eye and a stitched mouth,
two small horns, wide hanging sleeves, thin hands, and wings of hanging paper strips each with a glyph.

| Feature | Basis | Confidence |
|---|---|---|
| One of the highest-ranking devils, served Lucifero, Astaroth and Beelzebub, **Dark Magic**, was incarnated in the Paladin Morgen | English Wikipedia list (read) | high (role) |
| The script, the strips, the mask (a Word / Kotodama theme) | the **mod's** attribute (WORD), which the source does not give (it says Dark Magic) | low; **flag: the mod's attribute and the source disagree** |
| The look in general | not remembered | low |

Removed in the revision: the spiral horn and the floating tablets of the first draft.

### god_hand (Berserk, one figure for the God Hand)

**Design**: a tall armoured lord in the manner of Femto, the reborn Griffith: dark spiked armour with a crimson trim (the mod's colour), large pauldrons with one spike each, a long crimson cape, a helm with
two long swept horns and a swept crest over burning slits, and **huge white feathered wings** edged in a red glow. On the chest hangs the Behelit, a small egg with a face, on a chain.

| Feature | Basis | Confidence |
|---|---|---|
| The Eclipse, the Behelit that summons the God Hand, Griffith reborn as Femto | English Wikipedia plot text (read) | high (story) |
| The Behelit is an egg-shaped object with a face | memory | high |
| Femto: horned helm, spiked dark armour, a cape, large white wings | memory of the manga and its figures (the list mentions Femto and the God Hand figures but not the look) | medium |
| The other four members (Void, Ubik, Slan, Conrad) | **not drawn**: the owner asked for one figure; a Femto-type figure is the best known | n/a |
| Crimson | the mod's colour, not canon | n/a |

Removed in the revision: the black wings, the crown of five spires and the large chest egg of the first draft.

### etherious (Fairy Tail, a demon of the Books of Zeref)

**Design (typical, not a named demon)**: a lean demon with dark violet skin, a long dark tailcoat with a high collar and lapels, two broad horns sweeping outward and up, spiky white hair, glowing slit eyes, claws,
bat-like wings with spines, a long tail with an arrowhead, and thin glowing rune bars on the forearms.

| Feature | Basis | Confidence |
|---|---|---|
| Etherious are demons from books written by Zeref; Tartaros is a guild of them; Mard Geer commands it; E.N.D. is Zeref's strongest | English Wikipedia list (read) | high (role) |
| Horns, tail, a coat, a lean build for the Tartaros demons | memory | medium |
| Wings, white spiky hair, rune bars | memory / invented | low |
| Violet | the mod's colour | n/a |

Removed in the revision: the glowing seal (ring and star) on the chest of the first draft. There is no one canon Etherious, so no single character is imitated.


## Rule 5: new or replacement

All eight are **NEW**: nothing in the mod had a player-scale body for these devils. `lucifero` has a boss mesh (`entity_meshes/lucifero*.txt`, 2.7 blocks, kept untouched); it was used as a
reference only, and the new `devil_lucifero` is a separate, player-scale file (no id clash: `devil_lucifero`).

## Worst-case cost (rule 11)

Client drawing only; no server, network, tick or state cost from these files. Per devil in view per frame: at most 2800 quad lines (the largest is listed in the table) in two batches (cutout
body, additive glow), 8 to 11 part matrices; far away the LOD copy (about 40 percent of the quads, no glow pass). Memory: one parse per renderer per resource reload, about 0.2 to 0.5 MB of text each
(packed by `meshFolders` as the boss meshes), skins 1024 px, palette PNGs of 262 to 286 KB (`tools/shrink_pngs.py`, then a tighter palette so each stays under 300 KB; glow skins 9 to 35 KB).

## Sources (rule 10)

See the first section under "The eight": what was read (the English Wikipedia character lists, roles only), what failed (the fandom wikis, one 429 from ja.wikipedia), and which looks are memory. Each devil has its own basis table with a confidence.

## What could not be done / honest caveats

* **Nothing here was seen in a game** (no client): the bodies are checked by Blender renders (Cycles, a plain sun and a grey world, not the game's lighting), a pure-Python re-implementation of
  the mesh rules (`devil_mesh_check.py`), and not by `BossMeshWave2Test` itself (Gradle/Java were not run in this task; the new files are in no Java test list).
* The bodies are stylised low-poly figures from procedural builders: heads are simple (a skull, brow, nose, chin), hands are a palm blob with claws, legs are one part (no knee bend, as the pose
  solver only turns each part about its pivot). They reach the Djinn bar in silhouette and colour theme, not in surface detail.
* Looks are from memory and earlier notes (see the confidence tables): Lucifero and the God Hand (Femto) are the best remembered; Liebe is medium; Beelzebub, Astaroth, Megicula, Lucifugus and the Etherious are
  restrained originals in the series' style and are marked low confidence.
* Skin colours are baked under the studio light of `bl_lib.reset`, as the weapons and bosses (the game's own light is applied on top).
* The wings and tails are separate parts so the draw code can beat or sway them; at rest they are posed: wings half spread, tails curving back and down.
* A UV or glow-skin problem could only show in game: check first the glow strength (the glow skin is saturated), the wing parts' pivots when they beat, and the 2.0 block height against the hitbox.
