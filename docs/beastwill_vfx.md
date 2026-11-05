# Beast Will VFX: animal spirits and stronger ability effects (3.57.0 look pass)

Owner request: "I need the VFX for the Beast wills to be better", for ALL wills including the four that were in game before. Scope chosen: a per-beast animal spirit in the form, plus stronger ability effects.
Nothing here could be run in a game (no Minecraft jar, no Gradle here); see "Not verified" at the end.

## What a player sees

| Will | Phase 1 Acquire (translucent silhouette behind the player) | Phase 2 Integrate (bigger, brighter, animated, accent layer + specks) |
|---|---|---|
| Dragon | dragon head and neck, horns, fangs, spined back | spread wings that flap, flame tongues, rising embers |
| Shadow Panther | prowling cat, glowing eyes, curled tail | rising smoke tendrils behind it, smoke puffs |
| Elderwood Guardian | antlered bark stag-spirit with hollow eyes | crown of branches and roots, falling leaves |
| Lion | roaring lion head with a spiked mane | a turning sunburst mane of light, drifting gold sparkles |
| Ridge-Wolf | wolf with its head thrown back, howling | frost moon, ice crystals and curling mist, orbiting frost shards |
| Crested Boar | charging boar, bristle crest, tusk | speed lines, a rolling dust bank, thrown stones, dust along the ground |
| Wind-Skimmer Raptor | hooked-beak raptor, folded wing | spread feather-blade wings that beat, four wind ribbons orbiting |
| Flame-Serpent | coiling serpent with a hooded head and a flicking tongue | a turning wreath of flames, rising embers |
| Thunder-Fang Chimera | three heads (lion, goat, cobra) on one trunk | a crown of lightning that turns and flickers, bolts that jump |

The old glowing eyes and motes stay on every will (phase 2 shows six motes instead of ten when a spirit is there). A will the table does not know (a data-pack will) keeps the exact old look.
From the owner's own first-person camera no spirit is drawn (it is behind the body and would only get in the way).

## How the painter knows which will (no new packet)

The server already sends one int per aura lease: `style`. It was `(phase << 24) | tint` with the phase only 1 or 2, so bits 26..29 were free. `beastwill/BeastWillLook` (pure, no game class) now packs
`phase` into bits 24..25, the look **kind** (0 unknown, 1..9 the nine wills, up to 15 fit) into bits 26..29 and the tint into bits 0..23. The kind comes from the will id. `BeastWillManager.style` writes it,
`client/aura/BeastWillAura` reads it. A style written by the old code has kind 0 and so keeps the old look. Nothing else changed in the protocol.

## Ability effects (server, `BeastWillAbilities`)

Every will ability now sends a picture of its beast through the mod's `VfxSpawn` (existing shapes, nothing new on the client) plus a small puff of the will's own colour through `Fx.sprayTinted`.
The existing particles, sounds, numbers and rules are unchanged.

| Ability kind | Picture (by will) |
|---|---|
| dragon_sight | light flare at the eyes + a ring out to the sight radius |
| draconic_pulse (breath) | travel: dragon flame trail, raptor wind gale, serpent flame spiral, boar stone wave; then the will's burst at the end point |
| time_exclusion | magic circle on the ground at the telegraph; the dragon's burst when it takes hold (no Time Magic effect is used) |
| shadow_veil | the will's burst at the feet (panther shadow pool, raptor gust) |
| shadow_dash / ambush | travel: panther claw slash, wolf jagged crescent, raptor gale, boar stone wave, serpent spiral; the will's burst at the arrival or on the target |
| vine_lash | roots erupt at the target (elderwood) or a flame spiral to it (serpent), at most two targets pictured |
| verdant_mend | green rune circle + puff |
| restraining_vines | roots across the field, one picture for the whole field |
| bark_ward | the will's burst (roots, claws, wild flame) |
| mane_surge | lion: a flame lion bounds ahead + flare; wolf: wind mantle; chimera: lightning mantle |
| world_howl | howl rings + the will's mark (lion flare, wolf frost rings, chimera lightning pillar) |

