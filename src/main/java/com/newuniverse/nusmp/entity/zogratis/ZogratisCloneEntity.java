package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.wizard.BornMagic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A Zogratis clone: one of the realistic copies Lucius makes to distract the party while he carries out his plan. It wears his look
 * (the renderer draws the body of its {@link #form()}), keeps its distance, and every few seconds casts a weak copy of a tower spell:
 * a short telegraph, then slow soul motes in the colour of a random magic. It has little health and shatters into soul motes when
 * struck down (a Resurrection can raise it again); it fades on its own after {@link SoulMath#cloneLife}; it belongs to one Lucius,
 * dies without drops when he does, and is never more than {@link SoulMath#cloneCap} alive at once.
 * <p>
 * For the renderer: {@link #castLeft()} (the telegraph, arm raised), {@link #markLeft()} (the Swap Strike has chosen this clone: it
 * flashes gold), {@link #fade()} (1 down to 0 over its last two seconds), {@link #form()} and {@link #glowColor()}.
 */
public class ZogratisCloneEntity extends Monster {
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(ZogratisCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MARK = SynchedEntityData.defineId(ZogratisCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(ZogratisCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(ZogratisCloneEntity.class, EntityDataSerializers.INT);

    /** Ticks of the fade-out the client is told about. */
    public static final int FADE_TICKS = 40;
    private static final int TELL_TICKS = 14;

    private UUID ownerId;
    private int life = 600, power = 20;
    private double damageMult = 0.6;
    private long nextSpell = 60;
    private LivingEntity aimed;

    public ZogratisCloneEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.ARMOR, 2).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CAST, 0);
        b.define(MARK, 0);
        b.define(LIFE, FADE_TICKS + 1);
        b.define(FORM, ZogratisBoss.FORM_BASE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Hold(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Keeps 6 to 11 blocks from its target and circles it: a distraction that does not charge in. */
    private static final class Hold extends Goal {
        private final ZogratisCloneEntity clone;
        private int repath, flip;
        private double side = 1;

        Hold(ZogratisCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = clone.getTarget();
            return t != null && t.isAlive() && clone.castLeft() == 0;
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = clone.getTarget();
            if (t == null) return;
            clone.getLookControl().setLookAt(t, 30f, 30f);
            if (--repath > 0) return;
            repath = 6;
            double d = clone.distanceTo(t);
            Vec3 off = clone.position().subtract(t.position()).multiply(1, 0, 1);
            if (off.lengthSqr() < 1e-4) off = new Vec3(1, 0, 0);
            Vec3 radial = off.normalize();
            if (d > 11) clone.getNavigation().moveTo(t, 1.0);
            else if (d < 6) {
                Vec3 to = clone.position().add(radial.scale(5));
                clone.getNavigation().moveTo(to.x, to.y, to.z, 1.1);
            } else {
                if (--flip <= 0) { flip = 6 + clone.getRandom().nextInt(8); side = -side; }
                Vec3 to = t.position().add(radial.scale(Math.max(6, Math.min(11, d)))).add(new Vec3(-radial.z, 0, radial.x).scale(3.0 * side));
                clone.getNavigation().moveTo(to.x, to.y, to.z, 0.9);
            }
        }
    }

    // ---------------------------------------------------------------- what it is
    public Devil devil() { return Devil.ASTAROTH; }

    /** The glow colour (0xRRGGBB): Astaroth's gold. */
    public int glowColor() { return DevilStyle.color(devil()); }

    /** The body it wears: {@link ZogratisBoss#FORM_BASE} or {@link ZogratisBoss#FORM_DIVINE}, as Lucius had when it was made. */
    public int form() { return entityData.get(FORM); }

    /** Ticks left of the spell telegraph (0 when idle). */
    public int castLeft() { return entityData.get(CAST); }

    /** Ticks left of the Swap Strike's mark on this clone (0 when it is not marked). */
    public int markLeft() { return entityData.get(MARK); }

    /** 1 normally, falling to 0 over its last {@value #FADE_TICKS} ticks. */
    public float fade() { return Math.max(0f, Math.min(1f, (entityData.get(LIFE) - 1) / (float) FADE_TICKS)); }

    /** The Lucius it serves. */
    public UUID ownerId() { return ownerId; }

    /** The clone comes apart on purpose (Julius's will surfaces, the fight is over): not struck down, so Resurrection cannot raise it. */
    void fadeAway() {
        if (level() instanceof ServerLevel sl && !isRemoved()) shatter(sl, false);
    }

    /** Marks this clone as the one the Swap Strike will trade places with. */
    void mark(int ticks) { entityData.set(MARK, Math.max(0, ticks)); }

    /**
     * Makes a clone of 'boss' at 'at': its health, life and damage come from the boss's power. Null if the boss already has as many
     * clones as its power allows.
     */
    public static ZogratisCloneEntity spawnFor(LuciusBoss boss, ServerLevel level, Vec3 at) {
        if (boss.clones().size() >= SoulMath.cloneCap(boss.power())) return null;
        ZogratisCloneEntity c = ZogratisEntities.ZOGRATIS_CLONE.get().create(level);
        if (c == null) return null;
        c.moveTo(at.x, at.y, at.z, boss.getYRot(), 0);
        c.copyFrom(boss);
        level.addFreshEntity(c);
        LuciusFx.sparkle(level, at.add(0, 1, 0), 18, 0.5);
        LuciusFx.dust(level, at.add(0, 1, 0), LuciusFx.GOLD, 1.4f, 14, 0.5);
        Tells.ring(level, at, 1.4, LuciusFx.PALE);
        return c;
    }

    private void copyFrom(LuciusBoss boss) {
        ownerId = boss.getUUID();
        power = boss.power();
        damageMult = boss.tuning().damageMultiplier();
        life = SoulMath.cloneLife(power);
        nextSpell = tickCount + 40 + getRandom().nextInt(40);
        entityData.set(FORM, boss.form());
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(SoulMath.cloneHealth(power));
        setHealth(getMaxHealth());
    }

    private LuciusBoss owner(ServerLevel sl) {
        return ownerId != null && sl.getEntity(ownerId) instanceof LuciusBoss b && b.isAlive() ? b : null;
    }

    @Override public boolean isAlliedTo(Entity other) { return ZogratisEntities.isFamily(other) || super.isAlliedTo(other); }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    /** A clone leaves nothing behind. */
    @Override protected boolean shouldDropLoot() { return false; }

    // ---------------------------------------------------------------- behaviour
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        long t = tickCount;
        if (--life <= 0) {
            shatter(sl, false);
            return;
        }
        if (life <= FADE_TICKS) entityData.set(LIFE, life + 1);
        if (t % 10 == 0) {
            LuciusBoss boss = owner(sl);
            if (boss == null) {
                shatter(sl, false);
                return;
            }
            power = boss.power();
            damageMult = boss.tuning().damageMultiplier();
            LivingEntity bt = boss.getTarget();
            if ((getTarget() == null || !getTarget().isAlive()) && bt != null && bt.isAlive()) setTarget(bt);
        }
        int mark = markLeft();
        if (mark > 0) {
            entityData.set(MARK, mark - 1);
            if (mark % 3 == 0) {
                Tells.column(sl, position(), 3.0, LuciusFx.GOLD);
                Tells.ring(sl, position(), 1.6, LuciusFx.PALE);
            }
        }
        LivingEntity tg = getTarget();
        if (tg == null || !tg.isAlive()) {
            if (castLeft() > 0) entityData.set(CAST, 0);
            return;
        }
        int cast = castLeft();
        if (cast > 0) {
            entityData.set(CAST, cast - 1);
            getNavigation().stop();
            getLookControl().setLookAt(tg, 40f, 40f);
            if (cast % 3 == 0 && aimed != null && aimed.isAlive()) {
                Tells.line(sl, position().add(0, 1.5, 0), aimed.getBoundingBox().getCenter(), LuciusFx.PALE);
                LuciusFx.sparkle(sl, position().add(0, 1.6, 0), 2, 0.25);
            }
            if (cast == 1) release(sl);
        } else if (t >= nextSpell && distanceToSqr(tg) < 28 * 28 && hasLineOfSight(tg)) {
            nextSpell = t + SoulMath.cloneSpellGap(power) + getRandom().nextInt(30);
            aimed = tg;
            entityData.set(CAST, TELL_TICKS);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.9f, 1.4f);
        }
    }

    /** The weak spell: two slow soul motes in the colour of a random magic, each a third of the strength of Lucius's own. */
    private void release(ServerLevel sl) {
        LivingEntity tg = aimed != null && aimed.isAlive() ? aimed : getTarget();
        aimed = null;
        if (tg == null || !tg.isAlive()) return;
        MagicType[] all = MagicType.values();
        MagicType m = all[getRandom().nextInt(all.length)];
        if (m.restricted()) m = MagicType.SOUL;
        int rgb = BornMagic.colorOf(m) & 0xFFFFFF;
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        Vec3 from = position().add(0, 1.5, 0);
        Vec3 aim = tg.getBoundingBox().getCenter().subtract(from);
        final MagicType magic = m;
        final float base = TowerMath.cloneDamage(TowerMath.Shape.BOLT);
        for (int i = 0; i < 2; i++) {
            final double off = (i - 0.5) * 0.18;
            new LuciusFx.Shot(this, from, LuciusFx.rotateY(aim, off), 0.7, 36, 0.5, 0.06, tg, rgb, e -> LuciusFx.isFoe(this, e), e -> {
                if (ZogratisDamage.hit(this, e, base, damageMult, 1.0, power / 100.0, true)) LuciusFx.rider(magic.hit, e, this);
            }).launch(sl);
        }
        sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.8f, 1.5f);
    }

    /** It comes apart into soul motes. 'fallen' says it was struck down (the owner may raise it again) rather than faded. */
    private void shatter(ServerLevel sl, boolean fallen) {
        Vec3 at = position().add(0, 1, 0);
        LuciusFx.souls(sl, at, 22, 0.5);
        LuciusFx.sparkle(sl, at, 16, 0.5);
        LuciusFx.dust(sl, at, LuciusFx.GOLD, 1.5f, 14, 0.5);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.0f, 1.3f);
        if (fallen && owner(sl) != null) owner(sl).noteCloneFallen();
        discard();
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            shatter(sl, true);
            return;
        }
        super.die(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && ZogratisEntities.isFamily(source.getEntity())) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) { return false; }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Life", life);
        tag.putInt("Power", power);
        tag.putInt("Form", form());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        if (tag.contains("Life")) life = tag.getInt("Life");
        if (tag.contains("Power")) power = tag.getInt("Power");
        entityData.set(FORM, tag.getInt("Form"));
    }
}
