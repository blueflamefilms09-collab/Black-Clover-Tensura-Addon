# Saiyan race — Stage 1 Foundation (3.63.0) and Stage 2 Combat, Goku and the form roster (3.65.0)

Source of the design: the PDF "Saiyan Race System – The Warrior's Ascension" and the text copy you sent, and (3.65.0) your chart "All Saiyan Forms of Goku".
3.63.0 was **Stage 1** of the eight-stage roadmap (1 Foundation, 2 Combat, 3 Transformation framework, 4 Basic transformations, 5 Ultimate awakening and Goku, 6 Advanced forms, 7 Ultra Instinct, 8 Polish).
3.65.0 builds Stage 2 (combat) and a data-driven version of Stages 3–7 (the form engine, Goku, the 16-form roster including Ultra Instinct) on top of it; the part that is Stage 1 is described first and unchanged. See **Stage 2 and the Goku path (3.65.0)** below. Stage 8 (polish: models, hair, sounds, animations) is not in this release.

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

## Decision on `KiSkill` / `db_goku` (answered in 3.65.0)
The old open question "replace, wrap or live next to?" is decided: **live next to**. `KiSkill` and the `db_goku` content stay registered and untouched (rule 2). The Saiyan Goku path is a separate Ultimate Skill, **Son Goku, King of Martial Arts**, in `SaiyanUltimates`, restricted to Saiyans, and every higher form that needs it checks that Ultimate, never `db_goku`.

# Stage 2 and the Goku path (3.65.0)

Everything below is server-decided; the client sends one byte (an action number) and draws what the server sends. The action numbers are range-checked, rate-limited (one per 4 ticks; the Kamehameha release is never rate-limited so a charge can always be let go) and the sender must be a real server player who is a Saiyan.

