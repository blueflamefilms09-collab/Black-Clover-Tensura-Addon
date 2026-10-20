# The bespoke mob audio engine (round 1.99.0)

The owner's spec: "Mobs cannot use generic vanilla zombie/skeleton sounds. Implement a custom `DeferredRegister<SoundEvent>` mapping to specific audio cues based on internet
fantasy tropes (Goblins: a distinct creepy, evil, high-pitched laugh for ambient and attack; a Lich: echoing spectral whispers)."

Every mob family of the dungeon now has eight cues, each with several synthesised variants. No audio file was recorded or taken from anywhere: `tools/gen_mob_sounds.py`
writes all of them from nothing (numpy plus ffmpeg), so they can be redone, retuned or extended.

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| `audio/Cue`, `audio/MobSoundSet` (pure catalogue), `audio/ModSounds` (the `DeferredRegister<SoundEvent>`) | NEW | 25 families x 8 cues = 200 sound events `nusmp:mob.<family>.<cue>`, registered in a loop from the catalogue |
| `assets/nusmp/sounds.json` (200 entries), `assets/nusmp/sounds/mob/**` (525 `.ogg`, 5.5 MB), `tools/lang/mob_audio.json` (200 subtitles) | NEW | several variants per event, `subtitle` keys, `stream` for files of 2.4 s or more, a longer `attenuation_distance` for the big families |
| `tools/gen_mob_sounds.py` | NEW | the reproducible synthesiser (python3 + numpy, ffmpeg libvorbis, mono, 32 or 44.1 kHz) |
| `audio/MobVoice`, `audio/SoundFamilied`, `audio/MobSoundRemap` | NEW | the mob-class helpers and the server remap of vanilla mob voices in the dungeon |
| `audio/BossSoundSets` + one line in `DndProfile.parse` | NEW | the optional `sound_set` key of a boss profile |
| self-test `audio.mob_sounds`, `SoundsJsonTest`, `MobSoundSetTest` | NEW | see "Checks" |
| the sounds of `FoundryMobEntity` (400 mobs: a vanilla zombie, wolf, piglin brute, slime, iron golem, spider, ravager, blaze, drowned or pillager voice per faction) | REPLACED | the family's own voice, chosen by the words of the mob's id and then by faction; plus an attack cue, an aggro cue, a step and a cast cue (the ability wind-up) |
| the sounds of `DungeonCreature` and so of the Living Armor, Direwolf, Basilisk, Dungeon Slime, Floor Warden and Labyrinth Guardian | REPLACED | armour, beast, drake, slime families (the Guardian: by its profile); the Warden's and the Guardian's ability sounds (anvil, enderman teleport, wither spawn, warden roar) are the family's attack, cast and aggro cues |
| the hurt, death and ambient sounds of the Goliath, Charybdis, Udaeus (skeleton), Black Goliath, Amphisbaena, Minotaur, Blood Beast and the Named Corpses (zombie) | REPLACED | golem, drowned, skeleton, golem, dragon, ogre, beast, ghoul families |
| the Hub's Quest Giver and Merchant | NEW voice | the `npc` family (they had none) |
| `NUSounds` (the seven Zagred voice lines) and every entry of `sounds.json` that was there | KEPT | byte for byte |
| vanilla mobs outside the Labyrinth dimensions, and every vanilla sound that is not a creature's voice (a bow, a door, a block) | KEPT | untouched |

## The families and what they sound like

The cue columns say what the synthesiser makes of each cue; every family has all eight (ambient, aggro, attack, hurt, death, step, cast, taunt). "Used by" is who gets the voice
(Foundry counts are of the 400 roster mobs).

