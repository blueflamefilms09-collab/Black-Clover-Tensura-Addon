package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Why: the Fan, the Scythe, the Katana and the Book all need the same dozen small game actions, and the owner's rules for them are strict (never act on a
 * creative or spectator player, an ally, the wielder's own pets or summons, or a passive animal by accident; never keep a task alive after its wielder is gone;
 * never throw out of a technique). Writing them four times would let the four drift apart, so they are written once here: who counts as a foe, the foes in a
 * ring, a cone or along a line (nearest first, never more than asked), the foe under the crosshair, how far a ray gets before a wall, a push on any entity, a
 * delayed task and a counted series of pulses that stop when the wielder is dead or gone, and the few effect calls the weapons share. Everything goes through
 * {@link HeroCtx} (damage, heals, control, costs) or the mod's own effect system; nothing here changes a block or spawns an entity.
 */
public final class GlassKit {
    private GlassKit() {}

    /** The farthest any scan of this kit reaches from its centre, whatever a caller asks for. */
    public static final double MAX_SCAN = 24.0;

    // ------------------------------------------------------------------ who counts
    /** The wielder is on the server, alive and still in the world. */
    public static boolean alive(ServerPlayer p) { return p != null && p.isAlive() && !p.isRemoved(); }

    /**
     * A foe for an area technique: a hostile mob, a mob that hunts the wielder, or a player the wielder may harm (never creative or spectator, never an ally, a
     * pet of the wielder or an armour stand). Passive animals and villagers are not hit by accident.
     */
    public static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == null || e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** A target the wielder pointed at on purpose: any living thing that is not an ally, a pet of theirs, an armour stand or a creative or spectator player. */
    public static boolean isTarget(ServerPlayer p, LivingEntity e) {
        if (e == null || e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        return !(e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()));
    }

