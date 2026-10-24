> v1 of the Valkyrie armour (stiff faceted crystal wings), kept for reference; the redo is `valkyrie_wings.md`. Its image paths now live in `./v1/`.

# Valkyrie armour as a 3D render-layer armour (wings, tiara, bodice, skirt, shoulders, cuffs, greaves): PREVIEW ONLY

Owner request: "Valkyrie wings need to look like this as a 3D model since it's probably supposed to be Valkyrie Armor from the show, make it as close to the image provided and also use the internet to find more
references", then clarified: "i meant to say a render layer armor", i.e. the whole Valkyrie armour as a player-fitted 3D layer, like the Mode Armor overlays (`client/mode/*`, `ModeArmorWindV2`).
Nothing in `src/` or `assets/` was changed, nothing is wired into the game. No version, changelog, README, lang file or workflow was touched. Time Magic VFX untouched (rule 3). Everything is new files (rule 2);
the old Valkyrie overlay (`ModeArmorLayer.valkyrie`, the `water:*` Spirit Dive pieces) stays as it is.

**Honest limits (read first).** Software render (numpy + Pillow), no bloom, no world light or lightmap, additive drawn as plain addition, the bubbles, foam and green background in `valkyrie_vs_ref.png` are flat 2D
drawing, the stand-in is the block figure of `tools/mode_preview/preview.py` (same boxes) with hair and eyes added, NOT the real player model or skin. Nothing was seen in a game, GPU cost not measured, the
texture atlas / UVs are not made (UVs in the mesh files are placeholders). The Java layer that would draw it is not written. Counts are counted from the generated geometry.

## 1. Files

| File | What |
|---|---|
| `docs/previews/vfx_remake/valkyrie/v1/valkyrie_ref_crop.png` | the reference x5 with 8 numbered notes (wings, tiara, bodice, grimoire, skirt, legs, bubbles) |
| `.../valkyrie_wings_front_back_side.png` | turntable, 8 angles (front, 45 steps, back, sides) on the stand-in, NEAR level |
| `.../valkyrie_vs_ref.png` | the reference and the armour side by side: arms raised overhead, hovering, 3/4 front camera slightly from below, aqua-green background |
| `.../valkyrie_flutter.png` | five key poses of the wing flutter, from behind and from the 3/4 front |
| `.../valkyrie_player_fit.png` | player-fitted: standing, casting with both arms raised, one arm raised (lance pose), sneaking, walking; each front / 3/4 / side / back |
| `.../valkyrie_lod.png` | NEAR / MID / FAR from the front and the back, with the quad counts |
| `.../valkyrie_pieces.png` | every piece alone (near level, left copy) |
| `.../meshes/valk_<piece>[_mid|_lo].txt`, `pieces.json`, `counts.txt` | the meshes in the `q` text format of `gen_sdw3_models.py` (`Mesh` class), bone / pass / alpha table, quad counts |
| `.../ref/valkyrie_ref_130.png` | the owner's reference, kept so the preview tool is reproducible |

Rebuild: `python tools/gen_valkyrie_armor_meshes.py`, then `python tools/vfx_preview/valkyrie_preview.py [ref|turn|vs|flutter|fit|lod|pieces]` (numpy + Pillow only; about 15 s for everything).

## 2. The reference (read from the image, zoomed x5)

The image (200 x 144) is the same picture as `Valkyrie_Dress.webp` of the elemzip Water pack (pixel difference 0.0), i.e. the anime's **Valkyrie Dress** (the base form, not the Saint form, whose manga art is the
second picture of the pack: big feathered bird wings, halo, helmet crown). What it shows:

* **Wings.** Not butterfly lobes: two **long, narrow, curved blades** of translucent cyan glass that sweep up and outward from behind the shoulders in a V, about 30 degrees from vertical, tips far above the head
  (the right one a little longer, about 1.2 x the torso plus head). Brighter, thicker spine on the outer edge, thinner veil toward the body, widest near the shoulder where it folds like a sleeve, a rippled
  edge on the inner side, a needle tip. Colour: pale cyan to white, lighter at the spine.
* **Tiara.** A spiked crown on a band, 5 to 7 tall thorns, the middle ones tallest, pale ice blue. Side locks of hair rise like horns beside it (hair, not armour).
* **Bodice and skirt.** A short, frilly dress: overlapping frosty petals / feathers in white-aqua over the chest, a short flared skirt of the same petals, dark legs below it.
* **Arms.** Raised above the head, bent; shoulders covered by broad layered translucent sleeves that run into the wing roots.
* **Grimoire.** A flat, bright white glowing book at the hip.
* **Legs.** Wrapped in spiky ice-water that turns into foam and splashes at the bottom; bubbles (thin ring + glint) drift up all over the picture; the background is underwater green with horizontal light streaks.

