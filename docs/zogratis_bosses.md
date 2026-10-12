# The Zogratis bosses: design document and file map (1.20.0)

Status: **built in 1.20.0, written from the code on disk** (see section 14 for exactly what was and was not verified). The document covers
the four bosses of the Spade Kingdom: **Dante**, **Vanica** and **Zenon Zogratis** (the Dark Triad, contracted to Lucifero, Megicula and
Beelzebub) and **Lucius Zogratis** (the main antagonist, contracted to Astaroth, the Supreme Time Devil). Names in `code font` are real
classes, methods and constants. Numbers are starting values to tune in play; every number in the tables below was read from the code (or
computed by running the pure classes), not copied from the request.

Sections: 0 platform note, 1 the four at a glance, 2 request-to-implementation map, 3 file map, 4 spawning and server settings, 5 the shared
fight framework (Percentage Power System, Gates of Qliphoth, mana, Dark Disciples), 6 Dante, 7 Vanica, 8 Zenon (with Spatial Mana
Domination), 9 Lucius, 10 world safety, 11 performance, 12 tuning guide, 13 wiring checklist, 14 unverified and not done.

All Java paths below are relative to `src/main/java/com/newuniverse/nusmp/`, assets to `src/main/resources/assets/nusmp/`, tests to
`src/test/java/com/newuniverse/nusmp/`.

## 0. Platform note: the request was written for Bedrock, this repository is a NeoForge Java mod

The request asked for "everything" in the shape of a Bedrock / Forge JSON addon (behaviour pack, resource pack, entity JSON, animation
controllers, spawn rules, loot tables, a manifest, "where to place the files"). This repository is a **NeoForge 21.1.x / Minecraft 1.21.1 Java
mod** (`modid nusmp`, Java 21), so there is no addon folder to drop files into: the "addon folder structure" **is this repository's own source
tree**, and the deliverable is Java classes plus assets inside it. The equivalent of each Bedrock part:

| Bedrock / JSON addon part | Where it is in this mod |
|---|---|
| Behaviour pack: entity definitions, components, events, AI goals | Java classes in `entity/zogratis/` (`ZogratisBoss` is the shared fight framework; one class per boss). Stats are `createAttributes()` in each boss class. |
| Behaviour pack: scripts, animation controllers, state machines | The move scheduler and the state constants of `ZogratisBoss` (`STATE_COMBAT`, `STATE_CASTING`, `STATE_TRANSFORMING`, `STATE_RECOVERING`), plus the pure classes listed in section 3. |
| Resource pack: geometry, animations, textures, render controllers | GeoLite assets `assets/nusmp/{geo,animations,textures}/entity/zogratis_*` built by `tools/gen_zogratis_triad.py` and `tools/gen_zogratis_lucius.py`, drawn by `client/zogratis/`. |
| Spawn eggs, `spawn_rules` | Spawn eggs registered through `NUItems.ITEMS` (`ZogratisEntities`) and the command `/multiverse boss ...`. **No natural spawning** (there is no spawn rule). |
| Loot tables | **None.** The bosses drop experience only (`xpReward = 1000` on the bosses). See section 14. |
| `lang/*.lang` | 14 keys in `assets/nusmp/lang/en_us.json` (section 3.6). |
| `manifest.json` | `META-INF/neoforge.mods.toml` and `gradle.properties` (`mod_version=1.20.0`), owned by the lead. |

The two "reference image" sets supplied with the request (Lucius base and divine; the Triad close-ups and the Triad full body) were used only
as a visual guide: every model and texture is an **original pixel-art interpretation** drawn by the generator scripts, nothing is traced or
pasted from the images.

## 1. The four at a glance

| | Dante | Vanica | Zenon | Lucius |
|---|---|---|---|---|
| Role | Brawler-tank of the Triad | Striker-caster | Assassin-caster | Endgame boss, main antagonist |
| Devil (`Devil` enum, reused) | `LUCIFERO` (gravity) | `MEGICULA` (curse-warding) | `BEELZEBUB` (spatial) | `ASTAROTH` (time) |
| Native magic | Body Magic | Blood Magic | Bone Magic | Soul Magic (innate) |
| Devil magic | Gravity Magic | Curse-Warding Magic | Spatial Magic | Time Magic, plus Lucifero's gravity and the siblings' body, blood and bone |
| Health | 1200 | 900 | 950 | 3600 |
| Armor / toughness | 14 / 8 | 8 / 4 | 8 / 4 | 22 / 12 |
| Attack damage (plain blow base) | 18 (x0.75 in the plain blow) | 13 (x0.7) | 15 (x0.7) | 22 (x0.7, zero during Julius) |
| Movement speed attribute | 0.30 | 0.36 | 0.34 | 0.36 |
| Damage scale on top of the power tuning | 1.1 | 1.15 | 1.15 (x1.15 more while his own cube stands) | 1.25 |
| Preferred distance (the middle of its band is clamped to this; it strafes within 3 blocks of it) | 2.5 to 4.5 blocks | 4.5 to 9 | 6 to 11 | 7 to 14 (5 to 11 divine) |
| Form at 100% | `FORM_HYBRID` (model scale 1.18) | `FORM_HYBRID` (1.10) | `FORM_HYBRID` (1.10) | `FORM_DIVINE` (1.20; the human form is 1.05) |
| Ultimate(s) | Gravity Singularity (60%) | Crimson Crown (60%) | Spatial Mana Domination (60%) | Soul Purge (40%), Sibling Assimilation (60%), Time Stop (80%), Grimoire Tower Storm (80%) |
| Followers | Dark Disciples (Lucifero) | Dark Disciples (Megicula) and Blood Beasts | Dark Disciples (Beelzebub) | Soul Paladins and clones (no Dark Disciples) |
| Tensura skills it learns (`tensuraKeywords`) | gravity, burden, earth (3) | curse, darkness, shadow (3) | space, dimension (2) | soul, spirit, holy, time, gravity (4) |
| Passive | Body Magic pulse and trickle | Blood drain | Spatial Evasion | Precognition, Two Souls (Julius), mending flesh |

All four share: `bossAttributes(...)` extras (follow range 48, **knockback resistance 1.0**, step height 1.0, attack knockback 1.0), fire immunity
(`fireImmune()` on the entity types), no despawn, immunity to hard control (section 4), a boss bar in the devil's colour, and the same fight
framework.

## 2. Request-to-implementation map

