# Art of the Vassal Scythe, Katana, Fan, Carriage and Book (`tools/heroweapon/art_hart3.py`)

Job 1.62.0 (the weapons of The Rising of the Shield Hero), art builder `hart3`. This page is the WHY of the files the script writes, the numbers they were built
from, the sources, and an honest list of what could not be checked. The look each weapon was asked for is in the logic builders' pages
(`weapon_scythe.md`, `weapon_katana.md`, `weapon_fan.md`, `weapon_book.md` of the `hvc` worktree and `weapon_carriage.md` of `hvb`); section 7 says where this art
differs from them and why.

All five weapons are NEW (nothing in the mod looked like them): no existing model, texture or sprite was replaced or removed.

## 1. What is written, and how to run it

```
python3 -B tools/heroweapon/art_hart3.py                 # all five, in the order scythe, katana, fan, carriage, book (about 10 s)
python3 -B tools/heroweapon/art_hart3.py vassal_carriage # one of them
HART3_PREVIEW=<dir> python3 -B tools/heroweapon/art_hart3.py   # also renders the four-view preview of each written mesh into <dir>
python3 -B tools/heroweapon/test_art_hart3.py            # the self-check (HART3_DETERMINISM=1 also runs the script twice and compares every byte)
python3 -B tools/heroweapon/preview_hart3.py entity out.png [yaw,pitch ...] [--zoom CM --center HEIGHT,ALONG,ACROSS]
python3 -B tools/heroweapon/preview_hart3.py firstperson out.png vassal_book vassal_carriage ...
```

For each id `<id>` the script writes the six files `HeroAssetsTest` asks for, deterministically (a second run is byte for byte the same):

| file | what |
|---|---|
| `assets/nusmp/weapon_meshes/<id>.txt` | the 3D model (per-vertex smoothed normals, the part list as comments, the `grip` directive, the `glow` line) |
| `assets/nusmp/textures/entity/weapon/<id>.png` | the painted skin, 512 px |
| `assets/nusmp/textures/entity/weapon/<id>_glow.png` | the additive glow skin, 512 px |
| `assets/nusmp/textures/item/<id>.png` | the 16 x 16 sprite, the inventory icon (drawn by hand as a pixel map, never scaled down from a render) |
| `assets/nusmp/models/item/<id>.json` | the in-hand model (`builtin/entity`, the display transforms) |
| `assets/nusmp/models/item/<id>_icon.json` | the flat inventory model (`item/handheld`, or `item/generated` for the upright book and carriage) |

and `tools/heroweapon/classes_hart3.json` (the size class of each id, for the lead to merge into `tools/relics/weapon_classes.json`).

Files of the job: `art_hart3.py` (the five weapons, one function each, every proportion a named constant with a comment), `art_hart3_lib.py` (shared helpers: tapered
rods, tubes, tori, cut gems, a model-space decal painter, `ring_coords` that turns a lofted strip back into the faces of its section, `sweep_pieces` that builds one
sweep as several strips with shared end frames, `rebleed`, the display tables, the item model writer), `art_hart3_sprites.py` (the five pixel maps),
`test_art_hart3.py`, `preview_hart3.py`, `classes_hart3.json`, and this page. The owner's method is `tools/relics/mesh_lib.py`: real cross-sections (loft, sweep, lathe),
named parts, smoothed normals, one skin, a second additive glow skin, the gem on the glow skin in the weapon's colour.

## 2. The five weapons in numbers

Budget of the method: 8 to 14 named parts, at most 2800 quad lines with the glow layer counted. Lengths are the model's own centimetres; "apparent" is what a viewer sees
held in the hand (the mesh length times the third person display scale), which is what the class table bounds.

