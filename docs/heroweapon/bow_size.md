# Bigger bows in the hand

Owner request: "the hero bow or whatever should be bigger in the player's hand as it is tiny, and that applies to all the bows in my mod. Some of them seem like they're really tiny in the player's hand, in third person and in first person."

**Honesty notes.** Nothing here was seen in a game. The numbers come from the mesh files and the model json files; the pictures in `docs/previews/bows_bigger/` are software-rasteriser mock-ups (`tools/relics/bow_size.py --preview`, the grip sheets' rasteriser) with the item model's display block and the game's grip rule, not screenshots. The Java change was compiled only as far as `javac` against the jars in the Gradle cache allows (the third-party mana library is not in the cache, so a full compile was not possible; the new lines raised no error; the two errors `WeaponRenderer` shows there are old, from the stub jars). The pure classes and every test ran for real. No model was made or changed (no Blender, no Blockbench): this is a size change in the renderer.

## Root cause

* 1.72.0 (`tools/relics/bow_vanilla.py`, `BOW_LENGTH = 1.2`) put every bow mesh at the size of vanilla's bow on purpose (it had filled the screen at 1.8 to 2.6 blocks) and 1.63.0 gave every bow vanilla's Bow display block (scale **0.9** third person, **0.68** first person). So a bow of the mod is drawn exactly as small as a vanilla bow: 1.2 blocks of mesh, **1.08 blocks** in a third-person hand and **0.82** in a first-person hand. Next to a "hero" weapon that reads as tiny. The mesh forms of the Legendary Bow were a little longer (1.35 to 1.38 mesh, 1.22 to 1.24 apparent) and the curse look 1.20.
* It is one shared cause (the vanilla-size rule and the vanilla display block), not a per-bow mistake: the place lines are all 1.2 (or the same 1.35 to 1.39 for the forms), the display scales are all 0.9 / 0.68, `OversizeGuard` and `WeaponNormaliser` never fire on them (`orient manual`, apparent length far below the 1.5 limit), and the grip/hold audits are clean. So the fix is at the shared point of drawing, not in 21 meshes.

## What changed

`visual/sizes/BowSize` (pure) holds the size table; `WeaponRenderer.bowFactor` multiplies a held bow by its factor about the point the hand holds: third person the hand point (`GripState.shift`'s `t`), first person where the grip lands (the same pivot the oversize guard already shrinks about). The meshes, place lines, skins, grip table and model jsons are not touched, so the old size is one setting away.

Target apparent lengths (blocks, mesh length times the display scale):

| tier | bows | third person | band tested | first person (comes out) |
|---|---|---|---|---|
| hero | `legendary_bow`, `legendary_bow_curse` | **1.50** | 1.3 to 1.6 | 1.13 |
| form (vassal) | the nine `legendary_bow__*` forms | **1.40** | 1.2 to 1.5 | 1.06 |
| relic | Arash's Stella, Galewing, Aeon, Thornmother's Briar, Greed bow, Divine Mercy, The Dragon's Spirit, The Reaver, Embrasure, Spirit of Boreas | **1.35** | 1.2 to 1.5 | 1.02 |

Vanilla's bow is 1.08 / 0.82. The first person is capped at 1.2 apparent blocks whatever the setting is (`BowSize.FP_MAX_APPARENT`), so a bow never reaches the camera or fills the screen; the hold audit with the new size (`bow_size.py --hold`) shows no CAMERA, LOW, ARM, BODY or FLOAT flag on any of the 81 bow meshes (rest and draw stages), over the 21 rest meshes the nearest point to the camera is 0.35 to 0.51 blocks away (the audit's margin is 0.16), the forearm reach 0.25 to 0.31 (limit 0.34), the nearest surface point to the hand at most 0.085 (limit 0.10). The audit does not judge draw stages for arm and torso (the arm is raised), as before.

The factor of a mesh is worked out **once** from the length of the bow's rest mesh (tip to tip over layers 0 to 2), so `_pull1..3` use the very same factor as the rest mesh and the draw never jumps in size; the draw-stage meshes have the very same tip-to-tip length as their rest mesh (checked for all 21 bows). Crossbows are not bows and are untouched (1.27 to 1.33 mesh already, class crossbow 1.0 to 1.2).

### Measurement table (before and after)

Third person = mesh length x 0.9 x factor; first person = mesh length x 0.68 x factor. "Draw stages" are the `_pull1..3` meshes' lengths (blocks, before the factor); all the stages use the rest mesh's factor.

| bow | tier | mesh (blocks) | 3rd person before | 3rd person after | 1st person before | 1st person after | factor 3rd / 1st | draw stages (mesh) |
|---|---|---|---|---|---|---|---|---|
| `legendary_bow` | hero | 1.200 | 1.08 | **1.50** | 0.82 | **1.13** | x1.39 / x1.39 | 1.200 to 1.200 |
| `legendary_bow_curse` | hero | 1.202 | 1.08 | **1.50** | 0.82 | **1.13** | x1.39 / x1.39 | 1.202 to 1.202 |
| `legendary_bow__arrow_shower` | form | 1.361 | 1.22 | **1.40** | 0.93 | **1.06** | x1.14 / x1.14 | 1.361 to 1.361 |
| `legendary_bow__bind_arrow` | form | 1.375 | 1.24 | **1.40** | 0.94 | **1.06** | x1.13 / x1.13 | 1.375 to 1.375 |
| `legendary_bow__bird_hunting` | form | 1.381 | 1.24 | **1.40** | 0.94 | **1.06** | x1.13 / x1.13 | 1.381 to 1.381 |
| `legendary_bow__eagle_piercer` | form | 1.413 | 1.27 | **1.40** | 0.96 | **1.06** | x1.10 / x1.10 | 1.413 to 1.413 |
| `legendary_bow__frozen_rain` | form | 1.407 | 1.27 | **1.40** | 0.96 | **1.06** | x1.11 / x1.11 | 1.407 to 1.407 |
| `legendary_bow__hunters_bow` | form | 1.368 | 1.23 | **1.40** | 0.93 | **1.06** | x1.14 / x1.14 | 1.368 to 1.368 |
| `legendary_bow__meteor_bow` | form | 1.397 | 1.26 | **1.40** | 0.95 | **1.06** | x1.11 / x1.11 | 1.397 to 1.397 |
| `legendary_bow__sagittarius` | form | 1.383 | 1.24 | **1.40** | 0.94 | **1.06** | x1.13 / x1.13 | 1.383 to 1.383 |
| `legendary_bow__scouts_bow` | form | 1.359 | 1.22 | **1.40** | 0.92 | **1.06** | x1.14 / x1.14 | 1.359 to 1.359 |
| `arashs_stella_bow` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `black_sword_greed_bow` | relic | 1.390 | 1.25 | **1.35** | 0.95 | **1.02** | x1.08 / x1.08 | 1.390 to 1.390 |
| `divine_mercy` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `embrasure` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `silver_eagles_galewing_bow` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `spirit_of_boreas` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `starforged_aeon_bow` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `the_dragons_spirit` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `the_reaver` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |
| `thornmothers_briar_bow` | relic | 1.200 | 1.08 | **1.35** | 0.82 | **1.02** | x1.25 / x1.25 | 1.200 to 1.200 |

Not enlarged on purpose: the Djinn bows `djinn_weapon_barbatos`, `djinn_weapon_sitri` and their dark forms (bow-class grip entries) are 2.7 to 3.2 blocks long in the hand already; `BowSizeTest.aBowLeftOutOfTheTableIsAlreadyBig` keeps that true. The Genesis (upgraded, `weapon_meshes_vu`) Starforged Aeon Bow is drawn by `VuRenderer` with its own 1.6 mesh (1.44 third person, 1.09 first person, already inside the relic band) and is not scaled again; with `visual.upgrade` off the old 1.2 mesh is drawn and scaled to 1.35.

## Settings (CLIENT config, `config/nusmp-flatprops-client.toml`, section `[visual]`, next to `flatProps` and `heroSolidParts`; no new file, so `ConfigFileNamesTest` is unaffected)

| key | default | meaning |
|---|---|---|
| `visual.bigBows` | `true` | the bigger bows. `false` = the classic path: factor exactly 1, the sizes of before, number for number (`BowSizeTest.theClassicPathGivesBackTheSizesOfBefore`) |
| `visual.bowScale` | `1.0` (0.5 to 2.0) | a multiplier on top, to tune without a rebuild; ignored while `bigBows` is off; in first person the 1.2 cap still applies |

They only change how a bow looks (client config, mirrored into the pure `BowSize` switch like `heroSolidParts`); no packet, no server state, nothing keyed by player. The dedicated server never reads them.

## NEW / REPLACED / KEPT

* **NEW**: `visual/sizes/BowSize.java` (table, factor, switches), `BowSizeSelfTests.java` (self-test `visual.bow_size`), the two config keys, `WeaponRenderer.bowFactor` + `extentLength` (+ two fields in `Mesh`), `BowSizeTest`, a test in `BowDisplayGuardTest`, `tools/relics/bow_size.py` (table, hold check, previews), an optional hook `BOW_K` in `tools/weapon_grip/hold_audit.py` (None by default: the golden is byte-identical), this document, `docs/previews/bows_bigger/`.
* **REPLACED**: nothing was removed. The in-hand size of every bow of the mod is replaced by the new default size; the old size stays reachable (`visual.bigBows = false`).
* **KEPT**: every mesh, place line, skin, glow, grip line, display block, Better Combat file, engraving, the classic / `flat_parts_old` / `shared_stave` copies (the factor applies to those looks too, by the same ids), `BowDisplayGuardTest`'s mesh-length guard (0.9 to 1.4, the meshes did not change), `bow_vanilla.py` and its 1.2 length, `grip_golden.json` and `hold_golden.json` (regenerated: no change), arrows, shots, rain and damage logic (not touched), shields, the Time Magic VFX.

## Tests

* `BowSizeTest` (new, 9): every bow mesh of the mod (class bow, draw stages, forms, any file named `*bow*`) is in the table, so a bow added later without a size fails the build (also tried with a copied mesh: it fails and names the file); every table entry has its mesh and is class bow; a bow-class grip entry left out of the table must already be big; every bow mesh reads inside its tier band in third person and 0.9 to 1.2 in first person, draw stages within 6 percent of the rest mesh (they are identical today); the Genesis bow is inside its band; the classic path gives back `mesh x display scale` exactly; scale 0.5 / 2.0 move third person exactly and never pass the first-person cap; clamps; every bow's model json carries 0.9 / 0.68 (what the arithmetic assumes).
* `BowDisplayGuardTest` (one test added): every bow-class item has a tier and reads at least 1.2 / 0.9 in the hand in both persons; the old guards stay.
* Self-test `visual.bow_size` (server boot): table vs `weapon_classes.json`, bands, cap, classic factor 1. **Add `visual.bow_size` to the smoke list** (I did not touch `smoke.yml`).
* Run (console launcher via `javac` + the JUnit launcher API): GripTableTest, HoldAuditTest, HeroSolidTest, PerfSourceScanTest, WeaponClassesTest, WeaponMeshesTest, RangedStageTest, HeroAssetsTest, BowFormsFileTest, ServerSafetyTest, ConfigFileNamesTest, BowSizeTest, BowDisplayGuardTest, WeaponSizesTest, WeaponAttributesTest: 115 tests, all passed. `WeaponSizesTest` covers daggers and katanas only (`weapon_sizes.json`); the bow bands live in `BowSizeTest`. The grip and hold goldens were regenerated with `grip_table.py --write` and `hold_audit.py --write` (no change; the known NONE note `vassal_carriage__queens_chariot` stays). The full Gradle build and the server smoke test were not run.

## Cost (rule 11)

Server: none (client config, client drawing). Network: none. Client: one table lookup and one mesh scan the first time a bow mesh is drawn (cached in the mesh, redone on a resource reload or a setting switch), then one field read and one scale per frame for each held bow, no allocation. Quads per frame do not change (the same mesh, drawn bigger): the worst case stays the Sagittarius draw stage at 4681 quad lines. Far players' bows are skipped by the existing budgets as before.

## Caveats and decisions for the owner

* Not seen in a game: how the 1.5-block hero bow sits against the arm, the head and the second hand in the draw pose (the draw pose raises the arm; the audit judges the idle arm, as it always did) and how it reads in first person next to the screen's edge. If a bow clips the head in third person, lower `visual.bowScale` a little (0.9) or ask for a smaller tier.
* **Decision: the tiers.** 1.50 / 1.40 / 1.35 are my reading of "hero 1.3 to 1.6, vassal and relic 1.2 to 1.5"; they are one number each in `BowSize.Tier` (and the bands in `BowSizeTest`). Default `bowScale` stays 1.0; if the bows are still too small, 1.15 to 1.25 is possible in third person, but first person is capped at 1.2 whatever you choose.
* Third person and first person get the same factor, so the first-person bow grows by the same share (0.82 to 1.13 for the hero bow, vanilla's bow stays 0.82).
* The Genesis Aeon bow (1.44 / 1.09) is a little larger than a plain relic bow (1.35 / 1.02) because its upgraded mesh is longer; left alone.
