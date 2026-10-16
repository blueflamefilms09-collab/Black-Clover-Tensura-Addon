# KT: Kiato's Butoh dance (release 1.43.0)

Everything here is NEW. Nothing was replaced. Every old Butoh page (Dance Strike ... Seabed Hymn, Ebb Step, Trance Dance, Tidal Veil) is kept with the
same id and order; `ButohArts.ebbStep` is not edited (the deflect dash is its own code). Nothing could be run in game (no compiler / game in the build
session): see "Verified / unverified" at the end.

## What was added

| Part | Where |
|---|---|
| Page `butoh_dance` (the dance state, a toggle: cast again to stop) | `book/ButohBook.java` (appended), `book/ButohArts.dance`, `butoh/ButohDance` |
| Page `butoh_dash_deflect` (dash + missile deflection) | `book/ButohBook.java` (appended), `book/ButohArts.dashDeflect`, `butoh/ButohDash` |
| Evasion, shifting hitbox, sidestep | `butoh/ButohEvents.onIncomingDamage`, `butoh/ButohDance.slip`, `butoh/DanceScale` |
| Dance-cast +15% | `ButohBook.onDamageEntity` (additive block at the end; Trance Dance logic untouched) |
| Eight looping dance clips | `tools/gen_cast_animations.py` (`DANCE_CLIPS`), `assets/nusmp/animations/player/cast.animation.json` |
| Dance sequence sync (seed once) | `butoh/DanceChoreographer`, `butoh/DanceStatePayload`, `client/butoh/DanceClient` |
| VFX `BUTOH_DANCE_RIPPLE`, `BUTOH_AFTERIMAGE` | `vfx/client/layer/ButohLayer.java` (additions only) |
| Preview scenes | `tools/vfx_preview/scenes.d/butoh_dance.json` |
| Config | `butoh/ButohConfig` (`nusmp-butoh-server.toml`) |

Both pages are appended last in ButohBook's own list. Note: GrimoireBook adds the universal pages (Devil Union, Summon Grimoire, Store/Draw Weapon,
Mana Strike, Mana Zone, signature spells) AFTER the family list, so those shift by two mode numbers for the Butoh book, exactly as they do for the Soul /
Song books when ExclusiveExt appends pages. Page ids do not change.

## How it works

### The dance (`butoh_dance`)
* Casting starts the dance; casting again ends it. The page costs 4% of max magicule (floor 100) and has a 2 s cooldown against toggle spam.
* `ButohDance` keeps one in-memory `Session` per dancing player. It is never saved: the dance ends on logout, death, dimension change, stun
  (Tensura paralysis / sleep / petrification), sleep in a bed, a time stop (`TimeStop.isFrozen`), spectator mode, server stop, or empty mana.
* Mana upkeep: once a second, `butoh.danceDrain` (default 1%) of the maximum magicule, times `ManaLaw.costMult`, taken from the Tensura existence
  storage the same way Spirit Dive does it. The first cast goes through the normal page cost path.
* The moves come from the pure `DanceChoreographer`: a 64-bit seed (from the player id and the start tick) drives a walk over 8 moves (sway, spin,
  ripple, crouch-wave, lunge-feint, fold, drift, rise). The move never repeats the one before it, and each lasts 12 to 30 ticks. The server sends the
  seed and start tick ONCE (`DanceStatePayload`, also when a player starts tracking a dancer); server and every client walk the same sequence on the
  game clock. A client's game time can be a tick or two behind; at worst a move changes 1 or 2 ticks late on the screen. No per-move packet.
* Animation: the mod's OWN animation player is used (`anim/CastAnim`, `client/CastAnimClient`, `PlayerModelMixin`). Player Animator and ParCool are
  NOT used and are not dependencies. `DanceClient` hands each move's clip name to `CastAnimClient.receive`. The dance clips are looping, so
  `CastAnimClient` crossfades from one to the next (it only keeps a crossfade base for looping clips). The client re-names the current clip 4 ticks after
  the start in case the page's own release clip (`cast_release_side`, played by `GrimoireBook.castPage`) took the arms in the meantime. Casting another page while
  dancing plays that page's chant and release clips; the dance clip comes back at the next move change (at most 30 ticks).
