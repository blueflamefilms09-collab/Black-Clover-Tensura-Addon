# The vassal kit (`heroweapon/weapon/vassalkit/**`): what the Mirror, Musical Instrument, Harpoon, Sewing Set and Chain share

Builder `hvd`. These classes are used only by the five weapons of `weapon_mirror.md`, `weapon_instrument.md`, `weapon_harpoon.md`, `weapon_sewingset.md` and
`weapon_chain.md`. Nothing in the kit is registered anywhere else: each weapon's logic constructor registers its own self-test, and the kit holds no state of its own except the per-wielder
memory it hands out (`PlayerState`) and a bounded log throttle in `Safe`. Nothing here changes a block, spawns an entity or reads a config (the numbers of each weapon live in its own pure table).

## 1. Why a kit

Five weapons by one builder needed the same answers to the same questions, and the questions are exactly where a bug hides until someone plays: who may I strike, who may I help, did that shot cross the glass, how do I
keep a delayed effect from acting for somebody who is gone, is a table of numbers inside the owner's balance budget, is a forms file legal. One shared, tested answer is safer than five.

## 2. The classes

| Class | Why it exists | Pure? | Tests |
|---|---|---|---|
| `Budget` | the owner's balance budget (design.md section 5) as data: a `Row` per technique (kind, raw damage per hit, most on one target, targets, cooldown, cost, heal) and `violations(...)` listing every way a row breaks it. Each weapon builds its skills FROM its rows, so the table and the game cannot differ. | yes | `BudgetTest` (11) |
| `Geo` | cone, segment distance, fan of directions, mirror image, "did a shot cross a disc since the last tick" (`sweepsDisc`), the speed curve of a pull, and aiming off a direction by yaw and pitch, in the game's yaw convention | yes | `GeoTest` (11) |
| `FormsFile` | an independent check of the forms rules of design.md section 6 on a Gson tree (base first, tiers one deeper than parents, requirement counts, stat budgets, slot words, known skills, one ultimate per form, curse fields, every skill in some form) | yes (Gson only) | `FormsFileTest` (17), and each weapon's `<Weapon>FormsTest` runs it on the shipped file via `ShippedForms` |
| `Foes` | the single answer to "foe" and "ally": a foe is a hostile monster, a creature hunting the wielder or an ally, or a player the wielder may harm (never creative or spectator, never a pet or summon of anyone, never an armor stand); an ally is the wielder, their team, party, pets and summons, and any player they may not harm. Searches return nearest first and capped. | no | self-tests |
| `Safe` | every delayed task and zone: rebuilds the `HeroCtx` when the step runs, stops quietly when the person left or the bond changed, clamps to 20 seconds, catches and logs a failure once (the log set is emptied when the server stops) | no | self-tests |
| `PlayerState` | the per-wielder memory of a weapon (panes, beat, line, needles, links): dropped on logout, cleared when the server stops | no | self-tests (its `size()`) |
| `Deflect` | which projectiles are a danger to the wielder (not the harmless vanilla throwables) and how to turn one round with the game's own deflection call | no | self-tests |
| `Moves` | pulls by velocity and dashes by teleport to the farthest spot `thread.SafeSpot` accepts, never through a wall | no | self-tests |
| `Effects` | strongest-first selection, strip with copies taken before removal, lengths for infinite effects, effect lookup by registry id for optional mods | no | self-tests |
| `WeaponSelfTest` | the in-game self-test `hero.weapon.<pkg>` shared by the five weapons (section 4) | no | itself |

### The budget, as the code enforces it

| kind | raw per hit | most on one target | cooldown | cost | other |
|---|---|---|---|---|---|
| ordinary | 3 to 12 | 12 | at least 60 ticks | 1 to 3 percent | |
| heavy | 10 to 20 | 20 | at least 60 ticks | 1 to 3 percent | |
| utility | 0, or 3 to 12 | 12 | at least 100 ticks | 1 to 3 percent | |
| ultimate | up to 32 | 32 | at least 600 ticks | 5 to 8 percent | |

For all kinds: at most 12 targets, a heal of at most 25 percent of the target's maximum health, a row id that looks like `<weapon>.<snake_name>`, no id twice. Two separate hits of one technique on one
target (pulses, an echo, a return leg) must be more than 10 ticks apart (`Budget.HURT_WINDOW`), because a creature that was just hurt ignores a second blow that is not larger for ten ticks; every
repeating technique of the five weapons is at 12 ticks or more and each numbers test asserts it.

## 3. Rules the kit enforces on behalf of the weapons

