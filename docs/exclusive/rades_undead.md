# Rades Spirito's undead: Soul Zombies, the five numbered undead, and the character's name on the Unique Skill

Nothing here was run in a game. Type-checked against the API stubs (only the known stub artefacts remain), pure logic and data unit-tested, models previewed headless with `tools/render_preview/preview.py` (images in `docs/previews/rades_undead/`, which I looked at), a CI self-test `magic.rades_undead` is registered (it needs a server to run).

The owner's request, in three parts:

1. Basic Soul Corpse Magic and Rades's Reanimation / True Soul Corpse summon **zombies**, not the things they used to; those zombies **never burn in daylight**.
2. Rades Spirito's exclusive spells No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl, No.0 Michael Caesal summon **custom-looking undead** that follow the series art.
3. On every character's grimoire the **Unique Skill says the character's name** as well as the magic type.

## NEW / REPLACED / KEPT

| what | status |
|---|---|
| `SoulZombieEntity` (`nusmp:soul_zombie`, a thin subclass of the vanilla zombie), `SoulZombies` (the one door that summons), `SoulZombieRules` (numbers) | NEW |
| Marionette, Necropolis, Legion (generic Soul Corpse pages) raise zombies instead of puppet props | REPLACED, behind `corpse.zombiesInsteadOfPuppets` (default true; false = the old puppet props, code and props untouched) |
| Reanimation (`corpse_reanimate`) and True Soul Corpse Magic's re-raising of fallen pets raise zombies instead of the dead creature's own kind | REPLACED, same flag (false = the old raise of the creature's kind) |
| Corpse Swap decoy and the Grave Field / Legion hands (`SOUL_CORPSE_2`) | KEPT: a decoy and hands are not fighting summons |
| Page ids `corpse_jimmy`, `corpse_alfredo`, `corpse_carl`, `true_soul_corpse`, `corpse_reanimate` and their positions (modes 5..9) | KEPT |
| Page display names "No.4 - Jimmy", "No.2 - Alfredo", "No.1 - Carl" (were "Soul Corpse: Jimmy" ...) | REPLACED (names only) |
| Pages `corpse_david` (No.3) and `corpse_michael_caesal` (No.0) | NEW, appended as the last two family pages of the book (after `corpse_legion`), see the note below |
| `NamedCorpses` rows of Jimmy, Alfredo, Carl (stats, ids, registry ids `nusmp:corpse_*`) | KEPT |
| `NamedCorpses` rows DAVID and MICHAEL_CAESAL, entity types `nusmp:corpse_david`, `nusmp:corpse_michael_caesal` | NEW (enum appended) |
| Looks of Jimmy, Alfredo, Carl | REPLACED by `RadesUndeadRenderer` (new models `rades_*`); old models, textures, animations and `NamedCorpseRenderer` stay on disk; client switch `corpse.classicLooks` (`nusmp-corpse-client.toml`, default false) brings the old look back |
| Models, animations, 4 stage skins + glow maps of the five | NEW (`tools/corpse/gen_rades_undead.py`) |
| Signature moves: Alfredo soul barrier, David ground slam, Michael Caesal soul barrage | NEW |
| Undead flag (`minecraft:undead`, `minecraft:sensitive_to_smite`) for all six bodies | NEW (data tags that only add) |
| Locks: the five numbered pages open only in Rades's grimoire (`CharacterSpells`) | NEW for jimmy / alfredo / carl (they were open to every Soul Corpse grimoire, the 1.43 doc said "suggested"), NEW for the two new pages |
| Unique Skill name with the character (`CanonNaming`, `GrimoireSkillInstance`, `CanonSkillSync`) | NEW |

Note on page order: the owner asked for the new pages "at the end of the Rades page list". The grimoire numbers pages by position (the unlock bitmask and the mastery gate use the index), and putting them right after `true_soul_corpse` would have moved Marionette, Swap and Legion by two. So they are defined in `SoulCorpseExt.latePages()` (the end of Rades's list) and appended after Legion in `SoulCorpseBook.familyList()`. `ArmourPolicyTest` needed the two ids listed in `new_pages` of the SOUL_CORPSE entry of `armour_policy.json` (one additive edit).

## 1. Soul Zombies

* A real zombie (the vanilla renderer draws it; the Soul Corpse decay layer stitches the overlay over it).
* **Daylight**: `isSunSensitive()` and `isSunBurnTick()` are false. Lava, fire and fire attacks still burn it. It also never turns into a drowned, never infects a villager, never calls reinforcements, never picks up loot, is never saved, drops nothing and gives no experience, and does not vanish in Peaceful.
* **Owned**: it never targets the owner, the owner's allies (team, pact people) or any other summon; it follows the owner, fights the owner's foes only (the 1.44 `Corpses.isFoe` rules) and cannot be hurt by the owner or the owner's friends by accident. The `LivingChangeTargetEvent` guard of `CorpseHooks` also applies.
* **Stats** scale with the grimoire's power like the old puppets did: health 24 to 33.6, blow 5 to 7 (power factor 1.0 to 1.4, `SoulZombieRules`). Damage on players goes through the balance law's cap.
* **Life**: Marionette 25 s, Necropolis 10 s, Legion 20 s (as before, longer for mighty casters), Reanimation `corpse.lifeSeconds`. The clock, and the owner logging out, dying or leaving the dimension, end it (the existing sweep every 10 ticks; logout and server stop dismiss at once).
* **Caps**: per owner `corpse.zombieCap` (default 6, over all pages; the oldest is dismissed first), server-wide `corpse.zombieGlobalCap` (default 60). Pages keep their cooldowns (Marionette 13 s, Legion 90 s ...).
* **Cue**: a violet custom-particle burst and ring (`Fx`, theme `corpse`) and the existing CORPSE_RAISE effect on the first body of a summon.

## 2. The five numbered undead

Roles are invented where the canon is silent (see `rs.md`). All are the one class `NamedCorpseEntity` driven by their `NamedCorpses` row. Numbers: health / blow / speed / armour.

| No. | page id | look (series art) | role and numbers | signature (server side, capped) |
|---|---|---|---|---|
| 4 | `corpse_jimmy` | bandaged crouching figure, big golden spiral eyes, straps and buckles | brawler 70 / 7 / 0.36 / 2 (unchanged) | none |
| 3 | `corpse_david` | huge hunched stitched patchwork giant, blue-grey and brown, belt ring, sewn face (3.4 blocks tall) | bruiser 380 / 15 / 0.17 / 12, stays 135 s, page cooldown 30 s | ground slam: every 8 s with a foe within 4 blocks, after a 12-tick wind-up, foes within 3.5 blocks take 0.8 of his blow, are thrown back, slowed 2 s (at most 8 victims, bosses not slowed) |
| 2 | `corpse_alfredo` | tall armoured, belted, hooded, dark-skinned, visor, blue-white spirit aura | guard 260 / 12 / 0.20 / 10 (unchanged) | soul barrier: every 20 s in a fight, the owner, the pact's people, the owner's other bodies and himself within 12 blocks get Absorption II for 8 s (at most 6; a shield, not a heal) |
| 1 | `corpse_carl` | tall thin figure in long white ornate robes and a tall white mitre, dark wild-haired body inside | caster 56 / 3 / 0.26 (unchanged) | keeps his bone shards and Soul Corpse bolts; never heals |
| 0 | `corpse_michael_caesal` | lean, white featureless mask, grey ponytail, blue-grey robe, red sash, dark belts and bandages on the arms, arms spread wide | elite 210 / 18 / 0.38 / 6, stays 90 s, page cooldown 3 min, only one at a time (a second cast is refused while he stands) | soul barrage: every 10 s with a foe in sight within 18 blocks, after a wind-up, six soul bolts (one per 2 ticks), 5 each, 30 raw in all, magic damage, one target |

* Numbered undead per owner: at most `NamedCorpses.MAX_AT_ONCE` = 3 (the oldest is dismissed first; the old three still fit together), server-wide `corpse.namedGlobalCap` (default 30).
* No move deals a percentage of anyone's maximum health. Against players every hit goes through `BalanceLaw.cap`. Each move waits 3 s after the summon.
* Undead: they are in `minecraft:undead` and `minecraft:sensitive_to_smite` (what smite and the Soul Burial engraving read) and, being plain mobs, never burn in daylight. They are in `nusmp:corpse_immune` (never raised again).
* Look: GeoLite models built from boxes (28 to 47 boxes, 128 or 256 texture), clips idle / walk / run / attack / hurt / death (+ cast for David, Alfredo, Carl, Michael: the wind-up of the signature or the throw). Four skins per body (decay stage 0..3, each a little dirtier and more torn than the one before), each with a glow map (Jimmy's golden eyes, David's stitch light and violet eye, Alfredo's visor and aura, Carl's soul core and mitre gem, Michael's palms and mask rim) drawn with the geo drawer's additive pass (the safe additive helper), faded by 0.65 to 0.9 pulse and the death fade.

## 3. The character's name on the Unique Skill

* `grimoire/CanonNaming` (pure): `named("rades", "Soul Corpse Magic")` = "Rades Spirito's Soul Corpse Magic"; a name that ends in s takes a bare apostrophe ("Mars'"); no character (blank, unknown id) = the plain name. Tested for every `CanonBook` entry (all characters).
* `book/GrimoireSkillInstance` (a `TensuraSkillInstance`) overrides `getDisplayName()`; `GrimoireBook` and `GrimoireMagicSkill` return it from `createDefaultInstance()` (which loading a saved instance also calls). It reads the id from the instance's own tag `Canon` (the same id the grimoire item carries), in the style of the plain name (gold for a Unique Skill).
* `book/CanonSkillSync` (server): every 2 s (from `GrimoireKeeper.heal`, which runs per player tick and exits at once) the instance of the magic of the Grimoire Slot book gets `Canon` = the slot book's character, every other grimoire instance has it removed; a changed instance is marked dirty so ManasCore syncs it. Decided on the server; the client only reads the synced tag, no client state in common code.
* Also: the grimoire item tooltip's magic line and the chant line cast in chat ("Rades Spirito's Soul Corpse Magic: ...") carry the name. A grimoire with no character keeps the plain name.

## Cost bounds (owner rule 11, no lag)

Worst cases, by construction and pinned by `CorpseLagTest`:

* Entities: per player 6 zombies + 3 numbered undead; server-wide 60 zombies + 30 numbered undead (configurable). Not counted in the monster mob cap (MISC), never saved, gone with their owner.
* Server work per tick: the roster sweep runs every 10th tick over at most 90 bodies (a tag read and a distance check each). Foe search is **one** world scan per owner per half second (a 72 x 24 x 72 box, at most 40 candidates kept), shared by all that owner's bodies; bodies read the cached list. Before this change each body scanned for itself. Followers re-path at most once per second, follow range 24, no scan of their own. Signature moves scan only when their cooldown is over (David once per 8 s, Alfredo once per 20 s, Michael none: his bolts are hit-scans at one known target). The scan is wrapped in the probe section `s.corpse.foes`, the sweep in `s.corpse.tick`.
* Network: a summon sends one VFX packet and at most about 22 custom particles per zombie (through the particle budget); the decay marker effect packet (when the stage changes, and a repeat every 5 s per body, at most 90 bodies = 18 packets/s worst case server-wide) goes through the per-tick effect-packet budget. Wisps are 1 to 3 particles per body per second, budgeted. Bodies are tracked within 8 chunks only.
* Client draw: a numbered undead is 1344 (David), 1680 (Jimmy), 1824 (Carl), 1968 (Michael) or 2256 (Alfredo) vertices with the glow pass, half of that without. Nothing is drawn beyond 96 blocks, the glow pass is skipped beyond 40 blocks, the pose frame is one reused object (no allocation per frame). A zombie is a vanilla zombie plus the decay overlay (about 430 vertices). Worst realistic case in view: 3 undead + 6 zombies of one caster = about 9000 vertices; ten casters at once in one field of view about 90000 (22000 quads), which the distance cut-offs keep to the nearest.

## Config

Server (`nusmp-corpse-server.toml`, `corpse.*`): `zombiesInsteadOfPuppets` (true), `zombieCap` (6), `zombieGlobalCap` (60), `namedGlobalCap` (30), and all the 1.43 / 1.46 keys unchanged. Client (`nusmp-corpse-client.toml`, new file): `corpse.classicLooks` (false).

## Files

New: `corpse/SoulZombieEntity`, `SoulZombies`, `SoulZombieRules`, `RadesUndeadSelfTests`; `client/corpse/RadesUndeadRenderer`, `CorpseClientConfig`; `book/GrimoireSkillInstance`, `CanonSkillSync`; `grimoire/CanonNaming`; `tools/corpse/gen_rades_undead.py`; assets `rades_*` (geo, animations, textures); `tools/render_preview/scenes.d/rades_undead.json` and two preview stubs; `tools/lang/rades_undead.json`; `data/minecraft/tags/entity_type/undead.json` and `sensitive_to_smite.json`; tests `SoulZombieRulesTest`, `RadesUndeadTableTest`, `RadesUndeadAssetsTest`, `CorpseLagTest`, `CanonNamingTest`.
Changed (additions and the replacements above): `NamedCorpses`, `NamedCorpseEntity`, `CorpseEntities`, `CorpseConfig`, `Corpses`, `CorpseRoster`, `CorpseHooks` (self-test hook), `SoulCorpseSpells`, `book/SoulCorpseBook`, `book/ext/SoulCorpseExt`, `book/CharacterSpells`, `book/GrimoireBook`, `book/GrimoireKeeper`, `skill/GrimoireMagicSkill`, `blackclover/GrimoireItem`, `client/corpse/CorpseClient`, `NUSMP` (two client lines), `armour_policy.json`, `corpse_immune.json`.

## Unverified (nothing was run in a game)

* That `Zombie.isSunSensitive()` / `convertsInWater()` / `killedEntity()` / `shouldDespawnInPeaceful()` have exactly these names and visibility in 1.21.1 (they type-check against the stubs; a mismatch would be a compile error, not a silent bug).
* That ManasCore sends a skill instance tag change to the client and calls `TensuraSkillInstance.getDisplayName()` from the Tensura skill list, the selection panel and the learn / forget messages. If Tensura's screens read the skill's static name instead, only the mod's own panel, the tooltip and the chant show the character. If `TensuraSkillInstance.clone()` / `copy()` build a plain instance, a copy shows the plain name.
* That the dungeon player-model kit's renderer registration for Jimmy, Alfredo and Carl is overridden by the `EventPriority.LOWEST` registration (the kit's listener was documented as the last).
* The look of the models in the real renderer (the headless preview approximates the additive pass with the eyes type and uses stage 0 skins only; the stage 1..3 skins were generated but not viewed), the animation signs on the real pose clock, the glow brightness, the tall mitre and the white robes against real lighting.
* The balance numbers (David and Michael Caesal against a real fight), `Absorption` stacking with other sources, whether six soul bolts at 2-tick spacing really each land (each resets `invulnerableTime`), the knockback direction of the slam, `ZombieRenderer` taking the subclass type.
* The decay layer on the vanilla zombie model (overlay UVs are patterned for any layout, as for any body).
* Page locks: the three old pages (Jimmy, Alfredo, Carl) are now Rades-only, as the owner described them; a player who had them open without Rades's grimoire loses access.
* The preview tool's stubs were stale (it did not compile in this tree); two stubs and one stub line were added so it runs, and the previews use them.
