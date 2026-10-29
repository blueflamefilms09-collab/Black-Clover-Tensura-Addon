# Undine (Noelle Silva's water Spirit Lord)

The Spirit Lord of Water, Undine of Black Clover, rebuilt as a tall woman of water that looks like the owner's two reference stills and no longer sounds like the basic Tensura Spirit Lord: a hand-painted
cel-look figure in two forms (a companion beside Noelle and a full form that rises from the waves), ten poses, seven faces, a sound set of her own (water, glass bells, a watery choir hum, a wave crash) and spoken
lines in a calm, graceful, slightly ethereal woman's voice. Everything is decided on the server; the client only draws and plays.

Previews (made with the headless renderer, `python tools/render_preview/preview.py <scene>`; scenes in `tools/render_preview/scenes.d/undine.json`):
`docs/previews/undine/undine_vs_reference.png` (the two stills next to ours), `undine_scale.png` (the companion beside a player), `undine_companion.png`, `undine_full.png`, `undine_poses.png`, `undine_faces.png`.

## What she is in the game

* **Companion.** The key `]` (`key.nusmp.undine_link`, rebindable) links or unlinks her for a caster whose bound canon book is Noelle's. She floats beside the caster's right shoulder (about 1.9 blocks tall)
  and every spell of Noelle's Water grimoire leaves her open palm. Water breathing while she is near, as the Spirit Lord of Water gives.
* **Full form.** A page of cost tier 20 or more (the zone and signature pages) calls her full form: about 3.2 blocks, arms spread, standing on a ring of waves behind the caster for 5 seconds, casting from her raised hands.
  One per caster, at most one every 30 seconds, at most 6 on the whole server.
* **The Spirit Lord of Water entity** (the exclusive `ElementalSpiritLordEntity` of kind UNDINE) is drawn as the same full form while `magic.undine` is on; the old mermaid look stays in the code behind the switch.
* **Switches.** `magic.undine` (SERVER config `nusmp-undine-server.toml`, default true): false gives the old look, the old sound and the generic cast path. `magic.undineSpeech` (same file, default true): false = she hums and plays
  her sounds but does not speak. Caps `caps.maxSpirits` (48), `caps.maxFull` (6), `caps.soundsPerSecond` (12). Looks-only settings are the existing client effect quality.
* The pages, their ids, numbers and cooldowns of the Water grimoire are untouched.

## Poses, faces and moods

Poses are animation clips of `undine_body.animation.json`; the server stores the pose, the face and the tick it began in the prop's single int (`UndineRules`: bits 0-3 variant, bit 8 hidden, bits 9-11 face, bits 12-15
pose, bits 16-31 start tick), so a change is one entity-data sync. The server clears a pose when its time is up; the clients show a clip only while it is held.

| Pose | Clip(s) | Held | What it is | Chosen when |
|---|---|---|---|---|
| (none) | `idle`, `full_idle` | | serene idle, hair and gown flowing | default |
| RISE | `rise`, `rise_f` | 1.2 s | rising out of the wave | she is linked; the full form appears |
| WELCOME | `welcome` | 4 s | arms open (the second still) | idle moment |
| CLASP | `clasp` | 5 s | hands together in front | idle moment |
| WAVE | `wave` | 2.5 s | a gentle wave of one hand | the master kills something; idle moment |
| SORROW | `sorrow` | 5 s | head bowed, hands low | the master is at a third of their health or less |
| CAST | `cast`, `cast_f` | 1.2 s | arms spread, palms up | a spell below the zone tier |
| POWER | `power`, `power_f` | 2 s | arms flung wide, hair and gown flaring | the full form's big cast |
| FLINCH | `flinch`, `flinch_f` | 0.5 s | a flinch | the master is hurt |
| FAREWELL | `farewell`, `farewell_f` | 1.4 s | sinks and dissolves | she is unlinked (she stays 1.5 s, fading, then is gone) |

`_f` clips end in the full form's spread arms; the others end in the companion's relaxed arms.

Faces: calm (painted into the body sheet), smile, sad, stern, angry, surprised and serene (eyes closed). The six others are one soft card each over the calm face (`undine_face_<key>.geo.json` plus a 320 x 392
texture): a face change is a texture swap, the body model does not change; the card is one quad, drawn only on the two nearer levels and only when the camera sees her face.

The choice is the pure table `UndineMoodRules` (unit tested): LINK (rise, smile, link line), UNLINK (farewell, serene, farewell line), CAST_SMALL (cast, calm), CAST_BIG (power, angry, wave line), OWNER_HURT (flinch, surprised),
OWNER_LOW_HP (sorrow, sad), OWNER_KILL (wave, smile, win line), OWNER_DIED (her last words only), IDLE (clasp/serene, wave/smile, welcome/calm, clasp/smile in turn after 25 to 38 s of quiet). No flicker: every event has a
cooldown (hurt 3 s, low health 20 s, kill 6 s, cast 1 s, big cast 5 s), a held pose is not interrupted by a less important one (she may still say her line), two pose changes are at least half a second apart unless the new one
is more important, and a farewell is never cut short.

## Voice lines

