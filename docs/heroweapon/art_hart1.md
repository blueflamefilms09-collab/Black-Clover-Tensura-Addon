# The art of the four Holy Weapons (hart1)

Why this file exists: the owner's rule 7 says every weapon is a real 3D model in hand, keeps a 16 x 16 sprite as its inventory icon and gets a glow that fits it, with the
Genesis Demon-Slayer as the standard. The four Holy (Cardinal) Weapons of The Rising of the Shield Hero are the face of the 1.62.0 job, so each has TWO looks: the **base
look** (noble, plain, strong: clean steel, gold fittings and one bright colour, the gem) and the **curse look** (the same weapon corrupted by its sin: the same grip and
footprint, a more monstrous silhouette, a hotter glow). The look of each Seven Star and Vassal weapon is in the other art builders' docs.

New or replacement (CLAUDE.md rule 5): everything here is **NEW**. Nothing like it existed (the only shield, bow and sword models are other relics, unrelated); nothing is
replaced or removed; the old meshes, sprites and tools are only read.

## 1. What it writes, and how to run it

One deterministic script, `tools/heroweapon/art_hart1.py` (idempotent: a second run writes byte-identical files; `--check` proves it):

    python3 -B tools/heroweapon/art_hart1.py [id ...]            # no ids = all eight looks; also writes the class fragment and runs the self check
    python3 -B tools/heroweapon/art_hart1.py --check             # builds twice and compares the SHA-256 of all 44 files (the 43 asset files and the class fragment)
    HART1_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart1.py legendary_bow     # also renders the mesh preview of each look into <dir>

It uses the owner's model method (`tools/relics/mesh_lib.py`: real cross-sections, named parts, per-vertex smoothed normals, painted materials plus a second additive glow
skin, one UV atlas), the sprite helpers of `tools/relics/wave1_icons.py`, the draw-state rig of `tools/relics/ranged_meshes.py` and the display rule of
`tools/relics/display_presets.py`. It edits no shared file.

| file (under `src/main/resources/assets/nusmp/`) | what |
|---|---|
| `weapon_meshes/<id>.txt`, `<id>_curse.txt` | the eight meshes (`qn` lines, `orient manual`, `grip` where it is decidable) |
| `weapon_meshes/legendary_bow[_curse]_pull1..3.txt` | the draw states of both bows (the limbs flex, the string is a V, an arrow of light rides the nock); see 6 |
| `textures/entity/weapon/<id>.png`, `<id>_glow.png` (and the `_curse` pair) | the 512 px painted skin and the additive glow skin of every look |
| `textures/item/<id>.png` | the four 16 x 16 sprites (hand-built pixels, not a downscaled render) |
| `models/item/<id>.json`, `<id>_icon.json` | the in-hand model (`builtin/entity`, display transforms) and the flat inventory model |
| `models/item/legendary_shield_blocking.json` | the raised-guard model the shield's `blocking` override points to (a copy of the Magebane Shield's); see 6 |
| `tools/heroweapon/classes_hart1.json` | id -> size class of `tools/relics/weapon_classes.json`, for the lead to merge (no new class was needed) |

`HeroAssetsTest` (the nine files per Holy Weapon) is satisfied for all four: checked by `ls`, and by the script's own self check.

## 2. Common look and method

* **Palette.** The four weapons share their metals (a blue-grey polished steel, a warm gold for the fittings) and differ only in the gem: green (shield), blue (sword), red
  (spear), yellow (bow). The curse looks share black iron with a purple cast and differ in the sin's fire: red-orange (Wrath), amber (Greed and Gluttony), magenta (Lust),
  violet (Pride).
* **The gem** is a glowing cabochon on the glow skin in the weapon's colour: a domed stone on a gold bezel in the shield face, a lens through the sword's guard and the
  bow's grip (it shows on both faces, so it is seen from either side), an orb in a cage of claws in the spear's socket. Its body skin is a deep saturated colour (the glow
  layer is ADDED on top in game, a bright body would burn out to white); the light is in the glow skin.