| Request item | Implemented as | Where |
|---|---|---|
| **Dante**: Possessing Devil Lucifero | `Devil.LUCIFERO`; devil colour (violet) on the bar, telegraph dust, Disciples, hybrid form | `DanteBoss`, `DevilStyle` |
| Dante: Native Magic (Body Magic): strength + instant regeneration from mortal wounds | Titan Fist, Stomp, Leap, Body Surge; **mortal-wound pulse** that heals 8% to 26% of max health when a hit would leave him at 30% or less, with fair counters | `BodyRegen`, `DanteBoss.filterDamage`, `DanteMoves.titanFist` |
| Dante: Devil Magic (Gravity): AoE crushing, slowness, downward velocity | Lucifero's Domain (field), Gravity Pull + slam, Lucifero's Weight, Weighted Leap | `DanteMoves`, `GravityMath` |
| Dante: hurls compressed rock swords | Compressed Rock Swords (fan of 3 to 9) and Stone Spikes | `DanteMoves.rockSwordVolley`, `stoneSpikes` |
| Dante: ultimate Gravity Singularity (black-hole spheres warping space, destroying mountain terrain) | Black spheres on marked zones, a drag, a collapse, and (griefBlocks on) a bowl bitten out of the ground, all restored afterwards | `DanteMoves.singularity`, `bite` |
| **Vanica**: Possessing Devil Megicula | `Devil.MEGICULA`; crimson colour | `VanicaBoss` |
| Vanica: Native Magic (Blood Magic): weapons / beast-like constructs | Blood Lances, Scythe, Whip, Dash, Burst, Crimson Rain, Blood Frenzy, blood drain | `VanicaMoves`, `VanicaMath` |
| Vanica: Devil Magic (Curse-Warding): lethal decaying curses, weakening, rotting blocks | **Curse of Decay** (a brand: Weakness, healing cut, growing bite) and **Rotting Field** (Weakness/Wither, rots grass, soil, leaves, logs while the grief rule allows) | `VanicaCurse`, `CurseBrand`, `VanicaMoves.curseRot` |
| Vanica: Summons: Blood Beasts as hostile minions | `BloodBeastEntity` (own entity, model, renderer) | `BloodBeastEntity`, `VanicaMoves.spawnBeasts` |
| **Zenon**: Possessing Devil Beelzebub | `Devil.BEELZEBUB`; indigo colour | `ZenonBoss` |
| Zenon: Native Magic (Bone Magic): grows and launches bone spears / swords | Bone Spear Barrage, Bone Blade Dance, Dense Bone Lance, Bone Garden, Bone Armour | `ZenonMoves`, `BoneMath`, `BoneShell` |
| Zenon: Devil Magic (Spatial): teleportation portals | Every blink goes through a visible entry and exit portal: Rift Backstab, Portal Link, Warp Barrage, Spatial Evasion | `ZenonMoves`, `PortalMath` |
| Zenon: Domain Expansion "Spatial Mana Domination" (cubic domain that severs mana and negates spells) | `SpellSeal` (the cube, 60% power and up) | `SpellSeal`, `DomainCube`, `DomainClock`, `DomainMath`, `DomainIntegrity` |
| **Shared**: rulers of the Spade Kingdom | Names, lines, models (crowns, fur collars), `ZogratisBoss` framework, `TriadBond` | `ZogratisBoss`, `TriadBond` |
| Shared: contracts with the three highest devils; native + devil magic | Each boss = one devil + one native magic (above) | boss classes |
| Shared: **Percentage Power System**, up to 80%, later 100% tied to the Gates of the Tree of Qliphoth | Section 5.1 | `PowerCurve`, `ZogratisBoss` |
| Shared: **Mana Scaling** (near-infinite reserves) | Finite mana pool below 60% power, **unbound from 60%** | `ManaPool`, `PowerCurve.tuning` |
| Shared: **100% Transformation** to partial devil hybrids | 60-tick invulnerable cutscene, model swap to the hybrid models, bigger hitbox, all numbers at their peak | `ZogratisBoss.beginGates`, `gatesForm()` |
| Shared: **Dark Disciples** (up to 40% of the devil's power) | `DarkDiscipleEntity`, share = 0.4 x the owner's power fraction | `DarkDiscipleEntity`, `DiscipleStats` |
| Shared: visual design (close-up cues, full-body appearance) | Section 3.3 and the generator headers | `tools/gen_zogratis_triad.py` |
| **Lucius**: Boss hierarchy, god-tier stats | 3600 health, armor 22, four ultimates, 20 moves | `LuciusBoss`, `LuciusTable` |
| Lucius: Soul Magic (manipulate souls, purify devil power, resurrect, turn mobs into loyal immortal Paladins) | Soul Motes, Soul Drain, Soul Purge, Resurrection, Memory Manipulation, **Soul Paladins** | `LuciusMoves`, `SoulPaladins`, `SoulMath`, `PaladinLedger`, `ZogratisEffects` |
| Lucius: Two Souls (Julius Novachrono as sleeper / memory mechanic) | `JuliusWill`: Julius surfaces at 75, 55, 35 and 12% health | `JuliusWill`, `LuciusBoss` |
| Lucius: Time Magic (Astaroth): precognition, time stop, reversal, acceleration, beyond light speed | `Precognition`, Time Stop, Time Reversal, Chrono Acceleration, Chrono Stasis, Beyond Light steps | `Precognition`, `ChronoMoves`, `ChronoMath`, `ChronoRing` |
| Lucius: absorbed Gravity (Lucifero) | Lucifero's Weight, Sibling Assimilation | `LuciusMoves.gravityCrush` (calls `DanteMoves.gravityField`) |
| Lucius: Sibling Assimilation (Body, Blood, Bone) | Bone Arsenal (Zenon's functions), Blood Tide (Vanica's), a mending of the flesh (Dante's), and the ultimate | `LuciusMoves` |
| Lucius: Grimoire Tower Access (any spell from the Clover Kingdom's history) | The Towers Answer, Grimoire Tower Storm: pages read from the mod's own grimoires | `TowerSpells`, `TowerMath` |
| Lucius: Combat tactics: multiple realistic clones | Mirror Court, Swap Strike, `ZogratisCloneEntity` | `ZogratisCloneEntity`, `LuciusMoves` |
| Lucius: base and transformed / divine visual models | `lucius_base` and `lucius_divine` (three pairs of wings, gear halo, antlers, floating grimoire) | `tools/gen_zogratis_lucius.py`, `client/zogratis/` |
| "Make sure the generated code is optimized" | Section 11 | everywhere |

## 3. File map

### 3.1 Java, common side: `entity/zogratis/`

"Pure" means no Minecraft types (compiled and unit tested with plain `javac`); the rest is thin glue over the pure classes.

**Framework**

| File | Kind | Purpose |
|---|---|---|
| `ZogratisBoss.java` | glue | The shared fight: power steps, Gates transformation, move casting with telegraphs, arena and leash, abandon/reset, block edits and the ledger, Dark Disciples, boss bar, chat, save data, spawning. All four bosses extend it. |
| `package-info.java` | doc | "How to write a boss" in six steps. |
| `Move.java`, `MoveSpec.java` | glue / pure | One move: its numbers (`MoveSpec`) and the code that runs when the wind-up ends. |
| `MoveScheduler.java` | pure | Picks the next move (range, power, cooldown, mana, ultimate pacing, repeat limit, weights), deterministic for a seed. |
| `MoveBook.java` | pure | The checked move list (unique ids, order = the index the client is told). |
| `PowerCurve.java` | pure | The Percentage Power System: health to power, rage, step-up, damage clamp, the full tuning table. |
| `ManaPool.java` | pure | Finite mana below 60% power, unbound from 60%. |
| `DamageMath.java`, `ZogratisDamage.java` | pure / glue | How a Zogratis hit becomes damage (power x boss scale x victim EP factor, then `BalanceLaw`). |
| `DiscipleStats.java` | pure | Dark Disciple numbers from its share of the devil's power. |
| `DevilStyle.java` | pure | Colours and names per devil. |
| `Tells.java` | glue | Server-side telegraph dust: rings, lines, columns, motes, bursts. |
| `TriadBond.java` | glue | Weak registry of living Triad members; when one falls the others rage. |
| `BlockLedger.java`, `LedgerQueue.java` | glue / pure | Every changed block is recorded here first and put back later (inside `DungeonGuard.internal`). |
| `VolleyBudget.java` | pure | Limits how many times one volley may hit one victim. |
| `ZogratisEntities.java` | glue | Registers the 4 bosses, Dark Disciple, Blood Beast, clone, the 4 spawn eggs, attributes, and `isFamily(...)`. |
| `ZogratisEffects.java` | glue | Mob effects `soul_paladin` and `purified`. |
| `ZogratisCommand.java` | glue | `/multiverse boss dante / vanica / zenon / lucius / triad`. |
| `DarkDiscipleEntity.java` | glue | The elite follower (one telegraphed ability per devil). |

**Dante**: `DanteBoss.java` (glue, the boss), `DanteTable.java` (pure, move numbers), `DanteMoves.java` (glue, the moves, reused by Lucius), `GravityMath.java` (pure), `BodyRegen.java` (pure state machine of the Body Magic pulse).

**Vanica**: `VanicaBoss.java`, `VanicaTable.java` (pure), `VanicaMoves.java` (reused by Lucius), `VanicaMath.java` (pure), `VanicaCurse.java` (Curse of Decay in the world, registers the `megicula_decay` effect), `CurseBrand.java` (pure state machine of one brand), `BloodBeastEntity.java`.

**Zenon**: `ZenonBoss.java`, `ZenonTable.java` (pure), `ZenonMoves.java` (reused by Lucius), `BoneMath.java` (pure), `BoneShell.java` (pure, the armour pool), `PortalMath.java` (pure), `SpellSeal.java` (the cube in the world), `DomainCube.java`, `DomainClock.java`, `DomainMath.java`, `DomainIntegrity.java` (all pure).

**Lucius**: `LuciusBoss.java`, `LuciusTable.java` (pure), `LuciusMoves.java` (Soul Magic, siblings, clones), `LuciusFx.java` (shared helpers, `Shot` homing bolt), `ChronoMoves.java` (Time Magic), `ChronoMath.java` and `ChronoRing.java` (pure), `Precognition.java` (pure), `JuliusWill.java` (pure), `SoulPaladins.java` (the Paladin mechanic and its two game-bus handlers), `PaladinLedger.java` (pure), `SoulMath.java` (pure), `TowerSpells.java` (reads the grimoire library, casts it), `TowerMath.java` (pure), `ZogratisCloneEntity.java`.

### 3.2 Java, client side: `client/zogratis/`

| File | Purpose |
|---|---|
| `ZogratisClient.java` | `register(EntityRenderersEvent.RegisterRenderers)`: renderers for all seven entities. |
| `ZogratisRenderer.java` | Base: GeoLite model drawing, one pose clock per entity, hit tint, death fade, culling box. |
| `ZogratisBossRenderer.java` | The four bosses (one `Look` each): base to hybrid / divine model swap at the middle of the transformation, hover, glow tint, Lucius's book. |
| `ZogratisDiscipleRenderer.java`, `ZogratisBeastRenderer.java`, `ZogratisCloneRenderer.java` | Dark Disciple (texture variant per devil), Blood Beast (rise / crouch / leap / puddle), Lucius's clone. |
| `LuciusBook.java` | Lucius's floating open grimoire, drawn on him and on his clones. |
| `ZogratisPose.java` (pure) | Which animation clip plays at which second (dying, transform, cast, swing, hurt, run, walk, idle). |
| `ZogratisGlow.java` (pure) | Glow brightness, the transformation flash, death fade, hover. |

No `net.minecraft.client.*` import exists in `entity/zogratis/`.

### 3.3 Assets: `assets/nusmp/`

| Path | What |
|---|---|
| `geo/entity/zogratis_{dante,vanica,zenon}_{base,hybrid}.geo.json` | The six Triad bodies. Base = the full-body look of the second reference (Dante: broad, gold crown, swept-back hair, black cross scar marks, goatee, green eyes, brown tunic, red scarf; Vanica: slim, crown, black hair, one red eye and an eyepatch with a gold emblem, open smile, belted short dress with a ring buckle, black sleeves, thigh-high boots; Zenon: lean, no crown, short black hair, vertical scar with cross bars through forehead and eye, pale eyes, maroon belted vest). All three wear the huge white-grey fur collar over a tall black collar. Hybrid = the 100% devil body (Dante: violet patches, horns through the crown, glowing eyes, gravity halo; Vanica: curse sigils, horns, long red claws, halo of blood droplets; Zenon: bone spines and ribs through the cloak, bone crest, insect wing remnants, hollow glowing eyes). |
| `geo/entity/zogratis_lucius_{base,divine}.geo.json` | Lucius. Base: pale man, short spiky black hair, red eyes, forehead sigil, faint smile, white fur mantle over a dark collar and choker, black cloak, brown-maroon robe with crossed straps, a hand raised with a spark. Divine: floating and barefoot, pale-blue and white robe, fur mantle and crossed chain, three pairs of feathered wings, golden gear-and-lattice halo, golden stag antlers, gold eyes. |
| `geo/entity/zogratis_disciple.geo.json`, `zogratis_blood_beast.geo.json`, `zogratis_grimoire_prop.geo.json` | Dark Disciple (hooded, bone mask), Blood Beast (lion-wolf of blood), Lucius's floating grimoire. |
| `geo/entity/zogratis_soul_paladin.geo.json` | An armoured white-gold knight. **Not drawn by anything yet** (section 14). |
| `animations/entity/zogratis_<same 12 names>.animation.json` | Clips `idle walk run attack cast cast_big hurt transform death` on every humanoid; the Blood Beast has `idle walk run attack hurt death spawn`. |
| `textures/entity/zogratis_<name>.png` and `_glow.png` | 32 PNGs: each model plus its additive glow layer; the Disciple has a neutral skin plus `_lucifero`, `_megicula`, `_beelzebub`, `_astaroth` variants. Humanoids are 512 px wide at two texels per model pixel. |
| `textures/mob_effect/{megicula_decay,purified,soul_paladin}.png` | 18x18 effect icons. |
| `models/item/{dante,vanica,zenon,lucius}_zogratis_spawn_egg.json` | Spawn egg item models (parent `item/template_spawn_egg`). |

### 3.4 Tools

| File | Purpose |
|---|---|
| `tools/gen_zogratis_triad.py` | Builds the six Triad models, the Disciple and the Blood Beast (geo, animation, texture, glow) and `tools/render_preview/scenes.d/zogratis_triad.json`. `python3 -B tools/gen_zogratis_triad.py [name ...] [--manifest out.json]`. |
| `tools/gen_zogratis_lucius.py` | Builds Lucius (both forms), the Soul Paladin and the grimoire prop, and `scenes.d/zogratis_lucius.json`. |
| `tools/zogratis_lucius_kit.py` | Shared kit of the Lucius generator: texture packer, pixel canvas, material painters, animation helpers. |
| `tools/gen_megicula_decay_icon.py` | The `megicula_decay` effect icon. (No generator script exists in the repository for `purified.png` and `soul_paladin.png`.) |
| `tools/render_preview/scenes.d/zogratis_triad.json`, `zogratis_lucius.json` | Headless preview scenes (`python3 -B tools/render_preview/preview.py <scene>`). |

### 3.5 Tests (JUnit, pure classes only)

`entity/zogratis/`: `BodyRegenTest`, `BoneMathTest`, `ChronoMathTest`, `ChronoRingTest`, `CurseBrandTest`, `DanteTableTest`, `DiscipleStatsTest` (also covers `DamageMath` and `DevilStyle`), `DomainCubeTest`, `DomainRulesTest` (clock, size and integrity of the domain), `GravityMathTest`, `JuliusWillTest`, `LedgerQueueTest`, `LuciusTableTest`, `ManaPoolTest`, `MoveBookTest`, `MoveSchedulerTest`, `PaladinLedgerTest`, `PortalMathTest`, `PowerCurveTest`, `PrecognitionTest`, `SoulMathTest`, `TowerMathTest`, `VanicaMathTest`, `VanicaTableTest`, `ZenonTableTest`. `client/zogratis/`: `ZogratisGlowTest`, `ZogratisPoseTest`. The four `*TableTest` classes also play a whole fight through the scheduler and check what each boss does at each power and distance. 30 classes, 284 tests. `TriadBond`, the entities, the commands and every world-touching class have no unit test (they need the game). `BoneMathTest` also covers `VolleyBudget`.

### 3.6 Lang keys

The 14 keys were written as flat JSON fragments (in the session scratchpad, not in the repository) and are now merged into `assets/nusmp/lang/en_us.json`: `entity.nusmp.{dante_zogratis, vanica_zogratis, zenon_zogratis, lucius_zogratis, dark_disciple, blood_beast, zogratis_clone}`, `item.nusmp.{dante,vanica,zenon,lucius}_zogratis_spawn_egg`, `effect.nusmp.{megicula_decay, soul_paladin, purified}`. Boss speech, move names and chat warnings are literals in the code, not lang keys.

## 4. Spawning and server settings

**Commands** (permission level 2; the `boss` node of `/multiverse` is also admin only):

| Command | Result |
|---|---|
| `/multiverse boss dante` / `vanica` / `zenon` / `lucius` | Spawns that boss at the executor with a summoning circle and a sound; **the arena is that spot**. |
| `/multiverse boss triad` | Spawns Dante, Vanica and Zenon on a ring of 8 blocks round the executor, facing them, sharing one arena (the executor's spot). They are linked by `TriadBond`. |

Unlike Zagred, the Zogratis commands are **not** behind a gamerule or config switch (`ZogratisCommand` has no gate). `ZogratisCommand.addTo(...)` is called on the existing `boss` node of `MultiverseCommands` (section 13).

**Spawn eggs** (in the mod's main creative tab, through `ZogratisEntities.spawnEggs()`): `dante_zogratis_spawn_egg`, `vanica_zogratis_spawn_egg`, `zenon_zogratis_spawn_egg`, `lucius_zogratis_spawn_egg`. An egg-spawned boss has no summoning circle; its arena is the block it stands on at its first tick. Use the `triad` command to get the three in a shared arena.

**Gamerules and config that matter** (facts from `NUGameRules` / `NUConfig`):

| Setting | Default | Effect on the Zogratis fights |
|---|---|---|
| `/gamerule nusmpGriefBlocks` or server config `griefBlocks` (either one on is enough) | off | Gates all terrain changes (`NUGameRules.griefBlocks`). **Off: effects only**, no block is ever changed. |
| `/gamerule nusmpPvpHitCapPercent` (config `pvpHitCapPercent`) | 40 | The most one hit may take from a player, as a share of their max health. |
| `/gamerule nusmpSpellDamagePercent` (config `spellDamagePercent`) | 100 | Global addon spell damage factor, inside `BalanceLaw.damage`. |

No `ModConfigSpec` entries were added: every tunable is a `private static final` constant (section 12).

**Immunities.** A Zogratis boss never takes a *harmful* effect whose id contains `paraly`, `stun`, `petrif`, `sleep`, `fear`, `freez`, `restrain`, `stasis` or `time_stop` (`canBeAffected`). Members of the family (`ZogratisEntities.isFamily`: the four bosses, Disciples, Blood Beasts, clones) never hurt each other, and bosses are allied to each other.

**Boss bar and chat.** The bar is "Name - Devil NN%" (plus " - Gates open"), coloured by devil (Lucifero purple, Megicula red, Beelzebub blue, Astaroth yellow). An ultimate is named on the action bar of everyone within 64 blocks when its wind-up begins. The bosses speak in chat in their own voices; each power step, the Gates and death have a line.

## 5. The shared fight framework

### 5.1 The Percentage Power System

Each boss "draws out" its devil's power in percentages. Power is a pure function of health that **never goes down** (`PowerCurve.next`):

| Health | Power | What happens |
|---|---|---|
| 100% to 85% | **20%** | Opening. Finite mana. |
| under 85% | **40%** | First step. The moves marked 40 in the tables below unlock. |
| under 65% | **60%** | **Mana is unbound** (infinite). The 60%-ultimates unlock. |
| under 45% | **80%** | The cap. Stays 80% while the Gates are shut. |
| under 25% | **100%** | **The Gates of the Tree of Qliphoth open**: the transformation. |

Rules that keep the steps honest (all in `PowerCurve` / `ZogratisBoss.hurt`):

- **No step is skipped.** A boss that is due several steps in one tick climbs them one at a time (`stepUp`), each announced in chat, each with a ring of the devil's colour. `clampDamage` makes one hit stop half a point under the next line, so a big blow cannot carry the boss past a step.
- **No death between the last line and the Gates.** A boss that has crossed the 25% line but whose AI has not yet opened the Gates is unhittable (`hurt` returns false) until it does, and during the 60-tick transformation it is invulnerable (with a game-time deadline so the shield can never outlast it).
- **Triad rage.** When a sibling falls, each living Triad member takes one 10-point step (`PowerCurve.rage`: 20 to 30, 40 to 50, 70 to 80), **never past 80%**; 100% is only ever reached by the Gates. The tuning interpolates between the anchors, so 30, 50 and 70 are real values. Lucius is not part of the bond.
- A reset (everyone left, section 5.7) sets power back to 20%.

### 5.2 What power means: the tuning table (`PowerCurve.tuning`)

Anchors at 20, 40, 60, 80, 100; values between are linear.

| Power | Damage x | Cooldown x | Speed x | Wind-up x | Regen (max health per s) | Dark Disciples at once | Volley size (`castSlots`) | Min gap between ultimates | Mana pool / regen per s |
|---|---|---|---|---|---|---|---|---|---|
| 20% | 0.60 | 1.00 | 1.00 | 1.00 | 0.05% | 1 | 1 | 2400 ticks (120 s) | 100 / 2.5 |
| 40% | 0.80 | 0.92 | 1.03 | 1.00 | 0.10% | 2 | 1 | 1800 (90 s) | 140 / 3.5 |
| 60% | 1.00 | 0.70 | 1.06 | 0.95 | 0.15% | 3 | 2 | 1500 (75 s) | 200 / 9, **unbound** |
| 80% | 1.25 | 0.55 | 1.10 | 0.85 | 0.22% | 4 | 3 | 1200 (60 s) | 260 / 12, unbound |
| 100% | 1.60 | 0.42 | 1.18 | 0.75 | 0.30% | 6 | 4 | 900 (45 s) | 320 / 16, unbound |

- The cooldown drop from 0.92 to 0.70 between 40% and 60% is the "mana is unbound" moment.
- The movement speed multiplier is applied as an attribute modifier (`zogratis_power_speed`).
- The boss thinks every `max(2, round(5 x cooldown x))` ticks: 5 ticks at 20%, 2 at 100%.

### 5.3 Mana scaling (`ManaPool`)

Below 60% power a boss has a **finite pool** and every move costs mana (`6 + cooldown/16`, at most 32; an ultimate 60). A boss that chains its big moves runs dry and has to wait for the pool to refill, which is the party's window. Each power step re-sizes the pool and **fills it**. **From 60% the pool is unbound**: moves cost nothing, `available()` is infinite. (The ultimates of the Triad need 60% anyway; Lucius's Soul Purge at 40% is the one ultimate that spends 60 mana.) Bosses that learn Tensura skills are also topped up to 2,000,000 of Tensura mana every 10 seconds, so those skills never run dry.

### 5.4 The Gates of the Tree of Qliphoth: the 100% transformation (`ZogratisBoss`)

1. Health drops under 25% and the power becomes 100: the boss cancels any cast, stops, and becomes invulnerable. The state is `STATE_TRANSFORMING` for `PowerCurve.TRANSFORM_TICKS` = **60 ticks (3 s)**.
2. Chat: "The Gates of the Tree of Qliphoth open!" plus the boss's own line. A devil circle, an expanding ring and gathering motes play; a wither-spawn sound.
3. At the **middle** (tick 30) the form changes (`setForm(gatesForm())`): Triad to `FORM_HYBRID`, Lucius to `FORM_DIVINE`. The hitbox is refreshed (`formScale`), a burst of dust and a resonance sound mark it; the renderer swaps to the hybrid / divine model and plays each half of the transformation clip.
4. After it the boss recovers into combat; all tuning is at the 100% row, plus each boss's own Gates upgrades (below).

| Boss | What the Gates add |
|---|---|
| Dante | Fields 1.35x bigger, last 120 ticks, and chain into a second field; the Singularity sets 4 spheres and is cast a second time once per fight; Body Magic pulse 26% (from 14%), cooldown 26 s, trickle doubled; Disciple waves at +80, +260 and +460 ticks. |
| Vanica | Rotting Field radius x1.3 and spreads in 28 ticks; lances up to 6; Blood Beast cap 8, beasts 1.35x tougher; Curse level 3; Burst radius 9; Crown ring 13 blocks with 14 thorns and a stampede of 6; Disciple waves. |
| Zenon | Cube 22 blocks across for 13 s with two chained Portal Link waves; evasion every 3 s; Portal Link 5 pairs, Warp Barrage 5 hops; bone grows twice as fast; Disciple waves. |
| Lucius | Divine form; precognition 3 charges recharging in 6 s; Time Stop holds 3 s and has a third blow; Beyond Light Barrage 4 steps; clone cap 6; Resurrection raises 3. |

### 5.5 Moves: telegraph, wind-up, recovery, pacing (`Move`, `MoveScheduler`)

- **A telegraph for every big attack.** Every move with a wind-up has at least **8 ticks** (`MIN_WINDUP`) at every power; **an ultimate at least 30** (`MIN_ULTIMATE_WINDUP`), and all the ultimates actually wind up for 56 to 72 ticks before the power scaling. Each move has its own tell (`Move.tell`), called every third tick; a move without one gets the default ring of the devil's colour.
- **Locked marks.** The big moves record their ground marks when the wind-up starts and land exactly there, whoever moves: moving off the mark is always the answer.
- **Cooldown** = `max(10, move cooldown x cooldown multiplier)`; **wind-up** = `max(8, wind-up x wind-up multiplier)`.
- **Recovery.** After a move the boss starts nothing for `recoveryTicks` (8 by default). An ultimate leaves it **exposed for 60 ticks (3 s)**: state `STATE_RECOVERING`, it takes **x1.25 damage** (`EXPOSED_BONUS`), and the passives weaken: Dante's pulse and trickle are halved, Lucius's precognition does not work, Zenon never slips a blow.
- **Pacing.** A move is allowed when power, range band, cooldown and mana allow it; ultimates only after the first 400 ticks of a fight, never twice in a row, and not closer than the tuned gap; the same ordinary move never a third time in a row; ultimates get likelier as health falls (up to x3), crowd moves are preferred against two or more foes (x1.8) and shunned against one (x0.5), and a repeat is discouraged (x0.35). The boss walks to the distance its ready moves ask for (clamped per boss, section 1), strafing inside the band.
- When no move fits, the boss closes in and uses its plain blow (about once a second).
- Every move and every tell is wrapped in a try/catch: a move that throws is logged once and the fight carries on.

### 5.6 The damage law (`DamageMath`, `ZogratisDamage`)

Every Zogratis hit (bosses, Disciples, Blood Beasts, clones) is: `base x power damage multiplier x the boss's damage scale x victim factor`, then `BalanceLaw.damage`.

- **Victim factor** (`DamageMath.victimFactor`): x1 up to 1,000 EP, then **+0.5 per decade of EP**, at most **x3 (10 million EP)**. So a fresh player takes the base, and a strong Tensura player takes up to three times as much. EP 10,000 is x1.5, 100,000 x2.0, 1,000,000 x2.5.
- `BalanceLaw.damage` then multiplies by the attacker's own EP scale, a mastery factor (0.85 + 0.3 x the boss's power fraction), the server's spell-damage factor and the mana-law power multiplier, adds a sliver of the victim's max health, and **caps one hit at the pvp share of a player's maximum health (40% by default)**. No single hit is a one-shot on a full-health player; multi-hit moves can still add up, which is why volleys have per-victim hit budgets.
- On Hard difficulty the game multiplies a mob's hit by 1.5 after the cap, so the cap is divided by 1.5 first (`ZogratisDamage.hit`) and stays true to what lands.
- Magic hits ignore armour (`indirectMagic`); plain hits are mob attacks, so a **raised shield stops** them. The gravity moves use Tensura's own gravity damage types (so gravity resistance counts), the curse bite uses Tensura's curse type.

### 5.7 Arena, leash, reset

- The arena is the spot the boss was summoned at (`setArena`), radius `ARENA_RADIUS = 56`. A boss further out is called back by a spatial rift. A fighter further than 112 blocks (and nearer than 200) from the centre is teleported to a spot 12 blocks from the centre on the side they were on, at most once every 10 s, with the line "Return." A target further than 90 blocks from the arena is dropped.
- A fighter is a player who came within 40 blocks or hit the boss (or whose summon did).
- **Abandon rule.** If no player (survival) has been within 100 blocks of the arena centre for **10 seconds** the fight resets: full health, power 20%, base form, Disciples removed, pending edits dropped, and the world put back (section 10).
- The boss is saved and loaded with its power, form, fight flag, mana, arena, ledger and its boss-specific state.

### 5.8 Dark Disciples (`DarkDiscipleEntity`, `DiscipleStats`, `PowerCurve.discipleShare`)

Elite followers infused with a share of their devil's power: **share = 0.4 x the owner's power fraction when summoned**, so 8% at 20% power up to the maximum **40% at 100%**. Stats grow with `fill = share / 0.4`:

| Owner power | Share | Health | Attack | Armor | Speed | Ability cooldown | Ability damage (x attack) |
|---|---|---|---|---|---|---|---|
| 20% | 8% | 108 | 6 | 4 | 0.252 | 146 ticks | x1.36 |
| 40% | 16% | 156 | 8 | 6 | 0.264 | 132 | x1.52 |
| 60% | 24% | 204 | 10 | 8 | 0.276 | 118 | x1.68 |
| 80% | 32% | 252 | 12 | 10 | 0.288 | 104 | x1.84 |
| 100% | 40% | 300 | 14 | 12 | 0.300 | 90 | x2.00 |

Each has one telegraphed ability by devil: **Lucifero** a gravity pull then a slam (3.6 blocks, wind-up 26), **Megicula** a curse pulse (3 blocks, wind-up 20; its touch gives Weakness), **Beelzebub** a blink-step behind its target (it marks the spot first, wind-up 16), **Astaroth** a slow then a dash (wind-up 22; no boss summons this variant). A Disciple shares its master's target, never exceeds the power's cap per master, drops nothing but experience, and is unmade when its master dies, resets or is gone. The Triad summon them: one answers each power step (as many as the new step number, up to the cap), the move *Call the Disciples* (2, 3 at the Gates), and three waves of 2 after the Gates (+4 s, +13 s, +23 s). **Lucius does not summon Dark Disciples** (he has Paladins and clones).

### 5.9 The Dark Triad bond (`TriadBond`)

A weak registry of living Triad bosses per world. When one falls, every other living Triad member (same world) takes one rage step and says so. It holds members weakly and purges removed or dead ones whenever it is touched, so an unloaded chunk or a closed world cannot leak. Lucius's Soul Purge reaches the bond too (section 9).

## 6. Dante Zogratis (Lucifero: gravity; native magic: Body Magic)

1200 health, armor 14. The fight is a lesson in not standing still: heavy blows up close, gravity fields on the ground, rock swords at range.

### 6.1 Body Magic (passive, `BodyRegen`)

Superhuman strength is in the moves; the **near-instant regeneration** is a pulse that answers a mortal wound, with fair, visible counters (the chat teaches them).

- **Pulse.** A hit that would leave him at or under **30%** of max health (judged before armour, ignoring blows the invulnerability frames swallow) closes at once and restores a share of max health: **8% / 10% / 12% / 14% / 26%** at power 20 / 40 / 60 / 80 / 100 (stage steps). Then it needs **800 / 760 / 700 / 640 / 520 ticks** (40 s down to 26 s) and **each pulse is 25% weaker than the last** (x0.75 per pulse used).
- **Trickle.** 0.04% / 0.05% / 0.06% / 0.075% / 0.15% of max health per second on top of the framework's regeneration.
- **Counters.** (1) **Anti-magic** (a demon sword, the Anti-Magic Lord, or the floating Anti-Magic grimoire) cuts the Body Magic for **8 s**: no pulse, no trickle, no framework regeneration, and the pulse cooldown restarts. (2) **Burst damage**: damage dealt in the last ~1.5 s (a faded counter) of 10% of max health halves the pulse; 20% denies it; a blow bigger than health plus pulse is simply lethal. (3) **Exposed** after the Singularity: the pulse and the trickle are halved. (4) Waiting out the cooldown.
- **Body Magic: Surge** (move, 40%+): for 8 s his blows hit 25% harder, Speed I, and he heals 3% (not while cut).

### 6.2 Moves (`DanteTable`, `DanteMoves`)

Base damages are before the damage law. "Wind/CD" are ticks before the power scaling.

| Move | Power | Wind/CD | Range | Base damage | Tell | Counter |
|---|---|---|---|---|---|---|
| Compressed Rock Swords | 20 | 22 / 120 | 6 to 34 | 6.5 per sword; 3 / 3 / 5 / 7 / 9 swords at 20-100% | Aim lines of the fan drawn from his hands | Sidestep; swords stop at the first wall; a raised shield stops them |
| Titan Fist | 20 | 14 / 70 | 0 to 5.5 | 19 (x1.25 in a surge) | A red ring growing to 4.6 blocks and a line along his facing | Step out of the half-circle; it throws you up |
| Crushing Stomp | 20 | 20 / 150 | 0 to 9 (crowd) | 13 | A ring growing to the reach (9; 11 at the Gates) and a steady red edge | Jump the wave (feet 0.75 blocks off the floor) or leave the radius |
| Lucifero's Domain | 20 | 28 / 280 | 5 to 32 (crowd) | field: one crush a second of 1.2 x strength x weight x build-up | A ring on the spot where you stood when the wind-up began, with a shrinking inner ring and a column | Walk out: strongest at the core (weight 1) and 0.35 at the rim, nothing outside; anti-magic halves it |
| Gravity Pull | 40 | 22 / 220 | 8 to 30 (crowd) | slam 14 | A red ring of 5.5 blocks and a line from each foe to him | Resist with anti-magic (pull x0.5), jump the slam wave, leave the ring |
| Weighted Leap | 20 | 26 / 200 | 9 to 40 | 15 | A ring (7 blocks) at your feet, locked | Leave the ring; jump the wave. He lands on the mark only if there is floor there |
| Stone Spikes | 20 | 18 / 130 | 4 to 26 | 12 | Two parallel lines along the locked direction | Step off the line (3.4 blocks wide, one block per two ticks, up to 30 blocks) |
| Lucifero's Weight | 40 | 24 / 180 | 0 to 36 | 15 | A red ring (2.8 blocks) locked on you | Leave the ring; a shield cuts it to 35%, anti-magic to 40% |
| Body Magic: Surge | 40 | 20 / 520 | any | self | Orange motes gathering | Anti-magic; use the window |
| Call the Disciples | 20 | 30 / 700 | any | n/a | Ring and motes | Kill them, or kill him |
| Gravity Magic | 20 | 16 / 260 | 6 to 30 | n/a | Default | Casts one of the learned Tensura gravity / burden / earth skills (falls back to 3 rock swords) |
| **Gravity Singularity** (ultimate) | **60** | **56** / 1200, recovery 60 | 0 to 40 | 25.5 / 34 / 42.5 at 60 / 80 / 100% | Black spheres rise over his head; a red ring on the ground at **every** mark with a shrinking inner ring and a column; a chat line | See below |

**Lucifero's Domain** numbers: radius `(8 + stage)` blocks (8, 9, 10 and 11 by stage, and 12 x 1.35 = 16.2 at the Gates), strength `min(3.4, 1.2 + 0.6 x stage)`, 100 ticks (120 at the Gates), builds up over 16 ticks, up to Slowness VI at the core, forces foes down and crushes once a second, never causes fall damage. At the Gates a second field is announced 60 ticks later on another foe, shown for 24 more ticks, and then lasts 80 ticks.

**Gravity Singularity** (60%+, when his mana is unbound): the marks are **2 spheres at 60-79%, 3 at 80-99%, 4 at 100%**; the first on the target, the next on the other foes, then on a ring round the target, never stacked (`GravityMath.planMarks`). The zone radius is `5.5 + 1.5 x p` blocks with `p = power / 80` (6.6 at 60%, 7.0 at 80%, 7.4 at 100%). The wind-up is shown for the whole of its 56 ticks (about 2.7 s at 60% power, 2.1 s at 100% after the wind-up scaling), and **he releases every running gravity field first**, so nobody is pinned while dodging the spheres, and starts no new chained field. After the wind-up each sphere gathers for **30 ticks**; in the last 18 it drags foes towards it (gently: running works; fliers are brought down). At tick 30 it collapses: everyone in the zone takes the hit, once per collapse.
**Counters:** leave the ring; **jump on the last tick** (feet more than 1.1 blocks off the floor); raise a shield (x0.35); hold anti-magic (x0.40). With `griefBlocks` on, each sphere bites a bowl (radius 4 to 5, at most 420 blocks, ordered centre-out) queued through the boss's ledger; sand-like blocks are skipped. At the Gates he casts a second set of spheres once per fight, 60 ticks after the first set appears (30 ticks after it collapses), with its rings drawn from the first tick. The recovery leaves him **exposed for 3 s**: that is the window to hit him.

## 7. Vanica Zogratis (Megicula: curse-warding; native magic: Blood Magic)

900 health, armor 8, quick. She lives at mid range and smiles.

### 7.1 Blood drain (passive)

She heals by a share of the damage she deals: **15% at 20% power up to 40% at the Gates** (`VanicaMath.drainShare`), at most **1% of her max health per hit** and **4% per 2 seconds** (doubled in a Blood Frenzy), so a many-hit move never refills her. Her Blood Beasts heal on their bites too. **Counters:** burst her while she is not hitting anyone (the Crimson Crown leaves her exposed for 3 s and she starts nothing else meanwhile), and kill the beasts.

### 7.2 Moves (`VanicaTable`, `VanicaMoves`, `VanicaCurse`)

| Move | Power | Wind/CD | Range | Base damage | Tell | Counter |
|---|---|---|---|---|---|---|
| Blood Lances | 20 | 20 / 110 | 5 to 34 | 6 each; 2 / 2 / 3 / 4 / 6 lances at 20-100%; magic; bleeds (Wither) from 60% | Aim lines of the fan and gathering at her hands | Sidestep; walls stop them; they ignore armour and shields |
| Blood Scythe | 20 | 14 / 70 | 0 to 6.5 | 11 | The red outline of the 150-degree, 5.2-block crescent | Leave the cone; Wither from 40% |
| Blood Whip | 20 | 16 / 120 | 3.5 to 14 | 8 (magic) | A red line along the aim, two edges 1.3 blocks to each side | Step off the line; the first foe on it is yanked to 2.2 blocks of her and slowed for 1 s |
| Crimson Dash | 20 | 18 / 150 | 6 to 26 | 12 | Two parallel lines and a ring at the end point, drawn when the wind-up begins and kept | Step off the line (1.8 blocks wide, up to 22 blocks) |
| Blood Burst | 20 | 22 / 180 | 0 to 8.5 (crowd) | 9 (magic) | A ring growing to the radius (7.5; 9 at the Gates) and a steady edge | Leave the radius; Wither from 40% |
| Crimson Rain | 40 | 24 / 210 | 4 to 36 (crowd) | 9 per spot, 3 + volley size spots, radius 2.3 | Rings and columns at the locked spots | Leave the rings (each foe is hit once) |
| Rotting Field | 20 | 28 / 260 | 4 to 34 (crowd) | pulse 2.5 every 2.4 s | A ring locked where you stood, plus a chat line | Keep out: radius 6.5 + 1 per stage (x1.3 at the Gates, 16 at most), lasts 160 + 20 per stage ticks, spreads over 60 ticks (28 at the Gates). Weakness (II at the Gates), Wither from 40%. |
| Curse of Decay | 20 | 26 / 300 | 4 to 30 | bite 1.6 x a growing multiplier | A sigil ring locked where you stood, plus a chat line | Leave the ring **before it closes**; see below |
| Blood Beasts | 20 | 26 / 380 | any | beast bite | Motes and a ring at her feet | Kill the beasts (cap 2 / 3 / 4 / 5 / 8) |
| Blood Frenzy | 40 | 20 / 520 | any | self | Red motes | 8 s: faster, blows +20%, drain doubled and its caps doubled; burst her after |
| Call the Disciples | 20 | 30 / 700 | any | n/a | Ring and motes | As Dante's |
| Hex Magic | 20 | 16 / 260 | 6 to 30 | n/a | Default | Learned Tensura curse / darkness / shadow skill (falls back to 3 lances) |
| **Crimson Crown** (ultimate) | **60** | **60** / 1300, recovery 60 | 0 to 40 | thorns 14 each | A ring of marked thorn spots (8 to 14, radius 10.5; 13 at the Gates) drawn for the whole wind-up, a big blood effect, an expanding ring | Stand between the thorn rings (1.9 blocks each); a stampede of short-lived beasts (2 + stage, 6 at the Gates, 220 ticks each) and a rotting field start from her; **exposed for 3 s** afterwards |

**Curse of Decay** (`CurseBrand`, `VanicaCurse`): a sigil of radius 4.5 / 5 / 5.5 (curse level 1 / 2 / 3: level 2 from 60%, 3 at the Gates). Everyone standing in it when it closes (at most six) is **branded** for `200 + 20 x stage` ticks: the visible effect `megicula_decay`, Weakness (II from level 2), **every heal cut to 60% / 40% / 25%**, and a bite every second that grows 20% per second spent inside the sigil up to x2.0 / 2.2 / 2.4. It breaks when you **leave the sigil** (its life then burns 8 times faster, so a second or two), when you **fight with anti-magic** (3 times faster, half damage), when you are **cleansed** (milk or any effect strip), when it runs out, or when she falls. This is the "lethal, decaying curse" of the request, made readable.

**Blood Beasts** (`BloodBeastEntity`): hounds of blood that rise out of a puddle for about a second (inert, the telegraph), bite (Wither bleed, heal themselves half of what they take), and pounce from 4 to 9.5 blocks after a crouch. Health 28 at 20% power up to 97 at 80% and 162 at the Gates; attack 4 up to 9.6; a minute to live (220 ticks for the Crown's stampede); at most 14 alive in all; they drop only experience and vanish when she falls.

## 8. Zenon Zogratis (Beelzebub: spatial; native magic: Bone Magic)

950 health, armor 8, very fast, says almost nothing. Every blink is shown: a gate where he leaves and a gate where he arrives.

### 8.1 Spatial Evasion (passive)

The first **real** blow after a cooldown (8 s at 20% power, 7 / 6 / 4.5 s, **3 s at the Gates**) is slipped through a rift to a spot 5 to 8 blocks away (and does no damage, and does not wear his domain down). It needs a blow of at least 3 damage with an attacker, and it **never works while he is casting or transforming, while he is exposed, against anti-magic, or against damage that bypasses invulnerability**. Counter: strike when it has just been used, with anti-magic, or while he casts.

### 8.2 Moves (`ZenonTable`, `ZenonMoves`)

| Move | Power | Wind/CD | Range | Base damage | Tell | Counter |
|---|---|---|---|---|---|---|
| Bone Spear Barrage | 20 | 18 / 100 | 5 to 36 | 5.5; 3 / 3 / 5 / 7 / 9 spears; at most 3 hit one victim | Bone grows at his shoulders (5 ticks) and three aim lines | Sidestep; walls and shields stop spears |
| Bone Blade Dance | 20 | 14 / 90 | 0 to 12 | 6.5 per slash; 3 slashes (4 and 5 at the highest powers), 6 ticks apart | The two edges of the 120-degree, 4.2-block cone and a line to you if he must come in | Leave the cone or back out of reach; he comes in by a visible portal at most twice |
| Bone Garden | 20 | 26 / 200 | 3 to 34 (crowd) | 11; 5 to 8 spots, radius 2.3; a second burst (x0.6) at the Gates | Rings and columns at locked spots | Leave the rings; with grief on, bone columns grow beside them |
| Rift Backstab | 20 | 16 / 130 | 4 to 30 | 13 | The exit portal opens **behind you** for the whole wind-up, a chat line, a ring and a line to you | Turn and step aside once the portal opens; the cut reaches only 3.4 blocks and he retreats 8 ticks later |
| Portal Link | 40 | 24 / 190 | 6 to 38 (crowd) | 6.5; 2 / 2 / 3 / 4 / 5 pairs; two waves at the Gates | Entry rings in front of him, exit rings 6 to 9 blocks round you, lines from each exit to you | Read the exits and move: spears come out 18 ticks later, 3 ticks apart, aimed where you stand then |
| Dense Bone Lance | 20 | 34 / 220 | 10 to 42 | 20 (pierces everyone on the line once) | Two parallel lines locked at the start of the wind-up, red for the last third | Step off the line (2 blocks wide) |
| Bone Armour | 40 | 18 / 520 | any | shards 5 | Bone motes | A shell worth 4% + 5% x power-fraction of his max health (47.5 at 20% up to 85.5 at 100% for 950 health) for 8 s; burst it, or wait. It shatters outward (shards hit within 4 blocks) |
| Warp Barrage | 40 | 22 / 280 | 5 to 32 | 5; 3 / 3 / 4 / 4 / 5 hops | Two rings round you, an aim line per hop as each portal opens | He blinks round you 8 ticks apart, a spear 2 ticks after each arrival; keep moving |
| Spatial Cut | 20 | 16 / 240 | 6 to 30 | n/a | Default | Learned Tensura space / dimension skill (falls back to 3 spears) |
| Beelzebub's Disciples | 20 | 30 / 700 | any | n/a | Ring and motes | As Dante's |
| **Spatial Mana Domination** (ultimate) | **60** | **60** / 1200, recovery 60 | 0 to 36 | none (it is a seal) | The cube is drawn in dust from the start of the wind-up, with a footprint ring, a column and a chat announcement | Section 8.3 |

### 8.3 Spatial Mana Domination (Absolute Space): the rules (`SpellSeal`)

A cube of warped space that severs the mana of everyone inside who is not a Zogratis.

| | 60% power | 80% | 100% (Gates) |
|---|---|---|---|
| Side of the cube | 16 blocks | 19 | 22 |
| Time it stands | 7 s | 9 s | 13 s |
| Portal chain waves inside it | none | none | 2 |
| Integrity (hurt Zenon to break it) | 8% of his max health (76 for 950) | 11% (104.5) | 14% (133) |

- **Shape.** An axis-aligned box centred on the spot where the target stood when the wind-up began (locked), its floor 3 blocks under the ground, so it reaches `2 x half - 3` blocks above it. Zenon is moved inside it first if he is not. A caster has one cube at a time (a new one replaces the old); at most six cubes exist at once.
- **Timeline.** The edges are drawn in dust, four at a time, from the start of the *wind-up*. When the move fires the cube **closes for 16 ticks** during which nothing is sealed (and the action bar names the way out); then the seal bites until the cube's last tick, then it ends ("Mana flows again"). The wind-up plus the closing is always longer than the time a walking player (4.3 blocks per second) needs to reach the nearest wall from the middle (`DomainMath.leavableOnFoot`), so **it can always be walked out of**.
- **What is sealed** (everything inside, except the Zogratis family, spectators and the dead): (1) **grimoire spells** cannot be cast: the cube writes the mod's `nusmp_sealed_until` flag into each player inside every two ticks, which the existing grimoire cast path already checks ("Your grimoire has been sealed!"), and `GrimoireBook.castPage` also asks `SpellSeal.sealed(player)` directly; (2) **Tensura and ManasCore skills** are vetoed (`SpellSeal.vetoesSkill`, bound to the three ManasCore skill events in `NUSMP`; grimoire pages are exempt from this veto because the cast path above handles them and leaves the grimoire's utility pages alone); (3) **spell damage** a sealed attacker still lands (a spell in flight, a summon's spell) is cut to **a fifth** (`onIncomingDamage`; spell damage = vanilla magic or any non-physical Tensura damage type). **Weapons, items, movement and ordinary blows are untouched.**
- **Counter-play.** (1) Leave on foot: the nearest side wall and the distance are shown on the action bar, and the wall nearest you shimmers when you are within 3.5 blocks. (2) **Break it:** every point of damage dealt to Zenon while the cube stands also wears the cube down (anti-magic counts **double**), from inside or outside; when the integrity is gone it shatters and Zenon is staggered (Slowness III for 3 s). (3) It ends with its master (death, reset, removal). (4) He is exposed for 3 s after opening it.
- **While it stands** Zenon deals 15% more damage ("he rules the space"). State is in memory only (a world reload ends every cube).

## 9. Lucius Zogratis (Astaroth: time; two souls in one body)

3600 health, armor 22, toughness 12, speed 0.36 and steps beyond light. A calm duel with a god who answers everything and who is not quite alone in his body. He has 20 moves, four of them ultimates.

### 9.1 Soul Magic (innate)

| Move | Power | Wind/CD | Range | What it does | Tell / counter |
|---|---|---|---|---|---|
| Soul Motes | 20 | 16 / 80 | 0 to 38 | 2 + volley size slow homing motes (6.5 each, magic) | Motes gather and a line to you; they home weakly, so move and use cover |
| Soul Drain | 20 | 26 / 240 | 3 to 20 | A beam to up to 3 foes in sight for 3 s: 7 per pulse; he heals half of it (at most 0.6% of max health per pulse and **3% per cast**) | Lines and rings on the foes. Break line of sight or get 24+ blocks away; **anti-magic in hand halves the pulse and denies the healing** |
| Memory Manipulation | 40 | 30 / 520 | 0 to 40 (crowd) | Every player in 40 blocks without anti-magic takes a touch (4) and **the grimoire spell they cast last goes on a 6 s cooldown** | Lines from his eye to every foe. Hold anti-magic; Julius gives the spell back |
| Soul Magic: Paladins | 40 | 40 / 900 | any | Turns hostile mobs within 40 blocks into Soul Paladins (section 9.5); if there are none he calls a Vindicator knight | Lines and rings on the mobs he will take |
| Resurrection | 60 | 44 / 800 | any | Raises his kneeling Paladins and his fallen clones at once: 2 per cast (3 at the Gates), from a **budget of 3 + stage souls per fight** (3 to 7) | Rings on the kneeling; cut the Paladins' souls or spend his budget |
| **Soul Purge** (ultimate) | **40** | **64** / 1500, recovery 60 | 0 to 30 (crowd) | A wave of white and gold crosses the ground at one block per tick to 28 blocks. It strips devil power: ends Devil Union and meal attunement, removes this mod's blessings and any beneficial effect named for a devil / contract / attunement / union (a player who lost something is given the `purified` effect for 8 s, and while it lasts the grimoire refuses to enter Devil Union again: "Lucius's Soul Purge has stripped your devil's power for now."). 18 base damage (x1.5 in Devil Union, x0.5 behind a raised shield). It also **cuts the Dark Triad off from its devils**: each living sibling's mana is made finite and emptied for 8 s (12 s from 80%), so it can only use its fists, and its Dark Disciples are unmade. He heals 1.5% of max health per power stripped, at most 6% | A ring grows on the ground with the edge marked in red and a chat line. **Jump over the wave** (airborne above the ground when it passes); each foe is touched once |

### 9.2 Two Souls: Julius Novachrono (`JuliusWill`)

His other soul, the Wizard King, is asleep in the body. At **75%, 55%, 35% and 12%** health (each once per fight, however health moves afterwards; a hit that crosses two lines opens one window; a line crossed during a window waits, with a 100-tick pause between windows, and none opens during the Gates' transformation) Julius's will **surfaces for 8 seconds**:

- Lucius **stumbles**: every move he finishes fizzles ("Lucius's hand trembles"), his plain blow does nothing, he moves 60% slower, his clones fade away, his **precognition is off** (for the window plus 5 s) and he takes **40% more damage**.
- The boss bar becomes Julius's ("Julius Novachrono - the Wizard King's will") and a second white bar counts the window down.
- The party is **blessed**: harmful effects cleansed, healed 12% of max health, Regeneration II, Resistance and Speed for 8 s, **immune to time stops for 12 s**, and every spell is remembered (Memory Manipulation is undone).
- He speaks one of four lines; at the end Lucius says "Julius. Always so sentimental. Sleep now."

This is the "sleeper agent / memory manipulation" mechanic of the request, and it is also the fight's main damage window.

### 9.3 Time Magic: Astaroth (`Precognition`, `ChronoMoves`, `ChronoMath`, `ChronoRing`)

**Precognition (passive).** He holds **1 charge under 40% power, 2 under 80%, 3 from 80%**; a charge returns every 280 / 230 / 190 / 150 / 120 ticks (20 / 40 / 60 / 80 / 100%). A blow from an attacker he can read is **dodged** (he blinks 2.6 blocks aside if he is not casting), costs a charge, restarts the recharge, and he lashes the attacker (5 base, magic, Slowness II for 2 s). The action bar tells the attacker how many charges remain. It is beaten by:

| Counter | Rule |
|---|---|
| Two attackers at once | A blow from a *different* attacker within 12 ticks of a dodge is out of sequence: it lands and costs him nothing |
| Anti-magic | Always lands, and burns one of his charges |
| No attacker | Poison, fire, a field a player left behind: nothing to read, it lands |
| Spent charges | With none left the blow lands, with a chat line telling the party when the next returns |
| Open moments | Precognition does not work while he is exposed after an ultimate, and is off during Julius's window and for 2 s after a Time Reversal |

**Time moves.** All of them draw their line, mark or ring first and never exceed these limits:

| Move | Power | Wind/CD | What it does | Tell / counter |
|---|---|---|---|---|
| Beyond Light (steps beyond the speed of light) | 20 | 16 / 110 | He crosses up to 32 blocks along a line, hitting everything in the strip (20, plain blow, 1.5 blocks to each side) and shoving it along; walls shorten it | The line, both strip edges and an end ring are drawn for the wind-up. Step out of the strip |
| Beyond Light: Barrage | 60 | 18 / 380 | The step, then more: 2 steps in all below 80%, 3 from 80%, 4 at the Gates; each further step goes through a different foe and is shown **14 ticks** ahead | Same, for each step |
| Chrono Stasis | 40 | 34 / 420 | Stops **one** foe for 1.5 s (2 s from 80%; the balance law's control caps apply), steps in front of it and a mark closes on the spot; when the stop ends the mark strikes everything inside (24, radius 2.3). If the foe cannot be stopped the blow comes anyway | A clock effect on the foe and a ring that turns red in the last 14 ticks. Leave the ring when you can move; anti-magic prevents the stop |
| Chrono Acceleration | 40 | 20 / 700 | 8 s of +45% speed, plain blows x1.2, and **a precognition charge back** | Time ribbons on him and gathering motes |
| Time Reversal | 60 | 36 / 900 | He returns to where he was 3 to 5 s ago (60 ticks at the opening up to 100 at the Gates; the ring remembers 6 s), takes back some wounds (**at most 6% / 8% / 10% of max health**), and every harmful effect on him is unmade | A gold ring and column mark where he will reappear, and a chat line. **Anti-magic struck during the wind-up refuses it** |
| **Time Stop** (ultimate) | **80** | **64** / 1500, recovery 60 | Everyone who can see him and holds no anti-magic (up to 8) is stopped for **2.5 s (3 s at the Gates), never longer**. Nothing hurts them while stopped (that is what the in-game message says; the stop itself is the mod's existing `TimeStop` effect). When it ends they get **6 s of immunity to being stopped again**, then 2 (3 at the Gates) marked blows arrive: the first 18 ticks after the release, then every 14, **each marked on the ground 14 ticks ahead** under every foe, 16 base, radius 2.3 | A banner on the action bar ("hold anti-magic or break his line of sight"), a clock effect over each foe. **Anti-magic or cover prevents it; an ally's blow of 8 damage breaks it** (the existing `TimeStop`); leave the marks |

His own timing effects reuse the mod's existing Time Magic effects (stasis sphere, clock, rewind dial, acceleration ribbons); none of the Time Magic VFX was edited.

### 9.4 Grimoire Tower Access (`TowerSpells`, `TowerMath`)

He links to the towers and can cast **any page of any magic** the mod has: the library is read from the mod's own grimoire books (every non-restricted `MagicType`, every page that costs at least 5% mana), so the name shown on the action bar and in chat, "The towers answer: Flame Magic - Flame Bullet", is a real page. A mob has no grimoire to open, so a page is cast as the spell of its **tier** (the page's cost): under 10% a *bolt*, 10% to 18% a *lance*, 18% to 30% a *nova* or a lingering *field*, 30% and up a *rain*, in the colours of the magic with the magic's rider on every hit. A shuffled deck makes the towers answer with every magic before any repeats.

| Shape | Count at volley size 1..4 | Base damage | Marked before it lands |
|---|---|---|---|
| Bolt | 3 to 6 homing bolts | 6.5 | no mark (it flies: sidestep) |
| Lance | 2 to 3 beams on a drawn line | 11 | 14 ticks |
| Nova | 1 to 2 rings (radius 5) on foes | 13 | 24 ticks |
| Field | 1 lingering field (radius 4.5) | 5 per pulse | 22 ticks |
| Rain | 4 to 7 falling bolts (radius 2.2) | 9 | 16 ticks |

*The Towers Answer* (20%, wind-up 26, cooldown 130, weight 7: his most common move) draws a page; one cast in four uses a learned Tensura skill instead. **Grimoire Tower Storm** (ultimate, 80%, wind-up 72, cooldown 1500) is a barrage of 6 + stage answers (9 or 10), 18 ticks apart, each a different named magic, alternating rains and novas, 11 base per impact, every impact marked on the ground first.

### 9.5 Soul Paladins (`SoulPaladins`, `PaladinLedger`, `SoulMath`)

Not a new entity: an ordinary hostile mob (health 30 or more, **never a boss: 300 or more is refused**, never family) with a flag in its persistent data, the synced `soul_paladin` effect (+6 armor, +10% speed, an icon) and a place in Lucius's ledger.

- **Made** within 40 blocks, strongest first, up to **1 / 2 / 3 / 4** at once (20 / 40 / 60 / 80%+). Health becomes `1.6 + 0.4 x stage` times its own (at least 120, at most 600), attack gets +4 and +1.5 per stage (or +2 when it already hits hard). If he finds nobody to take, a Vindicator in iron arms rises as a Paladin (its gear does not drop).
- **Immortal while Lucius lives.** A blow that would kill a Paladin does not: it is **struck down**, kneeling at 1 health with no AI, immune to everything, with a soul tether drawn to Lucius, and **rises with full health after 12 s (8 s at the Gates)**. The first time, the chat explains how to keep it down.
- **Cutting the soul (the counter).** While a Paladin kneels, one **anti-magic** blow (a demon sword, the Anti-Magic Lord, the Anti-Magic grimoire) or one **holy / soul / severance / spirit** damage type kills it for real, with its drops. A kneeling Paladin takes no other damage.
- **Lucius falls or leaves the world:** every Paladin is freed (the mob it was before, mortal, its numbers back); a kneeling one dies with its tether.
- A Paladin fights with the mob's own attack (raised as above); that blow does **not** go through the Zogratis damage law of section 5.6, so its numbers come only from `SoulMath.paladinAttack`. Resurrection can raise kneeling Paladins at once (spending the souls budget). The effect is renewed every half second so milk does not free a Paladin, and the Paladins are pointed at Lucius's foes whenever they have no valid target.

### 9.6 Clones (`ZogratisCloneEntity`, `LuciusMoves`)

*Mirror Court* (20%, wind-up 28, cooldown 640, crowd) makes `2 + volley size` realistic copies on a ring 9 blocks round the target, up to **2 / 3 / 4 / 5 / 6** alive (20-100%). Gold columns and rings mark the spots in the wind-up. A clone wears his current body (human or divine) and his grimoire, **keeps 6 to 11 blocks away and circles**, and every 100 ticks (60 at the Gates) casts a *weak copy* of a tower spell: 14 ticks of telegraph, then two slow soul motes in a random magic's colour at 35% of his damage. It has 60 to 140 health, never melees, fades after 30 s (40 s at the Gates), shatters into soul motes when killed, and leaves nothing behind. Clones fade at once when Julius surfaces. *Swap Strike* (40%, wind-up 26, cooldown 260) trades places with the clone nearest the target, marked with a gold column and a line, and cuts a cone of 5 blocks (22 base); with no clone it is a light step.

### 9.7 Sibling Assimilation: absorbed Gravity, Body, Blood and Bone

| Magic | In his kit |
|---|---|
| Gravity (Lucifero's) | *Lucifero's Weight* (40%, wind-up 30, cooldown 290): `DanteMoves.gravityField` on the marked spot, radius 9 + stage, 90 ticks |
| Bone (Zenon's) | *Bone Arsenal* (20%, wind-up 22, cooldown 170): `ZenonMoves.boneSpears` (3 to 9 spears of 6.5) and blade slashes (8) if you are within 6 blocks |
| Blood (Vanica's) | *Blood Tide* (40%, wind-up 24, cooldown 240): `VanicaMoves.bloodLances` (up to 6 of 5.5), and from 60% a Blood Beast |
| Body (Dante's) | A slow mending (0.06% of max health x (1 + stage) per second, below 90% health) that **anti-magic stops for 8 s**; `DanteMoves.regenerate` inside the ultimate |

**Sibling Assimilation** (ultimate, 60%, wind-up 70, cooldown 1500): the violet ring of gravity falls on the marked spot at once; after 14 ticks Zenon's bone leaves his shoulders (5 + 2 x volley size spears, 7.5 base); after 40 ticks Vanica's blood (3 + volley size lances, 6 base, and beasts); after 70 ticks Dante's flesh mends him 3% and the chat tells the party he is **exposed**. Each part is announced by its colour a dozen ticks before it comes (violet, bone white, red).

### 9.8 Lucius's numbers by power

| Power | Precognition charges | Recharge | Paladin cap | Paladin revive | Clone cap | Resurrection budget | Stasis hold | Time Stop hold | Time Stop blows | Rewind | Rewind heal cap |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 20% | 1 | 14 s | 1 | 12 s | 2 | 3 | 1.5 s | n/a | n/a | 3 s | 6% |
| 40% | 2 | 11.5 s | 2 | 11 s | 3 | 4 | 1.5 s | n/a | n/a | 3.5 s | 6% |
| 60% | 2 | 9.5 s | 3 | 10 s | 4 | 5 | 1.5 s | n/a | n/a | 4 s | 6% |
| 80% | 3 | 7.5 s | 4 | 9 s | 5 | 6 | 2 s | 2.5 s | 2 | 4.5 s | 8% |
| 100% | 3 | 6 s | 4 | 8 s | 6 | 7 | 2 s | 3 s | 3 | 5 s | 10% |

### 9.9 Forms

Human (`lucius_base`) until the Gates; at the middle of the 3 s transformation the renderer swaps to the divine body (`lucius_divine`: three pairs of wings, gear halo, antlers), which **hovers about 0.4 blocks** above the ground. His grimoire floats before him in both forms (and on his clones). The glow turns white while Julius holds the body. Lucius's numbers are all at their peak at 100%.

### 9.10 All twenty moves at a glance

In the order of the move list (the client reads move names by index). Wind-up and cooldown are ticks before the power scaling of section 5.2; the minimum wind-up of any move is 8 ticks, of an ultimate 30.

| Move | Power | Wind/CD | Range | Kind | Described in |
|---|---|---|---|---|---|
| Soul Motes | 20 | 16 / 80 | 0 to 38 |  | 9.1 |
| Soul Drain | 20 | 26 / 240 | 3 to 20 |  | 9.1 |
| Beyond Light | 20 | 16 / 110 | 5 to 34 |  | 9.3 |
| The Towers Answer | 20 | 26 / 130 | 6 to 40 |  | 9.4 |
| Bone Arsenal | 20 | 22 / 170 | 4 to 34 |  | 9.7 |
| Mirror Court | 20 | 28 / 640 | 0 to 48 | crowd | 9.6 |
| Blood Tide | 40 | 24 / 240 | 3 to 30 |  | 9.7 |
| Lucifero's Weight | 40 | 30 / 290 | 3 to 34 | crowd | 9.7 |
| Swap Strike | 40 | 26 / 260 | 6 to 40 |  | 9.6 |
| Memory Manipulation | 40 | 30 / 520 | 0 to 40 | crowd | 9.1 |
| Chrono Stasis | 40 | 34 / 420 | 4 to 30 |  | 9.3 |
| Chrono Acceleration | 40 | 20 / 700 | 0 to 48 |  | 9.3 |
| Soul Magic: Paladins | 40 | 40 / 900 | 0 to 48 |  | 9.1 and 9.5 |
| Time Reversal | 60 | 36 / 900 | 0 to 48 |  | 9.3 |
| Resurrection | 60 | 44 / 800 | 0 to 48 |  | 9.1 |
| Beyond Light: Barrage | 60 | 18 / 380 | 5 to 30 |  | 9.3 |
| Soul Purge | 40 | 64 / 1500 | 0 to 30 | ultimate, crowd | 9.1 |
| Sibling Assimilation | 60 | 70 / 1500 | 0 to 40 | ultimate, crowd | 9.7 |
| Time Stop | 80 | 64 / 1500 | 0 to 30 | ultimate, crowd | 9.3 |
| Grimoire Tower Storm | 80 | 72 / 1500 | 0 to 44 | ultimate, crowd | 9.4 |

## 10. World safety (griefBlocks, budget, ledger, restore)

Only **three** moves change blocks: Dante's Singularity bite, Vanica's rot (the Rotting Field and the Crimson Crown's field) and Zenon's bone columns. **Lucius changes no blocks at all** (he reuses only the siblings' non-block moves), and neither do Disciples, Beasts or clones. The cube of Spatial Mana Domination is a rule in memory, not blocks.

1. **Respects `griefBlocks`.** Every change goes through `ZogratisBoss.tryChange` / `enqueueBlockChange`, which do nothing unless `NUGameRules.griefBlocks(level)` is true. With the rule off, the three moves still deal their damage and play their effects, and the terrain is untouched.
2. **Bounded per tick.** At most **48 block changes per tick** (`BLOCKS_PER_TICK`); big changes are queued (at most 4096 pending; 256 tried per tick). Per move: a sphere's bowl at most **420 blocks** (`GravityMath.CRATER_CAP`); a rotting field at most **360** (`VanicaMath.ROT_CAP`, at most 2 blocks per column); a bone garden at most **160 cells** (`BoneMath.GROWTH_CELL_CAP`), planted 24 per tick (48 at the Gates).
3. **Recorded and restored.** Every changed block is recorded in the `BlockLedger` (position, old state, state placed). The ledger holds at most **3072 positions** and refuses further changes when full. It is saved in the boss's NBT (as packed arrays with a palette), so a saved world keeps the restore information. The world is put back: when the boss **dies**, when it is **removed**, and when the fight is **abandoned** (restored gradually, 160 per tick). A block someone else changed since is left alone. Zenon's bone columns are taken back by themselves after 160 ticks.
4. **Inside the dungeon guard.** The restore writes run inside `DungeonGuard.internal(...)`, so even the Living Dungeon (whose blocks are unbreakable) accepts them. The other way round, the guard refuses the boss's own writes there; `tryChange` then returns false and records nothing.
5. **Only ordinary blocks.** Unbreakable blocks (destroy speed below 0), block entities and wither-immune blocks are never changed; falling blocks and fluids are skipped by the Singularity; the bone columns never close on an entity.

## 11. Performance notes

- **Pure logic is separated from glue.** The curves, tables, schedulers, geometry and state machines have no Minecraft types and are unit tested (section 3.5); the world-touching code reads their numbers.
- **Bounded everything.** Gravity fields look at no more than 32 foes every 4 ticks; projectiles are points on a clipped ray (no entities) with a life of 24 to 46 ticks and a per-victim hit budget (`VolleyBudget`); Dark Disciples are capped per power (1 to 6), Blood Beasts (hard cap 14), clones (6), Paladins (4); particle counts are capped per tell (rings at most 96 points, lines 64).
- **No per-tick scans of the world.** Block changes are budgeted (48 per tick) and drained from a queue; the ledger restore is stepped; the spell seal is a short list of cubes checked only when something asks (`sealed(...)`) and steps once every two ticks; the move scheduler thinks every 2 to 5 ticks, not every tick; the Triad registry is weak and purged lazily.
- **Telegraphs are plain particle packets** sent to players nearby (no client code needed for the tells), and each cube / ring uses a handful of points.
- **Every zone step and delayed task is wrapped** (`safe(...)`) so one failure stops that task, never the tick.
- **Client:** models are GeoLite (the same pipeline as the Direwolf and the Tower Wizard), one pose clock per entity, a culling box for wings and halos.

## 12. Tuning guide

Everything is a constant in code (no config entries). Most numbers live in the tables and math classes named below; a few move details are literals inside the boss's own move code (for example the radius and strength of Dante's Domain in `DanteBoss.fieldRadius` and `fieldStrength`, or the thorn damage in `VanicaTable`). Change it, build, and try it with the commands in section 4.

| To change | Edit |
|---|---|
| The power steps (health lines, 20/40/60/80/100) | `PowerCurve.GATE_HEALTH` and `GATE_POWER` |
| What each power means (damage, cooldown, speed, wind-up, regen, Disciples, volley size, ultimate gap, mana) | The arrays `DAMAGE`, `COOLDOWN`, `SPEED`, `WINDUP`, `REGEN`, `DISCIPLES`, `SLOTS`, `ULT_GAP`, `MANA_CAP`, `MANA_REGEN` in `PowerCurve`; `INFINITE_MANA_FROM` for the unbound-mana power; `RAGE_STEP`, `CAP`, `TRANSFORM_TICKS` |
| A boss's body (health, armor, speed, damage) | `createAttributes()` in `DanteBoss`, `VanicaBoss`, `LuciusBoss`; for Zenon the constants in `ZenonTable` |
| A boss's overall hit strength | `damageScale()` in each boss class; globally `DamageMath.MAX_VICTIM_FACTOR` and `victimFactor`; server side `/gamerule nusmpPvpHitCapPercent`, `nusmpSpellDamagePercent` |
| A move's wind-up, cooldown, range, weight, recovery, power gate | The `Spec` row in `DanteTable`, `VanicaTable`, `ZenonTable`, `LuciusTable` (order = the index the client reads) |
| A move's base damage | The constants at the top of the same tables (`FIST`, `LANCE`, `SPEAR`, `MOTE`, ...) |
| Shortest telegraphs, exposure, arena | `ZogratisBoss.MIN_WINDUP`, `MIN_ULTIMATE_WINDUP`, `EXPOSED_BONUS`, `ARENA_RADIUS`, `ABANDON_SECONDS`, `Move.EXPOSED_FROM`, `MoveScheduler.FIRST_ULTIMATE_DELAY`, `REPEAT_LIMIT` |
| Dante's Body Magic | `BodyRegen` (`MORTAL`, `PULSE`, `COOLDOWN`, `TRICKLE`, `STRAIN`, `SUPPRESS_TICKS`, `OVERLOAD_HALF`, `OVERLOAD_DENY`) |
| The Singularity | `GravityMath` (`GATHER`, `PULL`, `zoneRadius`, `collapseDamage`, `markCount`, `CRATER_CAP`), `DanteBoss.singularityPower` |
| Vanica's drain, rot, curse, beasts | `VanicaMath` (all of it), `CurseBrand` |
| Zenon's cube and evasion | `DomainMath`, `DomainClock`, `DomainIntegrity`, `ZenonTable.evadeCooldown`, `SpellSeal.SPELL_FACTOR` |
| Julius's windows | `JuliusWill.THRESHOLDS`, `DURATION`, `PAUSE`; `LuciusBoss.JULIUS_DAMAGE` |
| Lucius's precognition, time, Paladins, clones | `Precognition`, `ChronoMath`, `SoulMath`, `TowerMath` |
| Disciples | `DiscipleStats`, `PowerCurve.discipleShare` |
| World-change limits | `ZogratisBoss.BLOCKS_PER_TICK`, `BlockLedger.MAX_ENTRIES`, the caps listed in section 10 |
| Looks | The generator scripts in section 3.4, then re-run them; the glow feel in `client/zogratis/ZogratisGlow` |

After editing a pure class, run its test. The tests assert the shape of the numbers, for example: every move has a telegraph that keeps at least 8 ticks even at the Gates, an ultimate's table wind-up is at least 40 ticks (56 for Lucius's), no power step is skipped, power never falls, and a cube can be left on foot in the time its telegraph gives.

## 13. Where the bosses are wired into the rest of the mod

The Zogratis package owns its own registrations; the shared files were edited by the lead in the 1.20.0 release commit. As found in the checkout when this document was written:

| File | What it holds |
|---|---|
| `NUSMP.java` (constructor) | `ZogratisEntities.init()`, `ZogratisEffects.init()`, `ZogratisEntities.ENTITIES.register(modEventBus)`, `modEventBus.addListener(ZogratisEntities::attributes)`, `ZogratisEffects.EFFECTS.register(modEventBus)`, `VanicaCurse.EFFECTS.register(modEventBus)` |
| `NUSMP.java` (client only) | `modEventBus.addListener(ZogratisClient::register)` |
| `NUSMP.java` (game bus) | `VanicaCurse::onHeal` (Megicula's Decay cuts healing), `SpellSeal::onIncomingDamage` (a sealed attacker's spell damage is cut to a fifth), `SoulPaladins::onDeath` and `SoulPaladins::onIncomingDamage` (both at `EventPriority.HIGH`, so a Paladin is struck down before other handlers see the death) |
| `NUSMP.java` (ManasCore skill events) | `SkillEvents.ACTIVATE_SKILL`, `RELEASE_SKILL` and `TOGGLE_SKILL` each call `SpellSeal.vetoesSkill(skill, user)` and return `EventResult.interruptFalse()` when it is true, so Tensura and ManasCore skills cannot be used inside Spatial Mana Domination |
| `MultiverseCommands.java` | `ZogratisCommand.addTo(bossNode)` on the `boss` node (then `root.then(bossNode)`) |
| `NUCreativeTab.java` | the four eggs from `ZogratisEntities.spawnEggs()` in the main tab |
| `GrimoireBook.java` | In `castPage`: `SpellSeal.sealed(player)` refuses the cast with the seal's message (so the cube's refusal does not depend only on the `nusmp_sealed_until` flag it also writes). In the Devil Union entry: a player with `ZogratisEffects.PURIFIED` is refused ("Lucius's Soul Purge has stripped your devil's power for now.") |
| `assets/nusmp/lang/en_us.json` | the 14 keys of section 3.6 |

Nothing else in the mod calls into the package. To find every hook, search the source for `entity.zogratis`.

## 14. Unverified and not done

**Verified by the author of this document**

- The pure classes (every file marked pure in section 3, except `TriadBond` which needs the game) and all 30 test classes were compiled with `javac` in a throwaway directory outside the repository and run with the JUnit console launcher: **284 tests, all passed**. The numbers in the tables of this document for the tuning, Disciples, Body Magic, domain, Zenon, Vanica and Lucius were read off the output of running those classes, and every row of the Dante, Vanica, Zenon and Lucius move tables (power, wind-up, cooldown, range) was compared with the move tables by a script.
- All JSON assets, preview scenes and lang fragments of the family parse. The texture sizes and the animation clip names were read from the files.
- Every behavioural statement was written from reading the Java source on disk at the time of writing (not from the request or from other summaries). A few figures are plain arithmetic on those numbers (for example 12 x 1.35 = 16.2, or ticks turned into seconds).

**Not verified (nobody has run these in a game)**

- **Nothing has been compiled or run inside Minecraft.** The entities, renderers, commands, effects, event handlers and every move's world effect are written against 1.21.1 / NeoForge 21.1.x APIs but were not built by this document's author; the type-check harness and the CI compile are the checks. Gameplay, balance and visual quality are therefore unverified: the numbers are starting values.
- **Tensura / ManasCore integration** (the learned Tensura skills through `TensuraCaster`, Tensura's gravity and curse damage types, `Nullification.maxEP`, `TimeStop`, the grimoire library read by `TowerSpells`) depends on the installed Tensura and mod versions; the fallbacks exist (plain magic damage, a fixed library of four tiers per magic, volleys instead of a skill) but were not exercised.
- **The Living Dungeon guard** and the boss's block writes: the claim that a refused write leaves the ledger empty follows from the code order, not from a test.
- Whether the cast refusal in `GrimoireBook`, the `nusmp_sealed_until` flag and `SpellSeal.vetoesSkill` together stop every route to a spell (another mod casting through its own path is not covered). The ManasCore event signatures used by the veto lambdas in `NUSMP.java` were written by the lead and are not checked here.
- I did not render or look at any model, animation or texture: how the bosses look in game, and whether the animations fit the models, is unverified by me (the preview scene files exist for that).

**Not done / known gaps**

- **Boss behaviour inside the game is unobserved.** The wiring of section 13 exists in the source, but nobody has spawned a boss in a running game yet.
- **No loot, no advancement, no structure, no natural spawn.** The bosses give experience only; nothing was requested to drop, and nothing drops. A loot table (for example `data/nusmp/loot_table/entities/lucius_zogratis.json`) would be the next step.
- **Lucius has no Dark Disciples** (the request's shared "each boss can summon Dark Disciples" is met by the Triad; Lucius has Paladins and clones). The Astaroth-tinted Disciple skin and ability exist in the entity, but no boss summons it.
- **The Soul Paladin model is not used.** `zogratis_soul_paladin` (geo, animation, textures) is generated and previewed, but a Paladin is the original mob with an effect, gold dust and a kneel; no client code redraws it in white and gold. The effect, its icon and the server-side particles are what the player sees.
- **No sound events of their own:** the fights use vanilla sounds.
- **No Bedrock JSON files** were produced (section 0): there is no `.mcpack` / `.mcaddon` output, by design.
- **Boss speech, move names and warnings are literals in the code**, so they cannot be translated through lang files.
- The Time Magic VFX files were not touched (they are the quality standard); Lucius only calls the existing Time shapes.