| id | class | parts | quad lines (body + glow) | skin px/cm | apparent in hand | class range |
|---|---|---|---|---|---|---|
| `vassal_scythe` | scythe | 14 | 1582 + 578 = 2160 | 1.75 | 2.35 | 2.0 to 2.6 (head at least 0.6: it is 0.93) |
| `vassal_katana` | katana | 10 | 1418 + 343 = 1761 | 6.30 | 1.66 | 1.4 to 1.8 |
| `vassal_fan` | handitem | 12 | 1828 + 601 = 2429 | 2.23 | 0.80 (the span 0.93) | 0.5 to 0.9 |
| `vassal_carriage` | handitem | 14 | 1685 + 162 = 1847 | 1.76 | 0.80 (1.0 block in the item cube) | 0.5 to 0.9 |
| `vassal_book` | handitem | 14 | 1342 + 695 = 2037 | 3.62 | 0.84 (cover 0.56, with pages and ribbon) | 0.5 to 0.9 |

`tools/relics/audit_weapon_scale.py` finds no flag on any of the five once the class fragment is merged. `tools/relics/test_weapon_scale.py` passes 35 of 35 with the
fragment merged (merged temporarily and reverted with git; without the merge only `test_every_mesh_has_a_reviewed_class` fails, by design, until the lead merges).
`tools/relics/test_display_presets.py` passes (19 of 19). The class `carriage` and `fan` that the logic pages mention as options are NOT needed: both fit `handitem`.

### 2.1 Vassal Scythe (L'Arc Berg's)

