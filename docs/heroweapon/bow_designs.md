# The eleven bows: comparison and the 4.59.0 rework

Owner request: "The Sagittarius symbol should be on the bow, not floating in front of it. Also, the bows shouldn't have the same designs. If one seems too similar, you need to rework it so that they don't all look similar."

Honesty notes. The models are scripted `bpy` builds (Blender as a Python module, no Blender window, no Blockbench; `tools/blender/forms_legendary_bow_v3.py`, on the existing `forms_legendary_bow.py` / `forms_kit.py` / `bl_lib.py` pipeline). The stills in `docs/previews/bows_v3/` and `docs/heroweapon/form_previews/` are software-rasteriser stills (`tools/relics/preview_mesh.py`), not Cycles renders and not game screenshots. Nothing was seen in a game. No wiki research was needed (no canon lookup); the lore of each bow is the one already in `forms_legendary_bow.py`.

## 1. Sagittarius: the symbol is on the bow now

Before (4.58.0): the zodiac wheel, the archer glyph and the nine-star constellation with its joining lines were separate objects hanging in front of the riser and beside the limb. An object-by-object check of the old builder finds 19 floating objects (`riser_cstar0..8`, `riser_cline*`).

Now:
- the **zodiac wheel** is two sunk medallion discs, one per riser face, that cut into the riser (not a ring hanging on struts); the archer glyph (arrow, bar, tail) is raised on the disc, and the yellow jewel sits in the centre;
- the **nine-star constellation** is laid on both limb faces: each star is a flat four-point gold plate lifted 0.1 unit above the limb skin (`face_pt` follows the limb section), the joining lines are thin raised gold wire along the limb surface, visible on both sides (see `docs/previews/bows_v3/sagittarius_closeup.png`);
- the studs on the bands are skin-only detail (zero quads);
- the horse's head tip and the flowing tail tip are kept; the bow was narrowed to a slim midnight-enamel stave with a double curve so it no longer shares the Hunter's / Frozen Rain outline.

**Connectivity check** (`connectivity()` in `forms_legendary_bow_v3.py`, run after every v3 builder and on every bake; the build fails if it finds a floater): every object is a mesh; two objects touch when their surfaces overlap (BVH tree) or lie closer than 0.8 units (0.006 blocks); a union-find anchored on `riser_core` must reach every object, except `string*` and `arrow*` (the string and the nocked light arrow are free by design). `--selftest` proves it: the Sagittarius passes and a blob injected 3 units off the riser is named. Results of the shipped bakes: Sagittarius 99 objects, floating: none; the other five 59 to 89 objects, floating: none.

## 2. Comparison of the eleven bows (before the rework, 4.58.0)

Profile-silhouette overlap (IoU of the filled profile masks on one canvas, 1.00 = same outline; `similarity_before.txt`): the only pair above 0.6 was base bow ~ curse bow 0.88; next Hunter's ~ Sagittarius 0.56, Frozen Rain ~ Sagittarius 0.55, Frozen Rain ~ Hunter's 0.51. Mean of all 55 pairs 0.387. The numbers only measure the outline; the table below is the human reading of the stills.

