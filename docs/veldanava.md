# Veldanava, the Creator God (boss, 3.11.0)

One boss, three bodies, built from the owner's three reference images and the loot guide / build document / modelling prompt.
Everything below is what is in the mod now; the end of the file lists what the guide asks for that is **not** built.

## How to meet him

| What | How |
|---|---|
| Spawn (operator) | `/multiverse boss veldanava` (permission 2). Spawns on the ground where you stand; refused if one already lives in that world. |
| Spawn egg | **Veldanava Spawn Egg** in the creative tab (same rules: one per world, off when the config says so). |
| Take your boon | `/multiverse veldanava boon <0..3>`: this is what the clickable lines in chat run. `0` only collects the loot for someone who already holds a boon. |
| Turn him off | `nusmp-veldanava-server.toml`, `enabled = false`. |

Server config `nusmp-veldanava-server.toml` (all SERVER, they change gameplay): `enabled`, `bossHealth` (4000 per bar), `damageScale`, `soulSeizing`, `willOfTheCreatorSeconds` (300, 30..1800).

## The fight

| Body | Model | Hitbox | Moves |
|---|---|---|---|
| **Star King Dragon** (phase 1) | `veldanava_true_dragon`, drawn 1.6x its model file | 7.5 x 9 | Stardust Sweep, Gravity Pull, Starfall, Dragon Roar, Bite |
| **Creator God** (phase 2, a second full health bar) | `veldanava_humanoid` | 0.6 x 1.95 | God Speed, Genesis Slash, Justice King Michael, Covenant King Uriel, Law Manipulation |
| **Dark Avatar of God** (the last 10%: "Limiter Released") | `veldanava_dark_avatar` | 0.75 x 2.5 | the Creator God's five moves, faster and 30% harder, plus **Genesis: Creation of the World** |

* Form changes: the dragon cannot be killed; a blow that would kill it starts an 80-tick transformation (invulnerable, hitbox swaps at the midpoint) into the Creator God with a full bar. At or under 10% of that bar the Limiter is released and the Dark Avatar finishes the fight without a refill. Each change plays the old body's `cast_big` clip, a flash, then the new body's `transform` clip.
* Every move has a telegraph (windup), an effect and a recovery; an ultimate leaves him exposed (+damage taken) after it. Damage to players is a share of their max health with a floor and a cap (`VeldanavaRules.damage`), scaled by the body and by `damageScale`.
* **Soul Seizing** (anti-totem, `soulSeizing`): in the two human bodies a blow that would kill a player never lands; the player is put in spectator mode until the fight ends (restored on boss death, removal, reset or login). Not in the dragon body.
* **Justice King Michael:** an 80-tick guard, nothing gets through, projectiles are sent back; **Covenant King Uriel:** a prison of slowness and no-jump under a seal; **Law Manipulation:** one of Weight / Fragility / Decay on every player within 30 blocks (7 s at most).
* The boss is anchored to the arena, calls itself back past 64 blocks, resets (full health, ledger restored, seized players released) after 12 s with nobody near, and is capped by the shared boss DPS budget (`docs/boss_dps_budget.md`).
* Blocks: only when the `griefBlocks` game rule is on, only hardness 0..20 (never obsidian, bedrock, block entities, wither-immune), at most 24 per tick, all recorded in the `BlockLedger` and put back when the fight ends or the boss is removed.

## Will of the Creator and the loot

When he falls an orb (**Will of the Creator**) appears over him for `willOfTheCreatorSeconds`. Each fighter (up to 8: whoever hurt him or stood within 48 blocks) gets a chat message with three clickable lines. The choice runs `/multiverse veldanava boon N`, is validated on the server (sender is a player, the orb is waiting for them, N is 0..3, one boon per player ever, one share per orb) and:

| N | Boon (permanent attribute modifiers, survive death and relog) |
|---|---|
| 1 | Magic King Solomon: +24 max health, +3 armor toughness |
| 2 | Martial King Hercules: +8 attack damage, +10% movement speed |
| 3 | Wisdom King Raphael: +6 armor, +0.5 attack speed, +2 luck |
| 0 | no boon, only the loot (for someone who already has theirs) |