## 3. Research (rule 10: English wiki first, Japanese as fallback)

WebFetch could not reach any site from this session (every host failed DNS: en.wikipedia.org, blackclover.fandom.com, epicdope.com), so only WebSearch result summaries were available, never the pages themselves.
Fandom pages were not fetchable (as expected); the search tool did list `blackclover.fandom.com/wiki/Valkyrie_Dress` and `.../Saint_Valkyrie_Dress` with a summary. What each source said:

| Source | Language | What it gave | Used for |
|---|---|---|---|
| blackclover.fandom.com `Valkyrie Dress`, `Saint Valkyrie Dress` (search summary only, not opened) | EN | water armour made of a breastplate, faulds, spaulders, vambraces, chausses and a **tiara**; "wing-like ornaments cover the ears" and "two large wing-like ornaments hang off the lower back"; the Saint version changes the tiara to a small crown with a **halo**, and the lower-back ornaments become feathered, wing-like | the ear fins on the tiara, the second, smaller pair of wing lobes hanging from the lower back (`wing_low`), the piece list (bodice, skirt = faulds, pauldrons = spaulders, gauntlets = vambraces, greaves = chausses) |
| en.wikipedia.org `Noelle Silva` (search summary) | EN | Valkyrie Armor is armour of water; bird theme from "The Ugly Duckling", white feather-frilled clothes, wing-like ornaments | the feather / petal frills of the bodice and skirt |
| sportskeeda, fandomwire, epicdope articles (summaries) | EN | Dragon Form (manga ch. 359) has horns and dragon wings and little coverage; Undine acquired in ch. 295; Saint Valkyrie Dress = Valkyrie Dress + Undine | context only, no design detail; the Dragon Form is NOT this task |
| tv-tokyo.co.jp `bclover` character page, ciatr.jp (search summaries) | JA | basic character data only; no mention of the dress design | nothing |
| JA search `ヴァルキリードレス 海神 ウンディーネ 翼 王冠` | JA | no page describing wings or crown found; points to official art, the mobile game collab or pixiv encyclopedia | nothing |
| elemzip pack, `Valkyrie_Dress.webp`, `Saint_Valkyrie_Dress.webp` (opened) | image | the reference above, and the Saint manga page (labelled 聖域・海神戦乙女の羽衣, "Sanctuary: the sea god Valkyrie's feather robe") | the design; the Saint feathered wings are the **optional later variant** (section 9) |

**Not fetched at all:** every wiki page body, the manga chapter and anime episode numbers of the first Valkyrie Dress (the searches gave none; the Undine / Saint form is ch. 295 per one article, unverified),
the name "Sea Nymph Valkyrie Dress" (three searches found no source for that exact name; it is not used as a fact here). The design therefore rests on the image first, the summaries second.

The mod's own references: `ModeArmorLayer.valkyrie` (the old flat-card overlay: box plate, 6 skirt cards, a flapping card wing pair, helmet wings, a spinning drill lance), `SpiritDivePieces.WATER_VALKYRIE`
(`water:chest, skirt, pauldron, vambrace, gauntlet, greave, helm, wing_r/l, tail, halo, backdrop` geo pieces) and `SpiritDivePieces.WATER_SAINT` (feathered wings, halo, gold sash). The new armour keeps the same
piece vocabulary (chest, skirt, pauldrons, vambraces/gauntlets, greaves, helm, wings, tail) so it can replace either. The lance is not part of this task (the Tide Lance has its own mesh in the Water BC plan).

## 4. The design (what was built)

All authored with the `Mesh` class (`tools/gen_sdw3_models.py`), rest-pose player space in pixels (1/16 block), y up, feet at 0, front +z. Paired pieces are written once (left, +x); the layer draws the mirror.

| Piece | Bone | What | Near quads (per copy) |
|---|---|---|---|
| `wing_main` x2 | back hinge at (+-3.4, 21.6, -2.8) | the V blade: a curved spar, a faceted thick membrane 15 px wide near the shoulder, a saw of five pointed lobes on the inner edge, front and back faces with a deterministic facet jitter, closed edge strips that are the **glowing outline** (layer 1), **glowing veins** (layer 1 ribbons in the mid-plane, seen through the glass): one bone along the spar and one fanning to each lobe tip. Length 34 px (2.1 blocks). | 70 |
| `wing_low` x2 | lower-back hinge (+-2.2, 15.5, -3) | the second, smaller pair hanging from the lower back (wiki: "two large wing-like ornaments hang off the lower back"): 14 px blades down and out, two lobes, a vein | 21 |
| `crown` | head | spiked tiara: a band round the brow, nine thorns (front ones tallest, up to 13.5 px), a glowing brow gem, two fin blades over the ears | 59 |
| `bodice` | body | the shell round the torso (8-sided, rounded), a standing fin collar, three tiers of folded frost petals (8 each, scalloped hem, two quads per petal), a glowing belt | 98 |
| `skirt` | body | three tiers of folded petals flaring from the waist, 8 / 9 / 10 petals, the lowest ends above the knee | 54 |
| `pauldron` x2 | arm | three stacked flared lames, a fin pointing out and up, a glowing lower edge | 39 |
| `gauntlet` x2 | arm | flared ice cuff on the forearm, a petal ruffle, three thorns | 39 |
| `greave` x2 | leg | spiky water boot: flared rings, a ruffle, four fin thorns | 48 |
| `grimoire` (optional) | body | the flat glowing book floating open at the left hip | 18 |

