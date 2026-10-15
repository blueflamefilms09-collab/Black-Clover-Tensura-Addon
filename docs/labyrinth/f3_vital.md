# F3: HP buffer and Vital Strike (1.36.0)

Package `labyrinth/dnd/vital/`. Brief: item 7 of the owner's hybrid dungeon brief, "Lethality vs HP Buffers": give the dungeon's mobs D&D-sized hit-point pools, and let a critical blow or a high-tier Relic cut through them in an anime-style execution. Config lives in `DndConfig` (`hpBuffer`, `hpScale`, `vitalStrike`, `vitalStrikeEffects`, `vitalThreshold`, `vitalExecute`, `vitalRelicGrade`, `vitalCooldownTicks`); nothing was added to it.

Nothing here was run in game. The pure maths is tested (13 cases, `VitalMathTest`); the handlers were type-checked against the API dumps.

## Why the two halves exist

D&D fights are attrition: a pool of hit points that the party grinds down while it spends resources (the hunger and gloom clock of F2, the traps of F1). Anime fights are the opposite: one decisive blow that ends it. The Labyrinth wants both. The buffer makes every ordinary hit matter little, so a fight in a sealed trial chamber lasts; the Vital Strike gives the player who earns it (a strong hit, or a strong relic kept warm by a chain of hits) the anime moment. The two are separate classes because they have separate lifetimes: the buffer is a one-time change to a mob when it appears, the strike is a per-hit decision.

## Why the buffer is bypassed rather than removed

If a Vital Strike removed the buffer (set health to zero, or strip the modifier), then "max health" would stop meaning anything the moment a player owns one good relic: every relic holder would one-shot every mob and the buffer would be decoration. Instead a Vital Strike only raises the amount of one hit, to at least `vitalExecute` (35 % default) of the target's max health. A normal hit still has to chew through the buffer; a strike takes a third of it at once, so a mob dies in about three strikes, and the cooldown (5 s default) spaces them. Because the amount is raised and not the health lowered, everything that sits after the event still applies: armour, enchantment protection, resistance effects, absorption, a totem, another mod's damage reduction. A mob with Resistance V is not one-shot by an execution, and a mob with no armour is not made unkillable by one.

## Why `LivingIncomingDamageEvent`

NeoForge 21.1 split the old Forge hurt/damage pair into three events: `LivingIncomingDamageEvent` (the raw amount; `getAmount()`, `getOriginalAmount()`, `setAmount(float)`, `getSource()`, `getEntity()`, cancellable), then the `DamageContainer` reduction pipeline (armour, enchantments, effects, absorption), then `LivingDamageEvent.Post`. The signatures were read from the API dump (`net_neoforged_neoforge.txt`) of the exact 21.1 jar. Incoming is the only point where the amount is both still unreduced and writable; `LivingHurtEvent` does not exist in 21.1 and `LivingDamageEvent.Post` runs after the health has changed. The handler never cancels and only ever raises the amount, so it composes with the mod's other Incoming handlers (SpiritBond, EyeArts, Kotodama) regardless of order.

The attacker is `source.getEntity()`, the root cause, not `getDirectEntity()`: an arrow, a trident or a credited spell counts for its owner, so archers and mages get Vital Strikes too. Only a `ServerPlayer` can trigger one.

## Vital Strike rules (class `VitalStrike`, numbers in `VitalMath`)

A hit becomes a Vital Strike when the target is eligible and the player is off cooldown and either:

* it is heavy: `amount >= vitalThreshold x target max health` (18 %; the max health already includes the buffer, so a threshold hit on a buffered mob really is a heavy blow); or
* the player holds, wears or has in a Curios slot a relic of grade `>= vitalRelicGrade` (default 2 = RARE; `Relics.wornList`, the same cached scan the relic engine uses, hands + head/chest/feet + Curios) and this hit fills the Vital meter.

The meter gains 25 % per hit with such a relic and decays 5 % per second. It is not ticked: it is stored as (value, game tick written) and the decay is applied on read (`VitalMath.meterAt`), so idle players cost nothing. A hit that completes the meter is itself the strike (so four quick hits, or about five hits a second apart); the meter then resets to 0. A hit slower than one per five seconds can never fill it (the decay equals the gain), which is the intended pacing. During the cooldown neither path fires and the meter does not charge, so strikes cannot be banked. A heavy hit is its own trigger and leaves the meter as it was.

