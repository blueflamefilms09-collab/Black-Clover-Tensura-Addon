# Saiyan techniques III (3.71.0)

Four more Saiyan techniques, next to everything else (nothing existing is changed; the ops are appended after `OP_METEOR = 17`). All of it is decided on the server; the client only asks (one byte, the existing `saiyan_action` packet). No new packet, no entity is ever spawned, no block is touched. Master switch: `[techniques3] enabled` in `nusmp-saiyan-server.toml`. Visuals are server-side particles through the budget (`Fx`); a client painter for the dragon is not part of this release.

| Technique | Who | Key (default, rebindable) | What it does |
|---|---|---|---|
| **Dragon Fist** (`OP_DRAGON = 18`) | any awakened Saiyan with enough **martial training** (`dragonFistUnlockTraining`, default 15 points; a game day gives at most 60) | Numpad 1 | Locks the nearest foe in front inside 5 blocks. A golden dragon of spark motes winds up round the foe in 6 steps two ticks apart (the foe is lifted a little); the last step is the blow (3 x `dragonFistDamage`, through the balance law as the Saiyan's own martial hit) that throws the foe up. Bosses are hit but not thrown. Trains the martial category. |
| **God Bind** (`OP_BIND = 19`) | Son Goku Ultimate only | Numpad 2 | Locks one foe in front inside 24 blocks and holds it still for 6 s: its motion is zeroed each tick and Slowness X is refreshed twice a second. A **boss** is held for 25% of that, a **player** for 50% (the smaller share applies if both). Players only where `SaiyanAbilities.allowed` lets Ki hit them (PvP setting, teams, protected places). Ki cost 10%. Let go when the time ends, the foe dies, or the binder dies. |
| **Ki Deflection** (`OP_DEFLECT = 20`) | any awakened Saiyan | Numpad 3 | Opens a 4 s window. Every second tick, projectiles within 4 blocks that are fast and not already turned (at most 8 per pulse) are sent back at their shooter at 1.2x speed (never faster than 3 blocks/tick), re-owned by the Saiyan so the damage is the Saiyan's. A projectile whose shooter the Saiyan may not hit (ally, protected place, PvP off) is left alone. Ki cost 4%. |
| **Energy Transfer** (`OP_ENERGY = 21`) | any awakened Saiyan | Numpad 4 | Gives Ki to the nearest **allied player** within 8 blocks. The giver pays 20% of their own maximum; the ally gets that Ki less a 20% loss, at most 30% of the ally's own maximum per use (so Ki can never be multiplied). Cool-down 30 s. |

All numbers are server settings (`nusmp-saiyan-server.toml`, section `[techniques3]`), each with a hard bound; the pure arithmetic is in `SaiyanTech3Rules`, the server logic in `SaiyanTechniques3`, the state in `SaiyanManager.Run.tech3` (dropped on logout, death, dimension change and when the system is switched off).

## Worst-case cost
- Server, per Saiyan: Dragon Fist <= 8 steps x ~10 particles, 8 UUID lookups, 1 blow, plus one foe query when started. God Bind: 1 UUID lookup and 2 velocity writes per tick on ONE foe, 1 particle ring (4 particles) per half second, 1 foe query when started. Ki Deflection: 1 projectile query (box radius <= 8) every second tick for <= 20 s, <= 8 projectiles handled (and 64 looked at) per pulse. Energy Transfer: 1 player query per use (<= 64 looked at), 6 particles.
- Per-tick cost when idle: one `busy()` comparison.
- Network: 0 new packets. Particles go through the `Fx` budgets and only reach nearby players.
- Client: 4 key mappings, nothing drawn.

## Self-tests and tests
`saiyan.tech3.rules` (numbers and hard bounds) and `saiyan.tech3.player` (a stand-in Saiyan: God Bind refused without the Ultimate, Deflection open / cool-down / close, Energy Transfer with no ally changes nothing, God Bind holds and lets go a zombie, Dragon Fist refused untrained and run to its end once unlocked, no entity spawned) run in the CI server smoke test. Unit tests: `SaiyanTech3RulesTest`, `SaiyanTech3SourceScanTest`.

## Not included / honest caveats
- Not seen in a game. A redirected projectile uses `setDeltaMovement`; fireball-type projectiles that keep their own acceleration may drift back toward their old line. Ki Blasts and Kamehameha of other Saiyans are rays (not projectiles) and are not deflected.
- No client painter for the dragon (server particles only), no Giant Avatar and no Afterimage (later release).
- God Bind holds players mainly through Slowness and server motion; a client with a modified movement can still twitch.