## Keys (all rebindable, category "Saiyan")
`,` Ki Blast · `-` Rapid Ki Barrage · `G` Ki Flight on/off · Left Alt Master burst flight · Insert Saiyan Senses (radar) · Home Ki Suppression · End Rock-Scissors-Paper (press to start, press again for each part) · Delete **hold** Kamehameha (release to fire) · `` ` `` form on/off · Page Up / Page Down next / previous form. The defaults avoid every key this mod already uses; rebind in Options → Controls.

## Ki Flight (five tiers)
Novice (hover only, at most `hoverSeconds` = 10 s), Apprentice, Adept, Expert, Master. A tier is earned by seconds spent really moving in flight (120 / 600 / 1800 / 5400 s, all in `[saiyan.flight]`). Each tier is faster and cheaper per second than the last; Master adds a **burst** (Left Alt: a dash of `burstSpeed` blocks per tick, with a Ki and stamina price and a cooldown).
How it works without cheating the server: the Saiyan is given the creative flight ability for as long as the flight lasts and it is taken back afterwards (only what we gave, marked `nusmp_saiyan_flight`; creative and spectator players are never touched). Each second the server compares the distance moved with the tier's allowed speed (`SaiyanFlightRules.speedOk`); a violation ends the flight (no teleport back). Touching the ground for three seconds ends it; Ki is paid every second; running out of Ki ends it; fall damage is cancelled while flying and for five seconds after it ends, so running dry in the air is not a death sentence. Flight ends on logout, death, dimension change and when the race is removed.

## Saiyan Senses (16 / 32 / 64 / 128 blocks) and Ki Suppression
Insert pays a little Ki and turns on a radar for `durationSeconds` (10 s). Tier = number of sensings (Beginner 16 blocks, Intermediate after 30, Advanced after 150, Master after 500 = 128 blocks). The radar is a 64×64 panel inside the existing Saiyan HUD (forward is up), at most 32 dots, coloured by kind (creature, player, boss) and sized by power signature. Home toggles **Ki Suppression**: a small Ki cost per second, and a suppressed Saiyan does not show on another Saiyan's radar. It does **not** change what mobs notice.
Cost: one entity query per sensing player every 2 s, at most 4 radar queries server-wide per tick, ≤ 64 candidates examined, ≤ 32 entries (~7 bytes each) in one packet every 2 s.

## Warrior's Reflex, Battle Adaptation, Zenkai
- **Warrior's Reflex:** an automatic dodge of a blow with an attacker. Chance 3% → 15% with rank (`reflexBase`, `reflexMax`), a cooldown (2 s), a stamina price, at most 3 dodges per 20 s window, and a hard cap of 75% from any source (including the Ultra Instinct dodges). A dodge cancels the damage event and plays a spark.
- **Battle Adaptation:** the more times one kind of damage (melee, projectile, fire, explosion, magic) hits you inside one fight, the less it hurts, up to 15% after 10 hits; the memory is gone after 600 ticks without damage.
- **Zenkai:** a blow that leaves you under 20% health, from a foe at least half your Battle Power, marks a near-death; if you then recover to 60% health unhurt for five seconds you gain a permanent Battle Power share (5%, divided by 1 + 0.25 × Zenkai so far), at most one per game day and 24000 ticks apart. `/saiyan zenkai <player>` (operators) gives one.
- **Training:** hits on worthy foes (not on weak ones) fill four daily-capped meters — physical, Ki, martial, divine — that the form unlock rules read. Weak creatures give nothing.

## Son Goku, King of Martial Arts (Ultimate Skill)
The registry `SaiyanUltimates` is a table: an Ultimate has an id, a race restriction, awakening needs and a trial; the Goku entry is `Race.SAIYAN`-only, so Frieza and the other lines stay **separate racial evolutions**, not Saiyan Ultimates; a later Ultimate is a new row.
**Awakening needs** (all in `[saiyan.goku]`): Battle Power 100,000; 5 universal techniques mastered (Blast, Barrage, Flight, Senses, Suppression); Adept flight; Intermediate senses; 1 challenging foe beaten with martial arts; 1 Zenkai; the **Trial of the Martial King** (10 martial victories over foes at least 75% of your power). It is checked every 5 s; when it is met you get a title, a message and a short effect. An operator can give or remove it: `/saiyan ultimate <player> goku|none`.
**Kamehameha** (hold Delete): charging takes `kamehamehaChargeSeconds` (6 s; mastery of the technique cuts it by up to 40%) and pays Ki every second; a release before 10 ticks fizzles. The release fires one beam: a quick shot does 35% of the damage, 40% of the range, a narrow width and passes through one foe; a full charge does 100% damage, the whole range, 2 blocks of width and passes through up to eight foes, with more knockback (none on bosses) and a Ki price from 40% to 100% of `kamehamehaCostPercent`. One raycast (blocks stop it, no block is touched) + one entity query (≤ 64 candidates) + ≤ 57 budgeted particles per release; damage goes through the balance law and the active form's factor.
**Rock, Scissors 'N' Paper** (End): a three-part timed combo. **Rock** is a direct punch with a short lunge (one foe, reach 4.5), **Scissors** a fast precision strike (one foe, reach 5, 70% damage), **Paper** an open-palm shockwave that throws everything in front of it (up to 8 foes, 80% damage, 130% of that when all three were chained). Each part is one press of the key, inside 30 ticks of the last part and not sooner than 6 ticks after it (the inputs are timed; an early press is ignored); a late press starts a new Rock. Each part costs Ki and stamina; one entity query per part; the blows go through the balance law as the Saiyan's own attack (so they count as martial hits for the kill rules). A new combo cannot start for 40 ticks after Paper.

## The form roster (data-driven: `SaiyanForms.ALL`)
16 forms plus Base, from your chart, **without the two ape forms** (as you said): Kaio-ken, False Super Saiyan, Super Saiyan, Super Saiyan Full Power, Super Saiyan 2, Super Saiyan 3, Super Saiyan 4, Super Full Power Super Saiyan 4, Super Saiyan 4 Limit Breaker, Super Saiyan God, Super Saiyan Blue, Perfected Super Saiyan Blue, Super Saiyan Blue Kaio-Ken, Ultra Instinct Sign, Perfected Ultra Instinct, True Ultra Instinct. Because Super Saiyan 4 normally needs the Golden Ape, it needs **70% mastery of Super Saiyan 2** instead.
A form is a row: id, branch, whether it needs the Goku Ultimate, parent forms and the mastery share they need, Battle Power, Zenkai count, a training need, an operator flag, Ki to enter, Ki and stamina per second, damage / defence / speed / attack-speed bonuses, the cooldown before another form, the mastery time and an aura colour. The same table feeds the engine, the commands, the config (every number of every form is a server setting in `[saiyan.forms.<id>]`) and the tests. Adding a form is adding a row.
- **Earning:** every 5 s the engine checks each form's rules; a form that meets all of them is unlocked for good. `/saiyan forms` shows what you have and what is missing; `/saiyan form <id>` selects one.
- **Using:** the form key enters the selected form (the first unlocked if none is selected) or leaves the active one; Page Up / Down cycle (and switch at once while transformed). Entering costs Ki and needs stamina; the form's Ki and stamina drain are paid every second (less as mastery grows: a mastered form drains 60% of a new one); running out ends it with its own message.
- **Mastery:** only real fighting seconds count (within 10 s of dealing or taking a hit), capped per game day (`masteryCapSecondsPerDay` = 1800) — no idle farming. A new form works at 85% strength, a mastered one at 100%.
- **Kaio-ken:** its multiplier steps up with mastery (x2, x3, x4) and builds a **strain** meter; a full meter ends the form and the strain takes a share of the maximum stamina until it fades.
- **Ultra Instinct:** the Sign adds a real, capped dodge with a flow meter; Perfected and True UI dodge better and open a counter window (the next blow is stronger). All still capped at 75% and rate-limited.
- **Limit Breaker:** needs the operator flag `limit_breaker` (`/saiyan flag <player> limit_breaker`) because it has no natural unlock yet.
- The effects are speed and attack-speed attribute modifiers (transient, removed on leaving the form, death, logout and race removal), the damage and defence factors in the damage event, and an aura of tinted motes. No model, hair or animation changes (Stage 8).

## Commands added
`/saiyan forms`, `/saiyan form <id>` (anyone); operators: `/saiyan unlock <player> <form>`, `/saiyan flag <player> <flag>`, `/saiyan ultimate <player> goku|none`, `/saiyan zenkai <player>`. `/saiyan status` now also shows flight and sense tier, the Ultimate, Zenkai count, training and which Goku requirements are still missing.

## Cost (rule 11), worst case for 3.65.0
- Per Saiyan, per second: flight / senses / forms / Zenkai upkeep (a few comparisons), one form-unlock check every 5 s (16 rule checks, no allocation but a short list), up to 2 aura sprays a second through `Fx` (budgeted).
- Per damage event of a Saiyan: a handful of comparisons (dodge roll, adaptation lookup, form factors); no entity query.
- Radar: one `getEntitiesOfClass` per sensing player every 2 s, ≤ 4 per tick for the whole server, ≤ 64 examined, ≤ 32 reported.
- Kamehameha: 1 raycast + 1 entity query per release. Combo: 1 query per part. Blast / Barrage unchanged. **No entity spawned, no block touched, no chunk loaded** (pinned by `SaiyanSourceScanTest`).
- Network: the state packet grew to 24 fields but is still sent only when something changed, at most about 4 a second per Saiyan; radar pings ≤ 32 × 7 bytes every 2 s to the one player who sensed; nothing is sent to other players.
- Memory per Saiyan: the `Run` record (UUID-keyed, dropped at logout/death/race removal) plus a saved state of ≤ 32 unlocked forms, ≤ 32 mastery values, four training meters.

## NEW / REPLACED / KEPT (3.65.0)
- **NEW:** Ki Flight (5 tiers, burst), Saiyan Senses (4 tiers, radar), Ki Suppression, Warrior's Reflex, Battle Adaptation, Zenkai, the four training meters, the Ultimate Skill registry with *Son Goku, King of Martial Arts* (awakening, trial, Kamehameha, Rock-Scissors-Paper), the form engine and the 16-form roster, `/saiyan` commands, 11 new keys, the radar / form / charge HUD, server config groups `flight`, `senses`, `passives`, `goku`, `forms`, self-tests `saiyan.stage2.*` and `saiyan.goku.*`, tests `SaiyanStage2RulesTest`, `SaiyanFormsTest`, `GokuRulesTest`.
- **REPLACED:** nothing. (Ki Blast / Barrage gained form bonuses and training; their numbers and keys are unchanged.)
- **KEPT:** `KiSkill`, `db_goku`, every Tensura race and skill, the questionnaire, the `Race` enum, all saves (the Saiyan save format is read both ways: version 1 saves load with the new fields empty).

## How to test the new parts in Minecraft
1. `/saiyan race @s on`, `/saiyan awaken @s`, `/saiyan bp @s set 150000`.
2. Press `G`: you lift off (Novice hover); `/saiyan status` shows the tier. Land and wait 3 s: flight ends. Fall from a height after flight ends: no damage for 5 s.
3. Press Insert: the radar panel shows nearby creatures for 10 s. Press Home: Ki Suppression; another Saiyan's radar no longer shows you.
4. Get hit by mobs: sometimes "dodge" sparks; after several hits of one kind the damage drops a little.
5. `/saiyan ultimate @s goku`, hold Delete for a few seconds near a mob and release: a beam. Press End three times in quick succession (about every 0.3–1 s): Rock, Scissors, Paper.
6. `/saiyan forms` lists what is missing; `/saiyan unlock @s super_saiyan`, press `` ` ``: you transform, the aura shows, Ki drains; press it again to leave. Fight to build mastery.
7. `/saiyan zenkai @s` gives a Zenkai at once.

