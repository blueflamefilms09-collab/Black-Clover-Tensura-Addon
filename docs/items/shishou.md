# Shishou, the Akashic Relic (Grade: EX-Singularity)

The talking sword of "Reincarnated as a Sword", built from the owner's design document (`shishou_design.md`) and the reference image (`tools/shishou/shishou_reference.webp`).
One sword per world. He starts as a wild flying sword that hunts and eats monsters, settles into the ground when he is spent, is claimed by one player, levels up, learns skills and talks (Thought Communication text and, since 3.34.0, a spoken voice).

**Nothing here has been seen in a running game.** The pure rules, the assets and the source rules are unit-tested; the Minecraft glue is type-checked against the real jars and covered by CI self-tests (`relic.shishou`, `relic.shishou_claim`, `relic.shishou_skills`), which have not run yet. See "What is not verified".

## NEW / REPLACED / KEPT (rule 5)

| | what |
|---|---|
| NEW | everything about Shishou: item `nusmp:shishou`, entity `nusmp:shishou`, effect `nusmp:black_flame`, data components `shishou_owner`, `shishou_token`, `shishou_level`, the saved data `nusmp_shishou`, the 31-skill table, the keys H / J / K (REPLACED in 2.1.0 by the Tensura skill, see "Update 2.1.0"), the HUD, the command `/multiverse shishou ...` (2.1.0 adds `cast <ability>` and `throw` for the wielder), the 3D mesh and skins, the animation file and the far proxy model, the voice pipeline, the franchise `Reincarnated as a Sword` (2 engravings), the server config `nusmp-shishou-server.toml` |
| REPLACED | nothing. No existing item, skill, model or page was swapped out or removed |
| KEPT | everything else. Shared files only got appended lines (listed in the report): the mod constructor, the command root, the weapon renderer list, `WeaponEngravings`, the creative tab, `GuiMoveRegistry`, `VoiceCatalog`, the weapon-class tables, the Better Combat manifest, the franchise tables, the grip golden (regenerated) |

## What was followed from the owner's document

