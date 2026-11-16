# The Faith skills, removed

Removed on the owner's word ("remove the faiths i added"; CLAUDE.md rule 2, explicit replacement/removal).

## What was removed

- The Faith skill (`anime_multiverse:faith`: Bestow, Pray, Check Faith, Recruit, Gift) and its Ultimate Goddess of Faith (`anime_multiverse:goddess_of_faith`: Blessing, Miracle), the worship model (worshippers, offerings, chosen gift) and the `/faith ...` and `/anime_multiverse faith ...` commands.
- The eight Faith gods (Femto, Lucifero, Luminus, Mavis, Ill Ilah, Super Saiyan God, Ra, Aqua): their 16 skills (`faith_of_<id>` Unique and its god form), the ritual `/faith ascend`, `/faith gods`, `/anime_multiverse faithgod give`, the canon pass.
- Code: `FaithSkill`, `GoddessOfFaithSkill`, `FaithWorship`, `FaithGods`, `FaithGodSkill`, `FaithGodMoves`, `FaithGodCanonMoves`, `FaithSelfTests`, `FaithGodSelfTests`, `FaithRules`, `FaithWorshipRules`, `FaithGodRules`, `FaithCommand`, the Faith lines of `NUSkills`, `SkillEvolution` (Faith -> Goddess of Faith and the god evolutions), `SkillUtil` (`spendOffering`), `RaceConnectionChecker` (Faith no longer counts as a Falna), `UltimateClassSelfTests` (Goddess of Faith was a KING).
- Settings: the whole `faith.*` section (`bestowableSkills`, `bestowFaithCost`, `bestowCooldown`, `prayMagiculeCost`, `prayCooldown`, `worshipEnabled`, `maxWorshippers`, `inviteSeconds`, `recruitCooldown`, `recruitRange`, `offeringRange`, `offeringMagiculeCost`, `offeringCooldown`, `worshipTrickleTicks`, `worshipperGiftDiscount`) and the whole `faithGods.*` section (`enabled`, `ascendFaithCost`, `minWorshippers`, `minOffered`, `devotionPercent`, `powerPercent`, `evolutionEpGatePercent`, `maxGodLines`, `ascendCooldown`, `canon`). A config file that still holds them loads fine; the keys are ignored. `allFictionCanon` was written inside the `faithGods` block by mistake (4.7.0); it is now back under `ultimates_fiction_god`, where docs/all_fiction_rework.md says it is (default true, so only a server that had switched it off must set it again).
- Data and text: `data/anime_multiverse/avatar_skills/goddess_of_faith.json` (Soul Forge row), the Faith lang keys of `en_us.json` and `tools/skills/lang_fragment.json`, `tools/lang/faith_fix.json`, `faith_gods.json`, `faith_gods_canon.json`, `tools/faith_gods/`, the sixteen god icons (`textures/skill/faith_of_*.png` and the eight god-form icons), `docs/faith_gods.md`, `docs/faith_fix.md`, and the Faith rows of the other docs.
- Tests: `FaithRulesTest`, `FaithWorshipRulesTest`, `FaithGodRulesTest`, `FaithGodCanonRulesTest`, `FaithGodCanonSourceScanTest` were deleted; `SkillAssetsTest`, `SkillAnimeVfxAssetTest`, `UltimateClassSourceScanTest`, `SkillIconsV3Test` and `LegacyIdsTest` were updated.

## Kept for saves (inert placeholders)

The eighteen ids are appended LAST to `LegacyIds.REMOVED_SKILLS`, so `LegacySkills` registers each as an inert, hidden `LegacySkill` (same mechanism as Black Wings, docs/black_wings_removed.md): `faith`, `goddess_of_faith`, `faith_of_femto`, `femto_god_of_incarnation`, `faith_of_lucifero`, `lucifero_god_of_devils`, `faith_of_luminus`, `luminus_goddess_of_light`, `faith_of_mavis`, `mavis_goddess_of_fairy_law`, `faith_of_ill_ilah`, `ill_ilah_god_of_rukh`, `faith_of_super_saiyan_god`, `ssj_blue_god_of_divine_ki`, `faith_of_ra`, `ra_god_of_the_sun`, `faith_of_aqua`, `aqua_goddess_of_water`. A player whose saved skill list names one of them still loads; `LegacyMigration` forgets the stub at the next login or respawn. The stubs do nothing, have no mode, keybind or command, and are never taught. The self-test `skill.removed_ultimates` checks all of them.

The icons of `faith` and `goddess_of_faith` and their entries in the icon index (`skill_icons.json`, `tools/skills/icons_v3.txt`) stay, like the icons of the other removed skills (their ids are still registered, so the `skillicons.index` self-test keeps passing). The `FAITH` and `GODDESS` sigil styles of `SKILL_SIGIL` stay in the VFX layer (shared effect code; nothing sends them any more).

## What was restored

Nothing. Faith is one of the mod's first skills (it is in the first commit) and Goddess of Faith was only its evolution target; the Faith skills never replaced an older skill, so there is no earlier state to bring back. The other evolution paths are unchanged.

## Cost

Removal only: no new work per tick, no new packets, no new entities.
