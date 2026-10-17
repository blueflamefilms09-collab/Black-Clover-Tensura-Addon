# Lucifero, King of Devils (`lucifero`)

Boss of the Spade Kingdom Raid (`ArcSaga.SPADE_RAID`), tier 4. Second and last boss of the pair Morris Libardirt + Lucifero (`docs/arc/morris_libardirt.md`);
the story unlocks him after the Dark Triad (`dante`, `vanica`, `zenon`) and Morris. He is the gravity fight on the scale of a ROOM: Dante's gravity is circles and
spheres on marks, Lucifero's is lanes, one great well round himself, a wind that flattens the air, and a rule about who is on their knees.

Everything in this file was written without being able to run the game. What was type-checked, tested and looked at, and what was not, is in section 11 and 13.

## 1. What is new, what is kept

| Part | New / replacement / kept |
| --- | --- |
| The boss class, its numbers, its pure math, its tests | NEW (nothing like it existed: the mod had Lucifero only as a devil STYLE, `Devil.LUCIFERO`, and Dante's borrowed gravity) |
| `Devil.LUCIFERO`, Dante's `GravityMath` / `DanteMoves` | KEPT and reused (the violet colour, `gravityField`, `stompWave`, `antiMagicHeld`, the Tensura gravity damage types) |
| The avatar command literal `lucifero` (`AvatarCommand`, the Devil Avatar boss) and the relic socket id `lucifero` | KEPT; untouched. The new entity id `lucifero` is an entity-type id (`nusmp:lucifero`), a different registry, so nothing collides |
| Three mob effects (`flattened_air`, `kneeling`, `specimen`) | NEW (shared with Morris: `SpadeEffects`) |
| Model `arc_lucifero` + `arc_lucifero_form2` | NEW (art kit, `tools/arc/char_lucifero.py`) |

## 2. Canon (who he is, how the story fight went) and where it came from

HONEST NOTE ON SOURCES: the fetch tool could not resolve any host in the session (`getaddrinfo ENOTFOUND`), so NO page below was opened. Every fact comes from the
summaries that a web-search tool returned for these pages, and the search results themselves flag several of the sources as low quality. Facts that only one
weak source gave are marked (weak). Nothing here is quoted; it is facts, in my words.

* He is the King of Devils, the strongest devil, and the devil whose power Dante Zogratis carried (Dante's Gravity Magic and the strengthening of his Body Magic
  come from him). When the second gate of the Underworld is forced open (Morris's life and magic are used up to speed the ritual) he merges the lesser devils
  that pour out into one monstrous body and manifests through it in the ruins of the Spade Kingdom castle. The body bursts out of the castle and raises the
  gravity of the whole area.
* He orders everybody to bow and pins them to the ground with Gravity Magic, then turns on Asta for interfering with the ritual. The Magic Knight captains,
  strengthened by Rill's support spell, cannot hurt him; one article calls the fight humiliatingly one-sided and says he handles the strongest captains with ease
  all at once.
* His gravity: he can raise, lower and compress the gravitational pull of anything; a gesture raises the gravity of an area enough to crush the strongest human
  mage; he warps space to avoid an attack without moving; he tears spells apart by raising the gravity round them (he did so to Dorothy's Dream Magic);
  he levitates; he crushed Mereoleona's forearm (physical strength). (weak) one article says he can turn his wings into extra arms and move his heart inside his body.
* The counter the story used is anti-magic: Asta (and Liebe) cut at him with the Demon-Slayer Sword even when he distorts space; he catches the blade in one hand
  and gets only a small cut on a finger; Asta is one of the few who can move inside his gravity field. Nacht's Shadow Magic restrains him so that a barely recovered
  Asta can strike. The fight is won, but it was only an avatar, a partial manifestation of him; his heart was afterwards carried off by Adramelech to Lucius.
* Look: a tall figure with long white hair, two pairs of horns on the sides of the head, two pairs of hooked bat-like wings, thin bony hands, grey skin with black
  patterns over the whole body. At full power spikes form on the shoulders and elbows, the horns thicken, the wing digits disappear and a third eye with no iris or pupil
  opens on the forehead. (One unreliable source says red skin; the wiki sources say grey.)
* Conflicts between sources, not resolved: which underworld level he rules ("the seventh and lowest" vs "one of three rulers").

URLs (search-result pages, not opened):
`https://blackclover.fandom.com/wiki/Lucifero`, `https://blackclover.fandom.com/wiki/Magic_Knights_vs._Lucifero`, `https://blackclover.fandom.com/wiki/Chapter_319`,
`https://villains.fandom.com/wiki/Lucifero`, `https://tvtropes.org/pmwiki/pmwiki.php/Characters/BlackCloverDevils`,
`https://sportskeeda.com/anime/black-clover-chapter-319-lucifero-dominates-magic-knights`, `https://sportskeeda.com/anime/is-lucifero-strongest-devil-black-clover-explained` (the wings-as-arms line: weak),
`https://ww4.soapcentral.com/anime/black-clover-how-powerful-is-lucifero`, `https://www.cbr.com/black-clover-strongest-devils-ranked/`, `https://www.cbr.com/black-clover-every-devil-explained/`,
`https://www.cbr.com/black-clover-the-magic-knights-have-defeated-lucifero-but-is-he-really-finished/`, `https://blackclover.wiki/characters-and-relationships/who-is-lucifero-in-black-clover`,
`https://comicbook.com/anime/news/black-clover-magic-knight-captains-lucifero-fight-manga-cliffhanger-spoilers/`,
`https://littleotakuspace.wordpress.com/2022/01/24/black-clover-chapter-319-320-review-great-magic-knights-vs-king-of-devils-root-cause/`.

## 3. The fight in one paragraph

The owner's reference is "the Captains against Lucifero: pure overwhelming gravity that flattens max-tier fighters instantly". A boss cannot flatten anyone
instantly in this mod: the Balance Law caps one hit at 40% of a player's health and a hard control at three seconds, and the boss framework's per-second
budget caps what the boss itself can lose. So the fight turns the same feeling into FORCED MOVEMENT: the room has rules and the party has to read them. He opens
by ordering the room to kneel (the Bow); he does not chase, he keeps five to ten blocks from you, and the floor does the work (lanes, rings, a well that follows you, a rain of
debris); the air is pressed flat so that nobody jumps out of it; his gravity shreds what flies at him until somebody walks up and cuts it with a blade, as Asta did;
at 60% the whole sky kneels you unless you find still air, at 80% the room is pulled into him (walk away), and when the Gates open (25% health) his four wings
become arms beside his two hands and six strikes ring him. Anti-magic is the story's counter and the fight honours it everywhere, but it never makes him harmless:
it cuts what it touches to 40% and tears his defences, it does not end the fight.

Power curve (framework, shared by every Zogratis-framework boss): 20% at the start, 40% under 85% health, 60% under 65%, 80% under 45%, the Gates (100%, a 60-tick
transformation, the "hybrid" body drawn 22% bigger) under 25%. A move's "power" below is the lowest power at which it is allowed.

## 4. The three rules only he has

### 4.1 The Bow and Kneel Before the King (the forced kneel)
* WHY: "kneel" is the one thing everybody remembers about this fight. As a mechanic it has to be a DEMAND the players can understand, meet and fail without being
  cheated, and it has to have a counter that is not "have more health".
* The Bow (power 20, wind-up 40, cooldown 900 = 45 s, recovery 40 so he is EXPOSED afterwards: x1.25 damage for 2 s). It is also his opening move (the fight starts with it, as in
  the story). He says "Bow." and the action bar tells everyone. During the wind-up the boss samples every foe each tick. When the weight falls:
  * crouching (holding sneak) at the fall, or up to `bowGraceTicks` (4) ticks before it, = BOWED: spared, no damage, no kneel ("You bowed. He lets you stand.");
  * holding anti-magic (a demon sword in hand, the Anti-Magic Lord, the Anti-Magic grimoire out) = UNBOWED: spared ("Your anti-magic holds the weight off you.");
  * everyone else is CRUSHED: 20 base damage through Tensura's own gravity-press damage type (a raised shield takes 35% of it, anti-magic 40%), driven to the floor, and
    put on their knees (the Kneeling effect: no walking, no jumping, no drift) for `kneelSeconds` (2.5 s default, at most the Balance Law's 3 s per player, and the law's own
    shared 12 s lock decides whether a hold is allowed at all; if it is not, the crushed are slowed hard for 12 ticks instead).
  * The counter is a choice the player makes with the sneak key. It costs nothing but a moment of not attacking, and it is a reason to listen to the chat.
* Kneel Before the King (ultimate, power 60, wind-up 70, cooldown 1500 = 75 s, recovery 60 so he is exposed 3 s afterwards). It asks for MORE than a bow:
  crouching does not help. Round each foe's feet the boss lays POCKETS OF STILL AIR (`PocketMath`: white rings of 2.4 blocks radius with a pillar of light, one per foe, at
  least two, at most five, each 5 to 9 blocks from the foe it is laid for when the order is given, at least 6.4 blocks from the boss's middle and 6.3 from each other, always on floor and inside the arena; in a room so cramped that fewer than two such spots have floor, the foes are sheltered where they stand, so the counter always exists). When
  the sky falls (34 base, gravity-explode type):
  * inside a pocket: a quarter of the damage, not forced down;
  * holding anti-magic: 40% of the damage, not forced down;
  * everyone else: all of it, crushed to the knees (kneel as above).
  The walk to a pocket is always possible on foot (a test pins that the farthest pocket, 9 blocks, is within the walkable distance of the shortest wind-up at 60/80/100%).
* Numbers in code: `BowRules` (the verdict, the factors, the lag allowance), `WeightMath.kneelTicks/LAW_CAP/POCKET_RADIUS/pocketCount`, `PocketMath`.

### 4.2 The Gravity Shroud (his defence, with the story's counter)
* WHY: his gravity "tears apart spells". An arrow-and-spell party that stands back and shoots would trivialise a boss who never needs to move, and the story's answer
  was a blade (Asta, Liebe) and Nacht holding him. So: while the shroud stands, arrows, tridents and spells (ranged and magic) do HALF. Melee blows and anti-magic are not
  halved and WEAR it; when they have dealt 6% of his maximum health in total (`raiseShroud`: `max health * 0.06`) the shroud TEARS and he is staggered for 40 ticks (+20% damage).
  The shroud stands 160 ticks (8 s) at most; he raises it with the move Gravity Shroud (power 20, wind-up 16, cooldown 480).
* The Gravity Grasp's anti-magic cut (move 8 in section 5) also drops the shroud and staggers him, because the hand was cut.
* The counter is to walk up and cut. Because all the rest of his fight is "stay out of the lanes", the party has to do both, which is the point.
* Numbers: `ShroudState` (TORN_APART 0.5, STAGGER 40 ticks, bonus 1.2).

### 4.3 Flattened Air
* WHY: "flattens max-tier fighters instantly". The cheap way to make gravity feel heavy is to take away the vertical option. His lanes, rings and wells are meant to be
  walked out of; a party that bunny-hops or glides out of them would make the whole fight a dodge-by-jumping. So Flatten the Air (power 40, wind-up 24, cooldown 420) puts the
  effect Flattened Air on every foe in the arena for 140 ticks at 40%, 150 at 60%, 160 at 80%, 170 at the Gates: no jumping (jump strength -100%), double gravity,
  a glider is brought down. Walking is untouched. Holders of anti-magic are not held. `flattenedAir` in the config turns the jump lock off (the air then only drags fliers down).

## 5. The moves (13). Numbers are BASE damage before the framework's damage law

The framework law turns a base number into the real hit: x the power's multiplier (0.60 at 20% power up to 1.60 at the Gates) x `damageScale` (1.2 x the config factor) x a factor
for the victim's existence points, then the Balance Law (a sliver of the victim's maximum health, at most 40% of a player's health per hit). Wind-ups shrink with power
(x0.95 at 60%, x0.85 at 80%, x0.75 at the Gates) and never below 8 ticks (30 for an ultimate). Every move telegraphs: the tell runs every third tick of the wind-up.

| # | Id / label | Power | Wind-up | Cooldown | Range | Weight | Base dmg | What it does | What the party sees | Counter |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `crushing_palm` Crushing Palm | 20 | 16 | 70 | 0-6.5 | 6 | 22 | A heavy open hand on everything in a 110-degree wedge, reach 5.6: crushed, knocked down, Slowness III for 1 s. | A ring growing to 5.6 blocks, two lines at +-55 degrees, light gathering at his hand. | Leave the wedge (back or side); a shield takes 65% off, anti-magic 60%. |
| 2 | `falling_heavens` Falling Heavens | 20 | 24 | 130 | 6-34 | 5 | 11 per mark | A rain of weight on 6 marks (8 at 60%, 10 at 80%, 12 at the Gates), radius 2.4 each, landing a few ticks apart; one foe takes at most one piece. First mark on the target, next on the other foes, the rest scattered; marks never overlap. | A ring and a column of light on every mark for the whole wind-up. | Step off your mark. |
| 3 | `distortion_step` Distortion Step | 20 | 14 | 180 | 0-14 | 3 | 7 | He steps 8-13 blocks away from the target (never off the arena) and the place he stood becomes a lens: for 14 ticks it pulls gently inwards, then it collapses (radius 3.6, gravity-explode type). | A ring closing on him and a ring plus a column where he will appear. | Do not follow him into the lens; let it collapse, then go. |
| 4 | `the_bow` The Bow | 20 | 40 | 900 | 0-48 | 3 | 20 | See 4.1. The opening move. | A huge ring spreading from him, a ring at every foe's feet that turns light when that foe crouches, a sound, the chat and action bar order. | Crouch (hold sneak) or hold anti-magic. |
| 5 | `gravity_shroud` Gravity Shroud | 20 | 16 | 480 | 0-48 | 2 | none | See 4.2. | Light gathering round him; a violet aura while the shroud stands; the chat says "arrows and spells do half". | Melee or anti-magic until it tears. |
| 6 | `lesser_devils` Lesser Devils | 20 | 30 | 700 | 0-48 | 2 | none | Calls 2 lesser devils (3 at the Gates). From 60% power, when two or more already stand, he SWALLOWS up to four of them instead and wears a SHELL: a pool of damage of `min(8%, 2% x (0.75 + 0.25 x stage) x count)` of his maximum health that runs out after 200 ticks (10 s). The shell is a pool that is used up, never a heal. | A well at his feet; lines of light from each devil into him; the chat says how big the shell is. | Kill the devils before he swallows them, burst the shell, or wait it out; it falls off by itself. |
| 7 | `gravity_comb` Gravity Comb | 40 | 30 | 260 | 4-30 | 4 (crowd) | 17, pulse 8 | THREE lanes (FIVE from 80%) of crushing weight, each 3.2 wide and 24 long, 7.2 apart (a 4.0-block gap to stand in), laid along the line to the target when the wind-up begins. At the fall everyone in a lane is crushed and slowed; the lanes then stay 3 s as a field that slows, sinks and holds the air down, with one more pulse in the middle of it. | The lanes drawn on the floor for the whole wind-up (both edges re-drawn every 6 ticks), and the chat counts them. | Stand in the gaps. The comb always has a gap (a test pins that). |
| 8 | `gravity_grasp` Gravity Grasp | 40 | 20 | 240 | 3-18 | 3 | 26 | A hand of gravity along a line locked at the start of the wind-up seizes the nearest foe on it (16 blocks, width 1.4 each side). The seized foe is lifted and, if the Balance Law allows a hold, held half a second, then slammed (gravity-explode type). Without the law's permission: slammed at once for 60%. ANTI-MAGIC CUTS THE HAND: no damage, and his shroud falls and he staggers. | A red line from his hand to where you stand and a ring on you. | Step out of the locked line; or hold anti-magic and parry. |
| 9 | `wandering_weight` Wandering Weight | 40 | 26 | 300 | 4-32 | 3 | 3.2 pulse | A field of heavy air (radius 5.5) that sets out from its mark after its target at 2 blocks a second for 120 ticks (160 at the Gates): slows (the same law as Dante's fields: Slowness II to VI at the middle, by his power, and a third of that at the rim), presses down, and crushes once a second (strength 1.7 at 40% to 3.0 at the Gates). | A ring and a column on the mark in the wind-up, a closing ring, and then a ring that follows its target. | Leave the mark in the wind-up; if it is on you, leave sideways across the rim (the weight there is a third of the middle) and keep walking: its centre moves at 2 blocks a second, you at 4.3. |
| 10 | `flatten_air` Flatten the Air | 40 | 24 | 420 | 0-48 | 2 | none | See 4.3. | Light gathering, two expanding rings, a hint on the action bar. | Walk, never jump. Anti-magic holders are not held. |
| 11 | `kneel_before_the_king` Kneel Before the King (ULTIMATE) | 60 | 70 | 1500 | 0-56 | 5 | 34 | See 4.1. Exposed afterwards (x1.25 for 3 s). | A rising ring, pillars of white light on the pockets, his order in chat and on the action bar, a low sonic charge. | Stand in a white ring, or hold anti-magic. Crouching alone does not save you. |
| 12 | `event_horizon` Event Horizon (ULTIMATE) | 80 | 72 | 1500 | 0-56 | 5 | 36 | A great well forms on him. In the last 24 ticks of the wind-up everything within 30 blocks is DRAGGED towards the middle (0.03 blocks a tick at the rim up to 0.09 near the core, never more than a walker's 0.216, fliers are brought down, anti-magic halves it). When it collapses everyone within 9 blocks of the middle is crushed (gravity-explode type, shield 65% off, anti-magic 60% off), thrown out and slowed. Exposed afterwards. | A singularity above him, a warning ring of 9 blocks, a ring closing from 26 blocks to the core, the chat and action bar ("walk away"). | Walk away from him as soon as it starts. A test pins that a walker who starts at his feet is out of the core in time at 80% and 100%. |
| 13 | `six_limbs` Six Limbs | 100 (Gates) | 34 | 260 | 0-9 | 5 | 8 each | His four wings become arms beside his two hands: six strikes, six ticks apart, spread evenly round him from a random starting angle, each a 48-degree wedge with reach 5.8. One foe takes at most three of the six. | A ring of 5.8 blocks and six spokes drawn on the floor. | Back off beyond 5.8 blocks, or stand in a gap between spokes (six 12-degree gaps). |

Passive / event extras: when the power steps (40/60/80) he calls lesser devils (`summonDisciples(stage)`), and at the Gates three waves of lesser devils at +80, +260 and +460 ticks;
the chat speaks a line at each step. The opening Bow is forced through `chooseMove` as soon as it is eligible.

## 6. Numbers

| What | Value |
| --- | --- |
| Health / attack / speed / armor / toughness | 3500 / 20 / 0.34 / 20 / 12 (tier 4 band 3000-3800; under Lucius's 3600) |
| Plain melee | 70% of the attack attribute (the Palm is his real heavy hit) |
| `damageScale` | 1.2 x `luciferoDamage` (default 1.0) |
| Hybrid body (Gates) | drawn 1.22 x |
| Hitbox | 0.8 wide, 2.69 tall (the model's height from the manifest) |
| Preferred range | 5 to 10 blocks (he does not chase; the room is his) |
| The damage budget | He is a `ZogratisBoss`: the framework's per-second budget applies (by default 6% of max health per second, 9% in the final phase) |
| Floors | 90 to 100 on either road; his themes are `infernal_foundry`, `abyss`, `gravity`, `flame`, `shadow`, `necropolis`, so the Abyssal road's last floor is the natural one (the actual allocation is ARC-WORLD's) |
| Prerequisites | `dante`, `vanica`, `zenon`, `morris_libardirt` |
| Charter Grimoire (35% chance) | `gravity_domination`, `gravity_field`, `gravity_flight`, `gravity_attack_nullification`, `majesty`, `demon_lord_haki`, `magic_jamming`, `spatial_motion`, `mortal_fear` (each id checked against the Tensura id reference by `SpadeRosterTest`) |
| Colours | violet 0x5B2A8C, accent 0xB58CFF (the same family as the Zogratis devil style) |

## 7. How he keeps the shared rules

* No Unique/Ultimate Tensura skill abuse: none of his moves calls a Tensura skill; the gravity is the mod's own code plus Tensura's gravity DAMAGE TYPES (so a victim's gravity resistance counts; plain magic damage if the types are missing).
* No free healing. The shell of swallowed devils is a pool of damage that runs out; the framework's own slow regeneration (by power) is the only recovery, as for every boss.
* Crowd control only through `BalanceLaw.beginControl` (the shared 12 s lock) and `BalanceLaw.controlTicks` (the three-second cap on players): the Bow, the Kneel, the Grasp. Without the lock they are slows and slams, never holds.
* Damage only through `bossDamage` / `strike` (`SpadeFx.crush` wraps `bossDamage` + the Tensura damage type). No block is ever changed (he has no griefing at all).
* Effects: `Tells`, `ZogratisVfx` (gravity impact, well, lane, pillar, singularity, aura) and `VfxSpawn`; the only particle type used is the coloured dust of `Tells` itself (the framework's own telegraph primitive). Sounds are vanilla `SoundEvents`.
* Everything he does in a timer is wrapped (`SpadeFx.safe`) so a failure logs once and stops that zone; it never takes the tick down.

## 8. Config, effects, language, icons

* Config file `nusmp-spade-server.toml`, section `[spade]` (`SpadeConfig`, shared with Morris): `flattenedAir` (true), `kneelSeconds` (2.5, range 0.5-3.0), `bowGraceTicks` (4, range 0-20), `luciferoDamage` (1.0, range 0.25-3.0); `structuralTuning`, `tuningCut`, `morrisDamage` are Morris's.
* Mob effects (`SpadeEffects`, registered by the lead): `nusmp:flattened_air` (jump strength -100%, gravity +100%), `nusmp:kneeling` (speed -100%, jump -100%, and the tick takes the sideways drift away), `nusmp:specimen` (Morris's). Icons: `textures/mob_effect/{flattened_air,kneeling,specimen}.png`, made by `tools/gen_spade_effect_icons.py`.
* Language (22 keys, delivered to the lead as a fragment and merged into `en_us.json`): `entity.nusmp.morris_libardirt`, `entity.nusmp.lucifero`, `effect.nusmp.{flattened_air,kneeling,specimen}` and their `.description`, and the 14 action-bar lines `message.nusmp.spade.{bow_order,bowed,unbowed,crushed,kneel_order,kneel_ultimate,pocket,parried,flatten,horizon_order,specimen,tendril_cut,cage_order,cage_wall}`. Every hint also carries its English text as the fallback in the code, so a missing key still reads right.

* Trying it (operators, nothing here has been tried): `/multiverse arc summon lucifero` at your feet, `/multiverse arc info lucifero` for the record and its problems. He opens at power 20 and steps up as his health falls (40 under 85%, 60 under 65%, 80 under 45%, the Gates under 25%), so the later moves (the comb, the kneel, the well, the six limbs) show only in a long fight; `luciferoDamage` 0.25 makes it survivable while testing, and `kneelSeconds` / `flattenedAir` are the levers for the two harshest rules. The story gate does not apply to operators.

## 9. Model

* `tools/arc/char_lucifero.py` (art kit): lanky 2.7-block grey-skinned king, long white hair, a third eye, two big horns plus a second lower pair and two big bat wings plus a second smaller pair (added in `extra`), pauldrons and bracers of bone, a dark sphere on his right palm (the compressed well), hovering idle, violet glow on eyes, trim and the orb. Form 2 (the Gates): bigger wings and horns, spikes on shoulders and elbows, a white-eyed glow, a cracked face.
* `ArcLook` numbers (from `tools/arc/manifest.json`, bigger form for the culling box and the shadow): `new ArcLook("lucifero", 2.69f, 1.73f, 3.1f, 1.4f, 2.5f, 1.72f, true)`. They must be copied again whenever the spec changes.
* Previews (saved by the builder in its scratch folder `previews/s2/`; rebuild them with `RENDER_PREVIEW_BUILD=<dir> python3 -B tools/gen_arc_characters.py --preview lucifero front back face idle attack cast_big transform death`, and for the second form `python3 -B tools/render_preview/preview.py arc_lucifero_form2_front arc_lucifero_form2_back arc_lucifero_form2_face`): `arc_lucifero_{front,back,face,idle,attack,cast_big,transform,death}.png`, `arc_lucifero_form2_{front,back,face}.png`, and the icons sheet `spade_effect_icons.png`.
* Added for the game, not from the source: the violet glow, the dark orb on the palm, the hover. Unverified: the second pair of horns and wings is drawn as a modest lower pair, not from a reference picture.

## 10. What the owner's rules ask of a weapon (NOT built; rule 7 of the brief)

If Lucifero ever gives a weapon it should be a heavy scepter-or-gauntlet of gravity, "the Weight of the King" (a staff-class 3D model in hand in the Genesis Demon-Slayer's quality,
violet glow and a collapsing orb at the head). Engravings from the mod's own list (`WeaponEngravings`, ids that the mod already uses): `crushing` 3, `magic_interference` 2 (he tears
spells apart), `sturdy` 2, `barrier_piercing` 1 (gravity does not care for a ward). Nothing was built.

## 11. Tests, and what was looked at

* Pure classes with JUnit tests (the whole spade package: 85 tests, all pass in the local shim; CI runs the real JUnit): `WeightMath` (comb gaps, the well never beats walking, a walker escapes the core in time at 80% and 100%, a pocket is reachable on foot, a kneel never outlasts the law), `BowRules` (verdicts and lag), `ShroudState` (what is halved, what wears it, the stagger), `PocketMath` (pockets never overlap, never on the boss, seeded), `RainMath` (marks never overlap, first on the target, capped at 12), `LuciferoTable` (the table is well formed, the order is pinned, a whole fight is played through the framework's scheduler at each power and range: ultimates spaced and alternating, nothing available that should wait for the Gates, no dead range), `SpadeRoster` (record, chain, floors, colours, lawful Charter skills), `DamageKind`.
* Mutation checks: a harness applies ONE change to a pure class, runs that class's tests, puts the file back and compares it byte for byte. 26 single-line mutations were run over the whole package (15 of them on Lucifero's classes, 11 on Morris's). Lucifero's: the Event Horizon's pull maximum 0.09 to 0.25 (caught by 2 tests), the lane spacing to leave no gap (2), the core radius 9 to 14 (2), the kneel's law cap removed (1), the shroud halving melee too (1), the shroud's stagger 40 to 4 ticks (1), the bow's lag check off by one (1), the lag default 4 to 40 (SURVIVED the first time, see below), the pocket factor 0.25 to 1.0 (1), the anti-magic kneel factor 0.40 to 1.0 (1), the Event Horizon's wind-up cut to 20 (1), the flatten time base 140 to 14 (1), five lanes at 80% reduced to three (1), a limb's reach 5.8 to 15 (1), the rain's mark formula (1). Three mutations over the package survived at first (this lag default, the cut time of Morris's thread, and the number of doors of Morris's cage) because their tests read the very constant that was mutated; a test that pins each documented number was added and kills each of them now.
* Type-checked with the stub compiler against the real API dumps: all spade classes (the 17 remaining errors are the known stub artifacts: `new MobEffectInstance` is abstract in the stubs, and `INBTSerializable.deserializeNBT` on entity subclasses; no real error).
* Looked at: the model previews listed above, rendered by the repo's preview renderer (not in the game). NOT looked at / not run: any of it in the game. No move was seen with its telegraph, no number was felt, the hitbox and the culling box were not tried, the effect icons were seen only as enlarged PNGs.

## 12. For the lead

The integration lines are at the end of the s2 report: `ArcRoster.register(MorrisBoss.CHARACTER); ArcRoster.register(LuciferoBoss.CHARACTER);` in `ArcCharacters.bootstrap()` (Morris first), `SpadeEffects.init(); SpadeEffects.EFFECTS.register(modEventBus);` and
`modContainer.registerConfig(ModConfig.Type.SERVER, SpadeConfig.SPEC, "nusmp-spade-server.toml");` in `NUSMP`. No renderer is registered by this builder (`ArcClient` does it for every boss of `ArcEntities`).

## 13. Unverified, uncertain, and design choices that are not canon

* All the canon above is from search summaries (no page was opened). Several are fan wikis or news sites; the search tool itself flagged some of the pages as low quality.
* "Six Limbs" (four wings that become arms, plus two hands) rests on ONE weak source; everything else about the Gates (the hybrid body, the wave of lesser devils) follows the mod's Zogratis framework, not the story.
* "Crouch to bow", the pockets of still air, the Shroud's numbers, the lanes, the Wandering Weight and the Event Horizon are GAME rules chosen to express "he forces everyone to bow / tears spells / pins fighters with gravity"; the story has none of them as such. The anti-magic exemptions follow the story's counter (the Demon-Slayer Sword).
* Whether Lucifero should be the floor-100 boss of the Abyssal road is ARC-WORLD's allocation: `standsOn` says he may stand on floors 90-100 of either road.
* ARENA SIZE: the Event Horizon's core is 9 blocks and its pull reaches 30; the Gravity Comb's lanes are 24 long; Distortion Step needs 8-13 free blocks. They assume the framework's own arena (radius 56) or at least 24 blocks of open floor round him. In a room smaller than about 20 blocks across the Event Horizon cannot be walked out of (a raised shield and anti-magic still cut it, and the Balance Law still caps one hit at 40%). ARC-WORLD should give him a roomy arena.
* The hitbox is 0.8 blocks wide (the art kit suggests 0.55 for a body this thin): a design choice so that melee can reach him; the wings are only the picture.
* Not run in the game: damage, cooldowns, the feel of the lanes, whether the Flattened Air + Wandering Weight combination is too harsh (the `flattenedAir` key is the lever), whether five lanes at 80% leave enough room on a small arena (the comb is laid on the target, not the middle, so it moves with the party).
* The dust particle used by the telegraphs is the vanilla dust type (what `Tells` itself uses); no other vanilla particle type is used.
* The art kit as it stands needed three workarounds, none of them a change to the kit: `pattern_color` must be a tuple (a hex string crashes the pattern code), an attack style other than `punch` needs a held prop (so both bosses hold an orb and use `cast_jab`), and `--preview <id>_form2` is read as "all characters" (form 2 was previewed through `tools/render_preview/preview.py arc_<id>_form2_<scene>`).
