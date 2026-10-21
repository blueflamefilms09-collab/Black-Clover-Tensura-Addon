package com.newuniverse.nusmp.audio.voice;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: one {@code SoundEvent} per spoken line ({@code nusmp:voice.system_floor_enter} and so on, 120 in all), registered in a loop from {@link VoiceCatalog} so the registry, the
 * catalogue, {@code sounds.json} and the files cannot drift apart (the tests and the self-test {@code audio.voices} compare them). It is a fourth register next to
 * {@link com.newuniverse.nusmp.sound.NUSounds}, {@link com.newuniverse.nusmp.audio.ModSounds} and {@link com.newuniverse.nusmp.audio.music.MusicSounds}, so none of the files that other
 * builders append to is touched. The client plays the events with the VOICE category ({@code client/voice/VoiceClient}); a server never plays them.
 */
public final class VoiceSounds {
    private VoiceSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_LINE = new LinkedHashMap<>();

    static {
        for (String line : VoiceCatalog.ids()) {
            String id = VoiceCatalog.eventId(line);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id);
            BY_LINE.put(line, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** Every registered event by line id, in catalogue order (the id -> holder map of the catalogue). */
    public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> all() { return Collections.unmodifiableMap(BY_LINE); }

    /** The event of a line, or null for an id that is not in the catalogue. */
    public static SoundEvent event(String line) {
        DeferredHolder<SoundEvent, SoundEvent> h = BY_LINE.get(line);
        return h == null ? null : h.get();
    }
}
