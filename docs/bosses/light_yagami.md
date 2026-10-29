# Light Yagami (rank F gag boss, Death Note)

Sources: the spec in `docs/bosses/roster.md` (the Light Yagami section, with the owner's decisions). No wiki was fetched for this boss (rule 10: nothing needed beyond the owner's spec; the two quotes and the chip line are the owner's own).

## What it is

Not a fight: a scripted gag scene against a nearby survival player. He has about 28 HP, no knockback resistance and five token moves (so `TierRules` still holds), and every few seconds of calm he starts the scene:

1. **Chip beat (70 ticks, 3.5 s).** His over-serious monologue; the voice line "I'll take a potato chip... and eat it!" plays at the start, the eating sound at tick 44.
2. **Pen beat, raise (40 ticks, 2 s).** He raises the hand and the pen.
3. **Pen beat, write (80 ticks, 4 s).** The pen is on the page; a brush sound every 10 ticks. On the last tick: if `lightDeathNoteLethal` is true the player takes a REAL death through the normal death flow (damage type `nusmp:death_note`, message "<player> died of a heart attack"), and one of two random quotes ("Just as planned." / "I am justice!") plays as a sound and as a chat line. If the setting is false the ending is a harmless joke message.

Total 190 ticks (9.5 s). Counterplay: the scene is cancelled by killing Light, knocking him more than 1.5 blocks, one hit of at least 20 percent of his max HP, the target dying, logging out, changing dimension, turning creative/spectator, or going beyond 26 blocks. A cancelled scene gives a taunt line, a 60 s cooldown on that player and the reward: 2 to 4 baked potatoes named "Light's Potato Chip" (always), plus "Ryuk's Apple" if he died in the middle of a scene. A Totem of Undying works as usual (the damage type is NOT in `bypasses_invulnerability`; it IS in the armor/effects/enchantments/shield/cooldown/resistance bypass tags, so only the Totem or a cancel saves the player). Creative and spectator players are never chosen.

Trigger: state COMBAT, an eligible `ServerPlayer` target in the same level, 3 to 18 blocks away and in line of sight, checked once every 10 ticks; first scene 50 ticks after the fight starts.

## NEW / REPLACED / KEPT

- NEW: everything in `arc/tier/Light*.java` (scene state machine, registry, frame plan, packet, sounds, death source, events, boss), `TierRosterGag` (one row, appended last), `client/arc/LightSceneClient`, the damage type JSON, the `bypasses_resistance` tag, three sound events and `sounds.json` entries, 14 lang keys (`tools/lang/light_yagami.json`), the setting, the skin (`light_yagami*.png`, `arc_light_yagami*.png`), tests.
- REPLACED: nothing.
- KEPT: the 16 rows of `TierRosterF` and `TierRosterD` are untouched. Additive edits only: `ZogratisBoss.openingPower()` (a hook that returns the old `PowerCurve.OPENING` by default), `ArcConfig`, `ArcSystem`, `TierRoster`, `TierBosses`, `TierSelfTests`, `ArcClient`, the five vanilla damage-type tag files (one id appended each), `sounds.json`, `en_us.json`, `tier_specs.py`, `gen_tier_looks.py`, `gen_tier_skins.py` (which also got an `--only id` filter).

## Settings

- `lightDeathNoteLethal` (SERVER config, `nusmp-arc-server.toml`, default true). False = the scene still plays but ends in a harmless joke.
- Existing: `tierBosses`, `tierBossMax`. Nothing is a client config (the cinematic has no switch of its own; it follows the normal client rules).

## Look and the skin swap path

The look is an ORIGINAL design, not a likeness of any existing character: a neat young man in a plain dark navy suit, pale shirt, ink-blue side-parted hair, teal eyes (glowing), a small teal notebook with a gold ribbon in the breast pocket. It is generated with the tier skin tools:

    python3 -B tools/tier_bosses/gen_tier_looks.py light_yagami
    python3 -B tools/tier_bosses/gen_tier_skins.py --only light_yagami

To use your own skin, overwrite these files and keep the names and ids (nothing else needs to change):

- `src/main/resources/assets/nusmp/textures/entity/playermodel/light_yagami.png` (64x64, slim arms) and `light_yagami_glow.png`
- `src/main/resources/assets/nusmp/textures/entity/arc_light_yagami.png` and `arc_light_yagami_glow.png` (the fallback skin)

(`LightYagamiDataTest.theSkinIsTheOriginalDarkSuitWithTheNotebookMotif` checks the generated look; if you swap in your own skin, relax or delete that one test.) The display name is only the lang key `entity.nusmp.light_yagami` plus the `"Light Yagami"` string in `TierRosterGag`, so renaming him is one line each.

## Files and ids

- Damage type: `data/nusmp/damage_type/death_note.json`; tags `bypasses_armor`, `bypasses_cooldown`, `bypasses_effects`, `bypasses_enchantments`, `bypasses_shield`, `bypasses_resistance`.
- Sounds (events `light_yagami.chip_line`, `.death_just_as_planned`, `.death_i_am_justice`): `assets/nusmp/sounds/light_chip_line.ogg`, `light_death_just_as_planned.ogg`, `light_death_i_am_justice.ogg`. Played server side as level sounds in `SoundSource.VOICE`.
- Packet `nusmp:light_scene` (optional, server to client only): op, Light's entity id, target's entity id, role. No client to server packet exists, so there is no sender to trust; the receiver re-validates every field.
- Tests: pure JUnit `LightSceneTest`, `LightScenesTest`, `LightFrameTest`, `LightYagamiDataTest`; self-tests `tier.light.data`, `tier.light.scene`, `tier.light.cleanup` (they fall under the `tier` prefix already in the smoke list, so `smoke.yml` needs no change).

## Worst-case cost (rule 11)

- Idle: one tick comparison per Light per tick, and the scene search once every 10 ticks (one distance and line-of-sight check on at most the nearest eligible player). No world scan, no chunk load.
- Scenes: at most 4 at once on the whole server (`LightScenes.MAX_ACTIVE`); one scene per Light and per player; 256 cooldown entries (the oldest is forgotten).
- Server per scene tick: one pure state-machine step, no allocation, about 10 level sounds over the 190 ticks.
- Packets: at most 2 small packets (start, then end or cancel; about 8 bytes each) per viewer per scene, viewers are the players within 32 blocks capped at 12 plus the target, sent through the effect packet budget. Worst case 4 scenes x 12 viewers x 2 = 96 tiny packets in ten seconds.
- Client draw: at most 24 rectangles per frame (`LightFrame.MAX_QUADS`; measured maximum about 15), preallocated arrays, no per-frame allocation, a bystander draws only the bars and captions, a viewer farther than 40 blocks (or whose Light is gone or dead) draws nothing, a hard limit of 230 ticks ends any scene whose end packet was lost, a 10 tick fade-out.
- Cleanup: logout, death, dimension change, the Light dying or being removed, and server stop all clear the registry.

## Caveats (honest)

- NOT SEEN IN A GAME. The pure classes, data files and the type checking against the stub API are tested; the self-tests run only in the CI server smoke test, and the cinematic, the camera shake, the sounds and the skin have never been rendered in a real client. Only a preview of the skin sheet was looked at.
- Decision for the owner: the scene stays cancellable until the last tick of the writing (the spec said "before the pen touches the page"; the touch is at tick 110, the end at 190). It is easy to change in `LightScene` (move the last-cancel moment).
- The cinematic is drawn on the camera-overlay GUI layer, so the HUD and the chat draw over the bars.
- The kill is a single `hurt(Float.MAX_VALUE)` with no retry: a Totem saves the player once and the scene is then over (cooldown applies).
- The voice plays server side in `SoundSource.VOICE`, so the client `voice.enabled` option does not mute it (the voice volume slider does).
- The three ogg files were dropped in by the lead; their quality was not reviewed here.
- The skin glow colours and the notebook motif are small (about 10 pixels) and may need a touch-up after a look in game.