* **Real proportions as named constants** (every one in the script has a comment saying where it came from): a 62 cm round shield, a 150 cm sword (blade 118 cm), a 330 cm
  spear (head 53 cm), a 160 cm bow. `place` then scales each to the class table's apparent length in the hand (section 5).
* **Method.** Silhouette first, then sections (lofts along a real section such as a bevelled hexagon or a diamond, surfaces of revolution for the shield plate and the gems,
  sweeps for limbs, wings and horns), the small raised parts as their own named Parts, normals smoothed by angle per part, UVs projected on the dominant axis (rings as
  unrolled strips, with the long strips cut into pieces so the 512 px skin packs at 3 to 5 px per cm), painted materials, a glow skin. Every look was rendered and looked at
  (section 9) and corrected.
* **What is canon, what is memory, what is design.** The wiki summaries (search summaries only; the wiki pages themselves could not be opened from the build machine) give
  the **gem colours** (green shield, blue sword, red spear, yellow bow), the **sins** of the four Curse Series (Wrath, Greed and Gluttony, Lust, Pride) and, for Itsuki's
  Pride form, "an angel-like armour with demonic parts". The **shapes of the base weapons** (a round shield with a boss, a straight sword with a winged guard, a leaf-headed
  spear, a recurve bow) are from memory of the anime and are plain on purpose. Everything else is design: the compass ribs of the shield (a nod to the Four CARDINAL Heroes),
  the twelve clock ticks on the spear's socket (a nod to the time-reversal lore: its best-known form is the Dragon Minute Hand Spear), the angel feathers of the bow, and every
  detail of the curse looks (the Wrath shield's face, the maw guard, the thorned vine, the crown crest) which no summary describes.

## 3. The four base looks

| | Legendary Shield (green) | Legendary Sword (blue) | Legendary Spear (red) | Legendary Bow (yellow) |
|---|---|---|---|---|
| silhouette | a round plate, 62 cm, a shallow dish round a domed boss, a rolled gold rim | a slender double-edged blade with a long leaf point, a gold winged crossguard | a 3.3 m silver shaft, a gold socket with a cage of four claws and two swept wings, a bay-leaf head | a white recurve longbow, 160 cm tip to tip, swept-back gold feathers at the grip |
| parts | plate, rim, ribs, field_ring, studs, boss, gem_setting, gem, arm_strap, grip_bar (10) | blade, guard, gem_setting, gem, pommel, grip, grip_rings, blade_collar (8) | butt_cap, shaft, grip_wrap, bands, socket, claws, wings, gem, spearhead, head_collar (10) | limb_upper, limb_lower, limb_inlay, limb_tips, riser, grip_wrap, wings, gem_setting, gem, string (+ arrow_* in the draw states) |
| detail | four cardinal ribs and a ring bead, 12 rim rivets, an engraved double border, a compass ring with four glowing green diamonds, a thin green inlay round the boss, the back has an arm strap and a hand grip bar | hexagonal blade with ground edges and an engraved fuller whose core glows blue, a lens gem in a bezel on both faces, a blue-wrapped grip with three gold rings, a round pommel | a recessed dial with twelve ticks round the socket, a wrapped red hold at 0.33 of the length, seven gold bands, a diamond-section head whose midrib glows red | gold inlay lines (faintly glowing) along both limbs, the lens gem above a leather-wrapped grip, a string of light (glows), gold tip caps |
| glow | gem, four diamonds, the boss ring | gem, the fuller | orb, midrib | gem, string, inlay |
| size (class) | 1.06 blocks across (shield 0.90 - 1.20) | 1.70 blocks (longsword 1.40 - 1.80) | 3.30 blocks (spear 2.80 - 3.60) | 1.50 blocks (bow 1.30 - 1.60) |

## 4. The four curse looks (same grip, same plate / blade / shaft / limbs, a more monstrous silhouette)

* **Shield of Wrath** (id `legendary_shield_curse`, 11 parts): black iron, the gold turned dark. Twelve curved horns round the rim (long and short alternating, swept forward,
  glowing red at their roots), a maw of twelve teeth round a red gem (the stone is an eye in a gaping mouth), burning cracks: nine radial cracks painted in step on the face,
  plus a fine web at the rim and the boss, hot cores. The arm strap and the grip bar are exactly the base look's so the hand fits.
* **Sword of Greed and Gluttony** (`legendary_sword_curse`, 8 parts): a black blade with a saw of hooked teeth on both edges (a shark's: each tooth rises abruptly and slopes
  back) and gilded ground edges (a gold line that follows the teeth), whose fuller is a maw (two rows of pale teeth round a glowing amber throat), amber veins; the winged guard becomes two hooked mandibles with teeth on their inner edge
  and two lower tusks, a row of teeth clamps the blade's root, the pommel carries six spikes, the grip is black wound with gold wire, and the gem is an amber eye with a slit pupil.
* **Spear of Lust** (`legendary_spear_curse`, 13 parts): a black leaf head with a glowing magenta midrib, three pairs of backward-hooked thorn barbs along its edges, a
  thorned vine winding up the shaft, two chains of six links hanging from the wing tips, a hot magenta orb, a black shaft with magenta veins and a spiral ley line.
* **Bow of Pride** (`legendary_bow_curse`, 14 parts): black limbs with violet glowing inlay and veins, the angel feathers turned to three long black blades per root, a crown
  of four blades round each riser end (the limb is the fifth point), long horns that carry the tips on, a thicker violet string, a violet gem. Pride's summary ("an angel-like
  armour with demonic parts") is why the angel look of the base bow is the thing that is corrupted.

## 5. Placement, size and the hand

* **Sizes** follow the class table (docs/weapon_scale_orientation.md 10.3), in the upper half of each range because these are the heroes' weapons: the apparent length is the
  mesh length (blocks) times the third-person display scale of the model json. The table of the script (`APPARENT`, `THIRD`): shield 1.06 (1.01 x 1.05), sword 1.70 (2.00 x
  0.85), spear 3.30 (3.88 x 0.85), bow 1.50 (1.76 x 0.85). A curse look reuses the base look's `place` line, so the plate, blade, shaft and limbs stay exactly as large and
  the extras (spikes, horns) stand out beyond: the apparent length of the Wrath shield is 1.24 (it is a spiked shield), of the Bow of Pride 1.60.
* **The hand holds the grip.** `place_on_sprite` grows a mesh about one point of the sprite's axis; `diag_place` solves that point so the middle of the mesh's real grip lands
  where the hand takes a sprite weapon (the axis point nearest the vanilla hilt (0.25, 0.25), the same anchor `display_presets.py` uses): sword at 0.115 of its length, spear at
  0.335 (class grip 0.33). The shield and the bow stand upright with their grip (the shield's centre, the bow's riser) at the item cube's centre, which is where the hand is.
* **Display transforms.** The sword and the spear take the standard diagonal ones (`bloodbound_katana`) with the first-person scale of `display_presets.py` (0.515 and 0.265).
  The **shield** takes the Magebane Shield's (held on the forearm, face out; third-person scale 1.05) plus its raised "blocking" model. The **bow** has its own, solved with the
  hand chain of `tools/relics/weapon_sheet.py` (third person `[75, 90, 0]`, scale 0.85: the bow is held upright with its belly forward and its face to the side, where the wave
  23 bows lie flat like planks; first person `[0, 115, -20]`, scale 0.6, grip at the vanilla anchor). The shield's and the bow's json carry `"nusmp_display_preset": "manual"`
  so `display_presets.py` never rewrites them.
* **`orient manual` and the `grip` directive.** All eight meshes carry `orient manual` (the orientation was checked on the audit sheets; the runtime net must not second-guess
  a polearm's head, a bow or a shield, docs/weapon_scale_orientation.md). The sword and the spear carry `grip <fraction>` (0.08, 0.22: the hand end is low), derived from part
  names by mesh_lib. A trap found on the way: mesh_lib reads the word "spike" in a part name as "the business end", and a shield with a part named `spikes` got `grip 1.0`,
  which `grip_end_flag` obeys for ANY class and would have turned the shield upside down; the horns of the Wrath shield are therefore named `horns`, and the self check refuses
  any directive above 0.5.

## 6. What the lead must wire (the contract keeps these in the core's files)

1. **The class fragment**: merge `tools/heroweapon/classes_hart1.json` into the `weapons` map of `tools/relics/weapon_classes.json`, then
   `python tools/relics/audit_weapon_scale.py --write` for the game copy. The curse ids are listed on purpose (the id of a curse mesh is not a state suffix the table strips).
   With the fragment merged in memory the repo's `tools/relics/test_weapon_scale.py` passes all 35 tests except the one that compares the game copy with the tool table (which is
   exactly what the merge and `--write` fix).
2. **The curse look**: the core's `WeaponRenderer` hook chooses the mesh id `<id>_curse` when the stack's `nusmp:hero_look` is `curse`; the skin of that id is its own
   (`textures/entity/weapon/<id>_curse.png`, `_glow`), nothing else is needed. The curse meshes have no model json of their own (the item's json supplies the display).
3. **The bow's draw stages** (optional, made): `ItemStateVariants.stageOf` only knows relic bows (`RelicBearer`); add before its `return RangedStage.REST;`

       if (item instanceof com.newuniverse.nusmp.heroweapon.HeroWeaponItem hw && hw.kind() == com.newuniverse.nusmp.heroweapon.HeroKind.BOW) {
           boolean using = holder != null && holder.isUsingItem() && holder.getUseItem() == stack;
           float pull = using ? RangedStage.bowPull(stack.getUseDuration(holder), holder.getUseItemRemainingTicks()) : 0f;
           return RangedStage.stage(RangedStage.Kind.BOW, using, false, pull);
       }

   and the stages `legendary_bow_pull1..3` and `legendary_bow_curse_pull1..3` are drawn (the nocked arrow, an arrow of light that glows in the bow's colour, appears in the draw
   states only: the Holy Bow makes its own arrows). Until then the bow simply shows its rest mesh, which is correct, only without the draw animation. The Hero Bow item must
   start a use (as `HeroStyle.DRAW` implies) for `isUsingItem` to be true. Imports needed there: `HeroWeaponItem`, `HeroKind` (package `com.newuniverse.nusmp.heroweapon`).
4. **The shield's raised model**: `models/item/legendary_shield.json` has the Magebane Shield's `blocking` override; register vanilla's predicate for the shield next to
   the Magebane Shield's registration in `WeaponRenderer.init` (imports: `HeroItems`, `HeroKind`):

       modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> ItemProperties.register(HeroItems.get(HeroKind.SHIELD).get(),
               ResourceLocation.withDefaultNamespace("blocking"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F)));

   Without it the override never matches (an unknown predicate is never true) and the shield simply keeps its normal first-person pose.
5. `tools/relics/display_presets.py --apply` and `audit_weapon_scale.py` may be re-run over the whole tree: they leave these eight alone (checked: no change, no flag).

## 7. Budget (the art contract: 8 to 14 named parts, at most 2800 quad lines per mesh, one 512 px skin per look)

| look | parts | quad lines (body + glow copies) | skin px per cm |
|---|---|---|---|
| shield | 10 | 2664 (2520 + 144) | 3.1 |
| shield curse | 11 | 2707 (2072 + 635) | 2.9 |
| sword | 8 | 1204 | 3.8 |
| sword curse | 8 | 1833 (1446 + 387) | 3.5 |
| spear | 10 | 1006 | 5.1 |
| spear curse | 13 | 2117 | 3.0 |
| bow (rest / drawn) | 10 / 13 | 1446 / 1650 | 2.9 |
| bow curse (rest / drawn) | 11 / 14 | 2408 / 2612 | 2.7 |

A "glow copy" is how mesh_lib draws the glow skin: every body quad whose UV rectangle holds glow pixels is written a second time on layer 2; the curse looks spend their
budget on those (cracks, veins), so the glow patterns are kept to where they read.

## 8. Checks that were run

* The script's own self check after every run: every file of the list exists, quad lines <= 2800, 8 to 14 named parts, `grip` absent or <= 0.5, `orient manual`, finite numbers,
  UVs inside the 128 space, unit normals, no quad samples a transparent skin pixel (no see-through crack), the length of the base meshes equals the designed length.
* Determinism: `--check`: 44 files identical on a second run; building the four curse looks alone gives the same bytes as building all eight.
* `python tools/relics/audit_weapon_scale.py` logic on the eight ids with their classes merged in memory: every apparent length inside its class range (the Wrath shield 1.24 and
  the Bow of Pride 1.60 at the top), no size / orientation / grip flag. `tools/relics/test_weapon_scale.py` with the fragment merged: 34 of 35 pass, the 35th is the game-copy
  comparison the merge fixes.
* The first-person camera bounds of `display_presets.py` (every corner of every mesh in front of the near plane, the tip in view) hold for all of them.

## 9. Pictures that were looked at (`$S/previews/hart1/`)

`hart1_base_looks_three_quarter.png`, `hart1_curse_looks_three_quarter.png` (and the `_profile` pair): the eight looks; `hart1_held_view_01.png`: the audit sheet (the weapon in
the sprite plane and next to Steve, third person); `hart1_first_person.png`: a first-person look from the display chain; `hart1_bow_draw_states.png`,
`hart1_bow_curse_draw_states.png`: rest and the three draw stages; `hart1_icons_x14.png`: the four sprites at 14 x; `hart1_sword_curse_zoom.png`: the Sword of Greed and
Gluttony's blade, maw and guard close up. The audit sheet and the first-person picture were made before the last two touches that changed skins only (the gilded edges of
the Greed blade, the arrow of light); the geometry they show is unchanged.

