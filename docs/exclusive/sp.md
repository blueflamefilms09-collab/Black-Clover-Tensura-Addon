# SP: Spatial Magic portals (1.43.0)

Nothing here was run in the game. Type-checked against the API stubs, pure logic JUnit-tested, mesh previewed headless (see "Unverified").

## New vs replacement
- REPLACEMENT with switch: the `SPACE_PORTAL` drawing. `ArcaneSpellLayer.portal()` hands to `SpatialPortalLayer` when `spatial.portal3d` is on (default); off keeps the old flat portal code untouched. No payload or VfxShape change.
- NEW: shader `spatial_portal`, screen-copy refraction, warp pulse, Rukh at the horizon, pair thread, six `portal_*.png` textures (`tools/gen_portal_assets.py`).
- Mechanics (`SpatialBook.gate/hop`, signatures kept): momentum rotated through the gate, Projectiles and ItemEntities teleported (living entities as before), enter/exit sounds, 30 tick guard kept, exit 0.85 blocks in front of the exit gate. Both gates now share one payload seed so the client can link them.
- KEPT: old flat portal, old 3-arg `hop` (now a thin wrapper without rotation), old behaviour for saves without the stored gate facing (no rotation).
- Door of Fate, Fallen Angel Gate, Unopening Red Room (it uses SPACE_CUBE, not the portal: unchanged), Zenon, Sheath, wave-boss gates all get the new drawing through the shape. No `CanonBook.FINRAL` code uses SPACE_PORTAL, so nothing Finral-specific to wire.

## How it works (the why of each class)
- `vfx/client/portal/PortalSpec`: three detail tiers as counts (tier 0: 5 shells, 24 runic segments, 3 horizon rings; tier 2 fits the 400-vertex buffer). Tier by distance (24 / 48 blocks) and the particle setting.
- `PortalMesh`: procedural, not GeoLite: sector cos/sin tables generated once per tier and cached. A .geo.json is fixed cubes; this needs tiers, an oval of any size and rings that turn every frame.
- `PortalMath`: opening (12 ticks: slit grows tall, then widens) and collapse (8 ticks: width goes first), shell scale/depth, shell parallax (`v.xy * d / (|vz| + d)`, clamped to an ellipse so shells never leave the opening), swirl speed, deterministic jitter.
- `PortalComposer`: back to front: backdrop (plain path only), shells, horizon, rim glow, runic ring (with soft base band), rim lightning arcs; plus the thread between the pair. No allocation per frame.
- `PortalBufferSink`: plain translucent path through the shared effect buffer (preview, shader missing, or the shader draw threw).
- `PortalClient`: game side. Direct draw with the shader, once-per-frame screen copy (only if a gate is within 16 blocks and `spatial.refraction`), warp ripple within 3 blocks, Rukh (dark near the core, light near the rim, via `RukhClientFx`, no vanilla particles), sounds. A thrown draw sets `broken` and logs once; from then on the plain path is used. The copy target is destroyed when the last gate closes.
- `SpatialPortalLayer`: per-gate state, pair linking by equal payload seed, tier choice, thread. Preview-safe (no GL).
- `SpatialPortalConfig` / `PortalSettings`: client config with a safe fallback before it loads.
- `book/PortalMomentum`: the flat geometry (facing, turn = faceB - faceA - PI, exit point with mirrored sideways offset).
- Shader `spatial_portal.{json,vsh,fsh}`: horizon samples the copy through a swirl ripple, dark/violet toward the core, swirl light on top; `PortalMode` 1 redraws the copy with a radial ripple (clip-space quad).

## Config (client, `config/nusmp-spatial-portal.toml`)
`spatial.portal3d` (true), `spatial.warpStrength` (0.35, 0..1), `spatial.refraction` (true), `spatial.portalParticles` (true).

## Preview
`python tools/vfx_preview/preview.py portal_open portal_close portal_oblique portal_tiers`. The preview patch lifts the 400 cap per scene (`"budget"`), only for the preview. It shows the plain path (no shader, no refraction, no pair thread: the harness draws one gate).

## Unverified
Shader compile on real drivers, the framebuffer blit (assumes the main target is bound when VFX render; Fabulous graphics untested), the refraction look, Rukh feel, sounds, the thread, the warp pulse, entities leaving the gate in walls, players' client sync after `teleportTo` with rotation. Pre-existing and unchanged: gate A's visual (400 ticks) can end before the zone (400 ticks after B).
