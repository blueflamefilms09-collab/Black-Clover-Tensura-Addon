# Style guide for the hero-weapon rework (scripted Blender pipeline)

Companion of `style_audit.md` (the evidence). Goal from the owner: the weapons and bows should look designed, "not so AI looking".
This guide is rules a builder script can follow and a checker can test. It does not change any file in `tools/blender/`; it is the brief for the rework.

Repo rules that stay in force (CLAUDE.md): a rework the owner asked for is a **replacement** (rule 5), but old models stay reachable (rule 2: the classic folders and `classicHeroWeaponModels`,
data component ids untouched); every weapon keeps its Tensura engravings (rule 6, `WeaponEngravings.java`) and its Better Combat file (rule 13); models stay Blender-made (rule 14, Blockbench for
cube-style props, rule 15); the Genesis Demon-Slayer is still the quality bar (rule 7); cost may only go **down** (rule 11: fewer, bigger parts means fewer quads and a smaller skin load);
the Time Magic VFX is untouched (rule 3).

## 1. The ten rules in one screen

1. **Silhouette first.** Block out the weapon in 3 to 5 primitives, look at it as a black mask at 64 px, and only then add detail. If two weapons of one family share a mask, one of them is wrong.
2. **Big / medium / small.** Each weapon has one big shape (60 percent of the silhouette mass), two or three medium shapes (30 percent), and small detail (10 percent at most). No flat field of equal parts.
3. **One hero feature.** One thing the eye lands on, defined by lore (the quiver fan, the eagle head, the minute-hand blade). It is oversized and it is the only thing allowed to glow brightly.
4. **Asymmetry on purpose.** The weapon is not mirrored by default: wings or tufts on one limb only, the guard swept to one side, the gem off-centre or sunk. A mirrored pair needs a reason (a shield, a crescent).
5. **Restrained palette with a named accent.** 3 base colours + 1 accent, each named in the builder (`accent = "ember"`). Gold is an accent, not a base. Maximum 6 materials per weapon (bows 7).
6. **Paint the history into the skin.** Edge wear, grime in the crevices, one or two big mottle patches, a repaired section. No geometric noise for this (`noisy()` is retired for limbs).
7. **Fewer, bigger details.** Part budget per weapon in section 5. If a detail is smaller than the diagonal pixel of the 512 skin on a held weapon it is paint, not geometry.
8. **Glow only where lore says.** At most one glow group per weapon (two for curse looks); glow share of surface at most 0.12 (0.25 for fire/light/energy weapons).
9. **Break repetition.** Irregular spacing, grouped counts (2 + 1, 3 + 1), tapering, one-offs. No two weapons in a family may share a literal parameter (gem y/size/colour, band step).
10. **Name the difference.** Before building, write one line: "This weapon is the one with ___" and one line for what it must NOT look like (the other weapon of its family).

## 2. Silhouette: build order and the check

1. **Reference line.** Take the lore row (`docs/heroweapon/weapon_<kind>.md`, the "Look" section and the forms table: what it absorbed) and write the silhouette idea in one sentence.
   Example: Eagle Piercer (`weapon_bow.md`: diamond, netherite, high-quality magic crystal; sniper, `Farsight`, `Eagle Eye`) -> "a plain dark limb with ONE carved eagle head at the upper tip, a long swept
   tail-feather fan on the lower limb only".
2. **Blockout** with `bl_lib.loft/sweep/lathe` or `forms_kit.blade/strip/plate`: no bevel, no bands. Render a flat black mask (profile + three_quarter).
3. **Silhouette tests** (all scriptable; the audit script already does 3 and 4):
   - 64 px mask is recognisable as that weapon with the colours removed (eye check).
   - Largest connected "mass" after a 5 px opening holds at least 55 percent of the mask area (kills a thin stick wearing confetti).
   - Mask IoU against every other weapon of the family below 0.45 (`sameness_report.py`: the `sil IoU` column; the audit found shield 0.76, book 0.64, sword 0.54).
   - Width profile along the long axis has at most 3 local maxima above 60 percent of the max (the "big / medium" rule).
