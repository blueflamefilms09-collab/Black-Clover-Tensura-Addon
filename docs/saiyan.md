# Saiyan race — Stage 1: Foundation (3.63.0)

Source of the design: the PDF "Saiyan Race System – The Warrior's Ascension" and the text copy you sent. This release is **Stage 1 only** of the eight-stage roadmap
(1 Foundation, 2 Combat, 3 Transformation framework, 4 Basic transformations, 5 Ultimate awakening and Goku, 6 Advanced forms, 7 Ultra Instinct, 8 Polish).
Nothing in Stages 2–8 is in this release.

## What a Saiyan is in this build
A Saiyan is a **flag on the player** (`/saiyan race <player> on`), not an entry of the `Race` enum, so the questionnaire, the race pool and every existing `switch` over races stay untouched.
Taking the race away (`off`) removes every modifier and keeps the Battle Power for a later return.

**Ki is the Tensura magicule (your decision: "Ki replaces the magicule, both the bar and being able to use it").** There is no second Ki pool.
Everything that spends magicule — Tensura's skills, this mod's pages and skills — already spends Ki; Tensura saves and regenerates it. The race only
- adds capacity: a flat `kiBase` (100, the spec's "Ki capacity 100") plus `kiCapacity` (+30%, growing with Battle Power) as transient modifiers on Tensura's `MAX_MAGICULE`;
- adds a little regeneration: `kiRegenPercentPerSecond` (2% of the maximum per second) once the Ki has awakened;
- hides Tensura's magicule bar on the client while the local player is a Saiyan; the Saiyan HUD's **Ki** bar shows the same numbers (the layer is matched by name, see Caveats).

## Racial table (all server config, `serverconfig/nusmp-saiyan-server.toml`)
| Bonus | Spec | Default here | How it is applied |
|---|---|---|---|
| Strength | +35% | `strength` 0.35 | melee damage ×(1+bonus) in `LivingIncomingDamageEvent` (own calculation, not an attribute); Ki attacks are magic damage and are not multiplied |
| Durability | +30% | `durability` 0.30 | damage taken ÷(1+bonus) (void and `/kill` are left alone) |
| Speed | +20% | `speed` 0.20 | transient attribute modifier, capped |
| Attack speed | +15% | `attackSpeed` 0.15 | transient attribute modifier, capped |
| Jump | +25% | `jump` 0.25 | transient attribute modifier, capped |
| Knockback resistance | +15% | `knockbackResistance` 0.15 | transient attribute modifier, capped |
| Stamina capacity | +40% | `staminaCapacity` 0.40 | on `staminaBase` (100) |
| Ki capacity | 100 | `kiBase` 100 and `kiCapacity` 0.30 | see above |
| Ki regeneration | 2/s | `kiRegenPercentPerSecond` 2.0 | percent of the maximum, see above |
| Combat experience | +20% | `combatExperience` 0.20 | more Battle Power per qualifying kill |

Every bonus grows with Battle Power: `bonus = base × (1 + bonusGrowth × growth)`, cut at its own cap (`[race.caps]`). `growth` is 0 at Battle Power 100 and 1 at 10,000,000 (a log scale over the six ranks).
This is the one configurable conversion from Battle Power to real combat strength; Battle Power itself never multiplies damage directly.

## Battle Power and ranks
Low-Class 100–999 · Elite 1,000–9,999 · Super Elite 10,000–99,999 · Legendary 100,000–999,999 · Divine 1,000,000–9,999,999 · Transcendent 10,000,000+. Start: 100. Hard cap 10¹² (config `max` ≤ that).

Gain comes from kills only, under the anti-farm rules of the spec:
- **Weak creatures give nothing**: a victim below `minRatio` (25%) of your own Battle Power is worth 0; the weight rises smoothly to 1 at an equal fight and to at most 1.5 against a stronger one.
  A victim's power is `victimPowerScale × (max health + 4 × attack + 2 × armour) + epWeight × its Tensura Existence Points` (Tensura EP is used when `useTensuraEp` is on).
- **Repeats are worth less**: the same creature type inside `repeatWindowTicks` is worth `1 / (1 + kills / repeatLimit)`.
- **Daily cap**: at most the larger of `dailyCapFloor` (20) and `dailyCapFraction` (10%) of your Battle Power per game day.
- **Young creatures give nothing** (`minVictimAgeTicks`, 30 s): spawn-and-kill farms pay nothing.
- **No friends**: allies, team mates, tamed pets and armour stands never count; players give nothing unless `playersGiveBattlePower` is on (off by default).
- Each kill is `gainPerKill` (1%) of your current Battle Power × weight × repeat × (1 + combat experience bonus), cut at what is left of the day.

## Ki Awakening
Sneak still on the ground, unhurt, for `awakenSeconds` (10) in a row. Any movement, jump or hit starts the count over. Until then Ki attacks are refused with a message. An operator can skip it with `/saiyan awaken <player>`.

## Ki attacks (Stage 1 has two)
Keys (rebindable, category "Multiverse of Anime"): **`,` Ki Blast**, **`-` Rapid Ki Barrage**. The client only asks; the server decides.
- **Ki Blast**: one raycast (blocks stop it) and one entity query along the line; the first foe on it takes the hit. Cost 2% Ki + 1 stamina, cooldown 10 ticks.
- **Rapid Ki Barrage**: up to 8 (default 5) small blasts, each at 35% of a Blast, the damage of each target added up and dealt as ONE hit (so the hurt cooldown does not eat the volley). Cost 6% Ki + 10 stamina, cooldown 60 ticks.
- Damage = base × (1 + `kiDamageGrowth` × growth), then the mod's **balance law** (the caster's Existence Points, the PvP hit cap, the mob multiplier), dealt as magic damage.
- Foes only: the same foe rule as the Beast Wills (not allies, not team mates, players only if `hitPlayers` is on, the server allows PvP and the place can be interacted with).
- Nothing is spawned and no block is touched, so there is nothing to protect and nothing to clean up.

