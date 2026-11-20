# Hero and vassal weapon damage no longer starts at 10 000 (4.37.1)

Owner report: hero weapons and vassal weapons dealt about 10 000 damage from the very first blow.

## Cause
Four raises stacked from level 0: the hero weapon buff (x4, x1.5 more for the four Legendary weapons), the anime factor (x2), and the 3.74.0 power raise (x25 for Seven Star / Vassal weapons, x37.5 for the Legendary four). Only the anime factor grew with the weapon's level.

## Fix (server settings in `nusmp-power-server.toml`, section `[power]`)
- `levelRamp` (default true): the power raise now grows with the weapon's level. `false` gives the old behaviour exactly (full raise at level 0).
- `levelStartShare` (default 0.05): the share of the raise's extra a level-0 weapon has; it rises in a straight line to all of it at level 100. `PowerRules.ramp`.

| Weapon | Raise at level 0 | at level 50 | at level 100 |
|---|---|---|---|
| Seven Star / Vassal (factor 25) | x2.2 | x13.6 | x25 |
| Legendary four (factor 37.5) | x2.8 | x19.7 | x37.5 |

Example, a Vassal Scythe blow (8 damage): about 140 at level 0 (was about 1 600 before), about 1 600 at level 100 (unchanged). The Legendary Sword (7): about 240 at level 0 (was about 3 150), unchanged at level 100.

## KEPT
The hero buff, the anime factor, the Djinn and Beast Will raises, the PvP hit cap, the boss caps, every other setting. Only the vassal / legendary part of the raise is ramped.

## Cost
No new tick, packet or state: one extra level lookup in the existing damage hook (the bond is already read there).

## Not checked
Not played, and the new test (`PowerRulesTest.theRaiseGrowsWithTheWeaponLevel`) was not run locally (no JUnit jar this session); CI runs it. If level 0 is still too strong, lower `levelStartShare`.
