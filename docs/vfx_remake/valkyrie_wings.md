# Valkyrie armour as a 3D render-layer armour, v2 + the core drill lance (3 versions) + Mermaid look: PREVIEW ONLY

Owner request: the Valkyrie armour (Noelle's Valkyrie Dress) as a player-fitted 3D render-layer armour like the Mode Armor overlays, "as close to the image as possible"; then "try again": the first build
(v1, stiff faceted crystal wings, thorn frills, boot greaves) was not close enough. v1 is kept untouched (rule 2): `docs/vfx_remake/valkyrie_wings_v1.md`, `docs/previews/vfx_remake/valkyrie/v1/`,
`tools/gen_valkyrie_armor_meshes.py`, `tools/vfx_preview/valkyrie_preview.py`. v2 is new files next to it.
Nothing in `src/` or `assets/` was changed, nothing is wired into the game, no version, changelog, README, lang file or workflow was touched, Time Magic VFX untouched (rule 3), the Water BC builder's files untouched.

**Honest limits (read first).** Software render (numpy + Pillow); no bloom, no world light or lightmap; additive drawn as plain addition; the 8 textures are procedural 64 x 64 sheets drawn by Pillow, not hand-painted
art; the stand-in is the block figure of `tools/mode_preview/preview.py` (same boxes) with hair, eyes and raised twin tails added, NOT the player model or a skin; bubbles, foam and the green background are flat 2D
drawing; nothing was seen in a game; GPU cost not measured; the Java layer is not written; UV layout is per piece (an atlas is a wiring-time job).

## 1. Files (`docs/previews/vfx_remake/valkyrie/`)

| File | What |
|---|---|
| `valkyrie_ref_crop.png` | the reference x4 with the five crops studied at full zoom (left wing, right wing, tiara, bodice + book, legs) and what each shows |
| `valkyrie_vs_ref.png` | fair side by side: the figure posed like the still (arms raised and bent out, head back, hovering, legs apart, twin tails up), 3/4 front camera slightly from below, underwater green with light streaks, translucent sphere bubbles (behind and in front of the figure), foam puffs |
| `valkyrie_wings_front_back_side.png` | turntable, 8 angles, NEAR level |
| `valkyrie_flutter.png` | five key poses of the flutter incl. the travelling wave, from behind and 3/4 front |
| `valkyrie_player_fit.png` | player-fitted: standing, casting with both arms up, one arm up (lance pose), sneaking, walking; front / 3/4 / side / back |
| `valkyrie_saint.png` | the SAINT look next to the manga page of `Saint_Valkyrie_Dress.webp`, plus front / 3/4 / side / back |
| `valkyrie_lod.png`, `valkyrie_pieces.png` | NEAR / MID / FAR with counts; every piece alone |
| `valkyrie_first_person.png` | what a hand-render hook would draw in first person (right arm: sleeve, shoulder cap, cuff, sleeve veil) |
| `meshes/valk2_*.txt`, `pieces_v2.json`, `meshes/counts_v2.txt`, `tex/valk2_*.png`, `ref/*.png` | the meshes (`q` text format of `gen_sdw3_models.py`, `Mesh` class), bone / texture / look table, counts, the 8 textures, the two references |

Rebuild: `python tools/gen_valkyrie_armor_v2.py` then `python tools/vfx_preview/valkyrie_v2_preview.py [ref|turn|vs|flutter|fit|lod|pieces|saint|fp]` (numpy + Pillow, about 25 s for everything).

## 2. The reference, studied again (crops of the 200 x 144 still, `valkyrie_ref_crop.png`)

* **Wings.** Not stiff crystal and not butterfly lobes: **thin, smooth, translucent blades**. The viewer-left wing is a long slim crescent, sharp tip, widest about 40 % up from the shoulder, a bright cyan-white
  line along its outer edge, a teal body that gets clearer toward the inner edge. The viewer-right wing is wider and has a stepped, frilled trailing edge and a long needle tip. Both root at the shoulders / upper
  arms and run into the translucent sleeves of the raised arms. About 25 to 40 degrees from vertical. The still is drawn with anime proportions (long neck and legs), so the wings look shorter against the body
  than they can on the 4-heads-tall player model.
* **Tiara.** About five slim pale thorns on a band; dark hair loops up beside it (hair, not armour).
* **Bodice.** Layered, rounded frosty feathers / petals in white to aqua, a ruffled shoulder; short skirt of the same petals over dark legs.
* **Grimoire.** A flat white sheet with soft glowing edges at the hip.
* **Legs.** Pale twisted ropes of water, a crown of finger-like points at the top, foam below. Not boots.

## 3. Research (rule 10)

Unchanged from v1: WebFetch could reach no host (DNS failures), so only WebSearch result summaries were available; the fandom pages `Valkyrie Dress` / `Saint Valkyrie Dress` were listed by the search with
summaries but not opened; Japanese searches found no design page; no manga chapter / anime episode numbers for the first Valkyrie Dress turned up; "Sea Nymph Valkyrie Dress" has no source. What the summaries
said and how it was used: breastplate, faulds, spaulders, vambraces, chausses, tiara (piece list); "wing-like ornaments cover the ears" and "two large wing-like ornaments hang off the lower back" (ear feathers on
the tiara, the small lower wing pair); Saint version = small crown with a halo, feathered wing-like ornaments (the Saint look); Ugly Duckling theme with white feather-frilled clothes (the petal frills).
The images in the elemzip pack (`Valkyrie_Dress.webp`, the owner's reference, and `Saint_Valkyrie_Dress.webp`, the manga page) are the real sources; both are kept in `ref/`.

## 4. What changed from v1 to v2

| Owner priority | v1 | v2 |
|---|---|---|
| 1 wings | two faceted crystal slabs with thick edge strips | **soft veil**: one thin curved membrane per wing, a smooth 14 x 3 grid with a gentle static ripple, a scalloped trailing edge (4 smooth scallops), a second frill sheet over the outer half (offset in front, out-of-phase scallops), a bright rim strip along the leading edge, two glowing veins; **the gradient and the transparency are in the texture** (`valk2_veil.png`: white-cyan core and rim line, teal toward the trailing edge, alpha 0.93 down to 0.5, lighter inner glow band, faint veins), the mesh has real UVs (u across the veil, v along it); the wave and flutter move it per vertex |
| 2 frills | folded two-quad thorns | **rounded petal cards**: one textured card per petal with a rounded alpha mask (`valk2_petal.png`, white at the top to aqua at the hem, a rim line, a faint centre vein), two quads so it bends, tiers overlap like feathers; bodice 3 tiers + a rising collar, skirt 3 tiers (9 / 10 / 11 petals), shoulder caps 2 tiers, wrist cuffs 1 tier |
| 3 wing roots | hinge behind the neck | hinge at the shoulder blade (4.6, 21.5, -2.2), narrow root that opens quickly, plus a **sleeve veil** hanging off each arm (`arm_veil`) and a translucent **sleeve** tube (`sleeve`), so with the arms up the veils run together like the still |
| 4 legs and arms | boots, bare arms | legs = a translucent aqua sleeve, 3 spiral strands of water and a crown of flame fingers at the hip (`leg`); arms = translucent sleeve, frilled shoulder cap and cuff |
| 5 comparison image | pose and camera loosely like the still | pose built from the still (arm angles, head back, hover, legs apart, tails up), same aspect (1000 x 720), translucent sphere bubbles in front of and behind the figure |

## 5. The pieces (rest-pose player space, px, left copy written; the layer draws the mirror)

Layers: 0 plate (flat toon), 1 emissive flat, 3 CARD (textured, alpha-tested), 4 VEIL (textured, translucent, no culling), 5 GLOW (textured, additive). Render types the layer would use: CARD and plate
`entityCutoutNoCull`, VEIL `entityTranslucent` (no cull, so one sheet is enough), GLOW / emissive `SafeAdditive` (as in `ModeArmorWindV2`): 4 buffer switches per wearer.

| Piece | Bone | Look | Near / mid / far quads (per copy) |
|---|---|---|---|
| `wing_main` x2 | back hinge | VALKYRIE | 79 / 30 / 10 |
| `wing_low` x2 (small lower pair, from the wiki) | lower-back hinge | VALKYRIE | 24 / 10 / 6 |
| `arm_veil` x2, `sleeve` x2 | arm | VALKYRIE / sleeve both | 15 / 6 / 2, 12 / 5 / 4 |
| `crown` (slim thorns, ear feathers) | head | VALKYRIE | 33 / 27 / 15 |
| `bodice`, `skirt` | body | both | 98 / 62 / 30, 60 / 34 / 14 |
| `shoulder` x2, `cuff` x2 | arm | both | 30 / 12 / 10, 12 / 10 / 8 |
| `leg` x2 | leg | both | 39 / 19 / 10 |
| `grimoire` (optional) | body | both | 9 / 3 / 3 |
| `saint_wing` x2 (bird wings of feather cards: arm bone, 9 primaries, 7 secondaries, 8 coverts) | saint hinge | SAINT | 50 / 30 / 16 |
| `saint_halo`, `saint_crown` (winged helm), `saint_sash` (gold) | head / head / body | SAINT | 16 / 10 / 6, 29 / 23 / 15, 14 / 12 / 6 |

**Flutter.** No stored data: `w = |vertex - hinge| / wing length`; each vertex rotates about the hinge by `flap + bend * w^1.4` (about z) and `sweep * w` (about x), then moves along z by
`amp * w * sin(2 pi * 1.3 * v - phase) * (0.25 + 0.75 u)`, i.e. a wave travelling from the root to the tip, stronger at the trailing edge. Idle proposal: `flap = 5 sin(0.12 t)`, `bend = 6 sin(0.12 t + 0.6)`,
`sweep = 2 sin(0.09 t)`, `amp = 0.55`, `phase = 0.2 t`. Key poses A to E (`valkyrie_flutter.png`) are the extremes for a cast flare. The Saint wing uses the same hinge rule (flap about the shoulder).

## 6. The Saint look (second selectable look, decision 1 = yes)

Same plates, frills, legs, sleeves and grimoire; replaces `wing_main`, `wing_low`, `arm_veil`, `crown` by `saint_wing` (feathered bird wings, white with a hint of gold at the quills), `saint_halo` (a thin
glowing ring above the head), `saint_crown` (small crown + three swept-back feather blades each side) and `saint_sash` (gold band). Seen in `valkyrie_saint.png`; the manga pose spreads the wings wider and
asymmetrically, ours are symmetric; the manga armour is segmented plate, ours reuses the frill set, so the Saint look is closer in silhouette (halo, wings, helm) than in the armour itself.

## 7. How it would plug in (not done: preview only)

* **Layer.** New client class `client/mode/ModeArmorValkyrieV2` modelled on `ModeArmorWindV2`: static arrays built once by a pure `ModeArmorValkyrieMesh` (loads `assets/nusmp/valkyrie_meshes/valk2_*.txt`),
  one captured `PoseStack.Pose` per bone (body, head, arms, legs, two wing hinges), pass by pass draw, a mirrored second copy of paired pieces; only wing vertices do per-frame maths.
* **Hook.** `ModeArmorLayer.render0` already has `if (ModeArmorWindV2.active(mode))` before its `switch`: add the same line for `ModeArmor.Mode.VALKYRIE`; the old `ModeArmorLayer.valkyrie()` stays as CLASSIC.
  In the Spirit Dive, `SpiritDivePieces.WATER_VALKYRIE` / `WATER_SAINT` (`water:wing_*`, `helm`, `halo`, `saint_wing_*`) would be skipped through a `replacesAuraPieces`-style check while the layer is on.
* **Settings (CLIENT config, `nusmp-modearmor-client.toml`, appended next to `windLook`).** `modeArmor.valkyrieLook` = `V2` or `CLASSIC` (CLASSIC restores the old overlay); `modeArmor.valkyrieForm` =
  `VALKYRIE` or `SAINT` (which look V2 draws; the server-side mode / form decides who wears it, a Saint dive could force SAINT). Only changes what this client draws (rule 9).
* **First-person gauntlets (decision 4 = yes).** The entity layer is not drawn for the local player in first person. A client-only hook is needed: NeoForge `RenderArmEvent` (fired when the first-person
  arm is drawn; the name and signature must be checked against the NeoForge version at wiring) or `RenderHandEvent`, drawing only the right-arm pieces (`sleeve`, `shoulder`, `cuff`, `arm_veil`) with the event's
  pose stack and the hand's pose, behind the same setting. `valkyrie_first_person.png` shows those pieces (rough: the camera sees the arm from behind-above).
* **Server / network.** None: the mode already reaches clients through `ModeArmor.ModePayload`. No new packet, no server state.
* **Textures.** 8 sheets of 64 x 64 (`tex/`), pack them into one 128 x 128 atlas (`textures/entity/mode/valk2_atlas.png`) at wiring and remap the UVs in the loader.

## 8. Budgets (rule 11; the armour has its own 1200-quad pool, decision 3)

Per wearer, counted from the meshes (4 vertices per quad), both copies of paired pieces, grimoire included:

| Look | NEAR (inside 16 blocks) | MID (to 32) | FAR (to 64) | beyond |
|---|---|---|---|---|
| VALKYRIE (with scarf and fin greaves, wide wings) | **692 quads** (2,768 vertices) | **344** (1,376) | **174** (696) | 0 |
| SAINT | **512** (2,048) | **296** (1,184) | **170** (680) | 0 |
| MERMAID (optional look) | **343** (1,372) | **177** (708) | **117** (468) | 0 |

Own pool: **1200 quads (4,800 vertices) per frame for all armour wearers together**, whatever their number. Admission nearest first, each wearer gets the best level that still fits under the running cap, a wearer
that fits at no level is not drawn: without a lance 1 NEAR (692) + 1 MID (344) = 1,036 and a FAR (174) would make 1,210, over the cap, so the third wearer is not drawn; the numbers with a lance are in section 13. This pool is separate from the 1200-quad ceiling of the Water BC effects
(so the worst case for armour + Water effects together is 2,400 quads). Per-frame CPU: wing vertex maths only (NEAR about 1,100 vertices: 2 x 90 + 2 x 24 quads), static arrays and pose slots, no per-frame allocation,
4 buffer switches per wearer. Network 0 packets, server 0 work. `PerfSourceScanTest` needs no new pattern.

## 9. How well it matches now (6 rounds; honest)

Rounds: (1) veil built, wings invisible (low alpha, bright veins hid it); (2) opaque veil texture, thinner veins, more cyan; (3) wider wings, stronger rim; (4) sleeves and leg sleeve, camera closer, wing length cut
from 36 to 32 px; (5) Saint wings sized down, first-person view; (6) final pass and previews.

* **Now close:** the V of long slim curved translucent blades with a bright rim and a teal body, scalloped soft trailing edge, flowing out of the shoulders into translucent sleeves; the slim spiked tiara;
  layered rounded frosty petals on bodice, shoulders and skirt; the glowing soft-edged book at the left hip; pale rope-like legs; bubbles as translucent spheres; the aqua-green water with streaks.
* **Still different:** the still's wings are bigger against the body and the right one is wider with a visible step; ours are symmetric and a little narrow, and their cyan is a bit too saturated and clean
  (the still is murkier, with painted highlights); the still's shoulders and arms are slim and bare under thin sleeves, ours are block arms with a bulky translucent tube; the figure is a block player, so
  the head, face, hair loops, long legs and the raised bent elbows of the still cannot be matched; the legs read as pale blocks with stripes instead of flowing water ropes with fingers; the foam,
  splashes and light streaks are flat 2D and look cartoonish next to the painted still; no bloom; the petals are a little regular, the still's frill is messier.

## 10. NEW / REPLACED / KEPT (rule 5)

* **NEW:** v2 generator, textures, meshes (15 pieces x 3 levels), preview tool, the SAINT look, the first-person arm preview, all images above, this doc.
* **REPLACEMENT of v1** (owner: "try again"): the v2 images and meshes take the top-level names; v1 is moved, not deleted (`v1/`, `valkyrie_wings_v1.md`).
* **KEPT:** v1 files, the classic overlay, the geo pieces and every old preview.

## 11. Open points

1. Wing width and symmetry: a wider, stepped right wing like the still (asymmetric look) would need two wing meshes (about +40 quads) or a mirror flag; do you want it?
2. First-person hook: the event name / signature is unverified against the project's NeoForge version.
3. The blocky stand-in limits what the previews can prove; the real check is the player model with a skin in the game.
4. Texture art: the procedural sheets are placeholders in the same spirit as the other generated textures; hand-painted veil and petal sheets would raise the match more than any further mesh work.


---

# Update: the core weapon (the Valkyrie drill lance), three versions, plus the Mermaid look

Owner: "the core weapon is a water spear or lance" (first ask), then from the frames the real shape: the **giant spiral drill lance**; "show me 3 different ones before building it". Nothing is built into the game
(no item, no `src/`, no `assets/`): this is the preview of three candidates and the plan for the item. Wider wings, the blade-scarf and the fin greaves from the new frames are in the armour too.

## 12. Frames studied and research (rule 10)

Frames on disk (owner): `ref/valkyrie_ref_131_drill.png` (Valkyrie Dress, cel; the drill held out behind her to the upper left), `ref/valkyrie_ref_132_mermaid.png` (Mermaid Form, the lance below the tail),
`ref/valkyrie_ref_133_mermaid_art.png` (Mermaid fan art); described by the owner (not on disk): a diagonal flying frame with the drill at the hips / legs like a tail and wide marbled veil wings with folded ribbon
frills, and a three-panel sheet (Valkyrie Dress, Mermaid Form, Saint Valkyrie Dress).
What the frames show: **131** the drill is a long cone, wide at the base, with blue and white spiral stripes and fine ribs and a spiral groove, a big **mane of jagged white foam tufts** round its lower third sweeping
back toward the butt, a needle point, a blue water ribbon looping round the base; the armour has round shell discs at the ears of the tiara, plated aqua armour, **pointed fin boots**, and a **long curved blade-like scarf**
sweeping out from the waist; **132** the tail is a scaled fish tail with fan fins and a coiled curl, the lance is a slim dark-blue pointed spear with foam rings, held beside the tail; **133** a small spiral drill horn at the side of the head,
scaled pauldrons, a spiked coronet, a scalloped tail.
Search (WebSearch summaries only, WebFetch cannot resolve any host): one article says the Valkyrie Dress carries "a lance made out of spiraling water", ComicBook.com (episode 108) says the armour is "complete with a shifting
water lance" (low-detail sources, the manga and anime are the real ones); the fandom summary says the Saint form changes the lance: **"the lance's texture smooths out, and a rounded guard extends from the vamplate and
connects to the base of the handle"** (so the base form has a rough / ridged lance, the Saint form a smooth one, both with a vamplate hand guard); Japanese searches (`螺旋 ランス`) found no page naming the weapon.
The old mod overlay (`ModeArmorLayer.valkyrie`) already draws "the spinning drill lance" as cone and box cards; the mod's water spells `aqua_javelin` / `tide_lance` use a javelin model; the pack stills `Aqua_Javelin_Rhya`
(a long spear of water with flowing ribbons and a leaf head), `Sea_God's_Hammer`, `Solid_water_bullet` show how the mod's other water weapons look; the existing `sea_empress_trident` item is a different weapon (Requip).
Not found: an official name for the lance, a manga chapter or episode number for its first use.

## 13. The three versions (`tools/gen_valkyrie_lance.py`, meshes `meshes/lance_<A|B|C>[_mid|_lo].txt`)

All: length about 52 to 56 px (3.3 to 3.5 blocks), butt at y = 0, grip a quarter of the way up (`GRIP_FRACTION`), a grip and a rounded vamplate hand guard, a needle tip with a small glow, two blue water ribbons winding round the cone,
two ribbons trailing behind the butt. Layers (new 6 to 9 next to the armour's 0 to 5): FROST white foam plate, DEEP blue band plate, SCALE blue scale plate, GLASS translucent crystal.

| Version | What | Built from | Near / mid / far quads |
|---|---|---|---|
| **A  Frosted Drill** | the closest to frame 131: a fat cone with spiral blue / white stripes (the colour of each quad follows the spiral), fine ribs, one spiral groove, a mane of 40 jagged white foam tufts (cards, foam texture) round the lower half sweeping back, needle tip, a ribbon loop round the base | 131, the two-frame references | **213 / 106 / 44** |
| **B  Crystal Corkscrew** | a sharp translucent crystal screw: a three-lobed star section twisted 1.5 turns (a true thread), a thin glowing core inside it, four frosted ring collars, ribbons | 132 (slim lance with foam rings) and the "crystal" idea | **216 / 123 / 68** |
| **C  Living Water Dragon Lance** | a coiled sea-dragon tail: a scaled body winding round the axis (1.6 turns), fan fins on every segment (fin cards), a foam collar where the tail winds tight into a grooved drill point | 132 / 133 (scaled tail, fan fins) | **171 / 98 / 44** |

Images (`docs/previews/vfx_remake/valkyrie/`): `valkyrie_lance_compare.png` (each version attached at the legs, held with the arms raised, held ready to throw, floating beside the wearer: the hand is placed by the pose, the
floating one turns slowly about its axis), `valkyrie_lance_turntable.png` (3 x 8 angles), `valkyrie_lance_closeup.png` (tip and grip / base of each), `valkyrie_lance_vs_ref.png` (frame 131 and frame 130 next to each version on the figure),
`valkyrie_lance_saint.png` (the Saint look with each lance), `valkyrie_mermaid.png` (the Mermaid look with frame 132 beside it).

**Cost with the armour** (per wearer, near / mid / far): armour 692 / 344 / 174 plus lance A 213 / 106 / 44 = **905 / 450 / 218**; with B 908 / 467 / 242; with C 863 / 442 / 218. In the own 1200-quad pool one NEAR wearer with a lance
takes about 75 %: 1 NEAR (905) + 1 FAR (218) = 1,123; two MID wearers (900) fit with a FAR (218) = 1,118. The lance could also be dropped to MID at 16 to 32 blocks to save 100 quads per wearer. Per-frame CPU: the lance is rigid
(one pose), its spin is a rotation of the whole mesh (no per-vertex work), ribbons are static; the floating lance costs one extra pose.

**Recommendation: A, the Frosted Drill**, for the Valkyrie Dress: it is the shape of frame 131 (spiral stripes, foam mane, needle point) and the cheapest to read at a distance (white and blue stripes, silhouette like a cone with a fringe).
Use **C** for the Mermaid Form (the tail IS the lance in the frames; 132 / 133), and take **B's glowing core and crystal collars** as the Saint variant's smooth, holy lance (the wiki: smooth texture, rounded guard) if you want a third look.
What is still off in all three: facet count (they are toon-shaded polygons; the frames are painted), the foam mane of A is a spread of flat tufts (the frame's is a dense fluffy mass), B's core reads as a white pole, C reads as a blue twisted blob from afar.

## 14. The Mermaid Form (optional third look, `look = MERMAID`)

Pieces (`gen_valkyrie_armor_v2.py`): `mermaid_tail` (a scaled body round both legs that narrows, curls down and back, a fan tail fin and dorsal fins; the scales are a tiled texture, UVs repeat), `shell_top` (two
scalloped shell cups), `mermaid_frill` (waist frills), `horn` (the small spiral drill horn of 133), plus the shared `crown`, `shoulder`, `cuff`, `grimoire`. Near / mid / far: **343 / 177 / 117** quads; with lance C 514 / 275 / 161.
`valkyrie_mermaid.png`. Plug-in needs one more thing: the vanilla legs must be hidden or covered while the tail is on (set the leg parts invisible in the layer / a `RenderPlayerEvent`; the tail covers them at the hips only).
Not done: the twin tails are the stand-in's hair (no armour), no wrist bands beyond the shared cuffs, the tail's curl is fixed (it does not wave).

## 15. Armour changes of this round

* wings: wider (17 px), a stepped notch in the trailing edge, two stacked ribbon frill sheets (folded ribbons) instead of one, marbled veil texture (deep-blue marbling over the cyan; `valk2_veil.png`), 90 quads per wing near;
* `scarf`: the long blade-like scarf of 131 (18 / 12 / 4 quads), `fin_greave`: the pointed fin boots (15 / 11 / 4 per boot), both in the VALKYRIE look;
* the ear discs of 131 (round shell ornaments at the tiara) are NOT built yet (the crown keeps the ear feathers): decision.

## 16. The item plan (rules 6 and 7; the item is NOT built)

Name proposal: **Valkyrie Drill Lance** (id `valkyrie_drill_lance`), a Water-magic spear-class weapon, also what Noelle's Valkyrie Dress / Mermaid Form puts in the hand.
* **In-hand model** (as the Genesis Demon-Slayer): a builder `valkyrie_drill_lance(m, a)` in `tools/gen_weapon_models.py` appended to its weapon list, built from `lance_A` (the mesh frame is already the weapon frame: +y axis, butt at 0),
  written to `assets/nusmp/weapon_meshes/valkyrie_drill_lance.txt` and drawn by `client/WeaponRenderer.java`; textures `textures/entity/weapon/valkyrie_drill_lance.png` and the glow `_glow.png` (the second texture: needle tip,
  ribbons, belt: additive, pulsing through the `glow` line); the model json `models/item/valkyrie_drill_lance.json` (parent `builtin/entity`) gets its display transforms from the preset tool (length about 2.8 blocks: the
  1st-person scale about 0.35 to 0.4, apparent length at most 1.0, grip on the anchor); preview with `python tools/item_preview/preview_weapons.py`.
* **Moving part**: the drill cone is the one part that turns; `docs/asset_pipeline.md` rule 3 wants moving parts as separate named parts: name it `drill*` and add a spin to the renderer (about 90 degrees per second), which is a small
  new `WeaponRenderer` feature, or ship it static first.
* **Inventory icon** (rule 7): the old 16 x 16 sprite stays the icon (`valkyrie_drill_lance_icon.json`): concept = a diagonal drill, white / blue banded cone with a spiky white fringe in the middle, a blue grip bottom-left, a 2 px
  cyan glint at the tip, 1 px dark outline.
* **Grip entry**: `assets/nusmp/weapon_classes.json` lists `valkyrie_drill_lance` as class `spear`; `weapon_grips.json` `fractions`: `"valkyrie_drill_lance": 0.25` (the guard sits at the quarter), `GripTableTest` then passes.
* **Size**: `SIZE` in the generator about 1.9 to 2.0 (3rd person), long-weapon preset for first person.
* **Engravings** (`item/WeaponEngravings.java`, the `#tensura:engraving` ids that the file already uses): `barrier_piercing` 3 (a drill bores through wards, as the Legendary Spear), `elemental_boost` 2 (water),
  `crushing` 1 (it bores and breaks), `magic_weapon` 1 (a spell-made weapon); the Saint variant (smooth lance) would add `holy_weapon` 1 and drop `crushing`. The trident of the Sea Empress has `elemental_boost 2, barrier_piercing 1,
  magic_weapon 1`: this one is the heavier, wards-piercing one. Pick from the existing ids only; the final numbers are for the owner.
* **Not in this task**: the throw / drill attack (the existing `aqua_javelin` / `tide_lance` spells and the old overlay's spinning lance stay), crafting, loot, the lang entries.

## 17. How the lance plugs into the armour layer (not done)

`ModeArmorValkyrieV2` draws the lance as one more part: attached mode = a pose on the body bone (hips, pointing down-forward, about 3 blocks), held mode = the hand bone while the player holds the item or casts, floating mode =
a pose on the root. The preview places the grip on the hand and the direction by pose; the game would use the item display transform (held) and a fixed offset (attached / floating). One setting: `modeArmor.valkyrieLance` =
`OFF` | `ATTACHED` | `FLOATING` and the version `modeArmor.valkyrieLanceStyle` = `FROSTED` | `CRYSTAL` | `DRAGON` (client only).

## 18. Files of this round and rebuild

`tools/gen_valkyrie_lance.py`, `tools/vfx_preview/valkyrie_lance_preview.py` (new); `tools/gen_valkyrie_armor_v2.py` and `tools/vfx_preview/valkyrie_v2_preview.py` (extended, nothing removed);
`meshes/lance_*.txt`, `meshes/counts_lance.txt`, `lances.json`, `tex/valk2_fin.png`, `valk2_foam.png`, `valk2_scales.png`; the three new frames in `ref/`.
`python tools/gen_valkyrie_armor_v2.py ; python tools/gen_valkyrie_lance.py ; python tools/vfx_preview/valkyrie_v2_preview.py ; python tools/vfx_preview/valkyrie_lance_preview.py` (about 1 minute).