* The clips: each is a closed loop with smoothstep sub-keys baked in (the client samples linearly). `CastAnimClient` today only renders the arms and
  legs (it deliberately does not apply the body or head); the body and head keys are written anyway so the clips are complete poses for a later
  renderer and for Blockbench, like the older clips.

### Evasion and the hitbox
* `LivingIncomingDamageEvent` at HIGH priority. For a dancing server player: skip when the source bypasses invulnerability (this covers /kill and the
  void), is `FELL_OUT_OF_WORLD`, or has no attacker (fall, fire, poison, drowning, ...). Otherwise the chance is the current move's base (35% to 65%,
  `EvasionTable`), minus 20 points when the attacker is a boss (`BalanceLaw.isBoss`), never above `butoh.evadeMax` (default 0.65, held at 0.95 whatever the
  config says: never a guarantee). A dodged hit is cancelled.
* Sidestep (`butoh.danceSidestep`): the dancer slips one step (1.0, 0.7 or 0.4 block) at right angles to the blow, alternating sides, only to a spot
  with free space, no liquid and ground within 1.5 blocks; the same-side step is tried first, then the other side. Otherwise the hit is only cancelled.
  An ink after-image (`BUTOH_AFTERIMAGE`, client only, no entity) is left where the blow would have landed.
* Hitbox: a transient modifier `nusmp:butoh_dance_scale` (ADD_VALUE) on `Attributes.SCALE`, -0.15 to -0.25 by move (crouch-wave and fold are smallest), so
  the hitbox and the body shrink together. The pure `DanceScale` gate says "set X / clear / nothing" and remembers what it applied; the game side does only
  that. It is removed on every exit (stop, death, logout, server stop). Growing back needs room: if the bigger body would stand inside a block (a
  low tunnel) the modifier stays until it fits, forced after 10 s. The modifier is transient (never saved), so a crash cannot leave a shrunken player.
* The "after-image offset of about 0.4 block" is the ghost drawn at the old place plus the real sidestep; there is no separate client-only displaced
  hitbox (hits are decided on the server).

### Deflect dash (`butoh_dash_deflect`)
* Mid page, cost like other mids (12%), cooldown 120 ticks (about 6 s, before the mod's cooldown multipliers).
* Movement: like Ebb Step it finds a free spot along a line and moves there (`teleportTo`), but the path is swept (no passing through walls) and
  ground must exist under the end spot. Direction: the way the server saw the player move last tick (`getKnownMovement`; a strafe dashes sideways),
  else the horizontal look direction. Length 5.5 blocks, 6.5 while dancing.
* For 12 ticks after the dash, `ProjectileImpactEvent` (server) cancels a missile hitting the player, turns it with
  `Projectile.deflect(ProjectileDeflection.REVERSE, player, player, true)` (the player becomes the owner), then re-aims it at the shooter (or straight back)
  with `Projectile.shoot` at 1.0x (1.25x while dancing) of its speed and a random spread of 7 degrees (3 while dancing). A missile is turned at most
  twice; a third time it is discarded (as the twin-sword parry does).
* Works outside the dance. Spell bolts that are not entities (`SpellRuntime.bolt`) are not projectiles and are not covered.

### Dance-cast bonus
`ButohBook.onDamageEntity`: while dancing, damage dealt by the Butoh book's own damage types (water elemental, and the book's generic magic) is
multiplied by 1.15 (`butoh.danceCastBonus`), and never beyond the PvP per-hit cap for player targets. This runs after BalanceLaw, so the mob multiplier is not
applied twice.

### VFX (palette: ghostly white-blue 0xB8E6FF and ink blue-black 0x16233A on top of the existing teal / jade / pale Butoh look)
* `BUTOH_DANCE_RIPPLE`: from = feet (follows the dancer), power = size (1.0 + 0.12 per move index), duration = the move's length + 6. Three wave
  discs spreading on the ground, a chain-link ring on the water line, a low glow, a few ink flecks lifting off. One per move, so the ripples follow the dance.
