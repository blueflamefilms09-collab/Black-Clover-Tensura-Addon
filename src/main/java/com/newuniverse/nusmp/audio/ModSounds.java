package com.newuniverse.nusmp.audio;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the bespoke audio engine's registry (the owner's "custom DeferredRegister of SoundEvent mapping to specific audio cues"). One {@code SoundEvent} per family and cue of
 * {@link MobSoundSet} (25 families x 8 cues = 200 events, {@code nusmp:mob.goblin.taunt} and so on), built in a loop from the catalogue so the registry, the catalogue and
 * {@code sounds.json} cannot drift apart ({@code SoundsJsonTest}). It is a second register next to {@link com.newuniverse.nusmp.sound.NUSounds} (the Zagred voice lines): that one
 * names every event by hand, this one would need two hundred lines, and the two files stay append-only for the builders that edit them. The register is handed to the mod bus in
 * {@code NUSMP} ({@code ModSounds.SOUNDS.register(modEventBus)}). The events are variable-range, so the attenuation distance of each is the one {@code sounds.json} gives it.
 */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new LinkedHashMap<>();

    static {
        for (String id : MobSoundSet.allEventIds()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id);
            EVENTS.put(id, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** Every registered event by its path ({@code mob.goblin.taunt}), in catalogue order. */
    public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> all() { return Collections.unmodifiableMap(EVENTS); }

    /** The event of a cue of a family; a family that is not in the catalogue answers with the default family's event. */
    public static SoundEvent event(String family, Cue cue) {
        DeferredHolder<SoundEvent, SoundEvent> h = EVENTS.get(MobSoundSet.eventId(MobSoundSet.has(family) ? family : MobSoundSet.DEFAULT_FAMILY, cue));
        return h.get();
    }

    /** The registry holder of the event (what {@code PlayLevelSoundEvent#setSound} wants; the reference holder, so the packet carries a registry id, not a copy of the event). */
    public static Holder<SoundEvent> holder(String family, Cue cue) {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event(family, cue));
    }
}