| document | built as |
|---|---|
| Part 1 flying entity, `FlyingMoveControl` + `FlyingPathNavigation` | `ShishouEntity` (a `PathfinderMob`, invulnerable, no gravity) |
| `WanderAirGoal`, `HuntHostilesGoal`, `TelekineticStrikeGoal`, `DormantPedestalGoal` | `ShishouGoals` (own bounded goals, see below) |
| Predation (loot becomes EXP) | `ShishouService.onDrops` + pure `ShishouPredation`: drops and magic stones are removed and turned into EXP; a stone is worth more the stronger the mob it came from |
| Dormant after kills or days | after 25 kills or 7 days (config) he flies to the nearest solid ground (bounded search) and embeds himself; all hunting stops |
| Part 2 claim, one per world, UUID | right-click the dormant sword; owner UUID and a world token are stored in data components (the 1.21 replacement for NBT); `ShishouCap` is the one-per-world state machine; copies and ghosts dissolve |
| Anti-theft | a non-wielder who holds him gets Slowness X and Wither and the line "Let go of me, you aren't my wielder."; after 10 seconds he tears himself out and goes back to the wielder (or to the dormant state if there is none) |
| Skill Sharing | the sword has its own mana pool; every skill is cast with the SWORD's mana, never the player's. The skill "Skill Sharing" itself gives the wielder a small movement-speed bonus in the main hand |
| Telekinetic Throw | key H throws him out as a friendly minion, H again recalls him to the hand |
| Part 3 tiers | Tier 1 from level 1, tier 2 from level 21, tier 3 from level 50 (table below) |
| Part 4 four animation states | idle / aggro / attack / dormant clips of the mod's own animation file |
| Rukh trail at the hilt (`light_rukh` / `dark_rukh`) | NOT the Rukh types: the repo's own rule (`RukhExclusiveScopeTest`, owner decision) keeps Rukh for the Djinn and the dungeons only, and `FxSourceScanTest` forbids raw vanilla particles outside the allow-list. The trail uses the mod's custom particle library through the new theme `skill.shishou` (cyan, light tone for a wielder's sword, dark tone for the wild one), server-sent, through the perf budget |
| Capability structure (Level, EXP, Mana) | `ShishouRecord` (pure) stored in `ShishouData` (world saved data); the item stack carries the level and the token as data components |

## What was NOT built from the owner's document, and why

1. **GeckoLib.** Replaced by the mod's own animation system (GeoLite: `animations/entity/shishou_sword.animation.json`, parsed by the mod's `GeoAnim`, drawn by `ShishouRenderer`). Reasons: no new dependency to ship, vendor or keep compatible; the server never needs it; the mod already has the keyframe format and a bone sampler that its spirits use; the sword is one rigid bone, so GeckoLib's controller graph would add nothing. The four clips are named exactly as the document names them (`animation.shishou.idle` ...).
2. **NBT.** 1.21.1 has no item NBT; data components are used. Old data stays loadable (ids are only ever appended).
3. **The player capability that "syncs" with the sword.** Not built. The sword keeps its OWN mana and pays for every skill itself, so the player's mana (Tensura or Black Clover) is never touched and nothing has to be kept in sync across two mods. The effect asked for (cast without spending your own mana) is the same.
4. **Self-Repair with durability.** The sword is unbreakable; there is nothing to repair. The mana upkeep idea is kept as Ultra-Speed Regeneration: a swing costs 1 sword mana and the sword heals the wielder slowly from the pool.
5. **`NearestAttackableTargetGoal` as such.** `HuntHostilesGoal` is own code with a capped box query and a retarget gap, so that many hunting swords (only one can exist) and many hostiles never cost an unbounded scan.
6. **Predation of everything.** Only the wild sword eats the drops of its kills by default (a thrown minion leaves the drops for the wielder unless `minionEatsLoot` is on).
7. **The dimensional "Self-Evolution" ranks, a Sword Lord's skill tree beyond the 31 listed skills,** and any Tensura Unique-Skill ranking system. Not built.
8. **Real Tensura API calls.** None in 2.0.0. Names were carried as names (see "Canon"). Since 2.1.0 the skill `nusmp:shishou` is a real skill of the Tensura skill system (ManasCore's skill API, mirrored from the Requip skill).
9. **Appraisal reading Tensura's own appraisal data, trap detection.** Not built. Appraisal reads vanilla health and the sword's own estimates (levelish / manaish / a weakness mask).
10. **Walls of flame or earth as placed blocks.** No skill places or breaks a block (rule 11: no unbounded world edits). Skills are hit-scan or area effects with particles only.
11. **Audio.** Recorded in 3.34.0 (section "Voice" below).
12. **Custom 3D prop VFX for the skills.** Skill effects are particles through the perf budget; there are no new models for pillars or bolts.
13. **Dormant pedestal as a built structure.** He embeds himself in the ground block; no pedestal blocks are placed.

## Canon and sources (rule 10)

- English wiki first: `reincarnated-as-a-sword.fandom.com/wiki/Shishou` was tried once from this session and could not be reached (`ENOTFOUND`). The Japanese wiki hosts were not reachable either. **No wiki page was read in full by the builder.**
- Facts used came from the owner's document and from the coordinator's Japanese web-search snippets (not the full pages, unverified beyond that): Telekinesis (念動), Dimensional Storage that stores anything except living things (次元収納), Self-Repair / High-Speed Self-Repair / Self-Evolution with a per-stone magic-stone value (so Predation pays by the tier of the mob the stone came from), Dimensional Leap (次元跳躍), and that becoming a semi-divine / divine sword costs the ego (準神剣, 神剣化), which is why Divine Sword Release is a TEMPORARY release with a long cooldown.
- "Sword Lord's Haki" and "Elemental Synthesis" are the owner's design, not checked against canon. They are shipped as designed.
- The Tensura-style names (Magic Sense = Magikansi, Stomach = Ibukuro, Thought Communication = Nenka, Multilayer Barrier = Taju Kekkai, Ultra-Speed Regeneration = Chousokuryou, Demon Lord's Haki = Maka, Black Flame = Kuroen, Black Lightning = Kurokaminari, Spatial Motion = Kuukan Idou, Spatial Blade = Kuukan Zandan) were given by the coordinator. **They were not verified against the wikis, the kanji in the lang file (`.jp` keys) are not verified, and the name Maka in particular is unverified.**
- Not fetched at all: every page of the English and Japanese wikis for Shishou.

## How it plays

- **Wild:** one sword per world appears by command (`/multiverse shishou spawn`), by the creative tab entry (a bare stack adopts only if no Shishou exists, otherwise it dissolves) or, very rarely, by a daily roll near a random player (chance 0.004 per day, only while none exists, ring 40 to 80 blocks, never loads a chunk). He flies, hunts hostile mobs (2.1.0: within 40 blocks in a cube, see the update), charges, strikes, eats the drops and gains EXP.
- **Dormant:** after 25 kills or 7 days he embeds himself in the ground. The pose is the dormant clip.
- **Claim:** right-click him. A recovered sword is reserved for his old wielder for 7 days.
- **Held:** (2.0.0 had the keys H, J, K; since 2.1.0 he is the Tensura Ultimate skill Shishou: see "Update 2.1.0"). Sneak + use opens Stomach.
- **Lost:** if the body is lost (void, a destroyed stack, a ghost) the recovery plan returns him to his wielder, or sets him dormant at home or at spawn. The whole world never has two.
- **Level:** (2.0.0 numbers, replaced in 2.1.0: see the table in "(C) Strength") EXP curve `toNext(L) = round(25 + 6L + 0.35L^2)` was the 2.0.0 curve; since 2.1.0 `round(18 + 3.5L + 0.2L^2)`, maximum level 100, start level 10. Mana pool `250 + 14*L + 10*steps`, regeneration `3 + 0.1*L` per second, computed lazily (no per-tick work). Strike damage and the held attack start at 8.5 (an assumed low magisteel sword) and climb to 78.5 at level 100, capped by `strikeCap` (120).

## The 31 skills

Every skill is decided on the server, has a SERVER config switch (`[skills]` in `nusmp-shishou-server.toml`, default on), uses the sword's mana, has a cooldown, and is capped. Absorbed skills have no level: Skill Eater steals them from monsters with a chance by monster (a bounded table: exact ids first, then keywords; `stealChancePercent` scales it).

| skill | kind | unlock | mana | cooldown (ticks) | effect |
|---|---|---|---:|---:|---|
| Telekinesis | passive | L1 | 2 | 20 | the flying body; throw and recall (2.1.0: the Telekinesis mode of the skill Shishou) |
| Thought Communication | passive | L1 | 0 | 0 | he speaks to the wielder (action bar; nearby players only if the wielder turns broadcast on and the server allows it) |
| Ultra-Speed Regeneration | passive | L1 | 2 per use | 5 s gap | heals the wielder slowly when the pool is above 20% |
| Stat Concealment | passive | L1 | 0 | 0 | to anyone but the wielder the tooltip and the Appraisal readout show a false "Rusty Sword" |
| Skill Eater | passive | L1 | 0 | 0 | steals skills from kills; the wild sword's drops become EXP |
| Skill Sharing | passive | L1 | 0 | 0 | small movement-speed bonus while held |
| Wielder Registration | passive | L1 | 0 | 0 | the owner binding and the anti-theft verdicts |
| Appraisal | passive | L1 | 0 | 0 | a movable HUD readout of the looked-at mob (health, levelish, manaish, weaknesses); one request per second per player |
| Multilayer Barrier | toggle | L10 | 12 per deflect | 600 | a timed window; deflects up to 3 projectiles per second |
| Stomach | active | L21 | 4 | 60 | sneak + use opens the internal storage (3 to 6 rows); the cast vacuums up to 16 items in 5 blocks; stores anything except living things, bundles, containers and himself |
| Elemental Synthesis | passive | L21 | 0 | 0 | the elements he has absorbed add on-hit effects (fire, slow, poison, weakness, a spark) |
| Sword Arts Mastery | passive | L25 | 0 | 0 | extra attack speed and a share of attack damage |
| Magic Sense | toggle | L25 | 10 + 2 per refresh | 400 | for 10 s marks up to 64 hostile (and neutral) mobs within 35 blocks with the glow, never players |
| Black Flame | passive | L30 | 0 | 0 | on-hit effect `nusmp:black_flame`: a pulse every half second for 5 s, 1 + 0.04*L damage, capped at 5 |
| Stealth and Camouflage | toggle | L35 | 20 | 400 | 10 s: the sword is drawn translucent and mobs farther than 4 blocks drop the wielder as a target |
| Vibration Magic | passive | L40 | 0 | 0 | 20% of a hit as extra armour-bypassing damage |
| Spatial Motion | active | L50 | 15 | 100 | Dimensional Leap: a short-range teleport (12 blocks, line-checked, at most 40 steps) with 20 ticks of invulnerability |
| Demon Lord's Haki | passive | L50 | 0 | 0 | every second slows and weakens up to 16 hostile mobs with under 20 max health within 10 blocks |
| Divine Sword Release | active | L50 | 100 | 3600 | a temporary release: a pillar on up to 16 targets within 8 blocks of the aim point, capped at 60, plus a +25% bonus for 8 s |
| Black Lightning | active | L50 | 40 | 160 | one bolt within 25 blocks, capped at 80, a 40% splash to up to 6 targets within 3 blocks |
| Spatial Blade | passive | L50 | 15 | 20 | a melee hit adds a spatial slash with the mod's severance damage type |
| Doppelganger | active | L60 | 30 | 400 | up to 4 short-lived clones (5 s), at most 6 in the world, each worth 35% of a strike |
| Fire / Wind / Earth / Water-Ice / Darkness / Healing / Purification | active (absorbed) | by Skill Eater | 10 to 15 | 40 to 120 | hit-scan or area spells on up to 12 targets within 7 blocks of an aim point 24 blocks away; no blocks are changed; Healing heals the wielder (4 + 0.15 per sword level, at most 16) and cleanses curses, Purification cleanses more |
| Poison Coat | passive (absorbed) | by Skill Eater | 0 | 0 | takes effect through Elemental Synthesis (the poison element), so it needs level 21 as well |
| Spatial Motion (short) | active (absorbed) | by Skill Eater | 10 | 100 | the 6-block Dimensional Leap |

Honest status of the skills: all 31 are wired and server-decided; none is a silent stub. Poison Coat has no effect of its own, only through Elemental Synthesis. Skill Sharing is the small speed bonus described above, not a mana sync.

## Update 2.1.0 (hunting, the Tensura skill, strength, feeding, the Akashic GUI)

The owner played 2.0.0 and asked for: a faster hunter, no keys of his own (use the Tensura skill system, like an ultimate skill), a stronger start (a low magisteel sword, then a steep climb like the anime), feeding him from the inventory like a Djinn is put on an item, and cooler GUIs. Nothing was run in a game (see "What is not verified" at the end of this section).

### NEW / REPLACED / KEPT (rule 5)

| | what |
|---|---|
| NEW | the Tensura Ultimate skill `nusmp:shishou` ("Shishou, the Teacher", 7 modes) and its grant / revoke rule; the pure `ShishouHunt` (scan, charge, strike cooldown, patrol), the patrol goal; feeding from the inventory (`ShishouFeed`, `ShishouFeedService`, `relic.shishouFeed`); the migration of 2.0.0 saves; the Akashic GUI (`AkashicTheme`, `ShishouStomachScreen`, atlas `textures/gui/shishou/akashic.png`, skill icon `textures/skill/shishou.png`, generator `tools/shishou/gen_shishou_gui.py`, mock preview tool `tools/shishou/shishou_gui_preview.py`); the config keys of `[progress]`, `[feed]`, `[skill]` and `wild.huntRadius / huntScanTicks / huntMaxExamined / chargeSpeedPercent / patrolToPlayers`, `combat.damageScalePercent`; self-tests `relic.shishou_skill_grant`, `relic.shishou_feed`, `relic.shishou_hunt`; tests `ShishouUpdate21Test` |
| REPLACED | the three keys H, J, K, their key registrations (`ShishouClient`), the key packet and its handler (`ShishouNet.Key`, `ShishouService.onKey`): replaced by the skill's modes (the owner asked for no new keybinds). The target goal (`HuntHostilesGoal`, was a vanilla nearest-target scan) and the charge (`TelekineticStrikeGoal`, was move-control charging with a 24 tick cooldown). The numbers of level, mana, damage, experience and the dormant limit (table below). The look of the HUD (the old panel theme) and of the Stomach screen (was vanilla's chest screen). The tooltip line "Keys: ..." |
| KEPT | everything else: the 31 skills and their per-skill cooldown / mana logic (the modes only route to them), Shift + right click Stomach, the Appraisal HUD, the one-per-world rules, the wild / dormant / claim / recovery rules, all saved data (old data component ids, the saved record, bits of the learned mask), the lang keys of the old keys (`key.nusmp.shishou_*` stay in the lang file, unused), the 2.0.0 numbers (kept as `LEGACY_*` constants for the migration and the tables) |

### (A) The hunter

Why he was slow in 2.0.0: the target goal was vanilla's nearest-attackable scan (one look every ten ticks on average, and only 4 blocks up and down: a flying sword hovering above the ground saw nothing), 24 blocks of range, a 24 tick strike cooldown, a charge that depended on the move control and stalled on the first wall, and 25 kills then dormant.

| part | 2.0.0 | 2.1.0 |
|---|---|---|
| look for prey | about every 10 ticks, 4 blocks up and down | every 6 ticks (`huntScanTicks`, 2 to 40), a cube of the radius, up to 24 blocks up and down |
| radius (wild) | 24 | 40 (`huntRadius`, 8 to 96); a thrown sword: the leash (default 24, was 16) plus 6 |
| examined per look | unbounded list | at most 64 creatures (`huntMaxExamined`, 8 to 256) |
| choice | the nearest | the best score: distance minus a danger bonus (up to 8 blocks) minus 14 for a creature that is attacking a player |
| after a kill | the next scan within ten ticks | the next look is immediate |
| charge | move control, speed 1.9 x 0.6 | direct 3D steering: 0.45 blocks a tick, +0.16 a tick, up to 1.15 (level 10) to 1.9 (level 100) blocks a tick, i.e. 23 to 38 blocks a second (`chargeSpeedPercent`), slows near the target, never tunnels (hard cap 1.9) |
| blocked | stalls | after 3 blocked ticks slides up and aside; after 40 gives that prey up for 160 ticks and looks again |
| strike cooldown | 24 ticks | 15 at level 10 down to 6 at level 100 |
| no prey in range | random wander (speed 1.0) | flies toward the nearest player within 160 blocks (that is where monsters spawn; one nearest-player lookup a second, `patrolToPlayers`), else wanders at speed 1.6 |
| dormant | 25 kills, 100 wild mana at 4 a strike (25 strikes) | `dormantAfterKills` 60 + 2 per level (level 10: 80, level 100: 260), 400 wild mana at 2 a strike (200 strikes); days (7) unchanged |

He still never targets a player, a villager or other NPC, a tamed or owned creature, another Shishou or an ally (`ShishouTargets.hostileFor`, unchanged), and the one-per-world, dormant and claim rules are untouched. A thrown sword hunts only within its leash of its wielder and at the wielder's mana (1 per strike).

**Server cost (rule 11).** Looking for prey while he has no target: one box query every 6 ticks, i.e. at most 3.4 a second (10 a second at the fastest setting 2), at most 64 creatures examined per look (at most about 215 a second by default, hard cap from config); while he has a target there is no scan at all. The patrol: one nearest-player lookup a second, only while a wild sword has no prey. The charge: one velocity update a tick on one entity, no path finding, no block reads. No chunk is loaded (the box only sees loaded chunks; the sword stops at unloaded ones). One entity exists in the whole world, so all of this is world-wide cost, not per player.

### (B) The Tensura skill (replaces the keys)

**Name and canon.** Ultimate Skill "Shishou, the Teacher" (師匠, shishou). The naming follows the Tensura ultimate skills the mod's other skills follow (the Great Sage / Raphael "Wisdom King" line: an ultimate skill is a named intelligence with modes). Sources: the English wiki (tensura.fandom.com) and the Japanese Wikipedia could NOT be reached from this session (`ENOTFOUND` on both), so the naming is from the novel's well known skill names and the repo's own Ultimate skills, not read from a page this session. The Tensura jar is not available offline either: the registration, the modes and the learn / forget calls are copied from the proven Requip skill (`ErzaRequipSkill`, `RequipSystem.teach`, `NUSkills`) and type-checked against the API stubs only.

**Getting and losing it.** The skill belongs to the wielder of record (the owner UUID in the world's record): claiming the dormant sword (or a creative-tab adoption) grants it at once; when the sword is claimed by somebody else (or the record is reset) the previous wielder loses it, at once if online, else at their next login or within 5 seconds of their next tick. Every player is checked at login and once per 100 ticks (spread by entity id): one owner compare, a skill lookup only when needed. Server config `skill.grantUltimateSkill` (default true). Decisions are on the server, per player UUID; there is no state to clean at logout, death or dimension change beyond what 2.0.0 already drops.

**The modes** (the skill system's mode key switches, the skill key uses the mode, sneak + skill key picks the next ability of a mode that holds several; the pick is stored on the skill instance and mirrored on the HUD):

| mode | abilities (cast order) | press | sneak + press |
|---|---|---|---|
| Telekinesis | Telekinesis | throw the held sword out as a minion, or recall it | the same |
| Sensory | Magic Sense, Stealth and Camouflage | cast the picked one | next one |
| Defence | Multilayer Barrier, Spatial Motion, Spatial Motion (short) | cast | next |
| Offence | Black Lightning, Divine Sword Release, Doppelganger | cast | next |
| Storage | Stomach | the vacuum cast | opens Stomach (Shift + right click still works) |
| Magic | Fire, Wind, Earth, Water-Ice, Darkness, Healing, Purification (the absorbed ones) | cast | next |
| Counsel | (none) | one action-bar line: level, share to the next level, mana, skills eaten | the same |

The passive abilities (Thought Communication, Ultra-Speed Regeneration, Stat Concealment, Skill Eater, Skill Sharing, Wielder Registration, Appraisal, Elemental Synthesis, Sword Arts, Black Flame, Vibration, Demon Lord's Haki, Spatial Blade, Poison Coat) need no mode: they are on once he has them, as before. Every cast is the unchanged `ShishouService.cast`: the sword must be in a hand ("Hold me to cast"), the sword's own mana pays, the per-ability cooldown and caps apply. A mode whose abilities are not open yet says so. Presses are rate limited per player (4 ticks). Nothing was dropped silently: every ability reachable by a key in 2.0.0 is reachable by a mode. Fallback if the skill system cannot be reached (for example `skill.grantUltimateSkill = false`): Shift + right click Stomach, the held sword's passive abilities and the HUD still work; the wielder can still cast from chat: `/multiverse shishou cast <ability id>` (any castable ability, same cooldowns, mana and rate limit) and `/multiverse shishou throw` (throws or recalls him); `/multiverse shishou skills` lists what he knows.

**HUD readout.** The Akashic HUD shows the mode of the selected ability above its name (violet), his level, experience and mana gauges, the open windows and the Appraisal readout.

### (C) Strength

His weapon and strike damage start at a LOW MAGISTEEL SWORD and climb steeply. The real value is **not in the repo** (no weapon attribute file, tag or doc has it; the Tensura tier values are not in the API stubs) and the Tensura wiki hosts could not be reached (English first, then Japanese: both `ENOTFOUND`), so the start is an ASSUMPTION: item damage 8.5 (a little above a netherite sword's 7), shown as +8.5 in the tooltip and 9.5 with the player's own 1. It is the constant `ShishouRules.START_ATTACK`; the part that grows is scaled by `combat.damageScalePercent`. The growth: `0.6 per level + 0.0035 per level squared` above level 10, capped at +70. Genesis-grade weapon damage in this mod is the 40 point tier ceiling of `docs/relic_tiers_and_scaling.md` (and the Genesis Demon-Slayer's severance, a different mechanic); the held attack passes 40 at level 53 and the strike follows the same curve. He starts at level 10 (Tier 1 fully open and the Multilayer Barrier with it); his skills and mana are not "damage", so this does not contradict the low magisteel start. The cap stays level 100 and every number is a config or a constant.

| level | held attack 2.1.0 | 2.0.0 | strike 2.1.0 (0 steps) | strike 2.0.0 | mana pool | 2.0.0 | mana / s | 2.0.0 | exp to next | 2.0.0 | strike cooldown | charge b/tick | dormant kills |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 10 | 8.5 | 8.2 | 8.5 | 10.5 | 390 | 180 | 4.0 | 1.0 | 73 | 120 | 15 | 1.15 | 80 |
| 20 | 14.9 | 9.4 | 14.9 | 15.0 | 530 | 260 | 5.0 | 1.4 | 168 | 285 | 14 | 1.25 | 100 |
| 30 | 21.9 | 10.6 | 21.9 | 19.5 | 670 | 340 | 6.0 | 1.8 | 303 | 520 | 13 | 1.35 | 120 |
| 50 | 38.1 | 13.0 | 38.1 | 28.5 | 950 | 500 | 8.0 | 2.6 | 693 | 1200 | 11 | 1.55 | 160 |
| 70 | 57.1 | 15.4 | 57.1 | 37.5 | 1230 | 660 | 10.0 | 3.4 | 1243 | 2160 | 9 | 1.75 | 200 |
| 100 | 78.5 | 19.0 | 78.5 | 51.0 | 1650 | 900 | 13.0 | 4.6 | max | max | 6 | 1.90 | 260 |

Honest note on the start: with the low magisteel start the strike at levels 10 to 23 is up to 2 points below the 2.0.0 strike of the same level (8.5 against 10.5 at level 10); from level 24 on it is above, and the HELD attack is never below 2.0.0's. Strike caps: `strikeCap` 120 (was 60). Experience: a kill is `5 + 1.2 per max health` up to 600 (a boss counts double; was `3 + 0.8` up to 400; a zombie gives 29, was 19), a magic stone 8, low crystal 10, medium 30, high 90 (was 5, 6, 20, 60), a plain drop 2 + 3 per rarity step (was 1 + 2), per-kill and per-meal caps 3,000 (was 2,000); the curve `18 + 3.5 L + 0.2 L^2` (the whole climb is 84,782 experience, was 147,090) with `progress.expPercent` for the server. Mana pool `250 + 14 L + 10 per step` (was `100 + 8 L + 6`), regeneration `3 + 0.1 L` a second (was `0.6 + 0.04 L`).

**Migration.** A 2.0.0 record has no `ver` key: on load it keeps the level it had on the 2.0.0 curve (the new curve is gentler, so the level never drops), is raised to level 10 at least (`progress.startLevel` may ask for more; it never lowers), keeps its experience, kills, eaten skills, steps and selected skill, keeps its mana share of the larger pool (never less mana), and its wild budget keeps its share (100 of 100 becomes 400 of 400). The new version is written with it, so the migration runs once. Tested in `ShishouUpdate21Test.anOldSaveIsRaisedAndNeverLowered`.

### (D) Feeding from the inventory

The Djinn Seal pattern, copied: the Shishou item answers `overrideStackedOnOther` (the sword on the cursor clicked on a stack in your own inventory) and `overrideOtherStackedOnMe` (a stack on the cursor clicked on the sword in a slot). A left click eats the whole stack up to `feed.feedPerClick` (64), a right click eats one. What he eats: Tensura magic stones and crystals (valued by the Predation table as stones of a common monster) and monster drops (vanilla rotten flesh, bone, string, spider eye, gunpowder, slime ball, ender pearl, blaze rod / powder, ghast tear, phantom membrane, prismarine, magma cream, leather, feather, ink sacs, scute and raw meat, and any item whose name carries `_core`, `_scale`, `_horn`, `_fang`, `_claw`, `_hide`, `_tooth`, `_tusk`, `_bone`, `_flesh`, `_shell`, `_feather`, `_venom`, `_silk`, `_fur`, `_blood`, `_ichor`, `_gland`), for experience with the same table as his kills. Skill Eater may steal a skill from a drop through a small item to monster mapping (blaze rod and powder: Blaze, ender pearl: Enderman, prismarine: Guardian, spider eye and string: Spider, phantom membrane: Phantom, magma cream: Magma Cube, ghast tear: Ghast, any other name through the table's word rows; a stone has no species and steals nothing), at half the table's chance and at most four rolls a click.

Rules: only the OWNER (the sword stack's written wielder, and the world's record: both are checked); the decision whether the click is taken over depends only on the stack and the owner, so the client and the server always agree and the cursor cannot desync (the server then decides the outcome); a stack that is not food (tools, armour, blocks, containers, spawn eggs, buckets, himself, unlisted things) or a non-owner's click is left to the game (vanilla swap); rare or enchanted drops and a sword at the level cap are taken over but refuse with a short action-bar line and keep the stack; one feeding click per player per 4 ticks (`feed.feedGapTicks`); items are removed exactly once on the server (never duplicated); the experience goes through `progress.expPercent`, Thought Communication speaks, a small particle burst goes through the perf budget and a soft chime plays. Server switch `relic.shishouFeed` (default true). Worst case: O(1) per click (one table lookup, at most four steal rolls, one particle burst). Pure tests: `ShishouUpdate21Test` (food rules, per-click cap, value table, verdict order, non-owner, steal mapping) and the self-test `relic.shishou_feed`.

### (E) The Akashic GUI

An Akashic theme for his own GUIs: a dark blue-violet star field with a faint aurora and a constellation of fine prismatic lines, a glowing cyan frame (the blade's cyan, breathing slowly) with corner ornaments and gold studs, a prismatic line under the title row, 14 slowly drifting light motes (6 in the HUD), the sword emblem, and his level, experience and mana as glowing gauges (a travelling shimmer, a bright leading edge, red when low). **Stomach**: the vanilla chest screen is swapped on the client for `ShishouStomachScreen` (the title is Stomach's; same menu, same slots, same packets), with the gauges in its title row; registered as movable and sizeable (`shishou_stomach`, GUI Move and GUI Size). **HUD**: the existing movable `shishou_hud` in the same theme, with the mode of the selected ability. The classic UI theme gets a flat panel instead. Draw cost: a panel is at most 20 star-tile blits (clipped), 4 corners, 2 studs, 3 outlines, one gradient, 14 motes; a gauge about 12 fills; nothing is allocated per frame. The numbers live in the pure `ShishouGuiLayout` (tested), the atlas is made by `python3 tools/shishou/gen_shishou_gui.py`. **Previews** (a PIL MOCK, not a game screenshot; three rounds were looked at and fixed: frame glow too heavy, title overlapping the level, slot grid too bright, constellation node squares): `docs/previews/shishou/gui_stomach_mock.png`, `gui_hud_mock.png`, `gui_atlas.png`; the skill icon is `textures/skill/shishou.png`.

### Worst case (2.1.0 additions, rule 11)

| part | worst case |
|---|---|
| hunt scan | one box query per 6 ticks while no target, at most 64 creatures examined; none while a target is held |
| patrol | one nearest-player lookup a second, wild and no prey only |
| charge | one entity, one velocity update a tick |
| skill grant | per player: one owner compare per 100 ticks (spread), a skill lookup only when the list must change; claim and reset: one pass over the online players |
| skill press | rate limited per player (4 ticks); the work is the unchanged cast (see the table above) |
| feeding click | O(1), one per player per 4 ticks, at most four steal rolls, one particle burst |
| packets | fewer than 2.0.0: the key packet is gone; the state packet to the wielder (once a second) and the appraisal pair are unchanged |
| client | HUD and Stomach screen as in (E); only the wielder's HUD draws; the screen swap is one event on opening a chest screen |

### Tests and self-tests (2.1.0)

Changed existing tests: `ShishouProgressionTest` (the curve numbers 31 and 285 are now the legacy numbers and the new ones are 22 and 168; the fresh record starts at level 10, so the mana, level-up and bad-data cases assert the start level; the wild phase has about 200 strikes; the strike cap and the start damage), `ShishouSourceScanTest` (no key packet, the press is rate limited by `pressAllowed`, up to 3 box queries: the targets helper, the clone cleanup and the hunt's one scan). New: `ShishouUpdate21Test` (hunt bounds and curves, the damage table and the climb past 40, experience and mana, the migration of old saves, the mode routing, the grant rule, the food rules, the geometry of the GUI and the atlas, no key registration left, the hunt scan is bounded, the config keys, the lang entries). CI self-tests (prefix `relic.shishou` already in the smoke list, so the new ones run with it): `relic.shishou_skill_grant`, `relic.shishou_feed`, `relic.shishou_hunt`.

### What is not verified (2.1.0)

- Nothing was run in a game. The skill's registration and calls are type-checked against the API stubs only: whether the real Tensura skill UI lists a skill whose modes carry the pick in the name, whether `forgetSkill` called from the player tick behaves while the player is mid-cast, and the exact behaviour of the mode and skill keys with 7 modes are unverified.
- The start damage of the low magisteel sword (8.5) is an assumption: no number in the repo, the wiki hosts could not be reached.
- The charge's steering with the vanilla `travel` friction and the velocity blend, the unstick nudge against real walls, and how fast the patrol really finds monsters are unverified in a game (the self-test `relic.shishou_hunt` checks the scan and the acceleration only).
- The swap of the vanilla chest screen by a title check, the Akashic screen's look and its interplay with GUI Move / GUI Size, and the HUD's look in game are unverified; the previews are a mock.
- Both inventory click hooks follow the Djinn Seal pattern, but the cursor behaviour on a real client with the swap rules of other mods (for example inventory sorters) is unverified.
- The new skill has no Japanese text of its own beyond the name 師匠 used in this document; no kanji was verified against a wiki (rule 10: both wikis were unreachable).

## Models, art, HUD

- **3D sword** built from the reference image: eight parts (pommel and grip lathe, tang, gold guard, ornament, the head with its gem, the blade loft, the cyan inlay), about 4,965 quads with the glow layer; sampled 1024 skin and a glow mask; length 1.71 blocks. In hand it is drawn by the mod's weapon renderer (`WeaponRenderer`) like every weapon; the old sprite stays as the inventory icon.
- **Entity** draws the same mesh, animated by the animation file; beyond 48 blocks it draws an eight-cube proxy (`geo/entity/shishou_sword.geo.json`), beyond the cull range nothing.
- **Previews** (made by `tools/shishou/shishou_preview.py`): `docs/previews/shishou/*.png` (sword against the reference, guard and head close-up, the four states, dormant in the ground, inventory icon, HUD, in-hand).
- **HUD** `shishou_hud`: level, mana, selected skill, barrier / sense / stealth timers and the Appraisal readout; movable with the mod's GUI-move screen.
- **Particles:** every call goes through the custom particle facade (`Fx.options(FxThemes.SKILL_SHISHOU, ...)`), rewritten by `tools/particles/codemod.py`; the theme `skill.shishou` was appended to `tools/particles/gen_themes.py` and `map.json` (so `particle_themes.json`, `FxThemes.java` and `docs/custom_particles.md` were regenerated; the regeneration also added the already-existing relic wave themes 101 to 105 that the generated files had not listed yet).
- **Engravings** (rule 6): severance 2, barrier piercing 2, magicule absorption 2, energy steal 1, magic interference 1 (`item/WeaponEngravings.java`), and the franchise engravings `Teacher's Insight` and `Devoured Power`.

## Voice (3.34.0)

`tools/audio/shishou_lines.json` lists 41 lines in 18 groups. **All 41 are recorded** with the ElevenLabs voice "Jay - British, well-spoken, calm, husky." (`rm143ZlE6RfHtN634wZ8`, chosen by the owner), model `eleven_v4`, one take per line with a short delivery tag (`[calm, dry, amused]`, ...), converted to mono 32 kHz Ogg Vorbis (about 1.05 MB in all) by `tools/audio/shishou_voice_convert.py`. A first set made with a voice I had picked myself ("Blue - Commander with Grit") was replaced by the owner's choice before any release; none of it is in the jar. `ShishouVoiceLines` carries the same words as text (the subtitles are "Shishou: ..." in `tools/lang/shishou_voice.json`); a moment whose line has no file stays Thought Communication text only. Speech rules (`ShishouSpeechGate`): a per-wielder cooldown per group, an 80-tick gap between lines, only the wielder (and listeners within 32 blocks when broadcast is on). Config `relic.shishouVoice` switches the speech.

**To redo a line or change the voice:** put the new voice id into the header and the lines of `tools/audio/shishou_lines.json`, generate the takes (the `prompt` field is the exact text with its tag), save them as `<id>.mp3` in one folder and run `python3 -B tools/audio/shishou_voice_convert.py <folder>` (converts to ogg, writes `sounds.json` entries, updates the `REGISTERED` block). `ShishouVoiceAssetsTest` keeps the manifest, catalogue, sounds.json, files and lang text in step.

## Server and multiplayer (rule 9)

- All state is decided on the server. The client draws and asks: one packet to the server (appraisal request: one id; the 2.0.0 key packet is gone, the skill system carries the presses), two to the client (state sync to the wielder only; the appraisal reply). Every packet validates its data and sender on the server and is rate limited per player.
- Per-player state is keyed by UUID and dropped at logout, death, respawn, dimension change and server stop.
- Common code names no client class (`ShishouSourceScanTest`); the client hooks are set by the client.
- Config is one SERVER file, `nusmp-shishou-server.toml` (nothing of Shishou changes only looks, so no client config). Keys: `relic.shishouEnabled`, `relic.shishouVoice`, `relic.shishouFeed`, `wild.*` (2.1.0: `huntRadius`, `huntScanTicks`, `huntMaxExamined`, `chargeSpeedPercent`, `patrolToPlayers`), `progress.*` (`startLevel`, `expPercent`), `combat.*` (`damageScalePercent`), `minion.*`, `feed.*`, `skill.grantUltimateSkill`, `telepathy.*`, `sense.*`, `storage.rows`, `skills.<skill>`.
- The `/multiverse shishou` command: `spawn`, `status`, `reset`, `recover`, `level set <n>`, `skills`, `broadcast <true|false>`, `stomach auto <true|false>`.

## Worst-case cost (rule 11)

Only one Shishou exists, so every per-sword number is a world-wide number.

| part | worst case |
|---|---|
| holder hook (`inventoryTick`) | per stack in an inventory: one token compare (O(1)); the full upkeep once a second, only for the holder |
| server tick | one counter test per tick; once a second a flag test, one window sweep, one repair check; clones only while any live (every 10 ticks, at most 6) |
| kill / drop listeners | O(1) unless a sword body or a linked wielder is involved |
| the body (entity) | one entity; (2.1.0: one box query every 6 ticks while it has no target, at most 64 candidates, a strike every 15 to 6 ticks, see "Worst case (2.1.0 additions)"; 2.0.0 was one query per 20 ticks, 24 blocks, 12 candidates, a strike every 24 ticks); the particle trail 2 to 3 particles every third tick; ground search at most 40 blocks vertically and 8 sideways tries |
| Telekinesis throw / recall | 1 entity spawn or removal; gap 20 ticks per player |
| Stomach vacuum | one box query, at most 16 items, 60 ticks cooldown; auto mode at most 8 items per second |
| Multilayer Barrier | up to 3 deflects per second, per-player O(1) window gate |
| Magic Sense | one box query per 40 ticks while on, at most 64 targets, glow effect refreshes only |
| Stealth | O(1) window, no scan |
| Demon Lord's Haki | once a second, one box query (10 blocks), at most 16 effects |
| Spatial Motion | at most 40 raycast steps, no chunk load |
| Divine Sword Release | once per 3,600 ticks: one query, at most 16 targets, at most 16 particle bursts |
| Black Lightning | once per 160 ticks: one raycast of 25 blocks, at most 6 splash targets |
| Spatial Blade | once per 20 ticks per wielder, on a melee hit, one target |
| Doppelganger | at most 4 entities per cast, 6 in the world, each lives 100 ticks and is removed on logout, death, dimension change, server stop |
| Elemental spells (7) | one query each, at most 12 targets, cooldowns 40 to 120 ticks |
| Black Flame | on-hit effect, one pulse per 10 ticks per burning mob (damage capped) |
| Appraisal | one request per second per player, one reply packet to that player only |
| Skill Eater | on kill, a table lookup (a bounded table, no scan) |
| Recovery | rare: one chunk load at the destination |
| packets | to the wielder only: one state packet per second plus on change; nothing is broadcast |
| particles | every effect goes through `PerfParticles` (the per-tick budget); no raw `sendParticles` |
| client | at most about 4,965 quads per sword within 48 blocks (glow pass only inside the glow range), 8 cubes beyond, nothing beyond the cull range; no per-frame allocation; one appraisal request per second |

Measured by `PerfProbe` sections `s.shishou.hold`, `s.shishou.cast`, `s.shishou.hit`, `s.shishou.tick`.

## Tests

- Pure: `ShishouProgressionTest` (levels, skills, mana, damage, steal and predation tables), `ShishouCapRulesTest` (one per world, claim, recovery, theft), `ShishouVoiceAssetsTest`.
- Files and source rules: `ShishouAssetsTest` (mesh, skins, icon, models, animation clips, proxy, lang of every skill), `ShishouSourceScanTest` (no client class in common code, per-UUID state and cleanup, packet validation, one server config with a unique file name and a switch per skill, perf budget, bounded scans, probes, no hunger or exhaustion), plus the Shishou probe line in `PerfSourceScanTest`.
- CI self-tests, to be added to the smoke list: `relic.shishou`, `relic.shishou_claim`, `relic.shishou_skills` (2.1.0 adds `relic.shishou_skill_grant`, `relic.shishou_feed`, `relic.shishou_hunt`, which the `relic.shishou` prefix covers).

## What is not verified

- **Nothing was run in a game.** No server, no client, no CI self-test, no screenshot of the entity in a world. The previews are renders of the mesh with the repo's preview tool.
- The Minecraft glue was type-checked against the real jars only; the only type-check noise is the known stub artifact (abstract `KeyMapping`, `deserializeNBT`) of the checking setup.
- The kanji of the Japanese names in the lang file and the names Maka, Kuroen, Kurokaminari, Kuukan Zandan are unverified.
- Whether the damage type used for Spatial Blade, Black Lightning and Black Flame bypasses armour as intended (`indirect_magic`, the mod's severance type) is unverified in game.
- The dormant pose and the embedding in different ground blocks (slabs, snow, water) are unverified.
- The creative tab entry resolves at the first inventory tick (a bare stack adopts only if no Shishou exists).
- Storage lives in the world's saved data (`nusmp_shishou`), not in an item component: the item can be copied or lost without losing the contents, and a lost sword keeps his Stomach. This is a design choice for the one-per-world rule; it means another mod that clones the stack cannot duplicate stored items.
- The balance numbers are first guesses, not playtested; every cap is a config value.
- The Japanese wiki facts are from search snippets only.
- Tensura Reincarnated's appraisal and engraving behaviour was not integrated beyond the enchantment ids in `WeaponEngravings`.
- Audio: none exists.

## 3.2.0: the flurry bounce
After a landed strike Shishou bounces off the target (0.55 blocks a tick away from it, a little up, for 3 ticks, `ShishouHunt.bounce`) and then the charge starts again, like an anime sword flurry. Server setting `flurryBounce` (default true; false = the 3.1 behaviour of hovering on the target). Entity attributes: flying speed 1.5, movement speed 0.8, follow range 64, attack damage 14 (the strike damage itself still comes from the sword level, `ShishouRules.strikeDamage`). The 3.0 hunter (flying move control, flying navigation, direct 3D charge, one bounded box query per scan, no melee goal) is kept: a 2-tick scan over 64 blocks would break the no-lag rule, so the scan gap stays the configured `huntScanTicks`. Worst case cost of the bounce: one velocity set per strike, nothing per tick but a counter.
