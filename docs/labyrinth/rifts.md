# Dimensional rifts, pocket realms and the waypoint (1.73.0)

New in 1.73.0 (nothing like it existed). Code: `labyrinth/mega/rift/`, client `client/labyrinth/rift/`, relics `relics/wave/RiftActs.java` + `Wave36` (generated from `tools/relics/relic_rows_rift.py`).

## The rift block
`nusmp:dimensional_rift`: invisible block (render shape INVISIBLE), no collision, unbreakable and immovable, light 11, no item. Its block entity holds the type, age, life left, seed and the destination, ticks on both sides and syncs type/life/stability to the client.
`DimensionalRiftRenderer` draws a 2.6 block pointed-oval window that turns to face the camera, a rim, jagged arcs re-rolled every 3 ticks from the seed (cyan shortcut, violet pocket, gold return, green exit) and a soft glow; the window uses a skybox render type (End star field for shortcuts, the mod's crimson void for pockets, the End gateway field for return and exit). It opens over 1.5 s and gutters in its last 4 s; an unstable shortcut is drawn dim. Particles are Light and Dark Rukh only.

## Type B: shortcut rifts
Open by themselves near a player on a playable floor (every `checkSeconds`, chance `shortcutChance`, at most `maxNear` within 48 blocks, never on a boss floor, never near a stair pad or in a safe room). The jump is 3 to 5 floors (`jumpMin`, `jumpMax`), rolled from the rift's seed, held at the next boss floor (`RiftRules.shortcutTarget`: a shortcut can never skip a Decennial Boss's seal). If the destination floor is not built yet the rift is dim ("the floor behind it is still forming") and asks the generator for the floors behind it, one at a time, like the stairs. The Directional Lock is asked first. The player (and party mates within 6 blocks) arrive on the arrival pad of the destination floor; a used tear closes.

## Type A: pocket rifts
Rare (`pocketChance`). A pocket is an instanced hidden realm built in a cell of a far-away field of the Mega-Dungeon's dimension (x = 4,000,000 + 192 per slot, 64 slots), because a real new dimension per rift cannot be created at run time in NeoForge 1.21. Whoever steps into the same rift shares its realm. Layout: Antechamber (arrival, exit rift) -> corridor -> Hall -> barred door -> Vault with three chests (the dungeon's own loot tables: the relic vault's and the trial's = the rare loot). Two variants (`bossShare`): **secret boss** (a Labyrinth Guardian scaled and D&D-profiled by the floor the rift opened on; the bars lift when it dies) and **puzzle vault** (four coloured plates; the wall shows the order; the right order lifts the bars, a wrong press resets and wakes two vexes). It collapses after `pocketMinutes` (warnings at 60/30/10/5/3/2/1 s), throws everyone back to where they came in (or their waypoint, or Floor 0), clears its cell and frees the slot. A player who logs in inside a pocket that no longer exists is sent out.

## The waypoint (PlayerDungeonWaypoint)
NeoForge 1.21 has no capability for a player's own data; the waypoint is one compound in the player's persistent data (`nusmp_mega_waypoint`: dimension, sector, level 1..100, signed floor, position, rotation, tick), like the Hub's origin and the landings. It is sampled every 2 s while the player stands on solid ground in a playable floor (not on a stair pad, not in a pocket), so it is right for every way out. Return: the exact block if the floor is still a place of the player's own party; else the floor's arrival pad; else a message.
- **Return Crystal** (relic, charm): saves the place and takes you to Floor 0 (cooldown `crystalCooldown`).
- **Gate Key** (relic, charm): outside the dungeon, back to the waypoint (cooldown `gateKeyCooldown`); sneaking, opens a **Return Portal** (a RETURN rift, `portalSeconds`) three blocks ahead on Floor 0 for the whole party: each player who steps in goes to their own waypoint.
- **Rift Compass**: names the nearest open rift. **Pocket Key** (legend, once a day): opens a pocket rift in front of you. **Waypoint Anchor**: marks the place now.

## Config and commands
`world/serverconfig/nusmp-labyrinth-mega-rift-server.toml` `[rift]`. `/multiverse mega rift open <shortcut|pocket|return>`, `list`, `closeall`, `status`, `pocket list|collapse <slot>`, `waypoint show|save|clear|return [player]`. Self-tests (CI smoke): `rift.block`, `rift.waypoint`, `rift.pocket.boss`, `rift.pocket.puzzle`.
