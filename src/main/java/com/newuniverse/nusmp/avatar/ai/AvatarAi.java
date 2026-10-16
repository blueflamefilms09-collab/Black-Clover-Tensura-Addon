package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.chatter.ChatterReloadListener;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the single door through which the Avatar's personality AI joins the creation system. {@link #install} is called by
 * {@code AvatarEntity.registerGoals} after the Avatar's own goals (movement, melee, follow) and adds this module's goals; {@link #registerEvents} is
 * called once by {@code AvatarSystem.init} with the game event bus. Both are safe to call on a client (they do nothing there).
 *
 * <p>Goal priorities (lower runs first): the Yandere intercept and jealousy 1, retreat 1, glutton / lazy 3, the watcher and the caster take no flags
 * so their priority only orders the checks. All goals test the personality live, because {@code registerGoals} runs inside the Mob constructor,
 * before the synced personality fields are set.
 */
public final class AvatarAi {
    private AvatarAi() {}

    private static final java.lang.reflect.Field GOALS = field("goalSelector"), TARGETS = field("targetSelector");

    private static java.lang.reflect.Field field(String name) {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.Mob.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    private static GoalSelector read(java.lang.reflect.Field f, AvatarEntity a) {
        try { return f == null ? null : (GoalSelector) f.get(a); } catch (ReflectiveOperationException | RuntimeException bad) { return null; }
    }

    /**
     * Adds this module's goals. Mob's goal selectors are protected, so this finds them by reflection (the same way {@code soul.PaladinGoals} does);
     * AvatarEntity can avoid that by calling {@link #install(AvatarEntity, GoalSelector, GoalSelector)} with its own two selectors.
     */
    public static void install(AvatarEntity avatar) {
        if (avatar == null || avatar.level().isClientSide) return;
        install(avatar, read(GOALS, avatar), read(TARGETS, avatar));
    }

    public static void install(AvatarEntity avatar, GoalSelector g, GoalSelector targets) {
        if (avatar == null || avatar.level().isClientSide || g == null || targets == null) return;
        AvatarMind mind = new AvatarMind();
        g.addGoal(0, new AvatarWatcherGoal(avatar, mind));
        g.addGoal(1, new YandereInterceptGoal(avatar));
        g.addGoal(1, new YandereJealousyTargetGoal(avatar));
        g.addGoal(1, new PersonalityGoals.Retreat(avatar, mind));
        g.addGoal(2, new AvatarCastSkillGoal(avatar));
        g.addGoal(3, new PersonalityGoals.LazyIdle(avatar, mind));
        g.addGoal(3, new PersonalityGoals.EatDroppedFood(avatar));
        targets.addGoal(3, new PersonalityGoals.DefendVillagers(avatar));
    }

    /** Registers the Avatar module's game-bus listeners (call once, from AvatarSystem.init). */
    public static void registerEvents(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGH, AvatarAiEvents::onIncoming);
        gameBus.addListener(AvatarAiEvents::onDamagePost);
        gameBus.addListener(AvatarAiEvents::onChangeTarget);
        gameBus.addListener(AvatarAiEvents::onLeave);
        gameBus.addListener(AvatarAiEvents::onEntityTick);
        gameBus.addListener(AvatarAiEvents::onServerStopped);
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new ChatterReloadListener()));
    }
}
