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
