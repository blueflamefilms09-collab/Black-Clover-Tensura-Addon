# Boss rework, wave 1: abilities and VFX

The owner asked to "rework the bosses: models, abilities, and VFX's". This wave is **abilities and VFX only** (plus one look switch for the Goliath that was
already built). Models are not in this wave (see "Left for later waves").

Settings (each restores the old behaviour, rule 12):

| Setting | File / side | Default | Off means |
|---|---|---|---|
| `bossRules.bossRework1Moves` | `nusmp-boss-rules-server.toml`, SERVER | `true` | the old move lists exactly (the appended rows are cut off at build time; no old index moves) |
| `bossRules.bossRework1Vfx` | `nusmp-boss-rules-server.toml`, SERVER | `true` | Lucifero's old dust bursts, and no signature effect on the Black Goliath / Minotaur / Amphisbaena / tier signature moves |
| `playermodel.goliathStoneModel` | `nusmp-playermodel-client.toml`, CLIENT | `true` | the Goliath is drawn as in 1.99.0 (the kit's stretched body when `dungeon` is on, else the art kit's) |

`bossRework1Vfx` only changes looks, but it is a server setting because the server decides which effect packet it sends (the same reason `nenVfxV2` is a server setting).

Self-test: `boss.rework1` (in the smoke list, before `boss.stats`, which stays just before the final `tier`). It builds a Goliath, a Charybdis Spawn, a God of Order and
the seven tier bosses (never added to the world) and checks every old move is at its old index and every appended move is there (with a telegraph and code) when the
setting is on, absent when off.

Tests: `BossRework1Test` (append-only tables pinned against the old id lists, setting-off lists, telegraph cases, lang names, Lucifero dust scan, DanMachi signatures,
settings, smoke order, no client classes in common code, the pure rules, this doc); the existing table tests now say "the ultimate closes the **old** list".

## Per boss: NEW / REPLACED / KEPT

### Goliath (DanMachi, floor boss) — `dungeon/floor/goliath`
* NEW: 5 appended moves (`GoliathRework`), the Stone Skin shield (`GoliathBoss.filterDamage`, half of each hit passes for 6 s; a shield, never a heal: the no-regen rule holds).
* REPLACED: the default look. The stone giant (`GoliathRenderer`, `goliath_body` geo: moss, cracks, its own clips) is the default again (remake_plan.md quick win 1);
  the 1.99.0 routing (kit body / art kit) is kept behind `goliathStoneModel = false`.
* KEPT: the 7 old moves, numbers and indices, the rise, the Gates, the kit body and the art-kit renderers (still registered).
* Clips: the new moves reuse the old clips (`GoliathPose.clipOf`: roar/skin = roar, hurl = rubble, pound = slam, chain = stomp).

| # | id | Name | Power | Wind-up | Cooldown | Range | Weight | Damage (base) | Shape |
|---|---|---|---|---|---|---|---|---|---|
| 7 | `magebane_roar` | Magebane Roar | 40 | 30 | 520 | 0-18 | 2 | 6 + up to 2 timed blessings torn off | ring round the body (crowd) |
| 8 | `boulder_hurl` | Boulder Hurl | 40 | 30 | 220 | 10-40 | 3 | 18 | marked circle r 3, lands 14 ticks after the throw |
| 9 | `stone_skin` | Stone Skin | 40 | 24 | 700 | 0-48 | 2 | - (hits on it x0.5 for 120 ticks) | self |
| 10 | `pound_shockwave` | Ground-Pound Shockwave | 60 | 32 | 260 | 0-16 | 3 | 10 | fast jumpable ring, 16 blocks |
| 11 | `stomp_chain` | Giant Stomp Chain | 60 | 28 | 240 | 4-20 | 3 | 7 x4, budget 45% of max health | 4 circles r 3.2 walking to the foe, 8 ticks apart |

(The Gates add a quarter to every reach, as for the old moves.)

### Charybdis Spawn (Tensura, floor boss) — `dungeon/floor/charybdis`
* NEW: 4 appended moves (`CharybdisRework`).
* KEPT: the 7 old moves and their indices, the minions, the scales and parries, the old whirlpool code (the maw reuses it).

| # | id | Name | Power | Wind-up | Cooldown | Range | Weight | Damage | Shape |
|---|---|---|---|---|---|---|---|---|---|
| 7 | `scale_shard_storm` | Scale-Shard Storm | 40 | 34 | 300 | 0-36 | 3 | 9 magic + slow 2 s | a circle r 2.6 under up to 4 foes, lands 10 ticks after |
| 8 | `tempest_current` | Tempest Current | 40 | 28 | 320 | 6-30 | 3 | - (pull) | 50 ticks: foes within 20 dragged in (0 at core 4, 0.16/step at the rim; never beats walking) |
| 9 | `whirlpool_maw` | Whirlpool Maw | 60 | 40 | 420 | 0-14 | 3 | 20 at the end | the old whirlpool, r 8, 60 ticks, then the core r 3.2 snaps shut |
| 10 | `magic_interference` | Magic Interference | 40 | 30 | 600 | 0-24 | 2 | 4 magic + weakness 3 s + 2 blessings torn | ring r 16 |

