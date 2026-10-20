package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: Vanessa's thread traps must act like real tripwires without touching the world: no blocks to place (worldgen and grief
 * rules stay untouched, nothing to clean up if the server stops), but something a foe can actually walk into. One node is a small
 * entity box at knee height with a faint star of red filaments (drawn by {@code client.thread.ThreadTrapRenderer}); a cast lays several
 * in a line or a ring. A foe touching a node is slowed and weakened (the balance law clamps the time) and the caster is told where;
 * a projectile touching one is caught (arrows and thrown things vanish in a puff of thread) or turned back (fireballs and the like).
 * Nodes are never saved, last {@code thread.trapSeconds}, vanish with their caster, and the caster can have at most
 * {@code thread.trapMax} out (the oldest go first).
 */
public class ThreadTrapEntity extends Entity {
    private static final Map<UUID, ArrayDeque<ThreadTrapEntity>> OUT = new HashMap<>();
    private static final String TRIPPED = "nusmp_trap_until";

    private UUID owner;
    private int life = 1800;
    private int charges = 2;

    public ThreadTrapEntity(EntityType<? extends ThreadTrapEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isAttackable() { return false; }

    public void setOwner(ServerPlayer p, int lifeTicks) {
        this.owner = p.getUUID();
        this.life = lifeTicks;
    }

    // ================================================================ bookkeeping per caster
    /** How many traps this caster has out right now (nodes that are gone are dropped from the count). */
    public static int countFor(UUID caster) {
        ArrayDeque<ThreadTrapEntity> q = OUT.get(caster);
        if (q == null) return 0;
        q.removeIf(Entity::isRemoved);
        return q.size();
    }

    /** Registers a new node and removes the caster's oldest ones beyond the cap. */
    public static void track(ThreadTrapEntity t, int cap) {
        ArrayDeque<ThreadTrapEntity> q = OUT.computeIfAbsent(t.owner, k -> new ArrayDeque<>());
        q.removeIf(Entity::isRemoved);
        q.addLast(t);
        while (q.size() > Math.max(1, cap)) {
            ThreadTrapEntity old = q.pollFirst();
            if (old != null) old.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (owner != null) {
            ArrayDeque<ThreadTrapEntity> q = OUT.get(owner);
            if (q != null) { q.remove(this); if (q.isEmpty()) OUT.remove(owner); }
        }
        super.remove(reason);
    }

    // ================================================================ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        ServerPlayer o = owner == null ? null : sl.getServer().getPlayerList().getPlayer(owner);
        if (o == null || !o.isAlive() || o.level() != level() || ThreadMath.trapExpired(tickCount, life)) { puff(sl, 4); discard(); return; }
        AABB box = getBoundingBox();
        RougeEntity cat = RougeManager.of(o.getUUID());
        for (Projectile pr : sl.getEntitiesOfClass(Projectile.class, box.inflate(4.0), p -> !p.isRemoved())) {
            Vec3 cur = pr.position(), prev = cur.subtract(pr.getDeltaMovement());
            if (!box.contains(cur) && box.clip(prev, cur).isEmpty()) continue;
            if (pr.getOwner() instanceof LivingEntity po && friend(o, cat, po)) continue;
            if (stop(sl, o, pr) && --charges <= 0) { puff(sl, 8); discard(); return; }
        }
        if (tickCount % 2 == 0) {
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator() && !(e instanceof RougeEntity))) {
                if (friend(o, cat, le)) continue;
                trip(sl, o, le);
            }
        }
    }

    private static boolean friend(ServerPlayer owner, RougeEntity cat, LivingEntity e) {
        return e == owner || e.isAlliedTo(owner) || e instanceof OwnableEntity oe && owner.getUUID().equals(oe.getOwnerUUID()) || cat != null && cat.isProtecting(e);
    }

    /** A foe touched a thread: alert, slow, weaken (once per three seconds per foe). */
    private void trip(ServerLevel sl, ServerPlayer owner, LivingEntity foe) {
        long now = sl.getGameTime();
        if (now < foe.getPersistentData().getLong(TRIPPED)) return;
        foe.getPersistentData().putLong(TRIPPED, now + 60);
        int ticks = BalanceLaw.controlTicks(foe, 60);
        foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        owner.displayClientMessage(Component.literal("A thread trembles: " + foe.getDisplayName().getString() + " at " + (int) foe.getX() + ", " + (int) foe.getY() + ", " + (int) foe.getZ() + ".")
                .withStyle(ChatFormatting.RED), true);
        owner.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.6f);
        com.newuniverse.nusmp.quests.NusmpEvents.fire(owner, "thread.trap");
        puff(sl, 10);
    }

    /** Catches an arrow-like thing (it vanishes) or turns a fireball-like thing round (it now belongs to the caster). True if it was stopped. */
    private boolean stop(ServerLevel sl, ServerPlayer owner, Projectile pr) {
        if (pr instanceof AbstractHurtingProjectile) {
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0));
            pr.setOwner(owner);
            pr.hurtMarked = true;
        } else {
            pr.discard();
        }
        Fx.sprayDust(sl, FxThemes.MAGIC_THREAD, new Vector3f(1f, 0.3f, 0.4f), 0.9f, pr.getX(), pr.getY(), pr.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.8f, 1.8f);
        return true;
    }

    private void puff(ServerLevel sl, int n) {
        Fx.sprayDust(sl, FxThemes.MAGIC_THREAD, new Vector3f(1f, 0.2f, 0.33f), 0.8f, getX(), getY() + 0.4, getZ(), n, 0.5, 0.25, 0.5, 0.01);
    }
}
