package com.newuniverse.nusmp.companion.voice;

import com.newuniverse.nusmp.audio.voice.Voice;
import com.newuniverse.nusmp.audio.voice.VoiceCatalog;
import com.newuniverse.nusmp.perf.PerfBudget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Why: the hook between a chat line (a lang key) and its recorded voice, so a companion changes only as her lines get recordings. A voice line is either a SOUND EVENT ({@link #register}: the lang key is spoken by
 * this event, played to the players near the speaker the way every vanilla sound is) or an id of the mod's own voice catalogue ({@link #registerVoice}: it goes through {@code audio.voice.Voice}, which brings its
 * per-player cooldowns and the {@code voice.maxPerSecond} cap). {@link #play} finds the lang key of a line and plays what is registered for it; it answers false when the line has no audio, and then nothing
 * happens (the text is the whole line). NO RECORDING EXISTS for any companion line yet: nothing is registered by the mod itself, so every call answers false today. Registered once at setup; at most
 * {@value #MAX_LINES} lines; a global budget of {@value #PER_TICK} played lines per tick stops a burst from flooding the sound packets.
 */
public final class VoiceLines {
    private VoiceLines() {}

    public static final int MAX_LINES = 2048, PER_TICK = 4;

    private static final Map<String, Supplier<SoundEvent>> SOUNDS = new ConcurrentHashMap<>();
    private static final Map<String, String> CATALOGUE = new ConcurrentHashMap<>();
    private static final PerfBudget BUDGET = new PerfBudget("companion-voice", PER_TICK);

    /** This lang key is spoken by this sound event (registered the normal way, with a sounds.json entry). */
    public static void register(String langKey, Supplier<SoundEvent> sound) {
        if (langKey == null || langKey.isEmpty() || sound == null) return;
        if (SOUNDS.size() + CATALOGUE.size() >= MAX_LINES && !SOUNDS.containsKey(langKey)) throw new IllegalStateException("too many voice lines registered");
        SOUNDS.put(langKey, sound);
    }

    /** This lang key is spoken by a line of the mod's voice catalogue ({@link VoiceCatalog}); an id that is not in the catalogue is refused. */
    public static void registerVoice(String langKey, String catalogueId) {
        if (langKey == null || langKey.isEmpty() || !VoiceCatalog.has(catalogueId)) throw new IllegalArgumentException("not a voice catalogue id: " + catalogueId);
        if (SOUNDS.size() + CATALOGUE.size() >= MAX_LINES && !CATALOGUE.containsKey(langKey)) throw new IllegalStateException("too many voice lines registered");
        CATALOGUE.put(langKey, catalogueId);
    }

    /** Removes a registration (a line whose recording was withdrawn; the self-test). */
    public static void unregister(String langKey) {
        SOUNDS.remove(langKey);
        CATALOGUE.remove(langKey);
    }

    /** Does this lang key have audio? */
    public static boolean has(String langKey) { return langKey != null && (SOUNDS.containsKey(langKey) || CATALOGUE.containsKey(langKey)); }

    /** The lang key of a line (a translatable text), or null for any other. */
    public static String keyOf(Component line) {
        return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : null;
    }

    /**
     * Plays the audio of a line at a spot, if it has any. Server side. Returns false when the line has no audio, the level is not a server level, or the per-tick budget is spent.
     */
    public static boolean play(Level level, Vec3 at, Component line, SoundSource source) {
        String key = keyOf(line);
        if (key == null || !(level instanceof ServerLevel sl) || at == null || !has(key)) return false;
        if (BUDGET.take(sl.getGameTime(), 1) < 1) return false;
        Supplier<SoundEvent> s = SOUNDS.get(key);
        if (s != null) {
            SoundEvent ev = s.get();
            if (ev == null) return false;
            sl.playSound(null, at.x, at.y, at.z, ev, source == null ? SoundSource.NEUTRAL : source, 1.0f, 1.0f);
            return true;
        }
        String id = CATALOGUE.get(key);
        return id != null && Voice.sayNear(sl, at, id) > 0;
    }

    /** Forgets everything (tests only). */
    public static void clearForTests() {
        SOUNDS.clear();
        CATALOGUE.clear();
    }
}
