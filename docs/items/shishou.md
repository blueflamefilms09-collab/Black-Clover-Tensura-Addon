# Shishou, the Akashic Relic (Grade: EX-Singularity)

The talking sword of "Reincarnated as a Sword", built from the owner's design document (`shishou_design.md`) and the reference image (`tools/shishou/shishou_reference.webp`).
One sword per world. He starts as a wild flying sword that hunts and eats monsters, settles into the ground when he is spent, is claimed by one player, levels up, learns skills and talks (as action-bar text; the voice pipeline is built but no audio exists yet).

**Nothing here has been seen in a running game.** The pure rules, the assets and the source rules are unit-tested; the Minecraft glue is type-checked against the real jars and covered by CI self-tests (`relic.shishou`, `relic.shishou_claim`, `relic.shishou_skills`), which have not run yet. See "What is not verified".

## NEW / REPLACED / KEPT (rule 5)

| | what |
|---|---|
| NEW | everything about Shishou: item `nusmp:shishou`, entity `nusmp:shishou`, effect `nusmp:black_flame`, data components `shishou_owner`, `shishou_token`, `shishou_level`, the saved data `nusmp_shishou`, the 31-skill table, the keys H / J / K, the HUD, the command `/multiverse shishou ...`, the 3D mesh and skins, the animation file and the far proxy model, the voice pipeline, the franchise `Reincarnated as a Sword` (2 engravings), the server config `nusmp-shishou-server.toml` |
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
| Rukh trail at the hilt | the mod's own light / dark Rukh particles (`RukhParticles`), server-sent, through the perf budget |
| Capability structure (Level, EXP, Mana) | `ShishouRecord` (pure) stored in `ShishouData` (world saved data); the item stack carries the level and the token as data components |

## What was NOT built from the owner's document, and why

1. **GeckoLib.** Replaced by the mod's own animation system (GeoLite: `animations/entity/shishou_sword.animation.json`, parsed by the mod's `GeoAnim`, drawn by `ShishouRenderer`). Reasons: no new dependency to ship, vendor or keep compatible; the server never needs it; the mod already has the keyframe format and a bone sampler that its spirits use; the sword is one rigid bone, so GeckoLib's controller graph would add nothing. The four clips are named exactly as the document names them (`animation.shishou.idle` ...).
2. **NBT.** 1.21.1 has no item NBT; data components are used. Old data stays loadable (ids are only ever appended).
3. **The player capability that "syncs" with the sword.** Not built. The sword keeps its OWN mana and pays for every skill itself, so the player's mana (Tensura or Black Clover) is never touched and nothing has to be kept in sync across two mods. The effect asked for (cast without spending your own mana) is the same.
4. **Self-Repair with durability.** The sword is unbreakable; there is nothing to repair. The mana upkeep idea is kept as Ultra-Speed Regeneration: a swing costs 1 sword mana and the sword heals the wielder slowly from the pool.
5. **`NearestAttackableTargetGoal` as such.** `HuntHostilesGoal` is own code with a capped box query and a retarget gap, so that many hunting swords (only one can exist) and many hostiles never cost an unbounded scan.
6. **Predation of everything.** Only the wild sword eats the drops of its kills by default (a thrown minion leaves the drops for the wielder unless `minionEatsLoot` is on).
7. **The dimensional "Self-Evolution" ranks, a Sword Lord's skill tree beyond the 31 listed skills,** and any Tensura Unique-Skill ranking system. Not built.
8. **Real Tensura API calls.** None. Names are carried as names (see "Canon").
9. **Appraisal reading Tensura's own appraisal data, trap detection.** Not built. Appraisal reads vanilla health and the sword's own estimates (levelish / manaish / a weakness mask).
10. **Walls of flame or earth as placed blocks.** No skill places or breaks a block (rule 11: no unbounded world edits). Skills are hit-scan or area effects with particles only.
11. **Audio.** No voice was generated: the lines, the pipeline and the tests exist; the finish is one command (below).
12. **Custom 3D prop VFX for the skills.** Skill effects are particles through the perf budget; there are no new models for pillars or bolts.
13. **Dormant pedestal as a built structure.** He embeds himself in the ground block; no pedestal blocks are placed.

