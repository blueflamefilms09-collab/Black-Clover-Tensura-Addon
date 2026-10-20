package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.song.SongTargets;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the 1.93.0 rebuild of Song, Sound, Soul, Soul Corpse and Memory Magic gives each magic summoned things (note bolts, speaker stacks, soul wisps, marionettes,
 * memory echoes ...) and a temporary armour after every cast. The five books need the same plumbing, so it lives here once: which book / skill / page a living prop
 * belongs to (so the prop can hurt things in its caster's name through the book's balance-law {@code hurt}), a projectile that is a server hitbox AND a flying prop
 * model, who counts as a foe or an ally (the song package's rule: the singer, their team, other players without a team and tamed animals are allies; hostile
 * monsters and anything already hunting an ally are foes), the 5 second armour that every cast of these magics puts on the caster (Aura.SONG ... Aura.MEMORY, drawn
 * by client.aura.SongArmourKit), and the shared "fly" behaviour of a projectile prop. Server only; nothing here touches a client class.
 */
public final class SongRebuildKit {
    private SongRebuildKit() {}

    /** Ticks of the armour a plain cast puts on (5 seconds); the guard pages of each book use their own, longer time. */
    public static final int ARMOUR_TICKS = 100;
    /** At most this many creatures are touched by one pulse of a field or a summon. */
    public static final int MAX_TARGETS = 16;

    // ================================================================ which page a prop belongs to
    /** What a living prop needs to hurt things in its caster's name. */
    public record Ctx(GrimoireBook book, ManasSkillInstance inst, int mode, long until) {}

    private static final Map<UUID, Ctx> CTX = new HashMap<>();

    /** Remembers the page a prop was cast from (right after spawning it); entries of props that never ended are dropped when they are stale. */
    public static void bind(MagicPropEntity e, GrimoireBook b, ManasSkillInstance i, int mode, long until) {
        long now = e.level().getGameTime();
        CTX.values().removeIf(c -> c.until() < now);
        CTX.put(e.getUUID(), new Ctx(b, i, mode, until + 40));
    }

    public static Ctx ctx(MagicPropEntity e) { return CTX.get(e.getUUID()); }

    public static boolean bound(MagicPropEntity e) { return CTX.containsKey(e.getUUID()); }

    public static void unbind(MagicPropEntity e) { CTX.remove(e.getUUID()); }

    /** Spawns a prop for a cast and binds it to the page. Null when the world refused. */
    public static MagicPropEntity spawn(ServerPlayer p, PropKind kind, Vec3 at, float yaw, float scale, int life, int param,
                                        GrimoireBook b, ManasSkillInstance i, int mode) {
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), kind, at, yaw, scale, life, param, p);
        if (e != null) bind(e, b, i, mode, p.level().getGameTime() + life);
        return e;
    }

    /** The props of this kind that belong to the caster (living ones). */
    public static List<MagicPropEntity> mine(ServerPlayer p, PropKind kind) {
        List<MagicPropEntity> out = new ArrayList<>();
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, p.getBoundingBox().inflate(64),
                x -> x.kind() == kind && p.getUUID().equals(x.ownerId()) && x.isAlive())) out.add(e);
        return out;
    }

    /** Ends the caster's props of this kind (and param, when param >= 0) so that a new cast never piles up more than the cap. */
    public static void dismiss(ServerPlayer p, PropKind kind, int param, int keep) {
        List<MagicPropEntity> l = new ArrayList<>();
        for (MagicPropEntity e : mine(p, kind)) if (param < 0 || (e.param() & 0xFF) == param) l.add(e);
        l.sort((a, c) -> Integer.compare(c.life(), a.life()));                 // oldest first
        for (int k = 0; k < l.size() - keep; k++) l.get(k).expire();
    }

    // ================================================================ who counts
    public static List<LivingEntity> foes(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> all = SongTargets.foes(p, c, r);
        return all.size() > MAX_TARGETS * 2 ? new ArrayList<>(all.subList(0, MAX_TARGETS * 2)) : all;
    }

    public static List<LivingEntity> allies(ServerPlayer p, Vec3 c, double r) { return SongTargets.allies(p, c, r); }

    public static LivingEntity nearestFoe(ServerPlayer p, Vec3 c, double r) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : SongTargets.foes(p, c, r)) {
            double d = e.distanceToSqr(c);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    /** Ticks of a summon, longer for a mighty caster (up to +50 percent). */
    public static int span(int base, ServerPlayer p) { return (int) (base * Math.min(1.5, 1 + 0.04 * (BalanceLaw.epScale(p) - 1))); }

    /** The visual and hitbox scale of a summon: 1.0 for a novice up to 1.4 for the mightiest casters. */
    public static float size(ServerPlayer p) { return 1f + 0.05f * (float) Math.min(8.0, BalanceLaw.epScale(p) - 1.0); }

    /** Bosses take half of what the page would deal. */
    public static float safe(LivingEntity t, float raw) { return BalanceLaw.isBoss(t) ? raw * 0.5f : raw; }

    // ================================================================ the armour after a cast (1.99.0: there is none)
    /**
     * 1.99.0 (armour policy, {@link ArmourPolicy}): does nothing. Song, Sound, Soul, Soul Corpse and Memory Magic have no armour in the anime; the render layers (Aura.SONG ... Aura.MEMORY,
     * client.aura.SongArmourKit) stay registered and on disk, but no cast puts them on any more.
     */
    public static void armour(ServerPlayer p, Aura a, int ticks) { }

    /** 1.99.0: the cast as it is (it used to put the 5 second armour on the caster whenever it succeeded). */
    public static BookPage.Cast armoured(Aura a, BookPage.Cast c) { return c; }

    // ================================================================ projectiles: a server hitbox that is also a flying prop model
    /**
     * Fires a projectile: a SpellRuntime bolt (the hitbox) and a prop of {@code kind} (the model) that fly together; the prop ends when the bolt hits or runs out.
     * The prop's velocity is stored in its motion; {@link #fly} (the behaviour every projectile kind registers) moves it each tick.
     */
    public static void projectile(ServerPlayer p, PropKind kind, int param, Vec3 start, Vec3 dir, double speed, double radius, int life, boolean pierce,
                                  SpellRuntime.OnHit hit, SpellRuntime.OnEnd end, float scale) {
        Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(d.y, -1, 1)));
        MagicPropEntity prop = MagicProps.spawn(p.serverLevel(), kind, start, yaw, scale, life + 4, param, p);
        if (prop != null) {
            prop.setXRot(pitch);
            prop.setDeltaMovement(d.scale(speed));
        }
        SpellRuntime.bolt(p, start, d.scale(speed), radius, life, pierce, null, hit, (bolt, at) -> {
            if (prop != null) prop.expire();
            if (end != null) end.end(bolt, at);
        });
    }

    /** The shared behaviour of a projectile prop: it moves by its motion every tick (the hitbox is the bolt). */
    public static final MagicProps.Behavior FLY = new MagicProps.Behavior() {
        @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.5f * e.scale(), 0.5f * e.scale()); }
        @Override public void tick(MagicPropEntity e, ServerLevel sl) {
            Vec3 v = e.getDeltaMovement();
            e.setPos(e.getX() + v.x, e.getY() + v.y, e.getZ() + v.z);
        }
    };

    // ================================================================ small helpers
    public static float yawOf(double dx, double dz) { return (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI)); }

    public static List<LivingEntity> living(ServerLevel level, Vec3 c, double r) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(c) <= r * r);
    }
}