"The will's burst": dragon flame burst, panther shadow pool, elderwood roots, lion light flare, wolf frost rings, boar stone claws, raptor gust rings, serpent wild flame, chimera lightning pillar.

## NEW / REPLACED / KEPT

- NEW: `beastwill/BeastWillLook.java`; 24 textures `textures/particle/bwspirit_*.png` (nine bodies `_a`, nine accents `_b`, leaf, shard, bolt, ember, dust, ribbon); `tools/beastwill/gen_spirit_textures.py` (deterministic, also writes the preview);
  per-will ability pictures in `BeastWillAbilities`; the self-test `beastwill.look.kinds`; `BeastWillLookTest`; this doc.
- REPLACED (owner asked): the shared "same for every will" look of the form. The shared haze is no longer drawn for a will that has a spirit.
- KEPT: the old eyes, motes, rings, the old painter path for unknown wills, every old texture (air_glint, air_orb, air_ring, beast_haze), the aura protocol, every number and rule of the system, all old particles of the abilities.

## Cost (worst case, rule 11)

Client, per frame (nothing at all beyond 48 blocks, or for an invisible player):
- full detail (phase 2, within 24 blocks, and only the first 6 players of a frame, `FULL_PER_FRAME`): 2 eyes + 6 motes + 1 body + 1 accent + 8 specks (4 ribbons for the raptor) + 4 ring quads = 22 quads;
- light detail (phase 1, 24 to 48 blocks, or over the frame budget): at most 8 quads;
- N players in view: at most 6 x 22 + (N - 6) x 8 quads. A will without a spirit: at most 17 quads (as before). No allocation per frame; probe sections `c.beastwill.aura` and `c.beastwill.spirit`.
- Textures: nine 256 x 256 pairs (about 0.5 MB in memory together), fifteen small ones.

Server, per ability use: at most 2 VfxSpawn packets (each to players within 96 blocks, through the per-tick effect packet budget) and at most 12 tinted particles (through the particle budget). Abilities have cooldowns of 140+ ticks and an energy cost, so one player sends at most 2 packets per 7 s. Network for the aura: unchanged (same int, same packets).
Server configs/behaviour: nothing gameplay-related changed, nothing new to set.

## Tests

`BeastWillLookTest` (new): packing round trip and old-style compatibility; every shipped will has its own kind; the manager writes the kind; every texture exists, is 256 square, has transparent corners and no colour under a transparent pixel;
the painter caps (48/24 blocks, 6 full per frame, 8 specks, no allocation, no first-person spirit, own probe); the abilities send at most two pictures and 12 particles, never a Time Magic shape, never raw particles.
Existing tests this touches and that should stay green: `SquareBugAssetsTest` (every `tex("...")` of the aura painters exists; sprite corners), `SafeAdditiveTextureScanTest`, `BeastWillSourceScanTest`, `ServerSafetyTest`, `PerfSourceScanTest`.
Smoke: new self-test `beastwill.look.kinds` (add `beastwill.look` to SMOKE_EXPECT in `.github/workflows/smoke.yml`).

## Not verified / caveats

- Not compiled and not seen in a game. The painter was type-checked against stubs of the engine classes it calls, the ability code only parse-checked; the signatures were re-read from the repo source.
- The spirit sits behind the body in model space (front is -z, y points down, as the existing eyes assume) and faces the camera; from directly behind the player it overlaps the body. Sizes, offsets and alphas are guesses to tune in game.
- The silhouettes are procedural; the wolf and raptor are the weakest. Better hand-painted art can replace the PNGs without code changes (same file names).
- Ability pictures reuse existing shapes with colour 0 (each shape's own colours) and duration 0 (its own length); they are not tinted by the will, only the small puff is.
- The aura only draws while the form is on, as before; the preview image is a contact sheet of the textures, not a render from the game (`python3 tools/beastwill/gen_spirit_textures.py --preview <png>`).
