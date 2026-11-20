# Legendary Bow: real arrows with physical and magic damage

Owner request: "the arrows should be like magic and physical damage, as they should use like summon technically regular Minecraft arrows just reskinned, but then also have
magic effects depending on the bow type, the bow form you have equipped." Follow-up: "Arrow Rain and every rain/volley/shower-type bow ability must also summon actual arrow
entities (same real-arrow path, with that form's magic effect), falling from above, bounded, capped; `heroweapon.bow.realArrows=false` keeps the old rain."

## What it is, in one paragraph

When the wielder lets go of the string, the shot is now a real arrow ENTITY (`HeroArrow`, a small mod type that extends vanilla `AbstractArrow`) with normal Minecraft arrow
physics. It hurts twice: a PHYSICAL hit (an arrow hit of its owner; armour and shields matter) and a MAGIC hit (indirect magic of its owner, a share of the physical one;
magic resistance and the Resistance effect matter). It carries a magic effect chosen by the bow form in the hand (or by the armed technique), and a glow trail in the form's
colour. Arrow Rain, Frozen Rain, Starfall Volley, Autocracy Justice and Arrow Squall summon real arrows too. Everything is behind the SERVER setting
`heroweapon.bow.realArrows` (default on); off restores the hitscan lines and the instant rain waves exactly as they were.

## Decision: a mod entity extending `AbstractArrow`, not a marked vanilla arrow

| | mod entity `HeroArrow extends AbstractArrow` (chosen) | vanilla `minecraft:arrow` with persistent data |
|---|---|---|
| Physics, sounds, sticking, shield bounce, block collision | all inherited from `AbstractArrow` | same |
| "Is an arrow" for vanilla / Tensura / other mods | yes: listed in the `minecraft:arrows` entity-type tag (`data/minecraft/tags/entity_type/arrows.json`) and is an `AbstractArrow` | yes |
| Own look (per-form texture, trail) | one registered type, one renderer | impossible without replacing the vanilla arrow renderer for every arrow |
| Saved with the world / picked up | `noSave()`, pickup DISALLOWED, `tryPickup` refused, drops nothing | saved by default, pick-up-able by default, other arrow mods change it |
| Other mods' arrow changes (quivers, enchant mods) hitting our damage | they see an arrow but our `onHitEntity` owns the damage | they would rewrite our damage |
| Cost | one entity type + one renderer + one registry line | none, but all the downsides above |

Cost of the choice: one new registered entity type (`anime_multiverse:hero_arrow`). It is sized and tracked like the vanilla arrow (0.5 x 0.5, tracked by players within
64 blocks, position sync every 20 ticks), so only nearby players ever hear about it.

## NEW / REPLACED / KEPT (rules 2 and 5)

**NEW**
- `HeroArrow` (entity), `HeroArrowEntities` (register), `BowArrows` (spawning, release, rain wave, squall), `ArrowImpact` (the magic effects), `ArrowKind`, `BowArrowForms`
  (the form table), `ArrowSplit` (damage split rules), `ArrowBudget` (caps), `BowArrowsSelfTest` (`hero.bow.arrows`), in `heroweapon/weapon/bow/`.
- Client: `client/heroweapon/bow/HeroArrowRenderer` (vanilla arrow renderer plus the glow trail) and `BowArrowClient` (registration).
- 13 arrow textures + `glow.png` (`textures/entity/bow_arrow/`, painted by `tools/gen_bow_arrow_textures.py`; preview sheet `docs/heroweapon/bow_arrows_previews/arrow_textures.png`).
- Entity tag `data/minecraft/tags/entity_type/arrows.json`; lang key `entity.anime_multiverse.hero_arrow` (`tools/lang/bow_arrows.json`, merged with `tools/lang/merge_fragment.py`).
- Server settings `[heroweapon.bow]`: `realArrows`, `maxArrowsPerPlayer`, `maxArrowsGlobal`, `magicShareScale` (file `nusmp-heroweapon-server.toml`).
- Tests: `ArrowSplitTest`, `BowArrowFormsTest`, `BowArrowsSourceScanTest`; self-test `hero.bow.arrows`.

**REPLACED (behaviour only, while `realArrows` is on; the old code is still in the files and runs when it is off)**
- The release of the drawn string (plain arrow and every armed arrow: sniper, net, mark, burst, seeker, second, Pride's command) is a real arrow instead of a hitscan line / bolt.
- The waves of Arrow Rain, Frozen Rain, Starfall Volley and Autocracy Justice are real arrows falling from above instead of instant area hits plus drawn arrows.
- The Arrow Squall fan is seven real arrows instead of seven instant lines.

**KEPT**
- Every hitscan method (`BowShots.plain/sniper/net/mark/burst/seek/second/command`, the old rain wave and fan bodies), the numbers (`BowNumbers`, `DrawMath`), the laser sight of the
  sniper arrow, the rings on the ground, the muzzle and rain effects of the effect system, the armed-mode storage, the forms file, the techniques Bird Hunting, Backstep Shot,
  Law Fanatic, Bull of Phalaris, the Meteor Bow technique, and all bow models and in-hand placement (untouched).

## How a shot works

1. `BowShots.release` (unchanged order): draw fraction, shot lock, armed mode, cost. New: with the setting on and the wielder at the arrow cap, "Too many arrows in the air." and nothing is
   paid; otherwise `BowArrows.release` fires the arrow(s) and returns.
2. `BowArrows.fire` takes a place in `ArrowBudget` (per wielder UUID and global) BEFORE making the entity; over a cap nothing spawns.
3. Flight: vanilla arrow physics (gravity 0.05, drag, water inertia). Speed `1.2 + 1.8 * draw` blocks per tick (a full draw is the vanilla 3.0; Farsight x1.15), small spread that
   shrinks with the draw. Exceptions, each stated in the table below: the sniper arrow and the seeker have no gravity.
4. Hit on a foe (`HeroArrow.strike`, server only): only creatures the bow's targeting rules allow (`Foes.valid`: monsters, mobs fighting the wielder, bosses, harmable players; never
   creative or spectating players, allies, pets, villagers, armour stands, the wielder) can be hit at all; others are flown through.
   - PHYSICAL: `BalanceLaw.damage(raw x level power)` as an arrow hit of the owner (`damageSources().arrow(arrow, owner)`). `raw` is exactly the number the hitscan arrow used
     (`DrawMath.raw` for the plain arrow, `BowNumbers.*_RAW` for the techniques).
   - A blocked physical hit (raised shield) bounces the arrow off (it lives at most one more second) and no magic follows.
   - MAGIC: `BalanceLaw.damage(raw x share x magicShareScale x level power)` as `indirectMagic(arrow, owner)`. The hurt immunity left by the first blow is cleared for this one
     second blow of the same arrow only (`invulnerableTime = 0`), so the two parts both count; the next arrow meets the normal immunity again.
   - Then the effect of the form or technique (`ArrowImpact.onHit`), the sound, a point of weapon mastery and experience (as before), and the arrow ends (or flies on if it pierces).
5. Landing (block or end of flight) runs `ArrowImpact.impact` once (split, fire / star burst, burst fuse). A volley arrow never lingers stuck and never bursts.
6. The arrow ends by itself: 6 seconds in flight at most (the sniper arrow 1.25 s, the seeker 3 s, rain arrows 2 s), 2 seconds after sticking, at once when its owner dies, logs out or
   changes world (checked every 5 ticks). It gives its place in the budget back on every removal (hit, expiry, unload, discard).

### Hit numbers

Per arrow, per foe, raw damage (before the balance law, which also applies the boss caps and the hit immunity of the target):

| Source | Physical raw | Magic raw |
|---|---|---|
| Plain arrow (draw `f`) | `DrawMath.raw(f, ...)` (0 to 12) | physical x form share |
| Eagle Piercing Shot | 14, every foe up to 6 | 14 x form share |
| Bind / Mark / Seeker / Command | 5 / 3 / 8 / 4 | x form share |
| Burst Arrow | 0 (the burst is the damage: 10 raw to every foe within 3.5 blocks after 0.5 s, as a magic hit) | 0 |
| Rain arrow aimed at a foe | the wave's damage (2 / 2 / 3.25 / 7) | x form share |
| Rain arrow scattered | half the wave's damage | x form share |
| Squall arrow | 1.7 each | x form share |
| Shower child arrow | 40 % of the parent | x form share |

The worst a single arrow can do to one foe is `physical x (1 + share x scale)`, at most `12 x 1.8 = 21.6` raw at the largest share and scale (`ArrowSplit.worst`, tested).

## Forms to effects (the table is `BowArrowForms`; a test fails if a form of `legendary_bow.json` has no row or no texture)

| Form | Arrow effect | Magic share | Colour |
|---|---|---|---|
| base | LIGHT: a holy flash, nothing more | 30 % | white-gold |
| hunters_bow | BLEED: poison 3 s + a hunter's mark (fragility 5 s) | 30 % | green |
| scouts_bow | MARK: glows through walls, left frail 5 s | 25 % | teal |
| bind_arrow | BIND: root through the shared control lock and the control law (a boss is only staggered) | 30 % | white |
| bird_hunting | PIERCE: pierces one more foe (70 % damage), Farsight speed | 30 % | sky blue |
| arrow_shower | SPLIT: the first impact splits the arrow into 3 child arrows (40 % each) at foes within 8 blocks, else spread round; children never split again | 25 % | amber |
| meteor_bow | FIRE: burst of fire where it lands (radius 3, up to 6 foes, magic hit of 60 % of the arrow's damage, set alight 2 s); never changes a block | 40 % | orange |
| frozen_rain | FROST: slowness II for 2.5 s and an ice effect | 35 % | ice blue |
| eagle_piercer | PIERCE (as bird_hunting) plus Eagle Eye's bonus damage past 24 blocks | 30 % | gold |
| sagittarius | STAR: a zodiac star burst where it lands (radius 2.5, up to 6 foes, magic hit of 60 %) | 40 % | violet-white |
| bow_of_pride_1/2/3 | PRIDE: pride curse (Tensura `curse` 8 s) and Weakness 4 s | 35 % | Pride violet |

Piercing Arrows (the passive) adds one more pierced foe to any plain arrow, as before. The meteor_bow and sagittarius bursts are NOT done by arrows of a rain (a volley arrow
never bursts: it keeps the rain bounded) but by the plain arrows of the form. Pride arrows keep their +20 % damage but not the wider hitscan lane (a real arrow has its own hit box).

### Form-independent special arrows (the armed modes)

| Armed mode | Real arrow |
|---|---|
| Eagle Piercing Shot (sniper) | no gravity, speed 4, pierces up to 6 foes at full damage, 1.25 s life |
| Bind Arrow (net) | BIND on the first foe |
| Marking Arrow | MARK on the first foe |
| Burst Arrow | no direct damage; ring on the ground, bursts 0.5 s after it lands (old numbers, magic hit) |
| Seeker Arrow | no gravity, speed 1.5, steers towards the foe nearest the crosshair chosen when it left the string (no per-tick search), 3 s |
| Second Arrow | the form's own arrow twice (second after 10 ticks at 60 %, never above the ordinary budget together) |
| Pride's Command | turns the first monster hit against its own side (never a boss or a player; shared control lock) |

An armed arrow keeps the form's look and its magic share (the magic hit); the technique's own effect replaces the form's arrow effect.

### Rain, volleys and Squall as real arrows

- A wave of Arrow Rain / Frozen Rain / Starfall Volley / Autocracy Justice makes up to 6 / 8 / 10 / 10 arrows. The first ones are aimed at the foes inside the circle (nearest first),
  each starting 12 blocks above its target (lowered under a roof) and falling at 2 blocks per tick; the rest scatter over the circle by the existing golden-angle pattern (half damage).
  The effect is the form's, except Frozen Rain (FROST), Starfall Volley (STAR) and Autocracy Justice (PRIDE). Volley arrows end on impact and live 2 s at most.
- An aimed arrow that cannot be spawned (a cap) falls back to the old direct wave hit on its foe, so a capped rain still rains.
- Arrow Squall: seven real arrows along the fan after the draw delay, 1.7 raw each.
- Tradeoff: the old rain hit every foe in the circle for certain; real arrows can miss a foe that moves out from under them (the ring on the ground is still the warning).

## Settings (server config `nusmp-heroweapon-server.toml`, section `[heroweapon.bow]`)

| Key | Default | Meaning |
|---|---|---|
| `realArrows` | `true` | `false` restores the hitscan release, the instant rain waves and the instant squall exactly |
| `maxArrowsPerPlayer` | 24 (1 to 128) | hero arrows one wielder may have out at once |
| `maxArrowsGlobal` | 256 (1 to 2048) | hero arrows on the whole server |
| `magicShareScale` | 1.0 (0 to 2) | factor on the magic hit; 0 = physical only |

These change gameplay, so they are SERVER settings; nothing here is a client setting (the trail has no switch: it is 8 vertices and culled by distance).

## Worst-case cost (rule 11)

| Resource | Bound |
|---|---|
| Arrow entities | 24 per wielder (setting), 256 server-wide, every one ends by itself in at most 6 s (2 s stuck); an over-cap shot does not leave the string (a rain's aimed arrow falls back to one direct hit) |
| Spawns per second per wielder | plain arrows at most 5 a second (the 4-tick shot lock); the biggest rain (Starfall) 10 arrows every 0.5 s = 20 a second for 4 s; Squall 7 once; Shower 3 children per arrow, children never split |
| Packets | only the vanilla entity packets, to players within 64 blocks: 1 add + 1 remove per arrow, a position sync every second of flight, and for a seeker a motion packet every other tick for at most 3 s (30). Worst realistic: 20 arrows a second x 2 packets = about 40 packets a second per nearby player for 4 s, then none |
| Effect packets | at most one effect-system packet per arrow hit (flare, ice, star, web, compass), plus one burst effect per landing of a fire / star / burst arrow, all through the mod's existing effect budgets; none for flight |
| Particles per second | 0 sent by the server; the client draws 8 extra vertices per moving arrow within 48 blocks, nothing for a stuck or far arrow |
| Server CPU per arrow per tick | the vanilla arrow tick plus a counter, a 5-tick owner check, and for the seeker one steer every other tick; NO world scan, NO chunk load, NO entity query in `tick` (pinned by `BowArrowsSourceScanTest`) |
| Server CPU per hit | two `hurt` calls, one effect; per landing: one entity query for a Shower split, a fire / star burst or a burst arrow (up to 6 foes); none for a volley arrow |
| Per rain wave | one foe query and up to 10 short block rays (to find the roof) |
| Memory | one small object per live arrow; counts in `ArrowBudget` (a map of at most one entry per wielder), cleared when the server stops |
| Cleanup | owner death, logout and world change: the arrow checks every 5 ticks and discards itself; unloaded chunks and discards give the place back (`remove` and `onRemovedFromLevel`); the arrow is never saved |

Multiplayer and servers (rule 9): all rules run on the server; the client class is `client/heroweapon/bow/HeroArrowRenderer` (registered only inside the client block of `NUSMP`);
state is keyed by the owner's UUID; no packet of ours is new (the arrow uses the vanilla entity spawn), so there is no new payload to validate.
`ServerSafetyTest` passes without a new allow-list entry. No hunger is read or written (`NoHungerDrainTest`, plus a scan of the bow package).

## Reskin

The arrow is drawn by the vanilla arrow renderer (same model and draw) with a per-form 32 x 32 texture in the vanilla arrow layout (fletching in the form's colour, a pale point,
wood shaft), and a soft ribbon behind the moving arrow drawn through `SafeAdditive.additive` in the form's colour. Textures are painted by `tools/gen_bow_arrow_textures.py`:
`base, hunters_bow, scouts_bow, bind_arrow, bird_hunting, arrow_shower, meteor_bow, frozen_rain, eagle_piercer, sagittarius, bow_of_pride_1, bow_of_pride_2, bow_of_pride_3, glow`.

## Tests

- `ArrowSplitTest` (pure): the damage split, the share scale, negative inputs, the worst case below twice the physical hit, the Shower's children, the caps (per wielder, global, release
  order, clear).
- `BowArrowFormsTest` (reads the real forms file and the textures): every form of `legendary_bow.json` has a row, an effect that is not technique-only, a share in range and an existing 32 x 32
  texture; Pride forms carry PRIDE and nothing else does; the row order and kind order are pinned; the armed modes pick their kinds. It fails when a new form lacks an effect or a texture.
- `BowArrowsSourceScanTest` (source scan): extends the vanilla arrow, never saved or picked up, gives its place back on every removal, ends by itself, owner check on a timer, `tick` and
  `steer` have no scan or chunk load, budget before `create`, every hitscan path still behind the setting, the single Meteor seam, the settings and their defaults, the tag, the lang key, the
  self-test registration, no client class and no hunger in the bow package, renderer culling and `SafeAdditive`, the painter script names every form.
- Self-test **`hero.bow.arrows`** (registered in `BowLogic`, runs in the server smoke test): form rows exist for every bow form, the entity is in `minecraft:arrows`; a stand-in wielder fires an
  arrow at a husk with 200 health and the arrow's strike must land BOTH a physical and a magic hit and hurt the husk, the arrow must not be pick-up-able, end after a finished hit and give its
  place back; then `cap + 3` arrows are asked for and exactly `cap` may exist, and every place returns when they are removed. With `realArrows` off it checks that nothing spawns.
  **For the coordinator: add `hero.bow.arrows` to the smoke list in `smoke.yml`.**
- Also run and passing here: `ServerSafetyTest`, `NoHungerDrainTest`, `SafeAdditiveSourceScanTest`, `SafeAdditiveTextureScanTest`, `PerfSourceScanTest`, `ConfigFileNamesTest`, `HeroAssetsTest`, the
  other bow tests and the 44 test classes that read `en_us.json` or entity tags (3 failures in `ShishouReworkTest` come from the way the scratch runner loads resources, not from this change).

## The Meteor Bow seam

`BowSkills.meteor(HeroCtx)` is the one place the Meteor Bow TECHNIQUE is wired (today it calls `BowRain.meteor`: the old drawn arrow and the fire burst at the impact point). The shared
falling-meteor entity of the other change is swapped in there and nowhere else. The Meteor Bow FORM's arrows (a fire burst where each lands) are separate and stay.

## Caveats (honest list)

- **Not compiled by Gradle and not seen in a game.** The new and changed main sources were compiled with `javac` against the signature stubs of `.github/api` (`tools/typecheck/stubgen.py`
  builds them): no error in any file of this change except the stub artefacts for `abstract` stub classes, which are not real. The pure classes were compiled and their tests run with the JUnit
  launcher. The entity, the renderer, the self-test and the effect code were NOT run. Not seen: the feel of the flight constants (speed `1.2 + 1.8 x draw`, Farsight x1.15, sniper 4.0),
  the look of the textures on the real arrow model (only the sheet preview was looked at) and of the glow ribbon, the shield bounce (it relies on `LivingEntity.hurt` returning false for a blocked
  arrow, as the vanilla arrow does), the Tensura magic-resistance handling of `indirectMagic`, and whether `Entity.remove` / `onRemovedFromLevel` fire exactly as read from the API dump.
- **Balance.** The physical hit is the old number and the magic hit is ADDED, so a plain arrow now deals up to 25 to 40 % more to a foe with no armour or magic resistance than the hitscan
  arrow did (the owner's wording asked for a second hit; `magicShareScale` brings it down, 0 = the old totals). Both hits go through the hero buffs and the balance law, as every hit of the weapon.
- The magic hit clears the target's hurt immunity once per arrow (needed so both hits count); a rain can therefore hurt a foe once per arrow it takes, bounded by the caps and by the fact
  that only foes under the rain's own aimed arrows (plus the scatter) can be hit.
- Rain arrows can miss a moving foe, unlike the old instant wave.
- No block is ever changed, not even with mob griefing on (the brief allowed it; this release chose not to).
- Pride arrows lose the wider hitscan lane; Better Combat and other mods see the arrow as an arrow but nothing here was tested with them.
- Bird Hunting and Backstep Shot still use their hitscan shafts (not asked for: they are not rain or volley techniques); a follow-up can make them arrows the same way.
- `PerfSourceScanTest` was not extended (shared file): the new scan lives in `BowArrowsSourceScanTest`.

## Decisions for the owner

1. Should the magic hit be on top of the old damage (as built, up to +40 %) or carved out of it (physical = 70 %, magic = 30 %, same total)? One setting line: `magicShareScale`.
2. Should rain arrows be allowed to miss (as built), or should the aimed arrows home in on their foe like the Seeker?
3. Meteor / fire bursts: stay block-safe (as built) or set fire to blocks where mob griefing is allowed?
4. Bird Hunting and Backstep Shot as real arrows too?
