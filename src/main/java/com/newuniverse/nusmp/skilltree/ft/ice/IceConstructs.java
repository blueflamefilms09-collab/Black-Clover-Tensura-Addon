package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.skilltree.ft.kit.FtWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the constructs of Ice-Make (a floor, a wall, a cannon) are not entities and not blocks: the owner's rules allow few spawned things, none that live longer than ten seconds, and no
 * block changes at all, so a "construct" here is an EFFECT (a drawing the clients show) plus a short-lived rule on the server (a circle that chills, a box that eats projectiles, a point that
 * fires). This class holds the three pieces of server glue those rules share. THE LEDGER: how many of a caster's constructs still stand is kept as a small array of expiry ticks in the
 * player's own saved data, so there is no static state to clear when a server stops or a player leaves, and the arithmetic is the pure {@link IceNumbers#record}; the limit (four) is the
 * owner's limit for spawned things, applied to constructs. THE CHILL: a slowness plus a few frozen ticks, the game's own cold, added without ever passing what freezes a body, and skipped
 * for creatures the game says cannot freeze (snow golems, strays), because a chill that always fails would look like a bug. THE ERASER: the idea of the shared weapon-art eraser (a wall of
 * force that deletes projectiles) rewritten for the tree, as the owner's isolation rule requires, with one addition that matters in a party fight: a projectile fired by the caster or by
 * anyone in their party passes through, so the wall protects the group from outside and never blocks its own archers. Server thread only.
 */
public final class IceConstructs {
    private IceConstructs() {}

    /** The key under the player's persistent data where the expiry ticks of their standing constructs are kept. */
    static final String LEDGER = "nusmp_ft_ice_live";

    /** Is there room for one more of the caster's constructs? */
    public static boolean hasRoom(ServerPlayer p) { return IceNumbers.hasRoom(p.getPersistentData().getLongArray(LEDGER), p.level().getGameTime()); }

    /** Records a construct that will stand for {@code life} ticks (call after {@link #hasRoom} said yes and the cost was paid). */
    public static void record(ServerPlayer p, int life) {
        p.getPersistentData().putLongArray(LEDGER, IceNumbers.record(p.getPersistentData().getLongArray(LEDGER), p.level().getGameTime(), life));
    }

    /** How many of the caster's constructs stand right now (for the self-test). */
    public static int standing(ServerPlayer p) { return IceNumbers.liveCount(p.getPersistentData().getLongArray(LEDGER), p.level().getGameTime()); }

    /** Forgets the caster's constructs (self-test clean-up). */
    public static void forget(ServerPlayer p) { p.getPersistentData().remove(LEDGER); }

    /** Chills a creature: Slowness for {@code slowTicks} at amplifier {@code slowAmp}, and {@code freezeAdd} frozen ticks if its body can freeze. Hard control it is not: nothing here roots or stuns. */
    public static void chill(LivingEntity e, int slowTicks, int slowAmp, int freezeAdd) {
        if (slowTicks > 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, Math.max(0, slowAmp)));
        if (freezeAdd > 0 && e.canFreeze()) e.setTicksFrozen(IceNumbers.chilled(e.getTicksFrozen(), freezeAdd, e.getTicksRequiredToFreeze()));
    }

    /** The players of the caster's party (and the caster) within {@code radius} of {@code c}: the ones a floor of ice should carry. */
    public static List<ServerPlayer> friendsAround(ServerPlayer caster, Vec3 c, double radius) {
        Set<UUID> friends = FtWorld.friendIds(caster);
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer m : caster.serverLevel().players()) {
            if (friends.contains(m.getUUID()) && m.isAlive() && !m.isSpectator() && m.distanceToSqr(c) <= radius * radius) out.add(m);
        }
        return out;
    }

    /**
     * Deletes every projectile that is inside {@code wall} or about to cross it on its next move (a fast arrow flies several blocks a tick, so only looking at where it IS would let it
     * through) and that was not fired by the caster's party (or by nobody: a dispenser's arrow counts as foreign); a small shatter is drawn where each one struck, at most three per call so a
     * volley cannot flood the network. Returns how many projectiles it deleted.
     */
    public static int erase(ServerLevel level, Set<UUID> friends, IceNumbers.Wall wall) {
        double r = wall.reach() + 4.0;
        AABB box = new AABB(wall.cx() - r, wall.cy() - 4.0, wall.cz() - r, wall.cx() + r, wall.cy() + wall.height() + 4.0, wall.cz() + r);
        int erased = 0;
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box)) {
            Vec3 from = pr.position(), to = from.add(pr.getDeltaMovement());
            if (!wall.crossedBy(from.x, from.y, from.z, to.x, to.y, to.z)) continue;
            Entity owner = pr.getOwner();
            if (owner != null && friends.contains(owner.getUUID())) continue;
            if (erased < 3) VfxSpawn.send(level, VfxShape.ICE_FX3, from, from, IceRoute.ICE, 12, 0.4f);
            pr.discard();
            erased++;
        }
        return erased;
    }
}
