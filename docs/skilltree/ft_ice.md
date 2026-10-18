# Ice-Make: the maker's route of the Skill Tree (`nusmp:ice_make`)

Part of the 1.62.0 Skill Book & Skill Tree job (design: `docs/skilltree/design.md`, sections 2, 6 and 11). Built by `stfb`. This page is the WHY of every file of the route, the canon it is taken
from, every number, and an honest list of what has not been run. Its sister page is `ft_sky.md` (Sky Dragon Slayer); the two routes share the small toolkit in `ft/kit/**`, whose WHY is in the table
of section 7 here and in more words in `ft_sky.md`, section 7.

**New or replacement (CLAUDE.md rule 5).** The whole route is NEW: the mod had no skill tree and no Ice-Make (it has the Black Clover *Ice Magic* and *Ice Wedge* attributes of the grimoire, which are
different magics in a different system, and which this route does not touch; it only reuses their EFFECT shapes, which are neutral). Nothing is replaced or removed. The only existing file edited is the
stub the contract left for this job (`ft/IceMakeSkills.java`), whose `SKILLS` declaration is kept exactly.

## 1. At a glance

| | |
|---|---|
| Route id / file | `nusmp:ice_make` = `src/main/resources/data/nusmp/skill_trees/ice_make.json` |
| Class id | `ice_make` |
| Book | `nusmp:ice_make_book`, named *Chisel of Silent Winters* ("A frost-rimed pattern book. Every page is a blueprint that melts if the reader hesitates.") |
| Colour / icon | `#BDEFFF` (cyan-white) / `minecraft:blue_ice` |
| Loot weight / duplicate book | 2 (the brief) / 3 SP |
| Nodes | 9: 8 techniques (one is the Secret Art) and 1 passive |
| Cost of the whole route | 88 SP |
| Play style | maker and controller: decide where each piece of ice stands or flies; control and constructs rather than burst |
| Code | `skilltree/ft/IceMakeSkills` (register) and `skilltree/ft/ice/**`; the shared toolkit is `skilltree/ft/kit/**` |
| Lang | `nusmp.skill.ft_ice_make_*`: 16 lines (8 skills, a name and a description each) |

## 2. Canon, and where each fact comes from

WebSearch with `allowed_domains: ["fairytail.fandom.com"]` returns summaries of the wiki pages (the wiki itself cannot be fetched from the build machine). "Summary" means a search summary of that
page; "memory" means my own knowledge of the series that no source checked; "design" means my own decision for the game.

| Fact | Where from |
|---|---|
| Ice-Make is Maker (Molding) Magic. **Static Ice-Make** makes inanimate things (weapons, objects) or changes the environment; **Dynamic Ice-Make** makes animated figures, usually living creatures, that the caster controls, and is said to be quicker to cast | summary of <https://fairytail.fandom.com/wiki/Ice-Make> |
| Gray is mainly Static (he sometimes forges Dynamic constructs); Lyon is the Dynamic specialist | summary of the same page and <https://fairytail.fandom.com/wiki/Lyon_Vastia> |
| Lance: Gray extends his arms and fires long, curved ice spears that impale the enemy | summary of <https://fairytail.fandom.com/wiki/Gray_Fullbuster> and the Ice-Make page |
| Shield: a large barrier raised in front of him, vaguely flower-shaped, protecting what is behind it (Rufus copied and erased it in the Grand Magic Games) | summary of the same pages |
| Floor: freezes the ground under the opponents so they lose their footing; he can also freeze water and slide across it | summary of the same pages |
| Hammer: a large ice hammer floats over the opponent and drops with great force; first used against Lyon, who blocked it | summary of the Gray page |
| Cannon: a small, ornate ice cannon that uses nearby rubble as ammunition; several can be made and fired at once | summary of the Gray page |
| Saucer: a giant spinning ice disc with the cutting power of a buzz saw, launched at the target; **anime only** | summary of <https://fairytail.fandom.com/wiki/Ice-Make:_Saucer> |
| Eagle: several ice eagles fly through the air at the opponents and avoid obstacles because the spell is dynamic | summary of <https://fairytail.fandom.com/wiki/Ice-Make:_Eagle> |
| Cold Excalibur: a variation of Ice-Make: Sword, an extremely large longsword, as sharp and durable as metal, whose slashes trail ice; named after the legendary sword; Gray used it against Hades | summary of <https://fairytail.fandom.com/wiki/Ice-Make:_Cold_Excalibur> |
| Ice-Make Unlimited: One Sided Chaotic Dance (many large ice swords on a spiral chain), Ice Geyser (a tower of spikes), Ice Bringer (two swords, a cross-shaped slash), Snow Beast (a chimera of dynamic spells, game only) | summaries of their pages; **not used** |
| Ur taught Gray (and Lyon) Ice-Make; her and Ultear's ice is shown purple | summary of the Ice-Make page for the colour; the teaching is memory |
| Gray is not bothered by cold (he walks through blizzards half dressed) | **memory**, not checked against a source; it is the inspiration for the passive, nothing more |
| The brief's mapping "Dynamic Ice-Make (Hammer, Cannon, Saucer)" | **differs from the wiki**: the wiki lists Hammer, Cannon and Saucer among Gray's STATIC spells. See the note below |

