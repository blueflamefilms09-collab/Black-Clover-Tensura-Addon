# Trial effects (Israfil, Lord of Trials): the calls the server makes

Drawn by `vfx/client/layer/TrialLayer.java` (textures `textures/particle/trial_*.png` from `tools/gen_trial_vfx_textures.py`, preview scenes
`tools/vfx_preview/scenes.d/trial.json`). The packet colour is ignored (the palette is the layer's own: deep indigo, midnight teal, cyan-white, a little brass);
pass `0xFF6FE8F0` (`TrialFx.CYAN`). All six go through `VfxSpawn` (nearby players only, the effect-packet budget). Nothing runs on the server but the one packet.

| Shape | Call | from | to | power | ticks | follow |
|---|---|---|---|---|---|---|
| `TRIAL_LABYRINTH` | `VfxSpawn.sendFollowing(level, shape, target, centre, colour, 160, 4.5f)` (or `send` if it must stay where it was raised) | the trapped creature's centre (the floor is drawn 0.9 below it) | same point (unused) | cage radius, about 4.5 | 160 | yes (or no) |
| `TRIAL_GRAVITY` | `VfxSpawn.sendFollowing(level, shape, target, feet, colour, 40, size)` | the target's feet | unused | size, about 1.5 (1 to 3) | 40 | yes |
| `TRIAL_COLLAPSE` | `VfxSpawn.send(level, shape, groundPoint, groundPoint.add(0,1,0), colour, 120, radius)` | the ground point of the implosion | from + (0,1,0) | radius, about 6.5 | 120 | no |
| `TRIAL_SHIFT` | `VfxSpawn.send(level, shape, before, after, colour, 14, 1f)` | where the caster was | where it arrives (1 to 48 blocks away) | 1 | 14 | no |
| `TRIAL_TRUMPET` | `VfxSpawn.send(level, shape, centre, centre.add(0,1,0), colour, 50, radius)` | the caster's centre | from + (0,1,0) | radius of the blast (100 in game) | 50 | no |
| `TRIAL_RIFT` | `VfxSpawn.sendFollowing(level, shape, walker, centre, colour, 26..100, 1f)` re-sent while held | the walker's centre | unused | 1 | 26 to 100 (fades in 6, out 12) | yes |

Notes
- `TrialFx` (the server helper) already makes exactly these calls; labyrinth is sent there with `send` (fixed in place), which the layer draws the same way.
- The rift trail lies behind the followed entity's body yaw (`VfxInstance.followYaw`); with no follower it points back along -z.
- The trumpet's `VfxShake` and the collapse and gravity shakes are done by the layer on the client (`VfxShake.add`, which falls off to nothing at 32 blocks); the server sends nothing extra.

## Cost (rule 11)
Worst case quads per frame, scanned at 39 ages per effect in the preview (the activation cap is 100 quads = 400 vertices):
labyrinth 43, gravity 31, collapse 35, shift 35 (a 48-block shift; 30 for a short one), trumpet 62, rift 27.
Network: one small packet per use (rift: one about every second while held). Server work: none beyond building that packet. Client: no per-frame allocation beyond a few small vectors,
no state kept between frames, `ctx.seg` thins the counts with distance and the particle setting, far decorative extras are skipped.
