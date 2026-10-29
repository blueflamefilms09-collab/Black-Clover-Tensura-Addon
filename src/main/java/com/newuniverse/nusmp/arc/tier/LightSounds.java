package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: Light Yagami's three spoken lines are sound events of their own: the potato chip line ("I'll take a potato chip... and eat it!") and the two death quotes ("Just as planned." and "I am
 * justice!"), made with the mod's ElevenLabs voice-line tooling and dropped in as {@code sounds/light_chip_line.ogg}, {@code light_death_just_as_planned.ogg} and
 * {@code light_death_i_am_justice.ogg}. Registered exactly like the Spirit of Light's voice events (a {@link DeferredRegister} of variable-range events named in {@code sounds.json}); a
 * missing .ogg only makes the game log a warning and play nothing, never crash. The server plays them as ordinary level sounds, so only players within hearing range get a packet.
 */
public final class LightSounds {
    private LightSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    /** The sound event ids (also the keys of {@code sounds.json}). */
    public static final String CHIP_LINE = "light_yagami.chip_line", JUST_AS_PLANNED = "light_yagami.death_just_as_planned", I_AM_JUSTICE = "light_yagami.death_i_am_justice";
    /** The ids of the two death quotes, in the order the random pick indexes them. */
    public static final String[] DEATH_QUOTES = {JUST_AS_PLANNED, I_AM_JUSTICE};

    public static final DeferredHolder<SoundEvent, SoundEvent> CHIP = reg(CHIP_LINE);
    public static final DeferredHolder<SoundEvent, SoundEvent> PLANNED = reg(JUST_AS_PLANNED);
    public static final DeferredHolder<SoundEvent, SoundEvent> JUSTICE = reg(I_AM_JUSTICE);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String id) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id);
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(rl));
    }

    /** The death quote at {@code index} (any int: it wraps), as a registered event. */
    public static SoundEvent quote(int index) { return (Math.floorMod(index, DEATH_QUOTES.length) == 0 ? PLANNED : JUSTICE).get(); }

    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }
}
