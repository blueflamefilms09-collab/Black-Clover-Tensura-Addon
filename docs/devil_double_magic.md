# Double Magic (4.40.0 work): grimoire plus devil, One of One

A mage with a grimoire can now also hold a devil contract and use both magics. Each devil of the roster has one contract holder in the world. Any devil magic grants the Ultimate skill **Devil Magic** (and the intrinsic skill **Devil Bond**), both taken back when the contract ends. Adaptability never adapts to devil magic, and "Law Domination" style effects have no effect on it.

Status: written without a running game and without a local compile of anything that touches Minecraft (CI is the compiler). Nothing here was seen in a game. See "Not verified".

## NEW / REPLACED / KEPT

**NEW**
- Double Magic: the arts of Devil Magic (the devil's own art per attribute, plus a double art that mixes it with the grimoire magic); `devil/dual/*`, `skill/DevilArts`, `skill/DevilMagicSkill`.
- One of One: `DevilHolders` (world SavedData, keyed by player UUID), pure rules in `HolderRules`; override by bound duel, death lapse, absence lapse.
- Devil rank mapping onto the relic `Grade`/`Rankings` (`DevilRanks`, `rankings.json` object `devils`).
- Time Magic devil path (`TimeOrigin`), next to the Julius grimoire Time; both count as Time.
- Ultimate skill Devil Magic, granted and revoked by `DevilSkillGrant` (never duplicated).
- Intrinsic skill Devil Bond (assimilate and read the bond), shown with the other intrinsic skills.
- Animated skill icons composed at runtime from per-attribute emblems (`EmblemComposer`, `DevilIconClient`); 105 magic emblems and 17 devil emblems.
- Immunity API: `DevilMagic.adaptationBlocked`, `DevilMagic.lawDominationApplies`, damage type and tag `devil_magic`.
- Creeping screen corruption for body-part and sacrifice prices (`CorruptionRules`).
- Weg v2 markings (16 textures), swept horns (new geometry), v2 horns and wings textures; client setting `devil.classicLook`.
- Craftable Devil Contract (recipe, advancement, recipe-book entry, tag `#anime_multiverse:devil_contract_blood`); server setting `devil.contractCraftable`.
- Ritual dark rule (`DarkRitualRules`, data list `devil_dark_magic.json`, server settings `devil.ritual.*`).
- Ritual layout PNGs and a plain-text how-to.
- Self-tests with prefix `devil.double` (eight tests), an extended `devil.npc` test.
- Pure tests: `DualRulesTest`, `CorruptionRulesTest`, `DevilRanksTest`, `DualSourceScanTest`, `DevilFigureFacingTest`, `DevilTexturesV2Test`, `EmblemComposerTest`.

**REPLACED (the old behaviour stays behind a setting)**
| what | new default | how to get the old one |
|---|---|---|
| Weg dots, horns, wings look | v2 look | client `devil.classicLook = true` (old textures and old horns stay in the jar) |
| Devil NPC facing and ground placement | faces the caller, stands on the ground | none: it was a bug (the figure showed its back, and could hang in the air). Pinned by `DevilFigureFacingTest` and the `devil.npc` self-test |
| A contract ritual in any light | needs a dark magic at the circle, or a dark place | server `devil.ritual.darkRule = false` |
| The contract item is loot only | also craftable | server `devil.contractCraftable = false` |
| Anybody may call any devil | one holder per devil | server `devilDual.oneOfOne.enabled = false` |

**KEPT (untouched)**
- The relic form of the Devil Contract item and every old contract on disk; the data component ids of the 3.48.0 contract.
- The grimoire pact (`/devilpact`), the four Infernal Contracts, the Anti-Magic union, the Devil Union, the eight worlds and the 26 devils of `docs/devil_worlds.md`.
- All old Weg, horn and wing textures; the old HUD; the Time Magic VFX (not touched).
- Adaptability for everything that is not devil magic.

## One of One: override conditions

State: `DevilHolders` SavedData, one row per devil (holder UUID, since when, last seen, last death, reservation). Saved with the world. Every decision is made on the server when somebody tries to call a devil, never in a per-tick scan: the "last seen" stamp is written at login/logout, the "last death" stamp at the death event.

A devil that is held can only be called by another player if one condition frees it:

| condition | setting (`serverconfig/nusmp-devil-dual-server.toml`, `devilDual.oneOfOne.*`) | default |
|---|---|---|
| Death lapse: the holder died and has not let the devil in again for N in-game hours | `graceHours` (0 = never lapses) | 72 |
| Absent lapse: the holder was not on the server for N real days | `absentDays` (0 = never) | 30 |
| Bound duel: `/devil duel <holder>`; the challenger kills the holder inside the window, the holder loses the contract, the devil is reserved for the victor | `duelEnabled`, `duelSeconds` (30 to 1800), `duelRange` (4 to 128 blocks) | on, 300 s, 32 blocks |
| Reservation after a won duel | `reserveHours` | 24 |

Not one-of-one: the four pact devils (`pact_*`, the grimoire pacts of `/devilpact`) stay open to anybody, as before. They have ranks but are not locked. This is a decision for the owner (see below).

## Rank mapping (no ranking text invented)

The four `DevilTier` values map onto the existing relic `Grade`: LOW to RARE, MID to UNIQUE, HIGH to LEGEND, SUPREME to GOD. Each devil is placed in `rankings.json` under `devils`, as `grade:position`, with the order of the roster; the position is only an order within the grade, there are no rank descriptions.

- god (7): lucifero, beelzebub, astaroth, god_hand, daemon_noir, daemon_rouge, sebastian
- legend (13): liebe, megicula, lucifugus, curse_devil, future_devil, kin_of_lucifer, daemon_blanc, daemon_jaune, daemon_violet, daemon_vert, daemon_bleu, hall_demon, etherious
- unique (9): fox_devil, ghost_devil, kin_of_iblis, kin_of_egyn, kin_of_amaimon, pact_kairovore, pact_gravemaw, pact_ignivar, pact_verbael
- rare (1): kin_of_astaroth

All 30 devils of `docs/devil_worlds.md` plus the four pact devils: every one has a rank, an attribute and the skill (checked by `DevilRanksTest` and the `devil.double.rules` self-test).

## Time Magic, two origins

- Grimoire origin: the Julius TIME grimoire magic.
- Devil origin: the devil attribute TIME (Astaroth, the Kairovore pact).
- `TimeOrigin` is NONE, GRIMOIRE, DEVIL or BOTH. `isTime()` is true for any of them, so rules that ask "is this Time magic" accept both. Neither replaces the other; a player may carry both. The Time VFX files were not touched.

## Ultimate skill, never duplicated

`DevilSkillGrant` runs when the contract is signed, ends, at login and at a sync (not per tick): it adds `devil_magic` (Ultimate, dragon class) and `devil_bond` (intrinsic) if the player holds a contract, and removes them if not. It checks the player's skill list first, so a second sync, a second contract or a re-login never doubles the skill. Switches: `devilDual.ultimate.enabled`, `devilDual.ultimate.intrinsicEnabled`.

"10-star menu": there is no such menu in the repo; this was read as the intrinsic skill list. Devil Bond (intrinsic) is the body hub; Devil Magic (Ultimate) holds the attacks. They do not overlap.

## Immunity

- `DevilMagic.adaptationBlocked(source)`: true for the damage type `devil_magic` (the arts) and for the blows of a host with the devil let in. `AdaptabilityEvents` asks it first, so Adaptability does not record or adapt to them. Switch: `devilDual.immunity.adaptability`.
- `DevilMagic.lawDominationApplies(source)`: false for devil magic. Law Domination is not in this repo, so this is a tag (`devil_magic`) plus an API for the day it is added. Switch: `devilDual.immunity.lawDomination`.

## Creeping corruption

For body-part or sacrifice prices (lost eye, half health, flesh). Server counts, client draws (the client reads only the synced number). Defaults (thousandths of the screen per minute): idle 20 (after 60 s without a kill), assimilating +40, Devil Union +80; a kill takes 40 away; a death keeps 0.5 of it; cap 850 (85 %); a clear window around the crosshair is always kept. Switches: `devilDual.corruption.enabled`; client `devil.classicLook` (no creeping overlay). The cap and the clear window keep the game playable.

## Craftable Devil Contract and the dark ritual

- Recipe: very expensive; needs a blood from the optional tag `#anime_multiverse:devil_contract_blood`, which holds `tensura:royal_blood` and `tensura:zane_blood` (both optional entries) and the fallback `anime_multiverse:gneiss_crystal`, so the recipe works without Tensura. "Tinsel" does not exist in this repo or in a name I could match; Royal Blood and Zane Blood are the Tensura items used instead. A custom `anime_multiverse:config` condition hides the recipe when `devil.contractCraftable` is false.
- Advancement and recipe-book entry: added.
- Dark rule (server `devil.ritual.*`): a ritual may begin if a participant within `participantRadius` (8) has a dark magic (data list `devil_dark_magic.json`), or the place is dark: a dark biome (tag), a dark dimension (`darkDimensions`, the End by default), or light at or under `maxLight` (4). One check at the start; while the ritual runs only a light check once a second (one `getMaxLocalRawBrightness` read). A ritual in progress ends if the place turns lit. `darkRule = false` restores the old rituals.
- Layout: `docs/devil_ritual_layout.png` (1600 px), `docs/devil_ritual_layout_discord.png` (1200 px), `docs/devil_ritual_howto.md` (plain text). Original art, made by `tools/gen_devil_ritual_layout.py`.

## Union model and NPC audit

- Cause of the backward devil figure: the player-model front is -z, the living renderer turns it by 180 - yaw which assumes +z, so the figure showed its back. Fix: `DevilNpcRules.PLAYER_MODEL_TURN` applied in `DevilNpcRenderer` (`drawLook` and the fallback); the yaw toward the caller is computed by `yawToward`.
- Spawn: the figure used to be placed at the circle's y, so it could hang in the air or sink into a slope. `DevilNpcs.standY` searches at most 5 blocks (3 down, 2 up) for solid ground.
- The Devil Union player mesh path was audited by frame analysis (mesh frame y up, front +z; layer frame y down, front -z; the `FLIP`) and judged correct, so no change was made there. If the owner still sees a player mirrored, tell me the view (first/third person) and the form; this is a caveat, not a confirmed fix.
- Self-test `devil.npc` now asserts the facing and the ground reach.

## Weg v2, horns and wings

`tools/gen_devil_textures_v2.py` makes 16 `weg2_*` marking sheets, `horns2.png`, `wings2.png`, `wings2_glow.png`. Horns have new swept geometry (`HORNS_SWEPT`). **Wing geometry was not redesigned, only its texture.** Old textures and the old horns stay; `devil.classicLook` shows them.

Previews (absolute paths, in the worktree `/home/user/Black-Clover-Tensura-Addon/.claude/worktrees/agent-a728dd6cd3a21bfa4/`):
- `docs/previews/devil/weg2_sheet.png`
- `docs/previews/devil/parts2_sheet.png`
- `docs/previews/devil/compare_old_new.png`
- `docs/previews/devil/emblems_sheet.png`
- `docs/previews/devil/icons_composed_sheet.png` (made before the last emblem brightness tweak)

## Animated icons

`textures/skill/devil_magic.png` and `devil_bond.png` are still PNGs. While the player holds a contract, `DevilIconClient` registers an `AnimatedIconTexture` under those paths, composed by `EmblemComposer` from the devil attribute emblem and the grimoire magic emblem (every `MagicType`, every devil attribute). A resource reload resets the paths, so a once-a-second check puts the texture back; with no contract, or `devil.animatedIcons` / `devil.classicLook` off, the plain PNGs show.

## Worst-case costs (rule 11)

| thing | bound |
|---|---|
| Arts | at most 6 targets per cast, range 10, cooldowns 160 / 240 ticks; no spawned entities |
| Duels | at most 8 open at once; no per-tick work (checked at a death event and at expiry) |
| Holder rows | at most 256 (one per roster devil, the roster has 30) |
| Holder checks | on a call attempt, a login, a logout, a death: O(1) |
| Skill grant | at contract change, login, sync: a few list lookups, never per tick |
| Corruption | server: one add per player per second while a contract exists; client: one overlay quad set per frame, cap 850, no allocation |
| Dark rule | start: one pass over `l.players()` with a distance check plus a handful of block-light reads; running: one light read per second per ritual |
| NPC | at most 8 by default (`devil.npc.max`, hard cap in `DevilNpcRules.MAX_MAX`), at most 5 block reads on spawn; lifetime 60 s |
| Packets | no new per-tick packet; the sync packet is a few ints, sent to the owner only, on change |
| Client icons | 2 textures, at most 8 cached strips, at most 160 masks; a strip is composed when the (attribute, magic) pair changes, never per frame; one 32 x 32 upload per frame change |

## Self-test prefixes (new)

`devil.double.rules`, `devil.double.arts`, `devil.double.holders`, `devil.double.grant`, `devil.double.immunity`, `devil.double.corruption`, `devil.double.ritual`, `devil.double.icons`; the existing `devil.npc` was extended. `devil.double` was added to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`. The arts test fires at least one art per attribute on the server through `SelfTests.standIn` (a stand-in player, no real client).

## Not verified

- Nothing was seen in a running game: not the corruption overlay, the animated icons, the horns, the Weg v2 markings, the NPC facing, the ritual messages or the layout in Discord.
- The Minecraft-touching code was not compiled locally (offline Gradle); only the pure classes and their tests were compiled and run (87 local tests passed). `DevilStoreTagTest` needs real Minecraft classes and was not run locally. CI is the judge.
- The `anime_multiverse:config` condition (`ConfigCondition`) is a custom `ICondition`; its registration name and codec API were written from the 1.21.1 API as remembered and are unverified.
- Law Domination is absent from the repo, so the immunity is an API, tested only on its own.
- Wing geometry is unchanged.
- The Union player model path is unchanged (see above).
- Wiki research (rule 10): the devil roster and attributes come from `docs/devil_worlds.md`; this task added no new canon facts, so no wiki page was fetched. Nothing that needed a wiki was left unfetched.

## Decisions for the owner

1. Should the four `pact_*` devils become one-of-one too?
2. Is "10-star menu" the intrinsic skill list? If it is something else, tell me which screen.
3. Tinsel: which item is meant? Until then, Royal Blood, Zane Blood or the Gneiss Crystal fallback pay for the contract.
4. Wings: do you want a geometry redesign (a new mesh), not only the texture?
