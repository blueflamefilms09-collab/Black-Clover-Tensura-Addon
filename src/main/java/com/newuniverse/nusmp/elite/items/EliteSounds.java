package com.newuniverse.nusmp.elite.items;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the plushie's squeak and the crate's opening sounds are the subsystem's own sound events (the .ogg files are synthesised by {@code tools/elite/gen_items_sounds.py} with ffmpeg, so they are
 * reproducible and tiny, and the entries are in {@code sounds.json}). Registered in a register of this subsystem's own, attached by {@link EliteItems#init}.
 */
public final class EliteSounds {
    private EliteSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    /** The rubbery squeak of a squeezed plushie (pitched per size by the block). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PLUSH_SQUEAK = register("plush_squeak");
    /** The lock turning and the lid creaking open. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_UNLOCK = register("crate_unlock");
    /** The shimmer when the rewards appear. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_REVEAL = register("crate_reveal");
    /** A short, dull thud: the crate stays shut (no key). */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_LOCKED = register("crate_locked");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void init(IEventBus modBus) { SOUNDS.register(modBus); }
}