**A note on "static" and "dynamic".** The brief wanted a static branch (Shield, Floor, Wall) and a dynamic branch (Hammer, Cannon, Saucer). The wiki's definitions make a better rule for a game: *static = a construct that
stays where it was made* (a floor, a wall, a turret that stays and shoots), *dynamic = a construct that is sent* (a disc, a bird, a hammer that falls). The tree's two branches follow that rule and the node names say
which branch they belong to in the text. Cannon moved to the standing branch (it is a thing that stands and fires, exactly the wiki's "small ornate cannon"), Eagle (the one spell the wiki *calls* dynamic) was added to
the moving branch, and the wall is the Shield (the wiki has no separate wall).

Everything about numbers, ranges, shapes, which node hangs from which and what a "level" means is **design**.

## 3. The tree

```
y=1                         [shield]          [frost_body]
y=2             [floor]
y=3                         [cannon]
y=4 [lance]                                                [excalibur]
y=5                         [hammer]
y=6             [saucer]
y=7                         [eagle]
     x=0         x=2          x=4               x=6        x=7
```

| Node (id) | Kind | Needs | SP per level (total) | Max | Skill / passive |
|---|---|---|---|---|---|
| Ice-Make: Lance (`lance`) | technique, root | nothing | 1,1,2,2,3 (9) | 5 | `nusmp:ft_ice_make_lance` |
| Static Ice-Make: Floor (`floor`) | technique, standing | lance L1 | 2,2,2,3 (9) | 4 | `nusmp:ft_ice_make_floor` |
| Static Ice-Make: Shield (`shield`) | technique, standing | floor L1 | 2,2,3,3 (10) | 4 | `nusmp:ft_ice_make_shield` |
| Static Ice-Make: Cannon (`cannon`) | technique, standing | floor L2 | 2,2,3,3 (10) | 4 | `nusmp:ft_ice_make_cannon` |
| Ur's Lesson (`frost_body`) | passive, standing | shield L1 | 1,2,2 (5) | 3 | `ice_frost_body` |
| Ice-Make: Saucer (`saucer`) | technique, moving | lance L1 | 2,2,2,3 (9) | 4 | `nusmp:ft_ice_make_saucer` |
| Dynamic Ice-Make: Eagle (`eagle`) | technique, moving | saucer L1 | 2,2,3,3 (10) | 4 | `nusmp:ft_ice_make_eagle` |
| Ice-Make: Hammer (`hammer`) | technique, moving | saucer L2 | 2,3,3,3 (11) | 4 | `nusmp:ft_ice_make_hammer` |
| Ice-Make: Cold Excalibur (`excalibur`) | **Secret Art** | shield L2 **and** hammer L2 | 4,5,6 (15) | 3 | `nusmp:ft_ice_make_excalibur` |

The Secret Art needs a node of each branch: a weapon that STANDS (the Shield's branch) swung by a hand that MOVES (the Hammer's). Prices follow the owner's rule (1 to 3 SP a level, 4 to 8 for the Secret Art); max levels 1 to 5.

**WHY this shape.** A maker's identity is choice: the Lance is the root so the book is useful at once, and then every SP decides whether the player builds a battlefield (Floor, Shield, Cannon, with the cold-proof
passive at the end of the standing branch) or throws ice (Saucer, Eagle, Hammer). The Eagle is a multi-target answer that needs no clear line, the Saucer a piercing line, the Hammer a heavy telegraphed blow: three
different answers to "what do I do about this enemy". The Secret Art asks for both, so nobody gets it by only ever playing one way.

