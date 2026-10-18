package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Status;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Tally;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Timed;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: the Vassal Carriage is the one hero weapon whose technique is a VEHICLE: using the weapon calls a carriage that seats the wielder as its driver and two
 * more people as passengers, runs fast on the ground in the direction the driver looks, rams foes while a technique tells it to, and goes away by itself. It is
 * built on a {@link LivingEntity} (not a custom entity) because vanilla already knows how to let a player steer a living vehicle: the driver's client runs the
 * movement (the {@code getRiddenInput}, {@code tickRidden} and {@code getRiddenSpeed} hooks, exactly as a horse does) and the server accepts it through
 * the vehicle-move packet, so no network code of ours is involved and the movement is as smooth and as cheat-checked as a horse's. Like a tamed pet it is an
 * {@link OwnableEntity} of its wielder, so the weapons' foe filters leave it (and its owner's team) alone.
 * <p>
 * Rules (the owner's brief): ONE carriage per wielder ({@link CarriageRegistry}); only the OWNER drives it ({@link #getControllingPassenger}); other people may
 * board only as invited passengers (an ally, a teammate, a pet) and only once the owner is aboard ({@link CarriageRules#mayBoard}); it has health and can be
 * destroyed; it hurts nobody by driving: damage happens only while a technique runs a ram ({@link #startBoost}, {@link #startCharge}), once per foe, through
 * {@code HeroCtx.hit} for the owner (so the balance law applies); it never touches a block; it is gone when the owner has been off it for ten seconds, when
 * the weapon is put away or the bond ends, when the owner leaves the game, or when it is destroyed. Falls, drowning and being stuck in a wall do not hurt it.
 */
public final class CarriageEntity extends LivingEntity implements OwnableEntity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(CarriageEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(CarriageEntity.class, EntityDataSerializers.BYTE);
    private static final int FLAG_BOOST = 1, FLAG_BARRIER = 2, FLAG_CHARGE = 4, FLAG_HASTE = 8;

    /** What a running ram does. */
    private enum Ram { NONE, BOOST, CHARGE }

    /** Set once the first fault of a carriage's tick has been logged (one line per server session is enough; cleared when the server stops). */
    private static boolean FAULT_LOGGED;

    /** Lets the next session log its own first fault (called when a server stops). */
    static void forgetFaultLog() { FAULT_LOGGED = false; }

    private int seats = CarriageNumbers.SEATS_PLAIN;
    private Ram ram = Ram.NONE;
    private int ramTicks;
    private Tally rammed = new Tally(Budget.MAX_TARGETS, 1);
    private final List<LivingEntity> crashed = new ArrayList<>();
    private boolean stunned;
    private int barrierTicks, hasteTicks;
    private final CarriageRules.Absence absence = new CarriageRules.Absence();

    public CarriageEntity(EntityType<? extends CarriageEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, CarriageNumbers.BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, CarriageNumbers.BASE_SPEED)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_FLAGS, (byte) 0);
    }

    // ================================================================ identity and set-up
    /** The wielder who called the carriage (known to both sides: the driver's client needs it to know it is the driver). */
    public UUID ownerUuid() { return entityData.get(DATA_OWNER).orElse(null); }

    void setOwner(UUID owner) { entityData.set(DATA_OWNER, Optional.ofNullable(owner)); }

    /** Like a tamed pet the carriage belongs to its wielder: the weapons' foe filters (and the rest of the mod) then leave it, and its owner's team, alone. */
    @Override
    public UUID getOwnerUUID() { return ownerUuid(); }

    /** Applies a chassis: health, speed and seats. */
    void configure(CarriageNumbers.Chassis chassis) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(chassis.health());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(chassis.speed());
        setHealth((float) chassis.health());
        seats = chassis.seats();
    }

    public int seats() { return seats; }

    private boolean flag(int bit) { return (entityData.get(DATA_FLAGS) & bit) != 0; }

    private void setFlag(int bit, boolean on) {
        byte f = entityData.get(DATA_FLAGS);
        entityData.set(DATA_FLAGS, (byte) (on ? f | bit : f & ~bit));
    }

    public boolean boosting() { return flag(FLAG_BOOST); }

    public boolean charging() { return flag(FLAG_CHARGE); }

    /** True while the cabin barrier is up (the renderer shows it). */
    public boolean barrierUp() { return flag(FLAG_BARRIER); }

    /** True while Haikuikku runs (the carriage is faster; both sides need it because the driver's client does the moving). */
    public boolean hasted() { return flag(FLAG_HASTE); }

    private ServerPlayer ownerPlayer() {
        UUID id = ownerUuid();
        return id == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(id);
    }

    // ================================================================ boarding and driving
    /** May this entity get on right now? (server side) */
    public boolean mayBoard(Entity passenger) {
        UUID ownerId = ownerUuid();
        if (ownerId == null) return false;
        ServerPlayer owner = ownerPlayer();
        boolean isOwner = ownerId.equals(passenger.getUUID());
        boolean ownerAboard = false;
        for (Entity p : getPassengers()) if (ownerId.equals(p.getUUID())) ownerAboard = true;
        boolean ally = owner != null && passenger instanceof LivingEntity living && Foes.isAlly(owner, living);
        return CarriageRules.mayBoard(isOwner, ownerAboard, ally, getPassengers().size(), seats);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return level().isClientSide || mayBoard(passenger); }

    @Override
    public LivingEntity getControllingPassenger() {
        UUID ownerId = ownerUuid();
        if (ownerId == null) return null;
        for (Entity e : getPassengers()) if (e instanceof Player p && ownerId.equals(p.getUUID())) return p;
        return null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.sidedSuccess(true);
        if (player instanceof ServerPlayer sp) {
            if (sp.getVehicle() == this) return InteractionResult.sidedSuccess(false);          // the second hand of the same click
            if (!mayBoard(sp)) {
                boolean full = getPassengers().size() >= seats;
                UUID ownerId = ownerUuid();
                boolean ownerAboard = false;
                for (Entity p : getPassengers()) if (p.getUUID().equals(ownerId)) ownerAboard = true;
                sp.displayClientMessage(Component.literal(full ? "The carriage is full" : ownerAboard ? "This carriage is not yours to ride" : "The driver is not aboard yet"), true);
                return InteractionResult.CONSUME;
            }
            sp.startRiding(this);
        }
        return InteractionResult.sidedSuccess(false);
    }

    /** What the driver asks of the carriage: straight ahead, or a slow reverse; steering is by where the driver looks. */
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) { return new Vec3(0.0, 0.0, CarriageRules.forwardInput(player.zza)); }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (!isControlledByLocalInstance()) return;       // only the driver's own client steers; the server and everybody else take the yaw from the vehicle-move packets
        Vec3 v = getDeltaMovement();
        float yaw = (float) CarriageRules.steer(getYRot(), player.getYRot(), Math.sqrt(v.x * v.x + v.z * v.z));
        setRot(yaw, 0.0f);
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * CarriageRules.speedFactor(boosting(), charging(), hasted()));
    }

    // ================================================================ the tick
    @Override
    public void tick() {
        if (isInWater()) {                                      // a carriage floats, it does not sink (both sides: the driver's client moves it)
            Vec3 d = getDeltaMovement();
            setDeltaMovement(d.x, Math.min(0.2, d.y + 0.04), d.z);
        }
        super.tick();
        if (!level().isClientSide) serverTick();
    }

    private boolean stillCalled(ServerPlayer owner) {
        if (owner == null || !owner.isAlive()) return false;
        HeroBond bond = HeroBonds.of(owner);
        return bond.kind() == HeroKind.CARRIAGE && bond.summoned();
    }

    /** The server half of the tick; whatever goes wrong in it sends the carriage away (a throw out of an entity's tick would take the server down). */
    private void serverTick() {
        try {
            serverTickChecked();
        } catch (RuntimeException e) {
            if (!FAULT_LOGGED) {
                FAULT_LOGGED = true;
                LogUtils.getLogger().error("[nusmp] the carriage failed and was sent away", e);
            }
            try {
                dismiss();
            } catch (RuntimeException ignored) {
                discard();
            }
        }
    }

    private void serverTickChecked() {
        ServerPlayer owner = ownerPlayer();
        if (tickCount % 10 == 0 && !stillCalled(owner)) {
            dismiss();
            return;
        }
        absence.tick(owner != null && hasPassenger(owner));
        if (absence.expired(CarriageNumbers.ABSENT_LIMIT)) {
            dismiss();
            return;
        }
        ramStep(owner);
        if (barrierTicks > 0 && --barrierTicks == 0) setFlag(FLAG_BARRIER, false);
        if (hasteTicks > 0 && --hasteTicks == 0) setFlag(FLAG_HASTE, false);
    }

    // ================================================================ rams: the boost and the charge
    /** The boost: faster for {@code ticks}, and the first blow on each foe it runs into is {@code BOOST} raw with a shove. */
    void startBoost(int ticks) {
        beginRam(Ram.BOOST, ticks);
        setFlag(FLAG_BOOST, true);
        setFlag(FLAG_CHARGE, false);
    }

    /** The charge: faster still for {@code ticks}; each foe it crashes into takes the crash, the first is stunned, and every foe crashed into is stomped after. */
    void startCharge(int ticks) {
        beginRam(Ram.CHARGE, ticks);
        setFlag(FLAG_CHARGE, true);
        setFlag(FLAG_BOOST, false);
    }

    private void beginRam(Ram mode, int ticks) {
        ram = mode;
        ramTicks = ticks;
        rammed = new Tally(Budget.MAX_TARGETS, 1);
        crashed.clear();
        stunned = false;
    }

    /** True while a boost or a charge is running. */
    boolean ramming() { return ram != Ram.NONE; }

    private void ramStep(ServerPlayer owner) {
        if (ram == Ram.NONE) return;
        if (ramTicks-- <= 0) {
            endRam(owner);
            return;
        }
        if (owner == null || !hasPassenger(owner)) return;                       // only the owner at the reins makes it a weapon
        HeroCtx c = HeroCtx.of(owner);
        if (c == null || c.kind != HeroKind.CARRIAGE) return;
        double yaw = Math.toRadians(getYRot());
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        AABB zone = getBoundingBox().inflate(0.4).move(ahead.scale(CarriageNumbers.RAM_REACH));
        boolean charge = ram == Ram.CHARGE;
        double raw = charge ? CarriageNumbers.CRASH_CHARGE.rawMax() : CarriageNumbers.BOOST.rawBase();
        for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, zone, e -> e != this && !hasPassenger(e) && Foes.isFoe(owner, e))) {
            if (!rammed.allows(t.getUUID())) continue;
            if (!c.hit(t, (float) raw)) continue;
            rammed.add(t.getUUID());
            c.shove(t, position(), charge ? CarriageNumbers.CHARGE_PUSH : CarriageNumbers.RAM_PUSH);
            c.fx(VfxShape.GRAVITY_IMPACT, t.position(), t.position(), 0xFFFFC247, 12, 1.2f);
            level().playSound(null, t.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.8f, charge ? 0.7f : 1.0f);
            if (charge) {
                crashed.add(t);
                if (!stunned && c.control()) {
                    stunned = true;
                    Status.slow(t, BalanceLaw.controlTicks(t, CarriageNumbers.CHARGE_STUN_TICKS), 6);
                }
            }
        }
    }

    private void endRam(ServerPlayer owner) {
        boolean charge = ram == Ram.CHARGE;
        ram = Ram.NONE;
        setFlag(FLAG_BOOST, false);
        setFlag(FLAG_CHARGE, false);
        if (charge && owner != null && !crashed.isEmpty()) {
            HeroCtx c = HeroCtx.of(owner);
            if (c != null && c.kind == HeroKind.CARRIAGE) {
                List<LivingEntity> victims = new ArrayList<>(crashed);
                Timed.later(c, CarriageNumbers.STOMP_DELAY, live -> {
                    Tally once = new Tally(Budget.MAX_TARGETS, 1);
                    for (LivingEntity t : victims) {
                        if (!t.isAlive() || !once.allows(t.getUUID())) continue;
                        if (live.hit(t, (float) CarriageNumbers.CRASH_CHARGE.rawBase())) {
                            once.add(t.getUUID());
                            live.fx(VfxShape.GRAVITY_IMPACT, t.position(), t.position(), 0xFFFFC247, 14, 1.6f);
                        }
                    }
                    live.sound(SoundEvents.ANVIL_LAND, 0.9f, 0.6f);
                });
            }
        }
        crashed.clear();
    }

    /** Raises the cabin barrier for {@code ticks}: the carriage cannot be hurt meanwhile. */
    void startBarrier(int ticks) {
        barrierTicks = ticks;
        setFlag(FLAG_BARRIER, true);
    }

    /** Haikuikku: the carriage runs {@link CarriageNumbers#HASTE_FACTOR} times faster for {@code ticks}. */
    void startHaste(int ticks) {
        hasteTicks = ticks;
        setFlag(FLAG_HASTE, true);
    }

    // ================================================================ damage, death, going away
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (barrierUp() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && hasPassenger(attacker)) return false;            // the people aboard cannot hurt it
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public void die(DamageSource source) {
        if (level().isClientSide) super.die(source);
        else dismiss();
    }

    @Override
    protected void tickDeath() { discard(); }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {}

    @Override
    protected boolean shouldDropLoot() { return false; }

    @Override
    public boolean shouldDropExperience() { return false; }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) { return false; }

    @Override
    public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override
    public boolean canFreeze() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) CarriageRegistry.unregister(ownerUuid(), getUUID());
        super.remove(reason);
    }

    /** The visual model is longer than the hitbox: keep it drawn while its ends are still on screen. */
    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(1.2); }

    /** Puts everybody aboard on the ground beside the carriage (each at the first spot with room and ground) and returns them, the driver first. */
    List<Entity> ejectRiders() {
        List<Entity> riders = new ArrayList<>(getPassengers());
        if (level().isClientSide) return riders;
        for (Entity rider : riders) {
            try {
                Vec3 spot = safeSpot(rider);
                rider.stopRiding();
                rider.teleportTo(spot.x, spot.y, spot.z);
            } catch (RuntimeException e) {
                // one rider's own move failing must not keep the carriage standing; removing the carriage lets go of whoever is left aboard
            }
        }
        return riders;
    }

    /** Puts everybody aboard safely on the ground beside the carriage, shows it leaving and removes it. */
    public void dismiss() {
        if (level().isClientSide || isRemoved()) return;
        ejectRiders();
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.WIND_RING, position().add(0.0, 0.2, 0.0), position().add(0.0, 1.0, 0.0), 0xFFFFC247, 20, 1.6f);
            sl.playSound(null, blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
        discard();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return safeSpot(passenger); }

    /** A place for {@code rider} beside the carriage with room for the body and ground under it (the left, the right, behind, ahead), else on top of it. */
    private Vec3 safeSpot(Entity rider) {
        double yaw = Math.toRadians(getYRot());
        double[][] offsets = {{2.0, 0.0}, {-2.0, 0.0}, {0.0, -2.6}, {0.0, 2.6}, {1.4, -1.8}, {-1.4, -1.8}};          // {left, forward} in the carriage's frame
        for (double[] o : offsets) {
            double wx = getX() + o[0] * Math.cos(yaw) - o[1] * Math.sin(yaw);
            double wz = getZ() + o[0] * Math.sin(yaw) + o[1] * Math.cos(yaw);
            for (int dy : new int[]{0, 1, -1}) {
                double wy = getY() + dy;
                AABB box = rider.getBoundingBox().move(wx - rider.getX(), wy - rider.getY(), wz - rider.getZ());
                if (!level().noCollision(rider, box)) continue;
                if (level() instanceof ServerLevel sl ? Moves.groundBelow(sl, BlockPos.containing(wx, wy, wz)) : true) return new Vec3(wx, wy, wz);
            }
        }
        return new Vec3(getX(), getBoundingBox().maxY, getZ());
    }

    // ================================================================ save data (the type is not saved; this keeps the owner if that ever changes)
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = ownerUuid();
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) setOwner(tag.getUUID("Owner"));
    }

    // ================================================================ equipment: none
    @Override
    public Iterable<ItemStack> getArmorSlots() { return List.of(); }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    // ================================================================ spawning
    /** Makes the wielder's carriage at {@code spot} facing {@code yaw}, registers it (the caller has sent any older one away) and returns it, or null. */
    static CarriageEntity summon(ServerLevel level, ServerPlayer owner, CarriageNumbers.Chassis chassis, Vec3 spot, float yaw) {
        CarriageEntity e = CarriageEntities.CARRIAGE.get().create(level);
        if (e == null) return null;
        e.setOwner(owner.getUUID());
        e.configure(chassis);
        e.moveTo(spot.x, spot.y, spot.z, yaw, 0.0f);
        e.setYHeadRot(yaw);
        if (!level.addFreshEntity(e)) return null;
        CarriageRegistry.register(owner.getUUID(), e.getUUID());
        return e;
    }

    @Override
    public String toString() { return "CarriageEntity[owner=" + ownerUuid() + ", seats=" + seats + ", at=" + Mth.floor(getX()) + "," + Mth.floor(getY()) + "," + Mth.floor(getZ()) + "]"; }
}
