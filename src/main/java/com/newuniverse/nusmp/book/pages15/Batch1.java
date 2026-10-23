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
