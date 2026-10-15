package com.newuniverse.nusmp.boss.rules;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: wires the boss rules to the game in one call ({@link #register()}, once from the mod constructor). Four listeners:
 * <ul>
 *   <li><b>Heal</b>: {@code LivingHealEvent} on a boss in combat is cancelled and zeroed ({@link BossHealGuard}), which stops the passive
 *       regeneration, drain heals and every mod's potion at the one door they all use.</li>
 *   <li><b>Skill veto</b>: ManasCore's activation, release and toggle events are cancelled when a boss tries a forbidden skill (the
 *       same pattern SpellSeal uses for players). A skill given by another mod still cannot be used.</li>
 *   <li><b>Sweep</b>: a boss that joins a level is cleaned at once, and every 100 ticks each tracked boss is cleaned again
 *       (forbidden skills, and the forbidden permanent effects the old engine applied), so a boss saved under the old design is fixed on
 *       load and nothing can be slipped in later. Only the handful of live bosses are walked; nothing scans the world.</li>
 * </ul>
 * Everything is a no-op when the matching switch in {@link BossRulesConfig} is off, which is how the old behaviour stays reachable.
 */
public final class BossRulesHooks {
    private BossRulesHooks() {}

    public static final int SWEEP_TICKS = 100;

    private static boolean done;
    private static final Map<UUID, LivingEntity> TRACKED = new HashMap<>();

    public static void register() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(BossRulesHooks::onHeal);
        NeoForge.EVENT_BUS.addListener(BossRulesHooks::onJoin);
        NeoForge.EVENT_BUS.addListener(BossRulesHooks::onLeave);
        NeoForge.EVENT_BUS.addListener(BossRulesHooks::onTick);
        NeoForge.EVENT_BUS.addListener(BossRulesHooks::onStopping);
        SkillEvents.ACTIVATE_SKILL.register((skill, user, key, mode) -> BossSkillRules.vetoes(skill.get(), user) ? EventResult.interruptFalse() : EventResult.pass());
        SkillEvents.RELEASE_SKILL.register((skill, user, key, mode, held) -> BossSkillRules.vetoes(skill.get(), user) ? EventResult.interruptFalse() : EventResult.pass());
        SkillEvents.TOGGLE_SKILL.register((skill, user) -> BossSkillRules.vetoes(skill.get(), user) ? EventResult.interruptFalse() : EventResult.pass());
    }

    static void onHeal(LivingHealEvent e) {
        if (!BossHealGuard.blocks(e.getEntity())) return;
        e.setAmount(0f);
        e.setCanceled(true);
    }

    static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        if (!(ent instanceof LivingEntity le) || !BossIdentity.isBoss(ent)) return;
        TRACKED.put(ent.getUUID(), le);
        clean(le);
    }

    static void onLeave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide()) TRACKED.remove(e.getEntity().getUUID());
    }

    static void onStopping(ServerStoppingEvent e) { TRACKED.clear(); }

    static void onTick(ServerTickEvent.Post e) {
        if (TRACKED.isEmpty() || e.getServer().getTickCount() % SWEEP_TICKS != 0) return;
        for (LivingEntity b : new ArrayList<>(TRACKED.values())) {
            if (b.isRemoved() || !b.isAlive()) { TRACKED.remove(b.getUUID(), b); continue; }
            clean(b);
        }
    }

    /** One boss: forbidden skills off, forbidden permanent effects off (only the engine's own infinite ambient ones). */
    private static void clean(LivingEntity b) {
        BossSkillRules.sweep(b);
        for (MobEffectInstance i : new ArrayList<>(b.getActiveEffects())) {
            if (!i.isInfiniteDuration() || !i.isAmbient()) continue;
            var key = i.getEffect().unwrapKey();
            if (key.isPresent() && BossSkillRules.forbiddenEffect(key.get().location().toString())) b.removeEffect(i.getEffect());
        }
    }

    /** For tests and the status command: how many bosses the sweep is tracking. */
    public static int tracked() { return TRACKED.size(); }
}
