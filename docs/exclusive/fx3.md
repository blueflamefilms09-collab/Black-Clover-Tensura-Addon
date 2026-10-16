# FX3: three owner-reported fixes in the 1.44.0 exclusive casters (release 1.46.0)

Nothing was run in game. Type-checked against the API stubs (only stub gaps remain, negative controls flagged real mistakes); the new pure classes
have JUnit tests that were run with the JUnit shim. Rule 2 kept: nothing deleted; every old behaviour sits behind a plain config switch.

## 1. Butoh dance is a passive

Cause: `butoh_dance` was a page you had to cast and cast again to stop.

Change (REPLACEMENT of the toggle, old behaviour kept behind `butoh.passive=false`):
- Anyone who HAS `book_butoh` dances by themselves (dance state, looping animation, evasion, hitbox shrink, after-images, deflect bonus). Nothing is stored
  except the resting flag `DanceRest` on the skill tag, so login, respawn and dimension change simply re-arm it at the next check.
- `butoh/DancePassive` (pure): `decide(dancing, wanted, blocked, manaShare, restart)` -> START / STOP / NONE. A dance that is on is never stopped by a low
  share (the upkeep stops it when it cannot pay); a stopped dance restarts only at or above `butoh.restartShare` (0.15): hysteresis, no flicker.
  `drainFactor(idle, idleFactor)` and `Activity` (moved or fought in the last 20 ticks) give the idle upkeep: standing still pays `butoh.idleDrainFactor`
  (0.5), moving or fighting pays 100%.
- `butoh/ButohDance.passiveTick` (called from the existing tick): non-dancers are checked every 5th tick (cheap), dancers every tick. Blocked = stun
  (paralysis, sleep, petrification), sleeping, spectator, time stop, dead, riding, swimming, gliding, creative flight, climbing, crawling. A dimension change ends
  it quietly and it re-arms. Upkeep failure ends it with the old "mana runs dry" line.
- The page `butoh_dance` stays; in passive mode a cast flips resting / dancing and shows "Butoh dance: resting" / "Butoh dance: dancing". It costs nothing in
  passive mode (`ButohBook.getMagiculeCost` returns 0 for that page only), the upkeep is the price. The Kiato lock applies only to the page text; the passive
  is for everyone with the Butoh skill.
- Client (`client/butoh/DanceClient`): the dance clip is not played while an item is in use (the clip is taken off, put back afterwards) and not while a
  page's own one-shot cast clip plays (`CastAnimClient.oneShotActive`, new accessor): the cast animation wins and the dance loop, kept as the clip to return
  to, resumes. Move changes during a cast are picked up the tick the cast clip ends.
- New config (`nusmp-butoh-server.toml`, section butoh): `passive` (true), `idleDrainFactor` (0.5), `restartShare` (0.15).
- Tests: `DancePassiveTest` (8).

## 2. Rouge rests on the HEAD again, and can be named

Cause: the 1.44.0 entity sat on the shoulder (`SIT`); and `isPickable()` was false, so a name tag could never reach her.

Choice (documented): the entity stays the logic body; once she has settled on the head the entity is NOT drawn and the OLD head-cat layer
(`RougeCatLayer` / `RougeCatModel`, unchanged placement, scale 0.55, sit pose) draws her on the player. Exclusive by construction: `RougePerch.headCatDrawn`
and `bodyDrawn` are opposites (tested). The first 8 ticks of resting the entity is still drawn so the hop up is seen, then the layer takes over.
- New state `RougeEntity.HEAD = 7` (appended). `settle()` and the end of a reknit choose HEAD when `thread.rougeOnHead` (true), else the old SIT shoulder. After
  a tap she first scurries back if she is more than 3 blocks from the caster (no flying across the field).
- Server: `glueToHead` holds the entity at the top of the head (`RougePerch.headY`: 1.875 standing, 0.25 lower sneaking), eased in. The entity carries the
  position, the name plate and the interaction; it is not a vanilla passenger.
- Client: `RougeRest` is a map owner -> cat, written by each client-side cat every tick while she is settled on the head; the layer asks it (one lookup per
  player per frame, no allocation). `RougeCatLayer` draws for the old payload (`thread.rougeEntity=false`) or the map. Removal clears the entry.
