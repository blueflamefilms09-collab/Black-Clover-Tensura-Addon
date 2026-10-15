# Rukh core (R1, 1.36.0): Dark and Light Rukh particles

The owner's rule: relics, equipment and active magical abilities spawn no vanilla particles, only the Magi "Rukh" (a flock of birds): Dark
Rukh and Light Rukh, cheap to draw, short-lived, capped. This document is the WHY of every file R1 added. The owner's brief names the asset
path `assets/tensura_black_clover/...`; this mod's id is `nusmp`, so everything lives under `assets/nusmp/`.

## Server API: `particle/rukh/RukhFx.java`, `RukhMath.java`, `RukhBudget.java`
`RukhFx` is the one way to emit Rukh from server code (`burst`, `stream`, `gather`, `aura`, plus `spray` as the drop-in for a raw
`sendParticles`). Why a facade: the caps (12 per burst, 6 per tick per stream, 12 per gather, 2 per aura) and the per-level per-tick budget
(96) are enforced in one place, so no call site can flood the packet stream, and the purge is mechanical. Why `ServerLevel.sendParticles`:
it is the vanilla particle packet, so the client needs no custom networking. Directed particles (stream, gather) use `count = 0`, where the
packet delta is the velocity, one packet per particle; that is affordable because of the caps. `RukhBudget` is the cap as a pure class (a counter that
resets when the game tick changes), one per `ServerLevel` because each level has its own clock; excess requests are dropped, not queued (a
missing bird is invisible, a late one is a bug). `RukhMath` holds the clamps and the geometry (ring placement 2.5-4 blocks for the Magi
gathering, velocity that covers a distance in 18 ticks with a 0.6 blocks/tick limit, the scale and alpha curves) without Minecraft classes so
they are unit tested (`RukhBudgetTest`) and the server and client twins cannot drift apart.

## Client twin: `client/particle/rukh/RukhClientFx.java`
Same names for call sites that already run on the client (renderers, layers) and used `level.addParticle`. It only references common classes, so
common code may call it behind `level.isClientSide()`; budget 64 per tick because the client also receives server Rukh.

## Types and providers: `RukhParticles.java`, `RukhClient.java`, `particles/dark_rukh.json`, `particles/light_rukh.json`
`DARK_RUKH` / `LIGHT_RUKH` are `SimpleParticleType(false)` on `Registries.PARTICLE_TYPE` (no payload; the SimpleParticleType constructor is
protected, hence the anonymous subclass). `dark()` / `light()` never return null: the holder is only bound after registration; before that they hand
back the vanilla type, which only happens if someone breaks the rule "look the type up lazily, never in a static initialiser". `RukhClient`
listens to the mod-bus `RegisterParticleProvidersEvent` and uses `registerSpriteSet`, so the engine builds the SpriteSet from the JSON files
(`{"textures": ["nusmp:dark_rukh_0", ... "_5"]}`: six wing-pose sprites stitched into the particle atlas, reloaded with resource packs).

## Particle classes: `RukhParticleBase`, `DarkRukhParticle`, `LightRukhParticle`
Shared lifecycle in the base: lifetime 15..25 ticks, translucent particle sheet (batched with all vanilla particles), no gravity, no collision,
velocity clamped every tick, smooth scale in/out and a gentle alpha fade, and the flap cycle (each bird starts at a random phase and steps through the six
sprites twice per life, so a stream shows the flock of the references in many poses). Dark drifts and tumbles (sideways wobble, slow roll, faint upward
bias) and is lit by the world; Light is full-bright (`getLightColor` = 15728880) and curls in a slow spiral (the velocity vector is rotated, so speed and the clamp are untouched).

## Textures: `textures/particle/{dark,light}_rukh_{0..5}.png` by `tools/rukh/gen_rukh_textures.py`
32x32 RGBA, one bird per frame, six wing poses (wings up in a V, rising, spread, down, glide with swept tips, dive). Dark Rukh follows the owner's ink drawing: solid black with a
faint violet cast toward the wing tips, hard edges roughened by noise like brush work, no outline glow. Light Rukh follows the glowing flock: cream-white core, a soft
yellow halo that fades to translucent. The script is deterministic and has `--check`; `--sheet` writes the contact sheet used to review them.
The particles are tinted white (colour 1,1,1) so the texture colours are the final colours.

## Purge: `tools/rukh/purge.py`, `docs/rukh/scope.txt`, `RukhPurgeScopeTest`
`purge.py` rewrites `ParticleTypes.<NAME>` to `RukhParticles.light()/dark()` by a name table (dark: smoke, soul, ink, sculk, witch, ash,
portal, dragon breath, effect, damage indicator, sonic boom, spores, omens; everything else light) and refuses to touch RISKY lines: static/final initialisers
(the registry is not bound at class-init time, so they must become lazy), case labels, enum constructor arguments, ParticleOptions-typed values, and every
particle whose options carry data (Dust, Item, Block, Shriek, Vibration, entity effect colour, sculk charge): those need a human decision. A line is replaced whole or not at all.
The scope lives in `docs/rukh/scope.txt` (globs, `@pN` slice tags, `!` exclusions) and is read by BOTH the script and the test, so they cannot disagree.

Scope: IN = relics, client relics, items, charter, blackclover, skills, books (not `TimeBook`), core, grimoire client, the sword draw, wizard, nexus relics:
these are the relics, equipment and active abilities of the rule. OUT, and why: bosses (`entity/zogratis`, `entity/avatar`, `boss`) have their own tuned
spectacle and budget; the legacy and Labyrinth dungeons (`dungeon/**`, `labyrinth/**`, `client/dungeon`) are ecosystem and ambience, not relics; `entity/**`
animals and mobs are not abilities; world blocks are world; `vfx/**` includes the Time Magic layer, the quality standard, which must not change.
`RukhPurgeScopeTest` fails on any vanilla particle type, Dust/Item/Block option or an `addParticle`/`sendParticles` whose first argument is not a variable or a
Rukh type, except lines marked `// rukh-allow: <reason>`. It only enforces when `docs/rukh/purge_enforced` exists (the lead creates it when the purge is done) or with
`-Dnusmp.rukh.enforce=true`, so CI is green while the purge is in progress; its glob/scope-file checks always run.

## Registration lines (for the lead)
`com.newuniverse.nusmp.particle.rukh.RukhParticles.init(modEventBus);` (any time before the mod bus finishes dispatch) and, client dist only,
`com.newuniverse.nusmp.client.particle.rukh.RukhClient.init(modEventBus);`.