## 4. The techniques

All numbers below are the shipped level tables of the route file (`cost` is a share of the maximum magicules, `cooldown` is in ticks, 20 ticks = 1 s). A technique reads them at the player's node level through the
core's `RouteRegistry.value(route, node, key, level, fallback)`, so a datapack can retune them; the Java keeps the level-1 value as the `fallback` and a unit test keeps the two equal. A "chill" is Slowness for a while plus a few frozen ticks (the game's own cold,
skipped for creatures that cannot freeze); it is soft control, so no ice technique needs the hard-control lock.

### Ice-Make: Lance (`lance`)
*What.* `count` spears fanned `spread` degrees apart round the gaze; each is a line `range` long (stopped by the first solid block) and `width` wide; a foe is hit once, by the first spear that reaches it, and chilled.

| level | damage | range | spears | spread | width | chill (s) | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 5 | 14 | 1 | 0 | 0.9 | 1 | 4 | 2% | 80 |
| 2 | 5.5 | 16 | 1 | 0 | 1.0 | 1.5 | 6 | 2% | 75 |
| 3 | 6.5 | 18 | 2 | 7 | 1.0 | 2 | 8 | 2.5% | 70 |
| 4 | 7.5 | 20 | 3 | 8 | 1.1 | 2.5 | 10 | 2.5% | 65 |
| 5 | 8 | 22 | 3 | 9 | 1.2 | 3 | 12 | 3% | 60 |

*Tell and counter.* None (at most 8 raw). Counter: step out of the line; walls stop it. *Effects.* the mod's Heavenly Ice Fang (`ICE_FX1`) along each line, a small shatter (`ICE_FX3`) on each foe.
*WHY.* The fan widens what the technique covers without multiplying damage on one foe, which keeps it inside the balance law (one hit per target).

### Static Ice-Make: Floor (`floor`)
*What.* Where the caster looks (up to `reach` blocks, then down onto the ground) the ground freezes for `life` ticks: every half second every fair foe in `radius` is slowed (Slowness `slow_amp`+1) and gets `chill` frozen
ticks; the caster and the party inside get Speed I (the slide of the story). It does no damage of its own.

| level | radius | life | slow level | frozen ticks a pulse | reach | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|
| 1 | 3.5 | 80 | I | 20 | 8 | 6 | 2.5% | 200 |
| 2 | 4 | 100 | II | 25 | 9 | 8 | 3% | 190 |
| 3 | 4.5 | 120 | II | 30 | 10 | 10 | 3% | 180 |
| 4 | 5 | 140 | III | 35 | 11 | 12 | 3.5% | 170 |

*Tell and counter.* No damage, so no tell; the drawing (the mod's Winter Field) shows the circle for its whole life. Counter: do not stand in it. Takes one of the caster's four construct slots (section 6).
*WHY.* Floor is the cleanest "static" spell: nothing is thrown, the ground changes. No blocks may change on this route, so it is a circle on the clock; it can never be kept up (the budget check forces the cooldown
to be longer than the life) and never roots (Slowness III at most), so it needs no hard-control gate.

### Static Ice-Make: Shield (`shield`)
*What.* A wall of ice `width` wide and `height` tall, 1.6 thick, stands 2.2 blocks in front of the caster, turned the way the caster faces, for `life` ticks. Every tick every projectile inside it or about to cross it is
deleted, unless it was fired by the caster or a party member. Walking foes are NOT stopped. The caster also gets Absorption (`absorb` hit points: 4, 8 or 12).

| level | width | height | life | absorb | cost | cooldown |
|---|---|---|---|---|---|---|
| 1 | 4 | 3 | 60 | 4 | 3% | 300 |
| 2 | 5 | 3 | 80 | 4 | 3% | 280 |
| 3 | 6 | 3.5 | 100 | 8 | 3.5% | 260 |
| 4 | 7 | 3.5 | 120 | 12 | 4% | 240 |

*WHY.* The story's shield protects what is behind it; the game's equivalent of "what is behind it" is "arrows and spells coming from outside". Checking every tick and testing the arrow's NEXT move (not only where
it is) matters: an arrow flies about 3 blocks a tick and a wall 1.6 thick would be skipped by a check of positions alone (the unit test `aFastArrowCannotSlipThroughTheWallBetweenTwoTicks`). A wall that traps
walkers would be hard control, which the owner limits to one use per twelve seconds, so it is a projectile wall only. Friends' projectiles pass so the wall protects the group and never its own archers.
The drawing is a row of the mod's Wedge Field effects standing for the wall's whole life and one eruption of wedges when it rises.

