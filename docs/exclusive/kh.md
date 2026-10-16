# Kahono: Song Magic with Simple Voice Chat (1.43.0)

Nothing here has been run in game. Pure logic is unit tested (`SongMathTest`); the rest is type-checked against API stubs only.

## New versus replacement
All NEW. The generic Song book (five pages) is untouched and keeps its mode numbers; five pages are appended after it through `book/ext/SongExt`.

## How it plays
Cast a mode page once: it only SETS the song (stored on the skill tag `SongMode`, shown on the action bar, short chime, tiny mana cost).
Then sing. While your singing level is above zero, one pulse fires every `pulseInterval` ticks (40) around you (radius 10):

| Page id | Mode | Pulse |
|---|---|---|
| `song_mode_heal` | Healing Hymn | allies heal 2% max health x skill level (1..5 from mastery) x voice strength, through `BalanceLaw.heal`; never bosses |
| `song_mode_lullaby` | Lullaby | foes get heavy Slowness (amp 6) and lose their target for up to 3 s; `BalanceLaw.beginControl` + `controlTicks` (bosses 1 s, players 3 s, 12 s shared lock, diminishing returns); any damage wakes them |
| `song_mode_barrier` | Barrier Chorus | foes are thrown about 3 blocks, allies get Absorption for 6 s |
| `song_mode_requiem` | Requiem | weak damage over time to foes through the book's `hurt`; the most mana-hungry |
| `song_crescendo` | Crescendo (Kahono only) | cast to arm for 20 s; sing loudly for 3 s, stop: one pulse of the current mode, 2x strength, 1.6x radius, 3x mana |

Voice strength is 0.3 (just above the gate) to 1.0 (full voice). Mana per pulse goes through the existing mana system
(`BalanceLaw.cost`, `ManaLaw.costMult`, `ManaLaw.exhaust`); with no mana left the singer is told once and the song is silent for 5 s.
Foes: hostile monsters, mobs hunting an ally, and other players only where PvP is on. Allies: the singer, team, teamless players (the Time book rule), tamed pets.
No block is ever changed.

## Simple Voice Chat
Install Simple Voice Chat 2.5+ for NeoForge 1.21.1 on the server (clients too, to speak). Nothing else to configure.
`META-INF/services/de.maxhenkel.voicechat.api.VoicechatPlugin` names `song.voice.SongVoicePlugin` (plugin id `nusmp_song`). It listens to
`MicrophonePacketEvent`, decodes the Opus frame with a per-player decoder (closed on `PlayerDisconnectedEvent` and on server stop), takes the RMS
(`SongMath.rms`), gates it and writes only a number into `SingingState`. All `de.maxhenkel` references are confined to `song/voice`.
The mod runs without Simple Voice Chat (compileOnly dependency, the plugin class is never loaded by us).

Fallback without Simple Voice Chat: hold the Sing key (default V, "key.nusmp.song_sing"): the server sets a steady level (`keyLevel`, 0.6).
Only when `ModList.isLoaded("voicechat")` is false and `keyFallback` is true. The client repeats the key every 4 ticks while held.

## Classes (the why of each is in its Javadoc)
`song/SongMath`, `SingingMeter`, `SingingState` (voice thread to game thread), `SongConfig`, `SongClientConfig`, `SongMode` (mode table),
`PulseScheduler` (rate limit and per-tick budget), `CrescendoCharge`, `SongSkill`, `SongTargets`, `SongEffects`, `SongPages`, `SongTicker`, `SongNet`,
`song/voice/SongVoicePlugin`; client: `client/song/SongClient` (key, meter), `SongHudState`; VFX: `SongNotesLayer`, `SongRingLayer`
(new `VfxShape.SONG_NOTES`, `SONG_RING`, shared textures only, Song palette tinted per mode; Rukh light particles on each pulse).

## Config (`nusmp-song-server.toml`, section `song`)
gate 0.08, ceiling 0.35, holdMs 400, whisperCounts false, keyFallback true, keyLevel 0.6, pulseInterval 40, radius 10, pulseCapPerLevel 6,
pulseCostMult 1.0, crescendoLoud 0.6, crescendoTicks 60, crescendoWindow 400, crescendoRadiusMult 1.6. Client (`nusmp-song-client.toml`): hideHud.

## Unverified
Everything in game; how Simple Voice Chat discovers the plugin on NeoForge (the service file, as asked; if it is not found, the fix is to add the
`@ForgeVoicechatPlugin` annotation to `SongVoicePlugin`, the annotation exists in the 2.5 API); the look of the notes and the ring; the HUD position;
the knock-back distance; thread safety of `decode` (guarded with a per-decoder lock).
