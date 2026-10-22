# Sylph (Black Clover): Yuno's wind Spirit Lord as a small winged girl (key `sylph`)

Owner request: "remake the Spirit lord Sylph but add something so it doesn't sound like the basic one in base tensura ... try to make it as close as the image as possible". Read as BOTH *look* and *sound*: she must not look or sound like the basic Tensura Spirit Lord. Later addition from the owner: she must be SMALL, as in the anime.

Nothing here was seen or heard in a game. Every picture is the project's headless rasteriser (section 11), every sound was checked by its numbers and spectrogram only (section 12).

The whole feature is one **server** config switch: `magic.sylph` in `nusmp-sylph-server.toml` (default `true`; `false` gives back the old generic casting for everybody and the older fairy model of the exclusive Sylph).

## 1. What she is

| | |
|---|---|
| **Small form** (the default) | A fairy girl about **0.55 blocks** tall (hit box 0.4 x 0.6, drawn at 0.24 of the model's 1/16-block units), floating beside the caster's right shoulder, level with the head. She laughs, points, and flies after the caster. Every Wind page of the caster's grimoire leaves her outstretched hand. |
| **Powered form** | A bigger Sylph, about **1.8 blocks** tall with her hair streaming up (body about 1.2), wings spreading about **2.5 blocks**, hit box 0.9 x 1.7, hovering **above and behind** the caster (1.5 behind, 2.9 up). It comes for the big pages (cost tier 20 and up: Spirit Storm, Towering Tornado, Slicing Wind Emperor, Spirit of Zephyr) for 80 ticks (200 for the Spirit of Zephyr), at most once per 300 ticks per caster, one per caster and 6 on the whole server, linked or not. The page's spell leaves her hands. The small form hides while she is out. |
| **Who** | The caster whose grimoire is bound to the canon book of **Yuno** (the same lock as the Spirit of Zephyr page, `CharacterSpells`). Every other caster, and every caster with `magic.sylph` off, casts the Wind pages exactly as before. |
| **Link** | A key (`key.nusmp.sylph`, right bracket by default, rebindable) links or unlinks her. The server decides (Yuno's grimoire, the switch, one ask per half second). Cleanup on logout, death, respawn, dimension change and server stop. |
| **The exclusive Spirit Lord** | The exclusive `ElementalSpiritLordEntity` of kind SYLPH (one mage on the server holds it) is drawn with the same new small model when the switch is on; its hit box is now 0.4 x 0.6 (was 0.35 x 0.45). The Tensura-style orb Spirit Lord (`SpiritLordEntity`, kind SYLPH) now gusts with her sound set when it casts. |
| **Aura** | While she is linked she gives the caster Slow Falling (refreshed every 100 ticks, 11 seconds), as the exclusive Sylph already does. |

## 2. The look (from the three pictures)

Real 3D models built by `tools/gen_sylph_geo.py` with hand-painted sheets from `tools/sylph_art.py` (an anime cel look: flat colours, a hard shadow band, dark line work painted into the texture, made with PIL at 4x and reduced; deterministic). Drawn by `client/prop/SylphDraw`, previewed by `tools/render_preview` (scenes `tools/render_preview/scenes.d/sylph.json`, written by `tools/sylph_scenes.py`).

* **Face**: three faces as three thin plates on the head (a clip shows one, the others are scaled to 0.001): *laugh* (closed arc eyes, blush, a wide open mouth: picture 1), *smile* (pale green open eyes with catch-lights, an open smile: picture 2), *stern* (green narrow eyes, brows slanting down to the nose, a small flat mouth: picture 3); the powered form has *stern*, *shout* and *smile*. Pointed ears, choker.
* **Hair**: a cap with a ragged fringe, a bob with cheek locks and nape locks that sway (separate cards), the **ahoge** as a five-card curl. The powered form has five long ribbons of four cards each that bend as they rise (S-curves) and sway.
* **Dress**: a scalloped strapless bodice (two shell cups), and a skirt of two rings of ruffled panels (ten each, scalloped hems cut by alpha) that flare and sway; the powered form adds six translucent **petal wings** at the hip.
* **Wings**: four dragonfly wings (two pairs), each in two segments (a joint the clips whip), a translucent membrane with a soft iridescent sheen, a few big cell veins, a darker leading edge, drawn translucent plus an additive glow map of the veins tinted by a slowly wandering green-aqua film. A soft green halo (one camera-facing additive square, two layers) behind her.
* **Cel shading kept**: `client/prop/FlatLit` hands every vertex to the consumer with an upward normal, so the game's directional diffuse term (a front face gets about 0.74, a side face about 0.5) does not grey the painted skin; the lightmap still applies. The preview prints a `NORMAL_MISMATCH` warning for it: it is deliberate, and the render types are no-cull.

### 2.1 Animations (`animations/entity/sylph_small.animation.json`, `sylph_powered.animation.json`; bone rotation only)

| Form | Clip | What |
|---|---|---|
| small | `idle` | floating, seated and leaning back, one knee up, a hand at the cheek, the shoulders shaking with a laugh (picture 1); wings, skirt, hair and ahoge drift |
| small | `laugh` | a bigger fit of the same pose, played for the first 80 ticks of every 600 tick cycle; the server's giggle sound starts on the same tick |
| small | `point` | standing, both index fingers at her head, a bright smile, a hop (picture 2) |
| small | `cast` | arm thrust forward, palm open, hair and skirt blowing back; played 24 ticks after each cast |
| small | `fly` | leaning forward, legs trailing, wings beating fast, while the caster moves |
| powered | `hover` | arms spread, legs apart, hair streaming up, wings beating slowly, a stern face (picture 3) |
| powered | `powered_cast` | arms forward and up, the head lifted, the mouth open, wings spread wide; 30 ticks after each cast |
| powered | `shout` | the hover pose with the mouth open: the first 16 ticks after she arrives |

## 3. Levels of detail and the exact quads per frame

Counted from the model files (`SylphLodTest` reads them, so these numbers cannot drift from the files). Cost of one Sylph = body + wings (all levels) + glow-only model and halo (the two nearer levels).

| Form | Level | When (distance, best quality) | Body | Wings | Glow-only | Halo | **Per Sylph** |
|---|---|---|---|---|---|---|---|
| small | full | up to 12 blocks | 365 | 16 | 23 | 2 | **406** |
| small | half | up to 28 | 199 | 16 | 23 | 2 | **240** |
| small | low | up to 64 | 66 | 16 | 0 | 0 | **82** |
| small | culled | beyond 64 | | | | | 0 |
| powered | full | up to 32 | 381 | 28 | 23 | 2 | **434** |
| powered | half | up to 64 | 229 | 28 | 23 | 2 | **282** |
| powered | low | up to 112 | 70 | 28 | 0 | 0 | **98** |
| powered | culled | beyond 112 | | | | | 0 |

The small fairy covers few pixels, so her bands are shorter than the powered form's (a smaller thing needs its detail only close up). The effect quality (`PerfSettings.vfxQuality().detail`, 0.5 to 1) scales every band. **Shared per-frame budget** `SylphLod.FRAME_QUADS = 3200` for every Sylph on screen together (the linked ones, the powered ones and the exclusive Spirit Lord body all go through `client/prop/SylphView`): the nearest are drawn at the level their distance asks for, each is stepped down a level until it fits, then culled; **the viewer's own Sylph is never dropped** (at worst the lowest level, booked past the cap). The owner's brief gave the allowance of about 800 / 1000 quads at full detail, about 350 / 450 at level 1 and at most 140 at level 2: the models use about half of it at full detail and stay under it at every level on purpose (the likeness is spent in the texture, not in the geometry). Textures: the colour sheet per form (768 or 1024 wide), `_lod1` half and `_lod2` quarter size, a wing sheet and a wing glow map, a half size glow-only sheet, one 128 px halo.

## 4. The sound set (a NEW one: not the basic Tensura spirit's)

Eight cues, **synthesised from nothing** by `tools/gen_sylph_sounds.py` (numpy plus the DSP kit of `tools/gen_mob_sounds.py`, ffmpeg libvorbis, mono 44.1 kHz, deterministic seeds), 20 files, about 340 KB, 0.5 to 2.1 s each. Registered as `nusmp:sylph.<cue>` by `sylph/SylphSounds` (a fifth sound register, in a loop from the catalogue `sylph/SylphSoundSet`), `sounds.json` entries appended at the end (nothing else touched), subtitles in `tools/lang/sylph.json`.

| Cue | Files | What it is | When the server plays it |
|---|---|---|---|
| `link` | 2 | a glittering run of wind chimes (struck metal tubes, inharmonic partials) over sparkle dust | she is linked |
| `unlink` | 2 | the run falling away | she is unlinked |
| `chime` | 3 | one to three soft chimes, long decay | now and then (every 10 to 20 s) while she floats |
| `flutter` | 3 | the fast thin flutter of dragonfly wings (a 66 to 82 Hz beat on band-passed air) | the caster moves fast (at most every 2.5 s) |
| `giggle` | 3 | four to six quick bell-voiced "hee" notes that climb and tumble, a breath of noise, glitter after | the start of her laugh clip (once per 30 s cycle) |
| `gust` | 3 | a gust whooshing out of her hand with a flick of chimes | each Wind page she casts (also the exclusive Sylph and the orb Spirit Lord) |
| `surge` | 2 | a rising swell of wind and bells | the powered form arrives |
| `gale` | 2 | a deep whoosh with a howl and a ring of chimes | the big page the powered form casts |

**Server driven, nearby players only, budgeted** (the `VoiceLimiter` pattern of the spoken lines): `SylphSounds.play` walks the level's players, skips those further than 32 blocks, asks a per listener `VoiceLimiter` (a cooldown per cue, at most **3 Sylph sounds a second per listener**, never two within four ticks) and sends the allowed ones one `ClientboundSoundPacket`. The limiter record of a listener is dropped on logout.

## 5. The cast flow (the server decides, the client draws)

```
GrimoireBook.castPage(player, page):                  # existing checks (cooldown, mana, locks) unchanged
    ...
    SafeCast.run(  SylphCasting.run(book, page, player, mode,  () -> FireCasting.run(...))  )
SylphCasting.run:
    not the Wind book, not a family page, switch off, or not Yuno's grimoire  -> inner cast, untouched
    big page (cost >= 20) and powered form allowed (cooldown, caps)           -> spawn powered prop; emitter = her hands
    else linked and the small prop alive                                      -> stamp the cast on the prop; emitter = her hand
    no emitter                                                                -> inner cast, untouched
    VfxSpawn WIND_GALE burst at the emitter (nearby only), sound gust / gale, then withEmitter(hand, inner cast)
origin(caster, start):                                # SpellRuntime.Bolt, TensuraShots, GrimoireBook.vfx, WindBook lane / emperor VFX
    the start is moved to the emitter only if it was the caster's own start (within 2.2 of the eye or feet), and the emitter
    is kept within 4.6 of the eye, so a spell still goes where the caster aims
```

The emitter moves the start of the **bolts and effects**; the hits of the area pages (Gust Lane, Spirit Storm, Towering Tornado, Slicing Wind Emperor) are still the caster's, so no gameplay number changes (their pictures leave her hands, the damage is computed where it always was). The Spirit of Zephyr page keeps its Spirit Dive armour and all its effects; the powered form arrives with it and a gale bursts from her hands.

## 6. Worst-case cost (rule 11)

Server (defaults of `SylphConfig`, all lowerable by the owner):

| Thing | Per caster | Server-wide cap | Lifetime | Notes |
|---|---|---|---|---|
| small Sylph (prop entities) | 1 | 48 (`caps.maxSpirits`) | while linked | a position nudge every 5 ticks |
| powered Sylph (prop entities) | 1 | 6 (`caps.maxPowered`) | 80 ticks (200 for Zephyr), then 300 ticks cooldown | the small form is hidden meanwhile, so one visible body per caster |

Worst case entities from this feature: 48 + 6 = 54 prop entities (a caster has at most one of each, and only Yuno's casters can have any). Per tick: the upkeep runs every fourth tick and only for the players that have a Sylph out (`LIVE` set); no world scan, no chunk load; one `getEntity` lookup per live Sylph per four ticks. Packets: client to server one byte `Key` at most every 10 ticks, validated (action in range, sender a real player, Yuno, switch on); to clients only the ordinary prop traffic (a position every 5 ticks per live prop, a param update per cast), the VFX burst of a cast (nearby only, through `VfxSpawn`), and sounds (at most 3 a second per listener, within 32 blocks). Per listener a chime about every 15 s per Sylph in earshot.

Client per frame: at most **3200 quads** for all Sylphs together (section 3); one Sylph at full detail is 406 or 434 quads; at most five draw calls per Sylph (wings, wing glow, body, glow-only model, halo); nothing allocated per frame in the painter (primitive math; the one `LazyBuffers` wrapper the other prop painters also use); she moves by bone rotation only; no particles at all.

## 7. NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | the whole `sylph` package (rules, LOD maths, ledger, casting, props, net, events, config, sound set and sounds, self-test), `PropKind.SYLPH_1`, the key and its packet, `client/prop/SylphDraw`, `SylphPropPainter`, `SylphView`, `FlatLit`, `client/sylph/SylphClient`, 10 geo models and 2 animation files, their sheets, the halo, 20 sound files and 8 `sounds.json` entries, the generators (`gen_sylph_geo.py`, `sylph_art.py`, `sylph_geo_lib.py`, `gen_sylph_sounds.py`, `sylph_scenes.py`, `sylph_compare.py`), the preview example and scenes, tests, this doc, the lang fragment |
| REPLACED | **the look** of the exclusive Spirit Lord Sylph when `magic.sylph` is on (the honeycomb-winged fairy of 1.99.0 is no longer drawn by default); its hit box grew from 0.35 x 0.45 to 0.4 x 0.6; the way Yuno's Wind pages leave the caster (from her hand, not the caster's own hands) |
| KEPT | every Wind page id, order, number, cooldown and the Spirit of Zephyr lock and Spirit Dive armour; the entity and prop ids (`ElementalSpiritLordEntity` kind SYLPH, `SpiritLordEntity` kind SYLPH, `WindSpiritLordEntity`); the older Sylph models and textures (`spirit_sylph*`, `spirit_sylph_fairy*`, `arc_spirit_sylph`), still drawn when the switch is off (`GreatSpiritModels.sylph`); every other caster's casting; all other `sounds.json` entries byte for byte |

Existing files touched (hooks and append-only one-liners): `book/GrimoireBook` (the cast path calls `SylphCasting.run` around the fire casting hook; the spell VFX start goes through `SylphCasting.origin`), `book/SpellRuntime` and `book/TensuraShots` (bolt / shot start through `SylphCasting.origin`), `book/WindBook` (the pictures of Gust Lane and the Wind Emperor start at her hand), `prop/PropKind` (+`SYLPH_1`, appended), `prop/PropRegistry`, `client/prop/PropPainterRegistry`, `NUSMP` (two boot lines), `entity/ElementalSpiritLordEntity` (hit box, her sounds), `entity/SpiritLordEntity` (her gust), `client/ElementalSpiritRenderer` (the new small model), `sounds.json` (8 entries appended), `tools/safe_additive/scan_textures.py` and `SafeAdditiveTextureScanTest` (the Sylph glow sheets joined the additive scan).

## 8. Tests

`SylphRulesTest`, `SylphLodTest`, `SylphAssetsTest`, `SylphSoundsTest`, `SylphSourceScanTest` (pure JUnit and source scans: the server decides and the client only draws, nothing scans the world on a tick, every packet validates its sender, sounds go through the limiter to nearby players, the cast path still calls the hook, the Wind pages and their lock are untouched, nothing is allocated per frame, a unique config file name, the drawn sizes match the asset check); self-test key **`magic.sylph`** (`SylphSelfTests`, in the CI server smoke test: sound events, files, models, the Wind book, the rules, and on a stand-in player the link spawns, is counted, is capped and is gone after the player is forgotten); the guards that scan it: `GeoAssetsTest`, `SafeAdditiveTextureScanTest` (scope `entity`: the glow sheets), `FxSourceScanTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `SoundsJsonTest`, `SpiritLooksTest`.

## 9. Numbers of the three pictures vs ours (what differs, honestly)

See `docs/previews/sylph/sylph_vs_reference.png` (each picture beside its render) and `sylph_scale.png` (both forms next to a standing player).

* **Picture 1** (the laughing fairy): the closed arc eyes, blush, ahoge, open laughing mouth, ruffled lime dress, small translucent wings and the green glow match. Differs: she sits on the air with one knee up in the reference, ours leans back with a bent leg but is not truly seated; her skirt is a huge wavy trailing sheet, ours is a ring of ruffles; her hair is a soft pointed bob, ours a boxy cap; limbs are boxes, not curved.
* **Picture 2** (pointing at her head): the pale green eyes, open smile, ragged blonde-green hair, choker, scalloped strapless top and long iridescent wings are all there. Differs: the hair is windblown in a flowing mass in the reference, ours is the bob; the dress is nearly white in the reference, ours a pale lime; the wings are broad and rainbow-sheened in the reference, ours a flatter pastel; her pose (one foot kicked up) is not matched.
* **Picture 3** (the powered form): the stern face, hair streaming up in S-curves, big green veined wings, petal skirt, spread arms and legs, intense green glow match in layout. Differs: her hair is a smooth glossy mass of locks, ours five stiff ribbons; the wings have more, finer cells and a stronger iridescence; the petal skirt is far fuller; her proportions are slender and long-limbed, ours chunkier; the glow is a halo square, not a lit body.
* The **head is a box** (with hair cards over its corners), which no texture hides from the side; hands are blocks with three finger bars.

## 10. Sources (rule 10)

English wiki first: `https://blackclover.fandom.com/wiki/Sylph` **could not be fetched** in this session (`getaddrinfo ENOTFOUND blackclover.fandom.com`: no network route to fandom.com). Japanese fallback: `https://blackclover.fandom.com/ja/wiki/シルフ` and `https://ja.wikipedia.org/wiki/ブラッククローバーの登場人物` **could not be fetched either** (same error). So the design comes from the three pictures the owner supplied (a small glowing green fairy girl with a curled ahoge, pointed ears, a ruffled green dress, small wings; the same with pale green eyes, a white-mint scalloped dress, a choker and long iridescent dragonfly wings; the powered form with streaming hair, a stern face and big veined green wings) and from what the repository already says of her (Yuno's wind Spirit Lord, the Wind book, the Spirit of Zephyr page and its Spirit Dive armour). No wiki text was used; the numbers (sizes, cooldowns, caps) are this mod's own.

## 11. Not verified (honest list)

* **Nothing was run in a game or on a server.** The type check was against API stubs and the JUnit run in a shim without Minecraft; tests that need the game classes did not run and are left to CI.
* **Nothing was heard.** The sounds were checked by their numbers (duration, peak, loudness) and spectrograms; how convincing the chimes, the giggle and the gusts are is the owner's to judge; every recipe is a few lines of parameters in `tools/gen_sylph_sounds.py` and can be retuned.
* **The look only approximates the pictures** (section 9). The animation signs (which way a rotation turns) were checked only in the headless preview.
* The `FlatLit` trick (an upward normal on every vertex of a no-cull render type) is proven only in the preview's rasteriser; in the real shader it should give a diffuse term of 1, but it was never seen in a game. If it misbehaves, drawing the body through `SylphDraw.pass(..., flat=false)` gives the ordinary lit look.
* The wing glow tint, the halo size and the hover heights (0.7 right, 1.95 up for the small form; 1.5 behind, 2.9 up for the powered one) are estimates from the pictures.
* The default key (right bracket) is a guess and may clash with a pack's keys; it is rebindable.
* `magic.sylph` is read by the client renderer through NeoForge's server config sync; a client that has not received the config yet draws the default (on).
* Balance feel (the powered form's duration and cooldown, the Slow Falling aura) is untested. The area pages' hits are still the caster's, only their pictures leave her hands.
* Spirit commands are limited to the link toggle, the aura and the emitter (no AI follow, no attack commands): she is a prop, deliberately cheaper than a per-caster AI mob.
