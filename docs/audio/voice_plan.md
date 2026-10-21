# Spoken voices (ElevenLabs voice library): plan and pilot

Status: pilot only. Nothing here is wired into the game yet. The pilot takes were generated on the owner's ElevenLabs canvas (flow "Multiverse of Anime - voice pilot"); the audio files are not in the repository because the build environment cannot download from ElevenLabs.

## Pilot voices (library voices; check each voice's licence for commercial use in a released mod before shipping)
| Role | Voice | Voice id | Pilot line |
|---|---|---|---|
| System announcer | David - VOG and Public Address Voice | iBeucLPI8hr8MOJjjYTL | "System notice. Floor one hundred and twenty-four. The rooms below are sealed until every enemy has fallen." |
| Boss | Final Boss - Evil All Powerful Villain | d3qrd1ge6zWDvUxIeXiH | "You climbed all this way... only to kneel before me. Come, mortal. Let me show you what a god's patience costs." |
| Hub NPC (merchant) | Justin - Safe & Warm | fV5vDeUO8ghvK7ntjiVC | "Welcome back, adventurer! You made it out alive, and with loot, too. Come, let's see what coin it fetches." |
| Dark Djinn | Obsidian | QjEdRoRdZC2i3ncmFpNy | "You have called me from the dark, vessel. Command me... and I shall devour your enemies." |

## How the files get into the mod
1. Download the takes you like from the canvas (mp3 or wav).
2. Name them `<category>_<id>.mp3` (ids below) and give them to the build session; they are converted to mono Ogg Vorbis (`ffmpeg -ac 1 -ar 32000 -q:a 3`), placed in `assets/nusmp/sounds/voice/<category>/<id>.ogg`, and registered in `sounds.json` / `ModSounds` (`voice.<category>.<id>`).
3. Playback hooks (a later change): System lines on the existing System messages, boss lines on spawn / phase change / death (boss bar events), NPC lines on interact, Djinn lines on summon / equip.
4. Every voice line is optional for the player: client option `voice.enabled` and `voice.volume`, and the text of the line is always still shown.

## Line ids (first set)
- `system_*`: floor_enter, floor_sealed, lockdown_start, lockdown_clear, ambush_warning, rest_safe, boss_ahead, rift_open, floor_clear, low_health_warning
- `boss_<family>_*`: intro, phase2, enrage, death (families: holy, evil, undead, fire, ice, storm, void, dragon, devil, angel)
- `npc_<merchant|quest>_*`: greet, sell, buy, farewell, quest_offer, quest_done
- `djinn_<name>_*`: summon, attack, dismiss (36 Djinn; light and dark variants share lines)

Cost reference: about 1 US cent per spoken line take at the v4 model (the pilot took 2 takes of four lines for roughly 10 cents).