4. Only after the mask passes: medium shapes (guards, crests, limb tips), then small (rivets, line inlays), then paint.

## 3. Shape language per family

| family | big shape | medium | small | avoid (seen in the audit) |
|---|---|---|---|---|
| **Bow** (Legendary Bow + 9 forms) | the limb sweep itself plus ONE oversized feature per form (quiver fan, eagle, web, ice shard cluster, meteor head) | one riser shape and one limb-tip shape, different per form | a grip lashing, a string notch | a gem disc at the same spot, a feather tuft at the riser, gold collars up both limbs, 8 to 12 materials |
| **Sword** | blade profile with its own idea (lightning zig-zag, tortoise carapace, flame edge, hundred small blades), blade is 70 percent of the length | guard swept to one side or a single crescent | a fuller, one engraved line | a ten-talon starburst guard, honeycomb on two swords, a gold sphere pommel on all |
| **Spear / halberd** | the head (leaf, trident, needle, clock-hand) at least 20 percent of the length | the socket / cross-guard piece | one binding | a gold ring every metre of shaft (`legendary_spear` shaft), a red gem under every head |
| **Shield** | an outline from the creature or the idea (egg shell, leaf, balloon, honeycomb) | rim profile with varied thickness; one boss | arm strap, rivets only if the lore is a rivet shield | the sun-compass + rune ring + rim gems stack (`legendary_shield`) |
| **Hammer / axe / mace** | the head mass; blade or face carries one relief | the neck/collar | haft taper and a worn grip | navy block + two gold bands each end + gem + side spike |
| **Staff / wand** | the head (crown, ring, crystal cluster), 25 percent of the length | a ferrule, a knot of the shaft | one rune patch | a dotted-star navy shaft with three gold rings |
| **Katana / scythe / curved blades** | the curve and the tempering line | tsuba, blade tip shape | wrapping | noisy blade skin (hf 26), star halo plates |
| **Book / mirror / carriage / claw (props)** | the main body outline unique per form (a bound pipe book is not a tome with a medallion) | a clasp or strap system, a frame | corner brackets only if the lore calls | one sun medallion + gold corners + page fan on all four books |

Taper everything that is long: limbs, shafts, blades, claws, tails reduce 30 to 60 percent along their length; collars sit at the thick end only.

## 4. Asymmetry and one-offs (scripted)

- Add `asym` as a first-class step: apply one seeded 2 to 6 percent scale and 2 to 4 degree rotation difference between the "pair" parts (`forms_kit.asym_mirror()` exists and nothing calls it today; wire it in).
- One-off rule: every weapon gets at least one element that appears exactly once and is not on the centre line (a broken tip, a repaired limb, a hanging charm, a chipped guard, a single oversized feather).
- Pairs: allowed only for crescent, shield, wings of a bird. Everything else: unequal count (3 on one side, 2 on the other) or unequal size (graded 1.0, 0.82, 0.64).
- Seeds: `rng(seed)` differs per weapon and per form; never reuse the literal seed of another form (the bow forms use 5+s, 11+s, 21+s ... keep the idea, but vary counts too).

## 5. Materials, palette and the part budget

**Palette recipe** (builder constant, checked by the script):
- 3 base materials + 1 named accent + at most 2 utility materials (leather, cord) = **6 max** (bows 7, composite creature weapons 8).
- Base hue families come from the material the form absorbed (`forms` tables): bone and sinew, copper and verdigris, ivory and ash, obsidian and ember, ice and prismarine. **Gold is allowed as accent or as a base only
  when the lore says gold** (Legendary base weapons, Sagittarius' star-map, Meteor Bow's nuggets).