* `BUTOH_AFTERIMAGE`: from = old place, to = new place, duration 16. A pale ghost figure (legs, torso, head) with an ink-wash shadow, a smear along the
  move, two trailing sleeves, ink flecks. Used for the dodge and for the dash.
* Vertex budget (preview tool): ripple 96 / 400, after-image 84 / 400. Textures are the existing `butoh_*` ones plus the generic glow; nothing is
  allocated per frame beyond what the other layers already do.

## Config (`nusmp-butoh-server.toml`, section `butoh`)
`danceDrain` (0.01, 0..0.5, fraction of max magicule per second), `evadeMax` (0.65, 0..0.95), `danceCastBonus` (0.15), `danceShrink` (true),
`danceSidestep` (true).

## Locks (the lead pastes these into `book/CharacterSpells.java`)
```java
lock("book_butoh", "butoh_dance", CanonBook.KIATO);
lock("book_butoh", "butoh_dash_deflect", CanonBook.KIATO);
```
Everything else in the Butoh book stays open to anyone with the magic. If the owner wants the deflect dash open to everyone, drop its lock line.

## Integration lines for the lead
* `NUSMP.java`, common setup (next to the other `NeoForge.EVENT_BUS` listeners): `com.newuniverse.nusmp.butoh.ButohEvents.register(NeoForge.EVENT_BUS);`
* `NUSMP.java`, mod bus: `modEventBus.addListener(com.newuniverse.nusmp.butoh.DanceStatePayload::register);`
* `NUSMP.java`, client block: `com.newuniverse.nusmp.client.butoh.DanceClient.register(NeoForge.EVENT_BUS);`
* `NUSMP.java`: `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.butoh.ButohConfig.SPEC, "nusmp-butoh-server.toml");`
* `vfx/VfxShape.java`: append after `LUCIUS_CORE_GEM` (and after anything other builders appended before this):
  `BUTOH_DANCE_RIPPLE, BUTOH_AFTERIMAGE;` (ButohLayer does not compile without them).
* `vfx/client/VfxManager.java`: nothing to add, `ButohLayer` is already registered and now claims the two new shapes.
* `en_us.json`: `$S/exc/frag/kt.lang.json` (the two page descriptions).
* Without the registrations the pages still cast but the dance does nothing (no tick, no evasion, no deflect): all four lines are needed.

## Verified / unverified
* Type-checked against the API stubs (all my files; the only errors left are stub gaps: the network packages, `ServerStoppingEvent`, client `Minecraft`; one
  deliberate wrong call was reported as an error). Pure logic is unit tested: `DanceChoreographerTest`, `EvasionTableTest`, `DanceScaleTest`,
  `DeflectMathTest` (22 tests, run with the JUnit shim; a mutation of the no-repeat rule is caught). CI runs the real JUnit.
* VFX rendered with the headless preview (`python tools/vfx_preview/preview.py butoh_dance_ripple butoh_afterimage ...`), within the vertex budget.
  The after-image is a flat camera-facing ghost, tinted slightly teal by the leaf texture; it has not been seen in game.
* NOT run in game: the clips on a real player model, the crossfades, the network sync of the seed, `Attributes.SCALE` on a player (eye height, camera,
  the grow-back check), `Projectile.deflect` + `shoot` on real arrows / fireballs, `getKnownMovement` for the dash direction, `teleportTo` sidestep with
  the Dungeon's rules, and the exact mana numbers. Uncertain APIs: `ServerPlayer.getKnownMovement` (used as the movement direction), `Projectile.shoot` on
  `AbstractHurtingProjectile` (it keeps its own acceleration power), `AttributeInstance.addOrUpdateTransientModifier` with SCALE.
* A pre-existing problem noticed, not touched: in the committed `cast.animation.json` the clips `key_turn`, `dice_fiddle`, `dice_toss` sit INSIDE the
  `cast_weapon_dance` object (a bad hand edit), so `CastAnimClient` / `GeoAnim` never see them as clips and the Key and Dice casts play no body clip.
  Regenerating the whole file with `gen_cast_animations.py` would also drop them (the generator does not have them), so the file is updated with
  `python tools/gen_cast_animations.py --append-dance`, which only adds the eight `dance_*` clips and leaves every other byte of content alone.
