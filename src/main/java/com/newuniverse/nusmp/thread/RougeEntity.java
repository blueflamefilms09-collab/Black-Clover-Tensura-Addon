package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Why: in the anime Rouge is not a buff, she is a small red cat of thread that is <i>there</i>: she sits on Vanessa's shoulder, bolts
 * across the battlefield to whoever is about to be hit, taps them, and the blow never happens. A bare status effect cannot be seen,
 * cannot hurry to the right person and cannot be pointed at, so she is an entity. She is never saved (she belongs to a cast, not to
 * the world), cannot be hurt by anything but /kill and the void, is not pushed, hit, targeted or picked, and drops nothing. She lives
 * only as long as her caster does: she unravels when the caster dies, logs out, leaves the dimension, runs out of mana, casts the page
 * again, or when the time set by {@code thread.rougeMinutes} is up.
 * <p>
 * The brain is three vanilla goals on one controller: {@code WatchAlliesGoal} (no movement; scans every 4 ticks, ranks the protected
 * allies with {@link ThreatMath} and names the most endangered), {@code RunToEndangeredGoal} (sprint, leap, or hop through thread to
 * that ally) and {@code FollowCasterGoal} (scurry near the caster, hop back past 16 blocks, settle on the shoulder when idle). The fate
 * rewrite itself is decided in {@link RougeFateEvents}; this class only plays the tap ({@link #tap}) and the unravelling.
 * <p>
 * The synced {@code STATE} drives the client animation ({@code client.thread.RougeModel}); the client counts the age of a state itself.
 */
public class RougeEntity extends PathfinderMob {
    public static final int SIT = 0, SCURRY = 1, LEAP = 2, TAP = 3, UNRAVEL = 4, REKNIT = 5, HOP = 6;
    /** 1.46.0: resting on the caster's head (appended last so no number moves). The head-cat layer draws her once settled, see {@link RougePerch}. */
    public static final int HEAD = 7;
    public static final int TAP_TICKS = 6, REKNIT_TICKS = 12, UNRAVEL_TICKS = 16;
    public static final int RED = 0xFFFF3355;

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(RougeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(RougeEntity.class, EntityDataSerializers.INT);

    // ---- server side
    private long lifeEnd;
    private int stateTicks;
    @Nullable private UUID endangered;
    private double endangeredScore;
    private int calmTicks;
    private int cacheAt = -1000;
    private final Set<UUID> allies = new HashSet<>(), party = new HashSet<>();
    @Nullable private UUID tapTarget;
    private boolean unravelMessage;
    // ---- client side
    private int stateAge;

    public RougeEntity(EntityType<? extends RougeEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, Optional.empty());
        b.define(STATE, REKNIT);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WatchAlliesGoal(this));
        this.goalSelector.addGoal(1, new RunToEndangeredGoal(this));
        this.goalSelector.addGoal(2, new FollowCasterGoal(this));
    }

    // ================================================================ identity and rules of the body
    public void setOwner(ServerPlayer p) { this.entityData.set(OWNER, Optional.of(p.getUUID())); }
    @Nullable public UUID ownerId() { return this.entityData.get(OWNER).orElse(null); }
    public int state() { return this.entityData.get(STATE); }
    /** Client: ticks since the state last changed (for animation). */
    public int stateAge() { return stateAge; }
    public void setLifetime(long untilGameTime) { this.lifeEnd = untilGameTime; }

    private void setState(int s) {
        if (state() != s) { this.entityData.set(STATE, s); stateTicks = 0; }
    }

    public boolean busy() { int s = state(); return s == TAP || s == UNRAVEL || s == REKNIT; }

    /** Nothing hurts her but /kill and the void. */
    @Override public boolean isInvulnerableTo(DamageSource source) { return !(source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)); }
    @Override public boolean isPushable() { return false; }
    /**
     * She is never in the way: arrows and the crosshair pass through her. The one exception is a client whose player holds a name tag, so a
     * name tag can be used on her (the server never needs this: interaction packets do not ask it).
     */
    @Override public boolean isPickable() { return level().isClientSide && RougeRest.namingHand.getAsBoolean(); }
    @Override public boolean canBeHitByProjectile() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override protected void doPush(Entity other) {}
    @Override public void push(double x, double y, double z) {}
    @Override public boolean attackable() { return false; }
    @Override public boolean canBeSeenAsEnemy() { return false; }
    @Override public boolean canBeSeenByAnyone() { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean isAlliedTo(Entity other) { return other.getUUID().equals(ownerId()) || super.isAlliedTo(other); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) stateAge = 0;
    }

    @Override
    public void remove(RemovalReason reason) {
        UUID o = ownerId();
        if (level().isClientSide) RougeRest.clear(o, this);
        if (o != null) RougeManager.forget(o, this);
        super.remove(reason);
    }

    // ================================================================ the name (1.46.0)
    /**
     * A name tag used on her. Only the caster may name her (anyone else's tag is swallowed so it cannot rename her through the vanilla path);
     * the name is shown above her, and is kept on the caster's thread grimoire so the next cast gives it back. Sneaking with an empty hand opens nothing.
     */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(net.minecraft.world.item.Items.NAME_TAG)) return super.mobInteract(player, hand);
        net.minecraft.network.chat.Component tagName = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        String clean = tagName == null ? null : RougePerch.cleanName(tagName.getString());
        if (clean == null) return net.minecraft.world.InteractionResult.PASS;                    // a blank tag does nothing, as everywhere
        if (level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !sp.getUUID().equals(ownerId())) return net.minecraft.world.InteractionResult.CONSUME;
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        rename(sp, clean);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    /** Names her now and remembers the name on the caster's grimoire. */
    public void rename(ServerPlayer caster, String name) {
        applyName(name);
        RougeManager.threadBook(caster).ifPresent(i -> { i.getOrCreateTag().putString(RougePerch.NAME_TAG, name); i.markDirty(); });
        caster.displayClientMessage(Component.literal("Rouge answers to " + name + " now.").withStyle(net.minecraft.ChatFormatting.RED), true);
        if (level() instanceof ServerLevel sl) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.9f);
    }

    /** Sets the shown name without touching the grimoire (used when a recast gives the remembered name back). */
    public void applyName(String name) {
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
    }

    // ================================================================ the owner and the protected set
    @Nullable
    public ServerPlayer owner() {
        UUID id = ownerId();
        if (id == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(id);
    }

    /** Refreshes the cached whitelist and party every two seconds (and when asked) so the damage event does only lookups. */
    public void refreshCaches(ServerPlayer p) {
        allies.clear();
        allies.addAll(RougeManager.allies(p));
        party.clear();
        party.addAll(com.newuniverse.nusmp.multiverse.Squads.partyMembers(p));
        cacheAt = tickCount;
    }

    /** Is this living thing under Rouge's watch? (Pure lookups; see {@link ProtectedSet} for the rule.) */
    public boolean isProtecting(LivingEntity e) {
        UUID oid = ownerId();
        if (oid == null || e == this || !e.isAlive()) return false;
        boolean isCaster = e.getUUID().equals(oid);
        boolean pet = e instanceof OwnableEntity o && oid.equals(o.getOwnerUUID());
        ServerPlayer caster = owner();
        boolean mate = e instanceof ServerPlayer && (party.contains(e.getUUID()) || caster != null && e.isAlliedTo(caster));
        boolean attacking = e instanceof Mob m && m.getTarget() != null && oid.equals(m.getTarget().getUUID());
        return ProtectedSet.isProtected(isCaster, allies.contains(e.getUUID()), pet, mate, attacking, e instanceof RougeEntity);
    }

    // ================================================================ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            stateAge++;
            clientTrail();
            // the head-cat layer asks RougeRest; she is listed only while settled on the head so the layer and the body never both draw
            UUID co = ownerId();
            if (co != null) {
                if (RougePerch.headCatDrawn(state() == HEAD, stateAge)) RougeRest.put(co, this); else RougeRest.clear(co, this);
            }
            return;
        }
        stateTicks++;
        ServerPlayer p = owner();
        if (state() != UNRAVEL) {
            if (p == null || !p.isAlive() || p.level() != level() || level().getGameTime() >= lifeEnd) { beginUnravel(null); }
            else if (tickCount % 20 == 0 && RougeManager.mana(p) <= 0.0) beginUnravel(p);
        }
        if (p != null && tickCount - cacheAt >= 40) refreshCaches(p);
        switch (state()) {
            case REKNIT -> {
                if (stateTicks >= REKNIT_TICKS) {
                    // after a tap she runs back to the caster first; only when she is already close does she settle (no flying across the field)
                    if (p != null && distanceToSqr(p) > 3.0 * 3.0) setState(SCURRY); else setState(ThreadConfig.rougeOnHead() ? HEAD : SIT);
                }
            }
            case TAP -> tickTap();
            case UNRAVEL -> { if (stateTicks >= UNRAVEL_TICKS) discard(); }
            case HOP -> { if (stateTicks >= 4) setState(SCURRY); }
            case LEAP -> { if (onGround() && stateTicks > 3) setState(SCURRY); }
            case SIT -> { if (p != null) glueToShoulder(p); }
            case HEAD -> { if (p != null) glueToHead(p); }
            default -> { }
        }
        if (state() != SIT && state() != HEAD) setNoGravity(false);
    }

    /**
     * 1.46.0: resting ON the caster's head (the old head cat's place). The entity is held at the top of the head, eased in so the hop up reads as
     * a hop; the client stops drawing her body after {@link RougePerch#SETTLE_TICKS} and the head-cat layer (same placement, scale and sit pose as
     * before) takes over, so this entity only supplies position, name plate and interaction there.
     */
    private void glueToHead(ServerPlayer p) {
        setNoGravity(true);
        Vec3 spot = new Vec3(p.getX(), p.getY() + RougePerch.headY(p.isCrouching()), p.getZ());
        Vec3 now = position(), next = now.add(spot.subtract(now).scale(stateTicks < RougePerch.SETTLE_TICKS ? 0.4 : 0.9));
        setPos(next.x, next.y, next.z);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        setYRot(p.yBodyRot);
        yBodyRot = p.yBodyRot;
        yHeadRot = p.yHeadRot;
        fallDistance = 0;
    }

    /** Sitting on the caster's right shoulder: eased toward the spot so the hop up reads as a hop. */
    private void glueToShoulder(ServerPlayer p) {
        setNoGravity(true);
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 spot = p.position().add(-Math.cos(yaw) * 0.38, p.getBbHeight() * (p.isCrouching() ? 0.86 : 0.97), -Math.sin(yaw) * 0.38);
        Vec3 now = position(), next = now.add(spot.subtract(now).scale(stateTicks < 6 ? 0.35 : 0.8));
        setPos(next.x, next.y, next.z);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        setYRot(p.yBodyRot);
        yBodyRot = p.yBodyRot;
        yHeadRot = p.yHeadRot;
        fallDistance = 0;
    }

    private void clientTrail() {
        int s = state();
        if (s == SCURRY || s == LEAP || s == HOP) {
            if (random.nextInt(2) == 0) level().addParticle(new DustParticleOptions(new Vector3f(1f, 0.2f, 0.33f), 0.7f), getX() + (random.nextDouble() - 0.5) * 0.2,
                    getY() + 0.15, getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
        } else if (s == UNRAVEL || s == REKNIT) {
            for (int i = 0; i < 2; i++) level().addParticle(new DustParticleOptions(new Vector3f(1f, 0.25f, 0.4f), 0.9f), getX() + (random.nextDouble() - 0.5) * 0.4,
                    getY() + 0.1 + random.nextDouble() * 0.4, getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
    }

    // ================================================================ actions
    /** Plays the tap on 'ally' (hops next to them if far), faces them, and flashes the thread. Called by the fate event. */
    public void tap(LivingEntity ally) {
        if (!(level() instanceof ServerLevel sl) || state() == UNRAVEL) return;
        tapTarget = ally.getUUID();
        if (distanceToSqr(ally) > 2.6 * 2.6) hopTo(ally.position(), 1.2);
        getNavigation().stop();
        setNoGravity(false);
        setState(TAP);
        faceTowards(ally);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.7f);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.9f);
        VfxSpawn.sendFollowing(sl, VfxShape.ROUGE_FATE_TAP, ally, ally.position().add(0, ally.getBbHeight() * 0.5, 0), RED, 30, 1.0f);
        endangered = null;
    }

    private void tickTap() {
        Entity t = tapTarget == null || !(level() instanceof ServerLevel sl) ? null : sl.getEntity(tapTarget);
        if (t instanceof LivingEntity le) faceTowards(le);
        if (stateTicks >= TAP_TICKS) { tapTarget = null; setState(REKNIT); }
    }

    /** Short-hop through thread to a safe spot next to 'near' (a thread line is drawn between the two places). */
    public boolean hopTo(Vec3 near, double radius) {
        if (!(level() instanceof ServerLevel sl)) return false;
        Vec3 spot = SafeSpot.firstOk(sl, this, SafeSpot.ringAround(near, radius, random.nextDouble() * Math.PI * 2));
        if (spot == null) return false;
        Vec3 from = position();
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, from.add(0, 0.2, 0), spot.add(0, 0.2, 0), RED, 8, 0.8f);
        moveTo(spot.x, spot.y, spot.z, getYRot(), 0f);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        setNoGravity(false);
        fallDistance = 0;
        if (!busy()) setState(HOP);
        return true;
    }

    private void faceTowards(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw;
    }

    /** Begins coming apart (the caster is gone, out of mana, or has called her again). 'p' gets the message if given. */
    public void beginUnravel(@Nullable ServerPlayer p) {
        if (state() == UNRAVEL || !(level() instanceof ServerLevel sl)) return;
        setNoGravity(false);
        getNavigation().stop();
        setState(UNRAVEL);
        if (p != null && !unravelMessage) {
            unravelMessage = true;
            p.displayClientMessage(Component.literal("Rouge has no strength left to weave. She unravels.").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        VfxSpawn.send(sl, VfxShape.ROUGE_FATE_TAP, position().add(0, 0.2, 0), position().add(0, 0.5, 0), RED, 30, 3.0f);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    // ================================================================ what the goals read and write
    @Nullable
    LivingEntity endangeredAlly() {
        if (endangered == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(endangered) instanceof LivingEntity le && le.isAlive() && isProtecting(le) ? le : null;
    }

    void setEndangered(@Nullable UUID id, double score) {
        if (id == null) { if (endangered != null && ++calmTicks >= 6) { endangered = null; endangeredScore = 0; calmTicks = 0; } return; }
        calmTicks = 0;
        endangered = id;
        endangeredScore = score;
    }

    public double endangeredScore() { return endangeredScore; }
    int stateTicks() { return stateTicks; }
    void markScurry() { if (!busy() && state() != SCURRY && state() != LEAP && state() != HOP) setState(SCURRY); }
    void markLeap() { if (!busy()) setState(LEAP); }
    void settle() {
        int rest = ThreadConfig.rougeOnHead() ? HEAD : SIT;
        if (!busy() && state() != rest) setState(rest);
    }

    // ================================================================ goals
    /** Scans every 4 ticks for the protected ally nearest to harm. Moves nothing. */
    static final class WatchAlliesGoal extends Goal {
        private final RougeEntity cat;
        WatchAlliesGoal(RougeEntity cat) { this.cat = cat; setFlags(EnumSet.noneOf(Flag.class)); }

        @Override public boolean canUse() { return !cat.level().isClientSide && cat.state() != UNRAVEL && cat.owner() != null; }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (cat.tickCount % 4 != 0) return;
            ServerPlayer owner = cat.owner();
            if (owner == null || !(cat.level() instanceof ServerLevel sl)) return;
            AABB zone = cat.getBoundingBox().inflate(14.0);
            List<LivingEntity> guarded = new ArrayList<>();
            if (owner.level() == sl) guarded.add(owner);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, zone, e -> e != owner && cat.isProtecting(e))) guarded.add(e);
            if (guarded.isEmpty()) { cat.setEndangered(null, 0); return; }
            List<Mob> mobs = sl.getEntitiesOfClass(Mob.class, zone.inflate(10.0), m -> m.isAlive() && m != cat && m.getTarget() != null);
            List<Projectile> shots = sl.getEntitiesOfClass(Projectile.class, zone.inflate(6.0), pr -> !pr.isRemoved() && pr.getDeltaMovement().lengthSqr() > 0.01
                    && !(pr.getOwner() instanceof LivingEntity o && (cat.isProtecting(o) || o == owner)));
            LivingEntity best = null;
            double bestScore = 0;
            for (LivingEntity a : guarded) {
                boolean hostile = false, adjacent = false, boss = false;
                for (Mob m : mobs) if (m.getTarget() == a) {
                    double d = m.distanceTo(a);
                    if (d < 10.0) hostile = true;
                    if (d < 3.5) adjacent = true;
                    if (BossIdentity.isBoss(m)) boss = true;
                }
                int eta = -1;
                Vec3 c = a.getBoundingBox().getCenter();
                for (Projectile pr : shots) {
                    Vec3 v = pr.getDeltaMovement(), rel = c.subtract(pr.position());
                    double vv = v.lengthSqr(), t = rel.dot(v) / vv;
                    if (t < 0 || t > ThreatMath.PROJECTILE_HORIZON) continue;
                    double miss = c.distanceTo(pr.position().add(v.scale(t)));
                    if (miss > a.getBbWidth() * 0.5 + 1.0) continue;
                    int e = ThreatMath.projectileEta(rel.length(), rel.normalize().dot(v));
                    if (e >= 0 && (eta < 0 || e < eta)) eta = e;
                }
                boolean hurt = a.tickCount - a.getLastHurtByMobTimestamp() < 60 && a.getLastHurtByMobTimestamp() > 0;
                double score = ThreatMath.score(new ThreatMath.Threat(a.getHealth() / Math.max(1f, a.getMaxHealth()), hostile, hurt, eta, adjacent, boss));
                if (score > bestScore) { bestScore = score; best = a; }
            }
            if (best != null && bestScore >= ThreatMath.RUN_THRESHOLD) cat.setEndangered(best.getUUID(), bestScore);
            else cat.setEndangered(null, 0);
        }
    }

    /** Sprints and leaps to the most endangered ally, or hops through thread when they are far. */
    static final class RunToEndangeredGoal extends Goal {
        private final RougeEntity cat;
        private int leapCooldown;
        RunToEndangeredGoal(RougeEntity cat) { this.cat = cat; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }

        @Override public boolean canUse() { return !cat.busy() && cat.endangeredAlly() != null; }
        @Override public boolean canContinueToUse() { return !cat.busy() && cat.endangeredAlly() != null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void start() { cat.setNoGravity(false); cat.markScurry(); }

        @Override
        public void tick() {
            LivingEntity ally = cat.endangeredAlly();
            if (ally == null) return;
            if (leapCooldown > 0) leapCooldown--;
            double dist = cat.distanceTo(ally);
            switch (ThreatMath.approach(dist)) {
                case ARRIVED -> { cat.getNavigation().stop(); cat.getLookControl().setLookAt(ally); }
                case HOP -> cat.hopTo(ally.position(), 1.3);
                case SPRINT -> {
                    cat.getNavigation().moveTo(ally, 2.4);
                    if (cat.onGround() && leapCooldown == 0 && dist > 2.2) {
                        Vec3 d = ally.position().subtract(cat.position());
                        Vec3 h = new Vec3(d.x, 0, d.z).normalize().scale(Math.min(0.85, 0.25 + dist * 0.1));
                        cat.setDeltaMovement(h.x, 0.42 + Math.max(0, d.y) * 0.05, h.z);
                        cat.markLeap();
                        leapCooldown = 8;
                    }
                }
            }
        }

        @Override public void stop() { cat.getNavigation().stop(); }
    }

    /** Scurries after the caster, hops back when left far behind, settles on the shoulder when there is nothing to do. */
    static final class FollowCasterGoal extends Goal {
        private final RougeEntity cat;
        private int idle;
        FollowCasterGoal(RougeEntity cat) { this.cat = cat; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { return !cat.busy() && cat.state() != SIT && cat.state() != HEAD && cat.owner() != null; }
        @Override public boolean canContinueToUse() { return canUse() && cat.endangeredAlly() == null; }
        @Override public void start() { idle = 0; cat.markScurry(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            ServerPlayer p = cat.owner();
            if (p == null) return;
            double dist = cat.distanceTo(p);
            if (ThreatMath.tooFarFromCaster(dist)) { cat.hopTo(p.position(), 1.6); idle = 0; return; }
            if (dist > 2.6) {
                idle = 0;
                cat.getNavigation().moveTo(p, dist > 6 ? 1.9 : 1.3);
                if (cat.horizontalCollision && cat.onGround()) { cat.getJumpControl().jump(); }
            } else {
                cat.getNavigation().stop();
                if (++idle > 12) cat.settle();
            }
        }
    }
}
