package com.newuniverse.nusmp.undine;

import com.newuniverse.nusmp.audio.voice.Voice;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Why: the one place where the Undine SPEAKS (docs/magic/undine.md, "Voice lines"). A moment of her life ({@link UndineLines.Group}) becomes a spoken line through the project's voice system
 * ({@link Voice#say}: its limiter, its {@code voice.maxPerSecond} option, its client switch, its packet), after two limits of her own ({@link UndineSpeechBudget}): one cooldown per owner and group, and at most
 * ONE spoken line per listener every four seconds, whoever's Undine says it. Only players within {@link UndineSpeechBudget#RADIUS} (32 blocks) of her are told, nobody else gets a packet. A group with no
 * recorded line (the file was never generated) is not silent: her sung hum ({@link UndineSoundSet.Cue#HUM}) stands in, through the sound budget. All on the server; state per player UUID.
 */
public final class UndineSpeech {
    private UndineSpeech() {}

    static final UndineSpeechBudget BUDGET = new UndineSpeechBudget();
    private static long lastPrune;

    /**
     * Says a line of the group from a point (her mouth) for the owner's Undine. {@code counter} picks the line (round and round through the group). Returns how many players were sent a line (a hum counts as
     * one when it plays).
     */
    public static int say(ServerPlayer owner, ServerLevel sl, double x, double y, double z, UndineLines.Group g, int counter, boolean full) {
        if (g == null || owner == null || !UndineConfig.enabled() || !UndineConfig.speech()) return 0;
        long now = sl.getGameTime();
        if (now - lastPrune > 2400) { lastPrune = now; BUDGET.prune(now); }
        if (!BUDGET.ownerMay(owner.getUUID(), g, now)) return 0;
        String id = UndineLines.pick(g, counter);
        if (id == null) return UndineAudio.play(sl, owner.getUUID(), x, y, z, UndineSoundSet.Cue.HUM, full) ? 1 : 0;      // no recorded line: her sung hum
        double r2 = UndineSpeechBudget.RADIUS * UndineSpeechBudget.RADIUS;
        int sent = 0;
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || p.distanceToSqr(x, y, z) > r2 || !BUDGET.listenerMay(p.getUUID(), now)) continue;
            if (Voice.say(p, id, g.cooldown())) sent++;
        }
        return sent;
    }

    /** A player left: forget their limits. */
    public static void forget(UUID player) { BUDGET.forget(player); }

    public static void clearAll() { BUDGET.clear(); }
}
