package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the recorded voice lines of the Kakashi boss. A sound event is registered ONLY for a line whose recording exists ({@link KakashiLines#recorded()}, written by tools/audio/kakashi_voice.py
 * from the .ogg files present), so a line without a file costs nothing and shows its text in chat only; dropping a recording in and running the script is the whole change. The register is
 * a register of its own (no shared sound file is edited by the code). Playing uses the level's nearby-players broadcast, so only players within the sound's range hear it (rule 11); the pacing
 * is the boss's own {@link KakashiLines.Gate}. Server side only.
 */
public final class KakashiSounds {
    private KakashiSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> VOICE = new LinkedHashMap<>();

    static {
        for (String id : KakashiLines.recorded()) {
            String path = KakashiLines.eventPath(id);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path);
            VOICE.put(id, SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** Registers the events on the mod bus (called once from {@code ArcSystem.init}). */
    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }

    /** How many lines have a recording. */
    public static int voiceCount() { return VOICE.size(); }

    public static boolean has(String id) { return VOICE.containsKey(id); }

    /** Plays a recorded line at a point for the players near it; false when the line has no recording. */
    public static boolean play(ServerLevel level, Vec3 at, String id) {
        DeferredHolder<SoundEvent, SoundEvent> h = VOICE.get(id);
        if (h == null) return false;
        level.playSound(null, at.x, at.y, at.z, h.get(), SoundSource.HOSTILE, 1.4f, 1.0f);
        return true;
    }
}
