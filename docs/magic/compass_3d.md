# Compass Magic in 3D: one floating compass, every spell comes out of it

Owner request: "Compass Magic should just be a single 3D model compass floating like the anime and all the spells come out of it as 3D models" (reference: the Black Clover wiki's Compass Magic page and the anime's
compasses: big flat discs floating beside the caster, a thick olive-bronze rim, green concentric rings with a pale cream and gold 8 point compass rose, a thin gold needle with a small ring at its tail, a glassy glint
on the rim, a spiky yellow aura behind; the grimoire cover is a green book with a gold-rimmed compass rose).

Nothing here has been run in a game (see "Not verified"). Everything below was built and checked from files, a type-check against API stubs, pure JUnit tests and a headless re-implementation of the renderer.

## What it is now

* **One compass per caster.** A real 3D model (`compass3d.CompassMesh`, 576 + 94 + 128 quads: rim, dial, rose, needle, glass), prop `COMPASS_1`. Thick bronze rim with a back dome, bevels, 16 engraved notches, a bright
  front lip, a gold glass seat and four lugs; a recessed green dial with gold ring lines and degree ticks; a raised 8 point cream and gold rose (every point two facets, lit from the upper left, an outline underlay);
  a gold needle (shaft with a ridge, hub cap, a small ring at the tail) that is a spring: it swings to the charted foe (or to where the caster looks) with a little overshoot; a glass dome with a crescent highlight.
  Drawn with a bob, a slow sway and a slight roll, popped in over 10 ticks, faded out over 14.
  * Glow passes through `SafeAdditive` only: the dial's gold lines and the rose and needle (full bright), a spiky yellow aura card that always sits on the far side of the compass from the camera and turns slowly,
    a glint (four point flare) that runs round the rim. Custom particles (`particle/fx`, theme `magic.COMPASS`, tinted gold, through `Fx` and its budgets) at its appearance and end and from its needles.
  * It floats at the caster's right, a little behind and up (`CompassRules.SIDE/UP/BACK`), turned 25 degrees outward, follows with an ease. It appears when a Compass page is cast or held (charging), stays 8 seconds
    after the last one (the long pages keep it for their whole effect) and fades. Never more than one per player.
  * Modes (set by the page, `CompassRules.MODE_*`): attack (Useless North, Compass Rose: brighter glow and aura), guard (Willful Compass: two turning field rings round it), chart (Another Atlas: stronger aura, a slow
    outer ring).
* **Every spell comes out of it as 3D models** (`CompassKit`, prop `COMPASS_2`, variants in the low byte of the param):

| Spell (page) | What comes out of the compass | Server work |
|---|---|---|
| Useless North (starter) | 3 to 5+ **arrow-needles** (gold shaft, four fins, a ring at the tail, rolling about their axis) leave the compass and ride the page's own homing bolts to the foe; a **seal disc** (a small compass lying flat) flies out and turns at the foe's feet | the page's `SpellRuntime.bolt` is unchanged, `CompassKit.bolt` adds the prop that follows it |
| Willful Compass (mid, 10 s guard) | the compass guards (field rings); each shot that is bent back leaves a **turn disc** (a small compass facing the camera, its needle swinging at the shooter) | the page's zone is unchanged, `CompassKit.turnDisc` per bent shot |
| Another Atlas (mid, 15 s chart) | a **route** of marching 3D dashes and arrow heads from the compass to the charted foe, a seal disc turning at the foe's feet, an arrow-needle every 1.5 s | `CompassKit.route`, `seal`, `bolt` |
| Compass Rose (signature) | a **seal disc** flies out to each foe within 20 blocks (up to 10), two arrow-needles per foe; the compass spins up | `CompassKit.seal`, `bolt` |

  Gameplay is exactly the old one: page ids, order, costs, cooldowns, masteries, damage, speeds, radii, targeting, the redirect rule and the mark are untouched. The only gameplay-adjacent change is where a needle
  starts: the compass centre instead of a point on the old flat arc behind the caster (same neighbourhood, 1 to 2 blocks).
* **Caps per player** (`CompassRules`): 24 living arrow-needles, 10 seal discs, 6 turn discs, 2 routes (a new route replaces the oldest), one compass. Seals, turn discs also take the shared effect budget
  (`PerfBudgets.effectPacket`). A prop that cannot be spawned just means that effect has no model that time; the page still works.
* **Config** `visual.compass3d` (client, `config/nusmp-compass3d-client.toml`, default on): off draws the old flat effects and none of the 3D props. `visual.compass3d_aura` (default on) is the aura and the glint.
  The server sends both sets (the old `VfxSpawn` payloads as before, the props as entities); the client chooses. Looks only.

## NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `compass3d/CompassMesh` (the model), `CompassRules` (pure rules), `CompassKit` (server: one compass, arrow-needles, seals, turn discs, routes, caps), `CompassVisualConfig`, `CompassSelfTests`; `client/compass/CompassRender`; the filled `client/prop/CompassPropPainter` and `prop/CompassProps` (they were stubs); textures `entity/compass3d/{rim,face,face_glow,star,needle,glass,aura,glint,seal_ring}.png` and `tools/compass/*` (generator, layout, mesh dump, preview); tests; this page; the lang fragment. |
| REPLACED (visual only, by default) | The flat Compass look: `COMPASS_ARRAY` (several brass compasses in an arc), `COMPASS_NEEDLE` (a flat needle) and `COMPASS_LOCK` (a flat sigil) are no longer drawn while `visual.compass3d` is on; the one 3D compass, the arrow-needles, the seal discs, the route and the turn disc are drawn instead. Where a needle starts (the compass, not the old arc). |
| KEPT | Every page (ids, order, cost, cooldown, damage, mastery bits), `CompassExt`, `COMPASS_FX1..3`, `CompassLayer`, `CompassAura`, `behind()`, the old flat effect code in `SlashCompassMercuryLayer` (still drawn when the config is off), the old textures `compass_rim/dial/needle/glass`, the grimoire cover, the Time Magic VFX (untouched). |

