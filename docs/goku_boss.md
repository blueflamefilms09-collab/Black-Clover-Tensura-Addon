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

## 4.90.0: he is not killed, and he takes no anonymous damage

**The farewell.** Beaten (his health would reach 0), Goku is not killed: his health stays at 1, he cannot be hurt for 3.5 seconds, he is knocked up, and says "Haha... you got me! Adios! I'll see you next time, when I'm stronger!" (chat text, no voice clip). Then he flashes away (light, a teleport sound, invisible) and the framework's own death runs at that moment: his loot drops, the story credit and the end of the fight happen as for a win, with no second line. Setting `goku_boss.farewell` (default true); false = he dies as before. Cost: one tick counter, one flash and one sound; no packets beyond the existing particle ones.

**Random damage.** Setting `bossRules.anonymousDamageBosses` (default `anime_multiverse:goku_boss`): a listed boss takes no damage at all when nothing attacked it, whatever the damage type (a dimension's own damage, an unknown mod's zone, burning, a blow he dealt himself). A blow with an attacker, the void and `/kill` stay real. The first anonymous damage type met is written once to the server log ("a boss ... took anonymous damage of type ..."), so the real source can be found. Not seen in a game: the cause of the damage is not known, this stops every kind of it for him.

## Form bars (4.97.0)
Each form has its own health bar, like Lucifero's phases. When the bar of a form runs out, `breakBar()` (called first from `die`) keeps Goku at 1 health, starts the windup into the next form and makes him untouchable during it; when the form takes hold the bar is refilled to full (`refillBar`). The last form still ends with the farewell/death. Settings: `goku_boss.formBars` (default true; false = the old single bar) and `goku_boss.formBarShare` (default 0.4, the size of one bar as a share of the old whole bar). Worst case cost: one extra refill and one ring burst per form change; no new per-tick work.

## Punches, no theft, no eviction (4.98.0)
`goku_boss.punches` (default true): `punchTick` runs between moves, runs at the target and punches every `punchGapTicks`; physical blows are multiplied by `punchMultiplier` (4.0) and may take up to `punchHitShare` (0.75) of a player's health. Worst case: one blow per 14 ticks and one path request per 10 ticks per Goku. `bossRules.noPoolTheft` stops Omega's Energy Drain and Skill Seal and every boss use of the energy-drain helper. The realm only carries people home after a win (`bossRealm.returnOnlyAfterWin`) and no longer evicts on the 30-minute timeout unless `bossRealm.timeoutEvicts`.

## EP, the 1% floor and smart forms (4.102.0)
**Asked for:** Goku carries 1 trillion Existence Points (EP) at the start and 500 trillion at his strongest; he scales to one hundredth of the player he fights; every form change happens with him held at 1% of his health (he cannot be hurt below it, so the change is forced); and against a very overpowered player he may skip forms.

* **EP ladder** (`GokuEp`, `goku_boss.epLadder`, `epStart` 1e12, `epEnd` 5e14): his own Tensura EP is `start x (end/start)^(step/top)`, so exactly 1 T in Base and exactly 500 T in his last form (step 9 classic, step 10 with Omega), about 2x per form. Written with `EnergyHelper.setBaseMaxEP` once per form change, never per tick. This is his OWN EP; the EP of the players he reads for his health and damage is unchanged. Chart: Base 1 T, SS 2 T, SS2 4 T, SS3 8 T, God 16 T, Blue 32 T, Blue Kaio-ken 63 T, UI Sign 126 T, Perfected UI 251 T, True UI 500 T.
* **One hundredth of the player** (`goku_boss.epPlayerShare`, default 0.01, 0 = off): his EP is never below 1/100 of the strongest player's EP near him, read at the existing scan (every 5 s, at most 10 players); the ladder is the floor and this only raises it. Before the fight he follows the party up and down; in a fight it only grows.
* **1% floor** (`goku_boss.formFloor`, default 0.01, 0 = the old 1 health; needs `formBars`): when a hit would take the bar of a form below 1% of his maximum health, he stops AT 1%, is untouchable while he powers up, and comes back in the next form with a full bar. A single huge hit cannot kill him between forms (`hurt` holds him at the floor; the `die` path uses the same floor).
* **Smart forms** (`goku_boss.smartForms` default true, `maxFormSkip` default 3, 0 = never): the strongest player's EP is compared with his own; one extra form is skipped per doubling of the player's EP over his, at most `maxFormSkip`, never above his top form (`GokuLadder.skip/target`, pure). A weak or equal player gets the plain next form. Only a one-step change (bar break or timer) becomes a skip.
* **Cost:** no new entity, packet or tick work. One EP write per form change and, with the share on, per 5-second scan; one extra EP read per player in the existing 10-player scan.
* **Not checked in a real game:** whether 500 trillion EP changes Tensura's own damage, resistances or the Saiyan Battle Power the players earn; `setBaseMaxEP` may be rewritten by Tensura if it recalculates on its own (then the ladder shows less than set). The settings switch every part off.
* **Tests:** `GokuEpTest`, `GokuFloorSkipTest`, self-test `goku.boss.formbar` (now checks the floor and a heavy hit at the floor).

