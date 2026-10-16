"""Builds the whole worn relic model library: the atlas and shimmer textures, every model file and the catalog.

    python3 tools/relics/gen_worn_all.py [--check]

Writes src/main/resources/assets/nusmp/relic_worn/*.txt, catalog.txt and textures/relic_worn/{atlas,shimmer}.png (idempotent: the same files every run). With --check it
only reports the vertex count of every model and of every relic's ensemble and fails if one is over its cap. The catalog is data: to give a new relic its own look, add a
model function to a gen_worn_*.py file and one `cat.relic(...)` line here (docs/relic_visuals.md, "How to add a signature model").
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_worn_lib as L  # noqa: E402
import gen_worn_head  # noqa: E402
import gen_worn_back  # noqa: E402
import gen_worn_neck  # noqa: E402
import gen_worn_body  # noqa: E402
import gen_worn_sig  # noqa: E402
import gen_worn_armor  # noqa: E402
import gen_worn_armor_relics  # noqa: E402
import gen_worn_head_r4  # noqa: E402
import gen_worn_modes  # noqa: E402

MODEL_CAP = 2600                 # vertices of one model (all its passes' base geometry)
ENSEMBLE_CAP = 2600              # vertices of everything one relic wears
MODE_CAP = 3600                  # vertices of everything a transformation overlay wears (one mode is on at a time)

BUILDERS = {}
for mod in (gen_worn_head, gen_worn_back, gen_worn_neck, gen_worn_body, gen_worn_sig, gen_worn_armor, gen_worn_armor_relics, gen_worn_head_r4, gen_worn_modes):          # later modules replace earlier ones of the same name
    BUILDERS.update(mod.MODELS)


def catalog():
    cat = L.Catalog()
    # ---- the look of every slot: what a relic wears when it has no signature entry
    cat.look("HEAD", "CROWN", "crown_spired")
    cat.look("HEAD", "CIRCLET", "circlet_gem")
    cat.look("HEAD", "HAT", "hat_wizard")
    cat.look("HEAD", "HOOD", "hood_pointed")
    cat.look("HEAD", "PLATE", "helm_horned")
    cat.look("BACK", "CLOAK", "cloak_long")
    cat.look("BACK", "CAPE", "cape_short")
    cat.look("BACK", "MANTLE", "mantle_shoulder")
    cat.look("NECK", "AMULET", "amulet_medallion")
    cat.look("NECK", "PENDANT", "pendant_drop")
    cat.look("NECK", "LOCKET", "locket_oval")
    cat.look("BELT", "BELT", "belt_buckled")
    cat.look("HANDS", "GAUNTLETS", "bracer_plated@rarm", "bracer_plated@larm~")
    cat.look("HANDS", "GLOVES", "glove_cuffed@rarm", "glove_cuffed@larm~")
    cat.look("HANDS", "CHARM", "bracelet_charms@rarm", "ring_gem@larm~")
    cat.look("CHARM", "CHARM", "charm_orbit")
    cat.look("CHARM", "AMULET", "charm_beads")
    cat.look("CHARM", "POTION", "charm_flask")
    cat.look("CHARM", "TOME", "charm_tome")
    cat.look("CHEST", "PLATE", "chest_collar", "pauldron_plate@rarm", "pauldron_plate@larm~")
    cat.look("CHEST", "COAT", "coat_tails")
    cat.look("CHEST", "SUIT", "coat_tails")
    cat.look("CHEST", "LEATHER", "chest_straps", "pauldron_leather@rarm")
    cat.look("CHEST", "ROBE", "sleeves_flared@rarm", "sleeves_flared@larm~")
    cat.look("FEET", "BOOTS", "boots_cuffed@rleg", "boots_cuffed@lleg")
    cat.look("FEET", "GREAVES", "greaves_plated@rleg", "greaves_plated@lleg")
    cat.look("FEET", "SANDALS", "sandals_winged@rleg", "sandals_winged@lleg")
    # 1.46.0: charm looks the part 4 waves use that had no entry (they fell back to plain boxes); also three older charms with the same gap
    cat.look("CHARM", "ORB", "charm_orbit")
    cat.look("CHARM", "PENDANT", "brooch_badge")
    cat.look("CHARM", "CIRCLET", "charm_orbit")
    cat.look("CHARM", "BELT", "charm_pouch")

    # ---- signature relics, best grade first
    # GOD
    cat.relic("godking_crown", "crown_godking")
    cat.relic("greaves_of_the_wind_walker", "greaves_windwalker@rleg", "greaves_windwalker@lleg")
    # LEGEND
    for rid in ("gold_circlet", "silver_circlet", "steel_circlet"):
        cat.relic(rid, "circlet_dragon")
    cat.relic("cloak_of_levitation", "cloak_levitation")
    cat.relic("cape_of_nine_lives", "cape_nine_lives")
    cat.relic("amulet_of_the_coven", "amulet_coven")
    cat.relic("amulet_of_the_forest", "amulet_forest")
    cat.relic("hemlocks_amulet", "pendant_fang")
    cat.relic("the_amulet_of_lost_souls", "pendant_skull")
    cat.relic("amulet_of_dragon_rage", "amulet_dragon_claw")
    cat.relic("crest_of_foresight", "amulet_crest")
    cat.relic("bags_of_transferrence", "charm_pouch_pair")
    cat.relic("coat_of_the_duelist", "coat_tails")
    cat.relic("suit_of_the_golden_shadow", "suit_tails")
    cat.relic("armor_of_the_lionheart", "chest_collar", "pauldron_lion@rarm", "pauldron_lion@larm~")
    cat.relic("delphynes_cursed_armor", "chest_collar", "pauldron_horned@rarm", "pauldron_horned@larm~")
    # UNIQUE
    cat.relic("unicorn_crown", "crown_unicorn")
    cat.relic("dark_unicorn_crown", "crown_unicorn_dark")
    cat.relic("circlet_of_the_phoenix", "circlet_phoenix")
    cat.relic("crown_of_the_conjurer", "crown_conjurer")
    cat.relic("nightstalkers_mantle", "mantle_shoulder", "deco_wisps")
    cat.relic("cloak_mimic_tame", "cloak_mimic")
    cat.relic("darkened_cloak_of_elvenkind", "cloak_elven", "deco_leaves")
    cat.relic("mantle_of_selflessness", "mantle_shoulder", "deco_halo_back")
    cat.relic("alchemists_belt", "belt_alchemist")
    cat.relic("ruby_boots_of_planestriding", "boots_crystal@rleg", "boots_crystal@lleg")
    cat.relic("boots_of_the_high_ground", "greaves_plated@rleg", "greaves_plated@lleg")
    cat.relic("boots_of_burglery", "boots_shadow@rleg", "boots_shadow@lleg")
    cat.relic("hand_of_providence", "gauntlet_fist@rarm", "gauntlet_fist@larm~")
    cat.relic("elemental_hand_wraps", "wraps_hand@rarm", "wraps_hand@larm~")
    cat.relic("bracers_of_phasing", "bracer_phasing@rarm", "bracer_phasing@larm~")
    cat.relic("starstudded_leathers", "chest_stars", "pauldron_leather@rarm")
    cat.relic("armor_of_the_beheaded", "chest_collar", "pauldron_skull@rarm", "pauldron_plate@larm~")
    cat.relic("false_dragon_armor", "chest_collar", "pauldron_dragon@rarm", "pauldron_dragon@larm~")
    cat.relic("pendant_of_echoed_souls", "pendant_echo")
    cat.relic("amulet_of_the_sea_of_stars", "amulet_sea_stars")
    cat.relic("amulet_of_safeguarding", "amulet_ward")
    cat.relic("lifes_emergency", "charm_heart")
    cat.relic("urn_of_black_pudding", "charm_urn")
    # RARE / SPECIAL
    cat.relic("tiny_duck_crown", "crown_duck")
    cat.relic("diadem_of_fabulous_truthiness", "diadem_leaf")
    cat.relic("prides_crown", "crown_pride")
    cat.relic("dreamwalker_cowl", "hood_pointed")
    cat.relic("captains_tricorn", "hat_tricorn")
    cat.relic("arcanists_circlet", "circlet_arcanist")
    cat.relic("cape_of_bees", "cape_short", "deco_bees")
    cat.relic("handy_towel_cape", "cape_striped")
    cat.relic("cloak_of_the_first_snow", "cloak_furred", "deco_flakes")
    cat.relic("autumn_cloak", "cloak_long", "deco_leaves")
    cat.relic("cloak_of_the_depths", "cloak_scaled")
    cat.relic("cloak_of_the_moth_rider", "wings_moth")
    cat.relic("cloak_of_dichotomy", "cloak_split")
    cat.relic("plastic_sheriff_badge", "brooch_badge")
    cat.relic("sandals_of_sneaking", "sandals_winged@rleg", "sandals_winged@lleg")
    cat.relic("dragon_fire_boots", "boots_cuffed@rleg", "boots_cuffed@lleg", "deco_flames")
    cat.relic("lovelorn_locket", "locket_oval")
    cat.relic("fast_travel_beads", "charm_beads")
    cat.relic("madama_cupcake", "charm_orbit")
    cat.relic("crystal_dragon", "charm_orbit")
    cat.relic("shared_pouches_of_transfer", "charm_pouch_pair")
    cat.relic("bag_of_realistic_capacity", "charm_pouch")
    cat.relic("spellbook_of_concentration", "charm_tome")
    cat.relic("martels_magical_bag_of_candy", "charm_flask")
    cat.relic("pact_of_the_deck", "charm_orbit")
    cat.relic("holy_chains", "charm_beads")
    # part 4 (waves 25-30): the head relics' own models (gen_worn_head_r4.py)
    cat.relic("mask_of_indifference", "mask_indifference")
    cat.relic("mask_of_difference", "mask_difference")
    cat.relic("trickster_mask", "trickster_mask")
    cat.relic("bucket_of_disguise", "bucket_of_disguise")
    cat.relic("corpse_mask", "corpse_mask")
    cat.relic("mask_of_the_witchdoctor", "mask_of_the_witchdoctor")
    cat.relic("helmet_of_headbutting", "helmet_of_headbutting")
    cat.relic("soul_searchers", "soul_searchers")
    cat.relic("the_orb_of_ancient_sight", "the_orb_of_ancient_sight")
    cat.relic("jellyfish_cap", "jellyfish_cap")
    cat.relic("the_mad_kings_crown", "the_mad_kings_crown")
    cat.relic("mask_of_the_raven_queens_disciple", "mask_of_the_raven_queens_disciple")
    cat.relic("lens_of_straight_creepin", "lens_of_straight_creepin")
    cat.relic("death_knights_helmet", "death_knights_helmet")
    cat.relic("helm_of_whispers", "helm_of_whispers")
    cat.relic("mind_stone", "mind_stone")
    cat.relic("mask_of_mind_melting", "mask_of_mind_melting")
    cat.relic("frank", "frank")
    cat.relic("goggles_of_dwarvenkin", "goggles_of_dwarvenkin")
    cat.relic("eye_of_vecna", "eye_of_vecna")
    for rid, th in (("helm_of_whispers", "wisp"), ("mind_stone", "leaf"), ("trickster_mask", "wisp"), ("death_knights_helmet", "wisp"), ("the_orb_of_ancient_sight", "star")):
        cat.theme(rid, th)
    # ---- ambient particle themes where the glow colour alone would pick a poor one
    for rid, th in (("tiny_duck_crown", "star"), ("circlet_of_the_phoenix", "ember"), ("dragon_fire_boots", "ember"), ("cape_of_bees", "bee"),
                    ("cloak_of_the_first_snow", "snow"), ("autumn_cloak", "leaf"), ("darkened_cloak_of_elvenkind", "leaf"), ("nightstalkers_mantle", "wisp"),
                    ("the_amulet_of_lost_souls", "wisp"), ("unicorn_crown", "petal"), ("dark_unicorn_crown", "wisp"), ("amulet_of_the_forest", "leaf"),
                    ("cloak_of_the_moth_rider", "star"), ("godking_crown", "star"), ("greaves_of_the_wind_walker", "spark"), ("lifes_emergency", "ember"),
                    ("amulet_of_the_sea_of_stars", "star"), ("cloak_mimic_tame", "wisp"), ("urn_of_black_pudding", "ember")):
        cat.theme(rid, th)
    gen_worn_armor_relics.catalog_relics(cat)              # the armour relics that only had the model of their look
    gen_worn_armor.catalog_items(cat)                      # the armour items (the robe sets)
    gen_worn_armor.catalog_anime(cat)                      # 1.48.0: the same robe sets as anime cloaks (`anime:<id>` keys, chosen by relicfx.robeAnime)
    gen_worn_modes.catalog_modes(cat)                      # the transformation overlays
    gen_worn_armor.catalog_curios(cat)                     # Curios-worn items that are not relics
    return cat


def used_models(cat):
    names = set()
    for _s, _l, ps in cat.looks:
        for p in ps:
            names.add(p.split("@")[0].rstrip("~"))
    for _r, ps in cat.relics:
        for p in ps:
            names.add(p.split("@")[0].rstrip("~"))
    for _r, ps in cat.items:
        for p in ps:
            if p != gen_worn_armor.NONE_PIECE:                 # `none` draws nothing and has no model file
                names.add(p.split("@")[0].rstrip("~"))
    return names


def main(argv):
    check = "--check" in argv
    cat = catalog()
    names = sorted(used_models(cat))
    missing = [n for n in names if n not in BUILDERS]
    if missing:
        print("catalog names models that have no builder:", ", ".join(missing))
        return 1
    if not check:
        L.write_textures()
    verts = {}
    for n in sorted(BUILDERS):
        m = BUILDERS[n]()
        verts[n] = m.vertex_count()
        if not check:
            L.write_model(m)
    over = [(n, v) for n, v in verts.items() if v > MODEL_CAP]
    for n in sorted(verts):
        print("%-24s %5d%s" % (n, verts[n], "   OVER %d" % MODEL_CAP if verts[n] > MODEL_CAP else ""))
    bad = 0
    for rid, ps in cat.relics:
        tot = sum(verts[p.split("@")[0].rstrip("~")] for p in ps)
        if tot > ENSEMBLE_CAP:
            print("relic %s wears %d vertices (cap %d)" % (rid, tot, ENSEMBLE_CAP))
            bad += 1
    for rid, ps in cat.items:
        tot = sum(verts[p.split("@")[0].rstrip("~")] for p in ps if p != gen_worn_armor.NONE_PIECE)
        if tot > (MODE_CAP if rid.startswith("mode_") else ENSEMBLE_CAP):
            print("item %s wears %d vertices (cap %d)" % (rid, tot, ENSEMBLE_CAP))
            bad += 1
    if not check:
        cat.write()
    return 1 if over or bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
