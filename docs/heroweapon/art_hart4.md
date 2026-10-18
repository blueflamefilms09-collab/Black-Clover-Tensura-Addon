# The art of five Vassal weapons (hart4): Mirror, Harpoon, Chain, Musical Instrument, Sewing Set

Why this file exists: the owner's rule 7 says every weapon is a real 3D model in hand, keeps its 16 x 16 sprite as the inventory icon and gets a glow that fits it, with the
Genesis Demon-Slayer as the standard. The five Vassal weapons the logic builder `hvd` wrote (`vassal_mirror`, `vassal_harpoon`, `vassal_chain`, `vassal_instrument`,
`vassal_sewing_set`) had no art at all; this builder (`hart4`) made it. The Holy Weapons and the other Seven Star weapons are in the other art builders' docs.

New or replacement (CLAUDE.md rule 5): everything here is **NEW**. No mesh, skin, sprite or model json of these ids existed (`git ls-files | grep vassal` is empty at the
contract commit; only the contract code and the item names in `en_us.json` mention them). Nothing is replaced or removed; every old mesh, sprite and tool is only read.
Vassal weapons have no curse look (design.md section 11), so each weapon has exactly one look.

## 1. What it writes, and how to run it

Two deterministic scripts (a second run writes byte-identical files; the self test proves it):

    python3 -B tools/heroweapon/art_hart4.py [id ...]                     # build the files; no ids = all five
    python3 -B tools/heroweapon/art_hart4.py --check                      # read the written files back and list problems (nothing is rebuilt; ~2 s)
    python3 -B tools/heroweapon/art_hart4.py --check --preview=DIR        # also render the pictures into DIR (about 2 minutes); HART4_PREVIEW=DIR does the same
    HART4_PARTS=1 python3 -B tools/heroweapon/art_hart4.py vassal_chain   # also prints the quads of every part
    python3 -B tools/heroweapon/test_art_hart4.py                         # the self test (about a minute: it builds the five weapons twice into temporary folders)

