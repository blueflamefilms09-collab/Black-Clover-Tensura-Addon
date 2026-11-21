package com.newuniverse.nusmp.dragonballs.voice;

import com.newuniverse.nusmp.dragonballs.DbConfig;
import com.newuniverse.nusmp.dragonballs.DbVoiceProbe;
import com.newuniverse.nusmp.song.SongMath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the one place where the voice thread and the game meet, and the owner of the voice feature's life cycle. The voice thread (see {@link DbVoicePlugin}) may only call {@link #listening}, {@link #whisperOk},
 * {@link #feed} and {@link #endTransmission}: they read a volatile immutable set and hand audio to the bounded {@link SpeechEngine}; nothing of the game is touched. The server thread runs the gate job: once a
 * second, every online player is looked at once (spread over the 20 ticks of the second), and the listened-to set is chosen by {@link VoiceGate} (at most {@code voice.maxListeners}) and published as a new
 * immutable set. Cost per tick: one pass over the player list comparing a hash, plus the probe for the 1/20 of the players whose turn it is (their inventory and a 16-row registry; no world scan, no chunk).
 * Nothing at all runs when voice is off, Simple Voice Chat is not installed, or no recogniser loaded. Per-player state is keyed by UUID and dropped on logout.
 */
public final class DbVoice {
    private DbVoice() {}

    private static volatile MinecraftServer server;
    private static volatile SpeechEngine engine;
    private static volatile Set<UUID> listening = Set.of();
    private static volatile List<String> vocab = List.of();
    private static volatile VocabularySource source = new DefaultVocabulary();
    private static int vocabKey;
    private static final Map<UUID, Boolean> ELIGIBLE = new HashMap<>();   // server thread only

    // ================================================================ voice thread API
    /** Voice thread: is this player listened to right now? A set lookup; frames of everyone else are dropped before they are decoded. */
    public static boolean listening(UUID id) { return listening.contains(id); }

    public static boolean whisperOk(boolean whispering) { return VoiceGate.whisperOk(whispering, DbVoiceConfig.whisperCounts()); }

    /** Voice thread: a decoded frame (any rate; 48 kHz from Simple Voice Chat) of a listened-to player. */
    public static void feed(UUID id, short[] pcm, int rate) {
        SpeechEngine en = engine;
        if (en == null || !listening.contains(id)) return;
        short[] p = Resampler.to16k(pcm, rate);
        en.feed(id, p, SongMath.rms(p));
    }

    public static void endTransmission(UUID id) {
        SpeechEngine en = engine;
        if (en != null) en.endTransmission(id);
    }

    // ================================================================ server thread API
    public static MinecraftServer server() { return server; }

    /** Replaces where the recogniser's and the matcher's words come from (the wish tables). */
    public static void setVocabularySource(VocabularySource s) {
        if (s != null) source = s;
        vocabKey = 0;
    }

    /** The ritual vocabulary (single words). Server thread. */
    public static List<String> vocabulary() {
        refreshVocab();                                   // cheap (about 200 words, one hash) and once per spoken text, so a config change is seen at once
        return vocab;
    }

    private static void refreshVocab() {
        List<String> words = source.words();
        boolean grammar = DbVoiceConfig.useGrammar();
        int key = words.hashCode() * 31 + (grammar ? 1 : 0);
        if (key == vocabKey && !vocab.isEmpty()) return;
        vocabKey = key;
        List<String> v = SpeechMatch.vocabulary(words);
        vocab = v;
        SpeechEngine en = engine;
        if (en != null) en.setGrammar(grammar ? SpeechMatch.grammarJson(v) : "");
    }

    static boolean voiceModPresent() {
        ModList l = ModList.get();
        return l != null && l.isLoaded("voicechat");
    }

    // ================================================================ events
    public static void onServerStarted(ServerStartedEvent e) {
        server = e.getServer();
        if (!DbVoiceConfig.enabled() || !DbConfig.enabled() || !voiceModPresent()) return;
        Path lib = Path.of(DbVoiceConfig.libraryPath()), model = Path.of(DbVoiceConfig.modelPath());
        engine = new BoundedEngine(() -> VoskEngine.load(lib, model), DbVoice::fromEngine);
        refreshVocab();
    }

    public static void onServerStopping(ServerStoppingEvent e) {
        SpeechEngine en = engine;
        engine = null;
        listening = Set.of();
        ELIGIBLE.clear();
        vocab = List.of();
        vocabKey = 0;
        server = null;
        if (en != null) en.close();
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        ELIGIBLE.remove(id);
        SpeechEngine en = engine;
        if (en != null) en.forget(id);
    }

    /** Worker thread: a recognised text. Only hands it to the server thread. */
    private static void fromEngine(UUID id, String text) {
        MinecraftServer s = server;
        if (s != null) s.execute(() -> SpeechBridge.heard(id, text));
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        SpeechEngine en = engine;
        if (en == null) return;
        if (!en.available() || !DbVoiceConfig.enabled() || !DbConfig.enabled()) {
            if (!listening.isEmpty()) publish(Set.of());
            return;
        }
        MinecraftServer s = e.getServer();
        int tick = s.getTickCount(), bucket = tick % VoiceGate.BUCKETS;
        List<ServerPlayer> players = s.getPlayerList().getPlayers();
        for (ServerPlayer p : players)
            if (VoiceGate.bucket(p.getUUID()) == bucket) ELIGIBLE.put(p.getUUID(), p.isAlive() && !p.isSpectator() && DbVoiceProbe.nearCompleteSet(p));
        if (bucket != VoiceGate.BUCKETS - 1) return;
        List<UUID> eligible = new ArrayList<>();
        for (ServerPlayer p : players) if (Boolean.TRUE.equals(ELIGIBLE.get(p.getUUID()))) eligible.add(p.getUUID());
        List<UUID> pick = VoiceGate.select(DbVoiceProbe.summoners(), eligible, listening, DbVoiceConfig.maxListeners());
        Set<UUID> next = pick.isEmpty() ? Set.of() : Set.copyOf(pick);
        if (!next.equals(listening)) publish(next);
        if (tick % 200 == VoiceGate.BUCKETS - 1) refreshVocab();
    }

    private static void publish(Set<UUID> next) {
        Set<UUID> old = listening;
        listening = next;
        SpeechEngine en = engine;
        if (en != null) for (UUID id : old) if (!next.contains(id)) en.forget(id);
    }
}
