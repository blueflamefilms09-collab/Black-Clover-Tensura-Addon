# Fairy Tail, Celestial and Requip skill books removed

Owner request: every skill book item is deleted from the mod, and the skills that existed only through those books go with them.
This note covers the books outside the Skill Tree (the generic `nusmp:skill_book` / `sp_book` are handled with `skilltree/**`).

## Removed

| What | Details |
|---|---|
| Item `nusmp:fairy_tail_skill_book` | `fairytail/FtBookItem`, `FairyTailSystem.BOOK`, model, texture, lang, creative-tab entries (7 stacks), `data/nusmp/loot_table/chests/fairy_tail_skill_book.json`, its 12 % pool in `dungeon_relic_vault.json` (the rest of the vault table is unchanged) |
| Item `nusmp:celestial_spirit_magic_book` | `celestial/CelestialSkillBookItem`, `CelestialItems.SKILL_BOOK`, 3D mesh, skins, models, icon, weapon class / franchise / grip / Better Combat rows, `/multiverse celestial give <player> book` |
| Item `nusmp:titanias_requip` | `requip/TitaniasRequipItem`, `RequipItems.TITANIAS_REQUIP`, the dungeon-loot injection (`RequipSystem.LOOT`, `onLoot`), `/requip book`, model, texture, lang, franchise rows |
| The 8 Fairy Tail skills | `fire_dragon_slayer`, `fire_dragon_king`, `ice_make`, `sky_dragon_slayer`, `lightning_dragon_slayer`, `iron_dragon_slayer`, `crash`, `water_magic`: the whole `fairytail` package except `fairytail/storm/**` (FtLine, FtSkill, FtCast, FtWorld, FtRules, FtMove, FtKind, FtElement, FtForms, FairyTailSystem, FairyTailCommand, FairyTailSelfTests), the Fire Dragon Slayer to King evolution, `client/aura/FairyTailAura`, the 8 skill icons, the 70 form geo files, 140 form textures, 10 form animations and 20 form shell textures (`ft_*`), their lang keys (`tools/lang/fairy_tail_skills.json`), `tools/gen_fairy_tail_*.py`, `docs/fairy_tail_skills.md`, the `skill.fairytail` self-test prefix in `.github/workflows/smoke.yml` |
| The skill `nusmp:celestial_spirit_magic` | `skill/CelestialSpiritMagicSkill`, its `NUSkills` registration, its icon, its lang keys (`nusmp.skill.celestial_spirit_magic*`, `nusmp.skill.mode.celestial.*`, `nusmp.skill.msg.celestial.*`) |

Tests deleted: `fairytail/FairyTailAssetsTest`, `fairytail/FtRulesTest`. Tests edited: `celestial/CelestialAssetsTest` (no book, no skill lang), `requip/ArmourAssetsTest`, `skill/ErzaSelfTests` (no book asset checks).

## Admin commands

There is no learn logic left to keep for Fairy Tail or Celestial: the skills themselves are gone, so `/multiverse fairytail ...` was removed with them and `/multiverse celestial give` no longer accepts `book`.
`/requip teach <player>` still teaches Requip: The Knight directly (no EP gate); `/requip book` is gone.

## Kept on purpose

* `Aura.FAIRY_TAIL` (enum entry, unused): the aura ordinal is sent over the network, entries are append-only.
* The Celestial **keys, Gate, Spirit World, Spirit King, sceptre, props and `skill/CelestialArts`**: shared with the king and `CelestialProps`, and the Gate works without the skill. Without the skill nobody can summon the spirits, wear a Star Dress or use the King's Blessing / Summon modes; the King's recognition reward needs skill mastery, which is now always 0 (`GateRitual.mastery`), so only the challenge route pays out.
* `fairytail/storm/**` (Storm Magic, the Stormcaller Staff): an independent feature.
* The Requip **skill** `nusmp:erza_requip`, its armoury, loadouts, six weapons and `/requip teach`: a 5,000-line subsystem with its own weapons and assets that the owner did not name; only its book was removed. Removing it too is a separate, larger step.
* The 4 data component ids and the attachment ids: untouched.

## Save compatibility

A player who saved one of the removed skills keeps a skill id the registry no longer knows; as with the retired merged magics (`core/magic/profile/RetiredMagic`), the skill system drops or logs it on load. An item stack of a removed book in an old chest or inventory loads as an unknown item and is dropped by Minecraft.

## Cost

Only code and assets were removed: no new tick work, packets or draw cost. Worst case after the change is lower than before (one 100-tick player check and the form aura layer are gone).
