# Boss reference study: Leon Cromwell (TensuraLeonBoss) against our bosses

Research and report only. Nothing in `src/`, `tools/` or the changelog was touched, nothing is built, no version bump. Written for the owner's request that the
TensuraLeonBoss mod is the reference for how our bosses are designed, look, fight and scale.

## 0. What was read, what was inferred

| Source | How it was read | Trust |
|---|---|---|
| `/tmp/claude-0/refmods/tensuraleonboss.jar` (mod id `foxboss_leon_cromwell`, v1.0.2, author AblazeAQZL, deps GeckoLib 4.7.6, Tensura 2.0.0.1+, ManasCore 4) | `unzip` of every json/lang/data file (READ in full); `javap -p -c -constants` of the Java classes (READ: constants, field and method names, call targets). **Not decompiled**: control flow was followed only where a constant or call made it clear. | Numbers are exact. Behaviour described as "inferred" is reasoning from method names and call order. |
| `/tmp/claude-0/tensura_jar/tensura.jar` | `unzip -p` of `entity_existence`, `loot_table`; `javap` of `HinataSakaguchiEntity` attributes. | Exact for the numbers quoted. |
| Our repo | Java and docs READ directly (files named in each row). | Exact. |
| Not done | Nothing was run in a game. No sound file of Leon exists to listen to (his jar has none). Tensura's EP engine was not read. CLAUDE.md rule 10 (wiki research) does not apply: no canon lookup was needed. | |

---

## 1. How Leon is built

### 1.1 Entity, numbers, scaling (READ)

