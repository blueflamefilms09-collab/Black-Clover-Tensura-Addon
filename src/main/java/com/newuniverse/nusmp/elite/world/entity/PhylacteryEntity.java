package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.elite.world.MobTouchHandler;
import com.newuniverse.nusmp.elite.world.WorldEntities;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: the Lich's soul vessel. While phylacteries stand, the Lich is shielded (a blow does almost nothing to it); the players have to find and break them. A phylactery is a still, floating
 * crystal-and-bone vessel with its own health, linked to its Lich by a stream of Dark Rukh so that the link can be seen; it hits nobody. When the last one breaks the shield shatters and the
 * Lich staggers. A phylactery whose Lich is gone for a few seconds disappears (no orphans). It carries the helper mark, so it drops nothing and gives nothing.
 */
public class PhylacteryEntity extends Monster {
    private UUID bossId;
    private int orphanTicks;

    public PhylacteryEntity(EntityType<? extends PhylacteryEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        xpReward = 0;
        getPersistentData().putBoolean(MobTouchHandler.ADD, true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.ARMOR, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    /** Creates a phylactery for a Lich at a position with the given health. */
    public static PhylacteryEntity spawn(ServerLevel level, Vec3 at, LichBoss owner, double health) {
        PhylacteryEntity p = new PhylacteryEntity(WorldEntities.PHYLACTERY.get(), level);
        p.moveTo(at.x, at.y + 0.6, at.z, 0f, 0f);
        p.bossId = owner.getUUID();
        var hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(10.0, health));
        p.setHealth(p.getMaxHealth());
        level.addFreshEntity(p);
        RukhFx.burst(level, at.add(0, 1.0, 0), RukhFx.Alignment.DARK, 10);
        return p;
    }

    public UUID bossId() { return bossId; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl) || !isAlive()) return;
        Entity boss = bossId == null ? null : sl.getEntity(bossId);
        if (!(boss instanceof LichBoss lich) || !lich.isAlive()) {
            if (++orphanTicks > 100) discard();
            return;
        }
        orphanTicks = 0;
        if (tickCount % 6 == 0) {
            RukhFx.stream(sl, this, lich, RukhFx.Alignment.DARK, 2);
            RukhFx.aura(sl, this, RukhFx.Alignment.DARK);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity a = source.getEntity();
        if (a instanceof CalamityBoss || (a != null && MobTouchHandler.isHelper(a))) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        RukhFx.burst(sl, this, RukhFx.Alignment.DARK, 12);
        Entity boss = bossId == null ? null : sl.getEntity(bossId);
        if (boss instanceof LichBoss lich) lich.phylacteryDown(this);
    }

    @Override public boolean isPushable() { return false; }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override public boolean fireImmune() { return true; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (bossId != null) tag.putUUID("Boss", bossId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Boss")) bossId = tag.getUUID("Boss");
    }
}
