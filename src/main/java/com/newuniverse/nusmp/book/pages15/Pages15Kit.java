package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.TensuraShots;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Why: eight builders write about 700 pages for the "15 spells for every grimoire" project and every one needs the same boilerplate. This kit is the whole of it, built only from helpers the books already use
 * ({@link ElementBook} shapes, {@link TensuraShots}, {@link SpellRuntime}, {@link GrimoireBook#hurt}); there is no new engine. It does three things:
 * <ol>
 *   <li><b>Pages</b>: {@link #starter}, {@link #mid}, {@link #zone}, {@link #signature} make a {@link BookPage} with the tier's cost and the tier's default cooldown ({@link Tier}); chain
 *       {@code .withCooldown(ticks)} (kept inside the tier bounds, checked by the tests) and {@code .withAnim("thrust" | "sweep" | "slam" | "up" | "out")} like every other page.</li>
 *   <li><b>Standard casts</b> ({@link #bolt}, {@link #volley}, {@link #cone}, {@link #nova}, {@link #field}, {@link #line}, {@link #dash}, {@link #bind}, {@link #empower}, {@link #shot}, {@link #healAllies}, ...): the
 *       shared spell shapes with the DAMAGE chosen by the tier, so a starter hits for what the book's other starters do and a zone page for what its zone pages do. You pass only the picture
 *       ({@link VfxShape}, the book's own effect), the size and a rider (burn, slow, knockback ...).</li>
 *   <li><b>Custom casts</b>: {@link #cast} hands a {@link Ctx} to your lambda. Everything a custom spell may do is a method of the Ctx and each one is bounded: at most the tier's number of targets per call, every hit
 *       clamped to the tier's maximum, at most {@value Pages15Rules#MAX_PACKETS} effect packets per cast, one standing zone of at most {@value Pages15Rules#MAX_ZONE_TICKS} ticks in steps of at least
 *       {@value Pages15Rules#MIN_ZONE_STEP} ticks, and at most {@value #MAX_ZONES_PER_PLAYER} zones of this kit per player at once. No entity is spawned, no chunk is loaded, nothing is scanned beyond the spell's box.</li>
 * </ol>
 * Server only (a page cast runs on the server); nothing here touches a client class. Worst case of any page made with this kit: the figure of {@link Pages15Rules}.
 */
public final class Pages15Kit {
    private Pages15Kit() {}

    /** Zones of this kit one player can have running at once. */
    public static final int MAX_ZONES_PER_PLAYER = 2;

    // ------------------------------------------------------------------ pages by tier
    private static BookPage page(Tier t, String id, String name, BookPage.Cast c) {
        return new BookPage(id, name, name, t.costPercent, t.costFloor, t.cooldown, c);
    }

    /** Starter page: 8 % of the magicule, default cooldown 4 s (bounds 4..20 s). Opens early. */
    public static BookPage starter(String id, String name, BookPage.Cast c) { return page(Tier.STARTER, id, name, c); }

    /** Mid page: 12 %, default cooldown 8 s (bounds 8..35 s). */
    public static BookPage mid(String id, String name, BookPage.Cast c) { return page(Tier.MID, id, name, c); }

    /** Zone page: 20 %, default cooldown 15 s (bounds 15..70 s); needs Intermediate rank by default (pages.gateZone). */
    public static BookPage zone(String id, String name, BookPage.Cast c) { return page(Tier.ZONE, id, name, c); }

    /** Signature page: 35 %, default cooldown 30 s (bounds 30..150 s); needs the full mastery bar and Senior rank. At most ONE per book. */
    public static BookPage signature(String id, String name, BookPage.Cast c) { return page(Tier.SIGNATURE, id, name, c); }

    // ------------------------------------------------------------------ riders (what a hit does besides damage), re-exported so a batch file imports one class
    public static final ElementBook.Rider NONE = ElementBook.NONE;

    public static ElementBook.Rider effect(Supplier<MobEffectInstance> e) { return ElementBook.effect(e); }

    public static ElementBook.Rider ignite(int seconds) { return ElementBook.ignite(Math.min(seconds, 8)); }

    public static ElementBook.Rider knock(double strength) { return ElementBook.knock(Math.min(strength, 1.5)); }

    public static ElementBook.Rider lift(double y) { return ElementBook.lift(Math.min(y, 0.8)); }

    public static ElementBook.Rider leech(float hp) { return ElementBook.leech(Math.min(hp, 3f)); }

    public static ElementBook.Rider strip() { return ElementBook.strip(); }

    public static ElementBook.Rider all(ElementBook.Rider... riders) { return ElementBook.all(riders); }

    // ------------------------------------------------------------------ standard casts, damage by tier
    /** One flying hitbox (speed 1.6 blocks a tick, 24 ticks of life). {@code aoe} &gt; 0 makes it burst on impact (at most 5 blocks). */
    public static BookPage.Cast bolt(Tier t, double aoe, VfxShape trail, VfxShape burst, ElementBook.Rider rider) {
        return ElementBook.bolt(t.hit, 1.6, 0.6, 24, false, Math.min(aoe, 5.0), trail, burst, rider);
    }

    /** {@code n} (2..5) bolts in a fan; the tier's damage is shared between them (a full volley is about 1.2 hits of the tier). */
    public static BookPage.Cast volley(Tier t, int n, boolean homing, VfxShape trail, ElementBook.Rider rider) {
        int k = Math.max(2, Math.min(5, n));
        return ElementBook.volley(k, Math.max(2f, t.hit * 1.2f / k), 1.8, homing, trail, rider);
    }

    /** A cone in front of you ({@code range} up to 10 blocks; {@code width} is the minimum dot product, 0.4 = wide, 0.8 = narrow). */
    public static BookPage.Cast cone(Tier t, double range, double width, VfxShape shape, ElementBook.Rider rider) {
        return ElementBook.cone(t.hit, Math.min(range, 10.0), width, shape, rider);
    }

    /** A burst around you, or at the aim point ({@code radius} up to 8 blocks). */
    public static BookPage.Cast nova(Tier t, double radius, boolean atAim, VfxShape shape, ElementBook.Rider rider) {
        return ElementBook.nova(t.hit, Math.min(radius, 8.0), atAim, shape, rider);
    }

    /**
     * A lingering field. Bounded here: at most {@value Pages15Rules#MAX_ZONE_TICKS} ticks, a step of at least {@value Pages15Rules#MIN_ZONE_STEP} ticks, a radius of at most 8 blocks, the tier's pulse damage, and
     * at most {@value #MAX_ZONES_PER_PLAYER} kit zones per player at once.
     */
    public static BookPage.Cast field(Tier t, double radius, int ticks, int interval, boolean atAim, VfxShape shape, ElementBook.Rider rider) {
        int tk = Math.max(Pages15Rules.MIN_ZONE_STEP, Math.min(ticks, Pages15Rules.MAX_ZONE_TICKS)), step = Math.max(Pages15Rules.MIN_ZONE_STEP, interval);
        BookPage.Cast inner = ElementBook.field(t.pulse, Math.min(radius, 8.0), tk, step, atAim, shape, rider);
        return (b, i, p, mode) -> {
            if (!zoneSlot(p, tk)) { GrimoireBook.fail(p, "That magic is already holding two fields."); return false; }
            return inner.cast(b, i, p, mode);
        };
    }

    /** An instant line / beam ({@code length} up to 24 blocks). */
    public static BookPage.Cast line(Tier t, double length, double width, VfxShape shape, ElementBook.Rider rider) {
        return ElementBook.line(t.hit, Math.min(length, 24.0), Math.min(width, 3.0), shape, rider);
    }

    /** Dash forward (or behind the target) hitting everything on the way; {@code dist} up to 12 blocks. The tier's damage unless {@code hits} is false (a pure move). */
    public static BookPage.Cast dash(Tier t, boolean hits, double dist, boolean behind, VfxShape shape, ElementBook.Rider rider) {
        return ElementBook.dash(hits ? t.hit : 0f, Math.min(dist, 12.0), behind, shape, rider);
    }

    /** Hard control of one target (the shared 12 s control lock applies). */
    public static BookPage.Cast bind(Tier t, double range, boolean freeze, VfxShape shape, ElementBook.Rider rider) {
        return ElementBook.bind(t.hit * 0.5f, Math.min(range, 24.0), freeze, shape, rider);
    }

    /** Self power-up: your melee hits deal +{@code bonus} (at most half a hit of the tier) for {@code ticks} (at most 400). */
    @SafeVarargs
    public static BookPage.Cast empower(Tier t, int ticks, boolean thorns, VfxShape shape, Supplier<MobEffectInstance>... effects) {
        return ElementBook.empower(Math.max(20, Math.min(ticks, 400)), t.hit * 0.5f, thorns, shape, effects);
    }

    /** One of Tensura's own projectiles (its model, impact and kill credit) with the tier's damage. */
    public static BookPage.Cast shot(Tier t, TensuraShots.Shot shot, float speed, float knock, int burnTicks) {
        return TensuraShots.shot(shot, t.hit, Math.min(speed, 2.5f), Math.min(knock, 1.5f), Math.min(burnTicks, 160));
    }

    /** Heals the caster's allies in {@code radius} (up to 10 blocks) by a fraction of their health (at most 25 %). */
    public static BookPage.Cast healAllies(double radius, float fraction) {
        return ElementBook.healAllies(Math.min(radius, 10.0), Math.min(fraction, 0.25f));
    }

    // ------------------------------------------------------------------ custom casts
    /** What a custom spell is handed. All the methods are bounded (see the class comment). */
    public static final class Ctx {
        public final GrimoireBook book;
        public final ManasSkillInstance inst;
        public final ServerPlayer caster;
        public final int mode;
        public final Tier tier;
        private int packets;

        Ctx(GrimoireBook book, ManasSkillInstance inst, ServerPlayer caster, int mode, Tier tier) {
            this.book = book;
            this.inst = inst;
            this.caster = caster;
            this.mode = mode;
            this.tier = tier;
        }

        /** The size factor of the book's spells (Spirit Dive makes them bigger). */
        public float size() { return GrimoireBook.size(inst, caster); }

        /** The point under the crosshair, at most {@code range} (up to 32) blocks away. */
        public Vec3 aim(double range) { return GrimoireBook.aim(caster, Math.min(range, 32.0)); }

        /** The living thing under the crosshair within {@code range} (up to 32), or null. */
        public LivingEntity target(double range) { return GrimoireBook.target(caster, Math.min(range, 32.0)); }

        /** The caster's foes in a sphere ({@code radius} up to 12), at most the tier's number of targets. Allies, the caster and spectators are never in it. */
        public List<LivingEntity> foes(Vec3 centre, double radius) { return cap(GrimoireBook.around(caster, centre, Math.min(radius, 12.0))); }

        /** The caster's foes within {@code width} (up to 4) of a segment (at most 32 long), at most the tier's number of targets. */
        public List<LivingEntity> foesAlong(Vec3 a, Vec3 b, double width) {
            Vec3 end = b.subtract(a).lengthSqr() > 32.0 * 32.0 ? a.add(b.subtract(a).normalize().scale(32.0)) : b;
            return cap(GrimoireBook.along(caster, a, end, Math.min(width, 4.0)));
        }

        private List<LivingEntity> cap(List<LivingEntity> l) { return l.size() <= tier.maxTargets ? l : new ArrayList<>(l.subList(0, tier.maxTargets)); }

        /** Damage through the book's balance law (allies and the caster are skipped by the book). {@code raw} is clamped to 1.5 hits of the tier. */
        public void hit(LivingEntity target, float raw) {
            if (target == null || caster.isRemoved() || !caster.isAlive()) return;
            book.hurt(inst, caster, target, mode, Math.max(0f, Math.min(raw, tier.maxHit())));
        }

        /** One standard hit of the tier. */
        public void hit(LivingEntity target) { hit(target, tier.hit); }

        /** The chant circle at the caster's feet. */
        public void circle(float power) { book.castCircle(caster, Math.max(0.2f, Math.min(power, 2f))); }

        /** Sends one effect (the book's own shape) to the players near the caster. At most {@value Pages15Rules#MAX_PACKETS} per cast: returns false when the budget is spent. */
        public boolean fx(VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
            if (packets >= Pages15Rules.MAX_PACKETS) return false;
            packets++;
            book.vfx(caster, shape, from, to, Math.max(4, Math.min(ticks, 120)), Math.max(0.2f, Math.min(power, 4f)));
            return true;
        }

        /** The impact flash at a point (counts as a packet). */
        public boolean impact(Vec3 at, float power) {
            if (packets >= Pages15Rules.MAX_PACKETS) return false;
            packets++;
            book.impact(caster, at, Math.max(0.2f, Math.min(power, 2f)));
            return true;
        }

        /**
         * A standing effect: {@code step(age)} runs every {@code interval} ticks (at least {@value Pages15Rules#MIN_ZONE_STEP}) for {@code ticks} ticks (at most {@value Pages15Rules#MAX_ZONE_TICKS}). Returns false
         * (and does nothing) when the caster already has {@value #MAX_ZONES_PER_PLAYER} kit zones running.
         */
        public boolean zone(int ticks, int interval, IntConsumer step) {
            int tk = Math.max(Pages15Rules.MIN_ZONE_STEP, Math.min(ticks, Pages15Rules.MAX_ZONE_TICKS));
            if (!zoneSlot(caster, tk)) { GrimoireBook.fail(caster, "That magic is already holding two fields."); return false; }
            SpellRuntime.zone(caster.serverLevel(), tk, Math.max(Pages15Rules.MIN_ZONE_STEP, interval), age -> { if (!caster.isRemoved() && caster.isAlive()) step.accept(age); });
            return true;
        }

        /** Runs {@code run} after {@code delayTicks} (1..100). The task does nothing if the caster has died or left by then. */
        public void later(int delayTicks, Runnable run) {
            SpellRuntime.later(caster.serverLevel(), Math.max(1, Math.min(delayTicks, 100)), () -> { if (!caster.isRemoved() && caster.isAlive()) run.run(); });
        }

        /** Tells the caster why nothing happened; returns false so a cast can {@code return c.fail("...")}. */
        public boolean fail(String message) {
            GrimoireBook.fail(caster, message);
            return false;
        }
    }

    /** A custom cast: runs {@code body} with a {@link Ctx}; return false when nothing happened (no cooldown, no cost). */
    public static BookPage.Cast cast(Tier t, Predicate<Ctx> body) {
        return (b, i, p, mode) -> body.test(new Ctx(b, i, p, mode, t));
    }

    // ------------------------------------------------------------------ the per-player zone cap
    private static final Map<UUID, long[]> ZONE_UNTIL = new HashMap<>();

    /** Takes one of the player's {@value #MAX_ZONES_PER_PLAYER} zone slots for {@code ticks}; false when both are busy. A slot frees itself by time (no cleanup task, nothing to leak but 16 bytes per player). */
    static boolean zoneSlot(ServerPlayer p, int ticks) {
        long now = p.level().getGameTime();
        long[] u = ZONE_UNTIL.computeIfAbsent(p.getUUID(), k -> new long[MAX_ZONES_PER_PLAYER]);
        for (int k = 0; k < u.length; k++) {
            if (u[k] <= now) {
                u[k] = now + ticks + 2;
                return true;
            }
        }
        return false;
    }

    /** Forgets a player's zone slots (logout). */
    public static void forget(UUID player) { ZONE_UNTIL.remove(player); }
}