* Class `dev.foxboss.leon.entity.LeonEntity` extends **Tensura's `TensuraHumanoidEntity`** (so it inherits Tensura's tame/owner/inventory machinery; `OwnerHurtBy/OwnerHurtTarget/FollowOwner` goals are registered, the boss bar hides when it is tamed). `MobCategory.MONSTER`, hitbox 0.65 x 2.2, eye 1.96, `fireImmune`, tracking range 12 chunks, update interval 1, `xpReward = 100`, `setPersistenceRequired()`.
* Attributes (`createAttributes`): **max health 4000, armor 16, attack damage 150, movement 0.28, follow range 48, knockback resistance 1.0, step height 1.0**. Health is also forced to 4000 once by a data-pack tick function (`leon_stats/init_health.mcfunction`, tag `hp_4000_v1`; the function comment says it must not heal later damage).
* **EP lives in data, not code**: `data/foxboss_leon_cromwell/entity_existence/leon_cromwell.json`: `min_magicule 1,000,000 / max_magicule 1,200,000`, `min_aura 600,000 / max_aura 850,000`, `spiritualHealth 4000`, and **18 real Tensura skills** (hero_haki, thought_acceleration, universal_perception, chant_annulment, multilayer_barrier, ultraspeed_regeneration, spatial_motion, warp_portal, solar_beam, solar_wave, aura_sword, aura_slash, and six attack/condition resistances). Same file format Tensura uses for Hinata (3000 hp, 50 armor, 60 attack, 536,331 magicule, 500,000 aura, 27 abilities) and Shizu (143,543 aura, 50,000 magicule, 10 abilities).
* **Scaling: none found.** No `getDifficulty`, no player count, no per-player or per-world factor anywhere in `LeonEntity` (0 hits in the bytecode). Strength is the fixed attribute set plus the EP range (the min..max range suggests a roll at spawn; inferred, Tensura's loader was not read).
* Damage per swing = 150 x the attack's multiplier (`doHurtTarget(target, mult)`), multipliers 0.85 to 1.8 (table below). The spirit "coatings" add 100 (sword) and 25 (body) holy damage per contact, configurable, on custom damage types that bypass armor, shield, cooldown and knockback and carry Tensura's `bypass_protection_enchantment` tag (barrier bypass level 3.0, element HOLY).

### 1.2 AI, abilities, timing, telegraphs (READ + inferred)

No health phases were found. Leon is a **state machine with a few readable, reactive behaviours**, not a power ladder:

| Behaviour | Numbers (READ) | Notes |
|---|---|---|
| Goal order | 0 `ScriptedGoal` (plays a scripted clip), 1 float, 2 `SwordCombatGoal`, 3 `FollowOwner`, 5 wander (0.75), 6-7 look. Targets: owner-hurt, owner-target, hurt-by, nearest `LivingEntity` filtered by the entity tag `hostile_targets` (Yuuki only). | Combat chase speed 1.25. |
| **Attack timeline** (`LeonAttackTimeline`) | 6 clips, each with **clip length, first hit tick, optional second hit tick, damage multiplier**: attack 30/13/-/1.0, slash_horizontal 28/12/-/1.1, slash_rising 30/13/-/1.1, cleave_heavy 38/17/-/**1.8**, slash_advancing 31/13/-/1.1, slash_cross_combo 38/12/**22**/0.85 | The damage is applied on the animation's hit tick (`tickSwordAttack` -> `doHurtTarget`), so the animation IS the telegraph. Sweep sound plays at the hit. |
| **Guard stance** | guard enters within 5.0 blocks, exits at 6.0, walks at 0.9 speed; 24 to 48 ticks; at least 2 contacts and 3 block poses; 4 ticks gap between poses | Synced boolean `COMBAT_GUARD`. |
| **Sword block** | 70% chance on a hit while guarding, 12-tick block action, 4 block animation variants (left, high, hanging, default), reaction window 4 ticks | A blocked hit is remembered (`rememberDefendedAttack`) and answered (`reactToAttack`, counter timer `counterReadyAt`). |
| **Dodge** | 30% chance, 8-tick action with invulnerability (`dodgeInvulnerability`), 3 clips (left, right, back), `dodgeDirectionFree` checks the space, `negatesDodge` lets some damage types ignore it | Dodge cooldown field. |
| **Combat teleport** (`LeonCombatTeleport`) | triggers after about 61 ticks (3 s) of not reaching the target, destination 3 to 12 blocks from the target, area limit 16 blocks (256 sq), cooldown 160 | Anti-kiting. |
| **Real Tensura skills** (`LeonCombatSkills`, through ManasCore `SkillAPI`/`SkillStorage`) | Light magic alternates **solar_beam / solar_wave** every `lightMagicCooldownTicks` (200), range 5 to 48 blocks (25 to 2304 sq) and line of sight; **hero_haki** when a nearby target is not yet afraid, every `heroHakiCooldownTicks` (300), within 8 blocks; **multilayer_barrier** every 160 ticks when the barrier attribute is 0; **aura_sword** re-pressed every 100 ticks; **aura_slash** (cooldown 40) fired on a sword hit. Cast duration = the skill's own `Magic.getCastingTime`, shown with the `CAST` clip. | Because they are real skills, **Tensura draws the cast bar, particles and sounds**; the mod adds none. |
| **Spiritron coatings** (his own mechanic) | warm-up 20 ticks, duration 200, cooldown 400 (all config), start cost 5000 magicules, upkeep 200 per second, only inside 6 blocks, contact every 10 ticks within 0.6 blocks | Synced as **game-time longs** (`SPIRITRON_ACTIVE_AT`, `SPIRITRON_ENDS_AT`), not per-tick packets: the client computes the glow from the clock. Same trick for clips: `ACTION`, `ACTION_START`, `ACTION_SEQUENCE` (a counter that re-triggers the same clip). |
| Config (`LeonConfig`, COMMON toml) | 12 keys: spawn reference (Hinata/Shizu roll), light-magic and haki cooldowns, six spiritron numbers plus contact interval/range, start cost, upkeep | Everything that tunes the fight is a key. |
| Debug | `/foxboss_leon debug spiritron_sword|body|stop|status`, a `DEBUG_ANIMATION` data slot that plays any clip on demand, a `PREVIEW` action | Lets the author watch every clip in game. |

Death: `die()` stops all skills, plays the DEATH clip (46 ticks of `deathTime`), hides the bar, then `broadcastEntityEvent(60)` (inferred: the vanilla death-poof event) and removal.

### 1.3 Boss bar (READ)

Vanilla `ServerBossEvent` (YELLOW, PROGRESS) carries name and fill, updated every tick from health/max. A custom payload `leon_boss_bar` tells the client which bar ids are Leon's; `LeonBossBarOverlay` (`CustomizeGuiOverlayEvent.BossEventProgress`) cancels the vanilla draw and paints `textures/gui/boss_bar/leon_frame.png` (288x76) with `leon_progress.png` clipped to 182 px of fill. Cleared on logout. Hidden while tame.

### 1.4 Drops and spawn (READ / inferred)

* **No loot table and no drops config in the jar** (inferred: drops are what Tensura's humanoid base gives: his equipment/inventory plus 100 xp). The sword he holds is Tensura's `MOONLIGHT`.
* Spawn: natural, biome modifier `add_spawns` weight 5, min=max 1, biomes `#tensura:otherworlder_spawn`, plus `add_spawn_costs` (charge 4.0, budget 4.0); `checkSpawnRules` rolls **Tensura's own spawn-rate config** (otherworlder list, Hinata or Shizu chance, config `naturalSpawnReference`). Spawn egg item. No altar, no summon item, no arena.

### 1.5 Model, animation, look (READ)

* GeckoLib 4.7.6 (`GeoEntityRenderer`). `geo/leon_cromwell.geo.json`: format 1.12.0, **128 cubes in 26 bones**, texture **128x128**, visible bounds 4 x 5.25. Bones include `waist/body/upBody`, three **cloak** bones, per arm `Arm / LoreArm / PF PF2 PF3` (fingers), `head/hair/hair2`, legs with `foreLeg`, and **`held_item_right/left`** (a `BlockAndItemGeoLayer` puts the real Moonlight item in the hand). Largest cube 10.5 px, median 5 px, 4 cubes of 1 px: it is **blocky, but finely cut**.
* `textures/entity/leon_cromwell.png`: 128x128, **54 distinct colours**, hand-pixelled in flat colour patches (dark navy coat, gold trim, red lining, white shirt, pink skin): a clean pixel-art look, no baking.
* `animations/leon_cromwell.animation.json` (1.46 MB, Bedrock animation 1.8.0): **21 clips, about 8,490 keyframes**: idle 4.8 s, walk 1.12, run 0.72, death 2.3, sit_down/sit_idle/stand_up (2,000+ keyframes each), **5 attack clips (1.4 to 1.88 s) plus the base attack 1.48 s**, **4 block clips (0.6 s)**, guard_idle 1.2 (loop), **3 dodge clips (0.4 s)**. A `LeonLocomotion` class measures the real ground speed and sets the walk/run playback rate (so feet do not slide, 0.19 / 0.08 / 0.12 style constants).
* **Glow/VFX layers** (`LeonSpiritLayer`, `LeonSpiritEffects`, `LeonSpiritRenderTypes`): an additive, full-bright "spirit" shell drawn over the bones `body, upBody, RightLoreArm, LeftLoreArm, Right Leg, Left Leg, Cloak2` (inflated by about 0.01 to 0.065 blocks), a glowing sword blade shell, a sampled **blade-trail ribbon** (`Sample/Point/Blade` records), three extra textures (`leon_spirit` 128, `moonlight_spirit` 64, `spiritron_glow` 64). The glow follows the synced start/end clock.
* Sounds: **none shipped** (vanilla `PLAYER_ATTACK_SWEEP` only).

### 1.6 What Leon teaches (the five principles)

1. **The animation is the telegraph and the hit timer.** Damage lands on a stated tick of a clip made for that attack.
2. **Few moves, each with its own clip**, plus reactive defence (block, dodge, guard, anti-kite teleport), not a long menu.
3. **Real engine skills for the big magic** (the host mod draws the effects), with the boss's own state machine choosing.
4. **Stats are data plus one config file**; the boss is placed in the host mod's EP economy through `entity_existence`.
5. **Look = bone count + hand-keyed motion + a state-driven glow layer**, not mesh density.

---

## 2. Our bosses, as built today (READ)

### 2.1 The framework (all human and monster bosses stand on it)

* `entity/zogratis/ZogratisBoss` (1,140 lines) + `arc/ArcBoss` thin layer. **Percentage Power System** (`PowerCurve`): power opens at 20%, steps to 40/60/80 at health 85/65/45%, and **100% (the Gates, a 60-tick invulnerable transformation) under 25%**. Each step scales damage x0.60/0.80/1.00/1.25/1.60, cooldown x1.00/0.92/0.70/0.55/0.42, speed, wind-up x1.00 to 0.75, disciple cap, volley slots, ultimate gap 2400 to 900 ticks, mana pool (unbound from 60%). `clampDamage` stops one hit skipping a step.
* **Moves** (`Move`, `MoveScheduler`, per-boss `*Table` of `MoveSpec` rows: id, label, minPower, wind-up, cooldown, range band, weight, ultimate, crowd, recovery). One decision per second, weighted random among moves allowed by power, distance and cooldown; wind-up at least 8 ticks (ultimate 30); an ultimate leaves the boss **exposed** (x1.25 damage) for about 3 s.
* **Telegraphs**: `Tells` (server-sent vanilla **dust particles**: ring, line, column, gathering motes) in the character's colour, plus some `VfxShape` effects (wave 3 signature moves, Tier moves).
* **Damage law**: every blow through `ZogratisDamage.strike`: base x power multiplier x per-boss scale x **victim EP factor** (`DamageMath.victimFactor`: x1 to 1,000 EP, +0.5 per decade, max x3), then `BalanceLaw` caps one hit at 40-45% of a player's max health.
* **Time floor**: `boss/budget` (`DamageBudget`, `BudgetPolicy`): a boss can lose at most **6% of max health per second** (min 20 hp/s, x1.5 in the final phase). Docs: `docs/boss_dps_budget.md`.
* **Rules**: `boss/rules` (no regeneration, no Unique/Ultimate skills, with two scripted exceptions: the Gates refill and Lucius's Time Reversal), `docs/boss_rules.md`.
* **Buff engine** (`boss/BossBuffEngine`, `ArcBossProfile` ladder tier 1 to 5): permanent magic/physical barrier and haki-coat effect levels, lent theme skills (mortal fear etc.), anti-ultimate aura radius 8 to 14 blocks, engraving effects (pierce ticks, soul points).
* **Arena**: the spawn spot, leash radius 56, fight resets when nobody is left, block changes ledgered and restored.
* **Boss bar**: server `ServerBossEvent` plus the custom client stack (`boss/bars`, `client/gui/bossbar`, 35 styles, phase notches, ghost chip, shield, wind-up bar, live portrait, lair warning). `docs/boss_bars.md`. **This is ahead of Leon.**
* **Drops**: `bossdrops` (23 json tables, trophies modelled in Blender, coins, first-kill gold, Lucifero armour), Charter Grimoire (world-exclusive), Boss Cores for Decennial bosses. **Ahead of Leon.**
* **Sound**: themed music engine, voice lines (`BossSoundSets`, `audio/voice`). **Ahead of Leon.**
* **Summon**: `ArcSpawner` (command, spawn egg, story arena, floor-boss picker `arc/world`), story gate, one boss per arena (96-block present check). No natural roaming spawn.
* **Stats are in Java tables and the roster** (`ArcCharacter` `.stats(health, attack, speed, armor)`, `*Table`, `*Sheet`); only the Decennial families are data (`tier_families.json`). **No Tensura `entity_existence` file exists for any boss of ours** (`find src/main/resources/data -path "*entity_existence*"` is empty). Lucius alone gets Tensura EP, by attribute modifiers (+50,000,000 max magicule and aura).

### 2.2 How a boss is drawn

| Kind | Bosses | What it really is |
|---|---|---|
| **Player-model kit** (`client/playermodel`, `PlayerBodies.Arc`, `PlayerModelTable.ARC_BOSSES`) | Heath Grice, Vetto, Lotus, Mars, Revchi, Meliodas, Merlin, King, Escanor, Ban, Diane, Gowther, Fana, Rhya, Rades, Patolli, Morris, Zagred, Udaeus, all Tier List bosses (16), Dante, Vanica, Zenon, Lucius (as kit body), Devil Avatar, old Zagred, Dark Disciple | The **vanilla 12-part player body** with a 256x256 hand-painted HD skin (3.40.0), 3D hair (`HairLayer`), a glow map, poses and the shared clip set. This is also how **Tensura itself draws Hinata and Shizu** (64x64 player skins, `PlayerLikeModel`). |
| **Blender mesh body** (`client/bossmesh`, `entity_meshes/*.txt`, `tools/blender/boss_*.py`) | Goliath, Minotaur, Lucifero (v1, form2, v2, v2 form2, v2 gates), God of Order (+gates) | Baked, unwrapped Blender meshes (up to 2,569 body quads + 489 glow, LOD beyond 40 blocks, glow within 48), 8 to 19 rigid parts, **procedural part turns** (`MeshPose`: SLAM, BOW, BEAM, ROAR ...), no keyed clips. Kit renderer is the fallback. |
| **Art-kit geo** (`arc_<id>.geo.json`, 51 to 101 cubes, 512x64 texture, 9-10 clips) | exists for about 120 names, registered by `ArcRenderers` | **Overridden by the player kit** for every id in `ARC_BOSSES` (last registration wins). Still the live renderer for Amphisbaena, Black Goliath and non-person arc entities. |
| Old geo renderers | Zagred legacy (`ZagredModel`), Goliath stone model, Charybdis, Dungeon "dg_*" bodies | fallbacks and dungeon monsters |

**Shared clip set**: every art-kit model has the same 9-10 clips with identical lengths (idle 3.2 s, walk 1.1, run 0.62, attack 0.9, cast 1.2, cast_big 2.4, hurt 0.45, transform 1.75, death 2.8, spawn 2.0), measured on `arc_lucifero`, `arc_morris_libardirt`, `arc_fana`, `arc_heath_grice`, `arc_escanor`, `arc_dante_zogratis` (190 to 715 keyframes per clip, generated by `tools/gen_arc_kit.py`). A boss with 10 to 20 moves plays one of **three poses** (attack, cast, cast_big) for all of them; the Blender bodies map move ids to about five actions (`BossMeshRouting.actionOf`).

### 2.3 Boss list (READ: roster rows, tables, renderers)

Moves = rows in the boss's table (counted by pattern, plus or minus one), "+1" = the wave 3 signature move appended at run time (`Rework3Moves`, 16 bosses). Black Clover x3 = `bossRules.blackCloverStatMultiplier` (server, default 3.0, `docs/black_clover_bosses.md`): maximum health x3 (a permanent modifier) and every hit the boss deals x3, never more than 50% of the victim's max health in one blow. The x3 numbers are base x3; floor scaling and Lucius's arena scaling multiply on top.

| Boss | Work, tier | Body | Moves | Health / attack (base) | After x3 (health / attack) |
|---|---|---|---|---|---|
| Revchi Salik | BC, 1 | kit | 5 +1 | 750 / 10 | 2,250 / 30 |
| Lotus Whomalt | BC, 1 | kit | 6 +1 | 1,050 / 11 | 3,150 / 33 |
| Heath Grice | BC, 2 | kit | 7 +1 | 1,300 / 14 | 3,900 / 42 |
| Mars | BC, 2 | kit | 6 +1 | 1,600 / 15 | 4,800 / 45 |
| Vetto | BC, 2 | kit | 6 +1 | 2,000 / 20 | 6,000 / 60 |
| Fana | BC, 2 | kit | 13 +1 | 1,500 / 12 | 4,500 / 36 |
| Rades Spirito | BC, 2 | kit | 12 +1 | 1,400 / 11 | 4,200 / 33 |
| Rhya | BC, 2 | kit | 11 +1 | 1,450 / 12 | 4,350 / 36 |
| Patolli | BC, 2 | kit | 12 +1 | 1,900 / 14 | 5,700 / 42 |
| Zagred | BC, 2 | kit (old geo fallback) | 14 | 1,900 / 16 | 5,700 / 48 |
| Devil Avatar | BC, 3 | kit | 6 | 1,100 / 15 | 3,300 / 45 |
| Dante Zogratis | BC, 4 | kit | 14 | 1,700 / 18 | 5,100 / 54 |
| Vanica Zogratis | BC, 4 | kit | 15 | 1,300 / 13 | 3,900 / 39 |
| Zenon Zogratis | BC, 4 | kit | 13 | 1,350 / 15 | 4,050 / 45 |
| Morris Libardirt | BC, 4 | kit | 11 | 3,000 / 14 | 9,000 / 42 |
| **Lucifero** | BC, 4 | **Blender mesh** (v2, 19 parts) | 15 | 3,500 / 20 (OP mode: +3,500 health, +6 armour, phases 1.12x sooner) | 10,500 / 60 (21,000 in OP mode) |
| **Lucius Zogratis** | BC, 5 | kit | 20 (combo planner, 25 moves counting spells) | 4,800 / 22 | 14,400 / 66 (then arena scaling x0.70 to x1.30 by 1 to 4 players) |
| Meliodas | SDS, 3 | kit | 12 +1 | 3,200 / 20 | not multiplied |
| Escanor | SDS, 3 | kit | 10 +1 | 3,000 / 24 | not multiplied |
| Ban | SDS, 3 | kit | 10 +1 | 2,400 / 16 | not multiplied |
| King / Diane / Gowther / Merlin | SDS, 3 | kit | 10 / 11 / 10 / 12 (+1 each) | 2,400 / 2,600 / 2,300 / 2,500 (attack 15 to 17) | not multiplied |
| Minotaur | DanMachi, 1 | **Blender mesh** | 11 | 1,000 / 16 | not multiplied |
| Black Goliath / Amphisbaena / Udaeus | DanMachi, 2-3 | art-kit geo / art-kit geo / kit | 10 / 12 / 11 | 1,800/24, 1,900/20, 2,600/26 | not multiplied |
| Goliath | DanMachi, 2 | **Blender mesh** | 12 (7 + 5 appended) | 1,800 / 32 | not multiplied |
| Spawn of Charybdis | Tensura, 2 | geo | 11 | 1,350 / 25 | not multiplied |
| God of Order | own, - | **Blender mesh** | 8 | 1,500 / 16 | not multiplied |
| 16 Tier List bosses (F and D) | various | kit, HD skin | 5 each + 1 or 2 traits | tier rows (xp 20 to 145) | not multiplied |
| 200 Decennial guardians | own | dungeon bodies | profile files (2 to 4 phases) | profile | not multiplied |

Reference points: Leon 4,000 health / 150 attack / armor 16; Tensura Hinata 3,000 / 60 / armor 50. Our **base** attack numbers are one tenth of theirs by design because our damage goes through our own law (move base x power x victim EP factor, hit cap 40-45% of the victim); our **x3 health values (2,250 to 14,400) are in the same range as Leon's 4,000**.

---

## 3. Gaps against the reference

Ranked inside each area; "S" = seen in code/data, "I" = inferred.

### 3.1 Design and abilities

**G1. Our telegraphs are dust particles, Leon's are animations (S).** A wind-up of 8 to 30+ ticks shows a `Tells` dust ring/line while the body plays a shared pose; Leon's 28-38 tick clips make the arm raise, the blade swing and the hit tick all agree. A player cannot learn "this arm pose means that move" because 10 to 20 moves share three poses.

**G2. No per-attack hit timing (S).** `ZogratisBoss` fires melee on a cooldown (`nextMelee = t + max(16, 20 x cooldownMultiplier)`, line 855-857) and the rendered swing starts 0.2 s into a fixed 0.9 s clip. Leon stores (clip length, first hit tick, second hit tick, damage multiplier) per attack and applies damage on that tick.

**G3. No reactive defence outside special bosses (S).** Leon blocks 70%, dodges 30% (8 i-frames, 3 directions), has a guard stance and an anti-kite teleport. Our counters are bespoke per boss (Lucifero's Gravity Shroud, Escanor/Meliodas counter windows, Zagred's wards); sword bosses such as Heath, Vetto, Mars, Udaeus, Ban, King do not block or dodge. The only generic defence is the engine's barrier effects.

**G4. Move choice is a weighted random draw (S).** `MoveScheduler` picks among allowed moves once a second; Leon's `choose()` is a priority list reacting to state (barrier missing, aura sword not on, fear not applied, distance band, line of sight). Lucius alone has a planner (`LuciusCombo`: four-cast combos then a Tower spell).

**G5. Menu length vs readability (S).** 10 to 20 moves with cooldowns 70 to 1,500 ticks. Leon shows about 6 melee + 3 magic + 2 buffs. For the smaller bosses (Revchi 5, Lotus 6, Mars 6, Vetto 6) this is already fine; for 12+ move bosses the readability problem is G1.

### 3.2 Look

**G6. Most of our bosses wear the vanilla player body (S).** 12 cuboid parts plus hair; Leon has 128 cubes in 26 bones (cloak, fingers, hair, fore-legs, held-item bones). Our HD skins (256x256) are better painted than Leon's flat 54-colour 128x128 sheet, but **silhouette** is the player's. Only 4 bosses (Goliath, Minotaur, Lucifero, God of Order) have Blender meshes.

**G7. The Blender bodies are posed procedurally, not keyed (S).** `MeshPose` turns 8 to 19 rigid parts about pivots from a handful of actions. Leon's 21 clips have 80 to 2,200 keyframes each (8,490 total). Mesh bodies therefore cannot show a wind-up arm raise per move, only a pose class.

**G8. Weapon in the hand is a prop, no trail or glow state (I).** Leon's blade gets an inflated additive shell, a sampled swing trail and a warm-up glow driven by a synced clock. Our glow is `ZogratisGlow` (brightens with power, swells over a cast, starves while exposed). No boss-weapon swing-trail ribbon was found in `client/bossmesh` or `client/zogratis` (not searched exhaustively). The Time Magic VFX stay untouched (rule 3).

**G9. Shared clip set (S).** All art-kit characters have identical clip lengths; a boss does not move like himself (Escanor and Fana both play the same 0.9 s attack on the same rig).

### 3.3 Scaling

**G10. Black Clover x3 does not lengthen the fight for strong builds (S derived).** The per-second damage budget is a share of **max** health (6%), and x3 raises max health, so the cap triples too. The kill-time floor stays about 17 s (11 s in the final phase) whatever the health; the x3 only slows builds that deal less than the cap. `docs/black_clover_bosses.md` says the budget "grows with it" (read), the consequence for the floor is derived here. The x3 damage is limited by the 50% hit cap.

**G11. No party-size scaling except Lucius (S).** `boss/god/ArenaScaling` (1 player 70% health and 80% damage, 2 = 100%, 3 = 115%/106%, 4+ = 130%/112%) is Lucius only. A solo player and a squad fight the same 29 other bosses with the same health bar. Leon has no party scaling either, so the reference does not solve this; it is a gap against our own best practice.

**G12. No EP identity (S).** Leon is a 1.0 to 1.2M-magicule, 600-850k-aura entity with 18 real skills, so Tensura's resistances, EP comparisons and skill rules treat him as a peer of Hinata. Our bosses have no `entity_existence` row (Lucius excepted, by modifiers). Whether that matters depends on Tensura's EP engine (not read here).

**G13. Floor scaling is the only depth scaling, gentle (S).** `FloorScale`: health x(1 + perFloor x floors below design) capped x2.5, damage capped x1.6. Combined with the x3 this is fine, but nothing scales with the player's EP (the victim factor scales our damage up to x3 with the victim's EP; the boss's health never follows the fighter's power).

**G14. Boss numbers are Java constants (S).** About 30 `*Table`, `*Sheet` and roster rows; changing a cooldown or damage means a rebuild. Leon exposes 12 server-tunable keys and puts EP and skills in data. Only the Decennial families are data-driven here.

### 3.4 Where we are ahead (no change recommended)

Boss bar (35 styles, phase notches, wind-up bar, portrait), drops (trophies, coins, Charter, cores), music and voice, summon/arena/story system, damage budget, no-regen rules, multiplayer safety of the arena, tests and self-tests. Leon's bar is one custom frame; his drops are none; his sound is vanilla.

---

## 4. Ranked fix list (what to change, by file, effort, risk)

Effort: S = about a day, M = 2 to 4 days, L = a week or more, per boss or system, not counting preview and test sweep. Every item must follow CLAUDE.md: new next to old (rule 2), behind a setting that restores today's behaviour (rule 12), bounded cost (rule 11), server decides (rule 9), Blender for models (rule 14). Nothing below was built.

| # | Fix | Files | Effort | Risk | Why it ranks here |
|---|---|---|---|---|---|
| **1** | **Make the x3 count: tie the DPS floor to base health.** Option A: `BudgetPolicy` takes the cap from `base max health` (max health divided by the Black Clover modifier) so x3 lengthens the floor to about 50 s; option B: keep the cap but add the same x3 to the final-phase scale. New server key next to `dpsCapFraction`. | `boss/budget/BudgetPolicy.java`, `BossBudget.java`, `boss/BossStatBuff.java`, `BossBudgetConfig.java`, tests `BudgetPolicyTest`, `BossStatMathTest` | S | M (changes how long every BC fight lasts; old setting restores) | Cheapest change with the biggest felt effect; fixes G10. |
| **2** | **Per-attack timeline for melee bosses** (Leon's table): a pure `AttackTimeline` (clip ticks, hit tick, second hit, multiplier) per melee move, damage applied on the hit tick, wind-up = the clip's lead-in. Start with the sword bosses: Meliodas, Ban, Escanor, Heath, Vetto, Mars, Udaeus, Minotaur, Black Goliath. | new `entity/zogratis/AttackTimeline.java` (pure) + hook at `ZogratisBoss.java` melee (about line 855), `client/zogratis/ZogratisPose.java` (`ATTACK_LEAD`), per-boss tables; tests pinning ticks | M | M (animation and server timing must agree; old fixed lead kept as fallback) | Fixes G1 and G2 together; needs no new model. |
| **3** | **New clips for the kit and the meshes**: 5 attack variants (horizontal, rising, heavy cleave, advance, cross), block x2, dodge x3, guard idle, so each melee move maps to its own pose; extend `clipOf(moveId)` instead of three shared poses. | `tools/gen_arc_kit.py` + `tools/arc_kit/*` (clip generator), `tools/arc_hd` (kit poses), `client/playermodel/PosePatterns.java`, `ArmPoses.java`, `client/bossmesh/MeshPose.java` + `BossMeshRouting.actionOf`, `client/arc/ArcClips.java` | M to L | L-M (generated assets; previews must be sent per rule 12) | Needed by #2 and #4; fixes G7 and G9 in part. |
| **4** | **Shared reactive guard** (`ReactiveGuard`): opt-in per boss, Leon's numbers as defaults (block 70%, dodge 30%, 8 i-frames, 12-tick block window, guard within 5 blocks), cooldown-limited, **never bypasses the DPS budget, the power-step clamp or the no-regen rules**; counter swing after a block. First users: Meliodas, Ban, Escanor (already has counter), Heath, Vetto, Mars, Udaeus. | new `entity/zogratis/ReactiveGuard.java`, hook ahead of `PowerCurve.clampDamage` in `ZogratisBoss.hurt`, flags in `ArcCharacter`/`TierRow`, server keys in `BossRulesConfig` | M | M (must not make bosses unkillable; DPS budget stays the arbiter; add a per-second cap on dodges) | Fixes G3. |
| **5** | **Party-size scaling for every boss** by generalising `ArenaScaling` (read once at fight start, as Lucius does): health x0.70 / 1.00 / 1.15 / 1.30 for 1 / 2 / 3 / 4+ players, damage x0.80 / 1.00 / 1.06 / 1.12. | `boss/god/ArenaScaling.java` (reuse), `ZogratisBoss` fight-start hook (`onFightStart`, `fighters()`), `BossRulesConfig` key, test | S to M | M (stacks with x3 and floor scale; needs one combined cap) | Fixes G11 with an already tested table. |
| **6** | **Tensura EP identity (pilot on 2 bosses)**: add `data/anime_multiverse/entity_existence/<boss>.json` in Leon's format (abilities, min/max magicule and aura, `spiritualHealth`) for Lucifero and Lucius first; compare with Hinata (3000 hp, 536k EP) and Leon (1.0 to 1.2M). | new data files; no Java unless tests need a registry check; read `Tensura` loader first | S | **H-unknown**: interacts with `BalanceLaw.epScale`, `Nullification`, Tensura skill rules (Predator, Usurper), `UsurperPolicy`; the loader was not read. Pilot only. | Fixes G12 if the owner wants Tensura to see bosses as equals. |
| **7** | **Telegraph upgrade**: replace dust-only wind-ups with the existing `VfxShape` decals plus the animation from #3, for the 5 busiest bosses first (Lucius, Lucifero, Zagred, Meliodas, Escanor). Do not touch the Time Magic layer. | `entity/zogratis/Tells.java`, `vfx/*`, per-boss `*Moves`, `perf` budgets, `PerfSourceScanTest` | M | M (rule 11: packets per second and quads per frame must be stated) | Fixes the rest of G1. |
| **8** | **Bespoke bodies for the next marquee bosses**, by the Blender pipeline (`tools/blender/bl_entity.py`, `boss_lucifero_v2.py` as the model): **Lucius** (the biggest gap: 20 moves on a player body), **Dante, Vanica, Zenon**, **Zagred**, **Meliodas, Escanor**, **Morris**. Each with cloak/hair/held-weapon parts, an additive state shell and a swing trail like Leon's. Register through `BossMeshRouting` with the kit as the fallback. | `tools/blender/boss_<id>.py`, `entity_meshes/<id>(.|_lod).txt`, `textures/entity/boss_mesh/`, `client/bossmesh/BossMeshRouting.java`, `PlayerModelOptions` (one client key per body) | L per boss | L (fallback exists) but the heaviest item; quad budgets must be stated per boss | Fixes G6 and G8. |
| **9** | **Data-driven boss numbers**: `data/anime_multiverse/arc_bosses/<id>.json` (health, attack, speed, armor, move cooldown scale), Java defaults stay as the fallback. | `arc/ArcCharacter.java` (optional override), a loader like `BossDropData`, tests | M | M (touches every roster row; append-only) | Fixes G14; lets the owner retune without a rebuild. |
| **10** | **Natural spawn for roaming bosses (optional, Leon style)**: biome modifier weight 5, count 1, plus a world-level cap, for a few non-story bosses only. | `data/anime_multiverse/neoforge/biome_modifier/*`, `ArcSpawner` cap | S | M-H (public-server lag and griefing, rule 11; stays off by default) | Not needed for the stated problems. |

### Per boss, what changes first

| Boss | First change | Then |
|---|---|---|
| Lucius (14,400 hp, 20 moves) | #8 bespoke body, #7 telegraphs | #5 (already scaled), #6 EP pilot |
| Lucifero (mesh) | #3/#7 keyed clips for BOW, BEAM, Kneel | #6 EP pilot, #1 |
| Dante / Vanica / Zenon | #8 bodies after Lucius, #3 poses | #4 guard for Dante |
| Zagred | #7 telegraph (already has wards and verdict lines), #8 | |
| Meliodas, Escanor, Ban | #2 timeline, #3 clips, #4 guard | #8 bodies |
| Heath, Vetto, Mars, Revchi, Lotus | #2 timeline, #4 guard for the three sword users | none (small kits) |
| Fana, Rades, Rhya, Patolli | #3 clip per cast type | |
| DanMachi monsters | #2 for Minotaur, Black Goliath, Udaeus; #5 | |
| Tier List (16) and Decennial (200) | #5 only; they are data rows, keep as they are | |

---

## 5. Questions for the owner (answer yes or no)

1. **Should Black Clover x3 also lengthen the minimum kill time** (the damage budget counts the base health, so a max-EP build needs about 50 s instead of about 17 s)? Fix #1.
2. **Should every boss, not only Lucius, scale with party size** (70% / 100% / 115% / 130% health by 1 / 2 / 3 / 4+ fighters)? Fix #5.
3. **Should sword bosses block and dodge like Leon** (70% block, 30% dodge with 8 i-frames, always inside the damage budget)? Fix #4.
4. **Should melee damage land on a fixed tick of a per-attack clip** (Leon's timeline) for the nine sword bosses? Fix #2.
5. **Should I add the 5 attack, 2 block, 3 dodge and guard clips to the art kit** and give each boss its own attack clip per move? Fix #3.
6. **Do you want bespoke Blender bodies for the next eight bosses in this order: Lucius, Dante, Vanica, Zenon, Zagred, Meliodas, Escanor, Morris?** Fix #8.
7. **Should two bosses (Lucifero and Lucius) get Tensura EP rows (`entity_existence`) as a pilot**, with a rollback if Tensura's skills or the Balance Law behave differently? Fix #6.
8. **Should boss numbers move into data-pack json files** (Java values stay as defaults)? Fix #9.
9. **Should a few non-story bosses spawn naturally in the world** (off by default, capped)? Fix #10.
10. **Is "Leon-class" about 4,000 health the target for the strongest non-BC bosses** (Meliodas 3,200, Escanor 3,000 are close; DanMachi and the Tier List are far below)? If yes, which works should be lifted?

---

## 6. Limits of this study

* Leon was not decompiled and not run; his AI is described from constants, field names and call order. His real hit chances in play, the Tensura skill cast bars, and the look of the glow layers were not seen.
* The repo was read, not compiled or played. Move counts are pattern counts (plus or minus one). Dante/Vanica/Lucius/Zagred stats were checked in their boss classes; Revchi's and the Avatar's move counts come from `ChainTable` / `AvatarTable` only.
* Tensura's EP engine and `entity_existence` loader were not read, so fix #6 is a pilot, not a promise.
* No `docs/` file other than this one was written; nothing was committed.
