# Boss VFX and buffs, part B2 (1.35.0): the Devil Avatar, the Goliath, the Charybdis Spawn and the God of Order

Addendum to `boss_overhaul.md` (the sibling note for the other bosses is `boss_vfx_buff_b3.md`). Everything below only sends VFX packets and adds rules to
moves; no block is placed, moved or broken, and nothing was run in game (CI compiles; the effects were previewed headlessly, see the end).

## The shared pieces
- `boss/vfx/BossMoveVfx` (server door) and `boss/vfx/BossVfxRules` (pure, tested). Why: every boss move needs a telegraph and a strike effect, and the
  client keeps only 64 effects at once, so the four bosses send through one door that marks the effect as telegraph or strike (the lowest bit of the
  payload seed: 0 = the wind-up, dim and closing; 1 = the strike, bright and fast), clamps the radius to what a room can hold and spends from a budget of
  12 effects per game tick. A telegraph is sent once, when the wind-up begins (not every third tick like the dust tells), and lives as long as the wind-up
  (6 to 80 ticks). The old particle tells (`Tells.ring/line/...`) and the older VfxSpawn circles stay: they are the exact hit shapes; the new effects are
  the signature on top.
- `vfx/client/layer/BossFx` (drawing kit) and four layers, one per boss, each with four shapes (16 new `VfxShape` entries appended at the end, see
  `scratchpad/boss2/SHAPES_b2.md`). Why one layer per boss: each boss has its own silhouette language, and the kit keeps every shape inside the 400-vertex
  budget (measured by the preview: the heaviest is 368). No new texture: they are built from the existing particle textures. The ring of a soft glow samples
  one vertical line of the glow texture (a band that fades towards both edges) instead of the whole disc.

## Devil Avatar (`entity/avatar/AvatarVfx`, layer `DevilBossLayer`): hellfire circles, chains, devil sigils, brimstone cracks
Why: a high devil of the Spade Underworld; gravity is a chain from the roof, a curse is a sigil, a blow is a crack in the floor with a jet of hellfire.
| Move | Telegraph | Strike |
|---|---|---|
| Void Claws | cracks on the claw cone | cracks burst |
| Hex Volley | small devil sigil under the Avatar | the sigil flares |
| Shadow Step | sigil where it will step out | cracks where it arrives |
| Ceiling Pull | devil sigil on the whole ring + six chains hanging from the roof | chains snap taut + flare |
| Floor Crush | sigil on every mark | cracks on every mark |
| Inversion Field | sigil on the mark | chain cage over the field for 2 s |
| Dread Brand | brand sigil (crossed lines in a ring) and a dim pillar falling on it | the pillar collapses white-hot |
| Curse Nova | cracks across the whole reach | cracks and a shock ring |
| Call the Disciples | sigil | flare |
| Abyssal Collapse | the pull's sigil and chains, then a sigil on every crush mark | cracks on every mark, then the nova |
| Gates (true form) | a sigil 9 blocks wide for 2.5 s, cracks, chains | |
Colour: the Avatar's own (violet for Lucifero, crimson for Megicula).
New move, Brimstone Eruption (from 60% power, before the ultimate in the list, 24 ticks of wind-up, 200 cooldown, 13 base damage through the balance law):
spokes of brimstone burst along lines the wind-up draws (5 spokes; 7 at the Gates, and then a second wave on the gaps of the first, 24 ticks later, drawn in
the same wind-up). Why: it is the signature of the devil, and the gaps make it a move to read, not to tank: `AvatarMath.gapAt` is tested so that a player
fits a gap from 6 blocks out in both forms, and the Avatar keeps 5 to 10 blocks from its foe. Spoke mode of `BOSS_DEVIL_CRACKS`: the horizontal distance of
`to` from `from` is the spoke count, its direction the first spoke.
Buffs: health 900 -> 1100, armour 8 -> 9, toughness 4 -> 5, attack 14 -> 15 (`AvatarTable`), a fourth wave of Dark Disciples after the Gates open (at 640
ticks), plus the new move. Damage still goes through the balance law, the 45% player cap and the 6%/s budget.

