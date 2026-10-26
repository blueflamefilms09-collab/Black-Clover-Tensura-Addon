package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the passives of Jibril act inside the game's own events (an effect about to land, a blow about to land), so this is the one subscriber for them, wired once from NUSMP with {@link #register}. It reads
 * the player's {@link JudgmentState} mask (one int, no skill walk) through {@link JudgmentState#onFor}; every handler returns on its first line for a creature that is not an owner of Jibril and not
 * under one of its effects. Server side only. Rules (docs/veldanava_skills.md):
 * <ul>
 *   <li><b>Divine Rigor</b>: no harmful effect lands on the owner (a status debuff, a movement impairment, Temporal Stutter). Mastered: an ally within {@link JibrilRules#RIGOR_ALLY_RADIUS} blocks of the owner
 *       gets half of it, so the debuffs that land on them last half as long.</li>
 *   <li><b>Wings of Revelation</b>: a blow taken in the air is dodged with a chance of {@link JibrilRules#WINGS_DODGE_PERCENT}% (85% mastered); an ability's own true hit is never dodged.</li>
 *   <li><b>Word of Law</b>: while the parry window is open, the first blow with a living source is negated and comes back at 300%.</li>
 *   <li><b>Kingdom of God</b>: inside a standing sanctuary the owner and the owner's allies cannot be reduced below 1 HP (the last handler of the blow, so nothing raises it afterwards).</li>
 * </ul>
 * The hotbar of a commanded player is held every tick while a lock exists; the wings are settled at login; everything is dropped at death, logout and server stop.
 */
public final class JibrilEvents {
    private JibrilEvents() {}

    private static final int CODE = JibrilRules.CODE;

    /** True while Divine Rigor re-applies a debuff at half length (the re-application passes through the same event). */
    private static boolean halving;

    public static void register(IEventBus bus) {
        bus.addListener(JibrilEvents::onEffect);
        bus.addListener(JibrilEvents::onIncoming);
        bus.addListener(EventPriority.LOWEST, JibrilEvents::onIncomingLast);
        bus.addListener(EventPriority.LOWEST, JibrilEvents::onDamagePre);
        bus.addListener(JibrilEvents::onDeath);
        bus.addListener(JibrilEvents::onLogout);
        bus.addListener(JibrilEvents::onLogin);
        bus.addListener(JibrilEvents::onServerTick);
        bus.addListener(JibrilEvents::onServerStopping);
    }

    private static boolean serverSide(LivingEntity e) { return !e.level().isClientSide; }

    // ---------------------------------------------------------------- Divine Rigor
    static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled()) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (inst == null || inst.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;
        if (JudgmentState.onFor(p, CODE, 0)) {
            e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (halving || inst.getDuration() <= 1 || !JibrilState.rigorCovers(p)) return;
        e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        halving = true;
        try {
            p.addEffect(new MobEffectInstance(inst.getEffect(), JibrilRules.rigorAllyDuration(inst.getDuration()), inst.getAmplifier(), inst.isAmbient(), inst.isVisible(), inst.showIcon()));
        } finally {
            halving = false;
        }
    }

    // ---------------------------------------------------------------- Word of Law and the wings
    private static boolean parryable(DamageSource src) {
        return !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !JudgmentDamage.trueActive();
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || e.getAmount() <= 0f || JudgmentState.skill(p) != CODE) return;
        DamageSource src = e.getSource();
        if (!parryable(src)) return;
        if (JibrilState.parrying(p) && src.getEntity() != null) {
            e.setCanceled(true);
            JibrilState.closeParry(p);
            if (src.getEntity() instanceof LivingEntity attacker && attacker != p && attacker.isAlive()) {
                double back = Math.min(JudgmentConfig.activeCap(), JibrilRules.reflected(e.getAmount()));
                JudgmentDamage.raw(p, attacker, BalanceLaw.cap(attacker, (float) back));
                JibrilFx.reflect(p.serverLevel(), p, attacker);
            }
            return;
        }
        if (JudgmentState.onFor(p, CODE, 3) && JibrilState.airborne(p) && (src.getDirectEntity() != null || src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_PROJECTILE))
                && JibrilRules.dodges(p.getRandom().nextInt(100), JudgmentState.mastered(p))) {
            e.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- Kingdom of God
    /** The last look at the blow: inside a standing Kingdom an ally keeps at least 1 HP (health plus absorption minus 1 is the most one blow can take). */
    static void onIncomingLast(LivingIncomingDamageEvent e) {
        LivingEntity who = e.getEntity();
        if (e.isCanceled() || !serverSide(who) || e.getAmount() <= 0f || !JudgmentConfig.enabled() || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !JibrilState.kingdomCovers(who)) return;
        float left = (float) JibrilRules.kingdomLimited(e.getAmount(), who.getHealth(), who.getAbsorptionAmount());
        if (left <= 0f) e.setCanceled(true);
        else if (left < e.getAmount()) e.setAmount(left);
    }

    /** The same, after the armour and the other mods have had their say, counting health only (stricter, since the absorption may or may not have been taken off by now). */
    static void onDamagePre(LivingDamageEvent.Pre e) {
        LivingEntity who = e.getEntity();
        if (!serverSide(who) || e.getNewDamage() <= 0f || !JudgmentConfig.enabled() || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !JibrilState.kingdomCovers(who)) return;
        float left = (float) JibrilRules.kingdomLimited(e.getNewDamage(), who.getHealth(), 0.0);
        if (left < e.getNewDamage()) e.setNewDamage(left);
    }

    // ---------------------------------------------------------------- housekeeping
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) JibrilState.leave(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) JibrilState.leave(p);
    }

    /** A login: the flight of the wings is given or taken back to match the toggle (the player's abilities are saved with them). */
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && (JibrilState.wingsGiven(p) || JudgmentState.onFor(p, CODE, 3))) JibrilState.applyWings(p, JudgmentState.onFor(p, CODE, 3) && JudgmentConfig.enabled());
    }

    static void onServerTick(ServerTickEvent.Post e) { JibrilState.tickHotbar(e.getServer()); }

    static void onServerStopping(ServerStoppingEvent e) { JibrilState.clearAll(); }
}