### Static Ice-Make: Cannon (`cannon`)
*What.* A turret `reach` blocks in front of the caster: for `life` ticks, every `interval` ticks, `shots` times, it shoots the nearest fair foe within `range` that it can see (no block between), `damage` and a short chill. If nothing
is in sight when a shot is due, that shot is not fired.

| level | damage | shots | interval | range | life | reach | cost | cooldown |
|---|---|---|---|---|---|---|---|---|
| 1 | 4 | 3 | 18 | 12 | 60 | 4 | 3% | 260 |
| 2 | 4.5 | 4 | 16 | 13 | 70 | 4 | 3% | 250 |
| 3 | 5 | 5 | 14 | 14 | 76 | 5 | 3.5% | 240 |
| 4 | 5.5 | 6 | 12 | 15 | 78 | 5 | 3.5% | 230 |

*WHY.* The one construct that acts by itself, as the story's cannon does; it is a point and a drawing (an Ice Wedge eruption, a frost patch, a Heavenly Ice Fang as the shell), not an entity. Its life is tied to the shots
by the budget check (`shots * interval` must fit in `life`, and `life` is at most 10 s), and one cannon at a time per cast, so the caster who wants more places it where the foes will come.

### Ice-Make: Saucer (`saucer`)
*What.* A disc flies from the caster's hand along the gaze for `range` blocks (stopped by the first wall) at `speed` blocks a tick. Each fair foe on the path (up to `targets`, nearest first) is cut once, at the tick the
disc reaches it; from level 3 the disc turns at the end and comes back, and each foe is cut a second time on the return.

| level | damage | range | width | returns | speed | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|
| 1 | 5 | 12 | 1.0 | no | 1.4 | 6 | 2.5% | 100 |
| 2 | 6 | 14 | 1.1 | no | 1.4 | 8 | 2.5% | 95 |
| 3 | 7 | 16 | 1.2 | yes | 1.5 | 10 | 3% | 90 |
| 4 | 8 | 18 | 1.3 | yes | 1.5 | 12 | 3% | 85 |

*WHY.* A disc that travels needs a timetable, not an entity: the cut on each foe is scheduled for `ceil(distance / speed)` ticks, so the damage lands when the picture arrives (`IceNumbers.saucerDelay`), the picture is the mod's
Ice Wedge Lance drawn out and back, and no foe is cut before the disc reaches it. The return cut is later than the outward one (the unit test checks it), which also puts it past the creature's own protection for most foes.

### Dynamic Ice-Make: Eagle (`eagle`)
*What.* `count` eagles fly at the nearest foes within `range`, taking them in turn, never more than two on one foe (the second trails the first by 12 ticks); each strikes when it lands (`ceil(distance / speed) + 2` ticks):
`damage` and a chill. No line of sight is needed (the eagles dodge obstacles, as the wiki says). No foe in range: refused before it costs anything.

| level | damage | eagles | range | chill (s) | speed | cost | cooldown |
|---|---|---|---|---|---|---|---|
| 1 | 4 | 2 | 16 | 1 | 1.0 | 3% | 140 |
| 2 | 4.5 | 3 | 18 | 1.5 | 1.1 | 3% | 130 |
| 3 | 5 | 4 | 20 | 2 | 1.2 | 3.5% | 120 |
| 4 | 5.5 | 5 | 22 | 2.5 | 1.3 | 3.5% | 110 |

*WHY.* Dynamic Ice-Make in the wiki's words, and the right tool against foes behind cover, where the Lance and the Saucer fail. The two-per-foe cap (`IceNumbers.eagleAssignment`, tested for every combination of up to 8
eagles and 6 foes) is what stops five eagles from being 27 raw on one target: at most 11 raw on one foe, spread over a crowd otherwise.

