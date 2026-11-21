# Dragon Balls by voice

Status: written, **not tried with a real microphone and not seen in a game**. Pure rules and the wiring are unit tested and covered by the server self-tests `dbvoice.rules` and `dbvoice.bridge`
(neither needs Simple Voice Chat or Vosk). Nothing here replaces typed chat: typing the words works exactly as before, and `voice.enabled = false` is the old behaviour exactly.

## What it is (all NEW, nothing replaced, nothing removed)

A ritual phrase or a wish said into the microphone is turned into text and handed to the **same** code that reads chat lines (`DbChat.handle`), so it behaves exactly like the same text typed:
the words may be spread over several sentences inside `phraseWindowSeconds`, the summoner's text is read as a wish, and "cancel" / "never mind" / "farewell" / "goodbye" send the dragon away.
The text is **not** shown in chat. The speaker gets one action-bar line, `Heard: <text>`, so they know it worked. No new network packet exists.

| Part | Where |
|---|---|
| The one door for spoken text (`SpeechBridge.heard(uuid, text)`), server thread | `dragonballs/voice/SpeechBridge` |
| Relay command for **any** other voice-to-text mod, plugin or command block | `/multiverse dragonballs say <player> <words...>` (permission 2), `DbCommands` |
| Simple Voice Chat plugin (the only class that names a voice-mod type) | `dragonballs/voice/DbVoicePlugin`, listed in `META-INF/services/de.maxhenkel.voicechat.api.VoicechatPlugin` next to the Song plugin |
| Fuzzy word matching, listening gate rules, utterance limits, resampler (pure) | `SpeechMatch`, `VoiceGate`, `Utterance`, `Resampler` |
| One bounded worker thread + queue | `BoundedEngine` (interface `SpeechEngine`) |
| Offline recogniser (Vosk) loaded by reflection | `VoskEngine` |
| The words the recogniser and the matcher know (a swappable provider) | `VocabularySource`, `DefaultVocabulary`, `DbVoice.setVocabularySource` |
| Gate job, life cycle, voice-thread entry points | `DbVoice`, `DbVoiceProbe` (in `dragonballs/`, reads the ball registry) |
| Settings | `DbVoiceConfig` (SERVER, `nusmp-dbvoice-server.toml`) |
| Self-tests | `DbVoiceSelfTests` (prefix `dbvoice.`) |
| Lang | `tools/lang/dbvoice.json` merged into `en_us.json` |

Small change to existing code: `DbChat.acceptLine` (new public method that calls the unchanged `handle`), `DbCommands` (one appended `say` branch), `DbBoot` (config + four listeners + self-tests appended).
`DbChat.onChat`, `DbSession`, `DbManager`, `DbWishes` and `WishRules` are untouched.

## How a player does it

1. Play with Simple Voice Chat installed (client and server) and your microphone working.
2. Stand at a **complete set** of balls (carry them, or have them on the ground within `gatherRadius`), as for typing the words. The server then listens to you (and only you, plus at most three
   other such players).
3. Say the words (see `docs/dragon_balls.md`, "The ritual words"). Whispering works too. You see `Heard: ...` above the hotbar. If all the balls are there and the set is not stone, the dragon comes.
4. As the summoner you are listened to for the whole ritual: say the wish ("revive", "power", "coins", ...) or "cancel". You can still type or click as before.

Limits to know: the voice recogniser only knows the ritual and wish words (grammar mode), so a wish that needs a player name ("revive Steve") cannot be said, only typed; say "revive" to revive yourself.
The Namekian chant and "Shenron" are **not in the small English Vosk model**; Vosk drops words its model does not know, so with the default setting those words come out as nothing. The English calls
("come forth dragon of the gods ...", "arise ...") work best. Setting `voice.useGrammar = false` lets the model hear freely and the fuzzy matcher join what it heard ("tack are a doppo") back into the chant; whether that is good
enough is **unknown until tried with a real voice**.

## How an admin turns on recognition

