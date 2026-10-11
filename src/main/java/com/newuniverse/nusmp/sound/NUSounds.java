package com.newuniverse.nusmp.sound;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NUSounds {
    private NUSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_WORD = register("zagred_word");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_SEAL = register("zagred_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_FALL = register("zagred_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_SHATTER = register("zagred_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_HEAL = register("zagred_heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_OVERWRITE = register("zagred_overwrite");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAGRED_PHASE = register("zagred_phase");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
