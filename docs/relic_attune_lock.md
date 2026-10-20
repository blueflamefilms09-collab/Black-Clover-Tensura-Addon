# The attune lock (1.104.0)

Owner request: "a relic's max damage should be locked to only be allowed when attuned, to nerf it to only be about 20% power."

A relic that needs attunement (`RelicDef.needsAttunement()`: Legend, God and Genesis relics written with `.attune()`, see `AttuneRules.REQUIRED_FROM`)
and is held by someone it is NOT attuned to (never attuned, or attuned to another player) deals only 20% of its damage. The player it is attuned to
gets the full figure, as before. Nothing else about attunement changed: the five-second sneak ritual, the cap of three, the pair rule (one ritual
attunes both halves), the Unbinding Seal and `/multiverse relic unattune` are as they were.

## What the code did before

| Part | Before 1.104.0 |
|---|---|
| Melee | The weapon's attack attribute (`RelicWeaponItem.attributes`, the 1.97 grade table, normal 20-30 ... Genesis 260-350) applies to whoever holds it. Nothing looked at attunement, so an unattuned relic sword hit for full damage. |
| Bows and crossbows | `RelicRangedSupport.intercept` refused to draw an unattuned relic bow at all (0% power). |
| Acts | `RelicEngine.activate` refuses with "is not attuned to you" (`Attune.works`). Off, not weak. |
| Passives (attributes, effects, flags, resistances, skills) | `Relics.loadout` only lists relics that work for the wielder (`AttuneRules.working`), so an unattuned relic has none. Off, not weak. |
| Wave on-hit and event hooks | They read `Relics.loadout`, so they only run for an attuned wielder. |

## What it does now

One pure rule, `RelicPower.factor(attunedToWielder, needsAttunement, config)`: 1.0 when the relic needs no attunement, when it is attuned to the wielder,
or when `relics.attuneLock` is false; otherwise `relics.unattunedPower` (default 0.20, clamped 0.05 to 1.0). There is no per-relic exemption list
(`RelicPower.EXEMPT_IDS` is empty and a test keeps it empty). Mobs that wear a relic count as attuned (`Attune.isMine` answers true for a non-player).

It is applied in one place on the server, `RelicPowerEvents`:

1. **Melee.** A `LivingIncomingDamageEvent` (priority LOW, so the amount is the final one) whose damage is the attacker's own blow (damage type player
   attack or mob attack, and the entity that struck is the credited attacker) is scaled by the factor of the stack in the attacker's main hand at that
   moment. Once per hit. Vanilla attacks, sweep hits and Better Combat attacks all end in `Player.attack`, so they reach the same event; a Better Combat
   combo keeps the same main-hand stack.
2. **Arrows, bolts and thrown tridents.** When a projectile enters the world owned by a player, the launcher is looked up (the item in use, else a hand
   that holds a relic bow or crossbow) and, if its factor is below 1, the projectile is tagged `nusmp_relic_power` in its persistent data. The same event
   hook scales the hit of a tagged projectile. An untagged projectile (a vanilla bow in the other hand, a spell) is never touched.
3. **Relic bows are drawn.** With the lock on, a plain use of an unattuned relic bow or crossbow now draws it (its arrows then deal 20%); sneak + use is
   still the ritual. With `relics.attuneLock=false` the old refusal comes back.
4. **Acts and passives.** Left as they are: already off while unattuned (`Attune.works`), and an off power is not cut a second time. The act path
   multiplies the act-power helper (`ActPower`, used by `Fx.hit`) by `RelicPowerEvents.actFactor`, the same rule, which is 1.0 whenever the gate let the
   act through; it is there so a future relaxing of the gate cannot give an unattuned act full strength.

Only `Relics.defOf(stack) != null && def.needsAttunement()` is ever cut. Djinn weapons and overlays, Requip weapons, hero and Shield Hero weapons,
loot-table gear, Elsdocia (not a relic card) and every non-relic item are outside the rule. Creative players and operators are cut like anyone else
(the tooltip makes the state clear when testing).

## Feedback

* Tooltip on a main-hand relic that needs attunement and is not yours (client, `ItemTooltipEvent`): "Unattuned: 20% power. Hold sneak + use for 5 s to
  attune", or "Attuned to someone else: 20% power. Hold sneak + use for 5 s to take it over".
* The first nerfed hit of a session shows on the action bar: "This relic only deals 20% of its power until you attune it" (again after a relog).
* `/multiverse relic power [player]` (operators): per carried relic, whether it needs attunement, who it is attuned to, and the factor in force.
* The attunement ritual text is unchanged.

## Config (server file `nusmp-relicpower-server.toml`)

| Key | Default | Meaning |
|---|---|---|
| `relics.attuneLock` | `true` | `false` restores full power for everyone (and the old refusal to draw an unattuned relic bow). |
| `relics.unattunedPower` | `0.20` | The fraction an unattuned relic deals, 0.05 to 1.0. |

## NEW / REPLACED / KEPT

| | Item |
|---|---|
| NEW | `relics/RelicPower` (pure rule), `RelicPowerConfig`, `RelicPowerEvents` (melee and projectile hook, tooltip, once-a-session line, report), `RelicPowerSelfTests`; command `/multiverse relic power [player]`; lang fragment `tools/lang/attune_lock.json`; tests `RelicPowerTest`, `RelicPowerCoverageTest`. |
| REPLACED | Plain use of an unattuned relic bow or crossbow: refused before, now draws at the unattuned fraction (the lock off keeps the refusal). Unattuned melee: full damage before, 20% now. |
| KEPT | The works gate for acts and passives, the ritual, the cap of three, pairs, the Unbinding Seal, the grade damage table and every relic's attribute numbers (the tooltip still shows the full figure; the cut happens at the hit), the Time Magic VFX. |

## Self-test

`relic.attune_lock` (CI server smoke test): a stand-in player holds a registered relic weapon that needs attunement beside a 4000 HP chicken with no
armour. A blow of 100 raw damage costs 100 HP bare-handed, about 20 HP unattuned and unattuned-to-self (attuned to someone else), and not less than 100
attuned; an arrow tagged at launch from an unattuned relic bow (if one is registered) takes 20, an attuned bow's arrow is untagged; a relic that needs no
attunement deals full damage; a mob wearer's factor is 1.0; Elsdocia is not an attunement relic; the act factor is 1.0 attuned and the unattuned
fraction otherwise, and an act of an unattuned relic still does not run.

## Not verified (nothing could be run in a game)

* No game, no Gradle: the code was type-checked against the API stubs and the pure rule and source tests ran in the JUnit shim only. The self-test
  has not run.
* That Better Combat's server attack reaches `LivingIncomingDamageEvent` with the player attack damage type (it should, it calls `Player.attack`).
* The order against other mods' damage hooks: the cut runs at priority LOW, so a mod that sets the amount later overrides it.
* Tensura's own weapon hits that use their own damage types are not melee blows here and are not cut; relic acts never use the lock (they are off).
* Whether an adopted relic item with its own shooting code (a vanilla-style `ProjectileWeaponItem`) spawns projectiles owned by the player; if one spawns
  a projectile with no owner it is not tagged. No such adopted relic needs attunement today (all adopted ones are below Legend or written without it).
* The tooltip line and the action-bar line only exist in lang as a fragment (`tools/lang/attune_lock.json`, merged by whoever assembles `en_us.json`);
  until then the built-in fallback text is shown.
* Creative-mode and operator testing: they are cut too; use the tooltip or `/multiverse relic power` to see the state.