40 short lines in nine groups: link 4, farewell 3, cast 8, wave (the big cast) 4, hurt 5, idle 6, low health 4, win 4, died 2. The manifest is `tools/audio/undine_lines.json` (made by `tools/audio/undine_lines.py`: id,
group, voice id, model, prompt with its audio tags). They go through the project's voice system: `audio.voice.VoiceCatalog` appends the registered ids under the role `undine`, `VoiceSounds` registers `voice.undine_*`,
`Voice.say` applies the voice limiter and `voice.maxPerSecond`, the packet is `VoiceNet.Say`, the client switch for voices still works. On top of that `UndineSpeech` applies her own limits: one cooldown per owner and group
(`UndineLines.Group`), **at most one spoken line per listener every 4 seconds**, and only players **within 32 blocks** are told. The lines have subtitles (`subtitles.nusmp.voice.undine_*`, text in `tools/lang/undine.json`).
A moment whose group has no recorded line is not silent: her sung hum (`undine.hum`, three synthesised phrases of watery choir and a bell) plays through the sound budget.

Voice: the ElevenLabs library voice **"Julie I"** (`g12yXXabMWeFoLMzxPrF`, professional, described as mature, warm, soft, reflective, calm), model `eleven_v4`. It is a library voice, not a clone of anyone. **Licence:** the API
does not show the library voice's terms; they are the creator's and are on the voice's page in the ElevenLabs library. They were NOT verified here: check them before release (see "Not verified").

Making the files: `ELEVENLABS_API_KEY=... python3 tools/audio/undine_voice_generate.py <raw_dir>` asks for one take of each line that has none (a rate-limited answer is waited out), then
`python3 tools/audio/undine_voice_convert.py <raw_dir>` converts (mono, 32 kHz, trimmed, loudness -16 LUFS, Ogg Vorbis q3) into `sounds/voice/undine/`, appends `voice.<id>` to `sounds.json`, the subtitle to
`tools/lang/undine.json` and rewrites the REGISTERED array in `UndineLines.java`: **a line is registered if and only if its file exists** (`UndineVoiceAssetsTest` pins manifest, catalogue, sounds.json, files, subtitles
and the converter's report to each other; the set must stay under 3 MB).

**State of the lines in this build** (`tools/audio/undine_voice_report.json`): **all 40 lines are generated and registered (3.34.0)**, 912 KB in all, voice "Julie I" (`g12yXXabMWeFoLMzxPrF`), model `eleven_v4`, one take each. The first run made 13 (link 4, farewell 3, cast 5, died 1) and ran out of credits; the other 27 (cast 6-8, wave, hurt, idle, low health, win, died 2) were recorded in 3.34.0. The sung hum (`undine.hum`) stays as a sound of its own and as the fallback for a group whose line is not registered. To redo a line: generate it from `tools/audio/undine_lines.json`, save `<id>.mp3` in one folder and run `python3 -B tools/audio/undine_voice_convert.py <folder>`.

## Sounds

`UndineSoundSet` is the catalogue (pure data), `UndineSounds` registers one event per cue (`nusmp:undine.ambient`, `link`, `unlink`, `cast`, `full`, `crash`, `hurt`, `death`, `hum`), `tools/gen_undine_sounds.py` synthesises the
files from nothing with numpy (flowing water with bubbles, glass bells in D major pentatonic, a watery choir hum, a wave crash, a synthetic reverb) and writes the `sounds.json` entries and subtitles. 20 files, about 0.5 MB.
Server side only: `UndineAudio` applies a cooldown per cue and owner, a gap between any two, a cap per second across the server (12), and sends one sound packet to the players within the cue's range (at most 48 blocks).

## Cost (worst case) and the budgets

Quads per form (one quad = one textured rectangle; one pass per model):

| | body | water | foam (additive) | expression card | wave base | ripple rings | total |
|---|---|---|---|---|---|---|---|
| Companion level 0 | 209 | 136 | 26 | 1 | | | **372** |
| Companion level 1 | 88 | 54 | 26 | 1 | | | **169** |
| Companion level 2 | 66 | | | | | | **66** |
| Full form level 0 | 209 | 136 | 26 | 1 | 42 | 4 | **418** |
| Full form level 1 | 88 | 54 | 26 | 1 | 30 | 4 | **203** |
| Full form level 2 | 66 | | | | | | **66** |

The caps asked for (companion 900 / 400 / 150, full 1100 / 500 / 150) hold with room to spare. Distances (at best effect quality): companion 16 / 36 / 64 blocks, nothing past 64; full form 32 / 64 / 112, nothing
past 112; a weaker effect quality shortens them (never below half). All Undines on a client together share **3200 quads per frame**: a form that does not fit drops one level, then is not drawn; the owner's own close one is
never dropped (it degrades to level 2). Worst case with the server caps: 6 full forms and 48 companions in view would be 6 x 418 + 48 x 372 quads asked, the budget cuts that to 3200 per frame (about 8 companions or 7 full forms at level 0; more at the lower levels). Nothing is allocated per frame, the hair, gown and arms move by bone rotation only (one animation clip per form), the figure is drawn in 3 to 5 passes (ripples, base, body, water,
expression card, foam), the droplet particles go through the client particle budget once every 3 (full) or 12 (companion) ticks.

Server worst case per second: entity position nudge every 5 ticks per live prop (vanilla tracking, only to players that track it), at most one prop-data sync per pose change per player (at most about 2 per second), spoken
line packets at most one per listener per 4 seconds (a few bytes each), sound packets at most 12 per second across the whole server, one upkeep every 4 ticks only for players that have an Undine live. No world scans, no
chunk loads.

## Server and multiplayer

State per player UUID (`UndineCasting.STATES`), dropped at logout, death, respawn, dimension change and server stop; ledgers cap one companion and one full form per caster and the configured totals; the key packet
(`UndineNet`) is validated and rate limited on the server; the client never decides a rule; no `Minecraft.getInstance()` outside `client/`. Self test `magic.undine` (`UndineSelfTests`) runs in the CI server smoke test:
rules, the prop word, the mood rules, the speech limits, sounds, clips, face files, the companion link, the full form, a hurt, a cast and a kill, the unlink farewell and the cleanup.

## New, replaced, kept

* **New:** the Undine as a casting companion and full form (`undine/` package, `client/prop/UndineDraw`, `UndinePropPainter`, `client/undine/`), prop kind `UNDINE_1`, the key, the server config, the poses, faces, moods,
  the spoken lines, the sound set, the generators (`tools/gen_undine_*.py`, `tools/undine_*.py`, `tools/audio/undine_*`), the preview scenes, the tests.
* **Replaced** (the owner asked for a remake): how the Spirit Lord of Water looks and sounds (a tall woman of water in her own painted sheets and her own sounds); the generic path of a Noelle Water cast now runs through
  `UndineCasting.run`, which falls through to the generic path whenever the Undine does not apply.
* **Kept:** the old mermaid look of the entity (behind `magic.undine`), every Water page, id, number and cooldown, the Fire casting path, every other caster and book, `VoiceCatalog`'s 120 lines (the Undine's are appended).
* **Shared files touched (append-only one-liners):** `PropKind` (+UNDINE_1), `PropRegistry`, `PropPainterRegistry`, `NUSMP` (boot, client init), `GrimoireBook` (castPage calls `UndineCasting.run`; the vfx origin goes
  through `UndineCasting.origin`), `SpellRuntime` and `TensuraShots` (origin), `ElementalSpiritRenderer` (UNDINE branch), `ElementalSpiritLordEntity` (voice cues), `VoiceCatalog` (+ the Undine's role, `baseIds()`),
  `sounds.json` (appended), the tests `VoiceAssetsTest` (base ids; her subtitles) and `VoiceLogicTest` (base ids), `SafeAdditiveTextureScanTest` and `tools/safe_additive/scan_textures.py` (her textures in scope).

## Sources

The look is from the owner's two reference images (a tall woman of water: pale skin, calm face, pale blue eyes, fin-shaped ears, a huge mass of wavy pale-blue hair in scalloped ribbons, a strapless blue water gown
with white foam stripes, arms spread palms up, rising from stormy waves). The English Black Clover wiki page for Undine (`blackclover.fandom.com/wiki/Undine`) could **not** be fetched in this environment (host not
reachable), so nothing here is taken from it beyond the owner's link and the images; the personality of the voice lines (calm, caring, wistful) is the owner's brief, not wiki text.

