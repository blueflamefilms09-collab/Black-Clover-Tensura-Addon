# Scale Magic (Damnatio Kira): the floating balance scale and the suppression aura

Owner request (Objective 2 of the casting brief): do NOT add skills or spell trees; rework HOW the existing Scale Magic is executed. Two parts:
(1) a persistent "Scale", an off-hand style floating 3D balance scale beside the caster that actively measures the opponent's magicules and existence points; (2) a passive suppression
aura that scales an opponent's attack damage and magic output down by the weight the Scale calculated.

Nothing here has been run in a game (see "Not verified"). Everything below was built and checked from files: a type-check against API stubs, pure JUnit tests, source-scan pins and a headless
re-implementation of the renderer (`tools/scalecast/preview_scale.py`, PNGs in `docs/previews/scale_casting/`).

## What it is now

* **One Scale per caster**, a real 3D model (`scalecast.ScaleMesh`, prop kind `SCALE_BALANCE`): a golden turned beam with swells and end knobs, a hook ring under each end, a fluted stand with a foot and a
  gauge (nine ticks and a needle that turns with the beam), a pivot drum with a faceted teal jewel (the mod's Scale colour 63BDB5) and a finial ring, two gold pans with a teal scale-pattern enamel inside, three
  chains of crossed link plates per pan hanging from a top ring, and an orb of weight resting in each pan. 1380 quads whole, 330 medium, 74 low. Glow passes through `SafeAdditive` only: the etched runes of the
  beam, the soft glow of the jewel and of each pan's load, a halo behind (twelve spokes and a ring; stronger while the aura is on).
* **It floats at the caster's left** (the off-hand side; the Compass is on the right): 1.15 blocks to the side, 1.45 up, 0.20 ahead, turned 65 degrees toward the caster so the caster reads their own pan on the left
  and the foe's on the right. It follows with an ease, never more than one per player.
* **It appears** when the Scale grimoire is summoned (`GrimoireSummon.isFloating(p, SCALE)`, checked every 10 ticks), when any Scale page is cast (hook in `GrimoireBook.castPage`) and while one is held (hook in
  `GrimoireBook.onHeld`); it stays 12 s (`LINGER` 240 ticks) after the last of those and fades over 14 ticks.
* **It measures.** Every 10 ticks (staggered, throttled by a server-wide allowance) the server weighs the caster and the foes in reach; the beam tilts toward the heavier side, the two pans show the two weights
  as orbs (the heavier side's orb is full size, the other's is its share) and a small readout floats above the Scale (`You 5.0M | Zombie 40K`, `Pressed down: -35% blows`); the local player's own Scale also shows
  a plate over the hotbar. A thin line of light runs from the jewel to the weighed foe and a seal ring turns under its feet while the aura presses it.
* **The aura** is passive: while the Scale is out or the grimoire floats it presses down the nearest foes in reach (at most 8 per caster): their blows at the caster (or the caster's pets and team) lose a share
  by the curve below, at most 35 percent (half for a boss); a foe that is a spell caster has its spell power cut by up to 30 percent through the mod's spell power multiplier.
* **A cast is tied to the Scale**: every Scale page cast starts the weighing animation (the beam swings and rings down, the pans bob), and Scale Volley's five scales now leave the Scale instead of the caster's
  eyes (aimed at the crosshair point, so the volley hits what it hit before).
* **Server switch** `magic.scaleCasting` (default true). False is the old execution: no Scale, no aura, no weighing, Scale Volley starts at the eyes as before.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `scalecast/ScaleCastRules` (pure maths), `ScaleMesh` (the model), `ScaleLod` (pure levels of detail), `ScaleCastKit` (server: one Scale per caster, the weighing, the aura table, the hooks, caps, clean-ups), `ScaleCastConfig` (server), `ScaleVisualConfig` (client), `ScaleCastSelfTests`; `client/scalecast/ScaleRender`, `ScaleFrame`, `ScaleHud`; `client/prop/ScaleBalancePainter`; prop kind `SCALE_BALANCE` (appended last); textures `entity/scalecast/*` (7), `tools/scalecast/*` (texture generator, preview, mesh dump); lang fragment `tools/lang/scale_casting.json`; previews in `docs/previews/scale_casting/`. |
| REPLACED | Only where Scale Volley's bolts start: the Scale instead of the caster's eyes (same direction to the crosshair point, same speed, damage, radius, count, spread). With `magic.scaleCasting = false` it is the old start again. |
| KEPT | Every Scale page (ids `attribute_bolt`, `attribute_cut`, `attribute_field`, `attribute_domain`, `attribute_guard`, `scale_volley`, `scale_serpent`, `scale_shield`, `scale_mail`, `scale_tide`, their order, costs, cooldowns, damage, durations, masteries), the serpent and the shield props (`SCALE_1`, `SCALE_2`), the armour hook, `ScaleLayer` and the flat Scale effects (`SCALE_FX1..3`), `MagicType.SCALE`, the Scale grimoire and its skill. A source-scan test pins the page ids, their order and the numbers. |

Shared files edited (all additions): `prop/PropKind` (one constant at the end), `prop/ScaleProps` (three lines), `book/GrimoireBook` (two hook lines), `mana/ManaLaw.powerMult` (one line),
`book/ScaleSpells.volley` (the start point), `NUSMP` (the server config line and the client HUD init line), `client/prop/PropPainterRegistry` (one line), `tools/safe_additive/scan_textures.py` and
`SafeAdditiveTextureScanTest` (the new texture folder joins the scan scope), `PerfSourceScanTest` (the new packages join the throttle pin).

## How the Scale measures (logic flow)

```
every server tick, every player:                       (ScaleCastKit.tick0)
    st = STATES.get(uuid)                              one map lookup
    if st == null and tickCount % 10 != 3: return      a player without a Scale costs only that
    if !magic.scaleCasting: forget(uuid); return
    if tickCount % 10 == 3 and GrimoireSummon.isFloating(p, SCALE):   the grimoire is bound / selected
        if !st.wasFloating: show(p, 240)               the Scale appears, a chime and sparks
        st.auraUntil = max(st.auraUntil, now + 30)
    if dimension changed or player dead: forget; return
    if now < st.auraUntil:
        if now - st.lastScan >= 10 and SCANS.tryTake(now, 1):      server-wide allowance, 6 scans per tick
            scan(p, st, now)
    else if st holds foes: release(...)                           the entries are dropped at once
    if the Scale and the aura are over: forget

scan(p, st, now):                                      (one entity scan, radius 12 by default, 4..16)
    casterQ = quantize(weightOf(p))
    near = level.getEntitiesOfClass(LivingEntity, box(radius), isFoe)      the only scan in the kit
    keep the 8 nearest in two preallocated arrays (insertion, no sort, no allocation)
    weighed = the foe weighed 40 ticks ago while still in reach, else the foe under the crosshair if it is
              one of the 8, else the nearest                          (so the beam does not flicker)
    for each kept foe f:
        foeQ     = quantize(weightOf(f))
        d        = (foeQ - casterQ) / 20                              decades; positive = the foe is heavier
        red      = reduction(d, isBoss(f), damageCap)                  <= 0.35, half for a boss
        magicRed = magicReduction(red, magicCap)                       <= 0.30
        entry    = SUPP[f.uuid]; take over only if free, mine, expired or red is STRONGER   (never added up)
        entry.until = now + 24
    foes that left the radius: their entries are dropped if this caster holds them
    st.hasFoe / st.foe / st.foeQ / st.percent = what the beam shows
```

**The weight** (`ScaleCastKit.weightOf`, `ScaleCastRules.weight`) uses the same Tensura reads as the rest of the repo, nothing new:

* `EnergyHelper.getMaxEP(e)`: Tensura's existence points (the same value `BalanceLaw.epScale` and `MirrorWorks.power` take their logarithm of). It is the body of the weight.
* `TensuraStorages.getExistenceFrom(e).getMagicule()` over `EnergyHelper.getMaxMagicule(e)`: the loaded share of the magicule pool (the same two reads `EnergyBridge` and `GrimoireBook.onTakenDamage` use).
* `weight = EP * (0.5 + 0.5 * loadedShare)`: a creature that has spent its pool weighs half of what it does at full. A creature with no EP data falls back to ten times its max health.
* `BalanceLaw.epScale` is deliberately not used: it is a damage multiplier clamped to 1..12, which would squash every foe above 10M EP to the same weight; the Scale needs the raw ratio.
* `BalanceLaw.isBoss` marks bosses (half strength). `ManaLaw.powerMult` is the spell power hook.

The weight travels as a log code (20 steps per power of ten, 0..255, within 12 percent of the weight): `q = round(20 * log10(weight))`. Differences are read in **decades** (powers of ten).

**The beam** (client, from the synced word; `ScaleCastRules.tilt`): `tilt = 24 * tanh(decades / 1.4)` degrees, positive = the foe's pan down; about 8 degrees at half a decade, 15 at one, 21 at two. No foe weighed: level.

## How the aura scales damage and magic output (logic flow)

```
curve:    strength(d) = smoothstep(clamp((0.5 - d) / 2.0, 0, 1))     full at d <= -1.5 (foe 30x lighter), none at d >= +0.5
reduction(d, boss, cap)  = cap * strength(d) * (boss ? 0.5 : 1)      cap = min(config, 0.35);  < 0.02  ->  0 (dead zone)
magicReduction(r, cap)   = min(0.85 * r, min(config, 0.30));         < 0.02  ->  0
apply(amount, r)         = amount                                    if amount <= 1.0         (a hit of one point or less is never touched)
                         = max(1.0, amount * (1 - min(r, 0.35)))     otherwise                (never below the floor, never more than the cap)

damage event  (LivingIncomingDamageEvent, shared hook list AttributeEvents.incoming):
    if SUPP is empty: return                                        one isEmpty per hit on an idle server
    att = source.getEntity(); en = SUPP.get(att.uuid)               one hash lookup
    skip if: no entry, expired, BYPASSES_INVULNERABILITY (kill command, void), or a spell reduced this very tick
    skip unless the victim is the entry's caster, their pet or their team (victim.isAlliedTo(caster))
    event.setAmount(apply(event.getAmount(), en.red))                a share of the BLOW, never a percent of anyone's health; never kills

spell power   (ManaLaw.powerMult, once per spell damage a player computes):
    m *= ScaleCastKit.magicMult(p)       = 1 - en.magicRed  (>= 0.70)   stamps en.magicStamp = now, so the damage event does not cut the same hit twice
```

| foe weight relative to the caster | decades | beam | blows | spell power | boss blows |
|---|---|---|---|---|---|
| 30 times lighter or less | -1.5 | 19 deg to the caster's side | -35% | -29.75% (cap 30) | -17.5% |
| 10 times lighter | -1.0 | 14.7 | -29.5% | -25.1% | -14.8% |
| 3 times lighter | -0.5 | 8.2 | -17.5% | -14.9% | -8.8% |
| equal | 0 | level | -5.5% | -4.7% | -2.7% |
| 1.8 times heavier | +0.25 | 4.2 to the foe's side | 0 | 0 | 0 |
| 3 times heavier or more | +0.5 | 8.2 | 0 | 0 | 0 |

Hard limits (all pinned by tests, whatever a config, a caller or a datapack passes): a foe's blows lose at most 35 percent (17.5 for a boss), its spell power at most 30 percent, never below one hit point for a
hit bigger than that, never an increase, never a kill, never a percent of anyone's max health; at most 8 foes per caster; at most 32 Scales (and so 256 table entries) on the server; one entry per foe (the
strongest caster holds it); entries lapse 24 ticks after the last scan that renewed them (the scans come every 10); the aura radius is 4..16 blocks.
**Cached per tick:** the reductions are computed at scan time (every 10 ticks) and stored; the hit hook and the spell hook only read them (a hash lookup, no allocation, no recomputation).

## Server state and clean-up (rule 9)

* All decisions are on the server (`ScaleCastKit`, `ScaleCastRules`); the client draws the synced prop word and asks nothing. No new packet: the Scale is the existing `MagicPropEntity` (prop kind appended last),
  tracked to nearby players by the existing entity tracking (12 chunks); the data word is written only when it changes and the measured numbers at most every 10 ticks.
* Per-player state is keyed by player UUID (`STATES`, `SUPP`), touched on the server thread only, no static "current player", no `Minecraft.getInstance()`, no client class in common code (pinned, and
  `ServerSafetyTest` passes without a new allow-list entry).
* Cleaned on logout, death, respawn, dimension change (the tick also notices a changed dimension), the config turned off and server stop; expired aura entries are pruned every 100 ticks.
* The gameplay switch is a SERVER config: `serverconfig/nusmp-scale-casting-server.toml` (`magic.scaleCasting` default true, `magic.scaleAuraRadius` 12, `magic.scaleDamageCap` 0.35, `magic.scaleMagicCap` 0.30:
  the two caps can only be lowered). The looks are a CLIENT config: `config/nusmp-scale-casting-client.toml` (`visual.scale3d` default true: false draws none of the 3D Scale and the aura still works;
  `visual.scaleReadout` default true). Unique file names (`ConfigFileNamesTest`).
* CI server smoke test: `magic.scale_casting` (pure checks of the caps and floors over a range of decades and amounts, the prop word, the mesh; then on a real level a stand-in caster gets exactly one Scale
  however often it is asked, twelve zombies in reach are pressed by at most 8 and by no more than 0.35, everything is gone after `forget`).

## Animation and visual sync notes

* **Beam tilt and easing.** The client decodes the two weight codes from the synced word and drives the beam with a damped spring (`ScaleCastRules.spring`, stiffness 0.30, damping 0.42 per tick: settles in
  about a second, one overshoot of about a quarter of the step, stable for frames up to 2.5 ticks long) toward `-tilt(casterQ, foeQ)` (negative because the foe's pan is on the right and a clockwise turn lowers
  it). A new foe therefore makes the beam swing and settle; with no foe the beam rests level with a 2 degree sway; with a foe a 0.8 degree ripple keeps it alive. It first swings in from a 12 degree offset when it appears.
* **Pans.** The pans hang level from the hook rings whatever the beam does (their positions are the rotated hook points minus the chain length), bob with the weighing animation and breathe by 0.012 blocks.
  The orb in each pan is sized by that side's share of the heavier weight (`ScaleCastRules.load`: 0.15..1; the foe's pan is empty with no foe) and glows in its colour (teal for the caster, ember for the foe).
