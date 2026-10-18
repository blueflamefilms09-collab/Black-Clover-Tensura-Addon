# The Dungeon Mechanics Library (plan, then reference)

**Status:** plan committed first; this file is rewritten into the reference as the library grows. Code: `labyrinth/mega/mechanics/`,
data: `data/nusmp/mega/mechanics/*.json` (generated), generator: `tools/mechanics/gen_mechanics.py` + `tools/mechanics/tables/*.py`.

## What it is
A layer ON TOP of the traps, riddle rooms and room kinds that already exist (nothing is removed): **1000 data-defined mechanics** the
dungeon can attach to a floor, a room, a boss fight or a hazard zone. Each mechanic = id, name, description, flavour (lang), category,
source (an anime or a D&D book), tier range (5 tiers of 200 floors, as `DndRules.tierOf`), road affinity, theme affinities, a threat cost,
a weight, a TRIGGER, a list of EFFECTS from ~80 composable primitives (numbers scaled by floor), and an optional COUNTER (how a player
beats or disarms it).

## Categories (125 each = 1000)
| id | name | what |
|---|---|---|
| floor | Floor modifiers | gravity, silence, mana tide, rukh flow, time dilation, shifting walls, darkness, weather, burning floor ... |
| puzzle | Room puzzles | sequence plates, light/mirror routing, weight, levers, notes, colours, memory, carry ... |
| hazard | Hazards and traps | timed, proximity, line of sight, chains, falling ceilings, flooding, spores, collapsing floors, glyphs ... |
| combat | Combat gimmicks | kill order, shielded foes + switch, reflective phases, waves, cursed weapons, ambush rooms ... |
| risk | Risk and reward | cursed chests, bargains, gambling altars, sacrifice shrines, mimics, vault timers ... |
| boss | Boss mechanics | phases, lair actions, arena transformations, minion tethers, anti-cheese ... |
| travel | Traversal | moving platforms, wind lifts, one-way doors, teleport networks, shafts ... |
| party | Social and party | shared burdens, split-party rooms, revive rules, cooperative locks ... |

## Generation (data, deterministic)
~150 hand-designed BASE mechanics (`tools/mechanics/tables/<category>.py`), each with a few ELEMENT / TRIGGER / COUNTER alternatives.
`gen_mechanics.py` expands every base into its base + 5 or 6 variants that change something the player notices (the element it uses,
what triggers it, how it is beaten) and writes `data/nusmp/mega/mechanics/<category>.json` + `ops.json` (the primitive catalogue)
+ `tools/mechanics/lang_fragment.json`. `--check` exits 1 when a file on disk differs. A BEHAVIOUR SIGNATURE (sorted primitives + trigger + counter + scope
+ categorical arguments + bucketed numbers) must be unique across the library; so must every name and description.

## Engine (server)
`MechanicsDirector`: picks mechanics for a floor / room by weight from the floor's tier, road, theme and threat budget with a per-floor seed
(`Seeds`, reproducible), under per-floor and per-room caps, never stacking mechanics that share an exclusive CHANNEL (the conflict table),
never inside safe rooms, the Hub (a different dimension) or rift pockets (`RiftRules.slotAt >= 0`); activates an instance when a player
enters its area; ticks triggers, effects (sustained ones too) and counters; cleans up (blocks restored from a JOURNAL, spawned mobs removed,
flags dropped) when the area is left, the floor is left or unloaded. The journal of a running instance is mirrored in SavedData so a crash
cannot leave a block changed.

Hooks (all existing events): server tick, `FloorListener`, `LivingDeathEvent`, `LivingIncomingDamageEvent`, `PlayerInteractEvent`,
logout/dimension change/server stop.

## Config / commands / tests
`MechanicsConfig` (own file `nusmp-labyrinth-mega-mechanics-server.toml`), `/multiverse mega mechanic list|info|apply|clear|count|verify`,
in-game self-tests `mech.*`, JUnit tests for the signature guard, distribution, scaling, weights, conflicts and seeding.
