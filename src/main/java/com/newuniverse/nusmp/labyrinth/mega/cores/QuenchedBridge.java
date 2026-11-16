package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Why: the Ascendant armour is meant to speed up progression, and the other mod ("Tensura No Index", mod id {@code tensuranoindex}) has two more progression numbers: Telesma and Malice. This repo has
 * no verified API for either (the compat jar and data pack only reference items and entities, see docs/compat/README.md), so this class is an honest STUB: it can tell whether the mod is loaded and it
 * has the one place where a Telesma/Malice gain would be scaled, but it changes nothing. Nothing here touches a class of the other mod, so a server without it (or with a different version of it) can
 * never crash because of this class. When the API is checked against the real jar, only {@link #onAscendantGain} has to be filled in.
 */
public final class QuenchedBridge {
    private QuenchedBridge() {}

    public static final String MOD_ID = "tensuranoindex";
    private static boolean told;

    /** Is the other mod loaded? Never throws. */
    public static boolean present() {
        try { return ModList.get() != null && ModList.get().isLoaded(MOD_ID); } catch (RuntimeException e) { return false; }
    }

    /** Called after an Ascendant kill added {@code epGain} base EP. Does nothing yet (Telesma and Malice are not wired); says so once in the log when the other mod is present. */
    public static void onAscendantGain(ServerPlayer p, double epGain) {
        if (told || !present()) return;
        told = true;
        LogUtils.getLogger().info("[anime_multiverse] Boss Cores: Tensura No Index is loaded, but Telesma and Malice are not scaled by the Ascendant armour (its API is not verified); only EP is.");
    }
}
