# Beast Will (3.45.0)

The essence of a powerful mana beast, inspired by *The Beginning After the End*. A player who holds one can **assimilate** it over hours of play, **train** it, and use it in two forms (**Acquire**, then **Integrate**). This is a new system; nothing existing was replaced (see "New / replaced / kept" at the end).

Status: implemented, unit tested and typechecked. **Nothing has been seen in a game.** Verified vs not verified is listed at the bottom, and so are the temporary assets.

## 1. What was built and how it fits the mod

| Piece | Where | Notes |
|---|---|---|
| Rules (pure) | `beastwill/BeastRank`, `BeastWillRules`, `BeastWillState`, `BeastWillDef`, `BeastWillSchema` | No Minecraft types, unit tested. Every number and hard bound lives here. |
| Data-driven wills | `data/nusmp/beast_will/*.json`, read by `BeastWillLoader` / `BeastWillCatalog` | Server-side reload listener. A bad file is skipped and logged; it never stops the load. |
| Progress + persistence | `BeastWillStore` (attachment `beast_will`), `BeastWillLedger` (SavedData `nusmp_beastwill_ledger`) | Per player (UUID). The ledger records used offers so one beast gives one will to one player once. |
| Acquisition | `BeastWillDrops`, `BeastWillCoreItem`, `BeastWillItems`, `BeastWillAcquire`, `BeastWillApi` | Rank is worked out from Existence Points, so no new rank system was invented. |
| Execution | `BeastWillManager` (upkeep, forms, strain), `BeastWillAbilities`, `BeastWillStealth`, `BeastWillAttributes` | One tick of upkeep per second per player, staggered by `tickCount % 20`. |
| Network | `BeastWillNet`: `Action` (client to server), `Sync`, `Info`, `Preview` (server to client) | All `optional()`; a vanilla or old client simply never sees the feature. |
| Presentation (client) | `client/beastwill/BeastWillClient` (keys + HUD), `BeastWillConfirmScreen`, `BeastWillCodexPage`, `client/aura/BeastWillAura` | HUD is movable and scalable through the existing GUI Move / GUI Scale framework. |
| Config | `nusmp-beastwill-server.toml` (SERVER) | Every value changes gameplay, so there is no client config. |

It reuses what the mod already has instead of adding a parallel system:

