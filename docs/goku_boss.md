# The Goku boss

Owner request: "make Goku boss NPCs that is very overpowered and scales with his actual anime, and he can actually change his forms like the anime."
Addition: his strength follows the player's Tensura Existence Points (EP).

Status: written without a game and without a compiler on the author's side. Nothing here has been seen running. See "Not seen in a game" at the end.

## What was built, in plain words

A boss called **Son Goku** (id `goku_boss`, a member of the arcs' boss roster, saga Allies, tier 5). He is an ordinary arc boss (health bar, arena leash, story credit, spawn egg, boss registry
row, boss buff row) that:

* **changes form like the anime**, in the order Base, Super Saiyan, Super Saiyan 2, Super Saiyan 3, Super Saiyan God, Super Saiyan Blue, Blue with Kaio-ken, Ultra Instinct Sign, Perfected
  Ultra Instinct, True Ultra Instinct. A form is entered when his health falls under its line **or** when the fight has lasted long enough (so a party that keeps him high still meets the late
  forms). Only one step at a time and at least 6 seconds in a form. He never goes back down (healing does not undo a form).
* **shows every change**: a 2-second power-up (aura particles gather, a ring grows, thunder at the middle where his skin and hair colour swap, a burst at the end that shoves nearby players).
  He is not invulnerable during it and not healed by it. He shouts a line for each form in the chat of the players near him.
