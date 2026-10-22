package com.newuniverse.nusmp.spiritspace;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the Spirit of Space has its own voice, apart from the basic spirits and apart from the mob voices: a deep cosmic hum, a glassy ping, the spatial "tear" whoosh, the swell of a portal opening.
 * One sound event per {@link SoundGate.Kind} ({@code nusmp:spirit_space.hum} and so on), each with two variants synthesised by tools/gen_spirit_space_sounds.py (files under
 * {@code sounds/spirit_space/}, entries in {@code sounds.json}; {@code SpiritSpaceSoundsTest} keeps the three in step). The register is its own (the mob voice register is not edited) and is handed to the
 * mod bus by {@link SpiritSpaceBoot}.
 *
 * <p>The sounds are played by the SERVER with {@code ServerLevel.playSound}, which reaches only the players within the sound's range, and every one is asked of the {@link SoundGate} first (a gap per
 * player and kind, and a cap per tick), so a crowd of casters cannot flood the network.</p>
 */
public final class SpiritSpaceSounds {
    private SpiritSpaceSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);
    private static final Map<SoundGate.Kind, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new EnumMap<>(SoundGate.Kind.class);

    /** The sound event path of a kind: {@code spirit_space.hum}. */
    public static String id(SoundGate.Kind k) { return k.eventId(); }

    static {
        for (SoundGate.Kind k : SoundGate.Kind.values()) {
            if (k == SoundGate.Kind.VOICE) continue;                                          // the voice cues are registered below, one event per cue
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id(k));
            EVENTS.put(k, SOUNDS.register(id(k), () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    private static final Map<SpaceMoodRules.Voice, DeferredHolder<SoundEvent, SoundEvent>> VOICES = new EnumMap<>(SpaceMoodRules.Voice.class);

    /** The sound event path of a voice cue: {@code spirit_space.voice_whisper}. */
    public static String voiceId(SpaceMoodRules.Voice v) { return v.eventId(); }

    static {
        for (SpaceMoodRules.Voice v : SpaceMoodRules.Voice.values()) {
            if (v == SpaceMoodRules.Voice.NONE) continue;
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, voiceId(v));
            VOICES.put(v, SOUNDS.register(voiceId(v), () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    public static SoundEvent event(SoundGate.Kind k) { return EVENTS.get(k).get(); }

    public static SoundEvent voiceEvent(SpaceMoodRules.Voice v) { return VOICES.get(v).get(); }

    /** The gate of the server: single threaded (the server thread). */
    static final SoundGate GATE = new SoundGate();

    /** Plays a kind at a place to the players near it, if the gate lets this player's sound through. */
    public static void play(ServerLevel sl, ServerPlayer owner, SoundGate.Kind k, double x, double y, double z, float volume, float pitch) {
        if (!GATE.allow(owner.getUUID(), k, sl.getGameTime())) return;
        sl.playSound(null, x, y, z, event(k), SoundSource.PLAYERS, volume, pitch);
    }
}