Shading in the layer (same recipe as the Water BC cores): per-quad 3-tone toon from the quad normal (dark aqua / mid aqua / frost white), one small white specular, a pale rim at the grazing angle,
emissive floor so nothing goes dark, layer 1 (outlines, veins, belt, gem, book sheet) emissive / additive, wings translucent with a fresnel alpha (clearer face-on, denser at the edge), plates opaque-ish.
Passes per wearer (as in `ModeArmorWindV2`): plate (cutout), glass (translucent wings), emissive: **3 buffer switches**.

**Flutter.** No stored data: the layer takes the weight of a vertex as `w = |v - hinge| / wing length` and rotates the vertex about the hinge by `flap + bend * w^1.4` (about z, opening / closing) and `sweep * w` (about x,
tips forward / back), so the tips move more than the root. Idle flutter proposal: `flap = 5 sin(0.12 t)`, `bend = 6 sin(0.12 t + 0.6)`, `sweep = 2 sin(0.09 t)` (slow, about 52 ticks per beat). Key poses in
`valkyrie_flutter.png`: A closed (-14 / -18 / +5), B (-6 / -8 / +2), C rest, D (+8 / +10 / -2), E open (+16 / +24 / -5). A and E are the extremes for a cast flare, not the idle range. The lower pair beats at
-0.7 of the main flap. Vertex work: only the wing vertices need a sine and a rotation per frame (near: (70 x 2 + 21 x 2) x 4 = 728 vertices); everything else is rigid and drawn from a pose per bone.

**Pose fit (seen in `valkyrie_player_fit.png`).** Plates follow the bones: body, head (yaw, pitch), both arms (forward and outward angles), both legs, sneak lean about the hips, hover offset. The wings and grimoire
ride the body bone, so they lean with the torso when sneaking.

## 5. How well it matches (honest comparison, 6 rounds)

Rounds: (1) first mesh, camera too close, wings short and thin, petals as thorns; (2) wings longer and wider (34 px, 15 px wide), camera pulled back; (3) crown taller; (4) frills rebuilt as folded scalloped petals;
(5) glass colour more cyan, foam as puffs; (6) budget trim and cleanup. `valkyrie_vs_ref.png` is the result.

* **Close:** the V of long curved translucent blades behind the shoulders, their size against the body, the brighter spine, the raised arms with layered shoulders, the spiked tiara, the frosty petal bodice and skirt,
  the glowing book at the hip, the spiky ice legs.
