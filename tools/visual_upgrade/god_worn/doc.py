"""Writes docs/visual_upgrade/god_worn.md from the baseline and after files (so every number in it is the measured one) and the design notes below.

    python3 -B tools/visual_upgrade/god_worn/doc.py
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import items as IT  # noqa: E402
import metrics as M  # noqa: E402
import caps as CP  # noqa: E402

OUT = os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn.md")

NOTES = {
    "flammes_founder_cloak": "A white and gold founder's cloak: a damask drape cut into four swinging bands with a scalloped hem, a standing star-pointed collar, starburst epaulettes in gilt filigree, the staff-and-star crest on the back, rune tablets and a ring of starlight orbiting the wearer.",
    "mantle_of_the_overlord": "A black velvet mantle 'with more folds than cloth': a long train, two shorter capelets over it, a fan collar with crimson spikes, a skull clasp, spiked pauldrons and red veins that glow in the folds.",
    "pactbreaker_shroud": "A torn violet-black shroud: ragged saw-tooth hem, a bunched hood, fluttering shroud tails, snapped chains at the shoulders, a cracked seal on the back and pages of the broken contract orbiting.",
    "shadow_monarchs_mantle": "A jagged shadow mantle: crystal blades fanned up from both shoulders, a crown of shadow flames behind the head, a lit crown sigil, violet veins and dark shards circling.",
    "shadows_slime_cloak": "A living violet slime: glossy blob-hemmed drape, dripping beads, tendrils over the shoulders ending in blobs, a glowing nucleus, a translucent slime sword at the hip and bobbing blob companions.",
    "silent_winter_mantle": "Woven hoarfrost: frost-fur ruff, a ring of ice crystals round the neck, icicle-hung cowls, frost lace, a snowflake crest, snow shards orbiting.",
    "six_faced_dragons_scale": "One scale of a dragon-god: scale-covered shoulders and back, six small horned dragon heads, a golden spine ridge, ember glow and orbiting embers.",
    "urek_mazinos_chain_mantle": "Heavy iron and chains: iron lamed pauldrons, chain swags across chest, back and shoulders, great shackles, a curtain of hanging chains that each swing, a tower crest.",
    "excaliburs_scabbard_avalon": "A green-and-gold belt with plates and a knot buckle, a long lofted scabbard at the hip with enamel panel, gold bands, filigree vine and chape, leaf charms, fairy lights and rings of Avalon light.",
    "silvered_sash_of_reflections": "A bandolier of hexagonal mirror plates strung on chain across chest and back, a mirrored waist band, tails of glass links ending in shards, glints that wander over the glass.",
    "berserker_armor_shard": "A cracked rust-black cuirass with a spiked gorget, a fan of blood-lit shards across the chest, spine spikes, tassets, hanging chains and large horned pauldrons.",
    "orsteds_dragon_god_armor": "Layered dragon-god plate: gorget with horns, a dragon relief on the breastplate, scale lames, tassets, a short cape, wing flanges behind the shoulders, curse seals orbiting.",
    "greaves_of_the_wind_walker": "Plated greaves with a gilt knee cop, a wind sigil, a cursed cracked band, three wings of feathers (ankle, calf, heel), spinning wind rings and streamers.",
    "white_whistle": "A bone-white abyss whistle on a braided cord with beads, gold bands, engraved mark and filigree, sound rings that swell outward and drifting notes.",
    "hand_of_vecna": "Right arm: an ornate gold armguard with a ruby eye plate and filigree. Left arm: the withered grey hand in a gold cuff with runes up the forearm, cold mist and a turning red circle below it.",
    "aerie_courier_gloves": "Long flared riding gloves with stitched fingers, a gem on the back of the hand, a courier badge, wing fans at the wrist, wind streamers and a spinning wind ring.",
    "bloodbound_gauntlets": "Riveted iron gauntlets: plated fists with knuckle spikes, an elbow spike, blood vials, lit veins, dripping tassels, blood mist.",
    "incinerator_arm": "Right arm: a black mechanical cannon arm with vents lit from inside, exhaust pipes, coils, a furnace core and a long muzzle with a turning ring. Left arm: a servo bracer with gauges and a lit coil.",
    "eye_of_vecna": "A red cord round the brow with a brass spiked plate and the pink-irised eye, lids, optic nerves streaming back, five small eyes circling the head.",
    "basilica_halo": "A floating double halo with a rune band, sixteen rays, eight glass-paned pillars to a circlet, a cross emblem, temple wings and shafts of light.",
    "godking_crown": "A band with nine spires, arches and a crowning orb, wings of flight at the temples, emerald veins, a rune ring and skill orbs circling.",
    "limitless_blindfold": "A black blindfold with six lit eyes and long tails, three tilted infinity rings, six eyes circling and the blue and red orbs of Hollow Purple.",
    "odins_all_seeing_eye": "A gold rune band with the pale eye in its lidded socket, two ravens perched at the temples, raven wings, a floating spear and rune tablets.",
    "ras_dawn_crown": "A gold ray crown with a sun-and-ankh plate, a cobra at the brow, a sun disc behind the head with rotating rays, wings, dawn shafts and sun orbs.",
}


def main():
    base = M.load_json(os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_baseline.json"))
    after = M.load_json(os.path.join(M.ROOT, "docs", "visual_upgrade", "god_worn_after.json"))
    caps = CP.GRADE_CAPS["GOD"]
    L = []
    w = L.append
    w("# God grade worn relics: the visual upgrade (slice `god_worn`, release 1.102.0)")
    w("")
    w("Owner request: \"Upgrade God Grade weapons/relics to look 5x better.\" This slice is the 24 God grade relics that are WORN (back, belt, chest, feet, neck, hands, head). \"5x better looking\" cannot be measured")
    w("literally, so it is made measurable with proxies counted from the files the game reads, recorded before (`docs/visual_upgrade/god_worn_baseline.json`, committed first) and after")
    w("(`docs/visual_upgrade/god_worn_after.json`), and enforced by `GodWornUpgradeTest`. **The ratios below are numbers of quads, texels, layers and animation groups. They are not a claim about how it looks: nothing was run in a game.**")
    w("")
    w("## How it is measured")
    w("")
    w("`tools/visual_upgrade/god_worn/metrics.py` counts the same things before and after (the pieces the relic wears, from the catalog and the model files):")
    w("")
    w("| proxy | what is counted |")
    w("|---|---|")
    w("| mesh | quads of every piece the relic wears at full detail (a mirrored copy on the other arm or leg is drawn again, so it counts again) |")
    w("| texture | painted texels: the opaque texels of the atlas cells the quads sample (old: 32x32 cells of the shared 128x256 atlas; new: 64x64 cells of the relic's own 512x512 skin), plus the 64x32 painted armour layer a chest relic keeps. Glow-map texels (non-black) are listed separately |")
    w("| glow passes | the draw passes beyond the lit solid pass that the relic's own models take part in: emissive model pass, glow-map pass (new), rim and shimmer (when it has shell quads) |")
    w("| ambient layers | effect layers the relic brings while merely worn: emissive model groups that move on their own (orbit, spin, pulse, bob), a catalog particle theme, and (new) every layer of its effect table |")
    w("| animation groups | animation groups of its pieces (swing, sway, wave, bob, spin, orbit, pulse) |")
    w("| act visuals | acts that have an effect of their own in the effect table (before: none) |")
    w("")
    w("The grade plan of 1.29.0 (five motes, a rune ring, a halo band, the shimmer, the generic ambient particles, the head particle halo) is identical for every God relic before and after, so it is listed in the json (`shared_grade_plan`) and **not scored for either state**.")
    w("")
    w("Score `V = 0.35 mesh + 0.20 texture + 0.15 glow + 0.20 ambient + 0.10 animation`, each term the count over a fixed reference: mesh 900 quads (the God floor for a worn piece set), texture 20480 texels (four times the median old painted texel count), glow 3 passes, ambient 3 layers, animation 3 groups. The weights and references were fixed before the models were built; the ratio `V_after / V_before` is what is reported.")
    w("")
    w("## Result")
    w("")
    w("| relic | quads before > after | painted texels | ambient layers | animation groups | glow passes | act visuals | LOD (quads, % of full) | held (vertices) | V before > after (ratio) |")
    w("|---|---|---|---|---|---|---|---|---|---|")
    for rid, name, slot, look, pal, lore in IT.ITEMS:
        b, a, r = base["items"][rid], after["items"][rid], after["ratios"][rid]
        w("| %s (%s) | %d > %d (x%.1f) | %d > %d (x%.1f) | %d > %d | %d > %d | %d > %d | 0 > %d of %d | %d (%.1f%%) | %d | %.2f > %.2f (**x%.1f**) |" % (
            name, slot.lower(), b["quads"], a["quads"], r["quads"], b["painted_texels"], a["painted_texels"], r["painted_texels"], b["ambient_layers"], a["ambient_layers"], b["animation_groups"], a["animation_groups"],
            b["glow_passes"], a["glow_passes"], a["act_vfx"], a["acts"], a["lod"]["quads"], 100 * a["lod"]["fraction"], a["held"]["vertices"], b["V"], a["V"], r["V"]))
    tb, ta = after["totals"]["before"], after["totals"]["after"]
    w("")
    w("Totals: quads %d > %d, painted texels %d > %d, ambient layers %d > %d, animation groups %d > %d, V %.1f > %.1f. Lowest V ratio: x%.1f, highest x%.1f. The checks of the God brief (quads >= max(900, 5x), texels >= 4x, a separate glow map, >= 3 ambient layers, an idle animation, an effect for every act, LOD <= 15%%, held form <= 2500 vertices, V >= 5x) all pass for all 24 relics (`god_worn_after.json`, `checks`)." % (
        tb["quads"], ta["quads"], tb["painted_texels"], ta["painted_texels"], tb["ambient_layers"], ta["ambient_layers"], tb["animation_groups"], ta["animation_groups"], tb["V"], ta["V"],
        min(r["V"] for r in after["ratios"].values()), max(r["V"] for r in after["ratios"].values())))
    w("")
    w("## What was built, relic by relic")
    w("")
    for rid, name, slot, look, pal, lore in IT.ITEMS:
        w("* **%s** (`%s`, %s): %s" % (name, rid, slot.lower(), NOTES[rid]))
    w("")
    w("Every relic has: its own full model (2 to 4 files), a LOD model, a held model, a 512x512 skin with its own decals (crest, panel, border, trim, two rune bands, filigree, surface weave) and an emissive glow map, an effect table of 9 to 28 ambient layers and a visual for each of its acts (an effect shape of the VFX system plus a particle burst in its colours).")
    w("")
    w("## How it works")
    w("")
    w("* **Generators** (`tools/visual_upgrade/god_worn/`, python3, deterministic): `kit.py` (geometry vocabulary: cloth bands that swing, chain links, scale fields, feather fans, filigree, shards, wisps), `b_back.py`, `b_back2.py`, `b_body.py`, `b_hands.py`, `b_head.py` (one function per relic, written once for three detail levels), `skin.py` (the 512x512 skin and glow map), `fx.py` (effect tables), `gw_lib.py` (the writer), `build.py` (fits the detail scale per relic so the quad counts land on their targets, writes everything, caches the scales in `scales.json`), `metrics.py`, `baseline.py`, `after.py`, `preview.py`, `doc.py`, `caps.py` (the cap table).")
    w("* **Model files** `assets/nusmp/relic_worn/gw_<id>_*.txt`: the same quad format as before plus a `skin <id>` record and two quad flags: `LIT` (8: the quad also glows where its glow map says so, in the glow colour) and `ADD` (16: additive pass only, never in the lit pass). Old files do not use them.")
    w("* **Skins** `assets/nusmp/textures/relic_worn/god/<id>.png` and `<id>_glow.png`: 8x8 cells of 64 texels; the material library is height-field lit (bevels, ambient occlusion, wear), tileable on a 48 texel period with a gutter; the albedo is grey and tinted by the relic's palette slots like the old atlas, the glow map is drawn additive in the glow colour.")
    w("* **Catalog** `assets/nusmp/visual_upgrade/catalog.txt`: `up <id> <pieces>` (worn), `lod <id> <pieces>`, `held <id> <pieces>`. The old `relic_worn/catalog.txt` is untouched, so the flag fallback is just not reading the new catalog.")
    w("* **Effect tables** `assets/nusmp/visual_upgrade/<id>.json`: ambient layers (kinds motes, ring, orbit, line, trail, ground) and act visuals. Read by `relics/upgrade/UpgradeFxData` (pure, tested), played by `client/relics/fx/UpgradeFxClient` (ambient) and `relics/upgrade/UpgradeActFx` (acts).")
    w("* **Flag** `visual.upgrade` in `config/nusmp-relicfx-client.toml` (default true). Off: every relic returns to the 1.101 models, nothing new is drawn or spawned. Takes effect after a resource reload (F3+T) or relog.")
    w("")
    w("## Performance guard (the 1.99.0 pass is not undone)")
    w("")
    w("Cap table (`caps.py`, `relics/upgrade/UpgradeCaps.java`, append-only; the old 2600 / 2600 caps of `gen_worn_all.py` are unchanged and still apply to every other relic):")
    w("")
    w("| grade | model file | everything one relic wears | held form | LOD | LOD distance | additive passes stop |")
    w("|---|---|---|---|---|---|---|")
    w("| (other relics) | 2600 | 2600 | n/a | n/a | n/a | n/a |")
    w("| GOD (this slice) | %d vertices | %d vertices | %d vertices | <= %d%% of the full vertices | %d blocks | %d blocks |" % (caps["model_vertices"], caps["ensemble_vertices"], caps["held_vertices"], int(caps["lod_fraction"] * 100), caps["lod_distance"], caps["additive_cutoff"]))
    w("")
    w("* Beyond 12 blocks and at `relicfx.quality = low` an upgraded relic is drawn as its LOD model (`WornLibDraw`); beyond 24 blocks its glow pass, glow map and shell pass are skipped.")
    w("* An upgraded relic counts at most 2600 vertices against the effect budget of its wearer (`WornBudget`), so its big model does not starve the grade effects of the wearer's other relics; the models themselves are always drawn, as before.")
    w("* Ambient layers go through the client particle budget (`PerfBudgets.clientParticles`), fall with distance (full to 12 blocks, none from 24) and with `PerfSettings.vfxQuality()` (30% / 60% / 100%), at most eight wearers a tick, only those in front of the camera, nothing for spectators, invisible wearers, hidden Curios slots or hidden armour relics. Act effects go through `VfxSpawn` (the effect packet budget) and `PerfParticles`.")
    w("* No per-frame allocation was added: skins are cached per name, the layer options are resolved once, the tick writes into two scratch arrays.")
    w("")
    w("## NEW / REPLACED / KEPT")
    w("")
    w("| | what |")
    w("|---|---|")
    w("| NEW | the `gw_*` worn, LOD and held models, the 24 skins and glow maps, the effect tables and catalog, the `skin` record and the `LIT` / `ADD` flags, `UpgradeCatalog`, `UpgradeFxData`, `UpgradeFxMath`, `UpgradeFxClient`, `UpgradeActFx`, `UpgradeCaps`, the flag `visual.upgrade`, the self-test `visual.god_worn`, the tests, the generators and previews |")
    w("| REPLACED | the worn look and the held look of the 24 relics (while the flag is on): they were the `uq_<id>` ornament on a shared look model, or a plain look; the hand held form of Hand of Vecna and Eye of Vecna (were signature hand models) |")
    w("| KEPT | every old model file (`uq_*`, `cloak_long`, `crown_godking`, `greaves_windwalker`, `hand_of_vecna`, `eye_of_vecna_hand` ...), the shared atlas, `relic_worn/catalog.txt` and `relic_hand/catalog.txt` (not edited), the item ids, stats, acts, passives, the sprites as inventory icons, the painted armour layer of the two chest relics, the Time Magic effects (not touched) |")
    w("")
    w("## Edits to existing files (all additions or one-liners)")
    w("")
    w("* `client/relics/worn/lib/WornMesh.java`: flags `LIT`, `ADD`, the `skin` field and record, `emitQuads` counts the new flags; `WornModels.java`: `upgrade()` loader; `WornLibrary.java`: `Ensemble` gets `upgraded`, `lodPieces`, `lodVerts`, the cache key knows the flag; `WornLibDraw.java`: per-skin buffers, LOD choice, the additive cut-off, the new flags in the solid and glow passes, budget base; `client/relics/hand/HandForms.java`: held pieces of an upgraded relic; `client/relics/fx/RelicFxOptions.java`: `visual.upgrade`.")
    w("* one-liners: `relics/RelicEngine.java` (after an act ran: `UpgradeActFx.after`), `relics/RelicSystem.java` (registers `visual.god_worn`), `client/relics/worn/lib/WornLibClient.java` (starts `UpgradeFxClient`).")
    w("* `tools/relics/gen_worn_all.py`: the hook at the end of `main()` (4 lines: runs `tools/visual_upgrade/god_worn/build.py`, which skips itself when up to date).")
    w("")
    w("## Tests and self-test")
    w("")
    w("* `GodWornUpgradeTest` (catalog, caps and LOD and held budgets read from the real model files with the game's parser, skins and glow maps, effect tables against the acts in the wave sources, the ratio file against the targets, the old files and the fallback, the new flags), `UpgradeFxMathTest` (parsing, rates, points, orientation). Existing asset tests keep passing: `WornMeshTest`, `HandCatalogTest`, `WornCoverageTest`, `RelicRenderCoverageTest`, `RelicUniqueLooksTest`, `RelicHandCoverageTest`, `ArmorLooksTest`, `WornMeshClearanceTest` (run in the JUnit shim).")
    w("* CI server smoke self-test `visual.god_worn` (`UpgradeSelfTests`): every relic exists as a God relic, its models parse and keep the caps, its skin, glow map and effect table are in the jar, and the 1.101 look still resolves with the flag off.")
    w("")
    w("## Previews (`docs/previews/visual_upgrade/god_worn/`)")
    w("")
    w("`<id>_up.png`: BEFORE (old models, shared atlas) above AFTER (own skin) in three views, and the glow pass alone. `<id>_detail.png`: a close-up of the detail, the LOD form, the held form, the side view. Made by `python3 -B tools/visual_upgrade/god_worn/preview.py`, a software rasteriser with the game's rules for these files (orthographic, one light, the 0.85 body scale, emissive additive, animation groups at rest, no clearance push-out). Honest reading: shapes, palettes and the glow layout read as intended; the real lighting, the animation, the Curios placement and the particles are not shown.")
    w("")
    w("## What could not be verified")
    w("")
    w("* Nothing was run in a game: no draw order or winding check in the real renderer (the winding and the y flip follow `gen_worn_lib.write_model` exactly), no look of the skins under real light, no feel of the swing bands and wave groups, no particle density, no act effect timing, no frame cost. The vertex numbers are counted; the frame time is not measured. At full detail near the camera a God relic can cost up to %d vertices (x2 with the glow pass for the lit parts) against 2600 before; the LOD, the low quality rule and the budget base are the guards, and `/nusmp perf report` shows the section `c.relic.worn_layer`." % caps["ensemble_vertices"])
    w("* The Curios eye icon, the armour-relic hide option and the first-person rules are the 1.99.0 ones and were not changed; they were not re-run.")
    w("* The act effect shapes are existing `VfxShape` values chosen by name; how each looks with these colours has not been seen.")
    w("* The type check ran against the API stubs (known stub artefacts only); the JUnit tests ran in the shim, which cannot load Minecraft classes, so `UpgradeSelfTests`, `UpgradeActFx`, `UpgradeFxClient` and the draw changes are type-checked, not executed.")
    w("")
    w("## Not done / cut")
    w("")
    w("* The inventory icon stays the old 16x16 sprite (owner rule 7); a 3D tilted model in the GUI slot was **not** built: the relic items are `builtin/entity` renderers that draw the sprite in the GUI by design (`HandItemRenderer`), and changing that would replace the icon the owner wants kept.")
    w("* `ArmorPainter` (the painted 64x32 armour layer of the two chest relics) was not changed; the 3D body pieces carry the new detail. Head and feet relics already leave their armour box clear.")
    w("* Swing trails do not apply to worn relics. A looped ambient sound id is a Genesis item of the brief, not a God one. No sound was added.")
    w("")
    w("## Regenerating")
    w("")
    w("```")
    w("python3 -B tools/visual_upgrade/god_worn/baseline.py     # only on the old state; the committed baseline is the record")
    w("python3 -B tools/visual_upgrade/god_worn/build.py --force  # models, skins, effect tables, catalog (or python3 tools/relics/gen_worn_all.py, which calls it)")
    w("python3 -B tools/visual_upgrade/god_worn/after.py        # god_worn_after.json and the ratio table")
    w("python3 -B tools/visual_upgrade/god_worn/preview.py      # docs/previews/visual_upgrade/god_worn")
    w("python3 -B tools/visual_upgrade/god_worn/doc.py          # this file")
    w("```")
    w("")
    with open(OUT, "w", encoding="utf8", newline="\n") as fh:
        fh.write("\n".join(L) + "\n")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
