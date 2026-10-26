# Jibril VFX calls (3.16.0, "dawn / law of the sky" look)

The eight shapes are appended after `TRIAL_RIFT`; the order goes over the network, so nothing before them moved. Every call is one `VfxSpawn` packet from `JibrilFx` (the only class that may send them) with one `JIBRIL_*` shape; the packet colour is ignored, the layer has its own palette. Drawn by `vfx/client/layer/JibrilLayer.java`; textures `textures/particle/jibril_*.png` from `tools/gen_jibril_vfx_textures.py`; previews `python tools/vfx_preview/preview.py` (`tools/vfx_preview/scenes.d/jibril.json`, 17 scenes). The packet contract (from / to / power / duration / follow) and all server code are unchanged by this redo.

## Look (what makes it Jibril and not Sandalphon)

Pearl-white core, rose-gold and amber warmth, azure-to-lavender cool shadow, and an iridescent prismatic rim (soft pink to gold to aqua to violet). Motifs: layered feather wings, nested halos with tiny orbiting feathers, flowing ribbons and law banners, unrolling scrolls with cursive script, an open book under a crown of rays, braided ribbons with feather links, braided pillars with falling petals. It shares none of Sandalphon's black cracks, clock / tick rings, hexagram, steel grey or pure white-gold, and none of Israfil's cyan maze. No scales-and-sword emblem is used.

## Calls

- JIBRIL_COMMAND: Absolute Command, sent FOLLOWING the target: from = its centre, power = size (~1.5 = the target's width x 1.6), 100 ticks. Anticipation: a rose-window seal draws in on the floor. Lock (tick 9): shock rings, flash, prism bloom. Hold: a halt glyph and book emblem under nested halos over its head, a prison of law banners dropped from above with dawn curtains, braided feather-link chains with sag, orbiting scrolls and spiral ribbons. Afterglow: petals and motes.
- JIBRIL_JUDGMENT: Judgment of Heaven, sent at a point: from = the ground point under the target, to = from + (0,1,0), power = pillar radius (~2.5), 40 ticks: a braided pillar of light (haze, braid, counter-braid, core; lavender top, rose-gold foot) 64 blocks high with ribbon strips on its flanks, five halos sliding down it, strike flare and prism bloom, floor seal / glyph / spiral, rings and waves, a pair of wings opening at the foot, petal rain, feathers, sparks, camera shake (VfxShake at tick 8, through the shake budget).
- JIBRIL_PURGE: Heavenly Purge, sent at the caster: from = its centre, power = radius (50 in game), 36 ticks. Gather ring with inward ground streamers, flash and prism, glyph and seal, a braided column, a spiral and ray sweep, four wave rings (two with companion rings) and three dome rings out to the radius (the picture is clamped to 40 blocks), afterglow halo, petals, feathers and motes. Far wave rings are skipped by distance.
- JIBRIL_SHACKLES: Holy Shackles, sent FOLLOWING the target: from = its centre, to = the caster's chest, power = size (~1.5), 60 ticks. Floor seal, three braided chains of four segments wrap and lock (shed feathers), three chain rings with a halo, flash / prism / shock ring on the lock, orbiting scrolls, then a ribbon-and-beam stream with sparks flows to `to` and blooms at the caster.
- JIBRIL_WORD: Word of Law, sent FOLLOWING the caster: from = its centre, power = reach (~1.8), 12-16 ticks (the parry window). A ring converges, a rose-window seal and two glyphs turn against each other in front of the caster (in its facing), two halos and a sunburst behind, a scroll unrolls below, a small pair of wings opens, three parry rings and a wave strike out, script ribbons orbit, runes ride the edge, petals and feathers burst out.
- JIBRIL_REFLECT: the parry lands, sent at the caster: from = the caster's centre, to = the attacker's centre (1-48 blocks), power = 1, 14 ticks. Caster side: rings, glyph, emblem, halo, flash and prism. Between: three braided beams with an amber underlay, soft ribbon strips, travelling rings, feathers and petals riding the beam. Attacker side: glyph, seal, three rings, a wave, rays, flare / prism, sparks, petals and feathers.
- JIBRIL_KINGDOM: Kingdom of God, sent FOLLOWING the caster and re-sent every 80 ticks while held: from = its centre, power = radius (50 in game), 100 ticks (fades in 12, out 12). Rose-window floor seal, spiral, rim rings, rays and glyph near the camera, up to six of 24 dawn curtains (the ones nearest the look ray), nested halos with orbiting feathers over the caster, three streamers, petals and motes. Vertex budget 200 (50 quads).
- JIBRIL_WINGS: Wings of Revelation, sent FOLLOWING the player and re-sent every 80 ticks while the toggle is on and the player is in the air: from = its centre, power = span scale (1.0 = about 3 blocks per wing), 100 ticks. Two pairs of layered feather wings (alpha-blended with a faint prismatic rim light added on top) behind the body, a prismatic backlight, nested halos with orbiting feathers over the head, tip streamers, falling feathers and petals. Vertex budget 200 (50 quads).

## What "upgraded" means here (not 5x the quads)

Each effect is a staged sequence of at least five layers (anticipation, strike or lock, resolve, afterglow); textures were redrawn at 2-4x resolution (glyph, seal and wing 1024 px; scroll 1024x512; the rest 512 or 128x512) with the colour baked in, and six new ones were added (scroll, emblem, petal, ribbon, spiral, prism); the animation uses scrolling shimmer, staggered feather and petal fall, easing and a mix of camera-facing and flat layers. The quad budget is used about twice as much as in 3.15.

## Worst case per effect (measured in the preview at every integer tick, engine cap 100 quads / 400 vertices)

| Effect | 3.15 quads | 3.16 quads (worst frame) |
| --- | --- | --- |
| COMMAND | 35 | 73 |
| JUDGMENT | 44 | 69 |
| PURGE | 29 | 60 |
| SHACKLES | 35 | 70 |
| WORD | 17 | 59 |
| REFLECT | 23 | 58 |
| KINGDOM | 24 | 41 (82 during the 20-tick overlap of a re-send) |
| WINGS | 19 | 45 (90 during the 20-tick overlap of a re-send) |

Rule 11 cost: client only, no server work and no extra packets (one `VfxSpawn` per effect, unchanged). The layer keeps nothing between frames, allocates nothing per frame beyond a few scratch vectors, thins every repeated element through `ctx.seg` (distance and particle setting) and skips decorative extras beyond 40-90 blocks; the manager already drops an effect that is out of reach. Textures: 22 files, largest 1024x1024 (about 4 MB each in VRAM, the whole set about 20 MB).