## Commands
`/saiyan status` (anyone) · operators: `/saiyan status <player>`, `/saiyan race <player> on|off`, `/saiyan awaken <player>`, `/saiyan bp <player> set|add <value>`.

## Cost (rule 11), worst case
- Everyone who is not a Saiyan: one `hasData` lookup per player per tick, nothing else.
- Per Saiyan: one small upkeep per second (stamina, modifiers, extra Ki, awakening), at most **4 sync packets a second** (only when something changed, ~45 bytes each), one ask accepted per 4 ticks.
- A Ki Blast: 1 raycast + 1 entity query (≤ 64 candidates) + at most 16 trail particles + 5 spark particles, all through `PerfParticles`. A Barrage: up to 8 of those. **Zero entities spawned.**
- Memory per Saiyan: one `Run` record (UUID-keyed, dropped at logout, death and when the race is removed) and a state of ≤ 24 repeat-kill entries and ≤ 16 cooldowns.
- Client: one panel of about 30 fills and 6 strings per frame only while the local player is a Saiyan; the text is rebuilt twice a second; the Tensura-layer filter is one namespace compare per HUD layer per frame.

## Server friendliness (rule 9)
All rules are decided on the server; the client draws and asks. Per-player state is keyed by UUID; the save is a NeoForge attachment copied on death and never written for a player who is not a Saiyan. Every setting is a SERVER config. The packet is validated (one byte, range-checked, sender must be a real server player, rate limited). `SaiyanSourceScanTest` pins these; `ServerSafetyTest` guards the client-class rule.

