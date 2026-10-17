# The Amphisbaena (`amphisbaena`, Level 5, tier 2)

The Monster Rex of the 27th floor, the Water City: a two-headed water dragon, one head flame, one head mist. Part of [`danmachi.md`](danmachi.md). Class `arc/danmachi/AmphisbaenaBoss`, table
`AmphisbaenaTable`, record `DanMachiRoster.AMPHISBAENA`, model `tools/arc/char_amphisbaena.py`.

## 1. Canon (as summarised) and sources

* The Monster Rex of the **27th floor**, the "Water City": a huge white two-headed dragon with a long body and fins. One head breathes a **blue flame that burns through steel**, the other a **red mist that dissipates magic**
  (as the summaries describe it, it even undid a spell of Haruhime's that strengthened the party). A physical blow, strengthened by Mikoto's gravity, took the red head off; after that magic worked again and the other head fell.
* Sources (standard page titles, **not opened**): `https://danmachi.fandom.com/wiki/Amphisbaena`, `https://en.wikipedia.org/wiki/Is_It_Wrong_to_Try_to_Pick_Up_Girls_in_a_Dungeon%3F`.

## 2. Added for the game (not canon)

The plate armour with blue edges, the colours of the eyes (blue for the flame, red for the mist), the Scorch Field, the Twin Calamity, the Water City Call's drowned, the Level-Up and the Blue Flame Cyclone; every number.

## 3. Numbers

Health 1,900, attack attribute 20 (plain blow 10), speed 0.27, armor 8, toughness 3. Hitbox 1.7 x 4.09. Floors 22 to 36 on both roads, after the Black Goliath. Charter Grimoire (30%, LEGEND): `flame_breath`, `fire_breath`,
`water_jail`, `water_current_control`, `magic_jamming`, `scale_armor`, `water_attack_resistance`, `flame_attack_resistance`. Lines: "SHAAAAAAA!" / "Both heads rear, the flame and the mist! SHAAAAROOOOAR!" / "...shaaa...".

## 4. Moves (base numbers, before the power tuning)

| move | id | power | wind-up | cooldown | range | weight | recovery | flags | what it does |
|---|---|---|---|---|---|---|---|---|---|
| Twin Bite | `twin_bite` | 20% | 18 t (0.9 s) | 90 t (4.5 s) | 0,7 | 6 | 10 | - | its own code (reach 6.5, base 12) |
| Tail Sweep | `tail_sweep` | 20% | 22 t (1.1 s) | 150 t (7.5 s) | 0,10 | 4 | 12 | crowd | circle 9.0 blocks round the body, 12 dmg, knock 1.6 |
| Blue Flame Breath | `blue_flame_breath` | 20% | 28 t (1.4 s) | 240 t (12.0 s) | 4,18 | 5 | 14 | - | breath 16 blocks, +-28 deg, 8 pulses of 6.5 dmg, sweeps 70 deg |
| Red Mist Breath | `red_mist_breath` | 20% | 28 t (1.4 s) | 420 t (21.0 s) | 5,30 | 3 | 12 | crowd | its own code (reach 6.5, base 0) |
| Tidal Surge | `tidal_surge` | 20% | 24 t (1.2 s) | 180 t (9.0 s) | 0,12 | 4 | 12 | crowd | ring wave to 12 blocks, 11 dmg, knock 1.4, 2 ring(s), 14 ticks apart |
| Waterspouts | `waterspouts` | 20% | 24 t (1.2 s) | 210 t (10.5 s) | 5,32 | 4 | 10 | - | 3 mark(s) r 2.3, 10 dmg, knock 1.2, lands 16 ticks after the wind-up |
| Heads' Lash | `heads_lash` | 40% | 22 t (1.1 s) | 190 t (9.5 s) | 4,22 | 3 | 12 | - | 2 lane(s) 18 long, 2.0 wide, 13 dmg, knock 1.0, at once |
| Water City Call | `water_city_call` | 40% | 30 t (1.5 s) | 720 t (36.0 s) | 0,48 | 2 | 10 | - | its own code (reach 0, base 0) |
| Dive Strike | `dive_strike` | 40% | 28 t (1.4 s) | 260 t (13.0 s) | 8,34 | 3 | 12 | - | leap onto the target's mark, ring 8 blocks, 15 dmg, knock 1.3, 10 ticks in the air |
| Scorch Field | `scorch_field` | 60% | 26 t (1.3 s) | 330 t (16.5 s) | 5,30 | 3 | 12 | crowd | its own code (reach 7, base 4) |
| Twin Calamity | `twin_calamity` | 60% | 64 t (3.2 s) | 1500 t (75.0 s) | 0,36 | 5 | 60 | ultimate, crowd | its own code (reach 16, base 7) |
| Blue Flame Cyclone | `blue_flame_cyclone` | 100% | 30 t (1.5 s) | 300 t (15.0 s) | 0,14 | 4 | 14 | leveled | breath 14 blocks, +-28 deg, 12 pulses of 6.0 dmg, sweeps 360 deg |

* **Twin Bite**: two bites six ticks apart, each a cone of 6.5 blocks and 55 degrees (12 each), one budget. **Tail Sweep**: a circle of 9 blocks. **Tidal Surge**: two rings 14 ticks apart.
* **Blue Flame Breath**: a breath of 16 blocks in a cone of 28 degrees each side whose axis **sweeps 70 degrees** over eight pulses of 4 ticks (6.5 each, magic damage that ignores armour, one budget); the telegraph shows the whole sweep (63 degrees each side). The body holds still and turns with it.
* **Red Mist Breath**: a lance of mist to where the target stood when the wind-up began and **a cloud of 6.5 blocks that stands 110 ticks** there: the grimoire of every player in it is sealed (the mod's own `nusmp_sealed_until` flag, refreshed every two ticks), except
  users of anti-magic; it ends when the red head is cut. Step out of it, or cut the head.
* **Waterspouts**: three marks at 20% and 40%, four at 60%, five at 80%, six at the Gates, landing 16 ticks after the wind-up. **Heads' Lash**: two parallel lanes of 18 blocks (13). **Dive Strike**: a leap of 10 ticks, a ring of 8 blocks (15).
* **Water City Call**: drowned rise from the flood round it (40 kit health, 7 attack, scaled by `AddPlan`), as many as the cap allows.
* **Scorch Field** (60%): the blue flame burns where the target stood for 80 ticks (4 base damage every 10 ticks in a circle of 7 blocks, magic, one budget).
* **Twin Calamity** (ultimate, 60%, 3.2 s wind-up): **both heads breathe at once**: a pair of opposite beams (half angle 14 degrees, 16 blocks, 7 per pulse) that turn all the way round the room in 12 pulses (48 ticks), while the red head is on the mist
  seals the grimoire of everyone within 16 blocks for the whole turn; a ring of water (9) runs out at the end. With the red head cut it is the blue flame alone: one beam, no seal. Exposed for 60 ticks after.
* **Blue Flame Cyclone** (leveled): a breath of 14 blocks that turns all the way round in 12 pulses (6 each).

## 5. The mechanic and its counter (and why): the red head (`HeadCut`)

The story is decided by cutting the right head first, and the game cannot aim at a head, so the rule is built from what players can do: **when the red head rears up to breathe (the wind-up of the Red Mist Breath and 40 ticks after it) the
blows that land on the boss wear it down**: a physical blow counts once, an **anti-magic blow twice** (a Demon-Slayer sword in hand, or an Anti-Magic Lord: they unravel the mist), a **spell not at all** (the mist eats it). **7% of its maximum
health in counted damage cuts the head** (133 at 1,900); the action bar shows the progress to the one who strikes ("The red head reels: 40%. Blows count, spells do not."). A head not cut when the window closes forgets the wear. Once it is off, **the
mist moves are gone** (the Red Mist Breath is no longer chosen; the Calamity loses its mist) and the chat says so. **The fight itself tears the head off at 80% power** (`HeadCut.FORCE_CUT_POWER`; the Twin Calamity at 60% still has it once), so the
fight never depends on a mechanic being found. The head stays cut when the world is saved and loaded.

**The Level.** Party Level 5 gives 1 step (Level 6, Blue Flame Cyclone), 7 gives 2, 9 gives 3.

## 6. Verified and not

Pure and tested: the table, the head (the window, the weights, the cut, the forced cut and the reset), the geometry, the Level, the add plan. Looked at: the model (front, face, idle, attack, the leveled form).
**Not verified**: the game side (the sweeping breath's turn of the body, the mist cloud's seal of a player's grimoire, whether `DemonSlayerSwordItem` in hand is enough to call a blow "anti-magic"), the look of the blue flame (**there is no pure blue
flame effect in the mod**: the flame is the mod's fire shapes with a blue glow), the sounds (the ender dragon's), the balance, the canon points of section 1. The kit cannot build a serpent's body: the dragon is a humanoid with a dragon's tail, fin ears,
horns and a second head on its own bone beside the first.