## Goliath (`dungeon/floor/goliath/GoliathVfx`, layer `StoneBossLayer`): stone shockwave rings, dust columns, boulder trails, fissure lines
Why: a stone giant that fights with its weight; the ground is the weapon, so the effects are on the ground (and drawn only: no block changes).
| Move | Telegraph | Strike |
|---|---|---|
| Ground Slam, Twin Stomp, Roaring Shockwave, Bedrock Quake | outline of the wave with a ring closing in | a ring of dust and amber light running out as fast as the wave does, chips and a dust skirt |
| Bedrock Fist | the shadow of a boulder over the circle + the circle | the boulder falls, a column of dust, a ring |
| Seismic Line | dotted glowing line along the lane | a jagged crack runs down the lane with plumes of dust |
| Rubble Fall | a shadow on every circle | boulders fall with dust trails and a ring on landing |
| The waking | four fissures from the seal | a column and a ring when it climbs out |
| Gates | red ring, column and three fissures | |
Colour: amber; red once the Gates are open.
Buffs: health 1500 -> 1800, armour 12 -> 13, toughness 6 -> 7, attack 30 -> 32. At the Gates (the cracked giant): the Seismic Line becomes a fork of three
lines (the side lines turn 0.55 rad; `GoliathEnragedTest` checks that from 10 blocks out the gap between lines fits a player, and the hit list is shared so
nobody is struck twice), the Twin Stomp has a third ring (same shared budget of 55% of the victim's health), the Bedrock Fist is followed by a 6-block
aftershock ring 10 ticks later (drawn in the wind-up in red), the Rubble Fall drops one more circle (at most 8).

## Charybdis Spawn (`dungeon/floor/charybdis/CharybdisVfx`, layer `TideBossLayer`): whirlpool spirals, water spouts, tentacle sweeps, tide rings
Why: a leviathan of the basin: the water itself moves; spirals pull, spouts break out of walls, tentacles sweep, rings of tide run out from a crush.
| Move | Telegraph | Strike |
|---|---|---|
| Scale Volley | a small vortex gathering at the mouth | a tide ring at the mouth |
| Tentacle Sweep | the two edges and the rim of the arc | three tentacles sweep the 200 degrees with a ribbon of water |
| Whirlpool, Maelstrom | the rim and a faint spiral | the full turning vortex for the length of the zone (80 / 100 ticks) |
| Beak Lunge | a ring where the dive ends | a spout and a ring as it launches |
| Summon Megalodons | a spout at every wall spot | a spout when they tear loose |
| Tidal Crush | tide ring on the circle | three rings of foam and a spout |
| Surfacing, Gates (Frenzy) | | a vortex and spout; the Frenzy is a cold white-blue ring, vortex and spout |
Buffs: health 1100 -> 1350, attack 24 -> 25, armour 6 -> 7. The Frenzy (the Gates) makes the sweep reach 20% farther, the whirlpool 25% wider and the crush
40% wider (all drawn in the wind-up: the circles show the new size, `CharybdisFrenzyTest`), and a cast brings three megalodons with a cap of five (was two
and four). A rebound sweep that would have covered the whole circle was considered and dropped: two 200 degree arcs leave no gap to stand in.

## God of Order (`relics/order/OrderVfx`, layer `OrderBossLayer`): white-gold law sigils, grid lines, correction beams, ledger glyphs
Why: a judge: everything is straight, symmetric and written; no flame or water, only lines and script.
| Where | Telegraph | Strike |
|---|---|---|
| Decree of Binding | sigil + a ledger grid on the ring | both flash and stamp |
| Writ of Light | a thin correction beam and a ring on its end | a wide beam with script spiralling along it |
| Circle of Judgement | sigil + a beam from the sky onto it | the beam falls and the sigil is stamped |
| Erasure | the halo and a wide grid for the whole 60-tick tell | the halo rings out, the grid and sigil flash |
| The visit: arrival, census, correction, farewell | sigil + grid at the spot (6 s) / the halo while it reads / | a beam down from the sky when it steps out / the staff's beam burns the duplicate / the halo on leaving |
Buffs: health 1200 -> 1500, attack 14 -> 16, armour 10 -> 11. The Open Ledger (the Gates, white-gold) makes the Decree's ring 30% and the Judgement's circle 25%
wider and adds a second Writ line across the first (a quarter turn; the wind-up draws both, one hit per foe). Erasure used to start at 35% health, above the
25% at which the Gates open, so the Gates and their refilled bar were practically unreachable: `OrderTable.forcedErasureDue(ticks, fraction, gatesOpen)` now
ends the matter by time (75 s of combat, +30 s once the ledger is open) or, with the ledger open, at 15% of the refilled bar (3-argument rule; the old
2-argument rule and its test are kept). The Erasure tell is 3 s and the budget lets 15% go in 2.5 s, so the God stays killable.

## The 100% modes (the second health bar)
All four are `ZogratisBoss` subclasses, so the Gates (power 100: the Avatar's true form, the Goliath's cracked form, the Charybdis' Frenzy, the God's Open
Ledger) already call `refillForMode("gates")` (agent b1's framework, `boss/refill/*`, see `CONTRACT_REFILL.md`): health eases to full over 40 ticks inside the
60-tick transformation, the boss cannot be hurt meanwhile, the refill bypasses the damage budget, the bar flashes and a chat line says so, once per fight,
persisted. Nothing had to be added to the four bosses; their `onGatesOpened` hooks only add the VFX of the transformation. Loot, collapse events and the
Order's Erasure/XP fire only on the real death or the real end of the visit.

## Tests and previews
Pure tests: `BossVfxRulesTest`, `AvatarEruptionTest`, `GoliathEnragedTest`, `CharybdisFrenzyTest`, `OrderOpenLedgerTest`; the pinned numbers of
`OrderTableTest` and `CharybdisTableTest` were updated (health and attack). Previews: `python tools/vfx_preview/preview.py b2_devil_sigil_tell b2_devil_sigil_hit ...`
(scenes in `tools/vfx_preview/scenes.d/boss_b2.json`, 17 shapes/variants x telegraph/strike); `tools/vfx_preview/preview.py` got four lines so that the four layers
find `BossFx`.
