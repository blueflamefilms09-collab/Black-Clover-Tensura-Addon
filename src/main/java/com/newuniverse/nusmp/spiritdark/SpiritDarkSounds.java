package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.spiritdark.SpiritDarkSoundRules.Cue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Spirit of Darkness has its own sound set (low whispers, a dark chime, bat-wing flutter, the Shadow Step whoosh and arrival, the life steal siphon, the leaving and the demon's calling),
 * synthesised by tools/gen_spirit_dark_sounds.py and listed in sounds.json under {@code spirit.dark.*} with subtitles, and its SPOKEN lines (tools/audio/spirit_dark_voice.py; only the lines whose
 * file exists are registered, {@link SpiritDarkVoiceCatalog}). The events are registered here, on a register of their own, so no shared sound file is edited. Playing is server driven:
 * {@link #play} goes through {@link SpiritDarkSoundRules.Gate} (a per-source gap and a server-wide allowance per tick) and then uses the level's own nearby-players broadcast
 * ({@code ServerLevel#playSound}), which reaches only the players within the sound's range. {@link #say} sends a spoken line to each player within 32 blocks one by one, so each LISTENER's own
 * limits apply ({@link SpiritDarkVoiceCatalog.ListenerGate}: one line per four seconds, the same line not again for thirty).
 */
public final class SpiritDarkSounds {
    private SpiritDarkSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);
    private static final EnumMap<Cue, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new EnumMap<>(Cue.class);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> VOICE = new LinkedHashMap<>();
    static final SpiritDarkSoundRules.Gate GATE = new SpiritDarkSoundRules.Gate();
    static final SpiritDarkVoiceCatalog.ListenerGate LISTENERS = new SpiritDarkVoiceCatalog.ListenerGate();

    static {
        for (Cue c : Cue.values()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, c.eventPath());
            EVENTS.put(c, SOUNDS.register(c.eventPath(), () -> SoundEvent.createVariableRangeEvent(rl)));
        }
        for (String id : SpiritDarkVoiceCatalog.ids()) {                                 // only the lines that have a recording
            String path = voicePath(id);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path);
            VOICE.put(id, SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** The event path of a spoken line. */
    public static String voicePath(String id) { return "spirit.dark.voice." + id; }

    public static SoundEvent event(Cue c) { return EVENTS.get(c).get(); }

    /** Plays a cue at a point for the players near it, if the source's gap and the tick's allowance allow it. The source is the owner's UUID. */
    public static boolean play(ServerLevel level, UUID source, Cue c, Vec3 at) {
        if (!GATE.tryPlay(source, c, level.getGameTime())) return false;
        float pitch = SpiritDarkSoundRules.pitch(c, level.random.nextFloat());
        level.playSound(null, at.x, at.y, at.z, event(c), SoundSource.PLAYERS, c.volume, pitch);
        return true;
    }

    public static boolean play(ServerPlayer owner, Cue c, Vec3 at) { return play(owner.serverLevel(), owner.getUUID(), c, at); }

    /** Says a spoken line from a point to every player within {@link SpiritDarkVoiceCatalog#RANGE} blocks (each listener's own limits apply). Returns how many heard it. */
    public static int say(ServerLevel level, Vec3 at, String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = VOICE.get(id);
        if (h == null) return 0;
        long now = level.getGameTime();
        double r2 = SpiritDarkVoiceCatalog.RANGE * SpiritDarkVoiceCatalog.RANGE;
        int heard = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p instanceof FakePlayer || p.connection == null || p.distanceToSqr(at) > r2) continue;
            if (!LISTENERS.tryHear(p.getUUID(), id, now)) continue;
            p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(h.get()), SoundSource.VOICE, at.x, at.y, at.z, 1.0f, 1.0f, level.random.nextLong()));
            heard++;
        }
        return heard;
    }

    /** How many spoken lines are registered (for the self-test). */
    public static int voiceCount() { return VOICE.size(); }

    static void forget(UUID id) {
        GATE.forget(id);
        LISTENERS.forget(id);
    }

    static void clear() {
        GATE.clear();
        LISTENERS.clear();
    }
}
