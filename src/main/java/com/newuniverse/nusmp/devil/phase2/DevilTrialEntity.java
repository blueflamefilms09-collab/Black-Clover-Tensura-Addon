package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.devil.DevilTier;
import com.newuniverse.nusmp.devil.WorldDevil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Why: a devil that fights back before it agrees (the trial of docs/devil_contract_phase2.md; Liebe fought Asta before their contract) is a boss-like entity that only the caller can hurt and that only
 * fights the caller. It is NOT a new boss framework: a plain {@link Monster} with melee, health and damage by the devil's rank (numbers in {@link #stats}), a vanilla boss bar for the caller, the devil's
 * name, and the glow of the vanilla glowing flag. It never saves, never drops, never despawns by itself and is removed by {@link Trials#check} (every second) when its caller is gone or dead, in another level, too far, or the trial
 * ran out of time. The result goes to {@link Trials#report}: IMPRESSED when its health falls to the impress share, WON when it dies.
 *
 * <p>Cost: at most one per player and {@code trial.max} (default 4) on the server, each living at most {@code trial.seconds} (default 150); vanilla mob AI for one target; one boss-bar update every 10 ticks;
 * no scan, no packet of its own.
 */
public final class DevilTrialEntity extends Monster {
    private UUID challenger;
    private String devil = "";
    private int result;
    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Trial"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public DevilTrialEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.ARMOR, 6);
    }

    /** Health, attack and armour by rank (the health also by the server's trial.healthScale). */
    public static double[] stats(DevilTier t, double healthScale) {
        double hp = switch (t == null ? DevilTier.LOW : t) { case SUPREME -> 900; case HIGH -> 400; case MID -> 160; default -> 60; };
        double dmg = switch (t == null ? DevilTier.LOW : t) { case SUPREME -> 14; case HIGH -> 10; case MID -> 7; default -> 4; };
        double armor = switch (t == null ? DevilTier.LOW : t) { case SUPREME -> 14; case HIGH -> 10; case MID -> 6; default -> 2; };
        return new double[] {hp * Math.max(0.1, Math.min(20.0, healthScale)), dmg, armor};
    }

    /** Sets the trial up for this caller and devil; called once by {@link Trials#start}. */
    void init(ServerPlayer caller, WorldDevil d, double healthScale) {
        this.challenger = caller.getUUID();
        this.devil = d.id();
        double[] s = stats(d.tier(), healthScale);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s[0]);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(s[1]);
        getAttribute(Attributes.ARMOR).setBaseValue(s[2]);
        setHealth((float) s[0]);
        setCustomName(Component.literal(d.name()));
        setCustomNameVisible(true);
        setGlowingTag(true);
        bar.setName(Component.literal(d.name() + ": the trial"));
        bar.addPlayer(caller);
        setTarget(caller);
    }

    public UUID challenger() { return challenger; }

    public String devilId() { return devil; }

    public int result() { return result; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, t -> t.getUUID().equals(challenger)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % 10 == 0) bar.setProgress(Math.max(0f, Math.min(1f, getHealth() / getMaxHealth())));
        if (tickCount % 20 == 0) Trials.check(this);
    }

    /** Only the caller (or the void and the like) can hurt it; the first time its health reaches the impress share the trial reports IMPRESSED. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        boolean caller = source.getEntity() instanceof ServerPlayer sp && sp.getUUID().equals(challenger);
        if (!caller && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive()) {
            int now = Phase2Rules.trialResult(result, getHealth() / Math.max(1f, getMaxHealth()), false, Phase2Config.impressShare());
            if (now > result) {
                result = now;
                Trials.report(this, now);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (!wasDead && this.dead && !level().isClientSide && result < Phase2Rules.TRIAL_WON) {
            result = Phase2Rules.TRIAL_WON;
            Trials.report(this, Phase2Rules.TRIAL_WON);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
}
