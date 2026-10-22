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
* **Wings** (remade, see section 2.2): four long, narrow, tapered dragonfly wings (a large front pair and a smaller rear pair), each a chain of four (small form) or five (powered form) flat cards cut out of ONE painted wing picture, a painted vein network, a pale translucent membrane and an iridescent pastel sheen that sweeps over it. A soft green halo (one camera-facing additive square, two layers) behind her.
* **Cel shading kept**: `client/prop/FlatLit` hands every vertex to the consumer with an upward normal, so the game's directional diffuse term (a front face gets about 0.74, a side face about 0.5) does not grey the painted skin; the lightmap still applies. The preview prints a `NORMAL_MISMATCH` warning for it: it is deliberate, and the render types are no-cull.

### 2.1 Animations (`animations/entity/sylph_small.animation.json`, `sylph_powered.animation.json`; bone rotation, and the scale of a wing's root card)

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
| small | `swing` | NEW (1.99.0): sitting on the air and swinging both legs, a hand at the cheek, a bright smile (the first picture's seat) |
| small | `wave` | NEW: one arm high and waving, a little hop, a smile |
| small | `crossed` | NEW: hovering with the arms folded, the head tipped, a smug smirk |
| small | `pout` | NEW: turned away, arms folded, the head down and aside, puffed cheeks, a sulky mouth |
| small | `victory` | NEW: both arms thrown up, a big hop, the head back, a laugh |
| small | `sleepy` | NEW: curled up, knees to the chest, the head nodding, small slow wings, a yawn |
| small | `startled` | NEW: thrown back in the air, arms flung out, legs kicked up, wings flared, huge eyes and a small o |
| small | `hurt` | NEW: a flinch: hunched, a hand pressed to the chest, trembling, an angry face |
| small | `worry` | NEW: hands at the chest, the head tipped, trembling, wings drooping, wet eyes |
| small | `cry` | NEW: both hands at the eyes, the head down, the shoulders shaking, tears and a wide wobbly mouth (the first picture's cry) |
| small | `cheer` | NEW: a fist pumped over the head again and again, the other hand on the hip, a smile |
| powered | `battle` | NEW: the battle stance of the third picture: arms and legs spread wide, a stern face, wings spread wide |
| powered | `fury` | NEW: fists down, the head lowered, an angry face, the wings beating fast |
| powered | `triumph` | NEW: both arms up, the head back, a laugh, wings open |

The loops now run the WHOLE clip: the kit's sine track made one cycle and then held, so the sways of the earlier clips stopped after their first period; `tools/gen_sylph_geo.py` now repeats the cycle over the clip (`sn` / `cyc`), which makes the old idle, laugh, point, cast and fly clips livelier than the first version (their numbers are unchanged).

**Faces** (face plates on the head, a clip shows one and scales the others to nothing: a swap of a texture window, no geometry beyond its two plates): small form 9 (laugh with closed eyes, smile = happy, stern, and NEW smug, angry, sad / crying, surprised, sleepy, pout), powered form 5 (stern, shout, smile, and NEW angry, laugh). Each extra plate is two quads in the full and half models and one in the glow-only model (`docs/previews/sylph/sylph_faces.png`).

### 2.2 The wings (remade in 1.99.0: owner request "remake Sylphs Wings to be like the spirit's images")

Pictures studied: 119 (small, translucent yellow-green wings, short), 120 (long, narrow, tall iridescent wings with pointed tips, a rainbow / pastel sheen and fine cell veins, a large front pair and a smaller rear pair) and 121 (the powered form: big translucent green dragonfly wings, long, tapered, spread wide and angled up, a clear main vein and many fine cross veins forming small cells, a soft green glow). Comparison: `docs/previews/sylph/sylph_wings.png`.

| What | How |
|---|---|
| **Shape** | `tools/sylph_wing_art.py` (table `SPECS`): a long leaf, a rounded base, widest at about half the length, tapering to a point, a nearly straight leading edge with a slight S, the trailing edge the fuller belly. Small form: front wing 30 units long and 7.4 wide (the picture-120 wing rises about one head above her head), rear wing 21 long; powered form: front 54 long and 14 wide (a span of about 2 blocks), rear 36 long. Each wing is a chain of 4 / 5 cards ("wing", "wingo", "wingp", "wingq", "wingr" bones: the first bone keeps its old name) hinged with a small rest curl so the membrane cups a little; the silhouette is the alpha cut-out of the cards. Rolled up from the shoulders (small 60 and 20 degrees, powered 38 and 8) and swept back. |
| **Texture** | hand-tuned by the painter: a main vein along the leading edge (thick at the root, thin at the tip), 5 to 7 long veins out of the root that fork near the margin, a dark pterostigma near the tip, fine cross veins in staggered rows (cells about square), a marginal ring vein, a thin darker outline; the veins are drawn light with a dark edge so they read against a dark or a bright background. The membrane is translucent: pale yellow-green with a pastel rainbow tint (small, alpha 0.34) and a stronger green (powered, alpha 0.62). Sheets: small 640 wide, powered 1024 wide. |
| **Sheen** | `sylph_<form>_wings_glow.png` is now the SHEEN: 6 phases of an additive pastel-rainbow gradient (hue bands plus a brighter band that sweeps along the wing, strongest on the veins), each phase the layout of the wing sheet and masked by the cut-out, black under every transparent texel, stacked top to bottom at half size. `client/prop/SylphDraw.sheen` shows phase `age / 27 ticks` and, on the full level, cross-fades it with the next (two additive passes, a whole sweep about 8 s); the V shift is `client/prop/SheenUv` (a vertex-consumer wrapper, no new model, no allocation). Drawn only through `client/SafeAdditive.additive` (the alpha-weighted additive type; the sheet passes `tools/safe_additive/scan_textures.py`). Half level: the nearest phase only; lowest level: membrane only. |
| **Flutter** | `wing_flutter` in `tools/gen_sylph_geo.py`: the root card flaps (the V opens and closes) and twists a little, the cards further out only bend out of the plane (a rotation about the shared edge never opens a gap between two cards), each a little later than the one before (a wave runs to the tip), the rear pair lags the front pair by a tenth to an eighth of a cycle. Idle: gentle (about 0.8 s a beat), cast and fly: fast (0.2 to 0.25 s), hover 1.4 s. The small form seated and laughing (picture 119) has her wings at 62 percent scale (the scale track of the root card), standing (picture 120) full size. |
| **Kept** | the first-version wings as `sylph_<form>_wings_v1` (geo, sheet and glow, never drawn by default), all bone names of the first version (`wing_f_L` ...), file names, ids, the frame cap, the culling and the ledgers. The petal wings of the skirt (powered form) are unchanged. |

Cards found a mirror bug of the first version on the way: the north / south faces of a wing card were flipped the wrong way round, so the two cards of a first-version wing were each mirrored inside themselves, which is part of why those wings looked broken. The remake has the right flips; the `_v1` copies keep the old look as it was.

### 2.3 Poses, faces and voices (owner addition: "different poses, faces, and voice lines")

What she does follows what happens. The pure rules are `sylph/SylphMood` (event to pose + face + spoken group, with priorities, cooldowns and a chance, per owner, `SylphMoodRulesTest`); the server driver is `sylph/SylphMoods`; the pose goes to the clients in bits 4 to 7 of the prop word (`SylphRules.POSE_SHIFT`, no new packet) and the clients play the clip of that code (the face is part of the clip); the server clears the bits when the mood is over.

| Event (server) | Small pose (face) | Powered pose | Spoken group | Hold / cooldown |
|---|---|---|---|---|
| she is linked | `wave` (smile) | | `link` | 60 ticks |
| she is unlinked | | | `unlink` | |
| a page is cast | the `cast` clip as before | | `cast` (40 percent, or a `giggle` now and then) | line cooldown 200 ticks |
| a big page, no powered form | | | `big` | 300 |
| the powered form arrives | | `battle` (stern) for its stay | `shout` | 200 |
| the owner takes damage | `hurt` (angry) | `fury` | `hurt` (60 percent) | pose 16 ticks, gap 40, line 160 |
| a heavy hit (30 percent of max health at once) | `startled` (surprised) | `fury` | `startle` | 24 ticks |
| a hit that leaves them at 30 percent or less | `worry` (sad) | `fury` | `worry` | 120 ticks, gap 400 |
| the owner kills a monster | `cheer` (smile) | `triumph` (laugh) | `cheer` (60 percent) | 40 ticks |
| the owner kills a boss-sized monster (100 max health or more) | `victory` (laugh) | `triumph` | `victory` | 60 ticks |
| the owner dies | `cry` (sad) for 70 ticks, then she goes | | `sad` | |
| nothing happened for 45 s (no move, cast or fight) | idle rotation `swing`, `sleepy`, `crossed`, `wave`, `pout` | | `idle`, `sleepy`, `smug`, `idle`, `pout` | at most every 40 s; moving ends it |

A pose that is showing holds for its time and is only replaced by a more important one (priority 3 idle, 4 link, 5 cheer, 6 flinch, 7 startle, 8 worry, 10 grief), and an event cannot restart its own pose inside its gap, so she never flickers (a 20 000 event random run is a unit test). The cast clip still comes first (her hand is out). Two server config switches: `magic.moods.poses` and `magic.moods.voices` (both default true; off = the old behaviour).

### 2.4 Voice lines

56 short spoken lines were planned (`tools/audio/sylph_lines.json`, written by `tools/audio/sylph_lines.py`: 16 groups, one to seven lines each: link, unlink, cast, big, shout, hurt, startle, idle, worry, cheer, sad, giggle, pout, smug, sleepy, victory; English; 1 to 3 seconds). They are played through the project's voice system (`audio/voice`: `VoiceCatalog` gained a fifth role `sylph`, appended after the 120 lines; `VoiceSounds` registers `voice.sylph_<group>_<n>`; `Voice.say` and the tiny `VoiceNet.Say` payload; the client's `voice.enabled` and `voice.volume`; the server's `voice.maxPerSecond`). Server driven, players within 32 blocks only (`sylph/SylphVoice`), at most ONE Sylph line per listener every 4 seconds and the same group every 12 (`SylphVoiceGate`, per listener UUID, dropped on logout), per-owner cooldowns and chances in `SylphMood`, never the line said last in a group, each line with a subtitle (`subtitles.nusmp.voice.sylph_*`, text in `tools/lang/sylph_voice.json`).

**What exists, honestly: 2 of the 56 lines.** The voice was made with the ElevenLabs connector (model `eleven_v4`, one take per line, one call at a time): `sylph_link_1` ("Hi hi! It's me, Sylph! Let's go together!") and `sylph_link_2` ("I'm here! I'm here! Where are we flying to?") were generated and converted; every other call failed: the connector first answered `429 rate limited` over and over and then reported that the account's credit quota (10 000) was used up ("You have 33 credits remaining", later 4, while a line costs about one credit per character of the prompt, 30 to 55); other sessions were spending the same credits. **Nothing was faked.** Only lines whose Ogg file exists are registered (`SylphVoiceLines`, the voice catalogue, `sounds.json`, the subtitles); the 54 others are missing. A group with no spoken line falls back to the synthesised wordless cues of her own sound set (section 4: `giggle` for the sly and the giggly, `cheer` for the happy, `hum` for idle and sleepy, `gasp` for a hit or a start, `whimper` for worry and grief, `pout`), so the moods are not silent; the big casts and the arrival already have her gust, gale and surge sounds.

To finish the set (one command per step, needs an ElevenLabs API key with credits, about 2 800 credits for the 54 lines):

```
ELEVENLABS_API_KEY=<key> python3 -B tools/audio/sylph_voice_fetch.py <raw dir>     # fetches the missing lines one at a time, backs off on rate limits, skips what exists
python3 -B tools/audio/sylph_voice_convert.py <raw dir>                          # mono 32 kHz Ogg, trims, normalises, appends the sounds.json entries and subtitles, rewrites the generated count block of SylphVoiceLines
```

(or generate the lines with the connector and put `<id>.mp3` in the raw dir). Both are idempotent; `SylphVoiceDataTest` and `VoiceAssetsTest` check that manifest, list, files, `sounds.json`, subtitles and report agree.

**Voice and licence.** Voice: the ElevenLabs library voice "Candy - Girly and Sweet" (`Nggzl2QAXh3OijoXD116`, a "professional" library voice, young, cheerful, bright), picked from `creative_list_voices` for a young cheerful girl. No real person's voice was cloned by us; a library voice may itself be a clone of a voice actor who consented to it. **Commercial use could NOT be verified**: the ElevenLabs terms page could not be fetched here (no route to elevenlabs.io). As far as is generally known, generated audio may be used commercially only on a paid plan (the free plan asks for attribution and is non-commercial), and a library voice can carry conditions of its own: the owner must check the voice's page and the plan before a public release (the same caveat as the 120 older lines, `docs/audio/spoken_voices.md`).

## 3. Levels of detail and the exact quads per frame

Counted from the model files (`SylphLodTest` reads them, so these numbers cannot drift from the files). Cost of one Sylph = body + the wings model once per pass + glow-only model and halo (the two nearer levels).

| Form | Level | When (distance, best quality) | Body | Wings model x passes | Glow-only | Halo | **Per Sylph** |
|---|---|---|---|---|---|---|---|
| small | full | up to 12 blocks | 377 | 32 x 3 = 96 | 29 | 2 | **504** |
| small | half | up to 28 | 211 | 32 x 2 = 64 | 29 | 2 | **306** |
| small | low | up to 64 | 66 | 32 x 1 = 32 | 0 | 0 | **98** |
| small | culled | beyond 64 | | | | | 0 |
| powered | full | up to 32 | 385 | 52 x 3 = 156 | 25 | 2 | **568** |
| powered | half | up to 64 | 233 | 52 x 2 = 104 | 25 | 2 | **364** |
| powered | low | up to 112 | 70 | 52 x 1 = 52 | 0 | 0 | **122** |
| powered | culled | beyond 112 | | | | | 0 |

The wings model is counted once per pass it is drawn in: the membrane, and the sheen once on the half level and twice (two phases cross-faded) on the full level; none on the lowest (`SylphLod.wingPasses`). The wing model has 32 quads in the small form (4 wings x 4 cards x 2 faces) and 52 in the powered form (4 wings x 5 cards x 2 faces plus the 12 quads of the petal wings). The face plates grew the full and half bodies by 12 (small) and 4 (powered) quads. The owner's allowance for this round is up to 700 / 900 quads at full detail (small / powered): 504 / 568 are inside it, and the half (350 / 450) and low (140) levels too. The small fairy covers few pixels, so her bands are shorter than the powered form's (a smaller thing needs its detail only close up). The effect quality (`PerfSettings.vfxQuality().detail`, 0.5 to 1) scales every band. **Shared per-frame budget** `SylphLod.FRAME_QUADS = 3200` for every Sylph on screen together (the linked ones, the powered ones and the exclusive Spirit Lord body all go through `client/prop/SylphView`): the nearest are drawn at the level their distance asks for, each is stepped down a level until it fits, then culled; **the viewer's own Sylph is never dropped** (at worst the lowest level, booked past the cap). Earlier rounds allowed about 800 / 1000 quads at full detail; this round up to 700 / 900 (the wings use more cards now), about 350 / 450 at level 1 and at most 140 at level 2. Textures: the colour sheet per form (768 or 1024 wide), `_lod1` half and `_lod2` quarter size, a wing sheet and a wing glow map, a half size glow-only sheet, one 128 px halo.

## 4. The sound set (a NEW one: not the basic Tensura spirit's)

Thirteen cues (the first eight, and five wordless vocal stand-ins added with the moods: `hum`, `gasp`, `whimper`, `cheer`, `pout`, 2 variants each, made by `tools/gen_sylph_sounds.py --only hum,gasp,whimper,cheer,pout`, which appends their `sounds.json` entries and changes nothing else), **synthesised from nothing** by `tools/gen_sylph_sounds.py` (numpy plus the DSP kit of `tools/gen_mob_sounds.py`, ffmpeg libvorbis, mono 44.1 kHz, deterministic seeds), 20 files, about 340 KB, 0.5 to 2.1 s each. Registered as `nusmp:sylph.<cue>` by `sylph/SylphSounds` (a fifth sound register, in a loop from the catalogue `sylph/SylphSoundSet`), `sounds.json` entries appended at the end (nothing else touched), subtitles in `tools/lang/sylph.json`.

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
| `hum` | 2 | a little tune hummed to herself, soft sung notes | stand-in for the idle and sleepy lines |
| `gasp` | 2 | a quick breath and a small "oh" | stand-in for the hit and startle lines |
| `whimper` | 2 | a small sob, two falling wavering notes | stand-in for the worry and grief lines |
| `cheer` | 2 | a bright "yay" with a run of chimes | stand-in for the cheer and victory lines |
| `pout` | 2 | a huffy "hmph" | stand-in for the pout line |

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

Moods (rule 11): no new entity, no new packet and no per-tick world scan. Per tick: the damage and death listeners do one empty-set check when nobody has a Sylph out and one set lookup per hit otherwise; the upkeep (already every fourth tick, only for linked players) adds one position compare; a mood decision runs on an event or at most every 40 s idle, allocates one small record, and writes one int into the prop word (the prop's normal sync). Worst-case spoken packets: one tiny id string (at most 64 bytes) per listener every 4 seconds from all Sylphs together, to players within 32 blocks, and never more than the server's `voice.maxPerSecond` (default 2); the wordless stand-in sounds go through the existing per-listener sound limiter (at most 3 a second). Per-frame draw cost grew only by the wings (section 3); a pose is a different clip of the same models (no extra quads).

Worst case entities from this feature: 48 + 6 = 54 prop entities (a caster has at most one of each, and only Yuno's casters can have any). Per tick: the upkeep runs every fourth tick and only for the players that have a Sylph out (`LIVE` set); no world scan, no chunk load; one `getEntity` lookup per live Sylph per four ticks. Packets: client to server one byte `Key` at most every 10 ticks, validated (action in range, sender a real player, Yuno, switch on); to clients only the ordinary prop traffic (a position every 5 ticks per live prop, a param update per cast), the VFX burst of a cast (nearby only, through `VfxSpawn`), and sounds (at most 3 a second per listener, within 32 blocks). Per listener a chime about every 15 s per Sylph in earshot.

Client per frame: at most **3200 quads** for all Sylphs together (section 3); one Sylph at full detail is 504 or 568 quads; at most seven draw calls per Sylph (wings, two sheen passes, body, glow-only model, two halo layers); nothing allocated per frame in the painter (primitive math; the one `LazyBuffers` wrapper the other prop painters also use); she moves by bone rotation only; no particles at all.

## 7. NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | the whole `sylph` package (rules, LOD maths, ledger, casting, props, net, events, config, sound set and sounds, self-test), `PropKind.SYLPH_1`, the key and its packet, `client/prop/SylphDraw`, `SylphPropPainter`, `SylphView`, `FlatLit`, `client/sylph/SylphClient`, 10 geo models and 2 animation files, their sheets, the halo, 20 sound files and 8 `sounds.json` entries, the generators (`gen_sylph_geo.py`, `sylph_art.py`, `sylph_geo_lib.py`, `gen_sylph_sounds.py`, `sylph_scenes.py`, `sylph_compare.py`), the preview example and scenes, tests, this doc, the lang fragment |
| REPLACED | **the look** of the exclusive Spirit Lord Sylph when `magic.sylph` is on (the honeycomb-winged fairy of 1.99.0 is no longer drawn by default); its hit box grew from 0.35 x 0.45 to 0.4 x 0.6; the way Yuno's Wind pages leave the caster (from her hand, not the caster's own hands) |
| KEPT | every Wind page id, order, number, cooldown and the Spirit of Zephyr lock and Spirit Dive armour; the entity and prop ids (`ElementalSpiritLordEntity` kind SYLPH, `SpiritLordEntity` kind SYLPH, `WindSpiritLordEntity`); the older Sylph models and textures (`spirit_sylph*`, `spirit_sylph_fairy*`, `arc_spirit_sylph`), still drawn when the switch is off (`GreatSpiritModels.sylph`); every other caster's casting; all other `sounds.json` entries byte for byte |

**Round 1.99.0 additions to this table**

| | What |
|---|---|
| NEW | the wings' second version (`tools/sylph_wing_art.py`, the card chains, the sheen sheet and `client/prop/SheenUv`, `SylphLod.wingPasses` and the sheen constants), the face plates smug, angry, sad, surprised, sleepy, pout (small) and angry, laugh (powered), 11 small and 3 powered clips, `sylph/SylphMood` (+ `SylphMoods`, `SylphVoiceLines`, `SylphVoiceGate`, `SylphVoice`), the pose bits of the prop word, the config `magic.moods.poses` / `magic.moods.voices`, five wordless sound cues, 2 spoken lines, the manifest `tools/audio/sylph_lines.json` and its scripts (`sylph_lines.py`, `sylph_voice_fetch.py`, `sylph_voice_convert.py`), `tools/lang/sylph_voice.json`, `tools/sylph_wing_compare.py`, `tools/sylph_pose_sheets.py`, new preview scenes, `docs/previews/sylph/sylph_wings.png`, `sylph_poses.png`, `sylph_faces.png`, tests `SylphMoodRulesTest`, `SylphVoiceDataTest` |
| REPLACED | the look of the four wings (shape, texture, glow map: the glow sheet is now the sheen), the wing flutter, the wings' scale of the quad budget (the wings are counted per pass); the sine tracks of the clips now run the whole clip |
| KEPT | the first-version wings as `sylph_<form>_wings_v1`, every wing bone name, every older clip and face, the 120 older voice lines and their order (Sylph's role is appended after them), every older sound cue and `sounds.json` entry (new entries appended, nothing rewritten), ids, file names, the frame cap, culling, ledgers and the config file name |

Existing files touched in 1.99.0 (append-only or hooks): `audio/voice/VoiceCatalog` (role `sylph` and its ids appended), `sounds.json` (7 entries appended), `client/prop/SylphPropPainter` (the pose goes into the clip choice), the tests `VoiceAssetsTest`, `VoiceLogicTest`, `GeoAssetsTest` (the sheen sheet's size rule), `SoundsJsonTest` untouched, `SylphSoundsTest`, `SylphLodTest`, `SylphAssetsTest`, `SylphSourceScanTest`, `tools/safe_additive/scan_textures.py` untouched (its scope already names the wing glow sheets).

Existing files touched (hooks and append-only one-liners) in the first version: `book/GrimoireBook` (the cast path calls `SylphCasting.run` around the fire casting hook; the spell VFX start goes through `SylphCasting.origin`), `book/SpellRuntime` and `book/TensuraShots` (bolt / shot start through `SylphCasting.origin`), `book/WindBook` (the pictures of Gust Lane and the Wind Emperor start at her hand), `prop/PropKind` (+`SYLPH_1`, appended), `prop/PropRegistry`, `client/prop/PropPainterRegistry`, `NUSMP` (two boot lines), `entity/ElementalSpiritLordEntity` (hit box, her sounds), `entity/SpiritLordEntity` (her gust), `client/ElementalSpiritRenderer` (the new small model), `sounds.json` (8 entries appended), `tools/safe_additive/scan_textures.py` and `SafeAdditiveTextureScanTest` (the Sylph glow sheets joined the additive scan).

## 8. Tests

`SylphRulesTest`, `SylphLodTest`, `SylphAssetsTest`, `SylphSoundsTest`, `SylphSourceScanTest`, `SylphMoodRulesTest` (event to pose / face / voice, priorities, cooldowns, idle rotation, no flicker over a long random run, the pose bits, the clip choice, line picking, the listener gate), `SylphVoiceDataTest` (manifest, registered list, files, `sounds.json`, subtitles, report, size, licence note) (pure JUnit and source scans: the server decides and the client only draws, nothing scans the world on a tick, every packet validates its sender, sounds go through the limiter to nearby players, the cast path still calls the hook, the Wind pages and their lock are untouched, nothing is allocated per frame, a unique config file name, the drawn sizes match the asset check); self-test key **`magic.sylph`** (`SylphSelfTests`, in the CI server smoke test: sound events, files, models, the Wind book, the rules, and on a stand-in player the link spawns, is counted, is capped and is gone after the player is forgotten); the guards that scan it: `GeoAssetsTest`, `SafeAdditiveTextureScanTest` (scope `entity`: the glow sheets), `FxSourceScanTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `SoundsJsonTest`, `SpiritLooksTest`.

## 9. Numbers of the three pictures vs ours (what differs, honestly)

See `docs/previews/sylph/sylph_vs_reference.png` (each picture beside its render) and `sylph_scale.png` (both forms next to a standing player).

* **Picture 1** (the laughing fairy): the closed arc eyes, blush, ahoge, open laughing mouth, ruffled lime dress, small translucent wings and the green glow match. Differs: she sits on the air with one knee up in the reference, ours leans back with a bent leg but is not truly seated; her skirt is a huge wavy trailing sheet, ours is a ring of ruffles; her hair is a soft pointed bob, ours a boxy cap; limbs are boxes, not curved.
* **Picture 2** (pointing at her head): the pale green eyes, open smile, ragged blonde-green hair, choker, scalloped strapless top and long iridescent wings are all there. Differs: the hair is windblown in a flowing mass in the reference, ours is the bob; the dress is nearly white in the reference, ours a pale lime; the wings are broad and rainbow-sheened in the reference, ours a flatter pastel; her pose (one foot kicked up) is not matched.
* **Picture 3** (the powered form): the stern face, hair streaming up in S-curves, big green veined wings, petal skirt, spread arms and legs, intense green glow match in layout. Differs: her hair is a smooth glossy mass of locks, ours five stiff ribbons; the wings have more, finer cells and a stronger iridescence; the petal skirt is far fuller; her proportions are slender and long-limbed, ours chunkier; the glow is a halo square, not a lit body.
* **Wings** (remade): compared with pictures 120 and 121 the shape (long, narrow, pointed, two pairs, rolled up and back), the vein network (leading vein, long veins that fork, small cells) and the pastel sheen are there. Still different: the reference wings are two or three times wider across the sheet and have only a few big cells in picture 120 (ours are fine cells everywhere); the picture-121 wings have a stronger, patchier green with lighter patches (ours is an even green with a sheen); our membrane is a few flat cards, so from the side it is a line and its curl is a few degrees; the rear pair of 121 is a pale ruffled sheet (our petal wings are unchanged); the sheen is a cross-fade of 6 stored phases, not a continuous scroll (a scrolled UV window cannot be masked by the cut-out in one pass).
* The **head is a box** (with hair cards over its corners), which no texture hides from the side; hands are blocks with three finger bars.

## 10. Sources (rule 10)

English wiki first: `https://blackclover.fandom.com/wiki/Sylph` **could not be fetched** in this session (`getaddrinfo ENOTFOUND blackclover.fandom.com`: no network route to fandom.com). Japanese fallback: `https://blackclover.fandom.com/ja/wiki/シルフ` and `https://ja.wikipedia.org/wiki/ブラッククローバーの登場人物` **could not be fetched either** (same error). So the design comes from the three pictures the owner supplied (a small glowing green fairy girl with a curled ahoge, pointed ears, a ruffled green dress, small wings; the same with pale green eyes, a white-mint scalloped dress, a choker and long iridescent dragonfly wings; the powered form with streaming hair, a stern face and big veined green wings) and from what the repository already says of her (Yuno's wind Spirit Lord, the Wind book, the Spirit of Zephyr page and its Spirit Dive armour). No wiki text was used; the numbers (sizes, cooldowns, caps) are this mod's own.

## 11. Not verified (honest list)

* **Nothing was run in a game or on a server.** The type check was against API stubs and the JUnit run in a shim without Minecraft; tests that need the game classes did not run and are left to CI.
* **Nothing was heard.** The sounds were checked by their numbers (duration, peak, loudness) and spectrograms; how convincing the chimes, the giggle and the gusts are is the owner's to judge; every recipe is a few lines of parameters in `tools/gen_sylph_sounds.py` and can be retuned.
* **The look only approximates the pictures** (section 9). The animation signs (which way a rotation turns) were checked only in the headless preview.
* The `FlatLit` trick (an upward normal on every vertex of a no-cull render type) is proven only in the preview's rasteriser; in the real shader it should give a diffuse term of 1, but it was never seen in a game. If it misbehaves, drawing the body through `SylphDraw.pass(..., flat=false)` gives the ordinary lit look.
* The wings' look comes from a headless rasteriser; in the real shader the translucent membrane, the additive sheen and the sort order of the two translucent passes were never seen (the sheen cross-fade is two additive passes of the same cards, which should be cheap). The `SheenUv` wrapper relies on the entity render types reading V from `setUv`.
* The poses (sign and size of every angle) were checked only in the headless preview, which does not show how a pose reads in motion; the death cry lasts 70 ticks and shows only if the prop is still near the dead owner; the idle timings (45 s, 40 s) and the cooldowns are guesses.
* **Voice**: only 2 of the 56 planned lines exist (section 2.4) and nothing was heard; wording, accent, pace and the match of the voice to the character are unchecked; the voice licence is unchecked.
* The wing glow tint, the halo size and the hover heights (0.7 right, 1.95 up for the small form; 1.5 behind, 2.9 up for the powered one) are estimates from the pictures.
* The default key (right bracket) is a guess and may clash with a pack's keys; it is rebindable.
* `magic.sylph` is read by the client renderer through NeoForge's server config sync; a client that has not received the config yet draws the default (on).
* Balance feel (the powered form's duration and cooldown, the Slow Falling aura) is untested. The area pages' hits are still the caster's, only their pictures leave her hands.
* Spirit commands are limited to the link toggle, the aura and the emitter (no AI follow, no attack commands): she is a prop, deliberately cheaper than a per-caster AI mob.
