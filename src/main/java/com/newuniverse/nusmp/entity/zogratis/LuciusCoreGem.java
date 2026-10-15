package com.newuniverse.nusmp.entity.zogratis;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Why: the owner's core hitbox is a second, distinct target on Lucius's chest, live only in act 3 while the gem is shown. In NeoForge
 * that is a {@link PartEntity}: a child entity that the level indexes next to its parent (so the player's crosshair and arrows can
 * pick it) and whose {@code hurt} we route to the parent, exactly like the ender dragon's parts. It is a part and not a second mob
 * because it must move with him, share his health and never exist on its own: it is never saved and never spawned (its parent builds it
 * in its constructor on both sides), it has no collision, and it is pickable only while the parent says the gem is out (a synced flag,
 * so the client's crosshair agrees with the server). A blow on the gem is classified by the parent ({@link LuciusBoss#hurtGem}): a
 * critical hit or anti-magic shatters his resonance, anything else is an ordinary body hit.
 * <p>
 * The box is 0.6 x 0.6 and sits a little in front of the chest, so that an attacker who stands in front of him reaches it before the
 * body's own box; from behind the body is hit instead, which is how "face him to hit the gem" comes about without any extra rule. It has
 * no renderer: the gem is drawn by the VFX layer {@code LuciusCoreGemLayer} while the window is open.
 */
public final class LuciusCoreGem extends PartEntity<LuciusBoss> {
    /** The size of the gem's hit box. */
    public static final float SIZE = 0.6f;

    private final EntityDimensions size = EntityDimensions.scalable(SIZE, SIZE);

    public LuciusCoreGem(LuciusBoss parent) {
        super(parent);
        refreshDimensions();
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    /** Only while the gem is out: a closed gem is not a target at all, the crosshair passes to the body. */
    @Override public boolean isPickable() { return getParent().gemExposed(); }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        return getParent().hurtGem(source, amount);
    }

    /** The gem is part of Lucius: whatever asks "is this him?" gets yes. */
    @Override public boolean is(Entity other) { return this == other || getParent() == other; }

    @Override public EntityDimensions getDimensions(Pose pose) { return size == null ? super.getDimensions(pose) : size; }

    @Override public boolean shouldBeSaved() { return false; }
}
