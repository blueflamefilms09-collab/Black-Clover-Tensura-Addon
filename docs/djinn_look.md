# Djinn magic in its own element (3.75.0)

**The problem.** Every Djinn's magic was drawn with the same white-gold Rukh whatever its element: `DjinnFx.align` answered "Light Rukh" for all eight kinds, and the coloured "dust" of a Djinn became a Rukh too, so its colour was lost. Amon's fire, Vinea's water and Baal's lightning looked the same.

**What is drawn now** (`relics/djinn/DjinnLook`, a pure table; preview `docs/previews/djinn_look/djinn_elements.png`):

| element | particles (theme) | travelling cast | field | impact | strike on a mark |
|---|---|---|---|---|---|
| Heat | embers, flame tongues (`magic.FLAME`) | `FLAMEFX_CAST` | `FLAMEFX_ZONE` | `FLAMEFX_BURST` | `FIRE_PILLAR` |
| Lightning | electric orbs, sparks (`magic.LIGHTNING`) | `LIGHTNINGFX_CAST` | `LIGHTNINGFX_ZONE` | `LIGHTNINGFX_BURST` | `LIGHTNING_SPEAR` from 14 blocks up |
| Water | droplets, bubbles (`magic.WATER`) | `WATERFX_CAST` | `WATERFX_ZONE` | `WATERFX_BURST` | `WATER_BURST` |
| Wind | feathers, wisps (`magic.WIND`) | `WIND_GALE` | `WIND_TORNADO` | `WIND_RING` | `WIND_TORNADO` |
| Life | leaves, petals (`magic.PLANT`) | `PLANT_FX1` | `PLANT_FX2` | `PLANT_FX3` | `TREE_ROOTS` |
| Strength | rock chips (`magic.EARTH`) | `EARTHFAM_CAST` | `EARTHFAM_ZONE` | `EARTHFAM_BURST` | `EARTHFAM_BURST` |
| Sound | notes, rings (`magic.SOUND`) | `SOUND_FX1` | `SOUND_FX2` | `SOUND_FX3` | `SONG_RING` |
| Light | stars, glints (`magic.LIGHT`) | `DIVINE_RAY` | `DIVINE_SIGIL` | `LIGHT_FLARE` | `DIVINE_PILLAR` |

All of these effects and themes already existed in the mod (the Black Clover magics and the Lucius fight use them); nothing new was drawn. The mesh effects are tinted with the Djinn's own body colour.

**Where.** The Extreme Magic: the field under the caster while it charges (ZONE), then per shape: Nova / Wave / Quake / Implode an IMPACT, Bolts / Columns / Rain a STRIKE on every mark, Beam / Cone / Cross a travelling CAST (four for the Cross), Vortex a ZONE. Equipping, binding and the phase changes: an IMPACT on the bearer. The metal vessel's techniques: an IMPACT wherever they drew their double ring. The Half Equip's blows, the Lesser Summon's bolt, the equip aura and every ring, line and column of the spells now use the element's particles dyed in the Djinn's colour. A **Dark Djinn keeps the black Rukh** (its violet is no Djinn's colour, so nothing is re-themed), and the white Rukh flashes of the spells stay: the Rukh is still there, it is no longer the only thing.

**How the code finds the element.** The old helpers were only handed a colour, so `DjinnLook.affinityOf(rgb)` looks the colour up in the Djinn table (a body colour or a glow). All 36 Djinn resolve; plain white, which Djinns of three elements share as a glow, stays a Rukh.

**Setting.** `serverconfig/nusmp-djinn-bind-server.toml`, `[djinnLook] typedVfx` (true). false: exactly the look before 3.75.0. It is a server setting because the server picks the particle and sends the effect.

**Cost (rule 11).** Particles: the same counts as before (the helpers were only re-pointed: a ring is at most 96 points, a line or column 40, a spray 12), now through the particle facade's per-tick budget instead of the Rukh budget. Mesh effects: per Extreme Magic casting (10 minute cooldown) 1 field + at most 9 strikes (Bolts), 8 (Columns), the Rain's steps, 4 (Cross) or 3 (Quake); 1 per equip or phase change; 1 per vessel technique impact. Every one is a single packet to players within 96 blocks through `PerfBudgets.effectPacket` (160 a tick server-wide by default); the layers drawn were measured at 48 to 400 vertices each by the previewer. No tick work, no state.

**NEW**: `DjinnLook`, the setting, `Fx.sprayOptions` (one appended method of the particle facade), tests `DjinnLookTest`, self-test `djinn.look`. **REPLACED**: nothing is removed; with `typedVfx = false` the old look returns. **KEPT**: every number, shape and timing of the spells; the Rukh flashes; the Dark Djinn's look.

**Not seen in a game.** The preview is the mod's offline effect previewer (no world, no lighting). `SONG_RING` (Sound's strike) and `LIGHTNING_SPEAR` (Lightning's strike) could not be rendered by the previewer; they are existing effects of the Song magic and the Lightning magic. The sizes (`power`) were chosen from each effect's documented meaning, not tuned by eye.
