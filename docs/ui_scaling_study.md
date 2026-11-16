# UI hub and early / mid / late scaling: study

Status: READ-ONLY STUDY. Nothing in `src/` was changed, no version bump, no commit. This file is the only output.
Date: 2026-11-16. Mod version at the time: 4.3.0.

## 0. Plain words (rule 12)

**What would be built.** (1) One new animated hub screen that gathers the mod's many separate screens behind one key,
drawn in the style of Ascensions' Codex hub (radial nodes, rings, motes, intro) on top of No Index's discipline
(fixed stage, text fitting, pure layout classes, tests, global open / hover / click motion). (2) A scaling model
("rungs" and "bands") that makes every skill, weapon and ultimate hit usefully at any EP, early (about 1k EP),
mid (100k to 1M) and late (25M and up), using the numbers the reference mods use.

**How it is read.** The hub is opened with a key, shows tabs, every tab either shows live data or opens the old
screen. The scaling is read by players as "my skill does about X per cast at my EP, and it gets stronger with
mastery", and by us as tables (section 7).

**What is not included.** No code is written by this study. No reference mod becomes a requirement. No copied
reference code, textures or lang. No edit to Time Magic VFX (rule 3). No retuning of anything until the owner
answers the questions in section 14.

### Hard rule: soft dependency only

Only base Tensura Reincarnated and ManasCore are required (today's `neoforge.mods.toml`: `neoforge`, `tensura`,
`manascore_skill`, `manascore_storage` required; `bettercombat`, `curios` optional). **No Index, Ascensions
(`ascension`), Ascension Core (`ascensioncore`), Tensura Reincarnated Nightmares (`trnightmare`), Nightmare Utils,
Elite Tensura, Mysticism, Isekai Evolved and every other reference mod stay out of the dependency list.**
For comparison, No Index hard-requires tensura, manascore, architectury, trnightmare and nightmareutils, and
Ascensions hard-requires tensura, manascore and ascensioncore. We must not copy those requirements.
Any compatibility is:

1. gated by `ModList.get().isLoaded("<id>")` before anything else runs (the repo already does this for
   `ftbteams`, `ftbchunks`, `curios`, `voicechat`, `trnightmare`);
2. data-first (read JSON / tags those mods ship), reflection second behind try/catch, never an import of their
   classes, never a mixin target on their classes;
3. declared `type="optional"` in `neoforge.mods.toml` if declared at all;
4. covered by a self-test for the "mod absent" path (the default path).

## 1. Sources and what could not be fetched (rule 10)

Everything below comes from bytes read with `unzip`, `javap -p -c -constants`, and the jars' lang / data / wiki
JSON and Markdown. No reference code was run. English wiki first, Japanese wiki as fallback, was applied to the
Tensura Nightmares wiki pages (saved by the coordinator at `/tmp/claude-0/tn_wiki/pages/<tier>__<Name>.wiki`).

Read: `tensura-no-index.jar` (including its 285 bundled wiki .md files), `tensura-ascensions.jar`,
`tensura-reincarnated-nightmares.jar`, plus config classes of Elite Tensura, Tensura Mysticism, Minesur addon,
Hardcore NDC, MoreOverlord, Devil x Slime and Tensura Isekai Evolved.

Could not be verified:

- `ascensioncore` (the drawing library Ascensions uses: `GuiFx`, `ArcaneUi`) is not in the Ascensions jar and not
  in `/tmp/claude-0/refmods/`. Our description of the hub's rings, ribbons and motes is from call sites only.
- Nothing was seen running in a game. All "animated" descriptions are read from constants and draw code, not seen.
- `tensuramoreskills` config dump is unreadable (sentinel values 1.79e308 / Integer.MAX). Not used.
- tensura-restricted (quests / shop / gacha mod), tensuraleonboss, tensura-blade, date-a-slime, tensura-origins were
  only skimmed; Devil x Slime only for mastery. Their skill numbers are not in the tables.
- Nightmares wiki prose damage numbers are inconsistent (regex over free text, n is small); costs of 1e12 were parse
  artefacts and are excluded. Treat the Nightmares table as an order of magnitude.
- The config dump of the remaining mods (`/tmp/claude-0/ui_study/cfg/*.txt`) finished only after the tables were written; mods beyond those named in section 5.2 were not mined for numbers.
- No Gradle, so no test or build was run. "Which screen the owner dislikes" is a guess (section 3).
- Boss rung mapping for each Black Clover boss was not computed (only Lucifero, 21,000 HP with the x3, is known).

