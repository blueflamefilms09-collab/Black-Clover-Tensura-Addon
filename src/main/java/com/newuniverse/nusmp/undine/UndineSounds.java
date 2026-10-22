package com.newuniverse.nusmp.undine;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: one {@code SoundEvent} per cue of {@link UndineSoundSet} ({@code nusmp:undine.ambient}, {@code undine.link}, ...), registered in a loop from the catalogue so the registry, the catalogue,
 * {@code sounds.json} and the files cannot drift apart ({@code UndineSoundsTest} and the self-test {@code magic.undine} compare them). It is its own register next to {@code NUSounds},
 * {@code ModSounds}, {@code MusicSounds} and {@code VoiceSounds}, so none of the files other builders append to is touched. The events are variable-range: the attenuation distance of each is the
 * one {@code sounds.json} gives it.
 */
public final class UndineSounds {
    private UndineSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<UndineSoundSet.Cue, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new EnumMap<>(UndineSoundSet.Cue.class);

    static {
        for (UndineSoundSet.Cue c : UndineSoundSet.Cue.values()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, c.event());
            EVENTS.put(c, SOUNDS.register(c.event(), () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** The event of a cue. */
    public static SoundEvent event(UndineSoundSet.Cue cue) { return EVENTS.get(cue).get(); }

    /** The registry holder of the event (what a sound packet wants: the reference holder, so the packet carries a registry id, not a copy of the event). */
    public static Holder<SoundEvent> holder(UndineSoundSet.Cue cue) { return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event(cue)); }
}