### God of Order — `relics/order`
* NEW: 4 appended moves (`OrderRework`): 8 moves in all. It stays out of every buff (still left out of `BossRegistry`; nothing here touches that).
* KEPT: Decree, Writ, Judgement, Erasure at indices 0-3; the forced Erasure; the visit.
* NOT DONE (noted): its skin is still the **64x64** sheet; it is not repainted in this wave.

| # | id | Name | Power | Wind-up | Cooldown | Range | Weight | Damage | Shape |
|---|---|---|---|---|---|---|---|---|---|
| 4 | `seal_of_silence` | Seal of Silence | 40 | 30 | 360 | 0-24 | 3 | 6 magic + 2 blessings torn + slow II 3 s | locked ring r 4.5 |
| 5 | `scales_of_order` | Scales of Order | 40 | 34 | 300 | 4-30 | 3 | 14 magic | two circles r 3: under the foe and its mirror across the God |
| 6 | `chains_of_law` | Chains of Law | 60 | 30 | 320 | 3-22 | 3 | 8 magic + slowness III 2 s (soft, no hard hold) | corridor 18 x 2.6 |
| 7 | `verdict_rain` | Verdict Rain | 60 | 36 | 380 | 0-36 | 2 | 12 magic, once per foe | a circle r 2.8 under up to 4 foes, beam from the sky |

### Lucifero (Black Clover, Spade Kingdom gravity devil) — `arc/spade`
* REPLACED (behind `bossRework1Vfx`): every vanilla dust burst of his files now goes through `LuciferoFx`, which sends existing gravity shapes through `ZogratisVfx`
  (its per-level budget): Falling Heavens = a falling-heavens pillar (`DIVINE_PILLAR` in his violet) per mark; Kneel Before the King = the gravity crush ring
  (`GRAVITY_WELL`, r 26); Event Horizon collapse = the event-horizon sphere (`GRAVITY_SINGULARITY`); shroud torn / shell shattered / devils swallowed = `GRAVITY_IMPACT`;
  the Six Limbs telegraph = one `GRAVITY_LANES` per limb sent once for the wind-up (was `SpadeFx.spoke` dust every third tick). `LuciferoBoss` / `LuciferoMoves` no
  longer name `DustParticleOptions`, `SpadeFx.dust`, `SpadeFx.spoke` or `Tells.burst` (pinned by the test).