* **Weighing animation tied to the cast tick.** The prop word carries a 3-bit pulse counter the server increments inside `ScaleCastKit.onCast`, which `GrimoireBook.castPage` calls after the page's cast ran, i.e. on the
  server tick the cast really happened (after the wind-up), not at the key press. The painter starts the animation when it sees the counter change (the first word it ever sees starts nothing): the beam gets
  a swing `17 * e^(-0.14 t) * sin(0.52 t)` degrees (alternating sign per cast), the pans bob `0.06 * e^(-0.16 t) * sin(0.9 t)` blocks, the glow flares for 20 ticks; all over by 36 ticks. Volley's bolts leave on the
  same tick as the pulse; the hold keeps the Scale out but starts no animation.
* **Fade.** The server sets a `fading` bit 14 ticks before the Scale ends and the client fades by its own clock from the moment it sees it (no per-tick data); it pops in over 10 ticks with an overshoot.
* **Orientation.** Prop yaw = caster body yaw + 65 degrees, front toward the caster's face; from the front the Scale is seen at an angle, from behind it is seen the way the caster sees it. Both sides are
  drawn (no culling); the gauge and the jewel are on the front.
* **Readout.** Two lines of billboard text above the Scale within 14 blocks (the strings are rebuilt only when the word or the foe changes), and for the local player's own Scale a plate over the hotbar
  that lapses by itself if the painter stops refreshing it (6 ticks).