The flag is the player's own NBT key `hasReceivedVeldanavaBoon` (the guide's name). The orb gives up after the timer and drops everybody's loot, targeted to its owner (extended lifetime, invulnerable).

Loot per fighter: 1 **Star King Dragon Core**, 3 **Genesis Fragment**, 1 **Creator's Essence** (guaranteed); 15% one of **Justice King Michael / Covenant King Uriel / Nihility Fragment**; 1% **Veldanava's Divine Core**.

## The anime Veldanava (3.30.0, replaces the 3.11.0 numbers; `classicMode = true` brings them back)

Rebuilt from the "Boss Entity Implementation Guidelines" the owner uploaded. Source of each entry: the guide itself (owner's file); the names of the six moves are Tensura names (Absolute Severance, Black Lightning, Death Tornado, Disintegration, Hell Flare, Megiddo). No wiki page was fetched for this change; the move behaviour is mod-owned and themed on those names, not copied from a Tensura asset.

**NEW**
* **Stats** (`VeldanavaStats`): 5,000,000 health, attack 80 (one blow kills an unarmoured player, hurts a fully armoured one badly), speed 0.55 (a flash step), armor 30, toughness 20, knockback resistance 1.0, follow range 96.
* **Health above 1024** (`HealthCap`): vanilla clamps MAX_HEALTH to 1024; the cap of that one attribute is lifted once by reflection (both sides). If a runtime refuses, the boss falls back to a damage divisor (`VeldanavaStats.healthDivisor`) so the fight is the same size, and the bar still shows the whole pool. Percent-of-max-health skills of players hit the real maximum when the cap is lifted; in the fallback they are weaker.
* **Nullifications** (anime mode): fall, every kind of fire and lava, and wither damage are refused; poison, wither, slowness and every other potion/status effect cannot be applied, and he is fire-immune.
* **Ultraspeed Regeneration** (`VeldanavaStats.regenStep`): while he fights he heals 0.1% of his maximum per second (server setting `regenPercentPerSecond`, 0 = off), in one small step every 10 ticks, through the DPS budget bypass. A hit of the counter-element (damage type tag `nusmp:veldanava_counter`: `conceptual_severance` and, when present, `minecraft:out_of_world`) stops it for 6 seconds. Add damage types to the tag with a data pack to widen the counter.
* **Boss bar per form** (`VeldanavaBar`): dragon = teal (blue, 10 notches), Creator God = white and gold (12 notches), Dark Avatar = violet and crimson (20 notches); the title pulses between two colours, flashes white while he changes body, turns yellow while exposed or guarding and red while the regeneration is broken; the line under it names the state or the running move; the health is shown as "4,999,200 / 5,000,000". Refreshed every 5 ticks, a player only gets a packet when something shown changed.
* **Six moves** (appended to the table, ids 11..17, each used only by the bodies named in its table row): **Absolute Severance** (long thin lines of severing light; hits only along the lines), **Black Lightning** (up to 3 bolts at players), **Black Lightning Blast** (a big black-lightning burst with a wide ring), **Death Tornado** (a tornado that drifts toward the target), **Disintegration** (a ground circle whose ripples erode everything standing on it), **Hell Flare** (expanding rings of hell fire, a safe gap between the rings), **Megiddo** (second ultimate: a pillar of light on a marked spot, leaves him exposed). Each has a windup telegraph, a damage share like the older moves (`VeldanavaRules.damage`, the same caps), and its own visual effect (`VELD_*` shapes, `vfx/client/layer/Veld*Layer.java`, previews `tools/vfx_preview/scenes.d/veld_*.json`).

**REPLACED** (only outside `classicMode`): the 4000 health, attack 18, speed 0.36, armor 12 / toughness 6 and follow 64 of 3.11.0; the old hard-control resistance list (now every effect is refused).

**KEPT**: every old move, form, model, loot item and boon; `bossHealth` is still read in classic mode. Config additions: `classicMode` (false), `healthTotal` (5,000,000, 100..1e9) and `regenPercentPerSecond` (0.1, 0..5). Old servers with `bossHealth = 4000` get the new health from `healthTotal` without touching the file.

**Worst-case cost of the change.** Entities: still 1 boss per world, no entities added by any new move (the effects are packets). Server work per tick: bolts <= 3, beams <= 8 per cast, still walking only the player list, `MAX_TARGETS` 24; regeneration one `heal` every 10 ticks; effects cleared once a second; the bar looked at every 5 ticks. Packets: all new effect packets go through `fxBudget()` (<= 12 per tick for the boss and the global budget), nearby players only; the bar sends at most 4 small packets a second per viewer. Client: each new layer <= 400 vertices per activation (peaks: severance 336, lightning 312, blast 392, hell flare 364, disintegration 300, megiddo 252, tornado 324), thinned by distance. `HealthCap` is a one-time reflection.

**Caveats.** Not seen in a running game (previews and the CI smoke test only). 5,000,000 health is practically unkillable at ordinary damage (the DPS budget lets 300,000 a second through at most); lower `healthTotal` if that is too hard. The move effects use existing textures; no Tensura model or animation asset was added.

## Worst-case cost (CLAUDE.md rule 11)

| What | Bound |
|---|---|
| Entities | 1 boss per world, 1 orb after he falls; loot at most 8 fighters x 5 stacks = 40 item entities, once |
| Server work per tick | targets hurt <= 24 (`MAX_TARGETS`, walks the player list only: no entity or chunk scan), falling stars <= 12 per cast, blocks changed <= 24 and only with `griefBlocks`, ledger bounded and restored in 160-block steps |
| Packets | boss effects <= 12 per tick (`FX_PER_TICK`) and every one also passes the global `PerfBudgets.effectPacket` budget (160 per tick by default) and is sent to nearby players only; one boss-bar update per player in range |
| Per-player state | keyed by UUID (fighters, soul-seized, boons); cleared on logout/death/dimension change and when the fight ends; the hit table is capped at 128 |
| Client | 3 GeoLite models drawn in one pass plus one glow pass; dragon <= 1784 quads per pass; culled beyond 220 blocks and by a grown frustum box; no per-frame allocation, textures resolved once |
| Config | server config only; the visual effects reuse existing client settings |

Pinned by `VeldanavaRulesTest`, `VeldanavaBoonsTest`, `VeldanavaSourceScanTest` and the smoke self-tests `veldanava.rules`, `veldanava.assets`, `veldanava.boss`, `veldanava.orb`.

## Files

* Models and animation: `tools/gen_veldanava.py` -> `assets/nusmp/{geo,animations,textures}/entity/veldanava_<humanoid|dark_avatar|true_dragon>.*` (+ `_glow`), preview `python3 tools/gen_veldanava.py --preview <scene>`.
* Items and lang: `tools/gen_veldanava_items.py` (icons, item models, `tools/lang/veldanava.json` merged into `en_us.json`; `--sheet` writes a preview).
* Server: `entity/veldanava/` (`VeldanavaBoss`, `VeldanavaMoves`, `VeldanavaRules`, `VeldanavaSoul`, `VeldanavaBoons`, `WillOfTheCreator`, `VeldanavaCommand`, `VeldanavaEntities`, `VeldanavaConfig`, `VeldanavaEvents`, `VeldanavaSelfTests`).
* Client: `client/veldanava/` (`VeldanavaClient`, `VeldanavaRenderer`, `VeldanavaOrbRenderer`).

## What the guide asks for that is not built

* **Aura of the Creator** and **Nodens' sword** (the guide's crafted items): only their materials drop; no recipe or item exists yet.
* The Dragon Core socketing, the Genesis anvil upgrade and the Divine Core's off-hand negation are described on the items' cards, but the hooks belong to the weapon / ultimate-spell systems and are not wired.
* The boon choice is three clickable chat lines, not a GUI screen.
* Seen only in previews and the CI server smoke test; nothing of the fight has been seen in a running game.
