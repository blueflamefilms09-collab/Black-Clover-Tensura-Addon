package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.audio.voice.Voice;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: the one place where Shishou SPEAKS: Thought Communication (Nenka), his telepathy. A moment of his life ({@link ShishouVoiceLines.Group}) becomes (1) a short line of text on the WIELDER's action bar,
 * always, and (2) a spoken line through the project's voice system ({@link Voice#say}: its limiter, its switches, its packet) ONLY when that line has a recording. No line has one yet (the voice is still to be
 * chosen), so today everything is text. Limits ({@link ShishouSpeechGate}): one cooldown per wielder and group, at most ONE line per listener every four seconds. Only the wielder, and, when the wielder
 * switched broadcast on (off by default) and the server allows it, players within 32 blocks, are told; nobody else gets a packet. All on the server; state per player UUID.
 */
public final class ShishouSpeech {
    private ShishouSpeech() {}

    private static long lastPrune;

    /** Says a line of the group for the wielder. Returns true when something was sent. */
    public static boolean say(ServerPlayer wielder, ServerLevel level, ShishouVoiceLines.Group g, Object... args) {
        if (wielder == null || g == null) return false;
        if (!ShishouConfig.skillOn(ShishouSkill.THOUGHT_COMMUNICATION)) return false;
        long now = level.getGameTime();
        ShishouSpeechGate gate = ShishouLive.GATE;
        if (now - lastPrune > 2400) {
            lastPrune = now;
            gate.prune(now);
        }
        UUID w = wielder.getUUID();
        if (!gate.listenerCould(w, now) || !gate.wielderMay(w, g, now)) return false;
        gate.listenerMay(w, now);
        int counter = ShishouLive.SPEECH_COUNTER.merge(w, 1, Integer::sum);
        String id = ShishouVoiceLines.pickManifest(g, counter);
        Component text = Component.translatableWithFallback(ShishouVoiceLines.textKey(id), ShishouVoiceLines.text(id), args).withStyle(net.minecraft.ChatFormatting.AQUA);
        wielder.displayClientMessage(text, true);
        boolean voice = ShishouConfig.voice() && ShishouVoiceLines.has(id);
        if (voice) Voice.say(wielder, id, g.cooldown());
        boolean broadcast = ShishouConfig.allowBroadcast() && ShishouData.bound() && ShishouData.get().record().broadcast();
        if (broadcast || voice) {
            Vec3 at = wielder.position();
            double r2 = ShishouSpeechGate.RADIUS * ShishouSpeechGate.RADIUS;
            for (ServerPlayer p : level.players()) {
                if (p == wielder || p.isSpectator() || p.distanceToSqr(at) > r2 || !gate.listenerMay(p.getUUID(), now)) continue;
                if (broadcast) p.displayClientMessage(Component.translatableWithFallback("shishou.thought.broadcast", "Shishou: %s", text).withStyle(net.minecraft.ChatFormatting.GRAY), true);
                if (voice) Voice.say(p, id, g.cooldown());
            }
        }
        return true;
    }

    public static void forget(UUID p) { ShishouLive.GATE.forget(p); }
}