* **Damage** only through `HeroCtx.hit`, **heal** only through `HeroCtx.heal`, **hard control** only after `HeroCtx.control()`, **mana** through the dispatcher (plus `HeroCtx.pay` for the one extra, the Mirror's Covet).
  Documented exceptions: the Mirror's rebound (a reaction capped by the damage taken) and the Musical Instrument's Dirge (poison at level I, about six health); both are explained in their pages.
* **The control lock is 12 seconds and shared.** A technique takes it ONCE, at the cast, and not again later in the same technique: asking again 6 seconds later could never succeed. (The Chain's The Bill had this
  bug during construction and was fixed.)
* **Foes and allies** only through `Foes`: nothing acts on creative or spectator players, allies, the wielder's pets or summons, or somebody else's tamed animals.
* **Time.** Every delayed task goes through `Safe`, which rebuilds the context at the moment the step runs and ends the effect if the person, the bond or the weapon changed. No task runs longer than 20 seconds.
* **State.** Nothing is static except the throttle set of `Safe` and the Mirror's re-entrancy flag; the per-wielder memory is in `PlayerState`, cleared on logout and on server stop. Nothing is saved.
* **Blocks.** None is changed or placed. Movement is by velocity or teleport to a spot `SafeSpot` accepts.
* **Effects.** `VfxSpawn`/`VfxShape`, `Tells` and sounds only; no `ParticleTypes`.
* **Never throw out of a skill.** Delayed steps are wrapped; the bodies are written with null checks, and the dispatcher also wraps `cast` in a try/catch (core brief).

## 4. The shared self-test `hero.weapon.<pkg>`

Registered from each weapon's logic constructor (`WeaponSelfTest.register(kind, expect)`) so it exists as soon as the mod starts. When `/multiverse mega selftest` (the CI server smoke test) runs it:

1. reads `data/nusmp/hero/forms/<itemId>.json` from the jar and runs `FormsFile.check` with the weapon's expectation (skill ids from its numbers table, its ultimates, its sins, the count of ordinary and curse forms);
2. resolves every item id and tag of the requirements against the real registries (a wrong id fails CI here as well as in `hero.forms`);
3. checks that the logic registered exactly the skills of its table, that each form's skills exist, that cooldowns and costs are inside the budget, and, when more than the base form is loaded, that the forms the
   game loaded are the file's forms in the file's order;
4. with a stand-in player on the surface at the world spawn, bonded to the weapon (`HeroBonds.bind`), holding `HeroItems.stack(kind)` with the bond summoned, and a silent, mindless zombie three blocks ahead:
   casts every skill once, then every skill of every loaded form under that form (so the passives run), and calls every hook (`onSummon`, `onTick`, `onDamaged` with a blow and with magic, `onBlocked`, `onRelease`,
   `onFormChanged`, `onHit`, `onKill`, `onDismiss`), each in its own try/catch. A skill may refuse (return false); nothing may throw.
5. removes what it made, empties the hand, clears effects, heals and releases the bond.

It bypasses the dispatcher on purpose (no cost, no cooldown), so every cast happens. Delayed steps started by the casts find the bond gone and stop quietly.

## 5. How the tests are organised

* Every number a weapon casts with is in its `<Weapon>Numbers` (rows plus constants plus small pure rules), and its test hands the rows to `Budget.violations` (which checks the ranges of cooldown, cost, damage, targets and heal),
  pins what each multi-hit technique adds up to against its row, checks the hurt-window spacing of every repeating technique and the 20-second limit of every task, and tests each rule (e.g. the Mirror's `rayDamage` at 0, 50 and 100 gleam).
* Pure bookkeeping lives in its own class without any game import and with its own test (`PaneBook`, `GleamMeter`, `MirrorLedger`, `Tempo`, `TetherBook`, `ReelRule`, `NeedleBoard`, `WeightLedger`).
* The shipped forms files are read from the source tree by `ShippedForms` and run through `FormsFile` by each weapon's `<Weapon>FormsTest`, so a broken file fails `gradle test` before it can reach the game.
* Run locally with `WT=<worktree> KEY=<key> bash $S/mega/jt.sh <fully.qualified.TestClass>...`. 155 tests in 21 classes at the time of writing.
* One mutation check was done on purpose (section 6).

## 6. Mutation check

To prove that a test can actually fail, two rules were broken on purpose, the test re-run, and the file restored (a `diff` against the saved original printed "identical" both times):

1. `Tempo.preview`: the rule "a lapsed phrase starts again from nothing" (`if (gap > max) return 0;`) was changed to keep the old harmony. Result: 7 passed, 1 failed, and the failure was the right test:
   `TempoTest.aLapsedPhraseStartsAgainFromNothing -> and the next technique begins a new one: expected <0> but was <2>`. Restored: 8 passed.
2. `Budget.violations`: the minimum cooldown of an ultimate was lowered from 600 to 300 ticks. Result: 10 passed, 1 failed:
   `BudgetTest.anUltimateIsCappedAtThirtyTwoAndNeedsASixHundredTickCooldownAndFiveToEightPercent`. Restored: 11 passed.

## 7. Unverified, assumed, and to be checked by the lead

* **Nothing has run in the game.** The kit and the five weapons were type-checked against the API stubs that `tools/typecheck/stubgen.py` generates from the Minecraft, NeoForge and Tensura jars (the only error that remains in the closure of these files is the stub artifact "MobEffectInstance is abstract; cannot be
  instantiated") and the pure parts were unit-tested with the local shim. No `javac`/Gradle build of the whole project and no JUnit run of the real framework was possible here.
* The self-test is written for the FakePlayer pattern of `arc/ArcSelfTests` and `labyrinth/mega/SelfTests` as it stands in this worktree; `SelfTests.player(...)`, `SelfTests.register(...)` and `SelfTests.Result` were used
  with the signatures found there.
* `HeroForms.of(kind)` is the stub of this worktree (it may return only the base form); the check that the loaded forms equal the file only runs when more than one form is loaded.
* `thread.SafeSpot`, `book.SpellRuntime` and `multiverse.Squads.partyMembers` are existing classes reused as they are; their behaviour was read, not run.
* `Foes.isAlly` treats a player the wielder cannot harm (`canHarmPlayer` false) as an ally, so Stitch Mend or Glutton God Tango can help players who are on no team; the server's PvP rules decide.
* The projectile deflection uses `Projectile.deflect(ProjectileDeflection.NONE, owner, owner, true)`; the owner is changed to the wielder, which is the intended effect and is the same call a player's blow makes.
