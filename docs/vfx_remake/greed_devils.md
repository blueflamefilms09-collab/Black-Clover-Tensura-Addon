# Black Sword Greed and the devil hits, wave 2

The owner asked for every skill's effects to look like the anime it comes from instead of particle sprays. Wave 1 did the Saiyan Ki techniques
(`ki_dragonball.md`). This wave covers two systems:

- **The Black Sword Greed** (`greed/`, the owner calls it "Berserk of Gluttony"): eight weapon forms, each with a Secret Technique. Until now
  every one of them was a vanilla dust spray (`PerfParticles` with `DustParticleOptions`) or an `Fx.spray(DARK)`.
- **The devils** (`devil/`, eight worlds): the rituals already had shaped effects. The devil's **combat hit** (`DevilCombat`) was a single
  `Fx.sprayTinted` mote spray in the devil's colour.

## What was built

- **`vfx/client/layer/GreedLayer.java`**: one layer with 10 new shapes, appended at the end of `VfxShape` after `KI_TRANSFER`:
  `GREED_SLASH, GREED_LANCE, GREED_FLAME, GREED_THREADS, GREED_REAP, GREED_DOME, GREED_MAW, GREED_LIGHTNING, GREED_AWAKEN, DEVIL_HIT`.
  It is registered last in `VfxManager`. `DEVIL_HIT` is drawn by its helper **`GreedDevilHits.java`** (one look per devil world).
- **`tools/gen_greed_vfx_textures.py`**: makes 19 textures, `textures/particle/greed_*.png`. They are deterministic, use no fonts, and are white or
  grey so the vertex colour tints them: core, glow, void, slash, bolt, streak, ring, scales, scale, teeth, glint, flame, feather, pentagram,
  crack, runes, brand, blood, flash.
- **`vfx/client/layer/GreedVfx.java`**: a pure class holding the texture list, the devil-world list and the two classic switches.
- **`greed/GreedFx.java`**: the server helper. Every Greed effect goes out through it, as one `VfxSpawn` packet. It also holds the `greed.vfx` self-test.
- **`devil/DevilHitFx.java`**: the server helper for the devil hit, and the `devil.vfx` self-test.
- **`vfx/client/GreedVfxConfig.java`**: the client settings (see below).
- **Preview scenes**: `tools/vfx_preview/scenes.d/greed.json` (17 scenes) and `devil.json` (8 scenes). The renders are in
  `docs/vfx_remake/previews/greed_*.png` and `devil_*.png`, with a contact sheet in `greed_devil_sheet.png`.

### The look (how to read it)

Greed is ink-black with a thin crimson edge. The black parts are drawn with the alpha blend in a near-black ink, so they stay black on a bright
sky (an additive black would vanish). The crimson parts are drawn with the alpha blend too, so the red stays saturated, with a faint additive glow
on top where light is wanted. The pieces:

- **Ink crescents**: a black blade arc with a blood-red rim on its cutting edge. Used for every cut.
- **Black lightning**: a jagged ink core in a red glow. The zigzag is re-rolled every 2 ticks, which gives the anime flicker.
- **The hungry maw**: a black void with a torn rim, ringed with fangs, two red eyes over it.
- **The dome of black scales**: fish-scale rows bent round a ball, with a strong rim.
- **Red eye glints**: two thin red flares where the wielder's eyes are.
- **Black flames**: ink flame tongues with a crimson heart.

The Time Magic layer is the quality standard. Its techniques were copied, not its code: life ramps, eased opening, hoops (ribbons on a circle)
and seeded randomness, so every client sees the same picture. The Time Magic files were not touched.

## Sources (rule 10)