- Naming: `isPickable()` is true on a client whose player holds a name tag (hook `RougeRest.namingHand`, set in `ThreadClient.register`), false otherwise, so
  she never blocks the crosshair; `canBeHitByProjectile()` is false so arrows never hit her. `mobInteract` accepts a NAMED name tag: only the caster may name
  her (another player's tag is swallowed), the tag is used up outside creative, the name is shown above her (renderer calls `super.render` for the plate; on the
  head the plate is drawn above the head cat) and stored on the caster's thread grimoire (`RougeName`), and `RougeManager.summon` gives it back on a recast.
  Sneak + empty hand right click opens nothing (not handled, falls to `Mob.interact`, which does nothing for her). Because the head cat is hard to aim at,
  `RougeNameEvents.onUseNameTag`: look straight up (pitch -60 or higher) and use a named name tag in the air to name her. Lead line below.
- Kept: fate events, mana cost, tap, hop, unravel, `thread.rougeEntity` switch, RougeCat payload and all old classes.
- New config (`nusmp-thread-server.toml`): `thread.rougeOnHead` (true).
- Tests: `RougePerchTest` (4). Unverified in game: the exact look of the entity-to-layer hand-over, and the pick box with a name tag in hand.

## 3. Soul Corpse Magic spawns nothing

Causes found (each was a real bug or gap):
1. THE main cause: the Soul Corpse pages are appended after the generic five, and a page opens only when mastery reaches its place in the book
   (`MasteryPages.masteryCost`: page 6 of 10 needs 60%). Reanimation was sealed for most players. Fix: `Corpses.openStarterPage` (player tick, every 2 s) opens
   `corpse_reanimate` for every holder of the grimoire, with a one-time action-bar line. Jimmy, Alfredo, Carl and the pact still open by mastery.
2. Reanimation only raised the dead of the last 60 s; with an empty ledger it did nothing. Fix: it now CONJURES (a, below).
3. Named puppets were placed straight ahead at the caster's own height without any check: in a wall, on a slope or in a tunnel the body spawned inside blocks
   and suffocated at once, or the cap/roster dropped it. Fix: `findSpot` (24 tries, free box, sturdy ground) with the caster's place as the last resort.
4. Daylight: zombies, husks and skeletons burned to death within seconds on the surface, before their 60 s. Fix: raised bodies get Fire Resistance for their life.
5. The locks: `corpse_jimmy`, `corpse_alfredo`, `corpse_carl` are locked to Rades' canon grimoire in `CharacterSpells` (shared file). Lead action below.
Checked and found fine: entity types and attributes are registered (NUSMP lines 289-291), renderer registered (line 137), `finalizeSpawn` is called for
raised mobs, the hooks do not cancel a fresh spawn (`onJoin` only cancels bodies loaded from disk), the roster cap crumbles only the oldest, MISC category.

(a) Conjure: `corpse/ConjurePool` (pure): setting by dimension, water, dark, cold, dry; weighted vanilla pool (zombie, skeleton, husk, stray, drowned,
zombified piglin, wither skeleton rarely), optional Tensura `ghoul` / `wight` at weight 1 only if the entity types exist (`corpse.conjureTensura`); count 1 to
3 by mastery (1/3 and 2/3 of the bar), capped by `raiseMax`. `Corpses.conjure` finds a spot, pays `corpse.conjureManaPercent` (3) per body through
`CorpseMana`, then calls the same `raise` as the ledger path: same decay overlay, owner-bound AI, `maxActive` cap, VFX; plus a dirt burst. Ledger bodies are
still preferred (the 1.44.0 behaviour is the first branch); `corpse.conjure=false` restores it exactly.
(c) Action-bar lines: "The dead answer: ...", "The earth gives up a corpse" (with "(not enough mana for more)" if cut short), "Nothing to raise: not enough
mana.", "Nothing to raise: there is no room for a body to rise here.", "Nothing to raise: no one has died near you in the last minute." (conjure off), puppets
"X will not come here: the world refused the body." New config: `corpse.conjure`, `corpse.conjureTensura`, `corpse.conjureManaPercent`.
Tests: `ConjurePoolTest` (7).

## NEW vs REPLACEMENT
- NEW: DancePassive, passive config, RougePerch, RougeRest, RougeNameEvents, naming, rougeOnHead, ConjurePool, conjure, starter-page opening, spot finding, fire resistance.
- REPLACEMENT (old kept behind a switch): Butoh toggle -> passive (`butoh.passive`), shoulder -> head (`thread.rougeOnHead`), ledger-only Reanimation -> ledger first then
  conjure (`corpse.conjure`).
- KEPT: every old page, class, effect, VFX, animation, payload and lang key.

## Unverified
Everything in game: the Butoh clip hand-over with items and casts, the head-cat / entity hand-over, aiming a name tag at the head, ground bodies climbing out, the mastery
page opening message. Not type-checked against real jars (stubs only).