- Gold budget: at most 12 percent of surface (script: `gold` column; today 59 of 154 weapons are gold-dominant, bows 0.15, Sagittarius 0.32).
- Palette distance: nearest other weapon at least 0.25 (script `pal dist`; today the nearest-neighbour distance for bows is 0.17 to 0.22, books 0.12).
- Accent saturation: only the accent may exceed saturation 0.6 / value 0.75 (the "gem colour"); bodies sit at value 0.15 to 0.65.
- Keep the Genesis Demon-Slayer in mind (rule 7): dark body, restrained, one thing that burns.

**Surface** (use `forms_kit.surface()` for every weapon, including the base ones that still use `bl_lib.mat()`):
- `wear` 0.35 to 0.7 on metal edges with a bright `wear_color`; paint over metal = base metal 0 + `wear_metal` 1.
- `dirt` 0.2 to 0.5 for crevices (AO), `mottle` for one big stain / patina / forging colour (not per-part noise).
- `scratches` 0.1 to 0.3, never on glowing parts.
- Bevel width by class: blades 0.05, hardware 0.14, organic or wood 0.25; **not one value for the whole weapon** (today `mat(... bevel=0.14..0.3)` is the same on every part).
- Skin hf (script column `hf`): target below 12; if a surface reads rough it must be because of a painted break (a rust patch, a cloth wrap), not 512 px of bump noise. Katanas at 25 to 26 are the worst.
- Retire `noisy()` on limbs: replace with a single hand-placed bend (an asymmetric camber) and carved knots as paint.

**Part budget per weapon** (counts of geometric "ornament groups"; a group = a set of identical islands):

| pattern | max groups | max items per group | rule |
|---|---|---|---|
| bands / rings / collars | 2 | 3 | different widths, spacing ratio 1 : 1.6 : 2.4 (never equal), none on the hero feature |
| gems / jewels | 1 hero (+1 small) | 1 | size 40 to 100 percent different from the other forms of its family; sunk bezel or claws, not both on all forms |
| studs / spikes / thorns | 1 | 5 | only if the lore names them (thorn, hedgehog, chimera); graded lengths, not equal |
| wrapped grip (cord + collars) | 1 | - | a grip type per family: leather lace (sword), bare wood (spear), bone (bow), cloth (staff) |
| wing blades / feather tufts | 1 | 5 (graded) | on one limb or one side unless a bird's pair |
| star plates, rune rings, constellations | 1 | - | paint them in the skin where possible (they cost quads and read as clutter) |
| rivets | 1 row | 6 | only on plates; irregular |

Quad budget goes down: in the 108 listed hero weapons a mean of 28 percent of quad lines sit in band/ring/gem/stud/grip/star/wing/rune parts (38 percent in the worst 30); the target is 15 percent. The 2800-quad cap and the perf rules (11) are therefore safe.

## 6. Glow: only where lore says

| weapon type | glow allowed | glow share of surface |
|---|---|---|
| Legendary base weapons | the jewel only (canon: the gem) and, for the bow, the arrow of light | at most 0.08 |
| Forms | the one lore-driven feature (flames of the Phoenix Sword, lightning of the Thunder God Sword, ice of Frozen Rain) | at most 0.12; 0.25 for pure fire / lightning / light forms |
| Curse looks | the sin fire on 2 features (gem + edge or eyes) | at most 0.25 |
| Seven Star / Vassal base | the one named weakness (the axe's edge, the staff's crystal) | at most 0.10 |

Glow goes on **one** part class, never on gem + inlay + string + fins + studs together. Current mean is 0.27 and 80 of 154 are above 0.25 (script `glow` column).

## 7. How to break repetition (the toolbox)