## 2. UI findings

### 2.1 Tensura No Index

| Piece | Source class / file | What it does |
|---|---|---|
| Stage base | `client/gui/v2/GlassScreen` | Fixed 640x380 stage scaled to the window; `build()` plus text / button / tab / hold / bar / region helpers; art layers through `GuiDraw.Batch`; hover lerp array; `crisp()` / `gs()` scale snapping so text stays sharp |
| Draw helper | `client/gui/GuiDraw` (`Batch`) | Fill and line batching, so a whole panel is a few draw calls |
| Global motion | `client/gui/UiLife` (ScreenEvent overlay) | Open stagger and sweep (OPEN 320 ms, SWEEP 650 ms), click ripple (RIPPLE 520 ms), hover lerp per widget, cursor trail. Applies to every screen, no per-screen code |
| Scenes | `UiScenes`, `LatticeBackdrop`, `TechBackdrop`, `FancyBar` | Animated backdrops and bars, all geometry |
| Book layout | `lore/client/CodexScreen`, `LoreJournalScreen`, pure `LoreLayout`, `FitText` | 256x192 book units, layout is a pure class so it can be unit-tested; cut text gets a tooltip |
| Settings | `SettingsGlassScreen` (key `\`) | Tabs General, HUD Layout, Gauges, Visual FX, Bosses, Audio, Theme, Controls; per-row reset; search box |
| Perf and a11y | wiki `client_performance`, `low_end_and_server_load` | Animated screens have a switch; low-end preset turns UI animation off; `FxAuto` lowers effects under 30 fps |
| Theme | `UiTheme` | RGB-slider theme |
| HUD | `HudRules`, key `]` layout | 3-layer attention model: ambient, focus, crisis |
| Checks | `GuiFitCheck`, `LoreBookCheck`, preview PNG tools | Text-fit and layout are tested outside the game |

Lesson: No Index's good part is not one pretty screen, it is the discipline: one base class, global motion, pure
layout, fit tests, an off switch.

### 2.2 Tensura Ascensions

| Piece | Source class | What it does |
|---|---|---|
| Hub | `client/CodexHubScreen` | Radial 7-point star (POINTS 7, NODE_R 17, INTRO 128 ticks): rings, core, nodes, flare, motes. Tabs (enum `Tab`): CODEX, TALENTS, PROFILE, CHRONICLE, SUBORDINATES, MODIFIERS, SETTINGS; each has a point index, colour and a `sealed()` lock. Intro plays once (`intro_seen` flag), sounds BEACON_ACTIVATE and AMETHYST_BLOCK_RESONATE, hover card, hint "Ctrl + L to close" |
| Status window | `SystemScreen` | Solo-Leveling style window, tabs [STATUS] [QUEST INFO] [SHADOWS], stat rows STR / AGI / PER / VIT / INT with flash on change, + buttons, rename EditBox |
| Others | `TalentScreen` (constellation, 10 levels per node, keystones, capstones), `ProfileScreen`, `ChronicleScreen`, `ModifierScreen`, `SubordinateScreen`, `SettingsScreen` (effect intensity, screen shake, banners), `JobChangeScreen`, `CodexScreen` |
| Toasts | `SystemClient` | Toast life 90 ticks plus a `RenderGuiEvent` overlay |
| Data | `SystemPayload`, `TalentPayload`, `ProfilePayload`, `SubordinatesPayload`, `ModifierPayload`, `SettingsPayload`, `CodexSyncPayload`, `CodexActionPayload`, `CodexAwakenPayload` | One small payload per screen, requested on open |

Only one GUI texture exists in the jar (`great_mage_tome.png`). The shaders are for world FX only. Everything on
screen is geometry. So the "cool animated UI" can be rebuilt from code geometry plus our own textures, with nothing
copied.

### 2.3 Honest note on "No Index solves early / mid / late"

No Index's own `midgame_audit` doc lists "No early game": its EP tiers sit under its tree gate. It is a strong
damage framework but not a finished early-mid-late curve. We build the bands ourselves (section 7) and use No Index
as a numeric reference.

## 3. Our UI audit

| Screen | Class | Entry | Note |
|---|---|---|---|
| Codex (data sections) | `client/ui/CodexScreen` (332 lines), `CodexPages` | `MultiverseStatusClient.KEY`, unbound by default | Likely "the current one that is bad": 6 commits since the 1.27.0 theme, tabs Overview, Status, Weapons, Relics, Bestiary, Spirit Lords, Guide, Spells, Blocks, Entities, Magic, Effects, BeastWill |
| Multiverse Status | `MultiverseStatusScreen` | Same key; compact panel injected into Tensura's menu by a mixin | Read-only |
| Grimoire | `FourKingdomsScreen`, `GrimoireSlotScreen` | Tensura "?" slot (mixin) | |
| System | `SystemStatsScreen` (I), `SystemEditScreen` (U), overlay (Y) | `SystemClient` | |
| Shop | `skilltree/shop/ShopScreen` | Shop client | |
| Others | `MeditationScreen`, `WeaponBookScreen` (M), `AvatarBuilderScreen`, `SoulForgeScreen`, `AvatarActionScreen`, `QuestScreen`, `DungeonMapScreen`, `CompatCodexScreen`, `EngravingCodexScreen`, `GuiSizeScreen`, `GuiMoveScreen`, `GsCrestScreen`, `ShishouStomachScreen`, `QuestionnaireScreen`, `ZagredScreen`, BeastWill screens | Each has its own key or command | |

Our theme toolkit already exists in `client/ui/theme` (`UiTheme`, `UiPanel`, `UiBar`, `MagicCircle` with at most 8
textured quads, `SageBlock`, `ThemedButton`, `CodexSkin`, `ToastTimeline`, options `ui.theme` / `ui.motion` /
`ui.typing` / `ui.pageToast`). Keys in use: Y U I, F8, F9, M, Z X C B N, G, R, `[ ] ; \ =`, Saiyan keypad. `H` is
only read inside `GuiMoveScreen`, so it is free for a hub key.

Finding: the mod has about 25 screens with 10+ separate entry points and no single place a player starts. The
status data already travels to the client (MultiverseSync status NBT, `SystemNet.Sync`, `SystemPointsNet`,
`ShopSnapshot`, `HubNet`), so a hub can start with zero new packets.

## 4. UI proposal: "Multiverse Hub"

One screen, opened by `H` (rebindable) and also by the existing Multiverse Status key.

**Shell.** Fixed stage in No Index style (pure `HubLayout` class, scaled to the window, text through a fit helper
with tooltips for cut text). Around it an Ascensions-style radial ring of 7 to 8 nodes, one per tab.

| Tab | Content | Reuses |
|---|---|---|
| Status | EP, magicule / aura, rung name (section 7), race / form, active effects | `MultiverseStatusScreen` data, `SystemStatsScreen` |
| Skills | skills with tier (Base / Mastered / Ultimate), mastery bar, cooldown, cost, per-cast damage at my EP | Skill registry, `SkillEvolution`, new `ScalingBands` |
| Path | evolution chains (Unique -> Ultimate), what is missing | `SkillEvolution` pair tables |
| Attunement | meditation, resonance | `MeditationScreen` |
| Quests / Dungeons | tracker, map | `QuestScreen`, `DungeonMapScreen` |
| Codex | the data sections | `CodexScreen`, `CodexPages` |
| Multiverse | Four Kingdoms, Beast Will, compat | `FourKingdomsScreen`, BeastWill screens, `CompatCodexScreen` |
| Settings | UI scale / move / hub FX | `GuiSizeScreen`, `GuiMoveScreen`, new `ui.hubFx` |

**Animations** (all pure geometry or our existing textures; none copied):

- Intro, once per profile (flag in client config): rings expand over 128 ticks, nodes light one by one, two UI
  sounds. Skipped on click or when `ui.motion` is off.
- Open sweep 320 ms, stagger per widget, hover lerp, click ripple 520 ms (same timings No Index uses, our own code).
- Idle: slow ring rotation, a fixed pool of 24 motes (preallocated), node pulse on a tab that has news.
- Sealed tabs show a lock until their feature is unlocked, with a hint line.
- Value change flash on stat rows (Ascensions style).
- `ui.motion` off: everything static, clock frozen.

Not included: any 3D scene, shaders, new GUI textures in release A (we reuse `MagicCircle` textures), a rewrite of
the old screens' insides.

## 5. Reference numbers: damage and scaling systems

### 5.1 No Index (`core/combat`: `DamageFormula`, `MoveTier`, `EsperClass`, `CombatConfig`, `EpStats`, `PowerScaling`)

`damage = base + min(EP * epScale, base * capMult) + targetMaxHp * pct`. EP = max aura + max magicule.
The percent part is true damage, capped at 35% of target max HP per hit and x0.4 against players. The EP part is
capped at 10x base. The flat part is armour-blended, `bypass` is the share of armour ignored.

| MoveTier | Cooldown | Cost (% of pool) | epScale | pct max HP | bypass |
|---|---|---|---|---|---|
| UTILITY | 3-5 s | 1% | 2e-5 | 0 | 0 |
| BURST | 8-15 s | 4% | 6e-5 | 2% | 25% |
| ULTIMATE | 30-60 s | 15% | 1.5e-4 | 8% | 50% |

| EsperClass | damage / cap | scale | pct | bypass floor | strips | cost |
|---|---|---|---|---|---|---|
| NONE | 1 | 1 | 1 | 0 | no | 1 |
| UNIQUE | 2 | 2 | 1.25 | 0.25 | no | 0.85 |
| LORD | 4.5 | 4.5 | 1.35 | 0.5 | no | 0.8 |
| KING | 9 | 9 | 1.5 | 0.75 | yes | 0.7 |
| GOD | 50 | 25 | 2.0 | 1.0 | yes | 0.5 |

Per-hit caps: utility 120, burst 900, ultimate 4000. Per-cast bands (docs `class_scaling`): basic <= 2,500,
special 2,000-8,000, super 10-15k+; King 5k / 10-25k / 40-80k. Yardstick: against Veldora (24,000 HP) a Lord needs
about 6 specials (4,050 each), a King about 3 (8,100), a God 1 (40,000). Mastery does not scale damage there, it
gives Esper Level 0-5 and level 5 wakes the ultimate. Soul tiers by EP: T1 10k, T2 100k, T3 1M, T4 5M, T5 25M.
Gear EP bands: Rare 45k-225k, Unique 225k-750k, Legend 750k-2.5M, God 2.5M-10M, Genesis 10M-50M. Wielding power
`0.2 + 0.8 * ratio^1.5`, nothing under 10%, curse under 25%.

### 5.2 Other reference mods (unique and ultimate skills)

| Mod | Source | Numbers |
|---|---|---|
| Elite Tensura | `ETUltimateSkillConfig`, `ETUniqueSkillConfig`, `ETMagicConfig` | Every skill is an "X / XMastered" pair: mastery about doubles damage, cooldown x0.5-0.75, range x1.4-1.75. Void Sovereign Null Slash 150 / 300 dmg for 1500 MP; Soul Collapse 60 / 100, charge x1.5 / x2.5, 5000 MP, acquire 50,000 MP. Gaia Spikes 45 / 3000 MP. Beelzebuth predation 300. Unique Void Edge 25 / 50 dmg, 800 MP, cooldown 4 / 3 s. Hecate +30% / +75% spell damage. Magic Kamehameha 20 / 35 for 2500 / 5000 MP |
| Tensura Mysticism | configs | Gravitational Void 500,000 MP, damage 50-250, cooldown 420 / 300 ticks; restricted decimate 25 / 75; mastery 500 / 1500 |
| Minesur addon | `getRequiredEP` | 750k / 1M / 1.5M / 2.5M EP gates; max mastery 1000-2000 |
| Hardcore NDC / MoreOverlord / Devil x Slime | configs | Mastery 1 / 2 / 5; 2000; 0 or 1000 |
| Isekai Evolved | `TrevolvedConfig`, `ServerBalanceConfig`, `maintenance-config-catalog.json` | Per ability and per mode server balance: enabled, damage / cooldown / aura cost / magicule cost multipliers, family multipliers and range, allow flags, blocked ids. A good model for our per-skill override table |
| TR Nightmare | `NightmareDamageCapConfig`, `NightmareBalanceConfig`, `NightmareUltimateConfig`, `DungeonEpConfig` | Cap per hit 60% of max HP universally; bosses 750 flat or 4% per hit, boss budget 12% per second, overflow x0.25; resistance reduction 0.5, nullification 0.9, attribute cap 0.9, attack speed cap 2.8; heal fatigue (40-tick window, 12000 ticks, 5 stacks); ultimate material cost 5-75 per component; dungeon EP bonus 150k / 500k / 1M / 2M |
| Ascensions | `TalentPayload` data | Talent points from stars, max EP growth and XP; 10 levels per node |

### 5.3 Nightmares wiki class ladder (reference only, no dependency)

142 pages: unique 71, lord 50, king 11, god 6, dragon 4. Medians (cost = MP to obtain, points = to master).

| Class | Obtain cost (median MP) | Points to master | Passives / page | Actives / page | Prose damage per cast (n) |
|---|---|---|---|---|---|
| Unique | 75K (40K+) | 1000 (40 of 41) | 0.7 | 1.6 | median 80, p25 40, p75 180 (19) |
| Lord | about 1.5M (300K-5M+, common 500-900k) | 5000 (29 of 34; 25000 for 4) | 2.5 | 4.3 | median 125, p25 50, p75 250 (38) |
| King | 2.4M (500K-3.9M) | 5000 (1500 once) | 2.5 | 3.1 | median 90, p25 50, p75 100 (9) |
| God | 30M (2.5M-35M EP) | 5000 | 1.3 | 2.3 | none found |
| Dragon | 1M | 5000 | 0.5 | 1.0 | 1000 (2) |

Reading: the wiki ladder is about cost and mastery effort (Unique cheap, Lord 20x, King 30x, God 400x), with flat
per-cast damage and many more skills per higher class. That fits our `UltimateClass` price ladder (section 6) and
says the class jump is about breadth and price more than raw per-hit damage.

## 6. Our current model

`balance/BalanceLaw.damage`: `r = raw * epScale(caster, mastery) * lateGameScale * (0.85 + 0.3m) * spellDamagePercent *
ManaLaw.powerMult`, plus `DamageSliver`, then caps (PvP at most `pvpHitCapPercent` 40% of max HP per hit, mobs x
`mobDamageMultiplier`). `epScale = min(12, 1 + (log10(EP) - 3) * 2.5)`: x1 at 1k, x3.5 at 10k, x6 at 100k, x8.5 at
1M, x11 at 10M, cap 12 near 25M. `EpMastery.ramp` (`epNeedsMastery`, default true) grows the EP part from x1 at
mastery 0 to full at mastery. `LateGameScaling = 1 + 1.25 * log10(maxMagicule / 10,000)`, cap 6, players only.
Grimoire x1.75. `UltimateClass` (appended-only enum):

| Class | epCap | costPct | costRef | maxMastery | killBelow | shredBelow |
|---|---|---|---|---|---|---|
| LORD | 8 | 2 | 2000 | 10000 | 0 | 0 |
| KING | 10 | 3 | 3000 | 25000 | 0 | 0.70 |
| GOD | 12 | 5 | 4000 | 50000 | 0.60 | 0.80 |
| DRAGON | 12 | 4 | 3000 | 15000 | 0 | 0 |

`Tier` is BASE / MASTERED / ULTIMATE / ULTIMATE_MASTERED (x1.2 ultimate-mastery bonus). `SkillEvolution` runs every
5 s and turns a mastered Unique into its Ultimate when EP >= `EVOLUTION_MIN_EP` (default 0). Black Clover bosses are
3x health and 3x damage (`bossRules.blackCloverStatMultiplier`, hit capped at half the victim's max HP); boss DPS
budget 6% of max health per second (`docs/boss_dps_budget.md`).

Legacy damage of a raw-10 mastered page (grimoire included):

| EP | 1k | 10k | 100k | 1M | 10M | 25M | 100M |
|---|---|---|---|---|---|---|---|
| Damage | 20 | 70 | 226 | 534 | 968 | 1176 | 1358 |

## 7. Scaling proposal: rungs and bands

Rungs are read from EP only (our own `ScalingRung` pure class). They borrow No Index's soul tier edges as a
reference scale; the mod never reads No Index to compute them.

| Rung | EP | Name | Reference foe HP, own-world ladder | Tensura-world ladder |
|---|---|---|---|---|
| R0 | < 10k | Early | 60 | 60 |
| R1 | 10k-100k | Early-mid | 400 | 400 |
| R2 | 100k-1M | Mid | 3,000 | 3,000 |
| R3 | 1M-5M | Mid-late | 24,000 (Veldora yardstick; Lucifero 21k) | 24,000 |
| R4 | 5M-25M | Late | 60,000 | 120,000 |
| R5 | >= 25M | Endgame | 150,000 | 1,000,000 |

Per-cast target comes from "casts to kill": Lord special = refHP / 6, basic = special x 0.5, super = special x 3.
Class ratios against Lord: Unique 0.45, Lord 1, King 2, God 10 (No Index's own King 2x and God 10x yardstick,
Unique from its 2 vs 4.5).

| Rung | Lord special target (own-world) | Tensura-world |
|---|---|---|
| R0 | 10 | 10 |
| R1 | 67 | 67 |
| R2 | 500 | 500 |
| R3 | 4,000 | 4,000 |
| R4 | 10,000 | 20,000 |
| R5 | 25,000 | about 167,000 |

**Band.** floor 0.5x target, ceiling 4x at R0-R1 and 2x at R2 and above. Mastery moves the hit inside the band, from
floor (unmastered) to target (mastered), then x1.2 for a mastered Ultimate; this matches Elite Tensura's roughly
doubling by mastery. Early game stays a power fantasy (a raw-10 page at R0 is about 4.5x the target and is
intentionally left alone by the ceiling rule: the ceiling applies only above R1 as a clamp, R0 uses the legacy value).

**Finding.** Legacy vs target (geometric-mid EP, raw-10 page): R0 0.22, R1 0.56, R2 1.37, R3 5.96, R4 10.08, R5
19.73 (target divided by legacy for the shortfall rungs). So legacy tracks the band through R2, then flattens and
falls short by about 6x at R3, 10x at R4, 20x at R5. The late game is the part that needs work.

**Fix.** A post-step clamp in `BalanceLaw` (floor only, optionally ceiling) behind a server setting
`scaling.bands = off | floor | full`. `off` restores today's behaviour exactly (the default in release A). The
existing PvP 40% cap and boss DPS budget still run after it and win.

**Costs and cooldowns.** No Index tier percentages (utility 1%, burst 4%, ultimate 15% of pool) combined with the
class multipliers (Unique 0.85, Lord 0.8, King 0.7, God 0.5) and `UltimateClass.price` as the flat floor at low EP.
Cooldown windows 3-5 / 8-15 / 30-60 s, then the existing `spellCooldownPercent` 60.

**Boss interplay.** Black Clover bosses stay x3 (`blackCloverStatMultiplier`) and are mapped to rungs by post-x3 max
HP, so a boss never sits at a rung whose floor kills it in two hits. Nightmares-style caps (60% max HP per hit;
bosses 750 flat or 4%) are shown as references, not adopted. Unique -> Ultimate evolution should get an EP gate
(proposal 100k, today 0).

**Per-skill override table.** `scaling_overrides.json` (server config), in Isekai Evolved's shape: per skill and mode,
`enabled`, `damage`, `cooldown`, `cost`, `range` multipliers. Default empty.

## 8. Soft compatibility

| Mod | Read | Method | If absent |
|---|---|---|---|
| No Index | gear EP bands, wielding requirements, skill tags, damage types | `isLoaded("tensuranoindex")`, then data JSON; optional reflection on `EpStats` / `EsperClass` / `Tiers` in try/catch | nothing changes |
| Ascensions | talent EP growth, nothing else | `isLoaded("ascension")`, data only | nothing changes |
| Nightmares | class costs, ultimate tags (already `isLoaded("trnightmare")` in code) | data only | nothing changes |
| Others | none | none | none |

Never imported, never mixin-targeted. The `#tensura:engraving` tag reading (rule 6) already works the same way.

