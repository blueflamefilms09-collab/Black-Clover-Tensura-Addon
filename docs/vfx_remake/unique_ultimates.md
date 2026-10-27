# 3.29.0 — a signature effect for each Evolution Ultimate

Ten shapes (`VfxShape.ULT_*`, appended at the end of the enum), one layer each (`vfx/client/layer/Ult*Layer.java`, registered in `VfxManager`), one preview scene each (`tools/vfx_preview/scenes.d/ult_*.json`, run `python3 tools/vfx_preview/preview.py ult_<name>`). The Ultimate art (`debt_burst`) sends the shape once, at the caster's feet, `power` = the burst radius, 20 ticks. Each Ultimate's `vfx` in `tools/lines/rows/*.py` (and the generated `skill_trees/*.json`) now names its own shape.

Setting: CLIENT `visual.uniqueUltimates` in `config/nusmp-ultimates-client.toml` (default true). Off = the generic shape each Ultimate played before 3.29.0 (table in `vfx/UltimateVfxFallback.java`). Server data and damage are not touched.

| Ultimate | Line | Shape | Old shape (kept, used when the setting is off) | Peak vertices |
|---|---|---|---|---|
| Iron Shadow Dragon Mode | Gajeel | ULT_IRON_DRAGON | SHADOW_POOL | 376 |
| Satan Soul: Hell's Core | Mirajane | ULT_SATAN_CORE | DEVIL_CIRCLE | 340 |
| All Crush | Gildarts | ULT_ALL_CRUSH | ASH_FORMATION | 364 |
| Water Body: Great Flood | Juvia | ULT_GREAT_FLOOD | WATER_BURST | 356 |
| Monarch of Shadows | Ashborn | ULT_MONARCH_SHADOWS | SPIRIT_AURA | 384 |
| Chairman's Last Stand | Go Gun-hee | ULT_CHAIRMAN_STAND | SPIRIT_AURA | 384 |
| Devouring Shadow | Regis | ULT_DEVOURING_SHADOW | SHADOW_POOL | 356 |
| Final Dose | Lee Seolhwa | ULT_FINAL_DOSE | POISON_BREATH | 272 |
| Disaster of Floods | Shin Yoosung | ULT_DISASTER_FLOODS | WATER_BURST | 372 |
| Elf Ring: Thousand Elf | Lefiya | ULT_THOUSAND_ELF | ELF_CIRCLE | 316 |

## What each one shows (build-up 0-6, peak 6-12, aftermath 12-20)
- **Iron Shadow Dragon Mode**: black shadow pool and iron sigil, two ranks of iron spikes, an iron-scaled helix dragon body winds round the caster; the head snaps outward, ray flash, rust-orange cracks and a riveted shock ring; iron rusts, body dissolves into shadow.
- **Satan Soul: Hell's Core**: horned pentacle and counter-turning violet sigil, two bat wings of dark flame, a black-red core contracts; white detonation, hellfire ring with a flame wall, purple claw-arc lightning and bone-white claw rakes; wings burn away, smoke and embers.
- **All Crush**: white pin of light and contracting cyan pressure ring, fracture planes and fissures, a cracked-glass disc; everything lets go at about tick 9 (shards, rubble, dust column, flash); cracks cool and fade.
- **Great Flood**: rain, a dark whirlpool, a ring wall of deep-blue water, a glass-blue water woman stands up; wall rushes in, she bursts, a crashing wave ring runs to the rim; foam, ripples, rain stops.
- **Monarch of Shadows**: dark pool, two counter-turning sigils, ten cloaked shadow soldiers and a crown of seven spikes rise; flash and a violet pillar, shock rings, forked ground lightning; soldiers dissolve into motes.
- **Chairman's Last Stand**: gold guild seal under a blue storm rim, gold motes drawn in; white-gold impact, eight golden light lances, thunderclap rings, gold-white lightning, crater cracks; dust, sparks and the seal linger.
- **Devouring Shadow**: living shadow pool with indigo veins, a lower row of fangs thrusts up, a wolf head with pale eyes rises; the upper row falls and the maw slams shut with a violet-blue flash; everything is pulled into one black point.
- **Final Dose**: 28 fine needles rain and lean in, venom drops and a jade mist ring; the needles close on a point and flash; an eight-petal venom flower opens, then droops into a cold spore haze.
- **Disaster of Floods**: storm clouds and rain, a black-green tide with a huge whirlpool, a ring of jagged crests carrying monster silhouettes (heads, maws, tentacles, a fin); the tide surges out with lightning and foam; crests slump, foam recedes.
- **Elf Ring: Thousand Elf**: a vast emerald and gold rune circle, a crown of leaf petals, ten small elf circles unfold in the air; each fires a bolt in its own element (fire, ice, lightning, wind, light) onto the ground; white-gold threads close the verse in a pillar and seal flash; circles fold away.

## Cost (CLAUDE.md rule 11)
- Server: nothing added. The Ultimate sends the same one packet it sent before (effect at the caster plus the small per-foe ones); only the shape id differs. Cooldowns 15000-18000 ticks per player.
- Client: at most 400 vertices per activation (the existing hard cap), the worst measured peak is 384; constant loop counts, no per-frame collections, deterministic from the seed so every client agrees. One activation per cast, a few at most when many players cast at once (the existing `PerfSettings` active cap applies). Camera shake is mild (at most 0.9 for 12 ticks) and only for effects that already have one.
- The setting `visual.uniqueUltimates` off sends no new work at all.

## Not verified
Seen only in the headless preview (shape, colour, motion, vertex budget); nothing was seen in a running game, so depth against blocks, real lighting, blending and texture filtering are unchecked. Known weak spots: the Iron Shadow Dragon head is a spiky camera-facing shape, the Disaster of Floods monsters read as dark blobs with eyes, the Great Flood wave is boxy and the Devouring Shadow head reads as a shadow-cat more than a wolf.