## Worst-case cost (rule 11)

| | bound |
|---|---|
| Entities | at most one Scale prop per player, at most 32 on the whole server (the 33rd caster has no Scale and no aura); nothing else is spawned |
| Server work per tick | one hash lookup per player per tick; one `isFloating` check per player every 10 ticks; at most **6 aura scans per tick** server-wide (a shared allowance), each scan one entity query in a box of at most 16 blocks radius, one ray cast for the look target and at most 9 weight reads; with 32 Scales that is at most 64 scans per second (the allowance would allow 120); at most 256 table entries |
| Hit hook | one `isEmpty` per hit while nobody has an aura; with entries one hash lookup per hit with a living attacker; no scan, no allocation |
| Spell power hook | one `isEmpty` per computed spell damage, else one hash lookup |
| Packets | no new packet type. Per Scale: 1 spawn per watcher, 1 remove, the data word (a few per second at most: events at once, measured numbers every 10 ticks), and position updates only while the owner moves, every second tick, so at most **10 per second per watcher** (0 when standing still), to players tracking within 12 chunks. Particles (8 on appearing, 6 on ending) and the chime go through `Fx` and its budgets |
| Client quads per frame | one Scale: 1380 whole + 7 cards (within 16 blocks), 330 (within 32), 74 (within 48), nothing beyond or when wholly behind the camera; **all Scales together are charged to one frame budget** of a tenth of the effect quality's vertex budget (4000 quads HIGH, 1400 MEDIUM, 600 LOW): a Scale that does not fit drops a level, one within 5 blocks is never lost; so on HIGH at most 2 whole Scales and the rest medium or low |
| Client per frame | no allocation (pinned by `ScaleCastingScanTest`); about 9 render type switches for a whole Scale (gold, enamel, chain, gem, runes, soft glow, halo, ring, line), 7 medium; text drawn only within 14 blocks and only while a foe is weighed |
| Config off | no Scale, no state: the player hook returns at once for everyone |

