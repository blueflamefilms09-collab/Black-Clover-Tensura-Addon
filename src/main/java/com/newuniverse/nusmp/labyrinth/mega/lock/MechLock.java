package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Instance;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsDirector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Scope;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the adapter that lets the escape engine treat a sealing MECHANIC (one of the 214 that bar the doorways or cage the party) like any other lock. It wraps the live
 * {@link Instance}, keeps the two clocks the rules need that the instance does not keep for itself (when a player was last inside, when a hostile was last alive), reads the
 * facts from the instance (who is inside, which monsters of the fight are alive, whether a boss is alive in the arena, whether the counter can be met by the party present),
 * and opens the lock the way the Director opens any mechanic that is over: the instance is ENDED, which restores every block from its journals and removes its monsters and props,
 * and is marked as done so it cannot re-arm and shut the next party in (it pays no reward). A BOSS lock opened with the exit rune first sends the prisoners back to where each
 * came in and heals the boss, so the fight starts fresh. Server thread only.
 */
final class MechLock implements Lockable {
    private final Instance in;
    private final long since;
    private long lastPlayer, lastHostile;

    MechLock(Instance in, long now) {
        this.in = in;
        this.since = now;
        this.lastPlayer = now;
        this.lastHostile = now;
    }

    Instance instance() { return in; }

    @Override public String lockKey() { return "mech:" + in.key; }

    @Override public EscapeRules.Kind lockKind() { return in.def.scope() == Scope.BOSS ? EscapeRules.Kind.BOSS : EscapeRules.Kind.SEAL; }

    @Override
    public String lockName() {
        Language lang = Language.getInstance();
        return lang.has(in.def.nameKey()) ? lang.getOrDefault(in.def.nameKey()) : in.def.name();
    }

    @Override public ServerLevel lockLevel() { return in.level; }

    @Override public boolean soft() { return !in.sealsPlayers(); }

    @Override
    public Vec3 runePos() {
        List<ServerPlayer> inside = prisoners();
        BlockPos base = in.room != null ? in.room.center() : in.anchor;
        if (!inside.isEmpty()) {
            ServerPlayer p = inside.get(0);
            Vec3 pp = p.position();
            Vec3 toCentre = new Vec3(base.getX() + 0.5 - pp.x, 0.0, base.getZ() + 0.5 - pp.z);
            Vec3 step = toCentre.lengthSqr() < 1.0e-4 ? Vec3.ZERO : toCentre.normalize().scale(Math.min(2.5, toCentre.length()));
            Vec3 want = new Vec3(pp.x + step.x, pp.y, pp.z + step.z);
            if (free(want)) return want;
            return pp;
        }
        return new Vec3(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
    }

    private boolean free(Vec3 v) {
        BlockPos p = BlockPos.containing(v);
        return in.level.getBlockState(p).isAir() && in.level.getBlockState(p.above()).isAir();
    }

    @Override
    public List<ServerPlayer> prisoners() {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : in.inside) if (p.isAlive() && !p.isSpectator()) out.add(p);
        return out;
    }

    @Override
    public EscapeRules.Facts facts(long now) {
        int players = prisoners().size();
        if (players > 0) lastPlayer = now;
        int hostiles = 0;
        for (Mob m : in.mobsInArea()) if (m.isAlive()) hostiles++;
        if (hostiles > 0) lastHostile = now;
        boolean boss = lockKind() == EscapeRules.Kind.BOSS && bossAlive();
        boolean solvable = EscapeRules.solvable(in.def.counter().kind(), in.playersNeeded(), players);
        return new EscapeRules.Facts(lockKind(), now - since, players, now - lastPlayer, hostiles, now - lastHostile, boss, solvable);
    }

    private boolean bossAlive() {
        for (Mob m : in.level.getEntitiesOfClass(Mob.class, in.area.aabb(), x -> x.isAlive() && BossIdentity.isBoss(x))) {
            if (m.isAlive()) return true;
        }
        return false;
    }

    @Override
    public void open(ServerPlayer by, EscapeRules.Why why) {
        if (lockKind() == EscapeRules.Kind.BOSS && by != null) exitAndReset();
        MechanicsDirector.retire(in);
    }

    /** The exit rune of a boss arena: everyone inside goes back to where they came in, and the boss is healed and forgets them, so the fight starts again. */
    private void exitAndReset() {
        for (ServerPlayer p : prisoners()) {
            BlockPos at = in.entryOf(p.getUUID());
            if (at == null) at = in.room != null ? in.room.center() : in.anchor;
            UnstuckService.teleportNear(p, at);
        }
        for (Mob m : in.level.getEntitiesOfClass(Mob.class, in.area.aabb(), x -> x.isAlive() && BossIdentity.isBoss(x))) {
            m.setTarget(null);
            m.setHealth(m.getMaxHealth());
            if (m instanceof LivingEntity le) le.removeAllEffects();
        }
    }
}