## Differences from the references

Built as flat painted quads, so: the hair is a mass of scalloped ribbon cards and reads lighter and more regular than the reference's single flowing silhouette; the arms are straight tubes with small hands (the
reference has long, bent, delicate arms and open palms); the gown is a cone of overlapping strips with the S-shaped foam stripes, a little more translucent and paler than the reference's saturated blue; the wave base is
three rings of wave cards and looks fin-like from the side; the fin ears are small; the eyes and brows are a simple cel style. The three-quarter view of the figure is the weakest.

## Not verified (honest list)

* Nothing was seen in the game: the figure was rendered with the headless preview (the project's own renderer stubs), never in Minecraft; the animations were only seen as stills at chosen moments, not in motion.
* The Java was type-checked against API stubs and the pure rules and data tests were run with a JUnit stand-in; Gradle was not run, the full JUnit suite and the dedicated-server smoke test (`magic.undine`) were not run here.
  `VoiceLogicTest` needs Minecraft classes and was not run (its one changed line was only read).
* Nothing was listened to: the synthesised sounds were checked as spectrograms and levels only; the spoken lines were not listened to either.
* The licence of the library voice "Julie I" was not shown by the API and is not verified; commercial or redistribution terms must be checked on its ElevenLabs page.
* The English wiki page was not reachable (see Sources).

## Update 3.0.0: controls moved to the spirit control plane

The Undine key (the right bracket) is gone. Link and unlink her with the Water grimoire's "Summon Spirit / Release Spirit" page, or `/multiverse spirit summon undine` / `release`; her full form is the page "Undine Full Form" or `/multiverse spirit ability full`; "Spirit Combat Mode" lets her cast water spheres with her mana and yours. Only the key was removed. Full description, numbers and costs: `docs/magic/spirit_control.md`.
