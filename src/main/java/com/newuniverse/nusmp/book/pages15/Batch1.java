package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.pages15.Pages15Kit.Ctx;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.newuniverse.nusmp.book.pages15.Pages15Kit.NONE;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.all;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.bind;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.bolt;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.cast;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.cone;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.dash;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.empower;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.field;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.healAllies;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.ignite;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.knock;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.lift;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.line;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.nova;
import static com.newuniverse.nusmp.book.pages15.Pages15Kit.volley;

/**
 * Batch 1 of the "15 spells for every grimoire" project: Flame, Lightning, Star and Thread.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_explosion}, {@code book_storm}, {@code book_thread}, {@code book_magma}, {@code book_sword}, {@code book_reinforcement}, {@code book_fire}, {@code book_star}, {@code book_lightning}, {@code book_demon_fire}.
 *
 * <p>Every page is built from the kit ({@link Pages15Kit}): the standard casts with the tier's damage, and a few custom casts (the helpers at the end of this file) that only use the bounded {@link Ctx}.
 * Pages are appended AFTER every old page of the book, in the order written here; never reorder or rename one once released (page ids are save keys).
 * Origin, sources and worst case of every page: docs/magic/pages15_batch1.md. Worst case of a page here: one server cast, at most the tier's targets hit, at most 10 effect packets, at most 6 delayed tasks of at most 40 ticks, nothing per tick afterwards.
 */