1. **Irregular spacing.** Positions from `t_k = t0 + sum(gap_i)` with `gap_i` drawn from a fixed unequal set such as (0.9, 1.6, 0.7, 2.1) times a base; never `0.1, 0.24, 0.38 ...`. Script check: gap CV between 0.35 and 0.8.
2. **Grouping.** Items come in 2 + 1 or 3 + 1 clusters with a gap, like real lashings, not an evenly spaced picket row.
3. **Taper.** Size, width and thickness shrink along the length (collars 1.0 to 0.5), height of a feather fan grows then falls.
4. **One-off.** Section 4.
5. **Scale jumps.** In every cluster at least one item is 1.6 to 2.2 times the others.
6. **Break the helper defaults.** Every new helper takes the variation as a parameter that has NO default equal to the old literal (`jewel(size, mount, tint, glow)`; `band(widths, gaps, taper)`; `grip(kind=...)`).
7. **Cross-form check.** Run the report; if two forms of one family have `pal dist` < 0.25 or `sil IoU` > 0.45, fix the later one.

## 8. Helper changes to ask for (new code, old behaviour kept behind a flag, rule 2 / 12)

| helper | change |
|---|---|
| `forms_legendary_bow.band()` and the 5 other `band()`s | add `gaps=`, `widths=`, `taper=`; default group count 2; keep the old signature as `band_even()` for the classic look |
| `jewel()` | parameters for size, mount (`bezel`, `claws`, `sunk`), tint and emission per form; remove the shared 244,172,20 gem colour from the palettes except the base bow; offset y/z per form |
| `hand_binding()` | grip kinds (`lace`, `bare`, `bone`, `cloth`), one collar, wear |
| `star_plate()`, `noisy()` | retire from new work; paint stars; asymmetric camber replaces noise |
| `tuft()` / feather helpers | graded lengths, count 3 to 5, one side |
| `forms_kit.asym_mirror()` | wire into every builder's "pair" step |
| `legendary.py`, `seven_star.py`, `vassal_a/b.py` | move their `mats()` to `forms_kit.surface()` (wear, dirt, mottle); this alone removes the "freshly rendered" look from the 11 base hero weapons |

## 9. Before / after checklist (one page per weapon; the reviewer ticks all)

Before building:
- [ ] One-sentence idea and one-sentence "not like" (rule 10 of section 1).
- [ ] Lore source named (English wiki first, Japanese fallback; rule 10 of CLAUDE.md) and the forms table row read.
- [ ] Accent colour named; gold only if the lore says gold.
- [ ] Hero feature chosen; size at least 20 percent of the weapon length or 15 percent of its profile area.
- [ ] Engravings listed (`WeaponEngravings.java`) and Better Combat file planned (`tools/bettercombat/gen_attributes.py`).

After building (run `python tools/heroweapon/style/sameness_report.py --top 30`):
- [ ] 64 px black mask recognisable; largest mass >= 55 percent; mask IoU vs family < 0.45.
- [ ] Palette: <= 6 materials (bows 7); nearest palette distance >= 0.25; gold <= 0.12.
- [ ] Pattern groups within the budget table; no row of equal spacing (gap CV 0.35 to 0.8); no band on the hero feature.
- [ ] Exactly one hero gem or none; its size, mount and colour differ from the family siblings.
- [ ] At least one asymmetric element and one off-centre one-off; mirror score below 0.8 unless a shield / crescent.
- [ ] Glow share within section 6; glow on a single part class.
- [ ] Skin hf below 12; wear / dirt / mottle visible in the close-up; bevel widths differ by part class.
- [ ] Quads and skin size equal to or smaller than before; `PerfProbe` numbers noted in the doc (rule 11).
- [ ] Contact sheet of the family side by side (profile) shows five different silhouettes.
- [ ] Hand-over lists NEW vs REPLACED vs KEPT, which models came from Blender / Blockbench / could not.

## 10. The sameness score (what it is and how to use it)

`tools/heroweapon/style/sameness_report.py` (stdlib + numpy + PIL, reads `weapon_meshes/*.txt` and the skins; 17 seconds for 154 weapons):
`sil IoU` (profile mask overlap with its family), `pal dist` (4x4x4 RGB histogram distance to the nearest weapon), `gold/navy/gem` shares, `pattern` (quads in band, gem, stud, grip, star, wing, rune
parts), `repeat` (quads in rows of 3 or more identical islands and their spacing CV), `mirror`, `glow`, `hf` (skin noise). The composite `sameness` weights are in `WEIGHTS` at the top of the file.

