# Mercury Magic: the 1.93.0 rebuild (`MERCURY`, Nozel Silva and the Silver Eagles)

Everything in this file was written without being able to run the game. What was type-checked, tested and looked at, and what was not, is in section 9.

## 1. NEW / REPLACED / KEPT

| Part | Status | Notes |
| --- | --- | --- |
| Temporary armour "the Silver Guardian": `client/aura/MercuryAura` (was a stub, `Aura.MERCURY` existed), 11 geo pieces, the coat and sheen skins | NEW (stub filled) | |
| Constructs `mercury_blade`, `mercury_eagle`, `mercury_spear`, `mercury_cage` (+ 11 armour models); props `MERCURY_1`, `MERCURY_2` (existing ids, were stubs), `MERCURY_3` (appended) | NEW | |
| VFX `MERCURY_FX1..3` (existing shapes, were a one-glow stub), `MercuryLayer` rewritten, 6 textures | REPLACED | `MERCURY_DOME`, `_SPEAR`, `_EAGLE` and `SlashCompassMercuryLayer` KEPT registered (the Rain's cloud still uses the dome) |
| Pages `silver_blade`, `mercury_shield`, `mercury_rain`, `silver_eagle` (ids, order, tiers, the Eagle's 600 cooldown) | KEPT ids | casts re-implemented in `book/MercuryBook` (blades as bodies, the Guardian as armour with the toxin, the Rain with a pool, the Eagle as a carried flight) |
| Pages `silver_bullet`, `silver_spear`, `silver_shining_regal_spear`, `silver_star_of_execution` | NEW | appended through `book/ext/MercuryExt` (was an empty stub), casts in `book/MercuryArts` |
| `book/MercuryBook.java` | EDITED in place (own magic) | `spear()` is now a thin call to `MercuryArts.lance`; `blade`, `shield`, `silverRain`, `silverEagle` rewritten, the page list uses `MercuryRules` ids, `SILVER` is now 0xFFC8E8FF |
| `MercuryRulesTest` (5 tests), self-test `magic.mercury_rebuild` | NEW | |

## 2. Canon sources

The repo's own wiki pages, read in full: `.github/wiki/mercury_magic.txt` (a derivative of Water and Steel Magic; liquid metal of high density that can take any shape; toxic, so wounds can be fatal and it is effective against Blood Magic; compressed and solidified it is smooth enough to reflect Light Magic; at a disadvantage against Fire) and the spell pages `spell_silver_blade`, `_bullet`, `_spear`, `_rain`, `_guardian`, `_eagle_spell`, `_shining_regal_spear`, `_star_of_execution` (the user is Nozel Silva in the mod; the Silver Eagles' squad name is unrelated to the spell). The Dimension Slash: Scorching Light (a Mercury + Light compound spell) needs two casters and is NOT built. The wiki host itself could not be reached; no page beyond the repo's copies was read.

Mapping of the wiki text: Silver Blade = double-edged sword that floats and moves without contact, several at once (blades as props, three of them); Silver Spear = launched, several at once, melts in extreme heat (fire gets through the Guardian; the spear is a pierce-and-damage spell, melting is not modelled); Silver Bullet = small spheres at a target; Silver Rain = droplets from the sky, the user can keep them off themselves (the rain here never hits the caster: it uses `around`, which excludes the caster and allies); Silver Guardian = a coat over the whole body, solid enough to block, cracks when broken, flexible, harms the user (the armour, the dome, the toxin); Silver Eagle = an eagle that carries the user, large enough for several (the eagle carries the caster; carrying others is NOT built); Silver Shining Regal Spear = a long beam from the palm that follows it and pierces rock (the beam; it breaks barriers and projectiles, never blocks); Silver Star of Execution = strands and drops spread over an area and gathered into a giant cage whose bars switch between spears and shields (the cage; the "impossible in chaotic mana" clause is NOT built).

## 3. The armour: the Silver Guardian

| Piece | Anchor | Delay | Flies in from | What it is |
| --- | --- | --- | --- | --- |
| `mercury_helm` | head | 0 | above | a smooth chrome band, a crest of four tapering slabs swept back, side fins, a bead of mercury that hangs from the brow and drips |
| `mercury_chest` | body | 2 | front | three rounded plates stepping in toward the waist, a collar, a quicksilver core that pulses, a back plate with a ridge |
| `mercury_tasset` | body | 4 | below | a skirt of hanging drops of different lengths with glowing beads (sway) |
| `mercury_pauldron_r/_l` | arms | 3 | outward | four rounded lames and a bead that hangs from the point |
| `mercury_vambrace_r/_l` | arms | 5 | outward and front | chrome bracers with a glowing stud and a double-edged BLADE FIN along the outer forearm (sways) |
| `mercury_greave_r/_l` | legs | 6 | below and front | chrome greaves with a knee stud and a pooled boot wider than the foot |
| `mercury_back` | body | 8 | behind | A PAIR OF EAGLE WINGS, two articulated segments each (a leading edge, covert feathers on the inner bone, long primaries on the outer), idle and a fast flap (the painter uses `flap` while the wearer runs, `limbSwingAmount` above 0.7) |
| `mercury_halo` | head | 12 | above | six drops of two sizes orbiting the head, a cross of two chrome rods |

Passes: pieces solid `entityCutoutNoCull` / translucent while forging, each with its glow map (the quicksilver studs and beads); the coat: the posed model 3 percent larger, translucent (`entityTranslucent`, lit), rippled chrome with a few cracks (`textures/aura/mercury_coat.png`); the sheen: 4.5 percent larger, additive (`RenderType.eyes`, full bright), bands of light (`mercury_sheen.png`). Style 1 (a short armour of five seconds, worn after the Regal Spear and the Star of Execution; the Eagle's lasts 66 ticks) leaves out the wings and the drops. About 6,100 vertices with every glow pass.

## 4. The constructs

| Model | Cubes | Clips | Used by |
| --- | --- | --- | --- |
| `mercury_blade` | 13 | idle (with running drops), spin | Silver Blade's three floating blades (the visual body flying with the damage bolts) |
| `mercury_spear` | 11 | idle (spin) | Silver Spear, the Guardian's spears at its end, the Rain's drops, the cage's strikes: a chrome spear with a liquid tail of tapering drops |
| `mercury_eagle` | 54 | idle, flap, glide, dive, form | the Silver Eagle: body, neck, hooked beak, glowing eyes, a fanned tail, talons, two 2-segment wings of 5 and 6 feathers each; the prop stays under the caster and the spell carries the caster |
| `mercury_cage` | 64 | close (0.8 s), idle, open | the Silver Star of Execution: eight bars with arrow heads, a ring of eight slabs on top, four shield plates between the bars |

## 5. The spells (`GrimoireBook.hurt` applies the EP curve; `EnergyBridge.power` 0.8 to 2.4 is the aggressive factor the old pages already used)

| Page id | Tier | Cooldown | What it does |
| --- | --- | --- | --- |
| `silver_blade` | starter | 80 | 3 floating blades pour out, each flies at the aim 2, 4, 6 ticks later; each damages like the old spear: 6 x power, pierce, the middle one homes |
| `mercury_shield` Silver Guardian | signature | 600 | 8 s: the armour (style 0), a dome of radius 2.2 + 0.6 x power (shots that touch it are discarded, blows x0.5 except fire, through `onTakenDamage` as before), the toxin: 0.5 HP a second to you while above 6 HP and never in creative; at the end the dome flows into up to 6 spears (5 x power) |
| `mercury_rain` Silver Rain | signature | 600 | the cloud (old dome shape), a pool, 10 + 6 x tier drops at the aimed place in a radius of 4 + 1.5 x power, 3.5 x power each |
| `silver_eagle` | signature | 600 | the eagle forms; for 26 ticks the caster is carried 28 blocks along the (flattened) line of sight (fall damage cancelled), every foe within the wingspan is hit once for 9 x power and knocked aside; the armour (style 1, 66 ticks) |
| `silver_bullet` | starter | 80 | 5 to 8 bullets (power) in a spread, speed 2.6, 3 x power each |
| `silver_spear` | starter | 80 | 1 to 3 spears, 7 x power each, pierce, the middle one homes |
| `silver_shining_regal_spear` | signature | 600 | 40 ticks: from the palm along your look, reach 16 + 6 x power (at most 30), width 0.9: 1.6 x power every 2 ticks on everything in the line, barriers and projectiles in the beam broken as it opens; the 5 s armour |
| `silver_star_of_execution` | signature | 1200 | the cage at the target for 5 s, radius `starRadius` (4.5 to 7): every 20 ticks 2 to 5 spears at those inside (5 x power); enemy shots entering are discarded; if the shared control lock is free whoever tries to leave is turned back (not bosses); the 5 s armour |

Status helpers: `Nullification.shatterBarriers` (beam), `BalanceLaw.beginControl` (cage hold), `AttributeEvents` not used; `MercuryBook.onTakenDamage` (the dome cut) unchanged. Fire gets through the dome as before.

## 6. VFX (`MercuryLayer`, `tools/gen_mercury_vfx_textures.py`)

* FX1 cast: a ripple opens at the hand; a spear (the `mercury_spearhead` sprite) flies with a beaded liquid tail that thins behind it and droplets shed and falling; a splash crown where it lands. Used by every projectile of the book, beam pulses (power 1.6) and drops.
* FX2 zone (power = radius, drawn 1.5x): a pool of rippling mercury spreads, two ripple planes turn over it, a wall of liquid lattice stands on the rim as a hoop, six liquid bars rise and a ring closes on their tops, drops rise from the pool.
* FX3 impact: a white flash, a splash crown on the ground and a second one standing across the hint, three ripple rings, twelve droplets in arcs.
* Textures: `mercury_ripple` (256), `_droplet` (64), `_streak` (64 x 256), `_crown` (128), `_spearhead` (64 x 128), `_grid` (256). Up to 190 vertices per instance. The Time Magic VFX is untouched.

## 7. Files

New: `book/MercuryArts.java`, `MercuryRules.java`, `prop/MercuryProps.java` and `client/prop/MercuryPropPainter.java` (were stubs), `client/aura/MercuryAura.java` (stub), `vfx/client/layer/MercuryLayer.java` (stub), `book/ext/MercuryExt.java` (stub), `tools/gen_mercury_geo.py`, `tools/gen_mercury_vfx_textures.py`, `tools/lang/mercury_rebuild.json`, scenes `tools/render_preview/scenes.d/mercury_render.json` and `tools/vfx_preview/scenes.d/mercury.json`, 15 geo models, `textures/aura/mercury_coat.png` and `_sheen.png`, 6 particle textures, `MercuryRulesTest`. Shared one-line edits are in `transmutation_rebuild.md` section 7 (`PropKind.MERCURY_3`).

## 8. Tests and previews

`geo_check` (15 models OK), `GeoAssetsTest`, `MercuryRulesTest` (including that the toxin stops above its floor and never touches creative), self-test `magic.mercury_rebuild`; type check clean. Previews: `python3 -B tools/render_preview/preview.py mercury_eagle_idle mercury_cage_close mercury_blade_idle mercury_spear_idle mercury_armour_forge mercury_armour_full mercury_armour_shatter` (with `EquipAnimation` in the class list), `python3 -B tools/vfx_preview/preview.py mercury_cast mercury_zone mercury_impact`.

## 9. NOT verified

* Nothing was run in the game; the previews are re-implementations.
* The Eagle's carry sets the caster's velocity every tick for 26 ticks: how that behaves against walls, a ceiling, another mod's movement checks or anti-cheat was not tried; the eagle prop follows the caster, positioned a little below the feet from the model's height, not tested for a hitch.
* The Guardian's toxin uses `damageSources().magic()`; hooks that react to damage (armour hooks, other mods) will see 0.5 HP hits once a second.
* The old dome VFX still draws the Rain's cloud; the new pool and cloud are not synchronised beyond their start times.
* The wing and feather boxes are 0.5 to 1 px thick: thin from the front.
* The Silver Eagle cannot carry other people (the wiki says it can); the Star of Execution has no "chaotic mana" failure; Dimension Slash: Scorching Light is not built.

## 1.99.0 armour policy

Mercury Magic is policy kind TOGGLE (Silver Guardian). The armour is put on **only** by `mercury_shield` (cast it to forge the armour in with the Djinn equip animation; cast it again to take it off, free of cost). It stays until it is toggled off, the mana runs out (a little mana is paid every second), the wearer is knocked out or dies, and shatters out with the same animation; while it is on it gives the stats of the policy table. A cast of any other page never puts it on and never resets it. See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).
