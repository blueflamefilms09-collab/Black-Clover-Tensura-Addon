# Boss effects and buffs, part 1: the refill framework and the four Zogratis bosses (1.35.0)

## Why a second health bar
The owner asked that a boss's "100% mode" reset the health bar to full. The Gates of the Tree of Qliphoth (power 100) are that mode for
Dante, Vanica, Zenon, Lucius, the Devil Avatar, Goliath and Charybdis Spawn (all extend `ZogratisBoss`). `boss/refill/PhaseRefill` is the
pure state machine: `begin(mode, health, max, ticks)` records the mode as refilled (so it can never refill twice in a fight) and `tick()`
hands out the eased health for 40 ticks, ending on exactly the maximum. `ZogratisBoss.beginGates()` calls `refillForMode("gates")`, so the
bosses need no code of their own; a boss with another final mode calls `refillForMode(id)` with a new id.

* Why invulnerable: the refill sits inside the 60-tick transformation; `hurt()` also refuses while `refill.active()` (with a deadline guard).
* Why bypassing the DPS budget: a refill is the boss healing itself, not damage; it is applied inside `BossBudget.bypass`. The cap resumes after.
* Why persisted: `RefillTag` writes the done modes and a running refill into the boss's save data, so reloading mid-fight grants no second refill.
* Why only a real death pays out: health is only raised; loot, charter and xp stay on `die()`, which fires when health really reaches 0.
* The bar signal uses no new packet field: `BossBarSync.update` lightens the tint by `refillGlow()`; the client bar morphs the colour (and the
  Gates already trigger the phase shatter). `PhaseRefill.glowStep` quantises it so only a handful of style packets are sent.
* `resetFight` (the party left) clears the refills together with health, power and form.

## Effects
`vfx/client/layer/Zogratis{Gravity,Blood,Space,Divine}Layer` + `ZogratisFx`, textures from `tools/gen_boss_vfx_textures.py` (`bfx_*.png`),
shapes appended to the end of `VfxShape`, sent with `entity/zogratis/ZogratisVfx` (one small packet each, at most 20 per tick per level).
Preview: `python tools/vfx_preview/preview.py dante_well vanica_sigil zenon_tear lucius_pillar ...` (scenes in `scenes.d/zogratis_*.json`).
Every move has a telegraph effect that lasts the wind-up (a closing rim shows the time left) and a strike effect; the dust telegraphs stay.

* Dante, gravity: wells of warped space (`GRAVITY_WELL`), shock rings with ground cracks (`GRAVITY_IMPACT`), chevron lanes (`GRAVITY_LANES`),
  a black sphere with a lensing halo (`GRAVITY_SINGULARITY`), the heat round his body (`GRAVITY_AURA`).
* Vanica, blood: scythe crescents, barbed lances, thorny sigils, leaping arcs, falling blood (`BLOOD_*`).
* Zenon, space: cracked panes, torn slits, portal rings, shattering glass (`SPACE_*`); `portal()` also draws his gate ring.
* Lucius, light: gold pillars, halo and feathers, soul vortexes, rays, holy sigils (`DIVINE_*`, `SOUL_VORTEX`); the colour is
  `FxMath.divine(power)`: gold at 20 and 40%, drifting to purple at 60, 80 and with the Gates. Time moves keep the Time Magic effects
  (only called) and add a gold sigil or pillar.

## Buffs (all inside the balance law, the 45% player cap and the DPS budget)
* Health: Dante 1200 -> 1700 (armour 15, toughness 9), Vanica 900 -> 1300, Zenon 950 -> 1350, Lucius 3600 -> 4800. With the second bar, the Gates
  add the full bar again.
* New late-phase moves, appended to the move lists, each with a full telegraph: Dante `Twin Wells` (60%) and `Titan Quake` (80%, three
  jumpable rings 14 ticks apart); Vanica `Reaper's Dance` (60%, three cuts, the next two shown 16 ticks ahead) and `Blood Storm` (80%);
  Zenon `Rift Collapse` (60%, lines of cut space through the target's spot, three or four, with safe wedges) and `Gate Storm` (80%);
  Lucius `Judgment of Light` (60%) and `Chrono Rings` (80%, three jumpable rings).
* Tests: the table tests pin the old indices and the appended moves; `PhaseRefillTest`, `FxMathTest` are new.