### Ice-Make: Hammer (`hammer`)
*What.* Aim at a foe (the one nearest the crosshair that you can see, within an 18 degree cone and `range`). A ring of telegraph dust of the blow's size is drawn at once on the ground where it stands, a column of dust rises
there and a frost patch spreads; after `windup` ticks a giant ice fang drops the last blocks and the blow lands on whoever stands in the ring: `damage`, Slowness II for `slow_seconds` and 60 frozen ticks.

| level | damage | radius | wind-up | range | slow (s) | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|
| 1 | 8 | 2.5 | 16 | 14 | 2 | 6 | 3% | 180 |
| 2 | 10 | 3 | 15 | 16 | 3 | 8 | 3.5% | 170 |
| 3 | 12 | 3.5 | 14 | 18 | 3 | 10 | 4% | 160 |
| 4 | 14 | 4 | 12 | 20 | 4 | 12 | 4% | 150 |

*Tell and counter.* The ring, the column and the wind-up ARE the tell (the owner's line is 8 raw; this hits for 8 to 14, so the wind-up is never under 8 ticks and the budget check requires it). Counter: leave the ring
before the wind-up ends. With no foe in sight it is refused before it costs anything. If the caster dies or leaves, it does not fall.
*WHY.* "It hangs where you can see it" is the whole point of the story's hammer; the place is fixed when it is cast, so dodging is possible for a player or a boss. It is the only ice technique that hits harder than 8.

### Ice-Make: Cold Excalibur (`excalibur`, Secret Art)
*What.* The cast shows three lines of telegraph dust (the two edges and the middle of the arc) and motes gather in the hand; after `windup` ticks one huge arc is cut from the position and gaze the caster had when it was cast:
every fair foe inside the cone and in the clear from the sword's origin (up to `targets`) takes `damage`, a heavy chill (Slowness II for `slow_seconds`, 100 frozen ticks) and a shove back.

| level | damage | range | half angle | wind-up | slow (s) | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|
| 1 | 20 | 9 | 50 | 18 | 4 | 8 | 6% | 1100 (55 s) |
| 2 | 24 | 10 | 55 | 18 | 5 | 10 | 7% | 1000 |
| 3 | 28 | 11 | 60 | 16 | 6 | 12 | 8% | 900 (45 s) |

*Tell and counter.* The lines and the 16 to 18 tick wind-up are the tell; the counter is to leave the arc. *Limits.* At most 28 raw to a foe (the Secret Art cap is 30), at most 8% of the magicules, at least 45 s of cooldown.
*WHY.* The story's sword is "extremely large, sharp as metal, slashes trail ice": a wide arc and a lingering cold. Fixing the arc at the moment of the cast (not where the caster turns during the wind-up) makes
the dust an honest promise.

### Ur's Lesson (`frost_body`, passive)
*What.* Level 1: freezing damage no longer touches the owner, and the frost that powder snow builds up is wiped every second. Level 2 and 3: whoever strikes the owner in melee is chilled (Slowness I for 2 or 3 s and 40 or 60
frozen ticks), at most once every 80 or 60 ticks.
*WHY.* The cold cannot hurt an Ice-Make mage (memory, see section 2). Freezing damage is decided in the game's incoming-damage event, which can be cancelled before health is touched, and so is the melee attacker, so both
halves sit there; the rest between chills is kept in the owner's own data, so a swarm cannot turn it into a permanent freeze.

## 5. How a key press becomes an effect (the hook, in plain words)

The same as the Sky route (`ft_sky.md`, section 5): the player buys a node, the server records its level and teaches the skill; a key press reaches `TreeSkill.onRelease`, which asks
`SkillTreeApi.canCast(player, route, node)` (the Skill Tree switched on, route unlocked, node bought, route's class active) and refuses with "This technique is locked" and no other change when the answer is no; otherwise the technique
reads its numbers for that node level with `RouteRegistry.value` (the core's lookup, which answers the route's level-1 fallback when anything is unknown), pays through `AbilityKit.pay`, acts through `AbilityKit.hit` (damage through the balance law), schedules what lands later on the tree's clock, and `TreeSkill` starts the cooldown from
the table. Ice-Make uses no hard control at all, so it never calls `AbilityKit.control`. The passive sits on game events and asks `SkillTreeApi.isNodeActive` each time.

## 6. The constructs: why there are four and why they are not entities

The owner's rules for effects are strict: at most four spawned entities, none living more than ten seconds, no block changes, no vanilla particle spam. A floor, a wall and a cannon are things that *stand*, and the
natural way to build them (blocks, or armour stands) is forbidden or limited. So a construct here is **an effect plus a short rule on the server's clock**: a circle that chills (Floor), a box that eats projectiles
(Shield), a point that shoots (Cannon). The owner's limit of four is applied to constructs: how many of a caster's constructs still stand is a small array of expiry ticks in the player's own saved data
(`nusmp_ft_ice_live`), so there is no static state to clear when a server stops or a player leaves; `IceNumbers.record` forgets the fallen ones and clamps every life to ten seconds; a fifth construct is refused before it
costs anything. The shield erases projectiles with the idea of the shared weapon-art eraser rewritten for the tree (isolation rule), plus one rule that matters in a party fight: projectiles fired by the caster or a party member pass.

## 7. The route file and why the Java is built this way

**The route file** has exactly the structure explained, field by field, in `ft_sky.md`, section 6 (one file per route, `values` tables by level with names that are a contract with the code and the budget check, parents
as `{node, level}`, costs per level, a Secret Art with two parents). Ice-Make adds nothing to the schema. An abridged node:

```json
{ "id": "shield", "name": "Static Ice-Make: Shield", "parents": [ { "node": "floor", "level": 1 } ],
  "cost": 2, "costs": [2, 2, 3, 3], "max_level": 4, "skill": "nusmp:ft_ice_make_shield",
  "values": { "width": [4, 5, 6, 7], "height": [3, 3, 3.5, 3.5], "life": [60, 80, 100, 120], "absorb": [4, 4, 8, 12],
              "cost": [0.03, 0.03, 0.035, 0.04], "cooldown": [300, 280, 260, 240] },
  "x": 4, "y": 1, "icon": "minecraft:iron_chestplate" }
```

| File | Why |
|---|---|
| `ft/IceMakeSkills` | The register (one per route; its static initialiser wires the clock, the passive and the self-test, so no integration line is needed; deleting the route removes it completely). |
| `ice/IceRoute` | Every id and colour of the route in one place; the JSON, the register, the passive and the tests must agree. |
| `ice/IceNumbers` (pure) | The fallback table; the fan of the Lance; the saucer's timetable; the eagles' share-out; the wall as an oriented box with `crossedBy`; the construct ledger; the chill; the whole-route check. |
| `ice/IceConstructs` | The ledger in the player's data, the chill, who is a friend among players, the projectile eraser. |
| `ice/Ice*Skill` (eight) | One technique each; they write only `cast`. |
| `ice/IcePassives` | Ur's Lesson on the incoming-damage event and the per-player tick, behind `isNodeActive`. |
| `ice/IceSelfTests` | Section 9. |
| `kit/FtTaskQueue`, `FtClock`, `FtBudget`, `FtWorld`, `FtTreeSkill`, `FtRig` | Shared with the Sky route: the bounded queue ordered by tick; the clock that drains it on the server tick; the balance law as functions; the small world questions and the fair-target rule (not the party, not their pets, not villagers, other players only when PvP is on); the base class that reads numbers (with the core's `RouteRegistry.value` and the route's fallback), pays the cost, takes the cooldown from the table and shows the icon; the stage of a self-test. There is no number-lookup class of my own on purpose (see `ft_sky.md`, section 7). |

## 8. The balance law and how it is enforced

The owner's limits (design.md section 6) are checked on the numbers by `FtBudget`, on the shipped file at build time and on the loaded file in the game.

| Rule | Value | Ice-Make route |
|---|---|---|
| Raw damage per hit, ordinary | 4 to 14 | 4 to 14 (only the Hammer is above 8) |
| Raw damage, Secret Art | at most 30 | 20, 24, 28 |
| Hit above 8 raw | needs a tell (wind-up of at least 8 ticks) | Hammer (12 to 16), Excalibur (16 to 18) |
| Cooldown | at least 3 s; Secret Art at least 45 s | 60 ticks up; 900 ticks up |
| Cost | 1 to 4% of maximum magicules; Secret Art at most 8% | 2% to 4%; 6% to 8% |
| Targets | at most 12 | at most 12 |
| Hard control | only after `AbilityKit.control` | none used |
| Block changes | none | none |
| Spawned entities | at most 4, none over 10 s | none; at most 4 constructs of at most 10 s |
| Effects | `VfxSpawn` shapes and sounds; no vanilla particles | `Tells` dust for the telegraph only |

Raw damage per second against ONE target (damage of a cast over its cooldown in whole seconds): Lance 1.25 to 2.67, Cannon 0.92 to 2.75 (all shots on one target), Saucer 1.0 to 3.2 (the top needs both passes), Eagle 1.14 to 1.83 (two eagles
on one foe), Hammer 0.89 to 1.75, Excalibur 0.36 to 0.62. The Secret Art is a burst with a long rest; the ordinary techniques sit in a band of about 1 to 3 raw a second per target at equal investment.

## 9. Tests

* **Unit tests** (pure): `kit/FtBudgetTest`, `kit/FtTaskQueueTest`, `ice/IceNumbersTest`: the shipped file passes the whole check; the file text follows the schema; the skill ids of the file are the ones the code registers and
  every node is the root, the Secret Art or in a branch; the fan; the eagles' share-out for every combination; the wall (a wall facing +z, one turned to +x, a diagonal one; a fast arrow cannot slip through);
  the saucer's timetable out and back; the ledger (four at most, the fallen forgotten, no life over 200 ticks); the chill cap; **a hammer with no wind-up is refused**. `kit/RegisterWiringTest` reads the text of the register, of `IceRoute` and of each skill's
  constructor and asks what the core's in-game test `tree.core.routes` asks: every registered skill claims a node of the shipped file, that node names exactly this skill, every skill node is claimed by exactly one class.
  Mutation checks done (sampling the wall every 4 blocks instead of every 0.4 fails the arrow test; raising the heal cap fails the heal tests; swapping the lance and floor ids in the register fails the wiring test with a sentence naming both
  classes; sources restored and compared byte for byte).
* **The real core's checks, run on this file** (the core's `SkillTreeFilesTest` and `IsolationTest`, run from this worktree so that they read the shipped route files and scan the sources; and a scratch main that prints the schema's problems and
  warnings): `RouteSchema` finds **0 problems and 0 warnings** in `ice_make.json` (5135 bytes of the 64 KB a route may take); with stfa's two files added the cross-file rules still hold (distinct books, classes and skills); no Black Clover grimoire import.
* **In-game self-test `tree.ft.ice_make`** (key prefix `tree`): the loaded route passes the whole check; every skill id of the file is a registered `FtTreeSkill` of this route and node; the passive of the file is implemented;
  WITHOUT the route the real `onRelease` of every technique refuses (no cooldown, no cost, nobody touched, no construct built, freezing damage not cancelled); WITH the route, at level 1 and at the top level, the lance, saucer,
  eagles and cannon hurt a dummy, the eagles refuse with no foe, the hammer and the sword do NOT hurt before their wind-up is over and then do, the hammer refuses with no foe in sight, the floor slows and chills the
  dummy and carries the caster, the shield deletes a foreign arrow and keeps the caster's own and gives absorption, four floors are built and the fifth is refused without costing anything, and the passive cancels freezing
  damage and chills a melee attacker only from level 2 and only once per rest. With an empty route registry it passes and says so; with the Skill Tree switched off in the config (`skilltree.enabled=false`, when the gate refuses every
  cast by design) it still checks the data and the wiring and says that the casts were skipped.

## 10. Decisions and alternatives rejected

* **Static and dynamic by the wiki's definition, not the brief's list** (section 2).
* **Constructs are effects on the clock, not entities or blocks** (section 6): the owner forbids block changes and limits entities; a wall of armour stands would also have been targetable and killable.
* **The wall stops projectiles, not walkers.** A wall that traps is hard control, limited to one use per twelve seconds; a projectile wall is what "protects whatever is behind it" means in a shooter.
* **Eagles do not need a clear line, the Saucer and Lance do.** The wiki says the eagles dodge obstacles because they are dynamic; it is also what separates the two branches in play.
* **At most two eagles on a foe.** Five on one target is 27 raw in a second, over a Secret Art's cap per target.
* **Saucer returns from level 3 as a second cut, not as a second disc entity.**
* **No cooldown reduction for the dynamic branch** ("dynamic Ice-Make is quicker to cast"): `TreeSkill.cooldownTicks(int)` receives only the node level, not the player, so a passive cannot shorten it. Listed as a contract wish in the report.
* **No Ice Geyser, Ice Bringer, Unlimited, Snow Beast.** Nine nodes is the budget (7 to 10) and each would repeat a shape the tree has (a line of spikes ~ the Lance, two swords ~ the Sword, many swords ~ the Secret Art).
* **Reusing the Black Clover Ice Magic effects (`ICE_FX1..3`, `ICE_WEDGE_FX1..3`)** is allowed (the shapes are neutral enum values) and gives the route the same quality as the grimoire's ice with no new art; the grimoire's CODE
  is not imported.
* **No new item, texture, model or sound.** Skill icons are vanilla textures; the node icons are item ids.
* **The core's `RouteRegistry.value`, not a lookup of my own** (an early draft class was deleted when the core arrived with the same promise; see `ft_sky.md`, section 7).
* **A skill's identity is tested from the source text** (`RegisterWiringTest`): the register, the id holder and the skill's constructor must agree with the file, and the classes cannot be loaded by a pure test.

## 11. Integration notes (for the lead)

* **No integration line is needed** for this route (the register wires itself). Optionally `IceMakeSkills.init()` may be called explicitly; it is idempotent.
* **Depends on the finished core (merge stcore first):** the code calls `RouteRegistry.value(...)` and `SkillTreeApi.isEnabled()`, which the contract stub in the lead's branch does not have yet.
* **Lang:** `$S/hero/frag/stfb.lang.json` (shared with Sky Dragon Slayer): `nusmp.skill.<id>` and `nusmp.skill.<id>.description` for the 8 skills of this route (the pattern the game resolves, verified on the existing `nusmp.skill.firebolt`; the brief said `skill.nusmp.<id>`).
* **Config:** none. **Commands:** none new. **Quest keys:** none. **Shared files edited:** none. **Persistent data keys added to the player:** `nusmp_ft_ice_live` (the construct ledger) and `nusmp_ft_ice_rest` (the thorns rest).

## 12. What is NOT verified

Nothing here has run in the game. It was type-checked against the API stubs AND the finished core's real sources (the only errors in these files are the stub artefact "MobEffectInstance is abstract" on correct
`new MobEffectInstance(...)` calls) and the pure logic was unit-tested with the repository's JUnit shim (47 tests of the whole job pass).

