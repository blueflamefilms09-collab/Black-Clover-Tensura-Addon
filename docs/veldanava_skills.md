# Veldanava's ultimate skills (3.13.0 onward): Sandalphon, Lord of Judgment, Israfil, Lord of Trials (3.14.0) and Jibril, Lord of Rigor (3.15.0)

Source of the design: the owner's `veldanava_skill_drops.txt` (five skills, ten modes each). The reference jar `tensuramoreskills-0.0.4.9.3.jar` (All Rights Reserved) was read for ideas only; no code and no asset of it is used.
Wiki sources: none needed (the numbers come from the owner's file); nothing was fetched.

## What changed in how the Will of the Creator pays out

| | Before (3.11) | Now (3.13) |
|---|---|---|
| Reward | one of three attribute boons, picked in chat | **one of five ultimate skills**, picked on a card screen, once in a player's life |
| Boons | offered | **kept in the code and still working**, but not offered while `rewardScreen = true` (set it to false to get the 3.11 chat offer back) |
| Loot | core, fragments, essence | unchanged; the loot-only button of the screen still gives it |

This release gives **Sandalphon**. The other four cards (Israfil, Jibril, Solomon, Hercules) are on the screen but sealed; the server refuses a claim for them. They open one per release (the order of the owner's file).
Anyone who already took a boon in 3.11 can still take one skill (the boon stays; the skill flag is separate).

## How it is read in game

- Take **Sandalphon** from the Will of the Creator card screen (or `/veldanava skill sandalphon` while an orb waits for you, operators can use it too).
- It is a normal ultimate skill: bind it, cycle its modes with the mode key, press to use. 10 modes:

| # | Mode | Use | Cost (normal / mastered) |
|---|---|---|---|
| 0 | Executioner's Authority | toggle | free |
| 1 | Absolute Severance | toggle | free |
| 2 | Inevitable Demise | toggle | free |
| 3 | Judgment Gaze | toggle | free |
| 4 | Judgment Bullet | press | 2.5K / 1.8K MP |
| 5 | Condemnation | crouch + press | 4.2K / 3.1K MP |
| 6 | Supreme Edict | press | 5.5K / 4.0K MP |
| 7 | Guillotine Cleave | crouch + press | 3.8K / 2.5K MP |
| 8 | Verdict of Ruin | press | 8.0K / 5.5K MP |
| 9 | Final Judgment | held (3 s), **mastery only** | 45K MP, 120 s cooldown |

Costs are paid in Magicules through the Tensura bridge (the skill's own cost is 0, so the game's mode cost never double-charges). The server setting `costScale` multiplies all of them. Creative players pay nothing.

### The passives (modes 0-3, toggles; each stays on until toggled off)
- **Executioner's Authority**: 20% critical hits (x1.5); bonus true damage worth 0.35 per armour point of the target (at most 12); +1 Temporal Debt per hit; a bow draws a third faster; hostile creatures within 24 blocks show through walls (the 12 nearest, once a second). **Mastered:** a hit seals the target's teleporting for 2 s.
- **Absolute Severance**: your hits ignore armour, enchantment and potion-effect reduction, a shield does not block them, and the target cannot heal for 3 s.
- **Inevitable Demise**: your hits do x1 (full health) to x2 (no health left) damage. **Mastered:** a target at or below 15% health stutters and cannot heal for 2 s.
- **Judgment Gaze**: once a second, whoever looks straight at you within 24 blocks (the 16 nearest, with line of sight) is Action-Locked for 1.5 s and owes 2 Temporal Debt.

### The actives (modes 4-8)
- **Judgment Bullet**: instant, pierces blocks and up to 8 creatures along 64 blocks; true damage (base 80, scaled like every addon spell, Balance Law capped); strips up to 16 good effects, Temporal Stutter 2 s, no healing for 5 s, +10 debt.
- **Condemnation**: marks the creature in your sight (48 blocks) with your **Judgment Sigil** for 30 s (one sigil per player: the old one goes), +25 debt; a marked creature that falls to or below 20% health is executed; one already at or below 20% is executed at once.
- **Supreme Edict**: every foe within 30 blocks (the 32 nearest) loses up to 16 good effects; every **Judgment** field of another owner within the sphere ends.
- **Guillotine Cleave**: a 130-degree sweep, reach 5.5, up to 12 creatures: true damage (base 90), Future Rejection 3 s (no healing, no teleport, no hit frames), +5 debt.
- **Verdict of Ruin**: a field (radius 8, 12 s) that follows the creature in your sight (40 blocks) or stands where you aim; every 0.5 s each foe inside (at most 24) is silenced, hindered, and takes 1.5% of its maximum health as true damage.
- **Final Judgment** (held, mastered): hold on a target in sight (80 blocks) for 3 s; the target is frozen in time while you hold (a boss only stutters); then the **Temporal Debt** it carries detonates for debt x 20 x 5 (at least 200, at most `finalJudgmentCap`). Letting go early costs nothing. The 45K MP is paid at the moment it fires, and so is the 120 s cooldown.

### The conditions it puts on creatures (shown as normal potion icons)
Temporal Stutter, Temporal Debt (stacks, 30 s from the last stack, +1% magic damage taken a stack up to +50%), Event Silence (no skills, no teleport; also seals grimoire casts), Future Rejection, Movement Interference (no teleport), Action Lock (no attack, use, interact or skill; a mob drops its target). Hard control is passed through the Balance Law's control limits, so bosses and players are not locked for long.

## Server settings (`nusmp-veldanava-skills-server.toml`, SERVER config)
`enabled` (false = the skill does nothing), `rewardScreen` (false = the 3.11 boon offer), `costScale`, `debtDamage` (20), `finalJudgmentCap` (50000), `activeDamageCap` (4000), `executePlayers` (**false**: a player takes a Balance-Law-limited hit instead of dying), `executeBosses` (**false**: a boss takes 10% of its maximum health instead), `debtMax` (200), `fieldsPerPlayer` (2), `fieldsTotal` (24).

## Multiplayer and server rules (CLAUDE.md rule 9)
- Everything is decided on the server; the client only opens the card screen and sends one tiny claim packet.
- The claim packet carries one string read with a 24-character limit; the sender must be a server player with an orb waiting for them; the card id must be claimable in the rules table. Nothing else is believed.
- Per-player state is keyed by UUID (fields, sigils, toggles); nothing is a static "current player".
- The Judgment mask is one int on the player; event handlers read that int and return on their first line for anyone without the skill or the conditions.

## Cost and lag bounds (CLAUDE.md rule 11)
- **Per press**: one box query; at most 8 (bullet) / 32 (edict) / 12 (cleave) creatures touched; actives are 8 ticks apart per player, so at most 2.5 presses a second per player.
- **Per tick**: one hook, the field manager, which returns on its first line when no field exists. Worst case 24 fields (global cap), each pulsing twice a second with one bounding-box query touching at most 24 creatures: **at most 12 box queries and 288 hits per second server-wide**, on the `s.veldanava.fields` perf probe. A field lasts at most 12 s; it ends when its owner dies, logs out or changes dimension, when its target dies, and when the server stops.
- **Per second per player**: the two scanning passives run once a second (Presence: 12 targets; Gaze: 16 targets).
- **Network**: one effect packet per press, through `VfxSpawn` (nearby players only, the effect-packet budget); the Verdict field and the Sigil are one packet for their whole life (the client draws them). Worst case 2.5 packets a second per player, each to the players near. The Final Judgment cross is one packet. Card screen: 2 packets in a player's life.
- **Client draw**: see the VFX section below.
- **No** terrain damage, no chunk loads, no entity spawns, no block changes, no food-bar writes.

## VFX
Six shapes, appended at the end of `VfxShape` (ordinals 453-458; nothing moved), drawn by one new client layer `vfx/client/layer/JudgmentLayer.java` with 14 new textures (`textures/particle/judgment_*.png`, made by `tools/gen_judgment_vfx_textures.py`). They are our own work from scratch (pale gold, white and a hairline of black); the Time Magic VFX is not touched and not copied. Previews: `python tools/vfx_preview/preview.py judgment_bullet judgment_sigil judgment_edict judgment_cleave judgment_field judgment_cross`.

| Effect | Used by | Lasts | Worst case (quads / vertices) |
|---|---|---|---|
| JUDGMENT_BULLET | Judgment Bullet: needle, muzzle flash, impact ring, thin black crack trail | 12 ticks | 27 / 108 (33 / 132 on a 64-block line) |
| JUDGMENT_SIGIL | Condemnation: seal, ticking clock ring, hovering blade glyph on the target | up to 30 s, follows the target | 17 / 68 |
| JUDGMENT_EDICT | Supreme Edict: flat wave with glyph bands and ground cracks | 24 ticks | 73 / 292 |
| JUDGMENT_CLEAVE | Guillotine Cleave: crescent with a black core | 10 ticks | 78 / 312 |
| JUDGMENT_FIELD | Verdict of Ruin: ground field, follows its target | up to 12 s | 40 / 160 |
| JUDGMENT_CROSS | Final Judgment: charge, pillar, cross, flash, rings, cracks | 70 ticks | 55 / 220 |

The engine caps every effect activation at 100 quads / 400 vertices on top of that, and the layer goes through the normal `VfxManager` distance cull and LOD; it keeps nothing between frames (nothing to clean up on logout) and allocates nothing per frame beyond the scratch vectors the other layers use.
Known soft spots in the previews: the bullet's black trail is thin on a long shot, the cleave looks edge-on from the sides, the edict's rising motes are sparse, the cross bar is a little thinner than its pillar.

## Tests
`JudgmentRulesTest` (13 pure checks of the numbers and geometry), `JudgmentSourceScanTest` (server-safety, bounds, validated packet, SERVER config, assets and lang keys, smoke prefix, Time Magic untouched, boons kept), CI smoke prefix `sandalphon` (`sandalphon.rules`, `sandalphon.skill`, `sandalphon.conditions`) and the rewritten `veldanava.orb`.

## NEW / REPLACED / KEPT
- **NEW**: the Sandalphon skill and its 10 modes, six conditions, the Judgment VFX, the card screen and claim packet, `/veldanava skill`, the server config file, the tests.
- **REPLACED**: what the Will of the Creator *offers* (the three boons in chat become the five-card screen) - behind `rewardScreen`.
- **KEPT**: the three attribute boons and their commands/tags/tests (not offered, still working with `rewardScreen = false`), the loot, the orb, the Time Magic VFX (untouched).

## Not included / honest limits
- Israfil, Jibril, Solomon, Hercules (sealed cards, one per release). The Nihility Collapse item comes with Israfil.
- No "absolute accuracy" (a bullet is instant and does not miss, but no aim assist). Presence only highlights hostile mobs, not players. Hotbar swapping is not locked by Action Lock.
- Edict ends only Judgment fields of other owners, not Tensura Mana Zones.
- Not seen in a real game session: the CI smoke test proves the registry, the grant, the conditions and the caps on a real server; the visuals were checked in the preview harness, not in a running client.

---

# 3.14.0 - cooldowns by mastery, the shared base, and Israfil, Lord of Trials

Source of the design: the owner's `veldanava_skill_drops.txt` (Israfil section). Wiki sources: none needed (the numbers come from the owner's file); nothing was fetched.

## Owner decisions applied in 3.14.0
1. **Final Judgment may execute players, never bosses**: the server setting `executePlayers` now defaults to **true**, `executeBosses` stays **false**. (A server that already has a `nusmp-veldanava-skills-server.toml` keeps the value it was written with; delete the line to get the new default.)
2. **Players who already took a boon may still pick one skill**: unchanged (the boon flag and the skill flag are separate; a skill can be taken once).
3. **Every active has a cooldown that falls with mastery.** Sandalphon's five actives now cool down (normal / mastered seconds): Judgment Bullet 6 / 3, Condemnation 20 / 12, Supreme Edict 30 / 20, Guillotine Cleave 8 / 4, Verdict of Ruin 40 / 25; Final Judgment stays 120. The new server setting `cooldownScale` (0-10, default 1) multiplies every cooldown of every Veldanava skill (0 = none). The mastered column applies once the skill is mastered. The cooldown is shown in the mode name and survives a relog (it lives in the skill instance).
4. **Israfil together with the Nihility Collapse**: the owner's file has no separate item, so Nihility Collapse is built as the **damage field and its condition** of Spatial Collapse (see below). If an item was meant, tell me what it should do.

## The shared base (internal, no gameplay change for Sandalphon)
`VeldanavaSkillBase` holds what all five skills share: the mode table, the toggles (kept in the skill instance, copied once a second into the player's mask), the crouch rule, the cost (through the Tensura bridge, refunded when an active refuses), the cooldown, the 8-tick gap between actives, the mastery-only tenth mode and the mastery point. `SandalphonSkill` now extends it. The player mask carries the skill's **code** in bits 5-7 (1 Sandalphon, 2 Israfil, 3 Jibril, 4 Solomon, 5 Hercules; a mask written before 3.14.0 has code 0 and is read as Sandalphon's), so one skill's toggles can never switch another skill's passives on. `JudgmentFields` now takes a pluggable behaviour per field (the Verdict is the first; Israfil's cage and Nihility field are the next two) with a kind label, a hard life cap of 60 s, and the same per-player and global caps.

## Israfil, Lord of Trials: how it is read in game
Take **Israfil** from the Will of the Creator card screen (`/veldanava skill israfil` while an orb waits). Ten modes:

| # | Mode | Use | Cost (normal / mastered) | Cooldown (normal / mastered) |
|---|---|---|---|---|
| 0 | Spatial Domination | toggle | free | - |
| 1 | Trial of Endurance | toggle | free | - |
| 2 | Labyrinthian Mind | toggle | free | - |
| 3 | Weight of Consequence | toggle | free | - |
| 4 | Dimensional Labyrinth | press | 6.5K / 4.8K MP | 25 / 15 s |
| 5 | Weight of Sins | crouch + press | 4.0K / 2.9K MP | 15 / 9 s |
| 6 | Spatial Collapse | press | 9.5K / 7.2K MP | 35 / 22 s |
| 7 | Coordinate Shift | crouch + press | 3.0K / 1.5K MP | 10 / 5 s |
| 8 | Rift Walk | held, up to 4 s (8 s mastered) | 1.2K / 0.8K MP **per second** | 20 / 12 s |
| 9 | The Grand Trumpet | press, **mastery only** | 60K MP | 180 s |

### The passives (toggles)
- **Spatial Domination**: +30% movement speed; no fall damage; a melee blow aimed at you is dodged by a blink to a free spot 3-7 blocks away (at most once every 6 s); nothing can pull, swap, cage or throw you (the checks are inside this mod's own pulls, swaps and cages, and Movement Interference and Gravity Crush do not land on you). **Mastered:** a physical blow that still lands does half damage, and a projectile about to hit you is bent away (once per projectile). *Limit:* it cannot stop another mod's own teleport or knockback; it covers this mod's effects and the vanilla fall.
- **Trial of Endurance**: 35% of every blow you take is stored (at most 120 points, draining 2 a second). Each stored point gives +0.5 maximum health (at most +40) and +0.1 armour toughness (at most +8) for as long as it lasts, and whoever struck you gains Temporal Debt (1 per 10 damage, 1 to 6). Death or turning it off empties the store.
- **Labyrinthian Mind**: no mind-altering effect lands on you (any effect whose id names charm, fear, confusion, nausea, hypnosis, domination, mind, control, sleep, terror, madness, taming or possession); Sandalphon's presence scan skips you.
- **Weight of Consequence**: once a second, every foe within 20 blocks that is **moving** (the 16 nearest) gains +1 Temporal Debt and Slowness for 2 s.

### The actives
- **Dimensional Labyrinth**: the creature in your sight (40 blocks) is pulled out of the world into a pocket dimension, an ever-shifting maze (see "The pocket dimension" below); mastered, up to two creatures within 6 blocks of it come too. A boss is never pulled in: it is caged in the old way for 8 s (a virtual ring of radius 4.5, held inside, confused, Action-Locked and Movement-Interfered for the time the Balance Law allows, 1% of maximum health a second, +5 debt, no block placed). A target with Spatial Domination slips out of both.
- **Weight of Sins**: heavy true damage to the creature in sight (40 blocks): 60 + 8 per Temporal Debt stack + 0.4 per mob the target has killed (players only, at most +80), then **Gravity Crush** (falls fast, cannot fly, -20% speed) for 10 minutes (10 s on a boss); death or a milk bucket clears it. +8 debt.
- **Spatial Collapse**: the place under the target (or where you aim) implodes: up to 16 creatures within 6.5 blocks take 140 true damage and are pulled in, then a **Nihility Collapse field** stands for 6 s: every half second up to 24 foes inside carry the Nihility Collapse condition (slower, **no healing**) and lose 2% of maximum health.
- **Coordinate Shift**: swap places with the creature in your sight (48 blocks) or, if none, the nearest projectile (32 blocks). Every mob within 32 blocks (the 24 nearest) that was after you forgets you. Refuses a target that cannot be moved.
- **Rift Walk** (held): you phase out and walk through blocks for up to 4 s (8 s mastered), paying per second. Letting go, running out of Magicule, time up, death, logging out and a server stop all end it, and you are put on a free spot (straight up, then down, then where you began) with a short slow fall. Refused in creative, in spectator, on a mount and inside the Labyrinth (the Dungeon's stone is magic). Internally this is the spectator game mode, whose previous mode is saved on the player and restored even after a crash on the next login.
- **The Grand Trumpet** (mastery): every creature within 100 blocks (the 48 nearest) is silenced for 10 s (Event Silence), every skill of an enemy player gets a cooldown of **at least 60 s** ("reset to maximum" is capped at a minute so nobody is locked for long), and every field of another owner within 100 blocks ends. *Limit:* it does not break real barrier blocks (there are none of this skill's; Magic Barrier from other sources is untouched).

### Conditions added
Gravity Crush, Nihility Collapse (potion icons). Event Silence, Action Lock and Movement Interference from 3.13.0 are reused.

## Server settings added
`cooldownScale` (default 1.0, range 0-10). `executePlayers` default changed to true.

## Multiplayer and server rules (CLAUDE.md rule 9)
Same as Sandalphon: every decision is on the server; the passives read the player's mask int and return on the first line for anyone else; per-player state is on the player (store, blink time, saved game mode) or in UUID-keyed maps (fields, rift sessions); nothing is a static current player; no new packet except the effect packets through `VfxSpawn`.

## Cost and lag bounds (CLAUDE.md rule 11) for Israfil
- **Per press** (actives are 8 ticks apart, and every one has a cooldown of 5 s or more): Labyrinth 1 line query (+ one box query of at most 2 creatures mastered) and one maze; Weight of Sins 1 query + 1 hit; Spatial Collapse 1 query + at most 16 hits + 1 field; Coordinate Shift 1 query + at most 8 projectiles + one box query of at most 24 mobs; Rift Walk 1 session; Grand Trumpet 1 box query (100 blocks), at most 48 creatures touched, at most 48 players' skills walked once (16 modes each) and one field sweep.
- **Per second per owner**: Spatial Domination and Trial of Endurance refresh 2 attribute modifiers (only written when the value changes); Weight of Consequence touches at most 16 creatures with one box query.
- **Per tick**: the field manager (does nothing when empty; global cap 24 fields, each field pulsing at most 4 times a second; the cage is one creature, the Nihility field at most 24) and the Rift Walk manager, which returns on its first line when nobody walks and otherwise does one comparison per walker (a server holds a handful at most) plus one Magicule charge per walker per second.
- **Events**: seven handlers (fall, incoming damage, post damage, effect, projectile impact, heal, and the housekeeping events); each returns on its first line for anyone who is not an owner of Israfil or does not carry the condition.
- **Network**: one effect packet per press through `VfxSpawn` (nearby players only, the effect-packet budget), plus one small packet a second per Rift Walker. No new payloads.
- **No** terrain damage, no chunk loads, no entity spawns, no block changes, no food-bar writes.

## VFX (Trial layer)
Six shapes, appended at the end of `VfxShape` after `JUDGMENT_CROSS` (nothing moved; the order goes over the network), drawn by one new client layer `vfx/client/layer/TrialLayer.java` with 18 new textures (`textures/particle/trial_*.png`, made by `tools/gen_trial_vfx_textures.py`). They are our own work from scratch (deep indigo, midnight teal, cyan-white edges, a little brass; maze lines, coordinate ticks, folded-space shards, a trumpet and sound rings); the packet colour is ignored. The Time Magic VFX is not touched and not copied. Calls: `docs/vfx_remake/trial_fx_calls.md`. Previews: `python tools/vfx_preview/preview.py trial_labyrinth trial_gravity trial_collapse trial_shift trial_trumpet trial_rift`.

| Effect | Used by | Lasts | Worst case quads (cap 100) |
|---|---|---|---|
| TRIAL_LABYRINTH | Dimensional Labyrinth: a ring of maze panes that reshuffles every second, a maze floor, drifting cubes | 160 ticks | 43 |
| TRIAL_GRAVITY | Weight of Sins: a coordinate plate pressing down on a dark disc, streaks and chains sinking, ground cracks, a thud ring | 40 ticks | 31 |
| TRIAL_COLLAPSE | Spatial Collapse: shards and rings pulled in, a pinch flash and shock ring, then the lingering Nihility field | 120 ticks | 35 |
| TRIAL_SHIFT | Coordinate Shift and the natural blink: flipping coordinate rings at both ends, a dashed folded line, swapped afterimages | 14 ticks | 35 (48-block shift) |
| TRIAL_TRUMPET | The Grand Trumpet: a horn of light, rays, sound rings out to the radius, shattering glass, a screen shake | 50 ticks | 62 |
| TRIAL_RIFT | Rift Walk: ghostly rift lines, feet rings and afterimages on the walker | 26 ticks, re-sent each second | 27 |

The layer keeps nothing between frames (nothing to clean up on logout, death or dimension change), allocates nothing per frame beyond a few scratch vectors, thins its counts with distance and the particle setting, and goes through the normal `VfxManager` distance cull and LOD. The screen shakes use `VfxShake`, which falls off to nothing at 32 blocks.
Known soft spots in the previews (nothing was seen in a real game): additive glow looks stronger on the preview's dark sky than on a daytime world, the shift and rift afterimages are soft light-blue and may wash out in bright daylight, the rift lines jitter slightly at each re-send, the Collapse sphere assumes the server sends a ground point, and the rift trail direction (the walker's body yaw) could not be checked in the preview.

## Tests
`JudgmentRulesTest` (extended: skill codes, spec list, cooldowns), `JudgmentSourceScanTest` (extended: Israfil's caps, Rift Walk manager, `TrialFx` one shape per packet, config keys, assets, lang, smoke prefix), `TrialShapesTest` (agent), CI smoke prefix `israfil` (`israfil.rules`, `israfil.skill`, `israfil.conditions`), and the `veldanava.orb` test (a still-sealed card is now Jibril).

## NEW / REPLACED / KEPT
- **NEW**: Israfil and its 10 modes, Gravity Crush and Nihility Collapse conditions, the Trial VFX, the shared `VeldanavaSkillBase`, the skill code in the mask, pluggable field behaviours, the `cooldownScale` setting, the Israfil icon, tests.
- **REPLACED**: Sandalphon's actives had no cooldown and now have one that falls with mastery (owner request); `executePlayers` default false -> true (owner request, fresh configs only).
- **KEPT**: everything else of 3.13.0 (the numbers, the VFX, the card screen, the boons, the orb, the loot). The Time Magic VFX is untouched.

## Not included / honest limits
- Jibril, Solomon, Hercules (sealed; 3.15.0, 3.16.0, 3.17.0).
- The Trumpet ends other Veldanava fields and other casters' labyrinths, not real barrier blocks. The pocket maze has not been walked in a real client (the CI test paints, shifts and removes it block by block).
- Spatial Domination cannot stop another mod's own teleports or knockback.
- Not seen in a real game session: the CI smoke test proves the registry, grant, state and caps on a real server; the visuals were checked in the preview harness only. Rift Walk's spectator hand-over has not been played with two clients.

## The pocket dimension (Dimensional Labyrinth, 3.14.0)

Code: `entity/veldanava/pocket/PocketMaze` (pure: the grid, the walls, the shift) and `PocketLabyrinth` (the world: the dimension `nusmp:trial`, sessions, events). Data: `dimension/trial.json`, `dimension_type/trial.json` (a void, roofed, no sky, fixed night, 128 high). Server setting `pocketDimension` (default on; off = the old cage).

- **The maze**: 9 x 9 cells (11 x 11 mastered), a corridor 3 blocks wide and 4 high, walls and ceiling of black stone, floor of checkered deepslate with a sea lantern in every fourth cell (dim on purpose). It starts as a random perfect maze (depth-first, one way between two cells).
- **The shift**: every 60 ticks (36 mastered) a closed wall is picked; opening it makes one loop; one wall of that loop closes. The flash `TRIAL_SHIFT` runs between the two walls 16 ticks before they change. A wall is never closed while anyone stands in it or within a body's width of it (then that shift is skipped). The maze therefore stays one connected maze with a single way between two cells, whatever happens (`PocketMazeTest` runs 2,000 shifts and checks it).
- **The exit**: a 3 x 3 patch of crying obsidian in one cell; it moves to a random cell every third shift. Standing on it ends the stay at once.
- **The stay**: a player 400 ticks (600 mastered), a mob 600 (900); 1% of maximum health a second; +5 Temporal Debt. Then, or when the caster dies, logs out, forgets the skill or is hit by another Grand Trumpet, everyone is put back where they stood (same dimension, same spot; a different dimension too). A creature that has left cannot be pulled in again for 30 s.
- **Rules inside**: no block can be broken, placed or blown up; ender pearls and chorus fruit do nothing; the maze takes a creature that falls or is thrown out of it back to the start; a player who logs out inside is sent back at the next login; a server stop puts everyone back and takes the maze down.
- **Cost (rule 11)**: at most 8 mazes on a server (one per caster, 3 creatures each, 9 forced chunks each at most). Open and close: about 12,000 block reads and 6,500 writes once each (flags 2|16, no neighbour updates). While open: 24 block writes and 2 effect packets per shift (a wall every 1.8-3 s), a look at each member 5 times a second, one hit and one action-bar line a second. With no maze open: one empty-map check per tick.

---

# 3.15.0 - Jibril, Lord of Rigor

Source of the design: the owner's Jibril spec (ten modes). Wiki sources: none needed (the numbers come from the owner's file); nothing was fetched.

## How it is read in game
Take **Jibril** from the Will of the Creator card screen (`/veldanava skill jibril` while an orb waits). Code: `JibrilRules` (numbers, pure), `JibrilSkill` (modes), `JibrilEvents` (passives that live in the game's events), `JibrilState` (wings, hotbar holds, sanctuary registry, parry, steal, cleanse), `JibrilFx` (effect packets).

| # | Mode | Use | Cost (normal / mastered) | Cooldown (normal / mastered) |
|---|---|---|---|---|
| 0 | Divine Rigor | toggle | free | - |
| 1 | Heavenly Presence | toggle | free | - |
| 2 | Strict Governance | toggle | free | - |
| 3 | Wings of Revelation | toggle | free | - |
| 4 | Absolute Command | press | 5.0K / 3.5K MP | 25 / 15 s |
| 5 | Judgment of Heaven | crouch + press | 8.5K / 6.0K MP | 30 / 20 s |
| 6 | Heavenly Purge | press | 7.0K / 5.0K MP | 40 / 25 s |
| 7 | Holy Shackles | crouch + press | 4.5K / 3.0K MP | 20 / 12 s |
| 8 | Word of Law | press | 6.0K / 4.0K MP | 18 / 10 s |
| 9 | Kingdom of God | held, up to 60 s, **mastery only** | 4.0K / 2.5K MP **per second** | 90 / 60 s |

### Passives
- **Divine Rigor**: no effect of the harmful category lands on you (`MobEffectEvent.Applicable` is denied), the ones you carry are lifted once a second (8 at most). This includes the Veldanava conditions (Temporal Debt, Event Silence, Action Lock, ...). Mastered: a registry of Jibrils (one entry per owner, valid 40 ticks) lets an ally within 16 blocks take every debuff at half length (the half-length effect is re-applied through the same event, guarded by a flag).
- **Heavenly Presence**: once a second up to 12 creatures within 16 blocks that are undead (the vanilla undead tag), carry Temporal Debt, or have a demon word in their type id (word-matched, so "chimp" is not a demon) take holy (magic) damage: 14 + 0.5 per debt stack, the debt part capped at 20; the Balance Law applies.
- **Strict Governance** (needs `strictGovernance`): once a second, every other game-second, every running cooldown of up to 24 skills x 16 modes of each of up to 16 enemy players within 30 blocks gets +1 s, so their cooldowns run at half speed. At most once a second per target whatever the number of Jibrils. Mobs and allies are never touched.
- **Wings of Revelation** (needs `wingsFlight` for the flight): `mayfly` and fly speed 0.16 are given while on and taken back when off (only if this skill gave them; creative and spectator players are left alone; settled at login). A blow with a direct source, an explosion or a projectile taken while airborne is dodged with 70% (85% mastered); an ability's true hit and bypass-invulnerability damage are never dodged. Drawn every 80 ticks while airborne.

### Actives
- **Absolute Command**: creature in sight (40 blocks): `BalanceLaw.controlTicks` (100 ticks base; caps 3 s player, 5 s mob, 1 s boss), Action Lock and Event Silence for that time; a player's hotbar is held on its slot (re-sent only if moved, at most 64 players, 200 ticks).
- **Judgment of Heaven**: creature in sight (64 blocks): 220 holy damage (capped by `activeDamageCap`, Balance Law), good effects stripped, 50% to up to 6 creatures within 3 blocks of it.
- **Heavenly Purge**: within 50 blocks: up to 64 `AreaEffectCloud` removed, other owners' Veldanava fields ended, pocket mazes near ended, you and up to 24 allied players cleansed (8 effects each), fire put out. Never scans or changes blocks.
- **Holy Shackles**: creature in sight (40 blocks): Movement Interference (120 ticks base) and up to 8 good effects moved to you (each at most 5 minutes; an endless one becomes 5 minutes).
- **Word of Law**: opens the parry window on you (12 ticks, 20 mastered). The first blow with a living source is cancelled and returns as 300% raw damage to the attacker (capped by `activeDamageCap` and the Balance Law). Never parries true damage or bypass-invulnerability sources.
- **Kingdom of God** (held): one field of kind `kingdom` following you (50 blocks, at most 1,200 ticks, no renewal). Each second it pays the per-second cost (or ends), refreshes the sanctuary registry, strips 4 good effects and applies Gravity Crush for 30 ticks to up to 24 enemies. Inside it you and allies cannot be reduced below 1 HP by a blow (enforced at the lowest priority of the incoming and pre-damage events). Releasing the key, running out of Magicule, death, logout, dimension change and a server stop end it.

## Server settings added
`strictGovernance` (default true) and `wingsFlight` (default true), in `nusmp-veldanava-skills-server.toml` (SERVER config: they change gameplay).

## Multiplayer and server rules (CLAUDE.md rule 9)
Server decides everything; no client class is named (scanned by `JudgmentSourceScanTest`); per-player state is on the player or in UUID-keyed maps; the only effect path is `JibrilFx` -> `VfxSpawn`, one shape per packet; the only tick hook is the server tick that holds the hotbars; state is dropped at death, logout, login and server stop.

## Cost and lag bounds (CLAUDE.md rule 11) for Jibril
- **Per press**: Command 1 line query; Judgment 1 line query + 1 box query (7 hits); Purge 2 box queries (64 clouds, 24 players) + field sweep; Shackles 1 line query (8 effects); Word of Law none. All on cooldowns of 10 s or more.
- **Per second per owner**: one 30-block box query for Presence and Governance together (12 burned; 16 players x 24 skills x 16 modes walked at most), 8 cleansed, one ability refresh; Kingdom one box query, 24 enemies.
- **Per tick**: the field manager and the hotbar hold, which returns on its first line when empty.
- **Events**: effect, incoming damage (x2), pre-damage, death, logout, login; each returns on its first line for anyone who is not under Jibril.
- **Network**: one effect packet per press; Kingdom and wings one every 80 ticks; one tiny slot packet to a commanded player only when they move the slot.
- **Client draw**: at most 73 quads per effect since 3.16.0 (Command 73, Judgment 69, Purge 60, Shackles 70, Word 59, Reflect 58, Kingdom 41, Wings 45; engine cap 100).
- **No** chunk loads, block changes, entity spawns or food-bar writes.

## VFX (Jibril layer)
Eight shapes appended after `TRIAL_RIFT` (`JIBRIL_COMMAND`, `_JUDGMENT`, `_PURGE`, `_SHACKLES`, `_WORD`, `_REFLECT`, `_KINGDOM`, `_WINGS`), one client layer `vfx/client/layer/JibrilLayer.java`, 22 textures `textures/particle/jibril_*.png` from `tools/gen_jibril_vfx_textures.py`. **Since 3.16.0 the look is "dawn"**: pearl white, rose-gold and amber, azure to lavender, an iridescent rim; feather wings, nested halos, law banners, ribbons and scrolls, an open book under a crown of rays, a braided pillar with petals (3.15.0 had a white-gold look; replaced on the owner's request, the shapes and packets are unchanged). Worst-frame quads (cap 100): Command 73, Judgment 69, Purge 60, Shackles 70, Word 59, Reflect 58, Kingdom 41 (82 during a re-send overlap), Wings 45 (90). Nothing of the Time Magic layer, Sandalphon's or Israfil's is touched or copied. The layer keeps nothing between frames. Previews: `python tools/vfx_preview/preview.py` (scenes in `tools/vfx_preview/scenes.d/jibril.json`); calls and numbers in `docs/vfx_remake/jibril_fx_calls.md`.

## Tests
`JibrilRulesTest` (pure), `JudgmentSourceScanTest` (extended), `JibrilShapesTest`, smoke prefix `jibril` (`jibril.rules`, `jibril.skill`, `jibril.conditions`: registration, mask separation, wings, rigor, the parry event driven directly, holy hit, steal, hotbar lock, sanctuary registry, governance stretch, the Kingdom field cap and end).

## NEW / REPLACED / KEPT
- **NEW**: Jibril and its 10 modes, the Jibril VFX, two settings, the icon, tests.
- **REPLACED**: nothing (the card was sealed; it is open now).
- **KEPT**: everything of 3.14.0 and earlier.

## Not included / honest limits
- Solomon and Hercules stay sealed (3.16.0, 3.17.0).
- Not played in a real game. ManasCore cooldown units (seconds, counted down once a second) are assumed from the Israfil code. The Kingdom's floor does not stop `/kill` or the void. Divine Rigor blocks the Veldanava conditions too.

## Icons (3.17.0)
The five Ultimate skill icons are animated medallions (see `docs/releases/3.17.0.md`). Files per skill under `textures/skill/veldanava/`: `<id>.png` (32 px still, the path the skill names), `<id>_anim.png` (32 x 32N strip), `<id>_anim_hd.png` (128 x 128N strip), their `.mcmeta` (frametime 2), and for Sandalphon, Israfil and Jibril the old sprite `<id>_classic.png`. Made by `tools/gen_<id>_icon_anim.py` with `tools/icon_anim_lib.py` (`python tools/gen_<id>_icon_anim.py`, previews to `/tmp/claude-0/icons/<id>/`). In the game `client/veldanava/AnimatedIcons` registers an `AnimatedIconTexture` at the still's path, so every ManasCore list animates; the card screen uses the HD strips, only while it is open. Client setting `icons.ultimateStyle` = ANIMATED / STILL / CLASSIC. Client only, no server work. When Solomon and Hercules become real skills, their icon is already there (`VeldanavaSkillBase` names `textures/skill/veldanava/<id>.png`).

---

# 3.18.0 - Solomon, Lord of Sorcery

Source of the design: the owner's Solomon spec (ten modes, `docs/veldanava_spec/veldanava_skill_drops.txt`). Wiki sources: none needed (the numbers come from the owner's file); nothing was fetched.

## How it is read in game
Take **Solomon** from the Will of the Creator card screen (`/veldanava skill solomon` while an orb waits). Code: `SolomonRules` (numbers, pure), `SolomonSkill` (modes), `SolomonEvents` (passives that live in the game's events), `SolomonHooks` (the grimoire's chant and price, the drain guards), `SolomonState` (attribute modifiers, the Realm registry, absorb and message counters), `SolomonCasts` (the cast log and the stored copy), `SolomonSweep` (what Convergence and Erasure take), `SolomonPillars` and `SolomonTerrain` (the 72 Pillars), `SolomonFx` (effect packets).

| # | Mode | Use | Cost (normal / mastered) | Cooldown (normal / mastered) |
|---|---|---|---|---|
| 0 | Infinite Grimoire | toggle | free | - |
| 1 | Omnipotent Magic | toggle | free | - |
| 2 | Mana Skin | toggle | free | - |
| 3 | Elemental Sovereignty | toggle | free | - |
| 4 | Grand Spell Copy | press | 4.5K / 3.0K MP | 30 / 18 s |
| 5 | Arcane Convergence | crouch + press | 2.0K / 1.2K MP | 20 / 12 s |
| 6 | Realm of Magic | held, up to 60 s | 2.5K / 1.5K MP **per second** | 90 / 60 s |
| 7 | Spell Chain | press | 12K / 8.5K MP | 60 / 40 s |
| 8 | Mana Zone Erasure | crouch + press | 15K / 10K MP | 90 / 60 s |
| 9 | The 72 Pillars | press, **mastery only** | 80K MP | 240 s |

### Passives
- **Infinite Grimoire**: maximum Magicule +100% (+150% mastered) and Magicule regeneration +200% (+300%) as two transient attribute modifiers (put back once a second, so a login restores them); the chant of every grimoire cast is cut to 50% (35%), never below 2 ticks. Immunity to anti-magic and mana depletion: `SolomonHooks.antiMagicImmune` is checked at the drain points (`TensuraBridge.drainMagicule`, `NihilityZone`, `EnergyBridge.drain`, `WikiSpells.drainMana`, `ForbiddenBook`, `PaintStudio`, `LuciusCapabilities`), so those drains skip the wearer. Mastered: every grimoire spell costs 50%.
- **Omnipotent Magic**: with `solomonMagicBypass` on, the owner's magic blows ignore armour and resistance (their damage reductions are ignored, the result still limited by the Balance Law; a cancelled or fully immune hit is not forced through). Foresight: 25% (40% mastered) of an enemy's magic blow is not taken, with a short "foreseen" line at most every 3 seconds. Presence sense: the 8 nearest enemies within 24 blocks glow for 2 seconds, renewed once a second.
- **Mana Skin**: a blow that would kill is paid for with Magicule, 10 per point of damage, all of it when the pool allows, else as much as the pool pays for (the rest hurts). A blow that does not kill is left alone.
- **Elemental Sovereignty**: fire, lava, hot floor, freezing and poison damage is cancelled; poison, flame and frost on the wearer are lifted once a second; up to 10 elemental projectiles a second are absorbed and each gives 1% of the maximum Magicule (at most 5,000).

### Actives
- **Grand Spell Copy**: reads the newest grimoire spell that somebody else cast within 40 blocks in the last 30 seconds and writes it into the Grimoire (it is stored on the player and survives a restart). The next press of the same mode casts it once, free, with no chant and no cooldown of its own (a short gap stops double presses). Only grimoire pages are copied.
- **Arcane Convergence**: within 30 blocks up to 64 projectiles (spell bolts included) and 32 area clouds are drawn in, and fields of other owners end; each gives Magicule (250 / 400 / 1,500), one press never more than 25% of the maximum.
- **Realm of Magic** (held): a field following you (40 blocks, at most 1,200 ticks). Each second it pays the per-second cost (or ends), renews the realm record, and silences (Event Silence 30 ticks) and locks (Action Lock, Balance Law time) up to 24 enemies. Inside it you and your allies cast every grimoire spell for 0 Magicule with a one-tick chant. Releasing the key, running out of Magicule, death, logout, dimension change and a server stop end it.
- **Spell Chain**: the last three grimoire spells you cast are cast together, free. A replay is never logged, so a chain cannot feed itself.
- **Mana Zone Erasure**: within 40 blocks the enemy's projectiles (96), clouds (32), bolts, fields and summons (24) are deleted, and up to 24 casters get Temporal Stutter (60 ticks base, Balance Law).
- **The 72 Pillars**: up to 8 enemies within 60 blocks (players first, then the nearest) are marked, the 72 pillars are shared out between them, and they fall over about 6 seconds at the ground point fixed at cast time. Each pillar is a true hit (it bypasses Castle Guard) of 70 damage in a 2.8-block radius to at most 12 enemies, through the Balance Law and `activeDamageCap`, no creature taking more than 10 in one cast. Each impact may remove up to 10 blocks (600 per cast).

### Conditions
Event Silence, Action Lock, Temporal Stutter (Veldanava conditions already in the game), and the vanilla Glowing effect for the presence sense.

## Server settings added
`solomonTerrainDamage` (default true: the pillars dig craters; never in the Dungeon, never with `mobGriefing` off), `solomonSpellReplay` (default true: Grand Spell Copy and Spell Chain replay spells; off = both refused), `solomonMagicBypass` (default true: Omnipotent Magic ignores resistances) in `nusmp-veldanava-skills-server.toml` (SERVER config: they change gameplay).

## Multiplayer and server rules (CLAUDE.md rule 9)
The server decides everything; no client class is named (scanned by `JudgmentSourceScanTest`); per-player state is on the player or in UUID-keyed maps; the only effect path is `SolomonFx` -> `VfxSpawn`, one shape per packet; Solomon adds no tick hook (its second runs from the skill base); state is dropped at death, logout, forgetting and server stop. The replay is wrapped in `SafeCast`.

## Cost and lag bounds (CLAUDE.md rule 11) for Solomon
- **Per press**: Copy a walk of the 64-line log (no entity query); Convergence 2 box queries (64 projectiles, 32 clouds) and the bolt and field lists; Chain 3 grimoire spells (their own bounds); Erasure the same sweep plus one summon query (24 removed, 24 casters); Pillars one box query, at most 8 marks, 8 packets and one field.
- **Pillars worst case**: one field, 31 pulses of the server (every 4 ticks), at most 72 impacts, each one box query, 12 creatures hit, 600 blocks removed in all and only in loaded chunks; no entity spawns; one packet per target.
- **Per second per owner**: two attribute checks, one box query of 24 blocks for the presence sense (8 creatures marked); the Realm one box query of 40 blocks, 24 enemies touched.
- **Events**: effect, incoming damage, pre-damage (lowest priority), projectile impact, death, logout, server stop; each returns on its first line for anyone who is not under Solomon.
- **Memory**: the cast log holds 64 lines, each caster's list 3; both dropped at logout and server stop.
- **Network**: one effect packet per press (Realm: one every 80 ticks while held; Pillars: one per target; Skin and Absorb one small packet each, absorb at most 10 a second per wearer).
- **Client draw**: worst-frame quads per effect (engine cap 100): Copy 32, Convergence 71, Realm 39 (78 while two overlap for 20 ticks), Chain 37, Erasure 50, Mark 30, Pillar 34 with the usual 18 pillars and 90 with all 72 on one target (at most 6 falling and 6 landing drawn at once, every part checks the buffer budget), Skin 23, Absorb 16. The 17 textures take about 20 MB of video memory in all (largest 1024 x 1024).
- **No** chunk loads (terrain only touches loaded chunks), no entity spawns, no food-bar writes.

## VFX (Solomon layer)
Nine shapes appended after `JIBRIL_WINGS` (`SOLOMON_COPY`, `_CONVERGENCE`, `_REALM`, `_CHAIN`, `_ERASURE`, `_MARK`, `_PILLAR`, `_SKIN`, `_ABSORB`), one client layer `vfx/client/layer/SolomonLayer.java`, 17 textures `textures/particle/solomon_*.png` from `tools/gen_solomon_vfx_textures.py`; violet and gold with an emerald accent: grimoires with turning pages, hexagrams and seals, nested magic circles, linked chains, and pillars of eight elements. The pillars of one target are drawn from one packet: its seed names how many pillars, and the client works out when, where and which element each one is with the same functions the server lands its impacts with. Preview scenes: `tools/vfx_preview/scenes.d/solomon.json`.

## Tests
`SolomonRulesTest` (pure), `JudgmentRulesTest` and `JudgmentSourceScanTest` (extended, including the terrain, replay and event-hook scans), `SolomonShapesTest`, smoke prefix `solomon` (`solomon.rules`, `solomon.skill`, `solomon.conditions`: registration, mask separation, the modifiers, the chant and price hooks, the Realm registry and field, hazards, foresight, Mana Skin, the absorb counter, the cast log and the copy, the sweep, the terrain on a stone platform in the sky and with `mobGriefing` off, the pillars landing and being forgotten).

## NEW / REPLACED / KEPT
- **NEW**: Solomon and its 10 modes, the Solomon VFX, three settings, `SolomonRules` and the supporting classes, tests, lang. The drain-immunity guard in seven existing drain points (a first line that returns for anyone without Infinite Grimoire).
- **REPLACED**: nothing (the card was sealed; it is open now). The skill icon is the 3.17.0 animated one.
- **KEPT**: everything of 3.17.0 and earlier; the Time Magic VFX untouched.

## Not included / honest limits
- Hercules stays sealed (3.19.0).
- Not played in a real game.
- Only grimoire pages are copied or chained (not Ultimate abilities or other skills), and a page that needs a held weapon is not replayed. A replay uses your own copy of the book, or a fresh default one if you have not learned it.
- Omnipotent Magic ignores damage reductions (capped by the Balance Law); it cannot bypass a cancelled hit or an immunity that cancels the event.
- The presence sense is the vanilla Glowing effect on the 8 nearest enemies.
- The pillars land at the ground point fixed at cast time, so a target that runs away can dodge them; the effect shows the same point.
- The skill list's displayed price does not show the Realm's free spells or the mastered half price.
- The Realm's lock tapers with the Balance Law, so a boss or a long-held player is held for less.

---

# 3.19.0 - Hercules, Lord of Martial Prowess

Source of the design: the owner's Hercules spec (ten modes, `docs/veldanava_spec/veldanava_skill_drops.txt`). Wiki sources: none needed (the numbers come from the owner's file); nothing was fetched.

## How it is read in game
Take **Hercules** from the Will of the Creator card screen (`/veldanava skill hercules` while an orb waits). Code: `HerculesRules` (numbers, pure), `HerculesSkill` (modes), `HerculesEvents` (passives that live in the game's events), `HerculesState` (the leap, the momentum run and its two modifiers, Invictus, the picture gaps), `HerculesDamage` (the owner's blows through the Balance Law), `HerculesTerrain` (the only place a block is removed), `HerculesThrow` (the field that carries a throw), `HerculesFx` (effect packets).

| # | Mode | Use | Cost (normal / mastered) | Cooldown (normal / mastered) |
|---|---|---|---|---|
| 0 | Adamantine Body | toggle | free | - |
| 1 | God of Strength | toggle | free | - |
| 2 | Unstoppable Momentum | toggle | free | - |
| 3 | Colossal Vigor | toggle | free | - |
| 4 | Earth-Shattering Tremor | press | 3.0K / 2.0K MP | 25 / 15 s |
| 5 | Invictus | crouch + press | 8.0K / 5.5K MP | 90 / 60 s |
| 6 | Titan's Grip | press | 2.5K / 1.5K MP | 20 / 12 s |
| 7 | Devastating Throw | crouch + press | 4.0K / 2.5K MP | 30 / 18 s |
| 8 | Battle Roar | press | 5.0K / 3.2K MP | 45 / 30 s |
| 9 | World-Breaking Strike | press, **mastery only** | 15K MP | 180 s |

### Passives
- **Adamantine Body**: a physical blow is taken at 10% (90% less), a magical blow at 50%; fire, lava and burning blows are cancelled and flame is put out once a second; Temporal Stutter cannot be put on you and one that was on is lifted; no knockback; no fall damage. A true hit (Sandalphon's) steps past the body (it checks `JudgmentDamage.trueActive`), and a blow that bypasses invulnerability is not touched. A reduction of 3 points or more draws the plates, at most once every 10 ticks.
- **God of Strength**: your melee blows (the player's own attack, not a spell or an arrow) do x3 damage (x4.5 mastered), the growth limited by the Balance Law (`BalanceLaw.cap`, so a player takes at most the PvP hit cap). `herculesBlockBreak` on: blocks of hardness up to 50 (obsidian) break in one hit (the server raises the break speed in `PlayerEvent.BreakSpeed`; bedrock and other unbreakable blocks never, nothing at all in the Dungeon). `herculesShockwave` on: a blow also hurts up to 6 other enemies within 3 blocks of the one struck for 35% of the blow, at most once every 6 ticks.
- **Unstoppable Momentum**: once a second the server counts the distance covered; more than 3 seconds of continuous movement (2 or more blocks a second) gives stage 1, then stage 2 at 6 s and stage 3 at 9 s. Each stage adds 12% attack speed and 7% movement speed (two transient attribute modifiers) and gives back 15% (20% mastered) of the damage the armour took off a blow. Standing still ends the run. New stages draw one picture.
- **Colossal Vigor**: out of combat (100 ticks without a blow given or taken) 2% (4% mastered) of the maximum health a second; in combat 15% (25% mastered) of the damage of a melee blow comes back as health, at most 8% of the maximum for one blow.

### Actives
- **Earth-Shattering Tremor**: a leap (1.35 blocks a tick forward, 1.0 up). When you land within 6 seconds (the fall event), every enemy within 14 blocks (at most 24) takes 140 damage (full at the centre, 40% at the rim, Balance Law), is thrown up (a boss much less) and loses its footing (Movement Interference, Balance Law time), and the ground under you is broken in a bowl of radius 6 and depth 2, at most 240 blocks. The landing costs you no fall damage.
- **Invictus** (crouch + press): for 10 seconds your health cannot fall below 1 (health and absorption count; a blow that bypasses invulnerability still does) and your melee damage grows with the health you lack (up to x3, x4 mastered, at the last point). One field follows you, pulses once a second, draws the laurel again every 4 s and ends it; its end, death and logout clear the mark.
- **Titan's Grip**: the enemy in sight within 40 blocks (a flying boss too) is pulled to 2.2 blocks in front of you (a free spot is looked for), and Action Lock is laid on it (Balance Law time). One ray, one move.
- **Devastating Throw** (crouch + press): the enemy in sight within 12 blocks is hurled along your look direction at 2.4 blocks a tick; with none, a chunk of up to 12 blocks of the ground you look at (within 8 blocks) is lifted and hurled. One field of 2-tick pulses (at most 9) carries it: a path sweep hurts up to 8 creatures for 90 on the way; on landing the thrown creature takes 200, a blast of radius 4.5 hurts up to 12 for 130 and a crater of radius 3 removes at most 60 blocks (terrain only with the setting on, `mobGriefing` on and outside the Dungeon).
- **Battle Roar**: your own debuffs are cleansed (at most 24), and every enemy within 30 blocks (at most 32) gets Fear (Tensura's) and Temporal Stutter, both limited per target by the Balance Law.
- **World-Breaking Strike** (mastery only): one punch at the enemy in sight (or the point 5.6 blocks ahead). Barriers within 24 blocks end: the pockets of other players' Labyrinths (Israfil), the fields of the enemy, a player's Michael Castle Guard (spent for the day), the guard of a Seraphim and Uriel's prison. Of the (at most 8) enemies within 6 blocks, each has its armour broken (new effect Shattered Armour, 400 ticks, half that on a player or a boss), 60 stacks of Temporal Debt laid on it and takes 600 (the others half), through the Balance Law.

### Conditions
Movement Interference, Action Lock, Temporal Stutter, Temporal Debt (Veldanava conditions already in the game), Tensura's Fear, and the new **Shattered Armour** (armour and toughness fall to nothing).

## Server settings added
`herculesTerrainDamage` (default true: the tremor, the throw's crater and a lifted chunk remove blocks; never in the Dungeon, never with `mobGriefing` off), `herculesBlockBreak` (default true: God of Strength breaks blocks in one hit), `herculesShockwave` (default true: a heavy blow hurts the enemies around the one struck) in `nusmp-veldanava-skills-server.toml` (SERVER config: they change gameplay).

## Multiplayer and server rules (CLAUDE.md rule 9)
The server decides everything, including the one-hit mining (the client has no mask; the server raises the break speed, vanilla then breaks the block at once); no client class is named (scanned by `JudgmentSourceScanTest`); per-player state is on the player or in UUID-keyed maps; the only effect path is `HerculesFx` -> `VfxSpawn`, one shape per packet; Hercules adds no tick hook (its second runs from the skill base); state is dropped at death, logout, forgetting and server stop.

## Cost and lag bounds (CLAUDE.md rule 11) for Hercules
- **Per press**: Tremor one jump and, on the landing, one box query (24 creatures hurt and lifted, at most 24 effects) and a bowl of at most 240 blocks; Invictus one field and one packet now plus one every 80 ticks for 10 s; Grip one ray, one move, one effect; Throw one ray, one field running 9 pulses, per pulse at most one box query (8 creatures hurt on the way), on landing one box query (12 blasted) and 60 blocks; Roar one box query (32 enemies, 64 effects) and at most 24 effects cleansed; Strike one ray, one box query (8 creatures, 8 each of debt and broken armour) and the pockets and fields within 24 blocks ended.
- **Per second per owner**: two attribute checks, one heal, at most one picture when Momentum reaches a new stage; no box query.
- **Events**: effect, incoming damage, knockback, fall, pre- and post-damage, break speed, death, logout, server stop; each returns on its first line for anyone who is not under Hercules. A shockwave costs one 3-block box query, at most once every 6 ticks per owner (6 creatures hurt), and a shockwave never starts another.
- **Memory**: three small UUID maps (leap, run, picture gaps), dropped at death, logout, forgetting and server stop.
- **Network**: one effect packet per press (Invictus: one more every 80 ticks); the body picture at most once every 10 ticks per wearer, the shockwave picture at most every 6 ticks per owner.
- **Terrain**: loaded chunks only, never loaded for the cast, never blocks with an inventory or any block entity, never over hardness 50, never fluids; no drops and no break particles (the picture is the client's); one block write each (`HerculesTerrain.remove`).
- **No** chunk loads, no entity spawns (a thrown chunk is a moving point, not an entity), no food-bar writes.

## VFX (Hercules layer)
Ten shapes appended after `SOLOMON_ABSORB` (`HERCULES_LEAP`, `_TREMOR`, `_INVICTUS`, `_GRIP`, `_THROW`, `_ROAR`, `_STRIKE`, `_SHOCKWAVE`, `_BODY`, `_MOMENTUM`; the from / to / power / duration contract of each is in the enum's comments), one client layer `vfx/client/layer/HerculesLayer.java`, 17 textures `textures/particle/hercules_*.png` from `tools/gen_hercules_vfx_textures.py`; antique gold with bronze and white-hot flashes: fists, laurels, cracked ground, shock rings, chains and plates of adamantine. The call list is `docs/vfx_remake/hercules_fx_calls.md`. Preview scenes: `tools/vfx_preview/scenes.d/hercules.json`.

## Tests
`HerculesRulesTest` (pure), `JudgmentRulesTest` and `JudgmentSourceScanTest` (extended, including the terrain, throw and event-hook scans), `HerculesShapesTest`, smoke prefix `hercules` (`hercules.rules`, `hercules.skill`, `hercules.conditions`: registration, mask separation, the body's reductions, fire, Stutter and fall, strength, Invictus' floor, the leap and its landing, the terrain on a stone platform in the sky and with `mobGriefing` off, the throw's field ending inside its bound, the Castle Guard spent once, Shattered Armour, and the clean-up).

## NEW / REPLACED / KEPT
- **NEW**: Hercules and its 10 modes, the Hercules VFX, three settings, the Shattered Armour effect, `VeldanavaBoss.shatterSpatial`, `HerculesRules` and the supporting classes, tests, lang.
- **REPLACED**: nothing (the card was sealed; it is open now). The skill icon is the 3.17.0 animated one.
- **KEPT**: everything of 3.18.0 and earlier; the Time Magic VFX untouched.

## Not included / honest limits
- Not played in a real game.
- One-hit mining is decided by the server (the client has no mask), so the swing animation may show the block cracking for a moment before it breaks.
- The tremor needs a landing that raises Minecraft's fall event: a landing in water or on a slime block does not trigger it (the leap's window is 6 seconds, then it is forgotten).
- World-Breaking Strike shatters the barriers this mod defines (Michael's Castle Guard, Uriel's prison, the Seraphim's guard, Israfil's pockets, the enemy's fields); another mod's barrier is not touched.
- Adamantine Body does not stop a true hit (Sandalphon's) or a blow that bypasses invulnerability (the void, `/kill`).
- Devastating Throw's chunk is a moving point drawn by the effect, not an entity; it is stopped by the first solid block on its path.


## 3.20.0 - the master toggle and the Hercules upgrades

- **Master toggle (all five skills).** `VeldanavaSkillBase` answers `canBeToggled` with the setting `masterToggle`; `onToggleOn/Off` set the four passive bits together (`JudgmentRules.masterBits`), mirror them into the mask and call each skill's `toggled` hook for the four passives. The wheel has `count - 4` modes (`JudgmentRules.wheelCount/slot/cooldownSlot`): wheel position `w` is table mode `w + 4`, and cooldowns are stored at the wheel position so the game's own display and the cooldown copies of other skills line up. A position saved by an older build wraps into the shorter wheel. An old save with any passive on becomes toggle ON with all four (`settleMaster`, once a second). With `masterToggle` false the ten-mode wheel of 3.19.0 is back.
- **Hercules (setting `herculesUpgrades`).** Aftershock: 14 ticks after the landing, `AFTERSHOCK_RADIUS` 9, half a tremor falling off to the rim, at most 24 creatures, no blocks, one pending aftershock per owner (`HerculesState.tickAftershocks`, idle when empty). Titan's Grip: a crush of `GRIP_CRUSH_DAMAGE` 160. Battle Roar: thunder `ROAR_DAMAGE` 110 within 12 blocks (falling to half), at most 16 foes. Invictus: a second wind of 10% / 20% maximum health when it runs out by itself. Every blow is `HerculesDamage.blow` (Balance Law, cap of one press).
- **Cost.** Per press nothing new beyond one blow per creature already queried; the tick hook is one `isEmpty` test. No new packets.

## 3.21.0 - the Sandalphon upgrades

- **Sandalphon (setting `sandalphonUpgrades`).** Bullet: `SHATTER_PER_EFFECT` 12 per stripped good effect, at most `SHATTER_MAX` 3 (`JudgmentRules.shatterBonus`). Edict: `EDICT_DAMAGE` 60 + `DECREE_PER_EFFECT` 20 per stripped effect, at most 4 (`decreeDamage`), on enemies, players and mobs targeting the owner. Cleave: base damage grows by up to `CLEAVE_DEBT_BONUS_MAX` 30% with the target's Temporal Debt (`cleaveDamage`), and a sigil-marked target of this caster below `GUILLOTINE_BELOW` 35% health is executed (`guillotineFalls`). Verdict: when a field ends by running out (not evicted, cleared, or owner gone) it tolls `VERDICT_TOLL_SHARE` 6% of maximum health on creatures inside, one box query, at most `MAX_FIELD_TARGETS`. All blows go through the Balance Law and the active-damage cap; setting off restores 3.20.0.
- **Models.** `JudgmentModels`, `JudgmentMeshDraw`, `JudgmentModelsConfig` (client). See `docs/vfx_remake/judgment_models.md`.
- **Cost.** No new packets; one box query only at a natural Verdict end; worst client case 90 quads (Field) of the 100 cap.

## 3.22.0 - the Israfil upgrades

- **Israfil (setting `israfilUpgrades`).** Grand Trumpet: `TRUMPET_DAMAGE` 150 falling to `TRUMPET_FALLOFF_MIN` 25% at the 100-block rim (`IsrafilRules.trumpetDamage`), one blow per creature of the 48 it already touches. Coordinate Shift: `SHIFT_SHEAR_DAMAGE` 70 on a hostile creature it trades places with. Spatial Collapse: when the Nihility field ends by running out (not evicted, cleared or ownerless) it implodes for `NIHILITY_FINAL_SHARE` 8% of maximum health on creatures inside, one box query, at most `MAX_FIELD_TARGETS`, with a pull. All blows go through the Balance Law and the active-damage cap; setting off restores 3.21.0.
- **Models.** `IsrafilModels`, `IsrafilMeshDraw`, `IsrafilModelsConfig` (client). See `docs/vfx_remake/israfil_models.md`.
- **Cost.** No new packets; one box query only at a natural Nihility end; worst client case 98 quads (Trumpet) of the 100 cap.