Without these steps nothing is recognised (the feature is inert; one info line in the log says so; typed chat and the relay command keep working).

1. Install Simple Voice Chat on the server (mod id `voicechat`; the engine is not even started without it).
2. Download the Vosk Java library jar (`vosk-0.3.x.jar`) and its `jna` jar and put both into `config/nusmp/vosk-lib/` (setting `voice.libraryPath`), **or** put them on the server's class path some other way.
   A jar dropped into `mods/` without a mod descriptor is likely to be refused by NeoForge, which is why the mod looks into its own folder. Vosk's jar carries the native library for Windows, Linux and macOS.
3. Download a model from https://alphacephei.com/vosk/models (for English: `vosk-model-small-en-us`, about 40 MB) and unpack it so that the model files sit directly in `config/nusmp/vosk-model/` (setting `voice.modelPath`).
4. Restart the server. The log says `[dbvoice] speech recognition is on (Vosk, model ...)`.

Vosk is Apache 2.0; **no Vosk jar and no model is part of this mod's jar**.

## Any other voice chat mod (Plasmo Voice and others)

Only **Simple Voice Chat is wired directly.** Plasmo Voice and other voice mods have no plugin here. Anything that can turn speech into text can use the relay instead:
`/multiverse dragonballs say <player> <words...>` (operator, or a command block / plugin running as one). It runs the very same code as the microphone path (online and alive checks, fuzzy matching, the chat rules,
the `Heard:` line). The gate does not apply to the relay (the operator decides); the normal ritual checks do. It also obeys `voice.enabled`.

## Settings (`nusmp-dbvoice-server.toml`, section `voice`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | true | master switch. false = typed chat only, nothing listens, nothing loads, the relay refuses |
| `whisperCounts` | true | a whisper in Simple Voice Chat counts as speech |
| `maxListeners` | 4 | most players listened to at the same moment (0..16); the rest use chat |
| `modelPath` | `config/nusmp/vosk-model` | the unpacked Vosk model folder |
| `libraryPath` | `config/nusmp/vosk-lib` | folder for the vosk and jna jars (added to the settings the owner listed) |
| `matchTolerance` | true | fuzzy matching of what was recognised (false = must be exactly the configured words) |
| `useGrammar` | true | give the recogniser only the ritual / wish words (added to the settings the owner listed) |

The words themselves stay in `nusmp-dragonballs-server.toml` (`earthPhrases`, `superPhrases`). The grammar and the matcher are rebuilt from them (and from the wish tables) about every 10 seconds, so
a config change is picked up without a restart; the recogniser's model is not reloaded.

## The gate: who is listened to

A player is listened to **only** while (a) they carry or stand within `gatherRadius` (+2 blocks of slack) of **all** balls of a set that is not stone and has no ritual running, or (b) they are the summoner
of a running ritual. At most `maxListeners` players at once: summoners first, then players who were already listened to, then newcomers. Everyone else's microphone frames are dropped **before they are decoded**.
The gate runs on the server once a second per player (spread over the 20 ticks) and reads only the player's inventory and the 16-row ball registry: no world scan, no chunk. It is only a hint of who to listen to;
the real check of a summoning is unchanged (`DbManager.trySummon`).

## Fuzzy matching (`SpeechMatch`)

Every recognised word is pulled onto the closest word of the vocabulary before the text reaches the chat rules: case and punctuation ignored; allowed edit distance 0 for words up to 3 letters, 1 for 4 to 7, 2 for 8 or more
(measured on the vocabulary word); words of 5+ letters with the same sound key match too (so `takkaradoppo` and `takaradopo` are one word); two or three heard words are joined (`takara doppo` -> `takkaradoppo`),
and a fused word is split (`ideyoshenron` -> `ideyo shenron`). A refused word ("destroy", "immortal", ...) is also in the vocabulary, so a slightly misheard forbidden wish is still refused instead of
quietly turning into a harmless one. The matcher can match too much on short or similar words (a wish keyword may be heard where another word was said); a wish is never used up by a refusal.