Target: a tall reaper's scythe. Canon from search summaries: its swing throws a spinning disc of energy, it can float a second scythe, Naofumi made a plume accessory
for it, and a prop listing describes a black scythe with a curved blade and a spiked disc at the foot of the handle. Design with intent (and the logic page's palette):
black lacquer shaft bowed like a real snath, six silver rings (one at each end of the two wraps and two between), a crimson leather wrap over the two hand zones, a steel blade with a darker spine band and a polished
edge, a bronze socket and a bronze medallion in which the amethyst is set THROUGH the plate so it shows on both faces, a steel butt spike under a spiked dark disc (the
listing's), a small plume of violet feathers under the socket.

Key constants: pole 180 cm of 240 (26 to 206), radius 2.0 to 1.75; blade root (0, 213), heel spike (13, 211), point (-88, 164) (88 cm forward and 49 cm below the
root); widest 25 cm at a quarter of the way; thickness 2.4 to 0.8; amethyst girdle radius 5.4 (about 0.06 block); halo ring radius 14; grip 0.30 (the class value).
Parts: `snath`, `grip_wraps`, `snath_rings`, `butt_cone`, `butt_disc`, `cuff`, `blade`, `blade_spine_band`, `blade_runes`, `heel_plate`, `gem_setting`, `gem`, `disc_halo`, `plume`.
Glow: the amethyst (strong), a violet line along the whole cutting edge (in the skin) and a raised glowing line beside it on each face, engraved feather strokes (soul wisps) on the flat,
a ring of light with ticks round the jewel (the spinning disc of the swing). Nothing on the shaft. The blade's feather engravings are drawn in model space.

### 2.2 Vassal Katana

Target: a real katana's proportions made noble. Blade 72 cm with a 3 cm bend toward the edge, hilt 27 cm, a guard of 4.3 cm radius. Dark iron five-petal sakura
guard with gold inlay and a petal-shaped opening cut through each lobe (alpha cut-out), a gold collar (habaki), two washers, an indigo diamond lacing over white ray
skin with two gold blossom menuki on each side, a gold fuchi and kashira. The jewel (sakura pink) is a pointed bicone set through the collar at the centre of the guard,
so it shows from the wielder's side in first person; a second, smaller pink drop closes the pommel. Parts: `blade`, `habaki`, `habaki_gem`, `tsuba`, `tsuba_inlay`,
`tsuka_wrap`, `guard_collar`, `menuki`, `pommel_cap`, `pommel_gem`. Glow: both stones (strong), a hair-thin pink line along the hardening line (hamon), a soft pink
ring round each petal opening, a pink rim line on the guard. The hamon is texture only (no part).

### 2.3 Vassal Fan (Glass')

Target: an opened gunsen. Canon: an iron war fan used in a dance that throws wind blades; the charm Naofumi made is a round jewel on a string hung from the handle;
Glass wears a jet-black kimono with silver embroidery (the logic page follows her: jet-black ribs, silver trim). Built: a half circle of 44 cm radius opened 170
degrees, a pleated pale silver-blue paper leaf (each gap folds 0.45 cm out of its plane, like a real folded fan), nine jet-black lacquered ribs between two sharpened
steel blades, a thin silver rim with 33 small steel serrations along the top arc, a steel pivot collar with the spirit-cyan jewel set through it (both faces), a short
handle wrapped in cord with three silver rings, and the charm as a red tassel: a red cord, a red lacquer bead, the small cyan jewel in a silver ring and red strands.
The leaf carries a double silver border and a plum branch in silver thread with five-petal white-pink blossoms and buds. Parts: `fan_leaf`, `fan_ribs`, `leaf_rim`,
`fan_guards`, `fan_teeth`, `pivot_plate`, `pivot_gem`, `handle`, `handle_rings`, `charm_cord`, `charm_bead_red`, `charm_bead`. Glow: both jewels (strong), a soft ring round
the collar, the plum branch (faint cyan), the rim (very faint).

### 2.4 Vassal Book (Ethnobalt's, first Kyo Ethnina's)

Target: a heavy hardback spellbook held closed and upright like a grimoire, 36 x 26 x 8.5 cm. Deep teal-blue leather, a gold border line and four gilded corner caps, four
raised gold bands on the spine, a quill-and-feather emblem under the jewel, the jewel (a BLUE sapphire, see section 7) in a gold eight-point star with a ring and eight
small prongs, a dark-brown leather clasp strap that hangs OPEN from the fore-edge with its gold buckle, a red ribbon from the foot, and three loose pages standing out of
the top, fanned and curling toward the viewer, whose lines of text glow. Parts: `cover_front`, `cover_back`, `pages`, `spine`, `spine_bands`, `corner_caps`, `cover_frame`,
`gem_mount`, `emblem`, `gem`, `clasp_strap`, `buckle`, `ribbon`, `loose_pages`. Glow: the sapphire (strong), a ring of sixteen rune ticks and four channels from the ring to
the corner caps (the way the book draws materials in), the star mount and the frame line (faint), the loose pages' text (soft cyan-white), the page block's edge (very faint warm).
The cover is drawn 0.78 block tall in the item cube; the pages and ribbon stand out beyond it, so the largest dimension is 1.17 blocks in the cube and 0.84 in the hand.

### 2.5 Vassal Carriage (Fitoria's) and the entity

The mesh is also the entity's model (`CarriageRenderer` of `hvb` draws it about 3.2 times larger and does not use the `place` line). So it is built clean and closed, in the
frame the logic page binds: +y is the nose, -x is up (the roof at low x, the wheels touch the ground at x = 0, the highest x), z is the width, symmetric about z = 0.
Measured on the written file (the self-check asserts every line of this table):

| proportion (L = ymax - ymin = 120.0 cm) | page | measured |
|---|---|---|
| width over the wheels | 0.60 L | 0.600 L |
| height to the top of the canopy | 0.58 L | 0.580 L (the roof jewel to 0.63 L, the plumes to 0.78 L) |
| wheel diameter | 0.34 L | 0.340 L |
| body (floor) length | 0.70 L | 0.700 L |
| pole and yoke ahead of the body | 0.30 L | 0.299 L |
| seat surface above the ground | 0.17 L | 0.17 L (the bench tops) |
| driver's bench ahead of the middle of the body | 0.17 L | 0.17 L (bench centre y = 62.4, body middle 42.0) |
| rear bench behind the middle, 0.14 L either side | 0.19 L, 0.14 L | 0.19 L (y = 19.2); the bench is 52 cm wide, so both seats are on it |

Parts, exactly the names the page asks for: `wheel_left`, `wheel_right`, `axle`, `chassis`, `cabin`, `canopy`, `posts`, `bench_front`, `bench_rear`, `pole`, `yoke`, `lantern`,
`gem`, `plumes`. Each wheel is its own part so a renderer can turn it: the axle line is (x = -20.4, y = 38.0) along z, the wheel centre planes are z = -32.7 and +32.7, the
wheel radius is 20.4 and every vertex of a wheel part is within 20.4 cm of that line (the hub caps reach 3.3 cm beyond the centre plane, to the +-36.0 of the width).
The part comments of the file are `# part <name> <first quad line> <count>`; the quad lines of both layers of a part lie inside its range.
Thinnest things (the page's floor is 1.2 % of L = 1.44 cm): felloe 3.8 x 5.0, spokes 2.0 to 1.6 x 2.8 to 2.2, posts 3.6, cabin wall 2.6, lantern bars 1.8, feathers 1.6 (they
taper to a point at the tip and edges, like a real feather); nothing hangs free or loose: the lantern is on a rigid bracket and the plumes stand on the canopy.
Built: two spoked wheels (a felloe swept round, eight flat spokes, a banded hub with a cap), an ivory D-plan floor with two rails under it, low curved walls rising to a
rounded dash, four twisted gold posts standing on the walls, a shallow domed canopy (royal blue outside with eight gold ribs round a rosette and a gold rim, deep blue
lining), two deep-blue buttoned benches (the rear one with a back for two), a draught pole rising to a gold yoke with two knobs and a ring, a lantern with the amber jewel
in a gold cage at the canopy's front, a second amber jewel on the roof (the series puts the weapon's jewel there), and two plumes of three white feathers with sky-blue tips.
Glow: both jewels (strong), a thin gold vine and a crest on the cabin's sides (soft), the wheel hubs (faint). Nothing else.

In the hand it stands upright on its wheels, nose to the right of the item cube (`place` angle 0), 1.0 block long. The third person display `[0, 90, 90]` was found with the
repo's own chain (`tools/relics/weapon_sheet.py`): item +x (the nose) goes forward and +y (the roof) goes up. First person: vanilla's `[0, -90, 25]` points an item's +x at the
camera and puts the item's centre on the hand at the bottom edge of the screen, so the carriage would show only its roof; it uses `[6, 108, 0]`, translation
`[-1.6, 6.4, -2.2]`, scale 0.7, which in the software copy of the chain (`preview_hart3.py firstperson`) shows the whole carriage from behind and a little from the left in the
lower right third of the picture. The left hand entry is the vanilla-style mirrored literal (y rotation negated, translation the same, the game flips the x).

## 3. How each file was made (the method's steps)

0. The target is stated in each weapon's comment block: what the series shows (search summaries), what the logic page asks for, what is design.
1. Every proportion is a NAMED constant with a comment (a real object, the class table or the logic page).
2. Long forms are lofts, sweeps and lathes through real cross-sections (a one-edged blade section with a flat-ground bevel, a rounded-rectangle wall swept along a path,
   a felloe swept round a circle); the sakura guard is a polar prism whose outline is r(theta); the star mount is another.
3. Each small raised part is its own named part, so the part list reads like the weapon.
4. Normals are smoothed by angle per part; UVs are projected on the dominant axis, or unrolled for round parts and for the strips that would waste the atlas
   (the cabin wall is built as three strips with shared end frames, so there is no hairline between them).
5. The skin is painted in MODEL coordinates (a decal draws a line or a petal at the point of the part where it belongs, whatever the UV layout is), then each group's
   one-pixel border is copied outward again (`rebleed`) because mesh_lib fills it with the unpainted base colour, which showed as a pale hairline along a seam.
6. Every render was looked at (four views per weapon, close-ups, the skins, the held third person views, the first person copy, the sprites at slot size), and the ugly
   things fixed: a comb-like crest on the scythe, a lumpy guard outline, a blown-out gem (the stone's body is a dark saturated colour and its LIGHT is on the glow skin,
   only a little toward white), a gap at the fan's pivot, a too-flat canopy, floating plumes and posts, a hairline between two strips of the cabin wall.

## 4. Checks and how they were made

* `test_art_hart3.py` (13 tests, all pass; one more with `HART3_DETERMINISM=1`): the six files exist and are well formed; every quad is finite with unit normals and
  UVs inside the 128 space; 8 to 14 parts and at most 2800 quads; a `glow` header exists when a glow layer does; the hand end is the low end (`grip` at most 0.5); the carriage's part names and
  the entity proportions of section 2.5; no AI or model names in any file of the job; the class fragment is valid; the size and orientation audit finds nothing; a second run writes the same bytes.
* A mutation check: the carriage's half width was changed from 0.30 L to 0.36 L in a temporary copy of the script, which made `test_the_carriage_meets_the_entity_proportions`
  fail ("width over the wheels 0.72 != 0.6"); the real script then restored the files with the same hashes as before.
* `tools/relics/audit_weapon_scale.py` (the contact sheets of the item plane and of the third person hold beside Steve), `test_weapon_scale.py`, `test_display_presets.py`.
* The old `tools/item_preview/preview_weapons.py` reads only `q` lines, not the `qn` lines (per-vertex normals) that the method writes, so it shows only the sprite for
  these meshes; `tools/relics/preview_mesh.py` (which reads both) was used instead.

## 5. Sources (what was read, and how)

The fandom wiki pages themselves could not be opened from the build machine (`getaddrinfo ENOTFOUND shield-hero.fandom.com`); every canon fact below was read in the summary a
web search returned for the page. Colours and shapes marked "design" are not from any picture.

* The Vassal Book holds a blue gem that can absorb materials: https://shield-hero.fandom.com/wiki/Vassal_Book (search summary: "the book has a blue gem that can absorb materials").
  Holders: https://shield-hero.fandom.com/wiki/Kyo_Ethnina (the first), https://shield-hero.fandom.com/wiki/Ethnobalt (the book chose him after Kyo's defeat).
* The Vassal Fan and Glass: https://shield-hero.fandom.com/wiki/Vassal_Fan, https://shield-hero.fandom.com/wiki/Glass and
  https://shield-hero.fandom.com/wiki/Glass_(Web_Novel) (a black kimono with silver embroidery). A secondary site calls the fan a black iron war fan
  (https://anihk.com/en/character/glass-the-rising-of-the-shield-hero) and another describes fan dances that release wind blades (https://baike.baidu.com/en/item/Glass/3370538); neither was trusted for colours.
* The Vassal Scythe: https://shield-hero.fandom.com/wiki/Vassal_Scythe and https://shield-hero.fandom.com/wiki/L%27Arc_Berg_Sickle (the thrown energy disc, the floating scythe).
* The Vassal Katana: https://shield-hero.fandom.com/wiki/Vassal_Katana and https://shield-hero.fandom.com/wiki/Raphtalia; accessories made by Naofumi (a plume for the scythe, a charm for the fan, a sheath for
  the katana): https://shield-hero.fandom.com/wiki/Items/Accessories. A fan site's description of a lotus guard and a dragon and phoenix hilt (ranmafanon.fandom.com) was NOT used: it is a single, unconfirmed source on an unrelated wiki.
* The Vassal Carriage (a base shape that looks like a Roman chariot; the Filolial Queen Fitoria): https://shield-hero.fandom.com/wiki/Vassal_Carriage and https://shield-hero.fandom.com/wiki/Fitoria, as quoted in the logic page.
* The eight Vassal Weapons of Glass' world: https://shield-hero.fandom.com/wiki/Vassal_Weapons.

## 6. What could NOT be verified (honest list)

* Nothing was seen in the game: no Java was compiled or run by this job. Every picture is from the software rasteriser of `tools/relics/preview_mesh.py` (nearest texel, cut-out alpha,
  a fixed light, the glow layer added on top). Minecraft's lighting, the real glow pulse (`glow FFFFFF speed lo hi`) and the real render types may look different.
* The first person pictures are a software copy of `ItemInHandRenderer`'s chain as `tools/relics/display_presets.py` documents it (hand at 0.56, -0.52, -0.72; the display transform about the
  cube's centre; a 70 degree field); the player's arm, bobbing and the equip animation are not in it. The carriage's custom first person transform and the vanilla ones of the other four
  (they sit at the lower right edge like every vanilla handheld item, and the book, fan and scythe are partly out of view there, as the existing books and polearms are) were judged from that copy only.
* The carriage entity: how `CarriageRenderer` reads the mesh (the quad lines, the `place` line it ignores, the layers) was taken from the logic page, not from its code. The proportions and
  the part ranges were measured on the written file.
* Colours and shapes are design wherever section 5 gives no source: the shapes of all five weapons are not from any picture; the carriage palette is the logic page's; the katana's lotus guard
  and the like were not used. The Book's BLUE gem is from the wiki's summary; the logic page asked for a white-gold one.
* The carriage's second (roof) jewel is the series' (its jewel is on the roof like the other Vassal weapons: logic page, from summaries), the lantern jewel the page's: both are drawn.
* Texture density: 1.75 px/cm on the scythe and carriage; a 1.5 px line is the thinnest drawn (gold lines are 0.4 to 1.0 cm wide), so very fine tooling is suggested rather than resolved.
* Not built from the logic pages' looks: the scythe's `notch` and `finial` (a counter-blade heel spike stands in), the katana's second "tiny gold dot in the kashira" (a pink drop instead).

## 7. Where this art differs from the logic builders' pages, and why

| weapon | the page asks | built | why |
|---|---|---|---|
| Book | gem white-gold `#FFE9A0` | gem BLUE (sapphire `#2870F0`, light `#4696FF`) | the wiki's summary says the Vassal Book holds a blue gem; the page's author did not have that fact |
| Book | glow: gem, star, frame, loose pages, page edge | the same, plus a ring of rune ticks and four channels in blue | "the weapon that absorbs materials": the lines draw things in; faint, can be dropped (`front()` in `_paint_book`) |
| Katana | gem in the centre of the guard, on the wielder's side | a pointed bicone set through the gold collar just above the guard | the guard's centre is where the blade passes; the collar is the nearest flat that shows from the wielder's side |
| Katana | a second tiny gold dot in the kashira (optional) | a small pink faceted drop | the series puts the jewel on the hilt end of other weapons; it reads in the hand |
| Scythe | parts `notch`, `finial` | not built; a heel spike behind the pole | the page marks them as extras; the spike is the reaper's counter-blade |
| Scythe | steel spike cap at the butt | a long steel spike under a spiked dark disc | the prop listing's "spiked disc" (a hedged source) is added |
| Scythe | glow: gem, edge line, wisps; nothing on the shaft | the same, plus a ring of light round the jewel | the "spinning disc" of the swing |
| Fan | ribs fanned 150 degrees | 170 degrees | a half circle reads better; the page says "like a half circle" |
| Fan | tassel: a red cord with two beads | red cord, a red bead, then the canon charm's jewel, then red strands | the canon charm is a round jewel on a string; it is the second bead |
| Carriage | canopy lining deep blue | lining deep blue, outside royal blue with gold ribs | the page's own sprite note says "a blue canopy"; the model now agrees with its sprite |
| Carriage | seat surface 0.17 L | the bench tops are at 0.17 L; the rear bench has a back that rises above | the riders sit on the tops |
| Carriage | `item class: handitem or new class carriage` | handitem (apparent 0.80) | fits 0.5 to 0.9 |

## 8. For the lead

* Merge `tools/heroweapon/classes_hart3.json` (its `weapons` map) into `tools/relics/weapon_classes.json`, then `python3 tools/relics/audit_weapon_scale.py --write` (it also rewrites
  the game copy `assets/nusmp/weapon_classes.json`). Until then `test_every_mesh_has_a_reviewed_class` fails for these five ids.
* `python3 tools/relics/display_presets.py --apply --include-excluded` changes nothing for the five (the model files were written with the same rule applied). The carriage's
  first person entry is hand-set but its mesh is 1.0 block long, under the 1.4 limit, so the tool keeps it.
* Engravings (rule 6) come from the logic pages' sections (`e("soul_eater", 2) ...`); the art needs nothing more.
* The version bump and the publication of the jar are the lead's (the owner's rules 4 and 8); this job committed and pushed nothing.
* If the entity renderer wants the wheels to turn: pivot lines and part ranges are in section 2.5.