Targets after the rework: no weapon above 45 composite (today the listing of 108 runs 75 down to 54 over its top 30, median 47); family mean IoU below 0.45; zero weapons with an even row; mean glow below 0.15;
gold-dominant weapons below 25 of 154.

Commands: `python tools/heroweapon/style/sameness_report.py --top 30 --md out.md`; `--sheet sheet.png` (contact sheet of the 10 worst, uses `tools/relics/preview_mesh.py`);
`--only REGEX --exclude REGEX` to change the listing; `--all` to include the non-Blender weapons; `--selftest`.
Limits: see `style_audit.md` section 5 (the palette under-reads navy; IoU is a profile view; repeat rows also fire on feather tufts).

## 11. Ranked rework list: the 20 worst weapons and bows, in three waves

Rank = the script's composite plus a visual correction (the Seven Star base weapons and the base Legendary weapons are underrated by the script because they have few parts and dark lit navy; they are
the clearest case of the recipe on the contact sheets). "Script #" is the rank in the listing (`legendary_*` and `vassal_*`, curse looks excluded). Each curse look is reworked together with its base
(same builder). The direction lines use the lore in `docs/heroweapon/*.md`.

### Wave 1: the bows (fix `band()`, `jewel()`, `hand_binding()` first; one helper edit improves all nine forms)

| # | weapon | script # | direction (change what, from lore) |
|--:|---|---|---|
| 1 | `legendary_bow__sagittarius` | 17 | Mod-invented tier-5 centaur archer (nether star, dragon breath): midnight limbs with ONE gold accent, the horse-head tip kept as the hero feature, delete the 14 even gold collars and 12 star plates, paint the star map into the skin, a small sunk sapphire, glow only on the star points |
| 2 | `legendary_bow__eagle_piercer` | 2 | Sniper (Farsight, Eagle Eye): plain dark limb, one carved eagle head at the upper tip and a long swept tail-feather fan on the lower limb only; two uneven leather lashes instead of 4 gold hoops; no glow |
| 3 | `legendary_bow__hunters_bow` | 6 | Tier-1 starter (string, feather, bone): bone-and-sinew recurve with a thick bone riser, two uneven lashings, one amber chip instead of the polished jewel, one feather, no gold |
| 4 | `legendary_bow__arrow_shower` | 7 | Gunpowder, arrows: keep the quiver fan as the hero, make the limbs a plain iron-banded horn bow with a brass riser only; remove the 14-row alternating brass/iron hoops and the riser jewel |
| 5 | `legendary_bow` (base, with `legendary_bow_curse`) | 10 | Canon winged bow with THE GEM (`weapon_bow.md` section 1): one big folded wing of graded gold blades on the upper limb only, ivory limbs with a gold edge line painted not modelled, the cabochon in a plain bezel, smooth grip with one cord band; curse: broken crown and hooked talons stay asymmetric |
| 6 | `legendary_bow__bird_hunting` | 11 | Phantom membrane, feathers, rabbit foot: one large translucent membrane sail on the upper limb, lower limb bare, delete the silver ring row and the matching jewel; keep the rabbit-foot charm as the one-off |
| 7 | `legendary_bow__scouts_bow` | 32 | Leather, rabbit hide, copper (Farsight): two copper sleeves of different length and one hanging charm; replace the 5 verdigris hoops and the jewel with a painted verdigris patina; leaf set reduced to one side |

(`bind_arrow`, `meteor_bow`, `frozen_rain` are already built around one large idea; they ride along when the helpers change and then get the same checklist.)

### Wave 2: the base hero weapons that define the recipe

