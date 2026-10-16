# arn (1.46.0): Lucius balance, his arena, the Hourglass of the Final Hour, spawn only there

The full design, the before/after balance table, the layout, the item, the spawn gate, the config keys and what is unverified are in
[`docs/lucius_arena.md`](../lucius_arena.md). Tests: `src/test/java/com/newuniverse/nusmp/arena/*` (state machine, spawn gate table, return book round trip,
hourglass rules, courtyard layout) and `.../boss/god/{LuciusBalanceTest,ArenaScalingTest}` plus the updated tests of the moved constants.
