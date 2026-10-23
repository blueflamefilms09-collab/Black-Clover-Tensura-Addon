package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveForge.Cue;
import com.newuniverse.nusmp.book.SpiritDiveForge.Kind;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Spirit Dive, Djinn-equip style: plays the cues of {@link SpiritDiveForge} (the one place that touches the level): the vanilla sound packet to the players near the diver, nothing of our own. */
public final class SpiritDiveSound {
    private SpiritDiveSound() {}

    /** Plays the cue of a moment at the player: nothing when the switch is off or the cooldown has not run out. Bounded: at most {@link SpiritDiveForge#MAX_SOUNDS} sounds, one cue per second per player. */
    public static void play(ServerPlayer p, Element el, Stage st, boolean character, Kind kind) {
        if (!SpiritDiveConfig.forgeSound() || p == null || !(p.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        var data = p.getPersistentData();
        if (!SpiritDiveForge.allowed(data.getLong(SpiritDiveForge.KEY), now)) return;
        data.putLong(SpiritDiveForge.KEY, Math.max(1L, now));
        float base = SpiritDiveForge.pitch(el);
        for (Cue c : SpiritDiveForge.cue(kind, st, character)) {
            SoundEvent s = sound(c.sound());
            if (s != null) level.playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, c.volume(), base * c.pitchScale());
        }
    }

    private static SoundEvent sound(String id) {
        return switch (id) {
            case "AMETHYST_BLOCK_BREAK" -> SoundEvents.AMETHYST_BLOCK_BREAK;
            case "AMETHYST_BLOCK_CHIME" -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case "AMETHYST_BLOCK_RESONATE" -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case "BEACON_ACTIVATE" -> SoundEvents.BEACON_ACTIVATE;
            case "BEACON_POWER_SELECT" -> SoundEvents.BEACON_POWER_SELECT;
            default -> null;
        };
    }
}