On a strike: the amount is replaced by `max(amount, vitalExecute x max health)` (clamped, NaN and infinity safe), the actionbar says "Vital Strike!" (lang key `message.nusmp.vital_strike`), and with `vitalStrikeEffects` the thunder sound `TRIDENT_THUNDER` plays low (pitch 0.6) and a `RukhFx.burst` of light Rukh (12, the API's own cap) rises at the target's centre. The Rukh goes out through `ServerLevel.sendParticles`, so every nearby client sees it with no packet of our own; no vanilla particle type is used.

State is a `ConcurrentHashMap<UUID, State>` (meter, tick, last strike) as the brief asks, not a data attachment: a meter that decays in 20 s is not worth saving, and the map is cleared on logout (`PlayerLoggedOutEvent`) and on `ServerStoppingEvent` so it cannot leak across a singleplayer world change. The hand-over is only created once a player actually lands a relic or heavy hit on an eligible target.

## `HpBuffer` (the buffer itself)

A permanent MAX_HEALTH modifier, operation `ADD_MULTIPLIED_TOTAL`, value `hpScale - 1` (2.5 default, so x2.5), id `nusmp:labyrinth_hp_buffer`, applied through `addOrReplacePermanentModifier`. A modifier, not `setBaseValue`: it is saved with the entity, shows in `/attribute`, stacks with other mods' modifiers instead of overwriting the mob's own base, and can be found by id. The hook is `EntityJoinLevelEvent` on the server side in `Labyrinth.in(level)`, not `FinalizeSpawnEvent`: Sealed Archway spawns, spawn eggs, commands, wildcard-pool mods and chunk reloads all end in a join, only natural spawns go through finalize. The entity tag `nusmp_hpbuf` marks "looked at once" so a mob is never buffered twice (the modifier is the truth: `isBuffered` checks it). A new mob starts at full health; an old mob (loaded from disk without the tag, e.g. spawned before the update) keeps its wounds in proportion. Only `Enemy` mobs are buffered (villagers, pets, trial allies are not).

## Interaction with bosses, the budget and `BudgetedBoss`

A boss is excluded from both the buffer and the strike (`HpBuffer.isBoss`), because bosses already have their own balance and each system would break the other:

* `BudgetedBoss` (Zogratis, Zagred and any later boss that implements it) caps the damage the boss takes per second. A Vital Strike is a large single hit, exactly what that cap exists to stop; if it reached a budgeted boss it would either be clipped (so pointless) or, worse, bypass the budget. Multiplying such a boss's health on top of the cap would count the same balance twice. So `instanceof BudgetedBoss` is the first test.
* `nusmp:floor_warden` has its own 200 HP (config `wardenHealth`) and a boss bar; excluded by entity type.
* Anything in the `c:bosses` entity tag (Wither, Ender Dragon, other mods' bosses) and anything carrying the entity tag `nusmp_peak_boss` (the Peak Altar's roster) is excluded.
* Anything whose BASE max health is already >= 300 (the same limit as `BalanceLaw.isBoss`) is excluded. The check reads the base value, never the modified one: after the buffer a 150 HP mob would read 375 and every system that asks `getMaxHealth() >= 300` (BalanceLaw, GodTier) would mistake it for a boss. That is a known side effect for mobs of base health 120 and up; it is why `hpScale` is a config and why the base check is applied before the modifier.

The Vital Strike additionally requires `HpBuffer.isBuffered(target)`: a mob with no buffer (outside the Labyrinth, or the buffer switched off) has nothing to bypass, so ordinary combat elsewhere in the mod is untouched. `VitalMath.eligible(boss, buffered)` encodes this and is tested.

## Files

All Java in `src/main/java/com/newuniverse/nusmp/labyrinth/dnd/vital/`:

* `VitalMath.java`: pure arithmetic (threshold, meter, lazy decay, execution, cooldown, the whole `resolve` decision). No Minecraft classes, so CI can unit-test it. Every hostile float (NaN, infinity, zero or negative max health) answers "no strike" or the unchanged amount; the execution is clamped so it cannot overflow a float.
* `HpBuffer.java`: the buffer and the boss test.
* `VitalStrike.java`: the Incoming handler, the per-player map, the spectacle, and `register(IEventBus gameBus)` that adds the handlers (including `HpBuffer::onJoin`).
* `src/test/java/com/newuniverse/nusmp/labyrinth/dnd/vital/VitalMathTest.java`: 13 cases (threshold edge, execution floor, a stronger hit keeping its damage, bosses and unbuffered mobs, the fourth relic hit, decay and clock-backwards, slow hits never filling the meter, cooldown blocking both paths, overflow/NaN, grade gate).
* lang fragment (merged into en_us.json by the lead): one key, `message.nusmp.vital_strike` ("Vital Strike!"). No JSON, NBT or PNG was created.
