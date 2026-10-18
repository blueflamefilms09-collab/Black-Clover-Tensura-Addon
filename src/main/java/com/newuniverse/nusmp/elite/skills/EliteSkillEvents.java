package com.newuniverse.nusmp.elite.skills;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.skills.evolution.KillTracker;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Why: ManasCore only ticks passive skills now and then and only calls a skill's damage hooks while it considers it active, so the parts of the elite skills that must not depend on that (the
 * once-a-second upkeep of toggles and domains, the memory of Ouroboros, the kill rewards, the damage multipliers of marks and tempers, the runes, the loot, the knock-back immunity, the Gaia block guard,
 * the Void Sovereign teleport rule) are driven from the plain NeoForge game-bus events below. Every handler returns at once when the master switch is off, only looks at players that own the
 * skill in question, and is wrapped so that a bug in one skill's rule can never crash a server tick. Registered by {@link EliteSkills#init}.
 */
public final class EliteSkillEvents {
    private EliteSkillEvents() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static void register(IEventBus game) {
        game.addListener(EliteSkillEvents::onPlayerTick);
        game.addListener(EliteSkillEvents::onServerTick);
        game.addListener(EliteSkillEvents::onDeath);
        game.addListener(EliteSkillEvents::onIncomingDamage);
        game.addListener(EliteSkillEvents::onDamagePost);
        game.addListener(EliteSkillEvents::onKnockback);
        game.addListener(EliteSkillEvents::onDrops);
        game.addListener(GaiaBlockGuard::onBreak);
        game.addListener(GaiaBlockGuard::onExplosion);
        game.addListener((EntityTeleportEvent.EnderPearl e) -> onTeleport(e));
        game.addListener((EntityTeleportEvent.EnderEntity e) -> onTeleport(e));
        game.addListener((EntityTeleportEvent.ChorusFruit e) -> onTeleport(e));
        game.addListener(EliteSkillsCommand::register);
        game.addListener(EliteSkillEvents::onLogin);
        game.addListener(EliteSkillEvents::onLogout);
        game.addListener(EliteSkillEvents::onRespawn);
        game.addListener(EliteSkillEvents::onServerStopped);
    }

