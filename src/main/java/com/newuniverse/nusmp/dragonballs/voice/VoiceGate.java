package com.newuniverse.nusmp.dragonballs.voice;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: nobody is listened to all the time (privacy, and the cost of recognising speech). A player is listened to only while they stand at a complete, unlocked set of balls, or are the summoner of a
 * running ritual (so the wish can be spoken), and at most {@code voice.maxListeners} players at once. These are the rules, pure, so a test can pin them; the server job that feeds them is
 * {@link DbVoice}.
 */
public final class VoiceGate {
    private VoiceGate() {}

    /** Every player is looked at once a second, spread over this many ticks. */
    public static final int BUCKETS = 20;

    /** The tick (0..19 of each second) on which a player is looked at. */
    public static int bucket(UUID id) { return Math.floorMod(id.hashCode(), BUCKETS); }

    /** A whisper counts as speech only when the setting says so; normal speech always does. */
    public static boolean whisperOk(boolean whispering, boolean whisperCounts) { return !whispering || whisperCounts; }

    /** Should this player be listened to? The summoner always; anyone else only at a complete (all {@code needed} stars) set that is not stone. */
    public static boolean eligible(boolean summoner, int starsNear, int needed, boolean locked) {
        return summoner || (!locked && needed > 0 && starsNear >= needed);
    }

    /** Who is listened to now: summoners first, then the players already listened to who still qualify (so nobody flickers), then new ones, cut at {@code max}. */
    public static List<UUID> select(List<UUID> summoners, List<UUID> eligible, Set<UUID> current, int max) {
        Set<UUID> out = new LinkedHashSet<>();
        if (max <= 0) return new ArrayList<>();
        for (UUID s : summoners) if (out.size() < max) out.add(s);
        for (UUID e : eligible) if (out.size() < max && current.contains(e)) out.add(e);
        for (UUID e : eligible) if (out.size() < max) out.add(e);
        return new ArrayList<>(out);
    }
}