* **wears his form**: its damage, speed and armour are put on him as temporary attribute modifiers, replaced at each change and removed on death, removal, a lost fight and server stop.
* **fights with five attacks**: a four-blow combo, the Kamehameha, Instant Transmission, a Kaio-ken burst (Base to Super Saiyan 3 only) and the Spirit Bomb (from Super Saiyan 2).
* **dodges from Ultra Instinct Sign on**, with a chance, a cooldown and a limit per window (the chart's), never above the `uiDodgeCap` setting. A silver flicker shows when the dodge is ready.
* **gets angrier with time** (soft enrage): after `enrageMinutes` (12 by default, shorter with high EP) he is in True Ultra Instinct whatever his health.
* **is bound to his arena** (the framework's leash) and never spawns more than `globalCap` (2) at once on a server.

## How each attack works (all decided on the server)

| Attack | What happens | Bounds |
|---|---|---|
| Meteor Combination | Four blows 5 ticks apart; each re-checks the reach; the last is stronger and shoves. | 4 timed hits, reach 4.5 blocks |
| Kamehameha | 1.8-second telegraph (motes in his hands, a line to the target), then one block raycast and one foe query along the line; each foe on the line is hit once (magic damage). | 40 blocks, 1 raycast, 1 query, at most 24 particles per effect call |
| Instant Transmission | Blinks behind the target and strikes after 6 ticks. Needs a safe spot inside the arena leash, otherwise it is not used. | 8 to 48 blocks, cooldown 7.5 s |
| Kaio-ken burst | 5 seconds of x2 Kaio-ken damage and the chart's speed; he takes 10% more while it lasts. | Cooldown 25 s, forms Base to Super Saiyan 3 |
| Spirit Bomb | 6-second telegraph (a growing sphere over his head and a ring where it will land). The landing spot follows the target until the last 1.8 seconds, then it is fixed: **moving away avoids it**. A hit of 2% of his maximum health or more while it gathers **breaks it** and stuns him for 3 seconds. If it lands, everyone within 10 blocks takes a big magic hit. He is exposed afterwards. | Cooldown 45 s, 1 foe query |

Blocks: the Kamehameha and the Spirit Bomb break **nothing** unless `blockDamage` is on **and** the existing `griefBlocks` boss rule allows it; then at most 9 or 25 blocks per blast, through the
boss framework's own block ledger (which puts them back).

One hit on a player never takes more than `maxHitPercent` (30%) of that player's maximum health. On Hard difficulty the share is divided by 1.5 first (the game raises a mob's damage after the cap).

## The forms (the table the boss follows)

The numbers of damage, defence and speed are **read from the owner's Saiyan chart** (`saiyan/SaiyanForms`, the same forms a Saiyan player uses). Entry lines and time fractions are this boss's own
schedule. "Canon ratio" is how many times stronger the series says the form is than the one before; it is **informational** (the game uses the chart).

| # | Form | Health under | Or fight time | Damage (preset overpowered) | Defence (chart) | Speed (chart) | Canon ratio to previous | Cumulative |
|---|---|---|---|---|---|---|---|---|
| 0 | Base | - | - | x1.00 | 0.00 | +0.00 | 1 | 1 |
| 1 | Super Saiyan | 85% | 10% | x1.75 | 0.35 | +0.15 | 50 | 50 |
| 2 | Super Saiyan 2 | 72% | 20% | x2.10 | 0.45 | +0.22 | 2 | 100 |
| 3 | Super Saiyan 3 | 60% | 30% | x2.60 | 0.55 | -0.10 | 4 | 400 |
| 4 | Super Saiyan God | 50% | 40% | x2.90 | 0.80 | +0.18 | (no figure) | 400 |
| 5 | Super Saiyan Blue | 40% | 50% | x3.30 | 0.90 | +0.24 | 50 | 20,000 |
| 6 | Blue with Kaio-ken | 30% | 60% | x4.00 | 0.90 | +0.30 | 10 | 200,000 |
| 7 | Ultra Instinct Sign | 20% | 70% | x4.00 | 0.90 | +0.20 | (no figure) | - |
| 8 | Perfected Ultra Instinct | 10% | 82% | x4.00 | 1.00 | +0.25 | (no figure) | - |
| 9 | True Ultra Instinct | 4% | 95% | x4.00 | 1.05 | +0.28 | (no figure) | - |

"Or fight time" is a share of the enrage time. Notes on the table:

* Damage and defence never fall from one form to the next ("monotone maximum"). The chart gives Ultra Instinct Sign a lower damage cell than Blue with Kaio-ken (it is a dodging form), so the boss
  keeps the higher number: every form is at least as dangerous as the last. The price is that the last four forms have the same damage multiplier; they differ in defence, speed and the dodge.
* The skin shows five looks: black hair (Base), gold hair (Super Saiyan 1 to 3), red hair (God), blue hair (Blue and Blue with Kaio-ken), silver hair and a dark gi (Ultra Instinct forms).
* The "difficulty" preset scales how much of the chart's damage bonus counts (normal 60%, overpowered 100%, anime 130%).
* The share of every hit he takes less is defence / (1 + defence), at most 60%; half of it is applied as armour, half as damage taken less.

### Sources for the anime ladder (rule 10: English wiki first, Japanese as fallback)

* English Dragon Ball wiki (dragonball.fandom.com): **could not be fetched** from the build session (the host is not on the session's network list: `ENOTFOUND`). Not retried.
* Japanese Dragon Ball wiki: **not fetched** (the fallback was not reachable either; there was no second route).
* So the order of forms and the ratios in the "Canon ratio" column are **from the author's memory of the series and are not verified against a wiki**. They are printed for the owner to check; the
  game does not depend on them. The numbers the game uses (damage, defence, speed, Kaio-ken multipliers, the dodge) come from the owner's chart in `SaiyanForms`.
* Please check these ratios when a wiki is reachable: Super Saiyan 50x Base; Super Saiyan 2 2x Super Saiyan; Super Saiyan 3 4x Super Saiyan 2; Super Saiyan Blue 50x Super Saiyan God;
  Kaio-ken times ten on Blue.

## Scaling: EP first

EP is the first input. The same EP reader the rest of the mod uses (`TensuraBridge.maxEp`; the Saiyan code uses it for a victim's EP) is called **once per player per scan**; there is no second reader.

**When:** at spawn, then at most every **100 ticks** (5 seconds). Players are found with one box query (64 blocks around him); **at most 10** are read.

**What is read:** the strongest player's EP among those near him (creative and spectator players do not count).

**Fallback chain (never 0):**

1. Tensura EP of the strongest player (needs `epScaling` on and a result above 0).
2. Else the strongest Saiyan Battle Power, times 20 (the inverse of the Saiyan `epWeight` of 0.05).
3. Else the difficulty preset's fixed value: normal 5,000 EP, overpowered 30,000 EP, anime 300,000 EP.

The result is never below the preset's value (a weak party still meets a boss that is ahead of it).

**The curve** is the balance law's own: `curve(EP) = 1 + (log10(EP) - 3) x 2.5`, 1 at 1,000 EP and below, +2.5 per decade, at most `epCap` (itself 1 to 12).

**Health:** `preset health x healthMultiplier x (1 + epWeight x (curve - 1)) x (1 + perPlayerBonus x (players - 1))`, between 0.1 and **60**.
**Damage:** `preset damage x damageMultiplier x (1 + 0.5 x epWeight x (curve - 1)) x (1 + 0.4 x perPlayerBonus x (players - 1))`, between 0.1 and **24**. (Damage takes half of the EP growth so the per-hit cap still means something.)
**Form pace:** the enrage time is shortened by up to 40% as the curve rises to `epCap` (so a stronger party meets the late forms sooner).

Players counted: at most **10** (`playerCap` cannot be set higher).

Examples with the defaults (preset overpowered, epWeight 0.6, epCap 8, perPlayerBonus 0.35), base health 30,000:

| Strongest player EP | Curve | Health factor (1 player) | Health | Damage factor (1 player) |
|---|---|---|---|---|
| 800 or no data (preset) | 4.69 | x3.21 | about 96,000 | x2.11 |
| 100,000 | 6.00 | x4.00 | 120,000 | x2.50 |
| 630,000 and more (cap) | 8.00 | x5.20 | 156,000 | x3.10 |
| 630,000 with 10 players | 8.00 | x21.6 | 648,000 | x4.32 |

**During a fight he only ever grows:** a reading that falls (a strong player left) never lowers his health or damage; the reading is reset when a fight is lost or abandoned. When his maximum
health changes he keeps the same share of the bar (no free heal, no instant loss), and the change does not count as damage or healing for the framework's budgets.

## Settings: `nusmp-goku-boss-server.toml` (section `[goku_boss]`, server side)

| Key | Default | Range | What it does |
|---|---|---|---|
| `enabled` | true | | False: he cannot be summoned by any door. |
| `difficulty` | overpowered | normal / overpowered / anime | The preset: base health and damage, form strength, fixed EP. Unknown words mean overpowered. |
| `healthMultiplier` | 1.0 | 0.1 to 50 | On top of the preset and EP scaling. |
| `damageMultiplier` | 1.0 | 0.1 to 20 | Same, for damage. |
| `epScaling` | true | | False: Battle Power, then the preset. |
| `epWeight` | 0.6 | 0 to 5 | How strongly EP raises him (0 = EP changes nothing). |
| `epCap` | 8.0 | 1 to 12 | Highest value of the EP curve that counts. |
| `perPlayerBonus` | 0.35 | 0 to 2 | Extra health per player beyond the first (0 switches player scaling off). |
| `playerCap` | 10 | 1 to 10 | How many players count. |
| `maxHitPercent` | 0.30 | 0.05 to 0.9 | One hit takes at most this share of a player's maximum health. |
| `uiDodgeCap` | 0.35 | 0 to 0.6 | The highest chance of the Ultra Instinct dodge (0 switches it off). |
| `enrageMinutes` | 12 | 3 to 60 | The soft enrage timer. |
| `blockDamage` | false | | The beam and the bomb may break blocks (also needs the `griefBlocks` boss rule). |
| `globalCap` | 2 | 1 to 4 | How many may stand in the whole server. |

The original request named a `playerScaling` setting; it is covered by `perPlayerBonus` and `playerCap` (a bonus of 0 is "player scaling off"), so there is no extra switch.
Every reader falls back to the default while the config is not loaded.

## Doors (how he appears)

* `/multiverse goku_boss spawn [pos]` (operators, permission 2): at your feet or at the given spot. Also `/multiverse goku_boss status` (lists each one with form, reading, factors) and `/multiverse goku_boss clear`.
* The normal arc doors: `/multiverse arc summon goku_boss`, the arc spawn egg.
* Every door goes through `ArcSpawner.summon`, which asks `GokuBosses.gate()`: `enabled` off gives "disabled"; `globalCap` of them already standing gives "failed". Forced summonings are checked too: the cap is
  what keeps a server from lagging. A boss counts from the moment it is created (not from its first tick), so a burst of summonings in one tick cannot pass the cap.
* **One per arena:** a summoning closer than 24 blocks (same dimension) to a standing Goku is refused (`ALREADY_PRESENT`), forced summonings included; the arcs' own summoning also refuses a second boss of the same character in one spot.

## Cleanup

Logout, death and dimension change of a player remove that player from his fighter list and trigger a fresh reading next tick; server stop forgets every boss in the registry; his modifiers are removed
on death, removal and a lost fight; a boss removed from the world is dropped from the live set at once.

## Drops

Magic Stones (12 to 24), 2 to 4 Tensura gold coins and one Half Senzu Bean Charm (an existing relic of the mod; skipped if it were ever not registered). Dropped in the boss's own death loot, not
through a `boss_drops` data file (that folder is pinned by `BossDropAssetsTest`). **No Unique skill**: the boss rules (`docs/boss_rules.md`) forbid a boss to own or use Unique and Ultimate skills,
so he has none and gives none.

## Look

The shared person body (`npc_default`, the one the Tier List bosses wear) in five skins made by `python tools/goku_boss/gen_goku_skins.py` (it reuses the Tier List skin tool): `arc_goku`,
`arc_goku_ssj`, `arc_goku_god`, `arc_goku_blue`, `arc_goku_ui`, each with a tiny `_glow` (belt buckle). Each file is about 4.5 KB (limit 350 KB). The client picks the skin from the form (one synced
integer, no packets). Size 1.97 blocks.

**No new 3D model was made, and Blender was not used** (rule 14): the body is the existing shared one. Hair is a skin colour; spiked hair meshes and aura layers exist only for player models in this mod,
so the per-form hair and aura are the skin colour plus server-side particles.

## Worst-case cost (rule 11, the worst case)

* Entities: at most `globalCap` (2, hard maximum 4) bosses; no move spawns an entity.
* Per boss per tick: a few comparisons. Every 20 ticks the framework's upkeep. **Every 100 ticks one box query** (64 blocks) with at most **10 EP reads** (one per player). A form change at most every 120 ticks.
* Particles: one packet burst every 4 ticks while a form is on (at most 8 particles), and at most 24 particles per effect call, all through `PerfBudgets.particles`, so many players and bosses cannot flood the network.
* Network: ordinary particle packets to nearby players, the boss bar of the framework, and one synced integer for the form. No custom packet.
* Client draw: the shared body, five cached specs, an array lookup per frame; no new mesh, no per-frame allocation.
* Moves: the Kamehameha is 1 raycast + 1 foe query; the Spirit Bomb 1 foe query; Instant Transmission one safe-spot search; no per-tick world scan, no chunk load.
* Memory: a set of at most a handful of bosses; no per-player map (everything is rebuilt from the box query).

## Tests

* `GokuLadderTest`: canon order, every form is in the chart, no skipped step, never down, the dwell, time forcing, monotone strength, the skin files exist and are small.
* `GokuBossMathTest`: the fallback chain (EP, Battle Power, preset, never 0 even for NaN or infinity), the curve is monotone and capped, health and damage are monotone in EP and stop at `epCap`, the 10-player cap,
  the hard ceilings, the per-hit cap, the dodge cap, the settings' defaults.
* `GokuBossSourceScanTest`: nothing client-only, no entity spawns, no direct block writes, one rate-limited player query and one EP reader, particles only through the budget, every door asks the gate, the wiring.
* Self-tests in the server smoke run (`SMOKE_EXPECT`, prefix `goku.boss`): `goku.boss.data` (ladder, scaling, settings, roster, registry, entity type), `goku.boss.spawn` (a real summoning, the scan, a form change and
  its modifier, and the clean removal), `goku.boss.cap` (the cap holds and opens again).

## Honest caveats

* **Not seen in a game.** The build session had no compiler and no game; the code is checked by reading and by the CI. The look of the skins, the feel of the form changes, the balance of the numbers (96,000
  health at the low end, 4x damage in the late forms) and the pacing of the telegraphs are unverified.
* No custom audio: the lines are chat messages and the sounds are vanilla ones (growl, thunder, beacon, enderman teleport).
* No altar, no story arena, no quest: he is placed in the roster under the saga Allies (a new saga entry would have broken the existing saga tests). He appears by command or egg.
* The wiki numbers could not be fetched (see above).
* Local gradle could not run in the author's session.
* The version was **not** bumped and nothing was committed; the lead does that.

## NEW / REPLACED / KEPT

**NEW:** the whole `arc/goku` package (`GokuBoss`, `GokuLadder`, `GokuBossMath`, `GokuBossConfig`, `GokuFx`, `GokuMoves`, `GokuBosses`, `GokuBossEvents`, `GokuBossCommand`, `GokuBossSelfTests`); the
settings file `nusmp-goku-boss-server.toml`; the command `/multiverse goku_boss`; ten skin files `arc_goku*.png`; `tools/goku_boss/gen_goku_skins.py`; the language key `entity.anime_multiverse.goku_boss`; three tests; this document; the
self-test prefix `goku.boss` in the smoke list.

**REPLACED:** nothing. (Checked: the mod has the Saiyan forms for players and Goku abilities for players, but no Goku boss or NPC.)

**KEPT:** everything else, unchanged. Edits to shared files are additions only: one gate block in `ArcSpawner.summon`, one line in `ArcCharacters.bootstrap`, three lines in `ArcSystem.init`, one line in
`MultiverseCommands`, one skin hook in `ArcBossRenderer`, one token in `smoke.yml`, one key appended to `en_us.json`. `SaiyanForms`, the Saiyan engine and the Time Magic effects are untouched.
