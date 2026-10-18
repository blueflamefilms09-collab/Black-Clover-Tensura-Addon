# The Glass kit (`heroweapon/weapon/glassworld`): what the Fan, the Scythe, the Katana and the Book share

Builder `hvc`, job 1.62.0 (the weapons of The Rising of the Shield Hero). This page is the WHY of the small shared kit the four weapons of this builder
(`docs/heroweapon/weapon_fan.md`, `weapon_scythe.md`, `weapon_katana.md`, `weapon_book.md`) are built on. The name comes from Glass' World, the world of the
Vassal Weapons in the series (https://shield-hero.fandom.com/wiki/Glass'_World), which is where these four weapons belong.

## 1. Why a new package, and where it sits

* The brief gives the builder four folders (`weapon/fan`, `weapon/scythe`, `weapon/katana`, `weapon/book`). Four weapons that all need the same dozen game actions
  (who is a foe, a cone of foes, a delayed task that stops when the wielder is gone) would have written them four times and let them drift. The kit is therefore ONE
  new package, `heroweapon/weapon/glassworld`, which no other builder uses. It is new files only: no shared file is edited, nothing is registered by it, and the lead
  has nothing to integrate for it. If the lead wants a different folder for it, it is a move of nine files and one import line per weapon.
* Nothing in the kit is a Minecraft system of its own: it calls the core's `HeroCtx` for damage, heals, hard control, costs, effects and sounds, and the mod's own
  effect and spell-runtime helpers for the rest. It changes no block and spawns no entity.

## 2. The files (the WHY of each)

| file | why | pure? |
|---|---|---|
| `Budget` | The owner's balance budget (design.md section 5) as code: the four kinds of technique (ordinary, heavy, ultimate, utility) with their limits for one hit, for one foe per cast, for the rest between casts, for the cost, plus the most foes (12) and the longest duration (400 ticks). Each weapon states every technique as a `Spec` row; one test per weapon asks `Budget.violations(table)`, so a later edit that makes a technique too strong, too cheap, too wide or too long fails a test. The in-game self-test checks that each skill object really carries its row. | yes |
| `Expiry` | "This key is active until game tick T" (the mist veil, the floating scythe, the parry window, the foe that was just stripped). Bounded (a table that only grows is a leak), driven by the level's game time and not by a counter, and able to forget. | yes |
| `PerPlayer` | The per-wielder runtime state (residue, souls, tempo, pages) keyed by the player's UUID: made on first use, bounded, cleared in one call. The owner's server rule is that no static state may outlive a server; each weapon clears its `PerPlayer` tables on logout and on `ServerStoppedEvent`. | yes |
| `Geo` | The arithmetic of a dash (horizontal speed fixed, the pitch clamped so looking at the sky does not launch the wielder, a lift on the ground), a pull (never overshoots), a cone test and the stop short of a wall. Plain arithmetic is exactly where a sign error hides until a player flies away, so it is tested with numbers. | yes |
| `Immunity` | The rule for a foe's vanilla damage immunity. A foe hurt a moment ago shrugs off a second blow that is not larger than the first, so the second and third cut of a flurry, or a rider that follows a melee blow in the same tick, would do nothing and the damage the budget promises would be about half. The older weapons of this mod cut the immunity short before such a blow (`invulnerableTime`); `GlassKit.strike` does it for all four weapons with this rule: cut to ten ticks so the blow lands in full, except that the hits of a quick series (less than ten ticks apart) keep the vanilla immunity on a PLAYER, so a flurry cannot stack past the balance law's 40 percent cap on players. | yes |
| `Fmt` | The one way a number is printed in a description (`5`, `5.5`, `0.25`; ticks as seconds; shares as percents), always with a dot. The descriptions are built from the same constants as the code, so a text cannot say 5 while the code does 6. | yes |
| `Forgetful` | One method, `forget(UUID)`, that a logic with per-wielder state implements, so the self-test (and the logout handler) can drop a wielder's state without knowing which weapon it is looking at. | yes |
| `GlassKit` | The game glue: `isFoe` / `isTarget` (never a creative or spectator player, an ally, the wielder's own pet or summon, an armour stand; passive animals and villagers are not hit by accident), `foes` (a ring), `foesInCone`, `foesAlong` (a line), `focus` (the foe under the crosshair), `friends`, `clipEnd` / `reach` / `ground` (rays that stop at walls), `push` (the core's shove, plus a mark so a PLAYER is moved on their own screen: a bare shove is only seen by the server), `impulse` and `drawToward` (pushes and pulls, a heavy foe moves less), `later` and `pulses` (delayed tasks and counted series that stop when the wielder dies, leaves or changes dimension; none longer than 20 seconds), `ring` / `line` / `follow` (the shared effect calls), `swing`, `control` (the shared 12 second lock, said aloud when refused), `strike` and `striking` (see section 3). Every scan is capped (24 blocks, at most the number asked for, nearest first). | no |
| `GlassSelfTest` | The one self-test body that each weapon registers under `hero.weapon.<pkg>` (section 4). | no |
| `FormsRules` (test tree) | The rules of a forms file (design.md section 6) as code, run by each weapon's `*FormsTest` on the JSON in the source tree: base form first, a real tree, small well-formed requirements, stats inside the budget and the ability bonus inside a quarter of it, real input slots, only skills and passives the logic implements, at most one ultimate to a form, nothing left unused. It lives in the test tree because only tests use it; the registry check (is that item id real?) is the forms system's own self-test in the game. | yes (tests) |

Tests of the kit: `BudgetTest` (a good row passes, each limit is caught, the limits are exactly the owner's numbers, a repeated id is caught), `ExpiryTest`, `FmtTest`,
`GeoTest`, `ImmunityTest` (an open foe is left alone, a mob or a single strike is cut to ten, a rapid series on a player keeps the vanilla number, nothing is raised), `PerPlayerTest`; `FormsRules` is exercised through the four `*FormsTest` classes.

## 3. The `striking` convention (read this if you wire `onHit`)

The damage of one of the weapon's own techniques, dealt through `HeroCtx.hit`, is a damage event of the wielder like any melee blow. If the core forwards every damage
event of the wielder to `HeroLogic.onHit`, a rider added by a logic (a phantom cut, a riposte) would call `onHit` again with its own damage. To make that impossible the
four weapons deal ALL technique damage through `GlassKit.strike(ctx, target, raw)`, which sets a flag for the length of the call (restored in a `finally`), and each
logic's `onHit` returns at once while `GlassKit.striking()` is true. If the core only calls `onHit` for real melee swings, the flag is harmless. The flag is a static
boolean, but it is only ever true inside a synchronous call on the server thread and is always put back, so it cannot outlive a server.

### Damage immunity and the quick series

`GlassKit.strike(ctx, target, raw)` lands in full; `GlassKit.strike(ctx, target, raw, true)` marks the hit as one of a quick series on one foe. The series marked this way are the Scythe's Deadly Dance (4 ticks apart), the Katana's Triple Thrust (4) and Stardust Blade (3) and the Book's Needle Rain (5). Series with a gap of ten ticks or more (the Fan's petal tornado, the Flying Circle's two passes, the Dance of Sakura's formations, the Floating Scythe, Libraria) and every rider or single hit use the plain call. Against mobs the two calls do the same; against players a marked series keeps the vanilla immunity.

## 4. The self-test (`hero.weapon.fan|scythe|katana|book`)

Run it with the existing command `/multiverse mega selftest hero.weapon.<pkg>` (permission 2; also run by the CI server smoke test). It checks, then casts:

1. the registry: the logic is registered, every skill id carries the weapon's prefix and is unique and is the object the registry holds under that id, every skill has a name
   and a description, at least six skills;
2. the forms: the first form is the base form, every slot is a real input, every skill a form lists exists and belongs to this weapon, at most one ultimate to a form;
3. the rows: every skill's rest and cost equal its `Budget.Spec` row, the ultimate flag agrees with the row, every row is used;
4. the game: it binds the weapon to a stand-in player (a `FakePlayer` from `SelfTests.player`), puts the item in its hand, stands two dummy zombies (2000 health, no AI)
   3.5 and 5.5 blocks in front of it, casts every skill of the weapon with the base form, then every skill each form lists with that form, and calls every hook of the
   logic (summon, tick, hit twice, damaged by a mob and by a fall, blocked, kill, release, form change, dismiss), each inside try and catch: a skill may refuse (return
   false) but must never throw;
5. the weapon's own extra round (the Katana parries a blow, the Book reads a whole volume and casts Libraria);
6. the clean-up (in a `finally`): the dummies are discarded, the item is taken from the hand, the bond is released, the stand-in's health and effects are restored and
   the logic's state for it is forgotten.

What it cannot prove: how anything looks or feels (see each weapon's page, "What is unverified"). Assumptions: the stand-in player has the no-op connection NeoForge gives a
`FakePlayer` (so packets to it are dropped); the overworld chunk around (0, 100, 0) is loaded when the dummies are placed (if it is not, the Book's Libraria step is skipped
and the other steps still run). Delayed tasks that a cast leaves behind may still fire after the test has returned; they find the dummies gone and the wielder not
reachable, and do nothing.

## 5. Config keys, commands, sources

* Config keys: none. Commands: none of its own (the self-test command belongs to the Mega-Dungeon system).
* Sources: `docs/heroweapon/design.md` sections 4 to 7 (the contract, the budget, the forms file); `HERO_COMMON` / `WEAPON_COMMON` rules of the job; the Glass' World
  page above for the name.

## 6. What is unverified

* **Nothing here was run in a game.** The pure classes (`Budget`, `Expiry`, `PerPlayer`, `Geo`, `Fmt`) are covered by unit tests that ran; `GlassKit` and `GlassSelfTest`
  were type-checked against the API stubs and read twice, not executed.
* **`getEntitiesOfClass` with a predicate and `Level.clip`** are used as the vanilla 1.21.1 API defines them (an axis-aligned box and a collider clip that ignores fluids).
* **`isAlliedTo`** is what the game counts as an ally (a team mate, a tamed pet of the same owner); a third-party mod that changes it changes who is a friend.
* **`LivingEntity.invulnerableTime`** is read and written as the vanilla public field (the older code of this mod does the same); a mod that replaces the immunity logic of an entity would change how the cuts of a flurry land, which was not checked.
* **The `striking` flag** assumes the core calls `onHit` synchronously from the damage event; if it queues the call for a later tick the flag cannot see it, and a rider could
  answer a technique's damage once (the weapons' riders are small and none can chain, so the worst case is a few extra points of damage, not a loop).
