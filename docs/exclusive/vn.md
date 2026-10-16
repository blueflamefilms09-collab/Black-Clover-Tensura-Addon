# Vanessa Enoteca: Thread Magic and Rouge (1.43.0, builder VN)

Nothing here was run in game. Type-checked against API stubs (stub gaps only: ServerLevel.getEntity(UUID), Level.getWorldBorder, MutableBlockPos, BlockState.getCollisionShape, Minecraft are missing from the stubs but exist in the real API dumps), 28 JUnit tests pass on the pure classes (mutation-checked), VFX layer rendered headless with tools/vfx_preview.

## New vs replacement vs kept
- REPLACEMENT with switch: the `rouge` page in entity mode (`thread.rougeEntity=true`) calls a real cat entity. With the switch false the 1.34-0.49 behaviour (head cat, 12% dodge, one death undo) runs untouched. Kept: RougeCat, RougeCatLayer, RougeCatModel, ROUGE_CAT VFX. While the switch is on, an old "RougeArmed" flag does nothing.
- NEW pages appended last in ThreadBook: `arresting_dark_weave`, `dancing_doll_recall`, `thread_trap`, `reinforcement`.
- NEW: Rouge entity + AI, fate events, protected set, thread trap entity, Bound effect, 4 VFX shapes, config.
- KEPT unchanged: red_thread, arachnes_web, dancing_doll, thread_mending.
- Touched outside my area (additive): `multiverse/Squads.partyMembers(ServerPlayer)` (public, returns FTB party member ids).

## Classes (why)
- FateRules (pure): trigger rule (lethal after armour estimate, or >= bigHitFraction), repeat skip, boss cooldown, price.
- ProtectedSet (pure): cap/toggle/save text, who is protected (enemies never).
- ThreatMath (pure): ranks allies, chooses sprint/hop. ThreadMath (pure): trap geometry, pull velocity, drain.
- RougeEntity: PathfinderMob, invulnerable except /kill and void, not saved, unpickable, unpushable, untargetable, no drops. Goals: WatchAllies (scan 14 blocks every 4 ticks), RunToEndangered (sprint+leap, hop through thread beyond 8 blocks), FollowCaster (hop back beyond 16, settle on shoulder). Tap 6 ticks, reknit 12, unravel 16.
- RougeManager: one cat per caster, summon replaces, mana read/pay (magicule via TensuraStorages), ally list in the thread grimoire skill tag key `RougeAllies`.
- RougeFateEvents: LivingIncomingDamageEvent (HIGH), ProjectileImpactEvent (arrows; other projectiles are judged when their damage arrives), LivingDeathEvent safety net (cancel, health 1).
- ThreadTrapEntity, ThreadEffects (Bound: speed -100%, jump -100%, knockback immune), ThreadSpells (the page bodies and Mana Skin), SafeSpot, ThreadConfig, ThreadEntities.
- Client: RougeModel/RougeRenderer (reuses RougeCatModel.LAYER and rouge_cat.png, glow pass), ThreadTrapRenderer (faint lines, brighter when near), ThreadClient, ThreadFateLayer (VFX).

## Behaviour decisions
- Dimension change, logout, caster death: Rouge despawns (unravels). She does not follow.
- Mana cannot be paid in full: no mana is taken, Rouge fails and unravels.
- Boss blows: allowed, double cost, per-victim cooldown `rougeBossCooldownSeconds` (8). Victims in stopped time or with persistent time `nusmp_unfated_until` > now cannot be rewritten (hook for other features).
- Sneak + cast `rouge` on a player or anyone's pet toggles them on the list (no cost, no cooldown). Protected also: caster, own pets (OwnableEntity), FTB party, vanilla team mates.
- Reinforcement drain is percent of max mana per second (`reinforceDrain` 1.5), not absolute. Cancels IN_FIRE, ON_FIRE, LAVA, HOT_FLOOR for players while on.
- Dancing Doll: Recall refuses bosses; enemy pull uses BalanceLaw.beginControl. Arresting uses beginControl + controlTicks.
- Trap: sneak = ring round caster, else line at aim. Nodes catch arrows, turn fireballs back, slow+weaken+glow foes (clamped).

## Config (nusmp-thread-server.toml, section thread)
rougeEntity, rougeMinutes, rougeMaxAllies, rougeBigHitFraction, rougeManaFlat, rougeManaPercent, rougeBossCooldownSeconds, trapMax, trapSeconds, reinforceDrain, arrestSeconds.

## Unverified
Whole game side (goals, pathing, leaps, shoulder glue, event cancel/deflect behaviour, ProjectileImpactEvent cancel on arrows, teleport of players), renderer pose scale, mana numbers, whether `returning false` from a page refunds mana (assumed as the other pages do).