| Family | The trope | Built from | Used by |
|---|---|---|---|
| goblin | the creepy, evil, high-pitched laugh (taunt: an indrawn breath, 6 to 9 rising "heh" bursts with a fast warble, a second detuned voice behind, a shrill held end; ambient: giggling mutters and a hiss) | glottal pulse train, vowels e/i, tract x1.38, vibrato 13-19 Hz | goblins (2 Foundry mobs) |
| orc | gruff barks and a barking guffaw | low voiced growl, rough AM 62 Hz | orcs, hobgoblins, fighters and monks (34) |
| kobold | yaps, hisses, a yapping laugh | high yips, rough | kobolds (2) |
| skeleton | bone rattles, a chattering jaw (taunt: groups of three clacks over a hollow breath) | resonant bone clicks, dry noise | skeletons (6) |
| ghoul | wet groans, rasping moans | low rough voice with a sub-harmonic | the undead faction (20) |
| lich | echoing, spectral whispers (ambient, taunt: breathy "hh" bursts a layer down, a 55 Hz drone, a long echo and hall) | noise through moving vowel formants, ring modulation, echo, Schroeder reverb | liches, necromancers (2); D&D bosses with the lich look |
| wraith | airy wails | breathy voice, vibrato, chorus, reverb | ghosts, banshees, reapers (18) |
| armor | clanks, creaks, a hollow helm-laugh | inharmonic resonator banks, a squeak, a band-passed voice | Living Armor, the Floor Warden, the Guardian by default, constructs (39) |
| golem | stone grinding, cracks, thuds | brown noise with chaotic gating, crack and thud | golems, gargoyles (14), the Goliath bosses |
| dragon | deep rumbling roars, a low chuckle | sub-harmonic voice, formants of a huge throat, low-passed | dragons (15), Amphisbaena, dragon-look bosses |
| drake | screeching hisses | rough high voice, noise | wyverns, basilisks (3), the Basilisk |
| slime | squelches, bubbles, giggling blips | gliding sine bubbles, swept band-pass noise | slimes (14), the Dungeon Slime |
| demon | infernal growls, a slow dark laugh (an octave below, doubled, distorted) | low voice, sub-harmonic, drive, chorus | demons (17) |
| imp | raspy, gibbering cackles (10 to 14 bursts at 24-34 Hz) | very high rough voice | imps, bats (10) |
| harpy | shrill screeches, a shrieking laugh | high rough voice, wing flaps | harpies, birds (11) |
| troll | heavy grumbles, guffaws | low slack voice, stone and wet steps | trolls (2) |
| ogre | bellows, a hearty laugh | very low voice, heavy steps | ogres, giants (12), the Minotaur |
| beast | growls, snarls, yelps, howls | rough low voice, bark, howl glide | beasts (38), the Direwolf, the Blood Beast |
| insect | chitters, clicks, buzzing | noise clicks, wing buzz | insects, spiders (15) |
| elemental | crackling fire, surges | crackle pops, fire bed, swept noise | elementals (30) |
| aquatic | gurgles, bubbling croaks | low-passed voice with bubbles | the drowned, frogs, krakens (18), Charybdis |
| cultist | hooded chanting, mad laughter | choir murmur, hysterical laugh | casters, priests, witches (39) |
| vampire | silky murmurs, hisses, a soft chuckle | breathy voice, whispers | vampires, assassins (4) |
| celestial | choirs, bell chimes, a ringing laugh | detuned choir, bell partials | angels, aasimar (9) |
| npc | friendly hums and short exclamations | hummed voice, soft boots | bards, healers, scribes (26), the two Hub NPCs |

Pitch and volume: `MobSoundSet.Family` gives every family a pitch centre and a spread (feet stray twice as far, a death 0.6 times) and a volume; the numbers are in the catalogue and in
`MobSoundSetTest`. The synthesiser matches loudness per cue class (idle -25 dBFS RMS, steps -26, hurt and attack -20, deaths and taunts -21) and keeps every peak under -2 dBFS before encoding with a soft knee (and re-encodes a file a little quieter if the decoded Vorbis still overshoots 0.97);
every file is 0.3 to 3.0 s, mono, with a 3 ms fade in and a 25 to 60 ms fade out.

## How a mob gets its voice

1. Mob classes that are this mod's own: `DungeonCreature` (and through it the Living Armor, Direwolf, Basilisk, Dungeon Slime, Floor Warden and Labyrinth Guardian) and `FoundryMobEntity`
   implement `SoundFamilied` (`soundFamily()`), and override `getAmbientSound`, `getHurtSound`, `getDeathSound`, `getVoicePitch`, `playStepSound`, `doHurtTarget` (the attack cue) and
   `setTarget` (the aggro cue, at most every 3 s). The Foundry mob also plays the cast cue when an ability's wind-up starts. The bosses and the Hub NPC base override the sound hooks with
   `MobVoice.ambient/hurt/death(family)`.
2. Which family: a Foundry mob by the first known word of its id (`bone_archer_of_the_barrow` is a skeleton, `banshee_tower_mourner` a wraith, `black_dragon_mire_lord` a dragon,
   `kobold_*` a kobold), then by its faction (undead: ghoul, construct: armour, cultist: cultist, ...). A boss by the `sound_set` of its profile, else its look (`"look": "kraken"` is
   aquatic), else the first creature its name or D&D fusion names, else its side (holy celestial, evil demon). Optional `sound_set` in `data/<ns>/mega/dnd_bosses/<id>.json`: any family id;
   an unknown one is refused with a plain sentence.
