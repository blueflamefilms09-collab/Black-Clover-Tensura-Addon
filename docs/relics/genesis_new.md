# Genesis wave 101 (release 1.111.0, key `genesis_new`)

15 new Genesis relics: the seven Millennium Items of Yu-Gi-Oh and eight artifacts from eight other worlds. Code: `relics/wave/Wave101*.java`.
Tools: `tools/relics/new_wave101/` (`build_all.py` runs everything; it is hooked into `gen_worn_all.py` with one line). Lang: `tools/lang/genesis_new.json`.
Self-test: `relic.genesis_new`. Tests: `Wave101DefsTest`, `Wave101RulesTest`, `Wave101ModelsTest`.

## The relics

| id | franchise | slot | notes |
|---|---|---|---|
| millennium_puzzle | Yu-Gi-Oh | neck | second soul takes a killing blow, wake at 40% (once per 5 min); acts Other Me, Shadow Game |
| millennium_eye | Yu-Gi-Oh | head | Mind Read, Truth Gaze (marks foes) |
| millennium_ring | Yu-Gi-Oh | hands/charm | hunting monsters glow; Dowsing Needles, Draw Power |
| millennium_rod | Yu-Gi-Oh | main hand weapon | +70% vs foes under 30% health; Mind Control, Pharaoh's Command, Judgement Strike; damage 300.5 (genesis staff) |
| millennium_key | Yu-Gi-Oh | belt | purges fear, confusion, sleep; Soul Room, Mind Garden |
| millennium_scale | Yu-Gi-Oh | charm | Weigh the Heart, Ammit's Verdict |
| millennium_necklace | Yu-Gi-Oh | chest (collar) | first heavy blow halved every 8 s; Foresight, Prophecy |
| ea_sword_of_rupture | Fate | main hand weapon | +60% vs foes with 100+ health; damage 305 (genesis longsword) |
| requiem_stand_arrow | JoJo | main hand weapon | +80% under 40% health, 5% life steal; damage 260 (genesis dagger) |
| truth_seeking_orbs_of_the_sage | Naruto | back | Truth-Seeking Barrage, Shinra Tensei, Chibaku Tensei |
| hogyoku_of_the_crystal_dissolution | Bleach | charm | Fusion, Boundary Dissolution, Transcendence |
| seven_dragon_balls_of_shenron | Dragon Ball | belt | whole once a day; Dragon Radar, Eternal Dragon's Roar, Shenron's Wish |
| straw_hat_of_joy_boy | One Piece | head | resists lightning, falls, explosions; Gear Fifth, Rubber acts |
| law_of_cycles_gown | Madoka Magica | chest | curse resistance, heals allies; Arrow of Hope, Purify Despair, Law of Cycles |
| greed_island_original_binder | Hunter x Hunter | charm | Spell Card Leave, Reflection, Magnetic Force |

All 15 are Genesis grade (counts: Genesis 15, every other grade 0). All three weapons carry Tensura engravings via `WeaponEngravings`.

## Millennium set bonus

Counted from the worn loadout (`Wave101Set`). From 3 to 7 items: resistance, regeneration, strength, speed amplifiers rise by threshold, and relic power scales 1 + 0.05 x (count - 1), up to 1.30. Foes marked by Eye (+25%), Scale penalty (+20%) or weighed (+30%) take more damage, bonus capped at 60%. Mind control reuses the existing domination thralls (`Wave30Events.dominate`). All state is server side, keyed by player or foe UUID.

## NEW / REPLACED / KEPT

- NEW: all 15 relics above and their assets, tests and lang.
- REPLACED: none.
- KEPT: every existing relic, grade, slot, model and registry entry. Shared files received append-only lines only (RelicCatalog 2 lines, VuModels one block reading a separate `overrides_genesis_new.txt`, `act_vfx.txt` appended block, class rows, display presets, Better Combat manifest, Curios tags).

## Design notes

- The Millennium Items deliberately have no attunement, so the whole set can be worn together (the cap of 3 attuned relics would otherwise make the set impossible). The other 8 are attuned.
- The Necklace is a CHEST collar, not the neck slot, because the Puzzle owns the neck.
- The 12 non-weapon relics have Genesis-cap visual upgrade models (gold and lapis, Eye of Wdjat); with `visual.upgrade=false` the generic slot look is used. The three weapons use the weapon pipeline.

## Not verified

- Nothing was seen in game; models were checked through preview renders only.
- `GradeTest`, `RelicDefTest`, `RelicPowerCoverageTest`, `RelicChecksTest`, `AttuneGradeRuleTest`, `UnifiedTooltipRulesTest` need Minecraft classes and could not run offline; the full Gradle build and the `relic.genesis_new` self-test were not run.
- Rod, Ea and Arrow have no upgrade twin (Ea has no void parts); hand sprites are auto-rendered and rough.
- `relic_themes.json` was not touched.

## 3.35.0: the Millennium Items remade to canon

Source for powers and carriers: the owner's Yu-Gi-Oh! wiki tables (English fandom wiki, Millennium Items; nothing needed the Japanese wiki) and the anime for the look. Full tables: `docs/relics/millennium_canon.md`.

- Models (REPLACED, old builders kept in `tools/relics/new_wave101/items_*.py` unused): plain gold, the Eye of Wdjat as the only motif, no lapis, motes, halos or fate wheels. Puzzle = inverted pyramid with loop, chain and Eye; Eye = round disc on a head band; Ring = hoop with a pyramid and five needles; Key = ankh; Scale = beam, post, two dishes, no feather; Necklace = gold torc with the Eye plate and a chain; Rod = spiked shaft, round Eye setting, crescent blades, finial.
- Powers (NEW acts, all old acts kept): Puzzle Duel Monster; Eye Seal in a Card, Sight of the Dead; Ring Seal a Soul; Rod Seal in a Tablet; Key Redecorate the Soul; Scale Fuse Monsters; Necklace Sight of the Past. Carriers are in each tooltip.
- Bounds (`Wave101Rules`, tested): holds last 1 s to 5 min; a fusion takes at most 5 foes (x1.8 at five); the Ring seals only non-boss, non-player souls up to 200 health; bosses and players only falter. No per-tick code: every act runs once per use inside `SafeCast` with its cooldown and daily limit.
- Rod mesh 1902 quads (limit 3000); the six worn models are 630 to 1282 quads, under the Genesis caps, smaller than the old ones.

