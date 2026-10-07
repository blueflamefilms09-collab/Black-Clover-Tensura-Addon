package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Spirit Lord companion: the spirit that chose you, given a body. It follows you, fights your
 * enemies (melee + an elemental cast every few seconds) and uses Tensura's own spirit models
 * and animations, loaded straight from the Tensura mod (nothing is copied into this mod).
 */
public class SpiritLordEntity extends TamableAnimal implements GeoEntity {
    /** Spirit kinds: name, Tensura geo/animation id, Tensura texture, animations, colour. */
    public enum Kind {
        SALAMANDER("Salamander", "ifrit", "ifrit/ifrit", "idle", "walk", "fire_ball_right", 0xFFFF6A1E),
        UNDINE("Undine", "undine", "undine/undine", "idle", "walk", "water_ball_right", 0xFF4FA8FF),
        SYLPH("Sylph", "sylphide", "sylphide/sylphide", "idle", "fly", "wind_blade_right", 0xFF8CFFC2),
        GNOME("Gnome", "beast_gnome", "beast_gnome/beast_gnome", "idle", "walk", "slam", 0xFFB08850),
        BLACK_DEVIL("Black Devil", "greater_daemon", "daemon/greater_daemon", "idle", "walk", "magic_shoot", 0xFF2A0A30);

        public final String display, geo, texture, idle, move, cast;
        public final int color;
        Kind(String d, String g, String t, String i, String m, String c, int col) { display = d; geo = g; texture = t; idle = i; move = m; cast = c; color = col; }

        public RawAnimation anim(String name) { return RawAnimation.begin().thenLoop("animation." + geo + "." + name); }
        public static Kind byName(String n) { try { return valueOf(n); } catch (Exception e) { return SALAMANDER; } }
        public static Kind forSpirit(String spiritName) {
            return switch (spiritName) { case "Undine" -> UNDINE; case "Sylph" -> SYLPH; case "Gnome" -> GNOME; default -> SALAMANDER; };
        }
    }

    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(SpiritLordEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int castCooldown = 40;

    public SpiritLordEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 8);
    }

    public Kind kind() { return Kind.byName(entityData.get(KIND)); }
    public void setKind(Kind k) { entityData.set(KIND, k.name()); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, Kind.SALAMANDER.name());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SummonBrain.Caster(this, 0xFF9AE8FF, 7f, false, "spirit", "wind", "water", "fire", "light"));          // 0.49
        targetSelector.addGoal(0, new SummonBrain.Guard(this, 18));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1, 8f, 3f));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /** Every few seconds: an elemental cast at the current target. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (--castCooldown > 0 || t == null || !t.isAlive() || distanceToSqr(t) > 16 * 16 || !hasLineOfSight(t)) return;
        castCooldown = 60;
        Kind k = kind();
        triggerAnim("cast", "cast_" + k.name().toLowerCase());
        LivingEntity owner = getOwner();
        DamageSource src = owner != null ? damageSources().indirectMagic(this, owner) : damageSources().mobAttack(this);
        t.hurt(src, BalanceLaw.damage(t, 10f, 0.5));
        if (level() instanceof ServerLevel sl) {
            var from = getEyePosition(); var to = t.getBoundingBox().getCenter();
            switch (k) {
                case SALAMANDER -> { VfxSpawn.send(sl, VfxShape.FLAME_TRAIL, from, to, k.color, 14, 0.8f); t.igniteForSeconds(4); }
                case UNDINE -> { VfxSpawn.send(sl, VfxShape.WATER_SPLASH, to, to, k.color, 18, 0.9f); t.clearFire(); }
                case SYLPH -> { VfxSpawn.send(sl, VfxShape.WIND_SLASH, from, to, k.color, 14, 1f); t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0)); t.hurtMarked = true; }
                case GNOME -> VfxSpawn.send(sl, VfxShape.EARTH_SPIKES, position(), t.position(), k.color, 18, 1.2f);
                case BLACK_DEVIL -> VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, from, to, k.color, 16, 1.2f);
            }
        }
    }

    // ---------------------------------------------------------------- GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SpiritLordEntity> main = new AnimationController<>(this, "main", 5, state ->
                state.setAndContinue(state.isMoving() ? kind().anim(kind().move) : kind().anim(kind().idle)));
        controllers.add(main);
        AnimationController<SpiritLordEntity> cast = new AnimationController<>(this, "cast", 2, state -> software.bernie.geckolib.animation.PlayState.STOP);
        for (Kind k : Kind.values())
            cast.triggerableAnim("cast_" + k.name().toLowerCase(), RawAnimation.begin().thenPlay("animation." + k.geo + "." + k.cast));
        controllers.add(cast);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ---------------------------------------------------------------- misc
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == getOwner()) return false;   // owner can't hit their spirit
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putString("SpiritKind", kind().name()); }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setKind(Kind.byName(tag.getString("SpiritKind"))); }

    /** Summons (or dismisses, if already out) a player's Spirit Lord companion. */
    public static void toggle(net.minecraft.server.level.ServerPlayer p, Kind kind) {
        var data = p.getPersistentData();
        if (data.hasUUID("nusmp_spirit_lord")) {
            for (ServerLevel l : p.getServer().getAllLevels()) {
                var e = l.getEntity(data.getUUID("nusmp_spirit_lord"));
                if (e instanceof SpiritLordEntity s) {
                    VfxSpawn.send(l, VfxShape.MAGIC_CIRCLE_EXPLOSION, s.position(), s.position().add(0, 1, 0), s.kind().color, 20, 1f);
                    s.discard();
                    data.remove("nusmp_spirit_lord");
                    p.displayClientMessage(Component.literal(kind.display + " returns to you."), true);
                    return;
                }
            }
        }
        SpiritLordEntity s = NUEntities.SPIRIT_LORD.get().create(p.serverLevel());
        if (s == null) return;
        s.setKind(kind);
        s.tame(p);
        String trueName = com.newuniverse.nusmp.book.SpiritBond.trueName(p);
        s.setCustomName(Component.literal(trueName.isEmpty() ? p.getName().getString() + "'s " + kind.display : trueName + ", the " + kind.display));
        var at = p.position().add(p.getLookAngle().multiply(2, 0, 2));
        s.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180, 0);
        p.serverLevel().addFreshEntity(s);
        data.putUUID("nusmp_spirit_lord", s.getUUID());
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), kind.color, 30, 1.6f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, s, s.position().add(0, 1, 0), kind.color, 60, 1.2f);
        p.displayClientMessage(Component.literal(kind.display + " takes form beside you."), true);
    }
}
