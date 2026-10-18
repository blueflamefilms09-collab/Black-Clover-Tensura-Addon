# Vassal Carriage (`vassal_carriage`): the war chariot that carries and protects

Class VASSAL, style RIDE. In the series the Vassal Carriage is one of the eight Vassal Weapons of Raphtalia's world and its historic holder is Fitoria, the Filolial Queen
(the owner names her). Logic `heroweapon/weapon/carriage/CarriageLogic`, the vehicle `CarriageEntity` (same package), its renderer `client/heroweapon/carriage/**`, forms
`data/nusmp/hero/forms/vassal_carriage.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart3`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text, the wiki's forms (a "Small Carriage" whose base shape looks like a Roman chariot, a "Dragon Hourglass Sand Carriage", a "War
Carriage") and the series' role for the weapon (fast transport and protection of the people aboard). Colours and proportions are DESIGN.

**THIS MESH IS ALSO THE ENTITY.** The carriage you can ride is `weapon_meshes/vassal_carriage.txt` drawn about 3.2 times larger by `client/heroweapon/carriage/CarriageRenderer`
(it reads the same file, the same skin and the same glow skin; it does not use `place`). So build it as a small, clean, closed model: wheels as separate named parts, no thin or
floating detail that would break when scaled by three (nothing thinner than 1.2 % of the carriage's length; no free-hanging chains or loose cords).

* **Mesh space (binding, the renderer maps it like this).**
  * **+y is the nose** (the direction the carriage drives; the pole and the lantern are at the high-y end, the tail at the low-y end). This is also the item rule "the long
    axis is y".
  * **-x is up**: the canopy and the roof are at the low-x side, the wheels' ground contact at the high-x side. In the item frame this puts the carriage's roof on the sprite's
    up-left side, like the edge of a vanilla axe, so in the hand it drives up-right with its roof above it.
  * **z is the width**: the two wheels are at +z and -z, the axle runs along z, the carriage is symmetric about the z = 0 plane.
  * The renderer centres the mesh on its own bounding box in z and y, puts the lowest point of the wheels (the highest x) on the ground, and scales by `3.2 / (ymax - ymin)`.
* **Silhouette.** A small two-wheeled chariot-carriage in the Roman chariot manner (wiki), made ornate: a low rounded body (a chariot's curved front dash), a rounded open
  cabin with a domed canopy on four slim posts, a driver's bench in front under the canopy's front edge, a rear bench for two, one long draught pole with a crossbar yoke ahead of
  the nose (where the filolial would be harnessed), a hanging lantern at the front of the canopy, two upright plumes (filolial feathers) at the canopy's front corners.
  Two big spoked wheels, one each side. Proportions (fractions of the body length L, which the renderer scales to 3.2 blocks): width over the wheels 0.60 L, height to the top
  of the canopy 0.58 L, wheel diameter 0.34 L, body (floor) length 0.70 L, pole and yoke 0.30 L ahead of the body, seat surface 0.17 L above the ground.
* **Named parts (10 to 14).** `wheel_left`, `wheel_right` (each: a rim, 8 flat spokes, a hub; the rim at least 0.025 L thick, the spokes at least 0.012 L), `axle`, `chassis`
  (floor and low side panels, with the curved dash), `cabin` (the low walls), `canopy` (the dome), `posts` (four), `bench_front`, `bench_rear`, `pole` (draught pole), `yoke`
  (the crossbar), `lantern` (the frame) and `gem` (the lantern's jewel), `plumes` (two).
* **Seats (the entity seats three).** Driver: centre of the width, 0.17 L ahead of the middle of the body, seat surface 0.17 L above the ground. Two passengers: 0.19 L behind the
  middle, 0.14 L to either side of the centre line, same height. The benches must be where these points are so that the riders sit on them.
* **Palette.** Body ivory `#F2EBD6` with gold trim `#D8A93A` on every edge and on the canopy's rim; upholstery (benches, canopy lining) deep blue `#2F4B86`; wheels: dark walnut
  `#6B4429` spokes, gold rims; pole and yoke gold; plumes white with a sky-blue tip `#7FB6E8`; the lantern's jewel amber `#FFC247`.
* **The gem.** The lantern jewel (a faceted amber cabochon, about 0.035 L across) hanging at the front of the canopy; on the glow skin it is the strongest spot.
* **Glow skin** (additive): the gem (strong), thin gold engraving lines on the cabin's sides (soft), the wheel hubs (faint). Nothing else. The glow colour in the mesh's `glow`
  line is amber (`FFC247`).
* **In hand.** About 1.0 block long (the item class is `handitem`, measured by the largest dimension: 0.5 to 0.9 in the hand, about 1.0 for this long model is the art's choice). The grip point is the middle of the chassis underside (the hand
  holds it from below like a trophy model); the nose points up-forward on the icon's diagonal, the roof on the up-left side. Sprite (16 x 16, kept as the inventory icon): a tiny
  side view, two gold-rimmed wheels, an ivory body, a blue canopy and one amber dot.
* **Mood.** Bright, noble, a little festive; it is the cheerful member of the family.

## 2. Canon and sources

Each fact is tagged [summary] (read in a web search summary of the fandom wiki; the pages themselves were not opened), [memory] or [design].

* [summary] The Vassal Carriage is one of the eight Vassal Weapons of Raphtalia's world (the wiki lists it with the axe, hammer, gauntlet, claw, staff, projectile and whip); the owner writes its holder as "Fitoaria", the wiki writes Fitoria, the Filolial Queen and "Carriage Hero". https://shield-hero.fandom.com/wiki/Vassal_Carriage and https://shield-hero.fandom.com/wiki/Fitoria
* [summary] Its role: fast transport and protection of the people aboard on the battlefield; the whip hero Holn Anthreya created Fitoria because the carriage wanted a monster to pull it. https://shield-hero.fandom.com/wiki/Holn_Anthreya
* [summary] Its skills: Portal (teleports to saved locations; listed as an equip bonus of the Dragon Hourglass Sand Carriage) and Crash! Charge! (the carriage transforms into a massive chariot pulled by Fitoria, the pair charge into the target and stun it, stomping it on the way). Its forms: Small Carriage (the base shape looks like a Roman chariot), Dragon Hourglass Sand Carriage (from the sand of the Dragon Hourglass) and a War Carriage (the wiki marks it with a question mark). The wiki says the weapon specialises in mobility, like the Vassal Boat.
* [summary] Haikuikku is one of Fitoria's spells: it greatly increases the user's speed and infuses magic into melee attacks. https://shield-hero.fandom.com/wiki/Fitoria/Powers_and_Abilities
* [design] The vehicle itself is this mod's answer to "using the weapon calls a carriage": a rideable entity driven by the wielder, with two more seats for friends. Everything else is design: the gallop, the drift ram, the soothing ride, the cabin barrier, the waypoint (the wiki's "saved locations" as one saved place), the forms Filolial Carriage and the Queen's Chariot, and every number.
* [design] Colours and shape: the art brief (section 1): ivory body, gold trim, blue upholstery, an amber lantern gem.

## 3. How it plays

Using the weapon (Call the Carriage) puts a **carriage** in front of the wielder and seats them as the driver. The carriage is a real vehicle: it is steered by where the driver LOOKS (W goes forward, S backs up slowly, A and D do nothing), it is fast on the ground, it climbs a block high step without jumping, it floats on water, and it takes the wielder and one friend (two friends with the Roomy Cabin, from the first form up). It does no damage by being driven (so it cannot be used as a griefing or roadkill machine); the techniques turn it into a weapon: the gallop and the charge ram foes in front, the drift sweeps a ring round it, the barrier shields it, the soothing ride heals the people on it, and the portal carries it to a saved place. The weapon is a support-and-mobility weapon first: its damage is modest, its value is what it carries.

### The rules of the vehicle (all of them are tested or checked by the self-test as far as plain arithmetic goes)

* **One per wielder.** A registry (`CarriageRegistry`, memory only, emptied when the server stops) maps the wielder to the carriage; Call sends an older one away first.
* **Only the owner drives it.** `getControllingPassenger` is the owner alone; anybody else who sits is a passenger and cannot steer. Anybody else may get on only when (a) the owner is already aboard, (b) there is a seat and (c) they are an ally of the owner (a player of the team, a pet, a summon). A stranger right-clicking it gets a message and nothing else; `canAddPassenger` enforces the same on the server, so nothing else can seat them either.
* **It goes away** when the owner has been off it for 10 s, when the weapon is put away or the bond ends (checked every 0.5 s and by the weapon's dismiss hook), when the owner dies or leaves the game, when it is destroyed, and when the server stops. Anybody aboard is put on the ground beside it first (the left side, else the right, else behind, else ahead).
* **It can be destroyed**: 40 health (90 with Armoured Frame). It cannot be hurt by falling, drowning, being in a wall, cramming, cacti or berry bushes, nor by anybody aboard, nor while the cabin barrier is up. It is fire-proof, immune to potion effects, cannot be pushed, takes no knockback, uses no portals, drops nothing and gives no experience. It is NOT saved with the world (a restart has none) and cannot be spawned by a command.
* **It belongs to its wielder like a pet.** The entity implements `OwnableEntity`, so the weapons' foe filters (`Foes.belongsToSide`) leave it and its owner's team alone: a team mate's ram does not hit it, an area attack of this builder's weapons does not hurt it, and the Soothing Ride or the Staff's Healing Light may mend it.
* **It never changes a block.** Its step height is 1 block, it floats up in water.
* **Its blows are the owner's.** While a boost or a charge runs AND the owner is at the reins, each foe the carriage runs into is hit once through the shared hit gate for the owner (so the balance law's caps and the owner's mastery apply). Allies, pets, the wielder's summons, creative and spectator players are never hit.

### Numbers of the vehicle (in `CarriageNumbers`, tested)

* Speed attribute 0.22 for a plain carriage (a horse has 0.1125 to 0.3375; about 9 blocks a second on level ground), x1.15 with Swift Wheels, x1.3 with the Queen's Pull (the greater of the two counts, they do not add). A boost multiplies it by 1.8, a charge by 2.3, Haikuikku by 1.25 on top. Reverse is 0.3 of forward.
* Steering: the carriage turns towards the driver's yaw by at most 9 degrees a tick standing still, falling to 3 degrees a tick at half a block a tick or more, so a gallop is hard to turn (the counter to every ram).
* Seats: 2 (3 with Roomy Cabin); the driver on the front bench, the passengers on the rear bench; the seat points are in `CarriageNumbers.SEATS` (x to the left, y up, z forward, in blocks).
* Size: a 2.2 by 1.7 block hitbox; the model is drawn 3.2 blocks long.

### How it is built

* `CarriageEntity` extends `LivingEntity` and uses vanilla's ridden-vehicle movement (the hooks `getRiddenInput`, `tickRidden`, `getRiddenSpeed`, exactly as a horse does): the driver's client does the moving and sends it, the server validates it with the vanilla vehicle-move check. No packet of ours is involved. Its owner and its flags (boost, barrier, charge, haste) are synched data; everything else is server-side.
* `CarriageEntities` registers the entity type `nusmp:vassal_carriage_wagon` (a `DeferredRegister` attached to the mod bus the shared start-up stored for the logics) and its attributes. `CarriageLogic`'s constructor does that once, starts the client renderer on the client (`CarriageClient.init`, idempotent: it may also be listed in `HeroClient.init`), registers the self-test and installs the clean-up listeners.
* Client: `CarriageRenderer` draws the baked model of `CarriageMesh`, which finds the SAME mesh file as the small carriage in the hand (`assets/nusmp/weapon_meshes/vassal_carriage.txt`, skin `textures/entity/weapon/vassal_carriage.png`, glow skin `..._glow.png`) and gives its lines to the pure `MeshBake`, which reads the quads and maps them into the entity's frame with the pure `MeshFrame` (nose to the front, roof up, wheels on the ground, centred, scaled to 3.2 blocks). If the mesh is missing or unreadable a plain wooden wagon of boxes is drawn, so the carriage is never invisible.
* The seat points (`CarriageNumbers.SEATS`) are the bench surfaces of the art builder's model: the driver's bench at 0.06 ahead of the middle of the model (0.17 of its length ahead of the middle of the body, the draught pole not counted) and the rear bench 1.09 behind it, both at 0.50 above the ground (the cushions' top is at 0.544; the points are a little sunk into them so that a seated player's head clears the canopy); a rider's seat point (0.6 above their feet) is put on that point, as vanilla does for a horse's back.

State per wielder: the waypoint (saved in the bond: `bond.extra("carriage")` with `wpx`, `wpy`, `wpz` and `wpdim`, all optional); the carriage entity itself (memory, not saved).

## 4. Forms

Five forms, no curse series. The weapon has one look (the base look) for all of them; the form changes the chassis (through the passives) and the techniques.

| form | name | tier | parents | requirements | mastery | stats | skills (slot: skill) | passives |
|---|---|---|---|---|---|---|---|---|
| `base` | Small Carriage | 0 | - | - | 0 | armor 1 | use: call, key1: boost, key2: comfort | - |
| `filolial_carriage` | Filolial Carriage | 1 | base | feather x48, leather x32 | 120 | movement_speed 0.03, max_health 2, armor 1 | use: call, key1: boost, key2: haikuikku, key3: comfort | roomy_cabin, swift_wheels |
| `war_carriage` | War Carriage | 2 | filolial_carriage | iron_ingot x48, chain x16, #planks x32 | 200 | armor 3, armor_toughness 1, max_health 4, knockback_resistance 0.1 | use: call, key1: boost, key2: drift_ram, key3: cabin_barrier | roomy_cabin, armoured_frame |
| `hourglass_sand_carriage` | Hourglass Sand Carriage | 2 | filolial_carriage | sand x64, clock x8, ender_pearl x12 | 200 | movement_speed 0.04, luck 1, max_health 2 | use: call, sneak: waypoint, key1: boost, key2: portal, key3: haikuikku | roomy_cabin, swift_wheels |
| `queens_chariot` | Filolial Queen's Chariot | 3 | war_carriage, hourglass_sand_carriage | feather x64, gold_block x16, dragon_breath x6, tensura:medium_quality_magic_crystal x12 | 320 | movement_speed 0.06, armor 3, max_health 6, knockback_resistance 0.2 | use: call, sneak: comfort, key1: boost, key2: drift_ram, key3: crash_charge | roomy_cabin, armoured_frame, queens_pull |

The forms file passes the real forms validator (the job's forms check script: it knows `minecraft:chain` and the tag `minecraft:planks`); the one `tensura:` id is used by other systems of the mod.

## 5. Techniques

Nine techniques (6 required), one ultimate (only in the top form). Every number lives in `CarriageNumbers`; the live skills are built from it. The Crash! Charge! row says both of its blows (the crash 14 and the stomp 8) so that one foe's total, 22 raw, is inside an ultimate's 32. Most techniques are orders to the carriage and are refused unless the wielder is DRIVING it (sitting in the driver's seat of their own carriage). Refused techniques cost nothing and start no cooldown; a technique that is cancelled after it started (the wielder got off during the portal's 3 s or the charge's warning) has already been paid for.

| technique | id | kind | cooldown | cost | raw damage (base..strongest, hits per foe) | foes | heal |
|---|---|---|---|---|---|---|---|
| Call the Carriage | `carriage.call` | utility | 100 ticks (5 s) | 1% | none | 1 | 0% |
| Gallop Boost | `carriage.boost` | ordinary | 160 ticks (8 s) | 1.5% | 8..8 x1 | 12 | 0% |
| Drift Ram | `carriage.drift_ram` | ordinary | 140 ticks (7 s) | 2% | 7..7 x1 | 12 | 0% |
| Soothing Ride | `carriage.comfort` | utility | 400 ticks (20 s) | 3% | none | 6 | 12% |
| Cabin Barrier | `carriage.cabin_barrier` | utility | 360 ticks (18 s) | 2.5% | none | 4 | 0% |
| Haikuikku | `carriage.haikuikku` | utility | 300 ticks (15 s) | 2% | none | 4 | 0% |
| Mark Waypoint | `carriage.waypoint` | utility | 100 ticks (5 s) | 1% | none | 1 | 0% |
| Portal Gate | `carriage.portal` | utility | 1200 ticks (60 s) | 3% | none | 4 | 0% |
| Crash! Charge! | `carriage.crash_charge` | ultimate | 900 ticks (45 s) | 6% | 8..14 x2 | 12 | 0% |

### Call the Carriage (`carriage.call`)

* **What.** Calls the wielder's carriage. It appears 3.2 blocks in front of the wielder (or, when there is no room there, a little farther ahead or to either side: seven tries) on a spot where its whole 2.2 by 1.7 block body fits, with ground within three blocks and no lava or fire under it, facing the way the wielder looks; the wielder is seated in the driver's seat. A carriage that is already out (anywhere in the world) is sent away first, so "bring it to me" is the same action. Used while driving, it sends the carriage away: everybody aboard is put on the ground beside it. Refused (nothing spent) when the wielder rides something else or there is no room.
* **Numbers.** utility; cooldown 100 ticks (5 s); cost 1% of the maximum magicules; raw damage none; foes 1; heal 0%. Given by: base:use, filolial_carriage:use, war_carriage:use, hourglass_sand_carriage:use, queens_chariot:use.
* **Tell.** A gust ring at the place and the sound of hooves; the carriage is solid and visible.
* **Counter.** Not an attack. A foe can destroy the carriage (40 health, 90 armoured) or wait for it to leave.

### Gallop Boost (`carriage.boost`)

* **What.** While you drive: for 2.5 s the carriage runs 1.8 times faster, and the first blow on each foe it runs into is 8 raw with a shove of 1.0. The blow is struck by the carriage for its owner through the shared hit gate: each tick the foes (never allies, pets or the wielder's own summons) inside the carriage's box widened by 0.4 and pushed 1.4 ahead are hit, once each, up to 12. Refused when the wielder is not at the reins.
* **Numbers.** ordinary; cooldown 160 ticks (8 s); cost 1.5% of the maximum magicules; raw damage 8..8 x1; foes 12; heal 0%. Given by: base:key1, filolial_carriage:key1, war_carriage:key1, hourglass_sand_carriage:key1, queens_chariot:key1.
* **Tell.** A streak of wind ahead of the carriage and hooves; the carriage visibly speeds up. 8 raw is not above the 8 raw that needs a wind-up.
* **Counter.** Step aside: the carriage turns slowly at a gallop (3 degrees a tick), or block it with walls; a foe can destroy it.

### Drift Ram (`carriage.drift_ram`)

* **What.** While you drive: a ring of dust follows the carriage for 0.4 s, then everything within 4 blocks of the carriage (where it is then) takes 7 raw, a shove of 1.1 and Slowness II for 1.5 s, up to 12 foes.
* **Numbers.** ordinary; cooldown 140 ticks (7 s); cost 2% of the maximum magicules; raw damage 7..7 x1; foes 12; heal 0%. Given by: war_carriage:key2, queens_chariot:key2.
* **Tell.** A dust ring of 4 blocks that follows the carriage during the 0.4 s, then a gust ring and a crushing flash at the carriage.
* **Counter.** Stay out of 4 blocks of the carriage, or leave when the ring appears.

### Soothing Ride (`carriage.comfort`)

* **What.** Heals the wielder, everybody aboard, the carriage itself (it is an ally of its owner) and every ally within 6 blocks of the wielder or of the carriage, up to 6 in all (the people aboard are counted first), for 12 percent of their health through the shared heal gate, and washes away one harmful effect each. Refused (nothing spent) when nobody is hurt and nobody has a harmful effect.
* **Numbers.** utility; cooldown 400 ticks (20 s); cost 3% of the maximum magicules; raw damage none; foes 6; heal 12%. Given by: base:key2, filolial_carriage:key3, queens_chariot:sneak.
* **Tell.** A healing stream from the carriage (or the wielder) to each ally helped, and a chime.
* **Counter.** None needed: it is a heal; burst someone down before it comes back (20 s).

### Cabin Barrier (`carriage.cabin_barrier`)

* **What.** While you drive: for 5 s the carriage cannot be hurt (only a void or a kill command still can) and everyone aboard gets Resistance II. People who step aboard later do not get the effect.
* **Numbers.** utility; cooldown 360 ticks (18 s); cost 2.5% of the maximum magicules; raw damage none; foes 4; heal 0%. Given by: war_carriage:key3.
* **Tell.** A geodesic dome of light (the barrier effect) follows the carriage for the 5 s.
* **Counter.** Wait it out (5 s), or attack the people when they step off.

### Haikuikku (`carriage.haikuikku`)

* **What.** Fitoria's speed spell. For 8 s everybody aboard has Speed II and, while the wielder drives, the carriage itself is 25 percent faster (on top of a boost or a charge). On foot it hastens the wielder and up to 3 allies within 6 blocks. Up to 4 people in all.
* **Numbers.** utility; cooldown 300 ticks (15 s); cost 2% of the maximum magicules; raw damage none; foes 4; heal 0%. Given by: filolial_carriage:key2, hourglass_sand_carriage:key3.
* **Tell.** A wind mantle follows the carriage (or the wielder) for 2 s, and a whoosh.
* **Counter.** It is a buff: it makes the carriage hard to catch; cut it off with walls.

### Mark Waypoint (`carriage.waypoint`)

* **What.** Saves the place the wielder stands on (or the carriage's place when driving) as the waypoint: the world, x, y and z are written in the bond's "carriage" area, one at a time (a new one replaces the old). It survives death and logging out because the bond does. No carriage is needed.
* **Numbers.** utility; cooldown 100 ticks (5 s); cost 1% of the maximum magicules; raw damage none; foes 1; heal 0%. Given by: hourglass_sand_carriage:sneak.
* **Tell.** A dust ring and a compass sigil at the place for 2 s.
* **Counter.** Not an attack.

### Portal Gate (`carriage.portal`)

* **What.** From the driver's seat: opens a gate at the carriage and one at the waypoint; the carriage and everyone aboard cross after 3 s. The waypoint must be marked, in this world, and between 16 and 1000 blocks away, otherwise the technique is refused with the reason on the action bar. The land at the waypoint is asked to load while the gates turn (a chunk ticket round it, lasting a few seconds) so a far waypoint works. When the 3 s are up everything is checked again: the wielder must still drive the carriage, the land must be loaded and there must be a place for the carriage's body near the waypoint (this world's ground, no lava), otherwise the gate closes and says why (the cost is spent). Then everybody gets off, the carriage moves to the place, everyone is put just above it and, 4 ticks later, seated again, the wielder first. Nobody takes damage and nothing is placed or broken.
* **Numbers.** utility; cooldown 1200 ticks (60 s); cost 3% of the maximum magicules; raw damage none; foes 4; heal 0%. Given by: hourglass_sand_carriage:key2.
* **Tell.** A dust ring and a spinning portal ring at both ends for 3.5 s, and an ender sound; riders can see where they will go.
* **Counter.** Not an attack. A foe near the carriage can attack it during the 3 s (the gate does not protect); the gate cannot be used to follow, because only the carriage and its riders cross.

### Crash! Charge! (`carriage.crash_charge`)

* **What.** The ultimate (the wiki's Crash! Charge!). From the driver's seat: after 0.8 s of warning the carriage charges for 2.2 s at 2.3 times its speed. Each foe it crashes into (once each, up to 12) takes 14 raw and a shove of 1.6; the first foe crashed into is also stunned (Slowness VII) for 1 s as capped by the control gate, once per control lock. 0.5 s after the charge ends every foe that was crashed into and is still alive is stomped for 8 raw more (22 raw in all to one foe). If the wielder has left the carriage during the warning, the charge is called off.
* **Numbers.** ultimate; cooldown 900 ticks (45 s); cost 6% of the maximum magicules; raw damage 8..14 x2; foes 12; heal 0%. Given by: queens_chariot:key3.
* **Tell.** A dust line 20 blocks long ahead of the carriage and a heat aura round it for the 0.8 s of warning, and an angry whinny; then a gale streak.
* **Counter.** Leave the line in the 0.8 s warning, or put a wall in the way; the carriage turns slowly at that speed (3 degrees a tick) so it cannot follow.

## 6. Passives

| passive | id | effect | forms that have it |
|---|---|---|---|
| Roomy Cabin | `roomy_cabin` | The carriage seats 3 people instead of 2. | filolial_carriage, war_carriage, hourglass_sand_carriage, queens_chariot |
| Swift Wheels | `swift_wheels` | The carriage is 15 percent faster. | filolial_carriage, hourglass_sand_carriage |
| Armoured Frame | `armoured_frame` | The carriage has 90 health instead of 40. | war_carriage, queens_chariot |
| Queen's Pull | `queens_pull` | The carriage is 30 percent faster, as if the Filolial Queen drew it. | queens_chariot |

## 7. Engravings (the owner's rule 6: every weapon gets Tensura engravings)

The line for `item/WeaponEngravings.java`:

```java
Map.entry("vassal_carriage", List.of(e("sturdy", 2), e("crushing", 1), e("swift", 1), e("vitality", 1))),
```

* `sturdy` 2: a war carriage is built to take punishment and to protect the people aboard (the wiki's role for the weapon).
* `crushing` 1: it rams; the Crash! Charge! runs foes down and stomps them.
* `swift` 1: Haikuikku and the Queen's pull are about speed.
* `vitality` 1: the Soothing Ride tends the people aboard.

## 8. Files

* Logic: `heroweapon/weapon/carriage/`: `CarriageLogic`, `CarriageSpells` (the nine bodies), `CarriageEntity` (the vehicle), `CarriageEntities` (the registry of the type), `CarriageRegistry` (one per wielder), `CarriageNumbers` (no game classes), `CarriageRules` (steering, boarding, absence, no game classes), `CarriageText` (no game classes).
* Client: `client/heroweapon/carriage/`: `CarriageClient` (the one entry point), `CarriageRenderer`, `CarriageMesh` (finds the file, keeps the result), `MeshBake` and `MeshFrame` (the reading and the mapping, no game classes).
* Shared kit: `heroweapon/weapon/kit/`, described class by class in `weapon_staff.md` section 8.
* Forms: `src/main/resources/data/nusmp/hero/forms/vassal_carriage.json`. Lang: the builder's fragment (`hero.skill.carriage.*`, `hero.passive.carriage.*`, `entity.nusmp.vassal_carriage_wagon`).
* Tests: `CarriageNumbersTest` (budget, the chassis, the seats, steering, boarding and absence edges, haste, the portal's reasons), `MeshFrameTest` (the mesh-to-entity mapping: nose, roof, floor, width, a pure rotation), `MeshBakeTest` (the mesh text format: layers, both quad kinds, the glow line, broken meshes, the fallback wagon), `CarriageMeshContractTest` (reads the shipped mesh when it is in the tree and checks the contract of section 1 in the entity's frame: length, wheels on the ground and low, width inside the hitbox, nose narrower than tail, a bench surface and a roof at every seat; it was run against the art builder's real mesh and fails when the roof and floor or the nose and tail are swapped), `CarriageFormsTest`.

## 9. Tests and the self-test

* JUnit: the table against the budget (the ultimate's two blows included), the chassis from the passives, every seat inside the carriage, the steering curve, the boarding rule and the absence clock at their edges, the speed factors, the portal's reasons, the mesh mapping (a rotation, not a mirror), the forms file and the texts.
* Self-test `hero.weapon.carriage`: resolves the forms file and every skill id of every form, binds the weapon to a stand-in player at the world spawn with the weapon in hand, casts all nine techniques once (the first, Call the Carriage, spawns the carriage), then checks that the carriage is registered, names the stand-in, has the seats the form promises, refuses a stranger (the dummy) both as a boarder and as a passenger, that the stand-in drives it if it could be seated, that Call while driving sends it away and a second call makes a fresh one, and that sending it away empties the registry. It removes everything it made.

## 10. What is unverified (honest list)

Nothing has been run in the game, and the vehicle is the least certain part of this builder's work. Type-checked against API stubs: yes (signatures only, no behaviour). Unit-tested: the arithmetic, the data, the mesh mapping. Not verified:

* **Driving.** That the vanilla ridden-`LivingEntity` path really moves the carriage as intended for a player driver: that `getControllingPassenger` returning a `Player` makes the engine call the ridden hooks, that the input vector (forward only) turns into movement along the carriage's heading, that the speed attribute x factor gives about the speeds listed, that steering by the driver's look feels right (the per-tick turn limit is a guess) and that the server accepts the driver's client-side movement at the highest speeds (a charge with the Queen's pull and Haikuikku is about 35 blocks a second; vanilla's "vehicle moved too quickly" check is generous, but it has not been tried).
* **Seating.** That a rider's vehicle attachment point puts them on the benches (the seat points are the bench surfaces, a little sunk into the cushions, and the rider's own attachment is 0.6 above their feet, which is how vanilla seats a rider on a horse's back; the head of a seated player reaches about 1.7 blocks, the canopy's rim is at about 1.6, so a hat may touch the canopy), that the attachment points rotate with the carriage the way the code assumes (x to the left), and that `startRiding(force)` right after spawning works from the technique (the self-test only tries it with a stand-in player).
* **Dismounting.** That `getDismountLocationForPassenger` is the point vanilla puts a dismounting rider on (the code also places riders explicitly when the carriage goes away).
* **The Portal.** That teleporting riders and the carriage and seating them again 4 ticks later looks right to the clients, that the chunk ticket (`TicketType.PORTAL`, radius 2) loads a far waypoint in the 3 s, and that no client is left looking at unloaded land.
* **Rendering.** The baked model was looked at in a software render of the art builder's real mesh from the side, the front and the top (the carriage stands on its wheels, the roof is up, the nose with the draught pole is forward, and the seat points lie on the two benches). What was not seen is the game's own renderer: that the cut-out render type, the light and the additive glow layer look right at that size, that the shadow and the culling box are adequate, and that the renderer is registered in time (`CarriageClient.init` from the logic's constructor on the client, or from `HeroClient.init`). The contract test guards the mesh only when it is in the tree; this branch does not carry it.
* **Other systems.** That nothing else in the mod or in Tensura that reacts to every `LivingEntity` (existence points, magicule storage, damage hooks, the mod's own mob handlers) or to every `OwnableEntity` (the carriage counts as a pet of its wielder: the Charter, the Thread, the anti-magic nullification and the avatars all have pet rules) misbehaves on a carriage.
* **The stand-in player.** That a `FakePlayer` can be seated, can teleport and can be taken off a vehicle without a real connection (the self-test treats "could not be seated" as a skipped check, not a failure; any exception in a technique is a failure).
