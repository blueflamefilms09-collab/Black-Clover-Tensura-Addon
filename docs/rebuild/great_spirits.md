# Great Spirits (1.99.0): Sylph, Undine, Salamander and the Spirit Dive, made to look like the canon

Owner request: "Make the Black Clover Great Spirits and the Spirit Dive look right." Canon text and two reference pictures (Sylph and Undine) were sent with it.

## 1. The canon this follows

The Four Great Spirits are the physical manifestations of wind, fire, water and earth, made of natural mana; they choose mages of exceptional potential (Spirit Magic).

| Spirit | Canon | Host(s) |
|---|---|---|
| Sylph (wind) | a TINY winged fairy, jealous and possessive, tornadoes and condensed wind beams. Picture: blonde hair swept up in a messy updo with wisps, a pale lime-green short strapless dress with a flared layered skirt, barefoot, translucent iridescent wings with a honeycomb (hexagon cell) pattern shimmering green-blue-pink, hands on the hips, an annoyed / pouty face, green eyes | Yuno |
| Salamander (fire) | a MASSIVE dragon, the highest pure destructive power | Fana, Fuegoleon |
| Undine (water) | a GIANT, elegant ADULT MERMAID, a pact that passes from generation to generation of the Heart Kingdom queens. Picture: extremely long wavy flowing pale-blue hair that fills the frame, fin-like ears (webbed, ribbed, blue), pale blue eyes, pale grey-blue skin, a serious calm look, bare shoulders | Lolopechka, for a time Noelle |
| Earth spirit | exists; name, look and host were never shown: the mod keeps it generic (the stone giant "Gnome") | none shown |

Spirit Dive (Spirit Assimilation): the mage merges the spirit into the body; a big boost to power, speed and senses; the user passively absorbs ambient mana; the body is altered and cloaked in dense elemental mana. Yuno (Sylph): the first dive transforms only the LEFT half of the body (one wind wing, a half crown, a halo-like ring round the wrist); the Full Spirit Dive later has two wings, a full crown, a symmetrical wind aura. Noelle (Undine): the SAINT VALKYRIE DRESS, her usual water armour plus feathered wing-like parts and a halo. Fuegoleon (Salamander): arms cloaked in intense fire mana with draconic scales, claws and fiery wings.

## 2. The spirits (client.ElementalSpiritRenderer, tools/gen_*.py)

The entity (`entity/ElementalSpiritLordEntity`), the slot ids (`book/SpiritSlots`) and the renderer registration are unchanged: the old summon keeps working. The renderer now draws the canon models; `client/SpiritLooks.CANON` (one constant) brings the 0.102 models back.