- **Energy**: the existing Tensura energy through `ShadowMana.cost/canPay/pay` (a percentage of the player's pool, so it scales with level, like the other skills). No second resource.
- **Race**: `MultiverseProfile.race(p)` for host compatibility.
- **Foes / teams / protection**: `SongTargets.isFoe`, `FtbTeamHelper.sameTeam`, the server's PvP and protection checks, `BossIdentity` for bosses.
- **Hard control**: `BalanceLaw.beginControl/controlTicks` (the shared 12 s hard-control cooldown and the caps every other hard control obeys).
- **Transformations**: `ModeArmor.active(p)` (the mod's mode armours) for the conflict rule.
- **Visuals**: `Aura.BEAST_WILL` through the existing player-aura pipeline, the perf budgets (the aura's own caps, `PerfProbe`) and, for the ability effects, the mod's particle facade `Fx` (theme `skill`, light/dark flavour; per-tick budgets and quality levels built in).
- **Controls**: five rebindable keys in the mod's key category; no new input system.

## 2. How to get a will

There are two ways in, both through the same checks.

### a) Beast Core (drop)

A **Beast Will Core** drops, rarely, from a beast that is:

- wild (not tamed, not owned by anyone), at least 1 minute old (`minVictimAgeTicks`, stops spawn-and-kill farms),
- killed by a player,
- rank **A or higher**. The rank comes from the beast's Existence Points (EP): C 1,000 / B 5,000 / A 20,000 / S 100,000 / SS 500,000 (all configurable). A creature Tensura gives no EP to is judged by its health (100 EP per point), so a dragon from another mod can still count,
- one the will's file names (by entity id or by name keyword).

One chance per kill, one core at most. The chance is the will's own (`dropChance`) times `dropChanceScale`. A player waits `coreCooldownDays` game days between two cores of the same will and **cannot get the core of a will they already hold**. The core lies on the ground with no despawn timer and glows, so a kill far from home does not lose it.

| Will | Beast | Rank needed | Chance per kill | Days between cores |
|---|---|---|---|---|
| Dragon Will | dragon-type (`iceandfire` fire/ice/lightning dragons, or a name match) | **S** | 0.4 % | 14 |
| Shadow Panther Will | panther / leopard / jaguar type | A | 2 % | 5 |
| Elderwood Guardian Will | treant / dryad / elderwood type | A | 2 % | 5 |
| Lion Will | lion / leonid type | A | 2 % | 5 |

The core is a **rare, distinct item** (`nusmp:beast_will_core`, glowing, with the will and the beast on its tooltip), not an ordinary Tensura core. It is in the creative tab (one of each will) and `/beastwill core` gives it.

**Using the core**: right-click it. Nothing is consumed yet. A confirm screen opens first and shows:

- the will, the beast it came from, its **rank** and **affinity**,
- how well your race fits (percent),
- the **absorption requirement**: the EP you have against the EP it asks for,
- either the reason it is refused (in words), or a warning that absorbing is permanent.

**Eligibility is checked before the core is consumed**, and again when you confirm. The core is taken only on a successful bind. Declining, closing the screen or running out the clock costs nothing. Refusals: feature disabled, unknown will, rank too low, will already held, 8 wills already held, race not allowed (only when `otherRacesMayHost = false`), not enough max EP, offer already used.

### b) Direct inheritance (reusable integration point)

A beast that *wants* to hand its will over (a quest, a relationship, a scripted encounter, another mod) does it with **no new code**:

- tag the creature `nusmp_will_offer:<will id>` (optionally `nusmp_will_rank:<C|B|A|S|SS>` and a custom name);
- a player **sneaks and right-clicks it with an empty hand** and gets exactly the same preview and checks as with a core.

The offer is remembered per creature and player (`offer:<creature>:<player>:<will>`) in the ledger, so one beast gives one will to one player once.

From code: `BeastWillApi.offer(player, willId, beastName, rank, key)` opens the preview; `BeastWillApi.grant(player, willId)` binds with no check (for a reward the caller has already earned).

Example: `/summon minecraft:wolf ~ ~ ~ {Tags:["nusmp_will_offer:lion_will","nusmp_will_rank:A"],CustomName:'"Old Lion"'}`.

### Rare wills need progression

The Dragon Will needs **50,000 max EP** to absorb, **S rank** to drop, assimilates over about 3 hours and masters over about 2 hours of play with a 15 %-per-day cap. The A-rank wills need 10,000 to 15,000 EP.

## 3. Hosts and compatibility

One will is **active** per player. A player may keep the progress of up to **8** wills (swapping, below).

- **Hosts**: humans, elves and dwarves are full hosts of every will (compatibility 100 %).
- **Other races** may host too (`otherRacesMayHost`, default true) at the will's own figure (50 % Dragon, 60 % the others), never below 20 %. Poor compatibility makes assimilation slower and strain higher (up to double), never impossible. Add races as full hosts with `extraHostRaces`; scale the poor fit with `otherRaceCompatScale`.
- The fit is shown in the confirm screen, in the HUD, and in the Codex.

## 4. Assimilation, mastery and training

Every will keeps, per player: **assimilation** (0-100 %), **mastery** (0-100 %), the **strain** of the body, and the unlocked phase.

| Source | Assimilation | Mastery |
|---|---|---|
| Holding a form (Acquire / Integrate) | main source, times compatibility | small |
| Meditation (sneak still, will off) | a little | a little |
| Using an ability | none | the main active source: 20 s worth of mastery per use (inside the day cap; the cooldown and strain limit how often) |
| Hitting a foe while in form | none | a little, at most once per 2 s |
| Idle in form (no movement/combat for 10 s) | only a quarter | only a quarter |

**Anti-farming**: each will has a daily cap (`assimilationDayCap`, `masteryDayCap`, a share of the whole per game day: Dragon 15 %, the others 30 %). Hitting a foe is rate-limited (one gain per 2 s) and needs a real foe (the foe rule). Standing in form does not train at full rate. Meditation gains are inside the same daily caps.

**Meditation / recovery**: with the will **off**, sneaking still on the ground (not damaged for 5 s, no item in use) meditates: strain falls three times as fast and a little assimilation and mastery are gained. `meditation = false` turns it off.

**Stat power** grows with the training: 35 % at the first spark of assimilation, up to 100 % at full assimilation and full mastery, multiplied by the strain tier and `statScale`. A will's passive grows with the first assimilation even while no form is held.

## 5. Strain, penalties and rejection

Strain (0-100) belongs to the **body**, not the will: swapping wills does not clear it. It is shown as a bar with the tier name.

| Strain | Tier | What happens |
|---|---|---|
| 0-29 | Calm | Full effect |
| 30-59 | Strained | 85 % of bonuses and ability power; a warning |
| 60-84 | Exhausted | 60 %; Weakness while in form; a clear warning |
| 85-99 | Overload | The form **ends** at once; the will goes quiet 30 s; you lose a little health (default 1 heart, **never below 2 hearts, never a death**) |
| 100 | Rejection | Only after a **deliberate second press** at the brink: the form ends, the will is locked 10 min (`rejectLockMinutes`), and 10 % of the assimilation is lost (`rejectAssimilationLoss`, at most 50 %). Nothing else is taken: no skill, no level, no item, no death. |

Strain is gained by: holding a form (per second), and using abilities (per use). Both are multiplied by `(2 - compatibility)` (poor fit up to double) and by `strainRate`; holding a phase before you are used to it (Acquire below 40 % assimilation, Integrate below 70 % mastery) multiplies the per-second strain up to **triple**, so premature overuse is punished and shown.

Recovery: stop the form and rest (0.5 strain per second), or **meditate** (three times that). Death caps strain at 59 so a respawned player is not stuck in the red. `penalties = false` shows the bar only; `rejection = false` makes strain stop at overload.

## 6. The two phases

| | Acquire | Integrate |
|---|---|---|
| Unlock | 10 % assimilation | 95 % assimilation **and** 50 % mastery |
| Stats | modest | stronger |
| Abilities | basic (1-2) | advanced signature (1-2) |
| Drain (energy per second) | about 0.5 % | about 1.2-1.4 % |
| Strain | low | higher |
| Visuals | two eye glints, a few motes | bigger eyes, ten motes, a haze, two turning rings |
| Ends | on toggle, out of energy, overload, rejection, death, logout, dimension change | same |

Controls (rebindable, category "Multiverse of Anime"): **[** toggle Acquire, **]** toggle Integrate, **;** ability 1, **\\** ability 2, **=** swap will (Shift = previous). Keys act only with a will held and no screen open, at most once per 3 ticks client side; the server rate-limits again (`MIN_GAP_TICKS`). A phase ends **safely**: modifiers are removed, the aura is turned off, unfinished abilities are cancelled, and the strain stays with the body.

### Interaction with other transformations

- **Acquire** stacks with other things; its attribute bonuses are capped per attribute (hard table in `BeastWillRules.ATTR_CAPS`), so there is no runaway multiplication.
- **Integrate** cannot be held together with a mode armour of this mod (`ModeArmor`): starting it refuses with a message, and if a mode armour is put on while Integrate is on, Integrate ends with "conflict". Switch off with `exclusiveIntegrate = false`.
- Stat bonuses are applied as modifiers with fixed ids per player and are rebuilt, never stacked, on every change.

## 7. The four wills

Numbers below are generated from the data files (`data/nusmp/beast_will/<id>.json`); change the JSON, not the Java. Energy is a share of the player's energy pool. Cooldown is before `cooldownScale`.

### Dragon Will (Aether Dragon, Sylvia-inspired, Aether affinity, legendary)

- Core: dragon-type beasts, rank S or higher, 0.4 %, one core per 14 game days.
- Absorb: 50,000 max EP. Hosts: human, elf, dwarf (others 50 %).
- About 180 min of holding a form for assimilation, about 120 min for mastery; at most 15 % of each per game day.
- Acquire: 10 % assimilation, drain 0.5 %/s. Integrate: 95 % assimilation + 50 % mastery, drain 1.4 %/s.

| Form | Slot | Ability | Energy | Cooldown | Strain | Range / radius | Targets | Boss share |
|---|---|---|---|---|---|---|---|---|
| Acquire | 1 | **Dragon Sight** | 3 % | 30 s | 4 | 20 / 20 | 6 | 100 % |
| Acquire | 2 | **Draconic Pulse** | 4 % | 8 s | 6 | 6 / 6 | 4 | 60 % |
| Integrate | 1 | **Draconic Breath** | 7 % | 7 s | 9 | 8 / 8 | 6 | 60 % |
| Integrate | 2 | **Time Exclusion** | 18 % | 90 s | 24 | 0 / 7 | 4 | 0 % |

- *Dragon Sight*: Glowing on creatures in the radius for 6 s (at most 6 creatures).
- *Draconic Pulse*: a cone of damage and push, burning at higher power.
- *Draconic Breath*: a stronger cone with burn.
- *Time Exclusion* (**bounded, never pauses the game**): a ring appears at the caster's spot and, 15 ticks (0.75 s) later, the foes still inside it (at most 4) are held: Slowness VII, Mining Fatigue, Weakness and their movement stopped for 3 s. It is a normal hard control through `BalanceLaw`: it shares the 12 s hard-control cooldown, is capped (players 3 s, other creatures 5 s) and halves on re-application within 30 s. The caster gets a short Speed boost. **Bosses are immune** (boss share 0 %). Foes that step out of the ring during the telegraph are not held. The server tick, other players, projectiles and the world never stop; no game rule or time of day is touched; the pending ring is dropped on death, logout, dimension change or the end of the form.

### Shadow Panther Will (Shadow affinity)

- Core: panther / leopard / jaguar types, rank A, 2 %, 5 days. Absorb: 10,000 EP. Hosts: human, elf, dwarf (others 60 %).
- About 90 min assimilation, 60 min mastery; 30 % per day each.
- Acquire: 10 %, drain 0.5 %/s. Integrate: 95 % + 50 %, drain 1.2 %/s.

| Form | Slot | Ability | Energy | Cooldown | Strain | Range / radius | Targets | Boss share |
|---|---|---|---|---|---|---|---|---|
| Acquire | 1 | **Shadow Veil** | 2 % | 20 s | 3 | 0 / 0 | 1 | 100 % |
| Integrate | 1 | **Shadow Dash** | 4 % | 5 s | 6 | 10 / 0 | 1 | 100 % |
| Integrate | 2 | **Ambush** | 8 % | 16 s | 12 | 8 / 0 | 1 | 50 % |

- *Shadow Veil* (stealth rules, `stealth` setting): the player is invisible for **at most 10 s**. Creatures that are not already hunting the player do not pick them as a target unless they are within 3 blocks; bosses and creatures already targeting the player are never fooled. A faint shadow trail stays visible to everybody (the counterplay for other players). The veil is **revealed**, and cannot be re-cast until its cooldown ends, by: attacking, using any ability, taking any damage, a Glowing effect, or its own timer. The Acquire phase's quietness cancels step/swim/flap game events (what Wardens and sculk sensors hear), nothing more.
- *Shadow Dash* (**validated**): a horizontal dash of up to 10 blocks along the way you are looking. The server checks every half block of the path: the chunk is loaded, the spot is inside the world border, nothing solid is in the way, there is no lava on or under the path, and the player may interact there (block protection). The dash stops at the last good spot; if that is less than 1 block it is refused with a message and **nothing is charged**. Fall distance is reset. The client never sends a position: the server alone moves the player.
- *Ambush*: picks the nearest creature in front of you within 8 blocks, steps to a spot behind it if that spot is valid (loaded, inside the border, free, protection-checked, close enough) and strikes; if no spot is valid it still strikes when the target is within 3 blocks, otherwise it is refused and **nothing is charged**. **x1.5 damage if the player was veiled**; bosses take 50 %.

### Elderwood Guardian Will (Plant affinity)

- Core: treant / dryad / elderwood types, rank A, 2 %, 5 days. Absorb: 15,000 EP. Hosts: human, elf, dwarf (others 60 %).
- About 90 min assimilation, 60 min mastery; 30 % per day each.
- Acquire: 10 %, drain 0.55 %/s. Integrate: 95 % + 50 %, drain 1.3 %/s.

| Form | Slot | Ability | Energy | Cooldown | Strain | Range / radius | Targets | Boss share |
|---|---|---|---|---|---|---|---|---|
| Acquire | 1 | **Vine Lash** | 3 % | 5 s | 4 | 8 / 8 | 2 | 60 % |
| Acquire | 2 | **Verdant Mend** | 6 % | 40 s | 6 | 0 / 0 | 1 | 100 % |
| Integrate | 1 | **Restraining Vines** | 10 % | 30 s | 14 | 0 / 6 | 5 | 25 % |
| Integrate | 2 | **Bark Ward** | 8 % | 35 s | 10 | 0 / 0 | 1 | 100 % |

- *Vine Lash*: damage plus Slowness.
- *Verdant Mend*: heal plus cure Poison (healing goes through `BalanceLaw.heal`, so it respects the heal rules).
- *Restraining Vines*: roots (Slowness VII) with **hard duration limits through the balance law: at most 3 s on players, 5 s on other creatures, and bosses a fraction (25 %) of the 1 s boss cap**; the shared hard-control cooldown and the re-application halving apply.
- *Bark Ward*: Resistance and Absorption.
- **Plant structures are particles only.** No block is placed and no entity is spawned by any ability (pinned by `BeastWillSourceScanTest`), so there is nothing to clean up on logout, death, dimension change or crash, and nothing can bypass block protection or leak.

### Lion Will (Might affinity)

- Core: lion / leonid types, rank A, 2 %, 5 days. Absorb: 12,000 EP. Hosts: human, elf, dwarf (others 60 %).
- About 90 min assimilation, 60 min mastery; 30 % per day each.
- Acquire: 10 %, drain 0.55 %/s. Integrate: 95 % + 50 %, drain 1.3 %/s.

| Form | Slot | Ability | Energy | Cooldown | Strain | Range / radius | Targets | Boss share |
|---|---|---|---|---|---|---|---|---|
| Acquire | 1 | **Mane Surge** | 4 % | 30 s | 5 | 0 / 0 | 1 | 100 % |
| Integrate | 1 | **World Howl** | 12 % | 45 s | 16 | 10 / 10 | 8 | 25 % |
| Integrate | 2 | **Mane Surge** | 6 % | 25 s | 7 | 0 / 0 | 1 | 100 % |

- *Mane Surge*: Strength and Resistance for a short time (self).
- *World Howl*: pushes foes away and slows them (a **push + Slowness, never a stun or root**), so it does not lock a player down; **bosses take 25 %**; at most 8 targets within 10 blocks.

All balance values (cost, strain, cooldown, range, power, targets, boss share, drops, hosts, unlock thresholds, stats) can be changed per will in the JSON; the **hard bounds** (`BeastWillRules.KINDS`, `ATTR_CAPS`, `MAX_COST_PCT`, ...) cannot be passed by a data pack: a file that tries is rejected by `BeastWillDef.problem()` and skipped with a log line.

## 8. Safety on change: swap, death, respawn, logout, dimension, reconnect

| Event | What happens |
|---|---|
| **Swap** (`=`) | Only with the form off and the swap lock over (`swapLockMinutes`, default 10 min). **Strain stays** (it is the body's). **Cooldowns are kept per will**, so swapping never resets them. No attribute modifiers are left from the old will (rebuilt, not stacked). Progress is kept per will. |
| **Death** | The form ends, modifiers/aura/veil/pending abilities are removed, strain is capped to 59. No will is lost. |
| **Respawn / dimension change / login** | The form is cut off safely, modifiers are rebuilt from the saved progress, the client gets the state again. Cooldown clocks are clamped so a clock from another session or a changed game time can not lock an ability forever. |
| **Logout** | The form ends; every in-memory entry for the player (run, abilities, stealth, feedback, pending ask) is dropped. |
| **Server restart** | Everything that matters is in the attachment and the ledger; nothing runs from memory. |
| **Feature disabled** (`enabled=false`) | No drops, no core works, an active will ends; saved progress is kept. |

Death or other loss never gives or removes duplicate benefits: the core is consumed in the same step the will is bound; the ledger key is written in the same step; a repeated press of "Absorb" finds the will already held and is refused.

## 9. UI

- **HUD panel** (left, vertically centred; movable and scalable through GUI Move / GUI Scale as "Beast Will HUD"): will name and rank; beast and affinity; compatibility; current form; **assimilation, mastery and strain bars** (strain bar in tier colours, pulsing in overload); next unlock; energy drain; each ability with its key and cooldown; a status line (meditating, veiled, penalties, conflict, locked out, swap lock); key hints.
- **Confirm screen** for absorption (see 2a).
- **Codex page "Beast Will"** (appended last): what it is, how to get a will, how it is learned, the forms, strain, keys, the wills, and your own progress.
- **Feedback**: short action-bar style messages with a per-message throttle, so there is no chat flooding. Dangerous states (Exhausted, Overload, Lockout) are coloured and the HUD tells you the way out (rest, meditate, wait).

## 10. Configuration

`serverconfig/nusmp-beastwill-server.toml` (SERVER; each value is clamped again at read time, so a hand-edited file can not pass a bound):

| Section | Key | Default | Meaning |
|---|---|---|---|
| beastwill | `enabled` | true | Master switch |
| acquisition | `dropChanceScale` | 1.0 (0-5) | Multiplier on every will's drop chance; 0 = never |
| acquisition | `minDropRank` | A | Lowest rank that can drop a core (never below A) |
| acquisition | `minVictimAgeTicks` | 1200 | Beast must have lived this long |
| acquisition | `rankEpC/B/A/S/SS` | 1000 / 5000 / 20000 / 100000 / 500000 | EP of each rank |
| hosts | `otherRacesMayHost` | true | Unlisted races may host |
| hosts | `otherRaceCompatScale` | 1.0 | Scale on the poor fit (kept 0.2-1) |
| hosts | `extraHostRaces` | [] | Race names that count as full hosts for every will |
| progress | `assimilationRate` / `masteryRate` | 1.0 | Speed (the daily caps still apply) |
| progress | `meditation` | true | Meditation on/off |
| progress | `swapLockMinutes` | 10 | Wait between two swaps |
| strain | `strainRate` | 1.0 | Every strain gain; 0 = none |
| strain | `restPerSecond` | 0.5 | Strain shed per second while off |
| strain | `penalties` | true | Strain has effects |
| strain | `overloadDamage` | 1.0 | Health lost in overload (never lethal) |
| strain | `rejection` | true | Rejection at full strain |
| strain | `rejectLockMinutes` | 10 | Quiet time after a rejection |
| strain | `rejectAssimilationLoss` | 0.10 (max 0.5) | Assimilation lost on rejection |
| balance | `statScale` | 1.0 | Every attribute bonus |
| balance | `drainScale` | 1.0 | Every energy cost |
| balance | `cooldownScale` | 1.0 (0.5-2) | Every cooldown |
| balance | `maxTargetsCap` | 10 (1-10) | Most creatures one ability may affect |
| rules | `exclusiveIntegrate` | true | Integrate conflicts with mode armours |
| rules | `abilitiesHitPlayers` | true | Abilities may hit players (still PvP/team/protection rules); false = creatures only |
| rules | `stealth` | true | Shadow Panther stealth on/off |

Per-will data: `data/nusmp/beast_will/<id>.json` (a data pack can override or add one; `/reload` re-reads them, rebuilds the client's info and ends nothing).

## 11. Commands

`/beastwill status [player]` (anyone for self; op for others), and, for ops (permission 2): `list` (will ids), `give <player> <will>` (bind without a check), `core <player> <will>` (give the core item), `offer <player> <will>` (open the preview as an offer), `set <player> <assimilation> <mastery>` (percent), `strain <player> <value>`, `clear <player>` (remove all wills of a player; for testing).

## 12. Cost, limits and multiplayer

Worst case, with many players:

- **Server work**: a player with no will costs one attachment lookup per tick and nothing else. A player with a will: one upkeep a second (staggered across ticks). An ability touches at most **10** creatures (default 4-8, hard cap 10); each use does one bounded area query, no chunk loads, no block change, no entity spawn, no per-tick scan.
- **Packets**: at most **4 Sync packets per second per player** (and only when something changed: a hash check), `Info` only on will change or reload, `Preview` only on an ask, `Action` rate-limited by the client (3 ticks) and the server (4 ticks). Every `Action` is range-checked and the sender must be a real server player. No packet is broadcast; the aura is sent through the existing aura lease to nearby players.
- **Particles**: only through the `Fx` facade (it holds `PerfBudgets.particles` and the quality levels; no vanilla `ParticleTypes` in the package, pinned by `FxSourceScanTest` and `BeastWillSourceScanTest`); at most 24 per call and a few dozen per ability; Time Exclusion draws one ring.
- **Client draw**: the aura draws at most 19 quads per player per frame within 24 blocks (2 eyes, 10 motes, 1 haze, 4 for two rings), 6 between 24 and 48 blocks, nothing beyond 48 blocks, and not at all for invisible players. Timed with `PerfProbe` section `c.beastwill.aura`. The HUD builds its text every 10 ticks, not per frame.
- **Memory**: one small `Run` per player with a will; removed at logout, death and dimension change. The ledger grows by one short string per used offer.
- **No lag**: nothing here runs per tick for players without a will; nothing allocates in the render loop.

CI: self-tests `beastwill.catalog.load` (data files load and validate), `beastwill.rules.bounds` (arithmetic and bounds), `beastwill.player.roundtrip` (a player-level round trip); the smoke list holds the prefixes `beastwill.catalog`, `beastwill.rules`, `beastwill.player` are on the smoke list. `ServerSafetyTest` is untouched (no client class in common code); `BeastWillSourceScanTest` pins: no client class, no static player, UUID-keyed maps, no block/entity/chunk ops in abilities, hard control through the balance law, targets through the foe rule, particles only through the `Fx` facade, validated packets, SERVER config only.

## 13. Assumptions

1. **Ranks are derived from EP** (C/B/A/S/SS thresholds), because Tensura has no rank system of its own for beasts. The thresholds are configurable.
2. **A "mana beast" is any wild creature Tensura or a listed mod gives EP to**, plus the entity ids/keywords in the will files. The exact Tensura entity ids of lion/panther/treant-type beasts were **not verified in a game** (name keywords are used so a renamed entity still matches).
3. **One active will**; up to 8 kept (progress); swapping needs the form off and a lock.
4. **Cores are not trade-locked**: a core is an item and can be traded; the will binds to whoever absorbs it. (The owner can lock it later; no request for that.)
5. **Wills do not take skill slots**: a will is a new system, not a Tensura skill. This keeps `Reset Scroll` and skill-removal commands away from it (they can not remove a will; `/beastwill clear` can).
6. **The Sylvia-inspired Dragon Will is a legendary "Aether Dragon" will**, not a copy of the character's canon powers; Time Exclusion is a bounded, balanced reading of her time-stop-like ability (the owner's brief: "never pauses the game").

## 14. Verified, not verified, limitations

**Verified here**:

- The pure rules, the data validator, the state model: unit tests (`BeastWillRulesTest`, `BeastWillSchemaTest`, `BeastWillStateTest`).
- The source properties above: `BeastWillSourceScanTest`.
- All four data files load and validate.
- GUI Move / GUI Scale registrations, lang fragments, creative-tab items: the existing `GuiMoveRegistryTest` / `GuiScaleTest` / lang tests.
- Typecheck of all new and changed files (only the known stub-library artifacts are reported).

**Not verified (nothing has been seen in a game)**:

- The compile against the real NeoForge/Tensura/ManasCore jars (CI is the real compile; this doc is written before the CI result).
- The HUD layout and default position, the confirm screen layout, the Codex page layout at your GUI scale.
- The aura: the head-yaw sign for eye placement is unverified in a game.
- How each ability **feels** (damage numbers, ranges, particle counts) and balance.
- The Tensura entity ids of the lion / panther / treant beasts and the actual EP values they have (the drop only fires for beasts that reach rank A).
- Dedicated-server behaviour with a real second player (the code is server-authoritative and UUID-keyed; the CI smoke test runs the three Beast Will self-tests only).

**Temporary assets (flagged)**:

- The **core item icon** is a flat 16x16 glowing orb (`tools/beastwill/gen_core_icon.py`). It is temporary; the rule 7 standard (a 3D in-hand model like the Genesis Demon-Slayer) is the next step and needs art.
- The **forms have no per-creature model** (mane, panther ears, antlers, horns). The look is built from existing white-ish glow sprites (`air_glint`, `air_orb`, `air_ring`, `beast_haze`) tinted by the will's colour. A real model needs art.

**Limitations**: cores can be traded; one active will; abilities are effect-only (no summoned plants or blocks, by design); only the English language file is shipped; the four wills are the first set (new wills are a new JSON, no code, as long as they use the 12 existing ability kinds; a new kind needs Java and a `KindLimit`).

## New / replaced / kept

- **NEW**: the whole Beast Will system (rules, data, loader, store, ledger, drops, core item, acquisition and confirm screen, manager, 12 ability kinds, stealth, attributes, packets, HUD, Codex page, aura, 5 keys, 8 commands, SERVER config, 4 wills, tests, self-tests, docs, core icon).
- **REPLACED**: nothing.
- **KEPT**: everything else; shared files were only appended to (`Aura.BEAST_WILL`, `AuraRegistry`, `CodexPages`, `GuiMoveRegistry`, `GuiScaleRegistry`, `NUCreativeTab`, `NUSMP` hooks, the smoke list, the lang file).
