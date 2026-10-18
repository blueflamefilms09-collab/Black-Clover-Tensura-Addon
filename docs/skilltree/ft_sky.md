# Sky Dragon Slayer: the healer's route of the Skill Tree (`nusmp:sky_dragon_slayer`)

Part of the 1.62.0 Skill Book & Skill Tree job (design: `docs/skilltree/design.md`, sections 2, 6 and 11). Built by `stfb`. This page is the WHY of every
file of the route, the canon it is taken from, every number, and an honest list of what has not been run. Its sister page is `ft_ice.md` (Ice-Make); both routes share the small
toolkit described in section 8.

**New or replacement (CLAUDE.md rule 5).** The whole route is NEW: the mod had no skill tree, no SP and no Sky Dragon Slayer. Nothing is replaced or removed. The only existing
files touched are the two stubs the contract left for this job (`ft/SkyDragonSkills.java`, `ft/IceMakeSkills.java`), whose `SKILLS` declaration is kept exactly.

## 1. At a glance

| | |
|---|---|
| Route id / file | `nusmp:sky_dragon_slayer` = `src/main/resources/data/nusmp/skill_trees/sky_dragon_slayer.json` |
| Class id | `sky_dragon_slayer` |
| Book | `nusmp:sky_dragon_slayer_book`, named *Wing of Tender Tempests* ("A feather-light codex whose pages turn themselves for anyone who reads with a gentle heart.") |
| Colour / icon | `#6EC3F7` (sky blue) / `minecraft:feather` |
| Loot weight / duplicate book | 2 (the brief) / 3 SP |
| Nodes | 9: 7 techniques (one is the Secret Art) and 2 passives |
| Cost of the whole route | 77 SP |
| Play style | support: heal, cleanse, empower, with a wind offence that shoves and slows rather than kills |
| Code | `skilltree/ft/SkyDragonSkills` (register) and `skilltree/ft/sky/**`; the shared toolkit is `skilltree/ft/kit/**` |
| Lang | `nusmp.skill.ft_sky_*` and `nusmp.skill.ft_sky_dragons_*`: 14 lines (7 skills, a name and a description each), see section 11 |

## 2. Canon, and where each fact comes from

WebSearch with `allowed_domains: ["fairytail.fandom.com"]` returns summaries of the wiki pages (the wiki itself cannot be fetched from the build machine). "Summary" below means a
search summary of that page; "memory" means my own knowledge of the series that no source checked; "design" means my own decision for the game. Nothing is quoted at length.