## Completed / incomplete (3.65.0)
Completed: everything above.
Not in this release (by your instruction or by design): the Great Ape / Golden Great Ape; models, hair and animations; sound assets; Spirit Bomb, Instant Transmission, God Bind, Dragon Fist and Meteor Combination; the Giant Avatar; Ki Barrier, Deflection and Energy Transfer; the spec's other physical techniques and gravity training; block damage from the Kamehameha; Ki Suppression hiding from mob AI (it hides only from Saiyan senses). Spec extras left for later: Grade 2/3, Super Saiyan 2 Berserk, the Time Power forms, Blue Berserk, Kaio-ken x10 / x20.

## Caveats (honest, 3.65.0)
- Nothing was seen in a game and Gradle was not run locally; CI is the compile and the smoke test.
- The flight speeds and the server-side speed check are heuristics: a very laggy server could produce a false violation (the flight simply ends and can be started again). `speedScale` in `[saiyan.flight]` raises the allowed speed. The check reads the distance moved in a second, so a teleport or an ender pearl in mid-flight also ends the flight ("too fast"); start it again.
- Switching between two forms with the next / previous keys while transformed skips the cool-down between forms (the Ki price of the new form is still paid); entering a form from Base obeys it.
- The Goku awakening needs Battle Power 100,000 by default, which takes real play; every number is a server setting and `/saiyan ultimate` lets an operator grant it.
- Super Saiyan 4 Limit Breaker has no natural unlock yet (operator flag only).
- The radar is a HUD panel, not markers in the world.
- All form and technique numbers are starting balance from the spec's table and the chart; they need play-testing.

