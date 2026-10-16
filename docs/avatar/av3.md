# Avatar system, part AV3: creation screen, networking, appearance, renderer (release 1.51.0)

Everything here is NEW (nothing like it existed; the Devil Avatar boss in `entity/avatar` and `client/avatar/AvatarRenderer` + `AvatarClient` is a different thing and was not touched).
Namespace note: the owner's spec says `multiverse_mod`; the real mod id is `nusmp`, so ids and datapack paths use `nusmp` (`nusmp:player_avatar`, `data/nusmp/avatar_races/`).
Nothing was run in the game. What was checked: the pure classes were compiled and unit-tested with the JUnit shim; the Minecraft-facing classes were type-checked against the API stubs (the stubs lack Screen/EditBox/Minecraft and the payload classes, so the screens were reviewed by hand against the 1.21.1 API dump, see "Unverified").

## Classes and why they exist
| Class | Why |
|---|---|
| `avatar/appearance/AppearanceData` | One validated, NBT-backed record for the look (skin tone, hair 0-15, outfit 0-11, horn -1..7, wing -1..5, scale, aura RGB, eye RGB). -1 means no horns / no wings, so an old save never grows horns. |
| `avatar/appearance/DynamicAppearanceEngine` | Pure. `sanitize` clamps everything a client sends (scale to race size +-15%), `defaultsFor(race, scale, seed)` is the seeded pleasant default, `scaleFor/minScale/maxScale`. |
| `avatar/appearance/AvatarRaceGroup` | Maps any race id to one of three body skins (human, bulky, small) by name, so datapack races need no code. |
| `avatar/appearance/SkinCompose`, `TextureLru` | Pure skin arithmetic (grey flesh x skin tone, iris x eye colour, outfit over it) and the 64-entry LRU that owns the composed GPU textures (evicted ones are released). |
| `avatar/menu/AvatarMenus`, `AvatarBuilderMenu` | Slotless menu = the server's proof that the creation screen is open. `AvatarBuilderMenu.open(ServerPlayer)` opens it and sends the race list. |
| `avatar/menu/BuilderState` | Pure page state machine of the screen (what must be done before each page can be left, when Create is lit, the name rule). |
| `avatar/menu/AvatarActionMenu` | `open(ServerPlayer, AvatarEntity[, List<String> info])`: creator-only, sends the packet that opens the Name / Info screen. |
| `avatar/net/*` | `AvatarNet.register`, 7 payloads (C2S roll / finalize / name, S2C roll / races / result / open-actions), `AvatarNetServer` (all validation), `RaceEntry` (+`RaceEntryFactory`, the single seam to AV1's race records), `RollLimiter`, `NetText`, `AvatarNetConfig`. |
| `client/avatar/PlayerAvatarClient` | Client entry: renderer, menu screen, cache clears (resource reload, logout), the particle tick (dark red smoke in frenzy, aura motes otherwise). |
| `client/avatar/PlayerAvatarRenderer`, `AvatarFeatureLayer`, `AvatarEyesLayer`, `AvatarParts`, `AvatarSkinCache`, `AvatarLooks` | The renderer: a player-shaped body whose skin is composed once per (body group, skin tone, outfit, eye colour), GeoLite parts for hair / horns / wings / outfit pieces, emissive eyes (red and pulsing in frenzy), an aura ring on the ground tinted by the aura colour. |
| `client/avatar/AvatarBuilderScreen`, `AvatarPreview`, `ValueSlider`, `AvatarActionScreen`, `AvatarClientData`, `AvatarClientNet` | The 4-page screen (stats with an animated roll, race and personality, appearance with live 3D preview, confirm), the entity-less preview, the glass slider, the Name / Info screen, and the client end of the packets. |

### Deviations from the contract (and why)
* The renderer is `PlayerAvatarRenderer` and the client entry is `PlayerAvatarClient.register(IEventBus)`: `client/avatar/AvatarRenderer` and `AvatarClient` already belong to the Devil Avatar boss, and a second `register` overload in `AvatarClient` would make `modEventBus.addListener(AvatarClient::register)` in NUSMP ambiguous.
* GeckoLib is not used (the mod does not depend on it): the repo's GeoLite loader draws the same Blockbench `.geo.json` models with animation clips and glow maps; the body animates through the vanilla humanoid model. `AvatarEntity` is not a GeoEntity.
* `S2C_RollPayload.rerollCost` is filled from `AvatarConfig.rerollCostMagicule()`; the screen shows the line only when it is above 0.
* `C2S_FinalizeAvatarPayload` carries the roll token and the server calls the token overload of `AvatarCreation.finalizeAvatar` (a stale screen is refused).
* The real entity is NOT scaled by the renderer: AV1 sets the vanilla `SCALE` attribute from `appearance().scale()`, which scales hitbox and model. Only the GUI preview scales by the look (no entity there); the aura ring and particles read `getScale()`.
* A refused roll (`Roll.ok()` false) sends a refusal message instead of a roll.

## How a creation goes
1. Genesis Core -> `AvatarBuilderMenu.open(sp)` -> vanilla open-menu packet + `S2C_RacesPayload` (all races, each with `available` and the failed connection conditions from `RaceConnectionChecker`).
2. Dice button -> `C2S_RequestStatRollPayload` (empty). The server checks the open menu and the rate limit (`avatar_net.rollGapMillis`, 500 ms), calls `AvatarCreation.rollStats`, answers `S2C_RollPayload`.
3. Create -> `C2S_FinalizeAvatarPayload(raceId, primary, secondary, appearance NBT, name)`. The server cleans every string, resolves the race and personalities by name, runs `DynamicAppearanceEngine.sanitize` and calls `AvatarCreation.finalizeAvatar`; the answer comes as `S2C_CreationResultPayload` (reason shown in red on refusal); on success the menu is closed.
4. Right-click your own Avatar with an empty hand -> AV1 calls `AvatarActionMenu.open` -> Name / Info screen. Naming sends `C2S_NameAvatarPayload`; the server checks the Avatar is the sender's and within `nameReach` (12) blocks, rate-limits (1 s), calls `AvatarCreation.nameAvatar`. Appearance cannot be changed after creation (no action for it, by design).

## Assets (`python3 -B tools/avatar/gen_avatar_client_assets.py`, `--check`, `--preview`)
16 hairs (some with swaying tails / braid), 8 horns (some glow), 6 wings (angel, bat, dragon, fairy, fallen angel, crystal; flapping clip), 7 outfit 3D extras (pauldrons, capes, tails, apron), the aura ring, 3 body templates + masks, 12 outfit overlays, the eye glow map. Deterministic (checked: the second run is byte-identical).
Previews: `tools/render_preview/scenes.d/avatar.json` (38 scenes, each part on the preview's head/body stand) and contact sheets; the preview script needed `client/aura/LazyBuffers.java` added to its `REAL_BASE` list (it did not compile without it; additive fix). The composed skins were checked by running the real `SkinCompose` over the real PNGs for 12 outfit x group x tone combinations.
Honest look: the hairs read as blocky stylised styles (cap, spikes, ponytail, twin tails, bob, bun, mohawk, wild, side-swept, braid, ahoge, drills, pompadour, long layered, hime); horns and wings are readable but simple (the bat and dragon wings look like fans of panels, not membranes); the face on the 64x64 skin is tiny (2x2 pixel eyes). It has not been seen on a real model in the game.

## Config
`AvatarNetConfig` (own spec, the lead registers it, SERVER): `avatar_net.rollGapMillis` 500, `avatar_net.nameGapMillis` 1000, `avatar_net.nameReach` 12. The on/off switch is AV1's `avatar.enabled`.

## Tests (`src/test/java/.../avatar/{appearance,menu,net}`)
`AppearanceTest` (NBT round trip, missing keys, clamps, scale to race +-15%, NaN, idempotence, seeded defaults, race groups, every id has its asset file), `SkinComposeTest`, `TextureLruTest`, `BuilderStateTest`, `NetGuardsTest` (rate limit, text cleaning). 30 tests, all green with a fake `CompoundTag` (the stub jar's NBT throws). Payload StreamCodec round trips were NOT written: the codecs need the real Minecraft classes and there is no precedent for running them in this repo's tests.

## Unverified (read these first when it is first run)
* The whole GUI (layout numbers, widget behaviour, the `AbstractContainerScreen` render order with widgets over `renderBg`, the 3D preview pose and lighting in `AvatarPreview`) is unseen.
* `AvatarPreview`: model origin at the neck, feet 1.501 blocks below, rotated 180 degrees about Y for the GUI's z direction; if the figure is upside down or backwards, flip there only.
* Part placement in the world relies on `model.head/body.translateAndRotate` plus GeoLite's PLAYER space (verified in the headless preview against the preview's figure, not against the real model).
* `ServerLevel.getEntity(UUID)`, `ClientLevel.entitiesForRendering()`, `TextureManager.release(ResourceLocation)`, `Lighting.setupForEntityInInventory()` are in the API dump but were not compiled (stub gaps).
* Type-checked against AV1's real classes (av1-wt): `RaceEntryFactory` uses the real `AvatarRace` accessors.
* No AV1 stubs are in the repo.
