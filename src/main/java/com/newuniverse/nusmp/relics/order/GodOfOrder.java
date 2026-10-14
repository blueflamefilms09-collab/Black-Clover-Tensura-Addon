package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.relics.order.OrderPlan.Phase;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The God of Order: a tall, masked arbiter in white and gold with a halo of ledger glyphs and a long measuring staff. It comes when the {@link
 * UniqueLedger} finds a second copy of a relic, and takes the extra copy; it fights only the one who raises a hand against it. It is a {@link
 * ZogratisBoss} (the fight framework: the percentage power system it calls "authority", the move scheduler, the telegraphs, the damage law, the boss
 * bar), with a devil lent to it only for the framework's colours: gold. It has no loot and gives experience only.
 * <ul>
 *   <li><b>A visit</b> ({@link OrderDirector}, {@link OrderPlan}): it appears, reads its verdict, burns the duplicate with a beam, hands over the
 *       Notice of Order and bows. While it reads and while it burns it cannot be hurt; a blow then is remembered and answered afterwards.</li>
 *   <li><b>The fight</b> ({@link OrderMoves}): Decree of Binding (a ring of glyphs that slows), Writ of Light (a line of light), Circle of Judgement
 *       (a marked circle, one heavy hit) and Erasure, which takes the duplicates of everyone who fought it and ends the visit. After 75 s of fighting,
 *       or when it is badly hurt, it casts Erasure by itself.</li>
 *   <li>A God that belongs to no visit (a spawn egg, a summon) is a free God: it only fights what hits it and leaves after ten quiet minutes.</li>
 * </ul>
 */
