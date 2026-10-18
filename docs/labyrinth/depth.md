# Depth: the deeper (or higher) you go, the stronger and smarter the mobs (1.89.0)

Owner brief: "make it so the higher/lower you go in a dungeon the stronger and smarter the mobs will get."

## NEW / REPLACED / KEPT
- NEW: the depth rule (`labyrinth/mega/enc/Depth*.java`), config `nusmp-labyrinth-mega-depth-server.toml`, command `/multiverse mega dm depth [floor]`, self-test `enc.depth`.
- REPLACED: nothing.
- KEPT: the Dungeon Master's own threat scaling (+6 percent health and +4 percent damage per floor), the Mob Foundry's tier scaling, the Guardian and boss scaling. They are untouched.

## What it does
Every hostile mob that is born, or loaded, in the Mega-Dungeon gets two things from the DEPTH of its floor. Depth is the distance of the floor from the Hub, so floor +300 (Holy, above) and floor -300 (Abyssal, below) are exactly equally deep.

1. **Stronger.** Attribute bonuses on a saturating curve `bonus = ceiling * x / (x + half)`, `x = depth - 1` (nothing on floor 1, noticeable from floor 2, never a runaway):

   | stat | ceiling | half-way depth | floor 10 | floor 100 | floor 1000 |
   |---|---|---|---|---|---|
   | health | +400 % | 300 | +12 % | +99 % | +308 % |
   | damage | +250 % | 300 | +7 % | +62 % | +192 % |
   | movement speed | +22 % | 400 | +0.5 % | +5 % | +16 % |
   | armour | +12 points | 400 | +0.3 | +3 | +8.6 |
   | knockback resistance | +0.6 | 500 | +0.01 | +0.1 | +0.4 |
   | follow range | +24 blocks | 150 | +1.4 | +9.5 | +20.9 |

2. **Smarter.** A MIND LEVEL (0 to 6) from the depth, thresholds at depth 6, 16, 40, 100, 250, 500 (config `mindFloors`). Each level teaches one behaviour, and a deeper mind also thinks more often (every 20 ticks at mind 0, down to every 4 ticks at the top):

   | mind | behaviour | what the player sees |
   |---|---|---|
   | 1 | memory | a mob that loses sight of you keeps hunting where it last saw you (5 seconds, 2 more per level), and goes back to the chase when it sees you again |
   | 2 | alert | a mob that is hurt, or sees a new player, calls the monsters within 12 blocks (4 more per level, at most 40) which then hunt you |
   | 3 | flank | several melee mobs on one target spread round it, one to a side, instead of queuing in a line |
   | 4 | regroup | at 30 percent health a mob backs off, recovers (Regeneration II for 6 seconds) and returns at 60 percent, once in its life; it calls for help as it falls back |
   | 5 | close down | a target that keeps its distance (bow, pillar, hit and step back) is closed down with a burst of Speed II |
   | 6 | hunter | with two or more players in sight it goes for the one with the least health and armour, not the nearest |

   A mob only ever aims at a living player who is not in creative mode, not a spectator and not in a safe room.

## Who is touched, and who is not
- Touched: every `Enemy` mob that joins the Mega-Dungeon level on a known floor: the Dungeon Master's encounters, the Mob Foundry's 400, Tensura's and vanilla's own monsters, mechanic summons.
- Not touched: bosses (`BossIdentity`: Guardians, Floor Wardens, the wandering bosses, anything in the `nusmp:boss` tag), pets and summons that have an owner, non-hostile animals, the Hub and the gaps between sectors (floor 0).
- Mobs of the Dungeon Master and the Foundry already scale with the floor by their own rules, so they take only half of the strength bonus (config `shareForScaledMobs`). Their minds are not shared down: they get the full mind.
- The floor a mob was BORN on is saved on it (`nusmp_depth_floor`), so a mob that wanders keeps its strength and a reload does not re-roll it. The modifiers have fixed ids (`nusmp:mega_depth_*`), so applying twice replaces and never stacks.

## Config (`[depth]`)
`enabled` (true), `strength` (true), `smarts` (true), `strengthScale` (1.0, 0 to 5), `shareForScaledMobs` (0.5), `mindFloors` ("6,16,40,100,250,500").

## Command
`/multiverse mega dm depth` shows the table for the floor you stand on; `/multiverse mega dm depth <floor>` for any floor (op).

## How it is built
`DepthRules` (pure arithmetic and decisions, `DepthRulesTest`, 13 tests), `DepthConfig`, `DepthMobs` (the join listener and the attribute modifiers), `DepthMind` (the goal: no movement flags, so it runs beside the mob's own goals or brain, steering through `MobSteer` like the wandering bosses do), `DepthSelfTests` (`enc.depth`, run by the CI smoke test).

## Not verified
Nothing has been run in the game. The curves and thresholds are my design. The behaviours are the part most likely to need tuning in play: flanking and close-down share the mob's navigation with its own melee goal, which re-paths every few ticks, so the result may look jittery on some mobs; Tensura mobs that think with a brain may ignore a goal's steering for a pass.