`art_hart4_kit.py` holds what is not specific to one weapon (curves, a few solids the library lacks, the paint kit, the sprite and item-model writers, the placement on
the sprite's diagonal, the software "held in the hand" views). `art_hart4.py` reads as the design of five weapons: one constants block, one `build_<x>` (the mesh), one
`paint_<x>` (the skins) and one `icon_<x>` (the sprite) per weapon, every proportion a named constant with a comment, and the driver.

The method is the owner's (`tools/relics/mesh_lib.py`, worked examples `wave23_models.py` and `wave1_models.py`): real cross-sections (lofts, lathes, sweeps), the small raised
parts as their own named parts, normals smoothed by angle per part (written as `qn` lines), UVs projected on the dominant axis or unrolled along a ring, painted materials,
one 512 px skin and a second additive glow skin. Nothing in `tools/relics` or any shared file was edited.

| file (under `src/main/resources/assets/nusmp/`) | what |
|---|---|
| `weapon_meshes/<id>.txt` | the mesh: `qn` lines, `place` on the sprite's 45 degree diagonal, `glow` (colour, pulse speed, low, high), `grip` where a part name decides it |
| `textures/entity/weapon/<id>.png`, `<id>_glow.png` | the painted 512 px skin and the additive glow skin (the gem is on it, in the weapon's colour) |
| `textures/item/<id>.png` | the 16 x 16 sprite, drawn by hand-built pixel rows in the script (not a downscaled render) |
| `models/item/<id>.json`, `<id>_icon.json` | the in-hand model (`builtin/entity`, the display transforms of `brooch_pin`, plus `display_presets` for long weapons) and the flat inventory model (`handheld`) |
| `tools/heroweapon/classes_hart4.json` | id -> size class of `weapon_classes.json`, for the lead to merge |

`HeroAssetsTest` (the six files per weapon) is satisfied for all five: checked by `ls` and by the script's own `--check`.

## 2. The five weapons in numbers

Sizes are APPARENT blocks (mesh length x the 0.85 third-person display scale), the unit the class table uses. "Life size" is how much the real-centimetre drawing was enlarged
to meet the class's rule (apparent / (mesh cm x 0.85 / 100)): the harpoon is a true 2.8 m whaling harpoon; the violin is the biggest stretch because class `wand` starts at 0.8.

| | mirror | harpoon | chain | instrument (violin) | sewing set |
|---|---|---|---|---|---|
| class (`weapon_classes.json`) | `shield` 0.9 to 1.2 | `staff` 2.0 to 2.6 | `whip` 1.6 to 2.4 | `wand` 0.8 to 1.2 | `rapier` 1.0 to 1.3 |
| apparent length in the hand | 1.20 | 2.40 | 1.75 (along its axis) | 1.15 | 1.30 |
| mesh length, model cm | 93.8 | 279.0 | 171.5 | 74.2 | 122.6 |
| life size factor | 1.5 | 1.0 | 1.2 | 1.8 | 1.25 |
| named parts (8 to 14) | 14 | 12 | 10 | 14 | 11 |
| quad lines (budget 2800; body + glow copies) | 2508 | 1954 | 1866 | 2321 | 1784 |
| skin density, px per model cm | 4.20 | 3.88 | 4.04 | 4.29 | 3.85 |
| the hand holds, share of the mesh from the butt | 0.25 (the grip) | 0.25 (the wrap) | 0.25 (top of the grip) | 0.58 (the neck) | 0.25 (the spool) |
| the gem in the hand, blocks (across x along) | 0.075 x 0.099 | 0.045 x 0.064 | 0.062 x 0.078 | 0.084 x 0.055 | 0.083 x 0.070 |
| glow pulse (speed, low, high) | 0.10, 0.40, 1.0 | 0.22, 0.45, 1.0 | 0.045, 0.45, 1.0 | 0.14, 0.40, 1.0 | 0.12, 0.40, 1.0 |

Why these classes: the Mirror is held up like a small shield and works like one; the Harpoon is shorter than a lance, and the spear / trident classes start at 2.8 blocks, so the
2.4 block harpoon belongs to `staff` (hvd's doc leaves the choice to the art builder); the Chain is a long flexible weapon (the chain falls away from its axis, so its path is
about 2.4 blocks long); the violin and bow are held like a casting implement; the needle is a thin pointed weapon. The mirror and the sewing set sit at their class maximum (1.20 and
1.30) because hvd asked for about 1.3 and 1.5 blocks with their handles.

### 2.1 Vassal Mirror (`vassal_mirror`): a cold silver hand mirror, Envy by design

An ornate silver hand mirror held by its handle with the glass forward like a small shield (hvd's look: an oval, slightly teardrop frame, a fluted handle, a crest of three lobes
that holds the gem, two scrolls down the sides, a row of pearls). Parts: `pommel`, `grip` (eight flutes), `collar` (ferrule and flared neck), `chain_loop` (a ring, two small links
and a bead hanging from the pommel: the "mirror seen once, travel to it" motif), `head_rim` (an octagon-section frame with two cut lines and an engraved scroll), `head_bezel`,
`head_beads` (12 pearls), `head_glass` (a very shallow dome set under the rim), `back_plate` (engraved with a hexagram in two rings) and `back_boss`, `scrolls` (two volutes and a
vine on each side), `crest` (three leaf lobes), `gem_claws` (six claws) and `head_gem` (an aquamarine cabochon). The glass is painted as a bright reflective panel with two diagonal
sheen bands and three small stars; the glow skin carries the shimmer (strongest in the diagonal band and at the rim, cold white-cyan), the gem (full) and the pearls (faint).
Palette: polished silver, tarnish in the grooves `#8D98A5`, pearls, glass `#BFD9EA` to `#E8F6FF`, aquamarine `#4FD8E8` with glow `#7FF2FF`.
Canon (search summaries): the Vassal Mirror specialises in magical defence and movement, with skills that raise a glass shield, float a mirror and reflect all magic. The shape is
hvd's design; the engraving, the volutes and the claws are mine.

### 2.2 Vassal Harpoon (`vassal_harpoon`): a whaling harpoon with its line

A true-size 2.8 m whaling harpoon: a weathered dark wood haft in three pieces (so each skin strip stays short), a weighted iron butt, a leather grip wrap, three bronze bands, a
hemp line wound in two loose coils with the run to an iron eyelet ring and a loop hanging from it, a long iron socket with three bronze trims and a wave scrollwork, a neck with the
gem in two iron claws, a leaf-shaped blue-steel head with a crooked lightning vein, and one large swept-back barb each side. Parts: `butt`, `shaft`, `grip_wrap`, `bands`, `rope`,
`eyelet`, `socket`, `socket_bands`, `head_neck`, `gem`, `head_blade`, `head_barbs`. Glow: the gem (`#BFF6FF`), the lightning vein down the head and along both barbs (`#9FEFFF`,
thin crooked lines, not a filled area) and a faint ring round the eyelet. Canon: skills Lightning Strike Dragon, Whale Hunting Harpoon (a second harpoon is thrown), Brave Fish
Blaster, Lightning Shock Harpoon, Crucifixion; power-up by Magic and Skill Mastery. The shape (a harpoon with a tether) is hvd's design.

### 2.3 Vassal Chain (`vassal_chain`): a heavy war chain, Sloth by design

A kusarigama-style war chain without the sickle: a leather-wrapped grip with a blackened pommel and a short leather strap, a forged clasp (two ears, a pin with a ring, a hook) around an
iron bezel with the slate-blue gem, fifteen interlocking links (12.4 x 7.4 cm centre line, 1.6 cm wire, each turned 90 degrees from the last) falling in one slow S-curve forward and
down (mesh -x is down in the third-person hand), a swivel and a weight: a thick bar ending in a ball with five studs. Parts: `grip_cap`, `grip`, `strap`, `clasp`, `clasp_bezel`, `gem`,
`links`, `swivel`, `weight`, `spikes`. Each link is painted differently (rust-brown patches, bright wear on the outer curve, soot) from its own seed. Glow: the gem (`#BFD8EE`, full) and a
very faint slow pulse on the first three links (`#8FB0CC`), as if the weapon were heavy with sleep. Canon: from S'yne's world; its skills bind and incapacitate targets, defend with
chains, teleport, spin the chain into a ring and wear it as armour. The shape is hvd's design.

### 2.4 Vassal Musical Instrument (`vassal_instrument`): a violin with its bow

The wiki lists a violin among the instrument's forms and hvd chose it as the look. A full violin with real proportions (two bouts and a waist, arched top and back plates over ribs,
f-holes, a bridge, a tailpiece, a chin rest, four strings, a fingerboard, a pegbox with four pegs, a scroll) held by the neck, the scroll up and forward, the bow laid across the strings
with its tip forward. Parts: `body_top`, `body_back`, `ribs`, `neck_scroll`, `fingerboard`, `pegs`, `strings`, `bridge`, `fittings`, `gem`, `gold_fittings`, `bow_stick`, `bow_hair`,
`bow_frog`. The gem (rose-coral `#FF7AC8`) is a lens in the eye of the scroll, in two gold rings with claws, so it shows from both sides; the scroll and pegbox are 1.3 times life size so
the eye can carry it. Paint: maple-amber varnish darkening to the edge, a thin gold line of purfling, a figured (flamed) maple back, a pale highlight on the upper bout, three tiny note
inlays on the top plate. Glow: the gem (`#FF9AD8`), a thin warm line round each f-hole (`#FFB347`), the three notes (`#FFDDAA`), the strings (very faint `#FFE9B0`); the varnish does not
glow. Canon: the Vassal Musical Instrument's forms include a violin; its first known wielder was Hidemasa Miyaji, then Itsuki Kawasumi.

### 2.5 Vassal Sewing Set (`vassal_sewing_set`): a needle on a spool, with scissors

hvd chose a long steel needle with a large oval eye, set into a dark violet thread spool that is the grip (two silver-rimmed flanges, crimson silk wound on the core, a thimble as the pommel),
an amethyst cabochon in the centre of the top flange where the needle enters, a silk thread (one spline) that rises over the gem, passes through the eye from the front to the back and trails
in one loose S-curve, and small scissors hanging from the spool on a cord. Parts: `grip_core`, `grip_rims`, `grip_thimble`, `grip_thread`, `gem_bezel`, `gem`, `needle_eye`, `needle_point`, `thread_trail`, `cord`, `scissors`. The thread fades
from crimson `#C03A78` to violet `#7A3FB5`; the glow is the gem (`#D7A8FF`), the thread (faint violet `#B77CF0`, brighter toward its free end) and a hair-thin line down the needle.
Canon: from S'yne's world, a purple gem that absorbs materials, thread skills (Bind Wire, Spider Wire), needle skills (Needle Shot, Jumping Needle) and scissor skills (Scissor Cut,
Scissor Blaze); the same summaries describe its main form as a giant pair of scissors (and a ball of yarn as another form). hvd decided on the needle because it holds the thread and
reads as a point; the scissors on the cord and the spool are the nod to the other two forms. The owner may want the giant scissors instead: that would be a new mesh, not a change here.

## 3. Placement, sizes and the hand

* Axes (docs/weapon_scale_orientation.md): y along the weapon, the grip at low y; mesh -x is forward (down in the third-person hand); the face of a flat piece looks toward +z. Every
  mesh lies on the sprite's 45 degree diagonal with its tip up-forward; the audit finds no flag and `grip_at` low for all five.
* `place_on_diagonal` (kit) is `mesh_lib.place_on_sprite` with the axis fixed at 45 degrees and the grip generalised: the point of the mesh at share `grip` of its length lands where the
  hand holds a sprite (a quarter of the way along its diagonal, `display_presets.GRIP_FRACTION`). For `grip` = 0.25 it is exactly `place_on_sprite`'s growth about the hand (a test compares
  the two). The violin is the one exception (0.58): the hand holds the neck, the body hangs behind it and the scroll and the bow tip lead. It has no `grip` line in its mesh on purpose:
  a stated share above 0.5 would make `WeaponNormaliser` turn the mesh over; without the line the thin-end geometry decides, and it says "grip low".
