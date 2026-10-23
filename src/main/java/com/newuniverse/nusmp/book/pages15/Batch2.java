package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Batch 2 of the "15 spells for every grimoire" project: Water, Ice, Fluids and Art (92 new pages in 17 books; every page is appended, nothing is replaced).
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_creation}, {@code book_mist}, {@code book_painting}, {@code book_snow}, {@code book_ice}, {@code book_water}, {@code book_demon_water}, {@code book_mucus}, {@code book_mud}, {@code book_food}, {@code book_eye}, {@code book_demon_ice}, {@code book_bubble}, {@code book_ice_wedge}, {@code book_gel}, {@code book_black_oil}, {@code book_eyeball}.
 *
 * <p>Every page is made with the {@link Pages15Kit} standard casts (bolt, volley, cone, nova, field, line, dash, bind, empower, healAllies) or with the five small custom casts at the bottom of this class
 * ({@link #selfBuff}, {@link #pull}, {@link #reel}, {@link #trap}; all built on the kit's bounded {@link Pages15Kit.Ctx}: at most the tier's targets, at most {@value Pages15Rules#MAX_PACKETS} packets, at most one
 * 20-tick wait, no entity, no block, no per-tick work). The table of pages, with the canon or original mark and the worst case of each, is docs/magic/pages15_batch2.md; the descriptions are tools/lang/pages15_batch2.json.
 * Pages are appended AFTER every old page of a book, in the order written here; never reorder or rename one once released (page ids are save keys).
 */
public final class Batch2 {
    private Batch2() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    /** No rider (declared before the pages: static initialisers run in written order). */
    private static final ElementBook.Rider NONE = Pages15Kit.NONE;

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- the books of this batch, one register(...) each

    private static final List<BookPage> CREATION = register("book_creation", List.of(
                Pages15Kit.starter("created_crystal_armor", "Creation: Crystal Armor",
                        selfBuff(Tier.STARTER, VfxShape.WIKI_MAGIC_FIELD, 40, self(MobEffects.DAMAGE_RESISTANCE, 200, 0), self(MobEffects.ABSORPTION, 200, 1))),
                Pages15Kit.mid("wind_hawk", "Creation: Wind Hawk",
                        Pages15Kit.volley(Tier.MID, 2, true, VfxShape.WIKI_MAGIC_CAST, NONE)).withCooldown(200).withAnim("out"),
                Pages15Kit.starter("climbing_coelacanth", "Creation: Climbing Coelacanth",
                        Pages15Kit.line(Tier.STARTER, 10, 1.2, VfxShape.WIKI_MAGIC_CAST, lift(0.5))).withAnim("thrust"),
                Pages15Kit.starter("created_spear", "Creation: Spear",
                        Pages15Kit.line(Tier.STARTER, 14, 1.0, VfxShape.WIKI_MAGIC_CAST, NONE)).withAnim("thrust"),
                Pages15Kit.starter("created_shield", "Creation: Shield",
                        Pages15Kit.empower(Tier.STARTER, 200, true, VfxShape.WIKI_MAGIC_FIELD)).withAnim("up"),
                Pages15Kit.mid("created_arrows", "Creation: Arrow Volley",
                        Pages15Kit.volley(Tier.MID, 4, true, VfxShape.WIKI_MAGIC_CAST, NONE)).withCooldown(200).withAnim("out"),
                Pages15Kit.mid("created_chains", "Creation: Chains",
                        Pages15Kit.bind(Tier.MID, 16, false, VfxShape.WIKI_MAGIC_CAST, weak(60, 0))).withCooldown(240).withAnim("thrust"),
                Pages15Kit.mid("created_hammer", "Creation: Hammer",
                        Pages15Kit.nova(Tier.MID, 4, true, VfxShape.WIKI_MAGIC_BURST, knock(1.0))).withCooldown(220).withAnim("slam"),
                Pages15Kit.mid("created_steed", "Creation: Steed",
                        Pages15Kit.dash(Tier.MID, false, 12, false, VfxShape.WIKI_MAGIC_CAST, NONE)).withCooldown(180).withAnim("out"),
                Pages15Kit.zone("created_cage", "Creation: Cage",
                        Pages15Kit.field(Tier.ZONE, 4, 80, 10, true, VfxShape.WIKI_MAGIC_FIELD, slow(30, 3))).withCooldown(420).withAnim("slam"),
                Pages15Kit.zone("created_repairs", "Creation: Field Repairs",
                        Pages15Kit.healAllies(8, 0.2f)).withCooldown(480).withAnim("up"),
                Pages15Kit.signature("created_colossus", "Creation: Colossus",
                        Pages15Kit.nova(Tier.SIGNATURE, 7, false, VfxShape.WIKI_MAGIC_BURST, all(knock(1.3), slow(60, 2)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> MIST = register("book_mist", List.of(
                Pages15Kit.mid("mist_doubles", "Mist Doubles",
                        ElementBook.decoy()).withCooldown(240),
                Pages15Kit.mid("mist_bullets", "Condensed Mist Bullets",
                        Pages15Kit.volley(Tier.MID, 4, true, VfxShape.MIST_MAGIC, NONE)).withCooldown(200).withAnim("out"),
                Pages15Kit.zone("concealing_fog", "Concealing Fog",
                        Pages15Kit.field(Tier.ZONE, 6, 100, 20, false, VfxShape.MIST_MAGIC, blind(40))).withCooldown(420),
                Pages15Kit.starter("dew_needles", "Dew Needles",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.MIST_MAGIC, NONE)).withAnim("out"),
                Pages15Kit.starter("rolling_fog", "Rolling Fog",
                        Pages15Kit.cone(Tier.STARTER, 8, 0.5, VfxShape.MIST_MAGIC, slow(40, 1))).withAnim("sweep"),
                Pages15Kit.starter("chilling_haze", "Chilling Haze",
                        Pages15Kit.nova(Tier.STARTER, 4, false, VfxShape.MIST_MAGIC, all(slow(40, 1), weak(60, 0)))).withAnim("out"),
                Pages15Kit.mid("vapor_lance", "Vapor Lance",
                        Pages15Kit.line(Tier.MID, 16, 1.0, VfxShape.MIST_MAGIC, NONE)).withCooldown(200).withAnim("thrust"),
                Pages15Kit.mid("morning_mist", "Morning Mist",
                        Pages15Kit.healAllies(6, 0.15f)).withCooldown(300).withAnim("up"),
                Pages15Kit.mid("mist_ambush", "Mist Ambush",
                        Pages15Kit.dash(Tier.MID, true, 12, true, VfxShape.MIST_MAGIC, NONE)).withCooldown(240).withAnim("thrust"),
                Pages15Kit.starter("mist_cloak", "Mist Cloak",
                        selfBuff(Tier.STARTER, VfxShape.MIST_MAGIC, 40, self(MobEffects.MOVEMENT_SPEED, 100, 0), self(MobEffects.INVISIBILITY, 60, 0))).withCooldown(160),
                Pages15Kit.signature("sea_of_fog", "Sea of Fog",
                        Pages15Kit.field(Tier.SIGNATURE, 8, 100, 10, false, VfxShape.MIST_MAGIC, all(slow(40, 2), blind(30)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> PAINTING = register("book_painting", List.of(
                Pages15Kit.mid("heat_infused_ice", "Painting Magic: Invasion of Heat-Infused Ice",
                        Pages15Kit.bind(Tier.MID, 18, false, VfxShape.PAINTING_FX2, weak(100, 1))).withCooldown(260).withAnim("thrust"),
                Pages15Kit.starter("color_splash", "Painting Magic: Colour Splash",
                        Pages15Kit.nova(Tier.STARTER, 3.5, true, VfxShape.PAINT_SPLAT, blind(30))).withAnim("slam"),
                Pages15Kit.starter("ink_darts", "Painting Magic: Ink Darts",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.PAINT_STROKE, slow(30, 0))).withAnim("out"),
                Pages15Kit.starter("canvas_screen", "Painting Magic: Canvas Screen",
                        selfBuff(Tier.STARTER, VfxShape.PAINT_CAMO, 40, self(MobEffects.DAMAGE_RESISTANCE, 120, 0), self(MobEffects.ABSORPTION, 160, 0))).withAnim("up"),
                Pages15Kit.starter("gloss_slide", "Painting Magic: Gloss Slide",
                        Pages15Kit.dash(Tier.STARTER, false, 10, false, VfxShape.PAINT_TRAIL, NONE)).withCooldown(140).withAnim("out"),
                Pages15Kit.mid("wet_paint", "Painting Magic: Wet Paint",
                        Pages15Kit.field(Tier.MID, 4, 60, 20, true, VfxShape.PAINT_SPLAT, slow(40, 2))).withCooldown(240).withAnim("slam"),
                Pages15Kit.mid("vague_azur", "Painting Magic: Vague d'Azur",
                        Pages15Kit.line(Tier.MID, 14, 2.0, VfxShape.PAINT_TRAIL, knock(0.8))).withCooldown(220).withAnim("sweep"),
                Pages15Kit.mid("etincelle_rouge", "Painting Magic: Étincelle Rouge",
                        Pages15Kit.bolt(Tier.MID, 3.0, VfxShape.PAINTING_FX1, VfxShape.PAINTING_FX3, ignite(3))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.zone("verdure_embrace", "Painting Magic: Verdure Embrace",
                        Pages15Kit.healAllies(7, 0.2f)).withCooldown(480).withAnim("up"),
                Pages15Kit.zone("pigment_storm", "Painting Magic: Pigment Storm",
                        Pages15Kit.field(Tier.ZONE, 6, 80, 10, true, VfxShape.PAINTING_FX2, blind(30))).withCooldown(420).withAnim("slam"),
                Pages15Kit.signature("gallery_of_giants", "Painting Magic: Gallery of Giants",
                        Pages15Kit.nova(Tier.SIGNATURE, 7, true, VfxShape.PAINT_BEAST, all(knock(1.2), slow(60, 2)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> SNOW = register("book_snow", List.of(
                Pages15Kit.zone("miniature_snow_garden", "Miniature Garden of Illusory Snow",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 20, true, VfxShape.SNOW_FX2, all(slow(60, 1), eff(MobEffects.DIG_SLOWDOWN, 60, 1)))).withCooldown(420),
                Pages15Kit.mid("snow_barrier", "Snow Barrier",
                        Pages15Kit.field(Tier.MID, 5, 80, 20, false, VfxShape.SNOW_FX2, blind(30))).withCooldown(260),
                Pages15Kit.starter("snow_binding", "Snow Binding",
                        Pages15Kit.bind(Tier.STARTER, 14, false, VfxShape.SNOW_FX3, slow(60, 1))).withAnim("thrust"),
                Pages15Kit.starter("snow_powder_burst", "Powder Burst",
                        Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.SNOW_FX3, knock(0.8))).withAnim("slam"),
                Pages15Kit.starter("snowball_barrage", "Snowball Barrage",
                        Pages15Kit.volley(Tier.STARTER, 4, false, VfxShape.SNOW_FX1, slow(30, 0))).withAnim("out"),
                Pages15Kit.mid("frostbite_wind", "Frostbite Wind",
                        Pages15Kit.cone(Tier.MID, 8, 0.5, VfxShape.SNOW_FX1, all(slow(60, 1), weak(60, 0)))).withCooldown(200).withAnim("sweep"),
                Pages15Kit.signature("snow_avalanche", "Snow Avalanche",
                        Pages15Kit.nova(Tier.SIGNATURE, 7, true, VfxShape.SNOW_FX3, all(knock(1.2), slow(80, 2)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> ICE = register("book_ice", List.of(
                Pages15Kit.signature("ice_funeral", "Ice Funeral",
                        Pages15Kit.cast(Tier.SIGNATURE, c -> {
                    Vec3 at = c.caster.position();
                    List<LivingEntity> foes = c.foes(at, 5.0 * c.size());
                    c.circle(1.4f);
                    c.fx(VfxShape.ICE_FX3, at, at.add(0, 1, 0), 40, 1.6f);
                    for (LivingEntity f : foes) { c.hit(f); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)); }
                    c.caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    return true;
                })).withCooldown(1000).withAnim("slam"),
                Pages15Kit.mid("ice_slicer", "Ice Slicer",
                        Pages15Kit.bolt(Tier.MID, 0, VfxShape.ICE_FX1, VfxShape.ICE_FX3, slow(80, 2))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.starter("ice_needle", "Ice Needle",
                        Pages15Kit.volley(Tier.STARTER, 2, false, VfxShape.ICE_FX1, slow(30, 1))).withAnim("out"),
                Pages15Kit.mid("jotunns_ice_floe", "Jötunn's Ice Floe",
                        trap(Tier.MID, 12, 4.0, VfxShape.ICE_FX1, VfxShape.ICE_FX3, slow(60, 2))).withCooldown(240).withAnim("slam"),
                Pages15Kit.zone("cage_of_infinite_hail", "Cage of Infinite Hail",
                        Pages15Kit.field(Tier.ZONE, 5, 100, 10, true, VfxShape.ICE_FX2, slow(40, 2))).withCooldown(450).withAnim("slam"),
                Pages15Kit.mid("endless_ice_fangs", "Endless Ice Fangs",
                        Pages15Kit.line(Tier.MID, 10, 2.5, VfxShape.ICE_FX3, lift(0.5))).withCooldown(220).withAnim("slam")));

    private static final List<BookPage> WATER = register("book_water", List.of(
                Pages15Kit.zone("sea_dragon_god_vortex", "Sea Dragon God's Vortex",
                        pull(Tier.ZONE, 5, true, VfxShape.WATERFX_ZONE, slow(30, 1))).withCooldown(450).withAnim("slam"),
                Pages15Kit.mid("sea_dragon_god_tail", "Sea Dragon God's Tail",
                        Pages15Kit.cone(Tier.MID, 7, 0.3, VfxShape.WATER_BURST, knock(1.1))).withCooldown(200).withAnim("sweep"),
                Pages15Kit.mid("riptide_undertow", "Riptide Undertow",
                        Pages15Kit.bind(Tier.MID, 16, false, VfxShape.WATERFX_CAST, slow(60, 2))).withCooldown(240).withAnim("thrust"),
                Pages15Kit.starter("riptide_dash", "Riptide Dash",
                        Pages15Kit.dash(Tier.STARTER, true, 8, false, VfxShape.WATERFX_CAST, knock(0.6))).withCooldown(120).withAnim("out"),
                Pages15Kit.starter("tidal_geyser", "Tidal Geyser",
                        Pages15Kit.nova(Tier.STARTER, 3.0, true, VfxShape.WATERFX_BURST, lift(0.7))).withAnim("slam")));

    private static final List<BookPage> DEMON_WATER = register("book_demon_water", List.of(
                Pages15Kit.starter("brine_lash", "Demon Water Magic: Brine Lash",
                        Pages15Kit.line(Tier.STARTER, 10, 1.0, VfxShape.DEMON_WATER_FX1, slow(40, 1))).withAnim("sweep"),
                Pages15Kit.mid("drowned_chains", "Demon Water Magic: Drowned Chains",
                        Pages15Kit.bind(Tier.MID, 16, false, VfxShape.DEMON_WATER_FX2, weak(80, 1))).withCooldown(260).withAnim("thrust"),
                Pages15Kit.mid("abyssal_ambush", "Demon Water Magic: Abyssal Ambush",
                        Pages15Kit.dash(Tier.MID, true, 12, true, VfxShape.DEMON_WATER_FX1, slow(40, 1))).withCooldown(240).withAnim("thrust"),
                Pages15Kit.zone("black_deluge", "Demon Water Magic: Black Deluge",
                        Pages15Kit.field(Tier.ZONE, 6, 100, 10, true, VfxShape.DEMON_WATER_FX2, slow(50, 2))).withCooldown(450).withAnim("slam"),
                Pages15Kit.starter("depth_pressure", "Demon Water Magic: Depth Pressure",
                        Pages15Kit.nova(Tier.STARTER, 4, false, VfxShape.DEMON_WATER_FX3, all(knock(0.6), eff(MobEffects.CONFUSION, 60, 0)))).withAnim("slam")));

    private static final List<BookPage> MUCUS = register("book_mucus", List.of(
                Pages15Kit.starter("mucus_spit", "Mucus Magic: Mucus Spit",
                        Pages15Kit.cone(Tier.STARTER, 6, 0.5, VfxShape.MUCUS_FX1, slow(50, 2))).withAnim("thrust"),
                Pages15Kit.starter("mucus_siphon", "Mucus Magic: Siphoning Mucus",
                        Pages15Kit.bolt(Tier.STARTER, 0, VfxShape.MUCUS_FX1, VfxShape.MUCUS_FX3, all(leech(2f), drain(0.006)))).withAnim("thrust"),
                Pages15Kit.mid("mucus_cocoon", "Mucus Magic: Mucus Cocoon",
                        selfBuff(Tier.MID, VfxShape.MUCUS_FX2, 50, self(MobEffects.DAMAGE_RESISTANCE, 160, 1), self(MobEffects.ABSORPTION, 200, 1))).withCooldown(300).withAnim("up"),
                Pages15Kit.mid("glob_slam", "Mucus Magic: Glob Slam",
                        Pages15Kit.nova(Tier.MID, 4, true, VfxShape.MUCUS_FX3, all(knock(0.6), slow(60, 2)))).withCooldown(220).withAnim("slam"),
                Pages15Kit.zone("mucus_whirl", "Mucus Magic: Mucus Whirlpool",
                        pull(Tier.ZONE, 5, true, VfxShape.MUCUS_FX2, slow(60, 2))).withCooldown(450).withAnim("slam")));

    private static final List<BookPage> MUD = register("book_mud", List.of(
                Pages15Kit.starter("mud_splash", "Mud Magic: Mud Splash",
                        Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.MUD_FX3, blind(40))).withAnim("slam"),
                Pages15Kit.starter("mud_slide", "Mud Magic: Mud Slide",
                        Pages15Kit.dash(Tier.STARTER, true, 8, false, VfxShape.MUD_FX1, knock(0.4))).withCooldown(120).withAnim("out"),
                Pages15Kit.mid("grasping_mud", "Mud Magic: Grasping Mud",
                        reel(Tier.MID, 16, VfxShape.MUD_FX1, slow(40, 2))).withCooldown(220).withAnim("thrust"),
                Pages15Kit.mid("mud_armor", "Mud Magic: Mud Armor",
                        Pages15Kit.empower(Tier.MID, 240, true, VfxShape.MUD_FX2, self(MobEffects.DAMAGE_RESISTANCE, 240, 0))).withAnim("up"),
                Pages15Kit.zone("quagmire", "Mud Magic: Quagmire",
                        Pages15Kit.field(Tier.ZONE, 5, 100, 10, true, VfxShape.MUD_FX2, slow(50, 3))).withCooldown(420).withAnim("slam")));

    private static final List<BookPage> FOOD = register("book_food", List.of(
                Pages15Kit.mid("mana_munch", "Mana Munch",
                        Pages15Kit.bolt(Tier.MID, 0, VfxShape.FOOD_FX1, VfxShape.FOOD_FX3, all(leech(2f), drain(0.012)))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.starter("restoring_plate", "Restoring Plate",
                        Pages15Kit.healAllies(6, 0.12f)).withCooldown(240).withAnim("up"),
                Pages15Kit.starter("spice_burst", "Spice Burst",
                        Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.FOOD_FX3, ignite(3))).withAnim("slam"),
                Pages15Kit.zone("syrup_flood", "Syrup Flood",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.FOOD_FX2, slow(40, 2))).withCooldown(420).withAnim("slam")));

    private static final List<BookPage> EYE = register("book_eye", List.of(
                Pages15Kit.mid("hypnotic_gaze", "Hypnotic Gaze",
                        Pages15Kit.bind(Tier.MID, 20, false, VfxShape.EYE_FX2, eff(MobEffects.CONFUSION, 80, 0))).withCooldown(260).withAnim("thrust"),
                Pages15Kit.starter("dazzle_sweep", "Dazzle Sweep",
                        Pages15Kit.cone(Tier.STARTER, 8, 0.4, VfxShape.EYE_FX1, blind(40))).withAnim("sweep"),
                Pages15Kit.starter("silencing_gaze", "Silencing Gaze",
                        Pages15Kit.line(Tier.STARTER, 16, 0.8, VfxShape.EYE_FX1, silence(60))).withAnim("thrust"),
                Pages15Kit.zone("watchful_circle", "Watchful Circle",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, false, VfxShape.EYE_FX2, all(weak(40, 0), glow(60)))).withCooldown(420)));

    private static final List<BookPage> DEMON_ICE = register("book_demon_ice", List.of(
                Pages15Kit.starter("demon_ice_fangs", "Demon Ice: Frost Fangs",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.DEMON_ICE_FX1, slow(40, 1))).withAnim("out"),
                Pages15Kit.mid("demon_ice_chains", "Demon Ice: Frozen Chains",
                        Pages15Kit.bind(Tier.MID, 16, true, VfxShape.DEMON_ICE_FX2, NONE)).withCooldown(300).withAnim("thrust"),
                Pages15Kit.zone("demon_ice_comet", "Demon Ice: Black Comet",
                        trap(Tier.ZONE, 20, 5.0, VfxShape.DEMON_ICE_FX1, VfxShape.DEMON_ICE_FX3, slow(80, 3))).withCooldown(450).withAnim("slam")));

    private static final List<BookPage> BUBBLE = register("book_bubble", List.of(
                Pages15Kit.starter("soap_slide", "Soap Slide",
                        Pages15Kit.dash(Tier.STARTER, false, 9, false, VfxShape.BUBBLE_FX1, NONE)).withCooldown(140).withAnim("out"),
                Pages15Kit.mid("bubble_whirl", "Bubble Whirl",
                        pull(Tier.MID, 4, true, VfxShape.BUBBLE_FX2, lift(0.3))).withCooldown(220).withAnim("slam"),
                Pages15Kit.zone("bubble_bath", "Bubble Bath",
                        Pages15Kit.healAllies(7, 0.2f)).withCooldown(480).withAnim("up")));

    private static final List<BookPage> ICE_WEDGE = register("book_ice_wedge", List.of(
                Pages15Kit.starter("wedge_anchor", "Ice Wedge: Wedge Anchor",
                        Pages15Kit.bind(Tier.STARTER, 14, false, VfxShape.ICE_WEDGE_FX3, slow(40, 1))).withAnim("thrust"),
                Pages15Kit.mid("wedge_spiral", "Ice Wedge: Wedge Spiral",
                        Pages15Kit.line(Tier.MID, 14, 1.5, VfxShape.ICE_WEDGE_FX1, slow(50, 1))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.zone("wedge_cataclysm", "Ice Wedge: Wedge Cataclysm",
                        trap(Tier.ZONE, 15, 5.0, VfxShape.ICE_WEDGE_FX1, VfxShape.ICE_WEDGE_FX3, all(knock(0.6), slow(60, 2)))).withCooldown(450).withAnim("slam")));

    private static final List<BookPage> GEL = register("book_gel", List.of(
                Pages15Kit.starter("gel_whip", "Gel Whip",
                        reel(Tier.STARTER, 12, VfxShape.GEL_FX1, slow(40, 1))).withAnim("sweep"),
                Pages15Kit.mid("gel_mortar", "Gel Mortar",
                        Pages15Kit.bolt(Tier.MID, 3.0, VfxShape.GEL_FX1, VfxShape.GEL_FX3, slow(60, 2))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.zone("gel_flood", "Gel Flood",
                        Pages15Kit.field(Tier.ZONE, 5, 100, 10, true, VfxShape.GEL_FX2, slow(50, 3))).withCooldown(420).withAnim("slam")));

    private static final List<BookPage> BLACK_OIL = register("book_black_oil", List.of(
                Pages15Kit.starter("oil_spray", "Oil Spray",
                        Pages15Kit.cone(Tier.STARTER, 7, 0.5, VfxShape.BLACK_OIL_FX1, eff(MobEffects.WITHER, 40, 0))).withAnim("sweep"),
                Pages15Kit.mid("tar_chains", "Tar Chains",
                        Pages15Kit.bind(Tier.MID, 16, false, VfxShape.BLACK_OIL_FX2, eff(MobEffects.WITHER, 60, 0))).withCooldown(260).withAnim("thrust"),
                Pages15Kit.zone("oil_geyser", "Oil Geyser",
                        trap(Tier.ZONE, 16, 5.0, VfxShape.BLACK_OIL_FX2, VfxShape.BLACK_OIL_FX3, all(lift(0.5), eff(MobEffects.WITHER, 60, 0)))).withCooldown(450).withAnim("slam")));

    private static final List<BookPage> EYEBALL = register("book_eyeball", List.of(
                Pages15Kit.starter("eyeball_volley", "Eyeball Volley",
                        Pages15Kit.volley(Tier.STARTER, 3, true, VfxShape.EYEBALL_FX1, NONE)).withAnim("out"),
                Pages15Kit.mid("optic_beam", "Optic Beam",
                        Pages15Kit.line(Tier.MID, 18, 1.0, VfxShape.EYEBALL_FX1, blind(40))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.zone("eyeball_storm", "Eyeball Storm",
                        Pages15Kit.field(Tier.ZONE, 6, 100, 10, false, VfxShape.EYEBALL_FX2, all(glow(60), weak(40, 0)))).withCooldown(420)));


    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }

    // ---------------------------------------------------------------- riders (what a hit does besides damage)
    private static ElementBook.Rider eff(Holder<MobEffect> e, int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(e, ticks, amp)); }
    private static ElementBook.Rider slow(int ticks, int amp) { return eff(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp); }
    private static ElementBook.Rider weak(int ticks, int amp) { return eff(MobEffects.WEAKNESS, ticks, amp); }
    private static ElementBook.Rider blind(int ticks) { return eff(MobEffects.BLINDNESS, ticks, 0); }
    private static ElementBook.Rider glow(int ticks) { return eff(MobEffects.GLOWING, ticks, 0); }
    private static ElementBook.Rider knock(double k) { return Pages15Kit.knock(k); }
    private static ElementBook.Rider lift(double y) { return Pages15Kit.lift(y); }
    private static ElementBook.Rider ignite(int seconds) { return Pages15Kit.ignite(seconds); }
    private static ElementBook.Rider leech(float hp) { return Pages15Kit.leech(hp); }
    private static ElementBook.Rider all(ElementBook.Rider... riders) { return Pages15Kit.all(riders); }
    /** Takes a small share of the target's magicule (at most 2 % per hit) and gives it to the caster. */
    private static ElementBook.Rider drain(double fraction) { return (t, p) -> EnergyBridge.drain(t, p, Math.min(fraction, 0.02)); }
    /** Tensura's silence (magic jamming) for at most 4 s. */
    private static ElementBook.Rider silence(int ticks) { return (t, p) -> EnergyBridge.effect(t, "silence", Math.min(ticks, 80), 0); }

    private static Supplier<MobEffectInstance> self(Holder<MobEffect> e, int ticks, int amp) { return () -> new MobEffectInstance(e, Math.min(ticks, 400), amp); }

    // ---------------------------------------------------------------- the custom casts (all through the kit's bounded Ctx)
    /** Self buff: the effects (each at most 20 s) on the caster and one effect packet. No target, no zone. */
    @SafeVarargs
    private static BookPage.Cast selfBuff(Tier t, VfxShape shape, int fxTicks, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> {
            c.circle(0.8f);
            for (Supplier<MobEffectInstance> e : effects) c.caster.addEffect(e.get());
            Vec3 at = c.caster.position();
            c.fx(shape, at, at.add(0, 1, 0), fxTicks, 1.0f);
            return true;
        });
    }

    /** Draws the caster's foes in {@code radius} (at the aim point or round the caster) toward the centre, one hit each. At most the tier's targets, one packet. */
    private static BookPage.Cast pull(Tier t, double radius, boolean atAim, VfxShape shape, ElementBook.Rider rider) {
        return Pages15Kit.cast(t, c -> {
            Vec3 centre = atAim ? c.aim(20) : c.caster.position();
            List<LivingEntity> foes = c.foes(centre, Math.min(radius, 8.0) * c.size());
            if (foes.isEmpty()) return c.fail("No one in reach.");
            c.circle(1f);
            c.fx(shape, centre, centre.add(0, 1, 0), 30, (float) radius / 3f);
            for (LivingEntity f : foes) {
                c.hit(f);
                rider.apply(f, c.caster);
                Vec3 d = centre.subtract(f.position());
                if (d.lengthSqr() > 0.04) {
                    f.setDeltaMovement(f.getDeltaMovement().add(d.normalize().scale(0.7)).add(0, 0.1, 0));
                    f.hurtMarked = true;
                }
            }
            return true;
        });
    }

    /** One hit on the target under the crosshair, then it is dragged toward the caster. One target, one packet. */
    private static BookPage.Cast reel(Tier t, double range, VfxShape shape, ElementBook.Rider rider) {
        return Pages15Kit.cast(t, c -> {
            LivingEntity f = c.target(Math.min(range, 24.0));
            if (f == null) return c.fail("No target.");
            c.circle(0.8f);
            c.fx(shape, c.caster.getEyePosition(), f.getBoundingBox().getCenter(), 14, 1.0f);
            c.hit(f);
            rider.apply(f, c.caster);
            Vec3 d = c.caster.position().subtract(f.position());
            if (d.lengthSqr() > 1.0) {
                f.setDeltaMovement(f.getDeltaMovement().add(d.normalize().scale(0.9)).add(0, 0.1, 0));
                f.hurtMarked = true;
            }
            return true;
        });
    }

    /** A delayed strike: the aim point is marked now and hit after {@code delay} ticks (at most 20; the kit's wait is 100 at most), once. At most the tier's targets, two packets. */
    private static BookPage.Cast trap(Tier t, int delay, double radius, VfxShape mark, VfxShape burst, ElementBook.Rider rider) {
        int wait = Math.max(4, Math.min(delay, 20));
        double r = Math.min(radius, 8.0);
        return Pages15Kit.cast(t, c -> {
            Vec3 at = c.aim(24);
            c.circle(0.9f);
            c.fx(mark, at, at.add(0, 1, 0), wait + 8, (float) r / 3f);
            c.later(wait, () -> {
                for (LivingEntity f : c.foes(at, r * c.size())) {
                    c.hit(f);
                    rider.apply(f, c.caster);
                }
                c.fx(burst, at, at.add(0, 1, 0), 24, (float) r / 3f);
            });
            return true;
        });
    }
}
