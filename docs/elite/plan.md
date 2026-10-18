> **1.88.0: removed.** At the owner's request every area of this plan except the **Hub economy** (`elite/hub`, `docs/elite/hub.md`) was taken out of the mod: races, records, nations, forge, skills, world and items were deleted (council, seasons and voice were never merged). The text below is the historical plan.

# 1.83.0+: the Hub economy brief and the EliteTensuraAddon feature inventory

Two uploads of the owner: (1) a brief for a Tensura-coin economy, Hub NPCs and quests, a cyclic (anti-dead-end) dungeon generator and the skill-line engine (already built in 1.82.0); (2) `elitetensuraaddon_specification_prompt.md`, a prompt plus a "Pre-Extracted Feature Inventory" of another addon (skills, races, forge, nations and war, Walpurgis council, seasons, records and titles, calamities and the magicule world, items and plushies, voice announcers). "Add this stuff" = build every item of the inventory into this mod. The names below are the inventory's; the implementations are the mod's own (the original source is not available, so numbers and rules are designed here and listed honestly as such).

## Comparison with what the mod already has (CLAUDE.md rule 5)
| Area | Already in the mod | This job |
|---|---|---|
| Currency | Magic Stones (1.22.0) drop and are spent in the dungeon; the Black Market sells for Tensura gold/silver coins | **REPLACEMENT** (owner: "The generic Magic Stone economy is replaced"): Tensura coins are the currency of the dungeon, quests and shops. Magic Stone items, ids and saves are KEPT (config `economy.currency`, default `tensura_coins`) |
| Hub NPCs, quests, Dungeon Map | Hub (1.55), System floor quests (1.55), Merchant Oases (safe rooms) | **NEW**: QuestGiver and Merchant NPCs, shop GUI, quest data and overlay, the Dungeon Map item |
| WFC dead ends | Jigsaw/room generator, dead-end rooms exist | **EXTENDS** the generator with a cyclic mode (loops, fewer dead ends), old mode kept behind a config |
| Skills | 38 rebuilt skills (1.77.0), 283 skill lines | **NEW** skills named in the inventory that do not exist yet; existing ones (Gravity Well, Scavenger, Hephaestus...) are compared first and kept |
| Races | Tensura races + grimoire races | **NEW**: Saiyan tiers, Primordial race |
| Forge | none | **NEW** |
| Nations and war | none (squads and standings exist for the Multiverse) | **NEW** |
| Walpurgis and council | none | **NEW** (after nations) |
| Chronicles, seasons, legacy store | none | **NEW** (after nations) |
| Records, achievements, titles | Tensura's own titles only | **NEW** engine on top |
| Calamities, bosses, magicule world | Lich (existing mob), Basilisk | **NEW** |
| Items, crates, plushies | relics, Black Clover/Shield Hero weapons | **NEW** |
| Voice/TTS | none | **NEW**, opt-in and OFF by default (it sends text to a third party) |

## Rules every builder follows
* Package `com.newuniverse.nusmp.elite.<area>`; ONE registry class with `init(IEventBus, ModContainer)`; the single wiring line goes into `elite/EliteSystem.java` in your own section. Each subsystem has a master switch `enabled` in its own server config (`nusmp-elite-<area>-server.toml`) and does nothing when it is off.
* Cross-system reads go through `elite/api/EliteHooks.java` (late-bound, safe defaults). Never import another subsystem's classes.
* Data-driven where it makes sense (JSON under `data/nusmp/elite/<area>/`), with pure Minecraft-free rule classes and JUnit tests, an in-game self-test registered like the other `SelfTests` (key prefix `elite.<area>.`), a doc `docs/elite/<area>.md` (the WHY), a lang fragment `tools/elite/<area>_lang.json` (do not edit `en_us.json`), no version/CHANGELOG/README edits, no push.
* Owner rules: weapons carry Tensura engravings (`item/WeaponEngravings.java`) and are real 3D models (`tools/gen_weapon_models.py`, `client/WeaponRenderer.java`); only Light/Dark Rukh particles from abilities; never touch the Time Magic VFX; keep old things, add new; commit often.

(1.86.0 pending: nations merged, not yet released.)
