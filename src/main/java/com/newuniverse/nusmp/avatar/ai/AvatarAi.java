package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.chatter.ChatterReloadListener;
import com.newuniverse.nusmp.avatar.delve.AvatarDelveGoal;
import com.newuniverse.nusmp.avatar.evolution.AvatarEvolution;
import com.newuniverse.nusmp.avatar.inventory.AvatarInventoryEvents;
import com.newuniverse.nusmp.avatar.inventory.AvatarInventoryMenus;
import com.newuniverse.nusmp.avatar.inventory.AvatarOrdersCommand;
import com.newuniverse.nusmp.avatar.party.AutonomousNamingGoal;
import com.newuniverse.nusmp.avatar.party.AvatarPartyManager;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the single door through which the Avatar's AI joins the creation system. {@link #install} is called by {@code AvatarEntity.registerGoals} after the Avatar's
 * own goals (movement, melee, follow) and adds this module's goals; {@link #registerEvents} is called once by {@code AvatarSystem.init} with the game event bus. Both
 * are safe to call on a client (they do nothing there). Since the Avatar rebuild this door also wires the parts of the rebuild that need the game's events: the
 * inventory window (its menu type is registered on the mod bus from here, so no line has to be added to the mod constructor), the sneak-click that opens it, the
 * creator's {@code /avatar} command, the party's deaths, the Avatar's EP from kills and the graves of dead players.
 *
 * <p>Goal priorities (lower runs first): the watcher 0, the Yandere intercept and jealousy 1, retreat 1; the weapon goals (bow and crossbow, mage, shield) and the
 * caster 2 next to the melee goal the entity adds; looting, glutton / lazy and the autonomous naming 3; the dungeon trip 4 next to following. The watcher, the shield
 * and the caster take no movement flag so their priority only orders the checks. All goals test the personality live, because {@code registerGoals} runs inside the Mob
 * constructor, before the synced personality fields are set.
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
     * AvatarEntity can avoid that by calling {@link #install(AvatarEntity, GoalSelector, GoalSelector)} with its own two selectors, which it now does.
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
        g.addGoal(2, new AvatarRangedGoal(avatar));
        g.addGoal(2, new AvatarMageGoal(avatar));
        g.addGoal(2, new AvatarShieldGoal(avatar));
        g.addGoal(2, new AvatarCastSkillGoal(avatar, mind));
        g.addGoal(3, new AvatarLootGoal(avatar));
        g.addGoal(3, new PersonalityGoals.LazyIdle(avatar, mind));
        g.addGoal(3, new PersonalityGoals.EatDroppedFood(avatar));
        g.addGoal(3, new AutonomousNamingGoal(avatar));
        g.addGoal(4, new AvatarDelveGoal(avatar));
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
        // the Avatar rebuild
        gameBus.addListener(EventPriority.HIGH, AvatarInventoryEvents::onInteract);           // ahead of the relic system's take-back: sneak + empty hand opens the inventory
        gameBus.addListener((RegisterCommandsEvent e) -> AvatarOrdersCommand.register(e.getDispatcher()));
        gameBus.addListener(AvatarPartyManager::onDeath);
        gameBus.addListener(AvatarEvolution::onKill);
        gameBus.addListener(Graves::onDeath);
        gameBus.addListener(Graves::onServerStopped);
        AvatarInventoryMenus.init();                                                           // the menu type goes on the mod bus of the mod being constructed
    }
}
