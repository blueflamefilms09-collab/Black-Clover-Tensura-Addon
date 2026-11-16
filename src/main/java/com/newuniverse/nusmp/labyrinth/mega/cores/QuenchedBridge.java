package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Ascendant armour is meant to speed up progression, and the other mod ("Tensura No Index", mod id {@code tensuranoindex}) has two more progression numbers: Telesma and Malice. This repo
 * has no readable jar or API of that mod (docs/guide_fixes_p2.md), so the bridge touches no class of it: it is DATA-DRIVEN. The SERVER settings {@code bossCores.quenched.telesmaCommand} /
 * {@code maliceCommand} hold the command that mod answers to (for example one that adds Telesma to a player), with {@code {player}} and {@code {amount}} filled in; the amount is the Ascendant's EP gain
 * x {@code telesmaPerEp} / {@code malicePerEp} (rules in {@link QuenchedRules}). Everything is off until the owner fills it in (bridge off, commands blank, factors 0). A server without the other mod,
 * or with a different version of it, can never crash because of this class; a wrong command is only a refused command in the console.
 * Bounded: at most two commands per Ascendant kill, at most one payment per player per {@link QuenchedRules#MIN_INTERVAL_TICKS} ticks, one small map keyed by player UUID (dropped at logout / death / stop).
 */
public final class QuenchedBridge {
    private QuenchedBridge() {}

    public static final String MOD_ID = "tensuranoindex";
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** Is the other mod loaded? Never throws. */
    public static boolean present() {
        try { return ModList.get() != null && ModList.get().isLoaded(MOD_ID); } catch (RuntimeException e) { return false; }
    }

    /** Called after an Ascendant kill added {@code epGain} base EP: pays Telesma and Malice through the owner's commands when the bridge is on, the mod is loaded and a command is set. */
    public static void onAscendantGain(ServerPlayer p, double epGain) {
        if (!BossCoreConfig.quenchedBridge() || !present() || p.getServer() == null) return;
        long now = p.level().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (!QuenchedRules.due(last == null ? -1L : last, now)) return;
        long telesma = QuenchedRules.amount(epGain, BossCoreConfig.telesmaPerEp(), BossCoreConfig.quenchedMaxPerKill());
        long malice = QuenchedRules.amount(epGain, BossCoreConfig.malicePerEp(), BossCoreConfig.quenchedMaxPerKill());
        String name = p.getGameProfile().getName();
        String a = QuenchedRules.render(BossCoreConfig.telesmaCommand(), name, telesma);
        String b = QuenchedRules.render(BossCoreConfig.maliceCommand(), name, malice);
        if (a == null && b == null) return;
        LAST.put(p.getUUID(), now);
        run(p, a);
        run(p, b);
    }

    private static void run(ServerPlayer p, String command) {
        if (command == null) return;
        try {
            p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput().withPermission(2), command);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[anime_multiverse] Boss Cores: the Telesma / Malice command was refused: {}", e.toString());
        }
    }

    /** Logout, death and server stop: no state outlives the player. */
    public static void forget(UUID id) { LAST.remove(id); }

    public static void clear() { LAST.clear(); }
}
