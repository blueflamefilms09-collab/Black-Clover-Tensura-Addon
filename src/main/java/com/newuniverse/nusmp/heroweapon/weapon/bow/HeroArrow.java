package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner asked that the Legendary Bow's arrows be real arrows ("regular Minecraft arrows, just reskinned") that hurt twice, physically and with magic,
 * and carry a magic effect of the bow form. This is a small mod entity that EXTENDS the vanilla {@link AbstractArrow} (and is listed in the
 * {@code minecraft:arrows} entity-type tag), so gravity, drag, water inertia, sticking in blocks, the hit sounds, block collision, being bounced by a raised
 * shield and every other mod that treats arrows as arrows work exactly as for a vanilla arrow; only the damage and the end of life are the bow's. A vanilla
 * {@code minecraft:arrow} with persistent data was the alternative: it would have saved a registration and a renderer, but the vanilla arrow cannot be given
 * its own look, is saved with the world, is pick-up-able by default and would be changed by every arrow mod; a mod type avoids all of that (see
 * docs/heroweapon/bow_arrows.md).
 * <p>
 * It is never saved ({@code noSave}), never picked up (pickup is DISALLOWED and {@code tryPickup} is refused) and drops nothing. It lives at most
 * {@link #maxLife} ticks and two seconds after sticking, and is removed as soon as its owner dies, logs out or changes world (a check every five ticks, no
 * world scan). The per-wielder and global caps are {@link ArrowBudget}'s; a place is taken when it is fired and given back whenever it is removed. All the
 * rules (who may be hit, the damage, the effect) run on the server; the client only moves and draws it.
 */
public class HeroArrow extends AbstractArrow {
    /** What the client needs to draw it: the number of the bow form's look ({@link BowArrowForms#index}). */
    private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(HeroArrow.class, EntityDataSerializers.INT);

    // ---- server-side state (never saved, never sent)
    private HeroCtx ctx;
    private ArrowKind kind = ArrowKind.LIGHT;
    private double raw, magicShare, pierceFactor = BowNumbers.PIERCE_FACTOR;
    private boolean volley, child, eagle, reserved, bounced, impacted;
    private int pierceLeft, maxLife = 120, linger = 40, stuckTicks;
    private LivingEntity seek;
    private final List<Integer> hitIds = new ArrayList<>(4);
    /** What the last strike dealt, for the self-test (the raw numbers after the balance law). */
    float lastPhysical = -1f, lastMagic = -1f;

    public HeroArrow(EntityType<? extends HeroArrow> type, Level level) {
        super(type, level);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    // ================================================================ set up (server)
    void configure(HeroCtx c, ArrowKind k, int look, double rawDamage, double share, boolean isVolley, boolean isChild) {
        this.ctx = c;
        this.kind = k;
        this.raw = rawDamage;
        this.magicShare = share;
        this.volley = isVolley;
        this.child = isChild;
        this.reserved = true;
        this.entityData.set(LOOK, look);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
        if (isVolley) this.linger = 0;
    }

    void setPierce(int n, double factor) { this.pierceLeft = Math.max(0, Math.min(6, n)); this.pierceFactor = factor; }

    void setLife(int ticks) { this.maxLife = Math.max(5, Math.min(120, ticks)); }

    void setSeek(LivingEntity prey) { this.seek = prey; }

    void setEagle(boolean on) { this.eagle = on; }

    ArrowKind kind() { return kind; }

    boolean isVolley() { return volley; }

    boolean isChild() { return child; }

    double raw() { return raw; }

    double magicShare() { return magicShare; }

    HeroCtx ctx() { return ctx; }

    public int lookIndex() { return this.entityData.get(LOOK); }

    /** Gives the place in the budget back (once). */
    void releaseBudget() {
        if (!reserved) return;
        reserved = false;
        if (ctx != null) ArrowBudget.release(ctx.player.getUUID());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOOK, 0);
    }

    @Override
    protected ItemStack getDefaultPickupItem() { return ItemStack.EMPTY; }

    // ================================================================ never picked up
    @Override
    protected boolean tryPickup(Player player) { return false; }

    @Override
    public void playerTouch(Player player) {
        // a hero arrow is made of magicules: nothing to pick up, nothing to drop
    }

