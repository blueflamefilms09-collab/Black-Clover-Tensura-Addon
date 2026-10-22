package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.audio.voice.Voice;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Why: the one way Sylph SPEAKS (docs/magic/sylph.md, "Voice lines"). A mood ({@link SylphMood}) names a group of lines; this picks one (never the one said last), walks the level's players,
 * keeps those within {@link SylphRules#SOUND_RADIUS} (32 blocks) of her, asks the per-listener {@link SylphVoiceGate} (at most one Sylph line per listener every 4 seconds, the same group at most
 * every 12) and hands the line to the project's voice system ({@link Voice#say}: the server's {@code voice.maxPerSecond}, the one tiny payload, the client's own switch and volume). Server only, per
 * listener UUID, no world scan (the level's player list), no allocation beyond the line pick. A server whose {@code voice.maxPerSecond} is 0, or a Sylph whose {@code moods.voices} is off, says
 * nothing; the text of her mood is never needed.
 */
final class SylphVoice {
    private SylphVoice() {}

    static final SylphVoiceGate GATE = new SylphVoiceGate();
    private static long lastPrune;

    /**
     * Says a line of {@code group} from a point to the players near it. {@code mood} remembers the line said last per group (so the same one does not repeat). Returns how many listeners were sent it.
     */
    static int say(ServerLevel level, double x, double y, double z, String group, SylphMood.State mood, int rnd) {
        if (group == null || !SylphConfig.voices()) return 0;
        List<String> lines = SylphVoiceLines.group(group);
        int i = SylphVoiceLines.pick(group, rnd, mood.lastLine(group));
        if (i < 0 || i >= lines.size()) {                                                      // no spoken line for this group: the synthesised cue stands in (giggle, chime) or she stays quiet
            String cue = SylphVoiceLines.fallbackCue(group);
            return cue == null ? 0 : SylphSounds.play(level, x, y, z, cue);
        }
        long now = level.getGameTime();
        String id = lines.get(i);
        double r2 = SylphRules.SOUND_RADIUS * SylphRules.SOUND_RADIUS;
        int sent = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            if (!GATE.tryAcquire(p.getUUID(), group, now)) continue;
            if (Voice.say(p, id, 0L)) sent++;
        }
        if (sent > 0) mood.lineSaid(group, i);
        if (now - lastPrune >= 1200) {
            lastPrune = now;
            GATE.prune(now);
        }
        return sent;
    }

    static void forget(UUID listener) { GATE.forget(listener); }

    static void clearAll() { GATE.clear(); }
}
