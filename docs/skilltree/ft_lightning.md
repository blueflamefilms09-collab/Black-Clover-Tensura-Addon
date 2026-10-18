# Lightning Dragon Slayer (1.62.0): the second Fairy Tail route of the Skill Tree, the Scale of Crackling Sovereignty

The route `nusmp:lightning_dragon_slayer` is the fast, stunning counterpart of the Fire route (`docs/skilltree/ft_fire.md`): ten nodes, eight techniques that are real ManasCore skills and two passives, a
root (Lightning Dragon's Iron Fist), three branches, and a Secret Art (Roaring Thunder) that needs two of them. Reading the **Scale of Crackling Sovereignty** (`nusmp:skill_book` with the book id
`nusmp:lightning_dragon_slayer_book`, made by the items builder) unlocks the whole route for the player, who then spends Skill Points on its nodes; every bought node teaches its technique, which is cast with
Tensura's own skill keys. Everything here runs on the server; the client only sees effect packets and the tree screen. Where the Fire route burns and pushes, this one NUMBS (a slowdown), JUMPS (arcs from foe
to foe), STUNS (two techniques, behind the balance law's single hard-control gate) and ARRIVES (a streak, a rush).

**New or replacement (CLAUDE.md rule 5).** All of it is NEW: nothing in the mod was a Lightning Dragon Slayer or an SP-priced route. Nothing is replaced or removed. KEPT and reused unchanged: `BalanceLaw` (every
hit, heal and the control gate), `VfxSpawn` and the existing effect shapes (`LIGHTNING_SPEAR`, `LIGHTNING_GOD`, `LIGHTNING_FIEND`, `FX_LIGHTNING_ARC`, `MAGIC_CIRCLE`, `MAGIC_CIRCLE_EXPLOSION`), `Tells` (the
zogratis warning dust), Tensura's energy helper, the self-test registry `labyrinth/mega/SelfTests`, and the icon framework `tools/gen_skill_icons.py`. The toolbox `skilltree/ft/dragon/**` (timers, geometry and
effect helpers, the budget rules, the base class and the in-game harness) is written ONCE for both Dragon Slayer routes and is described in `ft_fire.md` section 7; this page repeats only what it needs.
Nothing of the Black Clover grimoire is imported (the engine's `IsolationTest` passes on these files).

## 1. Canon, and what is from where

Research was done with web search limited to the Fairy Tail wiki (`fairytail.fandom.com`; the pages themselves cannot be fetched from the build machine, so every canon fact below comes from the search
**summaries** of those pages, marked *[summary]*; what I know from the series without a source on hand is marked *[memory]*; what I decided is marked *[design]*). A summary does not say which of the
listed pages a sentence came from, so the pages named for a fact are the ones the search returned for it, and the page of a single spell is named only when the search returned it.

| Canon fact | Source | Used for |
|---|---|---|
| Lightning Dragon Slayer Magic gives Laxus the power and characteristics of a Lightning Dragon; he can consume external lightning (not his own) to heal and replenish his power, which makes him immune to lightning *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon_Slayer_Magic , https://fairytail.fandom.com/wiki/Laxus_Dreyar | Eating Lightning (heals and recharges; your own lightning does not count) |
| Iron Fist: one fist engulfed in lightning, a punch that causes sheer blunt damage *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon_Slayer_Magic (the spell list; the spell's own page was not returned) | the root punch |
| Roar: lightning generated in the mouth and released as a concentrated, destructive blast; by moving the head while casting it can be used in an ARCHED trajectory; a target that survives is left numb and cannot escape the next attack *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon's_Roar | the Roar as a FAN of bolts, and the numbing of everything it touches |
| Breakdown Fist: lightning gathered around one fist, a Magic Seal appears in front of Laxus and an oversized fist made of lightning is fired at the target; it damages and STUNS, leaving the target open to the next attack *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon%27s_Breakdown_Fist | Breakdown Fist: needs a target, opens a magic circle, stuns |
| Heavenward Halberd: lightning forms between the palms into a large trident or spear and is thrown at the enemy (the pages describe its shape differently) *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon%27s_Heavenward_Halberd | the long piercing lane |
| Jaw: both hands clasped into one fist and brought down on the target, smashing into the ground and sending out a circular shockwave *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon's_Jaw | the close slam with a ring of impact |
| Thunder Palace: several lacrima filled with a great amount of lightning hang in the air in a circular formation and, when released, strike the area with bolts (said to be able to destroy a whole town if every orb is used) *[summary]* | the pages the search returned: https://fairytail.fandom.com/wiki/Lightning_Dragon_Slayer_Magic , https://fairytail.fandom.com/wiki/Laxus_Dreyar (the spell's own page was not returned) | Thunder Palace: a ring and an orb circle over the aimed place, bolts one after another |
| Roaring Thunder (Narumikazuchi), a Secret Art: the user rushes the target and releases a large, arching burst of lightning from the fist; it heavily damages and electrocutes the target and throws it back with great force; first seen against Jura Neekis in the Grand Magic Games *[summary]* | https://fairytail.fandom.com/wiki/Roaring_Thunder , https://fairytail.fandom.com/wiki/Episode_189 | the Secret Art: a rush, then a burst, a stun and a heavy shove |
| In lightning form Laxus moves freely at high speed and over long distances, can appear at a destination out of sight almost like a teleport, can turn himself into a bolt (from sparks to large pillars), and physical and magical attacks pass harmlessly through him while he is transformed *[summary]* | https://fairytail.fandom.com/wiki/Laxus_Dreyar , https://fairytail.fandom.com/wiki/Lightning_Magic , https://fairytail.fandom.com/wiki/Lightning_Dragon_Slayer_Magic | Lightning Flash (a streak that stops at a wall) and Lightning Body (sprint speed, a blow that passes through) *[design]* |
| Laxus received the magic when his father Ivan implanted a Dragon Lacrima derived from Elexion, the Lightning Dragon King, and kept the power after the lacrima was removed *[summary]* | https://fairytail.fandom.com/wiki/Elexion , https://fairytail.fandom.com/wiki/Laxus_Dreyar | NOT built: flavour only (the book lore is a line of my own) |
| NOT built, and why (section 8): Iron Hammer (a fist gathers lightning and punches the ground, tearing it up); Raging Bolt (a clenched fist raised, a large bolt falls from the sky on the target; its classification as Dragon Slayer magic is disputed); Lightning Dragon King Mode and Red Lightning Dragon Mode (strongest states); the video-game-only Lightning Plunge *[summary]* | https://fairytail.fandom.com/wiki/Lightning_Dragon's_Iron_Hammer , https://fairytail.fandom.com/wiki/Raging_Bolt , https://fairytail.fandom.com/wiki/Lightning_Dragon_King_Mode , https://fairytail.fandom.com/wiki/Red_Lightning_Dragon_Mode | documented flavour only |
| Dragon Slayers have keener senses and the advanced ones suffer motion sickness in vehicles; Dragon Force is the strongest state *[summary]* | https://fairytail.fandom.com/wiki/Dragon_Slayer , https://fairytail.fandom.com/wiki/Dragonization | NOT built (same as the Fire route) |

The names *Lightning Body*, *Lightning Flash* and *Eating Lightning* are this builder's names for canon behaviours (the series gives the speed, the pass-through and the eating, but no such spell names); the node
order, the prices, the numbers, the tell and the way each technique is shaped for a block game are **design**. The yellow of the route (`#F5D63B`) is *[design]*; *[memory]*: the lightning of the series is yellow-white.

## 2. The tree

Ten nodes, three branches from the root, one Secret Art that needs two of them. `x` is the column and `y` the row of the canvas (0..12 by 0..9). The map and the table below are written by the script that
also writes the route file and the Java number tables from one source, so they cannot disagree with the file in section 9.

```
      x=0                    x=2                    x=4                    x=6
y=0                                                 [Heavenward Halberd]
y=1                          [Roar]
y=2                                                 [Thunder Palace]
y=3
y=4   [Iron Fist]            [Breakdown Fist]       [Jaw]
y=5
y=6                                                 [Lightning Flash]      {Roaring Thunder}
y=7                          (Lightning Body)
y=8                                                 (Eating Lightning)

[ ] a technique   ( ) a passive   { } the Secret Art.   Lines, parent (level it must have) --> child:
  Iron Fist (3) --> Roar
  Iron Fist (2) --> Breakdown Fist
  Iron Fist (1) --> Lightning Body
  Roar (2) --> Heavenward Halberd
  Roar (3) --> Thunder Palace
  Breakdown Fist (2) --> Jaw
  Lightning Body (2) --> Lightning Flash
  Lightning Body (1) --> Eating Lightning
  Breakdown Fist (3) --> Roaring Thunder
  Lightning Flash (2) --> Roaring Thunder
```

| node | name | parents (node level) | SP per level | max | skill | kind |
|---|---|---|---|---|---|---|
| `iron_fist` | Lightning Dragon's Iron Fist | root | 1 each | 5 | `nusmp:ft_lightning_dragons_iron_fist` | active |
| `roar` | Lightning Dragon's Roar | iron_fist 3 | 2/3/3 | 3 | `nusmp:ft_lightning_dragons_roar` | active |
| `breakdown_fist` | Lightning Dragon's Breakdown Fist | iron_fist 2 | 2/3/3 | 3 | `nusmp:ft_lightning_dragons_breakdown_fist` | active |
| `lightning_body` | Lightning Body | iron_fist 1 | 1/2/2 | 3 | - | passive |
| `heavenward_halberd` | Lightning Dragon's Heavenward Halberd | roar 2 | 2/3/3 | 3 | `nusmp:ft_lightning_dragons_heavenward_halberd` | active |
| `thunder_palace` | Thunder Palace | roar 3 | 3 each | 3 | `nusmp:ft_thunder_palace` | active |
| `jaw` | Lightning Dragon's Jaw | breakdown_fist 2 | 2/3/3 | 3 | `nusmp:ft_lightning_dragons_jaw` | active |
| `lightning_flash` | Lightning Flash | lightning_body 2 | 2/2/3 | 3 | `nusmp:ft_lightning_dragons_flash` | active |
| `thunder_eater` | Eating Lightning | lightning_body 1 | 1/2/2 | 3 | - | passive |
| `roaring_thunder` | Roaring Thunder | breakdown_fist 3, lightning_flash 2 | 4/6/8 | 3 | `nusmp:ft_roaring_thunder` | Secret Art |

Total to buy every level of every node: 81 SP.

The Secret Art needs the Breakdown Fist at level 3 and the Lightning Flash at level 2 (two different branches). Before its first level can be bought, **17 SP** must have been spent on what it
needs (Iron Fist 2, 2 SP; Breakdown Fist 3, 8 SP; Lightning Body 2, 3 SP; Lightning Flash 2, 4 SP), so its first level is the 21st SP of the route at the earliest; buying every level of every node costs 81 SP.

### 2.1 Why this shape

* **One root, the Iron Fist, and every branch hangs on a different LEVEL of it** (Lightning Body at fist 1, Breakdown Fist at fist 2, Roar at fist 3), exactly as in the Fire route, so that levelling the root opens
  the tree in order, cheapest branch first, and the two Dragon Slayer routes feel like the same family with different hands.
* **Three branches, three ways to play**: the Roar branch is range and area (Roar, Heavenward Halberd, Thunder Palace), the Fist branch is the stun and the close fight (Breakdown Fist, Jaw), the Body branch is
  speed and staying power (Lightning Body, Lightning Flash, Eating Lightning).
* **The Secret Art needs the two halves of its own description**: canon says Laxus RUSHES the target and PUNCHES with lightning, so the Secret Art needs the Breakdown Fist (the punch, at 3) and the Flash (the
  rush, at 2). It does NOT need the Roar: the Fire route's Secret Art needs the Roar because the owner named that shape (Iron Fist, Roar, Secret Art), and nothing requires it here, so a Lightning player can reach the
  Secret Art without ever buying a breath. The Roar branch is the other road, to range and area.
* **The Eating Lightning passive is a dead end on purpose**: staying power is not on the road to the Secret Art; a player who wants it pays for it. Lightning Body (fist 1, 1 SP) is the doorway to the whole speed
  branch, so the speed branch starts as cheap as the Fire route's Eating Flame.
* **Layout**: depth goes to the right (columns 0, 2, 4, 6); the branches are stacked top to bottom (Roar branch, Fist branch, Body branch); the Secret Art stands alone in the last column, level with the Flash,
  with a line from the Breakdown Fist coming down to it.

## 3. The techniques

All numbers are the tables of section 6. "Tell" is the owner's rule that any technique above 8 raw damage shows what it is about to do (a line, a cone or a ring of coloured dust drawn by `Tells`, server side).
"Counter" is how a foe (a monster, or a player in PvP) avoids it. A cast that has to be refused (no foe, no room, riding a mount, not enough magicules) is refused BEFORE it is paid for.

**An honest note on the tell.** A technique that reaches farther than melee (more than about 7 blocks) or carries the caster forward has a real WIND-UP, and the wind-up is its tell: the dust is drawn at the cast
and the hit comes a few ticks later along the same line, ring or lane, so a foe has time to leave it (here: the Roar, the Breakdown Fist, the Heavenward Halberd, the Thunder Palace, Roaring Thunder). The short-reach
blows (the Iron Fist, the Jaw, the Flash; 7 blocks at most, and the Flash at most 6 raw) land at once: their dust is drawn at the moment of the hit, it shows where the technique went but is not a warning in time, and
their counter is distance. If the owner wants a wind-up on those too, the tables already take a `windup` key and `DragonTimers` already queues it.

| Technique | What it does | Tell | Counter |
|---|---|---|---|
| **Lightning Dragon's Iron Fist** (root, 5 levels) | A lightning-wrapped punch at the foes in a cone in front of you (35 to 45 degrees half angle, 3.5 to 4.5 blocks): the nearest 1, 1, 1, 2, 2 are hit for raw 6, 7, 8, 9, 11, numbed for 1 to 2.5 s (Slowness II, not hard control) and pushed a little (0.2 to 0.4). From level 2 the shock ARCS from the first foe hit to 1, 1, 2, 2 other foes within 4 to 5 blocks of it (levels 2 to 5) for raw 4, 5, 5, 6 and numbs them too. 1 to 1.5 percent of the magicules; the cooldown falls from 5 s to 3 s with the level; a punch that finds nobody still shows its bolt. | at levels 4 and 5 (raw 9 and 11): a line of dust from the fist to the first foe, at the moment of the hit | step out of the short cone; the arc needs a first foe to jump from |
| **Lightning Dragon's Roar** (3 levels) | You draw a breath (6, 5, 4 ticks: motes gather at your mouth, the cone and its centre line are drawn), then breathe a FAN of three bolts across a cone of 12, 15, 18 degrees half angle and 10, 12, 14 blocks, steered by where you LOOK when the breath leaves (canon: the head is moved while breathing). The damage is the cone itself: everything inside it, nearest first, up to 4, 6, 8 foes, takes raw 9, 11, 13 and is numbed for 1.5, 2, 2.5 s. 3, 3, 3.5 percent; cooldown 8, 7, 6 s. | the gathering motes, the cone edges and the lane line during the inhale (raw is always above 8) | leave the cone or break line of sight in the 4 to 6 ticks (0.2 to 0.3 s) of the inhale, or fight beyond 14 blocks |
| **Lightning Dragon's Breakdown Fist** (3 levels) | You aim at the FIRST foe in the line in front of you (10, 12, 14 blocks, a line 0.9 to 1.1 blocks wide, stopping at walls); with no foe in the line the cast is refused and nothing is paid. A magic circle opens before you and the line is drawn; 6, 5, 4 ticks later (0.2 to 0.3 s) a bolt in the shape of a giant fist flies along that same line and strikes the first foe in it: raw 9, 11, 13, then a STUN of 20, 30, 40 ticks (1 to 2 s) if the caster's shared hard-control turn (one per 12 s) is free, for as long as the balance law allows that target (at most 3 s on a player, 5 s on a mob, 1 s on a boss, halved when repeated); with the turn spent the foe is only numbed (Slowness III) for 2, 2.5, 3 s. A foe that has left the line is missed, and the next foe in it is struck instead. 2.5, 3, 3.5 percent; cooldown 7, 6, 5 s. | from the cast: the line of dust from you to the end of the lane, motes at the seal and the magic circle (raw is always above 8) | step out of the line in the wind-up; keep another foe, a pet or a wall in the line (the nearest foe in the line takes it); stay out of 10 to 14 blocks |
| **Lightning Dragon's Heavenward Halberd** (3 levels) | Lightning gathers between your palms (8, 7, 6 ticks, 0.3 to 0.4 s) and the lane is drawn; then a great spear is thrown along that lane and pierces: the lane is 18, 22, 26 blocks long and 0.8 to 1 block wide, stops at the first wall, and everything in it at that moment, nearest first, up to 3, 4, 5 foes, takes raw 10, 12, 14 and is numbed for 1, 1.5, 2 s. There is no flight time and no area; a pillar of lightning marks the end. 3, 3.5, 4 percent; cooldown 9, 8, 7 s. | from the cast: a line of dust along the whole lane and motes at your hands (raw is always above 8) | step out of the thin lane in the wind-up, or put a wall in it |
| **Thunder Palace** (3 levels) | You aim (the first foe on your look ray, else the first wall, else the ground below the end of the range of 10, 12, 14 blocks). A ring of dust of 4, 5, 6 blocks radius is drawn on the ground there, a circle of orbs hangs in the air above, and after 20, 18, 16 ticks the first of 4, 5, 6 bolts falls, then one more every 13, 12, 11 ticks, each at a random spot inside the ring drawn at the cast. 6 ticks before each bolt a column of dust marks its exact spot. Each bolt hits everyone within 1.8, 2, 2.2 blocks of its spot, nearest first, up to 6, 8, 10 foes, for raw 8, 9, 10 and numbs them for 1, 1.5, 2 s. 3.5, 4, 4 percent; cooldown 12, 10.5, 9 s. | from the cast: the ring and the orb circle; then a column of dust 6 ticks (0.3 s) before every bolt | leave the ring, or leave each column in the 6 ticks it is shown |
| **Lightning Dragon's Jaw** (3 levels) | Both hands come down in a slam on a spot 2, 2.5, 3 blocks in front of you: everyone within 3, 3.5, 4 blocks of the spot, nearest first, up to 5, 7, 9 foes, takes raw 8, 10, 12, is numbed for 1, 1.5, 2 s and is thrown outward from the spot (0.5 to 0.9). 2, 2.5, 3 percent; cooldown 7.5, 6.5, 5.5 s. | at levels 2 and 3 (raw 10 and 12): a ring of dust on the ground at the spot, at the moment of the hit | do not stand in front of the caster within 5 to 7 blocks; the ring is the area |
| **Lightning Flash** (3 levels, a name of my own) | You streak 6, 8, 10 blocks forward along the ground and stop at the first wall (no room, less than 1 block, is a refusal; so is riding a mount). Every foe within 1 to 1.2 blocks of the path, nearest first, up to 2, 3, 4, takes raw 4, 5, 6 and is numbed for 1, 1, 1.5 s. 2, 2, 2.5 percent; cooldown 5, 4.5, 4 s. | none needed (raw is 6 or less); a bolt is drawn along the path | stay out of the path |
| **Roaring Thunder** (Secret Art, 3 levels, 4/6/8 SP) | The nearest foe you can see in a narrow cone (30 degrees half angle) within 10, 12, 14 blocks is the target; with none, or on a mount, the cast is refused and nothing is paid. You RUSH to 1.6 blocks in front of it (stopping at the first wall), and 8, 7, 6 ticks later a BURST goes off on the spot where the ring of dust was drawn (3.5, 4, 4.5 blocks radius): the target, if still inside, takes raw 18, 24, 30 and every other foe inside takes raw 10, 12, 14 (up to 6, 9, 12 foes in all); all are thrown away from you (1.4 to 2.2); the target is STUNNED for 20, 30, 40 ticks if the caster's hard-control turn is free (otherwise heavily numbed, Slowness IV, for the same time). 6, 7, 8 percent; cooldown 60, 54, 45 s. | from the rush: a line along the rush and a RING on the ground where the burst will be, 6 to 8 ticks (0.3 to 0.4 s) before it | leave the ring in that window (the burst does not follow you: a target that has left the ring is missed), or have a way out faster than a sprint |
| **Lightning Body** (passive, 3 levels) | Your body hums with current. While you sprint you are quickened (Speed I at levels 1 and 2, Speed II at level 3, re-applied every second). A blow struck by an attacker (not fall, drowning, starvation or the void) passes through you with a chance of 8, 12, 16 percent, at most once every 60, 50, 40 ticks. | none (a passive) | none needed; out-wait the interval |
| **Eating Lightning** (passive, 3 levels) | Lightning does not hurt a dragon slayer, it feeds him. Lightning damage (the game's lightning bolt, Tensura's lightning magic and thunder breath) is cancelled when it comes from somebody else; the first such hit each second heals 25, 40, 60 percent of the cancelled damage and refills 2, 3, 4 percent of your magicules. Each second that you stand under the open sky in a thunderstorm you heal 2, 3, 4 percent of your health and refill 1, 1.5, 2 percent of your magicules. Your own lightning does not feed you (canon). | none (a passive) | fight without lightning |

Numbers that are not in a table: numbing is the game's Slowness (amplifier 1 for the ordinary techniques, 2 and 3 where it stands in for a stun the balance law did not allow); a stun is Tensura's paralysis
(`tensura:paralysis`) for the controlled number of ticks, or a strong Slowness if Tensura has no such effect; a hit is never reset by the hurt-frames trick (see section 8), so two hits on the same foe are always at
least 11 ticks apart; every technique stops if the caster dies or leaves between two parts; effects are drawn for at most 6 of the foes that were hit. Walls: a blow or a breath that comes out of the body (the
punch, the Roar, the Jaw, and the choice of the Secret Art's target) hits only foes the caster can SEE (the game's eye-to-eye line-of-sight test); a lane (the Breakdown Fist, the Halberd, the Flash) ends at the first wall;
a burst at a distant point (a bolt of the palace, the burst of the Secret Art) is not blocked by a wall between the point and a foe.

## 4. The ability hook, in plain words

1. A player presses a Tensura skill key. ManasCore finds the skill in the player's list and calls `onRelease` on it. Every Fairy Tail skill extends `TreeSkill` (the engine's class).
2. `TreeSkill.onRelease` asks ONE question first, `SkillTreeApi.canCast(player, route, node)`: is the engine switched on, has the player unlocked this route (consumed its book), has the player bought this node
   (level 1 or more) and, if the server asks for it, is this route their active class. If the answer is no the technique shows "This technique is locked" and NOTHING happens: no cost, no cooldown, no effect.
   So a skill that was learned by a command, or kept after a refund, still cannot be cast. The same question is asked by the skill menu (`canInteractSkill`), which hides what cannot be used.
3. If the answer is yes, `TreeSkill` checks the cooldown, reads the node level the player bought (1 to the maximum) and calls `cast`. In this route `cast` is `DragonSkill.cast`, which only wraps the technique
   in a safety net (an exception is caught, logged, answered with "The technique fizzles" and counted, never allowed out into the key handler) and calls the technique's `perform`.
4. `perform` is the technique: it reads its numbers for that level from the loaded route (`RouteRegistry.value`; a datapack that retunes the file changes the technique after `/reload`), refuses before paying when it
   must (no foe, no room, a mount), pays a share of the maximum magicules (`AbilityKit.pay`), hits through the balance law (`AbilityKit.hit` and `BalanceLaw`), numbs, stuns only after the control gate
   (`AbilityKit.control`), sends the effect (`AbilityKit.fx`) and the sound, and returns true. Delayed parts (the inhale, the seal and the gathering of the two lane techniques, the bolts of the palace, the burst of the Secret Art) are queued on `DragonTimers`.
5. `TreeSkill` then starts the cooldown from the node's `cooldown` table and gives one mastery point.

The passives have no key: they are NeoForge game-bus listeners (the damage-taken event and a once-a-second player tick), each starting with the same `canCast` question. The damage listener may CANCEL the
incoming damage (a bolt that is eaten, a blow that passes through), which is why those two decisions are the only places where this route touches damage the player RECEIVES.

## 5. The route file

`src/main/resources/data/nusmp/skill_trees/lightning_dragon_slayer.json` (the exact file is in section 9). The WHY of each structure in it:

| Structure | Why it is built that way |
|---|---|
| One file per route, id `nusmp:lightning_dragon_slayer` from the file name | A new magic is a file, not a Java class or an item: the engine's loader reads `data/<namespace>/skill_trees/*.json`, so a datapack can add, retune or replace a route and `/reload` applies it. |
| `magic` `fairy_tail`, `name`, `desc` | The family the screen groups routes under, the route's name and a sentence for its tooltip. The sentence sells the route's idea (speed, the storm, the Secret Art that arrives before its own sound). |
| `book`, `book_name`, `book_lore` | One registered Skill Book item says which book it is by a component; the route names the book, so the item, its name and its lore need no code. The name *Scale of Crackling Sovereignty* (the Fire book is a *Fang*) and the lore line are mine. |
| `color` `#F5D63B`, `icon` `minecraft:lightning_rod` | The screen draws an unlocked route in its colour and the book is tinted by it (one sprite serves every route). The yellow of lightning, as far from the Fire route's ember orange as the palette allows. |
| `class_id` `lightning_dragon_slayer` | The route is also the player's *class* while it is the active one; only the active class casts (config). Unique among routes. |
| `loot_weight` 2, `duplicate_sp` 3 | The Mega-Dungeon drop picks a route by weight: Fire is the common first (3), Lightning is rarer (2). A second copy of the book is worth 3 SP so a duplicate is never wasted. |
| `nodes` in order, parents first | The loader refuses a parent that comes later in the file: this also makes a cycle impossible, with no graph search. |
| `parents`: objects `{node, level}` | Every parent is written with the level it must have, so the tree has real depth: the Roar needs the Iron Fist at 3, the Breakdown Fist at 2, Lightning Body at 1. The Secret Art lists two parents, which is the "two branches". |
| `cost` and `costs` | `costs` lists the price of each level so later levels cost more; `cost` is the first price (what the screen shows as "from"). Ordinary levels cost 1 to 3 SP, the Secret Art 4, 6 and 8. |
| `skill` | The ManasCore skill the node grants at level 1: the id of a class registered in `LightningDragonSkills`. Passives have an empty `skill` and name their passive in `passives`. |
| `values` | Every number a technique uses, by level, so balance is data. The keys and their units are in section 6. A table has exactly one entry per level, so every level has its own number. The most keys on a node are 11 (the limit is 16). |
| `x`, `y` | The canvas cell. Columns grow with the depth of the tree, three branches fan out from the root, the Secret Art sits at the right. |
| `icon` (item id) | The node's picture in the screen: an item everyone knows (glowstone dust for the fist, a firework star for the roar, a spectral arrow for the seal-fist, a trident for the halberd, an end crystal for the lacrima, a bone for the jaw, an ender pearl for the flash, a lightning rod for eating, a nether star for the Secret Art). |

## 6. The numbers

Units: damage keys (`damage`, `chain_damage`, `splash_damage`) are RAW damage before the balance law scales it by the caster's EP (x1 at 1,000 EP up to x12) and caps it against players;
`range`, `width`, `radius`, `strike_radius`, `burst_radius`, `chain_range`, `reach`, `dash`, `rush_range` are blocks; `half_angle` is degrees; `numb_seconds` seconds; `cooldown`, `windup`, `delay`, `gap`,
`stun_ticks`, `evade_icd` ticks (20 per second); `cost_pct`, `heal_pct`, `recharge_pct`, `eat_recharge_pct`, `evade_pct` percent (of the maximum magicules, or of the maximum health for `heal_pct`, or a chance for
`evade_pct`); `targets`, `chain_targets`, `strikes` counts; `knockback` the strength of a push; `eat_heal_ratio` a fraction of the eaten damage; `speed_amp` the amplifier of the Speed effect (0 is Speed I).

**Lightning Dragon's Iron Fist** (`iron_fist`)

| value | L1 | L2 | L3 | L4 | L5 |
|---|---|---|---|---|---|
| `damage` | 6 | 7 | 8 | 9 | 11 |
| `numb_seconds` | 1.0 | 1.5 | 1.5 | 2.0 | 2.5 |
| `range` | 3.5 | 3.5 | 4.0 | 4.0 | 4.5 |
| `half_angle` | 35 | 35 | 40 | 40 | 45 |
| `targets` | 1 | 1 | 1 | 2 | 2 |
| `chain_targets` | 0 | 1 | 1 | 2 | 2 |
| `chain_range` | 4.0 | 4.0 | 4.5 | 4.5 | 5.0 |
| `chain_damage` | 4 | 4 | 5 | 5 | 6 |
| `knockback` | 0.2 | 0.2 | 0.3 | 0.3 | 0.4 |
| `cost_pct` | 1 | 1 | 1 | 1.5 | 1.5 |
| `cooldown` | 100 | 90 | 80 | 70 | 60 |

**Lightning Dragon's Roar** (`roar`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 9 | 11 | 13 |
| `range` | 10 | 12 | 14 |
| `half_angle` | 12 | 15 | 18 |
| `targets` | 4 | 6 | 8 |
| `numb_seconds` | 1.5 | 2.0 | 2.5 |
| `windup` | 6 | 5 | 4 |
| `cost_pct` | 3 | 3 | 3.5 |
| `cooldown` | 160 | 140 | 120 |

**Lightning Dragon's Breakdown Fist** (`breakdown_fist`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 9 | 11 | 13 |
| `range` | 10 | 12 | 14 |
| `width` | 0.9 | 1.0 | 1.1 |
| `stun_ticks` | 20 | 30 | 40 |
| `numb_seconds` | 2.0 | 2.5 | 3.0 |
| `windup` | 6 | 5 | 4 |
| `cost_pct` | 2.5 | 3 | 3.5 |
| `cooldown` | 140 | 120 | 100 |

**Lightning Body** (`lightning_body`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `speed_amp` | 0 | 0 | 1 |
| `evade_pct` | 8 | 12 | 16 |
| `evade_icd` | 60 | 50 | 40 |

**Lightning Dragon's Heavenward Halberd** (`heavenward_halberd`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 10 | 12 | 14 |
| `range` | 18 | 22 | 26 |
| `width` | 0.8 | 0.9 | 1.0 |
| `targets` | 3 | 4 | 5 |
| `numb_seconds` | 1.0 | 1.5 | 2.0 |
| `windup` | 8 | 7 | 6 |
| `cost_pct` | 3 | 3.5 | 4 |
| `cooldown` | 180 | 160 | 140 |

**Thunder Palace** (`thunder_palace`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 8 | 9 | 10 |
| `range` | 10 | 12 | 14 |
| `radius` | 4.0 | 5.0 | 6.0 |
| `strikes` | 4 | 5 | 6 |
| `strike_radius` | 1.8 | 2.0 | 2.2 |
| `delay` | 20 | 18 | 16 |
| `gap` | 13 | 12 | 11 |
| `numb_seconds` | 1.0 | 1.5 | 2.0 |
| `targets` | 6 | 8 | 10 |
| `cost_pct` | 3.5 | 4 | 4 |
| `cooldown` | 240 | 210 | 180 |

**Lightning Dragon's Jaw** (`jaw`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 8 | 10 | 12 |
| `radius` | 3.0 | 3.5 | 4.0 |
| `reach` | 2.0 | 2.5 | 3.0 |
| `targets` | 5 | 7 | 9 |
| `knockback` | 0.5 | 0.7 | 0.9 |
| `numb_seconds` | 1.0 | 1.5 | 2.0 |
| `cost_pct` | 2 | 2.5 | 3 |
| `cooldown` | 150 | 130 | 110 |

**Lightning Flash** (`lightning_flash`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 4 | 5 | 6 |
| `dash` | 6 | 8 | 10 |
| `width` | 1.0 | 1.1 | 1.2 |
| `targets` | 2 | 3 | 4 |
| `numb_seconds` | 1.0 | 1.0 | 1.5 |
| `cost_pct` | 2 | 2 | 2.5 |
| `cooldown` | 100 | 90 | 80 |

**Eating Lightning** (`thunder_eater`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `heal_pct` | 2.0 | 3.0 | 4.0 |
| `recharge_pct` | 1.0 | 1.5 | 2.0 |
| `eat_heal_ratio` | 0.25 | 0.4 | 0.6 |
| `eat_recharge_pct` | 2 | 3 | 4 |

**Roaring Thunder** (`roaring_thunder`)

| value | L1 | L2 | L3 |
|---|---|---|---|
| `damage` | 18 | 24 | 30 |
| `splash_damage` | 10 | 12 | 14 |
| `rush_range` | 10 | 12 | 14 |
| `burst_radius` | 3.5 | 4.0 | 4.5 |
| `knockback` | 1.4 | 1.8 | 2.2 |
| `stun_ticks` | 20 | 30 | 40 |
| `windup` | 8 | 7 | 6 |
| `targets` | 6 | 9 | 12 |
| `cost_pct` | 6 | 7 | 8 |
| `cooldown` | 1200 | 1080 | 900 |

Budget (design.md section 6), as enforced by `DragonBudget` on these tables at build time and on the live registry in the self-test: raw damage per hit 4 to 14 (this route: 4 to 14) and at most 30 for the Secret Art
(18 to 30 on the target, 10 to 14 on the others); cooldown at least 60 ticks (3 s) and at least 900 (45 s) for the Secret Art (this route: 60 to 240 ticks, and 900 to 1200); cost 1 to 4 percent and at most 8 for the
Secret Art (this route: 1 to 4, and 6 to 8); at most 12 targets (the Secret Art reaches 12); heals far below the quarter of the health (the largest bite is 4 percent); hard control only after the gate (the Breakdown Fist
and Roaring Thunder, 1 to 2 s); no block is placed or broken; no entity is spawned. A higher level is never worse: every power number rises and every delay falls with the level (a unit test says so).

### 6.1 Why these numbers

| Family | What the tables do | Why |
|---|---|---|
| `damage`, `splash_damage`, `chain_damage` | Raw 4 to 14 for the ordinary techniques (the Flash is the weakest at 4 to 6, the Halberd the strongest at 10 to 14), the Secret Art 18 to 30 on the target and 10 to 14 on the others. | `BalanceLaw` multiplies raw damage by the caster's EP scale (x1 at 1,000 EP, at most x12) and caps one hit on a player at 40 percent of the victim's health, so these numbers are the SHAPE of the power curve, not hit points. The budget of the design is raw 4 to 14 (30 for a Secret Art); the route spans it, from the speed technique that shocks as it passes to the spear that is thrown from far away. |
| `cooldown` | The punch 5 s down to 3 s; the other techniques 4 s to 12 s, falling with the level; the Secret Art 60, 54 and 45 s. | The budget's floors are 3 s and 45 s (Secret Art). The longer the reach or the area, the longer the wait (the Halberd 9 to 7 s, the Thunder Palace 12 to 9 s); the quick Flash and the root punch are the shortest. |
| `cost_pct` | 1 to 1.5 percent for the punch, 2 to 4 for the rest, 6 to 8 for the Secret Art. | A share of the MAXIMUM magicules, so it scales with the player. The budget's ceilings are 4 and 8. |
| `numb_seconds` | 1 to 3 s of Slowness on almost every technique. | The route's identity effect, and it is NOT hard control (a slowdown never takes the target's turn away), so it needs no gate and can be on every technique. |
| `stun_ticks` | 20, 30, 40 ticks on the Breakdown Fist and on Roaring Thunder. | The only hard control of the route, behind `AbilityKit.control` (one per caster per 12 s, shared by every skill). The balance law caps a stun at 3 s on a player, 5 s on a mob and 1 s on a boss and halves it when it is repeated within 30 s; 1 to 2 s is under every cap, so the number in the table is what a fresh target gets (a boss gets 1 s at most, and a repeat within 30 s gets half). |
| `range`, `width`, `radius`, `half_angle`, `reach`, `dash`, `rush_range` | The fist 3.5 to 4.5 blocks, the Jaw's spot 2 to 3 in front, the Flash 6 to 10, the Roar and the Breakdown Fist 10 to 14, the Halberd 18 to 26. | Each shape keeps its role; a level makes it more forgiving, not a different technique. The Halberd is the longest because it is the only way to hit something far away. |
| `chain_targets`, `chain_range`, `targets`, `strikes` | The arc 0 to 2 foes within 4 to 5 blocks; 1 to 12 foes per technique; 4 to 6 bolts of the palace. | The budget's cap is 12 foes (the Secret Art uses all 12); the nearest are hit first. The palace's bolts are capped at 8 follow-ups whatever the table says. |
| `windup`, `delay`, `gap` | The inhale of the Roar 4 to 6 ticks, the seal of the Breakdown Fist 4 to 6, the gathering of the Halberd 6 to 8; the palace's first bolt after 16 to 20 ticks and one every 11 to 13; the Secret Art's burst 6 to 8 ticks after the rush. | The wind-ups and the rush-to-burst gap are the tells (0.2 to 0.4 s: long enough to step out of a thin lane, short enough for the fast route). The farther a technique reaches, the longer its wind-up (the Halberd, 18 to 26 blocks, waits longest). 11 is the vanilla hurt-frame margin, so two bolts of the palace can hit the same foe, and none is wasted. |
| `knockback` | 0.2 to 0.4 for the punch, 0.5 to 0.9 for the Jaw, 1.4 to 2.2 for Roaring Thunder. | Strength of a push in the game's own units; the strongest push belongs to the Secret Art. Knock-back is not hard control. |
| `heal_pct`, `recharge_pct`, `eat_heal_ratio`, `eat_recharge_pct` | Under a thunderstorm: 2 to 4 percent of the health and 1 to 2 percent of the magicules per second; eating a bolt: 25 to 60 percent of the cancelled damage and 2 to 4 percent of the magicules, once a second. | Small on purpose, and rare (lightning is rare). `BalanceLaw.heal` also caps a heal at half the health and cuts it to 40 percent when the target was healed in the last 20 seconds. |
| `speed_amp`, `evade_pct`, `evade_icd` | Speed I (II at the top level) while sprinting; a chance of 8 to 16 percent to let a blow through, at most once per 3 to 2 s. | A chance with an interval, so it is a pleasant surprise and never a promise: with the interval a player cannot be made untouchable, and the chance is low enough that nobody can build a plan on it. |

## 7. WHY of the code, by file group

| Files | Why |
|---|---|
| `ft/LightningDragonSkills` (the register, owned by this builder) | A ManasCore skill must be registered before the skill registry freezes, and every route has its OWN register so two builders never edit one file. Its static initializer also starts the timer, the passives and the self-test, so the lead's `SkillTreeSystem` needs no extra line; `init()` is public and idempotent in case the lead prefers to call it. |
| `ft/lightning/LightningDragonsIronFistSkill`, `...RoarSkill`, `...BreakdownFistSkill`, `...HalberdSkill`, `ThunderPalaceSkill`, `...JawSkill`, `...FlashSkill`, `RoaringThunderSkill` | One class per active node because the id of a registered skill and the node it serves must be one to one (the engine's self-test fails on a mismatch), and because each technique is a different shape (a cone with arcs, a fan, a targeted seal, a thin lane, a schedule of bolts, a slam, a streak, a rush and a burst). Each is a ManasCore skill so it lives in Tensura's skill menu, on Tensura's keys, with its cooldown display. Everything runs on the server in `perform`; the client sees only effect packets. |
| `ft/lightning/LightningDragonSkill` | The route-wide part: the route id, the shipped numbers, the yellow of the warning dust and of the effects that take a colour. |
| `ft/lightning/LightningDragonPassives` | A passive is something that happens TO the player, so it is a game-bus listener on the damage-taken event (eat a bolt, let a blow through) and a once-a-second player tick (sprint speed, the storm's feeding), server side, each gated by `canCast`. The decisions are public static methods with plain arguments (and the dice roll as an argument) so the self-test can drive them (a fake player is invulnerable, so the real event never reaches it). |
| `ft/lightning/LightningDragonNumbers` | The shipped numbers once, in a PURE class: the fallback under the data, the node ids in one place, and the subject of the unit tests. |
| `ft/lightning/LightningDragonSelfTests` | The route's key of the CI smoke test, `tree.ft.lightning_dragon_slayer`, and the questions only the Lightning route can ask (do both passives answer?). |
| `ft/dragon/*` (shared with the Fire route) | See `ft_fire.md` section 7: the base class, the geometry and effect helpers, the server-tick queue, the budget and the tables as pure code, the harness. |
| `textures/skill/ft/ft_lightning_*.png`, `tools/skilltree/gen_ft_dragon_icons.py` | The skill menu needs a 32 px icon per skill. They are drawn with the owner's own icon framework so they sit next to the grimoire icons, in a steel frame of their own (the Fire icons have an ember frame). |
| `src/test/.../ft/dragon/*Test` | The route file, the code table, the budget and the source are checked against each other at build time (section 10). |

## 8. Decisions and alternatives rejected

* **Numbers in the JSON, a mirror of them in code (`LightningDragonNumbers`).** Same decision as the Fire route: a mirror and a test that proves it equals the file, so a cast can never read a hole and a retune is still data.
* **Delayed effects on a server-tick queue (`DragonTimers`).** Alternatives: a projectile or marker entity (the design allows very few spawned entities), the grimoire's spell runtime (forbidden by the isolation rule),
  a held skill's tick (the technique could be dropped by the key). The Thunder Palace is the clearest case: a palace is a SCHEDULE of bolts drawn at the cast, not an entity.
* **Wind-ups only where the reach is beyond melee.** The Breakdown Fist (10 to 14 blocks, a stun) and the Halberd (18 to 26 blocks) draw their lane at the cast and strike along it 4 to 8 ticks later, so a foe can step out of
  it; the Iron Fist, the Jaw and the Flash (7 blocks at most; the Flash is 6 raw at most) land at once, because a wind-up on a punch pressed every 3 to 5 s would turn the route of speed into a route of waiting, and a foe
  3 blocks away cannot react in 0.25 s anyway. The alternative, the same wind-up on everything, was rejected for that reason; the tables take a `windup` key on any node if the owner wants more (section 3).
* **The lane of a wind-up is fixed at the cast.** Alternative: read the aim again when the technique leaves (as the Roar does, on purpose: canon moves the head while breathing). For the Breakdown Fist and the Halberd the
  dust shows the lane, and a lane that moved afterwards would make the dust lie.
* **The Roar is a FAN of three bolts, but the damage is the CONE.** Alternative: damage per bolt, which would hit a foe in the middle of the cone three times (up to 39 raw) and make the aiming trivial; the cone
  makes the aiming forgiving and the damage predictable, and the three bolts are the picture.
* **Hard control only in the Breakdown Fist and Roaring Thunder, behind `AbilityKit.control`; everything else only numbs.** Alternative: a stun on the root punch, rejected because a technique pressed every 3 to 5 s
  would spend the caster's single shared 12-second turn on nothing. The numbing is a Slowness effect, which is not control in the balance law's sense.
* **Roaring Thunder's burst is FIXED on the ring.** Alternative: a burst that follows the target, rejected because it leaves no counter (the ring would be decoration), because it would hit a target that teleported
  away, and because the Fire route's fireball is already fixed on its ring: the same rule for every ring. The consequence is that the burst is not blocked by a wall between the ring and a foe, like every other area
  technique of the two routes (the rush itself stops at the first wall).
* **Walls.** What comes out of the body (the punch, the Roar, the Jaw, the choice of the Secret Art's target) needs line of sight to each foe, because `AbilityKit.cone` measures only angle and distance and a blow
  through a wall is a bug in PvP; a lane stops at the first wall by construction; a burst at a distant point (a bolt of the palace, the Secret Art's burst) is NOT blocked, because it is an explosion and a wall test per
  foe per bolt is not worth the rare case.
* **Roaring Thunder needs a foe in sight, and refuses before paying.** Alternative: rush anyway toward the look direction, rejected because a Secret Art with a 45-to-60-second cooldown must not be wasted by a misaim.
* **Thunder Palace draws its points at the cast and warns each bolt 6 ticks ahead.** Alternatives: bolts that pick a LIVE target when they fall (unavoidable), or orb entities (spawns and cleanup). With the points fixed
  and a column of dust 6 ticks before each, a mover can always leave; a standing foe is hit by each bolt that lands on it.
* **Not built, on purpose.** Iron Hammer would tear up the ground (the design forbids block changes) and overlaps with the Jaw; Raging Bolt is one bolt from the sky, which the Thunder Palace already is, and its
  place in the Dragon Slayer list is disputed even on the wiki; Lightning Dragon King Mode and Red Lightning Dragon Mode are transformations that need a buff-window model and balance the owner has not asked for. The
  route format takes a mode later (a JSON node and one skill class). They are listed here so nobody thinks they were forgotten.
* **"Flash", "Lightning Body" and "Eating Lightning" are names of my own**, because canon gives the behaviour and not a spell name. They can be renamed in the JSON and the lang file with no code change.
* **Lightning Body lets a blow through by chance, with an interval.** Alternative: a block after a sprint (needs state per player and a rule for "after"), or a flat damage reduction (a number the player never sees).
  A chance with an interval is simple, visible (a flash on the body) and bounded.
* **Eating Lightning also feeds under a thunderstorm in the open.** Without it the passive would almost never do anything (lightning rarely hits a player). The feeding needs `isThundering` and open sky, and it is
  the smallest numbers of the route.
* **Passives ask `canCast`, not `isNodeActive`.** The brief said `isNodeActive`; the two differ only in the class rule (the route's class must be the active class when the server asks for it). A player who owns
  two routes and plays one should get the passives of the one they play; with the class rule off they are identical.
* **Skill icons are generated, not vanilla item textures.** The owner's skill menu already has painted 32 px icons for every grimoire; vanilla item sprites next to them would look like a different game.

## 9. The exact route file

`src/main/resources/data/nusmp/skill_trees/lightning_dragon_slayer.json`

```json
{
  "magic": "fairy_tail",
  "name": "Lightning Dragon Slayer",
  "desc": "The thunder dragon's magic: strike at the speed of lightning, eat the storm, and finish the duel with a Secret Art that arrives before its own sound.",
  "book": "nusmp:lightning_dragon_slayer_book",
  "book_name": "Scale of Crackling Sovereignty",
  "book_lore": "Hold it and your hair stands on end. Read a page aloud and the sky answers.",
  "color": "#F5D63B",
  "icon": "minecraft:lightning_rod",
  "class_id": "lightning_dragon_slayer",
  "duplicate_sp": 3,
  "loot_weight": 2,
  "nodes": [
    {
      "id": "iron_fist",
      "name": "Lightning Dragon's Iron Fist",
      "desc": "Wrap a fist in lightning and punch the foes in front of you. The shock numbs the target and arcs on to others nearby.",
      "parents": [],
      "cost": 1,
      "max_level": 5,
      "skill": "nusmp:ft_lightning_dragons_iron_fist",
      "passives": [],
      "values": {
        "damage": [6, 7, 8, 9, 11],
        "numb_seconds": [1.0, 1.5, 1.5, 2.0, 2.5],
        "range": [3.5, 3.5, 4.0, 4.0, 4.5],
        "half_angle": [35, 35, 40, 40, 45],
        "targets": [1, 1, 1, 2, 2],
        "chain_targets": [0, 1, 1, 2, 2],
        "chain_range": [4.0, 4.0, 4.5, 4.5, 5.0],
        "chain_damage": [4, 4, 5, 5, 6],
        "knockback": [0.2, 0.2, 0.3, 0.3, 0.4],
        "cost_pct": [1, 1, 1, 1.5, 1.5],
        "cooldown": [100, 90, 80, 70, 60]
      },
      "x": 0,
      "y": 4,
      "icon": "minecraft:glowstone_dust"
    },
    {
      "id": "roar",
      "name": "Lightning Dragon's Roar",
      "desc": "Draw a deep breath, then roar a fan of lightning down the way you look. It numbs everything it touches.",
      "parents": [ { "node": "iron_fist", "level": 3 } ],
      "cost": 2,
      "costs": [2, 3, 3],
      "max_level": 3,
      "skill": "nusmp:ft_lightning_dragons_roar",
      "passives": [],
      "values": {
        "damage": [9, 11, 13],
        "range": [10, 12, 14],
        "half_angle": [12, 15, 18],
        "targets": [4, 6, 8],
        "numb_seconds": [1.5, 2.0, 2.5],
        "windup": [6, 5, 4],
        "cost_pct": [3, 3, 3.5],
        "cooldown": [160, 140, 120]
      },
      "x": 2,
      "y": 1,
      "icon": "minecraft:firework_star"
    },
    {
      "id": "breakdown_fist",
      "name": "Lightning Dragon's Breakdown Fist",
      "desc": "Open a magic seal before you and fire an enormous fist of lightning at the first foe in the line. It hits hard and stuns.",
      "parents": [ { "node": "iron_fist", "level": 2 } ],
      "cost": 2,
      "costs": [2, 3, 3],
      "max_level": 3,
      "skill": "nusmp:ft_lightning_dragons_breakdown_fist",
      "passives": [],
      "values": {
        "damage": [9, 11, 13],
        "range": [10, 12, 14],
        "width": [0.9, 1.0, 1.1],
        "stun_ticks": [20, 30, 40],
        "numb_seconds": [2.0, 2.5, 3.0],
        "windup": [6, 5, 4],
        "cost_pct": [2.5, 3, 3.5],
        "cooldown": [140, 120, 100]
      },
      "x": 2,
      "y": 4,
      "icon": "minecraft:spectral_arrow"
    },
    {
      "id": "lightning_body",
      "name": "Lightning Body",
      "desc": "Passive. Your body hums with current: you run faster while sprinting, and now and then a blow passes through you as if you were a bolt.",
      "parents": [ { "node": "iron_fist", "level": 1 } ],
      "cost": 1,
      "costs": [1, 2, 2],
      "max_level": 3,
      "skill": "",
      "passives": ["lightning_body"],
      "values": {
        "speed_amp": [0, 0, 1],
        "evade_pct": [8, 12, 16],
        "evade_icd": [60, 50, 40]
      },
      "x": 2,
      "y": 7,
      "icon": "minecraft:amethyst_shard"
    },
    {
      "id": "heavenward_halberd",
      "name": "Lightning Dragon's Heavenward Halberd",
      "desc": "Forge lightning between your palms into a great spear and hurl it down a long lane, piercing everything in it.",
      "parents": [ { "node": "roar", "level": 2 } ],
      "cost": 2,
      "costs": [2, 3, 3],
      "max_level": 3,
      "skill": "nusmp:ft_lightning_dragons_heavenward_halberd",
      "passives": [],
      "values": {
        "damage": [10, 12, 14],
        "range": [18, 22, 26],
        "width": [0.8, 0.9, 1.0],
        "targets": [3, 4, 5],
        "numb_seconds": [1.0, 1.5, 2.0],
        "windup": [8, 7, 6],
        "cost_pct": [3, 3.5, 4],
        "cooldown": [180, 160, 140]
      },
      "x": 4,
      "y": 0,
      "icon": "minecraft:trident"
    },
    {
      "id": "thunder_palace",
      "name": "Thunder Palace",
      "desc": "Hang lacrima orbs of lightning over the spot you aim at. After a breath they loose a barrage of bolts, one after another, onto the area below.",
      "parents": [ { "node": "roar", "level": 3 } ],
      "cost": 3,
      "max_level": 3,
      "skill": "nusmp:ft_thunder_palace",
      "passives": [],
      "values": {
        "damage": [8, 9, 10],
        "range": [10, 12, 14],
        "radius": [4.0, 5.0, 6.0],
        "strikes": [4, 5, 6],
        "strike_radius": [1.8, 2.0, 2.2],
        "delay": [20, 18, 16],
        "gap": [13, 12, 11],
        "numb_seconds": [1.0, 1.5, 2.0],
        "targets": [6, 8, 10],
        "cost_pct": [3.5, 4, 4],
        "cooldown": [240, 210, 180]
      },
      "x": 4,
      "y": 2,
      "icon": "minecraft:end_crystal"
    },
    {
      "id": "jaw",
      "name": "Lightning Dragon's Jaw",
      "desc": "Clasp both hands and bring them down in front of you in a crushing, lightning-laced slam that throws the foes around the point of impact outward.",
      "parents": [ { "node": "breakdown_fist", "level": 2 } ],
      "cost": 2,
      "costs": [2, 3, 3],
      "max_level": 3,
      "skill": "nusmp:ft_lightning_dragons_jaw",
      "passives": [],
      "values": {
        "damage": [8, 10, 12],
        "radius": [3.0, 3.5, 4.0],
        "reach": [2.0, 2.5, 3.0],
        "targets": [5, 7, 9],
        "knockback": [0.5, 0.7, 0.9],
        "numb_seconds": [1.0, 1.5, 2.0],
        "cost_pct": [2, 2.5, 3],
        "cooldown": [150, 130, 110]
      },
      "x": 4,
      "y": 4,
      "icon": "minecraft:bone"
    },
    {
      "id": "lightning_flash",
      "name": "Lightning Flash",
      "desc": "Turn to lightning and streak forward, shocking the foes you pass through. The flash stops at a wall.",
      "parents": [ { "node": "lightning_body", "level": 2 } ],
      "cost": 2,
      "costs": [2, 2, 3],
      "max_level": 3,
      "skill": "nusmp:ft_lightning_dragons_flash",
      "passives": [],
      "values": {
        "damage": [4, 5, 6],
        "dash": [6, 8, 10],
        "width": [1.0, 1.1, 1.2],
        "targets": [2, 3, 4],
        "numb_seconds": [1.0, 1.0, 1.5],
        "cost_pct": [2, 2, 2.5],
        "cooldown": [100, 90, 80]
      },
      "x": 4,
      "y": 6,
      "icon": "minecraft:ender_pearl"
    },
    {
      "id": "thunder_eater",
      "name": "Eating Lightning",
      "desc": "Passive. Lightning does not hurt a dragon slayer, it feeds him. Lightning attacks against you, and standing under a thunderstorm in the open, heal you and refill your magicules. Lightning you made yourself does not count.",
      "parents": [ { "node": "lightning_body", "level": 1 } ],
      "cost": 1,
      "costs": [1, 2, 2],
      "max_level": 3,
      "skill": "",
      "passives": ["thunder_eater"],
      "values": {
        "heal_pct": [2.0, 3.0, 4.0],
        "recharge_pct": [1.0, 1.5, 2.0],
        "eat_heal_ratio": [0.25, 0.4, 0.6],
        "eat_recharge_pct": [2, 3, 4]
      },
      "x": 4,
      "y": 8,
      "icon": "minecraft:lightning_rod"
    },
    {
      "id": "roaring_thunder",
      "name": "Roaring Thunder",
      "desc": "Secret Art. Rush at the foe in front of you, then release a vast arching burst of lightning from your fist that hurls them away, stunned, and shocks everything around.",
      "parents": [ { "node": "breakdown_fist", "level": 3 }, { "node": "lightning_flash", "level": 2 } ],
      "cost": 4,
      "costs": [4, 6, 8],
      "max_level": 3,
      "skill": "nusmp:ft_roaring_thunder",
      "passives": [],
      "values": {
        "damage": [18, 24, 30],
        "splash_damage": [10, 12, 14],
        "rush_range": [10, 12, 14],
        "burst_radius": [3.5, 4.0, 4.5],
        "knockback": [1.4, 1.8, 2.2],
        "stun_ticks": [20, 30, 40],
        "windup": [8, 7, 6],
        "targets": [6, 9, 12],
        "cost_pct": [6, 7, 8],
        "cooldown": [1200, 1080, 900]
      },
      "x": 6,
      "y": 6,
      "icon": "minecraft:nether_star"
    }
  ]
}
```

## 10. What proves it, and what could not be proved

Checked at build time (all pure, run with the JUnit shim here, run by the real JUnit in CI): `LightningDragonRouteTest` (the file obeys the schema and the price bands; the names; the Secret Art needs two
different branches; the file and the code table say the same numbers and obey the budget; every number the code reads exists and every number is read; the register registers exactly the skills the file names, each
class serves the node of its id; every skill has its 32 px icon), `DragonBudgetTest`, `DelayQueueTest`. Each was mutated once (a cap loosened, the monotone rule removed, a number changed in the file, a key misspelt
in a skill, a registered id and an icon name changed) and failed as it should. The route files were also run through the REAL schema of the core builder (`RouteSchema`, `RouteIndex`, `RouteWire`) and its
`SkillTreeFilesTest` and `IsolationTest`: no problem and no warning; 5.7 KB on the wire.

**Type-checked, not built.** There was no Gradle build and no game in this session. All sources under `skilltree/ft/**` were compiled with `javac` against API stubs of Minecraft, NeoForge, ManasCore and Tensura
together with the REAL sources of the core builder; the only errors left are four reports "MobEffectInstance is abstract", which are an artifact of the stubs (the real class is concrete). The JUnit tests ran on a
small shim of JUnit 5 (the plain `assertTrue`, `assertFalse`, `assertEquals`, `assertNotNull` family), not under Gradle.

In the game, on the CI server, `tree.ft.lightning_dragon_slayer`: the route is loaded and its numbers obey the budget; every node agrees with a registered tree skill; WITHOUT the route the stand-in player cannot
cast any of them, spends nothing, hurts nothing and starts no cooldown; WITH the route every technique is cast at level 1 and at the top level next to a dummy mob and nothing throws or is swallowed; at least one
hurts the dummy; the delayed ones queued their follow-ups; both passives answer (a bolt eaten, the player's own bolt not, plain damage not, nothing without the node; a blow let through with a forced roll of 0, not with
99.9, not twice within the interval, not when it has no attacker, not without the node).

**Not verified (nothing could be run in the game):** how the effects LOOK (the effect shapes were chosen from the descriptions of their layers: sizes, durations and the colour argument of `MAGIC_CIRCLE` and
`MAGIC_CIRCLE_EXPLOSION` are guesses to be judged by eye); the feel of the timings (inhale length, palace spacing, rush-to-burst gap); how Tensura's skill menu draws the icons; that a fake player can be taught a
skill, can stand at (0.5, 100, 0.5) and can hurt a mob in the CI world (the self-test is written for it, and reports the exact exception if not); `LivingEntity.knockback` for a player's client (it relies on
`hurtMarked`); the Tensura damage-type keys `LIGHTNING_ELEMENTAL`, `LIGHTNING` and `THUNDER_BREATH` and the effect `tensura:paralysis`; the thunderstorm feeding and the sprint speed (they need weather and a
sprinting player; the self-test drives the eating and the evade, not the two ticks); the `nusmp.skill.<id>` lang keys (taken from the 465 existing entries of the mod). The route depends on two additions of the core
builder that the contract stub does not have: `RouteRegistry.value(...)` and `SkillTreeApi.isEnabled()`.

## 11. Files

New: `skilltree/ft/lightning/**`, `data/nusmp/skill_trees/lightning_dragon_slayer.json`, `assets/nusmp/textures/skill/ft/ft_lightning_*.png`, this page; shared with the Fire route and described in `ft_fire.md`:
`skilltree/ft/dragon/**`, `tools/skilltree/gen_ft_dragon_icons.py`, `src/test/.../skilltree/ft/dragon/*Test`. Edited: `skilltree/ft/LightningDragonSkills.java` (the register body; its declaration of `SKILLS`
is unchanged). Lang: `nusmp.skill.ft_*` keys, delivered as a fragment.
