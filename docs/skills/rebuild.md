# Skill rebuild

Why: the custom Tensura / ManasCore skills (`src/main/java/com/newuniverse/nusmp/skill/`) were stubs with a vanilla placeholder icon,
hard-coded English text, area effects that hit allies and pets, and Ultimates that were the base skill with a flag. This rebuild gives
every one of them real, server-authoritative mechanics, its own icon, translatable text, unit-tested numbers and a clean-up story.
Every registry id, the Unique -> Ultimate table (`SkillEvolution`), the config keys and the stored skill data (the Faith points, the Infinity
switch, the All Fiction health memory, cooldown tags) are unchanged, so old worlds load.

## How it is built

| Piece | File | Job |
|---|---|---|
| Rules (no Minecraft types, unit-tested) | `skill/rules/*.java` | Every number and decision: tiers, costs, cooldowns, radii, caps, chant timing, who is affected. |
| Base skill | `NUSkill` | Icon `textures/skill/<id>.png`, lang-keyed name and description (with English fallback), `tier()` (Unique / mastered Unique / Ultimate), cooldown and mastery helpers. |
| Text | `SkillText`, `SkillMsg` | `SkillText.of(id, name, description)`, `SkillMsg.t / mode / chant` = `Component.translatableWithFallback` with the English text next to the key. |
| Targets | `SkillTargets` | Who a skill may hurt (a *foe*: hostile, or already hunting you, or a player you may harm; never allies, pets, anybody's summons, creative/spectator players, armour stands), who it may heal (players and your own creatures), cones, beams, caps of 24. |
| Damage | `SkillDamage` | Every hit goes through `BalanceLaw.damage` (EP scaling, PvP hit cap) and the pack-wide DanMachi multiplier, and credits the kill to the caster. |
| Effects | `SkillFx` | The only way skills draw: Rukh (light / dark) via `RukhFx`, the mod's `VfxShape`s via `VfxSpawn`, sounds. No vanilla particle anywhere; the Time Magic effects are not touched. |
| Zones | `SkillZones` | Short lingering effects (blizzard, flame pillars, gravity, Freya's charm) that pulse on the owner's own tick; max 4 per owner, ended when the owner dies, leaves or changes dimension. |
| Summons | `Summons` | Creation, ownership tags (`nusmp_summon`, `nusmp_owner`), lifetime, steering, expiry, over-cap trimming and clean-up. |
| Upkeep | `SkillEvents` | Called from the listeners the mod already registers (`SkillEvolution.onPlayerTick`, `LiarisFreeseSkill.onKill`): **no new wiring needed**. |
| Icons | `tools/skills/gen_skill_icons.py` | 38 distinct 32x32 icons (frame by type: Extra = steel, Unique = gold, Ultimate = black iron). |
| Lang | `tools/skills/gen_lang_fragment.py` -> `tools/skills/lang_fragment.json` | Scans the Java sources for every key and writes the flat fragment the lead merges into `en_us.json`. `--check` fails if stale. |

Tiers: **BASE** (fresh Unique), **MASTERED** (the Unique once mastered), **ULTIMATE** (an Ultimate evolution; its own mastery adds x1.2 on top).
Pack-wide multipliers `[danmachi] damageMultiplier / costMultiplier / cooldownMultiplier` still apply to the god, chant, and god-tier skills.
Cooldowns below are ticks (20 = 1 s) before those multipliers.

## Uniques and their Ultimates

### Summoner (`summoner`) -> Lord of Summons (`lord_of_summons`)
*Rules: `SummonRules`. Config: `maxSummons` 3, `maxSummonsMastered` 6, `magiculeCost` 500, `cooldown` 100.*

| | Summoner | Lord of Summons |
|---|---|---|
| Modes | Beast Card, Stone Card, Recall | the same three + **Legion**, **Wraith Card** (appended) |
| Cap | 3 (6 mastered) | 12 (twice the mastered cap), hard ceiling 64 |
| Beast Card | 1 wolf, 30 HP / 5 dmg (mastered 60 / 10) | **3 wolves**, 120 HP / 20 dmg each |
| Stone Card | iron golem 100 HP / 15 dmg (mastered 200 / 20) | colossus 400 HP / 40 dmg, scale x1.35 |
| Legion / Wraith | - | 5 wolves (70 HP / 12) for 3x cost and 4x cooldown / 4 flying vexes (24 HP / 8) for 2x cost, 2 minutes |
| Lifetime | 10 min (20 mastered) | 40 min |

New: every summon carries owner, card and expiry in its own data, follows its owner (called back beyond 12 blocks, teleported beyond 28), picks
fights with monsters (never players), is discarded when its time is up, its owner dies or is offline, or the owner is over the cap (oldest first).
Nobody's area effect hits a summon. A partial cast (cap nearly full) costs a partial price. Recall only removes the caster's own.

### Faith (`faith`) -> Goddess of Faith (`goddess_of_faith`)
*Rules: `FaithRules`. Config: `bestowableSkills`, `bestowFaithCost` 10, `bestowCooldown` 12000, `prayMagiculeCost` 1000, `prayCooldown` 200.*
Modes: Bestow, Pray, Check Faith (+ **Blessing**, **Miracle** for the Goddess). Pray: +1 Faith (+2 mastered, **+3** Goddess).
Bestow: the player you look at (8 blocks, Goddess 16) learns one random configured skill they do not own (Faith is spent only once it worked);
Goddess: half the Faith, half the wait. Blessing (2 Faith, 16 blocks): allies get Strength II, Resistance, Regeneration, Absorption for 60 s.
Miracle (5 Faith): allies cleansed, healed (healing cap) and Resistance IV for 5 s. Faith is the tag `Faith`, clamped 0..1,000,000, carried over on evolution.

### Firebolt (`firebolt`, standalone)
*Rules: `FireboltRules`. Config: `fireboltDamage` 8, `fireboltMagiculeCost` 100, `fireboltCooldown` 20.*
Press: an instant bolt to the creature you look at (range 32, 48 mastered): full damage (x2 mastered), 50% to foes within 2 blocks (3 mastered), ignite
4 s (6 mastered), a visual-only lightning strike. No block damage. Mastery only for a shot that hit.

### Argonaut (`argonaut`) -> Hero's Argonaut (`heros_argonaut`)
*Rules: `ChargeRules`. Config: `argonautDamagePerCharge` 8, `argonautMagiculePerCharge` 200, `argonautCooldown` 200.*
HOLD: a bell every second, each paid in magicules, up to 5 (Hero: 10); release strikes the cone in front (6 blocks; Hero 8). Damage per charge x1.0 / x1.5 mastered /
x3.0 Hero (x1.2 if the Hero is mastered). Releasing early (or running out of magicules) still strikes. Hero adds mode **Hero's Beam**: the charge as a piercing beam
12-42 blocks (3 per charge), x1.25 damage, stops at the first wall.

### Liaris Freese (`liaris_freese`, standalone passive)
*Rules: `GrowthRules`. Config: `liarisChance` 0.5, `liarisMasteryPerKill` 1.*
Kills can raise the mastery of all your not-yet-mastered skills (grimoires excluded). New guards: your own pets/summons, yourself and armour stands do not count; at most one
payout per second; bosses pay x3. Press: shows how many times it has grown you.

### Airiel (`airiel`, standalone)
*Rules: `WindRules`. Config: `airielMagiculeCost` 300, `airielCooldown` 100.*
Tempest: 30 s (60 mastered) of Speed II (III), Jump II, Slow Falling, Strength (II mastered) **and 50% (65%) less arrow/projectile damage**. Wind Dash: burst forward, cost 1/3,
cooldown 40 ticks, **20 while the Tempest blows**.

### The gods: Hestia, Freya, Hephaestus, Loki (`hestia`, `freya`, `hephaestus`, `loki`)
*Rules: `GodRules`. Single press, cost and cooldown through the DanMachi multipliers; an unusable power (nothing to repair, no room to blink) costs nothing.*
- **Hestia** (1500, 1200): allies (players + your creatures) within 10 (16) heal 35% (60%) through the healing cap, Regeneration, Absorption; mastered also cleanses and gives Fire Resistance 60 s.
- **Freya** (1200, 900): foes within 9 (14) are slowed and weakened 6 s (10 s; the hard part through `BalanceLaw.controlTicks`), players reel; monsters drop aggro and **cannot re-target you for 10 s (20 s)** (re-applied every 0.5 s).
- **Hephaestus** (2000, 2400): repairs the held item fully; mastered also off-hand and all armour.
- **Loki** (800, 400): safe blink 10 (16) blocks (uses the relics' landing search), invisible 5 s (10 s), nearby hunters forget you.

### The chants: Wynn Fimbulvetr, Rea Laevateinn, Arcs Ray, Futsunomitama, Uchide no Kozuchi
*Rules: `ChantRules`. HOLD the key: one line of the chant finishes every 14 ticks (6 when mastered); the line shows on the action bar; release after the last line casts, releasing early breaks the chant and costs nothing (magicules are paid at the cast).*

| Spell | Lines | Cost / cooldown | Effect (base / mastered) |
|---|---|---|---|
| Wynn Fimbulvetr | 3 | 3000 / 600 | blizzard at the look point, radius 7 / 10: 20 / 30 damage, freeze + slow, then lingers 3 s / 5 s dealing 3 / 5 per second |
| Rea Laevateinn | 3 | 3500 / 700 | 3 / 5 waves of 6 / 9 flame pillars in a 3..10 / 14 block ring around you; 8 / 12 damage + burn, a foe once per wave |
| Arcs Ray | 1 | 800 / 100 | piercing beam 48 / 64 blocks, 14 / 22 damage, x1.5 on the undead, stops at walls |
| Futsunomitama | 2 | 2000 / 500 | gravity field radius 6 / 8: 10 / 16 crush + pin, then 3 s / 6 s of pulling in and 2 / 3 damage per half second |
| Uchide no Kozuchi | 3 | 4000 / 6000 | Level Boost on the ally you look at (player or your creature, else you): Strength/Resistance/Speed/Haste II + Absorption + heal, 60 s / 120 s |

### Hell Walker (`hell_walker`) -> Hell Conqueror (`hell_conqueror`)
*Rules: `AuraRules`. Toggle, 60 magicules per second, switches itself off when empty.*
Melee damage x (1 + missing health share x steepness): 1.1 / 1.6 mastered / **2.5** Conqueror (about x2 at 10% health for the Unique, x3.25 for the Conqueror).
Conqueror adds: below half health every blow heals 20% of its damage; a **hellfire aura** burns foes within 4.5 blocks each second; below a third of health speed + strength.

### Hawk Eye (`hawk_eye`, toggle) / Spirit Card (`spirit_card`, toggle)
Hawk Eye (30/s): night vision, hostile creatures within 32 (48) blocks glow, nearest 48 only. Spirit Card (80 per 2 s): players and your creatures within 8 (12) heal 2 (4) through the healing cap; mastered cleanses.

### Unbreakable Will (`unbreakable_will`) -> Immortal Will (`immortal_will`)
*Rules: `WillRules`.* Refuses a killing blow (not the void or /kill): rise with 30% (50% mastered) / **100%** health, cleansed, Resistance 5 s / **10 s**, cooldown 10 min (5) / **3 min**.
Immortal adds a **shockwave** (8 blocks, 18 damage, knockback) on every refused death and mode **Rally** (1200 magicules, 90 s: heal 40% and cleanse). Press to see readiness.

### Hati (`hati`, passive)
Fire / lightning / magic damage: 60% (40% mastered) lands; each absorbed hit stacks Strength (up to II, IV mastered) for 10 s (15 s); mastered also heals 25% of what it absorbed.

### Devil's Footprints (`devils_footprints`) -> Adolla Burst (`adolla_burst`)
*Rules: `FireForceRules`.* Ignition Dash (150, 30 ticks, power 1.7 / 2.4), Devil's Kick (300, 100 ticks, 5 blocks, 14 / 21 damage, burn 6 s), all fire damage cancelled.
Adolla Burst: dash becomes **Light Speed** (a blink up to 32 blocks, 22 damage and ignite to everything on the line), kick 8 blocks / 30 damage, and mode **Adolla Burst**
(1600, 200 ticks: 9-block flame explosion, 36 damage, burn 10 s, knockback).

### Book Maker (`book_maker`) -> All Fiction (`all_fiction`)
*Rules: `FictionRules`.* Book Maker: **Pin** (400, 80 ticks, 24 blocks): 10 / 16 damage, crawl (capped by the control rules), Weakness, Mining Fatigue, dragged down; **Barrage** (mastered; 1200, 240):
the pin on every foe in a 10-block cone at 60% damage. All Fiction (a different skill): **Undo** (1500, 400: health from 5 s ago, harmful effects cleansed), **Fiction** (4000, 1200, 24 blocks: erase a mob; a boss or protected player loses effects and 25% health;
a player is killed, never silently removed), passive **death becomes fiction** once per 15 min (never against the void or /kill).

### Six Eyes (`six_eyes`) -> Limitless (`limitless`)
*Rules: `AuraRules`, `GojoRules`.* Six Eyes (toggle, 40/s, 20 mastered): night vision, living things within 40 (64) blocks glow (nearest 64), the creature you look at is read out (health).
Limitless: **Infinity** (120/s, 60 mastered; stops every attack and turns projectiles back), **Blue** (1500: pull + 10 / 16 damage), **Red** (1800: push + 16 / 24), **Hollow Purple** (6000, 64 blocks: kills what it may, 50 / 80 damage to bosses and protected players). Foes only.

### Gold Experience (`gold_experience`) -> Gold Experience Requiem (`gold_experience_requiem`)
*Rules: `JoJoRules`.* GE: **Give Life** (800, 300: heal 10 / 16 through the healing cap + Regeneration, mastered cleanses), **Sensory Overload** (600, 160: 6 / 10 damage, nausea, blindness, slowness), melee hits slow.
Requiem: **Return to Zero** (5000, 2400: 6 s / 10 s in which every attack returns to zero; outside it 20% / 35% of attacks do, the attacker is left powerless), **Life Burst** (3000, 900: allies within 14 / 20 healed 50% through the cap, cleansed, Regeneration, Absorption IV).

### Vector Manipulation (`vector_manipulation`) -> Black Wings (`black_wings`)
*Rules: `VectorRules`.* Toggle 50/s (Black Wings 80/s): projectiles are reversed, 50% / 75% (mastered) / **100%** of a melee hit goes back (through the PvP cap; never reflected twice).
Black Wings adds no fall damage at all, projectiles return 1.6x faster **as your own**, and a **glide** (sneak in mid-air: slow sink, drift forward, dark Rukh wings).

## Standalone god-tier Ultimates (admin / event rewards)
*Rules: `GodTierRules`. Config `[god_tier]`: `affectsPlayers`, `bossHealthThreshold`, `instantDeathBossShare`, `almightyDodgeChance`.*
- **Instant Death**: Die (3000, 400 / 200: what you look at dies, boss / protected player loses the configured share) and Mass Death (8000, 3600: hostile creatures within 16 / 24 blocks). Only things you may hurt.
- **The Almighty**: dodge chance (config 0.2, x1.5 mastered, never above 95%); Rewrite the Future (5000, 3000): nothing reaches you for 5 s / 8 s and your harmful effects vanish.
- **The Visionary**: Meteor (4000, 600: 35 / 50 damage within 6 / 8 blocks, **no explosion**, no block or fire damage), Void (3500, 700: foes lifted, withered, darkened, blinded), Perfect Body (3000, 3600: Strength/Resistance/Speed III, Regeneration II, 60 s / 120 s).

## What changed everywhere
- Area effects hit foes only; heals and buffs reach players and the caster's own creatures only.
- Hard control (slows, freezes, pins) goes through `BalanceLaw.controlTicks`; damage through `BalanceLaw.damage`; heals through `BalanceLaw.heal`.
- All text is translatable (`nusmp.skill.*`, `.msg.*`, `.mode.*`, `.chant.*`); mode keys that existed keep their names.
- The chants are now actually chanted (hold, lines, release). The old text said "Instant cast"; the new descriptions replace it.
- The Time Magic effects, `TimePages`, `Grimoire*` skills and all grimoire code are untouched.

## Tests
`src/test/java/com/newuniverse/nusmp/skill/rules/*` (every rules class) and `skill/SkillAssetsTest` (ids still registered, evolution table unchanged,
38 distinct 32x32 icons, lang fragment complete, no vanilla particle or placeholder icon in a rebuilt skill).
