package com.newuniverse.nusmp.quests;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the one door between the game code and the advancement / questline system. A one-line {@code NusmpEvents.fire(player, "key")} at the
 * right spot of a feature counts the event for that player and lets any advancement criterion {@code nusmp:event} that waits for the key
 * (see {@link NusmpEventTrigger}) complete. Advancements in turn feed the FTB Quests "advancement" tasks, so this door also drives the
 * questline.
 *
 * <p>Safety: {@code fire} does nothing for null, fake or client players, hops to the server thread when called from another thread, and
 * swallows every exception (logged once), so a hook can never break the feature it sits in. Counters live in the player's persistent data
 * ({@value #COUNTS_TAG}), are bumped for {@code key} and for {@code key|arg}, and are copied when the player entity is cloned.
 *
 * <p>The lead calls {@link #init(IEventBus)} once from the mod constructor, before registries freeze.
 */
public final class NusmpEvents {
    private NusmpEvents() {}

    public static final String MODID = "nusmp";
    /** Name of the compound inside the player's persistent data that holds the counters. */
    public static final String COUNTS_TAG = "nusmp_quest_counts";

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, MODID);
    /** The trigger type {@code nusmp:event}. */
    public static final DeferredHolder<CriterionTrigger<?>, NusmpEventTrigger> EVENT = TRIGGERS.register("event", NusmpEventTrigger::new);

    /** Registers the trigger type and the clone listener. Call from the mod constructor (before registries freeze). */
    public static void init(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(NusmpEvents::onClone);
    }

    // ---------------------------------------------------------------- firing
    /** An event without an argument. */
    public static void fire(ServerPlayer player, String key) {
        fire(player, key, null);
    }

    /**
     * Counts the event ({@code key} and, with an argument, {@code key|arg}) and runs the player's {@code nusmp:event} criteria.
     * Safe to call anywhere on the server; a no-op for null, fake or client-side players.
     */
    public static void fire(ServerPlayer player, String key, String arg) {
        try {
            if (!real(player) || key == null || key.isEmpty()) return;
            MinecraftServer server = player.getServer();
            if (!server.isSameThread()) { server.execute(() -> fire(player, key, arg)); return; }
            Map<String, Integer> counts = load(player);
            EventCounters.increment(counts, key, arg);
            save(player, counts);
            deliver(player, key, arg, counts);
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    /** Like {@link #fire(ServerPlayer, String, String)} but only the first time for this exact key and argument (state-like events polled each tick). */
    public static void fireOnce(ServerPlayer player, String key, String arg) {
        try {
            if (!real(player) || key == null || key.isEmpty()) return;
            if (EventCounters.read(load(player), key, arg) > 0) return;
        } catch (Throwable t) {
            warnOnce(t);
            return;
        }
        fire(player, key, arg);
    }

    public static void fireOnce(ServerPlayer player, String key) {
        fireOnce(player, key, null);
    }

    /**
     * Reports the event for the player and, when {@code quests.shareSquadCredit} is on, for the online members of their FTB team who are
     * within the configured radius, so a squad boss kill counts for the whole squad. Without FTB Teams it is plain {@code fire}.
     */
    public static void fireTeam(ServerPlayer player, String key, String arg) {
        fire(player, key, arg);
        try {
            if (!real(player) || !QuestsConfig.shareSquadCredit()) return;
            for (ServerPlayer m : QuestTeams.nearbyMembers(player, QuestsConfig.shareRadius())) fire(m, key, arg);
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    /** Fires one event for each of the given players (a boss kill credits everyone who took part), each with the team sharing of {@link #fireTeam}. */
    public static void fireAll(Iterable<ServerPlayer> players, String key, String arg) {
        List<ServerPlayer> done = new ArrayList<>();
        for (ServerPlayer p : players) {
            if (p == null || done.contains(p)) continue;
            done.add(p);
            fire(p, key, arg);
        }
        try {
            if (!QuestsConfig.shareSquadCredit()) return;
            for (ServerPlayer p : new ArrayList<>(done)) {
                if (!real(p)) continue;
                for (ServerPlayer m : QuestTeams.nearbyMembers(p, QuestsConfig.shareRadius())) {
                    if (done.contains(m)) continue;
                    done.add(m);
                    fire(m, key, arg);
                }
            }
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    /** How many times this player has had the event (key alone, or key with exact argument). */
    public static int count(ServerPlayer player, String key, String arg) {
        try { return real(player) ? EventCounters.read(load(player), key, arg) : 0; } catch (Throwable t) { warnOnce(t); return 0; }
    }

    private static void deliver(ServerPlayer player, String key, String arg, Map<String, Integer> counts) {
        EVENT.get().fire(player, inst -> {
            String instArg = inst.arg().orElse(null);
            return TriggerRule.matches(inst.key(), instArg, inst.count(), key, arg, EventCounters.read(counts, inst.key(), instArg));
        });
    }

    // ---------------------------------------------------------------- future bosses
    /**
     * Grants the placeholder advancement {@code nusmp:future/<arc>_<boss>} (see {@link FutureBossIds}) to the player. A boss that has just
     * been built calls this from its death hook for the players who took part; until then the questline shows "Awaiting the boss".
     *
     * @return true when the advancement exists and was granted now or already earlier
     */
    public static boolean futureBoss(ServerPlayer player, String arc, String boss) {
        try {
            if (!real(player)) return false;
            MinecraftServer server = player.getServer();
            AdvancementHolder holder = server.getAdvancements().get(ResourceLocation.parse(FutureBossIds.id(arc, boss)));
            if (holder == null) return false;
            for (String criterion : player.getAdvancements().getOrStartProgress(holder).getRemainingCriteria()) {
                player.getAdvancements().award(holder, criterion);
            }
            return true;
        } catch (Throwable t) {
            warnOnce(t);
            return false;
        }
    }

    // ---------------------------------------------------------------- storage
    static boolean real(ServerPlayer p) {
        return p != null && !(p instanceof FakePlayer) && p.getServer() != null && !p.level().isClientSide;
    }

    static Map<String, Integer> load(ServerPlayer p) {
        Map<String, Integer> out = new HashMap<>();
        CompoundTag tag = p.getPersistentData().getCompound(COUNTS_TAG);
        for (String k : tag.getAllKeys()) out.put(k, tag.getInt(k));
        return out;
    }

    static void save(ServerPlayer p, Map<String, Integer> counts) {
        CompoundTag tag = new CompoundTag();
        counts.forEach(tag::putInt);
        p.getPersistentData().put(COUNTS_TAG, tag);
    }

    private static void onClone(PlayerEvent.Clone e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer now)) return;
            CompoundTag old = e.getOriginal().getPersistentData();
            if (old.contains(COUNTS_TAG, Tag.TAG_COMPOUND)) now.getPersistentData().put(COUNTS_TAG, old.getCompound(COUNTS_TAG).copy());
        } catch (Throwable t) {
            warnOnce(t);
        }
    }

    private static void warnOnce(Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] quest event hook failed (further failures are not logged)", t);
    }
}
