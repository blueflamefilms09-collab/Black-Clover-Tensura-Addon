package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpiritSlots;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * 0.102: the Black Clover Spirit Lords in their true bodies: Salamander (a winged fire dragon), Undine (a woman of water), Sylph (a green
 * fairy) and Gnome (a stone giant). 1.99.0: as the canon has them, Salamander is massive, Undine a giant mermaid and Sylph a tiny winged fairy (see docs/rebuild/great_spirits.md). Unlike the small orbs (see {@link SpiritLordEntity}, the Tensura-style spirits anyone may call), these
 * four are exclusive, as in Black Clover: each element has exactly one mage on the server ({@link SpiritSlots}), only that mage can call
 * it, and only one of each can exist. They are built from tools/gen_spirit_lords.py and drawn by client.ElementalSpiritRenderer.
 * <ul>
 *   <li>Salamander flies; <b>Flame Breath</b>: a cone of fire, and its master burns less.</li>
 *   <li>Undine floats; <b>Tidal Lance</b>: a water javelin that slows, and it mends its master.</li>
 *   <li>Sylph flies; <b>Gale Cut</b>: a wind blade that throws foes up, and it quickens its master.</li>
 *   <li>Gnome walks; <b>Earth Slam</b>: stone claws round the foe, and it hardens its master.</li>
 * </ul>
 */
public class ElementalSpiritLordEntity extends TamableAnimal {
    public enum Kind {
        // 1.99.0 (Great Spirits): Salamander a MASSIVE dragon, Undine a GIANT mermaid, Sylph a TINY fairy (the hit boxes follow the canon sizes the renderer draws, client.SpiritLooks)
        SALAMANDER("Salamander", "Salamander", 0xFFFF6A1E, true, 1.8f, 2.5f),
        UNDINE("Undine", "Undine", 0xFF4FA8FF, true, 1.3f, 3.3f),
        SYLPH("Sylph", "Sylph", 0xFF8CFFC2, true, 0.35f, 0.45f),
        GNOME("Gnome", "Gnome", 0xFFB08850, false, 1.3f, 3.0f);

        public final String display, slot;
        public final int color;
        public final boolean flies;
        final float width, height;

        Kind(String display, String slot, int color, boolean flies, float w, float h) {
            this.display = display; this.slot = slot; this.color = color; this.flies = flies; width = w; height = h;
        }

        public static Kind byName(String n) { try { return valueOf(n); } catch (Exception e) { return SALAMANDER; } }

        /** The server's spirit slot name (Salamander, Undine, Sylph, Gnome), or null. */
        public static Kind forSpirit(String slot) {
            for (Kind k : values()) if (k.slot.equalsIgnoreCase(slot)) return k;
            return null;
        }
    }

    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(ElementalSpiritLordEntity.class, EntityDataSerializers.STRING);
    static final byte EVENT_CAST = 100;
    static final String K_BODY = "nusmp_spirit_lord_body";

    private int castCooldown = 50, auraCooldown = 60, mendCooldown;
    /** Client: the tick the last cast began (drives the cast clip). */
    public int clientCastTick = -1000;

