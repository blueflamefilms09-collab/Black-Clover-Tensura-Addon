# Beast Will remake (3.60.0)

You asked for all 27 Beast Wills to be rebuilt on the idea of *That Time I Got Reincarnated... / TBATE*-style beast wills: **Acquire** keeps you a human who borrows the beast's strengths and wears **one** piece of it; **Integrate** turns you into a full armoured beast-humanoid with a signature weapon. No summoned armies, no copied Magi / Djinn names, the Shadow Panther drains mana fast, and a mastered skill's finisher speaks its own incantation. The approved design is `docs/beastwill_remake_design.md` (data: `tools/beastwill/remake/beastwill_remake_v3.json`).

## NEW / REPLACED / KEPT

| | |
|---|---|
| **REPLACED** (you asked for the remake) | the 27 will files `data/nusmp/beast_will/<id>.json` (names, lore, pitch, rank, drop chance, EP need, tint, drain/strain, stats, abilities (2 per phase, named, with effect text), the Acquire piece, the Integrate form + weapon + hair/eyes/aura, spirit colours); the 27 rows of `BeastSkillRules` (3 passives, 2 actives + the mastery finisher per beast); the 27 core tints (`BeastCoreLook`, distinct); the in-game spirit art (81 new `bwform_*` sprites) |
| **NEW** | the finisher incantation (one action-bar line to the caster, `nusmp.beastwill.incant.<beast>`); the lore line on the core tooltip; 189 + 270 lang lines; server setting `beastwill.classicWills`; client setting `beastwill.classicSpirits` (`nusmp-beastwill-spirit-client.toml`); `BeastWillRemakeTest` |
| **KEPT** | all 27 ids (they are save keys), bindings, assimilation / mastery progress, hosts, caps, unlock levels, passives, keywords, the core items, the engine kinds and their limits (a hold never takes a boss, hard controls keep boss resistance <= 0.25, attribute caps), the old spirits (`bwspirit_*`), the old will files (`data/nusmp/beast_will_classic/`), all 54 skill ids and icons |

Switches: server `beastwill.classicWills = true` loads the old will files (needs a restart / reload); client `beastwill.classicSpirits = true` draws the old animal spirits. The skills have no classic switch: the 27 old skill rows are kept as text in `tools/beastwill/remake/BeastSkillRules.classic.java.txt`.

## Decisions taken as designed
Kraken is rank S, Shadow Panther is rank A, and a few abilities reuse the nearest engine kind (Sphinx petrify = `time_exclusion`, Hydra / Yamata vine lashes = `vine_lash`, Nue lightning = `draconic_pulse`). Boss safety of the engine wins over the design: every `time_exclusion` has boss factor 0, `world_howl` / `restraining_vines` at most 0.25; passive + phase stat are trimmed where they would pass an attribute cap (Hafgufa knockback resistance 0.95 -> 0.90).

## Worst-case cost
- Server: nothing new per tick. Same ability kinds, same limits (targets, radius, duration, cooldown floors), same entity queries; no spawned entities; no new packets; the finisher line is one action-bar message to the caster.
- Client: the spirit painter draws the same layers at the same size as 3.57.0: **at most 22 quads per player** in full detail (18 for the four multi-headed wills), 8 in light detail, none beyond 48 blocks or from your own first-person camera, at most 6 full players a frame; no allocation per frame (the six texture tables are built once). The test pins `new ` count 6.
- Memory: 81 new 192x192 sprites, about 1.6 MB on disk.

## Not included / honest limits
- The spirits are voxel-hologram sprites behind the player (a running figure in Acquire, an armoured beast-humanoid in Integrate), not real player-model armour; nothing was seen in a running game.
- Skills lose two of the old actives per beast (the wheel is now 2 actives + the mastery finisher).
- Offline the Gradle build could not run; the pure tests were run with a small runner, the Gradle build and the server smoke test run in CI.

## Tools
`tools/beastwill/remake/`: `check_design.py`, `render_preview.py`, `render3d.py` (the previews), `build_data.py` (design -> will files), `gen_skill_rules.py`, `gen_lang.py`, `gen_form_textures.py`.
