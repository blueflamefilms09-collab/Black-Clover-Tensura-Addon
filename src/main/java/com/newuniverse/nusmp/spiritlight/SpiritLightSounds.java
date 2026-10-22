package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.perf.PerfBudgets;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Spirit of Light has its own sound set, synthesised by tools/gen_spirit_light_sounds.py and named in {@code sounds.json} as {@code spirit_light.*}: bright chimes (link, unlink), a choir-like
 * pad (pulse, the seraph rising), the beam's zap, a soft lion purr, and the lion cub's creature sounds (a mew, a small roar, a yawn, a chirp, a whimper: no spoken word). The seraph's spoken lines are
 * {@code spirit_light.voice.<id>} (tools/audio/spirit_light_voice.py), one event per line that HAS a file ({@link SpiritLightVoiceLines#AVAILABLE}). It is NOT the basic Tensura spirit's set and not
 * the mob voice engine's: its own register (so the shared sound files stay append only). The server decides when a sound is made and plays it for the players nearby only (the ordinary level sound, a
 * packet to the players within hearing range); every play goes through the effect packet budget ({@link PerfBudgets#effectPacket}) and a per player gap per kind ({@link SpiritLightRules#soundGap}), so
 * a crowd of linked casters is never a flood. The register is handed to the mod bus by {@link SpiritLightBoot}.
 */
public final class SpiritLightSounds {
    private SpiritLightSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final List<DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new ArrayList<>();
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> VOICE = new LinkedHashMap<>();

    static {
        for (String id : SpiritLightRules.SOUND_IDS) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id);
            EVENTS.add(SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
        for (String line : SpiritLightVoiceLines.AVAILABLE) {
            String id = voiceEventId(line);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, id);
            VOICE.put(line, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** The sound event path of a spoken line: {@code spirit_light.voice.spiritlight_beam_1}. */
    public static String voiceEventId(String line) { return "spirit_light.voice." + line; }

    /** The registered event of a kind ({@link SpiritLightRules#S_LINK} ...). */
    public static SoundEvent event(int kind) { return EVENTS.get(kind).get(); }

    /** The event of a spoken line, or null for a line without a file. */
    public static SoundEvent voice(String line) {
        DeferredHolder<SoundEvent, SoundEvent> h = VOICE.get(line);
        return h == null ? null : h.get();
    }

    /**
     * Plays a sound of {@code kind} for the players near {@code at}, if the per player gap and the effect packet budget allow. {@code last} is the player's own array of last play ticks (one slot per kind);
     * returns true when the sound was played.
     */
    public static boolean play(ServerLevel sl, Vec3 at, int kind, long[] last, float volume, float pitch) {
        long now = sl.getGameTime();
        if (kind < 0 || kind >= SpiritLightRules.SOUND_KINDS || !SpiritLightRules.gapOk(now, last[kind], SpiritLightRules.soundGap(kind))) return false;
        if (!PerfBudgets.effectPacket(now)) return false;
        last[kind] = now;
        sl.playSound(null, at.x, at.y, at.z, event(kind), SoundSource.PLAYERS, volume, pitch);
        return true;
    }
}