3. Everything else in the dungeon: `MobSoundRemap` listens to `PlayLevelSoundEvent` (server side) in the Labyrinth dimensions and in every other dimension of this mod except the Sanctuary
   Hub. When the sound is a stock hostile mob's voice (`entity.zombie.ambient|hurt|death|step`, `entity.skeleton.*`, `entity.spider.*`, `entity.slime.squish`, `entity.pillager.*`,
   `entity.wolf.growl|howl`, `entity.ravager.roar`, `entity.evoker.cast_spell`, ... see `MobSoundSet.parseVanillaVoice`) and the mob that made it is found (the entity itself, or the one
   standing at that exact spot), the event is replaced by the family's cue and the pitch drawn from the family's range. The family comes from the mob that made the sound, so a Foundry kobold that
   plays `entity.wolf.growl` as an ability sound still sounds like a kobold. A bow's twang, a door and every block sound are never touched.

## How to add a cue, a family or a variant

* A variant: raise the count in `VARIANTS` of `tools/gen_mob_sounds.py` (or add a recipe branch), run `python3 tools/gen_mob_sounds.py`. `sounds.json` and the lang fragment are rewritten
  (the entries that are not `mob.*` are kept byte for byte).
* A family: add it in three places and nowhere else: the `FAMILIES` table and a recipe set in `tools/gen_mob_sounds.py` (for a voiced family call `vocal_cues(...)` and add `taunt` and `step`),
  the `family(...)` line (and the words that name it) in `MobSoundSet`, and run the generator. `ModSounds` registers the eight new events by itself; `SoundsJsonTest` fails until the entries,
  the files and the subtitle lines agree. Then merge `tools/lang/mob_audio.json` into `en_us.json`.
* A cue: add it to `CUES` / `VARIANTS` / `LOUDNESS` in the generator and to the `Cue` enum (append only; the order is the order of the events), give every family a recipe for it (the generator
  refuses to run with a hole), and play it from the mob class with `MobVoice.play(entity, family, Cue.X)`.
* A mob class of your own: implement `SoundFamilied` and override the hooks like `DungeonCreature` does (copy its eight methods), or just return a family from `MobSoundSet.forEntityType`.

## Checks

* `SoundsJsonTest` (JUnit): every event of the catalogue has a `sounds.json` entry with at least two variants whose files exist, are mono Ogg Vorbis at 32 or 44.1 kHz and 0.3 to 3.0 s long;
  no `mob.*` entry or `.ogg` is an orphan; files of 2.4 s or more are streamed; every subtitle key has its line in `tools/lang/mob_audio.json` (or `en_us.json`); the Zagred entries are still
  there; the set is under 8 MB.
* `MobSoundSetTest`: 25 families x 8 cues, the pitch and volume ranges, the family rules (words, faction, look, entity type, boss), every shipped Foundry mob resolves to a known family and no
  voice is shared by half the roster, and the vanilla-voice reader reads the zombie and skeleton sounds and leaves a bow alone.
* server self-test `audio.mob_sounds`: all 200 events are in the sound-event registry (and are the events `ModSounds` hands out); `sounds.json` and every `.ogg` it names are on the class path;
  every Foundry mob and every boss profile resolves to a family; the remap reads `entity.zombie.ambient` and `entity.wolf.howl` and not `entity.arrow.shoot`.
* `python3 tools/gen_mob_sounds.py --check` validates the files outside the build (duration from the Ogg header, existence).

## What could not be verified

Nothing was played. The files were never listened to: they were checked by their numbers (duration, peak, loudness, spectral centroid and band shares, spectrograms) and by the parsers, not by ear,
so how convincing the laugh, the whispers or the roars are is the owner's to judge; every recipe is a few lines of parameters in the generator and can be retuned and regenerated. The remap relies on
`PlayLevelSoundEvent` (NeoForge 21.1.x) being fired for entity sounds with the entity (ambient, hurt, death) and for positional ones (steps, ability sounds) on the server; the type-check was against
API stubs and the behaviour was not run. Vorbis encoding differs a little between ffmpeg builds, so a regeneration may change the bytes of the `.ogg` without changing the sound.