* KEPT: his 13 moves and numbers; the dust telegraph rings and lines (`Tells.ring/line/column/gather`, the framework's telegraph); `SpadeFx` itself (Morris still uses it).
* No new VfxShape: existing gravity shapes were enough (no enum entry appended).

### Black Goliath, Minotaur, Amphisbaena (DanMachi) — `arc/danmachi`
* NEW: a signature effect per cast (`DanMachiBoss.signature`, one effect, behind `bossRework1Vfx`): Black Goliath = brimstone cracks (`BOSS_DEVIL_CRACKS`, its hot ember-red,
  reach of the move 3-12); Minotaur = the reaping arc of its natural weapon (`SLASH_SCYTHE` toward the foe, 3-7); Amphisbaena = a serpent of water flying to the foe
  (`WATER_DRAGON`).
* KEPT: every move, the DmFx telegraphs and strikes (they still draw the floor shapes), Udaeus unchanged.

### Tier List: the seven weakest kits — `arc/tier`
* NEW: one signature move each, appended as the 6th (last) row (TierRules allows 4-6), from the existing `TierMove.Kind`s so the telegraph is the kind's own `Tells`, and one
  shaped effect as it lands (`TierMoves.signatureLand`, behind `bossRework1Vfx`). All: power 40, wind-up 30, cooldown 360, weight 2 (so the tier pressure band still holds:
  `TierBalanceTest` green).
* KEPT: the shared body (no models this wave), the other ten tier bosses, all traits.

| Boss | id | Name | Kind | Range | Numbers | Effect | Basis |
|---|---|---|---|---|---|---|---|
| Bertholdt Hoover | `wall_breach_kick` | Wall-Breaching Kick | CLEAVE | 0-9 | 1.2x, reach 8, knock 1.2 | `BOSS_STONE_FISSURE` along the kick | THEME |
| Kurozumi Orochi | `yamata_venom_fangs` | Yamata no Orochi: Venom Fangs | VOLLEY | 6-28 | 5 x 0.5x, 70 deg, poison 3 s | `POISON_BREATH` | THEME |
| Shou Tucker | `life_sewing` | Life-Sewing Transmutation | MARKED | 4-24 | 3 x 0.8x magic, r 2.6, weakness 4 s | `ALCHEMY_CIRCLE` under the foe | THEME |
| Big Mom | `ikoku_sovereignty` | Ikoku Sovereignty | BEAM | 4-20 | 1.2x, 16 long, knock 1.0 | `SLASH_WAVE` crescent | NAMED (from memory) |
| Yammy Llargo | `ira_giant_cero` | Ira: Giant Cero | BEAM | 4-20 | 1.2x magic, 16 long | `DIVINE_RAY` in red (via `ZogratisVfx.ray`) | THEME |
| Code | `claw_grime` | Claw Grime | MARKED | 4-24 | 4 x 0.7x magic, r 2.4, slow 3 s | `DARK_SLASH_WILD` at the foe | NAMED (from memory) |
| Mr. 2 Bon Clay | `swan_arabesque` | Okama Kenpo: Swan Arabesque | RUSH | 3-21 | 1.2x, 16 ticks, knock 1.0 | `WIND_SLASH` | NAMED (from memory) |

## Caps and rules (unchanged, respected)
Every hit goes through the boss's own damage law (`strike`: the 45% player cap per hit, the 6%-of-max-health per-second DPS budget); multi-hit moves share a damage budget
(stomp chain 45%); no move heals (the Stone Skin is a damage cut); no Unique/Ultimate skill; no block is changed; no entity is spawned; every new move has a wind-up of 24+
ticks drawn by the existing `Tells` plus one telegraph shape, and an action-bar warning (Goliath, Charybdis).

## Worst-case cost
* Server per cast: at most 24 foes looked at per circle / ring / step (bounded AABB queries, as the old moves); at most 4 circles; delayed tasks: 4 (stomp chain), 1
  (boulder, shards, maw). Zones: Tempest Current 50 ticks stepping every 2 ticks (25 steps x <= 24 foes); Whirlpool Maw = the old whirlpool zone (60 ticks / 2);
  Ground-Pound = one ring wave (~9 ticks). No per-tick world scan, nothing on a player tick; every zone checks the boss is alive and ends by itself.
* Packets: all boss effects go through `BossMoveVfx` (12 a tick server-wide) or `ZogratisVfx` (its per-level cap), then `VfxSpawn` (perf budget, players within 96 blocks
  only). Per cast: roar 2, hurl 1 tell + 2, skin 1 + 1, pound 1 + 2, stomp chain 4 + 4 over 24 ticks, shard storm 4 + 4, current 1 + 1, maw 1 + 2, interference 1 + 2,
  God of Order moves 1-4 + 1-4, tier and DanMachi signatures 1. Lucifero sends fewer packets than before (one shape per event instead of a 22-90 particle burst packet;
  the limb lanes are sent once instead of dust every third tick).
* Client: each shape is drawn by its existing layer within its existing budget (boss shapes <= 400 vertices each, client cap 64 effects). The Goliath's stone model is the
  1512-vertex `goliath_body` geo instead of the kit body.

## Left for later waves
* **Models (rule 14, Blender)**: none made in this wave. The entity renderer path for Blender meshes does not exist yet: the weapon pipeline (`tools/blender/bl_lib.py`,
  `docs/heroweapon/blender_pipeline.md`, `WeaponRenderer`) draws item meshes only. An entity path would need: an export of a baked, unwrapped, rigged mesh to a format the mod
  reads (the geo JSON the `GeoModels` cache already loads, or a new baked-mesh format), bone or baked-clip animation hooked into `ZogratisPose` clips, a renderer base on
  `ZogratisRenderer` (glow pass, cracked / Gates skin swap, the rise sink), LOD meshes and distance culling under a per-entity vertex budget, an asset-presence and size
  test, and a preview tool like `tools/item_preview`. Then: the 17 tier bosses (one shared body today), God of Order (64x64 skin), Goliath and Black Goliath remodels.
* The 200 Decennial bosses (audit's second worst) are untouched.
* The other ten tier bosses, Udaeus, the Devil Avatar and the Zogratis four: no change.
* Lucifero has no new moves (only its effects were reworked).

## Caveats
* Not compiled against Minecraft here: the pure tests were run locally (javac + JUnit: the Goliath, Charybdis, God of Order, tier, arc and `BossRework1Test` suites),
  the game code is checked by CI.
* Nothing was seen in a game: the shapes' look with these colours and sizes (especially `WATER_DRAGON`, `SLASH_SCYTHE`, `DIVINE_PILLAR` in violet) and the balance of
  the new moves are unplayed.
* Wiki research (rule 10): the English fandom wikis (danmachi, tensura, onepiece) could not be reached from this session (host not resolvable); the Japanese wikis were
  not tried for the same reason. Names and lore are from memory and marked by their Basis (NAMED / THEME); Code's Claw Grime, Big Mom's Ikoku Sovereignty and Bon Clay's
  Swan Arabesque are canon names as remembered, the rest are themed names.
