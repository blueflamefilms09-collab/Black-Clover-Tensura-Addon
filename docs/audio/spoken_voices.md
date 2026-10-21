# Spoken voices (release 1.110.0)

120 spoken lines, generated with the ElevenLabs connector (model `eleven_v4`, one take per line, the manifest's prompt and voice verbatim), converted to small mono Ogg Vorbis files and played by the game at the right moment. The plan is in `voice_plan.md`; the manifest is `tools/audio/voice_lines.json`, the generator `tools/audio/voice_lines.py`, the converter `tools/audio/voice_convert.py`.

## What speaks, and when

| Role | Lines | When (all decided on the server) |
| --- | --- | --- |
| System (10) | `system_*` | floor enter, floor sealed (second wave of a lockdown), lockdown start, lockdown cleared, ambush warning, safe rest, boss ahead (a boss floor says this instead of floor enter), rift open, floor clear (only when the move is not also a new depth), low health (Mega-Dungeon only, a quarter of the hearts or less, at most once a minute per player) |
| Boss (30) | `boss_<family>_<intro/phase2/death>` | the boss bar's intro, phase change and death. The family (holy, evil, undead, fire, ice, storm, void, dragon, devil, angel) is chosen from the bar's style id through `BarStyleResolver`, so the voice follows the same archetype as the bar |
| NPC (8) | `npc_*` | talking to the Hub quest giver or merchant: greet, sell, buy, farewell, quest offer, quest done |
| Djinn (72) | `djinn_<name>_summon` / `_attack` | 36 Djinn, one line when summoned, one on the first attack of a cast |

The text of every message stays exactly as it was; the voice only adds sound.

## How it is built (server friendly)

- `Voice.say(player, id)` is the one facade. The server picks the line (`VoicePicker`, pure), applies the limits (`VoiceLimiter`, pure: a cooldown per line, a gap between lines, the `voice.maxPerSecond` server option, all keyed by player UUID) and sends the tiny `VoiceNet.Say` payload to nearby players only (`sayNear`). Fake players are skipped.
- The payload is server to client only; the id is a bounded string and is checked against `VoiceCatalog` again on the client. The channel is optional.
- The client plays the event in the `VOICE` sound category (`client/voice/VoiceClient`). Client config `nusmp-voice-client.toml`: `voice.enabled` (default true), `voice.volume` 0..1 (default 0.9).
- Server config `nusmp-voice-server.toml`: `voice.maxPerSecond` and the range and cooldown options.
- `audio/voice/VoiceNet.java` is on the allow-list `docs/server_safe_allow.txt` (it reaches the client handler by name inside a lambda).
- Self-test `audio.voices`: every id present in the catalogue and as a registered event, payload codec round trip, cooldown logic.
- JUnit: `VoiceAssetsTest` (manifest = catalogue, every id has an ogg, a `sounds.json` entry with subtitles off, size and 0.5 to 12 s duration, mono 32 kHz, no orphans) and `VoiceLogicTest` (catalogue, picker, limiter).

## Files

120 files under `assets/nusmp/sounds/voice/<role>/<id>.ogg`, 4,143,216 bytes in total (about 4.0 MB), 1.75 s to 7.0 s each. Chain: mono, 32 kHz, silence trimmed at both ends, `loudnorm` about -16 LUFS with true peak at or below -1.5 dB, libvorbis q3. `tools/audio/voice_report.json` lists seconds and bytes per line. The raw mp3 takes are not in the repository.

## Voices (ElevenLabs ids)

The voice id of each line is in `tools/audio/voice_lines.json`. Voices used include "Final Boss - Evil All Powerful Villain" (`d3qrd1ge6zWDvUxIeXiH`) and "Obsidian" (`QjEdRoRdZC2i3ncmFpNy`), plus the other voices of the manifest (among them David and Justin).

**Licence caveat for the owner:** before a public release, check the commercial-use terms of the voices David, Final Boss, Justin and Obsidian in the ElevenLabs voice library (library voices can carry their own conditions on top of the plan's licence). Nothing here has been cleared.

## New / replaced / kept

- NEW: the whole feature (120 sound events, `audio/voice/*`, `client/voice/*`, two configs, the payload, the self-test, the tests, the tools, this doc, the lang fragment `tools/lang/spoken_voices.json`).
- REPLACED: nothing. No existing sound, message or music was changed or removed.
- KEPT: every older `sounds.json` entry, all message texts, the themed music and the mob voices. Hooks into existing code are append-only one-liners (a `Voice.say` call next to the existing message) in the lockdown, ambush, rest, floor, rift, Djinn, Hub NPC, shop, quest and boss-bar code.

## Generation notes (honest)

- Four lines (`boss_dragon_phase2`, `boss_devil_phase2`, `boss_angel_intro`, `djinn_buer_summon`) failed once on the platform side ("unusual activity / Free Tier access has been disabled") and were generated a second time; the retries completed. The failed attempts may still have been billed.
- Credits: 6,573 for the first pass over all 120 lines (the six lines made before the rate-limit pauses cost 268 of these), plus 208 for the four retries, so 6,573 if the failed attempts were not charged and 6,781 at most.

## Not verified

Nothing has been listened to: the takes were not heard, so wording, accent, pace and the match of voice to character are unchecked, and the silence trim threshold (-45 dB) may have clipped a breathy start or end somewhere. The Java code was not compiled and no JUnit test was run in this session (Gradle could not fetch the NeoForged plugin: the egress proxy refuses maven.neoforged.net), so a type error, for example in the `LivingDamageEvent.Post` listener or the sound-instance constructor, is possible; `ServerSafetyTest` and `ConfigFileNamesTest` were also not run. The game has not been started and no dedicated server was booted; the hooks were placed by reading the code, and behaviour in play (spacing of lines, cooldowns, volume against music) is untested. The lang fragment is only a set of display names; nothing reads them yet.