## Where things are

* Model: `src/main/java/com/newuniverse/nusmp/compass3d/CompassMesh.java` (plain geometry, no Minecraft class; quads of 23 floats: four corners x y z u v, then the normal; layers RIM, FACE, STAR, NEEDLE, GLASS).
  The numbers it shares with the texture generator are in `tools/compass/compass3d_layout.json` (a test checks they agree).
* Textures: `python3 -B tools/compass/gen_compass3d_textures.py` (deterministic, seeded; every additive texture has no colour under a transparent texel).
* Preview without the game: `python3 -B tools/compass/preview_compass3d.py` (compiles `CompassMesh` with `tools/compass/MeshDump.java`, rasterises the real quads with a z-buffer) writes `docs/previews/compass_3d/`:
  `compass_views.png` (front, three quarter, side, back), `caster.png` (beside a caster, three views), `spells.png` (arrow-needle, route, seal disc), `casting.png` (Useless North and Compass Rose with a caster, foes, needles in flight and seal discs).
* Server: `compass3d/CompassKit` (behaviours registered by `prop/CompassProps.init()`), rules `compass3d/CompassRules`. Client: `client/prop/CompassPropPainter`, `client/compass/CompassRender`.
* Edits to existing files (all additions): `book/CompassBook` (calls to `CompassKit` in the four pages, `onHeld`, the needle helper; `behind()` kept), `vfx/client/layer/SlashCompassMercuryLayer` (an early return for the three flat shapes while the config is on),
  `NUSMP` (one `registerConfig` line), `prop/CompassProps` and `client/prop/CompassPropPainter` (filled in; they were stubs).

## Rule 9 (server and multiplayer)

All state and decisions are on the server (`CompassKit`); the client only draws (`CompassPropPainter` reads the synced prop word, the synced target and the synced rotation). State is per player UUID in small maps
cleaned when a prop ends or is found removed; a bolt that was dissolved by another spell ends its model after two still ticks. No static "current player", no `Minecraft.getInstance()` outside `client/`, no new packet (props are the existing entity), props
are tracked to nearby players by the existing entity tracking (12 chunks), particles go through `Fx` and `PerfBudgets`. The config is a CLIENT config (looks only).
Self-test (CI server smoke test): `magic.compass_3d` (mesh well formed, prop word round trip, on a real level a stand-in caster gets exactly one compass however often it is asked, the four caps hold, everything is gone after `forget`).

## Tests

`compass3d/CompassRulesTest` (prop word, position, needle angle, swing, fades, caps, route, seal flight), `CompassMeshTest` (layout shared with the generator, sane quads, stacking, outward normals, budgets),
`CompassAssetsTest` (textures exist with the layout's sizes, no colour under alpha 0, no black seam texels, the dial is green and the rose cream and gold, the renderer names only existing textures, the lang fragment). The existing
`ServerSafetyTest`, `SafeAdditiveSourceScanTest`, `SafeAdditiveTextureScanTest`, `ScreenBlurGuardTest`, `SquareBugAssetsTest`, `NoHungerDrainTest` still pass in the shim.

## Canon

The repo's own record of the wiki (the `CompassBook` header, written for 0.45) names: Useless North (offensive: the compasses change the trajectory of the user's attacks so they lock on and follow a target "much like a compass
locks on to magnetic north"), Willful Compass (defensive: bends incoming attacks back), Another Atlas (supplementary: marks a target's magicule signature). Compass Rose is the mod's appended signature. Letoile Becquerel and Kivn are the named users there.
The look follows the owner's description of the anime (above).

## Not verified (nothing could be run)

* **No frame of the game was seen.** The preview PNGs are a re-implementation of the renderer (z-buffer, nearest sampling, simple lighting, additive and alpha blends); the real lighting, blending order and depth behaviour can differ.
* **The Black Clover wiki could not be reached** from the build environment (the proxy refused the host), so the spell list is the one already in the repo (three wiki pages plus Compass Rose); other Compass spells on the wiki page, if any, were not read and have no 3D model.
* **Render-thread behaviour is reasoned, not run:** each pass fetches its buffer and writes it at once (the shared buffer source ends the previous batch when another render type is asked for); entity render types with the custom textures; the spring of the needle; the aura card side switch;
  the translucent fade of the solid passes; the glow offsets (z-fighting at distance). The type-check is against API stubs.
* **Needle model scale in flight and the route** (arrow 0.5 of model size, dashes 0.07 wide) were chosen from the previews; how they read at 20 blocks in a real scene was not seen.
* **First person:** the compass floats about 1.3 blocks to the right and slightly behind; whether it is in the way or visible enough was not checked.
* **Performance** was not measured. One compass is 700 to 900 quads drawn through the pose stack each frame with no cache; the cap is one per caster, but many casters in one place were not tried.
* **Network:** the server still sends the old flat `VfxSpawn` payloads next to the props (the client skips them when `visual.compass3d` is on); this doubles those effect packets for the Compass pages (they were never many).
* **The compass appears on a cast or a hold,** not merely when the grimoire is selected; if "the Compass book is active" should also mean selected, that needs a hook that was not added.
* A bolt dissolved by another spell (`SpellRuntime.dissolveBolts`) leaves its arrow-needle model for two ticks before it ends; a dimension change drops the compass and the next cast spawns a new one.
* `MobEffectInstance is abstract` in the type-check is a stub artefact in the unchanged `anotherAtlas` line.
