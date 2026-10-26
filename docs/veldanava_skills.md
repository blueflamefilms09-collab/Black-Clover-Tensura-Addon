# Veldanava's ultimate skills (3.13.0 onward): Sandalphon, Lord of Judgment

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
VFX_SECTION_PLACEHOLDER

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