| Fact | Where from |
|---|---|
| Sky Dragon Slayer Magic is a Caster Magic, an Ancient Spell, a Lost Magic and a Dragon Slayer Magic that uses air | summary of <https://fairytail.fandom.com/wiki/Sky_Dragon_Slayer_Magic> |
| Its healing is a lost art that can nearly fully cure most injuries and neutralise strong poisons, is draining, and **the user cannot heal themselves** | summary of the same page |
| It also supports: boosts the physical abilities of the user and allies, grants temporary abilities (also to weapons), removes abnormal conditions ("Enchanter") | summary of the same page |
| Its offence is large whirlwinds, wind-based slashing and wind propulsion for speed | summary of the same page |
| Wendy reaches Dragon Force by consuming air rich in Ethernano (not used in the tree) | summary of the same page |
| Sky Dragon's Roar: a tornado-like blast gathered and released from the mouth; a variant, "Roar + Cure", adds her healing | summary of <https://fairytail.fandom.com/wiki/Sky_Dragon's_Roar> |
| Sky Dragon's Wing Attack: a whirlwind from each arm that hits the surroundings; a mimic of Natsu's Fire Dragon's Wing Attack | summary of <https://fairytail.fandom.com/wiki/Sky_Dragon%27s_Wing_Attack> and Wendy's page |
| Sky Dragon's Claw: she jumps and a large wind falls as her leg swings down (not used; see section 10) | summary of the spell list |
| Sky Dragon's Wave Wind: a large, powerful tornado made by a swing of the hand | summary of <https://fairytail.fandom.com/wiki/Sky_Dragon's_Wave_Wind> |
| Sky Dragon's Breath: she raises her arms and sends gusts with healing properties at a badly hurt person, who may still need rest | summary of <https://fairytail.fandom.com/wiki/Sky_Dragon's_Breath> |
| Sky Dragon's Sigh is a different, video-game-only attack, not a heal | summary of <https://fairytail.fandom.com/wiki/Sky_Dragon's_Sigh> |
| Troia: a ball-shaped healing spell; she touches the target with the sphere to affect the sense of balance, which cures motion sickness; the effect is short and does not work twice in a row; she cannot cast it while sick herself | summary of <https://fairytail.fandom.com/wiki/Troia> |
| Arms (attack up, can hit several allies), Vernier (speed up, several allies), Armor (defence up), and the doubled "Ile" versions | summary of <https://fairytail.fandom.com/wiki/Arms>, `/Vernier`, `/Armor`, `/Ile_Arms`, `/Ile_Vernier` |
| Heal: a long-lost style that can drain poison and, at high cost, revive someone near death (not a node; its poison cure became Troia's cleanse) | summary of <https://fairytail.fandom.com/wiki/Heal> |
| Shattering Light: Sky Drill (the Secret Art): wide stance, arms spread, a fast wind barrier traps her and her opponent, her arms turn counter-clockwise, the wall squeezes in and throws the opponent up, who falls; Porlyusica passed the instructions on from Grandeeney and warned that careless practice can hurt Wendy's body; Ezel cut through the barrier with Mikazuki in the Tartaros arc | summary of <https://fairytail.fandom.com/wiki/Shattering_Light:_Sky_Drill> |
| Milky Way, her other Secret Art, summons the soul of a dead dragon (not used) | summary of <https://fairytail.fandom.com/wiki/Wendy_Marvell> and <https://fairytail.fandom.com/wiki/Grandeeney> |
| The brief's "Shredding Wing" | **not found** in any search summary; I did not invent a canon source for it. The offence branch uses the real Wave Wind instead |
| The brief's "Sky Dragon's Sky Drill" | the real name is *Shattering Light: Sky Drill* |
| Wendy is the Sky Dragon Slayer of Fairy Tail, raised by Grandeeney | memory (very well known; not checked) |

Everything about numbers, ranges, shapes, which node hangs from which and what a "level" means is **design**.

## 3. The tree

Layout on the canvas (x across, y down; the root at the left, the two branches, the Secret Art at the right):

```
y=1                         [wave_wind]
y=2             [wing_attack]
y=3                         [cushion]
y=4 [roar]                                          [sky_drill]
y=5                         [troia]
y=6             [breath]
y=7                         [enchant]   [aura]
     x=0         x=2          x=4        x=6          x=7
```

| Node (id) | Kind | Needs | SP per level (total) | Max | Skill / passive |
|---|---|---|---|---|---|
| Sky Dragon's Roar (`roar`) | technique, root | nothing | 1,1,2,2,3 (9) | 5 | `nusmp:ft_sky_dragons_roar` |
| Sky Dragon's Wing Attack (`wing_attack`) | technique, offence | roar L1 | 2,2,2,3 (9) | 4 | `nusmp:ft_sky_dragons_wing_attack` |
| Sky Dragon's Wave Wind (`wave_wind`) | technique, offence | wing_attack L2 | 2,3,3 (8) | 3 | `nusmp:ft_sky_dragons_wave_wind` |
| Updraft (`cushion`) | passive, offence | wing_attack L1 | 1,1,2 (4) | 3 | `sky_cushion` |
| Sky Dragon's Breath (`breath`) | technique, healing | roar L1 | 1,2,2,2,3 (10) | 5 | `nusmp:ft_sky_dragons_breath` |
| Troia (`troia`) | technique, healing | breath L2 | 2,2,3 (7) | 3 | `nusmp:ft_sky_troia` |
| Arms & Vernier (`enchant`) | technique, healing | breath L2 | 2,3,3 (8) | 3 | `nusmp:ft_sky_arms_vernier` |
| Armor Aura (`aura`) | passive, healing | enchant L1 | 2,2,3 (7) | 3 | `sky_aura` |
| Shattering Light: Sky Drill (`sky_drill`) | **Secret Art** | wave_wind L2 **and** troia L2 | 4,5,6 (15) | 3 | `nusmp:ft_sky_drill` |

The Secret Art needs a node of each branch (the offence branch and the healing branch), which is the owner's "needs two branches". Prices follow the owner's rule: 1 to 3 SP a level for
ordinary nodes, 4 to 8 for the Secret Art; max levels 1 to 5.

**WHY this shape.** A support class must be *playable alone* and *wanted in a party*. The root is an attack (the Roar), so a player who has only the book can fight; the healing branch
(Breath, Troia, Arms & Vernier, Armor Aura) is what a party wants; the offence branch (Wing Attack, Wave Wind, Updraft) is what keeps the healer alive and busy when nobody needs mending. The
Secret Art sits where both meet, as the story's Secret Art does (a trap round her and her opponent). Eight techniques and passives under one root would have been a list, not a tree; two
branches with a passive at the end of each make the choices real (a player with 20 SP cannot have everything).

## 4. The techniques

All numbers below are the shipped level tables of the route file (`cost` is a share of the caster's maximum magicules, `cooldown` is in ticks, 20 ticks = 1 s). A technique reads them at the
player's node level through the core's `RouteRegistry.value(route, node, key, level, fallback)`, so a datapack can retune them; the Java keeps the level-1 value as the `fallback` and a unit test keeps the two equal.

### Sky Dragon's Roar (`roar`)
*What.* A cone of wind from the mouth: every fair target in the cone and in line of sight takes `damage` and is shoved back by `knockback`; from level 4 allies in the cone are also mended
(`heal`, the "Roar + Cure" of the story).

| level | damage | range | half angle | knockback | heal | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|
| 1 | 5 | 7 | 20 | 0.6 | 0 | 6 | 2% | 100 |
| 2 | 5.5 | 8 | 22 | 0.8 | 0 | 7 | 2% | 95 |
| 3 | 6.5 | 9 | 24 | 1.0 | 0 | 8 | 2% | 90 |
| 4 | 7 | 10 | 26 | 1.2 | 3% | 9 | 2.5% | 85 |
| 5 | 8 | 11 | 28 | 1.4 | 5% | 10 | 2.5% | 80 |

*Tell and counter.* No tell: it never exceeds 8 raw, the owner's line for needing one. Counter: step out of the cone; a heavy target barely moves (its own knockback resistance applies).
*Effects.* `WIND_GALE` along the cone, `WIND_RING` round the caster, `SEAL_WOUND` ribbons to mended allies; sounds `ENDER_DRAGON_GROWL` and `BREEZE_SHOOT`.
*WHY.* A cone is the shape of a breath and exists in the shared kit; the shove is what makes a wind attack feel like wind and is soft control (no hard-control gate is needed).

### Sky Dragon's Wing Attack (`wing_attack`)
*What.* A whirlwind round the caster: every fair target within `radius` takes `damage` and is thrown outward.

| level | damage | radius | knockback | targets | cost | cooldown |
|---|---|---|---|---|---|---|
| 1 | 4 | 3.5 | 0.8 | 6 | 2% | 90 |
| 2 | 5 | 4 | 1.0 | 8 | 2% | 85 |
| 3 | 6 | 4.5 | 1.2 | 10 | 2.5% | 80 |
| 4 | 7 | 5 | 1.4 | 12 | 2.5% | 70 |

*Tell and counter.* None needed (at most 7 raw). Counter: stay out of the circle round the caster.
*Effects.* `WIND_RING` and two `WIND_SLASH` crescents to the left and right (the two wings); `ENDER_DRAGON_FLAP`, `PLAYER_ATTACK_SWEEP`.
*WHY.* The answer to being surrounded, which the Roar's cone cannot give. It is the first offence node because it is the escape tool of a healer.

### Sky Dragon's Wave Wind (`wave_wind`)
*What.* A tornado that walks forward along the caster's flat gaze for `distance` blocks (stopped by the first wall), hitting the fair targets inside its circle: each target is hit at most `hits`
times and never twice within 10 ticks, at most `targets` creatures are ever hit; every hit slows (Slowness I, 1.5 s). It lingers one second where it stops.

| level | damage | radius | distance | hits | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|
| 1 | 4 | 1.8 | 9 | 2 | 8 | 3% | 160 |
| 2 | 5 | 2.1 | 10 | 2 | 10 | 3% | 150 |
| 3 | 6 | 2.4 | 11 | 3 | 12 | 3.5% | 140 |

*Tell and counter.* None needed (at most 6 raw a hit). Counter: leave its path; it is slow (0.45 blocks a tick).
*Effects.* a `WIND_GALE` lane for the path and a small `WIND_TORNADO` left every third tick at its position.
*WHY.* The story's version is "a large tornado by a swing of the hand": it travels. The server has no tornado entity (the owner limits spawned things), so the tornado is a position on the
tree's clock (section 7). The 10-tick gap between hits is the length of a creature's own protection after a blow, so no hit is wasted on a protected target.

### Updraft (`cushion`, passive)
*What.* Falls hurt `fall_reduction` less: 40%, 70%, 100% at levels 1 to 3.
*WHY.* The story gives her wind propulsion and the game decides fall damage in one event (`LivingFallEvent`, which carries a multiplier for exactly this); a passive there changes nothing else.

### Sky Dragon's Breath (`breath`)
*What.* The heal. Allies in `radius` (the caster, the party, teammates and tamed pets of those) that are wounded are mended by `heal` of their own maximum health, the most wounded first, at most
`targets` of them. The caster receives only `self_share` of it. Nobody wounded: refused before it costs anything.

| level | heal | radius | targets | self share | cost | cooldown |
|---|---|---|---|---|---|---|
| 1 | 8% | 6 | 4 | 50% | 3% | 200 |
| 2 | 10% | 7 | 5 | 50% | 3% | 190 |
| 3 | 13% | 8 | 6 | 60% | 3% | 180 |
| 4 | 16% | 9 | 7 | 60% | 3.5% | 170 |
| 5 | 20% | 10 | 8 | 70% | 3.5% | 160 |

*Limits.* Every point goes through the balance law's heal (a heal within 20 s of another is weaker), the amount can never exceed 25% of the target's maximum health whatever the data says, and the
route test refuses a table above it.
*WHY.* "Wounded first" because a healer that spends a cast on a scratched friend while another is dying is broken; "self share" because in the story the sky dragons' healing cannot be turned on the
caster, and a fraction keeps that as a cost without making a lone player useless. The ribbon (`SEAL_WOUND`) shows who was reached.

### Troia (`troia`)
*What.* A cleanse. For each ally in `radius` (at most `targets` of those who need it) it removes up to `strips` harmful effects, nausea first (the sickness the spell is named for), then poison,
wither, blindness, darkness, slowness, weakness, hunger, mining fatigue, levitation, then anything else the game marks harmful. From level 2 it also puts out flames and thaws a chilled body.
Nobody to cleanse: refused before it costs anything. It restores no health.

| level | strips | radius | targets | thaw and extinguish | cost | cooldown |
|---|---|---|---|---|---|---|
| 1 | 1 | 5 | 3 | no | 2% | 200 |
| 2 | 2 | 7 | 5 | yes | 2% | 160 |
| 3 | 3 | 9 | 8 | yes | 2.5% | 120 |

*WHY.* "A ball that restores balance" is, in a game, a cleanse; making it heal nothing keeps it from being a second heal outside the heal cap. Effects are found by their category in the effect
registry, so effects from other mods that call themselves harmful are cleansed without the code knowing them.

### Arms & Vernier (`enchant`)
*What.* Allies in `radius` (the caster first, then the nearest, at most `targets`) get Strength I (Arms) and Speed (Vernier) for `seconds`; at level 3 the speed is one step higher (the "Ile" chant).

| level | seconds | speed amplifier | radius | targets | cost | cooldown |
|---|---|---|---|---|---|---|
| 1 | 10 | I | 6 | 4 | 3% | 400 |
| 2 | 13 | I | 8 | 6 | 3.5% | 380 |
| 3 | 16 | II | 10 | 8 | 4% | 360 |

*WHY.* The duration is always shorter than the cooldown (the budget check enforces it), so the buff can almost, never entirely, be kept up. Strength is left at I at every level: a second
step is a very large damage bonus and the doubled chant is already a gift. The effects are ambient and without particles (the mod purged particle spam).

### Armor Aura (`aura`, passive)
*What.* Every two seconds the owner and every ally within `radius` (6, 8, 10) get Resistance I for five seconds, so it never lapses while friends stand close and ends within five seconds of
leaving. *WHY.* Armor is the third of Wendy's enchantments; as an aura it needs no key, so a healer's hands stay free for the other nodes.

### Shattering Light: Sky Drill (`sky_drill`, Secret Art)
*What.* The sequence: the cast shows the tell at once (a ring of dust at the edge of the circle, motes gathering, a wind mantle on the caster); after the wind-up (10 ticks) the circle (fixed at the cast
position) pulses three times, ten ticks apart: every fair foe inside takes `pulse_damage` and is slowed (Slowness II), and at the first pulse every wounded ally inside is mended by `heal` in ONE heal (the caster half);
then it bursts: every foe inside takes `burst_damage` and, if the shared hard-control lock allows it (`AbilityKit.control`) and the foe is not a boss, is thrown into the air; otherwise it is only shoved back.

| level | radius | wind-up | pulses | pulse damage | burst damage | per target total | heal | targets | cost | cooldown |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 5 | 10 | 3 | 4 | 12 | 24 | 12% | 8 | 6% | 1200 (60 s) |
| 2 | 5.5 | 10 | 3 | 4.5 | 13.5 | 27 | 15% | 10 | 7% | 1100 |
| 3 | 6 | 10 | 3 | 5 | 15 | 30 | 18% | 12 | 8% | 1000 |

*Tell and counter.* The ring of dust and the 10-tick wind-up are the tell; the counter is to walk out of the circle before the first pulse (foes outside are never hit, and the drawn wall shrinks to 60%
only as a picture: it does not pull anyone in). *Limits.* The whole sequence is at most 30 raw per target (the budget check sums pulses and burst), the cost is at most 8%, the cooldown at least 45 s,
the lift is hard control and so uses the one-per-12-seconds lock.
*WHY one big heal and not three small ones.* The balance law makes a heal within 20 s of another 40% as strong; three pulses of healing would have healed about half what the table says. One heal at the
first pulse is also the "heal" half of the owner's "heal-and-burst" and arrives while the fight is still on.

## 5. How a key press becomes an effect (the hook, in plain words)

1. The player buys a node in the Skill Tree screen; the server records the level in the player's `nusmp:skill_tree` and teaches them the ManasCore skill the node names (`reconcileSkills`).
2. The skill appears in Tensura's skill menu and is bound to Tensura's own skill keys, like every other spell of the mod.
3. A key press reaches `TreeSkill.onRelease` (the shared contract class). It asks `SkillTreeApi.canCast(player, route, node)`: the Skill Tree switched on in the config (`skilltree.enabled`) **and** route unlocked
   **and** node level at least 1 **and** (config `requireActiveClass`, on by default) the route's class is the active one (creative players pass the class check). If not, the player sees "This technique is locked"
   and nothing else happens: no cooldown, no cost, no effect. A skill kept after a refund, or granted by hand, is stopped here.
4. Otherwise `TreeSkill` reads the node level and calls the technique's `cast(...)` (the class in `ft/sky/`). The technique reads its numbers for that level from the loaded route with `RouteRegistry.value`
   (through `FtTreeSkill.num`; it answers the technique's own level-1 default when the route, the node or the table is unknown, so a cast never throws), pays its share of
   the magicules (`AbilityKit.pay`), does its effect (damage through `AbilityKit.hit`, healing through `AbilityKit.heal`, hard control only after `AbilityKit.control`), and returns true.
5. `TreeSkill` then starts the cooldown from the node's `cooldown` table and adds a mastery point.
6. A technique that finishes later (the wave wind, the drill) leaves a task on the tree's clock; the clock runs it on the server tick that is due and the task checks that the caster is still there.
7. The two passives do not go through a key at all: they sit on game events and ask `SkillTreeApi.isNodeActive` each time.

## 6. The route file and why it is built this way

The file is one JSON object in the schema of design.md section 2. JSON has no comments, so the reason for every part is here.

| Part | Why |
|---|---|
| One file per route in `data/nusmp/skill_trees/` | A new magic is data: a server owner or the owner can retune or add a tree with a datapack and `/reload`, with no rebuild. The route id comes from the file name. |
| `magic`, `name`, `desc` | The screen groups routes by magic family and shows the name and the text; they are text, so they live with the data. |
| `book`, `book_name`, `book_lore` | There is ONE book item whose component names the book; the route that lists that id opens. The name and the line of lore are the route's, so a new route needs no new item. The name is mine (*Wing of Tender Tempests*) in the style of the owner's *Fang of Smoldering Insanity*. |
| `color`, `icon` | The screen draws the route and its book in this accent; an item id is the cheapest icon (no new art). |
| `class_id` | The class a player is while this route is active; unique among routes. |
| `duplicate_sp` (3), `loot_weight` (2) | What a second copy of the book is worth, and how often this route's book drops in the dungeon compared with the others (the brief gave 2). |
| `nodes[].id`, `parents` | Unique lower-case ids; a parent is `{node, level}` so a Secret Art can ask for a level of two different nodes. Parents come before children in the file, so the screen and the validation can walk the file once. |
| `cost` and `costs` | `costs` is the price of each level; `cost` is the price of the first one and the default for the others. |
| `max_level` | 1 to 5; the level is what the SP buys. |
| `skill` | The ManasCore skill the node grants; absent for a passive. |
| `passives` | The ids of the passive behaviours a node switches on; the code that implements them is `SkyPassives`. |
| `values` | One table of numbers per name, one entry per level. This is where balance lives. The names are the contract with the code and the budget check: `damage`, `cooldown`, `cost`, `targets`, `heal`, `windup` have a meaning in the law (section 9); the others are read by one technique. |
| `x`, `y` | The place on the canvas, in a grid of 13 by 10: root left, branches up and down, the Secret Art at the right. |
| `icon` | An item id that the node is drawn with (`minecraft:breeze_rod` for the drill, `minecraft:feather` for the Updraft ...). |

An abridged node, to read the structure (the real file is the source):

```json
{ "id": "sky_drill", "name": "Shattering Light: Sky Drill", "parents": [ { "node": "wave_wind", "level": 2 }, { "node": "troia", "level": 2 } ],
  "cost": 4, "costs": [4, 5, 6], "max_level": 3, "skill": "nusmp:ft_sky_drill",
  "values": { "radius": [5, 5.5, 6], "windup": [10, 10, 10], "pulses": [3, 3, 3], "pulse_damage": [4, 4.5, 5], "burst_damage": [12, 13.5, 15],
              "heal": [0.12, 0.15, 0.18], "targets": [8, 10, 12], "cost": [0.06, 0.07, 0.08], "cooldown": [1200, 1100, 1000] },
  "x": 7, "y": 4, "icon": "minecraft:breeze_rod" }
```

## 7. Why the Java is built this way (file group by file group)

Every class has a Javadoc whose first paragraph starts with `Why:`; this is the summary.

**`ft/SkyDragonSkills` (the register).** A ManasCore skill can only be added while the skill registry is open, so the skills are registered through the route's own `DeferredRegister` that
`SkillTreeSystem.init` attaches to the mod bus. One register per route means two builders never edit the same file. Its static initialiser also hooks the route's runtime (the clock, the passives, the
self-test): the first use of the class is `SkillTreeSystem.init`, on both sides, so **no integration line is needed**, and deleting the route's file, register and package removes the route completely.
`init()` is idempotent.

**`sky/SkyRoute`.** Every id and colour of the route in one place, because the JSON, the register, the passives and the tests must agree and a typo would silently make a technique uncastable.

**`sky/SkyNumbers` (pure).** The fallback table, the heal cap, the wounded-first choice, the cleanse order, the drill timeline and the whole-route check. Pure so a test can run it; the fallback exists
so that a missing table never throws or casts for nothing.

**`sky/SkyAllies`.** Who is a friend: the caster, the party (`SystemParty`, read-only, the seam the dungeon quests use), teammates, tamed pets of those. Decided from facts the game keeps, nothing stored.

**`sky/Sky*Skill` (seven techniques).** Each writes only `cast`; the gate, the cooldown and the mastery point are done by the shared `TreeSkill`; the numbers, the cost and the cooldown by `FtTreeSkill`.

**`sky/SkyPassives`.** A passive lives where the game decides the thing it changes: the fall event for the Updraft, the per-player tick for the aura; both behind `SkillTreeApi.isNodeActive`, so a
refunded node is gone on the next beat with nothing to clean up.

**`sky/SkySelfTests`.** Section 10.

**The toolkit `ft/kit/**`** (shared with Ice-Make):

| Class | Why |
|---|---|
| `FtTaskQueue` (pure) | "Run this in some ticks" without a thread or an entity: a queue ordered by tick, ties in order of adding, bounded so a technique can never grow the server's memory. |
| `FtClock` | The game-bus tick (`ServerTickEvent.Post`) drains the queue by the world's game time; every task runs inside a catch; cleared when the server stops (the only static state, cleared at the right moment); `drain` is public so the self-tests play time forward exactly as the server does. The older runtime that does the same belongs to the Black Clover grimoire, which the owner's isolation rule keeps out of the tree. |
| `FtBudget` (pure) | The balance law as functions over the `values` tables, used by the unit test on the shipped file and by the self-test on the loaded file. |
| `FtWorld` | Gaze, ground, fair targets, nearest, shove, launch, sounds: the small questions every technique asks, and the single place that decides who is a fair target (not the party, not their pets, not villagers, other players only when PvP is on). It only LOOKS at blocks; it never writes one. |
| `FtTreeSkill` | The four things every technique repeats (numbers, cost, cooldown from the table, icon) on top of `TreeSkill`; `castDirect` is the one seam for the self-tests. |
| `FtRig` | The stage of a self-test: a stand-in player high above the spawn column (only air, so scenery cannot make a correct technique look broken), a dummy, the route granted or revoked, a full pool, time that passes; everything it touched is put back. |

*There is deliberately no class here for reading a number.* An early draft had one (`FtNumbers`); when the core was finished it turned out to own exactly that lookup, `RouteRegistry.value(route, node, key, level, fallback)`,
with the same promise (the fallback when the route, the node or the table is unknown, never an exception in the middle of a cast). Keeping a second copy would have meant two lookups that could drift apart, so the draft class
was deleted and `FtTreeSkill.num` and the two passives call the core's method directly. The per-route `fallback` (the shipped level-1 value) stays in `SkyNumbers` / `IceNumbers`, because only the route knows its own defaults.

## 8. The balance law and how it is enforced

The owner's limits (design.md section 6) are checked on the numbers by `FtBudget`, on the shipped file at build time and on the loaded file in the game.

| Rule | Value | Sky route |
|---|---|---|
| Raw damage per hit, ordinary | 4 to 14 | 4 to 8 |
| Raw damage, Secret Art | at most 30 (over the whole sequence per target) | 24, 27, 30 |
| Hit above 8 raw | needs a tell (wind-up of at least 8 ticks) | only the drill's burst: wind-up 10 |
| Cooldown | at least 3 s; Secret Art at least 45 s | 70 ticks up; 1000 ticks up |
| Cost | 1 to 4% of maximum magicules; Secret Art at most 8% | 2% to 4%; 6% to 8% |
| Targets | at most 12 | at most 12 |
| Heal | at most 25% of maximum health per cast | at most 20% (Breath), 18% (drill) |
| Hard control | only after `AbilityKit.control` | only the drill's lift |
| Block changes | none | none |
| Spawned entities | at most 4, none over 10 s | none |
| Effects | `VfxSpawn` shapes and sounds; no vanilla particles | `Tells` dust for the telegraph only |

Raw damage per second against ONE target (damage of a cast over its cooldown), to compare techniques: Roar 1.0 to 2.0, Wing Attack 0.8 to 1.75, Wave Wind 1.0 to 2.6 (only if the target stays in it), Sky Drill 0.4 to 0.6.
The route is meant to heal and disrupt, so its offence is the lowest of the four Fairy Tail routes at equal investment.

## 9. Decisions and alternatives rejected

* **Wave Wind instead of "Shredding Wing" / Sky Dragon's Claw.** The brief named "Shredding Wing", which no source supports. Claw is canon but is a hop-and-kick, a second close-range attack beside the Wing Attack; Wave Wind adds a *travelling* shape the route lacked.
* **Troia is a cleanse, not a heal.** Canon is a balance spell; a second heal would have to share the heal cap and would crowd the Breath.
* **The caster heals only part of himself** (`self_share`) instead of nothing: canon says the user cannot heal themselves, but a route that is useless alone cannot be a route.
* **One heal in the drill, not three.** See section 4 (the 20-second decay).
* **No entity for the tornado, the floor, the wall.** The owner limits spawned entities and forbids block changes; a position on a clock does everything those would do.
* **A new clock instead of reusing the grimoire runtime (`SpellRuntime`).** It lives in the Black Clover package and is entangled with merged magic; the isolation rule forbids the dependency, and the clock is 100 lines.
* **State in the player's data and the entity's data, not in static maps.** Nothing here outlives a server except the clock's queue, which is cleared on stop.
* **Fallback = shipped level-1 value, not full tables.** The tables belong to the file; the fallback only has to keep a missing key from breaking a cast.
* **The core's `RouteRegistry.value`, not a lookup of my own.** See the note under the toolkit table in section 7: one lookup with one promise beats two that could drift.
* **A skill's identity is written three times and tested from the text.** The register names the id, the holder `SkyRoute` spells it, the skill's constructor names the node; the core's in-game test fails when they disagree,
  and the classes cannot be loaded by a pure test, so `RegisterWiringTest` reads the three spellings from the source text and asks the same questions at build time (rejected: reflection, which would need the game on the class path).
* **Passives behind `isNodeActive`, not the active class.** The brief says so; a passive costs nothing to keep and is part of owning the node.
* **The passive "eat the sky" (regain magicules from clean air)** was left out: canon says Dragon Slayers eat their element, but the mod's mana economy is the owner's, and a regeneration passive would have changed the balance of every cost in the route.
* **Cooldown reduction by a passive** was left out: `TreeSkill.cooldownTicks(int)` gets only the node level, not the player (see the report's contract wishes).

## 10. Tests

* **Unit tests** (pure, `src/test/java/.../skilltree/ft/`): `kit/FtBudgetTest` (every rule broken one at a time; the route rules), `kit/FtTaskQueueTest` (order, ties, bound, clear), `sky/SkyNumbersTest` (the shipped file passes
  the whole check; the file text follows the schema; the skill ids of the file are the ones the code registers; heal cap; wounded first; cleanse order; drill timeline; squeeze; fall factor; **a roar of 15 raw is refused**).
  `kit/RegisterWiringTest` reads the text of the register, the id holder and each skill's constructor and asks what the core's in-game test `tree.core.routes` asks of the loaded game: every registered skill claims a node of the
  shipped file, that node names exactly this skill, the route id is the file's, every skill node of the file is claimed by exactly one class (and a test feeds it doctored text to show it can fail).
  `kit/RouteFiles` reads the shipped files into the pure records (a lone number in `values` is read as a one-entry table, like the core's schema does). Run: `WT=<worktree> KEY=<key> bash $S/mega/jt.sh com.newuniverse.nusmp.skilltree.ft.sky.SkyNumbersTest ...`.
  Mutation checks done: raising `HEAL_MAX` from 0.25 to 0.5 fails `FtBudgetTest.targetHealAndLifeCaps` and `SkyNumbersTest.aHealIsAShareOfMaxHealthNeverOverTheCap`; sampling the wall every 4 blocks instead of 0.4 fails the Ice arrow test;
  pointing the Troia skill's constructor at the enchant node, and swapping two ids in the Ice register, each fail `RegisterWiringTest` with a sentence naming the node and the class (sources restored and compared byte for byte).
* **The real core's checks, run on these files** (the core's `SkillTreeFilesTest` and `IsolationTest`, run from this worktree so that they read `src/main/resources/data/*/skill_trees/*.json` and scan the sources; and a scratch main that
  prints the schema's problems and warnings): `RouteSchema` finds **0 problems and 0 warnings** in `sky_dragon_slayer.json` (4430 bytes of the 64 KB a route may take), the cross-file rules hold with the four Fairy Tail files together (books, classes and skills are
  distinct), and the isolation scan finds no Black Clover grimoire import.
* **In-game self-test `tree.ft.sky_dragon_slayer`** (key prefix `tree`, run by `/multiverse mega selftest`): the loaded route passes the whole check; every skill id of the file is a registered `FtTreeSkill` of this route
  and node; every passive of the file is implemented; WITHOUT the route the real `onRelease` of every technique refuses (no cooldown, no cost, nobody hurt, nobody buffed); WITH the route, at level 1 and at the top
  level, the roar, the wing attack and the wave wind hurt a dummy, the breath mends the wounded caster by no more than 25% and refuses when nobody is wounded, Troia strips exactly the effects the node level allows in the
  right order and refuses with nothing to strip, the enchant gives Strength and the speed step of the table, the drill does not hurt before its wind-up is over and then hurts the dummy and mends the caster, the Updraft
  changes a fall by the table's share and the aura covers its owner. With an empty route registry it passes and says so; with the Skill Tree switched off in the config (`skilltree.enabled=false`, when the gate refuses every
  cast by design) it still checks the data and the wiring and says that the casts were skipped.

## 11. Integration notes (for the lead)

* **No integration line is needed** for this route (the register wires itself). Optionally `SkyDragonSkills.init()` may be called explicitly; it is idempotent.
* **Depends on the finished core (merge stcore first).** The code calls `RouteRegistry.value(route, node, key, level, fallback)` and `SkillTreeApi.isEnabled()`, which the contract stub in the lead's branch does not have yet; against the
  stcore sources the route compiles with no error of its own. The skill ids, the node ids and the route id agree with what the core's `tree.core.routes` and `SkillGrants` demand (checked from the text by `RegisterWiringTest`).
* **Lang:** `$S/hero/frag/stfb.lang.json` (shared with Ice-Make): `nusmp.skill.<id>` and `nusmp.skill.<id>.description` for the 7 skills of this route. The brief said `skill.nusmp.<id>`; the pattern the game resolves
  (verified on the existing `nusmp.skill.firebolt`) is `<modid>.skill.<path>`, which the other builders use as well.
* **Config:** none. **Commands:** none new (`/multiverse tree ...` is the core builder's). **Quest keys:** none.
* **Shared files:** none edited.

## 12. What is NOT verified

Nothing here has run in the game. It was type-checked against the API stubs AND against the finished core's real sources (the only errors in these files are the stub artefact "MobEffectInstance is abstract", on 11 correct
`new MobEffectInstance(...)` calls; a deliberately wrong call to `RouteRegistry.value` was flagged, so the check is live), and the pure logic was unit-tested with the repository's JUnit shim (47 tests of the whole job pass; the mutants
of the budget, the wall and the wiring were killed).

* The look of every effect is taken from the layers' Javadocs, not seen; `WIND_TORNADO`, `SEAL_WOUND`, `SEAL_ORBIT` and `WIND_ZEPHYR` are the least certain to read as intended.
* `LivingEntity.knockback`, `LivingFallEvent.setDamageMultiplier`, `Entity.hurtMarked` and the effect constructor behave as in vanilla 1.21.1 (from memory of the source; the stubs only prove the names).
* The skill icons are vanilla textures chosen by name (`textures/item/goat_horn.png`, `elytra`, `wind_charge`, `glistering_melon_slice`, `milk_bucket`, `sugar`, `breeze_rod`); a wrong name shows the missing-texture square, nothing worse.
* The self-test drives the techniques through `castDirect` (the technique without the key-release bookkeeping); only the REFUSAL path goes through the real `onRelease`, because the success path needs a stored skill
  instance, which a stand-in player does not have.
* `TensuraStorages.getExistenceFrom` on a stand-in player: the test treats "no record" and "zero pool" as valid (it compares magicules only when there is a number).
* The numbers are design, not playtested: they obey the owner's law (checked), but whether a level-3 Breath feels right is a question for a player.
* The finished core's `canCast` was READ (engine switch, route, node, active class unless creative) and the self-test follows it: it sets the active class when it grants the route, and it skips the casts (reporting that it did)
  when `skilltree.enabled` is false. Whether a real key press arrives in `TreeSkill.onRelease` with a stored skill instance, as the self-test assumes for the refusal path, is the contract's promise, not something run here.
* The in-game test `tree.core.routes` was not run; its logic (route files load clean, every skill is a registered `TreeSkill` that agrees with its node, every registered skill is claimed by a node) was reproduced on the source text
  (`RegisterWiringTest`) and on the files by the real `RouteSchema` / `RouteIndex`, which is a strong hint and not the same as the game saying so.