## Smart spawn: EP from the players (4.102.0, all bosses)
`BossEpScale` (setting `bossRules.epFromPlayers`, default true; `epStrictness` 1.0; `epPerPlayer` 0.25): when a boss of the mod spawns, the players of ITS dimension are read once (alive, not creative or spectator, at most 64 EP reads) and its base EP is raised to `strictness x strongest player EP x (1 + 0.25 per extra player, at most 10 count)`. It only raises, never lowers. Shares: **Goku 1.0** (he starts level with the strongest player), **Sinbad 0.6**, **other bosses 0.4**, **Kakashi 0.3**. Goku's whole ladder is lifted by it: Base = that number (never below the 1 trillion start), last form = Base x 500 (the same climb). His scan also keeps it up to date before and during the fight (only growing in a fight), writing only when the number changed. Sinbad, Kakashi and the other bosses of `ArcSpawner.summon` get it once at spawn. The seven older bosses spawned by `ExistingBossSpawner` are NOT covered yet.
Cost: one pass over the dimension's players and one EP write per boss spawn; Goku one extra EP read per player in his existing 5 s scan and a write only when the number changes. Tests: `BossEpScaleTest`.

## True and spiritual damage (4.103.0)
Every blow Goku lands on a creature (fists, beams, Spirit Bomb, Kienzan, ... all go through `strike`) is cut into up to three hits in the same tick, after the hit cap has limited the whole blow:
* **normal**: the plain blow, as before (armour, resistance and shields count);
* **true** (`anime_multiverse:goku_true`): ignores armour, enchantments, resistance and effects (Minecraft `bypasses_*` tags) and Tensura's barriers and protection enchantments (`tensura:bypass_barrier`, `bypass_protection_enchantment`);
* **spiritual** (`anime_multiverse:goku_spirit`): added to Tensura's `tensura:is_spiritual` (the same family as Soul Scatter), so it passes barriers and dodges (`bypass_barrier`, `bypass_dodge`) and ignores armour and enchantments.
Shares: `goku_boss.trueShare` 0.30 and `spiritShare` 0.20 in his top form; 40% of that in Base, rising evenly (`GokuBossMath.splitScale`); together held to 0.9, so at least 10% stays a normal blow. If the normal part is dodged or blocked, the other two do not land either. The extra hits reset the target's hit-immunity (`invulnerableTime = 0`) so all three count. `goku_boss.splitDamage` false = one plain hit as before. The hit cap (`hitShare`, `maxHitPercent`) limits the SUM, so a player is not killed faster than before; the damage that gets past armour, barriers and dodges is what grows.
Cost: at most two extra `hurt` calls per landed blow of his (he strikes a few times a second at most), no entity, packet or tick work. Tests: `GokuSplitDamageTest`. Not seen in a real game: how Tensura's resistances treat the spiritual type on top of its tag membership, and whether other mods' damage hooks reduce the true part.