## Where things are

* Model: `src/main/java/com/newuniverse/nusmp/scalecast/ScaleMesh.java` (plain geometry, no Minecraft class; quads of 23 floats: four corners x y z u v, then the normal; layers GOLD, ENAMEL, CHAIN, GEM, RUNES).
* Textures: `python3 -B tools/scalecast/gen_scale_textures.py` writes `assets/nusmp/textures/entity/scalecast/{gold,enamel,chain,gem,glow_runes,glow_halo,glow_ring}.png` (deterministic). The three `glow_*` are additive
  and in the scope of `tools/safe_additive/scan_textures.py` (`--fix` found nothing to fix).
* Preview without the game: `python3 -B tools/scalecast/preview_scale.py` (compiles `ScaleMesh` and `ScaleCastRules` with `tools/scalecast/ScaleMeshDump.java`, rasterises the real quads with the z-buffer of the
  compass preview; the tilts and reductions in the images come from the real rules) writes `docs/previews/scale_casting/`: `scale_views.png` (idle: front, three quarter, side, back), `scale_weighing.png`
  (foe heavier, equal, foe lighter: the beam tilts, the orbs resize, the numbers), `scale_levels.png` (the three levels with their quad counts), `scale_caster.png` (beside a caster; as the caster sees it),
  `scale_aura.png` (the suppression aura: four foes of different weight, the line of the weighing, the rings under the pressed ones and the real percentages).
