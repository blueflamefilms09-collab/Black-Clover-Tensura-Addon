# Celestial Gate, keys, Spirit King and the Requip skill removed (3.36.0)

Owner request: after the skill books went, also remove the **Celestial Gate, the keys and the King**, and the **Erza Requip skill**.
This continues `docs/skill_books_removed.md` and `docs/fairy_tail_celestial_books_removed.md`.

## Removed

| What | Details |
|---|---|
| Celestial Gate, Spirit World and Spirit King | the Gate entity / ritual, the three King phases (`celestial_king_p1..p3`), the Spirit World dimension and dimension type, the 20 spirit and gate geo files, animations, textures (entity and glow), tools under `tools/celestial/**`, `docs/celestial_keys.md` |
| Keys and the Spirit King gate key | the golden / silver keys, the `spirit_king_gate_key` item and its models, the key collection logic and Curios slot data |
| Requip skill `nusmp:erza_requip` | the skill, the armoury, loadouts, blade, barrage, strike, partial armour and finishers, the six Requip weapons with their meshes, models, weapon textures and attributes, the Erza armour sets (`erza_*` entity and armour textures), `docs/erza_requip.md`, `docs/requip_magic.md`, `docs/previews/erza/**`, the `/requip` command |

About 1,250 files in total (mostly textures, geo files and animations), roughly 67,000 lines.

## Kept on purpose (save compatibility, append-only rules)

* `core/magic/profile/RetiredAttachments`: the attachment ids `nusmp:celestial` (key masks) and `nusmp:requip_armoury` (the pocket armoury) stay registered as **inert pass-through holders**. Nothing reads or writes them any more, but a world and its players load without an unknown-attachment error and the raw data (including any items stored in an armoury) survives a save, so it can be restored if the owner ever brings the feature back.
* `Aura.REQUIP_KNIGHT` and the other aura enum entries (ordinals are sent over the network, enums are append-only).
* `particle/fx/FxThemes.SKILL_REQUIP` and its row in the particle theme files (unused theme key).
* `skilltree/unique/LegacyIds`: the retired skill ids (`erza_requip_knight`, `requip_gunslinger`, `requip_titania`, `celestial_spirit_magic`, ...) so `LegacyMigration` clears them from players at login and respawn.
* The relic slot name `celestial_key` in `relics/RelicWear` comments and Curios slot data that other relics share.
* `fairytail/storm/**` (Storm Magic, the Stormcaller Staff): an independent feature, untouched.

## What a player sees

* The Celestial Gate, its spirits and the King no longer exist; nothing generates them. Chunks that already contain old gate blocks or entities load as unknown and the game drops them.
* Anyone who had the Requip skill loses it at the next login (`LegacyMigration`); their stored armoury items stay in the retired attachment, unreachable until restored by hand.
* Spirit King recognition (which needed Celestial mastery) is gone with the King.

## Cost

Only code and assets were removed: no new tick work, packets or draw cost. The worst case is lower than before (the Requip per-player armoury tick, the spirit summon AI and the Gate arena checks are gone).

## Not seen in a game

This change was checked by typechecking the stubs and the tests that read repo files; nothing was started in a game. CI runs the full build and the server smoke test.