## Techniques II (3.66.0): Ki Barrier, Instant Transmission, Spirit Bomb, Meteor Combination
All four are decided on the server (`SaiyanTechniques`, numbers in the pure `SaiyanTechRules`, settings in `nusmp-saiyan-server.toml` group `[saiyan.techniques]`, one switch `techniques.enabled`). The client only asks (new ops 13 to 17 of the `saiyan_action` packet, range-checked, rate limited; a Spirit Bomb release is never dropped). Keys (all rebindable, category "Saiyan"): `\` Instant Transmission, `]` Ki Barrier, `[` Spirit Bomb (hold, release), `;` Meteor Combination.

| Technique | Who | What it does | Defaults |
|---|---|---|---|
| **Ki Barrier** | any awakened Saiyan | a shield that soaks a pool of damage for a few seconds; what the pool cannot soak gets through; it is read where a blow lands, after the race's own resistances | pool 60 (up to 3x with Battle Power growth), 8 s, 5% Ki, 20 s cool-down |
| **Instant Transmission** | Son Goku | teleports to the creature you look at (a player only if an ally), landing beside it on your side, else blinks to where you look; ends with no fall damage for 5 s; the flight's speed check is moved with you | range 64, blink 24, 6% Ki, 8 s cool-down |
| **Spirit Bomb** | Son Goku | hold to gather (Ki per second); every living friend within 24 blocks (animals, villagers, allied players; never foes or bosses) makes it gather faster (+12% each, at most 2.5x) and hit harder (+2% each); release to throw it where you look: it flies as a few particles and lands after its travel time, blasting every foe in the radius (balance law, foes only, no block damage) | 120 damage, radius 8, 8 s to charge, range 48, up to 15% Ki, 30 s cool-down |
| **Meteor Combination** | Son Goku | locks onto the foe you look at and rushes it with 8 timed strikes 3 ticks apart (a martial hit each, flashing next to it again if it moves away); the last strike is three times as strong | 6 damage a strike, 6% Ki + 20 stamina, 15 s cool-down |

**Cost (worst case):** no entity is spawned, no block touched, no chunk loaded (a teleport lands only in a loaded chunk). Spirit Bomb: one entity query a second while gathering (at most 24 friends counted), at most 3 particles every second tick in flight, one query for the blast (at most 64 creatures looked at, about 30 particles). Instant Transmission: one raycast, one query, at most 12 box checks. Meteor Combination: at most 16 strikes, one UUID lookup each. Ki Barrier: two comparisons per blow. At most about 4 small packets a second to the one player (the existing state packet, unchanged in size).
**Server friendliness:** all state is in the player's run (UUID-keyed, dropped at death, logout and change of dimension); a bomb whose thrower died or left the dimension is gone; a thrown bomb or a rush cannot be started twice.
**NEW:** the four techniques, 4 keys, 15 text lines, the `[saiyan.techniques]` settings, self-tests `saiyan.tech.rules` and `saiyan.tech.player`. **REPLACED:** nothing. **KEPT:** everything else, including the Kamehameha and Rock Scissors 'N' Paper (the HUD charge bar is shared, so it shows a Spirit Bomb gather too).
**Not in this release:** God Bind, Dragon Fist, the Giant Avatar, Deflection and Energy Transfer; sounds beyond vanilla ones; a HUD cool-down line for the new techniques (they say "not ready" through the action bar only). Not seen in a game; balance numbers are starting values. A Saiyan look layer (hair, eyes, aura per form) was started and parked in `tools/saiyan_look_parked/` at the owner's request to build techniques instead; it is not part of this build. The Dragon Ball wiki and Wikipedia (ja) could not be reached from the build session, so no wiki facts were used for these four; they follow the series' common knowledge.


## The Saiyan look (3.69.0, no hair)
A transformed Saiyan is drawn by the client from one number, the form code, that the server sends (`PlayerAuras.set(p, Aura.SAIYAN, 60, SaiyanForms.code(form))`, renewed once a second while the form lasts, shortened to 18 ticks when it ends so the armour shatters). Table: `saiyan/SaiyanLook.java` (one row per form: eye colour, aura colour, fur, tail, flames, bolts, aura height, charge-up size, armour); painter: `client/aura/SaiyanAura.java`; settings: `nusmp-saiyan-look-client.toml`.
- **What is drawn:** the armour (9 pieces, `tools/gen_saiyan_armor_geo.py`), fur and tail (SS4 forms), a body haze, flames, lightning, a ground ring, glowing eyes, and for the first 46 ticks the charge-up (cracks, rock chips, energy streaks, flash, column of light, two shockwave rings). Textures: `tools/gen_saiyan_look_textures.py`. Mockup: `python3 tools/saiyan_look_preview/preview_saiyan_look.py forms|charge out.png`.
- **Hair:** not drawn, not shipped (no strand texture). It is on hold at the owner's request; the hair table and maths are in `tools/saiyan_look_parked/`.
- **Cost (worst case):** server: one packet a second per transformed Saiyan, to players tracking them only. Client, per Saiyan in full detail at the busiest charge-up moment: under 190 quads (12 flames + 1 haze, 8 bolts, 4 eye, 2 ring, 72 fur + 12 tail for SS4, 10 rock chips, 12 streaks, 2 crack, 2 pillar, 4 shockwave, 2 flash) plus the armour set; beyond 32 blocks about a fifth of that, beyond 64 blocks nothing; no allocation per frame; measured by `PerfProbe` section `c.saiyan.aura`.
- **Server friendliness:** the server decides nothing about the look (it is a lease with a form code); every number in the packet is checked by the client against the table (an unknown code draws nothing); stand-ins without a connection send nothing; the settings are client-only.
- **NEW:** armour, aura, eyes, SS4 fur and tail, charge-up, 4 client settings, `Aura.SAIYAN` (appended last, so no networked ordinal moved), self-test `saiyan.look.rules`. **REPLACED:** nothing. **KEPT:** the server motes and every rule. **Not in this release:** hair, sounds, per-form armour variants, a first-person power-up view (the owner's own camera skips the column of light and the eyes).