| Spirit | Verdict of the review | What was done |
|---|---|---|
| Sylph | did NOT match (lime-green hair, a leaf dress, a wide human body) | REPLACED by `spirit_sylph_fairy` + `spirit_sylph_fairy_wings`: 78 cubes, face painted at 4 texels per model pixel (green eyes with a catch-light, annoyed brows, a pouting mouth, blush), a blonde messy updo (bun, five fanned locks, a tail curling out of it, six loose wisps that drift), pale lime strapless bodice, a flared skirt of THREE layered flounces (eight hinged panels each), bare feet with toes, pointed ears, hands on the hips (the pose is the bones' own rotation). Wings: four plates, each a honeycomb of hexagon cells with thin borders, a thin-film colour that runs green - cyan - blue - violet - pink across the wing and differs per cell, a glow map on the cell walls; the renderer tints that glow with a hue that slowly wanders (`SpiritLooks.filmArgb`). Authored at 4 times its size (27 px) and drawn at 0.22 (`SpiritLooks.SYLPH_SCALE`): about 0.42 of a block. Hit box 0.35 x 0.45 (was 0.6 x 1.4). Clips: idle (hover, wisps and skirt drifting), move (leans into the flight, skirt and hair stream back), cast (one arm points: the condensed wind beam), hit, annoyed (an angry hop with a stomp and a head shake; the renderer plays it every 9 s while she is idle) |
| Undine | only half matched (pale blue hair and fin ears, but a wave skirt and a small body) | REPLACED by `spirit_undine_mermaid` + `spirit_undine_mermaid_water`: a giant adult mermaid, 127 cubes and 98 bones. Upper body upright, pale grey-blue skin, a calm face (long pale blue eyes with a catch-light, level thin brows, a closed mouth), fin ears (a plate of five ribbed spines with a translucent membrane, swept back and out), bare shoulders, a bodice of two pearl scallop shells and a band of water, slender arms with webbed fingers and a small fin along each forearm. Seven scaled tail segments in an S (back, then down) ending in a notched two-lobed fluke that hangs facing front and back so it shows; hip fins and dorsal fins. HAIR: nine long wavy locks (five to nine segments each, wide overlapping plates) fall from the crown, the middle ones stop at the shoulder blades so the tail curls out through the middle, the outer ones fall long and sweep outward so the hair fills the frame; two front locks frame the shells; every segment waves with a lag along its length. Water layer: two veils of water from the shoulders, a ring of foam beads and eight diamond droplets circling. About 3.2 blocks from the crown of the hair to the bottom (`UNDINE_SCALE` 1). Hit box 1.3 x 3.3 (was 0.8 x 2.0). Clips: idle (a slow float), move (a swimming stroke, the hair streams back), cast (both arms rise, the tail snaps), hit |
| Salamander | matches the canon (a winged fire dragon) but is thin in detail next to the new Sylph and Undine and not massive | the body model is KEPT; ADDED `spirit_salamander_blaze` (89 cubes, the same skeleton read from the body's geo file and the same clips): a mane of flames down the neck and over the head, five flame horns on the crown, flames along the trailing edge of both wing membranes and burning struts, a heavy row of flames down the back, a plume that grows along the tail, glowing claws and drifting embers, drawn additive over the body. The renderer draws the dragon 1.3 times bigger (`SALAMANDER_SCALE`): massive. Hit box 1.8 x 2.5 (was 1.4 x 1.9) |
| Earth spirit | matches (a hunched stone giant with a dark hollow face and glowing eyes; generic, as the canon never showed it) | left as it is, nothing changed |

## 3. The Spirit Dive (book/SpiritDiveForm, book/SpiritDivePieces, client/aura/SpiritDiveAura, tools/gen_spirit_dive_geo.py)

### 3.1 How it is wired

* The dive is drawn by its own render layer, the new `Aura.SPIRIT_DIVE` (one enum constant appended; one registry line). Its **style number** says element and stage: `book/SpiritDivePieces.style(element, stage)` = element (WIND 0, WATER 1, FIRE 2, EARTH 3) plus 4 times the stage (HALF 0, FULL 1, UPGRADE 2): the existing style byte of the aura lease, no new packet field.
* The server keeps the look alive with the same lease the armours use (the armour-policy design): `SpiritDiveForm` tells the clients "60 more ticks" at the start and every second the upkeep is paid, and "20 more ticks" (the same style) when the dive ends, so the pieces shatter out inside the 18 tick window of `EquipAnimation`. Death clears the look at once. New viewers get it from `PlayerAuras.onStartTracking` like every other aura.
* The stage is the one the armour policy already decides (`ArmourRules.diveScale`: the first three dives of an unmastered book are half dives, mastery makes any dive full). It is read from the stored stage of `ArmourState`.
* **Half to full by forging only the missing half.** If the book is mastered DURING a half dive, `SpiritDiveForm.second` upgrades it: the stats go to full (`ArmourState.putOn(..., 1f)`), the look's style becomes UPGRADE, and the painter keeps every piece of the half dive fully on (no new forge) while the missing pieces (for Wind the whole right half and the centre of the crown) forge in one after the other with `EquipAnimation`. A mage who starts a dive with three dives behind them starts it full.
* The four dives no longer start the old `blackclover.ModeArmor` overlays: `GrimoireBook.diveArmor()` returns null while `SpiritDivePieces.LOOK` is true (the overlays and their modes stay registered and still serve the older pages Spirit of Zephyr and Valkyrie Dress). Earth no longer starts the Gnome Plate layer through the policy (`SpiritDiveForm.withoutAura` hands `ArmourState.putOn` a copy of the policy entry without the form layer; the policy file is not edited).
* The painter (`SpiritDiveAura`) draws, in the order of the other armours: 1 a translucent mana skin over the parts of the body that transformed (the head is never covered: the face stays the player's own), 2 the pieces, each its own geo model anchored on the model part it was built for (the rig of the Djinn full equip, `WaterAuraRig`'s anchoring and slim-arm narrowing, copied so the shared rig is not edited, plus the "already on" of an upgrade), forged in and shattered out staggered by `EquipAnimation`, 3 their glow maps, 4 an additive haze, 5 a ring on the ground and the **ambient mana flowing in**: at most `SpiritDiveAura.MOTES` (10) glowing motes spiral from the air into the chest and `BITS` (8) element bits rise round the body (sprites of the existing particle set; none beyond 24 blocks; the wind half dive draws them on the left side only).

### 3.2 What each dive shows (pieces: `SpiritDivePieces`, models: `spiritdive_arm_*`)

| Dive | Half dive (first dives) | Full dive (later, or mastery) |
|---|---|---|
| Wind (Yuno, Sylph) | the LEFT half only: ONE honeycomb wind wing (an upper and a lower plate, the Sylph's wings at body size), HALF a crown (five leaf prongs and a thin band over the left of the head), a halo-like ring (a bead ring, an inner ring and three leaves, turning) round the LEFT wrist, a half mantle (a half breastplate, a shoulder fan of leaves), a sleeve and a greave of translucent wind mana on the left arm and leg; the mana skin covers only the left arm and leg | the same plus the whole right half: TWO wings, the FULL crown (both halves and the tall front leaf that joins them), a ring round each wrist, symmetrical; the mana skin covers the whole body |
| Water (Noelle, Undine) | the VALKYRIE armour: the old water armour set (`water_arm_*`, 16 pieces) tinted the deeper Valkyrie blue | the SAINT VALKYRIE DRESS: the same water armour (12 pieces) with its water wings, droplet halo and backdrop given up for feathered wings (primaries, secondaries and coverts of white feathers on a gold arm), a gold halo with a water-blue inner ring and feathers on it, feathered forearms (wing-like parts) and a gold sash with hanging feathers; white - gold - blue (the glow turns pale gold) |
| Fire (Fuegoleon, Salamander) | the arms cloaked in fire mana with draconic scales (scaled upper arm and forearm, a ridge of horn spikes down the outer arm, an elbow horn), long bone-coloured claws with hot tips, shoulder horns, and small fiery wings | adds the full pair of fiery wings (feathers of fire and flame sheets on a bone arm), draconic greaves with knee horns and toe claws, and a scaled chest with a burning heart and collar horns |
| Earth (the earth spirit, generic) | mossy plates of living stone on the chest, boulder pauldrons, vambraces, stone fists; the mana skin covers body and arms | adds greaves, a brow crown of amber crystals, a back slab with spikes and six rocks floating round the body |
| Light, Briar | KEPT as they are (no Light spirit dive exists in the mod, the Briar armour painter is still an empty stub); recorded in `armour_policy.md` and left for a later round | |

Numbers (preview tool, one frame at age 50, includes the mana skin and the extras): Wind half 3536 vertices / full 7616; Water Valkyrie 6080 / Saint 8912; Fire half 5552 / full 8960; Earth half 2384 / full 4688. The tool's soft budget note is 6000 per layer: the full dives are above it, in line with the other full armours (the Djinn set, the Flame armour); every piece flagged `detail` is skipped beyond 24 blocks and the whole layer beyond 64.

## 4. Files

New:

* `tools/great_spirits_lib.py` (kit: function materials, signed-distance shapes, hexagon grid, thin-film colours), `tools/gen_sylph_fairy.py`, `tools/gen_undine_mermaid.py`, `tools/gen_salamander_blaze.py`, `tools/gen_spirit_dive_geo.py`, `tools/geo_assets_check.py` (the GeoAssetsTest rules as a script)
* geo / animation / texture / glow files: `spirit_sylph_fairy(_wings)`, `spirit_undine_mermaid(_water)`, `spirit_salamander_blaze`, 44 `spiritdive_arm_*`, `textures/aura/spirit_{wind,water,fire,earth}_{shell,haze}(_glow)`
* `client/SpiritLooks` (pure numbers), `client/GreatSpiritModels` (the draw calls, shared by the game and the preview), `book/SpiritDivePieces` (pure: stage to pieces), `client/aura/SpiritDiveAura` (painter), `book/SpiritLooksSelfTests`
* tests: `SpiritDivePiecesTest` (17), `SpiritLooksTest` (6); previews: `tools/render_preview/scenes.d/great_spirits.json`, `great_spirits_dive.json`, `examples/GreatSpiritsProp.java`; `docs/previews/spirits/*.png`; lang fragment `tools/lang/great_spirits.json`

Edited (additions): `aura/Aura` (+ `SPIRIT_DIVE` at the end), `client/aura/AuraRegistry` (+ one line), `NUSMP` (+ one line: the self-test), `book/SpiritDiveForm` (the look, the upgrade, `withoutAura`), `book/GrimoireBook.diveArmor()` (one line), `client/ElementalSpiritRenderer` (the canon draw calls), `entity/ElementalSpiritLordEntity` (the hit box numbers of three kinds), `tools/render_preview/preview.py` (`realFiles` per scene), `Preview.java` and `GeoPreview.java` (geo files are read from disk for prop and aura scenes too).

## 5. NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| Sylph model (`spirit_sylph`, `spirit_sylph_wings`) | KEPT on disk, REPLACED in the renderer | the canon tiny fairy `spirit_sylph_fairy(_wings)`; `SpiritLooks.CANON = false` draws the old one again |
| Undine model (`spirit_undine`, `spirit_undine_water`) | KEPT on disk, REPLACED in the renderer | the giant mermaid `spirit_undine_mermaid(_water)` |
| Salamander body model | KEPT | drawn 1.3 times bigger |
| Salamander blaze, hit boxes of Sylph / Undine / Salamander | NEW (the hit boxes: changed numbers) | `spirit_salamander_blaze`; 0.35 x 0.45, 1.3 x 3.3, 1.8 x 2.5 |
| Earth giant (`spirit_gnome`) | KEPT, untouched | generic, as the canon never showed it |
| Entity, slot ids, renderer registration, calling / dismissing, abilities | KEPT | unchanged |
| Spirit Dive look of Wind, Water, Fire | REPLACED (the owner asked for the correction) | `Aura.SPIRIT_DIVE` instead of the ModeArmor overlays `WIND_SPIRIT_DIVE`, `FIRE_SPIRIT_DIVE`, `VALKYRIE` (those overlays stay registered, kept for Spirit of Zephyr, Valkyrie Dress and the constant `SpiritDivePieces.LOOK`) |
| Spirit Dive look of Earth | REPLACED | its own stone layer instead of the generic Gnome Plate armour layer (the Gnome Plate armour page is unchanged) |
| Half stage / Yuno's left half, Saint Valkyrie Dress, fire dive arms and claws, earth dive | NEW | `SpiritDivePieces`, the 44 pieces |
| Half to full upgrade by forging the missing half | NEW | `SpiritDiveForm.upgrade`, `Stage.UPGRADE` |
| Passive ambient mana flowing into the body (the look of it) | NEW | the mote stream of the painter (the intake itself was built in the armour-policy round) |
| `ArmourPolicy`, `ArmourState`, `ArmourRules`, the policy JSON | KEPT, not edited | read only; `ArmourState.putOn` is called with a copy of the entry |
| Light spirit dive, Briar armour painter | KEPT as they are | not built |
| The older Wind Spirit Lord mob (`entity/WindSpiritLordEntity`, admin summon, drawn by the kit probe with `spirit_sylph`) | KEPT, untouched | a different entity from the pact Sylph |

## 6. Checks that were run

* Type-check against the API stubs: 0 real errors in the new and edited files (the only errors are the known stub artefacts "is abstract; cannot be instantiated" and "does not override deserializeNBT" in the untouched parts of `ElementalSpiritLordEntity`); a deliberate wrong call fails as it should.
* JUnit in the shim: `SpiritDivePiecesTest` 17, `SpiritLooksTest` 6, `ArmourPolicyTest` 19 (it greps `SpiritDiveForm` for hunger code): all pass. `GeoAssetsTest` and `EquipAnimationTest` need Minecraft classes and cannot run in the shim: `tools/geo_assets_check.py` applies the GeoAssetsTest rules to the 49 new models (every bone linked, uvs inside the texture, the texture and the glow map at the declared size, every animated bone exists): 0 problems; `tools/geo_check.py` OK on all of them.
* Self-test `magic.spirit_looks` (registered, runs in the CI server smoke test): the files exist, the canon sizes (Sylph under 0.6, Undine at least 3.0, Salamander at least 2.2, the earth giant at least 2.5 blocks), and with a stand-in player the first Wind dive is the half look, a dive with three dives behind it the full look, ending keeps the look for the shatter, the Earth dive starts no Gnome Plate layer.
* Previews with the real render classes (`python3 tools/render_preview/preview.py great_sylph_scale ... dive_wind_upgrade`): 21 scenes, 0 errors; `docs/previews/spirits/` holds Sylph next to a player at scale, close-ups, the annoyed hop and the cast pose, Undine (idle, face, next to a player, cast), Salamander, the old Sylph and Undine for comparison, and every dive stage including the upgrade strip (the left half stays, the right half forges in) and the shatter strip.

## 7. What could not be verified

* Nothing was run in a game. All pictures come from the headless render preview, which uses the real painter and geo classes against stubs; it does not draw vanilla's lighting, fog, particles or the player's own skin and animations exactly, and it knows no first-person view.
* The face of Sylph and of Undine, the hair of Undine and the shape of the fin ears were judged in the preview only; the pictures the owner sent were described to this round, not available as files, so the match to them is by the description.
* The sizes (0.22 scale, hit boxes 0.35 x 0.45 / 1.3 x 3.3 / 1.8 x 2.5) are design values. The tall Undine and the wide Salamander hit boxes may not fit through narrow corridors (the follow goal teleports the spirit when it is more than 28 blocks away). Entity culling uses the hit box, so Undine's tail, which reaches about a block behind her, could pop out at the very edge of the screen.
* The in-dive upgrade fires only when the book becomes mastered during a dive (a rare moment); the usual way to a full dive is the next dive after the third. The stats at the upgrade are re-applied by calling `ArmourState.putOn` again, which shows the form message once more before the upgrade message.
* The look of a dive that lasts minutes (a lease renewed every second) is unseen; the piece clips are looped and none reads the total length.
* The ambient intake (1.2 percent of the pool a second) and every stat of the dive are the armour-policy round's numbers and untouched here.
* `Aura.SPIRIT_DIVE` is one more constant at the end of the enum (ordinal 80, the payload carries a byte, so up to 127 fit); another builder who appends to the same enum will conflict on that line only.