* Display transforms: `brooch_pin`'s, then `display_presets.plan_for / apply_plan` for each id (first-person scale and translation for the long ones: harpoon 0.357, chain 0.627, mirror and
  sewing set 0.68 with their own translation; ground and item-frame scales for the two over 1.6 blocks). The shared `tools/relics/display_presets_manifest.json` was NOT touched (it is
  the lead's); `python3 tools/relics/display_presets.py --check` reports "0 to rewrite" with these files in place.
* `python3 tools/relics/audit_weapon_scale.py` rows for the five (with their classes merged in memory): all inside their class range, no status, no flag.

## 4. For the lead: integration

1. Merge `tools/heroweapon/classes_hart4.json` (flat id -> class) into BOTH copies of the class table, under `"weapons"`: `tools/relics/weapon_classes.json` and
   `src/main/resources/assets/nusmp/weapon_classes.json` (the game's copy, plain strings; `python3 tools/relics/audit_weapon_scale.py --write` makes it from the first one, `--check` proves
   the two equal and that every mesh has a reviewed class). The lines:

       "vassal_mirror": "shield",
       "vassal_harpoon": "staff",
       "vassal_chain": "whip",
       "vassal_instrument": "wand",
       "vassal_sewing_set": "rapier"

   Until this is merged `tools/relics/test_weapon_scale.py` fails `test_every_mesh_has_a_reviewed_class` (and the copy-equality test); with the five lines merged in memory the whole
   file passes (35 tests).
2. Engravings are not mine: hvd gives the five `WeaponEngravings` lines in section 9 of its `weapon_<name>.md` files.
3. No lang entries, no quest keys, no shared item; no Java was written or touched.
4. Optional: `python3 tools/relics/display_presets.py --apply` records the five rows in the manifest (it changes no model json: they already hold the preset).

## 5. Checks that were run

* `python3 -B tools/heroweapon/art_hart4.py --check`: the six files of each weapon exist; every `qn` quad has UVs in the skin and unit normals; at most 2800 quad lines; 8 to 14 named
  parts; both skins 512 px and the glow skin not empty; the gem is on the glow skin in the weapon's colour (the glow skin is sampled under each glow quad of the gem part and its hue must be
  within 25 degrees of the weapon's gem colour); the sprite 16 x 16; the class exists; the repo's size and orientation audit finds no status and no flag and `grip_at` low.
* `python3 -B tools/heroweapon/test_art_hart4.py` (15 tests): files and budgets; every gem glows in its colour (and the check does catch a wrong colour and a part that does not glow); the class
  fragment equals the script's table; sprites 16 x 16 and not blank; two builds write identical
  bytes; the committed files equal a fresh build (so a changed number cannot hide behind stale pictures); the hand point maths; the placement equals `mesh_lib.place_on_sprite` for a
  quarter grip; the spline passes through its control points; the arc resampling is even; a chain link and a swept loop are closed surfaces (and the plain `ml.sweep` of the same ring is
  not: the reason `sweep_loop` exists); the mirror outline is counter-clockwise and egg shaped. Mutation check: changing the hand point to 0.40 fails the placement test.
* The repo's own `tools/relics/test_weapon_scale.py` with the five classes merged in memory: 35 tests, only the "game copy equals the tool table" test fails, as expected until the lead merges.
* Every render was looked at (section 6). Fixed along the way: a wedge-shaped crack at the seam of every swept ring (rim, links, eye, rings), a mirror over the quad budget, washed-out
  gems (the additive glow turned a bright gem body white: the gem bodies are deep colours and the light is in the glow skin), a chain clasp that read as two round ears, a floating
  scissors ring, an atlas wasted by the diagonal bow parts, and a Z-shaped kink in the sewing set's thread at the eye (two splines joined end to end, the first one overshooting the eye).

## 6. Pictures (software renders; `$S/previews/hart4/`)

`vassal_<id>.png` (profile, three-quarter, edge, top), `vassal_<id>_back.png` (back, three-quarter, edge), `held_vassal_<id>_third.png` (third person, beside Steve) and
`held_vassal_<id>_first.png` (first person) for each of the five, `sprites.png` (the five sprites at 14 x) and `audit_sheet_01.png` (each weapon in the sprite plane and held).

## 7. Sources (search summaries only: the wiki pages themselves could not be opened from the build machine)

* https://shield-hero.fandom.com/wiki/Vassal_Mirror (skills: glass shield, floating mirror, reflecting all magic; magical defence and movement)
* https://shield-hero.fandom.com/wiki/Vassal_Harpoon (the skill names above; Magic and Skill Mastery; light novel based)
* https://shield-hero.fandom.com/wiki/Vassal_Chain (S'yne's world; the skill groups above)
* https://shield-hero.fandom.com/wiki/Vassal_Musical_Instrument (the violin among its forms; Hidemasa Miyaji, Itsuki Kawasumi)
* https://shield-hero.fandom.com/wiki/Vassal_Sewing_Set (the purple gem; thread, needle and scissor skills; the giant scissors and yarn forms)
* https://shield-hero.fandom.com/wiki/Vassal_Weapons and https://shield-hero.fandom.com/wiki/Vassal_Weapons/Full_list_of_Weapons (the list and the worlds)
* `docs/heroweapon/weapon_mirror.md`, `weapon_harpoon.md`, `weapon_chain.md`, `weapon_instrument.md`, `weapon_sewingset.md` in the `hvd` worktree: the look each weapon was built to.

What is canon, what is memory, what is design: the skill names, gem colours (purple for the sewing set), worlds and wielders are from the summaries; the SHAPES of all five are design (the
series' wiki summaries describe what the weapons do, not what they look like, and no picture could be read), mostly hvd's, with the modelling, engraving, painting and glow mine.

## 8. Not verified (honest list)

* Nothing was run in the game: no Gradle build, no renderer, no `HeroAssetsTest` (its six files per weapon were checked with `ls` and the script's `--check`).
* Every picture is a software render by the repo's own preview rasteriser (`tools/relics/preview_mesh.py`, `weapon_sheet.py`) and by `held_view` in the kit, which applies the model json's
  display transforms and the `place` line with vanilla's hand pose. Lighting, the glow's brightness and pulse, and the first-person framing (long weapons sit at the edge of the view) are
  approximations: the glow is drawn additively in the game and may bloom whiter than here.
* The size classes and the hand points are my choices (section 2); the harpoon is `staff` where hvd suggested `spear` or `trident` (whose minimum, 2.8 blocks, is longer than the 2.3
  hvd wrote). The mirror (1.20) and the sewing set (1.30) are at their class maximum, shorter than the 1.3 and 1.5 blocks hvd mentioned.
* The gems are 0.045 to 0.099 blocks tall in the hand, smaller than the "about 0.12" of hvd's docs (a 0.12 gem would be wider than the harpoon's socket and the chain's clasp); they are named
  constants (`M_GEM`, `H_GEM`, `C_GEM`, `S_GEM`) if the owner wants them bigger.
* `tools/item_preview/preview_weapons.py` (named in rule 7) reads only the old `q` lines, not the `qn` lines that 178 of the 232 meshes in the tree (these five among them) use, so it cannot show them;
  `preview_mesh.py` and the audit sheet were used instead.
* The sewing set is a needle, not the giant scissors the summaries call its main form (hvd's decision, section 2.5).
