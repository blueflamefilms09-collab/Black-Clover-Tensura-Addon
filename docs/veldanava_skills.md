# Veldanava's ultimate skills (3.13.0 onward): Sandalphon, Lord of Judgment, and Israfil, Lord of Trials (3.14.0)

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
- **Labyrinthian Mind**: no mind-altering effect lands on you (any effect whose id names charm, fear, confusion, nausea, hypnosis, domination, mind, control, sleep, terror, madness, taming or possession); your own passives' presence scan and Sandalphon's presence scan skip you.
- **Weight of Consequence**: once a second, every foe within 20 blocks that is **moving** (the 16 nearest) gains +1 Temporal Debt and Slowness for 2 s.

### The actives
- **Dimensional Labyrinth**: the creature in your sight (40 blocks) is caged for 8 s in a virtual maze of radius 4.5: held inside the walls (a boss is not moved, it is only disoriented), confused, Action-Locked and Movement-Interfered for the time the Balance Law allows (3 s players, 5 s mobs, 1 s bosses), 1% of its maximum health every second, +5 debt. The maze is drawn, not built: **no block is placed or removed**. A target with Spatial Domination slips out.
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
- **Per press** (actives are 8 ticks apart, and every one has a cooldown of 5 s or more): Labyrinth 1 line query + 1 field; Weight of Sins 1 query + 1 hit; Spatial Collapse 1 query + at most 16 hits + 1 field; Coordinate Shift 1 query + at most 8 projectiles + one box query of at most 24 mobs; Rift Walk 1 session; Grand Trumpet 1 box query (100 blocks), at most 48 creatures touched, at most 48 players' skills walked once (16 modes each) and one field sweep.
- **Per second per owner**: Spatial Domination and Trial of Endurance refresh 2 attribute modifiers (only written when the value changes); Weight of Consequence touches at most 16 creatures with one box query.
- **Per tick**: the field manager (does nothing when empty; global cap 24 fields, each field pulsing at most 4 times a second; the cage is one creature, the Nihility field at most 24) and the Rift Walk manager, which returns on its first line when nobody walks and otherwise does one comparison per walker (a server holds a handful at most) plus one Magicule charge per walker per second.
- **Events**: seven handlers (fall, incoming damage, post damage, effect, projectile impact, heal, and the housekeeping events); each returns on its first line for anyone who is not an owner of Israfil or does not carry the condition.
- **Network**: one effect packet per press through `VfxSpawn` (nearby players only, the effect-packet budget), plus one small packet a second per Rift Walker. No new payloads.
- **No** terrain damage, no chunk loads, no entity spawns, no block changes, no food-bar writes.

## VFX (Trial layer)
VFX_PLACEHOLDER

## Tests
`JudgmentRulesTest` (extended: skill codes, spec list, cooldowns), `JudgmentSourceScanTest` (extended: Israfil's caps, Rift Walk manager, `TrialFx` one shape per packet, config keys, assets, lang, smoke prefix), `TrialShapesTest` (agent), CI smoke prefix `israfil` (`israfil.rules`, `israfil.skill`, `israfil.conditions`), and the `veldanava.orb` test (a still-sealed card is now Jibril).

## NEW / REPLACED / KEPT
- **NEW**: Israfil and its 10 modes, Gravity Crush and Nihility Collapse conditions, the Trial VFX, the shared `VeldanavaSkillBase`, the skill code in the mask, pluggable field behaviours, the `cooldownScale` setting, the Israfil icon, tests.
- **REPLACED**: Sandalphon's actives had no cooldown and now have one that falls with mastery (owner request); `executePlayers` default false -> true (owner request, fresh configs only).
- **KEPT**: everything else of 3.13.0 (the numbers, the VFX, the card screen, the boons, the orb, the loot). The Time Magic VFX is untouched.

## Not included / honest limits
- Jibril, Solomon, Hercules (sealed; 3.15.0, 3.16.0, 3.17.0).
- The labyrinth is virtual (no blocks); the Trumpet ends other Veldanava fields, not real barrier blocks.
- Spatial Domination cannot stop another mod's own teleports or knockback.
- Not seen in a real game session: the CI smoke test proves the registry, grant, state and caps on a real server; the visuals were checked in the preview harness only. Rift Walk's spectator hand-over has not been played with two clients.