public final class Batch1 {
    private Batch1() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- the books of this batch
    private static final List<BookPage> EXPLOSION = register("book_explosion", List.of(
                Pages15Kit.starter("blast_step", "Blast Step",
                        dash(Tier.STARTER, true, 7, false, VfxShape.FLAME_EXPLOSION, knock(0.6))).withCooldown(100).withAnim("thrust"),
                Pages15Kit.starter("flash_bang", "Flash Bang",
                        nova(Tier.STARTER, 4, false, VfxShape.FIRE_BURST, blind(60))),
                Pages15Kit.starter("powder_burst", "Powder Burst",
                        cone(Tier.STARTER, 6, 0.55, VfxShape.FLAME_EXPLOSION, knock(1.0))).withCooldown(90).withAnim("thrust"),
                Pages15Kit.mid("delayed_charge", "Delayed Charge",
                        cast(Tier.MID, c -> delayedBlast(c, 40, 4.5, VfxShape.FLAME_EXPLOSION, all(knock(1.2), ignite(2)), 1.3f))).withCooldown(200),
                Pages15Kit.mid("shockwave_ring", "Shockwave Ring",
                        nova(Tier.MID, 6, false, VfxShape.WIND_RING, all(knock(1.5), lift(0.3)))).withCooldown(200).withAnim("slam"),
                Pages15Kit.mid("cluster_bomb", "Cluster Bomb",
                        volley(Tier.MID, 4, false, VfxShape.FLAME_TRAIL, ignite(2))).withCooldown(220),
                Pages15Kit.mid("blast_ward", "Blast Ward",
                        empower(Tier.MID, 200, true, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.ABSORPTION, 200, 1))).withCooldown(360),
                Pages15Kit.mid("blast_leap", "Blast Leap",
                        cast(Tier.MID, c -> leap(c, 0.85, 1.1, 3.5, VfxShape.FLAME_EXPLOSION, knock(0.8)))).withCooldown(180).withAnim("up"),
                Pages15Kit.zone("carpet_blast", "Carpet Blast",
                        cast(Tier.ZONE, c -> strikes(c, 5, 5, 3.0, true, 2.6, VfxShape.FLAME_EXPLOSION, knock(0.6)))).withCooldown(420).withAnim("out"),
                Pages15Kit.zone("volatile_ground", "Volatile Ground",
                        field(Tier.ZONE, 5.5, 100, 25, true, VfxShape.FLAME_EXPLOSION, all(ignite(2), knock(0.4)))).withCooldown(480),
                Pages15Kit.signature("megaton_crater", "Megaton Crater",
                        nova(Tier.SIGNATURE, 7, true, VfxShape.MAGIC_CIRCLE_EXPLOSION, all(knock(1.5), ignite(4)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> STORM = register("book_storm", List.of(
                Pages15Kit.starter("gust_jab", "Gust Jab",
                        cone(Tier.STARTER, 5, 0.6, VfxShape.FX_WIND_GUST, knock(1.2))),
                Pages15Kit.starter("static_snap", "Static Snap",
                        bolt(Tier.STARTER, 2.5, VfxShape.LIGHTNING_SPEAR, null, slow(30, 1))),
                Pages15Kit.starter("squall_step", "Squall Step",
                        dash(Tier.STARTER, true, 8, false, VfxShape.FX_WIND_GUST, knock(0.5))).withCooldown(100).withAnim("thrust"),
                Pages15Kit.mid("rain_curtain", "Rain Curtain",
                        empower(Tier.MID, 200, false, VfxShape.WIND_RING, () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0), () -> new MobEffectInstance(MobEffects.ABSORPTION, 200, 1))).withCooldown(360),
                Pages15Kit.mid("thunderhead_strike", "Thunderhead Strike",
                        cast(Tier.MID, c -> skyStrike(c, 28, 1.2f, VfxShape.STORM_BOLT, slow(40, 1)))).withCooldown(200),
                Pages15Kit.mid("hailstone_volley", "Hailstone Volley",
                        volley(Tier.MID, 5, false, VfxShape.WIND_SLASH, slow(40, 1))).withCooldown(220),
                Pages15Kit.mid("squall_front", "Squall Front",
                        line(Tier.MID, 14, 2.5, VfxShape.STORM_FRONT, knock(1.4))).withCooldown(240).withAnim("out"),
                Pages15Kit.mid("downburst", "Downburst",
                        nova(Tier.MID, 5, true, VfxShape.STORM_FALL, all(knock(1.0), slow(40, 2)))).withCooldown(260).withAnim("slam"),
                Pages15Kit.zone("thundercloud", "Thundercloud",
                        field(Tier.ZONE, 6, 100, 20, false, VfxShape.STORM_CLOUD, all(lift(0.25), knock(0.4)))).withCooldown(480),
                Pages15Kit.zone("tempest_barrage", "Tempest Barrage",
                        cast(Tier.ZONE, c -> smite(c, 12, 4, VfxShape.STORM_BOLT, NONE))).withCooldown(450),
                Pages15Kit.signature("storm_maelstrom", "Storm Maelstrom",
                        nova(Tier.SIGNATURE, 8, true, VfxShape.STORM_CLOUD, all(knock(1.5), lift(0.5)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> THREAD = register("book_thread", List.of(
                Pages15Kit.starter("thread_whip", "Thread Whip",
                        line(Tier.STARTER, 8, 1.0, VfxShape.THREAD_LINE, pull(0.7))).withCooldown(90).withAnim("sweep"),
                Pages15Kit.starter("tripwire_snare", "Tripwire Snare",
                        cone(Tier.STARTER, 5, 0.5, VfxShape.THREAD_TRAP_LINE, slow(60, 3))),
                Pages15Kit.starter("thread_needle", "Thread Needle",
                        bolt(Tier.STARTER, 0, VfxShape.THREAD_LINE, null, weak(60, 0))),
                Pages15Kit.mid("thread_cocoon", "Thread Cocoon",
                        empower(Tier.MID, 160, true, VfxShape.THREAD_COCOON, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0), () -> new MobEffectInstance(MobEffects.ABSORPTION, 160, 0))).withCooldown(360),
                Pages15Kit.mid("puppet_strings", "Puppet Strings",
                        bind(Tier.MID, 16, false, VfxShape.THREAD_STRINGS, weak(80, 1))).withCooldown(260),
                Pages15Kit.mid("silk_swing", "Silk Swing",
                        dash(Tier.MID, false, 10, false, VfxShape.THREAD_LINE, NONE)).withCooldown(160).withAnim("thrust"),
                Pages15Kit.mid("razor_weave", "Razor Weave",
                        cone(Tier.MID, 7, 0.4, VfxShape.THREAD_LINE, slow(40, 1))).withCooldown(200).withAnim("sweep"),
                Pages15Kit.mid("stitching_threads", "Stitching Threads",
                        healAllies(6, 0.15f)).withCooldown(360),
                Pages15Kit.zone("thousand_threads", "Thousand Threads",
                        volley(Tier.ZONE, 5, true, VfxShape.THREAD_LINE, pull(0.4))).withCooldown(400),
                Pages15Kit.zone("thread_cage", "Thread Cage",
                        nova(Tier.ZONE, 5, true, VfxShape.THREAD_COCOON, all(slow(60, 3), weak(60, 0)))).withCooldown(420).withAnim("slam"),
                Pages15Kit.signature("crimson_loom", "Crimson Loom",
                        nova(Tier.SIGNATURE, 8, true, VfxShape.THREAD_WEB, all(slow(80, 3), pull(0.8)))).withCooldown(900).withAnim("slam")));

    private static final List<BookPage> MAGMA = register("book_magma", List.of(
                Pages15Kit.starter("ember_splash", "Ember Splash",
                        cone(Tier.STARTER, 5, 0.5, VfxShape.FLAME_TRAIL, ignite(4))),
                Pages15Kit.starter("lava_whip", "Lava Whip",
                        line(Tier.STARTER, 9, 1.0, VfxShape.FLAME_TRAIL, ignite(3))).withCooldown(90).withAnim("sweep"),
                Pages15Kit.starter("molten_stride", "Molten Stride",
                        dash(Tier.STARTER, true, 6, false, VfxShape.FLAME_TRAIL, ignite(3))).withCooldown(110).withAnim("thrust"),
                Pages15Kit.mid("obsidian_crust", "Obsidian Crust",
                        empower(Tier.MID, 200, true, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0), () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0))).withCooldown(380),
                Pages15Kit.mid("magma_geyser", "Magma Geyser",
                        nova(Tier.MID, 4, true, VfxShape.FIRE_PILLAR, all(lift(0.6), ignite(4)))).withCooldown(220).withAnim("slam"),
                Pages15Kit.mid("slag_shower", "Slag Shower",
                        volley(Tier.MID, 5, false, VfxShape.FLAME_TRAIL, ignite(3))).withCooldown(230),
                Pages15Kit.mid("molten_fist", "Molten Fist",
                        cone(Tier.MID, 3.5, 0.8, VfxShape.FLAME_EXPLOSION, all(ignite(5), knock(1.4)))).withCooldown(190).withAnim("thrust"),
                Pages15Kit.zone("lava_flow", "Lava Flow",
                        line(Tier.ZONE, 14, 2.5, VfxShape.EARTH_SPIKES, all(ignite(5), slow(40, 1)))).withCooldown(380).withAnim("out"),
                Pages15Kit.zone("pyroclastic_surge", "Pyroclastic Surge",
                        cone(Tier.ZONE, 9, 0.35, VfxShape.FIRE_WILD, all(ignite(5), knock(1.2)))).withCooldown(420).withAnim("out"),
                Pages15Kit.signature("supervolcano", "Supervolcano",
                        nova(Tier.SIGNATURE, 8, false, VfxShape.FIRE_WILD, all(ignite(6), lift(0.7), knock(1.2)))).withCooldown(1000).withAnim("slam")));

    private static final List<BookPage> SWORD = register("book_sword", List.of(
                Pages15Kit.starter("blade_flurry", "Blade Flurry",
                        cone(Tier.STARTER, 4, 0.65, VfxShape.WIND_SLASH, NONE)).withAnim("sweep"),
                Pages15Kit.starter("sword_step", "Sword Step",
                        dash(Tier.STARTER, true, 6, false, VfxShape.WIND_SLASH, NONE)).withCooldown(100).withAnim("thrust"),
                Pages15Kit.starter("light_edge", "Light Edge",
                        bolt(Tier.STARTER, 0, VfxShape.LIGHT_BLADE, null, NONE)).withCooldown(90).withAnim("thrust"),
                Pages15Kit.mid("crescent_wave", "Crescent Wave",
                        bolt(Tier.MID, 2.5, VfxShape.WIND_SLASH, VfxShape.WIND_RING, knock(0.8))).withCooldown(200).withAnim("sweep"),
                Pages15Kit.mid("light_thrust", "Light Thrust",
                        line(Tier.MID, 9, 0.9, VfxShape.LIGHT_BLADE, knock(0.6))).withCooldown(190).withAnim("thrust"),
                Pages15Kit.mid("blade_ward", "Blade Ward",
                        empower(Tier.MID, 200, true, VfxShape.WEAPON_CONSTRUCTS, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0))).withCooldown(380),
                Pages15Kit.mid("rising_cleave", "Rising Cleave",
                        cone(Tier.MID, 4.5, 0.7, VfxShape.WIND_SLASH, lift(0.7))).withCooldown(200).withAnim("up"),
                Pages15Kit.zone("sword_storm", "Sword Storm",
                        field(Tier.ZONE, 5, 100, 20, false, VfxShape.WEAPON_CONSTRUCTS, knock(0.3))).withCooldown(450),
                Pages15Kit.zone("skyfall_blades", "Skyfall Blades",
                        cast(Tier.ZONE, c -> strikes(c, 5, 4, 3.0, false, 2.2, VfxShape.KOTO_SWORDS, NONE))).withCooldown(420).withAnim("up"),
                Pages15Kit.signature("sovereign_blade", "Sovereign Blade",
                        line(Tier.SIGNATURE, 20, 2.5, VfxShape.LIGHT_BLADE, knock(1.0))).withCooldown(900).withAnim("thrust")));

    private static final List<BookPage> REINFORCEMENT = register("book_reinforcement", List.of(
                Pages15Kit.starter("leg_strength_boost", "Leg Strength Boost",
                        empower(Tier.STARTER, 160, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1), () -> new MobEffectInstance(MobEffects.JUMP, 160, 1))).withCooldown(200),
                Pages15Kit.starter("rushing_fist", "Rushing Fist",
                        dash(Tier.STARTER, true, 5, false, VfxShape.WIND_SLASH, knock(0.9))).withCooldown(100).withAnim("thrust"),
                Pages15Kit.starter("reinforced_guard", "Reinforced Guard",
                        empower(Tier.STARTER, 100, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1))).withCooldown(240),
                Pages15Kit.mid("arm_strength_boost", "Arm Strength Boost",
                        empower(Tier.MID, 200, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1))).withCooldown(360),
                Pages15Kit.mid("shoulder_charge", "Shoulder Charge",
                        dash(Tier.MID, true, 8, false, VfxShape.WIND_SLASH, knock(1.4))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.mid("rising_kick", "Rising Kick",
                        cone(Tier.MID, 3.5, 0.7, VfxShape.WIND_SLASH, lift(0.8))).withCooldown(190).withAnim("up"),
                Pages15Kit.mid("ironclad_body", "Ironclad Body",
                        empower(Tier.MID, 220, true, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0), () -> new MobEffectInstance(MobEffects.ABSORPTION, 220, 0))).withCooldown(400),
                Pages15Kit.zone("fist_barrage", "Hundred-Fist Barrage",
                        cast(Tier.ZONE, c -> barrage(c, 5.0, 6, 4))).withCooldown(380).withAnim("thrust"),
                Pages15Kit.zone("skyfall_smash", "Skyfall Smash",
                        cast(Tier.ZONE, c -> skySmash(c))).withCooldown(420).withAnim("slam"),
                Pages15Kit.signature("titan_body", "Titan Body",
                        empower(Tier.SIGNATURE, 280, false, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 280, 1), () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 280, 1), () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 280, 0))).withCooldown(1200).withAnim("up")));

    private static final List<BookPage> FIRE = register("book_fire", List.of(
                Pages15Kit.starter("flame_rope_binding_formation", "Flame Rope Binding Formation",
                        bind(Tier.STARTER, 14, false, VfxShape.FLAME_TRAIL, ignite(3))).withCooldown(140),
                Pages15Kit.starter("gamblers_base_run", "Gambler's Base Run",
                        dash(Tier.STARTER, true, 8, false, VfxShape.FLAME_TRAIL, ignite(2))).withCooldown(110).withAnim("thrust"),
                Pages15Kit.starter("cinder_jab", "Cinder Jab",
                        cone(Tier.STARTER, 4, 0.7, VfxShape.FIRE_BURST, all(ignite(3), knock(0.6)))).withAnim("thrust"),
                Pages15Kit.mid("prison_kill_buckshot", "Prison Kill Buckshot",
                        volley(Tier.MID, 5, true, VfxShape.FLAME_TRAIL, ignite(3))).withCooldown(230),
                Pages15Kit.mid("smoulder_veil", "Smoulder Veil",
                        empower(Tier.MID, 200, true, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0))).withCooldown(380),
                Pages15Kit.zone("purgatory_cremation", "Calidus Brachium Purgatory: Cremation",
                        field(Tier.ZONE, 5, 100, 20, false, VfxShape.FIRE_WILD, ignite(5))).withCooldown(480),
                Pages15Kit.zone("flame_lightning_explosive_cannon", "Flame-Lightning Explosive Cannon",
                        line(Tier.ZONE, 18, 2.0, VfxShape.FIRE_SPEAR, all(ignite(4), knock(1.2)))).withCooldown(420).withAnim("thrust"),
                Pages15Kit.zone("excelitius_leonum", "Excelitius Leonum",
                        volley(Tier.ZONE, 4, true, VfxShape.FIRE_LION, ignite(4))).withCooldown(480).withAnim("up"),
                Pages15Kit.signature("purgatory_flame_burial", "Calidus Brachium Purgatory: Flame Burial",
                        empower(Tier.SIGNATURE, 300, true, VfxShape.SPIRIT_AURA, () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0), () -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0))).withCooldown(1000).withAnim("up")));

    private static final List<BookPage> STAR = register("book_star", List.of(
                Pages15Kit.starter("quartile_flagellum", "Quartile Flagellum",
                        cone(Tier.STARTER, 8, 0.5, VfxShape.STAR_MAGIC, glow(60))).withCooldown(90).withAnim("sweep"),
                Pages15Kit.starter("twinkle_dart", "Twinkle Dart",
                        bolt(Tier.STARTER, 0, VfxShape.STAR_MAGIC, null, glow(80))),
                Pages15Kit.mid("star_conjunction", "Conjunction",
                        dash(Tier.MID, false, 12, false, VfxShape.STAR_MAGIC, NONE)).withCooldown(160).withAnim("thrust"),
                Pages15Kit.mid("never_never_land", "Never Never Land",
                        cast(Tier.MID, c -> neverNeverLand(c))).withCooldown(400),
                Pages15Kit.mid("star_beam", "Star Beam",
                        line(Tier.MID, 16, 1.2, VfxShape.STAR_MAGIC, glow(60))).withCooldown(200).withAnim("thrust"),
                Pages15Kit.mid("star_chain", "Star Chain",
                        bind(Tier.MID, 18, false, VfxShape.THREAD_LINE, glow(100))).withCooldown(260),
                Pages15Kit.zone("orbit_barrage", "Orbit Barrage",
                        volley(Tier.ZONE, 5, true, VfxShape.STAR_MAGIC, glow(60))).withCooldown(400),
                Pages15Kit.zone("stardust_field", "Stardust Field",
                        field(Tier.ZONE, 5, 100, 20, true, VfxShape.STAR_MAGIC, all(glow(40), slow(30, 1)))).withCooldown(450),
                Pages15Kit.signature("meteor_swarm", "Meteor Swarm",
                        cast(Tier.SIGNATURE, c -> strikes(c, 6, 6, 3.5, false, 2.6, VfxShape.MAGIC_CIRCLE_EXPLOSION, knock(0.8)))).withCooldown(900).withAnim("up")));


    // ---------------------------------------------------------------- riders (what a hit does besides damage)
    private static ElementBook.Rider slow(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp)); }

    private static ElementBook.Rider weak(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, ticks, amp)); }

    private static ElementBook.Rider blind(int ticks) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0)); }

    private static ElementBook.Rider glow(int ticks) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.GLOWING, ticks, 0)); }

    /** Drags the target a little toward the caster (a push, not a control lock). */
    private static ElementBook.Rider pull(double strength) {
        return (t, p) -> {
            Vec3 d = p.position().subtract(t.position());
            if (d.lengthSqr() < 1.0e-4) return;
            d = d.normalize().scale(Math.min(strength, 1.0));
            t.setDeltaMovement(t.getDeltaMovement().add(d.x, 0.15, d.z));
            t.hurtMarked = true;
        };
    }

    // ---------------------------------------------------------------- custom casts (only the bounded Ctx; no entity, no block, no scan beyond the spell's own box)
    private static boolean notFoe(Ctx c, LivingEntity t) { return t == null || t == c.caster || t.isAlliedTo(c.caster); }

    /** A charge at the aim point that goes off after {@code delay} ticks (1..100) in {@code radius} blocks; {@code mult} (at most 1.5) of the tier's hit. */
    private static boolean delayedBlast(Ctx c, int delay, double radius, VfxShape shape, ElementBook.Rider rider, float mult) {
        Vec3 at = c.aim(24);
        double r = radius * c.size();
        float raw = c.tier.hit * Math.min(mult, 1.5f);
        c.circle(0.8f);
        c.fx(VfxShape.MAGIC_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), delay, (float) (r / 1.6));
        c.later(delay, () -> {
            for (LivingEntity t : c.foes(at, r)) { c.hit(t, raw); rider.apply(t, c.caster); }
            c.fx(shape, at, at.add(0, 1, 0), 24, (float) (r / 3));
        });
        return true;
    }

    /** Up to 6 blasts, {@code gap} ticks apart: in a line walking away from the caster ({@code along}) or scattered round the aim point. Together about 1.4 hits of the tier on one spot. */
    private static boolean strikes(Ctx c, int n, int gap, double step, boolean along, double radius, VfxShape shape, ElementBook.Rider rider) {
        int count = Math.max(1, Math.min(n, 6));
        Vec3 base = along ? c.caster.position() : c.aim(24);
        Vec3 look = c.caster.getViewVector(1f).multiply(1, 0, 1);
        Vec3 dir = look.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : look.normalize();
        double r = radius * c.size();
        float raw = c.tier.hit * 1.4f / count;
        c.circle(1f);
        for (int k = 0; k < count; k++) {
            double ring = step * 0.8 * (0.35 + 0.65 * k / count), ang = k * 2.4;
            Vec3 at = along ? base.add(dir.scale(3.0 + step * k)) : base.add(Math.cos(ang) * ring, 0, Math.sin(ang) * ring);
            c.later(Math.max(1, k * Math.max(2, gap)), () -> {
                for (LivingEntity t : c.foes(at, r)) { c.hit(t, raw); rider.apply(t, c.caster); }
                c.fx(shape, at, at.add(0, 1, 0), 20, (float) (r / 3));
            });
        }
        return true;
    }

    /** A bolt from the sky on each of the tier's foes within {@code range} blocks of the caster, {@code gap} ticks apart. */
    private static boolean smite(Ctx c, double range, int gap, VfxShape shape, ElementBook.Rider rider) {
        List<LivingEntity> foes = c.foes(c.caster.position(), range * c.size());
        if (foes.isEmpty()) return c.fail("No foe in reach.");
        c.circle(1f);
        float raw = c.tier.hit * 0.75f;
        for (int k = 0; k < foes.size(); k++) {
            LivingEntity t = foes.get(k);
            c.later(4 + k * Math.max(2, gap), () -> {
                if (!t.isAlive()) return;
                c.hit(t, raw);
                rider.apply(t, c.caster);
                Vec3 at = t.position();
                c.fx(shape, at.add(0, 14, 0), at, 14, 1f);
            });
        }
        return true;
    }

    /** One bolt from the sky onto the foe under the crosshair. */
    private static boolean skyStrike(Ctx c, double range, float mult, VfxShape shape, ElementBook.Rider rider) {
        LivingEntity t = c.target(range);
        if (notFoe(c, t)) return c.fail("No target.");
        Vec3 at = t.position();
        c.circle(0.9f);
        c.fx(shape, at.add(0, 16, 0), at, 14, 1f);
        c.hit(t, c.tier.hit * Math.min(mult, 1.5f));
        rider.apply(t, c.caster);
        c.impact(at.add(0, 1, 0), 0.8f);
        return true;
    }

    /** A blast under the caster's feet hurts foes within {@code radius} blocks, then the caster is thrown up and forward and floats down softly for 2 seconds. */
    private static boolean leap(Ctx c, double up, double fwd, double radius, VfxShape shape, ElementBook.Rider rider) {
        ServerPlayer p = c.caster;
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        Vec3 dir = look.lengthSqr() < 1.0e-4 ? Vec3.ZERO : look.normalize();
        c.circle(1f);
        for (LivingEntity t : c.foes(p.position(), radius * c.size())) { c.hit(t); rider.apply(t, p); }
        c.fx(shape, p.position(), p.position().add(0, 1, 0), 20, (float) (radius / 3));
        p.setDeltaMovement(dir.x * fwd, up, dir.z * fwd);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0));
        return true;
    }

    /** {@code count} (2..6) quick blows on one foe within {@code range} blocks, {@code gap} ticks apart; together about 1.5 hits of the tier, the last one shoves. */
    private static boolean barrage(Ctx c, double range, int count, int gap) {
        LivingEntity t = c.target(range);
        if (notFoe(c, t)) return c.fail("No one in reach.");
        int n = Math.max(2, Math.min(count, 6));
        float raw = c.tier.hit * 1.5f / n;
        c.circle(0.9f);
        for (int k = 0; k < n; k++) {
            boolean last = k == n - 1;
            c.later(1 + k * Math.max(2, gap), () -> {
                if (!t.isAlive()) return;
                c.hit(t, raw);
                if (last) knock(1.2).apply(t, c.caster);
                c.impact(t.position().add(0, t.getBbHeight() / 2, 0), 0.4f);
            });
        }
        return true;
    }

    /** Jump high, drive yourself down ten ticks later and shatter the ground under you: foes within 5.5 blocks are hurt and thrown back. Slow falling for 3 seconds covers the landing. */
    private static boolean skySmash(Ctx c) {
        ServerPlayer p = c.caster;
        c.circle(1f);
        p.setDeltaMovement(p.getDeltaMovement().x, 0.95, p.getDeltaMovement().z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        c.later(10, () -> {
            p.setDeltaMovement(0, -1.8, 0);
            p.hurtMarked = true;
        });
        c.later(16, () -> {
            p.fallDistance = 0;
            for (LivingEntity t : c.foes(p.position(), 5.5 * c.size())) { c.hit(t); knock(1.3).apply(t, p); }
            c.fx(VfxShape.EARTH_SPIKES, p.position(), p.position().add(0, 1, 0), 20, 1.8f);
        });
        return true;
    }

    /** A solo reading of the union spell: you are faster and stronger for 10 seconds, the tier's foes within 8 blocks are weakened and slowed. */
    private static boolean neverNeverLand(Ctx c) {
        ServerPlayer p = c.caster;
        c.circle(1.2f);
        c.fx(VfxShape.STAR_MAGIC, p.position(), p.position().add(0, 1, 0), 40, 2.6f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
        for (LivingEntity t : c.foes(p.position(), 8 * c.size())) {
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        }
        return true;
    }

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