| Bow | Silhouette / limbs | Riser | Palette | Ornament family | Verdict |
|---|---|---|---|---|---|
| legendary_bow (Itsuki) | slim symmetric recurve, gold tips | gold-bound grip, yellow jewel | white / gold | two layered feather wings | KEPT (the canon bow) |
| legendary_bow_curse | the same stave, violet, spikes at the tips | same riser with an eye | black / violet | dark feather wings, eye | KEPT, see decision 1 |
| hunters_bow | same medium arc as the others | bone, jewel | bone / brown | barred feathers, fangs | TOO SIMILAR (to Sagittarius / Frozen Rain: same arc, same feather fans) |
| scouts_bow | thin recurve | wire-wound | copper / ash | leaf-wings, rabbit fur | similar family (feather/leaf fans) but a distinct outline; reworked for the ornament family |
| bind_arrow | thorned arch with hooked spider legs | cobweb | purple | legs, web, eight eyes | DISTINCT |
| bird_hunting | pale arch with membrane wings | talons, rabbit foot | white-blue | wing membranes, plumage | TOO SIMILAR (wing-fan family shared with base, Hunter's, Eagle) |
| arrow_shower | iron arch | quiver, fuses | black / gold | peacock fan of arrows | DISTINCT |
| meteor_bow | arch with a comet on the tip | pearls round the stone | purple / star-iron | rock and comet | DISTINCT (purple like bind_arrow, but nothing else shared) |
| frozen_rain | arch with crystal growth | snowflake | ice blue | crystals, icicles, snowflake | TOO SIMILAR (same arc as Hunter's / Sagittarius) |
| eagle_piercer | arch with two layered wings | eagle head | brown / gold | wing plumage | TOO SIMILAR (wing-fan family) |
| sagittarius | slim arch, floating zodiac ring | floating wheel | midnight / gold | horse head, stars | TOO SIMILAR in outline, plus the floating symbol |

The shared problem was one stave shape (a medium symmetric arc) and one ornament idea (fans of feathers or wings) on six of eleven bows.

## 3. What each bow is now (after, 4.59.0)

All keep their ids, lore, guard length, place line, Better Combat and grip files, the solid arrow (`arrow_solid`), and the working `pull1..pull3` meshes. Sizes: all five-to-six forms stay in the bow class (apparent 1.22 to 1.23 blocks, guard length inside BowDisplayGuardTest 0.9 to 1.4).

| Bow | New silhouette | Riser | Palette | Ornament family | Quad lines (rest / pull) | Skin + glow |
|---|---|---|---|---|---|---|
| hunters_bow | deep C arc of two real horns as limbs, tapering to curved horn tips | a carved beast skull with fangs holds the stone | bone / horn / red leather lashing | horn rings, ribs, fangs (no feathers) | 3243 / 3368 | 289 KB |
| scouts_bow | slim straight-handled reflex bow, long thin limbs, tips flicking back | wood grip with a brass spyglass lashed to the riser | olive-green / brass | wire winding, brass collars, a red tail ribbon on the top tip | 3221 / 3340 | 267 KB |
| bird_hunting | straight-limbed compound bow, round cams on both tips, cables | stabiliser, sight, rail, spring | carbon black / orange / steel | cams and cables, no wings | 3444 / 3563 | 309 KB |
| frozen_rain | short heavy prod: thick faceted ice limbs set in steel clamps, shallow arc | teal prismarine core between the clamps | glacier cyan / steel | ice facets, crystal spikes, icicles, a stirrup ring at the lower tip | 3420 / 3560 | 342 KB |
| eagle_piercer | asymmetric raptor bow: long slender upper limb ending in an eagle head, heavy straight lower limb ending in talons | gold-wound, a lance at the riser | dark wood / gold / steel plates | steel scale plates (raptor scales), eagle head and talon, no wing fans | 3330 / 3449 | 285 KB |
| sagittarius | slim S-curved midnight limbs, zodiac discs sunk in the riser, horse head and tail tips | two sunk medallions | midnight enamel / gold | glyph and constellation as raised inlay | 4562 / 4681 | 338 KB |

Reading the three other distinct bows next to them: arrow_shower (quiver fan), bind_arrow (thorns and web), meteor_bow (rock and comet) are unchanged. The silhouette IoU after the rework (`similarity_after.txt`): mean of all pairs 0.258 (was 0.387); highest pair still base ~ curse 0.88, then bird_hunting ~ eagle_piercer 0.46, meteor_bow ~ eagle_piercer 0.46, arrow_shower ~ curse 0.44, arrow_shower ~ base 0.43, hunters_bow ~ frozen_rain 0.42. Everything but the base/curse pair is now at or below 0.46.

### Decisions

1. **legendary_bow_curse kept as the corrupted twin of the base bow.** The four "curse looks" (3.83.0: sword, shield, spear, bow) are by design the same Blender models corrupted by their sin; the bow differs in colour (violet), tip spikes, the eye and the dark wings, but not in stave. Silhouette overlap is 0.88, so it is the one remaining resemblance. It was not reworked because that would break the set of four, and its classic slot already holds the pre-3.83 look (that is the only classic slot the bow can offer). Owner decision: if a different stave is wanted for the curse bow, say so and it gets its own build (the same installer handles it).
2. scouts_bow, bird_hunting and eagle_piercer were reworked as well as hunters_bow and frozen_rain, because they shared the "fan of feathers or wings on a medium arc" idea with the base bow.

## 4. NEW / REPLACED / KEPT

- **NEW**: `tools/blender/forms_legendary_bow_v3.py` (builders, connectivity check, self-test), `forms_legendary_bow_previews.py`, `tools/heroweapon/install_bow_forms_v3.py`, `docs/previews/bows_v3/` (sheet, close-up, before tiles, similarity tables), this document, classic copies (below).
- **REPLACED** (the owner asked for new designs): the rest + pull1..3 meshes and skins of hunters_bow, scouts_bow, bird_hunting, frozen_rain, eagle_piercer, sagittarius; `docs/heroweapon/form_previews/legendary_bow_<form>.png` of the same six.
- **KEPT**: legendary_bow, legendary_bow_curse, bind_arrow, arrow_shower, meteor_bow (meshes untouched); every old builder in `forms_legendary_bow.py` (still runnable, nothing deleted); ids, lore, place lines, Better Combat files, grip lines, engravings (`item/WeaponEngravings.java` keys the item `legendary_bow`, shared by all forms; holy, barrier piercing, swift, elemental boost still fit all of them), the arrow, manifests.

### Rule 2 (keep the old): classic copies

The repo's classic convention covers these forms: `WeaponRenderer.plainMesh` with the setting `classicHeroWeaponModels` reads `weapon_meshes/classic/<id>.txt` and `textures/entity/weapon/classic/<base>.png` / `_glow.png` for ids that start with `legendary_`. The installer copies the shipped 4.58.0 rest, pull1, pull2, pull3 meshes and skin and glow into those folders (6 files per form, 36 in all) and never overwrites an existing classic file. One gap: `readFlatOld` (setting `heroSolidParts=false`, reads `flat_parts_old`) holds the pre-4.55 pull stages of the old designs, so with that setting off the new rest mesh would have shown an old-design pull stage. `WeaponRenderer.java` got a one-line guard (`THIRD_LOOK_BOWS`) so those six ids skip `flat_parts_old` and fall back to the new meshes. This Java edit is not compiled by Gradle here (no Gradle in the session); the source-scan tests that read the file pass.

## 5. Costs (rules 9 and 11)

Client drawing only; no server code, no packet, no tick work, no new config. The worst case per bow in hand is the largest mesh: Sagittarius pull stages 4681 quad lines (budget 5000, body plus glow copies); the others 3243 to 3563. One bow is drawn at a time per player; far players' items are skipped by the existing perf budgets. Skins are 1024 px, 267 to 342 KB with glow (budget 350 KB). Memory per bow: one skin. Detail objects (studs, rivets, etc.) are baked into the skin and cost zero quads.

## 6. Checks run

`register_form_meshes.py`, `bettercombat/inventory.py` (manifest unchanged), `weapon_grip/grip_table.py --write`, `weapon_grip/hold_audit.py --write`. New meshes carry no `gripxyz`, so the finder measured fresh hand points; the hold audit flags no bow. Its two flags (`vassal_book__library_tome`, `vassal_carriage__queens_chariot`: no display block in the model json) are in other items and unrelated to the bows. JUnit (console launcher over `javac` of the real test classes, 95 tests, all passed): GripTableTest, HoldAuditTest, WeaponSizesTest, HeroSolidTest, ClassicVassalFilesTest, BowDisplayGuardTest, HeroAssetsTest, BowFormsFileTest, FormMeshFilesTest, WeaponAttributesTest, ServerSafetyTest, PerfSourceScanTest. The full Gradle build was not run.

## 7. Previews

`docs/previews/bows_v3/all_bows.png` (all 11 bows, labelled, same scale, before row above after row), `docs/previews/bows_v3/sagittarius_closeup.png` (whole, riser, limb), `docs/heroweapon/form_previews/legendary_bow_<form>.png` for the six rebuilt bows. Software-rasteriser stills.