The English wikis (`berserk-of-gluttony.fandom.com`, `chainsaw-man.fandom.com`) could not be reached from this session: the network did not resolve
them, so the Japanese fallback was not tried either (the session's network is the blocker, not the page). Every reference below comes from general
knowledge of the anime and **none of it is wiki-verified**. The least certain entries are marked. If you want them checked, allow the fandom hosts
(and `ja.wikipedia.org`) in the session network settings.

## Greed: per form and technique

| Form / technique (file) | Anime reference (unverified) | Shape and what it draws | Packets per cast |
|---|---|---|---|
| Black Sword: Bloody Ptarmigan Cross (`GreedForms.ptarmiganCross`) | Fate's crossed slash with the black sword; a cross of red-black cuts thrown forward | `GREED_SLASH` from the eyes to 14 blocks ahead, power 2: two crossed ink crescents with a crimson rim fly out, growing, a crimson streak behind them, a flash where they land | 1 |
| Black Bow: the drawn arrow (`GreedForms.bolt`) | The bow form's arrow of magic | `GREED_LANCE` power 0.3: a black-red streak flies to the foe or 48 blocks ahead with a glowing head; an ink splash, a flash and a ring where it ends | 1 |
| Black Bow: Piercing Meteor (`piercingMeteor`) | The bow's strongest shot, a meteor of dark power (name from the mod, look unverified) | `GREED_LANCE` power 0.7: a beam that shoots out in 3 ticks, crimson glow, ink core, black lightning coiling round it | 1 |
| Black Scythe: Soul Reap (`soulReap`) | The scythe form reaping souls | `GREED_REAP` round the wielder, radius 4.5: a huge ink crescent sweeps a full circle in 7 ticks, a crimson ring runs out, souls are torn from the circle and spiral in | 1 |
| Black Shield: a reflected blow (`shieldTook`) | The shield form | `GREED_DOME` power 1.6: the dome of black scales flashes up round the holder | 1, at most once every half second (the existing reflection gate) |
| Black Shield: Reflection Fortress (`reflectionFortress`) | Releasing what the shield took (look unverified) | `GREED_DOME` power 18 (10 + radius 8): the shell forms, shatters into scale shards that fly out to 8 blocks, shock rings, camera shake | 1 |
| Black Staff: Black Flames (`flames`) | Black fire from the staff | `GREED_FLAME` from the hand 14 blocks ahead: ink flame tongues with crimson hearts roll out in a cone | 1 |
| Black Staff: Twilight Healing (`twilightHealing`) | The staff's healing (look unverified) | `GREED_FLAME` with from = to, power 8, dusk colour: a ring of black flames at 8 blocks, a column of light, motes rising | 1 (not one per ally) |
| Black Gauntlets: Spatial Threads (`threads`) | Threads that cut space | `GREED_THREADS` power = creatures hit (at least 3): taut crimson threads shoot out in 3 ticks and snap at the end | 1 |
| Black Gauntlets: Dimension Destruction (`dimensionDestruction`) | Tearing space apart | `GREED_LIGHTNING` from the hand 10 blocks ahead: three strands of black lightning, a void rift torn at the end | 1 |
| Black Spear: the thrust (`thrust`) | The spear's piercing thrust | `GREED_LANCE` power 0.35, 8.5 blocks | 1 |
| Black Spear: Revolt Brionac (`revoltBrionac`) | The spear's lance of destruction | `GREED_LANCE` power 1.2, 30 blocks: a heavy beam with black lightning, camera shake | 1 |
| Dual Blades, Level Zero: the blink (`blink`) | Two cuts from behind the foe | `GREED_SLASH` from the wielder's new spot to the foe, power 2: an X of ink crescents on the foe | 1 |
| Dual Blades: Infinity Divide (`infinityDivide`) | Countless cuts all round | `GREED_SLASH` with from = to, power 12: a storm of 12 cuts round the wielder, one every 1.4 ticks, a crimson ring on the ground | 1 |
| Devouring a kill (`GreedService.onKill`) | Gluttony: the sword devours the souls of the slain (the core of the series) | `GREED_MAW` at the victim: a black void opens ringed with fangs and two red eyes, souls stream to the wielder, the jaws close | 1, at most one every half second (a technique that kills eight sends one) |
| Form switch (`GreedService.switchForm`) | Greed waking when it changes form; Fate's red eyes when the hunger rises | `GREED_AWAKEN` on the wielder: black lightning over the body, two red eye glints, black flames rise, an aura ring | 1, at most one every 2 seconds (the existing switch gate) |
| A new tier (`GreedService.checkTier`) | | `GREED_AWAKEN` power 9: the same, bigger, camera shake | 1 when a tier opens |

Not in this wave: the scythe's Soul Sever on a plain hit (a melee hit per swing would be a packet per target), the starvation drip
(`GreedService.starve`), the HUD vignette. Those keep their old look.

## The devil hits: per world

`DevilCombat` already applies the attribute effect at most once a second per host (`DevilRules.effectReady`). Right after it, `DevilHitFx.hit`
sends ONE `DEVIL_HIT` packet: from = the target's chest, to = the attacker's chest, power = the world (ordinal + 1), colour = the devil's own.
The signature piece keeps its world's colour; the devil's colour is the accent (the flash, a ring).

| World | Anime reference (unverified) | What DEVIL_HIT draws |
|---|---|---|
| Black Clover | The devils of the underworld tearing through into the world | A crack in space breaks open over the target, ink with a black-red glow, the devil's ring round it |
| Chainsaw Man | Chainsaw rips and sprays of blood | A saw-tooth rip slashes across with orange sparks, blood drops arc out and fall |
| Black Butler | Sebastian's contract seal (a pentagram) and his black feathers | The contract pentagram glows violet over the target, black feathers burst out and drift down |
| Blue Exorcist | The blue flames of Satan | Blue flames leap up round the target, a ring of blue fire on the ground |
| Tensura | The demonic aura of daemons | A demonic aura pulses out in purple waves from a dark core, motes rise |
| Dororo | Onibi, the pale ghost fires of the demons (least certain) | Pale ghost fires circle the target, bobbing |
| Berserk | The Brand of Sacrifice bleeding near a demon; the Eclipse | The Brand bleeds red on the target, a black eclipse with a red corona hangs over it |
| Fairy Tail | The Etherious and their curses (look unverified) | Two bands of curse runes turn round the target, a dark curse crack under them |

The pentagram, the Brand and the runes are drawn from lines in the generator (no font, no copied artwork).

## The classic settings

The settings are in `config/nusmp-greed-devil-vfx-client.toml`. They are CLIENT settings (looks only) and both default to `false`:

- `greed.classicVfx`: `true` makes the nine `GREED_*` shapes draw nothing.
- `devil.classicVfx`: `true` makes `DEVIL_HIT` draw nothing.

The server always sends the new shapes, and it still sends the old sprays exactly as before; nothing on the server was removed. With a classic
setting on, the player sees exactly the 3.96.0 look for that system. The values are mirrored into `GreedVfx` when the file loads or reloads (the
`HerculesModelsConfig` pattern). `ConfigFileNamesTest` sees the literal file name.

## Cost (rule 11)

**Server**

- No entity, no block change, no new query, no new map. One `static long` (the time of the last maw; the sword has one wielder per world).
- Each cast, kill, switch or reflected blow adds 1 effect packet (about 60 bytes) through `VfxSpawn.send`: the per-tick effect budget, players within 96 blocks only.
- Worst case per player: the Greed wielder is one player per world. Their casts are gated by the form cooldowns and the Secret Technique gap; the maw by
  half a second; the dome flash by the half-second reflection gate; the awakening by the 2-second switch gap. A devil host sends at most 1 packet a second.
- Nothing is ever sent per tick or per target.

**Client**

- Each effect is one activation under the engine's 400-vertex (100-quad) cap; the layer keeps the default cap.
- The highest vertex count the preview measured at full detail, per shape:

  | Shape | Vertices (quads) |
  |---|---|
  | GREED_SLASH | 320 (80, the Infinity Divide storm) |
  | GREED_LANCE | 312 (78, Piercing Meteor / Brionac) |
  | GREED_FLAME | 260 (65, Twilight Healing) |
  | GREED_THREADS | 96 (24) |
  | GREED_REAP | 296 (74) |
  | GREED_DOME | 248 (62, Reflection Fortress) |
  | GREED_MAW | 180 (45) |
  | GREED_LIGHTNING | 260 (65) |
  | GREED_AWAKEN | 296 (74, a tier) |
  | DEVIL_HIT | 184 (46, Tensura); Chainsaw Man 168, Fairy Tail 144, Blue Exorcist 140, the rest under 90 |

- Every arc, ring, hoop and beam checks `hasBudget`, so the cap is never passed. `ctx.seg` thins every loop by distance and the particle setting.
- The manager's existing caps apply: `maxActive` per quality level, distance level of detail, culling, the per-frame vertex allowance.
- Textures are static final fields, so no `ResourceLocation` is built per frame (`PerfSourceScanTest` lists `GreedLayer` and `GreedDevilHits`).
  No state is kept between frames (only short-lived pose vectors, as in every layer), so nothing needs cleaning up on logout, death or dimension change.
- A followed effect whose creature is gone draws nothing.

## NEW / REPLACED / KEPT

**NEW**

- `GreedLayer`, `GreedDevilHits`, `GreedVfx`, `GreedVfxConfig`, `GreedFx`, `DevilHitFx`
- the 10 shapes and the 19 `greed_*` textures, with their generator
- the preview scenes and the preview PNGs
- the client settings `greed.classicVfx` and `devil.classicVfx`
- the self-tests `greed.vfx` and `devil.vfx` (added to `SMOKE_EXPECT` right after `greed`)
- the test `GreedVfxAssetTest`; `PerfSourceScanTest` lists the two new layer files

**REPLACED**: nothing was removed. The new shapes are a new look on top of the old sprays. The spray-only look is still there, behind the classic settings.

**KEPT**

- every old `Fx` / `PerfParticles` spray in `GreedForms`, `GreedService` and `DevilCombat` (now the accent under the new shapes)
- every old shape, texture and layer, and the devil ritual effects
- the Time Magic files

## Caveats

- None of this has been seen in the game. It has only been rendered by the headless preview, which has no world lighting and no depth against blocks.
- Gradle could not be run here, so the Minecraft-side code (the server helpers, the wiring, the config) was checked only by reading it. `GreedLayer` and
  `GreedDevilHits` were compiled by the preview against stubs.
- The wielder's eyes in `GREED_AWAKEN` use the body yaw; while the head turns away from the body they can sit a little off the face.
- `KiVfxAssetTest` still pins the Ki shapes after `NEN_DOMAIN`; `GreedVfxAssetTest` now pins `DEVIL_HIT` as the last shape (a later family moves that pin).