## Answers to the DxD mod (4.103.0)
The Devil x Slime mod (`dxdtensura`, High School DxD) attacks a fighter's strength itself: Divine Dividing's `dxdtensura:divided` halves attack and speed once per level, `reduced` is Albion's poison, `compressed` crushes magicule and aura, plus pressure and binding effects and a long list of damage types (`compression_divider`, `destruction`, `god_slayer`, ...). Read from the jar (the effect and damage-type ids and the Divided effect's attribute changes); the mod's skill code was not run.
* **Effects do not take hold** (`GokuBoss.canBeAffected`): any HARMFUL effect whose id is in a namespace of `goku_boss.counterMods` (default `dxdtensura`) is refused on him. No event bus and no tick cost; the check runs only when something tries to apply an effect.
* **Purged attribute changes** (`purgeForeign`, twice a second): a modifier from a listed namespace on his attack, speed, max health, armour, attack speed or knockback resistance is removed, for a skill that changes the numbers without an effect. Six attribute reads per half second, no allocation unless one is found.
* **Damage cut** (`counterDamage`, 0.5): damage of a listed mod's damage types is halved against him (1 = off). The pools Compression Divider crushes are refilled by the existing `keepPoolsFull` rule.
* **The flare** (`counterFlare`): each time he shrugs one off he flares his ki: a ring in his form's colour and a shove of the players within 5 blocks (never a hurt), at most once per 5 seconds.
* The list is open: add any mod's namespace to `counterMods` to give it the same answers. `counterEffects` false switches the effect and attribute answers and the damage cut off.
* Cost: nothing per tick beyond the six-attribute purge every 10 ticks; one entity query per flare (at most one per 5 s per Goku). Tests: `GokuCountersTest`. Not seen in a real game with the DxD mod loaded; whether its Divide skill writes attributes or an effect was read from the class strings, not run.

## Gentle EP (4.103.1)
The 4.102.0 ladder (1 trillion to 500 trillion) is replaced by 1 billion to 10 billion, and `goku_boss.epTame` (on) keeps it there: the climb is at most `epMaxClimb` (10x), the last form at most `epCeiling` (100 billion). `GokuEp.tamed` slides the whole ladder down (never squashes it), so an old config that still says 1 trillion / 500 trillion ends on the ceiling with a 10x climb. Players still lift Base (smart spawn) and the `epPlayerShare` floor stays, both capped by the ceiling. `epTame` false = the 4.102.0 ladder as the numbers say. Cost: arithmetic once per form change.

## True damage that always lands, a stronger Goku, richer visuals (4.107.0)
**True damage.** Reported: he did not seem to do true damage. The 4.103.0 split only sent the true part when the normal part landed, the damage type did not skip hit-immunity or shields, the part was a small share of an already capped blow, and nothing showed it. Now (`trueGuarantee`): the true part is delivered even if the normal part is refused; `goku_true` is in `minecraft:bypasses_invulnerability`, `bypasses_shield`, `bypasses_cooldown` and `tensura:bypass_dodge`; the health really lost is compared with the intended part (`GokuBossMath.missing`) and the rest is taken directly (a lethal rest uses one maximal true hit so death, totems and drops go the normal way). Creative and spectator players are skipped. `trueFloor` (0.03) lifts the part to a share of the victim's max health (`GokuBossMath.trueAmount`), so high-EP victims feel it; `trueFeedback` shows a flash and an action-bar line.
**Stronger.** `powerUp` multiplies his health (`powerUpHealth` 1.5) and damage (`powerUpDamage` 1.35) after the preset, EP scaling and players, within twice the old factor caps, and shortens the punch gap by 20% (min 6 ticks). The hit caps (`maxHitPercent`, `punchHitShare`, Omega's cap) still apply to the normal blow; the true floor is the one planned exception (at most 3% of max health per blow).
**Visuals.** `richFx` adds the layers listed in the changelog; each goes through `PerfBudgets.particles` (`GokuFx.take`), so many players or two Gokus can never flood the network.
Settings (server): `goku_boss.trueGuarantee`, `trueFloor`, `trueFeedback`, `powerUp`, `powerUpHealth`, `powerUpDamage`, `richFx`; all false/1.0 restore the 4.106 behaviour.