## Privacy

Audio is only decoded for a listened-to player (see the gate), only kept in memory until the utterance has been recognised (at most 8 seconds, about 256 KB per listened-to player), then thrown away. It is
**never written to disk and never sent anywhere**: Vosk runs offline inside the server process. Only the recognised text is used (and the first 80 characters of it are shown back to the speaker).
The server log does not contain what was heard; the existing ritual log lines (`[dragonballs]`) are as before.

## Cost (rule 11), worst case

* Listeners: at most `maxListeners` (default 4, hard max 16) players. Everyone else: one set lookup per microphone frame (50 per second per talking player), no decode.
* Voice thread, per listened-to player: 50 Opus decodes per second, a 960 -> 320 sample average, one RMS pass, one 640-byte array copy per frame. An utterance buffer of at most 8 s (256 KB) per listened-to player,
  freed when the utterance ends or the player stops being listened to.
* Recogniser: **one** worker thread for the whole server (daemon, below normal priority), one job at a time. Queue: at most 4 waiting utterances per player and 8 in all; a job older than 15 s is dropped;
  drops are counted (`BoundedEngine.dropped()`). CPU: while somebody speaks, up to one core for the worker thread (the small model usually needs a fraction of real time, so a few seconds of speech cost a fraction of a second of one core);
  idle when nobody speaks. Memory: the model (about 50 MB for the small English one) only when the admin installed it.
* Server thread: per tick one pass over the player list comparing a hash; per second per player one probe (about 41 inventory stacks + 16 registry rows, no allocation); one small set copy only when the listened-to set changes;
  every 10 s the vocabulary is rebuilt (about 200 words). Per accepted text: the fuzzy matcher (at most 64 words x 400 vocabulary words, early exits) and the ordinary chat handling.
* Network: **0 packets added** (the `Heard:` line is the vanilla action bar message, sent to the speaker only, at most once per recognised utterance, which is at most one per player per 0.6 s of speech).
* Spawns: none. Disk: none. Chunk loads: none.

## Tests

* `SpeechMatchTest` (tolerances, sound key, mangled chant, split / fused words, wish words, forbidden words, bounds, grammar JSON), `VoiceGateTest` (eligibility, whisper, cap, ordering, stagger), `UtteranceTest` (silence end,
  8 s cap, click filter, queue limit, resampler), `BoundedEngineTest` (stand-in recogniser, drops, failing recogniser, missing recogniser, Vosk JSON), `VoiceSourceScanTest` (only `DbVoicePlugin` names a voice-mod
  type, the plugin never touches the game and drops frames before decoding, no Vosk import or dependency, bounded queue, one worker thread, config / command / self-tests registered, lang keys).
* Server self-test prefix `dbvoice.` (keys `dbvoice.rules`, `dbvoice.bridge`): the smoke list must contain `dbvoice.`.

## NOT included / not done (honest)

* **Nothing was tried with a real microphone, Simple Voice Chat, Vosk or in a game.** The Simple Voice Chat plugin is compiled against the API only; the reflective Vosk loader (class names, method signatures of
  `org.vosk.Model` / `Recognizer`, jar in its own class loader with jna) is written from the library's documented API and **never run against the real library**. Expect to fix details on first contact.
* Plasmo Voice and other voice mods are **not** wired directly; they use the relay command.
* Vosk's jar and a model are **not bundled**; without them nothing is recognised.
* The small English model does not know the Namekian chant or "Shenron" (see above); player names cannot be spoken.
* English only (the model the admin installs decides the language; the ritual words are English / romanised Japanese).
* The recogniser's model is not reloaded at runtime; a changed `modelPath` / `libraryPath` needs a restart.
* The wish vocabulary is read from today's wish tables through `VocabularySource`; when the wish tables grow, point `DbVoice.setVocabularySource` at the new provider (`WishRules.spokenVocabulary()`).