## 9. Worst-case cost bounds (rule 11)

**Client draw.**

| Mode | Quads per frame | Allocations per frame |
|---|---|---|
| Hub full | background <= 1,200, tab panel <= 400, 24 motes | 0 (preallocated arrays) |
| Hub low | `MagicCircle` 8 quads + static panel, <= 120 | 0 |
| Hub off (`ui.motion` off) | static panel <= 120, clock frozen | 0 |
| Screen closed | 0 | 0 |

Nothing is drawn before `super.render` (the `ScreenBlurGuardTest` rule), distance culling does not apply (a GUI),
a low-end preset selects "low".

**Network.**

- Release A: 0 new packets (hub reads the existing status NBT, `SystemPointsNet`, `ShopSnapshot`, `HubNet`).
- Release B: one request packet (tab enum, sender validated, 1 per 10 ticks per player) and one `HubSync` reply
  of <= about 1.5 KB to that player only, on open and on change (diff send, <= 2 per second). Worst case per
  player: 2 packets/s, 3 KB/s while the screen is open; 0 while closed. The server never trusts the client's rung
  or EP; it recomputes.

**Server.** The band clamp is a few comparisons per hit. The rung is cached per player per second (`BoundedCache`),
cleared on logout / death / dimension change. No new ticks, no entities, no world scans.