public class GodOfOrder extends ZogratisBoss {
    public static final String TAG = "nusmp_god_of_order";

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GodOfOrder.class, EntityDataSerializers.INT);
    private static final Phase[] PHASES = Phase.values();

    /** The marks the telegraphs lock (package-private: {@link OrderMoves} writes them in the wind-up and reads them when the move runs). */
    Vec3 mark, markDir;
    boolean erased;
    private UUID caseId, provoker;
    private final Set<UUID> attackers = new HashSet<>();
    private int freeTicks;
    private long combatStart, lastQuip;
    private boolean erasing;

    public GodOfOrder(EntityType<GodOfOrder> type, Level level) {
        super(type, level);
        addTag(TAG);
        this.xpReward = OrderTable.XP;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(OrderTable.HEALTH, OrderTable.ATTACK, OrderTable.SPEED, OrderTable.ARMOR, OrderTable.TOUGHNESS);
    }

    // ================================================================ identity
    @Override public String bossName() { return OrderText.NAME; }

    /** A framework boss must have a devil; gold is the colour of the Order (nothing the God shows or says names a devil). */
    @Override public Devil devil() { return Devil.ASTAROTH; }

    @Override protected BossEvent.BossBarColor barColor() { return BossEvent.BossBarColor.YELLOW; }

    @Override protected ChatFormatting chatColor() { return ChatFormatting.GOLD; }

    @Override public String barTitle() { return bossName() + " - Authority " + power() + "%" + (gatesOpen() ? " - Open ledger" : ""); }

    @Override protected float formScale(int form) { return 1.0f; }

    @Override protected float meleeDamage() { return (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.6f; }

    /** A judge keeps its distance: six to twelve blocks. */
    @Override protected double preferredRange(double suggested) { return Math.max(6, Math.min(12, suggested)); }

    // ---- its voice
    @Override protected String openingLine() { return OrderText.provoked(getRandom().nextInt(8)); }

    @Override protected String powerLine(int old, int now) { return OrderText.power(old, now); }

    @Override protected String gatesLine() { return OrderText.gates(); }

    @Override protected String deathLine() { return OrderText.death(); }

    /** The framework's own announcement of the Gates names a Tree of Qliphoth: it is dropped, the God's line follows it. */
    @Override
    public void announce(String text) {
        if (text != null && !text.contains("Qliphoth") && !text.contains("Triad")) super.announce(text);
    }

    // ================================================================ state for the client
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, Phase.COMBAT.ordinal());
    }

    /** Which part of its visit it is in (COMBAT for a free God). */
    public Phase orderPhase() {
        int i = entityData.get(PHASE);
        return i >= 0 && i < PHASES.length ? PHASES[i] : Phase.COMBAT;
    }

    void setOrderPhase(Phase p) {
        if (entityData.get(PHASE) != p.ordinal()) entityData.set(PHASE, p.ordinal());
    }

    /** True while it cannot be hurt: the reading, the beam, and the first second after it appeared. */
    public boolean invulnerableNow() {
        Phase p = orderPhase();
        return caseId != null && (p == Phase.READING || p == Phase.BEAM || tickCount < OrderPlan.GRACE_TICKS);
    }

    /** The visit this God belongs to, or null (a free God). */
    UUID caseId() { return caseId; }

    void bind(UUID id) { caseId = id; }

    /** The player who first struck it (never a creative one), or null. */
    UUID provokedBy() { return provoker; }

    /** Where the beam leaves the staff. */
    Vec3 staffTip() {
        Vec3 f = getLookAngle().multiply(1, 0, 1);
        f = f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
        return position().add(0, 2.9, 0).add(f.scale(1.1));
    }

    void beginCombat(ServerPlayer target) {
        if (target != null && target.isAlive() && !target.isCreative() && !target.isSpectator()) setTarget(target);
    }

    /** Players who struck it (the Erasure takes duplicates from these and nobody else). */
    Set<UUID> attackers() { return attackers; }

    // ================================================================ goals
    /** It does not hunt: the framework's goal that picks the nearest player is taken out, so it fights only what hits it. */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal);
    }

    // ================================================================ damage
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) {
            if (provoker == null) provoker = p.getUUID();
            attackers.add(p.getUUID());
        }
        if (!level().isClientSide && invulnerableNow() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    // ================================================================ the fight and the visit, every tick
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 20 == 0) {
            if (caseId != null) {
                if (!OrderDirector.knows(caseId)) {                                   // a God from an earlier session, or whose visit has ended
                    OrderFx.leave(sl, position());
                    discard();
                    return;
                }
            } else {
                freeTicks = fightActive() ? 0 : freeTicks + 20;
                if (freeTicks >= OrderTable.FREE_LIFETIME_TICKS) {
                    OrderFx.leave(sl, position());
                    discard();
                    return;
                }
            }
        }
        if (fightActive()) {
            if (combatStart == 0L) combatStart = Math.max(1, tickCount);
            if (!erasing && OrderTable.forcedErasureDue(tickCount - combatStart, getHealth() / getMaxHealth(), gatesOpen())) startErasure();
        }
    }

    /** The long way to Erasure: its words, a minute-long tell, and then the move itself. */
    void startErasure() {
        if (erasing || erased) return;
        erasing = true;
        say(OrderText.erasure());
        for (int t = 0; t < OrderTable.ERASURE_TELL_TICKS; t += 6) {
            final int left = OrderTable.ERASURE_TELL_TICKS - t;
            later(t, () -> OrderMoves.tellErasure(this, left, OrderTable.ERASURE_TELL_TICKS));
        }
        later(OrderTable.ERASURE_TELL_TICKS, () -> OrderMoves.erasure(this));
    }

    /** One of the move's lines now and then (at most every eight seconds). */
    void quip(String moveId) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(OrderText.quip(moveId, getRandom().nextInt(8)));
    }

    @Override
    protected List<Move> moves() { return OrderMoves.build(this); }

    @Override
    protected void onFightStart() { combatStart = Math.max(1, tickCount); }

    /** The Open Ledger (the 100% mode): the halo, the grid and the sigil flare, and the bar is full again (the framework's refill). */
    @Override
    protected void onGatesOpened() { OrderVfx.gates(this); }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) OrderDirector.godDied(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) OrderDirector.godRemoved(this);
        super.remove(reason);
    }

    @Override protected Component getTypeName() { return Component.translatable("entity.nusmp.god_of_order"); }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (caseId != null) tag.putUUID("OrderCase", caseId);
        tag.putInt("OrderFree", freeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OrderCase")) caseId = tag.getUUID("OrderCase");
        freeTicks = tag.getInt("OrderFree");
        addTag(TAG);
    }
}