* **Not close:** the anime wings are softer and wider near the root (a draped veil), ours are a stiffer faceted crystal; the hair, face and the dark legs of the figure are the block stand-in, not Noelle;
  the water splash and foam under the legs is drawn flat in the preview (the mod's Water FX would supply it); the bloom and cel-painted glow of the still are missing; the petals read as spiky crystal at the
  edges where the still has a smooth frill; the sleeve-like fold on the shoulders (the wing root merging into the sleeves) is approximated by the pauldrons only.

## 6. How it would plug in (not done, the owner said preview only)

* **Layer.** A new client class `client/mode/ModeArmorValkyrieV2` following `ModeArmorWindV2` exactly: static meshes built once from `assets/nusmp/valkyrie_meshes/valk_*.txt` by a pure `ModeArmorValkyrieMesh`
  (loader + sway weights from the hinge distance), one captured `PoseStack.Pose` per part (body, head, arms, legs, the two wing hinges), pass-by-pass draw, a mirrored second copy for paired pieces.
* **Hook.** `ModeArmorLayer.render0` already has `if (ModeArmorWindV2.active(mode))` before its `switch`; add the same line for `ModeArmor.Mode.VALKYRIE` (`ModeArmorValkyrieV2.active(mode)`).
  `ModeArmorLayer.valkyrie()` stays as the CLASSIC look. The Spirit Dive path (`SpiritDivePieces.WATER_VALKYRIE`, `client.aura.WaterAura`) would use a `replacesAuraPieces`-style check (as
  `ModeArmorWindV2.replacesAuraPieces`) so the old `water:wing_*` / `water:helm` pieces are skipped while the new layer is on.
* **Setting.** CLIENT config key `modeArmor.valkyrieLook` = `V2` or `CLASSIC` in `nusmp-modearmor-client.toml` (appended next to `windLook` / `windArms` in `ModeArmorWindV2Config`), mirrored into a volatile
  field like `ModeArmorWindV2.setLook`. CLASSIC restores the old overlay. It only changes what this client draws (rule 9).
* **First person.** `ModeArmorLayer` is an entity render layer: it is not drawn for the local player in first person (it was not checked whether the classic overlay adds a hand hook; none was found in `render0`).
  Showing only the gauntlets in first person would need a hand-render hook, listed as an open point.
* **Server / network.** None: the mode already reaches clients through `ModeArmor.ModePayload`; the server keeps deciding who wears it. No new packet, no server state, nothing keyed by anything but the entity id.
* **Textures.** Not made here. At wiring time: a 3-cell toon atlas (dark / mid / frost) plus a glow cell and a glass cell in `textures/entity/mode/valk_*.png`, UVs remapped by the loader (the placeholder UVs of the
  files are not used).

## 7. Budgets (rule 11)

Per wearer, counted from the generated meshes (4 vertices per quad), both copies of paired pieces included, grimoire included:

| Level | When | Quads | Vertices | Without grimoire |
|---|---|---|---|---|
| NEAR (`valk_*`) | closest wearer(s), inside 16 blocks | **663** | 2,652 | 645 |
| MID (`_mid`) | up to 32 blocks, or a crowd | **397** | 1,588 | 379 |
| FAR (`_lo`) | up to 64 blocks | **198** | 792 | 186 |
| beyond 64 blocks | | 0 | 0 | |

Against the **1200 mesh-quad frame ceiling of the Water BC plan**: one NEAR wearer alone is 55 % of it, so the armour cannot simply share the pool with the water effects at full detail. Proposal: the armour has its own admission, nearest wearer first: each wearer gets the best level (NEAR, else MID, else FAR) whose quads still fit under a running cap of 1200, a wearer that fits at no level is not drawn;
worst case **1200 quads (4,800 vertices) per frame whatever the number of wearers** (the usual crowd: 1 NEAR 663 + 1 MID 397 = 1,060, then FARs of 198 only while they fit: none; the next one is not drawn, or, if the owner
wants everyone visible, FAR gets a separate small pool of 6 x 198 = 1,188). If the armour has to live inside the same 1200 as the Water BC effects (which would leave a lone NEAR wearer almost no room
for a Roar), the FAR / MID levels would be used almost always: that is a decision for the owner (separate pool of 1200, or a shared 1200 with the armour at MID by default). Draw calls: 3 buffer switches per wearer.
Per-frame CPU: wing vertex deformation only (728 vertices NEAR, 400 MID, 160 FAR); no per-frame allocation (static arrays, static pose slots as in `ModeArmorWindV2`). Network: 0 new packets. Server: 0 work. Extend
`PerfSourceScanTest` only if a new pattern appears (none expected: no entity spawn, no scan, no packet).

## 8. NEW / REPLACED / KEPT (rule 5)

* **NEW:** the whole player-fitted 3D Valkyrie armour (9 pieces x 3 levels), the generator, the preview tool, the previews, this doc.
* **REPLACEMENT candidates (decided at wiring, not now):** the classic `ModeArmorLayer.valkyrie` overlay and, in the Spirit Dive, the `water:*` Valkyrie pieces would be swapped for it when the setting is `V2`.
* **KEPT:** everything that exists (the classic overlay, the geo pieces, the water BC wing mesh `bc_wing_r/l` of the other builder, all old previews). The other builder's files were not touched.

## 9. Caveats and decisions for the owner

1. The still is the base Valkyrie Dress, whose wings are glass blades; the **Saint Valkyrie Dress** has big feathered bird wings and a halo. This build follows the still. A Saint variant (feather wings, halo ring, gold
   sash) would be a second wing mesh on the same hinge, about +60 quads per wing; do you want it as a form of the same layer?
2. Wing look: faceted crystal (as built) or a softer draped veil like the anime (more quads in the root, vertex alpha gradient)? The veil would cost about +30 quads per wing.
3. Budget pool (section 7): separate 1200 for the armour, or shared with the Water effects?
4. First-person arms (section 6).
5. The stand-in figure is not Noelle; the real skin, hair and face come from the player; the twin-tail "horns" of the still are hair and are not part of the armour.
6. Never seen in a game: scale against real skins (slim arms, 3 px), z-fighting of the plate shell against the skin / other armour, translucent sorting of the wings against water and particles are all untested.