| # | weapon | script # | direction |
|--:|---|---|---|
| 8 | `legendary_shield` (base, + `legendary_shield_curse`) | 3 | Shield Hero's round shield with the green gem: keep the gem, delete the 12 rim gems, the 8-ray sun compass and the rune ring (runes into the skin as worn engraving), varied rim thickness, leather arm strap, edge wear |
| 9 | `legendary_sword` (base, + `legendary_sword_curse`) | 19 | Hero sword of the four: guard swept to one side as one cross-guard (not ten mirrored thorns), blade with a fuller and one rune line, plain pommel, navy lace grip with one collar, gem only |
| 10 | `vassal_hammer` | 56 (of 108) | Seven Star hammer: single face relief of the star ring on a dark iron head, drop the 4 gold bands and side spike and the second gem; worn oak haft with taper, glow on the ring only |
| 11 | `vassal_staff` (+ `rune_staff` #14, `archmage_staff` #18) | 31 | Choose crescent crown or eye ring, not both; shaft bare wood with one wrapped section, no dotted-star navy, one crystal that glows |
| 12 | `vassal_book` (+ `revised_edition` #4, `pipe_book` #12, `library_tome` #15) | 1 | Five books on one outline (IoU 0.83): give each its own body from the lore (pipe book bound with iron pipes; animal needle book with needles; library tome thick with a chain strap; revised edition with loose annotated pages); no shared sun medallion, no gold corners everywhere |
| 13 | `legendary_shield__light_metal_shield` | 5 | The party's wall: a plain bulwark plate with one large asymmetric wing blade set, delete the ring of near-identical leaf blades and the twelve small cyan accents |
| 14 | `vassal_axe` (Seven Star) | 75 (of 108) | Move it to the quality of `timber_axe`/`shine_axe`: one crescent blade of cold steel with the constellation as a painted scratch pattern, drop navy block + gold bands, keep the cyan edge as the only glow |

### Wave 3: spears, katanas and props

| # | weapon | script # | direction |
|--:|---|---|---|
| 15 | `legendary_spear__dragon_minute_hand` | 9 | The time spear (Dragon Minute Hand, Fitoria feather keepsake): one long minute-hand needle blade as the hero, a dragon-scale socket, the feather as the one-off; delete the 10 repeated ornament rows |
| 16 | `legendary_spear` (base, + `legendary_spear_curse`, `portal_spear` #33) | 23 | Leaf head with two hooked arms, bare wood shaft with one binding (not gold rings), red gem small; graded arm lengths |
| 17 | `vassal_katana__siderite_katana` (+ `white_tiger_katana`) | 8 | Stardust Blade: dark meteoric blade, one tempering line, delete star halo plates and the 26-hf noise skin, glow only on the hamon; white tiger gets a tiger-stripe temper line instead |
| 18 | `vassal_carriage__war_carriage` (+ `queens_chariot`) | 13 | Roman-chariot silhouette (`weapon_carriage.md`): big war-scarred hull, banner pole as the one-off, cut the 16 rivet rows to two irregular ones, no gold |
| 19 | `vassal_mirror__yata_mirror` | 20 | Sacred bronze mirror: a large round disc with a heavy engraved rim and a worn handle, different outline from the white tiger and envious mirrors (IoU 0.58), delete gold dots |
| 20 | `legendary_sword__phoenix_sword` | 16 | Phoenix Gale Blade: blade of hot metal graded red to black painted into the skin, one swept feather guard, gem tiny; fire glow only |

## 12. Not covered (said plainly)

- The about 460 weapons of the other pipelines (`tools/relics/wave*_models.py`, `gen_weapon_models.py`, `djinn_weapons.py`) were not rated; run the script with `--all` to see them.
- Nothing was seen in a game. The contact sheets are software renders; the real look on a graphics card under the game's light may differ slightly (see `blender_pipeline.md`).
- The directions are art direction from lore and the previews; the lore parts of `weapon_*.md` mark what is canon and what is the mod's invention, and this guide follows that.