## 10. What is NOT verified (nothing here was run in the game)

* The game never loaded these files. The previews are the mesh_lib software rasteriser (flat-lit, back faces culled like the game, the glow added), the audit sheet's hand
  chain and a first-person reproduction of the `display_presets.py` maths; the real lighting, the pulse of the glow and the look in the game's hand are unchecked.
* The bow's third-person pose and first-person pose, the shield's raised model and the curse hook are only as good as that simulation of the vanilla hand chain (which the
  earlier teams checked against screenshots for the handheld case only). Look at the bow in the hand first.
* The shapes of the base weapons are from memory of the anime, not from a picture; the curse looks are design. Say so to the owner and expect a "make the guard like this".
* The two things that need a Java line (the bow's stages, the shield's `blocking` predicate) are not wired by this builder.
* The sprites are 16 px miniatures drawn by hand code; they are legible in the 14 x sheet but unseen in an inventory slot.

## 11. The weapon builders' look pages compared (hsa: shield and sword; hsb: spear and bow)

The weapon builders wrote the look they want in `docs/heroweapon/weapon_shield.md`, `weapon_sword.md` (worktree hsa-wt) and `weapon_spear.md`, `weapon_bow.md` (worktree hsb-wt).
Their pages appeared while my meshes were being built; I compared them with the finished looks at the end. The owner's text in the brief is the specification, and both pages
agree with it; where a page asks for a detail I did not build, the difference is listed here and the lead decides. Two things were taken over after reading them: the Holy Bow's
nocked arrow is an arrow of LIGHT (all three arrow parts are bright and glow in the bow's colour, "the bow makes its own arrows"), and the blade edges of the Sword of Greed and
Gluttony are gilded (a gold line along each ground edge that follows the teeth). Nothing in the game code reads a part name (only the draw rig of the bow reads `limb_*`,
`string`, `arrow_*` while the meshes are built), so the pages' part lists differ from mine in names only.

| | already the same as the page | differs (my version stays unless the lead asks otherwise) |
|---|---|---|
| Shield | a round plate of about one block, brushed steel with a radial grain, a rolled rim, a boss that holds the emerald cabochon, rim rivets, a grip bar across the back and a forearm strap; curse: twelve curved horns leaning like a saw blade, a black iron face with cracks that run out from the boss and branch, red-orange glow with hot centres, a spiked collar round the stone | the rim is gold (page: bright steel with a teal line); four cardinal ribs and a raised ring bead (page: eight low ribs and an engraved ring); the Wrath ribs stay plain bars (page: flame-shaped fins round the collar); the Wrath stone is a cabochon in a maw of twelve teeth (page: a slit-pupil eye); no chain on the strap; no rim studs in the Wrath look (page: small spikes) |
| Sword | a slim double-edged blade with a blue fuller that glows, a ricasso collar, a guard with the lens gem through both faces, a wrapped grip, a pommel; curse: a black blade with gold edges, the guard as jaws with fangs, an amber slit-pupil eye, a black grip with gold thread | the guard is gold with winged quillons that curve up (page: silver with navy inlay, drooping quillons); a navy grip with three gold rings (page: five wrap rings); a round gold pommel ball (page: a flattened silver sphere); apparent length 1.70 blocks (page: 1.5, the class allows 1.4 to 1.8); six spikes on the Greed pommel (page: three); the Greed fuller is a maw of two rows of teeth and the edges are saw-toothed (page: a row of small fangs along the fuller) |
| Spear | 3.3 blocks, the class grip 0.33, a gold butt, socket and rings, a crimson wrap on the hold, a red gem, a bay-leaf head with a diamond section and a glowing ridge; curse: a black head with magenta glow and edges, barbs along the head, a thorned vine up the shaft, two chains, a magenta gem | the shaft is polished silver (page: ivory ash wood); the gem is an orb in a cage of four claws above the socket (page: set into the socket's front face); two swept wings under the head and no feather (page: one small white feather on a cord); seven gold bands (page: three rings); a plain butt spike (page: a ball finial, a thorned rosebud in the Lust look); no forked tip and no crack line across the gem |
| Bow | 1.5 to 1.6 blocks, upright, white limbs with gold inlay, gold tip caps, a wrapped grip, gold wings at the riser, a yellow gem above the grip, a string of light, three draw states with an arrow of light; curse: black limbs with violet veins and inlay, hooked horns at the tips, a crown crest, larger black wings, a thicker violet string, a violet gem, a black grip with violet stitch | the wings are swept feather blades (page: small leaf fins); no `arrow_rest` part; the crest is four blades round each riser end (page: a five-spike fan from the top of the riser); the Pride stone has no ring of tiny spikes; **orientation**: the page says the limbs curve forward (mesh -x), but the wave 23 bows (checked in the meshes: `divine_mercy`, `the_dragons_spirit`, `embrasure`) and these bows have the belly (the grip) at +x and the tips and string toward -x; the display transform holds the belly forward in the hand |

## 12. Sources

Canon from search summaries (the pages could not be fetched): https://shield-hero.fandom.com/wiki/Legendary_Shield_(Web_Novel), .../Legendary_Sword_(Web_Novel),
.../Legendary_Spear_(Web_Novel), .../Legendary_Bow_(Web_Novel) (the gem colours), https://shield-hero.fandom.com/wiki/Curse_Series and .../Curse_Series/Weapon_List (the sins and
the series' names), https://shield-hero.fandom.com/wiki/Legendary_Weapons, https://shield-hero.fandom.com/wiki/Itsuki_Kawasumi (the Pride form's angel-like armour).
