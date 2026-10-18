# Art of the Seven Star weapons (builder `hart2`, release 1.62.0)

Why this page exists: the seven Seven Star Weapons of Raphtalia's world (Vassal Hammer, Claw, Axe, Whip, Staff, Gauntlet, Projectile) must each be a real 3D model in the hand, built
the way the owner builds the Genesis Demon-Slayer (the owner's rule 7 of the repository rules), with the old-style 16 x 16 sprite kept as the inventory icon and a glowing gem, the series' central jewel, in the
weapon's colour. This page says what was built, why each file exists, the numbers, where the sources are, how to regenerate everything, what the lead has to merge, and, last,
what could NOT be verified. Nothing here ran in the game.

Everything is made by two Python files, deterministic and idempotent (a second run writes identical bytes; `--selfcheck` proves it):

* `tools/heroweapon/art_hart2.py`: one builder function per weapon (mesh and skin), one sprite function per weapon, the table `SPECS`, the class fragment writer and the command line.
* `tools/heroweapon/art_hart2_kit.py`: what the seven share (see section 2).

## 1. NEW, REPLACEMENT, KEPT

| | |
|---|---|
| NEW | Seven weapons, none of which existed: `vassal_hammer`, `vassal_claw`, `vassal_axe`, `vassal_whip`, `vassal_staff`, `vassal_gauntlet`, `vassal_projectile`. Per weapon six files (mesh, skin, glow skin, sprite, in-hand model, icon model), 42 files in all, plus the class fragment and the two scripts and this page. One look each; no curse look (the Seven Star weapons have none in the contract). |
| REPLACEMENT | None. Nothing existing is removed or edited. `mesh_lib.py`, `wave1_models.py`, `display_presets.py` and the rest of the owner's tools are imported as they are; the family's own painted materials (`hero_*`) are added to mesh_lib's material table at run time inside the kit, not by editing mesh_lib. |
| KEPT | Every shared file untouched (`weapon_classes.json`, `WeaponRenderer`, lang, `WeaponEngravings`, the display preset manifest and so on). The old 16 x 16 sprite style is kept as the inventory icon (`<id>_icon.json` is a `handheld` model of it). The Time Magic VFX files are not touched. |

## 2. The files and why each exists

| file | why |
|---|---|
| `src/main/resources/assets/nusmp/weapon_meshes/<id>.txt` | The 3D model drawn in the hand by `WeaponRenderer`: quads with smoothed per-vertex normals (`qn` lines), the `place` line (where the butt sits on the item's diagonal and how big the mesh is), a `glow` line (white tint, slow pulse 0.45 to 1.0), an explicit `grip` line (the share of the length at which the hand holds it) and `# part` lines naming each part. |
| `textures/entity/weapon/<id>.png` | The painted skin, 512 x 512 (UVs are in the renderer's 128 space, so the file may be any multiple). |
| `textures/entity/weapon/<id>_glow.png` | The second skin, drawn additively on top: the gem, the engraved lines, edge hairlines; this is what shines. Dark where nothing glows. |
| `textures/item/<id>.png` | The 16 x 16 sprite (inventory icon), built by hand-written pixel code from primitives (no downscaled render), butt low left and tip up right, one dark outline pixel, the gem the most saturated spot. |
| `models/item/<id>.json` | The in-hand model: `builtin/entity` with the display transforms. Third person and the other views are vanilla's `handheld` set (the same as `brooch_pin`); first person carries the oversized-weapon preset of `tools/relics/display_presets.py` for the long ones (hammer, axe, staff, whip) so the tip stays in view. |
| `models/item/<id>_icon.json` | The flat inventory model: parent `minecraft:item/handheld`, `layer0` the sprite. |
| `tools/heroweapon/classes_hart2.json` | The size class of each id for the lead to merge into the class table (section 8). No new class is needed. |
| `tools/heroweapon/art_hart2.py` | Builds the seven weapons (section 3 onward). |
| `tools/heroweapon/art_hart2_kit.py` | The shared kit: `hx` (hex colour to rgb), `register_materials` (the family's painted materials), the Big Dipper data and `dipper()`, geometry helpers (`cabochon`, `star_bezel` with seven sides, `rivet`, `rivet_ring`, `torus`, `tube`, `spike`, `bezier`), painters (`paint_gem`, `paint_cut_gem`, `engrave`, `dipper_engraving`, `stars_on_loft`, `braid`, `enamel_rect`, `etched_bit`), the 16 x 16 `Sprite` canvas and `save_sprite`, the item model writer, `place_diagonal` and `finish` (writes mesh, skins and models), `audit` (the repo's size / orientation audit against the class fragment merged in memory), and the previews (`preview`, `held_render`, `first_person_render`). One kit instead of seven copies so the weapons read as siblings. |

## 3. The family look (design)

The wiki summaries give no picture of any of the seven (they describe wielders, lineage, forms and power-up methods only; section 9), so the shapes are design. The colours of each weapon
(metal, wood, trim and above all the gem and its glow) are the ones the logic builders wrote in their look pages (`docs/heroweapon/weapon_hammer.md`, `weapon_claw.md`, `weapon_axe.md`,
`weapon_whip.md` of hva; `weapon_staff.md`, `weapon_gauntlet.md`, `weapon_projectile.md` of hvb), so the colour a weapon glows in the hand is the colour of its skill effects. What the
family shares is mine:

* **Seven Star = the Big Dipper.** The weapons are called "Seven Star", and the Big Dipper is the seven stars; so every gem sits in a **seven-sided bezel** and the Big Dipper is engraved
  in thin glowing lines (with a dot at each star) wherever the weapon has a plate to carry it (hammer faces, axe blade, claw plate, gauntlet back plate, whip guard disc) or as seven
  stars (staff shaft) or seven dots in a ring (axe poll, hammer rear face, gauntlet side plate). The star positions are the real ones (right ascension and declination of Dubhe, Merak,
  Phecda, Megrez, Alioth, Mizar, Alkaid, listed in the kit) projected flat. The reading "Seven Star = Big Dipper" is from memory and my own interpretation of the name, not a canon claim.
* **The gem is on BOTH faces.** A hand shows its -z face to onlookers and its +z face to the first-person camera (section 7), so every gem and engraving is built on both faces. The
  gauntlet's big gem is on its back plate (+x face), the face the first-person camera sees, because the series puts the jewel on the back of the hand.
* **Mesh -x is forward, the tip is up-forward on the sprite's diagonal** (`docs/weapon_scale_orientation.md`); every weapon states an explicit `grip` fraction.
* **Method** (the owner's, `tools/relics/mesh_lib.py`): real cross sections lofted, swept and turned; small raised parts as their own named parts; smoothed normals; painted
  materials plus an additive glow skin. Budget per mesh: 8 to 14 named parts, at most 2800 quads (body plus glow lines), one 512 px skin. Every proportion in the script is a named
  constant with a comment saying where it comes from.

## 4. The seven weapons

All lengths in "units" are mesh units (about a centimetre of the weapon as drawn); the final size is the apparent length in blocks (the class range decides it).

### 4.1 Vassal Hammer (`vassal_hammer`, class `hammer`)
* Look (design on the hva page): a big two-handed war hammer, a square block head on a straight haft: forged dark iron blocks `#4A4F5C` with lighter face plates
  `#7A8090`, a taller hub between the two blocks framed by brass rims, brass `#B8893A`; walnut haft `#4B3220`, black-brown leather grip `#2E2018` wound with brass wire, a flat brass
  pommel cap; a brass medallion on each cheek ringed by seven rivets, carrying the amber cabochon (`#FF9A2E`, glow `#FF9A2E`) in a seven-sided bezel; the Big Dipper on the striking face
  (mesh -x), on the rear face (the first-person face) and, fainter, round the gems. The front block is longer than the rear (the peen) so the mass sits forward.
* Parts (13): `haft`, `grip_wrap`, `haft_fittings`, `pommel_cap`, `neck_collar`, `head_blocks`, `head_hub`, `head_rim_bands`, `face_plates`, `medallions`, `gem_bezels`, `gems`, `rivets`.
* Numbers: 186 units long; head 85 units along the swing (front block 25, hub 22, rear block 17, plates 5.5 each end), 54 tall and 49 thick: 0.65 blocks wide in the hand against the
  class minimum 0.5. Apparent 1.62 blocks (class 1.3 to 1.8).

### 4.2 Vassal Axe (`vassal_axe`, class `battleaxe`)
* Look: a broad single-bit battle axe, a woodcutter's silhouette made noble: a long slightly curved edge (mesh -x) dropping into a pronounced beard, a thick squared poll with a short
  back spike, on a straight pale ash haft `#9C7A4E` with a black-brown wrap; blade steel `#9AA3B2` with a bright bevel `#D5DCE8` and a dark green etched channel `#3F8F66` that glows
  emerald; poll and spike `#6E7686`; dark iron fittings `#2F3238` with gold trim `#B8893A`; ONE emerald cabochon `#35E07C` (glow) in a seven-sided bezel on each cheek of the hub (the eye
  of the axe), the Big Dipper in tiny glowing dots with the faintest lines on the blade, seven dots round the back spike on the poll. A small spear-point finial on the top of the haft.
* Parts (11): `haft`, `grip_wrap`, `haft_fittings`, `head_hub`, `head_blade`, `poll_block`, `back_spike`, `hub_trim`, `gem_bezels`, `gems`, `haft_cap`.
* Numbers: 205 units long; the bit 112 tall at the horns and 66 from the haft axis to the edge. Apparent 1.82 blocks (class 1.6 to 2.0); head 1.03 blocks against the minimum 0.75.

### 4.3 Vassal Staff (`vassal_staff`, class `staff`)
* Look: a tall slender mage's staff, all the weight in the head: lacquered dark indigo shaft `#262A5A` with seven small stars `#A9B5FF` down the shaft below the grip, silver `#D3DAEA` bands
  and a pointed foot, pale gold `#E7D79A` trim, a dark violet leather grip `#3A2C3F`; a silver cup flaring into four small curved prongs that hold an OPEN ring (about 47 units across, 0.17 of
  the length); a seven-sided faceted double pyramid gem, violet-blue `#7B8CFF`, floating in the middle of the ring held by nothing; a small upturned pale gold crescent on top of the ring; a
  thread of the gem colour inlaid along the inside of the ring.
* Parts (11): `shaft`, `grip_wrap`, `shaft_bands`, `gold_trim`, `foot`, `collar`, `prongs`, `head_ring`, `head_ring_inner`, `crescent`, `gem`.
* Numbers: 280 units long; ring radius 21, tube 2.5; gem radius 11. Apparent 2.20 blocks (class 2.0 to 2.6).
* The seven stars are painted on the skin (lighter than the lacquer, soft on the glow skin), not modelled as raised geometry (that would spend parts and quads on something the skin shows as well).

### 4.4 Vassal Claw (`vassal_claw`, class `shortsword`)
* Look (my decision between "pair-claw" and "glove with long blades": a glove, the mesh is the claws and the cuff): a hand-claw whose knuckle bar carries three long curved needle blades
  that fan slightly outward and sweep forward and up (lengths 92, 86 and 66 units, curving toward -x like a katana's), a thumb spur of 44 and two small hooks behind the blades. Black-brown
  leather bracer `#3A2A22` with three steel lames `#8E96A8` and gold buckles `#B8893A`, a steel hand plate with three finger ridges on each face, a gold collar and knuckle bar with a socket under
  each blade, blades `#C9D2E3` with a dark root `#5A6070` and a cold hairline of glow `#8FB4FF` along the edges. ONE amber-orange cabochon `#FFA030` in a small seven-sided bezel on the back
  plate (on both faces), the Big Dipper engraved out from it.
* Parts (10): `cuff`, `cuff_lining`, `cuff_lames`, `buckles`, `back_plate`, `hand_collar`, `knuckle_bar`, `blades`, `gem_bezels`, `gems`.
* Numbers: the heaviest mesh in quads (2320, 566 of them glow lines). Apparent 1.00 blocks (class 1.0 to 1.3, exactly its minimum).

### 4.5 Vassal Gauntlet (`vassal_gauntlet`, class `handitem`)
* Look: ONE right-hand armoured gauntlet, a closed fist: a flared vambrace of brushed steel `#9AA3B2` (two lames, a rolled tiger-orange `#E58A2B` edge, a band of white fur `#F2EEE6` inside the
  mouth for the white-tiger holder, a leather lining and wrist strap `#5B3A2A` with gold buckles), a boxy plated hand whose back is the +x face (the face the first-person camera sees):
  a sunk panel in the darker recess colour `#4A5160` with an orange hairline, the big amber-orange oval cabochon `#FFA43A` (bright core `#FFD27A`) in a gold seven-sided bezel, and the
  Big Dipper's tail engraved out from under it; four finger plates folded into the palm (two bands each, painted as seams), the thumb plate across them, a row of four orange knuckle studs;
  the outer side (-z) is a flat plate with a second smaller gem and a heptagram.
* Parts (12): `vambrace`, `vambrace_lining`, `vambrace_lames`, `cuff_trim`, `cuff_fur`, `wrist_strap`, `buckles`, `back_plate`, `fingers`, `knuckle_studs`, `gem_bezels`, `gems`.
* Numbers: apparent 0.90 blocks, the top of the held-item class (0.5 to 0.9; items are held to a minimum largest dimension only).

### 4.6 Vassal Whip (`vassal_whip`, class `coilwhip`)
* Look: a coiled whip: a braided dark leather handle `#4A3426` with gold `#C9A24A` rings and a gold wire spiral, a gold guard disc with the Big Dipper engraved on it, a gold pommel disc with
  a violet cabochon `#9B5CFF` (glow) on each face in a seven-sided bezel; two loops of the lash gathered at the hand (turned 32 degrees out of the profile plane so the first-person camera
  sees them as a ring too), then the free lash in one S-curve (one bend and a half) tapering to a weighted tip: a gold cage of four curved bars round a faceted violet gem, with four
  tiny steel spikes on top (the morning-star hint of its Gigantic Star form). A hairline of violet thread inlaid along the lash, brighter toward the tip.
* Parts (10): `grip_braid`, `handle_rings`, `guard_disc`, `pommel`, `gem_bezels`, `gems`, `coil_and_lash`, `tip_cage`, `tip_spikes`, `tip_gem`. The whole cord (handle top, coil, lash) is ONE
  sweep of about 70 points, which is why the weapon costs few quads (1686).
* Numbers: apparent 1.62 blocks. That is inside the coiled-whip class (1.1 to 1.7) and also inside the extended `whip` class (1.6 to 2.4), so the audit passes whichever of the two a merge uses.

### 4.7 Vassal Projectile (`vassal_projectile`, class `dagger`)
* Look: ONE throwing knife of crystal (the wiki's base form is a semi-transparent knife that later becomes other throwing weapons): a leaf-shaped double-edged blade with a centre ridge,
  aqua `#A8EDE6` in the middle, near white `#E6FFFB` at the edges, darker veins `#6BCFC4` along the ridge like a crack in ice and a faint sheen on the glow skin (the "semi-transparent" look
  is painted, because the cutout render type cannot blend); thin bead-like edge glints along both edges; a short dark teal `#24545A` oval guard; a two-tone teal cord grip; a RING pommel (an open
  ring a finger could go through) with a round emerald-teal cabochon `#38F0C0` (core `#B8FFEA`) boss on each face, and a short cord loop hanging from it.
* Parts (8): `blade`, `edge_glint`, `guard`, `grip`, `pommel_ring`, `gem_bezels`, `gem`, `cord_loop`.
* Numbers: apparent 0.62 blocks (class 0.45 to 0.70); the finest skin (5.35 px per unit).

## 5. The numbers

All from the run of `python3 -B tools/heroweapon/art_hart2.py` (the audit is `tools/relics/audit_weapon_scale.py` with the fragment merged in memory; every row has no flags and status ok).

| id | class (range, blocks) | apparent | head | parts | quads (body + glow) | skin px / unit | `grip` line | mesh length (blocks) |
|---|---|---|---|---|---|---|---|---|
| `vassal_hammer` | hammer (1.30 - 1.80, head >= 0.5) | 1.62 | 0.65 | 13 | 2181 (1914 + 267) | 1.27 | 0.271 | 1.906 |
| `vassal_axe` | battleaxe (1.60 - 2.00, head >= 0.75) | 1.82 | 1.03 | 11 | 1250 (960 + 290) | 1.96 | 0.282 | 2.141 |
| `vassal_staff` | staff (2.00 - 2.60) | 2.20 | 0.37 | 11 | 1698 (1483 + 215) | 2.17 | 0.401 | 2.588 |
| `vassal_claw` | shortsword (1.00 - 1.30) | 1.00 | 0.42 | 10 | 2320 (1754 + 566) | 2.14 | 0.119 | 1.18 |
| `vassal_gauntlet` | handitem (0.50 - 0.90, item class) | 0.90 | 0.29 | 12 | 1862 (1594 + 268) | 2.47 | 0.274 | 1.06 |
| `vassal_whip` | coilwhip (1.10 - 1.70) | 1.62 | 0.29 | 10 | 1686 (1370 + 316) | 1.63 | 0.171 | 1.906 |
| `vassal_projectile` | dagger (0.45 - 0.70) | 0.62 | 0.09 | 8 | 1138 (778 + 360) | 5.35 | 0.366 | 0.73 |

"Apparent" is the mesh length times the third-person scale 0.85 (what a viewer sees); "mesh length" is before that scale. The hammer has the lowest texel density (1.27 px per unit: thirteen parts
with large flat faces share one 512 px skin), so its skin will look the softest in close view.

The seven weapons weigh about 4.2 MB in the repository (meshes 2.85 MB of text, skins about 1.3 MB).

## 6. Where the build departs from the look pages of the logic builders, and why

1. **Seven-sided bezels** on every gem (the hammer and axe pages say eight-sided): the family mark of section 3.
2. **Sizes follow the class table, not the pages.** The pages quote ranges of the older table in `docs/weapon_scale_orientation.md`; `weapon_classes.json`, which the audit, the runtime net and the CI
   test use, says staff 2.0 - 2.6 (page 1.85), dagger 0.45 - 0.70 (page 0.50), shortsword 1.0 - 1.3. So the staff is 2.20, the knife 0.62.
3. **Claw 1.00 and gauntlet 0.90** (pages 0.75 and 0.55): a bracer that small could not close round a player's 0.25 wide arm, and the owner has complained that weapons were too small. The claw is
   mapped to `shortsword` (the nearest class with a hand weapon's blade length), not `handitem`.
4. **Staff ring 0.17 of the length** (page 0.14): the owner asks for bold heads. The seven stars are painted, not raised.
5. **Hammer gems on both cheeks** (page: one gem on the head's side plate) and the Big Dipper also on the rear face (page: the striking face).
6. **The whip lash** is part of one sweep with the coil (page: a pose for the lash); the whip is 1.62 (page: aim 1.5) so it is also valid in the extended `whip` class.

## 7. How the weapons are placed in the hand

* Mesh y runs along the weapon (grip low), mesh -x is the forward / edge side, the tip lies on the item sprite's 45 degree diagonal (the `place` line is written with explicit butt and tip pixels
  of the sprite, `place_diagonal`). The audit's orientation detector (tip down, head toward the player, mirrored, grip at the far end) reports nothing for any of the seven.
* Right hand, third person: mesh +x is up, -x down / forward, mesh +y forward, mesh -z faces onlookers, mesh +z faces the player's body. First person: mesh +x comes toward the camera and mesh +z is
  to the camera's left, the weapon sits at the right edge of the screen. The gem and engravings are on both faces for that reason, and the first-person face (+x) of the hammer, axe, gauntlet
  carries its own mark.
* Every mesh states `grip <fraction>` explicitly, taken from the real middle of the grip (the automatic value could exceed 0.5, which the runtime net reads as "held by the far end").
* The model JSON's first-person transform is `tools/relics/display_presets.py`'s rule; `python3 tools/relics/display_presets.py --check` exits 0 with the seven in place and its dry run
  prints the same numbers the files carry (hammer 0.558, axe 0.533, staff 0.382, whip 0.492; the short three keep vanilla's 0.68). The manifest `display_presets_manifest.json` is not edited
  by me (a shared file): the next `--apply` of the lead lists the four long ones in it.

## 8. For the lead (integration)

1. **Class table.** Merge `weapons` of `tools/heroweapon/classes_hart2.json` into the `weapons` map of `tools/relics/weapon_classes.json` (existing entries there are `{"class": "..."}` objects; the
   loaders `weapon_classes.py`, `audit_weapon_scale.py` and `tools/bettercombat/inventory.py` accept a bare string too, the game copy is flat strings), then run
   `python tools/relics/audit_weapon_scale.py --write` (copies the table to `assets/nusmp/weapon_classes.json`) and `--check`. The `classes` map of the fragment is empty: no new class.
   Until it is merged, `audit_weapon_scale.py --check` and `tools/relics/test_weapon_scale.py::test_every_mesh_has_a_reviewed_class` list the seven ids as unreviewed (that is the expected state).
2. **Better Combat tests.** Once the seven ids are in the class table, `WeaponAttributesTest` will ask for them: `manifestIsInSyncWithTheClassTable` (every id in the game's class table must be in
   `tools/bettercombat/manifest.json` or its `excluded`) and `everyMeleeWeaponHasAFile` (a file in `data/nusmp/weapon_attributes/` for every id whose class is not `handitem`, `crossbow`, `bow`,
   `shield` or `item`). The classes I chose (hammer, battleaxe, staff, coilwhip, shortsword, dagger) all have profiles in `tools/bettercombat/profiles.py`, so `python tools/bettercombat/inventory.py`
   and `gen_attributes.py` will generate them; if the Seven Star weapons are driven by their own attack logic and need no Better Combat file, exclude the ids in the manifest instead. The gauntlet
   is `handitem` (a non-melee class for Better Combat); if a fist or claw swing is wanted, the lead adds classes (`gauntlet`, `claw`) and profiles, with ranges that include 0.90 and 1.00.
3. **Nothing from art goes into** lang, `WeaponEngravings`, quests or the creative tab. The in-hand model files are `builtin/entity`: they draw only if the item is registered the way the other
   hand-held 3D weapons are (the renderer hook is the core's).
4. **Previews:** `tools/item_preview/preview_weapons.py` parses only plain `q` quad lines and shows nothing for `qn` meshes (180 of the repo's 234 meshes are `qn`, so this is not specific to these
   seven). Use `HART2_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart2.py <id> --preview` (mesh views, held view next to Steve, sprite) or `--views` (textured third-person and first-person software views).

## 9. Sources (canon research)

`WebFetch` of the fandom wiki fails from this machine, so canon comes from search summaries (14 searches, `allowed_domains: shield-hero.fandom.com` for most). What they gave and what they did not:

* Gave (summaries of the wiki pages): the roster (eight Vassal Weapons of Raphtalia's world: Hammer and Claw are vassal to the Legendary Shield, Axe and Gauntlet to the Sword, Staff and Projectile to the
  Spear, Whip and Carriage to the Bow); wielders (Hammer: Empress Natalia, then Takt, Raphtalia in the web novel; Claw: Filolia, Rhubaldt; Axe: Takt, Mald, Midori in the web novel; Whip: Holn Anthreya,
  Takt, Rino; Staff: Aultcray, Ethnobalt shown holding an ornate staff; Gauntlet: Fohl Fayon, who is a white tiger demi-human, chosen after his sister Atla's death; Projectile: Rishia Ivyred, whose first
  form was a semi-transparent, wavering shadow of the true weapon; the knife shape is the logic builder's reading of its Small Projectile form); power-up methods (Hammer: Weapon Form Synthesis,
  Claw: Skill Mastery, Axe: muscle / body strengthening, Gauntlet: Skill Power-up Points, Staff: Magic Power-up Points; the Whip's special ability is controlling monsters); forms (Claw: Cursed, Rope, Dragon Head; Whip: Chain, Mace, Rope Whip, Morning Star, Dragon Hourglass Sand Whip; Staff: Fenrir Rod, Siderite
  Staff; Axe: Great Quake, Guillotine skills). The four Legendary Weapons' centre gems are green (Shield), blue (Sword), red (Spear) and yellow (Bow): I first built the family in the gem colour of
  each weapon's master and then dropped that for the logic builders' own palettes (a Vassal weapon is not the master's colour in any picture I found, and the skill effects reuse the page colours).
* Did NOT give: the look of any of the seven. The hammer, claw, axe, whip, staff, gauntlet and projectile shapes, proportions, materials and colours are DESIGN (the logic builders' look pages, with my
  family marks); "Seven Star = Big Dipper" and the star coordinates are from memory; the sizes come from the repository's class table, not from the series.

URLs (all search results of the summaries above; none was fetched):
`https://shield-hero.fandom.com/wiki/Vassal_Weapons`, `.../Vassal_Weapons/Web_Novel`, `.../Vassal_Hammer`, `.../Vassal_Claw`, `.../Vassal_Axe`, `.../Vassal_Whip`, `.../Vassal_Staff`,
`.../Vassal_Gauntlet`, `.../Vassal_Projectile`, `.../Seven_Star_Carriage`, `.../Heroes`, `.../Heroes_(Web_Novel)`, `.../Legendary_Weapons/Power-up_Methods`, `.../Legendary_Shield_(Web_Novel)`,
`.../Legendary_Sword_(Web_Novel)`, `.../Legendary_Spear_(Web_Novel)`, `.../Legendary_Bow_(Web_Novel)`, `.../Legendary_Jewel`, `.../Rhubaldt_Warbaldt`, `.../Filolia`, `.../Rishia_Ivyred`,
`.../Takt_Alsaholn_Faubrey/Powers_and_Abilities` (all under `https://shield-hero.fandom.com/wiki/`), and for the Gauntlet holder `https://www.tvmaze.com/characters/1370379/the-rising-of-the-shield-hero-fohl-fayon`
and `https://anibase.net/en/character/mz7B9/Fohl`.

## 10. Commands

```
python3 -B tools/heroweapon/art_hart2.py                        # all seven; no ids = all; writes the files of section 2 and prints the audit row of each
python3 -B tools/heroweapon/art_hart2.py vassal_axe             # one weapon
HART2_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart2.py --preview   # also the sheet of each: four mesh views, held view next to Steve, the sprite enlarged
HART2_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart2.py vassal_axe --views   # textured third-person and first-person software views
python3 -B tools/heroweapon/art_hart2.py --selfcheck            # builds each twice and compares the hashes of every output
```

A full run takes under a minute on four shared cores (about a minute and a half with `--preview`). The repository's own checks that apply: `python3 tools/relics/audit_weapon_scale.py <ids>`,
`python3 tools/relics/test_weapon_scale.py` (35 tests; with the fragment merged in memory 34 pass and the one failure is the game-copy-equals-tool-table test that `--write` fixes, without the merge
the failure is "every mesh has a reviewed class" for the seven), `python3 tools/relics/test_display_presets.py` (all pass), `python3 tools/relics/display_presets.py --check` (exit 0).

## 11. What is NOT verified (honest list)

* **Nothing ran in the game.** No Java was written or compiled by me; the CI (`WeaponMeshesTest`, `HeroAssetsTest`, `WeaponClassesTest`) was not run. `HeroAssetsTest`'s 42 files were checked by `ls`;
  `WeaponMeshesTest`'s rule (the runtime normaliser changes nothing on a shipped mesh) was checked through its Python twins (`weapon_orientation.py`, `audit_weapon_scale.py`), which agree with it
  on every other mesh of the repository but are not the Java test.
* **Every picture is a software approximation.** The mesh views are mesh_lib's rasteriser, the held view and the first-person view are my own small rasterisers (affine texture interpolation, so
  long quads swim slightly; simple lighting; the real item lighting, the additive glow pass, the glow pulse and the vanilla arm pose and swing are not reproduced). The first-person framing is the
  `display_presets.py` maths with a 70 degree field of view, not a screenshot.
* **How the skins look at in-game distance** (mip-mapping, the gem glow against a daylit world) is unseen; the hammer's skin is the least dense (1.27 px per unit).
* **The class of the claw and the gauntlet** is my choice (`shortsword`, `handitem`); a new `claw` / `gauntlet` class would be equally valid (section 8, item 2).
* **Canon:** none of the seven shapes comes from a picture; the Big Dipper reading of the name, the gauntlet's tiger trim and the star positions are from memory and the logic builders' pages.
* **Sprites:** drawn and looked at enlarged (`vassal_all_sprites.png`), not seen in an inventory slot at 1x with the real item model.
* **Quad counts** are the written counts (`# part` lines and the audit); the real draw cost is not measured. All seven are under the budget of 2800.