* Server: `scalecast/ScaleCastKit` (behaviour registered by `prop/ScaleProps.init()`), rules `scalecast/ScaleCastRules`. Client: `client/prop/ScaleBalancePainter`, `client/scalecast/ScaleRender`, `ScaleFrame`, `ScaleHud`.

## Tests

`scalecast/ScaleCastRulesTest` (weight, log code, tilt, spring, weighing swing, the curve, the caps and floors over wide ranges of decades, amounts and hostile inputs, the prop word, the send throttle, place, fades,
text), `ScaleMeshTest` (well formed, levels and budgets, symmetric beam, enamel faces up, chains from the top ring to the lugs, the unit orb), `ScaleLodTest` (bands, quality, behind cull, frame budget, worst case),
`ScaleCastAssetsTest` (textures and sizes, no colour under alpha 0, card borders fade out, gold is gold and the enamel teal, the renderer names only existing textures, the lang fragment, the previews exist),
`perf/ScaleCastingScanTest` (source-scan pins: level of detail before drawing, no allocation per frame in the painter, the renderer and the frame helper, the scan throttle, the global allowance, the caps, one entity scan
in the whole kit, no allocation and no scan in the hit and spell hooks, the strongest-wins rule, clean-ups, UUID keys, no client class in common code, server config / client config, the shared hooks, `SCALE_BALANCE`
last, the five page ids, their order and numbers). The existing `ServerSafetyTest`, `ConfigFileNamesTest`, `FxSourceScanTest`, `PerfSourceScanTest`, `SafeAdditiveSourceScanTest`,
`SafeAdditiveTextureScanTest`, `ScreenBlurGuardTest`, `SquareBugAssetsTest`, `NoHungerDrainTest` still pass in the shim.