    // ================================================================ life
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isRemoved()) return;
        if (ctx == null || tickCount > maxLife) {                                    // a /summoned or unconfigured arrow is never kept
            discard();
            return;
        }
        if (inGround) {
            if (++stuckTicks >= linger) {
                discard();
                return;
            }
        } else if (seek != null && tickCount % 2 == 0) {
            steer();
        }
        if (tickCount % 5 == 0 && !ownerLive()) discard();
    }

    private boolean ownerLive() {
        ServerPlayer p = ctx.player;
        return p.isAlive() && !p.hasDisconnected() && p.serverLevel() == level();
    }

    /** The seeker bends a little towards its prey every other tick (no search: the prey was chosen when the string was let go). */
    private void steer() {
        if (!seek.isAlive() || seek.level() != level()) {
            seek = null;
            setNoGravity(false);
            return;
        }
        Vec3 m = getDeltaMovement();
        double speed = m.length();
        if (speed < 0.1) return;
        Vec3 want = seek.getBoundingBox().getCenter().subtract(position());
        if (want.lengthSqr() < 1.0E-6) return;
        setDeltaMovement(m.normalize().scale(0.6).add(want.normalize().scale(0.4)).normalize().scale(speed));
        hasImpulse = true;
    }

    @Override
    public void onRemovedFromLevel() {
        releaseBudget();
        super.onRemovedFromLevel();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        releaseBudget();
        super.remove(reason);
    }

    // ================================================================ who it may hit
    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e)) return false;
        if (ctx == null) return true;                                                 // the client: the server decides, this only stops the picture at a body
        return e instanceof LivingEntity le && Foes.valid(ctx, le) && !hitIds.contains(e.getId());
    }

    @Override
    public byte getPierceLevel() { return (byte) Math.max(0, Math.min(6, pierceLeft)); }

    // ================================================================ the hit
    @Override
    protected void onHitEntity(EntityHitResult r) {
        if (level().isClientSide() || ctx == null || !(r.getEntity() instanceof LivingEntity t)) return;
        try {
            strike(t, r.getLocation());
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow arrow strike", e);
            discard();
        }
    }

    /**
     * The two hits of one arrow on one foe (also called by the self-test): the physical hit as an arrow of its owner (armour, shields and arrow protection
     * apply), then the magic hit as indirect magic of its owner (the magic damage type). Both go through the balance law, so the boss caps hold. A blocked
     * physical hit bounces the arrow off, as a vanilla arrow bounces off a raised shield, and no magic follows.
     */
    void strike(LivingEntity t, Vec3 at) {
        ServerPlayer owner = ctx.player;
        hitIds.add(t.getId());
        double r = raw;
        if (eagle && t.distanceTo(owner) > BowNumbers.EAGLE_EYE_DISTANCE) r = Math.min(BowNumbers.BASIC_MAX_RAW, r * BowNumbers.EAGLE_EYE_BONUS);
        boolean landed = true;
        lastPhysical = lastMagic = 0f;
        if (r > 0.0) {
            float d = BalanceLaw.damage(owner, t, (float) (ArrowSplit.physical(r) * ctx.power), ctx.masteryShare());
            landed = t.hurt(damageSources().arrow(this, owner), d);
            if (landed) lastPhysical = d;
        }
        if (!landed) {
            bounce();
            return;
        }
        double m = ArrowSplit.magic(r, magicShare, HeroConfig.bowMagicShareScale());
        if (m > 0.0 && t.isAlive()) {
            float d = BalanceLaw.damage(owner, t, (float) (m * ctx.power), ctx.masteryShare());
            t.invulnerableTime = 0;                                                  // the first blow set the hurt immunity; this second blow of the SAME arrow must count
            if (t.hurt(damageSources().indirectMagic(this, owner), d)) lastMagic = d;
        }
        playSound(SoundEvents.ARROW_HIT, 1.0f, 1.2f / (random.nextFloat() * 0.2f + 0.9f));
        if (t instanceof Player && owner != t) owner.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0f));
        ArrowImpact.onHit(this, ctx, t);
        BowShots.reward(ctx, List.of(t));
        if (pierceLeft > 0) {                                                        // flies on through the foe
            pierceLeft--;
            raw = r * pierceFactor;
            return;
        }
        finish(at);
    }

    private void bounce() {
        bounced = true;
        impacted = true;                                                             // a blocked arrow does nothing more
        setDeltaMovement(getDeltaMovement().scale(-0.1));
        setYRot(getYRot() + 180.0f);
        yRotO += 180.0f;
        setNoGravity(false);
        maxLife = Math.min(maxLife, tickCount + 20);
        playSound(SoundEvents.SHIELD_BLOCK, 0.6f, 1.4f);
    }

    boolean bounced() { return bounced; }

    @Override
    protected void onHitBlock(BlockHitResult r) {
        super.onHitBlock(r);
        if (level().isClientSide() || ctx == null) return;
        try {
            impact(r.getLocation());
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow arrow impact", e);
        }
        if (volley) discard();
    }

    private void finish(Vec3 at) {
        impact(at);
        discard();
    }

    /** The end of the arrow's flight at {@code at} (once): the effects that happen where it lands. */
    private void impact(Vec3 at) {
        if (impacted) return;
        impacted = true;
        ArrowImpact.impact(this, ctx, at);
    }
}
