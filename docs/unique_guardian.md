# Unique Guardian

Package `com.newuniverse.nusmp.unique`. Server config `nusmp-unique-server.toml`. Saved file `nusmp_unique.dat`. Self-tests `unique.rules`, `unique.kinds`, `unique.world`.

## What it does

Some items exist only once in a world (a 1-of-1): the Greed sword, the Shishou token, a Charter grimoire, a Dragon Ball, a unique relic or Infinity Stone, a Holy Weapon.
Each of them has its own rule that says "already exists / already dropped / already claimed", and that rule blocks the item from being given out again.
If the only copy is destroyed (void, lava, fire, explosion, a cleared inventory, a creative delete, a container that was broken by a command, a player who never comes back), that rule would block it for ever.

The guardian watches where the one copy is. When it is **confirmed lost**, it resets that blocking flag, ledger entry or claim, so the item can be obtained again from its **normal sources**. That is all it does:

* It never puts an item into anybody's inventory, into a chest or at the spawn. There is no instant give-back.
* It invalidates the old copy, so an old copy that turns up later dissolves (a message tells the carrier) and cannot become a duplicate.
* It writes every return to the server log and tells the operators online.
* It keeps dropped uniques safe: an item entity of a unique is invulnerable and never despawns, and one that falls into the void is lifted back to the surface (the End spawn platform in the End).

Each kind keeps its own rules. The guardian talks to a kind only through the small interface `UniqueKind` (`idOf`, `valid`, `exists`, `release`, `label`, `pool`). The five adapters are in `unique/kind/`.

## What counts as lost

A loss has to be confirmed. A guess would make a duplicate.

| Situation | Counted as lost? |
| --- | --- |
| Another player picked it up or took it from a chest (theft) | No. The record simply gets the new holder. |
| The holder logged out with it, died with it, or it is in a chest, ender chest, shulker box or bundle | No. All of these are looked at. |
| The tracked item entity was removed with a destroying reason (killed: fire, lava, cactus, explosion) or fell below the build floor | Yes, after 5 seconds (100 ticks), unless it is seen again. |
| The tracked copy is not found anywhere (a scan of its holder misses it, a plain discard of the item entity, a container block that is gone) | Yes, after `graceSeconds` (default 300 s = 5 min) without being seen again. |
| The holder is offline for `absenceDays` and the copy was last held by them | Yes, after those days (0 turns this off). |
| An operator runs `release` | Yes, at once, no clocks. |

Any sighting during a clock cancels it. A unique is never returned twice for the same copy (the record is in the pool until a new valid copy is seen), and a flap guard allows at most 3 returns of the same unique per hour.

## Back in the loot pool: what it means per kind, and where it then comes from

| Kind | id | What the guardian resets | Where it comes from afterwards |
| --- | --- | --- | --- |
| Greed sword | `greed` | The owner is released through `GreedService.release`, then `GreedData.vanish()` (the "already spawned" flag and the token). | Its normal sources: a Crowned boss-tier bearer, `/greed give`, or adoption of a bare stack. |
| Shishou token | `shishou` | `ShishouCap` is reset to its start state, the live minion is cleared and everybody is synced. | The natural daily roll (0.4 %), or `/shishou spawn`. |
| Charter grimoire | `charter:<boss>` | The drop claim in `ModWorldDropData` (`DropLedger`) for that boss, then all clients are sent the new state. | The boss's next kill rolls its drop again, with its normal chance. |
| Dragon Ball | `dragonball:<set>:<star>` | `DbManager.returnToWorld`: the ball is relocated and the old copy is no longer current. | The ball is placed again at a new far place by the ball sweep, and is found like on a new world. |
| Unique relic, Infinity Stone | `relic:<relicId>` | The `UniqueLedger` entry is set to not live and the Order forgets it (`UniqueLedger.releaseToPool`). | Its loot, boss or command source issues it again; the old copy is a duplicate and dissolves. |
| Holy Weapon (shield, sword, spear, bow) | `hero:<item>` | The `HeroRegistry` entry is cleared. This is only done for a holder who is away for `absenceDays`. | The acquisition ritual: another player can claim it. If the old holder comes back and somebody else has it, their bond dissolves. |

Notes:

* Greed and Shishou keep their own earlier rules (the 14-day release of Greed, the Shishou recovery). If one of those already returned the unique, the guardian only retires its record and says so to the operators.
* A Holy Weapon is a projection of a bond and cannot be destroyed as an item, so only the "holder never comes back" case applies. It only runs while the `holyUnique` setting of the Hero weapons is on.
* A Charter grimoire and a Dragon Ball carry no token of their own, so the guardian stamps the item with a hidden epoch (`ug_epoch`) when it first sees it. After a return, a copy with an older epoch is an old copy.

## Settings (`nusmp-unique-server.toml`, SERVER)

| Key | Default | Meaning |
| --- | --- | --- |
| `unique.enabled` | `true` | Master switch. Off restores the old behaviour of every kind (nothing is watched, nothing is returned). |
| `unique.graceSeconds` | `300` (30 to 86400) | How long a copy must be accounted for nowhere before it is lost. |
| `unique.absenceDays` | `21` (0 to 3650) | A holder who stays offline this long frees what they hold. 0 = off. |

## Commands (operators only, permission 2)

* `/multiverse unique list` shows the settings, how many are tracked, not accounted for and in the pool, and up to 30 lines.
* `/multiverse unique status <id>` shows one record, whether the owning system believes a valid copy exists, and where it comes from once it is back in the pool. Holy Weapons (`hero:legendary_sword`, ...) show their holders.
* `/multiverse unique release <id>` forces a unique back into the loot pool now (no clocks, no flap guard). The id is validated and suggested.

## Cost (worst case)

Everything is on the server, nothing is sent to clients (no packets), nothing runs on the client.

* **Per player, once a second** (staggered by entity id, so not every player on the same tick): one look at the main inventory, armour, off-hand, ender chest, cursor, the open menu and Curios, one level into shulker boxes and bundles. At most about 200 stacks and one pass over the tracked records (a handful). Each stack is one item-class test before anything else.
* **Item entities**: one `instanceof` on join and on leave; only the tracked unique is handled, and only a few item-class tests for any other item entity.
* **Server tick**: three modulo tests. Every 4th tick the void rescue looks at no more than 64 tracked dropped uniques, by uuid in their own level (no world scan, no chunk is loaded). Every 20th tick the clock pass over the records. Every 1200th tick (once a minute) the minute pass: one owner-system lookup per record, one loaded-chunk container check per contained record, and one registry read for each of the four Holy Weapons.
* **Memory and disk**: one small record per unique (a few dozen at most) in `nusmp_unique.dat`, written only when something changed or once a minute while a unique is being carried. Hero holder times are one map entry per Holy Weapon holder.
* **Log and chat**: one log line and one message per operator for each return. The flap guard caps the returns of one unique to 3 per hour.

## Tests

* `UniqueDataTest` (pure): lost to the pool, theft is not a loss, never returned twice, old copy dissolves, no second copy while one is seen, the flap guard, the absence rule, retire, the id shape.
* `UniqueSourceScanTest` (reads the source): the pure classes import no game class, no client class anywhere, no world scan or chunk load, every handler is gated and guarded, SERVER config.
* Self-tests on the dedicated server in CI: `unique.rules`, `unique.kinds` (every kind identifies its own stack and id), `unique.world` (a stand-in unique with two stand-in players: held, theft is not loss, grace, destroyed goes back once, tracked again, the guardian puts nothing in an inventory).

## What is not covered (honest list)

* Items inside modded backpacks or other mod containers (not shulker boxes or bundles) are not seen. A unique kept only there counts as not accounted for and returns after the grace, while the copy still exists. Such a copy becomes an old copy and dissolves when found, but its carrier loses it.
* A copy that was never stamped or never seen by the guardian (given before this system, kept in an unloaded chunk or an offline player's data) can survive a return. For tokened kinds (Greed, Shishou, relics) the old token is invalid and it dissolves; for the Charter and Dragon Ball kinds it dissolves only once it has an epoch stamp.
* Pair relic halves are left to the Order's own rules.
* A grimoire given by an operator without a drop claim has nothing to reset.
* A container is checked only when it is a block entity container in a loaded chunk.
* Boss drops, the sin weapons, and the Hero projection items are not 1-of-1s in this sense and are not covered.
* The Order's own rule that marks a relic as lost the moment its item entity is destroyed stays as it was; the guardian then retires its record without a second return.
* The mod could not be compiled or played in the session that wrote this. The pure rules are unit tested; the glue code that touches the game has not been built or run yet, so CI (build plus the server smoke test) is its first check.
