# Grimoire leaf power (4.83.0)

Owner request: "the power difference from a 3 leaf to a 4 leaf and so on should have a massive difference just like the anime. Give me a graph of the current power scaling to what the new one will be".

## What it is, in plain words

Until 4.82 the cover of a grimoire changed its spell power by only 5% per step (three-leaf x1.00, four-leaf x1.05, five-leaf x1.10). Now the cover
multiplies every spell by a real step on top of that:

| Cover | Old spell power | Leaf power (new) | Total now |
|---|---|---|---|
| Three-leaf (and every other tier-3 cover) | x1.00 | x1 | x1.00 |
| Four-leaf, Double Spade, Two-Heart, Five-Sided Diamond | x1.00 to 1.08 | x2.5 | about x2.6 |
| Five-leaf, Triple Spade | x1.10 to 1.12 | x6 | about x6.6 |
| Black Magic | x1.15 | x10 | x11.5 |
| God-Tier | x1.15 | x14 | x16.1 |

Chart: `docs/previews/grimoire_leaf_power_chart.png` (left: the multiplier, right: one mid page hit, 12 raw at 10,000 EP, with the grimoire buff
x1.75 and the EP scale x3.5: three-leaf 74, four-leaf 193, five-leaf 485, Black Magic 845, God-Tier 1,183).

## The price (4.84.0): so the scaling makes sense

Power with no price would make the three-leaf pointless. A higher cover now also asks more mana per spell (on top of the cover's own cost number):
four-leaf x1.6, five-leaf x2.5, Black Magic x3.2, God-Tier x3.8 (three-leaf and the other tier-3 covers x1). It stays below the power step, so a higher
cover is always more efficient (power per mana: about x1.7, x2.8, x3.6, x4.7), just not free. Settings: `leafCostFour` 1.6, `leafCostFive` 2.5, `leafCostBlack` 3.2,
`leafCostGod` 3.8 (all need `leafPower`). Applied in `GrimoireBook` where the page cost is computed; the tooltip shows the cost too.

## Where it applies

- `GrimoireBook.pageDamage` (every page through `hurt` / `hurtAs` / `hurtDirect`) and `TensuraShots.fire` (the Tensura-style bolts; their old
  28 hit cap is lifted by the same factor, otherwise the cap would hide the difference).
- The item tooltip shows the new total ("Spell power x...").
- Everything else is unchanged: the cover's own cost, the late-game scaling, the player-versus-player hit cap (a player still takes at most the
  `pvpHitCapPercent` of their health per hit, so a five-leaf cannot one-shot a player), the mob and boss rules.

## Settings (SERVER, `nusmp-server.toml`)

`leafPower` (true; false = the old covers exactly), `leafPowerFour` 2.5, `leafPowerFive` 6.0, `leafPowerBlack` 10.0, `leafPowerGod` 14.0.

## New, replaced, kept

- NEW: `grimoire/GrimoireLeafPower`, the five settings, `GrimoireLeafPowerTest`, the self-test `scaling.leaf` (under the existing `scaling` prefix).
- REPLACED (by default): the size of the step between covers (was 5%).
- KEPT: every cover, the cover damage and cost numbers, all page numbers, the legacy `GrimoireMagicSkill`, Time Magic.

## Cost

No extra work: one multiplication per hit, no packets, nothing per tick.

## Not done

Spell cost does not change with the leaf. The legacy `GrimoireMagicSkill` and `GrimoirePageSkill` hits were not changed (they do not go through
`GrimoireBook`). Not tuned in a game: the numbers are a first proposal for the owner to judge.