    // ------------------------------------------------------------------ scans
    /** Foes within {@code radius} of {@code centre}, nearest first, at most {@code max}. */
    public static List<LivingEntity> foes(HeroCtx c, Vec3 centre, double radius, int max) {
        ServerPlayer p = c.player;
        double r = Mth.clamp(radius, 0.5, MAX_SCAN);
        List<LivingEntity> found = c.level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(r),
                e -> isFoe(p, e) && e.distanceToSqr(centre) <= r * r);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(centre)));
        return limit(found, max);
    }

    /** Foes in front of the wielder: within {@code range} and within {@code halfAngleDeg} of where they look, nearest first, at most {@code max}. */
    public static List<LivingEntity> foesInCone(HeroCtx c, double range, double halfAngleDeg, int max) {
        Vec3 eye = c.eye(), look = c.look();
        double cos = Geo.cosHalf(halfAngleDeg);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : foes(c, c.player.position(), range + 1.0, 64)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() - e.getBbWidth() * 0.5 > range) continue;
            if (Geo.inCone(to.x, to.y, to.z, look.x, look.y, look.z, cos)) out.add(e);
        }
        return limit(out, max);
    }

    /** Foes within {@code width} of the segment from a to b, in the order along it, at most {@code max}. */
    public static List<LivingEntity> foesAlong(HeroCtx c, Vec3 a, Vec3 b, double width, int max) {
        ServerPlayer p = c.player;
        Vec3 ab = b.subtract(a);
        double len2 = Math.max(1e-6, ab.lengthSqr());
        List<Object[]> hits = new ArrayList<>();
        for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(width + 1.5), x -> isFoe(p, x))) {
            Vec3 centre = e.getBoundingBox().getCenter();
            double t = Mth.clamp(centre.subtract(a).dot(ab) / len2, 0.0, 1.0);
            if (a.add(ab.scale(t)).distanceTo(centre) <= width + e.getBbWidth() * 0.5) hits.add(new Object[] {t, e});
        }
        hits.sort(Comparator.comparingDouble(h -> (Double) h[0]));
        List<LivingEntity> out = new ArrayList<>();
        for (Object[] h : hits) {
            if (out.size() >= max) break;
            out.add((LivingEntity) h[1]);
        }
        return out;
    }

    /** The living thing under the crosshair within {@code range} (a wall in between hides it), or null; only a valid target. */
    public static LivingEntity focus(HeroCtx c, double range) {
        LivingEntity t = GrimoireBook.target(c.player, range);
        return t != null && isTarget(c.player, t) ? t : null;
    }

    /**
     * The wielder and the friends within {@code radius} of them, nearest first (the wielder first of all), at most {@code max}: allies by the game's own rule
     * ({@code isAlliedTo}: a team mate, a pet of the wielder), alive, never a spectator or an armour stand.
     */
    public static List<LivingEntity> friends(HeroCtx c, double radius, int max) {
        ServerPlayer p = c.player;
        Vec3 at = p.position();
        double r = Mth.clamp(radius, 0.5, MAX_SCAN);
        List<LivingEntity> near = c.level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && e.isAlliedTo(p) && e.distanceToSqr(at) <= r * r);
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        List<LivingEntity> out = new ArrayList<>();
        out.add(p);
        for (LivingEntity e : near) {
            if (out.size() >= max) break;
            out.add(e);
        }
        return out;
    }

    private static List<LivingEntity> limit(List<LivingEntity> l, int max) {
        return l.size() > max ? new ArrayList<>(l.subList(0, Math.max(0, max))) : l;
    }

    // ------------------------------------------------------------------ rays and moves
    /** Where a ray from {@code from} toward {@code to} ends: the first solid block it meets, else {@code to}. */
    public static Vec3 clipEnd(HeroCtx c, Vec3 from, Vec3 to) {
        BlockHitResult wall = c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player));
        return wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
    }

    /** How far a ray of at most {@code max} blocks from {@code from} along {@code dir} (a unit vector) gets before a wall; {@code max} when it meets none. */
    public static double reach(HeroCtx c, Vec3 from, Vec3 dir, double max) {
        return clipEnd(c, from, from.add(dir.scale(max))).distanceTo(from);
    }

    /** The point straight below {@code p} where the ground is, looking down at most {@code depth} blocks; {@code p} itself when there is none (a tell on the floor, not in the air). */
    public static Vec3 ground(HeroCtx c, Vec3 p, double depth) {
        BlockHitResult floor = c.level.clip(new ClipContext(p, p.subtract(0.0, depth, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player));
        return floor.getType() == HitResult.Type.MISS ? p : floor.getLocation().add(0.0, 0.05, 0.0);
    }

    /** Sets (or adds to) an entity's velocity and tells the clients; the fall in progress is forgotten so a dash does not end in damage. */
    public static void impulse(LivingEntity e, Vec3 v, boolean add) {
        e.setDeltaMovement(add ? e.getDeltaMovement().add(v) : v);
        e.hurtMarked = true;
        e.resetFallDistance();
    }

    /**
     * Draws a foe toward {@code point} with {@link Geo#pull}; a small lift frees it from the ground's friction. A heavy foe is drawn less: the pull is scaled by
     * what is left of its knockback resistance, as a push is by the game's own knockback.
     */
    public static void drawToward(LivingEntity e, Vec3 point, double gain, double maxSpeed) {
        Vec3 d = point.subtract(e.getBoundingBox().getCenter());
        Geo.Impulse v = Geo.pull(d.x, d.y, d.z, gain, maxSpeed);
        double open = 1.0 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        impulse(e, new Vec3(v.x() * open, (v.y() + (e.onGround() ? 0.12 : 0.0)) * open, v.z() * open), true);
    }

    /**
     * Pushes a foe away from {@code from} (the core's shove, which a foe's knockback resistance reduces) and marks it so the clients are told: a player is moved on
     * their own screen only when the server sends the new velocity, and a bare shove does not.
     */
    public static void push(HeroCtx c, LivingEntity e, Vec3 from, double strength) {
        c.shove(e, from, strength);
        e.hurtMarked = true;
    }

    /** The wielder's chest, where most effects start. */
    public static Vec3 chest(ServerPlayer p) { return p.position().add(0.0, 1.0, 0.0); }

    // ------------------------------------------------------------------ time
    /** Runs {@code task} after {@code delay} ticks, unless the wielder died, left or changed dimension by then. */
    public static void later(HeroCtx c, int delay, Runnable task) {
        ServerPlayer p = c.player;
        SpellRuntime.later(c.level, Math.max(1, delay), () -> {
            if (alive(p) && p.level() == c.level) task.run();
        });
    }

    /**
     * Runs {@code step(index)} exactly {@code count} times, {@code interval} ticks apart, the first one at once (in the tick of the cast, or the next); the series ends early when the wielder
     * dies or leaves. The longest allowed series is 20 seconds. (The spell runtime's zone may run one step more or fewer by when it was made; a counter here makes
     * the number of pulses exact.)
     */
    public static void pulses(HeroCtx c, int count, int interval, IntConsumer step) {
        ServerPlayer p = c.player;
        int n = Math.max(1, count), every = Math.max(1, interval);
        int total = Math.min(Budget.MAX_DURATION_TICKS, (n - 1) * every + 2);
        int[] fired = {0};
        SpellRuntime.zone(c.level, total, every, age -> {
            if (fired[0] >= n || !alive(p) || p.level() != c.level) return;
            step.accept(fired[0]++);
        });
    }

    // ------------------------------------------------------------------ show
    /** A flat ring of dust on the ground (a tell); {@code rgb} is 0xRRGGBB. */
    public static void ring(HeroCtx c, Vec3 centre, double radius, int rgb) { Tells.ring(c.level, centre, radius, rgb); }

    /** A line of dust (an aim line); {@code rgb} is 0xRRGGBB. */
    public static void line(HeroCtx c, Vec3 from, Vec3 to, int rgb) { Tells.line(c.level, from, to, rgb); }

    /** An effect that follows the wielder for {@code ticks}. */
    public static void follow(HeroCtx c, VfxShape shape, int argb, int ticks, float size) {
        VfxSpawn.sendFollowing(c.level, shape, c.player, c.player.position().add(0.0, 1.0, 0.0), argb, ticks, size);
    }

    /** The wielder swings the weapon arm (the animation the others see). */
    public static void swing(HeroCtx c) { c.player.swing(InteractionHand.MAIN_HAND, true); }

    /** Takes the shared hard-control lock or says how long it still holds; false when refused. Call it only once the technique will really happen. */
    public static boolean control(HeroCtx c) {
        if (c.control()) return true;
        int left = BalanceLaw.controlCooldownLeft(c.player);
        c.say("Control magic needs " + Fmt.n((left + 19) / 20) + "s.");
        return false;
    }

    private static boolean striking;

    /**
     * True while one of the weapon's own techniques is dealing damage. The damage event of such a blow looks like any melee blow of the wielder, and a logic's
     * {@code onHit} (which adds a rider to a real melee hit) must not answer its own weapon's techniques, or a rider's damage would call the rider again; each
     * logic's {@code onHit} returns at once while this is true.
     */
    public static boolean striking() { return striking; }

    /** Raw damage on a foe through the balance law, landing in full; false when it was not hit. A thin name so the skills read as sentences. */
    public static boolean strike(HeroCtx c, LivingEntity e, double raw) {
        return strike(c, e, raw, false);
    }

    /**
     * Raw damage on a foe through the balance law; false when it was not hit. A foe hurt a moment ago shrugs off a blow that is not larger than the last one (vanilla's
     * damage immunity), so the immunity is cut to ten ticks first and the blow lands in full ({@link Immunity}); {@code rapid} marks a hit of a quick series on one foe
     * (less than ten ticks apart), which keeps the vanilla immunity on a PLAYER so that a flurry cannot stack past the balance law's cap on players.
     */
    public static boolean strike(HeroCtx c, LivingEntity e, double raw, boolean rapid) {
        e.invulnerableTime = Immunity.before(e.invulnerableTime, rapid, e instanceof Player);
        boolean before = striking;
        striking = true;
        try {
            return c.hit(e, (float) raw);
        } finally {
            striking = before;
        }
    }
}
