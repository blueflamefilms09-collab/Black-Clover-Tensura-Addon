package com.newuniverse.nusmp.quests;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * Why: some quest events can be read from events the game already sends, so they need no edit inside the features at all: a boss dying
 * ({@code boss.defeated} and the floor, warden and peak variants) and the Convergence advancements being earned ({@code convergence.stage},
 * {@code convergence.anomaly}). Listening here keeps those features untouched.
 *
 * <p>Death is listened to at the lowest priority and cancelled deaths are not received, so Lucius's scripted stage changes (a cancelled fatal blow)
 * never count as a defeat; only the real death does.
 */
public final class QuestWorldEvents {
    private QuestWorldEvents() {}

    /** Persistent entity tag the Labyrinth's Peak Altar puts on its boss. */
    private static final String PEAK_BOSS_TAG = "nusmp_peak_boss";

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, QuestWorldEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(QuestWorldEvents::onAdvancement);
        NeoForge.EVENT_BUS.addListener(QuestWorldEvents::onPlayerTick);
    }

    private static void onDeath(LivingDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            if (dead.level().isClientSide || dead instanceof Player) return;
            boolean peak = dead.getTags().contains(PEAK_BOSS_TAG);
            if (!peak && !BossIdentity.isBoss(dead)) return;
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).getPath();
            Iterable<ServerPlayer> who = QuestHooks.participants(dead, e.getSource());
            NusmpEvents.fireAll(who, "boss.defeated", path);
            String floor = switch (path) {
                case "goliath" -> "goliath";
                case "charybdis_spawn" -> "charybdis";
                default -> null;
            };
            if (floor != null) NusmpEvents.fireAll(who, "dungeon.floor_boss", floor);
            if (path.equals("floor_warden")) NusmpEvents.fireAll(who, "labyrinth.warden_defeated", null);
            if (peak) NusmpEvents.fireAll(who, "labyrinth.seal_opened", "boss");
        } catch (Throwable t) {
            // a quest hook must never disturb a boss death
        }
    }

    /** Once a second, inside the arena: remember if the player picked up or put on a relic (for the no-relic clear). */
    private static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 3
                    && p.level().dimension().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NusmpEvents.MODID, "zogratis_arena"))) QuestHooks.arenaWatch(p);
        } catch (Throwable t) {
            // ignore
        }
    }

    /** The Convergence data pack grants {@code nusmp:convergence/*} advancements; each one is mirrored as an event. */
    private static void onAdvancement(AdvancementEvent.AdvancementEarnEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            String path = e.getAdvancement().id().getPath();
            if (!e.getAdvancement().id().getNamespace().equals(NusmpEvents.MODID) || !path.startsWith("convergence/")) return;
            String name = path.substring("convergence/".length());
            if (name.startsWith("stage_")) NusmpEvents.fire(p, "convergence.stage", name.substring("stage_".length()));
            else NusmpEvents.fire(p, "convergence.anomaly", name);
        } catch (Throwable t) {
            // ignore
        }
    }
}