**Multiplayer (rule 9).** All rules server side; the client only draws; per-player state keyed by UUID; `ui.hubFx`
is a CLIENT config; `scaling.*` are SERVER configs.

## 10. NEW / REPLACED / KEPT (rules 2 and 5)

| | Items |
|---|---|
| NEW | `MultiverseHubScreen` plus `HubLayout`, `HubNodes`, `HubFx` (pure + draw), key `H`, `ui.hubFx` client option, `ScalingRung`, `ScalingBands`, `scaling.*` server config, `scaling_overrides.json`, `HubSync` + request packet (release B), soft-compat readers, tests and self-tests |
| REPLACED | Nothing is removed. In practice the hub replaces the role of the Multiverse Status key as the starting point, and the band clamp supplements `BalanceLaw`'s late-game flattening (optional, off by default) |
| KEPT | `CodexScreen`, `MultiverseStatusScreen`, `FourKingdomsScreen`, `SystemStatsScreen`, `SystemEditScreen`, `ShopScreen`, `MeditationScreen`, `WeaponBookScreen`, `QuestScreen`, `DungeonMapScreen`, `CompatCodexScreen`, `EngravingCodexScreen`, `GuiSizeScreen`, `GuiMoveScreen`, every key, `BalanceLaw` formulas, `LateGameScaling`, `UltimateClass`, `Tier`, `SkillEvolution`, boss rules |

