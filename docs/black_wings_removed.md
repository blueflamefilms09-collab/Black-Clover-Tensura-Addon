# Black Wings was removed

Why: the owner asked for the Black Wings skill (`anime_multiverse:black_wings`, the Ultimate that reflected melee and projectiles, took no fall damage and glided) to go. CLAUDE.md rule 2 allows a removal the owner asks for; old worlds must still load, so the id stays (section 2). Same method as the 3.44.0 removals (`docs/skill_books_removed.md`, `docs/skills/rebuild.md`).

## 1. What was removed

* The skill class `skill/BlackWingsSkill.java` and its registration `NUSkills.BLACK_WINGS`.
* `skill/VectorManipulationSkill.java` (the abstract reflection engine; Black Wings was its only subclass) and `skill/rules/VectorRules.java` (its numbers), with the tests of those rules (`UltimateRulesTest.blackWingsReflectEverythingAndGlideOnlyInTheAir`, the `VectorRules` line of `UltimateClassTest`).
* The sender `SkillAnimeFx.wings(...)` (the 1 packet / 40 ticks glide effect) and its mention in the class doc; `NUSkills`, `SkillEvolution`, `UltimateClassSelfTests` (the LORD class check), `SkillAnimeVfxAssetTest` (WIRED list, glide assertion), `SkillAssetsTest`, `UltimateClassSourceScanTest` and `docs/ultimate_classes.md` no longer name it.
* Lang: `anime_multiverse.skill.black_wings` and `.description` in `lang/en_us.json` and `tools/skills/lang_fragment.json`.
* Soul Forge: `data/anime_multiverse/avatar_skills/black_wings.json` and its row in `tools/avatar/forge_tables.py` (the forge would have offered an inert stub; `ForgeSkillDataTest` also requires every forge id to be live in `NUSkills`).
* Docs: the Black Wings section of `docs/skills/rebuild.md`, the table row and the glide notes of `docs/vfx_remake/skill_anime.md`, the README line.

## 2. What stays, and why

* **The registry id `anime_multiverse:black_wings`**, appended LAST to `LegacyIds.REMOVED_SKILLS` (now seven ids), so `LegacySkills` registers it as an inert hidden `LegacySkill` stub, after the six of 3.44.0 (registry order of everything else is unchanged). A world that saved it loads; `LegacyMigration` forgets the stub at the next login or respawn. Nothing grants it any more.
* **The icon** `textures/skill/black_wings.png` and its entry in `tools/skills/gen_skill_icons.py` (as with the other removed skills).
* **`VfxShape.SKILL_WINGS`, its `SkillAnimeLayer` drawing, the `skillanime_wing` texture, the generator and the preview scene `skill_black_wings`**: enum entries are append-only and the shape ordinals after it must not move (`SkillAnimeVfxAssetTest` pins them); nothing sends the shape now, the test asserts that. Cost: zero (dead client code, never reached).
* **The item `albedos_black_wings_brooch`** (a Rare neck relic in `relics/wave/Wave37`, its models, worn model, curios tag, franchise map, lang): it is an item and never granted the skill (passives and the "Wings Spread" act only), so it is untouched.
* **The particle theme `skill.blackwings`** (`FxThemes.SKILL_BLACKWINGS`, `particle_themes.json`, `tools/particles/map.json` and `gen_themes.py`): generated files shared with the particle tests; the theme is now unused.
* `relics_before_wave101.txt`, `existing_before.txt`, `weapon_sizes_before.json`, `CHANGELOG.md`, `docs/releases/*`: history, untouched.

## 3. Cost and risk

Zero new runtime cost. The removal deletes a per-tick glide (one velocity write per tick while toggled on) and one effect packet per 40 ticks of glide. The only new work is none: the stub is a registry entry.

Risks: a player who still holds the skill loses it at next login (replaced by nothing); no Gradle was run here, CI is the compiler (watch for a missed reference to `VectorRules` / `BlackWingsSkill`; the repo grep is clean).

## 4. New, replaced, kept

* NEW: this document, the `black_wings` entry in `LegacyIds.REMOVED_SKILLS`.
* REPLACED: `RemovedSkillsSelfTests` (now asserts Black Wings is an inert stub, not live), `LegacyIdsTest` and `SkillAssetsTest` (seven removed ids).
* KEPT: everything in section 2.
