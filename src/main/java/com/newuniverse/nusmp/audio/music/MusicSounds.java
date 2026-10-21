package com.newuniverse.nusmp.audio.music;

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
 * Why: one {@code SoundEvent} per music track ({@code nusmp:music.stone.explore} and so on, 25 in all), registered in a loop from {@link MusicCatalog} so the registry, the catalogue,
 * {@code sounds.json} and the files cannot drift apart (the tests and the self-test {@code dungeon.music} compare them). It is a third register next to
 * {@link com.newuniverse.nusmp.sound.NUSounds} and {@link com.newuniverse.nusmp.audio.ModSounds}, so none of the files that other builders append to is touched. The client engine plays
 * the events with the MUSIC category ({@code client/music/ThemedMusicInstance}); a server never plays them.
 */
public final class MusicSounds {
    private MusicSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_TRACK = new LinkedHashMap<>();

    static {
        for (String track : MusicCatalog.trackNames()) {
            String id = MusicCatalog.eventId(track);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id);
            BY_TRACK.put(track, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** Every registered event by track name, in catalogue order. */
    public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> all() { return Collections.unmodifiableMap(BY_TRACK); }

    /** The event of a track, or null for a name that is not in the catalogue. */
    public static SoundEvent event(String track) {
        DeferredHolder<SoundEvent, SoundEvent> h = BY_TRACK.get(track);
        return h == null ? null : h.get();
    }
}