Nothing here is a replacement of an existing asset unless the owner answers question 1 as yes and asks for the old
Codex screen to be retired (then it is removed only on request, keeping data ids).

## 11. Phased plan

**Release A (minor).** Hub shell: tabs open the existing screens, geometry animation, `ui.hubFx`, key `H`, 0 new
packets. Pure `ScalingRung` / `ScalingBands` with unit tests and a self-test (`SelfTests` prefix added to the smoke
list). `scaling.bands` defaults to `off`. Tests: layout fit, text fit, `ScreenBlurGuardTest`, `ServerSafetyTest`,
`PerfSourceScanTest`, `ConfigFileNamesTest`. Lang merge.

**Release B (minor).** Hub data tabs (Status, Skills, Path) with `HubSync` and request packet, per-skill override
table, band clamp wired into `BalanceLaw` (still default off), boss rung mapping table, preview PNG tool.

**Release C (minor).** After a play-test and the owner's sign-off: band default `floor`, extended late-game curve,
Unique -> Ultimate EP gate, soft No Index / Nightmares data readers, hub intro polish.

Every release follows rule 12: version bump in `gradle.properties` and `neoforge.mods.toml`, changelog, `[smoke]
[strict]`, CI green, pre-release with the jar. Models: none of these releases need new 3D models (rule 14 does not
apply), the hub is 2D geometry.

