package com.newuniverse.nusmp.gnome;

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
 * Why: one {@code SoundEvent} per cue of the Gnome's own sound set ({@code nusmp:gnome.rise} and so on, {@link GnomeSoundRules#CUES}), registered in a loop from the catalogue so the registry, the catalogue,
 * {@code sounds.json} and the files cannot drift apart ({@code GnomeSoundsTest}, the self-test {@code magic.gnome}). A register of its own, next to the mob, voice and music ones, so none of the files other
 * builders append to is touched. The events are variable range: the distance each carries is the {@code attenuation_distance} that {@code sounds.json} gives it. Played by {@link GnomeVoice}.
 */
public final class GnomeSounds {
    private GnomeSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new LinkedHashMap<>();

    static {
        for (GnomeSoundRules.Cue c : GnomeSoundRules.CUES) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, c.event());
            EVENTS.put(c.id(), SOUNDS.register(c.event(), () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** Every registered event by cue id, in catalogue order. */
    public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> all() { return Collections.unmodifiableMap(EVENTS); }

    /** The event of a cue, or null for an id that is not in the catalogue. */
    public static SoundEvent event(String cueId) {
        DeferredHolder<SoundEvent, SoundEvent> h = EVENTS.get(cueId);
        return h == null ? null : h.get();
    }
}
