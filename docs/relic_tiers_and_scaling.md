# Relic tiers and aura scaling

End-game scaling for the Relics of Many Worlds (`relics/**`). Two systems live here: **tier ascension** (a relic climbs the seven Tensura grades
without a new item id) and the **aura** (a relic weapon hits harder in the hands of an awakened, large-pool player). Socketing and visual evolution
are written up in the second half of this file by the part that builds them.

Every component below has a **Why** paragraph (architecture, NeoForge, balance, performance), because the point of the design is that every
choice can be defended. Nothing here has been run in game; the pure rules are unit-tested (`TierRulesTest`, `AuraScalingTest`).

A note on the word "NBT": the original brief said NBT. NeoForge 1.21.1 does not keep item data in NBT any more: an `ItemStack` carries **data
components**. Everything the brief wants from NBT (a saved integer on the stack, synced to the client, copied with the stack) is a data component
here.

## 1. Tier ascension

### Files

| File | Job |
|---|---|
| `relics/tier/TierRules.java` | Pure numbers: the stat curves, the caps, the catalyst table, the success chances, the costs, the cooldowns. |
| `relics/tier/TierComponents.java` | The data component `nusmp:tier`. |
| `relics/tier/Tiers.java` | Read/write helpers: `tierOf`, `baseGrade`, `effectiveGrade`, `steps`, `multiplier`, `setTier`. The contract other systems use. |
| `relics/tier/TierEvents.java` | The `ItemAttributeModifierEvent` handler. |
| `relics/tier/ActPower.java` | The act-power multiplier of the relic act that is running. |
| `relics/tier/TierRitual.java` | The upgrade ritual (sneak + use a catalyst with the relic in the other hand). |
| `relics/tier/TierItems.java` | The two own catalyst items: Devil's Core, Genesis Seed. |
| `relics/tier/TierCommands.java` | `/multiverse relic tier <player> up` and `set <grade>` (permission 2). |
| `relics/tier/TierSystem.java` | The two registration entry points. |
| `tools/gen_tier_assets.py` | The two 16x16 icons and item models. |

### The component `nusmp:tier` (TierComponents)

An integer, the **absolute grade ordinal** the stack has been raised to (0 Normal, 1 Special, 2 Rare, 3 Unique, 4 Legend, 5 God, 6 Genesis).
Missing or 0 means "never raised". The effective grade is `max(def.grade(), tier)`. It is persistent (saved with the stack), network-synchronised
(the tooltip and renderers read it on the client) and copied by `ItemStack.copy()` like every component.

**Why a component, not an item per tier.** The relics are unique in the world: the Order's ledger tracks one serial per relic id. A new item id per
tier would be a new item for the ledger, the catalog, the loot tables, the Curios tags and every wave's code to know about. A component on the same
item keeps the item id, the serial and the attunement exactly as they are, so a tier change cannot break uniqueness. **Why absolute, not an offset
above the base grade.** If a def's base grade is ever changed by a later balance pass, an absolute tier still means the same thing (the item is at
least that grade), where an offset would silently move. `max(base, tier)` also makes the component harmless on a relic whose base is already higher.
**Why `persistent` + `networkSynchronized`.** Without the codec for saving the tier would vanish on relog; without the stream codec the client
tooltip and the worn-relic renderer would not know the tier. **Why a plain int.** The cheapest component to read, which matters because the read is
on the hot path of every attribute query (see the event below).

### Tiers (Tiers.java)

`effectiveGrade(stack)` is the one question every other system asks (tooltips, name colour, EP lift, visual evolution, socketing). `setTier` writes onto
the stack object in place and removes the component when the tier is not above the base, so a stack never carries a meaningless value.

**Why one helper class.** The brief asks for the grade to be read in many places. If each place read the component itself, the rule "max of base and
tier, clamped to 0..6" would be copied and could drift; one class keeps it in one place. **Why the def is passed where it is known.** `Relics.defOf`
is a map lookup; callers that already hold the def (the item classes) skip it.

### The curves (TierRules)

All multipliers are functions of the number of **steps** the effective grade stands above the base grade, not of the absolute grade. Step 0 is
exactly 1.0 for every stat, so an unraised relic behaves as it did before.

| Steps | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|---|
| Damage (weapon) | 1.00 | 1.08 | 1.17 | 1.27 | 1.38 | 1.50 | 1.65 |
| Attack speed | 1.00 | 1.02 | 1.04 | 1.07 | 1.10 | 1.14 | 1.18 |
| Armour (per piece) | 1.00 | 1.05 | 1.10 | 1.16 | 1.23 | 1.31 | 1.40 |
| Toughness (per piece) | 1.00 | 1.10 | 1.20 | 1.32 | 1.46 | 1.62 | 1.80 |
| Act power | 1.00 | 1.04 | 1.08 | 1.13 | 1.18 | 1.24 | 1.30 |
| Act cost (mana, existence) | 1.00 | 0.97 | 0.94 | 0.90 | 0.86 | 0.82 | 0.78 |
| Act cooldown | 1.00 | 0.98 | 0.96 | 0.93 | 0.90 | 0.87 | 0.85 |