## NEW / REPLACED / KEPT
- **NEW:** everything above (race flag, Ki as the magicule with capacity and regeneration, stamina, the racial table, Battle Power with six ranks and the anti-farm rules, Ki Awakening, Ki Blast, Rapid Ki Barrage, two keys, the HUD panel and `saiyan_hud` in GUI Move / GUI Size, server config, `/saiyan`, self-tests `saiyan.rules.bounds` and `saiyan.player.roundtrip`, `docs/saiyan.md`).
- **NEW (separate request):** the legendary-weapon consumables fix, see below.
- **REPLACED:** nothing. (The Ki-replaces-magicule bar is a client-side hide for Saiyans only; Tensura's bar is untouched for everyone else.)
- **KEPT:** the existing `KiSkill` and the `db_goku` content (see the open decision), every Tensura race and skill, the questionnaire, the `Race` enum, all saves.

## Legendary weapons and consumables (separate fix in the same release)
Cause: a legendary (hero) weapon in the main hand handles right-click in `use()` and returned a result that swallowed the click, so food and potions in the off hand were never used.
Fix: `ConsumablePass` — when the main hand holds a hero weapon and the off hand holds food you can eat or a drink, and you are not sneaking, the weapon passes the click and the off-hand item is used.
Sneak + right-click still fires the weapon's ability. Setting: `consumablesFirst` in `serverconfig/nusmp-heroweapon-server.toml` (default on; off restores the old behaviour). Pure tests: `ConsumablePassTest`.
Not verified in a game; other weapon families (non-hero) were not changed.

## How to test in Minecraft
1. `/saiyan race @s on` → the HUD panel appears at the bottom right (movable in GUI Move). Tensura's magicule bar is replaced by the **Ki** bar.
2. `/saiyan status` shows rank, Battle Power, the bonuses and today's gain.
3. Press `,` before awakening → "Your Ki has not awakened yet…". Sneak still for 10 s on the ground → "Your Ki awakens…". (`/saiyan awaken @s` skips it.)
4. Press `,` at a mob → a beam, damage, Ki and stamina drop, cooldown shows on the HUD. Press `-` → a volley.
5. Kill a zombie-class mob for Battle Power; kill a chicken (0 gain); kill ten zombies (falling gain); watch `gained today` in `/saiyan status` stop at the cap. `/saiyan bp @s set 100000` to jump to Legendary and see the bonuses grow.
6. `/saiyan race @s off` → every modifier and the HUD are gone, Tensura's bar returns.
7. Legendary weapons: hold a hero weapon in the main hand and food in the off hand; right-click eats. Sneak + right-click uses the weapon.

## Completed / incomplete
Completed (Stage 1): race flag, racial table, attribute and damage bonuses, Ki = magicule (capacity, regeneration, HUD, bar replacement), stamina, Battle Power with six ranks and the anti-farm rules, Ki Awakening, Ki Blast, Rapid Ki Barrage, keys, HUD, config, commands, persistence, tests.
Incomplete / not in this release: flight, sensing, physical techniques, barrier, dodge, Zenkai, the transformation framework and every form, the Ultimate Skill registry and Goku, models, extra VFX and sounds, a race-selection screen for Saiyans, and Warrior's Reflex / Battle Adaptation / Saiyan Senses as separate passives.
Frieza and other non-Saiyan lines stay **separate racial evolutions**, not Saiyan Ultimates; the Ultimate registry (Stage 5) will carry a race restriction.

## Caveats (honest)
- Nothing was seen in a game and Gradle was not run locally; CI is the compile and the smoke test.
- Tensura's magicule HUD layer id is unknown: the client hides layers in the `tensura` namespace whose path contains "magicule" and writes every Tensura layer name to the log once (`[nusmp] Saiyan: Tensura HUD layer …`). If Tensura draws its bar another way, both bars show until the id is added.
- The spec's "Ki 100 / 2 per second" is read as a flat +100 capacity and +2% of the maximum per second, because Ki is Tensura's magicule pool and its size varies enormously.
- Balance numbers are starting values (the spec's table, a log-scale growth, a 1%-per-kill gain, a 10% daily cap).
- The Ki attacks are raycasts, not projectile entities, so they hit instantly and cannot be dodged by moving; this is the price of costing nothing to spawn or clean up.

## Open decision
The mod already has `KiSkill` and a `db_goku` content set. They are kept untouched. Say whether the Saiyan Stage 5 Goku path should replace them, wrap them, or live next to them.