* The look of every effect is taken from the layers' Javadocs, not seen. Least certain: `ICE_WEDGE_FX2` used as a standing wall (it is a ground-sigil field with a rim of fans; a row of small ones may read as a hedge of ice, not a wall), `ICE_FX1` with power 2.6 as a dropping hammer, `WIND_EMPEROR` in ice colours as the sword arc.
* Arrow speed and the 0.4-block sampling: the unit test proves the geometry; whether a real arrow's `getDeltaMovement` at the moment of the tick is the move it is about to make (it is, in vanilla) is from memory.
* `LivingEntity.canFreeze`, `setTicksFrozen`, `getTicksRequiredToFreeze` behave as in vanilla 1.21.1 (the frozen count falls by 2 a tick when not in powder snow, so a chill lasts a second or two); the slow of a high count comes from the game's own powder-snow modifier. Names are verified, behaviour is not.
* `LivingIncomingDamageEvent` cancelling `DamageTypes.FREEZE` really prevents the damage in NeoForge 21.1.259 (the event is cancellable by contract; not run).
* Skill icons are vanilla textures chosen by name (`item/trident`, `block/packed_ice`, `block/blue_ice`, `item/crossbow_standby`, `item/prismarine_shard`, `item/feather`, `item/mace`, `item/diamond_sword`); a wrong name shows the missing-texture square.
* The self-test drives the techniques through `castDirect`; only the REFUSAL path goes through the real `onRelease`.
* The finished core's `canCast` was READ (engine switch, route, node, active class unless creative); the self-test sets the active class and skips the casts (saying so) when `skilltree.enabled` is false. The core's in-game test
  `tree.core.routes` was not run; its logic was reproduced on the source text (`RegisterWiringTest`) and on the files by the real `RouteSchema` / `RouteIndex`.
* The numbers are design, not playtested: they obey the owner's law (checked), whether a level-4 Cannon feels right is a question for a player.