## 12. Risks and caveats

- Numbers in section 7 are derived, not play-tested. Releases A and B ship them off by default for that reason.
- The owner's "No Index solves early, mid and late" is only partly true (section 2.3).
- The hub is built blind of `ascensioncore`; polish will come from our own iteration, not from reference code.
- Nightmares wiki medians are noisy.

## 13. Reference scratch

Scratch notes only in `/tmp/claude-0/ui_study/` (not part of the repo).

## 14. Questions for the owner (yes / no)

1. Is the Codex / Multiverse Status screen the "current one" you dislike?
2. Should the hub open on `H`, and should the existing Multiverse Status key also open it?
3. Should the bands follow our own-world HP ladder (ends at 150k) rather than No Index's Tensura-world ladder (ends at 1M)?
4. Should the band floor become default ON in release C, after a play-test?
5. Are the class ratios Unique 0.45 / Lord 1 / King 2 / God 10 acceptable?
6. Do you want both: a radial hub (Ascensions style) plus glass-stage tab pages (No Index style)?
7. May Black Clover bosses be mapped to rungs by their post-x3 max HP?
8. Do you accept `ui.hubFx` full / low / off, with low as the default on low-end clients?
9. Should soft compatibility read No Index and Nightmares data only, with no reflection at all?
10. May Unique -> Ultimate evolution get a minimum EP gate of 100k (today 0)?
