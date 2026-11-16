package com.newuniverse.nusmp.dungeon.rework2;

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
 * Why: one {@code SoundEvent} per cue of the dungeon bosses' own sound set ({@code anime_multiverse:dboss.slam} and so on, {@link DungeonBossSoundRules#CUES}), registered in a loop from the catalogue so
 * the registry, the catalogue, {@code sounds.json} and the files cannot drift apart ({@code DungeonBossSoundsTest}). A register of its own (the pattern of the Gnome's and the mob voices'), so none of the
 * files other builders append to is touched; it is handed to the mod bus in {@code NUSMP}. The events are variable range: the distance each carries is the one the game derives from its volume.
 */
public final class DungeonBossSounds {
    private DungeonBossSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new LinkedHashMap<>();

    static {
        for (DungeonBossSoundRules.Cue c : DungeonBossSoundRules.CUES) {
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