## Canon and sources (rule 10)

* The owner's brief (Objective 2): an off-hand or persistent "Scale" that actively measures the magicules and mana of the opponent; a passive suppression aura scaling an opponent's damage or magic output by the weight.
  That is the whole spec; the numbers are this mod's.
* English wiki (blackclover.fandom.com, Scale Magic and Damnatio Kira pages): **could not be fetched** (the host does not resolve from the build environment). Japanese fallback (ja.wikipedia.org): **could not be
  fetched either**. Nothing was taken from either. The only canon on record is `docs/attributes/scale_rebuild.md` (written for 1.93.0, also without wiki access): "the repo's wiki extract has no page for Scale Magic".
  The balance scale itself, the pans, the weighing and the suppression are the owner's design for this mod, not a wiki-sourced list of spells; no spell was added.

## Not verified (nothing could be run)

* **No frame of the game was seen.** The preview PNGs are a re-implementation of the renderer (z-buffer, nearest sampling, simple lighting, additive and alpha blends); real lighting, blending order, entity render
  types with the custom textures, depth behaviour of the glow offsets and the translucent fade can differ. The type-check is against API stubs; the client calls (`Font.drawInBatch`, `Camera.rotation`,
  `GuiGraphics`) were checked only that way.
* **The server behaviour is reasoned, not run**: the CI self-test `magic.scale_casting` is written but has not run. Whether `EnergyHelper.getMaxEP` gives a sensible weight for every kind of vanilla and Tensura
  mob (and a FakePlayer) was not seen; if it reads 0 the fallback is ten times max health. The numbers of the curve (full at 30 times lighter, none at 3 times heavier) are a design choice that was not
  playtested; the three server config values exist so an owner can soften it.
* **Which foes count** (`isFoe`: `Enemy` mobs, players when PvP is on and not creative, and any mob currently targeting the caster or their team) is a judgement; neutral or passive mobs that are not hunting the
  caster are not pressed, tamed mobs of the caster and their team are protected, not pressed.
* **The aura protects only the caster, their pets and their team** (`victim.isAlliedTo(caster)`): a suppressed foe's blow at somebody else is not reduced. Its spell power is reduced wherever it lands, because the
  spell power hook does not know the target.
* **The aura is on while the Scale is out or the grimoire floats**, not forever: unsummoning the grimoire ends it about 1.5 s later; casting without the grimoire out is not possible anyway.
* **First person:** the Scale floats about 1.15 blocks to the left and 0.2 ahead; it is only in view when the caster turns toward it. Whether it is in the way or readable was not checked.
* **Readout text** is drawn with `Font.drawInBatch` in the entity pose; its size, depth testing and legibility against bright skies were not seen. The plate over the hotbar sits 80 px above the bottom and may overlap
  other overlays (the Song meter sits at 58).
* **Position updates:** the Scale moves every second tick and the client is expected to smooth between updates (the tracker's usual interpolation); a visible stutter while sprinting was not ruled out.
* **Dimension change** ends the Scale and the aura; the next cast or summon makes a new one. A Scale of a caster who dies while the grimoire floats is dropped at once and comes back when the grimoire is summoned again.
* **Other builders:** the four Fire characters' casting (book/FireBook*, Flame*, Magna's bat) was not touched; the shared hook lines (`GrimoireBook`, `ManaLaw`, `PropKind`) are one-line additions that merge trivially.