    public ElementalSpiritLordEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        applyKind();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 160).add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 10).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public Kind kind() { return Kind.byName(entityData.get(KIND)); }

    public void setKind(Kind k) {
        entityData.set(KIND, k.name());
        applyKind();
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, Kind.SALAMANDER.name());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (KIND.equals(key)) refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        Kind k = kind();
        return EntityDimensions.scalable(k.width, k.height);
    }

    /** Flying kinds hover and use flying navigation; the Gnome walks. (The follow goal below reads the navigation each time, so a swap is safe.) */
    private void applyKind() {
        if (kind().flies) {
            moveControl = new FlyingMoveControl(this, 12, true);
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level());
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            nav.setCanPassDoors(true);
            navigation = nav;
            setNoGravity(true);
        } else {
            moveControl = new MoveControl(this);
            navigation = new GroundPathNavigation(this, level());
            setNoGravity(false);
        }
    }

    @Override
    public boolean fireImmune() { return kind() == Kind.SALAMANDER || super.fireImmune(); }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        targetSelector.addGoal(0, new SummonBrain.Guard(this, 20));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new Follow(this, 1.15, 7f, 3f));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /** Follows the owner (reads the current navigation every time) and rejoins them by teleport if left far behind. */
    private static final class Follow extends Goal {
        private final ElementalSpiritLordEntity mob;
        private final double speed;
        private final float start, stop;
        private int recalc;

        Follow(ElementalSpiritLordEntity mob, double speed, float start, float stop) {
            this.mob = mob; this.speed = speed; this.start = start; this.stop = stop;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = mob.getOwner();
            return o != null && !o.isSpectator() && mob.distanceToSqr(o) > start * start;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity o = mob.getOwner();
            return o != null && mob.distanceToSqr(o) > stop * stop;
        }

        @Override public void start() { recalc = 0; }

        @Override
        public void tick() {
            LivingEntity o = mob.getOwner();
            if (o == null) return;
            mob.getLookControl().setLookAt(o, 10f, mob.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 8;
            if (mob.distanceToSqr(o) > 28 * 28 && mob.level() instanceof ServerLevel) {                       // too far: step beside the owner
                Vec3 at = o.position().add(o.getLookAngle().multiply(-2, 0, -2));
                mob.teleportTo(at.x, o.getY() + (mob.kind().flies ? 1.0 : 0.0), at.z);
                mob.getNavigation().stop();
                return;
            }
            mob.getNavigation().moveTo(o, speed);
        }
    }

    // ---------------------------------------------------------------- abilities
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        LivingEntity owner = getOwner();
        Kind k = kind();
        if (k.flies && owner != null && !getNavigation().isInProgress() && getY() < owner.getY() + 0.5 && distanceToSqr(owner) < 100)
            setDeltaMovement(getDeltaMovement().add(0, 0.02, 0));                                              // hover a little above the ground
        if (--auraCooldown <= 0 && owner instanceof ServerPlayer p && distanceToSqr(p) < 24 * 24) {
            auraCooldown = 100;
            aura(p, k);
        }
        if (--mendCooldown <= 0 && owner != null && k == Kind.UNDINE && owner.getHealth() < owner.getMaxHealth() * 0.7f && distanceToSqr(owner) < 14 * 14) {
            mendCooldown = 160;
            BalanceLaw.heal(owner, owner.getMaxHealth() * 0.12f);
            VfxSpawn.send(sl, VfxShape.WATER_RING, owner.position(), owner.position().add(0, 1, 0), k.color, 18, 1.0f);
        }
        LivingEntity t = getTarget();
        if (--castCooldown > 0 || t == null || !t.isAlive() || distanceToSqr(t) > 14 * 14 || !hasLineOfSight(t)) return;
        castCooldown = 70;
        cast(sl, k, t, owner);
    }

    /** What the spirit gives its master while it is near: small, steady and in the spirit's nature. */
    private void aura(ServerPlayer p, Kind k) {
        switch (k) {
            case SALAMANDER -> p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0, true, false));
            case UNDINE -> p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 220, 0, true, false));
            case SYLPH -> { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, true, false)); p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, true, false)); }
            case GNOME -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 220, 0, true, false));
        }
    }

    private void cast(ServerLevel sl, Kind k, LivingEntity t, @Nullable LivingEntity owner) {
        level().broadcastEntityEvent(this, EVENT_CAST);
        DamageSource src = owner != null ? damageSources().indirectMagic(this, owner) : damageSources().mobAttack(this);
        Vec3 from = getEyePosition(), to = t.getBoundingBox().getCenter();
        Vec3 dir = to.subtract(from).normalize();
        switch (k) {
            case SALAMANDER -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(11), x -> x != this && x != owner && x.isAlive() && !x.isAlliedTo(this))) {
                    Vec3 d = e.getBoundingBox().getCenter().subtract(from);
                    if (d.length() > 11 || d.normalize().dot(dir) < 0.75) continue;
                    e.hurt(src, BalanceLaw.damage(e, 12f, 0.5));
                    e.igniteForSeconds(6);
                }
                VfxSpawn.send(sl, VfxShape.FIRE_SPIRAL, from, from.add(dir.scale(11)), k.color, 22, 1.4f);
                VfxSpawn.send(sl, VfxShape.FIRE_BURST, to, to, k.color, 16, 1.0f);
            }
            case UNDINE -> {
                t.hurt(src, BalanceLaw.damage(t, 11f, 0.5));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                t.clearFire();
                VfxSpawn.send(sl, VfxShape.WATER_JAVELIN, from, to, k.color, 16, 1.2f);
                VfxSpawn.send(sl, VfxShape.WATER_SPLASH, to, to, k.color, 18, 1.0f);
            }
            case SYLPH -> {
                t.hurt(src, BalanceLaw.damage(t, 9f, 0.5));
                t.setDeltaMovement(t.getDeltaMovement().add(0, 0.6, 0));
                t.hurtMarked = true;
                VfxSpawn.send(sl, VfxShape.WIND_EMPEROR, from, to, k.color, 16, 1.0f);
            }
            case GNOME -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(3), x -> x != this && x != owner && x.isAlive() && !x.isAlliedTo(this))) {
                    e.hurt(src, BalanceLaw.damage(e, 14f, 0.5));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1e-4) e.knockback(0.8, -away.x, -away.z);
                }
                VfxSpawn.send(sl, VfxShape.EARTH_CLAWS, t.position(), t.position(), k.color, 20, 2.2f);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CAST) clientCastTick = tickCount;
        else super.handleEntityEvent(id);
    }

    // ---------------------------------------------------------------- misc
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == getOwner()) return false;     // the master cannot hurt their spirit
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putString("SpiritKind", kind().name()); }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(Kind.byName(tag.getString("SpiritKind")));
    }

    // ---------------------------------------------------------------- calling and dismissing
    /**
     * Calls (or sends back, if already out) the player's elemental Spirit Lord. Only the one mage who holds the element can call it
     * ({@code force} is for admins); only one body of each element exists, so a stray one is removed first.
     * Returns false if nothing was done.
     */
    public static boolean toggle(ServerPlayer p, Kind kind, boolean force) {
        SpiritSlots slots = SpiritSlots.get(p.getServer());
        if (!force && !p.getUUID().equals(slots.owner(kind.slot))) {
            p.displayClientMessage(Component.literal(kind.display + " has not chosen you."), true);
            return false;
        }
        var data = p.getPersistentData();
        boolean wasOut = false;
        for (ServerLevel l : p.getServer().getAllLevels()) {                                                  // one body per element: remove any (also this player's own)
            for (ElementalSpiritLordEntity e : l.getEntities(NUEntities.ELEMENTAL_SPIRIT_LORD.get(), x -> x.kind() == kind)) {
                if (e.getOwner() == p) wasOut = true;
                VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE_EXPLOSION, e.position(), e.position().add(0, 1, 0), kind.color, 20, 1f);
                e.discard();
            }
        }
        data.remove(K_BODY);
        if (wasOut) {
            p.displayClientMessage(Component.literal(kind.display + " returns to you."), true);
            return true;
        }
        ElementalSpiritLordEntity s = NUEntities.ELEMENTAL_SPIRIT_LORD.get().create(p.serverLevel());
        if (s == null) return false;
        s.setKind(kind);
        s.tame(p);
        String trueName = com.newuniverse.nusmp.book.SpiritBond.trueName(p);
        s.setCustomName(Component.literal(trueName.isEmpty() ? p.getName().getString() + "'s " + kind.display : trueName + ", the " + kind.display));
        Vec3 at = p.position().add(p.getLookAngle().multiply(2.5, 0, 2.5));
        s.moveTo(at.x, p.getY() + (kind.flies ? 0.5 : 0), at.z, p.getYRot() + 180, 0);
        p.serverLevel().addFreshEntity(s);
        data.putUUID(K_BODY, s.getUUID());
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), kind.color, 30, 1.8f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, s, s.position().add(0, 1, 0), kind.color, 60, 1.4f);
        p.displayClientMessage(Component.literal(kind.display + " takes its true form beside you."), true);
        return true;
    }
}
