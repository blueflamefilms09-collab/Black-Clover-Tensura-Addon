package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.item.DemonSlayerSwordItem;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the passives of Sandalphon and the six conditions it puts on a creature act inside the game's own events (a hit, a heal, a teleport, a click, a skill press), so this is the one
 * subscriber, wired once from NUSMP with {@link #register}. It reads the player's {@link JudgmentState} mask (one int, no skill walk) and the effects; it holds no table of its own. Every handler returns
 * on its first line for a creature that has none of it. Server side only; a hit that is already one of the skill's own abilities ({@link JudgmentDamage#trueActive()}) is never counted a second time.
 *
 * <p>Rules (docs/veldanava_skills.md): Executioner's Authority: 20% critical (x1.5), bonus true damage from the target's armour, +1 Temporal Debt a hit, faster bow draw, and mastered a hit seals
 * the target's movement and spatial moves. Absolute Severance: armour, enchantments and potion effects do not reduce the hit, a shield does not block it, and the target cannot heal. Inevitable Demise:
 * damage x1 to x2 as the target's health falls, and mastered a target at or below 15% stutters and cannot heal. Temporal Debt: +1% magic damage taken a stack (at most +50%). Judgment Sigil:
 * a marked target at or below 20% health is executed. Future Rejection: no healing, teleporting or hit frames. Event Silence: no skill and no teleport. Movement Interference: no teleport.
 * Action Lock: no attack, use, interact or cast, and a mob drops its target.
 */
public final class JudgmentEvents {
    private JudgmentEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(JudgmentEvents::onIncomingDamage);
        bus.addListener(JudgmentEvents::onDamaged);
        bus.addListener(JudgmentEvents::onHeal);
        bus.addListener(JudgmentEvents::onTeleport);
        bus.addListener(JudgmentEvents::onShield);
        bus.addListener(JudgmentEvents::onChangeTarget);
        bus.addListener(JudgmentEvents::onAttack);
        bus.addListener(JudgmentEvents::onRightClickItem);
        bus.addListener(JudgmentEvents::onRightClickBlock);
        bus.addListener(JudgmentEvents::onEntityInteract);
        bus.addListener(JudgmentEvents::onLeftClickBlock);
        bus.addListener(JudgmentEvents::onUseStart);
        bus.addListener(JudgmentEvents::onUseTick);
        bus.addListener(JudgmentEvents::onDeath);
        bus.addListener(JudgmentEvents::onLogout);
        bus.addListener(JudgmentEvents::onServerTick);
        bus.addListener(JudgmentEvents::onServerStopping);
    }

    // ---------------------------------------------------------------- what counts as a strike
    /** A blow of the owner's own hand or projectile: it has a direct source, it is not magic and it is not one of the skill's own true hits. */
    private static boolean strike(DamageSource s) {
        return s.getDirectEntity() != null && !s.is(DemonSlayerSwordItem.SEVERANCE) && !AntiMagic.isMagic(s) && !JudgmentDamage.trueActive();
    }

    private static boolean serverSide(LivingEntity e) { return !e.level().isClientSide; }

    // ---------------------------------------------------------------- damage
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (!serverSide(victim) || !JudgmentConfig.enabled() || e.getAmount() <= 0f) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();

        if (attacker instanceof LivingEntity a && a != victim && !JudgmentDamage.trueActive() && JudgmentEffects.has(a, JudgmentEffects.ACTION_LOCK)) {      // an action-locked creature deals nothing
            e.setCanceled(true);
            return;
        }
        int debt = TemporalDebt.get(victim);
        if (debt > 0 && AntiMagic.isMagic(src)) e.setAmount((float) (e.getAmount() * JudgmentRules.magicVulnerability(debt)));

        if (!(attacker instanceof ServerPlayer p) || p == victim) return;
        int mask = JudgmentState.mask(p);
        if (mask == 0 || !strike(src)) return;

        float amount = e.getAmount();
        if (JudgmentState.on(p, 0) && p.getRandom().nextDouble() < JudgmentRules.CRIT_CHANCE) amount *= (float) JudgmentRules.CRIT_MULTIPLIER;
        if (JudgmentState.on(p, 2)) amount *= (float) JudgmentRules.demiseMultiplier(victim.getHealth() / Math.max(1f, victim.getMaxHealth()));
        e.setAmount(amount);
        if (JudgmentState.on(p, 1)) {                                                       // Absolute Severance: nothing takes anything off the hit
            e.addReductionModifier(DamageContainer.Reduction.ARMOR, (c, r) -> 0f);
            e.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (c, r) -> 0f);
            e.addReductionModifier(DamageContainer.Reduction.MOB_EFFECTS, (c, r) -> 0f);
        }
    }

    /** After a hit landed: the passives that need it to have landed, and the Judgment Sigil's execution. */
    static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (!serverSide(victim) || !JudgmentConfig.enabled()) return;
        if (JudgmentEffects.has(victim, JudgmentEffects.FUTURE_REJECTION)) victim.invulnerableTime = 0;      // the next blow always lands

        if (victim.isAlive() && JudgmentDamage.hasSigil(victim)) {
            ServerPlayer owner = JudgmentDamage.sigilOwner(victim.getServer(), victim);
            if (owner != null && JudgmentRules.atOrBelow(victim.getHealth(), victim.getMaxHealth(), JudgmentRules.EXECUTE_BELOW)) JudgmentDamage.execute(owner, victim);
        }

        if (!victim.isAlive() || !(e.getSource().getEntity() instanceof ServerPlayer p) || p == victim) return;
        if (JudgmentState.mask(p) == 0 || !strike(e.getSource())) return;
        if (JudgmentState.on(p, 0)) {
            TemporalDebt.add(victim, JudgmentRules.DEBT_PER_HIT);
            double pierce = JudgmentRules.armourPierce(victim.getArmorValue());
            if (pierce > 0.0) JudgmentDamage.raw(p, victim, (float) BalanceLaw.cap(victim, (float) pierce));
            if (JudgmentState.mastered(p) && victim.isAlive())
                victim.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, BalanceLaw.controlTicks(victim, JudgmentRules.SEAL_ON_HIT_TICKS), 0, false, true, true));
        }
        if (!victim.isAlive()) return;
        if (JudgmentState.on(p, 1)) {
            JudgmentDamage.markNoHeal(victim, JudgmentRules.NO_HEAL_HIT_TICKS);
            victim.removeEffect(MobEffects.REGENERATION);
        }
        if (JudgmentState.on(p, 2) && JudgmentState.mastered(p) && JudgmentRules.atOrBelow(victim.getHealth(), victim.getMaxHealth(), JudgmentRules.DEMISE_BELOW)) {
            victim.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, BalanceLaw.controlTicks(victim, 40), 0, false, true, true));
            JudgmentDamage.markNoHeal(victim, 40);
        }
    }

    static void onShield(LivingShieldBlockEvent e) {
        if (!serverSide(e.getEntity()) || !JudgmentConfig.enabled()) return;
        Entity a = e.getDamageSource().getEntity();
        if (a instanceof ServerPlayer p && JudgmentState.on(p, 1)) e.setBlocked(false);
        else if (JudgmentDamage.trueActive()) e.setBlocked(false);
    }

    // ---------------------------------------------------------------- healing, teleporting, targets
    static void onHeal(LivingHealEvent e) {
        LivingEntity who = e.getEntity();
        if (!serverSide(who)) return;
        if (JudgmentDamage.noHeal(who) || JudgmentEffects.has(who, JudgmentEffects.FUTURE_REJECTION)) e.setCanceled(true);
    }

    static void onTeleport(EntityTeleportEvent e) {
        if (!(e.getEntity() instanceof LivingEntity who) || !serverSide(who)) return;
        if (JudgmentEffects.has(who, JudgmentEffects.FUTURE_REJECTION) || JudgmentEffects.has(who, JudgmentEffects.MOVEMENT_INTERFERENCE)
                || JudgmentEffects.has(who, JudgmentEffects.EVENT_SILENCE)) e.setCanceled(true);
    }

    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (serverSide(e.getEntity()) && e.getNewAboutToBeSetTarget() != null && JudgmentEffects.has(e.getEntity(), JudgmentEffects.ACTION_LOCK)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- Action Lock: no attack, use, interact
    private static boolean locked(net.minecraft.world.entity.player.Player p) {
        return serverSide(p) && JudgmentEffects.has(p, JudgmentEffects.ACTION_LOCK);
    }

    static void onAttack(AttackEntityEvent e) { if (locked(e.getEntity())) e.setCanceled(true); }

    static void onRightClickItem(PlayerInteractEvent.RightClickItem e) { if (locked(e.getEntity())) e.setCanceled(true); }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) { if (locked(e.getEntity())) e.setCanceled(true); }

    static void onEntityInteract(PlayerInteractEvent.EntityInteract e) { if (locked(e.getEntity())) e.setCanceled(true); }

    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) { if (locked(e.getEntity())) e.setCanceled(true); }

    static void onUseStart(LivingEntityUseItemEvent.Start e) {
        LivingEntity who = e.getEntity();
        if (serverSide(who) && JudgmentEffects.has(who, JudgmentEffects.ACTION_LOCK)) e.setCanceled(true);
    }

    /** Executioner's Authority: a bow draws a third faster (one extra tick of draw every third tick). */
    static void onUseTick(LivingEntityUseItemEvent.Tick e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getItem().getItem() instanceof BowItem) || p.tickCount % 3 != 0) return;
        if (JudgmentConfig.enabled() && JudgmentState.on(p, 0)) e.setDuration(Math.max(1, e.getDuration() - 1));
    }

    // ---------------------------------------------------------------- the veto on skills (bound by NUSMP to ManasCore's skill events)
    /** Should this skill press, release or toggle be refused? True when the user carries Event Silence or Action Lock. */
    public static boolean vetoesSkill(ManasSkillInstance inst, LivingEntity user) {
        try {
            if (inst == null || user == null || user.level().isClientSide) return false;
            return JudgmentEffects.has(user, JudgmentEffects.EVENT_SILENCE) || JudgmentEffects.has(user, JudgmentEffects.ACTION_LOCK);
        } catch (RuntimeException failed) {
            return false;                                                            // a skill event must never break because of this: when in doubt the skill works
        }
    }

    // ---------------------------------------------------------------- housekeeping
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) JudgmentFields.onLogout(p.getUUID());
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) JudgmentFields.onLogout(p.getUUID());
    }

    static void onServerTick(ServerTickEvent.Post e) { JudgmentFields.onServerTick(e); }

    static void onServerStopping(ServerStoppingEvent e) { JudgmentFields.clearAll(); }
}