## Canon and sources (rule 10)

- English wiki first: `reincarnated-as-a-sword.fandom.com/wiki/Shishou` was tried once from this session and could not be reached (`ENOTFOUND`). The Japanese wiki hosts were not reachable either. **No wiki page was read in full by the builder.**
- Facts used came from the owner's document and from the coordinator's Japanese web-search snippets (not the full pages, unverified beyond that): Telekinesis (念動), Dimensional Storage that stores anything except living things (次元収納), Self-Repair / High-Speed Self-Repair / Self-Evolution with a per-stone magic-stone value (so Predation pays by the tier of the mob the stone came from), Dimensional Leap (次元跳躍), and that becoming a semi-divine / divine sword costs the ego (準神剣, 神剣化), which is why Divine Sword Release is a TEMPORARY release with a long cooldown.
- "Sword Lord's Haki" and "Elemental Synthesis" are the owner's design, not checked against canon. They are shipped as designed.
- The Tensura-style names (Magic Sense = Magikansi, Stomach = Ibukuro, Thought Communication = Nenka, Multilayer Barrier = Taju Kekkai, Ultra-Speed Regeneration = Chousokuryou, Demon Lord's Haki = Maka, Black Flame = Kuroen, Black Lightning = Kurokaminari, Spatial Motion = Kuukan Idou, Spatial Blade = Kuukan Zandan) were given by the coordinator. **They were not verified against the wikis, the kanji in the lang file (`.jp` keys) are not verified, and the name Maka in particular is unverified.**
- Not fetched at all: every page of the English and Japanese wikis for Shishou.

## How it plays

- **Wild:** one sword per world appears by command (`/multiverse shishou spawn`), by the creative tab entry (a bare stack adopts only if no Shishou exists, otherwise it dissolves) or, very rarely, by a daily roll near a random player (chance 0.004 per day, only while none exists, ring 40 to 80 blocks, never loads a chunk). He flies, hunts hostile mobs within 24 blocks, charges, strikes, eats the drops and gains EXP.
- **Dormant:** after 25 kills or 7 days he embeds himself in the ground. The pose is the dormant clip.
- **Claim:** right-click him. A recovered sword is reserved for his old wielder for 7 days.
- **Held:** key **H** throws or recalls him (Telekinesis), **J** casts the selected skill, **K** selects the next castable skill. Sneak + use opens Stomach.
- **Lost:** if the body is lost (void, a destroyed stack, a ghost) the recovery plan returns him to his wielder, or sets him dormant at home or at spawn. The whole world never has two.
- **Level:** EXP curve `toNext(L) = round(25 + 6L + 0.35L^2)`, maximum level 100. Mana pool `100 + 8*L + 6*steps`, regeneration `(0.6 + 0.04*L)` per second, computed lazily (no per-tick work). Strike damage `6 + 0.45*L + 0.5*steps + element bonus (up to +4)`, never above `strikeCap` (60). Melee attack on the item `7 + 0.12*L`, capped at +12.

## The 31 skills

Every skill is decided on the server, has a SERVER config switch (`[skills]` in `nusmp-shishou-server.toml`, default on), uses the sword's mana, has a cooldown, and is capped. Absorbed skills have no level: Skill Eater steals them from monsters with a chance by monster (a bounded table: exact ids first, then keywords; `stealChancePercent` scales it).

| skill | kind | unlock | mana | cooldown (ticks) | effect |
|---|---|---|---:|---:|---|
| Telekinesis | passive | L1 | 2 | 20 | the flying body; throw and recall (H) |
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

## Models, art, HUD

- **3D sword** built from the reference image: eight parts (pommel and grip lathe, tang, gold guard, ornament, the head with its gem, the blade loft, the cyan inlay), about 4,965 quads with the glow layer; sampled 1024 skin and a glow mask; length 1.71 blocks. In hand it is drawn by the mod's weapon renderer (`WeaponRenderer`) like every weapon; the old sprite stays as the inventory icon.
- **Entity** draws the same mesh, animated by the animation file; beyond 48 blocks it draws an eight-cube proxy (`geo/entity/shishou_sword.geo.json`), beyond the cull range nothing.
- **Previews** (made by `tools/shishou/shishou_preview.py`): `docs/previews/shishou/*.png` (sword against the reference, guard and head close-up, the four states, dormant in the ground, inventory icon, HUD, in-hand).
- **HUD** `shishou_hud`: level, mana, selected skill, barrier / sense / stealth timers and the Appraisal readout; movable with the mod's GUI-move screen.
- **Engravings** (rule 6): severance 2, barrier piercing 2, magicule absorption 2, energy steal 1, magic interference 1 (`item/WeaponEngravings.java`), and the franchise engravings `Teacher's Insight` and `Devoured Power`.

## Voice (no audio exists yet)

`tools/audio/shishou_lines.json` lists 41 lines in 18 groups with the voice id `null` in its header. `ShishouVoiceLines` carries the same words as text; every moment is Thought Communication text until audio is registered. Speech rules (`ShishouSpeechGate`): a per-wielder cooldown per group, an 80-tick gap between lines, only the wielder (and listeners within 32 blocks when broadcast is on). Config `relic.shishouVoice` switches the speech.

**One command to finish once a voice is chosen:** put the voice id into the header of `tools/audio/shishou_lines.json`, then run `python3 tools/audio/shishou_voice_generate.py` (it refuses to run while the voice is null) followed by `python3 tools/audio/shishou_voice_convert.py` (converts to ogg, writes `sounds.json` entries, updates the `REGISTERED` block). `ShishouVoiceAssetsTest` keeps the manifest, catalogue, sounds.json, files and lang text in step.

## Server and multiplayer (rule 9)

- All state is decided on the server. The client draws and asks: two packets to the server (key press: one byte; appraisal request: one id), two to the client (state sync to the wielder only; the appraisal reply). Every packet validates its data and sender on the server and is rate limited per player.
- Per-player state is keyed by UUID and dropped at logout, death, respawn, dimension change and server stop.
- Common code names no client class (`ShishouSourceScanTest`); the client hooks are set by the client.
- Config is one SERVER file, `nusmp-shishou-server.toml` (nothing of Shishou changes only looks, so no client config). Keys: `relic.shishouEnabled`, `relic.shishouVoice`, `wild.*`, `combat.*`, `minion.*`, `telepathy.*`, `sense.*`, `storage.rows`, `skills.<skill>`.
- The `/multiverse shishou` command: `spawn`, `status`, `reset`, `recover`, `level set <n>`, `skills`, `broadcast <true|false>`, `stomach auto <true|false>`.

## Worst-case cost (rule 11)

Only one Shishou exists, so every per-sword number is a world-wide number.

| part | worst case |
|---|---|
| holder hook (`inventoryTick`) | per stack in an inventory: one token compare (O(1)); the full upkeep once a second, only for the holder |
| server tick | one counter test per tick; once a second a flag test, one window sweep, one repair check; clones only while any live (every 10 ticks, at most 6) |
| kill / drop listeners | O(1) unless a sword body or a linked wielder is involved |
| the body (entity) | one entity; goals retarget at most once per 20 ticks with one capped box query (24 blocks, at most 12 candidates); a strike every 24 ticks; the Rukh trail 2 to 3 particles every third tick; ground search at most 40 blocks vertically and 8 sideways tries |
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
- CI self-tests, to be added to the smoke list: `relic.shishou`, `relic.shishou_claim`, `relic.shishou_skills`.

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
