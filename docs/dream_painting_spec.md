# Dream Magic and Painting Magic (0.41)

Wiki references, fetched through `.github/wiki-request.txt`: *Dream Magic*, *Glamour World*, *Dorothy Unsworth*, *Painting Magic* and *Rill Boismortier*. Both magics' VFX are in `vfx/client/layer/DreamPaintLayer.java`, built to the Time Magic standard: layered iridescent colour, motion in every piece, a bloom core, and a clear opening, middle and close. Textures come from `tools/gen_dream_paint_vfx_textures.py`. Preview every effect without the game with `python tools/vfx_preview/preview.py dream_transition dream_dome dream_manifest dream_shatter paint_stroke paint_splat paint_beast paint_camo`.

## Dream Magic: a real pocket dimension
From the wiki:
- The mist pulls anything it touches into a World of Dreams. Those inside appear destroyed from the outside.
- Whatever the user imagines takes real physical form.
- The victims grow drowsy.
- Two Dream users competing inside can overload the world and collapse it.
- Dorothy reads minds and manifests her victims' thoughts.

**In game:**
- **The dimension:** `nusmp:dream` is a void with a fixed starry dusk sky and a pastel fog that shifts slowly between pink and sky blue (client `DreamSkyClient`).
- **The arena:** every dream gets its own arena, 512 blocks apart, rebuilt each time:
  - a disc of pastel wool rings set with sea lanterns, on a white base;
  - flowers, cotton-candy trees and candle cakes;
  - a dome of stained glass in bands (pink → magenta → purple → light blue → cyan) that keeps everyone in.
- **Casting** (Dream World: any Dream mage; Glamour World: Dorothy's grimoire only):
  1. The opening plays (`DREAM_TRANSITION`): pastel mist puffs spiral out to the edge of the spell, starlight bursts with them, and a shimmering ring races across the ground. Then the mist folds back in and rises, swallowing everyone.
  2. 1.25 s later the caster stands at the south edge of the dream and the dreamers stand round the middle.
     - **Dream World:** the 2 nearest foes within 8 blocks, 15 s.
     - **Glamour World:** up to 8 foes within 12 blocks, 30 s.
     - Bosses can't be pulled in.
  3. The dome effect (`DREAM_DOME`) plays overhead: an aurora of veil bands whose colours slide pink → lilac → sky → mint as it turns, a shimmer ribbon round its foot, soap bubbles rising and stars twinkling.
- **Imagination Manifestation:** inside the dream, the caster says something and it becomes solid (`DREAM_MANIFEST`: smoke spirals in, condenses, then a flash, a pop of stars and a shockwave ring). There is a 2 s cooldown. Casting the new page does the same, cycling through the list.

  | Word | What appears |
  |---|---|
  | bear / teddy / plush | a giant stuffed bear (7 blocks of wool) drops on the target: 10 damage and slows |
  | feast / cake / food | a table of cakes; the caster is healed and fed |
  | fire / flame | a ring of campfires round each nearby dreamer; burning, 6 damage |
  | ice / frost / snow | a packed-ice prison round each nearby dreamer; frozen and slowed |
  | cage / trap / bird | a bird cage (iron bars, gold roof) round the target |
  | star / meteor / sky | six falling stars, one after another, 7 damage each |
  | wall / shield / candy | a candy wall between the caster and the dreamers |
  | sleep / pillow / lullaby | every dreamer's dream load +25 |

  **Mind reading:** if a dreamer says one of these words, the caster reads it and makes it real against them.
- **Exhaustion:** each dreamer's dream load rises every second (6/s in Glamour World, 8/s in Dream World). It shows on the action bar with how cracked the dream is.
  - At 100 their mind breaks: blind, nauseous, nearly unable to move, weakened, 10% of their magicules drained, and hurt for 4 + 5% of max health.
  - The load then falls back to 30 and climbs again.
- **Breakout:** the dream shatters (`DREAM_SHATTER`: the dome flashes and cracks into shards that burst outward and fall) when either:
  - the dreamers' combined damage inside reaches 30 + 12 per dreamer; or
  - a dreamer teleports (ender pearl, chorus fruit, a spatial skill), or casts Spatial, Time, Anti-Magic or Dream Magic (the second Dream mage overloads it).

  The caster takes the backlash: weakness, slowness and nausea.
- **Ending:** the dream ends when it runs out, shatters, or its caster dies or leaves. Everyone goes back exactly where they were. A player who logs out inside is sent back when they next log in.

## Painting Magic (new attribute)
- **What it is:** a palette and a brush. The paint comes to life and can become any element. Rill Boismortier's canon grimoire is now Painting Magic (it was Creation).
- **Character locks:** Painted Menagerie and Master of Valhalla are locked to Rill.

| Page | What it does | VFX |
|---|---|---|
| Brushstroke | a wet stroke of ink flung forward: hurts and slows, leaves a sticky puddle | PAINT_STROKE, PAINT_SPLAT |
| Spring of Restriction | sticky paint wells up under the target and sets like lacquer, rooting everything in it (control lock) | PAINT_STROKE, PAINT_SPLAT |
| Camouflage | paint yourself into the scenery: invisible for 15 s, until you strike | PAINT_CAMO |
| Souterrain Giant's Strong Arm | a painted giant's arm bursts up and flings the target skyward | PAINT_BEAST |
| Deux Tempêtes of Fire and Ice | two painted storms circle you for 5 s: one burns, one freezes | PAINT_SPLAT, PAINT_STROKE |
| God's Game | 8 s of your own rules: shots near you turn to paint, heavy resistance and absorption | PAINT_BEAST, PAINT_SPLAT |
| Elemental Quintet | five strokes of five elements at up to five foes (fire, water, wind, earth, lightning) | PAINT_STROKE ×5 |
| Master of Valhalla | a painted host charges out ahead of you: 16 damage down a 16-block lane | PAINT_BEAST, PAINT_STROKE |
| Painted Menagerie | painted beasts fight for you for 30 s | (kept from 0.34) |

**VFX:**
- **Ink and brushwork** (`PAINT_STROKE`): the stroke texture is laid down segment by segment along a gentle arc, so it paints on rather than stretching. It has a glossy wet highlight along one edge, droplets flicked off the brush, and a splash where it lands.
- **Viscosity** (`PAINT_SPLAT`): a splatter with drips running outward. Halfway through, a lacquer gloss sweeps across it as it hardens. That is the moment it starts rooting.
- **2D to 3D** (`PAINT_BEAST`): flat strokes lift off the ground and wrap round into the creature's shape, then close into a glowing wet outline that breathes.
- **Camouflage** (`PAINT_CAMO`): a refraction ripple slides up the body in bands, with flecks of paint drifting off. It is subtle on purpose.

## Also in this request (already in the mod)
- **Summon Grimoire base ability:** there since 0.22. The book floats beside your right hand, and pressing the key while sneaking puts it away with the closing animation.
- **Creative grimoire safeguard:** there since the first fixes. Binding a creative grimoire is refused when you already have one, so it never duplicates or overwrites.