Hard caps (an item already above a cap is left as it is, never lowered): total weapon attack damage 40 (the player's own 1 included); attack speed
never above the player's base 4.0 attacks per second; 10 armour and 5 toughness per piece; cost factor never under 0.75; cooldown factor never
under 0.80.

**Why steps, not absolute grade.** A Normal blade raised to Genesis has travelled six grades and gets the whole boost; a relic that is already
Genesis cannot be raised and gets nothing. An absolute curve would either under-reward the long climb or double-reward the relics that were
strong already. **Why these numbers.** The curve is gentle at the bottom (the first jumps are cheap and safe, so they must not be big) and
steeper at the top where the cost and the risk are. The damage ceiling x1.65 and the 40-point cap come from the end-game bosses: the Dark Triad and
the true demon lords have large health pools and the Zogratis bosses gate their phases by health fractions (`PowerCurve.clampDamage` stops a single
hit from skipping a phase), so extra weapon damage shortens the fight but cannot skip a step. Armour and toughness are capped per piece well under
vanilla's 20/8 total so four raised pieces stay under the vanilla armour ceiling, and the Balance Law's caps against players (`BalanceLaw.cap`,
the per-hit percentage of max health) are not touched by any of this. **Why a floor on the reductions.** A cost or cooldown that tends to zero lets
an act be spammed, which no damage cap can balance. **Why the attack speed cap.** A modifier above 0 would make the weapon quicker than an
unarmed hand, which breaks the attack-cooldown rhythm every other weapon is built on. **Why pure.** No Minecraft types: the whole table, the caps and
the ritual odds are unit-tested without starting the game.

### The attribute event (TierEvents)

`ItemAttributeModifierEvent` exists in NeoForge 21.1 (`net.neoforged.neoforge.event.ItemAttributeModifierEvent`; confirmed in the API dump). The handler
reads the component first; if it is missing or 0 it returns at once. Otherwise it walks the stack's `ADD_VALUE` modifiers on attack damage, attack
speed, armour and armour toughness, and calls `replaceModifier` with the same attribute, id and slot and the scaled amount from `TierRules`.

**Why this event and not a per-item `getAttributeModifiers` override.** The relic weapons, gear and armour are three item classes, and 1.21 no longer has
that override at all (the modifiers are a data component of the item, `ItemAttributeModifiers`). The event also covers adopted relics (items the mod
already had, whose classes we do not own) with no change to them. **Why keep the id and slot.** `replaceModifier` swaps by attribute and id; keeping
them means the game still sees the same modifier, only stronger, so the tooltip shows the new figure and the equipment-change logic re-applies it
the moment the component changes (the component change makes the stacks unequal). **Why the first lines are a single lookup.** The event fires every
time the game asks a stack for its modifiers, including once per tick per equipped stack, so the unraised path must cost almost nothing: that is the
"NBT must not cause lag" requirement. **Why only ADD_VALUE on four attributes.** Those are the item's own base stats; percentage modifiers from
enchantments or other mods are not ours to rewrite. (The passive attribute bonuses a def lists, applied once a second by the relic engine, are not
scaled by tier: they are the relic's lore, not its grade.)

### Act power, cost and cooldown (RelicEngine, Fx, ActPower)

`RelicEngine.activate` multiplies the percent costs by the cost factor and the cooldown by the cooldown factor, and sets `ActPower` around the act
body; `Fx.hit` multiplies the raw damage by `ActPower.current()` **before** `BalanceLaw.damage`. The balance law's EP scaling, the 400 raw cap and the
players' per-hit cap come after, unchanged.

**Why a static multiplier and not a parameter.** Acts are lambdas written across twenty waves, all calling `Fx.damage(p, target, amount, type)`.
Changing that signature would touch every wave and every other agent's work; a value set around the act body reaches them all. It is safe because acts
run on the server thread only and the value is restored in a `finally` (nested acts restore the outer value), so it is 1.0 outside an act. **Why the
tier acts before the balance law.** The law is the single place that caps hits on players and scales by EP; feeding it a bigger raw number means
the tier can never bypass it.

### The upgrade ritual (TierRitual)

Sneak and use a catalyst with a relic in the other hand (or sneak and use the relic with the catalyst in the other hand). The catalysts:

| Jump into | Catalyst | Cost (percent of max magicules, aura pays the rest) | Success | Fail: catalyst lost? | Fail cooldown |
|---|---|---|---|---|---|
| Special | 2x `tensura:low_magisteel_ingot` | 2 | 100% | no | 5 s |
| Rare | 2x `tensura:high_magisteel_ingot` | 4 | 95% | no | 10 s |
| Unique | 3x `tensura:mithril_ingot` | 6 | 85% | yes | 20 s |
| Legend | 3x `tensura:adamantite_ingot` | 10 | 70% | yes | 40 s |
| God | 4x `tensura:hihiirokane_ingot` or 1x `nusmp:devils_core` | 15 | 55% | yes | 80 s |
| Genesis | 1x `nusmp:genesis_seed` | 22 | 40% | yes | 120 s |

The Tensura ids were verified against the Tensura 2.0.1.3 id list. The ritual refuses before it takes anything if: the relic is not attuned, it is
already Genesis, the catalyst is wrong or short, the cooldown has not passed, or the pool cannot pay. After that the pool is paid and the catalyst
taken, then one roll. **The relic is never destroyed or lowered:** a failure sets a per-player cooldown (saved in the player's persisted data, so it
survives relog and death) and, from the Unique jump up, costs the catalysts. A success writes `nusmp:tier` onto the very stack: its serial and
attunement are untouched, so the Order's ledger (which identifies a relic by serial) still knows it as the same copy.

**Why a sneak-use event and not a Smithing Table recipe.** A recipe makes a new stack from ingredients, which is exactly the thing that would copy
or reissue a unique relic; the sneak-use edits the one stack in place. It also needs no recipe type, serializer or JEI plumbing, and the Tensura
catalysts have no recipe-friendly shape (counts, alternatives). **Why high priority and cancel.** The same click would otherwise fire the relic's own act.
**Why the odds rise with the grade.** The first jumps are for getting a relic out of the Normal tier and should feel like crafting, not gambling; the
top jumps are the end-game sink the brief asks for. **Why a failure never takes the relic.** Relics are unique world items: losing one to bad luck
would make the world unrecoverable, which the Order's design explicitly avoids. **Why the cost is paid from magicules then aura.** That is how every
relic act pays (`CostMath.spendPooled`), so the ritual works on base Tensura data and needs no pool of its own. **Why a cooldown after a success
too.** The use event repeats while the button is held; 2 s stops one click from raising two grades.

### The two own catalysts (TierItems)

`nusmp:devils_core` (stack of 16) and `nusmp:genesis_seed` (stack of 16), both glinting, with a short tooltip, a 16x16 icon and a flat item model.
**Why own items for the top two.** Tensura has Hihiirokane but nothing for a "genesis" step; the Devil's Core is the alternative to four Hihiirokane
ingots for the God jump, so a world with no Hihiirokane source is not locked out. They are registered in their own `DeferredRegister.Items`, so no
shared item file is touched.

### Display: name, tooltip, EP lift

The item name takes the colour of the **effective** grade, the glint starts at an effective Legend, the tooltip shows the effective grade's stars,
a line "Raised from X grade (+n grades)", the acts' costs and cooldowns as they really are, and the frame colour of the anime tooltip follows
the effective grade. The grade's EP lift (`Relics.work`) uses the effective grade.

**Why through `RelicItemSupport.name(base, def, stack)` and `foil(stack, def)`.** The three item classes already route through that helper, so
the change is one line each. **Why EP follows the tier.** The grade is what lifts the wearer's existence points; a raised relic that did not lift
more would make the ritual cosmetic. The lift is already capped at 25% in total by the engine.

### Commands

`/multiverse relic tier <player> up` raises the main-hand relic one grade; `... set <grade>` sets it (a grade at or under the base removes the
tier). Permission 2. **Why:** ops need a way to test and repair without catalysts; it uses the same `Tiers.setTier`, so it cannot create a state
the ritual cannot.

## 2. Aura: player-bound dynamic scaling

### Files

`relics/scaling/AuraScaling.java` (pure numbers), `AuraCache.java` (the once-a-second reading), `AuraEvents.java` (the damage handler),
`ScalingSystem.java` (registration).

### The rules (AuraScaling)

* **Pool bonus:** +3.5% damage for every doubling of the player's maximum magicule pool above 10,000, capped at +35% (reached at 1024 times the base
  pool, about 10 million).
* **Awakening:** none x1.00; seeded (a Demon Lord seed or a Hero egg) x1.05; **awakened** (true demon lord or true hero in Tensura's existence data,
  or the Wizard King's crown awakening) x1.20 and **15% of the target's armour reduction ignored**.
* **Mana Zone** (`ManaLaw.zoneActive`): x1.08 and 5% more armour ignored.
* The product is capped at x1.60 and the armour share at 25%; against a **boss** (`BalanceLaw.isBoss`) at x1.35 and 12%.

**Why a log curve.** Tensura's pools span several orders of magnitude (a player grows from thousands to millions); a linear bonus would be
irrelevant at the start and absurd at the end. Per doubling is smooth, explainable to a player ("every time you double your magicules"), and
bounded by the cap. **Why a base pool of 10,000.** Under it there is no bonus at all, so early-game players and unawakened mobs hit exactly as before
(the brief's "keep every old behaviour unchanged below the thresholds"). **Why awakened is the big step.** In Tensura the awakening is the
single largest jump in power (the config's EP multiplier), so it is the one state that also ignores armour. **Why tighter boss caps.** The Zogratis
bosses' fights are paced by health gates; a flat x1.35 and 12% keep the length of a fight in the range the boss overhaul was balanced for.

### The reading (AuraCache) and the handler (AuraEvents)

`LivingIncomingDamageEvent` (NeoForge 21.1; the one `RelicEngine` already uses for resistances) fires before armour and the vanilla reductions are
applied. The handler runs only when all of these hold: the damage type is `player_attack`, the player is both the entity and the direct attacker (a
plain melee hit), a relic weapon of a hand slot (not a shield) is in the **main hand** and works for them (attuned if it needs that). It then takes the
cached aura, raises the amount, never lowers it, and for a player target keeps the balance law's per-hit cap (`BalanceLaw.cap`), and adds a reduction
modifier on `DamageContainer.Reduction.ARMOR` that keeps `1 - ignored` of the armour reduction.

**Why only `player_attack`.** Relic acts already pass through `BalanceLaw.damage` (EP scaling), and arrows and spells are not "the weapon in
your hand"; scaling them again would double-count. **Why an armour reduction modifier instead of changing the amount.** The armour formula depends on
the damage and the target's toughness; reducing the *reduction* by a share is exact whatever the armour value is, and other mods' reductions stay
independent. **Why the player cap is kept.** The brief: the multipliers must pass through the existing per-hit caps for players. **Why a cache.**
Reading the magicule pool goes through the Tensura attribute and capability, and the awakened flags through its existence storage; doing that on
every hit of every swing in a crowded fight is waste, while the pool changes slowly. `AuraCache` keeps one reading per player for 20 ticks
(computed lazily on the first hit, so an idle player costs nothing) holding both the ordinary and the boss aura: a hit is a map read, a tick
comparison and a field read. It is cleared on logout and server stop, and bounded at 400 entries. Every Tensura read is wrapped so a missing
capability means "no bonus", never an exception inside a damage event.

## Registration (for the lead)

```java
com.newuniverse.nusmp.relics.tier.TierSystem.registerContent(modEventBus);          // before NUItems.ITEMS.register
com.newuniverse.nusmp.relics.tier.TierSystem.registerGame(NeoForge.EVENT_BUS);
com.newuniverse.nusmp.relics.scaling.ScalingSystem.registerGame(NeoForge.EVENT_BUS);
// MultiverseCommands: next to RelicCommands.addTo(root): com.newuniverse.nusmp.relics.tier.TierCommands.addTo(root);
// NUCreativeTab: for (net.minecraft.world.item.Item i : com.newuniverse.nusmp.relics.tier.TierItems.all()) output.accept(i);
```

## What was changed outside the new packages (all additive)

* `relics/RelicEngine.java`: `activate` scales the cost and cooldown and wraps the act body in `ActPower`; `pay` takes a cost factor.
* `relics/Fx.java`: `hit` multiplies the raw amount by `ActPower.current()`.
* `relics/Relics.java`: the EP lift uses the effective grade.
* `relics/RelicItemSupport.java`: `name(base, def, stack)` and `foil(stack, def)` (the old `name(base, def)` stays).
* `relics/RelicWeaponItem.java`, `RelicGearItem.java`, `RelicArmorItem.java`: `getName` and `isFoil` call the two new helpers.
* `client/relics/RelicTooltip.java`: the effective grade in the name colour, the frame, the grade row, a "Raised from" line, the act costs.

## 1.104.0: the attune lock

Tiers and grades set how strong a relic is for the player it is attuned to; since 1.104.0 that full strength belongs to that player alone. A relic that needs attunement (Legend and above) and is not attuned to its wielder deals 20% of its damage (melee, arrows, bolts), by one rule in `relics/RelicPower` applied at the damage event (`RelicPowerEvents`); acts and passives stay off while unattuned, and the tier act power is multiplied by the same factor. Config `relics.attuneLock` and `relics.unattunedPower`; see `docs/relic_attune_lock.md`.