    // ================================================================== the tick
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide || !EliteSkillsConfig.enabled()) return;
        int t = p.tickCount;
        boolean second = t % 20 == 5, half = t % 2 == 0;
        if (!second && !half) return;
        try {
            if (half) VoidSovereignSkill.domainTick(p);
            if (!second) return;
            for (ManasSkillInstance inst : SkillAPI.getSkillsFrom(p).getLearnedSkills()) {
                if (!(inst.getSkill() instanceof EliteSkill es)) continue;
                es.upkeep(inst, p);
                if (es instanceof OuroborosSkill) OuroborosSkill.sample(p);
            }
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] An elite skill upkeep failed for {}", p.getName().getString(), broken);
        }
    }

    static void onServerTick(ServerTickEvent.Post e) { EliteDefer.tick(); }

    // ================================================================== kills
    static void onDeath(LivingDeathEvent e) {
        if (!EliteSkillsConfig.enabled() || e.isCanceled() || !(e.getSource().getEntity() instanceof ServerPlayer killer) || killer == e.getEntity()) return;
        LivingEntity victim = e.getEntity();
        try {
            EliteUtil.instance(killer, EliteSkills.ANCIENT_SOUL).ifPresent(i -> AncientSoulSkill.onKill(i, killer, victim));
            EliteUtil.instance(killer, EliteSkills.BEELZEBUTH).ifPresent(i -> BeelzebuthSkill.onKill(i, killer, victim));
            EliteUtil.instance(killer, EliteSkills.GAIA).ifPresent(i -> GaiaSkill.onKill(i, killer, victim));
            KillTracker.onKill(killer, victim);
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] An elite kill reward failed", broken);
        }
    }

    // ================================================================== damage
    /** Multipliers on a blow BY a player: Beelzebuth's Hunger, a tempered weapon, a Mechanical Eye or Rapheal mark. */
    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!EliteSkillsConfig.enabled() || e.getAmount() <= 0f || !(e.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == e.getEntity()) return;
        try {
            LivingEntity victim = e.getEntity();
            double mult = 1.0;
            var hunger = EliteUtil.instance(attacker, EliteSkills.BEELZEBUTH);
            if (hunger.isPresent()) mult *= BeelzebuthSkill.hungerMultiplier(hunger.get(), attacker);
            var eye = EliteUtil.instance(attacker, EliteSkills.MECHANICAL_EYE);
            if (eye.isPresent()) mult *= MechanicalEyeSkill.markMultiplier(eye.get(), attacker, victim);
            var rapheal = EliteUtil.instance(attacker, EliteSkills.RAPHEAL);
            if (rapheal.isPresent()) mult *= RaphealSkill.markMultiplier(rapheal.get(), attacker, victim);
            if (e.getSource().getDirectEntity() == attacker) mult *= HephaestusSkill.temperMultiplier(attacker);
            if (mult != 1.0) e.setAmount((float) (e.getAmount() * mult));
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] An elite damage multiplier failed", broken);
        }
    }

    /** Runes on the weapon of a melee hit. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        if (!EliteSkillsConfig.enabled() || !(e.getSource().getEntity() instanceof ServerPlayer attacker) || e.getSource().getDirectEntity() != attacker || attacker == e.getEntity()) return;
        try {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty()) RunesmithsInsightSkill.applyRune(attacker, e.getEntity(), weapon, e.getNewDamage());
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] An elite rune failed", broken);
        }
    }

    static void onKnockback(LivingKnockBackEvent e) {
        if (!EliteSkillsConfig.enabled() || !(e.getEntity() instanceof ServerPlayer p)) return;
        try {
            if (EliteUtil.instance(p, EliteSkills.IRON_RESOLVE).filter(i -> IronResolveSkill.knockbackImmune(i, p)).isPresent()) e.setCanceled(true);
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] Iron Resolve knock-back check failed", broken);
        }
    }

    // ================================================================== loot
    static void onDrops(LivingDropsEvent e) {
        if (!EliteSkillsConfig.enabled() || !(e.getSource().getEntity() instanceof ServerPlayer killer) || killer == e.getEntity()) return;
        try {
            EliteUtil.instance(killer, EliteSkills.SCAVENGER).ifPresent(i -> ScavengerSkill.duplicate(killer, e.getEntity(), e.getDrops(), i.isMastered(killer)));
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] Scavenger drop failed", broken);
        }
    }

    // ================================================================== Void Sovereign
    static void onTeleport(EntityTeleportEvent e) {
        if (!EliteSkillsConfig.enabled() || !EliteSkillsConfig.get(EliteSkillsConfig.VOID_NULLIFY_TELEPORT) || e.getEntity().level().isClientSide) return;
        try {
            ServerPlayer owner = VoidSovereignSkill.nullifiesTeleport(e.getEntity().getServer(), e.getEntity(), e.getPrev(), e.getTarget());
            if (owner == null) return;
            e.setCanceled(true);
            if (e.getEntity() instanceof Player p && p instanceof ServerPlayer sp) {
                EliteBridge.actionbar(sp, SkillMsg.t("void_sovereign.nullified", "Space refuses to move for you.").withStyle(ChatFormatting.DARK_PURPLE));
            }
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] Void Sovereign teleport check failed", broken);
        }
    }

    // ================================================================== lifecycle
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ResetLedger.load(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        GaiaZone.remove(p.getUUID());
        GaiaWorld.forget(p.getUUID());
        OuroborosSkill.forget(p.getUUID());
        VoidSovereignSkill.forget(p.getUUID());
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        OuroborosSkill.forget(p.getUUID());                                  // the memory of the life that ended is gone
        GaiaZone.remove(p.getUUID());
        VoidSovereignSkill.forget(p.getUUID());
    }

    static void onServerStopped(ServerStoppedEvent e) {
        GaiaZone.clear();
        GaiaWorld.clearAll();
        OuroborosSkill.clearAll();
        VoidSovereignSkill.clearAll();
        EliteDefer.clear();
        ResetLedger.clearCache();
    }
}
